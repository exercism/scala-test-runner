import scala.util.parsing.combinator.RegexParsers

/** A trimmed-down SgfParsing solution.
  *
  * Its purpose here is to exercise scala-parser-combinators, which the test
  * runner has to provide on the classpath: bin/run.sh compiles a solution
  * against the assembly jar only and never reads the exercise's own build.sbt.
  */
object ExampleSgfParsing extends RegexParsers {

  case class Tree(nodes: List[Map[String, List[String]]], children: List[Tree])

  private def key: Parser[String] = """[A-Z]+""".r

  private def value: Parser[String] = "[" ~> """[^\]]*""".r <~ "]"

  private def property: Parser[(String, List[String])] =
    key ~ rep1(value) ^^ { case k ~ vs => k -> vs }

  private def node: Parser[Map[String, List[String]]] =
    ";" ~> rep(property) ^^ (_.toMap)

  private def tree: Parser[Tree] =
    "(" ~> rep1(node) ~ rep(tree) <~ ")" ^^ { case ns ~ cs => Tree(ns, cs) }

  def parse(input: String): Option[Tree] =
    parseAll(tree, input) match {
      case Success(result, _) => Some(result)
      case _                  => None
    }
}
