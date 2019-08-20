package lutrom

import chisel3._
import chisel3.util._
import lutrom._

class LUTCurveMux28 extends Module{
   val io = IO(new Bundle {
           val curveSelect = Input(UInt(32.W))
           val curve0 = Input(new LUTCurve())
           val curve1 = Input(new LUTCurve())
           val curve2 = Input(new LUTCurve())
           val curve3 = Input(new LUTCurve())
           val curve4 = Input(new LUTCurve())
           val curve5 = Input(new LUTCurve())
           val curve6 = Input(new LUTCurve())
           val curve7 = Input(new LUTCurve())
           val curve8 = Input(new LUTCurve())
           val curve9 = Input(new LUTCurve())
           val curve10 = Input(new LUTCurve())
           val curve11 = Input(new LUTCurve())
           val curve12 = Input(new LUTCurve())
           val curve13 = Input(new LUTCurve())
           val curve14 = Input(new LUTCurve())
           val curve15 = Input(new LUTCurve())
           val curve16 = Input(new LUTCurve())
           val curve17 = Input(new LUTCurve())
           val curve18 = Input(new LUTCurve())
           val curve19 = Input(new LUTCurve())
           val curve20 = Input(new LUTCurve())
           val curve21 = Input(new LUTCurve())
           val curve22 = Input(new LUTCurve())
           val curve23 = Input(new LUTCurve())
           val curve24 = Input(new LUTCurve())
           val curve25 = Input(new LUTCurve())
           val curve26 = Input(new LUTCurve())
           val curve27 = Input(new LUTCurve())
           val curveOut = Output(new LUTCurve()) 
   })

   io.curveOut := io.curve0

   switch(io.curveSelect){
      is(0.U){
        io.curveOut := io.curve0 
      }
      is(1.U){
        io.curveOut := io.curve1
      }
      is(2.U){
        io.curveOut := io.curve2
      }
      is(3.U){
        io.curveOut := io.curve3
      }
      is(4.U){
        io.curveOut := io.curve4
      }
      is(5.U){
        io.curveOut := io.curve5
      }
      is(6.U){
        io.curveOut := io.curve6
      }
      is(7.U){
        io.curveOut := io.curve7
      }
      is(8.U){
        io.curveOut := io.curve8
      }
      is(9.U){
        io.curveOut := io.curve9
      }
      is(10.U){
        io.curveOut := io.curve10
      }
      is(11.U){
        io.curveOut := io.curve11
      }
      is(12.U){
        io.curveOut := io.curve12
      }
      is(13.U){
        io.curveOut := io.curve13
      }
      is(14.U){
        io.curveOut := io.curve14
      }
      is(15.U){
        io.curveOut := io.curve15
      }
      is(16.U){
        io.curveOut := io.curve16
      }
      is(17.U){
        io.curveOut := io.curve17
      }
      is(18.U){
        io.curveOut := io.curve18
      }
      is(19.U){
        io.curveOut := io.curve19
      }
      is(20.U){
        io.curveOut := io.curve20
      }
      is(21.U){
        io.curveOut := io.curve21
      }
      is(22.U){
        io.curveOut := io.curve22
      }
      is(23.U){
        io.curveOut := io.curve23
      }
      is(24.U){
        io.curveOut := io.curve24
      }
      is(25.U){
        io.curveOut := io.curve25
      }
      is(26.U){
        io.curveOut := io.curve26
      }
      is(27.U){
        io.curveOut := io.curve27
      }
   }
  
} 