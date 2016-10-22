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

public final class FFILinkage extends HashMap<String,FFILinkage.LinkEntry> {

    public final static class LinkEntry {
        public final String name;
        public final boolean boxed;
        public final boolean untyped_import;
        public LinkEntry(String n, boolean box, boolean untyped) {
            name = n;
            boxed = box;
            untyped_import = untyped;
        }
    }

    private Map<String,List<String>> tables_to_generate;

    public FFILinkage() {
        super();
        tables_to_generate = new HashMap<>();
    }

    public void includeFile(InputStream is) {
        JsonParser p = new JsonParser();
        JsonObject linkage = p.parse(new InputStreamReader(is)).getAsJsonObject();
        includeLinkage(linkage);
    }
    public void includeFile(Path filename) throws IOException {
        JsonParser p = new JsonParser();
        JsonObject linkage = p.parse(Files.newBufferedReader(filename)).getAsJsonObject();
        includeLinkage(linkage);
    }
    public void includeLinkage(JsonObject linkage) {
        JsonArray decls = linkage.getAsJsonArray("globals");
        for (JsonElement d : decls) {
            parseDecl(d.getAsJsonObject());
        }
        JsonArray tables = linkage.getAsJsonArray("indirections");
        for (JsonElement t : tables) {
            parseTable(t.getAsJsonObject());
        }
    }

    public void parseDecl(JsonObject decl) {
        String name = decl.getAsJsonPrimitive("name").getAsString();
        boolean boxed = decl.getAsJsonPrimitive("boxed").getAsBoolean();
        if (decl.has("rewrite")) {
            System.err.println("Ignoring rewrite of top-level ["+name+"] into: "+decl.getAsJsonPrimitive("rewrite").getAsString());
        }
        boolean untyped = decl.has("untyped") && decl.getAsJsonPrimitive("untyped").getAsBoolean();
        LinkEntry l = new LinkEntry(name, boxed, untyped);
        put(name, l);
    }

    public void parseTable(JsonObject table) {
        String name = table.getAsJsonPrimitive("name").getAsString();
        JsonArray arr = table.getAsJsonArray("fields");
        LinkedList<String> l = new LinkedList<>();
        for (JsonElement fname : arr) {
            l.add(fname.getAsJsonPrimitive().getAsString());
        }
        tables_to_generate.put(name, l);
    }

    public Set<Map.Entry<String,List<String>>> getTablesToGenerate() {
        return tables_to_generate.entrySet();
    }
}
