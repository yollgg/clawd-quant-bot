# Agent Software Development Skill

This skill defines the mandatory standard for software development by this agent.

## Core Mandates

1.  **Persistence by Default**: Never use in-memory lists for critical data like trade history or account balance. Always use a database (H2, SQLite, or PostgreSQL) with JPA/Hibernate.
2.  **No Mocks in Production-Ready Code**: Avoid `Math.random()` for market prices once an API is available. Use real data from the first day.
3.  **Complete Implementation**: Never leave "TODO" or "Mock" comments in files that are presented as "finished".
4.  **Database Migration**: Always include `application.properties` and Entity definitions when starting a Spring Boot project.

## Workflow

1.  **Requirement Analysis**: Confirm if the user wants a prototype or a production-ready logic.
2.  **Infrastructure First**: Set up the database, logging, and real API clients before writing business logic.
3.  **Atomic Commits**: Push logical chunks of code to GitHub (e.g., "Add Persistence Layer", "Implement Real API").
4.  **Verification**: Always run the code (e.g., `mvn spring-boot:run`) and check the output before reporting status to the user.
