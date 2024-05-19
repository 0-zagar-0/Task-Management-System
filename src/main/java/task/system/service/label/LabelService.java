package task.system.service.label;

import java.util.List;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;

public interface LabelService {
    LabelResponseDto create(LabelRequestDto request);

    List<LabelResponseDto> getAllByProjectId(Long projectId);

    LabelResponseDto getById(Long id);

    LabelResponseDto updateById(Long id, LabelUpdateRequestDto request);

    void deleteById(Long id);
}
