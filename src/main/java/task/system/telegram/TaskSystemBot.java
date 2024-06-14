package task.system.telegram;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import task.system.exception.DataProcessingException;
import task.system.model.User;
import task.system.repository.user.UserRepository;
import task.system.telegram.model.TaskSystemBotChat;
import task.system.telegram.service.TaskSystemBotService;

@Component
public class TaskSystemBot extends TelegramLongPollingBot {
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private final TaskSystemBotService botService;
    private final UserRepository userRepository;

    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.name}")
    private String botName;

    public TaskSystemBot(TaskSystemBotService botService, UserRepository userRepository) {
        this.botService = botService;
        this.userRepository = userRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage sendMessage = checkUpdateText(update);
        try {
            if (sendMessage != null) {
                execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Can't send message to telegram!", e);
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

    public void sendMessage(String text, Long userId) {
        if (text != null && !text.isEmpty()) {
            if (botService.existsById(userId)) {
                TaskSystemBotChat botChat = botService.findByUserId(userId);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(botChat.getChatId());
                sendMessage.setText(text);

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new DataProcessingException("Can't send message", e);
                }
            }
        }
    }

    public void sendMessage(String text, Set<User> users) {
        if (text != null && !text.isEmpty()) {
            for (User user : users) {
                if (botService.existsById(user.getId())) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(botService.findByUserId(user.getId()).getChatId());
                    sendMessage.setText(text);

                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        throw new DataProcessingException("Can't send message", e);
                    }
                }
            }
        }
    }

    private SendMessage checkUpdateText(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());

            if (Pattern.compile(EMAIL_REGEX).matcher(message.getText()).matches()) {
                String email = message.getText();
                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    botService.saveChatId(message.getChatId(), userOptional.get().getId());
                    sendMessage.setText(
                            "Thank you! Now you will receive messages from your projects here =_)"
                    );
                } else {
                    sendMessage.setText("There are no users with this email address, "
                            + "please enter a valid email");
                }
            }

            if (message.getText().equals("/start")) {
                sendMessage.setText("Hello, " + message.getFrom().getFirstName()
                        + " please enter the e-mail address registered in the service.");

            }
            return sendMessage;
        }
        return null;
    }
}
