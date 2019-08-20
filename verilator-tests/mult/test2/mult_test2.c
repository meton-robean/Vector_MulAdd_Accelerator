//see LICENSE for license
// The following is a RISC-V program to test the functionality of the
// Compile with riscv-gcc sha3-rocc-2.c

#include <assert.h>
#include <stdio.h>
#include "rocc.h"


int main() {



    unsigned int a = 13;
    unsigned int b = 4;
    unsigned int result=0;

    ROCC_INSTRUCTION_DSS(0, result, a, b, 0); //@cmt 乘法运算 a*b--> result

    printf("%d * %d = %d \n", a, b, result );
    assert(result==(a*b));

    printf("success!\n");
    return 0;
}
