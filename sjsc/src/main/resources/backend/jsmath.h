/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __JSMATH__
#define __JSMATH__

extern value_t* Math;

// Intrinsics

extern wchar_t* __fp__toFixed(double x, int prec);
extern wchar_t* __fp__toString(double x);
extern wchar_t* __fp__toExponential(double x, int prec);

extern wchar_t* __int__toFixed(int x, int prec);
extern wchar_t* __int__toExponential(int x, int prec);
extern wchar_t* __int__toString(int x);

extern wchar_t* (* const string_of_int)(int x);

#endif // __JSMATH__
