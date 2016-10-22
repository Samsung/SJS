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
 * 
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;
import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.*;

public class Function extends Expression implements ILexicalScope {
    ArrayList<Var> argNames;
    ArrayList<Type> argTypes;
    Type returnType;
    String name;
    Block body;
    Scope scope;
    Set<String> captured;
    String envname;
    boolean isCtor;
    boolean isMethod;

    public Function(Scope enclosingScope, String name, Type ret) {
        super(Tag.Function);
        this.name = name;
        argTypes = new ArrayList<Type>();
        argNames = new ArrayList<Var>();
        this.returnType = ret;
        body = new Block();
        scope = new Scope(enclosingScope);
        captured = new HashSet<String>();
        envname = "__env"+name;
        scope.declareVariable(new Var(getEnvironmentName()), new EnvironmentType());
        isCtor = false;
        isMethod = false;
    }
    // This should only be called in transforming passes, in cases where a function is potentially renamed
    // but the environment name must be preserved
    public Function(Scope enclosingScope, String name, String envname, Type ret) {
        super(Tag.Function);
        this.name = name;
        argTypes = new ArrayList<Type>();
        argNames = new ArrayList<Var>();
        this.returnType = ret;
        body = new Block();
        scope = new Scope(enclosingScope);
        captured = new HashSet<String>();
        this.envname = envname;
        isCtor = false;
        isMethod = false;
    }
    public void setScope(Scope s) {
        assert (s.getParentScope() == scope.getParentScope());
        scope = s;
    }
    public boolean isMethod() { return isMethod; }
    public void markMethod() { isMethod = true; }
    public boolean isConstructor() { return isCtor; }
    public void markConstructor() { isCtor = true; }
    public Set<String> getCaptured() { return captured; }
    public void setCaptured(Set<String> vs) { captured = vs; }
    public void noteCapturedVariable(Var v) {
        captured.add(v.getIdentifier());
    }
    public int nargs() { return argNames.size(); }
    public Type getReturnType() { return returnType; }
    public Type argType(int i) { return argTypes.get(i); }
    public Var argName(int i) { return argNames.get(i); }
    public String getName() { return name; }
    public void addParameter(Var name, Type t) {
        assert(t != null);
        argNames.add(name);
        argTypes.add(t);
        scope.declareVariable(name, t);
    }
    public void addBodyStatement(Statement s) {
        if (s.declaresVariables()) {
            s.asDeclaration().declareInScope(this.scope);
        }
        body.addStatement(s);
    }
    public Block getBody() { return body; }
    public void setBody(Block b) { 
        body = new Block();
        for (Statement s : b) {
            addBodyStatement(s);
        }
    }
    @Override
    public String toSource(int x) {
        StringBuffer sb = new StringBuffer();
        sb.append("(<fun:"+name+">(");
        for (int i = 0; i < argTypes.size(); i++) {
            sb.append(argTypes.get(i).toString()+" "+argNames.get(i).toSource(0));
            if (i+1 < argTypes.size()) sb.append(", ");
        }
        sb.append(") : "+returnType.toString()+" => ");
        sb.append(body.toSource(x));
        return sb.toString();
    }
    public Scope getScope() { return scope; }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitFunction(this);
    }

    public String getEnvironmentName() {
        return envname;
    }
    private EnvironmentLayout envLayout;
    public void setLayout(EnvironmentLayout l) {
        envLayout = l;
    }
    public EnvironmentLayout getEnvLayout() {
        return envLayout;
    }
}
