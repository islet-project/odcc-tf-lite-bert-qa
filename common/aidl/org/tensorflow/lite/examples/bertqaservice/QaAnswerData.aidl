// Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// QaAnswerData.aidl
package org.tensorflow.lite.examples.bertqaservice;

parcelable QaAnswerData {
    String text;
    int start;
    int end;
    float logit;
}
