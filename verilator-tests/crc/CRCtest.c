#include <stdio.h>
#include "xcustom.h"
#include <inttypes.h>

#define k_SETPOLY 0
#define k_SETSTARTENDVALS 1
#define k_SETFLIPS 2
#define k_CALC 3
#define XCUSTOM_ACC 0
#define crcSetPoly(y, taps, length) ROCC_INSTRUCTION(XCUSTOM_ACC, y, taps, length, k_SETPOLY)
#define crcSetStartEndVals(y, start, endXOR) ROCC_INSTRUCTION(XCUSTOM_ACC, y, start, endXOR, k_SETSTARTENDVALS)
#define crcSetFlips(y, flipIn, flipOut) ROCC_INSTRUCTION(XCUSTOM_ACC, y, flipIn, flipOut, k_SETFLIPS)
#define crcCalc(y, memsrc, length) ROCC_INSTRUCTION(XCUSTOM_ACC, y, memsrc, length, k_CALC)

unsigned long read_cycles(void) {
  unsigned long cycles;
  asm volatile ("rdcycle %0" : "=r" (cycles));
  return cycles;
}


/*  Code below is adapted from  Sven Reifegerste
    Source URL:  http://www.zorc.breitbandkatze.de/crc.html
*/



int order = 32;
unsigned long polynom = 0x4c11db7;
int direct = 1;
unsigned long crcinit = 0xffffffff;
unsigned long crcxor = 0xffffffff;
int refin = 1;
int refout = 1;

// 'order' [1..32] is the CRC polynom order, counted without the leading '1' bit
// 'polynom' is the CRC polynom without leading '1' bit
// 'direct' [0,1] specifies the kind of algorithm: 1=direct, no augmented zero bits
// 'crcinit' is the initial CRC value belonging to that algorithm
// 'crcxor' is the final XOR value
// 'refin' [0,1] specifies if a data byte is reflected before processing (UART) or not
// 'refout' [0,1] specifies if the CRC will be reflected before XOR

// internal global values:
unsigned long crcmask;
unsigned long crchighbit;
unsigned long crcinit_direct;
unsigned long crcinit_nondirect;
unsigned long crctab[256];


unsigned long reflect (unsigned long crc, int bitnum) {
  // reflects the lower 'bitnum' bits of 'crc'
  unsigned long i, j = 1, crcout = 0;
  for (i = (unsigned long)1 << (bitnum - 1); i; i >>= 1) {
    if (crc & i) {
      crcout |= j;
    }
    j <<= 1;
  }
  return crcout;
}



unsigned long crcbitbybit(unsigned char* p, unsigned long len) {
  // bit by bit algorithm with augmented zero bytes.
  // does not use lookup table, suited for polynom orders between 1...32.
  unsigned long i, j, c, bit;
  unsigned long crc = crcinit_nondirect;

  for (i = 0; i < len; i++) {
    c = (unsigned long)*p++;
    if (refin) {
      c = reflect(c, 8);
    }
    for (j = 0x80; j; j >>= 1) {
      bit = crc & crchighbit;
      crc <<= 1;
      if (c & j) {
        crc |= 1;
      }
      if (bit) {
        crc ^= polynom;
      }
    }
  } 

  for (i = 0; i < order; i++) {
    bit = crc & crchighbit;
    crc <<= 1;
    if (bit) {
      crc ^= polynom;
    }
  }

  if (refout) {
    crc = reflect(crc, order);
  }
  crc ^= crcxor;
  crc &= crcmask;
  return crc & crcmask;
}

unsigned long crcbitbybitfast(unsigned char* p, unsigned long len) {
  // fast bit by bit algorithm without augmented zero bytes.
  // does not use lookup table, suited for polynom orders between 1...32.
  unsigned long i, j, c, bit;
  unsigned long crc = crcinit_direct;

  for (i = 0; i < len; i++) {
    c = (unsigned long)*p++;
    if (refin) {
      c = reflect(c, 8);
    }
    for (j = 0x80; j; j >>= 1) {
      bit = crc & crchighbit;
      crc <<= 1;
      if (c & j) {
        bit ^= crchighbit; 
      }
      if (bit) {
        crc ^= polynom;
      }
    }
  } 

  if (refout) {
    crc = reflect(crc, order);
  }
  crc ^= crcxor;
  crc &= crcmask;

  return crc;
}



int main() {
  printf("Running CRC\n");
  //uint16_t data [] = {0xdead, 0xbeef, 0x0bad, 0xf00d}, y;
  uint64_t data [] = {0xdeadbeef0badf00d}, y;
  uint8_t data_bytes [] = {0xDE, 0xAD, 0xBE, 0xEF, 0x0B, 0xAD, 0xF0, 0x0D};

  //Old CRC tests
  /*uint32_t polys [] =     {0x814141AB,  0x7, 0xD5,   0x1021,  0x4C11DB7,  0x4C11DB7, 0xC867, 0xC867, 0xC867};
  uint32_t flipIns [] =   {0,             0,    0,        0,          1,          0,      0,      0,      0};
  uint32_t flipOuts [] =  {0,             0,    0,        0,          1,          0,      0,      0,      0};
  uint32_t startVals [] = {0,             0,    0,        0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF,      0, 0xFFFF};
  uint32_t endXORs [] =   {0,             0,    0,        0, 0xFFFFFFFF, 0xFFFFFFFF,      0,      0,      0};
  uint32_t lengths [] =   {32,            8,    8,       16,         32,         32,     16,     16,     16};
  uint32_t memLens [] =   {64,           64,   64,       64,         64,         64,     64,     64,     64};*/

  //Current CRC tests
  uint32_t polys [] =     {0x1D, 0x1D,  0x7,   0x1021,  0x3D65,  0xC867,  0x4C11DB7, 0xA833982B, 0x814141AB};
  uint32_t flipIns [] =   {0,       1,    1,        0,       1,       0,          1,          1,          0};
  uint32_t flipOuts [] =  {0,       1,    1,        0,       1,       0,          1,          1,          0};
  uint32_t startVals [] = {0xFF, 0xFF, 0xFF,   0x1D0F,       0,  0xFFFF, 0xFFFFFFFF, 0xFFFFFFFF,          0};
  uint32_t endXORs [] =   {0xFF,    0,    0,        0,  0xFFFF,       0, 0xfFFFFFFF, 0xFFFFFFFF,          0};
  uint32_t lengths [] =   {8,       8,    8,       16,      16,      16,         32,         32,         32};
  uint32_t memLens [] =   {64,     64,   64,       64,      64,      64,         64,         64,         64};

	uint64_t crc = 0;
  unsigned long bit;
  uint64_t total = 0;
  uint32_t cyclesStart;
  uint32_t numCRCs = sizeof(polys)/sizeof(polys[0]);

  cyclesStart = read_cycles();
  for (int i = 0; i < numCRCs; i++) {
    // Have to assign explicit variables because the assembly macros act odd otherwise
    uint32_t poly = polys[i];
    uint32_t length = lengths[i];
    uint32_t memLen = memLens[i];
    uint32_t flipIn = flipIns[i];
    uint32_t flipOut = flipOuts[i];
    uint32_t startVal = startVals[i];
    uint32_t endXOR = endXORs[i];
    //printf("CRC polynomial:  %lu\n", poly);
    //printf("CRC size:  %lu\n", length);
    //printf("CRC memLen: %llu\n", memLen);
    crcSetPoly(y, poly, length);
    crcSetStartEndVals(y, startVal, endXOR);
    crcSetFlips(y, flipIn, flipOut);
  	crcCalc(crc, &data, memLen);
    total += crc;
    printf("CRC output:  %lu\n", crc);
  }
  uint32_t cyclesEnd = read_cycles();
  printf("Duration 1 was:  %llu\n\n", cyclesEnd - cyclesStart);



  cyclesStart = read_cycles();
  for (int i = 0; i < numCRCs; i++) {
    polynom = polys[i];
    order = lengths[i];
    refin = flipIns[i];
    refout = flipOuts[i];
    crcinit = startVals[i];
    crcxor = endXORs[i];

    crcmask = ((((unsigned long)1 << (order - 1)) - 1) << 1) | 1;
    crchighbit = (unsigned long)1 << (order - 1);
    if (!direct) {
      crcinit_nondirect = crcinit;
      crc = crcinit;
      for (int j = 0; j < order; j++) {
        bit = crc & crchighbit;
        crc <<= 1;
        if (bit) {
          crc ^= polynom;
        }
      }
      crc &= crcmask;
      crcinit_direct = crc;
    } else {
      crcinit_direct = crcinit;
      crc = crcinit;
      for (int j=0; j<order; j++) {
        bit = crc & 1;
        if (bit) crc^= polynom;
        crc >>= 1;
        if (bit) {
          crc |= crchighbit;
        }
      } 
      crcinit_nondirect = crc;
    }

    crc = crcbitbybit(data_bytes, 8);
    total += crc;
    //printf("CRC output:  %lu\n", crc);
  }
  cyclesEnd = read_cycles();
  printf("Duration 2 was:  %llu\n\n", cyclesEnd - cyclesStart);




  cyclesStart = read_cycles();
  for (int i = 0; i < numCRCs; i++) {
    polynom = polys[i];
    order = lengths[i];
    refin = flipIns[i];
    refout = flipOuts[i];
    crcinit = startVals[i];
    crcxor = endXORs[i];

    crcmask = ((((unsigned long)1 << (order - 1)) - 1) << 1) | 1;
    crchighbit = (unsigned long)1 << (order - 1);
    if (!direct) {
      crcinit_nondirect = crcinit;
      crc = crcinit;
      for (int j = 0; j < order; j++) {
        bit = crc & crchighbit;
        crc <<= 1;
        if (bit) {
          crc ^= polynom;
        }
      }
      crc &= crcmask;
      crcinit_direct = crc;
    } else {
      crcinit_direct = crcinit;
      crc = crcinit;
      for (int j = 0; j < order; j++) {
        bit = crc & 1;
        if (bit) {
          crc ^= polynom;
        }
        crc >>= 1;
        if (bit) {
          crc |= crchighbit;
        }
      } 
      crcinit_nondirect = crc;
    }

    crc = crcbitbybitfast(data_bytes, 8);
    total += crc;
    //printf("CRC output:  %lu\n", crc);
  }
  cyclesEnd = read_cycles();
  printf("Duration 3 was:  %llu\n\n", cyclesEnd - cyclesStart);


  printf("Totals were:  %llu\n\n", total);
	return 0;
}
