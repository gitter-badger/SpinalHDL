/*
 * SpinalHDL
 * Copyright (c) Dolu, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package spinal.core

import java.text.SimpleDateFormat
import java.util.Date

object SpinalVhdl {

  val runtime = Runtime.getRuntime
  println({SpinalLog.tag("Runtime", Console.YELLOW)} + s" JVM max memory : ${f"${(runtime.maxMemory()).toFloat/1048576f}%1.1f"}MiB")

  val curDate:Date = new Date()
  val dateFmt:SimpleDateFormat = new SimpleDateFormat ("yyyy.MM.dd HH:mm:ss")
  println({SpinalLog.tag("Runtime", Console.YELLOW)} + s" Current date : ${dateFmt.format(curDate)}")

  def apply[T <: Component](gen: => T): BackendReport[T] = apply(gen, _.nothing)

  def apply[T <: Component](gen: => T, config: SpinalVhdl[_] => Unit): BackendReport[T] = {
    def doIt(tryCounter: Int = 0): BackendReport[T] = {
      try {
        val factory = new SpinalVhdl(gen)
        config(factory)
        if (tryCounter != 0) GlobalData.get.scalaLocatedEnable = true
        val ret =  factory.elaborate
        println({SpinalLog.tag("Done", Console.GREEN)} + s" at ${f"${Driver.executionTime}%1.3f"}")
        return ret
      } catch {
        case e: Throwable => {
          tryCounter match {
            case 0 => {
              Thread.sleep(10);
              println("\n**********************************************************************************************")
              val errCnt = SpinalError.getErrorCount()
              SpinalWarning(s"Elaboration failed (${errCnt} error" + (if(errCnt > 1){s"s"} else {s""}) + s").\n" +
                            s"          Spinal will restart with scala trace to help you to find the problem.")
              println("**********************************************************************************************\n")
              Thread.sleep(10);
              return doIt(1)
            }
            case 1 => {
              Thread.sleep(10);
              println("\n**********************************************************************************************")
              val errCnt = SpinalError.getErrorCount()
              SpinalWarning(s"Elaboration failed (${errCnt} error" + (if(errCnt > 1){s"s"} else {s""}) + ").")
              println("**********************************************************************************************")
              Thread.sleep(10);
              throw e
            }
          }
        }
      }
      throw new Exception
    }
    return doIt()
  }
}

class SpinalVhdl[T <: Component](gen: => T) {
  val backend = new VhdlBackend
  val tbGen = new VhdlTestBenchBackend()

  def elaborate = {
    val report = backend.elaborate(() => gen)
    tbGen.elaborate(backend, report.toplevel)
    report
  }

  def setCommonClockDomainConfig(clockDomainConfig: ClockDomainConfig): this.type = {
    backend.globalData.commonClockConfig = clockDomainConfig
    this
  }

  def setDefaultClockFrequency(f : IClockDomainFrequency): Unit ={
    backend.defaultClockDomainFrequency = f
  }

  def setLibrary(name: String): this.type = {
    backend.library = name
    this
  }

  def setSpinalPackage(name: String): this.type = {
    backend.packageName = name
    this
  }

  def setEnumPackage(name: String): this.type = {
    backend.enumPackageName = name
    this
  }
  def onlyStdLogicVectorTopLevelIo : this.type = {
    backend.onlyStdLogicVectorTopLevelIo = true
    this
  }
  def setOutputFile(name: String): this.type = {
    backend.outputFile = name
    this
  }


  def forceMemToBlackboxTranslation: this.type = {
    backend.forceMemToBlackboxTranslation = true
    this
  }

  def setTbOutputFile(name: String): this.type = {
    tbGen.outputFile = name
    this
  }

  def setTbName(name: String): this.type = {
    tbGen.tbName = name
    this
  }

  def nothing: this.type = {
    this
  }
}

