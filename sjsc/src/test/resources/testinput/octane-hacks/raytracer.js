// The ray tracer code in this file is written by Adam Burmister. It
// is available in its original form from:
//
//   http://labs.flog.nz.co/raytracer/
//
// It has been modified slightly by Google to work as a standalone
// benchmark, but the all the computational code remains
// untouched. This file also contains a copy of parts of the Prototype
// JavaScript framework which is used by the ray tracer.



// Variable used to hold a number that can be used to verify that
// the scene was ray traced correctly.
var checkNumber = 0;

var Flog = {
    RayTracer : null,
};

Flog.RayTracer = {
    Color: Color,
    Light: Light,
    Vector: Vector,
    Ray: Ray,
    Scene: Scene,
    Material: {
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
};

function Color(r, g, b) {
    this.red = r;
    this.green = g;
    this.blue = b;
}

Color.prototype = {
    red: 0.0,
    green: 0.0,
    blue: 0.0,

    add: function (c1, c2) {
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
};

function Light(pos, color, intensity) {
    this.position = pos;
    this.color = color;
    this.intensity = intensity;
}

Light.prototype = {
    toString : function () {
        return 'Light [' + this.position.x + ',' + this.position.y + ',' + this.position.z + ']';
    }
};

function Vector(x, y, z) {
    this.x = x;
    this.y = y;
    this.z = z;
}

Vector.prototype = {
    x : 0.0,
    y : 0.0,
    z : 0.0,

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
        //    if(!w || !v) throw 'Vectors must be defined [' + v + ',' + w + ']';
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
};

function Ray(pos, dir) {
    this.position = pos;
    this.direction = dir;
}

Ray.prototype = {
    toString : function () {
        return 'Ray [' + this.position.toString() + ',' + this.direction.toString() + ']';
    }
};

function Scene() {
    this.camera = new Flog.RayTracer.Camera(
            new Flog.RayTracer.Vector(0.0,0.0,-5.0),
            new Flog.RayTracer.Vector(0.0,0.0,1.0),
            new Flog.RayTracer.Vector(0.0,1.0,0.0)
            );
    this.spheres = [];
    this.planes = [];
    this.lights = [];
    this.background = new Flog.RayTracer.Background(new Flog.RayTracer.Color(0.0,0.0,0.5), 0.2);
}

function Solid(color, reflection, refraction, transparency, gloss) {
    this.color = color;
    this.reflection = reflection;
    this.refraction = 0.5;
    this.transparency = transparency;
    this.gloss = gloss;
    this.hasTexture = false;
}

Solid.prototype = {
    getColor: function(u, v){
        return this.color;
    },

    wrapUp: function(t){
        t = t % 2;
        if(t < -1) t += 2.0;
        if(t >= 1) t -= 2.0;
        return t;
    },

    toString : function () {
        return 'SolidMaterial [gloss=' + this.gloss + ', transparency=' + this.transparency + ', hasTexture=' + (this.hasTexture? "true" : "false")  +']'; // FIXME: toString on BooleanType
    }
};

function Chessboard(colorEven, colorOdd, reflection, transparency, gloss, density) {
    this.color = null;
    this.colorEven = colorEven;
    this.colorOdd = colorOdd;
    this.reflection = reflection;
    this.refraction = 0.5;
    this.transparency = transparency;
    this.gloss = gloss;
    this.density = density;
    this.hasTexture = true;
}

Chessboard.prototype = {
    getColor: function(u, v){
        var t = this.wrapUp(u * this.density) * this.wrapUp(v * this.density);

        if(t < 0.0)
            return this.colorEven;
        else
            return this.colorOdd;
    },

    wrapUp: function(t){
        t = t % 2;
        if(t < -1) t += 2.0;
        if(t >= 1) t -= 2.0;
        return t;
    },

    toString : function () {
        return 'ChessMaterial [gloss=' + this.gloss + ', transparency=' + this.transparency + ', hasTexture=' + (this.hasTexture? "true" : "false")  +']'; // FIXME: toString on BooleanType 
    }

};

function Sphere(pos, radius, material) {
    this.radius = radius;
    this.position = pos;
    this.material = material;
}

Sphere.prototype = {
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
        return 'Sphere [position=' + this.position.toString() + ', radius=' + this.radius.toString() + ']';
    }
};

function Plane(pos, d, material) {
    this.position = pos;
    this.d = d;
    this.material = material;
}

Plane.prototype = {
    d: 0.0,

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
};

function IntersectionInfo() {
    this.color = new Flog.RayTracer.Color(0.0,0.0,0.0);
    this.isHit = false;
    this.hitCount = 0;
    this.shape = null;
    this.position = null;
    this.normal = null;
    this.color = null;
    this.distance = 0.0;
}

IntersectionInfo.prototype = {
    toString : function () {
        if (this.position) {
            return 'Intersection [' + this.position.toString() +  ']';
        } else {
            return 'Intersection [ null ]';
        }   
    }
};

function Camera(pos, lookAt, up) {
    this.position = pos;
    this.lookAt = lookAt;
    this.up = up;
    this.equator = lookAt.normalize().cross(this.up);
    this.screen = Flog.RayTracer.Vector.prototype.add(this.position, this.lookAt);
}

Camera.prototype = {
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
};

function Background(color, ambience) {
    this.color = color;
    this.ambience = ambience
}

Background.prototype = {
    color : null,
    ambience : 0.0,
};

function Engine(options){
    this.options = {
        canvasHeight: 100.0,
        canvasWidth: 100.0,
        pixelWidth: 2.0,
        pixelHeight: 2.0,
        renderDiffuse: false,
        renderShadows: false,
        renderHighlights: false,
        renderReflections: false,
        rayDepth: 2
    };

    this.options.canvasHeight = options.canvasHeight;
    this.options.canvasWidth = options.canvasWidth;
    this.options.pixelWidth = options.pixelWidth;
    this.options.pixelHeight = options.pixelHeight;
    this.options.renderDiffuse = options.renderDiffuse;
    this.options.renderShadows = options.renderShadows;
    this.options.renderHighlights = options.renderHighlights;
    this.options.renderReflections = options.renderReflections;
    this.options.rayDepth = options.rayDepth;

    this.options.canvasHeight /= this.options.pixelHeight;
    this.options.canvasWidth /= this.options.pixelWidth;

    /* TODO: dynamically include other scripts */
}

Engine.prototype = {
    canvas: null, /* 2d context we can render to */

    setPixel: function(x, y, color){
        var pxW, pxH;
        pxW = this.options.pixelWidth;
        pxH = this.options.pixelHeight;

        if (x === y) {
            checkNumber += color.brightness();
        }
        // print(x * pxW, y * pxH, pxW, pxH);
    },

    renderScene: function(scene, canvas) {
        checkNumber = 0;

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
            print("Scene rendered incorrectly");
            // throw new Error("Scene rendered incorrectly");
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
        var len = scene.planes.length;
        var planes = [];
        var spheres = [];

        for(var i=0; i<len; i++){
            var plane = scene.planes.pop();

            if(plane != exclude){
                var info = plane.intersect(ray);
                if(info.isHit && info.distance >= 0 && info.distance < best.distance){
                    best = info;
                    hits++;
                }
            }

            planes.push(plane);
        }

        for(var i=0; i<len; i++) {
          scene.planes.push(planes.pop());
        }

        len = scene.spheres.length;

        for(var i=0; i<len; i++){
            var sphere = scene.spheres.pop();
            if(sphere != exclude){
                var info = sphere.intersect(ray);
                if(info.isHit && info.distance >= 0 && info.distance < best.distance){
                    best = info;
                    hits++;
                }
            }
            spheres.push(sphere);
        }

        for(var i=0; i<len; i++) {
          scene.spheres.push(spheres.pop());
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

function renderScene() {
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

    scene.spheres.push(sphere);
    scene.spheres.push(sphere1);
    scene.planes.push(plane);

    var light = new Flog.RayTracer.Light(
            new Flog.RayTracer.Vector(5.0, 10.0, -1.0),
            new Flog.RayTracer.Color(0.8, 0.8, 0.8),
            10.0
            );

    var light1 = new Flog.RayTracer.Light(
            new Flog.RayTracer.Vector(-3.0, 5.0, -15.0),
            new Flog.RayTracer.Color(0.8, 0.8, 0.8),
            100
            );

    scene.lights.push(light);
    scene.lights.push(light1);

    var imageWidth = 100.0; // $F('imageWidth');
    var imageHeight = 100.0; // $F('imageHeight');
    var pixelWidth = 5.0;
    var pixelHeight = 5.0;
    var renderDiffuse = true; // $F('renderDiffuse');
    var renderShadows = true; // $F('renderShadows');
    var renderHighlights = true; // $F('renderHighlights');
    var renderReflections = true; // $F('renderReflections');
    var rayDepth = 2;//$F('rayDepth');

    var raytracer = new Flog.RayTracer.Engine(
            {
                canvasWidth: imageWidth,
        canvasHeight: imageHeight,
        pixelWidth: pixelWidth,
        pixelHeight: pixelHeight,
        renderDiffuse: renderDiffuse,
        renderHighlights: renderHighlights,
        renderShadows: renderShadows,
        renderReflections: renderReflections,
        rayDepth: rayDepth
            }
            );

    raytracer.renderScene(scene, null);
}

for(var i = 0; i <600; ++i) {
    renderScene();
}
