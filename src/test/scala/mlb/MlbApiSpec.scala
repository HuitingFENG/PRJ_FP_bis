// Test cases to be implemented

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, TestFailure, TestSuccess}
import zio.test.environment.TestEnvironment
import mlb.MlbApi
import mlb.Game

case class UpdateResult(rowsAffected: Int)
type Environment = TestEnvironment
type Failure = TestFailure[Throwable]
  
object MlbApiSpec extends DefaultRunnableSpec {

  // Helper function to create a Game object
  def createGame(date: String, team1: String, team2: String): Game =
    Game(
      date = date,
      season = 2021,
      neutral = false,
      playoff = false,
      team1 = team1,
      team2 = team2,
      elo1_pre = 100.0,
      elo2_pre = 200.0,
      elo_prob1 = 0.6,
      elo_prob2 = 0.4,
      elo1_post = 120.0,
      elo2_post = 180.0,
      rating1_pre = 1.0,
      rating2_pre = 2.0,
      pitcher1 = "Pitcher1",
      pitcher2 = "Pitcher2",
      pitcher1_rgs = 6.0,
      pitcher2_rgs = 7.0,
      pitcher1_adj = 1.0,
      pitcher2_adj = 1.0,
      rating_prob1 = 0.7,
      rating_prob2 = 0.3,
      rating1_post = 1.5,
      rating2_post = 2.5,
      score1 = 5,
      score2 = 3
    )

  val mlbApiSuite: ZSpec[Environment, Failure] =

    suite("MlbApi")(
      testM("create table") {
        for {
          _ <- ZIO.succeed(MlbApi.create)
        } yield assert(())(isUnit)
      },
      testM("insert rows") {
        for {
          result <- ZIO.succeed(MlbApi.insertRows)
        } yield assert(result)(equalTo(UpdateResult(1)))
      },
      testM("load all games") {
        for {
          games <- MlbApi.loadAllGames
        } yield assert(games)(isEmpty)
      },
      test("predict outcome") {
        val game = Game(
          date = "2023-07-13",
          season = 2023,
          neutral = false,
          playoff = false,
          team1 = "TeamA",
          team2 = "TeamB",
          elo1_pre = 1500.0,
          elo2_pre = 1500.0,
          elo_prob1 = 0.5,
          elo_prob2 = 0.5,
          elo1_post = 1500.0,
          elo2_post = 1500.0,
          rating1_pre = 0.0,
          rating2_pre = 0.0,
          pitcher1 = "PitcherA",
          pitcher2 = "PitcherB",
          pitcher1_rgs = 0.0,
          pitcher2_rgs = 0.0,
          pitcher1_adj = 0.0,
          pitcher2_adj = 0.0,
          rating_prob1 = 0.0,
          rating_prob2 = 0.0,
          rating1_post = 0.0,
          rating2_post = 0.0,
          score1 = 0,
          score2 = 0
        )
        val prediction = MlbApi.predictOutcome(game)
        assert(prediction)(equalTo("Expected prediction"))
      }
    )

  override def spec: ZSpec[Environment, Failure] =
    mlbApiSuite
}