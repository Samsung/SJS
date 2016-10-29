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
#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>
#include<math.h>

#include "xxhash.h"

#ifndef __SJS__
// Crutches for compiling separately from SJS runtime
typedef uint64_t value_t;
typedef uint8_t bool;
#define true 1
#define false 0

#define MEM_ALLOC(x) malloc(x)
#define MEM_FREE(x) free(x)
#define MEM_ALLOC_ATOMIC(x) malloc(x)
#endif

#ifdef __SJS__
#ifdef USE_GC
#include <gc.h>
#endif
#include <runtime.h>
#endif

// Assume for now we'll use the xxhash library, which is a 2-clause BSD licensed hash lib with
// excellent perf: https://code.google.com/p/xxhash/
extern uint32_t XXH32(const void* input, size_t length, unsigned seed);

typedef struct node {
    wchar_t* key;
    value_t val;
    struct node *next;
} node_t;

typedef struct map {
    int32_t nbuckets;
    uint32_t mask;
    node_t **buckets;
} map_t;

map_t* __alloc_map(int32_t init_props) {
    // Round #buckets up to next power of 2
    int32_t pow = ceil(log2(init_props));
    uint32_t nbuckets = (1 << pow);
    uint32_t mask = nbuckets - 1; // bit mask for this power of 2
    node_t** buckets = (node_t**)MEM_ALLOC(sizeof(node_t*)*nbuckets);
    memset(buckets, 0, nbuckets*sizeof(node_t*));
    map_t* m = (map_t*)MEM_ALLOC(sizeof(map_t));
    m->nbuckets = nbuckets;
    m->mask = mask;
    m->buckets = buckets;
    return m;
}

value_t __map_access(map_t* m, wchar_t* prop) {
    uint32_t hash = XXH32(prop, wcslen(prop), 0);
    uint32_t bucket = hash & m->mask;
    node_t* cell = m->buckets[bucket];
    while (cell != NULL) {
        if (___sjs_strcmp(cell->key, prop) == 0) {
            // match!
            return cell->val;
        }
        cell = cell->next;
    }
    //assert (cell != NULL); // TODO: Exceptions!  Or, really, we need to return undefined here.
    return (value_t){ .box = 0ULL };
}

// TODO: Resize crowded maps
value_t __map_store(map_t* m, wchar_t* prop, value_t v) {
    uint32_t hash = XXH32(prop, wcslen(prop), 0);
    uint32_t bucket = hash & m->mask;
    if (m->buckets[bucket] == NULL) {
        node_t* n = (node_t*)MEM_ALLOC(sizeof(node_t));
        n->key = prop;
        n->val = v;
        n->next = NULL;
        m->buckets[bucket] = n;
        return v;
    } else {
        // hash collision
        node_t* curr = m->buckets[bucket];
        while (curr != NULL) {
            if (___sjs_strcmp(curr->key, prop) == 0) {
                curr->val = v;
                return v;
            }
            curr = curr->next;
        }
        // If we reach this point, we're inserting rather than replacing
        node_t* n = (node_t*)MEM_ALLOC(sizeof(node_t));
        n->key = prop;
        n->val = v;
        n->next = m->buckets[bucket];
        m->buckets[bucket] = n;
        return v;
    }
}

// TODO: This will break on 32-bit, since string ptrs will only be 32 bits...
map_t* __alloc_map_literal(int n, ...) {
    map_t* m = __alloc_map(n);
    int i = 0;
    va_list ap;
    if (n == 0) return m;
    va_start(ap, n);
    for (i = 0; i < n; i++) {
        value_t name = va_arg(ap, value_t);
        value_t val = va_arg(ap, value_t);
        //__map_store(m, va_arg(ap, char*), va_arg(ap, value_t));
        __map_store(m, val_as_string(name), val);
    }
    va_end(ap);
    return m;
}

// TODO: spec says return false if it's a non-configurable property, which we shouldn't need to
// worry about for SJS maps
// TODO: shrink
bool __map_delete(map_t* m, wchar_t* prop) {
    uint32_t hash = XXH32(prop, wcslen(prop), 0);
    uint32_t bucket = hash & m->mask;
    node_t* n = m->buckets[bucket];
    if (n != NULL) {
        node_t* prev = NULL;
        while (n != NULL && ___sjs_strcmp(n->key, prop) != 0) {
            prev = n;
            n = n->next;
        }
        if (n != NULL) {
            // found it
            if (prev == NULL) {
                m->buckets[bucket] = n->next; // drop first link in bucket
            } else {
                prev->next = n->next;
            }
        }
    }

    return true;
}

/* Iteration:
 *
 * TODO: for..in loops for arrays
 * TODO: for..in loops and indexing for strings
 *
 * for..in loops need to be compiled down as iterators.
 * 
 * One option is to have a basic iterator with a compound (bucket+node) index into the hash table.
 * This is memory-efficient, but hasNext() becomes quite slow, needing to iterate ahead to search.
 *
 * Another option is to add an element count to the table, which will slow insertion and deletion
 * slightly, but allow hasNext to proceed by comparing a count of previously iterated objects to the
 * element count of the map.  next() still needs to search linearly.
 *
 * A final option is to thread a linked list through the hashtable nodes.  A singly-linked version
 * would make deletion complex, unless it only removed it from the hash and flagged it for deletion
 * from the linked list, which would be lazily handled by the next iterator pass... that small perf
 * overhead seems preferable to the uniform additional overhead of the extra accesses to double
 * links at insertion and deletion.
 */

typedef struct map_iterator {
    // map being traversed
    map_t* map;
    // The next node whose key will be returned by a next() call
    node_t* next_node;
    // Book-keeping, of the next place to look after the current node's
    // chain runs out
    uint32_t next_bucket;
} map_iterator_t;


map_iterator_t* __get_map_iterator(map_t* map) {
    map_iterator_t* iter = (map_iterator_t*)MEM_ALLOC(sizeof(map_iterator_t));
    iter->map = map;
    iter->next_node = NULL;
    int i = 0;
    // Find the first non-empty bucket, and initialize the next ptr
    while (i < map->nbuckets) {
        if (map->buckets[i] != NULL) {
            iter->next_node = map->buckets[i];
            break;
        }
        i++;
    }
    // Find the next non-empty bucket, if it exists
    i++;
    while (i < map->nbuckets) {
        if (map->buckets[i] != NULL) {
            break;
        }
        i++;
    }
    iter->next_bucket = i; // may be == map->nbuckets (no further non-empty buckets)
    // We take to heart that iterator semantics are undefined when the map is modified mid-iteration
    // allocation and each next() call searches ahead for the next reference.
    //fprintf(stderr, "__get_map_iterator(%p) --> %p\n", map, iter);
    return iter;
}

bool __map_iterator_has_next(map_iterator_t* iter) {
    //fprintf(stderr, "__map_iterator_has_next(%p)\n", iter);
    return iter->next_node != NULL;
}

wchar_t* __map_iterator_get_next(map_iterator_t* iter) {
    assert (iter->next_node != NULL);
    wchar_t* ret = iter->next_node->key;
    if (iter->next_node->next) {
        iter->next_node = iter->next_node->next;
    } else {
        // end of bucket chain: search for the next bucket
        node_t* next = NULL;
        int i = iter->next_bucket;
        while (i < iter->map->nbuckets) {
            if (next == NULL && iter->map->buckets[i]) {
                next = iter->map->buckets[i];
                i++;
            } else if (next != NULL && iter->map->buckets[i]) {
                break; // found a non-empty bucket after the next bucket
            } else {
                // Two cases:
                // 1) next==NULL & empty bucket[i]
                // 2) next!=NULL & empty bucket[i]
                i++;
            }
        }
        // After this loop, i <= iter->map->nbuckets.  If <, we found a second non-empty bucket
        // after initializing next to non-NULL.  Either way, if next is non-NULL we've found a next
        // node.  If < and next is NULL, we've got a problem.
        assert (next != NULL || i == iter->map->nbuckets);
        iter->next_node = next;
        iter->next_bucket = i;
    }
    //fprintf(stderr, "__map_iterator_get_next(%p) --> %s\n", iter, ret);
    return ret;
}

#ifndef __SJS__
int main() {
    map_t* m = __alloc_map(3);
    __map_store(m, L"answer", 42);
    __map_store(m, L"hello", 9);
    assert(__map_access(m, L"answer") == 42);
    assert(__map_access(m, L"hello") == 9);
}
#endif


