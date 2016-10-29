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
package com.samsung.sjs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.samsung.sjs.constraintsolver.TypeAssignment;
import com.samsung.sjs.theorysolver.FixingSetFinder;
import com.samsung.sjs.theorysolver.MaxSatFixingSetFinder;
import com.samsung.sjs.theorysolver.SJSTypeTheory;
import com.samsung.sjs.theorysolver.Sat4J;
import com.samsung.sjs.theorysolver.SatSolver;
import com.samsung.sjs.theorysolver.TheorySolver;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samsung.sjs.backend.ConstantInliningPass;
import com.samsung.sjs.backend.FieldAccessOptimizer;
import com.samsung.sjs.backend.IRCBackend;
import com.samsung.sjs.backend.IRClosureConversionPass;
import com.samsung.sjs.backend.IREnvironmentLayoutPass;
import com.samsung.sjs.backend.IRFieldCollector;
import com.samsung.sjs.backend.IRVTablePass;
import com.samsung.sjs.backend.IntrinsicsInliningPass;
import com.samsung.sjs.backend.PhysicalLayoutConstraintGathering;
import com.samsung.sjs.backend.RhinoToIR;
import com.samsung.sjs.backend.RhinoTypeValidator;
import com.samsung.sjs.backend.SwitchDesugaringPass;
import com.samsung.sjs.backend.ThreeAddressConversion;
import com.samsung.sjs.backend.asts.c.CompilationUnit;
import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.constraintsolver.DirectionalConstraintSolver;
import com.samsung.sjs.constraintsolver.SolverException;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.types.Type;

/**
 * SJS Compiler
 *
 * The purpose of this class is to allow just enough code generation to for backend and FFI work to
 * proceed in parallel with work on a more robust frontend parser and type inference engine.
 *
 */
public class Compiler extends ExternalRhinoVisitor
{

    private static Logger logger = LoggerFactory.getLogger(Compiler.class);

    @SuppressWarnings("static-access")
    public static void main( String[] args )
        throws IOException, SolverException, InterruptedException
    {
        boolean debug = false;
        boolean use_gc = true;
        CompilerOptions.Platform p = CompilerOptions.Platform.Native;
        CompilerOptions opts = null;
        boolean field_opts = false;
        boolean typecheckonly = false;
        boolean showconstraints = false;
        boolean showconstraintsolution = false;
        String[] decls = null;
        String[] links = null;
        String[] objs = null;
        boolean guest = false;
        boolean stop_at_c = false;
        String external_compiler = null;
        boolean encode_vals = false;
        boolean x32 = false;
        boolean validate = true;
        boolean interop = false;
        boolean oldExpl = false;
        String explanationStrategy = null;
        boolean efl = false;

        Options options = new Options();
        options.addOption("debugcompiler", false, "Enable compiler debug spew");
        //options.addOption("o", true, "Set compiler output file (must be .c)");
        options.addOption( OptionBuilder //.withArgName("o")
                                        .withLongOpt("output-file")
                                        .withDescription("Output file (must be .c)")
                                        .hasArg()
                                        .withArgName("file")
                                        .create("o") );
        options.addOption( OptionBuilder.withLongOpt("target")
                                        .withDescription("Select target platform: 'native' or 'web'")
                                        .hasArg()
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("gc")
                                        .withDescription("Disable GC: param is 'on' (default) or 'off'")
                                        .hasArg()
                                        .create() );

        options.addOption( OptionBuilder.withDescription("Enable field access optimizations: param is 'true' (default) or 'false'")
                                        .hasArg()
                                        .create("Xfields") );

        options.addOption( OptionBuilder.withDescription("Compile for encoded values.  TEMPORARY.  For testing interop codegen")
                                        .hasArg()
                                        .create("XEncodedValues") );

        options.addOption( OptionBuilder.withLongOpt("typecheck-only")
                                        .withDescription("Only typecheck the file, don't compile")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("show-constraints")
                                        .withDescription("Show constraints generated during type inference")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("show-constraint-solution")
                                        .withDescription("Show solution to type inference constraints")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("extra-decls")
                                        .withDescription("Specify extra declaration files, comma-separated")
                                        .hasArg()
                                        .withValueSeparator(',')
                                        .create() );
        options.addOption(OptionBuilder.withLongOpt("native-libs")
                .withDescription("Specify extra linkage files, comma-separated")
                .hasArg()
                .withValueSeparator(',')
                .create());
        Option extraobjs = OptionBuilder.withLongOpt("extra-objs")
                                        .withDescription("Specify extra .c/.cpp files, comma-separated")
                                        .hasArg()
                                        .withValueSeparator(',')
                                        .create();
        extraobjs.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(extraobjs);
        options.addOption( OptionBuilder.withLongOpt("guest-runtime")
                                        .withDescription("Emit code to be called by another runtime (i.e., main() is written in another language).")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("only-c")
                                        .withDescription("Generate C code, but do not attempt to compile it")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("c-compiler")
                                        .withDescription("Disable GC: param is 'on' (default) or 'off'")
                                        .hasArg()
                                        .create("cc") );
        options.addOption( OptionBuilder.withLongOpt("runtime-src")
                                        .withDescription("Specify path to runtime system source")
                                        .hasArg()
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("ext-path")
                                        .withDescription("Specify path to external dependency dir (GC, etc.)")
                                        .hasArg()
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("skip-validation")
                                        .withDescription("Run the backend without validating the results of type inference")
                                        .create() );

        options.addOption( OptionBuilder.withLongOpt("m32")
                                        .withDescription("Force 32-bit compilation")
                                        .create() );
        options.addOption( OptionBuilder.withLongOpt("Xinterop")
                                        .withDescription("Enable (experimental) interoperability backend")
                                        .create() );

        options.addOption( OptionBuilder.withLongOpt("oldExpl")
                                        .withDescription("Use old error explanations")
                                        .create() );

        String explanationStrategyHelp = "default: " + FixingSetFinder.defaultStrategy() + "; other choices: " +
            FixingSetFinder.strategyNames().stream()
                .filter(s -> !s.equals(FixingSetFinder.defaultStrategy()))
                .collect(Collectors.joining(", "));

        options.addOption( OptionBuilder.withLongOpt("explanation-strategy")
                                        .withDescription("Error explanation strategy to use (" + explanationStrategyHelp + ')')
                                        .hasArg()
                                        .create() );

        options.addOption( OptionBuilder.withLongOpt("efl")
                                        .withDescription("Set up efl environment in main()")
                                        .create() );

        try {
            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);

            String[] newargs = cmd.getArgs();
            if (newargs.length != 1) {
                throw new ParseException("Invalid number of arguments");
            }

            String sourcefile = newargs[0];
            if (!sourcefile.endsWith(".js")) {
                throw new ParseException("Invalid file extension on input file: "+sourcefile);
            }

            String gc = cmd.getOptionValue("gc", "on");
            if (gc.equals("on")) {
                use_gc = true;
            } else if (gc.equals("off")) {
                use_gc = false;
            } else {
                    throw new ParseException("Invalid GC option: "+gc);
            }
            String fields = cmd.getOptionValue("Xfields", "true");
            if (fields.equals("true")) {
                field_opts = true;
            } else if (fields.equals("false")) {
                field_opts = false;
            } else {
                    throw new ParseException("Invalid field optimization option: "+fields);
            }
            String encoding = cmd.getOptionValue("XEncodedValues", "false");
            if (encoding.equals("true")) {
                encode_vals = true;
            } else if (encoding.equals("false")) {
                encode_vals = false;
            } else {
                    throw new ParseException("Invalid value encoding option: "+encode_vals);
            }
            String plat = cmd.getOptionValue("target", "native");
            if (plat.equals("native")) {
                p = CompilerOptions.Platform.Native;
            } else if (plat.equals("web")) {
                p = CompilerOptions.Platform.Web;
            } else {
                    throw new ParseException("Invalid target platform: "+plat);
            }
            if (cmd.hasOption("cc")) {
                external_compiler = cmd.getOptionValue("cc");
            }
            if (cmd.hasOption("skip-validation")) {
                validate = false;
            }
            if (cmd.hasOption("typecheck-only")) {
                typecheckonly = true;
            }
            if (cmd.hasOption("show-constraints")) {
                showconstraints = true;
            }
            if (cmd.hasOption("show-constraint-solution")) {
                showconstraintsolution = true;
            }
            if (cmd.hasOption("debugcompiler")) {
                debug = true;
            }
            if (cmd.hasOption("m32")) {
                x32 = true;
            }
            if (cmd.hasOption("Xinterop")) {
                interop = true;
            }
            if (cmd.hasOption("oldExpl")) {
                oldExpl = true;
            }
            if (cmd.hasOption("explanation-strategy")) {
                explanationStrategy = cmd.getOptionValue("explanation-strategy");
            }
            String output = cmd.getOptionValue("o");
            if (output == null) {
                output = sourcefile.replaceFirst(".js$", ".c");
            } else {
                if (!output.endsWith(".c")) {
                    throw new ParseException("Invalid file extension on output file: "+output);
                }
            }
            String runtime_src = cmd.getOptionValue("runtime-src");
            String ext_path = cmd.getOptionValue("ext-path");
            if (ext_path == null) {
               ext_path = new File(".").getCanonicalPath()+"/external";
            }

            if (cmd.hasOption("extra-decls")) {
                decls = cmd.getOptionValues("extra-decls");
            }
            if (cmd.hasOption("native-libs")) {
                links = cmd.getOptionValues("native-libs");
            }
            if (cmd.hasOption("extra-objs")) {
                objs = cmd.getOptionValues("extra-objs");
            }
            if (cmd.hasOption("guest-runtime")) {
                guest = true;
            }
            if (cmd.hasOption("only-c")) {
                stop_at_c = true;
            }
            if (cmd.hasOption("efl")) {
                efl = true;
            }

            if (!Files.exists(Paths.get(sourcefile))) {
                System.err.println("File "+sourcefile+" was not found.");
                throw new FileNotFoundException(sourcefile);
            }

            String cwd = new java.io.File(".").getCanonicalPath();

            opts = new CompilerOptions(p,
                                       sourcefile,
                                       debug,
                                       output,
                                       use_gc,
                                       external_compiler == null ? "clang" : external_compiler,
                                       external_compiler == null ? "emcc" : external_compiler,
                                       cwd+"/a.out", // emcc automatically adds .js
                                       new File(".").getCanonicalPath(),
                                       true,
                                       field_opts,
                                       showconstraints,
                                       showconstraintsolution,
                                       runtime_src,
                                       encode_vals,
                                       x32,
                                       oldExpl,
                                       explanationStrategy,
                                       efl);
            if (guest) {
                opts.setGuestRuntime();
            }
            if (interop) {
                opts.enableInteropMode();
            }
            opts.setExternalDeps(ext_path);
            if (decls != null) {
                for (String s : decls) {
                    Path fname = FileSystems.getDefault().getPath(s);
                    opts.addDeclarationFile(fname);
                }
            }
            if (links != null) {
                for (String s : links) {
                    Path fname = FileSystems.getDefault().getPath(s);
                    opts.addLinkageFile(fname);
                }
            }
            if (objs == null) {
                objs = new String[0];
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sjsc", options);
        }

        if (opts != null) {
            // This typechecks, and depending on flags, also generates C
            compile(opts, typecheckonly, validate); // don't worry about type validation on command line for now
            if (!typecheckonly && !stop_at_c) {
                int ret = 0;
                // Kept around for debugging 32-bit...
                if (p == CompilerOptions.Platform.Native) {
                    if (opts.m32()) {
                        String[] x = new String[2];
                        x[0] = "-m32";
                        x[1] = opts.getExternalDeps()+"/gc/x86/lib/libgc.a";
                        ret = clang_compile(opts, objs, x);
                    } else {
                        ret = clang_compile(opts, objs, new String[0]);
                    }
                } else {
                    ret = emcc_compile(opts, objs, new String[0]);
                }
                // If clang failed, propagate the failure outwards
                if (ret != 0) {
                    System.exit(ret);
                }
            }
        }
    }

    /**
     * Compile with some temporary options hard-coded; this is the entry point for backend tests.
     *
     * This method will generate code (typecheckonly=false), validate the results of type inference
     * (checkTypes=true), and will not run the new solver (altsolver=false).
     */
    public static void compile(CompilerOptions opts) throws IOException, SolverException {
        compile(opts, false, true);
    }

    public static void compile(CompilerOptions opts, boolean typecheckonly, boolean checkTypes) throws IOException, SolverException {

        Path p = Paths.get(opts.getInputFileName());
        AstRoot sourcetree = null;
        Map<AstNode,Type> types;

        JSEnvironment env = opts.getRuntimeEnvironment();
        switch (opts.getTargetPlatform()) {
            case Web:
                // Fall-through
            case Native:
                // This may be the most hideous line of code I've ever written
                InputStream jsenv = Compiler.class.getClass().getResourceAsStream("/environment.json");
                assert (jsenv != null);
                env.includeFile(jsenv);
        }
        for (Path fname : opts.getExtraDeclarationFiles()) {
            env.includeFile(fname);
        }

        assert (opts.useConstraints());

        FFILinkage ffi = new FFILinkage();
        InputStream stdlib_linkage = Compiler.class.getClass().getResourceAsStream("/linkage.json");
        ffi.includeFile(stdlib_linkage);
        for (Path fname : opts.getExtraLinkageFiles()) {
            ffi.includeFile(fname);
        }

        String script = IOUtils.toString(p.toUri(), Charset.defaultCharset());
        org.mozilla.javascript.Parser parser = new org.mozilla.javascript.Parser();
        sourcetree = parser.parse(script, "", 1);
        ConstraintFactory factory = new ConstraintFactory();
        ConstraintGenerator generator = new ConstraintGenerator(factory, env);
        generator.generateConstraints(sourcetree);
        Set<ITypeConstraint> constraints = generator.getTypeConstraints();
        if (opts.shouldDumpConstraints()) {
            System.err.println("Constraints:");
            System.err.println(generator.stringRepresentationWithTermLineNumbers(constraints));
        }
        if (!opts.oldExplanations()) {
            SatSolver satSolver = new Sat4J();
            SJSTypeTheory theorySolver = new SJSTypeTheory(env, sourcetree);
            List<ITypeConstraint> initConstraints = theorySolver.getConstraints();
            ConstraintGenerator g = theorySolver.hackyGenerator();
            List<Integer> hardConstraints = new ArrayList<>(initConstraints.size());
            List<Integer> softConstraints = new ArrayList<>(initConstraints.size());
            for (int i = 0; i < initConstraints.size(); ++i) {
                boolean isSoft = g.hasExplanation(initConstraints.get(i));
                (isSoft ? softConstraints : hardConstraints).add(i);
            }

            FixingSetFinder<Integer> finder = FixingSetFinder.getStrategy(opts.explanationStrategy());
            Pair<TypeAssignment, Collection<Integer>> result = TheorySolver.solve(
                theorySolver, finder,
                hardConstraints, softConstraints);
            if (!finder.isOptimal() && !result.getRight().isEmpty()) {
                result = TheorySolver.minimizeFixingSet(theorySolver, hardConstraints, softConstraints, result.getLeft(), result.getRight());
            }
            if (!result.getRight().isEmpty()) {
                System.out.println("Found " + result.getRight().size() + " type errors");
                g = theorySolver.hackyGenerator();
                int i = 0;
                for (ITypeConstraint c : theorySolver.hackyConstraintAccess()) {
                    if (result.getRight().contains(i++)) {
                        System.out.println();
                        g.explainFailure(c, result.getLeft()).prettyprint(System.out);
                    }
                }
                System.exit(1);
            }
            types = result.getLeft().nodeTypes();
            if (opts.shouldDumpConstraintSolution()) {
                System.err.println("Solved Constraints:");
                System.err.println(result.getLeft().debugString());
            }
        } else {
            DirectionalConstraintSolver solver = new DirectionalConstraintSolver(constraints, factory, generator);
            TypeAssignment solution = null;
            try {
                solution = solver.solve();
            } catch (SolverException e) {
                System.out.println("type inference failed");
                String explanation = e.explanation();
                System.out.println(explanation);
                System.exit(1);
            }
            if (opts.shouldDumpConstraintSolution()) {
                System.err.println("Solved Constraints:");
                System.err.println(solution.debugString());
            }
            types = solution.nodeTypes();
        }

        if (typecheckonly) {
            return;
        }

        if (checkTypes) {
            new RhinoTypeValidator(sourcetree, types).check();
        }

        // Translate Rhino IR to SJS IR.
        RhinoToIR rti = new RhinoToIR(opts, sourcetree, types);
        com.samsung.sjs.backend.asts.ir.Script ir = rti.convert();

        // Collect the set of explicit property / slot names
        IRFieldCollector fc = new IRFieldCollector(env);
        ir.accept(fc);
        IRFieldCollector.FieldMapping m = fc.getResults();
        if (opts.debug()) {
            System.out.println(m.toString());
        }

        boolean iterate_constant_inlining = true;
        if (opts.debug()) {
            System.err.println("**********************************************");
            System.err.println("* Pre-constant inlining:                     *");
            System.err.println("**********************************************");
            System.err.println(ir.toSource(0));
        }
        while (iterate_constant_inlining) {
            ConstantInliningPass cip = new ConstantInliningPass(opts, ir);
            ir = cip.visitScript(ir);
            // Replacing may make more things constant (vars aren't const) so repeat
            iterate_constant_inlining = cip.didSomething();
            if (opts.debug()) {
                System.err.println("**********************************************");
                System.err.println("* Post-constant inlining:                    *");
                System.err.println("**********************************************");
                System.err.println(ir.toSource(0));
            }
        }

        IREnvironmentLayoutPass envlayout = new IREnvironmentLayoutPass(ir, opts.debug());
        ir.accept(envlayout);
        if (opts.debug()) {
            System.err.println("**********************************************");
            System.err.println("* IR Env Layout Result:                      *");
            System.err.println("**********************************************");
            System.err.println(ir.toSource(0));
        }

        SwitchDesugaringPass sdp = new SwitchDesugaringPass(opts, ir);
        com.samsung.sjs.backend.asts.ir.Script post_switch_desugar = sdp.convert();

        IntrinsicsInliningPass iip = new IntrinsicsInliningPass(post_switch_desugar, opts);
        com.samsung.sjs.backend.asts.ir.Script post_intrinsics = iip.convert();
        if (opts.debug()) {
            System.err.println("**********************************************");
            System.err.println("* IR Intrinsics Inlining Result:              *");
            System.err.println("**********************************************");
            System.err.println(post_intrinsics.toSource(0));
        }

        IRClosureConversionPass ccp = new IRClosureConversionPass(post_intrinsics, envlayout.getMainCaptures(), opts.debug(), opts.isGuestRuntime() ? "__sjs_main" : "main");
        com.samsung.sjs.backend.asts.ir.Script post_cc = ccp.convert();
        if (opts.debug()) {
            System.err.println("**********************************************");
            System.err.println("* IR Closure Conversion Result:              *");
            System.err.println("**********************************************");
            System.err.println(post_cc.toSource(0));
        }

        post_cc = new ThreeAddressConversion(post_cc).visitScript(post_cc);
        if (logger.isDebugEnabled()) {
          System.err.println("**********************************************");
          System.err.println("* Three-Address Conversion Result:           *");
          System.err.println("**********************************************");
          System.err.println(post_cc.toSource(0));
        }

        // Gather constraints for optimizing object layouts
        PhysicalLayoutConstraintGathering plcg = new PhysicalLayoutConstraintGathering(opts, m, ffi);
        post_cc.accept(plcg);
        // TODO: Eventually feed this to the IRVTablePass as a source of layout information

        // Decorate SJS IR with vtables.
        IRVTablePass irvt = new IRVTablePass(opts, m, ffi);
        post_cc.accept(irvt);
        if (opts.fieldOptimizations()) {
            System.err.println("WARNING: Running experimental field access optimizations!");
            post_cc = (com.samsung.sjs.backend.asts.ir.Script)(new FieldAccessOptimizer(post_cc, opts, m, irvt.getVtablesByFieldMap()).visitScript(post_cc));
            if (opts.debug()) {
                System.err.println("**********************************************");
                System.err.println("* Field Access Optimization Result:          *");
                System.err.println("**********************************************");
                System.err.println(post_cc.toSource(0));
            }
        }

        IRCBackend ir2c = new IRCBackend(post_cc, opts, m, ffi, env);
        CompilationUnit c_via_ir = ir2c.compile();
        if (opts.debug()) {
            System.err.println("**********************************************");
            System.err.println("* C via IR Result:                           *");
            System.err.println("**********************************************");
            for (com.samsung.sjs.backend.asts.c.Statement s : c_via_ir) {
                System.err.println(s.toSource(0));
            }
        }

        c_via_ir.writeToDisk(opts.getOutputCName());
    }

    public static Process quiet_exec(String... args) throws IOException {
        return exec(false, args);
    }
    public static Process verbose_exec(String... args) throws IOException {
        return exec(false, args);
    }

    public static Process exec(boolean should_inherit_io, String... args) throws IOException {
        System.err.println("Executing: "+Arrays.toString(args));
        Path tmp = Files.createTempDirectory("testing");
        tmp.toFile().deleteOnExit();
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(tmp.toFile());
        if (should_inherit_io) {
            pb.inheritIO();
        }
        return pb.start();
    }

    public static String getCurrentJarPath() {
        try {
            return Compiler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static {
        runningFromJar = getCurrentJarPath().endsWith(".jar");
    }

    public static final boolean runningFromJar;

    // TODO: Make these use getResourceAsStream(), and if necessary copy the resources out of the
    // JAR onto the filesystem(!) to invoke the C compiler
    public static String getCIncludeDirectory(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath();
        }
        assert (!runningFromJar);
        return new File(URI.create(Compiler.class.getClass().getResource("/backend").toString())).getPath();
    }
    public static String getRuntimeCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/runtime.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/runtime.c").toString())).getPath();
    }
    public static String getGlobalsCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/globals.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/globals.c").toString())).getPath();
    }
    public static String getMathCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/jsmath.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/jsmath.c").toString())).getPath();
    }
    public static String getArrayCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/array.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/array.c").toString())).getPath();
    }
    public static String getHashingCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/xxhash.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/xxhash.c").toString())).getPath();
    }
    public static String getMapCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/map.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/map.c").toString())).getPath();
    }
    public static String getDateCFile(CompilerOptions opts) {
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+"/date.c";
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend/date.c").toString())).getPath();
    }
    public static String getFFICFile(CompilerOptions opts) {
        String file = opts.getTargetPlatform() == CompilerOptions.Platform.Native ? "/native/ffi.c" : "/browser/ffi.c";
        if (opts.getRuntimeSourcePath() != null) {
            return opts.getRuntimeSourcePath()+file;
        }
        return new File(URI.create(Compiler.class.getClass().getResource("/backend"+file).toString())).getPath();
    }

    public static Process runClang(CompilerOptions opts) throws IOException {
        return runClang(opts, new String[0], new String[0]);
    }
    public static String[] getPlatformCompilerArgs(CompilerOptions opts) {
       	if (System.getProperty("os.name").equals("Mac OS X")) {
            String[] args = {}; return args;
	} else {
            String[] args = {"-lbsd", "-lm","-L", opts.getExternalDeps()+"/gc/native/lib", "-lgc"}; return args;
	}
    }
    public static Process runClang(CompilerOptions opts, String[] extra_objs, String[] extra) throws IOException {
        String cwd = new java.io.File(".").getCanonicalPath();
        System.err.println("Running clang from: "+cwd);
        File cfile = new File(opts.getOutputCName());
        // TODO: figure out how to avoid hard-coding the non-emscrtipen clang path while being able
        // to run regular and js codegen tests from the same shell
        String[] fixed_args = { opts.clangPath(),
                                "-g",
                                "-O3",
                                // BEGIN MACOS (doesn't cause issues on Linux)
                                opts.getExternalDeps()+"/gc/native/lib/libgc.a",
                                // END MACOS
                                "-lstdc++",
                                "-ferror-limit=100",
                                "-I", cwd,
                                "-I", getCIncludeDirectory(opts),
                                "-I", opts.getExternalDeps()+"/gc/native/include",
                                "-ftrapv", //"-ftrapv-handler=__overflow_trap",
                                "-Werror=implicit-int",
                                "-Werror=return-type",
                                "-Werror=implicit-function-declaration",
                                "-Werror=string-plus-int",
                                "-Wno-attributes",
                                "-Wno-unused-value",
                                "-Wno-parentheses-equality",
                                "-Wno-int-conversion",
                                "-D__SJS__",
                                opts.getMMScheme() == CompilerOptions.MMScheme.GC ? "-DUSE_GC" : "-DLEAK_MEMORY",
                                "-o", opts.execname(),
                                cfile.getAbsolutePath(),
                                getRuntimeCFile(opts),
                                getGlobalsCFile(opts),
                                getArrayCFile(opts),
                                getHashingCFile(opts),
                                getMapCFile(opts),
                                getDateCFile(opts),
                                getFFICFile(opts),
                                getMathCFile(opts) };
        // Make relative paths absolute
        for (int i = 0; i < extra_objs.length; i++) {
            extra_objs[i] = cwd + "/" + extra_objs[i];
        }
        return exec(opts.ccomp_spew(), ArrayUtils.addAll(fixed_args,
                                        ArrayUtils.addAll(extra_objs,
                                            ArrayUtils.addAll(getPlatformCompilerArgs(opts), extra))));
    }

    // Require emcc already in the environment
    public static Process runEmcc(CompilerOptions opts) throws IOException {
        return runEmcc(opts, new String[0], new String[0]);
    }
    public static Process runEmcc(CompilerOptions opts, String[] extra_objs, String[] extra) throws IOException {
        File cfile = new File(opts.getOutputCName());
        String browser_lib_path = opts.getRuntimeSourcePath() == null ?
                                    opts.baseDirectory()+"/src/main/resources/backend/browser" :
                                    opts.getRuntimeSourcePath()+"/browser";
        String[] fixed_args = { "emcc", "-g",
                                "-I", getCIncludeDirectory(opts),
                                "-I", opts.getExternalDeps()+"/gc/asmjs/include",
                                "--pre-js", browser_lib_path+"/marshalling.js",
                                "--js-library", browser_lib_path+"/library.js",
                                "-DLEAK_MEMORY",
                                "-s", "TOTAL_MEMORY=1526726656", // <-- this defaults to a separate .mem file, assumed to be in cwd
                                "--memory-init-file", "0", // <-- this disables .mem file, so working dir doesn't matter
                                "-O3",
                                "-lstdc++",
                                 "-D__SJS__",
                                "-s", "AGGRESSIVE_VARIABLE_ELIMINATION=1",
                                "-s", "ASSERTIONS=2",
                                "-o", opts.execname()+".js",
                                cfile.getAbsolutePath(),
                                opts.getExternalDeps()+"/gc/asmjs/lib/libgc.a",
                                getRuntimeCFile(opts),
                                getGlobalsCFile(opts),
                                getArrayCFile(opts),
                                getHashingCFile(opts),
                                getMapCFile(opts),
                                getDateCFile(opts),
                                getFFICFile(opts),
                                getMathCFile(opts) };
        return exec(opts.ccomp_spew(), ArrayUtils.addAll(fixed_args,
                                        ArrayUtils.addAll(extra_objs,
                                            ArrayUtils.addAll(getPlatformCompilerArgs(opts), extra))));
    }

    public static int clang_compile(CompilerOptions opts, String[] objs, String[] extra) throws IOException, InterruptedException {
        return manage_c_compiler(runClang(opts, objs, extra), opts);
    }
    public static int emcc_compile(CompilerOptions opts, String[] objs, String[] extra) throws IOException, InterruptedException {
        return manage_c_compiler(runEmcc(opts, objs, extra), opts);
    }
    public static int manage_c_compiler(Process clang, CompilerOptions opts) throws IOException, InterruptedException {
        clang.waitFor();
        if (clang.exitValue() != 0 && !opts.debug()) {
            // If clang failed and we didn't already dump its stderr
            StringWriter w_stdout = new StringWriter(),
                         w_stderr = new StringWriter();
            IOUtils.copy(clang.getInputStream(), w_stdout, Charset.defaultCharset());
            IOUtils.copy(clang.getErrorStream(), w_stderr, Charset.defaultCharset());
            String compstdout = w_stdout.toString();
            String compstderr = w_stderr.toString();
            System.err.println("C compiler ["+opts.clangPath()+"] failed.");
            System.out.println("C compiler stdout:");
            System.out.println(compstdout);
            System.err.println("C compiler stderr:");
            System.err.println(compstderr);
        }
        return clang.exitValue();
    }

}
