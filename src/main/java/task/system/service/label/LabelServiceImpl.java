package task.system.service.label;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.LabelMapper;
import task.system.model.Label;
import task.system.repository.label.LabelRepository;
import task.system.service.project.ProjectService;

@Service
public class LabelServiceImpl implements LabelService {
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;
    private final ProjectService projectService;

    public LabelServiceImpl(
            LabelRepository labelRepository,
            LabelMapper labelMapper,
            ProjectService projectService
    ) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
        this.projectService = projectService;
    }

    @Override
    public LabelResponseDto create(LabelRequestDto request) {
        projectService.getById(request.getProjectId());

        if (request.getColor() == null && request.getName() == null) {
            Label label = labelRepository.findDefaultGreyLabel();
            return labelMapper.toDto(label);
        }

        Optional<Label> byNameAndColor = labelRepository.findByNameAndColorAndProjectId(
                request.getColor(), request.getName(), request.getProjectId()
        );

        if (byNameAndColor.isPresent()) {
            return labelMapper.toDto(byNameAndColor.get());
        }

        Label label = labelMapper.toEntity(request);
        Label savedLabel = labelRepository.save(label);
        return labelMapper.toDto(savedLabel);
    }

    @Override
    public List<LabelResponseDto> getAllByProjectId(Long projectId) {
        projectService.getById(projectId);

        List<Label> allByProjectId = labelRepository.findAllByProjectId(projectId);
        return allByProjectId.stream()
                .map(labelMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LabelResponseDto getById(Long id) {
        return labelMapper.toDto(getLabelById(id));
    }

    @Override
    public LabelResponseDto updateById(Long id, LabelUpdateRequestDto request) {
        Label labelById = getLabelById(id);
        List<Label> defaultLabels = labelRepository.findDefaultLabels();
        Optional<Label> any = defaultLabels.stream()
                .filter(label -> label.getId().equals(labelById.getId()))
                .findAny();

        if (any.isPresent()) {
            throw new DataProcessingException("You can't update default label");
        }

        Optional.ofNullable(request.getName())
                .filter(name -> !name.equalsIgnoreCase(labelById.getName()))
                .ifPresent(labelById::setName);
        Optional.ofNullable(request.getColor())
                .filter(color -> !color.equals(labelById.getColor()))
                .ifPresent(labelById::setColor);
        return labelMapper.toDto(labelRepository.update(labelById));
    }

    @Override
    public void deleteById(Long id) {
        getLabelById(id);
        labelRepository.deleteById(id);
    }

    private Label getLabelById(Long id) {
        return labelRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find label by id: " + id)
        );
    }
}
