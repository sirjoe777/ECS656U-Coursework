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
	@ResponseBody
	public String home () {
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
			else if (string_matrix1==null){
				string_matrix2 = new String(file.getBytes());
			}
			else{
				return grpcClientService.processMatrices(string_matrix1, string_matrix2, redirectAttributes);
			}
		}
		return "redirect:/";
	}
}
