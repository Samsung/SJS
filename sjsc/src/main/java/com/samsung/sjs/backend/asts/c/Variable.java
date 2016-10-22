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
 * Representation of C variable
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
public class Variable extends Expression {
    private boolean isval;
    private String name;
    public Variable(String x) {
        name = x;
        isval = true;
    }
    public void makeIntrinsic() { isval = false; }
    @Override public String toSource(int x) { return name; }
    // Variables are always represented in value form
    @Override public Expression asValue(Type t) { 
        return isval ? this : super.asValue(t); 
    }
    @Override public Expression inType(Type t) {
        return isval ? super.inType(t) : this;
    }
}
