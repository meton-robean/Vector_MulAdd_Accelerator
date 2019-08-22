package kernel

import chisel3._
import float._

class FP_Mul(val n: Int) extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(n.W))
    val in1 = Input(UInt(n.W))
    val out = Output(UInt(n.W))
  })
  val op1 = (io.in0).asTypeOf(new Float)
  val op2 = (io.in1).asTypeOf(new Float)
  val res = Wire(new Float)
  res := op1 * op2

  io.out := res.asUInt 
}
