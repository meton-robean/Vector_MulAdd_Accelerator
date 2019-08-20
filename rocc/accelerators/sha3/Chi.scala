//see LICENSE for license
package sha3
import chisel3._

/*
for (j = 0; j < 25; j += 5) 
{
	for (i = 0; i < 5; i++)
		bc[i] = st[j + i];
	for (i = 0; i < 5; i++)
		st[j + i] ^= (~bc[(i + 1) % 5]) & bc[(i + 2) % 5];
}
*/

class ChiModule(val w: Int = 64) extends Module {
	val io = IO(new Bundle {
		val state_i = Input(Vec(25, UInt(w.W)))
		val state_o = Output(Vec(25, UInt(w.W)))
	})

	for (i <- 0 until 5) {
		for (j <- 0 until 5) {
			io.state_o(i*5+j) := io.state_i(i*5+j) ^ 
				//(~io.state_i(i*5+((j+1)%5)) & io.state_i(i*5+((j+2)%5)))
				((~io.state_i(((i+1)%5)*5+((j)%5))) & io.state_i(((i+2)%5)*5+((j)%5)))
		}
	}
}

