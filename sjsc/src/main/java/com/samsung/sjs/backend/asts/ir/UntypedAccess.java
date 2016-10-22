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
 * Access to an untyped global linked in from untyped code
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class UntypedAccess extends Expression {

    private Expression shared_global;

    public UntypedAccess(Expression x) {
        super(Tag.Untyped);
        shared_global = x;
        assert (x.getType() != null);
    }

    public Expression untypedVariable() { return shared_global; }

    @Override
    public String toSource(int x) {
        return "UNTYPED_ACCESS("+shared_global.toSource(0)+")";
    }

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitUntyped(this);
    }
}
