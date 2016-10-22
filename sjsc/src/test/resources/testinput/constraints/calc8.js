function CalcActionEventListener() {
    this.OnActionPerformed = function () {
        var displayText = this.calcForm.calcModel.OnActionPerformed();
    };
}
CalcActionEventListener.prototype = new __IActionEventListener();