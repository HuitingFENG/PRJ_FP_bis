import com.opencsv.CSVReader
import zio._
import zio.test.Assertion._
import zio.test._

object MlbApiSpec {
  override def spec: ZSpec[Environment, Failure] =
    suite("MlbApiSpec")(
      testM("readGameData should read data from CSV file") {
        val csvFilePath: String = "mlb_elo.csv"

        val expectedData = List(
            GameData("2022-01-01", 2024, true, false, "Team 1", "Team 2", 100.0, 200.0, 0.6, 0.4, 150.0, 250.0, 120.0, 180.0, "Pitcher 1", "Pitcher 2", 7.5, 8.2, 0.5, 0.3, 0.7, 0.2, 110.0, 190.0, 5, 3),
            GameData("2022-01-02", 2024, false, false, "Team 3", "Team 4", 300.0, 400.0, 0.8, 0.2, 350.0, 450.0, 280.0, 380.0, "Pitcher 3", "Pitcher 4", 6.8, 7.9, 0.4, 0.2, 0.6, 0.3, 320.0, 420.0, 7, 2)
        )

        val result = ZIO.effectTotal {
          val reader = new CSVReader(new FileReader(csvFilePath))
          val header = reader.readNext() // Skip header row
          val rows = Iterator.continually(reader.readNext()).takeWhile(_ != null)
          rows.map(GameData.fromCsvRow).toList
        }

        assertM(result)(equalTo(expectedData))
      }
    )
}
