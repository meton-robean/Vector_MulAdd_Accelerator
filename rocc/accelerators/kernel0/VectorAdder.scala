package kernel0

import chisel3._

class VectorAdder(val w: Int,val n: Int) extends Module{
  val io = IO(new Bundle{
    val vec1 =  Input(Vec(n,UInt(w.W)))
    val vec2 =  Input(Vec(n,UInt(w.W)))
    val out  =  Output(Vec(n,UInt(w.W)))
  })

  val adders = Array.fill(n)(Module(new Adder(w=w)).io)


  val sum   = RegInit(VecInit(Seq.fill(n)(0.asUInt(w.W))))
  




  //connect up adders
  for(i <- 0 until n) {
    adders(i).in0 := io.vec1(i)
    adders(i).in1 := io.vec2(i)
    sum(i) := adders(i).out
  }

  io.out := sum


}

object VectorAdder extends App {
  chisel3.Driver.execute(args,()=>new VectorAdder(2,3))
}
