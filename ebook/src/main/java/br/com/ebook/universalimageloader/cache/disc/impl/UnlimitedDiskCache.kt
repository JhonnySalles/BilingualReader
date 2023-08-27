/*******************************************************************************
 * Copyright 2011-2014 Sergey Tarasevich
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
package br.com.ebook.universalimageloader.cache.disc.impl

import br.com.ebook.universalimageloader.cache.disc.naming.FileNameGenerator
import java.io.File

/**
 * Default implementation of [disk cache][DiskCache].
 * Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class UnlimitedDiskCache : BaseDiskCache {
    /** @param cacheDir Directory for file caching
     */
    constructor(cacheDir: File?) : super(cacheDir) {}

    /**
     * @param cacheDir        Directory for file caching
     * @param reserveCacheDir null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     */
    constructor(cacheDir: File?, reserveCacheDir: File?) : super(cacheDir, reserveCacheDir) {}

    /**
     * @param cacheDir          Directory for file caching
     * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     * @param fileNameGenerator [                          Name generator][FileNameGenerator] for cached files
     */
    constructor(cacheDir: File?, reserveCacheDir: File?, fileNameGenerator: FileNameGenerator?) : super(
        cacheDir,
        reserveCacheDir,
        fileNameGenerator
    ) {
    }
}