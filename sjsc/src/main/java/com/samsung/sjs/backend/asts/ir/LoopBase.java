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
 * Base class for various loops
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public abstract class LoopBase extends Statement {
    protected Statement body;
    protected LoopBase(Tag t) {
        super(t);
    }
    public void setBody(Statement s) {
        if (!(s instanceof Block)) {
            Block b = new Block();
            b.addStatement(s);
            body = b;
        } else {
            body = s;
        }
    }
    public Statement getBody() { 
        return body;
    }
    @Override
    public final String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x, sb);
        sb.append(loopHeader());
        sb.append("\n");
        sb.append(body.toSource(x+2));
        sb.append("\n");
        sb.append(loopFooter());
        sb.append("\n");
        return sb.toString();
    }

    protected abstract String loopHeader();
    protected abstract String loopFooter();

    public abstract <R> R accept(IRVisitor<R> v);
}
