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
#include <stdio.h>
#include <stdlib.h>

#include "pair.h"

static void* data;
static cb_t registered_cb;

struct pair* alloc_pair() {
    struct pair* p = (struct pair*)malloc(sizeof(struct pair));
    p->x = 0;
    p->y = 0;
    return p;
}

void print_pair(struct pair* p) {
    printf("Pair: (%d,%d)\n", p->x, p->y);
}

void register_cb(void* d, cb_t f)
{
  data = d;
  registered_cb = f;
  printf("registered callback w/ data %p and function %p\n", d, f);
}

void add_pair(struct pair* p) {
// invoke function callback
    (*registered_cb)(data,p); // sjs
}


