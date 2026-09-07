package com.phoenix.phnx.tabs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.phnx.R

class TabOverviewDialog(
    context: Context,
    private val profileId: String,
    private val profileName: String,
    private val previewStore: TabPreviewStore,
    private val onSelect: (TabOverviewItem) -> Unit,
    private val onClose: (TabOverviewItem) -> Unit,
    private val onGroup: (TabOverviewItem) -> Unit,
    private val onNewTab: () -> Unit,
    private val onRecentTabs: () -> Unit,
) : android.app.Dialog(context) {
    private lateinit var countView: TextView
    private lateinit var search: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyTitle: TextView
    private lateinit var emptyAction: TextView
    private lateinit var adapter: TabOverviewAdapter
    private var allItems = emptyList<TabOverviewItem>()
    private var query = ""

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setDimAmount(0.28f)
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(10))
            setBackgroundColor(context.getColor(R.color.phnx_cream))
        }
        root.addView(buildHeader())
        root.addView(buildSearch())

        adapter = TabOverviewAdapter(previewStore, onSelect, onClose, { item, _ -> onGroup(item) })
        recycler = RecyclerView(context).apply {
            clipToPadding = false
            setPadding(context.dp(1), context.dp(8), context.dp(1), context.dp(8))
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@TabOverviewDialog.adapter
            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                val grid = layoutManager as? GridLayoutManager ?: return@addOnLayoutChangeListener
                val columns = (view.width / context.dp(180)).coerceIn(2, 4)
                if (grid.spanCount != columns) grid.spanCount = columns
            }
        }
        root.addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))

        emptyState = buildEmptyState()
        root.addView(emptyState, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                refreshFilteredItems()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    fun setTabs(items: List<TabOverviewItem>) {
        allItems = items.filter { it.profileId == profileId }
        if (::adapter.isInitialized) refreshFilteredItems()
    }

    private fun refreshFilteredItems() {
        val filtered = allItems.filter { it.matches(query) }
        adapter.submitItems(filtered)
        recycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        emptyTitle.text = if (allItems.isEmpty()) context.getString(R.string.no_open_tabs) else context.getString(R.string.no_matching_tabs)
        emptyAction.visibility = if (allItems.isEmpty()) View.VISIBLE else View.GONE
        countView.text = context.getString(R.string.open_tabs_count, allItems.size)
    }

    private fun buildHeader(): View = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        val labels = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val title = TextView(context).apply {
                text = context.getString(R.string.tab_overview_title)
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(context.getColor(R.color.phnx_text))
            }
            addView(title)
            countView = TextView(context).apply {
                textSize = 12f
                setTextColor(context.getColor(R.color.phnx_muted))
                text = context.getString(R.string.open_tabs_profile, profileName)
            }
            addView(countView)
        }
        addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val plus = actionButton("+", context.getString(R.string.new_tab)).apply {
            setOnClickListener { onNewTab(); dismiss() }
        }
        addView(plus)
        val overflow = actionButton("⋮", "Tab overview menu")
        overflow.setOnClickListener {
            PopupMenu(context, overflow).apply {
                menu.add("Recently closed")
                menu.add("Close overview")
                setOnMenuItemClickListener { item ->
                    if (item.title == "Recently closed") onRecentTabs() else dismiss()
                    true
                }
            }.show()
        }
        addView(overflow)
        val done = TextView(context).apply {
            text = context.getString(R.string.close)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.phnx_blue))
            isClickable = true
            isFocusable = true
            setPadding(context.dp(8), 0, context.dp(4), 0)
            contentDescription = context.getString(R.string.close_tab_overview)
            setOnClickListener { dismiss() }
        }
        addView(done, LinearLayout.LayoutParams(context.dp(52), context.dp(48)))
    }

    private fun buildSearch(): View = EditText(context).also { field ->
        search = field
        field.hint = context.getString(R.string.search_tabs)
        field.textSize = 15f
        field.isSingleLine = true
        field.setTextColor(context.getColor(R.color.phnx_text))
        field.setHintTextColor(context.getColor(R.color.phnx_muted))
        field.setPadding(context.dp(16), 0, context.dp(16), 0)
        field.background = GradientDrawable().apply {
            cornerRadius = context.dp(18).toFloat()
            setColor(context.getColor(R.color.phnx_search_surface))
            setStroke(context.dp(1), context.getColor(R.color.phnx_card_stroke))
        }
        field.contentDescription = context.getString(R.string.search_tabs)
        field.layoutParams = LinearLayout.LayoutParams(-1, context.dp(48)).apply {
            topMargin = context.dp(12)
            bottomMargin = context.dp(3)
        }
    }

    private fun buildEmptyState(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val mark = TextView(context).apply {
            text = "PHNX"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(context.getColor(R.color.phnx_blue))
        }
        addView(mark)
        emptyTitle = TextView(context).apply {
            textSize = 18f
            setTextColor(context.getColor(R.color.phnx_text))
            setPadding(0, context.dp(8), 0, context.dp(12))
        }
        addView(emptyTitle)
        emptyAction = actionButton(context.getString(R.string.new_tab), context.getString(R.string.new_tab)).apply {
            setOnClickListener { onNewTab(); dismiss() }
        }
        addView(emptyAction, LinearLayout.LayoutParams(-2, context.dp(48)))
    }

    private fun actionButton(label: String, description: String): TextView = TextView(context).apply {
        text = label
        textSize = if (label == "+") 27f else 14f
        gravity = Gravity.CENTER
        setTextColor(context.getColor(R.color.phnx_blue))
        isClickable = true
        isFocusable = true
        contentDescription = description
        setPadding(context.dp(8), 0, context.dp(8), 0)
        layoutParams = LinearLayout.LayoutParams(context.dp(if (label == "+") 46 else 84), context.dp(48))
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
