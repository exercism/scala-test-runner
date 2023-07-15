import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version 1.3.0 */
class ExampleMultipleTestsTest extends AnyFunSuite with Matchers {
  test("one-one") {
    true should be (true)
  }
}

class ExampleAnotherMultipleTestsTest extends AnyFunSuite with Matchers {
  test("one-two") {
    true should be (true)
  }
}