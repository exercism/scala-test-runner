object Sheep {
  val name: String = "Dolly"

  // Runaway recursion, so counting overflows the stack. The `1 +` keeps this from being a tail call, which Scala
  // would otherwise turn into a loop. ScalaTest treats a VirtualMachineError as aborting the run and rethrows it
  // rather than reporting a failure, so it escapes the suite without any event being reported for the test.
  def count(sheep: Int): Int = 1 + count(sheep + 1)
}
