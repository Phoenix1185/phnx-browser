package com.phoenix.phnx.tabs

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.phoenix.phnx.R

class TabOverviewAdapter(
    private val previewStore: TabPreviewStore,
    private val onSelect: (TabOverviewItem) -> Unit,
    private val onClose: (TabOverviewItem) -> Unit,
    private val onGroup: (TabOverviewItem, View) -> Unit,
) : RecyclerView.Adapter<TabOverviewAdapter.TabViewHolder>() {
    private var items = emptyList<TabOverviewItem>()

    fun submitItems(updated: List<TabOverviewItem>) {
        val previous = items
        items = updated
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = updated.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                previous[oldItemPosition].id == updated[newItemPosition].id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                previous[oldItemPosition] == updated[newItemPosition]
        }).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder =
        TabViewHolder(parent, previewStore, onSelect, onClose, onGroup)

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class TabViewHolder(
        parent: ViewGroup,
        private val previewStore: TabPreviewStore,
        private val onSelect: (TabOverviewItem) -> Unit,
        private val onClose: (TabOverviewItem) -> Unit,
        private val onGroup: (TabOverviewItem, View) -> Unit,
    ) : RecyclerView.ViewHolder(createCard(parent.context)) {
        private val card = itemView as FrameLayout
        private val preview = card.findViewById<ImageView>(R.id.tab_preview_image)
        private val previewPending = card.findViewById<TextView>(R.id.tab_preview_pending)
        private val favicon = card.findViewById<ImageView>(R.id.tab_favicon)
        private val title = card.findViewById<TextView>(R.id.tab_preview_title)
        private val domain = card.findViewById<TextView>(R.id.tab_preview_domain)
        private val status = card.findViewById<TextView>(R.id.tab_preview_status)
        private val group = card.findViewById<TextView>(R.id.tab_preview_group)
        private val close = card.findViewById<TextView>(R.id.tab_preview_close)

        fun bind(item: TabOverviewItem) {
            card.tag = item.id
            preview.tag = item.id
            favicon.tag = item.id
            title.text = item.title
            domain.text = item.domain.ifBlank { "New tab" }
            status.text = when {
                item.isPrivate -> "PRIVATE"
                item.isActive -> "ACTIVE"
                else -> ""
            }
            status.visibility = if (status.text.isNullOrBlank()) View.GONE else View.VISIBLE
            group.text = item.groupTitle ?: "Group"
            group.contentDescription = if (item.groupTitle == null) "Add tab to a group" else "Tab group: ${item.groupTitle}"
            group.setOnClickListener { onGroup(item, it) }
            close.contentDescription = "Close tab: ${item.title}"
            close.setOnClickListener { onClose(item) }
            card.contentDescription = buildString {
                append(item.title)
                item.domain.takeIf { it.isNotBlank() }?.let { append(", $it") }
                if (item.isPrivate) append(", private tab")
                if (item.isActive) append(", active tab")
            }
            card.setOnClickListener { onSelect(item) }
            applyCardStyle(item)

            preview.setImageDrawable(null)
            previewPending.visibility = View.VISIBLE
            previewStore.loadPreview(item) { bitmap ->
                if (preview.tag != item.id) return@loadPreview
                if (bitmap != null) {
                    preview.setImageBitmap(bitmap)
                    previewPending.visibility = View.GONE
                }
            }
            favicon.setImageDrawable(null)
            previewStore.loadFavicon(item) { bitmap ->
                if (favicon.tag == item.id && bitmap != null) favicon.setImageBitmap(bitmap)
            }
        }

        private fun applyCardStyle(item: TabOverviewItem) {
            val stroke = if (item.isActive) card.context.getColor(R.color.phnx_blue) else card.context.getColor(R.color.phnx_card_stroke)
            card.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = card.context.dp(20).toFloat()
                setColor(card.context.getColor(R.color.phnx_card_surface))
                setStroke(card.context.dp(if (item.isActive) 2 else 1), stroke)
            }
            card.elevation = card.context.dp(if (item.isActive) 5 else 2).toFloat()
        }

        companion object {
            private fun createCard(context: android.content.Context): FrameLayout {
                val card = FrameLayout(context).apply {
                    id = R.id.tab_preview_card
                    isClickable = true
                    isFocusable = true
                    setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(8))
                    layoutParams = RecyclerView.LayoutParams(-1, context.dp(244)).apply {
                        setMargins(context.dp(5), context.dp(5), context.dp(5), context.dp(5))
                    }
                }
                val preview = ImageView(context).apply {
                    id = R.id.tab_preview_image
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = GradientDrawable().apply {
                        cornerRadius = context.dp(15).toFloat()
                        setColor(context.getColor(R.color.phnx_preview_surface))
                    }
                }
                card.addView(preview, FrameLayout.LayoutParams(-1, context.dp(142)))
                val pending = TextView(context).apply {
                    id = R.id.tab_preview_pending
                    text = context.getString(R.string.tab_preview_pending)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(context.getColor(R.color.phnx_muted))
                }
                card.addView(pending, FrameLayout.LayoutParams(-1, context.dp(142)))
                val close = TextView(context).apply {
                    id = R.id.tab_preview_close
                    text = "×"
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x990F172A.toInt())
                    }
                    isClickable = true
                    isFocusable = true
                    setPadding(0, 0, 0, context.dp(2))
                }
                card.addView(close, FrameLayout.LayoutParams(context.dp(38), context.dp(38), Gravity.TOP or Gravity.END))

                val content = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(context.dp(5), context.dp(8), context.dp(5), 0)
                }
                val titleRow = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
                val favicon = ImageView(context).apply {
                    id = R.id.tab_favicon
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(context.getColor(R.color.phnx_navy))
                    }
                }
                titleRow.addView(favicon, LinearLayout.LayoutParams(context.dp(25), context.dp(25)))
                val title = TextView(context).apply {
                    id = R.id.tab_preview_title
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(context.getColor(R.color.phnx_text))
                    setPadding(context.dp(8), 0, context.dp(4), 0)
                }
                titleRow.addView(title, LinearLayout.LayoutParams(0, context.dp(25), 1f))
                val status = TextView(context).apply {
                    id = R.id.tab_preview_status
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(context.getColor(R.color.phnx_blue))
                }
                titleRow.addView(status)
                content.addView(titleRow)
                val domain = TextView(context).apply {
                    id = R.id.tab_preview_domain
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(context.getColor(R.color.phnx_muted))
                    setPadding(context.dp(33), context.dp(2), 0, 0)
                }
                content.addView(domain)
                val group = TextView(context).apply {
                    id = R.id.tab_preview_group
                    textSize = 11f
                    setTextColor(context.getColor(R.color.phnx_blue))
                    setPadding(context.dp(33), context.dp(5), 0, 0)
                    isClickable = true
                    isFocusable = true
                }
                content.addView(group)
                card.addView(content, FrameLayout.LayoutParams(-1, context.dp(96), Gravity.TOP).apply {
                    topMargin = context.dp(142)
                })
                return card
            }
        }
    }
}

private fun android.content.Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()
