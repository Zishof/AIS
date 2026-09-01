package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

/**
 * Tipe khusus untuk upload kelas. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code File file};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); validasi/perhitungan ({@code
 * checkPerkuliahan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class UploadKelas extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private File file;

	public UploadKelas() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadKelas(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_kelas_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload Kelas" + Common.ukuranLabelFileUpload(),
				"/img/upload.png");
		button.setParent(toolbar);
		button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {
			
			
			

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = media.getStreamData();
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();
					initSpreadsheet(file);

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});

		if (Common.getApakahAdmin()) {
			final MyToolbarbuttonConfig tidakAktifkan = new MyToolbarbuttonConfig("Tidak Aktifkan Upload",
					"/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig aktifkan = new MyToolbarbuttonConfig("Aktifkan Upload",
					"/img/svg/check2-circle.svg");
			aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
			tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));

			aktifkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfigurasi.setNilai(Konfigurasi.AKTIF);
					Common.refreshUpdate(konfigurasi);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
					button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
				}
			});
			aktifkan.setParent(toolbar);

			tidakAktifkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfigurasi.setNilai(Konfigurasi.TIDAK_AKTIF);
					Common.refreshUpdate(konfigurasi);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
					button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
				}
			});
			tidakAktifkan.setParent(toolbar);
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "KELAS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelas.java:185");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({})
	private void initSpreadsheet(final File fileUpload) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();

		Common.clear(center);

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final String[] ringkasan = new String[] { "" };
		final Label downloadPath = new Label("");

		final ais.action.master.feeder.integrator.ekspor.SaringanFeeder saringan = new ais.action.master.feeder.integrator.ekspor.SaringanFeeder();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					// Pembacaan berkas dan penyimpanannya milik ImporKelasFeeder;
					// layar ini hanya menyediakan berkas beserta saringannya lalu
					// menampilkan kemajuan dan laporannya. Menyalin aturan
					// penyimpanannya ke sini akan membuat dua aturan yang harus
					// dijaga sama -- dan yang berbeda hasilnya adalah isi basis data.
					ais.action.master.feeder.integrator.impor.HasilImpor hasil = ais.action.master.feeder.integrator.impor.ImporKelasFeeder.proses(
							fileUpload, file, saringan, tbmuser,
							new ais.common.newui.pekerjaan.PekerjaanRegistry.Progres() {
								@Override
								public void lapor(int persen, String pesan) {
									label.setValue(pesan + " (" + persen + " %)");
								}
							});
					sizedata.setValue(hasil.baris + 1);
					ringkasan[0] = hasil.ringkasan;
					if (hasil.laporan != null) downloadPath.setValue(hasil.laporan.getAbsolutePath());
					label.setValue("");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pemrosesan berkas Kelas yang diunggah", null, e,
							new String[] {
									"Periksa kembali format berkas Excel yang diunggah lalu ulangi.",
									"Pastikan data acuan yang disebut berkas sudah ada.",
									"Jika kendala berulang, hubungi Administrator Sistem." })
							.replace("\n", " "));
				} finally {
					/* currentNativeSession() wajib ditutup tepat sekali dan ThreadLocal dibersihkan. */
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();


		final Timer timerReport = new Timer(500);
		timerReport.setParent(UploadKelas.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(ringkasan[0], "Laporan Upload Kelas", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}

}
