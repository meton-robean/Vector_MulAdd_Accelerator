//see LICENSE for license

package sha3
import chisel3._
import chisel3.util.Cat

/*
t = st[1];
for (i = 0; i < 24; i++) {
   j = keccakf_piln[i];
   bc[0] = st[j];
   st[j] = ROTL64(t, keccakf_rotc[i]);
   t = bc[0];
}
*/

class RhoPiModule(val w: Int = 64) extends Module {
	val io = IO(new Bundle {
		val state_i = Input(Vec(25, Bits(w.W)))
		val state_o = Output(Vec(25, Bits(w.W)))
	})

	for (i <- 0 until 5) {
		for (j <- 0 until 5) {
			val temp = Wire(UInt(w.W))
			if ((RHOPI.tri(i*5+j)%w) == 0) {
				temp := io.state_i(i*5+j)
			} else {
				temp := Cat(io.state_i(i*5+j)((w-1) - (RHOPI.tri(i*5+j)-1)%w,0),io.state_i(i*5+j)(w-1,w-1 - ((RHOPI.tri(i*5+j)-1)%w)))
			}
			io.state_o(j*5+((2*i+3*j)%5)) := temp
		}
	}
}
