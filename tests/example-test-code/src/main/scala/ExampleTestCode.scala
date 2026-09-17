object Brackets:

  private val pairs = Map('(' -> ')', '[' -> ']', '{' -> '}')

  def isPaired(input: String): Boolean =
    val remaining = input.foldLeft(Option(List.empty[Char])) { (stack, bracket) =>
      stack.flatMap { open =>
        if pairs.contains(bracket) then Some(pairs(bracket) :: open)
        else if pairs.valuesIterator.contains(bracket) then
          if open.headOption.contains(bracket) then Some(open.tail) else None
        else Some(open)
      }
    }
    remaining.exists(_.isEmpty)
