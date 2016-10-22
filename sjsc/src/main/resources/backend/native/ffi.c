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
#ifdef USE_GC
#include<gc.h>
#endif
#include<runtime.h>
#include<ffi.h>

#include <stdio.h>
#include <assert.h>

#include <linkage.h>

extern int console_table[];
extern int object_proto_table[];

void console_log(env_t env, value_t objthis, value_t str) {
    // A bit inefficient, but avoids format string injection...
    if (str.box == 0xFFFF000700000000ULL) {
        str.str = L"undefined";
    }
    wprintf(L"%ls\n", val_as_string(str));
}
UNBOX_NATIVE_METHOD(console_log, console_log);

void console_assert(env_t env, value_t objthis, value_t b); // declare only, defined at EOF
UNBOX_NATIVE_METHOD(console_assert, console_assert);

void console_error(env_t env, value_t objthis, value_t str) {
    fwprintf(stderr, L"%ls\n", val_as_string(str));
}
UNBOX_NATIVE_METHOD(console_error, console_error);
UNBOX_NATIVE_METHOD(console_warn, console_error);

//object_t __ffi_console = {
//    .vtbl = &console_table,
//    .fields = { &console_log_box,
//                &console_assert_box,
//                &console_error_box,
//                &console_warn_box }
//};
object_t __ffi_console = {
    .vtbl = console_table,
    .__proto__ = NULL,
    .fields = { (value_t)(void*)&console_log_clos,
                (value_t)(void*)&console_assert_clos,
                (value_t)(void*)&console_error_clos,
                (value_t)(void*)&console_warn_clos }
};

// TODO: The JS spec says null.toString() returns "[object Null]" so we'll eventually need inline
// null checks at all method call sites unless they're dominated by a dereference...
value_t object_toString(env_t env, value_t o) {
    return string_as_val(L"[object Object]");
}
UNBOX_NATIVE_METHOD(object_toString, object_toString);

//object_t __ffi_object_proto = {
//    .vtbl = &object_proto_table,
//    .fields = { &object_toString_box }
//};
object_t __ffi_object_proto = {
    .vtbl = object_proto_table,
    .__proto__ = NULL,
    .fields = { (value_t)(void*)&object_toString_clos }
};


#undef NDEBUG
// console.assert should *always* execute according to standard JS runtimes, so
// we must ensure that no compilation setting disables this.  As a consequence,
// this code should not be hoisted above the #undef, and little (if anything)
// else should go below
// TODO: This is variadic in JS, taking an optional string
void console_assert(env_t env, value_t objthis, value_t b) {
    assert(val_as_boolean(b));
}
