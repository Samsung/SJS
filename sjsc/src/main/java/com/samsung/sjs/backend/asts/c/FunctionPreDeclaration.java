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
 * Predeclare a function in C
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.asts.c.types.CType;
import java.util.*;
public class FunctionPreDeclaration extends Statement {
    private final CType returnType;
    private final ArrayList<CType> argTypes;
    private final String name;
    public FunctionPreDeclaration(FunctionDeclaration f) {
        returnType = f.getReturnType();
        argTypes = new ArrayList<CType>();
        for (int i = 0; i < f.nargs(); i++) {
            argTypes.add(f.getArgType(i));
        }
        name = f.getName();
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append(returnType.toSource()+" "+name+"(");
        for(int i = 0, n=argTypes.size(); i < n; i++) {
            sb.append(argTypes.get(i).toSource());
            if (i+1 < n) sb.append(", ");
        }
        sb.append(");\n");
        return sb.toString();
    }
}
