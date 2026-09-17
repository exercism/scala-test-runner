import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Args, DoNotDiscover}

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

/** Stands in for a student's test file, and is read back as one by the test at the bottom of [[TestSourceSpec]]. */
@DoNotDiscover
class BracedExample extends AnyFunSuite, Matchers:

  test("a test whose own code is read back out of this file") {
    "([{}])".length should be(6)
  }

  ignore("an ignored test, which never starts and so is only ever named once") {
    "([{}])".length should be(6)
  }

class TestSourceSpec extends AnyFunSuite, Matchers:

  /** The code of the tests declared on the given lines, as [[TestSource]] reads them back. */
  def testCode(source: String, declarationLines: Int*): Map[Int, String] =
    TestSource.testCodeByLine(source, declarationLines.toList)

  /** The code of the one test in a source, which every example here declares on line 1. */
  def onlyTestCode(source: String): Option[String] = testCode(source, 1).get(1)

  test("A test's body should be reported without the line declaring it or the indentation it sits at"):
    val source =
      """|  test("year divisible by 400: leap year") {
         |    Leap.leapYear(2000) should be (true)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("Leap.leapYear(2000) should be (true)"))

  test("A body of several lines should keep the shape the student gave it"):
    val source =
      """|  test("counts sheep") {
         |    val counted = Sheep.count(3)
         |
         |    counted should be (3)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("val counted = Sheep.count(3)\n\ncounted should be (3)"))

  // Every exercise ships with all but its first test left `pending`, and the run strips that word out before compiling,
  // so this is the shape most of a submission's tests reach the extractor in.
  test("The blank line left where a pending test was enabled should not be reported"):
    val source =
      """|  test("year divisible by 4, not divisible by 100: leap year") {
         |    
         |    Leap.leapYear(1996) should be (true)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("Leap.leapYear(1996) should be (true)"))

  test("A brace written inside a string should not be taken for the end of the body"):
    val source =
      """|  test("paired and nested square brackets") {
         |    MatchingBrackets.isPaired("([{}])") should be (true)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("""MatchingBrackets.isPaired("([{}])") should be (true)"""))

  test("A brace written inside a triple quoted string should not end the body either"):
    val quotes = "\"\"\""
    val source =
      s"""|  test("reads a block") {
          |    Parser.parse($quotes{ a block }$quotes) should be (Block("a block"))
          |  }
          |""".stripMargin

    onlyTestCode(source) should be(Some(s"""Parser.parse($quotes{ a block }$quotes) should be (Block("a block"))"""))

  // `sgf-parsing` names a test with a string that ends in a quote, which closes on the fourth quote of the run rather
  // than the third.
  test("A triple quoted string ending in a quote should not leave one behind to open another"):
    val quotes = "\"\"\""
    // The declaration this builds reads: test("""parse "(;A[b])"""") {
    val source =
      s"""  test(${quotes}parse "(;A[b])"$quotes) {
         |    Sgf.parseSgf("(;A[b])") should be (Some(Node(Map("A" -> List("b")))))
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("""Sgf.parseSgf("(;A[b])") should be (Some(Node(Map("A" -> List("b")))))"""))

  test("A brace written as a character should not end the body"):
    val source =
      """|  test("the opening brace") {
         |    Brackets.opening should be ('{')
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("""Brackets.opening should be ('{')"""))

  test("A brace written inside a comment should not end the body"):
    val source =
      """|  test("a commented brace") {
         |    // the } below is prose
         |    /* and this } is /* nested */ prose */
         |    Brackets.opening should be ('{')
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(
      Some("""|// the } below is prose
              |/* and this } is /* nested */ prose */
              |Brackets.opening should be ('{')""".stripMargin),
    )

  test("Blocks written inside the body should be reported with it, not cut short at the first closing brace"):
    val source =
      """|  test("nested blocks") {
         |    val increment = (x: Int) => { x + 1 }
         |    increment(1) should be (2)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("val increment = (x: Int) => { x + 1 }\nincrement(1) should be (2)"))

  test("A declaration spread over several lines should still find its body"):
    val source =
      """|  test(
         |    "declared across several lines"
         |  ) {
         |    Leap.leapYear(2000) should be (true)
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("Leap.leapYear(2000) should be (true)"))

  test("A body given as an indented block rather than in braces should be reported too"):
    val source =
      """|  test("an indented block"):
         |    val counted = Sheep.count(3)
         |    counted should be (3)
         |
         |  test("the test below it") {
         |    Sheep.count(0) should be (0)
         |  }
         |""".stripMargin

    testCode(source, 1, 5) should be(
      Map(
        1 -> "val counted = Sheep.count(3)\ncounted should be (3)",
        5 -> "Sheep.count(0) should be (0)",
      ),
    )

  test("A test that opens no block at all should be reported without code"):
    val source =
      """|  test("given its body as an argument")(Leap.leapYear(2000) should be (true))
         |
         |  test("the test below it") {
         |    Leap.leapYear(1900) should be (false)
         |  }
         |""".stripMargin

    testCode(source, 1, 3) should be(Map(3 -> "Leap.leapYear(1900) should be (false)"))

  test("A test that opens no block should not be given one opened by whatever follows it"):
    val source =
      """|  test("given its body as an argument")(Leap.leapYear(2000) should be (true))
         |
         |  private def twice(year: Int) = {
         |    Leap.leapYear(year) && Leap.leapYear(year)
         |  }
         |
         |  test("the test below it") {
         |    Leap.leapYear(1900) should be (false)
         |  }
         |""".stripMargin

    testCode(source, 1, 7) should be(Map(7 -> "Leap.leapYear(1900) should be (false)"))

  // The brace the suite itself closes with would otherwise let an unclosed body balance, and swallow every test
  // declared between the two.
  test("A body that is never closed should not be reported as the test below it"):
    val source =
      """|  test("an unclosed body") {
         |    Leap.leapYear(2000) should be (true)
         |
         |  test("the test below it") {
         |    Leap.leapYear(1900) should be (false)
         |  }
         |}
         |""".stripMargin

    testCode(source, 1, 4) should be(Map(4 -> "Leap.leapYear(1900) should be (false)"))

  test("An escaped quote should not leave the closing one behind to open something else"):
    val source =
      """|  test("the quote character") {
         |    Brackets.quotes should be (List('\'','}'))
         |  }
         |""".stripMargin

    onlyTestCode(source) should be(Some("""Brackets.quotes should be (List('\'','}'))"""))

  test("A body that is never closed should be reported as no code rather than as the rest of the file"):
    val source =
      """|  test("an unclosed body") {
         |    Leap.leapYear(2000) should be (true)
         |""".stripMargin

    onlyTestCode(source) should be(None)

  test("A test with nothing in its body should be reported without code"):
    onlyTestCode("""  test("an empty body") { }""") should be(None)

  test("Each ScalaTest style the track writes its exercises in should be read the same way"):
    val flatSpec =
      """|  it should "keep everything" in {
         |    Strain.keep(List(1, 2, 3), _ => true) should be (List(1, 2, 3))
         |  }
         |""".stripMargin
    val funSpec  =
      """|  describe("a robot") {
         |    it("has a name") {
         |      new Robot().name should fullyMatch regex nameRegex
         |    }
         |  }
         |""".stripMargin

    onlyTestCode(flatSpec) should be(Some("Strain.keep(List(1, 2, 3), _ => true) should be (List(1, 2, 3))"))
    testCode(funSpec, 2).get(2) should be(Some("new Robot().name should fullyMatch regex nameRegex"))

  test("A test declared below another should be read from its own declaration, not the one above"):
    val source =
      """|  test("the first") {
         |    Leap.leapYear(2015) should be (false)
         |  }
         |
         |  test("the second") {
         |    Leap.leapYear(1996) should be (true)
         |  }
         |""".stripMargin

    testCode(source, 1, 5) should be(
      Map(
        1 -> "Leap.leapYear(2015) should be (false)",
        5 -> "Leap.leapYear(1996) should be (true)",
      ),
    )

  test("An outcome should be given the code of the test file it names"):
    val folder = Files.createTempDirectory("test-sources").toFile
    folder.deleteOnExit()
    Files.write(
      new File(folder, "LeapTest.scala").toPath,
      """|  test("year divisible by 400: leap year") {
         |    Leap.leapYear(2000) should be (true)
         |  }
         |""".stripMargin.getBytes(UTF_8),
    )

    val outcomes = List(
      TestOutcome("year divisible by 400: leap year", "pass", None, None, Some(TestLocation("LeapTest.scala", 1))),
      TestOutcome("a test from a file that is not there", "pass", None, None, Some(TestLocation("Gone.scala", 1))),
      TestOutcome("a test with nowhere to look", "error", Some("it died"), None),
    )

    TestSource.addTestCode(outcomes, folder).map(_.testCode) should be(
      List(Some("Leap.leapYear(2000) should be (true)"), None, None),
    )

  test("The code of a test that ran should be read back out of the file declaring it"):
    val collector = new TestRun.OutcomeCollector
    new BracedExample().run(None, Args(collector)).waitUntilCompleted()

    val outcomes = TestSource.addTestCode(collector.outcomes, new File("src/test/scala"))

    // Both are declared above, with the same body, and both have to be located: an ignored test reports no start, so
    // its declaration is only ever named by the event saying it was skipped.
    outcomes.map(_.name) should be(
      List(
        "a test whose own code is read back out of this file",
        "an ignored test, which never starts and so is only ever named once",
      ),
    )
    outcomes.map(_.testCode).distinct should be(List(Some("\"([{}])\".length should be(6)")))
