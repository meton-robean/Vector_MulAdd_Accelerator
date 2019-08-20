//see LICENSE for license
package spmv

import chisel3._

object BLOCK {
	val N = 8
}

object LATENCY {
	val compute = 2
	val store   = 1
}
