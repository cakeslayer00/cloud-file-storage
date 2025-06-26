# cloud-file-storage

pet-project that is replica of google drive on minimums or other storage services. Ultimate goal to understand spring boot, spring security, to learn to work with NoSQL, Docker etc.

# Table of content

[Technologies](#used-technologies)

[How to install](#how-to-install-and-run)

[Credits](#credits)

# Used technologies

- Spring Boot
- Spring Security
- Spring Data JPA
- Liquibase
- Redis
- PostgreSQL
- Testcontainers

# How to install and run

1. Simply clone repository using
2. Enter directory `cd cloud-file-storage`
3. Fill .env file with your own credentials or user default data
4. Execute docker command `docker compose up --build`, assuming you have docker installed
4. Application will be available on `localhost`

For the development use `compose-dev.yaml`

# Credits

This project implemented based on requirements provided by Sergey Zhukov's roadmap.
Here's link:
https://zhukovsd.github.io/java-backend-learning-course/projects/currency-exchange/
