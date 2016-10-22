function OrderedCollectionConstraint() {
    this.elms = [];
}
OrderedCollectionConstraint.prototype.add = function (elm) {
    var elms = this.elms;
    elms.push(elm);
};
OrderedCollectionConstraint.prototype.at = function (index) {
    var elms = this.elms;
    return elms[index];
};
function Variable() {
}
function UnaryConstraint(v, strength, isEdit) {
    this.strength = strength;
}
function BinaryConstraint(strength) {
    this.strength = strength;
}
function Planner() {
}
Planner.prototype.constraint_satisfy = function (c, mark) {
    (this.addPropagate(c, mark))
};
Planner.prototype.incrementalAdd = function (c) {
    var mark = this.newMark();
    var overridden = this.constraint_satisfy(c, mark);
};
Planner.prototype.incrementalRemove = function (c) {
    var out = c.output();
    var unsatisfied = this.removePropagateFrom(out);
    for (var i = 0;;) {
        var u = unsatisfied.at(i);
        (u.strength)
    }
};
Planner.prototype.addPropagate = function (c, mark) {
    var todoList = new OrderedCollectionConstraint();
    todoList.add(c);
};
Planner.prototype.removePropagateFrom = function (out) {
    var unsatisfied = new OrderedCollectionConstraint();
    return unsatisfied;
};
(function () {
    var planner = new Planner();
    var prev = null;
    var last = null;
    {
        var v = new Variable();
        {
            var nullVar = null;
            var constraint = new BinaryConstraint(planner.REQUIRED);
            planner.incrementalAdd(constraint);
        }
    }
    var safec = new UnaryConstraint(last, planner.STRONG_DEFAULT, false);
    planner.incrementalAdd(safec);
});