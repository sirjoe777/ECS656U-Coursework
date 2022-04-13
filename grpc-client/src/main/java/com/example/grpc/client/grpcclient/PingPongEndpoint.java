package com.example.grpc.client.grpcclient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@RestController
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
		return "redirect:/uploadForm";
	}
	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,RedirectAttributes redirectAttributes) throws IOException{

			
		System.out.println("I'M HERE");
		//String data1 = new String(file.getBytes());
		// Matrix matrix1=new Matrix(data1);

		// String data2 = new String(file2.getBytes());
		// Matrix matrix2=new Matrix(data2);

		// checkEqual(matrix1, matrix2);
		// if(operation.equals("Addition")){
		// 	redirectAttributes.addFlashAttribute("result", grpcClientService.add(matrix1,matrix2));
		// }
		// else{
		// 	redirectAttributes.addFlashAttribute("result", grpcClientService.multiply(matrix1,matrix2,deadline));
		// }
		
		// redirectAttributes.addFlashAttribute("error","");
		

		return "redirect:/";
	}
}
