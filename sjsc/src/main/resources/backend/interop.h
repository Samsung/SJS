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

#include <runtime.h>
typedef value_t Value;
#define __UNDEF__ ((value_t)0xFFFF000700000000ULL)

void OP_CLEARARGS();
Value OP_POPARG();
void OP_PUSHARG(Value x);

static inline Value OP_TYPEOF(Value x) {
    // TODO
    assert(false);
    return (Value)(0xFFFF000700000000ULL);
}
static inline Value OP_POS(Value x) {
    // TODO
    assert(false);
    return (Value)(0xFFFF000700000000ULL);
}
static inline Value OP_NEG(Value x) {
    // TODO
    if (val_is_int(x)) {
        x.i = -x.i;
        return x;
    } else if (val_is_double(x)) {
        x.dbl = -x.dbl;
        return x;
    } else {
        assert(false);
        return (Value)(0xFFFF000700000000ULL);
    }
}

// TODO: INC, DEC seem like they need to be macros

static inline Value OP_BITNOT(Value x) {
    int xv = val_is_int(x) ? x.i : ___int_of_float(val_as_double_noenc(x));
    return int_as_val(~xv);
}
static inline Value OP_LOGNOT(Value x) {
    assert (val_is_boolean(x));
    x.b = !x.b;
    return x;
}

// Binops
static inline Value OP_BITOR(Value x, Value y) {
    assert (val_is_double(x) || val_is_int(x));
    assert (val_is_double(y) || val_is_int(y));
    int xv = val_is_int(x) ? x.i : ___int_of_float(val_as_double_noenc(x));
    int yv = val_is_int(y) ? y.i : ___int_of_float(val_as_double_noenc(y));
    return int_as_val(xv | yv);
}
static inline Value OP_BITXOR(Value x, Value y) {
    assert (val_is_double(x) || val_is_int(x));
    assert (val_is_double(y) || val_is_int(y));
    int xv = val_is_int(x) ? x.i : ___int_of_float(val_as_double_noenc(x));
    int yv = val_is_int(y) ? y.i : ___int_of_float(val_as_double_noenc(y));
    return int_as_val(xv ^ yv);
}
static inline Value OP_BITAND(Value x, Value y) {
    assert (val_is_double(x) || val_is_int(x));
    assert (val_is_double(y) || val_is_int(y));
    int xv = val_is_int(x) ? x.i : ___int_of_float(val_as_double_noenc(x));
    int yv = val_is_int(y) ? y.i : ___int_of_float(val_as_double_noenc(y));
    return int_as_val(xv & yv);
}
static inline Value OP_EQ(Value x, Value y) {
    // TODO: NaN, coercions
    return boolean_as_val(x.box == y.box);
}
static inline Value OP_NE(Value x, Value y) {
    // TODO: NaN, coercions
    return boolean_as_val(x.box != y.box);
}
static inline Value OP_STRICTEQ(Value x, Value y) {
    // TODO: NaN
    return boolean_as_val(x.box == y.box);
}
static inline Value OP_STRICTNE(Value x, Value y) {
    // TODO: NaN
    return boolean_as_val(x.box != y.box);
}
static inline Value OP_LT(Value x, Value y) {
    // TODO This is quite slow vs. breaking out the cases...
    double xv = val_is_int(x) ? (double)x.i : val_as_double_noenc(x);
    double yv = val_is_int(y) ? (double)y.i : val_as_double_noenc(y);
    return boolean_as_val(xv < yv);
}
static inline Value OP_GT(Value x, Value y) {
    // TODO This is quite slow vs. breaking out the cases...
    double xv = val_is_int(x) ? (double)x.i : val_as_double_noenc(x);
    double yv = val_is_int(y) ? (double)y.i : val_as_double_noenc(y);
    return boolean_as_val(xv > yv);
}
static inline Value OP_LE(Value x, Value y) {
    // TODO This is quite slow vs. breaking out the cases...
    double xv = val_is_int(x) ? (double)x.i : val_as_double_noenc(x);
    double yv = val_is_int(y) ? (double)y.i : val_as_double_noenc(y);
    return boolean_as_val(xv <= yv);
}
static inline Value OP_GE(Value x, Value y) {
    // TODO This is quite slow vs. breaking out the cases...
    double xv = val_is_int(x) ? (double)x.i : val_as_double_noenc(x);
    double yv = val_is_int(y) ? (double)y.i : val_as_double_noenc(y);
    return boolean_as_val(xv >= yv);
}


static inline Value OP_ADD(Value x, Value y) {
    fwprintf(stderr, L"adding slowly...\n");
    // TODO: propagate undef
    if (val_is_int(x)) {
        fwprintf(stderr, L"x:int\n");
        if (val_is_int(y)) {
            fwprintf(stderr, L"y:int\n");
            return int_as_val(x.i+y.i);
        } else {
            assert(val_is_double(y));
            fwprintf(stderr, L"y:float\n");
            // TODO; float encoding...
            return double_as_val_noenc(x.i+val_as_double_noenc(y));
        }
    } else {
        assert(val_is_double(x));
        fwprintf(stderr, L"x:float\n");
        if (val_is_int(y)) {
            fwprintf(stderr, L"y:int\n");
            // TODO; float encoding...
            return double_as_val_noenc(y.i+val_as_double_noenc(x));
        } else {
            assert(val_is_double(y));
            fwprintf(stderr, L"y:float\n");
            // TODO; float encoding...
            return double_as_val_noenc(val_as_double_noenc(x)+val_as_double_noenc(y));
        }
    }
}
static inline Value OP_SUB(Value x, Value y) {
    // TODO: propagate undef
    if (val_is_int(x)) {
        if (val_is_int(y)) {
            return int_as_val(x.i-y.i);
        } else {
            assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(x.i-val_as_double_noenc(y));
        }
    } else {
        assert(val_is_double(x));
        if (val_is_int(y)) {
            // TODO; float encoding...
            return double_as_val_noenc(y.i-val_as_double_noenc(x));
        } else {
            assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(val_as_double_noenc(x)-val_as_double_noenc(y));
        }
    }
}
static inline Value OP_MUL(Value x, Value y) {
    // TODO: propagate undef
    if (val_is_int(x)) {
        if (val_is_int(y)) {
            return int_as_val(x.i*y.i);
        } else {
            assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(x.i*val_as_double_noenc(y));
        }
    } else {
        assert(val_is_double(x));
        if (val_is_int(y)) {
            // TODO; float encoding...
            return double_as_val_noenc(y.i*val_as_double_noenc(x));
        } else {
            assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(val_as_double_noenc(x)*val_as_double_noenc(y));
        }
    }
}
static inline Value OP_DIV(Value x, Value y) {
    double xv = val_is_int(x) ? (double)x.i : val_as_double_noenc(x);
    double yv = val_is_int(y) ? (double)y.i : val_as_double_noenc(y);
    return double_as_val_noenc(xv / yv);
}
static inline Value OP_MOD(Value x, Value y) {
    assert(val_is_int(x));
    assert(val_is_int(y));
    return int_as_val(x.i % y.i);
}

static inline Value OP_NAN() {
    // TODO: encoding
    return double_as_val_noenc((double)NAN); 
}
static inline Value OP_UNDEF() {
    return __UNDEF__; 
}
static inline Value OP_NONEXISTENT() {
    assert(false);
    return object_as_val(NULL); 
}
static inline Value OP_INFINITY() {
    // TODO: encoding
    return double_as_val_noenc((double)INFINITY); 
}
static inline Value OP_NULL() {
    return object_as_val(NULL); 
}
// TODO: should this be a double?
static inline Value OP_NUMBER(Value x) {
    return x; 
}
// TODO: should this be a (c) string arg?
static inline Value OP_STRING(Value x) {
    return x; 
}

static inline Value OP_FUNCTION(Value fptr, Value envptr) {
    // allocate an untyped closure.  Note this may be used for constructors as well.
    // TODO: type validation...
    Value v;
#ifdef SMALL_POINTER
    v.tags.tag = 0;
#endif
    v.ptr = closure_alloc_prim(fptr.ptr, envptr.ptr);
    // TODO: mark this as an untyped closure...
    return v;
}

static inline Value OP_NEWOBJECT() {
    // arbitrarily allocate 10 slots for possible future fixed-layout conversion
    // TODO: After the new object layout is complete, try to make the size of this allocation a
    // power of 2 for less fragmentation
    object_t* o = alloc_object_lit(NULL, 10, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__);
    return object_as_val(o);
}
static inline Value OP_NEWARRAY() {
    return object_as_val(array___lit(0));
}

// TODO: should this argument be a C integer/float?
static inline Value OP_NEWENV(Value x) {
    Value v;
#ifdef SMALL_POINTER
    v.tags.tag = 0;
#endif
    if (val_is_int(x)) {
        v.ptr = MEM_ALLOC(x.i * sizeof(value_t*));
    } else if (val_is_double(x)) {
        v.ptr = MEM_ALLOC(___int_of_float(val_as_double_noenc(x)) * sizeof(value_t*));
    } else {
        assert(false);
    }
    return v;
}

// TODO: OP_NEWREGEXP

// TODO: should this be a boolean arg (as in, the args will always literall be true or false?)
static inline Value OP_BOOLEAN(Value x) {
    return x; 
}

static inline Value OP_NEWBOX() {
    Value v;
#ifdef SMALL_POINTER
    v.tags.tag = 0;
#endif
    v.ptr = MKBOX(__UNDEF__);
    return v;
}

// Fields
static inline Value OP_GETPROP(Value x, Value y) {
    // TODO: primitive coercions
    assert(false);
}
static inline Value OP_SETPROP(Value x, Value y) {
    // TODO: primitive coercions
    assert(false);
}
static inline Value OP_SETGETTER(Value x, Value y) {
    // TODO: primitive coercions
    assert(false);
}
static inline Value OP_SETSETTER(Value x, Value y) {
    // TODO: primitive coercions
    assert(false);
}
static inline Value OP_DELPROP(Value x, Value y) {
    // TODO: primitive coercions
    assert(false);
}

// Control
// TODO: truthiness
#define OP_IFTRUE(x,l) if (!val_is_falsy(x)) goto l
#define OP_IFFALSE(x,l) if (val_is_falsy(x)) goto l
#define OP_JUMP(l) goto l

// Running code
static inline Value OP_CALL(Value x) {
    assert (val_is_closure(x));
    // TODO: This is dynamically-typed invocation.  Check argument types!
    // TODO: For that matter, pass arguments!
    // TODO: Need tag to determine typed/untyped, and if the former then arity, types, and return
    // type.
    return (Value)(0xFFFF000700000000ULL);
}

static inline Value OP_NEW(Value x) {
    // TODO: What's this operand?
    // Oh, it's the constructor function....
    assert(false);
    return object_as_val(NULL); 
}
