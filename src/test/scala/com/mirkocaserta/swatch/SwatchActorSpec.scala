package com.mirkocaserta.swatch

import akka.actor.{ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}

import com.typesafe.config.ConfigFactory

import concurrent.duration._
import java.nio.file.{Files, Paths}

import language.postfixOps
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SwatchActorSpec extends TestKit(ActorSystem("SwatchActorSpec",
  ConfigFactory.parseString(SwatchActorSpec.config)))
with DefaultTimeout with ImplicitSender
with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  import Swatch._

  "A Swatch actor" should {
    "send notifications" in {
      within(15 seconds) {
        val swatch = system.actorOf(Props[SwatchActor])
        val dir = tmp
        swatch ! Watch(dir, Seq(Create))
        Thread.sleep(3000)
        val file = Files.createTempFile(dir, "file-", "")
        expectMsg(Create(file))
      }
    }
  }

  override def afterAll {
    system.shutdown
  }

  private[this] def tmp = Files.createTempDirectory(Paths.get("target"), "watch-actor-")

}

object SwatchActorSpec {
  // test specific configuration
  val config = """
    akka {
      loglevel = "DEBUG"
    }
               """
}
