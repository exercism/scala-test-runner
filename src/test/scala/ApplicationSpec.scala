import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import org.json.JSONObject

class ApplicationSpec extends AnyFlatSpec with Matchers { //extends AnyFlatSpec with Matchers {

  "A successful xml" should "pass simply" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml")
    val jsonArray = Application.convertToJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects should contain only (null)
  }

  "A successful xml" should "be properly formatted as JSON" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml")
    val outputFileURL = getClass.getResource("/outputs/output.txt")
    val exercismOutput: JSONObject = Application.toExercismJSON(xmlTestURL, outputFileURL)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "pass")
    assert(exercismOutput.getString("message") == "")
    assert(exercismOutput.get("tests").asInstanceOf[Array[JSONObject]].length == 7)
  }

  "A failing xml" should "contain a failure object" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml")
    val jsonArray = Application.convertToJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects.filter( _ !== null ).length > 0 // how to write this idiomatically?
  }

  "A failing xml" should "be properly formatted as JSON" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml")
    val outputFileURL = getClass.getResource("/outputs/output_fail.txt")
    val exercismOutput: JSONObject = Application.toExercismJSON(xmlTestURL, outputFileURL)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "fail")
    assert(exercismOutput.getString("message") == "")

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 7)
    val failedTest = testCases(2)
    assert(failedTest.getString("status") == "fail")
    assert(failedTest.getString("message") == """TreeMap(2 -> List("James", "Blair2", "Paul")) was not equal to Map(2 -> List("James", "Blair", "Paul"))""")
  }

}
