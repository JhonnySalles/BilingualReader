package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoPageType
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "Page", strict = false)
data class ComicInfoPage @JvmOverloads constructor(
    @field: Attribute(name = "Bookmark", required = false) @param:Attribute(name = "Bookmark")
    var bookmark: String? = null,

    @field: Attribute(name = "Image", required = false) @param:Attribute(name = "Image")
    var image: Int? = null,

    @field: Attribute(name = "ImageHeight", required = false) @param:Attribute(name = "ImageHeight")
    var imageHeight: Int? = null,

    @field: Attribute(name = "ImageWidth", required = false) @param:Attribute(name = "ImageWidth")
    var imageWidth: Int? = null,

    @field: Attribute(name = "ImageSize", required = false) @param:Attribute(name = "ImageSize")
    var imageSize: Long? = null,

    @field: Attribute(name = "Type", required = false) @param:Attribute(name = "Type")
    var type: ComicInfoPageType? = null,

    @field: Attribute(name = "DoublePage", required = false) @param:Attribute(name = "DoublePage")
    var doublePage: Boolean? = null,

    @field: Attribute(name = "Key", required = false) @param:Attribute(name = "Key")
    var key: String? = null
)