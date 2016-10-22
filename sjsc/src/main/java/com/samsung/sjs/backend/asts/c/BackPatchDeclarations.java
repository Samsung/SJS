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
 * A pseudo-block with no delimiters that can be back-filled with
 * declarations.
 *
 * @colin.gordon
 */

package com.samsung.sjs.backend.asts.c;
import java.util.LinkedList;
public class BackPatchDeclarations extends Statement {
    private LinkedList<FunctionPreDeclaration> decls;
    private String typedefs;
    public BackPatchDeclarations() {
        decls = new LinkedList<FunctionPreDeclaration>();
    }
    public void setTypeDefs(String s) {
        typedefs = s;
    }
    public void preDeclare(FunctionDeclaration d) {
        decls.add(new FunctionPreDeclaration(d));
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        if (typedefs != null) {
            sb.append(typedefs);
        }
        for (FunctionPreDeclaration pd : decls) {
            indent(x,sb);
            sb.append(pd.toSource(0));
        }
        return sb.toString();
    }
}
