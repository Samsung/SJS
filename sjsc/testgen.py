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

## Enumerate a wide range of possible coercions, and check that each coercion attempt behaves
## according to the subtype hierarchy.  Currently not exhaustive.

import json
import sys
from parseTJSTIDLs import inline_glue_code, includes, namedTypes, EnumType, UnsignedIntegerType, TypeName, StringType, IntegerType, BooleanType, DoubleType, VoidType, ArrayType, AttachedMethodType, ClassType, export
import random
import os
import subprocess

fields = [ "foo", "bar", "baz" ]

# TODO: float (encoding)
# TODO: string (tagging)
# TODO: constructor
# TODO: methods (JSON currently only lets us specify *attached* methods, but it only makes sense to
# pass *unattached* methods to through the test harness
tycons = [ "int", "bool", "void", "obj" ]#, "array"] #, "fun", "meth", "obj" ]

class FunctionType:
    def __init__(self, args, ret):
        self.ret = ret;
        self.args = args;
    def toJSON(self):
        return { "typefamily": "function",
                 "args": map(lambda t: { 'name': "x", 'type': t.toJSON() }, self.args),
                 "return": self.ret.toJSON() }
    def randomInstance(self):
        if isinstance(self.ret, VoidType):
            return "function(x){ }"
        else:
            return "function(x) { return " + self.ret.randomInstance() + "; }"

class ObjectType:
    def __str__(self):
        return "{ "+", ".join(map(lambda (f,t): f+":"+str(t), self.fields))+" }"
    def __init__(self):
        self.fields = []
    def addField(self, name, ty):
        self.fields.append((name,ty))
    def toJSON(self):
        return { "typefamily": "object",
                 "members": map(lambda (f,t): { "name": f, "type": t.toJSON() }, self.fields) }
    def randomInstance(self):
        if random.randint(0,3) == 0:
            return "null";
        else:
            return "{ "+", ".join(map(lambda (f,t): f+":"+t.randomInstance(), self.fields))+" }"


class MapType:
    def __str__(self):
        return "map<"+str(self.elem)+">"
    def __init__(self, elemtype):
        self.elem = elemtype
    def toJSON(self):
        return { "typefamily": "map",
                 "elemtype": self.elem.toJSON() }
    def randomInstance(self):
        if random.randint(0,3) == 0:
            return "null";
        else:
            # Okay, this isn't really random
            return "{ \"fake\": "+self.elem.randomInstance()+" }"


# TODO: add depth parameter
def genRandomType(voidokay):
    index = random.randint(0,len(tycons)-1-1) ## TODO: temporarily don't generate nested object types
    con = tycons[index]
    if (con == "int"):
        return IntegerType()
    elif (con == "bool"):
        return BooleanType()
    elif (con == "void"):
        if voidokay:
            return VoidType()
        else:
            return genRandomType(False)
    elif (con == "array"):
        return ArrayType(genRandomType(False))
    elif (con == "obj"):
        o = ObjectType()
        for f in fields:
            if random.randint(0,1) == 0:
                o.addField(f, genRandomType(False))
        # If we don't do this, we'll generate the empty object literal... which is inferred as a map
        if (len(o.fields) == 0):
            o.addField("fake", IntegerType())
        return o
    elif (con == "meth"):
        assert False
    elif (con == "fun"):
        assert False

def gen(con):
    if (con == "int"):
        return IntegerType()
    elif (con == "bool"):
        return BooleanType()
    elif (con == "void"):
        return VoidType()
    elif (con == "array"):
        return ArrayType(genRandomType(False))
    elif (con == "obj"):
        o = ObjectType()
        for f in fields:
            if random.randint(0,1) == 0:
                o.addField(f, genRandomType(False))
        # If we don't do this, we'll generate the empty object literal... which is inferred as a map
        o.addField("fake", IntegerType())
        return o
    elif (con == "meth"):
        assert False
    elif (con == "fun"):
        assert False

def compileIt(tout,tin):
    devnull = open("/dev/null", "w")
    compiler_ret = subprocess.call("./sjsc untyped_gen.js --extra-decls untyped_env_gen.json --extra-objs untyped_blob.c --native-libs untyped_linkage.json -debugcompiler".split(), stdout=devnull, stderr=devnull)
    if compiler_ret != 0:
        print "SJS COMPILATION ERROR for ["+str(tin)+" -> "+str(tout)+"]:"
        cat = subprocess.Popen(["cat", "untyped_env_gen.json"])
        cat.wait()
        exit(1)

def writeTest(tin):
    hdl = open("untyped_gen.js", "w")
    hdl.write("garbage("+tin.randomInstance()+");")
    hdl.close()

def writeJSON(tout, tin):
    hdl = open("untyped_env_gen.json", "w")
    hdl.write(json.dumps([{"name": "garbage", 
                           "type": { "typefamily": "function",
                                     "args": [ { "name": "x", "type": tin.toJSON()} ],
                                     "return": tout.toJSON() } }], indent=4))
    hdl.close()


def subtype_of(sub, sup):

    if str(sub) == str(sup):
        return True
    else:
        if (isinstance(sub, ObjectType) and isinstance(sup, ObjectType)):
            subfields = {}
            for (f,t) in sub.fields:
                subfields[f] = t
            for (f,t) in sup.fields:
                if subfields.has_key(f) and str(t) != str(subfields[f]):
                    return False
            # Each supertype field exists at the same type in the subtype
            return True
        return False # TODO: fix arrays, functions, ...






def runIt(tout,tin):
    # In theory, subprocess.call defaults to stderr = None, which should not display anything.
    # That's how it works for stdout.  But in practice the default stdout=None has the desired
    # behavior and stderr still shows up on screen, so we see C assertion failures interleaved with
    # test results
    devnull = open("/dev/null", "w")
    result = subprocess.call(["./a.out"], stderr = devnull)
    expected = subtype_of(tin,tout)
    prefix = "Testing ["+str(tin)+" -> "+str(tout)+"]: "
    if expected and result == 0:
        print prefix+"PASS"
    elif not expected and result != 0:
        print prefix+"PASS (PROBABLE FAILSTOP)"
    else:
        print prefix+"FAIL"
    devnull.close()

def runTest(tout, tin):
    writeJSON(tout,tin)
    writeTest(tin)
    compileIt(tout,tin)
    runIt(tout,tin)




if __name__ == "__main__":
    random.seed()

    ### systematically explore coercions between various base kinds
    #for output in tycons:
    #    ty_out = gen(output)
    #    for arg in tycons:
    #        if arg == "void":
    #            continue
    #        ty_in = gen(arg);
    #        runTest(ty_out, ty_in)

    ## Also make sure we test identity and trivial subtype coercions for object types
    ## Currently, we'll always throw an extra field in there to avoid generating empty map literals,
    ## so we're testing subtyping.
    #objty = gen("obj")
    sup = ObjectType()
    sup.addField("fake", IntegerType())
    #runTest(objty, objty)
    #runTest(sup, objty)

    # Until it gets worked into the systematic process, here's one way to test dynamic object
    # coercion: maps.  Currently, maps lack a vtable.  So as long as we want to create an object
    # with fields of only one type, we can coerce a map to a fixed-layout object!  Subsequent uses
    # of the map from the SJS side would of course do weird stuff, but this is enough to test the
    # basics
    # TODO: Can we actually parse maps in the environment file?  I don't think that's implemented.

    mtype = MapType(IntegerType())
    runTest(sup, mtype)



