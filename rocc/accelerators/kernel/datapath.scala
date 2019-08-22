package kernel

import chisel3._
import chisel3.util._

object LATENCY {
	val compute = 4

}

class DpathModule(val w: Int, val n: Int) extends Module {

    ///@cmt w 数据位宽 n 计算单元数目
	val io = IO(new Bundle {
		val enable =  Input(Bool())
        val vec1   =  Input(Vec(n,UInt(w.W)))
        val vec2   =  Input(Vec(n,UInt(w.W)))
        val valid  =  Output(Bool())
        val out    =  Output(UInt(w.W))
	})

    val s_idle :: s_compute:: s_ready:: Nil = Enum(3)
    val state_reg = RegInit(s_idle)
    val lat   = RegInit(0.U)  // latency counter

    //vector mult add unit
    val VectorMulUnit = Module( new VectorMulAdd(w, n) ).io

    ///init
    for(i <- 0 until n){
        VectorMulUnit.vec1(i) := io.vec1(i)
        VectorMulUnit.vec2(i) := io.vec2(i)
    }
    io.valid := false.B
    io.out   := 0.U
    

    ///state
    switch(state_reg){
        is(s_idle){
            //printf("dpath: s_idle\n")
            io.valid := false.B
            when(io.enable === true.B){ //ctrl send data to datapath for computing...
                state_reg := s_compute
            }.otherwise{
                state_reg := s_idle
            }
        }
        is(s_compute){
            //printf("dpath: s_compute\n")
            io.valid :=false.B
            //waiting for a while ...
            // when (lat < (LATENCY.compute).U) {
            //     printf("%d\n", lat)
			// 	lat := lat + 1.U
			// 	state_reg := s_compute
			// } .otherwise {
			// 	lat := 0.U
			// 	state_reg := s_ready
			// }
            state_reg := s_ready
        }
        is(s_ready){
            //printf("dpath: s_ready\n")
            //get result from vector muladd unit...
            io.out    := VectorMulUnit.out
            io.valid  := true.B
            state_reg := s_idle
        }

    }
}