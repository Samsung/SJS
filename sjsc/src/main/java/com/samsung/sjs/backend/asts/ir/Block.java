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
 * Block
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;

public class Block extends Statement implements Iterable<Statement> {
    private ArrayList<Statement> body;
    public Block() {
        super(Tag.Block);
        body = new ArrayList<Statement>();
    }
    public void addStatement(Statement s) {
        body.add(s);
    }
    public Iterator<Statement> iterator() {
        return body.iterator();
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x,sb);
        sb.append("{\n");
        for (Statement s : this) {
            indent(x+2,sb);
            sb.append(s.toSource(0));
        }
        sb.append("}");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitBlock(this);
    }
    public void prefixStatement(Statement s) {
        body.add(0, s);
    }
}
