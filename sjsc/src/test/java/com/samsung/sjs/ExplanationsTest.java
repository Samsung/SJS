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

import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.constraintsolver.TypeAssignment;
import com.samsung.sjs.theorysolver.SatFixingSetFinder;
import com.samsung.sjs.theorysolver.SJSTypeTheory;
import com.samsung.sjs.theorysolver.Sat4J;
import com.samsung.sjs.theorysolver.SatSolver;
import com.samsung.sjs.theorysolver.TheorySolver;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Regression tests for type error explanations.
 */
public class ExplanationsTest extends SJSTest {
    private static final String FOLDER_NAME = "explanations";

    public ExplanationsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ExplanationsTest.class);
    }

    @Override
    protected String getTestDir() {
        return FOLDER_NAME + '/';
    }

    private void go() throws IOException {
        Path inputFile = Paths.get(getInputScriptPath());
        String input = readFileIntoString(inputFile);
        JSEnvironment env = new JSEnvironment();

        InputStream jsenv = Compiler.class.getClass().getResourceAsStream("/environment.json");
        assert (jsenv != null);
        env.includeFile(jsenv);

        System.out.println("##################### " + inputFile);
        String output = explainErrors(env, input);
        System.out.println("-----------");
        System.out.print(output);

        compareWithExpectedOutput(output, ".txt", FOLDER_NAME);
    }

    private static String explainErrors(JSEnvironment env, String sourceCode) {
        AstRoot root = new Parser().parse(sourceCode, "", 1);
        SatSolver sat = new Sat4J();
        SJSTypeTheory theory = new SJSTypeTheory(env, root);
        List<Integer> hard = new ArrayList<>();
        List<Integer> soft = new ArrayList<>();
        List<ITypeConstraint> constraints = theory.getConstraints();
        for (int i = 0; i < constraints.size(); ++i) {
            (theory.hackyGenerator().hasExplanation(constraints.get(i)) ? soft : hard).add(i);
        }
        Pair<TypeAssignment, Collection<Integer>> result =
                TheorySolver.solve(
                    theory, new SatFixingSetFinder<>(sat),
                    hard, soft);
        ConstraintGenerator g = theory.hackyGenerator();
        StringBuilder buf = new StringBuilder();
        for (int broken : result.getRight()) {
            ITypeConstraint c = theory.hackyConstraintAccess().get(broken);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            g.explainFailure(c, result.getLeft()).prettyprint(new PrintStream(stream));
            buf.append(stream.toString());
        }
        return buf.toString();
    }

    // Handy one-liner to generate this list:
    // ls src/test/resources/testinput/explanations | sed 's/^/        public void test_/' | sed 's/.js/() throws Exception { go(); }/'
    public void test_abstract_literal() throws Exception { go(); }
    public void test_annex_reduced() throws Exception { go(); }
    public void test_arity() throws Exception { go(); }
    public void test_arity2() throws Exception { go(); }
    public void test_arraystore2() throws Exception { go(); }
    public void test_assignments2() throws Exception { go(); }
    public void test_bad_field_write_in_method() throws Exception { go(); }
    public void test_badoperands() throws Exception { go(); }
    public void test_badunaryoperand() throws Exception { go(); }
    public void test_cons_and_method_update1() throws Exception { go(); }
    public void test_constructor3() throws Exception { go(); }
    public void test_constructor5() throws Exception { go(); }
    public void test_constructor_loop() throws Exception { go(); }
    public void test_defaulttype1() throws Exception { go(); }
    public void test_incompatible_types() throws Exception { go(); }
    public void test_inheritance2() throws Exception { go(); }
    public void test_intfloat3() throws Exception { go(); }
    // this one is no longer a type error, as we now allow float operands for bitwise operators
//    public void test_intfloat5() throws Exception { go(); }
    public void test_intfloat6() throws Exception { go(); }
    public void test_join_error_1() throws Exception { go(); }
    public void test_join_error_2() throws Exception { go(); }
    public void test_join_error_3() throws Exception { go(); }
    public void test_join_error_4() throws Exception { go(); }
    public void test_join_error_5() throws Exception { go(); }
    public void test_linkedlist() throws Exception { go(); }
    public void test_meet_error_1() throws Exception { go(); }
    public void test_meet_error_overriding() throws Exception { go(); }
    public void test_method_attachment() throws Exception { go(); }
    public void test_methodcall() throws Exception { go(); }
    public void test_methodcall2() throws Exception { go(); }
    public void test_methodextraction() throws Exception { go(); }
    public void test_methodupdate2() throws Exception { go(); }
    public void test_methodupdate3() throws Exception { go(); }
    public void test_mrwtest10() throws Exception { go(); }
    public void test_mrwtest11() throws Exception { go(); }
    public void test_mrwtest2() throws Exception { go(); }
    public void test_mrwtest4() throws Exception { go(); }
    public void test_mrwtest9() throws Exception { go(); }
    public void test_multi_error() throws Exception { go(); }
    public void test_object_types_reject_1() throws Exception { go(); }
    public void test_overriding2() throws Exception { go(); }
    public void test_primitive_property_access() throws Exception { go(); }
    public void test_proto6() throws Exception { go(); }
    public void test_proto9() throws Exception { go(); }
    public void test_prototype_of_variable() throws Exception { go(); }
    public void test_recursive_type_missing_property() throws Exception { go(); }
    public void test_rwtest1() throws Exception { go(); }
    public void test_rwtest2() throws Exception { go(); }
    public void test_rwtest4() throws Exception { go(); }
    public void test_wrong_number_of_arguments() throws Exception { go(); }
    public void test_forinloop_bad() throws Exception { go(); }
    public void test_map_read() throws Exception { go(); }
}
