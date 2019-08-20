//see LICENSE for license
// The following is a RISC-V program to test the functionality of the
// Compile with riscv-gcc sha3-rocc-2.c

#include <assert.h>
#include <stdio.h>
#include "rocc.h"

void VEC_VEC_MULADD(int *vec1_addr, int *vec2_addr, int vec_len, int *result){
    asm volatile ("fence");
    int N_COMPUTE_UNIT = 2 ;
    int iter = vec_len/N_COMPUTE_UNIT ;
    printf("%x %x\n", vec1_addr, vec2_addr);
    for(int i=0; i<iter; i++ ){
        int tmp=1;
        int base1 = vec1_addr + i*sizeof( int);
        int base2 = vec2_addr + i*sizeof( int);
        printf("%x %x\n", base1, base2);
        // ROCC_INSTRUCTION_SS(0, base1, base2, 0);
        // ROCC_INSTRUCTION_DS(0, tmp, N_COMPUTE_UNIT,  1); 
        *result = *result +tmp ;

    }

}


int main() {



    int a[4] = {1,2,3,4};
    int b[4] = {1,1,1,1};
    int result=0;
    int len =4;
    printf("a addr: %x, b_addr: %x\n", a, b); 
    printf("a addr: %x, b_addr: %x\n", &a[0], &b[0]);
    printf("%x, %x\n", &a[1], &b[1]);
    printf("%x\n", sizeof(unsigned int) );
    printf("%x, %x, %x, %x, %x\n",sizeof(unsigned int), a, b,  a+1, b+1);
    for(int i=0; i<len; i++){
    	result += a[i]*b[i];
    }

    printf("a{1,2,3,4} * b{1,1,1,1} +=> %d \n", result );
    assert(result== 10);
    printf("success!\n");
    result=0;
    VEC_VEC_MULADD(a, b, len, &result);
    printf("%d\n", result);
    return 0;


}
