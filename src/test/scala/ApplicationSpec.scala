import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class ApplicationSpec extends AnyFlatSpec with Matchers { //extends AnyFlatSpec with Matchers {

  "A successful xml" should "pass simply" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml")
    val jsonArray = Application.convertToJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects should contain only (null)
  }

  "A successful xml" should "be properly formatted as JSON" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml")
    val jsonArray = Application.convertToJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects.filter( _ !== null ).length > 0 // how to write this idiomatically?
  }

  "A failing xml" should "contain a failure object" in {
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml")
    val jsonArray = Application.convertToJSON(xmlTestURL)
    val objects = (0 until jsonArray.length).map( jsonArray.getJSONObject(_).optJSONObject("failure") )
    objects.filter( _ !== null ).length > 0 // how to write this idiomatically?
  }


}
