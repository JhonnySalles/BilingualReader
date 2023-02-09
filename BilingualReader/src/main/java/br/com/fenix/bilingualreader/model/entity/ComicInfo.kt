package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoAgeRating
import br.com.fenix.bilingualreader.model.enums.ComicInfoManga
import br.com.fenix.bilingualreader.model.enums.ComicInfoYesNo
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ComicInfo", strict = false)
class ComicInfo(
	@Element(name = "Title" , required = false ) 
	val title: String?,

	@Element(name = "Series" , required = false )
	val series: String?,

	@Element(name = "Number" , required = false )
	val number: Float?,

	@Element(name = "Volume" , required = false )
	val volume: Int?,

	@Element(name = "Notes" , required = false )
	val notes: String?,

	@Element(name = "Year" , required = false )
	val year: Int?,

	@Element(name = "Month" , required = false )
	val month: Int?,

	@Element(name = "Day" , required = false )
	val day: Int?,

	@Element(name = "Writer" , required = false )
	val writer: String?,

	@Element(name = "Penciller" , required = false )
	val penciller: String?,

	@Element(name = "Inker" , required = false )
	val inker: String?,

	@Element(name = "CoverArtist" , required = false )
	val coverArtist: String?,

	@Element(name = "Colorist" , required = false )
	val colorist: String?,

	@Element(name = "Letterer" , required = false )
	val letterer: String?,

	@Element(name = "Publisher" , required = false )
	val publisher: String?,

	@Element(name = "Tags" , required = false )
	val tags: String?,

	@Element(name = "Web" , required = false )
	val web: String?,

	@Element(name = "Editor" , required = false )
	val editor: String?,

	@Element(name = "Translator" , required = false )
	val translator: String?,

	@Element(name = "PageCount" , required = false )
	val pageCount: Int?,

	@ElementList(name = "Pages" , required = false )
	val pages: List<ComicInfoPage>?,

	@Element(name = "Count" , required = false )
	val count: Int?,

	@Element(name = "AlternateSeries" , required = false )
	val alternateSeries: String?,

	@Element(name = "AlternateNumber" , required = false )
	val alternateNumber: Float?,

	@Element(name = "StoryArc" , required = false )
	val storyArc: String?,

	@Element(name = "StoryArcNumber" , required = false )
	val storyArcNumber: String?,

	@Element(name = "SeriesGroup" , required = false )
	val seriesGroup: String?,

	@Element(name = "AlternateCount" , required = false )
	val alternateCount: Int?,

	@Element(name = "Summary" , required = false )
	val summary: String?,

	@Element(name = "Imprint" , required = false )
	val imprint: String?,

	@Element(name = "Genre" , required = false )
	val genre: String?,

	@Element(name = "LanguageISO" , required = false )
	val languageISO: String?,

	@Element(name = "Format" , required = false )
	val format: String?,

	@Element(name = "AgeRating" , required = false )
	val ageRating: ComicInfoAgeRating?,

	@Element(name = "CommunityRating" , required = false )
	val communityRating: Float?,

	@Element(name = "BlackAndWhite" , required = false )
	val blackAndWhite: ComicInfoYesNo?,

	@Element(name = "Manga" , required = false )
	val manga: ComicInfoManga?,

	@Element(name = "Characters" , required = false )
	val characters: String?,

	@Element(name = "Teams" , required = false )
	val teams: String?,

	@Element(name = "Locations" , required = false )
	val locations: String?,

	@Element(name = "ScanInformation" , required = false )
	val scanInformation: String?,

	@Element(name = "MainCharacterOrTeam" , required = false )
	val mainCharacterOrTeam: String?,

	@Element(name = "Review" , required = false )
	val review: String?,
)