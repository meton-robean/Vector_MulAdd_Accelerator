//see LICENSE for license
// The following is a RISC-V program to test the 
// functionality of the sconv RoCC accelerator.
// Compile with riscv64-unknown-elf-g++ spmv-rocc.c -o spmv-rocc.rv
// Run with spike --extension=spmv pk a.out

#include "rocc_spmv.h"
#include "graph_io.h"

int main(int argc, char *argv[]) {
	do {
		printf("Sparse Matrix-Vector Multiplication by Xuhao Chen\n");
		bool symm = false;

		// CSC data structures
		int m, n, nnz;
		IndexType *colptr = NULL, *rowidx = NULL;
		int *degree = NULL;
		ValueType *values = NULL;
		read_graph(argc, argv, m, n, nnz, colptr, rowidx, degree, values, symm, true, true, false, true);
		int num_cols = n;
		ValueType *x = (ValueType *)malloc(m * sizeof(ValueType));
		ValueType *y = (ValueType *)malloc(m * sizeof(ValueType));
		ValueType *z = (ValueType *)malloc(m * sizeof(ValueType));
		srand(13);
		for(int i = 0; i < nnz; i++) values[i] = 1.0 - 2.0 * (rand() / (RAND_MAX + 1.0)); // Ax[] (-1 ~ 1)
		for(int i = 0; i < m; i++) x[i] = rand() / (RAND_MAX + 1.0);
		//for(int i = 0; i < num_cols; i++) x[i] = 1.0;
		for(int i = 0; i < m; i++) {
			y[i] = 0.0;//rand() / (RAND_MAX + 1.0);
			z[i] = y[i];
		}

		int addr = 0;
		uint64_t a = 0;
		int b = 0;
		printf("Setup I/O vectors...\n");
		setupIO(a, x, y);
		asm volatile ("fence");

		doRead(a, addr);
		printf("[INFO] x_addr = 0x%lx (expected 0x%lx)\n", a, x);

		addr = 1;
		doRead(a, addr);
		printf("[INFO] y_addr = 0x%lx (expected 0x%lx)\n", a, y);

		printf("Setup CSC...\n");
		setupCSR(a, colptr, rowidx);
		asm volatile ("fence");

		addr = 2;
		doRead(a, addr);
		printf("[INFO] colptr = 0x%lx (expected 0x%lx)\n", a, colptr);

		addr = 3;
		doRead(a, addr);
		printf("[INFO] rowidx = 0x%lx (expected 0x%lx)\n", a, rowidx);

		printf("Start spmv...\n");
		doSconv(b, values, z);
		asm volatile ("fence");

		addr = 4;
		doRead(a, addr);
		printf("[INFO] values = 0x%lx (expected 0x%lx)\n", a, values);

		addr = 5;
		doRead(a, addr);
		printf("[INFO] z = 0x%lx (expected 0x%lx)\n", a, z);

		free(colptr);
		free(rowidx);
		free(values);
		free(x);
		free(y);
		free(z);
	} while(0);
	return 0;
}
