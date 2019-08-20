package multState

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.iotesters.PeekPokeTester
import chisel3._

class MultStateTest(c: MultState) extends PeekPokeTester(c) {

     // s_idle
     poke(c.io.req.bits.one,5.U)
     poke(c.io.req.bits.two,10.U)
     poke(c.io.req.valid,true.B)
     expect(c.io.req.ready,true.B)
     expect(c.io.resp.valid,false.B)
     step(1)
     // s_mult
     poke(c.io.resp.ready,true.B)
     expect(c.io.req.ready,false.B)
     expect(c.io.resp.valid,false.B)
     step(1)
     //s_m_wait
     expect(c.io.req.ready,false.B)
     expect(c.io.resp.valid,false.B)
     step(1)
     //s_wait
     expect(c.io.req.ready,false.B)
     expect(c.io.resp.valid,false.B)
     step(1)
     expect(c.io.req.ready,false.B)
     expect(c.io.resp.valid,true.B)
     expect(c.io.resp.bits.out,50.U)
     //s_resp
     step(1)
     //s_idle
     expect(c.io.req.ready,true.B)
     expect(c.io.resp.valid,false.B)
}

