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
 * VarDecl
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;
import com.samsung.sjs.types.Type;

public class VarDecl extends Statement implements IDeclaration {
    Var v;
    Type t;
    Expression e;
    public VarDecl(Var v, Type t, Expression e) {
        super(Tag.VarDecl);
        assert (v != null);
        assert (t != null);
        this.v = v;
        this.t = t;
        this.e = e;
    }
    public Type getType() { return t; }
    public Var getVar() { return v; }
    public Expression getInitialValue() { return e; }
    @Override
    public String toSource(int x) {
        return "var "+v.toSource(0)+" : "+t.toString()+" := "+ (e != null ? e.toSource(0)+";" : ";");
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitVarDecl(this);
    }

    public void declareInScope(Scope s) {
        s.declareVariable(v, t);
    }

    @Override
    public boolean declaresVariables() { return true; }
    @Override
    public IDeclaration asDeclaration() { return this; }
}
