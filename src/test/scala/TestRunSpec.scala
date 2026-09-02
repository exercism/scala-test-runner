import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Args, DoNotDiscover, Suite}

import java.io.File

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

  ignore("is ignored"):
    println("never runs")

class TestRunSpec extends AnyFunSuite, Matchers:

  def outcomesOf(suite: Suite): List[TestOutcome] =
    val recording = OutputRecorder.install()
    val collector = new TestRun.OutcomeCollector
    Console.withOut(recording)(suite.run(None, Args(collector)).waitUntilCompleted())
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

  test("A failing test should be recorded with its failure message and its output"):
    val failed = outcome(outcomesOf(new PrintingExample), "prints then fails")
    failed.status should be("fail")
    failed.message should be(Some("2 was not 3"))

  test("An ignored test should be recorded as a pass with nothing to show"):
    val ignored = outcome(outcomesOf(new PrintingExample), "is ignored")
    ignored.status should be("pass")
    ignored.output should be(None)

  test("Output longer than the interface allows should be truncated to fit"):
    val truncated = OutputRecorder.truncate("sheep " * 200)
    truncated.length should be <= 500
    truncated should startWith("sheep sheep")
    truncated should endWith("\nOutput was truncated. Please limit to 500 chars")

  test("Output that fits should be left alone"):
    OutputRecorder.truncate("counted 3 sheep\n") should be("counted 3 sheep\n")

  test("Suite discovery should find compiled suites and skip the ones it cannot run"):
    val classesFolder = new File(getClass.getResource("/").toURI)
    val discovered    = TestRun.suiteClasses(classesFolder, getClass.getClassLoader).map(_.getName)
    discovered should contain("ApplicationSpec")
    discovered should contain("TestRunSpec")
    discovered should not contain "PrintingExample"

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
