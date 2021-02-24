import scala.io.Source
import org.json.{XML, JSONObject, JSONArray}
import java.net.URL

object Application extends App {

  def getTestSuiteObject(filepath: URL): JSONObject = {
    val bufferedSource = Source.fromURL(filepath)
    val xml = bufferedSource.mkString
    bufferedSource.close
    XML.toJSONObject(xml).getJSONObject("testsuite")
  }

  def getTestCasesJSON(filepath: URL): JSONArray = {
    getTestSuiteObject(filepath).getJSONArray("testcase")
  }

  // log, not xml
  def findErrorsInLog(logFilePath: URL): String = {
    val fileSource = Source.fromURL(logFilePath)
    val rawContent = fileSource.mkString
    fileSource.close
    if (rawContent.contains("[error] (Test / compileIncremental) Compilation failed")) rawContent else ""
  }

  def toExercismJSON(filepath: URL, logFilePath: URL): JSONObject = {
    val baseObject = new JSONObject().put("version", 2)
    val errorMessage = findErrorsInLog(logFilePath)
    if(!errorMessage.isEmpty) {
      baseObject
      .put("status", "error")
      .put("message", errorMessage)
    } else {
      val testSuite = getTestSuiteObject(filepath)
      val failuresNum = testSuite.getInt("failures")
      val testCasesArray = testSuite.getJSONArray("testcase")

      val testCases: Array[JSONObject] = (0 until testCasesArray.length).toArray.map(idx => {
        val o = testCasesArray.getJSONObject(idx)
        val fail = o.optJSONObject("failure")
        new JSONObject()
        .put("name", o.getString("name"))
        .put("status", if(fail != null) "fail" else "pass" )
        .put("message", if(fail != null) fail.getString("message") else JSONObject.NULL)
        .put("output", "TOIMPLEMENT")
        .put("test_code", "TOIMPLEMENT")
      })

      baseObject
      .put("status", if(failuresNum > 0) "fail" else "pass")
      .put("message", JSONObject.NULL)
      .put("tests", testCases)
    }
  }
}
