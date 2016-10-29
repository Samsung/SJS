Attempt to create ports that have minimal difference from the original Octane2.0 version. This is in contract to octane-hacks folder.

1. richards

> Here we just needed to move elsewhere any stuff between

function C() { ...}
... from HERE
C.prototype.foo = ...


2. splay

> Minor: exceptions, upper and lower case

> The name SplayTree is used both as a constructor and as the name of an object. Here it seems to
be for the purpose of simulating namespaces.

function SplayTree() { }

SplayTree.Node = function () { ... } // defining a constructor


3. deltablue

> 'Strength' is used both as a constructor and as an object

> class inheritance pattern (below), with calls to super's constructor as well as methods, used pervasively.

    Object.defineProperty(Object.prototype, "inheritsFrom", {

      value: function (shuper) {
        function Inheriter() { }
        Inheriter.prototype = shuper.prototype;
        this.prototype = new Inheriter();
        this.superConstructor = shuper;
      }
    });
    function UnaryConstraint(v, strength) {
      UnaryConstraint.superConstructor.call(this, strength);
    // additional fields in the subclass
    }
    UnaryConstraint.inheritsFrom(Constraint);


> OrderedCollection is used polymorphically

> sometimes method overrides a function ... (minor)

4. navier_stokes

No issues

5. raytrace

> Uses the following for object creation and initialization.
    var Class = {
      create: function() {
        return function() {
          this.initialize.apply(this, arguments);
        }
      }
    };
    Object.extend = function(destination, source) {
      for (var property in source) {
        destination[property] = source[property];
      }
      return destination;
    };

> uses null to inhibit a float, which does not work

> some issues with undefined and floats. int to float conversion is broken too. 0 cannot work as 0.0,
due to some bug it becomes a NaN.

> toString conversions

> overriding of incompatible method sig

> Nice use of subtyping based on structural types. There are two kinds of Shapes: Spheres and Planes.
There is an array that stores Shapes, doesn't care if it is Spheres or Planes.

