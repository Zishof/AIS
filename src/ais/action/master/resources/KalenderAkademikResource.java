package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import ais.database.model.KalenderAkademik;


import com.sun.jersey.spi.resource.Singleton;

@Path("/kalenderAkademik")
@Singleton



public class KalenderAkademikResource extends DataResource<KalenderAkademik> {

	public KalenderAkademikResource() {
		super(KalenderAkademik.class);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public KalenderAkademikResource getXml() {
		return this;
	}

	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public KalenderAkademik getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KalenderAkademik> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KalenderAkademik> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KalenderAkademik> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search,
			@PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

}
