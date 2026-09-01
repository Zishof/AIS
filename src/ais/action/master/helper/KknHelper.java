package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import ais.database.dao.MahasiswaDapatKknDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kkn;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKkn;

/**
 * Helper ZK sederhana untuk menampilkan dan mengelola daftar mahasiswa peserta satu kegiatan
 * {@link Kkn} (Kuliah Kerja Nyata), lewat baris relasi {@link MahasiswaDapatKkn}. Grid berpaginasi
 * menampilkan NIM, nama, Jurusan, dan Fakultas tiap peserta dengan tombol hapus per baris; tombol
 * "Tambah Data" pada toolbar membuka {@link AmbilDataMahasiswaKknHelper} untuk menambah peserta baru.
 */
public class KknHelper implements DataLoader {

	private MyGrid grid;
	private Kkn kkn;

	/**
	 * Renderer lokal untuk layar/komponen {@link KknHelper}. Kelas ini menerjemahkan satu item data menjadi baris
	 * atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KknHelper} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KknHelper
	 */
	class DetailKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDapatKkn mahasiswaDapatKkn = (MahasiswaDapatKkn) data;

			Mahasiswa mahasiswa = mahasiswaDapatKkn.getMahasiswa();

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
											MahasiswaDapatKknDao kknDao = DaoFactory.getInstance()
													.getMahasiswaDapatKknDao();
											// kknDao.beginTransaction();
											kknDao.delete((mahasiswaDapatKkn));
											// kknDao.commitTransaction();

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
	 * Implementasi {@link DataLoader}: memuat ulang seluruh {@link MahasiswaDapatKkn} milik
	 * {@link #kkn} ke {@link #grid}.
	 *
	 * @param value tidak digunakan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<MahasiswaDapatKkn> mahasiswaDapatKkn = session.createCriteria(MahasiswaDapatKkn.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("kkn", kkn)).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatKkn);
		grid.setRowRenderer(new DetailKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Menampilkan panel daftar peserta {@code kkn} dengan tombol "Tambah Data".
	 *
	 * @param kkn       kegiatan KKN yang pesertanya akan ditampilkan
	 * @param component komponen ZK induk tempat panel dirender (dibersihkan lebih dulu)
	 * @param window    jendela induk, diteruskan ke {@link AmbilDataMahasiswaKknHelper} saat menambah peserta
	 */
	public void display(final Kkn kkn, final Component component, final MyWindow window) {
		this.kkn = kkn;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Daftar mahasiswa yang mengikuti KKN");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					new AmbilDataMahasiswaKknHelper().display(kkn, getDataloader(), window);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "KknHelper membuka dialog tambah peserta KKN");
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Form tambah peserta KKN belum dapat dibuka. Silakan coba kembali.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
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

	}

}
