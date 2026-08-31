package ais.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.jsoup.Jsoup;
import org.mortbay.util.StringUtil;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.database.model.Va;
import ais.database.model.VirtualAccountBank;
import ais.database.model.file.TugasFileContent;
import ais.database.model.inventory.Produk;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Utilitas berkas/media untuk common download upload. Kelas ini memusatkan resolusi lokasi,
 * stream, upload/download, atau transformasi media agar aturan penyimpanan dan respons file tidak
 * tersebar.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code uploadData()}, {@code
 * prosesUploadData()}, {@code loadEntityById()}, {@code loadSatuanKerjaByYayasan()}, {@code
 * saveUploadLogInfoSafely()}, {@code getClassMetadataSafely()}); mutasi data ({@code
 * pulihkanSessionSetelahGagal()}); pelaporan/ekspor ({@code cetakDataCustomButton()}, {@code
 * cetakDataCustomButton()}); operasi domain lain ({@code kunciLaporan()}, {@code safeEntityId()}, {@code
 * resolveYayasanId()}, {@code rollbackQuietly()}, {@code sebabGagalLengkap()}, {@code
 * closeCurrentSessionQuietly()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonDownloadUpload {

	public static MyToolbarbuttonConfig uploadData(final DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") final Class clazz, final EventListener uploadListener,
			final EventListener uploadListenerAfterSimpan, final Criterion idCrit, final HashMap<String, Object> nilai,
			final String... columns) {

		Tbmuser tbmuser = Common.getCurrentUser();
		final MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.setVisible(tbmuser != null);
		if (Common.bolehKonfigurasi("hanya_admin_saja_yg_boleh_uload", Konfigurasi.TIDAK_AKTIF)) {
			if (!Common.getApakahAdminBolehUpload()) {
				// Sembunyikan LANGSUNG saat tombol dibuat, supaya TIDAK "tampil sebentar
				// (pudar/disabled) lalu hilang". Perilaku lama hanya setDisabled(true) lalu
				// timer setVisible(false), sehingga tombol sempat terlihat ~1 detik dulu.
				upload.setVisible(false);
				// Timer TETAP dipertahankan sebagai PENJAGA: sejumlah pemanggil memanggil
				// upload.setVisible(...) SETELAH uploadData() mengembalikan tombol; bila nilainya
				// true, timer ini menyembunyikannya lagi agar pembatasan
				// "hanya_admin_saja_yg_boleh_uload" tetap dipatuhi. Karena tombol sudah
				// tak-terlihat sejak awal, penjagaan ini tidak lagi menimbulkan kedip pada
				// halaman yang tidak menimpa visibilitas (mis. Siswa).
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						upload.setVisible(false);
					}
				});
			}
		}
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					Criterion c = null;
					HashMap<String, Object> n = null;

					try {
						String nama_crit = (String) Sessions.getCurrent(true)
								.getAttribute("nama_crit_" + clazz.getSimpleName());
						GeneralValueObject generalValueObject = (GeneralValueObject) Sessions.getCurrent(true)
								.getAttribute("nilai_crit_" + clazz.getSimpleName());
						System.out.println("nama_crit " + nama_crit + " -> " + generalValueObject);
						if (nama_crit != null && generalValueObject != null) {
							c = Restrictions.eq(nama_crit, generalValueObject);
							n = new HashMap<String, Object>();
							n.put(nama_crit, generalValueObject);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:140");
					}

					String ta = "";
					try {
						ta = Common.getCurrentTahunAkademik();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:146");
					}
					Sekolah sekolah = null;
					try {
						sekolah = SekolahUtil.getSekolah();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:151");
					}
					prosesUploadData(dataSearchDefault, clazz, uploadListener, uploadListenerAfterSimpan,
							c != null ? c : idCrit, n != null ? n : nilai, media, upload, ta, sekolah, columns);

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}

		});
		return upload;
	}

	/**
	 * Penanda baris untuk laporan upload: dipakai agar pengguna bisa MENGENALI baris mana yang
	 * bermasalah tanpa harus membuka Excel-nya. Diambil dari kolom ke-2 (kolom pertama sesudah id,
	 * biasanya kode/nomor induk/nama); bila kosong dipakai representasi objeknya.
	 */
	private static String kunciLaporan(GeneralValueObject valueObject, XSSFSheet sheet, int barisKe) {
		try {
			String kolomKedua = Common.getSheetContentAsString(sheet, 1, barisKe);
			if (kolomKedua != null && !kolomKedua.trim().isEmpty()) {
				return kolomKedua.trim();
			}
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:kunciLaporan");
		}
		return valueObject == null ? "" : String.valueOf(valueObject);
	}

	private static void prosesUploadData(final DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") final Class clazz, final EventListener uploadListener,
			final EventListener uploadListenerAfterSimpan, final Criterion idCrit, final HashMap<String, Object> nilai,
			Media media, final MyToolbarbuttonConfig upload, final String ta, final Sekolah sekolah,
			final String... columns) throws Exception {
		InputStream inputStream = media.getStreamData();
		// // System.out.println("media = " + media);
		final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
		// // System.out.println("file = " + file.getAbsolutePath());
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		int c;
		while ((c = inputStream.read()) != -1) {
			fileOutputStream.write(c);
		}
		fileOutputStream.close();
		inputStream.close();

		File folder = CommonMedia.getMediaDirectory();

		final File f = new File(folder.getAbsolutePath() + "/" + URLEncoder
				.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(), "UTF-8"));

		Path sourceFile = Paths.get(file.getAbsolutePath());
		Path targetFile = Paths.get(f.getAbsolutePath());

		try {
			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			System.err.format("I/O Error when copying file");
		}

		PerguruanTinggi perguruanTinggiContext = null;
		Yayasan yayasanContext = null;
		try {
			perguruanTinggiContext = PerguruanTinggiUtil.getPerguruanTinggi();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:204");
		}
		try {
			yayasanContext = SekolahUtil.getYayasan();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:208");
		}

		final Long perguruanTinggiId = safeEntityId(perguruanTinggiContext);
		final Long sekolahId = safeEntityId(sekolah);
		final Long yayasanId = resolveYayasanId(sekolah, yayasanContext);
		final String namaBerkasUpload = media.getName();
		final Long uploadLogId = saveUploadLogInfoSafely(media.getName(), f.getAbsolutePath(),
				clazz == null ? "" : clazz.getName());

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final List<String> warnings = new ArrayList<String>();
				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
						"Upload " + (clazz == null ? "Data" : clazz.getSimpleName()));
				laporan.setNamaBerkasSumber(namaBerkasUpload);

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload .."));
				Clients.showBusy(label.getValue());
				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.showBusy(label.getValue());

						/*
						 * SATU JALUR HASIL untuk semua layar CRUD generik (AgamaAction dkk).
						 * Dulu ada dua cabang: bila ada warnings tampil "terdapat kesalahan" + unduh
						 * berkas error, selain itu SELALU "Upload data berhasil dilakukan" -- termasuk
						 * ketika NOL baris tersimpan, karena tidak ada penghitung sama sekali. Kini
						 * dipakai LaporanUpload: rincian per baris (berhasil/gagal/dilewati + sebabnya)
						 * otomatis terunduh, dan kotak pesan menyebut jumlahnya.
						 */
						if (label.getValue().equalsIgnoreCase("Tidak Cocok")) {
							MyMessageboxConfig.show(
								"Upload data gagal dilakukan. File upload tidak sesuai. File yang Anda upload haruslah file yg di download dari menu yang sama.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										dataSearchDefault.onSearchDefault(arg0);
									}
								});
							Clients.clearBusy();
							timer.detach();
						} else if (label.getValue().isEmpty()) {
							Clients.clearBusy();
							timer.detach();
							laporan.selesaikan(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									dataSearchDefault.onSearchDefault(arg0);
								}
							});
						}

					}
				});
				timer.start();

				new Thread(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						Session session = null;
						try {

							XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
							XSSFSheet sheet = workbook.getSheetAt(0);

							String s = (clazz == null ? "data" : clazz.getSimpleName());

							if (!s.toLowerCase().startsWith(sheet.getSheetName().toLowerCase())) {
								label.setValue("Tidak Cocok");
								return;
							}

							ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
							session = HibernateUtil.getSessionFactory().openSession();
							Sekolah uploadSekolah = (Sekolah) loadEntityById(session, Sekolah.class, sekolahId);
							Yayasan uploadYayasan = (Yayasan) loadEntityById(session, Yayasan.class, yayasanId);
							PerguruanTinggi uploadPerguruanTinggi = (PerguruanTinggi) loadEntityById(session,
									PerguruanTinggi.class, perguruanTinggiId);
							SatuanKerja uploadSatuanKerja = loadSatuanKerjaByYayasan(session, uploadYayasan);
							UploadLogInfo uploadLog = (UploadLogInfo) loadEntityById(session, UploadLogInfo.class,
									uploadLogId);

							int rowCount = (sheet.getLastRowNum() + 1);

							int hapusCol = 0;
							try {
								int colCount = (sheet.getRow(0).getLastCellNum() + 1);
								for (int i = 1; i < colCount; i++) {
									try {
										String cols = Common.getSheetContentAsString(sheet, i, 0);
										System.out.println(" cols->" + cols + " ke " + i);
										if (cols.equalsIgnoreCase("Apakah Hapus?")) {
											hapusCol = i;
											System.out.println(" hapusCol " + hapusCol);
											break;
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:344");
									}

								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:349");
							}

							for (int i = 1; i < rowCount; i++) {
								try {

									Long id = Common.getSheetContentAsLong(sheet, 0, i);
									GeneralValueObject valueObject = id == null || id.equals(-1L) ? null
											: (GeneralValueObject) session.createCriteria(clazz)
													.add(idCrit == null ? Restrictions.sqlRestriction("true") : idCrit)
													.add(Restrictions.idEq(id)).uniqueResult();

									System.out.println(valueObject + " id->" + id);

									String kelasAsli = null;
									KelasSiswa kelasSiswa = null;
									if (valueObject == null && !clazz.getName().equals(Matakuliah.class.getName())
											&& !clazz.getName().equals(GajiTabahan.class.getName())
											&& !clazz.getName().equals(Produk.class.getName())) {
										try {
											String kodeKol = null;
											int indexCol = 0;
											for (String col : columns) {
												if (col != null && col.equals("kode")) {
													kodeKol = Common.getSheetContentAsString(sheet, indexCol, i);
													break;
												}
												indexCol++;
											}

											/* Perbaikan NonUniqueResultException: kolom natural-key (kode / nomorInduk) TIDAK unik
											 * di tabel tujuan (bisa berulang antar sekolah/yayasan/periode, apalagi bila idCrit null
											 * sehingga scope-nya seluruh tabel). uniqueResult() polos melempar NonUniqueResultException,
											 * exception itu tertelan catch di bawah -> valueObject tetap null -> baris impor DISISIPKAN
											 * sebagai record baru, bukan meng-update yang lama (sumber data ganda). Pakai setMaxResults(1)
											 * seperti pemanggilan sejenis lain di file ini agar tetap mengambil kandidat pencocokan
											 * tanpa melempar exception. */
											if (kodeKol != null && !kodeKol.trim().isEmpty()) {
												valueObject = (GeneralValueObject) session.createCriteria(clazz)
														.add(idCrit == null ? Restrictions.sqlRestriction("true")
																: idCrit)
														.add(Restrictions.eq("kode", kodeKol)).setMaxResults(1).uniqueResult();
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									} else if (valueObject == null && clazz.getName().equals(Siswa.class.getName())) {
										try {
											String nomorInduk = null;

											int indexCol = 0;
											for (String col : columns) {
												if (col != null && col.equals("nomorInduk")) {
													nomorInduk = Common.getSheetContentAsString(sheet, indexCol, i);
													break;
												}
												if (col != null && col.equalsIgnoreCase("kelas")) {
													kelasAsli = Common.getSheetContentAsString(sheet, indexCol, i);
													break;
												}
												indexCol++;
											}

											if (nomorInduk != null && !nomorInduk.trim().isEmpty() && uploadSekolah != null
													&& uploadSekolah.getId() != null) {
												valueObject = (GeneralValueObject) session.createCriteria(clazz)
														.add(idCrit == null ? Restrictions.sqlRestriction("true")
																: idCrit)
														.add(Restrictions.eq("sekolah", uploadSekolah))
														.add(uploadYayasan == null || uploadYayasan.getId() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("yayasan", uploadYayasan))
														.add(Restrictions.eq("nomorInduk", nomorInduk)).setMaxResults(1).uniqueResult();
											} else if (nomorInduk != null && !nomorInduk.trim().isEmpty()
													&& uploadYayasan != null && uploadYayasan.getId() != null) {
												valueObject = (GeneralValueObject) session.createCriteria(clazz)
														.add(idCrit == null ? Restrictions.sqlRestriction("true")
																: idCrit)
														.add(uploadYayasan == null || uploadYayasan.getId() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("yayasan", uploadYayasan))
														.add(Restrictions.eq("nomorInduk", nomorInduk)).setMaxResults(1).uniqueResult();
											} else {
												valueObject = (GeneralValueObject) clazz.newInstance();
											}

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}

									else if (valueObject == null && clazz.getName().equals(Pegawai.class.getName())) {
										try {
											String nomorInduk = null;

											int indexCol = 0;
											for (String col : columns) {
												if (col != null && col.equals("code")) {
													nomorInduk = Common.getSheetContentAsString(sheet, indexCol, i);
													break;
												}
												if (col != null && col.equalsIgnoreCase("mycode")) {
													kelasAsli = Common.getSheetContentAsString(sheet, indexCol, i);
													break;
												}
												indexCol++;
											}

											if (nomorInduk != null && !nomorInduk.trim().isEmpty()) {
												valueObject = (GeneralValueObject) session.createCriteria(clazz)
														.add(idCrit == null ? Restrictions.sqlRestriction("true")
																: idCrit)
														.setMaxResults(1).add(Restrictions.eq("code", nomorInduk))
														.uniqueResult();
											} else if (kelasAsli != null && !kelasAsli.trim().isEmpty()) {
												valueObject = (GeneralValueObject) session.createCriteria(clazz)
														.add(idCrit == null ? Restrictions.sqlRestriction("true")
																: idCrit)
														.setMaxResults(1).add(Restrictions.eq("mycode", kelasAsli))
														.uniqueResult();
											} else {
												valueObject = (GeneralValueObject) clazz.newInstance();
											}

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}

									if (valueObject == null) {
										valueObject = (GeneralValueObject) clazz.newInstance();
									} else {

										try {
											if (hapusCol > 0) {
												Boolean apakahHapus = Common.getSheetContentAsBoolean(sheet, hapusCol,
														i);
												System.out.println(
														" apakahHapus " + apakahHapus + " hapusCol  " + hapusCol);
												try {
													if (apakahHapus != null && apakahHapus) {

														try {
															session.getTransaction().begin();
															Common.refreshDelete(session, valueObject);
															session.getTransaction().commit();
														} catch (Exception e) {
															session = pulihkanSessionSetelahGagal(session);
															warnings.add("Kesalahan hapus data " + valueObject
																	+ ", error sbb " + sebabGagalLengkap(e));
														}

														continue;
													}
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:497");
													session = pulihkanSessionSetelahGagal(session);
												}

											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:503");
										}

									}

									Map datum = Common.setObjectValues(classMetadata, valueObject, columns, 1, sheet,
											i);

									if (clazz.getName().equals(Siswa.class.getName())) {
										int indexCol = 0;
										for (String col : columns) {

											if (col != null && col.equalsIgnoreCase("kelas")) {
												kelasAsli = Common.getSheetContentAsString(sheet, indexCol, i);
												break;
											}
											indexCol++;
										}

										datum.put("kelas", kelasAsli);
									}

									if (clazz.getName().equals(Siswa.class.getName()) && uploadSekolah != null
											&& uploadSekolah.getId() != null) {
										((Siswa) valueObject).setSekolah(uploadSekolah);
										((Siswa) valueObject).setYayasan(uploadSekolah.getYayasan());
									} else if (clazz.getName().equals(Siswa.class.getName()) && uploadYayasan != null
											&& uploadYayasan.getId() != null) {
										((Siswa) valueObject).setYayasan(uploadYayasan);
									}
									if (clazz.getName().equals(Guru.class.getName()) && uploadSekolah != null
											&& uploadSekolah.getId() != null) {
										((Guru) valueObject).setSekolah(uploadSekolah);
										((Guru) valueObject).setYayasan(uploadSekolah.getYayasan());
									} else if (clazz.getName().equals(Guru.class.getName()) && uploadYayasan != null
											&& uploadYayasan.getId() != null) {
										((Guru) valueObject).setYayasan(uploadYayasan);
									}
									if (clazz.getName().equals(Matapelajaran.class.getName()) && uploadSekolah != null
											&& uploadSekolah.getId() != null) {
										((Matapelajaran) valueObject).setSekolah(uploadSekolah);
										((Matapelajaran) valueObject).setYayasan(uploadSekolah.getYayasan());
									} else if (clazz.getName().equals(Matapelajaran.class.getName()) && uploadYayasan != null
											&& uploadYayasan.getId() != null) {
										((Matapelajaran) valueObject).setYayasan(uploadYayasan);
									}
									if (clazz.getName().equals(JadwalPelajaran.class.getName()) && uploadSekolah != null
											&& uploadSekolah.getId() != null) {
										((JadwalPelajaran) valueObject).setSekolah(uploadSekolah);
										((JadwalPelajaran) valueObject).setYayasan(uploadSekolah.getYayasan());
									} else if (clazz.getName().equals(JadwalPelajaran.class.getName())
											&& uploadYayasan != null && uploadYayasan.getId() != null) {
										((JadwalPelajaran) valueObject).setYayasan(uploadYayasan);
									}
									if (clazz.getName().equals(Pegawai.class.getName())) {
										Pegawai pegawai = ((Pegawai) valueObject);
										if (pegawai.getSatuanKerja() == null) {
											pegawai.setSatuanKerja(uploadSatuanKerja);
										}
									}

									System.out.println("datum => " + datum);

									Map datumBaru = new HashMap();
									try {
										int colCount = sheet.getRow(i).getLastCellNum();
										for (int col = 0; col < colCount; col++) {
											try {
												String colName = Common.getSheetContentAsString(sheet, col, 0);
												String colVal = Common.getSheetContentAsString(sheet, col, i);
												datumBaru.put(colName, colVal);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:574");
												// TODO: handle exception
											}
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:578");
										// TODO: handle exception
									}

									datumBaru.putAll(datum);

									List apakahSimpan = new ArrayList();
									if (uploadListener != null) {
										uploadListener.onEvent(new Event("", upload, new Object[] { valueObject,
												session, datumBaru, apakahSimpan, sheet, i, warnings }));
									}

									if (nilai != null) {
										for (String key : nilai.keySet()) {
											classMetadata.setPropertyValue(valueObject, key, nilai.get(key),
													EntityMode.POJO);
										}
									}

									if (uploadSekolah != null && uploadSekolah.getId() != null
											&& clazz.getName().equals(Siswa.class.getName())) {
										int indexCol = 0;
										for (String col : columns) {
											if (col != null && col.equalsIgnoreCase("kelas")) {
												kelasAsli = Common.getSheetContentAsString(sheet, indexCol, i);
												break;
											}
											indexCol++;
										}

										if (uploadSekolah != null && uploadSekolah.getId() != null && kelasAsli != null
												&& !kelasAsli.trim().isEmpty()) {

											kelasSiswa = (KelasSiswa) session.createCriteria(KelasSiswa.class)
													.add(Restrictions.ilike("nama", kelasAsli.trim()))
													.add(Restrictions.eq("sekolah", uploadSekolah))
													.add(Restrictions.eq("tahunAjaran", ta)).setMaxResults(1)
													.uniqueResult();
											if (kelasSiswa == null) {
												kelasSiswa = new KelasSiswa();
												kelasSiswa.setSekolah(uploadSekolah);
												kelasSiswa.setYayasan(uploadSekolah.getYayasan());
												kelasSiswa.setNama(kelasAsli.trim());
												kelasSiswa.setTahunAjaran(ta);

												try {
													session.getTransaction().begin();
													session.save(kelasSiswa);
													session.getTransaction().commit();
												} catch (Exception e) {
													session = pulihkanSessionSetelahGagal(session);
													warnings.add("Kesalahan simpan data " + kelasSiswa + ", error sbb "
															+ sebabGagalLengkap(e) + ". Data sbb : " + datum);
												}

											}

											((Siswa) valueObject).setSekolah(uploadSekolah);
											((Siswa) valueObject).setYayasan(uploadSekolah.getYayasan());
											if (ta.equalsIgnoreCase(Common.getCurrentTahunAkademik())) {
												((Siswa) valueObject).setKelas(kelasSiswa);
											}
										}
									}

									try {
										if (uploadSekolah != null && uploadSekolah.getId() != null
												&& java.util.Arrays.asList(classMetadata.getPropertyNames()).contains("sekolah")) {
											classMetadata.setPropertyValue(valueObject, "sekolah", uploadSekolah,
													EntityMode.POJO);
										}
									} catch (org.hibernate.HibernateException e) {
										// Entitas tujuan (valueObject) tidak punya properti "sekolah" -> mapping
										// legacy/sudah usang. Lewati saja properti ini, jangan gagalkan baris.
										ais.common.ErrorAuditUtil.record(e,
												"auto-audit(properti sekolah tidak dikenal entitas ini, dilewati) src/ais/common/CommonDownloadUpload.java:638");
									}

									try {
										if (uploadYayasan != null && uploadYayasan.getId() != null
												&& java.util.Arrays.asList(classMetadata.getPropertyNames()).contains("yayasan")) {
											classMetadata.setPropertyValue(valueObject, "yayasan", uploadYayasan,
													EntityMode.POJO);
										}
									} catch (org.hibernate.HibernateException e) {
										ais.common.ErrorAuditUtil.record(e,
												"auto-audit(properti yayasan tidak dikenal entitas ini, dilewati) src/ais/common/CommonDownloadUpload.java:646");
									}

									try {
										if (uploadPerguruanTinggi != null && uploadPerguruanTinggi.getId() != null
												&& java.util.Arrays.asList(classMetadata.getPropertyNames()).contains("perguruanTinggi")) {
											classMetadata.setPropertyValue(valueObject, "perguruanTinggi",
													uploadPerguruanTinggi, EntityMode.POJO);
										}
									} catch (org.hibernate.HibernateException e) {
										// Root cause ERROR 18 (ID 72): "Unable to resolve property: perguruanTinggi"
										// -> entitas tujuan (valueObject) memang tidak memiliki properti ini di
										// mapping-nya. Tangkap spesifik HibernateException (mencakup juga
										// QueryException, sub-classnya) supaya tetap ketahuan bila ada exception
										// lain yang tak terduga, lalu lewati properti ini dan lanjutkan proses
										// baris berikutnya alih-alih menggagalkan seluruh upload.
										ais.common.ErrorAuditUtil.record(e,
												"auto-audit(properti perguruanTinggi tidak dikenal entitas ini, dilewati) src/ais/common/CommonDownloadUpload.java:654");
									}

									try {
										if (uploadLog != null && uploadLog.getId() != null
												&& java.util.Arrays.asList(classMetadata.getPropertyNames()).contains("uploadLog")) {
											classMetadata.setPropertyValue(valueObject, "uploadLog", uploadLog,
													EntityMode.POJO);
										}
									} catch (org.hibernate.HibernateException e) {
										ais.common.ErrorAuditUtil.record(e,
												"auto-audit(properti uploadLog tidak dikenal entitas ini, dilewati) src/ais/common/CommonDownloadUpload.java:662");
									}

									if (apakahSimpan.isEmpty()) {
										try {
											session.getTransaction().begin();
											session.saveOrUpdate(valueObject);
											session.getTransaction().commit();
											laporan.catatBerhasil(i, kunciLaporan(valueObject, sheet, i),
												String.valueOf(valueObject));
										} catch (Exception e) {
											// PENTING: rollback SAJA tidak cukup -- entitas gagal-insert (id masih
											// null) tetap tertinggal di persistence context session ini. Baris
											// berikutnya yang mem-flush session yang sama akan ikut gagal beruntun
											// dengan "AssertionFailure: null id ... (don't flush the Session after
											// an exception occurs)". pulihkanSessionSetelahGagal() membuang entitas
											// beracun via clear(), atau membuka session baru bila session ini sudah
											// tak bisa dipakai lagi.
											session = pulihkanSessionSetelahGagal(session);
											String sebab = sebabGagalLengkap(e);
											warnings.add("Kesalahan simpan data " + valueObject + ", error sbb "
													+ sebab + ". Data sbb : " + datum);
											laporan.catatGagal(i, kunciLaporan(valueObject, sheet, i), sebab);
										}
										label.setValue("Upload data \"" + valueObject.toString() + "\" ("
												+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

										if (uploadListenerAfterSimpan != null) {
											uploadListenerAfterSimpan.onEvent(new Event("", upload, new Object[] {
													valueObject, session, datumBaru, apakahSimpan, warnings }));

										}
									} else {
										/*
										 * apakahSimpan diisi oleh uploadListener milik layar ybs sebagai ALASAN baris ini
										 * tidak boleh disimpan (validasi khas menu). Sebelumnya alasan itu hilang begitu
										 * saja dan barisnya raib tanpa kabar; kini ikut tercatat di laporan.
										 */
										StringBuilder alasan = new StringBuilder();
										for (Object a : apakahSimpan) {
											if (a == null) {
												continue;
											}
											if (alasan.length() > 0) {
												alasan.append("; ");
											}
											alasan.append(String.valueOf(a));
										}
										laporan.catatDilewati(i, kunciLaporan(valueObject, sheet, i),
											alasan.length() == 0 ? "Baris tidak disimpan (ditolak pemeriksaan menu ini)"
												: alasan.toString());
									}

									if (valueObject != null && valueObject.getId() != null && uploadSekolah != null
											&& kelasSiswa != null && kelasSiswa.getId() != null
											&& clazz.getName().equals(Siswa.class.getName())) {
										Siswa siswa = (Siswa) valueObject;
										// System.out.println("kelasSiswa => " +
										// kelasSiswa + ", siswa => " + siswa);

										KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) session
												.createCriteria(KelasSiswaPunyaSiswa.class)
												.add(Restrictions.eq("kelasSiswa", kelasSiswa))
												.add(Restrictions.eq("siswa", siswa)).setMaxResults(1).uniqueResult();
										if (kelasSiswaPunyaSiswa == null) {
											kelasSiswaPunyaSiswa = new KelasSiswaPunyaSiswa();
											kelasSiswaPunyaSiswa.setKelasSiswa(kelasSiswa);
											kelasSiswaPunyaSiswa.setSiswa(siswa);
											try {
												session.getTransaction().begin();
												session.save(kelasSiswaPunyaSiswa);
												session.getTransaction().commit();
											} catch (Exception e) {
												session = pulihkanSessionSetelahGagal(session);
												warnings.add("Kesalahan simpan data " + kelasSiswaPunyaSiswa
														+ ", error sbb " + sebabGagalLengkap(e) + ". Data sbb : " + datum);
											}
										}

									}

								} catch (Exception e) {
									// Exception di luar blok simpan/hapus terlokalisir (mis. gagal saat
									// membaca sel/menyusun properti) bisa saja terjadi SETELAH transaksi
									// sempat dimulai tanpa sempat di-commit/rollback secara eksplisit
									// (mis. exception dilempar tepat di antara begin() dan commit() pada
									// bagian kode lain di baris ini). Pulihkan session di sini juga supaya
									// baris berikutnya tidak ikut gagal beruntun akibat session yang
									// tercemar/transaksi menggantung.
									session = pulihkanSessionSetelahGagal(session);
									laporan.catatGagal(i, kunciLaporan(null, sheet, i), sebabGagalLengkap(e));
									Common.tampilErrorJikaAdmin(e);
								}

							}

						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonDownloadUpload.java:731");
						} finally {
							closeOpenedSessionQuietly(session, true);
							closeCurrentSessionQuietly(false);
							// Keterangan teknis yang dulu diunduh sebagai berkas error terpisah kini ikut
							// dicetak di laporan, supaya seluruh informasi ada dalam SATU berkas.
							for (String w : warnings) {
								laporan.tambahCatatan(w);
							}
							label.setValue("");
						}
					}

				}).start();

			}
		}, Common.getBahasaConfig("Harap tunggu.. sedang melakukan proses upload data.."));
	}

	private static Long safeEntityId(GeneralValueObject value) {
		try {
			return value == null ? null : value.getId();
		} catch (Throwable e) {
			return null;
		}
	}

	private static Long resolveYayasanId(Sekolah sekolah, Yayasan yayasan) {
		Long yayasanId = safeEntityId(yayasan);
		if (yayasanId != null) {
			return yayasanId;
		}
		try {
			return sekolah == null || sekolah.getYayasan() == null ? null : sekolah.getYayasan().getId();
		} catch (Throwable e) {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private static Object loadEntityById(Session session, Class clazz, Long id) {
		if (session == null || clazz == null || id == null) {
			return null;
		}
		try {
			return session.get(clazz, id);
		} catch (Throwable e) {
			return null;
		}
	}

	private static SatuanKerja loadSatuanKerjaByYayasan(Session session, Yayasan yayasan) {
		if (session == null || yayasan == null || yayasan.getId() == null) {
			return null;
		}
		try {
			return (SatuanKerja) session.createCriteria(SatuanKerja.class).add(Restrictions.isNull("parent"))
					.add(Restrictions.eq("yayasan", yayasan)).setMaxResults(1).uniqueResult();
		} catch (Throwable e) {
			return null;
		}
	}

	private static Long saveUploadLogInfoSafely(String nama, String keterangan, String className) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			UploadLogInfo uploadLog = new UploadLogInfo();
			uploadLog.setNama(nama == null || nama.trim().length() == 0 ? "upload.xlsx" : nama);
			uploadLog.setKeterangan(keterangan);
			uploadLog.setClassName(className);
			try {
				Tbmuser currentUser = Common.getCurrentUser();
				if (currentUser != null) {
					uploadLog.setOlehId(currentUser.getUserId());
					uploadLog.setOleh(currentUser.getUserNama());
				}
			} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:803");
			}
			session.getTransaction().begin();
			session.save(uploadLog);
			session.getTransaction().commit();
			return uploadLog.getId();
		} catch (Throwable e) {
			rollbackQuietly(session);
			return null;
		} finally {
			closeOpenedSessionQuietly(session, false);
		}
	}

	private static void rollbackQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:825");
		}
	}

	/**
	 * Pemulihan session Hibernate SETELAH gagal simpan/hapus di tengah perulangan upload Excel.
	 *
	 * <p>Akar masalah produksi: ketika satu baris gagal insert (mis. {@code DataException} karena
	 * nilai kolom kelewat panjang), entitas gagal (id masih null) TETAP tersimpan di persistence
	 * context sesi bila hanya di-rollback tanpa {@code clear()}. Flush berikutnya (baris upload
	 * berikutnya) ikut memuat entitas beracun itu sehingga Hibernate melempar
	 * {@code AssertionFailure: null id ... (don't flush the Session after an exception occurs)}
	 * dan SEMUA baris sesudahnya ikut gagal beruntun.</p>
	 *
	 * <p>Urutan pemulihan: (1) rollback transaksi aktif bila ada; (2) {@code session.clear()} untuk
	 * membuang entitas beracun bila session masih terbuka; (3) bila session sudah tertutup atau
	 * {@code clear()} sendiri gagal, tutup paksa lalu buka session baru supaya baris berikutnya
	 * tetap bisa diproses.</p>
	 *
	 * @return session yang sudah bersih dan siap dipakai lagi (bisa jadi session BARU, bukan yang
	 *         dioper masuk) — caller WAJIB menampung nilai kembaliannya, mis.
	 *         {@code session = pulihkanSessionSetelahGagal(session);}
	 */
	private static Session pulihkanSessionSetelahGagal(Session session) {
		rollbackQuietly(session);
		try {
			if (session != null && session.isOpen()) {
				session.clear();
				return session;
			}
		} catch (Throwable eClear) {
			ais.common.ErrorAuditUtil.record(eClear,
					"auto-audit(pulihkan-session-clear-gagal) src/ais/common/CommonDownloadUpload.java:pulihkanSessionSetelahGagal");
		}
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (Throwable eClose) {
			ais.common.ErrorAuditUtil.record(eClose,
					"auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:pulihkanSessionSetelahGagal-close");
		}
		return HibernateUtil.getSessionFactory().openSession();
	}

	/**
	 * Susun keterangan gagal LENGKAP dengan rantai {@code cause} (maks 3 tingkat) supaya baris
	 * yang gagal karena mis. {@code DataException} (truncation nilai kolom) langsung menyebut
	 * pesan SQL aslinya (biasanya menyebut nama kolom/batas panjang), bukan sekadar
	 * "could not insert: [...]" yang generik.
	 */
	private static String sebabGagalLengkap(Throwable e) {
		if (e == null) {
			return "(tanpa keterangan)";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(e.getClass().getSimpleName());
		if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
			sb.append(": ").append(e.getMessage().trim());
		}
		Throwable cause = e.getCause();
		int guard = 0;
		while (cause != null && cause != e && guard++ < 3) {
			sb.append(" | disebabkan oleh ").append(cause.getClass().getSimpleName());
			if (cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
				sb.append(": ").append(cause.getMessage().trim());
			}
			cause = cause.getCause();
		}
		return sb.toString();
	}

	private static void closeCurrentSessionQuietly(boolean debug) {
		try {
			HibernateUtil.closeSession();
		} catch (Throwable e) {
			logDebug(debug, e);
		}
	}

	// =====================================================================================
	// 1. METHOD OVERLOADING (Backward Compatibility agar class lain tidak error)
	// =====================================================================================
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(final Class clazz,
			final DataCriteriaWithColumn dataCriteria, final List dataParam, String buttonLabel, String buttonImage,
			final List<String> columnHeadersAdding, final EventListener dataAdding, final Boolean adaTambahan,
			final List<String> columnHeadersAddingTambahan, final String namatabTambahan,
			final String... columnsTemporary) {

		// Memanggil method utama dengan default debug = false
		return cetakDataCustomButton(clazz, dataCriteria, dataParam, buttonLabel, buttonImage, columnHeadersAdding,
				dataAdding, adaTambahan, columnHeadersAddingTambahan, namatabTambahan, false, columnsTemporary);
	}

	// =====================================================================================
	// 2. METHOD UTAMA (Dengan parameter debug & fully optimized Multi-Threading)
	// =====================================================================================
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(final Class clazz,
			final DataCriteriaWithColumn dataCriteria, final List dataParam, String buttonLabel, String buttonImage,
			final List<String> columnHeadersAdding, final EventListener dataAdding, final Boolean adaTambahan,
			final List<String> columnHeadersAddingTambahan, final String namatabTambahan, final boolean debug,
			final String... columnsTemporary) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final boolean admin = Common.getApakahAdmin();

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				String fn = Sessions.getCurrent().getWebApp().getRealPath("/tmp/Daftar_"
						+ (namatabTambahan != null && !namatabTambahan.trim().isEmpty() ? namatabTambahan
								: (clazz == null ? "data" : clazz.getSimpleName()))
						+ "_"
						+ URLEncoder.encode(Common.dateFormat62.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				final Label fileLabel = new Label(fn);

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());
							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
								PesanFormalHelper.tampilkanGagal("pembuatan/pengunduhan berkas Excel",
										"Terjadi kesalahan pada saat sistem memproses data dan menulis berkas Excel di "
												+ "server, kemungkinan disebabkan data yang terlalu besar, format data "
												+ "yang tidak sesuai, atau keterbatasan ruang penyimpanan server.",
										new String[] { "Coba persempit filter/rentang data lalu ulangi proses unduh.",
												"Ulangi proses ini beberapa saat lagi." });
							} else if (label.getValue().isEmpty()) {
								int noUrutColumn = columnsTemporary.length
										+ (columnHeadersAdding == null ? 0 : columnHeadersAdding.size());
								Common.displayXlsx(fileLabel.getValue(), intbox, noUrutColumn);
								timer.detach();
							}
						} catch (Exception e) {
							if (debug)
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:902");
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				try {
					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {
						@SuppressWarnings({ "deprecation", "unchecked" })
						@Override
						public void run() {
							// =========================================================
							// MANAJEMEN SESSION UTAMA & AMAN
							// =========================================================
							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();

								Object[] obj = dataCriteria.initCriteria(true);
								Object d = obj[0];

								final List data;
								if (d instanceof Criteria) {
									Criteria crit = (Criteria) d;
									data = dataParam != null ? dataParam
											: ConstantValues.simpleList(crit.setMaxResults(1048576), clazz);
								} else {
									// d (hasil initCriteria[0]) bisa null → cegah NPE pada data.size().
									data = (d == null ? new java.util.ArrayList() : (List) d);
								}

								System.out.println("data -> " + data.size());

								final String[] columns = (obj.length > 1) ? (String[]) obj[1] : columnsTemporary;

								intbox.setValue(data.size());

								final XSSFWorkbook workbook = new XSSFWorkbook();
								final XSSFCellStyle hlink_style = UIUtil.solid_LIGHT_GRAY(workbook);
								final XSSFCellStyle bodystyle = UIUtil.solid_WHITE(workbook);

								Class classBaru = clazz;
								boolean empty = data.isEmpty();
								if (!empty) {
									classBaru = data.get(0).getClass();
									fileLabel
											.setValue(Common.REAL_PATH + "/tmp/"
													+ (classBaru == null ? "data" : classBaru.getSimpleName()) + "_"
													+ URLEncoder.encode(Common.datetimeFormat2s.get()
															.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
													+ ".xlsx");
								}

								final XSSFSheet sheet = workbook
										.createSheet((classBaru == null ? "data" : classBaru.getSimpleName()));
								final XSSFSheet sheetTambahan = adaTambahan
										? workbook.createSheet(Common.getBahasaConfig(namatabTambahan))
										: null;

								if (sheetTambahan != null)
									sheetTambahan.setDefaultColumnWidth(20);
								sheet.setDefaultColumnWidth(20);

								XSSFRow rowhead = sheet.createRow((short) 0);
								for (int i = 0; i < columns.length; i++) {
									String colName = Common.getBahasaConfig(columns[i].toUpperCase());
									XSSFCell cell = rowhead.createCell(i);
									cell.setCellValue(colName);
									cell.setCellStyle(hlink_style);
								}

								if (columnHeadersAdding != null) {
									for (int i = 0; i < columnHeadersAdding.size(); i++) {
										String colNameAdding = Common
												.getBahasaConfig(columnHeadersAdding.get(i).toUpperCase());
										XSSFCell cell = rowhead.createCell(columns.length + i);
										cell.setCellValue(colNameAdding);
										cell.setCellStyle(hlink_style);
									}
								}

								int noUrutColumn = columns.length
										+ (columnHeadersAdding == null ? 0 : columnHeadersAdding.size());
								XSSFCell celln = rowhead.createCell(noUrutColumn);
								celln.setCellValue(ConstantValues.NO_URUT.toUpperCase());
								celln.setCellStyle(hlink_style);

								if (admin) {
									celln = rowhead.createCell(noUrutColumn + 1);
									celln.setCellValue("Apakah Hapus?");
									celln.setCellStyle(hlink_style);
								}

								final XSSFRow rowheadTambahan = (adaTambahan && columnHeadersAddingTambahan != null)
										? sheetTambahan.createRow((short) 0)
										: null;
								if (rowheadTambahan != null) {
									for (int i = 0; i < columnHeadersAddingTambahan.size(); i++) {
										String colNameAdding = columnHeadersAddingTambahan.get(i).toUpperCase();
										XSSFCell cell = rowheadTambahan.createCell(i);
										cell.setCellValue(colNameAdding);
										cell.setCellStyle(hlink_style);
									}
								}

								if (!empty) {
									Class<? extends Object> clazzT = data.get(0).getClass();
									final ClassMetadata metadata = HibernateUtil.getClassMetadata(clazzT);
									final int totalData = data.size();

									// =========================================================
									// PARALLEL DATA EXTRACTION TERBATAS
									// =========================================================
									// Hibernate entity/proxy tidak aman jika dibaca oleh terlalu banyak thread.
									// Jumlah thread dibatasi agar export tetap responsif tanpa membebani DB/JVM.
									final int exportThreadCount = getSafeExportThreadCount(totalData);
									final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
											.newFixedThreadPool(exportThreadCount);
									final java.util.concurrent.atomic.AtomicInteger processedCounter = new java.util.concurrent.atomic.AtomicInteger(
											0);

									// Menyimpan hasil pemrosesan agar aman dari tabrakan Thread saat ditulis ke
									// Excel
									final java.util.Map<Integer, Object[]> rowDataMap = new java.util.concurrent.ConcurrentHashMap<Integer, Object[]>();

									for (int i = 0; i < totalData; i++) {
										final int index = i;
										final Object o = data.get(index);

										executor.submit(new Runnable() {
											@Override
											public void run() {
												if (o == null)
													return;

												// 1. UPDATE DB VIRTUAL ACCOUNT SECARA THREAD-SAFE
												if (o instanceof VirtualAccountBank) {
													VirtualAccountBank vaBank = (VirtualAccountBank) o;
													if (vaBank.getVa() == null || (vaBank.getKegiatan() != null
															&& vaBank.getWaktuBayar() == null)) {
														Session threadSession = null;
														try {
															// Buka koneksi DB independen khusus untuk thread ini agar
															// aman
															threadSession = HibernateUtil.getSessionFactory()
																	.openSession();
															threadSession.getTransaction().begin();

															boolean updated = false;
															if (vaBank.getVa() == null) {
																Va va = new Va();
																va.setKode(vaBank.getKode());
																threadSession.save(va);
																vaBank.setVa(va);
																updated = true;
															}

															if (vaBank.getKegiatan() != null
																	&& vaBank.getWaktuBayar() == null) {
																Date tanggal = (Date) threadSession
																		.createCriteria(CicilanPembayaran.class)
																		.add(Restrictions.eq("refVa", vaBank.getId()))
																		.setProjection(Projections.property("tanggal"))
																		.setMaxResults(1).uniqueResult();
																vaBank.setWaktuBayar(tanggal);
																updated = true;
															}

															if (updated) {
																threadSession.update(vaBank);
															}
															threadSession.getTransaction().commit();
														} catch (Exception e) {
															if (threadSession != null
																	&& threadSession.getTransaction().isActive())
																threadSession.getTransaction().rollback();
															if (debug)
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1082");
														} finally {
															closeOpenedSessionQuietly(threadSession, debug);
														}
													}
												}

												// 2. EKSTRAKSI ISI KOLOM
												Object[] cellValues = new Object[columns.length];
												for (int j = 0; j < columns.length; j++) {
													try {
														cellValues[j] = processDataContent(metadata, o, columns[j], debug);
													} catch (Throwable cellError) {
														cellValues[j] = "";
														logDebug(debug, cellError);
													}
												}
												rowDataMap.put(index, cellValues);

												// 3. UPDATE LABEL PROGRESS BAR
												int currentProcessed = processedCounter.incrementAndGet();
												label.setValue("Mengekstrak data paralel... (" + Common.numberFormat
														.get().format(currentProcessed * 100.0 / totalData) + " %)");
											}
										});
									}

									// Tunggu hingga semua 100 thread selesai memproses data
									executor.shutdown();
									executor.awaitTermination(Long.MAX_VALUE,
											java.util.concurrent.TimeUnit.NANOSECONDS);

									// =========================================================
									// SEQUENTIAL EXCEL WRITING (Mencegah Excel Corrupt)
									// =========================================================
									int rowIndex = 0;
									for (int i = 0; i < totalData; i++) {
										Object o = data.get(i);
										Object[] cellValues = rowDataMap.get(i);

										if (o == null || cellValues == null)
											continue;
										rowIndex++;

										label.setValue("Menulis ke file Excel... ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / totalData)
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);
										XSSFRow rowTambahan = (sheetTambahan != null)
												? sheetTambahan.createRow(rowIndex)
												: null;

										for (int j = 0; j < cellValues.length; j++) {
											Object content = cellValues[j];
											try {
												if ((content instanceof Double) && ((Double) content) < 9999.0
														&& ((Double) content) > -9999.0) {
													XSSFCell cell = row.createCell(j);
													cell.setCellStyle(bodystyle);
													cell.setCellValue((Double) content);
												} else {
													if (content instanceof Double) {
														content = Common.numberFormat1.get().format(content);
													}
													XSSFCell cell = row.createCell(j);
													cell.setCellStyle(bodystyle);
													cell.setCellValue(content == null
															|| content.toString().trim().equalsIgnoreCase("null") ? ""
																	: content.toString().replaceAll("<br>", "\n"));
												}
											} catch (Exception e) {
												if (debug)
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1155");
												XSSFCell cell = row.createCell(j);
												cell.setCellStyle(bodystyle);
												cell.setCellValue("");
											}
										}

										if (dataAdding != null) {
											try {
												Object id = metadata.getIdentifier(o, EntityMode.POJO);
												Object[] objects = new Object[] { o, id, row, workbook, rowTambahan,
														rowheadTambahan, bodystyle, hlink_style };
												dataAdding.onEvent(new Event("", null, objects));
											} catch (Exception e) {
												if (debug)
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1170");
											}
										}

										XSSFCell cellUrut = row.createCell(noUrutColumn);
										cellUrut.setCellStyle(bodystyle);
										cellUrut.setCellValue(rowIndex);

										if (admin) {
											XSSFCell cellAdmin = row.createCell(noUrutColumn + 1);
											cellAdmin.setCellStyle(bodystyle);
											cellAdmin.setCellValue("FALSE");
										}

										// OPTIMASI MEMORI saat iterasi penulisan
										if (rowIndex % 100 == 0) {
											flushAndClearQuietly(session, debug);
										}
									}
								}

								// Autosize columns aman
								for (int i = 0; i < columns.length; i++) {
									try {
										sheet.autoSizeColumn(i);
									} catch (Exception e) {
										if (debug)
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1197");
									}
								}

								if (columnHeadersAdding != null && sheetTambahan != null) {
									for (int i = 0; i < columnHeadersAdding.size(); i++) {
										try {
											sheetTambahan.autoSizeColumn(i);
										} catch (Exception e) {
											if (debug)
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1207");
										}
									}
								}

								boolean fileWritten = false;
								FileOutputStream fileOut = null;
								try {
									File outputFile = new File(fileLabel.getValue());
									CommonFileUtil.ensureWritableFile(outputFile, 50L * 1024L * 1024L);
									fileOut = new FileOutputStream(outputFile);
									workbook.write(fileOut);
									fileWritten = true;
								} catch (IOException e) {
									if (debug)
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1218");
									Common.tampilErrorJikaAdmin(e);
								} finally {
									if (fileOut != null) try { fileOut.close(); } catch (IOException closeError) {
										ais.common.ErrorAuditUtil.record(closeError, "auto-audit src/ais/common/CommonDownloadUpload.java:close-xlsx");
									}
								}

								if (d instanceof Criteria) {
									data.clear();
								}

								// Sinyal Selesai ke Timer UI
								label.setValue(fileWritten ? "" : "-");

							} catch (Exception e) {
								if (debug)
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1231");
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-"); // Sinyal Error ke Timer
							} finally {
								// =========================================================
								// KEWAJIBAN PEMBERSIHAN KONEKSI (Mencegah Database Lock)
								// =========================================================
								closeOpenedSessionQuietly(session, debug);
							}
						}
					}).start();

				} catch (Exception e) {
					if (debug)
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1245");
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		toolbarbutton.addEventListener("onClick", eventListener);
		toolbarbutton.setAttribute("eventListener", eventListener);

		return toolbarbutton;
	}

	// =====================================================================================
	// 3. PRIVATE STATIC HELPER: Engine Pemroses Field Data (Sangat Hemat Memory)
	// =====================================================================================
	@SuppressWarnings("deprecation")
	private static Object processDataContent(ClassMetadata metadata, Object o, String property, boolean debug) {
		if (property == null)
			return null;

		Object content = null;
		boolean jadikanText = property.contains("-text");
		boolean jadikanNumber = property.contains("-number");
		String cleanProperty = property.split("-")[0];

		if (StringUtils.contains(cleanProperty, "||")) {
			String[] ds = StringUtils.split(cleanProperty, "||");
			for (String dss : ds) {
				try {
					Object hasil = processDataContent(metadata, o, dss, debug); // Rekursi aman
					if (hasil != null && !hasil.toString().trim().isEmpty()) {
						content = hasil;
						break;
					}
				} catch (Exception e) {
					if (debug)
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1281");
				}
			}
		} else if (cleanProperty.equalsIgnoreCase("kelas") && (o instanceof Siswa)) {
			try {
				content = ((Siswa) o).getKelas() == null ? "" : ((Siswa) o).getKelas().getNama();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1287");
			}
		} else if (cleanProperty.equalsIgnoreCase("GuruBk") && (o instanceof Siswa)) {
			try {
				content = ((Siswa) o).getKelas() == null ? "" : ((Siswa) o).getKelas().getGuruBk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1292");
			}
		} else if (cleanProperty.equalsIgnoreCase("GuruPembina") && (o instanceof Siswa)) {
			try {
				content = ((Siswa) o).getKelas() == null ? "" : ((Siswa) o).getKelas().getGuruPembina();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1297");
			}
		} else if (cleanProperty.equalsIgnoreCase("ipk") && (o instanceof Mahasiswa)) {
			try {
				content = Common.singkronkanKrsMahasiswa((Mahasiswa) o).getIpk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1302");
			}
		} else if (cleanProperty.equalsIgnoreCase("namaTemp") && (o instanceof TugasFileContent)) {
			try {
				content = ((TugasFileContent) o).getNamaTemp();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1307");
			}
		} else if (cleanProperty.equalsIgnoreCase("id") && (o instanceof TugasFileContent)) {
			try {
				content = ((TugasFileContent) o).getId();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1312");
			}
		} else if (cleanProperty.equalsIgnoreCase("keterangan") && (o instanceof TugasFileContent)) {
			try {
				content = ((TugasFileContent) o).getKeterangan();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1317");
			}
		} else if (cleanProperty.equalsIgnoreCase("nilai") && (o instanceof TugasFileContent)) {
			try {
				content = ((TugasFileContent) o).getNilai();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1322");
			}
		} else if (cleanProperty.equalsIgnoreCase("sksk") && (o instanceof Mahasiswa)) {
			try {
				content = Common.singkronkanKrsMahasiswa((Mahasiswa) o).getSksk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1327");
			}
		} else if (cleanProperty.equalsIgnoreCase("ipk") && (o instanceof BiodataMahasiswa)) {
			try {
				content = Common.singkronkanKrsMahasiswa(((BiodataMahasiswa) o).getMahasiswa()).getIpk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1332");
			}
		} else if (cleanProperty.equalsIgnoreCase("sksk") && (o instanceof BiodataMahasiswa)) {
			try {
				content = Common.singkronkanKrsMahasiswa(((BiodataMahasiswa) o).getMahasiswa()).getSksk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1337");
			}
		} else if (cleanProperty.equalsIgnoreCase("status_mhs") && (o instanceof Mahasiswa)) {
			try {
				HistoryStatusMahasiswa krsMhs = Common.currentStatus((Mahasiswa) o);
				content = krsMhs.getStatusMahasiswa() == null ? "" : krsMhs.getStatusMahasiswa().getNama();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1343");
			}
		} else if (cleanProperty.equalsIgnoreCase("status_mhs") && (o instanceof BiodataMahasiswa)) {
			try {
				HistoryStatusMahasiswa krsMhs = ais.action.master.helper.HistoryStatusMahasiswaUtil
						.currentStatus(((BiodataMahasiswa) o).getMahasiswa());
				content = krsMhs.getStatusMahasiswa() == null ? "" : krsMhs.getStatusMahasiswa().getNama();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1350");
			}
		} else if (StringUtils.contains(cleanProperty, ",")) {
			String[] subproperties = StringUtils.split(cleanProperty, ",");
			for (String subproperty : subproperties) {
				try {
					Object hasil = extractDeepProperty(o, subproperty, debug);
					if (hasil != null && !hasil.toString().trim().isEmpty()) {
						content = hasil;
						break;
					}
				} catch (Exception e) {
					if (debug)
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1363");
				}
			}
		} else if (StringUtils.contains(cleanProperty, ".")) {
			// Perbaikan Pengganti Logika 3-Level Hardcode yang Berantakan
			content = extractDeepProperty(o, cleanProperty, debug);
		} else {
			if (cleanProperty.equals("")) {
				content = safeToString(o);
			} else {
				content = readPropertyValueSafely(o, cleanProperty, metadata, debug);
				if (content == null && "id".equalsIgnoreCase(cleanProperty)) {
					content = getIdentifierSafely(metadata, o, debug);
				}
			}
		}

		if (content != null && content instanceof Date) {
			String temp = Common.dateFormat3.get().format(content);
			if (temp.endsWith("00:00:00")) {
				temp = Common.dateFormat1.get().format(content);
			}
			try {
				temp = StringUtil.replace(temp, "01-01-1970", "").trim();
			} catch (Exception e) {
				if (debug)
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1389");
			}

			content = temp;
		}

		if (jadikanText && content != null) {
			try {
				content = Jsoup.parse(content.toString()).text();
			} catch (Exception e) {
				if (debug)
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1400");
			}
		} else if (jadikanNumber && content != null) {
			try {
				content = Double.parseDouble(content.toString());
			} catch (Exception e) {
				if (debug)
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1407");
			}
		}

		return content;
	}

	// =====================================================================================
	// 4. PRIVATE STATIC HELPER: Dynamic N-Level Extractor (Pengganti if bersarang 3
	// level)
	// =====================================================================================
	private static Object extractDeepProperty(Object obj, String path, boolean debug) {
		if (obj == null || path == null || path.trim().isEmpty()) {
			return null;
		}

		String[] parts = StringUtils.split(path, ".");
		Object currentObj = obj;

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (currentObj == null || part == null || part.trim().isEmpty()) {
				return null;
			}

			currentObj = readPropertyValueSafely(currentObj, part.trim(), null, debug);
		}
		return currentObj;
	}

	@SuppressWarnings("deprecation")
	private static Object readPropertyValueSafely(Object obj, String property, ClassMetadata preferredMetadata,
			boolean debug) {
		if (obj == null || property == null || property.trim().isEmpty()) {
			return null;
		}

		String cleanProperty = property.trim();
		ClassMetadata meta = preferredMetadata;
		if (meta == null) {
			meta = getClassMetadataSafely(obj.getClass());
		}

		if (meta != null) {
			try {
				return meta.getPropertyValue(obj, cleanProperty, EntityMode.POJO);
			} catch (Throwable e) {
				// Hibernate 3.x kadang melempar NPE dari AbstractEntityTuplizer ketika object
				// adalah proxy/lazy relation/detached entity. Jangan biarkan export gagal hanya
				// karena satu kolom tidak bisa dibaca.
				logDebug(debug, e);
			}

			if ("id".equalsIgnoreCase(cleanProperty)) {
				Object identifier = getIdentifierSafely(meta, obj, debug);
				if (identifier != null) {
					return identifier;
				}
			}
		}

		Object result = readByGetterSafely(obj, cleanProperty, debug);
		if (result != null) {
			return result;
		}

		return readByFieldSafely(obj, cleanProperty, debug);
	}

	@SuppressWarnings("rawtypes")
	private static ClassMetadata getClassMetadataSafely(Class clazz) {
		if (clazz == null) {
			return null;
		}

		Class current = clazz;
		while (current != null && !Object.class.equals(current)) {
			try {
				ClassMetadata metadata = HibernateUtil.getClassMetadata(current);
				if (metadata != null) {
					return metadata;
				}
			} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1489");
			}
			current = current.getSuperclass();
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object readByGetterSafely(Object obj, String property, boolean debug) {
		if (obj == null || property == null || property.length() == 0) {
			return null;
		}

		String capitalized = property.substring(0, 1).toUpperCase() + property.substring(1);
		String[] getterNames = new String[] { "get" + capitalized, "is" + capitalized };
		Class clazz = obj.getClass();

		for (int i = 0; i < getterNames.length; i++) {
			try {
				java.lang.reflect.Method method = clazz.getMethod(getterNames[i], new Class[0]);
				method.setAccessible(true);
				return method.invoke(obj, new Object[0]);
			} catch (Throwable e) {
				logDebug(debug, e);
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static Object readByFieldSafely(Object obj, String property, boolean debug) {
		if (obj == null || property == null || property.length() == 0) {
			return null;
		}

		Class clazz = obj.getClass();
		while (clazz != null && !Object.class.equals(clazz)) {
			try {
				java.lang.reflect.Field field = clazz.getDeclaredField(property);
				field.setAccessible(true);
				return field.get(obj);
			} catch (Throwable e) {
				logDebug(debug, e);
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private static Object getIdentifierSafely(ClassMetadata metadata, Object obj, boolean debug) {
		if (metadata == null || obj == null) {
			return null;
		}
		try {
			return metadata.getIdentifier(obj, EntityMode.POJO);
		} catch (Throwable e) {
			logDebug(debug, e);
			return null;
		}
	}

	private static String safeToString(Object obj) {
		if (obj == null) {
			return "";
		}
		try {
			return obj.toString();
		} catch (Throwable e) {
			return "";
		}
	}

	private static int getSafeExportThreadCount(int totalData) {
		int processors = 1;
		try {
			processors = Runtime.getRuntime().availableProcessors();
		} catch (Throwable e) {
			processors = 1;
		}

		int count = processors <= 1 ? 1 : processors * 2;
		if (count > 8) {
			count = 8;
		}
		if (totalData > 0 && count > totalData) {
			count = totalData;
		}
		return count <= 0 ? 1 : count;
	}

	private static void flushAndClearQuietly(Session session, boolean debug) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.flush();
				session.clear();
			}
		} catch (Throwable e) {
			logDebug(debug, e);
		}
	}

	private static void closeOpenedSessionQuietly(Session session, boolean debug) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Throwable e) {
					logDebug(debug, e);
				}
				try {
					session.disconnect();
				} catch (Throwable e) {
					logDebug(debug, e);
				}
				try {
					session.close();
				} catch (Throwable e) {
					logDebug(debug, e);
				}
			}
		} catch (Throwable e) {
			logDebug(debug, e);
		}
	}

	private static void logDebug(boolean debug, Throwable e) {
		if (debug && e != null) {
			try {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonDownloadUpload.java:1624");
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonDownloadUpload.java:1625");
			}
		}
	}


	@SuppressWarnings("rawtypes")
	public static void appendDownloadButton(Component anchor, Class clazz, DataCriteria dataCriteria,
			String... columns) {
		if (anchor == null || anchor.getParent() == null || clazz == null || dataCriteria == null || columns == null
				|| columns.length == 0) {
			return;
		}

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(clazz, dataCriteria, columns);
		anchor.getParent().appendChild(cetakToolbarbutton);
	}

	@SuppressWarnings("rawtypes")
	public static void appendDownloadUploadButtons(Component anchor, Class clazz, DataCriteria dataCriteria,
			DataSearchDefault dataSearchDefault, boolean uploadVisible, String... columns) {
		appendDownloadButton(anchor, clazz, dataCriteria, columns);

		if (anchor == null || anchor.getParent() == null || clazz == null || dataSearchDefault == null || columns == null
				|| columns.length == 0) {
			return;
		}

		MyToolbarbuttonConfig upload = Common.uploadData(dataSearchDefault, clazz, columns);
		upload.setVisible(uploadVisible);
		anchor.getParent().appendChild(upload);
	}

}
