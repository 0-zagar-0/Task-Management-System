package task.system.telegram.service;

import java.util.List;
import task.system.telegram.model.TaskSystemBotChat;

public interface TaskSystemBotService {
    void saveChatId(Long chatId);

    List<TaskSystemBotChat> findAll();
}
