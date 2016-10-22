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
#include <runtime.h>

/*
 * coerce coerces a value to a typed representation when passing an untyped value to typed code.
 * It will return the original value or a closure trampoline if the coercion is successful, and otherwise trip the dirty
 * flag and return the original value.
 */
value_t __coerce(value_t val, type_tag_t* type);

bool __subtype_lt(type_tag_t* sub, type_tag_t* sup);
bool __typetag_eq(type_tag_t *a, type_tag_t *b);
