package mlb

import com.github.tototoshi.csv._
import zio._
import zio.jdbc._
import zio.http._
import com.opencsv.CSVReader
import java.io.FileReader
import scala.collection.mutable.ListBuffer
import zio.jdbc.JdbcEncoder
import zio.interop.catz._
import java.sql.PreparedStatement
import zio.jdbc.JdbcEncoder._



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


object MlbApi extends ZIOAppDefault {
  
  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig] =
    ZLayer.succeed(ZConnectionPoolConfig.default)

  val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool: ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.h2mem(
      database = "testdb",
      props = properties
    )

  val create: ZIO[ZConnectionPool, Throwable, Unit] = transaction {
    execute(
      sql"""
        CREATE TABLE IF NOT EXISTS games (
          date VARCHAR(10) NOT NULL,
          season INT NOT NULL,
          neutral BOOLEAN NOT NULL,
          playoff BOOLEAN NOT NULL,
          team1 VARCHAR(50) NOT NULL,
          team2 VARCHAR(50) NOT NULL,
          elo1Pre DOUBLE NOT NULL,
          elo2Pre DOUBLE NOT NULL,
          eloProb1 DOUBLE NOT NULL,
          eloProb2 DOUBLE NOT NULL,
          elo1Post DOUBLE NOT NULL,
          elo2Post DOUBLE NOT NULL,
          rating1Pre DOUBLE NOT NULL,
          rating2Pre DOUBLE NOT NULL,
          pitcher1 VARCHAR(50) NOT NULL,
          pitcher2 VARCHAR(50) NOT NULL,
          pitcher1Rgs DOUBLE NOT NULL,
          pitcher2Rgs DOUBLE NOT NULL,
          pitcher1Adj DOUBLE NOT NULL,
          pitcher2Adj DOUBLE NOT NULL,
          ratingProb1 DOUBLE NOT NULL,
          ratingProb2 DOUBLE NOT NULL,
          rating1Post DOUBLE NOT NULL,
          rating2Post DOUBLE NOT NULL,
          score1 INT NOT NULL,
          score2 INT NOT NULL
        )
      """
    )
  }

  val csvFilePath: String = "mlb_elo.csv"

  val readGameData: ZIO[Any, Throwable, List[GameData]] =
  ZIO.succeed {
    val reader = new CSVReader(new FileReader(csvFilePath))
    val iterator = reader.iterator()
    val gameDataList = new ListBuffer[GameData]()
    while (iterator.hasNext) {
      val values = iterator.next()
      val gameData = GameData(
        date = values(0),
        season = values(1).toInt,
        neutral = values(2).toBoolean,
        playoff = values(3).toBoolean,
        team1 = values(4),
        team2 = values(5),
        elo1Pre = values(6).toDouble,
        elo2Pre = values(7).toDouble,
        eloProb1 = values(8).toDouble,
        eloProb2 = values(9).toDouble,
        elo1Post = values(10).toDouble,
        elo2Post = values(11).toDouble,
        rating1Pre = values(12).toDouble,
        rating2Pre = values(13).toDouble,
        pitcher1 = values(14),
        pitcher2 = values(15),
        pitcher1Rgs = values(16).toDouble,
        pitcher2Rgs = values(17).toDouble,
        pitcher1Adj = values(18).toDouble,
        pitcher2Adj = values(19).toDouble,
        ratingProb1 = values(20).toDouble,
        ratingProb2 = values(21).toDouble,
        rating1Post = values(22).toDouble,
        rating2Post = values(23).toDouble,
        score1 = values(24).toInt,
        score2 = values(25).toInt
      )
      gameDataList += gameData
    }   

    reader.close()

    gameDataList.toList
  }

  

  val insertRows: ZIO[ZConnectionPool, Throwable, UpdateResult] = for {
    gameDataList <- readGameData
    updateResults <- ZIO.foreach(gameDataList) { gameData =>
      transaction {
        insert(
          sql"INSERT INTO games VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            .values(
              gameData.date,
              gameData.season,
              gameData.neutral,
              gameData.playoff,
              gameData.team1,
              gameData.team2,
              gameData.elo1Pre,
              gameData.elo2Pre,
              gameData.eloProb1,
              gameData.eloProb2,
              gameData.elo1Post,
              gameData.elo2Post,
              gameData.rating1Pre,
              gameData.rating2Pre,
              gameData.pitcher1,
              gameData.pitcher2,
              gameData.pitcher1Rgs,
              gameData.pitcher2Rgs,
              gameData.pitcher1Adj,
              gameData.pitcher2Adj,
              gameData.ratingProb1,
              gameData.ratingProb2,
              gameData.rating1Post,
              gameData.rating2Post,
              gameData.score1,
              gameData.score2,
            )
        )

      }
    }
    updateResult = updateResults.reduceOption(_ zipWith _)(_ + _).getOrElse(UpdateResult.Nothing)
  } yield updateResult

  val endpoints: Http[Any, Throwable, Request, Response[Throwable, _]] =
    Http
      .collect[Request] {
        case Method.GET -> Root / "init" => create.toResponse
        case Method.GET -> Root / "games" => retrieveGameHistory.toResponse
        case Method.GET -> Root / "predict" / "game" / gameId => predictGame(gameId).toResponse
      }
      .withDefaultErrorResponse

  private def create: ZIO[ZConnectionPool, Throwable, Unit] =
    MlbDatabase.initializeDatabase

  private def retrieveGameHistory: ZIO[ZConnectionPool, Throwable, List[GameData]] =
    MlbDatabase.getGameHistory

  private def predictGame(gameId: String): ZIO[ZConnectionPool, Throwable, GamePrediction] =
    MlbPredictor.predictGame(gameId)

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(createZIOPoolConfig >>> connectionPool, Server.default)

  private def toResponse[A: HttpCodec](zio: ZIO[Any, Throwable, A]): ZIO[Any, Throwable, Response[Throwable, A]] =
    zio.foldM(
      error => ZIO.succeed(Response.fail(error)),
      result => ZIO.succeed(Response.succeed(result))
    )

  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create *> insertRows
    _ <- Server.serve(endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] = app.provide(createZIOPoolConfig >>> connectionPool, Server.default)

  def main(args: Array[String]): Unit = {
    MlbDatabase.getGameHistory
    MlbPredictor.predictGame("game123")
  }
}
}
