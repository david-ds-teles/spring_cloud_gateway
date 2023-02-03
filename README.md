#
This is a demo app using spring cloud gateway to router requests to different services.

## Author
- **David Teles** - david.ds.teles@gmail.com

## Flow
```mermaid
sequenceDiagram
    participant Client
    participant API Gateway
    participant Account

    Client->>+API Gateway: /api/demo/account/**
    Note right of API Gateway: RewritePath filter applied

    API Gateway->>-Account: /api/account/**
    Account-->>Client: Account Response
```