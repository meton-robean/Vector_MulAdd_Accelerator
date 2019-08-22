package floatingpoint

import scala.math.pow

import chisel3._
import chisel3.util._

object FloatingPoint {

    val nan :: an :: ninfinity :: infinity :: Nil = Enum(4)

}

class FloatingPoint(exp: Int, man: Int) extends Bundle {

    val sign = Bool()
    val exponent = UInt(exp.W)
    val mantissa = UInt(man.W)

    def identify(f: FloatingPoint): UInt = {
        val res = Wire(UInt(2.W))
        when(f.exponent === ("b"+("1"*exp)).U) {
            when(f.mantissa === 0.U) {
                when(f.sign === 0.U) {
                    res := FloatingPoint.infinity
                }
                .otherwise{
                    res := FloatingPoint.ninfinity
                }
            }
            .otherwise{
                res := FloatingPoint.nan
            }
        }
        .otherwise {
            res := FloatingPoint.an
        }
        return res
    }

    def absoluteValue(a: SInt): UInt = {
        val res = Wire(UInt())
        when(a < 0.S) {
            res := (-a).asUInt
        }
        .otherwise {
            res := a.asUInt
        }
        return res
    }

    def toInt(a: UInt, s: UInt): SInt = {
        val res = Wire(SInt())
        when(s(0).toBool) {
            res := -(a.asSInt)
        }
        .otherwise{
            res := a.asSInt
        }
        return res
    }

    override def cloneType = (new FloatingPoint(this.exp, this.man)).asInstanceOf[this.type]

    def +(that: FloatingPoint): FloatingPoint = {
        val res = Wire(new FloatingPoint(this.exp, this.man))
        // SETUP
        val Aidentity = Wire(UInt(2.W))
        val Bidentity = Wire(UInt(2.W))
        Aidentity := identify(this)
        Bidentity := identify(that)
        when((Aidentity === FloatingPoint.infinity & Bidentity === FloatingPoint.ninfinity)|(Bidentity === FloatingPoint.infinity & Aidentity === FloatingPoint.ninfinity)) {
            res := (("b"+("1"*(1+exp+1))+("0"*(man-1))).U).asTypeOf(new FloatingPoint(this.exp, this.man))
        }
        .elsewhen(Aidentity === FloatingPoint.infinity | Aidentity === FloatingPoint.ninfinity) {
            res := this
        }
        .elsewhen(Bidentity === FloatingPoint.infinity | Bidentity === FloatingPoint.ninfinity) {
            res := that
        }
        .otherwise {
            // INIT
            val exponent   = Wire(UInt(this.exp.W))
            val difference = Wire(UInt(this.exp.W))
            val a_mantissa = Wire(UInt((this.man+3).W))
            val b_mantissa = Wire(UInt((this.man+3).W))
            val mantissa   = Wire(UInt((this.man+3).W))
            val sum        = Wire(SInt((this.man+3).W))
            difference := absoluteValue((this.exponent).asSInt-(that.exponent).asSInt)
            // SHIFT EXPONENT
            when(this.exponent > that.exponent) {
                exponent := this.exponent
                a_mantissa := Cat(1.U(1.W), this.mantissa)
                b_mantissa := Cat(1.U(1.W), that.mantissa) >> difference
            }
            .otherwise{
                exponent := that.exponent;
                a_mantissa := Cat(1.U(1.W), this.mantissa) >> difference
                b_mantissa := Cat(1.U(1.W), that.mantissa)
            }
            sum := toInt(a_mantissa, this.sign)+toInt(b_mantissa, that.sign)
            mantissa := absoluteValue(sum)
            // MANTISSA COMPUTATION
            res.mantissa := Mux(mantissa(man+1), ((mantissa)+1.U)>>1.U, mantissa)(man-1, 0)
            // SIGN MANAGMENT
            res.sign := (sum < 0.S)
            // EXPONENT MANAGMENT
            res.exponent := Mux(sum =/= 0.S, exponent, 0.U)+Cat(0.U((exp-1).W), mantissa(man+1))
        }
        return res
    }

    def *(that: FloatingPoint): FloatingPoint = {
        // SETUP
        val res = Wire(new FloatingPoint(this.exp, this.man))
        val Aidentity = Wire(UInt(2.W))
        val Bidentity = Wire(UInt(2.W))
        Aidentity := identify(this)
        Bidentity := identify(that)
        when(Aidentity === FloatingPoint.infinity | Aidentity === FloatingPoint.ninfinity | Bidentity === FloatingPoint.infinity | Bidentity === FloatingPoint.ninfinity) {
            res := (Cat((this.sign^that.sign), ("b0"+("1"*(exp))+("0"*(man))).U((exp+man).W))).asTypeOf(new FloatingPoint(this.exp, this.man))
        }
        .elsewhen(Aidentity === FloatingPoint.nan) {
            res := (Cat(this.sign, ("b"+("1"*(exp+1))+("0"*(man-1))).U((exp+man).W))).asTypeOf(new FloatingPoint(this.exp, this.man))
        }
        .elsewhen(Bidentity === FloatingPoint.nan) {
            res := (Cat(that.sign, ("b"+("1"*(exp+1))+("0"*(man-1))).U((exp+man).W))).asTypeOf(new FloatingPoint(this.exp, this.man))
        }
        .elsewhen(this.asUInt === 0.U | that.asUInt === 0.U | this.asUInt === ("b1"+("0"*(exp+man))).U | that.asUInt === ("b1"+("0"*(exp+man))).U) {
            res := (0.U).asTypeOf(new FloatingPoint(this.exp, this.man))
        }
        .otherwise {
            res.sign := (this.sign^that.sign)
            val mantissa = (Cat(1.U(1.W), this.mantissa)*Cat(1.U(1.W), that.mantissa)) >> man.U
            res.mantissa := Mux(mantissa(man+1).toBool, (mantissa+1.U)>>1.U, mantissa)(man-1, 0)
            val exponent = this.exponent+that.exponent
            res.exponent := exponent-((pow(2, exp-1)-1).toInt).U+mantissa(man+1)
        }
        return res
    }

}
