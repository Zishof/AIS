package ais.action.master;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BerkasHasilAkreditasiPunyaNama;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;

import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk perguruan tinggi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchalamat}, {@code Textbox
 * searchkodeyayasan}, {@code Textbox searchkodepergururantinggi}, {@code Textbox searchkota};
 * inisialisasi/lifecycle ({@code reInitByDomain()}, {@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * init()}, {@code init()}, {@code initMain()}); pembacaan/pencarian ({@code getDspace()}, {@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code exportKeFeeder()}); operasi
 * domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class PerguruanTinggiAction extends GenericAutowireComposer implements DataInitDefault {

	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchalamat;
	private Textbox searchkodeyayasan;
	private Textbox searchkodepergururantinggi;
	private Textbox searchkota;
	/** Hanya untuk super admin: bila dicentang, tampilkan SEMUA PT (abaikan filter domain). */
	private Checkbox abaikanDomain;

	private Textbox rektor;
	private Textbox kodeYayasan;
	private Textbox kodePerguruanTinggi;
	private Textbox nama;
	private Textbox alamat1;
	private Textbox alamat2;
	private Textbox dusun;
	private Textbox kelurahan;
	private Textbox rt;
	private Textbox rw;
	private Textbox kota;
	private Textbox kodePos;
	private Textbox telepon;
	private Textbox faksimili;
	private MyDatebox tanggalAkta;
	private MyDatebox tanggalAwalPendirian;
	private Textbox nomorAkta;
	private Textbox email;
	private Textbox website;

	private Textbox skIzinOperasi;
	private Textbox pejabatIzinOperasi;
	private MyDatebox tglSkIzinOperasi;
	private Intbox tahunPertamaMenerimaMahasiswa;
	private Textbox noRek;
	private Textbox nmBank;
	private Textbox unitCabang;
	private Textbox nmRek;

	private MyToolbarbuttonConfig add;
	private PerguruanTinggi perguruanTinggi;

	private MyDoublebox luasTanahMilik;
	private MyDoublebox luasTanahBukanMilik;
	private Decimalbox luasTanahTotal;
	private Decimalbox luasKebunLahanPercobaanTotal;
	private Decimalbox luasTotalRuangKuliah;
	private Decimalbox jumlahRuangKuliah;
	private Decimalbox luasTotalLab;
	private Decimalbox jumlahRuangLab;
	private Decimalbox luasTotalRuangDosenTetap;
	private Decimalbox luasTotalRuangAdministrasi;
	private Decimalbox luasTotalRuangSeminar;
	private Decimalbox luasTotalRuangEkskul;
	private Decimalbox luasTotalRuangPuskom;
	private Decimalbox luasTotalRuangPerpus;
	private Decimalbox jumlahJudulBuku;
	private Decimalbox jumlahEksemplarBuku;
	private Textbox feeder;
	private MyCkEditor deskripsi;
	private boolean edit;
	private boolean delete;
	private Textbox noSkAkreditasi;
	private MyDatebox tanggalAkreditasi;
	private Textbox akreditasi;
	private Textbox kodeSinta;
	private Textbox peringkatAkreditasi;
	private Combobox css;
	protected LampiranLain kop;
	protected LampiranLain logo;
	protected LampiranLain background;
	protected LampiranLain backgroundLogin;
	protected LampiranLain banner;
	private Textbox domain;
	private Textbox motto;
	private Textbox rektorNip;
	protected LampiranLain bannerMobile;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private boolean pt;

	private Tabpanel LembagaLain;
	private AmbilDataPegawaiBanbox kepala;
	private Textbox wa;
	protected LampiranLain kopBawah;
	private MyCheckboxConfig dosenHarusPakaiSatuanKerja;
	private AmbilDataPegawaiBanbox wakil1;
	private AmbilDataPegawaiBanbox wakil2;
	private AmbilDataPegawaiBanbox wakil3;
	private Textbox labelPejabat1;
	private AmbilDataPegawaiBanbox pegawai1;
	private Textbox labelPejabat2;
	private AmbilDataPegawaiBanbox pegawai2;
	private Textbox labelPejabat3;
	private AmbilDataPegawaiBanbox pegawai3;
	private Textbox labelPejabat4;
	private AmbilDataPegawaiBanbox pegawai4;
	private Textbox labelPejabat5;
	private AmbilDataPegawaiBanbox pegawai5;
	protected LampiranLain kop_ppdb;
	protected LampiranLain bg_ppdb;
	protected LampiranLain footer_ppdb;
	protected LampiranLain bg_pt;
	private MyCkEditor headerpmb;
	protected LampiranLain kopStempel;
	private Combobox status;
	private Combobox piilhanTampilanCb;
	private Textbox propinsi;
	private Tbmuser tbmuser;

	public static volatile Map<String, PerguruanTinggi> perguruanTinggiByDomain = new HashMap<String, PerguruanTinggi>();
	public static volatile PerguruanTinggi perguruanTinggiDefault = new PerguruanTinggi();

	public static volatile Map<String, Pendaftar> pendaftarByDomain = new HashMap<String, Pendaftar>();

	private static final ReentrantLock REINIT_DOMAIN_LOCK = new ReentrantLock();
	private static volatile long reinitDomainTerakhir = 0L;
	/** Jeda minimum antar-rebuild peta domain (throttle anti thundering-herd query DB). */
	private static final long REINIT_DOMAIN_INTERVAL_MS = 60000L;

	/**
	 * Membangun ulang peta domain-&gt;PerguruanTinggi/Pendaftar dengan aman: single-flight (hanya satu
	 * thread rebuild; request lain langsung memakai snapshot lama tanpa menunggu), throttle (tak
	 * query ulang &lt; {@value #REINIT_DOMAIN_INTERVAL_MS} ms), dan
	 * atomic-swap (bangun ke peta BARU lalu tukar referensi). Dulu method ini {@code clear()} peta
	 * bersama lalu menjalankan DUA query per pemanggilan; saat peta kosong, setiap request paralel
	 * memanggilnya sehingga pool koneksi c3p0 HABIS (ratusan thread antre koneksi).
	 */
	public static void reInitByDomain() {
		if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
			return;
		}
		if (!REINIT_DOMAIN_LOCK.tryLock()) {
			return;
		}
		try {
			if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
				return;
			}
			Session session = null;
			try {
				boolean menggunakanPtDefault = Common.bolehKonfigurasi("menggunakanPtDefault");
				session = HibernateUtil.getSessionFactory().openSession();
				/* KE-FIX ("Session is closed!"): reInitByDomain dapat terpanggil DARI DALAM
				 * flush/commit (lewat getter entity seperti NomorSurat.getContohFormat) atau dari
				 * thread latar yang sesinya sudah ditutup. Memaksa createCriteria pada sesi mati
				 * hanya melempar exception dan menggagalkan commit pemanggil. Lewati saja --
				 * peta domain lama tetap dipakai dan disegarkan pada pemanggilan berikutnya. */
				if (session == null || !session.isOpen()) {
					return;
				}
				List<PerguruanTinggi> perguruanTinggis = ConstantValues.simpleList(
						session.createCriteria(PerguruanTinggi.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						PerguruanTinggi.class);
				List<Pendaftar> pendaftars = ConstantValues.simpleList(
						session.createCriteria(Pendaftar.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Pendaftar.class);

				Map<String, PerguruanTinggi> ptBaru = new HashMap<String, PerguruanTinggi>();
				PerguruanTinggi defaultBaru = menggunakanPtDefault ? perguruanTinggiDefault : new PerguruanTinggi();
				for (PerguruanTinggi perguruanTinggi : perguruanTinggis) {
					if (perguruanTinggi == null) {
						continue;
					}
					if (menggunakanPtDefault) {
						defaultBaru = perguruanTinggi;
					}
					// MULTI-DOMAIN: satu PT boleh punya beberapa domain (dipisah koma) → daftarkan tiap domain.
				for (String d : Common.pisahDomain(perguruanTinggi.getDomain())) {
					ptBaru.put(d, perguruanTinggi);
				}
				}
				Map<String, Pendaftar> pendaftarBaru = new HashMap<String, Pendaftar>();
				for (Pendaftar pendaftar : pendaftars) {
					if (pendaftar != null) {
						// MULTI-DOMAIN: satu Pendaftar boleh punya beberapa domain (dipisah koma).
						for (String d : Common.pisahDomain(pendaftar.getDomain())) {
							pendaftarBaru.put(d, pendaftar);
						}
					}
				}

				// Tukar referensi secara atomik — pembaca tak pernah melihat peta setengah-terisi.
				perguruanTinggiByDomain = ptBaru;
				pendaftarByDomain = pendaftarBaru;
				perguruanTinggiDefault = defaultBaru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				reinitDomainTerakhir = System.currentTimeMillis();
				try {
					if (session != null) session.clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "PerguruanTinggiAction.reInitByDomain clear session");
				}
				try {
					if (session != null && session.isConnected()) session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "PerguruanTinggiAction.reInitByDomain disconnect session");
				}
				try {
					if (session != null && session.isOpen()) session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/PerguruanTinggiAction.java:276");
				}
			}
		} finally {
			REINIT_DOMAIN_LOCK.unlock();
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		Div mainContainer = (Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		final MyButtonTabbox btabs = MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		Div panel0 = btabs.tambahTab(0, "Lembaga ini");
		MyButtonTabbox.muatZulEager(panel0,
				"/WEB-INF/z/x/y/pages/master/perguruan_tinggi_tab_0_lembaga_ini.zul");

		super.doAfterCompose(comp);

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];

		// Filter "Abaikan Domain" hanya tampil untuk super admin.
		if (abaikanDomain != null) {
			abaikanDomain.setVisible(Common.getApakahAdminLain(tbmuser));
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor Berkas", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();

							Session session = HibernateUtil.currentSession();
							Criteria criteria = session.createCriteria(BerkasHasilAkreditasiPunyaNama.class)
									.createCriteria("berkasHasilAkreditasi").createCriteria("perguruanTinggi")

									.addOrder(Order.asc("nama"))
									.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("nama", searchnama.getValue().trim(),
													MatchMode.ANYWHERE));

							List<BerkasHasilAkreditasiPunyaNama> berkasHasilAkreditasiPunyaNamas = criteria.list();

							int rowIndex = 1;
							for (BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama : berkasHasilAkreditasiPunyaNamas) {
								label.setValue(
										"Sedang memproses data " + berkasHasilAkreditasiPunyaNama.toString() + " ("
												+ Common.numberFormat.get().format(
														(rowIndex++) * 100.0 / berkasHasilAkreditasiPunyaNamas.size())
												+ " %)");
								JurusanAction.getDspace(cookie, berkasHasilAkreditasiPunyaNama, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor Berkas", "/img/svg/trash.svg");
		Common.appendKeToolbar(batalExport, add, comp);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												Session session = HibernateUtil.currentSession();
												Criteria criteria = session
														.createCriteria(BerkasHasilAkreditasiPunyaNama.class)
														.createCriteria("berkasHasilAkreditasi")
														.createCriteria("perguruanTinggi")

														.addOrder(Order.asc("nama")).add(Restrictions.ilike("nama",
																searchnama.getValue(), MatchMode.ANYWHERE));

												List<BerkasHasilAkreditasiPunyaNama> berkasHasilAkreditasiPunyaNamas = criteria
														.list();

												int rowIndex = 1;
												for (BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama : berkasHasilAkreditasiPunyaNamas) {

													label.setValue(
															"Sedang memproses data "
																	+ berkasHasilAkreditasiPunyaNama.toString() + " ("
																	+ Common.numberFormat.get().format((rowIndex++) * 100.0
																			/ berkasHasilAkreditasiPunyaNamas.size())
																	+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(
																	BerkasHasilAkreditasiPunyaNama.class.getName(),
																	berkasHasilAkreditasiPunyaNama.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
	}
});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder ?", "Pertanyaan",
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
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													exportKeFeeder(username, feederImporter, token, feederConnector);

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan dari pengguna.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"sinkronisasi data Perguruan Tinggi ke Neo Feeder",
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

		// Tab 1: Lembaga lain (lazy include — hanya untuk institusi PT)
		if (pt) {
			final String srcLembagaLain = "/WEB-INF/z/x/y/pages/master/perguruan_tinggi_lain.zul";
			btabs.tambahTabLazy(1, "Lembaga lain", new MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(Div panel) throws Exception {
					MyButtonTabbox.muatZul(panel, srcLembagaLain);
				}
			});
			btabs.pulihkanSeleksi(2);
		} else {
			btabs.pulihkanSeleksi(1);
		}

	        FilterLanjutHelper.setup(comp);
}

	private void exportKeFeeder(String username, FeederExporter feederImporter, String token,
			FeederConnector feederConnector) throws Exception {
		// FIX "gagal diam-diam": sebelumnya exception di sini ditelan total (hanya
		// dicatat ke log admin) sehingga pemanggil (thread latar) menganggap proses
		// SELESAI sukses walau sebenarnya gagal. Sekarang dilempar ke pemanggil.
		String filter = "";

		JSONArray dataMhsPt = feederConnector.getData("GetProfilPT", token, filter, "", "1", "0");

		for (int i = 0; i < dataMhsPt.length(); i++) {
			JSONObject jsonObject = dataMhsPt.getJSONObject(i);
			FeederJSONImport.perguruanTinggi(jsonObject);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PerguruanTinggiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PerguruanTinggiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PerguruanTinggiAction
	 */
	class PerguruanTinggiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(perguruanTinggi.getKodeYayasan()).setParent(vbox);
			new MyLabelAgakKecil(perguruanTinggi.getFeeder()).setParent(vbox);

			new Label(perguruanTinggi.getKodePerguruanTinggi()).setParent(arg0);

			RevisiHelper.createNewRevisi(PerguruanTinggi.class, perguruanTinggi, perguruanTinggi.getNama())
					.setParent(arg0);

			new Label(perguruanTinggi.getAlamat1()).setParent(arg0);
			new Label(perguruanTinggi.getKota()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(perguruanTinggi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perguruanTinggi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(perguruanTinggi);
				}
			});

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, perguruanTinggi, PerguruanTinggiAction.this))
					.setParent(arg0);

			GeneralValueObject.tampilKunci(toolbar, perguruanTinggi, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);
		}

	}

	public static DspaceInformation getDspace(String cookie, PerguruanTinggi perguruanTinggi, boolean update)
			throws Exception {

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", perguruanTinggi.getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", perguruanTinggi.getDeskripsi());

		jsonPost.put("shortDescription", "Repositori milik perguruan tinggi " + perguruanTinggi.getNama());
		jsonPost.put("sidebarText",
				"Berisi semua repository Repositori milik perguruan tinggi " + perguruanTinggi.getNama());

		return DspaceInformation.dspaceProcess(cookie, perguruanTinggi, jsonPost.toString(), update, "communities",
				"communities");
	}

	private void init(final PerguruanTinggi perguruanTinggi) throws Exception {
		this.perguruanTinggi = perguruanTinggi;

		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });

		{
			org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Data Utama", "/img/svg/book.svg");
			panel.appendChild(initMain(perguruanTinggi));
		}
		{
			org.zkoss.zul.Div panel = btnTab.tambahTab(1, "Data Pejabat", "/img/svg/user-tie.svg");
			panel.appendChild(initPejabat());
		}
		{
			org.zkoss.zul.Div panel = btnTab.tambahTab(2, "Fasilitas Penunjang", "/img/svg/chalkboard-teacher-light.svg");
			panel.appendChild(initFasilitasAkademik());
		}

		btnTab.tambahTabLazy(3, "Berkas Akreditasi", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude("/pages/master/berkas_hasil_akreditasi.zul?perguruanTinggi=" + perguruanTinggi.getId()).setParent(window);
			}
		});
		btnTab.setVisibleTombol(3, perguruanTinggi.getId() != null);

		{
			org.zkoss.zul.Div panel = btnTab.tambahTab(4, "Deskripsi", "/img/svg/pencil-square.svg");
			deskripsi = new MyCkEditor();
			deskripsi.setParent(panel);
			deskripsi.setValue(perguruanTinggi.getDeskripsi());
			deskripsi.setHeight("100%");
			deskripsi.setWidth("100%");
		}
		{
			org.zkoss.zul.Div panel = btnTab.tambahTab(5, "Tampilan", "/img/svg/desktop-light.svg");
			panel.appendChild(initTampilan());
		}

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

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		perguruanTinggi = (PerguruanTinggi) obj;
		init(perguruanTinggi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PerguruanTinggi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Borderlayout initMain(PerguruanTinggi perguruanTinggi) throws Exception {
		this.perguruanTinggi = perguruanTinggi;
		addWindow.setTitle(perguruanTinggi.getId() == null ? "Tambah Perguruan Tinggi" : "Ubah Perguruan Tinggi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Yayasan"));
		row.appendChild(kodeYayasan = new Textbox(
				perguruanTinggi.getKodeYayasan() == null ? "" : perguruanTinggi.getKodeYayasan()));
		kodeYayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Lembaga"));
		row.appendChild(kodePerguruanTinggi = new Textbox(
				perguruanTinggi.getKodePerguruanTinggi() == null ? "" : perguruanTinggi.getKodePerguruanTinggi()));
		kodePerguruanTinggi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(perguruanTinggi.getNama() == null ? "" : perguruanTinggi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Rektor / Ketua / Kepala"));
		row.appendChild(rektor = new Textbox(perguruanTinggi.getRektor()));
		rektor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIP Rektor / Ketua / Kepala"));
		row.appendChild(rektorNip = new Textbox(perguruanTinggi.getRektorNip()));
		rektorNip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat1"));
		row.appendChild(
				alamat1 = new Textbox(perguruanTinggi.getAlamat1() == null ? "" : perguruanTinggi.getAlamat1()));
		alamat1.setWidth("90%");
		alamat1.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat2"));
		row.appendChild(
				alamat2 = new Textbox(perguruanTinggi.getAlamat2() == null ? "" : perguruanTinggi.getAlamat2()));
		alamat2.setWidth("90%");
		alamat2.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status *"));
		row.appendChild(status = new Combobox());
		status.setWidth("90%");
		status.setReadonly(true);
		Comboitem comboitem = new Comboitem("Negeri");
		comboitem.setValue("Negeri");
		status.appendChild(comboitem);
		comboitem = new Comboitem("Swasta");
		comboitem.setValue("Swasta");
		status.appendChild(comboitem);
		Common.selectComboItem(status, perguruanTinggi.getStatus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dusun / Kampung"));
		row.appendChild(dusun = new Textbox(perguruanTinggi.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan"));
		row.appendChild(kelurahan = new Textbox(perguruanTinggi.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT"));
		row.appendChild(rt = new Textbox(perguruanTinggi.getRt()));
		rt.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW"));
		row.appendChild(rw = new Textbox(perguruanTinggi.getRw()));
		rw.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota / Kabupaten"));
		row.appendChild(kota = new Textbox(perguruanTinggi.getKota() == null ? "" : perguruanTinggi.getKota()));
		kota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi"));
		row.appendChild(propinsi = new Textbox(perguruanTinggi.getPropinsi()));
		propinsi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(
				kodePos = new Textbox((perguruanTinggi.getKodePos() == null ? "" : perguruanTinggi.getKodePos())));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telepon"));
		row.appendChild(
				telepon = new Textbox(perguruanTinggi.getTelepon() == null ? "" : perguruanTinggi.getTelepon()));
		telepon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Faksimili"));
		row.appendChild(
				faksimili = new Textbox(perguruanTinggi.getFaksimili() == null ? "" : perguruanTinggi.getFaksimili()));
		faksimili.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("WA Operator"));
		row.appendChild(wa = new Textbox(perguruanTinggi.getWa()));
		wa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Pendirian"));
		row.appendChild(nomorAkta = new Textbox(
				(perguruanTinggi.getNomorAkta() == null ? "" : perguruanTinggi.getNomorAkta())));
		nomorAkta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. SK Pendirian"));
		row.appendChild(tanggalAkta = new MyDatebox(perguruanTinggi.getTanggalAkta()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Pendirian"));
		row.appendChild(tanggalAwalPendirian = new MyDatebox(perguruanTinggi.getTanggalAwalPendirian()));

		// private Textbox skIzinOperasi;
		// private MyDatebox tglSkIzinOperasi;
		// private Textbox noRek;
		// private Textbox nmBank;
		// private Textbox unitCabang;
		// private Textbox nmRek;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Izin"));
		row.appendChild(skIzinOperasi = new Textbox((perguruanTinggi.getSkIzinOperasi())));
		skIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. SK Izin"));
		row.appendChild(tglSkIzinOperasi = new MyDatebox((perguruanTinggi.getTglSkIzinOperasi())));
		tglSkIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pejabat SK Izin"));
		row.appendChild(pejabatIzinOperasi = new Textbox((perguruanTinggi.getPejabatIzinOperasi())));
		pejabatIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Pertama Menerima Mahasiswa"));
		row.appendChild(tahunPertamaMenerimaMahasiswa = new Intbox(perguruanTinggi.getTahunPertamaMenerimaMahasiswa()));

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat Akreditasi Terakhir BAN-PT"));
		row.appendChild(peringkatAkreditasi = new Textbox(perguruanTinggi.getPeringkatAkreditasi()));
		peringkatAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Akreditasi Terakhir BAN-PT"));
		row.appendChild(akreditasi = new Textbox(perguruanTinggi.getAkreditasi()));
		akreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Akreditasi Terakhir BAN-PT"));
		row.appendChild(noSkAkreditasi = new Textbox(perguruanTinggi.getNoSkAkreditasi()));
		noSkAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akreditasi Terakhir BAN-PT"));
		row.appendChild(tanggalAkreditasi = new MyDatebox(perguruanTinggi.getTanggalAkreditasi()));
		tanggalAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Bank"));
		row.appendChild(nmBank = new Textbox((perguruanTinggi.getNmBank())));
		nmBank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Rekening Bank"));
		row.appendChild(noRek = new Textbox((perguruanTinggi.getNoRek())));
		noRek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Rekening Bank"));
		row.appendChild(nmRek = new Textbox((perguruanTinggi.getNmRek())));
		nmRek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Cabang"));
		row.appendChild(unitCabang = new Textbox((perguruanTinggi.getUnitCabang())));
		unitCabang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(perguruanTinggi.getEmail() == null ? "" : perguruanTinggi.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Website"));
		row.appendChild(
				website = new Textbox(perguruanTinggi.getWebsite() == null ? "" : perguruanTinggi.getWebsite()));
		website.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain"));
		row.appendChild(domain = new Textbox(perguruanTinggi.getDomain()));
		domain.setWidth("90%");
		Common.initKeterangan(rows,
				"Bisa lebih dari satu domain, dipisah tanda koma (,). Contoh: ecampus.a.ac.id, ecampus.b.ac.id, ecampus.c.ac.id. "
						+ "Sistem akan mencocokkan alamat yang sedang dibuka dengan salah satu domain di atas.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Motto"));
		row.appendChild(motto = new Textbox(perguruanTinggi.getMotto()));
		motto.setWidth("90%");
		motto.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false));
		satuanKerja.setAttribute("satuanKerja", perguruanTinggi.getSatuanKerja());
		satuanKerja
				.setValue(perguruanTinggi.getSatuanKerja() == null ? "" : perguruanTinggi.getSatuanKerja().getNama());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(dosenHarusPakaiSatuanKerja = new MyCheckboxConfig("Dosen Harus Pakai Satuan Kerja"));
		dosenHarusPakaiSatuanKerja.setChecked(perguruanTinggi.getDosenHarusPakaiSatuanKerja());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Sinta"));
		row.appendChild(kodeSinta = new Textbox(perguruanTinggi.getKodeSinta()));
		kodeSinta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Header PMB"));
		row.appendChild(headerpmb = new MyCkEditor());
		headerpmb.setValue(perguruanTinggi.getHeaderpmb());
		headerpmb.setHeight("100%");
		headerpmb.setWidth("100%");

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder() && pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Feeder"));
		row.appendChild(feeder = new Textbox(perguruanTinggi.getFeeder()));
		feeder.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan Tampilan UI/UX"));
		piilhanTampilanCb = new Combobox();
		piilhanTampilanCb.setReadonly(true);
		piilhanTampilanCb.setWidth("90%");
		Comboitem ciTampilanDefault = new Comboitem("Ikuti Default (Konfigurasi Sistem)");
		ciTampilanDefault.setValue(PerguruanTinggi.TAMPILAN_DEFAULT);
		piilhanTampilanCb.appendChild(ciTampilanDefault);
		Comboitem ciTampilanKlasik = new Comboitem("Tampilan Klasik (ZKoss)");
		ciTampilanKlasik.setValue(PerguruanTinggi.TAMPILAN_KLASIK);
		piilhanTampilanCb.appendChild(ciTampilanKlasik);
		Comboitem ciTampilanBaru = new Comboitem("Tampilan Baru & Modern");
		ciTampilanBaru.setValue(PerguruanTinggi.TAMPILAN_BARU);
		piilhanTampilanCb.appendChild(ciTampilanBaru);
		String curTampilanPT = perguruanTinggi.getPiilhanTampilan();
		boolean tampilanPTSet = false;
		for (Object oo : piilhanTampilanCb.getItems()) {
			Comboitem ci = (Comboitem) oo;
			if (curTampilanPT != null && curTampilanPT.equals(ci.getValue())) {
				piilhanTampilanCb.setSelectedItem(ci);
				tampilanPTSet = true;
				break;
			}
		}
		if (!tampilanPTSet) piilhanTampilanCb.setSelectedIndex(0);
		row.appendChild(piilhanTampilanCb);

		if (perguruanTinggi.getDikunci() != null) {
			Common.freezeGanti(center, true);
		} else if (perguruanTinggi.getPendaftar() != null) {
			Common.freezeGanti(kodeYayasan, kodePerguruanTinggi, nama, alamat1, telepon, email, domain, motto);
		}

		return borderlayout;

	}

	private Borderlayout initPejabat() throws Exception {
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rektor / Ketua / Kepala (ambil dari data pegawai)"));
		row.appendChild(kepala = new AmbilDataPegawaiBanbox(false));
		kepala.setAttribute("pegawai", perguruanTinggi.getKepala());
		kepala.setValue(perguruanTinggi.getKepala() == null ? "" : perguruanTinggi.getKepala().getNama());
		kepala.setWidth("90%");
		kepala.setReadonly(true);

		EventListener rektorEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai a = (Pegawai) kepala.getAttribute("pegawai");

				if (rektor.getParent() != null)
					rektor.getParent().setVisible(a == null);

				if (rektorNip.getParent() != null)
					rektorNip.getParent().setVisible(a == null);
			}
		};

		rektorEventListener.onEvent(null);
		kepala.setEventListener(rektorEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wakil I"));
		row.appendChild(wakil1 = new AmbilDataPegawaiBanbox(false));
		wakil1.setAttribute("pegawai", perguruanTinggi.getWakil1());
		wakil1.setValue(perguruanTinggi.getWakil1() == null ? "" : perguruanTinggi.getWakil1().getNama());
		wakil1.setWidth("90%");
		wakil1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wakil II"));
		row.appendChild(wakil2 = new AmbilDataPegawaiBanbox(false));
		wakil2.setAttribute("pegawai", perguruanTinggi.getWakil2());
		wakil2.setValue(perguruanTinggi.getWakil2() == null ? "" : perguruanTinggi.getWakil2().getNama());
		wakil2.setWidth("90%");
		wakil2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wakil III"));
		row.appendChild(wakil3 = new AmbilDataPegawaiBanbox(false));
		wakil3.setAttribute("pegawai", perguruanTinggi.getWakil3());
		wakil3.setValue(perguruanTinggi.getWakil3() == null ? "" : perguruanTinggi.getWakil3().getNama());
		wakil3.setWidth("90%");
		wakil3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(labelPejabat1 = new Textbox(perguruanTinggi.getLabelPejabat1()));
		row.appendChild(pegawai1 = new AmbilDataPegawaiBanbox(false));
		pegawai1.setAttribute("pegawai", perguruanTinggi.getPejabat1());
		pegawai1.setValue(perguruanTinggi.getPejabat1() == null ? "" : perguruanTinggi.getPejabat1().getNama());
		pegawai1.setWidth("90%");
		pegawai1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(labelPejabat2 = new Textbox(perguruanTinggi.getLabelPejabat2()));
		row.appendChild(pegawai2 = new AmbilDataPegawaiBanbox(false));
		pegawai2.setAttribute("pegawai", perguruanTinggi.getPejabat2());
		pegawai2.setValue(perguruanTinggi.getPejabat2() == null ? "" : perguruanTinggi.getPejabat2().getNama());
		pegawai2.setWidth("90%");
		pegawai2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(labelPejabat3 = new Textbox(perguruanTinggi.getLabelPejabat3()));
		row.appendChild(pegawai3 = new AmbilDataPegawaiBanbox(false));
		pegawai3.setAttribute("pegawai", perguruanTinggi.getPejabat3());
		pegawai3.setValue(perguruanTinggi.getPejabat3() == null ? "" : perguruanTinggi.getPejabat3().getNama());
		pegawai3.setWidth("90%");
		pegawai3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(labelPejabat4 = new Textbox(perguruanTinggi.getLabelPejabat4()));
		row.appendChild(pegawai4 = new AmbilDataPegawaiBanbox(false));
		pegawai4.setAttribute("pegawai", perguruanTinggi.getPejabat4());
		pegawai4.setValue(perguruanTinggi.getPejabat4() == null ? "" : perguruanTinggi.getPejabat4().getNama());
		pegawai4.setWidth("90%");
		pegawai4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(labelPejabat5 = new Textbox(perguruanTinggi.getLabelPejabat5()));
		row.appendChild(pegawai5 = new AmbilDataPegawaiBanbox(false));
		pegawai5.setAttribute("pegawai", perguruanTinggi.getPejabat5());
		pegawai5.setValue(perguruanTinggi.getPejabat5() == null ? "" : perguruanTinggi.getPejabat5().getNama());
		pegawai5.setWidth("90%");
		pegawai5.setReadonly(true);
		if (perguruanTinggi.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}
		return borderlayout;
	}

	public static Combobox buatTema() {
		Combobox css = new Combobox();
		css.setWidth("90%");
		css.setReadonly(true);
		Comboitem comboitem = new Comboitem("Default");
		comboitem.setValue("");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Biru");
		comboitem.setValue("/css/ytb.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Biru Tua");
		comboitem.setValue("/css/biru_tua.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Biru Merah");
		comboitem.setValue("/css/biru_merah.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Biru Hijau");
		comboitem.setValue("/css/biru_hijau.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Biru Orange");
		comboitem.setValue("/css/asm.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau");
		comboitem.setValue("/css/hijau.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau Orange I");
		comboitem.setValue("/css/hijau_orange.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau Orange II");
		comboitem.setValue("/css/hijau_orange2.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau Kuning");
		comboitem.setValue("/css/hijau_kuning.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Abu Abu");
		comboitem.setValue("/css/muda_hitam.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Merah 1");
		comboitem.setValue("/css/sd.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Merah 2");
		comboitem.setValue("/css/merah.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau Tua I");
		comboitem.setValue("/css/smp.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Hijau Tua II");
		comboitem.setValue("/css/sma.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Kuning Tua");
		comboitem.setValue("/css/tk.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Kuning Biru Tua");
		comboitem.setValue("/css/kuning_biru_tua.css");
		css.appendChild(comboitem);

		comboitem = new Comboitem("Kuning Hijau");
		comboitem.setValue("/css/kuning_hijau.css");
		css.appendChild(comboitem);
		return css;
	}

	private Borderlayout initTampilan() {
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tema"));
		row.appendChild(css = PerguruanTinggiAction.buatTema());
		Common.selectComboItem(css, perguruanTinggi.getCss());

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Atas (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.KOP_PT, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		kopBawah = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Bawah (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT, "KOP",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopBawah = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		kopStempel = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Stempel (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.STEMPEL_PT, "Stempel",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopStempel = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		kop_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HEADER PMB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.KOP_PMB_PT, "HEADER",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		bg_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background PMB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BG_PMB_PT, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		bg_pt = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Utama (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BG_PT, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_pt = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		footer_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("FOOTER PMB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.FOOTER_PMB_PT, "FOOTER",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						footer_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		logo = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Logo PT"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.LOGO_PT, "Logo", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						logo = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		background = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background PT"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BACKGROUND_PT,
				"Background", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						background = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		backgroundLogin = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Login PT"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BACKGROUND_LOGIN_PT,
				"Background Login", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						backgroundLogin = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		banner = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Banner Halaman Web"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BANNER_UTAMA_PT, "Banner",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						banner = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);

		bannerMobile = null;
		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Banner Halaman Mobile"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, perguruanTinggi.getId(), LampiranLain.BANNER_MOBILE_PT,
				"Banner", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bannerMobile = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, perguruanTinggi.getDikunci() == null, null);
		hbox.setParent(row);
		if (perguruanTinggi.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}
		return borderlayout;
	}

	private Borderlayout initFasilitasAkademik() {

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas Tanah Milik"));
		row.appendChild(luasTanahMilik = new MyDoublebox(perguruanTinggi.getLuasTanahMilik()));
		luasTanahMilik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas Tanah Bukan Milik"));
		row.appendChild(luasTanahBukanMilik = new MyDoublebox(perguruanTinggi.getLuasTanahBukanMilik()));
		luasTanahBukanMilik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas tanah total"));
		row.appendChild(luasTanahTotal = new Decimalbox(new BigDecimal(
				perguruanTinggi.getLuasTanahTotal() == null ? 0.0 : perguruanTinggi.getLuasTanahTotal())));
		luasTanahTotal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas Kebun Lahan Percobaan Total"));
		row.appendChild(luasKebunLahanPercobaanTotal = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasKebunLahanPercobaanTotal() == null ? 0
						: perguruanTinggi.getLuasKebunLahanPercobaanTotal())));
		luasKebunLahanPercobaanTotal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang kuliah"));
		row.appendChild(luasTotalRuangKuliah = new Decimalbox(new BigDecimal(
				perguruanTinggi.getLuasTotalRuangKuliah() == null ? 0.0 : perguruanTinggi.getLuasTotalRuangKuliah())));
		luasTotalRuangKuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah ruang kuliah"));
		row.appendChild(
				jumlahRuangKuliah = new Decimalbox(new BigDecimal(perguruanTinggi.getJumlahRuangKuliah() == null ? 0
						: perguruanTinggi.getJumlahRuangKuliah().intValue())));
		jumlahRuangKuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total lab studio"));
		row.appendChild(luasTotalLab = new Decimalbox(new BigDecimal(
				perguruanTinggi.getLuasTotalLabStudio() == null ? 0.0 : perguruanTinggi.getLuasTotalLabStudio())));
		luasTotalLab.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah ruang lab"));
		row.appendChild(jumlahRuangLab = new Decimalbox(new BigDecimal(
				perguruanTinggi.getJumlahRuangLab() == null ? 0 : perguruanTinggi.getJumlahRuangLab().intValue())));
		jumlahRuangLab.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang dosen tetap"));
		row.appendChild(luasTotalRuangDosenTetap = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasTotalRuangDosenTetap() == null ? 0.0
						: perguruanTinggi.getLuasTotalRuangDosenTetap())));
		luasTotalRuangDosenTetap.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang administrasi"));
		row.appendChild(luasTotalRuangAdministrasi = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasTotalRuangAdministrasi() == null ? 0.0
						: perguruanTinggi.getLuasTotalRuangAdministrasi())));
		luasTotalRuangAdministrasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang seminar"));
		row.appendChild(luasTotalRuangSeminar = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasTotalRuangSeminar() == null ? 0.0
						: perguruanTinggi.getLuasTotalRuangSeminar())));
		luasTotalRuangSeminar.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang ekskul"));
		row.appendChild(luasTotalRuangEkskul = new Decimalbox(new BigDecimal(
				perguruanTinggi.getLuasTotalRuangEkskul() == null ? 0.0 : perguruanTinggi.getLuasTotalRuangEkskul())));
		luasTotalRuangEkskul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang pusat komputer"));
		row.appendChild(luasTotalRuangPuskom = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasTotalPusatKomputer() == null ? 0.0
						: perguruanTinggi.getLuasTotalPusatKomputer())));
		luasTotalRuangPuskom.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas total ruang perpustakaan"));
		row.appendChild(luasTotalRuangPerpus = new Decimalbox(
				new BigDecimal(perguruanTinggi.getLuasTotalRuangPerpustakaan() == null ? 0.0
						: perguruanTinggi.getLuasTotalRuangPerpustakaan())));
		luasTotalRuangPerpus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah judul buku"));
		row.appendChild(jumlahJudulBuku = new Decimalbox(new BigDecimal(
				perguruanTinggi.getJumlahJudulBuku() == null ? 0 : perguruanTinggi.getJumlahJudulBuku().intValue())));
		jumlahJudulBuku.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah eksemplar buku"));
		row.appendChild(
				jumlahEksemplarBuku = new Decimalbox(new BigDecimal(perguruanTinggi.getJumlahEksemplarBuku() == null ? 0
						: perguruanTinggi.getJumlahEksemplarBuku().intValue())));
		jumlahEksemplarBuku.setWidth("90%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		if (perguruanTinggi.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}
		return borderlayout;
	}

	public boolean onSave(Event event) throws Exception {

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
		if (perguruanTinggi.getId() != null) {
			perguruanTinggi = (PerguruanTinggi) session.load(PerguruanTinggi.class, perguruanTinggi.getId());
		}
		perguruanTinggi.setKepala((Pegawai) kepala.getAttribute("pegawai"));
		if (css != null)
			perguruanTinggi.setCss((String) (css.getSelectedItem() == null ? "" : css.getSelectedItem().getValue()));
		perguruanTinggi.setRektorNip(rektorNip.getValue());
		perguruanTinggi.setRektor(rektor.getValue());
		perguruanTinggi.setAkreditasi(akreditasi.getValue());
		perguruanTinggi.setNoSkAkreditasi(noSkAkreditasi.getValue());
		perguruanTinggi.setTanggalAkreditasi(tanggalAkreditasi.getValue());
		perguruanTinggi.setPeringkatAkreditasi(peringkatAkreditasi.getValue());

		perguruanTinggi.setWa(wa.getValue());
		perguruanTinggi.setPropinsi(propinsi.getValue());
		perguruanTinggi.setTahunPertamaMenerimaMahasiswa(tahunPertamaMenerimaMahasiswa.getValue());
		perguruanTinggi.setPejabatIzinOperasi(pejabatIzinOperasi.getValue());
		perguruanTinggi.setDusun(dusun.getValue());
		perguruanTinggi.setKelurahan(kelurahan.getValue());
		perguruanTinggi.setRt(rt.getValue());
		perguruanTinggi.setRw(rw.getValue());
		perguruanTinggi.setSkIzinOperasi(skIzinOperasi.getValue());
		perguruanTinggi.setTglSkIzinOperasi(tglSkIzinOperasi.getValue());
		perguruanTinggi.setNoRek(noRek.getValue());
		perguruanTinggi.setNmBank(nmBank.getValue());
		perguruanTinggi.setUnitCabang(unitCabang.getValue());
		perguruanTinggi.setNmRek(nmRek.getValue());
		perguruanTinggi.setLuasTanahMilik(luasTanahMilik.getValue());
		perguruanTinggi.setLuasTanahBukanMilik(luasTanahBukanMilik.getValue());
		perguruanTinggi
				.setStatus((String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue()));

		perguruanTinggi.setKodeYayasan(kodeYayasan.getValue());
		perguruanTinggi.setKodePerguruanTinggi(kodePerguruanTinggi.getValue());
		perguruanTinggi.setNama(nama.getValue());
		perguruanTinggi.setAlamat1(alamat1.getValue());
		perguruanTinggi.setAlamat2(alamat2.getValue());
		perguruanTinggi.setKota(kota.getValue());
		perguruanTinggi.setKodePos(kodePos.getValue() == null ? null : (kodePos.getValue().toString()));
		perguruanTinggi.setTelepon(telepon.getValue());
		perguruanTinggi.setFaksimili(faksimili.getValue());
		perguruanTinggi.setTanggalAkta(tanggalAkta.getValue());
		perguruanTinggi.setTanggalAwalPendirian(tanggalAwalPendirian.getValue());
		perguruanTinggi.setNomorAkta(nomorAkta.getValue() == null ? null : (nomorAkta.getValue().toString()));
		perguruanTinggi.setEmail(email.getValue());
		perguruanTinggi.setWebsite(website.getValue());

		perguruanTinggi.setLuasTanahTotal(luasTanahTotal.getValue().doubleValue());
		perguruanTinggi.setLuasKebunLahanPercobaanTotal(luasKebunLahanPercobaanTotal.getValue().doubleValue());
		perguruanTinggi.setLuasTotalRuangKuliah(luasTotalRuangKuliah.getValue().doubleValue());
		perguruanTinggi.setJumlahRuangKuliah(jumlahRuangKuliah.getValue().intValue());
		perguruanTinggi.setLuasTotalLabStudio(luasTotalLab.getValue().doubleValue());
		perguruanTinggi.setJumlahRuangLab(jumlahRuangLab.getValue().intValue());
		perguruanTinggi.setLuasTotalRuangDosenTetap(luasTotalRuangDosenTetap.getValue().doubleValue());
		perguruanTinggi.setLuasTotalRuangAdministrasi(luasTotalRuangAdministrasi.getValue().doubleValue());
		perguruanTinggi.setLuasTotalRuangSeminar(luasTotalRuangSeminar.getValue().doubleValue());
		perguruanTinggi.setLuasTotalRuangEkskul(luasTotalRuangEkskul.getValue().doubleValue());
		perguruanTinggi.setLuasTotalPusatKomputer(luasTotalRuangPuskom.getValue().doubleValue());
		perguruanTinggi.setLuasTotalRuangPerpustakaan(luasTotalRuangPerpus.getValue().doubleValue());
		perguruanTinggi.setJumlahJudulBuku(jumlahJudulBuku.getValue().intValue());
		perguruanTinggi.setJumlahEksemplarBuku(jumlahEksemplarBuku.getValue().intValue());

		perguruanTinggi.setDeskripsi(deskripsi.getValue());
		perguruanTinggi.setFeeder(feeder.getValue().trim());

		perguruanTinggi.setDomain(domain.getValue().trim());
		perguruanTinggi.setMotto(motto.getValue().trim());
		perguruanTinggi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		perguruanTinggi.setKodeSinta(kodeSinta.getValue().trim());
		perguruanTinggi.setDosenHarusPakaiSatuanKerja(dosenHarusPakaiSatuanKerja.isChecked());

		if (piilhanTampilanCb != null && piilhanTampilanCb.getSelectedItem() != null) {
			perguruanTinggi.setPiilhanTampilan((String) piilhanTampilanCb.getSelectedItem().getValue());
		}

		perguruanTinggi.setWakil1((Pegawai) wakil1.getAttribute("pegawai"));
		perguruanTinggi.setWakil2((Pegawai) wakil2.getAttribute("pegawai"));
		perguruanTinggi.setWakil3((Pegawai) wakil3.getAttribute("pegawai"));
		perguruanTinggi.setPejabat1((Pegawai) pegawai1.getAttribute("pegawai"));
		perguruanTinggi.setPejabat2((Pegawai) pegawai2.getAttribute("pegawai"));
		perguruanTinggi.setPejabat3((Pegawai) pegawai3.getAttribute("pegawai"));
		perguruanTinggi.setPejabat4((Pegawai) pegawai4.getAttribute("pegawai"));
		perguruanTinggi.setPejabat5((Pegawai) pegawai5.getAttribute("pegawai"));

		perguruanTinggi.setLabelPejabat1(labelPejabat1.getValue());
		perguruanTinggi.setLabelPejabat2(labelPejabat2.getValue());
		perguruanTinggi.setLabelPejabat3(labelPejabat3.getValue());
		perguruanTinggi.setLabelPejabat4(labelPejabat4.getValue());
		perguruanTinggi.setLabelPejabat5(labelPejabat5.getValue());

		perguruanTinggi.setHeaderpmb(headerpmb.getValue());

		Common.refreshSaveOrUpdate(session, perguruanTinggi);

		if (kop != null && kop.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopBawah != null && kopBawah.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopBawah);
				kopBawah.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(kopBawah);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopStempel != null && kopStempel.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopStempel);
				kopStempel.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(kopStempel);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kop_ppdb != null && kop_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop_ppdb);
				kop_ppdb.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(kop_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bg_ppdb != null && bg_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bg_ppdb);
				bg_ppdb.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(bg_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bg_pt != null && bg_pt.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bg_pt);
				bg_pt.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(bg_pt);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (footer_ppdb != null && footer_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(footer_ppdb);
				footer_ppdb.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(footer_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (logo != null && logo.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(logo);
				logo.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(logo);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (background != null && background.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(background);
				background.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(background);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (backgroundLogin != null && backgroundLogin.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(backgroundLogin);
				backgroundLogin.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(backgroundLogin);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (banner != null && banner.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(banner);
				banner.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(banner);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bannerMobile != null && bannerMobile.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bannerMobile);
				bannerMobile.setRef(perguruanTinggi.getId());

				session.getTransaction().begin();
				session.update(bannerMobile);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PerguruanTinggiAction.reInitByDomain();
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		// Super admin yang mencentang "Abaikan Domain" -> tampilkan SEMUA PT tanpa
		// dibatasi domain/URL aktif. Cek hak akses diulang di sini (server-side).
		boolean abaikan = abaikanDomain != null && abaikanDomain.isChecked()
				&& Common.getApakahAdminLain(tbmuser);

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PerguruanTinggi.class)
				.add(abaikan ? Restrictions.sqlRestriction("true")
						: (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
								? Restrictions.idEq(selectedPerguruanTinggi.getId())
								: Restrictions.sqlRestriction("true")));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchalamat.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("alamat1", searchalamat.getValue(), MatchMode.ANYWHERE))
				.add(searchkodeyayasan.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kodeYayasan", searchkodeyayasan.getValue(), MatchMode.ANYWHERE))
				.add(searchkodepergururantinggi.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kodePerguruanTinggi", searchkodepergururantinggi.getValue(),
								MatchMode.ANYWHERE))
				.add(searchkota.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kota", searchkota.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PerguruanTinggi> perguruanTinggi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(perguruanTinggi);
		grid.setRowRenderer(new PerguruanTinggiRenderer());
		grid.setModelCheckMobile(strset);

	}
}
