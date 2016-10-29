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
 * When translating between phases, often one statement
 * turns into multiple.  This is more convenient with a
 * container statement that isn't attached to scoping,
 * so for example a JS var decl of 8 things of different
 * types can be desugared more easily in visitors.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;

public class CompoundStatement extends Statement implements Iterable<Statement> {
    private ArrayList<Statement> body;
    public CompoundStatement() {
        super(Tag.CompoundStatement);
        body = new ArrayList<Statement>();
    }
    public void addStatement(Statement s) {
        body.add(s);
    }
    public Iterator<Statement> iterator() {
        return body.iterator();
    }
    public int nstatements() { return body.size(); }
    public Statement getStatement(int n) {
        return body.get(n);
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        for (Statement s : this) {
            indent(x,sb);
            sb.append(s.toSource(0));
            sb.append("\n");
        }
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitCompoundStatement(this);
    }
}
