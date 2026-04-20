// QaAnswerData.aidl
package org.tensorflow.lite.examples.bertqa;

parcelable QaAnswerData {
    String text;
    int start;
    int end;
    float logit;
}