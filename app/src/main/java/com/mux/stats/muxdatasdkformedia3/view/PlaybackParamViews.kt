package com.mux.stats.muxdatasdkformedia3.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mux.stats.muxdatasdkformedia3.R
import com.mux.stats.muxdatasdkformedia3.databinding.NumericParamEntryBinding
import com.mux.stats.muxdatasdkformedia3.databinding.TextParamEntryBinding


class TextParamEntryView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

  private val binding: TextParamEntryBinding = TextParamEntryBinding.inflate(
    LayoutInflater.from(context),
    this,
    true
  )

  init {
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
  }

  var title: CharSequence? = null
    set(value) {
      binding.textParamEntryLbl.text = value
      field = value
    }
  var hint: CharSequence? = null
    set(value) {
      binding.textParamEntryIn.hint = value
      field = value
    }

  var onClear: (() -> Unit)? = null
  val entry: String? get() {
    val text = binding.textParamEntryIn.text?.trim()?.ifEmpty { null }?.toString()
    return text
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
