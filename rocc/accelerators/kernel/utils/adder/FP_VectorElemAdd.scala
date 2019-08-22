package kernel

import chisel3._
import float._

 

class FP_VectorElemAdd(val w: Int,val n: Int) extends Module {
  val io = IO (new Bundle {
    val input = Input(Vec(n, UInt(w.W)))
    val out = Output(UInt(w.W))
  })

  def fadd(a:UInt, b:UInt):UInt = {
    val op1 = a.asTypeOf(new Float)
    val op2 = b.asTypeOf(new Float)

    //val res = Wire(new Float)

    val res = (op1 + op2).asUInt
    res
    
  }

  io.out := io.input.reduce(fadd)



}
