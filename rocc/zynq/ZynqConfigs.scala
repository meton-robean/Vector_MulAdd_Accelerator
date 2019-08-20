package zynq

import chisel3._
import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink.BootROMParams
import freechips.rocketchip.rocket.{RocketCoreParams, MulDivParams, DCacheParams, ICacheParams}
import freechips.rocketchip.tile.{RocketTileParams, BuildCore, XLen}
import testchipip._

// Use this config as a base for adding accelerators to your rocket-chip
class ZynqFPGAAcceleratorConfig extends Config(new WithoutTLMonitors ++ new WithZynqAdapter ++ new WithBootROM)

// This config produces a rocket-chip for a Zynq FPGA using a custom accelerator whose config is MultAcceleratorConfig
// Configs for other accelerators will look similar to this one e.g.
// class YourConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.YourAcceleratorConfig)

// This config produces a rocket-chip for a Zynq FPGA using a custom accelerator whose config is MultAcceleratorConfig
//class ZynqFPGAMultAcceleratorConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.MultAcceleratorConfig)

// This config produces a rocket-chip for a Zynq FPGA using a custom accelerator whose config is MultStateAcceleratorConfig
class ZynqFPGAMultStateAcceleratorConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.MultStateAcceleratorConfig)

// This config produces a rocket-chip for a Zynq FPGA using a custom accelerator whose config is LUTROMAcceleratorConfig
//class ZynqFPGALUTROMAcceleratorConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.LUTROMAcceleratorConfig)

//spmv
//class ZynqFPGASpmvAccelConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.SpmvAccelConfig)

//sha3
//class ZynqFPGASha3AcceleratorConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.Sha3AcceleratorConfig)

//kernel
//class ZynqFPGAVecMulAddAccelConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.VecMulAddAccelConfig)

//crc
class ZynqFPGACrcAccelConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.CrcAccelConfig)