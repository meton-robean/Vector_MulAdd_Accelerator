

#include <assert.h>
#include <stdio.h>


int main() {
    //test1
    float a[4] = {1,2,3,4};
    float b[4] = {2,2.0,1,1};
    float tmp[2];
    printf("%f\n", *(a+1));

    for(int i=0; i<2; i++){
     tmp[i]= *(a+4-2+i);
     printf("%f\n", tmp[i]);
}

    return 0;
}
