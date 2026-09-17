import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleTestCodeFunSuiteTest extends AnyFunSuite with Matchers {

  test("a brace in the code a test ran is reported with it") {
    Brackets.isPaired("([{}])") should be (true)
  }

  test("so is a brace the test only mentions") {
    // a } here, and a { there
    Brackets.isPaired("{ ]") should be (false)
  }
}
