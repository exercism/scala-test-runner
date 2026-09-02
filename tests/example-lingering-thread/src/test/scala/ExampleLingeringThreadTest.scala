import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleLingeringThreadTest extends AnyFunSuite with Matchers {

  test("a passing test that leaves a thread running behind it") {
    Sheep.count(3) should be (3)
  }
}
