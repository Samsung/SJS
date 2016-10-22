 
function mkState() {
    return {
        MODE_INVALID : 104, 
        calcMode : 100, 

        UpdateOperand : function (k) {
            return false;
        },

        HandleNumKeys : function (k) { 
            switch (this.calcMode) {
            case this.MODE_INVALID:
                return true;    
            }
            return this.UpdateOperand(k);
        } 
    }
}

//mkState().HandleNumKeys(3);
