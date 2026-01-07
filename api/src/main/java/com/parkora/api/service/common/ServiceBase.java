package com.parkora.api.service.common;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generic base class for service-layer components.
 * <p>
 * It implements {@link BaseMapper} so concrete services only need to provide
 * the actual mapping logic between entity and DTO, while this base class
 * offers common helper methods (null-safety, list mapping, etc.).
 *
 * @param <E> entity type
 * @param <D> DTO type
 */
public abstract class ServiceBase<E, D> implements BaseMapper<E, D> {

    /**
     * Maps an entity to a DTO with null-safety.
     */
    public D mapToDto(E entity) {
        return entity == null ? null : toDto(entity);
    }

    /**
     * Maps a DTO to an entity with null-safety.
     */
    public E mapToEntity(D dto) {
        return dto == null ? null : toEntity(dto);
    }

    /**
     * Maps a collection of entities to a list of DTOs, skipping nulls.
     */
    public List<D> mapToDtoList(Collection<E> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps a collection of DTOs to a list of entities, skipping nulls.
     */
    public List<E> mapToEntityList(Collection<D> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(Objects::nonNull)
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
