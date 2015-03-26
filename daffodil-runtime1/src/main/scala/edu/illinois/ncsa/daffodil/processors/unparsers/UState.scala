package edu.illinois.ncsa.daffodil.processors.unparsers

import edu.illinois.ncsa.daffodil.api.DFDL
import edu.illinois.ncsa.daffodil.processors.VariableMap
import edu.illinois.ncsa.daffodil.processors.ProcessorResult
import edu.illinois.ncsa.daffodil.api.Diagnostic
import edu.illinois.ncsa.daffodil.util.Maybe
import edu.illinois.ncsa.daffodil.util.Maybe._
import scala.collection.mutable.Stack
import edu.illinois.ncsa.daffodil.processors.DINode
import edu.illinois.ncsa.daffodil.exceptions.Assert
import edu.illinois.ncsa.daffodil.processors.DISimple
import edu.illinois.ncsa.daffodil.processors.ParseOrUnparseState
import edu.illinois.ncsa.daffodil.processors.InfosetItem
import edu.illinois.ncsa.daffodil.dpath.DState
import edu.illinois.ncsa.daffodil.exceptions.ThrowsSDE
import edu.illinois.ncsa.daffodil.exceptions.SavesErrorsAndWarnings
import edu.illinois.ncsa.daffodil.api.DataLocation
import edu.illinois.ncsa.daffodil.processors.DataLoc
import edu.illinois.ncsa.daffodil.processors.DataProcessor
import edu.illinois.ncsa.daffodil.processors.ElementRuntimeData
import edu.illinois.ncsa.daffodil.processors.UnparseResult
import edu.illinois.ncsa.daffodil.processors.Success
import edu.illinois.ncsa.daffodil.processors.InfosetElement
import edu.illinois.ncsa.daffodil.processors.DIArray
import edu.illinois.ncsa.daffodil.dsom.ValidationError
import scala.collection.mutable.ArrayStack
import edu.illinois.ncsa.daffodil.processors.DelimiterStackNode
import edu.illinois.ncsa.daffodil.processors.DelimiterStackUnparseNode

sealed trait UnparserMode
case object UnparseMode extends UnparserMode
case object AccumulateNodesMode extends UnparserMode

class UState(
  infosetSource: InfosetSource, vmap: VariableMap, diagnosticsArg: List[Diagnostic],
  dataProcArg: DataProcessor, var outStream: OutStream)
  extends ParseOrUnparseState(new DState, vmap, diagnosticsArg, dataProcArg)
  with Iterator[InfosetEvent] with ThrowsSDE with SavesErrorsAndWarnings {

  def peekArrayEnd = {
    peek match {
      case End(a: DIArray) => true
      case _ => false
    }
  }

  private var currentInfosetEvent_ : Maybe[InfosetEvent] = Nope
  def currentInfosetNode = if (currentInfosetNodeStack.isEmpty) Nope else currentInfosetNodeStack.top
  def currentInfosetEvent = currentInfosetEvent_

  def hasNext = infosetSource.hasNext

  def peek = infosetSource.peek

  def next = {
    val ev = infosetSource.next
    currentInfosetEvent_ = One(ev)
    if (!currentInfosetNodeStack.isEmpty) currentInfosetNodeStack.pop
    currentInfosetNodeStack.push(One(ev.node))
    ev
  }

  def lengthInBytes: Long = ???

  var mode: UnparserMode = UnparseMode

  def thisElement: InfosetElement = currentInfosetNode.get.asInstanceOf[InfosetElement]

  override def status: ProcessorResult = Success

  val unparseResult = new UnparseResult(dataProcArg, this)

  def addDeferredElement(elt: DINode) {
    Assert.usage(elt.isInstanceOf[DISimple]) // only simple types for outputValueCalc

    //TODO: Implement something here. For now, we're going to have a really 
    //limited implementation - we know we can evaluate the deferred element
    //expressions when we have the ENTIRE infoset.
  }

  /**
   * flag indicates that the caller of the unparser does NOT care that the infoset
   * elements are retained; hence, the unparser is free to delete them, clobber
   * them or whatever it wants.
   */
  var removeUnneededInfosetElements = false // set from API of top level Unparser call. 

  def currentLocation: DataLocation = new DataLoc(bitPos1b, bitLimit1b, outStream)

  val currentInfosetNodeStack = Stack[Maybe[DINode]]()

  val arrayIndexStack = Stack[Long](1L)
  def moveOverOneArrayIndexOnly() = arrayIndexStack.push(arrayIndexStack.pop + 1)
  def arrayPos = arrayIndexStack.top

  val groupIndexStack = Stack[Long](1L)
  def moveOverOneGroupIndexOnly() = groupIndexStack.push(groupIndexStack.pop + 1)
  def groupPos = groupIndexStack.top

  // TODO: it doesn't look anything is actually reading the value of childindex
  // stack. Can we get rid of it?
  val childIndexStack = Stack[Long](1L)
  def moveOverOneElementChildOnly() = childIndexStack.push(childIndexStack.pop + 1)
  def childPos = childIndexStack.top

  val occursBoundsStack = new Stack[Long]
  def updateBoundsHead(ob: Long) = {
    occursBoundsStack.pop()
    occursBoundsStack.push(ob)
  }

  def occursBounds = occursBoundsStack.top
  
  val delimiterStack = new ArrayStack[DelimiterStackUnparseNode]()
  def pushDelimiters(node: DelimiterStackUnparseNode) = delimiterStack.push(node)
  def popDelimiters() = delimiterStack.pop
  def localDelimiters = delimiterStack.top

  def bitPos0b = outStream.bitPos0b
  def bitLimit0b = outStream.bitLimit0b
  def bytePos0b = bitPos0b >> 3
  def bytePos1b = (bitPos0b >> 3) + 1
  def bitPos1b = bitPos0b + 1
  def bitLimit1b = bitLimit0b + 1
  def whichBit0b = bitPos0b % 8

  def validationError(msg: String, args: Any*) {
    val ctxt = getContext()
    val vde = new ValidationError(Some(ctxt.schemaFileLocation), this, msg, args: _*)
    diagnostics = vde :: diagnostics
  }
}

object UState {

  def createInitialUState(
    out: OutStream,
    dataProc: DFDL.DataProcessor,
    docSource: InfosetSource): UState = {

    val variables = dataProc.getVariables
    val diagnostics = Nil
    val newState = new UState(docSource, variables, diagnostics, dataProc.asInstanceOf[DataProcessor], out)
    newState
  }
}
