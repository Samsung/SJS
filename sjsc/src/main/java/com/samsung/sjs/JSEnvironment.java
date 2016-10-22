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
/**
 * A class to collect the bindings of the global environment from one or more descriptor files.
 *
 * @author colin.gordon
 */
package com.samsung.sjs;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.samsung.sjs.types.*;

public final class JSEnvironment extends HashMap<String,Type> {

    public final Map<String,ObjectType> namedTypes;

    public JSEnvironment() {
        super();
        intrinsics = new HashMap<String,List<Property>>();
        overloads = new HashMap<>();
        namedTypes = new HashMap<>();
    }

    private Map<String,List<Property>> intrinsics;
    private Map<String,List<Type>> overloads;

    public List<Property> getIntrinsicProperties(Type pt) {
        if (pt instanceof IntegerType) {
            return intrinsics.get("int");
        } else if (pt instanceof FloatType) {
            return intrinsics.get("double");
        } else {
            throw new IllegalArgumentException(pt.toString());
        }
    }

    public void includeFile(InputStream is) {
        JsonParser p = new JsonParser();
        JsonArray decls = p.parse(new InputStreamReader(is)).getAsJsonArray();
        for (JsonElement d : decls) {
            parseDecl(d.getAsJsonObject());
        }
    }

    public void includeFile(Path filename) throws IOException {
        JsonParser p = new JsonParser();
        JsonArray decls = p.parse(Files.newBufferedReader(filename)).getAsJsonArray();
        for (JsonElement d : decls) {
            parseDecl(d.getAsJsonObject());
        }
    }

    public void parseDecl(JsonObject decl) {
        String name = decl.getAsJsonPrimitive("name").getAsString();
        if (decl.getAsJsonPrimitive("intrinsic") != null) {
            assert (decl.getAsJsonPrimitive("intrinsic").getAsBoolean());
            parseIntrinsic(name, decl.getAsJsonObject("type"));
        } else {
            Type t = parseType(decl.getAsJsonObject("type"));
            put(name, t);
            if (t instanceof ConstructorType) {
                namedTypes.put(name, (ObjectType)((ConstructorType)t).returnType());
            }
        }
    }

    public List<Type> getOverloads(String name) {
        return overloads.get(name);
    }

    public void parseIntrinsic(String name, JsonObject ty) {
        List<Property> props = new LinkedList<Property>();
        for (JsonElement e : ty.getAsJsonArray("operators")) {
            JsonObject eo = e.getAsJsonObject();
            props.add(parseProp(eo));
        }
        assert (intrinsics.get(name) == null);
        intrinsics.put(name, props);
        // TODO: Verify it's an expected intrinsic
    }

    public Type parseType(JsonObject ty) {
        assert (ty != null);
        String typefamily = ty.getAsJsonPrimitive("typefamily").getAsString();
        switch (typefamily) {
            case "string": return Types.mkString();
            case "int": return Types.mkInt();
            case "double": return Types.mkFloat();
            case "void": return Types.mkVoid();
            case "bool": return Types.mkBool();
            case "array":
                Type etype = parseType(ty.getAsJsonObject("elemtype"));
                return Types.mkArray(etype);
            case "map":
                Type mtype = parseType(ty.getAsJsonObject("elemtype"));
                return Types.mkMap(mtype);
            case "constructor":
            case "function":
            case "method":
                final Type ret = parseType(ty.getAsJsonObject("return"));
                final List<String> names = new LinkedList<String>();
                final List<Type> types = new LinkedList<Type>();
                for (JsonElement e : ty.getAsJsonArray("args")) {
                    JsonObject eo = e.getAsJsonObject();
                    names.add(eo.getAsJsonPrimitive("name").getAsString());
                    types.add(parseType(eo.getAsJsonObject("type")));
                }
                if (typefamily.equals("function")) {
                    return Types.mkFunc(ret, types, names);
                } else if (typefamily.equals("constructor")) {
                    return Types.mkCtor(types, names, ret, null);
                } else {
                    // method
                    assert (typefamily.equals("method"));
                    return Types.mkMethod(Types.mkAny(), ret, names, types); // TODO: Why does AttachedMethodType have a receiver?
                }
            case "object":
                final List<Property> props = new LinkedList<Property>();
                for (JsonElement e : ty.getAsJsonArray("members")) {
                    JsonObject eo = e.getAsJsonObject();
                    props.add(parseProp(eo));
                }
                ObjectType o =  Types.mkObject(props);
                if (ty.has("typename")) {
                    namedTypes.put(ty.getAsJsonPrimitive("typename").getAsString(), o);
                }
                return o;

            case "intersection":
                List<Type> mems = new LinkedList<>();
                for (JsonElement e : ty.getAsJsonArray("members")) {
                    JsonObject eo = e.getAsJsonObject();
                    mems.add(parseType(eo));
                }
                return new IntersectionType(mems);
            case "typevariable":
                int x = ty.getAsJsonPrimitive("id").getAsInt();
                return new TypeVariable(x);
            case "name":
                String name = ty.getAsJsonPrimitive("name").getAsString();
                return new NamedObjectType(name, this);
            default:
                throw new IllegalArgumentException(typefamily);
        }
    }

    public Property parseProp(JsonObject p) {
        String name = p.getAsJsonPrimitive("name").getAsString();
        return Types.mkProperty(name, parseType(p.getAsJsonObject("type")));
    }
}

