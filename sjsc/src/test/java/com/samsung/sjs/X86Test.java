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
 * Unit tests for C Code generation backend, for x86 (32-bit) portability checks
 *
 * @author colin.gordon
 */
package com.samsung.sjs;

import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Ignore;

public class X86Test extends ABackendTest {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public X86Test( String testName ) throws IOException
    {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(X86Test.class);
    }

    @Override
    protected String getTestDir() {
        return "endtoend/";
    }

    /*
     * Tests that work with the new frontend
     */
    public void test_2darray() {
        x86Test();
    }
    public void test_arraylit1() {
        x86Test();
    }
    public void test_arraylit2() {
        x86Test();
    }
    public void test_arraylit3() {
        x86Test();
    }
    public void test_arraylit4() {
        x86Test();
    }
    public void test_assert() {
        x86Test();
    }
    public void test_bool() {
        x86Test();
    }
    public void test_console1() {
        x86Test();
    }
    public void test_console2() {
        x86Test();
    }
    public void test_consoleassert1() {
        x86Test();
    }
    public void test_consoleerror() {
        x86Test();
    }
    public void test_consolewarn() {
        x86Test();
    }
    public void test_directcalls() {
        x86Test();
    }
    public void test_environments1() {
        x86Test();
    }
    public void test_fib() {
        x86Test();
    }
    public void test_forloop1() {
        x86Test();
    }
    public void test_forloop2() {
        x86Test();
    }
    public void test_lambda1() {
        x86Test();
    }
    public void test_lambda2() {
        x86Test();
    }
    public void test_lambda3() {
        x86Test();
    }
    public void test_lambda4() {
        x86Test();
    }
    public void test_lambda5() {
        x86Test();
    }
    public void test_objlit2() {
        x86Test();
    }
    public void test_objlit3() {
        x86Test();
    }
    public void test_objlit4() {
        x86Test();
    }
    public void test_objlit5() {
        x86Test();
    }
    public void test_store1() {
        x86Test();
    }
    public void test_store2() {
        x86Test();
    }
    public void test_update1() {
        x86Test();
    }

    public void test_constructor1() {
        x86Test();
    }
    public void test_constructors1() {
        x86Test();
    }
    public void test_rectype_ctor1() {
        x86Test();
    }

    public void test_ackermann() {
        x86Test();
    }
    public void test_spectralnorm_proper() {
        x86Test();
    }
    public void test_matrix() {
        x86Test();
    }
    public void test_objlit1() {
        x86Test();
    }
    public void test_objectliteral1() {
        x86Test();
    }
    public void test_objectliteral2() {
        x86Test();
    }
    public void test_simple_matmul() {
        x86Test();
    }
    public void test_proper_matmul() {
        x86Test();
    }
    public void test_ary() {
        x86Test();
    }
    public void test_fannkuchredux() {
        x86Test();
    }
    public void test_fibo() {
        x86Test();
    }
    public void test_nsieve() {
        x86Test();
    }
    public void test_methodsonliteral() {
        x86Test();
    }
    public void test_nbody() {
        x86Test();
    }
    public void test_nbody_ctor() {
        x86Test();
    }
    public void test_harmonic() {
        x86Test();
    }
    public void test_nsievebits() {
        x86Test();
    }
    public void test_partialsums() {
        x86Test();
    }
    public void test_nestedloop() {
        x86Test();
    }
    public void test_takfp() {
        x86Test();
    }
    public void test_random() {
        x86Test();
    }
    public void test_heapsort() {
        x86Test();
    }
    public void test_fannkuch() {
        x86Test();
    }
    public void test_printstub() {
        x86Test();
    }


    public void test_mapliteral1() {
        x86Test();
    }
    public void test_mapliteral3() {
        x86Test();
    }
    public void test_fasta() {
        x86Test();
    }
    public void test_strcat() {
        x86Test();
    }

    //// TODO: Array.prototype.concat() unimplemented 
    //public void test_sieve() {
    //    x86Test();
    //}
    //public void test_lists() {
    //    x86Test();
    //}
    // TODO: We fail due to integer overflow (2nd random number generated is > 2^32), don't run
    // because not only does node.js take forever to run it, but clang takes forever to compile it!
    //public void test_tsp_ga() {
    //    x86Test(false);
    //}

    //// TODO: Output would fail due to unicode vs. ascii, at least compile to detect regressions
    //public void test_mandelbrot() {
    //    x86Test(false);
    //}

    //// TODO: Output would fail due to int32_t overflow, at least compile to detect regressions
    //public void test_pidigits() {
    //    x86Test(false);
    //}

    // Misc. test programs
    public void test_calc() {
        x86Test();
    }
    public void test_switch() {
        x86Test();
    }
    public void test_string() {
        x86Test();
    }
    public void test_inlining() {
        x86Test();
    }

    public void test_taggedints() {
        x86Test();
    }
    public void test_taggedints2() {
        x86Test();
    }
    public void test_field_read_lval() {
        x86Test();
    }
    public void test_datetest() {
        x86Test();
    }
    public void test_proto() {
        x86Test();
    }
    public void test_proto2() {
        x86Test();
    }
}
