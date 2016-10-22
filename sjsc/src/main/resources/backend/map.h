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
#ifndef __SJS_MAP
#define __SJS_MAP

#ifdef __SJS__
#include <runtime.h>
#endif

struct map;
typedef struct object map_t;
struct map_iterator;

typedef struct map_iterator map_iterator_t;

map_t* __alloc_map_literal(type_tag_t*, int32_t nprops, ...);

map_t* __alloc_map(int32_t init_props);

bool __map_contains(map_t* om, wchar_t* prop);
value_t __map_access(map_t* m, wchar_t* prop);

value_t __map_store(map_t* m, wchar_t* prop, value_t v);

bool __map_delete(map_t* m, wchar_t* prop);

map_iterator_t* __get_map_iterator(map_t* m);

bool __map_iterator_has_next(map_iterator_t* it);

wchar_t* __map_iterator_get_next(map_iterator_t* it);

// This should really be in a private header, but we're using it in the interop code
struct map* __fresh_propbag(int init_props);

#endif // __SJS_MAP
