package com.example.grpc.client.grpcclient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import javax.naming.NamingException;

@Controller
public class PingPongEndpoint {   
	private String string_matrix1; 
	private String string_matrix2; 
	private int [][] matrix1;
	private int [][] matrix2;
	
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
	@ResponseBody
	public String add() {
		return grpcClientService.add(matrix1, matrix2);
	}
	@GetMapping("/")
	@ResponseBody
	public String upload () {
		if (string_matrix1==null && string_matrix2==null){
			return "Please upload two files!";
		}
		else if (string_matrix1==null){
			return "Please upload a file containing the first matrix!";
		}
		else if (string_matrix2==null){
			return "Please upload a file containing the second matrix!";
		}
		String response = "Successfully uploaded files!"+"<br>"+"Matrix 1:"+"<br>"+string_matrix1.replaceAll("\n", "<br>")+"<br>"+"Matrix 2:"+"<br>"+string_matrix2.replaceAll("\n", "<br>");
		return response;
	}
	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException{
		if (file.getBytes().length==0){
			redirectAttributes.addFlashAttribute("message", "Please make sure the file is not empty!");
			return "redirect:/";
		}
		else{
			if (string_matrix1==null){
				string_matrix1 = new String(file.getBytes());
			}
			else if (string_matrix2==null){
				string_matrix2 = new String(file.getBytes());
			}
			else{
				return processMatrices(string_matrix1, string_matrix2, redirectAttributes);
			}
		}
		return "redirect:/";
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
			//redirectAttributes.addFlashAttribute("message", "Files successfully uploaded!");
			return "redirect:/";
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
}
