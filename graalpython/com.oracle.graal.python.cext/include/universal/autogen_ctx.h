/* MIT License
 *  
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. 
 * Copyright (c) 2019 pyhandle
 *  
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *  
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/*
   DO NOT EDIT THIS FILE!

   This file is automatically generated by tools/autogen.py from tools/public_api.h.
   Run this to regenerate:
       make autogen

*/

typedef HPy* _HPyPtr;
typedef HPy _HPyConst;

#define HPy void*
#define HPyListBuilder void*
#define HPyTupleBuilder void*


struct _HPyContext_s {
    int ctx_version;
    _HPyConst h_None;
    _HPyConst h_True;
    _HPyConst h_False;
    _HPyConst h_ValueError;
    _HPyConst h_TypeError;
    _HPyConst h_BaseObjectType;
    _HPyConst h_TypeType;
    _HPyConst h_LongType;
    _HPyConst h_UnicodeType;
    _HPyConst h_TupleType;
    _HPyConst h_ListType;
    HPy (*ctx_Module_Create)(HPyContext ctx, HPyModuleDef *def);
    HPy (*ctx_Dup)(HPyContext ctx, HPy h);
    void (*ctx_Close)(HPyContext ctx, HPy h);
    HPy (*ctx_Long_FromLong)(HPyContext ctx, long value);
    HPy (*ctx_Long_FromUnsignedLong)(HPyContext ctx, unsigned long value);
    HPy (*ctx_Long_FromLongLong)(HPyContext ctx, long long v);
    HPy (*ctx_Long_FromUnsignedLongLong)(HPyContext ctx, unsigned long long v);
    HPy (*ctx_Long_FromSize_t)(HPyContext ctx, size_t value);
    HPy (*ctx_Long_FromSsize_t)(HPyContext ctx, HPy_ssize_t value);
    long (*ctx_Long_AsLong)(HPyContext ctx, HPy h);
    HPy (*ctx_Float_FromDouble)(HPyContext ctx, double v);
    double (*ctx_Float_AsDouble)(HPyContext ctx, HPy h);
    HPy_ssize_t (*ctx_Length)(HPyContext ctx, HPy h);
    int (*ctx_Number_Check)(HPyContext ctx, HPy h);
    HPy (*ctx_Add)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Subtract)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Multiply)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_MatrixMultiply)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_FloorDivide)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_TrueDivide)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Remainder)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Divmod)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Power)(HPyContext ctx, HPy h1, HPy h2, HPy h3);
    HPy (*ctx_Negative)(HPyContext ctx, HPy h1);
    HPy (*ctx_Positive)(HPyContext ctx, HPy h1);
    HPy (*ctx_Absolute)(HPyContext ctx, HPy h1);
    HPy (*ctx_Invert)(HPyContext ctx, HPy h1);
    HPy (*ctx_Lshift)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Rshift)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_And)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Xor)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Or)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_Index)(HPyContext ctx, HPy h1);
    HPy (*ctx_Long)(HPyContext ctx, HPy h1);
    HPy (*ctx_Float)(HPyContext ctx, HPy h1);
    HPy (*ctx_InPlaceAdd)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceSubtract)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceMultiply)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceMatrixMultiply)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceFloorDivide)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceTrueDivide)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceRemainder)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlacePower)(HPyContext ctx, HPy h1, HPy h2, HPy h3);
    HPy (*ctx_InPlaceLshift)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceRshift)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceAnd)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceXor)(HPyContext ctx, HPy h1, HPy h2);
    HPy (*ctx_InPlaceOr)(HPyContext ctx, HPy h1, HPy h2);
    void (*ctx_Err_SetString)(HPyContext ctx, HPy h_type, const char *message);
    int (*ctx_Err_Occurred)(HPyContext ctx);
    HPy (*ctx_Err_NoMemory)(HPyContext ctx);
    int (*ctx_IsTrue)(HPyContext ctx, HPy h);
    HPy (*ctx_Type_FromSpec)(HPyContext ctx, HPyType_Spec *spec, HPyType_SpecParam *params);
    HPy (*ctx_Type_GenericNew)(HPyContext ctx, HPy type, _HPyPtr args, HPy_ssize_t nargs, HPy kw);
    HPy (*ctx_GetAttr)(HPyContext ctx, HPy obj, HPy name);
    HPy (*ctx_GetAttr_s)(HPyContext ctx, HPy obj, const char *name);
    int (*ctx_HasAttr)(HPyContext ctx, HPy obj, HPy name);
    int (*ctx_HasAttr_s)(HPyContext ctx, HPy obj, const char *name);
    int (*ctx_SetAttr)(HPyContext ctx, HPy obj, HPy name, HPy value);
    int (*ctx_SetAttr_s)(HPyContext ctx, HPy obj, const char *name, HPy value);
    HPy (*ctx_GetItem)(HPyContext ctx, HPy obj, HPy key);
    HPy (*ctx_GetItem_i)(HPyContext ctx, HPy obj, HPy_ssize_t idx);
    HPy (*ctx_GetItem_s)(HPyContext ctx, HPy obj, const char *key);
    int (*ctx_SetItem)(HPyContext ctx, HPy obj, HPy key, HPy value);
    int (*ctx_SetItem_i)(HPyContext ctx, HPy obj, HPy_ssize_t idx, HPy value);
    int (*ctx_SetItem_s)(HPyContext ctx, HPy obj, const char *key, HPy value);
    void *(*ctx_Cast)(HPyContext ctx, HPy h);
    HPy (*ctx_New)(HPyContext ctx, HPy h_type, void **data);
    HPy (*ctx_Repr)(HPyContext ctx, HPy obj);
    HPy (*ctx_Str)(HPyContext ctx, HPy obj);
    HPy (*ctx_ASCII)(HPyContext ctx, HPy obj);
    HPy (*ctx_Bytes)(HPyContext ctx, HPy obj);
    HPy (*ctx_RichCompare)(HPyContext ctx, HPy v, HPy w, int op);
    int (*ctx_RichCompareBool)(HPyContext ctx, HPy v, HPy w, int op);
    HPy_hash_t (*ctx_Hash)(HPyContext ctx, HPy obj);
    int (*ctx_Bytes_Check)(HPyContext ctx, HPy h);
    HPy_ssize_t (*ctx_Bytes_Size)(HPyContext ctx, HPy h);
    HPy_ssize_t (*ctx_Bytes_GET_SIZE)(HPyContext ctx, HPy h);
    char *(*ctx_Bytes_AsString)(HPyContext ctx, HPy h);
    char *(*ctx_Bytes_AS_STRING)(HPyContext ctx, HPy h);
    HPy (*ctx_Unicode_FromString)(HPyContext ctx, const char *utf8);
    int (*ctx_Unicode_Check)(HPyContext ctx, HPy h);
    HPy (*ctx_Unicode_AsUTF8String)(HPyContext ctx, HPy h);
    HPy (*ctx_Unicode_FromWideChar)(HPyContext ctx, const wchar_t *w, HPy_ssize_t size);
    int (*ctx_List_Check)(HPyContext ctx, HPy h);
    HPy (*ctx_List_New)(HPyContext ctx, HPy_ssize_t len);
    int (*ctx_List_Append)(HPyContext ctx, HPy h_list, HPy h_item);
    int (*ctx_Dict_Check)(HPyContext ctx, HPy h);
    HPy (*ctx_Dict_New)(HPyContext ctx);
    int (*ctx_Dict_SetItem)(HPyContext ctx, HPy h_dict, HPy h_key, HPy h_val);
    HPy (*ctx_Dict_GetItem)(HPyContext ctx, HPy h_dict, HPy h_key);
    void (*ctx_FatalError)(HPyContext ctx, const char *message);
    HPy (*ctx_Tuple_FromArray)(HPyContext ctx, _HPyPtr items, HPy_ssize_t n);
    HPy (*ctx_FromPyObject)(HPyContext ctx, cpy_PyObject *obj);
    cpy_PyObject *(*ctx_AsPyObject)(HPyContext ctx, HPy h);
    void (*ctx_CallRealFunctionFromTrampoline)(HPyContext ctx, HPyFunc_Signature sig, void *func, void *args);
    void (*ctx_CallDestroyAndThenDealloc)(HPyContext ctx, void *func, cpy_PyObject *self);
    HPyListBuilder (*ctx_ListBuilder_New)(HPyContext ctx, HPy_ssize_t initial_size);
    void (*ctx_ListBuilder_Set)(HPyContext ctx, HPyListBuilder builder, HPy_ssize_t index, HPy h_item);
    HPy (*ctx_ListBuilder_Build)(HPyContext ctx, HPyListBuilder builder);
    void (*ctx_ListBuilder_Cancel)(HPyContext ctx, HPyListBuilder builder);
    HPyTupleBuilder (*ctx_TupleBuilder_New)(HPyContext ctx, HPy_ssize_t initial_size);
    void (*ctx_TupleBuilder_Set)(HPyContext ctx, HPyTupleBuilder builder, HPy_ssize_t index, HPy h_item);
    HPy (*ctx_TupleBuilder_Build)(HPyContext ctx, HPyTupleBuilder builder);
    void (*ctx_TupleBuilder_Cancel)(HPyContext ctx, HPyTupleBuilder builder);
};

#undef HPy
#undef HPyListBuilder
#undef HPyTupleBuilder
