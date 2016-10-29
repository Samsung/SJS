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
 * Representation of C block statement
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import java.util.LinkedList;
import java.util.List;
public class BlockStatement extends Statement {
    LinkedList<Statement> body;
    public BlockStatement() {
        body = new LinkedList<Statement>();
    }
    public final void addStatement(Statement s) {
        body.add(s);
    }
    public final void prefixStatement(Statement s) {
        body.add(0,s);
    }
    public List<Statement> getStatements() { return body; }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("{\n");
        for (Statement s : body) {
            sb.append(s.toSource(x+1));
        }
        indent(x,sb);
        sb.append("}\n");
        return sb.toString();
    }
}
