package kernel
import chisel3._
//import Chisel._
import chisel3.iotesters.{PeekPokeTester, Driver,TesterOptionsManager,  ChiselFlatSpec,SteppedHWIOTester}
import utils.TesterRunner

class VectorMulTest(c:VectorMul) extends PeekPokeTester(c){
  for(t <- 0 until 6){

      for(i<- 0 until c.n){
        poke(c.io.vec1(i),2)
        poke(c.io.vec2(i),3)

    }
    step(1)

    expect(c.io.out, 24 )
  }
}


class VectorElemAddTest(c:VectorElemAdd) extends PeekPokeTester(c){

    for(i<- 0 until c.n){
      poke(c.io.input(i),i+1)
    }
    step(1)

    expect(c.io.out, 10)
}

