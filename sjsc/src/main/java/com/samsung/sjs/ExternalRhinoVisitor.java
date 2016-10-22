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
/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * This file is derived from a file in Mozilla's Rhino project,
 * org.mozilla.javascript.IRFactory.  This version is heavily modified,
 * keeping intact only the visitor pattern; all other functionality
 * has been stripped out to build a generic visitor class.
 */
package com.samsung.sjs;

import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;

import java.util.List;
import java.util.ArrayList;

/*
 * This class is a heavily modified version of org.mozilla.javascript.IRFactory.
 * The main changes are to make this more general visitor class, hijacking
 * IRFactory's code layout as a starting point; see the visit(AstNode) method.
 */
public class ExternalRhinoVisitor
{
    public ExternalRhinoVisitor() {
    }

    public void visit(AstNode node) {
        switch (node.getType()) {
          case Token.ARRAYCOMP:
              visitArrayComp((ArrayComprehension)node);
              break;
          case Token.ARRAYLIT:
              visitArrayLiteral((ArrayLiteral)node);
              break;
          case Token.BLOCK:
              visitBlock(node);
              break;
          case Token.BREAK:
              visitBreak((BreakStatement)node);
              break;
          case Token.CALL:
              visitFunctionCall((FunctionCall)node);
              break;
          case Token.CONTINUE:
              visitContinue((ContinueStatement)node);
              break;
          case Token.DO:
              visitDoLoop((DoLoop)node);
              break;
          case Token.EMPTY:
              visitEmpty(node);
              break;
          case Token.FOR:
              if (node instanceof ForInLoop) {
                  visitForInLoop((ForInLoop)node);
              } else {
                  visitForLoop((ForLoop)node);
              }
              break;
          case Token.FUNCTION:
              visitFunction((FunctionNode)node);
              break;
          case Token.GENEXPR:
              visitGenExpr((GeneratorExpression)node);
              break;
          case Token.GETELEM:
              visitElementGet((ElementGet)node);
              break;
          case Token.GETPROP:
              visitPropertyGet((PropertyGet)node);
              break;
          case Token.HOOK:
              visitCondExpr((ConditionalExpression)node);
              break;
          case Token.IF:
              visitIf((IfStatement)node);
              break;

          case Token.TRUE:
          case Token.FALSE:
          case Token.THIS:
          case Token.NULL:
          case Token.DEBUGGER:
              visitLiteral(node);
              break;

          case Token.NAME:
              visitName((Name)node);
              break;
          case Token.NUMBER:
              visitNumber((NumberLiteral)node);
              break;
          case Token.NEW:
              visitNewExpr((NewExpression)node);
              break;
          case Token.OBJECTLIT:
              visitObjectLiteral((ObjectLiteral)node);
              break;
          case Token.REGEXP:
              visitRegExp((RegExpLiteral)node);
              break;
          case Token.RETURN:
              visitReturn((ReturnStatement)node);
              break;
          case Token.SCRIPT:
              visitScript((ScriptNode)node);
              break;
          case Token.STRING:
              visitString((StringLiteral)node);
              break;
          case Token.SWITCH:
              visitSwitch((SwitchStatement)node);
              break;
          case Token.THROW:
              visitThrow((ThrowStatement)node);
              break;
          case Token.TRY:
              visitTry((TryStatement)node);
              break;
          case Token.WHILE:
              visitWhileLoop((WhileLoop)node);
              break;
          case Token.WITH:
              visitWith((WithStatement)node);
              break;
          case Token.YIELD:
              visitYield((Yield)node);
              break;
          default:
              if (node instanceof ExpressionStatement) {
                  visitExprStmt((ExpressionStatement)node);
                  break;
              }
              if (node instanceof Assignment) {
                  visitAssignment((Assignment)node);
                  break;
              }
              if (node instanceof UnaryExpression) {
                  visitUnary((UnaryExpression)node);
                  break;
              }
              if (node instanceof XmlMemberGet) {
                  visitXmlMemberGet((XmlMemberGet)node);
                  break;
              }
              if (node instanceof InfixExpression) {
                  visitInfix((InfixExpression)node);
                  break;
              }
              if (node instanceof VariableDeclaration) {
                  visitVariables((VariableDeclaration)node);
                  break;
              }
              if (node instanceof ParenthesizedExpression) {
                  visitParenExpr((ParenthesizedExpression)node);
                  break;
              }
              if (node instanceof LabeledStatement) {
                  visitLabeledStatement((LabeledStatement)node);
                  break;
              }
              if (node instanceof LetNode) {
                  visitLetNode((LetNode)node);
                  break;
              }
              if (node instanceof XmlRef) {
                  visitXmlRef((XmlRef)node);
                  break;
              }
              if (node instanceof XmlLiteral) {
                  visitXmlLiteral((XmlLiteral)node);
                  break;
              }
              throw new IllegalArgumentException("Can't visit: " + node);
        }
    }

    protected void visitArrayComp(ArrayComprehension node) {
        throw new UnsupportedOperationException();
    }

    protected void visitArrayLiteral(ArrayLiteral node) {
        List<AstNode> elems = node.getElements();
        for (int i = 0; i < elems.size(); ++i) {
            AstNode elem = elems.get(i);
            if (elem.getType() != Token.EMPTY) {
                visit(elem);
            }
        }
    }

    protected void visitAssignment(Assignment node) {
        visit(node.getLeft());
        visit(node.getRight());
    }

    protected void visitBlock(AstNode node) {
        for (Node kid : node) {
            visit((AstNode)kid);
        }
    }

    protected void visitBreak(BreakStatement node) {
    }

    protected void visitCondExpr(ConditionalExpression node) {
        visit(node.getTestExpression());
        visit(node.getTrueExpression());
        visit(node.getFalseExpression());
    }

    protected void visitContinue(ContinueStatement node) {
    }

    protected void visitDoLoop(DoLoop loop) {
        visit(loop.getBody());
        visit(loop.getCondition());
    }

    protected void visitElementGet(ElementGet node) {
        visit(node.getTarget());
        visit(node.getElement());
    }

    protected void visitExprStmt(ExpressionStatement node) {
        visit(node.getExpression());
    }

    protected void visitEmpty(Node node) {
    }

    protected void visitForInLoop(ForInLoop loop) {
        visit(loop.getIterator());
        visit(loop.getIteratedObject());
        visit(loop.getBody());
    }

    protected void visitForLoop(ForLoop loop) {
        visit(loop.getInitializer());
        visit(loop.getCondition());
        visit(loop.getIncrement());
        visit(loop.getBody());
    }

    protected void visitFunction(FunctionNode fn) {
        visit(fn.getBody());
    }

    protected void visitFunctionCall(FunctionCall node) {
        visit(node.getTarget());
        List<AstNode> args = node.getArguments();
        for (int i = 0; i < args.size(); i++) {
            AstNode arg = args.get(i);
            visit(arg);
        }
    }
    
    protected void visitGenExpr(GeneratorExpression node) {
        visit(node.getResult());

        List<GeneratorExpressionLoop> loops = node.getLoops();
        int numLoops = loops.size();
        for (int i = 0; i < numLoops; i++) {
            GeneratorExpressionLoop acl = loops.get(i);
            visit(acl.getIteratedObject());
        }
        if (node.getFilter() != null) {
            visit(node.getFilter());
        }
    }

    protected void visitIf(IfStatement n) {
        visit(n.getCondition());
        visit(n.getThenPart());
        if (n.getElsePart() != null) {
            visit(n.getElsePart());
        }
    }

    protected void visitInfix(InfixExpression node) {
        visit(node.getLeft());
        visit(node.getRight());
    }

    protected void visitLabeledStatement(LabeledStatement ls) {
        visit(ls.getStatement());
    }

    protected void visitLetNode(LetNode node) {
        visitVariableInitializers(node.getVariables());
        visit(node.getBody());
    }

    protected void visitLiteral(AstNode node) {
    }

    protected void visitName(Name node) {
    }

    protected void visitNewExpr(NewExpression node) {
        // The initializer is an experimental Rhino extension for allocating via
        //      new C(...) { m: ..., f: ... }
        // where the object literal is used to initialize additional fields.
        // It's not standard JavaScript.
        //visit(node.getInitializer());
        List<AstNode> args = node.getArguments();
        for (int i = 0; i < args.size(); i++) {
            AstNode arg = args.get(i);
            visit(arg);
        }
    }

    protected void visitNumber(NumberLiteral node) {
    }

    protected void visitObjectLiteral(ObjectLiteral node) {
        List<ObjectProperty> elems = node.getElements();
        for (ObjectProperty prop : elems) {
            visit(prop.getRight());
        }
    }

    protected void visitParenExpr(ParenthesizedExpression node) {
        AstNode expr = node.getExpression();
        while (expr instanceof ParenthesizedExpression) {
            expr = ((ParenthesizedExpression)expr).getExpression();
        }
        visit(expr);
    }

    protected void visitPropertyGet(PropertyGet node) {
        visit(node.getTarget());
    }

    protected void visitRegExp(RegExpLiteral node) {
    }

    protected void visitReturn(ReturnStatement node) {
        AstNode rv = node.getReturnValue();
        if (rv != null)
            visit(rv);
    }

    protected void visitScript(ScriptNode node) {
        for (Node kid : node) {
            visit((AstNode)kid);
        }
    }

    protected void visitString(StringLiteral node) {
    }

    protected void visitSwitch(SwitchStatement node) {
        for (SwitchCase sc : node.getCases()) {
            AstNode expr = sc.getExpression();
            if (expr != null) {
                visit(expr);
            }
            List<AstNode> stmts = sc.getStatements();
            if (stmts != null) {
                for (AstNode kid : stmts) {
                    visit(kid);
                }
            }
        }
    }

    protected void visitThrow(ThrowStatement node) {
        visit(node.getExpression());
    }

    protected void visitTry(TryStatement node) {
        visit(node.getTryBlock());
        for (CatchClause cc : node.getCatchClauses()) {
            AstNode ccc = cc.getCatchCondition();
            if (ccc != null) {
                visit(ccc);
            }
            visit(cc.getBody());
        }
        if (node.getFinallyBlock() != null) {
            visit(node.getFinallyBlock());
        }
    }

    protected void visitUnary(UnaryExpression node) {
        int type = node.getType();
        if (type == Token.DEFAULTNAMESPACE) {
            visitDefaultXmlNamepace(node);
            return;
        }
        visit(node.getOperand());
    }

    protected void visitVariables(VariableDeclaration node) {
        visitVariableInitializers(node);
    }

    protected void visitVariableInitializers(VariableDeclaration node) {
        List<VariableInitializer> vars = node.getVariables();
        int size = vars.size(), i = 0;
        for (VariableInitializer var : vars) {
            AstNode target = var.getTarget();
            AstNode init = var.getInitializer();

            visit(target);
            if (init != null) visit(init);
        }
    }

    protected void visitWhileLoop(WhileLoop loop) {
        visit(loop.getCondition());
        visit(loop.getBody());
    }

    protected final void visitWith(WithStatement node) {
        throw new UnsupportedOperationException("SJS does not permit with statements");
    }

    protected void visitYield(Yield node) {
        if (node.getValue() != null)
            visit(node.getValue());
    }

    protected void visitXmlLiteral(XmlLiteral node) {
        throw new UnsupportedOperationException("SJS does not permit XML literals");
    }

    protected void visitXmlMemberGet(XmlMemberGet node) {
        throw new UnsupportedOperationException("SJS does not permit XML features");
    }

    // We get here if we weren't a child of a . or .. infix node
    protected void visitXmlRef(XmlRef node) {
        throw new UnsupportedOperationException("SJS does not permit XML features");
    }

    protected void visitDefaultXmlNamepace(UnaryExpression node) {
        visit(node.getOperand());
    }


}
