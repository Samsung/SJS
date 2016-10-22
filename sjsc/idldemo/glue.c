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
#include <linkage.h>
#include <idldemo/pair.h>


// extern declaration of indirection map for pair wrapper objects
extern object_map __vtbl_pair;

// code and closure allocations for pair accessor methods
value_t _pair_get_x(env_t env, value_t self) {
    struct pair* p = (struct pair*)self.obj->fields[0].ptr;
    return int_as_val(p->x);
}
value_t _pair_get_y(env_t env, value_t self) {
    struct pair* p = (struct pair*)self.obj->fields[0].ptr;
    return int_as_val(p->y);
}
void _pair_set_x(env_t env, value_t self, value_t newx) {
    struct pair* p = (struct pair*)self.obj->fields[0].ptr;
    p->x = val_as_int(newx);
}
void _pair_set_y(env_t env, value_t self, value_t newy) {
    struct pair* p = (struct pair*)self.obj->fields[0].ptr;
    p->y = val_as_int(newy);
}
// NOTE: The layout for closures will change in the near future!
__attribute__((__aligned__(8))) struct { env_t env; void *func; } _pair_get_x_clos = { NULL, (void*)_pair_get_x };
__attribute__((__aligned__(8))) struct { env_t env; void *func; } _pair_get_y_clos = { NULL, (void*)_pair_get_y };
__attribute__((__aligned__(8))) struct { env_t env; void *func; } _pair_set_x_clos = { NULL, (void*)_pair_set_x };
__attribute__((__aligned__(8))) struct { env_t env; void *func; } _pair_set_y_clos = { NULL, (void*)_pair_set_y };

// helper function to wrap C struct pair* as an SJS object w/ setters/getters
object_t* __wrap_pair(struct pair* p) {
    object_t* o = malloc(sizeof(object_t)+5*sizeof(value_t));
    o->vtbl = (object_map)&__vtbl_pair;
    o->__proto__ = NULL;
    // Note that the order of these fields matches the order of the accessors listed in the
    // linkage.json file
    o->fields[0].ptr = p;
    o->fields[1].ptr = (void*)&_pair_get_x_clos;
    o->fields[2].ptr = (void*)&_pair_set_x_clos;
    o->fields[3].ptr = (void*)&_pair_get_y_clos;
    o->fields[4].ptr = (void*)&_pair_set_y_clos;
    return o;
}

// Wrapper for alloc_pair
value_t alloc_pair_code(env_t env, value_t self) {
    struct pair* ret = alloc_pair();
    object_t* o = __wrap_pair(ret);
    return object_as_val(o);
}
__attribute__((__aligned__(8))) struct { env_t env; void *func; } __alloc_pair_clos = { NULL, (void*)alloc_pair_code };
BOXED_GLOBAL_OBJECT(js_alloc_pair, __alloc_pair_clos);

// Wrapper for print_pair
void print_pair_code(env_t env, value_t self, value_t o) {
    struct pair* p = (struct pair*)o.obj->fields[0].ptr;
    print_pair(p);
}
__attribute__((__aligned__(8))) struct { env_t env; void *func; } __print_pair_clos = { NULL, (void*)print_pair_code };
BOXED_GLOBAL_OBJECT(js_print_pair, __print_pair_clos);

// This helper function marshalls data between the C and JS representations
void pair_callback_helper(void* data, struct pair* p) {
    // Note the key things here are:
    // 1) Cast data to the appropriate closure type.  This type can be synthesized from the IDL.
    // Note that the details of the invocation (in particular, the cast vs. function pointer type)
    // details will change soon.
    // 2) Marshall arguments (and returns)
    // 3) Invoke the closure
    INVOKE_CLOSURE((struct { env_t env; void (*func)(env_t, value_t, value_t); }*)data, object_as_val(NULL), object_as_val(__wrap_pair(p)) );
}

void register_cb_code(env_t env, value_t self, value_t clos) {
    // Right now, val_as_pointer is important to clear tagging bits
    register_cb(val_as_pointer(clos), pair_callback_helper);
}
__attribute__((__aligned__(8))) struct { env_t env; void *func; } __register_cb_clos = { NULL, (void*)register_cb_code };
BOXED_GLOBAL_OBJECT(js_register_cb, __register_cb_clos);

