import org.json.{JSONArray, JSONObject}
import org.scalatest.events.{Event, TestCanceled, TestFailed, TestIgnored, TestPending, TestStarting, TestSucceeded}
import org.scalatest.{Args, DoNotDiscover, Reporter, Suite}

import java.io.{File, FileWriter}
import java.lang.reflect.{InvocationTargetException, Modifier}
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.mutable.ListBuffer

/** What a single test did, before it is shaped into the exercism format by [[Application]]. */
case class TestOutcome(name: String, status: String, message: Option[String], output: Option[String])

/** Runs a solution's ScalaTest suites and records what each test reported and printed.
  *
  * This takes the place of `org.scalatest.tools.Runner`, which cannot be used for capturing per-test output: it hands
  * its events to reporters on a background thread, so by the time a reporter hears that a test started, that test may
  * already have finished printing. Driving the suites directly keeps the reporter on the thread running the test, which
  * is what lets [[OutputRecorder]] tell whose output is whose.
  */
object TestRun:

  def main(args: Array[String]): Unit =
    val status =
      try
        run(args)
        0
      catch
        case error: Throwable =>
          error.printStackTrace()
          1
    System.out.flush()
    // A solution is free to leave a non-daemon thread running - an executor it never shut down, say - and that would
    // keep this JVM alive long after the results are on disk, hanging the run until the platform gives up on it.
    // `org.scalatest.tools.Runner.main`, which this replaces, exited explicitly for the same reason.
    System.exit(status)

  private def run(args: Array[String]): Unit =
    args match
      case Array(classesFolderPath, testResultsFilePath) =>
        val classesFolder = new File(classesFolderPath)
        if !classesFolder.isDirectory then throw new RuntimeException(s"Expected $classesFolderPath to be a folder")
        val recording = OutputRecorder.install()
        val outcomes  = Console.withOut(recording)(runSuites(classesFolder))
        writeTestResults(outcomes, testResultsFilePath)
      case _ =>
        throw new RuntimeException("usage: TestRun <folder with compiled classes> <test results json file>")

  def runSuites(classesFolder: File): List[TestOutcome] =
    val loader    = new URLClassLoader(Array(classesFolder.toURI.toURL), getClass.getClassLoader)
    val collector = new OutcomeCollector

    for suiteClass <- suiteClasses(classesFolder, loader) do
      try
        val suite = suiteClass.getConstructor().newInstance().asInstanceOf[Suite]
        suite.run(None, Args(collector)).waitUntilCompleted()
      catch case error: Throwable => collector.recordSuiteFailure(suiteClass.getName, error)

    collector.outcomes

  /** The suites compiled into `classesFolder`, ordered by class name so that a solution reports the same way twice.
    *
    * The interface asks for tests in the order the tests file declares them. ScalaTest reports a suite's own tests in
    * declaration order, which covers every exercise on the track: each ships a single test file holding a single suite.
    * Across several suites this orders by class name instead, which is at least stable.
    */
  def suiteClasses(classesFolder: File, loader: ClassLoader): List[Class[?]] =
    classFiles(classesFolder)
      .map(classNameOf(classesFolder, _))
      .sorted
      .flatMap(className => loadSuiteClass(className, loader))

  private def classFiles(folder: File): List[File] =
    Option(folder.listFiles).toList.flatten.flatMap: file =>
      if file.isDirectory then classFiles(file)
      else if file.getName.endsWith(".class") then List(file)
      else Nil

  private def classNameOf(classesFolder: File, classFile: File): String =
    classesFolder.toPath
      .relativize(classFile.toPath)
      .toString
      .stripSuffix(".class")
      .replace(File.separator, ".")

  private def loadSuiteClass(className: String, loader: ClassLoader): Option[Class[?]] =
    try
      val candidate = loader.loadClass(className)
      // Companion objects and anonymous classes compile to class files too, but none of them is a suite we can run:
      // only a concrete class with a public no-argument constructor can be instantiated here. A suite the author asked
      // to be left alone is skipped, as `org.scalatest.tools.Runner` skips it.
      val runnable  = classOf[Suite].isAssignableFrom(candidate)
        && !Modifier.isAbstract(candidate.getModifiers)
        && !candidate.isAnnotationPresent(classOf[DoNotDiscover])
        && candidate.getConstructors.exists(_.getParameterCount == 0)
      Option.when(runnable)(candidate)
    catch
      // Anything the class loader chokes on - a class file for a dependency that is not on the runpath, for instance -
      // is simply not a suite we can run.
      case _: Throwable => None

  def testResultsJSON(outcomes: List[TestOutcome]): JSONObject =
    val tests = new JSONArray()
    outcomes.foreach: outcome =>
      tests.put(
        new JSONObject()
          .put("name", outcome.name)
          .put("status", outcome.status)
          .put("message", outcome.message.getOrElse(JSONObject.NULL))
          .put("output", outcome.output.getOrElse(JSONObject.NULL)),
      )
    new JSONObject().put("tests", tests)

  def writeTestResults(outcomes: List[TestOutcome], testResultsFilePath: String): Unit =
    // Serialise before opening the file, so that a failure here leaves no half-written results behind.
    val json   = testResultsJSON(outcomes)
    val writer = new FileWriter(new File(testResultsFilePath), UTF_8)
    try json.write(writer)
    finally writer.close()

  /** Collects test events as they happen, on the thread that ran the test. */
  final class OutcomeCollector extends Reporter:
    private val collected = ListBuffer.empty[TestOutcome]

    // The test a TestStarting has been seen for and no outcome recorded yet, so that a test which dies without
    // reporting can still be named. Only ever touched from the runner thread, but guarded along with `collected`
    // because ScalaTest's async suites deliver their events from an execution context of the suite's choosing.
    private var running = Option.empty[String]

    def outcomes: List[TestOutcome] = synchronized(collected.toList)

    override def apply(event: Event): Unit = synchronized:
      event match
        case event: TestStarting  =>
          running = Some(event.testName)
          OutputRecorder.startTest()
        case event: TestSucceeded => record(event.testName, "pass", None)
        case event: TestFailed    => record(event.testName, "fail", Some(failureMessage(event)))
        // A pending or cancelled test says nothing about the solution either way, and the interface has no status for
        // "did not run", so both keep being reported as a pass, as they were when these results came from JUnit XML.
        case event: TestPending   => record(event.testName, "pass", None)
        case event: TestCanceled  => record(event.testName, "pass", None)
        case event: TestIgnored   => collected += TestOutcome(event.testName, "pass", None, None)
        case _                    => ()

    /** Records a suite that could not be constructed, or a test that threw something ScalaTest treats as aborting the
      * run - a `StackOverflowError`, say, which `Engine` rethrows instead of reporting as a failure.
      *
      * Neither reaches [[apply]]: `Suite.run` emits no event for them. Without recording them here, a suite that died
      * halfway would leave a run that looks like a clean pass with the remaining tests quietly missing.
      */
    def recordSuiteFailure(suiteName: String, error: Throwable): Unit = synchronized:
      val cause = error match
        case invocation: InvocationTargetException if invocation.getCause != null => invocation.getCause
        case thrown                                                              => thrown
      // The full trace is no use to a student, but it is exactly what a maintainer needs from the run's log.
      cause.printStackTrace()
      collected += TestOutcome(running.getOrElse(suiteName), "error", Some(cause.toString), OutputRecorder.finishTest())
      running = None

    private def record(testName: String, status: String, message: Option[String]): Unit =
      collected += TestOutcome(testName, status, message, OutputRecorder.finishTest())
      running = None

    // The interface requires a message on every test that did not pass, and ScalaTest can hand us a throwable with
    // none, so fall through to whatever does carry one.
    private def failureMessage(event: TestFailed): String =
      List(
        event.throwable.flatMap(throwable => Option(throwable.getMessage)),
        Some(event.message),
        event.throwable.map(_.getClass.getName),
      ).flatten
        .find(_.trim.nonEmpty)
        .getOrElse("The test failed without reporting a reason.")
