//see LICENSE for license

package sha3

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._

class CtrlModule(val w: Int, val s: Int)(implicit p: Parameters) extends Module() {
	//val r = 2*256
	val r = 0
	val c = 25*w - r // 25 words, 64 bits per word, 1600 bits in total
	val round_size_words = c/w // data size of each round
	val rounds = 24 //12 + 2l // we have 24 rounds
	val hash_size_words = 256/w
	val bytes_per_word = w/8

	val io = IO(new Bundle {
		val rocc_req_val = Input(Bool())
		val rocc_req_rdy = Output(Bool())
		val rocc_funct   = Input(Bits(2.W))
		val rocc_rs1     = Input(Bits(64.W))
		val rocc_rs2     = Input(Bits(64.W))
		val rocc_rd      = Input(Bits(5.W))
		val rocc_fire    = Input(Bool())
		val resp_data    = Output(UInt(32.W))
		val resp_rd      = Output(Bits(5.W))
		val resp_valid   = Output(Bool())

		val dmem_req_val = Output(Bool())
		val dmem_req_rdy = Input(Bool())
		val dmem_req_tag = Output(UInt(7.W))
		val dmem_req_cmd = Output(UInt(M_SZ.W))
		val dmem_req_typ = Output(UInt(MT_SZ.W))
		val dmem_req_addr = Output(UInt(32.W))

		val dmem_resp_val = Input(Bool())
		val dmem_resp_tag = Input(UInt(7.W))
		val dmem_resp_data = Input(UInt(w.W))

		val busy   = Output(Bool())
		val round  = Output(UInt(5.W))
		val stage  = Output(UInt(log2Ceil(s).W))
		val absorb = Output(Bool())
		val init   = Output(Bool())
		val write  = Output(Bool())
		val aindex = Output(UInt(log2Ceil(round_size_words).W))
		val windex = Output(UInt(log2Ceil(hash_size_words+1).W))

		val buffer_out = Output(Bits(w.W))
	})

	val msg_addr  = RegInit(0.U(64.W))
	val hash_addr = RegInit(0.U(64.W))
	val msg_len   = RegInit(0.U(32.W))
	val busy      = RegInit(false.B)
	val my_msg_len= RegInit(0.U(32.W))

	//memory pipe state
	val dmem_resp_tag_reg = RegNext(io.dmem_resp_tag)
	val fast_mem = p(FastMem)
	val m_idle :: m_read :: m_wait :: m_pad :: m_absorb :: Nil = Enum(5)
	val mem_s = RegInit(m_idle)

	//SRAM Buffer
	val buffer_sram = p(BufferSram)
	//val buffer_mem = Mem(round_size_words, UInt(w.W))
	//val buffer_mem = Mem(UInt(w.W), round_size_words, seqRead = true)
	val initValues = Seq.fill(round_size_words) { 0.U(w.W) }
	val buffer = RegInit(VecInit(initValues)) // to hold the data of each round
	val writes_done = RegInit(VecInit(Seq.fill(hash_size_words){false.B}))
	
	val buffer_reg_raddr = RegInit(0.U(log2Ceil(round_size_words).W))
	//val buffer_waddr = 0.U(w.W)
	//val buffer_wdata = 0.U(w.W)
	val buffer_rdata = 0.U(w.W)

	// some flag registers and counters
	val buffer_valid = RegInit(false.B)
	val buffer_count = RegInit(0.U(5.W))
	val areg   = RegInit(false.B) // a flag to indicate if we are doing absorb
	val read   = RegInit(0.U(32.W)) // count how many words in total have been read from memory, compare with msg_len to determine if the entire message is all read
	val hashed = RegInit(0.U(32.W)) // count how many words in total have been hashed
	val mindex = RegInit(0.U(5.W)) // the index for buffer, determine if the buffer is full
	val sindex = RegInit(0.U((log2Ceil(s)+1).W)) // stage index, a counter for hash
	val aindex = RegInit(0.U(log2Ceil(round_size_words).W)) // absorb counter
	val windex = RegInit(0.U(log2Ceil(hash_size_words+1).W))
	val rindex = RegInit((rounds+1).U(5.W)) // round index, a counter for absorb (Max=round_size_words-1)
	//val pindex = RegInit(0.U(log2Ceil(round_size_words).W))
	val rindex_reg = RegNext(rindex)

	val s_idle :: s_absorb :: s_hash :: s_write :: Nil = Enum(4)
	val state = RegInit(s_idle)

	io.absorb := areg
	areg      := false.B
	io.aindex := RegNext(aindex)
	io.windex := windex

	if (buffer_sram) {
		buffer_reg_raddr := aindex
		io.buffer_out := buffer_rdata
	} else {
		io.buffer_out := buffer(io.aindex)
	}

	//io.rocc_req_rdy := false.B
	io.init   := false.B
	io.busy   := busy
	//io.busy   := io.rocc_req_val || busy
	io.round  := rindex
	io.stage  := sindex
	io.write  := true.B

	io.dmem_req_val := false.B
	io.dmem_req_tag := rindex
	io.dmem_req_cmd := M_XRD
	io.dmem_req_typ := MT_D
	io.dmem_req_addr:= 0.U(32.W)

	io.rocc_req_rdy := !busy
	io.resp_rd := io.rocc_rd
	io.resp_valid := io.rocc_req_val
	// for debug
	when (io.rocc_rs2 === 1.U) {
		io.resp_data := msg_addr
	} .elsewhen (io.rocc_rs2 === 2.U) {
		io.resp_data := hash_addr
	} .elsewhen (io.rocc_rs2 === 0.U) {
		io.resp_data := windex
	} .otherwise {
		io.resp_data := my_msg_len
	}

	// decode the rocc instruction
	when (io.rocc_req_val && !busy) {
		io.busy := true.B
		when (io.rocc_funct === 0.U) {
			io.rocc_req_rdy := true.B
			msg_addr := io.rocc_rs1
			hash_addr := io.rocc_rs2
		} .elsewhen (io.rocc_funct === 1.U) {
			io.rocc_req_rdy := true.B
			busy := true.B
			msg_len := io.rocc_rs1
			my_msg_len := io.rocc_rs1
		} .elsewhen (io.rocc_funct === 3.U) {
			when(io.rocc_fire) {
				when (io.rocc_rs2 === 1.U) {
					msg_addr := io.rocc_rs1
				} .otherwise {
					hash_addr := io.rocc_rs1
				}
			}
		}
	}

	switch(mem_s) {
		is(m_idle) {
			val canRead = busy && (read < msg_len || (read === msg_len && msg_len === 0.U)) &&
							!buffer_valid && buffer_count === 0.U
			when (canRead) {
				// start reading data
				mindex := 0.U
				mem_s := m_read
			} .otherwise {
				mem_s := m_idle
			}
		}
		is(m_read) {
			//only read if we aren't writing
			when (state =/= s_write) {
				//dmem signals
				io.dmem_req_val := read < msg_len && mindex < round_size_words.U
				io.dmem_req_addr:= msg_addr + (mindex << 3.U) // read 1 word each time, 1 word = 8 bytes, so left shift 3 (i.e. times 8)
				io.dmem_req_tag := mindex
				io.dmem_req_cmd := M_XRD
				io.dmem_req_typ := MT_D

				// read data if ready and valid
				when(io.dmem_req_rdy && io.dmem_req_val) {
					mindex := mindex + 1.U // read 1 word each time
					read := read + 8.U // read 8 bytes each time
					mem_s := m_wait // wait until reading done
				} .otherwise {
					mem_s := m_read
				}
			}
		}
		is(m_wait) {
			when (io.dmem_resp_val) {
				// put the recieved data into buffer
				buffer(mindex - 1.U) := io.dmem_resp_data
			}
			buffer_count := buffer_count + 1.U

			// next state
			// the buffer is not full
			//when (mindex < (round_size_words-1).U) {
			when (mindex < (round_size_words).U) {
				when (read < msg_len) {
					// continue reading
					mem_s := m_read
				} .otherwise {
					// done reading
					buffer_valid := false.B
					mem_s := m_absorb
				}
			} .otherwise {
				// message not done yet, but buffer is full, so absorb current data in the buffer
				msg_addr := msg_addr + (round_size_words << 3).U // 1 word = 8 bytes, so left shift 3 (i.e. times 8)
				buffer_valid := false.B
				mem_s := m_absorb
				/*
				when (mindex < (round_size_words).U && !(io.dmem_req_rdy && io.dmem_req_val)) {
					//we are still waiting to send the last request
					mem_s := m_read
				} .otherwise {
					//we have reached the end of this chunk
					msg_addr := msg_addr + (round_size_words << 3).U
					when((msg_len < (read + 8.U))) {
						//but the buffer still isn't full
						buffer_valid := false.B
						mem_s := m_absorb
					} .otherwise {
						buffer_valid := true.B
						mem_s := m_idle
					}
				}
				*/
			}
		}
		is(m_absorb) {
			buffer_valid := true.B
			//move to idle when we know this thread was absorbed
			when(aindex >= (round_size_words-1).U) {
				mem_s := m_idle
			} .otherwise {
				mem_s := m_absorb
			}
		}
	}

	switch(state) {
		is(s_idle) {
			val canAbsorb = busy && rindex_reg >= rounds.U && buffer_valid && hashed <= msg_len
			when (canAbsorb) {
				busy  := true.B
				state := s_absorb
			} .otherwise {
				state := s_idle
			}
		}
		is(s_absorb) {
			io.write := !areg
			areg := true.B // notify dpath to start absorb data (io.absorb := areg)
			aindex := aindex + 1.U
			when(io.aindex >= (round_size_words-1).U) {
				aindex := 0.U
				rindex := 0.U
				sindex := 0.U
				areg := false.B
				buffer_valid := false.B
				buffer_count := 0.U
				hashed := hashed + (8*round_size_words).U
				state := s_hash
			} .otherwise {
				state := s_absorb
			}
		}
		is(s_hash) {
			when (rindex < rounds.U) {
				when (sindex < (s-1).U) {
					sindex := sindex + 1.U
					io.stage := sindex
					io.round := rindex
					io.write := false.B
					state := s_hash
				} .otherwise {
					sindex := 0.U
					rindex := rindex + 1.U
					io.round := rindex
					io.write := false.B
					state := s_hash
				}
			} .otherwise {
				io.write := true.B
				when (hashed > msg_len || (hashed === msg_len && rindex === rounds.U)) {
					// all hash done, start writing back the results
					windex := 0.U
					state := s_write
				} .otherwise {
					// back to process the next content in the message
					state := s_idle
				}
			}
		}
		is(s_write) {
			// set up writing request signals
			io.dmem_req_val := windex < hash_size_words.U
			io.dmem_req_addr:= hash_addr + (windex << 3.U)
			io.dmem_req_tag := round_size_words.U + windex
			io.dmem_req_cmd := M_XWR

			when (io.dmem_req_rdy) {
				windex := windex + 1.U
			}

			// there is a response from memory
			when (io.dmem_resp_val) {
				// this is a response to a write
				when(dmem_resp_tag_reg(4,0) >= round_size_words.U) {
					writes_done(dmem_resp_tag_reg(4,0) - round_size_words.U) := true.B
				}
			}
			when (writes_done.reduce(_&&_)) {
				// all the writes are done
				// reset
				busy := false.B
				writes_done := VecInit(Seq.fill(hash_size_words){false.B})
				windex := hash_size_words.U
				rindex := (rounds+1).U
				msg_addr := 0.U
				hash_addr := 0.U
				msg_len := 0.U
				hashed := 0.U
				read := 0.U
				buffer_valid := false.B
				buffer_count := 0.U
				io.init := true.B
				state := s_idle
			} .otherwise {
				// not done yet, continue writing
				state := s_write
			}
		}
	} // end swith
}

