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

import com.samsung.sjs.types.Types;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NewExpression;

import com.samsung.sjs.typeconstraints.FunctionCallTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.UnattachedMethodType;

/**
 * Perform checks on inferred solutions. For now, it only checks that
 * the number of arguments in a call is the same as the number of
 * arguments in the FunctionType inferred for that call.
 *
 * @author ftip
 */
public class SolutionChecker {

	public static void checkFunctionRet(FunctionReturnTerm t) {
		FunctionReturnTerm frt = (FunctionReturnTerm)t;
		int nrArgsInCall = frt.getNrParams();
		ITypeTerm functionTerm = frt.getFunctionTerm();
		Type type = functionTerm.getType();
		if (type instanceof FunctionType){
			int nrParamsInFun = ((FunctionType)frt.getFunctionTerm().getType()).nrParams();
			if (nrArgsInCall != nrParamsInFun){
				inconsistentArgsFailure(functionTerm);
			}
		} else if (type instanceof ConstructorType) {
			int nrParamsInFun = ((ConstructorType)frt.getFunctionTerm().getType()).nrParams();
			if (nrArgsInCall != nrParamsInFun){
				inconsistentArgsFailure(functionTerm);
			}
		} else if (type instanceof AttachedMethodType) {
			int nrParamsInFun = ((AttachedMethodType)frt.getFunctionTerm().getType()).nrParams();
			if (nrArgsInCall != nrParamsInFun){
				inconsistentArgsFailure(functionTerm);
			}
		} else if (type instanceof UnattachedMethodType) {
			int nrParamsInFun = ((UnattachedMethodType)frt.getFunctionTerm().getType()).nrParams();
			if (nrArgsInCall != nrParamsInFun){
				inconsistentArgsFailure(functionTerm);
			}
		} else  if (type instanceof IntersectionType) {
			IntersectionType iType = (IntersectionType)type;
			boolean found = false;
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof FunctionType){
					FunctionType fType = (FunctionType)componentType;
					if (fType.nrParams() == nrArgsInCall){
						found = true;
						break;
					}
				} else if (componentType instanceof AttachedMethodType){
					AttachedMethodType fType = (AttachedMethodType)componentType;
					if (fType.nrParams() == nrArgsInCall){
						found = true;
						break;
					}
				}
			}
			if (!found){
				inconsistentArgsFailure(functionTerm);
			}
		} else {
			throw new Error("unhandled case: " + type.getClass().getName());
		}
	}

	public static void checkFunctionCall(FunctionCallTerm t) {
		AstNode node = t.getNode();
		if (node instanceof NewExpression) {
			ITypeTerm target = t.getTarget();
			Type targetType = target.getType();
			if (!Types.usableAsConstructor(targetType)) {
				throw new SolverException("invoking non-constructor function (type=" + targetType + ") with new (line " + node.getLineno() + ")");
			}
		}
	}

    private static void inconsistentArgsFailure(ITypeTerm functionTerm) {
        String message = "inconsistent number of arguments and parameters";
        AstNode node = functionTerm.getNode();
        if (node != null) {
            message += " at call site on line " + node.getLineno();
        }
        throw new SolverException(message);
    }

}
