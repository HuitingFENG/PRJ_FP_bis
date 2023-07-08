import zio._
import zio.test.Assertion._
import zio.test._

object MlbApiSpec {
  override def spec: ZSpec[Environment, Failure] =
    suite("MlbApiSpec")(
      // ... existing test cases

      testM("insertGames should insert values into CSV file") {
        val csvFilePath: String = "test_mlb_elo.csv"

        val gameData = GameData("2022-01-01", 2024, true, false, "Team 1", "Team 2", 100.0, 200.0, 0.6, 0.4, 150.0, 250.0, 120.0, 180.0, "Pitcher 1", "Pitcher 2", 7.5, 8.2, 0.5, 0.3, 0.7, 0.2, 110.0, 190.0, 5, 3)

        val result = for {
          _ <- insertGames(csvFilePath, List(gameData))
          rows <- ZIO.effectTotal {
            val reader = new CSVReader(new FileReader(csvFilePath))
            val header = reader.readNext() // Skip header row
            Iterator.continually(reader.readNext()).takeWhile(_ != null).toList
          }
        } yield rows

        assertM(result)(equalTo(List(gameData.toCsvRow)))
      }
    )
}
