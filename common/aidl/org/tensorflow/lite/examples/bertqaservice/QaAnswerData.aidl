// QaAnswerData.aidl
package org.tensorflow.lite.examples.bertqaservice;

parcelable QaAnswerData {
    String text;
    int start;
    int end;
    float logit;
}
