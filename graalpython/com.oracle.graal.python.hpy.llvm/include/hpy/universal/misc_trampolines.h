/* MIT License
 *
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates.
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

#ifndef HPY_MISC_TRAMPOLINES_H
#define HPY_MISC_TRAMPOLINES_H

#ifdef GRAALVM_PYTHON_LLVM
#define UNWRAP(_h) ((_h)._i)
#define WRAP(_ptr) ((HPy){(_ptr)})
#else
#define UNWRAP(_h) _h
#define WRAP(_ptr) _ptr
#endif

static inline HPy _HPy_New(HPyContext *ctx, HPy h_type, void **data) {
    /* Performance hack: the autogenerated version of this trampoline would
       simply forward data to ctx_New.

       Suppose you call HPy_New this way:
           PointObject *point;
           HPy h = HPy_New(ctx, cls, &point);

       If you pass "data" to ctx->New, the C compiler must assume that anybody
       could write a different value at any time into this local variable
       because a pointer to it escaped. With this hack, it's no longer the
       case: what escaped is the address of data_result instead and that local
       variable disappears since this function is likely inlined.

       See https://github.com/pyhandle/hpy/pull/22#pullrequestreview-413365845
    */
    void *data_result;
    HPy h = WRAP(ctx->ctx_New(ctx, UNWRAP(h_type), &data_result));
    *data = data_result;
    return h;
}

static inline _HPy_NO_RETURN void
HPy_FatalError(HPyContext *ctx, const char *message) {
    ctx->ctx_FatalError(ctx, message);
    /* note: the following abort() is unreachable, but needed because the
       _HPy_NO_RETURN doesn't seem to be sufficient.  I think that what
       occurs is that this function is inlined, after which gcc forgets
       that it couldn't return.  Having abort() inlined fixes that. */
    abort();
}

static inline void *
HPyCapsule_GetPointer(HPyContext *ctx, HPy capsule, const char *name)
{
    return ctx->ctx_Capsule_Get(
            ctx, UNWRAP(capsule), HPyCapsule_key_Pointer, name);
}

static inline const char *
HPyCapsule_GetName(HPyContext *ctx, HPy capsule)
{
    return (const char *) ctx->ctx_Capsule_Get(
            ctx, UNWRAP(capsule), HPyCapsule_key_Name, NULL);
}

static inline void *
HPyCapsule_GetContext(HPyContext *ctx, HPy capsule)
{
    return ctx->ctx_Capsule_Get(
            ctx, UNWRAP(capsule), HPyCapsule_key_Context, NULL);
}

static inline HPyCapsule_Destructor
HPyCapsule_GetDestructor(HPyContext *ctx, HPy capsule)
{
    return (HPyCapsule_Destructor) ctx->ctx_Capsule_Get(
            ctx, UNWRAP(capsule), HPyCapsule_key_Destructor, NULL);
}

static inline int
HPyCapsule_SetPointer(HPyContext *ctx, HPy capsule, void *pointer)
{
    return ctx->ctx_Capsule_Set(
            ctx, UNWRAP(capsule), HPyCapsule_key_Pointer, pointer);
}

static inline int
HPyCapsule_SetName(HPyContext *ctx, HPy capsule, const char *name)
{
    return ctx->ctx_Capsule_Set(
            ctx, UNWRAP(capsule), HPyCapsule_key_Name, (void *) name);
}

static inline int
HPyCapsule_SetContext(HPyContext *ctx, HPy capsule, void *context)
{
    return ctx->ctx_Capsule_Set(
            ctx, UNWRAP(capsule), HPyCapsule_key_Context, context);
}

static inline int
HPyCapsule_SetDestructor(HPyContext *ctx, HPy capsule,
        HPyCapsule_Destructor destructor)
{
    return ctx->ctx_Capsule_Set(
            ctx, UNWRAP(capsule), HPyCapsule_key_Destructor, (void *) destructor);
}

#undef UNWRAP
#undef WRAP

#endif /* HPY_MISC_TRAMPOLINES_H */
