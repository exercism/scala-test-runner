import org.json.JSONObject
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ApplicationSpec extends AnyFunSuite, Matchers:

  // The path the runner would be given if TestRun died before writing anything.
  val missingTestResults = "/no/such/test-results.json"

  def resource(path: String): String = getClass.getResource(path).getPath

  test("Recorded outcomes should be read back in the order they were recorded"):
    val testResults = Application.readTestResults(resource("/GradeSchool_successful.json"))
    testResults.map(_.getString("name")) should contain theSameElementsInOrderAs List(
      "empty school",
      "add student",
      "add more students in same class",
      "add students to different grades",
      "get students in a grade",
      "get students in a non-existent grade",
      "sort school",
    )

  test("Passing outcomes should be properly formatted as JSON"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output.txt"), resource("/GradeSchool_successful.json"))

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

  test("A single passing outcome should be properly formatted as JSON"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output.txt"), resource("/HelloWorld_successful.json"))

    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "pass")
    assert(exercismOutput.opt("message") == null)

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 1)
    assert(testCases(0).toString() == """{"output":null,"name":"Say Hi!","test_code":null,"message":null,"status":"pass"}""")

  test("A failing outcome should be properly formatted as JSON"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output_fail.txt"), resource("/GradeSchool_failure.json"))

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

  test("What a test printed should be reported as its output"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output_fail.txt"), resource("/GradeSchool_with_output.json"))

    val testCases = exercismOutput.get("tests").asInstanceOf[Array[JSONObject]]
    assert(testCases.length == 2)
    assert(testCases(0).getString("output") == "school is empty\n")
    assert(testCases(1).getString("output") == "added Aimee to grade 2\n")
    assert(testCases(1).getString("message") == "1 was not 2")

  test("A build log with a syntax error should be properly reported as JSON"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output_error.txt"), missingTestResults)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("Syntax Error: "))

  test("A build log with a compile error caused by an empty solution should be properly reported as JSON"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output_empty.txt"), missingTestResults)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("Not Found Error: "))

  test("No test results and no errors in the build log should be reported as an error"):
    val exercismOutput: JSONObject = Application.toExercismJSON(resource("/outputs/output_no_errors.txt"), missingTestResults)
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("No test results were produced"))

  test("Test results without a single outcome should be reported as an error"):
    val buildLog                   = resource("/outputs/output.txt")
    val exercismOutput: JSONObject = Application.toExercismJSON(buildLog, resource("/no_test_results.json"))
    assert(exercismOutput.getInt("version") == 2)
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message") == Application.readBuildLog(buildLog))

  test("Test results left half-written should be reported as an error"):
    val buildLog                   = resource("/outputs/output_no_errors.txt")
    val exercismOutput: JSONObject = Application.toExercismJSON(buildLog, resource("/truncated_test_results.json"))
    assert(exercismOutput.getString("status") == "error")
    assert(exercismOutput.getString("message").contains("No test results were produced"))
