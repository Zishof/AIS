package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.KonfigurasiAction;
import ais.action.master.helper.util.KonfigurasiKalenderAkademikProcessor;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.KonfigurasiKalenderAkademikDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KalenderAkademik;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk mengelola relasi antara satu {@link KalenderAkademik} (kalender akademik suatu
 * Tahun Akademik/Semester) dengan sejumlah {@link Konfigurasi} yang otomatis berubah nilainya
 * ketika periode kalender tersebut mulai atau selesai. Setiap baris {@link KonfigurasiKalenderAkademik}
 * yang dikelola di sini menyimpan nilai target ({@code padaSaatMulaiBerubahMenjadi}/
 * {@code padaSaatSelesaiBerubahMenjadi}, umumnya {@link Konfigurasi#AKTIF}/{@link Konfigurasi#TIDAK_AKTIF})
 * yang akan diterapkan ke {@link Konfigurasi} terkait oleh
 * {@link ais.action.master.helper.util.KonfigurasiKalenderAkademikProcessor} saat kalender berpindah
 * fase — mekanisme ini dipakai, misalnya, untuk otomatis mengaktifkan "krs"/"penjadwalan"/"penilaian"
 * pada awal semester dan menonaktifkannya kembali saat semester berakhir.
 *
 * <p>
 * Kelas ini menyediakan dua model tampilan berbeda:
 * </p>
 * <ul>
 * <li><b>Tampilan grid penuh</b> ({@link #display(KalenderAkademik, Component)}) — daftar seluruh
 * relasi konfigurasi milik satu kalender akademik dalam tabel dengan tombol tambah/ubah/hapus per
 * baris, dipakai pada layar detail Kalender Akademik.</li>
 * <li><b>Tampilan checklist ringkas</b> ({@link #displayPilihanInline(KalenderAkademik, Component)}) —
 * dua tab: daftar konfigurasi umum yang sering dipakai (KRS, penjadwalan, penilaian, dsb., disaring
 * sesuai institusi Perguruan Tinggi/Sekolah via {@code Common#chekPtAtauSekolah}) sebagai checklist
 * cepat, dan form manual untuk konfigurasi lain di luar daftar tersebut. Dipakai pada wizard
 * pembuatan/penyalinan kalender akademik agar admin tidak perlu menambah relasi satu per satu.</li>
 * </ul>
 *
 * <p>
 * Perhatian: daftar konfigurasi "sering dipakai" (nama dan deskripsinya) muncul terduplikasi persis
 * di tiga tempat pada kelas ini ({@link #ambilDataKonfigurasiSering()}, dalam
 * {@link #displayPilihanInline}, dan dalam {@link #init(KonfigurasiKalenderAkademik)}) — perubahan
 * pada satu daftar harus disinkronkan manual ke daftar lainnya.
 * </p>
 */
public class KonfigurasiKalenderAkademikHelper implements DataLoader {

	/** Grid tampilan daftar (mode {@link #display}); dirender ulang oleh {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Kalender akademik yang sedang dikelola relasi konfigurasinya; diset di awal {@link #display} / {@link #displayPilihanInline}. */
	private KalenderAkademik kalenderAkademik;
	/** Baris {@link KonfigurasiKalenderAkademik} yang sedang diedit/dibuat lewat form manual ({@link #init}/{@link #onSave(Event)}). */
	private KonfigurasiKalenderAkademik konfigurasiKalenderAkademik;
	/** Window modal form tambah/ubah, dibuat oleh {@link #init(KonfigurasiKalenderAkademik)}. */
	private MyWindow addWindow;

	/** Input keterangan bebas pada form manual (dibangun oleh {@link #init}/{@link #tampilkanFormKonfigurasiLain}). */
	private Textbox keterangan;
	/** Kombo pilihan nilai target {@link Konfigurasi} saat kalender MULAI (biasanya {@link Konfigurasi#AKTIF}/{@link Konfigurasi#TIDAK_AKTIF}). */
	private Combobox padaSaatMulaiBerubahMenjadi;
	/** Kombo pilihan nilai target {@link Konfigurasi} saat kalender SELESAI (biasanya {@link Konfigurasi#AKTIF}/{@link Konfigurasi#TIDAK_AKTIF}). */
	private Combobox padaSaatSelesaiBerubahMenjadi;
	/** Bandbox pencarian/pemilihan {@link Konfigurasi} target pada form manual; nilai terpilih disimpan di atribut komponen ("konfigurasi"). */
	private AmbilDataKonfigurasiBanbox konfigurasi;
	/** Listener yang dipicu ulang setiap kali data relasi berubah, biasanya untuk menyegarkan layar induk. */
	private EventListener eventListener;

	/**
	 * @param eventListener listener yang dipicu ulang (via {@code Common#createDefaultTimer}) setiap
	 *                      kali data relasi konfigurasi berubah (simpan/hapus), biasanya dipakai
	 *                      pemanggil untuk menyegarkan tampilan induk (mis. detail Kalender Akademik).
	 */
	public KonfigurasiKalenderAkademikHelper(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KonfigurasiKalenderAkademikHelper}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KonfigurasiKalenderAkademikHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Tbmuser tbmuser}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KonfigurasiKalenderAkademikHelper
	 */
	class DetailKalenderAkademikRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KonfigurasiKalenderAkademik konfigurasiKalenderAkademik = (KonfigurasiKalenderAkademik) data;
			final Konfigurasi konfigurasi = konfigurasiKalenderAkademik.getKonfigurasi();

			if (konfigurasi.getKalenderAkademik() == null || (konfigurasi.getKalenderAkademik() != null
					&& !konfigurasi.getKalenderAkademik().getId().equals(kalenderAkademik.getId()))) {
				konfigurasi.setKalenderAkademik(kalenderAkademik);
				Common.refreshUpdate(konfigurasi);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
			}

			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, konfigurasi.getNama()).setParent(row);

			new Label(konfigurasi.getNilai()).setParent(row);
			new Label(konfigurasi.getTahunAkademik()).setParent(row);
			new Label(konfigurasi.getInfo1()).setParent(row);
			new Label(konfigurasiKalenderAkademik.getPadaSaatMulaiBerubahMenjadi()).setParent(row);
			new Label(konfigurasiKalenderAkademik.getPadaSaatSelesaiBerubahMenjadi()).setParent(row);
			new Label(konfigurasiKalenderAkademik.getKeterangan()).setParent(row);

			Hbox toolbar = new Hbox();

			KonfigurasiAction.tampilKunci(toolbar, konfigurasi, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					loadData(null);
				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setVisible(konfigurasi.getDikunci() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					init(konfigurasiKalenderAkademik);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(konfigurasi.getDikunci() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											Konfigurasi konfigurasi = konfigurasiKalenderAkademik.getKonfigurasi();
											session.refresh(konfigurasi);
											konfigurasi.setKalenderAkademik(null);
											Common.refreshUpdate(session, konfigurasi);
											MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

											Common.refreshDelete(session, konfigurasiKalenderAkademik);

											loadData(null);
											Common.createDefaultTimer(eventListener);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Implementasi {@link DataLoader}: memuat ulang seluruh {@link KonfigurasiKalenderAkademik}
	 * milik {@link #kalenderAkademik} saat ini ke {@link #grid}. Parameter {@code value} tidak
	 * dipakai (dipertahankan untuk kecocokan kontrak antarmuka {@link DataLoader}).
	 *
	 * @param value tidak digunakan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<KonfigurasiKalenderAkademik> konfigurasiKalenderAkademik = ConstantValues
				.simpleList(
						session.createCriteria(KonfigurasiKalenderAkademik.class).addOrder(Order.asc("id"))
								.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)),
						KonfigurasiKalenderAkademik.class);

		ListModel strset = new SimpleListModel(konfigurasiKalenderAkademik);
		grid.setRowRenderer(new DetailKalenderAkademikRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menampilkan tampilan grid penuh: tabel semua relasi {@link KonfigurasiKalenderAkademik} milik
	 * {@code kalenderAkademik}, lengkap dengan tombol "Tambah Konfigurasi" dan "Refresh" (yang
	 * menjalankan ulang {@code KonfigurasiKalenderAkademikProcessor.doProcess()} sebelum memuat
	 * ulang data).
	 *
	 * @param kalenderAkademik kalender akademik yang relasi konfigurasinya akan ditampilkan
	 * @param component        komponen ZK induk tempat grid dirender (dibersihkan lebih dulu)
	 */
	public void display(final KalenderAkademik kalenderAkademik, final Component component) {
		this.kalenderAkademik = kalenderAkademik;
		Common.clear(component);

		MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
		groupboxStyled.setParent(component);
		groupboxStyled.setWidth("100%");
		groupboxStyled.appendChild(new MyCaptionStyled("Daftar konfigurasi yang terkait kalender akademik ini"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupboxStyled);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Konfigurasi", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				init(new KonfigurasiKalenderAkademik());
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KonfigurasiKalenderAkademikProcessor.doProcess();
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupboxStyled);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Saat Mulai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Saat Selesai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		loadData(null);

	}

	/**
	 * Menampilkan tampilan checklist ringkas dua tab ("Konfigurasi yang sering dipakai" dan
	 * "Konfigurasi Lain") untuk memilih konfigurasi mana yang akan direlasikan ke
	 * {@code kalenderAkademik}. Status checklist diinisialisasi dari relasi yang sudah tersimpan di
	 * database (via {@link #ambilKonfigurasiTerpilihDariDatabase()}); perubahan dari tab "sering
	 * dipakai" hanya tersimpan ke database saat {@link #simpanInline(Event)} dipanggil oleh
	 * pemanggil (bukan otomatis saat klik checkbox).
	 *
	 * @param kalenderAkademik kalender akademik yang akan direlasikan dengan konfigurasi terpilih
	 * @param component        komponen ZK induk tempat tabbox dirender (dibersihkan lebih dulu)
	 */
	public void displayPilihanInline(final KalenderAkademik kalenderAkademik, final Component component) {
		this.kalenderAkademik = kalenderAkademik;
		this.konfigurasiKalenderAkademik = new KonfigurasiKalenderAkademik();
		dataSelected.clear();
		ambilKonfigurasiTerpilihDariDatabase();
		Common.clear(component);

		Borderlayout borderlayoutUtama = new Borderlayout();
		borderlayoutUtama.setParent(component);
		borderlayoutUtama.setWidth("100%");
		borderlayoutUtama.setHeight("100%");

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(centerUtama);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSering = new MyTabConfig("Konfigurasi yang sering dipakai");
		tabSering.setParent(tabs);

		MyTabConfig tabLain = new MyTabConfig("Konfigurasi Lain");
		tabLain.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelSering = new ais.ui.util.MyTabpanel();
		tabpanelSering.setStyle("min-height:400px;overflow:auto;");
		tabpanelSering.setParent(tabpanels);
		tampilkanChecklistKonfigurasiSering(tabpanelSering);

		Tabpanel tabpanelLain = new ais.ui.util.MyTabpanel();
		tabpanelLain.setStyle("min-height:400px;overflow:auto;");
		tabpanelLain.setParent(tabpanels);
		tampilkanFormKonfigurasiLain(tabpanelLain, this.konfigurasiKalenderAkademik);

	}

	/**
	 * Nama {@link Konfigurasi} (via {@link Konfigurasi#getNama()}) yang sedang tercentang pada
	 * checklist "sering dipakai" ({@link #displayPilihanInline}/{@link #init}). Diisi ulang dari
	 * database oleh {@link #ambilKonfigurasiTerpilihDariDatabase()} dan diubah langsung oleh
	 * listener {@code onClick} tiap checkbox; hanya dipersist ke database saat {@link #simpanInline}
	 * atau tombol Simpan pada {@link #init} dijalankan.
	 */
	private Set<String> dataSelected = new HashSet<String>();

	/**
	 * Mengisi ulang {@link #dataSelected} dari relasi {@link KonfigurasiKalenderAkademik} yang sudah
	 * tersimpan di database untuk {@link #kalenderAkademik} saat ini, agar checklist tampilan inline
	 * merefleksikan status tersimpan. Tidak melakukan apa pun bila {@link #kalenderAkademik} belum
	 * diset.
	 */
	private void ambilKonfigurasiTerpilihDariDatabase() {
		if (kalenderAkademik == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<KonfigurasiKalenderAkademik> list = ConstantValues.simpleList(
				session.createCriteria(KonfigurasiKalenderAkademik.class)
						.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)),
				KonfigurasiKalenderAkademik.class);
		for (int i = 0; i < list.size(); i++) {
			KonfigurasiKalenderAkademik item = list.get(i);
			if (item != null && item.getKonfigurasi() != null && item.getKonfigurasi().getNama() != null) {
				dataSelected.add(item.getKonfigurasi().getNama());
			}
		}
	}

	/**
	 * Membangun daftar statis nama+deskripsi {@link Konfigurasi} yang "sering dipakai" (KRS,
	 * penjadwalan, penilaian, checklist penilaian dosen/guru, dsb.), disaring berdasarkan jenis
	 * institusi (Perguruan Tinggi dan/atau Sekolah/Yayasan) via {@link Common#chekPtAtauSekolah()}.
	 * Dipakai baik oleh {@link #tampilkanChecklistKonfigurasiSering(Component)} maupun oleh
	 * {@link #simpanKonfigurasiTerpilih()} untuk menentukan nama mana yang termasuk kelompok
	 * "sering dipakai" (bukan form manual).
	 *
	 * <p><b>Perhatian:</b> daftar yang sama (nama dan deskripsinya) juga dituliskan ulang secara
	 * terpisah di dalam {@link #init(KonfigurasiKalenderAkademik)} — lihat catatan duplikasi pada
	 * Javadoc kelas ini.</p>
	 *
	 * @return daftar pasangan {@code {nama, deskripsi}}, tiap elemen array berukuran 2
	 */
	private List<String[]> ambilDataKonfigurasiSering() {
		List<String[]> dataKonfigurasi = new ArrayList<String[]>();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		if (ya) {
			dataKonfigurasi.add(new String[] { "checklist_penilaian_guru",
					"Aktivasi keharusan siswa melakukan penilaian terhadap guru-guru-nya" });
		}

		if (pt) {
			dataKonfigurasi.add(new String[] { "krs", "Pengambilan KRS oleh mahasiswa" });
			dataKonfigurasi.add(
					new String[] { "krs_sp", "Pengambilan KRS semester pendek / semester antara oleh mahasiswa" });
			dataKonfigurasi.add(new String[] { "krs_remedial", "Pengambilan KRS remedial oleh mahasiswa" });
			dataKonfigurasi.add(new String[] { "perbaikan_krs",
					"Perbaikan KRS oleh mahasiswa (yang sebelumnya tidak ambil KRS tidak boleh ambil kecuali dari admin)" });
			dataKonfigurasi.add(new String[] { "aktivasi_persetujuan_KRS_oleh_dosen",
					"Persetujuan KRS yang diambil mahasiswa oleh dosen / admin" });
			dataKonfigurasi.add(new String[] { "aktivasi_persetujuan_KRS_sp_oleh_dosen",
					"Persetujuan KRS semester pendek / semester antara yang diambil mahasiswa oleh dosen / admin" });
			dataKonfigurasi.add(new String[] { "penilaian", "Input penilaian mahasiswa oleh dosen" });
			dataKonfigurasi.add(new String[] { "penilaian_sp",
					"Input penilaian semester pendek / semester antara mahasiswa oleh dosen" });
			dataKonfigurasi.add(new String[] { "penjadwalan", "Input jadwal perkuliahan oleh akademik / admin" });
			dataKonfigurasi.add(new String[] { "penjadwalan_sp",
					"Input jadwal perkuliahan semester pendek / semester antara oleh akademik / admin" });
			dataKonfigurasi.add(new String[] { "checklist_penilaian_dosen",
					"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester selanjutnya. Misal: jika sekarang semester 2, maka mahasiswa wajib mengisi angket di semester 1" });
			dataKonfigurasi.add(new String[] { "checklist_penilaian_dosen_semester_berlangsung",
					"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester berlangsung. Misal: jika sekarang semester 1, maka mahasiswa wajib mengisi angket di semester 1" });
			dataKonfigurasi.add(new String[] { "checklist_penilaian_oleh_dosen",
					"Aktivasi keharusan dosen melakukan penilaian terhadap matakuliah yang diajar-nya" });
		}

		return dataKonfigurasi;
	}

	/**
	 * Merender grid checklist "Konfigurasi yang sering dipakai" (tab pertama pada
	 * {@link #displayPilihanInline}): satu baris per item {@link #ambilDataKonfigurasiSering()},
	 * checkbox diinisialisasi checked/unchecked dari {@link #dataSelected}, dan tiap klik langsung
	 * menambah/menghapus nama konfigurasi dari {@link #dataSelected} (belum dipersist ke database).
	 *
	 * @param parent komponen ZK induk tempat grid checklist dirender
	 */
	private void tampilkanChecklistKonfigurasiSering(Component parent) {
		MyGrid gridChecklist = new MyGrid();
		gridChecklist.setSclass("fgrid");
		gridChecklist.setWidth("100%");
		gridChecklist.setHeight("100%");
		gridChecklist.setParent(parent);

		Rows rows = new Rows();
		rows.setParent(gridChecklist);

		List<String[]> dataKonfigurasi = ambilDataKonfigurasiSering();
		for (final String[] d : dataKonfigurasi) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(d[1]);
			checkboxConfig.setStyle("font-size:12px;font-weight: bold;");
			checkboxConfig.setChecked(dataSelected.contains(d[0]));
			vbox.appendChild(checkboxConfig);
			vbox.appendChild(new MyLabelAgakKecilBold(d[0]));
			checkboxConfig.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkboxConfig.isChecked()) {
						dataSelected.add(d[0]);
					} else {
						dataSelected.remove(d[0]);
					}
				}
			});
		}
	}

	/**
	 * Merender form manual "Konfigurasi Lain" (tab kedua pada {@link #displayPilihanInline}): kombo
	 * {@link #padaSaatMulaiBerubahMenjadi}/{@link #padaSaatSelesaiBerubahMenjadi}, bandbox
	 * {@link #konfigurasi} untuk memilih {@link Konfigurasi} target di luar daftar "sering dipakai",
	 * dan {@link #keterangan}. Nilai awal komponen diambil dari {@code konfigurasiKalenderAkademik}
	 * yang diberikan (baris baru atau baris existing bila sedang mengubah).
	 *
	 * @param parent                     komponen ZK induk tempat form dirender
	 * @param konfigurasiKalenderAkademik sumber nilai awal form; boleh berupa instance baru (kosong)
	 */
	private void tampilkanFormKonfigurasiLain(Component parent,
			KonfigurasiKalenderAkademik konfigurasiKalenderAkademik) {
		MyGrid gridForm = new MyGrid();
		gridForm.setSclass("fgrid");
		gridForm.setWidth("100%");
		gridForm.setHeight("100%");
		gridForm.setParent(parent);

		Columns columns = new Columns();
		columns.setParent(gridForm);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridForm);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pada Saat Mulai Berubah Menjadi"));
		row.appendChild(padaSaatMulaiBerubahMenjadi = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig(Konfigurasi.AKTIF);
		comboitem.setValue(Konfigurasi.AKTIF);
		padaSaatMulaiBerubahMenjadi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Konfigurasi.TIDAK_AKTIF);
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		padaSaatMulaiBerubahMenjadi.appendChild(comboitem);
		Common.selectComboItem(padaSaatMulaiBerubahMenjadi,
				konfigurasiKalenderAkademik.getPadaSaatMulaiBerubahMenjadi());
		padaSaatMulaiBerubahMenjadi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pada Saat Selesai Berubah Menjadi"));
		row.appendChild(padaSaatSelesaiBerubahMenjadi = new Combobox());
		comboitem = new MyComboitemConfig(Konfigurasi.AKTIF);
		comboitem.setValue(Konfigurasi.AKTIF);
		padaSaatSelesaiBerubahMenjadi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Konfigurasi.TIDAK_AKTIF);
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		padaSaatSelesaiBerubahMenjadi.appendChild(comboitem);
		Common.selectComboItem(padaSaatSelesaiBerubahMenjadi,
				konfigurasiKalenderAkademik.getPadaSaatSelesaiBerubahMenjadi());
		padaSaatSelesaiBerubahMenjadi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Konfigurasi"));
		row.appendChild(konfigurasi = new AmbilDataKonfigurasiBanbox());
		konfigurasi.setValue(konfigurasiKalenderAkademik.getKonfigurasi() == null ? ""
				: konfigurasiKalenderAkademik.getKonfigurasi().getNama() + "-"
						+ konfigurasiKalenderAkademik.getKonfigurasi().getNilai() + ""
						+ (konfigurasiKalenderAkademik.getKonfigurasi().getTahunAkademik() == null ? ""
								: "-" + konfigurasiKalenderAkademik.getKonfigurasi().getTahunAkademik()));
		konfigurasi.setAttribute("konfigurasi", konfigurasiKalenderAkademik.getKonfigurasi());
		konfigurasi.setAttribute("myValue", konfigurasiKalenderAkademik.getKonfigurasi());
		konfigurasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(konfigurasiKalenderAkademik.getKeterangan() == null ? ""
				: konfigurasiKalenderAkademik.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
	}

	/**
	 * Menyimpan hasil checklist pada tampilan {@link #displayPilihanInline} (memanggil
	 * {@link #simpanKonfigurasiTerpilih()}), lalu — bila form "Konfigurasi Lain" juga diisi (kombo
	 * {@link #konfigurasi} punya nilai) — turut menyimpannya lewat {@link #onSave(Event)}.
	 *
	 * @param event event ZK pemicu (mis. klik tombol Simpan pada wizard pemanggil)
	 * @return {@code true} bila tidak ada form "Konfigurasi Lain" yang perlu divalidasi, atau hasil
	 *         validasi {@link #onSave(Event)} bila ada
	 * @throws Exception diteruskan dari kegagalan akses database
	 */
	public boolean simpanInline(Event event) throws Exception {
		simpanKonfigurasiTerpilih();
		if (konfigurasi != null && konfigurasi.getAttribute("konfigurasi") != null) {
			return onSave(event);
		}
		return true;
	}

	/** Memuat ulang tampilan checklist inline untuk {@code kalenderAkademik}; alias tipis di atas {@link #displayPilihanInline}. */
	public void refreshInline(final KalenderAkademik kalenderAkademik, final Component component) {
		displayPilihanInline(kalenderAkademik, component);
	}

	/**
	 * Menyimpan hasil checklist "sering dipakai" ({@link #dataSelected}) ke database untuk
	 * {@link #kalenderAkademik} saat ini: nama yang di-uncheck (tapi sebelumnya tersimpan dan
	 * termasuk kelompok "sering dipakai" menurut {@link #ambilDataKonfigurasiSering()}) dilepas
	 * relasinya (baris {@link KonfigurasiKalenderAkademik} dihapus, {@link Konfigurasi} terkait
	 * dilepas dari kalender ini), sedangkan nama yang tercentang dicarikan/dibuatkan
	 * {@link Konfigurasi} (dicocokkan berdasarkan nama+tahun akademik+jenis semester) lalu direlasikan
	 * lewat baris {@link KonfigurasiKalenderAkademik} baru bila belum ada. Diakhiri dengan menjalankan
	 * ulang {@code KonfigurasiKalenderAkademikProcessor.doProcess()} agar efek konfigurasi langsung
	 * diterapkan sesuai fase kalender saat ini.
	 */
	private void simpanKonfigurasiTerpilih() {
		Session session = HibernateUtil.currentSession();
		List<String[]> dataKonfigurasi = ambilDataKonfigurasiSering();
		Set<String> namaKonfigurasiSering = new HashSet<String>();
		for (int i = 0; i < dataKonfigurasi.size(); i++) {
			String[] d = dataKonfigurasi.get(i);
			if (d != null && d.length > 0 && d[0] != null) {
				namaKonfigurasiSering.add(d[0]);
			}
		}
		List<KonfigurasiKalenderAkademik> tersimpan = ConstantValues.simpleList(
				session.createCriteria(KonfigurasiKalenderAkademik.class)
						.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)),
				KonfigurasiKalenderAkademik.class);
		for (int i = 0; i < tersimpan.size(); i++) {
			KonfigurasiKalenderAkademik item = tersimpan.get(i);
			if (item == null || item.getKonfigurasi() == null || item.getKonfigurasi().getNama() == null) {
				continue;
			}
			String nama = item.getKonfigurasi().getNama();
			if (namaKonfigurasiSering.contains(nama) && !dataSelected.contains(nama)) {
				Konfigurasi konfigurasi = item.getKonfigurasi();
				konfigurasi.setKalenderAkademik(null);
				Common.refreshUpdate(session, konfigurasi);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
				Common.refreshDelete(session, item);
				session.flush();
			}
		}
		for (String s : dataSelected) {
			String jenisSemester = kalenderAkademik.getGanjilGenap();
			String tahunAkademik = kalenderAkademik.getTahunAjaran();
			Konfigurasi konfigurasi = (Konfigurasi) ConstantValues.simpleObject(
					session.createCriteria(Konfigurasi.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("info1", jenisSemester))
							.add(Restrictions.eq("nama", s))
							.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1),
					Konfigurasi.class);
			if (konfigurasi == null) {
				konfigurasi = new Konfigurasi();
				konfigurasi.setNama(s);
				konfigurasi.setTahunAkademik(tahunAkademik);
				konfigurasi.setNilai(Konfigurasi.AKTIF);
				konfigurasi.setInfo1(jenisSemester);
				session.save(konfigurasi);
				session.flush();
			}

			KonfigurasiKalenderAkademik konfigurasiKalenderAkademik = (KonfigurasiKalenderAkademik) ConstantValues
					.simpleObject(session.createCriteria(KonfigurasiKalenderAkademik.class)
							.add(Restrictions.eq("konfigurasi", konfigurasi))
							.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)).setMaxResults(1),
							KonfigurasiKalenderAkademik.class);
			if (konfigurasiKalenderAkademik == null) {
				konfigurasiKalenderAkademik = new KonfigurasiKalenderAkademik();
				konfigurasiKalenderAkademik.setKalenderAkademik(kalenderAkademik);
				konfigurasiKalenderAkademik.setKonfigurasi(konfigurasi);
				konfigurasiKalenderAkademik.setKeterangan(keterangan == null ? null : keterangan.getValue());
				session.save(konfigurasiKalenderAkademik);
				session.flush();
			}
		}
		KonfigurasiKalenderAkademikProcessor.doProcess();
	}

	/**
	 * Membangun dan menampilkan window modal tambah/ubah satu relasi {@link KonfigurasiKalenderAkademik}.
	 * Untuk baris BARU ({@code konfigurasiKalenderAkademik.getId() == null}) window menampilkan dua tab
	 * ("Konfigurasi yang sering dipakai" sebagai checklist, dan "Konfigurasi Lain" sebagai form manual
	 * yang membangun ulang grid via {@link #ambilDataKonfigurasiSering()}/{@link #dataSelected} —
	 * duplikat dari {@link #tampilkanChecklistKonfigurasiSering}, lihat catatan duplikasi pada Javadoc
	 * kelas); untuk baris EXISTING hanya form manual yang ditampilkan (tab checklist tidak relevan
	 * karena baris sudah terikat satu {@link Konfigurasi} tertentu). Tombol Simpan menyimpan baris
	 * checklist yang tercentang (bila ada, logika sama seperti {@link #simpanKonfigurasiTerpilih()}
	 * namun disalin inline) dan/atau memanggil {@link #onSave(Event)} untuk form manual.
	 *
	 * @param konfigurasiKalenderAkademik baris yang akan diedit, atau instance baru untuk membuat baris baru
	 * @throws Exception diteruskan dari kegagalan render/akses database
	 */
	private void init(KonfigurasiKalenderAkademik konfigurasiKalenderAkademik) throws Exception {
		this.konfigurasiKalenderAkademik = konfigurasiKalenderAkademik;
		addWindow = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setHeight("95%");
		addWindow.setWidth("550px");

		addWindow.setTitle(konfigurasiKalenderAkademik.getId() == null ? "Tambah Konfigurasi Kalender Akademik" : "Ubah Konfigurasi Kalender Akademik");
		Common.clear(addWindow);

		Borderlayout borderlayoutUtama = new Borderlayout();
		borderlayoutUtama.setParent(addWindow);
		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		Borderlayout borderlayout = new Borderlayout();

		if (konfigurasiKalenderAkademik == null || konfigurasiKalenderAkademik.getId() == null) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(centerUtama);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabSoal = new MyTabConfig("Konfigurasi yang sering dipakai");
			tabSoal.setParent(tabs);

			MyTabConfig tab1Pertemuan = new MyTabConfig();
			tab1Pertemuan.setParent(tabs);
			tab1Pertemuan.setLabel("Konfigurasi Lain");

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 400px;");
			tabpanelUtama.setParent(tabpanels);

			borderlayout.setParent(tabpanelUtama);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			MyGrid grid = new MyGrid();
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			List<String[]> dataKonfigurasi = new ArrayList<String[]>();

			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean pt = ptYa[0];
			boolean ya = ptYa[1];

			// Mode sekolah/yayasan dan mode perguruan tinggi DIPERIKSA INDEPENDEN (bukan else-if):
			// bila kedua mode aktif (institusi gabungan sekolah + PT), TAMPILKAN SEMUA konfigurasi
			// — checklist penilaian guru (sekolah) DAN seluruh konfigurasi perkuliahan (PT).
			if (ya) {
				dataKonfigurasi.add(new String[] { "checklist_penilaian_guru",
						"Aktivasi keharusan siswa melakukan penilaian terhadap guru-guru-nya" });
			}

			if (pt) {

				dataKonfigurasi.add(new String[] { "krs", "Pengambilan KRS oleh mahasiswa" });
				dataKonfigurasi.add(
						new String[] { "krs_sp", "Pengambilan KRS semester pendek / semester antara oleh mahasiswa" });

				dataKonfigurasi.add(new String[] { "krs_remedial", "Pengambilan KRS remedial oleh mahasiswa" });

				dataKonfigurasi.add(new String[] { "perbaikan_krs",
						"Perbaikan KRS oleh mahasiswa (yang sebelumnya tidak ambil KRS tidak boleh ambil kecuali dari admin)" });

				dataKonfigurasi.add(new String[] { "aktivasi_persetujuan_KRS_oleh_dosen",
						"Persetujuan KRS yang diambil mahasiswa oleh dosen / admin" });
				dataKonfigurasi.add(new String[] { "aktivasi_persetujuan_KRS_sp_oleh_dosen",
						"Persetujuan KRS semester pendek / semester antara yang diambil mahasiswa oleh dosen / admin" });

				dataKonfigurasi.add(new String[] { "penilaian", "Input penilaian mahasiswa oleh dosen" });
				dataKonfigurasi.add(new String[] { "penilaian_sp",
						"Input penilaian semester pendek / semester antara mahasiswa oleh dosen" });

				dataKonfigurasi.add(new String[] { "penjadwalan", "Input jadwal perkuliahan oleh akademik / admin" });
				dataKonfigurasi.add(new String[] { "penjadwalan_sp",
						"Input jadwal perkuliahan semester pendek / semester antara oleh akademik / admin" });

				dataKonfigurasi.add(new String[] { "checklist_penilaian_dosen",
						"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester selanjutnya. Misal: jika sekarang semester 2, maka mahasiswa wajib mengisi angket di semester 1" });
				dataKonfigurasi.add(new String[] { "checklist_penilaian_dosen_semester_berlangsung",
						"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester berlangsung. Misal: jika sekarang semester 1, maka mahasiswa wajib mengisi angket di semester 1" });
				dataKonfigurasi.add(new String[] { "checklist_penilaian_oleh_dosen",
						"Aktivasi keharusan dosen melakukan penilaian terhadap matakuliah yang diajar-nya" });
			}

			Rows rows = new Rows();
			rows.setParent(grid);

			for (final String[] d : dataKonfigurasi) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				Vbox vbox = new Vbox();
				vbox.setParent(row);
				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(d[1]);
				checkboxConfig.setStyle("font-size:12px;font-weight: bold;");
				vbox.appendChild(checkboxConfig);
				vbox.appendChild(new MyLabelAgakKecilBold(d[0]));
				checkboxConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkboxConfig.isChecked()) {
							dataSelected.add(d[0]);
						} else {
							dataSelected.remove(d[0]);
						}
					}
				});
			}

			tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 400px;");
			tabpanelUtama.setParent(tabpanels);

			borderlayout = new Borderlayout();
			borderlayout.setParent(tabpanelUtama);
		} else {
			borderlayout.setParent(centerUtama);
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pada Saat Mulai Berubah Menjadi"));
		row.appendChild(padaSaatMulaiBerubahMenjadi = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig(Konfigurasi.AKTIF);
		comboitem.setValue(Konfigurasi.AKTIF);
		padaSaatMulaiBerubahMenjadi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Konfigurasi.TIDAK_AKTIF);
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		padaSaatMulaiBerubahMenjadi.appendChild(comboitem);
		Common.selectComboItem(padaSaatMulaiBerubahMenjadi,
				konfigurasiKalenderAkademik.getPadaSaatMulaiBerubahMenjadi());
		padaSaatMulaiBerubahMenjadi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pada Saat Selesai Berubah Menjadi"));
		row.appendChild(padaSaatSelesaiBerubahMenjadi = new Combobox());
		comboitem = new MyComboitemConfig(Konfigurasi.AKTIF);
		comboitem.setValue(Konfigurasi.AKTIF);
		padaSaatSelesaiBerubahMenjadi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Konfigurasi.TIDAK_AKTIF);
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		padaSaatSelesaiBerubahMenjadi.appendChild(comboitem);
		Common.selectComboItem(padaSaatSelesaiBerubahMenjadi,
				konfigurasiKalenderAkademik.getPadaSaatSelesaiBerubahMenjadi());
		padaSaatSelesaiBerubahMenjadi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Konfigurasi"));
		row.appendChild(konfigurasi = new AmbilDataKonfigurasiBanbox());
		konfigurasi.setValue(konfigurasiKalenderAkademik.getKonfigurasi() == null ? ""
				: konfigurasiKalenderAkademik.getKonfigurasi().getNama() + "-"
						+ konfigurasiKalenderAkademik.getKonfigurasi().getNilai() + ""
						+ (konfigurasiKalenderAkademik.getKonfigurasi().getTahunAkademik() == null ? ""
								: "-" + konfigurasiKalenderAkademik.getKonfigurasi().getTahunAkademik()));

		konfigurasi.setAttribute("konfigurasi", konfigurasiKalenderAkademik.getKonfigurasi());
		konfigurasi.setAttribute("myValue", konfigurasiKalenderAkademik.getKonfigurasi());
		konfigurasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(konfigurasiKalenderAkademik.getKeterangan() == null ? ""
				: konfigurasiKalenderAkademik.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutUtama);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (!dataSelected.isEmpty()) {
					Session session = HibernateUtil.currentSession();
					for (String s : dataSelected) {
						String jenisSemester = kalenderAkademik.getGanjilGenap();
						String tahunAkademik = kalenderAkademik.getTahunAjaran();
						Konfigurasi konfigurasi = (Konfigurasi) ConstantValues.simpleObject(
								HibernateUtil.currentSession().createCriteria(Konfigurasi.class)
										.addOrder(Order.desc("id")).add(Restrictions.eq("info1", jenisSemester))
										.add(Restrictions.eq("nama", s))
										.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1),
								Konfigurasi.class);
						if (konfigurasi == null) {
							konfigurasi = new Konfigurasi();
							konfigurasi.setNama(s);
							konfigurasi.setTahunAkademik(tahunAkademik);
							konfigurasi.setNilai(Konfigurasi.AKTIF);
							konfigurasi.setInfo1(jenisSemester);
							session.save(konfigurasi);
							session.flush();
						}

						KonfigurasiKalenderAkademik konfigurasiKalenderAkademik = (KonfigurasiKalenderAkademik) ConstantValues
								.simpleObject(session.createCriteria(KonfigurasiKalenderAkademik.class)
										.add(Restrictions.eq("konfigurasi", konfigurasi))
										.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)).setMaxResults(1),
										KonfigurasiKalenderAkademik.class);
						if (konfigurasiKalenderAkademik == null) {
							konfigurasiKalenderAkademik = new KonfigurasiKalenderAkademik();
							konfigurasiKalenderAkademik.setKalenderAkademik(kalenderAkademik);
							konfigurasiKalenderAkademik.setKonfigurasi(konfigurasi);
							konfigurasiKalenderAkademik.setKeterangan(keterangan.getValue());
							session.save(konfigurasiKalenderAkademik);
							session.flush();
						}
					}
				}

				if (!dataSelected.isEmpty() || onSave(event)) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
							addWindow.detach();
							Common.createDefaultTimer(eventListener);
						}
					}, "Sedang menyiapkan data", false, 1000);
				}
			}
		});
		save.setParent(toolbar);

		addWindow.onModal();
	}

	/**
	 * Memvalidasi dan menyimpan satu relasi {@link KonfigurasiKalenderAkademik} dari form manual
	 * (dibangun oleh {@link #init(KonfigurasiKalenderAkademik)} atau
	 * {@link #tampilkanFormKonfigurasiLain}). Mensyaratkan {@code padaSaatMulaiBerubahMenjadi},
	 * {@code padaSaatSelesaiBerubahMenjadi}, dan {@link Konfigurasi} target terisi; setelah tersimpan
	 * menjalankan ulang {@code KonfigurasiKalenderAkademikProcessor.doProcess()} agar efeknya
	 * langsung diterapkan, lalu memicu {@link #eventListener}.
	 *
	 * @param event event ZK pemicu (mis. klik tombol Simpan)
	 * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila ada field wajib
	 *         yang belum terisi
	 * @throws Exception diteruskan dari kegagalan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (padaSaatMulaiBerubahMenjadi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, opsi 'Pada Saat Mulai Berubah Menjadi' belum dipilih. Langkah yang dapat dilakukan: (1) pilih opsi dari daftar yang tersedia; (2) pastikan konfigurasi awal sudah diisi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (padaSaatSelesaiBerubahMenjadi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, opsi 'Pada Saat Selesai Berubah Menjadi' belum dipilih. Langkah yang dapat dilakukan: (1) pilih opsi dari daftar yang tersedia; (2) pastikan konfigurasi akhir sudah diisi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (konfigurasi.getAttribute("konfigurasi") == null) {
			MyMessageboxConfig.show("Mohon maaf, konfigurasi kalender akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih konfigurasi dari daftar yang tersedia; (2) pastikan data konfigurasi sudah ada; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KonfigurasiKalenderAkademikDao konfigurasiKalenderAkademikDao = DaoFactory.getInstance()
				.getKonfigurasiKalenderAkademikDao();
		if (konfigurasiKalenderAkademik.getId() != null) {
			konfigurasiKalenderAkademik = konfigurasiKalenderAkademikDao.load(konfigurasiKalenderAkademik.getId());

		}

		konfigurasiKalenderAkademik.setKalenderAkademik(kalenderAkademik);
		konfigurasiKalenderAkademik.setKonfigurasi((Konfigurasi) konfigurasi.getAttribute("konfigurasi"));
		konfigurasiKalenderAkademik
				.setPadaSaatMulaiBerubahMenjadi((String) padaSaatMulaiBerubahMenjadi.getSelectedItem().getValue());
		konfigurasiKalenderAkademik
				.setPadaSaatSelesaiBerubahMenjadi((String) padaSaatSelesaiBerubahMenjadi.getSelectedItem().getValue());
		konfigurasiKalenderAkademik.setKeterangan(keterangan.getValue());

		if (konfigurasiKalenderAkademik.getId() != null) {
			konfigurasiKalenderAkademikDao.update(konfigurasiKalenderAkademik);
		} else {
			konfigurasiKalenderAkademikDao.save(konfigurasiKalenderAkademik);
		}

		KonfigurasiKalenderAkademikProcessor.doProcess();
		Common.createDefaultTimer(eventListener);
		return true;
	}

}
