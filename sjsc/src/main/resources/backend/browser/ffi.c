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
#include<runtime.h>
#include<ffi.h>
#include<gc.h>


//extern object_map* console_table;

#ifdef FALSE
extern object_map* document_table;

extern object_map* htmlelement_table;

object_t* alloc_wrapper(int slots) {
    return (object_t*)MEM_ALLOC(sizeof(object_map*)+slots*sizeof(value_t*));
}

extern void* __document_getElementById(char*);

// TODO: Note we may create n wrappers for n lookups of the same node.
// Each will give consistent views, though, since they each just bounce through the FFI
object_t* document_getElementById(object_t* this, char* id) {
    void* handle = __document_getElementById(id);
    // Allocate wrapper with extra unindexed slot for the
    // hidden handle rep
    object_t* wrapper = alloc_wrapper(3); // TODO: shouldn't hardcode # of members...
    wrapper->vtbl = htmlelement_table;
    // TODO: refactor this so we're not hardcoding object layouts in random places
    wrapper->fields[0] = handle; // TODO: storing an lval where most code expects rval...
    wrapper->fields[1] = innerHTML___get;
    wrapper->fields[2] = innerHTML___set;
    return wrapper;
}

object_t __ffi_document = {
    .vtbl = document_table,
    .fields = { document_getHTMLElementById }
};

object_t __ffi_htmlelement = {
    .vtbl = htmlelement_table,
    .fields = { htmlelement_innerHTML___set, 
                htmlelement_innerHTML___get }
};

#endif

extern void __console_log(char*);
extern void __console_error(char*);
extern void __console_assert(bool b);

void console_log(env_t env, object_t* this, char* str) {
    __console_log(str);
}
void console_error(env_t env, object_t* this, char* str) {
    __console_error(str);
}
void console_assert(env_t env, object_t* this, bool b) {
    __console_assert(b);
}
// TODO: everything from here down could be shared between native and web
// TODO: The native backend declares these boxes as value_t's, but if you cast a function pointer to
// a value_t, emcc complains that it's not a compile-time constant, while clang happily accepts
// it...
// TODO: asm.js is 32-bit, value_t's are 64-bit.  Bad things will happen if these are overwritten
// --- allocate some extra space
struct { env_t env; void (*func)(env_t, object_t*, char*); } console_log_clos = { NULL, console_log };
struct { env_t env; void (*func)(env_t, object_t*, bool); } console_assert_clos = { NULL, console_assert };
struct { env_t env; void (*func)(env_t, object_t*, char*); } console_error_clos = { NULL, console_error };
struct { env_t env; void (*func)(env_t, object_t*, char*); } console_warn_clos = { NULL, console_error };

struct { env_t env; void (*func)(env_t, object_t*, char*); } *console_log_box = &console_log_clos;
struct { env_t env; void (*func)(env_t, object_t*, bool); } *console_assert_box = &console_assert_clos;
struct { env_t env; void (*func)(env_t, object_t*, char*); } *console_error_box = &console_error_clos;
struct { env_t env; void (*func)(env_t, object_t*, char*); } *console_warn_box = &console_error_clos;

extern object_map console_table;
object_t __ffi_console = {
    .vtbl = &console_table,
    .fields = { &console_log_box,
                &console_assert_box,
                &console_error_box,
                &console_warn_box }
};
