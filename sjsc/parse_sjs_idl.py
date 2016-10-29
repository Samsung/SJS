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

import sys;

from parseTJSTIDLs import inline_glue_code, includes, namedTypes, EnumType, UnsignedIntegerType, TypeName, StringType, IntegerType, BooleanType, DoubleType, VoidType, ArrayType, AttachedMethodType, ClassType, export


def parseType(tystr):
    if tystr == None:
        return None;
    assert (not tystr.isspace());
    assert (not tystr == "");
    assert (not tystr[0] == ' ');
    if(tystr == 'string'):
        return StringType();
    elif (tystr == 'double'):
        return DoubleType();
    elif (tystr == 'int'):
        return IntegerType();
    elif (tystr == 'uint'):
        return UnsignedIntegerType();
    elif (tystr == 'bool'):
        return BooleanType();
    elif (tystr == 'void'):
        return VoidType();
    elif (tystr.startswith('enum ')):
        return EnumType(tystr[5:]);
    #elif (tystr.startswith("Array<") and tystr.endswith(">")):
    #    # array
    #    return ArrayType(parseType(tystr[6:-1]));
    elif (tystr.startswith('[](')):
        # method, in the TJS TIDL format
        i = 3;
        unbalanced = 1;
        # find closing argument paren
        # for SJS IDL, this is currently overkill since we currently can't nest parens in the IDL
        while (i < len(tystr)):
            if (tystr[i] == ')'):
                unbalanced = unbalanced - 1;
                if unbalanced == 0:
                    break;
            elif (tystr[i] == '('):
                unbalanced = unbalanced + 1;
            i = i + 1;
        assert tystr[i] == ')';
        # args range is tystr[1:i]
        arr = tystr.find("->",i);
        assert arr > i;
        returnType = parseType(tystr[arr+2:].strip());
        (args, out) = parseArgumentTypes(tystr[3:i]);
        if len(args) == 1 and isinstance(args[0],VoidType):
            args = []
        mtype = AttachedMethodType(args, returnType);
        mtype.out = out;
        return mtype;
    else:
        return TypeName(tystr);

def parseArgumentTypes(argstr):
    args = filter(lambda a: not a == "",map(lambda s: s.strip(), argstr.split(",")));
    result = [];
    out = None;
    for arg in args:
        if (arg.startswith('out ')):
            out = arg.split(' ')[1];
        else:
            result.append(arg);
    return (map(parseType, result), parseType(out));

def process(basename, files, parseType):
    # (lazily, partially) parse each .idl file
    for file in files:
        if (not (file.endswith('.idl') or file.endswith('.ts'))):
            print "ERROR: "+file+" is not .idl\n";
            sys.exit(1);
        hdl = open(file, 'r');
        # Read ALL lines, assuming interface files are fairly small
        lines = hdl.readlines();
        hdl.close();

        parseDecls(lines);

def parseDecls(lines):

    while (len(lines) > 0):
        if (lines[0].startswith('class ')):
            lines = parseClass(lines);
        elif (lines[0].startswith('#include')):
            includes.append(lines[0]);
            lines = lines[1:];
        elif (lines[0].startswith('using namespace ')):
            includes.append(lines[0]);
            lines = lines[1:];
        elif(lines[0].startswith("inline:")):
            inline_glue_code.extend(lines[1:]);
            lines = [];
        else:
            trimmed = lines[0].strip();
            if (trimmed.startswith('//') or trimmed == ""):
                # comment line or blank
                lines = lines[1:];
            else:
                print "ERROR: Bad decl start: "+lines[0];
                assert(False);

    # now that we have all the top level names, reify the method types
    for k in namedTypes.keys():
        namedTypes[k].processDeferred(parseType);

    export(basename, namedTypes);

def parseClass(lines):

    decl = lines[0];
    lines = lines[1:];

    if (not decl.startswith('class ')):
        print "invalid class decl start: "+decl;
        sys.exit(1);

    name = decl.split(' ')[1];
    clazz = ClassType(name);
    while (len(lines) > 0):
        line = lines[0];
        lines = lines[1:];
        l2 = line.strip(); # trim
        if (l2 == '}'):
            break;
        if (l2 == "noconstructor;"):
            clazz.disableCtorGen();
            continue;
        if (l2 == "deriving Wrappable;"):
            clazz.markForJSExtension();
            continue;
        if (l2.startswith("cast from ")):
            clazz.casts = map(lambda s: s.strip(), l2[10:][:-1].split(","));
            continue;
        if (len(l2) > 0):
            clazz.deferMethod(l2);
    namedTypes[name] = clazz;
    return lines;

### BEGIN SCRIPT
if __name__ == "__main__":
    basename = sys.argv[1];
    files = sys.argv[2:];
    if (len(sys.argv) < 3):
        print "Usage: %s <basename> <.idl file>*";
        print "First argument is the base name for the .json and .cpp files generated from the .tidl files.";
        sys.exit(1);
    process(basename, files, parseType);
