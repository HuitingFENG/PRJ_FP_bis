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
  // ...
  
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

  val create: ZIO[ZConnectionPool, Throwable, Unit] = ZIO.environment[ZConnectionPool] { connectionPool =>
    connectionPool.get.flatMap { connection =>
      val createTableSql =
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

      execute(createTableSql).provide(connection)
    }
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


  val insertRows: (GameData, String) => ZIO[Any, Throwable, Unit] = (gameData, csvFilePath) => ZIO.succeed {
    val writer = CSVWriter.open(new java.io.File(csvFilePath), append = true)
    val values = Seq(
      gameData.date,
      gameData.season.toString,
      gameData.neutral.toString,
      gameData.playoff.toString,
      gameData.team1,
      gameData.team2,
      gameData.elo1Pre.toString,
      gameData.elo2Pre.toString,
      gameData.eloProb1.toString,
      gameData.eloProb2.toString,
      gameData.elo1Post.toString,
      gameData.elo2Post.toString,
      gameData.rating1Pre.toString,
      gameData.rating2Pre.toString,
      gameData.pitcher1,
      gameData.pitcher2,
      gameData.pitcher1Rgs.toString,
      gameData.pitcher2Rgs.toString,
      gameData.pitcher1Adj.toString,
      gameData.pitcher2Adj.toString,
      gameData.ratingProb1.toString,
      gameData.ratingProb2.toString,
      gameData.rating1Post.toString,
      gameData.rating2Post.toString,
      gameData.score1.toString,
      gameData.score2.toString
    )
    writer.writeRow(values)
    writer.close()
  }



  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create *> insertRows
    _ <- Server.serve(endpoints)
  } yield ()





  // ...
}
