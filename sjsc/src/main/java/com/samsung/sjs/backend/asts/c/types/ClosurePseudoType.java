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
 * Represent the C closure-representation of our SJS closures
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c.types;
import com.samsung.sjs.backend.asts.c.*;
import java.util.*;
public class ClosurePseudoType extends CType {
    private CFunctionType code_ptr;
    private String render;
    private boolean ctor;
    public ClosurePseudoType(CFunctionType code, TypeNormalizer tn) {
        super(1);
        code_ptr = code;
        render = tn.normalize(toAnonSource());
        ctor = false;
    }
    public ClosurePseudoType(CFunctionType code, TypeNormalizer tn, boolean ctor) {
        super(1);
        code_ptr = code;
        this.ctor = ctor;
        render = tn.normalize(toAnonSource());
    }
    public String toAnonSource() {
        //return "CLOSURETY2("+code_ptr.toSource()+")"+getStars();
        StringBuilder sb = new StringBuilder();
        //sb.append("CLOSURETY("+code_ptr.getReturnType().toSource());
        sb.append("struct { const env_t env; "+code_ptr.getReturnType().toSource());
        //sb.append(" (func*)(");
        sb.append(" (* const func)(");
        for (int i = 0; i < code_ptr.getParamTypes().size(); i++) {
        //for (CType t : code_ptr.getParamTypes()) {
            //sb.append(", "+t.toSource());
            sb.append(code_ptr.getParamTypes().get(i).toSource());
            if (i+1 < code_ptr.getParamTypes().size()) sb.append(", ");
        }
        //sb.append(")");
        if (ctor) {
            sb.append("); object_t* proto; }*");
        } else {
            sb.append("); }*");
        }
        return sb.toString();
    }
    @Override
    public String toSource() {
        if (ctor) {
            return "constructor_t*";
        } else {
            return "closure_t*";
        }
    }
    public CFunctionType getFunType() {
        return code_ptr;
    }
}
