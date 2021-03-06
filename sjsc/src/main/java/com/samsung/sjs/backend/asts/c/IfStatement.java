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
 * Representation of C conditional statement
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
public class IfStatement extends Statement {
    private Expression test;
    private Statement tbranch, ebranch;
    public IfStatement(Expression test, Statement truecase, Statement falsecase) {
        this.test = test;
        tbranch = truecase;
        ebranch = falsecase;
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("if ("+test.toSource(0)+")\n");
        sb.append(tbranch.toSource(x+1));
        if (ebranch != null) {
            indent(x,sb);
            sb.append("else\n");
            sb.append(ebranch.toSource(x+1));
        }
        return sb.toString();
    }

}
