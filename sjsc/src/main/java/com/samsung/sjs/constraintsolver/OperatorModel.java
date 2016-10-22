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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.wala.util.collections.HashMapFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.BooleanType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.Types;

/**
 * This class provides a model for the types of expressions involving built-in unary and
 * binary operators such as "+" and "~". The class reads the model from the "operators.json"
 * file in src/main/resources and stores it into two maps. These maps can be consulted through
 * the getTypeOfUnaryExpression() and getTypeOfInfixExpression() methods.
 *
 * @author ftip
 *
 */
public class OperatorModel {

    /**
     * type information for one case of an infix operator: the left type, right
     * type, and result type
     *
     * @author m.sridharan
     */
    public static class InfixOpTypeCase {

        final Type leftType;
        final Type rightType;
        final Type resultType;


        InfixOpTypeCase(Type leftType, Type rightType, Type resultType) {
            super();
            this.leftType = leftType;
            this.rightType = rightType;
            this.resultType = resultType;
        }
    }

    /**
     * type information for one case of a unary operator: the operand type, the
     * result type, and whether the operator must be is in the prefix position
     *
     * @author m.sridharan
     *
     */
    public static class UnOpTypeCase {

        final Type operandType;
        final Type resultType;
        final boolean isPrefix;
        UnOpTypeCase(Type operandType, Type resultType, boolean isPrefix) {
            super();
            this.operandType = operandType;
            this.resultType = resultType;
            this.isPrefix = isPrefix;
        }


    }

    public OperatorModel() {
        Reader reader = new InputStreamReader(
                OperatorModel.class.getResourceAsStream("/operators.json"));

        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(reader);
        if (element.isJsonArray()) {
            JsonArray jsa = element.getAsJsonArray();
            for (Iterator<JsonElement> it = jsa.iterator(); it.hasNext();) {
                JsonElement operatorEntry = it.next();
                if (operatorEntry.isJsonObject()) {
                    JsonObject jso = operatorEntry.getAsJsonObject();
                    for (Entry<String, JsonElement> entry : jso.entrySet()) {
                        String operatorName = entry.getKey();
                        JsonElement value = entry.getValue();
                        if (value.isJsonArray()) {
                            JsonArray elements = value.getAsJsonArray();
                            for (Iterator<JsonElement> it2 = elements
                                    .iterator(); it2.hasNext();) {
                                JsonElement element2 = it2.next();
                                if (element2.isJsonObject()) {
                                    JsonObject object = element2
                                            .getAsJsonObject();
                                    JsonElement jsonElement = object
                                            .get("operand");
                                    if (jsonElement != null) {
                                        String op = jsonElement.getAsString();
                                        String result = object.get("result")
                                                .getAsString();
                                        String prefix = object.get("isprefix")
                                                .getAsString();
                                        boolean isPrefix;
                                        if (prefix.equals("true")) {
                                            isPrefix = true;
                                        } else if (prefix.equals("false")) {
                                            isPrefix = false;
                                        } else {
                                            throw new Error(
                                                    "unrecognized value for prefix status of unary operator: "
                                                            + prefix);
                                        }
                                        if (!unaryOperatorMap
                                                .containsKey(operatorName)) {
                                            unaryOperatorMap
                                                    .put(operatorName,
                                                            new ArrayList<UnOpTypeCase>());
                                        }
                                        List<UnOpTypeCase> cases = unaryOperatorMap
                                                .get(operatorName);
                                        cases.add(new UnOpTypeCase(toType(op),
                                                toType(result), isPrefix));
                                    } else {
                                        String left = object.get("left")
                                                .getAsString();
                                        String right = object.get("right")
                                                .getAsString();
                                        String result = object.get("result")
                                                .getAsString();
                                        if (!infixOperatorMap
                                                .containsKey(operatorName)) {
                                            infixOperatorMap
                                                    .put(operatorName,
                                                            new ArrayList<InfixOpTypeCase>());
                                        }
                                        List<InfixOpTypeCase> cases = infixOperatorMap
                                                .get(operatorName);
                                        cases.add(new InfixOpTypeCase(
                                                toType(left), toType(right),
                                                toType(result)));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    throw new Error("JsonObject expected");
                }
            }
        } else {
            throw new Error("JsonArray expected");
        }
    }

	/**
	 * convert from String to Type
	 */
	private Type toType(String typeName){
		switch (typeName){
		case "int":
			return IntegerType.make();
		case "float":
			return FloatType.make();
		case "boolean":
			return BooleanType.make();
		case "string":
			return StringType.make();
		case "reference":
            return TopReferenceType.make();
		case "object":
			return ConstraintGenUtil.OBJECT_TYPE;
		case "array":
			return ConstraintGenUtil.ARRAY_TYPE;
		case "function":
			return ConstraintGenUtil.FUNCTION_TYPE;
		case "map":
		    return ConstraintGenUtil.MAP_TYPE;
		case "typevar":

		default:
			throw new Error("type not supported: " + typeName);
		}
	}

	/**
	 * A more specific case is *less than* a less specific case.
	 *
	 * If the cases are incomparable, we throw a runtime exception.
	 */
	private final Comparator<InfixOpTypeCase> moreSpecificInfix = (c1, c2) -> {
	    if (Types.isSubtype(c1.leftType, c2.leftType) && Types.isSubtype(c1.rightType, c2.rightType)) {
	        // c1 is more specific (less than) c2
	        return -1;
	    } else if (Types.isSubtype(c2.leftType, c1.leftType) && Types.isSubtype(c2.rightType, c1.rightType)) {
	        // c2 is more specific (less than) c1
	        return 1;
	    } else {
	        throw new RuntimeException("not expecting incomparable cases here");
	    }
	};

    private Type normalizeType(Type type) {
        if (type.isObject()) type = ConstraintGenUtil.OBJECT_TYPE;
        if (type.isArray()) type = ConstraintGenUtil.ARRAY_TYPE;
        if (type.isFunction()) type = ConstraintGenUtil.FUNCTION_TYPE;
        if (type.isMap()) type = ConstraintGenUtil.MAP_TYPE;
        return type;
    }

    private List<InfixOpTypeCase> getFilteredInfixCases(String operatorName,
            Predicate<? super InfixOpTypeCase> pred) {
        if (!infixOperatorMap.containsKey(operatorName)) {
	        throw new RuntimeException("unsupported operator " + operatorName);
	    }
        return infixOperatorMap.get(operatorName).stream().filter(pred).collect(Collectors.toList());
    }

    public List<InfixOpTypeCase> getInfixCases(String operatorName, Type leftType, Type rightType, Type resultType) {
        final Type normLeft   = leftType   != null ? normalizeType(leftType)   : null;
        final Type normRight  = rightType  != null ? normalizeType(rightType)  : null;
        final Type normResult = resultType != null ? normalizeType(resultType) : null;
        return getFilteredInfixCases(operatorName, c ->
                (leftType   == null || Types.isSubtypeish(normLeft, c.leftType)) &&
                (rightType  == null || Types.isSubtypeish(normRight, c.rightType)) &&
                (resultType == null || Types.isSubtypeish(c.resultType, normResult)));
    }

    /**
     * A more specific case is *less than* a less specific case.
     *
     * If the cases are incomparable, we throw a runtime exception.
     */
    private final Comparator<UnOpTypeCase> moreSpecificUnary = (c1, c2) -> {
        if (Types.isSubtype(c1.operandType, c2.operandType)) {
            // c1 is more specific (less than) c2
            return -1;
        } else if (Types.isSubtype(c2.operandType, c1.operandType)) {
            // c2 is more specific (less than) c1
            return 1;
        } else if (c1.operandType instanceof ObjectType && c2.operandType instanceof ArrayType) {
        	// say that arrays are more specific than objects, even though really they are incomparable
        	return 1;
        } else if (c1.operandType instanceof ArrayType && c2.operandType instanceof ObjectType) {
        	// say that arrays are more specific than objects, even though really they are incomparable
        	return -1;
        } else {
            throw new RuntimeException("not expecting incomparable cases here");
        }
    };

	/**
	 * This method determines the type of a UnaryExpression for a given operatorName,
	 * and operand type.
	 */
	public UnOpTypeCase getTypeOfUnaryExpression(String operatorName, Type operandType, boolean isPrefix) {
		if (!unaryOperatorMap.containsKey(operatorName)){
			throw new Error("unsupported unary operator: " + operatorName);
		} else {
			final Type finOperandType = normalizeType(operandType);
            return unaryOperatorMap
                    .get(operatorName)
                    .stream()
                    .filter((c) -> {
                        return Types.isSubtype(finOperandType, c.operandType)
                                && c.isPrefix == isPrefix;
                    })
                    .min(moreSpecificUnary)
                    .<RuntimeException> orElseThrow(
                            () -> {
                                String prefix = isPrefix ? "prefix" : "postfix";
                                throw new IllegalArgumentException(
                                        "unsupported operand type "
                                                + finOperandType + " for "
                                                + prefix + " " + operatorName);
                            });
		}
	}

	private Map<String,List<InfixOpTypeCase>> infixOperatorMap = HashMapFactory.make();
	private Map<String,List<UnOpTypeCase>> unaryOperatorMap = HashMapFactory.make();
}
