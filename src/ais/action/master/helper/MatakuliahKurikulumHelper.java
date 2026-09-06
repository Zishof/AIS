package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.report.format1.akademik.LaporanKurikulum;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Matakuliah;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk grid "Matakuliah pada Kurikulum": menampilkan seluruh
 * {@link KurikulumPunyaMatakuliah} milik satu {@link Kurikulum} pada semester tertentu, termasuk
 * SKS, tahap (bila fitur tahapan kurikulum aktif), status matakuliah, indikator ekstrakurikuler/
 * UTS/UAS, jumlah sub-matakuliah, jumlah jadwal aktif, fakultas/jurusan, jumlah pertemuan default vs
 * realisasi, dan checkbox aktif/nonaktif. Setiap baris dapat dibuka detailnya (rencana pembelajaran
 * per pertemuan lewat {@link MatakuliahKurikulumDetailHelper}), diedit (dialog ubah semester/jumlah
 * pertemuan/flag inti-institusional-tugas), atau dihapus (diblokir bila konfigurasi
 * {@code kurikulum_yang_sudah_dijadwal_tidak_bisa_dihapus} aktif dan sudah ada jadwal
 * {@link Perkuliahan}).
 *
 * <p>
 * Menyediakan tombol "Generate Rencana Pembelajaran" ({@link #tampilTombolBuatKurikulumPunyaMatakuliahDetail})
 * yang membuat baris {@link KurikulumPunyaMatakuliahDetail} untuk setiap pertemuan (1..jumlah
 * pertemuan default), otomatis menandai pertemuan tengah sebagai UTS dan pertemuan terakhir sebagai
 * UAS bila diminta, serta opsi menghapus lebih dulu detail pertemuan lama. Bila fitur integrasi Neo
 * Feeder aktif ({@code aktifkan_terhubung_langsung_ke_feeder}), setiap baris juga menampilkan
 * indikator validitas data Feeder dan tombol kirim data ke Feeder (berjalan asinkron di thread
 * terpisah, dengan log galat ditampilkan lewat dialog dan opsi unduh file teks).
 * </p>
 *
 * <p>
 * Hak edit/hapus/tambah ditentukan sekali di konstruktor lewat
 * {@link ais.common.CommonPrivilages#checkPrevilages}, mengikuti privilese pengguna yang sedang
 * login.
 * </p>
 */
public class MatakuliahKurikulumHelper implements DataLoader {

	/** Grid daftar {@link KurikulumPunyaMatakuliah}; dibuat di dalam timer {@link #display} dan diisi ulang oleh {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Kurikulum yang matakuliahnya ditampilkan; menjadi bagian kunci penyaring pada {@link #loadData(Object)} bersama {@link #semester} dan {@link #indukMatakuliah}. */
	private Kurikulum kurikulum;
	/** Semester kurikulum yang ditampilkan; ikut menjadi penyaring {@link #loadData(Object)} dan judul laporan "Cetak Kurikulum". */
	private Integer semester;
	/**
	 * Hasil pencarian terakhir yang menjadi model grid. Daftar ini juga menjadi cakupan kerja tombol
	 * "Generate Rencana Pembelajaran" dan "Hapus Semua" — keduanya beroperasi pada isi daftar saat itu,
	 * yaitu hasil penyaringan kurikulum/semester/induk-matakuliah (dan checkbox "Hanya tampil yang
	 * aktif"), bukan pada seluruh isi kurikulum.
	 */
	private List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs;

	/** Hak tambah pengguna ({@link CommonPrivilages#CREATE}), dibaca sekali di konstruktor; mengatur tampil-tidaknya tombol "Ambil Matakuliah" dan "Generate Rencana Pembelajaran". */
	private Boolean add = false;
	/** Hak hapus pengguna ({@link CommonPrivilages#DELETE}), dibaca sekali di konstruktor; mengatur tampil-tidaknya tombol hapus per baris dan tombol "Hapus Semua". */
	private Boolean delete = false;
	/**
	 * Hak ubah pengguna ({@link CommonPrivilages#UPDATE}), dibaca sekali di konstruktor. Dipakai untuk
	 * menonaktifkan checkbox "Aktif" dan menyembunyikan tombol ubah per baris. Perlu diperhatikan
	 * bahwa combobox "Tahap" pada baris grid tidak ikut memeriksa field ini.
	 */
	private Boolean edit = false;
	/** Induk matakuliah yang sub-matakuliahnya ditampilkan; bila {@code null}, yang ditampilkan adalah matakuliah level teratas ({@code indukMatakuliah is null}). */
	private KurikulumPunyaMatakuliah indukMatakuliah;
	/** Paket perkuliahan terkait; bila tidak {@code null}, tombol "Manajemen Paket Perkuliahan Mahasiswa" dimunculkan untuk membuka {@link DetailPaketPerkuliahanHelper}. */
	private PaketPerkuliahan paketPerkuliahan;
	/** Pengguna yang sedang login, dibaca sekali di konstruktor; dipakai renderer baris untuk menonaktifkan checkbox "Aktif" bagi mahasiswa/dosen dan menampilkan blok Feeder. */
	private Tbmuser tbmuser = null;
	/**
	 * Checkbox penyaring "Hanya tampil yang aktif" (default tercentang). Selama bernilai {@code null}
	 * — yaitu pada pemuatan pertama sebelum toolbar terbangun — {@link #loadData(Object)} berlaku
	 * seolah-olah tercentang, sehingga hanya baris aktif atau yang flag aktifnya {@code null} yang
	 * ditampilkan.
	 */
	private MyCheckboxConfig hanyaTampilYangAktif;

	/** Menentukan hak tambah/ubah/hapus dari privilese pengguna saat ini dan menyimpan pengguna aktif untuk pengecekan peran (mahasiswa/dosen) di seluruh method instance. */
	public MatakuliahKurikulumHelper() {
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		tbmuser = Common.getCurrentUser();
	}

	/** Row renderer grid matakuliah kurikulum: nama/SKS/tahap/status/indikator UTS-UAS-ekstrakurikuler, jumlah sub-matakuliah, jumlah jadwal aktif, fakultas/jurusan, jumlah pertemuan default/realisasi, checkbox aktif, serta tombol Feeder/edit/hapus per baris. */
	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		/** Salinan pengguna yang sedang login milik renderer; dipakai untuk menentukan apakah combobox "Tahap" ditampilkan sebagai kontrol yang dapat diubah atau sekadar label. */
		private Tbmuser user;

		/** Membaca pengguna yang sedang login sekali saat renderer dibuat, bukan per baris. */
		public DetailMatakuliahRenderer() {
			user = Common.getCurrentUser();
		}

		/**
		 * Merender satu baris {@link KurikulumPunyaMatakuliah}. Baris diakhiri lebih awal tanpa satu
		 * pun kolom bila {@code kurikulumPunyaMatakuliah.getMatakuliah()} bernilai {@code null},
		 * sehingga baris yatim tampil kosong alih-alih memicu galat.
		 *
		 * <p>
		 * Kolom yang dihasilkan berurutan: {@link MyDetail} pembuka rencana pembelajaran per pertemuan
		 * ({@link MatakuliahKurikulumDetailHelper}, dimuat baru saat detail dibuka), kode matakuliah
		 * beserta tautan revisi {@link RevisiHelper} dan — bila integrasi Neo Feeder aktif — indikator
		 * validitas Feeder serta tombol "Krm ke feeder", nama, SKS, tahap, status, indikator
		 * ekstrakurikuler/UTS/UAS, jumlah sub-matakuliah, jumlah jadwal {@link Perkuliahan} aktif,
		 * fakultas, jurusan, jumlah pertemuan default, jumlah {@link KurikulumPunyaMatakuliahDetail}
		 * yang sudah ada, checkbox "Aktif", dan kolom aksi ubah/hapus.
		 * </p>
		 *
		 * <p>
		 * Dua kolom terakhir diisi tertunda lewat {@link Common#createDefaultTimer}: jumlah rencana
		 * pembelajaran dan jumlah jadwal aktif dihitung dengan {@code rowCount} per baris (satu
		 * pasang query untuk setiap baris yang dirender), dan hasil hitung jadwal itu pula yang
		 * menonaktifkan tombol hapus ketika konfigurasi
		 * {@code kurikulum_yang_sudah_dijadwal_tidak_bisa_dihapus} aktif.
		 * </p>
		 *
		 * <p>
		 * Combobox "Tahap" — yang menyimpan perubahannya seketika lewat {@link Common#refreshUpdate} —
		 * hanya ditampilkan ketika {@code ConstantValues.aktifkanTahapanKurikulum} menyala dan
		 * pengguna bukan dosen maupun mahasiswa; berbeda dengan checkbox "Aktif", kontrol ini tidak
		 * memeriksa hak ubah {@link #edit}. Tombol ubah dan hapus disusun ke dalam kolom aksi
		 * ringkas oleh {@code UIHelper.buatBarisAksi} dan hanya dikendalikan lewat visibilitas
		 * ({@link #edit}/{@link #delete}); listener masing-masing tombol tidak memeriksa ulang hak
		 * tersebut.
		 * </p>
		 *
		 * @param row  baris grid ZK yang sedang diisi
		 * @param data {@link KurikulumPunyaMatakuliah} yang dirender
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			// final Matakuliah matakuliah = (Matakuliah) data;
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) data;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MatakuliahKurikulumDetailHelper matakuliahKurikulumDetailHelper = new MatakuliahKurikulumDetailHelper();
						matakuliahKurikulumDetailHelper.display(kurikulumPunyaMatakuliah, null, detail);
					}
				}
			});

			Session session = HibernateUtil.currentSession();
//			session.refresh(kurikulumPunyaMatakuliah);

			Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
			if (matakuliah == null) {
				return;
			}
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			RevisiHelper.createNewRevisi(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah,
					matakuliah.getKode() + "  ( " + matakuliah.getId() + " )").setParent(vbox);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (kurikulumPunyaMatakuliah.getFeeder() != null
						&& !kurikulumPunyaMatakuliah.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox);
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											String[] kon = EksporFromFeederAction.koneksi();
											final String ip = kon[0];
											final String port = kon[1];
											final String username = kon[2];
											final String password = kon[3];
											final String url = kon[4];

											if (!EksporFromFeederAction.exists(url)) {

												MyMessageboxConfig.show(
														ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											final List<String> errorLog = new ArrayList<String>();

											final Label myLabelProsesDetail = Common
													.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(err, "Error Terjadi",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");

																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
															}

															loadData(null);
														}
													});

											new Thread(new Runnable() {

												@Override
												public void run() {
													try {
														FeederConnector feederConnector = new FeederConnector(ip,
																Integer.parseInt(port), null);

														String token = feederConnector.getToken(username, password);
														System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

														if (token == null || token.trim().isEmpty()
																|| token.trim().toLowerCase().startsWith("error")) {
															myLabelProsesDetail
																	.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														myLabelProsesDetail.setValue("Mengirim data " + kurikulum);

														feederImporter.kurikulumPunyaMatakuliah(
																kurikulumPunyaMatakuliah, errorLog);

													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MatakuliahKurikulumHelper.java:244");
													}

													myLabelProsesDetail.setValue("");
												}
											}).start();

										}

									}
								});

					}
				});

			}

			new Label(matakuliah.getNama()).setParent(row);
			new Label(matakuliah.getSks() + "").setParent(row);

			if (ConstantValues.aktifkanTahapanKurikulum && edit && user.getSiswa() == null
					&& user.getDosen() == null && user.getMahasiswa() == null) {

				if (ConstantValues.jumlahTahapan.isEmpty()) {
					ConstantValues.initJumlahTahapan();
				}

				int jumlahTahapan = ConstantValues.getJumlahTahapan(
						kurikulumPunyaMatakuliah.getKurikulum().getProgram() == null ? "Reguler"
								: kurikulumPunyaMatakuliah.getKurikulum().getProgram().getNama(),
						kurikulumPunyaMatakuliah.getKurikulum().getJurusan());

				final Combobox tahap = new Combobox();
				for (int i = 1; i <= (jumlahTahapan * 5); i++) {
					MyComboitemConfig comboitem = new MyComboitemConfig("Tahap " + i);
					comboitem.setValue(i);
					tahap.appendChild(comboitem);
				}
				MyComboitemConfig comboitem = new MyComboitemConfig("Tanpa tahap");
				comboitem.setValue(null);
				tahap.appendChild(comboitem);

				if (kurikulumPunyaMatakuliah.getTahap() == null) {
					tahap.setSelectedItem(comboitem);
				} else {
					Common.selectComboItem(tahap, kurikulumPunyaMatakuliah.getTahap());
				}
				tahap.setReadonly(true);
				tahap.setParent(row);
				tahap.setWidth("90%");
				tahap.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!edit || user.getSiswa() != null || user.getDosen() != null
								|| user.getMahasiswa() != null) {
							return;
						}
						kurikulumPunyaMatakuliah.setTahap((Integer) (tahap.getSelectedItem() == null ? null
								: tahap.getSelectedItem().getValue()));
						Common.refreshUpdate(kurikulumPunyaMatakuliah);
					}
				});
			} else {
				new Label(kurikulumPunyaMatakuliah.getTahap() == null ? ""
						: kurikulumPunyaMatakuliah.getTahap().toString()).setParent(row);
			}

			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(row);

			new Label((matakuliah.getExtraKulikuler() == null ? "" : matakuliah.getExtraKulikuler() ? "Ya" : "Tidak")
					+ "/" + (matakuliah.getTerdapatUts() ? "Ya" : "Tidak") + "/"
					+ (matakuliah.getTerdapatUas() ? "Ya" : "Tidak")).setParent(row);

			int subMk = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
					.add(Restrictions.eq("indukMatakuliah", kurikulumPunyaMatakuliah))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			new Label(Common.numberFormat.get().format(subMk)).setParent(row);

			final Label jmlJadwal = new Label("..");
			jmlJadwal.setParent(row);

			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getFakultas() == null ? ""
					: matakuliah.getJurusan().getFakultas().getNama()).setParent(row);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(row);

			new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()))
					.setParent(row);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(
					!edit || tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null);
			checkbox.setChecked(kurikulumPunyaMatakuliah.getAktif());
			checkbox.setParent(row);
			row.setValign("top");
			row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit || tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
						return;
					}
					kurikulumPunyaMatakuliah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kurikulumPunyaMatakuliah);
				}
			});

			MyToolbarbuttonConfig editButton = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			editButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final MyWindow addWindow = new MyWindow("Ubah Matakuliah", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					addWindow.setHeight("95%");
					addWindow.setWidth("90%");
					Common.clear(addWindow);
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
					final AmbilDataMatakuliahBanbox matakuliah;
					row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
					matakuliah.setAttribute("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah());
					matakuliah.setValue(kurikulumPunyaMatakuliah.getMatakuliah().getNama());
					matakuliah.setWidth("90%");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
					final Intbox semester;
					row.appendChild(semester = new Intbox(kurikulumPunyaMatakuliah.getSemester()));

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pertemuan Default"));
					final Intbox jumlahPertemuanPerkuliahanDefault;
					row.appendChild(jumlahPertemuanPerkuliahanDefault = new Intbox(
							kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()));

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));
					final MyCheckboxConfig inti = new MyCheckboxConfig(
							"Inti menurut rujukan peer group / SK Mendiknas 045/2002 (ps. 3 ayat 2e)");
					row.appendChild(inti);
					inti.setChecked(kurikulumPunyaMatakuliah.getInti());

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));
					final MyCheckboxConfig institusional = new MyCheckboxConfig("Institusional");
					row.appendChild(institusional);
					institusional.setChecked(kurikulumPunyaMatakuliah.getInstitusional());

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));
					final MyCheckboxConfig terdapatTugas = new MyCheckboxConfig(
							"Bobot Tugas, mata kuliah yang dalam penentuan nilai akhirnya memberikan bobot pada tugas-tugas (praktikum/praktek, PR atau makalah) >= 20%.");
					row.appendChild(terdapatTugas);
					terdapatTugas.setChecked(kurikulumPunyaMatakuliah.getTerdapatTugas());

					East east = new East();
					east.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(east, true);
					east.setWidth("80%");

					MatakuliahKurikulumHelper matakuliahKurikulumHelper = new MatakuliahKurikulumHelper();
					matakuliahKurikulumHelper.display(kurikulum, paketPerkuliahan, east,
							MatakuliahKurikulumHelper.this.semester, kurikulumPunyaMatakuliah);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							addWindow.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							kurikulumPunyaMatakuliah.setSemester(semester.getValue());
							kurikulumPunyaMatakuliah.setMatakuliah((Matakuliah) matakuliah.getAttribute("matakuliah"));
							kurikulumPunyaMatakuliah
									.setJumlahPertemuanPerkuliahanDefault(jumlahPertemuanPerkuliahanDefault.getValue());
							kurikulumPunyaMatakuliah.setInti(inti.isChecked());
							kurikulumPunyaMatakuliah.setInstitusional(institusional.isChecked());
							kurikulumPunyaMatakuliah.setTerdapatTugas(terdapatTugas.isChecked());
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							addWindow.detach();

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
					});
					save.setParent(toolbar);
					borderlayout.setParent(addWindow);
					addWindow.onModal();

				}
			});
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

			final Label jumlah = new Label();
			row.appendChild(jumlah);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					Integer count = ((Number) session.createCriteria(KurikulumPunyaMatakuliahDetail.class)
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					jumlah.setValue(Common.numberFormat.get().format(count));

					count = ((Number) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					jmlJadwal.setValue(Common.numberFormat.get().format(count));

					if (Common.bolehKonfigurasi("kurikulum_yang_sudah_dijadwal_tidak_bisa_dihapus")) {
						deleteButton.setDisabled(count > 0);
					}
				}
			});

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			deleteButton.setVisible(delete);
			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(kurikulumPunyaMatakuliah);

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
			editButton.setVisible(edit);
			editButton.setTooltiptext("Ubah Data");
			aksiButtons.add(editButton);
			aksiButtons.add(deleteButton);
			// Susun semua tombol: max 3 per baris, rata tengah
			Vbox aksiBox = ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
			aksiBox.setVisible(
					Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);

		}

	}

	/**
	 * Menambahkan tombol "Generate Rencana Pembelajaran" ke {@code toolbar} (tampil hanya untuk
	 * pengguna dengan hak tambah dan bukan mahasiswa/siswa/dosen). Saat diklik, membuka dialog opsi
	 * (tandai pertemuan tengah sebagai UTS, pertemuan akhir sebagai UAS, hapus detail pertemuan lama)
	 * lalu membuat baris {@link KurikulumPunyaMatakuliahDetail} untuk setiap
	 * {@link KurikulumPunyaMatakuliah} yang sedang ditampilkan sejumlah
	 * {@code jumlahPertemuanPerkuliahanDefault}-nya masing-masing.
	 *
	 * <p>
	 * Penandaan UTS memakai pembagian bilangan bulat {@code jumlahPertemuanPerkuliahanDefault / 2},
	 * sehingga untuk jumlah pertemuan ganjil UTS jatuh pada pertemuan sebelum titik tengah, dan untuk
	 * jumlah pertemuan bernilai 1 tidak ada pertemuan yang ditandai UTS sama sekali. Pembuatan baris
	 * bersifat menambah-bila-belum-ada: baris dengan {@code nomorUrut} yang sudah ada dibiarkan apa
	 * adanya, sehingga menjalankan ulang tombol ini tidak menimpa rencana pembelajaran yang sudah
	 * diisi kecuali opsi "Hapus pertamuan yang sebelumnya sudah ada" dicentang.
	 * </p>
	 *
	 * <p>
	 * Opsi hapus tersebut dijalankan sebagai {@code delete} SQL langsung terhadap tabel
	 * {@code kurikulum_punya_matakuliah_detail} (id disisipkan dari nilai {@code Long} entity),
	 * bukan lewat sesi Hibernate; akibatnya penghapusan itu tidak melewati cascade, interceptor, dan
	 * pencatatan revisi Envers, serta tidak menyegarkan cache tingkat pertama sesi yang sedang
	 * berjalan.
	 * </p>
	 *
	 * <p>
	 * Cakupan pembuatan mengikuti {@link #kurikulumPunyaMatakuliahs} — yakni hasil penyaringan grid
	 * saat itu (kurikulum, semester, induk matakuliah, dan checkbox "Hanya tampil yang aktif") —
	 * bukan seluruh matakuliah kurikulum.
	 * </p>
	 *
	 * @param toolbar        toolbar tempat tombol ditambahkan
	 * @param eventListener  callback yang dijalankan setelah proses generate selesai (biasanya memuat
	 *                       ulang grid pemanggil)
	 */
	public void tampilTombolBuatKurikulumPunyaMatakuliahDetail(Toolbar toolbar, final EventListener eventListener) {
		Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Generate Rencana Pembelajaran", "/img/new.gif");
		button.setVisible(add && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Window window = new Window();
				window.setHeight("95%");
				window.setWidth("90%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UTS"));
				final MyCheckboxConfig uts;
				row.appendChild(uts = new MyCheckboxConfig("Di pertengahan pertamuan merupakan jadwal UTS"));
				uts.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UAS"));
				final MyCheckboxConfig uas;
				row.appendChild(uas = new MyCheckboxConfig("Di akhir pertamuan merupakan jadwal UAS"));
				uas.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hapus pertamuan"));
				final MyCheckboxConfig hapus;
				row.appendChild(hapus = new MyCheckboxConfig("Hapus pertamuan yang sebelumnya sudah ada"));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Session session = HibernateUtil.currentSession();

						for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

							if (hapus.isChecked()) {
								session.createSQLQuery(
										"delete from kurikulum_punya_matakuliah_detail where kurikulum_punya_matakuliah="
												+ kurikulumPunyaMatakuliah.getId())
										.executeUpdate();
							}

							for (int i = 1; i <= kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault(); i++) {
								KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) session
										.createCriteria(KurikulumPunyaMatakuliahDetail.class)
										.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
										.add(Restrictions.eq("nomorUrut", i)).setMaxResults(1).uniqueResult();
								if (kurikulumPunyaMatakuliahDetail == null) {
									kurikulumPunyaMatakuliahDetail = new KurikulumPunyaMatakuliahDetail();
									kurikulumPunyaMatakuliahDetail.setNomorUrut(i);
									kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.TATAP_MUKA);

									if (uas.isChecked()) {
										if (i == kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()) {
											kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.UAS);
											kurikulumPunyaMatakuliahDetail.setTopik("Pertemuan ke " + i + " : UAS");
											kurikulumPunyaMatakuliahDetail
													.setMetodePembelajaran("Mengerjakan soal UAS");
										}
									}

									if (uts.isChecked()) {
										if (i == (kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()
												/ 2)) {
											kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.UTS);
											kurikulumPunyaMatakuliahDetail.setTopik("Pertemuan ke " + i + " : UTS");
											kurikulumPunyaMatakuliahDetail
													.setMetodePembelajaran("Mengerjakan soal UTS");
										}
									}

									kurikulumPunyaMatakuliahDetail
											.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									Common.refreshSaveOrUpdate(session, kurikulumPunyaMatakuliahDetail);
								}

							}
						}
						window.detach();

						Common.createDefaultTimer(eventListener);
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);
	}

	/** Memuat ulang daftar {@link KurikulumPunyaMatakuliah} kurikulum/semester/induk-matakuliah saat ini (memfilter status aktif bila checkbox "Hanya tampil yang aktif" dicentang) dan me-render ulang grid. Parameter {@code value} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		kurikulumPunyaMatakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)
				.createAlias("matakuliah", "matakuliah")
				.add(hanyaTampilYangAktif == null || hanyaTampilYangAktif.isChecked()
						? Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))
						: Restrictions.sqlRestriction("true"))

				.add(indukMatakuliah == null ? Restrictions.isNull("indukMatakuliah")
						: Restrictions.eq("indukMatakuliah", indukMatakuliah))

				.addOrder(Order.asc("matakuliah.nama")).add(Restrictions.eq("kurikulum", kurikulum))
				.add(Restrictions.eq("semester", this.semester)).list();
		ListModel strset = new SimpleListModel(kurikulumPunyaMatakuliahs);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * @return helper ini sendiri sebagai {@link DataLoader}, diteruskan ke
	 *         {@link AmbilDataMatakuliahKurikulumHelper} agar grid ini dimuat ulang setelah
	 *         matakuliah baru ditambahkan ke kurikulum
	 */
	private DataLoader getDataloader() {
		return this;
	}

	/** Seperti {@link #display(Kurikulum, PaketPerkuliahan, Component, Integer, KurikulumPunyaMatakuliah)} tanpa keterkaitan {@link PaketPerkuliahan} (tombol "Manajemen Paket Perkuliahan Mahasiswa" disembunyikan). */
	public void display(final Kurikulum kurikulum, final Component component, final Integer semester,
			final KurikulumPunyaMatakuliah indukMatakuliah) {
		display(kurikulum, null, component, semester, indukMatakuliah);
	}

	/**
	 * Membangun seluruh UI grid matakuliah kurikulum (toolbar aksi, kolom grid, filter aktif) di
	 * dalam {@code component} untuk kombinasi kurikulum/semester/induk-matakuliah yang diberikan, lalu
	 * memuat data awal secara asinkron lewat {@link Common#createDefaultTimer}.
	 *
	 * <p>
	 * Seluruh pembangunan toolbar, kolom, dan pemuatan data pertama berlangsung di dalam
	 * {@link Common#createDefaultTimer}, sehingga {@link #grid} baru tersedia setelah timer berjalan;
	 * pemanggilan {@link #loadData(Object)} sebelum saat itu tidak akan menemukan grid.
	 * </p>
	 *
	 * <p>
	 * Toolbar beserta tombol-tombolnya hanya dikendalikan lewat visibilitas: {@code toolbar} sendiri
	 * disembunyikan dari mahasiswa dan siswa, sedangkan "Ambil Matakuliah", "Generate Rencana
	 * Pembelajaran", dan "Hapus Semua" tambahan disyaratkan hak {@link #add}/{@link #delete} dan
	 * bukan dosen. Listener masing-masing tombol tidak memeriksa ulang hak tersebut. Tombol "Hapus
	 * Semua" sendiri hanya menghapus {@link KurikulumPunyaMatakuliah} yang belum memiliki
	 * {@link Perkuliahan} aktif — baris yang seluruh jadwalnya sudah dinonaktifkan tetap ikut
	 * terhapus.
	 * </p>
	 *
	 * @param kurikulum         kurikulum yang matakuliahnya ditampilkan
	 * @param paketPerkuliahan  paket perkuliahan terkait (mengaktifkan tombol manajemen paket), boleh
	 *                          {@code null}
	 * @param component         container ZK yang akan diisi
	 * @param semester          semester kurikulum yang ditampilkan
	 * @param indukMatakuliah   induk matakuliah untuk menampilkan sub-matakuliah saja, atau
	 *                          {@code null} untuk matakuliah level teratas
	 */
	public void display(final Kurikulum kurikulum, final PaketPerkuliahan paketPerkuliahan, final Component component,
			final Integer semester, final KurikulumPunyaMatakuliah indukMatakuliah) {
		this.kurikulum = kurikulum;
		this.semester = semester;
		this.indukMatakuliah = indukMatakuliah;
		this.paketPerkuliahan = paketPerkuliahan;
		Common.clear(component);

		final ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 2000px;");
		groupbox.setParent(component);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();

				Toolbar toolbar = new Toolbar();
				toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
				// toolbar.setHeight("25px");
				toolbar.setParent(groupbox);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Matakuliah", "/img/new.gif");
				button.setVisible(add && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.ambilDosen() == null);
				button.addEventListener("onClick", new EventListener() {

					private AmbilDataMatakuliahKurikulumHelper ambilDataMatakuliahKurikulumHelper = new AmbilDataMatakuliahKurikulumHelper();

					@Override
					public void onEvent(Event event) throws Exception {

						ambilDataMatakuliahKurikulumHelper.display(kurikulum, getDataloader(), semester,
								indukMatakuliah);
					}

				});
				button.setParent(toolbar);

				tampilTombolBuatKurikulumPunyaMatakuliahDetail(toolbar, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});

				String[] contents = new String[] { "id", "kurikulumPunyaMatakuliah", "nomorUrut", "indikator", "topik",
						"metodePembelajaran", "pengalamanBelajar", "waktupembelajaran", "tugasDanPenilaian",
						"bukuRujukan1", "statusPertemuan" };
				MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();

						return session.createCriteria(KurikulumPunyaMatakuliahDetail.class)
								.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah")
								.createAlias("kurikulumPunyaMatakuliah.matakuliah", "matakuliah")
								.addOrder(Order.asc("matakuliah.kode")).addOrder(Order.asc("nomorUrut"))
								.add(Restrictions.eq("kurikulumPunyaMatakuliah.kurikulum", kurikulum))
								.add(Restrictions.eq("kurikulumPunyaMatakuliah.semester", semester));
					}
				}, contents);
				toolbar.appendChild(cetakToolbarbutton);

				MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

					@Override
					public void onSearchDefault(Event event) {
						loadData(null);
					}
				}, KurikulumPunyaMatakuliahDetail.class, contents);
				toolbar.appendChild(upload);

				button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
				button.setVisible(delete && tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
													int count = ((Number) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("kurikulumPunyaMatakuliah",
																	kurikulumPunyaMatakuliah))
															.setProjection(Projections.rowCount()).uniqueResult())
															.intValue();
													if (count == 0) {
														Common.refreshDelete(session, kurikulumPunyaMatakuliah);
													}
												}

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

				button = new MyToolbarbuttonConfig("Manajemen Paket Perkuliahan Mahasiswa", "/img/new.gif");
				button.setVisible(paketPerkuliahan != null);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						MyWindow window = new MyWindow("Manajemen Paket Perkuliahan Mahasiswa", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
						window.setHeight("97%");
						window.setWidth("90%");

						new DetailPaketPerkuliahanHelper().display(paketPerkuliahan, semester, window);

						window.onModal();

					}

				});
				button.setParent(toolbar);

				hanyaTampilYangAktif = new MyCheckboxConfig("Hanya tampil yang aktif");
				hanyaTampilYangAktif.setChecked(true);
				hanyaTampilYangAktif.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						loadData(null);

					}

				});
				hanyaTampilYangAktif.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Cetak Kurikulum", "/img/print.png");

				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						LaporanKurikulum laporanKurikulum = new LaporanKurikulum(kurikulum, semester);
						laporanKurikulum.setHeight("95%");
						laporanKurikulum.setWidth("90%");
						laporanKurikulum.setClosable(true);
						laporanKurikulum.setBorder("none");
						laporanKurikulum.setTitle("Kurikulum Semester " + semester);
						laporanKurikulum.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						laporanKurikulum.onModal();

					}

				});
				button.setParent(toolbar);

				grid = new MyGrid();// grid.setOddRowSclass("non-odd");
				grid.setWidth("100%");
				grid.setMold("paging");
				grid.setPageSize(10);
				grid.getPagingChild().setMold("os");
				grid.setParent(groupbox);
				grid.setStyle("min-height: 2000px;");

				Columns columns = new Columns();

				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("");
				column.setWidth("40px");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Kode");
				column.setWidth("12%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Nama");
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("SKS");
				column.setWidth("5%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Tahap");
				column.setWidth(ConstantValues.aktifkanTahapanKurikulum ? "8%" : "0%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Status");
				column.setWidth("8%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Ekstra/Uts/Uas");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Sub Mk");
				column.setWidth("5%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jml. Jadwal");
				column.setWidth("8%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("0%");
				column.setLabel("Fakultas");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jurusan");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jml Def. Pert.");
				column.setWidth("5%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jml Pert.");
				column.setWidth("5%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Aktif");
				column.setWidth("5%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Aksi");
				column.setWidth("6%");

				loadData(null);
			}
		});

	}

}
