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
#include <typetags.h>
#include <interop.h>
#include <map.h>

#define MAX_DEPTH 50

typedef struct {
    type_tag_t* sub;
    type_tag_t* sup;
} coind_subtype_hypothesis_t;

typedef struct {
    value_t val;
    type_tag_t* type;
} coind_valtype_hypothesis_t;

coind_subtype_hypothesis_t subtype_stack[MAX_DEPTH];
int32_t subtype_stack_n = 0;
coind_valtype_hypothesis_t valtype_stack[MAX_DEPTH];
int32_t valtype_stack_n = 0;

// All of these should be declared in a separate header listing all the static
// allocations generated in the subject program's body, rather than the runtime
extern int closure_table[];
extern int untypedclosure_table[];
extern int constructor_table[];
extern int array_table[];
// TODO: Right now, maps have no vtable...
extern wchar_t* ____propname_by_id[];
extern const int ___propname_count;

extern value_t JS_Return;

/* Trampolines
 *
 * Trampolines are standard closures, whose environments are actually pointers to closure being
 * wrapped.
 *
 * Typed closures are wrapped in trampolines that type-check arguments, when sending typed closures
 * to untyped code.  These we refer to as "Call Trampolines."  These are installed when typed
 * closures escape (i.e., not during a coercion/type-check).
 *
 * Untyped closures are wrapped in trampolines that type-check return values.  These we call "Return
 * Trampolines."  These are installed when coercing untyped closures to typed.
 */
// Note: 0-arg refers to number of args after receiver
value_t _call_trampoline_v0(env_t env, value_t recv) {
    closure_t *clos = (closure_t*)env;
    assert (clos->type->tag == TYPE_CLOSURE);
    value_t res = PIVOT_INVOKE_CLOSURE(value_t (*)(env_t,value_t), clos, __coerce(recv, clos->type->body.code_sig->args[0]));
    return res;
}
value_t _return_trampoline_v0(env_t env, value_t recv) {
    closure_t *clos = (closure_t*)env[0];
    type_tag_t *type = (type_tag_t*)env[1];
    assert (type != NULL);
    assert (type->tag == TYPE_CLOSURE);
    // This is an untyped closure
    OP_PUSHARG(recv);
    OP_CALL(closure_as_val(clos));
    value_t ret = JS_Return;
    return __coerce(ret, type->body.code_sig->ret);
}
value_t _return_trampoline_v1(env_t env, value_t recv, value_t arg1) {
    closure_t *clos = (closure_t*)env[0];
    type_tag_t *type = (type_tag_t*)env[1];
    assert (type->tag == TYPE_CLOSURE);
    // This is an untyped closure
    OP_PUSHARG(arg1);
    OP_PUSHARG(recv);
    value_t ret = OP_CALL(closure_as_val(clos));
    return __coerce(ret, type->body.code_sig->ret);
}
value_t _return_trampoline_v2(env_t env, value_t recv, value_t arg1, value_t arg2) {
    closure_t *clos = (closure_t*)env[0];
    type_tag_t *type = (type_tag_t*)env[1];
    assert (type->tag == TYPE_CLOSURE);
    // This is an untyped closure
    OP_PUSHARG(arg2);
    OP_PUSHARG(arg1);
    OP_PUSHARG(recv);
    value_t ret = OP_CALL(closure_as_val(clos));
    return __coerce(ret, type->body.code_sig->ret);
}


value_t __coerce(value_t val, type_tag_t* type) {
    assert(type != NULL);
    if (val.box == __UNDEF_BYTES__)
        return val;

    switch (type->tag) {
        case TYPE_CLOSURE:
            if (!val_is_object(val)) {
                _____type_violation();
            } else {
                object_t* clos = val_as_object(val);
                // TODO: What do I do if I get a trampoline in here???
                if (clos->vtbl == (object_map)closure_table) {
                    // Typed closure
                    if (!__subtype_lt(clos->type, type)) {
                        _____type_violation();
                    }
                } else if (clos->vtbl == (object_map)untypedclosure_table) {
                    // TODO: wrap in a trampoline
                    // Note that we don't modify the vtable of the untyped closure.  This is
                    // crucial: it allows us to coerce the same untyped closure to multiple
                    // incompatible types (e.g., to abuse the FFI for polymorphism)
                    // TODO: This does install a typed vtable on the wrapped closure.  Should
                    // eventually unwrap trampolines.
                    object_t *trampoline = NULL;
                    preenv_t env = env_alloc_prim(2);
                    env[0] = (value_t*)clos;
                    env[1] = (value_t*)type;
                    switch (type->body.code_sig->nargs) {
                        case 0:
                            trampoline = closure_alloc_prim(env, type, _return_trampoline_v0);
                            break;
                        case 1:
                            trampoline = closure_alloc_prim(env, type, _return_trampoline_v1);
                            break;
                        case 2:
                            trampoline = closure_alloc_prim(env, type, _return_trampoline_v2);
                            break;
                        default:
                            assert(false && "UNIMPLEMENTED number of closure args for coercion");
                    }
                    return object_as_val(trampoline);
                } else {
                    // TODO: Handle trampolines, which will carry their own vtables... should have a
                    // code address in the tag, similar to the vtable address we should carry in
                    // object tags.
                    // TODO: Constructors

                    // If we're here and not a trampoline, there's no code associated with this
                    // object
                    _____type_violation();
                }
            }
            break;
        case TYPE_STRING:
            // TODO: Need to sort out string tagging again...
            assert(false && "UNIMPLEMENTED");
        case TYPE_OBJECT:
            if (val_is_object(val)) {
                object_t* o = val_as_object(val);
                if (o == NULL) {
                    return val;
                }
                if (o->vtbl != NULL) {
                    // This object is already typed, so we want to verify the subtyping relationship
                    // between the installed tag and this one
                    if (o->type != type && !(__subtype_lt(o->type, type))) {
                        _____type_violation();
                    }
                } else {
                    assert (o->type == NULL);
                    // allocated by untyped code, need to install vtable and tag, migrate properties
                    // from prop bag....
                    /* For recursive types/objects, need to install type tag first, so recursive
                       coercion (even to a subtype of the current target tag) will assume success 
                       of the current coercion.  

                       This is somewhat problematic if a nested coercion fails; need to undo the
                       speculative memoization.
                       
                       This is actually also stricter than strictly necessary.
                       In principle, *this* coercion might be targeting type A, while the recursive
                       coercion targets type B, for B <: A.  In this case, the current approach will
                       reject, because the recursive call just assumes it's A, which is </: B.
                       It's plausible at that point to recognize this (some protocol could be established
                       where the tag is set at the start of coercion and the vtable set after) and
                       start a "safe downcast" from A to B.  Of course, there could be a later recursive 
                       coercion (triggered by this current coercion, not the coercion to B) targeting 
                       some C <: B, in which case the B coercion may have already sealed the object...
                       Unless we set a general policy to attempt to check downcast safety when a
                       basic tag-based subtyping check fails, there will always be some such coercion that
                       could in theory be made to work but fails because we refuse to downcast.

                       The downcast attempts would be expensive, which is a deterrent, but at the same time
                       they would permit a bit more code to work, and once a downcast succeeds subsequent 
                       coercions would be just as fast.  Hmm, should poke around to see: I don't think
                       any of the gradual typing + mutation stuff on monotonic references has been done in
                       a type system with subtyping.  At least not formally.  Reticulated Python has
                       some notion of subtyping and uses monotonic references, but the paper's light on
                       low-level details.  This notion of repeatedly checking safe downcasts seems to
                       correspond to monotonic references in a gradually typed system with subtyping,
                       which I haven't seen noted before.  Need to check whether Reticulated uses
                       nominal or structural subtyping.  Nominal downcasts are cheap, structural not.
                       And in our system, the downcasts could be even more expensive and have non-local
                       performance implications, since moving more properties to fixed-layout slots
                       changes the vtable, which means there's an interaction with object layout...
                    */

                    /*
                     *  On the dynamic generation of new vtables:  One way or another, we're going
                     *  to end up generating vtables at runtime for prototype parents of coerced
                     *  objects --- we can't statically predict where inherited properties will end
                     *  up living.  If we do this poorly, it will break the field access
                     *  optimizations.
                     *
                     *  We can statically generate vtables for the writable fragment of any known
                     *  coercion type (i.e., untyped-import object type, or any object type in a
                     *  negative position of an untyped-import method/function type...), leaving the
                     *  read-only properties of originally-untyped objects to just be slower to
                     *  access by always forcing a crawl in that case.  In exchange for that cost,
                     *  we can dynamically generate ... no.  I was thinking we could dynamically
                     *  generate vtables for the prototype(s) of the coerced untyped object, by
                     *  making their layouts sparse and using the same physical offsets in the
                     *  prototype(s) as the forwarding pointer in the object being coerced.  But if
                     *  the child only has the writable properties in its vtable, then that actually
                     *  doesn't help us; we'd still need fresh vtables for the prototypes.
                     *
                     *  An alternative to consider is the output of the global field-layout
                     *  optimization.  If it's successful (after giving it extra input about the
                     *  types that may describe coerced untyped objects, since currently it only
                     *  looks at concrete layouts at allocations, so it would need more information),
                     *  then it means no two properties that are accessed by typed code on the same
                     *  object have the same physical offsets --- so we could embed that
                     *  propname->physoff information in the binary and generate compatible vtables
                     *  on the fly.  Slightly more generally, the non-collision really only needs to
                     *  hold for properties involved in the untyped FFI.
                     *
                     *  A really coarse approximation of this check would be to disable the field
                     *  access optimizations for any property involved in the untyped import (direct
                     *  objects and negative positions...).  Actually, we might be able to slightly
                     *  refine that: any writable property has to be there, so we can generate
                     *  subsets of the vtables at compile time: we can generate the physical layout
                     *  for the writable properties of the import (and feed that vtable to the
                     *  global optimization and field access optimizer passes), and just disable the
                     *  optimization for properties that show up read-only in an imported object
                     *  type.  So if we import {a:bool,x:int|y:int}, we generate physical layout
                     *  constraints for a and x, which allows safe reasoning about optimizing
                     *  accesses to properties a and x.  Then when coercing an untyped object to the
                     *  type above, we dynamically generate a vtable for that layout, and possibly
                     *  add to it depending on whether the RO property is local or inherited.  And
                     *  since y appears there, we disable access optimization for y (in this coarse
                     *  version...).  Think through how this all interacts with the "co-occurrence"
                     *  checks.
                     */

                    // TODO: Object tags should maybe include a vtable for installation...
                    // TODO: Need a property count or argv-style null terminator
                    // TODO: Need r/w split
                    // TODO: mro/mrw? Yes.  Don't need to do anything when converting the object,
                    // but dynamic code needs to have the mro/mrw around when installing typed
                    // method into typed object.  No check to do when installing untyped into typed,
                    // just the trampoline to install.
                    // TODO: instead of an array of string names, the prop list should be
                    // indirection table offsets, which can then be looked up by doing a lookup in
                    // propname_by_id
                    // TODO Be sure to handle local indirections correctly (in local vtable, but not
                    // local)
                    int nextslot = 0;
                    struct type_tag **field_tags = type->body.object_sig.field_tags;
                    //wchar_t **prop_names = type->body.object_sig.prop_names;
                    uint32_t *prop_idxs = type->body.object_sig.prop_indices;
                    //struct type_tag *proto_tag = type->body.object_sig.proto_tag;
                    object_map model_map = type->body.object_sig.vtbl_model;
                    // 1. Relocate local properties.
                    //      Running out of slots in this step is a fatal error.
                    // We must place fields in agreement with the model map...
                    o->vtbl = (object_map)malloc(sizeof(int)*___propname_count);
                    while (*field_tags != NULL) {
                        int off = 0;
                        bool isRO = false;
                        if (0x10000000 & *prop_idxs) {
                            off = (int)(~(*prop_idxs));
                            isRO = true;
                        } else {
                            off = *prop_idxs;
                            isRO = false;
                        }
                        wchar_t *propname = ____propname_by_id[off];
                        if (__map_contains(o, propname)) {
                            if (nextslot >= _FIXED_LAYOUT_CONVERSION_SLOTS_)
                                _____type_violation(); // Can't coerce!
                            o->fields[nextslot] = __coerce(__map_access(o, propname), *field_tags);
                            ((pre_object_map)o->vtbl)[off] = nextslot++;
                            // TODO: would benefit from fetch-and-delete prim to avoid another
                            // duplicated lookup.  Really,
                            //   bool __map_delete_if_present(map_t*, wchar_t*, out value_t*)
                            __map_delete(o, propname);
                        } else if(isRO) {
                            // TODO: This is just a sketch, currently.  Right now we can't really
                            // assume the prototype is fixed-layout, as the code below requires.
                            // The correct version of this needs to run after coercing the prototype
                            // chain, at which point any inherited property that's actually present
                            // on the prototype chain will have been migrated to *some* fixed-layout
                            // slot, and the code below would work

                            // TODO: move this to after prototype coercion (and fix prototype
                            // coercion
                            // Property must be inherited.  If it's in the immediate parent, install
                            // a forwarding pointer and update the vtable.
                            object_t *curproto = o->__proto__;
                            if (curproto->vtbl[off] != -1) {
                                // Inherited from immediate parent - install fwd pointer
                                int proto_off = curproto->vtbl[off];
                                if (nextslot >= _FIXED_LAYOUT_CONVERSION_SLOTS_)
                                    _____type_violation(); // Can't coerce!
                                o->fields[nextslot].box = ((uintptr_t)&curproto->fields[proto_off]) | 0x1;
                                ((pre_object_map)o->vtbl)[off] = nextslot++;
                            } else {
                                // Better be somewhere up the prototype chain...
                                while (curproto != NULL) {
                                    if (curproto->vtbl[off] != -1)
                                        break;
                                    curproto = curproto->__proto__;
                                }
                                if (curproto == NULL)
                                    _____type_violation();
                                // else it's up there somewhere, and the crawl will find it.
                            }
                        } else {
                            _____type_violation();
                        }
                        prop_idxs++;
                        //prop_names++;
                        field_tags++;
                    }

                    // 2. Recursively coerce prototype
                    // coerce(o.prototype, t - localprops(o));
                    // We can't really allow skipping to inherited-read optimization, because a dyn obj coerced
                    // to typed might shadow a property locally that was never at "this level" of
                    // the prototype chain in typed code.  This actualy also potentially messes with
                    // vtable gen; how do we avoid generating vtables for all possible subsets of ro
                    // properties being physically present?  Doing so would screw up physical layout
                    // optimization pretty badly.
                    // TODO: Redo prototype coercion completely
                    //o->__proto__ = val_as_object(__coerce(object_as_val(o->__proto__), proto_tag));

                    // 3. Install local-forward pointers for inherited properties, until we run out
                    // of slots.  This is done after coercing the prototype so we'll have final
                    // property addresses to tag.
                    // TODO: migrate appropriate case from above to down here, after fixing
                    // prototype coercion


                    // TODO: how to we back out partial coercions when the "outer" of two or more
                    // mutually recursive objects fails after the inner coercion has succeeded?
                    // Can't just undo all tagging because we might untag something that's actually
                    // typed
                    // TODO: Can worry about this after failstop works
                }
            } else {
                assert (false && "UNIMPLEMENTED coercion from non-object to object, such as primitive promotion");
            }
            break;
        case TYPE_ARRAY:
            // TODO: Try to install the array vtable, then recursively coerce array elements.
            // TODO: Return original if the array vtable is already installed and the type tag is
            // correct.
            // TODO: if dirty flag is set in a recursive call, this overall coercion failed, and we
            // need to back out the vtable/tag changes!
            // TODO: For dyn. object, need to find the range of array indices in the propbag,
            // initialize array pieces, install vtable, and convert elements.
            if (val_is_object(val)) {
                object_t* o = val_as_object(val);
                if (o->vtbl == (object_map)array_table) {
                    // already a typed array.  If the type's not already equal, fail.
                    if (o->type != type && !(__typetag_eq(o->type, type))) {
                        _____type_violation();
                    }
                    // else, already correctly typed.
                } else {
                    if (o->vtbl != NULL) {
                        // This is already some other typed object
                        _____type_violation();
                    } else {
                        // otherwise, convert
                        /* for key in array:
                            if (array[key]!=undef) coerce(array[key], type->body->array_elem)
                         */
                        assert(false && "UNIMPLEMENTED");
                    }
                }
            } else {
                // not a heap object, not an array
                _____type_violation();
            }
            assert(false && "UNIMPLEMENTED");
        case TYPE_CODE:
            // TODO: Delete this?  It's redundant w/ TYPE_CLOSURE...
            assert (false && "REMOVE ME");
        case TYPE_VOID:
            // Not 100% sure about this, but I suppose undefined is the one value that could be
            // considered void..
            if (val.box != __UNDEF_BYTES__) {
                _____type_violation();
            }
            break;
        case TYPE_TOP:
            // This is here for the receiver component of functions, which couldn't care less about
            // the receiver arg
            break;
        case TYPE_FLOAT:
            assert (false && "UNIMPLEMENTED because float shifting is unimplemented");
        case TYPE_INT:
            if (!val_is_int(val)) {
                _____type_violation();
            }
            break;
        case TYPE_BOOL:
            if (!val_is_boolean(val)) {
                _____type_violation();
            }
            break;
        case TYPE_MAP:
            /* for key in map:
                coerce(m[key], type->body->array_elem)
             */
            assert(false && "UNIMPLEMENTED");
        default:
            assert(false && "ERROR: Invalid type tag");
    }
    return val;
    // TODO: Why do we return val?  Why not return a boolean, which is more informative at the call
    // site?  Ah, need to permit untyped closure coercion to produce a trampoline.  Still, would be
    // nice to also have success/failure as a boolean.  Should make one of these an out-param.
}

/* TODO: All of the tags are generated at compile-time.  This means we could also
 * precompute the subtyping and equality relationships, which would make checks (as opposed to
 * coercions) quite fast.
 */
bool __subtype_lt(type_tag_t* sub, type_tag_t* sup) {
    // Check subtype stack for coinductive hypotheses
    for (int i = 0; i < subtype_stack_n; i++) {
        if (subtype_stack[i].sub == sub && subtype_stack[i].sup == sup)
            return true;
    }

    bool func_to_meth_conversion = false;
    assert(sub != NULL);
    assert(sup != NULL);
    switch (sub->tag) {
        case TYPE_CLOSURE:
            if (sub->body.code_sig->codetype != sup->body.code_sig->codetype) {
                if (sub->body.code_sig->codetype != CODE_FUNCTION ||
                        sup->body.code_sig->codetype != CODE_METHOD) {
                    return false;
                } else {
                    func_to_meth_conversion = true;
                }
            }
            // Correct varieties (or func->method conversion)
            // Check arities
            code_type_t *subsig = sub->body.code_sig;
            code_type_t *supsig = sup->body.code_sig;
            int sub_args = subsig->nargs;
            int sup_args = supsig->nargs;
            if (sub_args != sup_args || (func_to_meth_conversion && sub_args + 1 != sup_args))
                    return false;

            assert(subtype_stack_n+1 < MAX_DEPTH);
            subtype_stack[subtype_stack_n].sub = sub;
            subtype_stack[subtype_stack_n].sup = sup;
            subtype_stack_n++;
            if (!__subtype_lt(subsig->ret, supsig->ret)) {
                return false;
            }
            int subi = 0;
            int supi = 0;
            if (func_to_meth_conversion) {
                supi++; // Reciever not recorded in function, no comparison to do b/c functions don't care
            }
            while (supi < sup_args) {
                if (!__subtype_lt(subsig->args[subi], supsig->args[supi]))
                    return false;
                subi++;
                supi++;
            }

            // TODO: proto is a ref on constructors
            if (subsig->codetype == CODE_CTOR && !__typetag_eq(subsig->proto, supsig->proto)) {
                return false;
            }

            subtype_stack_n--;
            return true;

        case TYPE_OBJECT:
            if (sup->tag != TYPE_OBJECT)
                return false;
            int i = 0;
            uint32_t *prop_idxs = sup->body.object_sig.prop_indices;
            struct type_tag **field_tags = sup->body.object_sig.field_tags;
            // Make sure every property present in the supertype is present in the subtype
            // TODO: This is worst-case quadratic!!!  Fix this!
            while (*field_tags) {
                i = 0;
                int found = 0;
                while (!found && sub->body.object_sig.field_tags[i] != NULL) {
                    if (abs(*prop_idxs) == abs(sub->body.object_sig.prop_indices[i])) {
                        found = 1;
                    } else {
                        i++;
                    }
                }
                if (!found) {
                    // Missing property
                    return false;
                }
                if (!__typetag_eq(*field_tags, sub->body.object_sig.field_tags[i])) {
                    // wrong type
                    return false;
                }
                field_tags++;
                prop_idxs++;
            }
            return true;

        case TYPE_ARRAY:
        case TYPE_MAP:
            return __typetag_eq(sub->body.array_elem, sup->body.array_elem);

        case TYPE_INT:
            return sup->tag == TYPE_INT || sup->tag == TYPE_FLOAT;

        case TYPE_VOID:
        case TYPE_FLOAT:
        case TYPE_BOOL:
        case TYPE_STRING:
            return sub->tag == sup->tag;

        case TYPE_CODE:
        default:
            return false;
    }
}
bool __typetag_eq(type_tag_t *a, type_tag_t *b) {
    assert(a != NULL);
    assert(b != NULL);
    if (a == b)
        return true;
    switch (a->tag) {
        case TYPE_CLOSURE:
            assert(true); // This is totally ridiculous: Without this, the next line is a parse error...
            code_type_t *asig = a->body.code_sig;
            code_type_t *bsig = b->body.code_sig;
            if (asig->codetype != bsig->codetype)
                return false;
            if (asig->nargs != bsig->nargs)
                return false;
            if (asig->codetype == CODE_CTOR && !__typetag_eq(asig->proto, bsig->proto))
                return false;
            if (!__typetag_eq(asig->ret, bsig->ret)) {
                return false;
            }
            for (int i = 0; i < asig->nargs; i++) {
                if (!__typetag_eq(asig->args[i], bsig->args[i]))
                    return false;
            }
            return true;

        case TYPE_OBJECT:
            // TODO sort property indices (int encoding of field names) in TypeTagSerializer, so
            // with a size parameter, we can just memcmp the field buffers for a quick reject
            assert(false); // TODO

        case TYPE_ARRAY:
        case TYPE_MAP:
            return __typetag_eq(a->body.array_elem, b->body.array_elem);

        case TYPE_INT:
        case TYPE_VOID:
        case TYPE_FLOAT:
        case TYPE_BOOL:
        case TYPE_STRING:
            return a->tag == b->tag;

        case TYPE_CODE:
        default:
            return false;
    }
}
