package com.qq.taf.jce.plugin.ext

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * 定义：
 *
 * (1) 完整名 Lcom/tencent/mobileqq/MainActivity;
 * (2) 类型名 com/tencent/mobileqq/MainActivity
 * (3) 名称 MainActivity
 */

/**
 * 这个工具类几乎要报废了
 */
/**
 * 输入类型：完整名
 */
@JvmOverloads
fun ClassNode.hasAnnotation(
    annotation: String,
    visible: Boolean = true
): Boolean = getAnnotation(annotation, visible) != null

/**
 * 输入类型：完整名
 */
@JvmOverloads
fun FieldNode.hasAnnotation(
    annotation: String,
    visible: Boolean = true
): Boolean = getAnnotation(annotation, visible) != null

/**
 * 输入类型：完整名
 */
fun MethodNode.hasAnnotation(annotation: String, visible: Boolean = true): Boolean = getAnnotation(annotation, visible) != null

/**
 * 输入类型：名称
 */
fun ClassNode.hasMethod(name : String): Boolean {
    this.fields.forEach {
        if(it.name == name) return true
    }
    return false
}

/**
 * 输入类型：完整名
 */
@JvmOverloads
fun ClassNode.getAnnotation(annotation: String, visible: Boolean = true): AnnotationNode? {
    if(visible)
        this.visibleAnnotations?.forEach { if (it.desc == annotation) return it }
    else
        this.invisibleAnnotations?.forEach { if (it.desc == annotation) return it }
    return null
}

@JvmOverloads
fun ClassNode.removeAnnotation(annotation: AnnotationNode?, visible: Boolean = true) {
    if(visible)
        this.visibleAnnotations?.remove(annotation)
    else
        this.invisibleAnnotations?.remove(annotation)
}

/**
 * 输入类型：完整名
 */
@JvmOverloads
fun FieldNode.getAnnotation(annotation: String, visible: Boolean = true): AnnotationNode? {
    if(visible)
        this.visibleAnnotations?.forEach { if (it.desc == annotation) return it }
    else
        this.invisibleAnnotations?.forEach { if (it.desc == annotation) return it }
    return null
}

@JvmOverloads
fun FieldNode.removeAnnotation(annotation: AnnotationNode?, visible: Boolean = true) {
    if(visible)
        this.visibleAnnotations?.remove(annotation)
    else
        this.invisibleAnnotations?.remove(annotation)
}

/**
 * 输入类型：完整名
 */
@JvmOverloads
fun MethodNode.getAnnotation(annotation: String, visible: Boolean = true): AnnotationNode? {
    if(visible)
        this.visibleAnnotations?.forEach { if (it.desc == annotation) return it }
    else
        this.invisibleAnnotations?.forEach { if (it.desc == annotation) return it }
    return null
}

/**
 * 输入类型：名称
 */
@JvmOverloads
inline fun ClassNode.addMethod(access : Int, name: String, desc : String, sign : String? = null, e : Array<String>? = null, block : MethodNode.() -> Unit) {
    this.methods.add(MethodNode(access, name, desc, sign, e).also { it.block() })
}

fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0

/**
 * 获取ClassNode
 */
fun bufToClassNode(buf: ByteArray) : ClassNode {
    val node = ClassNode(ASM9)
    val cr = ClassReader(buf)

    cr.accept(node, ClassReader.EXPAND_FRAMES)

    return node
}