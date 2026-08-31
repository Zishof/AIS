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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.MahasiswaDaftarKkn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window modal untuk memilih mahasiswa (yang sudah diterima
 * mendaftar KKN, lewat {@link MahasiswaDaftarKkn}) dan memasukkannya ke satu {@link KelompokKkn}
 * (kelompok KKN), sambil menegakkan batas kuota kelompok.
 *
 * <p>
 * Kandidat mahasiswa dibatasi pada peserta {@link MahasiswaDaftarKkn} yang statusnya
 * {@code DITERIMA} untuk {@code Kkn} induk dari {@code kelompokKkn}, dengan filter opsional NIM,
 * nama, tahun angkatan, fakultas, dan prodi. Saat {@link #save()}, jumlah mahasiswa yang sudah
 * tercatat {@link MahasiswaDapatKelompokKkn#getDiterima()}{@code =true} ditambah jumlah baris yang
 * baru dicentang dibandingkan terhadap {@link KelompokKkn#getKuota()}; bila melebihi, penyimpanan
 * dibatalkan dan pesan peringatan ditampilkan.
 * </p>
 */
public class AmbilDataMahasiswaKelompokKknHelper {

	private KelompokKkn kelompokKkn;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	/** Menyiapkan combobox filter fakultas/jurusan (diisi opsi "Semua" + seluruh data aktif). */
	public AmbilDataMahasiswaKelompokKknHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/**
	 * Perender baris grid untuk satu {@link Mahasiswa}: checkbox pilih (dicentang bila mahasiswa
	 * sudah diterima di {@link #kelompokKkn} lewat {@link MahasiswaDapatKelompokKkn}), nama, tahun
	 * angkatan, dan jurusan.
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			Checkbox checkbox = new Checkbox(mahasiswa.getNim());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			Session session = HibernateUtil.currentSession();
			Integer jml = ((Number) session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true)).setProjection(Projections.rowCount())
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("kelompokKkn", kelompokKkn))
					.uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getJurusan().getNama()).setParent(arg0);
		}

	}

	/**
	 * Menambahkan mahasiswa yang tercentang di grid ke {@link #kelompokKkn}, dengan penegakan
	 * kuota: jumlah anggota diterima saat ini ditambah baris tercentang tidak boleh melebihi
	 * {@link KelompokKkn#getKuota()} — bila melebihi, ditampilkan peringatan dan method berhenti
	 * tanpa menyimpan apa pun. Mahasiswa yang sudah punya baris {@link MahasiswaDapatKelompokKkn}
	 * diterima untuk kelompok ini dilewati (tidak dibuat dobel).
	 *
	 * @throws Exception diteruskan dari kegagalan Hibernate yang tidak tertangkap di loop internal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws Exception {

		Session session = HibernateUtil.currentSession();

		int count = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class)
				.add(Restrictions.eq("kelompokKkn", kelompokKkn)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			// FIX ClassCastException "Label cannot be cast to Checkbox": data.get(0) mengasumsikan
			// child pertama Row SELALU Checkbox, padahal urutan child bisa berbeda (mis. baris tanpa
			// data mahasiswa). Renderer sudah menyimpan referensi Checkbox lewat setAttribute("checkbox",
			// ...) (lihat baris ~75) -- pakai itu, sama seperti loop simpan di bawah, bukan asumsi posisi.
			Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
			if (checkbox != null && checkbox.isChecked()) {
				count++;
			}
		}

		if (count > kelompokKkn.getKuota()) {
			MyMessageboxConfig.show("Jumlah mahasiswa tidak boleh melebihi kuota yang ditentukan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		for (Row row : list) {

			try {
				Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					Integer jml = ((Number) session.createCriteria(MahasiswaDapatKelompokKkn.class)
							.add(Restrictions.eq("diterima", true)).setProjection(Projections.rowCount())
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("kelompokKkn", kelompokKkn)).uniqueResult()).intValue();

					if (jml.equals(0)) {
						MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = new MahasiswaDapatKelompokKkn();
						mahasiswaDapatKelompokKkn.setKelompokKkn(kelompokKkn);
						mahasiswaDapatKelompokKkn.setKeterangan("");
						mahasiswaDapatKelompokKkn.setMahasiswa(mahasiswa);
						mahasiswaDapatKelompokKkn.setDiterima(true);
						session.save(mahasiswaDapatKelompokKkn);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKelompokKknHelper.java:137");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membuat dan menampilkan window modal "Ambil Data Mahasiswa" berisi form filter (NIM, nama,
	 * tahun angkatan, fakultas, prodi) dan grid mahasiswa peserta KKN yang bisa dipilih. Window
	 * dibuat baru dan dipasang langsung ke root halaman (bukan memakai window yang diberikan
	 * pemanggil). Tombol "Simpan" memanggil {@link #save()}, memuat ulang data pemanggil, lalu
	 * menutup window.
	 *
	 * @param kelompokKkn kelompok KKN tujuan penempatan mahasiswa
	 * @param dataLoader  callback muat-ulang data pemanggil setelah simpan
	 */
	public void display(final KelompokKkn kelompokKkn, final DataLoader dataLoader) {
		this.kelompokKkn = kelompokKkn;

		final MyWindow window = new MyWindow();
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("750px");
		window.setHeight("540px");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");

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

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

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
				window.detach();
			}
		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
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
				window.detach();
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.getPagingChild().setMold("os");
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
		final Checkbox checkbox = new Checkbox("NIM");
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						Checkbox myCheckbox = (Checkbox) row.getAttribute("checkbox");
						if (myCheckbox != null) {
							myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKelompokKknHelper.java:279");

					}
				}
			}
		});
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memuat grid dengan mahasiswa peserta {@code Kkn} induk dari {@link #kelompokKkn} yang
	 * berstatus diterima ({@link MahasiswaDaftarKkn#DITERIMA}), difilter opsional berdasarkan
	 * NIM/nama (ILIKE anywhere), tahun angkatan, jurusan, dan fakultas. Diurutkan berdasarkan
	 * tahun angkatan menurun lalu NIM menaik, dibatasi {@link Common#MAX_RESULT_1000} hasil.
	 *
	 * @param event tidak dipakai
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Mahasiswa> mahasiswa = ConstantValues.simpleList(session.createCriteria(MahasiswaDaftarKkn.class)

//				.add(Restrictions.sqlRestriction(
//						"this_.mahasiswa not in (select a.mahasiswa from mahasiswa_dapat_kelompok_kelompok_kkn a inner join kelompok_kkn b on (a.kelompok_kkn = b.id) where b.kkn="
//								+ kelompokKkn.getKkn().getId() + ")"))

				.setProjection(Projections.property("mahasiswa.id"))

				.add(Restrictions.eq("kkn", kelompokKkn.getKkn()))

				.add(Restrictions.eq("terima", MahasiswaDaftarKkn.DITERIMA)).createCriteria("mahasiswa")

				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT_1000), Mahasiswa.class, false);
		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
