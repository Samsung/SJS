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
 * Representation of C for loop
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
public class ForLoop extends BlockStatement {
    private Expression inc, test;
    private CNode init;
    // init is a CNode because it may be a VariableDeclaration or an expression.  Technically C99
    // forbids the former, but every C compiler under the sun accepts it and generates correct code
    // for it.
    public ForLoop(CNode init, Expression test, Expression inc) {
        this.init = init;
        this.inc = inc;
        this.test = test;
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("for (");
        if (init != null) sb.append(init.toSource(0));
        if (init == null || init.isExpression()) {
            sb.append("; "); // otherwise it's a statement, with its own ;
        }
        if (test != null) sb.append(test.toSource(0));
        sb.append("; ");
        if (inc != null) sb.append(inc.toSource(0));
        sb.append(") {\n");
        for (Statement s : body) {
            sb.append(s.toSource(x+1));
        }
        indent(x,sb);
        sb.append("}\n");
        return sb.toString();
    }
}
