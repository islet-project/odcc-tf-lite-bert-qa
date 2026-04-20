/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
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
package org.tensorflow.lite.examples.bertqa

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.task.text.qa.QaAnswer

import org.tensorflow.lite.examples.bertqaservice.QaAnswerData
import org.tensorflow.lite.examples.bertqaservice.IBertQaInterface

class BertQaHelper(
    val context: Context,
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    val answererListener: AnswererListener?
) {

    private var bertQaInterface : IBertQaInterface? = null

    fun clearBertQuestionAnswerer() {
        context.unbindService(mBartQaServiceConnection)
    }

    private val mBartQaServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected()");
            bertQaInterface = IBertQaInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected()");
            bertQaInterface = null
        }

    }

    init {
        setupBertQuestionAnswerer()
    }

    private fun setupBertQuestionAnswerer() {
        var intent = Intent()
        intent.setComponent(ComponentName(
                "org.tensorflow.lite.examples.bertqaservice",
                "org.tensorflow.lite.examples.bertqaservice.BertQaService"));
        if (context.bindService(intent, mBartQaServiceConnection, Context.BIND_AUTO_CREATE) == true) {
            Log.d(TAG, "bindService() returned true");
        }  else {
            Log.e(TAG, "bindService() returned false; probably it is not able to find the service");
        }
    }

    fun answer(contextOfQuestion: String, question: String) {
        if (bertQaInterface == null) {
            setupBertQuestionAnswerer()
        }

        try {
            // Inference time is the difference between the system time at the start and finish of the
            // process
            var inferenceTime = SystemClock.uptimeMillis()

            val serviceAnswers = bertQaInterface?.answer(contextOfQuestion, question)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            val answers = serviceAnswers?.map { toQaAnswer(it) }
            answererListener?.onResults(answers, inferenceTime)
        } catch (ex: RemoteException) {
            Log.e(TAG, "RemoteException has been thrown", ex);
            answererListener?.onError("BertQaService thrown an exception " + ex.toString())
        }
    }

    interface AnswererListener {
        fun onError(error: String)
        fun onResults(
            results: List<QaAnswer>?,
            inferenceTime: Long
        )
    }

    companion object {
        private fun toQaAnswer(data: QaAnswerData) : QaAnswer {
            return QaAnswer(data.text, data.start, data.end, data.logit)
        }
        private const val TAG = "BertQaHelper"
    }
}
