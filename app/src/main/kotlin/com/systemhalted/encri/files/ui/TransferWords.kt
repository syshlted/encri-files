package com.systemhalted.encri.files.ui

import android.content.Context
import android.text.format.Formatter
import com.systemhalted.encri.files.R
import com.systemhalted.encri.files.transfer.Direction
import com.systemhalted.encri.files.transfer.Made
import com.systemhalted.encri.files.transfer.NotCopied
import com.systemhalted.encri.files.transfer.Report
import com.systemhalted.encri.files.transfer.Transfer
import com.systemhalted.encri.files.transfer.Transfers
import com.systemhalted.encri.files.transfer.Why

/**
 * What the bar at the foot of the screen says about a copy (F6, F11): how far it has got, or how it ended.
 *
 * **Names only when [namesShown]**, which is only over an open vault that is not locked: a copy the vault locked
 * under ends while the screen shows nothing of the vault (F2), and its report waits, counted but unnamed, until the
 * vault is unlocked again.
 */
@Suppress("TooManyFunctions") // One per kind of ending, and the lines each kind shares.
object TransferWords {
    fun of(
        context: Context,
        transfer: Transfer,
        namesShown: Boolean,
    ): String =
        when (transfer) {
            is Transfer.Running -> running(context, transfer)
            is Transfer.Finished -> finished(context, transfer.report, namesShown)
        }

    /** How full the bar is, out of [PROGRESS_MAX], or null when nothing says how much there is. */
    fun progressOf(running: Transfer.Running): Int? {
        val bytes = running.bytesTotal?.takeIf { it > 0 }
        return when {
            bytes != null -> (running.bytesDone.coerceAtMost(bytes) * PROGRESS_MAX / bytes).toInt()
            running.total > 1 -> running.done * PROGRESS_MAX / running.total
            else -> null
        }
    }

    const val PROGRESS_MAX = 1000

    private fun running(
        context: Context,
        running: Transfer.Running,
    ): String {
        // Held by the service, a copy carries on when the user leaves; one the platform would not let the service
        // hold may be paused or stopped, which the user has to know to stay (F8).
        val stay = if (running.held) R.string.copying_carries_on else R.string.copying_keep_open
        return listOfNotNull(
            ProgressWords.doing(context, running),
            ProgressWords.amountOf(context, running),
            context.getString(stay),
        ).joinToString("\n")
    }

    private fun finished(
        context: Context,
        report: Report,
        namesShown: Boolean,
    ): String =
        when (report) {
            is Report.In -> {
                copiedIn(context, report, namesShown)
            }

            is Report.FolderIn -> {
                copiedFolderIn(context, report, namesShown)
            }

            is Report.NoRoom -> {
                context.getString(
                    R.string.copy_in_no_room,
                    size(context, report.neededBytes),
                    size(context, report.usableBytes),
                )
            }

            is Report.Out -> {
                copiedOut(context, report, namesShown)
            }
        }

    private fun copiedIn(
        context: Context,
        report: Report.In,
        namesShown: Boolean,
    ): String {
        val total = report.copied + report.notCopied.size
        val lines = mutableListOf<String>()
        lines +=
            when {
                report.notCopied.isEmpty() -> {
                    context.resources.getQuantityString(R.plurals.copied_in, report.copied, report.copied)
                }

                report.copied == 0 -> {
                    context.getString(R.string.copied_in_none)
                }

                else -> {
                    context.getString(R.string.copied_in_some, report.copied, total)
                }
            }
        lines += notCopiedLines(context, report.notCopied, report.copied, namesShown)
        lines += afterwards(context, report.leftUnfinished, report.copied)
        return lines.joinToString("\n")
    }

    /**
     * A folder copied in (F11): whole and named, or how much of it arrived, what did not, and where what did is. Over
     * a locked vault the folder is not named either.
     */
    private fun copiedFolderIn(
        context: Context,
        report: Report.FolderIn,
        namesShown: Boolean,
    ): String {
        val name = report.name?.takeIf { namesShown }
        val lines = mutableListOf<String>()
        lines +=
            when {
                report.made == Made.WHOLE && name != null -> {
                    context.resources.getQuantityString(
                        R.plurals.copied_folder_in,
                        report.copied,
                        report.copied,
                        name,
                    )
                }

                report.made == Made.WHOLE -> {
                    context.resources.getQuantityString(
                        R.plurals.copied_folder_in_unnamed,
                        report.copied,
                        report.copied,
                    )
                }

                report.copied == 0 -> {
                    context.getString(R.string.copied_in_none)
                }

                name != null -> {
                    context.getString(R.string.copied_folder_in_some, report.copied, report.total, name)
                }

                else -> {
                    context.getString(R.string.copied_folder_in_some_unnamed, report.copied, report.total)
                }
            }
        lines += notCopiedLines(context, report.notCopied, report.copied, namesShown)
        val left =
            when (report.made) {
                Made.UNFINISHED -> R.string.folder_left to R.string.folder_left_unnamed
                Made.LEFT_EMPTY -> R.string.folder_left_empty to R.string.folder_left_empty_unnamed
                Made.WHOLE, Made.NOTHING -> null
            }
        left?.let { (named, unnamed) -> lines += folderLeft(context, name, named, unnamed) }
        lines += afterwards(context, report.leftUnfinished, report.copied)
        return lines.joinToString("\n")
    }

    private fun folderLeft(
        context: Context,
        name: String?,
        named: Int,
        unnamed: Int,
    ): String = if (name != null) context.getString(named, name) else context.getString(unnamed)

    /**
     * What was not copied. Each file refused on its own, and each folder, is named, or over a locked vault promised
     * for later; the files a copy did not reach because it had to stop are counted, with the reason it stopped.
     */
    private fun notCopiedLines(
        context: Context,
        notCopied: List<NotCopied>,
        copied: Int,
        namesShown: Boolean,
    ): List<String> {
        val lines = mutableListOf<String>()
        val (ending, each) = notCopied.partition { !it.isFolder && it.why in Transfers.ENDS_A_COPY_IN }
        if (namesShown) {
            each.take(NAMED_AT_MOST).forEach { lines += line(context, it) }
            if (each.size > NAMED_AT_MOST) {
                val more = each.size - NAMED_AT_MOST
                lines += context.resources.getQuantityString(R.plurals.not_copied_more, more, more)
            }
        } else if (each.isNotEmpty()) {
            lines += context.getString(R.string.not_copied_unlock)
        }
        ending.firstOrNull()?.let { first ->
            val reason = context.getString(reasonOf(first.why, Direction.IN))
            lines +=
                if (copied == 0 && each.isEmpty()) {
                    reason
                } else {
                    context.resources.getQuantityString(R.plurals.not_copied_count, ending.size, ending.size) + " " +
                        reason
                }
        }
        return lines
    }

    /** What is left to do once a copy in has ended: copies to delete, and originals that are still where they were. */
    private fun afterwards(
        context: Context,
        leftUnfinished: Int,
        copied: Int,
    ): List<String> =
        listOfNotNull(
            leftUnfinished.takeIf { it > 0 }?.let {
                context.resources.getQuantityString(R.plurals.left_unfinished, it, it)
            },
            context.getString(R.string.originals_stay).takeIf { copied > 0 },
        )

    private fun copiedOut(
        context: Context,
        report: Report.Out,
        namesShown: Boolean,
    ): String =
        when (report.why) {
            null -> {
                if (namesShown) {
                    context.getString(R.string.copied_out_named, report.name)
                } else {
                    context.getString(R.string.copied_out)
                }
            }

            Why.SHORT -> {
                context.getString(R.string.copy_out_short)
            }

            else -> {
                context.getString(R.string.copy_out_failed, context.getString(reasonOf(report.why, Direction.OUT))) +
                    "\n" + context.getString(R.string.copy_out_left)
            }
        }

    private fun line(
        context: Context,
        notCopied: NotCopied,
    ): String {
        val reason = context.getString(reasonOf(notCopied.why, Direction.IN))
        val name = notCopied.name
        return when {
            notCopied.isFolder && name == null -> context.getString(R.string.not_copied_folder_unnamed, reason)
            notCopied.isFolder -> context.getString(R.string.not_copied_folder_line, name, reason)
            name == null -> context.getString(R.string.not_copied_unnamed, reason)
            else -> context.getString(R.string.not_copied_line, name, reason)
        }
    }

    /** A sentence for [why], which for a folder or a document that has gone depends on which way the copy went. */
    @Suppress("CyclomaticComplexMethod") // One branch per reason, which the compiler keeps exhaustive.
    fun reasonOf(
        why: Why,
        direction: Direction,
    ): Int =
        when (why) {
            Why.NAME_TAKEN -> R.string.reason_name_taken
            Why.BAD_NAME -> R.string.reason_bad_name
            Why.NO_NAME -> R.string.reason_no_name
            Why.NO_SPACE -> R.string.reason_no_space
            Why.LOCKED -> R.string.locked
            Why.NOT_ALLOWED -> R.string.reason_not_allowed
            Why.GONE -> if (direction == Direction.IN) R.string.reason_folder_gone else R.string.reason_gone
            Why.ACCESS_OFF -> R.string.reason_access_off
            Why.ENDED -> R.string.reason_ended
            Why.UNREADABLE -> R.string.reason_unreadable
            Why.VAULT_STOPPED -> R.string.reason_vault_stopped
            Why.DESTINATION -> R.string.reason_destination
            Why.SHORT -> R.string.copy_out_short
            Why.STOPPED -> R.string.reason_stopped
            Why.FAILED -> R.string.reason_failed
            Why.STILL_LOADING -> R.string.reason_still_loading
            Why.LOOPS -> R.string.reason_loops
            Why.TOO_DEEP -> R.string.reason_too_deep
        }

    private fun size(
        context: Context,
        bytes: Long,
    ): String = Formatter.formatShortFileSize(context, bytes)

    /** A badly refused copy can name hundreds of files; the bar names this many and counts the rest. */
    private const val NAMED_AT_MOST = 10
}
