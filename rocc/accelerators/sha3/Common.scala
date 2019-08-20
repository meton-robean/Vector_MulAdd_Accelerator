package sha3

import chisel3._
//import scala.util.Random
//#define ROTL64(x, y) (((x) << (y)) | ((x) >> (64 - (y))))
object common {
  def ROTL(x: UInt, y: UInt, w: UInt) = (((x) << (y)) | ((x) >> (w - (y))))
}
