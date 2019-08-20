// Used to test the functionality of the RoCC Interface
package multState

import chisel3._
import chisel3.util._

// A simple module that takes in two numbers and multiples them together
class MultState extends Module{
        val io = IO(new Bundle {
                val req = Flipped(Decoupled(new Bundle {
                        val one = Input(UInt(64.W))
                        val two = Input(UInt(64.W))
                }))
                val resp = Decoupled(new Bundle {
                        val out = Output(UInt(64.W))
                })
        })

        val s_idle :: s_mult :: s_m_wait :: s_wait :: s_resp :: Nil = Enum(5)
        val state  = RegInit(s_idle)

        val first  = RegInit(10.U(64.W))
        val second = RegInit(11.U(64.W))
        val output = RegInit(12.U(64.W))

        io.req.ready := (state === s_idle)
        io.resp.valid := (state === s_resp)
        io.resp.bits.out := output

        when(io.req.fire()){
            state := s_mult
            first := io.req.bits.one
            second := io.req.bits.two
        }

        //printf("first: %d second: %d output: %d\n",first,second,output);

        when(state === s_mult){
            state := s_m_wait
            output := first * second
        }

        when(state === s_m_wait){
            state := s_wait
        }

        when(state === s_wait){
            state := s_resp
        }

        when(io.resp.fire()){
            state := s_idle
        }
        
        /*
        printf("req: ready %d   valid %d -- resp:  ready %d  valid %d -- %d * %d = %d\n",
        io.req.ready, io.req.valid, io.resp.ready, io.resp.valid, io.req.bits.one, io.req.bits.two, io.resp.bits.out)
        */


}
