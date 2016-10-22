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
 * Base class for backend tests, that need to run external tools like node and clang
 *
 * @author colin.gordon
 */
package com.samsung.sjs;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import org.apache.commons.io.IOUtils;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.samsung.sjs.Compiler.*;

public abstract class ABackendTest extends SJSTest {
    public ABackendTest(String testName) throws IOException {
        super(testName);
        basedir = new File(".").getCanonicalPath();
        System.err.println("Base directory: "+basedir);
	if (System.getProperty("os.name").equals("Mac OS X")) {
		nodebin = "node";
	} else {
		nodebin = "nodejs";
	}
    }

    protected final String basedir;
    protected final String nodebin;

    public String baseDirectory() { return basedir; }

    public Process runNodePath(String path) {
        try {
        return exec(false, nodebin, path+".js");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        return null;
    }

    public Process runSpiderMonkeyPath(String path) {
        try {
        return exec(false, baseDirectory()+"/external/spidermonkey/js", path+".js");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        return null;
    }

    public Process runNode(File jsfile) throws IOException {
        return exec(false, nodebin, jsfile.getAbsolutePath());
    }

    protected String getTestDir() {
        return "endtoend/";
    }

    protected boolean doInterop() { return false; }
    protected boolean bootInterop() { return false; }

    /**
     * Prefix the specified JS file with some of the testing primitives we assume,
     * placing the output in tmpdir and returning the resulting File.
     */
    public File prefixJS(Path tmpdir, File js) throws IOException, FileNotFoundException {
        File result = File.createTempFile("___", ".js", tmpdir.toFile());
        OutputStream out = new FileOutputStream(result);
        IOUtils.write("function assert(cond) { if (!cond) { throw Error(); } }\n\n", out);
        IOUtils.write("function print(s) { console.log(s); }\n\n", out);
        IOUtils.write("function printInt(s) { console.log(s); }\n\n", out);
        IOUtils.write("function printString(s) { console.log(s); }\n\n", out);
        IOUtils.write("function printFloat(s) { console.log(s); }\n\n", out);
        IOUtils.write("function printFloat10(s) { console.log(s.toFixed(10)); }\n\n", out);
        IOUtils.write("function itofp(s) { return s; }\n\n", out);
        IOUtils.write("function string_of_int(x) { return x.toString(); }\n\n", out);
        IOUtils.write("var TyHint = {};\n\n", out);
        IOUtils.copy(new FileReader(js), out);
        return result;
    }

    public void compilerTest() {
        compilerTest(this::runClang, SJSTest::simple_exec);
    }
    public void compilerTest(boolean execute_code) {
        compilerTest(this::runClang, SJSTest::simple_exec, execute_code, true);
    }
    public void compilerTestNonVerbose() {
        compilerTest(this::runClang, SJSTest::simple_exec, true, false);
    }
    public void asmTest() {
        compilerTest(this::runEmcc, this::runSpiderMonkeyPath);
    }
    public Process runClang(CompilerOptions opts, String[] extra) {
        try {
            return Compiler.runClang(opts, new String[0], extra);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            return null;
        }
    }
    public Process runEmcc(CompilerOptions opts, String[] extra) {
        try {
            return Compiler.runEmcc(opts, new String[0], extra);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            return null;
        }
    }
    public void compilerTest(BiFunction<CompilerOptions,String[],Process> compiler,
                             Function<String,Process> evaluator) {
        compilerTest(compiler, evaluator, true, true);
    }

    public void x86Test() {
        compilerTest(this::runClang, SJSTest::simple_exec, true, true, "-m32", baseDirectory()+"/external/gc/x86/lib/libgc.a");
    }

    // Override in a subclass to test value encoding
    public boolean shouldEncodeValues() { return false; }

    public void compilerTest(BiFunction<CompilerOptions,String[],Process> compiler,
                             Function<String,Process> evaluator,
                             boolean execute_code, boolean verbose,
                             String... extra) {
        try {
            String script = getInputScriptPath();
            System.out.println("Looking for test script: "+script);
            System.err.println("COMPILING: "+script);
            File scriptfile = new File(script);
            Path tmpdir = Files.createTempDirectory("__fawefwea8ew");
            tmpdir.toFile().deleteOnExit();
            String ccode = scriptfile.getName().replaceFirst(".js$", ".c");
            File cfile = File.createTempFile("___", ccode, tmpdir.toFile());
            String exec = ccode.replaceFirst(".c$", "");
            File execfile = File.createTempFile("___", exec, tmpdir.toFile());
            CompilerOptions opts =
                    new CompilerOptions(CompilerOptions.Platform.Native,
                                        scriptfile.getAbsolutePath(),
                                        verbose, // -debugcompiler
                                        cfile.getAbsolutePath(),
                                        true /* use GC */,
                                        "clang",
                                        "emcc",
                                        execfile.getAbsolutePath(),
                                        baseDirectory(),
                                        false /* don't dump C compiler spew into JUnit console */,
                                        true /* apply field optimizations */,
                                        verbose /* emit type inference constraints to console */,
                                        verbose /* dump constraint solution to console */,
                                        null /* find runtime src for linking locally (not running from jar) */,
                                        shouldEncodeValues(),
                                        false /* TODO: x86Test passes explicit -m32 flag, rather than setting this */,
                                        false /* we don't care about error explanations */,
                                        null /* we don't care about error explanations */,
                                        false /* generate efl environment code */,
                                        3 /* pass C compiler -O3 */);
            if (doInterop()) {
                opts.enableInteropMode();
            }
            if (bootInterop()) {
                opts.startInInteropMode();
            }
            Compiler.compile(opts);
            Process clang = compiler.apply(opts, extra);
            clang.waitFor();
            if (clang.exitValue() != 0) {
                StringWriter w_stdout = new StringWriter(),
                             w_stderr = new StringWriter();
                IOUtils.copy(clang.getInputStream(), w_stdout, Charset.defaultCharset());
                IOUtils.copy(clang.getErrorStream(), w_stderr, Charset.defaultCharset());
                String compstdout = w_stdout.toString();
                String compstderr = w_stderr.toString();
                System.err.println("!!!!!!!!!!!!! Compiler exited with value "+clang.exitValue());
                System.err.println("Compiler stdout: "+compstdout);
                System.err.println("Compiler stderr: "+compstderr);
            }
            assertTrue(clang.exitValue() == 0);
            if (execute_code) {
                File prefixedscript = prefixJS(tmpdir, scriptfile);
                assertSameProcessOutput(runNode(prefixedscript), evaluator.apply(execfile.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
