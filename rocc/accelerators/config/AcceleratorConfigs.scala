// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package freechips.rocketchip.system

import Chisel._
//import freechips.rocketchip.config.Config
import freechips.rocketchip.subsystem._
//import freechips.rocketchip.devices.debug.{IncludeJtagDTM, JtagDTMKey}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

//import freechips.rocketchip.util.InOrderArbiter

import mult._
import lutrom._
import multState._
//import spmv._  //@cmt 
import sha3._  //@cmt 
import kernel._


// Multiply Accelerator
// class MultAcceleratorConfig extends Config(
//   new WithMultAccelerator ++ new DefaultConfig)


// class WithMultAccelerator extends Config((site, here, up) => {
//       case RocketTilesKey => up(RocketTilesKey, site).map { r =>
//         r.copy(rocc = Seq(
//           RoCCParams(
//             opcodes = OpcodeSet.custom0,
//             generator = (p: Parameters) => {
//               val multiplier = LazyModule(new mult.MultAccelerator()(p))
//               multiplier})
//           ))
//       }
// })


// LUTROM Accelerator
// class LUTROMAcceleratorConfig extends Config(
//   new WithLUTROMAccelerator ++ new DefaultConfig)


// class WithLUTROMAccelerator extends Config((site, here, up) => {
//       case RocketTilesKey => up(RocketTilesKey, site).map { r =>
//         r.copy(rocc = Seq(
//           RoCCParams(
//             opcodes = OpcodeSet.custom0,
//             generator = (p: Parameters) => {
//               val lutromacc = LazyModule(new lutrom.LUTROMAccelerator()(p))
//               lutromacc})
//           ))
//       }
// })

// Multiply State Accelerator
class MultStateAcceleratorConfig extends Config(
  new WithMultStateAccelerator ++ new DefaultConfig)

class WithMultStateAccelerator extends Config((site, here, up) => {
      case RocketTilesKey => up(RocketTilesKey, site).map { r =>
        r.copy(rocc = Seq(
          RoCCParams(
            opcodes = OpcodeSet.custom0,
            generator = (p: Parameters) => {
              val multiplierState = LazyModule(new multState.MultStateAccelerator()(p))
              multiplierState})
          ))
      }
})





/*
//spmv 稀疏矩阵加速器
class SpmvAccelConfig extends Config(new WithSpmvAccel ++ new DefaultConfig)

class WithSpmvAccel extends Config((site, here, up) => {
	case WidthP => 32
	case NumPEs => 8
	case RocketTilesKey => up(RocketTilesKey, site).map { r =>
	r.copy(rocc = Seq(
	RoCCParams(
	opcodes = OpcodeSet.custom0,
	generator = (p: Parameters) => {
	  val spmv = LazyModule(new SpmvAccel()(p) )
	  spmv})
	))
	}

})

*/

//sha3 accelerator
// class Sha3AccelConfig extends Config(new WithSha3Accel ++ new DefaultConfig)

// class WithSha3Accel extends Config((site, here, up) => {
// 	case WidthP => 64
// 	case Stages => 1
// 	case FastMem => false 
// 	case BufferSram => false
// 	case RocketTilesKey => up(RocketTilesKey, site).map { r =>
// 	r.copy(rocc = Seq(
// 	RoCCParams(
// 	opcodes = OpcodeSet.custom0,
// 	generator = (p: Parameters) => {
// 	  val sha3 = LazyModule(new Sha3Accel()(p))
// 	  sha3})
// 	))
// 	}
// })



//kernel

class VecMulAddAccelConfig extends Config(new WithVecMulAddAccel ++ new DefaultConfig)

class WithVecMulAddAccel extends Config((site, here, up) => {
      case RocketTilesKey => up(RocketTilesKey, site).map { r =>
        r.copy(rocc = Seq(
          RoCCParams(
            opcodes = OpcodeSet.custom0,
            generator = (p: Parameters) => {
              val vecmuladd = LazyModule(new kernel.VecMulAddAccel()(p))
              vecmuladd})
          ))
      }
})



//crc
class CrcAccelConfig extends Config(new WithCrcAccel ++ new DefaultConfig)

class WithCrcAccel extends Config((site, here, up) => {
      case RocketTilesKey => up(RocketTilesKey, site).map { r =>
        r.copy(rocc = Seq(
          RoCCParams(
            opcodes = OpcodeSet.custom0,
            generator = (p: Parameters) => {
              val crcd = LazyModule(new crc.CrcAccel()(p))
              crcd})
          ))
      }
})