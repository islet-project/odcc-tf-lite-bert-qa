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

// Samsung's changes: Put the TFLite model into a separate bound service, handle long running operations

package org.tensorflow.lite.examples.bertqa

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.tensorflow.lite.task.text.qa.QaAnswer

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData
import org.tensorflow.lite.examples.bertqaservice.IBertQaInterface
import org.tensorflow.lite.examples.bertqaservice.IBertQaAnswerCallback
import org.tensorflow.lite.examples.bertqaservice.IBertQaInitModelCallback

// Data class to hold both context and question together
private data class PendingQuestionData(val context: String, val question: String)

class BertQaHelper private constructor(context: Context) {
    val context: Context = context.applicationContext
    var numThreads: Int = 2
    var currentDelegate: Int = 0
    var answererListener: AnswererListener? = null
        private set
    private var bertQaInterface : IBertQaInterface? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val shouldRetryInitialization = AtomicBoolean(false)
    private var retryHandler: Handler? = null
    private val needsReinitialization = AtomicBoolean(false)
    private val initializationStarted = AtomicBoolean(false);
    private val modelInitialized = AtomicBoolean(false)
    private val pendingQuestionData = AtomicReference<PendingQuestionData?>(null)
    private var lastInitializedNumThreads: Int = -1
    private var lastInitializedDelegate: Int = -1

    // Initialize the singleton (it initiate the process of model setup)
    @Synchronized
    fun tryInitialize(): Boolean {
        if (modelInitialized.get()) {
            return true
        }

        if (initializationStarted.get()) {
            return false;
        }

        initializationStarted.set(true);

        setupBertQuestionAnswerer()

        return false
    }

    fun setAnswererListener(listener: AnswererListener?) {
        this.answererListener = listener
    }

    // Atomic operation to set pending question
    private fun setPendingQuestion(context: String, question: String) {
        pendingQuestionData.set(PendingQuestionData(context, question))
    }

    // Atomic operation to get and clear pending question
    private fun getAndClearPendingQuestion(): PendingQuestionData? {
        return pendingQuestionData.getAndSet(null)
    }

    private val isServiceBound = AtomicBoolean(false)

    fun clearBertQuestionAnswerer() {
        if (isServiceBound.get()) {
            try {
                context.unbindService(mBertQaServiceConnection)
                isServiceBound.set(false)
            } catch (ex: IllegalArgumentException) {
                Log.e(TAG, "Error unbinding service", ex)
            } catch (ex: IllegalStateException) {
                Log.e(TAG, "Error unbinding service", ex)
            }
        }
        // Stop model initialization retry mechanism to prevent resource leaks
        stopModelInitializationRetry()
        // Remove any pending messages from the main handler
        mainHandler.removeCallbacksAndMessages(null)
        modelInitialized.set(false)
        bertQaInterface = null
    }

    val initializeModelCallback = object : IBertQaInitModelCallback.Stub() {
        override fun onError(errorMessage: String?) {
            initializationStarted.set(false);
            Log.e(TAG, "Initialization of model has failed: $errorMessage")
            mainHandler.post {
                answererListener?.onError(errorMessage ?: "Unknown initialization error")
                // Clear pending questions using the new atomic method
                getAndClearPendingQuestion()
            }
        }

        override fun onSuccess() {
            Log.d(TAG, "Initialization of model was successful")
            modelInitialized.set(true)
            needsReinitialization.set(false)
            initializationStarted.set(false);

            // Store the parameters that were successfully used for initialization
            lastInitializedNumThreads = numThreads
            lastInitializedDelegate = currentDelegate

            // Notify listener about successful initialization
            mainHandler.post {
                answererListener?.onInitializationSuccess()
                // If there's a pending answer request, execute it after reinitialization
                // using the new atomic method
                val pendingData = getAndClearPendingQuestion()
                if (pendingData != null) {
                    // Notify that inference is about to start after reinitialization
                    answererListener?.onInferenceStarting()
                    answer(pendingData.context, pendingData.question)
                }
            }
        }
    }

    private fun tryInitializeModel(): Boolean {
        if (!modelInitialized.get()) {
            try {
                bertQaInterface?.initializeModel(currentDelegate, numThreads, initializeModelCallback)
        } catch (ex: IllegalStateException) {
            // RemoteException thrown across AIDL is wrapped in RuntimeException
            Log.e(TAG, "Exception while initializing model: ${ex.message}", ex)
            return false
        } catch (ex: Exception) {
            Log.e(TAG, "Unexpected exception while initializing model", ex)
            return false
        }
        }

        return true
    }

    private fun startModelInitializationRetry() {
        shouldRetryInitialization.set(true)
        retryHandler = Handler(Looper.getMainLooper())

        val retryRunnable = object : Runnable {
            override fun run() {
                if (!shouldRetryInitialization.get()) {
                    Log.d(TAG, "Stopping model initialization retry")
                    return
                }

                Log.d(TAG, "Attempting to initialize model...")
                if (tryInitializeModel()) {
                    Log.d(TAG, "Model initialization successful, stopping retry")
                    shouldRetryInitialization.set(false)
                } else {
                    Log.d(TAG, "Model initialization failed, will retry in 5 seconds")
                    // Schedule next retry in 5 seconds
                    retryHandler?.postDelayed(this, 5000)
                }
            }
        }

        // Start the first retry attempt immediately
        retryHandler?.post(retryRunnable)
    }

    private fun stopModelInitializationRetry() {
        shouldRetryInitialization.set(false)
        retryHandler?.removeCallbacksAndMessages(null)
        retryHandler = null
    }

    private val mBertQaServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected()")
            if (service == null) {
                Log.e(TAG, "Service binder is null")
                stopModelInitializationRetry()
                mainHandler.post {
                    answererListener?.onError("Service binder is null")
                }
                return
            }
            bertQaInterface = IBertQaInterface.Stub.asInterface(service)
            isServiceBound.set(true)

            // Start periodic retry thread for model initialization
            startModelInitializationRetry()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected()")
            bertQaInterface = null
            isServiceBound.set(false)

            // Stop the retry thread when service is disconnected
            stopModelInitializationRetry()
        }

    }

    private fun setupBertQuestionAnswerer() {
        var intent = Intent()
        intent.setComponent(ComponentName(
                "org.tensorflow.lite.examples.bertqaservice",
                "org.tensorflow.lite.examples.bertqaservice.BertQaService"))
        try {
            if (context.bindService(intent, mBertQaServiceConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "bindService() for bertqaservice returned true")
            } else {
                Log.e(TAG, "bindService() for bertqaservice returned false; probably it is not able to find the service")
                mainHandler.post {
                    answererListener?.onError("Cannot connect to bertqaservice")
                }
            }
        } catch (ex: SecurityException) {
            Log.e(TAG, "SecurityException while binding to bertqaservice", ex)
            context.unbindService(mBertQaServiceConnection)
            mainHandler.post {
                answererListener?.onError("SecurityException: ${ex.message}")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception while binding to bertqaservice", ex)
            mainHandler.post {
                answererListener?.onError("Exception: ${ex.message}")
            }
        }
    }

    private var inferenceStartTime: Long = 0

    val answerCallback = object : IBertQaAnswerCallback.Stub() {
        override fun onError() {
            Log.e(TAG, "Service error")
            mainHandler.post {
                answererListener?.onError("Service returned an error")
            }
        }

        override fun onAnswer(answers: List<QaAnswerData>?) {
            Log.d(TAG, "Service success with ${answers?.size ?: 0} answers")
            val inferenceTime = SystemClock.uptimeMillis() - inferenceStartTime
            val qaAnswers = answers?.map { toQaAnswer(it) }
            mainHandler.post {
                answererListener?.onResults(qaAnswers, inferenceTime)
            }
        }
    }

    fun answer(contextOfQuestion: String, question: String) {
        // Check if model needs reinitialization
        if (needsReinitialization.get()) {
            Log.d(TAG, "Model needs reinitialization, reinitializing...")
            setPendingQuestion(contextOfQuestion, question)

            // Notify listener that reinitialization is needed
            mainHandler.post {
                answererListener?.onReinitializationNeeded()
            }

            initializationStarted.set(true);
            // Reinitialize the model with current parameters
            tryInitializeModel()
            return
        }

        if (!modelInitialized.get()) {
            Log.e(TAG, "Cannot answer - model is not initialized")
            answererListener?.onError("Model is not initialized")
            return
        }

        // Check if bertQaInterface is null before calling answer
        if (bertQaInterface == null) {
            Log.e(TAG, "Cannot answer - bertQaInterface is null")
            answererListener?.onError("Service is not available")
            return
        }

        try {
            // Notify listener that inference is starting
            answererListener?.onInferenceStarting()

            // Record the start time for inference
            inferenceStartTime = SystemClock.uptimeMillis()

            // Call the service asynchronously (service handles threading)
            bertQaInterface?.answer(contextOfQuestion, question, answerCallback)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "IllegalStateException has been thrown", ex)
            answererListener?.onError("IllegalStateException has been thrown " + ex.toString())
        } catch (ex: Exception) {
            Log.e(TAG, "Exception has been thrown", ex)
            answererListener?.onError("Exception has been thrown " + ex.toString())
        }
    }

    fun checkAndMarkForReinitialization(): Boolean {
        // Check if parameters have changed from the last initialization
        if (numThreads != lastInitializedNumThreads || currentDelegate != lastInitializedDelegate) {
            needsReinitialization.set(true)
            modelInitialized.set(false)
            Log.d(TAG, "Parameters changed - marking for reinitialization. " +
                    "Threads: $lastInitializedNumThreads -> $numThreads, " +
                    "Delegate: $lastInitializedDelegate -> $currentDelegate")
            return true
        }
        return false
    }

    interface AnswererListener {
        fun onError(error: String)
        fun onResults(
            results: List<QaAnswer>?,
            inferenceTime: Long
        )
        fun onInitializationSuccess()
        fun onReinitializationNeeded()
        fun onInferenceStarting()
    }

    companion object {
        private fun toQaAnswer(data: QaAnswerData) : QaAnswer {
            return QaAnswer(data.text, data.start, data.end, data.logit)
        }

        private const val TAG = "BertQaHelper"

        @Volatile
        private var INSTANCE: BertQaHelper? = null

        private var applicationContextHolder: Context? = null

        fun setContext(context: Context) {
            applicationContextHolder = context.applicationContext
        }

        fun getInstance(): BertQaHelper {
            return INSTANCE ?: synchronized(this) {
                val context = applicationContextHolder ?: throw IllegalStateException("BertQaHelper not initialized. Call initialize() first.")
                INSTANCE ?: BertQaHelper(context).also {
                    INSTANCE = it
                }
            }
        }

        fun destroyInstance() {
            INSTANCE?.clearBertQuestionAnswerer()
            INSTANCE?.setAnswererListener(null)
            INSTANCE = null
        }
    }
}
