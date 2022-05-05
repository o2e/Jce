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


public interface JceStruct {

    static final byte BYTE = 0;
    static final byte SHORT = 1;
    static final byte INT = 2;
    static final byte LONG = 3;
    static final byte FLOAT = 4;
    static final byte DOUBLE = 5;
    static final byte STRING1 = 6;
    static final byte STRING4 = 7;
    static final byte MAP = 8;
    static final byte LIST = 9;
    static final byte STRUCT_BEGIN = 10;
    static final byte STRUCT_END = 11;
    static final byte ZERO_TAG = 12;
    static final byte SIMPLE_LIST = 13;

    static final int MAX_STRING_LENGTH = 100 * 1024 * 1024;

    default void writeTo(JceOutputStream output) {

    }

    default void readFrom(JceInputStream input) {
    }

    default void readFrom(byte[] bytes) {
        readFrom(new JceInputStream(bytes));
    }

//    public void display(StringBuilder sb, int level) {
//    }
//
//    public void displaySimple(StringBuilder sb, int level) {
//    }

    default JceStruct newInit() {
        throw new RuntimeException("newInit not implement");
    }

    default void recycle() {
        throw new RuntimeException("newInit not implement");
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

    default byte[] toByteArray() {
        JceOutputStream os = new JceOutputStream();
        writeTo(os);
        return os.toByteArray();
    }

    default byte[] toByteArray(Charset encoding) {
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
    default String servantName() {
        throw new RuntimeException("servantName not implement, Please @Tars(servantName = yourName)");
    }

    @NotNull
    default String funcName() {
        throw new RuntimeException("funcName not implement, Please @Tars(funcName = yourName)");
    }

    @NotNull
    default String reqName() {
        throw new RuntimeException("reqName not implement, Please @Tars(reqName = yourName)");
    }

    @NotNull
    default String respName() {
        throw new RuntimeException("respName not implement, Please @Tars(respName = yourName)");
    }
}
