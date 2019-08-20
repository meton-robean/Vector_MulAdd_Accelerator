package lutrom

import chisel3._

class FPGreaterThan extends Module{
   val io = IO(new Bundle {
           val greater = Input(UInt(32.W))
           val lesser = Input(UInt(32.W))
           val greater_than = Output(UInt(1.W))
   })

   val comparison = Reg(UInt(1.W))
   comparison := io.greater >= io.lesser
   val bit_same = Reg(UInt(1.W))
   bit_same := io.greater(31) === io.lesser(31)
   val sign = Reg(UInt(1.W))
   sign := io.greater(31)
   io.greater_than := (comparison & bit_same & !sign) | (!comparison & !bit_same) | (!comparison & bit_same & sign) 
  
} 
