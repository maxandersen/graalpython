/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.modules.hashlib;

import java.security.MessageDigest;
import java.util.List;

import com.oracle.graal.python.annotations.ArgumentClinic;
import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.modules.hashlib.DigestObjectBuiltinsClinicProviders.UpdateNodeClinicProviderGen;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.buffer.PythonBufferAccessLibrary;
import com.oracle.graal.python.builtins.objects.bytes.BytesNodes;
import com.oracle.graal.python.builtins.objects.bytes.PBytes;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.clinic.ArgumentClinicProvider;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;

@CoreFunctions(extendClasses = {PythonBuiltinClassType.SHA1Type, PythonBuiltinClassType.SHA224Type, PythonBuiltinClassType.SHA256Type, PythonBuiltinClassType.SHA384Type, PythonBuiltinClassType.SHA512Type})
public class DigestObjectBuiltins extends PythonBuiltins {
    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return DigestObjectBuiltinsFactory.getFactories();
    }

    @Builtin(name = "copy", parameterNames = {"self"})
    @GenerateNodeFactory
    abstract static class CopyNode extends PythonUnaryBuiltinNode {
        DigestObject copy(DigestObject self) {
            try {
                return factory().trace(new DigestObject(self.getType(), op(self)));
            } catch (CloneNotSupportedException e) {
                throw raise(PythonBuiltinClassType.ValueError);
            }
        }

        @TruffleBoundary
        static MessageDigest op(DigestObject self) throws CloneNotSupportedException {
            return (MessageDigest) self.getDigest().clone();
        }
    }

    @Builtin(name = "digest", parameterNames = {"self"})
    @GenerateNodeFactory
    abstract static class DigestNode extends PythonUnaryBuiltinNode {
        PBytes digest(DigestObject self) {
            byte[] digest = op(self);
            return factory().createBytes(digest);
        }

        @TruffleBoundary
        static byte[] op(DigestObject self) {
            return self.getDigest().digest();
        }
    }

    @Builtin(name = "hexdigest", parameterNames = {"self"})
    @GenerateNodeFactory
    abstract static class HexdigestNode extends PythonUnaryBuiltinNode {
        TruffleString hexdigest(DigestObject self,
                        @Cached BytesNodes.ByteToHexNode toHexNode) {
            byte[] digest = op(self);
            return toHexNode.execute(digest, digest.length, (byte) 0, 0);
        }

        @TruffleBoundary
        static byte[] op(DigestObject self) {
            return self.getDigest().digest();
        }
    }

    @Builtin(name = "update", parameterNames = {"self", "obj"})
    @ArgumentClinic(name = "obj", conversion = ArgumentClinic.ClinicConversion.ReadableBuffer)
    @GenerateNodeFactory
    abstract static class UpdateNode extends PythonBinaryClinicBuiltinNode {
        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return UpdateNodeClinicProviderGen.INSTANCE;
        }

        PNone update(VirtualFrame frame, DigestObject self, Object buffer,
                        @CachedLibrary("buffer") PythonBufferAccessLibrary bufferLib) {
            try {
                update(self, bufferLib.getInternalOrCopiedByteArray(buffer));
            } finally {
                bufferLib.release(buffer, frame, this);
            }
            return PNone.NONE;
        }

        @TruffleBoundary
        private static void update(DigestObject self, byte[] buffer) {
            self.getDigest().update(buffer);
        }
    }

    @Builtin(name = "block_size", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    abstract static class BlockSizeNode extends PythonUnaryBuiltinNode {
        @TruffleBoundary
        @Specialization
        static int get(DigestObject self) {
            switch (self.getDigest().getAlgorithm()) {
                case "sha1":
                case "sha224":
                case "sha256":
                    return 64;
                case "sha384":
                case "sha512":
                    return 128;
                default: return -1;
            }
        }
    }

    @Builtin(name = "digest_size", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    abstract static class DigestSizeNode extends PythonUnaryBuiltinNode {
        @TruffleBoundary
        @Specialization
        static int get(DigestObject self) {
            return self.getDigest().getDigestLength();
        }
    }

    @Builtin(name = "name", minNumOfPositionalArgs = 1,isGetter = true)
    @GenerateNodeFactory
    abstract static class NameNode extends PythonUnaryBuiltinNode {
        @TruffleBoundary
        @Specialization
        TruffleString get(DigestObject self) {
            return PythonUtils.toTruffleStringUncached(self.getDigest().getAlgorithm());
        }
    }
}
