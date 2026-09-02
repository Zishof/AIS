package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaDapatKknDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kkn;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKkn;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK berbentuk dialog pencarian-dan-pilih untuk mendaftarkan mahasiswa sebagai calon
 * peserta ({@link MahasiswaDapatKkn} — status "berhak mendaftar", berbeda dari {@link
 * ais.database.model.kkn.MahasiswaDaftarKkn} yang dipakai {@link
 * AmbilDataMahasiswaSeleksiKknHelper} untuk peserta yang sudah diterima) pada satu {@link Kkn}.
 * Menyediakan filter NIM/nama/tahun angkatan/fakultas/prodi; checkbox tiap baris tercentang
 * otomatis bila mahasiswa sudah terdaftar sebagai calon peserta KKN ini.
 *
 * <p>
 * {@link #save()} memproses hanya baris yang tercentang: memvalidasi syarat kepesertaan lewat
 * {@link Common#checkSyaratKkn}, lalu membuat baru {@link MahasiswaDapatKkn} bila belum ada
 * (tidak menimpa data yang sudah ada, dan baris yang dilepas centangnya tidak memicu penghapusan).
 * </p>
 */
public class AmbilDataMahasiswaKknHelper {

	private Kkn kkn;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;

	/** Menyiapkan combobox filter fakultas dan jurusan (dengan opsi "Semua"). */
	public AmbilDataMahasiswaKknHelper() {
	}

	/** Renderer baris kandidat mahasiswa: checkbox (tercentang bila sudah terdaftar sebagai calon peserta {@link #kkn} ini), NIM, nama, tahun angkatan. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private MahasiswaDapatKknDao mahasiswaDapatKknDao = DaoFactory.getInstance().getMahasiswaDapatKknDao();

		private Session session = mahasiswaDapatKknDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			Integer jml = ((Number) session.createCriteria(MahasiswaDapatKkn.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("kkn", kkn)).uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}

	}

	/**
	 * Menetapkan setiap mahasiswa tercentang sebagai calon peserta {@link #kkn} bila memenuhi
	 * syarat ({@link Common#checkSyaratKkn}) dan belum terdaftar sebelumnya.
	 */
	@SuppressWarnings("unchecked")
	public void save() throws Exception {
		MahasiswaDapatKknDao mahasiswaDapatKknDao = DaoFactory.getInstance().getMahasiswaDapatKknDao();
		Session session = mahasiswaDapatKknDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			@SuppressWarnings("rawtypes")
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					if (!Common.checkSyaratKkn(mahasiswa, kkn)) {
						continue;
					}

					Integer jml = ((Number) session.createCriteria(MahasiswaDapatKkn.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("kkn", kkn)).uniqueResult()).intValue();

					if (jml.equals(0)) {
						MahasiswaDapatKkn mahasiswaDapatKkn = new MahasiswaDapatKkn();
						mahasiswaDapatKkn.setKkn(kkn);
						mahasiswaDapatKkn.setKeterangan("");
						mahasiswaDapatKkn.setMahasiswa(mahasiswa);
						session.save(mahasiswaDapatKkn);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKknHelper.java:129");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Titik masuk utama: membangun dialog pencarian mahasiswa dan grid berpaging server-side,
	 * dengan tombol Simpan (memanggil {@link #save()}) / Cari / Batal.
	 *
	 * @param kkn        KKN tujuan pendaftaran calon peserta
	 * @param dataLoader dipanggil untuk memuat ulang tampilan pemanggil setelah simpan
	 * @param window     window pembungkus dialog (dibersihkan dan diisi ulang)
	 */
	public void display(final Kkn kkn, final DataLoader dataLoader, final MyWindow window) throws Exception {
		this.kkn = kkn;
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("750px");
		window.setHeight("540px");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mahasiswa (kosong = semua)"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.setTooltiptext(
				"Isi dengan tahun masuk mahasiswa, bukan tahun pelaksanaan KKN. Kosongkan untuk menampilkan semua angkatan.");

		row = new MyFormRow();
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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari kandidat mahasiswa sesuai filter");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKknHelper.java:262");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("65%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("25%");

		onSearchDefault(null);

		window.setVisible(true);
		window.onModal();
	}

	/** Memuat ulang grid kandidat mahasiswa (maks {@link Common#MAX_RESULT} baris) sesuai filter aktif. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Mahasiswa> mahasiswa = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setEmptyMessage(mahasiswa.isEmpty()
				? "Tidak ada kandidat mahasiswa yang sesuai. Kosongkan Angkatan Mahasiswa atau sesuaikan NIM, Nama, Fakultas, dan Prodi."
				: "Tidak ada kandidat mahasiswa yang cocok dengan filter.");
		grid.setModelCheckMobile(strset);

	}

}
