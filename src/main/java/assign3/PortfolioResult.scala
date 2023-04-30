package assign3

/**
 * This is what the worker sends back to the dispatcher once the worker finishes calculating the sum of factors within it's partition.
 * @param sum
 * @param t0
 * @param t1
 */
case class PortfolioResult(partialt1: Long) extends Serializable
