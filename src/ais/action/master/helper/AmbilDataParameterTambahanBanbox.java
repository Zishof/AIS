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
import ais.database.model.GrupParameterTambahan;
import ais.database.model.ParameterTambahan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.ParameterTambahan} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Parameter tambahan adalah atribut/field kustom (kode, nama, tipe data inputan, nilai) yang
 * dikelompokkan lewat {@link ais.database.model.GrupParameterTambahan}. Popup pencarian
 * menyediakan kriteria nama (ilike sebagian, {@code Textbox nama}) dan grup
 * ({@code Combobox searchgrup}), ditambah filter satuan institusi yang tampil kondisional
 * berdasarkan {@link ais.common.Common#chekPtAtauSekolah()}: kombinasi fakultas/prodi
 * ({@code searchfakultas}/{@code searchjurusan}) hanya terlihat pada konteks perguruan tinggi
 * (dan bila pilihan fakultas tersedia), sedangkan kombinasi yayasan/sekolah
 * ({@code searchyayasan}/{@code searchsekolah}) hanya terlihat pada konteks yayasan/sekolah.
 * Hasil pencarian selalu dibatasi ke parameter yang aktif ({@code aktif} null atau {@code true})
 * dan diurutkan berdasarkan nama. Pemilihan bersifat tunggal lewat
 * {@link org.zkoss.zul.Radiogroup}; nilai teks yang disimpan ke Bandbox adalah gabungan kode dan
 * nama ({@code kode + "-" + nama}). Grid hasil memakai mold "paging" client-side (bukan
 * {@code AmbilDataPagingHelper} — field itu dideklarasikan tapi tidak dipakai di file ini)
 * dibatasi {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataParameterTambahanBanbox extends Bandbox implements GetEventListener {

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
	public AmbilDataParameterTambahanBanbox() {
		super();

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
	private Combobox searchgrup;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	/**
	 * Renderer baris grid hasil pencarian: menampilkan radio button pilihan diikuti kolom kode,
	 * nama, tipe data inputan, nama grup (kosong bila tanpa grup), dan nilai. Saat radio dicentang
	 * ({@code onCheck}), popup ditutup, entity {@link ParameterTambahan} terpilih disimpan sebagai
	 * attribute {@code "parameterTambahan"} pada Bandbox, teks Bandbox diisi
	 * {@code kode + "-" + nama}, lalu {@link #eventListener} (bila terpasang) diberi tahu — lihat
	 * pola callback selengkapnya di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataParameterTambahanBanbox
	 */
	class ParameterTambahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahan parameterTambahan = (ParameterTambahan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(parameterTambahan.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataParameterTambahanBanbox.this.setOpen(false);
					AmbilDataParameterTambahanBanbox.this.setAttribute("parameterTambahan", parameterTambahan);
					AmbilDataParameterTambahanBanbox.this
							.setValue(parameterTambahan.getKode() + "-" + parameterTambahan.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(parameterTambahan.getKode()).setParent(arg0);
			new Label(parameterTambahan.getNama()).setParent(arg0);
			new Label(parameterTambahan.getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahan.getGrupParameterTambahan() == null ? ""
					: parameterTambahan.getGrupParameterTambahan().getNama()).setParent(arg0);

			new Label(parameterTambahan.getNilaiDataInputan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria nama/grup/fakultas-prodi/yayasan-sekolah + tombol
	 * Cari + grid hasil berbungkus {@link org.zkoss.zul.Radiogroup}) sekali saat popup pertama kali
	 * dibuka, lalu memanggil {@link #onSearchDefault(Event)} agar grid langsung terisi. Visibilitas
	 * blok kriteria fakultas/prodi vs yayasan/sekolah ditentukan oleh
	 * {@link Common#chekPtAtauSekolah()}.
	 */
	public void display() {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Grup"));
		row.appendChild(searchgrup = new Combobox());
		searchgrup.setWidth("90%");
		Common.insertComboDanSemua(searchgrup, "nama", GrupParameterTambahan.class);

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
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Grup");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("35%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link ParameterTambahan} berdasarkan kriteria pada form: grup (eq),
	 * jurusan/fakultas/sekolah/yayasan (eq berdasar id lewat
	 * {@link CommonSearchFilterHelper#eqSelectedWithId}, no-op bila belum dipilih), nama (ilike
	 * sebagian), selalu dibatasi ke parameter aktif ({@code aktif} null atau {@code true}) dan
	 * diurutkan berdasarkan nama. Hasil dipasang ke {@link #grid} lewat
	 * {@link ParameterTambahanRenderer} dan dibatasi {@link Common#MAX_RESULT} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, dipakai juga sebagai pengisi awal grid saat
	 *              popup pertama dibuka)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<ParameterTambahan> parameterTambahan = session.createCriteria(ParameterTambahan.class)

				.add(searchgrup.getSelectedItem() == null || searchgrup.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("grupParameterTambahan", searchgrup.getSelectedItem().getValue()))

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
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(parameterTambahan);
		ListModel strset = new SimpleListModel(parameterTambahan);
		grid.setRowRenderer(new ParameterTambahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah pengguna memilih satu baris parameter tambahan.
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
