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

import com.qq.taf.jce.exc.JceDecodeException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import com.qq.taf.jce.util.HexBin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JceInputStream {
    //private ByteBuffer bs; // 缓冲区
    private ByteBuf bs;

    public static class HeadData {
        public byte type;
        public int tag;

        public void clear() {
            type = 0;
            tag = 0;
        }
    }

    public JceInputStream() {

    }

    public JceInputStream(ByteBuf bs) {
        bs.resetReaderIndex();
        this.bs = bs;
    }

    public JceInputStream(byte[] bs) {
        this.bs = Unpooled.copiedBuffer(bs);
    }

    public JceInputStream(byte[] bs, int pos) {
        this.bs = Unpooled.copiedBuffer(bs);
        this.bs.readerIndex(pos);
    }

    public void warp(byte[] bs) {
        wrap(bs);
    }

    public void wrap(byte[] bs) {
        //this.bs = ByteBuffer.wrap(bs);
        this.bs = Unpooled.wrappedBuffer(bs);
    }

    public static int readHead(HeadData hd, ByteBuf bb) {
        byte b = bb.readByte();
        hd.type = (byte) (b & 15);
        hd.tag = ((b & (15 << 4)) >> 4);
        if (hd.tag == 15) {
            hd.tag = (bb.readByte() & 0x00ff);
            return 2;
        }
        return 1;
    }

    public void readHead(HeadData hd) {
        readHead(hd, bs);
    }

    private int peakHead(HeadData hd) {
        return readHead(hd, bs.duplicate());
    }

    private void skip(int len) {
        //bs.position(bs.position() + len);
        bs.readerIndex(bs.readerIndex() + len);
    }

    public boolean skipToTag(int tag) {
        try {
            HeadData hd = new HeadData();
            while (true) {
                int len = peakHead(hd);
                if (hd.type == JceStruct.STRUCT_END) {
                    return false;
                }
                if (tag <= hd.tag) return tag == hd.tag;
                skip(len);
                skipField(hd.type);
            }
        } catch (JceDecodeException | BufferUnderflowException | IndexOutOfBoundsException e) {
        }
        return false;
    }

    public void skipToStructEnd() {
        HeadData hd = new HeadData();
        do {
            readHead(hd);
            skipField(hd.type);
        } while (hd.type != JceStruct.STRUCT_END);
    }

    private void skipField() {
        HeadData hd = new HeadData();
        readHead(hd);
        skipField(hd.type);
    }

    private void skipField(byte type) {
        switch (type) {
            case JceStruct.BYTE:
                skip(1);
                break;
            case JceStruct.SHORT:
                skip(2);
                break;
            case JceStruct.INT:
                skip(4);
                break;
            case JceStruct.LONG:
                skip(8);
                break;
            case JceStruct.FLOAT:
                skip(4);
                break;
            case JceStruct.DOUBLE:
                skip(8);
                break;
            case JceStruct.STRING1: {
                //int len = bs.get();
                int len = bs.readByte();
                if (len < 0) len += 256;
                skip(len);
                break;
            }
            case JceStruct.STRING4: {
                skip(bs.readInt());
                break;
            }
            case JceStruct.MAP: {
                int size = read(0, 0, true);
                for (int i = 0; i < size * 2; ++i)
                    skipField();
                break;
            }
            case JceStruct.LIST: {
                int size = read(0, 0, true);
                for (int i = 0; i < size; ++i)
                    skipField();
                break;
            }
            case JceStruct.SIMPLE_LIST: {
                HeadData hd = new HeadData();
                readHead(hd);
                if (hd.type != JceStruct.BYTE) {
                    throw new JceDecodeException("skipField with invalid type, type value: " + type + ", " + hd.type);
                }
                int size = read(0, 0, true);
                skip(size);
                break;
            }
            case JceStruct.STRUCT_BEGIN:
                skipToStructEnd();
                break;
            case JceStruct.STRUCT_END:
            case JceStruct.ZERO_TAG:
                break;
            default:
                throw new JceDecodeException("invalid type.");
        }
    }

    public boolean read(boolean b, int tag, boolean isRequire) {
        byte c = read((byte) 0x0, tag, isRequire);
        return c != 0;
    }

    public byte read(byte c, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    c = 0x0;
                    break;
                case JceStruct.BYTE:
                    c = bs.readByte();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return c;
    }

    public short read(short n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    n = 0;
                    break;
                case JceStruct.BYTE:
                    n = (short) bs.readByte();
                    break;
                case JceStruct.SHORT:
                    n = bs.readShort();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return n;
    }

    public int read(int n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    n = 0;
                    break;
                case JceStruct.BYTE:
                    n = bs.readByte();
                    break;
                case JceStruct.SHORT:
                    n = bs.readShort();
                    break;
                case JceStruct.INT:
                    n = bs.readInt();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return n;
    }

    public long read(long n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    n = 0;
                    break;
                case JceStruct.BYTE:
                    n = bs.readByte();
                    break;
                case JceStruct.SHORT:
                    n = bs.readShort();
                    break;
                case JceStruct.INT:
                    n = bs.readInt();
                    break;
                case JceStruct.LONG:
                    n = bs.readLong();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return n;
    }

    public float read(float n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    n = 0;
                    break;
                case JceStruct.FLOAT:
                    n = bs.readFloat();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return n;
    }

    public double read(double n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.ZERO_TAG:
                    n = 0;
                    break;
                case JceStruct.FLOAT:
                    n = bs.readFloat();
                    break;
                case JceStruct.DOUBLE:
                    n = bs.readDouble();
                    break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return n;
    }

    public String readByteString(String s, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.STRING1: {
                    int len = bs.readByte();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.readBytes(ss);
                    s = HexBin.encode(ss);
                }
                break;
                case JceStruct.STRING4: {
                    int len = bs.readInt();
                    if (len > JceStruct.MAX_STRING_LENGTH || len < 0)
                        throw new JceDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    bs.readBytes(ss);
                    s = HexBin.encode(ss);
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return s;
    }

    public String read(String s, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.STRING1: {
                    int len = bs.readByte();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    bs.readBytes(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                case JceStruct.STRING4: {
                    int len = bs.readInt();
                    if (len > JceStruct.MAX_STRING_LENGTH || len < 0)
                        throw new JceDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.readBytes(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return s;
    }

    public String readString(int tag, boolean isRequire) {
        String s = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.STRING1: {
                    int len = bs.readByte();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.readBytes(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                case JceStruct.STRING4: {
                    int len = bs.readInt();
                    if (len > JceStruct.MAX_STRING_LENGTH || len < 0)
                        throw new JceDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.readBytes(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return s;
    }

    public String[] read(String[] s, int tag, boolean isRequire) {
        return readArray(s, tag, isRequire);
    }

    public Map<String, String> readStringMap(int tag, boolean isRequire) {
        HashMap<String, String> mr = new HashMap<String, String>();
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.MAP: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    for (int i = 0; i < size; ++i) {
                        String k = readString(0, true);
                        String v = readString(1, true);
                        mr.put(k, v);
                    }
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return mr;
    }

    public <K, V> HashMap<K, V> readMap(Map<K, V> m, int tag, boolean isRequire) {
        return (HashMap<K, V>) readMap(new HashMap<K, V>(), m, tag, isRequire);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <K, V> Map<K, V> readMap(Map<K, V> mr, Map<K, V> m, int tag, boolean isRequire) {
        if (m == null || m.isEmpty()) {
            return new HashMap();
        }

        Iterator<Map.Entry<K, V>> it = m.entrySet().iterator();
        Map.Entry<K, V> en = it.next();
        K mk = en.getKey();
        V mv = en.getValue();

        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.MAP: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    for (int i = 0; i < size; ++i) {
                        K k = (K) read(mk, 0, true);
                        V v = (V) read(mv, 1, true);
                        mr.put(k, v);
                    }
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return mr;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List readList(int tag, boolean isRequire) {
        List lr = new ArrayList();
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    for (int i = 0; i < size; ++i) {
                        HeadData subH = new HeadData();
                        readHead(subH);
                        switch (subH.type) {
                            case JceStruct.BYTE:
                                skip(1);
                                break;
                            case JceStruct.SHORT:
                                skip(2);
                                break;
                            case JceStruct.INT:
                                skip(4);
                                break;
                            case JceStruct.LONG:
                                skip(8);
                                break;
                            case JceStruct.FLOAT:
                                skip(4);
                                break;
                            case JceStruct.DOUBLE:
                                skip(8);
                                break;
                            case JceStruct.STRING1: {
                                int len = bs.readByte();
                                if (len < 0) len += 256;
                                skip(len);
                            }
                            break;
                            case JceStruct.STRING4: {
                                skip(bs.readInt());
                            }
                            break;
                            case JceStruct.MAP: {

                            }
                            break;
                            case JceStruct.LIST: {

                            }
                            break;
                            case JceStruct.STRUCT_BEGIN:
                                try {
                                    Class<?> newoneClass = Class.forName(JceStruct.class.getName());
                                    Constructor<?> cons = newoneClass.getConstructor();
                                    JceStruct struct = (JceStruct) cons.newInstance();
                                    struct.readFrom(this);
                                    skipToStructEnd();
                                    lr.add(struct);
                                } catch (Exception e) {
                                    throw new JceDecodeException("type mismatch. tag:"+tag + e);
                                }
                                break;
                            case JceStruct.ZERO_TAG:
                                lr.add(new Integer(0));
                                break;
                            default:
                                throw new JceDecodeException("type mismatch. tag:"+tag);
                        }
                    }
                }
                break;
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public boolean[] read(boolean[] l, int tag, boolean isRequire) {
        boolean[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new boolean[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public byte[] read(byte[] l, int tag, boolean isRequire) {
        byte[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.SIMPLE_LIST: {
                    HeadData hh = new HeadData();
                    readHead(hh);
                    if (hh.type != JceStruct.BYTE) {
                        throw new JceDecodeException("type mismatch, tag: " + tag + ", type: " + hd.type + ", " + hh.type);
                    }
                    int size = read(0, 0, true);
                    if (size < 0)
                        throw new JceDecodeException("invalid size, tag: " + tag + ", type: " + hd.type + ", " + hh.type + ", size: " + size);
                    lr = new byte[size];
                    //bs.get(lr);
                    bs.readBytes(lr);
                    break;
                }
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new byte[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public short[] read(short[] l, int tag, boolean isRequire) {
        short[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new short[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public int[] read(int[] l, int tag, boolean isRequire) {
        int[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new int[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public long[] read(long[] l, int tag, boolean isRequire) {
        long[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new long[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public float[] read(float[] l, int tag, boolean isRequire) {
        float[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new float[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public double[] read(double[] l, int tag, boolean isRequire) {
        double[] lr = null;
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    lr = new double[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return lr;
    }

    public <T> T[] readArray(T[] l, int tag, boolean isRequire) {
        if (l == null || l.length == 0) throw new JceDecodeException("unable to get type of key and value.");
        return readArrayImpl(l[0], tag, isRequire);
    }

    public <T> List<T> readArray(List<T> l, int tag, boolean isRequire) {
        if (l == null || l.isEmpty()) {
            return new ArrayList<T>();
        }
        T[] tt = readArrayImpl(l.get(0), tag, isRequire);
        if (tt == null) return null;
        ArrayList<T> ll = new ArrayList<T>(Arrays.asList(tt));
        return ll;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] readArrayImpl(T mt, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            HeadData hd = new HeadData();
            readHead(hd);
            switch (hd.type) {
                case JceStruct.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new JceDecodeException("size invalid: " + size);
                    T[] lr = (T[]) Array.newInstance(mt.getClass(), size);
                    for (int i = 0; i < size; ++i) {
                        T t = (T) read(mt, 0, true);
                        lr[i] = t;
                    }
                    return lr;
                }
                default:
                    throw new JceDecodeException("type mismatch. tag:"+tag);
            }
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return null;
    }

    public JceStruct directRead(JceStruct o, int tag, boolean isRequire) {
        JceStruct ref = null;
        if (skipToTag(tag)) {
            try {
                ref = o.newInit();
            } catch (Exception e) {
                throw new JceDecodeException(e.getMessage());
            }

            HeadData hd = new HeadData();
            readHead(hd);
            if (hd.type != JceStruct.STRUCT_BEGIN) throw new JceDecodeException("type mismatch. tag:"+tag);
            ref.readFrom(this);
            skipToStructEnd();
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return ref;
    }

    public <T extends JceStruct> T read(T o, int tag, boolean isRequire) {
        return (T) readTarsStruct(o, tag, isRequire);
    }

    public JceStruct readTarsStruct(JceStruct o, int tag, boolean isRequire) {
        JceStruct ref = null;
        if (skipToTag(tag)) {
            try {
                ref = o.getClass().newInstance();
            } catch (Exception e) {
                throw new JceDecodeException(e.getMessage());
            }

            HeadData hd = new HeadData();
            readHead(hd);
            if (hd.type != JceStruct.STRUCT_BEGIN) throw new JceDecodeException("type mismatch. tag:"+tag);
            ref.readFrom(this);
            skipToStructEnd();
        } else if (isRequire) {
            throw new JceDecodeException("require field not exist.");
        }
        return ref;
    }

    public JceStruct[] read(JceStruct[] o, int tag, boolean isRequire) {
        return readArray(o, tag, isRequire);
    }

    @SuppressWarnings("unchecked")
    public <T> Object read(T o, int tag, boolean isRequire) {
        if (o instanceof Byte) {
            return Byte.valueOf(read((byte) 0x0, tag, isRequire));
        } else if (o instanceof Boolean) {
            return Boolean.valueOf(read(false, tag, isRequire));
        } else if (o instanceof Short) {
            return Short.valueOf(read((short) 0, tag, isRequire));
        } else if (o instanceof Integer) {
            int i = read((int) 0, tag, isRequire);
            return Integer.valueOf(i);
        } else if (o instanceof Long) {
            return Long.valueOf(read((long) 0, tag, isRequire));
        } else if (o instanceof Float) {
            return Float.valueOf(read((float) 0, tag, isRequire));
        } else if (o instanceof Double) {
            return Double.valueOf(read((double) 0, tag, isRequire));
        } else if (o instanceof String) {
            return readString(tag, isRequire);
        } else if (o instanceof Map) {
            return readMap((Map) o, tag, isRequire);
        } else if (o instanceof List) {
            return readArray((List) o, tag, isRequire);
        } else if (o instanceof JceStruct) {
            return readTarsStruct((JceStruct) o, tag, isRequire);
        } else if (o.getClass().isArray()) {
            if (o instanceof byte[] || o instanceof Byte[]) {
                return read((byte[]) null, tag, isRequire);
            } else if (o instanceof boolean[]) {
                return read((boolean[]) null, tag, isRequire);
            } else if (o instanceof short[]) {
                return read((short[]) null, tag, isRequire);
            } else if (o instanceof int[]) {
                return read((int[]) null, tag, isRequire);
            } else if (o instanceof long[]) {
                return read((long[]) null, tag, isRequire);
            } else if (o instanceof float[]) {
                return read((float[]) null, tag, isRequire);
            } else if (o instanceof double[]) {
                return read((double[]) null, tag, isRequire);
            } else {
                return readArray((Object[]) o, tag, isRequire);
            }
        } else {
            throw new JceDecodeException("Unknown field type");
        }
    }

    protected Charset sServerEncoding = StandardCharsets.UTF_8;

    public int setServerEncoding(Charset se) {
        sServerEncoding = se;
        return 0;
    }

    public int setServerEncoding(String se) {
        sServerEncoding = Charset.forName(se);
        return 0;
    }

    public ByteBuf getBs() {
        return this.bs;
    }

    public byte[] toByteArray() {
        final byte[] byteArrays = new byte[bs.readableBytes()];
        bs.readBytes(byteArrays);
        return byteArrays;
    }
}

