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
 * Unit tests for C Code generation backend, long-running tests
 *
 * @author colin.gordon
 */
package com.samsung.sjs;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.Ignore;

@Ignore
public class CodeGenOnlyTest extends ABackendTest {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CodeGenOnlyTest( String testName ) throws IOException
    {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(CodeGenOnlyTest.class);
    }

    @Override
    protected String getTestDir() {
        return "endtoend/";
    }

    /*
     * Tests that work with the new frontend
     */
    public void test_2darray() {
        compilerTest(false);
    }
    public void test_arraylit1() {
        compilerTest(false);
    }
    public void test_arraylit2() {
        compilerTest(false);
    }
    public void test_arraylit3() {
        compilerTest(false);
    }
    public void test_arraylit4() {
        compilerTest(false);
    }
    public void test_assert() {
        compilerTest(false);
    }
    public void test_bool() {
        compilerTest(false);
    }
    public void test_console1() {
        compilerTest(false);
    }
    public void test_console2() {
        compilerTest(false);
    }
    public void test_consoleassert1() {
        compilerTest(false);
    }
    public void test_consoleerror() {
        compilerTest(false);
    }
    public void test_consolewarn() {
        compilerTest(false);
    }
    public void test_directcalls() {
        compilerTest(false);
    }
    public void test_environments1() {
        compilerTest(false);
    }
    public void test_fib() {
        compilerTest(false);
    }
    public void test_forloop1() {
        compilerTest(false);
    }
    public void test_forloop2() {
        compilerTest(false);
    }
    public void test_lambda1() {
        compilerTest(false);
    }
    public void test_lambda2() {
        compilerTest(false);
    }
    public void test_lambda3() {
        compilerTest(false);
    }
    public void test_lambda4() {
        compilerTest(false);
    }
    public void test_lambda5() {
        compilerTest(false);
    }
    public void test_objlit2() {
        compilerTest(false);
    }
    public void test_objlit3() {
        compilerTest(false);
    }
    public void test_objlit4() {
        compilerTest(false);
    }
    public void test_objlit5() {
        compilerTest(false);
    }
    public void test_store1() {
        compilerTest(false);
    }
    public void test_store2() {
        compilerTest(false);
    }
    public void test_update1() {
        compilerTest(false);
    }

    public void test_constructor1() {
        compilerTest(false);
    }
    public void test_constructors1() {
        compilerTest(false);
    }
    public void test_rectype_ctor1() {
        compilerTest(false);
    }

    public void test_ackermann() {
        compilerTest(false);
    }
    public void test_spectralnorm_proper() {
        compilerTest(false);
    }
    public void test_matrix() {
        compilerTest(false);
    }
    public void test_objlit1() {
        compilerTest(false);
    }
    public void test_objectliteral1() {
        compilerTest(false);
    }
    public void test_objectliteral2() {
        compilerTest(false);
    }
    public void test_simple_matmul() {
        compilerTest(false);
    }
    public void test_proper_matmul() {
        compilerTest(false);
    }
    public void test_ary() {
        compilerTest(false);
    }
    public void test_fannkuchredux() {
        compilerTest(false);
    }
    public void test_fibo() {
        compilerTest(false);
    }
    public void test_nsieve() {
        compilerTest(false);
    }
    public void test_methodsonliteral() {
        compilerTest(false);
    }
    public void test_nbody() {
        compilerTest(false);
    }
    public void test_nbody_ctor() {
        compilerTest(false);
    }
    public void test_harmonic() {
        compilerTest(false);
    }
    public void test_nsievebits() {
        compilerTest(false);
    }
    public void test_partialsums() {
        compilerTest(false);
    }
    public void test_nestedloop() {
        compilerTest(false);
    }
    public void test_takfp() {
        compilerTest(false);
    }
    public void test_random() {
        compilerTest(false);
    }
    public void test_heapsort() {
        compilerTest(false);
    }
    public void test_fannkuch() {
        compilerTest(false);
    }
    public void test_printstub() {
        compilerTest(false);
    }

    public void test_mapliteral1() {
        compilerTest(false);
    }
    public void test_mapliteral3() {
        compilerTest(false);
    }
    public void test_fasta() {
        compilerTest(false);
    }
    public void test_strcat() {
        compilerTest(false);
    }

    // TODO: Array.prototype.concat() unimplemented
    // TODO: Why do these cause clang to hang w/ 100% CPU utilization in the test harness, and
    // complete on the command line?
    //public void test_sieve() {
    //    compilerTest(false);
    //}
    //public void test_lists() {
    //    compilerTest(false);
    //}
    //public void test_tsp_ga() {
    //    compilerTest(false);
    //}

    public void test_mandelbrot() {
        compilerTest(false);
    }

    public void test_pidigits() {
        compilerTest(false);
    }

    // Misc. test programs
    public void test_calc() {
        compilerTest(false);
    }
    public void test_switch() {
        compilerTest(false);
    }
    public void test_string() {
        compilerTest(false);
    }
    public void test_inlining() {
        compilerTest(false);
    }

    public void test_taggedints() {
        compilerTest(false);
    }
    public void test_taggedints2() {
        compilerTest(false);
    }
    public void test_field_read_lval() {
        compilerTest(false);
    }
    public void test_datetest() {
        compilerTest(false);
    }
    public void test_proto() {
        compilerTest(false);
    }
    public void test_proto2() {
        compilerTest(false);
    }
}
