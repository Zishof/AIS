package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.TampilanPengumumanPMBAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Servlet endpoint PMB (Penerimaan Mahasiswa Baru) varian "3": menyiapkan empat kumpulan data
 * (pengumuman akademis, gelombang pendaftaran/jalur aktif, daftar program studi/jurusan, dan
 * forum diskusi pengumuman) dalam bentuk JSON, ditambah URL banner khusus perguruan tinggi, lalu
 * meneruskan (forward) permintaan ke {@code /WEB-INF/u/pmb3.jsp} untuk dirender. Merupakan
 * varian beranda PMB yang sejalur dengan {@code Pmb} dan {@link Pmb2} — struktur logikanya
 * sangat mirip (populate {@link PengumumanAkademis}/{@link GelombangPendaftaran}/{@link Jurusan}
 * lalu forward ke JSP berbeda); dibanding {@link Pmb2}, varian ini menambahkan key JSON yang
 * berbeda (mis. {@code cover} bukan {@code gambar}), forum diskusi gabungan lintas-pengumuman
 * ({@code forumData}), dan banner kop PMB per perguruan tinggi.
 *
 * <p>
 * Sama seperti {@link Pmb2}, data di-scope per {@link PerguruanTinggi} (tenant) berdasarkan
 * {@link PerguruanTinggiUtil#getPerguruanTinggi(HttpServletRequest)}: gelombang pendaftaran yang
 * ditampilkan hanya yang cocok dengan perguruan tinggi terpilih (diperbaiki r85516 — sebelumnya
 * listing gelombang tidak ikut disaring per tenant walau {@link Jurusan} sudah disaring, sehingga
 * berpotensi menampilkan gelombang milik perguruan tinggi lain pada instalasi multi-tenant).
 * </p>
 */
public class Pmb3 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Pmb3() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan POST dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mengonversi satu {@link PengumumanAkademis} (pengumuman akademis) menjadi
	 * {@link JSONObject} untuk beranda PMB varian 3: menyertakan id, sampul (diambil dari URL
	 * gambar pertama pada HTML isi pengumuman, atau gambar default {@code /img/pengumuman.png}
	 * bila tidak ada), judul, dan isi. Berbeda dengan {@link Pmb2}, key {@code link} dan
	 * {@code lampiran} pada varian ini masih berupa placeholder {@code "#"} (belum ditautkan ke
	 * data nyata).
	 *
	 * @param pengumumanAkademis pengumuman akademis yang akan dikonversi
	 * @param request permintaan HTTP, dipakai untuk membentuk URL gambar default
	 * @param session sesi Hibernate aktif (tidak dipakai langsung, disediakan untuk konsistensi
	 *                 signature dengan method {@code populate} lain)
	 * @return objek JSON representasi pengumuman
	 * @throws Exception bila terjadi galat saat mengekstrak URL gambar
	 */
	private JSONObject populate(PengumumanAkademis pengumumanAkademis, HttpServletRequest request, Session session)
			throws Exception {

		String html = pengumumanAkademis.getCatatan();
		List<String> urlGambar = Common.ambilUrlGambarDariHtml(html);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", pengumumanAkademis.getId());
		jsonObject.put("cover", urlGambar.isEmpty() ? Common.getRequestHostWithProtocol(request) + "/img/pengumuman.png"
				: urlGambar.get(0));
		jsonObject.put("title", pengumumanAkademis.getNama());
		jsonObject.put("content", pengumumanAkademis.getCatatan());

		jsonObject.put("link", "#");
		jsonObject.put("lampiran", "#");
		return jsonObject;
	}

	/**
	 * Mengonversi satu {@link GelombangPendaftaran} (gelombang/jalur pendaftaran) menjadi
	 * {@link JSONObject}: id, sampul (lampiran {@link LampiranLain#ICON_GELOMBANG_PMB} atau
	 * gambar default {@code /img/jalur.png} bila belum diunggah), nama, info, keterangan, dan
	 * tautan brosur (lampiran info tambahan {@code "INFO_PMB"}) bila ada. Key {@code biaya}
	 * sengaja dinonaktifkan (dikomentari) pada kode ini.
	 *
	 * @param gelombangPendaftaran gelombang pendaftaran yang akan dikonversi
	 * @param request permintaan HTTP, dipakai untuk membentuk URL gambar default
	 * @param session sesi Hibernate aktif (tidak dipakai langsung, disediakan untuk konsistensi
	 *                 signature dengan method {@code populate} lain)
	 * @return objek JSON representasi gelombang pendaftaran
	 * @throws Exception bila terjadi galat saat mengambil lampiran
	 */
	private JSONObject populate(GelombangPendaftaran gelombangPendaftaran, HttpServletRequest request, Session session)
			throws Exception {

		LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaran.getId(), LampiranLain.ICON_GELOMBANG_PMB);
		LampiranLain lampiranLainInfo = LampiranLain.ambil(gelombangPendaftaran.getId(), "INFO_PMB");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", gelombangPendaftaran.getId());
		jsonObject.put("cover",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/jalur.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", gelombangPendaftaran.getNama());
		jsonObject.put("info", gelombangPendaftaran.getInfo());
		jsonObject.put("keterangan", gelombangPendaftaran.getKeterangan());

//		jsonObject.put("biaya", gelombangPendaftaran.getNama());

		if (lampiranLainInfo != null) {
			jsonObject.put("brosur", lampiranLainInfo.createLinkUri());
		}

		return jsonObject;
	}

	/**
	 * Mengonversi satu {@link Jurusan} (program studi) menjadi {@link JSONObject}: id, sampul
	 * ikon program studi (lampiran {@link LampiranLain#ICON_JURUSAN} atau gambar default
	 * {@code /img/prodi.png} bila belum diunggah), nama, deskripsi ringkas (dipotong 50 karakter
	 * via {@link Common#simpleString(String, int)}), deskripsi lengkap ({@code infoRinci}),
	 * keterangan, dan tautan lampiran info tambahan ({@link LampiranLain#INFO_JURUSAN}) bila ada.
	 * Key {@code link} pada varian ini masih berupa placeholder {@code "#"}.
	 *
	 * @param jurusan program studi yang akan dikonversi
	 * @param request permintaan HTTP, dipakai untuk membentuk URL gambar default
	 * @param session sesi Hibernate aktif (tidak dipakai langsung, disediakan untuk konsistensi
	 *                 signature dengan method {@code populate} lain)
	 * @return objek JSON representasi program studi
	 * @throws Exception bila terjadi galat saat mengambil lampiran
	 */
	private JSONObject populate(Jurusan jurusan, HttpServletRequest request, Session session) throws Exception {

		LampiranLain lampiranLain = LampiranLain.ambil(jurusan.getId(), LampiranLain.ICON_JURUSAN);
		LampiranLain infoProdi = LampiranLain.ambil(jurusan.getId(), LampiranLain.INFO_JURUSAN);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", jurusan.getId());
		jsonObject.put("cover",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/prodi.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", jurusan.getNama());
		jsonObject.put("deskripsi", Common.simpleString(jurusan.getDeskripsi(), 50));
		jsonObject.put("infoRinci", jurusan.getDeskripsi());

		jsonObject.put("keterangan", jurusan.getKeterangan());
		jsonObject.put("link", "#");
		if (infoProdi != null) {
			jsonObject.put("lampiran", infoProdi.createLinkUri());
		}

		return jsonObject;
	}

	/**
	 * Membangun data beranda PMB varian 3: mengambil daftar pengumuman akademis aktif untuk
	 * perguruan tinggi terpilih (maksimal 100 baris), menyaring gelombang pendaftaran yang aktif,
	 * bisa dipilih pendaftar online, berada dalam rentang tanggal berjalan, dan cocok dengan
	 * tenant terpilih lalu mengurutkannya, mengambil daftar jurusan aktif pada perguruan tinggi
	 * terpilih, menggabungkan seluruh diskusi ({@link DiskusiPengumumanAkademis}) milik
	 * pengumuman-pengumuman yang diambil menjadi {@code forumData}, dan menentukan URL banner
	 * (lampiran {@link LampiranLain#KOP_PMB_PT} milik perguruan tinggi bila ada, atau
	 * {@code /img/banner_pmb.jpg} sebagai default). Seluruh data diteruskan sebagai request
	 * attribute ({@code pengumumanData}, {@code jalurData}, {@code prodiData},
	 * {@code forumData}, {@code banner}) ke {@code /WEB-INF/u/pmb3.jsp}. Sesi Hibernate yang
	 * dibuka di awal selalu dibersihkan dan ditutup pada blok {@code finally}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception bila terjadi galat query database atau saat forward ke JSP
	 */
	@SuppressWarnings({ "unchecked" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Session session = HibernateUtil.getSessionFactory().openSession();
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

		JSONArray pengumumanData = new JSONArray();
		JSONArray jalurData = new JSONArray();
		JSONArray prodiData = new JSONArray();
		JSONArray forumData = new JSONArray();
		String bannerUrl = Common.getRequestHostWithProtocol(request) + "/img/banner_pmb.jpg";
		try {
			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(TampilanPengumumanPMBAction
					.initCriteriaStatic(true, selectedPerguruanTinggi).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100),
					PengumumanAkademis.class);

			for (PengumumanAkademis pengumumanAkademis : listPengumumanAkademis) {
				pengumumanData.put(populate(pengumumanAkademis, request, session));
			}

			Date sekarang = WaktuUtil.getDate();
			List<GelombangPendaftaran> gelombangPendaftaransData = new ArrayList<GelombangPendaftaran>();
			Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues.ambilBerdasarClass(GelombangPendaftaran.class);
			for (Long gelId : gelombangsAktif.keySet()) {
				GelombangPendaftaran gelombangPendaftaran1 = (GelombangPendaftaran) ConstantValues
						.ambil(GelombangPendaftaran.class.getName(), gelId);
				PerguruanTinggi ptGelombang1 = gelombangPendaftaran1 == null ? null
						: gelombangPendaftaran1.getPerguruanTinggi();
				boolean ptCocok1 = selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						|| (ptGelombang1 != null && ptGelombang1.getId() != null
								&& ptGelombang1.getId().equals(selectedPerguruanTinggi.getId()));
				if (gelombangPendaftaran1 != null && gelombangPendaftaran1.getBisaDipilihPendaftarOnline()
						&& gelombangPendaftaran1.getAktif() && ptCocok1
						&& (gelombangPendaftaran1.getMulai().before(sekarang) || Common.dateFormat8.get()
								.format(gelombangPendaftaran1.getMulai()).equals(Common.dateFormat8.get().format(sekarang)))
						&& (gelombangPendaftaran1.getSampai().after(sekarang) || Common.dateFormat8.get()
								.format(gelombangPendaftaran1.getSampai()).equals(Common.dateFormat8.get().format(sekarang)))) {
					gelombangPendaftaransData.add(gelombangPendaftaran1);

				}
			}

			Collections.sort(gelombangPendaftaransData);

			for (GelombangPendaftaran gelombangPendaftaran : gelombangPendaftaransData) {
				jalurData.put(populate(gelombangPendaftaran, request, session));
			}

			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(Jurusan.class)
					.createAlias("fakultas", "fakultas")
					.add(Restrictions.eq("fakultas.perguruanTinggi", selectedPerguruanTinggi))
					.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					Jurusan.class);

			for (Jurusan jurusan : jurusans) {
				prodiData.put(populate(jurusan, request, session));
			}

			List<DiskusiPengumumanAkademis> diskusiPengumumanAkademis = listPengumumanAkademis.isEmpty()
					? new ArrayList<DiskusiPengumumanAkademis>()
					: session.createCriteria(DiskusiPengumumanAkademis.class)
							.add(Restrictions.in("pengumumanAkademis", listPengumumanAkademis)).addOrder(Order.desc("id"))
							.list();

			for (DiskusiPengumumanAkademis peminjamanPengadaanItemDetail : diskusiPengumumanAkademis) {
				JSONObject jsonObjectHistory = new JSONObject();
				jsonObjectHistory.put("pengumumanId", peminjamanPengadaanItemDetail.getId());
				jsonObjectHistory.put("user", peminjamanPengadaanItemDetail.getOleh());

				jsonObjectHistory.put("catatan", peminjamanPengadaanItemDetail.getCatatan());
				jsonObjectHistory.put("comment", Common.dateFormat33.get().format(peminjamanPengadaanItemDetail.getTanggal()));

				forumData.put(jsonObjectHistory);

			}
			diskusiPengumumanAkademis.clear();
			diskusiPengumumanAkademis = null;

			LampiranLain lampiranLain = LampiranLain.ambil(selectedPerguruanTinggi.getId(), LampiranLain.KOP_PMB_PT);
			if (lampiranLain != null && lampiranLain.getId() != null) {
				bannerUrl = lampiranLain.createLinkUri();
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:222");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:223");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:224");}
			}
		}

		request.setAttribute("pengumumanData", pengumumanData.toString());
		request.setAttribute("jalurData", jalurData.toString());
		request.setAttribute("prodiData", prodiData.toString());
		request.setAttribute("forumData", forumData.toString());
		request.setAttribute("banner", bannerUrl);

		request.getRequestDispatcher("/WEB-INF/u/pmb3.jsp").forward(request, response);
	}

}
