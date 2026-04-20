// Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// IBertQaInterface.aidl
package org.tensorflow.lite.examples.bertqaservice;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;

@GenerateCCService(FQCN="org.tensorflow.lite.examples.bertqaservice.BertQaService")
interface IBertQaInterface {
    void setOptions(int currentDelegate, int numThreads);

    List<QaAnswerData> answer(String context, String question);
}
