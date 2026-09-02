import java.io.{ByteArrayOutputStream, OutputStream, PrintStream}
import java.nio.charset.StandardCharsets.UTF_8

/** Collects whatever a solution prints while a single test runs.
  *
  * Students working in the in-browser editor have no debugger, so a `println` is the only way for them to look inside
  * their own code. ScalaTest never attributes standard output to individual tests, so this does it instead: [[TestRun]]
  * marks where a test begins and ends, and everything printed in between belongs to that test.
  *
  * Buffers are held per thread, so a test that spawns threads still records into its own buffer, and anything printed
  * outside a test - while a suite is being constructed, say - falls through to the real standard output untouched.
  */
object OutputRecorder:

  /** The test runner interface caps per-test output, notice included, at 500 characters. */
  private val MaxOutputChars = 500

  private val TruncationNotice = "Output was truncated. Please limit to 500 chars"

  // A character needs at most four bytes in UTF-8, so this is the most that can contribute to MaxOutputChars. Anything
  // past it is dropped as it arrives, so that a runaway `while (true) println(...)` cannot exhaust memory.
  private val MaxRecordedBytes = MaxOutputChars * 4

  private val originalOut = System.out

  private val recordings = new InheritableThreadLocal[ByteArrayOutputStream]

  /** Points standard output at the recording buffers, and returns the stream doing the recording.
    *
    * `System.setOut` alone is not enough: `scala.Console` copies the stream it prints to when it is first touched, and
    * Scala's own `println` goes through `Console`. Callers therefore have to wrap the test run in `Console.withOut` on
    * the returned stream as well.
    */
  def install(): PrintStream =
    val recording = new PrintStream(RecordingOutputStream, true, UTF_8.name)
    System.setOut(recording)
    recording

  /** Starts recording output for a test on the current thread. */
  def startTest(): Unit = recordings.set(new ByteArrayOutputStream())

  /** Stops recording and returns what the test printed, or `None` when it printed nothing. */
  def finishTest(): Option[String] =
    val recorded = Option(recordings.get)
    recordings.remove()
    recorded.map(bytes => new String(bytes.toByteArray, UTF_8)).filter(_.nonEmpty).map(truncate)

  def truncate(output: String): String =
    if output.length <= MaxOutputChars then output
    else output.take(MaxOutputChars - TruncationNotice.length - 1) + "\n" + TruncationNotice

  private object RecordingOutputStream extends OutputStream:

    override def write(byte: Int): Unit =
      recordings.get match
        case null      => originalOut.write(byte)
        case recording => if recording.size < MaxRecordedBytes then recording.write(byte)

    override def write(bytes: Array[Byte], offset: Int, length: Int): Unit =
      recordings.get match
        case null      => originalOut.write(bytes, offset, length)
        case recording =>
          val room = MaxRecordedBytes - recording.size
          if room > 0 then recording.write(bytes, offset, math.min(length, room))

    override def flush(): Unit = originalOut.flush()
