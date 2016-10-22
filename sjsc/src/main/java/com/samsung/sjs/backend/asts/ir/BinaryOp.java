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
 * BinaryOp
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class BinaryOp extends Expression {
    Expression left, right;
    String op;
    public BinaryOp(Expression l, String op, Expression r) {
        super(Tag.BinaryOp);
        this.op = op;
        left = l;
        right = r;
    }
    public Expression getLeft() { return left; }
    public Expression getRight() { return right; }
    public String getOp() { return op; }
    @Override
    public String toSource(int x) {
        return "("+left.toSource(0)+" "+op+" "+right.toSource(0)+")";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitBinaryOp(this);
    }

    @Override
    public boolean isPure() {
        switch (op) {
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
                return left.isPure() && right.isPure();
            default:
                return false;
        }
    }
    @Override
    public boolean isConst() {
        switch (op) {
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
                return left.isConst() && right.isConst();
            default:
                return false;
        }
    }
    @Override
    public boolean mustSaveIntermediates() {
        return left.mustSaveIntermediates() || right.mustSaveIntermediates() || op.equals(",");
    }
}
