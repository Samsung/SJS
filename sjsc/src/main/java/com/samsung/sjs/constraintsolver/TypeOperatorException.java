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
package com.samsung.sjs.constraintsolver;

import com.samsung.sjs.typeconstraints.ITypeTerm;

@SuppressWarnings("serial")
public class TypeOperatorException extends CoreException {

    private final ITypeTerm firstSource;

    private final ITypeTerm secondSource;

    /**
     * the term whose type was being updated / processed by the constraint
     */
    private final ITypeTerm targetTerm;

    public static enum OperatorType { JOIN,  MEET, INSIDE };

    private final OperatorType type;

    public TypeOperatorException(String message, ITypeTerm firstSource, ITypeTerm secondSource, ITypeTerm targetTerm, OperatorType type, Cause reason) {
        super(message, reason);
        this.firstSource = firstSource;
        this.secondSource = secondSource;
        this.targetTerm = targetTerm;
        this.type = type;
    }


    private static String genTermStr(ITypeTerm term, String label) {
        String result = label + ": ";
        if (term != null) {
            result += term.toString();
            if (term.getNode() != null) {
                result += " at line " + term.getNode().getLineno();
            }
        } else {
            result += "unknown";
        }
        return result;
    }

    public String explanation() {
        StringBuilder result = new StringBuilder(message);
        result.append("\n");
        String label1 = null;
        switch (type) {
        case JOIN:
        case INSIDE:
            label1 = "type source";
            break;
        case MEET:
            label1 = "use";
            break;
        default:
            assert false;
        }
        result.append(genTermStr(firstSource, label1));
        result.append("\n");
        String label2 = null;
        switch (type) {
        case JOIN:
            label2 = "type source";
            break;
        case INSIDE:
        case MEET:
            label2 = "use";
            break;
        default:
            assert false;
        }
        result.append(genTermStr(secondSource, label2));
        result.append("\n");
        result.append(genTermStr(targetTerm, "target term"));
        return result.toString();
    }

}
