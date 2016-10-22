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
 * Representation of C do-while loop
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
public class DoLoop extends BlockStatement {
    private Expression test;
    public DoLoop(Expression test) {
        super();
        this.test = test;
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("do {\n");
        for (Statement s : body) {
            sb.append(s.toSource(x+1));
        }
        indent(x,sb);
        sb.append("} while("+test.toSource(0)+");\n");
        return sb.toString();
    }
}

