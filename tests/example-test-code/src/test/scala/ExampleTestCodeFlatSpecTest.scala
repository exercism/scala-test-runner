import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** @version created manually **/
class ExampleTestCodeFlatSpecTest extends AnyFlatSpec with Matchers {

  it should "report the code of a test written as a flat spec" in {
    Brackets.isPaired("[]") should be (true)
  }
}
