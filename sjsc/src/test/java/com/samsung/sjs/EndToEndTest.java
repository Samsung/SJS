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


public class EndToEndTest extends ABackendTest {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public EndToEndTest( String testName ) throws IOException
    {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(EndToEndTest.class);
    }

    @Override
    protected String getTestDir() {
        return "endtoend/";
    }

    /*
     * Tests that work with the new frontend
     */
    public void test_2darray() {
        compilerTest();
    }
    public void test_arraylit1() {
        compilerTest();
    }
    public void test_arraylit2() {
        compilerTest();
    }
    public void test_arraylit3() {
        compilerTest();
    }
    public void test_arraylit4() {
        compilerTest();
    }
    public void test_assert() {
        compilerTest();
    }
    public void test_assignment_lhs_eval() {
        compilerTest();
    }
    public void test_assignment_rhs_eval() {
        compilerTest();
    }
    public void test_bool() {
        compilerTest();
    }
    public void test_console1() {
        compilerTest();
    }
    public void test_console2() {
        compilerTest();
    }
    public void test_consoleassert1() {
        compilerTest();
    }
    public void test_consoleerror() {
        compilerTest();
    }
    public void test_consolewarn() {
        compilerTest();
    }
    public void test_directcalls() {
        compilerTest();
    }
    public void test_emptystatement() {
        compilerTest();
    }
    public void test_environments1() {
        compilerTest();
    }
    public void test_fib() {
        compilerTest();
    }
    public void test_forloop1() {
        compilerTest();
    }
    public void test_forloop2() {
        compilerTest();
    }
    
    public void test_iife() {
    	compilerTest();
    }
    
    public void test_lambda1() {
        compilerTest();
    }
    public void test_lambda2() {
        compilerTest();
    }
    public void test_lambda3() {
        compilerTest();
    }
    public void test_lambda4() {
        compilerTest();
    }
    public void test_lambda5() {
        compilerTest();
    }
    public void test_objlit2() {
        compilerTest();
    }
    public void test_objlit3() {
        compilerTest();
    }
    public void test_objlit4() {
        compilerTest();
    }
    public void test_objlit5() {
        compilerTest();
    }
    public void test_store1() {
        compilerTest();
    }
    public void test_store2() {
        compilerTest();
    }
    public void test_update1() {
        compilerTest();
    }

    public void test_constructor1() {
        compilerTest();
    }
    public void test_constructors1() {
        compilerTest();
    }
    public void test_rectype_ctor1() {
        compilerTest();
    }

    public void test_ackermann() {
        compilerTest();
    }
    public void test_spectralnorm_proper() {
        compilerTest();
    }
    public void test_matrix() {
        compilerTest();
    }
    public void test_objlit1() {
        compilerTest();
    }
    public void test_objectliteral1() {
        compilerTest();
    }
    public void test_objectliteral2() {
        compilerTest();
    }
    public void test_simple_matmul() {
        compilerTest();
    }
    public void test_proper_matmul() {
        compilerTest();
    }
    public void test_ary() {
        compilerTest();
    }
    public void test_fannkuchredux() {
        compilerTest();
    }
    public void test_fibo() {
        compilerTest();
    }
    public void test_nsieve() {
        compilerTest();
    }
    public void test_methodsonliteral() {
        compilerTest();
    }
    public void test_nbody() {
        compilerTest();
    }
    public void test_nbody_ctor() {
        compilerTest();
    }
    public void test_harmonic() {
        compilerTest();
    }
    public void test_nsievebits() {
        compilerTest();
    }
    public void test_partialsums() {
        compilerTest();
    }
    public void test_nestedloop() {
        compilerTest();
    }
    public void test_takfp() {
        compilerTest();
    }
    public void test_random() {
        compilerTest();
    }
    public void test_heapsort() {
        compilerTest();
    }
    public void test_fannkuch() {
        compilerTest();
    }
    public void test_printstub() {
        compilerTest();
    }


    public void test_mapliteral1() {
        compilerTest();
    }
    public void test_mapliteral3() {
        compilerTest();
    }
    public void test_fasta() {
        compilerTest();
    }
    public void test_strcat() {
        compilerTest();
    }

    //// TODO: Array.prototype.concat() unimplemented
    //public void test_sieve() {
    //    compilerTest();
    //}
    //public void test_lists() {
    //    compilerTest();
    //}
    // TODO: We fail due to integer overflow (2nd random number generated is > 2^32), don't run
    // because not only does node.js take forever to run it, but clang takes forever to compile it!
    //public void test_tsp_ga() {
    //    compilerTest(false);
    //}

    // TODO: Output would fail due to unicode vs. ascii, at least compile to detect regressions
    public void test_mandelbrot() {
        compilerTest(false);
    }

    // TODO: Output would fail due to int32_t overflow, at least compile to detect regressions
    public void test_pidigits() {
        compilerTest(false);
    }

    // Misc. test programs
    public void test_annex_headless() {
        compilerTest();
    }
    public void test_calc() {
        compilerTest();
    }
    public void test_initfloatint() {
        compilerTest();
    }
    public void test_switch() {
        compilerTest();
    }
    public void test_switch_desugar() {
        compilerTest();
    }
    public void test_switch_desugar_1() {
        compilerTest();
    }
    public void test_switch_desugar_2() {
        compilerTest();
    }
    public void test_switch_desugar_3() {
        compilerTest();
    }
    public void test_switch_desugar_4() {
        compilerTest();
    }
    public void test_string() {
        compilerTest();
    }
    public void test_inlining() {
        compilerTest();
    }

    public void test_taggedints() {
        compilerTest();
    }
    public void test_taggedints2() {
        compilerTest();
    }
    public void test_field_read_lval() {
        compilerTest();
    }
    public void test_datetest() {
        compilerTest();
    }
    public void test_proto() {
        compilerTest();
    }
    public void test_proto2() {
        compilerTest();
    }
    public void test_heap() {
        compilerTest();
    }
    public void test_indirect_map_access() {
        compilerTest();
    }
    public void test_subProto1() {
        compilerTest();
    }
    public void test_floatshifting() {
        compilerTest();
    }
    public void test_fieldinc() {
        compilerTest();
    }
    public void test_fieldinc2() {
        compilerTest();
    }
    public void test_lazybinop() {
        compilerTest();
    }
    public void test_intfloatcoerce() {
        compilerTest();
    }
    public void test_intfloatargs() {
        compilerTest();
    }
    public void test_nonboolor() {
        compilerTest();
    }
    public void test_unused_prop1() {
        compilerTest();
    }
    public void test_justreadthis() {
        compilerTest();
    }

    public void test_closure_hoist() {
        compilerTest();
    }
    public void test_closure_hoist2() {
        compilerTest();
    }
    public void test_nullCompare1() {
        compilerTest();
    }
    public void test_nullCompare2() {
        compilerTest();
    }
    public void test_nullCompare3() {
        compilerTest();
    }
    public void test_nullCompare4() {
        compilerTest();
    }
    public void test_param_capture() {
        compilerTest();
    }
    public void test_arrayne() {
        compilerTest();
    }

    // TODO we need to match the iteration order over maps that
    // v8 uses to pass this test
//    public void test_maps1() {
//        compilerTest();
//    }

    public void test_mathtest() {
        compilerTest();
    }
    public void test_null3() {
        compilerTest();
    }
    public void test_bug36() {
        compilerTest();
    }
    public void test_protocompanion() {
        compilerTest();
    }
    public void test_branchundef() {
        compilerTest();
    }
    public void test_strpluseq() {
        compilerTest();
    }
    public void test_strindex() {
        compilerTest();
    }
    public void test_null5() {
        compilerTest();
    }
    public void test_parseInt_1() { compilerTest(); }
    public void test_toplevel_intrinsics() {
        compilerTest();
    }
    public void test_set_field_of_array_object()  {
        compilerTest();
    }
    public void test_floatround() {
        compilerTest();
    }
//    public void test_continue() {
//        compilerTest();
//    }

//    public void test_infix_in() {
//        compilerTest();
//    }
    public void test_envplusplus() {
        compilerTest();
    }
    public void test_badloopcode() {
        compilerTest();
    }
    public void test_share_prototype() {
        compilerTest();
    }
    public void test_negatenull() {
        compilerTest();
    }
    public void test_method_undefined() {
        compilerTest();
    }
    public void test_falsy2() {
        compilerTest();
    }

    public void test_falsy3() {
        compilerTest();
    }

    public void test_recursivelist() {
        compilerTest();
    }
    public void test_repro199() {
        compilerTest();
    }
    public void test_floatarraywithint() {
        compilerTest();
    }
    public void test_floatpp() {
        compilerTest();
    }
}
