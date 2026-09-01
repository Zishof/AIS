package ais.action.master.helper;


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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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
import ais.database.model.Asrama;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Asrama} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Asrama} adalah master data
 * asrama mahasiswa/tempat tinggal kampus, dikaitkan opsional dengan fakultas/jurusan dan tahun
 * angkatan tertentu (asrama bisa umum untuk semua fakultas/jurusan/angkatan bila field-field
 * tersebut kosong).
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}) dengan filter "Nama" (ILIKE
 * ANYWHERE), "Fakultas" dan "Prodi" (combobox, diinisialisasi lewat
 * {@code Common.initFakultasDanJurusanDanSemua} sehingga pilihan Jurusan mengikuti Fakultas yang
 * dipilih), dan "Angkatan" ({@link org.zkoss.zul.Decimalbox}). Hasil selalu dibatasi ke asrama
 * aktif ({@code aktif} null dianggap aktif); filter fakultas/jurusan/tahun angkatan bersifat
 * inklusif-OR terhadap NULL — asrama yang fakultas/jurusan/tahun-nya kosong (asrama umum) tetap
 * muncul di hasil pencarian apa pun nilai filternya, selain asrama yang cocok persis. Diurutkan
 * menaik berdasarkan jurusan, tahun angkatan, lalu nama.
 *
 * @see Bandbox
 */
public class AmbilDataAsramaBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Membangun komponen dan memasang listener {@code onOpen} yang, pada pembukaan pertama,
	 * menginisialisasi combobox Fakultas/Jurusan lewat
	 * {@code Common.initFakultasDanJurusanDanSemua}, membangun popup ({@link #display()}), lalu
	 * membukanya — mengikuti kerangka umum di {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataAsramaBanbox() {
		super();
		setReadonly(true);

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

	private Textbox nama;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Decimalbox searchtahun = new Decimalbox();

	/**
	 * Merender satu baris grid: radio pilih, nama asrama, nama jurusan ("Semua" bila kosong),
	 * tahun angkatan ("Semua" bila kosong), dan keterangan. Memilih baris menutup popup,
	 * menyimpan entity {@link Asrama} terpilih ke attribute {@code "asrama"} pada Bandbox,
	 * mengisi teks tampilan dengan {@code asrama.toString()}, lalu memicu {@link #eventListener}
	 * bila terpasang — mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataAsramaBanbox
	 */
	class AsramaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Asrama asrama = (Asrama) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(asrama.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataAsramaBanbox.this.setOpen(false);
					AmbilDataAsramaBanbox.this.setAttribute("asrama", asrama);
					AmbilDataAsramaBanbox.this.setValue(asrama.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(asrama.getNama()).setParent(arg0);
			new Label(asrama.getJurusan() == null ? "Semua" : asrama.getJurusan().getNama()).setParent(arg0);
			new Label(asrama.getTahunAngkatan() == null ? "Semua" : asrama.getTahunAngkatan() + "").setParent(arg0);
			new Label(asrama.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Nama/
	 * Fakultas/Prodi/Angkatan, grid hasil bermold "paging", lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Asrama");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchtahun);
		searchtahun.setWidth("90%");

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

		grid = new MyGrid();
		grid.setWidth("100%");
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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		onSearchDefault(null);

	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian {@link Asrama}: aktif, cocok nama (ILIKE
	 * ANYWHERE), dan filter fakultas/jurusan/tahun angkatan yang inklusif-OR terhadap NULL
	 * (asrama umum tanpa fakultas/jurusan/tahun tetap muncul selain yang cocok persis dengan
	 * filter), diurutkan menaik berdasarkan jurusan, tahun angkatan, lalu nama, dibatasi
	 * {@link Common#MAX_RESULT} baris. Mengisi ulang grid dengan hasilnya beserta
	 * {@link AsramaRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Asrama> asrama = session.createCriteria(Asrama.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("jurusan")).addOrder(Order.asc("tahunAngkatan")).addOrder(Order.asc("nama"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("tahunAngkatan",0),
								Restrictions.eq("tahunAngkatan", searchtahun.getValue().intValue())))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.setMaxResults(Common.MAX_RESULT)

				.list();

		System.out.println(asrama);
		ListModel strset = new SimpleListModel(asrama);
		grid.setRowRenderer(new AsramaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu asrama */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan asrama yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}

