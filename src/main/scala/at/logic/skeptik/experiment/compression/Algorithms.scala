package at.logic.skeptik.experiment.compression

import at.logic.skeptik.proof.sequent.SequentProof
import at.logic.skeptik.proof.ProofNodeCollection
import at.logic.skeptik.util.time._

// Results

class Result (result: SequentProof, time: Double)
extends Timed[SequentProof](result, time) {
  lazy val nodeCollection = ProofNodeCollection(this.result)
}
object Result {
  def apply(t: Timed[SequentProof]) = new Result(t.result, t.time)
}

class CountedResult (result: SequentProof, time: Double, val count: Int)
extends Result(result, time) {
  def +(other: Timed[SequentProof]) = new CountedResult(other.result, time + other.time, count + 1)
}
object CountedResult {
  def reset(r: Result) = new CountedResult(r.result, 0., 0)
}


// Algorithms

abstract class WrappedAlgorithm (val name: String)
extends Function1[Result,Result]

class SimpleAlgorithm (name: String, fct: SequentProof => SequentProof)
extends WrappedAlgorithm(name) {
  def apply(result: Result) = Result(timed { fct(result.result) })
}

class RepeatAlgorithm (name: String, fct: SequentProof => SequentProof)
extends WrappedAlgorithm(name) {
  def apply(result: Result) = {
    def repeat(preceding: CountedResult):CountedResult = {
      val next = preceding + timed { fct(result.result) }
      if (next.nodeCollection.size < preceding.nodeCollection.size) repeat(next) else next
    }
    repeat(CountedResult.reset(result))
  }
}

class TimeOutAlgorithm (name: String, fct: SequentProof => SequentProof)
extends WrappedAlgorithm(name) {
  lazy val factor = environment.getOrElse("timeout","1.").toDouble
  def apply(result: Result) = {
    var maxTime = result.time * factor
    def repeat(preceding: CountedResult):CountedResult = {
      val timeLeft = maxTime - preceding.time
      println(timeLeft + " time left")
      if (timeLeft <= 0.) preceding else
        timeout(timeLeft.toLong) { timed { fct(result.result) } } match {
          case None => preceding
          case Some(t) => repeat(preceding + t)
        }
    }
    repeat(CountedResult.reset(result))
  }
}

