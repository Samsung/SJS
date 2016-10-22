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
package com.samsung.sjs.constraintgenerator;

import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.ModuleSystem;
import com.samsung.sjs.constraintsolver.SolverException;
import com.samsung.sjs.constraintsolver.TypeAssignment;
import com.samsung.sjs.typeconstraints.CheckArityConstraint;
import com.samsung.sjs.typeconstraints.ConcreteConstraint;
import com.samsung.sjs.typeconstraints.IConstraint;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.TypeEqualityConstraint;
import com.samsung.sjs.typeerrors.Explainer;
import com.samsung.sjs.typeerrors.TypeErrorMessage;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates constraints from a Rhino AST.
 *
 * @author ftip
 *
 */
public class ConstraintGenerator {

	public ConstraintGenerator(ConstraintFactory factory, JSEnvironment jsEnv, ModuleSystem modsys){
		this.constraintVisitor = new ConstraintVisitor(factory, jsEnv, modsys, this);
	}

	private AstRoot astRoot;
	private ConstraintVisitor constraintVisitor;

	public AstRoot getAst(){
		return astRoot;
	}

	/**
	 * generate string representation of constraints for some JavaScript code
	 */
	public String generateString(String code){
		generateConstraints(code);
		Set<ITypeConstraint> constraints = getTypeConstraints();
		String stringRepresentation = stringRepresentation(constraints);
        System.out.println(stringRepresentation);
		return stringRepresentation;
	}

	/**
	 * generate string representation of constraints for some JavaScript code
	 */
	public String generateStringWithTermLineNumbers(String code){
		generateConstraints(code);
		Set<ITypeConstraint> constraints = getTypeConstraints();
		String stringRepresentation = stringRepresentationWithTermLineNumbers(constraints);
        System.out.println(stringRepresentation);
		return stringRepresentation;
	}


	/**
	 * generate constraints from JavaScript code
	 * @param code the AST to generate constraints for
	 */

	public void generateConstraints(String code) {
		Parser p = new Parser();
        astRoot = p.parse(code, "", 1);
        generateConstraints(astRoot);
    }

    /**
	 * generate constraints from JavaScript code
	 * @param code the AST to generate constraints for
	 */
	public void generateConstraints(AstRoot code) {
//        System.out.println(code.toSource(0));
        code.visit(constraintVisitor);
		List<SyntaxError> errors = constraintVisitor.getErrors();
		if (!errors.isEmpty()) {
			StringBuilder errmsg = new StringBuilder();
			errmsg.append("Found ").append(errors.size()).append(" syntax errors:");
			for (SyntaxError e : errors) {
				errmsg.append("\n  ").append(e.getMessage())
						.append(" (at line ").append(e.getNode().getLineno()).append(')');
			}
			throw new SolverException(errmsg.toString());
		}
	}

	public Set<ITypeConstraint> getTypeConstraints(){
		return constraintVisitor.getTypeConstraints();
	}

	/**
	 * String representation of the computed constraints for testing purposes.
	 * The constraints are sorted before being printed to avoid nondeterminacy
	 * in the output due to the use of hash-sets.
	 */
	public String stringRepresentation(Set<? extends IConstraint> constraints){
		List<String> strings = new ArrayList<String>();
		for (IConstraint constraint : constraints){
			strings.add(constraint.toString());
		}
		Collections.sort(strings);
		String result = "";
		for (String s : strings){
			result += s + "\n";
		}
		return result;
	}

	/**
	 * String representation of the computed constraints for testing purposes.
	 * The constraints are sorted before being printed to avoid nondeterminacy
	 * in the output due to the use of hash-sets.
	 */
	public String stringRepresentationWithSourceLineNumbers(Set<ITypeConstraint> constraints){
		List<String> strings = new ArrayList<String>();
		for (ITypeConstraint constraint : constraints){
			strings.add(sourceMapping.get(constraint) + ": " + constraint.toString());
		}
		Collections.sort(strings);
		String result = "";
		for (String s : strings){
			result += s + "\n";
		}
		return result;
	}

	public String stringRepresentationWithTermLineNumbers(Set<ITypeConstraint> constraints){
		List<String> strings = new ArrayList<>();
		for (ITypeConstraint constraint : constraints){
			String s = hasExplanation(constraint) ? "[SOFT] " : "[HARD] ";
			if (constraint instanceof ConcreteConstraint) {
				ITypeTerm term = ((ConcreteConstraint) constraint).getTerm();
				String annotatedTerm =
						term.toString() + "@" + termMapping.get(term);
				s += "CONCRETE(" + annotatedTerm + ')';
			} else if (constraint instanceof CheckArityConstraint) {
				ITypeTerm term = ((CheckArityConstraint) constraint).getTerm();
				String annotatedTerm =
						term.toString() + "@" + termMapping.get(term);
				s += "CHECK_ARITY(" + annotatedTerm + ')';
			} else {
				String op = (constraint instanceof TypeEqualityConstraint) ? "=" : "<:";
				ITypeTerm left = constraint.getLeft();
				ITypeTerm right = constraint.getRight();
				s +=
						left.toString() + "@" + termMapping.get(left) +
								op +
								right.toString() + "@" + termMapping.get(right);
			}
			strings.add(s);
		}
		Collections.sort(strings);
		StringBuilder buf = new StringBuilder();
		for (String s : strings){
			buf.append(s).append('\n');
		}
		return buf.toString();
	}

	public void addSourceLineNumber(IConstraint constraint, int lineno){
		if (!sourceMapping.containsKey(constraint)){
			sourceMapping.put(constraint, new LinkedHashSet<Integer>());
		}
		sourceMapping.get(constraint).add(lineno);
	}

	public void addTermLineNumber(ITypeTerm term, int lineno){
		if (!termMapping.containsKey(term)){
			termMapping.put(term, new LinkedHashSet<Integer>());
		}
		termMapping.get(term).add(lineno);
	}

	/**
	 * used for associating sets of source line numbers with type constraints
	 */
	private Map<IConstraint,Set<Integer>> sourceMapping = new LinkedHashMap<IConstraint,Set<Integer>>();


	/**
	 * used for associating sets of source line numbers with ITerms
	 */
	private Map<ITypeTerm,Set<Integer>> termMapping = new LinkedHashMap<ITypeTerm,Set<Integer>>();

    public Map<IConstraint, Set<Integer>> getSourceMapping() {
        return sourceMapping;
    }

    public Map<ITypeTerm, Set<Integer>> getTermMapping() {
        return termMapping;
    }

	private final Map<ITypeConstraint, Explainer> expls = new LinkedHashMap<>();
	public void mapExplanation(ITypeConstraint constraint, Explainer exp) {
		if (exp != null) {
			expls.put(constraint, exp);
		}
	}
	public TypeErrorMessage explainFailure(ITypeConstraint failure, TypeAssignment solution) {
		return expls.get(failure).explainFailure(solution);
	}
	public boolean hasExplanation(ITypeConstraint constraint) {
		return expls.containsKey(constraint);
	}
}
