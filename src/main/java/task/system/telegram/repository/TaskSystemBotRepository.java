package task.system.telegram.repository;

import java.util.List;
import java.util.Optional;
import task.system.telegram.model.TaskSystemBotChat;

public interface TaskSystemBotRepository {
    void save(TaskSystemBotChat botChat);

    Optional<TaskSystemBotChat> findByChatId(Long chatId);

    List<TaskSystemBotChat> findAll();

    Optional<TaskSystemBotChat> findByUserId(Long userId);
}
