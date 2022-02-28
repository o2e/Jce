/**
 * Tencent is pleased to support the open source community by making Tars available.
 * <p>
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 * <p>
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * https://opensource.org/licenses/BSD-3-Clause
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.qq.taf.jce;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;


public class JceStruct {

    protected static final byte BYTE = 0;
    protected static final byte SHORT = 1;
    protected static final byte INT = 2;
    protected static final byte LONG = 3;
    protected static final byte FLOAT = 4;
    protected static final byte DOUBLE = 5;
    protected static final byte STRING1 = 6;
    protected static final byte STRING4 = 7;
    protected static final byte MAP = 8;
    protected static final byte LIST = 9;
    protected static final byte STRUCT_BEGIN = 10;
    protected static final byte STRUCT_END = 11;
    protected static final byte ZERO_TAG = 12;
    protected static final byte SIMPLE_LIST = 13;

    protected static final int MAX_STRING_LENGTH = 100 * 1024 * 1024;

    public void writeTo(JceOutputStream output) {
    }

    public void readFrom(JceInputStream input) {
    }

    public void readFrom(byte[] bytes) {
        readFrom(new JceInputStream(bytes));
    }

//    public void display(StringBuilder sb, int level) {
//    }
//
//    public void displaySimple(StringBuilder sb, int level) {
//    }

    public JceStruct newInit() {
        throw new RuntimeException("newInit not implement");
    }

    public void recycle() {
//        throw new RuntimeException("newInit not implement");
    }
//
//    public boolean containField(String name) {
//        return false;
//    }
//
//    public Object getFieldByName(String name) {
//        return null;
//    }
//
//    public void setFieldByName(String name, Object value) {
//    }

    public byte[] toByteArray() {
        JceOutputStream os = new JceOutputStream();
        writeTo(os);
        return os.toByteArray();
    }

    public byte[] toByteArray(Charset encoding) {
        JceOutputStream os = new JceOutputStream();
        os.setServerEncoding(encoding);
        writeTo(os);
        return os.toByteArray();
    }

//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        display(sb, 0);
//        return sb.toString();
//    }
//
//    public static String toDisplaySimpleString(JceStruct struct) {
//        if (struct == null) {
//            return null;
//        }
//        StringBuilder sb = new StringBuilder();
//        struct.displaySimple(sb, 0);
//        return sb.toString();
//    }

    @NotNull
    public String servantName() {
        throw new RuntimeException("servantName not implement, Please @Tars(servantName = yourName)");
    }

    @NotNull
    public String funcName() {
        throw new RuntimeException("funcName not implement, Please @Tars(funcName = yourName)");
    }

    @NotNull
    public String reqName() {
        throw new RuntimeException("reqName not implement, Please @Tars(reqName = yourName)");
    }

    @NotNull
    public String respName() {
        throw new RuntimeException("respName not implement, Please @Tars(respName = yourName)");
    }
}
