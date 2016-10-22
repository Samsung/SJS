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
 * Class to manage functionality related to module imports:
 *  - Locating source for imported modules
 *  - Locating type declarations for imported modules
 *  - Running sjsc/jscomp on imported modules
 *  etc.
 *
 *  This class essentially implements its own very naive build system
 *
 *  TODO: Need to use lockfiles to detect cyclic dependencies, orchestrate waiting for DAG
 *  dependencies
 *  TODO: Need to export a method to record the type of the exports object when running sjsc in
 *  module mode
 *
 * @author colin.gordon
 */

package com.samsung.sjs;

import com.samsung.sjs.types.Type;

import java.io.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

public class ModuleSystem {

    private CompilerOptions opts;
    private LinkedList<String> dynamic_js_modules;
    private LinkedList<String> static_js_modules;
    private LinkedList<String> modnames;
    private Map<String,Type> modtypes;

    private final static String TYPE_ANNO_SUFFIX = ".json";
    private final static String GENERATED_TYPE_ANNO_SUFFIX = ".gentypes";
    private final static String JS_SUFFIX = ".js";

    public ModuleSystem(CompilerOptions opts) {
        this.opts = opts;
        this.dynamic_js_modules = new LinkedList<>();
        this.static_js_modules = new LinkedList<>();
        this.modnames = new LinkedList<>();
        this.modtypes = new HashMap<>();
    }

    public Map<String,Type> moduleTypes() { return modtypes; }

    public Type loadModule(String basepath) {
        // Interpret 'basepath' relative to the location of the source file for now
        File src = new File(opts.getInputFileName());
        File srcdir = src.getParentFile();
        if (srcdir != null) {
            System.err.println("Looking for module declarations in "+srcdir);
            basepath = srcdir + "/" + basepath;
        }
        JSEnvironment env = new JSEnvironment();
        try {
            env.includeFile(openTypeAnnotations(basepath));
        } catch (IOException e) {
            assert(false);
            System.err.println("Race: Checked existence of module import file, then opening failed!");
            System.exit(1);
        }
        Type modtype = env.get("exports");
        modtypes.put(new File(basepath).getName(), modtype);
        return modtype;
    }

    File locateFile(String file) {
        // TODO: protect against directory traversals
        // TODO: Use a proper search path
        File f = new File(file);
        if (!f.exists()) {
            return null;
        }
        return f;
    }

    File importSJSCode(String basepath) throws IOException {
        File gen_annos = locateFile(basepath+GENERATED_TYPE_ANNO_SUFFIX);
        File source = locateFile(basepath+JS_SUFFIX);

        static_js_modules.add(basepath);

        if (source == null) {
            if (gen_annos == null) {
                return null;
            } else {
                throw new IllegalArgumentException("Found generated type decls, but no source for module: "+basepath);
            }
        }
        if (gen_annos == null || gen_annos.lastModified() < source.lastModified()) {
            // Run the SJS compiler...
            // TODO: Pass the flag for module mode, strip things like -o while leaving things like
            // -O3 and --externl-decls
            Compiler.compile(opts, false, true);
            gen_annos = locateFile(basepath+GENERATED_TYPE_ANNO_SUFFIX);
            if (gen_annos == null) {
                throw new IllegalArgumentException("Failed to compile (likely) SJS module at: "+basepath);
            }
        }
        // We've either compiled the module or found up-to-date previously-generated annotations
        return gen_annos;
    }

    FileInputStream openTypeAnnotations(String basepath) throws IOException {
        // Might need to *generate* type annotations for SJS code eventually
        // We'll note generated type annotations by a different extension

        // add to list of module bases
        modnames.add(new File(basepath).getName());
        File manual_annos = locateFile(basepath+TYPE_ANNO_SUFFIX);
        if (manual_annos != null) {
            dynamic_js_modules.add(basepath);
            return new FileInputStream(manual_annos);
        }
        // No manual annotations found, need to look for typed SJS code
        File annos = importSJSCode(basepath);
        if (annos == null) {
            throw new IllegalArgumentException("Couldn't find type decls or SJS code for module ["+basepath+"]");
        }
        return new FileInputStream(annos);
    }

    public Collection<String> getModuleLoadCalls() {
        LinkedList<String> list = new LinkedList<>();
        for (String modname : modnames) {
            // TODO: update when hashing basename into export linkage
            list.add("__untyped_import_"+modname);
        }
        return list;
    }
}
