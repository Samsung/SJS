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
 * Case statement
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;

public class Case extends Statement {
    private Expression val;
    private List<Statement> statements;
    public Case(Expression v) {
        super(Tag.Case);
        val = v;
        statements = new LinkedList<>();
    }
    public Expression getValue() { return val; }
    public void addStatement(Statement s) { statements.add(s); }
    public List<Statement> getStatements() {
        assert (statements != null);
        return statements;
    }

    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x, sb);
        if (val != null) {
            sb.append("case "+val.toSource(0)+":\n");
        } else {
            sb.append("default:\n");
        }
        for (Statement s : statements) {
            sb.append(s.toSource(x+2));
        }
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitCase(this);
    }
}
