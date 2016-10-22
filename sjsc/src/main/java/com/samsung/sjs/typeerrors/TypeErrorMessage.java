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
package com.samsung.sjs.typeerrors;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.samsung.sjs.types.StringType;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.UnaryExpression;

import com.samsung.sjs.SourceLocation;
import com.samsung.sjs.constraintsolver.ConstraintUtil;
import com.samsung.sjs.constraintsolver.MROMRWVariable;
import com.samsung.sjs.constraintsolver.TypeAssignment;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.DefaultType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.PrimitiveType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.PropertyContainer;
import com.samsung.sjs.types.PropertyNotFoundException;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.Types;

/**
 * Type errors have a lot of associated data! Text descriptions, line numbers,
 * you name it. Instances of this class hold that data. This class also has
 * convenience methods for assembling type error messages.
 */
public class TypeErrorMessage {

    private final String message;
    private final SourceLocation location;
    private final List<String> notes;

    public TypeErrorMessage(String message, SourceLocation location, List<String> notes) {
        this.message = message;
        this.location = location;
        this.notes = notes;
    }

    private static final int STRING_CROP_LENGTH = 50;

    public static String abbreviate(String str) {
        str = str.trim();
        int brk = str.indexOf('\n');
        brk = brk < 0 ? Math.min(str.length(), STRING_CROP_LENGTH) : Math.min(brk, STRING_CROP_LENGTH);
        return (brk >= 0 && brk < str.length()) ? str.substring(0, brk).trim() + "..." : str;
    }

    public static String shortSrc(AstNode node) {
        return abbreviate(node.toSource());
    }

    public static String describeNode(AstNode node, ITypeTerm term, TypeAssignment solution) {
        return shortSrc(node) + " of type " + describeTypeOf(term, solution);
    }

    public static String describeNode(AstNode node, Type type) {
        return shortSrc(node) + " of type " + describeType(type);
    }

    public static String describeType(Type type) {
        return type.toString();
    }

    public static String describeTypeOf(ITypeTerm term, TypeAssignment solution) {
        return describeType(solution.typeOfTerm(term));
    }

    public static boolean hasType(ITypeTerm t, TypeAssignment solution) {
        Type type;
        try {
            type = solution.typeOfTerm(t);
        } catch (PropertyNotFoundException e) {
            return false;
        }
        return type != null;
    }

    /**
     * @param t a type
     * @param prop a property name
     * @return true if the property is present on the type AND is read-only, false otherwise
     */
    public static boolean isReadOnly(Type t, String prop) {
        if (t instanceof PropertyContainer) {
            try {
                Property p = ((PropertyContainer) t).getProperty(prop);
                return p == null || p.isRO();
            } catch (PropertyNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean hasProperty(Type t, String prop) {
        return (t instanceof PropertyContainer) && ((PropertyContainer) t).hasProperty(prop);
    }

    public static boolean unconstrained(Type type) {
        return type instanceof DefaultType || type instanceof TypeVar || type instanceof AnyType;
    }

    public static TypeErrorMessage genericTypeError(String message, SourceLocation location) {
        return new TypeErrorMessage(message, location, Collections.emptyList());
    }

    public static TypeErrorMessage typeEqualityError(String message, Type actual, Type expected, SourceLocation location) {
        TypeErrorMessage msg = genericTypeError(message, location);

        if (expected instanceof ObjectType && actual instanceof ObjectType) {
            ObjectType e = (ObjectType)expected;
            ObjectType a = (ObjectType)actual;
            for (Property p2 : a.properties()) {
                if (!e.hasProperty(p2.getName())) {
                    msg = msg.withNote("field " + p2.getName() + " was not expected");
                }
            }
            for (Property p1 : e.properties()) {
                Property p2 = null;
                try {
                    p2 = a.getProperty(p1.getName());
                } catch (PropertyNotFoundException ignored) { }
                if (p2 == null) {
                    msg = msg.withNote("field " + p1.getName() + " is missing");
                } else if (!Types.isEqual(p1.getType(), p2.getType())) {
                    msg = msg.withNote("field " + p1.getName() +
                            " has incompatible type " + describeType(p2.getType()) +
                            " instead of " + describeType(p1.getType()));
                } else if (p1.isRW() && !p2.isRW()) {
                    msg = msg.withNote("field " + p1.getName() + " is read-only");
                }
            }
        } else if (expected instanceof CodeType && actual instanceof CodeType) {
            CodeType e = (CodeType)expected;
            CodeType a = (CodeType)actual;
            if (e.nrParams() != a.nrParams()) {
                msg = msg.withNote("differing number of parameters (" + e.nrParams() + " vs " + a.nrParams() + ')');
            } else {
                for (int i = 0; i < e.nrParams(); ++i) {
                    Type et = e.paramTypes().get(i);
                    Type at = a.paramTypes().get(i);
                    if (!Types.isEqual(at, et) && !unconstrained(at) && !unconstrained(et)) {
                        msg = msg.withNote("differing arguments at position" + (i + 1)
                                + " (" + describeType(et) + " vs " + describeType(at) + ')');
                    }
                }
            }
            if (!Types.isEqual(e.returnType(), a.returnType()) && !unconstrained(e.returnType()) && !unconstrained(a.returnType())) {
                msg = msg.withNote("differing return types ("
                        + describeType(e.returnType()) + " vs " + describeType(a.returnType()) + ')');
            }
        } else {
            msg = msg.withNote("type is " + describeType(actual))
                    .withNote("expected " + describeType(expected));
        }

        return msg;
    }

    public static TypeErrorMessage wrongNumberOfParametersAtCallSite(int expected, int actual, AstNode callSite) {
        return genericTypeError("wrong number of parameters in call: " + shortSrc(callSite), locationOf(callSite))
            .withNote(expected + " expected, " + actual + " given");
    }

    public static TypeErrorMessage subtypeError(String message, Type subtype, Type supertype, SourceLocation location) {
        TypeErrorMessage msg = genericTypeError(message, location);

        if (subtype instanceof ObjectType && supertype instanceof ObjectType) {
            ObjectType l = (ObjectType)subtype;
            ObjectType r = (ObjectType)supertype;
            for (Property p1 : r.properties()) {
                Property p2 = null;
                try {
                    p2 = l.getProperty(p1.getName());
                } catch (PropertyNotFoundException ignored) { }
                if (p2 == null) {
                    msg = msg.withNote("field " + p1.getName() + " is missing");
                } else if (!Types.isEqual(p1.getType(), p2.getType())) {
                    msg = msg.withNote("field "
                            + p1.getName()
                            + " has incompatible type " + describeType(p2.getType())
                            + " instead of " + describeType(p1.getType()));
                } else if (p1.isRW() && !p2.isRW()) {
                    msg = msg.withNote("field " + p1.getName() + " is read-only");
                }
            }
        } else {
            msg = msg.withNote("type is " + describeType(subtype))
                    .withNote("expected " + describeType(supertype));
        }

        return msg;
    }

    public static TypeErrorMessage concretenessError(AstNode node, ITypeTerm term, TypeAssignment solution) {
        Type type = solution.typeOfTerm(term);
        if (type instanceof AttachedMethodType) {
            return genericTypeError("in " + shortSrc(node) + ": it is illegal to detach a method from an object",
                    locationOf(node));
        } else {
            TypeErrorMessage msg = genericTypeError(shortSrc(node) + " is abstract", locationOf(node));
            if (type instanceof ObjectType) {
                ObjectType ot = (ObjectType)type;
                Set<Property> roProps = ot.getROProperties();
                List<Property> rwProps = ot.getRWProperties();
                MROMRWVariable var = solution.getMROMRWVarForTerm(term);
                List<Property> missingMRO = var
                        .getMRO()
                        .stream()
                        .filter((p) -> !ConstraintUtil.getPropWithName(p.getName(),
                                roProps).isPresent()
                                && !ConstraintUtil.getPropWithName(
                                p.getName(), rwProps).isPresent()).collect(Collectors.toList());
                List<Property> missingMRW = var
                        .getMRW()
                        .stream()
                        .filter((p) -> !ConstraintUtil.getPropWithName(p.getName(),
                                rwProps).isPresent()).collect(Collectors.toList());
                for (Property p : missingMRO) {
                    msg = msg.withNote("missing read-only property '" + p.getName() + "'");
                }
                for (Property p : missingMRW) {
                    msg = msg.withNote("missing read-write property '" + p.getName() + "'");
                }
            }
            return msg;
        }
    }

    public static TypeErrorMessage binaryOperatorMisuse(InfixExpression node, Type t1, Type t2, Type outType) {
        TypeErrorMessage msg = genericTypeError("misuse of binary operator " + AstNode.operatorToString(node.getOperator()) + " in \"" + shortSrc(node) + '"',
                locationOf(node));
        if (node.getOperator() == Token.IN) {
            if (!unconstrained(t1) && !Types.isEqual(t1, StringType.make())) {
                msg = msg.withNote("left operand has type " + describeType(t1) + " instead of " + StringType.make());
            }
            if (!unconstrained(t2) && !Types.isMapType(t2)) {
                msg = msg.withNote("right operand has type " + describeType(t2) + " instead of " + new MapType(new DefaultType()));
            }
        } else {
            if (!unconstrained(t1)) {
                msg = msg.withNote("left operand has type " + describeType(t1));
            }
            if (!unconstrained(t2)) {
                msg = msg.withNote("right operand has type " + describeType(t2));
            }
        }
        if (!unconstrained(outType)) {
            msg = msg.withNote("result type is " + describeType(outType));
        }
        return msg;
    }

    public static TypeErrorMessage unaryOperatorMisuse(UnaryExpression node, Type inType, Type outType) {
        TypeErrorMessage msg = genericTypeError("misuse of unary operator " + AstNode.operatorToString(node.getOperator()) + " in " + shortSrc(node),
                locationOf(node));
        if (!unconstrained(inType)) {
            msg = msg.withNote("input has type " + describeType(inType));
        }
        if (!unconstrained(outType)) {
            msg = msg.withNote("result type is " + describeType(outType));
        }
        return msg;
    }

    public static TypeErrorMessage badPropertyRead(InfixExpression node, Type baseType, Type fieldType, Type expectedType) {
        assert node.getRight() instanceof Name;
        String fieldName = ((Name) node.getRight()).getIdentifier();
        if (fieldType != null && hasProperty(baseType, fieldName)) {
            return genericTypeError("property '" + fieldName + "' does not have type " + describeType(expectedType), locationOf(node))
                    .withNote("type is " + describeType(fieldType));
        } else if (baseType instanceof MapType) {
            return genericTypeError("cannot read property '" + fieldName
                    + "' from map object " + shortSrc(node.getLeft())
                    + " using '.' syntax; write " + shortSrc(node.getLeft()) + "['" + fieldName + "'] instead",
                    locationOf(node));
        } else {
            return genericTypeError("read of missing property '" + fieldName + "' on " + shortSrc(node.getLeft()), locationOf(node))
                    .withNote("type is " + describeType(baseType));
        }
    }

    public static TypeErrorMessage badPropertyWrite(PropertyGet node, Type baseType, Type fieldType, Type expType) {
        SourceLocation location = locationOf(node);
        String fieldName = node.getProperty().getIdentifier();
        if (fieldType != null) {
            if (baseType instanceof PrimitiveType) {
                return genericTypeError("cannot assign to property '" + fieldName + "' on primitive type " + describeType(baseType),
                        location);
            }
            if (expType instanceof CodeType && fieldType instanceof DefaultType) {
                return genericTypeError("cannot attach method to field '" + fieldName + "'", location)
                        .withNote("methods may only be attached to object literals, 'this', and constructor prototypes");
            }
            assert !Types.isSubtype(fieldType, expType) || isReadOnly(baseType, fieldName) :
                    "I don't know how to explain this failure";
            if (!Types.isSubtype(fieldType, expType)) {
                return subtypeError("bad assignment to property '" + fieldName + "'",
                        expType, fieldType, location);
            } else {
                return genericTypeError("cannot assign to read-only field '" + fieldName + "'",
                        location);
            }
        } else {
            return genericTypeError(shortSrc(node) + " has no property '" + fieldName + "'", location)
                    .withNote("type is " + describeType(baseType));
        }
    }

    public static SourceLocation locationOf(AstNode node) {
        final int line = node.getLineno();
        return () -> line;
    }

    public TypeErrorMessage withNote(String note) {
        List<String> notes = new ArrayList<>(this.notes);
        notes.add(note);
        return new TypeErrorMessage(this.message, this.location, notes);
    }

    public void prettyprint(PrintStream out) {
        out.println("Error on line " + location.getStartLine() + ": " + message);
        for (String note : notes) {
            out.println("    Note: " + note);
        }
    }

}
