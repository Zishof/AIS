package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import ais.database.model.PengumumanAkademis;


import com.sun.jersey.spi.resource.Singleton;

@Path("/pengumumanAkademis")
@Singleton



public class PengumumanAkademisResource extends DataResource<PengumumanAkademis> {

	public PengumumanAkademisResource() {
		super(PengumumanAkademis.class);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public PengumumanAkademisResource getXml() {
		return this;
	}

	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public PengumumanAkademis getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<PengumumanAkademis> getAllData(@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<PengumumanAkademis> getAllData(@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<PengumumanAkademis> getAllData(@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search,
			@PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

}
