package spmv

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._

// A memory controller that reads the CSC data structures and input(x)/output(y) vectors from host memory
// A scheduller that dispatchs compute tasks to PEs and detects hazards
// w is the bit width of data in values, n is the number of PEs
class Controller(val w: Int = 32, val n: Int = 8)(implicit p: Parameters) extends Module {
	val MAXID = 0x7fffffff
	val block_size = BLOCK.N
	val io = IO(new Bundle {
		// for RoCC communication
		val rocc_req_rdy = Output(Bool())
		val rocc_req_val = Input(Bool())
		val rocc_fire    = Input(Bool())
		val rocc_funct   = Input(Bits(2.W))
		val rocc_rs1     = Input(Bits(64.W))
		val rocc_rs2     = Input(Bits(64.W))
		val rocc_rd      = Input(Bits(5.W))
		val resp_rd      = Output(Bits(5.W))
		val resp_data    = Output(UInt(32.W))
		val resp_valid   = Output(Bool())

		// for memory controller to read/write data through the RoCC interface
		val dmem_req_rdy = Input(Bool())
		val dmem_req_val = Output(Bool())
		val dmem_req_tag = Output(UInt(7.W))
		val dmem_req_cmd = Output(UInt(M_SZ.W))
		val dmem_req_typ = Output(UInt(MT_SZ.W))
		val dmem_req_addr = Output(UInt(32.W))
		val dmem_resp_val = Input(Bool())
		val dmem_resp_tag = Input(UInt(7.W))
		val dmem_resp_data = Input(UInt(w.W))

		// control signals
		val busy   = Output(Bool())
		val valid  = Output(Vec(n, Bool()))
		val rowid  = Output(Vec(n, UInt(32.W)))
		val value  = Output(Vec(n, UInt(w.W)))
		val x_out  = Output(Vec(n, UInt(w.W)))
		val peid_wr = Output(UInt(log2Ceil(n).W))
	})

	val busy = RegInit(false.B)
//	val nrow = RegInit(0.U(32.W))
	val ncol = RegInit(0.U(32.W)) // number of columns
	val nnz  = RegInit(0.U(32.W)) // number of non-zero values

	// addresses (64-bit)
	val x_addr = RegInit(0.U(64.W))
	val y_addr = RegInit(0.U(64.W))
	val colptr = RegInit(0.U(64.W))
	val rowidx = RegInit(0.U(64.W))
	val values = RegInit(0.U(64.W))

	// initialize output signals
	io.busy         := busy
	io.dmem_req_val := false.B
	io.dmem_req_tag := 0.U(7.W)
	io.dmem_req_cmd := M_XRD
	io.dmem_req_typ := MT_D
	io.dmem_req_addr:= 0.U(32.W)
	io.rocc_req_rdy := !busy
	io.resp_rd      := io.rocc_rd
	io.resp_valid   := io.rocc_req_val

	// for debug
	when (io.rocc_rs2 === 0.U) {
		io.resp_data := x_addr
	} .elsewhen (io.rocc_rs2 === 1.U) {
		io.resp_data := y_addr
	} .elsewhen (io.rocc_rs2 === 2.U) {
		io.resp_data := colptr
	} .elsewhen (io.rocc_rs2 === 3.U) {
		io.resp_data := rowidx
	} .elsewhen (io.rocc_rs2 === 4.U) {
		io.resp_data := values
	} .otherwise {
		io.resp_data := nnz
	}

	// decode the rocc instruction
	when (io.rocc_req_val && !busy) {
		when (io.rocc_funct === 0.U) {
			io.busy := true.B
			io.rocc_req_rdy := true.B
			x_addr := io.rocc_rs1
			y_addr := io.rocc_rs2
		} .elsewhen (io.rocc_funct === 1.U) {
			io.busy := true.B
			io.rocc_req_rdy := true.B
			colptr := io.rocc_rs1
			rowidx := io.rocc_rs2
		} .elsewhen (io.rocc_funct === 2.U) {
			io.busy := true.B
			io.rocc_req_rdy := true.B
			//nrow := io.rocc_rs1
			ncol := io.rocc_rs2
		} .elsewhen (io.rocc_funct === 3.U) {
			io.busy := true.B
			io.rocc_req_rdy := true.B
			busy := true.B // launch the computation
			values := io.rocc_rs1
			nnz := io.rocc_rs2
		}
	}

	// counters
	val bindex = RegInit(0.U(log2Ceil(block_size).W)) // block index
	val cindex = RegInit(0.U(32.W)) // colptr index
	val rindex = RegInit(0.U(32.W)) // rowid index
	val vindex = RegInit(0.U(32.W)) // value index
	val xindex = RegInit(0.U(32.W)) // vector x index
	val yindex = RegInit(0.U(32.W)) // vector y index

	// buffers
	val x      = RegInit(0.U(w.W)) // current x
	val peid   = RegInit(0.U(log2Ceil(n).W)) // PE id
	val rowid  = RegInit(0.U(32.W)) // row id
	val value  = RegInit(0.U(w.W))  // matrix value
	val nzsize = RegInit(0.U(32.W)) // number of non-zero values in the current column
	val col_start = RegInit(0.U(32.W))
	val col_end   = RegInit(0.U(32.W))
	val stalled   = RegInit(false.B)
//	val all_done  = RegInit(false.B)
	val buffer_valid = RegInit(false.B)
	val writes_done = RegInit(VecInit(Seq.fill(n){false.B})) // for each PE
	/*
	for (i <- 0 until n) {
		io.rowid(i) := RegNext(rowid)
		io.value(i) := RegNext(value)
		io.x_out(i) := buffer(rowid)
	}
	*/

	for (i <- 0 until n) {
		io.x_out(i) := x
		io.rowid(i) := rowid
		io.value(i) := value
		io.valid(i) := false.B
	}
	io.peid_wr := 0.U

	// the scheduler
	val s_idle :: s_check :: s_update :: s_write :: Nil = Enum(4)
	val state = RegInit(s_idle)

	// the memory controller
	val m_idle :: m_read_x :: m_wait_x :: m_read_colptr :: m_wait_colptr :: m_read_rowid :: m_wait_rowid :: m_read_value :: m_wait_value :: m_stall :: m_write :: Nil = Enum(11)
	val mem_s = RegInit(m_idle)

	switch(mem_s) {
		is(m_idle) {
			val canRead = busy && xindex < ncol && !buffer_valid
			when (canRead) {
				// start reading data
				mem_s     := m_read_x
				col_start := 0.U
				col_end   := 0.U
				colptr    := colptr + 4.U // start reading colptr from the second element (the first is 0 anyway)
			} .otherwise {
				mem_s := m_idle
			}
		}
		is(m_read_x) {
			//only read if we aren't writing
			when (state =/= s_write) {
				//dmem signals
				io.dmem_req_val := xindex < ncol
				io.dmem_req_addr:= x_addr
				io.dmem_req_tag := 0.U
				io.dmem_req_cmd := M_XRD
				io.dmem_req_typ := MT_D

				// read data if ready and valid
				when(io.dmem_req_rdy && io.dmem_req_val) {
					mem_s := m_wait_x // wait until reading done
				} .otherwise {
					mem_s := m_read_x
				}
			}
		}
		is(m_wait_x) {
			when (io.dmem_resp_val) {
				// put the recieved data into buffer
				x := io.dmem_resp_data
			}
			xindex := xindex + 1.U
			x_addr := x_addr + 4.U
			cindex := 0.U
			mem_s  := m_read_colptr
		}
		is(m_read_colptr) {
			when (state =/= s_write) {
				// done reading x, start reading colptr
				io.dmem_req_val := cindex < ncol
				io.dmem_req_addr:= colptr
				io.dmem_req_tag := 0.U
				io.dmem_req_cmd := M_XRD
				io.dmem_req_typ := MT_D

				// read data if ready and valid
				when(io.dmem_req_rdy && io.dmem_req_val) {
					col_start := col_end
					mem_s := m_wait_colptr // wait until reading done
				} .otherwise {
					mem_s := m_read_colptr
				}
			}
		}
		is(m_wait_colptr) {
			when (io.dmem_resp_val) {
				col_end := io.dmem_resp_data
			}
			cindex := cindex + 1.U
			colptr := colptr + 4.U
			mem_s  := m_read_rowid
			rindex := 0.U
			nzsize := col_end - col_start
		}
		is(m_read_rowid) {
			when (state =/= s_write) {
				io.dmem_req_val := rindex < nzsize
				io.dmem_req_addr:= rowidx
				io.dmem_req_tag := 0.U 
				io.dmem_req_cmd := M_XRD
				io.dmem_req_typ := MT_D

				// read data if ready and valid
				when(io.dmem_req_rdy && io.dmem_req_val) {
					mem_s := m_wait_rowid
				} .otherwise {
					mem_s := m_read_rowid
				}
			}
		}
		is(m_wait_rowid) {
			when (io.dmem_resp_val) {
				rowid := io.dmem_resp_data
			}
			rindex := rindex + 1.U
			rowidx := rowidx + 4.U
			mem_s := m_read_value
		}
		is(m_read_value) {
			when (state =/= s_write) {
				io.dmem_req_val := vindex < nzsize
				io.dmem_req_addr:= values
				io.dmem_req_tag := 0.U 
				io.dmem_req_cmd := M_XRD
				io.dmem_req_typ := MT_D

				// read data if ready and valid
				when(io.dmem_req_rdy && io.dmem_req_val) {
					mem_s := m_wait_value
				} .otherwise {
					mem_s := m_read_value
				}
			}
		}
		is(m_wait_value) {
			when (io.dmem_resp_val) {
				value := io.dmem_resp_data
			}
			vindex := vindex + 1.U
			values := values + 4.U
			peid   := rowid / block_size.U
			mem_s  := m_stall
			buffer_valid := true.B
		}
		is(m_stall) {
			when (stalled) {
				mem_s := m_stall
			} .otherwise {
				// go fetch the next data
				when(rindex < nzsize) { // smae as vindex < nzsize
					mem_s := m_read_rowid // read the next value in the current column
				} .otherwise {
					when (xindex < ncol) { // same as cindex < ncol
						mem_s := m_read_x // read the next column
					} .otherwise {
						yindex := 0.U
						mem_s := m_write // all done, go write back results
					}
				}
			}
		}
		is(m_write) {
			// set up writing request signals
			io.dmem_req_val := yindex < ncol
			io.dmem_req_addr:= y_addr + (yindex << 2.U)
			io.dmem_req_tag := yindex % block_size.U
			io.dmem_req_cmd := M_XWR
			val id = yindex / block_size.U
			io.rowid(id) := yindex
			io.valid(id) := true.B
			io.peid_wr  := id

			when (io.dmem_req_rdy) {
				yindex := yindex + 1.U
			}
/*
			// there is a response from memory
			when (io.dmem_resp_val) {
				// this is a response to a write
				when(dmem_resp_tag_reg(4,0) >= block_size.U) {
					writes_done(dmem_resp_tag_reg(4,0) - block_size.U) := true.B
				}
			}
			when (writes_done.reduce(_&&_)) {
*/
			when (yindex >= ncol) {
				// all the writes are done, reset
				bindex := 0.U
				xindex := 0.U
				yindex := 0.U
				cindex := 0.U
				rindex := 0.U
				vindex := 0.U

				x      := 0.U
				nnz    := 0.U
				peid   := 0.U
				ncol   := 0.U
				rowid  := 0.U
				value  := 0.U
				nzsize := 0.U

				x_addr := 0.U
				y_addr := 0.U
				colptr := 0.U
				rowidx := 0.U
				values := 0.U

				col_start := 0.U
				col_end := 0.U
				busy := false.B
				stalled := false.B
//				all_done := false.B
				buffer_valid := false.B
				writes_done := VecInit(Seq.fill(n){false.B})
				mem_s := m_idle
			} .otherwise {
				// not done yet, continue writing
				state := m_write
			}
		}
	}

	// for each PE, we need a queue to hold the rowid of on-the-fly compute tasks
	val lat = LATENCY.compute + LATENCY.store
	val initValues = Seq.fill(lat*n) { MAXID.U(32.W) }
	val hazard_buf = RegInit(VecInit(initValues))
	val hazard_free = RegInit(true.B)
	val dmem_resp_tag_reg = RegNext(io.dmem_resp_tag)

	// The scheduler to dispatch tasks and detect hazards
	switch(state) {
		is(s_idle) {
			when (busy && buffer_valid) {
				busy  := true.B
				state := s_check
			} .otherwise {
				state := s_idle
			}
		}
		is(s_check) {
			for (i <- 0 until lat) {
				val id = hazard_buf(peid*lat.U + i.U)
				when (id =/= MAXID.U && id === rowid) {
					hazard_free := false.B
				}
			}
			when (hazard_free) {
				io.valid(peid) := true.B // notify the PE to absorb data
				buffer_valid := false.B
				stalled := false.B
			} .otherwise {
				io.valid(peid) := false.B // hold the data when hazard detected
				buffer_valid := true.B
				stalled := true.B
			}
			hazard_buf(peid*lat.U) := hazard_buf(peid*lat.U+1.U)
			state := s_update
		}
		is(s_update) {
			for (i <- 1 until lat-1) {
				hazard_buf(peid*lat.U + i.U) := hazard_buf(peid*lat.U + (i+1).U)
			}
			when (hazard_free) {
				hazard_buf(peid*lat.U + (lat-1).U) := rowid
				state := s_idle
			} .otherwise {
				hazard_buf(peid*lat.U + (lat-1).U) := MAXID.U
				state := s_check
			}
		}
	}
}
