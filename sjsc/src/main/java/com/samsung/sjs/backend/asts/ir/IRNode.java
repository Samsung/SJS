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
 * Base class for internal representation
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import com.samsung.sjs.types.Type;

public abstract class IRNode {
    private static long nextid = 0;

    protected Tag tag;

    // A unique identifier for every node
    // This is mutable only so node transformers can duplicate it when copying AST fragments
    protected long node_id;

    protected IRNode(Tag t) {
        tag = t;
        node_id = nextid++;
    }
    public final long getId() { return node_id; }

    public abstract String toSource(int indentation);

    public abstract <R> R accept(IRVisitor<R> v);

    protected final void indent(int x, StringBuilder b) {
        for (int i = 0; i < x; i++) {
            b.append(" ");
        }
    }
    protected final String parens(String s) {
        return "("+s+")";
    }

    public boolean isExpression() { return false; }
    public boolean isStatement() { return false; }
    public Expression asExpression() { 
        System.err.println("Not expression: "+this.toSource(0));
        throw new UnsupportedOperationException(); 
    }
    public Statement asStatement() { 
        System.err.println("Not statement: "+this.toSource(0));
        throw new UnsupportedOperationException(); 
    }

    protected Type myType;
    public void setType(Type t) {
        if (t == null) {
            System.err.println("ERROR: Trying to set type of ["+toSource(0)+"] to null!");
        }
        assert (t != null);
        myType = t;
    }
    public Type getType() { return myType; }

    public boolean declaresVariables() { return false; }
    public IDeclaration asDeclaration() { throw new UnsupportedOperationException(); }
}
