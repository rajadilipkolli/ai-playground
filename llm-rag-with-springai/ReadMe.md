# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.learning.ai.llm-rag-with-springai' is invalid and this project uses 'com.learning.ai.llmragwithspringai' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.4/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.4/maven-plugin/reference/html/#build-image)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/docs/3.2.4/reference/html/features.html#features.testing.testcontainers)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [OpenAI](https://docs.spring.io/spring-ai/reference/api/clients/openai-chat.html)
* [Testcontainers](https://java.testcontainers.org/)
* [PGvector Vector Database](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
* [Spring Web](https://docs.spring.io/spring-boot/docs/3.2.4/reference/htmlsingle/index.html#web)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.2.4/reference/htmlsingle/index.html#actuator)
* [Prometheus](https://docs.spring.io/spring-boot/docs/3.2.4/reference/htmlsingle/index.html#actuator.metrics.export.prometheus)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/docs/3.2.4/reference/html/features.html#features.testing.testcontainers.at-development-time).

Testcontainers has been configured to use the following Docker images:

* [`pgvector/pgvector:pg16`](https://hub.docker.com/r/pgvector/pgvector)

Please review the tags of the used images and set them to the same as you're running in production.

