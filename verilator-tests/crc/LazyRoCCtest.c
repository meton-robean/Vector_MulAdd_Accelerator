#include <stdio.h>
#include <assert.h>
#include "xcustom.h"

int main() {
    uint64_t x = 123, y = 456, z = 0, temp=0;
    // load x into accumulator 2 (funct=0)
    ROCC_INSTRUCTION(0, temp, x, 2, 0);
    // read it back into z (funct=1) to verify it
    ROCC_INSTRUCTION(0, z, x, 2, 1);
    assert(z == x);

    // accumulate 456 into it (funct=3)
    ROCC_INSTRUCTION(0, temp, y, 2, 3);
    // verify it
    ROCC_INSTRUCTION(0, z, temp, 2, 1);
    assert(z == x+y);
    // do it all again, but initialize acc2 via memory this time (funct=2)
    ROCC_INSTRUCTION(0, temp, &x, 2, 2);
    ROCC_INSTRUCTION(0, temp, y, 2, 3);
    ROCC_INSTRUCTION(0, z, temp, 2, 1);
    assert(z == x+y);

    printf("success!\n");
}