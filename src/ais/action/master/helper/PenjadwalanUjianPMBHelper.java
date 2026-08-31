package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk menjadwalkan sesi ujian PMB (Penerimaan Mahasiswa Baru) dalam satu
 * {@link JadwalUjianPMB} (materi ujian + paket + rentang waktu default). Setiap sesi ujian
 * direpresentasikan sebagai satu baris {@link Pertemuan} yang dapat diedit inline langsung pada
 * grid (topik, tanggal, jam mulai/selesai, dan status/jenis pertemuan dari {@link StatusPertemuan}).
 *
 * <p>
 * Tombol "Tambah Pertemuan" membuat baris {@link Pertemuan} baru langsung tersimpan ke database
 * dengan tanggal default digeser tujuh hari dari pertemuan terakhir yang ditambahkan (memudahkan
 * penjadwalan ujian mingguan berurutan) dan jam mulai/selesai mengikuti jadwal default
 * {@code jadwalUjianPMB}. Setiap baris dapat dihapus individual (setelah dicek boleh dihapus lewat
 * {@link PenjadwalanHelper#checkBolehHapus(Pertemuan)}) atau, saat {@link #save()} dipanggil,
 * baris yang sebelumnya ada di database namun sudah tidak muncul di grid akan turut dihapus
 * (rekonsiliasi penuh antara state grid dan database).
 * </p>
 */
public class PenjadwalanUjianPMBHelper {

	private JadwalUjianPMB jadwalUjianPMB;
	private MyGrid grid;
	private DataLoader dataLoader;

	private Date currDate;

	/**
	 * Renderer lokal untuk layar/komponen {@link PenjadwalanUjianPMBHelper}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenjadwalanUjianPMBHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenjadwalanUjianPMBHelper
	 */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		public PertemuanRenderer() {

		}

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

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			final MyDatebox tanggal = new MyDatebox();

			tanggal.setValue(pertemuan.getTanggal());
			tanggal.setFormat(Common.dateFormat1.get().toPattern());

			tanggal.setWidth("90%");
			tanggal.setParent(vbox);
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

			final Timebox waktuMulai;
			final Timebox waktuSelesai;
			waktuMulai = new ais.ui.util.MyTimebox();
			waktuSelesai = new ais.ui.util.MyTimebox();
			waktuMulai.setFormat(Common.timeFormat.get().toPattern());
			waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

			try {
				waktuMulai
						.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanUjianPMBHelper.java:116");

			}
			try {
				waktuSelesai.setValue(
						pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanUjianPMBHelper.java:123");

			}

			EventListener updateLocal = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setWaktuMulai(
							waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
					pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuSelesai.getValue()));

					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			};

			waktuMulai.setCols(1);
			waktuSelesai.setCols(1);

			waktuMulai.addEventListener("onChange", updateLocal);
			waktuSelesai.addEventListener("onChange", updateLocal);

			arg0.setAttribute("tanggal", tanggal);
			arg0.setAttribute("waktuMulai", waktuMulai);
			arg0.setAttribute("waktuSelesai", waktuSelesai);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			waktuMulai.setParent(hbox);
			waktuSelesai.setParent(hbox);

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
											jadwalUjianPMB.belum();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data jadwal ujian PMB",
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
	 * Menyinkronkan seluruh baris {@link Pertemuan} pada grid ke database: baris yang punya
	 * {@code myValue} (pertemuan sudah ada) diperbarui, baris baru disimpan dengan jam mulai/selesai
	 * mengikuti default {@link #jadwalUjianPMB}, dan pertemuan aktif milik {@link #jadwalUjianPMB}
	 * yang tidak lagi muncul di grid (sudah dihapus pengguna dari tampilan) turut dihapus dari
	 * database. Kegagalan pada satu baris ditelan dan dicatat lewat audit tanpa menghentikan proses
	 * baris lainnya.
	 *
	 * @return selalu {@code true}
	 * @throws InterruptedException tidak pernah dilempar dalam praktiknya; dipertahankan pada
	 *                              signature untuk kompatibilitas pemanggil
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws InterruptedException {

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Session session = HibernateUtil.currentSession();
		jadwalUjianPMB.reInitPertemuan(session);
		List<Pertemuan> pertemuans = jadwalUjianPMB.ambilPertemuanList();

		Map<Long, Pertemuan> map = new java.util.HashMap<Long, Pertemuan>();
		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan.getAktif()) {
				map.put(pertemuan.getId(), pertemuan);
			}
		}

		rows = grid.getRows();
		list = rows.getChildren();
		for (Row row : list) {
			try {
				List data = row.getChildren();
				Textbox topik = (Textbox) data.get(0);
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");

				Timebox waktuMulai = (Timebox) row.getAttribute("waktuMulai");
				Timebox waktuSelesai = (Timebox) row.getAttribute("waktuSelesai");

				Pertemuan pertemuan = (Pertemuan) (row.getAttribute("myValue") == null ? new Pertemuan()
						: row.getAttribute("myValue"));
				if (pertemuan.getAktif()) {
					pertemuan.setTanggal(tanggal.getValue());
					pertemuan.setTanggalEdit(tanggal.getValue());
					pertemuan.setJadwalUjianPMB(jadwalUjianPMB);

					pertemuan.setWaktuMulai(
							waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
					pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuSelesai.getValue()));

					pertemuan.setTopik(topik.getValue());

					if (pertemuan.getId() != null) {
						Common.refreshUpdate(session, (pertemuan));
					} else {
						pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai()));
						pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai()));
						session.save(pertemuan);
					}

					map.remove(pertemuan.getId());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanUjianPMBHelper.java:281");
			}
		}

		for (Pertemuan pertemuan : map.values()) {
			session.delete(pertemuan);
		}

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membangun jendela penjadwalan sesi ujian untuk satu {@link JadwalUjianPMB}: judul menampilkan
	 * ringkasan materi/paket/waktu, toolbar Tambah Pertemuan/Refresh, dan grid baris pertemuan yang
	 * dapat diedit inline. Tombol Simpan memanggil {@link #save()} dan menutup jendela bila berhasil.
	 *
	 * @param jadwalUjianPMB jadwal ujian PMB yang sesi-sesinya akan dikelola
	 * @param dataLoader     dipanggil setelah simpan untuk menyegarkan tampilan pemanggil
	 */
	public void display(final JadwalUjianPMB jadwalUjianPMB, final DataLoader dataLoader) {
		this.jadwalUjianPMB = jadwalUjianPMB;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(jadwalUjianPMB.infoSimple());

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

		String matkul1 = jadwalUjianPMB.getNama();

		String paket = jadwalUjianPMB.getPaket() == null ? "Semua" : jadwalUjianPMB.getPaket().getNama();

		String harijam = (", Waktu : " + Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai()) + " s.d "
				+ Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai()));

		String groupTxt = "Materi Ujian : " + matkul1 + ", Paket : " + paket + harijam;

		window.setTitle(groupTxt);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Pertemuan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyFormRow row = new MyFormRow();
				row.setValign("top");
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
				pertemuan.setJadwalUjianPMB(jadwalUjianPMB);
				pertemuan.setTopik("Ujian ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai()));
				pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai()));

				// pertemuan.setMulai(jadwalUjianPMB.getWaktuMulai());
				// pertemuan.setSelesai(jadwalUjianPMB.getWaktuSampai());

				pertemuan.setJadwalUjianPMB(jadwalUjianPMB);

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
						jadwalUjianPMB.reInitPertemuan(HibernateUtil.currentSession());
						onSearchDefault(new Event(null, arg0.getTarget(), true));
					}
				});
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
	 * Mengisi ulang grid dengan daftar {@link Pertemuan} milik {@link #jadwalUjianPMB}.
	 *
	 * @param event bila datanya berupa {@link Boolean} {@code true}, memaksa refresh cache daftar
	 *              pertemuan ({@code JadwalUjianPMB#ambilPertemuanList(boolean)}); nilai lain
	 *              (termasuk {@code null}) diperlakukan sebagai {@code false}
	 */
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = jadwalUjianPMB.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
