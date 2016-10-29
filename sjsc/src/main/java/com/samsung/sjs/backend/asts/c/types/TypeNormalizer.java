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
 * An interning system for rendering of anonymous types, by collecting, naming,
 * and generating typedefs.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c.types;
import java.util.*;
public class TypeNormalizer {
    private int nextid;
    private Map<String,Integer> named;
    private Map<Integer,String> canonical_by_id;
    public TypeNormalizer() {
        nextid = 0;
        named = new HashMap<String,Integer>();
        canonical_by_id = new HashMap<>();
    }
    public String normalize(String s) {
        Integer id = named.get(s);
        if (id == null) {
            id = nextid++;
            named.put(s, id);
            canonical_by_id.put(id, s);
        }
        return "anontype"+id.toString()+"_t";
    }
    public String genTypeDefs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nextid; i++) {
            String rep = canonical_by_id.get(i);
            assert (rep != null);
            sb.append(("typedef "+rep+" anontype"+i+"_t;\n"));
        }
        return sb.toString();
    }
}
