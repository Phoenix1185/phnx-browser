package com.phoenix.phnx.bookmarks

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import com.phoenix.phnx.system.BrowserNavigationIntent

class BookmarksActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.bookmarks)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.bookmarks)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        val currentUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
            content.addView(Button(this).apply {
                text = getString(R.string.add_current_bookmark)
                setOnClickListener {
                    app.bookmarkManager.add(profileId, currentTitle, currentUrl)
                    refresh()
                }
            })
        }
        content.addView(Button(this).apply {
            text = getString(R.string.new_folder)
            setOnClickListener { showFolderEditor(null, null) }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val folders = app.bookmarkManager.getFolders(profileId)
        folders.forEach { folder ->
            val row = LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(4))
            }
            row.addView(TextView(this).apply {
                text = folder.name
                textSize = 17f
                setTextColor(getColor(R.color.phnx_blue))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = getString(R.string.rename)
                setOnClickListener { showFolderEditor(folder.id, folder.name) }
            })
            row.addView(Button(this).apply {
                text = getString(R.string.delete)
                setOnClickListener { confirmDeleteFolder(folder.id, folder.name) }
            })
            list.addView(row)
        }
        val bookmarks = app.bookmarkManager.getForProfile(profileId)
        if (bookmarks.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_bookmarks)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        bookmarks.forEach { bookmark ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val label = bookmark.title.ifBlank { bookmark.url }
            row.contentDescription = getString(R.string.open_bookmark, label)
            row.setOnClickListener { openSavedUrl(bookmark.url) }
            row.addView(TextView(this).apply {
                val folderName = folders.firstOrNull { it.id == bookmark.folderId }?.name
                text = buildString {
                    append(label)
                    append('\n')
                    append(bookmark.url)
                    if (folderName != null) {
                        append('\n')
                        append(folderName)
                    }
                }
                textSize = 15f
                setTextColor(getColor(R.color.phnx_text))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = getString(R.string.move)
                setOnClickListener { showFolderPicker(bookmark.id, bookmark.folderId, folders) }
            })
            row.addView(Button(this).apply {
                text = getString(R.string.delete)
                setOnClickListener {
                    app.bookmarkManager.delete(profileId, bookmark.id)
                    refresh()
                }
            })
            list.addView(row)
        }
    }

    private fun openSavedUrl(rawUrl: String) {
        val intent = BrowserNavigationIntent.forSavedUrl(this, profileId, rawUrl)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.unable_to_open_saved_page), Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
        }
    }

    private fun showFolderEditor(folderId: String?, currentName: String?) {
        val input = EditText(this).apply {
            setText(currentName.orEmpty())
            hint = getString(R.string.folder_name)
            setSingleLine(true)
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        AlertDialog.Builder(this)
            .setTitle(if (folderId == null) R.string.new_folder else R.string.rename_folder)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(if (folderId == null) R.string.create else R.string.rename) { _, _ ->
                val saved = if (folderId == null) {
                    app.bookmarkManager.createFolder(profileId, input.text.toString()) != null
                } else {
                    app.bookmarkManager.renameFolder(profileId, folderId, input.text.toString())
                }
                if (!saved) Toast.makeText(this, getString(R.string.folder_name_required), Toast.LENGTH_SHORT).show()
                refresh()
            }
            .show()
    }

    private fun confirmDeleteFolder(folderId: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_folder)
            .setMessage(getString(R.string.delete_folder_warning, name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                app.bookmarkManager.deleteFolder(profileId, folderId)
                refresh()
            }
            .show()
    }

    private fun showFolderPicker(bookmarkId: String, currentFolderId: String?, folders: List<BookmarkFolder>) {
        val options = listOf(getString(R.string.unfiled)) + folders.map { it.name }
        val checked = currentFolderId?.let { id -> folders.indexOfFirst { it.id == id } + 1 } ?: 0
        AlertDialog.Builder(this)
            .setTitle(R.string.move_to_folder)
            .setSingleChoiceItems(options.toTypedArray(), checked) { dialog, which ->
                val folderId = folders.getOrNull(which - 1)?.id
                app.bookmarkManager.moveToFolder(profileId, bookmarkId, folderId)
                dialog.dismiss()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
