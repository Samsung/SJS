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

import static com.samsung.sjs.Compiler.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

/**
 * Base class for SJS tests, defining a number of helpful methods
 *
 * @author colin.gordon, ftip
 */
public abstract class SJSTest extends TestCase
{
    public final static boolean DEBUG = false;

    public SJSTest(String testName) {
        super(testName);
    }

    /**
     * Returns the subdirectory of testinput/ (and testoutput/) for
     * the current class.
     */
    protected abstract String getTestDir();

    protected String getInputScriptPath() {
    	URL url = this.getClass().getResource("/testinput/"+getTestDir());
    	String path = url.getPath();
    	String inputScript = path + getName().substring(getName().indexOf('_')+1) + ".js";
        return inputScript;
    }

    /**
     * Identify input script an input script based on the current test's name
     * and retrieve its code. For a test named "test_foo", a file "foo.js"
     * will be expected in the directory "/test/resources/testinput/constraints".
     * If such a file does not exist or if there are problems reading it,
     * an IOException is thrown.
     *
     * @return
     * @throws IOException
     */
    protected String getInputScript() throws IOException {
        String inputScript = getInputScriptPath();
    	if (DEBUG) System.out.println("input script is: " + inputScript);
    	BufferedReader br = null;
    	try {
	    	File file = new File(inputScript);
	    	if (!file.exists()){
	    		throw new IOException("non-existent input script: " + inputScript);
	    	}
			br = new BufferedReader(new FileReader(file));
	    	String fileContents = "";
	    	String line = null;
	    	while ((line = br.readLine()) != null) {
	    	    fileContents += line;
	    	    fileContents += "\n";
	    	}
	    	return fileContents;
    	} finally {
    		if (br != null) br.close();
    	}
    }

    public void assertSameProcessOutput(Process a, Process b) throws IOException, InterruptedException {
        a.waitFor();
        b.waitFor();
        System.out.println("result of process a: "+a.exitValue());
        System.out.println("result of process b: "+b.exitValue());
        //assertTrue(a.exitValue() == b.exitValue());
        // Requiring the same exit value and output is usually too strong when we're testing error/exception
        // code.  Both node and our compiled code will return 0 when everything is okay, and should
        // then produce the same output.  But when there's an assertion failure or exception,
        // the C standard library semantics and node's semantics differ, in both return code and
        // process output.  So we enforce a more relaxed check here:
        // - If both processes return 0, assert both output streams match for the two processses
        // - Otherwise, assert both processes are non-zero exits (both failed), and ignore the stream contents.
        // This leaves open the possibility that a test will pass when the processes fail for
        // different reasons.
        int exita = a.exitValue();
        int exitb = b.exitValue();
        StringWriter a_stdout = new StringWriter(),
                     b_stdout = new StringWriter(),
                     a_stderr = new StringWriter(),
                     b_stderr = new StringWriter();
        IOUtils.copy(a.getInputStream(), a_stdout, Charset.defaultCharset());
        IOUtils.copy(b.getInputStream(), b_stdout, Charset.defaultCharset());
        IOUtils.copy(a.getErrorStream(), a_stderr, Charset.defaultCharset());
        IOUtils.copy(b.getErrorStream(), b_stderr, Charset.defaultCharset());
        // Trim trailing newlines from output
        String a_stdout_str = a_stdout.toString().trim();
        String b_stdout_str = b_stdout.toString().trim();
        String a_stderr_str = a_stderr.toString().trim();
        String b_stderr_str = b_stderr.toString().trim();
        // HACK get rid of GC warning message from SJS stderr
        b_stderr_str = b_stderr_str.replaceAll("GC Warning: Repeated allocation of very large block \\(appr\\. size .*\\):\\n\\tMay lead to memory leak and poor performance.", "");
        boolean same_stdout = a_stdout_str.equals(b_stdout_str);
        boolean same_stderr = a_stderr_str.equals(b_stderr_str);
        if (0 == exita && 0 == exitb) {
            if (!same_stdout) {
                System.err.println("FAILURE: stdout mismatch");
                System.err.println("left stdout: ["+a_stdout_str+"]");
                System.err.println("right stdout: ["+b_stdout_str+"]");
            }
            assertTrue(same_stdout);
            if (!same_stderr) {
                System.err.println("FAILURE: stderr mismatch");
                System.err.println("left stderr: ["+a_stderr_str+"]");
                System.err.println("right stderr: ["+b_stderr_str+"]");
            }
            assertTrue(same_stderr);
        } else {
            if (exita == 0 || exitb == 0) {
                System.err.println("FAILURE: Process A returned "+exita+", Process B returned "+exitb);
                System.err.println("left stdout: ["+a_stdout_str+"]");
                System.err.println("right stdout: ["+b_stdout_str+"]");
                System.err.println("left stderr: ["+a_stderr_str+"]");
                System.err.println("right stderr: ["+b_stderr_str+"]");
            }
            assertTrue(exita != 0 && exitb != 0);
        }
    }

    protected String readFileIntoString(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    protected void compareWithExpectedOutput(String actualOutput, String suffix, String outputFolder)
            throws IOException {
        Path basePath = Paths
                .get("src/test/resources/testoutput/" + outputFolder + "/");
        assertTrue(Files.exists(basePath));
        String testName = getName().substring(getName().indexOf('_') + 1);
        Path path = Paths.get(basePath.toString(), testName + suffix);
        if (Files.exists(path)) {
            String expectedOutput = readFileIntoString(path);
            assertEquals(expectedOutput, actualOutput);
        } else {
            // create the file
            Files.write(path, actualOutput.getBytes());
        }
    }

    public static Process simple_exec(String prog) {
        try {
        return exec(false, prog);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            return null;
        }
    }

}
