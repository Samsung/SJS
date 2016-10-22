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
#include<iostream>
#include "runtime.h"
#include "linkage.h"

// Manually declare the indirection map requested in the linkage json file
extern "C" {
extern object_map extern_console_table;
}

// This isn't called by name from C, so it's fine to let it be name-mangled
void ext_cons_log(env_t env, object_t* ext_cons, char* str) {
    std::cout << str << std::endl;
}

extern "C" {
BOX_NATIVE_METHOD(ext_cons_log, ext_cons_log)

// Note that we're assigning the *address* of the table, not the table itself, because
// we have to lie a little bit about the extern; it's really an int[X] for some fixed X, and C gives
// different semantics to pointer names and fixed-size array names (really, it gives 2 semantics to
// the latter).  If you forget to take the address here, the linker will set the vtable to NULL!
cppobj<0,1> __extern_console = {
    .vtbl = reinterpret_cast<object_map>(&extern_console_table),
    .fields = { { .ptr = (void*)&ext_cons_log_clos } },
};

CPP_UNBOXED_GLOBAL_OBJECT(externalconsole, __extern_console)
}
