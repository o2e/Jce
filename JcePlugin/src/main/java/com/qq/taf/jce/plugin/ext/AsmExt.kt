@file:Suppress("unused", "NOTHING_TO_INLINE")
package com.qq.taf.jce.plugin.ext

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.InputStream

/**
 * ch: 贯彻kotlin写法的asm操作
 * en: Implement the asm operation of kotlin writing
 */

class ClassFuller(api: Int = ASM9) : ClassNode(api) {
    inline fun field(access : Int, name : String, args: String, sign : String? = null, value : String? = null) = fields.add(FieldNode(access, name, args, sign, value))

    inline fun method(maxStack : Int, maxLocals : Int, api : Int = ASM9 , block : MethodBuilder.() -> Unit) {
        this.methods.add(MethodNode(api).also {
            val builder = MethodBuilder(it)
            builder.block()

            it.visitMaxs(maxStack, maxLocals)
            it.visitEnd()

            with(it) {
                this.access = builder.access
                this.name = builder.name
                this.desc = let {
                    val buffer = StringBuffer()
                    buffer.append("(")
                    builder.args.forEach { v -> buffer.append(v) }
                    buffer.append(")")
                    buffer.append(builder.ret)
                    buffer.toString()
                }
                this.signature = builder.signature
                this.exceptions = builder.exceptions
            }
        })
    }

    inline fun from(classBytes : ByteArray, type: Int = ClassReader.EXPAND_FRAMES) {
        ClassReader(classBytes).accept(this, type)
    }

    inline fun from(input : InputStream, type: Int = ClassReader.EXPAND_FRAMES) {
        ClassReader(input).accept(this, type)
    }
}

class MethodBuilder(val mn : MethodNode) {
    var access : Int = 0
    var name : String? = null
    var signature : String? = null
    var exceptions : List<String>? = null

    var args : Array<String> = arrayOf()
    var ret : String = "V"

    inline fun code(block: CodeBuilder.() -> Unit) {
        mn.visitCode()
        CodeBuilder(mn).block()
        // mn.visitEnd()
    }
}

/*
 * ldc 使用注意 常量池大小只有 255
 *
 * !!!asm 没有提供ldc宽操作!!!
 *
 * ldc -- byte
 * index -- byte
 * so index : 0 ~ 255
 *
 * ldc_w / ldc2_w -- byte
 * short index -- 2 bytes
 * so index : 32767
 */


class CodeBuilder(val mn: MethodNode) {
    inline fun nop() {
        mn.visitInsn(NOP)
    }

    inline fun pushDouble(d : Double) {
        when(d) {
            .0 -> mn.visitInsn(DCONST_0)
            1.0 -> mn.visitInsn(DCONST_1)
            else -> mn.visitLdcInsn(d)
        }
    }

    inline fun pushBoolean(z : Boolean) {
        if(z) {
            mn.visitInsn(ICONST_1)
        } else {
            mn.visitInsn(ICONST_0)
        }
    }

    inline fun pushFloat(f : Float) {
        when(f) {
            0f -> mn.visitInsn(FCONST_0)
            1f -> mn.visitInsn(FCONST_1)
            2f -> mn.visitInsn(FCONST_2)
            else -> mn.visitLdcInsn(f)
        }
    }

    inline fun pushByte(b : Byte) = pushInt(b.toInt())

    inline fun pushChar(c : Char) = pushInt(c.toInt())

    inline fun pushShort(b : Byte) = pushInt(b.toInt())

    inline fun iConstM1() = mn.visitInsn(ICONST_M1)

    inline fun iConstN0() = mn.visitInsn(ICONST_0)

    inline fun iConstN1() = mn.visitInsn(ICONST_1)

    inline fun iConstN2() = mn.visitInsn(ICONST_2)

    inline fun iConstN3() = mn.visitInsn(ICONST_3)

    inline fun iConstN4() = mn.visitInsn(ICONST_4)

    inline fun iConstN5() = mn.visitInsn(ICONST_5)

    inline fun lConstN0() = mn.visitInsn(LCONST_0)

    inline fun lConstN1() = mn.visitInsn(LCONST_1)

    inline fun fConstN0() = mn.visitInsn(FCONST_0)

    inline fun fConstN1() = mn.visitInsn(FCONST_1)

    inline fun fConstN2() = mn.visitInsn(FCONST_2)

    inline fun dConstN0() = mn.visitInsn(DCONST_0)

    inline fun dConstN1() = mn.visitInsn(DCONST_1)

    inline fun pushInt(i : Int) {
        when(i) {
            -1 -> iConstM1()
            0 -> iConstN0()
            1 -> iConstN1()
            2 -> iConstN2()
            3 -> iConstN3()
            4 -> iConstN4()
            5 -> iConstN5()
            in -128 .. 127 -> {
                mn.visitIntInsn(BIPUSH, i)
            }
            in -32768 .. 32767 -> {
                mn.visitIntInsn(SIPUSH, i)
            }
            else -> {
                mn.visitLdcInsn(i)
            }
        }
    }

    inline fun ldc(l : Long) {
        mn.visitLdcInsn(l)
    }

    inline fun ldc(str : String) {
        mn.visitLdcInsn(str)
    }

    inline fun label(label: Label): Label {
        mn.visitLabel(label)
        return label
    }

    inline fun frameSame(localCnt : Int = 0, local : Array<String>? = null, stackCnt : Int = 0, stack : Array<String>? = null) {
        mn.visitFrame(F_SAME, localCnt, local, stackCnt, stack)
    }

    inline fun frameAppend(localCnt : Int = 0, local : Array<String>? = null, stackCnt : Int = 0, stack : Array<String>? = null) {
        mn.visitFrame(F_APPEND, localCnt, local, stackCnt, stack)
    }

    inline fun loadInt(varId: Int) {
        mn.visitVarInsn(ILOAD, varId)
    }

    inline fun loadLong(varId: Int) {
        mn.visitVarInsn(LLOAD, varId)
    }

    inline fun loadFloat(varId: Int) {
        mn.visitVarInsn(FLOAD, varId)
    }

    inline fun loadDouble(varId: Int) {
        mn.visitVarInsn(DLOAD, varId)
    }

    inline fun loadObject(varId: Int) {
        mn.visitVarInsn(ALOAD, varId)
    }

    //===== array 从array里面拿出某个值
    // load int from array
    inline fun loadIntFA(index: Int) {
        pushInt(index)
        mn.visitInsn(IALOAD)
    }

    inline fun loadLongFA(index: Int) {
        pushInt(index)
        mn.visitInsn(LALOAD)
    }

    inline fun loadFloatFA(index: Int) {
        pushInt(index)
        mn.visitInsn(FALOAD)
    }

    inline fun loadDoubleFA(index: Int) {
        pushInt(index)
        mn.visitInsn(DALOAD)
    }

    inline fun loadByteFA(index: Int) {
        pushInt(index)
        mn.visitInsn(BALOAD)
    }

    inline fun loadCharFA(index: Int) {
        pushInt(index)
        mn.visitInsn(CALOAD)
    }

    inline fun loadShortFA(index: Int) {
        pushInt(index)
        mn.visitInsn(SALOAD)
    }

    inline fun loadObjectFA(index: Int) {
        pushInt(index)
        mn.visitInsn(AALOAD)
    }

    inline fun storeBoolean(varId: Int) {
        mn.visitVarInsn(ISTORE, varId)
    }

    inline fun storeByte(varId: Int) {
        mn.visitVarInsn(ISTORE, varId)
    }

    inline fun storeShort(varId: Int) {
        mn.visitVarInsn(ISTORE, varId)
    }

    inline fun storeChar(varId: Int) {
        mn.visitVarInsn(ISTORE, varId)
    }

    inline fun storeInt(varId: Int) {
        mn.visitVarInsn(ISTORE, varId)
    }

    inline fun storeLong(varId: Int) {
        mn.visitVarInsn(LSTORE, varId)
    }

    inline fun storeFloat(varId: Int) {
        mn.visitVarInsn(FSTORE, varId)
    }

    inline fun storeDouble(varId: Int) {
        mn.visitVarInsn(DSTORE, varId)
    }

    inline fun storeObject(varId: Int) {
        mn.visitVarInsn(ASTORE, varId)
    }

    // store int to array
    inline fun storeIntTA() {
        mn.visitInsn(IASTORE)
    }

    inline fun storeByteTA() {
        mn.visitInsn(BASTORE)
    }

    inline fun storeCharTA() {
        mn.visitInsn(CASTORE)
    }

    inline fun storeShortTA() {
        mn.visitInsn(SASTORE)
    }

    inline fun storeLongTA() {
        mn.visitInsn(LASTORE)
    }

    inline fun storeFloatTA() {
        mn.visitInsn(FASTORE)
    }

    inline fun storeDoubleTA() {
        mn.visitInsn(DASTORE)
    }

    inline fun storeObjectTA() {
        mn.visitInsn(AASTORE)
    }

    inline fun pop() {
        mn.visitInsn(POP)
    }

    inline fun popWide() {
        mn.visitInsn(POP2)
    }

    inline fun dup() {
        mn.visitInsn(DUP)
    }

    inline fun dupX2() {
        mn.visitInsn(DUP_X1)
    }

    inline fun dupX3() {
        mn.visitInsn(DUP_X2)
    }

    inline fun dupWide() {
        mn.visitInsn(DUP2)
    }

    inline fun dupWideX2() {
        mn.visitInsn(DUP2_X1)
    }

    inline fun dupWideX3() {
        mn.visitInsn(DUP2_X2)
    }

    inline fun swap() {
        mn.visitInsn(SWAP)
    }

    /**
     * varId 被执行加减法的伐id
     * value 值
     */
    inline fun intAdd(varId: Int, value : Int) {
        mn.visitIincInsn(varId, value)
    }

    inline fun intAdd() {
        mn.visitInsn(IADD)
    }

    inline fun longAdd() {
        mn.visitInsn(LADD)
    }

    inline fun floatAdd() {
        mn.visitInsn(FADD)
    }

    inline fun doubleAdd() {
        mn.visitInsn(DADD)
    }

    inline fun intSub() {
        mn.visitInsn(ISUB)
    }

    inline fun longSub() {
        mn.visitInsn(LSUB)
    }

    inline fun floatSub() {
        mn.visitInsn(FSUB)
    }

    inline fun doubleSub() {
        mn.visitInsn(DSUB)
    }

    inline fun intMul() {
        mn.visitInsn(IMUL)
    }

    inline fun longMul() {
        mn.visitInsn(LMUL)
    }

    inline fun floatMul() {
        mn.visitInsn(FMUL)
    }

    inline fun doubleMul() {
        mn.visitInsn(DMUL)
    }

    inline fun intDiv() {
        mn.visitInsn(IDIV)
    }

    inline fun longDiv() {
        mn.visitInsn(LDIV)
    }

    inline fun floatDiv() {
        mn.visitInsn(FDIV)
    }

    inline fun doubleDiv() {
        mn.visitInsn(DDIV)
    }

    inline fun intRem() {
        mn.visitInsn(IREM)
    }

    inline fun longRem() {
        mn.visitInsn(LREM)
    }

    inline fun floatRem() {
        mn.visitInsn(FREM)
    }

    inline fun doubleRem() {
        mn.visitInsn(DREM)
    }

    inline fun intNeg() {
        mn.visitInsn(INEG)
    }

    inline fun longNeg() {
        mn.visitInsn(LNEG)
    }

    inline fun floatNeg() {
        mn.visitInsn(FNEG)
    }

    inline fun doubleNeg() {
        mn.visitInsn(DNEG)
    }

    inline fun intShl() {
        mn.visitInsn(ISHL)
    }

    inline fun longShl() {
        mn.visitInsn(LSHL)
    }

    inline fun intShr() {
        mn.visitInsn(ISHR)
    }

    inline fun longShr() {
        mn.visitInsn(LSHR)
    }

    inline fun intUShr() {
        mn.visitInsn(IUSHR)
    }

    inline fun longUShr() {
        mn.visitInsn(LUSHR)
    }

    inline fun intAnd() {
        mn.visitInsn(IAND)
    }

    fun longAnd() {
        mn.visitInsn(LAND)
    }

    inline fun intOr() {
        mn.visitInsn(IOR)
    }

    inline fun longOr() {
        mn.visitInsn(LOR)
    }

    inline fun intXor() {
        mn.visitInsn(IXOR)
    }

    inline fun longXor() {
        mn.visitInsn(LXOR)
    }

    inline fun intToByte() {
        mn.visitInsn(I2B)
    }

    inline fun intToChar() {
        mn.visitInsn(I2C)
    }

    inline fun intToShort() {
        mn.visitInsn(I2S)
    }

    inline fun intToFloat() {
        mn.visitInsn(I2F)
    }

    inline fun intToDouble() {
        mn.visitInsn(I2D)
    }

    inline fun intToLong() {
        mn.visitInsn(I2L)
    }

    inline fun longToInt() {
        mn.visitInsn(L2I)
    }

    inline fun longToFloat() {
        mn.visitInsn(L2F)
    }

    inline fun longToDouble() {
        mn.visitInsn(L2D)
    }

    inline fun longToByte() {
        longToInt()
        intToByte()
    }

    inline fun longToChar() {
        longToInt()
        intToChar()
    }

    inline fun longToShort() {
        longToInt()
        intToShort()
    }

    inline fun floatToInt() {
        mn.visitInsn(F2I)
    }

    inline fun floatToLong() {
        mn.visitInsn(F2L)
    }

    inline fun floatToDouble() {
        mn.visitInsn(F2D)
    }

    inline fun floatToByte() {
        floatToInt()
        intToByte()
    }

    inline fun floatToChar() {
        floatToInt()
        intToChar()
    }

    inline fun floatToShort() {
        floatToInt()
        intToShort()
    }

    inline fun doubleToInt() {
        mn.visitInsn(D2I)
    }

    inline fun doubleToLong() {
        mn.visitInsn(D2L)
    }

    inline fun doubleToFloat() {
        mn.visitInsn(D2F)
    }

    inline fun doubleToByte() {
        doubleToInt()
        intToByte()
    }

    inline fun doubleToChar() {
        doubleToInt()
        intToChar()
    }

    inline fun doubleToShort() {
        doubleToInt()
        intToShort()
    }

    // le >
    // ge <
    // eq =
    // ne !=

    inline fun goto(label: Label) {
        mn.visitJumpInsn(GOTO, label)
    }

    inline fun returnInt() {
        mn.visitInsn(IRETURN)
    }

    inline fun returnLong() {
        mn.visitInsn(LRETURN)
    }

    inline fun returnFloat() {
        mn.visitInsn(FRETURN)
    }

    inline fun returnDouble() {
        mn.visitInsn(DRETURN)
    }

    inline fun returnObject() {
        mn.visitInsn(ARETURN)
    }

    inline fun returnVoid() {
        mn.visitInsn(RETURN)
    }

    /**
     * type : 类型名
     */
    inline fun getField(fromClass : String, name : String, type : String, isStatic : Boolean = false) {
        mn.visitFieldInsn(if(isStatic) GETSTATIC else GETFIELD, fromClass, name, type)
    }

    inline fun putField(fromClass : String, name : String, type : String, isStatic: Boolean = false) {
        mn.visitFieldInsn(if (isStatic) PUTSTATIC else PUTFIELD, fromClass, name, type)
    }

    /**
     * type : 类型类名
     */
    inline fun newInstance(type: String, args : String, block : CodeBuilder.() -> Unit = {
        // not have args
    }) {
        mn.visitTypeInsn(NEW, type)
        mn.visitInsn(DUP)
        block.invoke(this)
        mn.visitMethodInsn(INVOKESPECIAL, type, "<init>", args, false)
    }

    /**
     * 类型类名
     */
    inline fun cast(type: String) {
        mn.visitTypeInsn(CHECKCAST, type)
    }

    /**
     * 类型类名
     */
    inline fun instanceof(type: String) {
        mn.visitTypeInsn(INSTANCEOF, type)
    }

    /**
     * 类型类名
     */
    inline fun newArray(type: String, length: Int) {
        pushInt(length)
        when(type) {
            "B" -> mn.visitIntInsn(NEWARRAY, T_BYTE)
            "Z" -> mn.visitIntInsn(NEWARRAY, T_BOOLEAN)
            "C" -> mn.visitIntInsn(NEWARRAY, T_CHAR)
            "I" -> mn.visitIntInsn(NEWARRAY, T_INT)
            "S" -> mn.visitIntInsn(NEWARRAY, T_SHORT)
            "J" -> mn.visitIntInsn(NEWARRAY, T_LONG)
            "D" -> mn.visitIntInsn(NEWARRAY, T_DOUBLE)
            "F" -> mn.visitIntInsn(NEWARRAY, T_FLOAT)
            else -> mn.visitTypeInsn(NEWARRAY, type)
        }
    }

    inline fun arraySize() {
        mn.visitInsn(ARRAYLENGTH)
    }

    inline fun ifEqual(label: Label) {
        mn.visitJumpInsn(IFEQ, label)
    }

    /**
     * 如果等于0(FALSE)则跳转
     */
    inline fun ifNotEqual(label: Label) {
        mn.visitJumpInsn(IFNE, label)
    }

    inline fun ifLE(label: Label) {
        mn.visitJumpInsn(IFLE, label)
    }

    inline fun ifGE(label: Label) {
        mn.visitJumpInsn(IFGE, label)
    }

    inline fun ifLt(label: Label) {
        mn.visitJumpInsn(IFLT, label)
    }

    inline fun ifGt(label: Label) {
        mn.visitJumpInsn(IFGT, label)
    }

    inline fun ifNull(label: Label) {
        mn.visitJumpInsn(IFNULL, label)
    }

    inline fun ifNoNull(label: Label) {
        mn.visitJumpInsn(IFNONNULL, label)
    }

    inline fun throwError() {
        mn.visitInsn(ATHROW)
    }

    inline fun invokeVirtual(fromClass: String, name: String, args: String, isInterface : Boolean = false) {
        mn.visitMethodInsn(INVOKEVIRTUAL, fromClass, name, args, isInterface)
    }

    inline fun invokeStatic(fromClass: String, name: String, args: String) {
        mn.visitMethodInsn(INVOKESTATIC, fromClass, name, args, false)
    }

    inline fun invokeInterface(fromClass: String, name: String, args: String) {
        mn.visitMethodInsn(INVOKEINTERFACE, fromClass, name, args, true)
    }

    inline fun invokeSpecial(fromClass: String, name: String, args: String) {
        mn.visitMethodInsn(INVOKESPECIAL, fromClass, name, args, false)
    }

    inline fun line(line : Int, label: Label) {
        mn.visitLineNumber(line, label)
    }
}