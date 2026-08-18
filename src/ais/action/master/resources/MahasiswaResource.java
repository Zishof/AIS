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



public class MahasiswaResource extends DataResource<Mahasiswa> {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public MahasiswaResource() {
		super(Mahasiswa.class);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public MahasiswaResource getXml() {
		return this;
	}

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

	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Mahasiswa> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search, @PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

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
