 function nBodySystem(){
   return { advance: function(dt){  
                       var bodyi = {  vx :   4.84143144246472090e+00 };  
                       bodyi.vx -= 1.0;
                    } 
   };
}

var bodies = nBodySystem(  );
(bodies.advance)(0.01); 