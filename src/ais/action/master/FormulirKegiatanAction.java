package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AktifitasFormulirKegiatanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataPerkuliahanBandbox;
import ais.action.master.helper.DetailperkuliahanForPenilaianHelper;
import ais.action.master.helper.FormulirKegiatanPesertaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.report.format1.akademik.LaporanFormulirKegiatan;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.GrupFormulirKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Sertifikat;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class FormulirKegiatanAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchfakultas;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private MyColumnConfig colfak;
	private MyColumnConfig colprod;
	private Combobox searchta;
	private Combobox searchsmt;
	private Textbox searchpeserta;

	private Textbox nama;
	private Textbox ttdKananOleh;
	private Textbox ttdKiriOleh;
	private Textbox ttdKananNama;
	private Textbox ttdKiriNama;
	private Textbox ttdKananNip;
	private Textbox ttdKiriNip;
	private Textbox keterangan;
	private Textbox kodeItemBiaya;
	private Textbox hanyaUntukAngkatan;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private MyCheckboxConfig harusBayarLunasSmtSaatIni;
	private MyCheckboxConfig harusBayarLunasSmtLalu;

	private boolean edit = true;
	private boolean delete = true;

	private FormulirKegiatan formulirKegiatan;
	private MyToolbarbuttonConfig add;
	private Combobox fakultas;
	private Combobox jurusan;

	private Combobox yayasan;
	private Combobox sekolah;

	private Textbox namaPembicara1;
	private Textbox jabatanPembicara1;
	private Textbox namaPembicara2;
	private Textbox jabatanPembicara2;
	private Textbox namaPembicara3;
	private Textbox jabatanPembicara3;
	private MyDatebox tanggal;
	private MyDatebox tanggalsampai;
	private Combobox program;
	private Combobox sertifikatCombo;
	private Tabpanel grupFormulirKegiatan;
	private Tabpanel sertifikat;
	private Intbox kuota;
	private Combobox kegiatanKemahasiswaan;

	private Mahasiswa mhs = null;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private MyCheckboxConfig pesertaMahasiswa;
	private MyCheckboxConfig pesertaDosen;
	private Dosen dosen = null;
	private Combobox kegiatanKedosenan;
	private MyCheckboxConfig sekaliBayar;
	private Grid gabungan;
	private Textbox jenisKegiatan;
	private MyTimebox waktumulai;
	private MyTimebox waktusampai;
	private Textbox tipeKegiatan;
	private Textbox namaEn;
	private Combobox jenisAktfitasMahasiswa;
	private Tbmuser tbmuser;
	private Textbox alamat;
	private Textbox noSk;
	private MyDatebox tglSk;

	private Row untukPt;
	private Row untukYa;
	private Siswa ssw;
	private Guru gr;
	private boolean pt;
	private boolean ya;
	private AmbilDataDosenBanbox dosenPembina;
	private AmbilDataGuruBanbox guruPembina;
	private AmbilDataPegawaiBanbox pegawaiPembina;
	private MyCheckboxConfig kodeItemBiayaMenggunakanAtau;
	private AmbilDataDosenBanbox dosenPembina2;
	private AmbilDataDosenBanbox dosenPembina3;
	private AmbilDataGuruBanbox guruPembina2;
	private AmbilDataGuruBanbox guruPembina3;

	private boolean loginSebagaiPesertaAtauPengajar() {
		return tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
	}

	public void onGrupFormulirKegiatan(Event event) {
		if (grupFormulirKegiatan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupFormulirKegiatan);
			MyInclude iframe = new MyInclude("/pages/master/grup_formulir_kegiatan.zul");
			iframe.setParent(window);
		}
	}

	public void onSertifikat(Event event) {
		if (sertifikat.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sertifikat);
			MyInclude iframe = new MyInclude("/pages/master/sertifikat.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel jenisFormulirKegiatan;
	private Combobox jenisLaporanKegiatan;
	protected LampiranLain ttdKiri;
	protected LampiranLain ttdKanan;
	private AmbilDataPegawaiBanbox pegawaiPembina2;
	private AmbilDataPegawaiBanbox pegawaiPembina3;
	private Combobox grupLaporanKegiatan;
	private Combobox syaratUjian;
	private Row syaratInfo = null;
	private Textbox namaKetuaPanitia;
	private Textbox namaWakilKetuaPanitia;

	public void onJenisFormulirKegiatan(Event event) {
		if (jenisFormulirKegiatan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisFormulirKegiatan);
			MyInclude iframe = new MyInclude("/pages/master/jenis_formulir_kegiatan.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.generateTahunAjaranDanSemua(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		if (searchsmt != null) { searchsmt.setSelectedItem(comboitem); }

//		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (untukPt != null)
			if (untukPt != null) { untukPt.setVisible(pt); }

		if (untukYa != null)
			if (untukYa != null) { untukYa.setVisible(ya); }

		if (ya) {
			if (colfak != null)
				colfak.setLabel("Yayasan");

			if (colprod != null)
				colprod.setLabel("Sekolah");
		}

		if (pt) {
			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		} else if (ya && searchyayasan != null) {
			Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		}
		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("dosen") != null) {
			dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
		} else if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
		} else if (execution.getParameter("siswa") != null) {
			ssw = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("siswa")))).uniqueResult();
		} else if (execution.getParameter("guru") != null) {
			gr = (Guru) HibernateUtil.currentSession().createCriteria(Guru.class).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("guru")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		}
		boolean boleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null;

		if (add != null) { add.setVisible(boleh && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = boleh && CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = boleh && CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		MyToolbarbuttonConfig ajukan = new MyToolbarbuttonConfig("Pengajuan Ikut Kegiatan",
				"/img/Apps-Google-Drive-Forms-icon.png");
		ajukan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus();
				laporan.setTitle("Pengajuan Form Kegiatan");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporan.onModal();
			}
		});
		if (ajukan != null) { ajukan.setParent(add.getParent()); }

		if (mhs != null || dosen != null) {
			Common.selectComboItem(searchfakultas, null);
			Common.selectComboItem(searchjurusan, null);
			searchfakultas.setDisabled(false);
			searchjurusan.setDisabled(false);
		} else if ((ssw != null || gr != null) && searchyayasan != null) {
			Common.selectComboItem(searchyayasan, null);
			Common.selectComboItem(searchsekolah, null);
			searchyayasan.setDisabled(false);
			searchsekolah.setDisabled(false);
		}

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

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
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

													List<FormulirKegiatan> tbmusers = ConstantValues
															.simpleList(initCriteria(true), FormulirKegiatan.class);
													int size = tbmusers.size();
													int index = 1;
													for (GeneralValueObject arg1 : tbmusers) {

														FormulirKegiatanPeserta formulirKegiatanPeserta;
														if (arg1 instanceof FormulirKegiatanPeserta) {
															formulirKegiatanPeserta = (FormulirKegiatanPeserta) arg1;
														} else {
															formulirKegiatanPeserta = null;
														}
														FormulirKegiatan formulirKegiatan = formulirKegiatanPeserta == null
																? (FormulirKegiatan) arg1
																: formulirKegiatanPeserta.getFormulirKegiatan();

														myLabelProsesDetail.setValue("Memproses "
																+ formulirKegiatan.getNama() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.aktivitasMahasiswaForm(formulirKegiatan,
																errorLog);

													}
													tbmusers.clear();
													tbmusers = null;

													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port) hanya dicatat ke log admin lalu progres
													// diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengiriman data Formulir Kegiatan (aktivitas mahasiswa) ke Neo Feeder",
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
	        FilterLanjutHelper.setup(comp);
}

	private static AktifitasFormulirKegiatanHelper aktifitasFormulirKegiatanHelper = new AktifitasFormulirKegiatanHelper();

	class FormulirKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormulirKegiatanPeserta formulirKegiatanPeserta;
			if (arg1 instanceof FormulirKegiatanPeserta) {
				formulirKegiatanPeserta = (FormulirKegiatanPeserta) arg1;
			} else {
				formulirKegiatanPeserta = null;
			}
			final FormulirKegiatan formulirKegiatan = formulirKegiatanPeserta == null ? (FormulirKegiatan) arg1
					: formulirKegiatanPeserta.getFormulirKegiatan();

			final MyDetail detail = new MyDetail();
			final EventListener detailEventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {

						if (formulirKegiatanPeserta != null) {
							ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
							groupbox.setStyle("min-height: 10000px;");
							aktifitasFormulirKegiatanHelper.initDetail(formulirKegiatan, groupbox);
							detail.appendChild(groupbox);
						} else {

							List<Perkuliahan> perkuliahans = formulirKegiatan.ambilDataPerkuliahans();
							if (!perkuliahans.isEmpty()) {

								ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(detail, "100%", new int[] { 0 });

								{ org.zkoss.zul.Div panelAgenda = btnTab.tambahTab(0, "Agenda Kegiatan", "/img/svg/calendar-check.svg");
								  panelAgenda.setStyle("min-height: 10000px;");
								  ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								  groupbox.setStyle("min-height: 10000px;");
								  aktifitasFormulirKegiatanHelper.initDetail(formulirKegiatan, groupbox);
								  panelAgenda.appendChild(groupbox); }

								final int[] idx = {1};
								for (final Perkuliahan perkuliahan : perkuliahans) {
									final int myIdx = idx[0]++;
									btnTab.tambahTabLazy(myIdx, perkuliahan.getSemester() + " " + perkuliahan.getKelas(), "/img/svg/chalkboard-teacher-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
										@Override
										public void muat(org.zkoss.zul.Div panel) throws Exception {
											Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
											Boolean aktifPenilaian = Common.checkApakahDosenBolehMenilai(dosen,
													tbmuser, perkuliahan.getTahunAjaran(),
													perkuliahan.getStatusSemesterPendek() != null ? Perkuliahan.SP
															: perkuliahan.getGanjilGenap());
											System.out.println("aktifPenilaian = " + aktifPenilaian);
											DetailperkuliahanForPenilaianHelper detailperkuliahanHelper = new DetailperkuliahanForPenilaianHelper(
													edit);
											detailperkuliahanHelper.display(perkuliahan, panel,
													new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
														}
													}, null, aktifPenilaian);
										}
									});
								}

							} else {

								ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(detail, "100%", new int[] { 0 });

								{ org.zkoss.zul.Div panelPeserta = btnTab.tambahTab(0, "Peserta Kegiatan", "/img/svg/users.svg");
								  int tinggi = 14;
								  panelPeserta.setStyle("min-height: 200px;");
								  panelPeserta.setHeight((100 + (90 * tinggi)) + "px");
								  FormulirKegiatanPesertaHelper detailperkuliahanHelper = new FormulirKegiatanPesertaHelper();
								  detailperkuliahanHelper.display(formulirKegiatan,
										formulirKegiatan.getGrupFormulirKegiatan(), panelPeserta, addWindow); }

								btnTab.tambahTabLazy(1, "Agenda Kegiatan", "/img/svg/calendar-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
									@Override
									public void muat(org.zkoss.zul.Div panel) throws Exception {
										panel.setStyle("min-height: 10000px;");
										ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
										groupbox.setStyle("min-height: 10000px;");
										aktifitasFormulirKegiatanHelper.initDetail(formulirKegiatan, groupbox);
										panel.appendChild(groupbox);
									}
								});
							}

						}
					}
				}
			};

			detail.setParent(arg0);
			detail.addEventListener("onOpen", detailEventListener);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(FormulirKegiatan.class, formulirKegiatan, formulirKegiatan.getNama()))
					.setParent(vbox);

			if (pt) {
				new Label(formulirKegiatan.getJenisAktfitasMahasiswa() == null ? ""
						: formulirKegiatan.getJenisAktfitasMahasiswa().getNama()).setParent(a);
				new Label(formulirKegiatan.getJenisAktfitasMahasiswa() == null ? ""
						: formulirKegiatan.getJenisAktfitasMahasiswa().getKampusMerderka() ? "Kampus Merdeka:YA"
								: "Kampus Merdeka:TIDAK")
						.setParent(a);
			}

			if (!formulirKegiatan.getNamaEn().isEmpty()) {
				new Label(formulirKegiatan.getNamaEn()).setParent(a);
			}

			if (!formulirKegiatan.getJenisKegiatan().isEmpty()) {
				new MyLabelKecilBold(formulirKegiatan.getJenisKegiatan()).setParent(a);
			}

			if (!formulirKegiatan.getTipeKegiatan().isEmpty()) {
				new MyLabelKecilBold(formulirKegiatan.getTipeKegiatan()).setParent(a);
			}

			if (formulirKegiatanPeserta != null) {
				Hbox hbox = new Hbox();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kartu Peserta", "/img/print.png");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						List<FormulirKegiatanPeserta> formulirKegiatanPesertas = new ArrayList<FormulirKegiatanPeserta>();
						formulirKegiatanPesertas.add(formulirKegiatanPeserta);
						SertifikatAction.cetakFormPendafatranKegiatan(formulirKegiatan, formulirKegiatanPesertas);
					}

				});
				button.setParent(hbox);

				Hbox hbox1 = new Hbox();
				hbox1.setParent(hbox);

				LampiranLain.createDownloadUploadFileLain(hbox1, formulirKegiatanPeserta.getId(),
						FormulirKegiatanPeserta.class.getName(), "Bukti Peserta", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				ais.ui.util.MenuAksiBaris.pasang(hbox);
				hbox.setParent(vbox);
			}

			new MyLabelKecil((formulirKegiatan.getKegiatanKemahasiswaan() == null ? ""
					: formulirKegiatan.getKegiatanKemahasiswaan().getNama())
					+ (formulirKegiatan.getKegiatanKedosenan() == null ? ""
							: " " + formulirKegiatan.getKegiatanKedosenan().getNama()))
					.setParent(a);
			new MyLabelKecil(formulirKegiatan.getKeterangan()).setParent(a);

			new MyLabelKecil(
					(formulirKegiatan.getMulai() == null ? "" : Common.dateFormat11.get().format(formulirKegiatan.getMulai()))
							+ (formulirKegiatan.getSampai() == null ? ""
									: " s.d " + Common.dateFormat11.get().format(formulirKegiatan.getSampai())))
					.setParent(vbox);
			new MyLabelKecil(formulirKegiatan.getHanyaUntukAngkatan()).setParent(vbox);

			if (formulirKegiatan.getDosenPembina() != null) {
				new MyLabelKecil(formulirKegiatan.getDosenPembina().getNama()).setParent(vbox);
			}
			if (formulirKegiatan.getGuruPembina() != null) {
				new MyLabelKecil(formulirKegiatan.getGuruPembina().getNama()).setParent(vbox);
			}
			if (formulirKegiatan.getPegawaiPembina() != null) {
				new MyLabelKecil(formulirKegiatan.getPegawaiPembina().getNama()).setParent(vbox);
			}

			new MyLabelKecil(formulirKegiatan.getJabatanPembicara1()).setParent(vbox);
			new MyLabelKecil(formulirKegiatan.getJabatanPembicara2()).setParent(vbox);
			new MyLabelKecil(formulirKegiatan.getJabatanPembicara3()).setParent(vbox);

			if (formulirKegiatan.getJenisFormulirKegiatan() != null) {
				new MyLabelKecil(formulirKegiatan.getJenisFormulirKegiatan().getNama()).setParent(vbox);
			}

			if (formulirKegiatan.getGrupFormulirKegiatan() != null) {
				new MyLabelKecil(formulirKegiatan.getGrupFormulirKegiatan().getNama()).setParent(vbox);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label("Kanan:" + formulirKegiatan.getTtdKananOleh()).setParent(vbox);
			new Label("Kiri:" + formulirKegiatan.getTtdKiriOleh()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label("Kanan:" + formulirKegiatan.getTtdKananNama()).setParent(vbox);
			new Label("Kiri:" + formulirKegiatan.getTtdKiriNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label("Kanan:" + formulirKegiatan.getTtdKananNip()).setParent(vbox);
			new Label("Kiri:" + formulirKegiatan.getTtdKiriNip()).setParent(vbox);

			if (pt) {
				new Label(formulirKegiatan.getFakultas() == null ? "Semua" : formulirKegiatan.getFakultas().getNama())
						.setParent(arg0);
				new Label(formulirKegiatan.getJurusan() == null ? "Semua" : formulirKegiatan.getJurusan().getNama())
						.setParent(arg0);
			} else if (ya) {
				new Label(formulirKegiatan.getYayasan() == null ? "Semua" : formulirKegiatan.getYayasan().getNama())
						.setParent(arg0);
				new Label(formulirKegiatan.getSekolah() == null ? "Semua" : formulirKegiatan.getSekolah().getNama())
						.setParent(arg0);
			} else {
				new Label().setParent(arg0);
				new Label().setParent(arg0);
			}

			new Label(formulirKegiatan.getProgram() == null ? "Semua" : formulirKegiatan.getProgram()).setParent(arg0);
			new Label(formulirKegiatan.getSertifikat() == null ? "" : formulirKegiatan.getSertifikat().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(formulirKegiatan.getKuota())).setParent(arg0);
			Session session = HibernateUtil.currentSession();
			if (formulirKegiatanPeserta == null) {
				int jumlah = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
						.add(Restrictions.or(Restrictions.isNotNull("siswa"),
								Restrictions.or(Restrictions.isNotNull("guru"),
										Restrictions.or(Restrictions.isNotNull("mahasiswa"),
												Restrictions.isNotNull("dosen")))))
						.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				new Label(Common.numberFormat.get().format(jumlah)).setParent(arg0);
			}

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (formulirKegiatan.getSyaratUjian() != null) {
				new Label(formulirKegiatan.getSyaratUjian().getNama()).setParent(hbox);
			}
			if (!formulirKegiatan.getKodeItemBiaya().trim().isEmpty()) {

				for (String kode : formulirKegiatan.getKodeItemBiaya().trim().split(",")) {

					String[] spl = StringUtils.split(kode.trim(), ":");
					String code = spl.length > 0 ? spl[0] : "";
					String tahun = spl.length > 1 ? spl[1] : "";

					ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(session
							.createCriteria(ItemBiaya.class).add(Restrictions.eq("kode", code.trim())).setMaxResults(1),
							ItemBiaya.class);
					if (itemBiaya != null) {
						new Label(itemBiaya.getKode() + "-" + itemBiaya.getNama()
								+ (tahun.trim().isEmpty() ? "" : " khusus untuk tahun angkatan " + tahun))
								.setParent(hbox);
					}
				}
			} else {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada item biaya")).setParent(hbox);
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formulirKegiatan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formulirKegiatan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(formulirKegiatan);
				}
			});

			new Label(formulirKegiatanPeserta == null ? "" : formulirKegiatanPeserta.getNilai()).setParent(arg0);
			new Label(formulirKegiatanPeserta == null ? "" : formulirKegiatanPeserta.getKeterangan()).setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi. Label status (Acc / feeder) bukan
			// tombol aksi sehingga tetap ditampilkan langsung di sel, bukan di dalam kebab.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox tempCrud = Common.copyEditDeleteButtons(edit, edit, delete, formulirKegiatan,
					FormulirKegiatanAction.this, true);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(tempCrud));

			if (formulirKegiatan.getJenisFormulirKegiatan() != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
				button.setOrient("vertical");
				button.setStyle("font-size:9px;");
				button.setTooltiptext("Cetak");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								LaporanFormulirKegiatan.cetak(formulirKegiatan);
							}
						});
					}

				});
				aksiButtons.add(button);
			}

			if (formulirKegiatanPeserta != null) {
				vbox1.appendChild(new Label("Acc : " + (formulirKegiatanPeserta.getAcc() ? "YA" : "BELUM")));

				MyToolbarbuttonConfig cetakToolbarbutton = new MyToolbarbuttonConfig("Cetak Form", "/img/print.png");
				cetakToolbarbutton.setOrient("vertical");
				cetakToolbarbutton.setStyle("font-size:9px;");
				cetakToolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LaporanPendidikanLingkunganKampus lingkunganKampus = new LaporanPendidikanLingkunganKampus(
								formulirKegiatanPeserta);
						lingkunganKampus.setClosable(true);
						lingkunganKampus.setTitle("Formulir Kegiatan");
						lingkunganKampus.setWidth("90%");
						lingkunganKampus.setHeight("95%");
						lingkunganKampus.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						lingkunganKampus.onModal();
					}
				});
				aksiButtons.add(cetakToolbarbutton);

				if (formulirKegiatanPeserta.getAcc()
						&& formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat() != null) {
					MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Cetak Sertifikat",
							"/img/certificate-icon.png");
					cetakToolbarbuttonSertifikat.setOrient("vertical");
					cetakToolbarbuttonSertifikat.setStyle("font-size:9px;");
					cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							SertifikatAction.cetakSertifikat(formulirKegiatanPeserta);
						}
					});
					aksiButtons.add(cetakToolbarbuttonSertifikat);
				}
			}

			Hbox myHbox = new Hbox();
			/* UBAH 21-08-2026: status feeder dahulu menumpang di sel AKSI yang hanya selebar
			 * tombol kebab, sehingga labelnya terpenggal menurun satu kata per baris. Kini
			 * ditempelkan di bawah sel identitas baris. Bila sel itu tidak ditemukan,
			 * wadah lama tetap dipakai agar keterangannya tidak hilang. */
			org.zkoss.zul.Vbox wadahFeeder = ais.ui.util.UIHelper.selIdentitas(arg0);
			if (wadahFeeder == null) {
				wadahFeeder = vbox1;
			}
			myHbox.setParent(wadahFeeder);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (formulirKegiatan.getFeeder() != null && !formulirKegiatan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				aksiButtons.add(buttonTagihan);
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

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														myLabelProsesDetail
																.setValue("Mengirim data " + formulirKegiatan);

														feederImporter.aktivitasMahasiswaForm(formulirKegiatan,
																errorLog);

														// FIX "gagal diam-diam": sebelumnya exception di sini (mis.
														// gagal konek/parse port) hanya dicatat ke log admin lalu
														// progres diset "" (=SUKSES palsu) di luar try, menutupi
														// kegagalan.
														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																"pengiriman data Formulir Kegiatan \"" + formulirKegiatan.getNama()
																		+ "\" ke Neo Feeder",
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

			}

			ais.ui.util.UIHelper.buatBarisAksi(vbox1, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new FormulirKegiatan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		formulirKegiatan = (FormulirKegiatan) obj;
		init(formulirKegiatan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(FormulirKegiatan formulirKegiatan) throws Exception {
		this.formulirKegiatan = formulirKegiatan;
		addWindow.setTitle(formulirKegiatan.getId() == null ? "Tambah Formulir Kegiatan" : "Ubah Formulir Kegiatan");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama / Judul Kegiatan *"));
		row.appendChild(nama = new Textbox(formulirKegiatan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama / Judul Kegiatan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(formulirKegiatan.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina Kegiatan I"));
		row.appendChild(dosenPembina = new AmbilDataDosenBanbox());
		dosenPembina.setAttribute("myValue", formulirKegiatan.getDosenPembina());
		dosenPembina.setAttribute("dosen", formulirKegiatan.getDosenPembina());
		dosenPembina.setValue(
				formulirKegiatan.getDosenPembina() == null ? "" : formulirKegiatan.getDosenPembina().getNama());
		dosenPembina.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina Kegiatan II"));
		row.appendChild(dosenPembina2 = new AmbilDataDosenBanbox());
		dosenPembina2.setAttribute("myValue", formulirKegiatan.getDosenPembina2());
		dosenPembina2.setAttribute("dosen", formulirKegiatan.getDosenPembina2());
		dosenPembina2.setValue(
				formulirKegiatan.getDosenPembina2() == null ? "" : formulirKegiatan.getDosenPembina2().getNama());
		dosenPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina Kegiatan III"));
		row.appendChild(dosenPembina3 = new AmbilDataDosenBanbox());
		dosenPembina3.setAttribute("myValue", formulirKegiatan.getDosenPembina3());
		dosenPembina3.setAttribute("dosen", formulirKegiatan.getDosenPembina3());
		dosenPembina3.setValue(
				formulirKegiatan.getDosenPembina3() == null ? "" : formulirKegiatan.getDosenPembina3().getNama());
		dosenPembina3.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembina Kegiatan I"));
		row.appendChild(guruPembina = new AmbilDataGuruBanbox());
		guruPembina.setAttribute("myValue", formulirKegiatan.getGuruPembina());
		guruPembina.setAttribute("guru", formulirKegiatan.getGuruPembina());
		guruPembina
				.setValue(formulirKegiatan.getGuruPembina() == null ? "" : formulirKegiatan.getGuruPembina().getNama());
		guruPembina.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembina Kegiatan II"));
		row.appendChild(guruPembina2 = new AmbilDataGuruBanbox());
		guruPembina2.setAttribute("myValue", formulirKegiatan.getGuruPembina2());
		guruPembina2.setAttribute("guru", formulirKegiatan.getGuruPembina2());
		guruPembina2.setValue(
				formulirKegiatan.getGuruPembina2() == null ? "" : formulirKegiatan.getGuruPembina2().getNama());
		guruPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembina Kegiatan III"));
		row.appendChild(guruPembina3 = new AmbilDataGuruBanbox());
		guruPembina3.setAttribute("myValue", formulirKegiatan.getGuruPembina3());
		guruPembina3.setAttribute("guru", formulirKegiatan.getGuruPembina3());
		guruPembina3.setValue(
				formulirKegiatan.getGuruPembina3() == null ? "" : formulirKegiatan.getGuruPembina3().getNama());
		guruPembina3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tenaga Kependidikan Pembina Kegiatan I"));
		row.appendChild(pegawaiPembina = new AmbilDataPegawaiBanbox());
		pegawaiPembina.setAttribute("myValue", formulirKegiatan.getPegawaiPembina());
		pegawaiPembina.setAttribute("pegawai", formulirKegiatan.getPegawaiPembina());
		pegawaiPembina.setValue(
				formulirKegiatan.getPegawaiPembina() == null ? "" : formulirKegiatan.getPegawaiPembina().getNama());
		pegawaiPembina.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tenaga Kependidikan Pembina Kegiatan II"));
		row.appendChild(pegawaiPembina2 = new AmbilDataPegawaiBanbox());
		pegawaiPembina2.setAttribute("myValue", formulirKegiatan.getPegawaiPembina2());
		pegawaiPembina2.setAttribute("pegawai", formulirKegiatan.getPegawaiPembina2());
		pegawaiPembina2.setValue(
				formulirKegiatan.getPegawaiPembina2() == null ? "" : formulirKegiatan.getPegawaiPembina2().getNama());
		pegawaiPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tenaga Kependidikan Pembina Kegiatan III"));
		row.appendChild(pegawaiPembina3 = new AmbilDataPegawaiBanbox());
		pegawaiPembina3.setAttribute("myValue", formulirKegiatan.getPegawaiPembina3());
		pegawaiPembina3.setAttribute("pegawai", formulirKegiatan.getPegawaiPembina3());
		pegawaiPembina3.setValue(
				formulirKegiatan.getPegawaiPembina3() == null ? "" : formulirKegiatan.getPegawaiPembina3().getNama());
		pegawaiPembina3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan"));
		row.appendChild(jenisKegiatan = new Textbox(formulirKegiatan.getJenisKegiatan()));
		jenisKegiatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Kegiatan"));
		row.appendChild(tipeKegiatan = new Textbox(formulirKegiatan.getTipeKegiatan()));
		tipeKegiatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Laporan Kegiatan"));
		row.appendChild(jenisLaporanKegiatan = new Combobox());
		Common.insertComboDanSemua(jenisLaporanKegiatan, new String[] { "nama" }, "keterangan",
				JenisFormulirKegiatan.class, "=Tidak ada laporan kegiatan=", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisLaporanKegiatan, formulirKegiatan.getJenisFormulirKegiatan());
		jenisLaporanKegiatan.setWidth("90%");
		jenisLaporanKegiatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Kegiatan"));
		row.appendChild(grupLaporanKegiatan = new Combobox());
		Common.insertComboDanSemua(grupLaporanKegiatan, new String[] { "nama" }, "keterangan",
				GrupFormulirKegiatan.class, "=Tidak ada grup kegiatan=", Restrictions.eq("aktif", true));
		Common.selectComboItem(grupLaporanKegiatan, formulirKegiatan.getGrupFormulirKegiatan());
		grupLaporanKegiatan.setWidth("90%");
		grupLaporanKegiatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pembicara / Tenaga Ahli / Pakar I"));
		row.appendChild(namaPembicara1 = new Textbox(formulirKegiatan.getNamaPembicara1()));
		namaPembicara1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan / Instansi Pembicara I"));
		row.appendChild(jabatanPembicara1 = new Textbox(formulirKegiatan.getJabatanPembicara1()));
		jabatanPembicara1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pembicara / Tenaga Ahli / Pakar II"));
		row.appendChild(namaPembicara2 = new Textbox(formulirKegiatan.getNamaPembicara2()));
		namaPembicara2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan / Instansi Pembicara II"));
		row.appendChild(jabatanPembicara2 = new Textbox(formulirKegiatan.getJabatanPembicara2()));
		jabatanPembicara2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pembicara / Tenaga Ahli / Pakar III"));
		row.appendChild(namaPembicara3 = new Textbox(formulirKegiatan.getNamaPembicara3()));
		namaPembicara3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan / Instansi Pembicara III"));
		row.appendChild(jabatanPembicara3 = new Textbox(formulirKegiatan.getJabatanPembicara3()));
		jabatanPembicara3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ketua Panitia"));
		row.appendChild(namaKetuaPanitia = new Textbox(formulirKegiatan.getNamaKetuaPanitia()));
		namaKetuaPanitia.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Wakil Ketua Panitia"));
		row.appendChild(namaWakilKetuaPanitia = new Textbox(formulirKegiatan.getNamaWakilKetuaPanitia()));
		namaWakilKetuaPanitia.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(pesertaMahasiswa = new MyCheckboxConfig("Peserta Mahasiswa"));
		pesertaMahasiswa.setChecked(formulirKegiatan.getPesertaMahasiswa());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(pesertaDosen = new MyCheckboxConfig("Peserta Dosen"));
		pesertaDosen.setChecked(formulirKegiatan.getPesertaDosen());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, formulirKegiatan.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Pendaftaran *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(formulirKegiatan.getMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai = new MyDatebox(formulirKegiatan.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pelaksanaan *"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tanggal = new MyDatebox(formulirKegiatan.getTanggal()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(tanggalsampai = new MyDatebox(formulirKegiatan.getTanggalsampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Pelaksanaan"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(waktumulai = new MyTimebox(formulirKegiatan.getWaktumulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(waktusampai = new MyTimebox(formulirKegiatan.getWaktusampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi / Alamat Kegiatan"));
		row.appendChild(alamat = new Textbox(formulirKegiatan.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK Kegiatan"));
		row.appendChild(noSk = new Textbox(formulirKegiatan.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Kegiatan"));
		row.appendChild(tglSk = new MyDatebox(formulirKegiatan.getTglSk()));

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (formulirKegiatan.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			formulirKegiatan.setFakultas(tbmuser.ambilFakultas());
		}
		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		if (formulirKegiatan.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			formulirKegiatan.setYayasan(tbmuser.ambilYayasan());
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, formulirKegiatan.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, formulirKegiatan.getJurusan());

		if (formulirKegiatan.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.getMahasiswa().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, formulirKegiatan.getYayasan());
		yayasan.setWidth("90%");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, formulirKegiatan.getSekolah());

		if (formulirKegiatan.getSekolah() == null) {
			if (tbmuser.ambilSekolah() != null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null)) {
				Common.pilihSekolah(sekolah,
						tbmuser == null || tbmuser.ambilSekolah() == null ? tbmuser.getSiswa().getSekolah()
								: tbmuser.ambilSekolah());
				sekolah.setDisabled(true);
			} else {
				sekolah.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk tahun angkatan"));
		row.appendChild(hanyaUntukAngkatan = new Textbox(formulirKegiatan.getHanyaUntukAngkatan()));
		hanyaUntukAngkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"Jika pengumuman ini khusus suatu tahun angkatan tertentu, masukkan tahun angkatan yang pisah menggunakan tanda koma (,). Misal : 2017,2018");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program = new Combobox());
		program.setWidth("90%");
		Common.initPrograms(program);
		Common.selectComboItem(program, formulirKegiatan.getProgram());
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota Peserta"));
		row.appendChild(kuota = new Intbox(formulirKegiatan.getKuota()));

		Common.initKeterangan(rows,
				"Kuota Peserta hanya berlaku untuk pendaftaran dari sisi mahasiswa, admin tetap bisa memasukkan tambahan peserta jika diangap perlu meskipun melebihi kuota yang ditentukan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikatCombo = new Combobox());
		Common.insertComboDanSemua(sertifikatCombo, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikatCombo, formulirKegiatan.getSertifikat());
		sertifikatCombo.setWidth("90%");
		sertifikatCombo.setReadonly(true);

		Common.initKeterangan(rows, "Jika form kegiatan ini tanpa sertifikat, pilih tanpa sertifikat.");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan Kemahasiswaan"));
		row.appendChild(kegiatanKemahasiswaan = new Combobox());
		String[] kolomKegiatanKemahasiswaan = loginSebagaiPesertaAtauPengajar()
				? new String[] { "nama", "tempat", "detailKelompokKegiatanKemahasiswaan", "tahunAkademik",
						"jenisSemester" }
				: new String[] { "nama", "tempat", "detailKelompokKegiatanKemahasiswaan", "diajukanOleh",
						"tahunAkademik", "jenisSemester" };
		Common.insertComboDanSemua(kegiatanKemahasiswaan,
				kolomKegiatanKemahasiswaan,
				"keterangan", KegiatanKemahasiswaan.class, "== Tanpa Kegiatan Kemahasiswaan ==",
				Restrictions.eq("status", KegiatanKemahasiswaan.DISETUJUI));
		Common.selectComboItem(kegiatanKemahasiswaan, formulirKegiatan.getKegiatanKemahasiswaan());
		kegiatanKemahasiswaan.setWidth("90%");
		kegiatanKemahasiswaan.setReadonly(true);

		Common.initKeterangan(rows,
				"Jika form kegiatan ini tanpa Kegiatan Kemahasiswaan, pilih tanpa Kegiatan Kemahasiswaan.");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan Dosen"));
		row.appendChild(kegiatanKedosenan = new Combobox());
		String[] kolomKegiatanKedosenan = loginSebagaiPesertaAtauPengajar()
				? new String[] { "nama", "tempat", "detailKelompokKegiatanKedosenan", "tahunAkademik",
						"jenisSemester" }
				: new String[] { "nama", "tempat", "detailKelompokKegiatanKedosenan", "diajukanOleh",
						"tahunAkademik", "jenisSemester" };
		Common.insertComboDanSemua(kegiatanKedosenan,
				kolomKegiatanKedosenan,
				"keterangan", KegiatanKedosenan.class, "== Tanpa Kegiatan Dosen ==",
				Restrictions.eq("status", KegiatanKedosenan.DISETUJUI));
		Common.selectComboItem(kegiatanKedosenan, formulirKegiatan.getKegiatanKedosenan());
		kegiatanKedosenan.setWidth("90%");
		kegiatanKedosenan.setReadonly(true);

		Common.initKeterangan(rows, "Jika form kegiatan ini tanpa Kegiatan Dosen, pilih tanpa Kegiatan Dosen.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan Kanan oleh"));
		row.appendChild(ttdKananOleh = new Textbox(formulirKegiatan.getTtdKananOleh()));
		ttdKananOleh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan Kiri oleh"));
		row.appendChild(ttdKiriOleh = new Textbox(formulirKegiatan.getTtdKiriOleh()));
		ttdKiriOleh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Tanda Tangan Kanan"));
		row.appendChild(ttdKananNama = new Textbox(formulirKegiatan.getTtdKananNama()));
		ttdKananNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Tanda Tangan Kiri"));
		row.appendChild(ttdKiriNama = new Textbox(formulirKegiatan.getTtdKiriNama()));
		ttdKiriNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIP / Kode Tanda Tangan Kanan"));
		row.appendChild(ttdKananNip = new Textbox(formulirKegiatan.getTtdKananNip()));
		ttdKananNip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIP / Kode Tanda Tangan Kiri"));
		row.appendChild(ttdKiriNip = new Textbox(formulirKegiatan.getTtdKiriNip()));
		ttdKiriNip.setWidth("90%");

		ttdKiri = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan Kiri (PNG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, formulirKegiatan.getId(), LampiranLain.TTD_FORMULIR_KIRI,
				"Tanda Tangan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ttdKiri = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true, null, false, false);

		hbox.setParent(row);

		ttdKanan = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan Kanan (PNG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, formulirKegiatan.getId(), LampiranLain.TTD_FORMULIR_KANAN,
				"Tanda Tangan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ttdKanan = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true, null, false, false);

		hbox.setParent(row);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(formulirKegiatan.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		if (pt) {
			syaratInfo = Common.initKeterangan(rows,
					"Jika syarat mengikuti seminar harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa yang mengikuti seminar. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Jika khusus untuk tahun angkatan tertentu, berikan tanda :, contoh : 502:2024,505:2024,506.");
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(sekaliBayar = new MyCheckboxConfig(
				"Kode item biaya tersebut sekali bayar saja, jadi kalau misalnya mahasiswa membayar di semester 7, tetap bisa mengajukan di semester 8 atau lebih tanpa membayar ulang."));
		sekaliBayar.setChecked(formulirKegiatan.getSekaliBayar());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(kodeItemBiayaMenggunakanAtau = new MyCheckboxConfig(
				"Syarat kode item biaya menggunakan kondisi (ATAU)"));
		kodeItemBiayaMenggunakanAtau.setChecked(formulirKegiatan.getKodeItemBiayaMenggunakanAtau());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Syarat Mengikuti Kegiatan"));
		row.appendChild(syaratUjian = new Combobox());
		Common.insertComboDanSemua(syaratUjian, new String[] { "nama" }, "keterangan", SyaratUjian.class,
				"== Tanpa Syarat Mengikuti Kegiatan ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(syaratUjian, formulirKegiatan.getSyaratUjian(), true);
		syaratUjian.setWidth("90%");
		syaratUjian.setReadonly(true);

		EventListener syaratUjianEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SyaratUjian ujian = (SyaratUjian) (syaratUjian.getSelectedItem() == null ? null
						: syaratUjian.getSelectedItem().getValue());

				kodeItemBiayaMenggunakanAtau.getParent().setVisible(ujian == null && pt);
				kodeItemBiaya.getParent().setVisible(ujian == null && pt);
				sekaliBayar.getParent().setVisible(ujian == null && pt);

				if (syaratInfo != null) {
					syaratInfo.setVisible(ujian == null && pt);
				}

			}
		};

		syaratUjian.addEventListener("onChange", syaratUjianEventListener);
		syaratUjianEventListener.onEvent(null);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusBayarLunasSmtSaatIni = new MyCheckboxConfig("Harus Bayar Lunas Smt Saat Ini"));
		harusBayarLunasSmtSaatIni.setChecked(formulirKegiatan.getHarusBayarLunasSmtSaatIni());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusBayarLunasSmtLalu = new MyCheckboxConfig("Harus Bayar Lunas Smt Lalu"));
		harusBayarLunasSmtLalu.setChecked(formulirKegiatan.getHarusBayarLunasSmtLalu());

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, formulirKegiatan.getTahunAkademik());

		jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, formulirKegiatan.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gabungan Perkuliahan"));
		row.appendChild(gabungan = initReferensi(formulirKegiatan, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		}));

		if (pt) {
			Common.initKeterangan(rows,
					"* Jika form kegiatan ini merupakan gabungan dari perkulihaahan, pilih satu atau lebih data perkuliahan yang akan digabungkan");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				formulirKegiatan.getKeterangan() == null ? "" : formulirKegiatan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

	public static Grid initReferensi(final FormulirKegiatan formulirKegiatan, final EventListener eventListener)
			throws Exception {

		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumnRef = new MyColumnConfig();
		subcolumnRef.setParent(subcolumns);
		subcolumnRef.setWidth("90%");

		MyColumnConfig subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);

		final Rows subrowsRefs = new Rows();
		subrowsRefs.setParent(subGrid);
		for (Perkuliahan perkuliahan : formulirKegiatan.ambilDataPerkuliahans()) {
			addReferensi(perkuliahan, subrowsRefs, eventListener);
		}

		final AmbilDataPerkuliahanBandbox button = new AmbilDataPerkuliahanBandbox();
		button.setReadonly(true);
		button.setCols(20);
		button.setTooltiptext("Tambah / Gabungkan dengan Perkuliahan");
		button.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Perkuliahan perkuliahan = (Perkuliahan) button.getAttribute("perkuliahan");
				if (perkuliahan != null) {
					addReferensi(perkuliahan, subrowsRefs, eventListener);
				}
				button.setAttribute("perkuliahan", null);
				button.setValue("");
			}
		});
		button.setParent(subcolumnRef);

		return subGrid;
	}

	private static void addReferensi(final Perkuliahan perkuliahan, Rows subrowsRefs, final EventListener eventListener)
			throws Exception {

		final MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrowsRefs);
		subrow.setValign("top");
		subrow.setAttribute("perkuliahan", perkuliahan);

		subrow.appendChild(new MyLabelAgakKecilBold(perkuliahan.infoSimple()));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									subrow.detach();
								}

							}
						});

			}
		});
		button.setParent(subrow);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Formulir Kegiatan",
					"Kolom Nama Formulir Kegiatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Formulir Kegiatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (formulirKegiatan.getId() != null) {
			formulirKegiatan = (FormulirKegiatan) session.load(FormulirKegiatan.class, formulirKegiatan.getId());

		}

		formulirKegiatan.setNamaKetuaPanitia(namaKetuaPanitia.getValue());
		formulirKegiatan.setNamaWakilKetuaPanitia(namaWakilKetuaPanitia.getValue());

		formulirKegiatan.setSyaratUjian((SyaratUjian) (syaratUjian.getSelectedItem() == null ? null
				: syaratUjian.getSelectedItem().getValue()));
		formulirKegiatan.setWaktumulai(waktumulai.getValue());
		formulirKegiatan.setWaktusampai(waktusampai.getValue());
		formulirKegiatan.setPesertaDosen(pesertaDosen.isChecked());
		formulirKegiatan.setPesertaMahasiswa(pesertaMahasiswa.isChecked());
		formulirKegiatan.setNama(nama.getValue());
		formulirKegiatan.setNamaEn(namaEn.getValue());
		formulirKegiatan.setJenisKegiatan(jenisKegiatan.getValue());
		formulirKegiatan.setTipeKegiatan(tipeKegiatan.getValue());
		formulirKegiatan.setNamaPembicara1(namaPembicara1.getValue());
		formulirKegiatan.setNamaPembicara2(namaPembicara2.getValue());
		formulirKegiatan.setNamaPembicara3(namaPembicara3.getValue());
		formulirKegiatan.setJabatanPembicara1(jabatanPembicara1.getValue());
		formulirKegiatan.setJabatanPembicara2(jabatanPembicara2.getValue());
		formulirKegiatan.setJabatanPembicara3(jabatanPembicara3.getValue());
		formulirKegiatan.setMulai(mulai.getValue());
		formulirKegiatan.setSampai(sampai.getValue());
		formulirKegiatan.setKeterangan(keterangan.getValue());
		formulirKegiatan.setTtdKananOleh(ttdKananOleh.getValue());
		formulirKegiatan.setTtdKiriOleh(ttdKiriOleh.getValue());
		formulirKegiatan.setTtdKananNama(ttdKananNama.getValue());
		formulirKegiatan.setTtdKiriNama(ttdKiriNama.getValue());
		formulirKegiatan.setTtdKananNip(ttdKananNip.getValue());
		formulirKegiatan.setTtdKiriNip(ttdKiriNip.getValue());
		formulirKegiatan.setKodeItemBiaya(kodeItemBiaya.getValue().trim());
		formulirKegiatan.setHarusBayarLunasSmtLalu(harusBayarLunasSmtLalu.isChecked());
		formulirKegiatan.setHarusBayarLunasSmtSaatIni(harusBayarLunasSmtSaatIni.isChecked());
		formulirKegiatan.setHanyaUntukAngkatan(hanyaUntukAngkatan.getValue());
		formulirKegiatan.setTanggal(tanggal.getValue());
		formulirKegiatan.setTanggalsampai(tanggalsampai.getValue());
		formulirKegiatan.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		formulirKegiatan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		formulirKegiatan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		formulirKegiatan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		formulirKegiatan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));

		formulirKegiatan
				.setJenisFormulirKegiatan((JenisFormulirKegiatan) (jenisLaporanKegiatan.getSelectedItem() == null ? null
						: jenisLaporanKegiatan.getSelectedItem().getValue()));

		formulirKegiatan
				.setGrupFormulirKegiatan((GrupFormulirKegiatan) (grupLaporanKegiatan.getSelectedItem() == null ? null
						: grupLaporanKegiatan.getSelectedItem().getValue()));

		formulirKegiatan.setSertifikat((Sertifikat) (sertifikatCombo.getSelectedItem() == null ? null
				: sertifikatCombo.getSelectedItem().getValue()));
		formulirKegiatan.setKuota(kuota.getValue());
		formulirKegiatan.setKegiatanKemahasiswaan(
				(KegiatanKemahasiswaan) (kegiatanKemahasiswaan.getSelectedItem() == null ? null
						: kegiatanKemahasiswaan.getSelectedItem().getValue()));

		formulirKegiatan.setKegiatanKedosenan((KegiatanKedosenan) (kegiatanKedosenan.getSelectedItem() == null ? null
				: kegiatanKedosenan.getSelectedItem().getValue()));

		formulirKegiatan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		formulirKegiatan.setSemester((String) jenisSemester.getSelectedItem().getValue());
		formulirKegiatan.setPesertaMahasiswa(pesertaMahasiswa.isChecked());
		formulirKegiatan.setPesertaDosen(pesertaDosen.isChecked());
		formulirKegiatan.setSekaliBayar(sekaliBayar.isChecked());

		formulirKegiatan.setJenisAktfitasMahasiswa(
				(JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa.getSelectedItem() == null ? null
						: jenisAktfitasMahasiswa.getSelectedItem().getValue()));
		formulirKegiatan.setAlamat(alamat.getValue());
		formulirKegiatan.setNoSk(noSk.getValue());
		formulirKegiatan.setTglSk(tglSk.getValue());

		formulirKegiatan.setDosenPembina2((Dosen) dosenPembina2.getAttribute("dosen"));
		formulirKegiatan.setDosenPembina3((Dosen) dosenPembina3.getAttribute("dosen"));

		formulirKegiatan.setDosenPembina((Dosen) dosenPembina.getAttribute("dosen"));
		formulirKegiatan.setGuruPembina((Guru) guruPembina.getAttribute("guru"));

		formulirKegiatan.setGuruPembina2((Guru) guruPembina2.getAttribute("guru"));
		formulirKegiatan.setGuruPembina3((Guru) guruPembina3.getAttribute("guru"));

		formulirKegiatan.setPegawaiPembina((Pegawai) pegawaiPembina.getAttribute("pegawai"));

		formulirKegiatan.setPegawaiPembina2((Pegawai) pegawaiPembina2.getAttribute("pegawai"));
		formulirKegiatan.setPegawaiPembina3((Pegawai) pegawaiPembina3.getAttribute("pegawai"));

		formulirKegiatan.setKodeItemBiayaMenggunakanAtau(kodeItemBiayaMenggunakanAtau.isChecked());

		String hasil = "";
		try {
			List<Row> rows = gabungan.getRows().getChildren();
			for (Row row : rows) {
				Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("perkuliahan");
				if (perkuliahan != null) {
					hasil += hasil.isEmpty() ? perkuliahan.getId().toString() : "," + perkuliahan.getId().toString();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		formulirKegiatan.setPerkuliahans(hasil);

		if (formulirKegiatan.getId() != null) {
			session.update(formulirKegiatan);
		} else {
			session.save(formulirKegiatan);
		}
		session.flush();

		if (formulirKegiatan.getCopyDari() != null && formulirKegiatan.getCopyDari().getId() != null) {
			FormulirKegiatan copyDari = (FormulirKegiatan) formulirKegiatan.getCopyDari();
			List<FormulirKegiatanPeserta> formulirKegiatanPesertas = session
					.createCriteria(FormulirKegiatanPeserta.class).add(Restrictions.eq("formulirKegiatan", copyDari))
					.list();

			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				FormulirKegiatanPeserta formulirKegiatanPesertaCopy = (FormulirKegiatanPeserta) formulirKegiatanPeserta
						.clone();
				formulirKegiatanPesertaCopy.setId(null);
				formulirKegiatanPesertaCopy.setFormulirKegiatan(formulirKegiatan);
				session.save(formulirKegiatanPesertaCopy);
				session.flush();
			}
		}

		if (ttdKanan != null && ttdKanan.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(ttdKanan);
				ttdKanan.setRef(formulirKegiatan.getId());

				session.getTransaction().begin();
				session.update(ttdKanan);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (ttdKiri != null && ttdKiri.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(ttdKiri);
				ttdKiri.setRef(formulirKegiatan.getId());

				session.getTransaction().begin();
				session.update(ttdKiri);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		if (dosen != null) {
			Criteria criteria = session.createCriteria(FormulirKegiatanPeserta.class)
					.createAlias("formulirKegiatan", "formulirKegiatan")

					.add(searchaktif != null && searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("formulirKegiatan.aktif"),
									Restrictions.eq("formulirKegiatan.aktif", true))
							: Restrictions.sqlRestriction("true"))

					.add(Restrictions.or(Restrictions.isNotNull("siswa"),
							Restrictions.or(Restrictions.isNotNull("guru"),
									Restrictions.or(Restrictions.isNotNull("mahasiswa"),
											Restrictions.isNotNull("dosen")))))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							|| searchsmt.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.semester",
											searchsmt.getSelectedItem().getValue()))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.tahunAkademik",
											searchta.getSelectedItem().getValue()))

					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)

					.add(searchpeserta == null || searchpeserta.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("siswa.namaSiswa", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(Restrictions.ilike(
													"guru.namaGuru", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.ilike("mahasiswa.nama",
															searchpeserta.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("mahasiswa.nim",
																	searchpeserta.getValue().trim(),
																	MatchMode.ANYWHERE)))),
									Restrictions.or(
											Restrictions.ilike("dosen.nama", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("dosen.nidn", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.eq("dosen", dosen))

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.jurusan", searchjurusan, false),
									Restrictions.isNull("formulirKegiatan.jurusan")))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.fakultas", searchfakultas, false),
									Restrictions.isNull("formulirKegiatan.fakultas")))

					.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.sekolah", searchsekolah, false),
											Restrictions.isNull("formulirKegiatan.sekolah")))
					.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
							|| searchyayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.yayasan", searchyayasan, false),
											Restrictions.isNull("formulirKegiatan.yayasan")))

			;
			if (order)
				criteria.addOrder(Order.desc("id"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("formulirKegiatan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
			return criteria;
		}

		else if (mhs != null) {
			Criteria criteria = session.createCriteria(FormulirKegiatanPeserta.class)

					.add(Restrictions.or(Restrictions.isNotNull("siswa"),
							Restrictions.or(Restrictions.isNotNull("guru"),
									Restrictions.or(Restrictions.isNotNull("mahasiswa"),
											Restrictions.isNotNull("dosen")))))

					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)

					.add(searchpeserta == null || searchpeserta.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("siswa.namaSiswa", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(Restrictions.ilike(
													"guru.namaGuru", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.ilike("mahasiswa.nama",
															searchpeserta.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("mahasiswa.nim",
																	searchpeserta.getValue().trim(),
																	MatchMode.ANYWHERE)))),
									Restrictions.or(
											Restrictions.ilike("dosen.nama", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("dosen.nidn", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.eq("mahasiswa", mhs)).createAlias("formulirKegiatan", "formulirKegiatan")

					.add(searchaktif != null && searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("formulirKegiatan.aktif"),
									Restrictions.eq("formulirKegiatan.aktif", true))
							: Restrictions.sqlRestriction("true"))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							|| searchsmt.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.semester",
											searchsmt.getSelectedItem().getValue()))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.tahunAkademik",
											searchta.getSelectedItem().getValue()))

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.jurusan", searchjurusan, false),
									Restrictions.isNull("formulirKegiatan.jurusan")))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.fakultas", searchfakultas, false),
									Restrictions.isNull("formulirKegiatan.fakultas")))

					.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.sekolah", searchsekolah, false),
											Restrictions.isNull("formulirKegiatan.sekolah")))
					.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
							|| searchyayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.yayasan", searchyayasan, false),
											Restrictions.isNull("formulirKegiatan.yayasan")));
			if (order)
				criteria.addOrder(Order.desc("id"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("formulirKegiatan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
			return criteria;
		}

		else if (ssw != null) {
			Criteria criteria = session.createCriteria(FormulirKegiatanPeserta.class)

					.add(Restrictions.or(Restrictions.isNotNull("siswa"),
							Restrictions.or(Restrictions.isNotNull("guru"),
									Restrictions.or(Restrictions.isNotNull("mahasiswa"),
											Restrictions.isNotNull("dosen")))))

					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)

					.add(searchpeserta == null || searchpeserta.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("siswa.namaSiswa", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(Restrictions.ilike(
													"guru.namaGuru", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.ilike("mahasiswa.nama",
															searchpeserta.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("mahasiswa.nim",
																	searchpeserta.getValue().trim(),
																	MatchMode.ANYWHERE)))),
									Restrictions.or(
											Restrictions.ilike("dosen.nama", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("dosen.nidn", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.eq("siswa", ssw)).createAlias("formulirKegiatan", "formulirKegiatan")

					.add(searchaktif != null && searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("formulirKegiatan.aktif"),
									Restrictions.eq("formulirKegiatan.aktif", true))
							: Restrictions.sqlRestriction("true"))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							|| searchsmt.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.semester",
											searchsmt.getSelectedItem().getValue()))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.tahunAkademik",
											searchta.getSelectedItem().getValue()))

					.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.sekolah", searchsekolah, false),
											Restrictions.isNull("formulirKegiatan.sekolah")))
					.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
							|| searchyayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.yayasan", searchyayasan, false),
											Restrictions.isNull("formulirKegiatan.yayasan")));
			if (order)
				criteria.addOrder(Order.desc("id"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("formulirKegiatan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
			return criteria;
		}

		else if (gr != null) {
			Criteria criteria = session.createCriteria(FormulirKegiatanPeserta.class)

					.add(Restrictions.or(Restrictions.isNotNull("siswa"),
							Restrictions.or(Restrictions.isNotNull("guru"),
									Restrictions.or(Restrictions.isNotNull("mahasiswa"),
											Restrictions.isNotNull("dosen")))))

					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)

					.add(searchpeserta == null || searchpeserta.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("siswa.namaSiswa", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(Restrictions.ilike(
													"guru.namaGuru", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.ilike("mahasiswa.nama",
															searchpeserta.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("mahasiswa.nim",
																	searchpeserta.getValue().trim(),
																	MatchMode.ANYWHERE)))),
									Restrictions.or(
											Restrictions.ilike("dosen.nama", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("dosen.nidn", searchpeserta.getValue().trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.eq("guru", gr)).createAlias("formulirKegiatan", "formulirKegiatan")

					.add(searchaktif != null && searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("formulirKegiatan.aktif"),
									Restrictions.eq("formulirKegiatan.aktif", true))
							: Restrictions.sqlRestriction("true"))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							|| searchsmt.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.semester",
											searchsmt.getSelectedItem().getValue()))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("formulirKegiatan.tahunAkademik",
											searchta.getSelectedItem().getValue()))

					.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.sekolah", searchsekolah, false),
											Restrictions.isNull("formulirKegiatan.sekolah")))

					.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
							|| searchyayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("formulirKegiatan.yayasan", searchyayasan, false),
											Restrictions.isNull("formulirKegiatan.yayasan")));
			if (order)
				criteria.addOrder(Order.desc("id"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("formulirKegiatan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
			return criteria;
		}

		else {
			Criteria criteria = session.createCriteria(FormulirKegiatan.class)

					.add(searchaktif != null && searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
							: Restrictions.sqlRestriction("true"))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()));

			if (searchpeserta != null && !searchpeserta.getValue().trim().isEmpty()) {
				criteria = session.createCriteria(FormulirKegiatanPeserta.class)

						.add(Restrictions.or(Restrictions.isNotNull("siswa"),
								Restrictions.or(Restrictions.isNotNull("guru"),
										Restrictions.or(Restrictions.isNotNull("mahasiswa"),
												Restrictions.isNotNull("dosen")))))

						.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
						.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
						.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
						.createAlias("guru", "guru", Criteria.LEFT_JOIN)

						.add(searchpeserta == null || searchpeserta.getValue().trim().isEmpty()
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("siswa.namaSiswa", searchpeserta.getValue().trim(),
														MatchMode.ANYWHERE),

												Restrictions.or(Restrictions.ilike(
														"guru.namaGuru", searchpeserta.getValue().trim(),
														MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("mahasiswa.nama",
																		searchpeserta.getValue().trim(),
																		MatchMode.ANYWHERE),
																Restrictions.ilike("mahasiswa.nim",
																		searchpeserta.getValue().trim(),
																		MatchMode.ANYWHERE)))),
										Restrictions.or(
												Restrictions.ilike("dosen.nama", searchpeserta.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("dosen.nidn", searchpeserta.getValue().trim(),
														MatchMode.ANYWHERE))))

						.setProjection(Projections.groupProperty("formulirKegiatan"));

				if (order)
					criteria.addOrder(Order.desc("formulirKegiatan"));

				criteria = criteria.createCriteria("formulirKegiatan");

			} else if (order)
				criteria.addOrder(Order.desc("id"));

			criteria.add(
					searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
									Restrictions.isNull("jurusan")))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false),
									Restrictions.isNull("fakultas")))

					.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false),
											Restrictions.isNull("sekolah")))
					.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
							|| searchyayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false),
											Restrictions.isNull("yayasan")))

			;

			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
			return criteria;
		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FormulirKegiatan> formulirKegiatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formulirKegiatan);
		grid.setRowRenderer(new FormulirKegiatanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Textbox getKodeItemBiaya() {
		return kodeItemBiaya;
	}

	public void setKodeItemBiaya(Textbox kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

}
