// Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// IBertQaInterface.aidl
package org.tensorflow.lite.examples.bertqaservice;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;
import org.tensorflow.lite.examples.bertqaservice.IBertQaAnswerCallback;
import org.tensorflow.lite.examples.bertqaservice.IBertQaInitModelCallback;

@GenerateCCService(FQCN="org.tensorflow.lite.examples.bertqaservice.BertQaService")
interface IBertQaInterface {
    void initializeModel(int currentDelegate, int numThreads, in IBertQaInitModelCallback callback);

    void answer(String context, String question, in IBertQaAnswerCallback callback);
}
