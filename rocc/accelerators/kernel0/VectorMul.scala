package kernel0

import chisel3._

class VectorMul(val w: Int,val n: Int) extends Module{
  val io = IO(new Bundle{
    val vec1 =  Input(Vec(n,UInt(w.W)))
    val vec2 =  Input(Vec(n,UInt(w.W)))
    val out  =  Output(UInt(w.W))
  })

  val muls = Array.fill(n)(Module(new Mul(n=w)).io)

  //val reg0_vec16 = Reg(Vec(Seq.fill(w){ UInt(16.W) }))
  //val reg1_vec16 = Reg(Vec(Seq.fill(w){ UInt() }))

  val quotient   = RegInit(Vec(Seq.fill(n)(0.asUInt(w.W))))


  for(i <- 0 until n) {
    muls(i).in0 := io.vec1(i)
    muls(i).in1 := io.vec2(i)
    quotient(i) := muls(i).out
    //result = result+quotient(i)
  }

  val adder=Module(new VectorElemAdd(w=w,n=n)).io

  adder.input :=quotient

  io.out := adder.out


}


// object VectorMul extends App {
//   chisel3.Driver.execute(args,()=>new VectorMul(2,9))
// }
