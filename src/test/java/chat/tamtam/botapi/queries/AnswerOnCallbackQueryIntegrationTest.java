package chat.tamtam.botapi.queries;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import chat.tamtam.botapi.model.AttachmentRequest;
import chat.tamtam.botapi.model.BotStartedUpdate;
import chat.tamtam.botapi.model.Callback;
import chat.tamtam.botapi.model.CallbackAnswer;
import chat.tamtam.botapi.model.CallbackButton;
import chat.tamtam.botapi.model.Chat;
import chat.tamtam.botapi.model.ContactAttachmentRequest;
import chat.tamtam.botapi.model.ContactAttachmentRequestPayload;
import chat.tamtam.botapi.model.FailByDefaultUpdateVisitor;
import chat.tamtam.botapi.model.InlineKeyboardAttachmentRequest;
import chat.tamtam.botapi.model.InlineKeyboardAttachmentRequestPayload;
import chat.tamtam.botapi.model.MessageBody;
import chat.tamtam.botapi.model.MessageCallbackUpdate;
import chat.tamtam.botapi.model.MessageCreatedUpdate;
import chat.tamtam.botapi.model.NewMessageBody;
import chat.tamtam.botapi.model.SendMessageResult;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author alexandrchuprin
 */
public class AnswerOnCallbackQueryIntegrationTest extends GetUpdatesIntegrationTest {
    @Test
    public void testInChat() throws Exception {
        shouldEditMessageOnAnswer(getByTitle(getChats(), "test chat #1"));
    }

    @Test
    public void testInChannel() throws Exception {
        shouldEditMessageOnAnswer(getByTitle(getChats(), "test channel #1"));
    }

    @Test
    public void testInDialog() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        FailByDefaultUpdateVisitor consumer = new FailByDefaultUpdateVisitor(bot1) {
            @Override
            public void visit(BotStartedUpdate model) {
                // ignore
            }

            @Override
            public void visit(MessageCallbackUpdate model) {
                // ignore
            }

            @Override
            public void visit(MessageCreatedUpdate model) {
                // bot 3 will reply to bot 1 that it has received `message_edited` update
                // will wait to finish test
                assertThat(model.getMessage().getSender().getUserId(), is(bot3.getUserId()));
                done.countDown();
            }
        };

        try (AutoCloseable ignored = bot1.addConsumer(BOT_1_BOT_3_DIALOG, consumer)) {
            shouldEditMessageOnAnswer(getChat(BOT_1_BOT_3_DIALOG));
            await(done);
        }

    }

    private void shouldEditMessageOnAnswer(Chat chat) throws Exception {
        bot3.startAnotherBot(bot1.getUserId(), null);

        ArrayBlockingQueue<Callback> callbacks = new ArrayBlockingQueue<>(1);
        FailByDefaultUpdateVisitor consumer = new FailByDefaultUpdateVisitor(bot1) {
            @Override
            public void visit(MessageCallbackUpdate model) {
                callbacks.add(model.getCallback());
            }

            @Override
            public void visit(BotStartedUpdate model) {
                assertThat(model.getUser().getUserId(), is(bot3.getUserId()));
            }

            @Override
            public void visit(MessageCreatedUpdate model) {
                // ignore
            }
        };


        Long chatId = chat.getChatId();
        try (AutoCloseable ignored = bot1.addConsumer(chatId, consumer)) {
            // bot1 send message with button
            String payload = randomText(16);
            NewMessageBody body = originalMessage(payload);
            SendMessageResult result = botAPI.sendMessage(body).chatId(chatId).execute();
            String messageId = result.getMessage().getBody().getMid();

            // bot3 presses callback button
            bot3.pressCallbackButton(messageId, payload);

            String editedText = randomText();
            ContactAttachmentRequestPayload arPayload = new ContactAttachmentRequestPayload(randomText(16))
                    .contactId(bot1.getUserId())
                    .vcfPhone("+79991234567");

            AttachmentRequest contactAR = new ContactAttachmentRequest(arPayload);
            NewMessageBody answerMessage = new NewMessageBody(editedText, Collections.singletonList(contactAR),
                    null);

            CallbackAnswer answer = new CallbackAnswer().message(answerMessage);
            Callback callback = callbacks.poll(10, TimeUnit.SECONDS);
            new AnswerOnCallbackQuery(client, answer, callback.getCallbackId()).execute();

            MessageBody editedMessage = getMessage(client, messageId).getBody();

            assertThat(editedMessage.getText(), is(editedText));
            compare(Collections.singletonList(contactAR), editedMessage.getAttachments());
        }
    }

    @NotNull
    private static NewMessageBody originalMessage(String payload) {
        CallbackButton button = new CallbackButton(payload, "button text");
        InlineKeyboardAttachmentRequestPayload keyboardPayload = new InlineKeyboardAttachmentRequestPayload(
                Collections.singletonList(Collections.singletonList(button)));
        AttachmentRequest keyboardAttach = new InlineKeyboardAttachmentRequest(keyboardPayload);
        String text = "AnswerOnCallbackQueryIntegrationTest message " + now();
        return new NewMessageBody(text, Collections.singletonList(keyboardAttach), null);
    }
}