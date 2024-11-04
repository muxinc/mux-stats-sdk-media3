package com.mux.stats.muxdatasdkformedia3.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mux.stats.muxdatasdkformedia3.R
import com.mux.stats.muxdatasdkformedia3.databinding.NumericParamEntryBinding
import com.mux.stats.muxdatasdkformedia3.databinding.ParamSpinnerWithDefaultBinding
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
  val entry: String?
    get() {
      val text = binding.textParamEntryIn.text?.trim()?.ifEmpty { null }?.toString()
      return text
    }
}

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
  val entry: Pair<String?, String?> get() {
    val chosenItem = adapter?.let { adapter -> adapter.items[selection] }
    val customEntry = binding.textParamEntryIn.text?.trim()?.ifEmpty { null }?.toString() ?: ""

    return if (chosenItem?.customAllowed == true) {
      Pair(chosenItem.title.toString(), customEntry)
    } else if (chosenItem != null) {
      Pair(chosenItem.title.toString(), chosenItem.text?.toString())
    } else {
      return Pair(null, null)
    }
  }

  var adapter: SpinnerParamEntryView.Adapter?
    get() = binding.textParamEntrySpinner.adapter as Adapter
    set(value) = binding.textParamEntrySpinner.setAdapter(value)
  var onSelected: ((Int) -> Unit)? = null

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
//      onClear?.invoke()
    }
    binding.textParamEntrySpinner.onItemSelectedListener = object: OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onSelected?.invoke(position)
      }
      override fun onNothingSelected(parent: AdapterView<*>?) {
        selection = defaultIndex
      }

    }

    selection = defaultIndex
  }

  inner class Adapter(
    val context: Context,
    val items: List<Item>,
  ) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Item  = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
      // no view holder is fine, this is a simple layout
      val view = convertView
        ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
      val item = getItem(position)

      if (item.customAllowed) {
        binding.textParamEntryIn.visibility = View.VISIBLE
      } else {
        binding.textParamEntryIn.visibility = View.GONE
      }

      if (item.text != null) {
        view.findViewById<TextView>(android.R.id.text2).text = item.text
      }
      view.findViewById<TextView>(android.R.id.text1).text = item.title

      return view
    }
  }

  data class Item(
    val customAllowed: Boolean,
    val title: CharSequence,
    val text: CharSequence?,
  )
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
