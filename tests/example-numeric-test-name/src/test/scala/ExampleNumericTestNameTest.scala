import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version 1.3.0 */
class ExampleNumericTestNameTest extends AnyFunSuite with Matchers {
  test("1") {
    true should be (true)
  }
}