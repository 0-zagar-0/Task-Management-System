package task.system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import task.system.config.MapperConfig;
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.model.Comment;

@Mapper(config = MapperConfig.class)
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "userId", ignore = true)
    Comment toEntity(CommentRequestDto request);

    CommentResponseDto toDto(Comment comment);
}
