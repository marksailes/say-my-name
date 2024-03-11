# Say My Name

Never be unsure how to pronounce a name again.

## Architecture

![High level architecture design](/docs/say-me-name-hld.png)

## Costs

| Service            | Service API                          | Cost                                                                         | Cost per request |
|--------------------|--------------------------------------|------------------------------------------------------------------------------|------------------|
| Amazon API Gateway | ApiGatewayRequest                    | $3.50/million requests - first 333 million requests/month                    |                  |
| Amazon Polly       | EU-SynthesizeSpeechNeural-Characters | $16/million characters for SynthesizeSpeechNeural-Characters in EU (Ireland) |                  |
| Amazon DynamoDB    | PayPerRequestThroughput              | $1.4135/million write request units (EU (Ireland))                           |                  |  

### Todo

- [x] Add x-ray segments for services
- [x] Make a front-end
- [ ] Expose trace to front-end
- [ ] Add cache-aside strategy
- [ ] Rate limiting
