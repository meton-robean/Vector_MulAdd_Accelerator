package kernel

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TesterRunner

object Launcher {
  val tests = Map(
    //@cmt 使用时取消注释即可
    // "VectorElemAdd" -> { (manager: TesterOptionsManager) =>
    //    Driver.execute(() => new VectorElemAdd(w=32,n=4), manager) {
    //      (c) => new VectorElemAddTest(c)
    //   }
    // }

    "VectorMul" -> { (manager: TesterOptionsManager) =>
       Driver.execute(() => new VectorMul(w=32,n=4), manager) {
         (c) => new VectorMulTest(c)
       }
    }

  )

  def main(args: Array[String]): Unit = {
    TesterRunner("kernel", tests, args)
  }
}


