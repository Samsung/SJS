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
 * ForLoop
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class ForLoop extends LoopBase {
    private Expression cond, incr;
    private IRNode init;
    public ForLoop(IRNode init, Expression cond, Expression incr) {
        super(Tag.ForLoop);
        this.init = init;
        this.cond = cond;
        this.incr = incr;
    }
    public IRNode getInitializer() { return init; }
    public Expression getCondition() { return cond; }
    public Expression getIncrement() { return incr; }
    @Override
    protected String loopHeader() {
        return "for ("+
            (init != null ? (init.isExpression() ? init.toSource(0)+"; " : init.toSource(0) ) : ";")
            +(cond != null ? cond.toSource(0) : "")
            +"; "
            +(incr != null ? incr.toSource(0) : "") +")";
    }
    @Override
    protected String loopFooter() {
        return "";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitForLoop(this);
    }
}
