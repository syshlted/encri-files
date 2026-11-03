package com.systemhalted.encri.files.transfer

import com.systemhalted.encri.client.Failure
import com.systemhalted.encri.files.FakeOpener
import com.systemhalted.encri.files.FakeOutside
import com.systemhalted.encri.files.FakeSession
import com.systemhalted.encri.files.document
import com.systemhalted.encri.files.folder
import com.systemhalted.encri.files.link.Opening
import com.systemhalted.encri.files.link.VaultLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Copying a folder and everything in it into the vault (AGENTS.md F11), against a stand-in for encri and for the app
 * keeping the folder. The half that matters is what is left when not everything arrives: never a folder in the vault
 * that reads as whole and is not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FolderCopyTest {
    private val root = folder("root", "Vault")
    private val notes = document("notes", "notes.txt", "root")
    private val session = FakeSession(root, mapOf("root" to listOf(notes)))
    private val outside = FakeOutside()
    private val trip = outside.addFolder("Trip", modified = 5_000)

    private fun TestScope.transfers(): Transfers {
        val link = VaultLink(FakeOpener(Opening.Open(session)), backgroundScope)
        link.open()
        return Transfers(link, outside, backgroundScope, EmptyCoroutineContext, pauseMillis = 0)
    }

    private fun Transfers.ended(): Report = (state.value as Transfer.Finished).report

    private fun Transfers.copyTrip() = assertTrue(copyFolderIn("root", trip.tree))

    /** The vault, folder by folder, as paths: folders end in a slash. */
    private fun shape(
        id: String = "root",
        above: String = "",
    ): List<String> =
        session.tree.getValue(id).flatMap { child ->
            val path = above + child.name
            if (child.isFolder) listOf("$path/") + shape(child.id, "$path/") else listOf(path)
        }

    private fun bytes(size: Int): ByteArray = ByteArray(size) { (it % 251).toByte() }

    private fun folderIn(
        copied: Int,
        notCopied: List<NotCopied>,
        made: Made,
        total: Int = copied + notCopied.count { !it.isFolder },
        leftUnfinished: Int = 0,
    ) = Report.FolderIn("Trip", total, copied, notCopied, leftUnfinished, made)

    // ---- Whole ---------------------------------------------------------------------------------

    @Test
    fun `a folder arrives whole, folders inside it and all, under its own name and with the dates its app gives`() =
        runTest(UnconfinedTestDispatcher()) {
            val photo = bytes(200_000)
            outside.add("a.txt", bytes(10), into = trip)
            val day = outside.addFolder("Day 1", trip, modified = 6_000)
            outside.add("beach.jpg", photo, "image/jpeg", modified = 7_000, into = day)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(folderIn(2, emptyList(), Made.WHOLE), transfers.ended())
            assertEquals(listOf("notes.txt", "Trip/", "Trip/a.txt", "Trip/Day 1/", "Trip/Day 1/beach.jpg"), shape())
            assertArrayEquals(photo, session.contents.getValue("file-2"))
            assertEquals(
                mapOf("file-2" to 7_000L, "new-Day 1" to 6_000L, "new-(unfinished) Trip" to 5_000L),
                session.modified,
            )
        }

    @Test
    fun `the folder is made unfinished, filled, and named last`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                listOf(
                    "create root (unfinished) Trip",
                    "createFile new-(unfinished) Trip (unfinished) a.txt",
                    "write file-1",
                    "rename file-1 a.txt",
                    "rename new-(unfinished) Trip Trip",
                ),
                session.changes,
            )
        }

    @Test
    fun `the whole folder is looked through before anything is made, and the bar says so`() =
        runTest(UnconfinedTestDispatcher()) {
            val day = outside.addFolder("Day 1", trip)
            outside.add("a.txt", bytes(10), into = trip)
            outside.add("b.txt", bytes(10), into = day)
            val transfers = transfers()
            val looking = mutableListOf<Transfer?>()
            outside.whileListing = {
                assertTrue("nothing is made while the folder is walked", session.changes.isEmpty())
                looking += transfers.state.value
            }
            var copying: Transfer? = null
            outside.whileReading = { copying = transfers.state.value }
            transfers.copyTrip()
            assertEquals(listOf(trip.id, day.id), outside.listed)
            assertTrue(looking.all { (it as Transfer.Running).looking })
            assertEquals(Transfer.Running(Direction.IN, 1, 2, 10, 20), copying)
        }

    @Test
    fun `an empty folder arrives as an empty folder`() =
        runTest(UnconfinedTestDispatcher()) {
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(folderIn(0, emptyList(), Made.WHOLE), transfers.ended())
            assertEquals(listOf("notes.txt", "Trip/"), shape())
        }

    @Test
    fun `a folder left unfinished by an earlier copy does not stand in the way`() =
        runTest(UnconfinedTestDispatcher()) {
            session.tree.getValue("root") += folder("old", "(unfinished) Trip", "root")
            session.tree["old"] = mutableListOf()
            outside.add("a.txt", bytes(10), into = trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(Made.WHOLE, (transfers.ended() as Report.FolderIn).made)
            assertEquals("create root (unfinished) Trip (1)", session.changes.first())
            assertEquals(listOf("notes.txt", "(unfinished) Trip/", "Trip/", "Trip/a.txt"), shape())
        }

    @Test
    fun `a folder still loading is asked again, and copied once it is all there`() =
        runTest(UnconfinedTestDispatcher()) {
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), into = day)
            outside.answers[day.id] = ArrayDeque(listOf(Contents.Loading, Contents.Loading))
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(folderIn(1, emptyList(), Made.WHOLE), transfers.ended())
            assertEquals(3, outside.listed.count { it == day.id })
        }

    // ---- Refused before anything is made -------------------------------------------------------

    @Test
    fun `a name already in the folder refuses the whole copy, and nothing is made`() =
        runTest(UnconfinedTestDispatcher()) {
            session.tree.getValue("root") += folder("taken", "Trip", "root")
            outside.add("a.txt", bytes(10), into = trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.NAME_TAKEN, isFolder = true)), Made.NOTHING, total = 1),
                transfers.ended(),
            )
            assertTrue(session.changes.isEmpty())
        }

    @Test
    fun `a folder that needs more room than the vault has is not copied at all`() =
        runTest(UnconfinedTestDispatcher()) {
            session.usable = 100
            outside.add("a.txt", bytes(60), into = trip)
            outside.add("b.txt", bytes(60), into = outside.addFolder("Day 1", trip))
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(Report.NoRoom(120, 100), transfers.ended())
            assertTrue(session.changes.isEmpty())
        }

    @Test
    fun `a folder its app will not list is not copied`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.answers[trip.id] = ArrayDeque(listOf(Contents.Refused))
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.UNREADABLE, isFolder = true)), Made.NOTHING),
                transfers.ended(),
            )
            assertTrue(session.changes.isEmpty())
        }

    @Test
    fun `a folder its app gives no name is not copied`() =
        runTest(UnconfinedTestDispatcher()) {
            val nameless = outside.addFolder(null)
            val transfers = transfers()
            assertTrue(transfers.copyFolderIn("root", nameless.tree))
            assertEquals(
                Report.FolderIn(null, 0, 0, listOf(NotCopied(null, Why.NO_NAME, isFolder = true)), 0, Made.NOTHING),
                transfers.ended(),
            )
            assertTrue(outside.listed.isEmpty())
        }

    @Test
    fun `a vault folder that cannot be listed refuses the copy with why`() =
        runTest(UnconfinedTestDispatcher()) {
            session.failNext = Failure.LOCKED
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.LOCKED, isFolder = true)), Made.NOTHING),
                transfers.ended(),
            )
        }

    @Test
    fun `encri refusing the folder itself refuses the copy`() =
        runTest(UnconfinedTestDispatcher()) {
            session.failFolder = "(unfinished) Trip" to Failure.NO_SPACE
            outside.add("a.txt", bytes(10), into = trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.NO_SPACE, isFolder = true)), Made.NOTHING, total = 1),
                transfers.ended(),
            )
            assertEquals(listOf("notes.txt"), shape())
        }

    @Test
    fun `a stop while the folder is looked through makes nothing`() =
        runTest(UnconfinedTestDispatcher()) {
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), into = day)
            val transfers = transfers()
            outside.whileListing = { transfers.stop() }
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.STOPPED, isFolder = true)), Made.NOTHING),
                transfers.ended(),
            )
            assertEquals("the folder inside was never listed", listOf(trip.id), outside.listed)
            assertTrue(session.changes.isEmpty())
        }

    @Test
    fun `one copy at a time`() =
        runTest(UnconfinedTestDispatcher()) {
            val transfers = transfers()
            var second: Boolean? = null
            outside.whileListing = { if (second == null) second = transfers.copyFolderIn("root", trip.tree) }
            transfers.copyTrip()
            assertEquals(false, second)
        }

    // ---- Not whole -----------------------------------------------------------------------------

    @Test
    fun `a folder inside it that its app will not list is named, and the folder keeps its unfinished name`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), into = day)
            outside.answers[day.id] = ArrayDeque(listOf(Contents.Refused))
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Day 1", Why.UNREADABLE, isFolder = true)), Made.UNFINISHED),
                transfers.ended(),
            )
            assertEquals(listOf("notes.txt", "(unfinished) Trip/", "(unfinished) Trip/a.txt"), shape())
            assertFalse("an unfinished folder keeps no date", "new-(unfinished) Trip" in session.modified)
        }

    @Test
    fun `a folder still loading after half a minute of asking is named with why`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            val day = outside.addFolder("Day 1", trip)
            outside.answers[day.id] = ArrayDeque(List(FolderWalk.LOADING_ASKS + 5) { Contents.Loading })
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Day 1", Why.STILL_LOADING, isFolder = true)), Made.UNFINISHED),
                transfers.ended(),
            )
            assertEquals(FolderWalk.LOADING_ASKS, outside.listed.count { it == day.id })
        }

    @Test
    fun `a folder its app lists inside itself is entered once`() =
        runTest(UnconfinedTestDispatcher()) {
            val inner = outside.addFolder("Inner", trip)
            outside.add("a.txt", bytes(10), into = inner)
            outside.alsoInside(trip, inner)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Inner/Trip", Why.LOOPS, isFolder = true)), Made.UNFINISHED),
                transfers.ended(),
            )
            assertEquals(listOf(trip.id, inner.id), outside.listed)
        }

    @Test
    fun `a folder more than 64 below the one chosen is not entered`() =
        runTest(UnconfinedTestDispatcher()) {
            val names = mutableListOf("Trip")
            var parent = trip
            var deepest = trip
            repeat(FolderWalk.MAX_DEPTH + 1) {
                names += "d${it + 1}"
                parent = deepest
                deepest = outside.addFolder("d${it + 1}", deepest)
            }
            outside.add("kept.txt", bytes(10), into = parent)
            outside.add("lost.txt", bytes(10), into = deepest)
            val transfers = transfers()
            transfers.copyTrip()
            val report = transfers.ended() as Report.FolderIn
            assertEquals(listOf(NotCopied(names.joinToString("/"), Why.TOO_DEEP, isFolder = true)), report.notCopied)
            assertEquals(1, report.copied)
            assertFalse("the one too deep was never listed", deepest.id in outside.listed)
        }

    @Test
    fun `two files of one name in one folder, and the second is refused by its path`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            outside.add("a.txt", bytes(20), into = trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/a.txt", Why.NAME_TAKEN)), Made.UNFINISHED),
                transfers.ended(),
            )
        }

    @Test
    fun `a folder that has the name of a file beside it is refused once, and what is in it is not listed`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("Day 1", bytes(10), into = trip)
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), into = day)
            outside.add("c.txt", bytes(10), into = day)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Day 1", Why.NAME_TAKEN, isFolder = true)), Made.UNFINISHED, 3),
                transfers.ended(),
            )
        }

    @Test
    fun `a folder encri will not make is refused once, and the bar still reaches the end`() =
        runTest(UnconfinedTestDispatcher()) {
            session.failFolder = "Day 1" to Failure.INVALID
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), into = day)
            outside.add("c.txt", bytes(10), into = outside.addFolder("Day 2", trip))
            val transfers = transfers()
            var last: Transfer? = null
            outside.whileReading = { last = transfers.state.value }
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Day 1", Why.BAD_NAME, isFolder = true)), Made.UNFINISHED, 2),
                transfers.ended(),
            )
            assertEquals("Day 1's file was counted past", 1, (last as Transfer.Running).done)
            assertEquals(listOf("notes.txt", "(unfinished) Trip/", "(unfinished) Trip/Day 2/"), shape().take(3))
        }

    @Test
    fun `a folder inside it with no name is said to be one`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            outside.addFolder(null, trip)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied(null, Why.NO_NAME, isFolder = true)), Made.UNFINISHED),
                transfers.ended(),
            )
        }

    @Test
    fun `a file that will not be read is named by its path, and the rest arrive`() =
        runTest(UnconfinedTestDispatcher()) {
            val day = outside.addFolder("Day 1", trip)
            outside.add("b.txt", bytes(10), failAfter = 0, into = day)
            outside.add("c.txt", bytes(10), into = day)
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip/Day 1/b.txt", Why.UNREADABLE)), Made.UNFINISHED),
                transfers.ended(),
            )
            assertEquals(
                listOf("notes.txt", "(unfinished) Trip/", "(unfinished) Trip/Day 1/", "(unfinished) Trip/Day 1/c.txt"),
                shape(),
            )
        }

    @Test
    fun `a stop keeps what finished, and counts every file it did not reach, in folders not yet made too`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            val second = outside.add("b.txt", bytes(200_000), into = outside.addFolder("Day 1", trip))
            outside.add("c.txt", bytes(10), into = outside.addFolder("Day 2", trip))
            val transfers = transfers()
            outside.whileReading = { if (outside.opened.last() == second) transfers.stop() }
            transfers.copyTrip()
            assertEquals(
                folderIn(
                    1,
                    listOf(NotCopied("Trip/Day 1/b.txt", Why.STOPPED), NotCopied("Trip/Day 2/c.txt", Why.STOPPED)),
                    Made.UNFINISHED,
                ),
                transfers.ended(),
            )
            assertEquals(
                listOf("notes.txt", "(unfinished) Trip/", "(unfinished) Trip/a.txt", "(unfinished) Trip/Day 1/"),
                shape(),
            )
        }

    @Test
    fun `a stop before anything arrived removes the folder made for it`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(200_000), into = trip)
            val transfers = transfers()
            outside.whileReading = { transfers.stop() }
            transfers.copyTrip()
            assertEquals(folderIn(0, listOf(NotCopied("Trip/a.txt", Why.STOPPED)), Made.NOTHING), transfers.ended())
            assertEquals(listOf("notes.txt"), shape())
        }

    @Test
    fun `a vault that locks under the copy leaves the folder it cannot remove, counted`() =
        runTest(UnconfinedTestDispatcher()) {
            session.refuseWriteAfter = 0 to Failure.LOCKED
            outside.add("a.txt", bytes(10), into = trip)
            outside.add("b.txt", bytes(10), into = trip)
            val transfers = transfers()
            outside.whileReading = { session.failChanges = Failure.LOCKED }
            transfers.copyTrip()
            assertEquals(
                folderIn(
                    0,
                    listOf(NotCopied("Trip/a.txt", Why.LOCKED), NotCopied("Trip/b.txt", Why.LOCKED)),
                    Made.LEFT_EMPTY,
                    leftUnfinished = 1,
                ),
                transfers.ended(),
            )
            assertEquals(listOf("notes.txt", "(unfinished) Trip/", "(unfinished) Trip/(unfinished) a.txt"), shape())
        }

    @Test
    fun `a name taken while the folder was copied keeps what arrived, under the unfinished name`() =
        runTest(UnconfinedTestDispatcher()) {
            outside.add("a.txt", bytes(10), into = trip)
            outside.whileReading = {
                if (session.tree.getValue("root").none { it.name == "Trip" }) {
                    session.tree.getValue("root") += folder("other", "Trip", "root")
                    session.tree["other"] = mutableListOf()
                }
            }
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(1, listOf(NotCopied("Trip", Why.NAME_TAKEN, isFolder = true)), Made.UNFINISHED, 1),
                transfers.ended(),
            )
            assertTrue("(unfinished) Trip/a.txt" in shape())
        }

    @Test
    fun `an empty folder that cannot take its name is removed again`() =
        runTest(UnconfinedTestDispatcher()) {
            session.afterFolderMade = {
                session.tree.getValue("root") += folder("other", "Trip", "root")
                session.tree["other"] = mutableListOf()
            }
            val transfers = transfers()
            transfers.copyTrip()
            assertEquals(
                folderIn(0, listOf(NotCopied("Trip", Why.NAME_TAKEN, isFolder = true)), Made.NOTHING),
                transfers.ended(),
            )
            assertEquals(listOf("notes.txt", "Trip/"), shape())
        }
}
