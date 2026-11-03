package com.systemhalted.encri.files.transfer

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** A file found by a walk, and how a report names it: by its path from the folder chosen, or null with no name. */
internal class Placed(
    val file: Picked,
    val path: String?,
)

/**
 * A folder found by walking the one the user chose (F11): what the app keeping it listed in it, or why it was not
 * entered. [path] is where it is from the folder chosen, that folder's own name first.
 */
internal class Found(
    val folder: PickedFolder,
    val path: String,
    val files: List<Picked>,
    val folders: List<Found>,
    /** Null for a folder listed whole; otherwise why nothing in it is copied. */
    val unlisted: Why?,
) {
    /** How a report names [file], which is in this folder. */
    fun pathOf(file: Picked): String? = file.name?.let { "$path/$it" }

    /** Every file in this folder and in every folder under it that was listed. */
    fun everyFile(): List<Placed> = files.map { Placed(it, pathOf(it)) } + folders.flatMap(Found::everyFile)
}

/**
 * Walks a folder outside the vault, folder by folder, before anything of it is copied (AGENTS.md F11), so the copy
 * knows how many files there are and how much room they need before it writes a byte, as a copy of files does (F6).
 *
 * **What cannot be listed whole is said, never skipped.** A folder whose app would not list it, failed part way, or
 * was still fetching it after [LOADING_ASKS] asks is found with the reason, and the copy reports it: a folder copied
 * without it would look whole and not be, which is the folder's form of the short read encri never gives.
 *
 * **A folder inside itself is not entered again, and nor is one more than [MAX_DEPTH] below the one chosen.** The
 * first is a document id met again among a folder's own parents, as an app keeping a graph rather than a tree could
 * list; the second is the guard for an app whose ids are paths, where a link back up makes every turn a new id, as
 * a terminal app's home folder can. Neither has been seen; without them either would copy the same files again and
 * again until the vault filled.
 */
internal class FolderWalk(
    private val outside: Outside,
    private val io: CoroutineContext,
    /** How long to wait before asking again an app that says it is still loading a folder. */
    private val pauseMillis: Long,
    private val stopped: () -> Boolean,
) {
    /** Walks [top], the folder chosen, which a report calls [name]. Stopped, what was not reached says so. */
    suspend fun walk(
        top: PickedFolder,
        name: String,
    ): Found = walk(top, name, emptyList())

    private suspend fun walk(
        folder: PickedFolder,
        path: String,
        above: List<String>,
    ): Found {
        val refusal =
            when {
                folder.id in above -> Why.LOOPS
                above.size > MAX_DEPTH -> Why.TOO_DEEP
                stopped() -> Why.STOPPED
                else -> null
            }
        if (refusal != null) return unlisted(folder, path, refusal)
        return when (val contents = contentsOf(folder)) {
            is Contents.Listed -> listed(folder, path, contents, above + folder.id)
            Contents.Loading -> unlisted(folder, path, Why.STILL_LOADING)
            Contents.Refused -> unlisted(folder, path, Why.UNREADABLE)
        }
    }

    private suspend fun listed(
        folder: PickedFolder,
        path: String,
        contents: Contents.Listed,
        above: List<String>,
    ): Found {
        val folders =
            contents.folders.map { inner ->
                val name = inner.name
                if (name == null) unlisted(inner, path, Why.NO_NAME) else walk(inner, "$path/$name", above)
            }
        return Found(folder, path, contents.files, folders, null)
    }

    private fun unlisted(
        folder: PickedFolder,
        path: String,
        why: Why,
    ) = Found(folder, path, emptyList(), emptyList(), why)

    /**
     * What [folder] holds, asked again while its app says it is still loading. A cloud app answers at once with
     * what it has and fetches the rest; what it first answers is not all there is (`DocumentsContract.EXTRA_LOADING`).
     */
    private suspend fun contentsOf(folder: PickedFolder): Contents {
        var contents = withContext(io) { outside.contents(folder) }
        var asked = 1
        while (contents == Contents.Loading && asked < LOADING_ASKS && !stopped()) {
            delay(pauseMillis)
            contents = withContext(io) { outside.contents(folder) }
            asked++
        }
        return contents
    }

    companion object {
        /** How many folders below the one chosen a walk goes; [Why.TOO_DEEP]'s sentence says the same number. */
        const val MAX_DEPTH = 64

        /** About half a minute at the pause a copy waits between asks. */
        const val LOADING_ASKS = 120
    }
}
