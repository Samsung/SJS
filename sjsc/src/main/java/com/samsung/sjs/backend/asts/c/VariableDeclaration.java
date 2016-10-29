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
 * Representation of (possibly multiple) C declarations (of a single type)
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.asts.c.types.*;
import java.util.ArrayList;
public class VariableDeclaration extends Statement {
    private ArrayList<Expression> names;
    private CType ty;
    private ArrayList<Expression> initializers;
    private boolean loopInit; // flag to indicate whether this is a loop initializer, and therefore we shouldn't print the ; when rendering
    public VariableDeclaration(boolean loopInit, CType ty) {
        assert (ty != null);
        this.loopInit = loopInit;
        names = new ArrayList<Expression>();
        initializers = new ArrayList<Expression>();
        this.ty = ty;
    }
    public void addVariable(Expression name, Expression init) {
        assert (!names.contains(name));
        assert (init != null); // C allows uninitialized declarations, but we don't want to generate any
        names.add(name);
        initializers.add(init);
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append(ty.toSource()+" ");
        for (int i = 0, n = names.size(); i < n; i++) {
            sb.append(names.get(i).toSource(0)+" = "+initializers.get(i).toSource(0));
            if (i+1 < n) sb.append(", ");
        }
        if (!loopInit) sb.append(";\n");
        return sb.toString();
    }
}
