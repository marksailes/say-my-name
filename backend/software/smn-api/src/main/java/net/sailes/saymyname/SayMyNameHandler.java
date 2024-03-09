package net.sailes.saymyname;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.LanguageCode;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for requests to Lambda function.
 */
public class SayMyNameHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final PollyClient pollyClient = PollyClient.create();
    private static final S3Client s3Client = S3Client.create();
    private static final S3Presigner s3Presigner = S3Presigner.create();
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(SayMyNameHandler.class);
    public static final String BUCKET_NAME = "public-say-my-name";

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        NameRequest nameRequest;
        String name;
        
        try {
            nameRequest = objectMapper.readValue(input.getBody(), NameRequest.class);
            name = nameRequest.name();
            logger.info("Received request to pronounce: {}", name);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }

        if (!NameRequestValidator.isValid(name)) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("""
                            {
                                "message": "invalid name - max length (70)"
                            }
                            """);
        }

        try {
            LanguageCode enGb = LanguageCode.EN_GB;
            ResponseInputStream<SynthesizeSpeechResponse> synthesizeSpeechResult = callPolly(enGb, name);
            String s3Key = UUID.randomUUID() + ".mp3";
            saveToS3(s3Key, synthesizeSpeechResult);
            String presignedUrl = getPresignedUrl(s3Key);
            storeItemInDynamoDb(name, enGb, s3Key);

            String body = "{ \"location\": \"" + presignedUrl + "\" }";
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(body);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }
    }

    private static void storeItemInDynamoDb(String name, LanguageCode enGb, String s3Key) {
        String itemKey = name.replaceAll(" ", "_").toLowerCase() + "-" + enGb;
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName("say-my-name")
                .item(Map.of("name", AttributeValue.fromS(itemKey),
                        "s3Location", AttributeValue.fromS(s3Key)))
                .build();
        dynamoDbClient.putItem(putItemRequest);
    }

    private static String getPresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        String presignedUrl = presignedRequest.url().toExternalForm();
        logger.info("S3 presigned url: {}", presignedUrl);
        return presignedUrl;
    }

    private static ResponseInputStream<SynthesizeSpeechResponse> callPolly(LanguageCode enGb, String name) {
        SynthesizeSpeechRequest speechRequest = SynthesizeSpeechRequest.builder()
                .engine(Engine.NEURAL)
                .voiceId(VoiceId.EMMA)
                .languageCode(enGb)
                .outputFormat(OutputFormat.MP3)
                .text(name)
                .build();

        ResponseInputStream<SynthesizeSpeechResponse> synthesizeSpeechResult = pollyClient.synthesizeSpeech(speechRequest);
        logger.info("Polly synthesized speech");
        return synthesizeSpeechResult;
    }

    private static void saveToS3(String s3Key, ResponseInputStream<SynthesizeSpeechResponse> synthesizeSpeechResult) throws IOException {
        logger.info("Creating an S3 s3Key of: {}", s3Key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
        PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(synthesizeSpeechResult.readAllBytes()));
        logger.info("S3 response id: {}", putObjectResponse.responseMetadata().requestId());
    }
}
