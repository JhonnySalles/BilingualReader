package br.com.fenix.bilingualreader.model.enums


interface BookLayoutType { }

enum class AlignmentLayoutType: BookLayoutType {
    Justify,
    Center,
    Right,
    Left;
}

enum class MarginLayoutType: BookLayoutType {
    Small,
    Medium,
    Big;
}

enum class SpacingLayoutType: BookLayoutType {
    Small,
    Medium,
    Big;
}