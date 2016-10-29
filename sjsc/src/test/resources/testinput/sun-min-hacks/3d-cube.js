// 3D Cube Rotation
// http://www.speich.net/computer/moztesting/3d.htm
// Created by Simon Speich

var Q = new Array();
var Q_props = null;
var MTrans = new Array();  // transformation matrix
var MQube = new Array();  // position information of qube
var I = new Array();      // entity matrix
var Origin = null;
var Testing = null;
var LoopTime = null;

var validation = [];
validation[20] = 2889.0000000000045;
validation[40] = 2889.0000000000055;
validation[80] = 2889.000000000005;
validation[160] = 2889.0000000000055;

var DisplArea = { Width: 300, Height: 300 };

function drawLine(From, To) {
  var x1 = From.V[0];
  var x2 = To.V[0];
  var y1 = From.V[1];
  var y2 = To.V[1];
  var dx = Math.abs(x2 - x1);
  var dy = Math.abs(y2 - y1);
  var x = x1;
  var y = y1;
  var IncX1, IncY1;
  var IncX2, IncY2;  
  var Den;
  var Num;
  var NumAdd;
  var NumPix;

  if (x2 >= x1) {  IncX1 = 1; IncX2 = 1;  }
  else { IncX1 = -1; IncX2 = -1; }
  if (y2 >= y1)  {  IncY1 = 1; IncY2 = 1; }
  else { IncY1 = -1; IncY2 = -1; }
  if (dx >= dy) {
    IncX1 = 0;
    IncY2 = 0;
    Den = dx;
    Num = dx / 2;
    NumAdd = dy;
    NumPix = dx;
  }
  else {
    IncX2 = 0;
    IncY1 = 0;
    Den = dy;
    Num = dy / 2;
    NumAdd = dx;
    NumPix = dy;
  }

  NumPix = Math.round(Q_props.LastPx + NumPix);

  var i = Q_props.LastPx;
  for (; i < NumPix; i++) {
    Num += NumAdd;
    if (Num >= Den) {
      Num -= Den;
      x += IncX1;
      y += IncY1;
    }
    x += IncX2;
    y += IncY2;
  }
  Q_props.LastPx = NumPix;
}

function calcCross(V0, V1) {
  var Cross = new Array();
  Cross[0] = V0[1]*V1[2] - V0[2]*V1[1];
  Cross[1] = V0[2]*V1[0] - V0[0]*V1[2];
  Cross[2] = V0[0]*V1[1] - V0[1]*V1[0];
  return Cross;
}

function calcNormal(V0, V1, V2) {
  var A = new Array();   var B = new Array(); 
  for (var i = 0; i < 3; i++) {
    A[i] = V0[i] - V1[i];
    B[i] = V2[i] - V1[i];
  }
  A = calcCross(A, B);
  var Length = Math.sqrt(A[0]*A[0] + A[1]*A[1] + A[2]*A[2]); 
  for (var i = 0; i < 3; i++) A[i] = A[i] / Length;
  A[3] = 1;
  return A;
}

function CreateP(X,Y,Z) {
  this.V = [X,Y,Z,1];
}

// multiplies two matrices
function mMulti(M1, M2) {
  var M = [[],[],[],[]];
  var i = 0;
  var j = 0;
  for (; i < 4; i++) {
    j = 0;
    for (; j < 4; j++) M[i][j] = M1[i][0] * M2[0][j] + M1[i][1] * M2[1][j] + M1[i][2] * M2[2][j] + M1[i][3] * M2[3][j];
  }
  return M;
}

//multiplies matrix with vector
function vMulti(M, V) {
  var Vect = new Array();
  var i = 0;
  for (;i < 4; i++) Vect[i] = M[i][0] * V[0] + M[i][1] * V[1] + M[i][2] * V[2] + M[i][3] * V[3];
  return Vect;
}

function vMulti2(M, V) {
  var Vect = new Array();
  var i = 0;
  for (;i < 3; i++) Vect[i] = M[i][0] * V[0] + M[i][1] * V[1] + M[i][2] * V[2];
  return Vect;
}

// add to matrices
function mAdd(M1, M2) {
  var M = [[],[],[],[]];
  var i = 0;
  var j = 0;
  for (; i < 4; i++) {
    j = 0;
    for (; j < 4; j++) M[i][j] = M1[i][j] + M2[i][j];
  }
  return M;
}

function translate(M, Dx, Dy, Dz) {
  var T = [
  [1,0,0,Dx],
  [0,1,0,Dy],
  [0,0,1,Dz],
  [0,0,0,1]
  ];
  return mMulti(T, M);
}

function rotateX(M, Phi) {
  var a = Phi;
  a *= Math.PI / 180;
  var Cos = Math.cos(a);
  var Sin = Math.sin(a);
  var R = [
  [1,0,0,0],
  [0,Cos,-Sin,0],
  [0,Sin,Cos,0],
  [0,0,0,1]
  ];
  return mMulti(R, M);
}

function rotateY(M, Phi) {
  var a = Phi;
  a *= Math.PI / 180;
  var Cos = Math.cos(a);
  var Sin = Math.sin(a);
  var R = [
  [Cos,0,Sin,0],
  [0,1,0,0],
  [-Sin,0,Cos,0],
  [0,0,0,1]
  ];
  return mMulti(R, M);
}

function rotateZ(M, Phi) {
  var a = Phi;
  a *= Math.PI / 180;
  var Cos = Math.cos(a);
  var Sin = Math.sin(a);
  var R = [
  [Cos,-Sin,0,0],
  [Sin,Cos,0,0],
  [0,0,1,0],   
  [0,0,0,1]
  ];
  return mMulti(R, M);
}

function drawQube() {
  // calc current normals
  var CurN = new Array();
  var i = 5;
  Q_props.LastPx = 0;
  for (; i > -1; i--) CurN[i] = vMulti2(MQube, Q_props.Normal[i]);
  if (CurN[0][2] < 0) {
    if (!Q_props.Line[0]) { drawLine(Q[0], Q[1]); Q_props.Line[0] = true; };
    if (!Q_props.Line[1]) { drawLine(Q[1], Q[2]); Q_props.Line[1] = true; };
    if (!Q_props.Line[2]) { drawLine(Q[2], Q[3]); Q_props.Line[2] = true; };
    if (!Q_props.Line[3]) { drawLine(Q[3], Q[0]); Q_props.Line[3] = true; };
  }
  if (CurN[1][2] < 0) {
    if (!Q_props.Line[2]) { drawLine(Q[3], Q[2]); Q_props.Line[2] = true; };
    if (!Q_props.Line[9]) { drawLine(Q[2], Q[6]); Q_props.Line[9] = true; };
    if (!Q_props.Line[6]) { drawLine(Q[6], Q[7]); Q_props.Line[6] = true; };
    if (!Q_props.Line[10]) { drawLine(Q[7], Q[3]); Q_props.Line[10] = true; };
  }
  if (CurN[2][2] < 0) {
    if (!Q_props.Line[4]) { drawLine(Q[4], Q[5]); Q_props.Line[4] = true; };
    if (!Q_props.Line[5]) { drawLine(Q[5], Q[6]); Q_props.Line[5] = true; };
    if (!Q_props.Line[6]) { drawLine(Q[6], Q[7]); Q_props.Line[6] = true; };
    if (!Q_props.Line[7]) { drawLine(Q[7], Q[4]); Q_props.Line[7] = true; };
  }
  if (CurN[3][2] < 0) {
    if (!Q_props.Line[4]) { drawLine(Q[4], Q[5]); Q_props.Line[4] = true; };
    if (!Q_props.Line[8]) { drawLine(Q[5], Q[1]); Q_props.Line[8] = true; };
    if (!Q_props.Line[0]) { drawLine(Q[1], Q[0]); Q_props.Line[0] = true; };
    if (!Q_props.Line[11]) { drawLine(Q[0], Q[4]); Q_props.Line[11] = true; };
  }
  if (CurN[4][2] < 0) {
    if (!Q_props.Line[11]) { drawLine(Q[4], Q[0]); Q_props.Line[11] = true; };
    if (!Q_props.Line[3]) { drawLine(Q[0], Q[3]); Q_props.Line[3] = true; };
    if (!Q_props.Line[10]) { drawLine(Q[3], Q[7]); Q_props.Line[10] = true; };
    if (!Q_props.Line[7]) { drawLine(Q[7], Q[4]); Q_props.Line[7] = true; };
  }
  if (CurN[5][2] < 0) {
    if (!Q_props.Line[8]) { drawLine(Q[1], Q[5]); Q_props.Line[8] = true; };
    if (!Q_props.Line[5]) { drawLine(Q[5], Q[6]); Q_props.Line[5] = true; };
    if (!Q_props.Line[9]) { drawLine(Q[6], Q[2]); Q_props.Line[9] = true; };
    if (!Q_props.Line[1]) { drawLine(Q[2], Q[1]); Q_props.Line[1] = true; };
  }
  Q_props.Line = [false,false,false,false,false,false,false,false,false,false,false,false];
  Q_props.LastPx = 0;
}

function loop() {
  if (Testing.LoopCount > Testing.LoopMax) return;
  var TestingStr = "" + Testing.LoopCount;
  while (TestingStr.length < 3) TestingStr = "0" + TestingStr;
  MTrans = translate(I, -Q[8].V[0], -Q[8].V[1], -Q[8].V[2]);
  MTrans = rotateX(MTrans, 1);
  MTrans = rotateY(MTrans, 3);
  MTrans = rotateZ(MTrans, 5);
  MTrans = translate(MTrans, Q[8].V[0], Q[8].V[1], Q[8].V[2]);
  MQube = mMulti(MTrans, MQube);
  var i = 8;
  for (; i > -1; i--) {
    Q[i].V = vMulti(MTrans, Q[i].V);
  }
  drawQube();
  Testing.LoopCount++;
  loop();
}

function init(CubeSize) {
  // init/reset vars
  Origin = { V: [150,150,20,1] };
  Testing = {
    LoopCount: 0,
    LoopMax: 50,
    TimeMax: 0,
    TimeAvg: 0,
    TimeMin: 0,
    TimeTemp: 0,
    Init: false
  };

  // transformation matrix
  MTrans = [
  [1,0,0,0],
  [0,1,0,0],
  [0,0,1,0],
  [0,0,0,1]
  ];
  
  // position information of qube
  MQube = [
  [1,0,0,0],
  [0,1,0,0],
  [0,0,1,0],
  [0,0,0,1]
  ];
  
  // entity matrix
  I = [
  [1,0,0,0],
  [0,1,0,0],
  [0,0,1,0],
  [0,0,0,1]
  ];
  
  // create qube
  Q[0] = new CreateP(-CubeSize,-CubeSize, CubeSize);
  Q[1] = new CreateP(-CubeSize, CubeSize, CubeSize);
  Q[2] = new CreateP( CubeSize, CubeSize, CubeSize);
  Q[3] = new CreateP( CubeSize,-CubeSize, CubeSize);
  Q[4] = new CreateP(-CubeSize,-CubeSize,-CubeSize);
  Q[5] = new CreateP(-CubeSize, CubeSize,-CubeSize);
  Q[6] = new CreateP( CubeSize, CubeSize,-CubeSize);
  Q[7] = new CreateP( CubeSize,-CubeSize,-CubeSize);
  
  // center of gravity
  Q[8] = new CreateP(0, 0, 0);

  Q_props = {
    // anti-clockwise edge check
    Edge: [[0,1,2],[3,2,6],[7,6,5],[4,5,1],[4,0,3],[1,5,6]],
    Normal: new Array(),
    // line drawn ?
    Line: [false,false,false,false,false,false,false,false,false,false,false,false],
    // create line pixels
    NumPx: 9 * 2 * CubeSize,
    LastPx: undefined
  };
  // calculate squad normals
  for (var i = 0; i < Q_props.Edge.length; i++) Q_props.Normal[i] = calcNormal(Q[Q_props.Edge[i][0]].V, Q[Q_props.Edge[i][1]].V, Q[Q_props.Edge[i][2]].V);
  
  for (var i = 0; i < Q_props.NumPx; i++) new CreateP(0,0,0);
  
  MTrans = translate(MTrans, Origin.V[0], Origin.V[1], Origin.V[2]);
  MQube = mMulti(MTrans, MQube);

  var i = 0;
  for (; i < 9; i++) {
    Q[i].V = vMulti(MTrans, Q[i].V);
  }
  drawQube();
  Testing.Init = true;
  loop();
  
  // Perform a simple sum-based verification.
  var sum = 0;
  for (var i = 0; i < Q.length; ++i) {
    var vector = Q[i].V;
    for (var j = 0; j < vector.length; ++j)
      sum += vector[j];
  }
  if (sum != validation[CubeSize])
    console.error("Error: bad vector sum for CubeSize = " + CubeSize + "; expected " + validation[CubeSize] + " but got " + sum);
}

for ( var i = 20; i <= 160; i *= 2 ) {
  init(i);
}

Q = null;
Q_props = null;
MTrans = null;
MQube = null;
I = null;
Origin = null;
Testing = null;
LoopTime = null;
DisplArea = null;

