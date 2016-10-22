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
 * Require statement for node.js-style module import
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class Require extends Expression {
    String modpath;

    public Require(String path) {
        super(Tag.Require);
        modpath = path;
    }

    public String getPath() { return modpath; }

    // For interop slow mode purposes, this is a call --- it results in the execution of other code,
    // possibly untyped
    @Override
    public boolean mustSaveIntermediates() { return true; }

    @Override
    public String toSource(int x) {
        return "require('" + modpath + "')";
    }

    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitRequire(this);
    }
}
