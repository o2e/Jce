package com.qq.taf.jce

/**
 * Jce序列化注解, 搭配 @JceId 使用
 * @author 洛洛 & 企鹅
 * */
@Target(AnnotationTarget.CLASS)
annotation class Jce(
    val read: Boolean = true,
    val write: Boolean = true,
    val servantName: String = "",
    val funcName: String = "",
    val reqName: String = "",
    val respName: String = ""
)