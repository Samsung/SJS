// Copyright 2008 the V8 project authors. All rights reserved.
// Copyright 1996 John Maloney and Mario Wolczko.

// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


// This implementation of the DeltaBlue benchmark is derived
// from the Smalltalk implementation by John Maloney and Mario
// Wolczko. Some parts have been translated directly, whereas
// others have been modified more aggressively to make it feel
// more like a JavaScript program.

// This typescript implementation of the DeltaBlue benchmark was
// written by Shams Imam at Rice University


/**
 * A JavaScript implementation of the DeltaBlue constraint-solving algorithm, as described in:
 *
 * "The DeltaBlue Algorithm: An Incremental Constraint Hierarchy Solver"
 *   Bjorn N. Freeman-Benson and John Maloney
 *   January 1990 Communications of the ACM,
 *   also available as University of Washington TR 89-08-06.
 *
 * Beware: this benchmark is written in a grotesque style where the constraint model is built by side-effects from constructors.
 * I've kept it this way to avoid deviating too much from the original implementation.
 */

// --- 
// S t r e n g t h
// --- 

//
// Strengths are used to measure the relative importance of constraints.
// New strengths may be inserted in the strength hierarchy without
// disrupting current constraints.  Strengths cannot be created outside
// this class, so pointer comparison can be used for value comparison.
//
function Strength(pStrengthValue) {
  this.strengthValue = pStrengthValue;
}

// --- 
// C o n s t r a i n t
// --- 

//
// An abstract class representing a system-maintainable relationship
// (or "constraint") between a set of variables. A constraint supplies
// a strength instance variable; concrete subclasses provide a means
// of storing the constrained variables and other information required
// to represent a constraint.
//
function Constraint() {
  this.strength = undefined;
}

Constraint.prototype.stronger = function(s1, s2) {
  if (s1.strengthValue < s2.strengthValue)
    return true;
  return false;
};

Constraint.prototype.weaker = function(s1, s2) {
  if (s1.strengthValue > s2.strengthValue)
    return true;
  return false;
};

Constraint.prototype.weakestOf = function(s1, s2) {
  if (this.weaker(s1, s2))
    return s1;
  return s2;
};

Constraint.prototype.execute = function() { 
  console.log("ALERT: 101");
};

Constraint.prototype.chooseMethod = function(mark) {
  // throw(102); 
};

Constraint.prototype.markInputs = function(mark) {
  // throw(103); 
};

Constraint.prototype.addToGraph = function() {
  // throw(104); 
};

Constraint.prototype.isSatisfied = function() {
  // throw(105); 
  return false;
};

Constraint.prototype.markUnsatisfied = function() {
 // throw(106); 
};

Constraint.prototype.recalculate = function() {
 // throw(107); 
};

Constraint.prototype.output = function() {
  // throw(108); 
  return null;
};

Constraint.prototype.removeFromGraph = function() {
  // throw(109); 
};

//
// Normal constraints are not input constraints.  An input constraint
// is one that depends on external state, such as the mouse, the
// keybord, a clock, or some arbitrary piece of imperative code.
//
Constraint.prototype.isInput = function() {
  return false;
};

Constraint.prototype.inputsKnown = function(mark) {
  // throw(110); 
  return false;
};

function OrderedCollectionConstraint() {
  this.elms = [];
}

OrderedCollectionConstraint.prototype.add = function(elm) {
  var elms = this.elms;
  elms.push(elm);
};

OrderedCollectionConstraint.prototype.at = function(index) {
  var elms = this.elms;
  return elms[index];
};

OrderedCollectionConstraint.prototype.size = function() {
  var elms = this.elms;
  return elms.length;
};

OrderedCollectionConstraint.prototype.removeFirst = function() {
  var elms = this.elms;
  return elms.pop();
};

OrderedCollectionConstraint.prototype.remove = function(elm) {
  var index = 0;
  var skipped = 0;
  var elms = this.elms;
  for (var i1 = 0; i1 < elms.length; i1 ++) {
    var value = elms[i1];
    if (value !== elm) {
      elms[index] = value;
      index ++;
    } else {
      skipped ++;
    }
  }
  for (var i2 = 0; i2 < skipped; i2 ++)
    elms.pop();
};

// --- 
// V a r i a b l e 
// --- 

// 
// A constrained variable. In addition to its value, it maintain the  
// structure of the constraint graph, the current dataflow graph, and 
// various parameters of interest to the DeltaBlue incremental 
// constraint solver.  
// 
function Variable(initialValue, strength) {
  this.value = initialValue;
  this.constraints = new OrderedCollectionConstraint();
  this.determinedBy = null;
  this.mark = 0;
  this.walkStrength = strength; // Strength.WEAKEST;  
  this.stay = true;
}

//  
// Add the given constraint to the set of all constraints that refer  
// this variable.   
//   
Variable.prototype.addConstraint = function(c) {
  var constraints = this.constraints;
  constraints.add(c);
};

//  
// Removes all traces of c from this variable.       
///    
Variable.prototype.removeConstraint = function(c) {
  var constraints = this.constraints;
  constraints.remove(c);
  if (this.determinedBy === c)
    this.determinedBy = null;
};

// --- 
// U n a r y   C o n s t r a i n t
// --- 

//
// Abstract superclass for constraints having a single possible output variable.
//
function UnaryConstraintProto() {
  //
  // Adds this constraint to the constraint graph
  //
  this.addToGraph = function() {
    var output = this.myOutput;
    output.addConstraint(this);
    this.satisfied = false;
  };
  
  //
  // Decides if this constraint can be satisfied and records that decision.
  //
  this.chooseMethod = function(mark) {
    var res = false;
       
    var myOutput = this.myOutput;
    if (myOutput.mark !== mark)
      if (this.stronger(this.strength, myOutput.walkStrength))
      this.satisfied = true;
  };
  
  //
  // Returns true if this constraint is satisfied in the current solution.
  //
  this.isSatisfied = function() {
    return this.satisfied;
  };

  this.markInputs = function(mark) {
    // has no inputs
  };

  //
  // Returns the current output variable.
  //
  this.output = function() {
    return this.myOutput;
  };
  
  this.isInput = function() {
    return this.isEdit;
  };

  this.execute = function() {
  };

  //
  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this constraint. Assume
  // this constraint is satisfied.
  this.recalculate = function() {
    var myOutput = this.myOutput;
    myOutput.walkStrength = this.strength;
    if (this.isInput())
      myOutput.stay = false
    else 
      myOutput.stay = true;
    if (myOutput.stay) 
      this.execute(); // Stay optimization
  };
  
  //
  // Records that this constraint is unsatisfied
  //
  this.markUnsatisfied = function() {
    this.satisfied = false;
  };

  this.inputsKnown = function(mark) {
    return true;
  };
  
  this.removeFromGraph = function() {
    var myOutput = this.myOutput;
    if (myOutput != null) 
      myOutput.removeConstraint(this);
    this.satisfied = false;
  };
}
UnaryConstraintProto.prototype = new Constraint();

function UnaryConstraint(v, strength, isEdit) {
  //this.initialize(strength);
  this.strength = strength;
  this.myOutput = v;
  this.satisfied = false;
  this.isEdit = isEdit;
}

UnaryConstraint.prototype = new UnaryConstraintProto();

//class Direction {
//  static NONE   = 0;
//  static FORWARD  = 1;
//  static BACKWARD = -1;
//}

// --- 
// B i n a r y   C o n s t r a i n t
// --- 

//
// Abstract superclass for constraints having two possible output
// variables.
//
function BinaryConstraintProto() {
  //
  // Decides if this constraint can be satisfied and which way it
  // should flow based on the relative strength of the variables related,
  // and record that decision.
  //
  this.chooseMethod = function(mark) {
    var v1 = this.v1;
    var v2 = this.v2;
    var flag = false;
    if (v1.mark === mark) {
      if (v2.mark !== mark)
      if (this.stronger(this.strength, v2.walkStrength))
        flag = true;
      if (flag)
      this.direction = 1; // Direction.FORWARD
      else 
      this.direction = 0 - 1; // : Direction.NONE;
    }
    if (v2.mark === mark) {
      if (v1.mark !== mark) 
      if (this.stronger(this.strength, v1.walkStrength))
        flag = true;
      if (flag)
      this.direction = 0 - 1; // Direction.BACKWARD
      else 
      this.direction = 0; //  Direction.NONE;
    }
    if (this.weaker(v1.walkStrength, v2.walkStrength)) {
      if (this.stronger(this.strength, v1.walkStrength))
        this.direction = 0 - 1; // Direction.BACKWARD
      else
        this.direction = 0; // Direction.NONE;
    } else {
      if (this.stronger(this.strength, v2.walkStrength))
      this.direction = 1; // Direction.FORWARD
      else
      this.direction = 0 - 1; // Direction.BACKWARD
    }
  };
  
  //
  // Add this constraint to the constraint graph
  //
  this.addToGraph = function() {
    var v1 = this.v1;
    v1.addConstraint(this);
    var v2 = this.v2;
    v2.addConstraint(this);
    this.direction = 0; // Direction.NONE;
    if (this.isScale) {
      var scale = this.scale;
      scale.addConstraint(this);
      var offset = this.offset;
      offset.addConstraint(this);
    }
  };

  //
  // Answer true if this constraint is satisfied in the current solution.
  //
  this.isSatisfied = function() {
    if (this.direction !== 0) // Direction.NONE;
      return true;
    return false;
  };
  
  //
  // Returns the current input variable 
  //
  this.input = function() {
    if (this.direction === 1) // Direction.FORWARD)  
      return this.v1;

    return this.v2;
  };

  //
  // Mark the input variable with the given mark.
  //
  this.markInputs = function(mark) {
    var input  = this.input();
    input.mark = mark;
    if (this.isScale) {
      var scale = this.scale;
      var offset = this.offset;
      scale.mark = mark;
      offset.mark = mark;
    }
  };
   
  //
  // Returns the current output variable
  //
  this.output = function() {
    if (this.direction === 1)// Direction.FORWARD)
      return this.v2;

    return this.v1;
  };
   
  this.execute = function() {
    if (this.isScale) {
      var v1 = this.v1;
      var v2 = this.v2;
      var scale = this.scale;
      var offset = this.offset;
      if (this.direction === 1) { // Direction.FORWARD) {
      v2.value = v1.value * scale.value + offset.value;
      } else {
      v1.value = (v2.value - offset.value) / scale.value;
      }
    } else {
      var input  = this.input();
      var output   = this.output();
      output.value = input.value;
    }
  };
  
  //
  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this
  // constraint. Assume this constraint is satisfied.
  //
  this.recalculate = function() {
    if (this.isScale) {
      var ihn  = this.input();
      var out  = this.output();
      out.walkStrength = this.weakestOf(this.strength, ihn.walkStrength);
      var scale = this.scale;
      var offset = this.offset;
      if (ihn.stay)
      if (scale.stay)
        if (offset.stay)
        out.stay = true;
      // out.stay = ihn.stay && this.scale.stay && offset.stay;
      if (out.stay) 
      this.execute();
    } else {
      var ihn_  = this.input();
      var out_  = this.output();
      out_.walkStrength = this.weakestOf(this.strength, ihn_.walkStrength);
      out_.stay = ihn_.stay;
      if (out_.stay) 
      this.execute();
    }
  };
  
  //
  // Record the fact that this constraint is unsatisfied.
  //
  this.markUnsatisfied = function() {
    this.direction = 0; // Direction.NONE;
  };
  
  this.inputsKnown = function(mark) {
    var i  = this.input();
    if (i.mark === mark)
      return true;
    if (i.stay)
      return true;
    if (i.determinedBy === null)
      return true;
    
    return false;
  };
  
  this.removeFromGraph = function() {
    var v1 = this.v1;
    if (v1 !== null) 
      v1.removeConstraint(this);
    var v2 = this.v2;
    if (v2 !== null) 
      v2.removeConstraint(this);
    this.direction = 0; // Direction.NONE;
    
    if (this.isScale) {
      var scale = this.scale;
      if (scale !== null)
      scale.removeConstraint(this);
      var offset = this.offset;
      if (offset !== null) 
      offset.removeConstraint(this);
    }
  };
}

BinaryConstraintProto.prototype = new Constraint();

function BinaryConstraint(src, scale, offset, dest, strength) {
  //this.initialize(strength);
  this.strength = strength;
  this.v1 = src;
  this.v2 = dest;
  this.direction = 0; // Direction.NONE;
  // this.addConstraint();
  
  this.scale = scale;
  this.offset = offset;
  if (this.scale === null)
    this.isScale = false;
  else 
    this.isScale = true;
}

BinaryConstraint.prototype = new BinaryConstraintProto();

function OrderedCollectionVariable() {
  this.elms = [];
}

OrderedCollectionVariable.prototype.add = function(elm) {
  var elms = this.elms;
  elms.push(elm);
};

OrderedCollectionVariable.prototype.at = function(index) {
  var elms = this.elms;
  return elms[index];
};

OrderedCollectionVariable.prototype.size = function() {
  var elms = this.elms;
  return elms.length;
};

OrderedCollectionVariable.prototype.removeFirst = function() {
  var elms = this.elms;
  return elms.pop();
};

OrderedCollectionVariable.prototype.remove = function(elm) {
  var index = 0;
  var skipped = 0;
  var elms = this.elms;
  for (var i1 = 0; i1 < elms.length; i1 ++) {
    var value = elms[i1];
    if (value !== elm) {
      elms[index] = value;
      index ++;
    } else {
      skipped ++;
    }
  }
  for (var i2 = 0; i2 < skipped; i2 ++)
    elms.pop();
};

// --- 
// P l a n 
// --- 

// 
// A Plan is an ordered list of constraints to be executed in sequence 
// to re-satisfy all currently satisfiable constraints in the face of 
// one or more changing inputs. 
//
function Plan() {
  this.v = new OrderedCollectionConstraint();
}

Plan.prototype.addConstraint = function(c) {
  var v = this.v;
  v.add(c);
};

Plan.prototype.size = function() {
  var v = this.v;
  return v.size();
};

Plan.prototype.constraintAt = function(index) {
  var v = this.v;
  return v.at(index);
};

Plan.prototype.execute = function() {
  for (var i = 0; i < this.size(); i++) {
    var c = this.constraintAt(i);
    c.execute();
  }
};

// --- 
// P l a n n e r
// --- 

//
// The DeltaBlue planner
//
function Planner() {
  this.currentMark = 0;
  // Strength constants.   
  this.REQUIRED         = new Strength(0);
  this.STONG_PREFERRED  = new Strength(1); 
  this.PREFERRED        = new Strength(2); 
  this.STRONG_DEFAULT   = new Strength(3); 
  this.NORMAL           = new Strength(4); 
  this.WEAK_DEFAULT     = new Strength(5); 
  this.WEAKEST          = new Strength(6);
  this.NOTHING          = new Strength(99);
}

Planner.prototype.nextWeaker = function(s) {
  if (s.strengthValue === 0)
    return this.WEAKEST;
  else if (s.strengthValue === 1)
    return this.WEAK_DEFAULT;
  else if (s.strengthValue === 2)
    return this.NORMAL;
  else if (s.strengthValue === 3)
    return this.STRONG_DEFAULT;
  else if (s.strengthValue === 4)
    return this.PREFERRED;
  else if (s.strengthValue === 5)
    return this.REQUIRED;

  return this.NOTHING;
};

Planner.prototype.constraint_satisfy = function(c , mark) {
  c.chooseMethod(mark);
     
  if (c.isSatisfied()) {
  } else{
    if (c.strength === this.REQUIRED)
      console.log("ALERT: Could not satisfy a required constraint!");
    return null;
  }
  
  c.markInputs(mark);
  var out  = c.output();
  var overridden = out.determinedBy;
     
  if (overridden !== null) overridden.markUnsatisfied();
  out.determinedBy = c;
     
  if (this.addPropagate(c, mark)) {
  } else {
    console.log("ALERT: Cycle encountered");
  }
  
  out.mark = mark;

  return overridden;
};

//
// Attempt to satisfy the given constraint and, if successful,
// incrementally update the dataflow graph.  Details: If satisfying
// the constraint is successful, it may override a weaker constraint
// on its output. The algorithm attempts to re-satisfy that
// constraint using some other method. This process is repeated
// until either a) it reaches a variable that was not previously
// determined by any constraint or b) it reaches a constraint that
// is too weak to be satisfied using any of its methods. The
// variables of constraints that have been processed are marked with
// a unique mark value so that we know where we've been. This allows
// the algorithm to avoid getting into an infinite loop even if the
// constraint graph has an inadvertent cycle.
//
Planner.prototype.incrementalAdd = function(c) {
  var mark = this.newMark();
  var overridden = this.constraint_satisfy(c, mark);
  while (overridden !== null)
    overridden = this.constraint_satisfy(overridden, mark);
};

//
// Entry point for retracting a constraint. Remove the given
// constraint and incrementally update the dataflow graph.
// Details: Retracting the given constraint may allow some currently
// unsatisfiable downstream constraint to be satisfied. We therefore collect
// a list of unsatisfied downstream constraints and attempt to
// satisfy each one in turn. This list is traversed by constraint
// strength, strongest first, as a heuristic for avoiding
// unnecessarily adding and then overriding weak constraints.
// Assume: c is satisfied.
//
Planner.prototype.incrementalRemove = function(c) {
  var out  = c.output();
  c.markUnsatisfied();
  c.removeFromGraph();
  var unsatisfied = this.removePropagateFrom(out);
  var strength = this.REQUIRED;
  while (strength !== this.WEAKEST) {
    for (var i = 0; i < unsatisfied.size(); i++) {
    var u = unsatisfied.at(i);
    if (u.strength === strength)
      this.incrementalAdd(u);
    }
    strength = this.nextWeaker(strength);
  }
};

//
// Select a previously unused mark value.
//
Planner.prototype.newMark = function() {
  this.currentMark = this.currentMark + 1;
  return this.currentMark;
};

//
// Extract a plan for re-satisfaction starting from the given source
// constraints, usually a set of input constraints. This method
// assumes that stay optimization is desired; the plan will contain
// only constraints whose output variables are not stay. Constraints
// that do no computation, such as stay and edit constraints, are
// not included in the plan.
// Details: The outputs of a constraint are marked when it is added
// to the plan under construction. A constraint may be appended to
// the plan when all its input variables are known. A variable is
// known if either a) the variable is marked (indicating that has
// been computed by a constraint appearing earlier in the plan), b)
// the variable is 'stay' (i.e. it is a constant at plan execution
// time), or c) the variable is not determined by any
// constraint. The last provision is for past states of history
// variables, which are not stay but which are also not computed by
// any constraint.
// Assume: sources are all satisfied.
//
Planner.prototype.makePlan = function(sources) {
  var mark = this.newMark();
  var plan = new Plan();
  var todoList = sources;
  while (todoList.size() > 0) {
  var c = todoList.removeFirst();
  var out  = c.output();
  if (out.mark !== mark)
    if (c.inputsKnown(mark)) {
    plan.addConstraint(c);
    out.mark = mark;
    this.addConstraintsConsumingTo(out, todoList);
    }
  }
  return plan;
};

//
// Extract a plan for resatisfying starting from the output of the
// given constraints, usually a set of input constraints.
//
Planner.prototype.extractPlanFromConstraints = function(constraints) {
  var sources = new OrderedCollectionConstraint();
  // var constraints = this.constraints;
  for (var i = 0; i < constraints.size(); i++) {
    var c = constraints.at(i);
    if (c.isInput())
    if (c.isSatisfied())
      // not in plan already and eligible for inclusion
      sources.add(c);
  }
  var plan = this.makePlan(sources);
  return plan;
};

//
// Recompute the walkabout strengths and stay flags of all variables
// downstream of the given constraint and recompute the actual
// values of all variables whose stay flag is true. If a cycle is
// detected, remove the given constraint and answer
// false. Otherwise, answer true.
// Details: Cycles are detected when a marked variable is
// encountered downstream of the given constraint. The sender is
// assumed to have marked the inputs of the given constraint with
// the given mark. Thus, encountering a marked node downstream of
// the output constraint means that there is a path from the
// constraint's output to one of its inputs.
//
Planner.prototype.addPropagate = function(c, mark) {
  var todoList = new OrderedCollectionConstraint();
  todoList.add(c);
  while (todoList.size() > 0) {
  var d = todoList.removeFirst();
  var out  = d.output();
  
  if (out.mark === mark) {
    this.incrementalRemove(c);
    return false;
  }
  d.recalculate();
  this.addConstraintsConsumingTo(out, todoList);
  }
  return true;
};

//
// Update the walkabout strengths and stay flags of all variables
// downstream of the given constraint. Answer a collection of
// unsatisfied constraints sorted in order of decreasing strength.
//
Planner.prototype.removePropagateFrom = function(out) {
  out.determinedBy = null;
  out.walkStrength = this.WEAKEST;
  out.stay = true;
  var unsatisfied = new OrderedCollectionConstraint();
  var todoList = new OrderedCollectionVariable();
  todoList.add(out);
  while (todoList.size() > 0) {
    var v = todoList.removeFirst();
    var v_constraints = v.constraints;
    for (var i1 = 0; i1 < v_constraints.size(); i1 ++) {
    var c = v_constraints.at(i1);
    if (c.isSatisfied()) {
    } else
      unsatisfied.add(c);
    }
    var determining = v.determinedBy;
    for (var i2 = 0; i2 < v_constraints.size(); i2 ++) {
    var next = v_constraints.at(i2);
    if (next !== determining)
      if (next.isSatisfied()) {
      next.recalculate();
      var out_  = next.output();
      todoList.add(out_);
      }
    }
  }
  return unsatisfied;
};

Planner.prototype.addConstraintsConsumingTo = function(v, coll) {
  var determining = v.determinedBy;
  var cc = v.constraints;
  for (var i = 0; i < cc.size(); i++) {
    var c = cc.at(i);
    if (c !== determining)
    if (c.isSatisfied())
      coll.add(c);
  }
};

// --- *
// M a i n
// --- *

//
// This is the standard DeltaBlue benchmark. A long chain of equality
// constraints is constructed with a stay constraint on one end. An
// edit constraint is then added to the opposite end and the time is
// measured for adding and removing this constraint, and extracting
// and executing a constraint satisfaction plan. There are two cases.
// In case 1, the added constraint is stronger than the stay
// constraint and values must propagate down the entire length of the
// chain. In case 2, the added constraint is weaker than the stay
// constraint so it cannot be accommodated. The cost in this case is,
// of course, very low. Typical situations lie somewhere between these
// two extremes.
//
function Deltablue() {
  this.planner = undefined;
}

Deltablue.prototype.chainTest = function(n) {
  var planner = new Planner();
  this.planner = planner;
  var prev = null;
  var first = null;
  var last = null;
  
  // Build chain of n equality constraints
  for (var i1 = 0; i1 <= n; i1 ++) {
    // var name = "v" + i;
    var v = new Variable(// name, 
         0, planner.WEAKEST);
    
    if (prev !== null) {
    var nullVar  = null;
    var constraint = new BinaryConstraint(prev, nullVar, nullVar, v, planner.REQUIRED); // EqualityConstriant
    constraint.addToGraph();
    planner.incrementalAdd(constraint);
    }
    if (i1 === 0) first = v;
    if (i1 === n) last = v;
    prev = v;
  }
  
  var safec = new UnaryConstraint(last, planner.STRONG_DEFAULT, false); // StayConstraint
  safec.addToGraph();
  planner.incrementalAdd(safec);
  var editc = new UnaryConstraint(first, planner.PREFERRED, true); // EditConstraint
  editc.addToGraph();
  planner.incrementalAdd(editc);
  var edits = new OrderedCollectionConstraint();
  edits.add(editc);
  var plan = planner.extractPlanFromConstraints(edits);
  for (var i2 = 0; i2 < 100; i2 ++) {
    first.value = i2;
    plan.execute();
   
    if (last.value !== i2) console.log("ALERT: Chain test failed.");
  }
};

Deltablue.prototype.change = function(v, newValue) {
  var planner = this.planner;
  var editc = new UnaryConstraint(v, planner.PREFERRED, true); // EditConstraint   
  editc.addToGraph();
  planner.incrementalAdd(editc);
  var edits = new OrderedCollectionConstraint();
  edits.add(editc);
  var plan = planner.extractPlanFromConstraints(edits);
  for (var i = 0; i < 10; i++) {
    v.value = newValue;
    plan.execute();
  }
  // editc.destroyConstraint();
  if (editc.isSatisfied())
    planner.incrementalRemove(editc);
  else
    editc.removeFromGraph();
};

//
// This test constructs a two sets of variables related to each
// other by a simple linear transformation (scale and offset). The
// time is measured to change a variable on either side of the
// mapping and to change the scale and offset factors.
//
Deltablue.prototype.projectionTest = function(n) {
  var planner = new Planner();
  this.planner = planner;
  var scale = new Variable(// "scale", 
         10, planner.WEAKEST);
  var offset = new Variable(// "offset", 
          1000, planner.WEAKEST);
  var src = null;
  var dst = null;
  
  var dests = new OrderedCollectionVariable();
  for (var i1 = 0; i1 < n; i1 ++) {
    src = new Variable(// "src" + i, 
           i1, planner.WEAKEST);
    dst = new Variable(// "dst" + i, 
           i1, planner.WEAKEST);
    dests.add(dst);
    var uryc = new UnaryConstraint(src, planner.NORMAL, false); // StayConstraint
    uryc.addToGraph();
    planner.incrementalAdd(uryc);
    var binc = new BinaryConstraint(src, scale, offset, dst, planner.REQUIRED); // ScaleConstraint
    binc.addToGraph();
    planner.incrementalAdd(binc);
  }
  
  this.change(src, 17);
  if (dst.value != 1170) console.log("ALERT: Projection 1 failed");
  this.change(dst, 1050);
  if (src.value != 5) console.log("ALERT: Projection 2 failed");
  this.change(scale, 5);
  for (var i = 0; i < n - 1; i++) {
    if (dests.at(i).value != i * 5 + 1000)
      console.log("ALERT: Projection 3 failed");
  }
  this.change(offset, 2000);
  for (var i = 0; i < n - 1; i++) {
    if (dests.at(i).value != i * 5 + 2000)
      console.log("ALERT: Projection 4 failed");
  }
};

Deltablue.prototype.deltaBlue = function() {
  this.chainTest(100);
  this.projectionTest(100);
};

// Invoke the method to execute the benchmark
var engine = new Deltablue();
engine.deltaBlue();
console.log("ALERT: End");
//throw(333); // terminates and reports dynamically allocated objects
