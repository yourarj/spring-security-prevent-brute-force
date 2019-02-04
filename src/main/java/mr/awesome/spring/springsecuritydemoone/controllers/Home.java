package mr.awesome.spring.springsecuritydemoone.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/site/")
public class Home {

    @GetMapping("home")
    protected Map<String,String> goHome(){
        return Collections.singletonMap("message", "Welcome Dude");
    }
}
