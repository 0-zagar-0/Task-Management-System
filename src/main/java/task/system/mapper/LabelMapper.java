package task.system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import task.system.config.MapperConfig;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.model.Label;
import task.system.model.Label.Color;

@Mapper(config = MapperConfig.class)
public interface LabelMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "color", source = "color", qualifiedByName = "setDefaultColor")
    @Mapping(target = "name", source = "name", qualifiedByName = "setDefaultName")
    Label toEntity(LabelRequestDto request);

    LabelResponseDto toDto(Label label);

    @Named("setDefaultColor")
    default Color setDefaultColor(Color color) {
        return color != null ? color : Color.GRAY;
    }

    @Named("setDefaultName")
    default String setDefaultName(String name) {
        return name == null ? "" : name;
    }

}
