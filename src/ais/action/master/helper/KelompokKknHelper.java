package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SertifikatAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper tampilan & pengelolaan anggota satu {@link KelompokKkn} (kelompok Kuliah Kerja Nyata):
 * daftar mahasiswa peserta ({@link MahasiswaDapatKelompokKkn}) dengan status diterima/belum,
 * nilai hasil, keterangan, dan pencetakan sertifikat kelulusan KKN. Menyediakan pencarian
 * (NIM/nama, filter diterima/belum diterima), penambahan anggota lewat pencarian mahasiswa,
 * cetak laporan PDF, dan impor massal data anggota dari berkas Excel (.xlsx).
 *
 * <p>
 * Hak edit inline pada grid ({@code boleh}) hanya diberikan kepada staf administratif (bukan
 * mahasiswa/siswa/calon mahasiswa/calon siswa yang login); mahasiswa yang login hanya dapat
 * melihat data miliknya sendiri (read-only) dan mencetak sertifikat bila sudah diterima dan
 * kelompoknya punya template sertifikat.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} (callback penyegaran) dan {@link DataCriteria}
 * (kriteria pencarian dipakai bersama oleh paging server-side dan cetak laporan
 * {@link Common#cetakData}).
 * </p>
 */
public class KelompokKknHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private KelompokKkn kelompokKkn;
	private Tbmuser tbmuser;

	private Paging paging;
	private MyCheckboxConfig diterima;
	private MyCheckboxConfig belumDiterima;
	private Textbox nim;

	/** Merender satu baris grid anggota KKN: foto+NIM, riwayat revisi (nama), jurusan/fakultas, hasil/keterangan (editable untuk staf, read-only untuk mahasiswa lain), checkbox diterima (auto-save), tombol cetak sertifikat, dan tombol hapus (hanya staf, hanya bila belum diterima). */
	class DetailKelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) data;

			AuditListener.prosesUntukElearning(mahasiswaDapatKelompokKkn, "", mahasiswaDapatKelompokKkn.getId());

			Mahasiswa mahasiswa = mahasiswaDapatKelompokKkn.getMahasiswa();

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
			new Label(mahasiswa.getNim()).setParent(vbox);

			RevisiHelper
					.createNewRevisi(MahasiswaDapatKelompokKkn.class, mahasiswaDapatKelompokKkn, mahasiswa.getNama())
					.setParent(row);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? ""
					: mahasiswa.getJurusan().getFakultas() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getNama())
					.setParent(row);

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			cetakToolbarbuttonSertifikat.setVisible(mahasiswaDapatKelompokKkn.getDiterima()
					&& mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat() != null);
			final MyToolbarbuttonConfig hapusButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			hapusButton.setOrient("vertical");

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Diterima");

			final boolean sama = tbmuser != null && tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(mahasiswa.getId());
			final boolean boleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null;
			if (boleh) {

				final MyTextbox hasil = new MyTextbox(mahasiswaDapatKelompokKkn.getHasil());
				hasil.setWidth("90%");
				hasil.setRows(2);

				final MyTextbox keterangan = new MyTextbox(mahasiswaDapatKelompokKkn.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaDapatKelompokKkn.setHasil(hasil.getValue());
						mahasiswaDapatKelompokKkn.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(mahasiswaDapatKelompokKkn);

					}
				};

				keterangan.addEventListener("onChange", eventListener);
				hasil.addEventListener("onChange", eventListener);
				hasil.setParent(row);
				keterangan.setParent(row);
				checkbox.setParent(row);
			} else {
				if (sama) {
					new Label(mahasiswaDapatKelompokKkn.getHasil()).setParent(row);
					new Label(mahasiswaDapatKelompokKkn.getKeterangan()).setParent(row);
				} else {
					cetakToolbarbuttonSertifikat.setVisible(false);
					new Label("-").setParent(row);
					new Label("-").setParent(row);
				}
				new Label(mahasiswaDapatKelompokKkn.getDiterima() ? "Ya" : "Belum").setParent(row);
			}

			checkbox.setChecked(mahasiswaDapatKelompokKkn.getDiterima());
			row.setValign("top");
			row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswaDapatKelompokKkn.setDiterima(checkbox.isChecked());
					Common.refreshSaveOrUpdate(mahasiswaDapatKelompokKkn);
					cetakToolbarbuttonSertifikat.setVisible(mahasiswaDapatKelompokKkn.getDiterima()
							&& mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat() != null);
					hapusButton.setVisible(boleh && !mahasiswaDapatKelompokKkn.getDiterima());
				}
			});

			Hbox toolbar = new Hbox();

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(mahasiswaDapatKelompokKkn);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);

			hapusButton.setVisible(boleh && !mahasiswaDapatKelompokKkn.getDiterima());
			hapusButton.setTooltiptext("Hapus Data");
			hapusButton.addEventListener("onClick", new EventListener() {
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
											Common.refreshDelete(mahasiswaDapatKelompokKkn);

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
			hapusButton.setParent(toolbar);
			if (tbmuser != null && tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(mahasiswa.getId())) {
				cetakToolbarbuttonSertifikat.setParent(row);
			} else {
				ais.ui.util.MenuAksiBaris.pasang(toolbar);
				toolbar.setParent(row);
			}
		}

	}

	/**
	 * Membangun {@link Criteria} pencarian {@link MahasiswaDapatKelompokKkn} untuk
	 * {@link #kelompokKkn}, disaring berdasarkan status diterima/belum diterima (checkbox
	 * toolbar) dan NIM/nama (contains, cocok pada salah satu).
	 *
	 * @param order bila {@code true}, tambahkan pengurutan berdasarkan id
	 * @return criteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(MahasiswaDapatKelompokKkn.class)
				.add(diterima.isChecked() ? Restrictions.eq("diterima", true) : Restrictions.sqlRestriction("true"))
				.add(belumDiterima.isChecked() ? Restrictions.eq("diterima", false)
						: Restrictions.sqlRestriction("true"))

				.createAlias("mahasiswa", "mahasiswa")

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("kelompokKkn", kelompokKkn));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	/** Memuat/menyegarkan grid dengan halaman anggota sesuai {@link #initCriteria} dan posisi {@link #paging} saat ini. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkn = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatKelompokKkn);
		grid.setRowRenderer(new DetailKelompokKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @return {@code this} sebagai {@link DataLoader}, diteruskan ke helper pencarian mahasiswa agar dapat memicu {@link #loadData(Object)} setelah data ditambahkan. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun panel lengkap daftar anggota {@code kelompokKkn} ke dalam {@code component}:
	 * toolbar (Tambah Anggota — hanya untuk staf; Cetak PDF; filter diterima/belum diterima;
	 * pencarian NIM/nama; cetak laporan; upload Excel massal) di atas grid paging 50 baris.
	 * Lebar kolom "Diterima" dan aksi disesuaikan tergantung apakah pengguna adalah dosen
	 * pembimbing 1 kelompok ini (kolom lebih lebar) atau bukan.
	 *
	 * @param kelompokKkn kelompok KKN yang anggotanya akan ditampilkan/dikelola
	 * @param component   kontainer ZK yang akan diisi (isi sebelumnya dibersihkan)
	 */
	public void display(final KelompokKkn kelompokKkn, final Component component) {
		this.kelompokKkn = kelompokKkn;
		Common.clear(component);

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti KKN"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Anggota", "/img/new.gif");
		button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMahasiswaKelompokKknHelper ambilDataKelompokKknHelper = new AmbilDataMahasiswaKelompokKknHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataKelompokKknHelper.display(kelompokKkn, getDataloader());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("id_kkn", kelompokKkn.getId());
				parameters.put("diterima", diterima.isChecked() ? 1 : 0);
				parameters.put("belumDiterima", belumDiterima.isChecked() ? 1 : 0);
				parameters.put("nim", nim.getValue().trim());
				Report.generatePDFReport(Report.PDF, parameters, "penerima_kelompok_kkn",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		diterima = new MyCheckboxConfig("Diterima");
		belumDiterima = new MyCheckboxConfig("Belum Diterima");

		diterima.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});
		belumDiterima.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});

		diterima.setParent(toolbar);
		belumDiterima.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setWidth("");
		nim.setWidth("70px");
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "mahasiswa", "diterima", "totalNilai",
				"keterangan", "hasil");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(groupbox);

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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hasil");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diterima");
		column.setWidth(tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
				|| (kelompokKkn.getDosen_pembimbing1() != null && tbmuser.ambilDosen() != null
						&& tbmuser.getDosen().getId().equals(kelompokKkn.getDosen_pembimbing1().getId()))) ? "10%"
								: "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
				|| (kelompokKkn.getDosen_pembimbing1() != null && tbmuser.ambilDosen() != null
						&& tbmuser.getDosen().getId().equals(kelompokKkn.getDosen_pembimbing1().getId()))) ? "10%"
								: "8%");

		loadData(null);

		paging.setParent(groupbox);
	}

	/**
	 * Mengimpor anggota kelompok KKN secara massal dari berkas Excel yang sudah diunggah:
	 * berjalan di thread terpisah (agar UI tidak terkunci), membaca sheet pertama baris demi
	 * baris (kolom: NIM/objek mahasiswa, diterima, total nilai, keterangan, hasil), mencocokkan
	 * mahasiswa lewat {@code Common.getSheetContentAsObject} lalu fallback pencarian by NIM bila
	 * sel tidak terbaca sebagai objek. Setiap baris disimpan dalam transaksi tersendiri dengan
	 * rollback eksplisit saat gagal (mencegah "Transaction already active" yang akan
	 * menggagalkan seluruh baris berikutnya). Progres dilaporkan lewat {@link ais.common.LaporanUpload}
	 * (baris berhasil/dilewati/gagal dicatat terpisah) dan indikator busy ZK, dipantau oleh
	 * {@link Timer} yang mem-poll label status hingga proses selesai.
	 *
	 * @param file          berkas Excel (.xlsx) sumber data anggota
	 * @param eventListener callback dipanggil setelah laporan upload selesai disusun
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		// Laporan hasil per baris. Menggantikan Label "peringatan" yang disiapkan untuk
		// menampung keterangan baris bermasalah tetapi TIDAK PERNAH diisi, sehingga baris
		// yang tak cocok hilang tanpa kabar sementara notifikasi tetap berbunyi berhasil.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Mahasiswa KKN");
		laporan.setNamaBerkasSumber(file.getName());

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								// Fallback pencarian lewat NIM. Layar ini sebelumnya TIDAK punya fallback,
								// sehingga baris gagal dicocokkan begitu sel tak terbaca sebagai objek/ID.
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}
							if (mahasiswa != null && mahasiswa.getId() != null) {

								Boolean diterima = Common.getSheetContentAsBoolean(sheet, 1, i);
								Double totalNilai = Common.getSheetContentAsDouble(sheet, 2, i);
								String keterangan = Common.getSheetContentAsString(sheet, 3, i);
								String hasil = Common.getSheetContentAsString(sheet, 4, i);

								MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) session
										.createCriteria(MahasiswaDapatKelompokKkn.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("kelompokKkn", kelompokKkn)).setMaxResults(1)
										.uniqueResult();
								if (mahasiswaDapatKelompokKkn == null) {
									mahasiswaDapatKelompokKkn = new MahasiswaDapatKelompokKkn();

								}
								mahasiswaDapatKelompokKkn.setDiterima(diterima);
								mahasiswaDapatKelompokKkn.setMahasiswa(mahasiswa);
								mahasiswaDapatKelompokKkn.setKelompokKkn(kelompokKkn);
								mahasiswaDapatKelompokKkn.setTotalNilai(totalNilai);
								mahasiswaDapatKelompokKkn.setKeterangan(keterangan);
								mahasiswaDapatKelompokKkn.setHasil(hasil);

								session.getTransaction().begin();
								try {
									Common.refreshSaveOrUpdate(session, mahasiswaDapatKelompokKkn);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() berikutnya
									// melempar "Transaction already active" -- satu baris bermasalah membuat
									// SELURUH baris sesudahnya ikut gagal tanpa jejak.
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
										"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
											+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/KelompokKknHelper.java:549");
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}
}
