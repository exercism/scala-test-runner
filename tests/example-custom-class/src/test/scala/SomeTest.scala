import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version 1.3.0 */
class SomeTest extends AnyFunSuite with Matchers {
  test("some test") {
    true should be (true)
  }
}