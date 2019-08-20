package crc

//import Chisel._

// import freechips.rocketchip.config._
// import freechips.rocketchip.coreplex._
// import freechips.rocketchip.diplomacy._
// import freechips.rocketchip.rocket._
// import freechips.rocketchip.tilelink._
// import freechips.rocketchip.util.InOrderArbiter
import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._
import freechips.rocketchip.tile._



class CrcAccel(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new CrcAccelModule(this)
}

class CrcAccelModule(outer: CrcAccel)(implicit p: Parameters) extends LazyRoCCModule(outer)
with HasCoreParameters {
  // The parts of the command are as follows
  // inst  - the parts of the instruction itself
  //   opcode
  //   rd  - destination register number
  //   rs1 - first source register number
  //   rs2 - second source register number
  //   funct
  //   xd  - is the destination register being used?
  //   xs1 - is the first source register being used?
  //   xs2 - is the second source register being used?
  //   rs1 - the value of source register 1
  //   rs2 - the value of source register 2

  val resp_rd = RegInit(0.U(5.W))  //Reg(io.resp.bits.rd)

  // The 32 bits that determine the location of the XOR taps
  val taps = Reg(Vec(32, Bool()))
  // The CRC size (# of bits)
  val length = Reg(UInt(10.W))
  // The memory location to read for CRC calculation
  val memsrc = Reg(UInt(64.W))
  // The length (in bits) of the data to calculate the CRC across
  val memlen = Reg(UInt(32.W))

  // The current global data offset (in bits)
  // Counts from 0 to memLen
  val globalCount = Reg(UInt(32.W))
  // The current block data offset (in bits)
  // Counts down from 64 to 0
  val localIndex = Reg(UInt(16.W))

  // The initial value for the CRC
  val startVal = Reg(UInt(64.W), init = UInt(0))
  // The value to XOR the CRC with at the final stage
  val finalXOR = Reg(UInt(32.W), init = UInt(0))

  // Are the input bytes reversed?
  val flipInput = Reg(Bool(), init = false.B)
  // Is the output CRC reversed?
  val flipOutput = Reg(Bool(), init = false.B)

  // Current 32-bit CRC register
  val crcState = Reg(Vec(32, Bool()))
  // Holds 64 bits of the most recent memory request
  val recv_data = Reg(UInt(64.W))

  //  Idle,   update len, acq data, recv data, calc CRC, fill 0s, responding
  val s_idle :: s_update :: s_acq :: s_recv :: s_calc :: s_zeroes:: s_resp :: Nil = Enum(7)
  // Current state of the state machine
  val state = Reg(init = s_idle)

  // Ready to receive new command when in idle state
  io.cmd.ready := (state === s_idle)
  // Cmd response is valid when in response state
  io.resp.valid := (state === s_resp)
  // Since crcState is a vector, element 0 is the MSB, thus Cat is reversed from what it should be
  io.resp.bits.data := Reverse(Cat(crcState))
  io.resp.bits.rd := resp_rd


  // Command received from CPU
  when (io.cmd.fire()) {
    resp_rd := io.cmd.bits.inst.rd
    val funct = io.cmd.bits.inst.funct
    when (funct === UInt(0)) {
      // Update taps for CRC polynomial
      for (i <- 0 to 31) {
        taps(i) := io.cmd.bits.rs1(i)
      }
      // Update CRC length
      length := io.cmd.bits.rs2(9, 0)
      state := s_resp
    } .elsewhen (funct === UInt(1)) {
      // Update initial value and final XOR value
      startVal := io.cmd.bits.rs1
      finalXOR := io.cmd.bits.rs2
      state := s_resp
    } .elsewhen (funct === UInt(2)) {
      // Update the reversal settings
      when (io.cmd.bits.rs1 === UInt(0)) {
        flipInput := false.B
      } .otherwise {
        flipInput := true.B
      }
      when (io.cmd.bits.rs2 === UInt(0)) {
        flipOutput := false.B
      } .otherwise {
        flipOutput := true.B
      }
      state := s_resp
    } .otherwise {
      // Actually begin the CRC calculation
      memsrc := io.cmd.bits.rs1
      memlen := io.cmd.bits.rs2

      globalCount := UInt(0)
      for (i <- 0 to 31) {
        crcState(i) := false.B
      }
      state := s_acq
    }
  }

  when (state === s_acq) {
    // Memory request sent
    when (io.mem.req.fire()) {
      state := s_recv
    }
  }

  when (state === s_recv) {
    // Memory request received
    when (io.mem.resp.valid) {
      val shiftAmt = UInt(64) - length
      val shifted = startVal << shiftAmt
      val memData = io.mem.resp.bits.data ^ shifted

/*      val allBytes = new Array[UInt](8)
      for (i <- 7 to 0) {
        val currByte = memData(i*8 + 7, i*8)
        allBytes(i) := Mux(flipInput === true.B, Reverse(currByte), currByte)
      }
      val halfBytes = new Array[UInt](4)
      for (i <- 3 to 0) {
        halfBytes(i) = Cat(allBytes(i*2 + 1), allBytes(i*2))
      }
      val quarterBytes = new Array[UInt](2)
      for (i <- 1 to 0) {
        quarterBytes(i) = Cat(halfBytes(i*2 + 1), halfBytes(i*2))
      }
      val endData = Cat(quarterBytes(1), quarterBytes(0))*/

      // This is terrible, I know, but I couldn't get an Array-based implementation to work
      val byte1 = memData(63, 56)
      val byte2 = memData(55, 48)
      val byte3 = memData(47, 40)
      val byte4 = memData(39, 32)
      val byte5 = memData(31, 24)
      val byte6 = memData(23, 16)
      val byte7 = memData(15, 8)
      val byte8 = memData(7, 0)
      val finalByte1 = Mux(flipInput === true.B, Reverse(byte1), byte1)
      val finalByte2 = Mux(flipInput === true.B, Reverse(byte2), byte2)
      val finalByte3 = Mux(flipInput === true.B, Reverse(byte3), byte3)
      val finalByte4 = Mux(flipInput === true.B, Reverse(byte4), byte4)
      val finalByte5 = Mux(flipInput === true.B, Reverse(byte5), byte5)
      val finalByte6 = Mux(flipInput === true.B, Reverse(byte6), byte6)
      val finalByte7 = Mux(flipInput === true.B, Reverse(byte7), byte7)
      val finalByte8 = Mux(flipInput === true.B, Reverse(byte8), byte8)

      val endData = Cat(finalByte1, finalByte2, finalByte3, finalByte4, finalByte5, finalByte6, finalByte7, finalByte8)

      recv_data := endData
      // Reset the startVal so that subsequent memory calls don't XOR if needed
      startVal := UInt(0)
      // Starts at 64, counts down to 1, changes state when 0
      localIndex := UInt(64)
      state := s_calc
    }
  }

  when (state === s_calc) {
    // CRC calculation
    when (globalCount === memlen) {
      // Done shifting data in, proceed to shift in several 0s
      localIndex := UInt(0)
      state := s_zeroes
    } .elsewhen (localIndex === UInt(0)) {
      // Ran out of memory to shift in, grab a new block of data
      memsrc := memsrc + UInt(8)
      state := s_acq
    } .otherwise {
      // Shift in the data, 1 bit at a time
      // Uses localIndex - 1 because localIndex = 0 is the escape from the current block
      val index = localIndex - UInt(1)
      val dataBit = recv_data(index)
      val finalBit = crcState(length - UInt(1))
      // Bits 1 to 31 all depend on previous data
      for (i <- 1 to 31) {
        val bitXOR = crcState(i - 1) ^ finalBit
        crcState(i) := Mux(taps(i), bitXOR, crcState(i - 1))
      }
      // Bit 0 has no previous data, depends on current data bit
      val bitXOR = dataBit ^ finalBit
      crcState(0) := Mux(taps(0), bitXOR, dataBit)
      // Decrement the block index
      localIndex := index
      // Increment the global counter
      globalCount := globalCount + UInt(1)
    }
  }

  when (state === s_zeroes) {
    // Shifting in zeroes
    when (localIndex >= length) {
      // All done, reverse the CRC if needed (shifting if needed) and mask the data
      val finalState = Reverse(Cat(crcState))
      val shiftAmt = UInt(32) - length
      val reversed = Reverse(finalState) >> shiftAmt
      val finalVal = Mux(flipOutput === true.B, reversed, finalState)
      val mask = ~(SInt(0xFFFFFFFF) << length)
      // Get the final output value
      for (i <- 0 to 31) {
        crcState(i) := (finalVal(i) ^ finalXOR(i)) & mask(i)
      }
      // Send back to the CPU
      state := s_resp
    } .otherwise {
      // Keep shifting in 0s for the CRC length # of iterations
      // Similar structure to the CRC calculation step, kept for clarity
      val dataBit = UInt(0)
      val finalBit = crcState(length - UInt(1))
      for (i <- 1 to 31) {
        val bitXOR = crcState(i - 1) ^ finalBit
        crcState(i) := Mux(taps(i), bitXOR, crcState(i - 1))
      }
      val bitXOR = dataBit ^ finalBit
      crcState(0) := Mux(taps(0), bitXOR, dataBit)
      // Increments localIndex this time until reaching CRC length
      localIndex := localIndex + UInt(1)
    }
  }

  // Response sent back to CPU
  when (io.resp.fire()) {
    state := s_idle
  }

  io.busy := (state =/= s_idle)
  io.interrupt := Bool(false)
  io.mem.req.valid := (state === s_acq)
  io.mem.req.bits.addr := memsrc
  io.mem.req.bits.tag := memsrc(5, 0)
  io.mem.req.bits.cmd := M_XRD  
  io.mem.req.bits.typ := MT_D // D = 8 bytes, W = 4, H = 2, B = 1
  io.mem.req.bits.data := Bits(0) // we're not performing any stores...
  io.mem.req.bits.phys := Bool(false)
  io.mem.invalidate_lr := Bool(false)
}