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
package com.samsung.sjs.constraintsolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mozilla.javascript.ast.AstNode;

import com.samsung.sjs.typeconstraints.EnvironmentDeclarationTerm;
import com.samsung.sjs.typeconstraints.FunctionParamTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.IndexedTerm;
import com.samsung.sjs.typeconstraints.KeyTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.typeconstraints.TypeParamTerm;
import com.samsung.sjs.types.Type;

/**
 * The solution obtained from a successful type inference run.
 */
public class TypeAssignment {

    private final Map<ITypeTerm, Type> termTypeMap;

    // TODO: it's ugly to keep this guy around.
    // We should do a bit of refactoring to break this dependency.
    private final TypeConstraintFixedPointSolver solver;

    public TypeAssignment(Map<ITypeTerm, Type> termTypeMap, TypeConstraintFixedPointSolver solver) {
        this.termTypeMap = termTypeMap;
        this.solver = solver;
    }

    public Type typeOfTerm(ITypeTerm term) {
        if (term instanceof EnvironmentDeclarationTerm) {
            return term.getType();
        }
        return termTypeMap.computeIfAbsent(term, this::computeTypeOfTerm);
    }

    private Type computeTypeOfTerm(ITypeTerm term) {
        // TODO: this doesn't work if term.getType() is a TypeVar.
        // We need a cleaner API here.
        solver.substituteTypeVars(term.getType());
        TypeConstraintFixedPointSolver.replaceNestedAny(term.getType());
        return term.getType();
    }

    public MROMRWVariable getMROMRWVarForTerm(ITypeTerm term) {
        return solver.getMROMRWVarForTerm(term);
    }

    public Map<AstNode, Type> nodeTypes() {
        Map<AstNode, Type> map = new LinkedHashMap<>();
        for (ITypeTerm term : termTypeMap.keySet()) {
            if (!(term instanceof TypeConstantTerm) && term.getNode() != null){
                map.put(term.getNode(), typeOfTerm(term));
            }
        }
        return map;
    }

    public String debugString() {
        List<String> list = new ArrayList<>();
        for (ITypeTerm term : termTypeMap.keySet()){
            if (!(term instanceof TypeParamTerm)
                    && !(term instanceof TypeConstantTerm)
                    && !(term instanceof EnvironmentDeclarationTerm)
                    && !(term instanceof IndexedTerm)
                    && !(term instanceof KeyTerm)
                    && !(term instanceof FunctionParamTerm)
                    && !(term instanceof FunctionReturnTerm)
                    && !(term instanceof PropertyAccessTerm)) {
                list.add(/* term.getClass().getSimpleName() + " " + */term
                        .toString() + " --> " + term.getType() + "\n");
            }
        }
        Collections.sort(list);
        String result = "";
        for (String s : list){
            result += s;
        }
        return result;
    }

    public String solutionAsString(){
        List<String> list = new ArrayList<>();
        Map<AstNode,Type> extSolution = nodeTypes();
        for (AstNode node : extSolution.keySet()){
            if (node != null){
                list.add(node.toSource() + " --> " + extSolution.get(node) + "\n");
            }
        }
        Collections.sort(list);
        String result = "";
        for (String s : list){
            result += s;
        }
        return result;
    }

    public String mroMRWAsString() {
        Map<AstNode, MROMRWVariable> externalMROMRW = solver.getExternalMROMRW();
        return externalMROMRW.keySet().stream()
                .map(n -> n.toSource() + " (line " + n.getLineno() + ") --> " + externalMROMRW.get(n).sortedString())
                .sorted()
                .collect(Collectors.joining("\n"));
    }

}
