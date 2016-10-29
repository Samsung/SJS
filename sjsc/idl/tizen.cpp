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
// XXX Next line a workaround for gcc4.{4,5} C++11 bug: https://llvm.org/bugs/show_bug.cgi?id=13364
namespace std { struct type_info; }
#include "runtime.h"
#include "linkage.h"
#include "calc.h"
#include <FBase.h>

#include <FUi.h>

#include <FAppAppRegistry.h>

#include <FAppApplication.h>

using namespace Tizen::Base;

using namespace Tizen::App;

using namespace Tizen::Ui;

using namespace Tizen::Ui::Controls;
typedef struct { env_t env; void* func; } _genclosure_t;

object_t* __init_wrap_Control(object_t* self, Control* o);
extern int __link_vtbl_Control[];
object_t* __init_wrap_String(object_t* self, String* o);
extern int __link_vtbl_String[];
value_t __String__IndexOf(env_t env, value_t _self, value_t _arg0, value_t _arg1);
void __String__Clear(env_t env, value_t _self);
value_t __String__Contains(env_t env, value_t _self, value_t _arg0);
value_t __String__SubString(env_t env, value_t _self, value_t _arg0);
value_t __String__IsEmpty(env_t env, value_t _self);
value_t __String__CompareTo(env_t env, value_t _self, value_t _arg0);
value_t __String__GetPointer(env_t env, value_t _self);
value_t __String__Append(env_t env, value_t _self, value_t _arg0);
object_t* __init_wrap_Form(object_t* self, Form* o);
extern int __link_vtbl_Form[];
value_t __Form__GetControl(env_t env, value_t _self, value_t _arg0);
void __Form__Draw(env_t env, value_t _self);
value_t __Form__OnTerminating(env_t env, value_t _self);
value_t __Form__GetHeader(env_t env, value_t _self);
value_t __Form__OnDraw(env_t env, value_t _self);
void __Form__SetFormBackEventListener(env_t env, value_t _self, value_t _arg0);
value_t __Form__Construct(env_t env, value_t _self, value_t _arg0);
value_t __Form__OnInitializing(env_t env, value_t _self);
object_t* __init_wrap_Application(object_t* self, Application* o);
extern int __link_vtbl_Application[];
value_t __Application__OnAppTerminating(env_t env, value_t _self, value_t _arg0, value_t _arg1);
value_t __Application__AddFrame(env_t env, value_t _self, value_t _arg0);
value_t __Application__OnAppInitializing(env_t env, value_t _self, value_t _arg0);
void __Application__Terminate(env_t env, value_t _self);
object_t* __init_wrap_Button(object_t* self, Button* o);
extern int __link_vtbl_Button[];
void __Button__SetText(env_t env, value_t _self, value_t _arg0);
void __Button__SetActionId(env_t env, value_t _self, value_t _arg0);
void __Button__AddActionEventListener(env_t env, value_t _self, value_t _arg0);
object_t* __init_wrap_IFormBackEventListener(object_t* self, IFormBackEventListener* o);
extern int __link_vtbl_IFormBackEventListener[];
void __IFormBackEventListener__OnFormBackRequested(env_t env, value_t _self, value_t _arg0);
object_t* __init_wrap_IActionEventListener(object_t* self, IActionEventListener* o);
extern int __link_vtbl_IActionEventListener[];
void __IActionEventListener__OnActionPerformed(env_t env, value_t _self, value_t _arg0, value_t _arg1);
object_t* __init_wrap_AppRegistry(object_t* self, AppRegistry* o);
extern int __link_vtbl_AppRegistry[];
object_t* __init_wrap_Header(object_t* self, Header* o);
extern int __link_vtbl_Header[];
value_t __Header__SetTitleText(env_t env, value_t _self, value_t _arg0);
value_t __Header__SetStyle(env_t env, value_t _self, value_t _arg0);
object_t* __init_wrap_Label(object_t* self, Label* o);
extern int __link_vtbl_Label[];
void __Label__SetTextHorizontalAlignment(env_t env, value_t _self, value_t _arg0);
void __Label__SetText(env_t env, value_t _self, value_t _arg0);
object_t* __init_wrap_Frame(object_t* self, Frame* o);
extern int __link_vtbl_Frame[];
value_t __Frame__SetCurrentForm(env_t env, value_t _self, value_t _arg0);
value_t __Frame__AddControl(env_t env, value_t _self, value_t _arg0);
value_t __Frame__Construct(env_t env, value_t _self);


object_t* __init_wrap_Control(object_t* self, Control* o) {
    assert(__link_vtbl_Control != NULL);
    self->vtbl = (object_map)&__link_vtbl_Control; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    
    return self;
}
value_t __Control_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Control != NULL);
    self->vtbl = (object_map)&__link_vtbl_Control; // Work around linker issue
    

    
    
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Control_clos = { NULL, (void*)__Control_code };
__attribute__((__aligned__(8))) value_t __Control_box = { (void*)&__Control_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Control = &__Control_box;

        class SJSString : public String {
        private:
            object_t* ___sjs_obj;
        public:
            SJSString(object_t* o);
                            int IndexOf(wchar_t*, int);
        void Clear();
        bool Contains(String&);
        int SubString(int);
        bool IsEmpty();
        int CompareTo(String&);
        wchar_t* GetPointer();
        int Append(String&);

        };

        SJSString::SJSString(object_t* o) {
            this->___sjs_obj = o;
        }
int SJSString::IndexOf(wchar_t* arg0, int arg1){
        wprintf(L"invoking SJSString::IndexOf\n");
        return val_as_int(((closure<value_t,value_t, value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_IndexOf)))->invoke(object_as_val(this->___sjs_obj), string_as_val(arg0), int_as_val(arg1)));
}
void SJSString::Clear(){
        wprintf(L"invoking SJSString::Clear\n");
        ((closure<void,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Clear)))->invoke(object_as_val(this->___sjs_obj));
}
bool SJSString::Contains(String& arg0){
        wprintf(L"invoking SJSString::Contains\n");
        return val_as_boolean(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Contains)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)&arg0))));
}
int SJSString::SubString(int arg0){
        wprintf(L"invoking SJSString::SubString\n");
        return val_as_int(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_SubString)))->invoke(object_as_val(this->___sjs_obj), int_as_val(arg0)));
}
bool SJSString::IsEmpty(){
        wprintf(L"invoking SJSString::IsEmpty\n");
        return val_as_boolean(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_IsEmpty)))->invoke(object_as_val(this->___sjs_obj)));
}
int SJSString::CompareTo(String& arg0){
        wprintf(L"invoking SJSString::CompareTo\n");
        return val_as_int(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_CompareTo)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)&arg0))));
}
wchar_t* SJSString::GetPointer(){
        wprintf(L"invoking SJSString::GetPointer\n");
        return val_as_string(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_GetPointer)))->invoke(object_as_val(this->___sjs_obj)));
}
int SJSString::Append(String& arg0){
        wprintf(L"invoking SJSString::Append\n");
        return val_as_int(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Append)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)&arg0))));
}

        void* __sjs_cpp_wrap_String(object_t* o) {
            void* p = new SJSString(o);
            wprintf(L"Wrapped SJS native object %p with SJSString instance at %p\n", o, p);
            return p;
        }
        

value_t __String__IndexOf(env_t env, value_t _self, value_t _arg0, value_t _arg1) {
        object_t* self = val_as_object(_self);
    wchar_t* arg0 = val_as_string(_arg0);
    int arg1 = val_as_int(_arg1);
    int outparam;
    int res = (int)((String*)val_as_object(self)->fields[0].ptr)->IndexOf(arg0, arg1, outparam);
    return int_as_val(outparam);
}
__attribute__((__aligned__(8))) _genclosure_t __String__IndexOf_clos = { NULL, (void*)__String__IndexOf };



void __String__Clear(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    ((String*)val_as_object(self)->fields[0].ptr)->Clear();

}
__attribute__((__aligned__(8))) _genclosure_t __String__Clear_clos = { NULL, (void*)__String__Clear };



value_t __String__Contains(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    bool res = (bool)((String*)val_as_object(self)->fields[0].ptr)->Contains(arg0);
    return boolean_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __String__Contains_clos = { NULL, (void*)__String__Contains };



value_t __String__SubString(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    int arg0 = val_as_int(_arg0);
    String* outparam = new String();
    int res = (int)((String*)val_as_object(self)->fields[0].ptr)->SubString(arg0, *outparam);
    return object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)outparam));
}
__attribute__((__aligned__(8))) _genclosure_t __String__SubString_clos = { NULL, (void*)__String__SubString };



value_t __String__IsEmpty(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    bool res = (bool)((String*)val_as_object(self)->fields[0].ptr)->IsEmpty();
    return boolean_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __String__IsEmpty_clos = { NULL, (void*)__String__IsEmpty };



value_t __String__CompareTo(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((String*)val_as_object(self)->fields[0].ptr)->CompareTo(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __String__CompareTo_clos = { NULL, (void*)__String__CompareTo };



value_t __String__GetPointer(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    wchar_t* res = (wchar_t*)((String*)val_as_object(self)->fields[0].ptr)->GetPointer();
    return string_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __String__GetPointer_clos = { NULL, (void*)__String__GetPointer };



value_t __String__Append(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((String*)val_as_object(self)->fields[0].ptr)->Append(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __String__Append_clos = { NULL, (void*)__String__Append };

object_t* __init_wrap_String(object_t* self, String* o) {
    assert(__link_vtbl_String != NULL);
    self->vtbl = (object_map)&__link_vtbl_String; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_String);

    self->fields[2].ptr = (void*)&__String__IndexOf_clos;
    self->fields[3].ptr = (void*)&__String__Clear_clos;
    self->fields[4].ptr = (void*)&__String__Contains_clos;
    self->fields[5].ptr = (void*)&__String__SubString_clos;
    self->fields[6].ptr = (void*)&__String__IsEmpty_clos;
    self->fields[7].ptr = (void*)&__String__CompareTo_clos;
    self->fields[8].ptr = (void*)&__String__GetPointer_clos;
    self->fields[9].ptr = (void*)&__String__Append_clos;
    return self;
}
value_t __String_code(env_t env, value_t _self , value_t arg0) {
    object_t* self = _self.obj;
    assert(__link_vtbl_String != NULL);
    self->vtbl = (object_map)&__link_vtbl_String; // Work around linker issue
    self->fields[0].ptr = (void*)(new String(val_as_string(arg0)));
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_String);

    self->fields[2].ptr = (void*)&__String__IndexOf_clos;
    self->fields[3].ptr = (void*)&__String__Clear_clos;
    self->fields[4].ptr = (void*)&__String__Contains_clos;
    self->fields[5].ptr = (void*)&__String__SubString_clos;
    self->fields[6].ptr = (void*)&__String__IsEmpty_clos;
    self->fields[7].ptr = (void*)&__String__CompareTo_clos;
    self->fields[8].ptr = (void*)&__String__GetPointer_clos;
    self->fields[9].ptr = (void*)&__String__Append_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __String_clos = { NULL, (void*)__String_code };
__attribute__((__aligned__(8))) value_t __String_box = { (void*)&__String_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __String = &__String_box;

        class SJSForm : public Form {
        private:
            object_t* ___sjs_obj;
        public:
            SJSForm(object_t* o);
                            virtual Control* GetControl(String&);
        virtual void Draw();
        virtual unsigned long OnTerminating();
        virtual Header* GetHeader();
        virtual unsigned long OnDraw();
        void SetFormBackEventListener(IFormBackEventListener*);
        int Construct(String&);
        virtual unsigned long OnInitializing();

        };

        SJSForm::SJSForm(object_t* o) {
            this->___sjs_obj = o;
        }
Control* SJSForm::GetControl(String& arg0){
        wprintf(L"invoking SJSForm::GetControl\n");
        return ((Control*)(val_as_object(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_GetControl)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)&arg0))))->fields[0].ptr));
}
void SJSForm::Draw(){
        wprintf(L"invoking SJSForm::Draw\n");
        ((closure<void,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Draw)))->invoke(object_as_val(this->___sjs_obj));
}
unsigned long SJSForm::OnTerminating(){
        wprintf(L"invoking SJSForm::OnTerminating\n");
        return ((unsigned long)val_as_int(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnTerminating)))->invoke(object_as_val(this->___sjs_obj))));
}
Header* SJSForm::GetHeader(){
        wprintf(L"invoking SJSForm::GetHeader\n");
        return ((Header*)(val_as_object(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_GetHeader)))->invoke(object_as_val(this->___sjs_obj)))->fields[0].ptr));
}
unsigned long SJSForm::OnDraw(){
        wprintf(L"invoking SJSForm::OnDraw\n");
        return ((unsigned long)val_as_int(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnDraw)))->invoke(object_as_val(this->___sjs_obj))));
}
void SJSForm::SetFormBackEventListener(IFormBackEventListener* arg0){
        wprintf(L"invoking SJSForm::SetFormBackEventListener\n");
        ((closure<void,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_SetFormBackEventListener)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_IFormBackEventListener((object_t*)new cppobj<2,0>(), (IFormBackEventListener*)arg0)));
}
int SJSForm::Construct(String& arg0){
        wprintf(L"invoking SJSForm::Construct\n");
        return val_as_int(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Construct)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_String((object_t*)new cppobj<9,0>(), (String*)&arg0))));
}
unsigned long SJSForm::OnInitializing(){
        wprintf(L"invoking SJSForm::OnInitializing\n");
        return ((unsigned long)val_as_int(((closure<value_t,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnInitializing)))->invoke(object_as_val(this->___sjs_obj))));
}

        void* __sjs_cpp_wrap_Form(object_t* o) {
            void* p = new SJSForm(o);
            wprintf(L"Wrapped SJS native object %p with SJSForm instance at %p\n", o, p);
            return p;
        }
        

value_t __Form__GetControl(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    Control* res = (Control*)((Form*)val_as_object(self)->fields[0].ptr)->GetControl(arg0);
    return object_as_val(__init_wrap_Control((object_t*)new cppobj<1,0>(), (Control*)res));
}
__attribute__((__aligned__(8))) _genclosure_t __Form__GetControl_clos = { NULL, (void*)__Form__GetControl };



void __Form__Draw(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    ((Form*)val_as_object(self)->fields[0].ptr)->Draw();

}
__attribute__((__aligned__(8))) _genclosure_t __Form__Draw_clos = { NULL, (void*)__Form__Draw };



value_t __Form__OnTerminating(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    unsigned long res = (unsigned long)((Form*)val_as_object(self)->fields[0].ptr)->OnTerminating();
    return int_as_val((int)res);
}
__attribute__((__aligned__(8))) _genclosure_t __Form__OnTerminating_clos = { NULL, (void*)__Form__OnTerminating };



value_t __Form__GetHeader(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    Header* res = (Header*)((Form*)val_as_object(self)->fields[0].ptr)->GetHeader();
    return object_as_val(__init_wrap_Header((object_t*)new cppobj<3,0>(), (Header*)res));
}
__attribute__((__aligned__(8))) _genclosure_t __Form__GetHeader_clos = { NULL, (void*)__Form__GetHeader };



value_t __Form__OnDraw(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    unsigned long res = (unsigned long)((Form*)val_as_object(self)->fields[0].ptr)->OnDraw();
    return int_as_val((int)res);
}
__attribute__((__aligned__(8))) _genclosure_t __Form__OnDraw_clos = { NULL, (void*)__Form__OnDraw };



void __Form__SetFormBackEventListener(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    IFormBackEventListener* arg0 = ((IFormBackEventListener*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<IFormBackEventListener*>((IFormBackEventListener*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((Form*)val_as_object(self)->fields[0].ptr)->SetFormBackEventListener(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Form__SetFormBackEventListener_clos = { NULL, (void*)__Form__SetFormBackEventListener };



value_t __Form__Construct(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((Form*)val_as_object(self)->fields[0].ptr)->Construct(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Form__Construct_clos = { NULL, (void*)__Form__Construct };



value_t __Form__OnInitializing(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    unsigned long res = (unsigned long)((Form*)val_as_object(self)->fields[0].ptr)->OnInitializing();
    return int_as_val((int)res);
}
__attribute__((__aligned__(8))) _genclosure_t __Form__OnInitializing_clos = { NULL, (void*)__Form__OnInitializing };

object_t* __init_wrap_Form(object_t* self, Form* o) {
    assert(__link_vtbl_Form != NULL);
    self->vtbl = (object_map)&__link_vtbl_Form; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_Form);

    self->fields[2].ptr = (void*)&__Form__GetControl_clos;
    self->fields[3].ptr = (void*)&__Form__Draw_clos;
    self->fields[4].ptr = (void*)&__Form__OnTerminating_clos;
    self->fields[5].ptr = (void*)&__Form__GetHeader_clos;
    self->fields[6].ptr = (void*)&__Form__OnDraw_clos;
    self->fields[7].ptr = (void*)&__Form__SetFormBackEventListener_clos;
    self->fields[8].ptr = (void*)&__Form__Construct_clos;
    self->fields[9].ptr = (void*)&__Form__OnInitializing_clos;
    return self;
}
value_t __Form_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Form != NULL);
    self->vtbl = (object_map)&__link_vtbl_Form; // Work around linker issue
    self->fields[0].ptr = (void*)(new SJSForm(self));
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_Form);

    self->fields[2].ptr = (void*)&__Form__GetControl_clos;
    self->fields[3].ptr = (void*)&__Form__Draw_clos;
    self->fields[4].ptr = (void*)&__Form__OnTerminating_clos;
    self->fields[5].ptr = (void*)&__Form__GetHeader_clos;
    self->fields[6].ptr = (void*)&__Form__OnDraw_clos;
    self->fields[7].ptr = (void*)&__Form__SetFormBackEventListener_clos;
    self->fields[8].ptr = (void*)&__Form__Construct_clos;
    self->fields[9].ptr = (void*)&__Form__OnInitializing_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Form_clos = { NULL, (void*)__Form_code };
__attribute__((__aligned__(8))) value_t __Form_box = { (void*)&__Form_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Form = &__Form_box;

        class SJSApplication : public Application {
        private:
            object_t* ___sjs_obj;
        public:
            SJSApplication(object_t* o);
                            virtual bool OnAppTerminating(AppRegistry&, bool);
        virtual int AddFrame(Frame&);
        virtual bool OnAppInitializing(AppRegistry&);
        virtual void Terminate();

        };

        SJSApplication::SJSApplication(object_t* o) {
            this->___sjs_obj = o;
        }
bool SJSApplication::OnAppTerminating(AppRegistry& arg0, bool arg1){
        wprintf(L"invoking SJSApplication::OnAppTerminating\n");
        return val_as_boolean(((closure<value_t,value_t, value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnAppTerminating)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_AppRegistry((object_t*)new cppobj<1,0>(), (AppRegistry*)&arg0)), boolean_as_val(arg1)));
}
int SJSApplication::AddFrame(Frame& arg0){
        wprintf(L"invoking SJSApplication::AddFrame\n");
        return val_as_int(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_AddFrame)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_Frame((object_t*)new cppobj<4,0>(), (Frame*)&arg0))));
}
bool SJSApplication::OnAppInitializing(AppRegistry& arg0){
        wprintf(L"invoking SJSApplication::OnAppInitializing\n");
        return val_as_boolean(((closure<value_t,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnAppInitializing)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_AppRegistry((object_t*)new cppobj<1,0>(), (AppRegistry*)&arg0))));
}
void SJSApplication::Terminate(){
        wprintf(L"invoking SJSApplication::Terminate\n");
        ((closure<void,value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_Terminate)))->invoke(object_as_val(this->___sjs_obj));
}

        void* __sjs_cpp_wrap_Application(object_t* o) {
            void* p = new SJSApplication(o);
            wprintf(L"Wrapped SJS native object %p with SJSApplication instance at %p\n", o, p);
            return p;
        }
        

value_t __Application__OnAppTerminating(env_t env, value_t _self, value_t _arg0, value_t _arg1) {
        object_t* self = val_as_object(_self);
    AppRegistry& arg0 = *((AppRegistry*)(val_as_object(_arg0)->fields[0].ptr));
    bool arg1 = val_as_boolean(_arg1);
    assert(dynamic_cast<AppRegistry*>((AppRegistry*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    bool res = (bool)((Application*)val_as_object(self)->fields[0].ptr)->OnAppTerminating(arg0, arg1);
    return boolean_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Application__OnAppTerminating_clos = { NULL, (void*)__Application__OnAppTerminating };



value_t __Application__AddFrame(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    Frame& arg0 = *((Frame*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<Frame*>((Frame*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((Application*)val_as_object(self)->fields[0].ptr)->AddFrame(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Application__AddFrame_clos = { NULL, (void*)__Application__AddFrame };



value_t __Application__OnAppInitializing(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    AppRegistry& arg0 = *((AppRegistry*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<AppRegistry*>((AppRegistry*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    bool res = (bool)((Application*)val_as_object(self)->fields[0].ptr)->OnAppInitializing(arg0);
    return boolean_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Application__OnAppInitializing_clos = { NULL, (void*)__Application__OnAppInitializing };



void __Application__Terminate(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    ((Application*)val_as_object(self)->fields[0].ptr)->Terminate();

}
__attribute__((__aligned__(8))) _genclosure_t __Application__Terminate_clos = { NULL, (void*)__Application__Terminate };

object_t* __init_wrap_Application(object_t* self, Application* o) {
    assert(__link_vtbl_Application != NULL);
    self->vtbl = (object_map)&__link_vtbl_Application; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_Application);

    self->fields[2].ptr = (void*)&__Application__OnAppTerminating_clos;
    self->fields[3].ptr = (void*)&__Application__AddFrame_clos;
    self->fields[4].ptr = (void*)&__Application__OnAppInitializing_clos;
    self->fields[5].ptr = (void*)&__Application__Terminate_clos;
    return self;
}
value_t __Application_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Application != NULL);
    self->vtbl = (object_map)&__link_vtbl_Application; // Work around linker issue
    self->fields[0].ptr = (void*)(new SJSApplication(self));
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_Application);

    self->fields[2].ptr = (void*)&__Application__OnAppTerminating_clos;
    self->fields[3].ptr = (void*)&__Application__AddFrame_clos;
    self->fields[4].ptr = (void*)&__Application__OnAppInitializing_clos;
    self->fields[5].ptr = (void*)&__Application__Terminate_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Application_clos = { NULL, (void*)__Application_code };
__attribute__((__aligned__(8))) value_t __Application_box = { (void*)&__Application_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Application = &__Application_box;

value_t __Button_of_Control_code(env_t env, value_t dummy, value_t o) {
    assert(dynamic_cast<Button*>((Button*)o.obj->fields[0].ptr) != NULL);
    return object_as_val(__init_wrap_Button((object_t*)new cppobj<4,0>(), ((Button*)(val_as_object(o)->fields[0].ptr))));
}
__attribute__((__aligned__(8))) _genclosure_t __Button_of_Control_clos = { NULL, (void*)__Button_of_Control_code };
extern "C" { value_t __Button_of_Control_box = { &__Button_of_Control_clos };
value_t* __Button_of_Control = &__Button_of_Control_box; }

void __Button__SetText(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((Button*)val_as_object(self)->fields[0].ptr)->SetText(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Button__SetText_clos = { NULL, (void*)__Button__SetText };



void __Button__SetActionId(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    int arg0 = val_as_int(_arg0);
    ((Button*)val_as_object(self)->fields[0].ptr)->SetActionId(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Button__SetActionId_clos = { NULL, (void*)__Button__SetActionId };



void __Button__AddActionEventListener(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    IActionEventListener& arg0 = *((IActionEventListener*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<IActionEventListener*>((IActionEventListener*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((Button*)val_as_object(self)->fields[0].ptr)->AddActionEventListener(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Button__AddActionEventListener_clos = { NULL, (void*)__Button__AddActionEventListener };

object_t* __init_wrap_Button(object_t* self, Button* o) {
    assert(__link_vtbl_Button != NULL);
    self->vtbl = (object_map)&__link_vtbl_Button; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    self->fields[1].ptr = (void*)&__Button__SetText_clos;
    self->fields[2].ptr = (void*)&__Button__SetActionId_clos;
    self->fields[3].ptr = (void*)&__Button__AddActionEventListener_clos;
    return self;
}
value_t __Button_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Button != NULL);
    self->vtbl = (object_map)&__link_vtbl_Button; // Work around linker issue
    self->fields[0].ptr = (void*)(new Button());
    
    self->fields[1].ptr = (void*)&__Button__SetText_clos;
    self->fields[2].ptr = (void*)&__Button__SetActionId_clos;
    self->fields[3].ptr = (void*)&__Button__AddActionEventListener_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Button_clos = { NULL, (void*)__Button_code };
__attribute__((__aligned__(8))) value_t __Button_box = { (void*)&__Button_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Button = &__Button_box;

        class SJSIFormBackEventListener : public IFormBackEventListener {
        private:
            object_t* ___sjs_obj;
        public:
            SJSIFormBackEventListener(object_t* o);
                            virtual void OnFormBackRequested(Form&);

        };

        SJSIFormBackEventListener::SJSIFormBackEventListener(object_t* o) {
            this->___sjs_obj = o;
        }
void SJSIFormBackEventListener::OnFormBackRequested(Form& arg0){
        wprintf(L"invoking SJSIFormBackEventListener::OnFormBackRequested\n");
        ((closure<void,value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnFormBackRequested)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_Form((object_t*)new cppobj<9,0>(), (Form*)&arg0)));
}

        void* __sjs_cpp_wrap_IFormBackEventListener(object_t* o) {
            void* p = new SJSIFormBackEventListener(o);
            wprintf(L"Wrapped SJS native object %p with SJSIFormBackEventListener instance at %p\n", o, p);
            return p;
        }
        

void __IFormBackEventListener__OnFormBackRequested(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    Form& arg0 = *((Form*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<Form*>((Form*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((IFormBackEventListener*)val_as_object(self)->fields[0].ptr)->OnFormBackRequested(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __IFormBackEventListener__OnFormBackRequested_clos = { NULL, (void*)__IFormBackEventListener__OnFormBackRequested };

object_t* __init_wrap_IFormBackEventListener(object_t* self, IFormBackEventListener* o) {
    assert(__link_vtbl_IFormBackEventListener != NULL);
    self->vtbl = (object_map)&__link_vtbl_IFormBackEventListener; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_IFormBackEventListener);

    self->fields[2].ptr = (void*)&__IFormBackEventListener__OnFormBackRequested_clos;
    return self;
}
value_t __IFormBackEventListener_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_IFormBackEventListener != NULL);
    self->vtbl = (object_map)&__link_vtbl_IFormBackEventListener; // Work around linker issue
    self->fields[0].ptr = (void*)(new SJSIFormBackEventListener(self));
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_IFormBackEventListener);

    self->fields[2].ptr = (void*)&__IFormBackEventListener__OnFormBackRequested_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __IFormBackEventListener_clos = { NULL, (void*)__IFormBackEventListener_code };
__attribute__((__aligned__(8))) value_t __IFormBackEventListener_box = { (void*)&__IFormBackEventListener_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __IFormBackEventListener = &__IFormBackEventListener_box;

        class SJSIActionEventListener : public IActionEventListener {
        private:
            object_t* ___sjs_obj;
        public:
            SJSIActionEventListener(object_t* o);
                            virtual void OnActionPerformed(const Control&, int);

        };

        SJSIActionEventListener::SJSIActionEventListener(object_t* o) {
            this->___sjs_obj = o;
        }
void SJSIActionEventListener::OnActionPerformed(const Control& arg0, int arg1){
        wprintf(L"invoking SJSIActionEventListener::OnActionPerformed\n");
        ((closure<void,value_t, value_t, value_t>*)val_as_pointer(FIELD_READ(this->___sjs_obj, ___js_OnActionPerformed)))->invoke(object_as_val(this->___sjs_obj), object_as_val(__init_wrap_Control((object_t*)new cppobj<1,0>(), (Control*)&arg0)), int_as_val(arg1));
}

        void* __sjs_cpp_wrap_IActionEventListener(object_t* o) {
            void* p = new SJSIActionEventListener(o);
            wprintf(L"Wrapped SJS native object %p with SJSIActionEventListener instance at %p\n", o, p);
            return p;
        }
        

void __IActionEventListener__OnActionPerformed(env_t env, value_t _self, value_t _arg0, value_t _arg1) {
        object_t* self = val_as_object(_self);
    const Control& arg0 = *((Control*)(val_as_object(_arg0)->fields[0].ptr));
    int arg1 = val_as_int(_arg1);
    assert(dynamic_cast<Control*>((Control*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((IActionEventListener*)val_as_object(self)->fields[0].ptr)->OnActionPerformed(arg0, arg1);

}
__attribute__((__aligned__(8))) _genclosure_t __IActionEventListener__OnActionPerformed_clos = { NULL, (void*)__IActionEventListener__OnActionPerformed };

object_t* __init_wrap_IActionEventListener(object_t* self, IActionEventListener* o) {
    assert(__link_vtbl_IActionEventListener != NULL);
    self->vtbl = (object_map)&__link_vtbl_IActionEventListener; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_IActionEventListener);

    self->fields[2].ptr = (void*)&__IActionEventListener__OnActionPerformed_clos;
    return self;
}
value_t __IActionEventListener_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_IActionEventListener != NULL);
    self->vtbl = (object_map)&__link_vtbl_IActionEventListener; // Work around linker issue
    self->fields[0].ptr = (void*)(new SJSIActionEventListener(self));
    self->fields[1].ptr = reinterpret_cast<void*>(__sjs_cpp_wrap_IActionEventListener);

    self->fields[2].ptr = (void*)&__IActionEventListener__OnActionPerformed_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __IActionEventListener_clos = { NULL, (void*)__IActionEventListener_code };
__attribute__((__aligned__(8))) value_t __IActionEventListener_box = { (void*)&__IActionEventListener_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __IActionEventListener = &__IActionEventListener_box;



object_t* __init_wrap_AppRegistry(object_t* self, AppRegistry* o) {
    assert(__link_vtbl_AppRegistry != NULL);
    self->vtbl = (object_map)&__link_vtbl_AppRegistry; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    
    return self;
}
value_t __AppRegistry_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_AppRegistry != NULL);
    self->vtbl = (object_map)&__link_vtbl_AppRegistry; // Work around linker issue
    

    
    
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __AppRegistry_clos = { NULL, (void*)__AppRegistry_code };
__attribute__((__aligned__(8))) value_t __AppRegistry_box = { (void*)&__AppRegistry_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __AppRegistry = &__AppRegistry_box;



value_t __Header__SetTitleText(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((Header*)val_as_object(self)->fields[0].ptr)->SetTitleText(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Header__SetTitleText_clos = { NULL, (void*)__Header__SetTitleText };



value_t __Header__SetStyle(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    HeaderStyle arg0 = (HeaderStyle)val_as_int(_arg0);
    int res = (int)((Header*)val_as_object(self)->fields[0].ptr)->SetStyle(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Header__SetStyle_clos = { NULL, (void*)__Header__SetStyle };

object_t* __init_wrap_Header(object_t* self, Header* o) {
    assert(__link_vtbl_Header != NULL);
    self->vtbl = (object_map)&__link_vtbl_Header; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    self->fields[1].ptr = (void*)&__Header__SetTitleText_clos;
    self->fields[2].ptr = (void*)&__Header__SetStyle_clos;
    return self;
}
value_t __Header_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Header != NULL);
    self->vtbl = (object_map)&__link_vtbl_Header; // Work around linker issue
    

    
    self->fields[1].ptr = (void*)&__Header__SetTitleText_clos;
    self->fields[2].ptr = (void*)&__Header__SetStyle_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Header_clos = { NULL, (void*)__Header_code };
__attribute__((__aligned__(8))) value_t __Header_box = { (void*)&__Header_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Header = &__Header_box;

value_t __Label_of_Control_code(env_t env, value_t dummy, value_t o) {
    assert(dynamic_cast<Label*>((Label*)o.obj->fields[0].ptr) != NULL);
    return object_as_val(__init_wrap_Label((object_t*)new cppobj<3,0>(), ((Label*)(val_as_object(o)->fields[0].ptr))));
}
__attribute__((__aligned__(8))) _genclosure_t __Label_of_Control_clos = { NULL, (void*)__Label_of_Control_code };
extern "C" { value_t __Label_of_Control_box = { &__Label_of_Control_clos };
value_t* __Label_of_Control = &__Label_of_Control_box; }

void __Label__SetTextHorizontalAlignment(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    HorizontalAlignment arg0 = (HorizontalAlignment)val_as_int(_arg0);
    ((Label*)val_as_object(self)->fields[0].ptr)->SetTextHorizontalAlignment(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Label__SetTextHorizontalAlignment_clos = { NULL, (void*)__Label__SetTextHorizontalAlignment };



void __Label__SetText(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    String& arg0 = *((String*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<String*>((String*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    ((Label*)val_as_object(self)->fields[0].ptr)->SetText(arg0);

}
__attribute__((__aligned__(8))) _genclosure_t __Label__SetText_clos = { NULL, (void*)__Label__SetText };

object_t* __init_wrap_Label(object_t* self, Label* o) {
    assert(__link_vtbl_Label != NULL);
    self->vtbl = (object_map)&__link_vtbl_Label; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    self->fields[1].ptr = (void*)&__Label__SetTextHorizontalAlignment_clos;
    self->fields[2].ptr = (void*)&__Label__SetText_clos;
    return self;
}
value_t __Label_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Label != NULL);
    self->vtbl = (object_map)&__link_vtbl_Label; // Work around linker issue
    self->fields[0].ptr = (void*)(new Label());
    
    self->fields[1].ptr = (void*)&__Label__SetTextHorizontalAlignment_clos;
    self->fields[2].ptr = (void*)&__Label__SetText_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Label_clos = { NULL, (void*)__Label_code };
__attribute__((__aligned__(8))) value_t __Label_box = { (void*)&__Label_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Label = &__Label_box;



value_t __Frame__SetCurrentForm(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    Form* arg0 = ((Form*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<Form*>((Form*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((Frame*)val_as_object(self)->fields[0].ptr)->SetCurrentForm(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Frame__SetCurrentForm_clos = { NULL, (void*)__Frame__SetCurrentForm };



value_t __Frame__AddControl(env_t env, value_t _self, value_t _arg0) {
        object_t* self = val_as_object(_self);
    Form* arg0 = ((Form*)(val_as_object(_arg0)->fields[0].ptr));
    assert(dynamic_cast<Form*>((Form*)val_as_object(_arg0)->fields[0].ptr) != NULL);
    int res = (int)((Frame*)val_as_object(self)->fields[0].ptr)->AddControl(arg0);
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Frame__AddControl_clos = { NULL, (void*)__Frame__AddControl };



value_t __Frame__Construct(env_t env, value_t _self) {
        object_t* self = val_as_object(_self);
    int res = (int)((Frame*)val_as_object(self)->fields[0].ptr)->Construct();
    return int_as_val(res);
}
__attribute__((__aligned__(8))) _genclosure_t __Frame__Construct_clos = { NULL, (void*)__Frame__Construct };

object_t* __init_wrap_Frame(object_t* self, Frame* o) {
    assert(__link_vtbl_Frame != NULL);
    self->vtbl = (object_map)&__link_vtbl_Frame; // Work around linker issue
    self->fields[0].ptr = (void*)o;
    
    self->fields[1].ptr = (void*)&__Frame__SetCurrentForm_clos;
    self->fields[2].ptr = (void*)&__Frame__AddControl_clos;
    self->fields[3].ptr = (void*)&__Frame__Construct_clos;
    return self;
}
value_t __Frame_code(env_t env, value_t _self ) {
    object_t* self = _self.obj;
    assert(__link_vtbl_Frame != NULL);
    self->vtbl = (object_map)&__link_vtbl_Frame; // Work around linker issue
    self->fields[0].ptr = (void*)(new Frame());
    
    self->fields[1].ptr = (void*)&__Frame__SetCurrentForm_clos;
    self->fields[2].ptr = (void*)&__Frame__AddControl_clos;
    self->fields[3].ptr = (void*)&__Frame__Construct_clos;
    return object_as_val(self);
}
__attribute__((__aligned__(8))) _genclosure_t __Frame_clos = { NULL, (void*)__Frame_code };
__attribute__((__aligned__(8))) value_t __Frame_box = { (void*)&__Frame_clos }; // NOTE: inits first member of union, .ptr
__attribute__((__aligned__(8))) value_t* __Frame = &__Frame_box;
object_t* __platform_return_val = NULL;

    void __platform_return_code(env_t env, value_t dummy, value_t val) {
        __platform_return_val = val.obj;
    }
    __attribute__((__aligned__(8))) _genclosure_t __platform_return_clos = { NULL, (void*)__platform_return_code };
    __attribute__((__aligned__(8))) value_t __platform_return_box = { (void*)&__platform_return_clos };
    __attribute__((__aligned__(8))) value_t* __platform_return = &__platform_return_box;

cppobj<2,0> __cast_obj = { .vtbl = (object_map)&__cast_obj_vtbl, .__proto__ = NULL, .fields = {(void*)&__Button_of_Control_clos, (void*)&__Label_of_Control_clos} };
cppobj<12,0> __TizenLib = { .vtbl = (object_map)&__TizenLib_vtbl, .__proto__ = NULL, .fields = {(void*)&__Control_clos, (void*)&__String_clos, (void*)&__Form_clos, (void*)&__Application_clos, (void*)&__Button_clos, (void*)&__IFormBackEventListener_clos, (void*)&__IActionEventListener_clos, (void*)&__AppRegistry_clos, (void*)&__Header_clos, (void*)&__Label_clos, (void*)&__Frame_clos, (void*)&__cast_obj} };
__attribute__((__aligned__(8))) value_t __TizenLib_box = { (void*)&__TizenLib };
__attribute__((__aligned__(8))) value_t* TizenLib = & __TizenLib_box;


    using namespace std;
#include<iostream>
extern "C" {
extern "C" int __sjs_main(int);
_EXPORT_ int OspMain(int argc, char* pArgv[]);

Application* ___get_app() {
    return (Application*)__platform_return_val->fields[0].ptr;
}
    
int OspMain(int argc, char* pArgv[]) {

	AppLog("asdf");
    __sjs_main(argc);

    Tizen::Base::Collection::ArrayList args;
	args.Construct();
	for (int i = 0; i < argc; i++)
	{
		args.Add(*(new (std::nothrow) String(pArgv[i])));
	}
    Application* app = (Application*)__platform_return_val->fields[0].ptr;
    wprintf(L"Passing Application* %p to Application::Execute...\n", app);
    unsigned long res = Tizen::App::Application::Execute(___get_app, &args);
    cout << res << endl;
    wprintf(L"Application::Execute(..) --> %d\n", res);
    fflush(stdout);
    return 0;
}

} // extern C
