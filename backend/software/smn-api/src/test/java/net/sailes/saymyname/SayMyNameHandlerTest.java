package net.sailes.saymyname;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SayMyNameHandlerTest {

    private final SayMyNameHandler sayMyNameHandler = new SayMyNameHandler();
    @Test
    public void test() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        String nameRequest = """
                {
                    "name": "Mark Sailes"
                }
                """;
        request.setBody(nameRequest);
        APIGatewayProxyResponseEvent response = sayMyNameHandler.handleRequest(request, null);

        assertEquals(200, (int) response.getStatusCode());
        assertTrue(response.getBody().contains(".mp3"));
    }

    @Test
    public void clientErrorReturnedWhenNameTooLong() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        String nameRequest = """
                {
                    "name": "asdafagsdfsfasdewegsdvsdfgthgdgdfgsdfsdfsdfsdgsgfsgsdgsgsd asdfadqweqwafdzgfhhfgsdfsfesadafrrsfdsfsdfsdfsdf"
                }
                """;
        request.setBody(nameRequest);
        APIGatewayProxyResponseEvent response = sayMyNameHandler.handleRequest(request, null);

        assertEquals(400, (int) response.getStatusCode());
        assertTrue(response.getBody().contains("\"message\": \"invalid name - max length (70)\""));
    }
}