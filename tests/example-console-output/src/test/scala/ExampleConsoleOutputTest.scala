import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleConsoleOutputTest extends AnyFunSuite with Matchers {

  test("what a passing test prints is reported") {
    println("counted 3 sheep")
    Sheep.count(3) should be (3)
  }

  test("what a failing test prints is reported") {
    pending
    println("about to miscount")
    Sheep.count(2) should be (3)
  }

  test("a test that prints nothing has no output") {
    pending
    Sheep.count(1) should be (1)
  }

  test("output too long for the interface is truncated") {
    pending
    println("sheep" * 120)
    Sheep.count(0) should be (0)
  }
}
