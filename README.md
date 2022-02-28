# 使用必读

### 第一步

```groovy
//gradle 运行publishing task 吧依赖库发布到本地maven库中
Jce:
JcePlugin[publishToMavenLocal]
Jce:
JceLib[publishToMavenLocal]
```

### 第二步添加本地maven源

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

### 第三步 安装插件

普通 gradle、kts 可以这样用

```groovy
id("taf-jce") version "1.0.1"
```

android 可以这样用

```groovy
//新款base
id 'taf-jce' version '1.0.1' apply false

//老式的项目base是这样的
buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("taf-jce:taf-jce.gradle.plugin:1.0.1")
    }
}

//app模块
plugins {
    id 'taf-jce'
}

```

### 可用注解

``@Jce()`` ``@Tars`` ``@JceId()`` ``@TarsId()``

注意看JceStruct里面有 servantName funcName等方法 可以用注解实现填空@Jce(funcName="阿巴巴")

@飞翔的企鹅 小小企鹅可可爱爱吸洛洛