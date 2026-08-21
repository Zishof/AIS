package ais.action.master.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyHtml;
import ais.ui.util.MyThemeProvider;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>DaftarBimbinganPerDosenHelper — Papan "Mahasiswa Bimbingan per Dosen PA"</h2>
 *
 * <p>
 * Kelas bantu (helper) yang bertugas <b>menampilkan daftar mahasiswa bimbingan yang
 * dikelompokkan per Dosen Pembimbing Akademik (Dosen PA)</b> dalam sebuah jendela
 * modal yang rapi, modern, dan mengikuti warna tema aplikasi. Fitur ini dipanggil dari
 * tombol <i>"Daftar Bimbingan"</i> yang diletakkan bersebelahan dengan tombol
 * <i>"Download"</i> pada halaman <b>Bimbingan KRS</b>
 * ({@code krs_mahasiswa.zul} / {@code ais.action.master.KrsMahasiswaAction}). Tujuannya
 * sederhana: seorang petugas Akademik, Kaprodi, atau Dosen PA dapat melihat dengan
 * sekali klik "dosen A membimbing mahasiswa siapa saja", tanpa harus menelusuri baris
 * per baris pada grid utama yang tersusun per mahasiswa/semester.
 * </p>
 *
 * <h3>Cara kerja &amp; alur data</h3>
 * <p>
 * Helper ini sengaja dibuat <b>tidak mengakses basis data secara langsung</b>. Data
 * sudah disiapkan oleh pemanggil ({@code KrsMahasiswaAction}) melalui method
 * {@code initCriteria(true).list()} yang MENGIKUTI seluruh filter pencarian yang sedang
 * aktif di layar (nama/NIM mahasiswa, dosen, tahun ajaran, ganjil/genap, rentang
 * semester, fakultas, prodi, program, angkatan, dan sebagainya). Dengan begitu, isi
 * papan ini selalu konsisten dengan apa yang sedang tampil pada grid. Pemisahan ini
 * juga menjaga aturan sesi Hibernate: pemanggil memakai
 * {@code HibernateUtil.currentSession()} (sesi yang dikelola kontainer ZK) sehingga
 * <u>tidak boleh ditutup manual</u>; helper hanya membaca properti entity yang sudah
 * ter-load pada sesi tersebut. Tidak ada {@code openSession()} maupun
 * {@code currentNativeSession()} di kelas ini, sehingga tidak ada sesi yang perlu
 * ditutup dalam blok {@code finally}.
 * </p>
 *
 * <p>
 * Langkah pengolahan data dilakukan sekali jalan dan hemat memori:
 * </p>
 * <ol>
 *   <li><b>{@link #kelompokkan(List)}</b> — menelusuri daftar {@link KrsMahasiswa} satu
 *       kali, mengelompokkannya ke dalam {@link LinkedHashMap} dengan kunci
 *       <i>id Dosen PA</i> (atau {@code -1} untuk mahasiswa yang belum memiliki Dosen
 *       PA). Karena satu mahasiswa bisa muncul beberapa kali (beberapa baris KRS lintas
 *       semester), tiap grup memakai {@code Map&lt;idMahasiswa, Mahasiswa&gt;} untuk
 *       <b>menghilangkan duplikat</b> secara otomatis sehingga setiap mahasiswa hanya
 *       terhitung sekali per dosen. Struktur ringan ini hanya menyimpan referensi
 *       entity yang memang sudah ada di memori, bukan salinan baru.</li>
 *   <li><b>{@link #urutkan(Map)}</b> — mengurutkan grup berdasarkan nama dosen (abjad,
 *       tidak peduli huruf besar/kecil), dan selalu menempatkan grup "Tanpa Dosen PA"
 *       di posisi paling bawah agar mudah ditindaklanjuti oleh petugas.</li>
 *   <li><b>{@link #bangunHtml(List)}</b> — menyusun tampilan memakai HTML/CSS modern
 *       (bukan komponen berat, bukan JFreeChart): dua kartu ringkasan (jumlah dosen dan
 *       jumlah mahasiswa), lalu satu kartu per dosen berisi tabel mahasiswa (No, NIM,
 *       Nama, Kelas, Angkatan) dengan baris selang-seling agar nyaman dibaca. Seluruh
 *       warna memakai variabel tema ({@code --ais-theme-primary}, {@code --ais-theme-accent},
 *       dst.) sehingga otomatis menyesuaikan tema yang dipilih tenant.</li>
 * </ol>
 *
 * <h3>Antarmuka publik &amp; reuse</h3>
 * <p>
 * Satu-satunya pintu masuk adalah {@link #buka(List)}. Method statis ini dirancang agar
 * dapat dipakai ulang oleh halaman lain yang juga memiliki daftar {@link KrsMahasiswa}
 * (mis. laporan konsultasi, dasbor Dosen PA, atau rekap prodi) cukup dengan menyediakan
 * daftar KRS-nya — tidak perlu menyalin logika pengelompokan maupun tampilan. Seluruh
 * pemrosesan dibungkus {@code try/catch} bergaya Java 1.6 (tanpa multi-catch, tanpa
 * lambda, tanpa diamond operator, tanpa try-with-resources) agar tetap kompatibel dengan
 * kompilasi Java 1.7 yang dipakai proyek ini. Kesalahan tak terduga ditampilkan lewat
 * {@code Common.tampilErrorJikaAdmin(e)} sehingga pengguna non-admin tidak terganggu.
 * </p>
 *
 * <h3>Aksesibilitas &amp; responsivitas</h3>
 * <p>
 * Jendela dibuat dapat diperbesar ({@code maximizable}) dan diubah ukurannya
 * ({@code sizable}); lebar default 880px namun tinggi memakai persentase agar muat di
 * layar kecil, dengan area konten yang bisa digulir. Kartu ringkasan memakai
 * {@code flex-wrap} sehingga menumpuk otomatis di layar sempit (ponsel), sementara tabel
 * memakai lebar penuh. Dengan demikian papan ini nyaman dilihat baik di desktop maupun
 * perangkat mobile.
 * </p>
 *
 * @author AIS
 */
public final class DaftarBimbinganPerDosenHelper {

	/** Kelas utilitas — tidak untuk diinstansiasi. */
	private DaftarBimbinganPerDosenHelper() {
	}

	/**
	 * Penampung ringan satu grup dosen: identitas dosen + kumpulan mahasiswa uniknya.
	 * Memakai {@link LinkedHashMap} agar urutan kemunculan pertama mahasiswa terjaga.
	 */
	private static final class Grup {
		private String namaDosen;
		private String nidn;
		private final Map<Long, Mahasiswa> mahasiswa = new LinkedHashMap<Long, Mahasiswa>();
	}

	/**
	 * Membuka jendela modal berisi daftar mahasiswa bimbingan yang dikelompokkan per
	 * Dosen Pembimbing Akademik. Aman dipanggil dari dalam listener {@code onClick}
	 * sebuah tombol toolbar.
	 *
	 * @param daftar daftar {@link KrsMahasiswa} hasil pencarian aktif (boleh {@code null}
	 *               atau kosong; keduanya ditangani dengan pesan yang ramah pengguna).
	 */
	public static void buka(List<KrsMahasiswa> daftar) {
		try {
			Map<Long, Grup> peta = kelompokkan(daftar);
			List<Grup> grup = urutkan(peta);

			MyWindow window = new MyWindow("Daftar Mahasiswa Bimbingan per Dosen PA", "normal", true);
			window.setWidth("880px");
			window.setHeight("88%");
			window.setMaximizable(true);
			window.setSizable(true);
			window.setContentStyle("overflow:auto;background:#eef2f7;");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Vbox wadah = new Vbox();
			wadah.setWidth("100%");
			wadah.setStyle("padding:4px;");
			wadah.setParent(window);

			final List<Map> mapsCetak = bangunMapsCetak(daftar);
			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak PDF", "/img/print.png");
			cetak.setTooltiptext("Cetak daftar bimbingan akademik per Dosen PA sesuai filter yang sedang tampil");
			cetak.setDisabled(mapsCetak.isEmpty());
			cetak.setParent(wadah);
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tampilkanCetak(mapsCetak);
				}
			});

			MyHtml html = new MyHtml(bangunHtml(grup));
			html.setParent(wadah);

			try {
				window.onModal();
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/DaftarBimbinganPerDosenHelper.java:146");
				// InterruptedException wajar terjadi saat modal ditutup — aman diabaikan.
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<Map> bangunMapsCetak(List<KrsMahasiswa> daftar) {
		Map<Long, KrsMahasiswa> unik = new LinkedHashMap<Long, KrsMahasiswa>();
		if (daftar != null) {
			for (KrsMahasiswa krs : daftar) {
				if (krs == null || krs.getMahasiswa() == null) {
					continue;
				}
				Mahasiswa mahasiswa = krs.getMahasiswa();
				Long kunci = mahasiswa.getId() == null ? Long.valueOf((long) System.identityHashCode(mahasiswa))
						: Long.valueOf(mahasiswa.getId().longValue());
				if (!unik.containsKey(kunci)) {
					unik.put(kunci, krs);
				}
			}
		}

		List<Map> maps = new ArrayList<Map>();
		for (KrsMahasiswa krs : unik.values()) {
			Mahasiswa mahasiswa = krs.getMahasiswa();
			Dosen dosen = null;
			Jurusan jurusan = null;
			Fakultas fakultas = null;
			try {
				dosen = krs.getDosenPa();
			} catch (Exception e) {
				dosen = null;
			}
			try {
				jurusan = mahasiswa.getJurusan();
			} catch (Exception e) {
				jurusan = null;
			}
			try {
				fakultas = jurusan == null ? null : jurusan.getFakultas();
			} catch (Exception e) {
				fakultas = null;
			}

			Map map = new HashMap();
			map.put("dosen_id", dosen == null || dosen.getId() == null ? Long.valueOf(-1L) : dosen.getId());
			map.put("nim", mahasiswa.getNim() == null ? "" : mahasiswa.getNim());
			map.put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama());
			map.put("dosen", dosen == null || dosen.getNama() == null ? "" : dosen.getNama());
			map.put("jur", jurusan == null || jurusan.getNama() == null ? "" : jurusan.getNama());
			map.put("fak", fakultas == null || fakultas.getNama() == null ? "" : fakultas.getNama());
			map.put("tahunangkatan", mahasiswa.getTahunangkatan());
			map.put("status_mahasiswa",
					mahasiswa.getKelompokStatusMahasiswa() == null ? "" : mahasiswa.getKelompokStatusMahasiswa().getNama());
			map.put("kelamin", mahasiswa.getKelamin() == null ? "" : mahasiswa.getKelamin());
			map.put("code", dosen == null || dosen.getCode() == null ? "" : dosen.getCode());
			map.put("mycode", dosen == null || dosen.getMycode() == null ? "" : dosen.getMycode());
			map.put("nidn", dosen == null || dosen.getNidn() == null ? "" : dosen.getNidn());
			map.put("fakultas", fakultas == null || fakultas.getId() == null ? Long.valueOf(-1L) : fakultas.getId());
			map.put("kaprodi_nama", jurusan == null || jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNama());
			map.put("nip_kaprodi", jurusan == null || jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getCode());
			map.put("nidn_kaprodi", jurusan == null || jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNidn());
			map.put("dekan_nama", fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getNama());
			map.put("nip_dekan", fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getCode());
			map.put("nidn_dekan", fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getNidn());
			maps.add(map);
		}
		return maps;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void tampilkanCetak(List<Map> maps) throws Exception {
		if (maps == null || maps.isEmpty()) {
			Messagebox.show("Belum ada data mahasiswa bimbingan untuk dicetak.");
			return;
		}

		MyWindow window = new MyWindow("Cetak Daftar Bimbingan Akademik PA", "normal", true);
		window.setWidth("95%");
		window.setHeight("92%");
		window.setMaximizable(true);
		window.setSizable(true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("maps", maps);
		File file = Report.generateFileReport(Report.PDF, parameters, "Rekap_dosen_pa",
				ais.ui.util.WaktuUtil.getDate(), (Toolbar) null);
		CommonReport.tampilkanReportPDF(center, file);
		window.onModal();
	}

	/**
	 * Mengelompokkan daftar KRS per Dosen PA sekaligus menghilangkan duplikat mahasiswa.
	 *
	 * @param daftar daftar KRS (boleh {@code null}).
	 * @return peta {@code idDosen -> Grup} (kunci {@code -1L} untuk tanpa Dosen PA).
	 */
	private static Map<Long, Grup> kelompokkan(List<KrsMahasiswa> daftar) {
		Map<Long, Grup> peta = new LinkedHashMap<Long, Grup>();
		if (daftar == null) {
			return peta;
		}
		for (KrsMahasiswa krs : daftar) {
			if (krs == null) {
				continue;
			}
			Mahasiswa m = krs.getMahasiswa();
			if (m == null) {
				continue;
			}
			Dosen dosen = null;
			try {
				dosen = krs.getDosenPa();
			} catch (Exception abaikan) {
				dosen = null;
			}
			Long kunci = (dosen == null || dosen.getId() == null) ? Long.valueOf(-1L)
					: Long.valueOf(dosen.getId().longValue());
			Grup g = peta.get(kunci);
			if (g == null) {
				g = new Grup();
				g.namaDosen = (dosen == null) ? null : dosen.getNama();
				g.nidn = (dosen == null) ? null : dosen.getNidn();
				peta.put(kunci, g);
			}
			Long idMhs = (m.getId() == null) ? Long.valueOf((long) System.identityHashCode(m))
					: Long.valueOf(m.getId().longValue());
			if (!g.mahasiswa.containsKey(idMhs)) {
				g.mahasiswa.put(idMhs, m);
			}
		}
		return peta;
	}

	/**
	 * Mengurutkan grup: berdasarkan nama dosen (abjad, abai huruf besar/kecil); grup
	 * "Tanpa Dosen PA" selalu di paling bawah.
	 *
	 * @param peta hasil {@link #kelompokkan(List)}.
	 * @return daftar grup terurut.
	 */
	private static List<Grup> urutkan(Map<Long, Grup> peta) {
		List<Grup> grup = new ArrayList<Grup>(peta.values());
		Collections.sort(grup, new Comparator<Grup>() {
			@Override
			public int compare(Grup a, Grup b) {
				boolean aKosong = (a.namaDosen == null || a.namaDosen.trim().isEmpty());
				boolean bKosong = (b.namaDosen == null || b.namaDosen.trim().isEmpty());
				if (aKosong && bKosong) {
					return 0;
				}
				if (aKosong) {
					return 1;
				}
				if (bKosong) {
					return -1;
				}
				return a.namaDosen.compareToIgnoreCase(b.namaDosen);
			}
		});
		return grup;
	}

	/**
	 * Menyusun HTML papan (ringkasan + kartu per dosen + tabel mahasiswa). Seluruh warna
	 * memakai variabel tema agar mengikuti tema aktif.
	 *
	 * @param grup daftar grup terurut.
	 * @return string HTML siap ditaruh ke {@link MyHtml}.
	 */
	private static String bangunHtml(List<Grup> grup) {
		int totalDosen = 0;
		int totalMhs = 0;
		for (int i = 0; i < grup.size(); i++) {
			Grup g = grup.get(i);
			if (g.namaDosen != null && !g.namaDosen.trim().isEmpty()) {
				totalDosen++;
			}
			totalMhs += g.mahasiswa.size();
		}

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style='font-family:Poppins,Arial,Helvetica,sans-serif;color:#1f2937;'>");

		// Keterangan singkat (mudah dipahami awam)
		sb.append("<div style='font-size:12.5px;color:#475569;margin:2px 2px 12px;'>")
				.append("Daftar mahasiswa yang menjadi bimbingan tiap dosen Pembimbing Akademik, ")
				.append("dikelompokkan agar mudah dilihat per dosen.")
				.append("</div>");

		// Dua kartu ringkasan (flex-wrap → menumpuk di layar sempit)
		sb.append("<div style='display:flex;gap:10px;flex-wrap:wrap;margin:0 2px 14px;'>");
		sb.append(kartuRingkas("Jumlah Dosen PA", String.valueOf(totalDosen),
				"linear-gradient(135deg,var(--ais-theme-primary,#007131),var(--ais-theme-primary-dark,#005c28))",
				"rgba(var(--ais-theme-primary-rgb,0,113,49),.25)"));
		sb.append(kartuRingkas("Jumlah Mahasiswa Bimbingan", String.valueOf(totalMhs),
				"linear-gradient(135deg,var(--ais-theme-accent,#e27d21),var(--ais-theme-accent-dark,#b95f12))",
				"rgba(var(--ais-theme-accent-rgb,226,125,33),.25)"));
		sb.append("</div>");

		if (grup.isEmpty()) {
			sb.append("<div style='background:#fff;border:1px dashed #cbd5e1;border-radius:12px;")
					.append("padding:26px;text-align:center;color:#64748b;font-size:13px;'>")
					.append("Belum ada data mahasiswa bimbingan untuk filter yang dipilih.")
					.append("</div>");
			sb.append("</div>");
			return sb.toString();
		}

		for (int i = 0; i < grup.size(); i++) {
			Grup g = grup.get(i);
			boolean tanpaDosen = (g.namaDosen == null || g.namaDosen.trim().isEmpty());
			String namaDosen = tanpaDosen ? "Tanpa Dosen PA" : g.namaDosen.trim();
			String nidn = (g.nidn == null || g.nidn.trim().isEmpty()) ? "" : ("NIDN: " + g.nidn.trim());

			sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:12px;")
					.append("box-shadow:0 2px 8px rgba(15,23,42,.06);margin:0 2px 14px;overflow:hidden;'>");

			// Header kartu (nama dosen + jumlah)
			sb.append("<div style='display:flex;align-items:center;justify-content:space-between;")
					.append("gap:10px;padding:10px 14px;")
					.append("background:linear-gradient(90deg,rgba(var(--ais-theme-primary-rgb,0,113,49),.12),transparent);")
					.append("border-left:4px solid var(--ais-theme-primary,#007131);'>");
			sb.append("<div>");
			sb.append("<div style='font-weight:700;font-size:14px;color:var(--ais-theme-primary,#007131);'>")
					.append(esc(namaDosen)).append("</div>");
			if (nidn.length() > 0) {
				sb.append("<div style='font-size:11px;color:#64748b;margin-top:1px;'>")
						.append(esc(nidn)).append("</div>");
			}
			sb.append("</div>");
			sb.append("<div style='background:var(--ais-theme-primary,#007131);color:#fff;border-radius:999px;")
					.append("padding:3px 12px;font-size:12px;font-weight:700;white-space:nowrap;'>")
					.append(g.mahasiswa.size()).append(" mahasiswa</div>");
			sb.append("</div>");

			// Tabel mahasiswa
			sb.append("<div style='overflow-x:auto;'>");
			sb.append("<table style='width:100%;border-collapse:collapse;font-size:12.5px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;color:#334155;text-align:left;'>")
					.append("<th style='padding:7px 10px;width:44px;'>No</th>")
					.append("<th style='padding:7px 10px;width:140px;'>NIM</th>")
					.append("<th style='padding:7px 10px;'>Nama Mahasiswa</th>")
					.append("<th style='padding:7px 10px;width:70px;'>Kelas</th>")
					.append("<th style='padding:7px 10px;width:90px;'>Angkatan</th>")
					.append("</tr></thead><tbody>");

			int no = 0;
			for (Mahasiswa m : g.mahasiswa.values()) {
				no++;
				String warnaBaris = (no % 2 == 0) ? "#ffffff" : "#f8fafc";
				String nim = (m.getNim() == null) ? "" : m.getNim();
				String nama = (m.getNama() == null) ? "" : m.getNama();
				String kelas = (m.getKelas() == null) ? "" : m.getKelas();
				String angkatan = (m.getTahunangkatan() == null) ? "" : String.valueOf(m.getTahunangkatan());

				sb.append("<tr style='border-top:1px solid #eef2f7;background:").append(warnaBaris).append(";'>")
						.append("<td style='padding:6px 10px;color:#64748b;'>").append(no).append("</td>")
						.append("<td style='padding:6px 10px;font-weight:600;'>").append(esc(nim)).append("</td>")
						.append("<td style='padding:6px 10px;'>").append(esc(nama)).append("</td>")
						.append("<td style='padding:6px 10px;'>").append(esc(kelas)).append("</td>")
						.append("<td style='padding:6px 10px;'>").append(esc(angkatan)).append("</td>")
						.append("</tr>");
			}

			sb.append("</tbody></table></div>");
			sb.append("</div>");
		}

		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Membuat satu kartu ringkasan (label kecil + angka besar) berlatar gradasi tema.
	 *
	 * @param label      teks keterangan.
	 * @param nilai      angka yang ditonjolkan.
	 * @param gradasi    nilai CSS {@code background} (gradasi tema).
	 * @param bayangRgba warna bayangan dalam bentuk {@code rgba(...)}.
	 * @return potongan HTML kartu.
	 */
	private static String kartuRingkas(String label, String nilai, String gradasi, String bayangRgba) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("<div style='flex:1 1 180px;min-width:160px;color:#fff;border-radius:12px;")
				.append("padding:12px 16px;background:").append(gradasi).append(";")
				.append("box-shadow:0 6px 16px ").append(bayangRgba).append(";'>");
		sb.append("<div style='font-size:12px;opacity:.92;'>").append(esc(label)).append("</div>");
		sb.append("<div style='font-size:26px;font-weight:800;line-height:1.1;margin-top:2px;'>")
				.append(esc(nilai)).append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Meng-escape teks agar aman disisipkan ke HTML (reuse {@link MyThemeProvider#escapeHtml(String)}).
	 *
	 * @param teks teks mentah.
	 * @return teks aman-HTML.
	 */
	private static String esc(String teks) {
		return MyThemeProvider.escapeHtml(teks);
	}
}
