// See LICENSE.txt for license details.
package utils

import scala.collection.mutable.ArrayBuffer
import chisel3.iotesters._

object TesterRunner {
  def apply(section: String, testerMap: Map[String, TesterOptionsManager => Boolean], args: Array[String]): Unit = {
    var successful = 0
    val errors = new ArrayBuffer[String]

    val optionsManager = new TesterOptionsManager()
    optionsManager.doNotExitOnHelp()

    optionsManager.parse(args)

    val programArgs = optionsManager.commonOptions.programArgs

    if(programArgs.isEmpty) {
      println("Available testers")
      for(x <- testerMap.keys) {
        println(x)
      }
      println("all")
      System.exit(0)
    }

    val problemsToRun = if(programArgs.exists(x => x.toLowerCase() == "all")) {
      testerMap.keys
    }
    else {
      programArgs
    }

    for(testName <- problemsToRun) {
      testerMap.get(testName) match {
        case Some(test) =>
          println(s"Starting test $testName")
          try {
            optionsManager.setTopName(testName)
            // Chooses where the target directory name/path is
            optionsManager.setTargetDirName(s"test_run_dir/$section/$testName")
            if(test(optionsManager)) {
              successful += 1
            }
            else {
              errors += s"Tester $testName: test error occurred"
            }
          }
          catch {
            case exception: Exception =>
              exception.printStackTrace()
              errors += s"Tester $testName: exception ${exception.getMessage}"
            case t : Throwable =>
              errors += s"Tester $testName: throwable ${t.getMessage}"
          }
        case _ =>
          errors += s"Bad tester name: $testName"
      }

    }
    if(successful > 0) {
      println(s"Tester passing: $successful")
    }
    if(errors.nonEmpty) {
      println("=" * 80)
      println(s"Errors: ${errors.length}: in the following testers")
      println(errors.mkString("\n"))
      println("=" * 80)
      System.exit(1)
    }
  }
}
