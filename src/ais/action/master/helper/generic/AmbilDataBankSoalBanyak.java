package ais.action.master.helper.generic;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataPenjelasanBankSoalBanbox;
import ais.action.master.helper.DetailUjianHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.KategoriBankSoal;
import ais.database.model.Matakuliah;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data bank soal banyak. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code EventListener
 * eventListener}, {@code List bankSoals}, {@code List bankSoalsHanyaDitampilkan}, {@code Set ids}, {@code
 * MyTextbox soal}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code
 * satuanKerjaTreeModel}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataBankSoalBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;
	private List<Long> bankSoals;
	private List<Long> bankSoalsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	private MyTextbox soal;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	private Combobox searchjenis;

	private AmbilDataMatakuliahBanbox searchmatakuliah;
	private AmbilDataDosenBanbox searchdosen;
	private String jenisKoreksi;

	private Paging paging;
	private Matakuliah matakuliah = null;
	private AmbilDataGuruBanbox searchguru;
	private Combobox searchkategoriBankSoal;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataPenjelasanBankSoalBanbox penjelasanBankSoalBanbox;
	private Matapelajaran matapelajaran;
	private Combobox mk;
	private boolean pt;
	private boolean ya;

	public AmbilDataBankSoalBanyak(List<Long> bankSoals, String jenisKoreksi, Matakuliah matakuliah,
			Matapelajaran matapelajaran) {
		super();
		this.bankSoals = bankSoals;
		this.jenisKoreksi = jenisKoreksi;
		this.matakuliah = matakuliah;
		this.matapelajaran = matapelajaran;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataBankSoalBanyak.java:110");
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataBankSoalBanyak(List<Long> bankSoals, List<Long> bankSoalsHanyaDitampilkan, String jenisKoreksi,
			Matakuliah matakuliah, Matapelajaran matapelajaran) {
		super();
		this.bankSoals = bankSoals;
		this.jenisKoreksi = jenisKoreksi;
		this.bankSoalsHanyaDitampilkan = bankSoalsHanyaDitampilkan;
		this.matakuliah = matakuliah;
		this.matapelajaran = matapelajaran;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataBankSoalBanyak.java:134");
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataBankSoalBanyak}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataBankSoalBanyak} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Tbmuser tbmuser}, {@code
	 * EventListener ubahEventListener}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataBankSoalBanyak
	 */
	class BankSoalRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		private EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		};

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BankSoal bankSoal = (BankSoal) arg1;
			arg0.setAttribute("bankSoal", bankSoal);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Long myBankSoal : bankSoals) {
				if (myBankSoal.equals(bankSoal.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(bankSoal.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(bankSoal.getId());
					} else {
						ids.remove(bankSoal.getId());
					}
				}
			});

			DetailUjianHelper.tampilSoalDanJawaban(arg0, bankSoal, null, tbmuser, true, true, true, ubahEventListener,
					false, false);

		}

	}

	public void display() throws Exception {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		searchyayasan = new Combobox();
		searchsekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Bank Soal");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(searchjenis = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig(BankSoal.PILIHAN_GANDA);
		comboitem.setValue(BankSoal.PILIHAN_GANDA);
		searchjenis.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.ESAY);
		comboitem.setValue(BankSoal.ESAY);
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		searchjenis.appendChild(comboitem);

		searchjenis.setReadonly(true);
		searchjenis.setSelectedItem(comboitem);

		searchjenis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (jenisKoreksi != null) {
			if (jenisKoreksi.equals(PenjelasanBankSoal.KOREKSI_MANUAL)) {
				comboitem = new MyComboitemConfig(BankSoal.ESAY);
				comboitem.setValue(BankSoal.ESAY);
				searchjenis.appendChild(comboitem);
				comboitem = new MyComboitemConfig(BankSoal.JAWABAN_SINGKAT);
				comboitem.setValue(BankSoal.JAWABAN_SINGKAT);
				searchjenis.appendChild(comboitem);
			} else if (jenisKoreksi.equals(PenjelasanBankSoal.KOREKSI_MANUAL)) {
				comboitem = new MyComboitemConfig(BankSoal.PILIHAN_GANDA);
				comboitem.setValue(BankSoal.PILIHAN_GANDA);
				searchjenis.appendChild(comboitem);
			}
		}

		searchjenis.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Soal"));
		row.appendChild(soal = new MyTextbox());
		soal.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		searchdosen = new AmbilDataDosenBanbox(true);
		searchguru = new AmbilDataGuruBanbox(true);

		row.setParent(rows);

		if (ya) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
			row.appendChild(searchguru);
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
			row.appendChild(searchdosen);
		}

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchguru.setWidth("90%");
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Matakuliah")));
		row.appendChild(searchmatakuliah = new AmbilDataMatakuliahBanbox());
		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		if (matakuliah != null) {
			searchmatakuliah.setAttribute("matakuliah", matakuliah);
			searchmatakuliah.setValue(matakuliah.getKode() + "-" + matakuliah.getNama());
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran"));
		row.appendChild(mk = new Combobox());
		mk.setWidth("90%");

		EventListener mkListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Common.insertComboDanSemua(mk, new String[] { "nama", "jenisPenilaian" }, "kelompokMatapelajaran",
						Matapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				mk.setReadonly(true);

				Common.selectComboItem(true, mk, matapelajaran);
			}
		};

		searchsekolah.addEventListener("onChange", mkListener);
		Common.createDefaultTimer(mkListener);

		mk.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		Common.insertComboDanSemua(searchkategoriBankSoal = new Combobox(), new String[] { "nama" }, "keterangan",
				KategoriBankSoal.class, "Kategori", Restrictions.eq("aktif", true));
		toolbar.appendChild(searchkategoriBankSoal);
		searchkategoriBankSoal.setCols(5);

		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue("Unit/Satker");
		row.appendChild(satuanKerja);
		satuanKerja.setCols(5);
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		toolbar.appendChild(satuanKerja);

		penjelasanBankSoalBanbox = new AmbilDataPenjelasanBankSoalBanbox();
		penjelasanBankSoalBanbox.setValue("Penjelasan");
		penjelasanBankSoalBanbox.setCols(5);
		penjelasanBankSoalBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		toolbar.appendChild(penjelasanBankSoalBanbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		// FIX UiException "Only one child is allowed: <Center null>": myCenter1
		// (org.zkoss.zul.Center, region BorderLayout) HANYA boleh punya SATU child
		// langsung. Sebelumnya style block ini DAN grid di bawah sama-sama
		// di-setParent(myCenter1) -- setParent kedua (grid) melempar exception
		// karena myCenter1 sudah terisi oleh elemen style. Bungkus keduanya dalam
		// satu Div pembungkus yang jadi satu-satunya child myCenter1.
		Div wrapperKontenPusat = new Div();
		wrapperKontenPusat.setWidth("100%");
		wrapperKontenPusat.setHeight("100%");
		wrapperKontenPusat.setParent(myCenter1);

		// "Rapikan" tampilan kartu soal (khusus popup ini, scoped ke .bank-soal-rapi): kartu MyGroupboxStyled
		// bawaan memakai lebar 97% + margin:auto + shadow berlapis sehingga tiap tingkat (Soal > Pertanyaan >
		// Pilihan Jawaban > Skor) makin menjorok & terlihat berat. CSS di bawah membuatnya rata penuh, datar
		// (tanpa shadow), border tipis, dan badge caption lebih kecil — tanpa mengubah renderer bersama
		// DetailUjianHelper.tampilSoalDanJawaban yang dipakai di banyak layar lain.
		new ais.ui.util.MyHtml("<style>"
				+ ".bank-soal-rapi .z-groupbox{width:100% !important;max-width:100% !important;margin:6px 0 !important;"
				+ "box-shadow:none !important;border:1px solid #e5e7eb !important;border-radius:8px !important;"
				+ "background:#fff !important;padding:2px 8px 8px !important;}"
				+ ".bank-soal-rapi .z-groupbox .z-groupbox{margin:8px 0 8px 6px !important;background:#fbfcfe !important;}"
				+ ".bank-soal-rapi .z-caption,.bank-soal-rapi .z-groupbox-title{font-size:11px !important;"
				+ "font-weight:600 !important;padding:3px 10px !important;}"
				+ ".bank-soal-rapi .z-grid,.bank-soal-rapi .z-grid .z-row,.bank-soal-rapi .z-grid .z-cell{"
				+ "background:transparent !important;border:0 !important;}"
				+ "</style>").setParent(wrapperKontenPusat);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(wrapperKontenPusat);
		grid.setSclass("fgrid bank-soal-rapi");
		grid.setOddRowSclass("non-odd");

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataBankSoalBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<BankSoal> bankSoals = new ArrayList<BankSoal>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							BankSoal myBankSoal = (BankSoal) row.getAttribute("bankSoal");
							bankSoals.add(myBankSoal);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), bankSoals);
					eventListener.onEvent(myEvent);
				}
				AmbilDataBankSoalBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) penjelasanBankSoalBanbox
				.getAttribute("penjelasanBankSoal");

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<Long> values = new ArrayList<Long>();
		if (bankSoalsHanyaDitampilkan != null) {
			for (Long bankSoal : bankSoalsHanyaDitampilkan) {
				values.add(bankSoal);
			}
		}

		List<Long> notIn = new ArrayList<Long>();
		if (bankSoals != null) {
			for (Long u : bankSoals) {
				notIn.add(u);
			}
		}

		Criteria criteria = session.createCriteria(BankSoal.class)

				.add(penjelasanBankSoal == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal))

				.add(Restrictions.or(Restrictions.isNull("jenisKoreksi"),
						Restrictions.eq("jenisKoreksi", jenisKoreksi)))

				.add(searchkategoriBankSoal.getSelectedItem() == null
						|| searchkategoriBankSoal.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriBankSoal",
										searchkategoriBankSoal.getSelectedItem().getValue()))

				.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notIn)))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

				.add(bankSoalsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(pt || searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(pt || searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(soal.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("soal", soal.getValue().trim(), MatchMode.ANYWHERE))

				.add(ya || searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosen.getAttribute("myValue")))

				.add(pt || searchguru.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("guru", searchguru.getAttribute("myValue")))

				.add(pt || mk.getSelectedItem() == null || mk.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matapelajaran", mk.getSelectedItem().getValue()))

				.add(ya || searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah")))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()))
				.add(ya || searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(ya || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		Session session = HibernateUtil.currentSession();
		List<BankSoal> bankSoal = ids.size() == 0 ? new ArrayList<BankSoal>()
				: ConstantValues.simpleList(
						session.createCriteria(BankSoal.class).addOrder(Order.asc("soal")).add(
								ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
						BankSoal.class);

		List<BankSoal> myBankSoal = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						BankSoal.class);

		bankSoal.addAll(myBankSoal);

		ListModel strset = new SimpleListModel(bankSoal);
		grid.setRowRenderer(new BankSoalRenderer());
		grid.setModel(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
