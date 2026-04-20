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

// Samsung's changes: Handle long running operations

package org.tensorflow.lite.examples.bertqa.fragments

import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.bertqa.BertQaHelper
import org.tensorflow.lite.examples.bertqa.R
import org.tensorflow.lite.examples.bertqa.databinding.FragmentQaBinding
import org.tensorflow.lite.examples.bertqa.dataset.LoadDataSetClient
import org.tensorflow.lite.task.text.qa.QaAnswer

class QaFragment : Fragment(), BertQaHelper.AnswererListener {
    private var _fragmentQaBinding: FragmentQaBinding? = null
    private val fragmentQaBinding get() = _fragmentQaBinding!!

    private lateinit var bertQaHelper: BertQaHelper
    private val args: QaFragmentArgs by navArgs()
    private var content: String = ""
    private var questions: List<String> = emptyList()
    private lateinit var progressContainer: FrameLayout
    private lateinit var progressText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentQaBinding = FragmentQaBinding.inflate(inflater, container, false)

        progressContainer = fragmentQaBinding.root.findViewById(R.id.progressContainer)
        progressText = fragmentQaBinding.root.findViewById(R.id.progressText)

        return fragmentQaBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bertQaHelper = BertQaHelper.getInstance()
        bertQaHelper.setAnswererListener(this)

        if (!bertQaHelper.tryInitialize()) {
            // Show progress while initializing BertQa model
            showProgress("Initializing BertQa model...")
        }

        val client = LoadDataSetClient(requireActivity())
        client.loadJson()?.let {
            content = it.getContents()[args.datasetPosition]
            questions = it.questions[args.datasetPosition]
        }

        fragmentQaBinding.tvDatasetContent.text = content
        initRecyclerView()
        handleListener()
    }

    private fun initRecyclerView() {
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        with(fragmentQaBinding.recyclerView) {
            adapter = QaAdapter(questions) {
                setQuestion(it)
            }
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            addItemDecoration(decoration)
        }
    }

    private fun handleListener() {
        fragmentQaBinding.edtQuestion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // no op
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // Only allow clicking Ask button if there is a question.
                val shouldAskButtonActive: Boolean = charSequence.toString().isNotEmpty()
                fragmentQaBinding.imgBtnAsk.isClickable = shouldAskButtonActive
                fragmentQaBinding.imgBtnAsk.setImageResource(
                    if (shouldAskButtonActive) R.drawable.ic_ask_active else R.drawable.ic_ask_inactive
                )
            }

            override fun afterTextChanged(editable: Editable?) {
                // no op
            }

        })

        // When clicked, add the question in suggestion question list to query edittext.
        fragmentQaBinding.imgBtnAsk.setOnClickListener {
            answerQuestion(fragmentQaBinding.edtQuestion.text.toString())
        }

        // When clicked, decrease the number of threads used for answer
        fragmentQaBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (bertQaHelper.numThreads > 1) {
                bertQaHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for answer
        fragmentQaBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (bertQaHelper.numThreads < 4) {
                bertQaHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentQaBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentQaBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    bertQaHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Mark for reinitialization if parameters changed.
    private fun updateControlsUi() {
        fragmentQaBinding.bottomSheetLayout.threadsValue.text = bertQaHelper.numThreads.toString()
        // Only mark for reinitialization if parameters have actually changed from the last initialization
        bertQaHelper.checkAndMarkForReinitialization()
    }

    private fun setQuestion(position: Int) {
        fragmentQaBinding.edtQuestion.setText(questions[position])
    }

    private fun showProgress(message: String) {
        progressText.text = message
        progressContainer.visibility = View.VISIBLE
        // Disable interaction with the bottom view and tvDatasetContent
        fragmentQaBinding.bottomView.isClickable = false
        fragmentQaBinding.bottomView.alpha = 0.5f
        fragmentQaBinding.tvDatasetContent.isClickable = false
        fragmentQaBinding.tvDatasetContent.alpha = 0.5f
        // Disable and gray out the bottom sheet layout
        fragmentQaBinding.bottomSheetLayout.bottomSheetLayout.isClickable = false
        fragmentQaBinding.bottomSheetLayout.bottomSheetLayout.alpha = 0.5f
    }

    private fun hideProgress() {
        progressContainer.visibility = View.GONE
        // Enable interaction with the bottom view and tvDatasetContent
        fragmentQaBinding.bottomView.isClickable = true
        fragmentQaBinding.bottomView.alpha = 1.0f
        fragmentQaBinding.tvDatasetContent.isClickable = true
        fragmentQaBinding.tvDatasetContent.alpha = 1.0f
        // Enable and restore bottom sheet layout
        fragmentQaBinding.bottomSheetLayout.bottomSheetLayout.isClickable = true
        fragmentQaBinding.bottomSheetLayout.bottomSheetLayout.alpha = 1.0f
    }

    private fun answerQuestion(question: String) {
        bertQaHelper.answer(content, question)
    }

    // Highlight the answer
    private fun highlightAnswer(answer: String) {
        val start = content.indexOf(answer)
        val end = start + answer.length

        val str = SpannableString(content)
        str.setSpan(
            BackgroundColorSpan(requireActivity().getColor(R.color.highlight_background_color)),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        str.setSpan(
            ForegroundColorSpan(requireActivity().getColor(R.color.highlight_text_color)),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        fragmentQaBinding.tvDatasetContent.text = str
    }

    override fun onError(error: String) {
        hideProgress()
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(results: List<QaAnswer>?, inferenceTime: Long) {
        hideProgress()
        results?.first()?.let {
            highlightAnswer(it.text)
        }

        fragmentQaBinding.tvInferenceTime.text = String.format(
            requireActivity().getString(R.string.bottom_view_inference_time),
            inferenceTime
        )
    }

    override fun onInitializationSuccess() {
        // Hide progress when model initialization is successful
        hideProgress()
    }

    override fun onReinitializationNeeded() {
        // Show progress when model needs reinitialization
        showProgress("Reinitializing BertQa model...")
    }

    override fun onInferenceStarting() {
        // Show progress when inference is starting
        showProgress(getString(R.string.performing_inference))
    }

    override fun onDestroyView() {
        fragmentQaBinding.edtQuestion.addTextChangedListener(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
