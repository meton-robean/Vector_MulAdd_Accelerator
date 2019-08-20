package lutrom

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.iotesters.PeekPokeTester
import chisel3._

class LUTROMTest(c: LUTROM) extends PeekPokeTester(c) {


  // Legend:
  // c.io.req.bits.v_mem : The input v_mem that is used to figure out what slope and/or offset value
  // c.io.req.bits.curve_select : The input curve_select that decides what curve you want
  // c.io.req.bits.slope : The output slope that is the nearest slope for the input v_mem for the selected curve
  // c.io.req.bits.offset : The output offset that is the nearest offset for the input v_mem for the selected curve
  // c.io.req.ready : The output req.ready that is true when LUT_ROM is ready to recieve a request
  // c.io.req.valid : The input req.valid that is true when a request is sent to LUT_ROM
  // c.io.resp.ready : The input resp.ready that is true when the LUT_ROM can respond back
  // c.io.resp.valid : The output resp.valid that is true when the LUT_ROM has a valid response

  // Testing Curve #0
  // New v_mem and curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"h00000000".U(32.W))
  poke(c.io.req.bits.curve_select, 0)
  expect(c.io.req.ready,true.B)
  poke(c.io.req.valid,true.B)
  poke(c.io.resp.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h3ffa4745".U(32.W))
  expect(c.io.resp.bits.offset,"h3f7c9321".U(32.W))

  step(1)
  // Same v_mem and curve_select values
  // s_idle
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h3ffa4745".U(32.W))
  expect(c.io.resp.bits.offset,"h3f7c9321".U(32.W))

  step(1)
  // Same v_mem and curve_select values, don't trigger request
  // s_idle
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  poke(c.io.req.valid,false.B)
  step(1)
  //s_idle
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_idle
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)

  // Now request it
  poke(c.io.req.valid,true.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h3ffa4745".U(32.W))
  expect(c.io.resp.bits.offset,"h3f7c9321".U(32.W))

  step(1)
  // New v_mem and same curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"hbdcccccd".U(32.W))
  poke(c.io.req.bits.curve_select, 0)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h3e879d0a".U(32.W))
  expect(c.io.resp.bits.offset,"h3cae642c".U(32.W))

  step(1)
  // New v_mem and same curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"h3ca3d70a".U(32.W))
  poke(c.io.req.bits.curve_select, 0)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h00000000".U(32.W))
  expect(c.io.resp.bits.offset,"h3f7fd567".U(32.W))

  step(1)
  // New v_mem and same curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"hbd4ccccd".U(32.W))
  poke(c.io.req.bits.curve_select, 0)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h40a3cd36".U(32.W))
  expect(c.io.resp.bits.offset,"h3e983559".U(32.W))

  step(1)
  // Same v_mem and curve_select values
  // s_idle
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h40a3cd36".U(32.W))
  expect(c.io.resp.bits.offset,"h3e983559".U(32.W))

  step(1)
  // Testing Curve #2
  // New v_mem and curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"hbd158106".U(32.W))
  poke(c.io.req.bits.curve_select, 2)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"hc1907ae1".U(32.W))
  expect(c.io.resp.bits.offset,"h3e9b089a".U(32.W))

  step(1)
  // Testing Curve #27
  // New v_mem and curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"hbdcccccd".U(32.W))
  poke(c.io.req.bits.curve_select, 27)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  poke(c.io.resp.ready,false.B)
  step(1)
  // s_resp and not ready to receive response
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"hc01b4880".U(32.W))
  expect(c.io.resp.bits.offset,"h3fa04a62".U(32.W))
  poke(c.io.resp.ready,true.B)

  // s_resp and now ready to receive response
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"hc01b4880".U(32.W))
  expect(c.io.resp.bits.offset,"h3fa04a62".U(32.W))

  step(1)
  // New v_mem and same curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"h3d4ccccd".U(32.W))
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h00000000".U(32.W))
  expect(c.io.resp.bits.offset,"h4117db8c".U(32.W))

  step(1)
  // New v_mem and same curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"h3c23d70a".U(32.W))
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"h41f38f5c".U(32.W))
  expect(c.io.resp.bits.offset,"h410d8460".U(32.W))

  step(1)
  // Testing Curve #23
  // New v_mem and curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"hbd23d70a".U(32.W))
  poke(c.io.req.bits.curve_select, 23)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"hbe646499".U(32.W))
  expect(c.io.resp.bits.offset,"h3bbcc6c5".U(32.W))

  step(1)
  // Testing Curve #12
  // New v_mem and curve_select values
  // s_idle
	poke(c.io.req.bits.v_mem,"h00000000".U(32.W))
  poke(c.io.req.bits.curve_select, 12)
  expect(c.io.req.ready,true.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_sub
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_xor
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_index
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_set_o_s
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,false.B)
  step(1)
  // s_resp
  expect(c.io.req.ready,false.B)
  expect(c.io.resp.valid,true.B)
  expect(c.io.resp.bits.slope,"hbbc56275".U(32.W))
  expect(c.io.resp.bits.offset,"h3a3aa583".U(32.W))

}

