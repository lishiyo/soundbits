object Versions {
    val compile_sdk = 27
    val min_sdk = 21
    val target_sdk = 27
    val version_code = 1
    val build_tools_version = "27.0.3"
    val version_name = "1.0"
    val android_gradle_plugin = "3.2.0-alpha02"
    val kotlin_version = "1.2.21"

    val ankoVersion = "0.10.4"
    val stethoVersion = "1.5.0"
    val core_ktx = "0.1"

    val supportVersion = "27.0.2"
    val flexbox = "0.3.2"
    val constraintLayoutVersion = "1.0.2"
    val archVersion = "1.1.0"
    val room = "1.0.0"
    val spotifyAuthVersion = "1.0.0-alpha"
    val spotifySdkVersion = "spotify-player-24-noconnect-2.20b@aar"
    val spotifyWebApiVersion = "0.4.1"
    val retrofitVersion = "2.3.0"
    val okhttpVersion = "3.9.1"
    val daggerVersion = "2.14.1"
    val rxAndroidVersion = "2.0.1"
    val rxKotlin = "2.2.0"
    val rxRelay = "2.0.0"
    val glideVersion = "4.4.0"
    val AVLVersion = "2.1.3"

    val ah_bottom_nav = "2.1.0"
    val lottie = "2.5.0-beta3"
    val toasty = "1.2.8"
    val expansion_panel = "1.0.6"
    val fab_reveal_menu = "1.0.3"
    val fab_loading = "1.0.0"
    val simplerangeview = "0.2.0"
    val carouselview = "0.1.4"
    val equalizer_view = "v0.2"
    val placeholder_view = "0.7.2"
    val circular_music = "v1.3.0"
    val material_dialogs = "0.9.6.0"
    val sectioned_recyclerview = "1.1.3"
    val circleimageview = "2.2.0"
    val navigationtabstrip = "1.0.4"
    val chipcloud = "3.0.5"
}

object Deps {
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin_version}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin_version}"
    val anko = "org.jetbrains.anko:anko-commons:${Versions.ankoVersion}"
    val stetho = "com.facebook.stetho:stetho:${Versions.stethoVersion}"
    val core_ktx = "androidx.core:core-ktx:${Versions.core_ktx}"

    // support libs
    val support_libs = "com.android.support:appcompat-v7:${Versions.supportVersion}"
    val support_design_libs = "com.android.support:design:${Versions.supportVersion}"
    val cardview = "com.android.support:cardview-v7:${Versions.supportVersion}"
    val vector_drawables = "com.android.support:support-vector-drawable:${Versions.supportVersion}"
    val constraint_layout = "com.android.support.constraint:constraint-layout:${Versions.constraintLayoutVersion}"
    val recyclerview = "com.android.support:recyclerview-v7:${Versions.supportVersion}"
    val support_core_utils = "com.android.support:support-core-utils:${Versions.supportVersion}"
    val flexbox = "com.google.android:flexbox:${Versions.flexbox}"

    // arch components
    val arch_components =  "android.arch.lifecycle:extensions:${Versions.archVersion}"
    val arch_components_compiler =  "android.arch.lifecycle:compiler:${Versions.archVersion}"
    val room =  "android.arch.persistence.room:runtime:${Versions.room}"
    val room_compiler =  "android.arch.persistence.room:compiler:${Versions.room}"
    val room_rx =  "android.arch.persistence.room:rxjava2:${Versions.room}"

    // spotify
    val spotify_auth = "com.spotify.android:auth:${Versions.spotifyAuthVersion}"
    val spotify_sdk = "com.spotify.sdk:${Versions.spotifySdkVersion}"
    val spotify_web_api = "com.github.kaaes:spotify-web-api-android:${Versions.spotifyWebApiVersion}"

    // retrofit and gson
    val okhttp3 = "com.squareup.okhttp3:okhttp:${Versions.okhttpVersion}"
    val okhttp3_logging = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttpVersion}"
    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofitVersion}"
    val retrofit_gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofitVersion}"
    val retrofit_rx = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofitVersion}"

    // dagger 2
    val dagger = "com.google.dagger:dagger:${Versions.daggerVersion}"
    val dagger_android = "com.google.dagger:dagger-android:${Versions.daggerVersion}"
    val dagger_android_support = "com.google.dagger:dagger-android-support:${Versions.daggerVersion}"
    val dagger_compiler = "com.google.dagger:dagger-compiler:${Versions.daggerVersion}"
    val dagger_android_compiler = "com.google.dagger:dagger-android-processor:${Versions.daggerVersion}"

    // Rx
    val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxAndroidVersion}"
    val rx_android = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroidVersion}"
    val rx_kotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    val rx_relay = "com.jakewharton.rxrelay2:rxrelay:${Versions.rxRelay}"

    // Image loading
    val glide =  "com.github.bumptech.glide:glide:${Versions.glideVersion}"
    val glide_annotations =  "com.github.bumptech.glide:annotations:${Versions.glideVersion}"
    val glide_compiler =  "com.github.bumptech.glide:compiler:${Versions.glideVersion}"

    // Animation
    val avl = "com.wang.avi:library:${Versions.AVLVersion}"

    // UI elements
    val ah_bottom_nav = "com.aurelhubert:ahbottomnavigation:${Versions.ah_bottom_nav}"
    val lottie = "com.airbnb.android:lottie:${Versions.lottie}"
    val toasty = "com.github.GrenderG:Toasty:${Versions.toasty}"
    val expansion_panel = "com.github.florent37:expansionpanel:${Versions.expansion_panel}"
    val fab_reveal_menu = "com.hlab.fabrevealmenu:fab-reveal-menu:${Versions.fab_reveal_menu}"
    val simplerangeview = "me.bendik.simplerangeview:simplerangeview:${Versions.simplerangeview}"
    val carouselview = "com.synnapps:carouselview:${Versions.carouselview}"
    val equalizer_view = "com.github.gsotti:EqualizerView:${Versions.equalizer_view}"
    val fab_loading = "io.saeid:fab-loading:${Versions.fab_loading}"
    val placeholder_view =  "com.mindorks:placeholderview:${Versions.placeholder_view}"
    val circular_music =  "com.github.aliab:circular-music-progressbar:${Versions.circular_music}"
    val material_dialogs =  "com.afollestad.material-dialogs:core:${Versions.material_dialogs}"
    val sectioned_recyclerview = "io.github.luizgrp.sectionedrecyclerviewadapter:sectionedrecyclerviewadapter:${Versions.sectioned_recyclerview}"
    val circleimageview = "de.hdodenhof:circleimageview:${Versions.circleimageview}"
    val navigation_tab_strip = "com.github.devlight.navigationtabstrip:navigationtabstrip:${Versions.navigationtabstrip}"
    val chipcloud = "com.github.fiskurgit:ChipCloud:${Versions.chipcloud}"
}