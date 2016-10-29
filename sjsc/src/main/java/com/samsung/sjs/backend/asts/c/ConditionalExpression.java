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
 * Representation of conditional expresion in C
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
public class ConditionalExpression extends Expression {
    private Expression test, left, right;
    public ConditionalExpression() {}
    public void setTest(Expression e) {
        test = e;
    }
    public Expression getTest() {
        return test;
    }
    public void setTrueBranch(Expression e) {
        left = e;
    }
    public Expression getTrueBranch() {
        return left;
    }
    public void setFalseBranch(Expression e) {
        right = e;
    }
    public Expression getFalseBranch() {
        return right;
    }
    @Override
    public String toSource(int x) { 
        return "( "+test.toSource(0)+" ? "+left.toSource(0)+" : "+right.toSource(0)+" )"; 
    }
    @Override
    public Expression asValue(Type t) {
        // sink the coercion to the branches
        ConditionalExpression b = new ConditionalExpression();
        b.setTest(test);
        b.setTrueBranch(left.asValue(t));
        b.setFalseBranch(right.asValue(t));
        return b;
    }
}
