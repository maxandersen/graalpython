/* MIT License
 *
 * Copyright (c) 2020, Oracle and/or its affiliates.
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

#ifndef HPY_COMMON_RUNTIME_TUPLEBUILDER_H
#define HPY_COMMON_RUNTIME_TUPLEBUILDER_H

#include <Python.h>
#include "hpy.h"
#include "common/hpytype.h"


_HPy_HIDDEN HPyTupleBuilder ctx_TupleBuilder_New(HPyContext ctx,
                                                 HPy_ssize_t initial_size);
_HPy_HIDDEN void ctx_TupleBuilder_Set(HPyContext ctx, HPyTupleBuilder builder,
                                      HPy_ssize_t index, HPy h_item);
_HPy_HIDDEN HPy ctx_TupleBuilder_Build(HPyContext ctx, HPyTupleBuilder builder);
_HPy_HIDDEN void ctx_TupleBuilder_Cancel(HPyContext ctx,
                                         HPyTupleBuilder builder);


#endif /* HPY_COMMON_RUNTIME_TUPLEBUILDER_H */
