package com.example.grpc.client.grpcclient;

import com.example.grpc.server.grpcserver.PingRequest;
import com.example.grpc.server.grpcserver.PongResponse;
import com.example.grpc.server.grpcserver.PingPongServiceGrpc;
import com.example.grpc.server.grpcserver.MatrixRequest;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.MatrixServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Service
public class GRPCClientService {
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
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost",9090)
		.usePlaintext()
		.build();
		MatrixServiceGrpc.MatrixServiceBlockingStub stub
		 = MatrixServiceGrpc.newBlockingStub(channel);
		MatrixReply A=stub.addBlock(MatrixRequest.newBuilder()
			.setA00(1)
			.setA01(2)
			.setA10(5)
			.setA11(6)
			.setB00(1)
			.setB01(2)
			.setB10(5)
			.setB11(6)
			.build());
		String resp= A.getC00()+" "+A.getC01()+"<br>"+A.getC10()+" "+A.getC11()+"\n";
		return resp;
    }

	public String processMatrices(String string_matrix1, String string_matrix2, RedirectAttributes redirectAttributes){
		try{
			String [] rows1 = string_matrix1.split("\n");
			String [] rows2 = string_matrix2.split("\n");
			//Check size is a power of 2
			if (!checkPowerOfTwo(rows1.length) || !checkPowerOfTwo(rows2.length)){
				redirectAttributes.addFlashAttribute("message", "Please make sure matrices' size is a power of 2.");
				System.out.println(rows1.length);
				System.out.println(rows2.length);
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
			int [][] matrix1 = new int[rows1.length][rows1.length];
			int [][] matrix2 = new int[rows2.length][rows2.length];
			System.out.println("Building matrix 1");
			matrix1 = buildMatrix(rows1);
			System.out.println("Building matrix 2");
			matrix2 = buildMatrix(rows2);
			// printMatrix(matrix1);
			// printMatrix(matrix2);
		}
		catch(Exception e){

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

			}
		}
		return true;
	}

	private int[][] buildMatrix(String[] rows){
		final int SIZE = rows.length;
		int [][] matrix = new int[SIZE][SIZE];
		try{
			for (int row=0; row<SIZE; row++){
				String [] row_entries = rows[row].split(",");
				for (int column=0; column<SIZE; column++){
					matrix[row][column] = Integer.parseInt(row_entries[column]);
					System.out.print(matrix[row][column]);
				}
				System.out.println();
			}
		}
		catch(Exception e){

		}
		return matrix;
	}
}
