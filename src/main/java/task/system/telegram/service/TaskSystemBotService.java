package task.system.telegram.service;

import java.util.List;
import task.system.telegram.model.TaskSystemBotChat;

public interface TaskSystemBotService {
    void saveChatId(Long chatId, Long email);

    List<TaskSystemBotChat> findAll();

    TaskSystemBotChat findByUserId(Long userId);

    boolean existsById(Long userId);
}
