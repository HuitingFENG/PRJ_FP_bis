import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment

object MlbApiSpec extends DefaultRunnableSpec {
  val api: ULayer[MlbDatabase] = ???

  val testEnv: ULayer[TestEnvironment] = TestEnvironment.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("MLB API")(
      testM("initialize database") {
        for {
          _ <- MlbApi.initDatabase.provideLayer(api)
          teams <- MlbApi.getTeams.provideLayer(api)
          players <- MlbApi.getPlayers.provideLayer(api)
          games <- MlbApi.getGames.provideLayer(api)
        } yield assert(teams.nonEmpty)(isTrue) &&
          assert(players.nonEmpty)(isTrue) &&
          assert(games.nonEmpty)(isTrue)
      },
      testM("retrieve games") {
        for {
          _ <- MlbApi.initDatabase.provideLayer(api)
          games1 <- MlbApi.getGames.provideLayer(api)
          _ <- MlbApi.insertData.provideLayer(api)
          games2 <- MlbApi.getGames.provideLayer(api)
        } yield assert(games1.isEmpty)(isTrue) &&
          assert(games2.nonEmpty)(isTrue)
      },
      testM("predict game outcome") {
        for {
          _ <- MlbApi.initDatabase.provideLayer(api)
          game1 <- MlbApi.getGame("gameId1").provideLayer(api)
          prediction1 <- game1.fold(ZIO.none)(MlbApi.predictOutcome).provideLayer(api)
          _ <- MlbApi.insertData.provideLayer(api)
          game2 <- MlbApi.getGame("gameId2").provideLayer(api)
          prediction2 <- game2.fold(ZIO.none)(MlbApi.predictOutcome).provideLayer(api)
        } yield assert(prediction1)(isNone) &&
          assert(prediction2)(isSome)
      }
    )
}

object MlbApiTestRunner extends zio.App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    MlbApiSpec
      .run()
      .provideCustomLayer(MlbApi.api ++ MlbApi.testEnv)
      .exitCode
}
