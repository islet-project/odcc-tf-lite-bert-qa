// Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// IBertQaAnswerCallback.aidl
package org.tensorflow.lite.examples.bertqaservice;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;

oneway interface IBertQaAnswerCallback {
    void onError();

    void onAnswer(in List<QaAnswerData> answers);
}
