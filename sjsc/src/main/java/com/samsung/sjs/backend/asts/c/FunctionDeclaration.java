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
 * Representation of C function
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.asts.c.types.CType;
import com.samsung.sjs.backend.asts.c.types.CVoid;
import java.util.ArrayList;
public class FunctionDeclaration extends Statement {
    private final CType returnType;
    private final ArrayList<String> argNames;
    private final ArrayList<CType> argTypes;
    private final String name;
    private final BlockStatement body;
    public FunctionDeclaration(String name, CType ret) {
        this.returnType = ret;
        this.name = name;
        this.argNames = new ArrayList<String>();
        this.argTypes = new ArrayList<CType>();
        this.body = new BlockStatement();
    }
    public String getName() { return name; }
    public CType getReturnType() { return returnType; }
    public boolean isVoidReturn() { return returnType instanceof CVoid; }
    public void addArgument(String name, CType ty) {
        assert (!argNames.contains(name));
        argNames.add(name);
        argTypes.add(ty);
    }
    public final String getArgName(int i) { return argNames.get(i); }
    public final CType getArgType(int i) { return argTypes.get(i); }
    public final int nargs() { return argNames.size(); }
    public final BlockStatement getBody() { return body; }
    public final void addBodyStatement(Statement s) {
        body.addStatement(s);
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append(returnType.toSource()+" "+name+"(");
        for(int i = 0, n=nargs(); i < n; i++) {
            sb.append(getArgType(i).toSource()+" "+getArgName(i));
            if (i+1 < n) sb.append(", ");
        }
        sb.append(")\n");
        sb.append(body.toSource(x)+"\n");
        return sb.toString();
    }
}
