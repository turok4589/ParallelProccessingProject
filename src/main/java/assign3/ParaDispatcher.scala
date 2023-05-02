package assign3

import org.apache.log4j.Logger
import parascale.actor.last.{Dispatcher, Task}
import parascale.util._
import parabond.cluster._
import parabond.util.{JavaMongoHelper, MongoHelper}

object ParaDispatcher extends App {
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")
  // For initial testing on a single host, use this socket.
  // When deploying on multiple hosts, use the VM argument,
  // -Dsocket=<ip address>:9000 which points to the second
  // host.
  val socket2 = getPropertyOrElse("socket", "localhost:9000")

  // This spawns a list of relay workers at the sockets
  new ParaDispatcher(List("localhost:8000", socket2))

}

class ParaDispatcher(sockets: List[String]) extends Dispatcher(sockets) {
  import ParaDispatcher._

  def act: Unit = {
    JavaMongoHelper.hush()
    val ladder = List(
      1000,
      2000,
      4000,
      8000,
      16000,
      32000,
      64000,
      100000)
    println("ParaBond Analysis")
    println("By Miguel Vasquez")
    println("May 7th, 2023")
    println("BasicNode")
    val n = sumCoreCount()
    println(workers(0).replyAddr + " " + workers(1).forwardAddr + " " + MongoHelper.getHost)
    println("N  Missed   T1(S)   TN(s)    R    e")
    println("Cores: " + n)
     (0 until ladder.length).foreach { index =>
        val numPortfolios = ladder(index)
        val portfIds = checkReset(numPortfolios)

        val t0 = System.nanoTime()
        //Dispatch two workers
        workers(0) ! Partition(numPortfolios/2, 1)
        workers(1) ! Partition(numPortfolios/2, numPortfolios/2)

        //Expecting two results from the workers.
        val resultList = waitForWorker
        println("IM FREEEE")
        val t1 = System.nanoTime()

        val TN = (t1 - t0)/1000000000.0
        val sumT1 = sumPartialT1s(resultList)/1000000000.0

        val miss = check(portfIds)
        //print out results
        ask(numPortfolios, miss.length, sumT1, TN, n)
     }
     println("Dispatcher Finished")
  }


  /**
   * Calculated Speedup, and Efficiency and formats the report.
   *
   * @param totalSum
   * @param candidate
   * @param T1
   * @param TN
   * @param N
   */
  def ask(numPortfolios: Int, missed: Int, T1: Double, TN: Double, N: Int): Unit = {

    val R = T1 / TN
    val e = R / N

    println("%-6d %2d %6.2f %7.2f %5.2f %5.2f".format(numPortfolios, missed, T1, TN, R, e))
  }

  /**
   * Waits for workers to return their results with a comprehension.
   *
   * @return A list of results
   */
  def waitForWorker: IndexedSeq[PortfolioResult] = {
    println(workers.length)
    val workerResults = for (x <- 0 until workers.length) yield {
      receive match {
        case task: Task if (task.kind == Task.REPLY && task.payload.isInstanceOf[PortfolioResult]) =>
          println("Got Result")
          println(task.payload.asInstanceOf[PortfolioResult].partialt1)
          task.payload.asInstanceOf[PortfolioResult]
        case x =>
          PortfolioResult(0)
      }
    }
    println("Done")
    workerResults
  }

  /**
   * Results just return a partial t1 so just need to sum them up.
   * @param result
   * @return
   */
  def sumPartialT1s(result: IndexedSeq[PortfolioResult]): Long = {
    val partialT1 = result.foldLeft(0L) { (sum, workerResult) =>
      sum + workerResult.partialt1
    }
    partialT1
  }

  /**
   * Sum the core count returned by both of the workers.
   *
   * @param r list of getCores objects
   * @return the sum of cores between the two hosts
   */
  def sumCoreCount(): Int = {
    //Indicates Single Host
    if (workers(1).forwardAddr.equals("localhost")) {
      Runtime.getRuntime.availableProcessors()
    }
    else
      Runtime.getRuntime.availableProcessors() + 4 + 16
  }

}

