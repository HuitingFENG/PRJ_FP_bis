# Major League Baseball API

This project is a REST API that provides access to the "Major League Baseball Dataset" from Kaggle. It leverages the power of Scala 3 and the ZIO framework to build a functional and scalable backend application. The API allows users to retrieve game history, make predictions for future games, and initialize the underlying H2 database engine.

## Authors 

Mohammed Saber BELLAAIRI -
Huiting FENG -
Bouthayna ATIK -
Camille FOUR


## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Data Structures](#data-structures)
- [Dependencies](#dependencies)
- [Database Initialization](#database-initialization)
- [Endpoints](#endpoints)
- [Testing](#testing)

## Prerequisites

Before running the API, make sure you have the following installed:

- Java Development Kit (JDK) 11 or higher
- Scala 3
- sbt (Scala Build Tool)

## Project Structure

The project consists of several files organized in the following structure:

- `build.sbt`: Configuration file for sbt build tool.
- `src/`
  - `main/`
    - `scala/`
      - `mlb/`
        - `MlbApi.scala`: Main entry point of the application. It initializes the database, defines API endpoints, and configures the ZIO environment.
        - `MlbDatabase.scala`: Contains functions for initializing the database, inserting data from the CSV file, and retrieving game history.
        - `MlbPredictor.scala`: Implements the logic for making predictions based on game history data.
  - `test/`
    - `scala/`
      - `mlb/`
        - `MlbApiSpec.scala`: Test suite to validate the functionality of the API.

## Data Structures

The application defines the following data structures to represent games and related entities:

```scala
case class GameData(
  date: String,
  season: Int,
  neutral: Boolean,
  playoff: Boolean,
  team1: String,
  team2: String,
  elo1Pre: Double,
  elo2Pre: Double,
  eloProb1: Double,
  eloProb2: Double,
  elo1Post: Double,
  elo2Post: Double,
  rating1Pre: Double,
  rating2Pre: Double,
  pitcher1: String,
  pitcher2: String,
  pitcher1Rgs: Double,
  pitcher2Rgs: Double,
  pitcher1Adj: Double,
  pitcher2Adj: Double,
  ratingProb1: Double,
  ratingProb2: Double,
  rating1Post: Double,
  rating2Post: Double,
  score1: Int,
  score2: Int
)
```

The `GameData` case class represents a single game and contains various attributes such as date, season, team names, ratings, scores, etc.

## Dependencies

The following dependencies are used in the project:

- **H2 Database (com.h2database:h2)**: H2 is an open-source in-memory database. It provides a fast and reliable database solution for development and testing purposes.

- **ZIO (dev.zio:zio)**: ZIO is a powerful, type-safe, and composable library for asynchronous and concurrent programming in Scala. It provides a functional programming approach to building concurrent and effectful applications.

- **ZIO Streams (dev.zio:zio-streams)**: ZIO Streams is a library that enables streaming and processing of data in a purely functional way. It provides abstractions for working with streams of data and performing various operations on them.

- **ZIO Schema (dev.zio:zio-schema)**: ZIO Schema is a library that helps in building type-safe schemas and validating data structures. It provides a way to define complex data structures with type-level guarantees and perform validation on them.

- **ZIO JDBC (dev.zio:zio-jdbc)**: ZIO JDBC is a library that integrates ZIO with JDBC (Java Database Connectivity). It provides utilities for working with databases using JDBC, allowing you to perform database operations in a purely functional way.

- **ZIO JSON (dev.zio:zio-json)**: ZIO JSON is a library for JSON (JavaScript Object Notation) parsing and encoding. It provides facilities for parsing JSON data into Scala data structures and encoding Scala data structures into JSON.

- **ZIO HTTP (dev.zio:zio-http)**: ZIO HTTP is a library for building HTTP clients and servers using ZIO. It provides a type-safe and purely functional way to interact with HTTP-based APIs.

- **OpenCSV (com.opencsv:opencsv)**: OpenCSV is a library for reading and writing CSV (Comma-Separated Values) files. It provides utilities for parsing CSV data and converting it into structured formats.

- **ZIO Interop Cats (dev.zio:zio-interop-cats)**: ZIO Interop Cats is a library that enables interoperability between ZIO and Cats, which is another popular functional programming library in Scala. It allows you to use Cats data types and functions alongside ZIO.

- **Scala CSV (com.github.tototoshi:scala-csv)**: Scala CSV is a library for reading and writing CSV files in Scala. It provides utilities for parsing CSV data and working with tabular data structures.

These dependencies are crucial for different aspects of the application, such as database connectivity, data processing, JSON handling, HTTP communication, and CSV file operations. They enable the implementation of the desired functionality outlined in the project requirements.

## Database Initialization

The database is initialized at the startup of the application. This is achieved by calling the `initializeDatabase` function in the `MlbDatabase` object. It creates the necessary table schema using SQL statements and ensures that the database is ready for use.

Alternatively, a dedicated endpoint can be created in the REST API to trigger the database initialization process. This approach allows for separate control over database initialization and provides flexibility in managing the application lifecycle.

## Endpoints

The API provides the following endpoints:

- `GET /games`: Retrieves the game history, returning a list of `GameData` objects representing past games.
- `GET /predict/game/:gameId`: Makes a prediction for the next game based on the provided `gameId`. The predicted outcome is returned as a string representing the team name.

These endpoints allow users to interact with the MLB dataset, access game history, and make predictions for future games. The API is designed to be user-friendly and provide relevant information to enhance user experience.

## Testing

The project includes a test suite, `MlbApiSpec`, which validates the functionality of the API. The test suite covers various scenarios, including initializing the database, inserting rows from CSV, retrieving game history, and predicting the next game. It utilizes the ZIO Test framework to write and execute test cases, ensuring the correctness of the implemented functionality.

To run the tests, execute the following command in the project root directory:

```
sbt test
```

The tests should pass, confirming that the application behaves as expected and meets the defined requirements.

## Git Repository and Documentation Quality

The project source code is managed using Git, providing version control and collaboration capabilities. The repository is well-organized, with appropriate commits and a clear README file, enabling others to understand and contribute to the project.

The codebase is thoroughly documented, including class and method-level comments that explain the purpose and functionality of each component. This documentation enhances code readability and maintainability, making it easier for developers to understand and modify the codebase.

## Conclusion

The Major League Baseball API provides a powerful backend for accessing and interacting with the MLB dataset. Leveraging the functional programming capabilities of Scala 3 and the ZIO framework, the application demonstrates the use of immutable data structures, database operations, CSV processing, and REST API design.

By adhering to functional programming principles, the application promotes code modularity, maintainability, and testability. The comprehensive test suite validates the functionality of the API and ensures a robust and reliable system.

The project repository, with its well-structured code, commits, and documentation, reflects a commitment to software development best practices and high-quality documentation.
