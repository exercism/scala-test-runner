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

  // log, not xml
  def findErrorsInLog(buildLogFilePath: String): String =
    val fileSource = Source.fromFile(buildLogFilePath)
    val rawContent = fileSource.mkString
    fileSource.close
    if rawContent.contains("Error: ") then rawContent else ""

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

    if errorMessage.nonEmpty || testResultsFiles.isEmpty then
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
        .reduce((a, b) => (a._1 + b._1, Array.concat(a._2, b._2)))
      baseObject
        .put("status", if failuresCount > 0 then "fail" else "pass")
        .put("message", JSONObject.NULL)
        .put("tests", testCases)
