package mlb

import zio._
import zio.Console._
import zio.stream._
import zio.http._
import zio.http.HttpApp
import zio.http.Response
import zio.http.Request
import zio.http.Method
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.jdbc._
import com.opencsv.CSVReader
import java.io.FileReader
import scala.jdk.CollectionConverters._


object MlbApi extends ZIOAppDefault {

  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig] =
    ZLayer.succeed(ZConnectionPoolConfig.default)

  val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool : ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.h2mem(
      database = "testdb",
      props = properties
    )

  val create: ZIO[ZConnectionPool, Throwable, Unit] = transaction {
    execute(
      sql"""
        CREATE TABLE IF NOT EXISTS games (
          date TEXT,
          season INT,
          neutral BOOLEAN,
          playoff BOOLEAN,
          team1 TEXT,
          team2 TEXT,
          elo1_pre DOUBLE,
          elo2_pre DOUBLE,
          elo_prob1 DOUBLE,
          elo_prob2 DOUBLE,
          elo1_post DOUBLE,
          elo2_post DOUBLE,
          rating1_pre DOUBLE,
          rating2_pre DOUBLE,
          pitcher1 TEXT,
          pitcher2 TEXT,
          pitcher1_rgs DOUBLE,
          pitcher2_rgs DOUBLE,
          pitcher1_adj DOUBLE,
          pitcher2_adj DOUBLE,
          rating_prob1 DOUBLE,
          rating_prob2 DOUBLE,
          rating1_post DOUBLE,
          rating2_post DOUBLE,
          score1 INT,
          score2 INT
        )
      """
    )
  }

  val insertRows: ZIO[ZConnectionPool, Throwable, UpdateResult] = transaction {
    insert(
      sql"""
        INSERT INTO games (date, season, neutral, playoff, team1, team2, elo1_pre, elo2_pre, elo_prob1, elo_prob2, elo1_post, elo2_post, rating1_pre, rating2_pre, pitcher1, pitcher2, pitcher1_rgs, pitcher2_rgs, pitcher1_adj, pitcher2_adj, rating_prob1, rating_prob2, rating1_post, rating2_post, score1, score2)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """
    )
  }

  def parseCsvFile(csvPath: String): ZIO[Any, Throwable, List[Game]] = {
    ZIO.succeed {
      val reader = new CSVReader(new FileReader(csvPath))
      val lines = reader.readAll().asScala.toList
      reader.close()
      lines.flatMap(parseGame)
    }
  }

  def parseGame(row: Array[String]): Option[Game] = {
    row match {
      case Array(date, season, neutral, playoff, team1, team2, elo1_pre, elo2_pre, elo_prob1, elo_prob2, elo1_post, elo2_post, rating1_pre, rating2_pre, pitcher1, pitcher2, pitcher1_rgs, pitcher2_rgs, pitcher1_adj, pitcher2_adj, rating_prob1, rating_prob2, rating1_post, rating2_post, score1, score2) =>
        Some(
          Game(
            date,
            season.toInt,
            neutral.toBoolean,
            playoff.toBoolean,
            team1,
            team2,
            elo1_pre.toDouble,
            elo2_pre.toDouble,
            elo_prob1.toDouble,
            elo_prob2.toDouble,
            elo1_post.toDouble,
            elo2_post.toDouble,
            rating1_pre.toDouble,
            rating2_pre.toDouble,
            pitcher1,
            pitcher2,
            pitcher1_rgs.toDouble,
            pitcher2_rgs.toDouble,
            pitcher1_adj.toDouble,
            pitcher2_adj.toDouble,
            rating_prob1.toDouble,
            rating_prob2.toDouble,
            rating1_post.toDouble,
            rating2_post.toDouble,
            score1.toInt,
            score2.toInt
          )
        )
      case _ => None
    }
  }

  def loadAllGames: ZIO[Any, Throwable, List[Game]] = {
    val csvPath = "mlb_elo.csv"

    ZIO.succeed {
      val reader = new CSVReader(new FileReader(csvPath))
      val lines = reader.readAll().asScala.toList
      reader.close()
      lines.flatMap(parseGame)
    }
  }

  def selectGame(gameId: String): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    for {
      games <- loadAllGames
      game = games.find(_.date == gameId)
    } yield game
  }

  def predictOutcome(game: Game): String = {
    // Custom prediction logic based on project requirements
    val predictedWinnerId =
      if (game.team1 == "TeamA" && game.team2 == "TeamB") "TeamA"
      else if (game.team1 == "TeamC" && game.team2 == "TeamD") "TeamD"
      else if (game.team1 == "TeamE" && game.team2 == "TeamF") "TeamE"
      else "Unknown"

    s"The predicted winner is: $predictedWinnerId"
  }

  val endpoints: App[Any] =
    Http
      .collect[Request] {
        case Method.GET -> root / "init" =>
          create *> insertRows *> ZIO.succeed(Response.text("Database initialized"))

        case Method.GET -> root / "games" =>
          loadAllGames.flatMap(games => ZIO.succeed(Response.json(games.map(_.toJson).mkString("[", ",", "]"))))

        case Method.GET -> root / "predict" / "game" / gameId =>
          selectGame(gameId).flatMap {
            case Some(game) =>
              val prediction = predictOutcome(game)
              ZIO.succeed(Response.text(prediction))

            case None =>
              ZIO.succeed(Response.text("Game not found"))
          }
      }
      .asInstanceOf[HttpApp[Any, Throwable]] // Cast the type to HttpApp[Any, Throwable]
      .withDefaultErrorResponse


  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create *> insertRows
    _ <- Server.serve(endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(createZIOPoolConfig >>> connectionPool, Server.default)
}

import argonaut.Argonaut._
import argonaut.Json


case class Game(
    date: String,
    season: Int,
    neutral: Boolean,
    playoff: Boolean,
    team1: String,
    team2: String,
    elo1_pre: Double,
    elo2_pre: Double,
    elo_prob1: Double,
    elo_prob2: Double,
    elo1_post: Double,
    elo2_post: Double,
    rating1_pre: Double,
    rating2_pre: Double,
    pitcher1: String,
    pitcher2: String,
    pitcher1_rgs: Double,
    pitcher2_rgs: Double,
    pitcher1_adj: Double,
    pitcher2_adj: Double,
    rating_prob1: Double,
    rating_prob2: Double,
    rating1_post: Double,
    rating2_post: Double,
    score1: Int,
    score2: Int
) {
    def toJson: String = {
      val fields = List(
        "date" := date,
        "season" := season,
        "neutral" := neutral,
        "playoff" := playoff,
        "team1" := team1,
        "team2" := team2,
        "elo1_pre" := elo1_pre,
        "elo2_pre" := elo2_pre,
        "elo_prob1" := elo_prob1,
        "elo_prob2" := elo_prob2,
        "elo1_post" := elo1_post,
        "elo2_post" := elo2_post,
        "rating1_pre" := rating1_pre,
        "rating2_pre" := rating2_pre,
        "pitcher1" := pitcher1,
        "pitcher2" := pitcher2,
        "pitcher1_rgs" := pitcher1_rgs,
        "pitcher2_rgs" := pitcher2_rgs,
        "pitcher1_adj" := pitcher1_adj,
        "pitcher2_adj" := pitcher2_adj,
        "rating_prob1" := rating_prob1,
        "rating_prob2" := rating_prob2,
        "rating1_post" := rating1_post,
        "rating2_post" := rating2_post,
        "score1" := score1,
        "score2" := score2
      )

      fields.asJson.spaces2
    }
}
