package chat.tamtam.botapi.queries;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import chat.tamtam.botapi.VisitedUpdatesTracer;
import chat.tamtam.botapi.model.BotAddedToChatUpdate;
import chat.tamtam.botapi.model.BotRemovedFromChatUpdate;
import chat.tamtam.botapi.model.Chat;
import chat.tamtam.botapi.model.FailByDefaultUpdateVisitor;
import chat.tamtam.botapi.model.NoopUpdateVisitor;
import chat.tamtam.botapi.model.User;
import chat.tamtam.botapi.model.UserAddedToChatUpdate;
import chat.tamtam.botapi.model.UserRemovedFromChatUpdate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author alexandrchuprin
 */
public class UserAddedRemovedWebhookUpdatesTest extends GetUpdatesIntegrationTest {
    @Test
    public void shouldGetUpdatesInChat() throws Exception {
        test("UserAddedRemovedWebhookUpdatesTest#shouldGetUpdatesInChat", false);
    }

    @Test
    public void shouldGetUpdatesInChannel() throws Exception {
        test("UserAddedRemovedWebhookUpdatesTest#shouldGetUpdatesInChannel", true);
    }

    private void test(String chatTitle, boolean isChannel) throws Exception {
        Chat commonChat = getByTitle(getChats(), chatTitle);
        Long commonChatId = commonChat.getChatId();
        User bot2user = new User(bot2.getUserId(), bot2.getName(), bot2.getUsername(), true,
                System.currentTimeMillis());

        CountDownLatch bot2added = new CountDownLatch(1);
        CountDownLatch bot2removed = new CountDownLatch(1);
        CountDownLatch bot3expectedUpdates = new CountDownLatch(2);
        VisitedUpdatesTracer bot3updates = new VisitedUpdatesTracer(new NoopUpdateVisitor() {
            @Override
            public void visit(UserAddedToChatUpdate model) {
                assertThat(model.getChatId(), is(commonChatId));
                assertThat(model.getInviterId(), is(bot1.getUserId()));
                assertUser(model.getUser(), bot2user);
                assertThat(model.isChannel(), is(isChannel));
                bot3expectedUpdates.countDown();
            }

            @Override
            public void visit(UserRemovedFromChatUpdate model) {
                assertThat(model.getChatId(), is(commonChatId));
                assertThat(model.getAdminId(), is(bot1.getUserId()));
                assertUser(model.getUser(), bot2user);
                assertThat(model.isChannel(), is(isChannel));
                bot3expectedUpdates.countDown();
            }
        });

        bot2.addConsumer(new FailByDefaultUpdateVisitor() {
            @Override
            public void visit(BotAddedToChatUpdate model) {
                bot2added.countDown();
            }

            @Override
            public void visit(BotRemovedFromChatUpdate model) {
                bot2removed.countDown();
            }
        });

        bot1.addConsumer(new Bot1ToBot3RedirectingUpdateVisitor(bot3updates));

        try {
            addUser(client, commonChatId, bot2.getUserId());
            await(bot2added);
        } finally {
            removeUser(client, commonChatId, bot2.getUserId());
            await(bot2removed);
        }


        await(bot3expectedUpdates);
    }
}