package lutrom

import Chisel._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._

import lutrom._

class  LUTROMAccelerator(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new LUTROMAcceleratorModule(this)
}

class LUTROMAcceleratorModule(outer: LUTROMAccelerator, n: Int = 4)(implicit p: Parameters) extends LazyRoCCModule(outer)
  with HasCoreParameters {
  // Always reg inputs
  val funct = RegInit(0.U(5.W))
  val v_mem = Reg(UInt(32.W))
  val curve_select = Reg(UInt(32.W))
  val req_rd = Reg(io.cmd.bits.inst.rd)
  val call_count = Reg(UInt(64.W))

  val do_LUT_offset = (funct === 0.U)
  val do_LUT_slope = (funct === 1.U)
  val do_reset_count = (funct === 2.U)
  val do_get_count = (funct === 3.U)

  // initialize LUT
  val LUT = Module(new LUTROM())
  LUT.io.req.bits.curve_select := curve_select
  LUT.io.req.bits.v_mem := v_mem

  // Return variable
  val output = RegInit(0.U(32.W))

  // Setup states
  val s_idle :: s_req_lut :: s_resp_lut :: s_resp_count :: s_resp :: Nil = Enum(5)
  val state = RegInit(s_idle)

  // When we get a command, start the LUT and
  // move to the s_req_lut state, waiting for the
  // LUT to finish
  when (io.cmd.fire()){
      // io.cmd only has the right values in this block
      v_mem := io.cmd.bits.rs1
      curve_select := io.cmd.bits.rs2
      funct := io.cmd.bits.inst.funct
      req_rd := io.cmd.bits.inst.rd
      when (io.cmd.bits.inst.funct === 2.U) {
        call_count := 0.U
        state := s_resp_count
      } .elsewhen (io.cmd.bits.inst.funct === 3.U) {
        state := s_resp_count
      } .otherwise {
        call_count := call_count + 1.U
        state := s_req_lut
      }

  }

  when (LUT.io.req.fire()) { state := s_resp_lut }

  when (LUT.io.resp.fire()) {
    output := Mux(do_LUT_offset, LUT.io.resp.bits.offset, LUT.io.resp.bits.slope)
    state := s_resp
  }

  when (state === s_resp_count) {
    output := call_count
    state := s_resp
  }

  when (io.resp.fire()) { state := s_idle }

  printf("cmd: ready %d   valid %d -- resp:  ready %d  valid %d -- v:%x c:%x output:%x\n",
      io.cmd.ready, io.cmd.valid, io.resp.ready, io.resp.valid, io.cmd.bits.rs1, io.cmd.bits.rs2, io.resp.bits.data)

  LUT.io.req.valid := (state === s_req_lut)
  LUT.io.resp.ready := (state === s_resp_lut)

  io.cmd.ready := (state === s_idle)

  io.resp.valid := (state === s_resp)
  io.resp.bits.rd := req_rd
  io.resp.bits.data := output
  // NEVER have the following, it results in the processor stalling
  // io.resp.bits.rd := io.cmd.bits.inst.rd

  io.busy := (state =/= s_idle)
  io.interrupt := false.B
  io.mem.req.valid := false.B
}
