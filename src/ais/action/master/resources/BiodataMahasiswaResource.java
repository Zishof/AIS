package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

@Path("/biodataMahasiswa")
@Singleton



public class BiodataMahasiswaResource extends DataResource<BiodataMahasiswa> {

	public BiodataMahasiswaResource() {
		super(BiodataMahasiswa.class);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public BiodataMahasiswaResource getXml() {
		return this;
	}

	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public BiodataMahasiswa getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	@GET
	@Path("getDataByNIM/{username}/{password}/{nim}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public BiodataMahasiswa getDataByNIM(
			@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("nim") String nim) {
		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			BiodataMahasiswa generalValueObject = (BiodataMahasiswa) session
					.createCriteria(BiodataMahasiswa.class)
					.createAlias("mahasiswa", "mahasiswa")
					.add(Restrictions.eq("mahasiswa.nim", nim.trim()))
					.setMaxResults(1).uniqueResult();
			
			HibernateUtil.closeSession();
			if (generalValueObject == null) {
				throw new NotFoundException("data detail mahasiswa dengan NIM "
						+ nim + " tidak ditemukan");
			}
			return generalValueObject;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search,
			@PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

}
