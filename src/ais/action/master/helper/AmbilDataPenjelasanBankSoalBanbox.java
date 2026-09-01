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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.PenjelasanBankSoalAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PenjelasanBankSoal;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.PenjelasanBankSoal} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Penjelasan bank soal adalah teks pengantar/bacaan (judul + isi HTML) yang dipakai bersama oleh
 * sekelompok soal dalam bank soal (mis. teks bacaan untuk beberapa soal pemahaman sekaligus).
 * Popup pencarian menyediakan kriteria judul ({@code Textbox nama}, ilike sebagian) dan isi
 * ({@code Textbox searchisi}, ilike sebagian ke kolom {@code keterangan}), ditambah filter satuan
 * institusi yang tampil kondisional berdasarkan {@link ais.common.Common#chekPtAtauSekolah()}:
 * fakultas/prodi untuk konteks perguruan tinggi, yayasan/sekolah untuk konteks yayasan/sekolah.
 * Hasil selalu dibatasi ke data aktif ({@code aktif} null atau {@code true}), diurutkan id
 * menurun lalu nama menaik. Setiap baris grid menampilkan {@link ais.ui.util.MyDetail} yang
 * me-render isi ({@code keterangan}) sebagai HTML secara lazy saat expand, di samping radio
 * pilihan tunggal ({@link org.zkoss.zul.Radiogroup}). Toolbar menambahkan tombol "Tambah Grup
 * Soal/Penjelasan" yang membuka form tambah baru lewat
 * {@link ais.action.master.PenjelasanBankSoalAction#onAddExternal}; hasil simpanan langsung
 * dipilihkan ke Bandbox ini (memicu {@link #eventListener}) dan grid dimuat ulang. Grid hasil
 * memakai mold "paging" client-side (bukan {@code AmbilDataPagingHelper} — field itu dideklarasikan
 * tapi tidak dipakai di file ini) dibatasi {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataPenjelasanBankSoalBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Catatan: field ini dideklarasikan tapi tidak dipakai secara aktif di file ini — grid hasil
	 * pencarian di display() memakai mold "paging" client-side, bukan AmbilDataPagingHelper. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor standar pola Bandbox picker: kunci input jadi read-only dan pasang listener
	 * {@code onOpen} yang membangun popup pencarian secara lazy pada pembukaan pertama, lalu
	 * membuka popup lewat {@link Common#createDefaultTimer}. Lihat
	 * {@link ais.ui.util.GetEventListener} untuk penjelasan lengkap kerangka ini.
	 */
	public AmbilDataPenjelasanBankSoalBanbox() {
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
	private Textbox searchisi;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	/**
	 * Renderer baris grid hasil pencarian: menampilkan {@link ais.ui.util.MyDetail} yang me-render
	 * isi ({@code keterangan}) sebagai {@link org.zkoss.zul.Html} secara lazy saat baris di-expand
	 * (hanya dibangun sekali, {@code detail.getChildren().isEmpty()}), diikuti radio pilihan dan
	 * label nama/judul. Saat radio dicentang ({@code onCheck}), popup ditutup, entity
	 * {@link PenjelasanBankSoal} terpilih disimpan sebagai attribute {@code "penjelasanBankSoal"}
	 * pada Bandbox, teks Bandbox diisi nama/judulnya, lalu {@link #eventListener} (bila terpasang)
	 * diberi tahu — lihat pola callback selengkapnya di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPenjelasanBankSoalBanbox
	 */
	class PenjelasanBankSoalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (detail.getChildren().isEmpty()) {
						detail.appendChild(new Html(penjelasanBankSoal.getKeterangan()));
					}
				}
			});

			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(penjelasanBankSoal.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPenjelasanBankSoalBanbox.this.setOpen(false);
					AmbilDataPenjelasanBankSoalBanbox.this.setAttribute("penjelasanBankSoal", penjelasanBankSoal);
					AmbilDataPenjelasanBankSoalBanbox.this.setValue(penjelasanBankSoal.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(penjelasanBankSoal.getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria judul/isi/fakultas-prodi/yayasan-sekolah + tombol
	 * Cari + tombol "Tambah Grup Soal/Penjelasan" + grid hasil berbungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat popup pertama kali dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi. Visibilitas blok kriteria
	 * fakultas/prodi vs yayasan/sekolah ditentukan oleh {@link Common#chekPtAtauSekolah()}. Tombol
	 * tambah membuka form buat-baru {@link PenjelasanBankSoal} lewat
	 * {@link PenjelasanBankSoalAction#onAddExternal}; hasil simpanannya langsung dipilihkan ke
	 * Bandbox ini dan grid dimuat ulang.
	 */
	public void display() {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Parameter");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Isi"));
		row.appendChild(searchisi = new Textbox());
		searchisi.setWidth("90%");

		MyFormRow hbFakultasLabel = new MyFormRow();
		hbFakultasLabel.setParent(rows);

		hbFakultasLabel.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		hbFakultasLabel.appendChild(searchfakultas = new Combobox());
		searchfakultas.setWidth("90%");

		hbFakultasLabel.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		hbFakultasLabel.appendChild(searchjurusan = new Combobox());
		searchjurusan.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);

		MyFormRow hbYayasan = new MyFormRow();
		hbYayasan.setParent(rows);

		hbYayasan.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		hbYayasan.appendChild(searchyayasan = new Combobox());
		searchyayasan.setWidth("90%");

		hbYayasan.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		hbYayasan.appendChild(searchsekolah = new Combobox());
		searchsekolah.setWidth("90%");

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);
		hbYayasan.setVisible(ya);

		Toolbar toolbar = new Toolbar();
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

		toolbar.appendChild(Common.createCleanButton(this, this));

		button = new MyToolbarbuttonConfig("Tambah Grup Soal/Penjelasan", "/img/new.gif");
		button.setTooltiptext("Tambah Grup Soal/Penjelasan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				PenjelasanBankSoal penjelasanBankSoal = new PenjelasanBankSoal();

				PenjelasanBankSoalAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) arg0.getData();
						AmbilDataPenjelasanBankSoalBanbox.this.setOpen(false);
						AmbilDataPenjelasanBankSoalBanbox.this.setAttribute("penjelasanBankSoal", penjelasanBankSoal);
						AmbilDataPenjelasanBankSoalBanbox.this.setAttribute("myValue", penjelasanBankSoal);
						AmbilDataPenjelasanBankSoalBanbox.this.setValue(penjelasanBankSoal.getNama());
						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
						onSearchDefault(arg0);
					}
				}, penjelasanBankSoal);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link PenjelasanBankSoal} berdasarkan kriteria pada form:
	 * jurusan/fakultas/sekolah/yayasan (eq berdasar id lewat
	 * {@link CommonSearchFilterHelper#eqSelectedWithId}, no-op bila belum dipilih), judul dan isi
	 * (ilike sebagian ke {@code nama} dan {@code keterangan}), selalu dibatasi ke data aktif
	 * ({@code aktif} null atau {@code true}), diurutkan id menurun lalu nama menaik. Hasil dipasang
	 * ke {@link #grid} lewat {@link PenjelasanBankSoalRenderer} dan dibatasi
	 * {@link Common#MAX_RESULT} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, dipakai juga sebagai pengisi awal/ulang grid)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<PenjelasanBankSoal> penjelasanBankSoal = session.createCriteria(PenjelasanBankSoal.class)

				.addOrder(Order.desc("id"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchisi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchisi.getValue().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(penjelasanBankSoal);
		ListModel strset = new SimpleListModel(penjelasanBankSoal);
		grid.setRowRenderer(new PenjelasanBankSoalRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah pengguna memilih (atau menambah baru) satu baris
	 * penjelasan bank soal.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Mengambil listener yang sedang terpasang.
	 *
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
