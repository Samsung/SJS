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
/*
 * JS Date implementation
 */

#ifndef __JSDATE__
#define __JSDATE__

#include <sys/time.h>
#include <time.h>


// TODO: split into header
// concatenation of machine & user representations, since we're immutable
typedef struct jsdate {
    struct timeval tv;
    struct tm tm;
} jsdate_t;
#define DATE_N_METHODS 12

#include<gc.h>
#include<runtime.h>
#include<linkage.h>

// Wrapper methods...

value_t _date_getDate(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_mday); // for C++ FFI purposes, we can assume this is ~ a method call
}
UNBOX_NATIVE_METHOD(_date_getDate, _date_getDate);

value_t _date_getDay(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_wday);
}
UNBOX_NATIVE_METHOD(_date_getDay, _date_getDay);

value_t _date_getFullYear(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_year+1900); // convert to 4-digit year
}
UNBOX_NATIVE_METHOD(_date_getFullYear, _date_getFullYear);

value_t _date_getHours(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_hour);
}
UNBOX_NATIVE_METHOD(_date_getHours, _date_getHours);


value_t _date_getMilliseconds(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    // calculate from seconds and microseconds
    return int_as_val(dt->tv.tv_sec * 1000 + dt->tv.tv_usec / 1000);
}
UNBOX_NATIVE_METHOD(_date_getMilliseconds, _date_getMilliseconds);

value_t _date_getMinutes(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_min);
}
UNBOX_NATIVE_METHOD(_date_getMinutes, _date_getMinutes);

value_t _date_getMonth(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_mon);
}
UNBOX_NATIVE_METHOD(_date_getMonth, _date_getMonth);

value_t _date_getSeconds(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_sec);
}
UNBOX_NATIVE_METHOD(_date_getSeconds, _date_getSeconds);

value_t _date_getTime(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tv.tv_sec * 1000 + dt->tv.tv_usec / 1000); // convert microseconds to milliseconds...
}
UNBOX_NATIVE_METHOD(_date_getTime, _date_getTime);


value_t _date_getTimezoneOffset(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tm.tm_gmtoff/60); // convert seconds to minutes...
}
UNBOX_NATIVE_METHOD(_date_getTimezoneOffset, _date_getTimezoneOffset);

value_t _date_valueOf(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    return int_as_val(dt->tv.tv_sec * 1000 + dt->tv.tv_usec / 1000);
}
UNBOX_NATIVE_METHOD(_date_valueOf, _date_valueOf);

// TODO: more wrappers for UTC stuff...
// TODO: setters, conversions like toString()

// buffer size defined in manpage for ctime_r
char ctime_tmp_buf[26];

value_t _date_toDateString(env_t env, value_t self) {
    jsdate_t* dt = (jsdate_t*)self.obj->fields[DATE_N_METHODS].ptr;
    ctime_r(&dt->tv.tv_sec, ctime_tmp_buf);
    // TODO: technically ctime_r (and asciitime) give date strings different than the JS spec for
    // Date.prototype.toDateString()
    ctime_tmp_buf[24] = '\0'; // drop the newline that ctime adds
    wchar_t* wcs = (wchar_t*)MEM_ALLOC_ATOMIC(27*sizeof(wchar_t));
    mbstowcs(wcs, ctime_tmp_buf, 27);
    return (value_t)wcs; // TODO: string tagging via string_as_val
}
UNBOX_NATIVE_METHOD(_date_toDateString, _date_toDateString);

extern int date_table[];
value_t _ctor_Date(env_t env, value_t self) {
    // The self.obj pointer is pre-allocated, and set with the correct vtable already
    jsdate_t* dt = MEM_ALLOC(sizeof(jsdate_t));
    int ret = 0;
    ret = gettimeofday(&dt->tv, NULL);
    assert(ret == 0);
    struct tm *tmp = localtime(&dt->tv.tv_sec);
    assert (tmp != NULL);
    dt->tm.tm_sec  = tmp->tm_sec  ;
    dt->tm.tm_min  = tmp->tm_min  ;
    dt->tm.tm_hour = tmp->tm_hour ;
    dt->tm.tm_mday = tmp->tm_mday ;
    dt->tm.tm_mon  = tmp->tm_mon  ;
    dt->tm.tm_year = tmp->tm_year ;
    dt->tm.tm_wday = tmp->tm_wday ;
    dt->tm.tm_yday = tmp->tm_yday ;
    dt->tm.tm_isdst= tmp->tm_isdst;
    dt->tm.tm_zone = tmp->tm_zone ;
    dt->tm.tm_gmtoff= tmp->tm_gmtoff;

    int nextfield = 0;
    self.obj->vtbl = date_table;
    self.obj->fields[nextfield++].ptr = &_date_getDate_clos;
    self.obj->fields[nextfield++].ptr = &_date_getDay_clos;
    self.obj->fields[nextfield++].ptr = &_date_getFullYear_clos;
    self.obj->fields[nextfield++].ptr = &_date_getHours_clos;
    self.obj->fields[nextfield++].ptr = &_date_getMilliseconds_clos;
    self.obj->fields[nextfield++].ptr = &_date_getMinutes_clos;
    self.obj->fields[nextfield++].ptr = &_date_getMonth_clos;
    self.obj->fields[nextfield++].ptr = &_date_getSeconds_clos;
    self.obj->fields[nextfield++].ptr = &_date_getTime_clos;
    self.obj->fields[nextfield++].ptr = &_date_getTimezoneOffset_clos;
    self.obj->fields[nextfield++].ptr = &_date_toDateString_clos;
    self.obj->fields[nextfield++].ptr = &_date_valueOf_clos;
    self.obj->fields[nextfield].ptr = (void*)dt;
    assert (nextfield == DATE_N_METHODS);
    return (value_t)self.obj; // required in all constructors
}
NEW_BOX_NATIVE_CTOR(_ctor_Date, _ctor_Date);

value_t* Date = &_ctor_Date_box;


#endif // __JSDATE__
