package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import ais.database.model.PengumumanAkademis;


import com.sun.jersey.spi.resource.Singleton;

/**
 * Endpoint REST (JAX-RS/Jersey) baca-saja untuk entitas {@link PengumumanAkademis} (pengumuman
 * akademik), dipakai oleh integrasi eksternal (mis. aplikasi mobile) untuk mengambil satu
 * pengumuman atau mencarinya. Seluruh logika query diwarisi dari {@link DataResource}; kelas ini
 * hanya mendefinisikan pemetaan path REST dan meneruskan parameter ke implementasi induk.
 *
 * <p>
 * <b>Catatan keamanan</b> — seluruh endpoint pencarian/pengambilan data mewajibkan
 * {@code username} dan {@code password} sebagai <i>segmen path URL</i> (bukan header
 * Authorization atau body). Pola ini menyebabkan kredensial pengguna tersimpan dalam bentuk
 * teks-jelas pada log akses server, riwayat proxy/CDN, dan kemungkinan cache/log browser klien.
 * Ini adalah keputusan desain lama pada seluruh keluarga {@code *Resource} di paket ini, bukan
 * sesuatu yang diperbaiki di sini sesuai batasan tugas dokumentasi ini.
 * </p>
 */
@Path("/pengumumanAkademis")
@Singleton



public class PengumumanAkademisResource extends DataResource<PengumumanAkademis> {

	/** Membuat resource untuk entitas {@link PengumumanAkademis}. */
	public PengumumanAkademisResource() {
		super(PengumumanAkademis.class);
	}

	/** @return resource ini sendiri (dipakai sebagai respons JSON kosong/placeholder pada path root). */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public PengumumanAkademisResource getXml() {
		return this;
	}

	/**
	 * Mengambil satu pengumuman akademik berdasarkan id.
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param id       id pengumuman yang dicari
	 * @return data pengumuman, atau {@code null}/kosong bila autentikasi gagal atau tidak ditemukan
	 */
	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public PengumumanAkademis getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @return seluruh pengumuman akademik yang dapat diakses user
	 */
	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<PengumumanAkademis> getAllData(@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param search   kata kunci pencarian pertama
	 * @return pengumuman akademik yang cocok dengan {@code search}
	 */
	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<PengumumanAkademis> getAllData(@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param search   kata kunci pencarian pertama
	 * @param search1  kata kunci pencarian kedua (kriteria tambahan)
	 * @return pengumuman akademik yang cocok dengan kombinasi {@code search} dan {@code search1}
	 */
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
