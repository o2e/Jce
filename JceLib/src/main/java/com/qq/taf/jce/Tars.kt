package com.qq.taf.jce

/**
 * Tars序列化注解, 搭配 @TarsId 使用
 * @author 洛洛 & 企鹅
 * */
@Target(AnnotationTarget.CLASS)
annotation class Tars(
    val read: Boolean = true,
    val write: Boolean = true,
    val servantName: String = "",
    val funcName: String = "",
    val reqName: String = "",
    val respName: String = ""
)