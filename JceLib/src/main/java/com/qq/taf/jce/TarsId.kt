package com.qq.taf.jce

/**
 * Tars序列化注解
 * @author 洛洛 & 企鹅
 * */
@Target(AnnotationTarget.FIELD)
annotation class TarsId(val id: Int, val require: Boolean = false)