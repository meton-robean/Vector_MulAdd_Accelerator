package lutrom

import chisel3._

class LUTCurve extends Bundle {
   val v_mem = Vec(32, UInt(32.W))
   val offset = Vec(32, UInt(32.W))
   val slope = Vec(32, UInt(32.W))
}