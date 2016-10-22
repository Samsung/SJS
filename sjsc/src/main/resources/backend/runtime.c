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
 * Implementations of runtime primitives
 */

#ifdef USE_GC
#include<gc.h>
#endif
#include<stdio.h>
#include<stdlib.h>
#include<runtime.h>
#include<string.h>
#include<linkage.h>

// global flag for global slow mode after a type violation
int __dirty = 0;
void _____type_violation() {
    assert (false);
    __dirty = 1; // No going back...
}

void print(wchar_t* s) {
    wprintf(L"%ls\n", s);
}

int parseInt(wchar_t* s, int radix) {
    return (int)wcstol(s, NULL, radix);
}
int parseInt_noradix(wchar_t* s) {
    return (int)wcstol(s, NULL, 10);
}
double parseFloat(wchar_t* s) {
    // TODO: errno, exceptions
    return (double)wcstod(s, NULL);
}

void printInt(int s) {
    wprintf(L"%d\n", s);
}
/* To match the formatting behavior of console.log(x) for a double x
 * on node.js, we need to use the g format specifier with a very high precision.
 * Essentially, g prints the double as the normal %f would, except it omits 
 * trailing zeros.  Empirically, node.js seems to round floats when printed to 17
 * decimal places.
 */
void printFloat(double s) {
    //printf("%f\n", s);
    printf("%.17g\n", s);
}
void printFloat10(double s) {
    //printf("%f\n", s);
    printf("%.10f\n", s);
}

// Since we control the runtime, we can ensure all strings are null-terminated...
wchar_t* __str__concat(wchar_t* a, wchar_t* b) {
    int alen = wcslen(a),
        blen = wcslen(b);
    wchar_t* res = (wchar_t*)MEM_ALLOC_ATOMIC((alen+blen+1)*sizeof(wchar_t));
    //wmemcpy(res, a, alen);
    wcsncpy(res, a, alen);
    wcsncpy(res+alen, b, blen+1); // copy null
    return res;
}

void* boxprim(value_t val) {
    // TODO: make allocation of boxes more precise, since we'll overallocate for e.g. bool
    value_t *mem = (value_t*)MEM_ALLOC(sizeof(value_t));
    *mem = val;
    return mem;
}

// TODO: This could really screw with performance, vs. specializing allocation sites by size
void* env_alloc_prim(int count, ...) {
    va_list ap;
    value_t** allocation = NULL;
    int i = 0;
    if (count == 0) return NULL;
    va_start(ap, count);
    allocation = (value_t**)MEM_ALLOC(count * sizeof(value_t*));
    // TODO: if allocation fails, invoke GC
    for (i = 0; i < count; i++) {
        allocation[i] = va_arg(ap, value_t*);
    }
    va_end(ap);
    return allocation;
}

// align to 8 bytes by padding allocation and shifting result
// We need this so closure allocations are 8-byte aligned
void* last_align_basis = NULL;
void* aligned_alloc(size_t n) {
    void* m = MEM_ALLOC(n+7);
    last_align_basis = m;
    assert(m != NULL);
    uint64_t mi = (uint64_t)m;
    mi = (mi + 0x7) & ~0x7ULL; // if this is already 8-byte aligned, this will add and mask off 7.  If it's not, it will bump past the next 8-byte boundary and round down
    return (void*)mi;
}
void* last_alloc_attempt = NULL;
object_t* closure_alloc_prim(void* env, type_tag_t *tag, void* code) {
    closure_t* cl = (closure_t*)blank_obj(2);
    cl->vtbl = closure_table;
    cl->env.ptr = env;
    cl->code.ptr = code; 
    cl->type = tag;
    return (object_t*)cl;
}
object_t* ctor_alloc_prim(void* env, type_tag_t *tag, void* code, void* proto) {
    constructor_t* cl = (constructor_t*)blank_obj(3);
    cl->env.ptr = env;
    cl->code.ptr = code; 
    cl->prototype.ptr = proto;
    cl->type = tag;
    return (object_t*)cl;
}

object_t* alloc_object_lit(object_map vtbl, type_tag_t *tag, int nfields, ...) {
    // TODO: inherited Object methods...
    va_list ap;
    int i = 0;
    // add extra field space to include boxes
    // Note that the value_t* is smaller than a value_t on 32 bit platforms
    object_t* o = (object_t*)blank_obj(nfields);
    o->vtbl = vtbl;
    o->type = tag;
    o->__proto__ = NULL;
    va_start(ap, nfields);
    for (i = 0; i < nfields; i++) {
        uint64_t v = va_arg(ap, uint64_t);
        o->fields[i] = (value_t)v;
    }
    va_end(ap);
    return o;
}
// construct_object just allocates memory and sets the vtable
object_t* construct_object(object_map vtbl, type_tag_t *tag, object_t* proto, int nslots) {
    int i = 0;
    // alloc an extra slot for FFI objects
    object_t* o = (object_t*)blank_obj(nslots);
    o->vtbl = vtbl;
    o->__proto__ = proto;
    o->type = tag;
    // TODO: set undef or deleted?
    for (i = 0; i < nslots; i++) {
        o->fields[i].box = ((uint64_t)INT_TAG << 32); // This is 0 for ints and bools, and will be overwritten for 64-bit types
    }
    return o;
}

wchar_t* __str__substring(wchar_t* s, int start, int end) {
    assert(start <= end); // TODO - exceptions!

    int newlen = end - start + 1; // + null terminator
    wchar_t* r = (wchar_t*)MEM_ALLOC_ATOMIC(newlen*sizeof(wchar_t));
    wcsncpy(r, s+start, end-start);
    r[end-start] = L'\0';
    return r;
}

wchar_t* __str__fromCharCode(int32_t code) {
    wchar_t* r = (wchar_t*)MEM_ALLOC_ATOMIC(2*sizeof(wchar_t));
    r[0] = (wchar_t)code;
    r[1] = L'\0';
    return r;
}
wchar_t* string_fromCharCode_method(env_t e, value_t o, value_t code) {
    return __str__fromCharCode(code.i);
}
UNBOX_NATIVE_METHOD(string_fromCharCode, string_fromCharCode_method);

#include <math.h>
double __str__charCodeAt(wchar_t* s, int32_t off) {
    int len = wcslen(s); // Runtime ensures null terminator
    if (off >= 0 && off < len) {
        return (double)s[off];
    } else {
        return (double)NAN;
    }
}

wchar_t* __str__charAt(wchar_t*s, int32_t off) {
    if (off < 0 || off >= wcslen(s)) {
        return L""; // Looks like a cop-out, but is actually the spec for charAt
    }
    wchar_t* ret = (wchar_t*)MEM_ALLOC_ATOMIC(2*sizeof(wchar_t));
    ret[0] = s[off];
    ret[1] = L'\0';
    return ret;
}

int32_t __str__indexOf(wchar_t* base, wchar_t* search) {
    wchar_t* res = wcsstr(base, search);
    if (res == NULL) {
        return -1;
    } else {
        size_t res2 = (size_t)res;
        size_t base2 = (size_t)base;
        return (res-base)/sizeof(char);
    }
}

bool ___sjs_strcmp(void* x, void* y) {
    // Take void* args because we might sometimes take one string and one null pointer of a
    // different type
    // Remember we're extending the behavior of wcscmp, which returns 0 if the strings match
    if (x == y) {
        return false;
    } else if (x == NULL || y == NULL) {
        return true;
    } else {
        return (bool)wcscmp((wchar_t*)x, (wchar_t*)y);
    }
}

int __str__localeCompare(void* x, void* y) {
    return wcscoll((wchar_t*)x, (wchar_t*)y);
}

extern int string_table[];

object_t __String_obj = {
    .vtbl = string_table,
    .__proto__ = NULL,
    .fields = { (value_t)(void*)&string_fromCharCode_clos }
};
object_t* string_box = &__String_obj;
object_t** String = &string_box;


#ifndef TIZEN
#if defined(__linux__)
#include <bsd/stdio.h>
#include <bsd/wchar.h>
#endif
wchar_t* __readline() {
    size_t len = 0;
    // fgetln uses a shared buffer, returned as tmp.  We must copy out for the user.
    // NOTE: the return is NOT null-terminated!
    wchar_t* tmp = fgetwln(stdin, &len);
    if (len <= 0 || (len == 1 && tmp[0]=='\n')) {
        return NULL;
    }
    wchar_t* out = (wchar_t*)MEM_ALLOC_ATOMIC((wcslen(tmp)+1)*sizeof(wchar_t));
    return wcsncpy(out, tmp, len);
}
#endif

void __overflow_trap() {
    fprintf(stderr, "Math over/underflow trap.\n");
}
