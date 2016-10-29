// Here the error is on line 7: we should not assign a number into a string
// field.

var w = {
    playerNum: "hi",
    init: function _init() {
        this.playerNum = 1;
    }
};
