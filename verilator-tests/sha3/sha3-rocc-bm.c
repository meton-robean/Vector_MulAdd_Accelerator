//see LICENSE for license
// The following is a RISC-V program to test the 
// functionality of the sha3 RoCC accelerator.
// Compile with riscv64-unknown-elf-gcc sha3-rocc.c -o sha3-rocc.rv
// Run with spike --extension=sha3 pk a.out

#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include "sha3.h"
#include "rocc_sha3.h"

int main() {
	do {
		unsigned int len = 150;
		unsigned char input[150] = "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000";
		unsigned char output[SHA3_256_DIGEST_SIZE];

		unsigned int temp = 0;
		setUp(temp, input, output);
		doSha3(temp, len);
	} while(0);
	return 0;
}
