package com.qq.taf.jce.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.qq.taf.jce.plugin.ext.ClassFuller
import com.qq.taf.jce.plugin.tars.TarsPrecompile
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * 安卓专用处理器 遍历classes用的
 * */
class AndroidTransform(
    private val project: Project,
    private val android: BaseExtension,
    private val b: Boolean
) :
    Transform() {
    override fun getName(): String {
        return AndroidTransform::class.java.simpleName
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    // 指定 Transform 处理的数据， CONTENT_CLASS 表示处理 java class 文件，
    //  CONTENT_RESOURCES, 表示处理 java 的资源
    override fun getOutputTypes(): Set<QualifiedContent.ContentType> {
        return inputTypes
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? {
        return if (b) TransformManager.SCOPE_FULL_PROJECT else mutableSetOf(
            QualifiedContent.Scope.PROJECT
        )
    }

    override fun isIncremental(): Boolean {
        return true
    }

    val bootPath = ArrayList<File>()

    // Transform 要操作的内容范围
    // 1.PROJECT 只有项目内容
    // 2.SUB_PROJECTS 只有子项目内容
    // 3.EXTERNAL_LIBRARIES 只有外部库
    // 4.TESTED_CODE 当前变量（包括依赖项）测试的代码
    // 5.PROVIDED_ONLY 本地或者员村依赖项
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        // OutputProvider管理输出路径，如果消费型输入为空，你会发现OutputProvider == null
        val outputProvider = transformInvocation.outputProvider

        transformInvocation.inputs.forEach(Consumer { input ->
            input.jarInputs.forEach(Consumer { jarInput ->
                bootPath.add(jarInput.file)
            })
            input.directoryInputs.forEach(Consumer { directoryInput ->
                bootPath.add(directoryInput.file)
            })
        })

        transformInvocation.inputs.forEach(Consumer { input ->
            input.jarInputs.forEach(Consumer { jarInput ->
                try {
                    processJarInput(jarInput, outputProvider)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
            input.directoryInputs.forEach(Consumer { directoryInput ->
                try {
                    processDirectoryInputs(directoryInput, outputProvider)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
        })
    }

    fun processJarInput(jarInput: JarInput, outputProvider: TransformOutputProvider) {
        val dest = outputProvider.getContentLocation(
            jarInput.file.absolutePath,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
//        println("-----" + jarInput.file)
//        outputProvider.deleteAll()//?????????
        // to do some transform
//        handleJarInputs(jarInput, outputProvider)

        // 将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
//        jarInput.file.copyTo(dest, true)

        if (jarInput.file.absolutePath.contains("build/intermediates") && jarInput.file.absolutePath.endsWith(".jar")) {
            //重名名输出文件,因为可能同名,会覆盖
            println("jce process local jar:" + jarInput.file.absolutePath)
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            val tmpFile = File(jarInput.file.parent + File.separator + jarInput.file.name + "_temp.jar")
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
            //用于保存
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(jarEntry)
                //插桩class
                if (entryName.endsWith(".class") && "R.class" != entryName && "BuildConfig.class" != entryName) {
                    //class文件处理
                    val readBytes = inputStream.readBytes()
                    val fuller = ClassFuller()
                    fuller.from(readBytes)
                    TarsPrecompile(fuller).invoke().apply {
                        onSuccess {
                            println("jce process local class:$entryName")
                            jarOutputStream.putNextEntry(zipEntry)
                            jarOutputStream.write(it)
                        }
                        onFailure {
                            jarOutputStream.putNextEntry(zipEntry)
                            jarOutputStream.write(readBytes)
                        }
                    }
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(inputStream.readBytes())
                }
                jarOutputStream.closeEntry()
            }
            //结束
            jarOutputStream.close()
            jarFile.close()

            tmpFile.copyTo(dest, true)
//            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        } else {
            jarInput.file.copyTo(dest, true)
        }
    }

    fun processDirectoryInputs(
        directoryInput: DirectoryInput,
        outputProvider: TransformOutputProvider
    ) {
        val dest = outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes, scopes,
            Format.DIRECTORY
        )
        // 建立文件夹
        dest.mkdirs()
//        println("-----------" + directoryInput.file)
        directoryInput.file.walk().maxDepth(1000)
            .filter { it.isFile && it.name.endsWith(".class") }
            .filter { it.name != "BuildConfig.class" }
            .filter { it.name != "R.class" }
            .forEach { file ->
                val fuller = ClassFuller()
                fuller.from(file.readBytes())
                TarsPrecompile(fuller).invoke().apply {
                    onSuccess {
                        println("jce process local class:${file.name}")
                        file.writeBytes(it)
                    }
                }
            }

        // 将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        directoryInput.file.copyRecursively(dest, true)

    }
}