import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ExampleSgfParsing._

/** @version created manually **/
class ExampleSgfParsingTest extends AnyFunSuite with Matchers {

  test("a single node with one property") {
    parse("(;FF[4])") should be (Some(Tree(List(Map("FF" -> List("4"))), Nil)))
  }

  test("a property with multiple values") {
    pending
    parse("(;AB[aa][bb])") should be (Some(Tree(List(Map("AB" -> List("aa", "bb"))), Nil)))
  }

  test("a value may contain whitespace") {
    pending
    parse("(;C[hello world])") should be (Some(Tree(List(Map("C" -> List("hello world"))), Nil)))
  }

  test("a sequence of nodes") {
    pending
    parse("(;FF[4];C[root])").map(_.nodes.size) should be (Some(2))
  }

  test("nested child trees") {
    pending
    parse("(;FF[4](;C[a])(;C[b]))").map(_.children.size) should be (Some(2))
  }

  test("a tree without enclosing parens is rejected") {
    pending
    parse(";FF[4]") should be (None)
  }
}
