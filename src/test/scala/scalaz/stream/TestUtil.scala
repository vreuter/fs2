package scalaz.stream

import scala.concurrent.duration._
import java.util.concurrent.{ThreadFactory, Executors, ExecutorService}
import java.util.concurrent.atomic.AtomicInteger
import scalaz.concurrent.Task

/**
 * Various testing helpers
 */
private[stream] object TestUtil {

  /** simple timing test, returns the duration and result **/
  def time[A](a: => A, l: String = ""): (FiniteDuration, A) = {
    val start = System.currentTimeMillis()
    val result = a
    val stop = System.currentTimeMillis()
     println(s"$l took ${(stop - start) / 1000.0 } seconds")
    ((stop - start).millis, result)
  }

  /** like `time` but will return time per item based on times supplied **/
  def timePer[A](items:Int)(a: => A, l: String = ""): (FiniteDuration, A) = {
    val (tm, ra) = time(a,l)
    (tm / items, ra)
  }

  val DefaultSpecExecutorService: ExecutorService = {
    val threadIndex = new AtomicInteger(0);

    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors max 32, new ThreadFactory {
      def newThread(r: Runnable) = {
        val t = new Thread(r,s"stream-spec-${threadIndex.incrementAndGet()}")
        t.setDaemon(true)
        t
      }
    })
  }

  case class UnexpectedException(e: Throwable) extends RuntimeException

  implicit class ExpectExn[O](val p: Process[Nothing, O]) extends AnyVal {
    def expectExn(pred: Throwable => Boolean): Process[Nothing, O] = p.onHalt {
      rsn =>
        if (pred(rsn)) Process.halt
        else Process.Halt(UnexpectedException(rsn))
    }
  }

  implicit class ExpectExnTask[O](val p: Process[Task, O]) extends AnyVal {
    def expectExn(pred: Throwable => Boolean): Process[Task, O] = p.onHalt {
      rsn =>
        if (pred(rsn)) Process.halt
        else Process.Halt(UnexpectedException(rsn))
    }
  }
}
