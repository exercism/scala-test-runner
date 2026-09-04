import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Args, DoNotDiscover, Suite}

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors

/** Stands in for a student's test file. Never run on its own: [[TestRunSpec]] drives it. */
@DoNotDiscover
class PrintingExample extends AnyFunSuite:

  test("prints then passes"):
    println("counted 3 sheep")

  test("prints then fails"):
    println("about to fail")
    fail("2 was not 3")

  test("prints nothing"):
    ()

  test("prints from a thread it started"):
    val thread = new Thread(() => println("from a thread"))
    thread.start()
    thread.join()

  test("prints without a trailing newline"):
    print("no newline here")

  ignore("is ignored"):
    println("never runs")

/** Stands in for a solution printing from a `Future`, as `parallel-letter-frequency` invites students to do.
  *
  * The worker is created the first time a test submits to it and then reused, which is the case a per-thread buffer
  * cannot capture: by the second test the worker's own copy points at the first test's buffer.
  */
@DoNotDiscover
class PoolPrintingExample extends AnyFunSuite:

  private val pool = Executors.newSingleThreadExecutor: runnable =>
    val thread = new Thread(runnable, "pool-printing-example")
    thread.setDaemon(true)
    thread

  private def printFromPool(message: String): Unit =
    pool.submit(new Runnable:
      override def run(): Unit = println(message),
    ).get()

  test("the test that first uses the pool"):
    printFromPool("from the first test")

  test("a later test reusing the same pool thread"):
    printFromPool("from the second test")

/** Stands in for a solution that dies in a way ScalaTest reports no event for. */
@DoNotDiscover
class AbortingExample extends AnyFunSuite:

  test("a test that passes before the trouble starts"):
    println("still fine")

  test("a test that dies without reporting"):
    println("about to die")
    // ScalaTest treats a VirtualMachineError as aborting the run and rethrows it instead of reporting a failure, so
    // this escapes `suite.run` exactly as a real StackOverflowError from runaway recursion would.
    throw new StackOverflowError()

class TestRunSpec extends AnyFunSuite, Matchers:

  /** Runs one suite the way [[TestRun.runSuites]] runs each of them. */
  def outcomesOf(suite: Suite, suiteName: String = "ExampleSuite"): List[TestOutcome] =
    val recording = OutputRecorder.install()
    val collector = new TestRun.OutcomeCollector
    Console.withOut(recording):
      try suite.run(None, Args(collector)).waitUntilCompleted()
      catch case error: Throwable => collector.recordSuiteFailure(suiteName, error)
    collector.outcomes

  def outcome(outcomes: List[TestOutcome], name: String): TestOutcome =
    outcomes.find(_.name == name).getOrElse(fail(s"no outcome was recorded for '$name'"))

  test("Every test in a suite should be reported once, in the order it ran"):
    val outcomes = outcomesOf(new PrintingExample)
    outcomes.map(_.name) should contain theSameElementsInOrderAs List(
      "prints then passes",
      "prints then fails",
      "prints nothing",
      "prints from a thread it started",
      "prints without a trailing newline",
      "is ignored",
    )

  test("What a test printed should be recorded against that test alone"):
    val outcomes = outcomesOf(new PrintingExample)
    outcome(outcomes, "prints then passes").output should be(Some("counted 3 sheep\n"))
    outcome(outcomes, "prints then fails").output should be(Some("about to fail\n"))
    outcome(outcomes, "prints nothing").output should be(None)

  test("Output from a thread the test started should be recorded too"):
    val outcomes = outcomesOf(new PrintingExample)
    outcome(outcomes, "prints from a thread it started").output should be(Some("from a thread\n"))

  test("Output from a pooled thread should be recorded for every test that uses it"):
    val outcomes = outcomesOf(new PoolPrintingExample)
    outcome(outcomes, "the test that first uses the pool").output should be(Some("from the first test\n"))
    outcome(outcomes, "a later test reusing the same pool thread").output should be(Some("from the second test\n"))

  test("A trailing print with no newline should still be recorded"):
    outcome(outcomesOf(new PrintingExample), "prints without a trailing newline").output should be(Some("no newline here"))

  test("A failing test should be recorded with its failure message and its output"):
    val failed = outcome(outcomesOf(new PrintingExample), "prints then fails")
    failed.status should be("fail")
    failed.message should be(Some("2 was not 3"))

  test("An ignored test should be recorded as a pass with nothing to show"):
    val ignored = outcome(outcomesOf(new PrintingExample), "is ignored")
    ignored.status should be("pass")
    ignored.output should be(None)

  test("A test that dies without reporting should be recorded as an error, not silently dropped"):
    val outcomes = outcomesOf(new AbortingExample)

    outcome(outcomes, "a test that passes before the trouble starts").status should be("pass")

    val died = outcome(outcomes, "a test that dies without reporting")
    died.status should be("error")
    died.message should be(Some("java.lang.StackOverflowError"))
    died.output should be(Some("about to die\n"))

  test("A suite that cannot be constructed should be recorded under its own name, with the cause unwrapped"):
    val collector = new TestRun.OutcomeCollector
    collector.recordSuiteFailure("BrokenSuite", new InvocationTargetException(new IllegalStateException("no lasagna")))

    collector.outcomes should have size 1
    val recorded = collector.outcomes.head
    recorded.name should be("BrokenSuite")
    recorded.status should be("error")
    recorded.message should be(Some("java.lang.IllegalStateException: no lasagna"))

  test("Output longer than the interface allows should be truncated to fit"):
    val truncated = OutputRecorder.truncate("sheep " * 200)
    truncated.length should be <= 500
    truncated should startWith("sheep sheep")
    truncated should endWith("\nOutput was truncated. Please limit to 500 chars")

  test("Truncation should not strand half of a surrogate pair"):
    // A ram lands exactly on the cut: 452 filler characters, then a character that needs a surrogate pair.
    val truncated = OutputRecorder.truncate(("a" * 452) + "🐏" + ("b" * 200))
    truncated.length should be <= 500
    truncated should not include "\uD83D"
    truncated should endWith("\nOutput was truncated. Please limit to 500 chars")

  test("Output that fits should be left alone"):
    OutputRecorder.truncate("counted 3 sheep\n") should be("counted 3 sheep\n")

  test("Suite discovery should find compiled suites and skip the ones it cannot run"):
    val classesFolder = new File(getClass.getResource("/").toURI)
    val discovered    = TestRun.suiteClasses(classesFolder, getClass.getClassLoader).map(_.getName)
    discovered should contain("ApplicationSpec")
    discovered should contain("TestRunSpec")
    discovered should not contain "PrintingExample"
    discovered should not contain "AbortingExample"

  test("Recorded outcomes should be written as JSON the runner can read back"):
    val outcomes = List(
      TestOutcome("prints then passes", "pass", None, Some("counted 3 sheep\n")),
      TestOutcome("prints then fails", "fail", Some("2 was not 3"), None),
    )
    val tests    = TestRun.testResultsJSON(outcomes).getJSONArray("tests")

    assert(tests.length == 2)
    assert(tests.getJSONObject(0).getString("output") == "counted 3 sheep\n")
    assert(tests.getJSONObject(0).isNull("message"))
    assert(tests.getJSONObject(1).getString("message") == "2 was not 3")
    assert(tests.getJSONObject(1).isNull("output"))
