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
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.QuotaWisudaUntukFakultas;
import ais.database.model.Skripsi;
import ais.database.model.Wisuda;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window modal untuk mendaftarkan mahasiswa (yang skripsinya sudah
 * dinilai) ke satu {@link Wisuda}, membuat baris {@link PendaftaranWisuda}, dengan penegakan kuota
 * per fakultas atau kuota total perguruan tinggi.
 *
 * <p>
 * Checkbox baris hanya ditampilkan untuk mahasiswa yang punya {@link Skripsi} dengan
 * {@code totalNilai > 0} (skripsi sudah dinilai). Saat {@link #save()}, kuota dicek berdasarkan
 * {@link Wisuda#getHanyaGunakanKuotaPerguruanTinggi()}: bila {@code true}, kuota diambil dari
 * {@link Wisuda#getMaksimalQuota()} dan dibandingkan terhadap total pendaftar wisuda; bila
 * {@code false}, kuota diambil per fakultas dari {@link QuotaWisudaUntukFakultas} (wajib sudah
 * diisi, bila belum penyimpanan dibatalkan dengan peringatan) dan dibandingkan terhadap jumlah
 * pendaftar dari fakultas yang sama. Pendaftaran yang melebihi kuota tidak disimpan (baris lain
 * pada save yang sama tetap diproses).
 * </p>
 */
public class AmbilDataMahasiswaMendaftarWisudaHelper {

	private Wisuda wisuda;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Decimalbox tahunlulus;

	/**
	 * Menyiapkan combobox filter fakultas (aktif) dan memasang listener yang mengisi ulang
	 * combobox prodi (difilter sesuai fakultas terpilih) setiap kali fakultas berubah.
	 */
	public AmbilDataMahasiswaMendaftarWisudaHelper() {
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link AmbilDataMahasiswaMendaftarWisudaHelper}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataMahasiswaMendaftarWisudaHelper} dan
		 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see AmbilDataMahasiswaMendaftarWisudaHelper
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

	}

	/**
	 * Perender baris grid untuk satu {@link Mahasiswa}: checkbox pilih (tercentang bila sudah
	 * terdaftar di {@link #wisuda}, dan hanya tampil bila mahasiswa punya skripsi dengan nilai
	 * lebih dari 0), foto, NIM, nama, angkatan, fakultas, dan status skripsi ("Sudah"/"Belum").
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			// final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			Integer jml = ((Number) HibernateUtil.currentSession().createCriteria(PendaftaranWisuda.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("wisuda", wisuda)).uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			Session session = HibernateUtil.currentSession();
			Integer skripsi = ((Number) session.createCriteria(Skripsi.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.gt("totalNilai", 0.0))
					.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult()).intValue();

			checkbox.setVisible(!skripsi.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);

			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

			new Label(skripsi.equals(0) ? "Belum" : "Sudah").setParent(arg0);

		}

	}

	/**
	 * Mendaftarkan setiap mahasiswa yang checkbox-nya tercentang ke {@link #wisuda}, menegakkan
	 * kuota (per fakultas atau total PT — lihat javadoc kelas) sebelum menyimpan tiap baris.
	 * Mahasiswa yang sudah terdaftar dilewati (tidak dobel). Bila kuota fakultas belum diisi,
	 * penyimpanan untuk baris tersebut dibatalkan dengan pesan peringatan dan method langsung
	 * {@code return} (baris lain yang belum diproses pada iterasi ini ikut terlewat).
	 *
	 * @throws Exception diteruskan dari kegagalan Hibernate yang tidak tertangkap di loop internal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws Exception {
		// PendaftaranWisudaDao pendaftaranWisudaDao = DaoFactory.getInstance()
		// .getPendaftaranWisudaDao();
		Session session = HibernateUtil.currentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					Integer jml = ((Number) session.createCriteria(PendaftaranWisuda.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("wisuda", wisuda)).uniqueResult()).intValue();

					if (jml.equals(0)) {
						Integer jmlWisudaUntukFakultas = 0;
						Integer quotaWisuda = 100000;
						Session session2 = HibernateUtil.currentSession();

						if (!wisuda.getHanyaGunakanKuotaPerguruanTinggi()) {
							QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = (QuotaWisudaUntukFakultas) session2
									.createCriteria(QuotaWisudaUntukFakultas.class)
									.add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()))
									.add(Restrictions.eq("wisuda", wisuda)).uniqueResult();

							if (quotaWisudaUntukFakultas == null || quotaWisudaUntukFakultas.getQuota() == null) {
								MyMessageboxConfig.show(
										"Quota wisuda untuk " + Common.getBahasaConfig("Fakultas") + " "
												+ mahasiswa.getJurusan().getFakultas().getNama() + "  "
												+ " untuk wisuda ke " + wisuda.getWisudaKe() + " belum dimasukkan",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}

							quotaWisuda = quotaWisudaUntukFakultas.getQuota();

							jmlWisudaUntukFakultas = ((Number) session.createCriteria(PendaftaranWisuda.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("wisuda", wisuda))
									.createCriteria("mahasiswa").createCriteria("jurusan", Criteria.LEFT_JOIN)
									.add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()))
									.uniqueResult()).intValue();
						} else {
							quotaWisuda = wisuda.getMaksimalQuota();
							jmlWisudaUntukFakultas = ((Number) session.createCriteria(PendaftaranWisuda.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("wisuda", wisuda))
									.uniqueResult()).intValue();
						}

						if (jmlWisudaUntukFakultas >= quotaWisuda) {
							if (wisuda.getHanyaGunakanKuotaPerguruanTinggi()) {
								MyMessageboxConfig.show(
										"Quota wisuda keseluruahn adalah (" + quotaWisuda + ") " + " sudah penuh",
										"Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
							} else {
								MyMessageboxConfig.show(
										"Quota wisuda untuk " + Common.getBahasaConfig("Fakultas") + " "
												+ mahasiswa.getJurusan().getFakultas().getNama() + "(" + quotaWisuda
												+ ") " + "sudah penuh",
										"Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
							}
						} else {
							PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) session
									.createCriteria(PendaftaranWisuda.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

							Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

							if (pendaftaranWisuda == null) {
								pendaftaranWisuda = new PendaftaranWisuda();
								pendaftaranWisuda.setKeterangan("");
								pendaftaranWisuda.setMahasiswa(mahasiswa);
								pendaftaranWisuda.setWisuda(wisuda);
							}

							pendaftaranWisuda.setSkripsi(skripsi);
							pendaftaranWisuda.setWisuda(wisuda);
							session.saveOrUpdate(pendaftaranWisuda);
						}

					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaMendaftarWisudaHelper.java:223");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun window modal "Ambil Data Mahasiswa yang Mendaftar Wisuda" berisi form filter
	 * (NIM, nama, angkatan, fakultas, prodi, tahun lulus) dan grid mahasiswa berpaging. Tombol
	 * "Simpan" memanggil {@link #save()}, memuat ulang data pemanggil, lalu menyembunyikan window.
	 *
	 * @param wisuda     kegiatan wisuda tujuan pendaftaran
	 * @param dataLoader callback muat-ulang data pemanggil setelah simpan
	 * @param window     window modal tempat UI dibangun
	 */
	public void display(final Wisuda wisuda, final DataLoader dataLoader, final MyWindow window) {
		this.wisuda = wisuda;
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa yang Mendaftar Wisuda");
		window.setWidth("950px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus"));
		row.appendChild(tahunlulus = new Decimalbox());
		tahunlulus.setWidth("90%");

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaMendaftarWisudaHelper.java:339");

					}
				}
			}
		});
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getKonfigurasi("label_skripsi", "skripsi").getNilai());
		column.setWidth("6%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
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
				window.setVisible(false);
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
	 * Memuat grid mahasiswa aktif sesuai filter NIM/nama (ILIKE anywhere), tahun angkatan, tahun
	 * lulus, jurusan, dan fakultas. Diurutkan berdasarkan tahun angkatan lalu NIM (keduanya
	 * menaik), dibatasi {@link Common#MAX_RESULT_500} hasil.
	 *
	 * @param event tidak dipakai
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Mahasiswa> mahasiswa = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.addOrder(Order.asc("tahunangkatan")).addOrder(Order.asc("nim"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(tahunlulus.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunLulus", tahunlulus.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT_500).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
