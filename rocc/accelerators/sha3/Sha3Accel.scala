//see LICENSE for license
//authors: Xuhao Chen
package sha3
import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._
import freechips.rocketchip.tile._

case object WidthP extends Field[Int]
case object Stages extends Field[Int]
case object FastMem extends Field[Boolean]
case object BufferSram extends Field[Boolean]

class Sha3Accel(implicit p: Parameters) extends LazyRoCC() {
	override lazy val module = new Sha3AccelModuleImp(this)
}

class Sha3AccelModuleImp(outer: Sha3Accel) extends LazyRoCCModule(outer) with HasCoreParameters {
	val w = outer.p(WidthP)
	val s = outer.p(Stages)
	io.resp.valid := false.B

	// control
	val ctrl = Module(new CtrlModule(w,s)(outer.p))

	ctrl.io.rocc_funct   <> io.cmd.bits.inst.funct
	ctrl.io.rocc_rs1     <> io.cmd.bits.rs1
	ctrl.io.rocc_rs2     <> io.cmd.bits.rs2
	ctrl.io.rocc_rd      <> io.cmd.bits.inst.rd
	ctrl.io.rocc_req_val <> io.cmd.valid
	ctrl.io.rocc_req_rdy <> io.cmd.ready
	ctrl.io.busy         <> io.busy
	ctrl.io.resp_data    <> io.resp.bits.data
	ctrl.io.resp_rd      <> io.resp.bits.rd
	ctrl.io.resp_valid   <> io.resp.valid
	when (io.cmd.fire()) {
		ctrl.io.rocc_fire := true.B
	} .otherwise {
		ctrl.io.rocc_fire := false.B
	}
	ctrl.io.dmem_req_val <> io.mem.req.valid
	ctrl.io.dmem_req_rdy <> io.mem.req.ready
	ctrl.io.dmem_req_tag <> io.mem.req.bits.tag
	ctrl.io.dmem_req_cmd <> io.mem.req.bits.cmd
	ctrl.io.dmem_req_typ <> io.mem.req.bits.typ
	ctrl.io.dmem_req_addr<> io.mem.req.bits.addr

	ctrl.io.dmem_resp_val <> io.mem.resp.valid
	ctrl.io.dmem_resp_tag <> io.mem.resp.bits.tag
	ctrl.io.dmem_resp_data := io.mem.resp.bits.data

	// datapath
	val dpath = Module(new DpathModule(w,s))

	dpath.io.message_in <> ctrl.io.buffer_out
	dpath.io.init   <> ctrl.io.init
	dpath.io.round  <> ctrl.io.round
	dpath.io.write  <> ctrl.io.write
	dpath.io.stage  <> ctrl.io.stage
	dpath.io.absorb <> ctrl.io.absorb
	dpath.io.aindex <> ctrl.io.aindex

	// output hash back to the memory
	io.mem.req.bits.data := dpath.io.hash_out(ctrl.io.windex)

	io.interrupt := false.B
	//io.imem.acquire.valid := false.B
	//io.imem.grant.ready := true.B
	//io.dmem.head.acquire.valid := false.B
	//io.dmem.head.grant.ready := false.B
	//io.iptw.req.valid := false.B
	//io.dptw.req.valid := false.B
	//io.pptw.req.valid := false.B
	//io.mem.invalidate_lr := false.B
}

