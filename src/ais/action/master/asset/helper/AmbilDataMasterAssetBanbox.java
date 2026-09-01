package ais.action.master.asset.helper;

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
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
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
import ais.database.model.asset.MasterAsset;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.asset.MasterAsset} — lihat {@link ais.ui.util.GetEventListener} untuk
 * arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback). {@code
 * MasterAsset} adalah master data katalog jenis barang/jasa sarana-prasarana (mis. "Kursi Kuliah
 * Merk X", "Jasa Servis AC") — berbeda dari {@link ais.database.model.asset.AssetDetail} yang
 * merupakan satu unit fisik/barcode dari suatu {@code MasterAsset}.
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}/{@link Radio}) dengan filter "Kode"
 * dan "Nama" (keduanya ILIKE ANYWHERE), ditambah filter tetap opsional {@link #tipe} yang
 * ditentukan lewat constructor (mis. membatasi popup hanya menampilkan barang, atau hanya jasa —
 * lihat konstanta tipe pada {@link MasterAsset}). Kolom grid menampilkan kode, nama, merk, jenis,
 * tipe, dan kelompok asset. Tidak ada filter scoping satuan kerja (katalog master bersifat global).
 *
 * @see Bandbox
 */
public class AmbilDataMasterAssetBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private String tipe = null;

	/**
	 * Membangun komponen: memasang mode read-only standar dan listener {@code onOpen} yang
	 * membangun popup ({@link #display()}) hanya pada pembukaan pertama, mengikuti kerangka umum
	 * di {@link ais.ui.util.GetEventListener}.
	 *
	 * @param tipe filter tetap kolom {@code tipe} pada {@link MasterAsset} (mis. barang/jasa),
	 *             atau {@code null} untuk menampilkan semua tipe
	 */
	public AmbilDataMasterAssetBanbox(String tipe) {
		super();
		this.tipe = tipe;
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
	private Textbox kode;

	/**
	 * Merender satu baris grid: radio pilih berlabel kode, nama, merk, jenis, tipe, dan kelompok
	 * asset. Memilih baris menutup popup, menyimpan entity {@link MasterAsset} terpilih ke
	 * attribute {@code "masterAsset"} pada Bandbox, mengisi teks tampilan dengan
	 * {@code masterAsset.toString()}, lalu memicu {@link #eventListener} bila terpasang —
	 * mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataMasterAssetBanbox
	 */
	class MasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasterAsset masterAsset = (MasterAsset) arg1;
			Radio checkbox = new Radio(masterAsset.getKode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(masterAsset.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMasterAssetBanbox.this.setOpen(false);
					AmbilDataMasterAssetBanbox.this.setAttribute("masterAsset", masterAsset);
					AmbilDataMasterAssetBanbox.this.setValue(masterAsset.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(masterAsset.getNama()).setParent(arg0);
			new Label(masterAsset.getMerk()).setParent(arg0);
			new Label(masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()).setParent(arg0);
			new MyLabelConfig(masterAsset.getTipe()).setParent(arg0);
			new Label(masterAsset.getKelompokAsset() == null ? "" : masterAsset.getKelompokAsset().getNama())
					.setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Kode/Nama,
	 * grid hasil bermold "paging" (kolom kode, nama, merk, jenis, tipe, kelompok), lalu memuat
	 * data awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Barang dan Jasa");
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
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Merk");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		onSearchDefault(null);

	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian {@link MasterAsset}: cocok kode (ILIKE
	 * ANYWHERE), cocok nama (ILIKE ANYWHERE), dan filter tetap {@link #tipe} bila diisi lewat
	 * constructor; diurutkan menaik berdasarkan nama, dibatasi {@link Common#MAX_RESULT_500}
	 * baris. Mengisi ulang grid dengan hasilnya beserta {@link MasterAssetRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<MasterAsset> masterAsset = session.createCriteria(MasterAsset.class).addOrder(Order.asc("nama"))

						.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))
						.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

						.add(tipe == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tipe", tipe))

						.setMaxResults(Common.MAX_RESULT_500)

						.list();

//		System.out.println(masterAsset);
		ListModel strset = new SimpleListModel(masterAsset);
		grid.setRowRenderer(new MasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu master asset */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan master asset yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
