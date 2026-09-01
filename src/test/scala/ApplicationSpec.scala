import org.json.{JSONArray, JSONObject}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.io.File

class ApplicationSpec extends AnyFunSuite, Matchers:

  def getTestCasesJSON(path: String): JSONArray =
    val file = new File(path)
    Application.getTestSuiteObject(file).getJSONArray("testcase")

  test("A successful xml should pass simply"):
    val xmlTestURL = getClass.getResource("/GradeSchool_successful.xml").getPath
    val jsonArray  = getTestCasesJSON(xmlTestURL)
    val objects    = (0 until jsonArray.length).map(jsonArray.getJSONObject(_).optJSONObject("failure"))
    objects should contain only null

  test("A successful xml should be properly formatted as JSON"):
    val xmlTestURL                 = getClass.getResource("/GradeSchool_successful.xml").getPath
    val outputFileURL              = getClass.getResource("/outputs/output.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, List(new File(xmlTestURL)))

    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "pass")
    assert(exercismOutput.opt("message") == null)

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 7)
    assert(testCases(0).toString() == """{"output":null,"name":"empty school","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(1).toString() == """{"output":null,"name":"add student","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(2).toString() == """{"output":null,"name":"add more students in same class","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(3).toString() == """{"output":null,"name":"add students to different grades","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(4).toString() == """{"output":null,"name":"get students in a grade","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(5).toString() == """{"output":null,"name":"get students in a non-existent grade","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(6).toString() == """{"output":null,"name":"sort school","test_code":null,"message":null,"status":"pass"}""")

  test("A successful xml with a single test case should be properly formatted as JSON"):
    val xmlTestURL                 = getClass.getResource("/HelloWorld_successful.xml").getPath
    val outputFileURL              = getClass.getResource("/outputs/output.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, List(new File(xmlTestURL)))

    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "pass")
    assert(exercismOutput.opt("message") == null)

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 1)
    assert(testCases(0).toString() == """{"output":null,"name":"Say Hi!","test_code":null,"message":null,"status":"pass"}""")

  test("A failing xml should contain a failure object"):
    val xmlTestURL = getClass.getResource("/GradeSchool_failure.xml").getFile
    val jsonArray  = getTestCasesJSON(xmlTestURL)
    val objects    = (0 until jsonArray.length).map(jsonArray.getJSONObject(_).optJSONObject("failure"))
    objects.exists(_ !== null)

  test("A failing xml should be properly formatted as JSON"):
    val xmlTestURL                 = getClass.getResource("/GradeSchool_failure.xml").getPath
    val outputFileURL              = getClass.getResource("/outputs/output_fail.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, List(new File(xmlTestURL)))
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "fail")
    assert(exercismOutput.opt("message") == null)

    val testCases: Array[JSONObject] = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 7)
    assert(testCases(0).toString() == """{"output":null,"name":"empty school","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(1).toString() == """{"output":null,"name":"add student","test_code":null,"message":null,"status":"pass"}""")

    val failedTest = testCases(2)
    assert(failedTest.getString("status") == "fail")
    assert(failedTest.getString("message") == """TreeMap(2 -> List("James", "Blair2", "Paul")) was not equal to Map(2 -> List("James", "Blair", "Paul"))""")

    assert(testCases(3).toString() == """{"output":null,"name":"add students to different grades","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(4).toString() == """{"output":null,"name":"get students in a grade","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(5).toString() == """{"output":null,"name":"get students in a non-existent grade","test_code":null,"message":null,"status":"pass"}""")
    assert(testCases(6).toString() == """{"output":null,"name":"sort school","test_code":null,"message":null,"status":"pass"}""")

  test("A build log with a syntax error should be properly reported as JSON"):
    val outputFileURL              = getClass.getResource("/outputs/output_error.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, Nil)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("Syntax Error: "))

  test("A build log with a compile error caused by an empty solution should be properly reported as JSON"):
    val outputFileURL              = getClass.getResource("/outputs/output_empty.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, Nil)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("Not Found Error: "))

  test("No test reports and no errors in the build log should be reported as an error"):
    val outputFileURL              = getClass.getResource("/outputs/output_no_errors.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, Nil)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("No test results were produced"))

  test("A test report without a single test case should be reported as an error"):
    val xmlTestURL                 = getClass.getResource("/GradeSchool_no_test_cases.xml").getPath
    val outputFileURL              = getClass.getResource("/outputs/output.txt").getPath
    val exercismOutput: JSONObject = Application.toExercismJSON(outputFileURL, List(new File(xmlTestURL)))
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message") == Application.readBuildLog(outputFileURL))
