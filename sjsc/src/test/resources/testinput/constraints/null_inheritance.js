function Variable() {
}
function UnaryConstraint(v, strength, isEdit) {
}
function BinaryConstraint(strength) {
    this.strength = strength;
}
function Planner() {
}
Planner.prototype.constraint_satisfy = function (c, mark) {
    return null;
};
Planner.prototype.incrementalAdd = function (c) {
    var mark = this.newMark();
    var overridden = this.constraint_satisfy(c, mark);
    this.constraint_satisfy(overridden, mark);
};
function Deltablue() {
}
Deltablue.prototype.chainTest = function () {
    var planner = new Planner();
    this.planner = planner;
    var prev = null;
    {
        var v = new Variable();
        {
            var nullVar = null;
            var constraint = new BinaryConstraint(planner.REQUIRED);
            planner.incrementalAdd(constraint);
        }
    }
};
Deltablue.prototype.change = function (v) {
    var planner = this.planner;
    var editc = new UnaryConstraint(v, planner.PREFERRED, true);
    planner.incrementalAdd(editc);
};