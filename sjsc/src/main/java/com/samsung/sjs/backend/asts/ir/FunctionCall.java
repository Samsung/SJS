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
 * FunctionCall --- invoking a function or closure
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;
public class FunctionCall extends Call {
    Expression func;
    public FunctionCall(Expression f, Expression... args) {
        super(Tag.FunctionCall, args);
        func = f;
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        if (isDirectCall()) {
            sb.append("(<app-DIRECT> "+func.toSource(0));
        } else {
            sb.append("(<apply> "+func.toSource(0));
        }
        for (Expression e : args) { 
            sb.append(" "+e.toSource(0));
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitFunctionCall(this);
    }

    public Expression getTarget() { return func; }
    public List<Expression> getArguments() { return args; }

    private Function directCallTarget;
    public void setDirectCall(Function f) { directCallTarget = f; }
    public boolean isDirectCall() { return directCallTarget != null; }
    public Function getDirectCallTarget() { return directCallTarget; }
}
