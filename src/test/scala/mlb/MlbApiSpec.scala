import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.test.mock.Expectation._
import zio.test.mock._

object MlbApiSpec extends DefaultRunnableSpec {

  def spec: ZSpec[Environment, Failure] =
    suite("MlbApiSpec")(
      testM("initialize database") {
        for {
          _ <- MlbDatabase.initializeDatabase.either
        } yield assertCompletes
      },
      testM("insert rows from CSV") {
        val gameDataList = List(
          GameData("2023-01-01", 2023, false, false, "Team A", "Team B", 1500.0, 1500.0, 0.5, 0.5, 1500.0, 1500.0, 1500.0, 1500.0, "Pitcher A", "Pitcher B", 5.0, 5.0, 0.0, 0.0, 0.5, 0.5, 1500.0, 1500.0, 5, 5),
          GameData("2023-01-02", 2023, false, false, "Team C", "Team D", 1600.0, 1400.0, 0.6, 0.4, 1600.0, 1400.0, 1600.0, 1400.0, "Pitcher C", "Pitcher D", 6.0, 4.0, 0.0, 0.0, 0.6, 0.4, 1600.0, 1400.0, 6, 4)
        )
        for {
          result <- MlbDatabase.insertRows(gameDataList).either
        } yield assert(result)(isRight)
      },
      testM("retrieve game history") {
        val expectedGameDataList = List(
          GameData("2023-01-01", 2023, false, false, "Team A", "Team B", 1500.0, 1500.0, 0.5, 0.5, 1500.0, 1500.0, 1500.0, 1500.0, "Pitcher A", "Pitcher B", 5.0, 5.0, 0.0, 0.0, 0.5, 0.5, 1500.0, 1500.0, 5, 5),
          GameData("2023-01-02", 2023, false, false, "Team C", "Team D", 1600.0, 1400.0, 0.6, 0.4, 1600.0, 1400.0, 1600.0, 1400.0, "Pitcher C", "Pitcher D", 6.0, 4.0, 0.0, 0.0, 0.6, 0.4, 1600.0, 1400.0, 6, 4)
        )
        for {
          result <- MlbDatabase.getGameHistory.either
        } yield assert(result)(isRight(equalTo(expectedGameDataList)))
      },
      testM("predict next game") {
        val gameDataList = List(
          GameData("2023-01-01", 2023, false, false, "Team A", "Team B", 1500.0, 1500.0, 0.5, 0.5, 1500.0, 1500.0, 1500.0, 1500.0, "Pitcher A", "Pitcher B", 5.0, 5.0, 0.0, 0.0, 0.5, 0.5, 1500.0, 1500.0, 5, 5),
          GameData("2023-01-02", 2023, false, false, "Team C", "Team D", 1600.0, 1400.0, 0.6, 0.4, 1600.0, 1400.0, 1600.0, 1400.0, "Pitcher C", "Pitcher D", 6.0, 4.0, 0.0, 0.0, 0.6, 0.4, 1600.0, 1400.0, 6, 4)
        )
        val expectedPrediction = "Team A"
        for {
          _ <- MlbDatabase.getGameHistory.returns(gameDataList)
          result <- MlbPredictor.predictNextGame.either
        } yield assert(result)(isRight(equalTo(expectedPrediction)))
      }
    )
}
