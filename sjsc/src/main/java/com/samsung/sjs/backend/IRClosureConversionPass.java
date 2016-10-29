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
 * Class to perform closure conversion on our internal IR
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;
import java.util.*;

import com.samsung.sjs.types.*;

import com.samsung.sjs.backend.asts.ir.*;

public final class IRClosureConversionPass extends IRTransformer
{
    public static boolean isEnvName(String s) {
        return s.startsWith(envNamePrefix);
    }
    private static final String envNamePrefix = "__env";
    private final Stack<Function> nesting;
    // topLevels is a stack of function nodes that must be hoisted to the top level.
    // We push onto the stack upon <i>finishing</i> processing, so nested
    // function scopes will be pushed deeper in the stack than enclosing functions.
    // Then when processing is complete (this action is triggered by the class
    // driving visitation calling reattachFunctions()) we push the nodes onto the
    // front of the root node, reversing the order, ensuring that the resulting
    // AST defines each function before its use.
    private final Stack<Function> toplevels;
    private final Script root;
    private int fnsalt;
    private final Map<Function,String> names;
    private final boolean debug;
    private Set<Var> maincaptures;
    private String main_name;
    public IRClosureConversionPass(Script r, 
                                  Set<Var> maincaptures,
                                 boolean debug,
                                 String main_name) {
        super(r);
        nesting = new Stack<Function>();
        root = r;
        toplevels = new Stack<Function>();
        fnsalt = 0;
        this.debug = debug;
        names = new HashMap<Function,String>();
        this.maincaptures = maincaptures;
        this.main_name = main_name;
    }

    public Script convert() {
        return visitScript(root);
    }

    public void reattachFunctions(Script s2) {
        while(!toplevels.isEmpty()) {
            s2.prefixStatement(mkExpressionStatement(toplevels.pop()));
        }
    }

    private final EnvironmentLayout getCurrentLayout() {
        return nesting.peek().getEnvLayout();
    }
    private final Function getCurrentFunction() {
        return nesting.peek();
    }

    @Override
    public IRNode visitFunction(Function fn) {
        /*
         * TODO: Optimizations:
         * - Inline when immediately invoking (non-recursive) function expression
         *
         * Some of these should be done by earlier passes, not this one.
         */

        // Generate the new function
        int mysalt = fnsalt++;
        String newname = (fn.getName()+"__"+String.valueOf(mysalt));
        if (debug) {
            System.err.println("Renaming function ["+fn.getName()+"] to ["+newname+"]");
        }
        //try {
        //    throw new UnsupportedOperationException();
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
        Function newfn = new Function(curScope, newname, fn.getEnvironmentName(), fn.getReturnType());
        names.put(fn, newname);
        Var envname = mkVar(fn.getEnvironmentName());
        envname.setType(new EnvironmentType());
        newfn.addParameter(envname, new EnvironmentType());
        if (fn.isConstructor()) {
            newfn.markConstructor();
            // TODO: This really ought to be a pseudoliteral
            newfn.addParameter(mkVar("__this"), fn.getReturnType());
        } else if (fn.isMethod()) {
            newfn.markMethod();
            //newfn.addParameter(mkVar("__this"), lastctor);
            // TODO: Hack while we wait for proper distinction between attached and unattached
            // method types in the frontend
            Type thistype = Types.mkObject(new LinkedList<>());//((MethodType)fn.getType()).receiverType();
            assert(thistype != null);
            newfn.addParameter(mkVar("__this"), thistype);
        } else {
            // Experiment with single calling convention
            Type thistype = Types.mkObject(new LinkedList<>());
            newfn.addParameter(mkVar("__this"), thistype);
        }
        for (int i = 0; i < fn.nargs(); i++) {
            newfn.addParameter(fn.argName(i), fn.argType(i));
        }
        newfn.setCaptured(fn.getCaptured());
        newfn.setType(fn.getType());
        newfn.setLayout(fn.getEnvLayout());
        newfn.setType(fn.getType());
        // Update current newfnunction innewfno
        //nesting.push(newfn);
        // We push the old fn for name comparisons in potentially-recursive calls
        nesting.push(fn);
        curScope = newfn.getScope();
        oldScope = fn.getScope();
        for (Statement s : (Block)fn.getBody().accept(this)) {
            newfn.addBodyStatement(s);
        }
        nesting.pop();
        curScope = curScope.getParentScope();
        oldScope = oldScope.getParentScope();

        // getCurrentLayout() now gets the layout for the function containing
        // the one we just transformed

        // Return a closure allocation
        AllocClosure alloc = mkAllocClosure(newfn);
        alloc.setType(newfn.getType()); // Technically we're giving functions and closures the same type here
        // Get environment layout for the function in the closure
        EnvironmentLayout l = newfn.getEnvLayout();
        if (!nesting.isEmpty()) {
            for (int i = 0; i < l.size(); i++)  {
                // we know the original source name for the captured arguments,
                // but some of those may in fact be captured from further
                // enclosing scopes...
                Var v = mkVar(l.getName(i));
                v.setType(new EnvironmentType());
                alloc.addCapturedVariable(v.accept(this).asExpression());
            }
        } else {
            // No enclosing scopes for fn, so we're at the top level
            // Technically we could transform and it *should* be a no-op
            for (int i = 0; i < l.size(); i++) {
                Var v = mkVar(l.getName(i));
                v.setType(new EnvironmentType());
                alloc.addCapturedVariable(v.accept(this).asExpression());
            }
        }

        if (debug) {
            System.err.println("Translation of: "+fn.toSource(0));
            System.err.println("Translation result: "+newfn.toSource(0));
            System.err.println("Transformed fn expression to allocate closure: "+alloc.toSource(0));
        }

        // Push flattened result into new tree
        toplevels.push(newfn);

        // TODO: Rewrite once FunctionType is a proper class
        // Generate a type for the new function, and for its __env argument
        //Type oldType = types.get(fn);
        //types.put(newfn, oldType);
        //types.put(o, oldType);

        return alloc;
    }

    @Override
    public IRNode visitFunctionCall(FunctionCall node) {
        if (node.getTarget() instanceof Var && !nesting.isEmpty()) {
            Var n = (Var)node.getTarget();
            if (node.isDirectCall()) {
                // It looks like iteration on the stack runs top->bottom
                for (Function f : nesting) {
                    if (f.getName().equals(n.getIdentifier())) {
                        String newf = names.get(f);
                        if (debug) {
                            System.err.println("************Translating call to "+n.getIdentifier()+" into "+newf);
                        }
                        Var n2 = mkVar(newf);
                        n2.setType(node.getDirectCallTarget().getType());
                        FunctionCall c = mkFunctionCall(n2);
                        c.setDirectCall(node.getDirectCallTarget());
                        c.setType(node.getType());
                        for (Expression child : node.getArguments()) {
                            c.addArgument(child.accept(this).asExpression());
                        }
                        return c;
                    }
                }
            }
        }
        // otherwise...
        return super.visitFunctionCall(node);
    }

    @Override
    public IRNode visitVar(Var n) {
        if (n.getType() == null) {
            System.err.println("ERROR: Var with missing type: "+n.getIdentifier());
        }
        assert(n.getType() != null);
        if (nesting.isEmpty()) return n; // top level variables
        if (getCurrentLayout().contains(n.getIdentifier())) {
            int offset = getCurrentLayout().getOffset(n.getIdentifier());
            Var envname = mkVar(getCurrentFunction().getEnvironmentName());
            envname.setType(new EnvironmentType());
            ArrayIndex e = new ArrayIndex(envname, //envNamePrefix+getCurrentFunction().getName()),
                                          mkIntLiteral(offset));
            if (debug) {
                System.err.println("[["+n.getIdentifier()+"]] = "+envNamePrefix+getCurrentFunction().getName()+"["+offset+"]");
            }
            assert (n.getType() != null);
            e.setType(n.getType());
            return e;
        } else {
            if (debug) {
                System.err.println("environment of "+getCurrentFunction().getName()+"("+getCurrentLayout().toString()+") doesn't contain "+n.getIdentifier());
            }
            return n;
        }
    }

    @Override
    public Statement visitForLoop(ForLoop node) {
        IRNode init = null;
        if (node.getInitializer() != null) init = node.getInitializer().accept(this);
        Expression cond = null;
        if (node.getCondition() != null) cond = node.getCondition().accept(this).asExpression();
        Expression incr = null;
        if (node.getIncrement() != null) incr = node.getIncrement().accept(this).asExpression();
        Statement body = node.getBody().accept(this).asStatement();
        ForLoop l = mkForLoop(init, cond, incr);
        l.setBody(body);
        return l;
    }

    @Override
    public Script visitScript(Script scr) {
        curScope = scr.getScope(); // curScope is main()'s scope
        assert(curScope.getParentScope() != null);
        Block toplevelstatements = (Block)visitBlock(scr.getBody());
        Function main = mkFunction(curScope.getParentScope(), main_name, Types.mkInt());
        for (Var n : maincaptures) {
            main.noteCapturedVariable(n);
        }
        main.setScope(curScope);
        Type main_count_type = Types.mkInt();
        Var main_count_var = freshVar(main.getScope(), main_count_type);
        final List<Type> mainargs = new LinkedList<Type>();
        mainargs.add(Types.mkInt());
        //mainargs.add(new CRuntimeArray(Types.mkString()));
        final List<String> mainnames = new LinkedList<String>();
        mainnames.add(main_count_var.getIdentifier());
        //mainnames.add("arguments");
        main.setType(Types.mkFunc(Types.mkInt(), mainargs, mainnames));
        main.addParameter(main_count_var, Types.mkInt());
        //main.addParameter(mkVar("arguments"), new CRuntimeArray(Types.mkString()));
        main.setBody(toplevelstatements);
        main.addBodyStatement(new Return(new IntLiteral(0)));
        Block b = mkBlock();
        b.addStatement(mkExpressionStatement(main));
        Script s2 = new Script(b, curScope.getParentScope()); // new script's scope is environment, since main's is contained in main()
        reattachFunctions(s2);
        return s2;
    }
}
