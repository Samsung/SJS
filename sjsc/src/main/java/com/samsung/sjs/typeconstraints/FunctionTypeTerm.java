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
package com.samsung.sjs.typeconstraints;

/**
 * Created by schandra on 3/10/15.
 */

import org.mozilla.javascript.ast.AstNode;

import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.Type;

/**
 * An instance of this Term will be involved in subtype constraints.
 * Note that this term is not associated with a fragment of the AST, rather this is just a type.
 */

public class FunctionTypeTerm extends ATerm implements ITypeTerm {

    public FunctionTypeTerm(CodeType type) {
        super(null);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(Type ty) {
        assert false;
    }

    @Override
    public AstNode getNode() {
        return null;
    }

    @Override
    public String stringRepresentation() {
        return type.toString();
    }


    private Type type;
}
