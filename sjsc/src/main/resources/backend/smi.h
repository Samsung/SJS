/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __SMI_H__
#define __SMI_H__

// Small integers --- SMIs --- are 31-bit signed integers with 0x1 signifying undefined
typedef union {
    int32_t asBits;
    struct {
        uint8_t isUndef : 1; // low order --- little-endian
        int32_t val : 31;
    } i;
} smi_t;

// This is 4 simple integer ALU instructions on both ARM and x86-64 using -O2
static inline smi_t __smi_plus(smi_t x, smi_t y) {
    return ((smi_t){ .asBits = ((x.asBits + y.asBits) | (0x1 & (x.asBits | y.asBits))) });
}
static inline smi_t __smi_sub(smi_t x, smi_t y) {
    return ((smi_t){ .asBits = ((x.asBits - y.asBits) | (0x1 & (x.asBits | y.asBits))) });
}

static inline smi_t __smi_mul(smi_t x, smi_t y) {
    // If I use (x.asBits * (y.asBits >> 1)) I get an extra instruction on both architectures... but
    // (x.asBits * (y.asBits >> 1)) == (x * 2 * (y * 2 / 2)) == x * y * 2, so it should be
    // equivalent...
    return ((smi_t){ .asBits = ((x.asBits * y.asBits >> 1) | (0x1 & (x.asBits | y.asBits))) });
}

// 10 on amd64... ARM generates software div
static inline smi_t __smi_div(smi_t x, smi_t y) {
    // If I use (x.asBits * (y.asBits >> 1)) I get an extra instruction on both architectures... but
    // (x.asBits * (y.asBits >> 1)) == (x * 2 * (y * 2 / 2)) == x * y * 2, so it should be
    // equivalent...
    return ((smi_t){ .asBits = (((x.asBits / y.asBits) << 1) | (0x1 & (x.asBits | y.asBits))) });
}

// warning: lossy!
static inline smi_t __int_to_smi(int x) {
    // 0 low bit
    return ((smi_t){ .asBits = x << 1 });
}

// TODO: boxed/encoded booleans...
static inline bool __smi_eq(smi_t x, smi_t y) {
    return x.asBits == y.asBits || (x.i.isUndef && y.i.isUndef);
}

#endif // __SMI_H
