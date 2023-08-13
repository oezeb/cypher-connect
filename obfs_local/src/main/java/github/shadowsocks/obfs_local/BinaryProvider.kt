package com.github.shadowsocks.obfs_local

import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import com.github.shadowsocks.plugin.NativePluginProvider
import com.github.shadowsocks.plugin.PathProvider


class BinaryProvider : NativePluginProvider() {
    override fun populateFiles(provider: PathProvider) {
        provider.addPath("obfs-local", 755)
    }

    override fun getExecutable(): String {
        val nativeLibraryDir = context?.applicationInfo?.nativeLibraryDir
        return "$nativeLibraryDir/libobfs-local.so"
    }

    override fun openFile(uri: Uri): ParcelFileDescriptor {
        return when (uri.path) {
            "/obfs-local" -> ParcelFileDescriptor
                .open(File(getExecutable()), ParcelFileDescriptor.MODE_READ_ONLY)
            else -> throw FileNotFoundException()
        }
    }
}
