package lutrom

import chisel3._
import chisel3.util._


class LUTROM extends Module{
   // Declare inputs and outputs
   val io = IO(new Bundle {
      // Decoupled inputs must be Flipped
      val req = Flipped(Decoupled(new Bundle {
         val curve_select = Input(UInt(5.W))
         val v_mem = Input(UInt(32.W))
      }))
      val resp = Decoupled(new Bundle {
         val slope = Output(UInt(32.W))
         val offset = Output(UInt(32.W))
      })
   })

   printf("req: ready %d   valid %d -- resp:  ready %d  valid %d --v:%x c:%x => o:%x s:%x\n",
      io.req.ready, io.req.valid, io.resp.ready, io.resp.valid, io.req.bits.v_mem, io.req.bits.curve_select, io.resp.bits.offset, io.resp.bits.slope)

   // // Always reg inputs
   // val v_mem = Reg(UInt(32.W))
   // val curve_select = Reg(UInt(32.W))

   // Declare and initialize registers to keep state between states
   val comparison = RegInit(VecInit(Seq.fill(32){ 0.U(1.W) }))  //Wire(Vec(32, UInt(1.W)))
   val xor_comparison = RegInit(VecInit(Seq.fill(31){ 0.U(1.W) }))  //Wire(Vec(31, UInt(1.W)))
   val greater_than_all = RegInit(false.B)
   val interp_index = RegInit(0.U(5.W))

   // Declare and initialize registers for caching results when inputs don't change
   val valid_cache = RegInit(false.B)
   // Make sure old_v_mem and old_curve_select are values we don't expect to ever see
   val old_v_mem = RegInit("h41200000".U(32.W))
   val old_curve_select = RegInit(28.U(5.W))
   val old_slope = Reg(UInt(32.W))
   val old_offset = Reg(UInt(32.W))

   // Declare and initialize states
   val s_idle :: s_sub :: s_xor :: s_index :: s_set_o_s :: s_resp :: Nil = Enum(6)
   val state = RegInit(s_idle)

   // LUT is ready for input when it is in the idle state
   io.req.ready := (state === s_idle)

   // Indicates that the LUT is done processing and its output values can be used
   io.resp.valid := (state === s_resp)
   io.resp.bits.slope := old_slope
   io.resp.bits.offset := old_offset

   // io.req.fire() is true when both io.req.ready and io.req.valid are true.
   // this happens when the LUT is ready for input and the caller has indicated
   // its inputs to the LUT are valid.
   when (io.req.fire()) {
      // v_mem := io.req.bits.v_mem
      // curve_select := io.req.bits.curve_select
      // when (!valid_cache | (io.req.bits.v_mem =/= old_v_mem) | (io.req.bits.curve_select =/= old_curve_select)) {
      when ((io.req.bits.v_mem =/= old_v_mem) | (io.req.bits.curve_select =/= old_curve_select)) {
         // When inputs change, start curve lookup process
         state := s_sub
      } .otherwise {
         // When inputs don't change
         state := s_resp
      }
      // state := s_sub
   }

   // Load in hard-coded curves
   // JVM complains when too many of these curves are initialized in the same
   // file so we had to split them to 2 files
   val curves1 = Module(new LUTCurves1())
   val curves2 = Module(new LUTCurves2())

   // Initialize inputs to mux for selecting what curve we look at
   val mux = Module(new LUTCurveMux28())
   mux.io.curveSelect := io.req.bits.curve_select
   mux.io.curve0 := curves1.io.curve0
   mux.io.curve1 := curves1.io.curve1
   mux.io.curve2 := curves1.io.curve2
   mux.io.curve3 := curves1.io.curve3
   mux.io.curve4 := curves1.io.curve4
   mux.io.curve5 := curves1.io.curve5
   mux.io.curve6 := curves1.io.curve6
   mux.io.curve7 := curves1.io.curve7
   mux.io.curve8 := curves1.io.curve8
   mux.io.curve9 := curves1.io.curve9
   mux.io.curve10 := curves1.io.curve10
   mux.io.curve11 := curves1.io.curve11
   mux.io.curve12 := curves1.io.curve12
   mux.io.curve13 := curves1.io.curve13
   mux.io.curve14 := curves2.io.curve14
   mux.io.curve15 := curves2.io.curve15
   mux.io.curve16 := curves2.io.curve16
   mux.io.curve17 := curves2.io.curve17
   mux.io.curve18 := curves2.io.curve18
   mux.io.curve19 := curves2.io.curve19
   mux.io.curve20 := curves2.io.curve20
   mux.io.curve21 := curves2.io.curve21
   mux.io.curve22 := curves2.io.curve22
   mux.io.curve23 := curves2.io.curve23
   mux.io.curve24 := curves2.io.curve24
   mux.io.curve25 := curves2.io.curve25
   mux.io.curve26 := curves2.io.curve26
   mux.io.curve27 := curves2.io.curve27

   // Setup curve selecting mux output
   val selected_curve = Wire(new LUTCurve())
   selected_curve := mux.io.curveOut

   // Parallel "subtractions" to determine which stored v_mem values are greater
   // than and less than io.req.bits.v_mem
   when (state === s_sub) {
      val fpgt0 = Module(new FPGreaterThan())
      fpgt0.io.greater := io.req.bits.v_mem
      fpgt0.io.lesser := selected_curve.v_mem(0)
      comparison(0) := fpgt0.io.greater_than

      val fpgt1 = Module(new FPGreaterThan())
      fpgt1.io.greater := io.req.bits.v_mem
      fpgt1.io.lesser := selected_curve.v_mem(1)
      comparison(1) := fpgt1.io.greater_than

      val fpgt2 = Module(new FPGreaterThan())
      fpgt2.io.greater := io.req.bits.v_mem
      fpgt2.io.lesser := selected_curve.v_mem(2)
      comparison(2) := fpgt2.io.greater_than

      val fpgt3 = Module(new FPGreaterThan())
      fpgt3.io.greater := io.req.bits.v_mem
      fpgt3.io.lesser := selected_curve.v_mem(3)
      comparison(3) := fpgt3.io.greater_than

      val fpgt4 = Module(new FPGreaterThan())
      fpgt4.io.greater := io.req.bits.v_mem
      fpgt4.io.lesser := selected_curve.v_mem(4)
      comparison(4) := fpgt4.io.greater_than

      val fpgt5 = Module(new FPGreaterThan())
      fpgt5.io.greater := io.req.bits.v_mem
      fpgt5.io.lesser := selected_curve.v_mem(5)
      comparison(5) := fpgt5.io.greater_than

      val fpgt6 = Module(new FPGreaterThan())
      fpgt6.io.greater := io.req.bits.v_mem
      fpgt6.io.lesser := selected_curve.v_mem(6)
      comparison(6) := fpgt6.io.greater_than

      val fpgt7 = Module(new FPGreaterThan())
      fpgt7.io.greater := io.req.bits.v_mem
      fpgt7.io.lesser := selected_curve.v_mem(7)
      comparison(7) := fpgt7.io.greater_than

      val fpgt8 = Module(new FPGreaterThan())
      fpgt8.io.greater := io.req.bits.v_mem
      fpgt8.io.lesser := selected_curve.v_mem(8)
      comparison(8) := fpgt8.io.greater_than

      val fpgt9 = Module(new FPGreaterThan())
      fpgt9.io.greater := io.req.bits.v_mem
      fpgt9.io.lesser := selected_curve.v_mem(9)
      comparison(9) := fpgt9.io.greater_than

      val fpgt10 = Module(new FPGreaterThan())
      fpgt10.io.greater := io.req.bits.v_mem
      fpgt10.io.lesser := selected_curve.v_mem(10)
      comparison(10) := fpgt10.io.greater_than

      val fpgt11 = Module(new FPGreaterThan())
      fpgt11.io.greater := io.req.bits.v_mem
      fpgt11.io.lesser := selected_curve.v_mem(11)
      comparison(11) := fpgt11.io.greater_than

      val fpgt12 = Module(new FPGreaterThan())
      fpgt12.io.greater := io.req.bits.v_mem
      fpgt12.io.lesser := selected_curve.v_mem(12)
      comparison(12) := fpgt12.io.greater_than

      val fpgt13 = Module(new FPGreaterThan())
      fpgt13.io.greater := io.req.bits.v_mem
      fpgt13.io.lesser := selected_curve.v_mem(13)
      comparison(13) := fpgt13.io.greater_than

      val fpgt14 = Module(new FPGreaterThan())
      fpgt14.io.greater := io.req.bits.v_mem
      fpgt14.io.lesser := selected_curve.v_mem(14)
      comparison(14) := fpgt14.io.greater_than

      val fpgt15 = Module(new FPGreaterThan())
      fpgt15.io.greater := io.req.bits.v_mem
      fpgt15.io.lesser := selected_curve.v_mem(15)
      comparison(15) := fpgt15.io.greater_than

      val fpgt16 = Module(new FPGreaterThan())
      fpgt16.io.greater := io.req.bits.v_mem
      fpgt16.io.lesser := selected_curve.v_mem(16)
      comparison(16) := fpgt16.io.greater_than

      val fpgt17 = Module(new FPGreaterThan())
      fpgt17.io.greater := io.req.bits.v_mem
      fpgt17.io.lesser := selected_curve.v_mem(17)
      comparison(17) := fpgt17.io.greater_than

      val fpgt18 = Module(new FPGreaterThan())
      fpgt18.io.greater := io.req.bits.v_mem
      fpgt18.io.lesser := selected_curve.v_mem(18)
      comparison(18) := fpgt18.io.greater_than

      val fpgt19 = Module(new FPGreaterThan())
      fpgt19.io.greater := io.req.bits.v_mem
      fpgt19.io.lesser := selected_curve.v_mem(19)
      comparison(19) := fpgt19.io.greater_than

      val fpgt20 = Module(new FPGreaterThan())
      fpgt20.io.greater := io.req.bits.v_mem
      fpgt20.io.lesser := selected_curve.v_mem(20)
      comparison(20) := fpgt20.io.greater_than

      val fpgt21 = Module(new FPGreaterThan())
      fpgt21.io.greater := io.req.bits.v_mem
      fpgt21.io.lesser := selected_curve.v_mem(21)
      comparison(21) := fpgt21.io.greater_than

      val fpgt22 = Module(new FPGreaterThan())
      fpgt22.io.greater := io.req.bits.v_mem
      fpgt22.io.lesser := selected_curve.v_mem(22)
      comparison(22) := fpgt22.io.greater_than

      val fpgt23 = Module(new FPGreaterThan())
      fpgt23.io.greater := io.req.bits.v_mem
      fpgt23.io.lesser := selected_curve.v_mem(23)
      comparison(23) := fpgt23.io.greater_than

      val fpgt24 = Module(new FPGreaterThan())
      fpgt24.io.greater := io.req.bits.v_mem
      fpgt24.io.lesser := selected_curve.v_mem(24)
      comparison(24) := fpgt24.io.greater_than

      val fpgt25 = Module(new FPGreaterThan())
      fpgt25.io.greater := io.req.bits.v_mem
      fpgt25.io.lesser := selected_curve.v_mem(25)
      comparison(25) := fpgt25.io.greater_than

      val fpgt26 = Module(new FPGreaterThan())
      fpgt26.io.greater := io.req.bits.v_mem
      fpgt26.io.lesser := selected_curve.v_mem(26)
      comparison(26) := fpgt26.io.greater_than

      val fpgt27 = Module(new FPGreaterThan())
      fpgt27.io.greater := io.req.bits.v_mem
      fpgt27.io.lesser := selected_curve.v_mem(27)
      comparison(27) := fpgt27.io.greater_than

      val fpgt28 = Module(new FPGreaterThan())
      fpgt28.io.greater := io.req.bits.v_mem
      fpgt28.io.lesser := selected_curve.v_mem(28)
      comparison(28) := fpgt28.io.greater_than

      val fpgt29 = Module(new FPGreaterThan())
      fpgt29.io.greater := io.req.bits.v_mem
      fpgt29.io.lesser := selected_curve.v_mem(29)
      comparison(29) := fpgt29.io.greater_than

      val fpgt30 = Module(new FPGreaterThan())
      fpgt30.io.greater := io.req.bits.v_mem
      fpgt30.io.lesser := selected_curve.v_mem(30)
      comparison(30) := fpgt30.io.greater_than

      val fpgt31 = Module(new FPGreaterThan())
      fpgt31.io.greater := io.req.bits.v_mem
      fpgt31.io.lesser := selected_curve.v_mem(31)
      comparison(31) := fpgt31.io.greater_than

      state := s_xor
   }

   // xor adjacent states of earlier subtraction to determine where
   // values in comparison switch from 0 to 1
   when (state === s_xor) {
      xor_comparison(0) := comparison(0) ^ comparison(1)
      xor_comparison(1) := comparison(1) ^ comparison(2)
      xor_comparison(2) := comparison(2) ^ comparison(3)
      xor_comparison(3) := comparison(3) ^ comparison(4)
      xor_comparison(4) := comparison(4) ^ comparison(5)
      xor_comparison(5) := comparison(5) ^ comparison(6)
      xor_comparison(6) := comparison(6) ^ comparison(7)
      xor_comparison(7) := comparison(7) ^ comparison(8)
      xor_comparison(8) := comparison(8) ^ comparison(9)
      xor_comparison(9) := comparison(9) ^ comparison(10)
      xor_comparison(10) := comparison(10) ^ comparison(11)
      xor_comparison(11) := comparison(11) ^ comparison(12)
      xor_comparison(12) := comparison(12) ^ comparison(13)
      xor_comparison(13) := comparison(13) ^ comparison(14)
      xor_comparison(14) := comparison(14) ^ comparison(15)
      xor_comparison(15) := comparison(15) ^ comparison(16)
      xor_comparison(16) := comparison(16) ^ comparison(17)
      xor_comparison(17) := comparison(17) ^ comparison(18)
      xor_comparison(18) := comparison(18) ^ comparison(19)
      xor_comparison(19) := comparison(19) ^ comparison(20)
      xor_comparison(20) := comparison(20) ^ comparison(21)
      xor_comparison(21) := comparison(21) ^ comparison(22)
      xor_comparison(22) := comparison(22) ^ comparison(23)
      xor_comparison(23) := comparison(23) ^ comparison(24)
      xor_comparison(24) := comparison(24) ^ comparison(25)
      xor_comparison(25) := comparison(25) ^ comparison(26)
      xor_comparison(26) := comparison(26) ^ comparison(27)
      xor_comparison(27) := comparison(27) ^ comparison(28)
      xor_comparison(28) := comparison(28) ^ comparison(29)
      xor_comparison(29) := comparison(29) ^ comparison(30)
      xor_comparison(30) := comparison(30) ^ comparison(31)
      greater_than_all := (comparison(31) === 1.U)
      state := s_index
   }

   // Find the interpolation index for the selected curve
   when (state === s_index) {
      switch(xor_comparison.asUInt){
         is(0.U){
            when(greater_than_all){
               interp_index := 31.U
            }.otherwise{
               interp_index := 0.U
            }
         }
         is(1.U){
            interp_index := 0.U
         }
         is(2.U){
            interp_index := 1.U
         }
         is(4.U){
            interp_index := 2.U
         }
         is(8.U){
            interp_index := 3.U
         }
         is(16.U){
            interp_index := 4.U
         }
         is(32.U){
            interp_index := 5.U
         }
         is(64.U){
            interp_index := 6.U
         }
         is(128.U){
            interp_index := 7.U
         }
         is(256.U){
            interp_index := 8.U
         }
         is(512.U){
            interp_index := 9.U
         }
         is(1024.U){
            interp_index := 10.U
         }
         is(2048.U){
            interp_index := 11.U
         }
         is(4096.U){
            interp_index := 12.U
         }
         is(8192.U){
            interp_index := 13.U
         }
         is(16384.U){
            interp_index := 14.U
         }
         is(32768.U){
            interp_index := 15.U
         }
         is(65536.U){
            interp_index := 16.U
         }
         is(131072.U){
            interp_index := 17.U
         }
         is(262144.U){
            interp_index := 18.U
         }
         is(524288.U){
            interp_index := 19.U
         }
         is(1048576.U){
            interp_index := 20.U
         }
         is(2097152.U){
            interp_index := 21.U
         }
         is(4194304.U){
            interp_index := 22.U
         }
         is(8388608.U){
            interp_index := 23.U
         }
         is(16777216.U){
            interp_index := 24.U
         }
         is(33554432.U){
            interp_index := 25.U
         }
         is(67108864.U){
            interp_index := 26.U
         }
         is(134217728.U){
            interp_index := 27.U
         }
         is(268435456.U){
            interp_index := 28.U
         }
         is(536870912.U){
            interp_index := 29.U
         }
         is(1073741824.U){
            interp_index := 30.U
         }
      }

      state := s_set_o_s
   }

   // Set outputs based on interp_index
   // Might be able to merge with s_set_o_s
   when (state === s_set_o_s) {
      valid_cache := true.B
      old_slope := selected_curve.slope(interp_index)
      old_offset := selected_curve.offset(interp_index)
      old_v_mem := io.req.bits.v_mem
      old_curve_select := io.req.bits.curve_select

      state := s_resp
   }

   // io.resp.fire() is true when both io.resp.valid and io.resp.ready are true
   // This happens when the LUT's result is ready and the caller is ready for our result
   when (io.resp.fire()) {
      state := s_idle
   }

}