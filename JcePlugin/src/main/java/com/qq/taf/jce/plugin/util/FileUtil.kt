package com.qq.taf.jce.plugin.util

import java.io.File

object FileUtil {
    @JvmStatic
    fun traverseFile(dir: String, block : (index : Int, f : File) -> Unit) {
        traverseDir(dir) { index, f ->
            if (f.isDirectory) {
                traverseFile(f.absolutePath, block)
            } else {
                block(index, f)
            }
        }
    }

    @JvmStatic
    fun traverseDir(dir: String, block : (index : Int, f : File) -> Unit) = File(dir).listFiles().forEachIndexed(block)

    @JvmStatic
    fun has(string: String) = File(string).exists()
}