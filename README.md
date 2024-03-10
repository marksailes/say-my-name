# Say My Name

Never be unsure how to pronounce a name again.

## Architecture

![High level architecture design](/docs/say-me-name-hld.png)

## Costs

### Amazon API Gateway 

ApiGatewayRequest
$3.50/million requests - first 333 million requests/month

### Amazon Polly 

EU-SynthesizeSpeechNeural-Characters
$16 per million characters for SynthesizeSpeechNeural-Characters in EU (Ireland)

### DynamoDB 

PayPerRequestThroughput
$1.4135 per million write request units (EU (Ireland))

Storage
no worth estimating

### Todo

- [x] Add x-ray segments for services
- [ ] Make a front-end
- [ ] Rate limiting
- [ ] Expose trace to front-end
- [ ] Add cache-aside strategy