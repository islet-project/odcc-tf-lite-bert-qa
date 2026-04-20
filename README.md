# The TensorFlow Lite BERT Question & Answer Service and Client Demo Applications

This repository contains the code based on https://github.com/tensorflow/examples/tree/master/lite/examples/bert_qa/android that has been adjusted to the AOSP build system and provides a separate bound service implementing the main TF Lite BERT QA functionality.


## Cloning

```
cd $ANDROID_BUILD_TOP
cd external
git clone -b on-device-cc-service-split-host-realm git@github.sec.samsung.net:SYSSEC/odcc-tf-lite-bert-qa.git
```

## AOSP preparations

The AOSP should be patched according to the description taken from [CCServiceDemoApp Readme file](https://github.sec.samsung.net/SYSSEC/odcc-template-android-app/tree/devel/p.sawicki2/handle-binder-parcelable-in-java-experiments?tab=readme-ov-file#preparing-android)

**Differences**:

The packages/modules/Virtualization/build/microdroid/public.libraries.microandroid.txt file should contain following content:
```
/system/lib64/libc++.so
/system/lib64/libc.so
/system/lib64/libdl.so
/system/lib64/libbase.so
```

Apply also the patches from this repo:

- ```art-libnativeloader.patch``` - hanges the visibility of `<AOSP>/art/libnativeloader`
- ```external-icu.patch``` - changes the visibility of `<AOSP>/external/icu`
- ```libnativehelper.patch``` - changes the visibility of `<AOSP>/libnativehelper`
- ```microdroid_launcher.patch``` - configures the library namespace for `<AOSP>/packages/modules/Virtualization/guest/microdroid_launcher`
- ```frameworks-native-libs.patch``` - a workaround that allows to register functions used by Java Binder JNI

## Building

Firstly you need to build patched AOSP.

To build the client and service applications execute the following command.

```
UNBUNDLED_BUILD_SDKS_FROM_SOURCE=true TARGET_BUILD_APPS="TfLiteBertQADemo TfLiteBertQADemoService" m apps_only dist
```

The resulting APK files are located in out/dist folder.

```
out/dist/TfLiteBertQADemo.apk
out/dist/TfLiteBertQADemoService.apk
```

## Installation

You can install them at once by simply running:

```
adb install-multi-package out/dist/TfLiteBertQADemo.apk out/dist/TfLiteBertQADemoService.apk
```

## Running

Launch the Cuttlefish emulator, then:

```
adb root
adb shell
setenforce 0
```

After installation of both apps (client and service). Launch the TfLiteBertQaDemo client app and select the article. Wait 10-15 seconds. Then select or write the question and click the arrow button on the bottom.

Observe the logs:
```
tail -f /data/data/org.tensorflow.lite.examples.bertqaservice/files/microdroid.txt
tail -f /data/data/org.tensorflow.lite.examples.bertqaservice/files/console.txt
```
