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
/*
 * Implementation of Math object in JS
 */

#include <runtime.h>
#include <jsmath.h>
#include <math.h>
#include <stdlib.h>
#include <float.h>
#include <limits.h>
#ifdef USE_GC
#include <gc.h>
#endif
#include <string.h>

// TODO: Writing to fields of Math is a no-op; subsequent reads don't observe the writes.
// TODO: Write optimization pass to inline static accesses to the Math object.

// Note: We're telling clang these are all pure, so we better only use these macros for pure (e.g.,
// math) functions!
#define prim2void_method(f) __attribute__ ((pure)) value_t ___##f(env_t env, value_t o) { return (value_t)f(); } \
                            struct { env_t env; value_t (*func)(env_t, value_t); } ___##f##_clos = { NULL, ___##f }; \
                            value_t f##_box = (value_t)(void*)&___##f##_clos;
#define prim2double_method(f) __attribute__ ((pure)) value_t ___##f(env_t env, value_t o, value_t x) { return (value_t)f(x.dbl); } \
                            struct { env_t env; value_t (*func)(env_t, value_t, value_t); } ___##f##_clos = { NULL, ___##f }; \
                            value_t f##_box = (value_t)(void*)&___##f##_clos;
#define prim2doubledouble_method(f) __attribute__ ((pure)) value_t ___##f(env_t env, value_t o, value_t x, value_t y) { return (value_t)f(x.dbl, y.dbl); } \
                            struct { env_t env; value_t (*func)(env_t, value_t, value_t, value_t); } ___##f##_clos = { NULL, ___##f }; \
                            value_t f##_box = (value_t)(void*)&___##f##_clos;
#define prim2double_bool_method(f) __attribute__ ((pure)) value_t ___##f(env_t env, value_t o, value_t x) { return boolean_as_val(f(x.dbl)); } \
                            struct { env_t env; value_t (*func)(env_t, value_t, value_t); } ___##f##_clos = { NULL, ___##f }; \
                            value_t f##_box = (value_t)(void*)&___##f##_clos;
// Constants
// NOTE: on 32 bit platforms, these constants are of type long double (??) so we need to cast
// TODO: figure out what to do with these constants w.r.t. float shifting for interop
value_t E_box = (value_t)(double)M_E;
value_t LN2_box = (value_t)(double)M_LN2;
value_t LN10_box = (value_t)(double)M_LN10;
value_t LOG2E_box = (value_t)(double)M_LOG2E;
value_t LOG10E_box = (value_t)(double)M_LOG10E;
value_t PI_box = (value_t)(double)M_PI;
value_t SQRT1_2_box = (value_t)(double)M_SQRT1_2;
value_t SQRT2_box = (value_t)(double)M_SQRT2;

prim2double_method(fabs)
prim2double_method(acos)
prim2double_method(acosh)
prim2double_method(asin)
prim2double_method(asinh)
prim2double_method(atan)
prim2double_method(atanh)
prim2doubledouble_method(atan2)
prim2double_method(cbrt)
prim2double_method(ceil)
prim2double_method(cos)
prim2double_method(cosh)
prim2double_method(exp)
prim2double_method(expm1)
prim2double_method(floor)
prim2doubledouble_method(hypot)
prim2double_method(log)
prim2double_method(log1p)
prim2double_method(log10)
prim2double_method(log2)
prim2doubledouble_method(fmax)
prim2doubledouble_method(fmin)
prim2doubledouble_method(pow)
prim2void_method(drand48)
prim2double_method(round)
prim2double_method(sin)
prim2double_method(sinh)
prim2double_method(sqrt)
prim2double_method(tan)
prim2double_method(tanh)
prim2double_method(trunc)


//value_t abs_box = (value_t)(void*)__fabs;
//value_t acos_box = (value_t)(void*)__acos;
//value_t acosh_box = (value_t)(void*)__acosh;
//value_t asin_box = (value_t)(void*)__asin;
//value_t asinh_box = (value_t)(void*)__asinh;
//value_t atan_box = (value_t)(void*)__atan;
//value_t atanh_box = (value_t)(void*)__atanh;
//value_t atan2_box = (value_t)(void*)__atan2;
//value_t cbrt_box = (value_t)(void*)__cbrt;
//value_t ceil_box = (value_t)(void*)__ceil;
//// TODO: clz32 impl!
////double __clz32(double x);
//value_t cos_box = (value_t)(void*)__cos;
//value_t cosh_box = (value_t)(void*)__cosh;
//value_t exp_box = (value_t)(void*)__exp;
//value_t expm1_box = (value_t)(void*)__expm1;
//value_t floor_box = (value_t)(void*)__floor;
double fround(double x) {
    return (float)x;
}
prim2double_method(fround)
//value_t fround_box = (value_t)(void*)__fround;
// TODO: variable arity in spec, binary in C: square of sum of squares
//value_t hypot_box = (value_t)(void*)__hypot;
// TODO: Verify that this is the correct interpretation of "32-bit integer multiplication"
double imul(double x, double y) {
    // TODO: Mozilla gives a potentially faster bit-shifting implementation, which will require
    // careful sign management:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/imul
    return (double)(((int32_t)x) * ((int32_t)y));
}
prim2doubledouble_method(imul)
//value_t imul_box = (value_t)(void*)__imul;
//value_t log_box = (value_t)(void*)__log;
//value_t log1p_box = (value_t)(void*)__log1p;
//value_t log10_box = (value_t)(void*)__log10;
//value_t log2_box = (value_t)(void*)__log2;
//// TODO: variable arity max and min
//value_t max_box = (value_t)(void*)__fmax;
//value_t min_box = (value_t)(void*)__fmin;
//value_t pow_box = (value_t)(void*)__pow;
//// TODO: initialize random number generator
//value_t random_box = (value_t)(void*)__drand48;
//value_t round_box = (value_t)(void*)__round;
// Implementation as per the specification at https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/sign
double sign(double x) {
    if (x == 0.0 || isnan(x)) {
        return x;
    }
    return x > 0 ? 1 : -1;
}
prim2double_method(sign)
//value_t sign_box = (value_t)(void*)__sign;
//value_t sin_box = (value_t)(void*)__sin;
//value_t sinh_box = (value_t)(void*)__sinh;
//value_t sqrt_box = (value_t)(void*)__sqrt;
//value_t tan_box = (value_t)(void*)__tan;
//value_t tanh_box = (value_t)(void*)__tanh;
wchar_t* __toSource() {
    return L"Math";
}
value_t toSource_box = (value_t)(void*)__toSource;
//value_t trunc_box = (value_t)(void*)__trunc;

extern int math_table[];

object_t __ffi_Math = {
    .vtbl = math_table,
    .fields = { 
                (value_t)M_E, // TODO: What do I do here about the cast & rotation for NAN boxing?
                (value_t)M_LN2,
                (value_t)M_LN10,
                (value_t)M_LOG2E,
                (value_t)M_LOG10E,
                (value_t)M_PI,
                (value_t)M_SQRT1_2,
                (value_t)M_SQRT2,
                (value_t)(void*)&___fabs_clos,
                (value_t)(void*)&___acos_clos,
                (value_t)(void*)&___acosh_clos,
                (value_t)(void*)&___asin_clos,
                (value_t)(void*)&___asinh_clos,
                (value_t)(void*)&___atan_clos,
                (value_t)(void*)&___atanh_clos,
                (value_t)(void*)&___atan2_clos,
                (value_t)(void*)&___cbrt_clos,
                (value_t)(void*)&___ceil_clos, //&clz32_clos,
                (value_t)(void*)&___cos_clos,
                (value_t)(void*)&___cosh_clos,
                (value_t)(void*)&___exp_clos,
                (value_t)(void*)&___expm1_clos,
                (value_t)(void*)&___floor_clos,
                (value_t)(void*)&___fround_clos,
                (value_t)(void*)&___hypot_clos,
                (value_t)(void*)&___imul_clos,
                (value_t)(void*)&___log_clos,
                (value_t)(void*)&___log1p_clos,
                (value_t)(void*)&___log10_clos,
                (value_t)(void*)&___log2_clos,
                (value_t)(void*)&___fmax_clos,
                (value_t)(void*)&___fmin_clos,
                (value_t)(void*)&___pow_clos,
                (value_t)(void*)&___drand48_clos, //&random_clos,
                (value_t)(void*)&___round_clos,
                (value_t)(void*)&___sign_clos,
                (value_t)(void*)&___sin_clos,
                (value_t)(void*)&___sinh_clos,
                (value_t)(void*)&___sqrt_clos,
                (value_t)(void*)&___tan_clos,
                (value_t)(void*)&___tanh_clos,
                (value_t)(void*)&__toSource,
                (value_t)(void*)&___trunc_clos,
              }
};

value_t Math_box = { .obj = &__ffi_Math };

// boxed
value_t* Math = &Math_box;

// TODO: Don't need these with new object rep
value_t EPSILON_box = (value_t)DBL_EPSILON;
value_t MAX_SAFE_INTEGER_box = (value_t)((0x2ull << 53) - 1);
value_t MAX_VALUE_box = (value_t)DBL_MAX;
value_t MIN_SAFE_INTEGER_box = (value_t)(-((0x2ull << 53) - 1));
value_t MIN_VALUE_box = (value_t)DBL_MIN;
value_t NaN_box = (value_t)(double)NAN;
value_t NEGATIVE_INFINITY_box = (value_t)(double)(-INFINITY);
value_t POSITIVE_INFINITY_box = (value_t)(double)INFINITY;

// static methods
prim2double_bool_method(isfinite);
// ^^ Note ANSI C also defines finite(), and they differ on results for NaN.  isfinite matches the
// FF docs
prim2double_bool_method(isnan);
// TODO: isInteger(), isSafeInteger()
// TODO: parseFloat, parseInt

//value_t isFinite_box = (value_t)(void*)__isfinite;
//value_t isNaN_box = (value_t)(void*)__isnan;

extern int number_table[];

object_t __ffi_Number = {
    .vtbl = number_table,
    .fields = { (value_t)DBL_EPSILON, // TODO: Again, NaN boxing...
                (value_t)((0x2ull << 53) - 1),
                (value_t)DBL_MAX,
                (value_t)(-((0x2ull << 53) - 1)),
                (value_t)DBL_MIN,
                (value_t)(double)NAN,
                (value_t)(double)(-INFINITY),
                (value_t)(double)INFINITY,
                (value_t)(void*)&___isfinite_clos,
                (value_t)(void*)&___isnan_clos
              }
};

object_t* Number_box = &__ffi_Number;

object_t** Number = &Number_box;

// Misc. intrinsics implementations
//

// TODO: these number -> string conversions would be faster for large numbers
// if we precomputed the string length precisely and sprintf'd directly to a fresh
// buffer allocation.  For integers the math is easy but it's not clear a modulus is
// faster than copying for smaller (more common) values.  For doubles, the math
// is a bit tricky.

// The largest number of decimal digits possible for a base 10 ASCII
// representation of an IEEE double is
// (log_10 2^53 + 1) + (log_10 2^11 + 1)
// plus the . and null terminator, which is < 64
#define MAX_FLT_ASCII_LEN 64
wchar_t toFixed_buf[MAX_FLT_ASCII_LEN];

wchar_t* __fp__toFixed(double x, int precision) {
    int written;
    assert (precision >=0);
    // TODO: rewrite to use asprintf
    // TODO: We can pick an upper bound of our choosing, but the spec mandates
    // support for 0-20 inclusive
    switch (precision) {
        case 0:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.0f", x); break;
        case 1:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.1f", x); break;
        case 2:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.2f", x); break;
        case 3:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.3f", x); break;
        case 4:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.4f", x); break;
        case 5:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.5f", x); break;
        case 6:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.6f", x); break;
        case 7:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.7f", x); break;
        case 8:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.8f", x); break;
        case 9:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.9f", x); break;
        case 10: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.10f", x); break;
        case 11: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.11f", x); break;
        case 12: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.12f", x); break;
        case 13: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.13f", x); break;
        case 14: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.14f", x); break;
        case 15: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.15f", x); break;
        case 16: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.16f", x); break;
        case 17: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.17f", x); break;
        case 18: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.18f", x); break;
        case 19: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.19f", x); break;
        case 20: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.20f", x); break;
        default:
            return NULL; // TODO: exceptions
    }
    assert (written + 1 < MAX_FLT_ASCII_LEN);
    wchar_t* ret = (wchar_t*)MEM_ALLOC_ATOMIC((written+1)*sizeof(wchar_t));
    // Almost every C coding manual forbids strcpy, but we know by the
    // max length calculation above + the use of sprintf that the input
    // will always be null-terminated (modulo a bug in sprintf)
    wcscpy(ret, toFixed_buf);
    return ret;
}
// To preserve output compatibility with JS, we must format this as if it were
// a floating point number with no fractional part
wchar_t* __int__toFixed(int x, int precision) {
    return __fp__toFixed((double)x, precision);
}
wchar_t* __fp__toExponential(double x, int precision) {
    int written;
    assert (precision >=0);
    // TODO: rewrite to use asprintf
    // TODO: We can pick an upper bound of our choosing, but the spec mandates
    // support for 0-20 inclusive
    switch (precision) {
        case 0:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.0e", x); break;
        case 1:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.1e", x); break;
        case 2:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.2e", x); break;
        case 3:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.3e", x); break;
        case 4:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.4e", x); break;
        case 5:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.5e", x); break;
        case 6:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.6e", x); break;
        case 7:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.7e", x); break;
        case 8:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.8e", x); break;
        case 9:  written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.9e", x); break;
        case 10: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.10e", x); break;
        case 11: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.11e", x); break;
        case 12: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.12e", x); break;
        case 13: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.13e", x); break;
        case 14: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.14e", x); break;
        case 15: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.15e", x); break;
        case 16: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.16e", x); break;
        case 17: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.17e", x); break;
        case 18: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.18e", x); break;
        case 19: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.19e", x); break;
        case 20: written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%.20e", x); break;
        default:
            return NULL; // TODO: exceptions
    }
    assert (written + 1 < MAX_FLT_ASCII_LEN);
    wchar_t* ret = (wchar_t*)MEM_ALLOC_ATOMIC((written+1)*sizeof(wchar_t));
    // Almost every C coding manual forbids strcpy, but we know by the
    // max length calculation above + the use of sprintf that the input
    // will always be null-terminated (modulo a bug in sprintf)
    wcscpy(ret, toFixed_buf);
    return ret;
}
wchar_t* __int__toExponential(int x, int precision) {
    return __fp__toExponential((double)x, precision);
}

// Eventually this should be reworked to be the intrinsic for <int>.toString()
wchar_t* __int__toString(int x) {
    int written = 0;
    written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%d", x);
    wchar_t* ret = (wchar_t*)MEM_ALLOC_ATOMIC((written+1)*sizeof(wchar_t));
    wcscpy(ret, toFixed_buf);
    return ret;
}
// TODO: ECMA262 S7.1.12.1 is horrendous
wchar_t* __fp__toString(double x) {
    int written = 0;
    // ECMA-262 S7.1.12.1 item 2 says all 0s print as "0"
    if (x == -0.0) {
        x = 0.0;
    }
    written = swprintf(toFixed_buf, MAX_FLT_ASCII_LEN, L"%g", x);
    wchar_t* ret = (wchar_t*)MEM_ALLOC_ATOMIC((written+1)*sizeof(wchar_t));
    wcscpy(ret, toFixed_buf);
    return ret;
}
wchar_t* (* const string_of_int)(int x) = __int__toString;
