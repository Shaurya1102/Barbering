package com.Barbering.backend.controllers;


import com.Barbering.backend.dto.SignUpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
public class SignUp {

    @PostMapping("/signup")
    public String signup(@RequestBody SignUpRequest signUpRequest){
        System.out.println(signUpRequest.getName());
        return "agya";
    }
}
