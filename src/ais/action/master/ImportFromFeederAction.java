package ais.action.master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederImporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ImportFromFeederAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4908026432590924291L;

	private Textbox ip;
	private Textbox username;
	private Textbox password;
	private Intbox port;
	private Progressmeter progressmeter;
	private Progressmeter progressmeterChild;
	private Label labelProses;
	private Label labelProsesDetail;
	private Textbox usernameFeeder;
	private MyToolbarbuttonConfig button;

	private MyToolbarbuttonConfig uploadData;
	private MyToolbarbuttonConfig uploadDataUlang;

	private Progressmeter progressmeterFile;
	private Progressmeter progressmeterChildFile;
	private Label labelProsesFile;

	private Timer timer;

	protected File file;
	private Hbox pilihan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		final Konfigurasi konfigurasi = Common.getKonfigurasi("username_feeder", "");
		if (usernameFeeder != null) { usernameFeeder.setValue(konfigurasi.getNilai()); }
		usernameFeeder.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				konfigurasi.setNilai(usernameFeeder.getValue().trim());
				Common.refreshUpdate(konfigurasi);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
			}
		});

		String tbl = "agama,ikatan_kerja_dosen,bobot_nilai,jabfung,jenis_evaluasi,jenis_keluar,jenis_pendaftaran,jenjang_pendidikan,lembaga_pengangkat,negara,"
				+ "pangkat_golongan,pekerjaan,penghasilan,status_keaktifan_pegawai,status_kepegawaian,status_mahasiswa,wilayah,dosen,dosen_pt,ajar_dosen,"
				+ "kurikulum,mahasiswa,mahasiswa_pt,mata_kuliah,mata_kuliah_kurikulum,kelas_kuliah,nilai,nilai_transfer";
		final MyCheckboxConfig checkboxSemua = new MyCheckboxConfig("Semua Tabel");
		if (checkboxSemua != null) { checkboxSemua.setValue("semua tabel"); }
		if (checkboxSemua != null) { checkboxSemua.setChecked(true); }
		pilihan.appendChild(checkboxSemua);

		int i = 0;
		Vbox vbox = null;
		for (String tb : tbl.split(",")) {
			if (i % 10 == 0) {
				vbox = new Vbox();
				vbox.setParent(pilihan);
			}
			MyCheckboxConfig checkbox = new MyCheckboxConfig("Tabel " + tb);
			checkbox.setValue(tb);
			checkbox.setChecked(true);
			vbox.appendChild(checkbox);
			i++;
		}

		checkboxSemua.addEventListener("onClick", new EventListener() {

			@Override
			@SuppressWarnings("unchecked")
			public void onEvent(Event arg0) throws Exception {
				List<Component> vboxs = pilihan.getChildren();
				for (Component vbox : vboxs) {
					if (vbox instanceof Vbox) {
						List<MyCheckboxConfig> checkboxs = vbox.getChildren();
						for (MyCheckboxConfig checkbox : checkboxs) {
							if (checkbox != checkboxSemua) {
								checkbox.setChecked(checkboxSemua.isChecked());
							}
						}
					}
				}
			}
		});

		Konfigurasi konfigurasiFile = Common.getKonfigurasi("lokasi_penyimpanan_upload_feeder", "");
		if (!konfigurasiFile.getNilai().trim().isEmpty()) {
			final File file = new File(konfigurasiFile.getNilai().trim());
			uploadDataUlang.setVisible(file.exists());
		}
	}

	public void onUploadDataUlang(Event event) throws Exception {
		Konfigurasi konfigurasiFile = Common.getKonfigurasi("lokasi_penyimpanan_upload_feeder", "");
		if (!konfigurasiFile.getNilai().trim().isEmpty()) {
			final File file = new File(konfigurasiFile.getNilai().trim());
			if (file != null && file.exists()) {
				doUpload(file);
			}
		}
	}

	public void onDownloadAplikasi32(Event event) throws Exception {
		execution.sendRedirect("http://ecampus.id/FEEDER.zip");
	}

	public void onDownloadAplikasi(Event event) throws Exception {
		execution.sendRedirect("http://ecampus.id/FEEDER_64.zip");
	}

	@SuppressWarnings("unchecked")
	private void doUpload(final File file) {
		final Progressmeter myProgressmeter = new Progressmeter();
		final Progressmeter myProgressmeterChild = new Progressmeter();
		final Label myLabelProses = new Label();
		final String username = usernameFeeder.getValue();
		final List<String> tables = new ArrayList<String>();
		List<Component> vboxs = pilihan.getChildren();
		for (Component vbox : vboxs) {
			if (vbox instanceof Vbox) {
				List<MyCheckboxConfig> checkboxs = vbox.getChildren();
				for (MyCheckboxConfig checkbox : checkboxs) {
					if (checkbox.isChecked()) {
						tables.add(checkbox.getValue().toString());
					}
				}
			}
		}

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				try {

					new FeederJSONImport(file, myProgressmeter, myProgressmeterChild, myLabelProses, username, tables)
							.proses();
					myLabelProses.setValue("Upload data feeder berhasil dilakukan ");
				} catch (Exception e) {
					myLabelProses.setValue("Terjadi kesalahan " + e.getMessage());
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

				System.out.println(myProgressmeterChild.getValue() + "% " + myLabelProses.getValue()
						+ " ======================================================");
				progressmeterFile.setValue(myProgressmeter.getValue());
				progressmeterChildFile.setValue(myProgressmeterChild.getValue());
				labelProsesFile.setValue(myLabelProses.getValue());

				uploadData.setDisabled(true);
				if (myProgressmeter.getValue() == 100) {
					uploadData.setDisabled(false);
					MyMessageboxConfig.show(myLabelProses.getValue(), "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									int count = ((Number) HibernateUtil.currentSession().createCriteria(Jurusan.class)
											.add(Restrictions.or(Restrictions.isNull("feeder"),
													Restrictions.eq("feeder", "")))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();

									if (count > 0) {
										MyMessageboxConfig.show("Peringatan: Terdapat " + count
												+ " data program studi yang belum masuk data feeder-nya.\nHarap melengkapi Kode PDPT di menu program studi dan sesuaikan dengan \"Kode Program Studi\" di feeder Anda !.\n\nKemudian lakukan upload ulang.",
												"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							});

					timer.detach();

				}

			}
		});
		timer.setRepeats(true);
		timer.start();
	}

	public void onUploadData(Event event) throws Exception {
		ForwardEvent forwardEvent = (ForwardEvent) event;
		final Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (usernameFeeder.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Masukkan username feeder dengan benar", "Error", MyMessageboxConfig.OK,
							MyMessageboxConfig.ERROR);
					usernameFeeder.focus();
					return;
				}

				if (media.getName().toLowerCase().endsWith("json") || media.getName().toLowerCase().endsWith("zip")) {

					File tempfile;
					tempfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);

					tempfile.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(tempfile);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();
					if (media.getName().toLowerCase().endsWith("zip")) {
						List<File> files = Common.extractZip(tempfile, tempfile.getParentFile().getAbsolutePath());
						for (final File file : files) {
							if (file.getName().toLowerCase().endsWith("json")) {
								tempfile = file;
								break;
							}
						}
					}

					File file = tempfile;

					File folder = CommonMedia.getMediaDirectory();

					File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
							ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + file.getName(), "UTF-8"));

					// System.out.println("file = " + tempfile.getAbsolutePath() + ", lagi " +
					// f.getAbsolutePath());

					FileUtils.copyFile(file, f);
					uploadDataUlang.setVisible(file.exists());
					Konfigurasi konfigurasi = Common.getKonfigurasi("lokasi_penyimpanan_upload_feeder",
							f.getAbsolutePath());
					konfigurasi.setNilai(f.getAbsolutePath());
					Common.refreshUpdate(konfigurasi);

					if (file.getName().toLowerCase().endsWith("json")) {

						doUpload(file);

					} else {
						MyMessageboxConfig.show("File yang anda upload harus ber-format *.json" + media, "Error",
								MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				} else {
					MyMessageboxConfig.show("File yang anda upload harus ber-format *.json atau *.zip" + media, "Error",
							MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});

	}

	public static boolean exists(String URLName) {
		try {

			System.out.println("Coba akses URL = " + URLName);
			HttpURLConnection.setFollowRedirects(false);
			// note : you may also need
			// HttpURLConnection.setInstanceFollowRedirects(false)
			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");

			int rest = con.getResponseCode();
			System.out.println("URL = " + URLName + ", respond " + rest);
			return (rest == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
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
				// ImportFromEpsbedHelper.importData(dir, myProgressmeter,
				// myProgressmeterChild, myLabelProses);

				FeederConnector feederConnector;
				try {

					String url = "http://" + ip + ":" + (port.getValue() == null ? 8082 : port.getValue())
							+ "/ws/live.php";

					myLabelProses.setValue("Mencoba mengakses " + url);

					if (!exists(url)) {
						myProgressmeter.setValue(100);
						myLabelProses.setValue(ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(
						ip, String.valueOf(port.getValue() == null ? 8082 : port.getValue()),
						Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF),
						"Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."));
						return;
					}

					feederConnector = new FeederConnector(ip, port.getValue() == null ? 8082 : port.getValue(),
							myLabelProsesDetail);

					String username = ImportFromFeederAction.this.username.getValue().trim();
					String password = ImportFromFeederAction.this.password.getValue().trim();

					String token = feederConnector.getToken(username, password);
					System.out.println("TOKEN => " + token);

					if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
						myProgressmeter.setValue(100);
						myLabelProses.setValue(ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
						return;
					}

					System.out.println("TABLES => " + feederConnector.listTable(token));

					FeederImporter feederImporter = new FeederImporter(feederConnector, token, myProgressmeter,
							myProgressmeterChild, myLabelProses);
					feederImporter.doImport();
				} catch (Exception e) {
					myLabelProses.setValue("Terjadi kesalahan " + e.getMessage());
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
					button.setDisabled(false);
					MyMessageboxConfig.show(myLabelProses.getValue(), "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					timer.detach();

				}

			}
		});
		timer.setRepeats(true);
		timer.start();

	}

}
