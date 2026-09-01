package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Gedung;
import ais.database.model.Jurusan;
import ais.database.model.Ruang;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Ruang} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Ruang} adalah entity master
 * ruangan fisik kampus (ruang kelas, laboratorium, aula, dsb.) yang dipakai antara lain sebagai
 * lokasi penjadwalan kuliah/ujian/seminar.
 *
 * <p>
 * Kriteria pencarian: kode/nama ruang ({@code nama}, cocok ANYWHERE terhadap {@code nama} ATAU
 * {@code kodeRuangan}), gedung ({@code gedung}), serta — tergantung apakah instansi berjenis
 * perguruan tinggi atau sekolah (hasil {@link Common#chekPtAtauSekolah()}) — fakultas/program
 * studi ({@code fakultas}, {@code jurusan}) atau yayasan/sekolah ({@code yayasan}, {@code
 * sekolah}). Hasil selalu dibatasi ke ruang yang berstatus aktif ({@code aktif} null atau
 * {@code true}), dan tiap filter entity induk (fakultas/jurusan/yayasan/sekolah) mengizinkan baris
 * dengan nilai kolom NULL di database ikut tampil (pola {@code Restrictions.isNull(...) OR
 * eq(...)}) alih-alih membatasi ketat. Mode pilih data bersifat TUNGGAL lewat {@link
 * org.zkoss.zul.Radiogroup}; saat baris dipilih, ruang berkapasitas 0 ditolak dengan pesan
 * peringatan (ruang semacam itu dianggap tidak layak dipakai).
 * </p>
 * <p>
 * {@link #setJurusan(Jurusan)} dan {@link #setFakultas(Fakultas)} memungkinkan pemanggil membatasi
 * pencarian ke satu jurusan/fakultas tertentu (mis. saat Bandbox ini dipasang pada form yang sudah
 * punya konteks fakultas/jurusan) — combobox terkait otomatis dinonaktifkan (disabled) bila nilai
 * awal ini diberikan, sehingga pengguna tidak bisa mengubahnya lagi.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataRuangBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Membangun Bandbox dalam mode readonly dan memasang listener {@code onOpen} standar (lazy-build
	 * popup pencarian ruang saat pertama kali dibuka). Mengikuti kerangka konstruktor baku — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataRuangBanbox() {
		super();
		setReadonly(true);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	private Textbox nama;
	private Combobox gedung;
	private Combobox fakultas;
	private Combobox jurusan;
	private Jurusan selectedJurusan;
	private Fakultas selectedFakultas;
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;

	/**
	 * Renderer baris grid hasil pencarian ruang: menampilkan kode, nama, luas, kapasitas, gedung,
	 * "digunakan oleh" (gabungan fakultas/jurusan atau yayasan/sekolah), dan keterangan, ditambah satu
	 * radio button pemilihan. Saat radio dicentang, ruang berkapasitas 0 ditolak (dianggap tidak layak
	 * dipakai) dan pemilihan dibatalkan; selain itu popup ditutup, entity {@code Ruang} terpilih
	 * disimpan sebagai attribute {@code "ruang"} pada Bandbox, teks tampilan diisi
	 * "{kode} - {nama}", lalu {@link #eventListener} (bila terpasang) diberi tahu — lihat pola callback
	 * di {@link ais.ui.util.GetEventListener}.
	 */
	class RuangRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ruang ruang = (Ruang) arg1;
			final MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(ruang.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (ruang.getKapasitasRuangan().equals(0)) {
						MyMessageboxConfig.show("Ruang dengan kapasitas peserta didik 0 tidak bisa digunakan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						checkbox.setChecked(false);
						return;
					}

					AmbilDataRuangBanbox.this.setOpen(false);
					AmbilDataRuangBanbox.this.setAttribute("ruang", ruang);
					AmbilDataRuangBanbox.this.setValue(ruang.getKodeRuangan() + " - " + ruang.getNama());
					// AmbilDataRuangBanbox.this.setId("ruang_" +
					// ruang.getId());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(ruang.getKodeRuangan()).setParent(arg0);
			new Label(ruang.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(ruang.getLuas()) + " m2").setParent(arg0);
			new Label(
					ruang.getKapasitasRuangan() == null ? "" : Common.numberFormat.get().format(ruang.getKapasitasRuangan()))
					.setParent(arg0);
			new Label(ruang.getGedung() == null ? "" : ruang.getGedung().getNama()).setParent(arg0);

			new Label(((ruang.getFakultas() == null ? "" : ruang.getFakultas().getNama())
					+ (ruang.getJurusan() == null ? "" : " / " + ruang.getJurusan().getNama()))
					+ (ruang.getYayasan() == null ? "" : ruang.getYayasan().getNama())
					+ (ruang.getSekolah() == null ? "" : " / " + ruang.getSekolah().getNama())).setParent(arg0);

			new Label(ruang.getKeterangan()).setParent(arg0);
		}

	}

	/**
	 * Membangun popup pencarian ruang (form filter kode/nama, gedung, dan fakultas/jurusan atau
	 * yayasan/sekolah sesuai jenis instansi + grid hasil dibungkus {@link
	 * org.zkoss.zul.Radiogroup}), lalu memanggil {@link #onSearchDefault(Event)} agar grid terisi
	 * saat popup pertama kali dibuka. Dipanggil sekali oleh listener {@code onOpen} pada konstruktor.
	 */
	public void display() {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1000px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Ruang");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gedung"));
		row.appendChild(gedung = new Combobox());
		Common.insertCombo(gedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		gedung.setWidth("90%");
		MyComboitemConfig comboitemSemua = new MyComboitemConfig("Semua");
		comboitemSemua.setValue(null);
		gedung.appendChild(comboitemSemua);
		gedung.setReadonly(true);
		if (gedung.getSelectedItem() == null) {
			gedung.setSelectedItem(comboitemSemua);
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, fakultas, jurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, yayasan, sekolah);

		if (pt) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(fakultas);
			fakultas.setWidth("90%");

			if (selectedFakultas != null) {
				Common.selectComboItem(true, this.fakultas, selectedFakultas);
			}

			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(jurusan);
			jurusan.setWidth("90%");

			if (selectedJurusan != null) {
				Common.selectComboItem(true, this.jurusan, selectedJurusan);
			}
			this.jurusan.setDisabled(selectedJurusan != null);
			this.fakultas.setDisabled(selectedFakultas != null);
		}

		if (ya) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
			row.appendChild(yayasan);
			yayasan.setWidth("90%");

			row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
			row.appendChild(sekolah);
			sekolah.setWidth("90%");
		}

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// BanboxFilterToggle.pasang merapatkan North ke 94px (heuristik "1 baris filter"),
		// padahal baris filter Ruang LEBAR (Kode/Nama + Gedung + Fakultas + Prodi) sehingga
		// tinggi 94px memotong toolbar → tombol "Cari" & "Bersihkan" tak terlihat. Kembalikan
		// tinggi North yang cukup SETELAH pasang agar toolbar tampil utuh.
		north.setHeight("130px");
		north.setAutoscroll(true);
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Pesan bila daftar ruang KOSONG (tidak ada ruang aktif / tak cocok filter) —
		 * agar popup tidak tampak "blank" tanpa keterangan (keluhan: "blank atau tidak ada
		 * datanya"). ZK Grid menampilkan pesan ini otomatis saat model kosong. */
		grid.setEmptyMessage("Belum ada data ruang yang aktif atau cocok dengan filter. "
				+ "Tambahkan/aktifkan data Ruang pada menu Master Ruang, atau ubah filter di atas.");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Ruang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Luas");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kap.");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gedung");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Digunakan oleh");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ket");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian ruang lewat {@link #cariRuang()} dan mengisi grid hasil. Bila pencarian
	 * gagal, grid diisi model kosong (memicu {@code emptyMessage}) alih-alih membiarkan popup blank,
	 * dan error dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param event event pemicu (boleh {@code null}, mis. saat dipanggil dari {@link #display()})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		try {
			cariRuang();
		} catch (Exception e) {
			// Jangan biarkan kegagalan pencarian meninggalkan popup blank tanpa keterangan:
			// tampilkan model kosong (grid memunculkan emptyMessage) + laporkan ke admin.
			try {
				if (grid != null) {
					grid.setRowRenderer(new RuangRenderer());
					grid.setModelCheckMobile(new SimpleListModel(new java.util.ArrayList<Ruang>()));
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataRuangBanbox.java:347");
			}
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menjalankan {@code Session.createCriteria(Ruang.class)} dengan filter status aktif, kode/nama,
	 * gedung, dan (bila diisi) fakultas/jurusan/yayasan/sekolah — masing-masing filter entity induk
	 * juga meloloskan baris dengan kolom NULL, hasil dibatasi {@link Common#MAX_RESULT_50} lewat
	 * {@link #pagingHelper}, lalu grid di-render ulang dengan {@link RuangRenderer}.
	 */
	@SuppressWarnings("unchecked")
	private void cariRuang() {

		Session session = HibernateUtil.currentSession();
		List<Ruang> ruang = pagingHelper.cariDenganCriteria(session.createCriteria(Ruang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("fakultas")).addOrder(Order.asc("jurusan"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("kodeRuangan", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(gedung.getSelectedItem() == null || gedung.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", gedung.getSelectedItem().getValue()))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)))

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)))

				.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sekolah"),
								CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false)))

				.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false)))

				.setMaxResults(Common.MAX_RESULT_50), Ruang.class);

		ListModel strset = new SimpleListModel(ruang);
		grid.setRowRenderer(new RuangRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * {@inheritDoc}
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Membatasi pencarian ke satu jurusan tertentu (dipanggil pemanggil sebelum popup dibuka, mis.
	 * saat Bandbox ini dipasang pada form yang sudah berkonteks jurusan). Combobox jurusan pada popup
	 * akan dinonaktifkan bila nilai ini diisi. Juga memanggil {@link Common#clear(org.zkoss.zk.ui.Component)}
	 * untuk membersihkan state Bandbox.
	 *
	 * @param jurusan jurusan yang membatasi hasil pencarian
	 */
	public void setJurusan(Jurusan jurusan) {
		Common.clear(this);
		this.selectedJurusan = jurusan;
	}

	/**
	 * Membatasi pencarian ke satu fakultas tertentu (dipanggil pemanggil sebelum popup dibuka).
	 * Combobox fakultas pada popup akan dinonaktifkan bila nilai ini diisi. Juga memanggil
	 * {@link Common#clear(org.zkoss.zk.ui.Component)} untuk membersihkan state Bandbox.
	 *
	 * @param fakultas fakultas yang membatasi hasil pencarian
	 */
	public void setFakultas(Fakultas fakultas) {
		Common.clear(this);
		this.selectedFakultas = fakultas;
	}
}
