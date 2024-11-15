package com.guyshalev.Salt_security.mapper;

import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.ParameterDTO;
import com.guyshalev.Salt_security.model.entity.Model;
import com.guyshalev.Salt_security.model.entity.Parameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ModelMapper {

    @Mapping(target = "id", ignore = true)
    Model toEntity(ModelDTO dto);

    ModelDTO toDTO(Model entity);

    @Mapping(target = "id", ignore = true)
    Parameter toEntity(ParameterDTO dto);

    ParameterDTO toDTO(Parameter entity);

    List<Model> toEntityList(List<ModelDTO> dtos);

    List<ModelDTO> toDTOList(List<Model> entities);
}
