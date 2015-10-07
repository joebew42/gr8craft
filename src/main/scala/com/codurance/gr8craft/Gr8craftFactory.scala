package com.codurance.gr8craft

import akka.actor.{ActorSystem, Props}
import com.codurance.gr8craft.infrastructure.TwitterApiService
import com.codurance.gr8craft.infrastructure.TwitterFactoryWithConfiguration.createTwitter
import com.codurance.gr8craft.model.inspiration.{Archivist, Inspiration, Shelf}
import com.codurance.gr8craft.model.publishing.{Publisher, TwitterService}
import com.codurance.gr8craft.model.supervision.{Editor, Supervisor}

import scala.concurrent.duration._

object Gr8craftFactory {
  def createApplication(system: ActorSystem = ActorSystem("Gr8craftSystem"), twitterService: TwitterService = new TwitterApiService(createTwitter()), initalInspirations: Set[Inspiration] = Set.empty, tweetInterval: Duration = 1.hour): Gr8craft = {
    val tweeter = system.actorOf(Props(new Publisher(twitterService)))
    val shelf = system.actorOf(Props(new Archivist(new Shelf(initalInspirations))))
    val tweetRunner = system.actorOf(Props(new Editor(tweeter, shelf)))
    val scheduler = system.actorOf(Props(new Supervisor(tweetInterval, tweetRunner)))
    new Gr8craft(scheduler)
  }
}
