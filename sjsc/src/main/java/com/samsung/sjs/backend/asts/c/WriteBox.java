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
 * Update the value stored in a box
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
public class WriteBox extends Expression {
    Expression target, val;
    public WriteBox(Expression e, Expression v) {
        target = e;
        val = v;
    }
    @Override
    public String toSource(int x) {
        return "WRITEBOX("+target.toSource(0)+", "+val.toSource(0)+")";
    }
}
