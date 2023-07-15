# MLB API Application

The MLB API application is a Scala project that provides functionality to interact with MLB game data. It allows you to initialize a database, insert game data, load game data, and predict game outcomes.

## Authors 

Mohammed Saber BELLAAIRI -
Huiting FENG -
Bouthayna ATIK -
Camille FOUR

## Functionality

The MLB API application provides the following functionality:

- Create a database table to store MLB game data.
- Insert rows of game data into the database.
- Load all games from a CSV file.
- Select a specific game based on the game ID.
- Predict the outcome of a game based on custom logic.

## Usage

To use the MLB API application, follow these steps:

1. Make sure you have Scala 3 installed on your system. If not, you can update the Scala 3 version by running the following command:

   ```shell
   brew upgrade scala3
   ```

2. Clone the MLB API repository to your local machine.

3. Open a terminal and navigate to the root directory of the project.

4. Run the following command to compile and run the tests:

   ```shell
   sbt test
   ```

   Note: Make sure you have all the required dependencies mentioned in the `build.sbt` file.

5. Once the tests pass successfully, you can run the application by executing the following command:

   ```shell
   sbt run
   ```

   This will start the MLB API server.

6. You can now interact with the API by sending HTTP requests to the available endpoints. Here are some examples:

   - Initialize the database:
     ```shell
     curl http://localhost:8080/init
     ```

   - Load all games:
     ```shell
     curl http://localhost:8080/games
     ```

   - Predict the outcome of a specific game:
     ```shell
     curl http://localhost:8080/predict/game/{gameId}
     ```

     Replace `{gameId}` with the ID of the desired game.

7. The API responses will be displayed in the terminal.

## External Libraries Used

The MLB API application utilizes the following external libraries:

- ZIO: Provides a functional programming toolkit for building asynchronous and concurrent applications.
- ZIO-HTTP: Offers a type-safe, purely functional HTTP server and client.
- ZIO-JDBC: Provides a pure functional interface to JDBC.
- Argonaut: Offers JSON parsing and serialization capabilities.
- Scala CSV: Provides utilities for reading and writing CSV files.
- MUnit: A testing framework for Scala.

