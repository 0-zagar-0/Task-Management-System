package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;
import task.system.service.label.LabelService;

@Tag(name = "Label management", description = "Endpoints for labels action")
@RestController
@RequestMapping(value = "/labels")
public class LabelController {
    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    @Operation(summary = "Create", description = "Create label")
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponseDto create(@Valid @RequestBody LabelRequestDto request) {
        return labelService.create(request);
    }

    @GetMapping(value = "/project/{projectId}")
    @Operation(summary = "Get all", description = "Get all labels by project id")
    @ResponseStatus(HttpStatus.OK)
    public List<LabelResponseDto> getAll(@PathVariable Long projectId) {
        return labelService.getAllByProjectId(projectId);
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Get by id", description = "Get label by id")
    @ResponseStatus(HttpStatus.OK)
    public LabelResponseDto getById(@PathVariable Long id) {
        return labelService.getById(id);
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Update by id", description = "Update label by id")
    @ResponseStatus(HttpStatus.OK)
    public LabelResponseDto updateById(@PathVariable Long id,
                                       @RequestBody LabelUpdateRequestDto request
    ) {
        return labelService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete by id", description = "Delete label by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        labelService.deleteById(id);
    }
}
