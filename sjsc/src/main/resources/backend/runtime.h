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
 * This file defines the runtime layouts of SJS data structures.
 * The SJS compiler emits C code that relies on these definitions.
 * The intent is that the compiler can emit C code using these type names
 * macros, with (almost) no dependency on the actual object layout.
 * This should simplify code-gen from the higher level tool, and give
 * us an easy way to compare performance of different layout representations
 * (by swapping out versions of this header).
 */
#ifndef __SJS_RUNTIME
#define __SJS_RUNTIME

// Prelude
#include <string.h>
#include <wchar.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <assert.h>
#include <math.h>

// Pick an allocator
#if defined (LEAK_MEMORY)
#define MEM_ALLOC(x) malloc(x)
#define MEM_ALLOC_ATOMIC(x) malloc(x)
#define MEM_FREE(x) free(x)
#elif defined (USE_GC)
#include <gc.h>
#define MEM_ALLOC(x) GC_MALLOC(x)
#define MEM_ALLOC_ATOMIC(x) GC_MALLOC_ATOMIC(x)
#define MEM_FREE(x) GC_FREE(x)
#else
#error "No memory manager selected.  Must define USE_GC or LEAK_MEMORY."
#endif

// Check endianness
// clang and gcc on mac will pass this, as will the GCC on ARM
#if (__BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__)
#else
#error "Flags not set for little-endian architecture: code is only written for little-endian"
#endif

// Check pointer size, which controls whether we emit extra memory initialization
#if __SIZEOF_POINTER__ == 4
#define SMALL_POINTER 1
#elif __SIZEOF_POINTER__ == 8
#else
#error "Pointer size undefined; cannot compile code for cleaning tag bits"
#endif

#ifdef __cplusplus
extern "C" {
#endif
void print(wchar_t*);
int parseInt(wchar_t*, int);
int parseInt_noradix(wchar_t* s);
double parseFloat(wchar_t* s);
void printInt(int);
void printFloat(double);
void printFloat10(double);
#ifdef __cplusplus
}
#endif


// let C's conversion work
#define itofp(x) (x)

#ifdef __cplusplus
extern "C" {
#endif
wchar_t* __str__concat(wchar_t* a, wchar_t* b);
#ifdef __cplusplus
}
#endif

// Primitive types
#ifndef __cplusplus
typedef unsigned char bool;
#define true 1
#define false 0
#endif

// Currently unused
#include<smi.h>

// predeclaration
struct object;
struct map;

#define DOUBLE_SHIFT 0x1000000000000ULL
#define shift_double(d) (((value_t)(((value_t)(d)).box + DOUBLE_SHIFT)).box)
#define unshift_double(v) (((value_t)(v.box - DOUBLE_SHIFT)).dbl)
#define INT_TAG 0xFFFF0000LL // tag bits of integer
#define BOOL_TAG 0xFFFF0001LL // tag bits of boolean
#define UNDEF_TAG 0xFFFF0007LL // tag bits of undefined
#define PTR_MASK 0xFFFF000000000000ULL // if any of these are set, not a pointer
#define MAX_PTR  0x0000FFFFFFFFFFF8ULL // mask for largest pointer any 64 bit OS creates, clears ptr tag bits

// Pointer Tags -- 3 bits; we'll 8-byte align
#define OBJ_TAG 0x0
#define UND_TAG 0x1 // NOTE: Currently unused.
#define BOX_TAG 0x2
#define STR_TAG 0x4
#define CLS_TAG 0x0 // NOTE: Currently unused
// The UND_TAG above works with pointer types, and booleans 
// Notice that if we switch to SMIs and rejigger the boolean values, the low bit being set to 1 means its either a double or
// undefined

// Static asserts.  We use this to check the safety of a few casts later that convert between
// value_t and uint64_t.  Emscripten can't compile variadic macros of aggregate types (unions,
// structs) yet, so we exploit this size relationship in some casts...
#define STATIC_ASSERT(e, msg) struct __ ## msg { char* __static_assert_machinery[(e) ? 0 : -1]; };

/*
 * IEEE double-precision floats have *two* NaN spaces, because the sign bit is independent of the
 * NaN pattern.  This means that, throwing away a couple extra bits as the JSC design does, we have
 * two 48-bit ranges to stash non-double values in.  We're devoting one (0x0000:....:....:....) to
 * pointers.  We're devoting 2^32 of the remaining space to integers and booleans, since without
 * unions we actually have enough information in the types to distinguish the two.  This leaves us
 * with 2^48-2^32 64-bit values that we can use to encode other information, because, to borrow and
 * modify a figure from the JSC source (http://www.publicsource.apple.com/source/JavaScriptCore/JavaScriptCore-7600.1.17/runtime/JSCJSValue.h):
 *     Pointer      0000:----:----:----
 *                / 0001:----:----:----
 *     Double    {          ...
 *                \ FFFE:----:----:----
 *     Integer  --> FFFF:0000:----:----
 *     Boolean  --> FFFF:0001:----:----
 *                / FFFF:0002:----:----
 *     Others?   {          ...
 *                \ FFFF:FFFF:----:----
 *
 * We could potentially stick a lot of stuff in there eventually, as long as it's not pointers
 * (since not only do we need up to 48 bits to store them, but with the high-end bits set the GC
 * won't recognize them).  We could use this space for things like deleted field flags, but it will
 * probably require a bit more thought; we want to minimize the number of checks to disambiguate
 * three classes of values: box pointers, deleted fields, and local (present) values.
 */

// Values are all 64 bits
typedef union u {
    void *ptr;
    uint64_t box;
    unsigned long long box2;
    int32_t i;
    double dbl; // We're bit-shifting doubles by DOUBLE_SHIFT when stored in FIELDS, so this is only useful for boxes/arrays (and the shifting macros below)
    bool b;
    char c;
    wchar_t* str;
    union u *box_ptr;
    struct object *obj;
    struct { // little endian: LSB first
        uint64_t fraction : 52;
        uint64_t exponent : 11;
        uint64_t sign : 1;
    } dbl_fields;
    struct {
        uint32_t _;
        uint32_t tag;
    } tags;
#ifdef __cplusplus
    // Need explicit constructors for casts in C++
    u(wchar_t* s) : str(s) {
#ifdef SMALL_POINTER
        this->tags.tag = 0;
#endif
    }
    u(void* p) : ptr(p) {
#ifdef SMALL_POINTER
        this->tags.tag = 0;
#endif
        // TODO: no tagging to do....
    }
    u(int x) : i(x) { this->tags.tag = INT_TAG; }
    u(bool x) : b(x) {
        this->tags.tag = BOOL_TAG;
        // Note that in this case, there are 3 bytes that are uninitialized garbage!
    }
    u(uint64_t x) : box(x) {}
    u(double d) : dbl(d) {} // TODO: <-- HACK!  This is really unsafe, and we should remove it (or replace this with a shifting op
    u() : box(0xFFFF000000000001ULL) {}
    // TODO: Should I get rid of this?  There's no tagging, so it's pretty unsafe... 
    // TODO: I think this was only used for closures, in which case I could make it a variadic
    // template that instantiates a closure template pointer, and does the appropriate tagging.
    template<typename T>
    u(T* p) : ptr((void*)p) {
#ifdef SMALL_POINTER
        this->tags.tag = 0;
#endif
    }
#endif
} value_t;

STATIC_ASSERT(offsetof(value_t, tags.tag) == 4, tag_word_offset);
//STATIC_ASSERT(offsetof(value_t, tags._) == 0);

// TODO: If we're using the lowest 3 bits for pointer tagging, then other things that are sort of
// hidden in the pointer space need to shift up a couple bits (null, undef, true, false, deleted).
// Need to adjust masking process to distinguish these values from pointers so we don't e.g. confuse
// C's 'true' value (0x1) with a box pointer...

#define UNARY_POST_INC(v) (v.i++)
#define UNARY_PRE_INC(v) (++v.i)
#define UNARY_POST_DEC(v) (v.i--)
#define UNARY_PRE_DEC(v) (--v.i)
#define FLOAT_UNARY_POST_INC(v) (v.dbl++)
#define FLOAT_UNARY_PRE_INC(v) (++v.dbl)
#define FLOAT_UNARY_POST_DEC(v) (v.dbl--)
#define FLOAT_UNARY_PRE_DEC(v) (--v.dbl)

/*
 * We need to tag more than just what JITs use, so we can compact our object representation.
 * We need to distinguish general (object, closure, etc) pointers as values, and "box pointers" as a distinct
 * class of pointers.  Setting the 0 (low) bit on pointers to boxes seems reasonable.  This is still
 * distinguishable from other primitives (true, false, undef, etc.) which have only low-order bits
 * set, but also distinguishable from valid object pointers.
 */
//#define BOX_MASK 0xFFFF000000000001LL // bit-and of a pointer with this == 1 --> box pointer
#define PTR_TAG_MASK 0xFFFF000000000007LL // bit-and of a pointer with this == X_TAG --> is X
#define FIELD_READ(o, f_index) ((o->fields[o->vtbl[(f_index)]].box & PTR_TAG_MASK) == BOX_TAG ? \
                                 *((value_t*)(o->fields[o->vtbl[(f_index)]].box & MAX_PTR)) : \
                                 o->fields[o->vtbl[(f_index)]] )
#define FIELD_ACCESS(o, off) ((o->fields[off].box & PTR_TAG_MASK) == BOX_TAG ? \
                                 *((value_t*)(o->fields[off].box & MAX_PTR)) : \
                                 o->fields[off] )
#define INLINE_BOX_ACCESS(o, off) (o->fields[off])

// TODO: We should know physical offsets for at least rcv here
// This macro sets up field f of rcv to inherit from field f of proto, compressing prototype paths
// for constant access
#define INHERIT_FIELD_COMPRESSED(rcv, proto, f) \
    if ((proto->fields[proto->vtbl[f]].box & PTR_TAG_MASK) == BOX_TAG) { \
        rcv->fields[rcv->vtbl[f]].box = proto->fields[proto->vtbl[f]].box; \
    } else { \
        rcv->fields[rcv->vtbl[f]].box = ((uint64_t)&proto->fields[proto->vtbl[f]]) | BOX_TAG; \
    }

#define IS_CPP_WRAPPER(o) (o->vtbl[___js______cpp_receiver] != -1)
#define WRAPPED_CPP_OBJ(o) ((object_t*)o->fields[0].ptr) // Note that ^^cpp_receiver is always at offset 0!

#define IS_INHERITABLE_NATIVE(o) (o != NULL && IS_CPP_WRAPPER(o) && (o->vtbl[___js______gen_cpp_proxy] != -1))
#define GEN_CPP_PROXY(o) \
    assert(o->__proto__ != NULL); \
    assert(o->__proto__->vtbl[___js______gen_cpp_proxy] != -1); \
    o->fields[0].ptr = ((void* (*)(object_t*))o->__proto__->fields[1].ptr)(o); \
    o->fields[1] = o->__proto__->fields[1]

#define MAYBE_GEN_CPP_PROXY(o) \
    if (IS_INHERITABLE_NATIVE(o->__proto__)) { \
        GEN_CPP_PROXY(o); \
    }

/*
 * NOTE: Since writing to a field means it's locally present, we never need to check for indirection
 * on field write --- by construction, if the field is written and the program type-checks, the
 * field will always be local instead of inherited.
 * TODO: Revisit this once we start interop work
 */
#define FIELD_WRITE_boxed(o, f_index, proj, op, v) \
    ((o->fields[o->vtbl[(f_index)]].box & PTR_TAG_MASK) == BOX_TAG ? \
     ((((value_t*)(o->fields[o->vtbl[(f_index)]].box & MAX_PTR))->proj) op (v)) : \
     ((o->fields[o->vtbl[(f_index)]]).proj op (v) ))
#define FIELD_WRITE(o, f_index, proj, op, v) ((o->fields[o->vtbl[(f_index)]]).proj op (v) )

#define FIELD_READ_WRITABLE(o, f_index) (o->fields[o->vtbl[(f_index)]])

// TODO: Ultimately, we need to be able to distinguish at least between string, object, closure, and
// box pointers.  4 options --> 2 bits, which is fine since everything we allocate will be (at
// least) 4-byte aligned anyways.  But actually implementing this requires slightly adjusting the 
// low-order bit treatment above.  We won't need this until we introduce union types.

STATIC_ASSERT(sizeof(value_t) == sizeof(uint64_t), value_size_check);

// On Darwin these two types are defined to be equal, on Linux they're defined separately (but should be compatible)
STATIC_ASSERT(sizeof(unsigned long long) == sizeof(uint64_t), unsigned_int_size_check);

STATIC_ASSERT(PTR_MASK == (INT_TAG << 32), ptr_tag_consistency_check);

// Variables: a variable representation is a pointer to the value,
// to support lval sharing.  Fortunately C's syntax for lval and
// rval are identical, and the context disambiguates (like most
// languages).  The only case we need to distinguish is for environment
// capture, where we need to capture the box pointer, rather than the value
#define MKBOX(val) ((value_t*)boxprim((value_t)val))
#ifdef __cplusplus
extern "C" {
#endif
void* boxprim(value_t val);
#ifdef __cplusplus
}
#endif

#define UNBOX(x) (*(x))
#define WRITEBOX(bx, v) (*(bx) = v)
#define LVAL(x) (x)

/*
 * Type Tagging Design
 *
 * For interop mode, we must be able to tag and check the types of various things.  Every heap
 * allocation (eventually) will carry 
 */

// Constants for (future) status word (i.e., union tag).  We need just enough to distinguish how
// property accesses should behave, and whether the function component is active or not.
#define TYPE_CLOSURE 0x1 // function, method, constructor
#define TYPE_STRING  0x2 // strings
#define TYPE_OBJECT  0x3 // objects, maps
#define TYPE_ARRAY   0x4 // arrays
#define TYPE_CODE    0x5 // all code types, use CODE_* tags below
#define TYPE_VOID    0x6
#define TYPE_FLOAT   0x7 // really double in C's type system
#define TYPE_INT     0x8
#define TYPE_BOOL    0x9
#define TYPE_MAP     0xA
#define TYPE_TOP     0xB
#define CODE_FUNCTION  0x1
#define CODE_METHOD    0x2
#define CODE_CTOR      0x3
struct code_type;

// TEMPORARY until we emit proper tags for FFI objects
// For now, this marks native closures that actually return something (i.e., have C return type
// value_t) so they can be invoked correctly
#define NONVOID_BUILTIN_TAG_HACK ((void*)(uintptr_t)(1UL))

//!!! Need to refactor the object types slightly to add property names.  Could reconstruct from physical order + vtable on a typed object, but that's pretty expensive

typedef struct type_tag {
    uint8_t tag; // TYPE_*
    union {
        struct type_tag *array_elem; // single pointer, also used for maps
        struct code_type *code_sig;
        struct {
            // TODO: Making the field_tags an array of inline tags instead of an array of pointers
            // would save space and make use faster
            struct type_tag **field_tags; // array of field types
            /* An array of vtable indices for properties, which can be translated to strings by
             * looking up in ____propname_by_id.  One twist: read-only props are represented by
             * the bitwise negation of the index, and can be recognized by checking the high bit
             * (set means it's RO, and needs to be bitwise negated before use).  This implicitly
             * assumes there are fewer than 2^31 property names in the typed code, which seems
             * reasonable.
             */
            uint32_t *prop_indices;
            /* A model vtable, suitable for use in the case that the coerced object has exactly the
             * right fields.  This is technically redundant --- we'll sort the order of fields in
             * the structures above by physical layout order (with RO / possibly-RO fields last), so
             * processing these fields in order will lay out the objects in a manner consistent with 
             * assumptions used in the field access optimizer.  TODO: prove this handling of RO
             * fields doesn't break anything, since we don't really optimize RO field accesses so
             * much, or weaken optimization of RO field access :-/  Worst case is blacklisting
             * access to fields that show up RO in object types in the positive positions of a
             * coercion (shouldn't be terrible unless the export object is huge)
             */
            int const* vtbl_model; // pointer to base vtable, for at least 
        } object_sig;
    } body;
} type_tag_t;
typedef struct code_type {
    uint8_t codetype;
    uint8_t nargs;
    struct type_tag *ret;
    struct type_tag *proto; // for constructors only
    struct type_tag **args; // includes receiver in args[0] for method/ctor, where void receiver is function
} code_type_t;


// Closures
// Each closure generated may have a different environment layout, arity, etc.
// We assume the closure is some type akin to:
//     type closure(ret, ...) = struct { env __env; ret (func*)(...); }
// where env is just an array of value pointers (lvals)
// Remember that env_t is doubly-indirect: it is an array of lvals, so an array of pointers to
// values
typedef value_t** preenv_t;
typedef value_t* const * env_t;
// Bizarrely, for this macro to work, C requires the token before ## to be ,
#define CLOSURETY(ret, ...) struct { env_t env; ret (*func)(env_t, ##__VA_ARGS__ ); }
#define CLOSURETY2(CODETY) struct { env_t env; CODETY; }
#define CLOSURECODEPTR(ret, ...) ret (*)(env_t __env, ##__VA_ARGS__)
#define CLOSURECODE(id, ret, ...) ret id (env_t __env, ##__VA_ARGS__)
//#define PIVOT_INVOKE_CLOSURE(T, f, ...) (((T)((old_closure_t*)f)->func)(((old_closure_t*)f)->env, ##__VA_ARGS__))
#define PIVOT_INVOKE_CLOSURE(T, f, ...) (((T)(((closure_t*)f)->code.ptr))((env_t)(((closure_t*)f)->env.ptr), ##__VA_ARGS__))
#define ENV_ACCESS(i) (*__env[i])
#define ENV_COPY(i) (__env[i])

// Note that if we support JavaScript's bind operation, this will produce something
// that looks like a closure: a record with a this pointer (like an environment)
// and the method code pointer, where the invocation works just like a closure
// invocation.  Eventually it would be nice if these shared a C representation type,
// since they'll actually be interchangeable on the SJS side

#ifdef __cplusplus
extern "C" {
#endif
void* env_alloc_prim(int count, ...);
struct object* closure_alloc_prim(void* env, type_tag_t *tag, void* code);
struct object* ctor_alloc_prim(void* env, type_tag_t *tag, void* code, void* proto);
#ifdef __cplusplus
}
#endif

#define ALLOC_CLOSURE(fptr, tag, n, ...) (closure_alloc_prim(env_alloc_prim(n, ##__VA_ARGS__), tag, fptr))
#define ALLOC_CTOR_CLOSURE(fptr, tag, proto, n, ...) (ctor_alloc_prim(env_alloc_prim(n, ##__VA_ARGS__), tag, fptr, proto))

// Object representation
typedef int const* object_map; // int[]
typedef int* pre_object_map;

// Note there are no consts here --- dirty bits live in the vtbl pointer, and mutable fields are inline
struct object {
    object_map vtbl;
    struct object* __proto__;
    struct map* __propbag;
    type_tag_t* type;
#ifdef SMALL_POINTER
    // !!! Whenever the number of pointers above this line is odd, we need a 4-byte padding.
    // Whenever the number of pointers above this line is even, we're fine.  We're maintaining
    // 8-byte alignment of the fields array.  Whenever this padding is present, we also need to fix
    // the SMALL_POINTER entries in linkage.h to initialize the padding
    // on 32-bit, this is necessary to get nelems 8-byte aligned
    //void *_____padding;
#endif
#ifdef __cplusplus
    value_t fields[0];
#else
    value_t fields[];
#endif
};
typedef struct object object_t;

static inline object_t* blank_obj(int n) {
    object_t* o = (object_t*)MEM_ALLOC(sizeof(struct object)+n*sizeof(value_t));
    // The GC clears this memory.  If GC is disabled, clients are responsible for
    // initializing/clearing these fields
    return o;
}
static inline object_t* blank_obj_vtable(int n, object_map m) {
    object_t* o = blank_obj(n);
    o->vtbl = m;
    return o;
}

typedef struct old_closure {
    env_t env;
    void* func;
} old_closure_t;
STATIC_ASSERT(sizeof(old_closure_t) == 2*sizeof(void*), old_closure_packing_check);

// NEW closure representation, compatible with objects.  Used for functions, methods
typedef struct closure_rep {
    object_map vtbl;
    struct object* __proto__;
    struct map* __propbag;
    type_tag_t* type;
#ifdef SMALL_POINTER
    // on 32-bit, this is sometimes necessary to get nelems 8-byte aligned
    // see comment in object_t
    //void *_____padding;
#endif
    value_t env;
    value_t code;
} closure_t;
STATIC_ASSERT(offsetof(closure_t, vtbl) == offsetof(object_t, vtbl), closure_vtbl_offset);
STATIC_ASSERT(offsetof(closure_t, __proto__) == offsetof(object_t, __proto__), closure___proto___offset);
STATIC_ASSERT(offsetof(closure_t, __propbag) == offsetof(object_t, __propbag), closure___propbag_offset);
STATIC_ASSERT(offsetof(closure_t, env) == offsetof(object_t, fields[0]), closure_env_offset);
STATIC_ASSERT(offsetof(closure_t, code) == offsetof(object_t, fields[1]), closure_code_offset);

// NEW constructor representation, compatible with object/closure
typedef struct constructor {
    object_map vtbl;
    struct object* __proto__;
    struct map* __propbag;
    type_tag_t* type;
#ifdef SMALL_POINTER
    // on 32-bit, this is sometimes necessary to get nelems 8-byte aligned
    // see comment in object_t
    //void *_____padding;
#endif
    value_t env;
    value_t code;
    value_t prototype;
} constructor_t;
STATIC_ASSERT(offsetof(constructor_t, vtbl) == offsetof(object_t, vtbl), constructor_vtbl_offset);
STATIC_ASSERT(offsetof(constructor_t, __proto__) == offsetof(object_t, __proto__), constructor___proto___offset);
STATIC_ASSERT(offsetof(constructor_t, __propbag) == offsetof(object_t, __propbag), constructor___propbag_offset);
STATIC_ASSERT(offsetof(constructor_t, env) == offsetof(object_t, fields[0]), constructor_env_offset);
STATIC_ASSERT(offsetof(constructor_t, code) == offsetof(object_t, fields[1]), constructor_code_offset);
STATIC_ASSERT(offsetof(constructor_t, prototype) == offsetof(object_t, fields[2]), constructor_prototype_offset);

#ifdef __cplusplus
extern "C" {
#endif
object_t* alloc_object_lit(object_map vtbl, type_tag_t *tag, int nfields, ...);
object_t* construct_object(object_map vtbl, type_tag_t *tag, object_t* proto, int nslots);
#ifdef __cplusplus
}
#endif

// TODO: undefined...
/*
 * Current thoughts on undefined:
 * Represent undefined explicitly _within_each_type_, except for doubles, where we'll reuse the
 * pointer representation (0x1ULL).  This gives a definite test for undefined (low bit set, tags
 * don't show double).  It makes it easy(-ish) to propagate undefinedness through primops (e.g.,
 * treating 0x1ULL as an encoded double and pushing through the ALU generates a NaN as per ES spec).
 * Initializing memory is straightforward.  The only issue is value comparison; some type spaces
 * have multiple representations of undefined.  This can be handled by compiling all equality tests
 * to handle undefined, but this is a bit slow for such a common (and otherwise cheap) operation.
 *
 * The alternative is to use 0x1ULL as the *only* representation of undefined.  This makes equality
 * comparisons a bit more sane (technically there's still the NaN != NaN issue, which will require
 * some handling...).  Type tests become marginally more expensive, but barely.  Other binary
 * operations (e.g., integer, bool, double ops) need to explicitly check to propagate undefined, but
 * integer and boolean binops need to do this anyways in the other scheme due to tagging.  The real
 * cost is in making all type-correct binops 64-bits wide, vs. the smi and bool ops in the other
 * scheme, which are 32 and 16 bits wide respectively; a cost in memory bandwidth.
 *
 * It's not obvious which cost is worse in terms of performance.
 *
 * Thinking some more, the cost of propagating a tag bit and leaving other bits alone for spaces
 * with multiple undefined represenations (i.e., int and bool) isn't much different (or shouldn't
 * be) from canonicalizing the undefined representative on a per-type basis.  (Hybrid compilation of
 * equality still needs to handle the |types| different undefined reps, though the 0x1 test will
 * make that fairly cheap).
 *
 * Current leaning: one undefined val per type space.  (Hybrid writes will need to check for cases
 * of converting between undefined reps based on the target type.)
 *
 * Steps to enable:
 * 1) Switch to boxed-everywhere --- uniform local, parameter, and return passing
 * 2) Switch to SMIs
 * 3) Switch to alternate boolean representation
 * 4) Clean up memory initialization & field reads to handle undefined init and generate undefined
 *    coercions.
 * 5) Clean up array reads, which have a lot of redundancy in macro expansions
 * We MUST compare performance before and after each of these changes!  Step 1 is mostly unavoidable
 * with the new interop story, but if any of steps 2--4 hurts performance too much, it's worth
 * backing up and trying the single undefined representation.
 *
 */
static inline bool val_is_pointer(value_t v) { return !(v.box & PTR_MASK); }
static inline bool val_is_object(value_t v) { return val_is_pointer(v) && ((v.box & MAX_PTR) == v.box); }
static inline bool val_is_int(value_t v) { return v.tags.tag == INT_TAG; }
static inline bool val_is_double(value_t v) {
    return v.tags.tag > 0x0000FFFFUL && v.tags.tag < 0xFFFF0000UL;
    // Above should be equivalent of:
    // return (v.box & PTR_MASK) && (v.tags.tag < 0xFFFF0000UL);
    // but requires fewer bytes read / only one 32-bit load and no masks
}
// TODO: need to fix...
static inline bool val_is_string(value_t v) { return val_is_pointer(v) && ((v.box & STR_TAG) == STR_TAG); }
static inline bool val_is_closure(value_t v) { return val_is_pointer(v) && ((v.box & CLS_TAG) == CLS_TAG); }
static inline bool val_is_boolean(value_t v) {
    return v.tags.tag == BOOL_TAG;
}

// TODO: 0xFFFF000700000000 falls in the NaN space for both encoded and unencoded floats; the
// unencoded->encoded transition in interop mode should preserve this particular bit pattern
static inline bool val_is_undef(value_t v) {
    //return ((v.box & (0xFFFF000700000000 | UND_TAG)) == (0xFFFF000700000000 | UND_TAG)) && true; // TODO: if bit 0 is set, also need to check that we're not a valid double
    return v.box == 0xFFFF000700000000;
}
// TODO: This needs to be split according to whether or not we're in an encoding region
static inline bool val_is_falsy(value_t v) {
    // TODO: some strings are falsy (empty string?)
    // TODO: This needs some careful thought w.r.t. optimization
    return val_is_undef(v) || ((v.tags.tag >> 16 == 0) && ((v.box & MAX_PTR) == 0ULL)) || ((v.tags.tag == INT_TAG || v.tags.tag == BOOL_TAG) && v.i == 0);
}

static inline object_t* __get_prototype(object_t* o) { return o->__proto__; }
static inline void __set_prototype(object_t* o, object_t* p) { o->__proto__ = p; }

static inline value_t int_as_val(int i) { 
    value_t tmp; 
    tmp.tags.tag = INT_TAG; 
    tmp.i = i; 
    return tmp;
}
static inline value_t double_as_val(double d) { return (value_t)shift_double(d); }
static inline value_t double_as_val_noenc(double v) { return (value_t)v; };
static inline value_t boolean_as_val(bool b) { value_t tmp; tmp.tags.tag = BOOL_TAG; tmp.i = (b ? 0x10 : 0); return tmp; }
static inline value_t string_as_val(wchar_t* s) {
    value_t tmp;
#ifdef SMALL_POINTER
    tmp.tags.tag = 0;
#endif
    tmp.ptr = s;
    tmp.box |= STR_TAG;
    return tmp; }
static inline value_t object_as_val(object_t* o) {
    value_t tmp;
#ifdef SMALL_POINTER
    tmp.tags.tag = 0;
#endif
    tmp.ptr = o;
    return tmp;
}
static inline value_t closure_as_val(void* p) {
    value_t tmp;
#ifdef SMALL_POINTER
    tmp.tags.tag = 0;
#endif
    tmp.ptr = p;
    tmp.box |= CLS_TAG;
    return tmp;
}

static inline void* val_as_pointer(value_t v) { return (void*)(v.box & MAX_PTR); }
static inline object_t* val_as_object(value_t v) { return v.obj/*(object_t*)(v.box & MAX_PTR)*/; } // objects are untagged
static inline int32_t val_as_int(value_t v) { return v.i; }
static inline double val_as_double(value_t v) { return unshift_double(v); };
static inline double val_as_double_noenc(value_t v) { return v.dbl; };
static inline wchar_t* val_as_string(value_t v) { return val_is_undef(v) ? (wchar_t*)L"undefined" : (wchar_t*)(v.box & MAX_PTR); }
// TODO undefined checks such as the above needed elsewhere too?
// TODO: tag closure runtime rep with # args, types, etc.
static inline bool val_as_boolean(value_t v) { return (v.box & 0x10) != 0; }

static inline void store_int(value_t* v, int i) {
    v->tags.tag = INT_TAG;
    v->i = i;
}
static inline void store_bool(value_t* v, bool b) {
    v->tags.tag = BOOL_TAG;
    v->i = (b ? 0x10 : 0); // store to .i to clear other bits to avoid confusion
}
static inline void store_double(value_t* v, double d) { v->box = shift_double(d); }
static inline void store_pointer(value_t* v, void* p) {
#ifdef SMALL_POINTER
    v->tags.tag = 0;
#endif
    v->ptr = p;
}

static inline value_t find_field(object_t* o, int ioff) {
    // TODO: top of prototype chain... fail
    int phys = o->vtbl[ioff];
    if (phys >= 0) {
        return o->fields[phys];
    } else {
        return find_field(o->__proto__, ioff);
    }
}

value_t field_read(object_t*, wchar_t*);

void field_write(object_t*, wchar_t*, value_t);

static inline int ___int_of_float(double x) {
    if (x != x || x == (double)INFINITY || x == (double)(-INFINITY)) {
        // ES spec says ToInt32 returns 0 for these values
        return 0;
    } else {
        // When x doesn't fit in the range of 32-bit signed integers, C's casts don't do what JS
        // expects.  The following tries to follow the spec for ToInt32
        // http://www.ecma-international.org/ecma-262/6.0/index.html#sec-toint32
        // round towards 0...
        double rtz = trunc(x);
        // modulo 2^32, still as double
        double int32bit = fmod(rtz, (double)0x100000000ll);
        // now have a double in the unsigned 32 bit range...
        // last step of the spec:
        if (int32bit > (double)0x80000000ll) {
            return (int)(int32bit - (double)0x100000000ll);
        } else {
            return (int)int32bit;
        }
    }
}
static inline int ___castFloor(double x) {
    return ___int_of_float(floor(x));
}


// Arrays
#include<array.h>

// Math
#ifdef __cplusplus
extern "C" {
#endif
#include<jsmath.h>
#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
extern "C" {
#endif
// Strings
bool ___sjs_strcmp(void* x, void* y);
wchar_t* __str__substring(wchar_t* s, int start, int end);
double __str__charCodeAt(wchar_t* s, int32_t off);
wchar_t* __str__charAt(wchar_t*s, int32_t off);
wchar_t* __str__fromCharCode(int32_t code);
int32_t __str__indexOf(wchar_t* base, wchar_t* search);
int32_t __str__localeCompare(void* base, void* search);
#ifndef TIZEN
wchar_t* __readline();
#endif
#ifdef __cplusplus
}
#endif

// Interoperability
extern int __dirty;
void _____type_violation();
// Access to untyped imports
#include <typetags.h>
static inline value_t ACCESS_HEAP_UNTYPED(value_t *box, type_tag_t *tag) {
    value_t val = *box;
    // TODO: this will break on strings
    if (val_is_pointer(val) && val_as_object(val)->type != NULL) {
        if (val_as_object(val)->type == tag)
            return val;
        else if (__subtype_lt(val_as_object(val)->type, tag)) {
            return val;
        }
        _____type_violation();
    } else if (val_is_pointer(val)) {
        // heap object, not tagged (NULL tag)
        val = __coerce(val, tag);
        *box = val;
        return val;
    } else {
        assert(false);  //TODO: this will screw up codegen
    }
    return val;
}
static inline value_t ACCESS_INT(value_t *box) {
    value_t val = *box;
    if (!val_is_int(val)) {
        _____type_violation();
    }
    return val;
}

#endif // __SJS_RUNTIME
