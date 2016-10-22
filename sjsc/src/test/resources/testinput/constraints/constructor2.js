function C(x, y, z){
  this.x = x;
  this.y = y;
  this.z = z; 
  this.getX = function f1(){ return this.x; }
  this.setXY = function f2(x, y){ this.x = x; this.y = y; } 
}