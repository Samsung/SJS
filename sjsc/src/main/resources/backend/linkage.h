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
/* This file contains macros for generating linkage between C/C++ and SJS code */
#include "runtime.h"
#include <stddef.h>

#ifdef SMALL_POINTER
#define pointer_as_val(p) ((value_t)(0ULL | (uint32_t)p))
#else
#define pointer_as_val(p) ((value_t)p)
#endif

#ifdef __cplusplus
extern "C" {
#endif
extern int closure_table[];
extern int untypedclosure_table[];
extern int constructor_table[];
#ifdef __cplusplus
}
#endif

#define BOXED_GLOBAL_OBJECT(name, objimpl) \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = &objimpl }; \
__attribute__((__aligned__(8))) value_t* name = &name##_box;

// This macro doesn't do much, but makes it easy to toggle boxed vs. unboxed for a global
#define UNBOXED_GLOBAL_OBJECT(name, objimpl) \
__attribute__((__aligned__(8))) object_t* name = &objimpl;
#ifdef __cplusplus
#define CPP_UNBOXED_GLOBAL_OBJECT(name, objimpl) \
object_t* name = reinterpret_cast<object_t*>(&objimpl);
#endif

/* In general, we require the field ordering to be that all inherited fields
 * precede all local fields, and the box contents are in the same order as
 * the writable fields.  In addition, here we allocate extra boxes for the
 * read-only trampolines, which must go *after* the writable trampolines. 
 * It's on the IDL / C++ author to do this correctly. */
#define DECLARE_NATIVE_OBJECT_WRAPPER(name, tyname, numRO, numWR) \
typedef struct name { \
    object_map vtbl; \
    object_t* __proto__; \
    value_t* fields[numRO + numWR]; \
    value_t wrboxes[numWR]; \
    value_t roboxes[numRO]; \
    tyname *native_ptr; \
};

// We use static initializers a lot for objects, but C++ doesn't permit initialization of flexible
// members.  So we need to export a layout-compatible template for the general case.
#ifdef __cplusplus
template<int numRO, int numWR>
struct cppobj {
    object_map vtbl;
    object_t* __proto__;
    value_t fields[numRO + numWR];
};
#endif


/* Statically-allocate a closure and box (for the closure) for a method implemented natively.
 *
 * We're relying on the fact that a code pointer and void pointer will be packed
 * the same way, and abusing void* for polymorphism */
#define BOX_NATIVE_METHOD(name, mcode) \
__attribute__((__aligned__(8))) struct { env_t env; void *func; } name##_clos = { NULL, (void*)mcode }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
#define BOX_NATIVE_CTOR(name, mcode) \
__attribute__((__aligned__(8))) struct { env_t env; void *func; object_t* proto; } name##_clos = { NULL, (void*)mcode, NULL }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
#define NEW_BOX_NATIVE_METHOD(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&closure_table), NULL, NULL, NULL, pointer_as_val(NULL), pointer_as_val((void*)mcode) }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
#define BOX_UNTYPED_NATIVE_METHOD(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&untypedclosure_table), NULL, NULL, NULL, pointer_as_val(NULL), pointer_as_val((void*)mcode) }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
// For interop, we need both void and non-void returning variants, temporarily (until we properly
// generate tags for FFI objects).
#ifdef SMALL_POINTER
#define UNBOX_NATIVE_METHOD(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&closure_table), NULL, NULL, NULL, { .ptr = NULL }, { .ptr = (void*)mcode } };
#define UNBOX_NATIVE_METHOD_NONVOID(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&closure_table), NULL, NULL, NONVOID_BUILTIN_TAG_HACK, { .ptr = NULL }, { .ptr = (void*)mcode } };
#else
#define UNBOX_NATIVE_METHOD(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&closure_table), NULL, NULL, NULL, { .ptr = NULL }, { .ptr = (void*)mcode } };
#define UNBOX_NATIVE_METHOD_NONVOID(name, mcode) \
__attribute__((__aligned__(8))) closure_t name##_clos = \
    { (object_map)(&closure_table), NULL, NULL, NONVOID_BUILTIN_TAG_HACK, { .ptr = NULL }, { .ptr = (void*)mcode } };
#endif

#ifdef SMALL_POINTER
#define NEW_BOX_NATIVE_CTOR(name, mcode) \
__attribute__((__aligned__(8))) constructor_t name##_clos = \
    { (object_map)(&constructor_table), NULL, NULL, NULL, { .ptr = (NULL)}, { .ptr = ((void*)mcode)}, { .ptr = (NULL) } }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
#else
#define NEW_BOX_NATIVE_CTOR(name, mcode) \
__attribute__((__aligned__(8))) constructor_t name##_clos = \
    { (object_map)(&constructor_table), NULL, NULL, NULL, { .ptr = (NULL)}, { .ptr = ((void*)mcode)}, { .ptr = (NULL) } }; \
__attribute__((__aligned__(8))) value_t name##_box = { .ptr = (void*)&name##_clos };
#endif
    //{ (object_map)(&constructor_table), NULL, pointer_as_val(NULL), pointer_as_val((void*)mcode), pointer_as_val(NULL) }; \
#define BOX2GLOBAL(name, box) \
__attribute__((__aligned__(8))) value_t name = { .ptr = (void*)&box };

// Awful, but C++11 states that __cplusplus should be 201103L and larger in future revisions... so
// this is how to test for C++11
//#if __cpp_variadic_templates >= 200704 || __cplusplus > 201100L
#if defined(__cplusplus)
/* Convenient template for writing (nested) closure types from C++.  Rough analog of CLOSURETY()
 * macro in runtime.h, but (a) requires C++11 and (b) works properly when nested (unlike variadic
 * macros). */
#include <stddef.h>
template<typename R, typename... Args>
struct closure {
    public:
        env_t env;
        R (*func)(env_t, Args...);
        // Less hacky invocation
        constexpr R invoke(Args... args) const {
            return this->func(this->env, args...);
        }
};

template<typename R, typename... Args> using closurep = closure<R, Args...>*;

// Assert that this layout is compatible with what we generate in the SJS compiler
//typedef CLOSURETY(int,int) __test_ty; // Can't use this macro in an offsetof
//static_assert(offsetof(closure<int,int>, env) == offsetof(__test_ty, env));
//static_assert(offsetof(closure<int,int>, func) == offsetof(__test_ty, func));
#endif
