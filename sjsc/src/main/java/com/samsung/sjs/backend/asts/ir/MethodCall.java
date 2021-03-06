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
 * MethodCall
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;

public class MethodCall extends Call {

    Expression obj;
    String m;
    public MethodCall(Expression o, String m, Expression... args) {
        super(Tag.FunctionCall, args);
        obj = o;
        this.m = m;
    }
    public Expression getTarget() { return obj; }
    public String getField() { return m; }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        sb.append("(<methinvoke> "+obj.toSource(0)+"."+m);
        for (Expression e : args) { 
            sb.append(" "+e.toSource(0));
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitMethodCall(this);
    }
}
