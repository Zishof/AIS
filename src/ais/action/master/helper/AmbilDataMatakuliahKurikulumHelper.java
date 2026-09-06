package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.KurikulumPunyaMatakuliahDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper terfokus untuk ambil data matakuliah kurikulum. Tipe ini membungkus satu variasi kecil
 * dari alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Kurikulum kurikulum}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code Textbox kodeMk}, {@code Textbox namaMk}, {@code Combobox jurusan}, {@code
 * Combobox fakultas}, {@code Combobox jenjang}; inisialisasi/lifecycle ({@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code save()}); penghapusan/pembatalan ({@code
 * hapusJikaTidakDipakaiPerkuliahan()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code
 * jenjang}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class AmbilDataMatakuliahKurikulumHelper {

	/** Kurikulum konteks: mata kuliah yang dipilih/dihapus di popup ini akan diikat ke kurikulum ini. */
	private Kurikulum kurikulum;
	/** Grid hasil pencarian mata kuliah, dirender ulang oleh {@link #onSearchDefault(Event)}. */
	private MyGrid grid;
	/** Komponen paging grid; perpindahan halaman memicu pencarian ulang lewat {@link #onSearchDefault(Event)}. */
	private Paging paging;

	/** Filter pencarian: kode mata kuliah (cocok sebagian, {@link MatchMode#ANYWHERE}). */
	private Textbox kodeMk;
	/** Filter pencarian: nama mata kuliah (cocok sebagian, {@link MatchMode#ANYWHERE}). */
	private Textbox namaMk;
	/** Filter pencarian: program studi/jurusan mata kuliah. */
	private Combobox jurusan = new Combobox();
	/** Filter pencarian: fakultas; memilih fakultas menyaring pilihan {@link #jurusan}. */
	private Combobox fakultas = new Combobox();
	/** Filter pencarian: jenjang program studi mata kuliah. */
	private Combobox jenjang = new Combobox();
	/** Filter pencarian: hanya mata kuliah milik universitas (menonaktifkan {@link #fakultas}/{@link #jurusan} saat dicentang). */
	private MyCheckboxConfig milikUniversitas = new MyCheckboxConfig("Milik Universitas");
	/** Filter pencarian: hanya mata kuliah ekstrakurikuler. */
	private MyCheckboxConfig extraKulikuler = new MyCheckboxConfig();
	/**
	 * Penampung sementara baris {@link KurikulumPunyaMatakuliah} yang dicentang untuk dihapus dari
	 * kurikulum, dikumpulkan oleh {@link MatakuliahRenderer} dan dieksekusi saat {@link #save()}.
	 */
	private Map<Long, KurikulumPunyaMatakuliah> deletedMatakuliahs = new HashMap<Long, KurikulumPunyaMatakuliah>();
	/** Semester kurikulum yang sedang diedit; dipakai sebagai bagian kunci pencarian/penyimpanan {@link KurikulumPunyaMatakuliah}. */
	private Integer semester;
	/**
	 * Bila tidak {@code null}, popup ini sedang mengedit sub-mata kuliah (modul) dari induk ini;
	 * pencarian dibatasi hanya pada mata kuliah ber-{@code merupakanModul=true} (lihat
	 * {@link #initCriteria(boolean)}) dan baris baru yang disimpan diikat ke induk ini.
	 */
	private KurikulumPunyaMatakuliah indukMatakuliah;

	/**
	 * Menyiapkan combobox filter fakultas/jurusan/jenjang (jenjang dibatasi ke yang aktif atau
	 * belum diisi status aktifnya) dan menghubungkan {@link #paging} ke {@link #onSearchDefault(Event)}
	 * sehingga perpindahan halaman otomatis memuat ulang data.
	 */
	public AmbilDataMatakuliahKurikulumHelper() {
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataMatakuliahKurikulumHelper}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataMatakuliahKurikulumHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataMatakuliahKurikulumHelper
	 */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk sebuah {@link Matakuliah}: label identitas/SKS/status/flag
		 * ekstra-uts-uas, badge kelompok mata kuliah (ambil kelompok terbaru berdasarkan id, bukan
		 * riwayat penuh), badge jurusan, daftar kurikulum lain yang memakai mata kuliah ini
		 * (menampilkan nama kurikulum dan semester), serta checkbox status "sudah termasuk kurikulum
		 * ini pada semester ini" yang diisi belakangan lewat {@link Common#createDefaultTimer} agar
		 * tidak memblokir render baris lain.
		 *
		 * <p>Pengambilan daftar kurikulum lain (variabel {@code kurikulumPunyaMatakuliahs}) sengaja
		 * dibungkus try/catch: query {@code .list()} di sini dapat memicu auto-flush Hibernate atas
		 * perubahan tertunda (mis. dari {@link AmbilDataMatakuliahKurikulumHelper#save()} sebelumnya)
		 * yang dapat bentrok dengan baris {@code kurikulum_punya_matakuliah} yang sedang dikunci
		 * pengguna lain (lock timeout). Kegagalan pada satu baris tidak boleh menggagalkan seluruh
		 * grid; badge kurikulum baris tersebut cukup dilewati.</p>
		 *
		 * @param arg0 baris ZK tujuan render.
		 * @param arg1 data baris, harus berupa {@link Matakuliah}.
		 * @throws Exception diteruskan apa adanya dari operasi ZK/Hibernate (di luar blok try/catch
		 *         lokal untuk daftar kurikulum lain).
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("matakuliah", matakuliah);
			checkbox.setVisible(false);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) HibernateUtil
							.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
							.add(Restrictions.eq("matakuliah", matakuliah)).add(Restrictions.eq("kurikulum", kurikulum))
							.add(Restrictions.eq("semester", semester)).uniqueResult();
					if (kurikulumPunyaMatakuliah != null) {
						if (!checkbox.isChecked()) {
							deletedMatakuliahs.remove(kurikulumPunyaMatakuliah.getId());
						} else {
							deletedMatakuliahs.put(kurikulumPunyaMatakuliah.getId(), kurikulumPunyaMatakuliah);
						}
					}

				}
			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getMerupakanModul() ? Common.numberFormat.get().format(matakuliah.getSksSubMk())
					: Common.numberFormat.get().format(matakuliah.getSks())).setParent(arg0);
			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(arg0);

			// new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);

			new Label((matakuliah.getExtraKulikuler() == null ? "" : matakuliah.getExtraKulikuler() ? "Ya" : "Tidak")
					+ "/" + (matakuliah.getTerdapatUts() ? "Ya" : "Tidak") + "/"
					+ (matakuliah.getTerdapatUas() ? "Ya" : "Tidak")).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) session
					.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
					.add(Restrictions.eq("matakuliah", matakuliah)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			new Label(kelompokMatakuliahPunyaMatakuliah == null
					|| kelompokMatakuliahPunyaMatakuliah.getKelompokMatakuliah() == null ? ""
							: kelompokMatakuliahPunyaMatakuliah.getKelompokMatakuliah().getNama())
					.setParent(arg0);

			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs;
			try {
				// KE-FIX (GenericJDBCException "canceling statement due to lock timeout"):
				// .list() di sini bisa memicu auto-flush dari perubahan tertunda di session
				// (mis. dari save() sebelumnya) yang bentrok dgn baris kurikulum_punya_matakuliah
				// sedang dikunci pengguna lain -- render satu baris grid tidak boleh menggagalkan
				// seluruh grid, cukup lewati badge kurikulum baris ini & lanjut render baris lain.
				@SuppressWarnings("unchecked")
				List<KurikulumPunyaMatakuliah> hasil = session.createCriteria(KurikulumPunyaMatakuliah.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.isNotNull("kurikulum")).add(Restrictions.eq("matakuliah", matakuliah)).list();
				kurikulumPunyaMatakuliahs = hasil;
			} catch (Exception eLock) {
				ais.common.ErrorAuditUtil.record(eLock,
						"auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:MatakuliahRenderer-lock");
				kurikulumPunyaMatakuliahs = java.util.Collections.emptyList();
			}
			int i = 1;
			for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
				new MyLabelKecil(i + "." + kurikulumPunyaMatakuliah.getKurikulum().getNama() + ", smt : "
						+ kurikulumPunyaMatakuliah.getSemester()).setParent(vbox);
				i++;
			}
			vbox.setParent(arg0);
			kurikulumPunyaMatakuliahs = null;

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					Integer jml = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("matakuliah", matakuliah))
							.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.eq("semester", semester))
							.uniqueResult()).intValue();
					checkbox.setChecked(!jml.equals(0));
					checkbox.setVisible(true);
				}
			});

		}

	}

	/**
	 * Menyimpan hasil centang/hapus pada grid ke dalam {@link KurikulumPunyaMatakuliah}: untuk tiap
	 * baris grid yang checkbox-nya dicentang, membuat (atau memperbarui bila sudah ada) baris
	 * {@link KurikulumPunyaMatakuliah} untuk kombinasi kurikulum/mata kuliah/semester ini; untuk
	 * baris yang tidak dicentang tetapi sebelumnya sudah termasuk kurikulum, baris terkait dihapus
	 * lewat {@link #hapusJikaTidakDipakaiPerkuliahan(Session, KurikulumPunyaMatakuliah)}. Baris yang
	 * dicentang untuk dihapus lewat interaksi checkbox tersembunyi di {@link MatakuliahRenderer}
	 * (dikumpulkan di {@link #deletedMatakuliahs}) juga diproses di akhir agar konsisten dengan
	 * baris yang tidak lagi tampil di grid saat ini (mis. setelah filter berubah).
	 *
	 * <p>Kegagalan per baris ditangkap dan dicatat lewat {@link ais.common.ErrorAuditUtil#record}
	 * agar satu baris bermasalah tidak menggagalkan penyimpanan baris lain. Mata kuliah yang gagal
	 * dihapus karena masih dipakai perkuliahan berjalan dikumpulkan dan ditampilkan sebagai satu
	 * pesan peringatan di akhir, bukan dilempar sebagai error.</p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		KurikulumPunyaMatakuliahDao kurikulumPunyaMatakuliahDao = DaoFactory.getInstance()
				.getKurikulumPunyaMatakuliahDao();
		Session session = kurikulumPunyaMatakuliahDao.getCurrentSession();
		StringBuilder gagalDihapus = new StringBuilder();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				if (data.get(0) instanceof MyCheckboxConfig) {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);

					if (checkbox.isChecked()) {
						Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");

						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
								.createCriteria(KurikulumPunyaMatakuliah.class)
								.add(Restrictions.eq("kurikulum", kurikulum))
								.add(Restrictions.eq("matakuliah", matakuliah))
								.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();

						if (kurikulumPunyaMatakuliah == null) {
							kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
						}
						kurikulumPunyaMatakuliah.setIndukMatakuliah(indukMatakuliah);
						kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
						kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
						kurikulumPunyaMatakuliah.setSemester(semester);
						session.saveOrUpdate(kurikulumPunyaMatakuliah);

					} else {
						Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");
						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
								.createCriteria(KurikulumPunyaMatakuliah.class)
								.add(Restrictions.eq("matakuliah", matakuliah))
								.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.eq("semester", semester))
								.setMaxResults(1).uniqueResult();
						if (kurikulumPunyaMatakuliah != null && !hapusJikaTidakDipakaiPerkuliahan(session,
								kurikulumPunyaMatakuliah)) {
							gagalDihapus.append(gagalDihapus.length() == 0 ? "" : ", ").append(matakuliah.getNama());
						}

					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:224");
			}

		}

		if (deletedMatakuliahs != null) {
			for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : deletedMatakuliahs.values()) {
				if (!hapusJikaTidakDipakaiPerkuliahan(session, kurikulumPunyaMatakuliah)) {
					String nama = kurikulumPunyaMatakuliah.getMatakuliah() == null ? ""
							: kurikulumPunyaMatakuliah.getMatakuliah().getNama();
					if (gagalDihapus.indexOf(nama) < 0) {
						gagalDihapus.append(gagalDihapus.length() == 0 ? "" : ", ").append(nama);
					}
				}
			}

		}

		if (gagalDihapus.length() > 0) {
			try {
				ais.ui.util.MyMessageboxConfig.show(
						"Mata kuliah berikut tidak dihapus dari kurikulum karena sudah dipakai pada perkuliahan yang berjalan: "
								+ gagalDihapus.toString(),
						"Perhatian", org.zkoss.zul.Messagebox.OK, org.zkoss.zul.Messagebox.EXCLAMATION);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:save-warn"); }
		}

	}

	/**
	 * KE-FIX (GenericJDBCException "could not delete ... KurikulumPunyaMatakuliah",
	 * FK "perkuliahan" masih mereferensikan baris ini): sebelumnya session.delete()
	 * langsung dipanggil di sini, tapi Hibernate MENUNDA eksekusi DELETE-nya sampai
	 * flush berikutnya -- yang justru terjadi di MatakuliahKurikulumHelper.loadData()
	 * (query .list() lain, saat refresh grid) sehingga pelanggaran FK muncul di
	 * tempat yang sama sekali tidak terkait & TIDAK tertangkap oleh try/catch di
	 * save(). Cek dulu apakah masih dipakai Perkuliahan sebelum menghapus, supaya
	 * baris yang masih dipakai dilewati dgn pesan yang jelas alih-alih membuat
	 * transaksi Postgres macet ("current transaction is aborted") utk sisa request.
	 *
	 * @param session sesi Hibernate aktif tempat cek dan delete dijalankan.
	 * @param kurikulumPunyaMatakuliah baris yang akan dihapus bila tidak dipakai perkuliahan mana pun.
	 * @return {@code true} bila baris berhasil dihapus (atau tidak dipakai perkuliahan apa pun);
	 *         {@code false} bila masih dipakai setidaknya satu {@link ais.database.model.Perkuliahan}
	 *         (baris tidak dihapus) atau bila pengecekan/penghapusan gagal karena exception.
	 */
	private boolean hapusJikaTidakDipakaiPerkuliahan(Session session,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) {
		try {
			Number jml = (Number) session.createCriteria(ais.database.model.Perkuliahan.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).uniqueResult();
			if (jml != null && jml.longValue() > 0) {
				return false;
			}
			session.delete(kurikulumPunyaMatakuliah);
			return true;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:hapusJikaTidakDipakaiPerkuliahan");
			return false;
		}
	}

	/**
	 * Membangun dan menampilkan (modal) popup pencarian/pemilihan mata kuliah untuk diikat ke
	 * {@code kurikulum} pada {@code semester} tertentu: form filter (fakultas/prodi/kode/nama/
	 * jenjang/milik-universitas/ekstrakurikuler), grid hasil pencarian dengan checkbox pilih-semua,
	 * dan tombol Cari/Simpan/Batal. Tombol Simpan memanggil {@link #save()} lalu
	 * {@code dataLoader.loadData(null)} untuk memuat ulang layar pemanggil sebelum menutup popup.
	 *
	 * @param kurikulum kurikulum tujuan; menentukan default filter fakultas/jurusan/jenjang dari
	 *        {@link Kurikulum#getJurusan()} dan disimpan ke {@link #kurikulum} untuk dipakai
	 *        {@link #save()}/{@link MatakuliahRenderer}.
	 * @param dataLoader callback pemuat ulang data pada layar pemanggil, dipanggil setelah
	 *        {@link #save()} berhasil dijalankan dari tombol Simpan.
	 * @param semester semester kurikulum yang diedit, disimpan ke {@link #semester}.
	 * @param indukMatakuliah bila tidak {@code null}, popup membatasi pencarian ke mata kuliah yang
	 *        merupakan modul (sub-mata kuliah) dari induk ini; disimpan ke {@link #indukMatakuliah}.
	 */
	public void display(final Kurikulum kurikulum, final DataLoader dataLoader, final Integer semester,
			final KurikulumPunyaMatakuliah indukMatakuliah) {

		this.kurikulum = kurikulum;
		this.semester = semester;
		this.indukMatakuliah = indukMatakuliah;

		final MyWindow window = new MyWindow();
		window.setTitle("Ambil Data Matakuliah");
		window.setWidth("90%");
		window.setHeight("95%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, kurikulum.getJurusan() == null ? null : kurikulum.getJurusan().getFakultas());
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Common.clear(jurusan);
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue()));
		Common.pilihJurusan(jurusan, kurikulum.getJurusan());

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");
		kodeMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(milikUniversitas);
		milikUniversitas.setWidth("90%");

		milikUniversitas.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (milikUniversitas.isChecked()) {
					fakultas.setSelectedItem(null);
					jurusan.setSelectedItem(null);

					fakultas.setDisabled(true);
					jurusan.setDisabled(true);
					onSearchDefault(null);
				} else {
					Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
					onSearchDefault(null);
				}
			}
		});

		row.setParent(rows);
		row.appendChild(extraKulikuler = new MyCheckboxConfig("Extra"));
		extraKulikuler.setWidth("90%");
		extraKulikuler.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang);
		Common.selectComboItem(jenjang, kurikulum.getJurusan() == null ? null : kurikulum.getJurusan().getJenjang());
		jenjang.setWidth("90%");
		jenjang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");
		namaMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:427");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ekstra/Uts/Uas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kurikulum");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membentuk {@link Criteria} pencarian {@link Matakuliah} sesuai filter yang sedang terisi di
	 * form: hanya mata kuliah aktif atau belum berstatus (null); cocok sebagian ({@code ILIKE
	 * ANYWHERE}) pada nama dan kode; jenjang, jurusan, dan fakultas (lewat alias {@code jurusan})
	 * disaring hanya bila combobox terkait sudah memilih item; hanya modul ({@code merupakanModul})
	 * bila {@link #indukMatakuliah} tidak {@code null}; hanya ekstrakurikuler bila
	 * {@link #extraKulikuler} dicentang; hanya milik universitas bila {@link #milikUniversitas}
	 * dicentang. Filter yang tidak aktif diisi dengan {@code Restrictions.sqlRestriction("1=1")}
	 * (klausa selalu-benar) alih-alih dihilangkan, agar struktur query tetap seragam.
	 *
	 * @param order parameter dipertahankan untuk kompatibilitas tanda tangan pemanggil; tidak
	 *        memengaruhi pengurutan hasil query pada implementasi saat ini (pengurutan default
	 *        Hibernate/tanpa {@code addOrder} eksplisit).
	 * @return {@link Criteria} siap dieksekusi ({@code .list()}, {@code setMaxResults}, dsb.) oleh
	 *         pemanggil.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		criteria.add(Restrictions.ilike("nama", namaMk.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeMk.getValue(), MatchMode.ANYWHERE))
				.createAlias("jurusan", "jurusan")
				.add(jenjang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", jenjang, false))

				.add(indukMatakuliah != null ? Restrictions.eq("merupakanModul", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(extraKulikuler.isChecked() ? Restrictions.eq("extraKulikuler", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(milikUniversitas.isChecked() ? Restrictions.eq("milikUniversitas", true)
						: Restrictions.sqlRestriction("1=1"))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false));

		return criteria;

	}

	/**
	 * Menjalankan ulang pencarian mata kuliah: menyinkronkan {@link #paging} dengan jumlah total
	 * baris hasil filter saat ini lewat {@link Common#initPaging(Criteria, Paging)}, lalu memuat
	 * satu halaman ({@link Common#ROWS_COUNT_ON_PAGE} baris, maksimum 50) sesuai halaman aktif ke
	 * {@link #grid} dengan {@link MatakuliahRenderer} sebagai perender baris.
	 *
	 * @param event event pemicu (klik tombol Cari, ganti combobox, dsb.); tidak dipakai langsung
	 *        oleh method ini selain menandai bahwa pemanggil memang meminta pencarian ulang.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Matakuliah> matakuliah = initCriteria(true).setMaxResults(50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
