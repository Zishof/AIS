package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.DaftarBimbinganPerDosenHelper;
import ais.action.master.helper.StudiMahasiswaHelper;
import ais.action.master.helper.util.PenilaianUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusKeluar;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class KrsMahasiswaAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchTahunAjaran;
	private Combobox searchGanjilGenap;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox statusKeluar;

	private Intbox mulai;
	private Intbox sampai;
	protected AmbilDataDosenBanbox searchdosen;
	protected Textbox searchnamadsn;
	private MyCheckboxConfig searchsp;
	private MyCheckboxConfig searchbukansp;

	private MyToolbarbuttonConfig find;
	private boolean edit = true;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.insertComboDanSemua(statusKeluar, new String[] { "nama", "feeder" }, "keterangan", StatusKeluar.class,
				"== Mahasiswa Belum Keluar / Masih Aktif ==", Restrictions.sqlRestriction("true"));
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchGanjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchGanjilGenap.appendChild(comboitem);

		Common.selectComboItem(searchGanjilGenap,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		if (searchGanjilGenap != null) { searchGanjilGenap.setReadonly(true); }

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setDisabled(true);
		}
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			searchnim.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setValue(tbmuser.getMahasiswa().getNama());
			searchnim.setReadonly(true);
			searchnim.setDisabled(true);
			searchnama.setReadonly(true);
			searchnama.setDisabled(true);
		}

		String[] contents = new String[] { "id", "mahasiswa", "tahunAkademik", "semester", "tahapan", "maksSks",
				"sksYangDiambil", "sksk", "mkBelumDiniali", "mkkBelumDinilai", "mkDinilai", "mkkDinilai",
				"semesterPendek", "selisih", "iplast", "ipk", "ips", "minip", "catatan", "catatanKhs", "noUts", "noUas",
				"krs", "dosenPa", "kelas", "belumDinilai", "telahDinilai" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		// Tombol "Daftar Bimbingan" DI SAMPING tombol Download: menampilkan daftar mahasiswa
		// bimbingan yang DIKELOMPOKKAN per Dosen Pembimbing Akademik. Datanya memakai ulang
		// initCriteria(true) sehingga selalu mengikuti filter pencarian yang sedang aktif.
		MyToolbarbuttonConfig daftarBimbinganButton = new MyToolbarbuttonConfig("Daftar Bimbingan",
				"/img/svg/user-group.svg");
		daftarBimbinganButton.setTooltiptext("Lihat daftar mahasiswa bimbingan, dikelompokkan per Dosen PA");
		daftarBimbinganButton.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				DaftarBimbinganPerDosenHelper.buka(initCriteria(true).list());
			}
		});
		Common.appendKeToolbar(daftarBimbinganButton, find, comp);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim Dosen PA ke Feeder",
					"/img/Finance-Invoice-icon.png");
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
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													List<KrsMahasiswa> tbmusers = initCriteria(true).list();
													int size = tbmusers.size();
													int index = 1;
													for (KrsMahasiswa krsMahasiswa : tbmusers) {
														myLabelProsesDetail.setValue("Memproses "
																+ krsMahasiswa.getMahasiswa() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.aktivitasMahasiswaKrs(krsMahasiswa, errorLog);

													}
													tbmusers.clear();
													tbmusers = null;

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengiriman data Dosen PA (KRS Mahasiswa) ke Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
												}
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, find, comp);

			buttonTagihan = new MyToolbarbuttonConfig("Kirim AKM ke Feeder", "/img/Finance-Invoice-icon.png");

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
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), myLabelProsesDetail);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													List<KrsMahasiswa> krsMahasiswas = initCriteria(true).list();

													int size = krsMahasiswas.size();
													int index = 1;
													for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {
														Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
														myLabelProsesDetail.setValue("Memproses " + mahasiswa.getNim()
																+ " " + mahasiswa.getNama() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;

														System.out.println("mahasiswa " + mahasiswa + "  krsMahasiswa "
																+ krsMahasiswa + " id = " + krsMahasiswa.getId());

														MonitorKRSMahasiswaAction.kirimKeFeeder(feederImporter,
																feederConnector, token, krsMahasiswa, errorLog);

													}
													krsMahasiswas.clear();
													krsMahasiswas = null;

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengiriman data AKM (KRS Mahasiswa) ke Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
												}
											}
										}).start();
									}

								}
							});
				}
			});
			Common.appendKeToolbar(buttonTagihan, find, comp);
		}

		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	public static void displayRow(Component arg0, final KrsMahasiswa krsMahasiswa, Html html, Html komentarshtml,
			MyLabelAgakKecil catatan, MyLabelAgakKecil catatanKhs, final EventListener eventListener) throws Exception {
		AuditListener.prosesUntukElearning(krsMahasiswa, "", krsMahasiswa.getId());
		Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
		Double[] batas = Common.getMinDanMaxIPK(mahasiswa, krsMahasiswa.getSemester(),
				krsMahasiswa.getSemesterPendek());
		Integer maxsks = batas[0].intValue();
		Hbox hbox = new Hbox();
		hbox.setParent(arg0);
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vb = RevisiHelper.createNewRevisi(KrsMahasiswa.class, krsMahasiswa, mahasiswa.getNama());
		vb.setParent(hbox);

		vb.appendChild(new MyLabelAgakKecil(mahasiswa.getNim()));

		vb.appendChild(new MyLabelAgakKecil("Kelas:" + krsMahasiswa.getKelas()));
		vb.appendChild(new MyLabelAgakKecil(
				"PA:" + (krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama())));
		vb.appendChild(new MyLabelAgakKecil("Angkatan:" + mahasiswa.getTahunangkatan()));

//		TbmuserAction.tampilkanSocialMediaProfile(vb, mahasiswa.getSocialMediaProfile());

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		vbox.appendChild(new MyLabelAgakKecil("TA:" + krsMahasiswa.getTahunAkademik()));
		vbox.appendChild(new MyLabelAgakKecil("Smt:" + krsMahasiswa.getSemester()));
		vbox.appendChild(new MyLabelAgakKecil("SP:" + (krsMahasiswa.getSemesterPendek() == null ? "Tidak" : "Ya")));
		if (krsMahasiswa.getTahapan() != null && krsMahasiswa.getTahapan() > 0) {
			vbox.appendChild(new MyLabelAgakKecil("Thp:" + krsMahasiswa.getTahapan()));
		}

		mahasiswa.tampilkanHp(vbox);
		mahasiswa.tampilkanEmail(vbox);

		new MyLabelAgakKecil(mahasiswa.getAlamat()).setParent(vbox);

		vbox = new Vbox();
		vbox.setParent(arg0);
		vbox.appendChild(new MyLabelAgakKecil("SKS:" + krsMahasiswa.getSksYangDiambil()));
		vbox.appendChild(new MyLabelAgakKecil("SKSK:" + Common.numberFormat.get().format(krsMahasiswa.getSksk())));
		vbox.appendChild(new MyLabelAgakKecil("Max SKS:" + maxsks));
		vbox.appendChild(new MyLabelAgakKecil("IPS:" + Common.numberFormat.get().format(krsMahasiswa.getIps())));
		vbox.appendChild(new MyLabelAgakKecil("IPK:" + Common.numberFormat.get().format(krsMahasiswa.getIpk())));

		vbox = new Vbox();
		vbox.setParent(arg0);

		html.setParent(vbox);
		catatan.setParent(vbox);
		catatanKhs.setParent(vbox);

		Vbox vbox1 = new Vbox();
		vbox1.setParent(vbox);
		Hbox hbox1 = new Hbox();

		LampiranLain.createDownloadUploadFileLain(hbox1, krsMahasiswa.getId(), "KRS_DISETUJUI", "Catatan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true);

		hbox1.setParent(vbox1);

		Integer komentars = krsMahasiswa.getKomentars();
		String kom = komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar";
		komentarshtml.setContent(kom);
		komentarshtml.setParent(vbox);

		if (Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim AKM",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
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
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												if (eventListener != null)
													eventListener.onEvent(arg0);

											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), myLabelProsesDetail);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													MonitorKRSMahasiswaAction.kirimKeFeeder(feederImporter,
															feederConnector, token, krsMahasiswa, errorLog);

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengiriman data AKM (KRS Mahasiswa) mahasiswa ini ke Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
												}
											}
										}).start();
									}

								}
							});
				}
			});
			vbox.appendChild(buttonTagihan);
		}
	}

	class KrsMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private EventListener event = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		};

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KrsMahasiswa krsMahasiswa = (KrsMahasiswa) arg1;
			final Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();

			final Label ip = new Label("IPK:" + Common.numberFormat.get().format(krsMahasiswa.getIpk()));
			final A sksk = new A("SKSK:" + Common.numberFormat.get().format(krsMahasiswa.getSksk()));

			sksk.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PenilaianUtil.downloadSemuaKRS(krsMahasiswa.getSkskS(), krsMahasiswa.getMahasiswa());
				}
			});

			final Html html = new ais.ui.util.MyHtml(mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
					krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false));
			final Html komentarshtml = new ais.ui.util.MyHtml("");
			final MyLabelAgakKecil catatan = new MyLabelAgakKecil(krsMahasiswa.getCatatan());
			final MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil(krsMahasiswa.getCatatanKhs());

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Tbmuser tbmuser = Common.getCurrentUser();

						boolean adminLainBoleh = false;
						String admLain = Common.getKonfigurasi(
								"admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", "").getNilai();
						String[] aa = admLain.split(";");
						for (String a : aa) {
							try {
								adminLainBoleh = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
								if (adminLainBoleh) {
									break;
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);

							}
						}

						StudiMahasiswaHelper studiMahasiswaHelper = new StudiMahasiswaHelper(
								krsMahasiswa.getSemesterPendek(),
								adminLainBoleh || (Common.bolehKonfigurasi("admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", Konfigurasi.TIDAK_AKTIF) && Common.getApakahAdmin()),
								true, edit);
						studiMahasiswaHelper.display(mahasiswa, krsMahasiswa.getTahunAkademik(),
								krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(), detail, html, komentarshtml, ip,
								sksk, catatan, catatanKhs);
					}
				}
			});

			KrsMahasiswaAction.displayRow(arg0, krsMahasiswa, html, komentarshtml, catatan, catatanKhs, event);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KrsMahasiswa.class)

				.add(searchsp.isChecked() ? Restrictions.eq("semesterPendek", Perkuliahan.SEMESTER_PENDEK)
						: Restrictions.sqlRestriction("true"))

				.add(searchbukansp.isChecked() ? Restrictions.isNull("semesterPendek")
						: Restrictions.sqlRestriction("true"))

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosenPa", searchdosen.getAttribute("myValue"))))
				.createAlias("mahasiswa", "mahasiswa")

				.add(statusKeluar.getSelectedItem() == null || statusKeluar.getSelectedItem().getValue() == null
						? Restrictions.isNull("mahasiswa.statusKeluar")
						: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar.getSelectedItem().getValue()))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()));

		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("dosenPa", "dosenPa")
					.add(Restrictions.ilike("dosenPa.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(Restrictions.between("semester", mulai.getValue() == null ? 0 : mulai.getValue(),
						sampai.getValue() == null ? 100 : sampai.getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", searchnim.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchGanjilGenap.getSelectedItem() == null
						|| searchGanjilGenap.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction("this_.semester%2="
										+ (searchGanjilGenap.getSelectedItem().getValue().equals(Perkuliahan.GENAP)
												? "0"
												: "1")))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()));

		// Join jurusan/fakultas cukup dibuat ketika filter fakultas benar-benar dipakai.
		// Pada pencarian default join ini memperbesar count/list KRS dan pernah memicu
		// statement timeout pada data besar.
		if (searchfakultas != null && searchfakultas.getSelectedItem() != null
				&& searchfakultas.getSelectedItem().getValue() != null) {
			criteria.createCriteria("mahasiswa.jurusan", Criteria.LEFT_JOIN)
					.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KrsMahasiswa> krsMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(krsMahasiswa);
		grid.setRowRenderer(new KrsMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
