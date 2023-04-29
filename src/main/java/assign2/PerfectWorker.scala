package assign2

import jdk.internal.org.jline.reader.Candidate
import org.apache.log4j.Logger
import parascale.actor.last.{Task, Worker}
import parascale.future.perfect.{_sumOfFactorsInRange, ask, candidates}
import parascale.util._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Spawns workers on the localhost.
  * @author Miguel Vasquez
  */
object PerfectWorker extends App {
  val LOG = Logger.getLogger(getClass)

  LOG.info("started")

  // Number of hosts in this configuration
  val nhosts = getPropertyOrElse("nhosts",1)

  // One-port configuration
  val port1 = getPropertyOrElse("port", 8000)

  // If there is just one host, then the ports will include 9000 by default
  // Otherwise, if there are two hosts in this configuration, use just one
  // port which must be specified by VM options
  val ports = if(nhosts == 1) List(port1, 9000) else List(port1)

  // Spawn the worker(s).
  // Note: for initial testing with a single host, "ports" contains two ports.
  // When deploying on two hosts, "ports" will contain one port per host.
  for(port <- ports) {
    // Construction forks a thread which automatically runs the actor act method.
    new PerfectWorker(port)
  }
}

/**
  * Template worker for finding a perfect number.
  * @param port Localhost port this worker listens to
  * @author Miguel Vasquez
  */
class PerfectWorker(port: Int) extends Worker(port) {
  import PerfectWorker._

  /**
    * Handles actor startup after construction.
    */
  override def act: Unit = {
    val name = getClass.getSimpleName
    LOG.info("started " + name + " (id=" + id + ")")

    // Wait for inbound messages as tasks.
    while (true) {
      receive match {
          // It gets the partition range info from the task payload then
          // spawns futures (or uses parallel collections) to analyze the
          // partition in parallel. Finally, when done, it replies
          // with the partial sum and the total estimated serial time for
          // this worker.
        case task: Task =>
          LOG.info("got task = " + task + " sending reply")
          task.payload match {
            //Worker receives a partition from the dispatcher and then returns results back to dispatcher
            case p: Partition  =>
            //val r = isPerfect(p.start, p.end, p.candidate)
              val r = isPerfect(p.start, p.end, p.candidate)
            sleep(250)
            //sender.send(r)
             sender.send(r)
            case n: getCores =>
              val n = Runtime.getRuntime.availableProcessors()
              sleep(250)
              sender.send(getCores(n))
          }

          // Send a simple reply to test the connectivity.
          //sender ! name + " READY (id=" + id + ")"
      }
    }
  }

  import scala.collection.parallel.CollectionConverters._

  /**
   * Calculate the sum of factors of perfect numbers using parallel collections
   * @param start
   * @param end
   * @param candidate
   * @return Results object back to dispatcher
   */
  def isPerfect(start: Long, end: Long, candidate: Long): Result = {
    println("Partition info " + start + " " + end + " " + candidate)
    val RANGE = 1000000L
    //val RANGE = (end - start);
    //val numPartitions = (startPar(start) / RANGE).ceil.toIntval
    val numPartitions = checkstart(start, candidate, RANGE)

    val startRange = (start: Long, candidate: Long, RANGE: Long, numPartitions: Long) => {
        if(start < candidate/2.0 || numPartitions == 1){
           val x = 0L
           x
        }
        else{
           val x = ((candidate.toDouble/2.0)/RANGE).ceil.toInt
           x
        }
    }
    // Start with a par collection which propagates through subsequent calculations
    //val partitions = (0L until numPartitions)
    val partitions = (startRange(start, candidate, RANGE, numPartitions) until numPartitions).par

    val ranges = for (k <- partitions) yield {
      val lower: Long = k * RANGE + 1
      //remember factors start at 1 or in this case wherever the partition begins.
      val upper: Long = end min (k + 1) * RANGE

      if(numPartitions == 1){
        (start, end)
      }
      else
      (lower, upper)
    }

    // Ranges is a collection of 2-tuples of the lower-to-upper partition bounds
    val sums = ranges.map { lowerUpper =>
      val startOfT1Clock = System.nanoTime()
      val (lower, upper) = lowerUpper
      val sum = _sumOfFactorsInRange(lower, upper, candidate)
      val endOfT1Clock = System.nanoTime()
      (sum, startOfT1Clock, endOfT1Clock)
    }

    /**
     * Calculate the partialsum, partialt0, and partial t1 inside of the sums 3-tuple.
     */
    val partialSum = sums.foldLeft(0L) { (sum, partialSums) =>
      val (partialPartialsum, partialT0, partialT1) = partialSums
      sum + partialPartialsum
    }


    val t0 = sums.foldLeft(0L) { (sum, partialSums) =>
      val (partialPartialsum, partialT0, partialT1) = partialSums
      sum + partialT0
    }


    val t1 = sums.foldLeft(0L) { (sum, partialSums) =>
      val (partialPartialsum, partialT0, partialT1) = partialSums
      sum + partialT1
    }

    //Send results back to the dispatcher
    Result(partialSum, t0, t1)
  }

  /**
   * This helps with the repartitioning of the range for each of the worker.
   * @param start
   * @param candidate
   * @param range
   * @return The number of partitions for each worker.
   */
  def checkstart(start: Long, candidate: Long, range: Long): Int ={
     if(start > candidate/2){
        //1
       (candidate.toDouble/range).ceil.toInt
     }
     else
       ((candidate/2).toDouble/range).ceil.toInt
  }

}
