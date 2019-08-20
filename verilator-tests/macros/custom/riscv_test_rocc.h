// See LICENSE for license details.

#ifndef ROCC_SOFTWARE_SRC_RISCV_TEST_ROCC_H_
#define ROCC_SOFTWARE_SRC_RISCV_TEST_ROCC_H_


#define RVTEST_XS_ENABLE                        \
  li a0, MSTATUS_XS & (MSTATUS_XS >> 1);        \
  csrs mstatus, a0;

#define RVTEST_WITH_ROCC                        \
  .macro init;                                  \
  RVTEST_XS_ENABLE                              \
  .endm

#endif // ROCC_SOFTWARE_SRC_RISCV_TEST_ROCC_H_