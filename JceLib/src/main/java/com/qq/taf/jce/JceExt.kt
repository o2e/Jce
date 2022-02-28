package com.qq.taf.jce

import java.lang.reflect.Constructor

/**
 * 就是调用个toByteArray()而已
 * */
inline fun <reified T : JceStruct> T.encodeTars(): ByteArray = encodeJce()

/**
 * 就是调用个toByteArray()而已
 * */
inline fun <reified T : JceStruct> T.encodeJce(): ByteArray {
    return toByteArray()
}

/**
 * 特别注意 这里采用无参构造函数反射初始化对象,
 * 如果是kotlin data class 需要用一个官方的插件 kotlin("plugin.noarg") version "1.6.10"
 * 如果是普通class 则可以自己手动写个无参构造函数
 * */
inline fun <reified T : JceStruct> ByteArray.decodeTars(): T = decodeJce()

/**
 * 特别注意 这里采用无参构造函数反射初始化对象,
 * 如果是kotlin data class 需要用一个官方的插件 kotlin("plugin.noarg") version "1.6.10"
 * 如果是普通class 则可以自己手动写个无参构造函数
 * */
inline fun <reified T : JceStruct> ByteArray.decodeJce(): T {
    val clazz = T::class.java
    val apply = _JceDecodeClassCache.getOrPut(clazz) { clazz.getConstructor() }.newInstance() as T
    apply.readFrom(this)
    return apply
}

val _JceDecodeClassCache = HashMap<Class<*>, Constructor<*>>()