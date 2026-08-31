package ais.action.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONExport;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk ekspor from feeder. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox ip}, {@code Textbox username},
 * {@code Textbox adminLain}, {@code Textbox password}, {@code Intbox port}, {@code Progressmeter progressmeter},
 * {@code Progressmeter progressmeterChild}, {@code Label labelProses}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onDownloadAplikasi32()}, {@code
 * onDownloadAplikasi()}, {@code onDownloadData()}); operasi domain lain ({@code display()}, {@code koneksi()},
 * {@code onLogin()}, {@code onAktifkan()}, {@code onLihatBantuan()}, {@code exists()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class EksporFromFeederAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4908026432590924291L;

	public static void display() {
		MyWindow myWindow = new MyWindow("Terhubung ke Feeder", "none", true);
		myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		myWindow.setHeight("80%");
		myWindow.setWidth("500px");
		myWindow.appendChild(new MyInclude("/pages/master/export_from_feeder.zul?hidden=true"));

		try {
			myWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private Textbox ip;
	private Textbox username;
	private Textbox adminLain;
	private Textbox password;
	private Intbox port;
	private Progressmeter progressmeter;
	private Progressmeter progressmeterChild;
	private Label labelProses;
	private Label labelProsesDetail;

	private MyLabelBolder onlineFeeder;

	private MyToolbarbuttonConfig button;
	private MyToolbarbuttonConfig downloadData;

	private Progressmeter progressmeterFile;
	private Progressmeter progressmeterChildFile;
	private Label labelProsesFile;

	private Timer timer;

	private MyCheckboxConfig bobot_nilai;
	private MyCheckboxConfig mata_kuliah;

	private Combobox fakultas_matkul;
	private Combobox jurusan_matkul;

	private MyCheckboxConfig kurikulum;
	private MyCheckboxConfig mata_kuliah_kurikulum;
	private Combobox fakultas_mata_kuliah_kurikulum;
	private Combobox jurusan_mata_kuliah_kurikulum;

	private MyCheckboxConfig kelas_kuliah;
	private MyCheckboxConfig mahasiswa;
	private MyCheckboxConfig mahasiswa_pt;
	private MyCheckboxConfig dosen_pt;
	private MyCheckboxConfig ajar_dosen;
	private MyCheckboxConfig krs;
	private MyCheckboxConfig nilai;
	private MyCheckboxConfig nilai_transfer;
	private MyCheckboxConfig kuliah_mahasiswa;

	public static String[] koneksi() {

		String postfix = "";
		if (Common.bolehKonfigurasi("masing_masing_pt_koneksi_langsung_ke_feeder", Konfigurasi.TIDAK_AKTIF)) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			postfix = perguruanTinggi == null ? "" : "_" + perguruanTinggi.getId();
		}

		Konfigurasi konfig = Common.getKonfigurasi("ip_koneksi_langsung_ke_feeder" + postfix, "10.0.0.0");
		String ip = konfig.getNilai();

		konfig = Common.getKonfigurasi("port_koneksi_langsung_ke_feeder" + postfix, "8082");
		String port = konfig.getNilai();

		konfig = Common.getKonfigurasi("username_koneksi_langsung_ke_feeder" + postfix, "");
		String username = konfig.getNilai();

		konfig = Common.getKonfigurasi("password_koneksi_langsung_ke_feeder" + postfix, "");
		String password = konfig.getNilai();

		boolean https = Common.bolehKonfigurasi("aktifkan_https_ke_feeder" + postfix, Konfigurasi.TIDAK_AKTIF);

		String url = (https ? "https" : "http") + "://" + ip + ":" + port + "/ws/live.php";

		return new String[] { ip, port, username, password, url };
	}

	/**
	 * Tombol "Coba Login Feeder". Seluruh hasil (berhasil/gagal koneksi/gagal login/kendala tak
	 * terduga) ditampilkan memakai {@link ais.action.master.feeder.util.NeoFeederPesanFormalHelper}
	 * — pesan formal, sopan, dan mudah dipahami yang SELALU memuat penyebab, langkah tindak lanjut,
	 * serta anjuran eskalasi ke Administrator/Pengembang Sistem (dengan tangkapan layar) bila
	 * langkah tersebut belum menyelesaikan masalah.
	 */
	public void onLogin(Event event) throws Exception {

		String[] kon = EksporFromFeederAction.koneksi();
		final String ip = kon[0];
		final String port = kon[1];
		final String username = kon[2];
		final String password = kon[3];
		final String url = kon[4];
		final boolean https = Common.bolehKonfigurasi("aktifkan_https_ke_feeder", Konfigurasi.TIDAK_AKTIF);

		if (!EksporFromFeederAction.exists(url)) {
			ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port, https,
					"Pemeriksaan ketersediaan alamat " + url + " gagal (server tidak merespons pada "
							+ "percobaan pemeriksaan jaringan).");
			return;
		}

		FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), null);

		try {
			List<String> warnings = new ArrayList<String>();
			String token = feederConnector.getToken(username, password, warnings);
			System.out.println("TOKEN => " + token);

			if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
				String detailTeknis = warnings.isEmpty() ? null : warnings.get(0);
				ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalLogin(username, detailTeknis);
				return;
			}

			ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanSukses(
					"Uji Koneksi dan Login ke Neo Feeder",
					"Sistem berhasil terhubung ke server Neo Feeder pada alamat \"" + ip + "\" port \"" + port
							+ "\" dan berhasil masuk (login) dengan nama pengguna \"" + username + "\".");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalUmum(
					"Uji Koneksi dan Login ke Neo Feeder", e.getMessage(),
					"Periksa kembali Alamat IP, Port, Username, dan Password Feeder pada Pengaturan Koneksi.");
		}
	}

	private Checkbox aktifkan_koneksi_langsung_ke_feeder;

	private Checkbox aktifkan_https_ke_feeder;

	public void onAktifkan(Event event) {

		String postfix = "";
		if (Common.bolehKonfigurasi("masing_masing_pt_koneksi_langsung_ke_feeder", Konfigurasi.TIDAK_AKTIF)) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			postfix = perguruanTinggi == null ? "" : "_" + perguruanTinggi.getId();
		}

		Konfigurasi konfig = Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder" + postfix,
				Konfigurasi.AKTIF);
		konfig.setNilai(aktifkan_koneksi_langsung_ke_feeder.isChecked() ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
		MemoryDbUtil.getKonfigurasi().put("aktifkan_terhubung_langsung_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("aktifkan_https_ke_feeder" + postfix, Konfigurasi.TIDAK_AKTIF);
		konfig.setNilai(aktifkan_https_ke_feeder.isChecked() ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
		MemoryDbUtil.getKonfigurasi().put("aktifkan_https_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("ip_koneksi_langsung_ke_feeder" + postfix, "10.0.0.0");
		konfig.setNilai(ip.getValue());
		MemoryDbUtil.getKonfigurasi().put("ip_koneksi_langsung_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("port_koneksi_langsung_ke_feeder" + postfix, "8082");
		konfig.setNilai(port.getValue() == null ? "8082" : port.getValue().toString());
		MemoryDbUtil.getKonfigurasi().put("port_koneksi_langsung_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("username_koneksi_langsung_ke_feeder" + postfix, "");
		konfig.setNilai(username.getValue());
		MemoryDbUtil.getKonfigurasi().put("username_koneksi_langsung_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("password_koneksi_langsung_ke_feeder" + postfix, "");
		konfig.setNilai(password.getValue());
		MemoryDbUtil.getKonfigurasi().put("password_koneksi_langsung_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		konfig = Common.getKonfigurasi("admin_yg_boleh_kirim_ke_feeder" + postfix, "");
		konfig.setNilai(adminLain.getValue().trim());
		MemoryDbUtil.getKonfigurasi().put("admin_yg_boleh_kirim_ke_feeder" + postfix, konfig);
		Common.refreshUpdate(konfig);

		boolean admin = Common.getApakahAdmin();

		adminLain.setDisabled(!admin);
		aktifkan_koneksi_langsung_ke_feeder.setDisabled(!admin);
		ip.setDisabled(!admin);
		port.setDisabled(!admin);
		username.setDisabled(!admin);
		password.setDisabled(!admin);
	}

	private Intbox ta_kurikulum;
	private Combobox fakultas_kurikulum;
	private Combobox jurusan_kurikulum;

	private Intbox ta_mahasiswa;
	private Combobox fakultas_mahasiswa;
	private Combobox jurusan_mahasiswa;

	private Intbox ta_mahasiswa_pt;
	private Combobox fakultas_mahasiswa_pt;
	private Combobox jurusan_mahasiswa_pt;

	private Intbox ta_dosen_pt;
	private Combobox fakultas_dosen_pt;
	private Combobox jurusan_dosen_pt;

	private Intbox ta_kelas_kuliah;
	private Combobox fakultas_kelas_kuliah;
	private Combobox jurusan_kelas_kuliah;

	private Intbox ta_ajar_dosen;
	private Combobox fakultas_ajar_dosen;
	private Combobox jurusan_ajar_dosen;

	private Intbox ta_krs;
	private Combobox fakultas_krs;
	private Combobox jurusan_krs;
	private Textbox nama_krs;

	private Intbox ta_nilai;
	private Combobox fakultas_nilai;
	private Combobox jurusan_nilai;
	private Textbox nama_nilai;

	private Intbox ta_nilai_transfer;
	private Combobox fakultas_nilai_transfer;
	private Combobox jurusan_nilai_transfer;
	private Textbox nama_nilai_transfer;

	private Intbox semester_kuliah_mahasiswa;
	private Intbox ta_kuliah_mahasiswa;
	private Combobox fakultas_kuliah_mahasiswa;
	private Combobox jurusan_kuliah_mahasiswa;

	private Textbox matkul;

	public void onLihatBantuan(Event event) throws Exception {
		Common.previewFile("/help/Kunci_Keberhasil_Export_Ke_Feeder.pdf");
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		String postfix = "";
		if (Common.bolehKonfigurasi("masing_masing_pt_koneksi_langsung_ke_feeder", Konfigurasi.TIDAK_AKTIF)) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			postfix = perguruanTinggi == null ? "" : "_" + perguruanTinggi.getId();
		}

		Konfigurasi konfig = Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder" + postfix,
				Konfigurasi.AKTIF);
		if (aktifkan_koneksi_langsung_ke_feeder != null) { aktifkan_koneksi_langsung_ke_feeder.setChecked(konfig.getNilai().equals(Konfigurasi.AKTIF)); }

		konfig = Common.getKonfigurasi("aktifkan_https_ke_feeder" + postfix, Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_https_ke_feeder != null) { aktifkan_https_ke_feeder.setChecked(konfig.getNilai().equals(Konfigurasi.AKTIF)); }

		konfig = Common.getKonfigurasi("ip_koneksi_langsung_ke_feeder" + postfix, "10.0.0.0");
		if (ip != null) { ip.setValue(konfig.getNilai()); }

		konfig = Common.getKonfigurasi("port_koneksi_langsung_ke_feeder" + postfix, "8082");
		try {
			port.setValue(Integer.parseInt(konfig.getNilai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/EksporFromFeederAction.java:318");
			// TODO: handle exception
		}

		konfig = Common.getKonfigurasi("username_koneksi_langsung_ke_feeder" + postfix, "");
		if (username != null) { username.setValue(konfig.getNilai()); }

		konfig = Common.getKonfigurasi("password_koneksi_langsung_ke_feeder" + postfix, "");
		if (password != null) { password.setValue(konfig.getNilai()); }

		konfig = Common.getKonfigurasi("admin_yg_boleh_kirim_ke_feeder" + postfix, "");
		if (adminLain != null) { adminLain.setValue(konfig.getNilai()); }

		if (onlineFeeder != null) { onlineFeeder.setValue("URL untuk ekspor feeder di FEEDER_CLIENT : " + Common.getRequestHostWithProtocol()); }

		if (execution.getParameter("hidden") != null) {
			onlineFeeder.setVisible(false);
		}

		Common.initFakultasDanJurusan(fakultas_matkul, jurusan_matkul, fakultas_dosen_pt, jurusan_dosen_pt);

		Common.initFakultasDanJurusan(fakultas_mata_kuliah_kurikulum, jurusan_mata_kuliah_kurikulum, fakultas_kurikulum,
				jurusan_kurikulum);

		Common.initFakultasDanJurusan(fakultas_mahasiswa, jurusan_mahasiswa, fakultas_mahasiswa_pt,
				jurusan_mahasiswa_pt);

		Common.initFakultasDanJurusan(fakultas_kelas_kuliah, jurusan_kelas_kuliah, fakultas_ajar_dosen,
				jurusan_ajar_dosen);

		Common.initFakultasDanJurusan(fakultas_nilai, jurusan_nilai, fakultas_kuliah_mahasiswa,
				jurusan_kuliah_mahasiswa);

		Common.initFakultasDanJurusan(fakultas_krs, jurusan_krs, fakultas_nilai_transfer, jurusan_nilai_transfer);
	}

	public void onDownloadAplikasi32(Event event) throws Exception {
		execution.sendRedirect("http://ecampus.id/FEEDER.zip");
	}

	public void onDownloadAplikasi(Event event) throws Exception {
		execution.sendRedirect("http://ecampus.id/FEEDER_64.zip");
	}

	public static boolean exists(String URLName) {
		try {

			String postfix = "";
			if (Common.bolehKonfigurasi("masing_masing_pt_koneksi_langsung_ke_feeder", Konfigurasi.TIDAK_AKTIF)) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				postfix = perguruanTinggi == null ? "" : "_" + perguruanTinggi.getId();
			}

			Konfigurasi konfig = Common.getKonfigurasi("ip_koneksi_langsung_ke_feeder" + postfix, "10.0.0.0");
			String ip = konfig.getNilai();
			if (ip.equalsIgnoreCase("10.0.0.0")) {
				EksporFromFeederAction.display();
				return false;
			}

			System.out.println("Coba akses URL = " + URLName);
//			HttpURLConnection.setFollowRedirects(false);
//			// note : you may also need
//			// HttpURLConnection.setInstanceFollowRedirects(false)
//			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
//			if (con instanceof HttpsURLConnection) {
//				((HttpsURLConnection) con).setHostnameVerifier(new HostnameVerifier() {
//
//					@Override
//					public boolean verify(String hostname, SSLSession session) {
//						return true;
//					}
//				});
//			}
//			con.setRequestMethod("HEAD");
//			con.setReadTimeout(5000);
//			int rest = con.getResponseCode();

			String[] command = { "curl", "-o", "/dev/null", "-s", "-w", "\"%{http_code}\\n\"", URLName };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			String cleanText = hasil.replaceAll("\\p{Punct}", "");

			int rest = Integer.parseInt(cleanText.trim());
			System.out.println("URL = " + URLName + ", respond " + rest);
			return (rest == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			EksporFromFeederAction.display();
			ais.common.Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	public void onImport(Event event) {

		final Progressmeter myProgressmeter = new Progressmeter();
		final Progressmeter myProgressmeterChild = new Progressmeter();
		final Label myLabelProses = new Label();
		final Label myLabelProsesDetail = new Label();
		final String ip = this.ip.getValue().trim();

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				FeederConnector feederConnector;
				try {

					String url = "http://" + ip + ":" + (port.getValue() == null ? 8082 : port.getValue())
							+ "/ws/live.php";

					myLabelProses.setValue("Mencoba mengakses " + url);

					if (!exists(url)) {
						myProgressmeter.setValue(100);
						myLabelProses.setValue(ais.action.master.feeder.util.NeoFeederPesanFormalHelper
								.pesanGagalKoneksi(ip, String.valueOf(port.getValue() == null ? 8082 : port.getValue()),
										Common.bolehKonfigurasi("aktifkan_https_ke_feeder", Konfigurasi.TIDAK_AKTIF),
										"Pemeriksaan ketersediaan alamat " + url + " gagal."));
						return;
					}

					feederConnector = new FeederConnector(ip, port.getValue() == null ? 8082 : port.getValue(),
							myLabelProsesDetail);

					String username = EksporFromFeederAction.this.username.getValue().trim();
					String password = EksporFromFeederAction.this.password.getValue().trim();

					List<String> warnings = new ArrayList<String>();
					String token = feederConnector.getToken(username, password, warnings);
					System.out.println("TOKEN => " + token);

					if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
						myProgressmeter.setValue(100);
						myLabelProses.setValue(ais.action.master.feeder.util.NeoFeederPesanFormalHelper
								.pesanGagalLogin(username, warnings.isEmpty() ? null : warnings.get(0)));
						return;
					}

					System.out.println("TABLES => " + feederConnector.listTable(token));

					FeederExporter feederImporter = new FeederExporter(feederConnector, token, myProgressmeter,
							myProgressmeterChild, myLabelProses);
					feederImporter.doEksport();
				} catch (Exception e) {
					myLabelProses.setValue(ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalUmum(
							"Impor/Sinkronisasi Data dari Neo Feeder", e.getMessage(),
							"Periksa kembali Alamat IP, Port, Username, dan Password Feeder pada Pengaturan Koneksi."));
					Common.tampilErrorJikaAdmin(e);
				}
				myProgressmeter.setValue(100);
			}
		};

		new Thread(runnable).start();

		timer = new Timer(1000);
		page.getFirstRoot().appendChild(timer);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				System.out.println("======================================================");
				progressmeter.setValue(myProgressmeter.getValue());
				progressmeterChild.setValue(myProgressmeterChild.getValue());
				labelProses.setValue(myLabelProses.getValue());
				labelProsesDetail.setValue(myLabelProsesDetail.getValue());

				button.setDisabled(true);
				if (myProgressmeter.getValue() == 100) {
					MyMessageboxConfig.show(myLabelProses.getValue(), "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);

					button.setDisabled(false);
					timer.detach();

				}

			}
		});
		timer.setRepeats(true);
		timer.start();

	}

	public void onDownloadData(Event event) throws Exception {

		final Set<String> tables = new HashSet<String>();
		if (bobot_nilai.isChecked()) {
			tables.add("bobot_nilai");
		}
		if (mata_kuliah.isChecked()) {
			tables.add("mata_kuliah");
		}
		if (kurikulum.isChecked()) {
			tables.add("kurikulum");
		}
		if (mata_kuliah_kurikulum.isChecked()) {
			tables.add("mata_kuliah_kurikulum");
		}
		if (kelas_kuliah.isChecked()) {
			tables.add("kelas_kuliah");
		}
		if (mahasiswa.isChecked()) {
			tables.add("mahasiswa");
		}
		if (mahasiswa_pt.isChecked()) {
			tables.add("mahasiswa_pt");
		}
		if (dosen_pt.isChecked()) {
			tables.add("dosen_pt");
		}
		if (ajar_dosen.isChecked()) {
			tables.add("ajar_dosen");
		}
		if (krs.isChecked()) {
			tables.add("krs");
		}
		if (nilai.isChecked()) {
			tables.add("nilai");
		}
		if (nilai_transfer.isChecked()) {
			tables.add("nilai_transfer");
		}
		if (kuliah_mahasiswa.isChecked()) {
			tables.add("kuliah_mahasiswa");
		}

		if (tables.isEmpty()) {
			MyMessageboxConfig.show("Pilihlah minimal satu tabel", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		final Progressmeter myProgressmeter = new Progressmeter();
		final Progressmeter myProgressmeterChild = new Progressmeter();
		final Label myLabelProses = new Label();

		File file = null;
		try {
			file = new File(Sessions.getCurrent().getWebApp().getRealPath(
					"/temp/hasil_export_ke_feeder_" + URLEncoder.encode(tables.toString().replaceAll("\\[", "")
							.replaceAll("\\]", "").replaceAll(" ", "").replaceAll(",", "_"), "UTF-8") + ".json"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		final File f = file;

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				FeederJSONExport feederJSONExport = new FeederJSONExport(f, tables, myProgressmeter,
						myProgressmeterChild, myLabelProses);

				feederJSONExport.fakultas_matkul = (Fakultas) (fakultas_matkul.getSelectedItem() == null ? null
						: fakultas_matkul.getSelectedItem().getValue());
				feederJSONExport.jurusan_matkul = (Jurusan) (jurusan_matkul.getSelectedItem() == null ? null
						: jurusan_matkul.getSelectedItem().getValue());

				feederJSONExport.fakultas_mata_kuliah_kurikulum = (Fakultas) (fakultas_mata_kuliah_kurikulum
						.getSelectedItem() == null ? null
								: fakultas_mata_kuliah_kurikulum.getSelectedItem().getValue());
				feederJSONExport.jurusan_mata_kuliah_kurikulum = (Jurusan) (jurusan_mata_kuliah_kurikulum
						.getSelectedItem() == null ? null : jurusan_mata_kuliah_kurikulum.getSelectedItem().getValue());

				feederJSONExport.ta_mahasiswa = ta_mahasiswa.getValue();
				feederJSONExport.fakultas_mahasiswa = (Fakultas) (fakultas_mahasiswa.getSelectedItem() == null ? null
						: fakultas_mahasiswa.getSelectedItem().getValue());
				feederJSONExport.jurusan_mahasiswa = (Jurusan) (jurusan_mahasiswa.getSelectedItem() == null ? null
						: jurusan_mahasiswa.getSelectedItem().getValue());

				feederJSONExport.ta_kurikulum = ta_kurikulum.getValue();
				feederJSONExport.fakultas_kurikulum = (Fakultas) (fakultas_kurikulum.getSelectedItem() == null ? null
						: fakultas_kurikulum.getSelectedItem().getValue());
				feederJSONExport.jurusan_kurikulum = (Jurusan) (jurusan_kurikulum.getSelectedItem() == null ? null
						: jurusan_kurikulum.getSelectedItem().getValue());

				feederJSONExport.ta_mahasiswa_pt = ta_mahasiswa_pt.getValue();
				feederJSONExport.fakultas_mahasiswa_pt = (Fakultas) (fakultas_mahasiswa_pt.getSelectedItem() == null
						? null
						: fakultas_mahasiswa_pt.getSelectedItem().getValue());
				feederJSONExport.jurusan_mahasiswa_pt = (Jurusan) (jurusan_mahasiswa_pt.getSelectedItem() == null ? null
						: jurusan_mahasiswa_pt.getSelectedItem().getValue());

				feederJSONExport.ta_dosen_pt = ta_dosen_pt.getValue();
				feederJSONExport.fakultas_dosen_pt = (Fakultas) (fakultas_dosen_pt.getSelectedItem() == null ? null
						: fakultas_dosen_pt.getSelectedItem().getValue());
				feederJSONExport.jurusan_dosen_pt = (Jurusan) (jurusan_dosen_pt.getSelectedItem() == null ? null
						: jurusan_dosen_pt.getSelectedItem().getValue());

				feederJSONExport.ta_kelas_kuliah = ta_kelas_kuliah.getValue();
				feederJSONExport.fakultas_kelas_kuliah = (Fakultas) (fakultas_kelas_kuliah.getSelectedItem() == null
						? null
						: fakultas_kelas_kuliah.getSelectedItem().getValue());
				feederJSONExport.jurusan_kelas_kuliah = (Jurusan) (jurusan_kelas_kuliah.getSelectedItem() == null ? null
						: jurusan_kelas_kuliah.getSelectedItem().getValue());

				feederJSONExport.ta_ajar_dosen = ta_ajar_dosen.getValue();
				feederJSONExport.fakultas_ajar_dosen = (Fakultas) (fakultas_ajar_dosen.getSelectedItem() == null ? null
						: fakultas_ajar_dosen.getSelectedItem().getValue());
				feederJSONExport.jurusan_ajar_dosen = (Jurusan) (jurusan_ajar_dosen.getSelectedItem() == null ? null
						: jurusan_ajar_dosen.getSelectedItem().getValue());

				feederJSONExport.ta_nilai = ta_nilai.getValue();
				feederJSONExport.fakultas_nilai = (Fakultas) (fakultas_nilai.getSelectedItem() == null ? null
						: fakultas_nilai.getSelectedItem().getValue());
				feederJSONExport.jurusan_nilai = (Jurusan) (jurusan_nilai.getSelectedItem() == null ? null
						: jurusan_nilai.getSelectedItem().getValue());
				feederJSONExport.nama_nilai = nama_nilai.getValue().trim();

				feederJSONExport.ta_nilai_transfer = ta_nilai_transfer.getValue();
				feederJSONExport.fakultas_nilai_transfer = (Fakultas) (fakultas_nilai_transfer.getSelectedItem() == null
						? null
						: fakultas_nilai_transfer.getSelectedItem().getValue());
				feederJSONExport.jurusan_nilai_transfer = (Jurusan) (jurusan_nilai_transfer.getSelectedItem() == null
						? null
						: jurusan_nilai_transfer.getSelectedItem().getValue());
				feederJSONExport.nama_nilai_transfer = nama_nilai_transfer.getValue().trim();

				feederJSONExport.ta_krs = ta_krs.getValue();
				feederJSONExport.fakultas_krs = (Fakultas) (fakultas_krs.getSelectedItem() == null ? null
						: fakultas_krs.getSelectedItem().getValue());
				feederJSONExport.jurusan_krs = (Jurusan) (jurusan_krs.getSelectedItem() == null ? null
						: jurusan_krs.getSelectedItem().getValue());
				feederJSONExport.nama_krs = nama_krs.getValue().trim();

				feederJSONExport.semester_kuliah_mahasiswa = semester_kuliah_mahasiswa.getValue();
				feederJSONExport.ta_kuliah_mahasiswa = ta_kuliah_mahasiswa.getValue();
				feederJSONExport.fakultas_kuliah_mahasiswa = (Fakultas) (fakultas_kuliah_mahasiswa
						.getSelectedItem() == null ? null : fakultas_kuliah_mahasiswa.getSelectedItem().getValue());
				feederJSONExport.jurusan_kuliah_mahasiswa = (Jurusan) (jurusan_kuliah_mahasiswa
						.getSelectedItem() == null ? null : jurusan_kuliah_mahasiswa.getSelectedItem().getValue());
				feederJSONExport.matkul = matkul.getValue().trim();

				try {
					feederJSONExport.proses();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				myProgressmeter.setValue(100);
			}
		};

		new Thread(runnable).start();

		timer = new Timer(1000);
		page.getFirstRoot().appendChild(timer);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				System.out.println("======================================================");
				progressmeterFile.setValue(myProgressmeter.getValue());
				progressmeterChildFile.setValue(myProgressmeterChild.getValue());
				labelProsesFile.setValue(myLabelProses.getValue());

				downloadData.setDisabled(true);
				if (myProgressmeter.getValue() == 100) {
					MyMessageboxConfig.show("Proses telah selesai, klik OK untuk download file", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Filedownload.save(f, "application/json");
								}
							});

					downloadData.setDisabled(false);
					timer.detach();

				}

			}
		});
		timer.setRepeats(true);
		timer.start();

	}

}
