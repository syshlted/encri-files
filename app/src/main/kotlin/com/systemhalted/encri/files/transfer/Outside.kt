package com.systemhalted.encri.files.transfer

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/** A file outside the vault, as the app that keeps it describes it. Anything it will not say is null. */
data class Picked(
    val uri: Uri,
    val name: String?,
    val mimeType: String,
    val sizeBytes: Long?,
    val modifiedMillis: Long?,
)

/**
 * A folder outside the vault, as the app that keeps it describes it (F11). [id] is the app's own document id for it,
 * inside [tree], the folder the user chose in the system's picker, whose grant covers everything under it.
 */
data class PickedFolder(
    val tree: Uri,
    val id: String,
    val name: String?,
    val modifiedMillis: Long?,
)

/** What the app keeping a folder says is in it (F11). */
sealed interface Contents {
    data class Listed(
        val files: List<Picked>,
        val folders: List<PickedFolder>,
    ) : Contents

    /** The app has said it is still fetching what is in it (`DocumentsContract.EXTRA_LOADING`), so it may be more. */
    data object Loading : Contents

    /** The app would not list it, or failed part way, or listed something it gave no way to reach. */
    data object Refused : Contents
}

/**
 * The phone's storage outside the vault: files and folders the user chose in the system's picker, to copy in, and
 * the place they chose for a copy out (AGENTS.md F6, F11).
 *
 * An interface so the copies run off a phone against a stand-in; [ResolverOutside] is the real one.
 * Everything here blocks, and is called off the main thread.
 */
interface Outside {
    /** What the app keeping [uri] says about it. A file it will not describe is described as unknown. */
    fun describe(uri: Uri): Picked

    fun openInput(uri: Uri): InputStream

    /** The folder the user chose as [tree]. One the app keeping it will not describe has no name. */
    fun folder(tree: Uri): PickedFolder

    /** What the app keeping [folder] lists in it, asked once. */
    fun contents(folder: PickedFolder): Contents

    /** A stream that replaces whatever [uri] held. */
    fun openOutput(uri: Uri): OutputStream

    /** How long the app keeping [uri] says it is, or null when it will not say. */
    fun keptLength(uri: Uri): Long?
}
