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
import ais.database.dao.DetailperkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.Perkuliahan;

/**
 * Helper composer ZK yang menampilkan daftar mahasiswa peserta satu {@link Perkuliahan} (kelas
 * matakuliah) beserta rekap nilai per komponen. Meski nama kelas menyebut "Absensi", isi grid yang
 * ditampilkan sebenarnya adalah baris {@link Detailperkuliahan} per mahasiswa dengan satu kolom per
 * {@link FormatNilai} (komponen penilaian) matakuliah tersebut, ditambah kolom total nilai dan
 * nilai huruf — bukan rekap kehadiran per pertemuan.
 *
 * <p>
 * Hanya menampilkan mahasiswa yang belum "mengikuti perkuliahan" secara resmi
 * ({@code ikutiPerkuliahan} null) — yaitu peserta yang baru terdaftar di {@code Detailperkuliahan}
 * tapi belum diproses lebih lanjut. Alur pemakaian: pemanggil memanggil
 * {@link #displayDetailPerkuliahan} untuk membangun UI, lalu data dimuat lewat
 * {@link #loadData(Object)}. Penambahan peserta dilakukan lewat {@link AmbilDataMahasiswaHelper};
 * setiap baris punya tombol hapus (lewat {@link DetailperkuliahanDao}) dan tombol ubah nilai (buka
 * {@link PenilaianHelper}).
 * </p>
 */
public class DetailAbsensiHelper implements DataLoader {

	/** Grid utama (mode paging) yang menampilkan daftar peserta {@link #perkuliahan}. */
	private MyGrid grid;
	/** Kelas matakuliah yang peserta dan nilainya sedang ditampilkan; diisi oleh {@link #displayDetailPerkuliahan}. */
	private Perkuliahan perkuliahan;
	/** Daftar komponen penilaian {@link #perkuliahan}, satu kolom grid dibuat per elemen. */
	private List<FormatNilai> formatNilais;
	/** Window pemanggil, diteruskan ke {@link AmbilDataMahasiswaHelper} dan {@link PenilaianHelper} saat membuka dialog anak. */
	private MyWindow window;
	/** Penanda semester pendek, diteruskan ke {@link AmbilDataMahasiswaHelper} agar daftar mahasiswa yang ditawarkan sesuai jenis semester. */
	private Integer semesterPendek;

	/**
	 * @param semesterPendek penanda semester pendek, diteruskan ke {@link AmbilDataMahasiswaHelper}
	 *                       saat menambah peserta agar daftar mahasiswa yang ditawarkan sesuai
	 *                       jenis semester
	 */
	public DetailAbsensiHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Perender baris grid untuk satu {@link Detailperkuliahan}: menampilkan NIM, nama mahasiswa,
	 * nilai per {@link FormatNilai} (lewat {@link Detailperkuliahan#retreiveDetailNilai}), total
	 * nilai + nilai huruf, serta tombol hapus dan tombol ubah nilai (membuka
	 * {@link PenilaianHelper}).
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu {@link Detailperkuliahan}: NIM, nama mahasiswa, nilai
		 * per {@link FormatNilai}, total + nilai huruf, tombol hapus, dan tombol ubah nilai.
		 *
		 * @param row  baris grid yang akan diisi
		 * @param data data baris, harus berupa {@link Detailperkuliahan}
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) data;

			new Label(detailperkuliahan.getMahasiswa().getNim()).setParent(row);
			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
			for (FormatNilai formatNilai : formatNilais) {
				Double n = detailperkuliahan.retreiveDetailNilai(formatNilai);
				new Label(n + "").setParent(row);
			}
			Double totalNilai = detailperkuliahan.getTotalNilai();
			System.out.println("totalNilai = " + totalNilai);
			try {
				ais.ui.util.NilaiHurufAnalisisPopupHelper
						.buatLabel(detailperkuliahan.getTotalNilai() + "(" + detailperkuliahan.getNilaiHuruf() + ")",
								detailperkuliahan)
						.setParent(row);
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailAbsensiHelper.java:66");
			}

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
											DetailperkuliahanDao perkuliahanDao = DaoFactory.getInstance()
													.getDetailperkuliahanDao();
											// perkuliahanDao.beginTransaction();
											perkuliahanDao.delete(perkuliahanDao.merge(detailperkuliahan));
											// perkuliahanDao.commitTransaction();

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
			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {

				private PenilaianHelper penilaianHelper = new PenilaianHelper();

				@Override
				public void onEvent(Event event) throws Exception {
					penilaianHelper.display(detailperkuliahan, window, getDataloader());
				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Memuat ulang isi grid dengan seluruh {@link Detailperkuliahan} milik {@link #perkuliahan}
	 * yang belum resmi mengikuti perkuliahan ({@code ikutiPerkuliahan} null), diurutkan
	 * berdasarkan id.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> detailperkuliahan = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan")).addOrder(Order.asc("id"))
				.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

		ListModel strset = new SimpleListModel(detailperkuliahan);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @return {@code this} sebagai {@link DataLoader}, dipakai saat meneruskan callback muat-ulang ke helper anak. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun UI panel "Daftar mahasiswa yang mengikuti perkuliahan ..." di dalam
	 * {@code component} induk: kolom NIM, nama, satu kolom per {@link FormatNilai} matakuliah
	 * (lebar kolom nama menyusut otomatis sesuai jumlah komponen nilai), kolom total, dan kolom
	 * aksi. Lalu memuat datanya.
	 *
	 * @param perkuliahan kelas matakuliah yang peserta dan nilainya ditampilkan/dikelola
	 * @param component   komponen induk ZK; isinya dibersihkan lebih dulu lewat
	 *                    {@link Common#clear(Component)}
	 * @param window      window pemanggil, diteruskan ke {@link AmbilDataMahasiswaHelper} dan
	 *                    {@link PenilaianHelper}
	 */
	public void displayDetailPerkuliahan(final Perkuliahan perkuliahan, final Component component,
			final MyWindow window) {
		this.perkuliahan = perkuliahan;
		this.window = window;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString());
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaHelper dataMahasiswaHelper = new AmbilDataMahasiswaHelper(perkuliahan,
						semesterPendek);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(panelchildren);

		formatNilais = Common.getFormatNilais(HibernateUtil.currentSession(), perkuliahan);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth((75 - (formatNilais.size() * 5)) + "%");

		for (FormatNilai formatNilai : formatNilais) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel(formatNilai.getStatusPertemuan() == null ? "" : formatNilai.getNama());
			column.setWidth("5%");
		}
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
