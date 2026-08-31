package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import ais.database.model.KalenderAkademik;


import com.sun.jersey.spi.resource.Singleton;




/**
 * Endpoint web service JAX-RS ({@code /kalenderAkademik}) untuk akses data {@link KalenderAkademik}
 * (kalender akademik) dari luar aplikasi, mengikuti pola generik {@link DataResource} yang dipakai
 * seragam oleh seluruh resource pada paket {@code ais.action.master.resources}: autentikasi
 * username/password per-permintaan (tanpa sesi) dan operasi ambil-satu ({@link #getData}) atau
 * cari-banyak ({@link #getAllData}, dengan 0/1/2 kata kunci pencarian tambahan). Seluruh method
 * hanya mendelegasikan ke implementasi baku pada {@link DataResource}; kelas ini murni memasang
 * anotasi path/tipe entitas.
 *
 * <p>
 * <b>Perhatian keamanan:</b> username dan password dikirim sebagai <i>path parameter</i> URL
 * (bukan header {@code Authorization} atau body), sehingga berisiko tercatat pada log akses
 * server, cache proxy, riwayat browser, dan header {@code Referer} — pola ini konsisten dengan
 * resource {@code DataResource} lain di paket yang sama dan bukan sesuatu yang unik pada file ini,
 * namun tetap perlu diketahui sebagai keterbatasan desain API ini.
 * </p>
 */
@Path("/kalenderAkademik")
@Singleton
public class KalenderAkademikResource extends DataResource<KalenderAkademik> {

	/** Mendaftarkan {@link KalenderAkademik} sebagai tipe entitas yang dilayani resource ini. */
	public KalenderAkademikResource() {
		super(KalenderAkademik.class);
	}

	/** @return resource ini sendiri (dipakai sebagai respons JSON kosong/placeholder untuk {@code GET /kalenderAkademik}). */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public KalenderAkademikResource getXml() {
		return this;
	}

	/**
	 * Mengambil satu {@link KalenderAkademik} berdasarkan {@code id}, setelah autentikasi
	 * {@code username}/{@code password} lewat {@link DataResource#getData}.
	 *
	 * @param username kredensial akses web service
	 * @param password kredensial akses web service
	 * @param id       id entitas yang dicari
	 * @return entitas yang ditemukan, atau sesuai perilaku baku {@link DataResource} bila tidak ada
	 */
	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public KalenderAkademik getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	/**
	 * Mengambil seluruh {@link KalenderAkademik} yang dapat diakses kredensial yang diberikan,
	 * tanpa filter pencarian tambahan.
	 *
	 * @param username kredensial akses web service
	 * @param password kredensial akses web service
	 * @return daftar entitas
	 */
	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KalenderAkademik> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	/**
	 * Seperti {@link #getAllData(String, String)}, dengan satu kata kunci pencarian tambahan
	 * {@code search} (semantik pencarian mengikuti {@link DataResource#getAllData(String, String, String)}).
	 */
	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KalenderAkademik> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	/**
	 * Seperti {@link #getAllData(String, String, String)}, dengan kata kunci pencarian kedua
	 * {@code search1} (semantik gabungan mengikuti {@link DataResource#getAllData(String, String, String, String)}).
	 */
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
