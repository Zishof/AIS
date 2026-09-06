package ais.action.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PmbArkatama;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.JenisSeleksi;
import ais.database.model.KelompokJenisSeleksi;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis seleksi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Textbox
 * searchKelompok}, {@code Textbox nama}, {@code Textbox deskripsi}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code
 * exportKeFeeder()}); operasi domain lain ({@code onKelompokJenisSeleksi()}, {@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JenisSeleksiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchkode;
	private Textbox searchnama;
	private Textbox searchKelompok;

	private Textbox nama;
	private Textbox deskripsi;
	private Set<VerifikasiKelengkapanCalonMahasiswa> selectedVerifikasiKelengkapanCalonMahasiswa;
	private JenisSeleksi jenisSeleksi;
	private Textbox kode;

	private Checkbox searchaktif;

	private MyToolbarbuttonConfig add;
	//
	private boolean edit = false;
	private boolean delete = false;
	private Textbox kodeLain;
	private Combobox jenisDiskonMahasiswa;
	private Textbox feeder;

	private Tabpanel kelompokJenisSeleksiTab;
	private Combobox kelompokJenisSeleksi;

	public void onKelompokJenisSeleksi(Event event) {
		if (kelompokJenisSeleksiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompokJenisSeleksiTab);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_jenis_seleksi.zul");
			iframe.setParent(window);
		}
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
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(JenisSeleksi.class).add(Restrictions.eq("kode", "3"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			JenisSeleksi jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("PMDK Penelusuran minat dan kemampuan (akademik)");
			jenisSeleksi.setKode("3");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("SBMPTN");
			jenisSeleksi.setKode("1");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("SNMPTN");
			jenisSeleksi.setKode("2");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Seleksi Mandiri PTN");
			jenisSeleksi.setKode("5");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Seleksi Mandiri PTS");
			jenisSeleksi.setKode("6");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Ujian Masuk Bersama PTN (UMB-PT)");
			jenisSeleksi.setKode("7");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Ujian Masuk Bersama PTS (UMB-PTS)");
			jenisSeleksi.setKode("8");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Program Internasional");
			jenisSeleksi.setKode("9");
			session.save(jenisSeleksi);

			jenisSeleksi = new JenisSeleksi();
			jenisSeleksi.setNama("Program Kerjasama");
			jenisSeleksi.setKode("11");
			session.save(jenisSeleksi);
		}

		if (Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder PMB",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder PMB ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													PmbArkatama.synJalurMasuk(myLabelProsesDetail);
													// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"sinkronisasi Jalur Masuk Jenis Seleksi ke Feeder PMB",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Feeder PMB (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan konfigurasi integrasi PMB Arkatama sudah benar.",
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
			Common.appendKeToolbar(buttonTagihan, add, comp);

		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder PDDIKTI",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder PDDIKTI ?", "Pertanyaan",
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

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												onSearchDefault(null);
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

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													exportKeFeeder(perguruanTinggi, feederImporter, token,
															feederConnector);
													// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"sinkronisasi data Jenis Seleksi ke Neo Feeder PDDIKTI",
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
			Common.appendKeToolbar(buttonTagihan, add, comp);
		}

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "deskripsi", "kodeLain",
				"kelompokJenisSeleksi", "aktif", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisSeleksi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisSeleksi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private void exportKeFeeder(PerguruanTinggi perguruanTinggi, FeederExporter feederImporter, String token,
			FeederConnector feederConnector) {
		try {

			JSONArray dataProdi = feederConnector.getData("GetJalurMasuk", token, "", "", "5000", "0");
			System.out.println("jenisSeleksi size -> " + dataProdi.length());
			for (int index = 0; index < dataProdi.length(); index++) {
				JSONObject jsonObject = dataProdi.getJSONObject(index);
				System.out.println("jsonObject -> " + jsonObject);
				String id_jalur_masuk = jsonObject.getString("id_jalur_masuk");
				Session session = HibernateUtil.currentNativeSession();
				String nama_jalur_masuk = jsonObject.getString("nama_jalur_masuk");
				JenisSeleksi existing = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
						.add(Restrictions.ilike("nama", nama_jalur_masuk, MatchMode.EXACT)).setMaxResults(1)
						.uniqueResult();
				if (existing == null) {
					existing = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
							.add(Restrictions.eq("feeder", id_jalur_masuk)).setMaxResults(1).uniqueResult();
				}
				System.out.println("existing -> " + existing);
				if (existing != null) {
					existing.setFeeder(id_jalur_masuk);
					session.getTransaction().begin();
					session.saveOrUpdate(existing);
					session.getTransaction().commit();
				} else {
					existing = new JenisSeleksi();
					existing.setNama(nama_jalur_masuk);
					existing.setKode(id_jalur_masuk);
					existing.setFeeder(id_jalur_masuk);
					session.getTransaction().begin();
					session.save(existing);
					session.getTransaction().commit();
				}
				HibernateUtil.closeSession();

			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link JenisSeleksiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenisSeleksiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JenisSeleksiAction
	 */
	class JenisSeleksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisSeleksi jenisSeleksi = (JenisSeleksi) arg1;

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(JenisSeleksi.class, jenisSeleksi, jenisSeleksi.getKode()))
					.setParent(arg0);
			aa.appendChild(new Label(jenisSeleksi.getKodeLain()));
//			aa.appendChild(new Label(jenisSeleksi.getFeeder()));

			new Label(jenisSeleksi.getNama()).setParent(arg0);
			new Label(jenisSeleksi.getDeskripsi()).setParent(arg0);
			new Label(jenisSeleksi.getJenisDiskonMahasiswa() == null ? ""
					: jenisSeleksi.getJenisDiskonMahasiswa().getNama()).setParent(arg0);

			new Label(jenisSeleksi.getKelompokJenisSeleksi() == null ? ""
					: jenisSeleksi.getKelompokJenisSeleksi().getNama()).setParent(arg0);

			HibernateUtil.currentSession().refresh(jenisSeleksi);
			List<VerifikasiKelengkapanCalonMahasiswa> berkas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
					jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas());

			try {
				Collections.sort(berkas);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/JenisSeleksiAction.java:430");
				// TODO: handle exception
			}

			if (!berkas.isEmpty()) {
				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				vbox.setParent(detail);
				vbox.appendChild(new MyLabelBoldAja("Kelengkapan Berkas :"));
				int i = 1;
				for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : berkas) {
					vbox.appendChild(new MyLabelKecil(i + ". " + verifikasiKelengkapanCalonMahasiswa.getNama()));
					i++;
				}
				vbox.appendChild(new Html("<hr>"));
			}

			final Intbox intbox = new Intbox(jenisSeleksi.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisSeleksi.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(jenisSeleksi);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisSeleksi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisSeleksi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisSeleksi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisSeleksi, JenisSeleksiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisSeleksi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisSeleksi = (JenisSeleksi) obj;
		init(jenisSeleksi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
		addWindow.setTitle(jenisSeleksi.getId() == null ? "Tambah Jenis Seleksi" : "Ubah Jenis Seleksi");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(kode = new Textbox(jenisSeleksi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Lain (jika diperlukan)"));
		row.appendChild(kodeLain = new Textbox(jenisSeleksi.getKodeLain()));
		kodeLain.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(jenisSeleksi.getNama() == null ? "" : jenisSeleksi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(
				deskripsi = new Textbox(jenisSeleksi.getDeskripsi() == null ? "" : jenisSeleksi.getDeskripsi()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		row.appendChild(kelompokJenisSeleksi = new Combobox());
		Common.insertComboDanSemua(kelompokJenisSeleksi, new String[] { "nama" }, "keterangan",
				KelompokJenisSeleksi.class, "=Tanpa Kelompok=", Restrictions.eq("aktif", true));
		Common.selectComboItem(kelompokJenisSeleksi, jenisSeleksi.getKelompokJenisSeleksi());
		kelompokJenisSeleksi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diskon"));
		row.appendChild(jenisDiskonMahasiswa = new Combobox());
		Common.insertComboDanSemua(jenisDiskonMahasiswa, new String[] { "nama" }, "keterangan",
				JenisDiskonMahasiswa.class, "=Tanpa Diskon=", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisDiskonMahasiswa, jenisSeleksi.getJenisDiskonMahasiswa());
		jenisDiskonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(
				formulirVerifikasi = new MyCheckboxConfig("Terdapat Verifikasi Kelengkapan Berkas Pada Jenis Seleksi"));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Kelengkapan Berkas Pada Jenis Seleksi");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(VerifikasiKelengkapanCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				VerifikasiKelengkapanCalonMahasiswa.class);

		if (jenisSeleksi.getId() != null) {
			HibernateUtil.currentSession().refresh(this.jenisSeleksi);
		}
		selectedVerifikasiKelengkapanCalonMahasiswa = this.jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas();
		HashSet<Long> ids = new HashSet<Long>();
		for (VerifikasiKelengkapanCalonMahasiswa v : selectedVerifikasiKelengkapanCalonMahasiswa) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		subGrid.setVisible(!selectedVerifikasiKelengkapanCalonMahasiswa.isEmpty());
		formulirVerifikasi.setChecked(!selectedVerifikasiKelengkapanCalonMahasiswa.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
			final Checkbox checkbox = new Checkbox(verifikasiKelengkapanCalonMahasiswa.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(verifikasiKelengkapanCalonMahasiswa.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedVerifikasiKelengkapanCalonMahasiswa.add(verifikasiKelengkapanCalonMahasiswa);
					} else {
						for (VerifikasiKelengkapanCalonMahasiswa a : selectedVerifikasiKelengkapanCalonMahasiswa) {
							if (a.getId().equals(verifikasiKelengkapanCalonMahasiswa.getId())) {
								selectedVerifikasiKelengkapanCalonMahasiswa.remove(a);
								break;
							}
						}
					}

					System.out.println("selectedVerifikasiKelengkapanCalonMahasiswa => "
							+ selectedVerifikasiKelengkapanCalonMahasiswa);
				}
			});
		}

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode FEEDER"));
		row.appendChild(feeder = new Textbox(jenisSeleksi.getFeeder()));
		feeder.setWidth("90%");
		// feeder.setReadonly(true);

		// row = new MyFormRow();
		//		// row.setParent(rows);
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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisSeleksi.getId() != null) {
			jenisSeleksi = (JenisSeleksi) session.load(JenisSeleksi.class, jenisSeleksi.getId());

		}

		jenisSeleksi.setKode(kode.getValue().trim());
		jenisSeleksi.setNama(nama.getValue().trim());
		jenisSeleksi.setDeskripsi(deskripsi.getValue());
		jenisSeleksi
				.setJenisDiskonMahasiswa((JenisDiskonMahasiswa) (jenisDiskonMahasiswa.getSelectedItem() == null ? null
						: jenisDiskonMahasiswa.getSelectedItem().getValue()));

		jenisSeleksi
				.setKelompokJenisSeleksi((KelompokJenisSeleksi) (kelompokJenisSeleksi.getSelectedItem() == null ? null
						: kelompokJenisSeleksi.getSelectedItem().getValue()));

		jenisSeleksi.setKodeLain(kodeLain.getValue().trim());
		jenisSeleksi.setFeeder(feeder.getValue());

		jenisSeleksi.setVerifikasiKelengkapanCalonMahasiswas(selectedVerifikasiKelengkapanCalonMahasiswa);

		if (jenisSeleksi.getId() == null) {
			session.save(jenisSeleksi);
			session.flush();
		} else {
			Common.refreshUpdate(jenisSeleksi);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisSeleksi.class)
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("aktif", true) : Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		if (!searchKelompok.getValue().trim().isEmpty()) {
			criteria.createAlias("kelompokJenisSeleksi", "kelompokJenisSeleksi").add(Restrictions
					.ilike("kelompokJenisSeleksi.nama", searchKelompok.getValue().trim(), MatchMode.ANYWHERE));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisSeleksi> jenisSeleksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisSeleksi);
		grid.setRowRenderer(new JenisSeleksiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
