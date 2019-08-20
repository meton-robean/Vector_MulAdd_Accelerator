//see LICENSE for license
package sha3

import chisel3._
import chisel3.util._

class DpathModule(val w: Int, val s: Int) extends Module {
	//val r = 2*256
	val r = 0
	val c = 25*w - r
	val round_size_words = c/w
	val hash_size_words = 256/w

	val io = IO(new Bundle {
		val init   = Input(Bool())
		val write  = Input(Bool())
		val absorb = Input(Bool())
		val round  = Input(UInt(5.W))
		val stage  = Input(UInt(log2Ceil(s).W))
		val aindex = Input(UInt(log2Ceil(round_size_words).W))
		val message_in = Input(Bits(w.W))
		val hash_out = Output(Vec(hash_size_words, UInt(w.W)))
	})

	//val state = Vec(25, RegInit(0.U(w.W)))
	//val state = Vec(25, UInt(w.W))
	val initValues = Seq.fill(25) { 0.U(w.W) }
	val state = RegInit(VecInit(initValues))

	val theta = Module(new ThetaModule(w)).io
	val rhopi = Module(new RhoPiModule(w)).io
	val chi = Module(new ChiModule(w)).io
	val iota = Module(new IotaModule(w)).io

	//iota.round := 0.U
	//theta.state_i := VecInit(initValues)

	theta.state_i := state
	rhopi.state_i <> theta.state_o
	chi.state_i <> rhopi.state_o
	iota.state_i <> chi.state_o
	state := iota.state_o
	iota.round := io.round

	when (io.absorb) {
		state := state
		when(io.aindex < round_size_words.U) {
			state((io.aindex%5.U)*5.U+(io.aindex/5.U)) :=
				state((io.aindex%5.U)*5.U+(io.aindex/5.U)) ^ io.message_in
		}
	}

	for( i <- 0 until hash_size_words) {
		io.hash_out(i) := state(i*5)
	}

	when (io.write) {
		state := state
	}

	when (io.init) {
		state := VecInit(initValues)
	}
}
