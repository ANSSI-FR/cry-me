/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.core.glide

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.app.core.extensions.singletonEntryPoint
import org.matrix.android.sdk.api.util.MatrixItem

data class AvatarPlaceholder(val matrixItem: MatrixItem)

class AvatarPlaceholderModelLoaderFactory(private val context: Context) : ModelLoaderFactory<AvatarPlaceholder, Drawable> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AvatarPlaceholder, Drawable> {
        return AvatarPlaceholderModelLoader(context)
    }

    override fun teardown() {
        // Is there something to do here?
    }
}

class AvatarPlaceholderModelLoader(private val context: Context) :
    ModelLoader<AvatarPlaceholder, Drawable> {

    override fun buildLoadData(model: AvatarPlaceholder, width: Int, height: Int, options: Options): ModelLoader.LoadData<Drawable>? {
        return ModelLoader.LoadData(ObjectKey(model), AvatarPlaceholderDataFetcher(context, model))
    }

    override fun handles(model: AvatarPlaceholder): Boolean {
        return true
    }
}

class AvatarPlaceholderDataFetcher(context: Context, private val data: AvatarPlaceholder) :
    DataFetcher<Drawable> {

    private val avatarRenderer = context.singletonEntryPoint().avatarRenderer()

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        val avatarPlaceholder = avatarRenderer.getPlaceholderDrawable(data.matrixItem)
        callback.onDataReady(avatarPlaceholder)
    }

    override fun cleanup() {
        // NOOP
    }

    override fun cancel() {
        // NOOP
    }

    override fun getDataClass(): Class<Drawable> {
        return Drawable::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}
