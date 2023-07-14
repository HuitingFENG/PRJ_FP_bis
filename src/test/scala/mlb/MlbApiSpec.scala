import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import mlb.MlbApi
import mlb.Game

object MlbApiSpec extends DefaultRunnableSpec {

  val mockApi: MlbApi.Service = new MlbApi.Service {
    override def create: ZIO[ZConnectionPool, Throwable, Unit] =
      ZIO.unit

    override def insertRows: ZIO[ZConnectionPool, Throwable, UpdateResult] =
      ZIO.succeed(UpdateResult(1))

    override def loadAllGames: ZIO[Any, Throwable, List[Game]] =
      ZIO.succeed(List(Game("2023-07-13", 2023, neutral = false, playoff = false, "TeamA", "TeamB", 100.0, 200.0, 0.6, 0.4, 120.0, 180.0, 1.0, 2.0, "PitcherA", "PitcherB", 6.0, 7.0, 1.0, 1.0, 0.7, 0.3, 1.5, 2.5, 5, 3)))

    override def selectGame(gameId: String): ZIO[ZConnectionPool, Throwable, Option[Game]] =
      ZIO.succeed(Some(Game("2023-07-13", 2023, neutral = false, playoff = false, "TeamA", "TeamB", 100.0, 200.0, 0.6, 0.4, 120.0, 180.0, 1.0, 2.0, "PitcherA", "PitcherB", 6.0, 7.0, 1.0, 1.0, 0.7, 0.3, 1.5, 2.5, 5, 3)))

    override def predictOutcome(game: Game): String =
      if (game.team1 == "TeamA" && game.team2 == "TeamB") "TeamA"
      else if (game.team1 == "TeamC" && game.team2 == "TeamD") "TeamD"
      else if (game.team1 == "TeamE" && game.team2 == "TeamF") "TeamE"
      else "Unknown"
  }

  val testApiLayer: ULayer[MlbApi] = ZLayer.succeed(mockApi)

  override def spec: ZSpec[TestEnvironment, Any] = suite("MlbApi")(
    testM("initialize should create the games table") {
      for {
        _ <- MlbApi.initialize.provideLayer(testApiLayer)
        result <- MlbApi.checkIfTableExists.provideLayer(testApiLayer)
      } yield assert(result, isTrue)
    },
    testM("loadAllGames should return a list of games") {
      for {
        games <- MlbApi.loadAllGames.provideLayer(testApiLayer)
      } yield assert(games, hasSize(equalTo(1)))
    },
    testM("selectGame should return the correct game") {
      val gameId = "2023-07-13"
      for {
        game <- MlbApi.selectGame(gameId).provideLayer(testApiLayer)
      } yield assert(game, isSome(equalTo(Game("2023-07-13", 2023, neutral = false, playoff = false, "TeamA", "TeamB", 100.0, 200.0, 0.6, 0.4, 120.0, 180.0, 1.0, 2.0, "PitcherA", "PitcherB", 6.0, 7.0, 1.0, 1.0, 0.7, 0.3, 1.5, 2.5, 5, 3))))
    },
    testM("selectGame should return None for a non-existent game") {
      val gameId = "2023-07-14"
      for {
        game <- MlbApi.selectGame(gameId).provideLayer(testApiLayer)
      } yield assert(game, isNone)
    },
    test("predictOutcome should return the predicted winner") {
      val game = Game("2023-07-13", 2023, neutral = false, playoff = false, "TeamA", "TeamB", 100.0, 200.0, 0.6, 0.4, 120.0, 180.0, 1.0, 2.0, "PitcherA", "PitcherB", 6.0, 7.0, 1.0, 1.0, 0.7, 0.3, 1.5, 2.5, 5, 3)
      val expectedWinner = "TeamA"

      val result = MlbApi.predictOutcome(game)
      assert(result, equalTo(s"The predicted winner is: $expectedWinner"))
    }
  )
}
