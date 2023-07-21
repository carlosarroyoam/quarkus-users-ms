package com.carlosarroyoam.userservice.resources;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import com.carlosarroyoam.userservice.dto.CreateUserRequest;
import com.carlosarroyoam.userservice.dto.UserResponse;
import com.carlosarroyoam.userservice.services.UserService;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/users")
@ApplicationScoped
@Authenticated
public class UserResource {

	private final UserService userService;

	@Inject
	public UserResource(final UserService userService) {
		super();
		this.userService = userService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("Admin")
	public RestResponse<List<UserResponse>> findAll() {
		return RestResponse.ok(userService.findAll());
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("Admin")
	public RestResponse<UserResponse> findById(@RestPath Long id) {
		return RestResponse.ok(userService.findById(id));
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("Admin")
	@Transactional
	public RestResponse<Object> create(CreateUserRequest createUserRequest) {
		UserResponse user = userService.create(createUserRequest);
		return RestResponse.created(URI.create("/users/" + user.getId()));
	}

	@GET
	@Path("/me")
	@Produces(MediaType.APPLICATION_JSON)
	public RestResponse<UserResponse> me(@Context SecurityContext securityContext) {
		return RestResponse.ok(userService.findByUsername(securityContext.getUserPrincipal().getName()));
	}

}
