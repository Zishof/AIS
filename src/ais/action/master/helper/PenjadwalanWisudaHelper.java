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
import ais.database.model.Wisuda;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer ZK berbentuk window untuk menyusun jadwal rangkaian acara/kegiatan
 * {@link Pertemuan} satu {@link Wisuda} (mis. gladi bersih, upacara, sesi per fakultas), mengikuti
 * pola penjadwalan yang sama dengan {@link PenjadwalanHelper} untuk objek pembelajaran lain.
 *
 * <p>
 * Setiap baris grid adalah satu {@link Pertemuan} yang bisa diedit langsung di tempat (inline
 * editing): topik, tanggal, jam mulai/selesai, dan status pertemuan — setiap perubahan langsung
 * disimpan (auto-save per field lewat event {@code onChange}, bukan menunggu tombol Simpan).
 * Tombol "Tambah Kegiatan" membuat {@link Pertemuan} baru dengan tanggal default 7 hari setelah
 * pertemuan terakhir yang ditambahkan ({@link #currDate}). Tombol download/hapus massal serta
 * "atur ulang waktu" didelegasikan ke helper penjadwalan umum {@link PenjadwalanHelper}. Tombol
 * "Simpan" utama hanya memicu {@link Wisuda#reInitPertemuan(Session)} (menyinkronkan ulang cache
 * pertemuan wisuda) dan memberi tahu {@link DataLoader} pemanggil.
 * </p>
 */
public class PenjadwalanWisudaHelper {

	private Wisuda wisuda;
	private MyGrid grid;
	private DataLoader dataLoader;

	/** Tanggal pertemuan terakhir yang ditambahkan lewat tombol "Tambah Kegiatan"; dipakai untuk menghitung tanggal default pertemuan berikutnya (+7 hari). */
	private Date currDate;

	/**
	 * Perender baris grid untuk satu {@link Pertemuan}: textbox topik, tanggal (readonly, diubah
	 * lewat komponen lain), jam mulai/selesai, combobox {@link StatusPertemuan}, dan tombol hapus
	 * — seluruh field langsung menyimpan perubahannya ke database saat berubah.
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanWisudaHelper.java:107");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			final Timebox waktuSelesai;
			hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuSelesai.setValue(
						pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanWisudaHelper.java:119");

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
											wisuda.belum();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data jadwal wisuda",
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
	 * Menyinkronkan ulang cache pertemuan {@link #wisuda} lewat
	 * {@link Wisuda#reInitPertemuan(Session)} dan memberi tahu {@link #dataLoader} pemanggil agar
	 * memuat ulang tampilannya. Perubahan per baris sendiri sudah disimpan langsung oleh
	 * {@link PertemuanRenderer} saat field diedit.
	 *
	 * @return selalu {@code true}
	 * @throws InterruptedException tidak pernah dilempar secara eksplisit di implementasi ini
	 */
	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		Session session = HibernateUtil.currentSession();

		wisuda.reInitPertemuan(session);

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membangun window penjadwalan rangkaian kegiatan {@code wisuda}: toolbar (tambah kegiatan,
	 * refresh, ambil jadwal dari sumber lain, atur ulang waktu, download, hapus massal — via
	 * {@link PenjadwalanHelper}) dan grid pertemuan yang bisa diedit langsung. Window dipasang
	 * langsung ke root halaman (bukan lewat window pemanggil).
	 *
	 * @param wisuda     kegiatan wisuda yang jadwalnya disusun
	 * @param dataLoader callback muat-ulang data pemanggil, dipicu setelah tombol "Simpan"
	 */
	public void display(final Wisuda wisuda, final DataLoader dataLoader) {
		this.wisuda = wisuda;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(wisuda.infoSimple());

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Kegiatan", "/img/new.gif");
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
				pertemuan.setWisuda(wisuda);
				pertemuan.setTopik("Topik kegiatan ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.timeFormat2.get().format(currDate));
				pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(currDate));

				// pertemuan.setMulai(currDate);
				// pertemuan.setSelesai(currDate);

				pertemuan.setWisuda(wisuda);

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
						wisuda.reInitPertemuan(HibernateUtil.currentSession());
						onSearchDefault(new Event(null, arg0.getTarget(), true));
					}
				});
			}
		});

		// PenjadwalanHelper.tampilTombol(toolbar, null, wisuda, null,
		// null, null);
		PenjadwalanHelper.tampilTombolAmbil(toolbar, null, null, null, null, null, null, wisuda, null);

		PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, null, null, null, null, null, null, null, wisuda,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, null, null, null, null, null, null, null, wisuda);

		PenjadwalanHelper.tampilTombolHapus(toolbar, null, null, null, null, null, null, null, wisuda,
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
	 * Memuat ulang grid dengan daftar {@link Pertemuan} milik {@link #wisuda} lewat
	 * {@link Wisuda#ambilPertemuanList(boolean)}.
	 *
	 * @param event bila data-nya berupa {@link Boolean}, diteruskan sebagai parameter
	 *              {@code ambilPertemuanList} (mis. paksa muat ulang dari database); selain itu
	 *              dianggap {@code false}
	 */
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuans = wisuda.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);

		ListModel strset = new SimpleListModel(pertemuans);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
