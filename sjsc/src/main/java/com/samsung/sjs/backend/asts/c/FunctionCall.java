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
 * Representation of a function call node in C
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
import java.util.ArrayList;
public class FunctionCall extends Expression {
    protected Expression func;
    protected ArrayList<Expression> args;
    public FunctionCall(Expression f, Expression... args) {
        func = f;
        this.args = new ArrayList<Expression>();
        for (Expression e : args) {
            this.args.add(e);
        }
    }
    public FunctionCall(String s, Expression... args) {
        func = new Variable(s);
        this.args = new ArrayList<Expression>();
        for (Expression e : args) {
            this.args.add(e);
        }
    }
    public void addActualArgument(Expression e) {
        args.add(e);
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        sb.append(func.toSource(0));
        sb.append("(");
        for (int i = 0, n = args.size(); i < n; i++) {
            sb.append(args.get(i).toSource(0));
            if (i+1 < n) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public Expression asValue(Type t) {
        // calling convention dictates the result is returned as a value_t, so no coercion is
        // necessary.  The only wrinkle is that we've historically abused the FunctionCall node for
        // various primitives as well...
        return this;
    }
}
