package com.qq.taf.jce.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.qq.taf.jce.plugin.ext.ClassFuller
import com.qq.taf.jce.plugin.tars.TarsPrecompile
import com.qq.taf.jce.plugin.util.FileUtil
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tars、Jce编译时处理器 支持 Android Project、Java Project
 * android多Module支持处理依赖Module里面的Jce、Tars类 但是要手动给那个Module添加Jce、Tars依赖
 * @author 洛洛 & 企鹅
 * */
open class BasePlugin : Plugin<ProjectInternal> {
    override fun apply(target: ProjectInternal) {

//        这里是添加插件后自动给项目添加 JceLib依赖
        target.dependencies.add(
            "implementation", target.dependencies.create("com.qq.taf:jce:1.0.1")
        )

        println("o2e jce enable.")
        val hasAppPlugin = target.plugins.hasPlugin(AppPlugin::class.java)
        if (hasAppPlugin) {
            val android = target.extensions.getByType(AppExtension::class.java)
            android.registerTransform(AndroidTransform(target, android, true))
        } else if (target.plugins.hasPlugin(LibraryPlugin::class.java)) {
            val libraryExtension = target.extensions.getByType(LibraryExtension::class.java)
            libraryExtension.registerTransform(AndroidTransform(target, libraryExtension, false))
        } else {
            target.afterEvaluate { thisProject ->
                thisProject.getTasksByName("classes", false).forEach { task ->
                    task.doLast {
                        val int = AtomicInteger(0)
                        val buildDir = File(target.buildDir, "classes")
                        FileUtil.traverseFile(buildDir.absolutePath) { _, classFile ->
                            if (classFile.name.endsWith(".class")) {
                                val thread = object : Thread() {
                                    override fun run() {
                                        int.incrementAndGet()
                                        val fuller = ClassFuller()
                                        classFile.inputStream().use {
                                            fuller.from(it)
                                        }

                                        TarsPrecompile(fuller).invoke().apply {
                                            onSuccess {
                                                classFile.writeBytes(it)
                                            }
                                        }

                                        println("Jce Task-${classFile.name} finished, rest: ${int.decrementAndGet()}")
                                    }
                                }
                                thread.start()
                            }
                        }

                        while (int.get() > 0) {
                            Thread.sleep(500) // 等待所有任务结束
                        }
                    }
                }
            }
        }
    }
}