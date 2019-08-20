# Verilator
The verilator is a cycle-accurate verilog simulator that is included in the [rocket-chip](https://github.com/freechipsproject/rocket-chip/tree/7cd3352c3b802c3c50cb864aee828c6106414bb3) directory that is used in the [fpga-zynq](https://github.com/ucb-bar/fpga-zynq) repository inside our [deca](https://github.com/rhit-neuro/deca) repository. These verilator simulations are compiled and ran inside the emulator folder. The verilator can be used to test the custom accelerator that is created by showing a cycle by cycle output of what happens when running specific tests written in either c or assembly.

### Development Environment
We used docker to simplify setting up development environments. Make sure you are using your container for the docker image, [deca-docker/rocket-chip-env](https://github.com/rhit-neuro/deca-docker/tree/master/rocket-chip-env#running-for-the-first-time). Look inside this repository for instructions on how to run your docker container for the deca repository.

### Writing Assembly Tests
The first step to using the verilator is to write custom assembly tests. This is done inside the `deca/verilator-tests` folder.

```bash
cd verilator-tests
```

Make a directory that will contain the assembly tests you will create.

```bash
mkdir myAccelerator
cd myAccelerator
```

Create an assembly file

```bash
touch myAcceleratorTest_0.S
```

Use the following template for the assembly file. See [doMult.S](mult/doMult.S) for an example.

```assembly
#include "riscv_test.h"
#include "riscv_test_rocc.h"
#include "xcustom.h"

#define FUNCT_DO_MYACCELERATOR 0

#define CUSTOM_X 0


    RVTEST_WITH_ROCC # Define TVM used by program

start:

    RVTEST_CODE_BEGIN

    li x12, num1
    li x13, num2
    ROCC_INSTRUCTION_RAW_R_R_R(CUSTOM_X,11, 12, 13, FUNCT_DO_MYACCELERATOR)
    li x5, result
    bne x11, x5, fail
    RVTEST_PASS # success

fail:
    RVTEST_FAIL # failure

    RVTEST_CODE_END # End of test code

RVTEST_DATA_BEGIN

RVTEST_DATA_END
```

All the tests will go between `RVTEST_CODE_BEGIN` and the `fail` flag. Use `ROCC_INSTRUCTION_RAW_R_R_R` to call your custom accelerator. The first input is the custom accelerator you are using (can be 0-3). Make sure this is the same custom accelerator number you used when creating the accelerator. The next input is the output register. The next two inputs are the registers for the inputs. The last input is the function code (can be 0-127). Make sure this is the same function code you use in your custom accelerator.

Create a Makefrag file.

```bash
touch Makefrag
```

Inside the Makefrag, put:

```Makefile
#=======================================================================
# Makefrag for myAccelerator tests
#-----------------------------------------------------------------------

myaccelerator_sc_tests = \
	myAcceleratorTest_0 \
    # Add more tests here if you have more accelerator tests for myAccelerator

myaccelerator_p_tests = $(addprefix mult-p-, $(myaccelerator_sc_tests))
myaccelerator_v_tests = $(addprefix mult-v-, $(myaccelerator_sc_tests))

spike_tests += $(myaccelerator_p_tests) $(myaccelrator_v_tests)
```

#### Things you need to change
-   `myaccelerator` - change all instances of `myaccelerator` to the name of your accelerator, keeping the given suffix. You will use these later in the patch
-   `myAcceleratorTest_0.S` - this can be changed to any name your want it to be, keeping the suffix `.S`. Just make sure that it is the same as the other instance of `myAcceleratorTest_0`
-   `myAcceleratorTest_0` - change this to be the same as `myAcceratorTest_0.S` without the suffix `.S`
-   `myaccelerator_sc_tests =` - you can add more tests than the one there

### Create the necessary patch file
Now you will need to create a patch for `deca/fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa`. This is the directory where the assembly tests are compiled and built.

Download [assembly-tests-base.patch](https://gist.github.com/heidecjj/a03fda51fb43a7cc086b606ff2adbdc4) and use that as your base patch. Put this in the `deca/verilator-tests` folder and name it, `MyAccelerator_tests.patch`.

#### Things you need to change
-   `MyAccelerator_tests.patch` - change `MyAccelerator` to the name of your accelerator, keeping the suffix `_tests.patch`
-   `+include $(src_dir)/myAccelerator/Makefrag` - change `myAccelerator` to the name of the folder you put the Makefrag inside of
-   `+$(eval $(call compile_template,myaccelerator,-march=rv64g -mabi=lp64))` - change `myaccelerator` to what you used in the Makefrag
-   `+accelerators = myaccelerator-v-myAcceleratorTest_0 myaccelerator-p-myAcceleratorTest_0` - change both instances `myaccelerator` to what you used in the Makefrag and `myAcceleratorTest_0` to what you called your assembly test. You may add more tests here if you have more tests using the outline, `myaccelerator-v-myAcceleratorTest_# myaccelerator-p-myAcceleratorTest_#`

### Compiling and Building Assembly Tests
You should now have all the necessary things to compile and build the assembly tests. The following assumes you are in the `deca/verilator-tests` folder.

You can create a script so you don't have to retype the same steps everytime. See [build-custom-mult-tests.sh](../scripts/build-custom-mult-tests.sh) for an example of a script

First, you need to add symbolic links to your myaccelerator test assembly files along with then necessary header files.

```bash
ln -s $(pwd)/myAccelerator $(pwd)/../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/myAccelerator
ln -s $(pwd)/macros/custom $(pwd)/../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/macros/custom
```

Now, you need to apply the patch you created above, clean the directory, compile and build our tests, then undo the patch.

```bash
pushd ../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/
git apply ../../../../../verilator-tests/myaccelerator_tests.patch
make clean
make all
git apply -R ../../../../../verilator-tests/myaccelerator_tests.patch
popd
```

Now, we want to move the verilator tests to a folder in deca.

```bash
mkdir -p output

mv ../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/myaccelerator-p-myAcceleratorTest_0 output
mv ../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/myaccelerator-v-myAcceleratorTest_0 output
# If you have more tests, move the corresponding files too
```

Now lets remove all the dump files in the isa directory and unlink the symbolic links we added above.

```bash
rm -f ../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/*.dump

rm -f $(pwd)/../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/myAccelerator
rm -f  $(pwd)/../fpga-zynq/rocket-chip/riscv-tools/riscv-tests/isa/macros/custom
```

### Run Verilator
Now you are ready to run your tests on the verilator.

First, start off by uninstalling and reinstalling the symlinks.

```bash
pushd ../scripts
./uninstall-symlinks.sh
./install-symlinks.sh
popd
```

Now, we need to put the verilator test we just created into the `fpga-zynq/rocket-chip/emulator` folder.

```bash
cp output/myAccelerator-p-myAcceleratorTest_0 ../fpga-zynq/rocket-chip/emulator
# If you have more tests, move the corresponding files too
```

Now, go to the `fpga-zynq/rocket-chip/emulator` folder, make the accelerator in the emulator, and run the test(s).

```bash
pushd ../fpga-zynq/rocket-chip/emulator
make CONFIG=MyAcceleratorConfig
./emulator-freechips.rocketchip.system-MyAcceleratorConfig +verbose MyAccelerator-p-myAcceleratorTest_0 2> OUTPUT.txt
popd
```

Inspect the `OUTPUT.txt` file inside the deca/fpga-zynq/rocket-chip/emulator file to see if your test passed or not.

### Helpful Resources

The [riscv-tests](https://github.com/riscv/riscv-tests/tree/ba39c5fc2885eb1400d6f9e13ae6c7588c1c1241) repository explains more on how writing the assembly tests work.