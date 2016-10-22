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
 * Switch statement
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;

public class Switch extends Statement {
    Expression discriminee;
    List<Case> cases;
    public Switch(Expression disc) {
        super(Tag.Switch);
        discriminee = disc;
        cases = new LinkedList<>();
    }

    public Expression getDiscriminee() { return discriminee; }
    public void addCase(Case c) { cases.add(c); }
    public List<Case> getCases() { return cases; }

    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        indent(x, sb);
        sb.append("switch ("+discriminee.toSource(0)+") {\n");
        for (Case c : cases) {
            sb.append(c.toSource(x+2));
        }
        sb.append("\n");
        indent(x, sb);
        sb.append("}");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitSwitch(this);
    }
}
