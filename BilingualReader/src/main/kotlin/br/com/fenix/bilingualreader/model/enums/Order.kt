package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class Order(private val description: Int) {
    None(R.string.option_order_none),

    //Book - Manga
    Name(R.string.option_order_name),
    Date(R.string.option_order_date),
    LastAccess(R.string.option_order_access),
    Favorite(R.string.option_order_favorite),

    Genre(R.string.option_order_genre),
    Author(R.string.option_order_author),
    Series(R.string.option_order_series),

    //Vocabulary
    Description(R.string.option_order_description),
    Frequency(R.string.option_order_frequency);

    open fun getDescription() : Int = this.description
    
}