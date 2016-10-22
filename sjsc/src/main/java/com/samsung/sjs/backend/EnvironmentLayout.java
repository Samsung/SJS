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
 * Represent the environment of a closure
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;
import com.samsung.sjs.types.*;
import org.mozilla.javascript.ast.Name;
import java.util.*;
public final class EnvironmentLayout {
    private ArrayList<String> names;
    private int nextOffset;
    private Map<String,Integer> offsets;
    private Set<Name> directcalls;
    public EnvironmentLayout() {
        names = new ArrayList<String>();
        nextOffset = 0;
        offsets = new HashMap<String,Integer>();
        directcalls = new HashSet<Name>();
    }
    public void addDirectCall(Name n) {
        directcalls.add(n);
    }
    public boolean isDirectCall(Name n) {
        return directcalls.contains(n);
    }
    public boolean isEmpty() { return nextOffset == 0; }
    public int size() { return nextOffset; }
    public String getName(int i) { return names.get(i); }
    public void addCapturedVariable(String name) {
        if (!names.contains(name)) {
            names.add(name);
            offsets.put(name,nextOffset);
            nextOffset++;
        }
    }
    public boolean contains(String n) {
        return names.contains(n);
    }
    public int getOffset(String n) {
        Integer i = offsets.get(n);
        if (i == null) {
            System.err.println("ERROR getting offset of "+n);
            System.err.println("Env: "+toString());
            throw new IllegalArgumentException();
        }
        return i;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Env[ ");
        for (int i = 0; i < nextOffset; i++) {
            String name = names.get(i);
            sb.append(name+";");
            if (i + 1 < nextOffset) sb.append(" ");
        }
        sb.append(" ]");
        return sb.toString();
    }
}
