package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederExporter;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Window "Kirim ke Feeder" generik untuk entitas yang BELUM memiliki window kirim tersendiri di
 * paket integrator (Mata Kuliah, Kurikulum, Mahasiswa, serta aktivitas KKN/PKL/Skripsi/Tugas
 * Akhir/Kegiatan/Penghargaan/Formulir). Menyamakan aksi "Kirim ke Feeder" pada dashboard Neo Feeder
 * ({@code DasbordSinkronisasiNeoFeeder}) dengan tombol "Kirim ke Feeder" di halaman mandiri.
 *
 * <h3>Contoh pemakaian {@link NeoFeederWindowBase}</h3>
 * Window ini adalah contoh nyata adopsi base class Neo Feeder: alih-alih menduplikasi pola
 * "koneksi + token + thread latar + indikator progres", ia memakai helper base:
 * <ul>
 *   <li>{@link #runFeederTask(String, FeederTask)} — menjalankan proses kirim di thread latar sambil
 *       menampilkan <b>popup progress melayang</b> (persentase + bar), dan otomatis menangani error
 *       serta memanggil {@link #onSelesaiTask(Event)} saat selesai.</li>
 *   <li>{@link #konek()} — membuka koneksi + memeriksa keterjangkauan server + mengambil token dalam
 *       satu langkah; melempar pesan jelas bila gagal (ditangkap {@code runFeederTask} &rarr; popup
 *       error), sehingga tidak lagi ada bug "menampilkan 'selesai' padahal login gagal".</li>
 * </ul>
 *
 * <h3>Logika kirim tidak diduplikasi</h3>
 * Proses kirim tetap memakai ulang {@link FeederExporter} — method "kirim semua" yang sama persis
 * dipakai halaman mandiri ({@code matakuliah(errorLog)}, {@code kurikulum(errorLog)},
 * {@code mahasiswa()}, dan {@code aktivitas*} untuk data kemahasiswaan).
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZK 5.5.
 *
 * @author Tim AIS
 */
public class KirimKeFeederWindow extends NeoFeederWindowBase {

	private static final long serialVersionUID = 1L;

	/** Jenis entitas: {@code matakuliah} | {@code kurikulum} | {@code mahasiswa} | aktivitas*. */
	private final String jenis;

	private Center center = new Center();

	/** Kumpulan pesan error selama proses kirim; dibaca {@link #onSelesaiTask(Event)}. */
	private final List<String> errorLog = new ArrayList<String>();

	/**
	 * @param jenis jenis entitas yang dikirim (mis. {@code "matakuliah"})
	 */
	public KirimKeFeederWindow(String jenis) {
		super();
		this.jenis = (jenis == null ? "" : jenis.trim().toLowerCase());
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @param jenis    jenis entitas
	 * @param title    judul window
	 * @param border   gaya border
	 * @param closable dapat ditutup atau tidak
	 */
	public KirimKeFeederWindow(String jenis, String title, String border, boolean closable) {
		super(title, border, closable);
		this.jenis = (jenis == null ? "" : jenis.trim().toLowerCase());
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Label tampil untuk {@link #jenis}. */
	private String judulJenis() {
		if ("matakuliah".equals(jenis)) {
			return "Mata Kuliah";
		}
		if ("kurikulum".equals(jenis)) {
			return "Kurikulum";
		}
		if ("mahasiswa".equals(jenis)) {
			return "Mahasiswa";
		}
		if ("kkn".equals(jenis)) {
			return "Aktivitas KKN";
		}
		if ("pkl".equals(jenis)) {
			return "Aktivitas PKL";
		}
		if ("skripsi".equals(jenis)) {
			return "Aktivitas Skripsi";
		}
		if ("tugasakhir".equals(jenis)) {
			return "Aktivitas Tugas Akhir";
		}
		if ("kegiatan".equals(jenis)) {
			return "Kegiatan Kemahasiswaan";
		}
		if ("penghargaan".equals(jenis)) {
			return "Penghargaan Mahasiswa";
		}
		if ("formulir".equals(jenis)) {
			return "Formulir Kegiatan (Aktivitas)";
		}
		return jenis;
	}

	/**
	 * Ambil daftar entitas lokal yang akan dikirim (meniru pola query "kirim semua" di
	 * {@link FeederExporter} / halaman mandiri). Session ditutup di blok {@code finally} agar tidak
	 * bocor walau query melempar exception.
	 *
	 * @param jenis jenis entitas
	 * @return daftar entitas ({@link ArrayList} kosong untuk jenis yang tak punya daftar sendiri)
	 */
	@SuppressWarnings("unchecked")
	private static List<?> ambilDaftar(String jenis) {
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> list;
			if ("kkn".equals(jenis)) {
				list = session.createCriteria(KelompokKkn.class).list();
			} else if ("pkl".equals(jenis)) {
				list = session.createCriteria(KelompokPkl.class).list();
			} else if ("skripsi".equals(jenis)) {
				// Hanya yang sudah disetujui sidang (selaras SkripsiAction).
				list = session.createCriteria(Skripsi.class).add(Restrictions.eq("setujuiSidang", true)).list();
			} else if ("tugasakhir".equals(jenis)) {
				// Status SEMINAR/AKTIF/LULUS (selaras MahasiswaRequestTugasAkhirAction).
				list = session.createCriteria(MahasiswaRequestTugasAkhir.class)
						.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
								Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS),
										Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS))))
						.list();
			} else if ("kegiatan".equals(jenis)) {
				// Hanya kegiatan yang sudah DISETUJUI yang dikirim ke feeder.
				list = session.createCriteria(KegiatanKemahasiswaan.class)
						.add(Restrictions.eq("status", KegiatanKemahasiswaan.DISETUJUI)).list();
			} else if ("penghargaan".equals(jenis)) {
				// Hanya penghargaan yang sudah DISETUJUI yang dikirim ke feeder.
				list = session.createCriteria(PenghargaanMahasiswa.class)
						.add(Restrictions.eq("status", PenghargaanMahasiswa.DISETUJUI)).list();
			} else if ("formulir".equals(jenis)) {
				list = session.createCriteria(FormulirKegiatan.class).list();
			} else {
				list = new ArrayList<Object>();
			}
			return list;
		} finally {
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	/** Bangun tampilan window: toolbar tombol kirim + keterangan. */
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		MyToolbarbuttonConfig btnKirim = new MyToolbarbuttonConfig("Mulai Kirim " + judulJenis() + " ke Feeder",
				"/img/save.gif");
		btnKirim.setParent(toolbar);
		btnKirim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				konfirmasiKirim();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Vbox info = new Vbox();
		info.setStyle("padding:12px; color:#444;");
		info.setParent(center);
		Label keterangan = new Label("Klik tombol di atas untuk mengirim SELURUH data " + judulJenis()
				+ " ke Neo Feeder PDDikti. Proses berjalan di latar dengan indikator progres; daftar error"
				+ " (bila ada) otomatis diunduh.");
		keterangan.setParent(info);
	}

	/** Konfirmasi sebelum mengirim seluruh data. */
	private void konfirmasiKirim() throws Exception {
		MyMessageboxConfig.show("Apakah yakin ingin mengirim SEMUA data " + judulJenis() + " ke feeder ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (Integer.parseInt(event.getData().toString()) != MyMessageboxConfig.OK) {
							return;
						}
						jalankanKirim();
					}
				});
	}

	/**
	 * Menjalankan proses kirim memakai {@link #runFeederTask(String, FeederTask)} (thread latar +
	 * popup progres melayang) dan {@link #konek()} (koneksi + token). Logika kirim memakai ulang
	 * {@link FeederExporter}.
	 */
	private void jalankanKirim() throws Exception {
		errorLog.clear();
		runFeederTask("Mengirim " + judulJenis() + " ke Neo Feeder", new FeederTask() {
			@Override
			public void run(Label progres) throws Exception {
				FeederAkses akses = konek();
				// Memakai ulang FeederExporter — logika kirim yang SAMA dengan halaman mandiri.
				FeederExporter feederExporter = new FeederExporter(akses.connector, akses.token, null, null, progres);

				if ("matakuliah".equals(jenis)) {
					feederExporter.matakuliah(errorLog);
				} else if ("kurikulum".equals(jenis)) {
					feederExporter.kurikulum(errorLog);
				} else if ("mahasiswa".equals(jenis)) {
					feederExporter.mahasiswa();
				} else if ("kkn".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						feederExporter.aktivitasMahasiswaKkn((KelompokKkn) o, errorLog);
					}
				} else if ("pkl".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						feederExporter.aktivitasMahasiswaPkl((KelompokPkl) o, errorLog);
					}
				} else if ("skripsi".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						Skripsi sk = (Skripsi) o;
						if (sk.getMahasiswa() != null && sk.getMahasiswa().getIdRegPd() != null) {
							feederExporter.aktivitasMahasiswa(sk, errorLog);
						} else {
							errorLog.add("Skripsi: mahasiswa " + sk.getMahasiswa()
									+ " belum terdaftar di feeder (idRegPd kosong).");
						}
					}
				} else if ("tugasakhir".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						MahasiswaRequestTugasAkhir ta = (MahasiswaRequestTugasAkhir) o;
						if (ta.getMahasiswa() != null && ta.getMahasiswa().getIdRegPd() != null) {
							feederExporter.aktivitasMahasiswaBimbingan(ta, errorLog);
						} else {
							errorLog.add("Tugas Akhir: mahasiswa " + ta.getMahasiswa()
									+ " belum terdaftar di feeder (idRegPd kosong).");
						}
					}
				} else if ("kegiatan".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						feederExporter.aktivitasKegiatanMahasiswa((KegiatanKemahasiswaan) o, errorLog);
					}
				} else if ("penghargaan".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						feederExporter.aktivitasMahasiswaPengahargaan((PenghargaanMahasiswa) o, errorLog);
					}
				} else if ("formulir".equals(jenis)) {
					for (Object o : ambilDaftar(jenis)) {
						feederExporter.aktivitasMahasiswaForm((FormulirKegiatan) o, errorLog);
					}
				} else {
					errorLog.add("Jenis kirim tidak dikenal: " + jenis);
				}
			}
		});
	}

	/**
	 * Dipanggil saat proses kirim selesai/gagal (di UI thread). Menampilkan hasil: bila proses gagal
	 * (mis. login/koneksi), tampilkan pesan error; bila ada catatan error, unduh berkasnya; selain
	 * itu tampilkan pesan selesai.
	 *
	 * @param e {@code null} saat sukses; {@code Event} berisi teks error saat gagal
	 */
	@Override
	protected void onSelesaiTask(Event e) throws Exception {
		if (e != null && e.getName() != null && !e.getName().trim().isEmpty()) {
			MyMessageboxConfig.show(e.getName(), "Gagal", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (!errorLog.isEmpty()) {
			String err = "";
			for (String s : errorLog) {
				err += err.isEmpty() ? s
						: "\n----------------------------------------------------------------------------------------------------------\n"
								+ s;
			}
			MyMessageboxConfig.show("Selesai dengan " + errorLog.size()
					+ " catatan error; berkas error akan otomatis ter-download.", "Selesai", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			File file = new File("/opt/ecampus/error_kirim_" + jenis + "_" + Common.randLong() + ".txt");
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			FileUtils.writeStringToFile(file, err);
			Filedownload.save(file, "text/plain");
		} else {
			MyMessageboxConfig.show("Kirim " + judulJenis() + " ke Feeder selesai.", "Selesai", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}
	}
}
