package com.mirkocaserta.swatch

import akka.actor.ActorRef

import concurrent.Future
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.WatchEvent.Kind

import language.implicitConversions
import org.slf4j.LoggerFactory

import util.{Failure, Success, Try}

/**
 * A wrapper for a Java 7 [[java.nio.file.WatchService]].
 */
object Swatch {

  val log = LoggerFactory.getLogger(getClass)

  type Listener = (SwatchEvent) => Unit

  sealed trait EventType

  case object Create extends EventType

  case object Modify extends EventType

  case object Delete extends EventType

  case object Overflow extends EventType

  sealed trait SwatchEvent {
    def path: Path
  }

  case class Create(path: Path) extends SwatchEvent

  case class Modify(path: Path) extends SwatchEvent

  case class Delete(path: Path) extends SwatchEvent

  private[this] implicit def eventType2Kind(et: EventType) = {
    import java.nio.file.StandardWatchEventKinds._

    et match {
      case Create => ENTRY_CREATE
      case Modify => ENTRY_MODIFY
      case Delete => ENTRY_DELETE
      case Overflow => OVERFLOW
    }
  }

  private[this] implicit def kind2EventType(kind: Kind[Path]) = {
    import java.nio.file.StandardWatchEventKinds._

    kind match {
      case ENTRY_CREATE => Create
      case ENTRY_MODIFY => Modify
      case ENTRY_DELETE => Delete
      case _ => Overflow
    }
  }

  implicit def string2path(path: String): Path = Paths.get(path)

  /**
   * Message class for the SwatchActor.
   *
   * @param path the path to watch
   * @param eventTypes event types to watch for
   * @param recurse should subdirs be watched too?
   * @param listener an optional [[akka.actor.ActorRef]]
   *                 where notifications will get sent to;
   *                 if unspecified, the [[akka.actor.Actor#sender]]
   *                 ref will be used
   */
  case class Watch(path: Path, eventTypes: Seq[EventType], recurse: Boolean = false, listener: Option[ActorRef] = None)

  /**
   * Watch the given path by using a Java 7
   * [[java.nio.file.WatchService]].
   *
   * @param path the path to watch
   * @param eventTypes event types to watch for
   * @param listener events will be sent here
   * @param recurse should subdirs be watched too?
   */
  def watch(path: Path,
            eventTypes: Seq[EventType],
            listener: Listener,
            recurse: Boolean = false): Unit = {
    log.debug(s"watch(): entering; path='$path', eventTypes='$eventTypes', listener='$listener', recurse=$recurse")
    val watchService = FileSystems.getDefault.newWatchService
    log.trace(s"watchService: $watchService, fs: ${FileSystems.getDefault}")

    if (recurse) {
      Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        override def preVisitDirectory(path: Path, attrs: BasicFileAttributes) = {
          watch(path, eventTypes, listener)
          FileVisitResult.CONTINUE
        }
      })
    } else path.register(watchService, eventTypes map eventType2Kind: _*)

    import concurrent.ExecutionContext.Implicits.global

    Future {
      import scala.collection.JavaConverters._
      var loop = true

      while (loop) {
        Try(watchService.take) match {
          case Success(key) =>
            log.debug(s"watch(): took from watchService; key==$key")

            key.pollEvents.asScala foreach {
              event =>
                import java.nio.file.StandardWatchEventKinds.OVERFLOW

                event.kind match {
                  case OVERFLOW => log.debug(s"watch(): got overflow event - underlying APIs were overloaded!")
                  case _ =>
                    val ev = event.asInstanceOf[WatchEvent[Path]]
                    val tpe = kind2EventType(ev.kind)
                    val notification = tpe match {
                      case Create => Create(path.resolve(ev.context))
                      case Modify => Modify(path.resolve(ev.context))
                      case Delete => Delete(path.resolve(ev.context))
                    }
                    log.debug(s"watch(): notifying listener; notification=$notification")
                    listener(notification)
                    if (!key.reset) {
                      log.debug("watch(): reset unsuccessful, exiting the loop")
                      loop = false
                    }
                }
            }
          case Failure(e: InterruptedException) => // keep on truckin', this is literally telling us to try again
          case Failure(e: ClosedWatchServiceException) =>
            log.debug("watch(): watch was closed elsewhere, existing the loop")
            loop = false
          case Failure(e) => // that wasn't supposed to happen
            log.debug("watch(): unexpected exception from watchService.take", e)
        }
      }
    }
    log.debug(s"watch(): exiting; path='$path', eventTypes='$eventTypes', listener='$listener', recurse=$recurse")
  }

}

