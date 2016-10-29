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
#include <interop.h>

Value arg_stack[100];
int nargs = 0;

void OP_CLEARARGS() {
    nargs = 0;
}

Value OP_POPARG() {
    assert(nargs > 0);
    return arg_stack[--nargs];
}

void OP_PUSHARG(Value arg) {
    assert(nargs < 100);
    arg_stack[nargs++] = arg;
}
