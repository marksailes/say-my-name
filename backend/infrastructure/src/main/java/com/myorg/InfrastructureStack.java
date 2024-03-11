package com.myorg;

import net.sailes.saymyname.SayMyNameHandler;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.Cors;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

import java.util.List;

public class InfrastructureStack extends Stack {

    public static final int MEMORY_SIZE_1VCPU = 1792;

    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ITable sayMyNameTable = Table.fromTableName(this, "say-my-name-table", "say-my-name");
        IBucket sayMyNamePublicBucket = Bucket.fromBucketName(this, "say-my-name-public-bucket", "public-say-my-name");

        Function sayMyNameFunction = Function.Builder.create(this, "say-my-name-function")
                .runtime(Runtime.JAVA_21)
                .memorySize(MEMORY_SIZE_1VCPU)
                .timeout(Duration.seconds(10))
                .handler(SayMyNameHandler.class.getCanonicalName())
                .architecture(Architecture.ARM_64)
                .tracing(Tracing.ACTIVE)
                .code(Code.fromAsset("../software/smn-api/target/say-my-name-api-1.0.jar"))
                .build();

        sayMyNameFunction.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("polly:SynthesizeSpeech"))
                .resources(List.of("*"))
                .build());
        sayMyNamePublicBucket.grantReadWrite(sayMyNameFunction);
        sayMyNameTable.grantReadWriteData(sayMyNameFunction);

        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "say-my-name-api")
                .restApiName("Say-My-Name-API")
                .handler(sayMyNameFunction)
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowHeaders(List.of("*"))
                        .allowOrigins(Cors.ALL_ORIGINS)
                        .allowMethods(Cors.ALL_METHODS)
                        .build())
                .build();

        new CfnOutput(this, "API URL", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }
}
