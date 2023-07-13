import zio._
import zio.Console._
import zio.http._
import zio.jdbc._
import zio.stream.ZStream
import zio.interop.catz._

import scala.io.Source

object MlbApi extends ZIOAppDefault {

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
  )

  val games: Ref[Map[String, MlbApi.Game]] = Ref.make(Map.empty[String, MlbApi.Game])
  val predictions: Ref[Map[String, String]] = Ref.make(Map.empty[String, String])

  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig.Config] =
    ZLayer.succeed(ZConnectionPoolConfig.Config.default)

  val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool: ZLayer[ZConnectionPoolConfig, Throwable, Has[ZConnectionPool]] =
    ZConnectionPool.fromConfig(
      ZConnectionPoolConfig.h2(
        url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        config = ZConnectionPoolConfig.Config.default
      )
    )

  val createTable: ZIO[ZConnectionPool, Throwable, Unit] = {
    val createQuery =
      sql"""
           |CREATE TABLE IF NOT EXISTS games (
           |  date VARCHAR(20) PRIMARY KEY,
           |  season INT,
           |  neutral BOOLEAN,
           |  playoff BOOLEAN,
           |  team1 VARCHAR(20),
           |  team2 VARCHAR(20),
           |  elo1_pre DOUBLE,
           |  elo2_pre DOUBLE,
           |  elo_prob1 DOUBLE,
           |  elo_prob2 DOUBLE,
           |  elo1_post DOUBLE,
           |  elo2_post DOUBLE,
           |  rating1_pre DOUBLE,
           |  rating2_pre DOUBLE,
           |  pitcher1 VARCHAR(50),
           |  pitcher2 VARCHAR(50),
           |  pitcher1_rgs DOUBLE,
           |  pitcher2_rgs DOUBLE,
           |  pitcher1_adj DOUBLE,
           |  pitcher2_adj DOUBLE,
           |  rating_prob1 DOUBLE,
           |  rating_prob2 DOUBLE,
           |  rating1_post DOUBLE,
           |  rating2_post DOUBLE,
           |  score1 INT,
           |  score2 INT
           |)
           |""".stripMargin

    ZConnectionPool
      .fromConfigManaged(ZConnectionPoolConfig.fromPrefix("h2"))
      .use(_.executeUpdate(createQuery))
      .unit
  }

  val loadData: ZIO[ZConnectionPool, Throwable, Unit] =
    for {
      source <- ZIO.effect(Source.fromResource("mlb_elo.csv"))
      _ <- ZIO
        .effectTotal(source.getLines().toList)
        .flatMap(ZIO.foreach_(_)(insertData))
      _ <- ZIO.effectTotal(source.close())
    } yield ()

  def insertData(data: String): ZIO[ZConnectionPool, Throwable, Unit] = {
    val game = data.split(",").map(_.trim)

    val insertQuery =
      sql"""
           |INSERT INTO games
           |VALUES (
           |  ${game(0)},
           |  ${game(1).toInt},
           |  ${game(2).toBoolean},
           |  ${game(3).toBoolean},
           |  ${game(4)},
           |  ${game(5)},
           |  ${game(6).toDouble},
           |  ${game(7).toDouble},
           |  ${game(8).toDouble},
           |  ${game(9).toDouble},
           |  ${game(10).toDouble},
           |  ${game(11).toDouble},
           |  ${game(12).toDouble},
           |  ${game(13).toDouble},
           |  ${game(14)},
           |  ${game(15)},
           |  ${game(16).toDouble},
           |  ${game(17).toDouble},
           |  ${game(18).toDouble},
           |  ${game(19).toDouble},
           |  ${game(20).toDouble},
           |  ${game(21).toDouble},
           |  ${game(22).toDouble},
           |  ${game(23).toInt},
           |  ${game(24).toInt}
           |)
           |""".stripMargin

    ZConnectionPool
      .fromConfigManaged(ZConnectionPoolConfig.fromPrefix("h2"))
      .use(_.executeUpdate(insertQuery))
      .unit
  }

  val initDatabase: ZIO[ZConnectionPool, Throwable, Unit] =
    createTable *> loadData

  val gameHistory: ZIO[ZConnectionPool, Throwable, List[Game]] =
    ZConnectionPool
      .fromConfigManaged(ZConnectionPoolConfig.fromPrefix("h2"))
      .use { conn =>
        val selectQuery = sql"SELECT * FROM games"
        conn
          .execute(selectQuery)
          .map(_.map { row =>
            val game = Game(
              date = row.getString("date"),
              season = row.getInt("season"),
              neutral = row.getBoolean("neutral"),
              playoff = row.getBoolean("playoff"),
              team1 = row.getString("team1"),
              team2 = row.getString("team2"),
              elo1_pre = row.getDouble("elo1_pre"),
              elo2_pre = row.getDouble("elo2_pre"),
              elo_prob1 = row.getDouble("elo_prob1"),
              elo_prob2 = row.getDouble("elo_prob2"),
              elo1_post = row.getDouble("elo1_post"),
              elo2_post = row.getDouble("elo2_post"),
              rating1_pre = row.getDouble("rating1_pre"),
              rating2_pre = row.getDouble("rating2_pre"),
              pitcher1 = row.getString("pitcher1"),
              pitcher2 = row.getString("pitcher2"),
              pitcher1_rgs = row.getDouble("pitcher1_rgs"),
              pitcher2_rgs = row.getDouble("pitcher2_rgs"),
              pitcher1_adj = row.getDouble("pitcher1_adj"),
              pitcher2_adj = row.getDouble("pitcher2_adj"),
              rating_prob1 = row.getDouble("rating_prob1"),
              rating_prob2 = row.getDouble("rating_prob2"),
              rating1_post = row.getDouble("rating1_post"),
              rating2_post = row.getDouble("rating2_post"),
              score1 = row.getInt("score1"),
              score2 = row.getInt("score2")
            )
            game
          }.toList)
      }

  def predictOutcome(game: Game): ZIO[ZConnectionPool, Throwable, String] = {
    // Custom prediction logic based on project requirements
    val predictedWinnerId =
      if (game.team1 == "TeamA" && game.team2 == "TeamB") "TeamA"
      else if (game.team1 == "TeamC" && game.team2 == "TeamD") "TeamD"
      else if (game.team1 == "TeamE" && game.team2 == "TeamF") "TeamE"
      else "Unknown"

    val prediction = s"The predicted winner is: $predictedWinnerId"
    ZIO.succeed(prediction)
  }

  val predictionHandler: HttpApp[Any, Throwable] = HttpApp.collectM {
    case Method.GET -> Root / "predict" / "game" / gameId =>
      for {
        gamesMap <- games.get
        game <- ZIO.fromOption(gamesMap.get(gameId))
        prediction <- predictOutcome(game)
      } yield Response.text(prediction)
  }

  val endpoints: HttpApp[Any, Throwable] =
    Http.collect[Request] {
      case Method.GET -> Root / "init" =>
        initDatabase *> Response.text("Database initialized")

      case Method.GET -> Root / "games" =>
        gameHistory.map(games => Response.json(games))

      case Method.GET -> Root / "predict" / "game" / gameId =>
        for {
          gamesMap <- games.get
          game <- ZIO.fromOption(gamesMap.get(gameId))
          prediction <- predictOutcome(game)
        } yield Response.text(prediction)
    }

  val app: ZIO[Console with ZConnectionPool with Server, Throwable, Unit] =
    for {
      _ <- initDatabase
      _ <- Server.start(8080, endpoints.orNotFound).fork
      _ <- putStrLn("Server started on port 8080")
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    app
      .provideCustomLayer(
        createZIOPoolConfig >>> connectionPool ++ Console.live ++ Server.default
      )
      .exitCode
}
