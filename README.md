# TensorFlow Lite BERT QA

This repository contains the code based on [https://github.com/tensorflow/examples/tree/master/lite/examples/bert_qa/android](https://github.com/tensorflow/examples/tree/master/lite/examples/bert_qa/android) that has been adapted to the AOSP build system and provides a separate bound service implementing the main TF Lite BERT QA functionality.


## Source code

The source code of the application should be automatically downloaded into `external/odcc-tf-lite-bert-qa` once the AOSP supporting CC services is downloaded.

## The `gen/` folder

`gen/` holds a **checked-in reference copy** of Java and metadata that **`aidl`** generates from `IBertQaInterface.aidl` (invoked from `ccplugin_template/Android.bp`), so you can read the auto-generated proxy, manifest fragment, and realm target hint without a full tree build. For layout, cross-links to other example repos, what each path contains, and how to refresh the snapshot from Soong or `aidl`, see **[gen/README.md](gen/README.md)**.

## Building

To build the client and service applications, execute the following command.

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
```

This allows you to run the CC service inside a standard VM (crosvm VMM is used to launch the service).
To run the CC service in a Realm VM, type the following commands:

```
setprop persist.avf.kvmtool true
setprop persist.avf.realm true
```

After installation of both apps (client and service), launch the TfLiteBertQaDemo client app and select the article. Depending on the used platform and VMM, it may take from several seconds to a couple of minutes to load the Tensorflow Lite model. Then select a question from the list and click the right arrow button at the bottom.

Observe the logs:
```
tail -f /data/data/org.tensorflow.lite.examples.bertqaservice/files/microdroid.txt
tail -f /data/data/org.tensorflow.lite.examples.bertqaservice/files/console.txt
```
