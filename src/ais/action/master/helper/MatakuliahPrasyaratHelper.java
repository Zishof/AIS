package ais.action.master.helper;
import ais.common.PesanFormalHelper;

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
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper tampilan "Daftar Matakuliah Prasyarat" pada layar detail {@link Matakuliah}:
 * menampilkan grid berpaging berisi mata kuliah prasyarat ({@link MatakuliahPrasyarat})
 * milik satu mata kuliah, memungkinkan pengambilan prasyarat baru (lewat
 * {@link AmbilDataMatakuliahPrasyaratHelper}), pengeditan nilai minimal lulus inline, dan
 * penghapusan baris.
 *
 * <p>
 * Sejak baris kode prasyarat dapat berisi entri "tanpa prasyarat" — {@code matakuliah}
 * pada {@link MatakuliahPrasyarat} boleh {@code null} dan hanya menyimpan syarat nilai/SKS
 * minimal — render baris menangani kasus tersebut dengan menampilkan placeholder
 * {@code "-"}/{@code "(tanpa prasyarat)"}.
 * </p>
 */
public class MatakuliahPrasyaratHelper implements DataLoader {

	/** Grid daftar mata kuliah prasyarat, dirender ulang oleh {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Mata kuliah yang daftar prasyaratnya sedang ditampilkan. */
	private Matakuliah matakuliah;

	/** Perender baris grid: menampilkan detail mata kuliah prasyarat, kolom nilai minimal lulus yang dapat diedit langsung, dan tombol hapus baris. */
	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link MatakuliahPrasyarat}: kode/nama/SKS/jurusan/jenis
		 * mata kuliah prasyarat (atau placeholder bila prasyarat {@code null}), kotak
		 * angka nilai minimal lulus yang langsung menyimpan perubahan on-change lewat
		 * {@link Common#refreshUpdate}, dan tombol hapus yang mengonfirmasi lalu
		 * menghapus baris via {@link Common#refreshDelete} (dengan pesan galat ramah
		 * bila penghapusan gagal karena relasi data).
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MatakuliahPrasyarat matakuliahPrasyarat = (MatakuliahPrasyarat) data;

			// Prasyarat kini OPSIONAL: bisa null (baris hanya berisi Minimal Nilai Lulus/SKS/IPK).
			final Matakuliah matakuliah = matakuliahPrasyarat.getMatakuliahPrasyarat();
			new Label(matakuliah == null ? "-" : matakuliah.getKode()).setParent(row);
			new Label(matakuliah == null ? "(tanpa prasyarat)" : matakuliah.getNama()).setParent(row);
			new Label(matakuliah == null ? "" : matakuliah.getSks() + "").setParent(row);
			new Label(matakuliah == null || matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama())
					.setParent(row);
			new Label(matakuliah == null ? "" : matakuliah.getJenisMatakuliah()).setParent(row);

			final MyDoublebox minimalNilaiLulus = new MyDoublebox(matakuliahPrasyarat.getMinimalNilaiLulus());
			minimalNilaiLulus.setParent(row);
			minimalNilaiLulus.setWidth("90%");

			minimalNilaiLulus.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliahPrasyarat.setMinimalNilaiLulus(minimalNilaiLulus.getValue());
					Common.refreshUpdate(matakuliahPrasyarat);
				}
			});

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
									Common.refreshDelete(matakuliahPrasyarat); 

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
	 * Memuat ulang daftar {@link MatakuliahPrasyarat} aktif milik {@link #matakuliah}
	 * (diurutkan berdasarkan id) dan mengikatnya ke {@link #grid} lewat
	 * {@link DetailMatakuliahRenderer}. Dipanggil saat tampilan pertama kali dibangun
	 * dan setelah operasi tambah/hapus prasyarat.
	 *
	 * @param value tidak digunakan (parameter kontrak {@link DataLoader})
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<MatakuliahPrasyarat> matakuliahPrasyarat = session.createCriteria(MatakuliahPrasyarat.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).add(Restrictions.eq("matakuliah", matakuliah)).list();

		ListModel strset = new SimpleListModel(matakuliahPrasyarat);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** Mengembalikan diri sendiri sebagai {@link DataLoader}, dipakai sebagai callback refresh untuk {@link AmbilDataMatakuliahPrasyaratHelper}. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun tampilan panel "Daftar Matakuliah Prasyarat" ke dalam {@code component}:
	 * toolbar tombol "Ambil Matakuliah Prasyarat" (membuka {@link AmbilDataMatakuliahPrasyaratHelper}),
	 * grid berpaging dengan kolom Kode/Nama/SKS/Jurusan/Keberadaan/Nilai Lulus, lalu
	 * memuat data awal lewat {@link #loadData(Object)}.
	 *
	 * @param matakuliah mata kuliah yang prasyaratnya ditampilkan
	 * @param component  komponen ZK induk yang akan diisi ulang (dibersihkan lebih dulu)
	 * @param window     jendela induk, diteruskan ke {@link AmbilDataMatakuliahPrasyaratHelper}
	 */
	public void display(final Matakuliah matakuliah, final Component component, final MyWindow window) {
		this.matakuliah = matakuliah;
		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar matakuliah prasyarat");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Matakuliah Prasyarat", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMatakuliahPrasyaratHelper ambilDataMatakuliahHelper = new AmbilDataMatakuliahPrasyaratHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataMatakuliahHelper.display(matakuliah, getDataloader(), window);
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
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Lulus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");
		loadData(null);

	}

}
