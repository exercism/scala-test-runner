import java.io.{ByteArrayOutputStream, OutputStream, PrintStream}
import java.nio.charset.StandardCharsets.UTF_8

/** Collects whatever a solution prints while a single test runs.
  *
  * Students working in the in-browser editor have no debugger, so a `println` is the only way for them to look inside
  * their own code. ScalaTest never attributes standard output to individual tests, so this does it instead: [[TestRun]]
  * marks where a test begins and ends, and everything printed in between belongs to that test. Anything printed
  * outside a test - while a suite is being constructed, say - falls through to the real standard output untouched.
  */
object OutputRecorder:

  /** The test runner interface caps per-test output, notice included, at 500 characters. */
  private val MaxOutputChars = 500

  private val TruncationNotice = "Output was truncated. Please limit to 500 chars"

  // A character needs at most four bytes in UTF-8, so this is the most that can contribute to MaxOutputChars. Anything
  // past it is dropped as it arrives, so that a runaway `while (true) println(...)` cannot exhaust memory. Keep this at
  // three times MaxOutputChars or more: below that, a cap reached on multi-byte characters could decode to fewer
  // characters than `truncate` cuts to, and the replacement character left by a cut mid-sequence would survive.
  private val MaxRecordedBytes = MaxOutputChars * 4

  private val originalOut = System.out

  // One slot rather than a thread local, because [[TestRun]] runs suites and their tests one after another and only one
  // test is ever recording. A thread local would capture only threads born while the owning test was running, which
  // misses the case that matters: a student printing inside a `Future`, whose pool worker was created during some
  // earlier test - or before the run began - and so carries no buffer or a dead one. The trade is that a thread
  // outliving its test and still printing is recorded against whichever test is running then; text in the wrong bucket
  // is a far better failure than text that vanishes.
  @volatile private var recording: ByteArrayOutputStream = null

  /** Points standard output at the recording buffer and returns the stream doing the recording.
    *
    * `System.setOut` alone is not enough: `scala.Console` copies the stream it prints to when it is first touched, and
    * Scala's own `println` goes through `Console`. Callers therefore have to wrap the test run in `Console.withOut` on
    * the returned stream as well.
    */
  def install(): PrintStream =
    val stream = new PrintStream(RecordingOutputStream, true, UTF_8.name)
    System.setOut(stream)
    stream

  /** Starts recording output for a test. */
  def startTest(): Unit = recording = new ByteArrayOutputStream()

  /** Stops recording and returns what the test printed, or `None` when it printed nothing. */
  def finishTest(): Option[String] =
    val recorded = Option(recording)
    recording = null
    recorded.map(bytes => new String(bytes.toByteArray, UTF_8)).filter(_.nonEmpty).map(truncate)

  def truncate(output: String): String =
    if output.length <= MaxOutputChars then output
    else
      val limit = MaxOutputChars - TruncationNotice.length - 1
      // Cutting between the halves of a surrogate pair would strand a lone code unit at the end of the output.
      val cut   = if Character.isHighSurrogate(output.charAt(limit - 1)) then limit - 1 else limit
      output.take(cut) + "\n" + TruncationNotice

  private object RecordingOutputStream extends OutputStream:

    override def write(byte: Int): Unit =
      recording match
        case null    => originalOut.write(byte)
        case current => if current.size < MaxRecordedBytes then current.write(byte)

    override def write(bytes: Array[Byte], offset: Int, length: Int): Unit =
      recording match
        case null    => originalOut.write(bytes, offset, length)
        case current =>
          val room = MaxRecordedBytes - current.size
          if room > 0 then current.write(bytes, offset, math.min(length, room))

    override def flush(): Unit = originalOut.flush()
