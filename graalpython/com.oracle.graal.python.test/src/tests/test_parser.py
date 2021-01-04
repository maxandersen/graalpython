# Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.


def test_lambdas_as_function_default_argument_values():
    globs = {}
    exec("""
def x(aaaa, bbbb=None, cccc={}, dddd=lambda name: '*'+name):
    return aaaa, bbbb, cccc, dddd(aaaa)

retval = x('hello')
    """, globs)
    assert globs["retval"] == ('hello', None, {}, '*hello')


def test_required_kw_arg():
    globs = {}
    exec("""
def x(*, a=None, b):
    return a, b

try:
    x()
except TypeError:
    assert True
else:
    assert False

retval = x(b=42)
    """, globs)
    assert globs["retval"] == (None, 42)


def test_syntax_error_simple():
    globs = {}
    was_exception = False
    try:
        exec("""c = a += 3""", globs)
    except SyntaxError:
        was_exception = True
    assert was_exception


def test_lambda_no_args_with_nested_lambdas():
    no_err = True
    try:
        eval("lambda: ((lambda args: args[0], ), (lambda args: args[1], ), )")
    except Exception as e:
        no_err = False
    assert no_err


def test_byte_numeric_escapes():
    assert eval('b"PK\\005\\006\\00\\11\\22\\08"') == b'PK\x05\x06\x00\t\x12\x008'


def test_decorator_cell():
    foo = lambda x: "just a string, not %s" % x.__name__
    def run_me():
        @foo
        def func():
            pass
        return func
    assert run_me() == "just a string, not func", run_me()


def test_single_input_non_interactive():
    import sys
    oldhook = sys.displayhook
    got_value = None
    def newhook(value):
        nonlocal got_value
        got_value = value
    sys.displayhook = newhook
    try:
        code = compile('sum([1, 2, 3])', '', 'single')
        assert exec(code) == None
        assert got_value == 6
    finally:
        sys.displayhook = oldhook


def test_underscore_in_numbers():
    assert eval('1_0') == 10
    assert eval('0b1_1') == 0b11
    assert eval('0o1_7') == 0o17
    assert eval('0x1_f') == 0x1f


def test_annotation_scope():
    def foo(object: object):
        pass
    assert foo.__annotations__['object'] == object


import sys

def assert_raise_syntax_error(source, msg):
    try:
        compile(source, "", "single")
    except SyntaxError as e:
        assert msg in str(e), "\nCode:\n----\n%s\n----\n  Expected message: %s\n  Actual message: %s" % (source, msg, str(e))
    else:
        assert False , "Syntax Error was not raised.\nCode:\n----\n%s\n----\nhas to raise Syntax Error: %s" % (source,msg) 

def test_cannot_assign():
    if sys.implementation.version.minor >= 8:
        def check(s, msg):
            # test simple assignment
            assert_raise_syntax_error("%s = 1" % s, msg)
            # testing aug assignment
            assert_raise_syntax_error("%s += 1" % s, msg)
            # test with statement
            assert_raise_syntax_error("with foo as %s:\n pass" % s, msg)
            # test for statement
            assert_raise_syntax_error("for %s in range(1,10):\n pass" % s, msg)
            # test for comprehension statement
            assert_raise_syntax_error("[1 for %s in range(1,10)]" % s, msg)
        check("1", "cannot assign to literal")
        check("1.1", "cannot assign to literal")
        check("{1}", "cannot assign to set display")
        check("{}", "cannot assign to dict display")
        check("{1: 2}", "cannot assign to dict display")
        check("[1,2, 3]", "cannot assign to literal")
        check("(1,2, 3)", "cannot assign to literal")
        check("1.2j", "cannot assign to literal")
        check("None", "cannot assign to None")
        check("...", "cannot assign to Ellipsis")
        check("True", "cannot assign to True")
        check("False", "cannot assign to False")
        check("b''", "cannot assign to literal")
        check("''", "cannot assign to literal")
        check("f''", "cannot assign to f-string expression")
        check("(a,None, b)", "cannot assign to None")
        check("(a,True, b)", "cannot assign to True")
        check("(a,False, b)", "cannot assign to False")
        check("a+b", "cannot assign to operator")
        check("fn()", "cannot assign to function call")
        check("{letter for letter in 'ahoj'}", "cannot assign to set comprehension")
        check("[letter for letter in 'ahoj']", "cannot assign to list comprehension")
        check("(letter for letter in 'ahoj')", "cannot assign to generator expression")
        check("obj.True", "invalid syntax")
        check("(a, *True, b)", "cannot assign to True")
        check("(a, *False, b)", "cannot assign to False")
        check("(a, *None, b)", "cannot assign to None")
        check("(a, *..., b)", "cannot assign to Ellipsis")
        check("__debug__", "cannot assign to __debug__")
        check("a.__debug__", "cannot assign to __debug__")
        check("a.b.__debug__", "cannot assign to __debug__")

def test_cannot_assign_without_with():
    if sys.implementation.version.minor >= 8:
        def check(s, msg):
            # test simple assignment
            assert_raise_syntax_error("%s = 1" % s, msg)
            # testing aug assignment
            assert_raise_syntax_error("%s += 1" % s, msg)
            # test for statement
            assert_raise_syntax_error("for %s in range(1,10):\n pass" % s, msg)
            # test for comprehension statement
            assert_raise_syntax_error("[1 for %s in range(1,10)]" % s, msg)
        check("*True", "cannot assign to True")
        check("*False", "cannot assign to False")
        check("*None", "cannot assign to None")
        check("*...", "cannot assign to Ellipsis")
        check("[a, b, c + 1],", "cannot assign to operator")

def test_cannot_assign_other():
    if sys.implementation.version.minor >= 8:
        assert_raise_syntax_error("a if 1 else b = 1", "cannot assign to conditional expression")
        assert_raise_syntax_error("a if 1 else b -= 1", "cannot assign to conditional expression")
        assert_raise_syntax_error("f(True=2)", "cannot assign to True")
        assert_raise_syntax_error("f(__debug__=2)", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(__debug__): pass\n", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(*, x=lambda __debug__:0): pass\n", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(*args:(lambda __debug__:0)): pass\n", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(**kwargs:(lambda __debug__:0)): pass\n", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(**__debug__): pass\n", "cannot assign to __debug__")
        assert_raise_syntax_error("def f(*xx, __debug__): pass\n", "cannot assign to __debug__")

def test_invalid_assignmetn_to_yield_expression():
    if sys.implementation.version.minor >= 8:
        assert_raise_syntax_error("def fn():\n (yield 10) = 1", "cannot assign to yield expression")
        assert_raise_syntax_error("def fn():\n (yield 10) += 1", "cannot assign to yield expression")
        assert_raise_syntax_error("def fn():\n with foo as (yield 10) : pass", "cannot assign to yield expression")
        assert_raise_syntax_error("def fn():\n for (yield 10) in range(1,10): pass", "cannot assign to yield expression") 

def test_invalid_ann_assignment():
    if sys.implementation.version.minor >= 8:
        assert_raise_syntax_error("lambda: x:x", 'illegal target for annotation')
        assert_raise_syntax_error("list(): int", "illegal target for annotation")
        assert_raise_syntax_error("{}: int", "illegal target for annotation")
        assert_raise_syntax_error("{1,2}: int", "illegal target for annotation")
        assert_raise_syntax_error("{'1':'2'}: int", "illegal target for annotation")
        assert_raise_syntax_error("[1,2]: int", "only single target (not list) can be annotated")
        assert_raise_syntax_error("(1,2): int", "only single target (not tuple) can be annotated")

def test_invalid_aug_assignment():
    if sys.implementation.version.minor >= 8:
        assert_raise_syntax_error("[] += 1", 'illegal expression for augmented assignment')
        assert_raise_syntax_error("() += 1", 'illegal expression for augmented assignment')
        assert_raise_syntax_error("x, y += (1, 2)", 'illegal expression for augmented assignment')
        assert_raise_syntax_error("x, y += 1", 'illegal expression for augmented assignment')

def test_invalid_star_import():
    assert_raise_syntax_error("def f(): from bla import *\n", 'import * only allowed at module level')

def test_invalid_return_statement():
    assert_raise_syntax_error("return 10", "'return' outside function")
    assert_raise_syntax_error("class A: return 10\n", "'return' outside function")


def test_mangled_class_property():
    class P:
        __property = 1
        def get_property(self):
            return self.__property
        def get_mangled_property(self):
            return self._P__property

    p = P()
    assert P._P__property == 1
    assert "_P__property" in dir(P)
    assert "__property" not in dir(P)
    assert p._P__property == 1
    assert "_P__property" in dir(p)
    assert "__property" not in dir(p)
    assert p.get_property() == 1
    assert p.get_mangled_property() == 1

    try:
        print(P.__property)
    except AttributeError as ae:
        pass
    else:
        assert False, "AttributeError was not raised"

    try:
        print(p.__property)
    except AttributeError as ae:
        pass
    else:
        assert False, "AttributeError was not raised"

    class B:
        __ = 2

    b = B()
    assert B.__ == 2
    assert "__" in dir(B)
    assert b.__ == 2
    assert "__" in dir(b)

    class C:
        ___ = 3

    c = C()
    assert C.___ == 3
    assert "___" in dir(C)
    assert c.___ == 3
    assert "___" in dir(c)

    class D:
        _____ = 4

    d = D()
    assert D._____ == 4
    assert "_____" in dir(D)
    assert d._____ == 4
    assert "_____" in dir(d)

    class E:
        def __init__(self, value):
            self.__value = value
        def getValue(self):
            return self.__value

    e = E(5)
    assert e.getValue() == 5
    assert e._E__value == 5
    assert "_E__value" in dir(e)
    assert "__value" not in dir(e) 

    class F:
        def __init__(self, value):
            self.__a = value
        def get(self):
            return self.__a
     
    f = F(5)
    assert "_F__a" in dir(f)
    assert "__a" not in dir(f)
     
def test_underscore_class_name():
    class _:
        __a = 1
    
    assert _.__a == 1
    assert "__a" in dir(_)
    

def test_mangled_class_method():
    class A:
        ___sound = "hello"
        def __hello(self):
            return self.___sound
        def hello(self):
            return self.__hello()

    a = A()
    assert "_A__hello" in dir(A)
    assert "__hello" not in dir(A)
    assert a.hello() == 'hello'
    
    try:
        print(A.__hello)
    except AttributeError as ae:
        pass
    else:
        assert False, "AttributeError was not raised"

    try:
        print(a.__hello)
    except AttributeError as ae:
        pass
    else:
        assert False, "AttributeError was not raised"

def test_mangled_private_class():
    class __P:
        __property = 1
    
    p = __P()
    assert __P._P__property == 1
    assert "_P__property" in dir(__P)
    assert "__property" not in dir(__P)
    assert p._P__property == 1
    assert "_P__property" in dir(p)
    assert "__property" not in dir(p)

    class __:
        __property = 2

    p = __()
    assert __.__property == 2
    assert "__property" in dir(__)
    assert p.__property == 2
    assert "__property" in dir(p)
   

    class _____Testik__:
        __property = 3

    p = _____Testik__()
    assert _____Testik__._Testik____property == 3
    assert "_Testik____property" in dir(_____Testik__)
    assert "__property" not in dir(_____Testik__)
    assert p._Testik____property == 3
    assert "_Testik____property" in dir(p)
    assert "__property" not in dir(p)

def test_mangled_import():
    class X:
        def fn(self):
            import __mangled_module

    assert '_X__mangled_module' in X.fn.__code__.co_varnames
    assert '__mangled_module' not in X.fn.__code__.co_varnames
    

def test_mangled_params():
    def xf(__param):
        return __param
    assert '__param' in xf.__code__.co_varnames
    assert xf(10) == 10

    class X:
        def m1(self, __param):
            return __param
        def m2(self, *__arg):
            return __arg
        def m3(self, **__kw):
            return __kw
        

    assert '_X__param' in X.m1.__code__.co_varnames
    assert '__param' not in X.m1.__code__.co_varnames
    assert X().m1(11) == 11

    assert '_X__arg' in X.m2.__code__.co_varnames
    assert '__arg' not in X.m2.__code__.co_varnames
    assert X().m2(1, 2, 3) == (1,2,3)
    
    assert '_X__kw' in X.m3.__code__.co_varnames
    assert '__kw' not in X.m3.__code__.co_varnames
    assert X().m3(a = 1, b = 2) == {'a':1, 'b':2}

def test_mangled_local_vars():
    class L:
        def fn(self, i):
            __result = 0
            for __index in range (1, i + 1):
                __result += __index
            return __result

    assert '_L__result' in L.fn.__code__.co_varnames
    assert '__result' not in L.fn.__code__.co_varnames
    assert '_L__index' in L.fn.__code__.co_varnames
    assert '__index' not in L.fn.__code__.co_varnames
    assert L().fn(5) == 15


def test_mangled_inner_function():
    def find_code_object(code_object, name):
        import types
        for object in code_object.co_consts:
            if type(object) == types.CodeType and object.co_name == name:
                return object

    class L:
        def fn(self, i):
            def __help(__i):
                __result = 0
                for __index in range (1, __i + 1):
                    __result += __index
                return __result
            return __help(i)

    assert '_L__help' in L.fn.__code__.co_varnames
    assert '__help' not in L.fn.__code__.co_varnames

    # CPython has stored as name of the code object the non mangle name. The question is, if this is right. 
    co = find_code_object(L.fn.__code__, '__help')
    if co is None:
        co = find_code_object(L.fn.__code__, '_L__help')
    assert co is not None
    assert '_L__result' in co.co_varnames
    assert '__result' not in co.co_varnames
    assert '_L__index' in co.co_varnames
    assert '__index' not in co.co_varnames
    assert L().fn(5) == 15
    

def test_mangled_default_value_param():
    class D:
        def fn(self, __default = 5):
            return __default

    assert D().fn(_D__default = 11) == 11

    try:
        D().fn(__default = 11)
    except TypeError as ae:
        pass
    else:
        assert False, "TypeError was not raised"


def test_mangled_slots():
    class SlotClass:
        __slots__ = ("__mangle_me", "do_not_mangle_me")
        def __init__(self):
            self.__mangle_me = 123
            self.do_not_mangle_me = 456

    b = SlotClass()
    assert b._SlotClass__mangle_me == 123
    assert b.do_not_mangle_me == 456


def test_method_decorator():
    class S:
        def __init__(self, value):
            self.value = value
        @property
        def __ser(self):
            return self.value
        @__ser.setter
        def __ser(self, value):
            self.value = value

    s = S(10)
    assert s.value == 10
    assert s._S__ser == 10
    assert '_S__ser' in dir(S)
    assert '_S__ser' in dir(s)
    assert '__ser' not in dir(S)
    assert '__ser' not in dir(s)

    s.value = 11
    assert s._S__ser == 11

    s._S__ser = 12
    assert s.value == 12

    s.__ser = 13
    assert s.value == 12
    assert s.__ser == 13
    assert s._S__ser == 12
