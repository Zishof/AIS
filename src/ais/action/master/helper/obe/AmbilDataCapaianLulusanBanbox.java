package ais.action.master.helper.obe;

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
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.obe.CapaianLulusan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.obe.CapaianLulusan}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code CapaianLulusan} adalah Capaian
 * Pembelajaran Lulusan (CPL) dalam kurikulum berbasis OBE (Outcome-Based Education) — rumusan
 * kompetensi lulusan program studi yang dipetakan ke mata kuliah.
 *
 * <p>
 * Pencarian memakai kotak teks {@code nama} (dicocokkan ILIKE ke kolom {@code kode} maupun
 * {@code nama} CPL sekaligus, digabung {@code OR}) serta dua combobox {@code searchfakultas} dan
 * {@code searchjurusan} untuk menyempitkan berdasarkan fakultas/jurusan pemilik CPL. Hanya CPL
 * dengan {@code aktif = true} yang ditampilkan. Bila constructor dipanggil dengan parameter
 * {@code matakuliah} tidak null, hasil juga disaring agar hanya memuat CPL umum ({@code
 * khususBuatMk} null) atau CPL yang memang dikhususkan untuk mata kuliah tersebut. Bila constructor
 * dipanggil dengan {@code jurusan} tidak null, combobox fakultas/jurusan disembunyikan (diganti
 * label statis nama jurusan) dan hasil dikunci ke jurusan tersebut — kedua parameter ini adalah
 * filter dari entity induk yang meletakkan Bandbox ini di formnya. Pemilihan bersifat tunggal
 * (baris grid dibungkus {@link org.zkoss.zul.Radiogroup}, satu {@code MyRadioConfig} per baris).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataCapaianLulusanBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private Jurusan jurusan = null;
	private Matakuliah matakuliah = null;

	/**
	 * Constructor tanpa filter — sama dengan memanggil {@link #AmbilDataCapaianLulusanBanbox(Jurusan,
	 * Matakuliah)} dengan kedua parameter {@code null} (semua CPL aktif dapat dicari, tanpa dikunci
	 * ke jurusan/mata kuliah tertentu).
	 */
	public AmbilDataCapaianLulusanBanbox() {
		this(null, null);
	}

	/**
	 * Constructor dengan filter dari entity induk. Mengikuti kerangka standar
	 * {@link ais.ui.util.GetEventListener}: {@code setReadonly(true)} implisit lewat {@link #display()},
	 * lalu memasang listener {@code onOpen} yang lazy-build popup pada pembukaan pertama.
	 *
	 * @param jurusan    bila tidak {@code null}, hasil pencarian dikunci ke jurusan ini dan combobox
	 *                   fakultas/jurusan pada popup diganti label statis (read-only)
	 * @param matakuliah bila tidak {@code null}, hasil pencarian dibatasi ke CPL umum ({@code
	 *                   khususBuatMk} null) ditambah CPL yang dikhususkan untuk mata kuliah ini
	 */
	public AmbilDataCapaianLulusanBanbox(Jurusan jurusan, Matakuliah matakuliah) {
		super();
		this.jurusan = jurusan;
		this.matakuliah = matakuliah;
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
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	/**
	 * Renderer baris grid hasil pencarian CPL. Mengikuti pola standar {@link ais.ui.util.GetEventListener}:
	 * tiap baris menampilkan label kode dan nama CPL plus satu radio button; memilih radio menutup
	 * popup, menyimpan {@link CapaianLulusan} terpilih ke atribut {@code "capaianLulusan"} pada
	 * Bandbox, mengisi teks tampilan Bandbox dengan {@code kode-nama}, lalu meneruskan event ke
	 * {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataCapaianLulusanBanbox
	 */
	class CapaianLulusanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu {@link CapaianLulusan}: kolom checkbox/radio pilihan,
		 * kolom kode, dan kolom nama.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CapaianLulusan capaianLulusan = (CapaianLulusan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(capaianLulusan.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCapaianLulusanBanbox.this.setOpen(false);
					AmbilDataCapaianLulusanBanbox.this.setAttribute("capaianLulusan", capaianLulusan);
					AmbilDataCapaianLulusanBanbox.this
							.setValue(capaianLulusan.getKode() + "-" + capaianLulusan.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(capaianLulusan.getKode()).setParent(arg0);
			new Label(capaianLulusan.getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria + tombol Cari + grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat pertama dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi. Mengikuti kerangka standar
	 * {@link ais.ui.util.GetEventListener}.
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
		panel.setTitle("Daftar Capaian Pembelajaran Lulusan (CPL)");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama/Kode"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		if (jurusan == null) {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(searchfakultas);
			searchfakultas.setWidth("90%");

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
			row.appendChild(searchjurusan);
			searchjurusan.setWidth("90%");

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		} else {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
			row.appendChild(new Label(jurusan.getNama()));
		}

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

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		searchjurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link CapaianLulusan} aktif berdasarkan kriteria pada popup: kombinasi
	 * jurusan (dari constructor dan/atau combobox {@code searchjurusan}), fakultas (combobox
	 * {@code searchfakultas}), kecocokan {@code kode}/{@code nama} terhadap teks {@code nama}, serta
	 * batasan {@code khususBuatMk} dari constructor. Hasil dipasang ke {@link #grid} lewat
	 * {@link CapaianLulusanRenderer}, dibatasi {@link Common#MAX_RESULT_1000} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, tidak dipakai isinya — hanya sinyal untuk
	 *              menjalankan ulang pencarian)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Jurusan s = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		Fakultas f = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();
		List<CapaianLulusan> capaianLulusan = session.createCriteria(CapaianLulusan.class)
				.add(matakuliah != null
						? Restrictions.or(Restrictions.isNull("khususBuatMk"),
								Restrictions.eq("khususBuatMk", matakuliah))
						: Restrictions.isNull("khususBuatMk"))
				.createAlias("jurusan", "jurusan")

				.add(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))
				.add(s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", s))
				.add(f == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", f))
				.add(Restrictions.eq("aktif", true)).addOrder(Order.asc("kode"))
				.add(Restrictions.or(Restrictions.ilike("kode", nama.getText().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE)))
				.setMaxResults(Common.MAX_RESULT_1000).list();

		System.out.println(capaianLulusan);
		ListModel strset = new SimpleListModel(capaianLulusan);
		grid.setRowRenderer(new CapaianLulusanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memasang listener yang dipanggil setelah pengguna memilih satu CPL di grid.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * @return listener yang saat ini terpasang, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
