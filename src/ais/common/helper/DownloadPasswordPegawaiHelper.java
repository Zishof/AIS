package ais.common.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DownloadPasswordPegawaiHelper — unduh struk Username/Password Pegawai (bukan Dosen/Guru) di UTAS LATAR + BAR PROGRES persen</h1>
 *
 * <p><b>Untuk apa.</b> Admin membuatkan &amp; mengambil username/password akun untuk semua pegawai yang
 * BUKAN dosen/guru, lalu mengunduhnya sebagai berkas Excel (kolom: ID, Username, Password, Nama Lengkap,
 * Kode Install Mobile). Sebelumnya proses ini berjalan SINKRON di utas event ZK sehingga MEMBEKUKAN UI
 * (per baris ada penyimpanan akun + panggilan HTTP {@code curl} eksternal; dengan ~1300 pegawai bisa
 * sangat lama). Helper ini memindahkan seluruh pekerjaan ke SATU utas latar sambil menampilkan
 * {@link Progressmeter} dalam persen, lalu mengirim berkas ke browser saat selesai.</p>
 *
 * <p><b>Pola.</b> Meniru {@code DownloadFotoMassalHelper.prosesDownload}: utas latar mengerjakan
 * pembuatan akun + penyusunan Excel dan menaruh hasil di {@code Atomic*}; sebuah {@link Timer} (di utas
 * event) mem-poll kemajuan tiap 400ms untuk memperbarui bar persen; ketika selesai, Timer menutup
 * jendela lalu memanggil {@link Filedownload} (WAJIB di utas event). Karena hanya utas event yang
 * menyentuh komponen ZK, TIDAK diperlukan server-push.</p>
 *
 * <p><b>Sesi DB di utas latar (PENTING).</b> {@code Common.refreshSaveOrUpdate(o)} memakai
 * {@code HibernateUtil.currentSession()} yang transaksinya baru di-commit di AKHIR request (OpenSessionInView)
 * — TIDAK berlaku di utas latar. Maka penyimpanan akun di sini memakai {@code openSession()} + transaksi
 * EKSPLISIT (begin/commit) sendiri. {@code Common.saveOrUpdateUserAccess(...)} sudah membuka sesi &amp;
 * transaksi sendiri sehingga aman dipanggil apa adanya. Nilai yang bergantung pada HTTP request / entitas
 * (media Perguruan Tinggi, host, filename, dsb) DIHITUNG DI UTAS EVENT lalu dioper sebagai String final.</p>
 */
public final class DownloadPasswordPegawaiHelper {

	private DownloadPasswordPegawaiHelper() {
	}

	/**
	 * Jalankan pembuatan akun + unduhan struk password di utas latar dengan bar progres persen.
	 *
	 * @param pegawaiIds  id pegawai (bukan dosen/guru) hasil query di utas event (denominator progres).
	 * @param filename    path absolut berkas .xlsx tujuan (dihitung di utas event, mis. di bawah /tmp).
	 * @param strURL      endpoint API "ambil kode install".
	 * @param requestHost {@code Common.getRequestHostWithProtocol()} (harus dari utas event).
	 * @param link/namaPt/bgLoginPt/bgPt/logoPt/bannerPt/mottoPt/alamatPt/telpPt/emailPt payload PT (utas event).
	 */
	public static void proses(final List<Long> pegawaiIds, final String filename, final String strURL,
			final String link, final String namaPt, final String requestHost, final String bgLoginPt,
			final String bgPt, final String logoPt, final String bannerPt, final String mottoPt,
			final String alamatPt, final String telpPt, final String emailPt) throws Exception {

		if (pegawaiIds == null || pegawaiIds.isEmpty()) {
			MyMessageboxConfig.show("Tidak ada pegawai (bukan dosen/guru) yang dapat diproses.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final int total = pegawaiIds.size();
		final AtomicInteger diproses = new AtomicInteger(0);
		final AtomicInteger berhasil = new AtomicInteger(0);
		final AtomicReference<File> hasilFile = new AtomicReference<File>();
		final AtomicReference<String> pesanError = new AtomicReference<String>();
		final AtomicBoolean selesai = new AtomicBoolean(false);

		// ── Jendela progres (modal, tidak memblok utas event) ────────────────────────────────
		final org.zkoss.zk.ui.Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		final MyWindow winProgress = new MyWindow("Membuat & Mengunduh Password Pegawai", "normal", false);
		winProgress.setWidth("480px");
		winProgress.setClosable(false);
		winProgress.setParent(root);
		Vbox boxP = new Vbox();
		boxP.setWidth("100%");
		boxP.setStyle("padding:16px 18px;");
		boxP.setParent(winProgress);
		new Label("Memproses " + total + " pegawai (membuat akun & mengambil kode), harap tunggu...").setParent(boxP);
		final Progressmeter meter = new Progressmeter();
		meter.setValue(0);
		meter.setWidth("100%");
		meter.setParent(boxP);
		final Label lblPct = new Label("0%  (0 / " + total + ")");
		lblPct.setStyle("font-weight:800;color:#1e3a5f;font-size:14px;");
		lblPct.setParent(boxP);
		winProgress.doHighlighted();

		// ── Utas latar: buat akun + rakit Excel ──────────────────────────────────────────────
		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					hasilFile.set(bangunBerkas(pegawaiIds, filename, strURL, link, namaPt, requestHost, bgLoginPt,
							bgPt, logoPt, bannerPt, mottoPt, alamatPt, telpPt, emailPt, diproses, berhasil));
				} catch (Exception e) {
					pesanError.set(String.valueOf(e.getMessage()));
				} finally {
					selesai.set(true);
				}
			}
		});
		worker.setDaemon(true);
		worker.start();

		// ── Timer di utas event: perbarui persen; saat selesai → tutup + unduh ───────────────
		final Timer timer = new Timer(400);
		timer.setRepeats(true);
		timer.setParent(root);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int d = diproses.get();
				int pct = total > 0 ? (int) ((long) d * 100L / total) : 100;
				if (pct > 100) {
					pct = 100;
				}
				meter.setValue(pct);
				lblPct.setValue(pct + "%  (" + d + " / " + total + ")");
				if (selesai.get()) {
					try {
						timer.stop();
						timer.detach();
					} catch (Exception ex) {
						ais.common.ErrorAuditUtil.record(ex,
								"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:timer-stop");
					}
					try {
						winProgress.detach();
					} catch (Exception ex) {
						ais.common.ErrorAuditUtil.record(ex,
								"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:win-detach");
					}
					if (pesanError.get() != null) {
						PesanFormalHelper.tampilkanGagal("pembuatan & pengunduhan password pegawai",
								"Sistem gagal menyelesaikan pembuatan akun / penyusunan berkas. Keterangan teknis: \""
										+ pesanError.get() + "\".",
								new String[] { "Coba ulangi beberapa saat lagi.",
										"Bila jumlah pegawai sangat banyak, persempit filter data agar diproses bertahap." });
						return;
					}
					if (hasilFile.get() == null) {
						MyMessageboxConfig.show("Berkas password pegawai gagal disusun.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					Filedownload.save(new AMedia(hasilFile.get().getName(), "xlsx",
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", hasilFile.get(), true));
				}
			}
		});
		timer.start();
	}

	/**
	 * (UTAS LATAR) Untuk tiap pegawai: buat akun bila belum ada (sesi + transaksi eksplisit), ambil kode
	 * install via HTTP, lalu tulis satu baris Excel. {@code diproses} dinaikkan tiap pegawai (untuk persen),
	 * {@code berhasil} untuk baris yang benar-benar tertulis. Mengembalikan berkas .xlsx.
	 */
	@SuppressWarnings("deprecation")
	private static File bangunBerkas(final List<Long> pegawaiIds, final String filename, final String strURL,
			final String link, final String namaPt, final String requestHost, final String bgLoginPt,
			final String bgPt, final String logoPt, final String bannerPt, final String mottoPt,
			final String alamatPt, final String telpPt, final String emailPt, final AtomicInteger diproses,
			final AtomicInteger berhasil) throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("PEGAWAI");
		sheet.setDefaultColumnWidth(20);

		XSSFRow rowhead = sheet.createRow(0);
		rowhead.createCell(0).setCellValue("ID");
		rowhead.createCell(1).setCellValue("Username");
		rowhead.createCell(2).setCellValue("Password");
		rowhead.createCell(3).setCellValue("Nama Lengkap");
		rowhead.createCell(4).setCellValue("Kode Install Mobile");

		int rowIndex = 0;
		for (Long id : pegawaiIds) {
			Session s = null;
			Transaction tx = null;
			try {
				s = HibernateUtil.getSessionFactory().openSession();
				Pegawai pegawai = (Pegawai) s.get(Pegawai.class, id);
				if (pegawai == null || pegawai.getNama() == null || pegawai.getNama().trim().isEmpty()) {
					continue;
				}

				Tbmuser tbmuser = (Tbmuser) s.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

				String passwPlain = null;
				if (tbmuser == null || tbmuser.getUserId() == null) {
					tbmuser = new Tbmuser();
					String newUsername = (StringUtils.split(pegawai.getNama(), " ")[0] + ""
							+ RandomStringUtils.randomNumeric(3)).toLowerCase().trim();
					tbmuser.setUserId(newUsername);
					tbmuser.setEmail(pegawai.getEmail());
					tbmuser.setFakultas(pegawai.getFakultas());
					tbmuser.setIs_encripted(true);
					tbmuser.setJurusan(pegawai.getJurusan());
					tbmuser.setRoot(false);
					tbmuser.setUserNama(pegawai.getNama());
					passwPlain = RandomStringUtils.randomNumeric(5);
					tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passwPlain.trim()));
					tbmuser.setUserRole(ConstantValues.rolePegawai);
					tbmuser.setUserShow(1);
					tbmuser.setPegawai(pegawai);

					// Simpan akun dengan transaksi EKSPLISIT (currentSession()+OpenSessionInView tidak
					// berlaku di utas latar → tanpa ini akun TIDAK ter-commit).
					tx = s.beginTransaction();
					s.saveOrUpdate(tbmuser);
					tx.commit();
					tx = null;

					// Hak akses user — helper ini membuka sesi & transaksinya sendiri (aman di utas latar).
					Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(), passwPlain.trim(),
							tbmuser.getEmail());
				}

				String pwdTampil = "";
				try {
					pwdTampil = Common.desEncrypter.get().decrypt(tbmuser.getUserPassword());
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:decrypt");
				}

				String kodeInstall = ambilKodeInstall(tbmuser.getUserId(), requestHost, strURL, link, namaPt, bgLoginPt,
						bgPt, logoPt, bannerPt, mottoPt, alamatPt, telpPt, emailPt);

				rowIndex++;
				XSSFRow row = sheet.createRow(rowIndex);
				row.createCell(0).setCellValue(pegawai.getId());
				row.createCell(1).setCellValue(tbmuser.getUserId());
				row.createCell(2).setCellValue(pwdTampil);
				row.createCell(3).setCellValue(pegawai.getNama());
				row.createCell(4).setCellValue(kodeInstall);
				berhasil.incrementAndGet();

			} catch (Exception ePeg) {
				if (tx != null) {
					try {
						tx.rollback();
					} catch (Exception ig) {
						ais.common.ErrorAuditUtil.record(ig,
								"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:rollback");
					}
				}
				ais.common.ErrorAuditUtil.record(ePeg,
						"auto-audit src/ais/common/helper/DownloadPasswordPegawaiHelper.java:baris-pegawai " + id);
			} finally {
				if (s != null) {
					try {
						s.close();
					} catch (Exception ig) {
						ais.common.ErrorAuditUtil.record(ig,
								"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:sesi-close");
					}
				}
				diproses.incrementAndGet();
			}
		}

		File file = new File(filename);
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(file);
			workbook.write(fileOut);
		} finally {
			try {
				if (fileOut != null) {
					fileOut.close();
				}
			} catch (Exception ig) {
				ais.common.ErrorAuditUtil.record(ig,
						"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:xlsx-close");
			}
			try {
				workbook.close();
			} catch (Exception ig) {
				ais.common.ErrorAuditUtil.record(ig,
						"auto-audit(empty-catch) src/ais/common/helper/DownloadPasswordPegawaiHelper.java:wb-close");
			}
		}
		return file;
	}

	/**
	 * (UTAS LATAR) Panggil API eksternal via {@code curl} untuk mengambil "Kode Install Mobile" satu user.
	 * Meniru logika lama di {@code PegawaiAction} apa adanya; gagal dikembalikan sebagai string kosong.
	 */
	private static String ambilKodeInstall(String userId, String requestHost, String strURL, String link,
			String namaPt, String bgLoginPt, String bgPt, String logoPt, String bannerPt, String mottoPt,
			String alamatPt, String telpPt, String emailPt) {
		try {
			JSONObject postData = new JSONObject();
			postData.put("username", userId + ";" + requestHost);
			postData.put("link", link);
			postData.put("nama_pt", namaPt);
			postData.put("login_bg_pt", bgLoginPt);
			postData.put("bg_pt", bgPt);
			postData.put("logo_pt", logoPt);
			postData.put("banner_pt", bannerPt);
			postData.put("motto_pt", mottoPt);
			postData.put("alamat_pt", alamatPt);
			postData.put("telp_pt", telpPt);
			postData.put("email_pt", emailPt);
			postData.put("action", "code");

			String[] command = { "curl", "-d", postData.toString(), "-H", "Content-Type: application/json", strURL };
			ProcessBuilder process = new ProcessBuilder(command);
			Process p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String baris;
			while ((baris = reader.readLine()) != null) {
				builder.append(baris);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();
			JSONObject jsonObject = new JSONObject(hasil);
			return jsonObject.isNull("code") ? "" : jsonObject.get("code") + "";
		} catch (Exception e) {
			return "";
		}
	}
}
