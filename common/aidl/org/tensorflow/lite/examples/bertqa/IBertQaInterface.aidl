// IBertQaInterface.aidl
package org.tensorflow.lite.examples.bertqa;

import org.tensorflow.lite.examples.bertqa.QaAnswerData;

interface IBertQaInterface {
    void setOptions(int currentDelegate, int numThreads);

    List<QaAnswerData> answer(String context, String question);
}
