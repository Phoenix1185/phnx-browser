package com.phoenix.phnx.update

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AtomicUpdateStore(private val root: File) {
    private val current = File(root, CURRENT_NAME)
    private val backup = File(root, BACKUP_NAME)
    private val partial = File(root, PARTIAL_NAME)

    @Synchronized
    fun stage(input: InputStream, expectedSha256: String): File? {
        if (!root.exists() && !root.mkdirs()) return null
        partial.delete()
        FileOutputStream(partial).use { output ->
            input.copyTo(output)
            output.fd.sync()
        }
        if (!ChecksumVerifier.verify(partial.inputStream(), expectedSha256)) {
            partial.delete()
            return null
        }

        if (current.exists()) {
            backup.delete()
            if (!current.renameTo(backup)) {
                partial.delete()
                return null
            }
        }
        if (!partial.renameTo(current)) {
            backup.renameTo(current)
            partial.delete()
            return null
        }
        return current
    }

    @Synchronized
    fun currentArtifact(): File? = current.takeIf { it.isFile }

    @Synchronized
    fun rollback(): Boolean {
        if (!backup.isFile) return false
        current.delete()
        return backup.renameTo(current)
    }

    @Synchronized
    fun clear() {
        current.delete()
        backup.delete()
        partial.delete()
    }

    private companion object {
        const val CURRENT_NAME = "phnx-update.apk"
        const val BACKUP_NAME = "phnx-update.previous.apk"
        const val PARTIAL_NAME = "phnx-update.part"
    }
}
