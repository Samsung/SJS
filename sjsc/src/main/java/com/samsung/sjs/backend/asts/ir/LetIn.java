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
 * LetIn
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import com.samsung.sjs.types.Type;

public class LetIn extends Expression implements ILexicalScope {
    private Var x;
    private Expression v, e;
    private Type t;
    private Scope scope;
    public LetIn(Scope s, Var x, Type t, Expression v, Expression e) {
        super(Tag.LetIn);
        this.x = x;
        this.v = v;
        this.e = e;
        scope = new Scope(s);
        s.declareVariable(x, t);
        this.t = t;
    }
    public Var getVar() { return x; }
    public Expression getBoundExpression() { return v; }
    public Expression getOpenExpression() { return e; }
    public Type getBoundType() { return t; }
    @Override
    public String toSource(int d) {
        return "(let "+x.toSource(0)+" : "+t.toString()+" = "+v.toSource(0)+" in "+e.toSource(0)+")";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitLetIn(this);
    }
    public Scope getScope() { return scope; }
}
