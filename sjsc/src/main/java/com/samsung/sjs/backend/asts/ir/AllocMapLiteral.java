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
 * AllocMapLiteral
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;
import com.samsung.sjs.types.Type;

public class AllocMapLiteral extends Expression implements Iterable<AllocMapLiteral.KVPair> {

    public static class KVPair {
        public final String name;
        public final Expression val;
        public KVPair(String n, Expression e) {
            name = n;
            val = e;
        }
    }

    public Iterator<KVPair> iterator() {
        return entries.iterator();
    }

    private ArrayList<KVPair> entries;
    private Type type;

    public AllocMapLiteral(Type t) {
        super(Tag.AllocObjectLiteral);
        entries = new ArrayList<>();
        type = t;
    }
    public int nentries() { return entries.size(); }
    public Type getRangeType() { return type; }

    public void addEntry(String name, Expression v) {
        entries.add(new KVPair(name, v));
    }

    @Override
    public String toSource(int x) {
        return "<TODO:mapliteral>";
    }

    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitAllocMapLiteral(this);
    }
    @Override
    public boolean mustSaveIntermediates() {
        for (KVPair kv : entries) {
            if (kv.val.mustSaveIntermediates()) {
                return true;
            }
        }
        return false;
    }
}
