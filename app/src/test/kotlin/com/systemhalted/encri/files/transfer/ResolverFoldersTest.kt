package com.systemhalted.encri.files.transfer

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * A folder the system's picker handed over, as [ResolverOutside] reaches it (F11): through the tree it was granted
 * as, against a real `ContentProvider` standing in for the app keeping it, so every call goes through the resolver
 * and an unstable client as it does on a phone.
 */
@RunWith(RobolectricTestRunner::class)
class ResolverFoldersTest {
    private val outside = ResolverOutside(RuntimeEnvironment.getApplication().contentResolver)
    private val tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "trip")

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(TreeProvider::class.java, AUTHORITY)
        TreeProvider.extras = Bundle.EMPTY
        TreeProvider.refuses = false
        TreeProvider.rows =
            mapOf(
                "trip" to listOf(Row("trip", "Trip", Document.MIME_TYPE_DIR, null, MODIFIED)),
                "trip/children" to
                    listOf(
                        Row("a", "a.txt", "text/plain", 3, MODIFIED),
                        Row("day", "Day 1", Document.MIME_TYPE_DIR, null, MODIFIED),
                        Row("b", "b.bin", null, null, 0),
                    ),
            )
    }

    private fun trip() = outside.folder(tree)

    @Test
    fun `the folder chosen is described through its tree`() {
        assertEquals(PickedFolder(tree, "trip", "Trip", MODIFIED), trip())
    }

    @Test
    fun `what is in it comes as files and folders, each reached through the tree`() {
        assertEquals(
            Contents.Listed(
                listOf(
                    Picked(DocumentsContract.buildDocumentUriUsingTree(tree, "a"), "a.txt", "text/plain", 3, MODIFIED),
                    // A type, a size or a date its app does not give is not made up.
                    Picked(
                        DocumentsContract.buildDocumentUriUsingTree(tree, "b"),
                        "b.bin",
                        "application/octet-stream",
                        null,
                        null,
                    ),
                ),
                listOf(PickedFolder(tree, "day", "Day 1", MODIFIED)),
            ),
            outside.contents(trip()),
        )
    }

    @Test
    fun `a folder its app is still loading says so`() {
        TreeProvider.extras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, true) }
        assertEquals(Contents.Loading, outside.contents(trip()))
    }

    @Test
    fun `a folder its app reports an error beside is refused, whatever rows came with it`() {
        TreeProvider.extras = Bundle().apply { putString(DocumentsContract.EXTRA_ERROR, "offline") }
        assertEquals(Contents.Refused, outside.contents(trip()))
    }

    @Test
    fun `a row with no document id to reach it by refuses the whole listing`() {
        TreeProvider.rows += "trip/children" to listOf(Row("a", "a.txt", "text/plain", 3, 0), Row(null, "lost", null))
        assertEquals(Contents.Refused, outside.contents(trip()))
    }

    @Test
    fun `an app that fails while listing refuses`() {
        TreeProvider.refuses = true
        assertEquals(Contents.Refused, outside.contents(PickedFolder(tree, "trip", "Trip", null)))
    }

    @Test
    fun `a folder no app serves is refused, and described with no name`() {
        val nobody = DocumentsContract.buildTreeDocumentUri("nobody.here", "x")
        assertEquals(PickedFolder(nobody, "x", null, null), outside.folder(nobody))
        assertEquals(Contents.Refused, outside.contents(outside.folder(nobody)))
    }

    @Test
    fun `something that is not a tree has no name`() {
        val document = DocumentsContract.buildDocumentUri(AUTHORITY, "a")
        assertEquals(PickedFolder(document, "", null, null), outside.folder(document))
    }

    /** One row of a listing; a null [id] is a row an app gave no way to reach. */
    class Row(
        val id: String?,
        val name: String,
        val type: String?,
        val size: Long? = null,
        val modified: Long = 0,
    )

    /** The other app: a folder served as a tree, its rows by document, and extras on its listings when told to. */
    class TreeProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            check(!refuses) { "not now" }
            val listing = uri.lastPathSegment == "children"
            val id = DocumentsContract.getDocumentId(uri)
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                rows[if (listing) "$id/children" else id].orEmpty().forEach { row ->
                    addRow(columns.map { row.valueOf(it) })
                }
                if (listing) extras = TreeProvider.extras
            }
        }

        private fun Row.valueOf(column: String): Any? =
            when (column) {
                Document.COLUMN_DOCUMENT_ID -> id
                Document.COLUMN_DISPLAY_NAME -> name
                Document.COLUMN_MIME_TYPE -> type
                Document.COLUMN_SIZE -> size
                Document.COLUMN_LAST_MODIFIED -> modified
                else -> null
            }

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        companion object {
            var rows: Map<String, List<Row>> = emptyMap()
            var extras: Bundle = Bundle.EMPTY
            var refuses = false
        }
    }

    private companion object {
        const val AUTHORITY = "test.trees"
        const val MODIFIED = 1_700_000_000_000L
    }
}
