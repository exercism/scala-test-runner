import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleTestThatDiesTest extends AnyFunSuite with Matchers {

  test("a test that passes before the trouble starts") {
    Sheep.name should be ("Dolly")
  }

  test("a test that overflows the stack") {
    Sheep.count(0) should be (3)
  }
}
