package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Intbox;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kurikulum;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Kurikulum} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Kurikulum} adalah master data kurikulum akademik (kumpulan struktur mata kuliah per
 * semester yang berlaku untuk suatu prodi/program pada tahun tertentu). Popup pencarian
 * menyediakan field {@code nama} (ilike substring), {@code tahun} (Intbox, eq), dan Combobox
 * fakultas/prodi. KHAS di antara subclass sejenis: setiap baris grid punya
 * {@link ais.ui.util.MyDetail} yang bisa diperluas (expand) — saat dibuka, memuat
 * {@code DetailSemesterKurikulumHelper} untuk menampilkan rincian struktur kurikulum per semester
 * langsung di dalam popup pencarian, tanpa perlu membuka layar terpisah. Hanya baris
 * {@code aktif == true} atau {@code aktif} kosong yang tampil, diurutkan menurun berdasar tahun.
 * Pemilihan bersifat TUNGGAL (Radiogroup). Tidak ada constructor dengan parameter tambahan; field
 * {@code pagingHelper} dideklarasikan tapi TIDAK dipakai — pencarian masih memakai
 * {@code grid.setMold("paging")} client-side lama dibatasi {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKurikulumBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor standar: mempersiapkan Combobox fakultas/prodi (termasuk opsi "Semua") lewat
	 * {@link ais.common.Common#initFakultasDanJurusanDanSemua} dan memasang listener
	 * {@code onOpen} yang membangun popup pencarian secara lazy pada pembukaan pertama. Mengikuti
	 * kerangka standar di {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus
	 * entity ini.
	 */
	public AmbilDataKurikulumBanbox() {
		super();
		setReadonly(true);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	/** Kriteria pencarian: nama kurikulum (ilike, substring). */
	private Textbox nama;
	/** Kriteria pencarian: fakultas (termasuk opsi "Semua"). */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi (termasuk opsi "Semua"). */
	private Combobox searchjurusan = new Combobox();
	/** Kriteria pencarian: tahun berlaku kurikulum (eq). */
	private Intbox tahun;

	/**
	 * Renderer baris grid hasil pencarian {@link Kurikulum}: kolom nama, program, jurusan, tahun,
	 * dan tahun akademik/jenis semester mulai berlaku, plus satu radio button pilihan. KHAS
	 * renderer ini: setiap baris memasang {@link ais.ui.util.MyDetail} yang bisa diperluas — saat
	 * dibuka, memuat {@code DetailSemesterKurikulumHelper} untuk menampilkan rincian struktur
	 * kurikulum per semester langsung di baris tersebut. Selebihnya mengikuti kerangka renderer
	 * standar di {@link ais.ui.util.GetEventListener} — listener {@code onCheck} pada radio button
	 * menutup popup, menyimpan entity terpilih ke atribut {@code "kurikulum"} dan teks tampilan
	 * {@code kurikulum.toString()}, lalu meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataKurikulumBanbox
	 */
	class KurikulumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kurikulum kurikulum = (Kurikulum) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(kurikulum.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKurikulumBanbox.this.setOpen(false);
					AmbilDataKurikulumBanbox.this.setAttribute("kurikulum", kurikulum);
					AmbilDataKurikulumBanbox.this.setValue(kurikulum.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						MyWindow addWindow = new MyWindow();
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

						DetailSemesterKurikulumHelper detailSemesterKurikulumHelper = new DetailSemesterKurikulumHelper();
						detailSemesterKurikulumHelper.display(kurikulum, detail, addWindow);
					}
				}
			});

			new Label(kurikulum.getNama()).setParent(arg0);
			new Label(kurikulum.getProgram() == null ? "" : kurikulum.getProgram().getNama()).setParent(arg0);
			new Label(kurikulum.getJurusan() == null ? "" : kurikulum.getJurusan().getNama()).setParent(arg0);
			new Label(kurikulum.getTahun() + "").setParent(arg0);
			new Label(kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester()).setParent(arg0);
		}

	}

	/**
	 * Membangun popup pencarian {@link Kurikulum} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field nama, tahun, fakultas, dan prodi, tombol Cari, dan grid
	 * hasil dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka
	 * {@code display()} standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kurikulum");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Intbox());
		tahun.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

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
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Berlaku Mulai");
		column.setWidth("20%");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link Kurikulum} dengan filter {@code aktif} (baris nonaktif
	 * disembunyikan kecuali kolomnya {@code null}), {@code nama} (ilike substring), {@code tahun}
	 * (eq bila diisi), prodi (eq bila dipilih), dan fakultas (eq bila dipilih, lewat join ke
	 * {@code jurusan}). Diurutkan menurun berdasar tahun, dibatasi
	 * {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link KurikulumRenderer} dan model hasil
	 * ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Kurikulum> kurikulum = session.createCriteria(Kurikulum.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tahun"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(tahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", tahun.getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(kurikulum);
		ListModel strset = new SimpleListModel(kurikulum);
		grid.setRowRenderer(new KurikulumRenderer());
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
