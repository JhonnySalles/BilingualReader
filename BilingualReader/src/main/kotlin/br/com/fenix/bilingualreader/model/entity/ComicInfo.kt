package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoAgeRating
import br.com.fenix.bilingualreader.model.enums.ComicInfoManga
import br.com.fenix.bilingualreader.model.enums.ComicInfoYesNo
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root


@Root(name = "ComicInfo", strict = false)
data class ComicInfo @JvmOverloads constructor(
	@field:Element(name = "Title" , required = false ) @param:Element(name = "Title")
	var title: String? = null,

	@field:Element(name = "Series" , required = false ) @param:Element(name = "Series")
	var series: String? = null,

	@field:Element(name = "Number" , required = false ) @param:Element(name = "Number")
	var number: Float? = null,

	@field:Element(name = "Volume" , required = false ) @param:Element(name = "Volume")
	var volume: Int? = null,

	@field:Element(name = "Notes" , required = false ) @param:Element(name = "Notes")
	var notes: String? = null,

	@field:Element(name = "Year" , required = false ) @param:Element(name = "Year")
	var year: Int? = null,

	@field:Element(name = "Month" , required = false ) @param:Element(name = "Month")
	var month: Int? = null,

	@field:Element(name = "Day" , required = false ) @param:Element(name = "Day")
	var day: Int? = null,

	@field:Element(name = "Writer" , required = false ) @param:Element(name = "Writer")
	var writer: String? = null,

	@field:Element(name = "Penciller" , required = false ) @param:Element(name = "Penciller")
	var penciller: String? = null,

	@field:Element(name = "Inker" , required = false ) @param:Element(name = "Inker")
	var inker: String? = null,

	@field:Element(name = "CoverArtist" , required = false ) @param:Element(name = "CoverArtist")
	var coverArtist: String? = null,

	@field:Element(name = "Colorist" , required = false ) @param:Element(name = "Colorist")
	var colorist: String? = null,

	@field:Element(name = "Letterer" , required = false ) @param:Element(name = "Letterer")
	var letterer: String? = null,

	@field:Element(name = "Publisher" , required = false ) @param:Element(name = "Publisher")
	var publisher: String? = null,

	@field:Element(name = "Tags" , required = false ) @param:Element(name = "Tags")
	var tags: String? = null,

	@field:Element(name = "Web" , required = false ) @param:Element(name = "Web")
	var web: String? = null,

	@field:Element(name = "Editor" , required = false ) @param:Element(name = "Editor")
	var editor: String? = null,

	@field:Element(name = "Translator" , required = false ) @param:Element(name = "Translator")
	var translator: String? = null,

	@field:Element(name = "PageCount" , required = false ) @param:Element(name = "PageCount")
	var pageCount: Int? = null,

	@field:ElementList(name = "Pages" , required = false ) @param:ElementList(name = "Pages")
	var pages: List<ComicInfoPage>? = null,

	@field:Element(name = "Count" , required = false ) @param:Element(name = "Count")
	var count: Int? = null,

	@field:Element(name = "AlternateSeries" , required = false ) @param:Element(name = "AlternateSeries")
	var alternateSeries: String? = null,

	@field:Element(name = "AlternateNumber" , required = false ) @param:Element(name = "AlternateNumber")
	var alternateNumber: Float? = null,

	@field:Element(name = "StoryArc" , required = false ) @param:Element(name = "StoryArc")
	var storyArc: String? = null,

	@field:Element(name = "StoryArcNumber" , required = false ) @param:Element(name = "StoryArcNumber")
	var storyArcNumber: String? = null,

	@field:Element(name = "SeriesGroup" , required = false ) @param:Element(name = "SeriesGroup")
	var seriesGroup: String? = null,

	@field:Element(name = "AlternateCount" , required = false ) @param:Element(name = "AlternateCount")
	var alternateCount: Int? = null,

	@field:Element(name = "Summary" , required = false ) @param:Element(name = "Summary")
	var summary: String? = null,

	@field:Element(name = "Imprint" , required = false ) @param:Element(name = "Imprint")
	var imprint: String? = null,

	@field:Element(name = "Genre" , required = false ) @param:Element(name = "Genre")
	var genre: String? = null,

	@field:Element(name = "LanguageISO" , required = false ) @param:Element(name = "LanguageISO")
	var languageISO: String? = null,

	@field:Element(name = "Format" , required = false ) @param:Element(name = "Format")
	var format: String? = null,

	@field:Element(name = "AgeRating" , required = false ) @param:Element(name = "AgeRating")
	var ageRating: ComicInfoAgeRating? = null,

	@field:Element(name = "CommunityRating" , required = false ) @param:Element(name = "CommunityRating")
	var communityRating: Float? = null,

	@field:Element(name = "BlackAndWhite" , required = false ) @param:Element(name = "BlackAndWhite")
	var blackAndWhite: ComicInfoYesNo? = null,

	@field:Element(name = "Manga" , required = false ) @param:Element(name = "Manga")
	var manga: ComicInfoManga? = null,

	@field:Element(name = "Characters" , required = false ) @param:Element(name = "Characters")
	var characters: String? = null,

	@field:Element(name = "Teams" , required = false ) @param:Element(name = "Teams")
	var teams: String? = null,

	@field:Element(name = "Locations" , required = false ) @param:Element(name = "Locations")
	var locations: String? = null,

	@field:Element(name = "ScanInformation" , required = false ) @param:Element(name = "ScanInformation")
	var scanInformation: String? = null,

	@field:Element(name = "MainCharacterOrTeam" , required = false ) @param:Element(name = "MainCharacterOrTeam")
	var mainCharacterOrTeam: String? = null,

	@field:Element(name = "Review" , required = false ) @param:Element(name = "Review")
	var review: String? = null
)