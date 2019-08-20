package spmv

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._
import freechips.rocketchip.tile._

case object WidthP extends Field[Int]
case object NumPEs extends Field[Int]
//case object FastMem extends Field[Boolean]
//case object BufferSram extends Field[Boolean]


//class SpmvAccel(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(opcodes)	
class SpmvAccel(implicit p: Parameters) extends LazyRoCC {
	override lazy val module = new SpmvAccelModuleImp(this)
}

class SpmvAccelModuleImp(outer: SpmvAccel) extends LazyRoCCModule(outer) with HasCoreParameters {
	val w = outer.p(WidthP)
	val n = outer.p(NumPEs)

	// controller
	val ctrl = Module(new Controller(w, n)(outer.p)).io

	ctrl.busy         <> io.busy
	ctrl.rocc_rd      <> io.cmd.bits.inst.rd
	ctrl.rocc_rs1     <> io.cmd.bits.rs1
	ctrl.rocc_rs2     <> io.cmd.bits.rs2
	ctrl.rocc_funct   <> io.cmd.bits.inst.funct
	ctrl.rocc_req_val <> io.cmd.valid
	io.cmd.ready      := ctrl.rocc_req_rdy
	io.resp.bits.rd   := ctrl.resp_rd     
	io.resp.bits.data := ctrl.resp_data  
	io.resp.valid     := ctrl.resp_valid 
	ctrl.rocc_fire    := io.cmd.fire()

	ctrl.dmem_req_val <> io.mem.req.valid
	ctrl.dmem_req_rdy <> io.mem.req.ready
	ctrl.dmem_req_tag <> io.mem.req.bits.tag
	ctrl.dmem_req_cmd <> io.mem.req.bits.cmd
	ctrl.dmem_req_typ <> io.mem.req.bits.typ
	ctrl.dmem_req_addr<> io.mem.req.bits.addr

	ctrl.dmem_resp_val  <> io.mem.resp.valid
	ctrl.dmem_resp_tag  <> io.mem.resp.bits.tag
	ctrl.dmem_resp_data := io.mem.resp.bits.data

	// processing elements
	val pe_array = VecInit(Seq.fill(n)(Module(new ProcessingElement(w)).io))

	for (i <- 0 until n) {
		pe_array(i).rowid <> ctrl.rowid(i)
		pe_array(i).value <> ctrl.value(i)
		pe_array(i).x_in  <> ctrl.x_out(i)
		pe_array(i).valid <> ctrl.valid(i)
	}
	
	// write output data back to the memory
	io.mem.req.bits.data := pe_array(ctrl.peid_wr).y_out

	io.interrupt  := false.B
	io.resp.valid := false.B
}
