package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatStatusKerjasamaMahasiswa;
import ais.database.model.StatusKerjasamaMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK untuk menampilkan dan mengelola daftar mahasiswa yang mendapatkan satu
 * {@link StatusKerjasamaMahasiswa} tertentu (relasi {@link MahasiswaDapatStatusKerjasamaMahasiswa}).
 * Membangun panel berisi toolbar tambah data (membuka
 * {@code AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper}) dan grid ber-paging yang menampilkan
 * NIM, nama, jurusan, dan fakultas mahasiswa, dengan tombol hapus per baris (konfirmasi +
 * penanganan galat integritas referensial via {@link PesanFormalHelper}).
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} agar dapat dipakai sebagai callback penyegar data oleh
 * helper penambah data setelah relasi baru disimpan.
 * </p>
 */
public class StatusKerjasamaMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private StatusKerjasamaMahasiswa statusKerjasamaMahasiswa;

	/** Perender baris grid: menampilkan NIM, nama, jurusan, fakultas mahasiswa, dan tombol hapus. */
	class DetailStatusKerjasamaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDapatStatusKerjasamaMahasiswa mahasiswaDapatStatusKerjasamaMahasiswa = (MahasiswaDapatStatusKerjasamaMahasiswa) data;

			Mahasiswa mahasiswa = mahasiswaDapatStatusKerjasamaMahasiswa.getMahasiswa();

			new Label(mahasiswa.getNim()).setParent(row);
			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? ""
					: mahasiswa.getJurusan().getFakultas() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getNama()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(mahasiswaDapatStatusKerjasamaMahasiswa);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data status kerjasama mahasiswa ini",
													e,
													new String[] {
															"Periksa apakah data ini masih berelasi dengan data lain sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
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
	 * Memuat ulang isi grid dengan seluruh {@link MahasiswaDapatStatusKerjasamaMahasiswa} milik
	 * {@link #statusKerjasamaMahasiswa} saat ini, diurutkan menaik berdasarkan id.
	 *
	 * @param value tidak digunakan; ada untuk memenuhi kontrak {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<MahasiswaDapatStatusKerjasamaMahasiswa> mahasiswaDapatStatusKerjasamaMahasiswa = session
				.createCriteria(MahasiswaDapatStatusKerjasamaMahasiswa.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("statusKerjasamaMahasiswa", statusKerjasamaMahasiswa)).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatStatusKerjasamaMahasiswa);
		grid.setRowRenderer(new DetailStatusKerjasamaMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @return referensi ke helper ini sendiri, dipakai sebagai {@link DataLoader} oleh helper penambah data. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun dan menampilkan panel daftar mahasiswa penerima status kerjasama tertentu ke dalam
	 * {@code component}. {@code component} dibersihkan lebih dulu, lalu diisi panel berisi toolbar
	 * tambah data dan grid kolom Kode/Nama/Jurusan/Fakultas ber-paging.
	 *
	 * @param statusKerjasamaMahasiswa status kerjasama yang daftar mahasiswanya ditampilkan
	 * @param component                komponen ZK tujuan tampilan
	 * @param window                   window pemanggil, diteruskan ke helper tambah data
	 */
	public void displayPrasyaratStatusKerjasamaMahasiswa(final StatusKerjasamaMahasiswa statusKerjasamaMahasiswa,
			final Component component, final MyWindow window) {
		this.statusKerjasamaMahasiswa = statusKerjasamaMahasiswa;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Daftar mahasiswa yang mendapatkan status kerjasama");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper ambilDataStatusKerjasamaMahasiswaHelper = new AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataStatusKerjasamaMahasiswaHelper.display(statusKerjasamaMahasiswa, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(panelchildren);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
