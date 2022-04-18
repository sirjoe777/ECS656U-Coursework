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
		String [] ips = {"10.128.0.7","10.128.0.8","10.128.0.9","10.128.0.10","10.128.0.11","10.128.0.12","10.128.0.13","10.128.0.14"};
		for (int i=0; i<ips.length; i++){
			ManagedChannel channel = ManagedChannelBuilder.forAddress(ips[i],9090)
			.usePlaintext()
			.build();
			MatrixServiceGrpc.MatrixServiceBlockingStub stub
			= MatrixServiceGrpc.newBlockingStub(channel);
			stubs[i] = stub;
		}

		int[][] b1_1 = blocks_1.get(0);
		int[][] b1_2 = blocks_1.get(1);
		int[][] b1_3 = blocks_1.get(2);
		int[][] b1_4 = blocks_1.get(3);

		int[][] b2_1 = blocks_2.get(0);
		int[][] b2_2 = blocks_2.get(1);
		int[][] b2_3 = blocks_2.get(2);
		int[][] b2_4 = blocks_2.get(3);
		MatrixReply A1=stubs[0].addBlock(MatrixRequest.newBuilder()
			.setA00(b1_1[0][0])
			.setA01(b1_1[0][1])
			.setA10(b1_1[1][0])
			.setA11(b1_1[1][1])
			.setB00(b2_1[0][0])
			.setB01(b2_1[0][1])
			.setB10(b2_1[1][0])
			.setB11(b2_1[1][1])
			.build());
		MatrixReply A2=stubs[1].addBlock(MatrixRequest.newBuilder()
			.setA00(b1_2[0][0])
			.setA01(b1_2[0][1])
			.setA10(b1_2[1][0])
			.setA11(b1_2[1][1])
			.setB00(b2_2[0][0])
			.setB01(b2_2[0][1])
			.setB10(b2_2[1][0])
			.setB11(b2_2[1][1])
			.build());
		MatrixReply A3=stubs[4].addBlock(MatrixRequest.newBuilder()
			.setA00(b1_3[0][0])
			.setA01(b1_3[0][1])
			.setA10(b1_3[1][0])
			.setA11(b1_3[1][1])
			.setB00(b2_3[0][0])
			.setB01(b2_3[0][1])
			.setB10(b2_3[1][0])
			.setB11(b2_3[1][1])
			.build());
		MatrixReply A4=stubs[3].addBlock(MatrixRequest.newBuilder()
			.setA00(b1_4[0][0])
			.setA01(b1_4[0][1])
			.setA10(b1_4[1][0])
			.setA11(b1_4[1][1])
			.setB00(b2_4[0][0])
			.setB01(b2_4[0][1])
			.setB10(b2_4[1][0])
			.setB11(b2_4[1][1])
			.build());

		String resp= A1.getC00()+" "+A1.getC01()+" "+A2.getC00()+" "+A2.getC01()+"<br>"+
					 A1.getC10()+" "+A1.getC11()+" "+A2.getC10()+" "+A2.getC11()+"<br>"+
					 A3.getC00()+" "+A3.getC01()+" "+A4.getC00()+" "+A4.getC01()+"<br>"+
					 A3.getC10()+" "+A3.getC11()+" "+A3.getC10()+" "+A3.getC11()+"<br>";
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

	private ArrayList<int[][]> createBlocks(int matrix[][]) {
		ArrayList<int[][]> result = new ArrayList<>();
		int stride = 2;
		int size = matrix.length;
		//Process all rows
		for (int row = 0; row < size - stride + 1; row += 2) {
			//Process all columns
			for (int column = 0; column < size - stride + 1; column += 2) {
				int block[][] = new int[2][2];
				//Use boolean flags to make sure we don't process same entries multiple times
				boolean[][]  processed = new boolean[2][2];
				//Process rows of current sub-matrix
				for (int i = row; i < stride + row; i++) {
					//Keep a counter for the number of iterations
					int iter = 0;
					//Process columns of current sub-matrix
					for (int j = column; j < stride + column; j++) {
						if (iter == 0 && !processed[0][0]) {
							processed[0][0] = true;
							block[0][0] = matrix[i][j];
							iter++;
						}
						else if (iter == 0 && !processed[1][0]) {
							processed[1][0] = true;
							block[1][0] = matrix[i][j];
							iter++;
						}
						else if (iter == 1 && !processed[0][1]) {
							processed[0][1] = true;
							block[0][1] = matrix[i][j];
							iter++;
						}
						else if (iter == 1 && !processed[1][1]) {
							processed[1][1] = true;
							block[1][1] = matrix[i][j];
							iter++;
						}
					}
				}
				result.add(block);
			}
		}
		return result;
	}
}
