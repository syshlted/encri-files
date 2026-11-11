# F4 — Previews: asked of encri per row, held in memory alone, dropped at every lock
*2026-10-23 · accepted; **flagged for veto on one point** (below)*

encri D112 set the rules and this builds them: a toggle of this app's own, on by default, saying it changes
this app alone; previews encri encodes, so this app never decodes an original; and never a preview on the
phone's storage. Four choices are this app's.

**A row asks for its preview when it is drawn, and stops asking when it is scrolled away** (`ListView`'s
recycler listener), and cancelling a request cancels encri's work (encri-api L12). So a long folder flung past
asks for what is on screen and little else, which matters because encri makes two previews at a time behind
a queue of 256 (encri D120). *Rejected:* encri D112's background pass, which is encri's to build and would
fill a cache encri keeps; this app asks for what it shows.

**Held in an `LruCache` of decoded pictures for the process**, an eighth of the heap and never more than
16 MiB, keyed by the document's id, its modified time, its size and the size asked: a rewritten document is a
new preview. Nothing is ever written to the phone's storage, which is why no image library is used (F1).

**Dropped at every lock, when the session goes, and when the user turns previews off**, along with every
request in flight; an answer to a request made before the drop is thrown away rather than shown. A locked vault
shows nothing of itself (F2), and a picture is more of it than a name. A preview encri cannot make — the type
is not one it previews, or the answer does not decode — is not asked for again until the next drop; one that
merely failed is.

**One square slot per row while previews are on**, so names line up, sized 48 dp, which encri fits each
preview inside without enlarging it (encri D121). Off, the slot is gone and nothing is asked for.

**Verified.** Under Robolectric and coroutine tests: asked at the row's size and held; not asked for a folder
or a type encri cannot preview; nothing asked while off; dropped at a lock, and a request in flight then is
cancelled and never shown — removing the drop at a lock fails both of those tests; a cancelled request
cancels encri's; a rewritten document asked for again. 94% of lines. **On the A20:** a photo shared into
encri from the Gallery app showed its preview in the folder, the text file beside it none, and turning previews
off took every slot away.

**Flagged for veto:**

1. **No background pass of this app's own**, so the first look at a folder of photos waits for each preview.
