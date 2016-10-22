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
 * WhileLoop
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

// TODO: This should inherit from LoopBase...
public class WhileLoop extends Statement {
    private Expression cond;
    private Statement body;
    public WhileLoop(Expression cond, Statement body) {
        super(Tag.WhileLoop);
        this.cond = cond;
        if (!(body instanceof Block)) {
            Block b = new Block();
            b.addStatement(body);
            this.body = b;
        } else {
            this.body = body;
        }
    }
    public Expression getCondition() { return cond; }
    public Statement getBody() { return body; }
    @Override
    public String toSource(int x) { return "while ("+cond.toSource(0)+")\n"+body.toSource(x+2)+"\n"; }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitWhileLoop(this);
    }
}
