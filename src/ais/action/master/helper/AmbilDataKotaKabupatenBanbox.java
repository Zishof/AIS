package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Wilayah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyPanel;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Wilayah} (tingkat
 * kota/kabupaten) — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Sama seperti {@link AmbilDataKecamatanBanbox}, entity yang dicari adalah
 * {@link ais.database.model.Wilayah} — model hierarki wilayah administratif self-referencing lewat
 * {@code wilayahInduk} — tapi kelas ini LEBIH SEDERHANA: level dikunci hardcode ke {@code "2"}
 * (kota/kabupaten), tidak ada constructor dengan level dinamis maupun fitur tambah cepat modal
 * berjenjang. Popup pencarian menyediakan field {@code nama} (nama kota/kab., ilike substring) dan
 * {@code namaProp} (nama propinsi induk, ilike substring lewat join alias {@code prop}), dengan
 * layout yang menyesuaikan tampilan mobile ({@link ais.common.Common#isMobile()}). Pemilihan
 * bersifat TUNGGAL (Radiogroup). Tidak ada constructor dengan parameter tambahan; field
 * {@code pagingHelper} dideklarasikan tapi TIDAK dipakai — pencarian masih memakai
 * {@code grid.setMold("paging")} client-side lama dibatasi {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKotaKabupatenBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor standar: memasang listener {@code onOpen} yang membangun popup pencarian secara
	 * lazy pada pembukaan pertama. Mengikuti kerangka standar di
	 * {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus entity ini.
	 */
	public AmbilDataKotaKabupatenBanbox() {
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

	/** Kriteria pencarian: nama kota/kabupaten (ilike, substring). */
	private Textbox nama;
	/** Kriteria pencarian: nama propinsi induk (ilike, substring, lewat join alias {@code prop}). */
	private Textbox namaProp;

	/**
	 * Renderer baris grid hasil pencarian {@link Wilayah} tingkat kota/kabupaten: menampilkan nama
	 * dan propinsi induk, plus satu radio button pilihan. Mengikuti kerangka renderer standar di
	 * {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup popup, menyimpan
	 * entity terpilih ke atribut {@code "wilayah"} dan teks tampilan {@code wilayah.getNama()},
	 * lalu meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataKotaKabupatenBanbox
	 */
	class WilayahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Wilayah wilayah = (Wilayah) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(wilayah.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKotaKabupatenBanbox.this.setOpen(false);
					AmbilDataKotaKabupatenBanbox.this.setAttribute("wilayah", wilayah);
					AmbilDataKotaKabupatenBanbox.this.setValue(wilayah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(wilayah.getNama()).setParent(arg0);

			final Label prop = new Label(wilayah.getWilayahInduk() == null ? "" : wilayah.getWilayahInduk().getNama());
			prop.setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Wilayah} tingkat kota/kabupaten sekali (dipanggil lazy dari
	 * listener {@code onOpen}): form dengan field nama dan propinsi, tombol Cari, dan grid hasil
	 * dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Layout menyesuaikan tampilan
	 * mobile ({@link ais.common.Common#isMobile()}). Mengikuti kerangka {@code display()} standar
	 * — lihat {@link ais.ui.util.GetEventListener}. Memanggil {@link #onSearchDefault(Event)} di
	 * akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {

		boolean mobile = Common.isMobile();

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth(mobile ? "97%" : "600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kota / Kabupaten");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kota/Kab."));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		if (mobile) {
			row = new MyFormRow();
			row.setParent(rows);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Prop."));
		row.appendChild(namaProp = new Textbox());
		namaProp.setWidth("90%");

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
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Propinsi");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link Wilayah} dengan {@code level} dikunci ke {@code "2"} (kota/
	 * kabupaten), filter {@code nama} dan {@code namaProp} (keduanya ilike substring, {@code
	 * namaProp} lewat join alias {@code prop} ke {@code wilayahInduk}). Diurutkan menaik berdasar
	 * nama, dibatasi {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link WilayahRenderer}
	 * dan model hasil ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Wilayah> wilayah = session.createCriteria(Wilayah.class)

				.createAlias("wilayahInduk", "prop", Criteria.LEFT_JOIN)

				.addOrder(Order.asc("nama")).add(Restrictions.eq("level", "2"))

				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(namaProp.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("prop.nama", namaProp.getText().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(wilayah);
		ListModel strset = new SimpleListModel(wilayah);
		grid.setRowRenderer(new WilayahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}
