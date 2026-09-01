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
import ais.database.model.Kelas;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Kelas} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Kelas} adalah master data kelas/rombongan belajar akademik (mis. "TI-2023-A"), opsional
 * dikaitkan ke {@code Jurusan}/{@code Fakultas} dan {@code tahunAngkatan} (tahun angkatan). Popup
 * pencarian menyediakan field {@code nama} (ilike substring), Combobox fakultas dan prodi, serta
 * {@code Textbox searchtahun} untuk tahun angkatan. Filter jurusan dan fakultas memakai
 * {@code Restrictions.or(isNull(...), ...)} agar kelas lintas-jurusan/fakultas (berlaku "Semua")
 * tetap muncul; filter fakultas bahkan mencocokkan baik kolom {@code fakultas} langsung pada
 * {@code Kelas} maupun {@code jurusan.fakultas} (join alias {@code jurusanAlias}). Filter tahun
 * angkatan hanya diterapkan bila {@link #ambilTahunAngkatanFilter()} berhasil mem-parse input jadi
 * angka murni (input kosong/"semua"/non-angka diabaikan), dan baris dengan
 * {@code tahunAngkatan == 0} (berlaku "Semua Angkatan") selalu ikut tampil apa pun tahun yang
 * dicari. Kelas ini memakai paging SERVER-SIDE {@link ais.ui.util.AmbilDataPagingHelper} dengan
 * ukuran halaman 50 — tiap perubahan filter (Enter di field nama, ganti combo, klik Cari) mereset
 * halaman aktif ke 0 lewat {@code pagingHelper.getPaging().setActivePage(0)} sebelum mencari
 * ulang. Pemilihan bersifat TUNGGAL (Radiogroup). Tidak ada constructor dengan parameter tambahan.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKelasBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper(50);
	private EventListener eventListener;

	/**
	 * Konstruktor standar: memasang callback paging server-side ke {@link #pagingHelper}, lalu
	 * listener {@code onOpen} yang mempersiapkan Combobox fakultas/prodi (termasuk opsi "Semua")
	 * dan membangun popup pencarian secara lazy pada pembukaan pertama. Mengikuti kerangka standar
	 * di {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus entity ini.
	 */
	public AmbilDataKelasBanbox() {
		super();
		setReadonly(true);
		pagingHelper.pasangOnPaging(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	/** Kriteria pencarian: nama kelas (ilike, substring). */
	private Textbox nama;
	/** Kriteria pencarian: fakultas (langsung atau lewat jurusan; termasuk opsi "Semua"). */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi (termasuk opsi "Semua"). */
	private Combobox searchjurusan = new Combobox();
	/** Kriteria pencarian: tahun angkatan (bebas teks; hanya dipakai bila berupa angka murni). */
	private Textbox searchtahun = new Textbox();

	/**
	 * Renderer baris grid hasil pencarian {@link Kelas}: kolom nama, jurusan (tampil "Semua" bila
	 * kosong), tahun angkatan (tampil "Semua" bila kosong), keterangan, dan satu radio button
	 * pilihan. Mengikuti kerangka renderer standar di {@link ais.ui.util.GetEventListener} —
	 * listener {@code onCheck} menutup popup, menyimpan entity terpilih ke atribut {@code "kelas"}
	 * dan teks tampilan {@code kelas.toString()}, lalu meneruskan event ke {@link #eventListener}
	 * bila terpasang.
	 *
	 * @see AmbilDataKelasBanbox
	 */
	class KelasRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kelas kelas = (Kelas) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(kelas.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelasBanbox.this.setOpen(false);
					AmbilDataKelasBanbox.this.setAttribute("kelas", kelas);
					AmbilDataKelasBanbox.this.setValue(kelas.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kelas.getNama()).setParent(arg0);
			new Label(kelas.getJurusan() == null ? "Semua" : kelas.getJurusan().getNama()).setParent(arg0);
			new Label(kelas.getTahunAngkatan() == null ? "Semua" : kelas.getTahunAngkatan() + "").setParent(arg0);
			new Label(kelas.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Kelas} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field nama, fakultas, prodi, dan tahun angkatan (masing-masing
	 * memanggil ulang {@link #onSearchDefault(Event)} dan mereset halaman aktif saat berubah),
	 * tombol Cari, dan grid hasil ber-paging server-side dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka {@code display()}
	 * standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
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
		panel.setTitle("Daftar Kelas");
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
			public void onEvent(Event event) throws Exception {
				pagingHelper.getPaging().setActivePage(0);
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pagingHelper.getPaging().setActivePage(0);
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pagingHelper.getPaging().setActivePage(0);
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchtahun);
		searchtahun.setWidth("90%");
		searchtahun.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pagingHelper.getPaging().setActivePage(0);
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
				pagingHelper.getPaging().setActivePage(0);
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
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		Row rowKeempat = new Row();
		rowKeempat.setParent(rowsUtama);
		pagingHelper.getPaging().setParent(rowKeempat);

		onSearchDefault(null);

	}

	public Criteria initCriteria(Session session, boolean order) {
		Integer tahunAngkatan = ambilTahunAngkatanFilter();
		Criteria criteria = session.createCriteria(Kelas.class)
				.createAlias("jurusan", "jurusanAlias", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(tahunAngkatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("tahunAngkatan", 0),
								Restrictions.eq("tahunAngkatan", tahunAngkatan)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.or(
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false),
												CommonSearchFilterHelper.eqSelectedWithId("jurusanAlias.fakultas", searchfakultas, false)),
										Restrictions.and(Restrictions.isNull("fakultas"), Restrictions.isNull("jurusan"))));
		if (order) {
			criteria.addOrder(Order.asc("jurusan")).addOrder(Order.asc("tahunAngkatan")).addOrder(Order.asc("nama"));
		}
		return criteria;
	}

	private Integer ambilTahunAngkatanFilter() {
		String value = searchtahun == null ? null : searchtahun.getValue();
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.length() == 0 || "all".equalsIgnoreCase(value) || "semua".equalsIgnoreCase(value)) {
			return null;
		}
		if (!value.matches("\\d+")) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<Kelas> kelas = pagingHelper.cari(new ais.ui.util.AmbilDataPagingHelper.CriteriaFactory() {
			@Override
			public Criteria initCriteria(Session session, boolean order) {
				return AmbilDataKelasBanbox.this.initCriteria(session, order);
			}
		}, Kelas.class, new ais.ui.util.AmbilDataPagingHelper.Inisialisasi<Kelas>() {
			@Override
			public void init(Kelas data) {
				if (data != null) {
					data.getJurusan();
					data.getFakultas();
				}
			}
		});

		System.out.println(kelas);
		ListModel strset = new SimpleListModel(kelas);
		grid.setRowRenderer(new KelasRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
