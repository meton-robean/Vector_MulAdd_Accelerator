//see LICENSE for license
// The following is a RISC-V program to test the functionality of the
// Compile with riscv-gcc sha3-rocc-2.c

#include <assert.h>
#include <stdio.h>
#include <time.h>
#include <stdlib.h>
#include <sys/time.h>
#include "rocc.h"

#define TEST_A
#define TEST_B
#define TEST_C
#define TEST_D
#define TEST_E

int main() {

    struct timeval start,end;
    long fpgatimeuse;
    long cputimeuse;
    int tmp;

    //********test a*********
    #ifdef TEST_A
    int len1    =1000;
    long long c1=1;
    long long d1[len1];
    //ins 格式：三层循环迭代次数3个，（数组起始地址，和i，j，k是否相关，相关为1，否则为0）*3组，最后一位是op
    int instr1[16]={1,1,len1,&c1,0,0,0,&c1,0,0,0,d1,0,0,1,0};

    for(int i = 0;i < len1;i++){
        d1[i] = i%11 + 1;
    }

    gettimeofday(&start, NULL );//计时开始
    for(int i=0;i<len1;i++){
        c1+=d1[i];
    }
    gettimeofday(&end, NULL );//计时结束
    cputimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("****************test a cpu runtime: %f scends\n",cputimeuse /1000000.0);
    printf("c1:%lld\n",c1);
    c1=1;  

    gettimeofday(&start, NULL );
    ROCC_INSTRUCTION_DS(0, tmp, instr1, 1);          //
    gettimeofday(&end, NULL );
    fpgatimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("****************test a fpga runtime: %f scends\n",fpgatimeuse /1000000.0);
    printf("c1:%lld\n",c1);
    #endif


    //********test b*********
    #ifdef TEST_B
    int len2    =1000;
    long long c2[len2];
    long long d2[len2];
    int instr2[16]={1,1,len2,c2,0,0,1,c2,0,0,1,d2,0,0,1,0};
    for(int i = 0;i < len2;i++){
        c2[i] = i%3 + 1;
        d2[i] = i%11 + 1;
    }

    gettimeofday(&start, NULL );//计时开始
    for(int i=0;i<len2;i++){
        c2[i]+=d2[i];
    }
    gettimeofday(&end, NULL );//计时结束
    cputimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("*****************test b cpu runtime: %f scends\n",cputimeuse /1000000.0);

    for(int i=0;i<len2;i++){
        printf("%lld ",c2[i]);
        c2[i] = i%3 + 1;
    }
    printf("\n");

    gettimeofday(&start, NULL );
    ROCC_INSTRUCTION_DS(0, tmp, instr2, 1);          //
    gettimeofday(&end, NULL );
    fpgatimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("****************test b fpga runtime: %f scends\n",fpgatimeuse /1000000.0);
    for(int i=0;i<len2;i++){
        printf("%lld ",c2[i]);
    }
    printf("\n");
    #endif

    //********test c**********   
    #ifdef TEST_C
    int len3 = 30;
    long long cc[len3];
    long long dd[len3];
    long long zz[len3][len3];
    int instr3[16]={1,len3,len3,zz,0,1,1,cc,0,1,0,dd,0,0,1,0};

    for(int i = 0;i < len3;i++){
        cc[i] = i%3 + 1;
        dd[i] = i%11 + 1;
    }

    //统计代码在cpu运行时间    
    
    
    gettimeofday(&start, NULL );//计时开始
    for(int i = 0;i < len3;i++){
        for(int j = 0;j < len3;j++){
            zz[i][j] = cc[i]+dd[j];
        }
    }
    gettimeofday(&end, NULL );//计时结束
    cputimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("*****************test c cpu runtime: %f scends\n",cputimeuse /1000000.0);
    for(int i = 0;i < len3;i++){
        for(int j = 0;j < len3;j++){
            printf("%lld ",zz[i][j]);
            zz[i][j] = 0;
        }
    }
    printf("\n");

    gettimeofday(&start, NULL );
    ROCC_INSTRUCTION_DS(0, tmp, instr3, 1);          //
    gettimeofday(&end, NULL );
    fpgatimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("**************test c fpga runtime: %f scends\n",fpgatimeuse /1000000.0);

    for(int i = 0;i < len3;i++){
        for(int j = 0;j < len3;j++){
            printf("%lld ",zz[i][j]);
        }
    }
    printf("\n"); 
    #endif


    //********test d*********
    #ifdef TEST_D
    int len4    =30;
    long long c4[len4];
    long long d4[len4][len4];
    int instr4[16]={1,len4,len4,c4,0,0,1,c4,0,0,1,d4,0,1,1,0};

    for(int i=0;i<len4;i++){
        c4[i] = i%3 + 1;
        for (int j=0;j<len4;j++){
            d4[i][j]=i+j;
        }
    }

    gettimeofday(&start, NULL );//计时开始
    for(int i=0;i<len4;i++){
        for (int j=0;j<len4;j++){
            c4[j]+=d4[i][j];
        }
    }
    gettimeofday(&end, NULL );//计时结束
    cputimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("*****************test d cpu runtime: %f scends\n",cputimeuse /1000000.0);

    for(int i=0;i<len4;i++){
        printf("%lld ",c4[i]);
        c4[i]= i%3 + 1;
    }
    printf("\n");

    gettimeofday(&start, NULL );
    ROCC_INSTRUCTION_DS(0, tmp, instr4, 1);          //
    gettimeofday(&end, NULL );
    fpgatimeuse =1000000 * ( end.tv_sec - start.tv_sec ) + end.tv_usec - start.tv_usec;
    printf("****************test d fpga runtime: %f scends\n",fpgatimeuse /1000000.0);

    for(int i=0;i<len4;i++){
        printf("%lld ",c4[i]);
    }
    printf("\n");
    #endif


    //********test e*********
    #ifdef TEST_E

    #endif


    
    return 0;
}
