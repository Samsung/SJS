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
 * Compiler options object
 *
 * @author colin.gordon
 */
package com.samsung.sjs;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public final class CompilerOptions {

    private final String jssource;
    private final String outputc;
    private final boolean debug;
    private final Platform plat;
    private final JSEnvironment env;
    private final MMScheme gc;
    private final String clang_path;
    private final String emcc_path;
    private final String execname;
    private final String basedir;
    private final boolean inherit_io;
    private final boolean field_opts;
    private final LinkedList<Path> extra_decls;
    private final LinkedList<Path> extra_links;
    private final boolean dumpConstraints;
    private final boolean dumpConstraintSolution;
    private boolean guest;
    private final String runtime_src;
    private String extpath;
    private final boolean encoded_vals;
    private boolean x32;
    private boolean interop;
    private final boolean oldExpl;
    private final String explanationStrategy;
    private boolean efl;
    private boolean start_in_interop;
    private final int coptlevel;

    public enum Platform { Native, Web };

    public enum MMScheme {
        GC ("USE_GC"), NoGC ("LEAK_MEMORY");
        private final String directive;
        MMScheme(String s) { directive = s; }
        public String directiveName() { return directive; }
    }


    public CompilerOptions(Platform p,
                           String jssource,
                           boolean debug,
                           String outputc,
                           boolean use_gc,
                           String clang_path,
                           String emcc_path,
                           String execname,
                           String basedir,
                           boolean inherit_io,
                           boolean fopts,
                           boolean dumpConstraints,
                           boolean dumpConstraintSol,
                           String runtime_src,
                           boolean encoded_vals,
                           boolean x32,
                           boolean oldExpl,
                           String explanationStrategy,
                           boolean efl,
                           int copt) {
        this.jssource = jssource;
        this.debug = debug;
        this.outputc = outputc;
        plat = p;
        env = new JSEnvironment();
        if (use_gc) {
            gc = MMScheme.GC;
        } else {
            gc = MMScheme.NoGC;
        }
        this.clang_path = clang_path;
        this.emcc_path = clang_path;
        this.execname = execname;
        this.basedir = basedir;
        this.inherit_io = inherit_io;
        this.field_opts = fopts;
        this.extra_decls = new LinkedList<>();
        this.extra_links = new LinkedList<>();
        this.dumpConstraints = dumpConstraints;
        this.dumpConstraintSolution = dumpConstraintSol;
        this.guest = false;
        this.runtime_src = runtime_src;
        this.extpath = basedir+"/external";
        this.encoded_vals = encoded_vals;
        this.x32 = x32;
        this.efl = efl;
        this.interop = false;
        this.oldExpl = oldExpl;
        this.explanationStrategy = explanationStrategy;
        assert (-1 <= copt && copt <= 3);
        this.coptlevel = copt;
    }

    public boolean useConstraints() { return true; }
    public boolean debug() { return debug; }
    public Platform getTargetPlatform() { return plat; }
    public String getInputFileName() { return jssource; }
    public String getOutputCName() { return outputc; }
    public JSEnvironment getRuntimeEnvironment() { return env; }
    public MMScheme getMMScheme() { return gc; }
    public String clangPath() { return clang_path; }
    public String emccPath() { return emcc_path; }
    public String execname() { return execname; }
    public String baseDirectory() { return basedir; }
    public boolean ccomp_spew() { return inherit_io; }
    public boolean fieldOptimizations() { return field_opts; }

    public void addDeclarationFile(Path s) {
        extra_decls.add(s);
    }
    public List<Path> getExtraDeclarationFiles() { return extra_decls; }
    public void addLinkageFile(Path s) {
        extra_links.add(s);
    }
    public List<Path> getExtraLinkageFiles() { return extra_links; }

    public boolean shouldDumpConstraints() { return dumpConstraints; }
    public boolean shouldDumpConstraintSolution() { return dumpConstraintSolution; }

    public void setGuestRuntime() { guest = true; }
    public boolean isGuestRuntime() { return guest; }

    public String getRuntimeSourcePath() { return runtime_src; }
    public void setExternalDeps(String path) { extpath = path; }
    public String getExternalDeps() { return extpath; }

    public boolean encodeVals() { return encoded_vals; }
    public boolean m32() { return x32; }

    public void enableInteropMode() { interop = true; }
    public boolean interopEnabled() { return interop; }

    public void startInInteropMode() { start_in_interop = true; }
    public boolean shouldStartInInterop() { return start_in_interop; }

    public boolean oldExplanations() { return oldExpl; }
    public String explanationStrategy() { return explanationStrategy; }

    public boolean eflEnabled() { return efl; }

    public String COptimizationFlag() {
        switch (coptlevel) {
            case -1: return "-O";
            case 0: return "-O0";
            case 1: return "-O1";
            case 2: return "-O2";
            case 3: return "-O3";
            default: throw new IllegalArgumentException("Bad optimization level made it past ctor???: "+coptlevel);
        }
    }
}
