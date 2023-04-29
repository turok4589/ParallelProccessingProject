package assign2

import org.apache.log4j.Logger
import parascale.actor.last.{Dispatcher, Task}
import parascale.future.perfect.{ask, _sumOfFactorsInRange, candidates}
import parascale.util._
/**
  * Spawns a dispatcher to connect to multiple workers.
  * @author Ron.Coleman
  */
object PerfectDispatcherVersion1 extends App {
  println("PNF Using Futures")
  println("By Miguel Vasquez")
  println("April 19th, 2023")
  println("Cores: " + Runtime.getRuntime.availableProcessors())
  println("192.168.14.1, 192.168.1.158")
  println("Candidate  Perfect T1(S)   TN(s)    R    e")
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")

  // For initial testing on a single host, use this socket.
  // When deploying on multiple hosts, use the VM argument,
  // -Dsocket=<ip address>:9000 which points to the second
  // host.
  val socket2 = getPropertyOrElse("socket","localhost:9000")

  // Construction forks a thread which automatically runs the actor act method.
  /**
  val candidates: List[Long] =
    List(
      5,
      6,
      7, // Negative test
      28,
      30, // Negative test
      496,
      8128,
      33550336,
      33550336 + 1, // Negative test
      8589869056L + 1, // Negative test
      8589869056L,
      137438691328L,
      2305843008139952128L // May take est. 170 years to prove.
    )
  */
   /**
  (0 until candidates.length).foreach { index =>
    val candidate = candidates(index)
    new PerfectDispatcher(List("localhost:8000", socket2), candidate)
  }
  */
  new PerfectDispatcher(List("localhost:8000", socket2))
}

/**
  * Template dispatcher which tests readiness of the host(s)
  * @param sockets List of sockets
  * @author Ron.Coleman
  */
class PerfectDispatcherVersion1(sockets: List[String]) extends Dispatcher(sockets) {
  import PerfectDispatcher._

  /**
    * Handles actor startup after construction.
    */
  def act: Unit = {
    val candidates: List[Long] =
      List(
        5,
        6,
        7, // Negative test
        28,
        30, // Negative test
        496,
        8128,
        33550336,
        33550336 + 1, // Negative test
        8589869056L + 1, // Negative test
        8589869056L,
        137438691328L,
        2305843008139952128L // May take est. 170 years to prove.
      )
    (0 until candidates.length).foreach { index =>
      val candidate = candidates(index)
      var counter = 0;
      var totalSum = 0L;
      var totalt1 = 0L;
      LOG.info("sockets to workers = " + sockets)

      // TODO: Replace the code below to implement PNF
      // Create the partition info and put it in two seaparate messages,
      // one for each worker, then wait below for two replies, one from
      // each worker
      val t0 = System.nanoTime()
      workers(0) ! Partition(1, candidate / 2, candidate)
      workers(1) ! Partition((candidate / 2) + 1, candidate, candidate)

      while (counter != 2)
      // This while loop waits forever but we really just need to wait
      // for two replies, one from each worker. The result, that is,
      // the partial sum and the elapsed times are in the payload as
      // a Result class.
        receive match {
          case task: Task if task.kind == Task.REPLY =>
            task.payload match {
              case r: Result =>
                counter += 1
                LOG.info("Good enough: " + r)
                totalSum += r.sum
                totalt1 = totalt1 + (r.t1 - r.t0)
                if (counter == 2) {
                  val tN = System.nanoTime() - t0
                  println(ask(totalSum, candidate, totalt1, tN))
                }

              case that =>
                LOG.warn("Got unexpected message =" + that)
            }
          case that =>
            LOG.warn("Got unexpected message =" + that)
        }
    }
  }

  def ask(totalSum: Long, candidate: Long, t1: Long, tN: Long): String = {

    val verify = (s : Long, candidate: Long) => {
       s == candidate*2
    }
    val isTrue = verify(totalSum, candidate)


    val answer = if (isTrue) "YES" else "NO"

    val R = t1.toDouble/tN.toDouble
    val N = Runtime.getRuntime.availableProcessors()
    val e = (R/N)

    val out = String.format("%.2f", (t1 / 1000000000.0))
    //println("T1 = " + out + "s")
    val out2 = String.format("%.2f", (tN) / 1000000000.0)
    //println("TN = " + out2 + "s")
    //println("N = " + N)
    val out3 = String.format("%.2f", R)
    //println("R = " + out3)
    val out4 = String.format("%.2f", e)
    //println("e = " + out4)

    candidate + "         " + answer + "     " + out + "s   "  + out2 + "s    " + out3 + "    " + out4
  }
}

