// The ray tracer code in this file is written by Adam Burmister. It
// is available in its original form from:
//
//   http://labs.flog.nz.co/raytracer/
//
// It has been modified slightly by Google to work as a standalone
// benchmark, but the all the computational code remains
// untouched. This file also contains a copy of parts of the Prototype
// JavaScript framework which is used by the ray tracer.

//var RayTrace = new BenchmarkSuite('RayTrace', [739989], [
//  new Benchmark('RayTrace', true, false, 600, renderScene)
//]);


// Variable used to hold a number that can be used to verify that
// the scene was ray traced correctly.
var checkNumber = 0;// SJS


// ------------------------------------------------------------------------
// ------------------------------------------------------------------------

// The following is a copy of parts of the Prototype JavaScript library:

// Prototype JavaScript framework, version 1.5.0
// (c) 2005-2007 Sam Stephenson
//
// Prototype is freely distributable under the terms of an MIT-style license.
// For details, see the Prototype web site: http://prototype.conio.net/

// SJS cannot handle these
//var Class = {
//  create: function() {
//    return function() {
//      this.initialize.apply(this, arguments);
//    }
//  }
//};
//
//
//Object.extend = function(destination, source) {
//  for (var property in source) {
//    destination[property] = source[property];
//  }
//  return destination;
//};


// ------------------------------------------------------------------------
// ------------------------------------------------------------------------

// The rest of this file is the actual ray tracer written by Adam
// Burmister. It's a concatenation of the following files:
//
//   flog/color.js
//   flog/light.js
//   flog/vector.js
//   flog/ray.js
//   flog/scene.js
//   flog/material/basematerial.js
//   flog/material/solid.js
//   flog/material/chessboard.js
//   flog/shape/baseshape.js
//   flog/shape/sphere.js
//   flog/shape/plane.js
//   flog/intersectioninfo.js
//   flog/camera.js
//   flog/background.js
//   flog/engine.js


/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

var Flog = {
    RayTracer : {
                    Color: Color,
                    Light: Light,
                    Vector: Vector,
                    Ray: Ray,
                    Scene: Scene,
                    Material: {
                        BaseMaterial : BaseMaterial,
                        Solid: Solid,
                        Chessboard: Chessboard
                    },
                    Shape: {
                        Sphere: Sphere,
                        Plane: Plane
                    },
                    IntersectionInfo: IntersectionInfo,
                    Camera: Camera,
                    Background: Background,
                    Engine: Engine
                }
};

function Color(r,g,b) {
    this.red = undefined;
    this.green = undefined;
    this.blue = undefined;
    this.initialize(r,g,b);
}

Color.prototype = {
    red : 0.0,
    green : 0.0,
    blue : 0.0,

    initialize : function(r, g, b) {
//        if(!r) r = 0.0; SJS limitation on floats
//        if(!g) g = 0.0;
//        if(!b) b = 0.0;

        this.red = r;
        this.green = g;
        this.blue = b;
    },

    add : function(c1, c2){
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red + c2.red;
        result.green = c1.green + c2.green;
        result.blue = c1.blue + c2.blue;

        return result;
    },

    addScalar: function(c1, s){
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red + s;
        result.green = c1.green + s;
        result.blue = c1.blue + s;

        result.limit();

        return result;
    },

    subtract: function(c1, c2){
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red - c2.red;
        result.green = c1.green - c2.green;
        result.blue = c1.blue - c2.blue;

        return result;
    },

    multiply : function(c1, c2) {
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red * c2.red;
        result.green = c1.green * c2.green;
        result.blue = c1.blue * c2.blue;

        return result;
    },

    multiplyScalar : function(c1, f) {
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red * f;
        result.green = c1.green * f;
        result.blue = c1.blue * f;

        return result;
    },

    divideFactor : function(c1, f) {
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);

        result.red = c1.red / f;
        result.green = c1.green / f;
        result.blue = c1.blue / f;

        return result;
    },

    limit: function(){
        this.red = (this.red > 0.0) ? ( (this.red > 1.0) ? 1.0 : this.red ) : 0.0;
        this.green = (this.green > 0.0) ? ( (this.green > 1.0) ? 1.0 : this.green ) : 0.0;
        this.blue = (this.blue > 0.0) ? ( (this.blue > 1.0) ? 1.0 : this.blue ) : 0.0;
    },

    distance : function(color) {
        var d = Math.abs(this.red - color.red) + Math.abs(this.green - color.green) + Math.abs(this.blue - color.blue);
        return d;
    },

    blend: function(c1, c2, w){
        var result = new Flog.RayTracer.Color(0.0,0.0,0.0);
        result = Flog.RayTracer.Color.prototype.add(
                    Flog.RayTracer.Color.prototype.multiplyScalar(c1, 1 - w),
                    Flog.RayTracer.Color.prototype.multiplyScalar(c2, w)
                  );
        return result;
    },

    brightness : function() {
        var r = Math.floor(this.red*255);
        var g = Math.floor(this.green*255);
        var b = Math.floor(this.blue*255);
        return (r * 77 + g * 150 + b * 29) >> 8;
    },

    toString : function () {
        var r = Math.floor(this.red*255);
        var g = Math.floor(this.green*255);
        var b = Math.floor(this.blue*255);

        return "rgb("+ r +","+ g +","+ b +")";
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Light(pos, color, intensity) {
    this.position = undefined;
    this.color = undefined;
    this.intensity = undefined;
    this.initialize(pos, color, intensity);
}

Light.prototype = {
    position: null,
    color: null,
    intensity: 10.0,

    initialize : function(pos, color, intensity) {
        this.position = pos;
        this.color = color;
        this.intensity = (intensity ? intensity : 10.0);
    },

    toString : function () {
        return 'Light [' + this.position.x + ',' + this.position.y + ',' + this.position.z + ']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Vector(x,y,z) {
    this.x = undefined;
    this.y = undefined;
    this.z = undefined;
    this.initialize(x,y,z);
}

Vector.prototype = {
    x : 0.0,
    y : 0.0,
    z : 0.0,

    initialize : function(x, y, z) {
        this.x = x; //(x ? x : 0.0); SJS limitaiton on floats
        this.y = y; //(y ? y : 0.0);
        this.z = z; //(z ? z : 0.0);
    },

    copy: function(vector){
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    },

    normalize : function() {
        var m = this.magnitude();
        return new Flog.RayTracer.Vector(this.x / m, this.y / m, this.z / m);
    },

    magnitude : function() {
        return Math.sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
    },

    cross : function(w) {
        return new Flog.RayTracer.Vector(
                                            -this.z * w.y + this.y * w.z,
                                           this.z * w.x - this.x * w.z,
                                          -this.y * w.x + this.x * w.y);
    },

    dot : function(w) {
        return this.x * w.x + this.y * w.y + this.z * w.z;
    },

    add : function(v, w) {
        return new Flog.RayTracer.Vector(w.x + v.x, w.y + v.y, w.z + v.z);
    },

    subtract : function(v, w) {
        if(!w || !v) console.error ( 'Vectors must be defined [' + v.toString() + ',' + w.toString() + ']' );
        return new Flog.RayTracer.Vector(v.x - w.x, v.y - w.y, v.z - w.z);
    },

    multiplyVector : function(v, w) {
        return new Flog.RayTracer.Vector(v.x * w.x, v.y * w.y, v.z * w.z);
    },

    multiplyScalar : function(v, w) {
        return new Flog.RayTracer.Vector(v.x * w, v.y * w, v.z * w);
    },

    toString : function () {
        return 'Vector [' + this.x + ',' + this.y + ',' + this.z + ']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Ray(position, direction) {
    this.position = undefined;
    this.direction = undefined;
    this.initialize(position, direction);
}

Ray.prototype = {
    position : null,
    direction : null,
    initialize : function(pos, dir) {
        this.position = pos;
        this.direction = dir;
    },

    toString : function () {
        return 'Ray [' + this.position.toString() + ',' + this.direction.toString() + ']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Scene() {
    this.camera = undefined;
    this.shapes = undefined;
    this.lights = undefined;
    this.background = undefined;
    this.initialize();
}

Scene.prototype = {
    camera : null,
    shapes : [],
    lights : [],
    background : null,

    initialize : function() {
        this.camera = new Flog.RayTracer.Camera(
            new Flog.RayTracer.Vector(0.0,0.0,-5.0),
            new Flog.RayTracer.Vector(0.0,0.0,1.0),
            new Flog.RayTracer.Vector(0.0,1.0,0.0)
        );
        this.shapes = new Array();
        this.lights = new Array();
        this.background = new Flog.RayTracer.Background(new Flog.RayTracer.Color(0.0,0.0,0.5), 0.2);
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};
//if(typeof(Flog.RayTracer.Material) == 'undefined') Flog.RayTracer.Material = {};

function BaseMaterial() {
//    this.initialize(); // SJS: note this class is never instantiated
}

BaseMaterial.prototype = {

    gloss: 2.0,             // [0...infinity] 0 = matt
    transparency: 0.0,      // 0=opaque
    reflection: 0.0,        // [0...infinity] 0 = no reflection
    refraction: 0.50,
    hasTexture: false,

// interferes with the method in subclasses
//    initialize : function() {
//
//    },
//
//    getColor: function(u, v){
//
//    },

    wrapUp: function(t){
        t = t % 2;
        if(t < -1) t += 2.0;
        if(t >= 1) t -= 2.0;
        return t;
    },

    toString : function () {
    // SJS printing of bool
        return 'Material [gloss=' + this.gloss + ', transparency=' + this.transparency + ', hasTexture=' + (this.hasTexture?"true":"false") +']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function SolidInheritor() {
    this.initialize = null;
    this.getColor = null;
    this.toString = null;
}
SolidInheritor.prototype = BaseMaterial.prototype;

function Solid(color, reflection, refraction, transparency, gloss) {
    this.color = undefined;
    this.gloss = undefined;
    this.transparency = undefined;
    this.reflection = undefined;
    this.refraction = undefined;
    this.hasTexture = undefined;
    this.initialize(color, reflection, refraction, transparency, gloss)
}

Solid.prototype = new SolidInheritor();

Solid.prototype.initialize = function(color, reflection, refraction, transparency, gloss) {
            this.color = color;
            this.reflection = reflection;
            this.transparency = transparency;
            this.gloss = gloss;
            // SJS: what about refraction?
            this.hasTexture = false;
}

Solid.prototype.getColor = function(u, v){
            return this.color;
        }

Solid.prototype.toString = function () {
            // SJS deal with printing of boolean
            return 'SolidMaterial [gloss=' + this.gloss + ', transparency=' + this.transparency + ', hasTexture=' + (this.hasTexture ? "true" : "false") +']';
}

/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function ChessboardInheritor() {
    this.initialize = null;
    this.getColor = null;
    this.toString = null;
}
ChessboardInheritor.prototype = BaseMaterial.prototype;

function Chessboard(colorEven, colorOdd, reflection, transparency, gloss, density) {
    this.color = undefined;
    this.gloss = undefined;
    this.transparency = undefined;
    this.reflection = undefined;
    this.refraction = undefined;
    this.hasTexture = undefined;
    this.colorEven = undefined;
    this.colorOdd = undefined;
    this.density = undefined;
    this.initialize(colorEven, colorOdd, reflection, transparency, gloss, density)
}
Chessboard.prototype = new ChessboardInheritor();

Chessboard.prototype.initialize = function(colorEven, colorOdd, reflection, transparency, gloss, density) {
            this.colorEven = colorEven;
            this.colorOdd = colorOdd;
            this.reflection = reflection;
            this.transparency = transparency;
            this.gloss = gloss;
            this.density = density;
            this.hasTexture = true;
}

Chessboard.prototype.getColor = function(u, v){
            var t = this.wrapUp(u * this.density) * this.wrapUp(v * this.density);

            if(t < 0.0)
                return this.colorEven;
            else
                return this.colorOdd;
}

Chessboard.prototype.toString = function () {
// SJS: does not convert boolean to string well
            return 'ChessMaterial [gloss=' + this.gloss + ', transparency=' + this.transparency + ', hasTexture=' + (this.hasTexture? "true" : "false") +']';
}

/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};
//if(typeof(Flog.RayTracer.Shape) == 'undefined') Flog.RayTracer.Shape = {};

function Sphere(pos, radius, material) {
    this.radius = undefined;
    this.position = undefined;
    this.material = undefined;
    this.initialize(pos, radius, material);
}

Sphere.prototype = {
    initialize : function(pos, radius, material) {
        this.radius = radius;
        this.position = pos;
        this.material = material;
    },

    intersect: function(ray){
        var info = new Flog.RayTracer.IntersectionInfo();
        info.shape = this;

        var dst = Flog.RayTracer.Vector.prototype.subtract(ray.position, this.position);

        var B = dst.dot(ray.direction);
        var C = dst.dot(dst) - (this.radius * this.radius);
        var D = (B * B) - C;

        if(D > 0){ // intersection!
            info.isHit = true;
            info.distance = (-B) - Math.sqrt(D);
            info.position = Flog.RayTracer.Vector.prototype.add(
                                                ray.position,
                                                Flog.RayTracer.Vector.prototype.multiplyScalar(
                                                    ray.direction,
                                                    info.distance
                                                )
                                            );
            info.normal = Flog.RayTracer.Vector.prototype.subtract(
                                            info.position,
                                            this.position
                                        ).normalize();

            info.color = this.material.getColor(0,0);
        } else {
            info.isHit = false;
        }
        return info;
    },

    toString : function () {
        return 'Sphere [position=' + this.position.toString() + ', radius=' + this.radius + ']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};
//if(typeof(Flog.RayTracer.Shape) == 'undefined') Flog.RayTracer.Shape = {};

function Plane(pos, d, material) {
    this.position = undefined;
    this.d = undefined;
    this.material = undefined;
    this.initialize(pos, d, material);
}

Plane.prototype = {
    d: 0.0,

    initialize : function(pos, d, material) {
        this.position = pos;
        this.d = d;
        this.material = material;
    },

    intersect: function(ray){
        var info = new Flog.RayTracer.IntersectionInfo();

        var Vd = this.position.dot(ray.direction);
        if(Vd == 0) return info; // no intersection

        var t = -(this.position.dot(ray.position) + this.d) / Vd;
        if(t <= 0) return info;

        info.shape = this;
        info.isHit = true;
        info.position = Flog.RayTracer.Vector.prototype.add(
                                            ray.position,
                                            Flog.RayTracer.Vector.prototype.multiplyScalar(
                                                ray.direction,
                                                t
                                            )
                                        );
        info.normal = this.position;
        info.distance = t;

        if(this.material.hasTexture){
            var vU = new Flog.RayTracer.Vector(this.position.y, this.position.z, -this.position.x);
            var vV = vU.cross(this.position);
            var u = info.position.dot(vU);
            var v = info.position.dot(vV);
            info.color = this.material.getColor(u,v);
        } else {
            info.color = this.material.getColor(0,0);
        }

        return info;
    },

    toString : function () {
        return 'Plane [' + this.position.toString() + ', d=' + this.d + ']';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function IntersectionInfo() {
    this.isHit = false;
    this.hitCount = 0;
    this.shape = null;
    this.position = null;
    this.normal = null;
    this.color = undefined;
    this.distance = 0.0; // SJS: null not a valid float
    this.initialize();
}

IntersectionInfo.prototype = {
    isHit: false,
    hitCount: 0,
    shape: null,
    position: null,
    normal: null,
    color: null,
    distance: 0.0,

    initialize : function() {
        this.color = new Flog.RayTracer.Color(0.0,0.0,0.0);
    },

    toString : function () {
        if (this.position) {
            return 'Intersection [' + this.position.toString() +  ']';
        } else {
            return 'Intersection [ null ]';
        }
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Camera(pos, lookAt, up) {
    this.position = undefined;
    this.lookAt = undefined;
    this.equator = undefined;
    this.up = undefined;
    this.screen = undefined;
    this.initialize(pos, lookAt, up);
}

Camera.prototype = {
    position: null,
    lookAt: null,
    equator: null,
    up: null,
    screen: null,

    initialize : function(pos, lookAt, up) {
        this.position = pos;
        this.lookAt = lookAt;
        this.up = up;
        this.equator = lookAt.normalize().cross(this.up);
        this.screen = Flog.RayTracer.Vector.prototype.add(this.position, this.lookAt);
    },

    getRay: function(vx, vy){
        var pos = Flog.RayTracer.Vector.prototype.subtract(
            this.screen,
            Flog.RayTracer.Vector.prototype.subtract(
                Flog.RayTracer.Vector.prototype.multiplyScalar(this.equator, vx),
                Flog.RayTracer.Vector.prototype.multiplyScalar(this.up, vy)
            )
        );
        pos.y = pos.y * -1;
        var dir = Flog.RayTracer.Vector.prototype.subtract(
            pos,
            this.position
        );

        var ray = new Flog.RayTracer.Ray(pos, dir.normalize());

        return ray;
    },

    toString : function () {
        return 'Ray []';
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Background(color, ambience) {
    this.color = undefined;
    this.ambience = undefined;
    this.initialize(color, ambience);
}

Background.prototype = {
    color : null,
    ambience : 0.0,

    initialize : function(color, ambience) {
        this.color = color;
        this.ambience = ambience;
    }
}
/* Fake a Flog.* namespace */
//if(typeof(Flog) == 'undefined') var Flog = {};
//if(typeof(Flog.RayTracer) == 'undefined') Flog.RayTracer = {};

function Engine(options) {
    this.canvas = null;
    this.options = {
       canvasHeight: 100,
       canvasWidth: 100,
       pixelWidth: 2,
       pixelHeight: 2,
       renderDiffuse: false,
       renderShadows: false,
       renderHighlights: false,
       renderReflections: false,
       rayDepth: 2
    };
    this.initialize(options);
}

Engine.prototype = {
    canvas: null, /* 2d context we can render to */

    initialize: function(options){
        if (options) {
            this.options.canvasHeight = options.canvasHeight;
            this.options.canvasWidth = options.canvasWidth;
            this.options.pixelWidth = options.pixelWidth;
            this.options.pixelHeight = options.pixelHeight;
            this.options.renderDiffuse = options.renderDiffuse;
            this.options.renderShadows = options.renderShadows;
            this.options.renderHighlights = options.renderHighlights;
            this.options.renderReflections = options.renderReflections;
            this.options.rayDepth = options.rayDepth;
        }

        this.options.canvasHeight /= this.options.pixelHeight;
        this.options.canvasWidth /= this.options.pixelWidth;

        /* TODO: dynamically include other scripts */
    },

    setPixel: function(x, y, color){
        var pxW, pxH;
        pxW = this.options.pixelWidth;
        pxH = this.options.pixelHeight;

        if (this.canvas) {
          this.canvas.fillStyle = color.toString();
          this.canvas.fillRect (x * pxW, y * pxH, pxW, pxH);
        } else {
          if (x ===  y) {
            checkNumber += color.brightness();
          }
          // print(x * pxW, y * pxH, pxW, pxH);
        }
    },

    renderScene: function(scene, canvas){
        checkNumber = 0;
        /* Get canvas */
        if (canvas) {
          this.canvas = canvas.getContext("2d");
        } else {
          this.canvas = null;
        }

        var canvasHeight = this.options.canvasHeight;
        var canvasWidth = this.options.canvasWidth;

        for(var y=0; y < canvasHeight; y++){
            for(var x=0; x < canvasWidth; x++){
                var yp = y * 1.0 / canvasHeight * 2 - 1;
          		var xp = x * 1.0 / canvasWidth * 2 - 1;

          		var ray = scene.camera.getRay(xp, yp);

          		var color = this.getPixelColor(ray, scene);

            	this.setPixel(x, y, color);
            }
        }
        if (checkNumber !== 2321) {
          //throw new Error("Scene rendered incorrectly");
          console.error("Scene rendered incorrectly");
        }
    },

    getPixelColor: function(ray, scene){
        var info = this.testIntersection(ray, scene, null);
        if(info.isHit){
            var color = this.rayTrace(info, ray, scene, 0);
            return color;
        }
        return scene.background.color;
    },

    testIntersection: function(ray, scene, exclude){
        var hits = 0;
        var best = new Flog.RayTracer.IntersectionInfo();
        best.distance = 2000.0;

        for(var i=0; i<scene.shapes.length; i++){
            var shape = scene.shapes[i];

            if(shape != exclude){
                var info = shape.intersect(ray);
                if(info.isHit && info.distance >= 0 && info.distance < best.distance){
                    best = info;
                    hits++;
                }
            }
        }
        best.hitCount = hits;
        return best;
    },

    getReflectionRay: function(P,N,V){
        var c1 = -N.dot(V);
        var R1 = Flog.RayTracer.Vector.prototype.add(
            Flog.RayTracer.Vector.prototype.multiplyScalar(N, 2*c1),
            V
        );
        return new Flog.RayTracer.Ray(P, R1);
    },

    rayTrace: function(info, ray, scene, depth){
        // Calc ambient
        var color = Flog.RayTracer.Color.prototype.multiplyScalar(info.color, scene.background.ambience);
        var oldColor = color;
        var shininess = Math.pow(10, info.shape.material.gloss + 1);

        for(var i=0; i<scene.lights.length; i++){
            var light = scene.lights[i];

            // Calc diffuse lighting
            var v = Flog.RayTracer.Vector.prototype.subtract(
                                light.position,
                                info.position
                            ).normalize();

            if(this.options.renderDiffuse){
                var L = v.dot(info.normal);
                if(L > 0.0){
                    color = Flog.RayTracer.Color.prototype.add(
                                        color,
                                        Flog.RayTracer.Color.prototype.multiply(
                                            info.color,
                                            Flog.RayTracer.Color.prototype.multiplyScalar(
                                                light.color,
                                                L
                                            )
                                        )
                                    );
                }
            }

            // The greater the depth the more accurate the colours, but
            // this is exponentially (!) expensive
            if(depth <= this.options.rayDepth){
          // calculate reflection ray
          if(this.options.renderReflections && info.shape.material.reflection > 0)
          {
              var reflectionRay = this.getReflectionRay(info.position, info.normal, ray.direction);
              var refl = this.testIntersection(reflectionRay, scene, info.shape);

              if (refl.isHit && refl.distance > 0){
                  refl.color = this.rayTrace(refl, reflectionRay, scene, depth + 1);
              } else {
                  refl.color = scene.background.color;
                        }

                  color = Flog.RayTracer.Color.prototype.blend(
                    color,
                    refl.color,
                    info.shape.material.reflection
                  );
          }

                // Refraction
                /* TODO */
            }

            /* Render shadows and highlights */

            var shadowInfo = new Flog.RayTracer.IntersectionInfo();

            if(this.options.renderShadows){
                var shadowRay = new Flog.RayTracer.Ray(info.position, v);

                shadowInfo = this.testIntersection(shadowRay, scene, info.shape);
                if(shadowInfo.isHit && shadowInfo.shape != info.shape /*&& shadowInfo.shape.type != 'PLANE'*/){
                    var vA = Flog.RayTracer.Color.prototype.multiplyScalar(color, 0.5);
                    var dB = (0.5 * Math.pow(shadowInfo.shape.material.transparency, 0.5));
                    color = Flog.RayTracer.Color.prototype.addScalar(vA,dB);
                }
            }

      // Phong specular highlights
      if(this.options.renderHighlights && !shadowInfo.isHit && info.shape.material.gloss > 0){
        var Lv = Flog.RayTracer.Vector.prototype.subtract(
                            info.shape.position,
                            light.position
                        ).normalize();

        var E = Flog.RayTracer.Vector.prototype.subtract(
                            scene.camera.position,
                            info.shape.position
                        ).normalize();

        var H = Flog.RayTracer.Vector.prototype.subtract(
                            E,
                            Lv
                        ).normalize();

        var glossWeight = Math.pow(Math.max(info.normal.dot(H), 0), shininess);
        color = Flog.RayTracer.Color.prototype.add(
                            Flog.RayTracer.Color.prototype.multiplyScalar(light.color, glossWeight),
                            color
                        );
      }
        }
        color.limit();
        return color;
    }
};


function renderScene(){
    var scene = new Flog.RayTracer.Scene();

    scene.camera = new Flog.RayTracer.Camera(
                        new Flog.RayTracer.Vector(0.0, 0.0, -15.0),
                        new Flog.RayTracer.Vector(-0.2, 0.0, 5.0),
                        new Flog.RayTracer.Vector(0.0, 1.0, 0.0)
                    );

    scene.background = new Flog.RayTracer.Background(
                                new Flog.RayTracer.Color(0.5, 0.5, 0.5),
                                0.4
                            );

    var sphere = new Flog.RayTracer.Shape.Sphere(
        new Flog.RayTracer.Vector(-1.5, 1.5, 2.0),
        1.5,
        new Flog.RayTracer.Material.Solid(
            new Flog.RayTracer.Color(0.0,0.5,0.5),
            0.3,
            0.0,
            0.0,
            2.0
        )
    );

    var sphere1 = new Flog.RayTracer.Shape.Sphere(
        new Flog.RayTracer.Vector(1.0, 0.25, 1.0),
        0.5,
        new Flog.RayTracer.Material.Solid(
            new Flog.RayTracer.Color(0.9,0.9,0.9),
            0.1,
            0.0,
            0.0,
            1.5
        )
    );

    var plane = new Flog.RayTracer.Shape.Plane(
                                new Flog.RayTracer.Vector(0.1, 0.9, -0.5).normalize(),
                                1.2,
                                new Flog.RayTracer.Material.Chessboard(
                                    new Flog.RayTracer.Color(1.0,1.0,1.0),
                                    new Flog.RayTracer.Color(0.0,0.0,0.0),
                                    0.2,
                                    0.0,
                                    1.0,
                                    0.7
                                )
                            );

    scene.shapes.push(plane);
    scene.shapes.push(sphere);
    scene.shapes.push(sphere1);

    var light = new Flog.RayTracer.Light(
        new Flog.RayTracer.Vector(5.0, 10.0, -1.0),
        new Flog.RayTracer.Color(0.8, 0.8, 0.8),
        10.0 // SJS - otherwise this arg is undefined
    );

    var light1 = new Flog.RayTracer.Light(
        new Flog.RayTracer.Vector(-3.0, 5.0, -15.0),
        new Flog.RayTracer.Color(0.8, 0.8, 0.8),
        100
    );

    scene.lights.push(light);
    scene.lights.push(light1);

    var imageWidth = 100; // $F('imageWidth');
    var imageHeight = 100; // $F('imageHeight');
    var pixelSize = [5.0, 5.0]; //"5,5".split(','); //  $F('pixelSize').split(',');
    var renderDiffuse = true; // $F('renderDiffuse');
    var renderShadows = true; // $F('renderShadows');
    var renderHighlights = true; // $F('renderHighlights');
    var renderReflections = true; // $F('renderReflections');
    var rayDepth = 2;//$F('rayDepth');

    var raytracer = new Flog.RayTracer.Engine(
        {
            canvasWidth: imageWidth,
            canvasHeight: imageHeight,
            pixelWidth: pixelSize[0],
            pixelHeight: pixelSize[1],
            renderDiffuse: renderDiffuse,
            renderHighlights: renderHighlights,
            renderShadows: renderShadows,
            renderReflections: renderReflections,
            rayDepth: rayDepth
        }
    );

    raytracer.renderScene(scene, null);
}

for(var i = 0; i < 600; ++i) {
    renderScene();
}
