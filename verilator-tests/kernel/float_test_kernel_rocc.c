//see LICENSE for license
// The following is a RISC-V program to test the functionality of the
// Compile with riscv-gcc sha3-rocc-2.c

#include <assert.h>
#include <stdio.h>
#include "float_rocc.h"



void VEC_VEC_MULADD(float *vec1_addr, float *vec2_addr, int vec_len, float *result){
    
    int N_COMPUTE_UNIT = 4 ;
    int iter = vec_len/N_COMPUTE_UNIT ;
    int n_rest_element = vec_len%N_COMPUTE_UNIT;
    float rest_array1[N_COMPUTE_UNIT];
    float rest_array2[N_COMPUTE_UNIT];
    float tmp =0;
    int base1, base2;
    if(n_rest_element>0){                                //不够整除，需要用0拼接
        iter+=1;
        for(int i=0; i<n_rest_element; i++){
            rest_array1[i] = *(vec1_addr+(vec_len-n_rest_element+i) );
            rest_array2[i] = *(vec2_addr+(vec_len-n_rest_element+i) );
            
        }
        for(int i=n_rest_element; i<N_COMPUTE_UNIT; i++){
            rest_array1[i] = 0.0;
            rest_array2[i] = 0.0;
        }
    }
    
    for(int i=0; i<iter; i++ ){
        asm volatile ("fence");
        if( (i==iter-1) && (n_rest_element>0)  ){
            base1 = &rest_array1[0];
            base2 = &rest_array2[0];
        }else{
            base1 = vec1_addr + i*sizeof( float);
            base2 = vec2_addr + i*sizeof( float);
        }

        ROCC_INSTRUCTION_SS(0, base1, base2, 0);          //传送两个vector的基地址
        ROCC_INSTRUCTION_DS(0, tmp, N_COMPUTE_UNIT,  1);  //传送结果要返回的地址， 要计算的向量长度
        asm volatile ("fence");
        printf("tmp[%d]:%f\n", i , tmp);                  //中间结果
        *result = *result +tmp ;                          //最终结果
        

    }

}


int main() {
    //test1
    float a[4] = {0.1, 0.22, 3.1, 4.2};
    float b[4] = {2.1, 2.0,  1.2, 1.2};
    float result=0.0;
    int   len =4;
    printf("[INFO] a addr: %x, b_addr: %x\n", a, b);

    asm volatile ("fence");
    ROCC_INSTRUCTION_SS(0, a, b, 0);         //result  a rs1, b rs2
    ROCC_INSTRUCTION_DS(0, result, len,  1); //result, len
    asm volatile ("fence");

    //assert(result == 9.41);
    printf("[INFO] ***** result %f ****** success! \n", result );


    //test2
    float c[8] = {1,2,3,4,5,6,7,8};
    float d[8] = {1,2,1,7,1,1,1,1};
    float result2 =0;
    int   len2    =8;

    VEC_VEC_MULADD(c, d, len2, &result2);

    //assert(result2 == 62);
    printf("[INFO] ***** result: %f ****** success!\n", result2);

    //test3
    float c2[10] = {1,2,3,4,5,6,7,8, 2, 1};
    float d2[10] = {1,2,1,7,1,1,1,1, 3, 2};
    float result3 =0;
    int   len3    =10;

    VEC_VEC_MULADD(c2, d2, len3, &result3);

    //assert(result3 == 70);
    printf("[INFO] ***** result: %f ****** success!\n", result3);
    return 0;
}
