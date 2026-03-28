package br.com.fenix.bilingualreader.model.interfaces

interface Entity<ID, T : Entity<ID, T>> {
    var id: ID?
}
