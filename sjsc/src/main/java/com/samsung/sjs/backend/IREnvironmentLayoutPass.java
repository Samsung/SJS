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
/*
 * Calculate environment layout for closures that capture variables
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;
import java.util.*;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.backend.*;
public final class IREnvironmentLayoutPass extends VoidIRVisitor {
    // Global variables
    private final Scope globals;
    private final Stack<Set<Var>> captures;
    private final Set<Var> maincaptures;
    private final Stack<Function> functions;
    private final boolean debug;
    private Script root;
    private Scope currScope;
    public IREnvironmentLayoutPass(Script scr, boolean debug) {
        captures = new Stack<Set<Var>>();
        maincaptures = new HashSet<Var>();
        functions = new Stack<Function>();
        this.debug = debug;
        // scr.getScope() will become main()'s scope.  It's parent is the environment.
        globals = scr.getScope().getParentScope();
        assert (globals != null);
        currScope = scr.getScope();
    }

    public Set<Var> getMainCaptures() { return maincaptures; }   

    @Override
    public Void visitFunction(Function fn) {
        if (debug) {
            System.err.println("Computing IR closure environment for "+fn.getName()+":");
            System.err.println(fn.toSource(0));
        }
        functions.push(fn);
        Set<Var> capt = new HashSet<Var>();
        captures.push(capt);
        currScope = fn.getScope();

        fn.getBody().accept(this);

        // capt still points to the current function's captured set,
        // which we now use to compute an environment layout for fn
        EnvironmentLayout l = new EnvironmentLayout();
        for (Var n : capt) {
            l.addCapturedVariable(n.getIdentifier());
        }
        fn.setLayout(l);
        if (debug) {
            System.err.println("Environment layout for ["+fn.getName()+"]: "+l.toString());
        }
        //layouts.put(fn, l);

        // clean up the stacks
        captures.pop();
        Function cur = functions.pop();
        assert (cur == fn);
        currScope = currScope.getParentScope();
        assert (functions.isEmpty() || functions.peek().getScope() == currScope);
        // If fn is nested inside another function expression, than anything
        // captured by this closure is also captured by the enclosing
        // function expression(s).  Propagation past the closest enclosing
        // function will be handled by that functions visitFunction cleanup.
        if (!captures.empty()) {
            // TODO: Oh, we should only add captured variables that are not locals in the next
            // function up the stack.
            //captures.peek().addAll(capt);
            for (Var n : capt) {
                // Don't propagate capture of a local variable *OR* the function's environment in
                // the case of direct calls --- we're early enough in the chain that environment
                // variables aren't explicitly in the scope chains
                if (!functions.peek().getScope().isLocallyBound(n) && !functions.peek().getEnvironmentName().equals(n.getIdentifier())) {
                    if (debug) {
                        System.err.println(">>>> propogating capture of "+n+" out a level to enclosing function of "+cur.getName());
                    }
                    captures.peek().add(n);
                } else {
                    if (debug) {
                        System.err.println(">>>> noting capture of local variable "+n.getIdentifier()+" of function "+functions.peek().getName());
                    }
                    functions.peek().noteCapturedVariable(n);
                }
            }
        } else {
            for (Var n : capt) {
                maincaptures.add(n);
            }
        }
        if (debug) {
            System.err.println("IR Env Layout Result for "+fn.getName()+": "+l);
        }
        return null;
    }

    private Function capturesEnclosingFunctionEnv(Var name) {
        for (Function f : functions) {
            if (f != functions.peek()) {
                if (f.getScope().isLocallyBound(name)) {
                    return null; // Would capture local in outer scope, a function further out
                }
                if (f.getName().equals(name.getIdentifier())) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public Void visitFunctionCall(FunctionCall node) {
        // Identify direct calls to the current function or an enclosing function,
        // in order to mark that function's environment captured to support direct calls
        // Don't bother checking for direct calls if we're in the top level 
        if (!functions.isEmpty() && node.getTarget() instanceof Var) {
            Var v = (Var)node.getTarget();
            String name = v.getIdentifier();
            Function maybeF = null;
            if (!currScope.isLocallyBound(v) && functions.peek().getName().equals(name)) {
                maybeF = functions.peek();
            } else {
                maybeF = capturesEnclosingFunctionEnv(v);
            }
            if (maybeF != null) {
                // Direct call!  Don't visit the target for capture
                node.setDirectCall(maybeF);
                if (debug) {
                    System.err.println("ID'd direct call in function ["+functions.peek().getName()+"]: target ["+v.toSource(0)+"] of ["+node.toSource(0)+"]h");
                }
                // But if this isn't a recursive call, we need to mark the outer function's
                // environment as a capture
                if (!functions.peek().getName().equals(v.getIdentifier())) {
                    captures.peek().add(mkVar(maybeF.getEnvironmentName()));
                }
            } else {
                if (debug) {
                    System.err.println("Function called via identifier ["+v.getIdentifier()+"] within function ["+functions.peek().getName()+"] is not direct-call optimization candidate.");
                }
                node.getTarget().accept(this);
            }
        } else {
        	node.getTarget().accept(this);
        }
        for (Expression e : node.getArguments()) {
            e.accept(this);
        }
        return null;
    }

    // TODO: HANDLE Let-In construct!  It has its own scope, and the function stack traversal will
    // miss let-bound variables...

    @Override
    public Void visitVar(Var v) {
        // Before we get here, visitFunctionCall ensures we skip this if
        // this would be a direct reference to an enclosing function, and therefore
        // this is not a direct call (not even recursive)
        //
        // Global variable accesses aren't captures
        if (!functions.isEmpty()) {
            if (!currScope.isLocallyBound(v)) {
                // Variable capture!  
                // TODO: Right now top-level program declarations aren't actually in an environment.
                // This works for now, but is fragile, and eneds to be fixed.
                if (globals.isLocallyBound(v)) {
                    // Globals don't need to be captured
                    if (debug) {
                        System.err.println("Function ["+functions.peek().getName()+"] *skipping* capture ["+v.getIdentifier()+"] (global)");
                    }
                } else {
                    // capture variable from enclosing scope
                    if (debug) {
                        System.err.println("Function ["+functions.peek().getName()+"] captures ["+v.getIdentifier()+"]");
                    }
                    captures.peek().add(v);
                }
            }
        }
        return null;
    }

}
