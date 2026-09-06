package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import ais.action.master.PengumumanAkademisAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoDosen;
import ais.ui.util.WaktuUtil;

/**
 * Servlet dasbor jadwal perkuliahan: merangkai jadwal mingguan (semua {@link Perkuliahan} pada
 * tahun ajaran/semester berjalan) dan jadwal harian (pertemuan hari ini, memakai cache statis
 * {@link PengumumanAkademisAction#pertemuansHarian} sehingga tidak query ulang bila entri untuk
 * tanggal berjalan sudah ada) ke dalam dua array JSON, lalu forward ke {@code jadwal.jsp}.
 *
 * <p>
 * Tidak ada penyaringan satuan kerja/tenant pada query {@link Perkuliahan} — seluruh jadwal
 * kuliah aktif pada semester berjalan diikutsertakan tanpa memandang program studi/perguruan
 * tinggi pemanggil, konsisten dengan sifat servlet ini sebagai tampilan informasi jadwal publik
 * (mis. papan pengumuman), bukan data per-pengguna.
 * </p>
 */
public class Jadwal extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor baku servlet, tanpa inisialisasi tambahan.
	 */
	public Jadwal() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat tak tertangani hanya
	 * ditampilkan bila pemanggil admin (lihat {@link Common#tampilErrorJikaAdmin(Exception)}),
	 * tidak dilempar ulang ke kontainer.
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
	 * Menangani permintaan POST dengan perilaku identik {@link #doGet}: mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan meredam galat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
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
	 * Membentuk satu baris JSON tampilan jadwal untuk sebuah {@link Perkuliahan} dan (opsional)
	 * satu {@link Dosen} pengajarnya: foto dosen (fallback ikon generik bila dosen null atau
	 * tidak punya foto), nama, NIDN, nama matakuliah beserta semester/kelas, hari, rentang jam,
	 * dan nama ruangan.
	 *
	 * @param perkuliahan baris perkuliahan sumber data jadwal; tidak boleh null
	 * @param dosen dosen pengajar yang ditampilkan pada baris ini, atau {@code null} bila
	 *        perkuliahan belum/tidak punya dosen ("Tanpa Dosen" ditampilkan sebagai nama)
	 * @param request dipakai untuk menyusun URL absolut ikon fallback via
	 *        {@link Common#getRequestHostWithProtocol(HttpServletRequest)}
	 * @return objek JSON siap dimasukkan ke array jadwal mingguan/harian
	 * @throws Exception diteruskan dari operasi pengambilan foto dosen ({@link FileFotoLain#ambil})
	 */
	private JSONObject populate(Perkuliahan perkuliahan, Dosen dosen, HttpServletRequest request) throws Exception {
		FileFotoLain fotodosen = dosen == null ? null
				: FileFotoLain.ambil(dosen.getId(), FotoDosen.DEFAULT_JENIS, FotoDosen.class);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("foto",
				dosen == null ? (Common.getRequestHostWithProtocol(request) + "/img/graduated-icon.png")
						: (fotodosen == null || fotodosen.getId() == null
								? Common.getRequestHostWithProtocol(request) + "/img/graduated-icon.png"
								: fotodosen.createLinkUri()));
		jsonObject.put("nama", dosen == null ? "Tanpa Dosen" : dosen.getNama());
		jsonObject.put("nidn", dosen == null ? "" : dosen.getNidn());
		jsonObject.put("matakuliah", (perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama())
				+ " (" + perkuliahan.getSemester() + " " + perkuliahan.getKelas() + ")");
		jsonObject.put("hari", perkuliahan.getHari());
		jsonObject.put("jam", perkuliahan.getWaktuMulai() == null ? ""
				: (perkuliahan.getWaktuMulai() + " sd " + perkuliahan.getWaktuSelesai()));
		jsonObject.put("ruangan", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());

		return jsonObject;
	}

	/**
	 * Menyusun data untuk {@code jadwal.jsp}: (1) jadwal mingguan — semua {@link Perkuliahan}
	 * pada tahun ajaran dan semester (ganjil/genap) berjalan yang punya hari dan waktu mulai
	 * terisi, diurutkan Senin→Minggu lalu jam mulai, satu baris JSON per dosen pengajar (atau
	 * satu baris "Tanpa Dosen" bila tidak ada dosen terdaftar); (2) jadwal harian — pertemuan
	 * hari ini yang aktif dan terkait jadwal pelajaran/perkuliahan, diambil dari cache statis
	 * {@link PengumumanAkademisAction#pertemuansHarian} (query database hanya dijalankan sekali
	 * per tanggal, hasilnya disimpan di cache untuk permintaan berikutnya pada tanggal yang
	 * sama). Hasil akhir disimpan sebagai atribut request {@code weeklySchedulesData} dan
	 * {@code dailySchedulesData} (JSON string), lalu di-forward ke {@code /WEB-INF/u/jadwal.jsp}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan diisi hasil forward
	 * @throws Exception diteruskan dari operasi Hibernate/pembentukan JSON di dalam method ini
	 */
	@SuppressWarnings({ "unchecked" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Session session = HibernateUtil.getSessionFactory().openSession();
		JSONArray weeklySchedulesData = new JSONArray();
		JSONArray dailySchedulesData = new JSONArray();
		try {
			List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session.createCriteria(Perkuliahan.class)
					.add(Restrictions.eq("tahunAjaran", Common.getCurrentTahunAkademik()))
					.add(Restrictions.eq("ganjilGenap",
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP))

					.add(Restrictions.ne("hari", "")).add(Restrictions.isNotNull("hari"))

					.add(Restrictions.ge("waktuMulaiD", 0.1))

					.add(Restrictions.sqlRestriction(
							"1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"))

					, Perkuliahan.class);

			for (Perkuliahan perkuliahan : perkuliahans) {
				List<Dosen> dosens = perkuliahan.populateDosenBuNama();
				if (dosens.isEmpty()) {
					Dosen dosen = null;
					weeklySchedulesData.put(populate(perkuliahan, dosen, request));
				} else {
					for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
						weeklySchedulesData.put(populate(perkuliahan, dosen, request));
					}
				}
			}

			request.setAttribute("weeklySchedulesData", weeklySchedulesData.toString());

			String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
			Map<Long, Pertemuan> pertemuans = PengumumanAkademisAction.pertemuansHarian.get(sekarang);
			if (pertemuans == null) {

				List<Pertemuan> pertemuansData = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
						.add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
								Restrictions.isNotNull("perkuliahan")))
						.list();
				pertemuans = new java.util.concurrent.ConcurrentHashMap<Long, Pertemuan>();
				for (Pertemuan pertemuan : pertemuansData) {
					pertemuans.put(pertemuan.getId(), pertemuan);
				}
				pertemuansData = null;
				PengumumanAkademisAction.pertemuansHarian.put(sekarang, pertemuans);
			}

			for (Map<Long, Pertemuan> pertemuansD : new ArrayList<Map<Long, Pertemuan>>(
					PengumumanAkademisAction.pertemuansHarian.values())) {
				for (Pertemuan pertemuan : new ArrayList<Pertemuan>(pertemuansD.values())) {
					if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
						Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
						List<Dosen> dosens = perkuliahan.populateDosenBuNama();
						if (dosens.isEmpty()) {
							Dosen dosen = null;
							dailySchedulesData.put(populate(perkuliahan, dosen, request));
						} else {
							for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
								dailySchedulesData.put(populate(perkuliahan, dosen, request));
							}
						}
					}
				}
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:163");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:164");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:165");}
			}
		}
		request.setAttribute("weeklySchedulesData", weeklySchedulesData.toString());
		request.setAttribute("dailySchedulesData", dailySchedulesData.toString());
		request.setAttribute("tanggal", Common.dateFormat6.get().format(WaktuUtil.getDate()));

		request.getRequestDispatcher("/WEB-INF/u/jadwal.jsp").forward(request, response);
	}

}
