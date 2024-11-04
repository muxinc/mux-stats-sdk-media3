package com.mux.stats.muxdatasdkformedia3.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mux.stats.muxdatasdkformedia3.R
import com.mux.stats.muxdatasdkformedia3.databinding.NumericParamEntryBinding
import com.mux.stats.muxdatasdkformedia3.databinding.ParamSpinnerWithDefaultBinding

class SpinnerParamEntryView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

  private val binding = ParamSpinnerWithDefaultBinding.inflate(
    LayoutInflater.from(context),
    this,
    true
  )
  var title: CharSequence? = null
    set(value) {
      binding.spinnerParamEntryLbl.text = value
      field = value
    }
  var hint: CharSequence? = null
    set(value) {
      binding.textParamEntryIn.hint = value
      field = value
    }
  var selection: Int
    get() = binding.textParamEntrySpinner.selectedItemPosition
    set(value) {
      binding.textParamEntrySpinner.setSelection(value)
    }
  val entry: String? get() {
    // todo - get from Spinner adpter
    val text = binding.textParamEntryIn.text?.trim()?.ifEmpty { null }?.toString()
    return text
  }
  var onClear: (() -> Unit)? = null

  init {
    val defaultIndex: Int

    context.theme.obtainStyledAttributes(attrs, R.styleable.SpinnerParamEntryView, 0, 0).apply {
      try {
        defaultIndex = getInt(R.styleable.SpinnerParamEntryView_default_index, 0)
      } finally {
        recycle()
      }
    }
    context.theme.obtainStyledAttributes(attrs, R.styleable.TextParamEntryView, 0, 0).apply {
      try {
        hint = getString(R.styleable.TextParamEntryView_hint)
      } finally {
        recycle()
      }
    }
    context.theme.obtainStyledAttributes(attrs, R.styleable.ParamEntry, 0, 0).apply {
      try {
        title = getString(R.styleable.ParamEntry_title)
      } finally {
        recycle()
      }
    }
    
    binding.textParamEntryClear.setOnClickListener {
      binding.textParamEntryIn.text = null
      onClear?.invoke()
    }
    selection = defaultIndex
  }
}

class NumericParamEntryView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

  private val binding: NumericParamEntryBinding = NumericParamEntryBinding.inflate(
    LayoutInflater.from(context),
    this,
    true
  )

  init {
    context.theme.obtainStyledAttributes(attrs, R.styleable.NumericParamEntryView, 0, 0).apply {
      try {
        hint = getFloat(R.styleable.NumericParamEntryView_hint_num, Float.NaN)
          .toDouble()
          .takeIf { !it.isNaN() }
      } finally {
        recycle()
      }
    }
    context.theme.obtainStyledAttributes(attrs, R.styleable.ParamEntry, 0, 0).apply {
      try {
        title = getString(R.styleable.ParamEntry_title)
      } finally {
        recycle()
      }
    }

    binding.numericParamEntryClear.setOnClickListener {
      binding.numericParamEntryIn.text = null
      onClear?.invoke()
    }
  }

  var title: CharSequence? = null
    set(value) {
      binding.numericParamEntryLbl.text = value
      field = value
    }
  var hint: Double? = null
    set(value) {
      binding.numericParamEntryIn.hint = value?.toString()
      field = value
    }

  var onClear: (() -> Unit)? = null
  val entry: Double?
    get() {
      val text =
        binding.numericParamEntryIn.text?.trim()?.ifEmpty { null }?.toString()?.toDoubleOrNull()
      return text
    }
}
