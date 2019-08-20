package multState

import Chisel._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._

class  MultStateAccelerator(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new MultStateAcceleratorModule(this)
}

class MultStateAcceleratorModule(outer: MultStateAccelerator, n: Int = 4)(implicit p: Parameters) extends LazyRoCCModule(outer)
  with HasCoreParameters {

  val funct = io.cmd.bits.inst.funct
  val doMult = funct === UInt(0)

  //val output = RegInit(6.U(64.W))//Wire(UInt(6,64))

  val one = RegInit(7.U(64.W))
  val two = RegInit(8.U(64.W))
  val result = RegInit(9.U(64.W))
  // datapath
  val mult = Module(new MultState())

  val s_idle :: s_req_mult :: s_resp_mult :: s_wait :: s_resp :: Nil = Enum(5)
  val state = RegInit(s_idle)
  

  mult.io.req.valid := (state === s_req_mult)
  mult.io.resp.ready := (state === s_resp_mult)

  val req_rd = Reg(io.resp.bits.rd) 
   
  // control
  when (io.cmd.fire()) {
      //req_rd := io.cmd.bits.inst.rd
      state := s_req_mult
      one := io.cmd.bits.rs1
      two := io.cmd.bits.rs2
      mult.io.req.bits.one := io.cmd.bits.rs1 //@cmt data
      mult.io.req.bits.two := io.cmd.bits.rs2
      req_rd := io.cmd.bits.inst.rd  //@cmt 保存 计算结果写回的目标寄存器地址
      
  }

  when(mult.io.req.fire()) {
    state := s_resp_mult
    mult.io.req.bits.one := one
    mult.io.req.bits.two := two
    result := one * two
    printf("mult.io.req fire! ----  one: %d two: %d  --- begins to mult -----\n", one, two);
  }

  when(mult.io.resp.fire()) {
    state := s_wait
  }

  when(state === s_wait) {
    state := s_resp
  }

  when (io.resp.fire()) {
      state := s_idle

      printf("rocc io.cmd: ready %d   valid %d -- rocc io.resp:  ready %d  valid %d -- %d * %d = %d\n",
      io.cmd.ready, io.cmd.valid,    io.resp.ready,  io.resp.valid,        one, two, io.resp.bits.data)
  }
  

  io.cmd.ready := (state === s_idle)
    // command resolved if no stalls AND not issuing a load that will need a request

  // PROC RESPONSE INTERFACE
  io.resp.valid := (state === s_resp)
    // valid response if valid command, need a response, and no stalls
  io.resp.bits.rd := req_rd
    //@cmt 计算结果返回
  io.resp.bits.data := mult.io.resp.bits.out
    // Semantics is to always send out prior accumulator register value

  io.busy := (state =/= s_idle)
    // Be busy when have pending memory requests or committed possibility of pending requests
  io.interrupt := false.B
    // Set this true to trigger an interrupt on the processor (please refer to supervisor documentation)
  io.mem.req.valid := false.B

}