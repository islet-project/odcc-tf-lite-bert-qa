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

import java.io.File;
import java.lang.IllegalStateException;
import java.lang.NullPointerException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.system.virtualization.payload.IProvisioningCallback;
import android.system.virtualization.payload.ProvisioningError;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.islet.Cca;

import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer;
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer.BertQuestionAnswererOptions;
import org.tensorflow.lite.task.text.qa.QaAnswer;

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData;
import org.tensorflow.lite.examples.bertqaservice.IBertQaInterface;
import org.tensorflow.lite.examples.bertqaservice.IBertQaAnswerCallback;
import org.tensorflow.lite.examples.bertqaservice.IBertQaInitModelCallback;


public class BertQaService extends Service {

    private static final String TAG = "BertQaService";
    private static final String BERT_QA_MODEL = "demo-model.tflite";
    private static final String BERT_QA_MODEL_URL = "https://192.168.97.1:1337/" + BERT_QA_MODEL;
    private static final String BERT_QA_MODEL_SERVER_CA_CERTIFICATE = "ca-cert.pem"; // located in assets folder

    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final int DELEGATE_NNAPI = 2;

    private static final int MIN_NUM_THREADS = 1;
    private static final int MAX_NUM_THREADS = 4;
    private static final int MAX_EXECUTOR_SERVICE_THREADS = 4;

    /**
     * Model state enumeration to track the lifecycle of the BERT QA model.
     */
    private enum ModelState {
        UNINITIALIZED,   // No model loaded
        INITIALIZING,    // Model is being initialized
        READY,           // Model is ready for inference
        ERROR            // Model failed to initialize
    }

    private String mBertQaProvisionedModelPath = null;
    private BertQuestionAnswerer mBertQuestionAnswerer = null;
    private ModelState mModelState = ModelState.UNINITIALIZED;
    private int mNumThreads = 2;
    private IBertQaInitModelCallback mInitModelCallback = null;
    private ExecutorService mExecutorService;
    private final Object mStateLock = new Object();

    private void notifyInitializationError(IBertQaInitModelCallback callback, String message) {
        if (callback != null) {
            Log.e(TAG, message);
            try {
                callback.onError(message);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onError on IBertQaInitModelCallback callback: " + ex.getMessage(), ex);
            }
        }
    }

    private void notifyInitializationSuccess(IBertQaInitModelCallback callback, String message) {
        if (callback != null) {
            Log.i(TAG, message);
            try {
                callback.onSuccess();
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onSuccess IBertQaInitModelCallback callback: " + ex.getMessage(), ex);
            }
        }
    }

    private void setupBertQuestionAnswerer() {
        Log.i(TAG, "Setup of TFLite Bert QA model");

        IBertQaInitModelCallback callbackToNotify;
        ModelState modelState = ModelState.UNINITIALIZED;
        int numThreads = 2;
        synchronized (mStateLock) {
            numThreads = mNumThreads;
            callbackToNotify = mInitModelCallback;
            mInitModelCallback = null;
            modelState = mModelState;
        }

        if (modelState != ModelState.INITIALIZING) {
            notifyInitializationError(callbackToNotify, "setupBertQuestionAnswerer() should be called in ModelState.INITIALIZING state");
            return;
        }

        if (mBertQaProvisionedModelPath == null) {
            notifyInitializationError(callbackToNotify, "mBertQaProvisionedModelPath is null");
            return;
        }

        try {
            BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads);
            BertQuestionAnswerer.BertQuestionAnswererOptions options = BertQuestionAnswererOptions.builder()
                    .setBaseOptions(baseOptionsBuilder.build())
                    .build();
            BertQuestionAnswerer newModel = BertQuestionAnswerer.createFromFileAndOptions(new File(mBertQaProvisionedModelPath), options);

            synchronized (mStateLock) {
                mBertQuestionAnswerer = newModel;
                mModelState = ModelState.READY;
            }

            // This is called outside the synchronized block to prevent deadlock
            notifyInitializationSuccess(callbackToNotify, "The TFLite Bert QA model has been successfully initialized");
        } catch (Exception e) {
            synchronized (mStateLock) {
                mBertQuestionAnswerer = null;
                mModelState = ModelState.ERROR;
            }

            notifyInitializationError(callbackToNotify, "Failed to load the TFLite Bert QA model: " + e.getMessage());
        }
    }

    private String getProvisioningErrorMessage(byte errorCode) {
        switch (errorCode) {
            case ProvisioningError.INVALID_ARGUMENT:
                return "Invalid argument provided to provisioning service";
            case ProvisioningError.PERMISSION_DENIED:
                return "Permission denied for provisioning operation";
            case ProvisioningError.SYSTEM_ERROR:
                return "System error occurred during provisioning";
            case ProvisioningError.ENCRYPTEDSTORE_IS_NOT_ENABLED:
                return "Encrypted store is not enabled";
            case ProvisioningError.NETWORK_ERROR:
                return "Network error during provisioning";
            case ProvisioningError.UNKNOWN_ERROR:
                return "Unknown error occurred";
            default:
                return "Undefined provisioning error (" + errorCode + ")";
        }
    }

    private IProvisioningCallback mProvisioningCallback = new IProvisioningCallback.Stub() {

        @Override
        public void onError(byte code) {
            IBertQaInitModelCallback callbackToNotify;
            synchronized (mStateLock) {
                callbackToNotify = mInitModelCallback;
                mInitModelCallback = null;
                mModelState = ModelState.ERROR;
            }

            // This is called outside the synchronized block to prevent deadlock
            notifyInitializationError(callbackToNotify, "Provisioning of TFLite Bert QA model has failed: " +
                    getProvisioningErrorMessage(code) + " (code: " + code + ")");
        }

        @Override
        public void onSuccess(String url, String destination) throws RemoteException {
            Log.i(TAG, "Provisioning of the TFLite Bert QA model has succeeded URL: " + url + " destination: " + destination);
            // Run setupBertQuestionAnswerer in background thread to avoid blocking the provisioning service
            if (mExecutorService != null) {
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        setupBertQuestionAnswerer();
                    }
                });
            }
        }
    };


    private void provisionOrInitModelFromFile() throws RemoteException, IllegalStateException {
        if (mBertQaProvisionedModelPath == null) {
            throw new IllegalStateException("TFLite Bert QA model file path is not initialized. Service may not be properly created.");
        }

        Path path = Paths.get(mBertQaProvisionedModelPath);
        if (!Files.exists(path)) {
            Log.i(TAG, "The TFLite Bert QA model doesn't exist in encryptedstore - start provisioning...");
            // Start provisioning process
            Cca.StartProvisioningStatus status = Cca.startProvisioning(BERT_QA_MODEL_URL,
                BERT_QA_MODEL_SERVER_CA_CERTIFICATE, // ca-cert.pem located in assets folder
                BERT_QA_MODEL, // provisioned model will be saved at /mnt/encryptedstore/demo-model.tflite
                mProvisioningCallback);
            if (status != Cca.StartProvisioningStatus.OK) {
                IBertQaInitModelCallback callbackToNotify;
                synchronized(mStateLock) {
                    callbackToNotify = mInitModelCallback;
                    mInitModelCallback = null;
                    mModelState = ModelState.ERROR;
                }
                notifyInitializationError(callbackToNotify, "Error while starting provisioning process.");
            }
        } else {
            Log.i(TAG, "The TFLite Bert QA model is already provisioned to encryptedstore");
            setupBertQuestionAnswerer();
        }
    }

    private List<QaAnswer> answer(String contextOfQuestion, String question) {
        BertQuestionAnswerer answerer = null;
        synchronized (mStateLock) {
            if (mModelState == ModelState.READY && mBertQuestionAnswerer != null) {
                answerer = mBertQuestionAnswerer;
            }
        }

        if (answerer == null) {
            Log.e(TAG, "mBertQuestionAnswerer is not initialized!");
            return null;
        }

        return answerer.answer(contextOfQuestion, question);
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
        public void initializeModel(int currentDelegate, int numThreads, IBertQaInitModelCallback callback) throws RemoteException {

            Log.i(TAG, "initalizeModel(" + currentDelegate + "," + numThreads + ")");

            // Validate parameters
            if (currentDelegate != DELEGATE_CPU || numThreads < MIN_NUM_THREADS || numThreads > MAX_NUM_THREADS) {
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifyInitializationError(callback, "Invalid arguments passed to initializeModel().");
                    }
                });
                return;
            }

            IBertQaInitModelCallback callbackToReject = null;
            synchronized (mStateLock) {
                // Check if initialization is already in progress
                if (mModelState == ModelState.INITIALIZING) {
                    Log.i(TAG, "The model is in INITIALIZING state");
                    callbackToReject = callback;
                } else {
                    Log.i(TAG, "The model is being initialized");
                    // Set initialization state and callback
                    mModelState = ModelState.INITIALIZING;
                    BertQaService.this.mInitModelCallback = callback;
                    BertQaService.this.mNumThreads = numThreads;
                    BertQaService.this.mBertQuestionAnswerer = null;
                    // Run initialization in a background thread
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.i(TAG, "Starting provisionOrInitModelFromFile()");
                                provisionOrInitModelFromFile();
                            } catch (Exception e) {
                                IBertQaInitModelCallback callbackToNotify;
                                synchronized (mStateLock) {
                                    callbackToNotify = mInitModelCallback;
                                    mInitModelCallback = null;
                                    mModelState = ModelState.ERROR;
                                }

                                notifyInitializationError(callbackToNotify, "Error initializing the TFLite Bert QA model: " + e.getMessage());
                            }
                        }
                    });
                }
            }

            // Invoke callback outside synchronized block to prevent deadlock
            notifyInitializationError(callbackToReject, "Initialization already in progress. Please wait for the current operation to complete.");
        }

        @Override
        public void answer(String context, String question, IBertQaAnswerCallback callback) throws RemoteException {
            Log.i(TAG, "answer(\"" + context + "\", \"" + question +"\")");

            if (context == null || question == null || context.isEmpty() || question.isEmpty()) {
                Log.e(TAG, "Invalid answer() arguments");
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onError();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "Error calling onError IBertQaAnswerCallback callback: " + ex.getMessage(), ex);
                        }
                    }
                });
                return;
            }

            // Run answer processing in a background thread
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<QaAnswer> answers = BertQaService.this.answer(context, question);

                        if (answers == null) {
                             try {
                                 callback.onError();
                             } catch (RemoteException ex) {
                                 Log.e(TAG, "Error calling onError callback: " + ex.getMessage(), ex);
                             }
                             return;
                        }

                        List<QaAnswerData> answerDataList = answers.stream()
                                .map(BertQaService::fromQaAnswer)
                                .collect(Collectors.toList());

                        Log.i(TAG, "answer call succeeded");

                        try {
                            callback.onAnswer(answerDataList);
                        } catch (RemoteException ex) {
                            Log.e(TAG, "Error calling onAnswer callback: " + ex.getMessage(), ex);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing answer call: " + e.getMessage(), e);
                        try {
                            callback.onError();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "Error calling onError callback: " + ex.getMessage(), ex);
                        }
                    }
                }
            });
        }
    };

    @Override
    public void onCreate() throws IllegalStateException {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        mExecutorService = Executors.newFixedThreadPool(MAX_EXECUTOR_SERVICE_THREADS);
        String encryptedStorePath = Cca.getEncryptedStoragePath();
        if (encryptedStorePath != null) {
            mBertQaProvisionedModelPath = encryptedStorePath + "/" + BERT_QA_MODEL;
        } else {
            throw new NullPointerException("Cannot retrieve encrypted store path!");
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();

        // Close the question answerer to release resources
        synchronized (mStateLock) {
            if (mBertQuestionAnswerer != null) {
                mBertQuestionAnswerer.close();
                mBertQuestionAnswerer = null;
            }
            mModelState = ModelState.UNINITIALIZED;
            mInitModelCallback = null;
        }

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            try {
                if (!mExecutorService.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    mExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public BertQaService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return mService;
    }
}
