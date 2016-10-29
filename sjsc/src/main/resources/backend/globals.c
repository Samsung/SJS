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
 * Global variables expected by JS
 */

#include<runtime.h>
#include<ffi.h>
#include<math.h>

#include<linkage.h>

//object_t* console_box = &__ffi_console;
//// boxed
//object_t** console = &console_box;
BOXED_GLOBAL_OBJECT(console, __ffi_console);

object_t* object_proto_box = &__ffi_object_proto;
object_t** __object_proto = &object_proto_box;

// TODO: Inline all accesses to global constant Infinity (NaN is already a C macro)
const value_t Infinity_box = { .dbl = INFINITY }; // TODO: shift_double((double)INFINITY) is an invalid initializer; hardcode bit pattern
const value_t * Infinity = &Infinity_box;
