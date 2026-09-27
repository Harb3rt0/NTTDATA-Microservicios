package tacos.api.error;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiNotFoundController {

    @RequestMapping({"/api", "/api/**"})
    public void apiRouteNotFound() {
        throw new ResourceNotFoundException(
            ApiErrorCodes.RESOURCE_NOT_FOUND,
            "The requested API resource was not found."
        );
    }
}
