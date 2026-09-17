import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.tailrec
import scala.io.Source
import scala.util.Using

/** Where a test is declared: the file holding it, and the line its declaration starts on. */
case class TestLocation(fileName: String, lineNumber: Int)

/** Reads the code a test ran out of the test file it was declared in.
  *
  * The interface asks every test for the `test_code` that checked the behaviour. Looking that up by test name would
  * mean reimplementing how each ScalaTest style builds one - `AnyFlatSpec` composes a subject with a verb, `AnyFunSpec`
  * joins nested describes - so this goes by where the test was declared instead, which ScalaTest reports the same way
  * whichever style wrote it.
  *
  * A location carries a line and nothing finer: no column, and no file path unless scalactic was built with
  * `SCALACTIC_FILL_FILE_PATHNAMES`, which it is not. A declaration is therefore read from the start of its line up to
  * the start of the next test's line in the same file, and files are found by name under the folder holding the
  * solution's tests. Bounding each declaration that way keeps one this cannot make sense of from swallowing the test
  * below it: it reports no code at all instead.
  */
object TestSource:

  /** Fills in the code of every test this can find the body of, and leaves the rest reporting none. */
  def addTestCode(outcomes: List[TestOutcome], testSourcesFolder: File): List[TestOutcome] =
    val testCode = outcomes
      .flatMap(_.location)
      .groupBy(_.fileName)
      .flatMap: (fileName, locations) =>
        readSource(new File(testSourcesFolder, fileName)) match
          case None         => Map.empty[TestLocation, String]
          case Some(source) =>
            testCodeByLine(source, locations.map(_.lineNumber))
              .map((lineNumber, code) => TestLocation(fileName, lineNumber) -> code)
    outcomes.map(outcome => outcome.copy(testCode = outcome.location.flatMap(testCode.get)))

  /** A test file's contents, or nothing when it cannot be read.
    *
    * Reporting no code costs a student a detail on the results page; letting the failure out would cost them the
    * results entirely, so every way this can go wrong - a file the compiler saw but the run cannot, bytes that are not
    * UTF-8 - ends the same way.
    */
  private def readSource(file: File): Option[String] =
    Option.when(file.isFile)(Using(Source.fromFile(file, UTF_8.name))(_.mkString).toOption).flatten

  /** The code of each test declared on the given lines, keyed by line, skipping the ones with no body to report. */
  def testCodeByLine(source: String, declarationLines: List[Int]): Map[Int, String] =
    val starts       = lineStarts(source)
    // In the order they are written, so that each declaration knows where the next one begins. A line can be given
    // twice, by tests a loop declared from the one call.
    val lines        = declarationLines.distinct.sorted
    val declarations = lines.map(lineStart(starts, _))
    // A declaration runs until the next one begins. The last of them is free to run to the end of the file: what
    // follows a test is the rest of its suite, and a body that reached there never closed in the first place.
    val regionEnds   = declarations.drop(1) :+ source.length
    lines
      .lazyZip(declarations)
      .lazyZip(regionEnds)
      .flatMap((lineNumber, from, until) => testCodeIn(source, from, until).map(lineNumber -> _))
      .toMap

  /** The body of the test whose declaration starts at `from`, as the student wrote it, indentation removed. */
  def testCodeIn(source: String, from: Int, until: Int): Option[String] =
    blockOpener(source, from, until)
      .flatMap: opener =>
        if source.charAt(opener) == '{' then bracedBody(source, opener)
        else indentedBody(source, from, opener)
      .map(dedent)
      .filter(_.nonEmpty)

  /** Where the test's body begins: the brace of `test("...") { ... }`, or the colon Scala 3 opens a block with.
    *
    * Whichever comes first within the declaration wins. A test given its body some other way - as an ordinary argument,
    * or by a helper method - opens no block here, and is reported without code.
    */
  @tailrec
  private def blockOpener(source: String, index: Int, until: Int): Option[Int] =
    if index >= until then None
    else
      val afterOpaque = skipOpaque(source, index)
      if afterOpaque > index then blockOpener(source, afterOpaque, until)
      else if source.charAt(index) == '{' || opensIndentedBlock(source, index) then Some(index)
      else blockOpener(source, index + 1, until)

  /** Whether the colon at `index` is the one that opens an indented block, rather than part of anything else. */
  private def opensIndentedBlock(source: String, index: Int): Boolean =
    source.charAt(index) == ':' && (index + 1 until endOfLine(source, index)).forall(source.charAt(_).isWhitespace)

  /** The text between the brace at `open` and the one closing it, or nothing when the source never closes it. */
  private def bracedBody(source: String, open: Int): Option[String] =
    @tailrec
    def closingBrace(index: Int, depth: Int): Option[Int] =
      if index >= source.length then None
      else
        val afterOpaque = skipOpaque(source, index)
        if afterOpaque > index then closingBrace(afterOpaque, depth)
        else
          source.charAt(index) match
            case '{'               => closingBrace(index + 1, depth + 1)
            case '}' if depth == 1 => Some(index)
            case '}'               => closingBrace(index + 1, depth - 1)
            case _                 => closingBrace(index + 1, depth)

    closingBrace(open + 1, 1).map(source.substring(open + 1, _))

  /** The lines below the colon at `opener` that the declaration starting at `from` indented further than itself. */
  private def indentedBody(source: String, from: Int, opener: Int): Option[String] =
    val declarationIndent = indentOf(source.substring(from).takeWhile(_ != '\n'))
    val afterColonLine    = source.indexOf('\n', opener)
    Option.when(afterColonLine >= 0):
      source
        .substring(afterColonLine + 1)
        .linesIterator
        .takeWhile(line => line.isBlank || indentOf(line) > declarationIndent)
        .mkString("\n")

  /** A body as it should be reported: trailing spaces, surrounding blank lines and shared indentation all gone.
    *
    * The interface's example of `test_code` is a test's body on its own, without the line declaring it and without the
    * indentation that line put it at. A test the student left `pending` reaches here with a blank line where the run
    * stripped that word out, which is what makes dropping leading blank lines worth doing.
    */
  private def dedent(body: String): String =
    val lines  = body.linesIterator.map(_.stripTrailing).toList.dropWhile(_.isEmpty).reverse.dropWhile(_.isEmpty).reverse
    val indent = lines.filter(_.nonEmpty).map(indentOf).minOption.getOrElse(0)
    lines.map(line => line.drop(indent)).mkString("\n")

  private def indentOf(line: String): Int = line.length - line.stripLeading.length

  /** Advances past a comment, a string or a character literal beginning at `index`, and stays put for anything else.
    *
    * Braces are written inside all three - `isPaired("([{}])")`, as `matching-brackets` has students check - and
    * counting those as code would end a test's body in the wrong place, or fail to find its end at all.
    */
  private def skipOpaque(source: String, index: Int): Int =
    if source.startsWith("//", index) then endOfLine(source, index)
    else if source.startsWith("/*", index) then endOfComment(source, index + 2, 1)
    else if source.startsWith("\"\"\"", index) then endOfTripleQuoted(source, index + 3)
    else if source.charAt(index) == '"' then endOfString(source, index + 1)
    else if source.charAt(index) == '\'' then endOfCharLiteral(source, index)
    else index

  private def endOfLine(source: String, index: Int): Int =
    source.indexOf('\n', index) match
      case -1      => source.length
      case newline => newline

  // Scala nests block comments, so a `/*` inside one has to be counted rather than ignored.
  @tailrec
  private def endOfComment(source: String, index: Int, depth: Int): Int =
    if index >= source.length then source.length
    else if source.startsWith("/*", index) then endOfComment(source, index + 2, depth + 1)
    else if source.startsWith("*/", index) then
      if depth == 1 then index + 2 else endOfComment(source, index + 2, depth - 1)
    else endOfComment(source, index + 1, depth)

  @tailrec
  private def endOfTripleQuoted(source: String, index: Int): Int =
    if index >= source.length then source.length
    else if source.startsWith("\"\"\"", index) then endOfQuotes(source, index + 3)
    else endOfTripleQuoted(source, index + 1)

  /** The end of a run of quotes.
    *
    * A triple quoted string closes on the last quote of a run of three or more, so `\"\"\"a\"\"\"\"` is a string ending in a
    * quote rather than a string with a stray quote after it. `sgf-parsing` names a test exactly that way, and stopping
    * at the first three would leave that last quote opening a string that was never there.
    */
  @tailrec
  private def endOfQuotes(source: String, index: Int): Int =
    if index < source.length && source.charAt(index) == '"' then endOfQuotes(source, index + 1) else index

  /** The end of an ordinary string literal, which is also the end of the line when the quote opened no literal at all.
    *
    * Treating an unclosed quote as a quoted character and no more keeps a single stray one - inside a comment this
    * reads as code, say - from turning the rest of the file into one long string.
    */
  @tailrec
  private def endOfString(source: String, index: Int): Int =
    if index >= source.length || source.charAt(index) == '\n' then index
    else if source.charAt(index) == '\\' then endOfString(source, index + 2)
    else if source.charAt(index) == '"' then index + 1
    else endOfString(source, index + 1)

  /** The end of a character literal: `'a'`, or an escape such as `'\n'`. A quote opening neither is left where it is,
    * since Scala 3 also writes one before a quoted block and ScalaTest's own DSL is full of them.
    */
  private def endOfCharLiteral(source: String, index: Int): Int =
    if source.startsWith("\\", index + 1) then
      source.indexOf('\'', index + 2) match
        case -1    => index + 1
        case close => close + 1
    else if index + 2 < source.length && source.charAt(index + 2) == '\'' then index + 3
    else index + 1

  /** The offset each line of the source starts at, indexed from zero for line one. */
  private def lineStarts(source: String): Array[Int] =
    (0 +: source.zipWithIndex.collect { case ('\n', index) => index + 1 }).toArray

  private def lineStart(starts: Array[Int], lineNumber: Int): Int =
    starts.lift(lineNumber - 1).getOrElse(starts.last)
