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
	public String home () {
		return "uploadForm";
	}
	@PostMapping("/")
	public String handleFileUpload(@RequestParam("matrix1") MultipartFile file1, @RequestParam("matrix2") MultipartFile file2, RedirectAttributes redirectAttributes) throws IOException{
		//Make sure a file has been uploaded
		if (file1.getBytes().length==0){
			redirectAttributes.addFlashAttribute("message", "Please make sure the first file is not empty!");
			return "redirect:/";
		} 
		if (file2.getBytes().length==0){
			redirectAttributes.addFlashAttribute("message", "Please make sure the second file is not empty!");
			return "redirect:/";
		} 
		string_matrix1 = new String(file1.getBytes());
		string_matrix2 = new String(file2.getBytes());
		return grpcClientService.processMatrices(string_matrix1, string_matrix2, redirectAttributes);
	}
	@GetMapping("/display")
	public String displayMatrices(Model mod) {
		if (string_matrix1!=null && string_matrix2!=null){
			Integer[][] test1 = new Integer[3][3];
			Integer[][] test2 = new Integer[3][3];
			test1[0][0]=0;
			test1[0][1]=0;
			test1[0][2]=0;
			test1[1][0]=0;
			test1[1][1]=0;
			test1[1][2]=0;
			test1[2][0]=0;
			test1[2][1]=0;
			test1[2][2]=0;
			test2[0][0]=0;
			test2[0][1]=0;
			test2[0][2]=0;
			test2[1][0]=0;
			test2[1][1]=0;
			test2[1][2]=0;
			test2[2][0]=0;
			test2[2][1]=0;
			test2[2][2]=0;
			mod.addAttribute("M1", test1);
			mod.addAttribute("M2", test2);
			return "display";
		}
		else return "uploadForm";
		
	}
}
