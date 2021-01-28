package com.happybird.photosender.domain

interface EntityMapper<Entity, Domain> {

    fun toDomain(e: Entity): Domain
    fun toEntity(d: Domain): Entity

    fun toDomainList(e: Iterable<Entity>): List<Domain> {
        return e.map {
            toDomain(it)
        }
    }

    fun toEntityList(e: Iterable<Domain>): List<Entity> {
        return e.map {
            toEntity(it)
        }
    }
}