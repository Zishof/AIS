package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.JenisKegiatanDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisKegiatanDetail;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper CRUD untuk daftar {@link JenisKegiatanDetail} (kombinasi Fakultas+Jurusan) yang
 * berlaku pada satu {@link JenisKegiatan}. Menampilkan grid berpaging dengan aksi
 * tambah/ubah/hapus, di mana form tambah/ubah dirender ke jendela modal terpisah
 * ({@link #init(JenisKegiatanDetail)}) dan setiap baris grid dapat dibuka ({@link MyDetail})
 * untuk mengelola rincian biaya terkait lewat {@link DetailBiayaHelper}.
 */
public class DetailJenisKegiatanHelper implements DataLoader {

	private MyGrid grid;
	private JenisKegiatan jenisKegiatan;
	private MyWindow addWindow;
	private JenisKegiatanDetail jenisKegiatanDetail;

	private Combobox fakultas;
	private Combobox jurusan;

	/** Perender baris grid: detail biaya yang dapat dibuka, label Fakultas/Jurusan, dan tombol ubah/hapus. */
	class JenisKegiatanDetailRenderer extends ais.ui.util.MyRowRenderer {

		// private AmbilDetailBiayaHelper ambilDetailBiayaHelper = new
		// AmbilDetailBiayaHelper();

		public JenisKegiatanDetailRenderer() {

		}

		/**
		 * Merender satu baris {@link JenisKegiatanDetail}: komponen {@link MyDetail}
		 * yang saat dibuka menampilkan rincian biaya via {@link DetailBiayaHelper},
		 * label nama Fakultas dan Jurusan, tombol ubah (membuka form
		 * {@link #init(JenisKegiatanDetail)}) dan tombol hapus (dengan konfirmasi dan
		 * pesan galat ramah bila gagal karena relasi data).
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final JenisKegiatanDetail jenisKegiatanDetail = (JenisKegiatanDetail) data;
			// Common.clear(addWindow);
			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// AmbilDetailBiayaHelper ambilDetailBiayaHelper = new
					// AmbilDetailBiayaHelper(
					// jenisKegiatanDetail);
					DetailBiayaHelper detailBiayaHelper = new DetailBiayaHelper();
					detailBiayaHelper.displayDetailBiaya(jenisKegiatanDetail, detail, addWindow);
				}
			});

			new Label(jenisKegiatanDetail.getFakultas().getNama()).setParent(row);
			new Label(jenisKegiatanDetail.getJurusan().getNama()).setParent(row);

			Hbox toolbar = new Hbox();
			;

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenisKegiatanDetail);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
											JenisKegiatanDetailDao jenisKegiatanDetailDao = DaoFactory.getInstance()
													.getJenisKegiatanDetailDao();
											// jenisKegiatanDetailDao.beginTransaction();
											jenisKegiatanDetailDao.delete((jenisKegiatanDetail));
											// jenisKegiatanDetailDao.commitTransaction();

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
	 * Memuat ulang daftar {@link JenisKegiatanDetail} milik {@link #jenisKegiatan}
	 * (diurutkan berdasarkan id) dan mengikatnya ke {@link #grid}.
	 *
	 * @param value tidak digunakan (parameter kontrak {@link DataLoader})
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<JenisKegiatanDetail> jenisKegiatanDetail = session.createCriteria(JenisKegiatanDetail.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).list();

		ListModel strset = new SimpleListModel(jenisKegiatanDetail);
		grid.setRowRenderer(new JenisKegiatanDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun panel "Detail Jenis Kegiatan" ke dalam {@code component}: toolbar tombol
	 * "Tambah Data" (membuka form {@link #init(JenisKegiatanDetail)} dengan entitas
	 * baru) dan grid berpaging dengan kolom Fakultas/Jurusan, lalu memuat data awal.
	 *
	 * @param jenisKegiatan jenis kegiatan yang detailnya ditampilkan
	 * @param component     komponen ZK induk yang akan diisi ulang (dibersihkan lebih dulu)
	 * @param window        jendela modal yang dipakai bersama untuk form tambah/ubah
	 */
	public void displayJenisKegiatanDetail(final JenisKegiatan jenisKegiatan, final Component component,
			final MyWindow window) {

		this.jenisKegiatan = jenisKegiatan;
		Common.clear(component);
		this.addWindow = window;
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Detail Jenis Kegiatan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				init(new JenisKegiatanDetail());
				window.setVisible(true);
				window.onModal();
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
		column.setLabel("-");
		column.setWidth("1%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("50%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	/**
	 * Membangun form tambah/ubah {@link JenisKegiatanDetail} ke dalam {@link #addWindow}:
	 * combobox Fakultas (aktif saja) yang saat berubah mengisi ulang combobox Prodi
	 * (Jurusan) sesuai fakultas terpilih, serta tombol Batal/Simpan
	 * ({@link #onSave(Event)}).
	 *
	 * @param jenisKegiatanDetail entitas yang diedit (baru atau sudah tersimpan), disimpan ke {@link #jenisKegiatanDetail}
	 */
	private void init(JenisKegiatanDetail jenisKegiatanDetail) {
		this.jenisKegiatanDetail = jenisKegiatanDetail;
		addWindow.setTitle("Form Detail Jenis Kegiatan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		/*
		 * MyFormRow row = new MyFormRow();row.setValign("top"); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		 * Common.insertCombo(fakultas=new Combobox(), new String[]{"nama", "kode"},
		 * Fakultas.class);
		 * 
		 * row.appendChild(fakultas); fakultas.setWidth("90%");
		 * 
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"+""));
		 * Common.insertCombo(jurusan=new Combobox(), "nama", Jurusan.class,
		 * Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
		 * true))); row.appendChild(jurusan); jurusan.setWidth("90%");
		 */

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(fakultas = new Combobox(), new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());
		row.appendChild(fakultas);
		// row.appendChild(jurusan);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		// Common.insertCombo(jurusan=new Combobox(), "nama", Jurusan.class,
		// Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
		// true)));
		row.appendChild(jurusan = new Combobox());
		jurusan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onSearchDefault(null);
					loadData(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	/**
	 * Menyimpan (insert/update) {@link #jenisKegiatanDetail} dari form: memuat ulang
	 * entitas dari database bila sudah punya id (menghindari overwrite data basi),
	 * mengisi Fakultas/Jurusan/{@link #jenisKegiatan} dari combobox terpilih, lalu
	 * update atau save sesuai ada tidaknya id.
	 *
	 * @param event tidak digunakan langsung (parameter kontrak listener tombol Simpan)
	 * @return selalu {@code true} (tidak ada jalur validasi yang mengembalikan {@code false})
	 * @throws Exception diteruskan dari akses DAO/Hibernate
	 */
	public boolean onSave(Event event) throws Exception {

		JenisKegiatanDetailDao jenisKegiatanDetailDao = DaoFactory.getInstance().getJenisKegiatanDetailDao();
		if (jenisKegiatanDetail.getId() != null) {
			jenisKegiatanDetail = jenisKegiatanDetailDao.load(jenisKegiatanDetail.getId());
		}
		jenisKegiatanDetail.setFakultas((Fakultas) fakultas.getSelectedItem().getValue());
		jenisKegiatanDetail.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		jenisKegiatanDetail.setJenisKegiatan(jenisKegiatan);

		// jenisKegiatanDetailDao.beginTransaction();
		if (jenisKegiatanDetail.getId() != null) {
			jenisKegiatanDetailDao.update(jenisKegiatanDetail);
		} else {
			jenisKegiatanDetailDao.save(jenisKegiatanDetail);
		}
		// jenisKegiatanDetailDao.commitTransaction();
		return true;
	}

}
