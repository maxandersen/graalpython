/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.nodes;

import static com.oracle.graal.python.builtins.PythonBuiltinClassType.KeyError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.StopIteration;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.SystemError;
import static com.oracle.graal.python.compiler.OpCodesConstants.*;
import static com.oracle.graal.python.nodes.BuiltinNames.__BUILD_CLASS__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__ANNOTATIONS__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__CLASS__;

import java.math.BigInteger;
import java.util.Arrays;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.cell.PCell;
import com.oracle.graal.python.builtins.objects.code.PCode;
import com.oracle.graal.python.builtins.objects.common.HashingCollectionNodes;
import com.oracle.graal.python.builtins.objects.common.HashingCollectionNodes.SetItemNode;
import com.oracle.graal.python.builtins.objects.common.HashingStorage;
import com.oracle.graal.python.builtins.objects.common.HashingStorageLibrary;
import com.oracle.graal.python.builtins.objects.dict.DictNodes;
import com.oracle.graal.python.builtins.objects.dict.PDict;
import com.oracle.graal.python.builtins.objects.ellipsis.PEllipsis;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.builtins.objects.function.Signature;
import com.oracle.graal.python.builtins.objects.list.ListBuiltins;
import com.oracle.graal.python.builtins.objects.list.PList;
import com.oracle.graal.python.builtins.objects.module.PythonModule;
import com.oracle.graal.python.builtins.objects.object.PythonObject;
import com.oracle.graal.python.builtins.objects.set.PSet;
import com.oracle.graal.python.builtins.objects.set.SetBuiltins;
import com.oracle.graal.python.builtins.objects.set.SetNodes;
import com.oracle.graal.python.builtins.objects.slice.PSlice;
import com.oracle.graal.python.builtins.objects.slice.SliceNodes;
import com.oracle.graal.python.compiler.BinaryOpsConstants;
import com.oracle.graal.python.compiler.CodeUnit;
import com.oracle.graal.python.compiler.OpCodes;
import com.oracle.graal.python.compiler.OpCodes.CollectionBits;
import com.oracle.graal.python.compiler.UnaryOpsConstants;
import com.oracle.graal.python.lib.PyObjectDelItem;
import com.oracle.graal.python.lib.PyObjectGetAttr;
import com.oracle.graal.python.lib.PyObjectGetIter;
import com.oracle.graal.python.lib.PyObjectGetMethod;
import com.oracle.graal.python.lib.PyObjectGetMethodNodeGen;
import com.oracle.graal.python.lib.PyObjectIsTrueNode;
import com.oracle.graal.python.lib.PyObjectSetAttr;
import com.oracle.graal.python.lib.PyObjectSetItem;
import com.oracle.graal.python.nodes.argument.keywords.ExpandKeywordStarargsNode;
import com.oracle.graal.python.nodes.argument.positional.ExecutePositionalStarargsNode;
import com.oracle.graal.python.nodes.builtins.ListNodes;
import com.oracle.graal.python.nodes.builtins.TupleNodes;
import com.oracle.graal.python.nodes.bytecode.ExitWithNode;
import com.oracle.graal.python.nodes.bytecode.GetNextNode;
import com.oracle.graal.python.nodes.bytecode.ImportFromNode;
import com.oracle.graal.python.nodes.bytecode.SetupWithNode;
import com.oracle.graal.python.nodes.bytecode.UnpackSequenceNode;
import com.oracle.graal.python.nodes.call.CallNode;
import com.oracle.graal.python.nodes.call.special.CallBinaryMethodNode;
import com.oracle.graal.python.nodes.call.special.CallQuaternaryMethodNode;
import com.oracle.graal.python.nodes.call.special.CallTernaryMethodNode;
import com.oracle.graal.python.nodes.call.special.CallUnaryMethodNode;
import com.oracle.graal.python.nodes.expression.BinaryArithmetic;
import com.oracle.graal.python.nodes.expression.BinaryComparisonNode;
import com.oracle.graal.python.nodes.expression.BinaryOp;
import com.oracle.graal.python.nodes.expression.CoerceToBooleanNode;
import com.oracle.graal.python.nodes.expression.ContainsNode;
import com.oracle.graal.python.nodes.expression.InplaceArithmetic;
import com.oracle.graal.python.nodes.expression.UnaryArithmetic.InvertNode;
import com.oracle.graal.python.nodes.expression.UnaryArithmetic.NegNode;
import com.oracle.graal.python.nodes.expression.UnaryArithmetic.PosNode;
import com.oracle.graal.python.nodes.expression.UnaryOpNode;
import com.oracle.graal.python.nodes.frame.DeleteGlobalNode;
import com.oracle.graal.python.nodes.frame.ReadGlobalOrBuiltinNode;
import com.oracle.graal.python.nodes.frame.WriteGlobalNode;
import com.oracle.graal.python.nodes.object.IsBuiltinClassProfile;
import com.oracle.graal.python.nodes.object.IsNode;
import com.oracle.graal.python.nodes.statement.AbstractImportNode.ImportName;
import com.oracle.graal.python.nodes.statement.ExceptNode.ExceptMatchNode;
import com.oracle.graal.python.nodes.statement.ExceptionHandlingStatementNode;
import com.oracle.graal.python.nodes.statement.RaiseNode;
import com.oracle.graal.python.nodes.subscript.DeleteItemNode;
import com.oracle.graal.python.nodes.subscript.GetItemNode;
import com.oracle.graal.python.nodes.util.CastToJavaIntExactNode;
import com.oracle.graal.python.runtime.ExecutionContext.CalleeContext;
import com.oracle.graal.python.runtime.PythonContext;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.object.PythonObjectFactory;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitchBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public final class PBytecodeRootNode extends PRootNode implements BytecodeOSRNode {

    private static final NodeSupplier<RaiseNode> NODE_RAISENODE = () -> RaiseNode.create(null, null);
    private static final NodeSupplier<DeleteItemNode> NODE_DELETE_ITEM = DeleteItemNode::create;
    private static final NodeSupplier<PyObjectDelItem> NODE_OBJECT_DEL_ITEM = PyObjectDelItem::create;
    private static final PyObjectDelItem UNCACHED_OBJECT_DEL_ITEM = PyObjectDelItem.getUncached();

    private static final NodeSupplier<SetItemNode> NODE_SET_ITEM = HashingCollectionNodes.SetItemNode::create;
    private static final SetItemNode UNCACHED_SET_ITEM = HashingCollectionNodes.SetItemNode.getUncached();
    private static final NodeSupplier<CastToJavaIntExactNode> NODE_CAST_TO_JAVA_INT_EXACT = CastToJavaIntExactNode::create;
    private static final CastToJavaIntExactNode UNCACHED_CAST_TO_JAVA_INT_EXACT = CastToJavaIntExactNode.getUncached();
    private static final NodeSupplier<ImportName> NODE_IMPORT_NAME = ImportName::create;
    private static final ImportName UNCACHED_IMPORT_NAME = ImportName.getUncached();
    private static final NodeSupplier<PyObjectGetAttr> NODE_OBJECT_GET_ATTR = PyObjectGetAttr::create;
    private static final PyObjectGetAttr UNCACHED_OBJECT_GET_ATTR = PyObjectGetAttr.getUncached();
    private static final NodeSupplier<PRaiseNode> NODE_RAISE = PRaiseNode::create;
    private static final PRaiseNode UNCACHED_RAISE = PRaiseNode.getUncached();
    private static final NodeSupplier<IsBuiltinClassProfile> NODE_IS_BUILTIN_CLASS_PROFILE = IsBuiltinClassProfile::create;
    private static final IsBuiltinClassProfile UNCACHED_IS_BUILTIN_CLASS_PROFILE = IsBuiltinClassProfile.getUncached();
    private static final NodeSupplier<CallNode> NODE_CALL = CallNode::create;
    private static final CallNode UNCACHED_CALL = CallNode.getUncached();
    private static final NodeSupplier<CallQuaternaryMethodNode> NODE_CALL_QUATERNARY_METHOD = CallQuaternaryMethodNode::create;
    private static final CallQuaternaryMethodNode UNCACHED_CALL_QUATERNARY_METHOD = CallQuaternaryMethodNode.getUncached();
    private static final NodeSupplier<CallTernaryMethodNode> NODE_CALL_TERNARY_METHOD = CallTernaryMethodNode::create;
    private static final CallTernaryMethodNode UNCACHED_CALL_TERNARY_METHOD = CallTernaryMethodNode.getUncached();
    private static final NodeSupplier<CallBinaryMethodNode> NODE_CALL_BINARY_METHOD = CallBinaryMethodNode::create;
    private static final CallBinaryMethodNode UNCACHED_CALL_BINARY_METHOD = CallBinaryMethodNode.getUncached();
    private static final NodeSupplier<CallUnaryMethodNode> NODE_CALL_UNARY_METHOD = CallUnaryMethodNode::create;
    private static final CallUnaryMethodNode UNCACHED_CALL_UNARY_METHOD = CallUnaryMethodNode.getUncached();
    private static final NodeSupplier<PyObjectGetMethod> NODE_OBJECT_GET_METHOD = PyObjectGetMethodNodeGen::create;
    private static final PyObjectGetMethod UNCACHED_OBJECT_GET_METHOD = PyObjectGetMethodNodeGen.getUncached();
    private static final NodeSupplier<GetNextNode> NODE_GET_NEXT = GetNextNode::create;
    private static final GetNextNode UNCACHED_GET_NEXT = GetNextNode.getUncached();
    private static final NodeSupplier<PyObjectGetIter> NODE_OBJECT_GET_ITER = PyObjectGetIter::create;
    private static final PyObjectGetIter UNCACHED_OBJECT_GET_ITER = PyObjectGetIter.getUncached();
    private static final NodeSupplier<HashingStorageLibrary> NODE_HASHING_STORAGE_LIBRARY = () -> HashingStorageLibrary.getFactory().createDispatched(2);
    private static final HashingStorageLibrary UNCACHED_HASHING_STORAGE_LIBRARY = HashingStorageLibrary.getUncached();
    private static final NodeSupplier<PyObjectSetAttr> NODE_OBJECT_SET_ATTR = PyObjectSetAttr::create;
    private static final PyObjectSetAttr UNCACHED_OBJECT_SET_ATTR = PyObjectSetAttr.getUncached();
    private static final NodeSupplier<ReadGlobalOrBuiltinNode> NODE_READ_GLOBAL_OR_BUILTIN_BUILD_CLASS = () -> ReadGlobalOrBuiltinNode.create(__BUILD_CLASS__);
    private static final NodeFunction<String, ReadGlobalOrBuiltinNode> NODE_READ_GLOBAL_OR_BUILTIN = ReadGlobalOrBuiltinNode::create;
    private static final ReadGlobalOrBuiltinNode UNCACHED_READ_GLOBAL_OR_BUILTIN = ReadGlobalOrBuiltinNode.getUncached();
    private static final NodeSupplier<PyObjectSetItem> NODE_OBJECT_SET_ITEM = PyObjectSetItem::create;
    private static final PyObjectSetItem UNCACHED_OBJECT_SET_ITEM = PyObjectSetItem.getUncached();
    private static final NodeSupplier<PyObjectIsTrueNode> NODE_OBJECT_IS_TRUE = PyObjectIsTrueNode::create;
    private static final PyObjectIsTrueNode UNCACHED_OBJECT_IS_TRUE = PyObjectIsTrueNode.getUncached();
    private static final NodeSupplier<GetItemNode> NODE_GET_ITEM = GetItemNode::create;
    private static final ExceptMatchNode UNCACHED_EXCEPT_MATCH = ExceptMatchNode.getUncached();
    private static final NodeSupplier<ExceptMatchNode> NODE_EXCEPT_MATCH = ExceptMatchNode::create;
    private static final SetupWithNode UNCACHED_SETUP_WITH_NODE = SetupWithNode.getUncached();
    private static final NodeSupplier<SetupWithNode> NODE_SETUP_WITH = SetupWithNode::create;
    private static final ExitWithNode UNCACHED_EXIT_WITH_NODE = ExitWithNode.getUncached();
    private static final NodeSupplier<ExitWithNode> NODE_EXIT_WITH = ExitWithNode::create;
    private static final ImportFromNode UNCACHED_IMPORT_FROM = ImportFromNode.getUncached();
    private static final NodeSupplier<ImportFromNode> NODE_IMPORT_FROM = ImportFromNode::create;
    private static final ExecutePositionalStarargsNode UNCACHED_EXECUTE_STARARGS = ExecutePositionalStarargsNode.getUncached();
    private static final NodeSupplier<ExecutePositionalStarargsNode> NODE_EXECUTE_STARARGS = ExecutePositionalStarargsNode::create;
    private static final ExpandKeywordStarargsNode UNCACHED_EXPAND_KEYWORD_STARARGS = ExpandKeywordStarargsNode.getUncached();
    private static final NodeSupplier<ExpandKeywordStarargsNode> NODE_EXPAND_KEYWORD_STARARGS = ExpandKeywordStarargsNode::create;
    private static final SliceNodes.CreateSliceNode UNCACHED_CREATE_SLICE = SliceNodes.CreateSliceNode.getUncached();
    private static final NodeSupplier<SliceNodes.CreateSliceNode> NODE_CREATE_SLICE = SliceNodes.CreateSliceNode::create;
    private static final ListNodes.ConstructListNode UNCACHED_CONSTRUCT_LIST = ListNodes.ConstructListNode.getUncached();
    private static final NodeSupplier<ListNodes.ConstructListNode> NODE_CONSTRUCT_LIST = ListNodes.ConstructListNode::create;
    private static final TupleNodes.ConstructTupleNode UNCACHED_CONSTRUCT_TUPLE = TupleNodes.ConstructTupleNode.getUncached();
    private static final NodeSupplier<TupleNodes.ConstructTupleNode> NODE_CONSTRUCT_TUPLE = TupleNodes.ConstructTupleNode::create;
    private static final SetNodes.ConstructSetNode UNCACHED_CONSTRUCT_SET = SetNodes.ConstructSetNode.getUncached();
    private static final NodeSupplier<SetNodes.ConstructSetNode> NODE_CONSTRUCT_SET = SetNodes.ConstructSetNode::create;
    private static final NodeSupplier<HashingStorage.InitNode> NODE_HASHING_STORAGE_INIT = HashingStorage.InitNode::create;
    private static final NodeSupplier<ListBuiltins.ListExtendNode> NODE_LIST_EXTEND = ListBuiltins.ListExtendNode::create;
    private static final SetBuiltins.UpdateSingleNode UNCACHED_SET_UPDATE = SetBuiltins.UpdateSingleNode.getUncached();
    private static final NodeSupplier<DictNodes.UpdateNode> NODE_DICT_UPDATE = DictNodes.UpdateNode::create;
    private static final NodeSupplier<SetBuiltins.UpdateSingleNode> NODE_SET_UPDATE = SetBuiltins.UpdateSingleNode::create;
    private static final ListNodes.AppendNode UNCACHED_LIST_APPEND = ListNodes.AppendNode.getUncached();
    private static final NodeSupplier<ListNodes.AppendNode> NODE_LIST_APPEND = ListNodes.AppendNode::create;
    private static final SetNodes.AddNode UNCACHED_SET_ADD = SetNodes.AddNode.getUncached();
    private static final NodeSupplier<SetNodes.AddNode> NODE_SET_ADD = SetNodes.AddNode::create;
    private static final UnpackSequenceNode UNCACHED_UNPACK_SEQUENCE = UnpackSequenceNode.getUncached();
    private static final NodeSupplier<UnpackSequenceNode> NODE_UNPACK_SEQUENCE = UnpackSequenceNode::create;

    private static final WriteGlobalNode UNCACHED_WRITE_GLOBAL = WriteGlobalNode.getUncached();
    private static final NodeFunction<String, WriteGlobalNode> NODE_WRITE_GLOBAL = WriteGlobalNode::create;

    private static final NodeFunction<String, DeleteGlobalNode> NODE_DELETE_GLOBAL = DeleteGlobalNode::create;

    private static final IntNodeFunction<UnaryOpNode> UNARY_OP_FACTORY = (int op) -> {
        switch (op) {
            case UnaryOpsConstants.NOT:
                return CoerceToBooleanNode.createIfFalseNode();
            case UnaryOpsConstants.POSITIVE:
                return PosNode.create();
            case UnaryOpsConstants.NEGATIVE:
                return NegNode.create();
            case UnaryOpsConstants.INVERT:
                return InvertNode.create();
            default:
                throw CompilerDirectives.shouldNotReachHere();
        }
    };

    private static final IntNodeFunction<Node> BINARY_OP_FACTORY = (int op) -> {
        switch (op) {
            case BinaryOpsConstants.ADD:
                return BinaryArithmetic.Add.create();
            case BinaryOpsConstants.SUB:
                return BinaryArithmetic.Sub.create();
            case BinaryOpsConstants.MUL:
                return BinaryArithmetic.Mul.create();
            case BinaryOpsConstants.TRUEDIV:
                return BinaryArithmetic.TrueDiv.create();
            case BinaryOpsConstants.FLOORDIV:
                return BinaryArithmetic.FloorDiv.create();
            case BinaryOpsConstants.MOD:
                return BinaryArithmetic.Mod.create();
            case BinaryOpsConstants.LSHIFT:
                return BinaryArithmetic.LShift.create();
            case BinaryOpsConstants.RSHIFT:
                return BinaryArithmetic.RShift.create();
            case BinaryOpsConstants.AND:
                return BinaryArithmetic.And.create();
            case BinaryOpsConstants.OR:
                return BinaryArithmetic.Or.create();
            case BinaryOpsConstants.XOR:
                return BinaryArithmetic.Xor.create();
            case BinaryOpsConstants.POW:
                return BinaryArithmetic.Pow.create();
            case BinaryOpsConstants.MATMUL:
                return BinaryArithmetic.MatMul.create();
            case BinaryOpsConstants.INPLACE_ADD:
                return InplaceArithmetic.IAdd.create();
            case BinaryOpsConstants.INPLACE_SUB:
                return InplaceArithmetic.ISub.create();
            case BinaryOpsConstants.INPLACE_MUL:
                return InplaceArithmetic.IMul.create();
            case BinaryOpsConstants.INPLACE_TRUEDIV:
                return InplaceArithmetic.ITrueDiv.create();
            case BinaryOpsConstants.INPLACE_FLOORDIV:
                return InplaceArithmetic.IFloorDiv.create();
            case BinaryOpsConstants.INPLACE_MOD:
                return InplaceArithmetic.IMod.create();
            case BinaryOpsConstants.INPLACE_LSHIFT:
                return InplaceArithmetic.ILShift.create();
            case BinaryOpsConstants.INPLACE_RSHIFT:
                return InplaceArithmetic.IRShift.create();
            case BinaryOpsConstants.INPLACE_AND:
                return InplaceArithmetic.IAnd.create();
            case BinaryOpsConstants.INPLACE_OR:
                return InplaceArithmetic.IOr.create();
            case BinaryOpsConstants.INPLACE_XOR:
                return InplaceArithmetic.IXor.create();
            case BinaryOpsConstants.INPLACE_POW:
                return InplaceArithmetic.IPow.create();
            case BinaryOpsConstants.INPLACE_MATMUL:
                return InplaceArithmetic.IMatMul.create();
            case BinaryOpsConstants.EQ:
                return BinaryComparisonNode.EqNode.create();
            case BinaryOpsConstants.NE:
                return BinaryComparisonNode.NeNode.create();
            case BinaryOpsConstants.LT:
                return BinaryComparisonNode.LtNode.create();
            case BinaryOpsConstants.LE:
                return BinaryComparisonNode.LeNode.create();
            case BinaryOpsConstants.GT:
                return BinaryComparisonNode.GtNode.create();
            case BinaryOpsConstants.GE:
                return BinaryComparisonNode.GeNode.create();
            case BinaryOpsConstants.IS:
                return IsNode.create();
            case BinaryOpsConstants.IN:
                return ContainsNode.create();
            default:
                throw CompilerDirectives.shouldNotReachHere();
        }
    };

    private final int stacksize;
    private final Signature signature;
    private final String name;
    public final String filename;

    private final int celloffset;
    private final int freeoffset;
    private final int stackoffset;
    private final int bcioffset;
    private final int classcellIndex;

    public static final class FrameInfo {
        private final CodeUnit code;
        private final Source source;

        public FrameInfo(CodeUnit code, Source source) {
            this.code = code;
            this.source = source;
        }

        public int getCellOffset() {
            return code.varnames.length;
        }

        public int getFreeOffset() {
            return getCellOffset() + code.cellvars.length;
        }

        public int getStackOffset() {
            return getFreeOffset() + code.freevars.length;
        }

        public int getBciOffset() {
            return getStackOffset() + code.stacksize;
        }

        public int bciToLine(int bci) {
            return PBytecodeRootNode.bciToLine(code, source, bci);
        }

        public int getBci(Frame frame) {
            int bciOffset = getBciOffset();
            try {
                return frame.getInt(bciOffset);
            } catch (FrameSlotTypeException e) {
                return -1;
            }
        }

        public int getLineno(Frame frame) {
            return bciToLine(getBci(frame));
        }

        public void syncLocals(VirtualFrame frame, Object localsObject, Frame frameToSync, PyObjectSetItem setItem, PyObjectDelItem delItem, IsBuiltinClassProfile errorProfile) {
            for (int i = 0; i < code.varnames.length; i++) {
                setVar(frame, localsObject, setItem, delItem, errorProfile, code.varnames[i], frameToSync.getObject(i));
            }
            int cellOffset = getCellOffset();
            for (int i = 0; i < code.cellvars.length; i++) {
                PCell cell = (PCell) frameToSync.getObject(cellOffset + i);
                setVar(frame, localsObject, setItem, delItem, errorProfile, code.cellvars[i], cell.getRef());
            }
            int freeOffset = getFreeOffset();
            for (int i = 0; i < code.freevars.length; i++) {
                PCell cell = (PCell) frameToSync.getObject(freeOffset + i);
                setVar(frame, localsObject, setItem, delItem, errorProfile, code.freevars[i], cell.getRef());
            }
        }

        private void setVar(VirtualFrame frame, Object localsObject, PyObjectSetItem setItem, PyObjectDelItem delItem, IsBuiltinClassProfile errorProfile, String name, Object value) {
            if (value == null) {
                try {
                    delItem.execute(frame, localsObject, name);
                } catch (PException e) {
                    e.expect(KeyError, errorProfile);
                }
            } else {
                setItem.execute(frame, localsObject, name, value);
            }
        }
    }

    private final CodeUnit co;

    private final Source source;
    private SourceSection sourceSection;

    @CompilationFinal(dimensions = 1) private final byte[] bytecode;
    @CompilationFinal(dimensions = 1) private final Object[] consts;
    @CompilationFinal(dimensions = 1) private final long[] longConsts;
    @CompilationFinal(dimensions = 1) private final String[] names;
    @CompilationFinal(dimensions = 1) private final String[] varnames;
    @CompilationFinal(dimensions = 1) private final String[] freevars;
    @CompilationFinal(dimensions = 1) private final String[] cellvars;

    @CompilationFinal(dimensions = 1) protected final Assumption[] cellEffectivelyFinalAssumptions;

    @CompilationFinal(dimensions = 1) private final short[] exceptionHandlerRanges;

    /**
     * PE-final store for quickened bytecodes.
     */
    @CompilationFinal(dimensions = 1) private final int[] extraArgs;

    @Children private final Node[] adoptedNodes;
    @Child private CalleeContext calleeContext = CalleeContext.create();
    @Child private PythonObjectFactory factory = PythonObjectFactory.create();
    @CompilationFinal private LoopConditionProfile exceptionChainProfile1 = LoopConditionProfile.createCountingProfile();
    @CompilationFinal private LoopConditionProfile exceptionChainProfile2 = LoopConditionProfile.createCountingProfile();

    @CompilationFinal private Object osrMetadata;

    private static final Node MARKER_NODE = new Node() {
        @Override
        public boolean isAdoptable() {
            return false;
        }
    };

    private static FrameDescriptor makeFrameDescriptor(CodeUnit co, Source source) {
        FrameDescriptor.Builder newBuilder = FrameDescriptor.newBuilder(4);
        newBuilder.info(new FrameInfo(co, source));
        // locals
        newBuilder.addSlots(co.varnames.length, FrameSlotKind.Illegal);
        // cells
        newBuilder.addSlots(co.cellvars.length, FrameSlotKind.Illegal);
        // freevars
        newBuilder.addSlots(co.freevars.length, FrameSlotKind.Illegal);
        // stack
        newBuilder.addSlots(co.stacksize, FrameSlotKind.Illegal);
        // BCI filled when unwinding the stack to note the location for tracebacks
        newBuilder.addSlot(FrameSlotKind.Int, null, null);
        return newBuilder.build();
    }

    @TruffleBoundary
    public PBytecodeRootNode(TruffleLanguage<?> language, Signature sign, CodeUnit co, Source source) {
        this(language, makeFrameDescriptor(co, source), sign, co, source);
    }

    @TruffleBoundary
    public PBytecodeRootNode(TruffleLanguage<?> language, FrameDescriptor fd, Signature sign, CodeUnit co, Source source) {
        super(language, fd);
        FrameInfo info = (FrameInfo) fd.getInfo();
        this.celloffset = info.getCellOffset();
        this.freeoffset = info.getFreeOffset();
        this.stackoffset = info.getStackOffset();
        this.bcioffset = info.getBciOffset();
        this.source = source;
        this.signature = sign;
        this.bytecode = co.code;
        this.adoptedNodes = new Node[co.code.length];
        this.extraArgs = new int[co.code.length];
        this.consts = co.constants;
        this.longConsts = co.primitiveConstants;
        this.names = co.names;
        this.varnames = co.varnames;
        this.freevars = co.freevars;
        this.cellvars = co.cellvars;
        this.stacksize = co.stacksize;
        this.filename = co.filename;
        this.name = co.name;
        this.exceptionHandlerRanges = co.exceptionHandlerRanges;
        this.co = co;
        assert stacksize < Math.pow(2, 12) : "stacksize cannot be larger than 12-bit range";
        assert bytecode.length < Math.pow(2, 16) : "bytecode cannot be longer than 16-bit range";
        cellEffectivelyFinalAssumptions = new Assumption[cellvars.length];
        for (int i = 0; i < cellvars.length; i++) {
            cellEffectivelyFinalAssumptions[i] = Truffle.getRuntime().createAssumption("cell is effectively final");
        }
        int classcellIndex = -1;
        for (int i = 0; i < this.freevars.length; i++) {
            if (__CLASS__.equals(this.freevars[i])) {
                classcellIndex = this.freeoffset + i;
                break;
            }
        }
        this.classcellIndex = classcellIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "<bytecode " + name + " at " + Integer.toHexString(hashCode()) + ">";
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public boolean isPythonInternal() {
        return false;
    }

    @ExplodeLoop
    private void copyArgs(Object[] args, VirtualFrame frame) {
        for (int i = 0; i < PArguments.getUserArgumentLength(args); i++) {
            // we can set these as object, since they're already boxed
            frame.setObject(i, args[i + PArguments.USER_ARGUMENTS_OFFSET]);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        calleeContext.enter(frame);
        try {
            Object[] arguments = frame.getArguments();
            copyArgs(arguments, frame);
            int varargsIdx = signature.getVarargsIdx();
            if (varargsIdx >= 0) {
                frame.setObject(varargsIdx, factory.createList(PArguments.getVariableArguments(arguments)));
            }
            if (signature.takesVarKeywordArgs()) {
                int idx = signature.getParameterIds().length + signature.getKeywordNames().length;
                if (varargsIdx >= 0) {
                    idx += 1;
                }
                frame.setObject(idx, factory.createDict(PArguments.getKeywordArguments(arguments)));
            }
            initCellVars(frame);
            initFreeVars(frame, arguments);

            return executeOSR(frame, encodeBCI(0) | encodeStackTop(stackoffset - 1), arguments);
        } finally {
            calleeContext.exit(frame, this);
        }
    }

    @FunctionalInterface
    private static interface NodeSupplier<T> {
        T get();
    }

    @FunctionalInterface
    private static interface NodeFunction<A, T> {
        T apply(A argument);
    }

    @FunctionalInterface
    private static interface IntNodeFunction<T extends Node> {
        T apply(int argument);
    }

    @SuppressWarnings("unchecked")
    private <A, T extends Node> T insertChildNode(Node node, NodeFunction<A, T> nodeSupplier, int bytecodeIndex, A argument) {
        if (node != null) {
            return (T) node;
        }
        return doInsertChildNode(node, nodeSupplier, bytecodeIndex, argument);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <A, T extends Node> T doInsertChildNode(Node node, NodeFunction<A, T> nodeSupplier, int bytecodeIndex, A argument) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert node == null;
        T newNode = nodeSupplier.apply(argument);
        adoptedNodes[bytecodeIndex] = insert(newNode);
        return newNode;
    }

    @SuppressWarnings("unchecked")
    private <A, T extends Node> T insertChildNode(Node node, T uncached, NodeFunction<A, T> nodeSupplier, int bytecodeIndex, A argument) {
        if (node != null && node != MARKER_NODE) {
            return (T) node;
        }
        return doInsertChildNode(node, uncached, nodeSupplier, bytecodeIndex, argument);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <A, T extends Node> T doInsertChildNode(Node node, T uncached, NodeFunction<A, T> nodeSupplier, int bytecodeIndex, A argument) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (node == null) { // first execution uncached
            adoptedNodes[bytecodeIndex] = MARKER_NODE;
            return uncached;
        } else {
            assert node == MARKER_NODE; // second execution caches
            T newNode = nodeSupplier.apply(argument);
            adoptedNodes[bytecodeIndex] = insert(newNode);
            return newNode;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T insertChildNode(Node node, IntNodeFunction<T> nodeSupplier, int bytecodeIndex, int argument) {
        if (node != null) {
            return (T) node;
        }
        return doInsertChildNode(node, nodeSupplier, bytecodeIndex, argument);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <T extends Node> T doInsertChildNode(Node node, IntNodeFunction<T> nodeSupplier, int bytecodeIndex, int argument) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert node == null;
        T newNode = nodeSupplier.apply(argument);
        adoptedNodes[bytecodeIndex] = insert(newNode);
        return newNode;
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T insertChildNode(Node node, T uncached, IntNodeFunction<T> nodeSupplier, int bytecodeIndex, int argument) {
        if (node != null && node != MARKER_NODE) {
            return (T) node;
        }
        return doInsertChildNode(node, uncached, nodeSupplier, bytecodeIndex, argument);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <T extends Node> T doInsertChildNode(Node node, T uncached, IntNodeFunction<T> nodeSupplier, int bytecodeIndex, int argument) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (node == null) { // first execution uncached
            adoptedNodes[bytecodeIndex] = MARKER_NODE;
            return uncached;
        } else if (node == MARKER_NODE) { // second execution caches
            T newNode = nodeSupplier.apply(argument);
            adoptedNodes[bytecodeIndex] = insert(newNode);
            return newNode;
        } else {
            return (T) node;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T insertChildNode(Node node, NodeSupplier<T> nodeSupplier, int bytecodeIndex) {
        if (node != null) {
            return (T) node;
        }
        return doInsertChildNode(nodeSupplier, bytecodeIndex);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <T extends Node> T doInsertChildNode(NodeSupplier<T> nodeSupplier, int bytecodeIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        T newNode = nodeSupplier.get();
        adoptedNodes[bytecodeIndex] = insert(newNode);
        return newNode;
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T insertChildNode(Node node, T uncached, NodeSupplier<T> nodeSupplier, int bytecodeIndex) {
        if (node != null && node != MARKER_NODE) {
            return (T) node;
        }
        return doInsertChildNode(node, uncached, nodeSupplier, bytecodeIndex);
    }

    @BytecodeInterpreterSwitchBoundary
    @SuppressWarnings("unchecked")
    private <T extends Node> T doInsertChildNode(Node node, T uncached, NodeSupplier<T> nodeSupplier, int bytecodeIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (node == null) { // first execution uncached
            adoptedNodes[bytecodeIndex] = MARKER_NODE;
            return uncached;
        } else { // second execution caches
            assert node == MARKER_NODE;
            T newNode = nodeSupplier.get();
            adoptedNodes[bytecodeIndex] = insert(newNode);
            return newNode;
        }
    }

    @Override
    public Object getOSRMetadata() {
        return osrMetadata;
    }

    @Override
    public void setOSRMetadata(Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }

    private static int decodeBCI(int target) {
        return (target >>> 16) & 0xffff; // unsigned
    }

    private static int decodeStackTop(int target) {
        return (target & 0xffff) - 1;
    }

    private static int encodeBCI(int bci) {
        return bci << 16;
    }

    private static int encodeStackTop(int stackTop) {
        return stackTop + 1;
    }

    /**
     * @param target - encodes bci (16bit), stackTop (12bit), and blockstackTop (4bit)
     */
    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    @SuppressWarnings("fallthrough")
    public Object executeOSR(VirtualFrame frame, int target, Object originalArgs) {
        PythonLanguage lang = PythonLanguage.get(this);
        PythonContext context = PythonContext.get(this);
        PythonModule builtins = context.getBuiltins();

        boolean inInterpreter = CompilerDirectives.inInterpreter();

        Object globals = PArguments.getGlobals((Object[]) originalArgs);
        Object locals = PArguments.getSpecialArgument((Object[]) originalArgs);

        int loopCount = 0;
        int stackTop = decodeStackTop(target);
        int bci = decodeBCI(target);

        byte[] localBC = bytecode;
        int[] localArgs = extraArgs;
        Object[] localConsts = consts;
        long[] localLongConsts = longConsts;
        String[] localNames = names;
        Node[] localNodes = adoptedNodes;

        verifyBeforeLoop(target, stackTop, bci, localBC);

        while (true) {
            // System.out.println(java.util.Arrays.toString(stack));
            // System.out.println(bci);

            final byte bc = localBC[bci];

            verifyInLoop(stackTop, bci, bc);

            try {
                switch (bc) {
                    case LOAD_NONE:
                        frame.setObject(++stackTop, PNone.NONE);
                        break;
                    case LOAD_ELLIPSIS:
                        frame.setObject(++stackTop, PEllipsis.INSTANCE);
                        break;
                    case LOAD_TRUE:
                        frame.setObject(++stackTop, true);
                        break;
                    case LOAD_FALSE:
                        frame.setObject(++stackTop, false);
                        break;
                    case LOAD_BYTE:
                        frame.setObject(++stackTop, (int) localBC[++bci]); // signed!
                        break;
                    case LOAD_LONG: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(++stackTop, localLongConsts[oparg]);
                        break;
                    }
                    case LOAD_DOUBLE: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(++stackTop, Double.longBitsToDouble(localLongConsts[oparg]));
                        break;
                    }
                    case LOAD_BIGINT: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(++stackTop, factory.createInt((BigInteger) localConsts[oparg]));
                        break;
                    }
                    case LOAD_STRING:
                    case LOAD_CONST: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(++stackTop, localConsts[oparg]);
                        break;
                    }
                    case LOAD_BYTES: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(++stackTop, factory.createBytes((byte[]) localConsts[oparg]));
                        break;
                    }
                    case MAKE_COMPLEX: {
                        double imag = (double) frame.getObject(stackTop--);
                        double real = (double) frame.getObject(stackTop);
                        frame.setObject(stackTop, factory.createComplex(real, imag));
                        break;
                    }
                    case MAKE_KEYWORD: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        String key = (String) localConsts[oparg];
                        Object value = frame.getObject(stackTop);
                        frame.setObject(stackTop, new PKeyword(key, value));
                        break;
                    }
                    case BUILD_SLICE: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeBuildSlice(frame, stackTop, bci, oparg, localNodes);
                        break;
                    }
                    case COLLECTION_FROM_COLLECTION: {
                        int type = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeCollectionFromCollection(frame, type, stackTop, localNodes, bci);
                        break;
                    }
                    case COLLECTION_ADD_COLLECTION: {
                        /*
                         * The first collection must be in the target format already, the second one
                         * is a python object.
                         */
                        int type = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeCollectionAddCollection(frame, type, stackTop, localNodes, bci);
                        break;
                    }
                    case COLLECTION_FROM_STACK: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        int count = oparg & CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        int type = oparg & ~CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        stackTop = bytecodeCollectionFromStack(frame, type, count, stackTop, localNodes, bci);
                        break;
                    }
                    case COLLECTION_ADD_STACK: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        // Just combine COLLECTION_FROM_STACK and COLLECTION_ADD_COLLECTION for now
                        int count = oparg & CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        int type = oparg & ~CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        stackTop = bytecodeCollectionFromStack(frame, type, count, stackTop, localNodes, bci - 1);
                        stackTop = bytecodeCollectionAddCollection(frame, type, stackTop, localNodes, bci);
                        break;
                    }
                    case ADD_TO_COLLECTION: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        int depth = oparg & CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        int type = oparg & ~CollectionBits.MAX_STACK_ELEMENT_COUNT;
                        stackTop = bytecodeAddToCollection(frame, stackTop, bci, localNodes, depth, type);
                        break;
                    }
                    case UNPACK_SEQUENCE: {
                        int count = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeUnpackSequence(frame, stackTop, bci, localNodes, count);
                        break;
                    }
                    case UNPACK_SEQUENCE_LARGE: {
                        int count = Byte.toUnsignedInt(localBC[bci++]) << 8 | Byte.toUnsignedInt(localBC[bci++]);
                        stackTop = bytecodeUnpackSequence(frame, stackTop, bci, localNodes, count);
                        break;
                    }
                    case NOP:
                        break;
                    case LOAD_FAST: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        Object value = frame.getObject(oparg);
                        if (value == null) {
                            if (localArgs[bci] == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                localArgs[bci] = 1;
                                throw PRaiseNode.raiseUncached(this, PythonBuiltinClassType.UnboundLocalError, ErrorMessages.LOCAL_VAR_REFERENCED_BEFORE_ASSIGMENT, varnames[oparg]);
                            } else {
                                PRaiseNode raiseNode = insertChildNode(localNodes[bci], NODE_RAISE, bci);
                                throw raiseNode.raise(PythonBuiltinClassType.UnboundLocalError, ErrorMessages.LOCAL_VAR_REFERENCED_BEFORE_ASSIGMENT, varnames[oparg]);
                            }
                        }
                        frame.setObject(++stackTop, value);
                        break;
                    }
                    case LOAD_CLOSURE: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        PCell cell = (PCell) frame.getObject(celloffset + oparg);
                        frame.setObject(++stackTop, cell);
                        break;
                    }
                    case CLOSURE_FROM_STACK: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeClosureFromStack(frame, stackTop, oparg);
                        break;
                    }
                    case LOAD_DEREF: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeLoadDeref(frame, stackTop, bci, localNodes, oparg);
                        break;
                    }
                    case STORE_DEREF: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeStoreDeref(frame, stackTop, oparg);
                        break;
                    }
                    case DELETE_DEREF: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeDeleteDeref(frame, bci, localNodes, oparg);
                        break;
                    }
                    case STORE_FAST: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        frame.setObject(oparg, frame.getObject(stackTop));
                        frame.setObject(stackTop--, null);
                        break;
                    }
                    case POP_TOP:
                        frame.setObject(stackTop--, null);
                        break;
                    case ROT_TWO: {
                        Object top = frame.getObject(stackTop);
                        frame.setObject(stackTop, frame.getObject(stackTop - 1));
                        frame.setObject(stackTop - 1, top);
                        break;
                    }
                    case ROT_THREE: {
                        Object top = frame.getObject(stackTop);
                        frame.setObject(stackTop, frame.getObject(stackTop - 1));
                        frame.setObject(stackTop - 1, frame.getObject(stackTop - 2));
                        frame.setObject(stackTop - 2, top);
                        break;
                    }
                    case ROT_FOUR: {
                        Object top = frame.getObject(stackTop);
                        frame.setObject(stackTop, frame.getObject(stackTop - 1));
                        frame.setObject(stackTop - 1, frame.getObject(stackTop - 2));
                        frame.setObject(stackTop - 2, frame.getObject(stackTop - 3));
                        frame.setObject(stackTop - 3, top);
                        break;
                    }
                    case DUP_TOP:
                        frame.setObject(stackTop + 1, frame.getObject(stackTop));
                        stackTop++;
                        break;
                    case DUP_TOP_TWO:
                        frame.setObject(stackTop + 2, frame.getObject(stackTop));
                        frame.setObject(stackTop + 1, frame.getObject(stackTop - 1));
                        stackTop += 2;
                        break;
                    case UNARY_OP: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        UnaryOpNode opNode = insertChildNode(localNodes[bci], UNARY_OP_FACTORY, bci, oparg);
                        Object value = frame.getObject(stackTop);
                        Object result = opNode.execute(frame, value);
                        frame.setObject(stackTop, result);
                        break;
                    }
                    case BINARY_OP: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        BinaryOp opNode = (BinaryOp) insertChildNode(localNodes[bci], BINARY_OP_FACTORY, bci, oparg);
                        Object right = frame.getObject(stackTop);
                        frame.setObject(stackTop--, null);
                        Object left = frame.getObject(stackTop);
                        Object result = opNode.executeObject(frame, left, right);
                        frame.setObject(stackTop, result);
                        break;
                    }
                    case BINARY_SUBSCR: {
                        GetItemNode getItemNode = insertChildNode(localNodes[bci], NODE_GET_ITEM, bci);
                        Object slice = frame.getObject(stackTop);
                        frame.setObject(stackTop--, null);
                        frame.setObject(stackTop, getItemNode.execute(frame, frame.getObject(stackTop), slice));
                        break;
                    }
                    case STORE_SUBSCR: {
                        stackTop = bytecodeStoreSubscr(frame, stackTop, bci, localNodes);
                        break;
                    }
                    case DELETE_SUBSCR: {
                        stackTop = bytecodeDeleteSubscr(frame, stackTop, bci, localNodes);
                        break;
                    }
                    case RAISE_VARARGS: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeRaiseVarargs(frame, stackTop, bci, oparg, localNodes);
                        break;
                    }
                    case RETURN_VALUE:
                        if (inInterpreter) {
                            LoopNode.reportLoopCount(this, loopCount);
                        }
                        return frame.getObject(stackTop);
                    case LOAD_BUILD_CLASS: {
                        ReadGlobalOrBuiltinNode read = insertChildNode(localNodes[bci], UNCACHED_READ_GLOBAL_OR_BUILTIN, NODE_READ_GLOBAL_OR_BUILTIN_BUILD_CLASS, bci);
                        frame.setObject(++stackTop, read.read(frame, globals, __BUILD_CLASS__));
                        break;
                    }
                    case LOAD_ASSERTION_ERROR: {
                        frame.setObject(++stackTop, PythonBuiltinClassType.AssertionError);
                        break;
                    }
                    case STORE_NAME: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeStoreName(frame, lang, globals, locals, stackTop, bci, oparg, localNames, localNodes);
                        break;
                    }
                    case DELETE_NAME: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeDeleteName(frame, globals, locals, bci, oparg, localNames, localNodes);
                        break;
                    }
                    case STORE_ATTR: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeStoreAttr(frame, stackTop, bci, oparg, localNodes, localNames);
                        break;
                    }
                    case DELETE_ATTR: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeDeleteAttr(frame, stackTop, bci, oparg, localNodes, localNames);
                        break;
                    }
                    case STORE_GLOBAL: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeStoreGlobal(frame, globals, stackTop, bci, oparg, localNodes, localNames);
                        break;
                    }
                    case DELETE_GLOBAL: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeDeleteGlobal(frame, globals, bci, oparg, localNodes, localNames);
                        break;
                    }
                    case LOAD_NAME: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeLoadName(frame, globals, locals, stackTop, bci, oparg, localNodes, localNames, lang);
                        break;
                    }
                    case LOAD_GLOBAL: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeLoadGlobal(frame, globals, stackTop, bci, localNames[oparg], localNodes[bci]);
                        break;
                    }
                    case DELETE_FAST: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeDeleteFast(frame, bci, localNodes, oparg);
                        break;
                    }
                    case LOAD_ATTR: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        bytecodeLoadAttr(frame, stackTop, bci, oparg, localNodes, localNames);
                        break;
                    }
                    case IMPORT_NAME: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeImportName(frame, context, builtins, globals, stackTop, bci, oparg, localNames, localNodes);
                        break;
                    }
                    case IMPORT_FROM: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeImportFrom(frame, stackTop, bci, oparg, localNames, localNodes);
                        break;
                    }
                    case JUMP_FORWARD:
                        bci += Byte.toUnsignedInt(localBC[bci + 1]);
                        continue;
                    case JUMP_FORWARD_FAR:
                        bci += ((Byte.toUnsignedInt(localBC[bci + 1]) << 8) | Byte.toUnsignedInt(localBC[bci + 2]));
                        continue;
                    case POP_AND_JUMP_IF_FALSE: {
                        PyObjectIsTrueNode isTrue = insertChildNode(localNodes[bci], UNCACHED_OBJECT_IS_TRUE, NODE_OBJECT_IS_TRUE, bci);
                        Object cond = frame.getObject(stackTop);
                        frame.setObject(stackTop--, null);
                        if (!isTrue.execute(frame, cond)) {
                            bci += Byte.toUnsignedInt(localBC[bci + 1]);
                            continue;
                        }
                        bci++;
                        break;
                    }
                    case POP_AND_JUMP_IF_TRUE: {
                        PyObjectIsTrueNode isTrue = insertChildNode(localNodes[bci], UNCACHED_OBJECT_IS_TRUE, NODE_OBJECT_IS_TRUE, bci);
                        Object cond = frame.getObject(stackTop);
                        frame.setObject(stackTop--, null);
                        if (isTrue.execute(frame, cond)) {
                            bci += Byte.toUnsignedInt(localBC[bci + 1]);
                            continue;
                        }
                        bci++;
                        break;
                    }
                    case JUMP_IF_FALSE_OR_POP: {
                        PyObjectIsTrueNode isTrue = insertChildNode(localNodes[bci], UNCACHED_OBJECT_IS_TRUE, NODE_OBJECT_IS_TRUE, bci);
                        Object cond = frame.getObject(stackTop);
                        if (!isTrue.execute(frame, cond)) {
                            bci += Byte.toUnsignedInt(localBC[bci + 1]);
                            continue;
                        } else {
                            frame.setObject(stackTop--, null);
                        }
                        bci++;
                        break;
                    }
                    case JUMP_IF_TRUE_OR_POP: {
                        PyObjectIsTrueNode isTrue = insertChildNode(localNodes[bci], UNCACHED_OBJECT_IS_TRUE, NODE_OBJECT_IS_TRUE, bci);
                        Object cond = frame.getObject(stackTop);
                        if (isTrue.execute(frame, cond)) {
                            bci += Byte.toUnsignedInt(localBC[bci + 1]);
                            continue;
                        } else {
                            frame.setObject(stackTop--, null);
                        }
                        bci++;
                        break;
                    }
                    case JUMP_BACKWARD:
                    case JUMP_BACKWARD_FAR: {
                        if (inInterpreter) {
                            loopCount++;
                            if (BytecodeOSRNode.pollOSRBackEdge(this)) {
                                // we're in the interpreter, so the unboxed storage for locals
                                // is not used
                                int newBCI;
                                if (bc == JUMP_BACKWARD_FAR) {
                                    newBCI = bci - ((Byte.toUnsignedInt(localBC[bci + 1]) << 8) | Byte.toUnsignedInt(localBC[bci + 2]));
                                } else {
                                    newBCI = bci - Byte.toUnsignedInt(localBC[bci + 1]);
                                }
                                Object osrResult = BytecodeOSRNode.tryOSR(this, encodeBCI(newBCI) | encodeStackTop(stackTop), originalArgs, null, frame);
                                if (osrResult != null) {
                                    LoopNode.reportLoopCount(this, loopCount);
                                    return osrResult;
                                }
                            }
                        }
                        if (bc == JUMP_BACKWARD_FAR) {
                            bci -= ((Byte.toUnsignedInt(localBC[bci + 1]) << 8) | Byte.toUnsignedInt(localBC[bci + 2]));
                        } else {
                            bci -= Byte.toUnsignedInt(localBC[bci + 1]);
                        }
                        continue;
                    }
                    case GET_ITER:
                        frame.setObject(stackTop, insertChildNode(localNodes[bci], UNCACHED_OBJECT_GET_ITER, NODE_OBJECT_GET_ITER, bci).execute(frame, frame.getObject(stackTop)));
                        break;
                    case FOR_ITER:
                    case FOR_ITER_FAR: {
                        try {
                            Object next = insertChildNode(localNodes[bci], UNCACHED_GET_NEXT, NODE_GET_NEXT, bci).execute(frame, frame.getObject(stackTop));
                            if (next != null) {
                                frame.setObject(++stackTop, next);
                                bci = bci + 1 + (bc - FOR_ITER);
                                break;
                            }
                        } catch (PException e) {
                            e.expect(StopIteration, insertChildNode(localNodes[bci + 1], UNCACHED_IS_BUILTIN_CLASS_PROFILE, NODE_IS_BUILTIN_CLASS_PROFILE, bci + 1));
                        }
                        int oparg = Byte.toUnsignedInt(localBC[bci + 1]);
                        if (bc == FOR_ITER_FAR) {
                            oparg = (oparg << 8) | Byte.toUnsignedInt(localBC[bci + 2]);
                        }
                        frame.setObject(stackTop--, null);
                        bci += oparg;
                        continue;
                    }
                    case CALL_METHOD: {
                        int argcount = Byte.toUnsignedInt(localBC[++bci]);
                        String methodName = localNames[Byte.toUnsignedInt(localBC[++bci])];
                        stackTop = bytecodeCallMethod(frame, stackTop, bci, argcount, methodName, localNodes);
                        break;
                    }
                    case CALL_METHOD_VARARGS: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        callMethodVarargs(frame, stackTop, bci, localNames, oparg, localNodes);
                        break;
                    }
                    case CALL_FUNCTION: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeCallFunction(frame, stackTop, bci, oparg, localNodes);
                        break;
                    }
                    case CALL_FUNCTION_VARARGS: {
                        stackTop = bytecodeCallFunctionVarargs(frame, stackTop, bci, localNodes);
                        break;
                    }
                    case CALL_FUNCTION_KW: {
                        stackTop = bytecodeCallFunctionKw(frame, stackTop, bci, localNodes);
                        break;
                    }
                    case MAKE_FUNCTION: {
                        int oparg = Byte.toUnsignedInt(localBC[++bci]);
                        stackTop = bytecodeMakeFunction(frame, globals, stackTop, oparg);
                        break;
                    }
                    case MATCH_EXC_OR_JUMP: {
                        Object exception = frame.getObject(stackTop - 1);
                        Object matchType = frame.getObject(stackTop);
                        frame.setObject(stackTop--, null);
                        ExceptMatchNode matchNode = insertChildNode(localNodes[bci], UNCACHED_EXCEPT_MATCH, NODE_EXCEPT_MATCH, bci);
                        if (!matchNode.executeMatch(frame, exception, matchType)) {
                            bci += Byte.toUnsignedInt(localBC[bci + 1]);
                            continue;
                        }
                        bci++;
                        break;
                    }
                    // TODO MATCH_EXC_OR_JUMP_FAR
                    case UNWRAP_EXC: {
                        Object exception = frame.getObject(stackTop);
                        if (exception instanceof PException) {
                            frame.setObject(stackTop, ((PException) exception).getEscapedException());
                        }
                        // Let interop exceptions be
                        break;
                    }
                    case SETUP_WITH: {
                        SetupWithNode setupWithNode = insertChildNode(localNodes[bci], UNCACHED_SETUP_WITH_NODE, NODE_SETUP_WITH, bci);
                        stackTop = setupWithNode.execute(frame, stackTop);
                        break;
                    }
                    case EXIT_WITH: {
                        ExitWithNode exitWithNode = insertChildNode(localNodes[bci], UNCACHED_EXIT_WITH_NODE, NODE_EXIT_WITH, bci);
                        stackTop = exitWithNode.execute(frame, stackTop);
                        break;
                    }
                    case PUSH_EXC_INFO: {
                        Object exception = frame.getObject(stackTop);
                        if (!(exception instanceof PException)) {
                            throw CompilerDirectives.shouldNotReachHere("interop exception state not implemented");
                        }
                        frame.setObject(stackTop++, PArguments.getException(frame));
                        PArguments.setException(frame, (PException) exception);
                        frame.setObject(stackTop, exception);
                        break;
                    }
                    case POP_EXCEPT: {
                        Object savedException = frame.getObject(stackTop);
                        if (!(savedException instanceof PException)) {
                            throw CompilerDirectives.shouldNotReachHere("interop exception state not implemented");
                        }
                        frame.setObject(stackTop--, null);
                        PArguments.setException(frame, (PException) savedException);
                        break;
                    }
                    case END_EXC_HANDLER: {
                        bytecodeEndExcHandler(frame, stackTop);
                    }
                    default:
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw insertChildNode(localNodes[bci], NODE_RAISE, bci).raise(SystemError, "not implemented bytecode %s", OpCodes.VALUES[bc]);
                }
                // prepare next loop
                bci++;
            } catch (PException e) {
                long newTarget = findHandler(bci);
                CompilerAsserts.partialEvaluationConstant(newTarget);
                e.markAsOriginatingFromBytecode();
                PException exceptionState = PArguments.getException(frame);
                if (exceptionState != null) {
                    ExceptionHandlingStatementNode.chainExceptions(e.getUnreifiedException(), exceptionState, exceptionChainProfile1, exceptionChainProfile2);
                }
                if (newTarget == -1) {
                    frame.setObject(bcioffset, bci);
                    throw e;
                } else {
                    e.setCatchingFrameReference(frame, this, bci);
                    int stackSizeOnEntry = decodeStackTop((int) newTarget);
                    stackTop = unwindBlock(frame, stackTop, stackSizeOnEntry + stackoffset);
                    // handler range encodes the stacksize, not the top of stack. so the stackTop is
                    // to be replaced with the exception
                    frame.setObject(stackTop, e);
                    bci = decodeBCI((int) newTarget);
                    continue;
                }
            } catch (AbstractTruffleException | StackOverflowError e) {
                throw e;
            } catch (ControlFlowException | ThreadDeath e) {
                // do not handle ThreadDeath, result of TruffleContext.closeCancelled()
                throw e;
            }
        }
    }

    private void bytecodeDeleteDeref(VirtualFrame frame, int bci, Node[] localNodes, int oparg) {
        PCell cell = (PCell) frame.getObject(celloffset + oparg);
        Object value = cell.getRef();
        if (value == null) {
            raiseUnboundCell(localNodes[bci], bci, oparg);
        }
        cell.clearRef();
    }

    private int bytecodeStoreDeref(VirtualFrame frame, int stackTop, int oparg) {
        PCell cell = (PCell) frame.getObject(celloffset + oparg);
        Object value = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        cell.setRef(value);
        return stackTop;
    }

    private int bytecodeLoadDeref(VirtualFrame frame, int stackTop, int bci, Node[] localNodes, int oparg) {
        PCell cell = (PCell) frame.getObject(celloffset + oparg);
        Object value = cell.getRef();
        if (value == null) {
            raiseUnboundCell(localNodes[bci], bci, oparg);
        }
        frame.setObject(++stackTop, value);
        return stackTop;
    }

    private int bytecodeClosureFromStack(VirtualFrame frame, int stackTop, int oparg) {
        PCell[] closure = new PCell[oparg];
        moveFromStack(frame, stackTop - oparg + 1, stackTop + 1, closure);
        stackTop -= oparg - 1;
        frame.setObject(stackTop, closure);
        return stackTop;
    }

    private void bytecodeEndExcHandler(VirtualFrame frame, int stackTop) {
        Object exception = frame.getObject(stackTop);
        Object savedException = frame.getObject(stackTop - 1);
        if (!(savedException instanceof PException)) {
            throw CompilerDirectives.shouldNotReachHere("interop exception state not implemented");
        }
        PArguments.setException(frame, (PException) savedException);
        if (exception instanceof PException) {
            throw ((PException) exception).getExceptionForReraise();
        } else if (exception instanceof AbstractTruffleException) {
            throw (AbstractTruffleException) exception;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw PRaiseNode.raiseUncached(this, SystemError, "expected exception on the stack");
        }
    }

    private void bytecodeLoadAttr(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes, String[] localNames) {
        PyObjectGetAttr getAttr = insertChildNode(localNodes[bci], UNCACHED_OBJECT_GET_ATTR, NODE_OBJECT_GET_ATTR, bci);
        String varname = localNames[oparg];
        Object owner = frame.getObject(stackTop);
        Object value = getAttr.execute(frame, owner, varname);
        frame.setObject(stackTop, value);
    }

    private void bytecodeDeleteFast(VirtualFrame frame, int bci, Node[] localNodes, int oparg) {
        Object value = frame.getObject(oparg);
        if (value == null) {
            PRaiseNode raiseNode = insertChildNode(localNodes[bci], UNCACHED_RAISE, NODE_RAISE, bci);
            throw raiseNode.raise(PythonBuiltinClassType.UnboundLocalError, ErrorMessages.LOCAL_VAR_REFERENCED_BEFORE_ASSIGMENT, varnames[oparg]);
        }
        frame.setObject(oparg, null);
    }

    private int bytecodeLoadGlobal(VirtualFrame frame, Object globals, int stackTop, int bci, String localName, Node localNode) {
        String varname = localName;
        ReadGlobalOrBuiltinNode read = insertChildNode(localNode, UNCACHED_READ_GLOBAL_OR_BUILTIN, NODE_READ_GLOBAL_OR_BUILTIN, bci, varname);
        frame.setObject(++stackTop, read.read(frame, globals, varname));
        return stackTop;
    }

    private void bytecodeDeleteGlobal(VirtualFrame frame, Object globals, int bci, int oparg, Node[] localNodes, String[] localNames) {
        String varname = localNames[oparg];
        DeleteGlobalNode deleteGlobalNode = insertChildNode(localNodes[bci], NODE_DELETE_GLOBAL, bci, varname);
        deleteGlobalNode.executeWithGlobals(frame, globals);
    }

    private int bytecodeStoreGlobal(VirtualFrame frame, Object globals, int stackTop, int bci, int oparg, Node[] localNodes, String[] localNames) {
        String varname = localNames[oparg];
        WriteGlobalNode writeGlobalNode = insertChildNode(localNodes[bci], UNCACHED_WRITE_GLOBAL, NODE_WRITE_GLOBAL, bci, varname);
        writeGlobalNode.write(frame, globals, varname, frame.getObject(stackTop));
        frame.setObject(stackTop--, null);
        return stackTop;
    }

    private int bytecodeDeleteAttr(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes, String[] localNames) {
        PyObjectSetAttr callNode = insertChildNode(localNodes[bci], UNCACHED_OBJECT_SET_ATTR, NODE_OBJECT_SET_ATTR, bci);
        String varname = localNames[oparg];
        Object owner = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        callNode.delete(frame, owner, varname);
        return stackTop;
    }

    private int bytecodeStoreAttr(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes, String[] localNames) {
        PyObjectSetAttr callNode = insertChildNode(localNodes[bci], UNCACHED_OBJECT_SET_ATTR, NODE_OBJECT_SET_ATTR, bci);
        String varname = localNames[oparg];
        Object owner = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        Object value = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        callNode.execute(frame, owner, varname, value);
        return stackTop;
    }

    private void bytecodeDeleteName(VirtualFrame frame, Object globals, Object locals, int bci, int oparg, String[] localNames, Node[] localNodes) {
        String varname = localNames[oparg];
        if (locals != null) {
            PyObjectDelItem delItemNode = insertChildNode(localNodes[bci - 1], UNCACHED_OBJECT_DEL_ITEM, NODE_OBJECT_DEL_ITEM, bci - 1);
            delItemNode.execute(frame, locals, varname);
        } else {
            DeleteGlobalNode deleteGlobalNode = insertChildNode(localNodes[bci], NODE_DELETE_GLOBAL, bci, varname);
            deleteGlobalNode.executeWithGlobals(frame, globals);
        }
    }

    private int bytecodeDeleteSubscr(VirtualFrame frame, int stackTop, int bci, Node[] localNodes) {
        DeleteItemNode delItem = insertChildNode(localNodes[bci], NODE_DELETE_ITEM, bci);
        Object slice = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        Object container = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        delItem.executeWith(frame, container, slice);
        return stackTop;
    }

    private int bytecodeStoreSubscr(VirtualFrame frame, int stackTop, int bci, Node[] localNodes) {
        PyObjectSetItem setItem = insertChildNode(localNodes[bci], UNCACHED_OBJECT_SET_ITEM, NODE_OBJECT_SET_ITEM, bci);
        Object index = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        Object container = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        Object value = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        setItem.execute(frame, container, index, value);
        return stackTop;
    }

    private int bytecodeBuildSlice(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes) {
        Object step;
        if (oparg == 3) {
            step = frame.getObject(stackTop);
            frame.setObject(stackTop--, null);
        } else {
            assert oparg == 2;
            step = PNone.NONE;
        }
        Object stop = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        Object start = frame.getObject(stackTop);
        SliceNodes.CreateSliceNode sliceNode = insertChildNode(localNodes[bci], UNCACHED_CREATE_SLICE, NODE_CREATE_SLICE, bci);
        PSlice slice = sliceNode.execute(start, stop, step);
        frame.setObject(stackTop, slice);
        return stackTop;
    }

    private int bytecodeCallFunctionKw(VirtualFrame frame, int initialStackTop, int bci, Node[] localNodes) {
        int stackTop = initialStackTop;
        CallNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL, NODE_CALL, bci);
        Object callable = frame.getObject(stackTop - 2);
        Object[] args = (Object[]) frame.getObject(stackTop - 1);
        frame.setObject(stackTop - 2, callNode.execute(frame, callable, args, (PKeyword[]) frame.getObject(stackTop)));
        frame.setObject(stackTop--, null);
        frame.setObject(stackTop--, null);
        return stackTop;
    }

    private int bytecodeCallFunctionVarargs(VirtualFrame frame, int initialStackTop, int bci, Node[] localNodes) {
        int stackTop = initialStackTop;
        CallNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL, NODE_CALL, bci);
        Object callable = frame.getObject(stackTop - 1);
        Object[] args = (Object[]) frame.getObject(stackTop);
        frame.setObject(stackTop - 1, callNode.execute(frame, callable, args, PKeyword.EMPTY_KEYWORDS));
        frame.setObject(stackTop--, null);
        return stackTop;
    }

    private void callMethodVarargs(VirtualFrame frame, int stackTop, int bci, String[] localNames, int oparg, Node[] localNodes) {
        PyObjectGetMethod getMethodNode = insertChildNode(localNodes[bci], UNCACHED_OBJECT_GET_METHOD, NODE_OBJECT_GET_METHOD, bci);
        Object[] args = (Object[]) frame.getObject(stackTop);
        String methodName = localNames[oparg];
        Object rcvr = args[0];
        Object func = getMethodNode.execute(frame, rcvr, methodName);
        CallNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL, NODE_CALL, bci);
        frame.setObject(stackTop, callNode.execute(frame, func, args, PKeyword.EMPTY_KEYWORDS));
    }

    private int bytecodeLoadName(VirtualFrame frame, Object globals, Object locals, int initialStackTop, int bci, int oparg, Node[] localNodes, String[] localNames, PythonLanguage lang) {
        int stackTop = initialStackTop;
        String localName = localNames[oparg];
        Object result = null;
        if (locals != null) {
            Node helper = localNodes[bci - 1];
            if (helper instanceof HashingStorageLibrary) {
                if (locals instanceof PDict && ((PDict) locals).getShape() == PythonBuiltinClassType.PDict.getInstanceShape(lang)) {
                    HashingStorageLibrary lib = (HashingStorageLibrary) helper;
                    result = lib.getItem(((PDict) locals).getDictStorage(), localName);
                } else { // generalize
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    GetItemNode newNode = GetItemNode.create();
                    localNodes[bci - 1] = helper.replace(newNode);
                    result = newNode.execute(frame, locals, localName);
                }
            } else if (helper instanceof GetItemNode) {
                result = ((GetItemNode) helper).execute(frame, locals, localName);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert helper == null || helper == MARKER_NODE;
                if (locals instanceof PDict && ((PDict) locals).getShape() == PythonBuiltinClassType.PDict.getInstanceShape(lang)) {
                    HashingStorageLibrary lib = insertChildNode(localNodes[bci - 1], UNCACHED_HASHING_STORAGE_LIBRARY, NODE_HASHING_STORAGE_LIBRARY, bci - 1);
                    result = lib.getItem(((PDict) locals).getDictStorage(), localName);
                } else {
                    GetItemNode newNode = insertChildNode(localNodes[bci - 1], NODE_GET_ITEM, bci - 1);
                    result = newNode.execute(frame, locals, localName);
                }
            }
        }
        if (result == null) {
            ReadGlobalOrBuiltinNode read = insertChildNode(localNodes[bci], UNCACHED_READ_GLOBAL_OR_BUILTIN, NODE_READ_GLOBAL_OR_BUILTIN, bci,
                            localName);
            result = read.read(frame, globals, localName);
        }
        frame.setObject(++stackTop, result);
        return stackTop;
    }

    private int bytecodeCallFunction(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes) {
        Object func = frame.getObject(stackTop - oparg);
        switch (oparg) {
            case 0: {
                CallNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL, NODE_CALL, bci);
                Object result = callNode.execute(frame, func, PythonUtils.EMPTY_OBJECT_ARRAY, PKeyword.EMPTY_KEYWORDS);
                frame.setObject(stackTop, result);
                break;
            }
            case 1: {
                CallUnaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_UNARY_METHOD, NODE_CALL_UNARY_METHOD, bci);
                Object result = callNode.executeObject(frame, func, frame.getObject(stackTop));
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, result);
                break;
            }
            case 2: {
                CallBinaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_BINARY_METHOD, NODE_CALL_BINARY_METHOD, bci);
                Object arg1 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg0 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, callNode.executeObject(frame, func, arg0, arg1));
                break;
            }
            case 3: {
                CallTernaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_TERNARY_METHOD, NODE_CALL_TERNARY_METHOD, bci);
                Object arg2 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg1 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg0 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, callNode.execute(frame, func, arg0, arg1, arg2));
                break;
            }
            case 4: {
                CallQuaternaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_QUATERNARY_METHOD, NODE_CALL_QUATERNARY_METHOD, bci);
                Object arg3 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg2 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg1 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg0 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, callNode.execute(frame, func, arg0, arg1, arg2, arg3));
                break;
            }
        }
        return stackTop;
    }

    private int bytecodeCallMethod(VirtualFrame frame, int stackTop, int bci, int oparg, String methodName, Node[] localNodes) {
        Object rcvr = frame.getObject(stackTop - oparg);
        PyObjectGetMethod getMethodNode = insertChildNode(localNodes[bci - 1], UNCACHED_OBJECT_GET_METHOD, NODE_OBJECT_GET_METHOD, bci - 1);
        Object func = getMethodNode.execute(frame, rcvr, methodName);
        switch (oparg) {
            case 0: {
                CallUnaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_UNARY_METHOD, NODE_CALL_UNARY_METHOD, bci);
                Object result = callNode.executeObject(frame, func, rcvr);
                frame.setObject(stackTop, result);
                break;
            }
            case 1: {
                CallBinaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_BINARY_METHOD, NODE_CALL_BINARY_METHOD, bci);
                Object result = callNode.executeObject(frame, func, rcvr, frame.getObject(stackTop));
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, result);
                break;
            }
            case 2: {
                CallTernaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_TERNARY_METHOD, NODE_CALL_TERNARY_METHOD, bci);
                Object arg1 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg0 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, callNode.execute(frame, func, rcvr, arg0, arg1));
                break;
            }
            case 3: {
                CallQuaternaryMethodNode callNode = insertChildNode(localNodes[bci], UNCACHED_CALL_QUATERNARY_METHOD, NODE_CALL_QUATERNARY_METHOD, bci);
                Object arg2 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg1 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                Object arg0 = frame.getObject(stackTop);
                frame.setObject(stackTop--, null);
                frame.setObject(stackTop, callNode.execute(frame, func, rcvr, arg0, arg1, arg2));
                break;
            }
        }
        return stackTop;
    }

    private int bytecodeStoreName(VirtualFrame frame, PythonLanguage lang, Object globals, Object locals, int stackTop, int bci, int oparg, String[] localNames, Node[] localNodes) {
        String varname = localNames[oparg];
        Object value = frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        if (locals != null) {
            Node helper = localNodes[bci - 1];
            if (helper instanceof HashingCollectionNodes.SetItemNode) {
                if (locals instanceof PDict && ((PDict) locals).getShape() == PythonBuiltinClassType.PDict.getInstanceShape(lang)) {
                    HashingCollectionNodes.SetItemNode setItemNode = (HashingCollectionNodes.SetItemNode) helper;
                    setItemNode.execute(frame, (PDict) locals, varname, value);
                    return stackTop;
                }
            }
            if (helper instanceof PyObjectSetItem) {
                ((PyObjectSetItem) helper).execute(frame, locals, varname, value);
                return stackTop;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert helper == null || helper == MARKER_NODE;
            if (locals instanceof PDict && ((PDict) locals).getShape() == PythonBuiltinClassType.PDict.getInstanceShape(lang)) {
                HashingCollectionNodes.SetItemNode newNode = insertChildNode(localNodes[bci - 1], UNCACHED_SET_ITEM, NODE_SET_ITEM, bci - 1);
                newNode.execute(frame, (PDict) locals, varname, value);
            } else {
                PyObjectSetItem newNode = insertChildNode(localNodes[bci - 1], UNCACHED_OBJECT_SET_ITEM, NODE_OBJECT_SET_ITEM, bci - 1);
                newNode.execute(frame, locals, varname, value);
            }
        } else {
            WriteGlobalNode writeGlobalNode = insertChildNode(localNodes[bci], UNCACHED_WRITE_GLOBAL, NODE_WRITE_GLOBAL, bci, varname);
            writeGlobalNode.write(frame, globals, varname, value);
        }
        return stackTop;
    }

    private int bytecodeRaiseVarargs(VirtualFrame frame, int stackTop, int bci, int oparg, Node[] localNodes) {
        RaiseNode raiseNode = insertChildNode(localNodes[bci], NODE_RAISENODE, bci);
        int arg = oparg;
        Object cause;
        Object exception;
        if (arg > 1) {
            cause = frame.getObject(stackTop);
            frame.setObject(stackTop--, null);
        } else {
            cause = PNone.NO_VALUE;
        }
        if (arg > 0) {
            exception = frame.getObject(stackTop);
            frame.setObject(stackTop--, null);
        } else {
            exception = PNone.NO_VALUE;
        }
        raiseNode.execute(frame, exception, cause);
        return stackTop;
    }

    private void raiseUnboundCell(Node node, int bci, int oparg) {
        PRaiseNode raiseNode = insertChildNode(node, UNCACHED_RAISE, NODE_RAISE, bci);
        if (oparg < freeoffset) {
            int varIdx = oparg - celloffset;
            throw raiseNode.raise(PythonBuiltinClassType.UnboundLocalError, ErrorMessages.LOCAL_VAR_REFERENCED_BEFORE_ASSIGMENT, cellvars[varIdx]);
        } else {
            int varIdx = oparg - freeoffset;
            throw raiseNode.raise(PythonBuiltinClassType.NameError, ErrorMessages.UNBOUNDFREEVAR, freevars[varIdx]);
        }
    }

    private int bytecodeImportName(VirtualFrame frame, PythonContext context, PythonModule builtins, Object globals, int initialStackTop, int bci, int oparg, String[] localNames,
                    Node[] localNodes) {
        CastToJavaIntExactNode castNode = insertChildNode(localNodes[bci - 1], UNCACHED_CAST_TO_JAVA_INT_EXACT, NODE_CAST_TO_JAVA_INT_EXACT, bci - 1);
        String modname = localNames[oparg];
        int stackTop = initialStackTop;
        String[] fromlist = (String[]) frame.getObject(stackTop);
        frame.setObject(stackTop--, null);
        int level = castNode.execute(frame.getObject(stackTop));
        ImportName importNode = insertChildNode(localNodes[bci], UNCACHED_IMPORT_NAME, NODE_IMPORT_NAME, bci);
        Object result = importNode.execute(frame, context, builtins, modname, globals, fromlist, level);
        frame.setObject(stackTop, result);
        return stackTop;
    }

    private int bytecodeImportFrom(VirtualFrame frame, int initialStackTop, int bci, int oparg, String[] localNames, Node[] localNodes) {
        int stackTop = initialStackTop;
        String name = localNames[oparg];
        Object from = frame.getObject(stackTop);
        ImportFromNode importFromNode = insertChildNode(localNodes[bci], UNCACHED_IMPORT_FROM, NODE_IMPORT_FROM, bci);
        Object imported = importFromNode.execute(frame, from, name);
        frame.setObject(++stackTop, imported);
        return stackTop;
    }

    private void initCellVars(VirtualFrame frame) {
        if (cellvars.length <= 32) {
            initCellVarsExploded(frame);
        } else {
            initCellVarsLoop(frame);
        }
    }

    @ExplodeLoop
    private void initCellVarsExploded(VirtualFrame frame) {
        for (int i = 0; i < cellvars.length; i++) {
            frame.setObject(celloffset + i, new PCell(cellEffectivelyFinalAssumptions[i]));
        }
    }

    private void initCellVarsLoop(VirtualFrame frame) {
        for (int i = 0; i < cellvars.length; i++) {
            frame.setObject(celloffset + i, new PCell(cellEffectivelyFinalAssumptions[i]));
        }
    }

    private void initFreeVars(VirtualFrame frame, Object[] originalArgs) {
        if (freevars.length > 0) {
            if (freevars.length <= 32) {
                initFreeVarsExploded(frame, originalArgs);
            } else {
                initFreeVarsLoop(frame, originalArgs);
            }
        }
    }

    @ExplodeLoop
    private void initFreeVarsExploded(VirtualFrame frame, Object[] originalArgs) {
        PCell[] closure = PArguments.getClosure(originalArgs);
        for (int i = 0; i < freevars.length; i++) {
            frame.setObject(freeoffset + i, closure[i]);
        }
    }

    private void initFreeVarsLoop(VirtualFrame frame, Object[] originalArgs) {
        PCell[] closure = PArguments.getClosure(originalArgs);
        for (int i = 0; i < freevars.length; i++) {
            frame.setObject(freeoffset + i, closure[i]);
        }
    }

    private void verifyInLoop(int stackTop, int bci, final byte bc) {
        CompilerAsserts.partialEvaluationConstant(bc);
        CompilerAsserts.partialEvaluationConstant(bci);
        CompilerAsserts.partialEvaluationConstant(stackTop);
    }

    private void verifyBeforeLoop(int target, int stackTop, int bci, byte[] localBC) {
        CompilerAsserts.partialEvaluationConstant(localBC);
        CompilerAsserts.partialEvaluationConstant(target);
        CompilerAsserts.partialEvaluationConstant(bci);
        CompilerAsserts.partialEvaluationConstant(stackTop);
    }

    private int bytecodeMakeFunction(VirtualFrame frame, Object globals, int lastStackTop, int oparg) {
        int stackTop = lastStackTop;
        CodeUnit newCode = (CodeUnit) frame.getObject(stackTop);

        PCell[] closure = null;
        Object annotations = null;
        PKeyword[] kwdefaults = null;
        Object[] defaults = null;

        if ((oparg & CodeUnit.HAS_CLOSURE) != 0) {
            frame.setObject(stackTop--, null);
            closure = (PCell[]) frame.getObject(stackTop);
        }
        if ((oparg & CodeUnit.HAS_ANNOTATIONS) != 0) {
            frame.setObject(stackTop--, null);
            annotations = frame.getObject(stackTop);
        }
        if ((oparg & CodeUnit.HAS_KWONLY_DEFAULTS) != 0) {
            frame.setObject(stackTop--, null);
            kwdefaults = (PKeyword[]) frame.getObject(stackTop);
        }
        if ((oparg & CodeUnit.HAS_DEFAULTS) != 0) {
            frame.setObject(stackTop--, null);
            defaults = (Object[]) frame.getObject(stackTop);
        }
        int posArgCount = newCode.argCount + newCode.positionalOnlyArgCount;
        String[] parameterNames = Arrays.copyOf(newCode.varnames, posArgCount);
        String[] kwOnlyNames = Arrays.copyOfRange(newCode.varnames, posArgCount, posArgCount + newCode.kwOnlyArgCount);
        int varArgsIndex = newCode.takesVarArgs() ? posArgCount : -1;
        Signature newSignature = new Signature(newCode.positionalOnlyArgCount,
                        newCode.takesVarKeywordArgs(),
                        varArgsIndex,
                        newCode.positionalOnlyArgCount > 0,
                        parameterNames,
                        kwOnlyNames);
        PBytecodeRootNode rootNode = new PBytecodeRootNode(PythonLanguage.get(this), newSignature, newCode, source);
        PCode codeobj = factory.createCode(rootNode.getCallTarget(), newSignature, newCode.nlocals, newCode.stacksize, newCode.flags,
                        newCode.constants, newCode.names, newCode.varnames, newCode.freevars, newCode.cellvars, newCode.filename, newCode.name,
                        newCode.startOffset, newCode.srcOffsetTable);
        frame.setObject(stackTop, factory.createFunction(newCode.name, null, codeobj, (PythonObject) globals, defaults, kwdefaults, closure));
        if (annotations != null) {
            DynamicObjectLibrary.getUncached().put((DynamicObject) frame.getObject(stackTop), __ANNOTATIONS__, annotations);
        }
        return stackTop;
    }

    @ExplodeLoop
    @SuppressWarnings("unchecked")
    private static <T> void moveFromStack(VirtualFrame frame, int start, int stop, T[] target) {
        CompilerAsserts.partialEvaluationConstant(start);
        CompilerAsserts.partialEvaluationConstant(stop);
        for (int j = 0, i = start; i < stop; i++, j++) {
            target[j] = (T) frame.getObject(i);
            frame.setObject(i, null);
        }
    }

    private int bytecodeCollectionFromStack(VirtualFrame frame, int type, int count, int oldStackTop, Node[] localNodes, int nodeIndex) {
        int stackTop = oldStackTop;
        Object res = null;
        switch (type) {
            case CollectionBits.LIST: {
                Object[] store = new Object[count];
                moveFromStack(frame, stackTop - count + 1, stackTop + 1, store);
                res = factory.createList(store);
                break;
            }
            case CollectionBits.TUPLE: {
                Object[] store = new Object[count];
                moveFromStack(frame, stackTop - count + 1, stackTop + 1, store);
                res = factory.createTuple(store);
                break;
            }
            case CollectionBits.SET: {
                PSet set = factory.createSet();
                HashingCollectionNodes.SetItemNode newNode = insertChildNode(localNodes[nodeIndex], UNCACHED_SET_ITEM, NODE_SET_ITEM, nodeIndex);
                for (int i = stackTop - count + 1; i <= stackTop; i++) {
                    newNode.execute(frame, set, frame.getObject(i), PNone.NONE);
                    frame.setObject(i, null);
                }
                res = set;
                break;
            }
            case CollectionBits.DICT: {
                PDict dict = factory.createDict();
                HashingCollectionNodes.SetItemNode setItem = insertChildNode(localNodes[nodeIndex], UNCACHED_SET_ITEM, NODE_SET_ITEM, nodeIndex);
                assert count % 2 == 0;
                for (int i = stackTop - count + 1; i <= stackTop; i += 2) {
                    setItem.execute(frame, dict, frame.getObject(i), frame.getObject(i + 1));
                    frame.setObject(i, null);
                    frame.setObject(i + 1, null);
                }
                res = dict;
                break;
            }
            case CollectionBits.KWORDS: {
                PKeyword[] kwds = new PKeyword[count];
                moveFromStack(frame, stackTop - count + 1, stackTop + 1, kwds);
                res = kwds;
                break;
            }
            case CollectionBits.OBJECT: {
                Object[] objs = new Object[count];
                moveFromStack(frame, stackTop - count + 1, stackTop + 1, objs);
                res = objs;
                break;
            }
        }
        stackTop -= count;
        frame.setObject(++stackTop, res);
        return stackTop;
    }

    private void bytecodeCollectionFromCollection(VirtualFrame frame, int type, int stackTop, Node[] localNodes, int nodeIndex) {
        Object sourceCollection = frame.getObject(stackTop);
        Object result;
        switch (type) {
            case CollectionBits.LIST: {
                ListNodes.ConstructListNode constructNode = insertChildNode(localNodes[nodeIndex], UNCACHED_CONSTRUCT_LIST, NODE_CONSTRUCT_LIST, nodeIndex);
                result = constructNode.execute(frame, sourceCollection);
                break;
            }
            case CollectionBits.TUPLE: {
                TupleNodes.ConstructTupleNode constructNode = insertChildNode(localNodes[nodeIndex], UNCACHED_CONSTRUCT_TUPLE, NODE_CONSTRUCT_TUPLE, nodeIndex);
                result = constructNode.execute(frame, sourceCollection);
                break;
            }
            case CollectionBits.SET: {
                SetNodes.ConstructSetNode constructNode = insertChildNode(localNodes[nodeIndex], UNCACHED_CONSTRUCT_SET, NODE_CONSTRUCT_SET, nodeIndex);
                result = constructNode.executeWith(frame, sourceCollection);
                break;
            }
            case CollectionBits.DICT: {
                // TODO create uncached node
                HashingStorage.InitNode initNode = insertChildNode(localNodes[nodeIndex], NODE_HASHING_STORAGE_INIT, nodeIndex);
                HashingStorage storage = initNode.execute(frame, sourceCollection, PKeyword.EMPTY_KEYWORDS);
                result = factory.createDict(storage);
                break;
            }
            case CollectionBits.OBJECT: {
                ExecutePositionalStarargsNode executeStarargsNode = insertChildNode(localNodes[nodeIndex], UNCACHED_EXECUTE_STARARGS, NODE_EXECUTE_STARARGS, nodeIndex);
                result = executeStarargsNode.executeWith(frame, sourceCollection);
                break;
            }
            case CollectionBits.KWORDS: {
                ExpandKeywordStarargsNode expandKeywordStarargsNode = insertChildNode(localNodes[nodeIndex], UNCACHED_EXPAND_KEYWORD_STARARGS, NODE_EXPAND_KEYWORD_STARARGS, nodeIndex);
                result = expandKeywordStarargsNode.execute(sourceCollection);
                break;
            }
            default:
                throw CompilerDirectives.shouldNotReachHere("Unexpected collection type");
        }
        frame.setObject(stackTop, result);
    }

    private int bytecodeCollectionAddCollection(VirtualFrame frame, int type, int initialStackTop, Node[] localNodes, int nodeIndex) {
        int stackTop = initialStackTop;
        Object collection1 = frame.getObject(stackTop - 1);
        Object collection2 = frame.getObject(stackTop);
        Object result;
        switch (type) {
            case CollectionBits.LIST: {
                // TODO uncached node
                ListBuiltins.ListExtendNode extendNode = insertChildNode(localNodes[nodeIndex], NODE_LIST_EXTEND, nodeIndex);
                extendNode.execute(frame, (PList) collection1, collection2);
                result = collection1;
                break;
            }
            case CollectionBits.SET: {
                SetBuiltins.UpdateSingleNode updateNode = insertChildNode(localNodes[nodeIndex], UNCACHED_SET_UPDATE, NODE_SET_UPDATE, nodeIndex);
                PSet set = (PSet) collection1;
                set.setDictStorage(updateNode.execute(frame, set.getDictStorage(), collection2));
                result = set;
                break;
            }
            case CollectionBits.DICT: {
                // TODO uncached node
                DictNodes.UpdateNode updateNode = insertChildNode(localNodes[nodeIndex], NODE_DICT_UPDATE, nodeIndex);
                updateNode.execute(frame, (PDict) collection1, collection2);
                result = collection1;
                break;
            }
            // Note: we don't allow this operation for tuple
            case CollectionBits.OBJECT: {
                Object[] array1 = (Object[]) collection1;
                ExecutePositionalStarargsNode executeStarargsNode = insertChildNode(localNodes[nodeIndex], UNCACHED_EXECUTE_STARARGS, NODE_EXECUTE_STARARGS, nodeIndex);
                Object[] array2 = executeStarargsNode.executeWith(frame, collection2);
                Object[] combined = new Object[array1.length + array2.length];
                System.arraycopy(array1, 0, combined, 0, array1.length);
                System.arraycopy(array2, 0, combined, array1.length, array2.length);
                result = combined;
                break;
            }
            case CollectionBits.KWORDS: {
                PKeyword[] array1 = (PKeyword[]) collection1;
                ExpandKeywordStarargsNode expandKeywordStarargsNode = insertChildNode(localNodes[nodeIndex], UNCACHED_EXPAND_KEYWORD_STARARGS, NODE_EXPAND_KEYWORD_STARARGS, nodeIndex);
                PKeyword[] array2 = expandKeywordStarargsNode.execute(collection2);
                PKeyword[] combined = new PKeyword[array1.length + array2.length];
                System.arraycopy(array1, 0, combined, 0, array1.length);
                System.arraycopy(array2, 0, combined, array1.length, array2.length);
                result = combined;
                break;
            }
            default:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw PRaiseNode.getUncached().raise(SystemError, "Invalid type for COLLECTION_ADD_COLLECTION");
        }
        frame.setObject(stackTop--, null);
        frame.setObject(stackTop, result);
        return stackTop;
    }

    private int bytecodeAddToCollection(VirtualFrame frame, int initialStackTop, int nodeIndex, Node[] localNodes, int depth, int type) {
        int stackTop = initialStackTop;
        Object collection = frame.getObject(stackTop - depth);
        Object item = frame.getObject(stackTop);
        switch (type) {
            case CollectionBits.LIST: {
                ListNodes.AppendNode appendNode = insertChildNode(localNodes[nodeIndex], UNCACHED_LIST_APPEND, NODE_LIST_APPEND, nodeIndex);
                appendNode.execute((PList) collection, item);
                break;
            }
            case CollectionBits.SET: {
                SetNodes.AddNode addNode = insertChildNode(localNodes[nodeIndex], UNCACHED_SET_ADD, NODE_SET_ADD, nodeIndex);
                addNode.execute(frame, (PSet) collection, item);
                break;
            }
            case CollectionBits.DICT: {
                Object key = frame.getObject(stackTop - 1);
                HashingCollectionNodes.SetItemNode setItem = insertChildNode(localNodes[nodeIndex], UNCACHED_SET_ITEM, NODE_SET_ITEM, nodeIndex);
                setItem.execute(frame, (PDict) collection, key, item);
                frame.setObject(stackTop--, null);
                break;
            }
            default:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw PRaiseNode.getUncached().raise(SystemError, "Invalid type for ADD_TO_COLLECTION");
        }
        frame.setObject(stackTop--, null);
        return stackTop;
    }

    private int bytecodeUnpackSequence(VirtualFrame frame, int stackTop, int bci, Node[] localNodes, int count) {
        UnpackSequenceNode unpackNode = insertChildNode(localNodes[bci], UNCACHED_UNPACK_SEQUENCE, NODE_UNPACK_SEQUENCE, bci);
        Object collection = frame.getObject(stackTop);
        unpackNode.execute(frame, stackTop - 1, collection, count);
        return stackTop - 1 + count;
    }

    /**
     * @see #saveExceptionBlockstack
     */
    @ExplodeLoop
    private long findHandler(int bci) {
        CompilerAsserts.partialEvaluationConstant(bci);

        int targetBCI = -1;
        int targetStackTop = -1;
        for (int i = 0; i < exceptionHandlerRanges.length; i += 3) {
            // The ranges are ordered by their end bci, so the first one found is the most specific
            // one
            if (bci >= exceptionHandlerRanges[i] && bci < exceptionHandlerRanges[i + 1]) {
                // bci is inside this try-block range. get the target stack size
                targetBCI = exceptionHandlerRanges[i + 1];
                targetStackTop = exceptionHandlerRanges[i + 2] & 0xffff;
                break;
            }
        }
        if (targetBCI == -1) {
            return -1;
        } else {
            return encodeBCI(targetBCI) | encodeStackTop(targetStackTop);
        }
    }

    @ExplodeLoop
    private static int unwindBlock(VirtualFrame frame, int stackTop, int stackTopBeforeBlock) {
        CompilerAsserts.partialEvaluationConstant(stackTop);
        CompilerAsserts.partialEvaluationConstant(stackTopBeforeBlock);
        for (int i = stackTop; i > stackTopBeforeBlock; i--) {
            frame.setObject(i, null);
        }
        return stackTopBeforeBlock;
    }

    public PCell readClassCell(Frame frame) {
        if (classcellIndex < 0) {
            return null;
        }
        return (PCell) frame.getObject(classcellIndex);
    }

    public Object readSelf(Frame frame) {
        if (signature.takesNoArguments()) {
            return null;
        }
        return frame.getObject(0);
    }

    public int getStartOffset() {
        return co.startOffset;
    }

    @TruffleBoundary
    public static int bciToLine(CodeUnit code, Source source, int bci) {
        if (source != null && bci >= 0) {
            return source.createSection(code.bciToSrcOffset(bci), 0).getStartLine();
        }
        return -1;
    }

    public int bciToLine(int bci) {
        return bciToLine(co, source, bci);
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceSection != null) {
            return sourceSection;
        } else if (source == null) {
            return null;
        } else {
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int bci = 0; bci < co.code.length; bci++) {
                int offset = co.bciToSrcOffset(bci);
                min = Math.min(min, offset);
                max = Math.max(max, offset);
            }
            sourceSection = source.createSection(min, max - min);
            return sourceSection;
        }
    }
}
