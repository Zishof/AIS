package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupPertemuan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper jendela penjadwalan untuk satu {@link GrupPertemuan} (grup pertemuan/konsultasi,
 * mis. bimbingan dosen PA): menampilkan dan mengelola daftar peserta grup, di mana setiap
 * peserta ({@link Mahasiswa}) memiliki satu {@link Pertemuan} individual sendiri yang
 * ditautkan lewat baris {@link PertemuanPunyaGrupPertemuan}. Menyediakan pencarian
 * peserta by NIM/nama, unduh data, tambah peserta (lewat
 * {@code AmbilDataMahasiswaForGrupPertemuanDosenPaHelper}), dan pengeditan
 * catatan/tanggal/waktu pertemuan langsung pada grid, serta penghapusan peserta+pertemuan
 * terkait.
 */
public class PenjadwalanGrupPertemuanHelper {

	private GrupPertemuan grupPertemuan;
	private MyGrid grid;
	private Textbox nama;

	/** Membuat helper untuk satu {@code grupPertemuan} yang akan dikelola. */
	public PenjadwalanGrupPertemuanHelper(GrupPertemuan grupPertemuan) {
		this.grupPertemuan = grupPertemuan;
	}

	/**
	 * Merender komponen edit inline untuk satu {@link PertemuanPunyaGrupPertemuan} ke
	 * dalam {@code arg0}: foto+nama mahasiswa dengan riwayat revisi, textbox catatan,
	 * datebox tanggal (readonly — hanya bisa diubah lewat aksi lain), dan sepasang
	 * timebox waktu mulai/selesai — seluruhnya langsung menyimpan perubahan ke
	 * {@link Pertemuan} terkait via {@link Common#refreshUpdate} saat berubah.
	 *
	 * @param arg0                        komponen ZK induk (biasanya satu baris grid) tempat elemen disisipkan
	 * @param pertemuanPunyaGrupPertemuan entri keanggotaan grup yang direpresentasikan
	 * @throws Exception diteruskan dari operasi tampilan
	 */
	public static void displayRow(Component arg0, final PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan)
			throws Exception {
		final Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();
		final Mahasiswa mahasiswa = pertemuanPunyaGrupPertemuan.getMahasiswa();
		arg0.setAttribute("myValue", pertemuanPunyaGrupPertemuan.getPertemuan());

		Hbox vb = new Hbox();
		vb.setParent(arg0);
		vb.appendChild(CommonMedia.tampilkanGambarKecil(mahasiswa));
		Vbox a = RevisiHelper.createNewRevisi(PertemuanPunyaGrupPertemuan.class, pertemuanPunyaGrupPertemuan,
				mahasiswa.getNim());

		a.appendChild(new Label(mahasiswa.getNama()));
		vb.appendChild(a);

		final Textbox catatan = new Textbox();
		catatan.setValue(pertemuan.getCatatan() == null ? "" : pertemuan.getCatatan());
		catatan.setParent(arg0);
		catatan.setWidth("90%");
		catatan.setRows(2);
		catatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pertemuan.setCatatan(catatan.getValue());
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session, (pertemuan));
			}
		});

		final MyDatebox tanggal = new MyDatebox();
		tanggal.setValue(pertemuan.getTanggal());
		tanggal.setWidth("90%");
		tanggal.setParent(arg0);
		tanggal.setReadonly(true);
		tanggal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pertemuan.setTanggal(tanggal.getValue());
				pertemuan.setTanggalEdit(tanggal.getValue());
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session, (pertemuan));
			}
		});

		Hbox hbox = new Hbox();
		arg0.appendChild(hbox);

		final Timebox waktuMulai;
		hbox.appendChild(waktuMulai = new ais.ui.util.MyTimebox());
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuMulai.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
					: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanGrupPertemuanHelper.java:111");

		}

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		final Timebox waktuSelesai;
		hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuSelesai
					.setValue(pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanGrupPertemuanHelper.java:123");

		}

		waktuMulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pertemuan.setWaktuMulai(
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()));
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session, (pertemuan));
			}
		});

		waktuSelesai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pertemuan.setWaktuSelesai(
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()));
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session, (pertemuan));
			}
		});
	}

	/** Perender baris grid peserta grup: field edit inline via {@link #displayRow} plus tombol hapus. */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		public PertemuanRenderer() {

		}

		/**
		 * Merender satu baris {@link PertemuanPunyaGrupPertemuan} lewat
		 * {@link #displayRow} lalu menambahkan tombol hapus yang mengecek
		 * {@link PenjadwalanHelper#checkBolehHapus(Pertemuan)}, mengonfirmasi, lalu
		 * menghapus baris keanggotaan dan {@link Pertemuan} terkait sekaligus (dengan
		 * pesan galat ramah bila gagal karena relasi data).
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) arg1;

			PenjadwalanGrupPertemuanHelper.displayRow(arg0, pertemuanPunyaGrupPertemuan);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();
					if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
						return;
					}
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pertemuanPunyaGrupPertemuan);
											Common.refreshDelete(pertemuan);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data jadwal pertemuan",
													e, new String[] {
															"Pastikan tidak ada data lain (mis. nilai, kehadiran) yang masih berelasi dengan data ini.",
															"Muat ulang (refresh) halaman ini lalu coba hapus kembali.",
															"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
													});
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	/**
	 * Membangun dan menampilkan jendela modal penjadwalan grup pertemuan: toolbar
	 * "Tambah Peserta Konsultasi", tombol unduh data, pencarian by NIM/nama, grid
	 * peserta, dan tombol Tutup yang memanggil {@code eventListener} sebelum menutup
	 * jendela.
	 *
	 * @param eventListener callback yang dipanggil (dengan event {@code null}) saat jendela ditutup
	 */
	public void display(final EventListener eventListener) {

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(grupPertemuan.getNama());

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Peserta Konsultasi", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataMahasiswaForGrupPertemuanDosenPaHelper dataMahasiswaHelper = new AmbilDataMahasiswaForGrupPertemuanDosenPaHelper(
						grupPertemuan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						onSearchDefault(null);
					}
				});

			}
		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "grupPertemuan", "mahasiswa", "pertemuan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(null, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				return PenjadwalanGrupPertemuanHelper.this.initCriteria(nama.getValue());
			}
		}, "Download", "/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		nama = new Textbox();
		nama.setCols(10);
		nama.setParent(toolbar);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("", "/img/search.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				eventListener.onEvent(null);
				window.detach();
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun kriteria pencarian peserta {@link #grupPertemuan}, disaring opsional
	 * berdasarkan NIM/nama mahasiswa (cocok sebagian, tidak case-sensitive), diurutkan
	 * berdasarkan NIM.
	 *
	 * @param nama kata kunci NIM/nama; boleh kosong/{@code null} untuk tanpa filter
	 * @return kriteria Hibernate atas {@link PertemuanPunyaGrupPertemuan}
	 */
	public Criteria initCriteria(String nama) {
		Session session = HibernateUtil.currentSession();

		Criteria crit = session.createCriteria(PertemuanPunyaGrupPertemuan.class)
				.add(Restrictions.eq("grupPertemuan", grupPertemuan)).createAlias("pertemuan", "pertemuan")
				.createAlias("mahasiswa", "mahasiswa")
				.add(nama != null && !nama.trim().isEmpty()
						? Restrictions.or(Restrictions.ilike("mahasiswa.nim", nama, MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama, MatchMode.ANYWHERE))
						: Restrictions.sqlRestriction("true"))
				.addOrder(Order.asc("mahasiswa.nim"));

		return crit;
	}

	/**
	 * Memuat ulang grid peserta grup pertemuan sesuai filter nama yang sedang terisi
	 * (dibatasi {@link Common#MAX_RESULT_100} baris).
	 *
	 * @param event tidak digunakan; parameter kontrak listener/pemanggilan langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = initCriteria(nama.getValue()).setMaxResults(Common.MAX_RESULT_100).list();
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
