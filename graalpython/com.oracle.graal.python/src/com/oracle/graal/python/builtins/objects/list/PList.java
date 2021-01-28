/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 * Copyright (c) 2013, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.builtins.objects.list;

import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.common.IndexNodes;
import com.oracle.graal.python.builtins.objects.common.SequenceStorageNodes;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.object.PythonObjectLibrary;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.classes.IsSubtypeNode;
import com.oracle.graal.python.nodes.interop.PForeignToPTypeNode;
import com.oracle.graal.python.nodes.literal.ListLiteralNode;
import com.oracle.graal.python.runtime.GilNode;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.sequence.PSequence;
import com.oracle.graal.python.runtime.sequence.storage.SequenceStorage;
import com.oracle.graal.python.util.OverflowException;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;

@ExportLibrary(InteropLibrary.class)
@ExportLibrary(PythonObjectLibrary.class)
public final class PList extends PSequence {
    private final ListLiteralNode origin;
    private SequenceStorage store;

    public PList(Object cls, Shape instanceShape, SequenceStorage store) {
        super(cls, instanceShape);
        this.origin = null;
        this.store = store;
    }

    public PList(Object cls, Shape instanceShape, SequenceStorage store, ListLiteralNode origin) {
        super(cls, instanceShape);
        this.origin = origin;
        this.store = store;
    }

    @Override
    public final SequenceStorage getSequenceStorage() {
        return store;
    }

    @Override
    public final void setSequenceStorage(SequenceStorage newStorage) {
        this.store = newStorage;
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        StringBuilder buf = new StringBuilder("[");

        for (int i = 0; i < store.length(); i++) {
            Object item = store.getItemNormalized(i);
            buf.append(item.toString());

            if (i < store.length() - 1) {
                buf.append(", ");
            }
        }

        buf.append("]");
        return buf.toString();
    }

    public final void reverse() {
        store.reverse();
    }

    @Ignore
    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof PList)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        PList otherList = (PList) other;
        SequenceStorage otherStore = otherList.getSequenceStorage();
        return store.equals(otherStore);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    public ListLiteralNode getOrigin() {
        return origin;
    }

    public static PList require(Object value) {
        if (value instanceof PList) {
            return (PList) value;
        }
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException("PList required.");
    }

    public static PList expect(Object value) throws UnexpectedResultException {
        if (value instanceof PList) {
            return (PList) value;
        }
        throw new UnexpectedResultException(value);
    }

    @ExportMessage
    public SourceSection getSourceLocation(@Exclusive @Cached GilNode gil) throws UnsupportedMessageException {
        boolean mustRelease = gil.acquire();
        try {
            ListLiteralNode node = getOrigin();
            SourceSection result = null;
            if (node != null) {
                result = node.getSourceSection();
            }
            if (result == null) {
                throw UnsupportedMessageException.create();
            }
            return result;
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public boolean hasSourceLocation() {
        return getOrigin() != null && getOrigin().getSourceSection() != null;
    }

    @ExportMessage
    public boolean isArrayElementModifiable(long index,
                    @Cached.Exclusive @Cached SequenceStorageNodes.LenNode lenNode,
                    @Cached.Exclusive @Cached IndexNodes.NormalizeIndexCustomMessageNode normalize,
                    @Exclusive @Cached GilNode gil) {
        boolean mustRelease = gil.acquire();
        try {
            final int len = lenNode.execute(store);
            try {
                normalize.execute(index, len, ErrorMessages.INDEX_OUT_OF_RANGE);
            } catch (PException e) {
                return false;
            }
            return true;
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public boolean isArrayElementInsertable(long index,
                    @Cached.Exclusive @Cached SequenceStorageNodes.LenNode lenNode,
                    @Exclusive @Cached GilNode gil) {
        boolean mustRelease = gil.acquire();
        try {
            final int len = lenNode.execute(store);
            return index == len;
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public boolean isArrayElementRemovable(long index,
                    @Cached.Exclusive @Cached SequenceStorageNodes.LenNode lenNode,
                    @Cached.Exclusive @Cached IndexNodes.NormalizeIndexCustomMessageNode normalize,
                    @Exclusive @Cached GilNode gil) {
        boolean mustRelease = gil.acquire();
        try {
            final int len = lenNode.execute(store);
            try {
                normalize.execute(index, len, ErrorMessages.INDEX_OUT_OF_RANGE);
            } catch (PException e) {
                return false;
            }
            return true;
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value,
                    @Cached PForeignToPTypeNode convert,
                    @Cached.Exclusive @Cached SequenceStorageNodes.SetItemScalarNode setItem,
                    @Exclusive @Cached GilNode gil) throws InvalidArrayIndexException {
        boolean mustRelease = gil.acquire();
        try {
            try {
                setItem.execute(store, PInt.intValueExact(index), convert.executeConvert(value));
            } catch (OverflowException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw InvalidArrayIndexException.create(index);
            }
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public void removeArrayElement(long index,
                    @Cached.Exclusive @Cached SequenceStorageNodes.DeleteItemNode delItem,
                    @Exclusive @Cached GilNode gil) throws InvalidArrayIndexException {
        boolean mustRelease = gil.acquire();
        try {
            try {
                delItem.execute(store, PInt.intValueExact(index));
            } catch (OverflowException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw InvalidArrayIndexException.create(index);
            }
        } finally {
            gil.release(mustRelease);
        }
    }

    @ExportMessage
    public static boolean typeCheck(PList receiver, Object type,
                    @Cached IsSubtypeNode isSubtypeNode,
                    @Cached ConditionProfile pListProfile,
                    @CachedLibrary("receiver") PythonObjectLibrary pol) {
        if (pListProfile.profile(type == PythonBuiltinClassType.PList)) {
            return true;
        }
        return isSubtypeNode.execute(pol.getLazyPythonClass(receiver), type);
    }
}
