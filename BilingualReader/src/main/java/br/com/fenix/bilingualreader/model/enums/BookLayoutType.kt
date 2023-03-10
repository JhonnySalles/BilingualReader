package br.com.fenix.bilingualreader.model.enums


interface BookType { }

enum class AlignmentType: BookType {
    Justify,
    Center,
    Right,
    Left;
}

enum class MarginType: BookType {
    Small,
    Medium,
    Big;
}

enum class SpacingType: BookType {
    Small,
    Medium,
    Big;
}