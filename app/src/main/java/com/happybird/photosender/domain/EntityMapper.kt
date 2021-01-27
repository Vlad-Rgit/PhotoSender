package com.happybird.photosender.domain

interface EntityMapper<Entity, Domain> {
    fun toDomain(e: Entity): Domain
    fun toEntity(d: Domain): Entity

    fun toDomainList(e: List<Entity>): List<Domain> {
        return e.map {
            toDomain(it)
        }
    }

    fun toEntityList(e: List<Domain>): List<Entity> {
        return e.map {
            toEntity(it)
        }
    }
}