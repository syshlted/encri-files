package com.systemhalted.encri.files

import android.net.Uri
import com.systemhalted.encri.files.transfer.Contents
import com.systemhalted.encri.files.transfer.Outside
import com.systemhalted.encri.files.transfer.Picked
import com.systemhalted.encri.files.transfer.PickedFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The phone's storage outside the vault, standing in for the apps the system's picker reaches: files and folders to
 * copy in, and the places copies out are written to.
 */
class FakeOutside : Outside {
    private class Kept(
        val picked: Picked,
        val bytes: ByteArray,
        val failAfter: Int?,
    )

    private val files = mutableMapOf<Uri, Kept>()

    private class KeptFolder(
        val folder: PickedFolder,
        val files: MutableList<Picked> = mutableListOf(),
        val folders: MutableList<PickedFolder> = mutableListOf(),
    )

    private val folders = mutableMapOf<String, KeptFolder>()

    /** What listing a folder answers, by its id, in turn, before it answers what the folder holds. */
    val answers = mutableMapOf<String, ArrayDeque<Contents>>()

    /** Folders listed, by id, in order. */
    val listed = mutableListOf<String>()

    /** Run on every listing, which is how a test acts while a folder is walked. */
    var whileListing: (() -> Unit)? = null

    /** What each copy out left at its place, once closed. */
    val written = mutableMapOf<Uri, ByteArray>()

    /** Files a copy in opened, in order. */
    val opened = mutableListOf<Uri>()

    /** Run on every read of a file being copied in, which is how a test acts while a copy is in flight. */
    var whileReading: (() -> Unit)? = null

    /** Files that are described and then refuse to open, as a revoked grant does. */
    val unopenable = mutableSetOf<Uri>()

    /** Run on every write to a place a copy out goes. */
    var whileWriting: (() -> Unit)? = null
    var refuseOutput = false

    /** When set, a place written to refuses once it holds this many bytes. */
    var refuseWriteAfter: Int? = null

    /** When set, a place written to fails to close. */
    var refuseClose = false

    /** What a place says it holds once written; by default what arrived. */
    var keptLength: (Uri) -> Long? = { written[it]?.size?.toLong() }
    var keptAsked = 0

    /** Adds a file a picker could hand over, or one inside [into], and answers its URI. */
    @Suppress("LongParameterList") // A fixture: every field is one a test may want to set.
    fun add(
        name: String?,
        bytes: ByteArray,
        mimeType: String = "text/plain",
        size: Long? = bytes.size.toLong(),
        modified: Long? = null,
        failAfter: Int? = null,
        into: PickedFolder? = null,
    ): Uri {
        val uri = Uri.parse("content://elsewhere/${files.size + 1}")
        val picked = Picked(uri, name, mimeType, size, modified)
        files[uri] = Kept(picked, bytes, failAfter)
        into?.let { folders.getValue(it.id).files += picked }
        return uri
    }

    /** Adds a folder inside [parent], or with none one a picker could hand over, whose [PickedFolder.tree] it is. */
    fun addFolder(
        name: String?,
        parent: PickedFolder? = null,
        modified: Long? = null,
    ): PickedFolder {
        val id = "folder-${folders.size + 1}"
        val folder = PickedFolder(parent?.tree ?: Uri.parse("content://elsewhere/tree/$id"), id, name, modified)
        folders[id] = KeptFolder(folder)
        parent?.let { folders.getValue(it.id).folders += folder }
        return folder
    }

    /** Lists [folder], already somewhere, inside [parent] too, as an app keeping a graph rather than a tree could. */
    fun alsoInside(
        folder: PickedFolder,
        parent: PickedFolder,
    ) {
        folders.getValue(parent.id).folders += folder
    }

    override fun folder(tree: Uri): PickedFolder =
        folders[tree.lastPathSegment]?.folder?.takeIf { it.tree == tree } ?: PickedFolder(tree, "", null, null)

    override fun contents(folder: PickedFolder): Contents {
        listed += folder.id
        whileListing?.invoke()
        val answer = answers[folder.id]?.removeFirstOrNull()
        val kept = folders[folder.id]
        return answer ?: kept?.let { Contents.Listed(it.files.toList(), it.folders.toList()) } ?: Contents.Refused
    }

    override fun describe(uri: Uri): Picked =
        files[uri]?.picked ?: Picked(uri, null, "application/octet-stream", null, null)

    override fun openInput(uri: Uri): InputStream {
        val kept = files[uri] ?: throw FileNotFoundException("no $uri")
        if (uri in unopenable) throw SecurityException("no grant on $uri")
        opened += uri
        val bytes =
            object : ByteArrayInputStream(kept.bytes) {
                override fun read(
                    into: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    whileReading?.invoke()
                    return super.read(into, offset, length)
                }
            }
        return kept.failAfter?.let { FailingAfter(bytes, it) } ?: bytes
    }

    override fun openOutput(uri: Uri): OutputStream {
        if (refuseOutput) throw IOException("the place would not open")
        val limit = refuseWriteAfter
        return object : ByteArrayOutputStream() {
            override fun write(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ) {
                whileWriting?.invoke()
                if (limit != null && size() + length > limit) throw IOException("the place is full")
                super.write(bytes, offset, length)
            }

            override fun close() {
                written[uri] = toByteArray()
                if (refuseClose) throw IOException("the place would not close")
            }
        }
    }

    override fun keptLength(uri: Uri): Long? {
        keptAsked++
        return keptLength.invoke(uri)
    }
}
