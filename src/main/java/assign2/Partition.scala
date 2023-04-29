package assign2

/**
 * Divide the candidate among the two workers.
 * First worker receives partition(1, candidate/2)
 * Second worker receives partition(candidate+1/2, candidate)
 * @param start
 * @param end
 * @param candidate
 */
case class Partition(start: Long, end: Long, candidate: Long) extends Serializable
