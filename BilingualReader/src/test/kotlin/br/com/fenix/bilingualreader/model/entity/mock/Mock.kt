package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.interfaces.Entity

interface Mock<ID, E : Entity<ID, E>> {
    fun mockEntity(): E
    fun mockEntityList(): List<E>
    fun mockEntity(id: ID): E
    fun asserts(expected: E?, actual: E?)
}
