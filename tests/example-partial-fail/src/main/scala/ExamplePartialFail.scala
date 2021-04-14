object Leap {
  def leapYear(year: Int): Boolean = {
    def divisibleBy(i: Int) = year % i == 0
    divisibleBy(4) && (divisibleBy(401) || !divisibleBy(100))
  }
}
