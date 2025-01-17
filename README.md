[![][cloudentity-logo]](https://cloudentity.com)

* [Introduction](#intro)
* [Build](#build)
  * [Standalone](#build-standalone)
  * [Docker](#build-docker)
* [Run](#run)
  * [Standalone](#run-standalone)
  * [Docker](#run-docker)
* [Configure](#config)
  * [Meta config](#config-meta)
  * [Routing rules](#config-routing)
  * [API groups](#config-api-groups)
  * [Service discovery](#config-service-discovery)
  * [HTTP server](#config-http-server)
  * [HTTP clients](#config-http-clients)
  * [Open tracing](#config-open-tracing)
  * [Access log](#config-access-log)
  * [Proxy headers](#config-proxy-headers)
* [Plugins](#plugins)
  * [Authentication](docs/plugins/authn.md)
    * OAuth 2 with JWT access token
    * OAuth 2 with opaque token introspection
  * [Request transformation](docs/plugins/transform-request.md)
  * [CORS](docs/plugins/cors.md)
  * [Brute-force protection](docs/plugins/bruteforce.md)
  * [Rate limiting](docs/plugins/rate-limit.md)
  * [ACP authorizer](docs/plugins/acp-authz.md)
* [Plugin development guide](docs/plugin-dev.md)
* [How to](#how-to)
  * [Read routing rules from Consul](docs/howtos/config-store-consul.md)
  * [Read secrets from Vault](docs/howtos/config-store-vault.md)
  * [Inject OAuth2 subject into request](docs/howtos/inject-oauth2-sub.md)
  * [Configure SSL/TLS for ingress traffic with private key in Vault secret](docs/howtos/tls/http-server-tls-vault.md)
  * [Configure SSL/TLS for ingress traffic with private key in environment variable or file](docs/howtos/tls/http-server-tls-env.md)
* [Performance](#performance)

<a id="intro"></a>

## Introduction

**Pyron**  is a lightweight, developers, and DevOps friendly API Gateway with advanced authentication and authorization capabilities.  It's build using Scala on top of the Vertx.io framework enabling high performance and non-blocking execution. Plugin based architecture allows further extensions of capabilities and features as well as seamless integrations. Native support of the HashiCorp Consul service catalog enables Pyron to bridge the traditional network with service discovery based routing.  Support for declarative configuration model allows Pyron to be easily integrated into the  CI/CD process. 

### Supported functionality


#### Deployment

Pyron can be deployed and run as:

* **Standalone JVM process**
* **Docker Container**
* **Kubernetes service deployed using Helm**

#### Routing and Proxy

Pyron provides many tools to publish and manage your API endpoints:

* **L7 Routing** - API calls can be routed to different targets based on the URI path patterns
* **URI Rewrite** - ability to drop a prefix or modify the URI before the request is proxied to the upstream service
* **TCP/IP Proxy Headers** - Support for X-Forwarded-For, X-TrueClient-IP, Proxy VIA headers
* **Load Balancing** - Client-side load balancing support
* **Static up-stream service discovery** - ability to configure a static set of upstream services to be used in load balancing pool 
* **Catalog based up-stream service discovery** - ability to utilize Consul service catalog to discover instances of the up-stream services
* **Static TLS support** - ability to enable TLS based on provided certificate and key, ability to configure desired ciphers and TLS versions
* **Vault based TLS support** - ability to use Vault as a source for the TLS certificates

#### Request/response Transformation

Cloudentity Pyron can normalize your API by transforming and managing requests to the services it protects.

* **HTTP Request Header transformation** - ability to inject/modify headers
* **HTTP Request path parameter transformation** - ability to inject/modify path parameters
* **HTTP Request body transformation** - ability to modify the request body (currently only available for JSON based payload)
* **Identity context injection** - *Enterprise Feature* - ability to inject the JWT based header with client/user context to upstream services
* **Cross-Domain Support** - CORS headers publishing and support

#### Logging & Monitoring
* **Correlation ID integration** - Injection as well as utilization of the external correlation id to relate transactions for full tracing
* **Open Tracing support** - provides visibility into cross-service communication and instrumentation, enabling the distributed tracing.
* **Rich Access Logs** - the ability to stream logs to file, socket or Kafka 


#### Authentication

As an enforcement point, Pyron integrates with a wide range of protocols and tools to ensure the request is not only authorized but also secure in the context of a wide range of risk and business rules.

* **SSO based** - *Enterprise Feature* - authenticate and authorize the request using CE SSO token
* **JWT OAuth Access Token based** - authenticate and authorize the request using OAuth Token
* **Opaque OAuth Access Token based** - authenticate and authorize the request using Opaque OAuth Token with Introspection
* **OAuth1.0 access token based** - *Enterprise Feature* - authenticate and authorize the request using OAuth 1.0b access token
* **Custom JWT based** - *Enterprise Feature* - authenticate and authorize the request using custom JWT
* **HMAC header-based** - *Enterprise Feature* - authenticate and authorize the request using HMAC authorization headers
* **Fallback Authentication** - the ability to chain multiple authentication methods together and define the fallback scenarios Request authentication.


#### Authorization (Enterprise Feature)

* **Distributed Authorization** - *Enterprise Feature* - ability to integrate with Cloudentity * MicroPerimeter PDP to perform local authorization decisions including RBAC and ABAC and PBAC
* **Centralized Authorization** - *Enterprise Feature* - ability to integrate with Cloudentity centralized Trust Engine to perform advanced risk adaptive and data-level authorization 
* **Policy-based authorization** - *Enterprise Feature* - enforce conditional, if/then/else policies as defined in the TrUST Engine.

#### Configuration

Pyron offers support for declarative configuration model out of the box with support for various sources of configuration.

* **File-based Declarative configuration** - the ability to load configuration from JSON/YAML based configuration files 
* **Consul & Vault based declarative configuration** - the ability to load configuration from Consul Key-Value Store as well as secrets and certificates from Vault
* **HTTP based declarative configuration** - the ability to load configuration from an external HTTP endpoint

#### Integration with Microservices

* **Ingress for Zero Trust Network** - *Enterprise Feature* - Integration with Zero Trust networks and ability to inject the client/user identity & service identity fingerprints to enable transaction-based micro-segmentation
* **API Request retries** - ability to configure failure conditions and count of the API request retries strategy
* **Request failover** - the ability to failover the request to next health instance of the upstream microservice
* **Circuit Breaker support** - support for the circuit breaker pattern, works in concert with request retries and request failover
* **OpenAPI Specification publishing and transformation** - ability to transform and published the OpenAPI specs provided by the upstream services

#### API Security

Cloudentity Pyron also provides broad API protection with many standard features.

* **Brute Force Protection (in-memory implementation)** - Ability to protect APIs against brute force attacks or perform simple rate spike arrests - in-memory implementation
* **Brute Force Protection (IMDG based)** - *Enterprise Feature* - Ability to protect APIs against brute force attacks or make simple rate spike arrests - implementation using IMDG enabling shared state between all instances of the API Gateway.
* **API Throttling** - *Enterprise Feature* - the ability to throttle the request based on IP/client ID/user
* **Audit** - *Enterprise Feature* - Detailed audit logs enriched with authentication context of the user/client 

#### Extensibility

Pyron also allows for custom plugins, which can be used to integrate legacy or proprietary systems as part of the standard data flow and enforcement. This could include custom callouts, complex business logic, or custom protocol/security management.

* **Plugins** - ability to add third-party plugins in Java or Scala
* **Plugins Reload** - ability to add plugins in runtime

<a id="build"></a>
## Build

Pyron depends on https://github.com/Cloudentity/vertx-tools. Clone it and build with `mvn install` command first.

#### Prerequisites

* Maven 3
* JDK 1.8
* make
* Docker 19.03 (optional)

<a id="build-standalone"></a>
### Standalone

```
$ make standalone
```

<a id="build-docker"></a>
### Docker

```
$ make docker
```

<a id="run"></a>
## Run

Configure routing rules in `rules.json` and environment variables in `envs` file if required.

By default, Pyron runs on 8080 port. Set `HTTP_SERVER_PORT` env variable to change it.

<a id="run-standalone"></a>
### Standalone

```
$ cd run/standalone
$ ./run.sh
```
<a id="run-docker"></a>
### Docker

```
$ cd run/docker
$ docker run --env-file envs --network="host" --name pyron -v "$(pwd)"/configs:/configs -v "$(pwd)"/plugin-jars:/plugin-jars -d cloudentity/pyron
```

<a id="config"></a>
## Configure

* [Meta config](#config-meta)
* [Routing rules](#config-routing)
  * [Method and path pattern](#config-method-path-pattern)
  * [Path prefix](#config-path-prefix)
  * [Rewrite path](#config-rewrite-path)
  * [Rewrite method](#config-rewrite-method)
  * [Response timeout](#config-response-timeout)
  * [Retry](#config-retry)
  * [Preserve Host header](#config-preserve-host-header)
  * [Request body handling (buffer/stream/drop)](#config-request-body-handling)
  * [Request body size limit](#config-request-body-limit)
* [API groups](#config-api-groups)
* [Service discovery](#config-service-discovery)
  * [Consul service discovery](#sd-consul)
  * [Configuration-based service discovery](#sd-static)
  * [Self-registration in Consul](#sd-register)
* [HTTP server](#config-http-server)
* [HTTP clients](#config-http-clients)
  * Default retries and timeout
  * Circuit breaker
* [Open tracing](#config-open-tracing)
* [Access log](#config-access-log)
  * Authentication context and request headers in access log
* [Proxy headers](#config-proxy-headers)

<a id="config-meta"></a>
### Meta config

At startup Pyron needs `meta-config.json` file describing where to read configuration from.

```json
{
  "scanPeriod": 5000,
  "stores": [
    {
      "type": "classpath",
      "format": "json",
      "config": {
        "path": "config.json"
      }
    },
    {
      "type": "file",
      "format": "json",
      "config": {
        "path": "rules.json"
      }
    }
  ]
}
```

Above `meta-config.json` defines two configuration stores: `config.json` from JAR classpath and `rules.json` from the file system.

`config.json` defines minimal configuration required to run Pyron. Routing rules are provided in `rules.json`.

You will find `meta-config.json` in the run folder (`run/standalone` or `run/docker`).

Learn how to read configuration from [Consul](docs/howtos/config-store-consul.md) and secrets from [Vault](docs/howtos/config-store-vault.md).

<a id="config-routing"></a>
### Routing rules

A rule defines routing to a target endpoint. Rules are grouped in blocks that share common attributes in the `default` object.
If an endpoint attribute is missing then it is taken from `default`.

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        ...
      ]
    }
  ]
}
```

| Attribute          | Description                                    |
|:-------------------|:-----------------------------------------------|
| targetHost         | host of target service (upstream)              |
| targetPort         | port of target service                         |

<a id="config-method-path-pattern"></a>
#### Method and path pattern

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user"
        }
      ]
    }
  ]
}
```

| Attribute          | Description                                                                            |
|:-------------------|:---------------------------------------------------------------------------------------|
| method             | HTTP method                                                                            |
| pathPattern        | regular expression extended with support of path-param placeholders, e.g. `/user/{id}` |

Example: client's call `POST /user` is proxied to target `POST /user`.

<a id="config-path-prefix"></a>
#### Path prefix

Expose multiple endpoints using the same path prefix.

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80,
        "pathPrefix": "/example",
        "dropPrefix": true
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user"
        },
        {
          "method": "GET",
          "pathPattern": "/user/{id}"
        }
      ]
    }
  ]
}
```

| Attribute          | Description                                                   |
|:-------------------|:--------------------------------------------------------------|
| pathPrefix         | prefix appended to `pathPattern` (optional)                   |
| dropPrefix         | drop path prefix when calling target service (default true)   |

By default, the prefix is dropped when calling target service. To preserve the prefix set `dropPrefix` to false.

Example: client's call `POST /example/user` is proxied to target `POST /user`.

<a id="config-rewrite-path"></a>
#### Rewrite path

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "GET",
          "pathPattern": "/user/{id}",
          "rewritePath": "/entities/user/{id}"
        }
      ]
    }
  ]
}
```

| Attribute          | Description                                                                            |
|:-------------------|:---------------------------------------------------------------------------------------|
| rewritePath        | path that Pyron calls target service at (optional, `pathPattern` used if this not set) |

Example: client's call `GET /user/123` is proxied to target `GET /entities/user/123`

<a id="config-rewrite-method"></a>
#### Rewrite method

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
              "method": "POST",
              "rewriteMethod": "PUT",
              "pathPattern": "/user"
            }
      ]
    }
  ]
}
```

| Attribute          | Description                                                                            |
|:-------------------|:---------------------------------------------------------------------------------------|
| rewriteMethod      | method that Pyron calls target service with (optional, `method` used if this not set)  |

Example: client's call `POST /user` is proxied to target `PUT /user`.

<a id="config-response-timeout"></a>
#### Response timeout

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user",
          "call": {
            "responseTimeout": 3000
          }
        }
      ]
    }
  ]
}
```

| Attribute            | Description                                                                            |
|:---------------------|:---------------------------------------------------------------------------------------|
| call.responseTimeout | target service response timeout in milliseconds                                        |

<a id="config-retry"></a>
#### Retry

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user",
          "call": {
            "retries": 1,
            "failureHttpCodes": [500],
            "retryFailedResponse": true,
            "retryOnException": true
          }
        }
      ]
    }
  ]
}
```

| Attribute                | Description                                                                            |
|:-------------------------|:---------------------------------------------------------------------------------------|
| call.retries             | maximum number of retries                                                              |
| call.failureHttpCodes    | response codes that Pyron retries if returned by target service                         |
| call.retryFailedResponse | retry call if target service returned code in `failureHttpCodes` (default true)        |
| call.retryOnException    | retry call on HTTP client exception, e.g. response timeout (default true)              |

<a id="config-preserve-host-header"></a>
#### Preserve Host header

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user",
          "preserveHostHeader": true
        }
      ]
    }
  ]
}
```

| Attribute            | Description                                                                        |
|:---------------------|:-----------------------------------------------------------------------------------|
| preserveHostHeader   | should send to target service Host header received from the client (default false) |

By default, Pyron sends target host in Host header to target service, set `preserveHostHeader` to true to send Host header sent by the client instead.

<a id="config-request-body-handling"></a>
#### Request body handling (buffer/stream/drop)

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user",
          "requestBody": "buffer"
        }
      ]
    }
  ]
}
```

| Attribute     | Description                                                             |
|:--------------|:------------------------------------------------------------------------|
| requestBody   | body handling strategy (`buffer`, `stream` or `drop`, default `buffer`) |

* `buffer` - load entire body into memory, required by some plugins (e.g. `transform-request`)
* `stream` - stream the body directly to target service (after applying request plugins)
* `drop` - ignore the body, do not transfer it to target service (`Content-Length` header of target request is set to 0)

<a id="config-request-body-limit"></a>
#### Request body size limit

```json
{
  "rules": [
    {
      "default": {
        "targetHost": "example.com",
        "targetPort": 80
      },
      "endpoints": [
        {
          "method": "POST",
          "pathPattern": "/user",
          "requestBodyMaxSize": 100
        }
      ]
    }
  ]
}
```

| Attribute            | Description                                                      |
|:---------------------|:-----------------------------------------------------------------|
| requestBodyMaxSize   | max number of kilobytes transferred to target service (optional) |

> NOTE<br/>
> If maximum body size is reached then Pyron responds to the client with `413` status code.
> <br/>
> <br/>
> If the request body is using `chunked` Transfer-Encoding (content length is not known upfront) and `requestBody` is `stream`
> then the body streaming to target service stops when `requestBodyMaxSize` kilobytes has been streamed.
> Otherwise no data is sent to target service if `requestBodyMaxSize` limit would be reached.
> <br/>
> <br/>
> Set DEFAULT_REQUEST_BODY_MAX_SIZE env variable with default `requestBodyMaxSize` for all routing rules.

<a id="config-api-groups"></a>
### API Groups

API Groups allow separating routing rule sets. You can define a set of rules and expose it on a domain and/or base-path.
An incoming request is initially matched against domain and base-path and then dispatched to appropriate set for
further processing.

```json
{
  "apiGroups": {
    "example": {
      "_group": {
        "domains": ["demo.com"],
        "basePath": "/apis"
      },
      "_rules": [
        {
          "default": {
            "targetHost": "example.com",
            "targetPort": 8080
          },
          "endpoints": [
            {
              "method": "GET",
              "pathPattern": "/user"
            }
          ]
        }
      ]
    }
  }
}
```

| Attribute                | Description                                        |
|:-------------------------|:---------------------------------------------------|
| _group.domains           | Host headers Pyron matches the API group for        |
| _group.basePath          | base path Pyron matches the API group at (optional) |

Note `_` (underscore) in `_rules` and `_group`.

[API Groups configuration details.](docs/api-groups.md)

<a id="config-service-discovery"></a>
### Service discovery

Pyron Gateway provides support for service discovery utilizing Consul client or configuration object.

```json
{
  "rules": [
    {
      "default": {
        "targetService": "example-service"
      },
      "endpoints": [
        ...
      ]
    }
  ]
}
```

| Attribute          | Description                                         |
|:-------------------|:----------------------------------------------------|
| targetService      | service-name of target nodes from service-discovery |

Pyron calls nodes with `targetService` service-name using a round-robin load balancer.

Below you will find instructions on how to enable service discovery providers.

<a id="sd-consul"></a>
#### Consul service discovery

Add `sd-provider/consul` to `MODULES` environment variable, i.e. `MODULES=["sd-provider/consul"]`.

| Env variable          | Description                                         |
|:----------------------|:----------------------------------------------------|
| CONSUL_HOST           | host                                                |
| CONSUL_PORT           | port (default 8500)                                 |
| CONSUL_SSL            | SSL enabled (default false)                         |
| CONSUL_ACL_TOKEN      | ACL token (optional)                                |
| CONSUL_DC             | data center (optional)                              |
| CONSUL_TIMEOUT        | connection timeout (optional)                       |
| CONSUL_SD_SCAN_PERIOD | nodes refresh period in milliseconds (default 2000) |

Note: nodes registered in Consul need to have `http-endpoint` tag and `ssl` tag if exposed over SSL.

<a id="sd-static"></a>
#### Configuration-based service discovery

Add `sd-provider/static` to `MODULES` environment variable, i.e. `MODULES=["sd-provider/static"]`.

Add `sd-records` configuration attribute (e.g. in `system.json` file).

```json
{
  "sd-records": [
    {
      "name": "example-service",
      "location": {
        "host": "example.com",
        "port": 80,
        "ssl": false,
        "root": "/v1"
      }
    }
  ]
}
```

| Attribute          | Description                          |
|:-------------------|:-------------------------------------|
| name               | service-name of target node          |
| location.host      | host of target node                  |
| location.port      | port of target node                  |
| location.ssl       | SSL of target node                   |
| location.root      | root path of target node (optional)  |

<a id="sd-register"></a>
#### Self-registration in Consul

Pyron node can register itself in Consul for service discovery.

Add `sd-registrar/consul` to `MODULES` environment variable, i.e. `MODULES=["sd-registrar/consul"]`.

| Env variable                     | Description                                                    |
|:---------------------------------|:---------------------------------------------------------------|
| CONSUL_HOST                      | host                                                           |
| CONSUL_PORT                      | port (default 8500)                                            |
| CONSUL_SSL                       | SSL enabled (default false)                                    |
| CONSUL_ACL_TOKEN                 | ACL token (optional)                                           |
| CONSUL_DC                        | data center (optional)                                         |
| CONSUL_TIMEOUT                   | connection timeout (optional)                                  |
| REGISTER_SD_SERVICE_NAME         | Pyron service name                                              |
| REGISTER_SD_HOST                 | host of Pyron node                                              |
| REGISTER_SD_PORT                 | port of Pyron node                                              |
| REGISTER_SD_SSL                  | ssl of Pyron node (default false)                               |
| REGISTER_SD_HEALTHCHECK_HOST     | host of Pyron health-check (default REGISTER_SD_HOST)           |
| REGISTER_SD_HEALTHCHECK_PORT     | port of Pyron health-check (default REGISTER_SD_PORT)           |
| REGISTER_SD_HEALTHCHECK_PATH     | path of Pyron health-check (default /alive)                     |
| REGISTER_SD_HEALTHCHECK_INTERVAL | health-check interval (default 3s)                             |
| REGISTER_SD_DEREGISTER_AFTER     | node de-register period when health-check fails (default 600s) |
| REGISTER_SD_TAGS                 | extra node tags (default [])                                   |

<a id="config-http-server"></a>
### HTTP server

Pyron uses Vertx HTTP server implementation. Use environment variables to configure `io.vertx.core.http.HttpServerOptions`.

Environment variables map to `HttpServerOptions` ([see docs](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html)) attributes in following way:

* All variable names start with `HTTP_SERVER_` prefix,
* HttpServerOptions attribute name is capitalized and camel-case is replaced with underscore `_`.
* If an attribute has object value then it's sub-attribute env names are prefixed with `HTTP_SERVER_{parent-env-name}__` (note double underscore).

Examples:

| Name                                            | HttpServerOptions attribute                                                                                                                                |
|:------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| HTTP_SERVER_PORT                                | [port](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html#setPort-int-)                                                               |
| HTTP_SERVER_ACCEPT_BACKLOG                      | [acceptBacklog](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html#setAcceptBacklog-int-)                                            |
| HTTP_SERVER_PEM_TRUST_OPTIONS__CERT_PATHS       | [pemTrustOptions.certPaths](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html#setPemTrustOptions-io.vertx.core.net.PemTrustOptions-) |

In order to set `HttpServerOptions` attribute with collection value use JSON syntax, e.g. `HTTP_SERVER_PEM_TRUST_OPTIONS__CERT_PATHS=["/etc/ssl/cert.pem"]`.

<a id="config-http-clients"></a>
### HTTP clients

Pyron uses Vertx implementation of HTTP clients. Use environment variables to configure default `io.vertx.core.http.HttpClientOptions`.

Environment variables map to `HttpClientOptions` ([see docs](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html)) attributes the same way they map to `HttpServerOptions`.


| Name                                            | HttpClientOptions attribute                                                                                                                                |
|:------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| HTTP_CLIENT_MAX_POOL_SIZE                       | [maxPoolSize](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html#setMaxPoolSize-int-)                                                 |
| HTTP_CLIENT_KEEP_ALIVE                          | [keepAlive](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html#setKeepAlive-boolean-)                                                 |
| HTTP_CLIENT_TRUST_ALL                           | [trustAll](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html#setTrustAll-boolean-)                                                   |


You can configure HTTP client for each target service separately (note that default attributes from environment variables are ignored in this case):

```json
{
  "smart-http-target-clients": {
    "example-service": {
      "http": {
        "maxPoolSize": 50
      }
    }
  }
}
```

#### Default retries and timeout

```json
{
  "smart-http-target-clients": {
    "example-service": {
      "responseTimeout": 3000,
      "retries": 5,
      "failureHttpCodes": [500]
    }
  }
}
```

Target client retry and timeout default attributes are overridden by values set in routing rule.

#### Circuit breaker

Configure `io.vertx.circuitbreaker.CircuitBreakerOptions` in `circuitBreaker` object to enable circuit breaker functionality per target service.

```json
{
  "smart-http-target-clients": {
    "example-service": {
      "circuitBreaker": {
        "maxFailures": 3
      }
    }
  }
}
```

<a id="config-open-tracing"></a>
### Open tracing

Add `tracing/jaeger` to `MODULES` environment variable, i.e. `MODULES=["tracing/jaeger"]`.

| Env variable                      | Description                                                         |
|:----------------------------------|:--------------------------------------------------------------------|
| TRACING_SERVICE_NAME              | Pyron name in Jaeger                                                |
| JAEGER_AGENT_HOST                 | Jaeger agent host (optional)                                        |
| JAEGER_AGENT_PORT                 | Jaeger agent port (optional)                                        |
| JAEGER_SAMPLER_MANAGER_HOST_PORT  | Jaeger sampler host:port (optional)                                 |
| TRACING_FORMAT                    | tracing format: cloudentity, jaeger, zipkin (default `cloudentity`) |

`cloudentity` tracing format allows configuration of span context key and baggage item prefix:

| Env variable            | Description                                            |
|:------------------------|:-------------------------------------------------------|
| TRACING_TRACE_ID        | span context key (default `x-trace-id`)                |
| TRACING_BAGGAGE_PREFIX  | baggage item prefix (default `x-ctx-`)                 |

<a id="config-access-log"></a>
### Access log

```json
{
  "timestamp": "2018-04-06T15:14:33.929Z",
  "trueClientIp": "192.168.0.13",
  "remoteClient": "192.168.0.127",
  "http": {
    "httpVersion": "HTTP/1.1",
    "method": "GET",
    "uri": "/service-a/user/123",
    "status": "200"
  },
  "gateway": {
    "method": "GET",
    "path": "/user/{userId}",
    "pathPrefix": "/service-a",
    "aborted": false,
    "targetService": "service-a"
  },
  "request": {
    "headers": {
      "Host": ["example.com"]
    }
  },
  "authnCtx": {
      "method": "oauth2",
      "user":"4b1b17f8-a934-458f-3c08-cc01d9f9b917",
      "uid":"admin@cloudentity.com"
    }
  },
  "timeMs": "3"
}
```

| Attribute             | Description                                                                                               |
|:----------------------|:----------------------------------------------------------------------------------------------------------|
| timestamp             | request time in ISO 8601 format                                                                           |
| trueClientIp          | IP address of original client, either X-Real-IP header or first IP from X-Forwarded-For or remote address |
| remoteIp              | IP of the direct client                                                                                   |
| authnCtx              | authentication context                                                                                    |
| httpVersion           | HTTP version                                                                                              |
| method                | HTTP method                                                                                               |
| uri                   | URI without host                                                                                          |
| status                | HTTP status code                                                                                          |
| gateway.method        | method of matching rule                                                                                   |
| gateway.path          | path pattern of matching rule                                                                             |
| gateway.pathPrefix    | path prefix of matching rule                                                                              |
| gateway.aborted       | true if Pyron aborted the call without proxying to target service; false otherwise                        |
| gateway.interrupted   | true if the call was interrupted by the client; false otherwise                                           |
| gateway.failed        | true if an exception occurred on target call or plugin application; not set otherwise                     |
| gateway.targetService | target service of matching rule                                                                           |
| request.headers       | request headers                                                                                           |
| timeMs                | time from receiving the request body till writing full response body                                      |

#### Authentication context and request headers in the access log

| Env variable                            | Description                                         | Example                                             |
|:----------------------------------------|:----------------------------------------------------|:----------------------------------------------------|
| ACCESS_LOG_SLF4J_DISABLED               | disable SLF4J access logging (default false)        | true                                                |
| ACCESS_LOG_AUTHN_CTX                    | authentication context set in access log (optional) | {"method":"authnMethod","user":"sub","uid":"email"} |
| ACCESS_LOG_REQUEST_HEADERS_ALL          | log all headers flag (default false)                | true                                                |
| ACCESS_LOG_REQUEST_HEADERS_WHITELIST    | log selected headers (optional)                     | ["Host","User-Agent"]                               |
| ACCESS_LOG_REQUEST_HEADERS_MASK_WHOLE   | mask whole logged header (optional)                 | ["Authorization"]                                   |
| ACCESS_LOG_REQUEST_HEADERS_MASK_PARTIAL | mask logged header partially (optional)             | ["Token"]                                           |

<a id="config-proxy-headers"></a>
### Proxy headers

Pyron applies following request headers modification (unless disabled):

* Add `remote-address.host` to `X-Forwarded-For` headers
* Add `remote-address.protocol` to `X-Forwarded-Proto` headers
* If `Host` header is set then add it to `X-Forwarded-Host` headers
* If True Client IP header is missing then set it to first `X-Forwarded-For` value
* Set True Client IP header to upstream service

| Env variable                      | Description                                            |
|:----------------------------------|:-------------------------------------------------------|
| PROXY_HEADERS_ENABLED             | enable proxy headers (default true)                    |
| INPUT_TRUE_CLIENT_IP_HEADER       | True Client IP header name (default X-Real-IP)         |
| OUTPUT_TRUE_CLIENT_IP_HEADER      | Outgoing True Client IP header name (default X-Real-IP)|

<a id="plugins"></a>
### Plugins

Plugins extend request-response flow, e.g. can enforce authorization rules, modify request or response, enhance access or audit logs, etc.

* [Authentication](docs/plugins/authn.md)
* [Request transformation](docs/plugins/transform-request.md)
* [CORS](docs/plugins/cors.md)
* [Brute-force protection](docs/plugins/bruteforce.md)
* [Rate limiting](docs/plugins/rate-limit.md)
* [ACP authorizer](docs/plugins/acp-authz.md)

Read about [plugins application](docs/plugins.md) in routing rules.

<a id="how-to"></a>
### How to

* [Read routing rules from Consul](docs/howtos/config-store-consul.md)
* [Read secrets from Vault](docs/howtos/config-store-vault.md)
* [Inject OAuth2 subject into request](docs/howtos/inject-oauth2-sub.md)
* [Configure SSL/TLS for ingress traffic with private key in Vault secret](docs/howtos/tls/http-server-tls-vault.md)
* [Configure SSL/TLS for ingress traffic with private key in environment variable or file](docs/howtos/tls/http-server-tls-env.md)

<a id="config-performance"></a>

## Performance

We have put Pyron Gateway under load to see how performant it is.

### Setup

* The test was run on a machine with i7-8550U CPU @ 1.80GHz (4 processing cores)
* `wrk` is used to generate load, a single test takes 30s and uses 10 threads
* target service is mocked with server responding to 140k req/sec with ~20 bytes response body

### Proxying request with no plugins

Pyron Gateway proxies requests to mocked target service without applying any plugins.

With no target service delay and 10 connections:

| Requests/sec | Latency avg | Latency Stdev | Latency p90 | Latency p99 |
|:-------------|:------------|:--------------|:------------|:------------|
| 21796        | 577.79	µs   | 774.63 µs     | 700.00 µs   | 4.57ms      |

With no target service delay and 30 connections:

| Requests/sec | Latency avg | Latency Stdev | Latency p90 | Latency p99 |
|:-------------|:------------|:--------------|:------------|:------------|
| 22692        | 1.42 ms     | 1.25 ms       | 2.34 ms     | 5.18 ms     |



### Proxying request with applying JWT-signing plugin

Pyron Gateway signs each request with empty JWT with symmetric key, puts the signature in the header and proxies request to mocked target service.

With no target service delay and 30 connections:

| Requests/sec | Latency avg | Latency Stdev | Latency p90 | Latency p99 |
|:-------------|:------------|:--------------|:------------|:------------|
| 12415        | 2.65 ms     | 3.33 ms       | 3.62 ms     | 9.27 ms     |



[cloudentity-logo]: docs/logo-3x.png
