package com.qq.taf.jce.plugin.tars

import com.qq.taf.jce.plugin.ext.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.*

const val NAME_TARS_CLASS = "Lcom/qq/taf/jce/Tars;"
const val NAME_TARS_CLASS1 = "Lcom/qq/taf/jce/Jce;"
const val NAME_TARS_FIELD = "Lcom/qq/taf/jce/TarsId;"
const val NAME_TARS_FIELD1 = "Lcom/qq/taf/jce/JceId;"

const val NAME_TARS_OUT = "com/qq/taf/jce/JceOutputStream"
const val NAME_TARS_IN = "com/qq/taf/jce/JceInputStream"

/**
 * Tars、Jce编译时处理器 自动实现readFrom 和 writeTo 方法 包括集合属性的 cache 字段实现
 * @author 洛洛 & 企鹅
 * */
class TarsPrecompile(
    private val fuller: ClassFuller
) {
    private val fields: TreeMap<Int, FieldInfo> by lazy { TreeMap() }
    private val tarsClassInfo = TarsClass()

    // ================ INFO
    private lateinit var className: String

    private var isInit: Boolean = false


    fun invoke(): Result<ByteArray> {
        init()

        if (!isInit) return Result.failure(RuntimeException("init failed"))

        if (tarsClassInfo.requireWrite) transformWrite()
        if (tarsClassInfo.requireRead) transformRead()

        if (tarsClassInfo.servantName.isNotEmpty() && !fuller.hasMethod("servantName")) fuller.methods.add(
            MethodNode(
                Opcodes.ACC_PUBLIC,
                "servantName",
                "()Ljava/lang/String;",
                null,
                null
            ).also { mv ->
                mv.visitCode()
                mv.visitLdcInsn(tarsClassInfo.servantName)
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
            })
        if (tarsClassInfo.funcName.isNotEmpty() && !fuller.hasMethod("funcName")) fuller.methods.add(
            MethodNode(
                Opcodes.ACC_PUBLIC,
                "funcName",
                "()Ljava/lang/String;",
                null,
                null
            ).also { mv ->
                mv.visitCode()
                mv.visitLdcInsn(tarsClassInfo.funcName)
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
            })
        if (tarsClassInfo.reqName.isNotEmpty() && !fuller.hasMethod("reqName")) fuller.methods.add(
            MethodNode(
                Opcodes.ACC_PUBLIC,
                "reqName",
                "()Ljava/lang/String;",
                null,
                null
            ).also { mv ->
                mv.visitCode()
                mv.visitLdcInsn(tarsClassInfo.reqName)
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
            })
        if (tarsClassInfo.respName.isNotEmpty() && !fuller.hasMethod("respName")) fuller.methods.add(
            MethodNode(
                Opcodes.ACC_PUBLIC,
                "respName",
                "()Ljava/lang/String;",
                null,
                null
            ).also { mv ->
                mv.visitCode()
                mv.visitLdcInsn(tarsClassInfo.respName)
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
            })
        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
        fuller.sourceFile = "TarsAuto"
        fuller.accept(cw)
        return Result.success(cw.toByteArray())
    }

    private fun transformRead() {
        if (fuller.hasMethod("readFrom")) return


        fuller.method(7, 7) {
            access = Opcodes.ACC_PUBLIC
            name = "readFrom"
            args = arrayOf("L$NAME_TARS_IN;")

            code {
                if (fuller.superName != "java/lang/Object") {
                    loadObject(0)
                    loadObject(1)
                    invokeSpecial(fuller.superName, name!!, "(${args[0]})V")
                }
                fields.forEach { (tag, field) ->
                    if (field.isEnum) {
                        val nextLabel = Label()
                        loadObject(1) // p1 (input)
                        pushInt(tag)
                        pushBoolean(field.require)
                        invokeVirtual(NAME_TARS_IN, "readString", "(IZ)Ljava/lang/String;")
                        // p0 p1 被占用 --> 使用p2
                        storeObject(2) // save result

                        loadObject(2)
                        ifNull(nextLabel)

                        loadObject(0) // for put field

                        loadObject(2) // string value
                        invokeStatic(
                            field.type.let { it.substring(1, it.length - 1) },
                            "valueOf",
                            "(Ljava/lang/String;)${field.type}"
                        )

                        putField(className, field.name, field.type)

                        label(nextLabel)
                        // frameAppend(1, arrayOf("java/lang/String"))
                        frameSame()
                    } else {
                        val fieldType = field.type
                        if (field.isTarsObject()) {
                            val codeLabel = Label()
                            val cacheName = "cache_${field.name}"
                            fuller.field(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, cacheName, fieldType, field.sign)
                            getField(className, cacheName, fieldType, true)
                            ifNoNull(codeLabel)
                            when {
                                fieldType == "Ljava/util/HashMap;" || fieldType == "Ljava/util/Map;" -> {
                                    // Ljava/util/HashMap<Ljava/lang/Byte;LTarsTest$Objsua;>;
                                    val allType =
                                        field.sign!!.substring(fieldType.length).let { it.substring(0, it.length - 2) }
                                            .split(";")
                                    // 0 key 1 value 2 empty
                                    val keyType = allType[0].substring(1)
                                    var newClassName = allType[1]
                                    newInstance("java/util/HashMap", "()V")
                                    storeObject(2)

                                    loadObject(2)
                                    forPut(keyType) // key

                                    if (newClassName.startsWith("[")) {
                                        newArray(newClassName.substring(1), 1)
                                    } else {
                                        newClassName = newClassName.substring(1)
                                        forPut(newClassName) // value
                                    }
                                    invokeVirtual(
                                        "java/util/HashMap",
                                        "put",
                                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                                    )
                                    pop() // 弹出put操作的返回值

                                    loadObject(2)
                                    putField(className, cacheName, fieldType, true)
                                }
                                fieldType == "Ljava/util/ArrayList;" || fieldType == "Ljava/util/List;" -> {
                                    // Ljava/util/ArrayList<Lxxx;>;
                                    val valueType =
                                        field.sign!!.substring(fieldType.length).let { it.substring(0, it.length - 2) }
                                    // Lxxx;
                                    val newClassName = valueType.substring(1).let { it.substring(0, it.length - 1) }
                                    // list sign 内一定是对象 则不可能是 baseValue

                                    newInstance("java/util/ArrayList", "()V")
                                    storeObject(2) // save to p2

                                    loadObject(2)
                                    forPut(newClassName) // value
                                    invokeInterface("java/util/List", "add", "(Ljava/lang/Object;)Z")
                                    pop() // pop add result

                                    loadObject(2)
                                    putField(className, cacheName, fieldType, true)
                                }
                                fieldType.startsWith("[") -> {
                                    // if is array then type = “[Lxxx; or [B”
                                    var newClassName = fieldType.substring(1)
                                    if (newClassName.startsWith("L")) {
                                        newClassName = newClassName.substring(1)
                                            .let { it.substring(0, it.length - 1) } // remove "L" and ";"
                                        newArray(newClassName, 1)
                                        storeObject(2) // save to p2

                                        loadObject(2)
                                        iConstN0() // array index
                                        forPut(newClassName)
                                        storeObjectTA()

                                        loadObject(2)
                                    } else newArray(newClassName, 1)
                                    putField(className, cacheName, fieldType, true)
                                }
                                else -> {
                                    newInstance(
                                        fieldType.substring(1).let { it.substring(0, it.length - 1) },
                                        "()V"
                                    ) // remove "L" and ";"
                                    putField(className, cacheName, fieldType, true)
                                }
                            }
                            label(codeLabel)
                            frameSame()

                            // frameAppend(1, arrayOf("java/lang/String")) not to do, will wrong

                            loadObject(0) // p0 ,for put field
                            loadObject(1) // p1 ,for read
                            getField(className, cacheName, fieldType, true)
                        } else {
                            loadObject(0) // p0 ,for put field
                            loadObject(1) // p1 ,for read
                            loadObject(0) // for get field
                            getField(className, field.name, fieldType)
                        }

                        pushInt(tag)
                        pushBoolean(field.require)
                        invokeVirtual(
                            NAME_TARS_IN,
                            "read",
                            if (field.isBaseType()) "(${fieldType}IZ)${fieldType}" else "(Ljava/lang/Object;IZ)Ljava/lang/Object;"
                        )
                        if (!field.isBaseType()) {
                            if (fieldType.startsWith("[")) {
                                // 如果是 array就不能去掉L
                                cast(fieldType)
                            } else {
                                cast(fieldType.substring(1).let { it.substring(0, it.length - 1) })
                            }
                        }
                        putField(className, field.name, fieldType)
                    }
                }
                returnVoid()
            }

        }
    }

    private fun transformWrite() {
        if (fuller.hasMethod("writeTo")) return

//        println("transform write-method")

        fuller.method(3, 2) {
            access = Opcodes.ACC_PUBLIC
            name = "writeTo"
            args = arrayOf("L$NAME_TARS_OUT;")
            // ret = "V" default

            code {
                if (fuller.superName != "java/lang/Object") {
                    loadObject(0)
                    loadObject(1)
                    invokeSpecial(fuller.superName, name!!, "(${args[0]})V")
                }
                var startLabel = Label()
                var nextLabel = Label()
                fields.forEach { (tag, field) ->
                    label(startLabel)
                    frameSame()

                    field.bindMethod.forEach { method ->
                        if (isStatic(method.access)) {
                            invokeStatic(className, method.name, method.desc)
                        } else {
                            invokeVirtual(className, method.name, method.desc)
                        }
                    }

                    if (field.needCheckNull()) {
                        loadObject(0) // this
                        getField(className, field.name, field.type)
                        ifNull(nextLabel)
                    }

                    loadObject(1) // p1 (output)
                    loadObject(0) // this

                    getField(className, field.name, field.type)
                    if (field.isEnum) {
                        invokeVirtual(field.type.let { it.substring(1, it.length - 1) }, "name", "()Ljava/lang/String;")
                    }
                    pushInt(tag)
                    if (field.isBaseType()) {
                        invokeVirtual(NAME_TARS_OUT, "write", "(${field.type}I)V")
                    } else {
                        invokeVirtual(NAME_TARS_OUT, "write", "(Ljava/lang/Object;I)V")
                    }
                    // ==== exchange label
                    startLabel = nextLabel
                    nextLabel = Label()
                }
                label(startLabel)
                frameSame()

                returnVoid()
            }
        }
    }

    private fun init() {
        this.className = fuller.name

        (fuller.getAnnotation(NAME_TARS_CLASS) ?: fuller.getAnnotation(NAME_TARS_CLASS1))?.let {
            this.isInit = true
            fuller.removeAnnotation(it)

            val iterator = it.values.also { vs ->
                if (vs == null) return@let
            }.iterator()
            tarsClassInfo.runCatching {
                while (iterator.hasNext()) {
                    val name = iterator.next() as String
                    val value = iterator.next()
                    when (name) {
                        "write" -> requireWrite = value as Boolean
                        "read" -> requireRead = value as Boolean
                        "servantName" -> servantName = value as String
                        "funcName" -> funcName = value as String
                        "reqName" -> reqName = value as String
                        "respName" -> respName = value as String
                        else -> error("unknown tars class.")
                    }
                }
            }
        }

        if (!tarsClassInfo.requireRead && !tarsClassInfo.requireWrite && !isInit) return

        fuller.fields.forEach {
            val annotation = it.getAnnotation(NAME_TARS_FIELD) ?: it.getAnnotation(NAME_TARS_FIELD1)
            if (annotation != null) {
                val iterator = annotation.values?.iterator()!!
                val info = FieldInfo(it.name, it.desc, sign = it.signature)


                while (iterator.hasNext()) {
                    val name = iterator.next() as String
                    val value = iterator.next()
                    when (name) {
                        "id" -> info.tag = value as Int
                        "require" -> info.require = value as Boolean
                        else -> error("unknown tars field")
                    }
                }

                fields[info.tag] = info

                it.removeAnnotation(annotation)
            }
        }


    }

    private fun CodeBuilder.forPut(type: String) {
        when (type) {
            "java/lang/Byte" -> {
                iConstN0()
                invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;")
            }
            "java/lang/Short" -> {
                iConstN0()
                invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;")
            }
            "java/lang/Integer" -> {
                iConstN0()
                invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
            }
            "java/lang/Long" -> {
                lConstN0()
                invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;")
            }
            "java/lang/Float" -> {
                fConstN0()
                invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
            }
            "java/lang/Double" -> {
                dConstN0()
                invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;")
            }
            "java/lang/Character" -> {
                iConstN0()
                invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;")
            }
            "java/lang/String" -> ldc("")
            // "L", "I",
            else -> {
                newInstance(type, "()V")
            }
        }
    }
}

data class FieldInfo(
    var name: String,
    var type: String,
    var tag: Int = 0,
    var sign: String? = null,
    var require: Boolean = false,
    var isEnum: Boolean = false
) {
    val bindMethod: ArrayList<MethodInfo> = arrayListOf()

    fun bind(method: MethodInfo) {
        bindMethod.add(method)
    }
}

data class MethodInfo(
    val access: Int,
    val name: String,
    val desc: String
)

data class TarsClass(
    var requireRead: Boolean = true,
    var requireWrite: Boolean = true,
    var servantName: String = "",
    var funcName: String = "",
    var reqName: String = "",
    var respName: String = ""
)

private val BaseTypeArray = arrayOf(
    "Ljava/lang/String;",
    "B", "S", "I", "J", "F", "D", "C", "Z"
)

private val NotNeedCheck = arrayOf(
    "B", "S", "I", "J", "F", "D", "C", "Z"
)

private val systemClass = arrayOf(
    "Ljava/lang/Byte;",
    "Ljava/lang/Short;",
    "Ljava/lang/Integer;",
    "Ljava/lang/Character;",
    "Ljava/lang/Long;",
    "Ljava/lang/Float;",
    "Ljava/lang/Double;",
    "Ljava/lang/Boolean;",
)

fun FieldInfo.isTarsObject(): Boolean {
    if (!isBaseType()) {
        return !systemClass.contains(this.type)
    }
    return false
}

fun FieldInfo.isBaseType() = isBaseType(this.type)

fun FieldInfo.needCheckNull() = needCheckNull(this.type)

fun isBaseType(string: String) = BaseTypeArray.contains(string)

fun needCheckNull(string: String) = !NotNeedCheck.contains(string)