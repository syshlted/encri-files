package com.systemhalted.encri.files.transfer

import android.net.Uri
import com.systemhalted.encri.client.EncriDocument
import com.systemhalted.encri.client.EncriResult
import com.systemhalted.encri.client.Failure
import com.systemhalted.encri.client.NameConflict
import com.systemhalted.encri.files.link.VaultLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/** Which way a copy goes. */
enum class Direction {
    IN,
    OUT,
}

/** Why a file was not copied, which the screen says in the user's terms (F6). */
enum class Why {
    /** Something with that name is already in the folder. */
    NAME_TAKEN,

    /** encri will not take the name. */
    BAD_NAME,

    /** The app the file came from gives it no name. */
    NO_NAME,
    NO_SPACE,
    LOCKED,
    NOT_ALLOWED,

    /** Copying in, the folder is no longer in the vault; copying out, the document is not. */
    GONE,
    ACCESS_OFF,
    ENDED,

    /** The app the file came from would not hand it over, or stopped part way. */
    UNREADABLE,

    /** encri stopped handing the document over part way, as it does when the vault locks. */
    VAULT_STOPPED,

    /** The place chosen for a copy out would not take it. */
    DESTINATION,

    /** The place chosen for a copy out holds less than was written. */
    SHORT,

    /** The user stopped the copy. */
    STOPPED,
    FAILED,

    /** A folder whose app was still fetching what is in it after half a minute (F11). */
    STILL_LOADING,

    /** A folder its app lists inside itself (F11). */
    LOOPS,

    /** A folder more than [FolderWalk.MAX_DEPTH] below the one chosen (F11). */
    TOO_DEEP,
}

/**
 * A file or a folder a copy in did not bring, and why. [name] is null for one whose app gave it none; inside a folder
 * copied in, it is the path from that folder, whose own name comes first (F11).
 */
data class NotCopied(
    val name: String?,
    val why: Why,
    val isFolder: Boolean = false,
)

/** What became of the folder a copy in made for the folder chosen (F11). */
enum class Made {
    /** Everything in it arrived, and it has the folder's own name. */
    WHOLE,

    /** Not everything arrived: it holds what did, as `(unfinished) <name>`. */
    UNFINISHED,

    /** Nothing arrived, and it could not be removed: it is left as `(unfinished) <name>`, with nothing whole in it. */
    LEFT_EMPTY,

    /** Nothing was made, or what was made was removed again. */
    NOTHING,
}

/** How a copy ended. */
sealed interface Report {
    /** A copy in: how many arrived, each that did not, and how many are left with *(unfinished)* names. */
    data class In(
        val copied: Int,
        val notCopied: List<NotCopied>,
        val leftUnfinished: Int,
    ) : Report

    /**
     * A copy in of the folder [name], or of a folder whose app gave it no name when null (F11). [total] files were
     * found in it and [copied] arrived whole; [notCopied] names, by their paths, the files and folders that did not.
     * [leftUnfinished] files inside it are left with *(unfinished)* names, and [made] is what became of the folder.
     */
    data class FolderIn(
        val name: String?,
        val total: Int,
        val copied: Int,
        val notCopied: List<NotCopied>,
        val leftUnfinished: Int,
        val made: Made,
    ) : Report

    /** Nothing was copied in: the files need [neededBytes] and the vault can take [usableBytes] more. */
    data class NoRoom(
        val neededBytes: Long,
        val usableBytes: Long,
    ) : Report

    /** A copy out of [name]: [why] is null when the copy is whole at the place chosen. */
    data class Out(
        val name: String,
        val why: Why?,
    ) : Report
}

/** A copy running, or the one that last ended, which stays until the user dismisses it. */
sealed interface Transfer {
    data class Running(
        val direction: Direction,
        /** Files finished, copied or not. */
        val done: Int,
        val total: Int,
        val bytesDone: Long,
        /** Null when an app a file came from did not say how long it is. */
        val bytesTotal: Long?,
        /** True once a foreground service holds the copy, so it carries on when the user leaves the app (F8). */
        val held: Boolean = false,
        /** True while a folder copied in is looked through, before its files are counted (F11). */
        val looking: Boolean = false,
    ) : Transfer

    data class Finished(
        val report: Report,
    ) : Transfer
}

/**
 * Copies into and out of the vault, one at a time, for the life of the process (AGENTS.md F6), files or a folder
 * and everything in it (F11).
 *
 * Held by the application beside the [VaultLink], so leaving the screen does not stop a copy, and coming back
 * shows it.
 *
 * **Copying in never leaves a file that reads as whole and is not.** Each file is created as
 * `(unfinished) <name>`, written, and renamed to its own name only once encri has saved it, which is encri's
 * own rule for a share (encri D118). A copy that fails or is stopped removes what it made; one that cannot —
 * the vault locked under it — leaves the name that says what it is. encri saves a write only when the writer
 * finishes (encri-api L11), so a stopped copy is abandoned by throwing, never by returning early.
 *
 * **A name already in the folder is refused**, as every change here refuses one (F5): the file is reported,
 * never given a name the user did not choose.
 */
@Suppress("TooManyFunctions") // One per step of a copy, each with its own ending.
class Transfers(
    private val link: VaultLink,
    private val outside: Outside,
    private val scope: CoroutineScope,
    private val io: CoroutineContext = Dispatchers.IO,
    /**
     * How long a copy waits before asking another app again: for a length that comes back short after a copy out
     * (encri B102), and for a folder it says it is still loading (F11).
     */
    private val pauseMillis: Long = LENGTH_PAUSE_MILLIS,
) {
    private val mutableState = MutableStateFlow<Transfer?>(null)
    val state: StateFlow<Transfer?> = mutableState.asStateFlow()

    private val stopping = AtomicBoolean(false)
    private var held = false
    private var screens = 0

    /** The folder a copy in goes into, while the user chooses files or a folder in the system's picker. */
    var pendingFolderId: String? = null

    /** The document a copy out is of, while the user chooses a place for it in the system's picker. */
    var pendingDocument: EncriDocument? = null

    val isRunning: Boolean get() = state.value is Transfer.Running

    /** True while a screen of this app is in front, where the copy bar already says how a copy ended (F8). */
    val isWatched: Boolean get() = screens > 0

    /** A screen came to the front. */
    fun screenStarted() {
        screens++
    }

    /** A screen went from the front. */
    fun screenStopped() {
        screens = (screens - 1).coerceAtLeast(0)
    }

    /**
     * Whether a foreground service holds the copy running (F8), which the bar says: held, it carries on when the
     * user leaves; not held, it may not.
     */
    fun hold(held: Boolean) {
        this.held = held
        (state.value as? Transfer.Running)?.let { mutableState.value = it.copy(held = held) }
    }

    /** Copies the files at [uris] into [folderId]; false, and nothing done, while another copy runs. */
    fun copyIn(
        folderId: String,
        uris: List<Uri>,
    ): Boolean =
        start(Transfer.Running(Direction.IN, 0, uris.size, 0, null)) {
            runIn(folderId, uris)
        }

    /**
     * Copies the folder the user chose in the system's picker as [tree], and everything in it, into [folderId] (F11);
     * false, and nothing done, while another copy runs.
     */
    fun copyFolderIn(
        folderId: String,
        tree: Uri,
    ): Boolean =
        start(Transfer.Running(Direction.IN, 0, 0, 0, null, looking = true)) {
            runFolderIn(folderId, tree)
        }

    /** Copies [document] to [destination], a file the system's picker made; false while another copy runs. */
    fun copyOut(
        document: EncriDocument,
        destination: Uri,
    ): Boolean =
        start(Transfer.Running(Direction.OUT, 0, 1, 0, document.sizeBytes)) {
            runOut(document, destination)
        }

    /** Stops the copy running at its next block. What it had finished stays; what it was writing goes. */
    fun stop() = stopping.set(true)

    /** Forgets how the last copy ended, once the user has read it. */
    fun dismiss() {
        if (state.value is Transfer.Finished) mutableState.value = null
    }

    private fun start(
        first: Transfer.Running,
        run: suspend () -> Report,
    ): Boolean {
        if (isRunning) return false
        stopping.set(false)
        held = false
        mutableState.value = first
        scope.launch { mutableState.value = Transfer.Finished(run()) }
        return true
    }

    // ---- Copying in ----------------------------------------------------------------------------

    private suspend fun runIn(
        folderId: String,
        uris: List<Uri>,
    ): Report {
        val files = withContext(io) { uris.map(outside::describe) }
        val sizes = files.map(Picked::sizeBytes)
        val progress = Progress(Direction.IN, files.size, sizes.takeIf { null !in it }?.sumOf { it ?: 0L })
        progress.show()
        return when (val begun = begin(folderId, files) { everyFile(files, it) }) {
            is Begun.Refused -> begun.report
            is Begun.Ready -> copyAll(folderId, files, begun.taken, progress)
        }
    }

    /** What a copy in starts from: the names already in the folder, or why nothing will be copied. */
    private sealed interface Begun {
        class Ready(
            val taken: MutableSet<String>,
        ) : Begun

        class Refused(
            val report: Report,
        ) : Begun
    }

    /**
     * Lists the folder, and refuses the whole copy when the vault has no room for what is known of it; a folder that
     * cannot be listed is reported as [refusedAs] says.
     */
    private suspend fun begin(
        folderId: String,
        files: List<Picked>,
        refusedAs: (Why) -> Report,
    ): Begun {
        val taken =
            when (val listed = link.ask { it.list(folderId) }) {
                is EncriResult.Ok -> listed.value.mapTo(HashSet(), EncriDocument::name)
                is EncriResult.Failed -> return Begun.Refused(refusedAs(whyOf(listed.failure)))
            }
        val needed = files.sumOf { it.sizeBytes ?: 0L }
        val usable = (link.ask { it.usableBytes() } as? EncriResult.Ok)?.value
        return if (usable != null && needed > usable) {
            Begun.Refused(Report.NoRoom(needed, usable))
        } else {
            Begun.Ready(taken)
        }
    }

    private fun everyFile(
        files: List<Picked>,
        why: Why,
    ): Report = Report.In(0, files.map { NotCopied(it.name, why) }, 0)

    private suspend fun copyAll(
        folderId: String,
        files: List<Picked>,
        taken: MutableSet<String>,
        progress: Progress,
    ): Report {
        val outcome = InOutcome(progress)
        for (file in files) copyFile(folderId, file, file.name, taken, outcome)
        return Report.In(outcome.copied, outcome.notCopied, outcome.leftUnfinished)
    }

    /**
     * Copies [file] into [folderId], or reports why not as [shown], which is its name or its path in a folder copied
     * in. Once a copy has had to stop, every file after is reported with the same reason and not tried.
     */
    private suspend fun copyFile(
        folderId: String,
        file: Picked,
        shown: String?,
        taken: MutableSet<String>,
        outcome: InOutcome,
    ) {
        val why = outcome.ended ?: notTried(file, taken) ?: copyOne(folderId, file, requireNotNull(file.name), outcome)
        if (why == null) {
            outcome.copied++
            taken += requireNotNull(file.name)
        } else {
            outcome.notCopied += NotCopied(shown, why)
            if (why in ENDS_A_COPY_IN) outcome.ended = why
        }
        outcome.progress.fileDone(file.sizeBytes)
    }

    /** Why [file] is not even tried, or null to try it. */
    private fun notTried(
        file: Picked,
        taken: Set<String>,
    ): Why? =
        when {
            stopping.get() -> Why.STOPPED
            file.name == null -> Why.NO_NAME
            file.name in taken -> Why.NAME_TAKEN
            else -> null
        }

    /** Copies one file in; null when it arrived whole under its own name. */
    private suspend fun copyOne(
        folderId: String,
        file: Picked,
        name: String,
        outcome: InOutcome,
    ): Why? =
        when (
            val created =
                link.ask { it.createFile(folderId, UNFINISHED + name, file.mimeType, NameConflict.PICK_FREE_NAME) }
        ) {
            is EncriResult.Failed -> whyOf(created.failure)
            is EncriResult.Ok -> fill(created.value, file, name, outcome)
        }

    /** Writes [file] into [document], made for it under its unfinished name, and gives it its own name. */
    private suspend fun fill(
        document: EncriDocument,
        file: Picked,
        name: String,
        outcome: InOutcome,
    ): Why? {
        var ending: Why? = null
        val stoppedBy: (Why) -> Unit = { ending = it }
        val written =
            link.ask { session ->
                session.write(document.id) { out -> pourIn(file.uri, out, outcome.progress, stoppedBy) }
            }
        if (written is EncriResult.Failed) return discard(document, ending ?: whyOf(written.failure), outcome)
        return when (val renamed = link.ask { it.rename(document.id, name) }) {
            is EncriResult.Ok -> {
                // After the rename, which encri stamps with the time it happened. A date not kept is not worth
                // failing a whole copy over: the file is in the vault, whole, under its own name.
                file.modifiedMillis?.let { modified -> link.ask { it.setModified(document.id, modified) } }
                null
            }

            is EncriResult.Failed -> {
                // Taken since the folder was listed: the copy goes, and the file is reported as every
                // taken name is. Anything else leaves a whole copy under the unfinished name.
                if (renamed.failure == Failure.NAME_TAKEN) {
                    discard(document, Why.NAME_TAKEN, outcome)
                } else {
                    outcome.leftUnfinished++
                    whyOf(renamed.failure)
                }
            }
        }
    }

    /** Pours [uri]'s bytes into [out], a block at a time; [stoppedBy] hears why, before it throws, when it stops. */
    @Suppress("TooGenericExceptionCaught") // A provider refuses a read with whatever it likes; all are its failure.
    private fun pourIn(
        uri: Uri,
        out: OutputStream,
        progress: Progress,
        stoppedBy: (Why) -> Unit,
    ) {
        val input =
            try {
                outside.openInput(uri)
            } catch (refused: Exception) {
                abandon(Why.UNREADABLE, refused, stoppedBy)
            }
        input.use {
            val buffer = ByteArray(PIECE)
            while (true) {
                if (stopping.get()) abandon(Why.STOPPED, null, stoppedBy)
                val read =
                    try {
                        input.read(buffer)
                    } catch (refused: Exception) {
                        abandon(Why.UNREADABLE, refused, stoppedBy)
                    }
                if (read < 0) return
                // A failure here is encri refusing the write, which the client answers with encri's reason.
                out.write(buffer, 0, read)
                progress.add(read)
            }
        }
    }

    /**
     * Abandons the write being made, which encri then saves nothing of: a writer that returns is a writer that
     * finished, so stopping one means throwing out of it (encri-api L11). [stoppedBy] hears why first.
     */
    private fun abandon(
        why: Why,
        cause: Throwable?,
        stoppedBy: (Why) -> Unit,
    ): Nothing {
        stoppedBy(why)
        throw Abandoned(cause)
    }

    /** Deletes a copy that will not be kept; one that cannot be deleted is left with its unfinished name. */
    private suspend fun discard(
        document: EncriDocument,
        why: Why,
        outcome: InOutcome,
    ): Why {
        if (link.ask { it.delete(document.id) } is EncriResult.Failed) outcome.leftUnfinished++
        return why
    }

    // ---- Copying a folder in (F11) -------------------------------------------------------------

    /**
     * Walks the folder chosen whole before anything is made, so the copy knows how many files it holds and whether
     * the vault has room for them, and refuses it at once if any part of the walk was refused.
     */
    private suspend fun runFolderIn(
        folderId: String,
        tree: Uri,
    ): Report {
        val top = withContext(io) { outside.folder(tree) }
        val name = top.name ?: return refusedFolder(null, 0, Why.NO_NAME)
        val found = FolderWalk(outside, io, pauseMillis, stopping::get).walk(top, name)
        val unwalked = found.unlisted ?: Why.STOPPED.takeIf { stopping.get() }
        return if (unwalked != null) refusedFolder(name, 0, unwalked) else copyFound(folderId, found, name)
    }

    private suspend fun copyFound(
        folderId: String,
        found: Found,
        name: String,
    ): Report {
        val files = found.everyFile().map(Placed::file)
        val sizes = files.map(Picked::sizeBytes)
        val progress = Progress(Direction.IN, files.size, sizes.takeIf { null !in it }?.sumOf { it ?: 0L })
        progress.show()
        return when (val begun = begin(folderId, files) { refusedFolder(name, files.size, it) }) {
            is Begun.Refused -> {
                begun.report
            }

            is Begun.Ready -> {
                if (name in begun.taken) {
                    refusedFolder(name, files.size, Why.NAME_TAKEN)
                } else {
                    copyTree(folderId, found, name, InOutcome(progress))
                }
            }
        }
    }

    /**
     * Makes the folder as `(unfinished) <name>`, fills it, and names it only once everything in it has arrived. A
     * free name is taken for the unfinished one, so a copy left unfinished before does not stand in the way of this.
     */
    private suspend fun copyTree(
        folderId: String,
        found: Found,
        name: String,
        outcome: InOutcome,
    ): Report {
        val total = outcome.progress.total
        val made =
            when (
                val created =
                    link.ask { it.createFolder(folderId, UNFINISHED + name, NameConflict.PICK_FREE_NAME) }
            ) {
                is EncriResult.Ok -> created.value
                is EncriResult.Failed -> return refusedFolder(name, total, whyOf(created.failure))
            }
        fillFolder(made.id, found, outcome)
        val kept = finish(made, found, name, outcome)
        return Report.FolderIn(name, total, outcome.copied, outcome.notCopied, outcome.leftUnfinished, kept)
    }

    /** Copies what [found] holds into [folderId]: its files first, then each folder in it, made as it is reached. */
    private suspend fun fillFolder(
        folderId: String,
        found: Found,
        outcome: InOutcome,
    ) {
        val taken = HashSet<String>()
        for (file in found.files) copyFile(folderId, file, found.pathOf(file), taken, outcome)
        for (inner in found.folders) copyFolder(folderId, inner, taken, outcome)
    }

    private suspend fun copyFolder(
        parentId: String,
        found: Found,
        taken: MutableSet<String>,
        outcome: InOutcome,
    ) {
        val name = found.folder.name
        val why =
            outcome.ended ?: found.unlisted ?: when {
                stopping.get() -> Why.STOPPED
                name == null -> Why.NO_NAME
                name in taken -> Why.NAME_TAKEN
                else -> null
            }
        if (why != null || name == null) return notMade(found, why ?: Why.NO_NAME, outcome)
        when (val made = link.ask { it.createFolder(parentId, name, NameConflict.REFUSE) }) {
            is EncriResult.Failed -> {
                notMade(found, whyOf(made.failure), outcome)
            }

            is EncriResult.Ok -> {
                taken += name
                fillFolder(made.value.id, found, outcome)
                // Last, in case anything made inside it moved its date; a date not kept fails nothing.
                dated(made.value.id, found.folder.modifiedMillis)
            }
        }
    }

    /**
     * [found] was not made, for [why]. Once a copy has had to stop, each file in it is counted with that reason, as a
     * copy of files counts the ones it never reached (F6); otherwise the folder is reported once, by its path.
     */
    private fun notMade(
        found: Found,
        why: Why,
        outcome: InOutcome,
    ) {
        val files = found.everyFile()
        if (why in ENDS_A_COPY_IN) {
            outcome.ended = why
            files.forEach { outcome.notCopied += NotCopied(it.path, why) }
        } else {
            outcome.notCopied += NotCopied(found.folder.name?.let { found.path }, why, isFolder = true)
        }
        files.forEach { outcome.progress.fileDone(it.file.sizeBytes) }
    }

    /**
     * Gives the folder made its own name when everything arrived. One that cannot take it, or that not everything
     * reached, keeps its unfinished name with what did arrive: unlike a file, a folder holds copies that are whole,
     * and a name taken meanwhile is no reason to throw them away. One that nothing reached is removed.
     */
    private suspend fun finish(
        made: EncriDocument,
        found: Found,
        name: String,
        outcome: InOutcome,
    ): Made {
        if (outcome.notCopied.isEmpty()) {
            when (val renamed = link.ask { it.rename(made.id, name) }) {
                is EncriResult.Ok -> {
                    // After the rename, which encri stamps with the time it happened (F6).
                    dated(made.id, found.folder.modifiedMillis)
                    return Made.WHOLE
                }

                is EncriResult.Failed -> {
                    outcome.notCopied += NotCopied(name, whyOf(renamed.failure), isFolder = true)
                }
            }
        }
        return when {
            outcome.copied > 0 -> Made.UNFINISHED
            link.ask { it.delete(made.id) } is EncriResult.Ok -> Made.NOTHING
            else -> Made.LEFT_EMPTY
        }
    }

    private suspend fun dated(
        documentId: String,
        modifiedMillis: Long?,
    ) {
        modifiedMillis?.let { modified -> link.ask { it.setModified(documentId, modified) } }
    }

    /** Nothing of the folder chosen was copied, for [why], which is said of the folder itself. */
    private fun refusedFolder(
        name: String?,
        total: Int,
        why: Why,
    ): Report = Report.FolderIn(name, total, 0, listOf(NotCopied(name, why, isFolder = true)), 0, Made.NOTHING)

    // ---- Copying out ---------------------------------------------------------------------------

    private suspend fun runOut(
        document: EncriDocument,
        destination: Uri,
    ): Report {
        val progress = Progress(Direction.OUT, 1, document.sizeBytes)
        val input =
            when (val opened = link.ask { it.openRead(document.id) }) {
                is EncriResult.Ok -> opened.value
                is EncriResult.Failed -> return Report.Out(document.name, whyOf(opened.failure))
            }
        val poured = withContext(io) { input.use { pourOut(it, destination, progress) } }
        val why =
            when (poured) {
                is Poured.Stopped -> {
                    poured.why
                }

                is Poured.Whole -> {
                    val kept = keptLength(destination, poured.bytes)
                    if (kept != null && kept < poured.bytes) Why.SHORT else null
                }
            }
        progress.fileDone(document.sizeBytes)
        return Report.Out(document.name, why)
    }

    /**
     * Pours the vault's [input] into [destination], a block at a time. A block is 64 KiB, so no single write
     * is larger than a FUSE request that can cross an AppFuse bridge safely, which a destination served by
     * another app's provider may be behind (encri D88).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException") // A refusal is the destination failing, said so.
    private fun pourOut(
        input: InputStream,
        destination: Uri,
        progress: Progress,
    ): Poured {
        val output =
            try {
                outside.openOutput(destination)
            } catch (refused: Exception) {
                return Poured.Stopped(Why.DESTINATION)
            }
        var bytes = 0L
        var closed = false
        val stopped =
            try {
                pourBlocks(input, output, progress) { bytes += it }
            } finally {
                // Closing is when an app that keeps a file of its own records what arrived, so a close that
                // fails is the destination failing.
                closed = runCatching { output.close() }.isSuccess
            }
        return when {
            stopped != null -> Poured.Stopped(stopped)
            !closed -> Poured.Stopped(Why.DESTINATION)
            else -> Poured.Whole(bytes)
        }
    }

    // The destination's failures, whatever they are, and the vault's, each said as which side failed.
    @Suppress("TooGenericExceptionCaught", "ReturnCount", "SwallowedException")
    private fun pourBlocks(
        input: InputStream,
        output: OutputStream,
        progress: Progress,
        counted: (Int) -> Unit,
    ): Why? {
        val buffer = ByteArray(PIECE)
        while (true) {
            if (stopping.get()) return Why.STOPPED
            val read =
                try {
                    input.read(buffer)
                } catch (stopped: IOException) {
                    return Why.VAULT_STOPPED
                }
            if (read < 0) return null
            try {
                output.write(buffer, 0, read)
            } catch (refused: Exception) {
                return Why.DESTINATION
            }
            counted(read)
            progress.add(read)
        }
    }

    /**
     * The length [destination] reports once it reaches [expected], or the last it reported in about three
     * seconds (encri B102): cloud apps answer 0 straight after the close and the whole length a moment later,
     * so a short first answer is asked again before it is believed.
     */
    private suspend fun keptLength(
        destination: Uri,
        expected: Long,
    ): Long? {
        var reported = withContext(io) { outside.keptLength(destination) }
        var attempt = 1
        while (reported != null && reported < expected && attempt < LENGTH_ATTEMPTS) {
            delay(pauseMillis)
            reported = withContext(io) { outside.keptLength(destination) }
            attempt++
        }
        return reported
    }

    private sealed interface Poured {
        data class Whole(
            val bytes: Long,
        ) : Poured

        data class Stopped(
            val why: Why,
        ) : Poured
    }

    // ---- What both share -----------------------------------------------------------------------

    /** How a copy in is going: what arrived, what did not, and the reason it had to stop, once it has. */
    private class InOutcome(
        val progress: Progress,
    ) {
        var copied = 0
        val notCopied = mutableListOf<NotCopied>()
        var leftUnfinished = 0
        var ended: Why? = null
    }

    /** How far a copy has got, told to [state] at most once a mebibyte and at the end of every file. */
    private inner class Progress(
        private val direction: Direction,
        val total: Int,
        private val bytesTotal: Long?,
    ) {
        private var done = 0
        private var bytes = 0L
        private var fileStart = 0L
        private var told = 0L

        fun add(read: Int) {
            bytes += read
            if (bytes - told >= TELL_EVERY_BYTES) show()
        }

        /** A file finished, whether or not it was copied: the bar moves past all of it. */
        fun fileDone(sizeBytes: Long?) {
            done++
            bytes = fileStart + (sizeBytes ?: (bytes - fileStart))
            fileStart = bytes
            show()
        }

        fun show() {
            told = bytes
            mutableState.value = Transfer.Running(direction, done, total, bytes, bytesTotal, held)
        }
    }

    /** Thrown out of a writer to abandon its write, which encri then saves nothing of. */
    private class Abandoned(
        cause: Throwable?,
    ) : RuntimeException(cause)

    companion object {
        /** The name a file is copied in under until it is whole, as encri's share screen names one (encri D118). */
        const val UNFINISHED = "(unfinished) "

        /** A block: encri's own (encri D2), and what a pipe's buffer holds. */
        private const val PIECE = 64 * 1024

        private const val TELL_EVERY_BYTES = 1L shl 20
        private const val LENGTH_PAUSE_MILLIS = 250L
        private const val LENGTH_ATTEMPTS = 13

        /** Endings after which the rest of a copy in would fail the same way, so it is not tried. */
        val ENDS_A_COPY_IN =
            setOf(Why.NO_SPACE, Why.LOCKED, Why.NOT_ALLOWED, Why.GONE, Why.ACCESS_OFF, Why.ENDED, Why.STOPPED)

        /** What encri's refusal means for a copy. */
        fun whyOf(failure: Failure): Why =
            when (failure) {
                Failure.NAME_TAKEN -> Why.NAME_TAKEN
                Failure.INVALID -> Why.BAD_NAME
                Failure.NO_SPACE -> Why.NO_SPACE
                Failure.LOCKED -> Why.LOCKED
                Failure.NOT_AUTHORIZED -> Why.NOT_ALLOWED
                Failure.NOT_FOUND -> Why.GONE
                Failure.ACCESS_OFF -> Why.ACCESS_OFF
                Failure.ENDED, Failure.DISCONNECTED -> Why.ENDED
                Failure.UNSUPPORTED, Failure.FAILED, Failure.CANCELLED -> Why.FAILED
            }
    }
}
