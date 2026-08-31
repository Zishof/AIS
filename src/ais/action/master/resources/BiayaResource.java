package ais.action.master.resources;

import java.net.URLDecoder;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.resources.model.CommonID;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/**
 * Resource JAX-RS ({@code /biaya}) yang mengekspos data referensi dan transaksi biaya kuliah
 * (jenis kegiatan pembayaran, item biaya, detail teks biaya per bahasa, serta riwayat cicilan
 * pembayaran mahasiswa) sebagai JSON ke konsumen eksternal. Mewarisi CRUD generik dari
 * {@link DataResource DataResource&lt;Mahasiswa&gt;}.
 * <p>
 * <b>Catatan keamanan</b>: hampir seluruh endpoint mengautentikasi lewat {@code username}/
 * {@code password} yang dikirim sebagai segmen URL path (lihat pola serupa di
 * {@link KelulusanResource} dan {@link PerpustakaanResource}), termasuk method {@link #pembayaran}
 * yang bahkan mengenkripsi ulang password path secara manual untuk dicocokkan langsung ke kolom
 * {@code pass} milik {@link Mahasiswa} — pola ini berisiko kredensial tercatat di log server atau
 * riwayat browser.
 * </p>
 */
@Path("/biaya")
@Singleton



public class BiayaResource extends DataResource<Mahasiswa> {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Konstruktor default; mendaftarkan {@link Mahasiswa} sebagai entitas CRUD generik ke superclass {@link DataResource}. */
	public BiayaResource() {
		super(Mahasiswa.class);
	}

	/** Endpoint pemeriksaan/handshake sederhana yang mengembalikan resource ini sendiri sebagai JSON. */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public BiayaResource getXml() {
		return this;
	}

	/** Mengambil seluruh {@link DetailBiaya} (teks rincian biaya, dapat multi-bahasa) untuk satu {@link JenisKegiatan} berdasarkan kodenya. */
	@GET
	@Path("daftar/{username}/{password}/{kode}")
	@Produces({ MediaType.APPLICATION_JSON })
	@SuppressWarnings("unchecked")
	public List<DetailBiaya> daftar(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("kode") String kode) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		List<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
				.createAlias("jenisKegiatan", "jenisKegiatan").add(Restrictions.eq("jenisKegiatan.kode", kode.trim()))
				.list();

		HibernateUtil.closeSession();
		return detailBiayas;
	}

	/** Mengambil seluruh {@link JenisKegiatan} tanpa filter maupun autentikasi. */
	@GET
	@Path("jenis_bayar")
	@Produces({ MediaType.APPLICATION_JSON })
	@SuppressWarnings("unchecked")
	public List<JenisKegiatan> jenisPembayaran() {

		Session session = HibernateUtil.currentNativeSession();
		List<JenisKegiatan> jenisPembayaran = session.createCriteria(JenisKegiatan.class).list();

		HibernateUtil.closeSession();
		return jenisPembayaran;
	}

	/** Mengambil {@link JenisKegiatan} aktif, opsional difilter berdasarkan flag {@code defaultKegiatan}, setelah validasi kredensial via {@link Common#checkLogin}. */
	@GET
	@Path("jenis_pembayaran/{username}/{password}/{defaultKegiatan}")
	@Produces({ MediaType.APPLICATION_JSON })
	@SuppressWarnings("unchecked")
	public List<JenisKegiatan> jenisPembayaran(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("defaultKegiatan") String defaultKegiatan) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		List<JenisKegiatan> jenisPembayaran = session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(defaultKegiatan == null || defaultKegiatan.trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultKegiatan", Boolean.parseBoolean(defaultKegiatan)))
				.list();

		HibernateUtil.closeSession();
		return jenisPembayaran;
	}

	/** Mengambil satu {@link DetailBiaya} spesifik berdasarkan kombinasi kode jenis kegiatan, kode item biaya, dan kode bahasa (exact match), setelah validasi kredensial. */
	@GET
	@Path("ambil/{username}/{password}/{kode}/{item}/{bahasa}")
	@Produces({ MediaType.APPLICATION_JSON })
	public DetailBiaya ambil(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("kode") String kode, @PathParam("item") String item, @PathParam("bahasa") String bahasa) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
				.createAlias("jenisKegiatan", "jenisKegiatan").createAlias("itemBiaya", "itemBiaya")
				.add(Restrictions.eq("bahasa", bahasa)).add(Restrictions.eq("itemBiaya.kode", item))
				.add(Restrictions.eq("jenisKegiatan.kode", kode.trim())).setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();
		return detailBiaya;
	}

	/**
	 * Mengambil {@link Kegiatan} pembayaran mahasiswa untuk satu jenis pembayaran dan semester.
	 * Autentikasi pada method ini <b>tidak</b> memakai {@link Common#checkLogin} seperti endpoint
	 * lain, melainkan mengenkripsi ulang {@code password} secara manual
	 * ({@link Common#desEncrypter}) dan mencocokkannya langsung ke kolom {@code pass} pada
	 * {@link Mahasiswa} aktif dengan NIM sama dengan {@code username}.
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila kredensial gagal atau pembayaran tidak ditemukan
	 */
	@GET
	@Path("pembayaran/{username}/{password}/{jenis_pembayaran}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Kegiatan pembayaran(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("jenis_pembayaran") String jenis_pembayaran, @PathParam("semester") String semester) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

		JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(jenis_pembayaran);
		HibernateUtil.closeSession();
		Kegiatan kegiatan = mahasiswa.ambilKegiatans(Integer.parseInt(semester), jenisKegiatan);
		if (kegiatan == null || kegiatan.getId() == null) {
			throw new NotFoundException("pembayaran tidak ditemukan");
		}
		return kegiatan;
	}

	/**
	 * Mencari {@link JenisKegiatan} aktif berdasarkan ilike nama atau kode kegiatan, terurut nama,
	 * dipaginasi, dikembalikan sebagai JSON array (string) di dalam {@code info1} pada
	 * {@link CommonID}. Catatan: parameter {@code username}/{@code password} diterima tetapi tidak
	 * divalidasi (tidak ada pemanggilan {@link Common#checkLogin}) pada overload ini.
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("jenis_pembayaran/{username}/{password}/{jenis}/{start}/{max}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID jenisPembayaran(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("jenis") String jenis, @PathParam("start") String start, @PathParam("max") String max)
			throws Exception {
		username = URLDecoder.decode(username.replaceAll("_", ""), "UTF-8");
		password = URLDecoder.decode(password.replaceAll("_", ""), "UTF-8");
		jenis = URLDecoder.decode(jenis.replaceAll("_", "").trim(), "UTF-8");
		start = URLDecoder.decode(start.replaceAll("_", "").trim(), "UTF-8");
		max = URLDecoder.decode(max.replaceAll("_", "").trim(), "UTF-8");

		Session session = HibernateUtil.currentNativeSession();

		List<JenisKegiatan> jenisKegiatans = session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("namaKegiatan"))

				.add(jenis == null || jenis.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("namaKegiatan", jenis, MatchMode.ANYWHERE),
								Restrictions.ilike("kode", jenis, MatchMode.ANYWHERE)))

				.setFirstResult(start == null || start.trim().isEmpty() || !Common.isNumber(start) ? 0
						: Integer.parseInt(start.trim()))
				.setMaxResults(max == null || max.trim().isEmpty() || !Common.isNumber(max) ? 10
						: Integer.parseInt(max.trim()))

				.list();

		HibernateUtil.closeSession();
		CommonID commonID = new CommonID();

		JSONArray array = new JSONArray();

		for (JenisKegiatan jenisKegiatan : jenisKegiatans) {
			JSONObject json = new JSONObject();

			json.put("id", jenisKegiatan.getId());
			json.put("nama", jenisKegiatan.getNamaKegiatan());
			json.put("kode", jenisKegiatan.getKode());

			array.put(json);
		}

		commonID.setInfo1(array.toString());
		return commonID;
	}

	/**
	 * Mencari {@link ItemBiaya} aktif berdasarkan ilike nama atau kode, terurut nama, dipaginasi,
	 * dikembalikan sebagai JSON array (string) di dalam {@code info1} pada {@link CommonID}.
	 * Catatan: parameter {@code username}/{@code password} diterima tetapi tidak divalidasi pada
	 * overload ini.
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("item_biaya/{username}/{password}/{jenis}/{start}/{max}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID itemBiaya(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("jenis") String jenis, @PathParam("start") String start, @PathParam("max") String max)
			throws Exception {
		username = URLDecoder.decode(username.replaceAll("_", ""), "UTF-8");
		password = URLDecoder.decode(password.replaceAll("_", ""), "UTF-8");
		jenis = URLDecoder.decode(jenis.replaceAll("_", "").trim(), "UTF-8");
		start = URLDecoder.decode(start.replaceAll("_", "").trim(), "UTF-8");
		max = URLDecoder.decode(max.replaceAll("_", "").trim(), "UTF-8");

		Session session = HibernateUtil.currentNativeSession();

		List<ItemBiaya> itemBiayas = session.createCriteria(ItemBiaya.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))

				.add(jenis == null || jenis.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", jenis, MatchMode.ANYWHERE),
								Restrictions.ilike("kode", jenis, MatchMode.ANYWHERE)))

				.setFirstResult(start == null || start.trim().isEmpty() || !Common.isNumber(start) ? 0
						: Integer.parseInt(start.trim()))
				.setMaxResults(max == null || max.trim().isEmpty() || !Common.isNumber(max) ? 10
						: Integer.parseInt(max.trim()))

				.list();

		HibernateUtil.closeSession();
		CommonID commonID = new CommonID();

		JSONArray array = new JSONArray();

		for (ItemBiaya itemBiaya : itemBiayas) {
			JSONObject json = new JSONObject();

			json.put("id", itemBiaya.getId());
			json.put("nama", itemBiaya.getNama());
			json.put("kode", itemBiaya.getKode());

			array.put(json);
		}

		commonID.setInfo1(array.toString());
		return commonID;
	}

	/**
	 * Mencari riwayat {@link CicilanPembayaran} (mahasiswa maupun calon mahasiswa) berdasarkan
	 * kombinasi filter program, NIM/no. registrasi (ilike), jenis kegiatan, tahun akademik dan
	 * paritas semester (dari parameter {@code ta} 5 digit: 4 digit tahun mulai + 1 digit kode
	 * semester), terurut terbaru dan dipaginasi. Tiap hasil dirangkum sebagai JSON: jenis
	 * pembayaran, identitas pembayar (mahasiswa atau calon mahasiswa), semester, item biaya,
	 * bulan (untuk cicilan bulanan), waktu, nilai, dan keterangan — dikembalikan sebagai JSON
	 * array (string) di dalam {@code info1} pada {@link CommonID}.
	 *
	 * <p>
	 * <b>Catatan keamanan (IDOR DITUTUP 2026-09-01):</b> {@link Common#checkLogin(String, String)}
	 * hanya memvalidasi kredensial PEMANGGIL (bisa staf {@link Tbmuser} ATAU mahasiswa via NIM), tapi
	 * sebelumnya TIDAK membatasi filter {@code nim} — sekali kredensial mahasiswa mana pun valid,
	 * pemanggil bisa memasukkan NIM mahasiswa LAIN dan membaca riwayat pembayaran orang tersebut
	 * (nominal, item, keterangan) — celah IDOR (broken object-level authorization). Kini bila
	 * {@code username} yang login cocok dengan NIM {@link Mahasiswa} (bukan staf {@link Tbmuser}),
	 * filter {@code nim} DIPAKSA ke NIM mahasiswa itu sendiri, mengabaikan nilai {@code nim} yang
	 * dikirim lewat URL — mahasiswa hanya bisa melihat riwayat pembayarannya sendiri. Staf yang login
	 * (bukan mahasiswa) TIDAK dibatasi, sesuai kebutuhan pencarian administratif lintas mahasiswa yang
	 * sudah menjadi fungsi utama endpoint ini.
	 * </p>
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila autentikasi username/password gagal
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("pembayaran_mahasiswa/{username}/{password}/{jenis}/{program}/{ta}/{nim}/{start}/{max}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID pembayaranMahasiswa(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("jenis") String jenis, @PathParam("program") String program, @PathParam("ta") String ta,
			@PathParam("nim") String nim, @PathParam("start") String start, @PathParam("max") String max)
			throws Exception {

		username = URLDecoder.decode(username.replaceAll("_", ""), "UTF-8");
		password = URLDecoder.decode(password.replaceAll("_", ""), "UTF-8");
		ta = URLDecoder.decode(ta.replaceAll("_", ""), "UTF-8");
		program = URLDecoder.decode(program.replaceAll("_", "").trim(), "UTF-8");
		jenis = URLDecoder.decode(jenis.replaceAll("_", "").trim(), "UTF-8");
		nim = URLDecoder.decode(nim.replaceAll("_", "").trim(), "UTF-8");

		start = URLDecoder.decode(start.replaceAll("_", "").trim(), "UTF-8");
		max = URLDecoder.decode(max.replaceAll("_", "").trim(), "UTF-8");

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		// Common#checkLogin memvalidasi Tbmuser (staf) LEBIH DULU, dan hanya mengecek Mahasiswa bila
		// tidak ada Tbmuser yang cocok — replikasi urutan yang sama di sini agar staf yang username-nya
		// kebetulan sama dengan NIM seorang mahasiswa tetap diperlakukan sebagai staf (tidak dibatasi).
		Session loginCheckSession = HibernateUtil.currentNativeSession();
		Tbmuser loginSebagaiStaf = (Tbmuser) loginCheckSession.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("userId", username)).setMaxResults(1).uniqueResult();
		if (loginSebagaiStaf == null) {
			Mahasiswa loginSebagaiMahasiswa = (Mahasiswa) loginCheckSession.createCriteria(Mahasiswa.class)
					.add(Restrictions.eq("nim", username)).setMaxResults(1).uniqueResult();
			if (loginSebagaiMahasiswa != null) {
				// Mahasiswa yang login hanya boleh melihat riwayat pembayarannya sendiri — abaikan nim
				// dari URL untuk mencegah IDOR (lihat catatan keamanan pada javadoc method).
				nim = username;
			}
		}

		String tahunAkademik = Common.getCurrentTahunAkademik();
		String semesters = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GANJIL;
		if (ta != null && ta.length() == 5) {
			Integer mulai = Integer.parseInt(ta.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta.toString().substring(4, 5));
			semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		}

		System.out.println("ta => " + ta + ", program => " + program + ", nim => " + nim + ", jenis => " + jenis
				+ ", start => " + start + ", max => " + max);

		Session session = HibernateUtil.currentNativeSession();

		List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
				.addOrder(Order.desc("id")).createCriteria("kegiatan").createAlias("jenisKegiatan", "jenisKegiatan")
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("mahasiswa.program", program),
								Restrictions.eq("calonMahasiswa.program", program)))

				.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", nim, MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", nim, MatchMode.ANYWHERE)))

				.add(jenis == null || jenis.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisKegiatan.kode", jenis))
				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction("semster % 2 = " + (semesters.equals(Perkuliahan.GANJIL) ? "1"
								: "0")))

				.setFirstResult(start == null || start.trim().isEmpty() || !Common.isNumber(start) ? 0
						: Integer.parseInt(start.trim()))
				.setMaxResults(max == null || max.trim().isEmpty() || !Common.isNumber(max) ? 10
						: Integer.parseInt(max.trim()))

				.list();

		HibernateUtil.closeSession();
		CommonID commonID = new CommonID();

		JSONArray array = new JSONArray();

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			JSONObject json = new JSONObject();

			json.put("id", cicilanPembayaran.getId());
			json.put("jenis_pembayaran", cicilanPembayaran.getKegiatan().getJenisKegiatan() == null ? ""
					: cicilanPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan());
			json.put("mahasiswa",
					cicilanPembayaran.getKegiatan().getMahasiswa() == null
							? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? ""
									: cicilanPembayaran.getKegiatan().getCalonMahasiswa().getNoRegistrasi() + "-"
											+ cicilanPembayaran.getKegiatan().getCalonMahasiswa().getNama())
							: cicilanPembayaran.getKegiatan().getMahasiswa().getNim() + "-"
									+ cicilanPembayaran.getKegiatan().getMahasiswa().getNama());
			json.put("semester", cicilanPembayaran.getKegiatan().getSemster());
			json.put("item_biaya",
					cicilanPembayaran.getItemBiaya() == null ? "" : cicilanPembayaran.getItemBiaya().getNama());
			json.put("bulan",
					cicilanPembayaran.getPengaturanPembayaranBulanan() == null
							|| cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan() == null ? ""
									: cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan().toString());
			json.put("waktu", cicilanPembayaran.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(cicilanPembayaran.getTanggal()));
			json.put("nilai", cicilanPembayaran.getNilai());
			json.put("keterangan", cicilanPembayaran.getKeterangan());
			Kegiatan kegiatan = cicilanPembayaran.getKegiatan();
			json.put("smt", kegiatan.getSemster() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
			json.put("ta", kegiatan.getTahunAkademik());
			array.put(json);
		}

		commonID.setInfo1(array.toString());
		return commonID;
	}

}
