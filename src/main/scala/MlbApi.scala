package mlb

import com.github.tototoshi.csv._
import zio._
import zio.jdbc._
import zio.http._
import com.opencsv.CSVReader
import java.io.FileReader
import scala.collection.mutable.ListBuffer


object MlbApi extends ZIOAppDefault {
  // ...

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


  val insertGames: ZIO[ZConnectionPool, Throwable, UpdateResult] = transaction {
    readGameData.flatMap { games =>
      ZIO.foreach(games) { game =>
        insert(
          sql"INSERT INTO games(date, season, neutral, playoff, team1, team2, elo1Pre, elo2Pre, eloProb1, eloProb2, elo1Post, elo2Post, rating1Pre, rating2Pre, pitcher1, pitcher2, pitcher1Rgs, pitcher2Rgs, pitcher1Adj, pitcher2Adj, ratingProb1, ratingProb2, rating1Post, rating2Post, score1, score2)".values((
            game.date, 
            game.season, 
            game.neutral, 
            game.playoff,
            game.team1,
            game.team2,
            game.elo1Pre,
            game.elo2Pre,
            game.eloProb1,
            game.eloProb2,
            game.elo1Post,
            game.elo2Post,
            game.rating1Pre,
            game.rating2Pre,
            game.pitcher1,
            game.pitcher2,
            game.pitcher1Rgs,
            game.pitcher2Rgs,
            game.pitcher1Adj,
            game.pitcher2Adj,
            game.ratingProb1,
            game.ratingProb2,
            game.rating1Post,
            game.rating2Post,
            game.score1,
            game.score2))
        )
      }
    }
  }

  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create *> insertGames
    _ <- Server.serve(endpoints)
  } yield ()

  // ...
}
