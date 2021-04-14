  import scala.io.Source
  import org.json.{JSONArray, JSONObject, XML}

  import java.io.{File, FileWriter}

  object Application extends App {
    require(args.length == 3, "Invalid number of arguments. Expected: <build-log-file-path> <test-results-file-path> <results-json-file-path>")
    val buildLogFilePath = args(0)
    val testResultsFilePath = args(1)
    val resultsJsonFilePath = args(2)

    writeResultsJSON(buildLogFilePath, testResultsFilePath, resultsJsonFilePath)

    def writeResultsJSON(buildLogFilePath: String, testResultsFilePath: String, resultsJsonFilePath: String): Unit = {
      val resultsJsonFile = new File(resultsJsonFilePath)
      val resultsJsonFileWriter = new FileWriter(resultsJsonFile)

      val json = toExercismJSON(buildLogFilePath, testResultsFilePath)
      json.write(resultsJsonFileWriter)
      resultsJsonFileWriter.close()
    }

    def getTestSuiteObject(testResultsFilePath: String): JSONObject = {
      val bufferedSource = Source.fromFile(testResultsFilePath)
      val xml = bufferedSource.mkString
      bufferedSource.close
      XML.toJSONObject(xml).getJSONObject("testsuite")
    }

    def getTestCasesJSON(testResultsFilePath: String): JSONArray = {
      getTestSuiteObject(testResultsFilePath).getJSONArray("testcase")
    }

    // log, not xml
    def findErrorsInLog(buildLogFilePath: String): String = {
      val fileSource = Source.fromFile(buildLogFilePath)
      val rawContent = fileSource.mkString
      fileSource.close
      if (rawContent.contains("[error] (Compile / compileIncremental) Compilation failed") ||
          rawContent.contains("[error] (Test / compileIncremental) Compilation failed")) rawContent else ""
    }

    def toTestCaseJSON(testCase: JSONObject): JSONObject = {
      val fail = testCase.optJSONObject("failure")
      new JSONObject()
      .put("name", testCase.getString("name"))
      .put("status", if(fail != null) "fail" else "pass" )
      .put("message", if(fail != null) fail.getString("message") else JSONObject.NULL)
      .put("output", JSONObject.NULL)
      .put("test_code", JSONObject.NULL)
    }

    def toExercismJSON(buildLogFilePath: String, testResultsFilePath: String): JSONObject = {
      val baseObject = new JSONObject().put("version", 2)
      val errorMessage = findErrorsInLog(buildLogFilePath)
      if(!errorMessage.isEmpty) {
        baseObject
        .put("status", "error")
        .put("message", errorMessage)
      } else {
        val testSuite = getTestSuiteObject(testResultsFilePath)
        val failuresNum = testSuite.getInt("failures")
        val testcase = testSuite.get("testcase")
        val testCases: Array[JSONObject] = testcase match {
          case arr: JSONArray => (0 until arr.length).map(idx => toTestCaseJSON(arr.getJSONObject(idx))).toArray
          case obj: JSONObject => Array(toTestCaseJSON(obj))
        }

        baseObject
        .put("status", if(failuresNum > 0) "fail" else "pass")
        .put("message", JSONObject.NULL)
        .put("tests", testCases)
      }
    }
  }
