# AI Model Provisioining

This repository contains the code of the TensorFlow Lite BERT QA application
[https://github.com/tensorflow/examples/tree/master/lite/examples/bert_qa/android](https://github.com/tensorflow/examples/tree/master/lite/examples/bert_qa/android)
that has been adapted to the AOSP build system and provides a separate bound
service implementing the main TF Lite BERT QA functionality.

The service uses a provisioning mechanism to provision the TensorFlow Bert QA
model from an external provisioning server.

## Source code

The source code of the application should be automatically downloaded into
`external/odcc-tf-lite-bert-qa-provisioning` once the AOSP supporting CC
services is downloaded using the steps below.

# Running the application with the Android ODCC stack

## Android ODCC

We first need to checkout and setup the Android On Device Confidential Computing
stack. The repository and steps we need to follow are here:
[https://github.com/islet-project/odcc-islet-script-qemu-rme](https://github.com/islet-project/odcc-islet-script-qemu-rme).

Please checkout that repository and follow the steps in its README. Before
continuing here please make sure it runs successfully and you can connect to the
the Android both through `adb shell` and graphically (e.g. with VNC).

## Environment setup

You should have checked out two repositories, the repository above and
[Islet](https://github.com/islet-project/islet) as instructed. Those two
directories will be referenced throughout this README as `$ANDROID` and `$ISLET`
respectively.

We will need one more repository, so check it out now:
https://github.com/islet-project/ratls. We'll be referencing this repository as
`$RATLS`.

Also please make sure to setup the android development environment. Basically,
in every shell that will run commands presented here please make sure to execute
the following:

```
export ANDROID="/path/to/odcc-islet-script-qemu-rme"
export ISLET="/path/to/islet-project/islet"
export RATLS="/path/to/islet-project/ratls"

cd "$ANDROID"/aosp-15.0.0_r8
source build/envsetup.sh
lunch aosp_cf_arm64_only_phone-trunk_staging-userdebug
```

The easiest way would be to write this (with correct paths) to some temporary
file and source this file in every new terminal. With that all the commands
presented in this README (bar one) can be copy & pasted verbatim.

The successful output of Android development environment setup should look like
this:

```
============================================
PLATFORM_VERSION_CODENAME=VanillaIceCream
PLATFORM_VERSION=VanillaIceCream
TARGET_PRODUCT=aosp_cf_arm64_only_phone
TARGET_BUILD_VARIANT=userdebug
TARGET_ARCH=arm64
TARGET_ARCH_VARIANT=armv8-a
TARGET_CPU_VARIANT=cortex-a53
HOST_OS=linux
HOST_OS_EXTRA=Linux-6.8.0-106-generic-x86_64-Ubuntu-24.04.4-LTS
HOST_CROSS_OS=linux_musl
BUILD_ID=AP4A.241205.013.B1
OUT_DIR=out
============================================
```

## Building the BertQa application

Now build the client and service applications:

```
cd "$ANDROID"/aosp-15.0.0_r8
UNBUNDLED_BUILD_SDKS_FROM_SOURCE=true TARGET_BUILD_APPS="TfLiteBertQADemoProvisioning TfLiteBertQADemoServiceProvisioning" m apps_only dist
```

The resulting APK files are located in out/dist folder.

1. `out/dist/TfLiteBertQADemoProvisioning.apk`
2. `out/dist/TfLiteBertQADemoServiceProvisioning.apk`

## Gathering measurements

For provisioning to work we need attestation. And for attestation to work we
need reference measurements that are going to be provisioned to the attestation
verification services.

Measurements consist of platform measurements (bootloaders, TF-A, Islet RMM,
etc.) and realm measurements (realm, microdroid, application).

### Platform reference measurements

Platform measurements are in form of a CCA attestation token. To obtain the
attestation token we need to run the android, then microdroid and execute a
command on the microdroid environment.

*Terminal 1*:

```
cd "$ANDROID"
./scripts/run_cuttlefish.sh "$ISLET"
```

*Terminal 2*:

```
adb -s 0.0.0.0:6520 wait-for-device
adb -s 0.0.0.0:6520 root
adb -s 0.0.0.0:6520 shell setprop persist.avf.kvmtool true
adb -s 0.0.0.0:6520 shell setprop persist.avf.realm true
```

After the above commands executed successfully wait a minute and run the
microdroid, *Terminal 2*:

```
adb -s 0.0.0.0:6520 shell /apex/com.android.virt/bin/vm run-microdroid
```

After the microdroid has been booted (new logs on *Terminal 2* stop appearing)
connect to microdroid and get the token.

*Terminal 3*:

```
vm_shell connect
```

*Terminal 3* inside microdroid:
```
rsictl attest -o /data/token.bin
exit
```

Download the token and stop the emulator, *Terminal 3*:

```
cd $ANDROID
adb -s localhost:8000 pull /data/token.bin .
stop_cvd
```

### Realm reference measurements

Realm measurements are in a form of RIM (Realm Initial Measurement) and REMs
(Realm Extensible Measurements) values. We provide tools for measuring those
values.

First build the tools:

```
cd "$ANDROID"/aosp-15.0.0_r8
m realm_metadata_tool
m microdroid_calculate_rem
m ccservice_calculate_rem
```

Obtain `RIM`:

```
cd "$ANDROID"/aosp-15.0.0_r8
realm_metadata_tool dump -i out/target/product/vsoc_arm64_only/apex/com.android.virt/etc/fs/microdroid_realm_metadata.bin | grep "^rim:"
```

Obtain `REM0`:

```
cd "$ANDROID"/aosp-15.0.0_r8
microdroid_calculate_rem   --vbmeta-system-image out/target/product/vsoc_arm64_only/apex/com.android.virt/etc/fs/microdroid_vbmeta.img | grep "^REM0:"
```

Obtain `REM1`:

```
cd "$ANDROID"/aosp-15.0.0_r8
ccservice_calculate_rem --main-apk out/dist/TfLiteBertQADemoServiceProvisioning.apk  \
                        --apex-root-path ./out/soong/.intermediates/   \
                        --extra-apk-root-path out/target/product/vsoc_arm64_only/system/ \
                        --config-file-name vm_config.json
```

Please write down the 3 obtained values (`RIM`, `REM0`, `REM1`). We will need
them in the next step.

## Provisioning server

We need to setup a server that will provision the model to the
application/service over the network. This server resides with `ratls`
repository we've checked out earlier. For it to work fully we need to setup
everything with the measurements we've obtained earlier.

### Realm reference measurements

Realm measurements are handled directly by the provisioning server. Using the
obtained RIM/REMs values set them as follows (**this command needs to be amended,
cannot be pasted as is**):

```
export RIM="HEX_RIM_VALUE"
export REM0="HEX_REM0_VALUE"
export REM1="HEX_REM1_VALUE"
```

Now generate the file with reference realm measurements for use with the
provisioning server:

```
cd "$RATLS"/tools/ratls-serve/ratls
cat > odcc.json << EOF
{
    "version": "0.1",
    "issuer": {
        "name": "Samsung",
        "url": "https://cca-realms.samsung.com/"
    },
    "realm": {
        "uuid": "f7e3e8ef-e0cc-4098-98f8-3a12436da040",
        "name": "Data Processing Service",
        "version": "1.0.0",
        "release-timestamp": "2026-03-24T09:00:00Z",
        "attestation-protocol": "HTTPS/RA-TLSv1.0",
        "port": 8088,
        "reference-values": {
            "rim": "$RIM",
            "rems": [
                [
                    "$REM0",
                    "$REM1",
                    "0000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000"
                ]
            ],
            "hash-algo": "sha-256"
        }
    }
}
EOF
```

### Platform reference measurements

This requires Veraison services running and configured. This in turn requires to
have `docker` and `go` installed and working.

This step can be omitted in case of issues. We will have to disable Veraison
usage in such case when running the provisioning server.

Build and run the Veraison service. This process is mostly automated using the
following script:

```
cd "$ISLET"/examples/app-provisioning/veraison
./bootstrap.sh
```

We can check whether the Veraison has been run successfully with:

```
cd "$ISLET"/examples/app-provisioning/veraison
source services/deployments/docker/env.bash
veraison status
```

The command should output something like:

```
         vts: running
provisioning: running
verification: running
  management: running
    keycloak: running
```

We also need a public CPAK key that is used to verify the platform token. The
token is generated and signed with the usage of private CPAK key by the HES
service run in the background with the Android ODCC stack automatically.

To obtain the CPAK public key:

```
cd "$ISLET"/hes/cpak-generator
cargo run
```

We also need to build one tool used by scripts below:

```
cd "$ISLET"/examples/app-provisioning
make bin/rocli
```

Now provision the already running Veraison with platform reference measurements
and the public part of CPAK key:

```
cd "$ISLET"/examples/app-provisioning/veraison/provision
./run.sh -t "$ANDROID"/token.bin -c "$ISLET"/hes/out/cpak_public.pem
```

We can see the the platform reference measurements and the public part of CPAK
key inside Veraison with:

```
cd "$ISLET"/examples/app-provisioning/veraison
source services/deployments/docker/env.bash
veraison stores
```

## Running the whole stack

### Running the Android ODCC and services

First run the provisioning server. The model being provisioned is already
included in the repository (`$RATLS/tools/ratls-serve/root/demo-model.tflite`).

*Terminal 1*:

If we have Veraison service running and configured:

```
cd "$RATLS"/tools/ratls-serve
cargo run -- -j ratls/odcc.json
```

If Veraison is not running:

```
cd "$RATLS"/tools/ratls-serve
cargo run --features disable-veraison -- -j ratls/odcc.json
```

Run the Android with ODCC, *Terminal 2*:

```
cd "$ANDROID"
./scripts/run_cuttlefish.sh "$ISLET"
```

*Terminal 3*:

```
adb -s 0.0.0.0:6520 wait-for-device
adb -s 0.0.0.0:6520 root
adb -s 0.0.0.0:6520 shell setprop persist.avf.kvmtool true
adb -s 0.0.0.0:6520 shell setprop persist.avf.realm true
```

Connect to the Android graphically and wait for it to boot completely, until the
lock screen is visible.

### Running the BertQA application

When Android is up and running install the application we built previously:

```
cd "$ANDROID"/aosp-15.0.0_r8
adb -s 0.0.0.0:6520 install-multi-package out/dist/TfLiteBertQADemoProvisioning.apk out/dist/TfLiteBertQADemoServiceProvisioning.apk
```

Unlock the screen, open the application launcher and run the first (the one on
the left) `TFLite Bert Qa Demo` app. Alternatively you can do that from command
line:

```
adb -s 0.0.0.0:6520 shell input keyevent KEYCODE_MENU
adb -s 0.0.0.0:6520 shell am start org.tensorflow.lite.examples.bertqa/.MainActivity
```

Select an article. After you see the spinner you can observe the microdroid logs
with:

```
adb -s 0.0.0.0:6520 shell tail -f /data/data/org.tensorflow.lite.examples.bertqaservice/files/console.txt
```

Wait a couple of minutes. You should see the microdroid VM booting in the
logs. First the kernel, then the microdroid manager, application CC service and
then the provisioning client that will download the model.

You can see the provisioning process in the logs with lines similar to:

```
BertQaService: The model is being initialized
BertQaService: Starting provisionOrInitModelFromFile()
BertQaService: The TFLite Bert QA model doesn't exist in encryptedstore - start provisioning...
54   130 I microdroid_manager: microdroid_manager::vm_payload_service: startProvisioning has been called https://192.168.97.1:1337/demo-model.tflite ca-cert.pem demo-model.tflite
[INFO  ratls_get] Cli {
        root_ca: "/mnt/apk/assets/ca-cert.pem",
        url: "https://192.168.97.1:1337/demo-model.tflite",
        output: "/mnt/encryptedstore/demo-model.tflite",
        tls: RaTLS,
        token: None,
        cont: false,
        retry: 3,
    }
[INFO  ratls::cert_resolver] Generating RSA 2048bit key.
[INFO  ratls::cert_resolver] Finished generating RSA key.
[INFO  ratls_get] Saving as: "/mnt/encryptedstore/demo-model.tflite"
[INFO  ratls_get] Downloaded 100670436 bytes
54   136 I microdroid_manager: microdroid_manager::vm_payload_service: Provisioning of https://192.168.97.1:1337/demo-model.tflite to demo-model.tflite succeded
BertQaService: Provisioning of the TFLite Bert QA model has succeeded URL: https://192.168.97.1:1337/demo-model.tflite destination: demo-model.tflite
BertQaService: Setup of TFLite Bert QA model
BertQaService: The TFLite Bert QA model has been successfully initialized
```

The provisioning server on *Terminal 1* will also show the logs of a network
connection, verification process and serving the model file.

Of importance are lines like those:

```
[INFO  ratls::cert_verifier] Received client CCA token:
[DEBUG reqwest::connect] starting new connection: https://localhost:8080/
[INFO  veraison_verifier::verifier] Submod CCA_SSD_PLATFORM affirms token
[INFO  veraison_verifier::verifier] Verification passed successfully
[DEBUG realm_verifier] RIM match
[DEBUG realm_verifier] REMs match
[DEBUG realm_verifier] Hash algorithm match
[DEBUG tower_http::trace::make_span] request; method=GET uri=/demo-model.tflite version=HTTP/1.1
[DEBUG tower_http::trace::on_request] started processing request
[INFO  ratls_serve::httpd] Handling payload request: demo-model.tflite
[DEBUG tower_http::trace::on_response] finished processing request latency=0 ms status=200

```

Now, going back to the application UI, the spinner should disappear and we can
select or write a question under the article and click the blue arrow at the
bottom. Note that the inference process may also take several minutes.
