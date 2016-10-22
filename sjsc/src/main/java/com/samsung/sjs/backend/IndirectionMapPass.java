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
 * A compiler pass to calculate the indirection maps, and generate
 * allocations for them.
 *
 * The only time we need to (potentially) generate a new vtable is when
 * we create a new object (which may have a different layout from other objects)
 * or when generating the table for an FFI object.  The main compiler driver
 * takes care of the latter, so here we just need to handle object allocations
 * here: new expressions and object literals
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.*;
import com.samsung.sjs.types.*;
import org.mozilla.javascript.ast.*;
import java.util.*;

public class IndirectionMapPass extends ExternalRhinoVisitor {
    private Map<AstNode,Type> types;
    private IRFieldCollector.FieldMapping field_codes;
    private Map<AstNode,Integer> vtable_needs;
    private Map<Integer,int[]> tables;
    private int nextid;

    public IndirectionMapPass(Map<AstNode,Type> types, IRFieldCollector.FieldMapping field_codes) {
        this.types = types;
        this.field_codes = field_codes;
        vtable_needs = new HashMap<AstNode,Integer>();
        tables = new HashMap<Integer,int[]>();
        nextid = 0;
    }
    
    @Override
    protected void visitObjectLiteral(ObjectLiteral node) {
        int id = nextid++;
        vtable_needs.put(node,id);
        int[] vt = new int[field_codes.size()];
        java.util.Arrays.fill(vt, -1);
        int physical_index = 0;
        for (ObjectProperty prop : node.getElements()) {
            Name f = (Name)prop.getLeft();
            vt[field_codes.indexOf(f.getIdentifier())] = physical_index;
            physical_index++;
        }
        tables.put(id,vt);
        super.visitObjectLiteral(node);
    }

    @Override
    protected void visitNewExpr(NewExpression node) {
        throw new UnsupportedOperationException(); // temporary
    }

    public IndirectionMapping computeVTables() {
        int[][] tablesById = new int[nextid][];
        for (int i = 0; i < nextid; i++) {
            tablesById[i] = tables.get(i);
        }
        return new IndirectionMapping(tablesById, vtable_needs);
    }
    
    public static class IndirectionMapping {
        private int[][] maps;
        private Map<AstNode,Integer> ids;
        public IndirectionMapping(int[][] maps, Map<AstNode,Integer> ids) {
            this.maps = maps;
            this.ids = ids;
        }
        public Map<AstNode,Integer> getAllocIds() { return ids; }
        public int getIndirectionMapId(AstNode node) {
            return ids.get(node);
        }
        public int[] getIndirectionMap(int id) {
            return maps[id];
        }
        public int[] getIndirectionMap(AstNode node) {
            return maps[ids.get(node)];
        }
        public int mapCount() { return maps.length; }
    }

}
