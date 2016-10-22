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
 * AllocNewObject
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;

public class AllocNewObject extends Call {
    private Expression ctor;
    private int[] vtbl;
    public AllocNewObject(Expression ctor) {
        super(Tag.AllocNewObject);
        this.ctor = ctor;
    }
    public Expression getConstructor() { return ctor; }

    public void setVTable(int[] vt) { vtbl = vt; }
    public int[] getVTable() { return vtbl; }

    @Override
    public String toSource(int x) { return null; }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitAllocNewObject(this);
    }
}
