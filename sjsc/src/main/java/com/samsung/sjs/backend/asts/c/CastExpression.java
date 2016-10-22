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
 * Representation of a C cast
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;

import com.samsung.sjs.backend.asts.c.types.CType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.Types;

public class CastExpression extends Expression {
    private CType type;
    private Expression expr;
    public CastExpression(CType t, Expression e) {
        type = t;
        expr = e;
    }
    public Expression getBaseExpression() { return expr; }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("((");
        sb.append(type.toSource());
        sb.append(")");
        sb.append(expr.toSource(0));
        sb.append(")");
        return sb.toString();
    }
    @Override
    public Expression inType(Type t) {
        //assert Types.isEqual(t,type);
        return expr;
    }
}
