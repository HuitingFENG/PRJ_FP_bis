import zio._
import zio.jdbc._
import com.github.tototoshi.csv.CSVReader
import scala.collection.mutable.ListBuffer

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


object MlbDatabase extends MlbApi {

  def initializeDatabase: ZIO[ZConnectionPool, Throwable, Unit] =
    for {
      _ <- createTableIfNotExists
      _ <- loadGameDataIntoTable
    } yield ()

  private def createTableIfNotExists: ZIO[ZConnectionPool, Throwable, Unit] =
    transaction {
      execute(
        sql"""
          CREATE TABLE IF NOT EXISTS games (
            date VARCHAR(10) NOT NULL,
            season INT NOT NULL,
            neutral BOOLEAN NOT NULL,
            playoff BOOLEAN NOT NULL,
            team1 VARCHAR(50) NOT NULL,
            team2 VARCHAR(50) NOT NULL,
            elo1_pre DOUBLE NOT NULL,
            elo2_pre DOUBLE NOT NULL,
            elo_prob1 DOUBLE NOT NULL,
            elo_prob2 DOUBLE NOT NULL,
            elo1_post DOUBLE NOT NULL,
            elo2_post DOUBLE NOT NULL,
            rating1_pre DOUBLE NOT NULL,
            rating2_pre DOUBLE NOT NULL,
            pitcher1 VARCHAR(50) NOT NULL,
            pitcher2 VARCHAR(50) NOT NULL,
            pitcher1_rgs DOUBLE NOT NULL,
            pitcher2_rgs DOUBLE NOT NULL,
            pitcher1_adj DOUBLE NOT NULL,
            pitcher2_adj DOUBLE NOT NULL,
            rating_prob1 DOUBLE NOT NULL,
            rating_prob2 DOUBLE NOT NULL,
            rating1_post DOUBLE NOT NULL,
            rating2_post DOUBLE NOT NULL,
            score1 INT NOT NULL,
            score2 INT NOT NULL
          )
        """
      )
    }

  private def loadGameDataIntoTable: ZIO[ZConnectionPool, Throwable, Unit] =
    for {
      gameDataList <- readGameDataFromCSV
      _ <- insertGameData(gameDataList)
    } yield ()

  private def readGameDataFromCSV: ZIO[Any, Throwable, List[GameData]] =
    ZIO.succeed {
      val csvFilePath: String = "mlb_elo.csv"
      val reader = CSVReader.open(csvFilePath)
      val gameDataList = reader
        .all()
        .tail // Skip header row
        .map { values =>
          GameData(
            date = values(0),
            season = values(1).toInt,
            neutral = values(2).toBoolean,
            playoff = values(3).toBoolean,
            team1 = values(4),
            team2 = values(5),
            elo1_pre = values(6).toDouble,
            elo2_pre = values(7).toDouble,
            elo_prob1 = values(8).toDouble,
            elo_prob2 = values(9).toDouble,
            elo1_post = values(10).toDouble,
            elo2_post = values(11).toDouble,
            rating1_pre = values(12).toDouble,
            rating2_pre = values(13).toDouble,
            pitcher1 = values(14),
            pitcher2 = values(15),
            pitcher1_rgs = values(16).toDouble,
            pitcher2_rgs = values(17).toDouble,
            pitcher1_adj = values(18).toDouble,
            pitcher2_adj = values(19).toDouble,
            rating_prob1 = values(20).toDouble,
            rating_prob2 = values(21).toDouble,
            rating1_post = values(22).toDouble,
            rating2_post = values(23).toDouble,
            score1 = values(24).toInt,
            score2 = values(25).toInt
          )
        }
      reader.close()
      gameDataList
    }

  private def insertGameData(gameDataList: List[GameData]): ZIO[ZConnectionPool, Throwable, UpdateResult] =
    transaction {
      ZIO.foreach(gameDataList) { gameData =>
        insert(
          sql"INSERT INTO games VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            .values(
              gameData.date,
              gameData.season,
              gameData.neutral,
              gameData.playoff,
              gameData.team1,
              gameData.team2,
              gameData.elo1_pre,
              gameData.elo2_pre,
              gameData.elo_prob1,
              gameData.elo_prob2,
              gameData.elo1_post,
              gameData.elo2_post,
              gameData.rating1_pre,
              gameData.rating2_pre,
              gameData.pitcher1,
              gameData.pitcher2,
              gameData.pitcher1_rgs,
              gameData.pitcher2_rgs,
              gameData.pitcher1_adj,
              gameData.pitcher2_adj,
              gameData.rating_prob1,
              gameData.rating_prob2,
              gameData.rating1_post,
              gameData.rating2_post,
              gameData.score1,
              gameData.score2
            )
        )
      }
    }

  def getGameHistory: ZIO[ZConnectionPool, Throwable, List[GameData]] =
    transaction {
      ZIO.foreachPar(readGameDataQuery)(execute(_).map(_ => ()))
        .flatMap(_ => selectAllGames)
    }

  private def readGameDataQuery: ZIO[Any, Throwable, PreparedStatement] =
    prepareStatement(
      """
      SELECT * FROM games
      """
    )

  private def selectAllGames: ZIO[ZConnectionPool, Throwable, List[GameData]] =
    ZIO.fromPreparedStatement(selectAllGamesQuery)(rs => readGameDataFromResultSet(rs))

  private def selectAllGamesQuery: ZIO[Any, Throwable, PreparedStatement] =
    prepareStatement(
      """
      SELECT * FROM games
      """
    )

  private def readGameDataFromResultSet(rs: ResultSet): ZIO[Any, Throwable, List[GameData]] =
    ZIO.effectTotal {
      val gameDataList = new ListBuffer[GameData]()
      while (rs.next()) {
        val gameData = GameData(
          date = rs.getString("date"),
          season = rs.getInt("season"),
          neutral = rs.getBoolean("neutral"),
          playoff = rs.getBoolean("playoff"),
          team1 = rs.getString("team1"),
          team2 = rs.getString("team2"),
          elo1_pre = rs.getDouble("elo1_pre"),
          elo2_pre = rs.getDouble("elo2_pre"),
          elo_prob1 = rs.getDouble("elo_prob1"),
          elo_prob1 = rs.getDouble("elo_prob1"),
          elo_prob2 = rs.getDouble("elo_prob2"),
          elo1_post = rs.getDouble("elo1_post"),
          elo2_post = rs.getDouble("elo2_post"),
          rating1_pre = rs.getDouble("rating1_pre"),
          rating2_pre = rs.getDouble("rating2_pre"),
          pitcher1 = rs.getString("pitcher1"),
          pitcher2 = rs.getString("pitcher2"),
          pitcher1_rgs = rs.getDouble("pitcher1_rgs"),
          pitcher2_rgs = rs.getDouble("pitcher2_rgs"),
          pitcher1_adj = rs.getDouble("pitcher1_adj"),
          pitcher2_adj = rs.getDouble("pitcher2_adj"),
          rating_prob1 = rs.getDouble("rating_prob1"),
          rating_prob2 = rs.getDouble("rating_prob2"),
          rating1_post = rs.getDouble("rating1_post"),
          rating2_post = rs.getDouble("rating2_post"),
          score1 = rs.getInt("score1"),
          score2 = rs.getInt("score2")
        )
        gameDataList += gameData
      }
      gameDataList.toList
    }
}
