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
#include <map.h>

__attribute__((__aligned__(8))) wchar_t typeof_undefined[] = L"undefined";
__attribute__((__aligned__(8))) wchar_t typeof_number[] = L"number";
__attribute__((__aligned__(8))) wchar_t typeof_boolean[] = L"boolean";

extern int closure_table[];

Value arg_stack[100];
int nargs = 0;

void OP_CLEARARGS() {
    nargs = 0;
}

Value OP_POPARG() {
    if (nargs > 0) {
        return arg_stack[--nargs];
    } else {
        // if a function isn't called with enough args, JS says the other args get value undefined
        return __UNDEF__;
    }
}

void OP_PUSHARG(Value arg) {
    assert(nargs < 100);
    arg_stack[nargs++] = arg;
}

#include <typetags.h>
extern int untypedclosure_table[];
Value JS_Return;
Value OP_CALL(Value x) {
    Value ret = (Value)(0xFFFF000700000000ULL);
    assert (val_is_closure(x));
    closure_t* clos = (closure_t*)val_as_pointer(x);
    /*
     * Koushik's function prologue assumes that the top of the argument stack is the environment,
     * function, and base object (then args).  Call sites will push the receiver (or undefined for
     * function-style invocation), leaving this code to push the function and environment.
     */
    if (clos->vtbl == (object_map)closure_table) {
        // TODO: This is dynamically-typed invocation of typed closure.  Check argument types!
        // TODO: Need tag to determine typed/untyped, and if the former then arity, types, and return
        // type.  For now, we'll assume void since we're trying to invoke console.log
        // TODO: Use tags on typed closures to introduce type checks on arguments
        // Infer calling convention from type tag.  For now, until native built-ins are tagged,
        // assume void return for untagged closures
        bool voidret = clos->type == NULL || (clos->type != NONVOID_BUILTIN_TAG_HACK && clos->type->body.code_sig->ret->tag == TYPE_VOID);
        struct code_type *sig = clos->type->body.code_sig;
        // FFI objects don't have the required signatures, so in the interim we'll use MCOERCE
#define MCOERCE(v, t) ((clos->type != NULL) ? __coerce(v, t) : v)
        assert (nargs == sig->nargs); // TODO: more graceful handling of incorrect call arity
        // TODO: Doing this part directly from C is limiting; we can only emit fixed-arity calls,
        // while JS (including the SJS compiler) permits arbitrary arity.  At some point, this bit
        // will need to switch to inline assembly, to simply 'rep' (or whatever the ARM equivalent
        // is) push the args on the stack.
        // TODO: Figure out the right strategy here to avoid making typed interop calls super slow
        if (voidret) {
            switch (nargs) {
                // In each case, the args are in increasing order in arg_stack starting at 0 (at least
                // as TYPEDCALL pushes them), but the receiver is on the top of the stack
                // (arg_stack[nargs-1])
                // !!! TODO !!! Figure out returns!  On 32-bit platforms, the calling convention for
                // void return vs value_t return differs! and there's no tag to check (yet) to do the
                // right cast...
                case 0: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t), clos, object_as_val(NULL)); break;
                case 1: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t), clos, MCOERCE(arg_stack[0], sig->args[0])); break;
                        // Remember that we push arguments in reverse order
                //case 2: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t), clos, arg_stack[1], arg_stack[0]); break;
                case 2: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[1], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1])); break;
                case 3: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t), clos,  
                                MCOERCE(arg_stack[2], sig->args[0]), 
                                MCOERCE(arg_stack[0], sig->args[1]), 
                                MCOERCE(arg_stack[1], sig->args[2])); break;
                case 4: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[3], sig->args[0]), 
                                MCOERCE(arg_stack[0], sig->args[1]), 
                                MCOERCE(arg_stack[1], sig->args[2]), 
                                MCOERCE(arg_stack[2], sig->args[3])); break;
                case 5: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[4], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4])); break;
                case 6: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[5], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5])); break;
                case 7: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[6], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5]),
                                MCOERCE(arg_stack[5], sig->args[6])); break;
                case 8: PIVOT_INVOKE_CLOSURE(void (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[7], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5]),
                                MCOERCE(arg_stack[5], sig->args[6]),
                                MCOERCE(arg_stack[6], sig->args[7])); break;
                default:
                        assert(false && "unimplemented number of arguments...");
            }
            ret = __UNDEF__;
        } else {
            switch (nargs) {
                // In each case, the args are in increasing order in arg_stack starting at 0 (at least
                // as TYPEDCALL pushes them), but the receiver is on the top of the stack
                // (arg_stack[nargs-1])
                case 0: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t), clos, object_as_val(NULL)); break;
                case 1: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t), clos, MCOERCE(arg_stack[0], sig->args[0])); break;
                        // Remember that we push arguments in reverse order
                //case 2: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t), clos, arg_stack[1], arg_stack[0]); break;
                case 2: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[1], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1])); break;
                case 3: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t), clos,  
                                MCOERCE(arg_stack[2], sig->args[0]), 
                                MCOERCE(arg_stack[0], sig->args[1]), 
                                MCOERCE(arg_stack[1], sig->args[2])); break;
                case 4: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[3], sig->args[0]), 
                                MCOERCE(arg_stack[0], sig->args[1]), 
                                MCOERCE(arg_stack[1], sig->args[2]), 
                                MCOERCE(arg_stack[2], sig->args[3])); break;
                case 5: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t, value_t, value_t), clos, 
                                MCOERCE(arg_stack[4], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4])); break;
                case 6: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[5], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5])); break;
                case 7: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[6], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5]),
                                MCOERCE(arg_stack[5], sig->args[6])); break;
                case 8: ret = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t, value_t), clos,
                                MCOERCE(arg_stack[7], sig->args[0]),
                                MCOERCE(arg_stack[0], sig->args[1]),
                                MCOERCE(arg_stack[1], sig->args[2]),
                                MCOERCE(arg_stack[2], sig->args[3]),
                                MCOERCE(arg_stack[3], sig->args[4]),
                                MCOERCE(arg_stack[4], sig->args[5]),
                                MCOERCE(arg_stack[5], sig->args[6]),
                                MCOERCE(arg_stack[6], sig->args[7])); break;
                default:
                        assert(false && "unimplemented number of arguments...");
            }
        }
    } else {
        assert (clos->vtbl == (object_map)untypedclosure_table);
        OP_PUSHARG(x);
        OP_PUSHARG(object_as_val(clos->env.ptr));
        ((void(*)())(clos->code.ptr))();
        ret = JS_Return;
    }
    return ret;
#undef MCOERCE
}
Value TYPEDCALL(Value recv, Value f, int n, ...) {
    int x = n;
    va_list ap;
    va_start(ap, n);
    for (x = 0; x < n; x++) {
        value_t v = va_arg(ap, value_t);
        arg_stack[x] = v;
    }
    nargs = n;
    OP_PUSHARG(recv);
    va_end(ap);
    return OP_CALL(f);
}
Value OP_FUNCTION(void* fptr, Value envptr) {
    // allocate an untyped closure.  Note this may be used for constructors as well.
    // TODO: type validation...
    Value v;
#ifdef SMALL_POINTER
    v.tags.tag = 0;
#endif
    // TODO: what about constructors...?
    closure_t* clos = (closure_t*)closure_alloc_prim(envptr.ptr, NULL, fptr);
    clos->vtbl = (object_map)untypedclosure_table;
    v.ptr = clos;
    return v;
}

extern const int ___propname_count;
extern wchar_t** ____propname_by_id; // This is an external fixed-size array, so we need to do the same address-of hack we do with object tables :-/

int __prop_indirection_lookup(wchar_t* pname) {
    wchar_t** ids = (wchar_t**)&____propname_by_id;
    for (int i = 0; i < ___propname_count; i++) {
        if (wcscmp(ids[i], pname) == 0) {
            return i;
        }
    }
    return -1;
}

Value OP_GETPROP(Value x, Value y) {
    // TODO: primitive coercions
    // TODO: getters
    // TODO: protect runtime-internal names (closure fields, etc.)
    assert(val_is_object(x));
    object_t* o = val_as_object(x);
    if (val_is_int(y)) {
        int index = val_as_int(y);
        return array_get(x.obj, index);
    }
    wchar_t* pname = (wchar_t*)val_as_string(y);
    assert(pname != NULL); // TODO: null and undefined coerce to property names...
    int offset = __prop_indirection_lookup(pname);
    // TODO: implement a fast-path for compiling typed code, so a clean object can still have fast
    // access
    if (offset > -1) {
        object_t* curr = o;
        while (curr != NULL) {
            int phys_off = curr->vtbl != NULL ? curr->vtbl[offset] : -1;
            if (phys_off != -1) {
                return curr->fields[phys_off];
            } else if (curr->__propbag != NULL) {
                // TODO: look in bag.  Currently no primitive to distinguish undefined from not
                // there
                value_t tmp = __map_access((map_t*)curr, pname);
                if (!val_is_undef(tmp)) {
                    return tmp;
                }
            }
            curr = curr->__proto__;
        }
    } else {
        // TODO: climb looking in bags only.
    }
    // TODO: make this not horridly slow
    // TODO: look in propbag!
    return __UNDEF__;
}
Value OP_SETPROP(Value x, Value y, Value z) {
    // TODO: primitive coercions
    // TODO: setters
    assert(val_is_object(x));
    object_t* o = val_as_object(x);
    if (val_is_int(y)) {
        int index = val_as_int(y);
        return array_put(x.obj, index, z);
    }
    wchar_t* pname = (wchar_t*)val_as_pointer(y);
    assert(pname != NULL); // TODO: null and undefined coerce to property names...
    //fwprintf(stderr, L"Storing to property [%ls]\n", pname);
    int offset = __prop_indirection_lookup(pname);
    if (offset > -1 && o->vtbl != NULL && o->vtbl[offset] > -1) {
        // TODO: Verify type preservation
        return (o->fields[o->vtbl[offset]] = z);
    } else {
        if (wcscmp(L"__proto__", pname) == 0) {
            assert (val_is_object(z) || val_is_undef(z));
            if (val_is_object(z)) {
                o->__proto__ = val_as_object(z);
            }
            return z;
        }
        // TODO: If this creates a new property shadow, we've violated the fixed layout assumptions,
        // and need to trip the dirty flag
        if(o->__propbag == NULL) {
            o->__propbag = __fresh_propbag(10);
        }
        return __map_store((map_t*)o, pname, z);
    }
}
Value OP_SETGETTER(Value x, Value y) {
    // TODO: primitive coercions
    assert(false && "Unimplemented: OP_SETGETTER");
    return __UNDEF__;
}
Value OP_SETSETTER(Value x, Value y) {
    // TODO: primitive coercions
    assert(false && "Unimplemented: OP_SETSETTER");
    return __UNDEF__;
}
Value OP_DELPROP(Value x, Value y) {
    // TODO: primitive coercions
    assert(val_is_object(x));
    object_t* o = val_as_object(x);
    wchar_t* pname = (wchar_t*)val_as_pointer(y);
    assert(pname != NULL); // TODO: null and undefined coerce to property names...
    int offset = __prop_indirection_lookup(pname);
    if (offset > -1 && o->vtbl[offset] > -1) {
        assert(false && "Unsupported: removing fixed-layout property");
    } else {
        __map_delete((map_t*)o, pname);
    }
    return boolean_as_val(true);
}

// The following are only used by typed slow mode
// TODO: predicted versions, and inlining
// can get property name by lookup in ____propname_by_id table
// TODO: decouple this into finding the address of the storage location as a value_t*, and change to
// a RMW on that address.  Then all these lval ops can share more code.
Value OP_field_inc(Value v, int findex, bool postfix) {
    Value pre, post;
    assert(val_is_object(v));
    object_t *obj = v.obj;
    int phys = obj->vtbl[findex];
    if (phys >= 0) {
        pre = obj->fields[phys];
        post = OP_ADD(pre, int_as_val(1));
        obj->fields[phys] = post;
    } else {
        // TODO prototype climbing, and adding the field to the prop bag...
        assert(false);
    }
    return postfix ? post : pre;
}
Value OP_field_dec(Value v, int findex, bool postfix) {
    Value pre, post;
    assert(val_is_object(v));
    object_t *obj = v.obj;
    int phys = obj->vtbl[findex];
    if (phys >= 0) {
        pre = obj->fields[phys];
        post = OP_SUB(obj->fields[phys], int_as_val(1));
        obj->fields[phys] = post;
    } else {
        // TODO prototype climbing, and adding the field to the prop bag...
        assert(false);
    }
    return postfix ? post : pre;
}
