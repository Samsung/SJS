var placeHint = [0,0];
var placeListHint = [[0,0]];

function isContain(place, _array) {
//GRP1//    placeHint = place;
//GRP1//    placeListHint = _array;

    //var heat = _array || [];
    // var heat = _array ? _array : [];
    var heat = _array; // SATISH commented the above line
    var i=0;
    for (i=0; i < heat.length;i++){ // SATISH: eliminating for-in
        if (heat[i][0] == place[0] && heat[i][1] == place[1]) {
            return true;
        }
    }
    return false;
}

//GRP2// isContain(placeHint, placeListHint);