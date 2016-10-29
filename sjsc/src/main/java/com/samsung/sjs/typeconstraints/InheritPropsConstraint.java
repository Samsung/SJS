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
 * a constraint indicating that one term represents the prototype parent of another,
 * and hence the child inherits the properties of the parent.  The child is the nested
 * term of the included {@link ProtoParentTerm}
 * @author m.sridharan
 *
 */
public class InheritPropsConstraint implements ITypeConstraint {

    private final ProtoParentTerm protoParentTerm;

    public InheritPropsConstraint(ProtoParentTerm protoParentTerm) {
        super();
        this.protoParentTerm = protoParentTerm;
    }

    @Override
    public ITypeTerm getLeft() {
        return protoParentTerm;
    }

    @Override
    public ITypeTerm getRight() {
        return protoParentTerm;
    }

    @Override
    public String toString() {
        return "InheritPropsConstraint [protoParentTerm=" + protoParentTerm
                + "]";
    }

}
