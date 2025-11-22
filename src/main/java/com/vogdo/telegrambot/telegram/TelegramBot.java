package com.vogdo.telegrambot.telegram;

import com.vogdo.telegrambot.agents.AIAgent;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.Comparator;
import java.util.List;
import java.nio.file.Files;
import org.springframework.core.io.ByteArrayResource;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.api.key}")
    private String telegramBotToken;
    private final AIAgent aiAgent;

    public TelegramBot(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @PostConstruct
    public void registerTelegramBot() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        UserMessage userMessage;

        try {
            sendTypingQuestion(chatId);



            // if image
            if (update.getMessage().hasPhoto()) {
                String caption = update.getMessage().getCaption();

                /*if (caption == null) {
                    caption = "Décris cette image.";
                }*/

                PhotoSize photo = update.getMessage().getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElse(null);

                if (photo == null) {
                    sendTextMessage(chatId, "Impossible de récupérer la photo.");
                    return;
                }

                GetFile getFileRequest = new GetFile(photo.getFileId());
                org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFileRequest);

                /*File telegramFile = execute(getFileRequest);
                String fileUrl = telegramFile.getFileUrl(getBotToken());*/

                //download locally
                java.io.File local = downloadFile(tgFile);
                byte[] bytes = Files.readAllBytes(local.toPath());

                // créer objet media avec url
                //List<Media> medias = new ArrayList<>();
                //Media img = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create(fileUrl));
                Media img = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes));


                String baseInstr = """
                        Tu reçois UNE image et éventuellement une question (en français).
                        - Si une question est fournie, réponds UNIQUEMENT à cette question.
                        - Sinon, fais une description détaillée (personnes/objets/texte/contexte).
                        - Si la question commence par "combien", renvoie uniquement un NOMBRE ENTIER.
                        """;

                String question = (caption != null && !caption.isBlank())
                        ? caption.trim()
                        : "Décris cette image.";

                boolean wantsCount = question.toLowerCase().startsWith("combien");
                if (wantsCount) {
                    question = question + " Réponds uniquement par un entier.";
                }


                // construire le UserMessage avec le texte caption et image media
                // si default msg => .text(caption != null ? caption : "Décris cette image.")
                userMessage = UserMessage.builder()
                        .text(baseInstr + "\\n\\nQUESTION_UTILISATEUR: " + question)
                        .media(List.of(img))
                        .build();

            }




            // que le texte
            else if (update.getMessage().hasText()) {
                userMessage = UserMessage.builder()
                        .text(update.getMessage().getText())
                        .build();
            }
            // ignore tous
            else {
                return;
            }

            // sent UserMessage to AIAgent wait for resp
            String answer = aiAgent.askAgent(userMessage);
            sendTextMessage(chatId, answer);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendTextMessage(chatId, "une erreur est survenue...");
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "SALSAHABOT";
    }

    @Override
    public String getBotToken() {
        return telegramBotToken;
    }

    private void sendTextMessage(long chatId, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        execute(sendMessage);
    }

    private void sendTypingQuestion(long chatId) throws TelegramApiException {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}