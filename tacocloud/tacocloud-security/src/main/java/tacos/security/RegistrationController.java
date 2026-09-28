package tacos.security;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/register")
public class RegistrationController {
  
  private RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) { //modificacion para TC-10
    this.registrationService = registrationService;
  }
  
  @GetMapping
  public String registerForm() {
    return "registration";
  }
  
  //TC-10 - Componer el registro HTML hasta completar la persistencia
  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Mono<String> processRegistration(@Valid RegistrationForm form) {
    return registrationService.register(form)
        .thenReturn("redirect:/login");
  }
  //Fin TC-10

  //TC-10 - Exponer registro JSON seguro para clientes REST
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Mono<ResponseEntity<RegistrationResponse>> processRegistrationJson(@Valid @RequestBody RegistrationForm form) {
    return registrationService.register(form)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }
  //Fin TC-10

}
