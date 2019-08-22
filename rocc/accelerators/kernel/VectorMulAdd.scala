package kernel

import chisel3._

class VectorMulAdd(val w: Int,val n: Int) extends Module{
  val io = IO(new Bundle{
    val vec1 =  Input(Vec(n,UInt(w.W)))
    val vec2 =  Input(Vec(n,UInt(w.W)))
    val out  =  Output(UInt(w.W))
  })

  //val muls = Array.fill(n)(Module(new Mul(n=w)).io)  //int 
  val muls = Array.fill(n)(Module(new FP_Mul(n=w)).io) //float

  val quotient   = RegInit(Vec(Seq.fill(n)(0.asUInt(w.W))))


  for(i <- 0 until n) {
    muls(i).in0 := io.vec1(i)
    muls(i).in1 := io.vec2(i)
    quotient(i) := muls(i).out
    //result = result+quotient(i)
  }

  //val adder=Module(new VectorElemAdd(w=w,n=n)).io  //int
  val adder=Module(new FP_VectorElemAdd(w=w,n=n)).io //float

  adder.input :=quotient

  io.out := adder.out


}


// object VectorMul extends App {
//   chisel3.Driver.execute(args,()=>new VectorMul(2,9))
// }
