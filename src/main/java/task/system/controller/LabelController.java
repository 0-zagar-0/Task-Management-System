package task.system.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;
import task.system.service.label.LabelService;

@RestController
@RequestMapping(value = "/labels")
public class LabelController {
    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    public LabelResponseDto create(@Valid @RequestBody LabelRequestDto request) {
        return labelService.create(request);
    }

    @GetMapping(value = "/project/{projectId}")
    public List<LabelResponseDto> getAll(@PathVariable Long projectId) {
        return labelService.getAllByProjectId(projectId);
    }

    @GetMapping(value = "/{id}")
    public LabelResponseDto getById(@PathVariable Long id) {
        return labelService.getById(id);
    }

    @PutMapping(value = "/{id}")
    public LabelResponseDto updateById(@PathVariable Long id,
                                       @RequestBody LabelUpdateRequestDto request
    ) {
        return labelService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteById(@PathVariable Long id) {
        labelService.deleteById(id);
    }
}
