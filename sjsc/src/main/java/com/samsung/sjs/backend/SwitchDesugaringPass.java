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
/** Javascript cases (in switch statements) can contain arbitrary expressions,
 * whereas the C11 standard requires they be integer constant expressions.
 * This pass transforms switch statements with non-integer constant expressions
 * in their case statements into nested if statements.
 *
 * @author Cole Schlesinger (cschles1@gmail.com)
 */
package com.samsung.sjs.backend;

import java.util.*;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.BooleanType;

public class SwitchDesugaringPass extends IRTransformer {

    protected final CompilerOptions options;
    protected final Script script;
    private Stack<Boolean> switchStack;

    /** Build a traversal object.
     *  @param opts compiler options
     *  @param s    script to be traversed
     */
    public SwitchDesugaringPass(CompilerOptions opts, Script s) {
        super(s);
        this.options = opts;
        this.script = s;
        this.switchStack = new Stack<Boolean>();
    }

    /** Traverse the script given to the constructor.
     *  @return the desugared script
     */
    public Script convert() {
        return (Script) script.accept(this);
    }

    private boolean desugaringSwitch() {
        if (switchStack.isEmpty()) {
            return false;
        }
        return switchStack.peek().booleanValue();
    }

    /** Determine whether an expression contains only integer and character
     * constants and has an integer type.
     *
     * The C11 standard requires that expressions in switch case statements be
     * integer constant expressions.  C11 defines other valid operands, such as
     * sizeof(), which aren't exposed in Javascript.
     *
     * Note that this is subtly different than Expression.isPure(), which
     * simply checks whether an expression is (extensionally) pure, and
     * Expression.isConst(), which can include non-integer constants.  This
     * traversal is more strict: it requires that operands be (C-style)
     * integers, not, say, floats.
     */
    private class IsIntegerConstantExpression extends VoidIRVisitor {

        private boolean isConstant;

        /** Return {@code true} if {@code node} is an integer constant
         * expression and {@code false} otherwise.
         */
        public boolean check(Expression node) {
            // Assume an expression is an integer constant expression until the
            // traversal discover otherwise.
            this.isConstant = true;
            node.accept(this);
            return this.isConstant;
        }

        @Override
        public Void visitAllocArrayLiteral(AllocArrayLiteral node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitAllocClosure(AllocClosure node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitAllocMapLiteral(AllocMapLiteral node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitAllocNewObject(AllocNewObject node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitAllocObjectLiteral(AllocObjectLiteral node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitArrayIndex(ArrayIndex node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitCondExpr(CondExpr node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitFieldAssignment(FieldAssignment node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitPredictedFieldAssignment(PredictedFieldAssignment node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitFieldRead(FieldRead node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitPredictedFieldRead(PredictedFieldRead node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitFloatLiteral(FloatLiteral node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitFunction(Function node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitFunctionCall(FunctionCall node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitIntrinsicName(IntrinsicName node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitLambda(Lambda node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitLetIn(LetIn node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitMethodCall(MethodCall node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitNamedLambda(NamedLambda node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitStr(Str node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitThisLiteral(ThisLiteral node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitUndef(Undefined node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitVar(Var node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitVarAssignment(VarAssignment node) {
            this.isConstant = false;
            return null;
        }

        @Override
        public Void visitBinaryOp(BinaryOp node) {
            switch (node.getOp()) {
                case "+":
                case "-":
                case "*":
                case "/":
                case "%":
                case "&&":
                case "||":
                case ">>":
                case ">>>":
                case "<<":
                    return super.visitBinaryOp(node);
                default:
                    this.isConstant = false;
                    return null;
            }
        }

        @Override
        public Void visitUnaryOp(UnaryOp node) {
            switch (node.getOp()) {
                case "-":
                    return super.visitUnaryOp(node);
                default:
                    this.isConstant = false;
                    return null;
            }
        }
    }

    /** Traverse each statement, replacing switch statements where necessary,
     * and packaging the resulting statements into a Block.
     */
    private Block traverseAndWrap(List<Statement> statements) {

        // TODO(cns): what if the list is empty?  Is there a nop statement we
        // should insert here?
        Block body = new Block();
        for (Statement s : statements) {
            if (s instanceof Break) {
                continue;
            }
            Statement desugaredStatement = (Statement) s.accept(this);
            body.addStatement(desugaredStatement);
        }
        return body;
    }

    private IRNode mkIfOfSwitch(Switch node) {
        Statement rv = null;
        Expression discriminee = node.getDiscriminee();
        List<Case> cases = node.getCases();

        // If a default case exists, initialize rv with it and remove it from
        // the list.
        for (Case c : cases) {
            if (c.getValue() == null) {
                rv = traverseAndWrap(c.getStatements());
                cases.remove(c);
                break;
            }
        }

        // Build the if statement from the inside out.
        for (int i = cases.size() - 1; i >= 0; i--) {
            Case c = cases.get(i);
            BinaryOp test = new BinaryOp(discriminee, "==", c.getValue());
            test.setType(BooleanType.make());

            rv = new IfThenElse(
                test,
                traverseAndWrap(c.getStatements()),
                rv);

        }
        return rv;
    }

    @Override public IRNode visitBreak(Break b) {
        if (desugaringSwitch())
            // Replace break statements with an empty block (i.e. a nop) in
            // switch statements that are being desugared.
            return IRManipulator.mkBlock();
        else
            return super.visitBreak(b);
    }

    @Override
    public IRNode visitSwitch(Switch node) {
        // Check whether all cases are integer constant expressions.
        IRNode rv;
        boolean hasConstCases = true;
        IsIntegerConstantExpression checker =
          new IsIntegerConstantExpression();
        for (Case c : node.getCases()) {
            Expression val = c.getValue();
            if (val == null) {
                // NB: Case encodes "default" as a null value.
                continue;
            }
            else if (!checker.check(val)) {
                hasConstCases = false;
                break;
            }
        }
        if (hasConstCases) {
            // Leave as switch.
            this.switchStack.push(Boolean.FALSE);
            rv = super.visitSwitch(node);
            this.switchStack.pop();
        } else {
            // Transform to if.
            this.switchStack.push(Boolean.TRUE);
            rv = mkIfOfSwitch(node);
            this.switchStack.pop();
        }

        return rv;
    }
}
