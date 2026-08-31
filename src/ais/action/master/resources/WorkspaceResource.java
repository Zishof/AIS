package ais.action.master.resources;

import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.rab.util.RabUtil;
import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiRab;
import ais.database.model.rab.InformasiRab;
import ais.database.model.rab.InformasiRabKomentar;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.WaktuUtil;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

/**
 * Endpoint REST (JAX-RS/Jersey) untuk entitas {@link Workspace} (ruang kerja RAB) dan data terkait
 * (informasi RAB beserta lampiran/komentarnya). Method dasar (load/search) diwarisi dari
 * {@link DataResource} — lihat catatan keamanan kredensial-di-URL pada javadoc kelas itu, yang
 * berlaku sama di sini. Method tambahan di kelas ini melayani kebutuhan spesifik workspace: pohon
 * workspace default per satuan kerja, serta feed informasi RAB (papan pengumuman/berita internal
 * satuan kerja) beserta komentarnya.
 *
 * <p>
 * <b>Catatan keamanan (DITAMBAL 2026-09-01):</b> {@link #daftarWorkspace}, kedua overload
 * {@link #daftarInformasiRab}, {@link #daftarInformasiRabKomentar}, dan
 * {@link #daftarInformasiRabJumlahKomentar} sebelumnya TIDAK memvalidasi username/password sama
 * sekali (berbeda dari method load/search), sehingga siapa pun yang menjangkau path
 * {@code /user_workspace/*} dapat membaca struktur workspace internal dan feed informasi RAB
 * tanpa login — termasuk {@link #daftarInformasiRabKomentar}, yang mengembalikan nama/kontak/email
 * PRIBADI penulis komentar untuk {@code item} (id {@link InformasiRab}) mana pun yang berhasil
 * ditebak, tanpa autentikasi. Kelima method ini kini mewajibkan {@code username}/{@code password}
 * yang valid (segmen path, divalidasi lewat {@link Common#checkLogin(String, String)} — pola yang
 * sama dipakai {@link #getData}/{@link #getAllData} di kelas ini), konsisten dengan seluruh method
 * lain di kelas ini. Pemanggil lama yang belum menyertakan segmen {@code username}/{@code password}
 * pada URL akan ditolak; pembaruan pada sisi pemanggil diperlukan agar endpoint-endpoint ini
 * kembali berfungsi.
 * </p>
 */
@Path("/user_workspace")
@Singleton

public class WorkspaceResource extends DataResource<Workspace> {

	/** Membuat resource untuk entitas {@link Workspace}. */
	public WorkspaceResource() {
		super(Workspace.class);
	}

	/** @return resource ini sendiri (dipakai sebagai respons JSON kosong/placeholder pada path root). */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public WorkspaceResource getXml() {
		return this;
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param id       id workspace yang dicari
	 * @return workspace yang ditemukan
	 */
	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Workspace getData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @return seluruh workspace yang dapat diakses user
	 */
	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Workspace> getAllData(@PathParam("username") String username, @PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param search   kata kunci pencarian
	 * @return workspace yang cocok dengan {@code search}
	 */
	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Workspace> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param search   kata kunci pencarian pertama
	 * @param search1  kata kunci pencarian kedua
	 * @return workspace yang cocok dengan kombinasi {@code search} dan {@code search1}
	 */
	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Workspace> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search, @PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

	/**
	 * Mengambil daftar workspace default (bawaan/carry-over) yang menjadi anak langsung dari
	 * {@code parent}, beserta ringkasan sasaran RAB tiap workspace lewat
	 * {@link RabUtil#getDetailWorkspace}.
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param parent   id workspace induk, atau {@code "_"}/{@code "-1"} untuk workspace tingkat akar
	 * @return daftar workspace anak beserta ringkasan sasaran (di {@code info2}/{@code info3})
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_workspace/{username}/{password}/{parent}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarWorkspace(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("parent") String parent) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		List<CommonID> commonIDs = new ArrayList<CommonID>();

		parent = parent.trim().equals("_") || parent.trim().equals("-1") ? "" : parent.trim();

		Session session = HibernateUtil.currentNativeSession();
		List<Workspace> workspaces = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("defaultItem", true)).add(parent.trim().equals("") ? Restrictions.isNull("parent")
						: Restrictions.eq("parent.id", Long.parseLong(parent)))
				.addOrder(Order.asc("nama")).list();
		for (Workspace workspace : workspaces) {

			Serializable[] serializables = RabUtil.getDetailWorkspace(WorkspacePunyaSasaran.class, "sasaran",
					new String[] { "a1.nama" }, workspace);

			CommonID commonID = new CommonID(workspace.getId());
			commonID.setInfo1(workspace.toString());
			commonID.setInfo2(Common.numberFormat.get().format(serializables[0]));
			commonID.setInfo3(Common.numberFormat.get().format(serializables[0]));
			commonIDs.add(commonID);
		}
		HibernateUtil.closeSession();
		return commonIDs;
	}

	/** Seperti {@link #daftarInformasiRab(String, String, String, String, String, String)} dengan halaman pertama (10 baris pertama). */
	@GET
	@Path("daftar_informasi_rab/{username}/{password}/{satuanKerja}/{cari}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiRab(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("cari") String cari) throws Exception {
		return daftarInformasiRab(username, password, satuanKerja, cari, "0", "10");
	}

	/**
	 * Mengambil daftar {@link InformasiRab} (informasi/pengumuman RAB) yang sedang berlaku pada
	 * tanggal berjalan (antara {@code mulai} dan {@code sampai}, atau {@code sampai} kosong),
	 * dengan pencarian teks opsional pada {@code content} dan filter satuan kerja opsional,
	 * dipaginasi. Setiap item disertai daftar lampiran foto (link unduh) dan jumlah komentar.
	 *
	 * @param username    kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password    kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param satuanKerja id satuan kerja pemilik, atau {@code ""}/{@code "_"}/{@code "-1"} untuk semua
	 * @param cari        kata kunci pencarian pada isi informasi (URL-encoded), atau penanda kosong untuk tanpa filter
	 * @param start       offset baris awal (paginasi)
	 * @param banyak      jumlah baris maksimal yang diambil
	 * @return daftar informasi RAB beserta metadata (lampiran, satuan kerja, tanggal berlaku, jumlah komentar, jenis)
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_informasi_rab/{username}/{password}/{satuanKerja}/{cari}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiRab(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("cari") String cari, @PathParam("start") String start, @PathParam("banyak") String banyak)
			throws Exception {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		List<CommonID> commonIDs = new ArrayList<CommonID>();

		cari = URLDecoder.decode(cari, "UTF-8");

		Session session = HibernateUtil.currentNativeSession();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		List<InformasiRab> informasiRabs = session.createCriteria(InformasiRab.class)
				.add(cari.trim().equals("") || cari.trim().equals("_") || cari.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("content", cari, MatchMode.ANYWHERE))

				.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_") || satuanKerja.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerja)))

				.add(Restrictions
						.sqlRestriction("date(mulai) <= date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate())
								+ "') and (sampai is null or date(sampai) >= date('"
								+ Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "'))"))
				.addOrder(Order.desc("mulai")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (InformasiRab informasiRab : informasiRabs) {
			CommonID commonID = new CommonID();

			List<FotoInformasiRab> fotoItems = streamingSession.createCriteria(FotoInformasiRab.class)
					.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
					.add(Restrictions.eq("informasiRab", informasiRab.getId())).addOrder(Order.desc("id")).list();
			String lampiran = "";
			for (FotoInformasiRab fotoItem : fotoItems) {
				String url = CommonMedia.getLampiranInformasiRab(fotoItem.getId());
				lampiran += lampiran.equals("") ? ("<a href=" + url + ">" + fotoItem.getNama() + "</a>")
						: (", <a href=" + url + ">" + fotoItem.getNama() + "</a>");
			}
			fotoItems = null;

			commonID.setId(informasiRab.getId());
			String html = informasiRab.getContent();
			commonID.setInfo1(html);
			commonID.setInfo2(lampiran);
			commonID.setInfo3(informasiRab.getSatuanKerja() == null ? "" : informasiRab.getSatuanKerja().getNama());
			commonID.setInfo4("");
			commonID.setInfo5(
					informasiRab.getMulai() == null ? "" : Common.dateFormat2.get().format(informasiRab.getMulai()));
			commonID.setInfo6(
					informasiRab.getSampai() == null ? "" : Common.dateFormat2.get().format(informasiRab.getSampai()));
			Integer count = ((Number) session.createCriteria(InformasiRabKomentar.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("informasiRab", informasiRab))
					.uniqueResult()).intValue();
			commonID.setInfo7(Common.numberFormat.get().format(count));
			commonID.setInfo8(
					informasiRab.getJenisInformasiRab() == null ? "" : informasiRab.getJenisInformasiRab().getNama());
			commonIDs.add(commonID);
		}
		StreamingHibernateUtil.getInstance().closeSession();
		HibernateUtil.closeSession();
		return commonIDs;
	}

	/**
	 * Mengambil komentar-komentar (hingga {@link Common#MAX_RESULT_20}, terbaru dahulu) pada satu
	 * {@link InformasiRab}.
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param item     id informasi RAB yang komentarnya diambil
	 * @return daftar komentar beserta nama, kontak, email, dan tanggal ubah
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_informasi_rab_komentar/{username}/{password}/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiRabKomentar(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("item") String item) throws Exception {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		List<InformasiRabKomentar> komentarItems = session.createCriteria(InformasiRabKomentar.class)
				.add(Restrictions.eq("informasiRab.id", Long.parseLong(item))).addOrder(Order.desc("tanggal_dirubah"))
				.setMaxResults(Common.MAX_RESULT_20).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (InformasiRabKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getInformasiRab().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonID.setInfo6(komentarItem.getEmail());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	/**
	 * Mengambil jumlah komentar pada satu {@link InformasiRab} tanpa mengunduh isi komentarnya.
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param password kredensial user (lihat catatan keamanan di javadoc {@link DataResource})
	 * @param item     id informasi RAB yang dihitung jumlah komentarnya
	 * @return objek dengan id = {@code item} dan {@code info1} = jumlah komentar
	 */
	@GET
	@Path("daftar_informasi_rab_jumlah_komentar/{username}/{password}/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID daftarInformasiRabJumlahKomentar(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("item") String item) throws Exception {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		Integer jumlahKomentarItems = ((Number) session.createCriteria(InformasiRabKomentar.class)
				.add(Restrictions.eq("informasiRab.id", Long.parseLong(item))).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		CommonID commonID = new CommonID(Long.parseLong(item));
		commonID.setInfo1(Common.numberFormat.get().format(jumlahKomentarItems));
		HibernateUtil.closeSession();
		return commonID;
	}
}
