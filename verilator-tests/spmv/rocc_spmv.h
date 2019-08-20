// See LICENSE for license details.

#ifndef SRC_MAIN_C_ACCUMULATOR_H
#define SRC_MAIN_C_ACCUMULATOR_H

#include "xcustom.h"

#define k_SETUP_IO  0
#define k_SETUP_CSC 1
#define k_DO_READ   2
#define k_DO_SPMV   3

#define XCUSTOM_ACC 0

#define setupIO(x, in, out)                                       \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, in, out, k_SETUP_IO);
#define setupCSR(x, row, col)                                     \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, row, col, k_SETUP_CSC);
#define doRead(x, addr)                                           \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, 0, addr, k_DO_READ);
#define doSconv(x, vec_x, vec_y)                                  \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, vec_x, vec_y, k_DO_SPMV);

#endif  // SRC_MAIN_C_ACCUMULATOR_H
