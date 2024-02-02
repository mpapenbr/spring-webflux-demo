package dev.mpapenbr.webflux.demo;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/hello")
    public String hello(Authentication authentication) {
        return "Welcome " + authentication.getName() + "!";
    }
}
