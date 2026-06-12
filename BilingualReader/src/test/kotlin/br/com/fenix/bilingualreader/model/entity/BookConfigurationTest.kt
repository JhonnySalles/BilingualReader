package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.PaginationType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookConfigurationTest {

    @Test
    fun `test book configuration construction`() {
        val config = BookConfiguration(
            id = 1L,
            idBook = 10L,
            alignment = AlignmentLayoutType.Center,
            margin = MarginLayoutType.Medium,
            spacing = SpacingLayoutType.Big,
            fontType = FontType.BabelStoneHan,
            fontSize = 18.0f,
            scrolling = ScrollingType.Horizontal,
            pagination = PaginationType.Default
        )

        assertEquals(1L, config.id)
        assertEquals(10L, config.idBook)
        assertEquals(AlignmentLayoutType.Center, config.alignment)
        assertEquals(MarginLayoutType.Medium, config.margin)
        assertEquals(SpacingLayoutType.Big, config.spacing)
        assertEquals(FontType.BabelStoneHan, config.fontType)
        assertEquals(18.0f, config.fontSize)
        assertEquals(ScrollingType.Horizontal, config.scrolling)
        assertEquals(PaginationType.Default, config.pagination)
    }
}
