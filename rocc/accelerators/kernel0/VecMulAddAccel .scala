package kernel0

import Chisel._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._

class  VecMulAddAccel(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new VecMulAddAccelModule(this)
}

class VecMulAddAccelModule(outer: VecMulAddAccel, w: Int=64,   n: Int = 32)(implicit p: Parameters) extends LazyRoCCModule(outer)
  with HasCoreParameters {

	  // control
    val ctrl = Module(new KernelController(w,n) )

    ctrl.io.rocc_funct   <> io.cmd.bits.inst.funct
    ctrl.io.rocc_rs1     <> io.cmd.bits.rs1
    ctrl.io.rocc_rs2     <> io.cmd.bits.rs2
    ctrl.io.rocc_rd      <> io.cmd.bits.inst.rd
    ctrl.io.rocc_req_val <> io.cmd.valid
    io.cmd.ready      :=  ctrl.io.rocc_req_rdy 
    io.busy           :=  ctrl.io.busy
    io.resp.bits.data :=  ctrl.io.resp_data 
    io.resp.bits.rd   :=  ctrl.io.resp_rd   
    io.resp.valid     :=  ctrl.io.resp_valid 
    ctrl.io.resp_ready   <> io.resp.ready
    when (io.cmd.fire()) {
      ctrl.io.rocc_fire := true.B
    } .otherwise {
      ctrl.io.rocc_fire := false.B
    }
    io.mem.req.valid     := ctrl.io.dmem_req_val
    ctrl.io.dmem_req_rdy := io.mem.req.ready
    io.mem.req.bits.tag  :=ctrl.io.dmem_req_tag     
    io.mem.req.bits.cmd  :=ctrl.io.dmem_req_cmd     
    io.mem.req.bits.typ  :=ctrl.io.dmem_req_typ     
    io.mem.req.bits.addr :=ctrl.io.dmem_req_addr
    io.mem.req.bits.data :=ctrl.io.dmem_req_data

    ctrl.io.dmem_resp_val  <> io.mem.resp.valid
    ctrl.io.dmem_resp_tag  <> io.mem.resp.bits.tag
    ctrl.io.dmem_resp_data := io.mem.resp.bits.data
    
    //datapath
    val dpath = Module(new DpathModule(w,n))

    dpath.io.enable := ctrl.io.enable
    for (i <- 0 until n) {
        dpath.io.vec1(i)  <> ctrl.io.vec1(i)
        dpath.io.vec2(i)  <> ctrl.io.vec2(i)
	  }
    ctrl.io.valid_VecMulAdd_result := dpath.io.valid 
    ctrl.io.tree_result := dpath.io.tree_result  
    for (i <- 0 until n) {
        ctrl.io.VecMulAdd_result(i) := dpath.io.out(i)
	  }
    //io.mem.req.bits.data := 123.U << 32.U + 123.U

    io.interrupt := false.B

}


