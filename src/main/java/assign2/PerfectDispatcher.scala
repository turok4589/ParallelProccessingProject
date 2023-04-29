package assign2

import org.apache.log4j.Logger
import parascale.actor.last.{Dispatcher, Task}
import parascale.util._
/**
  * Spawns a dispatcher to connect to multiple workers.
  * @author Miguel Vasquez
  */
object PerfectDispatcher extends App {
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")

  // For initial testing on a single host, use this socket.
  // When deploying on multiple hosts, use the VM argument,
  // -Dsocket=<ip address>:9000 which points to the second
  // host.
  val socket2 = getPropertyOrElse("socket","localhost:9000")

  // Construction forks a thread which automatically runs the actor act method.
  new PerfectDispatcher(List("localhost:8000", socket2))
}

/**
  * Template dispatcher which tests readiness of the host(s)
  * @param sockets List of sockets
  * @author Miguel Vasquez
  */
class PerfectDispatcher(sockets: List[String]) extends Dispatcher(sockets) {
  import PerfectDispatcher._

  /**
    * Handles actor startup after construction.
    */
  def act: Unit = {
    val candidates: List[Long] = {
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
    }
    workers(0) ! getCores(0)
    workers(1) ! getCores(0)
    val workerCores = waitForCoreCount
    val n = sumCoreCount(workerCores)

    println("PNF Using Parallel Collections")
    println("By Miguel Vasquez")
    println("April 19th, 2023")
    println("Cores: " + n)
    println(workers(0).replyAddr + " " + workers(1).forwardAddr)
    println("Candidate  Perfect   T1(S)   TN(s)    R    e")

    (0 until candidates.length).foreach { index =>
      val candidate = candidates(index)
      LOG.info("sockets to workers = " + sockets)

      val t0 = System.nanoTime()
      workers(0) ! Partition(1, candidate / 2, candidate)
      workers(1) ! Partition((candidate / 2) + 1, candidate, candidate)

      val results = waitForWorker
      val t1 = System.nanoTime()
      val TN = (t1 - t0)/1000000000.0

      //Sum both the partial sums and the partial t1's returned by the workers
      val sum = sumResults(results)
      val T1 = sumPartialT1(results)
      ask(sum, candidate, T1, TN, n)

    }
  }

  /**
   * Calculated Speedup, and Efficiency and formats the report.
   * @param totalSum
   * @param candidate
   * @param T1
   * @param TN
   * @param N
   */
  def ask(totalSum: Long, candidate: Long, T1: Double, TN: Double, N: Int): Unit = {

    val verify = (s: Long, candidate: Long) => {
      s == candidate * 2
    }
    val isTrue = verify(totalSum, candidate)

    val answer = if (isTrue) "YES" else "NO"

    val R = T1 / TN
    val e = R / N
    //Old formatting
    /**
     *  val out = String.format("%.2f", T1)
     *  //println("T1 = " + out + "s")
     *  val out2 = String.format("%.2f", TN)
     *  //println("TN = " + out2 + "s")
     *  //println("N = " + N)
     *  val out3 = String.format("%.2f", R)
     *  //println("R = " + out3)
     *  val out4 = String.format("%.2f", e)
     *  //println("e = " + out4)
     *
     *  candidate + "         " + answer + "     " + out + "s   "  + out2 + "s    " + out3 + "    " + out4
     */
    println("%-12d %5s %6.2f %7.2f %5.2f %5.2f".format(candidate, answer, T1, TN, R, e))
  }


  /**
   * Waits for workers to return their results with a comprehension.
   * @return A list of results
   */
  def waitForWorker: IndexedSeq[Result] = {
    val workerResults = for (x <- 0 until workers.length) yield {
      receive match {
        case task: Task if (task.kind == Task.REPLY && task.payload.isInstanceOf[Result]) =>
          task.payload.asInstanceOf[Result]
        case x =>
          Result(0, 1, -1)
      }
    }
    workerResults
  }

  /**
   * This waits for the two workers to return the core counts on their machines.
   * @return A list of getCores objects that contain the number of cores
   */
  def waitForCoreCount: IndexedSeq[getCores] = {
    val workerResults = for (x <- 0 until workers.length) yield {
      receive match {
        case task: Task if (task.kind == Task.REPLY && task.payload.isInstanceOf[getCores]) =>
          task.payload.asInstanceOf[getCores]
        case x =>
          getCores(0)
      }
    }
    workerResults
  }

  /**
   * Sums the partial sums returns by the workers
   * @param r is a list of results
   * @return sum: the combined partial sums of the two workers
   */
  def sumResults(r: IndexedSeq[Result]): Long = {
    val sum = r.foldLeft(0L) { (sum, workerResult) =>
      sum + workerResult.sum
    }
    sum
  }

  /**
   * Calculate T1 based on the partial t1 and t0's returned by the workers.
   * @param r: List of results
   * @return T1: Time it would take for the serial version of the program to execute.
   */
  def sumPartialT1(r: IndexedSeq[Result]): Double = {
    val T1 = r.foldLeft(0L) { (sum, workerResult) =>
      sum + (workerResult.t1 - workerResult.t0)
    }
    T1/1000000000.0
  }

  /**
   * Sum the core count returned by both of the workers.
   * @param r list of getCores objects
   * @return the sum of cores between the two hosts
   */
  def sumCoreCount(r: IndexedSeq[getCores]): Int = {
    val coreCount = r.foldLeft(0) { (sum, workerResult) =>
      sum + workerResult.Cores
    }
    //Indicates Single Host
    if (workers(1).forwardAddr.equals("localhost")) {
       Runtime.getRuntime.availableProcessors()
    }
    else
    coreCount
  }
}

