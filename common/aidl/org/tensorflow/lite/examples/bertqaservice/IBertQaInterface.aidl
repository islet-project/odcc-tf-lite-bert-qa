// IBertQaInterface.aidl
package org.tensorflow.lite.examples.bertqaservice;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;

@GenerateCCService(FQCN="org.tensorflow.lite.examples.bertqaservice.BertQaService")
interface IBertQaInterface {
    void setOptions(int currentDelegate, int numThreads);

    List<QaAnswerData> answer(String context, String question);
}
