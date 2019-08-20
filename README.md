# Vector_MulAdd_Accelerator
vector multiplication adder accelerator (using chisel3 and RocketChip RoCC )

**rocc/accelerators/ : some accelerators using rocketchip RoCC.**  

　#1 kernel/ : simple vector multiplication adder accelerator  
　#2 crc/ : crc checking  
　#3 multState/ : simple scalar multiplication( e.g. a*b=c)  
　#4 sha3/ : sha3 algs  
　#5 spmv/ : sparse matrix multiplication  
 
**rocc/sbt/ : ｓｂｔ unit tests of accelerators**   
**rocc/zynq/ : zynq config**  
**verilator-tests/ : C test cases **   


### Usage:  
 #1 download rocketchip RoCC accelerator template:  
   git clone https://github.com/meton-robean/deca.git  
 #2 cd deca/    
 #3 replace rocc/ and verilator-tests/ in deca with rocc/ and verilator-tests in this repository  
 #4 **follow the README in deca to test the accelerators**   
   
### Test Result:  
e.g.if you test kernel accel(simple vector multiplication adder accelerator ):  

#### C test case:  
  '''
  
  int main() {
    
    //test1
    int a[4] = {1,2,3,4};
    int b[4] = {2,7,1,1};
    int result=1;
    int len =4;
    printf("[INFO] a addr: %x, b_addr: %x\n", a, b);

    asm volatile ("fence");
    ROCC_INSTRUCTION_SS(0, a, b, 0);         //result  a rs1, b rs2
    ROCC_INSTRUCTION_DS(0, result, len,  1); //result, len
    asm volatile ("fence");

    assert(result == 23);
    printf("[INFO] ***** result %d ****** success! \n", result );


    //test2
    int c[8] = {1,2,3,4,5,6,7,8};
    int d[8] = {1,2,1,7,1,1,1,1};
    int result2 =0;
    int len2    =8;

    VEC_VEC_MULADD(c, d, len2, &result2);

    assert(result2 == 62);
    printf("[INFO] ***** result: %d ****** success!\n", result2);

    //test3
    int c2[10] = {1,2,3,4,5,6,7,8, 2, 1};
    int d2[10] = {1,2,1,7,1,1,1,1, 3, 2};
    int result3 =0;
    int len3    =10;

    VEC_VEC_MULADD(c2, d2, len3, &result3);

    assert(result3 == 70);
    printf("[INFO] ***** result: %d ****** success!\n", result3);
    return 0;
}

  '''  
#### run the accelerator emulator:  

 ./emulator-freechips.rocketchip.system-VecMulAddAccelConfig +verbose pk ~/Applications/deca/verilator-tests/kernel/test_kernel_rocc  

#### result:  

 、、、
 
 、、、



