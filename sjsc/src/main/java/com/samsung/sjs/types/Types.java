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
package com.samsung.sjs.types;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * Helper / factory class for manipulating types
 *
 * @author colin.gordon
 */
public class Types {

	/**
	 * Some of the subtypes of Type are mutable so equals() and hashCode()
	 * methods cannot be written in a reliable way on these types. This
	 * method should be used for comparing types.
	 */
	public static boolean isEqual(Type type1, Type type2){
		return isEqualHelper(type1, type2, HashSetFactory.make());
	}

    private static boolean isEqualHelper(Type type1, Type type2, Set<Pair<Type, Type>> queried) {
        Pair<Type,Type> pair = Pair.make(type1, type2);
        if (queried.contains(pair)) {
            // optimistically assume types are equal
            return true;
        } else {
            queried.add(pair);
        }
        if (type1 == type2){
			return true;
		} else if (type1 instanceof IntegerType && type2 instanceof IntegerType){
			return true;
		} else if (type1 instanceof VoidType && type2 instanceof VoidType){
			return true;
		} else if (type1 instanceof FloatType && type2 instanceof FloatType){
			return true;
		} else if (type1 instanceof BooleanType && type2 instanceof BooleanType){
			return true;
		} else if (type1 instanceof StringType && type2 instanceof StringType){
			return true;
		} else if (type1 instanceof AnyType && type2 instanceof AnyType){
			return true;
		} else if (type1 instanceof ArrayType && type2 instanceof ArrayType){
			ArrayType aType1 = (ArrayType)type1;
			ArrayType aType2 = (ArrayType)type2;
			return isEqualHelper(aType1.elemType(), aType2.elemType(), queried);
		} else if (type1 instanceof MapType && type2 instanceof MapType){
			MapType mType1 = (MapType)type1;
			MapType mType2 = (MapType)type2;
			return isEqualHelper(mType1.elemType(), mType2.elemType(), queried);
		} else if (type1 instanceof FunctionType && type2 instanceof FunctionType){
			// for now, we ignore type parameters
			FunctionType fType1 = (FunctionType)type1;
			FunctionType fType2 = (FunctionType)type2;
			if (fType1.nrParams() != fType2.nrParams()){
				return false;
			} else {
				boolean result = isEqualHelper(fType1.returnType(), fType2.returnType(), queried);
				for (int i=0; i < fType1.nrParams(); i++){
					result = result && isEqualHelper(fType1.paramTypes().get(i), fType2.paramTypes().get(i), queried);
				}
				return result;
			}
		} else if (type1 instanceof AttachedMethodType && type2 instanceof AttachedMethodType){
			// for now, we ignore type parameters
			AttachedMethodType mType1 = (AttachedMethodType)type1;
			AttachedMethodType mType2 = (AttachedMethodType)type2;
			if (mType1.nrParams() != mType2.nrParams()){
				return false;
			} else {
				boolean result = isEqualHelper(mType1.returnType(), mType2.returnType(), queried);
//				result = result && isEqualHelper(mType1.receiverType(), mType2.receiverType());
				for (int i=0; i < mType1.nrParams(); i++){
					result = result && isEqualHelper(mType1.paramTypes().get(i), mType2.paramTypes().get(i), queried);
				}
				return result;
			}
		} else if (type1 instanceof ConstructorType && type2 instanceof ConstructorType){
			// for now, we ignore type parameters
			ConstructorType ctorType1 = (ConstructorType)type1;
			ConstructorType mType2 = (ConstructorType)type2;
			if (ctorType1.nrParams() != mType2.nrParams()){
				return false;
			} else {
				boolean result = isEqualHelper(ctorType1.returnType(), mType2.returnType(), queried);
				for (int i=0; i < ctorType1.nrParams(); i++){
					result = result && isEqualHelper(ctorType1.paramTypes().get(i), ctorType1.paramTypes().get(i), queried);
				}
				return result;
			}
		} else if (type1 instanceof ObjectType && type2 instanceof ObjectType){
			ObjectType oType1 = (ObjectType)type1;
			ObjectType oType2 = (ObjectType)type2;
			Type oType1Parent = oType1.getPrototypeParent();
			Type oType2Parent = oType2.getPrototypeParent();
			if ((oType1Parent != null || oType2Parent != null) && !isEqualHelper(oType1Parent, oType2Parent, queried)) {
			    return false;
			}
			List<Property> oType1Props = oType1.properties();
			if (oType1Props.size() != oType2.properties().size()) {
			    return false;
			}
            for (Property p: oType1Props) {
			    String name = p.getName();
			    if (!oType2.hasProperty(name)) {
			        return false;
			    }
			    Property p2 = oType2.getProperty(name);
			    if (!isEqualHelper(p.getType(), p2.getType(), queried) || p.isRO() != p2.isRO()) {
			        return false;
			    }
			}
			return true;
		} else if (type1 instanceof IntersectionType && type2 instanceof IntersectionType){
			IntersectionType iType1 = (IntersectionType)type1;
			IntersectionType iType2 = (IntersectionType)type2;
			List<Type> types1 = iType1.getTypes();
			List<Type> types2 = iType2.getTypes();
			return isContainedIn(types1, types2) && isContainedIn(types2, types1);
		} else if (type1 instanceof TypeVar && type2 instanceof TypeVar) {
		    return type1.toString().equals(type2.toString());
		}
		return false;
    }

	private static boolean isContainedIn(List<Type> types, Type type){
		for (Type t : types){
			if (isEqual(t, type)){
				return true;
			}
		}
		return false;
	}

	private static boolean isContainedIn(List<Type> types1, List<Type> types2){
		for (Type t : types2){
			if (!isContainedIn(types1, t)){
				return false;
			}
		}
		return true;
	}


    public static Type mkArray(Type celltype) {
        return new ArrayType(celltype) ;
    }

    public static Type mkMap(Type celltype) {
        return new MapType(celltype) ;
    }

    public static ConstructorType mkCtor(List<Type> paramTypes, List<String> paramNames, Type returnType, Type proto) {
        return new ConstructorType(paramTypes, paramNames, returnType, proto);
    }

    public static FunctionType mkFunc(Type ret, List<Type> paramty) {
        List<String> params = new LinkedList<>();
        for (int i = 0; i < paramty.size(); i++) {
            params.add("a"+i);
        }
        return new FunctionType(paramty, params, ret, 0);
    }

    public static FunctionType mkFunc(Type ret, List<Type> paramty, List<String> params) {
        return new FunctionType(paramty, params, ret, 0);
    }

	/**
	 * Total hack.  Like with Array, we need to decide if String is a constructor or not.
	 * @param targetType
	 * @return
	 */
    public static boolean isStringType(Type targetType) {
        if (targetType instanceof IntersectionType) {
            IntersectionType intersectionType = (IntersectionType) targetType;
            List<Type> cases = intersectionType.getTypes();
            if (cases.size() == 3) {
                Type firstCase = cases.get(0);
                if (firstCase instanceof ObjectType && ((ObjectType)firstCase).hasProperty("fromCharCode")) {
                    return true;
                }
            }
        }
        return false;
    }

	/**
     * Temporary hack. Eventually, we should make a decision on whether Array is
     * a constructor or not
     *
     * @param targetType
     * @return
     */
    public static boolean isArrayType(Type targetType) {
        if (targetType instanceof IntersectionType) {
            IntersectionType intersectionType = (IntersectionType) targetType;
            List<Type> cases = intersectionType.getTypes();
            if (cases.size() == 3) {
                for (Type t: cases) {
                    if (!(t instanceof FunctionType && ((FunctionType)t).returnType() instanceof ArrayType)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isMapType(Type type) {
        return type instanceof MapType;
    }

    public static class VoidT extends VoidType {

    }

    public static Type mkAny() {
        return new AnyType();
    }

    public static Type mkVoid() {
        return new VoidT();
    }

    public static FloatType mkFloat() {
        return FloatType.make();
    }

    public static StringType mkString() {
        return StringType.make();
    }

    public static BooleanType mkBool() {
        return BooleanType.make();
    }

    public static IntegerType mkInt() {
        return IntegerType.make();
    }

    public static Property mkProperty(final String name, final Type ty) {
        return new Property(name, ty, true);
    }

    public static AttachedMethodType mkMethod(final Type thistype, final Type ret, List<Type> pty) {
        final List<String> pnames = new LinkedList<String>();
        for (int count = 0; count < pty.size(); count++) {
            pnames.add("x"+count);
        }
        return mkMethod(thistype, ret, pnames, pty);
    }

    public static AttachedMethodType mkMethod(final Type thistype, final Type ret, Type... args) {
        int count = 0;
        final List<Type> pty = new LinkedList<Type>();
        final List<String> pnames = new LinkedList<String>();
        for (Type t : args) {
            pty.add(t);
            pnames.add("x"+count);
            count++;
        }
        return mkMethod(thistype, ret, pnames, pty);
    }

    public static AttachedMethodType mkMethod(Type thistype, Type ret, List<String> pnames, List<Type> pty) {
        return new AttachedMethodType(pty, pnames, ret);
    }

    //private static List<Property> mkArrayProperties(Type thistype, Type elemType) {
    //    LinkedList<Property> props = new LinkedList<Property>();
    //    props.add(mkProperty("length", mkInt()));
    //    props.add(mkProperty("push", mkMethod(thistype, mkVoid(), elemType)));
    //    props.add(mkProperty("pop", mkMethod(thistype, elemType)));
    //    return props;
    //}

    public static ObjectType mkObject(List<Property> props) {
        ObjectType o = new ObjectType();
        for (Property p : props) {
            o.setProperty(p.getName(), p.getType(), p.isRO());
        }
        return o;
    }

    public static class MapIteratorType implements Type {
        private Type elems;
        public MapIteratorType(Type t) {
            elems = t;
        }
        public Type elemType() { return elems; }

        @Override
        public String toString() { return "map_iter<"+elems+">"; }

        @Override public boolean isPrimitive() { return false; }
        @Override public boolean isObject() { return false; }
        @Override public boolean isFunction() { return false; }
        @Override public boolean isConstructor() { return false; }
        @Override public boolean isAttachedMethod() { return false; }
        @Override public boolean isUnattachedMethod() { return false; }
        @Override public boolean isMap() { return false; }
        @Override public boolean isArray() { return false; }
        @Override public boolean isAny() { return false; }
        @Override public boolean isVar() { return false; }
        @Override public boolean isIntersectionType() { return false; }
        @Override
        public RepresentationSort rep() { return RepresentationSort.NEVERBOXED; }
    }

    public static Type mkMapIteratorType(Type t) {
        return new MapIteratorType(t);
    }


    /**
     * Check if a type occurs as a component of a given IntersectionType
     */
	public static boolean isComponentOf(Type type, IntersectionType iType){
		for (Type t : iType.getTypes()){
			if (isEqual(type, t)){
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if subType is a subtype of superType, false otherwise
	 */
	public static boolean isSubtype(Type subType, Type superType) {
	    if (Types.isEqual(subType, superType)) return true;
	    if (subType instanceof IntegerType && superType instanceof FloatType) {
	        return true;
	    }
	    if (superType instanceof TopReferenceType && isRefType(subType)) {
	        return true;
	    }
	    if (subType instanceof BottomReferenceType && isRefType(superType)) {
	        return true;
	    }
	    if (subType instanceof ObjectType && superType instanceof ObjectType) {
	        // NOTE: this only handles concrete types and does not reason about MRO/MRW
	        ObjectType subObjType = (ObjectType) subType;
	        List<Property> superTypeProps = ((ObjectType)superType).properties();
	        // each super type property should be present in the subtype with (1) the same
	        // type and (2) an equally strong or stronger read-write permission
	        for (Property superProp: superTypeProps) {
	            if (!subObjType.hasProperty(superProp.getName())) {
	                return false;
	            }
	            Property subProp = subObjType.getProperty(superProp.getName());
	            if (!Types.isEqual(superProp.getType(), subProp.getType())) {
	                return false;
	            }
	            if (superProp.isRW() && !subProp.isRW()) { // permissions weakened
	                return false;
	            }
	        }
	        return true;
	    }
	    return false;
	}

    public static boolean isRefType(Type t) {
        return t instanceof ObjectType || t instanceof CodeType
                || t instanceof IndexableType || t instanceof ObjectUnionType
                || t instanceof BottomReferenceType || t instanceof TopReferenceType;
    }

	/**
	 * Currently Array and String are represented as intersection types, not
	 * constructor types. This utility function determines whether a type is
	 * legal as a constructor or not.
	 * @param t the type to check
	 * @return true if t can be used as a constructor
	 */
	public static boolean usableAsConstructor(Type t) {
		return t.isConstructor() || isArrayType(t) || isStringType(t);
	}

	/**
	 * Sneaky, slightly modified version of {@link #isSubtype(Type, Type)} for dealing
	 * with object types and indexable types which may be primitive types in disguise.
	 * @param subType    the subtype to check
	 * @param superType  the supertype to check
	 * @return true when <code>subType</code> is a subtype of <code>superType</code>, or
	 *         when <code>subType</code> is a primitive type and <code>superType</code>
	 *         is an object with fields that <em>could</em> be the fields on that
	 *         primitive type, or when <code>subType</code> is a string and
	 *         <code>superType</code> is an unknown indexable type
	 */
	public static boolean isSubtypeish(Type subType, Type superType) {
		if (isSubtype(subType, superType)) {
			return true;
		}
		if (subType instanceof PrimitiveType && superType instanceof ObjectType) {
			PrimitiveType l = (PrimitiveType) subType;
			ObjectType r = (ObjectType) superType;
			for (Property p : r.properties()) {
				try {
					l.getTypeForProperty(p.getName());
				} catch (PropertyNotFoundException e) {
					return false;
				}
			}
			return true;
		}
		if (subType instanceof StringType && superType instanceof UnknownIndexableType) {
			return true;
		}
		return false;
	}

	/**
	 * Compute an approximation of the least-uppper-bound. The approximation is
	 * guaranteed to be tight for primitive types.
	 */
	public static Type coarseUpperBound(Type t1, Type t2) {
		if (isSubtype(t1, t2)) {
			return t2;
		} else if (isSubtype(t2, t1)) {
			return t1;
		}
		return TopType.SINGLETON;
	}

	/**
	 * Compute an approximation of the greatest-lower-bound. The approximation is
	 * guaranteed to be tight for primitive types.
	 */
	public static Type coarseLowerBound(Type t1, Type t2) {
		if (isSubtype(t1, t2)) {
			return t1;
		} else if (isSubtype(t2, t1)) {
			return t2;
		}
		return BottomType.SINGLETON;
	}

	/**
	 * return the lowest subtype of t that is not {@link BottomType}
	 * @param t
	 * @return
	 */
    public static Type lowestSubtype(Type t) {
        if (t instanceof FloatType) {
            return IntegerType.make();
        }
        // for reference types, we really should be returning
        // BottomReferenceType here. But, doing that would require that
        // BottomReferenceType behave like an ObjectType with all possible
        // properties, which we have not implemented.
        // TODO clean this up
        return t;
    }

}
