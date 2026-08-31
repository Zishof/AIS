package ais.action.master.resources;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.resources.helper.KrsResourceHelper;
import ais.action.master.resources.model.CommonID;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatTemplateSurat;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PembayaranMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.ReportLog;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.TemplateSurat;

@Path("/mahasiswa")
@Singleton



/**
 * Titik akhir REST (Jersey/JAX-RS) untuk integrasi eksternal (mis. aplikasi mobile) seputar akun dan
 * data akademik mahasiswa: login (beberapa varian: password DES-encrypted, NIM saja, PIN), lihat/ambil/
 * hapus KRS (didelegasikan ke {@link KrsResourceHelper}), lihat nilai dan detail pembayaran per
 * semester (lewat {@link PembayaranUtil}), pencarian data mahasiswa generik (diwarisi dari
 * {@link DataResource}), serta beberapa endpoint terkait surat dan pembayaran. Autentikasi pada
 * hampir seluruh endpoint memakai pola {@code username}/{@code password} sebagai segmen path URL
 * (password dienkripsi DES sebelum dibandingkan ke kolom {@code pass}, lihat
 * {@link Common#desEncrypter}), sama seperti keluarga {@code *Resource} lain di paket ini.
 *
 * <p>
 * <b>Catatan keamanan:</b>
 * </p>
 * <ul>
 * <li>Password dikirim sebagai bagian path URL pada permintaan GET (bukan header/body) — rawan
 * tercatat di log akses, cache proxy, riwayat browser, dan header {@code Referer}, berlaku pada
 * seluruh method yang menerima {@code @PathParam("password")}.</li>
 * <li><b>{@link #bayar(String, String, String, String, String, String)} TIDAK melakukan
 * autentikasi/otorisasi apa pun</b> (tidak ada {@code Common.checkLogin} atau pemeriksaan sesi) —
 * endpoint ini menerima id mahasiswa, id jenis kegiatan, id item biaya, dan NOMINAL secara langsung
 * dari path URL, lalu membuat baris {@link DetailBiaya} dan memicu
 * {@link PembayaranUtil#simpanPembayaranMahasiswa} yang menghasilkan {@link Kegiatan} pembayaran.
 * Siapa pun yang mengetahui/menebak URL-nya dapat men-trigger pembuatan transaksi pembayaran untuk
 * mahasiswa mana pun dengan nominal sembarang tanpa login.</li>
 * <li>{@link #reportLog(String)} dan {@link #getFormatTemplateSurat(String, String)} juga tidak
 * memeriksa autentikasi (siapa pun dapat menulis baris {@link ReportLog} atau membaca format
 * template surat).</li>
 * </ul>
 */
public class MahasiswaResource extends DataResource<Mahasiswa> {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Membuat resource yang terikat ke entitas {@link Mahasiswa}. */
	public MahasiswaResource() {
		super(Mahasiswa.class);
	}

	/** Mengembalikan diri sendiri (self) sebagai representasi JSON kosong — dipakai untuk pemeriksaan endpoint dasar. */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public MahasiswaResource getXml() {
		return this;
	}

	/**
	 * Melakukan login mahasiswa (NIM + password terenkripsi DES) dan mengembalikan ringkasan profil
	 * ({@link CommonID}) berisi nama, NIM, jurusan/fakultas, email, foto, tahun akademik berjalan,
	 * semester, dan status pembayaran semester berjalan.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL, dienkripsi DES sebelum dibandingkan)
	 * @return ringkasan profil mahasiswa
	 * @throws NotFoundException bila login gagal (NIM/password tidak cocok atau mahasiswa nonaktif)
	 */
	@GET
	@Path("masuk/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getMasuk(@PathParam("username") String username, @PathParam("password") String password) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();

		if (mahasiswa == null) {
			throw new NotFoundException("Login mahasiswa gagal dilakukan");
		}

		CommonID commonID = new CommonID();
		commonID.setId(mahasiswa.getId());
		commonID.setInfo1(mahasiswa.getNama());
		commonID.setInfo2(mahasiswa.getNim());
		commonID.setInfo3(mahasiswa.getJurusan().getNama());
		commonID.setInfo4(mahasiswa.getJurusan().getFakultas().getNama());
		commonID.setInfo5(mahasiswa.getEmail());
		try {
			commonID.setInfo6(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			int semester = mahasiswa.currentSemester();
			boolean bayar = Common.checkStatusPembayaranMahasiswa(semester, mahasiswa.currentTahapan(), mahasiswa,
					false, false);

			commonID.setInfo7(Common.getCurrentTahunAkademik());
			commonID.setInfo8(semester + "");
			commonID.setInfo9(bayar ? "Telah Membayar" : "Belum Membayar");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		return commonID;
	}

	@GET
	@Path("login/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Melakukan login mahasiswa (NIM + password terenkripsi DES) dan mengembalikan entitas
	 * {@link Mahasiswa} lengkap.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL, dienkripsi DES sebelum dibandingkan)
	 * @return entitas mahasiswa yang login
	 * @throws NotFoundException bila login gagal
	 */
	public Mahasiswa getLogin(@PathParam("username") String username, @PathParam("password") String password) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();

		if (mahasiswa == null) {
			throw new NotFoundException("Login mahasiswa gagal dilakukan");
		}

		return mahasiswa;
	}

	@GET
	@Path("login_nim/{username}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil mahasiswa aktif berdasarkan NIM saja, TANPA verifikasi password.
	 *
	 * @param username NIM mahasiswa
	 * @return entitas mahasiswa yang cocok
	 * @throws NotFoundException bila NIM tidak ditemukan
	 */
	public Mahasiswa getLoginNim(@PathParam("username") String username) {
		Session session = HibernateUtil.currentNativeSession();
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();

		if (mahasiswa == null) {
			throw new NotFoundException("Login mahasiswa gagal dilakukan");
		}

		return mahasiswa;
	}

	@GET
	@Path("login_nim/{username}/{pin}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil mahasiswa aktif berdasarkan NIM dan PIN (bukan password).
	 *
	 * @param username NIM mahasiswa
	 * @param pin      PIN akun mahasiswa
	 * @return entitas mahasiswa yang cocok
	 * @throws NotFoundException bila NIM/PIN tidak cocok
	 */
	public Mahasiswa getLoginNim(@PathParam("username") String username, @PathParam("pin") Long pin) {
		Session session = HibernateUtil.currentNativeSession();
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pin", pin)).setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();

		if (mahasiswa == null) {
			throw new NotFoundException("Login mahasiswa gagal dilakukan");
		}

		return mahasiswa;
	}

	@GET
	@Path("lihat_krs/{username}/{password}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu mengecek/melihat pengambilan KRS pada semester tertentu, mendelegasikan
	 * ke {@link KrsResourceHelper#checkAmbil}.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester yang KRS-nya dilihat
	 * @param krs      data KRS (dipetakan sebagai query param, bukan path — lihat {@code @PathParam("krs")} tanpa segmen path terkait)
	 * @return entitas mahasiswa
	 * @throws NotFoundException bila login gagal
	 */
	public Mahasiswa lihatKrs(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("semester") Integer semester, @PathParam("krs") String krs) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();

		if (mahasiswa != null) {
			KrsResourceHelper.checkAmbil(mahasiswa, semester, krs);
			return mahasiswa;
		}

		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@GET
	@Path("check_ambil_krs/{username}/{password}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu memeriksa kelayakan pengambilan KRS pada semester tertentu, mendelegasikan
	 * ke {@link KrsResourceHelper#checkAmbil}.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester yang diperiksa
	 * @param krs      data KRS yang diperiksa
	 * @return entitas mahasiswa
	 * @throws NotFoundException bila login gagal
	 */
	public Mahasiswa checkAmbilKrs(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("semester") Integer semester, @PathParam("krs") String krs) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();

		if (mahasiswa != null) {
			KrsResourceHelper.checkAmbil(mahasiswa, semester, krs);
			return mahasiswa;
		}

		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@GET
	@Path("ambil_krs/{username}/{password}/{semester}/{krs}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu mengambil (mendaftarkan) KRS pada semester tertentu, mendelegasikan ke
	 * {@link KrsResourceHelper#ambilKrs}.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester KRS yang diambil
	 * @param krs      data KRS yang diambil
	 * @return entitas mahasiswa
	 * @throws NotFoundException bila login gagal
	 */
	public Mahasiswa ambilKrs(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("semester") Integer semester, @PathParam("krs") String krs) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();

		if (mahasiswa != null) {
			KrsResourceHelper.ambilKrs(mahasiswa, semester, krs, null);
			return mahasiswa;
		}

		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@GET
	@Path("hapus_krs/{username}/{password}/{semester}/{krs}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu menghapus KRS pada semester tertentu, mendelegasikan ke
	 * {@link KrsResourceHelper#hapusKrs}.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester KRS yang dihapus
	 * @param krs      data KRS yang dihapus
	 * @return entitas mahasiswa
	 * @throws NotFoundException bila login gagal
	 */
	public Mahasiswa hapusKrs(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("semester") Integer semester, @PathParam("krs") String krs) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();

		if (mahasiswa != null) {
			KrsResourceHelper.hapusKrs(mahasiswa, semester, krs);
			return mahasiswa;
		}

		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("lihat_nilai/{username}/{password}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu mengambil daftar nilai perkuliahan (yang belum ditandai mengulang
	 * {@code ikutiPerkuliahan}), opsional difilter satu semester.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester yang nilainya diambil, atau {@code null} untuk semua semester
	 * @return daftar detail perkuliahan/nilai
	 * @throws NotFoundException bila login gagal
	 */
	public List<Detailperkuliahan> lihatNilai(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("semester") Integer semester) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		if (mahasiswa != null) {
			List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan")).addOrder(Order.asc("semester"))
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(semester == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("semester", semester))
					.list();
			//
			// HibernateUtil.closeSession();
			HibernateUtil.closeSession();
			return detailperkuliahans;
		}

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();
		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("lihat_detail_pembayaran/{username}/{password}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu mengambil rincian item biaya tagihan pembayaran mahasiswa lama pada
	 * semester tertentu (lewat {@link PembayaranUtil#getDetailBiayaMahasiswa}).
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester tagihan yang dilihat
	 * @return daftar rincian biaya
	 * @throws NotFoundException bila login gagal atau tagihan pembayaran semester tersebut tidak ditemukan
	 */
	public List<DetailBiaya> lihatDetailPembayaran(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("semester") Integer semester) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		if (mahasiswa != null) {
			JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			PembayaranMahasiswa pembayaranMahasiswa = pembayaranUtil.checkPembayaranMahasiswa(mahasiswa, semester,
					jenisKegiatan);
			if (pembayaranMahasiswa == null) {
				throw new NotFoundException("Pembayaran mahasiswa semester " + semester + " tidak ditemukan");
			}

			Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, semester,
					jenisKegiatan, false);
			List<DetailBiaya> myBiayas = new ArrayList<DetailBiaya>();
			for (DetailBiaya biaya : detailBiayas) {
				myBiayas.add(biaya);
			}
			//
			// HibernateUtil.closeSession();
			HibernateUtil.closeSession();
			return myBiayas;
		}

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();
		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@GET
	@Path("lihat_pembayaran/{username}/{password}/{semester}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mahasiswa lalu mengambil status tagihan pembayaran mahasiswa lama pada semester tertentu.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @param semester semester tagihan yang dilihat
	 * @return tagihan pembayaran mahasiswa
	 * @throws NotFoundException bila login gagal atau tagihan pembayaran semester tersebut tidak ditemukan
	 */
	public PembayaranMahasiswa lihatPembayaran(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("semester") Integer semester) {
		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username))
				.add(Restrictions.eq("pass", mypassword)).setMaxResults(1).uniqueResult();

		if (mahasiswa != null) {
			JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			PembayaranMahasiswa pembayaranMahasiswa = pembayaranUtil.checkPembayaranMahasiswa(mahasiswa, semester,
					jenisKegiatan);
			if (pembayaranMahasiswa == null) {
				throw new NotFoundException("Pembayaran mahasiswa semester " + semester + " tidak ditemukan");
			}
			//
			// HibernateUtil.closeSession();
			HibernateUtil.closeSession();

			return pembayaranMahasiswa;
		}

		//
		// HibernateUtil.closeSession();
		HibernateUtil.closeSession();
		throw new NotFoundException("Login mahasiswa gagal dilakukan");
	}

	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Mahasiswa getData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	/** Mengambil seluruh data {@link Mahasiswa} tanpa filter pencarian, setelah autentikasi. */
	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	/** Mengambil data {@link Mahasiswa} yang cocok dengan satu kata kunci pencarian, setelah autentikasi. */
	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	/** Mengambil data {@link Mahasiswa} yang cocok dengan dua kata kunci pencarian, setelah autentikasi. */
	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search, @PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

	/**
	 * Mengambil seluruh {@link TemplateSurat} yang tersedia, setelah autentikasi.
	 *
	 * @param username NIM mahasiswa
	 * @param password password akun (dikirim polos via path URL)
	 * @return daftar template surat
	 * @throws NotFoundException bila autentikasi gagal
	 */
	@GET
	@Path("jenis_surat/{username}/{password}")
	@Produces({ MediaType.APPLICATION_JSON })
	@SuppressWarnings("unchecked")
	public List<TemplateSurat> jenisPembayaran(@PathParam("username") String username,
			@PathParam("password") String password) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		Session session = HibernateUtil.currentNativeSession();
		List<TemplateSurat> templateSurats = session.createCriteria(TemplateSurat.class).list();

		HibernateUtil.closeSession();
		return templateSurats;
	}

	@GET
	@Path("report_log/{url}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mencatat satu baris {@link ReportLog} berisi URL yang diberikan. TIDAK memeriksa autentikasi
	 * apa pun.
	 *
	 * @param url string URL/keterangan yang dicatat sebagai log
	 * @return baris log yang baru disimpan
	 */
	public ReportLog reportLog(@PathParam("url") String url) {
		Session session = HibernateUtil.currentNativeSession();
		ReportLog reportLog = new ReportLog();
		reportLog.setKeterangan(url);
		session.getTransaction().begin();
		session.save(reportLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		return reportLog;
	}

	@GET
	@Path("biaya_surat/{templateSurat}/{bahasa}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil format template surat terbaru untuk satu template dan bahasa tertentu. TIDAK
	 * memeriksa autentikasi apa pun.
	 *
	 * @param templateSurat id {@link TemplateSurat} (string angka)
	 * @param bahasa        kode bahasa format surat
	 * @return format template surat yang cocok, atau instance kosong bila tidak ditemukan
	 */
	public FormatTemplateSurat getFormatTemplateSurat(@PathParam("templateSurat") String templateSurat,
			@PathParam("bahasa") String bahasa) {
		Session session = HibernateUtil.currentNativeSession();
		FormatTemplateSurat formatTemplateSurat = (FormatTemplateSurat) session
				.createCriteria(FormatTemplateSurat.class).setMaxResults(1)
				.add(Restrictions.eq("templateSurat.id", Long.parseLong(templateSurat.trim())))
				.add(Restrictions.eq("bahasa", bahasa)).addOrder(Order.desc("id")).uniqueResult();
		HibernateUtil.closeSession();
		return formatTemplateSurat == null ? new FormatTemplateSurat() : formatTemplateSurat;
	}

	@GET
	@Path("bayar/{jenisKegiatan}/{mahasiswa}/{itemBiaya}/{bahasa}/{nominal}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Membuat satu transaksi pembayaran ({@link Kegiatan}) untuk mahasiswa, jenis kegiatan, item
	 * biaya, dan nominal yang diberikan langsung lewat path URL: menyusun baris {@link DetailBiaya}
	 * dari data mahasiswa saat ini (angkatan, fakultas/jurusan, jenjang, status, dsb.) dengan nominal
	 * sesuai parameter, menyimpannya, lalu memicu {@link PembayaranUtil#simpanPembayaranMahasiswa}.
	 *
	 * <p>
	 * <b>Catatan keamanan:</b> method ini TIDAK melakukan autentikasi/otorisasi apa pun — tidak ada
	 * pemeriksaan login atau kepemilikan sesi. Siapa pun yang mengetahui/menebak URL dapat memicu
	 * pembuatan transaksi pembayaran untuk mahasiswa mana pun dengan nominal sembarang.
	 * </p>
	 *
	 * @param jenisKegiatan id {@link JenisKegiatan} (string angka)
	 * @param mahasiswa     id {@link Mahasiswa} (string angka)
	 * @param itemBiaya     id {@link ItemBiaya} (string angka)
	 * @param bahasa        kode bahasa untuk detail biaya
	 * @param nominal       nominal pembayaran (string angka, tidak divalidasi batas)
	 * @return kegiatan pembayaran yang terbentuk
	 */
	public Kegiatan bayar(@PathParam("jenisKegiatan") String jenisKegiatan, @PathParam("mahasiswa") String mahasiswa,
			@PathParam("itemBiaya") String itemBiaya, @PathParam("bahasa") String bahasa,
			@PathParam("nominal") String nominal) {
		Session session = HibernateUtil.currentNativeSession();
		Mahasiswa aMahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(mahasiswa.trim()))).uniqueResult();

		JenisKegiatan aJenisKegiatan = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(jenisKegiatan.trim()))).uniqueResult();

		ItemBiaya aItemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
				.add(Restrictions.idEq(Long.parseLong(itemBiaya.trim()))).uniqueResult();

		Double aNominal = Double.parseDouble(nominal);

		String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		Integer semester = Common.getSemester(aMahasiswa.getTahunangkatan(), semesterMulai,
				aMahasiswa.getPindahKeKampusIniMasukSemester(), aMahasiswa.getSemesterMulai());

		StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(aMahasiswa).getStatusMahasiswa();

		DetailBiaya detailBiaya = new DetailBiaya();
		detailBiaya.setAngkatan(aMahasiswa.getTahunangkatan());
		detailBiaya.setBahasa(bahasa);
		detailBiaya.setFakultas(aMahasiswa.getJurusan().getFakultas());
		detailBiaya.setItemBiaya(aItemBiaya);
		detailBiaya.setJenisKegiatan(aJenisKegiatan);
		detailBiaya.setJenjang(aMahasiswa.getJenjang());
		detailBiaya.setJurusan(aMahasiswa.getJurusan());
		detailBiaya.setKeterangan("VCM");
		detailBiaya.setMerupakanPembayaran(true);
		detailBiaya.setMulaiBelajarDiSemester(aMahasiswa.getSemesterMulai());
		detailBiaya.setNama("VCM");
		detailBiaya.setNilaiBiaya(aNominal);
		detailBiaya.setProgram(aMahasiswa.getProgram());
		detailBiaya.setSemester(semester);
		detailBiaya.setStatusMahasiswa(statusMahasiswa);
		detailBiaya.setTahunAkademik(Common.getCurrentTahunAkademik());
		detailBiaya.setWnaAtauWni(aMahasiswa.getWarganegara());

		session.getTransaction().begin();
		session.save(detailBiaya);
		session.getTransaction().commit();

		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		detailBiayas.add(detailBiaya);
		HibernateUtil.closeSession();

		Kegiatan kegiatan = pembayaranUtil.simpanPembayaranMahasiswa(null, null, aJenisKegiatan, aMahasiswa,
				detailBiayas, aNominal.doubleValue(), null, "_" + (++Common.increments));

		return kegiatan;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("mahasiswa/{username}/{password}/{nim_nama}/{start}/{max}")
	@Produces({ MediaType.TEXT_PLAIN })
	/**
	 * Mencari mahasiswa aktif berdasarkan NIM/nama (kata kunci diberikan URL-encoded, dengan garis
	 * bawah sebagai placeholder karakter yang di-strip sebelum decode), dengan paging manual
	 * ({@code start}/{@code max}), setelah autentikasi. Hasil dikembalikan sebagai teks JSON manual
	 * (bukan lewat {@code @Produces(APPLICATION_JSON)} otomatis) berisi id, NIM, nama, angkatan,
	 * program studi, program, status awal, dan status mahasiswa saat ini.
	 *
	 * @param username NIM untuk autentikasi (URL-encoded, garis bawah di-strip sebelum decode)
	 * @param password password untuk autentikasi (URL-encoded, garis bawah di-strip sebelum decode)
	 * @param nim_nama kata kunci pencarian NIM/nama (URL-encoded)
	 * @param start    indeks awal hasil (paging), default 0 bila kosong/bukan angka
	 * @param max      jumlah maksimum hasil (paging), default 10 bila kosong/bukan angka
	 * @return string JSON array berisi data mahasiswa yang cocok
	 * @throws Exception termasuk {@link NotFoundException} bila autentikasi gagal, atau kegagalan decode URL
	 */
	public String pembayaranMahasiswa(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("nim_nama") String nim_nama, @PathParam("start") String start, @PathParam("max") String max)
			throws Exception {

		username = URLDecoder.decode(username.replaceAll("_", ""), "UTF-8");
		password = URLDecoder.decode(password.replaceAll("_", ""), "UTF-8");
		nim_nama = URLDecoder.decode(nim_nama.replaceAll("_", "").trim(), "UTF-8");

		start = URLDecoder.decode(start.replaceAll("_", "").trim(), "UTF-8");
		max = URLDecoder.decode(max.replaceAll("_", "").trim(), "UTF-8");

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		System.out.println("nim_nama => " + nim_nama + ", start => " + start + ", max => " + max);

		Session session = HibernateUtil.currentNativeSession();

		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("tahunangkatan"))
				.addOrder(Order.asc("nim"))

				.add(nim_nama == null || nim_nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nim", nim_nama, MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nim_nama, MatchMode.ANYWHERE)))

				.setFirstResult(start == null || start.trim().isEmpty() || !Common.isNumber(start) ? 0
						: Integer.parseInt(start.trim()))
				.setMaxResults(max == null || max.trim().isEmpty() || !Common.isNumber(max) ? 10
						: Integer.parseInt(max.trim()))

				.list();

		HibernateUtil.closeSession();

		JSONArray array = new JSONArray();

		for (Mahasiswa mahasiswa : mahasiswas) {
			JSONObject json = new JSONObject();

			json.put("id", mahasiswa.getId());
			json.put("nim", mahasiswa.getNim());
			json.put("nama", mahasiswa.getNama());
			json.put("angkatan", mahasiswa.getTahunangkatan());
			json.put("prodi", mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
			json.put("program", mahasiswa.getProgram());
			json.put("status_awal",
					mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama());
			json.put("status", ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa().getNama());
			array.put(json);
		}

		return array.toString();
	}
}
