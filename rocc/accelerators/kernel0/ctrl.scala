package kernel0

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
        val dmem_req_data =  Output(UInt(w.W))//write data to cache

		val dmem_resp_val  =  Input(Bool())
		val dmem_resp_tag  =  Input(UInt(7.W))
		val dmem_resp_data =  Input(UInt(w.W))

		/// control signals
        val busy   = Output(Bool())

        /// for computing unit 
		val enable = Output(Bool())
		val vec1   = Output(Vec(n, UInt(w.W)))
		val vec2   = Output(Vec(n, UInt(w.W)))
		val VecMulAdd_result =  Input(Vec(n,UInt(w.W)))
		val valid_VecMulAdd_result = Input(Bool())
        val tree_result = Input(UInt(w.W))
	})

    /// register
    val instr_addr = RegInit(0.U(64.W))
    val x_addr = RegInit(0.U(64.W))    // x_addr is the base addr of the first vector
	val y_addr = RegInit(0.U(64.W))    // y_addr is the base addr of second vector
    val z_addr = RegInit(0.U(32.W))     //z_addr is the base addr of destination operand
    val x_index = RegInit(0.U(32.W))    // vector x index
	val y_index = RegInit(0.U(32.W))    // vector y index
    val z_index = RegInit(0.U(32.W))    // write back index
    val x_offset = RegInit(0.U(32.W))
    val y_offset = RegInit(0.U(32.W))
    val z_offset = RegInit(0.U(32.W))

    val x_i = RegInit(0.U(32.W))
    val x_j = RegInit(0.U(32.W))
    val x_k = RegInit(0.U(32.W))
    val cx_i = RegInit(0.U(32.W))
    val cx_j = RegInit(0.U(32.W))
    val cx_k = RegInit(0.U(32.W))

    val y_i = RegInit(0.U(32.W))
    val y_j = RegInit(0.U(32.W))
    val y_k = RegInit(0.U(32.W))
    val cy_i = RegInit(0.U(32.W))
    val cy_j = RegInit(0.U(32.W))
    val cy_k = RegInit(0.U(32.W))

    val z_i = RegInit(0.U(32.W))
    val z_j = RegInit(0.U(32.W))
    val z_k = RegInit(0.U(32.W))
    val cz_i = RegInit(0.U(32.W))
    val cz_j = RegInit(0.U(32.W))
    val cz_k = RegInit(0.U(32.W))

    val counts_P = RegInit(0.U(32.W))
    val counts_Q = RegInit(0.U(32.W))
    val counts_R = RegInit(0.U(32.W))

    val index = RegInit(0.U(32.W))      //total index for the total vector
    val counts = RegInit(0.U(32.W))
    val index_instr = RegInit(0.U(32.W))
    
    val inner_len = RegInit(0.U(32.W))
    val true_len = RegInit(0.U(32.W))
    val can_compute_len = RegInit(n.U(32.W))

    val instruction = RegInit(VecInit(Seq.fill(16)(0.asUInt(32.W))))
    val x_data  = RegInit(VecInit(Seq.fill(n)(0.asUInt(w.W))))
    val y_data  = RegInit(VecInit(Seq.fill(n)(0.asUInt(w.W))))
    val z_data  = RegInit(VecInit(Seq.fill(n)(0.asUInt(w.W))))

    val result_data = RegInit(0.U(w.W))
    val rd_reg_addr = RegInit(0.U(5.W))
    //val delay = RegInit(0.U(32.W))


    /// initialize output signals
    val busy = RegInit(false.B)
    val read_goon = RegInit(false.B)
    val read_instr = RegInit(false.B)
	io.busy         := busy
	io.dmem_req_val := false.B
	io.dmem_req_tag := 0.U(7.W)
	io.dmem_req_cmd := M_XRD            // load; M_XWR for stores
	io.dmem_req_typ := MT_D             // D = 8 bytes, W = 4, H = 2, B = 1
	io.dmem_req_addr:= 0.U(32.W)
    io.dmem_req_data:= 0.U(32.W)
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
    val m_idle :: m_read_instr :: m_wait_instr :: m_access :: m_read_x :: m_wait_x :: m_read_y :: m_wait_y ::Nil = Enum(8)   
    val m_state = RegInit(m_idle)

    /// main state init
    val s_idle  :: s_compute :: s_compute_wait :: s_compute_done ::s_resp_rocc :: s_write :: s_wait_write :: Nil =Enum(7)
    val state = RegInit(s_idle)


	/// decode the rocc instruction
	when (io.rocc_req_val && !busy && io.rocc_fire) {  //rocc_req fire! cmd comes!

        io.busy := true.B 
		when (io.rocc_funct === 1.U) {
			instr_addr := io.rocc_rs1
            rd_reg_addr := io.rocc_rd    //rd
            busy := true.B               //when busy reg  is ture,  stop fetching cmds from Rocc
            read_instr := true.B             
		}

	}

    /// fetch data from memory
    switch(m_state){
        is(m_idle){
            //val canRead = read_instr && busy && (index < vec_len)  && (vec_len =/= 0.U)
            when(read_instr){
                m_state := m_read_instr
                read_instr := false.B
            }.otherwise{
                m_state := m_idle
            }
        }
        is(m_read_instr){
            when(index_instr < 16.U){
                io.dmem_req_val := true.B
                io.dmem_req_addr:= instr_addr + (index_instr << 2.U)
                io.dmem_req_tag := index_instr
                io.dmem_req_cmd := M_XRD
                io.dmem_req_typ := MT_W

                when(io.dmem_req_rdy && io.dmem_req_val) {
					m_state := m_wait_instr // wait until reading done
				} .otherwise {
					m_state := m_read_instr
				}
            }.otherwise{        //decode
                printf("instruction:")//debug
                for(i <- 0 until 16){
                    printf("%d,", instruction(i))
                }
                printf("\n")
                //@cmt 三层嵌套循环 各层的迭代次数
                counts_P := instruction(0)
                counts_Q := instruction(1)
                counts_R := instruction(2)

                //@cmt 总的迭代次数
                counts := instruction(0) * instruction(1) * instruction(2)
                inner_len := instruction(2)

                x_addr := instruction(7)
                y_addr := instruction(11)
                z_addr := instruction(3)
                ///cx_i,cx_j,cx_k
                when(instruction(8) === 0.U){   ///0
                    cx_k := 0.U
                    when(instruction(9) === 0.U){   ///00
                        cx_j := 0.U
                        when(instruction(10) === 0.U){ ///000
                            cx_i :=0.U
                        }.otherwise{    ///001
                            cx_i :=1.U
                        }
                    }.elsewhen(instruction(10) === 0.U){    ///010
                            cx_j :=1.U
                            cx_i :=0.U
                    }.otherwise{    ///011
                            cx_j := instruction(2)  ///R
                            cx_i :=1.U
                    }
                }.elsewhen(instruction(9) === 0.U){ ///10
                    cx_j :=0.U
                    when(instruction(10) === 0.U){  ///100
                        cx_k := 1.U
                        cx_i := 0.U
                    }.otherwise{                    ///101
                        cx_k := instruction(2)  ///R
                        cx_i := 1.U
                    }
                }.otherwise{                    ///11
                    when(instruction(10) === 0.U){  ///110
                        cx_k := instruction(1)  ///Q
                        cx_j := 1.U
                        cx_i := 0.U

                    }.otherwise{                    ///111
                        cx_k := instruction(2) * instruction(1)  ///R*Q
                        cx_j := instruction(2)      ///R
                        cx_i := 1.U
                    }
                }

                ///cy_i,cy_j,cy_k
                when(instruction(12) === 0.U){   ///0
                    cy_k := 0.U
                    when(instruction(13) === 0.U){   ///00
                        cy_j := 0.U
                        when(instruction(14) === 0.U){ ///000
                            cy_i :=0.U
                        }.otherwise{    ///001
                            cy_i :=1.U
                        }
                    }.elsewhen(instruction(14) === 0.U){    ///010
                            cy_j :=1.U
                            cy_i :=0.U
                    }.otherwise{    ///011
                            cy_j := instruction(2)  ///R
                            cy_i :=1.U
                    }
                }.elsewhen(instruction(13) === 0.U){ ///10
                    cy_j :=0.U
                    when(instruction(14) === 0.U){  ///100
                        cy_k := 1.U
                        cy_i := 0.U
                    }.otherwise{                    ///101
                        cy_k := instruction(2)  ///R
                        cy_i := 1.U
                    }
                }.otherwise{                    ///11
                    when(instruction(14) === 0.U){  ///110
                        cy_k := instruction(1)  ///Q
                        cy_j := 1.U
                        cy_i := 0.U

                    }.otherwise{                    ///111
                        cy_k := instruction(2) * instruction(1)  ///R*Q
                        cy_j := instruction(2)      ///R
                        cy_i := 1.U
                    }
                }

                ///cz_i,cz_j,cz_k
                when(instruction(4) === 0.U){   ///0
                    cz_k := 0.U
                    when(instruction(5) === 0.U){   ///00
                        cz_j := 0.U
                        when(instruction(6) === 0.U){ ///000
                            cz_i :=0.U
                        }.otherwise{    ///001
                            cz_i :=1.U
                        }
                    }.elsewhen(instruction(6) === 0.U){    ///010
                            cz_j :=1.U
                            cz_i :=0.U
                    }.otherwise{    ///011
                            cz_j := instruction(2)  ///R
                            cz_i :=1.U
                    }
                }.elsewhen(instruction(5) === 0.U){ ///10
                    cz_j :=0.U
                    when(instruction(6) === 0.U){  ///100
                        cz_k := 1.U
                        cz_i := 0.U
                    }.otherwise{                    ///101
                        cz_k := instruction(2)  ///R
                        cz_i := 1.U
                    }
                }.otherwise{                    ///11
                    when(instruction(6) === 0.U){  ///110
                        cz_k := instruction(1)  ///Q
                        cz_j := 1.U
                        cz_i := 0.U

                    }.otherwise{                    ///111
                        cz_k := instruction(2) * instruction(1)  ///R*Q
                        cz_j := instruction(2)      ///R
                        cz_i := 1.U
                    }
                }

                m_state := m_access
                read_goon := true.B
            }

        }
        is(m_wait_instr){
            when (io.dmem_resp_val) {
				instruction(index_instr) := io.dmem_resp_data         //put the recieved data into buffer 
                index_instr := index_instr + 1.U
                m_state := m_read_instr

            }.otherwise{                
                m_state := m_wait_instr                         //data is not coming , just wait!
            }
        }
        is(m_access){
            val canRead = read_goon
            when(canRead){
                printf("*************m_access*********** \n")
                read_goon := false.B
                when(index < counts){
                    when((inner_len - x_i) > can_compute_len){
                        true_len := can_compute_len
                    }.otherwise{
                        true_len := inner_len - x_i
                    }
                }.otherwise{
                    true_len := 0.U
                }
                m_state := m_read_x 
            }.otherwise{
                m_state := m_access
            }
        }
        is( m_read_x){                                      //read x data
            when( x_index < true_len){
                printf("x_addr:%d\n",x_addr + (x_offset << 3))
                io.dmem_req_val := x_index < true_len
                io.dmem_req_addr:= x_addr + (x_offset << 3)
                io.dmem_req_tag := x_index
                io.dmem_req_cmd := M_XRD
                io.dmem_req_typ := MT_D
                when(io.dmem_req_rdy && io.dmem_req_val) {  //read data if ready and valid
                    m_state := m_wait_x                     //wait for data's coming
                    index := index + 1.U
                    x_index := x_index + 1.U
                    x_i := x_i + 1.U
                    when((x_i + 1.U) === counts_R){
                        x_i := 0.U
                        x_j := x_j + 1.U
                        when((x_j + 1.U) === counts_Q){
                            x_j := 0.U
                            x_k := x_k + 1.U
                        }
                    }                    
                } .otherwise {
                    m_state := m_read_x
                }         
            }.otherwise{
                printf("x_data:")//debug
                for(i <- 0 until n){
                    printf("%d,", x_data(i))
                }
                printf("\n")
                m_state := m_read_y                         //begin to read y data 
                printf("true_len:%d\n",true_len)
            }                         
        }
        is(m_wait_x){
            when (io.dmem_resp_val) {
				x_data(x_index-1.U) := io.dmem_resp_data         //put the recieved data into buffer 
                printf("io.mem.resp_data for x vector :%d \n", io.dmem_resp_data) //debug                
                x_offset := x_i * cx_i + x_j * cx_j + x_k * cx_k
                m_state := m_read_x

            }.otherwise{                
                m_state := m_wait_x                         //data is not coming , just wait!
            }
        }
        is( m_read_y){                                      //read y data
            when( y_index < true_len){
                printf("y_addr:%d",y_addr + (y_offset << 3))
                io.dmem_req_val := y_index < true_len
                io.dmem_req_addr:= y_addr + (y_offset << 3)
                io.dmem_req_tag := y_index
                io.dmem_req_cmd := M_XRD
                io.dmem_req_typ := MT_D
                // read data if ready and valid
                when(io.dmem_req_rdy && io.dmem_req_val) {
                    m_state := m_wait_y                     //wait until reading done
                    y_index := y_index + 1.U
                    y_i := y_i + 1.U
                    when((y_i + 1.U) === counts_R){
                        y_i := 0.U
                        y_j := y_j + 1.U
                        when((y_j + 1.U) === counts_Q){
                            y_j := 0.U
                            y_k := y_k + 1.U
                        }
                    }               
                } .otherwise {
                    m_state := m_read_y
                }                     
            }.otherwise{
                printf("y_data:")//debug
                for(i <- 0 until n){
                    printf("%d,", y_data(i))
                }
                printf("\n")
                when((index < counts) ){
                    m_state := m_access  //finish memory data fetching!! but not reset xindex , yindex until next cmd fecthing!
                }.otherwise{
                    m_state := m_idle
                }
            } 
        }
        is( m_wait_y){
            when (io.dmem_resp_val) {
				y_data(y_index-1.U) := io.dmem_resp_data          // put the recieved data into buffer
                printf("io.mem.resp_data for y vector :%d\n", io.dmem_resp_data)
                y_offset := y_i * cy_i + y_j * cy_j + y_k * cy_k
                m_state  := m_read_y
                
            }.otherwise{
                m_state := m_wait_y
            }

        }

    }
    

    ///main ctrl 
    switch(state){
        is(s_idle){
            val canCompute = (x_index === true_len) && (y_index === true_len) && (true_len =/= 0.U)
            when(canCompute){
                //printf("vec1:")//debug
                for(i <- 0 until n){
                    //printf("%d,", x_data(i))
                }
                //printf("\n")

                //printf("vec2:")
                for(i <- 0 until n){
                    //printf("%d,", y_data(i))
                }
                //printf("\n")

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
                result_data := io.tree_result
                for(i <- 0 until n){
                    z_data(i) := io.VecMulAdd_result(i)
                }
                
                //printf("1result(0):%d\n",result_vec(0))
                state := s_write
            }.otherwise{
                state :=s_compute_wait
            }
        }
        is(s_write){
            when(z_index < true_len){
                //printf("z_addr:%d",z_addr + (z_offset << 3))
                io.dmem_req_val := true.B
                io.dmem_req_addr:= z_addr + (z_offset << 3.U)
                io.dmem_req_tag := z_index
                io.dmem_req_cmd := M_XWR
                //io.dmem_req_typ := MT_W
                when(instruction(6) === 1.U){   //p type
                    io.dmem_req_data := z_data(z_index)
                }.otherwise{                    //tree type 
                    io.dmem_req_data := x_data(z_index) + result_data
                }
                
                //printf("z_data%d:%d ",z_index,z_data(z_index))
                when(io.dmem_req_rdy && io.dmem_req_val){                    
                    state := s_wait_write
                    z_index := z_index + 1.U
                    z_i := z_i + 1.U
                    when((z_i + 1.U) === counts_R){
                        z_i := 0.U
                        z_j := z_j + 1.U
                        when((z_j + 1.U) === counts_Q){
                            z_j := 0.U
                            z_k := z_k + 1.U
                        }
                    }  
                }.otherwise{
                    state :=s_write
                }
            }.otherwise{
                state := s_resp_rocc
                /*printf("z_data:")//debug
                for(i <- 0 until n){
                    printf("%d,", z_data(i))
                }
                printf("\n")*/
            }
        }
        is(s_wait_write){
            when(io.dmem_resp_val){ 
                //printf("dmem_resp_tag(%d):%d  \n",wr_index,io.dmem_resp_tag)
                state :=s_write
                z_offset := z_i * cz_i + z_j * cz_j + z_k * cz_k
            }.otherwise{
                state := s_wait_write
            }
        }
        is(s_resp_rocc){
            //printf("result: %d\n", result_data)           //debug
            io.enable     := false.B
            when(index < counts){
                printf("read_goon true!\n")
                read_goon := true.B
                state := s_idle
                x_index := 0.U                                  //reset
                y_index := 0.U
                z_index := 0.U

                for(i <- 0 until n){
                    x_data(i) := 0.U
                    y_data(i) := 0.U
                    z_data(i) := 0.U
                }
                


            }.otherwise{
                io.resp_valid := true.B
                printf("*******io.resp_valid*******\n" ) //debug
                io.resp_data  := result_data
                when(io.resp_ready){
                    state := s_compute_done
                }.otherwise{
                    state := s_resp_rocc
                }
            }
            
            
        }
        is(s_compute_done){
            printf("s_compute_done\n")
            io.resp_valid := false.B
            busy   := false.B                              //finish! fetching next cmd 
            index := 0.U 
            x_index := 0.U                                  //reset
            y_index := 0.U
            true_len := 0.U
            z_index := 0.U
            state  := s_idle
            counts := 0.U
            index_instr := 0.U
            result_data := 0.U
        }

    }
    
}