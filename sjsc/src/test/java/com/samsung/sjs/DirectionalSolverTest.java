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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.samsung.sjs.constraintsolver.TypeAssignment;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.mozilla.javascript.ast.AstNode;

import com.samsung.sjs.backend.RhinoTypeValidator;
import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.constraintsolver.DirectionalConstraintSolver;
import com.samsung.sjs.constraintsolver.SolverException;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.types.Type;

/**
 * Tests for the type constraint generator
 *
 *
 */
public class DirectionalSolverTest extends SJSTest
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DirectionalSolverTest(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DirectionalSolverTest.class );
    }

    /**
     * Identify input script an input script based on the current test's name
     * and retrieve its code. For a test named "test_foo", a file "foo.js"
     * will be expected in the directory "/test/resources/testinput/constraints".
     * If such a file does not exist or if there are problems reading it,
     * an IOException is thrown.
     *
     * @return
     * @throws java.io.IOException
     */
    private String getInputScript(boolean altSpecific, boolean withTizen) throws IOException  {
        if (withTizen) {
            return getTizenScript();
        }
        String testFolder = altSpecific ? "/testinput/altconstraints/" : "/testinput/constraints/";
        URL url = this.getClass().getResource(testFolder);
    	String path = url.getPath();
    	String inputScript = path + getName().substring(getName().indexOf('_')+1) + ".js";
    	if (DEBUG) System.out.println("input script is: " + inputScript);
    	return readFileIntoString(Paths.get(inputScript));
    }


    private String getTizenScript() throws IOException  {
        Path path = Paths.get("idl/"
                + getName().substring(getName().indexOf('_') + 1) + ".js");
        return readFileIntoString(path);
    }

    public void setUp(){
    	System.out.println("------ start " + getName() + " ------------");
    }

    public void tearDown(){
    	System.out.println("------- end " + getName() + " ------------");
    }

    private JSEnvironment setupEnvironment(boolean withTizen){
    	try {
    		JSEnvironment jsEnv = new JSEnvironment();
			URL jsenv = Compiler.class.getClass().getResource("/environment.json");
			assert (jsenv != null);
			jsEnv.includeFile(FileSystems.getDefault().getPath(jsenv.getPath()));
			if (withTizen) {
			    jsEnv.includeFile(Paths.get("idl/tizen.json"));
			}
//            jsEnv.includeFile(FileSystems.getDefault().getPath("/Users/m.sridharan/git-repos/sjs-compiler/sjsc/idl/tizen.json"));
			return jsEnv;
		} catch (IOException e){
			throw new Error("Exception occurred while reading environment.json");
		}
    }



    /**
     * Solver comparison test: retrieves the input script and expected output
     * in the solver's internal format (identified using name of current test),
     * computes the actual output, and asserts that the expected and actual
     * output are the same.
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void solverComparisonTest(boolean expectSuccess, boolean altSpecific, boolean withTizen) {
        try {
            String script = getInputScript(altSpecific, withTizen);
            ConstraintFactory factory = new ConstraintFactory();
            JSEnvironment jsEnv = setupEnvironment(withTizen);
            ConstraintGenerator generator = new ConstraintGenerator(factory, jsEnv);
            boolean generatorSuccess = false;
            try {
                generator.generateConstraints(script);
                generatorSuccess = true;
            } catch (SolverException e) {
                String explanation = e.explanation();
                System.err.println(explanation);
                assertFalse("unexpected solver failure", expectSuccess);
                compareWithExpectedExplanation(explanation);
                generatorSuccess = false;
            }
            if (!generatorSuccess) {
                assertFalse(expectSuccess);
                return;
            }
            Set<ITypeConstraint> constraints = generator.getTypeConstraints();
            System.out.println(generator.stringRepresentation(constraints));
            DirectionalConstraintSolver solver = new DirectionalConstraintSolver(constraints, factory, generator);
            boolean success;
            TypeAssignment solution = null;
            try {
                solution = solver.solve();
                success = true;
            } catch (SolverException e) {
                String explanation = e.explanation();
                System.err.println(explanation);
                assertFalse("unexpected solver failure", expectSuccess);
                compareWithExpectedExplanation(explanation);
                success = false;
            }
            assertTrue(success == expectSuccess);
            if (expectSuccess) {
                // stop comparing these internal solutions; too flaky
//                String internalSolution = solver.internalSolutionAsString();
//                compareWithExpectedOutput(internalSolution, ".sol", "altconstraints");

                String solutionString = solution.solutionAsString();
//                System.out.println(solution);
                compareWithExpectedOutput(solutionString, ".typemap", "altconstraints");

                String mroMRW = solution.mroMRWAsString();
                compareWithExpectedOutput(mroMRW, ".mromrw", "altconstraints");
                Map<AstNode, Type> typeMap = solution.nodeTypes();
                RhinoTypeValidator validator = new RhinoTypeValidator(
                        generator.getAst(), typeMap);
                validator.check();
            }
        } catch (IOException e) {
            assertFalse("Got an IOException!!" + e.getMessage(), false);
        }
    }

    private void solverComparisonTestAltSpecific() {
        solverComparisonTest(true, true, false);
    }

    private void solverComparisonTest() {
        solverComparisonTest(true, false, false);
    }

    private void failingTest() {
        solverComparisonTest(false, false, false);
    }

    private void compareWithExpectedExplanation(String actualExplanation) throws IOException  {
        Path basePath = Paths.get("src/test/resources/testoutput/altconstraints/");
        assertTrue(Files.exists(basePath));
        String testName = getName().substring(getName().indexOf('_')+1);
        Path path = Paths.get(basePath.toString(), testName + ".expl");
        if (DEBUG) System.out.println("expected output is: " + path);
        if (Files.exists(path)) {
            String expectedOutput = readFileIntoString(path);
            assertEquals(expectedOutput, actualExplanation);
        } else {
            // create the file
            Files.write(path, actualExplanation.getBytes());
        }
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj1()  {
    	solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj2()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj3()  {
        solverComparisonTest(false, true, false);
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj4()  {
        solverComparisonTest(false, true, false);
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj5()  {
        solverComparisonTest(false, true, false);
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj6()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj7()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj8()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleObj9()  {
        solverComparisonTestAltSpecific();
    }


    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleOper1()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleOper2()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleOper3()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleArray1()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleArray2()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleArray3()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleArray4()  {
        solverComparisonTest(false, true, false);
    }

    // IGNORE needs fix in generated Array constraints
//    /**
//     * @throws java.io.IOException
//     * @throws com.samsung.sjs.constraintsolver.SolverException
//     */
//    public void test_simpleArray5()  {
//        solverComparisonTest(false, true);
//    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleMap1()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleMap2()  {
        solverComparisonTest(true, true, false);
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleFunction1()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleFunction2()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleFunction3()  {
        solverComparisonTestAltSpecific();
    }

    /**
     * @throws java.io.IOException
     * @throws com.samsung.sjs.constraintsolver.SolverException
     */
    public void test_simpleFunction4()  {
        solverComparisonTestAltSpecific();
    }


    /**
     * tests that the appropriate constraints are solved for simple declarations
     * of primitive types and strings
     * @
     * @throws SolverException
     */
    public void test_integerAndStringConstants()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for simple declarations
     * of booleans and floats
     * @
     * @throws SolverException
     */
    public void test_booleanAndFloatConstants()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for simple assignments
     * of integers
     * @
     * @throws SolverException
     */
    public void test_assignments()  {
        solverComparisonTest();
    }

    public void test_assignments2()  {
        failingTest();
    }

    /**
     * tests that the appropriate constraints are solved for return statements
     * @
     * @throws SolverException
     */
    public void test_functionreturns()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for a simple function call
     * @
     */
    public void test_simplecall()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for expressions constructed with built-in
     * operators
     * @
     * @throws SolverException
     */
    public void test_operators()  {
        solverComparisonTest();
    }

    public void test_operators2()  {
        solverComparisonTest();
    }

    public void test_operators3()  {
        solverComparisonTest();
    }

    public void test_operators4()  {
        solverComparisonTest();
    }


    public void test_bitwisenot()  {
        solverComparisonTest();
    }
    public void test_prepost()  {
        solverComparisonTest();
    }
    public void test_typeof()  {
        solverComparisonTest();
    }

    /**
     * tests while loops
     */
    public void test_whileloop()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for function calls (without receivers)
     * @
     * @throws SolverException
     */
    public void test_functioncalls()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for arrays of integers
     * @
     * @throws SolverException
     */
    public void test_intarrays()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for length operations on arrays
     * @
     * @throws SolverException
     */
    public void test_intarraylength()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for infix expressions
     * @
     * @throws SolverException
     */
    public void test_infix()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for for-loops
     * @
     * @throws SolverException
     */
    public void test_forloop()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for arrays of integers
     * @
     * @throws SolverException
     */
    public void test_intarrays_full()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for writes to arrays
     * @
     * @throws SolverException
     */
    public void test_arraystore()  {
        solverComparisonTest();
    }

    public void test_arraystore2()  {
        failingTest();
    }

    /**
     * tests array access operations where the index is a complex expression
     *
     * @
     * @throws SolverException
     */
    public void test_arrayaccess()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for parenthesized expressions
     * @
     * @throws SolverException
     */
    public void test_paren()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for conditional expressions
     * @
     * @throws SolverException
     */
    public void test_conditionalexpression()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for recursive function calls
     * @
     * @throws SolverException
     */
    public void test_recursion()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for calls to external methods
     *
     * @
     * @throws SolverException
     */
    public void test_external()  {
        solverComparisonTest();
    }

    public void test_external2()  {
        solverComparisonTest();
    }

    /**
     * tests that the appropriate constraints are solved for ackermann.js (slightly
     * modified to avoid incorrect use of arguments and call to nonexistent print function)
     *
     * @
     * @throws SolverException
     */
    public void test_ackermann()  {
        solverComparisonTest();
    }

    public void test_notcalledfunction()  {
        solverComparisonTest();
    }

    /**
     * Very simple test in which a function is assigned to a variable and then invoked
     *
     * @
     */
    public void test_closures()  {
        solverComparisonTest();
    }

    /**
     * Infer types for calls to Number.toFixed(int), as a simple example of method
     * calls on built-in types
     *
     * @
     */
    public void test_tofixed()  {
        solverComparisonTest();
    }

    /**
     * Infer types for method calls on library objects, such as method Math.sqrt(float)
     *
     * @
     */
    public void test_externalmethods()  {
        solverComparisonTest();
    }

    /**
     * test that correct scopes are used for function parameters
     *
     * @
     * @throws SolverException
     */
    public void test_scopes()  {
        solverComparisonTest();
    }

    /**
     * test empty file
     *
     * @
     * @throws SolverException
     */
    public void test_empty()  {
        solverComparisonTest();
    }

    /**
     * spectralnorm shootout benchmark
     * @
     */
    public void test_spectralnorm()  {
        solverComparisonTest();
    }

    /**
     * test assignment instead of VariableDeclaration in for-loop
     * @
     */
    public void test_forloop2()  {
        solverComparisonTest();
    }

    /**
     * test two-dimensional arrays
     * @
     */
    public void test_2darray()   {
        solverComparisonTest();
    }

    /**
     * Test matrix
     */
    public void test_matrix()  {
        solverComparisonTest();
    }

    /**
     * object literals
     */
    public void test_objectliteral1()  {
        solverComparisonTest();
    }

    /**
     * object literals
     */
    public void test_objectliteral2()  {
        solverComparisonTest();
    }

    /**
     * Colin's simple_matmul.js
     */
    public void test_simple_matmul()  {
        solverComparisonTest();
    }

    /**
     * bitwise operators
     */
    public void test_bitwiseoperators()  {
        solverComparisonTest();
    }

    /**
     * tsp-ga inhouse benchmark
     */
    public void test_tsp_ga()  {
        solverComparisonTest();
    }

    public void test_array()  {
        solverComparisonTest();
    }

    public void test_array2()  {
        solverComparisonTest();
    }

    /**
     * ary shootout benchmark
     */
    public void test_ary()  {
        solverComparisonTest();
    }

    /**
     * fannkuchredux shootout benchmark
     */
    public void test_fannkuchredux()  {
        solverComparisonTest();
    }

    /**
     * fibo shootout benchmark
     */
    public void test_fibo()  {
        solverComparisonTest();
    }

    /**
     * map literals
     */
    public void test_mapliteral1()  {
        solverComparisonTest();
    }

    public void test_mapliteral2()  {
        solverComparisonTest();
    }

    /**
     * for-in loops
     */
    public void test_forinloop()  {
        solverComparisonTest();
    }

    public void test_forinloop_bad()  {
        failingTest();
    }

    /**
     * fasta shootout benchmark
     */
    public void test_fasta()  {
        solverComparisonTest();
    }

    /**
     * random shootout benchmark
     */
    public void test_random()  {
        solverComparisonTest();
    }

    /**
     * sieve shootout benchmark
     */
    public void test_sieve()  {
        solverComparisonTest();
    }

    /**
     * nsieve shootout benchmark
     */
    public void test_nsieve()  {
        solverComparisonTest();
    }

    /**
     * object literal with methods
     */
    public void test_methodsonliteral()  {
        solverComparisonTest();
    }

    /**
     * nbody shootout benchmark
     */
    public void test_nbody()  {
        solverComparisonTest();
    }

    public void test_nbody_ctor()  {
        solverComparisonTest();
    }

    /**
     * harmonic shootout benchmark
     */
    public void test_harmonic()  {
        solverComparisonTest();
    }

    /**
     * nsievebits shootout benchmark
     */
    public void test_nsievebits()  {
        solverComparisonTest();
    }

    /**
     * partialsums shootout benchmark
     */
    public void test_partialsums()  {
        solverComparisonTest();
    }

    /**
     * takfp shootout benchmark
     */
    public void test_takfp()  {
        solverComparisonTest();
    }

    /**
     * nestedloop shootout benchmark
     */
    public void test_nestedloop()  {
        solverComparisonTest();
    }

    /**
     * make sure line comments are handled
     */
    public void test_linecomments()  {
        solverComparisonTest();
    }

    /**
     * fannkuch shootout benchmark
     */
    public void test_fannkuch()  {
        solverComparisonTest();
    }

    /**
     * heapsort shootout benchmark
     */
    public void test_heapsort()  {
        solverComparisonTest();
    }

    /**
     * Calls to Array.push()
     */
    public void test_arraypush()  {
        solverComparisonTest();
    }

    public void test_arraypush2()  {
        solverComparisonTest();
    }

    public void test_arraypush3()  {
        solverComparisonTest();
    }

    public void test_arraypush4()  {
        solverComparisonTest();
    }

    /**
     * lists shootout benchmark
     */
    public void test_lists()  {
        solverComparisonTest();
    }

    /**
     * assertions: assert::bool->void
     */
    public void test_assert()  {
        solverComparisonTest();
    }

    /**
     * string-indexing
     */
    public void test_stringindex()  {
        solverComparisonTest();
    }

    /**
     * revcomp shootout benchmark
     */
    public void test_revcomp()  {
        solverComparisonTest();
    }

    /**
     * strcat shootout benchmark
     */
    public void test_strcat()  {
        solverComparisonTest();
    }

    /**
     * sumcol shootout benchmark
     */
    public void test_sumcol()  {
        solverComparisonTest();
    }

    /**
     * mandelbrot shootout benchmark
     */
    public void test_mandelbrot()  {
        solverComparisonTest();
    }

    /**
     * overloaded methods on builtin types
     */
    public void test_overloading()  {
        solverComparisonTest();
    }

    /**
     * hash shootout benchmark
     */
    public void test_hash()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: lambda1
     */
    public void test_lambda1()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: lambda2
     */
    public void test_lambda2()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: lambda5
     */
    public void test_lambda5()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: consoleerror
     */
    public void test_consoleerror()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: consolewarn
     */
    public void test_consolewarn()  {
        solverComparisonTest();
    }

    /**
     * end-to-end test: arraylit4
     */
    public void test_arraylit4()  {
        solverComparisonTest();
    }

    /**
     * One-shot closure with method.
     */
    public void test_oneshotclosure()  {
        solverComparisonTest();
    }

    /**
     * duplicate "var"
     */
    public void test_duplicatevar()  {
        solverComparisonTest();
    }

    public void test_propertyaccess()  {
        solverComparisonTest();
    }

    /**
     * null in comparison expressions
     */
    public void test_nullincomparison()  {
        solverComparisonTest();
    }

    /**
     * pidigits shootout benchmark
     */
    public void test_pidigits()  {
        solverComparisonTest();
    }

    /**
     * methods
     */
    public void test_methods()  {
        solverComparisonTest();
    }

    /**
     * objlit4
     */
    public void test_objlit4()  {
        solverComparisonTest();
    }

    /**
     * switch
     */
    public void test_switch()  {
        solverComparisonTest();
    }

    /**
     * uses of the external String object/function
     */
    public void test_string()  {
        solverComparisonTest();
    }

    /**
     * more use of the external String object/function
     */
    public void test_string2()  {
        solverComparisonTest();
    }

    public void test_print()  {
        solverComparisonTest();
    }

    public void test_null()  {
        solverComparisonTest();
    }

    public void test_null2()  {
        solverComparisonTest();
    }

    public void test_null3()  {
        solverComparisonTest();
    }

    public void test_null6()  {
        solverComparisonTest();
    }

    public void test_nonboolor1()  {
        solverComparisonTest();
    }

    public void test_nonboolor2()  {
        solverComparisonTest();
    }

    public void test_nonboolor3()  {
        solverComparisonTest();
    }

    public void test_calc2()  {
        solverComparisonTest();
    }

    public void test_callonobjlit()  {
        solverComparisonTest();
    }

    public void test_constructor1()  {
        solverComparisonTest();
    }

    public void test_constructor3()  {
        failingTest();
    }

    public void test_constructor4()  {
        solverComparisonTest();
    }

    public void test_constructor5()  {
        failingTest();
    }

    public void test_constructor6()  {
        solverComparisonTest();
    }

    public void test_constructor7()  {
        failingTest();
    }


    public void test_constructors1()  {
        solverComparisonTest();
    }

    public void test_rectype_ctor1()  {
        solverComparisonTest();
    }

    public void test_rectype_ctor2()  {
        solverComparisonTest();
    }

    public void test_thisinconstructormethod()  {
        solverComparisonTest();
    }

    public void test_functionreassign()  {
        solverComparisonTest();
    }

    public void test_unknownglobal()  {
        failingTest();
    }

    public void test_intersectiontype()  {
        solverComparisonTest();
    }

    public void test_proto1()  {
        failingTest();
     }

    public void test_proto1fixed()  {
        solverComparisonTest();
     }

     public void test_proto2()  {
         failingTest();
     }

     public void test_proto2fixed()  {
         solverComparisonTest();
     }

     public void test_proto3()  {
         failingTest();
     }

     public void test_proto3fixed()  {
         solverComparisonTest();
      }

     public void test_proto5()  {
         solverComparisonTest();
     }

     public void test_proto6()  {
         failingTest();
     }

     // not sure if this one is legal or not...
//     public void test_proto7()  {
//         solverComparisonTest();
//     }

     public void test_proto8()  {
         solverComparisonTest();
     }

     public void test_proto9()  {
         failingTest();
     }

     public void test_proto10()  {
         solverComparisonTest();
     }

     public void test_calc8()  {
         failingTest();
     }

     public void test_calcmin1()  {
         solverComparisonTest();
     }

     public void test_subProto1()  {
         solverComparisonTestAltSpecific();
     }

     public void test_returnSub()  {
         solverComparisonTest();
     }

     public void test_overriding()  {
         solverComparisonTest();
     }

     public void test_overriding2()  {
         failingTest();
     }

     public void test_methodcall()  {
         failingTest();
     }

     public void test_methodcall2()  {
         failingTest();
     }

     public void test_annex_reduced()  {
         failingTest();
     }

     public void test_undefined()  {
         solverComparisonTest();
     }

     public void test_voidoperator()  {
         solverComparisonTest();
     }

     public void test_multipleundefined()  {
         solverComparisonTest();
     }

     public void test_undefPtr()  {
         solverComparisonTest();
     }


     public void test_undefined_with_use()  {
         solverComparisonTest();
     }

     public void test_join_error_1()  {
         failingTest();
     }

     public void test_join_error_2()  {
         failingTest();
     }

     public void test_join_error_3()  {
         // TODO we get a bad error message here, due to context insensitivity
         failingTest();
     }

     public void test_join_error_4()  {
         failingTest();
     }

     public void test_join_error_5()  {
         failingTest();
     }

     public void test_meet_error_1()  {
         failingTest();
     }

     public void test_meet_error_overriding()  {
         failingTest();
     }

     public void test_fieldreadincond()  {
         solverComparisonTest();
     }
     public void test_rwtest1()  {
         failingTest();
     }

     public void test_rwtest2()  {
         failingTest();
     }

     public void test_rwtest3()  {
         solverComparisonTest();
     }

     public void test_rwtest4()  {
         failingTest();
     }


     public void test_function_method_compatibility()  {
         solverComparisonTest();
     }

     public void test_object_types_reject_1()  {
         failingTest();
     }

     public void test_rectype_reject() { failingTest();}

     public void test_cons_and_method_update1()  {
         failingTest();
     }

     public void test_cons_and_method_update3()  {
         failingTest();
     }

//     @Ignore("not sure about this one.  TODO: study more")
//     public void test_cons_and_method_update2()  {
//         solverComparisonTest();
//     }

     // this one requires abstract types, i.e., MRO/MRW
     public void test_cons_and_method_update4()  {
         solverComparisonTest();
     }

     public void test_methodupdate()  {
         solverComparisonTest();
     }

     public void test_methodupdate2()  {
         failingTest();
     }

     public void test_methodupdate3()  {
         failingTest();
     }

     public void test_intfloat1()  {
         solverComparisonTest();
     }

     public void test_intfloat2()  {
         solverComparisonTest();
     }

     public void test_intfloat3()  {
         failingTest();
     }

     public void test_intfloat4()  {
         solverComparisonTest();
     }

     public void test_intfloat5()  {
         solverComparisonTest();
     }

     public void test_intfloat6()  {
         failingTest();
     }

     public void test_float_to_string()  {
         solverComparisonTest();
     }

     public void test_arity()  {
         failingTest();
     }

     public void test_arity2()  {
         failingTest();
     }

     public void test_constructor_loop()  {
         failingTest();
     }

     public void test_mrwtest1()  {
         solverComparisonTest();
     }

     public void test_mrwtest2()  {
         failingTest();
     }

     public void test_mrwtest3()  {
         solverComparisonTest();
     }

     public void test_mrwtest4()  {
         failingTest();
     }

     public void test_mrwtest5()  {
         solverComparisonTest();
     }

     public void test_mrwtest6()  {
         solverComparisonTest();
     }

     public void test_mrwtest7()  {
         solverComparisonTest();
     }

     public void test_mrwtest8()  {
         failingTest();
     }

     public void test_mrwtest9()  {
         failingTest();
     }

     public void test_mrwtest10()  {
         failingTest();
     }

     public void test_mrwtest11()  {
         failingTest();
     }

     public void test_methodextraction()  {
         failingTest();
     }

     public void test_inheritance()  {
         solverComparisonTest();
     }

     public void test_inheritance2()  {
         failingTest();
     }

     public void test_unused_prop1()  {
         solverComparisonTest();
     }

     public void test_justreadthis()  {
         solverComparisonTest();
     }

     public void test_badoperands()  {
         failingTest();
     }

     public void test_badunaryoperand()  {
         failingTest();
     }

     public void test_badlub()  {
         solverComparisonTest();
     }

     public void test_badlub2()  {
         solverComparisonTest();
     }

//     public void test_badlub3()  {
//         solverComparisonTest();
//     }
//
//     public void test_badlub4()  {
//         solverComparisonTest();
//     }

//     public void test_badlub5()  {
//         solverComparisonTest();
//     }

     public void test_prototypenullprop()  {
         solverComparisonTest();
     }

     public void test_proto_empty_statements()  {
         solverComparisonTest();
     }

     public void test_condexprnonbool()  {
         solverComparisonTest();
     }

     public void test_isequal_stack_overflow()  {
         solverComparisonTest();
     }

     public void test_defaulttype1()  {
         failingTest();
     }

     public void test_proto2_other()  {
         solverComparisonTest();
     }

     public void test_null_inheritance()  {
         failingTest();
     }

     public void test_bad_integer_bound()  {
         failingTest();
     }

     public void test_set_field_of_array_object()  {
         solverComparisonTest();
     }

     public void test_large_constant()  {
         solverComparisonTest();
     }

     public void test_rectype_simple()  {
         solverComparisonTest();
     }

     public void test_continue()  {
         solverComparisonTest();
     }

     public void test_infix_in()  {
         solverComparisonTest();
     }

     public void test_negatenull()  {
         solverComparisonTest();
     }

     public void test_share_prototype()  {
         solverComparisonTest();
     }

     public void test_method_undefined()  {
         solverComparisonTest();
     }

     public void test_incomplete3() {
         solverComparisonTest();
     }

     public void test_invalid_proto() {
         failingTest();
     }

     public void test_dupdecl() {
         failingTest();
     }
// the remaining tests are more like "system tests",
     // but we keep them here to catch regressions
     public void test_calc()  {
         solverComparisonTest(true, true, true);
     }

     public void test_annex()  {
         solverComparisonTest(true, true, true);
     }

    @Override
    protected String getTestDir() {
        throw new RuntimeException("shouldn't call this");
    }
}
