package com.parkora.api.service.common;

/**
 * Generic mapper contract for converting between an entity and its DTO.
 *
 * @param <E> entity type
 * @param <D> DTO type
 */
public interface BaseMapper<E, D> {

    D toDto(E entity);

    E toEntity(D dto);
}

