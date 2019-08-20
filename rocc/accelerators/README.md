# RoCC Accelerators
This is where you should put the `chisel` code for your accelerators. The following two templates along with the included `MultAccelerator` and `MultStateAccelerator` should provide good starting points for making your own accelerator.

If you want to create an accelerator module that is used by the following RoCC Accelerator, go to this [README.md](../sbt/README.md).

### RoCC Accelerator Template
Suppose the accelerator you want to make is called `MyAcceleratorAccelerator`. First make a directory (if it wasn't already created following this [README.md](../sbt/README.md)) for your scala files:

```bash
mkdir myAccelerator
cd myAccelerator
```

Now make a new scala file, `myAcceleratorAccelerator.scala` using the following as a template:
```scala
package myaccelerator

import Chisel._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._

class  MyAcceleratorAccelerator(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new MyAcceleratorAcceleratorModule(this)
}

class MyAcceleratorAcceleratorModule(outer: MyAccelerator, n: Int = 4)(implicit p: Parameters) extends LazyRoCCModule(outer)
  with HasCoreParameters {
  // Your accelerator code goes here
  // See multState/MultStateAccelerator.scala or mult/MultAccelerator.scala for an example
}
```
#### Things you need to change
  - `package myaccelerator` - Change `myaccelerator` to whatever you want to name the package your accelerator resides in, preferably all lowercase.
  - `MyAcceleratorAccelerator` - Change `MyAccelerator` to whatever you want to name your accelerator and keep the `Accelerator` suffix. Note, this name is used twice in this template. This name will be used again later in your [AcceleratorConfigs.scala](config/AcceleratorConfigs.scala).
  - `MyAcceleratorAcceleratorModule` - Change `MyAccelerator` to whatever you want to name your accelerator and keep the `AcceleratorModule` suffix. Note, this name is used twice in this template
  - You'll also need to add your accelerator code to the body of class `MyAcceleratorAcceleratorModule`. See [MultStateAccelerator.scala](multState/MultStateAccelerator.scala) and [MultAccelerator.scala](mult/MultAccelerator.scala) for two working examples.

### RoCC Accelerator Config Template
Continuing the example for creating `MyAcceleratorAccelerator`, add a configuration in [config/AcceleratorConfigs.scala](config/AcceleratorConfigs.scala) using the following template:
```scala
import myaccelerator._ // goes with the other imports

class MyAcceleratorConfig extends Config(
  new WithMyAccelerator ++ new DefaultConfig)


class WithMyAccelerator extends Config((site, here, up) => {
      case RocketTilesKey => up(RocketTilesKey, site).map { r =>
        r.copy(rocc = Seq(
          RoCCParams(
            opcodes = OpcodeSet.custom0,
            generator = (p: Parameters) => {
              val acc = LazyModule(new myaccelerator.MyAcceleratorAccelerator()(p))
              acc})
          ))
      }
})
```
#### Things you need to change
  - `myaccelerator` - CChange `myaccelerator` to what you decided to name the package your accelerator resides in, preferably all lowercase.
  - `MyAcceleratorConfig` - Change `MyAccelerator` while keeping the suffix `Config`. This is the name of the config you will use when creating a `ZynqFPGA...` config in [ZynqConfigs.scala](../zynq/ZynqConfigs.scala).
  - `WithMyAccelerator` - Change `MyAccelerator` while keeping the prefix `With`. Note that this name appears twice in the template.
  - `opcodes = OpcodeSet.custom0` - Change `custom0` to match up with the opcode you plan on using for your RoCC instruction that accesses your accelerator (can be `custom0` through `custom3`).
  - `val acc = LazyModule(new myaccelerator.MyAcceleratorAccelerator()(p))` - `acc` can be any name you want. Change `myaccelerator` to the package you declared your accelerator in. Change `MyAccelerator` to the name of your accelerator class and keep the `Accelerator` suffix.
  - `acc` - make sure this name matches whatever name you used for the `LazyModule` in the previous line.

### Putting your accelerator on a FPGA
Follow the instructions in [zynq](../zynq) to configure a `rocket-chip` that can be put on an FPGA with your accelerator.

### Testing your accelerator using the Verilator
Follow the instructions in [verilator-tests](../../verilator-tests) to test your accelerator using the cycle-accurate Verilator before putting your accelerator on the FPGA.

Note: you must create the zynq configurations mentioned in [Putting your accelerator on a FPGA](#Putting-your-accelerator-on-a-FPGA) before testing your accelerator using the Verilator.