package task.system.service.label;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.LabelMapper;
import task.system.model.Label;
import task.system.model.Project;
import task.system.repository.label.LabelRepository;
import task.system.service.project.ProjectService;

@ExtendWith(MockitoExtension.class)
class LabelServiceImplTest {
    private static ProjectDetailsResponseDto projectDetails;
    @InjectMocks
    private LabelServiceImpl labelService;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private LabelMapper labelMapper;
    @Mock
    private ProjectService projectService;

    @BeforeAll
    static void setUp() {
        projectDetails = createProjectDetails(
                1L,
                1L,
                "project1",
                "description1",
                createUsersIds(1, 6),
                createUsersIds(1, 3)
        );
    }

    @Test
    @DisplayName("Create label with valid data, should return LabelResponseDto")
    void create_WithValidData_ShouldReturnLabelResponseDto() {
        //Given
        LabelRequestDto request = createLabelRequestDto(1L, "label1", Label.Color.CYAN);
        Label label = createLabel(
                null, request.getName(), request.getColor(), request.getProjectId()
        );
        Label savedLabel = createLabel(
                7L, request.getName(), request.getColor(), request.getProjectId()
        );
        LabelResponseDto expected = createLabelResponse(savedLabel);

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(labelMapper.toEntity(request)).thenReturn(label);
        when(labelRepository.save(label)).thenReturn(savedLabel);
        when(labelMapper.toDto(savedLabel)).thenReturn(expected);
        when(labelRepository.findByNameAndColorAndProjectId(
                request.getColor(), request.getName(), request.getProjectId())
        ).thenReturn(Optional.empty());

        //Then
        LabelResponseDto actual = labelService.create(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectService, times(1)).getById(request.getProjectId());
        verify(labelRepository, times(1)).findByNameAndColorAndProjectId(
                request.getColor(), request.getName(), request.getProjectId()
        );
        verify(labelMapper, times(1)).toEntity(request);
        verify(labelRepository, times(1)).save(label);
        verify(labelMapper, times(1)).toDto(savedLabel);
    }

    @Test
    @DisplayName("Create label with project id non exists, should throw an Exception")
    void create_WithNonExistProjectId_ShouldThrowException() {
        //Given
        Long projectId = 999L;
        LabelRequestDto request = createLabelRequestDto(projectId, "label1", Label.Color.CYAN);
        String expected = "Can't find project by id: " + projectId;

        //When
        when(projectService.getById(projectId)).thenThrow(new EntityNotFoundException(expected));
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.create(request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(projectId);
    }

    @Test
    @DisplayName("Create label with null color and null name, should return default grey label")
    void create_WithNullColorAndName_ShouldReturnGreyLabel() {
        //Given
        LabelRequestDto request = createLabelRequestDto(1L, null, null);
        Label defaultLabel = createLabel(1L, null, Label.Color.GRAY, 1L);
        LabelResponseDto expected = createLabelResponse(defaultLabel);

        //When
        when(projectService.getById(1L)).thenReturn(projectDetails);
        when(labelRepository.findDefaultGreyLabel()).thenReturn(defaultLabel);
        when(labelMapper.toDto(defaultLabel)).thenReturn(expected);

        //Then
        LabelResponseDto actual = labelService.create(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectService, times(1)).getById(1L);
        verify(labelRepository, times(1)).findDefaultGreyLabel();
        verify(labelMapper, times(1)).toDto(defaultLabel);
    }

    @Test
    @DisplayName("Create label with exists label data, should return exists LabelResponseDto")
    void create_WithExistsLabelData_ShouldReturnExistsLabel() {
        //Given
        Label existsLabel = createLabel(7L, "label1", Label.Color.LIME, 1L);
        LabelRequestDto request = createLabelRequestDto(1L, "label1", Label.Color.LIME);
        LabelResponseDto expected = createLabelResponse(existsLabel);

        //When
        when(projectService.getById(1L)).thenReturn(projectDetails);
        when(labelRepository.findByNameAndColorAndProjectId(
                request.getColor(), request.getName(), request.getProjectId())
        ).thenReturn(Optional.of(existsLabel));
        when(labelMapper.toDto(existsLabel)).thenReturn(expected);

        //Then
        LabelResponseDto actual = labelService.create(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectService, times(1)).getById(1L);
        verify(labelRepository, times(1)).findByNameAndColorAndProjectId(
                request.getColor(), request.getName(), request.getProjectId()
        );
        verify(labelMapper, times(1)).toDto(existsLabel);
    }

    @Test
    @DisplayName("Get all with valid project id, should return all LabelResponseDto")
    void getAll_WithValidProjectId_ShouldReturnAllLabelResponseDto() {
        //Given
        Long projectId = 1L;
        List<Label> labels = createListLabel();

        //When
        when(projectService.getById(projectId)).thenReturn(projectDetails);
        when(labelRepository.findAllByProjectId(projectId)).thenReturn(labels);

        for (Label label : labels) {
            when(labelMapper.toDto(label)).thenReturn(createLabelResponse(label));
        }

        //Then
        List<LabelResponseDto> expected = createListLabelResponseDtos(labels);
        List<LabelResponseDto> actual = labelService.getAllByProjectId(projectId);
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(6), actual.get(6)));

        //Verify
        verify(projectService, times(1)).getById(projectId);
        verify(labelRepository, times(1)).findAllByProjectId(projectId);
        verify(labelMapper, times(labels.size())).toDto(any(Label.class));
    }

    @Test
    @DisplayName("Get all with non exists project id, should throw an Exception")
    void getAll_WithNonExistProjectId_ShouldThrowException() {
        //Given
        Long projectId = 999L;
        String expected = "Can't find project by id: " + projectId;

        //When
        when(projectService.getById(projectId)).thenThrow(new EntityNotFoundException(expected));
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.getAllByProjectId(projectId)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(projectId);
    }

    @Test
    @DisplayName("Get by id with valid id, should return LabelResponseDto")
    void getById_WithValidId_ShouldReturnLabelResponseDto() {
        //Given
        Long id = 7L;
        Label label = createLabel(7L, "label1", Label.Color.LIME, 1L);
        LabelResponseDto expected = createLabelResponse(label);

        //When
        when(projectService.getById(1L)).thenReturn(projectDetails);
        when(labelRepository.findById(id)).thenReturn(Optional.of(label));
        when(labelMapper.toDto(label)).thenReturn(expected);

        //Then
        LabelResponseDto actual = labelService.getById(id);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectService, times(1)).getById(1L);
        verify(labelRepository, times(1)).findById(id);
        verify(labelMapper, times(1)).toDto(label);
    }

    @Test
    @DisplayName("Get by id with non exists label id, should throw an Exception")
    void getById_WithNonExistLabelId_ShouldThrowException() {
        //Given
        Long id = 999L;
        String expected = "Can't find label by id: " + id;

        //When
        when(labelRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.getById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(labelRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update by id with valid data, should return updated LabelResponseDto")
    void updateById_WithValidData_ShouldReturnUpdatedLabelResponseDto() {
        //Given
        Label label = createLabel(7L, "label1", Label.Color.LIME, 1L);
        List<Label> defaultLabels = createListDefaultLabels();
        Label updatedLabel = createLabel(7L, "update", Label.Color.MAGENTA, 1L);
        LabelResponseDto expected = createLabelResponse(updatedLabel);

        //When
        when(labelMapper.toDto(updatedLabel)).thenReturn(expected);
        when(labelRepository.findById(7L)).thenReturn(Optional.of(label));
        when(labelRepository.findDefaultLabels()).thenReturn(defaultLabels);
        when(projectService.getById(1L)).thenReturn(projectDetails);
        label.setName("update");
        label.setColor(Label.Color.MAGENTA);
        when(labelRepository.update(label)).thenReturn(updatedLabel);

        //Then
        LabelUpdateRequestDto request = createLabelUpdateRequestDto("update", Label.Color.MAGENTA);
        LabelResponseDto actual = labelService.updateById(7L, request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(labelRepository, times(1)).findById(7L);
        verify(labelRepository, times(1)).findDefaultLabels();
        verify(projectService, times(1)).getById(1L);
        verify(labelRepository, times(1)).update(label);
        verify(labelMapper, times(1)).toDto(updatedLabel);
    }

    @Test
    @DisplayName("Update by id with id default label, should throw an Exception")
    void updateById_WithIdDefaultLabel_ShouldThrowException() {
        //Given
        Long id = 3L;
        Label label = createLabel(id, null, Label.Color.BLUE, 1L);
        LabelUpdateRequestDto request = createLabelUpdateRequestDto("update", Label.Color.MAGENTA);
        List<Label> defaultLabels = createListDefaultLabels();

        //When
        when(labelRepository.findById(id)).thenReturn(Optional.of(label));
        when(labelRepository.findDefaultLabels()).thenReturn(defaultLabels);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> labelService.updateById(id, request)
        );

        //Then
        String expected = "You can't update or delete default label";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(labelRepository, times(1)).findById(id);
        verify(labelRepository, times(1)).findDefaultLabels();
    }

    @Test
    @DisplayName("Delete by id with valid id, should does not throw an Exception")
    void deleteById_WithValidId_ShouldDoesNotThrowException() {
        //Given
        Long id = 7L;
        Label label = createLabel(7L, "label1", Label.Color.LIME, 1L);
        List<Label> defaultLabels = createListDefaultLabels();

        //When
        when(labelRepository.findById(id)).thenReturn(Optional.of(label));
        when(labelRepository.findDefaultLabels()).thenReturn(defaultLabels);
        when(projectService.getById(1L)).thenReturn(projectDetails);

        //Then
        assertDoesNotThrow(() -> labelService.deleteById(id));

        //Verify
        verify(labelRepository, times(1)).findById(id);
        verify(labelRepository, times(1)).findDefaultLabels();
        verify(projectService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("Delete by id with non exists id, should throw an Exception")
    void deleteById_WithNonExistId_ShouldThrowException() {
        //Given
        Long id = 999L;

        //When
        when(labelRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.deleteById(id)
        );

        //Then
        String expected = "Can't find label by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(labelRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Delete by id with id default label, should throw an Exception")
    void deleteById_WithIdDefaultLabel_ShouldThrowException() {
        //Given
        Long id = 2L;
        Label label = createLabel(id, null, Label.Color.RED, 1L);
        List<Label> defaultLabels = createListDefaultLabels();

        //When
        when(labelRepository.findById(id)).thenReturn(Optional.of(label));
        when(labelRepository.findDefaultLabels()).thenReturn(defaultLabels);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> labelService.deleteById(id)
        );

        //Then
        String expected = "You can't update or delete default label";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(labelRepository, times(1)).findById(id);
        verify(labelRepository, times(1)).findDefaultLabels();
    }

    private LabelUpdateRequestDto createLabelUpdateRequestDto(String name, Label.Color color) {
        LabelUpdateRequestDto updateRequestDto = new LabelUpdateRequestDto();
        updateRequestDto.setName(name);
        updateRequestDto.setColor(color);
        return updateRequestDto;
    }

    private List<LabelResponseDto> createListLabelResponseDtos(List<Label> labels) {
        List<LabelResponseDto> labelResponseDtos = new ArrayList<>();

        for (Label label : labels) {
            labelResponseDtos.add(createLabelResponse(label));
        }

        return labelResponseDtos;
    }

    private List<Label> createListLabel() {
        List<Label> responseDtos = new ArrayList<>(createListDefaultLabels());

        for (int i = 7; i <= 8; i++) {
            Label.Color color = Label.Color.values()[i];
            responseDtos.add(createLabel((long) i, "label" + i, color, 1L));
        }

        return responseDtos;
    }

    private List<Label> createListDefaultLabels() {
        List<Label> defaultLabels = new ArrayList<>();
        defaultLabels.add(createLabel(1L, null, Label.Color.GRAY, null));
        defaultLabels.add(createLabel(2L, null, Label.Color.RED, null));
        defaultLabels.add(createLabel(3L, null, Label.Color.BLUE, null));
        defaultLabels.add(createLabel(4L, null, Label.Color.GREEN, null));
        defaultLabels.add(createLabel(5L, null, Label.Color.BROWN, null));
        defaultLabels.add(createLabel(6L, null, Label.Color.PURPLE, null));
        return defaultLabels;
    }

    private LabelResponseDto createLabelResponse(
            Long id, String name, Label.Color color, Long projectId
    ) {
        LabelResponseDto response = new LabelResponseDto();
        response.setId(id);
        response.setName(name);
        response.setColor(color);
        response.setProjectId(projectId);
        return response;
    }

    private LabelResponseDto createLabelResponse(Label label) {
        LabelResponseDto response = new LabelResponseDto();
        response.setId(label.getId());
        response.setName(label.getName());
        response.setColor(label.getColor());
        response.setProjectId(label.getProjectId());
        return response;
    }

    private Label createLabel(Long id, String labelName, Label.Color color, Long projectId) {
        Label label = new Label();
        label.setId(id);
        label.setName(labelName);
        label.setColor(color);
        label.setProjectId(projectId);
        return label;
    }

    private LabelRequestDto createLabelRequestDto(Long projectId, String name, Label.Color color) {
        LabelRequestDto request = new LabelRequestDto();
        request.setProjectId(projectId);
        request.setName(name);
        request.setColor(color);
        return request;
    }

    private static ProjectDetailsResponseDto createProjectDetails(
            Long id,
            Long mainUserId,
            String name,
            String description,
            Set<Long> users,
            Set<Long> admins) {
        ProjectDetailsResponseDto project = new ProjectDetailsResponseDto();
        project.setId(id);
        project.setName(name);
        project.setDescription(description);
        project.setStatus(Project.Status.IN_PROGRESS);
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        users.add(mainUserId);
        project.setUserIds(users);
        admins.add(mainUserId);
        project.setAdministratorIds(admins);
        project.setMainUser(mainUserId);
        return project;
    }

    private static Set<Long> createUsersIds(int startIndex, int endIndex) {
        Set<Long> users = new HashSet<>();

        for (int i = startIndex; i <= endIndex; i++) {
            users.add((long) i);
        }

        return users;
    }
}
