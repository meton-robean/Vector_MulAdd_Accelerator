package mult

import Chisel._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._

class  MultAccelerator(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new MultAcceleratorModule(this)
}

class MultAcceleratorModule(outer: MultAccelerator, n: Int = 4)(implicit p: Parameters) extends LazyRoCCModule(outer)
  with HasCoreParameters {

  val busy = Reg(init = Vec.fill(n){Bool(false)})

  val cmd = Queue(io.cmd)
  val funct = cmd.bits.inst.funct
  val doMult = funct === UInt(0)

  // datapath
  val one = Reg(UInt(width = 16))
  val two = Reg(UInt(width = 16))

  val result = Reg(UInt(width = 16))

  when (cmd.fire() && doMult) {
      one := cmd.bits.rs1
      two := cmd.bits.rs2

      result := cmd.bits.rs1 // Result is not being used currently
      printf("command fired\n")
      printf("rs1: %d, rs2: %d\n",cmd.bits.rs1,cmd.bits.rs2)
  }

  // control
  when (io.resp.fire()){
    printf("response fired\n")
    printf("rs1: %d, rs2: %d\n",cmd.bits.rs1,cmd.bits.rs2)
    printf("result: %d\n", io.resp.bits.data)
  }

  printf("cmd: ready %d   valid %d -- resp:  ready %d  valid %d -- %d * %d = %d\n",
      cmd.ready, cmd.valid, io.resp.ready, io.resp.valid, cmd.bits.rs1, cmd.bits.rs2, io.resp.bits.data)

  val doResp = cmd.bits.inst.xd
  val stallResp = doResp && !io.resp.ready

  cmd.ready := !stallResp
    // command resolved if no stalls AND not issuing a load that will need a request

  // PROC RESPONSE INTERFACE
  io.resp.valid := cmd.valid && doResp
    // valid response if valid command, need a response, and no stalls
  io.resp.bits.rd := cmd.bits.inst.rd
    // Must respond with the appropriate tag or undefined behavior
  io.resp.bits.data := cmd.bits.rs1 * cmd.bits.rs2
    // Semantics is to always send out prior accumulator register value

  io.busy := cmd.valid || busy.reduce(_||_)
    // Be busy when have pending memory requests or committed possibility of pending requests
  io.interrupt := Bool(false)
    // Set this true to trigger an interrupt on the processor (please refer to supervisor documentation)
  io.mem.req.valid := Bool(false)

}