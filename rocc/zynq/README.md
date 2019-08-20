# Zynq Configs
This is where you can add your accelerator to the `Rocket Chip` generated for your `Zynq` FPGA. You should have followed the steps in [accelerators/](../accelerators/) already. All you need to do is add a line to [`ZynqConfigs.scala`](ZynqConfigs.scala) as follows:
```scala
// You will replace MyAccelerator with the name of your accelerator
class ZynqFPGAMyAcceleratorConfig extends Config(new ZynqFPGAAcceleratorConfig ++ new freechips.rocketchip.system.MyAcceleratorConfig)
```
You should have defined `MyAcceleratorConfig` in [`AcceleratorConfigs.scala`](../accelerators/config/AcceleratorConfigs.scala) and you will use the name `ZynqFPGAMyAcceleratorConfig` when you compile the `Rocket Chip` for your FPGA.

You're now ready to start building a `Rocket Chip` with your accelerator. Continue following the instructions in the toplevel [README](../../README.md#installing_your_accelerator).