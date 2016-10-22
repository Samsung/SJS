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
 * Representation of a C binary (infix) operation
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
public class UnaryExpression extends Expression {
    protected Expression exp;
    protected String op;
    protected boolean postfix;
    public UnaryExpression(Expression e, String op, boolean postfix) {
        exp = e;
        this.op = op;
        this.postfix = postfix;
    }
    public Expression getExpression() { return exp; }
    public boolean isPostfix() { return postfix; }
    @Override
    public String toSource(int x) { 
        if (postfix) {
            return "("+exp.toSource(0)+op+")";
        } else {
            return "("+op+exp.toSource(0)+")";
        }
    }
}
