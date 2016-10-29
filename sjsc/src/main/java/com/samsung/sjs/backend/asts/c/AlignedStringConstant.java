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
 * Aligned declaration of a string constant
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;

public class AlignedStringConstant extends Statement {
    private final CStringLiteral str;
    private final Variable name;
    public AlignedStringConstant(Variable v, CStringLiteral s) {
        str = s;
        name = v;
    }
    @Override public String toSource(int x) {
        // TODO: Make this const char[], which propagates elsewhere...
        return "__attribute__((__aligned__(8))) wchar_t "+name.toSource(0)+"[] = "+str.toSource(0)+";";
    }
}
