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
/**
 * SJS array implementation.
 *
 * JavaScript arrays, with the exception of the splice method, are essentially just dequeues.
 *
 * @author colin.gordon
 */
#ifdef USE_GC
#include <gc.h>
#endif
#include <string.h>
#include <runtime.h>
#include <array.h>
#include <assert.h>
#include <stdlib.h>

/**
 * Array implementation.  With the exception of a couple methods that do some serious surgery,
 * JS arrays are essentially double-ended queues.
 *
 * Here we exploit the fact that the backing store for each array is owned wholly by the individual
 * array, with no references escaping, so when we resize we can actually reclaim the old array
 * contents.
 */

extern int array_table[];

// TODO: OOM hardening

__attribute__((__aligned__(8))) struct { env_t env; value_t (*func)(env_t, value_t); } array_pop_clos = { NULL, array_pop };
__attribute__((__aligned__(8))) struct { env_t env; value_t (*func)(env_t, value_t, value_t); } array_push_clos = { NULL, array_push };
__attribute__((__aligned__(8))) struct { env_t env; value_t (*func)(env_t, value_t); } array_shift_clos = { NULL, array_shift };
__attribute__((__aligned__(8))) struct { env_t env; void (*func)(env_t, value_t, value_t); } array_unshift_clos = { NULL, array_unshift };

object_t* mk_array_obj(unsigned int store_size, unsigned long long nelems, value_t* backing) {
    // We allocate enough slots for the fields themselves + an extra one for the actual array
    // pointer.
    array_obj_t *obj = MEM_ALLOC(sizeof(array_obj_t));
    obj->vtbl = array_table;
    obj->__proto__ = NULL;
    assert (&array_table != NULL);
    // Note that technically, this permits the JS to write to the length field
    // TODO: Figure out where that write will be disallowed: type system, or runtime?
    obj->pop_op.ptr     = (void*)&array_pop_clos;
    obj->push_op.ptr    = (void*)&array_push_clos;
    obj->shift_op.ptr   = (void*)&array_shift_clos;
    obj->unshift_op.ptr = (void*)&array_unshift_clos;
    obj->store_size = store_size;
    obj->start = backing;
    obj->nelems = nelems;
    obj->nelems_tag = INT_TAG;
    obj->backing = backing;
    return (object_t*)obj;
}

object_t* inner_Array(int length) {
    if (length == 0) { length = 16; }
    value_t* backing = (value_t*)MEM_ALLOC(sizeof(value_t)*(length));
    int i = 0;
    for (i = 0; i < length; i++) {
        backing[i].tags.tag = INT_TAG; // tag high order bits to be int/bool zero
    }
    // Never allocate size 0 array, as it will never really grow
    return mk_array_obj(length, length, backing);
}
value_t clos_Array(env_t env, value_t self, value_t length) {
    return object_as_val(inner_Array(val_as_int(length)));
}
array_clos_t Array_clos = { NULL, clos_Array };
value_t Array_box = { .ptr = (void*)&Array_clos };
value_t* Array = &Array_box;

object_t* array___lit(int length, ...) {
    va_list ap;
    int i = 0;
    value_t* backing = (value_t*)MEM_ALLOC(sizeof(value_t)*(length+1));
    // TODO: Should we pre-allocate more than the literal?
    va_start(ap, length);
    for (i = 0; i < length; i++) {
        /* We're trying to pull value_ts in, which works fine on x64; the machine word is the 
         * same size as a value_t.  But on x86/emscripten, some values are larger than the machine
         * word size.  In particular, array___lit(3, 1, 2, 3) pushes 12 bytes of data on the stack
         * instead of 24.  So we rely on the CALLER to cast each argument passed to a value_t, since
         * the caller controls the stack layout.
         */
        backing[i] = (value_t)va_arg(ap, uint64_t);
    }
    backing[length].tags.tag = INT_TAG;
    va_end(ap);

    return mk_array_obj(length+1, length, backing);
}

void inner_grow(array_obj_t* arr, int factor) {
    int x = 0;
    int next_size = arr->store_size + factor;
    value_t* newstore = (value_t*)MEM_ALLOC(sizeof(value_t) * next_size);
    // TODO: for new rep, need to take offset for skipping during copy...
    memcpy(newstore, &(arr->backing[0]), sizeof(value_t)*arr->nelems);
    MEM_FREE(arr->start);
    int oldsize = arr->store_size;
    arr->backing = newstore;
    arr->start = newstore;
    arr->store_size = next_size;
    for (x = oldsize; x < next_size; x++) {
        arr->backing[x] = (value_t)0x0ULL;
    }
}

void grow(array_obj_t* arr) {
    inner_grow(arr, arr->store_size);
}

value_t slow_array_put(object_t* objthis, int elem, value_t v) {
    int i;
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis);
    // TODO: Exceptions
    if (elem < 0 || elem >= athis->nelems) goto slow_put;
    athis->backing[elem] = v;
    return v;
slow_put:
    if (elem < 0) goto fail_put;
    // We're extending the array; grow it (reorg)
    if (elem >= athis->store_size) { 
        inner_grow(athis, elem);
    }
    int x = athis->nelems;
    // TODO: This is technically mimicking Java semantics, not JS semantics w/ undefined
    memset(&athis->backing[x], 0, sizeof(value_t)*(elem-x));
    athis->backing[elem] = v;
    athis->nelems = elem+1;
    return v;
fail_put:
    FAIL;
    return (value_t)0;
}

__attribute__ ((pure))
int array_length(object_t* objthis) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis);
    return athis->nelems;
}

int array_every(object_t* objthis, CLOSURETY2(int (*func)(env_t env, value_t v)) *f) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis);
    int i;
    for (i = 0; i < athis->nelems; i++) {
        if (! (INVOKE_CLOSURE(f, array_get(objthis, i)))) {
            return 0;
        }
    }
    return 1;
}

void array_forEach(object_t* objthis, CLOSURETY2(void (*func)(env_t, value_t)) *f) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis);
    int i;
    for (i = 0; i < athis->nelems; i++) {
        INVOKE_CLOSURE(f, array_get(objthis, i));
    }
}

array_obj_t* array_map(object_t* objthis, CLOSURETY2(value_t (*func)(env_t, value_t, int, object_t*)) *f) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis);
    int i;
    object_t* a2obj = inner_Array(athis->nelems);
    array_obj_t* a2 = ACCESS_ARRAY_REP(a2obj);
    for (i = 0; i < athis->nelems; i++) {
        array_put(a2obj, i, (INVOKE_CLOSURE(f, array_get(objthis, i), i, objthis)));
    }
    return a2;
}

// Pop *last* element
value_t array_pop(env_t env, value_t objthis) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis.obj);
    assert(athis->nelems > 0); // TODO: undefined / exceptions
    value_t res = array_get(objthis.obj, athis->nelems - 1);
    athis->nelems--;
    return res;
}

// Push onto the *end*
value_t array_push(env_t env, value_t objthis, value_t v) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis.obj);
    if (athis->nelems == athis->store_size) {
        grow(athis);
    }
    //athis->backing[athis->nelems] = v;
    int x = athis->nelems;
    // Note: this macro modifies athis->nelems prior to reading the index, so this offset *must* be
    // a separately bound variable...
    array_put(objthis.obj, x, v);
    //athis->nelems++; // <-- done in prev line
    assert(athis->nelems == x+1);
    return int_as_val(athis->nelems);
}

// Pop the *first* element
value_t array_shift(env_t env, value_t objthis) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis.obj);
    assert(athis->nelems > 0); // TODO: undefined / exceptions
    athis->nelems--;
    value_t ret = athis->backing[0];
    athis->backing++; // pointer increment
    return ret;
}

// Push onto the *front*
void array_unshift(env_t env, value_t objthis, value_t v) {
    array_obj_t* athis = ACCESS_ARRAY_REP(objthis.obj);
    if (athis->nelems == athis->store_size) grow(athis);
    //if (athis->start > 0) {
    //    // We just checked for growth, so we will not hit the end of the array
    //    athis->start--;
    //} else {
    //    athis->start = athis->store_size-1;
    //}
    //athis->backing[athis->start] = v;
    //athis->nelems++;
    assert(false); // TODO: Fix for new shift policy
}

object_t* array_reverse(env_t env, object_t* array_as_obj) {
    array_obj_t* arr = ACCESS_ARRAY_REP(array_as_obj);
    //// TODO: This is pretty awful; remove this hack eventually.
    //if (arr->start+arr->nelems >= arr->store_size) {
    //    // Array contents are discontiguous...
    //    grow(arr);
    //}
    // elements are contiguous
    int i = 0;
    for (i = 0; i < arr->nelems/2; i++) {
        int other = arr->nelems - i;
        value_t tmp = arr->backing[i];
        arr->backing[i] = arr->backing[other];
        arr->backing[other] = tmp;
    }
    return array_as_obj;
}
