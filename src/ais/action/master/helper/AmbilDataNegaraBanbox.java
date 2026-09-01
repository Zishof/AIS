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
import ais.database.model.Negara;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyPanel;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Negara} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Negara} adalah master data negara (dipakai mis. sebagai bagian hierarki wilayah
 * administratif — lihat juga {@link AmbilDataKecamatanBanbox} yang membuat {@code Negara} baru
 * secara inline). Popup pencarian hanya menyediakan satu field {@code nama}, dicocokkan
 * case-insensitive ke kolom {@code namaNegara} dengan
 * {@link org.hibernate.criterion.MatchMode#ANYWHERE} (substring di posisi mana pun; TIDAK ada
 * penanganan khusus untuk kosong — nilai kosong tetap dikirim sebagai pola ilike, secara efektif
 * mencocokkan semua baris). Hasil diurutkan menaik berdasar nama, tanpa pembatasan bisnis lain
 * (tidak ada filter aktif/scoping). Pemilihan bersifat TUNGGAL (Radiogroup). Tidak ada constructor
 * dengan parameter tambahan; field {@code pagingHelper} dideklarasikan tapi TIDAK dipakai —
 * pencarian masih memakai {@code grid.setMold("paging")} client-side lama dibatasi
 * {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataNegaraBanbox extends Bandbox implements GetEventListener {

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
	public AmbilDataNegaraBanbox() {
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

	/** Kriteria pencarian: nama negara (ilike, substring). */
	private Textbox nama;

	/**
	 * Renderer baris grid hasil pencarian {@link Negara}: menampilkan kolom nama dan satu radio
	 * button pilihan. Mengikuti kerangka renderer standar di {@link ais.ui.util.GetEventListener}
	 * — listener {@code onCheck} menutup popup, menyimpan entity terpilih ke atribut
	 * {@code "negara"} dan teks tampilan {@code negara.getNamaNegara()}, lalu meneruskan event ke
	 * {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataNegaraBanbox
	 */
	class NegaraRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Negara negara = (Negara) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(negara.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataNegaraBanbox.this.setOpen(false);
					AmbilDataNegaraBanbox.this.setAttribute("negara", negara);
					AmbilDataNegaraBanbox.this.setValue(negara.getNamaNegara());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(negara.getNamaNegara()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Negara} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan satu field {@code nama}, tombol Cari, dan grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka {@code display()}
	 * standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
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

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Negara");
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

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
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

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
	 * Mengeksekusi pencarian {@link Negara} berdasar kriteria {@code nama} (ilike, substring di
	 * mana pun — TIDAK ada penanganan khusus input kosong, lihat catatan di Javadoc class) dengan
	 * {@code Restrictions.ilike}, diurutkan menaik berdasar nama dan dibatasi
	 * {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link NegaraRenderer} dan model hasil ke
	 * {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Negara> negara = session
				.createCriteria(Negara.class)
				.addOrder(Order.asc("namaNegara"))
				.add(Restrictions.ilike("namaNegara", nama.getText().trim(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();

		System.out.println(negara);
		ListModel strset = new SimpleListModel(negara);
		grid.setRowRenderer(new NegaraRenderer());
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

