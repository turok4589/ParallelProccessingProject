package assign3

import org.apache.log4j.Logger
import parascale.actor.last.{Task, Worker}
import parascale.util._
import parabond.cluster._
import parabond.util.{JavaMongoHelper}

object ParaWorker extends App {

     JavaMongoHelper.hush()
     val LOG = Logger.getLogger(getClass)

     LOG.info("started")
     //a. If worker running on a single host, spawn two workers
     //   else spawn one worker.
     val nhosts = getPropertyOrElse("nhosts", 1)
     // One-port configuration
     val port1 = getPropertyOrElse("port", 8000)

     // If there is 1 host, then ports include 9000 by default
     // Otherwise, if there are two hosts in this configuration,
     // use just one port which must be specified by VM options
     val ports =  if (nhosts == 1) List(port1, 9000) else List(port1)

     // Spawn the worker(s).
     // Note: for initial testing with a single host, "ports"
     // contains two ports. When deploying on two hosts, "ports"
     // will contain one port per host.
     for (port <- ports) {
         // Start a new worker.
       new ParaWorker(port)
  }

}


class ParaWorker(port: Int) extends Worker(port) {
  import ParaWorker._

  /**
   * Handles actor startup after construction.
   */
  override def act: Unit = {

    val name = getClass.getSimpleName
    LOG.info("started " + name + " (id=" + id + ")")

    // Wait for inbound messages as tasks.
    while (true) {
      receive match {
        case task: Task =>
          LOG.info("got task = " + task + " sending reply")
          task.payload match {
            //Worker receives a partition from the dispatcher and then returns results back to dispatcher
            case p: Partition =>
              println("Begin: " + p.begin)
              println("Num" + p.n)
              // BasicNode is default, use -Dnode=className to modify
              val node = Node.getInstance(p)

              assert(node != null, "failed to construct node")
              println("Analyzing....")
              println("Begin: " + p.begin)
              println("Num" + p.n)
              val analysis = node.analyze()

              /**
               * Analysis contains a seq of jobs: results
               * These jobs contain a result which contain t0, and t1
               * We need to sum these to get the partial T1's of the individual workers.
               */
               println("Finished analyzing time to sum t1")
               val partialT1 = analysis.results.foldLeft(0L){(sum, jobResult) =>
                  sum + (jobResult.result.t1 - jobResult.result.t0)
               }
              println("Begin: " + p.begin)
              println("Num" + p.n)
              println("Partial T1: " + partialT1)
              sender.send(PortfolioResult(partialT1))
          }

        // Send a simple reply to test the connectivity.
        //sender ! name + " READY (id=" + id + ")"
      }
    }
  }
}


