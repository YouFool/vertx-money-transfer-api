# Revolut Challange


Design and implement a **RESTful API** (including data model and the backing implementation) for
**money transfers between accounts.**

## Explicit requirements

1. You can use Java or Kotlin
2. Keep it simple and to the point (e.g. no need to implement any authentication).
3.  Assume the API is invoked by multiple systems and services on behalf of end users.
4. You can use frameworks/libraries if you like **(except Spring)**, but don't forget about
requirement #2 and keep it simple and avoid heavy frameworks.
5. The datastore should run in-memory for the sake of this test.
6.  The final result should be executable as a standalone program (should not require a
pre-installed container/server).
7. Demonstrate with tests that the API works as expected.

## Implicit requirements

1. The code produced by you is expected to be of high quality.
2. There are no detailed requirements, use common sense

## Stack
* Java 8
* Maven
* Vert.x (Core, Config, Web, JdbcClient, VertxUnit)
* H2 In-Memory Database
* Log4J2

## Assumptions made
* A transaction must **always** happen between two accounts
* The transfer will not be successful if **sender's balance < amount** 
* The API supports `application/json` content-type only 

## Get Started

**You should have Java 8 + JDK and maven installed to build the project**

1. Clone this repository
2. Open the terminal and execute `mvn package` in the project root to build it
3. Run the "fat jar" with `java -jar target/vertx-money-transfer-api-1.0-SNAPSHOT.jar`
4. The HTTP server will be served at `localhost:8080` by default. (You can change the port in the `config.json` file).
5. Open your REST client and let's call the API's! (You can import this project postman collection too)


    Represents an account
    entity account : {
         id : uuid?
         balance: double
    }
    
    Represents a transaction
    entity transaction : {
        	id: uuid?
        	from: account
        	to: account
        	amount: double
    }
    
    GET /api/account -> Get all accounts 
    GET /api/account/:id -> Get account by id 
    
    POST /api/transfer ->
        input : transaction
        output : transaction

---
