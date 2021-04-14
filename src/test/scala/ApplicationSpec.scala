import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import org.json.JSONObject

import java.io.File

class ApplicationSpec extends AnyFlatSpec with Matchers {

  "A successful xml" should "pass simply" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml").getPath
    val jsonArray = Application.getTestCasesJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects should contain only (null)
  }

  "A successful xml" should "be properly formatted as JSON" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml").getPath
    val outputFileURL = getClass.getResource("/outputs/output.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, xmlTestURL)

    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "pass")
    assert(exercismOutput.opt("message") == null)

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 7)
    assert(testCases(0).toString() == """{"output":"TOIMPLEMENT","name":"empty school","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(1).toString() == """{"output":"TOIMPLEMENT","name":"add student","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(2).toString() == """{"output":"TOIMPLEMENT","name":"add more students in same class","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(3).toString() == """{"output":"TOIMPLEMENT","name":"add students to different grades","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(4).toString() == """{"output":"TOIMPLEMENT","name":"get students in a grade","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(5).toString() == """{"output":"TOIMPLEMENT","name":"get students in a non-existent grade","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(6).toString() == """{"output":"TOIMPLEMENT","name":"sort school","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
  }

  "A failing xml" should "contain a failure object" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml").getPath
    val jsonArray = Application.getTestCasesJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects.filter( _ !== null ).length > 0
  }

  "A failing xml" should "be properly formatted as JSON" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml").getPath
    val outputFileURL = getClass.getResource("/outputs/output_fail.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, xmlTestURL)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "fail")
    assert(exercismOutput.opt("message") == null)

    val testCases: Array[JSONObject] = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 7)
    assert(testCases(0).toString() == """{"output":"TOIMPLEMENT","name":"empty school","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(1).toString() == """{"output":"TOIMPLEMENT","name":"add student","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")

    val failedTest = testCases(2)
    assert(failedTest.getString("status") == "fail")
    assert(failedTest.getString("message") == """TreeMap(2 -> List("James", "Blair2", "Paul")) was not equal to Map(2 -> List("James", "Blair", "Paul"))""")

    assert(testCases(3).toString() == """{"output":"TOIMPLEMENT","name":"add students to different grades","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(4).toString() == """{"output":"TOIMPLEMENT","name":"get students in a grade","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(5).toString() == """{"output":"TOIMPLEMENT","name":"get students in a non-existent grade","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")
    assert(testCases(6).toString() == """{"output":"TOIMPLEMENT","name":"sort school","test_code":"TOIMPLEMENT","message":null,"status":"pass"}""")

  }

  "An xml with a syntax error" should "be properly reported as JSON" in {
    val outputFileURL = getClass.getResource("/outputs/output_error.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, null)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
  }

  "An xml with a syntax error due to an empty file" should "be properly reported as JSON" in {
    val outputFileURL = getClass.getResource("/outputs/output_empty.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, null)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
  }
}
