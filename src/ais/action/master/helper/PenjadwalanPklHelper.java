package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper jendela penjadwalan pertemuan PKL (Praktik Kerja Lapangan) untuk satu
 * {@link KelompokPkl} (kelompok mahasiswa PKL). Menampilkan grid pertemuan dengan
 * field topik/tanggal/waktu/jenis pertemuan yang dapat diedit inline (langsung tersimpan
 * on-change), tombol tambah pertemuan (tanggal baru otomatis +7 hari dari pertemuan
 * terakhir), serta tombol bersama {@link PenjadwalanHelper} untuk ambil jadwal massal,
 * atur ulang waktu, unduh, dan hapus semua pertemuan kelompok.
 */
public class PenjadwalanPklHelper {

	private KelompokPkl kelompokPkl;
	private MyGrid grid;
	private DataLoader dataLoader;

	/** Tanggal pertemuan terakhir yang dirender/ditambahkan, dipakai untuk menghitung tanggal +7 hari saat menambah pertemuan baru. */
	private Date currDate;

	/** Perender baris grid pertemuan PKL: field edit inline topik/tanggal/waktu/jenis pertemuan, plus tombol hapus. */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		public PertemuanRenderer() {

		}

		/**
		 * Merender satu baris {@link Pertemuan}: textbox topik, datebox tanggal
		 * (readonly), sepasang timebox waktu mulai/selesai, dan combobox
		 * {@link StatusPertemuan} — seluruhnya langsung menyimpan perubahan via
		 * {@link Common#refreshUpdate} saat berubah — serta tombol hapus yang mengecek
		 * {@link PenjadwalanHelper#checkBolehHapus(Pertemuan)}, mengonfirmasi, lalu
		 * menghapus pertemuan (dengan pesan galat ramah bila gagal karena relasi data).
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			arg0.setAttribute("myValue", pertemuan);

			final Textbox topik = new Textbox();
			topik.setValue(pertemuan.getTopik() == null ? "" : pertemuan.getTopik());
			topik.setParent(arg0);
			topik.setWidth("90%");
			topik.setRows(2);
			topik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setTopik(topik.getValue());
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
				waktuMulai
						.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanPklHelper.java:107");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			final Timebox waktuSelesai;
			hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuSelesai.setValue(
						pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanPklHelper.java:119");

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

			final Combobox ujian = new Combobox();
			Common.insertCombo(ujian, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(ujian, pertemuan.getStatusPertemuan());
			ujian.setReadonly(true);
			arg0.appendChild(ujian);
			ujian.setWidth("90%");
			ujian.setReadonly(true);
			ujian.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// KE-FIX (NullPointerException): getSelectedItem() bisa null (mis. combobox
					// readonly sempat kosong sesaat/onChange terpicu sebelum seleksi settle).
					if (ujian.getSelectedItem() == null) {
						return;
					}
					pertemuan.setStatusPertemuan((StatusPertemuan) ujian.getSelectedItem().getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			currDate = pertemuan.getTanggal();

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
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
											PertemuanDao pertemuanDao = DaoFactory.getInstance().getPertemuanDao();
											// pertemuanDao.beginTransaction();

											pertemuanDao.delete((pertemuan));
											// pertemuanDao.commitTransaction();
											kelompokPkl.belum();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data jadwal pertemuan PKL",
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
	 * Memuat ulang cache pertemuan pada {@link #kelompokPkl} dan memberi tahu pemanggil
	 * lewat {@link #dataLoader} bahwa data telah berubah. Perubahan baris itu sendiri
	 * sudah tersimpan langsung on-change lewat {@link PertemuanRenderer}, sehingga
	 * method ini hanya menyegarkan cache dan menutup jendela lewat pemanggil.
	 *
	 * @return selalu {@code true}
	 * @throws InterruptedException tidak pernah dilempar secara nyata pada implementasi saat ini
	 */
	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		Session session = HibernateUtil.currentSession();

		kelompokPkl.reInitPertemuan(session);

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membangun dan menampilkan jendela modal penjadwalan PKL untuk {@code kelompokPkl}:
	 * toolbar "Tambah Kegiatan PKL" (menambah baris {@link Pertemuan} baru langsung ke
	 * grid dengan tanggal +7 hari dari pertemuan terakhir), Refresh, serta tombol
	 * bersama {@link PenjadwalanHelper} (ambil jadwal, atur ulang waktu, unduh, hapus
	 * semua), grid pertemuan, dan tombol Simpan/Batal.
	 *
	 * @param kelompokPkl kelompok PKL yang dijadwalkan
	 * @param dataLoader  callback pemuatan ulang data pemanggil setelah Simpan
	 */
	public void display(final KelompokPkl kelompokPkl, final DataLoader dataLoader) {
		this.kelompokPkl = kelompokPkl;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(kelompokPkl.infoSimple());

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Kegiatan PKL", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyFormRow row = new MyFormRow();row.setValign("top");
				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				rows.setParent(grid);
				row.setParent(rows);

				Date myDate = ais.ui.util.WaktuUtil.getDate();
				if (currDate != null) {
					Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
					myCalendar.setTime(currDate);
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
					currDate = myCalendar.getTime();
				} else {
					currDate = myDate;
				}

				Pertemuan pertemuan = new Pertemuan();

				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
				pertemuan.setTanggal(currDate);
				pertemuan.setKelompokPkl(kelompokPkl);
				pertemuan.setTopik("Topik PKL ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.timeFormat2.get().format(currDate));
				pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(currDate));

				pertemuan.setKelompokPkl(kelompokPkl);

				Session session = HibernateUtil.currentSession();

				session.save(pertemuan);

				onSearchDefault(event);

			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kelompokPkl.reInitPertemuan(HibernateUtil.currentSession());
						onSearchDefault(new Event("onTimer", arg0.getTarget(), true));
					}
				});
			}
		});

		PenjadwalanHelper.tampilTombolAmbil(toolbar, null, null, kelompokPkl, null, null, null, null, null);
		PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, null, null, kelompokPkl, null, null, null, null, null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, null, null, kelompokPkl, null, null, null, null,
				null);

		PenjadwalanHelper.tampilTombolHapus(toolbar, null, null, kelompokPkl, null, null, null, null, null,
				new EventListener() {

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

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (save()) {
					window.detach();
				}
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memuat ulang grid pertemuan {@link #kelompokPkl}.
	 *
	 * @param event bila data-nya bertipe {@link Boolean}, diteruskan ke {@link KelompokPkl#ambilPertemuanList(boolean)} (mis. untuk memaksa reload); selain itu diperlakukan sebagai {@code false}
	 */
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = kelompokPkl.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
