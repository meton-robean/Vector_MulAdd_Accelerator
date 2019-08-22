//see LICENSE for license
// The following is a RISC-V program to test the functionality of the
// Compile with riscv-gcc sha3-rocc-2.c

#include <assert.h>
#include <stdio.h>
#include "rocc.h"



void VEC_VEC_MULADD(int *vec1_addr, int *vec2_addr, int vec_len, int *result){
    
    int N_COMPUTE_UNIT = 4 ;
    int iter = vec_len/N_COMPUTE_UNIT ;
    int n_rest_element = vec_len%N_COMPUTE_UNIT;
    int rest_array1[N_COMPUTE_UNIT];
    int rest_array2[N_COMPUTE_UNIT];
    int tmp =0;
    int base1, base2;
    if(n_rest_element>0){                                //不够整除，需要用0拼接
        iter+=1;
        for(int i=0; i<n_rest_element; i++){
            rest_array1[i] = *(vec1_addr+(vec_len-n_rest_element+i) );
            rest_array2[i] = *(vec2_addr+(vec_len-n_rest_element+i) );
        }
        for(int i=n_rest_element; i<N_COMPUTE_UNIT; i++){
            rest_array1[i] = 0;
            rest_array2[i] = 0;
        }
    }
    
    for(int i=0; i<iter; i++ ){
        asm volatile ("fence");
        if( (i==iter-1) && (n_rest_element>0)  ){
            base1 = &rest_array1[0];
            base2 = &rest_array2[0];
        }else{
            base1 = vec1_addr + i*sizeof( int);
            base2 = vec2_addr + i*sizeof( int);
        }

        ROCC_INSTRUCTION_SS(0, base1, base2, 0);          //传送两个vector的基地址
        ROCC_INSTRUCTION_DS(0, tmp, N_COMPUTE_UNIT,  1);  //传送结果要返回的地址， 要计算的向量长度
        asm volatile ("fence");
        printf("tmp[%d]:%d\n", i , tmp);                  //中间结果
        *result = *result +tmp ;                          //最终结果
        

    }

}


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
