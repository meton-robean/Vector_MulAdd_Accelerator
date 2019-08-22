package kernel

import chisel3._

class Mul(val n: Int) extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(n.W))
    val in1 = Input(UInt(n.W))
    val out = Output(UInt(n.W))
  })


  io.out := io.in0 * io.in1
}
