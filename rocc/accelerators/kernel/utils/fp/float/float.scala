package float

import chisel3._
import chisel3.util._

import floatingpoint._

class Float() extends FloatingPoint(8, 23) {

    override def cloneType = (new Float()).asInstanceOf[this.type]

}
