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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;

public class ConstraintUtil {

    public static Optional<Property> getPropWithName(String name, Collection<Property> props) {
        return props.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    public static void equatePropertyTypeWithMROMRWSets(MROMRWVariable mroMRWVar, Property p, TypeConstraintFixedPointSolver solver, Cause reason, Logger logger) {
        String name = p.getName();
        Type type = p.getType();
        Consumer<? super Property> propTypeEquator = (prop) -> {
            Type propType = prop.getType();
            logger.debug("equate {} to {}", propType, type);
            solver.equateTypes(propType, type, reason);
        };
        getPropWithName(name, mroMRWVar.getMRO()).ifPresent(propTypeEquator);
        getPropWithName(name, mroMRWVar.getMRW()).ifPresent(propTypeEquator);
    }

    /**
     * copy property p into the MRO set of var, unless it's already in the MRO or MRW set,
     * in which case the types are equated
     * @param p
     * @param var
     * @param logger
     * @param solver
     * @return
     */
    public static boolean copyIntoMRO(Property p, MROMRWVariable var, Logger logger, TypeConstraintFixedPointSolver solver, Cause reason) {
        Set<Property> mrw = var.getMRW();
        Set<Property> mro = var.getMRO();
        Optional<Property> extantMRW = ConstraintUtil.getPropWithName(p.getName(), mrw);
        if (extantMRW.isPresent()) {
            Property mrwProp = extantMRW.get();
            // equate the property types
            logger.debug("equating types {} and {}", mrwProp.getType(), p.getType());
            solver.equateTypes(mrwProp.getType(), p.getType(), reason);
        } else {
            Optional<Property> extantMRO = ConstraintUtil.getPropWithName(p.getName(), mro);
            if (extantMRO.isPresent()) {
                Property mroProp = extantMRO.get();
                // equate the property types
                logger.debug("equating types {} and {}", mroProp.getType(), p.getType());
                solver.equateTypes(mroProp.getType(), p.getType(), reason);
            } else {
                logger.debug("adding {} into MRO", p);
                var.addMRO(p, reason);
                return true;
            }
        }
        return false;
    }

    /**
     * copy property p into MRW set of var.  If property with same name as p is already in MRO,
     * move it to MRW.
     * @param p
     * @param var
     * @param logger
     * @param solver
     * @return
     */
    public static boolean copyIntoMRW(Property p, MROMRWVariable var, Logger logger, TypeConstraintFixedPointSolver solver, Cause reason) {
        Set<Property> mro = var.getMRO();
        Set<Property> mrw = var.getMRW();
        Optional<Property> extantMRO = ConstraintUtil.getPropWithName(p.getName(), mro);
        if (extantMRO.isPresent()) {
            // equate types, and move it to the MRW
            Property mroProp = extantMRO.get();
            logger.debug("equating types {} and {}", mroProp.getType(), p.getType());
            solver.equateTypes(mroProp.getType(), p.getType(), reason);
            assert !ConstraintUtil.getPropWithName(p.getName(), mrw).isPresent() : "duplicated mro and mrw property "
            + p.getName() + " " + var;
            logger.debug("moving {} to MRW", mroProp);
            var.removeMRO(mroProp, reason);
            var.addMRW(new Property(mroProp.getName(), mroProp.getType(), false, mroProp.getSourceLoc()), reason);
            return true;
        } else {
            Optional<Property> extantMRW = ConstraintUtil.getPropWithName(p.getName(), mrw);
            if (extantMRW.isPresent()) {
                Property mrwProp = extantMRW.get();
                // equate the property types
                logger.debug("equating types {} and {}", mrwProp.getType(), p.getType());
                solver.equateTypes(mrwProp.getType(), p.getType(), reason);
            } else {
                logger.debug("adding {} to MRW", p);
                var.addMRW(p, reason);
                return true;
            }
        }
        return false;
    }
}
