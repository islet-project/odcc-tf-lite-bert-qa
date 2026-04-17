// Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// IBertQaInitModelCallback.aidl
package org.tensorflow.lite.examples.bertqaservice;

oneway interface IBertQaInitModelCallback {
    void onError(String errorMessage);

    void onSuccess();
}
