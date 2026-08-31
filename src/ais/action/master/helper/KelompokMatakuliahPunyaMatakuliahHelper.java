package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokMatakuliah;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk mengelola anggota matakuliah satu {@link KelompokMatakuliah}: menampilkan grid
 * {@link KelompokMatakuliahPunyaMatakuliah} (kode/nama/status matakuliah beserta fakultas/jurusannya)
 * dengan pencarian kode/nama/fakultas/jurusan, tombol "Ambil data Matakuliah" untuk menambah anggota
 * baru, hapus per baris, dan hapus massal seluruh anggota kelompok (query SQL langsung, hanya tampil
 * bagi pengguna dengan hak {@link CommonPrivilages#DELETE}).
 */
public class KelompokMatakuliahPunyaMatakuliahHelper implements DataLoader {

	private MyGrid grid;
	private KelompokMatakuliah kelompokMatakuliah;
	private Textbox kode;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	private boolean delete = false;

	/** Menentukan hak hapus dari privilese pengguna saat ini dan menyiapkan komponen paging dengan filter fakultas/jurusan. */
	public KelompokMatakuliahPunyaMatakuliahHelper() {

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan); 

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/** Row renderer grid anggota kelompok matakuliah: kode/nama/status matakuliah, fakultas/jurusan pemiliknya, dan tombol hapus (bila punya hak). */
	class DetailKelompokMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) data;

			RevisiHelper
					.createNewRevisi(
							KelompokMatakuliahPunyaMatakuliah.class,
							kelompokMatakuliahPunyaMatakuliah,
							kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
									.getKode()).setParent(row);

			new Label(kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
					.getNama()).setParent(row);

			new Label(kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
					.getStatus() == null ? ""
					: kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
							.getStatus()).setParent(row);

			new Label(kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
					.getJurusan() == null ? ""
					: kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
							.getJurusan().getFakultas().getNama()
							+ "").setParent(row);

			new Label(kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
					.getJurusan() == null ? ""
					: kelompokMatakuliahPunyaMatakuliah.getMatakuliah()
							.getJurusan().getNama()
							+ "").setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil
													.currentSession();

											session.delete(
													(kelompokMatakuliahPunyaMatakuliah));

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Membangun kriteria Hibernate {@link KelompokMatakuliahPunyaMatakuliah} milik kelompok
	 * matakuliah saat ini, difilter fakultas/jurusan/kode/nama matakuliah.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan kode matakuliah menaik
	 * @return kriteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session
				.createCriteria(KelompokMatakuliahPunyaMatakuliah.class);

		criteria.createAlias("matakuliah", "matakuliah")
				.createAlias("matakuliah.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"matakuliah.jurusan", searchjurusan.getSelectedItem()
								.getValue()))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"jurusan.fakultas", searchfakultas.getSelectedItem()
								.getValue()))

				.add(Restrictions.ilike("matakuliah.kode", kode.getValue()
						.trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("matakuliah.nama", nama.getValue()
						.trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("kelompokMatakuliah", kelompokMatakuliah));

		if (order)
			criteria.addOrder(Order.asc("matakuliah.kode"));

		return criteria;
	}

	/** Memuat ulang halaman anggota kelompok matakuliah saat ini dan me-render ulang grid. Parameter {@code value} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		initCriteria(true).list();

		Common.initPaging(initCriteria(false), paging);
		List<KelompokMatakuliahPunyaMatakuliah> myKelompokMatakuliahPunyaMatakuliahs = initCriteria(
				true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(
				myKelompokMatakuliahPunyaMatakuliahs);
		grid.setRowRenderer(new DetailKelompokMatakuliahRenderer());
		grid.setModelCheckMobile(strset);
		

	}

	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun UI grid anggota kelompok matakuliah (toolbar cari/ambil-data/hapus-semua, kolom grid)
	 * di dalam {@code component} untuk kelompok matakuliah yang diberikan dan memuat data awal.
	 *
	 * @param kelompokMatakuliah kelompok matakuliah yang anggotanya ditampilkan
	 * @param component          container ZK yang akan diisi
	 * @param window             diteruskan ke dialog "Ambil data Matakuliah"
	 */
	public void display(final KelompokMatakuliah kelompokMatakuliah,
			final Component component, final MyWindow window) {
		this.kelompokMatakuliah = kelompokMatakuliah;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled(
				"Daftar matakuliah yang mengikuti kelompok "
						+ kelompokMatakuliah.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode : ")));
		toolbar.appendChild(kode = new Textbox());

		kode.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label("Fakultas"
				+ " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil data Matakuliah", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMatakuliahKelompokMatakuliahHelper dataMatakuliahHelper = new AmbilDataMatakuliahKelompokMatakuliahHelper();
				dataMatakuliahHelper.display(kelompokMatakuliah,
						getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setOrient("vertical");
		button.setVisible(delete);
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus semua data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil
												.currentSession();

										session.createSQLQuery(
												"delete from kelompok_matakuliah_punya_matakuliah where kelompok_matakuliah = "
														+ kelompokMatakuliah
																.getId())
												.executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e); 
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

}
