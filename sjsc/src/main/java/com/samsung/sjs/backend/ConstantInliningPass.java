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
 * Infer const-ness for var declarations, and replace uses when appropriate.
 *
 * This is not so useful within a local scope, but it:
 * (1) reduces the number of variables captured in closures
 * (2) increases the number of switch statements we can emit as C switch statements
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import java.util.*;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.CompilerOptions;

public class ConstantInliningPass extends IRTransformer {
    // We maintain two maps:
    // candidates_by_scope is a per-scope map of variable names to constant initializers.  These
    // variables *might* be constants.
    // blacklist_by_scope is a per-scope set of modified variables.  We need this second map because
    // in some exceptionally poorly behaved programs, we see writes to a variable before it is
    // initialized!
    protected final Map<Scope,Map<String,Expression>> candidates_by_scope;
    protected final Map<Scope,Set<String>> blacklist_by_scope;
    protected final CompilerOptions options;
    protected final Script script;
    protected boolean did_something;

    public ConstantInliningPass(CompilerOptions opts, Script s) {
        super(s);
        this.options = opts;
        this.candidates_by_scope = new HashMap<>();
        this.blacklist_by_scope = new HashMap<>();
        this.script = s;
        s.accept(new ConstantInference());
        did_something = false;
    }

    public boolean didSomething() { return did_something; }

    @Override
    public Script visitScript(Script node) {
        assert (node == script);
        return (Script)super.visitScript(node);
    }

    @Override
    public IRNode visitVar(Var node) {
        Map<String,Expression> m = candidates_by_scope.get(oldScope.getContainingScope(node));
        Set<String> blacklist = blacklist_by_scope.get(oldScope.getContainingScope(node));
        //assert (m != null);
        if (m == null) {
            return node;
        }
        if (m.containsKey(node.getIdentifier()) && !blacklist.contains(node.getIdentifier())) {
            if (options.debug())
                System.err.println("Replacing ["+node.toSource(0)+"] with ["+m.get(node.getIdentifier()).toSource(0)+"]");
            did_something = true;
            return m.get(node.getIdentifier());
        }
        return node;
    }
    @Override
    public IRNode visitVarDecl(VarDecl node) {
        // Make sure we remove declarations of the variables we're inlining
        Map<String,Expression> m = candidates_by_scope.get(oldScope.getContainingScope(node.getVar()));
        Set<String> blacklist = blacklist_by_scope.get(oldScope.getContainingScope(node.getVar()));
        if (m.containsKey(node.getVar().getIdentifier()) && !blacklist.contains(node.getVar().getIdentifier())) {
            if (options.debug())
                System.err.println("Removing declaration of "+node.getVar().getIdentifier());
            did_something = true;
            return mkCompoundStatement(); // ~ EmptyStatement
        } else {
            return super.visitVarDecl(node);
        }
    }

    private class ConstantInference extends VoidIRVisitor {
        // Note that this scope is not stable across trees
        private Scope currentScope;
        @Override
        public Void visitScript(Script node) {
            currentScope = node.getScope();
            Map<String,Expression> global_constants = new HashMap<>();
            if (options.debug())
                System.err.println("Initializing map for global scope: "+currentScope.getParentScope());
            candidates_by_scope.put(currentScope.getParentScope(), global_constants);
            blacklist_by_scope.put(currentScope.getParentScope(), new HashSet<>());
            Map<String,Expression> constants = new HashMap<>();
            if (options.debug())
                System.err.println("Initializing map for script scope: "+currentScope);
            candidates_by_scope.put(currentScope, constants);
            blacklist_by_scope.put(currentScope, new HashSet<>());
            return super.visitScript(node);
        }
        @Override
        public Void visitFunction(Function node) {
            Scope oldScope = currentScope;
            currentScope = node.getScope();

            Map<String,Expression> constants = new HashMap<>();
            if (options.debug())
                System.err.println("Initializing map for function scope: "+currentScope+" ("+node.getName()+")");
            candidates_by_scope.put(currentScope, constants);
            blacklist_by_scope.put(currentScope, new HashSet<>());
            super.visitFunction(node);

            currentScope = oldScope;
            return null;
        }
        private void addCandidate(Var x, Expression val) {
            Map<String,Expression> m = candidates_by_scope.get(currentScope);
            if (!m.containsKey(x.getIdentifier())) {
                m.put(x.getIdentifier(), val);
            }
            if (!currentScope.isLocallyBound(x)) {
                System.err.println(currentScope+" does not bind "+x.getIdentifier()+" but we're marking it possibly const there!");
            }
            assert (currentScope.isLocallyBound(x));
        }
        private void removeCandidate(Var x) {
            // As far as the docs suggest, this should iterate top down, which corresponds to
            // proper scope nesting
            Scope modified = currentScope.getContainingScope(x);
            Map<String,Expression> m = candidates_by_scope.get(modified);
            if (m.containsKey(x.getIdentifier())) {
                m.remove(x.getIdentifier());
            }
            blacklist_by_scope.get(modified).add(x.getIdentifier());
        }
        @Override
        public Void visitVarDecl(VarDecl node) {
            Expression init = node.getInitialValue();
            if (init != null && init.isConst()) {
                addCandidate(node.getVar(), init);
            }
            // This doesn't cause issues with mutually-referential declarations because
            // variables aren't considered constants (this pass must be iterated)
            return super.visitVarDecl(node);
        }
        @Override
        public Void visitVarAssignment(VarAssignment node) {
            if (node.getAssignedVar().isVar()) {
                removeCandidate(node.getAssignedVar().asVar());
            }
            return super.visitVarAssignment(node);
        }
        @Override
        public Void visitUnaryOp(UnaryOp node) {
            if (node.getExpression().isVar()) {
                if (node.getOp().equals("--") || node.getOp().equals("++")) {
                    removeCandidate(node.getExpression().asVar());
                }
            } else {
                super.visitUnaryOp(node);
            }
            return null;
        }
        @Override
        public Void visitBinaryOp(BinaryOp node) {
            if (node.getLeft().isVar()) {
                switch (node.getOp()) {
                    case "+=":
                    case "-=":
                    case "*=":
                    case "/=":
                    case "%=":
                    case ">>=":
                    case "<<=":
                    case "=":
                        removeCandidate(node.getLeft().asVar());
                    default:
                }
            }
            return super.visitBinaryOp(node);
        }
        @Override
        public Void visitForInLoop(ForInLoop node) {
            // Can't inline iterator variables
            removeCandidate(node.getVariable());
            return super.visitForInLoop(node);
        }
    }
}
