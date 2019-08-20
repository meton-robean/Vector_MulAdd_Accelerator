package kernel

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config._


class KernelController(val w: Int = 32, val n: Int = 8)(implicit p: Parameters) extends Module {

	val io = IO(new Bundle {
		/// for RoCC communication
		val rocc_req_rdy = Output(Bool())
		val rocc_req_val = Input(Bool())
		val rocc_fire    = Input(Bool())
		val rocc_funct   = Input(Bits(2.W))
		val rocc_rs1     = Input(Bits(64.W))
		val rocc_rs2     = Input(Bits(64.W))
		val rocc_rd      = Input(Bits(5.W))
		val resp_rd      = Output(Bits(5.W))
		val resp_data    = Output(UInt(w.W))
		val resp_valid   = Output(Bool())
        val resp_ready   = Input(Bool())

		/// for memory controller to read/write data through the RoCC interface
		val dmem_req_rdy  =  Input(Bool())
		val dmem_req_val  =  Output(Bool())
		val dmem_req_tag  =  Output(UInt(7.W))
		val dmem_req_cmd  =  Output(UInt(M_SZ.W))
		val dmem_req_typ  =  Output(UInt(MT_SZ.W))
		val dmem_req_addr =  Output(UInt(32.W))

		val dmem_resp_val  =  Input(Bool())
		val dmem_resp_tag  =  Input(UInt(7.W))
		val dmem_resp_data =  Input(UInt(w.W))

		/// control signals
        val busy   = Output(Bool())

        /// for computing unit 
		val enable = Output(Bool())
		val vec1   = Output(Vec(n, UInt(w.W)))
		val vec2   = Output(Vec(n, UInt(w.W)))
		val VecMulAdd_result =  Input(UInt(w.W))
		val valid_VecMulAdd_result = Input(Bool())
	})

    /// register
    val x_addr = RegInit(0.U(64.W))    // x_addr is the base addr of the first vector
	val y_addr = RegInit(0.U(64.W))    // y_addr is the base addr of second vector
    val xindex = RegInit(0.U(32.W))    // vector x index
	val yindex = RegInit(0.U(32.W))    // vector y index
    val rd_reg_addr = RegInit(0.U(5.W))
    val vec_len = RegInit(0.U(64.W))
    val x_data  = RegInit(Vec.fill(n)(0.U(w.W)))
    val y_data  = RegInit(Vec.fill(n)(0.U(w.W)))
    val result_data = RegInit(0.U(w.W))


    /// initialize output signals
    val busy = RegInit(false.B)
	io.busy         := busy
	io.dmem_req_val := false.B
	io.dmem_req_tag := 0.U(7.W)
	io.dmem_req_cmd := M_XRD            // load; M_XWR for stores
	io.dmem_req_typ := MT_W             // D = 8 bytes, W = 4, H = 2, B = 1
	io.dmem_req_addr:= 0.U(64.W)
	io.rocc_req_rdy := !busy            // when busy reg  is ture, Rocc stops to fetch cmd from Queue 
    io.resp_rd      := rd_reg_addr
	io.resp_valid   := false.B  
    io.resp_data    := result_data
    for(i <- 0 until n){
        io.vec1(i) := x_data(i)
        io.vec2(i) := y_data(i)
    }
    io.enable := false.B


    /// mem state init
    val m_idle :: m_read_x :: m_wait_x :: m_read_y :: m_wait_y ::Nil = Enum(5)   
    val m_state = RegInit(m_idle)

    /// main state init
    val s_idle  :: s_compute :: s_compute_wait :: s_compute_done ::s_resp_rocc :: Nil =Enum(5)
    val state = RegInit(s_idle)


	/// decode the rocc instruction
	when (io.rocc_req_val && !busy && io.rocc_fire) {  //rocc_req fire! cmd comes!

        io.busy := true.B 
		when (io.rocc_funct === 0.U) {     
			//io.rocc_req_rdy := true.B
			x_addr := io.rocc_rs1        //rs1
			y_addr := io.rocc_rs2        //rs2
            
             
		} .elsewhen (io.rocc_funct === 1.U) {
			//io.rocc_req_rdy := true.B
			vec_len := io.rocc_rs1
            rd_reg_addr := io.rocc_rd    //rd
            busy := true.B               //when busy reg  is ture,  stop fetching cmds from Rocc 
            
		} 

	}

    /// fetch data from memory
    switch(m_state){
        is(m_idle){
            val canRead = busy && (xindex < vec_len) && (yindex < vec_len) && (vec_len =/= 0.U)
            when(canRead){
                printf("rs1: %x, rs2: %x\n", x_addr, y_addr)               //debug
                printf("vec_len: %d,    rd: %x\n", vec_len, rd_reg_addr )  //debug
                printf("mem read begin!,  status--** busy reg:%d\n", busy) //debug
                m_state := m_read_x
            }otherwise{
                m_state := m_idle
            }
        }
        is( m_read_x){                                      //read x data
            when( xindex < vec_len){
                io.dmem_req_val := xindex < vec_len
                io.dmem_req_addr:= x_addr
                io.dmem_req_tag := xindex
                io.dmem_req_cmd := M_XRD
                io.dmem_req_typ := MT_W
                when(io.dmem_req_rdy && io.dmem_req_val) {  //read data if ready and valid
                    m_state := m_wait_x                     //wait for data's coming
                } .otherwise {
                    m_state := m_read_x
                }                

            }.otherwise{
                m_state := m_read_y                         //begin to read y data 
            }
              
        }
        is(m_wait_x){
            when (io.dmem_resp_val) {
				x_data(xindex) := io.dmem_resp_data         //put the recieved data into buffer 
                //printf("io.mem.resp_data for x vector :%d\n", io.dmem_resp_data) //debug
                xindex := xindex + 1.U
                x_addr := x_addr + 4.U                      //int type is 4 bytes
                m_state := m_read_x

            }.otherwise{
                
                m_state := m_wait_x                         //data is not coming , just wait!
            }

        }
        is( m_read_y){                                      //read y data
            when( yindex < vec_len){
                // printf("y_data[%d] : %d\n", yindex, y_data(yindex)) //debug
                io.dmem_req_val := yindex < vec_len
                io.dmem_req_addr:= y_addr
                io.dmem_req_tag := yindex
                io.dmem_req_cmd := M_XRD
                io.dmem_req_typ := MT_W
                // read data if ready and valid
                when(io.dmem_req_rdy && io.dmem_req_val) {
                    m_state := m_wait_y                     //wait until reading done
                } .otherwise {
                    m_state := m_read_y
                }                     
            }.otherwise{
                m_state := m_idle  //finish memory data fetching!! but not reset xindex , yindex until next cmd fecthing!
            } 
        }
        is( m_wait_y){
            when (io.dmem_resp_val) {
				y_data(yindex) := io.dmem_resp_data          // put the recieved data into buffer
                //printf("io.mem.resp_data for y vector :%d\n", io.dmem_resp_data)
                yindex := yindex + 1.U
                y_addr := y_addr + 4.U                      //int type is 4 bytes
                m_state  := m_read_y
                
            }.otherwise{
                m_state := m_wait_y
            }

        }

    }
    


    ///main ctrl 
    switch(state){
        is(s_idle){
            val canCompute = (m_state === m_idle) && (xindex === vec_len) && (yindex === vec_len) && (vec_len =/= 0.U)
            when(canCompute){
                printf("vec1:")//debug
                for(i <- 0 until n){
                    printf(" %d ", x_data(i))
                }
                printf("\n")

                printf("vec2:")
                for(i <- 0 until n){
                    printf(" %d ", y_data(i))
                }
                printf("\n")

                state := s_compute

            }.otherwise{
                state := s_idle
            }
        }
        is(s_compute){
            //printf("s_compute\n")
            io.enable := true.B                          //enbale compute

            state := s_compute_wait
        }
        is(s_compute_wait){
            //printf("s_compute_wait\n")
            when( io.valid_VecMulAdd_result){            //compute done!!
                result_data := io.VecMulAdd_result
                state := s_resp_rocc
            }.otherwise{
                state :=s_compute_wait
            }
        }
        is(s_resp_rocc){
            printf("result: %d\n", result_data)           //debug
            io.enable     := false.B
            io.resp_valid := true.B
            //printf("cmt: io.resp_rd: %d\n",io.resp_rd ) //debug
            io.resp_data  := result_data
            when(io.resp_ready){
                state := s_compute_done
            }.otherwise{
                state := s_resp_rocc
            }
            
        }
        is(s_compute_done){
            printf("s_compute_done\n")
            io.resp_valid := false.B
            busy   := false.B                              //finish! fetching next cmd 
            xindex := 0.U                                  //reset
            yindex := 0.U
            state  := s_idle
        }

    }
    
}