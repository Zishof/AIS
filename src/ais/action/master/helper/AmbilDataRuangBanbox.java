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
 * Tipe khusus untuk ambil data ruang banbox. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code Textbox nama},
 * {@code Combobox gedung}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code Jurusan selectedJurusan};
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code cariRuang()}, {@code setEventListener()}, {@code
 * getEventListener()}); mutasi data ({@code setJurusan()}, {@code setFakultas()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
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
	 * Renderer lokal untuk layar/komponen {@link AmbilDataRuangBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataRuangBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataRuangBanbox
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

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setJurusan(Jurusan jurusan) {
		Common.clear(this);
		this.selectedJurusan = jurusan;
	}

	public void setFakultas(Fakultas fakultas) {
		Common.clear(this);
		this.selectedFakultas = fakultas;
	}
}
