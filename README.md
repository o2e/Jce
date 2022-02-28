# 使用必读

```groovy
//project file settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal() //非常重要，否则插件无法安装
    }
}

//或者
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal() //非常重要，否则插件无法安装
}
```

可用注解 ``@Jce() @Tars @JceId() @TarsId()``

注意看JceStruct里面有 servantName funcName等方法 可以用注解实现填空@Jce(funcName="阿巴巴")

@飞翔的企鹅 小小企鹅可可爱爱吸洛洛