package com.qq.taf.jce

/**
 * Jce序列化注解
 * @author 洛洛 & 企鹅
 * */
@Target(AnnotationTarget.FIELD)
annotation class JceId(val id: Int, val require: Boolean = false)