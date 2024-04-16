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
    implementation 'com.github.pinhai:MarqueeView:1.0.3'
}
```

属性
|:---:|

```groovy
<declare-styleable name="MarqueeView">
        <attr name="marqueeview_content" format="string"/> <!--要滚动显示的内容-->
        <attr name="marqueeview_repeat_type" format="enum">
            <enum name="repeat_once_time" value="0"/><!-- 播放一次 -->
            <enum name="repeat_continuous" value="1"/>  <!--连续的，当前字段完全播完，再接着播-->
            <enum name="repeat_continuous_custom_interval" value="2"/>  <!--连续的，两段距离使用marqueeview_item_distance设置-->
            <enum name="repeat_always_show" value="3"/> <!--右边全部显示出来后就结束滚动，依此循环-->
            <enum name="repeat_always_show_once_time" value="4"/> <!--右边全部显示出来后就结束滚动，只滚动一次-->
        </attr>
        <attr name="marqueeview_item_distance" format="dimension"/><!--repeat_continuous_custom_interval模式中item之间的距离-->
        <attr name="marqueeview_text_start_location_distance" format="float"/><!--开始的起始位置，按距离控件左边的比值，0~1之间 -->
        <attr name="marqueeview_text_speed" format="integer"/><!--文字滚动速度，取值范围1~100，越小速度越快-->
        <attr name="marqueeview_text_color" format="color|reference"/><!-- 文字颜色 -->
        <attr name="marqueeview_text_size" format="float"/><!-- 文字大小 -->
        <attr name="marqueeview_is_click_pause" format="boolean"/><!--是否点击暂停，默认为false-->
        <attr name="marqueeview_is_reset_location" format="boolean"/><!--重新改变内容的时候，是否初始化位置，默认为true-->
        <attr name="marqueeview_stay_time_before_scroll" format="integer"/> <!--滚动前停留的时间，单位ms-->
        <attr name="marqueeview_stay_time_scroll_finished" format="integer"/> <!--一次滚动循环结束后停留的时间，单位ms，仅repeat_always_show和repeat_always_show_once_time模式有效-->
    </declare-styleable>
```