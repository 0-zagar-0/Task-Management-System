package task.system.config;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import task.system.exception.DataProcessingException;
import task.system.telegram.TaskSystemBot;

@Configuration
public class TelegramConfig {
    private final TaskSystemBot taskSystemBot;

    public TelegramConfig(TaskSystemBot taskSystemBot) {
        this.taskSystemBot = taskSystemBot;
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(taskSystemBot);
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Telegram exception: " + e.getMessage());
        }
    }
}
