package ais.action.master.sekolah.helper;


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

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.sekolah.KelasSiswa}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback).
 *
 * <p>
 * {@code KelasSiswa} merepresentasikan satu rombongan belajar/kelas pada modul sekolah (mis. "7A",
 * "XII IPA 1") — dipilih di sini secara bebas lintas sekolah dan tingkat (tanpa parameter filter
 * dari entity induk pada constructor), sehingga popup menampilkan seluruh kelas yang cocok dengan
 * kriteria pencarian, bukan dibatasi ke satu sekolah/kelas tertentu seperti pada varian sejenis yang
 * menerima parameter pembatas di constructor-nya. Popup pencarian menyediakan tiga kriteria:
 * {@code nama} kelas (ilike, tombol Enter langsung mencari), {@code tahunAkademik} (Combobox hasil
 * {@link ais.common.Common#generateTahunAjaran}, filter {@code eq} pada field {@code tahunAjaran}),
 * dan {@code searchsekolah} (Combobox berisi semua {@link ais.database.model.sekolah.Sekolah} plus
 * opsi "Semua" dari {@link ais.common.Common#insertComboDanSemua}; bila konteks sekolah user sudah
 * diketahui lewat {@link ais.action.master.sekolah.util.SekolahUtil#getSekolah()}, combo ini
 * otomatis dipilih dan dikunci/disabled). Setiap perubahan kriteria (onChange/onOK) langsung memicu
 * pencarian ulang. Query dasar hanya menyaring kelas berstatus aktif ({@code aktif} null atau true)
 * dan diurutkan berdasar sekolah, tingkat, lalu nama; hasil ditampilkan sebagai grid dengan pilihan
 * TUNGGAL via radio button (dibungkus {@link org.zkoss.zul.Radiogroup}, komponen pilihan per baris
 * {@link ais.ui.util.MyRadioConfig}). Memilih satu baris menutup popup dan mengisi atribut
 * {@code kelasSiswa} dan {@code kelas} pada instance Bandbox ini dengan entity {@code KelasSiswa}
 * yang dipilih.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKelasSiswaSemuaBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Constructor standar pola Bandbox picker: {@code readonly}, popup dibangun lazy pada
	 * {@code onOpen} pertama via {@link #display()}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public AmbilDataKelasSiswaSemuaBanbox() {

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
	private Combobox searchsekolah = new Combobox();
	private Combobox tahunAkademik;

	/**
	 * Renderer baris grid popup untuk {@link ais.database.model.sekolah.KelasSiswa}: menampilkan
	 * nama kelas, ruang, tingkat, sekolah (atau label "Semua" bila kelas tidak terikat sekolah
	 * tertentu), kurikulum, dan wali/guru pembina. Memilih radio pada suatu baris menutup popup,
	 * menyimpan entity terpilih ke atribut {@code kelasSiswa}/{@code kelas} serta teks tampilan pada
	 * Bandbox induk, lalu memicu {@link #eventListener} pemanggil — lihat
	 * {@link ais.ui.util.GetEventListener} untuk pola callback ini.
	 *
	 * @see AmbilDataKelasSiswaSemuaBanbox
	 */
	class KelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswa kelas = (KelasSiswa) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelasSiswaSemuaBanbox.this.setOpen(false);
					AmbilDataKelasSiswaSemuaBanbox.this.setAttribute("kelasSiswa", kelas);
					AmbilDataKelasSiswaSemuaBanbox.this.setAttribute("kelas", kelas);
					AmbilDataKelasSiswaSemuaBanbox.this.setValue(kelas.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kelas.getNama()).setParent(arg0);
			new Label(kelas.getRuang() == null ? "" : kelas.getRuang().getNama()).setParent(arg0);
			new Label(kelas.getTingkat() + "").setParent(arg0);
			new Label(kelas.getSekolah() == null ? "Semua" : kelas.getSekolah().getNama() + "").setParent(arg0);
			new Label(kelas.getKurikulumSekolah() == null ? "" : kelas.getKurikulumSekolah().getNama()).setParent(arg0);
			new Label(kelas.getGuruPembina() == null ? "" : kelas.getGuruPembina().getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form nama/tahun akademik/sekolah + grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat pertama dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("950px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kelas Siswa");
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
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new MyLabelConfig("TA : "));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		Common.insertComboDanSemua(searchsekolah, "nama", Sekolah.class);

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null) {
			Common.selectComboItem(searchsekolah, sekolah);
			searchsekolah.setDisabled(true);
		}

		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
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
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tingkat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kurikulum");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Wali Kelas");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link ais.database.model.sekolah.KelasSiswa} aktif berdasarkan
	 * {@code nama} (ilike), {@code tahunAkademik} (eq), dan {@code searchsekolah} (eq id sekolah,
	 * opsional bila "Semua" dipilih), diurutkan sekolah/tingkat/nama, lalu memasang
	 * {@link KelasSiswaRenderer} ke {@link #grid}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<KelasSiswa> kelas = session.createCriteria(KelasSiswa.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("sekolah")).addOrder(Order.asc("tingkat")).addOrder(Order.asc("nama"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						|| tahunAkademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.setMaxResults(Common.MAX_RESULT_1000)

				.list();
		ListModel strset = new SimpleListModel(kelas);
		grid.setRowRenderer(new KelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @see ais.ui.util.GetEventListener#setEventListener(EventListener) */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @see ais.ui.util.GetEventListener#getEventListener() */
	public EventListener getEventListener() {
		return eventListener;
	}
}
