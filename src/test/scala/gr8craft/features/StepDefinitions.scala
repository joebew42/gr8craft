package gr8craft.features

import java.util.concurrent.TimeUnit

import cucumber.api.scala.{EN, ScalaDsl}
import gr8craft.ApplicationRunner
import gr8craft.article.{Shelf, Article, InMemoryShelf}
import gr8craft.scheduling.ScheduledExecutor
import gr8craft.twitter.{TweetRunner, TwitterApiService}
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import twitter4j.{Status, TwitterFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class StepDefinitions extends ScalaDsl with EN with Matchers with Eventually {

  private val twitter = TwitterFactory.getSingleton
  val twitterService = new TwitterApiService(twitter)
  var shelf: Shelf = null
  var application: ApplicationRunner = null

  Before() { _ =>
    twitter.getUserTimeline.asScala.foreach(status => twitter.destroyStatus(status.getId))
  }

  After() { _ =>
    application.stop()
  }

  Given( """^the next article on the shelf about "([^"]*)" can be found at "([^"]*)"$""") { (topic: String, articleLocation: String) =>
    shelf = InMemoryShelf(Seq(new Article(topic, articleLocation)))
  }

  When( """^the hour is reached$""") { () =>
    val scheduler = new ScheduledExecutor(TimeUnit.SECONDS, new TweetRunner(twitterService, shelf));
    this.application = new ApplicationRunner(scheduler)
    application.startTwitterBot()
  }

  Then( """^gr8craft tweets "([^"]*)"$""") { (expectedTweet: String) =>
    val newestTweet = eventually(timeout(5.seconds), interval(1.second)) {
      val newestTweet: Option[Status] = twitter.getUserTimeline.asScala.headOption
      newestTweet.map(_.getText)
    }
    newestTweet.get shouldEqual expectedTweet
  }
}
