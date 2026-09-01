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
import org.zkoss.zul.Columns;
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
import ais.database.model.PerguruanTinggiLain;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.PerguruanTinggiLain} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Perguruan tinggi lain merepresentasikan institusi pendidikan tinggi DI LUAR institusi pengguna
 * sendiri (mis. asal kampus mahasiswa pindahan/transfer, riwayat pendidikan sebelumnya) — berbeda
 * dari entity perguruan tinggi milik sendiri. Popup pencarian menyediakan kriteria kode
 * ({@code Textbox kode}, cocok ke {@code kodePerguruanTinggi}) dan nama ({@code Textbox nama}),
 * keduanya dicocokkan {@code ilike} tanpa penjagaan kosong eksplisit (nilai kosong menghasilkan
 * pola {@code "%%"} yang tetap cocok ke semua baris, efeknya sama dengan idiom
 * {@code isEmpty() ? sqlRestriction("1=1") : ilike(...)} yang dipakai file lain, hanya ditulis
 * lebih ringkas). Hasil selalu dibatasi ke data aktif ({@code aktif} true atau null), diurutkan
 * nama menaik, dan dibatasi {@link ais.common.Common#MAX_RESULT_50} baris (bukan
 * {@code MAX_RESULT}). Pemilihan bersifat tunggal lewat {@link org.zkoss.zul.Radiogroup}. Selain
 * constructor tanpa parameter, tersedia constructor {@code AmbilDataPerguruanTinggiLainBanbox(Boolean
 * notDeafault)} — parameter ini TIDAK dipakai di badan constructor manapun (murni penanda overload,
 * bukan filter fungsional); constructor tanpa parameter meneruskannya dengan nilai {@code true}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataPerguruanTinggiLainBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Catatan: field ini dideklarasikan tapi tidak dipakai secara aktif di file ini — grid hasil
	 * pencarian di display() memakai mold "paging" client-side, bukan AmbilDataPagingHelper. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Constructor default, meneruskan ke {@link #AmbilDataPerguruanTinggiLainBanbox(Boolean)}
	 * dengan nilai {@code true} (parameter tersebut tidak dipakai di badan constructor).
	 */
	public AmbilDataPerguruanTinggiLainBanbox() {
		this(true);
	}

	/**
	 * Constructor standar pola Bandbox picker: kunci input jadi read-only dan pasang listener
	 * {@code onOpen} yang membangun popup pencarian secara lazy pada pembukaan pertama, lalu
	 * membuka popup lewat {@link Common#createDefaultTimer}. Lihat
	 * {@link ais.ui.util.GetEventListener} untuk penjelasan lengkap kerangka ini.
	 *
	 * @param notDeafault tidak dipakai di badan constructor ini; hanya penanda overload
	 */
	public AmbilDataPerguruanTinggiLainBanbox(Boolean notDeafault) {
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

	private Textbox kode;
	private Textbox nama;

	/**
	 * Renderer baris grid hasil pencarian: menampilkan radio button pilihan diikuti kolom kode dan
	 * nama perguruan tinggi. Saat radio dicentang ({@code onCheck}), popup ditutup, entity
	 * {@link PerguruanTinggiLain} terpilih disimpan sebagai attribute {@code "perguruanTinggiLain"}
	 * (dan {@code "myValue"}) pada Bandbox, teks Bandbox diisi namanya, lalu {@link #eventListener}
	 * (bila terpasang) diberi tahu — lihat pola callback selengkapnya di
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPerguruanTinggiLainBanbox
	 */
	class PerguruanTinggiLainRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PerguruanTinggiLain perguruanTinggiLain = (PerguruanTinggiLain) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(perguruanTinggiLain.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPerguruanTinggiLainBanbox.this.setOpen(false);
					AmbilDataPerguruanTinggiLainBanbox.this.setAttribute("perguruanTinggiLain", perguruanTinggiLain);
					AmbilDataPerguruanTinggiLainBanbox.this.setAttribute("myValue", perguruanTinggiLain);
					AmbilDataPerguruanTinggiLainBanbox.this.setValue(perguruanTinggiLain.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(perguruanTinggiLain.getKodePerguruanTinggi()).setParent(arg0);

			new Label(perguruanTinggiLain.getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria kode/nama + tombol Cari + grid hasil berbungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat popup pertama kali dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi.
	 */
	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Perguruan Tinggi");
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

		row.setParent(rows);
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link PerguruanTinggiLain} berdasarkan kode dan nama (keduanya
	 * {@code ilike} sebagian terhadap teks pada form, kosong berarti cocok ke semua), selalu
	 * dibatasi ke data aktif ({@code aktif} true atau null) dan diurutkan nama menaik. Hasil
	 * dipasang ke {@link #grid} lewat {@link PerguruanTinggiLainRenderer} dan dibatasi
	 * {@link Common#MAX_RESULT_50} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, dipakai juga sebagai pengisi awal grid saat
	 *              popup pertama dibuka)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PerguruanTinggiLain.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		criteria.addOrder(Order.asc("nama")).add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kodePerguruanTinggi", kode.getText().trim(), MatchMode.ANYWHERE))

		;

		List<PerguruanTinggiLain> perguruanTinggiLain = criteria.setMaxResults(Common.MAX_RESULT_50).list();

		ListModel strset = new SimpleListModel(perguruanTinggiLain);
		grid.setRowRenderer(new PerguruanTinggiLainRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah pengguna memilih satu baris perguruan tinggi
	 * lain.
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
