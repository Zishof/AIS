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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Matapelajaran;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.sekolah.Matapelajaran}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback).
 *
 * <p>
 * {@code Matapelajaran} adalah entity master mata pelajaran pada modul sekolah (mis. "Matematika",
 * "Bahasa Inggris"), masing-masing memiliki kode, nama, KKM (Kriteria Ketuntasan Minimal), dan
 * terikat ke satu {@link ais.database.model.sekolah.Sekolah}. Constructor terlebih dulu mengisi
 * combo {@code searchyayasan}/{@code searchsekolah} dengan seluruh yayasan/sekolah plus opsi
 * "Semua" lewat {@link ais.common.Common#initYayasanDanSekolahDanSemua} sebelum memasang listener
 * {@code onOpen} standar. Popup pencarian menyediakan empat kriteria: {@code kodeMatapelajaranan}
 * (kode, ilike), {@code nama} (ilike), {@code searchyayasan}, dan {@code searchsekolah} (keduanya
 * filter {@code eq} id, no-op bila "Semua" dipilih) — berbeda dari sebagian besar picker sejenis,
 * di sini pencarian HANYA dipicu lewat tombol "Cari" (tidak ada listener onOK/onChange otomatis per
 * field). Query dasar menyaring mata pelajaran berstatus aktif ({@code aktif} null atau true),
 * diurutkan berdasar nama. Hasil ditampilkan sebagai grid dengan pilihan TUNGGAL via radio button
 * (dibungkus {@link org.zkoss.zul.Radiogroup}, komponen pilihan per baris
 * {@link ais.ui.util.MyRadioConfig}). Memilih satu baris menutup popup, mengisi atribut
 * {@code matapelajaran} pada instance Bandbox ini dengan entity terpilih, dan menampilkan teks
 * gabungan {@code kode - nama} sebagai nilai Bandbox.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMatapelajaranBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Mengisi combo yayasan/sekolah (plus opsi "Semua") lewat
	 * {@link ais.common.Common#initYayasanDanSekolahDanSemua}, lalu memasang constructor standar
	 * pola Bandbox picker: {@code readonly}, popup dibangun lazy pada {@code onOpen} pertama via
	 * {@link #display()}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public AmbilDataMatapelajaranBanbox() {
		super();
		setReadonly(true);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

	private Textbox kodeMatapelajaranan;
	private Textbox nama;
	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	/**
	 * Renderer baris grid popup untuk {@link ais.database.model.sekolah.Matapelajaran}: menampilkan
	 * kode (dengan id), nama, KKM, dan sekolah pemilik. Memilih radio pada suatu baris menutup
	 * popup, menyimpan entity terpilih ke atribut {@code matapelajaran} serta teks tampilan
	 * {@code kode - nama} pada Bandbox induk, lalu memicu {@link #eventListener} pemanggil — lihat
	 * {@link ais.ui.util.GetEventListener} untuk pola callback ini.
	 *
	 * @see AmbilDataMatapelajaranBanbox
	 */
	class MatapelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matapelajaran matapelajaran = (Matapelajaran) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(matapelajaran.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMatapelajaranBanbox.this.setOpen(false);
					AmbilDataMatapelajaranBanbox.this.setAttribute("matapelajaran", matapelajaran);
					AmbilDataMatapelajaranBanbox.this
							.setValue(matapelajaran.getKode() + " - " + matapelajaran.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(matapelajaran.getKode() + " (" + matapelajaran.getId() + ") ").setParent(arg0);
			new Label(matapelajaran.getNama()).setParent(arg0);
			new Label(matapelajaran.getKkm() + "").setParent(arg0);
			new Label(matapelajaran.getSekolah() == null ? "" : matapelajaran.getSekolah().getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kode/nama/yayasan/sekolah + grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup}, pencarian dipicu tombol "Cari") sekali saat pertama
	 * dibuka, lalu memanggil {@link #onSearchDefault(Event)} agar grid langsung terisi.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Matapelajaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMatapelajaranan = new Textbox());
		kodeMatapelajaranan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

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
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("KKM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");
		column.setWidth("25%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link ais.database.model.sekolah.Matapelajaran} aktif berdasarkan
	 * {@code kodeMatapelajaranan} (ilike), {@code nama} (ilike), {@code searchsekolah}, dan
	 * {@code searchyayasan} (keduanya eq id, opsional bila "Semua" dipilih), diurutkan nama, lalu
	 * memasang {@link MatapelajaranRenderer} ke {@link #grid}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Matapelajaran> matapelajaran = session.createCriteria(Matapelajaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeMatapelajaranan.getText().trim(), MatchMode.ANYWHERE))
				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(matapelajaran);
		grid.setRowRenderer(new MatapelajaranRenderer());
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
