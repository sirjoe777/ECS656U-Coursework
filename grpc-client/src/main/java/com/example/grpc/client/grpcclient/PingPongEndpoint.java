package com.example.grpc.client.grpcclient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import javax.naming.NamingException;

@Controller
public class PingPongEndpoint {    

	GRPCClientService grpcClientService;    
	@Autowired
    	public PingPongEndpoint(GRPCClientService grpcClientService) {
        	this.grpcClientService = grpcClientService;
    	}    
	@GetMapping("/ping")
    	public String ping() {
        	return grpcClientService.ping();
    	}
        @GetMapping("/add")
	public String add() {
		return grpcClientService.add();
	}
	@GetMapping("/")
	public String home () {
		return "uploadForm.html";
	}
	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException{
		//Make sure a file has been uploaded
		if (file.getBytes().length==0){
			redirectAttributes.addFlashAttribute("message", "Please upload a file containing two matrices!");
			return "redirect:/";
		} 
		else{
			try{
				String content = new String(file.getBytes());
				String [] matrices = content.split("&\\n");
				//Check that exactly two matrices have been uploaded
				if (matrices.length!=2){
					redirectAttributes.addFlashAttribute("message", "Please make sure the file contains exactly two matrices, in the format described in README.");
					return "redirect:/";
				}
				else{
					String string_matrix1 = matrices[0];
					String string_matrix2 = matrices[1];
					System.out.println(string_matrix1);
					System.out.println(string_matrix2);

					String [] rows1 = string_matrix1.split("\n");
					String [] rows2 = string_matrix2.split("\n");
					//Check size is a power of 2
					if (checkPowerOfTwo(rows1.length) || checkPowerOfTwo(rows2.length)){
						System.out.println(rows1.length);
						System.out.println(rows2.length);
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
					int [][] matrix1 = new int[rows1.length][rows1.length];
					int [][] matrix2 = new int[rows2.length][rows2.length];
					matrix1 = buildMatrix(rows1);
					matrix2 = buildMatrix(rows2);
					System.out.println(string_matrix1);
					System.out.println();
					System.out.println(string_matrix1);
				}
			}
			catch(Exception e){

			}
		}
		return "redirect:/";
	}

	//Taken from https://www.geeksforgeeks.org/java-program-to-find-whether-a-no-is-power-of-two/
	static boolean checkPowerOfTwo(int n)
    {
        if (n == 0)
            return false;
 
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
				}
			}
		}
		catch(Exception e){

		}
		return matrix;
	}
}
