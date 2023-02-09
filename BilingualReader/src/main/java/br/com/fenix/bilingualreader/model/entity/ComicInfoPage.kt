package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoPageType
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root

@Root(name = "Page", strict = false)
class ComicInfoPage(
    @field: Attribute(name = "Bookmark", required = false)
    var bookmark: String?,

    @field: Attribute(name = "Image", required = false)
    var image: Int?,

    @field: Attribute(name = "ImageHeight", required = false)
    var imageHeight: Int?,

    @field: Attribute(name = "ImageWidth", required = false)
    var imageWidth: Int?,

    @field: Attribute(name = "ImageSize", required = false)
    var imageSize: Long?,

    @field: Attribute(name = "Type", required = false)
    var type: ComicInfoPageType?,

    @field: Attribute(name = "DoublePage", required = false)
    var doublePage: Boolean?,

    @field: Attribute(name = "Key", required = false)
    var key: String?
)