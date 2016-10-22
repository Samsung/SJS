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
 * Represent signature of a C function
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c.types;
import java.util.*;
public class CFunctionType extends CType {
    private String name;
    private CType returntype;
    private ArrayList<CType> types;
    public CFunctionType(String name) {
        super(1);
        this.name = name;
        types = new ArrayList<CType>();
    }
    public void setReturn(CType t) {
        returntype = t;
    }
    public CType getReturnType() { return returntype; }
    public void addParameterType(CType t) {
        types.add(t);
    }
    public List<CType> getParamTypes() { return types; }
    @Override
    public String toSource() {
        StringBuilder sb = new StringBuilder();
        sb.append(returntype.toSource());
        sb.append(" ");
        sb.append("(*");
        sb.append(name);
        sb.append(")(");
        int max = types.size();
        for (int i = 0; i < max; i++) {
            sb.append(types.get(i).toSource());
            if (i+1 < max) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }
}
