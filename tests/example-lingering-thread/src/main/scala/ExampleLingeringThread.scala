object Sheep {
  // A thread that is neither a daemon nor ever joined, which is what an executor a solution forgets to shut down
  // amounts to. It keeps the JVM alive after the tests are done unless the runner exits deliberately.
  def count(sheep: Int): Int = {
    new Thread(() => Thread.sleep(600000)).start()
    sheep
  }
}
