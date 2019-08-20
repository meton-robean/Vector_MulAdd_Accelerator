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
		uint16_t addr = 1;
		uint64_t y = 0;
		//unsigned int len = 8;
		//unsigned char input[8] = "\000\000\000\000\000\000\000\000";
		unsigned int len = 150;
		unsigned char input[150] = "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000";
		unsigned char output[SHA3_256_DIGEST_SIZE];

		unsigned int temp = 0;
		asm volatile ("fence");
		printf("Setup...\n");
		setUp(temp, input, output);
		asm volatile ("fence");
		doRead(y, addr);
		printf("[INFO] msg_addr = 0x%lx (expected 0x%lx)\n", y, input);
		addr = 2;
		doRead(y, addr);
		printf("[INFO] hash_addr = 0x%lx (expected 0x%lx)\n", y, output);
		printf("Do Sha3...\n");
		doSha3(temp, len);
		asm volatile ("fence");
		printf("Done...\n");
		uint32_t x = 0;
		addr = 3;
		doRead(x, addr);
		printf("[INFO] msg_len = %d (expected %d)\n", x, len);
		addr = 0;
		doRead(x, addr);
		printf("[INFO] windex = %d (expected 4)\n", x);
		// Check result
		unsigned char result[SHA3_256_DIGEST_SIZE] =
		{221,204,157,217,67,211,86,31,54,168,44,245,97,194,193,26,234,42,135,166,66,134,39,174,184,61,3,149,137,42,57,238};
		//sha3ONE(input, len, result);
		for(int i = 0; i < SHA3_256_DIGEST_SIZE; i++) {
			printf("output[%d]:%d ==? results[%d]:%d \n",i,output[i],i,result[i]);
			assert(output[i]==result[i]); 
		}
	} while(0);
	printf("success!\n");
	return 0;
}
