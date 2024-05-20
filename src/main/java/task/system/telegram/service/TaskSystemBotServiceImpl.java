package task.system.telegram.service;

import java.util.List;
import org.springframework.stereotype.Service;
import task.system.telegram.model.TaskSystemBotChat;
import task.system.telegram.repository.TaskSystemBotRepository;

@Service
public class TaskSystemBotServiceImpl implements TaskSystemBotService {
    private final TaskSystemBotRepository botRepository;

    public TaskSystemBotServiceImpl(TaskSystemBotRepository botRepository) {
        this.botRepository = botRepository;
    }

    @Override
    public void saveChatId(Long chatId) {
        if (botRepository.findByChatId(chatId).isEmpty()) {
            TaskSystemBotChat botChat = new TaskSystemBotChat();
            botChat.setChatId(chatId);
            botRepository.save(botChat);
        }
    }

    @Override
    public List<TaskSystemBotChat> findAll() {
        return botRepository.findAll();
    }
}
