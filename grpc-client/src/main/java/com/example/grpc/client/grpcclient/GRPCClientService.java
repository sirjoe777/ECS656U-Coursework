package com.example.grpc.client.grpcclient;
import com.example.grpc.server.grpcserver.PingRequest;
import com.example.grpc.server.grpcserver.PongResponse;
import com.example.grpc.server.grpcserver.PingPongServiceGrpc;
import com.example.grpc.server.grpcserver.MatrixRequest;
import java.util.ArrayList;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.MatrixServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Service
public class GRPCClientService {
	private final String[] IPS = {"10.128.0.7","10.128.0.8","10.128.0.16","10.128.0.10","10.128.0.11","10.128.0.12","10.128.0.13","10.128.0.17"};
	private int [][] matrix1;
	private int [][] matrix2;
	private MatrixServiceGrpc.MatrixServiceBlockingStub [] stubs = new MatrixServiceGrpc.MatrixServiceBlockingStub [8];
	private ArrayList<int[][]> blocks_1 = new ArrayList<>();
	private ArrayList<int[][]> blocks_2 = new ArrayList<>();
	
    public String ping() {
        	ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();        
		PingPongServiceGrpc.PingPongServiceBlockingStub stub
                = PingPongServiceGrpc.newBlockingStub(channel);        
		PongResponse helloResponse = stub.ping(PingRequest.newBuilder()
                .setPing("")
                .build());        
		channel.shutdown();        
		return helloResponse.getPong();
    }
    public String add(){
		createStubs();
		ArrayList<MatrixReply> replies = new ArrayList<>();
		final int MAX_SERVER = 7;
		int current_server = 0;
		for (int i=0; i<blocks_1.size(); i++){
			int [][] current_block1 = blocks_1.get(i);
			int [][] current_block2 = blocks_2.get(i);
			MatrixReply current_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
			.setA00(current_block1[0][0])
			.setA01(current_block1[0][1])
			.setA10(current_block1[1][0])
			.setA11(current_block1[1][1])
			.setB00(current_block2[0][0])
			.setB01(current_block2[0][1])
			.setB10(current_block2[1][0])
			.setB11(current_block2[1][1])
			.build());
			replies.add(current_reply);
			current_server++;
			if(current_server==8) current_server=0; 
		}
		String resp = getFinalResult(replies);
		return resp;
    }

	public String mult(float deadline){
		createStubs();

		//Initialize server to -1 because these will be calculated later with deadline and footprinting
		int max_servers = -1;
		//Convert the provided deadline (if any) to nanoseconds
		long deadline_nano = (long)((Math.pow(10,9))*deadline);
		
		//Base case: the matrices are 2x2 so they consist of a single block
		if(matrix1.length==2){
			MatrixReply reply = stubs[0].multiplyBlock(MatrixRequest.newBuilder()
								.setA00(matrix1[0][0])
								.setA01(matrix1[0][1])
								.setA10(matrix1[1][0])
								.setA11(matrix1[1][1])
								.setB00(matrix2[0][0])
								.setB01(matrix2[0][1])
								.setB10(matrix2[1][0])
								.setB11(matrix2[1][1])
								.build());
			return reply.getC00()+" "+reply.getC01()+"<br>"+reply.getC10()+" "+reply.getC11();
		}

		//If matrices are not 2x2 then use divide and conquer approach
		//described on https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm#Divide-and-conquer_algorithm
		ArrayList<MatrixReply> final_replies = new ArrayList<>();
		ArrayList<MatrixReply> mult_replies = new ArrayList<>();
		int current_server = 0;
		MatrixReply current_reply = MatrixReply.newBuilder().setC00(0).setC01(0).setC10(0).setC11(0).build();

		//Divide matrices into a matrix of 2x2 blocks
		int[][][][] blocks1 = createBlocks2(matrix1);
		int[][][][] blocks2 = createBlocks2(matrix2);
		int size = matrix1.length/2;

		//Perform divide-and-conquer matrix multiplication where each 2x2 block is multiplied using
		//the function provided in the server
		for (int i=0; i<size; i++){
			for (int j=0; j<size; j++){
				for (int k=0; k<size; k++){
					int [][] current_block1 = blocks1[i][k];
					int [][] current_block2 = blocks2[k][j];

					//Calculate time before function call to find footprinting (if needed)
					long start = System.nanoTime();
					MatrixReply current_mult = stubs[current_server].multiplyBlock(MatrixRequest.newBuilder()
					.setA00(current_block1[0][0])
					.setA01(current_block1[0][1])
					.setA10(current_block1[1][0])
					.setA11(current_block1[1][1])
					.setB00(current_block2[0][0])
					.setB01(current_block2[0][1])
					.setB10(current_block2[1][0])
					.setB11(current_block2[1][1])
					.build());

					//If the user provided a deadline and if we haven't initialized the maximum number
					//of servers yet, then use footprinting to calculate it
					if (deadline>0 && max_servers==-1){
						// three nested loops, each "size" number of times, for each of these we call the
						// multiplyBlock function once (we subtract one so we don't account for the 
						// function call we used to calculate footprint)
						int number_of_operations = (int)Math.pow(size,3)-1;
						long end = System.nanoTime();
						long footprint = end-start;
						max_servers = (int)((footprint*number_of_operations)/deadline_nano);

						//Make sure max is between 1 and 8
						if (max_servers<1) max_servers=1;
						else if (max_servers>7) max_servers = 8;
					}
					else{
						max_servers=8;
					}
					mult_replies.add(current_mult);
					current_server++;

					// If index of current server is 8 then reset it back to 0
					if (current_server==max_servers) current_server=0;
				}
			}
		}

		//Reset current server to 0 to add the blocks after multiplication
		current_server=0;
		int row=1;
		for (int i = 0; i < mult_replies.size(); i+=size) {
			for (int j=i;j<size*row;j++) {
				if (j==i) {
					current_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(mult_replies.get(j).getC00())
					.setA01(mult_replies.get(j).getC01())
					.setA10(mult_replies.get(j).getC10())
					.setA11(mult_replies.get(j).getC11())
					.setB00(mult_replies.get(j+1).getC00())
					.setB01(mult_replies.get(j+1).getC01())
					.setB10(mult_replies.get(j+1).getC10())
					.setB11(mult_replies.get(j+1).getC11())
					.build());
					j++;
				} else {
					current_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(current_reply.getC00())
					.setA01(current_reply.getC01())
					.setA10(current_reply.getC10())
					.setA11(current_reply.getC11())
					.setB00(mult_replies.get(j).getC00())
					.setB01(mult_replies.get(j).getC01())
					.setB10(mult_replies.get(j).getC10())
					.setB11(mult_replies.get(j).getC11())
					.build());
				}
			}
			final_replies.add(current_reply); 
			row++;
			current_server++;
			if (current_server==max_servers) {
				current_server=0;
			}
		}
		String resp = getFinalResult(final_replies);
		return resp;
    }

	//Process files, convert the text files into matrices and store them
	//Also check if there is any error in the files (e.g., if matrices have different sizes)
	//and in that case redirect to upload form
	public String processMatrices(String string_matrix1, String string_matrix2, RedirectAttributes redirectAttributes){
		try{
			String [] rows1 = string_matrix1.split("\n");
			String [] rows2 = string_matrix2.split("\n");
			//Check size is a power of 2
			if (!checkPowerOfTwo(rows1.length) || !checkPowerOfTwo(rows2.length)){
				redirectAttributes.addFlashAttribute("message", "Please make sure matrices' size is a power of 2.");
				return "redirect:/";
			}
			//We know matrices are square, so we can check if they have same size by simply checking
			//that the number of rows is the same.
			if (rows1.length!=rows2.length){
				redirectAttributes.addFlashAttribute("message", "Please make sure matrices have the same size.");
				return "redirect:/";
			}
			//Check if provided matrices are square
			boolean isSquare1 = check_square_matrix(rows1);
			boolean isSquare2 = check_square_matrix(rows2);
			if (!isSquare1 || !isSquare2){
				redirectAttributes.addFlashAttribute("message", "Please make sure you provide square matrices.");
				return "redirect:/";
			}
			matrix1 = buildMatrix(rows1);
			matrix2 = buildMatrix(rows2);
			blocks_1 = createBlocks(matrix1);
			blocks_2 = createBlocks(matrix2);
			return "redirect:/display";
		}
		catch(Exception e){
			System.out.println("Exception in processMatrices");
			System.out.println(e.getMessage());
		}
		return "redirect:/";
	}

	//Taken from https://www.geeksforgeeks.org/java-program-to-find-whether-a-no-is-power-of-two/
	private boolean checkPowerOfTwo(int n)
    { 
        while (n != 1) {
            if (n % 2 != 0)
                return false;
            n = n / 2;
        }
        return true;
    }

	private boolean check_square_matrix(String[] rows){
		for (String row : rows){
			try{
				String [] row_entries = row.split(",");
				if (row_entries.length!=rows.length){
					return false;
				}
			}
			catch(Exception e){
				System.out.println("Exception in check_square_matrix");
				System.out.println(e.getMessage());
			}
		}
		return true;
	}

	private int[][] buildMatrix(String[] rows){
		final int SIZE = rows.length;
		int [][] matrix = new int[SIZE][SIZE];
		try{
			for (int row=0; row<SIZE; row++){

				String [] row_entries = rows[row].replaceAll("[^,0-9]","").split(",");
				for (int column=0; column<SIZE; column++){
					matrix[row][column] = Integer.parseInt(row_entries[column]);
				}
			}
		}
		catch(Exception e){
			System.out.println("Exception in buildMatrix");
			System.out.println(e.toString());
		}
		return matrix;
	}

	//Very similar to createBlocks, but here we are creating four-dimensional matrices.
	//This is done so that we have matrices of 2x2 blocks, which can then be used for
	//the divide-and-conquer approach in matrix multiplication
	private int[][][][] createBlocks2(int matrix[][]){
		final int N_BLOCKS = (int) Math.pow((matrix.length/2), 2);
		int row = 0;
		int col = 0;
        int bigger_matrix_row = 0;
        int bigger_matrix_col = 0;
        int [][][][] blocks = new int[N_BLOCKS][N_BLOCKS][2][2];
		int [][] current_block = new int[2][2];
        int ii=0;
        int jj=0;
		while (row<matrix.length){
			while (col<matrix.length){
				for(int i=row; i<row+2; i++){
					for (int j=col; j<col+2; j++){
						current_block[ii][jj] = matrix[i][j];
                        jj++;
					}
                    ii++;
                    jj=0;
				}
                ii=0;
				col = col+2;
                blocks[bigger_matrix_row][bigger_matrix_col] = current_block;
                bigger_matrix_col++;
                current_block = new int[2][2];
			}
            bigger_matrix_row++;
            bigger_matrix_col=0;
			row = row+2;
			col = 0;
		}
		return blocks;
	}

	//Split a matrix into 2x2 blocks and store them in a list
	private ArrayList<int[][]> createBlocks(int matrix[][]){
		final int N_BLOCKS = (int) Math.pow((matrix.length/2), 2);
		int row = 0;
		int col = 0;
		int [][] current_block = new int[2][2];
		int ii=0;
		int jj=0;
		ArrayList<int[][]> result = new ArrayList<>();
		while (row<matrix.length){
			while (col<matrix.length){
				for(int i=row; i<row+2; i++){
					for (int j=col; j<col+2; j++){
						current_block[ii][jj] = matrix[i][j];
						jj++;
					}
					ii++;
					jj=0;
				}
				ii=0;
				col = col+2;
				result.add(current_block);
				current_block = new int[2][2];
			}
			row = row+2;
			col = 0;
		}
		return result;
	}

	//Reassemble the blocks into a matrix and return a String representation of
	//the final result
	private String getFinalResult(ArrayList<MatrixReply> replies) {
		int size = matrix1.length;
		int [][] responses_as_matrix = new int[size][size];
		int k = 0;
		for (int i = 0; i < size; i +=2) {
			for (int j = 0; j <size ; j += 2) {

				responses_as_matrix[i][j] = replies.get(k).getC00();
				responses_as_matrix[i][j + 1] = replies.get(k).getC01();
				responses_as_matrix[i + 1][j] = replies.get(k).getC10();
				responses_as_matrix[i + 1][j + 1] = replies.get(k).getC11();
				k++;
			}
		}
		String response = "";
		for (int i=0; i<responses_as_matrix.length; i++)
    	{
    		for (int j=0; j<responses_as_matrix[i].length;j++)
    		{
    			response+=responses_as_matrix[i][j]+" ";
    		}
    		response+="<br>";
    	}
		return response;
	}

	private void createStubs(){
		for (int i=0; i<IPS.length; i++){
			ManagedChannel channel = ManagedChannelBuilder.forAddress(IPS[i],9090)
			.usePlaintext()
			.build();
			MatrixServiceGrpc.MatrixServiceBlockingStub stub
			= MatrixServiceGrpc.newBlockingStub(channel);
			stubs[i] = stub;
		}
	}

	/* *************************************************************
	                FOR DEMONSTRATION ONLY
	
	This method computes multiplication using a single server.
	Used in video demonstration to show the speedup when multiple 
	servers are used

	*****************************************************************/
	public String simpleMult(){
		createStubs();
		//Base case: the matrices are 2x2 so they consist of a single block
		if(matrix1.length==2){
			MatrixReply reply = stubs[0].multiplyBlock(MatrixRequest.newBuilder()
								.setA00(matrix1[0][0])
								.setA01(matrix1[0][1])
								.setA10(matrix1[1][0])
								.setA11(matrix1[1][1])
								.setB00(matrix2[0][0])
								.setB01(matrix2[0][1])
								.setB10(matrix2[1][0])
								.setB11(matrix2[1][1])
								.build());
			return reply.getC00()+" "+reply.getC01()+"<br>"+reply.getC10()+" "+reply.getC11();
		}

		//If matrices are not 2x2 then use divide and conquer approach
		//described on https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm#Divide-and-conquer_algorithm
		ArrayList<MatrixReply> final_replies = new ArrayList<>();
		ArrayList<MatrixReply> mult_replies = new ArrayList<>();
		int current_server = 0;
		MatrixReply current_reply = MatrixReply.newBuilder().setC00(0).setC01(0).setC10(0).setC11(0).build();

		//Divide matrices into a matrix of 2x2 blocks
		int[][][][] blocks1 = createBlocks2(matrix1);
		int[][][][] blocks2 = createBlocks2(matrix2);
		int size = matrix1.length/2;

		//Perform divide-and-conquer matrix multiplication where each 2x2 block is multiplied using
		//the function provided in the server
		for (int i=0; i<size; i++){
			for (int j=0; j<size; j++){
				for (int k=0; k<size; k++){
					int [][] current_block1 = blocks1[i][k];
					int [][] current_block2 = blocks2[k][j];

					//Calculate time before function call to find footprinting (if needed)
					long start = System.nanoTime();
					MatrixReply current_mult = stubs[current_server].multiplyBlock(MatrixRequest.newBuilder()
					.setA00(current_block1[0][0])
					.setA01(current_block1[0][1])
					.setA10(current_block1[1][0])
					.setA11(current_block1[1][1])
					.setB00(current_block2[0][0])
					.setB01(current_block2[0][1])
					.setB10(current_block2[1][0])
					.setB11(current_block2[1][1])
					.build());
					mult_replies.add(current_mult);
				}
			}
		}
		int row=1;
		for (int i = 0; i < mult_replies.size(); i+=size) {
			for (int j=i;j<size*row;j++) {
				if (j==i) {
					current_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(mult_replies.get(j).getC00())
					.setA01(mult_replies.get(j).getC01())
					.setA10(mult_replies.get(j).getC10())
					.setA11(mult_replies.get(j).getC11())
					.setB00(mult_replies.get(j+1).getC00())
					.setB01(mult_replies.get(j+1).getC01())
					.setB10(mult_replies.get(j+1).getC10())
					.setB11(mult_replies.get(j+1).getC11())
					.build());
					j++;
				} else {
					current_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(current_reply.getC00())
					.setA01(current_reply.getC01())
					.setA10(current_reply.getC10())
					.setA11(current_reply.getC11())
					.setB00(mult_replies.get(j).getC00())
					.setB01(mult_replies.get(j).getC01())
					.setB10(mult_replies.get(j).getC10())
					.setB11(mult_replies.get(j).getC11())
					.build());
				}
			}
			final_replies.add(current_reply); 
			row++;
		}
		String resp = getFinalResult(final_replies);
		return resp;
    }
}
