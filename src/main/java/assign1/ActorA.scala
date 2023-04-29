package assign1

import java.net.InetAddress
import org.apache.log4j.Logger
import parascale.actor.last.{Actor, Relay, Task}
import parascale.util.{getPropertyOrElse, sleep}

/**
* This object spawns an actor which is bound to a "relay" actor.
* The relay actor forwards messages from this actor to a remote
* actor on the same host but a different JVM or a different host.
* @author R. Coleman
*/

object ActorA extends App {
     val LOG = Logger.getLogger(getClass)

     //Instantiation automatically invokes actor's act method
     new ActorA
}

/**
 * This actor initiates communication with remote actor on different host.
 */

class ActorA extends Actor {
   //Get the socket to which the relay forwards messages
   val socket = getPropertyOrElse("remote", " 192.168.1.158" + ":" + 9000)
   //Instantiate the relay and bind this actor to it to recieve replies
   val relay = new Relay(socket, this)

   //Give actors time to start
   sleep(250)

   //Send a message to the remote actor
   relay ! Y("Hello there from " + this)

     /**
      * Start running here immediately after construction
      */
   def act = {
      import ActorA._
      LOG.info("Started")

      //Wait to recieve for a reply message in the mailbox.
      receive match {
           //All replies return to us as a reply task
           case reply: Task if reply.kind == Task.REPLY =>
              LOG.info("got reply = " + reply)

           //Since "that" has no type (see reply above which has a type), in Scala
           //It's called "type erausre" which means it is an AnyRef -- semantically
           //Case that (or any name) is like Java's defualt switch case -- a catch-all.

           case that =>
              LOG.warn("Got unexpected message = " + that)
      }
   }
}