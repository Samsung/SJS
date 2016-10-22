/*
 * Copyright (C) Rich Moore.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

/////. Start CORDIC

var AG_CONST = 0.6072529350;

function lFIXED(X)
{
  return X * 65536.0;
}

function lFLOAT(X)
{
  return X / 65536.0;
}

function lDEG2RAD(X)
{
  return 0.017453 * (X);
}

var Angles = [
  lFIXED(45.0), lFIXED(26.565), lFIXED(14.0362), lFIXED(7.12502),
  lFIXED(3.57633), lFIXED(1.78991), lFIXED(0.895174), lFIXED(0.447614),
  lFIXED(0.223811), lFIXED(0.111906), lFIXED(0.055953),
  lFIXED(0.027977) 
              ];

var Target = 28.027;

function cordicsincos(Target) {
    var X;
    var Y;
    var TargetAngle;
    var CurrAngle;
    var Step;
 
    X = lFIXED(AG_CONST);         /* AG_CONST * cos(0) */
    Y = 0;                       /* AG_CONST * sin(0) */

    TargetAngle = lFIXED(Target);
    CurrAngle = 0;
    for (Step = 0; Step < 12; Step++) {
        var NewX;
        if (TargetAngle > CurrAngle) {
            NewX = X - (Y >> Step);
            Y = (X >> Step) + Y;
            X = NewX;
            CurrAngle += Angles[Step];
        } else {
            NewX = X + (Y >> Step);
            Y = -(X >> Step) + Y;
            X = NewX;
            CurrAngle -= Angles[Step];
        }
    }

    return lFLOAT(X) * lFLOAT(Y);
}

///// End CORDIC

var total = 0;

function cordic( runs ) {
  var start = new Date();

  for ( var i = 0 ; i < runs ; i++ ) {
      total += cordicsincos(Target);
  }

  var end = new Date();

  return end.getTime() - start.getTime();
}

cordic(25000);

var expected = 10362.570468755888;

if (total != expected)
    console.log("Error")
//    throw "ERROR: bad result: expected " + expected + " but got " + total;

