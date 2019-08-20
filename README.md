# Vector_MulAdd_Accelerator
vector multiplication adder accelerator (using chisel3 and RocketChip RoCC )

**rocc/accelerators/ : some accelerators using rocketchip RoCC.**  
**kernel是向量乘加加速器实现，除此之外还有其他几个测试用的加速器例子供参考**  
　#1 kernel/ : simple vector multiplication adder accelerator 
　#2 crc/ : crc checking  
　#3 multState/ : simple scalar multiplication( e.g. a*b=c)  
　#4 sha3/ : sha3 algs  
　#5 spmv/ : sparse matrix multiplication  
 
**rocc/sbt/ : sbt unit tests of accelerators 单元测试**   
**rocc/zynq/ : zynq config file**   
**verilator-tests/ : C test cases**      


### Usage:  
 #1 download rocketchip RoCC accelerator template:   
  git clone https://github.com/meton-robean/deca.git  
 #2 cd deca/    
 #3 replace rocc/ and verilator-tests/ in deca with rocc/ and verilator-tests in this repository  
 #4 **follow the README in deca to test the accelerators**   
   
### Test:  
e.g.if you test kernel accel(simple vector multiplication adder accelerator ):  

#### C test case:  
  '''
  
using random seed 1566300015  
This emulator compiled with JTAG Remote Bitbang client. To enable, use +jtag_rbb_enable=1.  
Listening on port 37035  

//test1---------------------------------------------------  

[INFO] a addr: fee9ae0, b_addr: fee9ad0  
rs1: 000000000fee9ae0, rs2: 000000000fee9ad0  
vec_len:                    4,    rd: 0a  
mem read begin!,  status--** busy reg:1  
vec1:          1           2           3           4   
vec2:          2           7           1           1   
result:         23  
s_compute_done  
[INFO] ***** result 23 ****** success!   

//test2---------------------------------------------------  

rs1: 000000000fee9ab0, rs2: 000000000fee9a90  
vec_len:                    4,    rd: 0a  
mem read begin!,  status--** busy reg:1  
vec1:          1           2           3           4   
vec2:          1           2           1           7   
result:         36  
s_compute_done  
tmp[0]:36  
rs1: 000000000fee9ac0, rs2: 000000000fee9aa0  
vec_len:                    4,    rd: 0a   
mem read begin!,  status--** busy reg:1   
vec1:          5           6           7           8    
vec2:          1           1           1           1   
result:         26   
s_compute_done   
tmp[1]:26  
[INFO] ***** result: 62 ****** success!  

//test3---------------------------------------------------  

rs1: 000000000fee9a60, rs2: 000000000fee9a38  
vec_len:                    4,    rd: 0a  
mem read begin!,  status--** busy reg:1  
vec1:          1           2           3           4   
vec2:          1           2           1           7   
result:         36  
s_compute_done  
tmp[0]:36  
rs1: 000000000fee9a70, rs2: 000000000fee9a48  
vec_len:                    4,    rd: 0a  
mem read begin!,  status--** busy reg:1  
vec1:          5           6           7           8   
vec2:          1           1           1           1   
result:         26  
s_compute_done  
tmp[1]:26  
rs1: 000000000fee9980, rs2: 000000000fee9960  
vec_len:                    4,    rd: 0a  
mem read begin!,  status--** busy reg:1  
vec1:          2           1           0           0    
vec2:          3           2           0           0    
result:          8  
s_compute_done  
tmp[2]:8  
[INFO] ***** result: 70 ****** success!  
Completed after 3156322 cycles  
}  

  '''  
  
 

 



