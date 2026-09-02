import org.json.{JSONArray, JSONObject}
import org.scalatest.events.{Event, TestCanceled, TestFailed, TestIgnored, TestPending, TestStarting, TestSucceeded}
import org.scalatest.{Args, DoNotDiscover, Reporter, Suite}

import java.io.{File, FileWriter}
import java.lang.reflect.Modifier
import java.net.URLClassLoader
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
      catch
        // A suite that cannot even be built tells us nothing about individual tests. Say so on stderr, where it joins
        // the rest of the run's diagnostics, and let the remaining suites report as usual.
        case error: Throwable => System.err.println(s"Could not run ${suiteClass.getName}: $error")

    collector.outcomes

  /** The suites compiled into `classesFolder`, ordered by class name so that a solution reports the same way twice. */
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
    val writer = new FileWriter(new File(testResultsFilePath))
    testResultsJSON(outcomes).write(writer)
    writer.close()

  /** Collects test events as they happen, on the thread that ran the test. */
  final class OutcomeCollector extends Reporter:
    private val collected = ListBuffer.empty[TestOutcome]

    def outcomes: List[TestOutcome] = collected.toList

    override def apply(event: Event): Unit = event match
      case _: TestStarting      => OutputRecorder.startTest()
      case event: TestSucceeded => record(event.testName, "pass", None)
      case event: TestFailed    => record(event.testName, "fail", Some(failureMessage(event)))
      // A pending or cancelled test says nothing about the solution either way, and the interface has no status for
      // "did not run", so both keep being reported as a pass, as they were when these results came from JUnit XML.
      case event: TestPending   => record(event.testName, "pass", None)
      case event: TestCanceled  => record(event.testName, "pass", None)
      case event: TestIgnored   => collected += TestOutcome(event.testName, "pass", None, None)
      case _                    => ()

    private def record(testName: String, status: String, message: Option[String]): Unit =
      collected += TestOutcome(testName, status, message, OutputRecorder.finishTest())

    private def failureMessage(event: TestFailed): String =
      event.throwable.flatMap(throwable => Option(throwable.getMessage)).getOrElse(event.message)
