import org.json.{JSONArray, JSONObject, XML}
import java.io.{File, FileFilter, FileWriter}
import scala.io.Source

object Application:
  @main
  def run(
    buildLogFilePath: String,
    testResultsFolderPath: String,
    resultsJsonFilePath: String,
  ): Unit =
    val testResultsFolder = new File(testResultsFolderPath)
    if !testResultsFolder.isDirectory then
      throw new RuntimeException(
        s"Expected $testResultsFolderPath to be a folder",
      )
    val testResultFiles   = testResultsFolder
      .listFiles(
        new FileFilter():
          override def accept(file: File): Boolean =
            file.getName.matches("TEST-.*\\.xml"),
      )
      .toList
    writeResultsJSON(buildLogFilePath, testResultFiles, resultsJsonFilePath)

  def writeResultsJSON(
    buildLogFilePath: String,
    testResultsFiles: List[File],
    resultsJsonFilePath: String,
  ): Unit =
    val resultsJsonFile       = new File(resultsJsonFilePath)
    val resultsJsonFileWriter = new FileWriter(resultsJsonFile)

    val json = toExercismJSON(buildLogFilePath, testResultsFiles)
    json.write(resultsJsonFileWriter)
    resultsJsonFileWriter.close()

  def getTestSuiteObject(testResultsFile: File): JSONObject =
    val bufferedSource = Source.fromFile(testResultsFile)
    val xml            = bufferedSource.mkString
    bufferedSource.close
    XML.toJSONObject(xml).getJSONObject("testsuite")

  def readBuildLog(buildLogFilePath: String): String =
    val fileSource = Source.fromFile(buildLogFilePath)
    val rawContent = fileSource.mkString
    fileSource.close
    rawContent

  // log, not xml
  def findErrorsInLog(buildLogFilePath: String): String =
    val rawContent = readBuildLog(buildLogFilePath)
    if rawContent.contains("Error: ") then rawContent else ""

  // Nothing ran, and the build log holds no error this runner recognises: the
  // compiler died in an unexpected way, a suite failed before writing its
  // report, the report folder was empty. Show whatever the build printed so
  // that the student is not left with a bare "error".
  def missingTestResultsMessage(buildLogFilePath: String): String =
    val buildLog = readBuildLog(buildLogFilePath)
    if buildLog.nonEmpty then buildLog
    else
      "No test results were produced, so no test cases can be reported. If your solution compiles and its tests run locally, " +
        "please report this at https://github.com/exercism/scala-test-runner/issues"

  def toTestCaseJSON(testCase: JSONObject): JSONObject =
    val fail = testCase.optJSONObject("failure")
    new JSONObject()
      .put("name", testCase.get("name").toString)
      .put("status", if fail != null then "fail" else "pass")
      .put(
        "message",
        if fail != null then fail.getString("message") else JSONObject.NULL,
      )
      .put("output", JSONObject.NULL)
      .put("test_code", JSONObject.NULL)

  def toExercismJSON(
    buildLogFilePath: String,
    testResultsFiles: List[File],
  ): JSONObject =
    val baseObject   = new JSONObject().put("version", 2)
    val errorMessage = findErrorsInLog(buildLogFilePath)

    if errorMessage.nonEmpty then
      baseObject
        .put("status", "error")
        .put("message", errorMessage)
    else
      val (failuresCount, testCases) = testResultsFiles
        .map(getTestSuiteObject)
        .filter(testSuite => testSuite.has("testcase"))
        .map(testSuite =>
          val failuresCount                = testSuite.getInt("failures")
          val testcase                     = testSuite.get("testcase")
          val testCases: Array[JSONObject] = testcase match
            case arr: JSONArray  =>
              (0 until arr.length)
                .map(idx => toTestCaseJSON(arr.getJSONObject(idx)))
                .toArray
            case obj: JSONObject => Array(toTestCaseJSON(obj))
          (failuresCount, testCases),
        )
        .foldLeft((0, Array.empty[JSONObject]))((a, b) => (a._1 + b._1, Array.concat(a._2, b._2)))
      if testCases.isEmpty then
        baseObject
          .put("status", "error")
          .put("message", missingTestResultsMessage(buildLogFilePath))
      else
        baseObject
          .put("status", if failuresCount > 0 then "fail" else "pass")
          .put("message", JSONObject.NULL)
          .put("tests", testCases)
