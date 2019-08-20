#include "rocc.h"


#define CUSTOM_X 0


    RVTEST_WITH_ROCC # Define TVM used by program

start:

    RVTEST_CODE_BEGIN

    li x12, 6
    li x13, 7
    ROCC_INSTRUCTION_RAW_R_R_R(CUSTOM_X,11, 12, 13, 0)
    ROCC_INSTRUCTION_RAW_R_R_R(CUSTOM_X,11, 12, 13, 1)
    li x5, 42
    bne x11, x5, fail
    RVTEST_PASS # success

fail:
    RVTEST_FAIL # failure

    RVTEST_CODE_END # End of test code

RVTEST_DATA_BEGIN

RVTEST_DATA_END
