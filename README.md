gif效果图
|:---:|
<img src="https://github.com/pinhai/MarqueeView/blob/master/marqueeview.gif" width="300px" height="600px">

```groovy
//项目build.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

//模块build.gradle
dependencies {
    implementation 'com.github.pinhai:MarqueeView:Tag'
}
```