package chat.tamtam.botapi.client;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import chat.tamtam.botapi.UnitTest;
import chat.tamtam.botapi.server.TamTamService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author alexandrchuprin
 */
@Category(UnitTest.class)
public class TamTamClientTest {
    @Test
    public void shouldObtainEndpointFromEnvironment() {
        String endpoint = "https://testapi.tamtam.chat";
        TamTamClient client = new TamTamClient(TamTamService.ACCESS_TOKEN, mock(TamTamTransportClient.class),
                mock(TamTamSerializer.class)) {
            @Override
            String getEnvironment(String name) {
                if (name.equals(TamTamClient.ENDPOINT_ENV_VAR_NAME)) {
                    return endpoint;
                }

                return super.getEnvironment(name);
            }
        };

        assertThat(client.getEndpoint(), is(endpoint));
    }
}