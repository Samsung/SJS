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
#include <interop.h>

//value_t danger_box = { .ptr = NULL };
//value_t danger_box = { .box = 0xFFFF000000000003ULL }; // integer 3
value_t danger_box = __UNDEF__;
value_t *danger = &danger_box;

// This lets us expose un-interposed writes at some type to the typed code, before importing at a
// possibly-different type as 'danger'.  This way we can do *some* interop testing in a
// self-contained way (still need to hook up to jscomp for untyped object/closure coercions)
extern value_t arg_stack[100];
value_t garbage_code() {
    return arg_stack[0];
}
BOX_UNTYPED_NATIVE_METHOD(garbage, garbage_code);
value_t *garbage = &garbage_box;
