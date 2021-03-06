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
 * Representation of source file, with global scope
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;

public class Script extends IRNode implements Iterable<Statement>, ILexicalScope {
    private Scope global_scope;
    private Block code;

    public Script(Block b, Scope s) {
        super(Tag.Script);
        global_scope = s;
        code = b;
    }
    @Override
    public String toSource(int x) {
        return code.toSource(x);
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitScript(this);
    }

    public Block getBody() {
        return code;
    }

    public Scope getScope() {
        return global_scope;
    }

    public Iterator<Statement> iterator() {
        return code.iterator();
    }

    public void prefixStatement(Statement s) {
        code.prefixStatement(s);
    }
}
