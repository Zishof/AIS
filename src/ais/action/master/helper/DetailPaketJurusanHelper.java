package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PilihanPaketPerJurusanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;

/**
 * Helper composer ZK yang menampilkan dan mengelola daftar "Paket" (pilihan paket jurusan) yang
 * terkait dengan satu {@link JurusanSekolahMahasiswaBaru} (pemetaan jurusan asal sekolah calon
 * mahasiswa baru). Dipakai untuk menampilkan panel kecil berisi grid {@link
 * PilihanPaketPerJurusanMhsBaru} dengan tombol tambah dan hapus per baris.
 *
 * <p>
 * Alur pemakaian: pemanggil memanggil {@link #displayDetailPaketJurusan} untuk membangun UI
 * (panel + toolbar + grid) di dalam {@link Component} induk yang diberikan, lalu grid dimuat lewat
 * {@link #loadData(Object)}. Penambahan data dilakukan lewat {@link AmbilPaketHelper} (dipanggil dari
 * tombol "Tambah Data"), sedangkan penghapusan dilakukan langsung dari baris grid lewat
 * {@link DetailPaketRenderer}. Kelas ini mengimplementasikan {@link DataLoader} agar dapat diberikan
 * sebagai callback refresh ke helper lain (mis. {@link AmbilPaketHelper}) setelah data berubah.
 * </p>
 */
public class DetailPaketJurusanHelper implements DataLoader {

	private MyGrid grid;
	// private DosenPembimbingAkademik dosenPembimbingAkademik;
	private JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru;

	/**
	 * Perender baris grid untuk satu {@link PilihanPaketPerJurusanMhsBaru}: menampilkan nama
	 * paket dan tombol hapus yang, setelah konfirmasi, menghapus baris lewat
	 * {@link PilihanPaketPerJurusanDao} dan memuat ulang grid via {@link #loadData(Object)}.
	 */
	class DetailPaketRenderer extends ais.ui.util.MyRowRenderer {

		public DetailPaketRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru = (PilihanPaketPerJurusanMhsBaru) data;

			new Label(pilihanPaketPerJurusanMhsBaru.getPaket().getNama()).setParent(row);

			Hbox toolbar = new Hbox();
	
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											PilihanPaketPerJurusanDao pilihanPaketPerJurusan = DaoFactory.getInstance()
													.getPilihanPaketPerJurusanDao();
											// dosenPembimbingAkademikDao.beginTransaction();
											pilihanPaketPerJurusan.delete(
													pilihanPaketPerJurusan.merge(pilihanPaketPerJurusanMhsBaru));
											// dosenPembimbingAkademikDao.commitTransaction();

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
	 * Memuat ulang isi grid dengan seluruh {@link PilihanPaketPerJurusanMhsBaru} (yang sudah
	 * punya {@code paket}) milik {@link #jurusanSekolahMahasiswaBaru} yang sedang aktif.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PilihanPaketPerJurusanMhsBaru> pilihanPaketPerJurusanMhsBaru = session
				.createCriteria(PilihanPaketPerJurusanMhsBaru.class).add(Restrictions.isNotNull("paket"))
				.add(Restrictions.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru)).list();

		ListModel strset = new SimpleListModel(pilihanPaketPerJurusanMhsBaru);
		grid.setRowRenderer(new DetailPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun UI panel "Daftar Paket" (toolbar tambah + grid berpaging berisi paket yang sudah
	 * dipilih) di dalam {@code component} induk, lalu memuat datanya. Tombol "Tambah Data"
	 * disembunyikan bila user yang login adalah dosen (hanya admin/staf yang boleh menambah).
	 *
	 * @param jurusanSekolahMahasiswaBaru pemetaan jurusan sekolah asal yang datanya ditampilkan
	 * @param component                   komponen induk ZK; isinya dibersihkan lebih dulu lewat
	 *                                     {@link Common#clear(Component)}
	 * @param window                      window pemanggil, diteruskan ke {@link AmbilPaketHelper}
	 *                                     saat menambah data
	 */
	public void displayDetailPaketJurusan(final JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru,
			final Component component, final MyWindow window) {
		this.jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaru;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Daftar Paket");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.setVisible(Common.getCurrentUser().getDosen() == null);
		button.addEventListener("onClick", new EventListener() {

			private AmbilPaketHelper ambilPaketHelper = new AmbilPaketHelper();

			@Override
			public void onEvent(Event event) throws Exception {

				ambilPaketHelper.display(jurusanSekolahMahasiswaBaru, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("90%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
