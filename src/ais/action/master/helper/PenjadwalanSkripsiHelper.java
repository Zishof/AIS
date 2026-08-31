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
import org.zkoss.zul.Label;
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
import ais.database.model.Skripsi;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk mengelola jadwal bimbingan/revisi satu {@link Skripsi} (Skripsi/Tugas
 * Akhir/Thesis) — setiap sesi bimbingan direpresentasikan sebagai baris {@link Pertemuan} yang
 * dapat diedit inline (topik, tanggal, jam mulai/selesai, status/jenis pertemuan). Berbeda dari
 * {@link PenjadwalanUjianPMBHelper}, kelas ini membedakan hak edit: mahasiswa hanya boleh mengubah
 * agenda/jadwal bimbingannya sendiri bila diizinkan oleh konfigurasi
 * {@code FormatNilaiSkripsi#getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan()}
 * ({@link PertemuanRenderer}); bila tidak, baris hanya tampil sebagai label baca-saja.
 *
 * <p>
 * Toolbar jendela menyediakan "Tambah Jadwal Revisi" (baris baru langsung tersimpan, tanggal
 * default digeser tujuh hari dari revisi terakhir), Refresh, serta tombol bersama dari
 * {@link PenjadwalanHelper} untuk ambil-alih jadwal ({@code tampilTombolAmbil}), atur ulang waktu
 * massal ({@code tampilTombolAturUlangWaktu}), unduh data pertemuan ke Excel
 * ({@code tampilTombolDownload}), dan hapus massal ({@code tampilTombolHapus}). {@link #save()}
 * sendiri hanya memicu {@code skripsi.reInitPertemuan(session)} (menyegarkan cache pertemuan) karena
 * setiap perubahan baris sudah langsung disimpan saat diedit ({@link PertemuanRenderer}).
 * </p>
 */
public class PenjadwalanSkripsiHelper {

	private Skripsi skripsi;
	private MyGrid grid;
	private DataLoader dataLoader;

	private Date currDate;

	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private boolean edit = true;

		public PertemuanRenderer() {
			Tbmuser tbmuser = Common.getCurrentUser();
			edit = tbmuser == null || tbmuser.getMahasiswa() == null || skripsi.getFormatNilaiSkripsi() == null ? true
					: skripsi.getFormatNilaiSkripsi().getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan();
		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			arg0.setAttribute("myValue", pertemuan);

			if (!edit) {

				new Label(pertemuan.getTopik()).setParent(arg0);
				new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat4.get().format(pertemuan.getTanggal()))
						.setParent(arg0);
				new Label(pertemuan.getWaktuMulai()).setParent(arg0);
				new Label(pertemuan.getWaktuSelesai()).setParent(arg0);
				new Label(pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama())
						.setParent(arg0);
				new Label().setParent(arg0);
			} else {

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
					waktuMulai.setValue(
							pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanSkripsiHelper.java:124");

				}

				hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
				final Timebox waktuSelesai;
				hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
				waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					waktuSelesai.setValue(
							pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanSkripsiHelper.java:136");

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
						pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? ""
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));
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
												skripsi.belum();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException(
														"menghapus data jadwal skripsi",
														e, new String[] {
																"Pastikan tidak ada data lain (mis. nilai, bimbingan) yang masih berelasi dengan data ini.",
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

	}

	/**
	 * Menyegarkan cache daftar {@link Pertemuan} milik {@link #skripsi} (perubahan per baris sudah
	 * disimpan langsung saat diedit lewat {@link PertemuanRenderer}, sehingga tidak ada penulisan
	 * data massal di sini) lalu memicu {@link #dataLoader} pemanggil.
	 *
	 * @return selalu {@code true}
	 * @throws InterruptedException tidak pernah dilempar dalam praktiknya; dipertahankan pada
	 *                              signature untuk kompatibilitas pemanggil
	 */
	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		Session session = HibernateUtil.currentSession();

		skripsi.reInitPertemuan(session);

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membangun jendela penjadwalan bimbingan/revisi untuk satu {@link Skripsi}: toolbar aksi (lihat
	 * dokumentasi kelas) dan grid baris pertemuan. Tombol Simpan memanggil {@link #save()} dan
	 * menutup jendela bila berhasil.
	 *
	 * @param skripsi    skripsi/tugas akhir yang jadwal bimbingannya akan dikelola
	 * @param dataLoader dipanggil setelah simpan untuk menyegarkan tampilan pemanggil
	 */
	public void display(final Skripsi skripsi, final DataLoader dataLoader) {
		this.skripsi = skripsi;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(skripsi.infoSimple());

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Jadwal Revisi", "/img/new.gif");
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
				pertemuan.setSkripsi(skripsi);
				pertemuan.setTopik("Revisi ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.timeFormat2.get().format(currDate));
				pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(currDate));

				pertemuan.setSkripsi(skripsi);

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
						skripsi.reInitPertemuan(HibernateUtil.currentSession());
						onSearchDefault(new Event("onSearchDefault", arg0.getTarget(), true));
					}
				});
			}
		});

		PenjadwalanHelper.tampilTombolAmbil(toolbar, null, null, null, null, skripsi, null, null, null);
		PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, null, null, null, null, skripsi, null, null, null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, null, null, null, null, skripsi, null, null, null);

		PenjadwalanHelper.tampilTombolHapus(toolbar, null, null, null, null, skripsi, null, null, null,
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
	 * Mengisi ulang grid dengan daftar {@link Pertemuan} milik {@link #skripsi}.
	 *
	 * @param event bila datanya berupa {@link Boolean} {@code true}, memaksa refresh cache daftar
	 *              pertemuan; nilai lain (termasuk {@code null}) diperlakukan sebagai {@code false}
	 */
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = skripsi.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
