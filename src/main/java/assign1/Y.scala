package assign1

/**
4 * This is the message sent from actor A to actor B.
5 * It MUST be serializable to be transmitted as a POJO.
6 * @author R. Coleman
7 * @param s String message.
8 */

case class Y(s: String) extends Serializable
