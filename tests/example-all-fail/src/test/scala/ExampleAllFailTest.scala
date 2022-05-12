import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version 1.3.0 */
class ExampleAllFailTest extends AnyFunSuite with Matchers {

  test("year not divisible by 4: common year") {
    Leap.leapYear(2015) should be (false)
  }

  test("year divisible by 4, not divisible by 100: leap year") {
    pending
    Leap.leapYear(1996) should be (true)
  }
}