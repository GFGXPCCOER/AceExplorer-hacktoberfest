/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.main.model.helper

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.siju.acexplorer.R
import com.siju.acexplorer.common.types.FileInfo
import com.siju.acexplorer.extensions.canHandleIntent
import com.siju.acexplorer.logging.Logger
import com.siju.acexplorer.main.model.groups.Category
import com.siju.acexplorer.main.model.groups.CategoryHelper
import com.siju.acexplorer.main.model.groups.CategoryHelper.checkIfFileCategory
import java.util.*


object ShareHelper {

    fun shareFiles(context: Context, fileInfo: ArrayList<FileInfo>, category: Category) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        if (checkIfFileCategory(category)) {
            intent.type = "*/*"
        }
        else {
            val extension = fileInfo[0].extension
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            Logger.log("ShareHelper", "Mime:" + mimeType!!)
            intent.type = mimeType
        }

        val files = ArrayList<Uri>()

        for (info in fileInfo) {
            val uri = UriHelper.createContentUri(context, info.filePath)
            uri?.let { files.add(it) }
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        if (context.canHandleIntent(intent)) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        }
    }

    fun shareMedia(context: Context, category: Category?, uri: Uri?, path : String? = null) {
        Log.d("ShareHelper", "shareMedia:$path, category:$category, uri:$uri")
        if (uri == null && path == null) {
            return
        }
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        if (CategoryHelper.isAnyVideoCategory(category)) {
            intent.type = "video/*"
        }
        else if (category == Category.AUDIO || CategoryHelper.isMusicCategory(category)) {
            intent.type = "audio/*"
        }
        else {
            intent.type = "image/*"
        }
        if (uri == null) {
            val newUri = UriHelper.createContentUri(context, path)
            intent.putExtra(Intent.EXTRA_STREAM, newUri)
            intent.clipData = ClipData.newUri(context.contentResolver, null, uri)
        }
        else {
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.clipData = ClipData.newUri(context.contentResolver, null, uri)
        }
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

        if (context.canHandleIntent(intent)) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        }
    }
}
