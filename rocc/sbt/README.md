# Creating an Accelerator and Testing it with Sbt

Sbt is an open-source build tool for Scala and Java projects. As Chisel is an Scala Embedded Language, we use sbt in order to test our Chisel code.

This is where you should test the chisel code for the accelerators before integrating it with the rocket chip following the instructions in the [rocc](../rocc) folder. The following template along with the included [MultState.scala](../accelerators/multState/MultState.scala) should provide a good starting point for making your own accelerator.

### Creating an Accelerator

Suppose you want to make an accelerator called MyAccelerator. First create a directory for your scala files inside the `deca/rocc/accelerators` folder.

```bash
mkdir myAccelerator
cd myAccelerator
```

Next, you create an accelerator called `MyAccelerator.scala`, using the following template. See [MultState.scala](../accelerators/multState/MultState.scala) for an example.

```scala
package myaccelerator

import chisel3._
import chisel3.util._

class MyAccelerator extends Module{
        val io = IO(new Bundle {
                val req = Flipped(Decoupled(new Bundle {
                        // inputs
                }))
                val resp = Decoupled(new Bundle {
                        // outputs
                })
        })
        // code goes here
}
```

#### Things you need to change
-   `mkdir myAccelerator` - change `myAccelerator` with what you want your accelerator to be called, prefereably starting with a lowercase letter
-   `package myaccelerator` - change `myaccelerator` to whatever you want to name the package your accelerator resides in, preferably all lowercase. Make this the same as the other references to `package myaccelerator`
-   `class MyAccelerator` - change `MyAccelerator` to whatever you want to name your accelerator. This will be used again when you test it down below and when the `MyAcceleratorAccelerator.scala` file referenced in [accelerators](../accelerators/README.md) calls it.

### Creating Chisel Tests for Accelerator
Now that you have created the accelerator, you will want to test it. Go to the `deca/rocc/sbt/tests` folder.

Create a directory for the scala test files.

```bash
mkdir myAccelerator
cd myAccelerator
```

Next, create an accelerator test file called `MyAcceleratorTest.scala`, using the following template. See [MultStateTest.scala](tests/multState/MultStateTest.scala) for an example.

```scala
package myaccelerator

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.iotesters.PeekPokeTester
import chisel3._

class MyAcceleratorTest(c: MyAccelerator) extends PeekPokeTester(c) {
    // poke, expect, and step code goes here
}
```

#### PeekPokeTester
-   `poke` - poke takes in two arguments, an input variable for MyAccelerator and the value you are setting that input variable too.
-   `expect` - expect takes in two arguments, an output variable for MyAccelerator and the value you are expecting it to be. If the two are not the same, this test will fail.
-   `step` - step takes in one argument, an integer. This argument is the number of cycles you want to advance. For example, step(1) goes to the next cycle.

Now, we need to create file called `Launcher.scala` inside the accelerator folder, using the following template. This file is used to launch the specific tests for the given accelerator with the sbt command line. See [Launcher.scala](tests/multState/Launcher.scala) for an example.

```scala
package myaccelerator

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TesterRunner

object Launcher {
  val tests = Map(
    "MyAccelerator" -> { (manager: TesterOptionsManager) =>
       Driver.execute(() => new MyAccelerator(), manager) {
         (c) => new MyAcceleratorTest(c)
       }
    }
  )

  def main(args: Array[String]): Unit = {
    TesterRunner("myaccelerator", tests, args)
  }
}
```

#### Things you need to change
-   `mkdir myAccelerator` - change `myAccelerator` with what you want your accelerator to be called, prefereably starting with a lowercase letter
-   `package myaccelerator` - change `myaccelerator` to whatever you want to name the package your accelerator resides in, preferably all lowercase. Make this the same as the other references to `package myaccelerator`
-   `class MyAcceleratorTest(c: MyAccelerator)` - change `MyAccelerator` to what you used above. Make sure to keep the `Test` Prefix for `MyAcceleratorTest`
-   `"MyAccelerator" -> { (manager: TesterOptionsManager)` - change `MyAccelerator` to whatever you want. This needs to be the same thing you will use when you run the Chisel Test below.
-   `Driver.execute(() => new MyAccelerator(), manager)` - change `MyAccelerator` to the accelerator name you used above for `MyAccelerator`
-   `(c) => new MyAcceleratorTest(c)` - change `MyAcceleratorTest` to the name you used above for `MyAcceleratorTest`
-   `TesterRunner("myaccelerator", tests, args)` - change `myaccelerator` to the same as the package name

### Checking build.sbt before Running Chisel Tests
The build.sbt gives sbt specific properties, including the path to source files and declaring what files to exclude.

-   `scalaSource in Test := baseDirectory.value / "tests"` - If you followed the tutorial, this line is fine and doesn't need any changes. If your tests are in a different folder, `tests` will need to change the path that needs to be taken to get to that folder. (The tests are allowed to be in the subfolders of the specified folder)
-   `scalaSource in Compile := baseDirectory.value / "../accelerators"` - If you followed the turoial, this line is fine and doesn't need any changes. If your source files are in a different folder, `../accelerators` will need to change to the path that needs to be taken to get to that folder. (The sources are allowed to be in the subfolders of the specified folder)
-   `excludeFilter in unmanagedSources :=` - After the `=`, there will be a list of file names separated by a `||`. Make sure that all files in the `tests` and `../accelerators` folder that reference the rocket chip are included here. This should be the `MyAcceleratorAccelerator.scala` files and the `AcceleratorConfigs.scala` file.

## Running Chisel Tests for Accelerator
Now that you created the tests, you can now run the tests using the sbt command line. First, go to the `deca/rocc/sbt` directory. Next type the following into the command line.

```bash
sbt
```

This will launch the sbt terminal.

Now, to run the test, type the following in the sbt command line.

```sbt
test:runMain myacelerator.Launcher MyAccelerator
```

If all the tests passed, you can now proceed to the next step, connecting the accelerator to the Rocket Chip using the RoCC Interface.

#### Things you need to change
-   `myaccelerator.Launcher` - change `myaccelerator` to the package name you have been using for this accelerator
-   `MyAccelerator` - change `MyAccelerator` to the name you used in the `"MyAccelerator" -> { (manger: TesterOptionsManager)` line above.

## Connecting to the Rocket Chip
To learn how to connect this module to the rocket chip, see [README.md](../accelerators/README.md).