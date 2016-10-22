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
 * Base class for statements
 * 
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public abstract class Statement extends IRNode {
    protected Statement(Tag t) {
        super(t);
    }
    @Override
    public final boolean isStatement() { return true; }
    @Override
    public final Statement asStatement() { return this; }
}
