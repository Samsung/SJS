function Body(x){
   this.x = x; 
}
function NBodySystem(bodies3){ 
   this.bodies = bodies3;
   this.advance = function(dt){ 
                       var bodyi = this.bodies[0]; 
                    };
}
var bodies2 = new NBodySystem( [ new Body(4.4)  ]);