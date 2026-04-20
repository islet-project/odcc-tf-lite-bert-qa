# The `gen/` folder (AIDL output, reference copy)

This repository keeps a **checked-in copy** of the Java and metadata that **`aidl` produces** from `IBertQaInterface.aidl` (the Soong rules in `ccplugin_template/Android.bp` invoke `aidl` with the CC plugin). The folder is **intentional**: readers who want to see the **actual source of the auto-generated parts**—for review, diffing, or learning—can open `gen/` without building a full AOSP tree. (This layout was requested for the example apps.)

## What `gen/` contains

| Path | Role |
|------|------|
| `for_cc_proxy/BertQaService.java` | **Host-side CC proxy** `Service`: receives client calls on the AIDL surface, drives `VmManager` / `IRealmService`, and forwards `setOptions` / `answer` to the BERT QA implementation running inside the protected VM. |
| `for_cc_proxy/AndroidManifest.xml` | Manifest **fragment** for that CC proxy: virtualization-related permissions / features and an exported `BertQaService` with the app’s intent action. |
| `for_cc_stub/target_service.txt` | Single line: the fully qualified class name of the **VM-side** service that runs TF Lite BERT QA (`org.tensorflow.lite.examples.bertqaservice.BertQaService`). Packaged as a build asset so the CC stack knows which component to bind inside the realm. |

The hand-written implementation that runs **inside** the realm is under `service-app/java/.../BertQaService.java` (`tflite_bertqa_demo_service_service_for_realm` in `service-app/Android.bp`). That is **not** the same artifact as `for_cc_proxy/BertQaService.java`, which is the **AIDL-generated** host-side proxy on the host.

The build runs `aidl` from `ccplugin_template/Android.bp`:

- `tflite_bertqa_demo_service_generate_cc_service` → proxy Java  
- `tflite_bertqa_demo_service_generate_android_manifest` → extra manifest  
- `tflite_bertqa_demo_service_generate_realm_target_service_str` → `target_service.txt`

## How to view or refresh `gen/`

**View without building**  
Open the files under `for_cc_proxy/` and `for_cc_stub/` in this directory.

**Regenerate via AOSP / Soong (authoritative)**  
With this project in the tree (e.g. `external/odcc-tf-lite-bert-qa`), build the service module, for example:

```
m TfLiteBertQADemoService
```

(or the `apps_only dist` flow in the [top-level README](../README.md#building)). Soong writes intermediates under `out/soong/.intermediates/`. Locate the outputs of the three modules named above (proxy `.java`, generated `AndroidManifest.xml`, `target_service.txt`) and **copy** them into `for_cc_proxy/` and `for_cc_stub/` if you are updating the reference snapshot. A quick search:

```
find out/soong/.intermediates -path '*tflite_bertqa_demo_service_generate*' 2>/dev/null | head
find out/soong/.intermediates -name 'target_service.txt' 2>/dev/null | head
```

**Regenerate with `aidl` manually (optional)**  
`for_cc_proxy/BertQaService.java` begins with a comment that records an **`aidl` command line** from a prior run. With the same inputs under `common/aidl/`, you can run a similar invocation and adjust `-o`, `-N`, and include paths to match your checkout; the flags should mirror `ccplugin_template/Android.bp` for the three rules above.