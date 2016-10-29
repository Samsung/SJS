# Copyright 2014-2016 Samsung Research America, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#!/usr/bin/python
'''
This script parses the .tidl files HQ sent us for their Eirene UI library (which wraps
Elementary/EFL), and emits type declarations for the constructors/types described therein.
'''

import sys

namedTypes = {};
includes = []
classDecls = [];
funDecls = [];
inline_glue_code = [];

class EnumType:
    def __init__(self, name):
        self.name = name;
    def toJSON(self):
        return { "typefamily": 'int'};
    def toNativeType(self):
        return self.name;
    def toCPPName(self):
        return "value_t"; #"int";
    def unwrap(self, val_str):
        return "("+self.name+")val_as_int("+val_str+")";
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        return "(int_as_val((int)"+val_str+"))";

class TypeName:
    def __init__(self, name):
        if (name.startswith('const ')):
            self.name = name[6:];
            self.const = True;
        else:
            self.name = name;
            self.const = False;
    def toJSON(self):
        if (self.name.endswith('&')):
            return { "typefamily": 'name', "name": self.name[:-1] };
        else:
            return { "typefamily": 'name', "name": self.name };
    def toNativeType(self):
        return ("const " if self.const else "")+self.name+("" if self.name.endswith("&") else "*");
    def toCPPName(self):
        return "value_t"; "object_t*";
    def unwrap(self, val_str):
        # val_str is the C++ representation of an SJS runtime entity, here an object_t*
        name = self.name;
        prefix = "";
        if name.endswith('&'):
            # unwrapping in a byref argument position
            name = name[:-1];
            prefix = "*";
        return prefix+"((%(clazz)s*)(val_as_object(%(exp)s)->fields[0].ptr))" % { 'clazz': name, 'exp': val_str };
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        cpp_byref = self.name.endswith("&");
        name = self.name[:-1] if cpp_byref else self.name;
        prefix = "&" if cpp_byref else "";
        return "object_as_val(__init_wrap_"+name+"((object_t*)new cppobj<"+str(len(namedTypes[name].methods)+1)+",0>(), ("+name+"*)"+prefix+val_str+"))";
    def getCPP(self, mname, clazz, self_offset):
        print "ERROR: Can't get C++ code for a type alias: "+self.name;
        assert(False);

class StringType:
    def __str__(self):
        return "string";
    def toJSON(self):
        return { "typefamily": 'string' };
    def toNativeType(self):
        return "wchar_t*";
    def toCPPName(self):
        return "value_t"; #"wchar_t*";
    def unwrap(self, val_str):
        return "val_as_string(%s)" % val_str;
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        return "string_as_val(%s)" % val_str;

class IntegerType:
    def __str__(self):
        return "int";
    def toJSON(self):
        return { "typefamily": 'int' };
    def toNativeType(self):
        return "int";
    def toCPPName(self):
        return "value_t"; #"int";
    def unwrap(self, val_str):
        return "val_as_int(%s)" % val_str;
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        return "int_as_val(%s)" % val_str;

class UnsignedIntegerType:
    # This class is a lie, of sorts.  SJS things it's signed.
    def __str__(self):
        return "uint";
    def toJSON(self):
        return { "typefamily": 'int' };
    def toNativeType(self):
        return "unsigned long";
    def toCPPName(self):
        return "value_t"; #"int";
    def unwrap(self, val_str):
        return "((unsigned long)val_as_int("+val_str+"))";
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        return "int_as_val((int)"+val_str+")";

class BooleanType:
    def __str__(self):
        return "boolean";
    def toJSON(self):
        return { "typefamily": 'bool' };
    def toNativeType(self):
        return "bool";
    def toCPPName(self):
        return "value_t"; #"bool";
    def unwrap(self, val_str):
        return "val_as_boolean(%s)" % val_str;
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        return "boolean_as_val(%s)" % val_str;

class DoubleType:
    def __str__(self):
        return "double";
    def toJSON(self):
        return { "typefamily": 'double' };
    def toNativeType(self):
        return "double";
    def toCPPName(self):
        return "value_t"; #"double";
    def unwrap(self, val_str):
        # TODO: encode/decode!!!
        return "val_as_double_noenc(%s)" % val_str;
    # wrap :: <C++ platform rep> -> <SJS runtime rep>
    def wrap(self, val_str):
        # TODO: encode/decode!!!
        return "double_as_val_noenc(%s)" % val_str;

class VoidType:
    def __str__(self):
        return "void";
    def toJSON(self):
        return { "typefamily": 'void' };
    def toNativeType(self):
        return "void";
    def toCPPName(self):
        return "void";
    def unwrap(self, val_str):
        raise "Shouldn't unwrap 'values' of type void";
    def wrap(self, val_str):
        raise "Shouldn't wrap 'values' of type void";

class ArrayType:
    def __init__(self, elemType):
        self.elemty = elemType;
    def toJSON(self):
        return { "typefamily": 'array', "elemtype": self.elemty.toJSON() };
    def toNativeType(self):
        assert False;
    def toCPPName(self):
        return "object_t*";
    def unwrap(self, val_str):
        return val_str; # TODO: Need SJS<->C++ array interface...
    def wrap(self, val_str):
        return val_str; # TODO: Need SJS<->C++ array interface...

def encode_mname(m,clazz):
    return "__"+clazz+"__"+m;
class AttachedMethodType:
    def __init__(self, args, ret):
        self.args = args;
        self.out = None;
        self.ret = ret;
    def toJSON(self):
        if (self.out):
            return { 'typefamily': 'method',
                    'args': map(lambda t: { 'name': "x", 'type': t.toJSON() }, self.args),
                     'return': self.out.toJSON() };
        else:
            return { 'typefamily': 'method',
                    'args': map(lambda t: { 'name': "x", 'type': t.toJSON() }, self.args),
                     'return': self.ret.toJSON() };
    def toNativeType(self):
        assert False; # This should only be serialized to C++ by the class instance
    def toCPPName(self):
        # Note that the env_t is implicit in the closure
        ty = "closure<"+self.ret.toCPPName()+",value_t";
        for a in self.args:
            ty = ty + ", " + a.toCPPName();
        ty = ty + ">*";
        return ty;
    def getCPP(self, mname, clazz, self_offset):
        # TODO: self_offset is now unused, since it WAS the offset of the receiver, which is now at
        # 0
        cpp_mname = encode_mname(mname,clazz);
        argstring = (", ".join(map(lambda (n,t): (t.toCPPName()+" _arg"+str(n)),zip(range(0,len(self.args)),self.args))));
        if (len(self.args) > 0):
            sig = "%s %s(env_t env, value_t _self, %s)"
            sig = sig % (self.out.toCPPName() if self.out else self.ret.toCPPName() , cpp_mname , argstring);
        else:
            sig = "%s %s(env_t env, value_t _self)"
            sig = sig % (self.out.toCPPName() if self.out else self.ret.toCPPName() , cpp_mname);
        funDecls.append(sig+";");
        body = "    object_t* self = val_as_object(_self);\n";
        for (n,t) in zip(range(0,len(self.args)),self.args):
            body = body + "    "+t.toNativeType()+" arg"+str(n)+" = "+t.unwrap("_arg"+str(n))+";\n";
        # Add dynamic tag tests
        # We can't use the vtable, because that doesn't handle subtyping.  Instead, use C++
        # dynamic_cast
        i = 0;
        for arg in self.args:
            if isinstance(arg,TypeName):
                name = arg.name;
                if name.endswith('&'):
                    name = name[:-1];
                body = body + "    assert(dynamic_cast<"+name+"*>(("+name+"*)val_as_object(_arg"+str(i)+")->fields[0].ptr) != NULL);\n";
            i = i + 1;
        receiver = "(("+clazz+"*)val_as_object(self)->fields[0].ptr)";
        args = (map(lambda n: ("arg"+str(n)),range(0,len(self.args))));
        if self.out:
            if (isinstance(self.out, TypeName)):
                body = body + "    " + self.out.name + "* outparam = new " + self.out.name + "();\n";
                args.append("*outparam");
            else:
                body = body + "    " + self.out.toNativeType() + " outparam;\n";
                args.append("outparam");
        call = receiver+"->"+mname+"("+", ".join(args)+");\n";
        if (not isinstance(self.ret, VoidType)):
            # return, and cast away const-ness of result...
            #call = self.ret.toCPPName()+" res = ("+self.ret.toCPPName()+")"+call;
            call = self.ret.toNativeType()+" res = ("+self.ret.toNativeType()+")"+call;
        body = body + "    " + call;
        if (self.out):
            assert (not isinstance(self.out, VoidType));
            body = body + "    return "+self.out.wrap("outparam")+";";
        elif (not isinstance(self.ret, VoidType)):
            body = body + "    return "+self.ret.wrap("res")+";";
        return '''
%s {
    %s
}
__attribute__((__aligned__(8))) _genclosure_t %s_clos = { NULL, (void*)%s };
''' % (sig , body , cpp_mname , cpp_mname);
    def unwrap(self, val_str):
        # really, we're _wrapping_ an SJS closure in a std::function; we have the standard
        # contract issue of repeated round-tripping on functions accumulating arbitrarily many
        # levels of wrapping
        argstring = (", ".join(map(lambda (n,t): (t.toCPPName()+" arg"+str(n)),zip(range(0,len(self.args)),self.args))));
        call = "INVOKE_CLOSURE("+(", ".join([val_str]+map(lambda n: self.args[n].unwrap("arg"+str(n)),range(0,len(self.args)))))+")";
        # return value marshalling
        if (not isinstance(self.ret,VoidType)):
            call = self.ret.wrap(call);
        return "[=]("+argstring+") { return "+call+"; }";
    def wrap(self, val_str):
        # TODO: alloc new SJS closure that dispatches to this c++ function...
        return "(*(int*)0x1)";



class ClassType:
    def __init__(self, name):
        self.name = name;
        self.ctor = None;
        self.methods = {};
        self.lazyMethods = [];
        self.impureVirtual = [];
        self.pureVirtual = [];
        self.js_extensible = False;
        self.casts = [];
        self.should_generate_ctor = True;

    def disableCtorGen(self):
        self.should_generate_ctor = False;

    def __str__(self):
        return "type "+self.name+"<"+str(len(self.methods))+" methods>";

    def deferMethod(self, m):
        self.lazyMethods.append(m);

    def markForJSExtension(self):
        self.js_extensible = True;

    def processDeferred(self, parseType):
        for m in self.lazyMethods:
            if (m.startswith('//')):
                continue;
            splits = m.split(':');
            if (len(splits) == 2):
                name = splits[0].strip();
                if (name.startswith("static ")):
                    name = name.split(' ', 1)[1].strip();
                if (name.startswith("virtual ")):
                    name = name.split(' ', 1)[1].strip();
                    self.impureVirtual.append(name);
                if (name.startswith("pure virtual ")):
                    name = name.split(' ', 2)[2].strip();
                    self.pureVirtual.append(name);
                tystr = splits[1].strip().split(';')[0];
                if (name == "^^constructor"):
                    self.ctor = parseType(tystr);
                else:
                    self.methods[name] = parseType(tystr);
        self.lazyMethods = [];

    def toJSON(self):
        mems = map(lambda k: { 'name': k, 'type': self.methods[k].toJSON() }, self.methods.keys());
        if self.js_extensible:
            mems = [ { 'name': "_____cpp_receiver", 'type': VoidType().toJSON() },
                     { 'name': "_____gen_cpp_proxy", 'type': VoidType().toJSON() } ] + mems;
        else:
            mems = [ { 'name': "_____cpp_receiver", 'type': VoidType().toJSON() } ] + mems;
        return { 'typefamily': 'object', 'members': mems, 'typename': self.name };
    def toCPPName(self):
        return "value_t"; #"object_t*";
    def genSelfInstall(self):
        # Note that we MUST stash at offset 0, so accessing a subtype instance pointer as a
        # supertype instance pointer works
        argstring = "";
        if self.ctor:
            argstring = (", ".join(map(lambda (n,t): t.unwrap("arg"+str(n)),zip(range(0,len(self.ctor.args)),self.ctor.args))));
        if len(self.pureVirtual) > 0:
            # TODO: self-passing + args...
            return "self->fields[0].ptr = (void*)(new SJS"+self.name+"(self));";
        return "self->fields[0].ptr = (void*)(new "+self.name+"("+argstring+"));";
    def genMethodInstall(self):
        installations = [];
        # Note that the order of this list must match the order of self.methds, which matches the
        # order of vtable fields
        mid = 2 if self.js_extensible else 1;
        for m in self.methods:
            installations.append("self->fields["+str(mid)+"].ptr = (void*)&__"+self.name+"__"+m+"_clos;");
            mid = mid + 1;
        return "\n    ".join(installations);

    def genWrapperInstallation(self):
        if not self.js_extensible:
            return "";
        return "self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_"+self.name+");\n";

    def genWrapperMethodDecls(self):
        assert self.js_extensible;
        code = "        ";
        for m in self.methods:
            # TODO: tear out this awful hack, and just build a method name to prefix map during parsing
            isoverride = False;
            for f in self.pureVirtual+self.impureVirtual:
                if f == m:
                    isoverride = True;
            meth = self.methods[m];
            code = code +"        "+("virtual " if isoverride else "")+((meth.ret.toNativeType())+" "+m+"("+(", ".join(map(lambda t: t.toNativeType(), meth.args)))+");\n");
        return code;
    def genWrapperMethodImpls(self):
        assert self.js_extensible;
        code = '''
        SJS%(name)s::SJS%(name)s(object_t* o) {
            this->___sjs_obj = o;
        }\n''' % { "name": self.name };
        for m in self.methods:
            meth = self.methods[m];
            param_names = map(lambda i: "arg"+str(i), range(0,len(meth.args)));
            formals = ", ".join(map(lambda (t,n): t.toNativeType()+" "+n, zip(meth.args, param_names)));
            xlated = (map(lambda (t,n): t.wrap(n), zip(meth.args, param_names)));
            xlated_str = ", ".join(["object_as_val(this->___sjs_obj)"]+xlated);
            code = code + ((meth.ret.toNativeType())+" SJS"+self.name+"::"+m+"("+formals+"){\n");
            # TODO
            code = code + "        ";
            code = code + '''wprintf(L"invoking SJS%(name)s::%(m)s\\n");\n''' % { 'name': self.name, 'm' : m };
            code = code + "        ";
            if not isinstance(self.methods[m].ret, VoidType):
                # TODO: should this be unwrap?
                code = code + "return "+(self.methods[m].ret.unwrap("(("+self.methods[m].toCPPName()+")val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_"+m+")))->invoke("+xlated_str+")"));
                code = code + ";\n";
            else:
                code = code + "(("+self.methods[m].toCPPName()+")val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_"+m+")))->invoke("+xlated_str+");\n";
            code = code + ("}\n");
        return code;

    def genWrapperClass(self):
        assert self.js_extensible;
        return '''        class SJS%(name)s : public %(name)s {
        private:
            object_t* ___sjs_obj;
        public:
            SJS%(name)s(object_t* o);
            %(methods)s
        };\n''' % { "name": self.name, "methods": self.genWrapperMethodDecls() } + self.genWrapperMethodImpls();

    def genWrapper(self):
        if not self.js_extensible:
            return "";
        # TODO: generate subclass....
        wrapperclass = self.genWrapperClass();
        return wrapperclass+'''
        void* __sjs_cpp_wrap_%(name)s(object_t* o) {
            void* p = new SJS%(name)s(o);
            wprintf(L"Wrapped SJS native object %%p with SJS%(name)s instance at %%p\\n", o, p);
            return p;
        }
        ''' % { "name": self.name };

    def genCasts(self):
        # Note that we use the target type for wrapping and unwrapping, which (1) makes the C++ instance pointer pass to the init_wrap function type check, and (2) is safe because all c++ wrappers store the instance in field 0.
        casts = map(lambda src: "value_t __%(name)s_of_%(src)s_code(env_t env, value_t dummy, value_t o) {\n    assert(dynamic_cast<%(name)s*>((%(name)s*)o.obj->fields[0].ptr) != NULL);\n    return %(castexpr)s;\n}" % { "name": self.name, "src" : src if not src.endswith("*") else src[:-1] , "castexpr": self.wrap(self.unwrap("o"))}, self.casts);
        closures = map(lambda src: "__attribute__((__aligned__(8))) _genclosure_t __%(name)s_of_%(src)s_clos = { NULL, (void*)__%(name)s_of_%(src)s_code };\nextern \"C\" { value_t __%(name)s_of_%(src)s_box = { &__%(name)s_of_%(src)s_clos };\nvalue_t* __%(name)s_of_%(src)s = &__%(name)s_of_%(src)s_box; }" % { "name": self.name, "src" : src }, self.casts)
        return "\n".join(casts + closures);
    def genCastJSON(self):
        decls = map(lambda src: { "name": self.name+"_of_"+src, "type": mkCastTypeJSON(self.name, src)}, self.casts);
        return decls;
    def genCastLinkage(self):
        decls = map(lambda src: { "name": "__"+self.name+"_of_"+src, "boxed": True }, self.casts);
        return decls;

    def getCPP(self):
        # TODO: if self.ctor != None, generate the appropriate constructor instead of the default
        classDecls.append("class "+self.name+";");
        funDecls.append("object_t* __init_wrap_"+self.name+"(object_t* self, "+self.name+"* o);");
        #if (self.ctor and len(self.ctor.args) > 0):
        #    funDecls.append("object_t* __"+self.name+"(env_t, object_t*, "+(", ".join(map(lambda a: a.toCPPName(), self.ctor.args)))+");");
        #else:
        #    funDecls.append("object_t* __"+self.name+"(env_t, object_t*);");
        funDecls.append("extern int __link_vtbl_"+self.name+"[];");
        argstring = "";
        if self.ctor:
            argstring = (", ".join([""]+map(lambda (n,t): (t.toCPPName()+" arg"+str(n)),zip(range(0,len(self.ctor.args)),self.ctor.args))));
        #if not self.should_generate_ctor:
        #    return self.genWrapper() + self.genCasts() + self.getMethods();
        return self.genWrapper() + self.genCasts() + '''
%(methods)s
object_t* __init_wrap_%(name)s(object_t* self, %(name)s* o) {
    assert(__link_vtbl_%(name)s != NULL);
    self->vtbl = (object_map)&__link_vtbl_%(name)s; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    %(wrapinstall)s
    %(minstall)s
    return self;
}
value_t __%(name)s_code(env_t env, value_t _self %(argstring)s) {
    object_t* self = _self.obj;
    assert(__link_vtbl_%(name)s != NULL);
    self->vtbl = (object_map)&__link_vtbl_%(name)s; // Work around linker issue
    %(selfinstall)s
    %(wrapinstall)s
    %(minstall)s
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __%(name)s_clos = { NULL, (void*)__%(name)s_code };
__attribute__((__aligned__(8))) value_t __%(name)s_box = { (void*)&__%(name)s_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __%(name)s = &__%(name)s_box;
''' % { "name" : self.name, "methods": self.getMethods(), "minstall": self.genMethodInstall(), "selfinstall": self.genSelfInstall() if self.should_generate_ctor else "\n",
        "argstring": argstring, "wrapinstall": self.genWrapperInstallation() };
    def getMethods(self):
        ms = "\n\n".join(map(lambda m: self.methods[m].getCPP(m, self.name, len(self.methods.keys())), self.methods.keys()));
        return ms;
    def unwrap(self, val_str):
        # val_str is the C++ representation of an SJS runtime entity, here an object_t*
        return "((%(clazz)s*)(val_as_object(%(exp)s)->fields[0].ptr))" % { 'clazz': self.name, 'exp': val_str, 'self_offset':
                        len(namedTypes[self.name].methods) };
    def wrap(self, val_str):
        #return "__init_wrap_"+self.name+"((object_t*)new "+self.name+"(), "+val_str+")";
        # TODO: prefix for byref?
        return "object_as_val(__init_wrap_"+self.name+"((object_t*)new cppobj<"+str(len(self.methods)+1)+",0>(), "+val_str+"))";

def mkCastTypeJSON(dst, src):
    return { "typefamily": "function", "args": [ { "name": "x", "type": TypeName(src).toJSON() } ], "return": TypeName(dst).toJSON() };

def process(basename, files, parseType):
    # (lazily, partially) parse each .tidl file
    for file in files:
        if (not file.endswith('.tidl')):
            print "ERROR: "+file+" is not .tidl\n";
            sys.exit(1);
        hdl = open(file, 'r');
        # Read ALL lines, assuming interface files are fairly small
        lines = hdl.readlines();
        hdl.close();

        decl = lines[0];
        lines = lines[1:];

        if (not decl.startswith('declare class ')):
            print "invalid class decl start: "+decl;
            sys.exit(1);

        name = decl.split(' ')[2];
        clazz = ClassType(name);
        for line in lines:
            l2 = line.strip(); # trim
            if (l2 == '}'):
                continue;
            if (len(l2) > 0):
                clazz.deferMethod(l2);
        namedTypes[name] = clazz;


    # now that we have all the top level names, reify the method types
    for k in namedTypes.keys():
        namedTypes[k].processDeferred(parseType);

    export(basename, namedTypes);

def export(basename, namedTypes):
    # dump JSON for type decls
    import json
    js = open(basename+".json", "w");
    keys = namedTypes.keys();
    #js.write("[\n"+",\n".join(map(lambda k: "{ name: '"+k+"', type: { typefamily: 'constructor', args: [], return: "+namedTypes[k].toJSONString()+" } }",keys))+"\n]");
    ctors = map(lambda k: { "name": k, "type": { "typefamily": "constructor", "args": map(lambda arg: { "name": "x", "type": arg.toJSON() }, namedTypes[k].ctor.args) if namedTypes[k].ctor else [], "return": namedTypes[k].toJSON() } }, keys);
    casts = reduce(lambda x,y: x+y, map(lambda k: namedTypes[k].genCastJSON(), namedTypes.keys()), []);
    mems = ctors + [{"name": "casts", "type": { "typefamily": "object", "members": casts } }];
    tizen_obj = { "name": "TizenLib", "type": { "typefamily": "object", "members": mems } };
    # TODO: bake this into the IDL
    global_hacks = [ { "name": "__platform_return", "type": { "typefamily": "function", "args": [ { "name": "asdf", "type": { "typefamily": "name", "name": "Application" } } ], "return": { "typefamily": "void" } } } ]
    js.write(json.dumps([tizen_obj]+global_hacks, indent=4));
    js.close();

    # dump some C++
    cpp = open(basename+".cpp", "w");
    # gcc4.{4,5} bug workaround:
    cpp.write("// XXX Next line a workaround for gcc4.{4,5} C++11 bug: https://llvm.org/bugs/show_bug.cgi?id=13364\n");
    cpp.write("namespace std { struct type_info; }\n");
    cpp.write("#include \"runtime.h\"\n");
    cpp.write("#include \"linkage.h\"\n");
    cpp.write("#include \"calc.h\"\n"); # TODO: remove hack
    #cpp.write("#include \"/Users/colin.gordon/research/sjs-main/sjsc/idl/tizenstr.h\"\n"); # TODO: remove hack
    cpp.write("\n".join(includes));
    cpp.write("typedef struct { env_t env; void* func; } _genclosure_t;\n");
    code = []
    for k in keys:
        code.append(namedTypes[k].getCPP());
    # cpp.write("\n".join(classDecls));
    cpp.write("\n");
    cpp.write("\n".join(funDecls));
    cpp.write("\n");
    cpp.write("\n".join(code));
    cpp.write("object_t* __platform_return_val = NULL;\n");
    cpp.write('''
    void __platform_return_code(env_t env, value_t dummy, value_t val) {
        __platform_return_val = val.obj;
    }
    __attribute__((__aligned__(8))) _genclosure_t __platform_return_clos = { NULL, (void*)__platform_return_code };
    __attribute__((__aligned__(8))) value_t __platform_return_box = { (void*)&__platform_return_clos };
    __attribute__((__aligned__(8))) value_t* __platform_return = &__platform_return_box;
''');
    cpp.write("\n");
    cpp.write("cppobj<%d,0> __cast_obj = { .vtbl = (object_map)&__cast_obj_vtbl, .__proto__ = NULL, .fields = {%s} };\n" %
            (len(casts), ", ".join(map(lambda kjson: "(void*)&__"+kjson["name"]+"_clos", casts))) );
    cpp.write("cppobj<%d,0> __TizenLib = { .vtbl = (object_map)&__TizenLib_vtbl, .__proto__ = NULL, .fields = {%s} };\n" %
                (len(namedTypes.keys())+1, (", ".join(map(lambda k: "(void*)&__"+k+"_clos", namedTypes.keys())+["(void*)&__cast_obj"])) ));
    cpp.write("__attribute__((__aligned__(8))) value_t __TizenLib_box = { (void*)&__TizenLib };\n");
    cpp.write("__attribute__((__aligned__(8))) value_t* TizenLib = & __TizenLib_box;\n");
    cpp.write("\n");
    cpp.write("".join(inline_glue_code)); # Note these strings are pulled in from reading a file, and already contain trailing newlines
    cpp.close();

    # dump linkage
    link = open(basename+".linkage.json", "w");
    # TODO: remove hack
    exec_hack = [ { "name": "__platform_return", 'boxed': True } ];
    # TODO: linkage for casts
    # map(lambda x: { 'name': "__"+x, 'boxed': True }, keys) + reduce(lambda x,y: x+y, map(lambda x: namedTypes[x].genCastLinkage(), filter(lambda k: len(namedTypes[k].casts) > 0, keys)), [])
    tizen_indirect = [{ "name": "__TizenLib_vtbl", "fields": namedTypes.keys()+["casts"] }, {"name": "__cast_obj_vtbl", "fields": map(lambda kj: kj["name"], casts)}];
    linkage = { 'globals': exec_hack + [{ "name": "TizenLib", "boxed": True }],
                'indirections':
                    map(lambda x: { 'name': "__link_vtbl_"+x,
                                    'fields': (["_____cpp_receiver","_____gen_cpp_proxy"] if namedTypes[x].js_extensible else ["_____cpp_receiver"])+namedTypes[x].methods.keys() },
                        keys) + tizen_indirect };
    link.write(json.dumps(linkage, indent=4));
    link.close();

