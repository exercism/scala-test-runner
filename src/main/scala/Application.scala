
import scala.io.Source
import org.json.{XML, JSONObject, JSONArray}
import java.net.URL

object Application extends App {

  def getTestSuiteObject(filepath: URL): JSONObject = {
    val xml = Source.fromURL(filepath).mkString
    XML.toJSONObject(xml).getJSONObject("testsuite")
  }

  def convertToJSON(filepath: URL): JSONArray = {
    getTestSuiteObject(filepath).getJSONArray("testcase")
  }

  // log, not xml
  def findErrorsInLog(filepath: URL): String = {
    val fileSource = Source.fromURL(filepath)
    val content = fileSource.mkString
    if (fileSource.mkString.contains("[error]")) content else ""
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
      // println(failuresNum)
      val testCasesArray = testSuite.getJSONArray("testcase")
      val testCases: Array[JSONObject] = (0 until testCasesArray.length).toArray.map(idx => {
        val o = testCasesArray.getJSONObject(idx)
        val fail = o.optJSONObject("failure")
        if(fail != null) println(fail.getString("message"))
        new JSONObject()
        .put("name", o.getString("name"))
        .put("status", if(fail != null) "fail" else "pass" )
        .put("message", if(fail != null) fail.getString("message") else "")
        .put("output", "TOIMPLEMENT")
        .put("test_code", "TOIMPLEMENT")
      })

      baseObject
      .put("status", if(failuresNum > 0) "fail" else "pass")
      .put("message", "")
      .put("tests", testCases)
    }
  }
}
