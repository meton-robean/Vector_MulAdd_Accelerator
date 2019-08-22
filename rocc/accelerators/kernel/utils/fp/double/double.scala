package double

import chisel3._
import chisel3.util._

import floatingpoint._

class Double() extends FloatingPoint(11, 52) {

    override def cloneType = (new Double()).asInstanceOf[this.type]

}
