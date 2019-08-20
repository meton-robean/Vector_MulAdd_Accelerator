package kernel

import chisel3._

class VectorElemAdd(val w: Int,val n: Int) extends Module {
  val io = IO (new Bundle {
    val input = Input(Vec(n, UInt(w.W)))
    val out = Output(UInt(w.W))
  })
  io.out := io.input.reduce(_ + _)
}
