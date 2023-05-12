/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.builtins.objects.cext.hpy;

import com.oracle.graal.python.builtins.objects.cext.common.LoadCExtException.ApiInitException;
import com.oracle.graal.python.builtins.objects.cext.common.LoadCExtException.ImportException;
import com.oracle.graal.python.builtins.objects.cext.hpy.GraalHPyContext.HPyUpcall;
import com.oracle.graal.python.builtins.objects.module.PythonModule;
import com.oracle.graal.python.nodes.util.CannotCastException;
import com.oracle.graal.python.runtime.PythonContext;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public abstract class GraalHPyNativeContext implements TruffleObject {

    protected final GraalHPyContext context;

    /**
     * This field mirrors value of {@link PythonOptions#HPyEnableJNIFastPaths}. We store it in this
     * final field because the value is also used in non-PE code paths.
     */
    protected final boolean useNativeFastPaths;

    protected GraalHPyNativeContext(GraalHPyContext context, boolean useNativeFastPaths, boolean traceUpcalls) {
        this.context = context;
        this.useNativeFastPaths = useNativeFastPaths;
    }

    protected abstract String getName();

    protected final PythonContext getContext() {
        return context.getContext();
    }

    protected abstract void initNativeContext() throws Exception;

    protected abstract void finalizeNativeContext();

    protected abstract HPyUpcall[] getUpcalls();

    protected abstract int[] getUpcallCounts();

    public abstract long createNativeArguments(Object[] delegate, InteropLibrary lib);

    public abstract void freeNativeArgumentsArray(int nargs);

    public abstract void initHPyDebugContext() throws ApiInitException;

    public abstract PythonModule getHPyDebugModule() throws ImportException;

    public final boolean useNativeCache() {
        return useNativeFastPaths;
    }

    protected abstract void setNativeCache(long cachePtr);

    protected abstract long getWcharSize();

    @ExportMessage
    public void toNative() {
        try {
            toNativeInternal();
            if (useNativeFastPaths) {
                initNativeFastPaths();
                /*
                 * Allocate a native array for the native space pointers of HPy objects and
                 * initialize it.
                 */
                context.allocateNativeSpacePointersMirror();
            }
        } catch (CannotCastException e) {
            /*
             * We should only receive 'toNative' if native access is allowed. Hence, the exception
             * should never happen.
             */
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    protected abstract void toNativeInternal();

    protected abstract void initNativeFastPaths();

}
