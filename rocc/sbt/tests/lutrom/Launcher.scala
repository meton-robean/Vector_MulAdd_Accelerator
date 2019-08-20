package lutrom

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TesterRunner

object Launcher {
  val tests = Map(
    "LUTROM" -> { (manager: TesterOptionsManager) =>
       Driver.execute(() => new LUTROM(), manager) {
         (c) => new LUTROMTest(c)
       }
    },
    "FPGreaterThan" -> { (manager: TesterOptionsManager) =>
       Driver.execute(() => new FPGreaterThan(), manager) {
         (c) => new FPGreaterThanTests(c)
       }
    }
  )

  def main(args: Array[String]): Unit = {
    TesterRunner("lutrom", tests, args)
  }
}
