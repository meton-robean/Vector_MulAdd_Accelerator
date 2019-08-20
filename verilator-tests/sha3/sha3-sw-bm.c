//see LICENSE for license
// The following is a RISC-V program to test the 
// functionality of the sha3 RoCC accelerator.
// Compile with riscv64-unknown-elf-gcc sha3-sw-bm.c -o sha3-sw-bm.rv
// Run with spike --extension=sha3 pk sha3-sw-bm.rv

#include <stdint.h>
#include "sha3.h"

int main() {
  do {
    unsigned int ilen = 150;
    unsigned char input[150] = "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000";
    unsigned char output[SHA3_256_DIGEST_SIZE];
    sha3ONE(input, ilen, output);
  } while(0);
  return 0;
}
