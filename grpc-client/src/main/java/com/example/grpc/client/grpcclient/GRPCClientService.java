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
		initializeStubs();
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
		String resp = getResponse(replies);
		return resp;
    }

	public String mult(){
		initializeStubs();
		ArrayList<MatrixReply> final_replies = new ArrayList<>();
		ArrayList<MatrixReply> mult_replies = new ArrayList<>();
		final int MAX_SERVER = 7;
		int current_server = 0;
		int[][][][] blocks1 = createBlocks2(matrix1);
		int[][][][] blocks2 = createBlocks2(matrix2);
		int size = (int)Math.sqrt(blocks1.length);
		MatrixReply current_reply = MatrixReply.newBuilder().setC00(0).setC01(0).setC10(0).setC11(0).build();
		int [][] accumulator = new int[2][2];
		for (int i=0; i<size; i++){
			for (int j=0; j<size; j++){
				for (int k=0; k<size; k++){
					int [][] current_block1 = blocks1[i][k];
					int [][] current_block2 = blocks2[k][j];
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
					current_server++;
					if (current_server==MAX_SERVER) current_server=0;
				}
			}
		}
		current_server=0;
		MatrixReply prev_reply = null;

		int rows=size*2;
		int blocks_per_row=rows/2;

		
		int row_n=1;
		for (int i = 0; i < mult_replies.size(); i+=blocks_per_row) {
			for (int j=i;j<blocks_per_row*row_n;j+=2) {
				if (j==i) {
					prev_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(mult_replies.get(j).getC00())
					.setA01(mult_replies.get(j).getC01())
					.setA10(mult_replies.get(j).getC10())
					.setA11(mult_replies.get(j).getC11())
					.setB00(mult_replies.get(j+1).getC00())
					.setB01(mult_replies.get(j+1).getC01())
					.setB10(mult_replies.get(j+1).getC10())
					.setB11(mult_replies.get(j+1).getC11())
					.build());
				} else {
					prev_reply = stubs[current_server].addBlock(MatrixRequest.newBuilder()
					.setA00(prev_reply.getC00())
					.setA01(prev_reply.getC01())
					.setA10(prev_reply.getC10())
					.setA11(prev_reply.getC11())
					.setB00(mult_replies.get(j).getC00())
					.setB01(mult_replies.get(j).getC01())
					.setB10(mult_replies.get(j).getC10())
					.setB11(mult_replies.get(j).getC11())
					.build());
					j--;
				}
			}
			final_replies.add((prev_reply)); 
			
			
			row_n++;
			current_server++;
			//start again from server 0
			if (current_server==MAX_SERVER) {
				current_server=0;
			}
		}

		// current_reply = stubs[current_server_add].addBlock(MatrixRequest.newBuilder()
		// .setA00(current_mult.getC00())
		// .setA01(current_mult.getC01())
		// .setA10(current_mult.getC10())
		// .setA11(current_mult.getC11())
		// .setB00(current_reply.getC00())
		// .setB01(current_reply.getC01())
		// .setB10(current_reply.getC10())
		// .setB11(current_reply.getC11())
		// .build());
		String resp = getResponse(final_replies);
		System.out.println(final_replies.size());
		return resp;
    }

	public String processMatrices(String string_matrix1, String string_matrix2, RedirectAttributes redirectAttributes){
		try{
			String [] rows1 = string_matrix1.split("\n");
			String [] rows2 = string_matrix2.split("\n");
			//Check size is a power of 2
			if (!checkPowerOfTwo(rows1.length) || !checkPowerOfTwo(rows2.length)){
				redirectAttributes.addFlashAttribute("message", "Please make sure matrices' size is a power of 2.");
				return "redirect:/";
			}
			//Check if provided matrices are square
			boolean isSquare1 = check_square_matrix(rows1);
			boolean isSquare2 = check_square_matrix(rows2);
			if (!isSquare1 || !isSquare2){
				redirectAttributes.addFlashAttribute("message", "Please make sure you provide square matrices.");
				return "redirect:/";
			}
			//We know matrices are square, so we can check if they have same size by simply checking
			//that the numner of rows is the same.
			if (rows1.length!=rows2.length){
				redirectAttributes.addFlashAttribute("message", "Please make sure matrices have the same size.");
				return "redirect:/";
			}
			matrix1 = buildMatrix(rows1);
			matrix2 = buildMatrix(rows2);
			blocks_1 = createBlocks(matrix1);
			blocks_2 = createBlocks(matrix2);
			//redirectAttributes.addFlashAttribute("message", "Files successfully uploaded!");
			return "redirect:/display";
		}
		catch(Exception e){
			System.out.println("Exception in processMatrices");
			System.out.println(e.getMessage());
		}
		return "redirect:/";
	}

	private void printMatrix(int [][] matrix){
		for (int row=0; row<matrix.length; row++){
			for(int column=0; column<matrix[row].length; column++){
				System.out.print(matrix[row][column]);
				System.out.print(" ");
			}
			System.out.println();
		}
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

	private void printArray(String[] array){
		for (String element : array){
			System.out.println(element);
		}
	}

	private static int[][][][] createBlocks2(int matrix[][]){
		final int N_BLOCKS = (int) Math.pow((matrix.length/2), 2);
        //int [][] b = new int[N_BLOCKS*2][N_BLOCKS*2];
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

	private ArrayList<int[][]> createBlocks(int matrix[][]){
		try{
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
		catch(Exception e){
			System.out.println("createblocks\n"+e.getMessage());
			return null;
		}
	}

	private String assembleBack(ArrayList<MatrixReply> replies){
		String res = "";
        int length = (int)Math.sqrt((double)replies.size());

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++){
                MatrixReply cur = resps.get(i+j);
                res = res + cur.getC00() +" "+ cur.getC01() + " ";
            }
            res = res.strip();
            res = res + "<br>";
            for (int j = 0; j < length; j++){
                MatrixReply cur = resps.get(i+j);
                res = res + cur.getC10() +" "+ cur.getC11() + " ";
            }
            res = res.strip();
            if (i == length-1){
                res = res + "\n";
            } else {res = res + "<br>";}
            
        }
        return res;
	}

	// private String getResponse (ArrayList<MatrixReply> replies){
	// 	final double SIZE = 2*Math.sqrt(replies.size());
	// 	double row = 0;
	// 	int reply_index1 = 0;
	// 	int reply_index2 = 1;
	// 	String response = "";
	// 	MatrixReply current_reply;
	// 	while (row<SIZE){
	// 		current_reply = replies.get(reply_index1);
	// 		response = response + current_reply.getC00()+" "+current_reply.getC01()+" ";
	// 		current_reply = replies.get(reply_index2);
	// 		response = response + current_reply.getC00()+" "+current_reply.getC01()+"<br>";
	// 		current_reply = replies.get(reply_index1);
	// 		response = response + current_reply.getC10()+" "+current_reply.getC11()+" ";
	// 		current_reply = replies.get(reply_index2);
	// 		response = response + current_reply.getC10()+" "+current_reply.getC11()+"<br>";
	// 		reply_index1+=2;
	// 		reply_index2+=2;
	// 		row+=2;
	// 	}
	// 	return response;
	// }

	private void initializeStubs(){
		for (int i=0; i<IPS.length; i++){
			ManagedChannel channel = ManagedChannelBuilder.forAddress(IPS[i],9090)
			.usePlaintext()
			.build();
			MatrixServiceGrpc.MatrixServiceBlockingStub stub
			= MatrixServiceGrpc.newBlockingStub(channel);
			stubs[i] = stub;
		}
	}
}
