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
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TemplateFormatBimbingan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Jendela penjadwalan bimbingan tugas akhir/skripsi: mengelola daftar {@link Pertemuan}
 * (agenda bimbingan) untuk satu {@link MahasiswaRequestTugasAkhir} (permintaan tugas akhir
 * mahasiswa yang sudah disetujui dosen pembimbing). Menampilkan grid agenda bimbingan yang
 * dapat diedit inline (topik, tanggal, jam mulai/selesai, status pertemuan) dengan auto-save
 * per perubahan, serta toolbar untuk menambah jadwal baru (otomatis menggeser 7 hari dari
 * jadwal terakhir), mengambil/menyalin jadwal dari sumber lain, mengatur ulang waktu massal,
 * mengunduh, dan menghapus (lewat method utilitas bersama di {@link PenjadwalanHelper}).
 *
 * <p>
 * Kemampuan edit per baris ({@code bolehUbah}) ditentukan oleh
 * {@link TemplateFormatBimbingan} yang terkait dengan detail kurikulum pertemuan (bila ada):
 * dosen hanya dapat mengubah bila {@code getBolehDiubahOlehDosen()}, mahasiswa hanya bila
 * {@code getBolehDiubahOlehMahasiswa()}; bila tidak ada template terkait atau pengguna bukan
 * dosen/mahasiswa (staf admin), edit selalu diizinkan. Untuk mahasiswa secara umum, ada gerbang
 * tambahan {@code edit} berbasis
 * {@link MahasiswaRequestTugasAkhir#getFormatNilaiProposalSkripsi()}{@code .getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan()}.
 * </p>
 */
public class PenjadwalanTugasAkhirHelper {

	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private MyGrid grid;
	private DataLoader dataLoader;

	private Date currDate;

	/** Merender satu baris agenda bimbingan: read-only (label) bila pengguna tidak berhak mengubah baris ini, atau editable (textbox/datebox/timebox/combobox dengan auto-save) bila berhak — lihat javadoc kelas untuk aturan hak edit. */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private Dosen currentDosen = null;
		private Mahasiswa currentMahasiswa = null;

		private boolean edit = true;

		/** Menentukan dosen/mahasiswa pengguna saat ini dan gerbang izin ubah agenda mahasiswa dari template penilaian terkait. */
		public PertemuanRenderer() {
			Tbmuser tbmuser = Common.getCurrentUser();
			currentDosen = tbmuser == null ? null : tbmuser.ambilDosen();
			currentMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

			edit = tbmuser == null || tbmuser.getMahasiswa() == null
					|| mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? true
							: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
									.getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan();
		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			arg0.setAttribute("myValue", pertemuan);

			TemplateFormatBimbingan templateFormatBimbingan = (TemplateFormatBimbingan) (pertemuan
					.getKurikulumPunyaMatakuliahDetail() == null
							? null
							: HibernateUtil.currentSession().createCriteria(TemplateFormatBimbingan.class)
									.add(Restrictions.eq("id", pertemuan.getKurikulumPunyaMatakuliahDetail()))
									.uniqueResult());
			boolean bolehUbah = templateFormatBimbingan == null || (templateFormatBimbingan != null
					&& ((currentDosen != null && templateFormatBimbingan.getBolehDiubahOlehDosen())
							|| (currentMahasiswa != null && templateFormatBimbingan.getBolehDiubahOlehMahasiswa())
							|| (currentDosen == null && currentMahasiswa == null)));

			if (!bolehUbah || !edit) {
				new Label(pertemuan.getTopik()).setParent(arg0);
				new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat4.get().format(pertemuan.getTanggal()))
						.setParent(arg0);
				Hbox hbox = new Hbox();
				arg0.appendChild(hbox);

				new Label(pertemuan.getWaktuMulai()).setParent(hbox);
				new Label(pertemuan.getWaktuSelesai()).setParent(hbox);
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanTugasAkhirHelper.java:149");

				}

				hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
				final Timebox waktuSelesai;
				hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
				waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					waktuSelesai.setValue(
							pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanTugasAkhirHelper.java:161");

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
												mahasiswaRequestTugasAkhir.belum();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException(
														"menghapus data jadwal tugas akhir",
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

	/** Memicu {@link MahasiswaRequestTugasAkhir#reInitPertemuan} (menyusun ulang urutan/state pertemuan) lalu menyegarkan pemanggil lewat {@link #dataLoader}. Selalu mengembalikan {@code true}. */
	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		Session session = HibernateUtil.currentSession();

		mahasiswaRequestTugasAkhir.reInitPertemuan(session);

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membangun jendela penjadwalan bimbingan untuk {@code mahasiswaRequestTugasAkhir}: toolbar
	 * (Tambah Jadwal Bimbingan, Refresh, tombol-tombol bersama dari {@link PenjadwalanHelper}
	 * untuk ambil/atur-ulang-waktu/unduh/hapus) di atas grid agenda, dan toolbar Batal/Simpan di
	 * bawah. "Tambah Jadwal Bimbingan" membuat {@link Pertemuan} baru dengan tanggal otomatis
	 * 7 hari setelah agenda terakhir yang ditambahkan (atau hari ini bila belum ada), status
	 * default {@link ConstantValues#TATAP_MUKA}, dan langsung disimpan ke database.
	 *
	 * @param mahasiswaRequestTugasAkhir permintaan tugas akhir yang agenda bimbingannya dikelola
	 * @param dataLoader                 callback disegarkan setelah {@link #save()} dipanggil
	 */
	public void display(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final DataLoader dataLoader) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(mahasiswaRequestTugasAkhir.infoSimple());

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Jadwal Bimbingan", "/img/new.gif");
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
				pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
				pertemuan.setTopik("Bimbingan ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.timeFormat2.get().format(currDate));
				pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(currDate));

				// pertemuan.setMulai(currDate);
				// pertemuan.setSelesai(currDate);

				pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);

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
						mahasiswaRequestTugasAkhir.reInitPertemuan(HibernateUtil.currentSession());
						// Nama event WAJIB tidak null: konstruktor org.zkoss.zk.ui.event.Event melempar
						// NullPointerException bila nama null (Event.java:83). onSearchDefault hanya memakai
						// getData() (Boolean) -> pakai nama "onSearch" + data Boolean.TRUE agar perilaku
						// (reload penuh) tetap sama tanpa NPE.
						onSearchDefault(new Event("onSearch", arg0.getTarget(), Boolean.TRUE));
					}
				});
			}
		});

		PenjadwalanHelper.tampilTombolAmbil(toolbar, null, null, null, mahasiswaRequestTugasAkhir, null, null, null,
				null);
		PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, null, null, null, mahasiswaRequestTugasAkhir, null, null,
				null, null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, null, null, null, mahasiswaRequestTugasAkhir, null,
				null, null, null);

		PenjadwalanHelper.tampilTombolHapus(toolbar, null, null, null, mahasiswaRequestTugasAkhir, null, null, null,
				null, new EventListener() {

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

	/** Memuat/menyegarkan grid dengan daftar {@link Pertemuan} milik {@link #mahasiswaRequestTugasAkhir}; {@code event.getData()} bertipe {@link Boolean} (bila ada) diteruskan ke {@code ambilPertemuanList} untuk menentukan mode muat (mis. paksa muat ulang penuh). */
	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = mahasiswaRequestTugasAkhir.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
