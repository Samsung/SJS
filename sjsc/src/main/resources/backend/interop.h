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
#include <errno.h>
#include <runtime.h>
typedef value_t Value;
#define __UNDEF_BYTES__ (0xFFFF000700000000ULL)
#define __UNDEF__ ((value_t)__UNDEF_BYTES__)

#define OP_GETVAR(x) (x)

void OP_CLEARARGS();
Value OP_POPARG();
void OP_PUSHARG(Value x);

extern wchar_t typeof_undefined[];
extern wchar_t typeof_number[];
extern wchar_t typeof_boolean[];


static inline Value OP_TYPEOF(Value x) {
    // TODO
    assert(false);
    Value s = __UNDEF__;
    if (val_is_undef(x)) {
        s = string_as_val(typeof_undefined);
    } else if (val_is_int(x) || val_is_double(x)) {
        s = string_as_val(typeof_number);
    } else if (val_is_boolean(x)) {
        s = string_as_val(typeof_boolean);
    }
    return s;
}
/* TODO: This is implemented the way jscomp requires, as a function (jscomp transforms x++ into an
 * explicit assignment, storing old value as appropriate).  The current typed slow mode backend
 * expects OP_INC to handle the lvalue semantics itself, which will fail.  The typed slow mode
 * backend needs to be fixed to use the jscomp semantics.
 */
static inline Value OP_INC(Value x) {
    assert(val_is_int(x));
    x.i++;
    return x;
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
    // x.b = !x.b; except we're not using the lowest bit
    x.i ^= 0x10; // clear the true bit if set, set it if not
    return x;
}

// Binops
// TODO: All binops also have a read-modify-write (RMW) equivalent for fields, arrays, variables.
// Variables get desugared in the typed-slow-path codegen, which is fine.  For properties and
// arrays, there will often be a lot of redundancy in the desugaring (which is currently done in
// fast mode as well).  Need to implement field- and array-specific mrw ops which accept a function
// pointer to an appropriate binop and the RHS of the rmw operation.
// TODO: Should also move the typed-slow-mode-specific ops to a separate header at least
// TODO: OP_USHR
static inline Value OP_SHR(Value x, Value y) {
    // TODO: ToInt32!
    assert (val_is_int(x));
    assert (val_is_int(y));
    return (int_as_val(x.i >> y.i));
}
static inline Value OP_SHL(Value x, Value y) {
    // TODO: ToInt32!
    assert (val_is_int(x));
    assert (val_is_int(y));
    return (int_as_val(x.i << y.i));
}
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
    //fwprintf(stderr, L"adding slowly...\n");
    // TODO: propagate undef
    if (val_is_int(x)) {
        //fwprintf(stderr, L"x:int\n");
        if (val_is_int(y)) {
            //fwprintf(stderr, L"y:int\n");
            return int_as_val(x.i+y.i);
        } else {
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
            //fwprintf(stderr, L"y:float\n");
            // TODO; float encoding...
            return double_as_val_noenc(x.i+val_as_double_noenc(y));
        }
    } else {
        // TODO: need this assertion, but it will fail until we do float encoding
        //assert(val_is_double(x));
        //fwprintf(stderr, L"x:float\n");
        if (val_is_int(y)) {
            //fwprintf(stderr, L"y:int\n");
            // TODO; float encoding...
            return double_as_val_noenc(y.i+val_as_double_noenc(x));
        } else {
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
            //fwprintf(stderr, L"y:float\n");
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
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(x.i-val_as_double_noenc(y));
        }
    } else {
        // TODO: need this assertion, but it will fail until we do float encoding
        //assert(val_is_double(x));
        if (val_is_int(y)) {
            // TODO; float encoding...
            return double_as_val_noenc(y.i-val_as_double_noenc(x));
        } else {
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
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
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
            // TODO; float encoding...
            return double_as_val_noenc(x.i*val_as_double_noenc(y));
        }
    } else {
        // TODO: need this assertion, but it will fail until we do float encoding
        //assert(val_is_double(x));
        if (val_is_int(y)) {
            // TODO; float encoding...
            return double_as_val_noenc(y.i*val_as_double_noenc(x));
        } else {
            // TODO: need this assertion, but it will fail until we do float encoding
            //assert(val_is_double(y));
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
    // TODO: make this distinct from null/undefined
    return object_as_val(NULL); 
}
static inline Value OP_INFINITY() {
    // TODO: encoding
    return double_as_val_noenc((double)INFINITY); 
}
static inline Value OP_NULL() {
    return object_as_val(NULL); 
}
static inline Value OP_NUMBER(double x) {
    // TODO: produce an integer when in range
    // TODO: encoding
    if (isfinite(x)) {
        int32_t i = lrint(x);
        // doubles can represent integers outside the 32-bit signed int range, so lrint can fail
        if (errno != EDOM && errno != ERANGE) {
            return int_as_val(x);
        }
    }
    return double_as_val_noenc(x); 
}
static inline Value OP_STRING(wchar_t* x) {
    if (((uintptr_t)x) & 0x7) {
        // TODO: The right fix for this is to have the jscomp compiler align string literals, not to
        // realloc here.  But right now, if we don't do this we get weird behaviors when string
        // bitmasking misbehaves on unaligned string pointers
        //fwprintf(stderr, L"Warning: string constant [%ls] is unaligned: %p\n", x, x);
        return string_as_val(wcsdup(x)); 
    } else {
        return string_as_val(x); 
    }
}

Value OP_FUNCTION(void* fptr, Value envptr);

#define _FIXED_LAYOUT_CONVERSION_SLOTS_ 10
static inline Value OP_NEWOBJECT() {
    // arbitrarily allocate 10 slots for possible future fixed-layout conversion
    // TODO: After the new object layout is complete, try to make the size of this allocation a
    // power of 2 for less fragmentation
    object_t* o = alloc_object_lit(NULL, NULL, _FIXED_LAYOUT_CONVERSION_SLOTS_, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__, __UNDEF__);
    return object_as_val(o);
}
static inline Value OP_NEWARRAY() {
    return object_as_val(array___lit(NULL, 0));
}

#define OP_SETINDEX(env, i, v) (((preenv_t)val_as_pointer(env))[i] = val_as_pointer(v))
#define OP_SETINDEXSTAR(env, i, v) (*(((preenv_t)val_as_pointer(env))[i]) = (v))
#define OP_GETINDEX(env, i) (object_as_val((object_t*)((preenv_t)val_as_pointer(env))[i]))
#define OP_GETINDEXSTAR(env, i) (*(((preenv_t)val_as_pointer(env))[i]))

#define OP_SETINDEXSTARUNBOXED(env, i, v) (*(((preenv_t)(env))[i]) = (v))
#define OP_GETINDEXSTARUNBOXED(env, i) (*(((preenv_t)(env))[i]))
#define OP_GETINDEXUNBOXED(env, i) ((((preenv_t)(env))[i]))

#define OP_GETVARSTAR(x) (*(value_t*)(val_as_pointer(x)))
#define OP_SETVARSTAR(x, y) ((*(value_t*)(val_as_pointer(x))) = y)

#define OP_FALSYOR(a, b) (!val_is_falsy(a) ? (a) : (b))
#define OP_FALSYAND(a, b) (val_is_falsy(a) ? (a) : (b))

static inline Value OP_NEWENV(int x) {
    Value v;
#ifdef SMALL_POINTER
    v.tags.tag = 0;
#endif
    v.ptr = MEM_ALLOC(x * sizeof(value_t*));
    return v;
}

// TODO: OP_NEWREGEXP

// TODO: should this be a boolean arg (as in, the args will always literall be true or false?)
static inline Value OP_BOOLEAN(bool x) {
    return boolean_as_val(x); 
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
Value OP_GETPROP(Value x, Value y);
Value OP_SETPROP(Value x, Value y, Value z);
Value OP_SETGETTER(Value x, Value y);
Value OP_SETSETTER(Value x, Value y);
Value OP_DELPROP(Value x, Value y);

// Control
// TODO: truthiness
#define OP_IFTRUE(x,l) if (!val_is_falsy(x)) goto l
#define OP_IFFALSE(x,l) if (val_is_falsy(x)) goto l
#define OP_JUMP(l) goto l
#define OP_JUMPNE(x, y, l) if((x) != (y)) goto l

// Running code
Value OP_CALL(Value x);
// Invoked by typed slow mode
Value TYPEDCALL(Value recv, Value f, int n, ...);

static inline Value OP_NEW(Value x) {
    // TODO: What's this operand?
    // Oh, it's the constructor function....
    assert(false);
    return object_as_val(NULL); 
}

// The following are only used by typed slow mode
Value OP_field_inc(Value obj, int findex, bool postfix);
Value OP_field_dec(Value obj, int findex, bool postfix);

#define EMPTY_STATEMENT do { } while(0)

// Implementation "private":
int __prop_indirection_lookup(wchar_t* pname);
