package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.TimDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jabatan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window modal untuk memilih (secara massal, via checkbox) beberapa
 * {@link Dosen} yang akan diberi satu {@link Jabatan} (jabatan fungsional/struktural) tertentu.
 * Menampilkan grid dosen aktif dengan filter pencarian nama, fakultas, dan program studi
 * (prodi/jurusan), memakai paging bawaan grid ZK dibatasi {@link Common#MAX_RESULT} hasil.
 *
 * <p>
 * Field jabatan yang diisi bergantung pada {@link Jabatan#getPtSendiri()}: bila jabatan berasal
 * dari perguruan tinggi sendiri, disimpan ke {@link Dosen#setSpesifikasiJabatan(Jabatan)}; bila
 * berasal dari PT lain (dosen dengan afiliasi ganda), disimpan ke
 * {@link Dosen#setSpesifikasiJabatanPtLain(Jabatan)}. Penyimpanan hanya untuk baris yang
 * checkbox-nya dicentang dan tidak dalam keadaan disabled.
 * </p>
 */
public class AmbilDataDosenUntukJabatanHelper {
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Jabatan jabatan;
	private Textbox nama;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	/** Menyiapkan combobox filter fakultas/jurusan (diisi opsi "Semua" + seluruh data aktif). */
	public AmbilDataDosenUntukJabatanHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Perender baris grid: checkbox pilih, kode/NIP, nama, jabatan saat ini, jurusan, fakultas dosen. */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Dosen dosen = (Dosen) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("dosen", dosen);

			// checkbox.setChecked(dosen.getSpesifikasiJabatan() != null);
			// checkbox.setDisabled(dosen.getSpesifikasiJabatan() != null);

			new Label(dosen.getCode()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getSpesifikasiJabatan() == null ? "" : dosen.getSpesifikasiJabatan().getNama())
					.setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);
			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);
		}
	}

	/**
	 * Menerapkan {@link #jabatan} ke setiap {@link Dosen} pada baris grid yang checkbox-nya
	 * tercentang (dan tidak disabled): field tujuan ditentukan oleh
	 * {@link Jabatan#getPtSendiri()} (lihat javadoc kelas). Kegagalan per baris ditangkap dan
	 * dicatat ke audit tanpa menghentikan proses baris lain.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		TimDosenDao timDosenDao = DaoFactory.getInstance().gettTimDosenDao();
		Session session = timDosenDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {

					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					if (jabatan.getPtSendiri()) {
						dosen.setSpesifikasiJabatan(jabatan);
					} else {
						dosen.setSpesifikasiJabatanPtLain(jabatan);
					}
					session.saveOrUpdate(dosen);

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenUntukJabatanHelper.java:102");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membuka window modal berisi form filter (nama/fakultas/prodi) dan grid dosen bercentang
	 * untuk memilih penerima {@code jabatan}. Tombol "Simpan" memanggil {@link #save()}, memuat
	 * ulang data pemanggil lewat {@code dataLoader}, lalu menutup window.
	 *
	 * @param jabatan    jabatan yang akan diberikan ke dosen terpilih
	 * @param dataLoader callback muat-ulang data pemanggil setelah simpan
	 * @param window     window modal tempat UI dibangun
	 */
	public void display(Jabatan jabatan, final DataLoader dataLoader, final MyWindow window) {
		this.jabatan = jabatan;
		Common.clear(window);
		window.setTitle("Ambil Data Dosen");
		window.setWidth("750px");
		window.setHeight("540px");
		//
		// Panel panel = new ais.ui.util.MyPanelConfig();
		// panel.setParent(window);
		// panel.setWidth("100%");
		// panel.setHeight("100%");
		// panel.setTitle("Daftar Dosen");
		// panel.setBorder("none");
		// panel.setStyle("border:0px;");
		//
		// Panelchildren panelchildren = new Panelchildren();
		// panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
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
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		button.setParent(toolbar);

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
		column.setWidth("50px");
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenUntukJabatanHelper.java:217");

					}
				}
			}
		});

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.detach();
			}
		});

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	/**
	 * Memuat grid dosen aktif sesuai filter nama (ILIKE anywhere) dan, bila dipilih,
	 * jurusan/fakultas — dosen yang berstatus {@code milikUniversitas} lolos filter
	 * jurusan/fakultas apa pun (selalu tampil). Hasil dibatasi {@link Common#MAX_RESULT} baris,
	 * diurutkan berdasarkan id.
	 *
	 * @param event tidak dipakai
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Dosen> dosen = session.createCriteria(Dosen.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
