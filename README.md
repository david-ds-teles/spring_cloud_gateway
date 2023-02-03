# About
A demo app using spring cloud gateway with spring security applied to router requests to different services.

- The demo has security applied to all the endpoints except whitelisted ones configured at `application.yml`.
- The /login will is whitelisted and proxied to `demo-account` project to generate a basic jwt token to illustrate authentication/authorization flow.
- Method security is applied to `demo-account` using the roles and the `@PreAuthorize` annotation as an example.


## Author
- **David Teles** - david.ds.teles@gmail.com

## Flow
The security flow is illustrated below:

```mermaid
sequenceDiagram
    participant Client
    participant API Gateway
    participant SecurityFilter
    participant Account

    Client ->> API Gateway: /api/demo/account/login
    API Gateway ->> Account: /login whitelisted path
    Account -->> Client: authorization token
    
    Client ->> API Gateway: /api/demo/account/**
    API Gateway ->> SecurityFilter: has authorization
    SecurityFilter ->> Account: authorized
    Account -->> Client: response

    Client ->> API Gateway: /api/demo/account/**
    API Gateway ->> SecurityFilter: invalid credentials
    SecurityFilter --x Client: 401 unauthorized

```