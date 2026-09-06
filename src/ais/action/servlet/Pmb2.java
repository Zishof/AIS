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
 * Servlet endpoint PMB (Penerimaan Mahasiswa Baru) varian "2": menyiapkan tiga kumpulan data
 * (pengumuman akademis, gelombang pendaftaran/jalur aktif, dan daftar program studi/jurusan)
 * dalam bentuk JSON lalu meneruskan (forward) permintaan ke {@code /WEB-INF/u/pmb2.jsp} untuk
 * dirender. Merupakan varian beranda PMB yang sejalur dengan {@code Pmb} dan {@link Pmb3} —
 * struktur logikanya sangat mirip (populate {@link PengumumanAkademis}/
 * {@link GelombangPendaftaran}/{@link Jurusan} lalu forward ke JSP berbeda); perbedaan
 * antar-varian terutama pada nama key JSON yang diekspos ke JSP dan berkas JSP tujuannya.
 *
 * <p>
 * Data di-scope per {@link PerguruanTinggi} (tenant) berdasarkan
 * {@link PerguruanTinggiUtil#getPerguruanTinggi(HttpServletRequest)}: gelombang pendaftaran yang
 * ditampilkan hanya yang cocok dengan perguruan tinggi terpilih (diperbaiki r85515 — sebelumnya
 * listing gelombang tidak ikut disaring per tenant walau {@link Jurusan} sudah disaring, sehingga
 * berpotensi menampilkan gelombang milik perguruan tinggi lain pada instalasi multi-tenant).
 * </p>
 */
public class Pmb2 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Pmb2() {
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
	 * {@link JSONObject} untuk beranda PMB: menyertakan id, judul, gambar sampul (diambil dari
	 * URL gambar pertama pada HTML isi pengumuman, atau gambar default {@code /img/pengumuman.png}
	 * bila tidak ada), isi pengumuman, dan daftar diskusi ({@link DiskusiPengumumanAkademis})
	 * terkait diurutkan dari yang terbaru.
	 *
	 * @param pengumumanAkademis pengumuman akademis yang akan dikonversi
	 * @param request permintaan HTTP, dipakai untuk membentuk URL gambar default
	 * @param session sesi Hibernate aktif untuk mengambil daftar diskusi terkait
	 * @return objek JSON representasi pengumuman beserta diskusinya
	 * @throws Exception bila terjadi galat saat mengekstrak URL gambar atau query diskusi
	 */
	@SuppressWarnings("unchecked")
	private JSONObject populate(PengumumanAkademis pengumumanAkademis, HttpServletRequest request, Session session)
			throws Exception {

		String html = pengumumanAkademis.getCatatan();
		List<String> urlGambar = Common.ambilUrlGambarDariHtml(html);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", pengumumanAkademis.getId());
		jsonObject.put("gambar",
				urlGambar.isEmpty() ? Common.getRequestHostWithProtocol(request) + "/img/pengumuman.png"
						: urlGambar.get(0));
		jsonObject.put("judul", pengumumanAkademis.getNama());
		jsonObject.put("isi", pengumumanAkademis.getCatatan());

		List<DiskusiPengumumanAkademis> diskusiPengumumanAkademis = session
				.createCriteria(DiskusiPengumumanAkademis.class)
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).addOrder(Order.desc("id")).list();

		JSONArray diskusi = new JSONArray();
		for (DiskusiPengumumanAkademis peminjamanPengadaanItemDetail : diskusiPengumumanAkademis) {
			JSONObject jsonObjectHistory = new JSONObject();
			jsonObjectHistory.put("id", peminjamanPengadaanItemDetail.getId());
			jsonObjectHistory.put("oleh", peminjamanPengadaanItemDetail.getOleh());

			jsonObjectHistory.put("catatan", peminjamanPengadaanItemDetail.getCatatan());

			diskusi.put(jsonObjectHistory);

		}
		diskusiPengumumanAkademis.clear();
		diskusiPengumumanAkademis = null;
		jsonObject.put("diskusi", diskusi);
		return jsonObject;
	}

	/**
	 * Mengonversi satu {@link GelombangPendaftaran} (gelombang/jalur pendaftaran) menjadi
	 * {@link JSONObject}: id, gambar ikon (lampiran {@link LampiranLain#ICON_GELOMBANG_PMB} atau
	 * gambar default {@code /img/jalur.png} bila belum diunggah), nama, deskripsi/info,
	 * keterangan, dan tautan lampiran info tambahan ({@code "INFO_PMB"}) bila ada.
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
		jsonObject.put("gambar",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/jalur.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", gelombangPendaftaran.getNama());
		jsonObject.put("deskripsi", gelombangPendaftaran.getInfo());
		jsonObject.put("keterangan", gelombangPendaftaran.getKeterangan());

		if (lampiranLainInfo != null) {
			jsonObject.put("lampiran", lampiranLainInfo.createLinkUri());
		}

		return jsonObject;
	}

	/**
	 * Mengonversi satu {@link Jurusan} (program studi) menjadi {@link JSONObject}: id, gambar
	 * ikon program studi (lampiran {@link LampiranLain#ICON_JURUSAN} atau gambar default
	 * {@code /img/prodi.png} bila belum diunggah), nama, deskripsi, keterangan, dan tautan
	 * lampiran info tambahan ({@link LampiranLain#INFO_JURUSAN}) bila ada.
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
		jsonObject.put("gambar",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/prodi.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", jurusan.getNama());
		jsonObject.put("info", jurusan.getDeskripsi());
		jsonObject.put("keterangan", jurusan.getKeterangan());

		if (infoProdi != null) {
			jsonObject.put("lampiran", infoProdi.createLinkUri());
		}

		return jsonObject;
	}

	/**
	 * Membangun data beranda PMB varian 2: mengambil daftar pengumuman akademis aktif untuk
	 * perguruan tinggi terpilih (maksimal 100 baris), menyaring gelombang pendaftaran yang aktif,
	 * bisa dipilih pendaftar online, berada dalam rentang tanggal berjalan, dan cocok dengan
	 * tenant terpilih lalu mengurutkannya, serta mengambil daftar jurusan aktif pada perguruan
	 * tinggi terpilih. Ketiga kumpulan data dikonversi ke JSON lalu diteruskan sebagai request
	 * attribute ({@code pengumumanData}, {@code jalurData}, {@code prodiData}) ke
	 * {@code /WEB-INF/u/pmb2.jsp}. Sesi Hibernate yang dibuka di awal selalu dibersihkan dan
	 * ditutup pada blok {@code finally}.
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
		List<PengumumanAkademis> listPengumumanAkademis = null;
		try {
			listPengumumanAkademis = ConstantValues.simpleList(TampilanPengumumanPMBAction
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
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb2.java:210");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb2.java:211");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb2.java:212");}
			}
		}

		request.setAttribute("pengumumanData", pengumumanData.toString());
		request.setAttribute("jalurData", jalurData.toString());
		request.setAttribute("prodiData", prodiData.toString());

		request.getRequestDispatcher("/WEB-INF/u/pmb2.jsp").forward(request, response);
	}

}
