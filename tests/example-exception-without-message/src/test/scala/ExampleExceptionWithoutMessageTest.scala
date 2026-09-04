import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleExceptionWithoutMessageTest extends AnyFunSuite with Matchers {

  test("a solution that throws without a message still reports a failure") {
    Stub.answer() should be (42)
  }
}
