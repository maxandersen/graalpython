/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.runtime;

// Auto generated by gen_native_cfg.py at 2021-03-15 12:54:37.728221
// on Darwin 18.7.0 Darwin Kernel Version 18.7.0: Tue Aug 20 16:57:14 PDT 2019; root:xnu-4903.271.2~2/RELEASE_X86_64 x86_64
class PosixConstantsDarwin {

    private PosixConstantsDarwin() {
    }

    static void getConstants(PosixConstants.Registry constants) {
        constants.put("HAVE_FUTIMENS", false);
        constants.put("HAVE_UTIMENSAT", false);
        constants.put("FD_SETSIZE", 1024);
        constants.put("PATH_MAX", 1024);
        constants.put("L_ctermid", 1024);
        constants.put("AT_FDCWD", -2);
        constants.put("SEEK_SET", 0);
        constants.put("SEEK_CUR", 1);
        constants.put("SEEK_END", 2);
        constants.put("SEEK_DATA", 4);
        constants.put("SEEK_HOLE", 3);
        constants.put("O_ACCMODE", 0x00000003);
        constants.put("O_RDONLY", 0x00000000);
        constants.put("O_WRONLY", 0x00000001);
        constants.put("O_RDWR", 0x00000002);
        constants.put("O_CREAT", 0x00000200);
        constants.put("O_EXCL", 0x00000800);
        constants.put("O_TRUNC", 0x00000400);
        constants.put("O_APPEND", 0x00000008);
        constants.put("O_NONBLOCK", 0x00000004);
        constants.put("O_NDELAY", 0x00000004);
        constants.put("O_DSYNC", 0x00400000);
        constants.put("O_CLOEXEC", 0x01000000);
        constants.put("O_SYNC", 0x00000080);
        constants.put("S_IFMT", 0x0000F000);
        constants.put("S_IFSOCK", 0x0000C000);
        constants.put("S_IFLNK", 0x0000A000);
        constants.put("S_IFREG", 0x00008000);
        constants.put("S_IFBLK", 0x00006000);
        constants.put("S_IFDIR", 0x00004000);
        constants.put("S_IFCHR", 0x00002000);
        constants.put("S_IFIFO", 0x00001000);
        constants.put("MAP_SHARED", 0x00000001);
        constants.put("MAP_PRIVATE", 0x00000002);
        constants.put("MAP_ANONYMOUS", 0x00001000);
        constants.put("PROT_NONE", 0x00000000);
        constants.put("PROT_READ", 0x00000001);
        constants.put("PROT_WRITE", 0x00000002);
        constants.put("PROT_EXEC", 0x00000004);
        constants.put("LOCK_SH", 0x00000001);
        constants.put("LOCK_EX", 0x00000002);
        constants.put("LOCK_NB", 0x00000004);
        constants.put("LOCK_UN", 0x00000008);
        constants.put("F_RDLCK", 1);
        constants.put("F_WRLCK", 3);
        constants.put("F_UNLCK", 2);
        constants.put("DT_UNKNOWN", 0);
        constants.put("DT_FIFO", 1);
        constants.put("DT_CHR", 2);
        constants.put("DT_DIR", 4);
        constants.put("DT_BLK", 6);
        constants.put("DT_REG", 8);
        constants.put("DT_LNK", 10);
        constants.put("DT_SOCK", 12);
        constants.put("DT_WHT", 14);
        constants.put("WNOHANG", 1);
        constants.put("WUNTRACED", 2);
        constants.put("R_OK", 0x00000004);
        constants.put("W_OK", 0x00000002);
        constants.put("X_OK", 0x00000001);
        constants.put("F_OK", 0x00000000);
        constants.put("RTLD_LAZY", 0x00000001);
        constants.put("RTLD_NOW", 0x00000002);
        constants.put("RTLD_GLOBAL", 0x00000008);
        constants.put("RTLD_LOCAL", 0x00000004);
    }
}
