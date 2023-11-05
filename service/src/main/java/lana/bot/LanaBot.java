package lana.bot;

import lana.handlers.KeyBoardHandler;
import lana.properties.BotProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class LanaBot extends TelegramLongPollingBot {
    private final BotProperties botProperties;
    private final PostCreatorService postCreatorService;
    private final GroupService groupService;
    private final UserTelegramService userTelegramService;

    public LanaBot(BotProperties botProperties , PostCreatorService postCreatorService, GroupService groupService, UserTelegramService userTelegramService) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.postCreatorService = postCreatorService;
        this.groupService = groupService;
        this.userTelegramService = userTelegramService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMyChatMember()) {
            validateChatMembers(update);
        }
        //тупая заглушка чтобы просто протестить перессылку админу
//        if (update.hasMessage()) {
//            createMessageToAdminsApprove(update);
//        }
//        //тут типа действия при том или ином нажатии кнопки, я пока сделал заглушку такую
//        if (update.hasCallbackQuery()) {
//            processingCallBackData(update);
//        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }

    private void processingCallBackData(Update update) {

        String callData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String answer = callData.equals(BotCallbacks.ACCEPT.getCallbackData()) ?
                "Принял предложку" : getReactionOfReject(update.getCallbackQuery().getMessage().getText());
        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer).parseMode("MarkdownV2").build();
        editMessage(editMessageText);
    }

    private void editMessage(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(SendMessage sendMessage) {
        SendChatAction sendChatAction = SendChatAction.builder()
                .chatId(sendMessage.getChatId())
                .action(ActionType.TYPING.toString())
                .build();

        try {
            execute(sendMessage);
            execute(sendChatAction);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void createMessageToAdminsApprove(Update update) {
        //TODO: делегировать пост крейтор сервису создание поста для бд
//        Message message = update.getMessage();getMessage
//        Long userID = message.getChat().getId();
//        String text = message.getText();
//        //TODO: научиться получать контент из сообщения
//        Byte[] binaryData = null;
//        Post post = postCreatorService.generatePost(userID,);

        KeyBoardHandler keyBoardHandler = new KeyBoardHandler();

//        for (var adminID : adminsID) {
//            SendMessage sendMessage = SendMessage.builder()
//                    .text(update.getMessage().getText())
//                    .chatId(adminID)
//                    .build();
//            sendMessage = keyBoardHandler.createInlineKeyBoard(sendMessage);
//            sendMessage(sendMessage);
//        }
    }

    private String getReactionOfReject(String answer) {
        return "~" + answer + "~";
    }

    private void findAdminsAfterAddGroup(Chat chat) {
        List<ChatMember> admins = new ArrayList<>();
        var id = chat.getId();
        try {
            admins = execute(new GetChatAdministrators(String.valueOf(id)));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        //TODO: переделать на метод в репозиторй
        for(var adm : admins) {
            userTelegramService.createAdmin(adm.getUser(), chat.getId());
        }

    }
    private void validateChatMembers(Update update) {
        var myChatMember = update.getMyChatMember();
        var chatMember = myChatMember.getNewChatMember();
        var user = chatMember.getUser();
        if (user.getIsBot()) {
            doWithGroup(update);
        }
    }
    private void doWithGroup(Update update) {
        var myChatMember = update.getMyChatMember();
        var chatMember = myChatMember.getNewChatMember();
        var chat = myChatMember.getChat();

        if (!chat.getType().equals("channel"))
            return;

        if (chatMember instanceof ChatMemberAdministrator) {
            groupService.addChannel(chat);
            findAdminsAfterAddGroup(chat);
        }

        if (chatMember instanceof ChatMemberLeft || chatMember instanceof ChatMemberBanned) {
            groupService.deleteChannel(chat);
        }

    }

    private void checkUserIsAdminOrBanned(Update update) {
        var myChatMember = update.getMyChatMember();
        var newChatMember = myChatMember.getNewChatMember();
//        var oldChatMember = myChatMember.getOldChatMember();
        if (newChatMember instanceof ChatMemberAdministrator) {
            userTelegramService.createAdmin(myChatMember);
        }
    }
}
