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
 * Parent class to represent compound C statements 
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;

import java.util.*;

public class CompoundStatement extends Statement {
    LinkedList<Statement> body;
        public CompoundStatement() {
        body = new LinkedList<Statement>();
    }
    public final void addStatement(Statement s) {
        body.add(s);
    }
    public final void addExpressionStatement(Expression e) {
        body.add(new ExpressionStatement(e));
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        for (Statement s : body) {
            sb.append(s.toSource(x)+"\n");
        }
        return sb.toString();
    }
}
