import org.json.{JSONException, JSONObject}
import java.io.{File, FileWriter}
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

object Application:
  @main
  def run(
    buildLogFilePath: String,
    testResultsFilePath: String,
    resultsJsonFilePath: String,
  ): Unit =
    writeResultsJSON(buildLogFilePath, testResultsFilePath, resultsJsonFilePath)

  def writeResultsJSON(
    buildLogFilePath: String,
    testResultsFilePath: String,
    resultsJsonFilePath: String,
  ): Unit =
    // Build the results before opening the file, so that a failure while reading the run's output leaves the previous
    // results.json alone rather than truncating it to nothing.
    val json                  = toExercismJSON(buildLogFilePath, testResultsFilePath)
    val resultsJsonFileWriter = new FileWriter(new File(resultsJsonFilePath), UTF_8)
    try json.write(resultsJsonFileWriter)
    finally resultsJsonFileWriter.close()

  /** The outcomes [[TestRun]] recorded, or nothing at all when it never got as far as writing them.
    *
    * A run cut short - by a timeout, say - can leave a half-written file behind, which is no more use than a missing
    * one. Either way the caller reports the build log instead.
    */
  def readTestResults(testResultsFilePath: String): List[JSONObject] =
    val testResultsFile = new File(testResultsFilePath)
    if !testResultsFile.isFile then List.empty
    else
      val bufferedSource = Source.fromFile(testResultsFile, UTF_8.name)
      val rawContent     = bufferedSource.mkString
      bufferedSource.close
      try
        val tests = new JSONObject(rawContent).getJSONArray("tests")
        (0 until tests.length).map(tests.getJSONObject).toList
      catch case _: JSONException => List.empty

  def readBuildLog(buildLogFilePath: String): String =
    val fileSource = Source.fromFile(buildLogFilePath, UTF_8.name)
    val rawContent = fileSource.mkString
    fileSource.close
    rawContent

  // plain text from the compiler, not a results file
  def findErrorsInLog(buildLogFilePath: String): String =
    val rawContent = readBuildLog(buildLogFilePath)
    if rawContent.contains("Error: ") then rawContent else ""

  // Nothing ran, and the build log holds no error this runner recognises: the
  // compiler died in an unexpected way, a suite failed before reporting any
  // test, the solution held no suites at all. Show whatever the build printed
  // so that the student is not left with a bare "error".
  def missingTestResultsMessage(buildLogFilePath: String): String =
    val buildLog = readBuildLog(buildLogFilePath)
    if buildLog.nonEmpty then buildLog
    else
      "No test results were produced, so no test cases can be reported. If your solution compiles and its tests run locally, " +
        "please report this at https://github.com/exercism/scala-test-runner/issues"

  // The interface puts a run at "fail" when any test failed or errored, and reserves "error" for a run where nothing
  // ran at all. Naming the failing statuses keeps that rule readable, and immune to a status the runner never emits.
  private val FailingStatuses = Set("fail", "error")

  def toTestCaseJSON(testResult: JSONObject): JSONObject =
    new JSONObject()
      .put("name", testResult.getString("name"))
      .put("status", testResult.getString("status"))
      .put("message", orNull(testResult, "message"))
      .put("output", orNull(testResult, "output"))
      .put("test_code", JSONObject.NULL)

  // A missing key and an explicitly null one mean the same thing here, and `put` drops a key given a Java null.
  private def orNull(testResult: JSONObject, key: String): Object =
    val value = testResult.opt(key)
    if value == null then JSONObject.NULL else value

  def toExercismJSON(
    buildLogFilePath: String,
    testResultsFilePath: String,
  ): JSONObject =
    val baseObject   = new JSONObject().put("version", 2)
    val errorMessage = findErrorsInLog(buildLogFilePath)

    if errorMessage.nonEmpty then
      baseObject
        .put("status", "error")
        .put("message", errorMessage)
    else
      val testCases = readTestResults(testResultsFilePath).map(toTestCaseJSON).toArray
      if testCases.isEmpty then
        baseObject
          .put("status", "error")
          .put("message", missingTestResultsMessage(buildLogFilePath))
      else
        baseObject
          .put("status", if testCases.exists(testCase => FailingStatuses(testCase.getString("status"))) then "fail" else "pass")
          .put("message", JSONObject.NULL)
          .put("tests", testCases)
