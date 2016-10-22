var PI = 3.141592653589793;
var SOLAR_MASS = 4 * PI * PI;
var DAYS_PER_YEAR = 365.24;

function body(x,y,z,vx,vy,vz,mass){
  return {
   x : x,
   y : y,
   z : z,
   vx : vx,
   vy : vy,
   vz : vz,
   mass : mass,

   offsetMomentum : function(px,py,pz) {
       this.vx = -px / SOLAR_MASS;
       this.vy = -py / SOLAR_MASS;
       this.vz = -pz / SOLAR_MASS;
   }
  };
}

var jupiter = body(
      4.84143144246472090e+00,
      -1.16032004402742839e+00,
      -1.03622044471123109e-01,
      1.66007664274403694e-03 * DAYS_PER_YEAR,
      7.69901118419740425e-03 * DAYS_PER_YEAR,
      -6.90460016972063023e-05 * DAYS_PER_YEAR,
      9.54791938424326609e-04 * SOLAR_MASS
   );
jupiter.offsetMomentum(0.0,0.0,0.0);