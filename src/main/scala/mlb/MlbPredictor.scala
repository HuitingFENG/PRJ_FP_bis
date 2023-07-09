import zio._
import zio.console._
import zio.random._
import zio.duration._
import zio.clock._


object MlbPredictor extends MlbApi {
  
  def predictGame(game: GameData): ZIO[Console with Random with Clock, Nothing, Unit] =
    for {
      _ <- putStrLn(s"Predicting game: ${game.team1} vs ${game.team2}")
      _ <- sleep(random.nextInt(1000).map(_.millis))
      winner <- random.nextInt(2).map(i => if (i == 0) game.team1 else game.team2)
      _ <- putStrLn(s"The predicted winner is: $winner")
    } yield ()

  def predictNextGame(games: List[GameData]): ZIO[Console with Random with Clock, Nothing, Unit] =
    games.headOption match {
      case Some(game) => predictGame(game)
      case None       => putStrLn("No games available.")
    }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    for {
      _ <- putStrLn("MLB Predictor")
      games <- ZIO.succeed(MlbDatabase.getGameHistory)
      _ <- predictNextGame(games)
    } yield ExitCode.success
}
