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
 * Conversion pass taking a decorated Rhino AST to our IR
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import org.mozilla.javascript.ast.*;
import org.mozilla.javascript.*;

import com.samsung.sjs.ExternalRhinoVisitor;
import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;

import java.util.*;

// NOTE: Static import
import static com.samsung.sjs.backend.asts.ir.IRManipulator.*;

public class RhinoTypeValidator extends ExternalRhinoVisitor {

    private AstRoot source;
    private Map<AstNode,Type> types;

    public RhinoTypeValidator(AstRoot source, Map<AstNode,Type> types) {
        this.source = source;
        this.types = types;
    }

    public static class MissingTypeException extends Error {
        public final AstNode n;
        public MissingTypeException(AstNode node) {
            this.n = node;
        }
        @Override
        public String toString() {
            return "Missing type information for AST node: "+this.n.toSource(0);
        }
    }

    public static class TypeError extends Error {
        public final AstNode n;
        public final Type t;
        public final String msg;
        public TypeError(String s, AstNode node, Type t) {
            this.n = node;
            this.msg = s;
            this.t = t;
        }
        @Override
        public String toString() {
            return msg+": "+n.toSource(0)+" :: "+t+" at line "+n.getLineno();
        }
    }

    public void check() {
        System.err.println("[TypeValidator] running...");
        visit(this.source);
    }

    @Override
    protected void visitName(Name node) {
        Type t = getType(node);
        if (t == null) {
        	System.err.println("error at line: " + node.getLineno());
        	throw new MissingTypeException(node);
        }
    }

    @Override
    protected void visitElementGet(ElementGet node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        visit(node.getTarget());
        visit(node.getElement());
    }

    @Override
    protected void visitNumber(NumberLiteral node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
    }

    @Override
    protected void visitLiteral(AstNode node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
    }

    protected Type getType(AstNode n) {
        return types.get(n);
    }

    @Override
    protected void visitVariableInitializers(VariableDeclaration node) {
        List<VariableInitializer> vars = node.getVariables();
        CompoundStatement cs = mkCompoundStatement();
        for (VariableInitializer vi : vars) {
            Type t = getType(vi.getTarget());
            if (t == null) {
                System.err.println(vi.getLineno() + " BAD LINE NO");
                throw new MissingTypeException(vi.getTarget());
            }

            if (vi.getInitializer() != null) {
                Type t2 = getType(vi.getInitializer());
                if (t2 == null) { throw new MissingTypeException(vi.getInitializer()); }
                visit(vi.getInitializer());
            }
        }
    }

    @Override
    protected void visitExprStmt(org.mozilla.javascript.ast.ExpressionStatement node) {
        visit(node.getExpression());
    }

    @Override
    protected void visitInfix(InfixExpression node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
        if (t.isAny()) { throw new TypeError("Infix expression has type any", node, t); }

        visit(node.getLeft());
        visit(node.getRight());
    }

    @Override
    protected void visitAssignment(org.mozilla.javascript.ast.Assignment node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        visit(node.getLeft());
        visit(node.getRight());
    }

    @Override
    protected void visitFunction(FunctionNode node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        for (AstNode arg : node.getParams()) {
            visit(arg);
        }
        visit(node.getBody());
    }

    protected void visitObjectLiteral(ObjectLiteral node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        for (ObjectProperty p : node.getElements()) {
            visit(p.getRight());
        }
    }

    protected void visitFunctionCall(org.mozilla.javascript.ast.FunctionCall node) {
        if (RhinoToIR.looksLikeModuleImport(node, types, null)) {
            return;
        }
        Type t = getType(node.getTarget());
        if (t == null) { throw new MissingTypeException(node.getTarget()); }
        boolean mcall = RhinoToIR.looksLikeMethodCall(node, types, null, false);
        // TODO: intersection types!!!
        if (mcall) {
            if (t.isIntersectionType()) {
                t = ((IntersectionType)t).findMethodType(node.getArguments().size());
                if (t == null) {
                    throw new TypeError("Expected method type on method invocation, got intersection without method of proper arity", node.getTarget(), t);
                }
            }
            if (!t.isAttachedMethod() && !t.isUnattachedMethod()) {
                throw new TypeError("Expected Method type on method invocation, got ", node.getTarget(), t);
            }
        } else {
            if (t.isIntersectionType()) {
                t = ((IntersectionType)t).findFunctionType(node.getArguments().size());
                if (t == null) {
                    throw new TypeError("Expected method type on method invocation, got intersection without method of proper arity: ", node.getTarget(), t);
                }
            }
            if (!t.isFunction()) {
                throw new TypeError("Expected Function type on function invocation, got ", node.getTarget(), t);
            }
        }

        visit(node.getTarget());
        for (AstNode arg : node.getArguments()) {
            visit(arg);
        }
    }

    protected void visitReturn(org.mozilla.javascript.ast.ReturnStatement node) {
        if (node.getReturnValue() != null) {
            visit(node.getReturnValue());
        }
    }

    protected void visitCondExpr(org.mozilla.javascript.ast.ConditionalExpression node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
        visit(node.getTestExpression());
        visit(node.getTrueExpression());
        visit(node.getFalseExpression());
    }

    protected void visitUnary(org.mozilla.javascript.ast.UnaryExpression node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
        visit(node.getOperand());
    }

    protected void visitPropertyGet(PropertyGet node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
        if (t.isAny()) { throw new TypeError("Property access has type any", node, t); }
        visit(node.getTarget());
    }

    protected void visitString(StringLiteral node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }
    }

    protected void visitIf(org.mozilla.javascript.ast.IfStatement n) {
        visit(n.getCondition());
        visit(n.getThenPart());
        if (n.getElsePart() != null) {
            visit(n.getElsePart());
        }
    }

    protected void visitBlock(AstNode node) {
        for (Node n : node) {
            visit((AstNode)n);
        }
    }

    protected void visitArrayLiteral(ArrayLiteral node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        for (AstNode n : node.getElements()) {
            visit(n);
        }
    }

    protected void visitForLoop(org.mozilla.javascript.ast.ForLoop loop) {
        visit(loop.getInitializer());
        visit(loop.getCondition());
        visit(loop.getIncrement());
        visit(loop.getBody());
    }

    protected void visitNewExpr(NewExpression node) {
        Type t = getType(node);
        if (t == null) { throw new MissingTypeException(node); }

        visit(node.getTarget()); // NewExpression <: FunctionCall
        for (AstNode arg : node.getArguments()) {
            visit(arg);
        }
    }

    protected void visitParenExpr(ParenthesizedExpression node) {
        visit(node.getExpression());
    }

    protected void visitWhileLoop(org.mozilla.javascript.ast.WhileLoop loop) {
        visit(loop.getCondition());
        visit(loop.getBody());
    }

    protected void visitBreak(org.mozilla.javascript.ast.BreakStatement node) {
    }

    protected void visitDoLoop(org.mozilla.javascript.ast.DoLoop loop) {
        visit(loop.getCondition());
        visit(loop.getBody());
    }

    protected void visitForInLoop(org.mozilla.javascript.ast.ForInLoop loop) {
        visit(loop.getIteratedObject());
        visit(loop.getIterator());
        visit(loop.getBody());
    }

    protected void visitArrayComp(ArrayComprehension node) {
        throw new UnsupportedOperationException();
    }
    protected void visitContinue(org.mozilla.javascript.ast.ContinueStatement node) {
    }
    protected void visitDefaultXmlNamepace(org.mozilla.javascript.ast.UnaryExpression node) {
        throw new UnsupportedOperationException();
    }
    protected void visitGenExpr(GeneratorExpression node) {
        throw new UnsupportedOperationException();
    }
    protected void visitLabeledStatement(LabeledStatement ls) {
        throw new UnsupportedOperationException();
    }
    protected void visitLetNode(LetNode node) {
        throw new UnsupportedOperationException();
    }
    protected void visitRegExp(RegExpLiteral node) {
        throw new UnsupportedOperationException();
    }
    protected void visitScript(ScriptNode node) {
        for (Node n : node) {
            visit((AstNode)n);
        }
    }
    protected void visitSwitch(SwitchStatement node) {
    	 visit(node.getExpression());
    	 for (SwitchCase sc : node.getCases()){
    		 if (sc.getExpression() != null){
    			 visit(sc.getExpression());
    		 }
    		 if (sc.getStatements() != null){
    			 for (AstNode n : sc.getStatements()){
    				 visit(n);
    			 }
    		 }
    	 }
    }

    protected void visitThrow(ThrowStatement node) {
        throw new UnsupportedOperationException();
    }
    protected void visitTry(TryStatement node) {
        throw new UnsupportedOperationException();
    }
    //protected void visitVariables(org.mozilla.javascript.ast.VariableDeclaration node) {
    //    throw new UnsupportedOperationException();
    //}
    protected void visitXmlLiteral(XmlLiteral node) {
        throw new UnsupportedOperationException();
    }
    protected void visitXmlMemberGet(XmlMemberGet node) {
        throw new UnsupportedOperationException();
    }
    protected void visitXmlRef(XmlRef node) {
        throw new UnsupportedOperationException();
    }
    protected void visitYield(Yield node) {
        throw new UnsupportedOperationException();
    }


}
