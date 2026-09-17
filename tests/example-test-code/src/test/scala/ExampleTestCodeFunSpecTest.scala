import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleTestCodeFunSpecTest extends AnyFunSpec with Matchers {

  describe("a string of brackets") {
    it("is paired when it holds none at all") {
      Brackets.isPaired("") should be (true)
    }
  }
}
