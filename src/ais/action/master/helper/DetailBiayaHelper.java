package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatanDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK untuk menampilkan dan mengelola daftar {@link DetailBiaya} (rincian
 * item/nilai biaya) milik satu {@link JenisKegiatanDetail}. Membangun grid ber-paging berisi
 * kolom item biaya, jenis biaya, dan nilai, lengkap dengan tombol tambah data (membuka
 * {@code AmbilDetailBiayaHelper}) dan tombol hapus per baris (dengan konfirmasi dan penanganan
 * galat integritas referensial via {@link PesanFormalHelper}).
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} sehingga dapat memuat ulang datanya sendiri
 * ({@link #loadData(Object)}) setelah operasi tambah/hapus, dan meneruskan dirinya sendiri
 * sebagai {@code DataLoader} ke helper penambah data agar grid otomatis menyegarkan tampilan
 * setelah data baru disimpan.
 * </p>
 */
public class DetailBiayaHelper implements DataLoader {

	private MyGrid grid;
	private JenisKegiatanDetail jenisKegiatanDetail;
	private DetailBiaya[] detailBiayas;

	/** Perender baris grid: menampilkan kolom item biaya, jenis biaya, nilai, dan tombol hapus. */
	class DetailBiayaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailBiayaRenderer() {

		}

		/**
		 * Merender satu baris {@link DetailBiaya}: label nama item biaya, nama jenis kegiatan, nilai
		 * biaya (default {@code 0.0} bila {@code null}), dan tombol hapus yang meminta konfirmasi
		 * sebelum menghapus data via {@link DetailBiayaDao} lalu memuat ulang grid.
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DetailBiaya detailBiaya = (DetailBiaya) data;

			new Label(detailBiaya.getItemBiaya().getNama()).setParent(row);
			new Label(detailBiaya.getJenisKegiatan().getNamaKegiatan()).setParent(row);
			new Label((detailBiaya.getNilaiBiaya() == null ? new Double(0.0) : detailBiaya.getNilaiBiaya()).toString())
					.setParent(row);

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
									DetailBiayaDao detailBiayaDao = DaoFactory.getInstance().getDetailBiayaDao();
									// detailBiayaDao.beginTransaction();
									detailBiayaDao.delete(detailBiayaDao.merge(detailBiaya));
									// detailBiayaDao.commitTransaction();

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
			// button.setParent(toolbar);
			// toolbar.setParent(south);

		}

	}

	/**
	 * Memuat ulang isi grid dari {@link #detailBiayas} (array yang sudah diambil sebelumnya di
	 * {@link #displayDetailBiaya}). Parameter {@code value} tidak dipakai; ada semata untuk
	 * memenuhi kontrak antarmuka {@link DataLoader}.
	 *
	 * @param value tidak digunakan
	 */
	public void loadData(Object value) {

		ListModel strset = new SimpleListModel(detailBiayas);
		grid.setRowRenderer(new DetailBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @return referensi ke helper ini sendiri, dipakai sebagai {@link DataLoader} oleh helper penambah data. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun dan menampilkan seluruh UI rincian biaya (toolbar tombol tambah + grid ber-paging)
	 * untuk satu {@link JenisKegiatanDetail} ke dalam {@code component} yang diberikan.
	 * {@code jenisKegiatanDetail} dimuat ulang dari sesi Hibernate saat ini agar koleksi
	 * {@code detailBiayas}-nya lazim-load dengan aman, lalu {@code component} dibersihkan
	 * ({@link Common#clear(Component)}) sebelum diisi ulang.
	 *
	 * @param jenisKegiatanDetail entitas induk yang rincian biayanya akan ditampilkan
	 * @param component           komponen ZK tujuan (akan dibersihkan lalu diisi ulang)
	 * @param window              window pemanggil, diteruskan ke helper tambah data untuk konteks tampilan
	 */
	public void displayDetailBiaya(final JenisKegiatanDetail jenisKegiatanDetail, final Component component,
			final MyWindow window) {
		this.jenisKegiatanDetail = (JenisKegiatanDetail) HibernateUtil.currentSession().load(JenisKegiatanDetail.class,
				jenisKegiatanDetail.getId());
		Common.clear(component);

		detailBiayas = this.jenisKegiatanDetail.getDetailBiayas().toArray(new DetailBiaya[] {});
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDetailBiayaHelper ambilDetailBiayaHelper = new AmbilDetailBiayaHelper(jenisKegiatanDetail);
				ambilDetailBiayaHelper.display(jenisKegiatanDetail.getJenisKegiatan(), getDataloader(), window);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Biaya");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("35%");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("");
		// column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
