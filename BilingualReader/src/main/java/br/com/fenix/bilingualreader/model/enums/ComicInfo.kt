package br.com.fenix.bilingualreader.model.enums

enum class ComicInfoYesNo {
    Unknown,
    No,
    Yes;
}

enum class ComicInfoPageType {
    FrontCover,
    InnerCover,
    Roundup,
    Story,
    Advertisement,
    Editorial,
    Letters,
    Preview,
    BackCover,
    Other,
    Deleted;
}

enum class ComicInfoManga {
    Unknown,
    No,
    Yes,
}

enum class ComicInfoAgeRating(var description: String) {
    Unknown("Unknown"),
    Adults("Adults Only 18+"),
    Early("Early Childhood"),
    Everyone("Everyone"),
    Everyone10("Everyone 10+"),
    G("G"),
    Kids("Kids to Adults"),
    M("M"),
    MA15("MA15+"),
    Mature("Mature 17+"),
    PG("PG"),
    R18("R18+"),
    Pending("Rating Pending"),
    Teen("Teen"),
    X18("X18+");
}