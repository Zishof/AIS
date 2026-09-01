package ais.action.master.helper;

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
import ais.database.model.Wilayah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Wilayah} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Wilayah} adalah entity wilayah administratif berjenjang (level 1 = propinsi, level di
 * bawahnya dipakai picker lain seperti {@code AmbilDataKecamatanBanbox}); kelas ini secara khusus
 * membatasi pencarian ke {@code Restrictions.eq("level", "1")} sehingga hanya menampilkan
 * propinsi. Kriteria pencarian hanya nama ({@code Textbox nama}, ilike sebagian), hasil diurutkan
 * nama menaik dan dibatasi {@link ais.common.Common#MAX_RESULT} baris. Lebar popup menyesuaikan
 * perangkat lewat {@link ais.common.Common#isMobile()} (97% pada mobile, 400px pada desktop).
 * Pemilihan bersifat tunggal lewat {@link org.zkoss.zul.Radiogroup}; nilai Bandbox diisi langsung
 * dari nama propinsi.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataPropinsiBanbox extends Bandbox implements GetEventListener {

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
	public AmbilDataPropinsiBanbox() {
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

	/**
	 * Renderer baris grid hasil pencarian: menampilkan radio button pilihan diikuti label nama
	 * propinsi. Saat radio dicentang ({@code onCheck}), popup ditutup, entity {@link Wilayah}
	 * terpilih disimpan sebagai attribute {@code "wilayah"} pada Bandbox, teks Bandbox diisi
	 * namanya, lalu {@link #eventListener} (bila terpasang) diberi tahu — lihat pola callback
	 * selengkapnya di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPropinsiBanbox
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
					AmbilDataPropinsiBanbox.this.setOpen(false);
					AmbilDataPropinsiBanbox.this.setAttribute("wilayah", wilayah);
					AmbilDataPropinsiBanbox.this.setValue(wilayah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(wilayah.getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria nama + tombol Cari + grid hasil berbungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat popup pertama kali dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi. Lebar popup menyesuaikan
	 * perangkat lewat {@link Common#isMobile()}.
	 */
	public void display() {

		boolean mobile = Common.isMobile();

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth(mobile ? "97%" : "400px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Propinsi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Prop."));
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

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link Wilayah} yang dibatasi ke {@code level == "1"} (propinsi),
	 * ditambah kriteria nama (ilike sebagian terhadap teks pada form, kosong berarti cocok ke
	 * semua), diurutkan nama menaik. Hasil dipasang ke {@link #grid} lewat
	 * {@link WilayahRenderer} dan dibatasi {@link Common#MAX_RESULT} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, dipakai juga sebagai pengisi awal grid saat
	 *              popup pertama dibuka)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Wilayah> wilayah = session.createCriteria(Wilayah.class)

				.addOrder(Order.asc("nama")).add(Restrictions.eq("level", "1"))

				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(wilayah);
		ListModel strset = new SimpleListModel(wilayah);
		grid.setRowRenderer(new WilayahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah pengguna memilih satu baris propinsi.
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
