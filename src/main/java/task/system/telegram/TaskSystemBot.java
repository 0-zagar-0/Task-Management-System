package task.system.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import task.system.exception.DataProcessingException;
import task.system.telegram.model.TaskSystemBotChat;
import task.system.telegram.service.TaskSystemBotService;

@Component
public class TaskSystemBot extends TelegramLongPollingBot {
    private final TaskSystemBotService botService;

    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.name}")
    private String botName;

    public TaskSystemBot(TaskSystemBotService botService) {
        this.botService = botService;
    }

    public void sendMessage(String message) {
        for (TaskSystemBotChat botChat : botService.findAll()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(botChat.getChatId());
            sendMessage.setText(message);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new DataProcessingException("Can't send message to telegram bot!", e);
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            botService.saveChatId(message.getChatId());

            for (TaskSystemBotChat botChat : botService.findAll()) {
                if (botChat.getChatId().equals(message.getChatId())) {
                    continue;
                }

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(botChat.getChatId());
                sendMessage.setText(getSenderName(message.getFrom()) + System.lineSeparator()
                        + message.getText()
                );

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new DataProcessingException(
                            "Can't send message, Telegram exception: " + e.getMessage()
                    );
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private String getSenderName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName() + System.lineSeparator()
                    + addUnderline(user.getFirstName(), user.getLastName());
        } else if (user.getFirstName() != null) {
            return user.getFirstName() + System.lineSeparator()
                    + addUnderline(user.getFirstName(), "");
        }

        return "Anonymous";
    }

    private String addUnderline(String firstName, String lastName) {
        int countLength = firstName.length() + lastName.length() + 1;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < countLength; i++) {
            result.append("_");
        }

        return result.toString();
    }
}
