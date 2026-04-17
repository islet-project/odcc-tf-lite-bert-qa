/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 * Copyright (c) 2026 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.bertqaservice;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.stream.Collectors;

import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer;
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer.BertQuestionAnswererOptions;
import org.tensorflow.lite.task.text.qa.QaAnswer;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;
import org.tensorflow.lite.examples.bertqaservice.IBertQaInterface;

public class BertQaService extends Service {

    private static final String TAG = "BertQaService";
    private static final String BERT_QA_MODEL = "mobilebert.tflite";

    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final int DELEGATE_NNAPI = 2;

    private BertQuestionAnswerer bertQuestionAnswerer = null;
    private int numThreads = 2;
    private int currentDelegate = DELEGATE_CPU;

    private void setupBertQuestionAnswerer() throws RemoteException {
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads);

        BertQuestionAnswerer.BertQuestionAnswererOptions options = BertQuestionAnswererOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .build();

        try {
            bertQuestionAnswerer = BertQuestionAnswerer.createFromFileAndOptions(getApplicationContext(), BERT_QA_MODEL,
                    options);
        } catch (Exception e) {
            Log.e(TAG, "TFLite failed to load model with error: " + e.getMessage());
            throw new RemoteException("Cannot initialize BertQuestionAnswerer " + e.getMessage());
        }
    }

    List<QaAnswer> answer(String contextOfQuestion, String question) throws RemoteException {
        if (bertQuestionAnswerer == null) {
            setupBertQuestionAnswerer();
        }

        return bertQuestionAnswerer.answer(contextOfQuestion, question);
    }

    private static QaAnswerData fromQaAnswer(QaAnswer answer) {
        QaAnswerData answerData = new QaAnswerData();
        answerData.text = answer.text;
        answerData.start = answer.pos.start;
        answerData.end = answer.pos.end;
        answerData.logit = answer.pos.logit;
        return answerData;
    }

    private IBinder mService = new IBertQaInterface.Stub() {

        @Override
        public void setOptions(int currentDelegate, int numThreads) throws RemoteException {
            if (bertQuestionAnswerer != null) {
                throw new RemoteException("BertQuestionAnswerer has been already instantiated");
            }

            switch (currentDelegate) {
                case DELEGATE_CPU: // CPU
                    break;
                case DELEGATE_GPU: // GPU
                    throw new RemoteException("GPU backend for TfLite is not supported");
                case DELEGATE_NNAPI: // NNAPI
                    throw new RemoteException("NNAPI backend for TfLite is not supported");
            }

            BertQaService.this.currentDelegate = currentDelegate;
            BertQaService.this.numThreads = numThreads;
        }

        @Override
        public List<QaAnswerData> answer(String context, String question) throws RemoteException {
            List<QaAnswer> answers = BertQaService.this.answer(context, question);

            return answers.stream()
                    .map(BertQaService::fromQaAnswer)
                    .collect(Collectors.toList());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public BertQaService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mService;
    }
}
