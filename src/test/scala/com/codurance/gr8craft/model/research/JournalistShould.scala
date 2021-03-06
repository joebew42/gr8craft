package com.codurance.gr8craft.model.research

import akka.actor.{ActorRef, Kill, Props}
import akka.testkit.TestProbe
import com.codurance.gr8craft.messages._
import com.codurance.gr8craft.util.AkkaTest
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JournalistShould extends AkkaTest("JournalistShould") with MockFactory {
  private val lastId = DirectMessageId(42L)

  private val textOfDirectMessage = "inspiration: DDD | location: http://t.co/lqJDZlGcJE | contributor: @gr8contributor"
  private val directMessage = new DirectMessageBuilder().withText(textOfDirectMessage).withId(lastId).build()
  private val textOfLaterDirectMessage = "inspiration: Another | location: url | contributor: @anotherContributor"
  private val laterDirectMessage = new DirectMessageBuilder().withText(textOfLaterDirectMessage).build()

  private val archivist = TestProbe()
  private val researcher = TestProbe()

  private var journalist = createJournalist()

  test("receive a trigger and ask the researcher for new DMs since last asked") {
    journalist ! Trigger

    researcher.expectMsg(FetchDirectMessages(None))

    journalist ! AddDirectMessage(directMessage)
    journalist ! Trigger

    researcher.expectMsg(FetchDirectMessages(Some(lastId)))
  }

  test("receive a new inspiration for the archivist and forward it") {
    journalist ! AddDirectMessage(directMessage)

    expectInspirationAddedFrom(textOfDirectMessage)
    researcher.expectNoMsg()
  }

  test("recover trigger by continuing from last id asked afterwards") {
    journalist ! Trigger
    researcher.expectMsg(FetchDirectMessages(None))
    journalist ! AddDirectMessage(directMessage)

    recoverFromShutdown()

    researcher.expectNoMsg()

    journalist ! Trigger
    researcher.expectMsg(FetchDirectMessages(Some(lastId)))
  }

  test("recover getting DirectMessages") {
    journalist ! AddDirectMessage(directMessage)
    expectInspirationAddedFrom(textOfDirectMessage)
    journalist ! AddDirectMessage(laterDirectMessage)
    expectInspirationAddedFrom(textOfLaterDirectMessage)

    recoverFromShutdown()

    expectInspirationAddedFrom(textOfDirectMessage)
    expectInspirationAddedFrom(textOfLaterDirectMessage)
    researcher.expectNoMsg()
  }

  private def recoverFromShutdown(): Unit = {
    journalist ! Kill
    journalist = createJournalist()
  }

  private def createJournalist(): ActorRef = {
    system.actorOf(Props(new Journalist(researcher.ref, archivist.ref)))
  }

  private def expectInspirationAddedFrom(text: String): AddInspiration = {
    archivist.expectMsg(AddInspiration(new Suggestion(text).parse.get))
  }
}
