package multState

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TesterRunner

object Launcher {
  val tests = Map(
    "MULTSTATE" -> { (manager: TesterOptionsManager) =>
       Driver.execute(() => new MultState(), manager) {
         (c) => new MultStateTest(c)
       }
    }
  )

  def main(args: Array[String]): Unit = {
    TesterRunner("multState", tests, args)
  }
}
