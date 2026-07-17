/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.ebook.universalimageloader.core

import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import br.com.ebook.universalimageloader.core.listener.ImageLoadingListener
import br.com.ebook.universalimageloader.core.listener.ImageLoadingProgressListener
import java.util.concurrent.locks.ReentrantLock

/**
 * Information for load'n'display image task
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see MemoryCacheUtils
 *
 * @see DisplayImageOptions
 *
 * @see ImageLoadingListener
 *
 * @see ImageLoadingProgressListener
 *
 * @since 1.3.1
 */
internal class ImageLoadingInfo(
    val uri: String, val imageAware: ImageAware, val targetSize: ImageSize, val memoryCacheKey: String,
    val options: DisplayImageOptions, val listener: ImageLoadingListener,
    val progressListener: ImageLoadingProgressListener, val loadFromUriLock: ReentrantLock
)