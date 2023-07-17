import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExampleMultipleFilesTest extends AnyFunSuite with Matchers {
  test("two-one") {
    true should be (true)
  }
}