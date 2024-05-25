package task.system.telegram.service;

import java.util.List;
import org.springframework.stereotype.Service;
import task.system.exception.EntityNotFoundException;
import task.system.telegram.model.TaskSystemBotChat;
import task.system.telegram.repository.TaskSystemBotRepository;

@Service
public class TaskSystemBotServiceImpl implements TaskSystemBotService {
    private final TaskSystemBotRepository botRepository;

    public TaskSystemBotServiceImpl(TaskSystemBotRepository botRepository) {
        this.botRepository = botRepository;
    }

    @Override
    public void saveChatId(Long chatId, Long userId) {
        if (botRepository.findByChatId(chatId).isEmpty()) {
            TaskSystemBotChat botChat = new TaskSystemBotChat();
            botChat.setChatId(chatId);
            botChat.setUserId(userId);
            botRepository.save(botChat);
        }
    }

    @Override
    public List<TaskSystemBotChat> findAll() {
        return botRepository.findAll();
    }

    @Override
    public TaskSystemBotChat findByUserId(Long userId) {
        return botRepository.findByUserId(userId).orElseThrow(() -> new EntityNotFoundException(
                "Can't find TaskSystemBotChat by user ID: " + userId)
        );
    }

    @Override
    public boolean existsById(Long userId) {
        return botRepository.findByUserId(userId).isPresent();
    }
}
