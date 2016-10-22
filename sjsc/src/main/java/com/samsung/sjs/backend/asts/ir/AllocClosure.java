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
 * IR node for allocating a closure + environment.  Note that we capture expressions rather
 * than variables, since what were originally variables may turn into further
 * environment accesses.
 *
 * TODO: When doing codegen, anything that shows up as an expression better be an lval, and is
 * either an environment access or a boxed variable.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;

public class AllocClosure extends Expression {
    ArrayList<Expression> captured;
    Function code;
    private int[] vtable;
    public AllocClosure(Function f) {
        super(Tag.AllocClosure);
        code = f;
        captured = new ArrayList<Expression>();
    }
    // the VTable corresponds to the constructor's initial prototype object
    public void setVTable(int[] vt) { vtable = vt; }
    public int[] getVTable() { return vtable; }
    public Function getCode() { return code; }
    public List<Expression> getCapturedVars() { return captured; }
    public int environmentSize() { return captured.size(); }
    public void setCapturedVars(List<Expression> c) { captured = new ArrayList<Expression>(c); }
    public void addCapturedVariable(Expression x) {
        captured.add(x);
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        sb.append("<closure>["+code.getName()+"](");
        for (int i = 0; i < captured.size(); i++) {
            sb.append(captured.get(i).toSource(0));
            if (i + 1 < captured.size()) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitAllocClosure(this);
    }
    @Override
    public boolean mustSaveIntermediates() {
        // closure alloc only captures lvals, so there's no failing subexpression to consider
        return false;
    }
}
