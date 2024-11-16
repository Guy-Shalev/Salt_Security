# API Request Validation Service

[![Java](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A high-performance microservice for validating HTTP requests against predefined models

## 📋 Table of Contents

- [Features](#-features)
- [Technologies](#-technologies)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation-and-running)
- [Usage](#-usage)
- [API Endpoints](#-api-endpoints)
- [Testing](#-testing)
- [Database](#-database)
- [Design Decisions and Trade-offss](#-design-decisions-and-trade-offs)
- [License](#-license)

## ✨ Features

- Store and manage API models that define the expected structure of requests
- Validate incoming requests against these stored models in real-time
- Support for multiple parameter types
- Detailed validation error reporting

## 🛠 Technologies

- Java 11
- Spring Boot 2.x
- Spring Data JPA
- H2 Database (in-memory)
- JUnit 5
- Log4j2
- Lombok

## 🚀 Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Installation and Running 

1. Clone the repository:
   ```sh
   https://github.com/Guy-Shalev/Salt_security.git
   ```

2. Navigate to the project directory:
   ```sh
   cd Salt_security
   ```

3. Build the project:
   ```sh
   mvn clean install
   ```

4. Run the application:
   ```sh
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`.

## 🖥 Usage

Once the application is running, you can use tools like cURL, Postman, or any HTTP client to interact with the API endpoints at http://localhost:8080.

## 📡 API Endpoints

### POST /api/models

Stores model definitions for API endpoints. These models define the expected structure and types for requests.

### Request Body

```json
{
    "path": "/users/info",
    "method": "GET",
    "query_params": [
        {
            "name": "QueryParam1",
            "types": ["type1", "type2", "typeX"],
            "required": "Boolean"
        }
    ],
    "headers": [
        {
            "name": "HeaderParam1",
            "types": ["type1", "type2", "typeX"],
            "required": "Boolean"
        }
    ],
    "body": [
        {
            "name": "BodyParam1",
            "types": ["type1", "type2", "typeX"],
            "required": "Boolean"
        }
    ]
}
```

| Parameter    | Type   | Description                             |
|:-------------|:-------|:----------------------------------------|
| path         | string | Required. API endpoint path             |
| method       | string | Required. HTTP method (GET, POST, etc.) |
| query_params | array  | Query parameters specification          |
| headers      | array  | Header parameters specification         |
| body         | array  | Body parameters specification           |

### GET /api/models

Retrieves all stored API models.

### Response

```json
[
    {
        "path": "/users/info",
        "method": "GET",
        "query_params": [],
        "headers": [],
        "body": []
    }
]
```

### POST /api/validate

Validates an API request against previously stored models.

### Request Body

```json
{
    "path": "/users/info",
    "method": "GET",
    "query_params": [
        {
            "name": "with_extra_data",
            "value": false
        }
    ],
    "headers": [
        {
            "name": "Authorization",
            "value": "Bearer abc123"
        }
    ],
    "body": [
        {
            "name": "order_type",
            "value": "4"
        }
    ]
}
```

### Response

```json
{
    "valid": true,
    "anomalies": {}
}
```

Or in case of validation errors:

```json
{
    "valid": false,
    "anomalies": {
        "header.Authorization": "Required parameter is missing",
        "query.user_id": "Value does not match any of the allowed types: Int, UUID"
    }
}
```

### Supported Types

| **Type**       | **Description**                 | **Example**                                |
|------------|-----------------------------|----------------------------------------|
| Int        | Integer numbers             | 42                                     |
| String     | Text values                 | "example"                              |
| Boolean    | True/false values           | true                                   |
| List       | Array of values             | [1, 2, 3]                              |
| Date       | Date in dd-mm-yyyy format   | "25-12-2024"                           |
| Email      | Valid email addresses       | "user@example.com"                     |
| UUID       | Universal Unique Identifier | "123e4567-e89b-12d3-a456-426614174000" |
| Auth-Token | Bearer authentication token | "Bearer abc123"                        |

## 🧪 Testing

```sh
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

## 💾 Database

- The application uses an H2 in-memory database.
- H2 console is enabled and can be accessed at `http://localhost:8080/h2-console` when the application is running.
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

## 🔮 Design Decisions and Trade-offs

### Database Solution

#### Current Implementation (H2)

* Used for development and testing
* Provides quick setup and validation of concepts
* No production deployment concerns

#### Production Options Analysis

|   **Aspect**   |                                                            **SQL**                                                                        |                                                  NoSQL                                                           |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Data Structure | ✅ Fixed schema suits our model definitions<br>✅ Strong relationships between models and parameters<br>❌ Schema changes require migrations | ✅ Flexible schema for evolving models<br>✅ Easy to store nested parameters<br>❌ Less control over data structure |
| Queries        | ✅ Complex queries for validation rules<br>✅ ACID compliance<br>❌ More complex joins needed                                                | ✅ Simple queries for model lookup<br>✅ Better for document-based queries<br>❌ Limited transaction support        |
| Performance    | ✅ Optimized for relations<br>✅ Efficient with proper indexing<br>❌ Join operations overhead                                               | ✅ Faster for single-document operations<br>✅ Better horizontal scaling<br>❌ Less efficient for related data      |
| Scaling        | ✅ Vertical scaling<br>✅ Read replicas<br>❌ Complex sharding                                                                               | ✅ Easy horizontal scaling<br>✅ Built-in sharding<br>❌ Eventually consistent                                      |

### Recommendation: SQL 

#### Why SQL?

1. Data Structure
  * Models have a well-defined, consistent structure
  * Strong relationships between models and parameters
  * Schema validation at database level

2. Query Requirements
  * Need for complex validation queries
  * ACID compliance for model updates
  * Transaction support for batch operations

3. Performance Needs
  * Efficient querying with indexes
  * Predictable query performance
  * Optimization possibilities

### NoSQL Considerations
Would be better if:
* Models were more dynamic
* Schema changed frequently
* Needed faster horizontal scaling
* Had less structured data

### Implementation Impact
SQL Advantages for Our Use Case:
* Enforces data integrity
* Better for related data queries
* Strong consistency
* Clear data modeling

### Key Performance Optimizations:
* Indexing on path/method
* Connection pooling
* Query optimization
* Caching strategy

This decision prioritizes:
* Data consistency
* Query flexibility
* Strong relationships
* Schema validation
* Future maintainability

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

---

For any additional questions or concerns, please open an issue or contact the repository maintainers.  
