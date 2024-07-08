package io.quarkiverse.nagios.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;

@Path("/nagios")
@ApplicationScoped
public class NagiosResource {

    @GET
    public String hello() {
        return "Hello nagios";
    }
}
