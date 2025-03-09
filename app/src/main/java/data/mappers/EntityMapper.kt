package com.elena.autoplanner.data.mappers

interface EntityMapper<Entity, Domain> {
    fun mapToDomain(entity: Entity): Domain
    fun mapToEntity(domain: Domain): Entity
}