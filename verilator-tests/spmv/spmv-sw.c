//see LICENSE for license
// The following is a RISC-V program to test the 
// functionality of the sconv RoCC accelerator.
// Compile with riscv64-unknown-elf-g++ spmv-sw.c -o spmv-sw.rv
// Run with spike --extension=spmv pk a.out

#include "timer.h"
#include "graph_io.h"

void SpmvSolver(int num_rows, int nnz, IndexType *Ap, IndexType *Aj, ValueType *Ax, ValueType *x, ValueType *y, int *degree) {
	Timer t;
	t.Start();
	for (int i = 0; i < num_rows; i++){
		const IndexType row_begin = Ap[i];
		const IndexType row_end   = Ap[i+1];
		ValueType sum = y[i];
		for (IndexType jj = row_begin; jj < row_end; jj++) {
			const IndexType j = Aj[jj];  //column index
			sum += x[j] * Ax[jj];
		}
		y[i] = sum; 
	}
	t.Stop();
	printf("\truntime = %f ms.\n", t.Millisecs());
	return;
}

int main(int argc, char *argv[]) {
	printf("Sparse Matrix-Vector Multiplication by Xuhao Chen\n");
	bool is_directed = true;
	if (argc> 2) {
		is_directed = atoi(argv[2]);
		if(is_directed) printf("A is not a symmetric matrix\n");
		else printf("A is a symmetric matrix\n");
	}
	bool symm = !is_directed;

	// CSC data structures
	int m, n, nnz;
	IndexType *colptr = NULL, *rowidx = NULL;
	int *degree = NULL;
	ValueType *weight = NULL;
	read_graph(argc, argv, m, n, nnz, colptr, rowidx, degree, weight, symm, true, true, false, true);
	int num_cols = n;
	ValueType *x = (ValueType *)malloc(m * sizeof(ValueType));
	ValueType *y = (ValueType *)malloc(m * sizeof(ValueType));
	ValueType *z = (ValueType *)malloc(m * sizeof(ValueType));
	srand(13);
	for(int i = 0; i < nnz; i++) weight[i] = 1.0 - 2.0 * (rand() / (RAND_MAX + 1.0)); // Ax[] (-1 ~ 1)
	for(int i = 0; i < m; i++) x[i] = rand() / (RAND_MAX + 1.0);
	//for(int i = 0; i < num_cols; i++) x[i] = 1.0;
	for(int i = 0; i < m; i++) {
		y[i] = 0.0;//rand() / (RAND_MAX + 1.0);
		z[i] = y[i];
	}

	SpmvSolver(m, nnz, colptr, rowidx, weight, x, y, degree);
	//SpmvVerifier(m, nnz, colptr, rowidx, weight, x, z, y);
	free(colptr);
	free(rowidx);
	//free(degree);
	free(weight);
	free(x);
	free(y);
	return 0;
}
