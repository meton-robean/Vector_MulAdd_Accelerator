// See LICENSE for license details.

#ifndef SRC_MAIN_C_ACCUMULATOR_H
#define SRC_MAIN_C_ACCUMULATOR_H

#include "xcustom.h"

#define k_SET_UP  0
#define k_DO_SHA3 1
#define k_DO_READ 2
#define k_DO_WRITE 3

#define XCUSTOM_ACC 0

#define setUp(x, in, out)                                               \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, in, out, k_SET_UP);
#define doSha3(x, y)                                                    \
  ROCC_INSTRUCTION(XCUSTOM_ACC, x, y, 0, k_DO_SHA3);
#define doRead(y, addr)                                                 \
  ROCC_INSTRUCTION(XCUSTOM_ACC, y, 0, addr, k_DO_READ);
#define doWrite(y, addr, data)                                          \
  ROCC_INSTRUCTION(XCUSTOM_ACC, y, data, addr, k_DO_WRITE);

#endif  // SRC_MAIN_C_ACCUMULATOR_H
