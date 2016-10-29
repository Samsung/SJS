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

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.constraintsolver.SolverException;

/**
 * Tests for the type constraint generator
 *
 * @author ftip
 *
 */
public class ConstraintGenTest extends SJSTest
{
	public final static boolean DEBUG = false;


    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ConstraintGenTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ConstraintGenTest.class );
    }

    public void setUp(){
    	System.out.println("------------------");
    }

    protected String getTestDir() {
        return "constraints/";
    }

    private JSEnvironment setupEnvironment(){
    	try {
    		JSEnvironment jsEnv = new JSEnvironment();
			URL jsenv = Compiler.class.getClass().getResource("/environment.json");
			assert (jsenv != null);
			jsEnv.includeFile(FileSystems.getDefault().getPath(jsenv.getPath()));
			return jsEnv;
		} catch (IOException e){
			throw new Error("Exception occurred while reading environment.json");
		}
    }

    /**
     * Comparison test: retrieves the input script and expected output
     * (identified using name of current test), computes the actual
     * output, and asserts that the expected and actual output are the same.
     */
    public void comparisonTest() throws IOException {
    	String script = getInputScript();
   	 	JSEnvironment jsEnv = setupEnvironment();
   	 	ConstraintFactory factory = new ConstraintFactory();
   	 	ConstraintGenerator generator = new ConstraintGenerator(factory, jsEnv);
   	 	String actualOutput = generator.generateString(script);
   	 	compareWithExpectedOutput(actualOutput, ".out", "constraints");
    }

    /**
     * A test that is expected to fail with an exception
     *
     * @throws IOException
     */
    public void failingTest() throws IOException {
    	String script = getInputScript();
    	ConstraintFactory factory = new ConstraintFactory();
    	JSEnvironment jsEnv = setupEnvironment();
    	ConstraintGenerator generator = new ConstraintGenerator(factory, jsEnv);
    	boolean fail = false;
    	try {
    		generator.generateConstraints(script);
    		fail = true;
    	} catch (SolverException e){
    		String actualOutput = e.getMessage();
			System.out.println(actualOutput);
    	}
    	if (fail){
    		fail("constraint generation succeeded but was expected to fail");
    	}
    }

    /**
     * tests that the appropriate constraints are generated for simple declarations
     * of primitive types and strings
     * @throws IOException
     */
    public void test_integerAndStringConstants() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for simple declarations
     * of booleans and floats
     * @throws IOException
     */
    public void test_booleanAndFloatConstants() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for simple assignments
     * of integers
     * @throws IOException
     */
    public void test_assignments() throws IOException {
    	comparisonTest();
    }

    public void test_assignments2() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for return statements
     * @throws IOException
     */
    public void test_functionreturns() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for a simple function call
     * @throws IOException
     */
    public void test_simplecall() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for expressions constructed with built-in
     * operators
     * @throws IOException
     */
    public void test_operators() throws IOException {
    	comparisonTest();
    }

    public void test_operators2() throws IOException {
    	comparisonTest();
    }

    /**
     * tests while loops
     */
    public void test_whileloop() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for function calls (without receivers)
     * @throws IOException
     */
    public void test_functioncalls() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for arrays of integers
     * @throws IOException
     */
    public void test_intarrays() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for length operations on arrays
     * @throws IOException
     */
    public void test_intarraylength() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for infix expressions
     * @throws IOException
     */
    public void test_infix() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for for-loops
     * @throws IOException
     */
    public void test_forloop() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for arrays of integers
     * @throws IOException
     */
    public void test_intarrays_full() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for writes to arrays
     * @throws IOException
     */
    public void test_arraystore() throws IOException {
    	comparisonTest();
    }

    public void test_arraystore2() throws IOException {
    	comparisonTest();
    }

    /**
     * tests array access operations where the index is a complex expression
     *
     * @throws IOException
     */
    public void test_arrayaccess() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for parenthesized expressions
     * @throws IOException
     */
    public void test_paren() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for conditional expressions
     * @throws IOException
     */
    public void test_conditionalexpression() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for recursive function calls
     * @throws IOException
     */
    public void test_recursion() throws IOException {
    	comparisonTest();
    }

//    /**
//     * tests that the appropriate constraints are generated for type hints like TyHint.int
//     *
//     * @throws IOException
//     */
//    public void test_tyhint() throws IOException {
//    	comparisonTest();
//    }

    /**
     * tests that the appropriate constraints are generated for calls to external methods
     *
     * @throws IOException
     */
    public void test_external() throws IOException {
    	comparisonTest();
    }

    public void test_external2() throws IOException {
    	comparisonTest();
    }

    /**
     * tests that the appropriate constraints are generated for ackermann.js (slightly
     * modified to avoid incorrect use of arguments and call to nonexistent print function)
     *
     * @throws IOException
     */
    public void test_ackermann() throws IOException {
    	comparisonTest();
    }

    /**
     * Infer types for functions that are not called.
     *
     *
     * @throws IOException
     */
    public void test_notcalledfunction() throws IOException {
    	comparisonTest();
    }

    /**
     * Very simple test in which a function is assigned to a variable and then invoked
     *
     * @throws IOException
     */
    public void test_closures() throws IOException {
    	comparisonTest();
    }

    /**
     * Infer types for calls to Number.toFixed(int), as a simple example of method
     * calls on built-in types
     *
     * @throws IOException
     */
    public void test_tofixed() throws IOException {
    	comparisonTest();
    }

    /**
     * test external method Math.sqrt(float)
     * @throws IOException
     */
    public void test_externalmethods() throws IOException {
    	comparisonTest();
    }

    /**
     * test that correct scopes are used for function parameters
     *
     * @throws IOException
     * @throws SolverException
     */
    public void test_scopes() throws IOException {
    	comparisonTest();
    }

    /**
     * spectralnorm shootout benchmark
     * @throws IOException
     */
    public void test_spectralnorm() throws IOException {
    	comparisonTest();
    }

    public void test_forloop2() throws IOException {
    	comparisonTest();
    }

    /**
     * simple object literal
     */
    public void test_objectliteral1() throws IOException {
    	comparisonTest();
    }

    public void test_objectliteral2() throws IOException {
    	comparisonTest();
    }

    /**
     * Colin's simple_matmul.js
     */
    public void test_simple_matmul() throws IOException {
    	comparisonTest();
    }

    /**
     * Test two-dimensional arrays
     */
    public void test_2darray() throws IOException {
    	comparisonTest();
    }

    /**
     * Test two-dimensional arrays
     */
    public void test_matrix() throws IOException {
    	comparisonTest();
    }

    /**
     * bitwise operators
     */
    public void test_bitwiseoperators() throws IOException {
    	comparisonTest();
    }

    /**
     * tsp-ga inhouse benchmark
     */
    public void test_tsp_ga() throws IOException {
    	comparisonTest();
    }

    /**
     * ary shootout benchmark
     */
    public void test_ary() throws IOException {
    	comparisonTest();
    }

    /**
     * test Array() function
     */
    public void test_array() throws IOException {
    	comparisonTest();
    }

    public void test_array2() throws IOException {
    	comparisonTest();
    }

    /**
     * fannkuchredux shootout benchmark
     */
    public void test_fannkuchredux() throws IOException {
    	comparisonTest();
    }

    /**
     * fibo shootout benchmark
     */
    public void test_fibo() throws IOException {
    	comparisonTest();
    }

    /**
     * map literals
     */
    public void test_mapliteral1() throws IOException {
    	comparisonTest();
    }

    public void test_mapliteral2() throws IOException {
    	comparisonTest();
    }

    /**
     * for-in loops
     */
    public void test_forinloop() throws IOException {
    	comparisonTest();
    }

    /**
     * fasta shootout benchmark
     */
    public void test_fasta() throws IOException {
    	comparisonTest();
    }

    /**
     * random shootout benchmark
     */
    public void test_random() throws IOException {
    	comparisonTest();
    }

    /**
     * sieve shootout benchmark
     */
    public void test_sieve() throws IOException {
    	comparisonTest();
    }

    /**
     * nsieve shootout benchmark
     */
    public void test_nsieve() throws IOException {
    	comparisonTest();
    }

    /**
     * object literal with methods
     */
    public void test_methodsonliteral() throws IOException {
    	comparisonTest();
    }

    /**
     * nbody shootout benchmark
     */
    public void test_nbody() throws IOException {
    	comparisonTest();
    }

    public void test_nbody_ctor() throws IOException {
    	comparisonTest();
    }

    /**
     * harmonic shootout benchmark
     */
    public void test_harmonic() throws IOException {
    	comparisonTest();
    }

	/**
	 * nsievebits shootout benchmark
	 */
	public void test_nsievebits() throws IOException {
		comparisonTest();
	}

	/**
	 * partialsums shootout benchmark
	 */
	public void test_partialsums() throws IOException {
		comparisonTest();
	}

	/**
	 * takfp shootout benchmark
	 */
	public void test_takfp() throws IOException {
		comparisonTest();
	}

	/**
	 * nestedloop shootout benchmark
	 */
	public void test_nestedloop() throws IOException {
		comparisonTest();
	}

    public void test_linecomments() throws IOException {
    	comparisonTest();
    }

    /**
     * fannkuch shootout benchmark
     */
    public void test_fannkuch() throws IOException {
    	comparisonTest();
    }

    /**
     * heapsort shootout benchmark
     */
    public void test_heapsort() throws IOException {
    	comparisonTest();
    }

    /**
     * calls to Array.push()
     */
    public void test_arraypush() throws IOException {
    	comparisonTest();
    }

    public void test_arraypush2() throws IOException {
    	comparisonTest();
    }

    public void test_arraypush3() throws IOException {
    	comparisonTest();
    }

    /**
     * lists shootout benchmark
     */
    public void test_lists() throws IOException {
    	comparisonTest();
    }

    /**
     * revcomp shootout benchmark
     */
    public void test_revcomp() throws IOException {
    	comparisonTest();
    }

    /**
     * assertions: assert::bool->void
     */
    public void test_assert() throws IOException {
	    comparisonTest();
    }

    /**
     * string-indexing
     */
    public void test_stringindex() throws IOException {
	    comparisonTest();
    }

    /**
     * strcat shootout benchmark
     */
    public void test_strcat() throws IOException {
    	comparisonTest();
    }

    /**
     * sumcol shootout benchmark
     */
    public void test_sumcol() throws IOException {
    	comparisonTest();
    }

    /**
     * mandelbrot shootout benchmark
     */
    public void test_mandelbrot() throws IOException {
    	comparisonTest();
    }

    /**
     * overloaded methods of builtin types
     */
    public void test_overloading() throws IOException {
    	comparisonTest();
    }

    /**
     * hash shootout benchmark
     */
    public void test_hash() throws IOException {
    	comparisonTest();
    }

    /**
     * lambda1 end-to-end test
     */
    public void test_lambda1() throws IOException {
    	comparisonTest();
    }

    /**
     * lambda2 end-to-end test
     */
    public void test_lambda2() throws IOException {
    	comparisonTest();
    }

    /**
     * lambda5 end-to-end test
     */
    public void test_lambda5() throws IOException {
    	comparisonTest();
    }

    /**
     * consoleerror end-to-end test
     */
    public void test_consoleerror() throws IOException {
    	comparisonTest();
    }

    /**
     * consolewarn end-to-end test
     */
    public void test_consolewarn() throws IOException {
    	comparisonTest();
    }

    /**
     * arraylit4 end-to-end test
     */
    public void test_arraylit4() throws IOException {
    	comparisonTest();
    }

    /**
     * One-shot closure with method
     */
    public void test_oneshotclosure() throws IOException {
    	comparisonTest();
    }

    /**
     * duplicate "var"
     */
    public void test_duplicatevar() throws IOException {
    	comparisonTest();
    }

    /**
     * property access
     */
    public void test_propertyaccess() throws IOException {
    	comparisonTest();
    }

    /**
     * null in comparison expressions
     */
    public void test_nullincomparison() throws IOException {
    	comparisonTest();
    }

    /**
     * methods
     */
    public void test_methods() throws IOException {
    	comparisonTest();
    }

    /**
     * objlit4
     */
    public void test_objlit4() throws IOException {
    	comparisonTest();
    }

    /**
     * pidigits shootout benchmark
     */
    public void test_pidigits() throws IOException {
    	comparisonTest();
    }

    /**
     * switch
     */
    public void test_switch() throws IOException {
    	comparisonTest();
    }

    /**
     * uses of the external String object/function
     */
    public void test_string() throws IOException {
    	comparisonTest();
    }

    /**
     * more uses of the external String object/function
     */
    public void test_string2() throws IOException {
    	comparisonTest();
    }

    /**
     * Satish's calculator
     */
    public void test_calc() throws IOException {
    	comparisonTest();
    }

    public void test_calcrefactored() throws IOException {
    	comparisonTest();
    }

    /**
     * print
     */
    public void test_print() throws IOException {
  	    comparisonTest();
    }

  /**
   * null
   */
    public void test_null() throws IOException {
	   comparisonTest();
    }

    public void test_calc2() throws IOException {
    	comparisonTest();
    }

    /**
     * simple constructor
     */
    public void test_constructor1() throws IOException {
  	    comparisonTest();
    }

    public void test_constructors1() throws IOException {
  	    comparisonTest();
    }

    public void test_rectype_ctor1() throws IOException {
  	    comparisonTest();
    }

    public void test_rectype_ctor2() throws IOException {
  	    comparisonTest();
    }

    public void test_thisinconstructormethod() throws IOException {
  	    comparisonTest();
    }

    public void test_functionreassign() throws IOException {
  	    comparisonTest();
    }

    public void test_unknownglobal() throws IOException {
    	failingTest();
    }

    public void test_intersectiontype() throws IOException {
    	comparisonTest();
    }

//    public void test_constructor2() throws IOException {
//    	comparisonTest();
//    }
//

    public void test_proto1fixed() throws IOException {
    	comparisonTest();
    }

    public void test_proto2fixed() throws IOException {
    	comparisonTest();
    }

    public void test_proto3fixed() throws IOException {
    	comparisonTest();
    }

    public void test_return() throws IOException {
    	comparisonTest();
    }

    public void test_methodcall() throws IOException {
    	comparisonTest();
    }

    public void test_methodcall2() throws IOException {
    	comparisonTest();
    }

    public void test_methodupdate() throws IOException {
    	comparisonTest();
    }

    public void test_undefined() throws IOException {
    	comparisonTest();
    }

    public void test_voidoperator() throws IOException {
    	comparisonTest();
    }

    public void test_multipleundefined() throws IOException {
    	comparisonTest();
    }

    public void test_maps1() throws IOException {
    	comparisonTest();
    }

    public void test_arrayne() throws IOException {
    	comparisonTest();
    }

    public void test_condexpr2() throws IOException {
    	comparisonTest();
    }

    public void test_nestedpropaccess() throws IOException {
    	comparisonTest();
    }

    public void test_undefPtr() throws IOException {
    	comparisonTest();
    }

    public void test_cons_and_method_update4() throws IOException {
    	comparisonTest();
    }

    public void test_bitwisenot() throws IOException {
    	comparisonTest();
    }

    public void test_typeof() throws IOException {
    	comparisonTest();
    }

    public void test_prepost() throws IOException {
    	comparisonTest();
    }

    public void test_scoping() throws IOException { //  FCSSJS-58
    	comparisonTest();
    }

    public void test_nestedthis() throws IOException { //  FCSSJS-53
    	comparisonTest();
    }

    public void test_set_field_of_array_object() throws IOException {
        comparisonTest();
    }

    public void test_continue() throws IOException {
        comparisonTest();
    }

    public void test_infix_in() throws IOException {
        comparisonTest();
    }

//    public void test_navier_stokes() throws IOException { //  FCSSJS-63
//    	comparisonTest();
//    }

//    // TODO: for testing only..
//    public void test_test() throws IOException {
//    	comparisonTest();
//    }
}
