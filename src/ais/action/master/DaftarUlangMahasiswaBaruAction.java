package ais.action.master;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataCalonMahasiswaDaftarUlangBaruBanbox;
import ais.action.master.helper.DaftarUlangPembayaranHelper;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiCicilanPembayaranHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.keuangan.DownloadCicilanCalonMahasiswa;
import ais.action.master.helper.keuangan.UploadCicilanCalonMahasiswa;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankBankaltimtara;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankBjb;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.MahasiswaVirtualAccountHelper;
import ais.action.master.pmb.TampilanPaymentGateway;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BriCommon;
import ais.common.BsiCommon;
import ais.common.CicilanPembayaranRecoveryHelper;
import ais.common.CimbCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DokuCommon;
import ais.common.FaspayCommon;
import ais.common.FinpayCommon;
import ais.common.IndonesianNumberToWords;
import ais.common.IpaymuCommon;
import ais.common.JatelindoCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.render.DetailPembayaranMahasiswaRenderer;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DaftarUlangMahasiswaBaruAction extends AbstractDaftarUlangMahasiswaAction {

	private static final long serialVersionUID = -4681108885695239730L;

	private Label semester;
	private Combobox akun;
	private Label tanggalValidasi;
	protected Combobox semesterPilihan;
	private boolean edit = true;
	private boolean delete = true;
	private Double tabungan = 0.0;
	// Batas saldo saat isi cicilan otomatis "Dari Tabungan" (0 = normal/tanpa batas).
	private double capSaldoIsiCicilan = 0.0;
	private Row rowNim, rowJenisKuliah, rowProdi, rowSemester, rowTahunMasuk, rowTahunAkademik, rowTanggalValidasi,
			rowKeteranganValidasi, rowValidator, rowPengurangan, rowKeterangan, rowListBiaya, rowMobile;
	private Component panelMencicil;
	private org.zkoss.zul.Div portalHost;
	private Component panelAnalisis;
	private Label jenisKuliah, prodi, labelNoUjianMahasiswa, labelNamaMahasiswa, labelTahunMasuk, labelTahunAkademik,
			labelKeteranganValidasi, validator;
	private Vbox labelFotoMahasiswa;
	private List<MyDoubleboxMin> pengurangan;
	private Textbox keterangan;
	private Grid gridss;
	private Grid gridCicilan = new Grid();
	private AmbilDataCalonMahasiswaDaftarUlangBaruBanbox pilihCalon;
	private Map<Long, LampiranLain> buktiPembayarans = new HashMap<Long, LampiranLain>();
	private Borderlayout borderlayoutUtama;
	private MyLabelBoldAja labelFooterItemBiaya, labelFooterTagihan, labelFooterDibayarAja;
	private Kegiatan kegiatan;
	protected JenisKegiatan jenisKegiatan = (JenisKegiatan) ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
	private JadwalPembayaran jadwalPembayaran;
	private BiodataCalonMahasiswa calonMahasiswa;
	private PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	private Double nilaiBiayaHarusDiBayars = 0.0;
	private boolean simpan;
	private Vbox center;
	private boolean bolehmencicilBaru = true;
	private BiodataCalonMahasiswa biodataCalonMahasiswaAktif = null;
	private Tbmuser tbmuser;
	private Button tombolRefresh;
	private double jumlahYangAkanDibayar;
	protected Tabpanel tabpanelUploadDanDownloadCicilanMahasiswa;
	private Tabpanel tabpanelDiskonMahasiswa;
	private Tabpanel tabpanelPembelianMahasiswa;
	private boolean tampilkanTanggalKwitansi = false;
	private BuktiPembayaran buktiPembayaran = null;
	private MyLabelBoldAja footerTotal, footerTotalTerbilang, footerDibayar, footerDibayarTerbilang;
	private MyCheckboxConfig mencicil;
	private Hbox hboxJenisPembayaran;
	private Rows rowsCicilan;
	private EventListener jumlahCicilahEventListener;
	private HashMap<Long, DetailBiaya> itemBiayas;
	private int countPengaturanBulanan = 0;
	private List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
	private List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = new ArrayList<PengaturanPembayaranBulanan>();
	@SuppressWarnings("rawtypes")
	protected List detailBiayas = new ArrayList();
	private DetailPembayaranMahasiswaRenderer detailPembayaranMahasiswaRenderer;
	private MyLabelBoldAja labelFooterKekurangan;
	private HashMap<Long, Double> dataTagihan;
	private boolean refresh = false;
	private Label terbilang, terbilangTagihan, terbilangSisa, terbilangSisaPersen;
	private Button sesuaikanDenganTagihan, sesuaikanDenganTagihanBulanan;
	private Box myspaceBayar;
	private MyToolbarbuttonConfig buttonReset;
	private MyLabelBoldAja labelTabungan;
	@SuppressWarnings("rawtypes")
	private ArrayList dataTagihanData = null;

	private static void buatPlaceholderPanel(org.zkoss.zk.ui.Component host, String ikon, String pesan) {
		if (host == null) return;
		org.zkoss.zul.Html ph = new org.zkoss.zul.Html();
		ph.setContent("<div style='text-align:center;padding:24px 12px 16px;color:#94a3b8;font-size:12px;line-height:1.6;'>"
			+ "<div style='font-size:22px;margin-bottom:8px;color:#e2e8f0;'>" + ikon + "</div>"
			+ "<div style='font-weight:600;color:#475569;font-size:12px;margin-bottom:4px;'>" + pesan + "</div>"
			+ "Pilih mahasiswa terlebih dahulu.</div>");
		ph.setParent(host);
	}

	private void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (!session.isOpen()) {
				return;
			}
		} catch (Exception e) {
			return;
		}
		Common.closeNativeSessionQuietly(session);
	}



	// === Accessor hook untuk AbstractDaftarUlangMahasiswaAction (state spesifik subclass) ===
	@Override
	protected Grid getGridCicilan() {
		return gridCicilan;
	}

	@Override
	protected Grid getGridBiaya() {
		return gridss;
	}

	@Override
	protected List<CicilanPembayaran> getCicilanPembayarans() {
		return cicilanPembayarans;
	}

	@Override
	protected java.util.Collection<DetailBiaya> getSemuaItemBiaya() {
		return itemBiayas.values();
	}

	@Override
	protected MyLabelBoldAja getFooterDibayar() {
		return footerDibayar;
	}

	@Override
	protected MyLabelBoldAja getFooterDibayarTerbilang() {
		return footerDibayarTerbilang;
	}

	@Override
	protected MyLabelBoldAja getFooterTotal() {
		return footerTotal;
	}

	@Override
	protected MyLabelBoldAja getFooterTotalTerbilang() {
		return footerTotalTerbilang;
	}

	public void onPembelianMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelPembelianMahasiswa, "/pages/master/koperasi/pem_online.zul?langsungBayar=true&sumberPembayaran=mahasiswa&modePelanggan=mahasiswa&jenisPelanggan=mahasiswa&hanyaMahasiswa=true&dariDaftarUlangMahasiswa=true");
	}

	public void onDiskonMahasiswa(Event event) {
		if (tabpanelDiskonMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelDiskonMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/diskon_calon_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onUploadDanDownloadCicilanMahasiswa(Event event) {
		if (tabpanelUploadDanDownloadCicilanMahasiswa.getChildren().size() == 0) {
			ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(tabpanelUploadDanDownloadCicilanMahasiswa, "100%", new int[] { 0 });

			{ org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Download Pembayaran", "/img/svg/download.svg");
			  DownloadCicilanCalonMahasiswa laporan = new DownloadCicilanCalonMahasiswa();
			  laporan.setHeight("100%"); laporan.setWidth("100%"); laporan.setParent(panel); }

			{ org.zkoss.zul.Div panel = btnTab.tambahTab(1, "Upload Pembayaran", "/img/svg/upload.svg");
			  UploadCicilanCalonMahasiswa upload = new UploadCicilanCalonMahasiswa();
			  upload.setHeight("100%"); upload.setWidth("100%"); upload.setParent(panel); }
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null) {
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}
		if (rowTanggalValidasi != null) {
			rowTanggalValidasi.setVisible(false);
		}
		if (execution.getParameter("buktiPembayaran") != null) {
			buktiPembayaran = (BuktiPembayaran) HibernateUtil.currentSession().createCriteria(BuktiPembayaran.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("buktiPembayaran")))).uniqueResult();
		}

		String idBiodataCalonMahasiswa = execution.getParameter("biodataCalonMahasiswa");
		if (idBiodataCalonMahasiswa != null) {
			biodataCalonMahasiswaAktif = (BiodataCalonMahasiswa) ConstantValues
					.simpleObject(
							HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.idEq(Long.parseLong(idBiodataCalonMahasiswa))),
							BiodataCalonMahasiswa.class);
		}

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			biodataCalonMahasiswaAktif = tbmuser.getBiodataCalonMahasiswa();
		}

		if (tombolRefresh != null) {
			tombolRefresh.setAttribute("janganDisabled", true);
		}

		if (biodataCalonMahasiswaAktif == null) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}

		tampilkanTanggalKwitansi = Common.bolehKonfigurasi("tampilkan_tanggal_kwitansi_di_pembayaran", Konfigurasi.TIDAK_AKTIF);

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		for (int i = 0; i < maxSemesterPilihan; i++) {
			Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterPilihan.appendChild(comboitem);
		}
		if (semesterPilihan != null) { semesterPilihan.setReadonly(true); }
		Common.selectComboItem(semesterPilihan, 1);
		if (semesterPilihan != null) { semesterPilihan.setAttribute("janganDisabled", true); }
		if (tanggalValidasi != null) { tanggalValidasi.setAttribute("janganDisabled", true); }

		pilihCalon.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onCariMahasiswa(null);
					}
				});
			}
		});

		/*
		 * Tata letak PORTAL responsif (reuse ais.ui.util.PortalUiHelper): 2 kolom di
		 * desktop (Data Mahasiswa | Pembayaran) lalu dasbor Analisis MEMBENTANG penuh
		 * di bawah; otomatis menumpuk 1 kolom di HP. Logika lama tidak diubah.
		 */
		MyPortallayout portal = ais.ui.util.PortalUiHelper.portal(portalHost);
		MyPortalchildren kolMahasiswa = ais.ui.util.PortalUiHelper.kolom(portal, "50%");
		MyPortalchildren kolPembayaran = ais.ui.util.PortalUiHelper.kolom(portal, "50%");
		MyPortalchildren kolAnalisis = ais.ui.util.PortalUiHelper.kolom(portal, "100%");

		org.zkoss.zk.ui.Component bodyMahasiswa = ais.ui.util.PortalUiHelper.panel(kolMahasiswa,
				"Data Mahasiswa & Tagihan",
				"Identitas calon/mahasiswa beserta rincian biaya yang harus dibayar.");
		if (center != null) {
			center.setParent(bodyMahasiswa);
			center.setVisible(true);
		}

		panelMencicil = ais.ui.util.PortalUiHelper.panel(kolPembayaran,
				"Daftar Pembayaran / Angsuran",
				"Catatan tiap pembayaran/cicilan dan tombol untuk membayar.");
		buatPlaceholderPanel(panelMencicil, "💳", "Belum ada data pembayaran");

		panelAnalisis = ais.ui.util.PortalUiHelper.panel(kolAnalisis,
				"Analisis & Dasbor Pembayaran",
				"Ringkasan mudah: sudah dibayar berapa, sisanya berapa, dan tren pembayarannya.");
		buatPlaceholderPanel(panelAnalisis, "📊", "Belum ada analisis");

		simpan = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		if (buktiPembayaran != null && buktiPembayaran.getBiodataCalonMahasiswa() != null) {
			BiodataCalonMahasiswa bioCalon = buktiPembayaran.getBiodataCalonMahasiswa();
			pilihCalon.setAttribute("calonMahasiswa", bioCalon);
			pilihCalon.setValue(bioCalon.getNoRegistrasi() + " - " + bioCalon.getNama());
			pilihCalon.setId("calonmhs_" + bioCalon.getId());
			pilihCalon.setDisabled(true);

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onCariMahasiswa(arg0);
					pilihCalon.setDisabled(true);
				}
			});
		} else if (biodataCalonMahasiswaAktif != null) {
			pilihCalon.setAttribute("calonMahasiswa", biodataCalonMahasiswaAktif);
			pilihCalon.setValue(
					biodataCalonMahasiswaAktif.getNoRegistrasi() + " - " + biodataCalonMahasiswaAktif.getNama());
			pilihCalon.setId("calonmhs_" + biodataCalonMahasiswaAktif.getId());
			pilihCalon.setDisabled(true);

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onCariMahasiswa(null);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private boolean checkKondisiSebelumbayarBaru() throws Exception {
		if (countPengaturanBulanan > 0 && cicilanPembayarans.isEmpty()) {
			inputSesuaiTagihanBulanan(null);
		} else if (gridCicilan != null && countPengaturanBulanan == 0) {
			boolean ada = false;
			List<Row> rows = gridCicilan.getRows().getChildren();
			for (Row row : rows) {
				try {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
					cicilanPembayaran.setNilai(jumlahCicilan.getValue());
					row.setValign("top");
					row.setAttribute("cicilanPembayaran", cicilanPembayaran);
					ada |= (jumlahCicilan != null && row.isVisible());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
			if (!ada) {
				inputSesuaiTagihan();
			}
		}
		return true;
	}

	public boolean apakah0(boolean chek) throws Exception {
		jumlahYangAkanDibayar = hitungJumlahYangAkanDibayarDariTampilan();

		if (chek && Math.abs(jumlahYangAkanDibayar) < 0.01) {
			MyMessageboxConfig.show(
					"Belum ada nilai pembayaran baru yang dapat dikirim ke bank atau payment gateway. Silakan klik Pilih Semua, pilih tagihan yang akan dibayar, atau isi nilai cicilan terlebih dahulu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	private EventListener rubahTanggal = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			Date tanggal = WaktuUtil.getDate();
			Integer tahunAngkatanMhs = calonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(Integer.parseInt(semester.getValue()),
					tahunAngkatanMhs, semesterMulai, calonMahasiswa.getSemesterMulai());
			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

			Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(tanggal,
					jenisKegiatan, calonMahasiswa.getJenjang(), tahunAkademik,
					calonMahasiswa.getSemesterMulai().equalsIgnoreCase(Perkuliahan.GANJIL),
					calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(), calonMahasiswa.getNoRegistrasi(),
					calonMahasiswa.getGelombangPendaftaran());

			jadwalPembayaran = (JadwalPembayaran) serializables[0];
			if (jadwalPembayaran == null) {
				// BLOK LANGSUNG: jadwal tidak ditemukan utk konteks ini (belum dibuat, sudah
				// selesai, atau belum dimulai) -> JANGAN fallback ke kegiatan.getJadwalPembayaran()
				// (jadwal lama yang menempel di Kegiatan bisa milik TA lain / sudah kedaluwarsa).
				MyMessageboxConfig.show(
						"Mohon maaf, jadwal pembayaran belum tersedia, telah terlewat, atau belum dimulai. Langkah yang dapat dilakukan: (1) periksa kembali periode jadwal pembayaran yang berlaku; (2) pastikan tanggal saat ini berada dalam rentang jadwal pembayaran; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan atau Administrator sistem.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			Common.clear(rowListBiaya);
			rowListBiaya.setVisible(true);
			listBiaya(calonMahasiswa, kegiatan, jenisKegiatan);
		}
	};
//	private List<CicilanPembayaran> cicilanPembayaransTemp = new ArrayList<CicilanPembayaran>();

	public void onCariMahasiswa(final Event event) throws Exception {
		tabungan = 0.0;
//		cicilanPembayaransTemp.clear();
		if (event != null && event.getTarget() instanceof Button || ((jenisKegiatan != null
				&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
				|| (jenisKegiatan != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())))) {
			if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
				calonMahasiswa.reInitKegiatan(HibernateUtil.currentSession());
			}
			refresh = true;
		} else {
			refresh = false;
		}

		Common.clear(panelMencicil);
		kegiatan = null;
		calonMahasiswa = (BiodataCalonMahasiswa) pilihCalon.getAttribute("calonMahasiswa");

		if (calonMahasiswa == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Ujian yang dimasukkan tidak terdaftar. Langkah yang dapat dilakukan: (1) periksa kembali penulisan Nomor Ujian; (2) pastikan calon mahasiswa telah terdaftar pada sistem; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		} else {
			// getSelectedItem() bisa null bila belum ada semester terpilih -> .getValue() NPE.
			if (semesterPilihan.getSelectedItem() == null
					|| semesterPilihan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu memilih semester terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan semester; (2) pilih semester yang sesuai; (3) lanjutkan kembali proses pencarian data mahasiswa.",
						"Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();

			if (jenisKegiatan != null && (jenisKegiatan.getMinSmt() > smt || jenisKegiatan.getMaxSmt() < smt)) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, jenis pembayaran \"{V1}\" tidak tersedia untuk semester {V2}. Langkah yang dapat dilakukan: (1) periksa kembali pilihan semester pada kolom yang tersedia; (2) pilih semester yang sesuai dengan ketentuan jenis pembayaran ini; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						jenisKegiatan.getNamaKegiatan(), smt);
				return;
			}

			if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
					&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
				if (Common.bolehKonfigurasi("calon_mahasiswa_harus_lulus_sebelum_bayar_daftar_ulang")) {
					Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
					if (prodiLulus == null || prodiLulus.getId() == null) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, calon mahasiswa atas nama \"{V1}\" belum dinyatakan lulus sehingga pembayaran daftar ulang belum dapat diproses. Langkah yang dapat dilakukan: (1) pastikan status kelulusan calon mahasiswa telah ditetapkan; (2) lengkapi proses penetapan kelulusan terlebih dahulu; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								calonMahasiswa.getNama());
						return;
					}
				}
				kegiatan = calonMahasiswa.ambilKegiatansRefresh(smt, jenisKegiatan);
			} else {
				kegiatan = calonMahasiswa.ambilKegiatansRefresh(null, jenisKegiatan);
			}

			Mahasiswa mahasiswa = calonMahasiswa.getMahasiswa();
			if (mahasiswa != null) {
				if (refresh) {
					mahasiswa.reInitKegiatan(HibernateUtil.currentSession());
				}
				tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(mahasiswa);
			}

			if (labelTabungan != null) {
				labelTabungan.setValue("Tabungan : " + Common.numberFormat.get().format(tabungan));
				labelTabungan.setVisible(tabungan > 0.1 && !(Common.bolehKonfigurasi("sembunyikan_nominal_tabungan_ke_mahasiswa", ais.database.model.Konfigurasi.AKTIF) && Common.getCurrentUser() != null && (Common.getCurrentUser().getMahasiswa() != null || Common.getCurrentUser().getBiodataCalonMahasiswa() != null)));
			}
			if (rowMobile != null)
				rowMobile.setVisible(false);
			rowNim.setVisible(true);
			labelNoUjianMahasiswa.setValue((calonMahasiswa.getNoUjian() == null ? "" : calonMahasiswa.getNoUjian())
					+ (calonMahasiswa.getPaket() == null ? "" : "/" + calonMahasiswa.getPaket().getNama())
					+ (calonMahasiswa.getJenisSeleksi() == null ? "" : "/" + calonMahasiswa.getJenisSeleksi().getNama())
					+ (calonMahasiswa.getStatusAwalMahasiswa() == null ? ""
							: "/" + calonMahasiswa.getStatusAwalMahasiswa().getNama())
					+ "/" + calonMahasiswa.getSemesterMulai() + (calonMahasiswa.getGelombangPendaftaran() == null ? ""
							: "/" + calonMahasiswa.getGelombangPendaftaran().getNama()));

			labelNamaMahasiswa.setValue(calonMahasiswa.getNama()
					+ (calonMahasiswa.getTeleponRumah() == null || calonMahasiswa.getTeleponRumah().trim().isEmpty()
							? ""
							: " / " + calonMahasiswa.getTeleponRumah())
					+ (calonMahasiswa.getEmail() == null || calonMahasiswa.getEmail().trim().isEmpty() ? ""
							: " / " + calonMahasiswa.getEmail())
					+ (calonMahasiswa.getAlamat() == null || calonMahasiswa.getAlamat().trim().isEmpty() ? ""
							: " / " + calonMahasiswa.getAlamat())
					+ " / " + calonMahasiswa.getKewarganegaraan());

			if (labelFotoMahasiswa != null) {
				Common.clear(labelFotoMahasiswa);
				CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(labelFotoMahasiswa);
			}

			rowJenisKuliah.setVisible(true);
			jenisKuliah.setValue(calonMahasiswa.getProgram());
			rowProdi.setVisible(true);

			String p = (calonMahasiswa.getProdi1() == null ? "" : calonMahasiswa.getProdi1().getNama() + "/")
					+ (calonMahasiswa.getProdi2() == null ? "" : calonMahasiswa.getProdi2().getNama() + "/")
					+ (calonMahasiswa.getProdi3() == null ? "" : calonMahasiswa.getProdi3().getNama() + "/")
					+ (calonMahasiswa.getProdi4() == null ? "" : calonMahasiswa.getProdi4().getNama() + "/")
					+ (calonMahasiswa.getProdi5() == null ? "" : calonMahasiswa.getProdi5().getNama() + "/")
					+ (calonMahasiswa.getProdiLulus() == null ? ""
							: " Lulus : " + calonMahasiswa.getProdiLulus().getNama());

			prodi.setValue(p);
			rowSemester.setVisible(false);
			semester.setValue(smt.toString());

			rowTahunMasuk.setVisible(true);
			Integer tahunAngkatanMhs = calonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(Integer.parseInt(semester.getValue()),
					tahunAngkatanMhs, semesterMulai, calonMahasiswa.getSemesterMulai());
			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

			labelTahunAkademik.setValue(tahunAkademik);
			labelTahunMasuk.setValue(calonMahasiswa.getTahun() == null
					? String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR))
					: calonMahasiswa.getTahun().toString());

			rowTahunAkademik.setVisible(true);
			rowTanggalValidasi.setVisible(false);
			rowValidator.setVisible(true);
			rowPengurangan.setVisible(true);
			rowKeterangan.setVisible(true);
			rowKeteranganValidasi.setVisible(true);

			if (kegiatan != null) {
				labelKeteranganValidasi.setValue("Sudah divalidasi");
				tanggalValidasi.setValue(Common.dateFormat6.get().format(kegiatan.getTanggal()));
				validator.setValue(kegiatan.getValidator() == null ? "" : kegiatan.getValidator());
				keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());
			} else {
				labelKeteranganValidasi.setValue("Belum divalidasi");
				tanggalValidasi.setValue(Common.dateFormat6.get().format(ais.ui.util.WaktuUtil.getDate()));
			}

			tanggalValidasi.addEventListener("onChange", rubahTanggal);
			rubahTanggal.onEvent(null);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void listCicilan(final Kegiatan kegiatan, final boolean refresh) throws Exception {
		Common.clear(panelMencicil);
		if (panelMencicil instanceof East)
			((East) panelMencicil).setTitle("Daftar Pembayaran / Angsuran");

		Integer jumlah = 40;
		cicilanPembayarans = new ArrayList<CicilanPembayaran>();
		if (kegiatan != null && kegiatan.getId() != null) {
			cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, refresh);
		}
		if (detailPembayaranMahasiswaRenderer != null) {
			pengaturanPembayaranBulanans = detailPembayaranMahasiswaRenderer.ubahWarnaStatus(cicilanPembayarans);
		}

		// PERINGATAN "item hilang dari tagihan": bila sebuah Item Biaya PERNAH dibayar
		// (ada di riwayat cicilan) untuk kegiatan ini, tapi TIDAK ADA lagi di tagihan yang
		// SEDANG tampil saat ini (mis. tagihan ter-generate ulang dari Setting Biaya yang
		// beda, contoh kasus: item beasiswa "Gratis Pol" hilang berganti item SPP reguler),
		// tampilkan keterangan mencolok/merah di panel riwayat pembayaran agar staf langsung
		// sadar sebelum melanjutkan proses entry pembayaran/pelunasan.
		if (kegiatan != null && kegiatan.getId() != null && itemBiayas != null) {
			java.util.Map<Long, String> namaItemHilang = new java.util.LinkedHashMap<Long, String>();
			java.util.Set<Long> itemBiayaIdSaatIni = new java.util.HashSet<Long>();
			for (DetailBiaya db : itemBiayas.values()) {
				if (db != null && db.getItemBiaya() != null && db.getItemBiaya().getId() != null) {
					itemBiayaIdSaatIni.add(db.getItemBiaya().getId());
				}
			}
			for (CicilanPembayaran riwayat : cicilanPembayarans) {
				if (riwayat == null || riwayat.getNilai() == null || Math.abs(riwayat.getNilai()) < 0.1
						|| riwayat.getItemBiaya() == null || riwayat.getItemBiaya().getId() == null) {
					continue;
				}
				Long idItem = riwayat.getItemBiaya().getId();
				if (!itemBiayaIdSaatIni.contains(idItem) && !namaItemHilang.containsKey(idItem)) {
					namaItemHilang.put(idItem, riwayat.getItemBiaya().getKode() + " - " + riwayat.getItemBiaya().getNama());
				}
			}
			if (!namaItemHilang.isEmpty()) {
				ais.ui.util.MyDiv peringatanItemHilang = new ais.ui.util.MyDiv();
				peringatanItemHilang.setStyle(
						"background:#fee2e2;border:1px solid #dc2626;color:#991b1b;font-weight:700;"
								+ "padding:8px 10px;margin:4px 0 8px 0;border-radius:4px;");
				StringBuilder pesanItemHilang = new StringBuilder(
						"⚠ Perhatian: item biaya berikut PERNAH dibayar/ditagih sebelumnya untuk kegiatan ini, "
								+ "tetapi TIDAK muncul lagi di tagihan yang sedang tampil saat ini. Ini bisa menandakan "
								+ "tagihan ter-generate ulang dari Setting Biaya yang berbeda (mis. beasiswa berubah "
								+ "menjadi reguler). Mohon periksa kembali Paket/Jenis Seleksi/Setting Biaya mahasiswa "
								+ "ini sebelum melanjutkan entry pembayaran:");
				for (String namaItem : namaItemHilang.values()) {
					pesanItemHilang.append("\n- ").append(namaItem);
				}
				new Label(pesanItemHilang.toString()).setParent(peringatanItemHilang);
				peringatanItemHilang.setParent(panelMencicil);
			}
		}

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaran cicilanPembayaran = cicilanPembayarans.isEmpty() ? null
					: cicilanPembayarans.get(cicilanPembayarans.size() - 1);
			if (cicilanPembayaran != null) {
				Common.selectComboItem(akun, cicilanPembayaran.getJenisPembayaran());
			} else {
				Common.selectComboItem(akun, ConstantValues.TUNAI);
			}
		} else {
			Common.selectComboItem(akun, ConstantValues.TUNAI);
		}

		if (akun != null && akun.getSelectedItem() == null) {
			if (JenisPembayaran.DEFAULT_JENIS_PEMBAYARAN == null)
				JenisPembayaran.reloadDefault();
			Common.selectComboItem(akun, JenisPembayaran.DEFAULT_JENIS_PEMBAYARAN);
		}

		footerTotal = new MyLabelBoldAja();
		footerTotalTerbilang = new MyLabelBoldAja();

		final Row rowUtama = (panelMencicil instanceof East) ? Common.tampilanScroll(panelMencicil)
				: Common.tampilanScroll1(panelMencicil);

		if (detailPembayaranMahasiswaRenderer != null) {
			// Dasbor Analisis dipindah ke panel sendiri yang MEMBENTANG penuh di bawah
			// (kolom Analisis portal). Fallback ke rowUtama bila portal belum tersedia.
			if (panelAnalisis != null) {
				Common.clear(panelAnalisis);
				Row analisisRow = Common.tampilanScroll1(panelAnalisis);
				detailPembayaranMahasiswaRenderer.pasangPanelAnalisisPembayaran(cicilanPembayarans, analisisRow);
			} else {
				MyFormRow rowAnalisisFallback = new MyFormRow();
				rowAnalisisFallback.setParent(rowUtama.getParent());
				detailPembayaranMahasiswaRenderer.pasangPanelAnalisisPembayaran(cicilanPembayarans, rowAnalisisFallback);
			}
		}

		// Alat penanganan data pembayaran GANDA (peringatan + checkbox tampilkan +
		// tombol Bersihkan Data Ganda). Hanya muncul bila terdeteksi duplikat.
		// Ditaruh di BARIS sendiri (1 kolom) — bukan satu baris dengan toolbar — agar
		// tata letak panel tetap 1 kolom (tombol/toolbar di atas, lalu peringatan, lalu grid).
		MyFormRow rowDuplikat = new MyFormRow();
		rowDuplikat.setParent(rowUtama.getParent());
		ais.action.master.helper.CicilanDuplikatHelper.pasangAlatDuplikat(kegiatan, cicilanPembayarans, rowDuplikat,
				new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						listCicilan(kegiatan, true);
					}
				});

		// Deteksi PER ITEM: pembayaran lebih dari sekali (dibayar > tagihan akibat baris berulang) +
		// tombol "Terdeteksi pembayaran lebih dari sekali" → bersihkan kelebihan sampai pas tagihan.
		ais.action.master.helper.CicilanDuplikatHelper.pasangAlatPembayaranBerulang(kegiatan, cicilanPembayarans,
				rowDuplikat, new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						listCicilan(kegiatan, true);
					}
				});

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(rowUtama);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CicilanPembayaran.class, new DataCriteria() {
			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				return session.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kegiatan", kegiatan))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke"));
			}
		}, "kegiatan", "jenisPembayaran", "tanggal", "nilai", "itemBiaya", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);
		cetakToolbarbutton.setVisible(!cicilanPembayarans.isEmpty());

		BuktiPembayaranAction.ambilBukti(null, 1, calonMahasiswa, jenisKegiatan, gridCicilan, buktiPembayaran)
				.setParent(toolbar);

		mencicil = new MyCheckboxConfig("Pembayaran dengan cara bertahap");
		mencicil.setParent(toolbar);
		mencicil.setChecked(cicilanPembayarans.size() > 0 || bolehmencicilBaru);
		mencicil.setVisible(false);

		EventListener mencicilListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				gridCicilan.setVisible(mencicil.isChecked());
				hboxJenisPembayaran.setVisible(!mencicil.isChecked()
						&& Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF)
						&& tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			}
		};
		mencicil.addEventListener("onCheck", mencicilListener);
		Common.createDefaultTimer(mencicilListener);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Surat Tagihan", "/img/invoice-icon_surat.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
				CommonReportHelper.prosesSuratTagihan(calonMahasiswa, jenisKegiatan, kegiatan, smt, jadwalPembayaran);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bukti Pembayaran", "/img/invoice-icon_surat.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiCicilanPembayaranHelper revisiHelper = new RevisiCicilanPembayaranHelper(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								listCicilan(kegiatan, true);
							}
						});
					}
				}, kegiatan);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		button.setParent(toolbar);

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaranRecoveryHelper.createRecoveryButton(kegiatan, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								onCariMahasiswa(null);
							} catch (Exception ex) {
								listCicilan(kegiatan, true);
							}
						}
					});
				}
			}).setParent(toolbar);
		}

		MyFormRow myrow = new MyFormRow();
		myrow.setParent(rowUtama.getParent());

		gridCicilan.setMold("paging");
		gridCicilan.setPageSize(10000);
		gridCicilan.setParent(myrow);
		gridCicilan.setVisible(cicilanPembayarans.size() > 0);
		gridCicilan.setSclass("dgrid du-nowarna");

		Columns columns = gridCicilan.getColumns() == null ? new Columns() : gridCicilan.getColumns();
		Common.clear(columns);
		columns.setParent(gridCicilan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bayar ke");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai bayar");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(tampilkanTanggalKwitansi ? "Tgl Byr/Tgl Kwitansi" : "Tanggal Bayar");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Cara bayar");
		column.setWidth("15%");
		column.setAlign("right");
		column.setVisible(tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		rowsCicilan = gridCicilan.getRows() == null ? new Rows() : gridCicilan.getRows();
		Common.clear(rowsCicilan);
		rowsCicilan.setParent(gridCicilan);

		jumlahCicilahEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							if (gridCicilan == null || gridCicilan.getPage() == null
									|| gridCicilan.getRows() == null) {
								// grid sudah tidak terpasang ke halaman (mis. window sudah ditutup/
								// dibangun ulang) sebelum timer sempat berjalan - tidak perlu lanjut
								return;
							}

							Double jumlah = 0.0;
							Double jumlahDibayar = 0.0;
							List<Row> rows = gridCicilan.getRows().getChildren();
							for (Row row : rows) {
								MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
								if (jumlahCicilan == null) {
									continue;
								}
								jumlah += (jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());

								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
										.getAttribute("cicilanPembayaran");
								if (cicilanPembayaran == null) {
									continue;
								}
								cicilanPembayaran.setNilai(jumlahCicilan.getValue());
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);

								if (cicilanPembayaran.getId() == null) {
									jumlahDibayar += (jumlahCicilan.getValue() == null ? 0.0
											: jumlahCicilan.getValue());
								}
							}

							footerTotal.setValue(Common.numberFormat.get().format(jumlah));
							footerTotalTerbilang.setValue(Common
									.kapitalAwalKata(IndonesianNumberToWords.convert(jumlah.longValue()) + " rupiah"));

							footerDibayar.setValue(Common.numberFormat.get().format(jumlahDibayar));
							footerDibayarTerbilang.setValue(Common.kapitalAwalKata(
									IndonesianNumberToWords.convert(jumlahDibayar.longValue()) + " rupiah"));

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaBaruAction.java:945");
//							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				});
			}
		};

		boolean bolehMerubahCicilan = tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null
				&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR);

		if (biodataCalonMahasiswaAktif != null) {
			bolehMerubahCicilan = false;
		} else if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
			String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am")
					.getNilai();
			String[] aa = admLain.split(";");
			for (String a : aa) {
				try {
					bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
					if (bolehMerubahCicilan)
						break;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!bolehMerubahCicilan) {
				admLain = Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa", "").getNilai();
				aa = admLain.split(";");
				for (String a : aa) {
					try {
						bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.getUserId());
						if (bolehMerubahCicilan)
							break;
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		List<DetailBiaya> yangSudahDibayar = new ArrayList<DetailBiaya>();
		if (countPengaturanBulanan > 0) {
			List<Integer> bulans = new ArrayList<Integer>();
			for (final PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
				Double n = pengaturanPembayaranBulanan.getNominal();
				if (!bulans.contains(pengaturanPembayaranBulanan.getRealBulan()) && n > 0.1) {
					bulans.add(pengaturanPembayaranBulanan.getRealBulan());
					MyToolbarbuttonConfig sesuaikanDenganTagihanBulananBtn = new MyToolbarbuttonConfig(
							pengaturanPembayaranBulanan.getNamaBulan(), "/img/svg/check2.svg");
					sesuaikanDenganTagihanBulananBtn.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							inputSesuaiTagihanBulanan(pengaturanPembayaranBulanan.getRealBulan());
						}
					});
					toolbar.appendChild(sesuaikanDenganTagihanBulananBtn);
				}
			}
		} else {
			yangSudahDibayar = updateDetalBiayaUntukDibayar();
		}

		ArrayList<Long> yangSudahDibayarBulanans = new ArrayList<Long>();
		final ArrayList<Combobox> comboboxsItemBiaya = new ArrayList<Combobox>();

		for (int i = 0; i < jumlah; i++) {
			final MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			CicilanPembayaran cicilanPembayaran = null;
			try {
				cicilanPembayaran = cicilanPembayarans.get(i);
			} catch (Exception e) {
				cicilanPembayaran = null;
			}

			if (cicilanPembayaran != null && cicilanPembayaran.getId() != null && cicilanPembayaran.getNilai() > 0.1) {
				sesuaikanDenganTagihan.setVisible(false);
				sesuaikanDenganTagihanBulanan.setVisible(false);
			}

			if (cicilanPembayaran == null) {
				cicilanPembayaran = new CicilanPembayaran(null);
				cicilanPembayaran.setValidator(tbmuser == null ? "" : tbmuser.getUserId());
			}

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setVisible(cicilanPembayaran.getId() != null);
			final Hbox hboxLampiran = Common.initCicilan(buktiPembayarans, rowsCicilan, row, i, cicilanPembayaran,
					buttonHapus);

			boolean tidakBolehUbah = cicilanPembayaran.getId() != null
					|| (tbmuser != null
							&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getMahasiswaBolehMencicilkan())
					|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getAdminBolehMencicilkan());

			final Combobox myCaraBayar = new Combobox();
			myCaraBayar.setReadonly(true);
			myCaraBayar.setAttribute("janganDisabled", true);
			final Textbox keteranganRow = new Textbox(
					cicilanPembayaran == null ? "" : cicilanPembayaran.getKeterangan());
			keteranganRow.setRows(2);
			final MyDoublebox jumlahCicilan = new MyDoublebox(
					cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai());
			jumlahCicilan.setWidth("90%");

			if (tidakBolehUbah) {
				if (cicilanPembayaran != null && cicilanPembayaran.getDenda() > 0.1) {
					Vbox vbox = new Vbox();
					row.appendChild(vbox);
					vbox.appendChild(new Label(Common.numberFormat.get().format(cicilanPembayaran.getNilai())));
					vbox.appendChild(
							new Label("Denda:" + Common.numberFormat.get().format(cicilanPembayaran.getDenda())));
				} else {
					row.appendChild(new Label(Common.numberFormat.get()
							.format(cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai())));
				}
			} else {
				row.appendChild(jumlahCicilan);
			}

			jumlahCicilan.addEventListener("onChange", jumlahCicilahEventListener);
			jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

			final MyDatebox tanggal = new MyDatebox(
					cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate() : cicilanPembayaran.getTanggal());
			tanggal.setFormat(Common.dateFormat31.get().toPattern());
			tanggal.setWidth("90%");
			tanggal.setDisabled(true);
			tanggal.setReadonly(false);

			final MyDatebox tanggalKwitansi = new MyDatebox(cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate()
					: cicilanPembayaran.getTanggalKwitansi());
			tanggalKwitansi.setFormat(Common.dateFormat31.get().toPattern());
			tanggalKwitansi.setWidth("90%");
			tanggalKwitansi.setDisabled(true);
			tanggalKwitansi.setReadonly(false);

			if (tidakBolehUbah) {
				Vbox v;
				(v = RevisiHelper.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
						Common.dateFormat.get().format(cicilanPembayaran.getTanggal()))).setParent(row);
				if (tampilkanTanggalKwitansi) {
					new Label(Common.dateFormat.get().format(cicilanPembayaran.getTanggalKwitansi())).setParent(v);
				}
			} else {
				if (tampilkanTanggalKwitansi) {
					Vbox v = new Vbox();
					v.appendChild(tanggal);
					v.appendChild(tanggalKwitansi);
					row.appendChild(v);
				} else {
					row.appendChild(tanggal);
				}
			}

			final Combobox myItemBiaya = new Combobox();
			myItemBiaya.setReadonly(true);
			comboboxsItemBiaya.add(myItemBiaya);
			myItemBiaya.setDisabled(true);
			myItemBiaya.setWidth("90%");

			if (tidakBolehUbah) {
				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pBulanan = cicilanPembayaran.getPengaturanPembayaranBulanan();
					DetailBiaya dbLabelB = null;
					try { dbLabelB = pBulanan.getDetailBiaya(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaBaruAction.java:1117");}
					String desc = pBulanan.getKeterangan();
					if (desc.isEmpty() && dbLabelB != null && dbLabelB.getItemBiaya() != null) {
						try { desc = dbLabelB.getItemBiaya().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaBaruAction.java:1120");}
					}
					desc = desc + ",  " + pBulanan.getNamaBulan()
							+ (dbLabelB != null && dbLabelB.getSettingBiayaDetail() != null
									&& dbLabelB.getDetailSettingBiaya() != null
									&& dbLabelB.getDetailSettingBiaya().getSettingBiaya() != null
									&& dbLabelB.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1
											? ", ke-" + dbLabelB.getBayarKe()
											: "");
					row.appendChild(new Label(desc));
				} else {
					row.appendChild(
							new Label(cicilanPembayaran.getItemBiaya() == null ? ""
									: cicilanPembayaran.getItemBiaya().getNama() + (cicilanPembayaran.getBayarKe() > 1
											? " ke-" + cicilanPembayaran.getBayarKe()
											: "")));
				}
			} else {
				row.appendChild(myItemBiaya);
			}

			row.setValign("top");
			row.setAttribute("jumlahCicilan", jumlahCicilan);
			row.setAttribute("tanggal", tanggal);
			row.setAttribute("tanggalKwitansi", tanggalKwitansi);
			row.setAttribute("itemBiaya", myItemBiaya);
			row.setAttribute("caraBayar", myCaraBayar);
			row.setAttribute("keterangan", keteranganRow);

			if (countPengaturanBulanan > 0) {
				tanggal.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Comboitem selectedComboitem = myItemBiaya.getSelectedItem();
						PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) (selectedComboitem == null ? null
								: selectedComboitem.getValue());
						if (pb != null && !pb.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()) {
							Double nom = pb.getNominal();
							Double denda = 0.0;
							if (pb.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								for (MyDoubleboxMin kurang : pengurangan) {
									DetailBiaya db = (DetailBiaya) kurang.getAttribute("itemBiaya");
									if (db != null && db.getId().equals(pb.getDetailBiaya().getId())) {
										nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
										break;
									}
								}
							} else {
								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains(
												"," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran : null;
								denda = pb.checkDenda(nom, tanggal.getValue(), jdw, jenisKegiatan) - nom;
							}
							CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
							cp.setPengaturanPembayaranBulanan(pb);
							cp.setTanggal(tanggal.getValue());

							if (denda > 0.1) {
								cp.setNilai(nom + denda);
								cp.setNilaiAsli(cp.getNilai());
								cp.setDenda(denda);
								keteranganRow.setValue(cp.getKeterangan());
								keteranganRow.setDisabled(false);
								jumlahCicilan.setValue(nom + denda);
							}
							row.setAttribute("cicilanPembayaran", cp);
						}
					}
				});

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pb = cicilanPembayaran.getPengaturanPembayaranBulanan();
					String namaIBBaru = "";
					String kodeIBBaru = "";
					try {
						if (pb.getDetailBiaya() != null && pb.getDetailBiaya().getItemBiaya() != null) {
							namaIBBaru = pb.getDetailBiaya().getItemBiaya().getNama();
							kodeIBBaru = pb.getDetailBiaya().getItemBiaya().getKode();
						}
					} catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
					MyComboitemConfig comboitem = new MyComboitemConfig(
							namaIBBaru + ", Bulan " + pb.getNamaBulan());
					comboitem.setDescription(kodeIBBaru + "-"
							+ namaIBBaru + ", bulan " + pb.getNamaBulan()
							+ ", nominal Rp. " + Common.numberFormat.get().format(cicilanPembayaran.getNilai())
							+ (pb.getDetailBiaya() != null && pb.getDetailBiaya().getSettingBiayaDetail() != null
									&& pb.getDetailBiaya().getDetailSettingBiaya() != null
									&& pb.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya() != null
									&& pb.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya()
											.getJumlahPembayaran() > 1 ? ", ke-" + pb.getDetailBiaya().getBayarKe()
													: ""));
					comboitem.setValue(pb);
					myItemBiaya.appendChild(comboitem);
					myItemBiaya.setSelectedItem(comboitem);
					myItemBiaya.setTooltiptext(comboitem.getDescription());
				} else {
					for (PengaturanPembayaranBulanan pb : pengaturanPembayaranBulanans) {
						int tahapan = 0;
						if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan) {
							try {
								String bln = Common.BULAN[pb.getRealBulan() - 1];
								tahapan = Common.poulateTahapan(calonMahasiswa.getProgram(),
										pb.getDetailBiaya().getJurusan(), 0, calonMahasiswa.getSemesterMulai())
										.get(bln);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

						Double nominalModifikasi = dataTagihan.containsKey(pb.getId()) ? dataTagihan.get(pb.getId())
								: pb.getNominal();

						if (nominalModifikasi >= 0.1 || nominalModifikasi <= -0.1) {
							JadwalPembayaran jdw = jadwalPembayaran != null
									&& jadwalPembayaran.getKhususUntukNim() != null
									&& jadwalPembayaran.getKhususUntukNim()
											.contains("," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran
													: null;
							Double hasilDenda = pb.checkDenda(nominalModifikasi, tanggal.getValue(), jdw,
									jenisKegiatan);

							String namaIBL = "";
							String kodeIBL = "";
							try {
								if (pb.getDetailBiaya() != null && pb.getDetailBiaya().getItemBiaya() != null) {
									namaIBL = pb.getDetailBiaya().getItemBiaya().getNama();
									kodeIBL = pb.getDetailBiaya().getItemBiaya().getKode();
								}
							} catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
							MyComboitemConfig comboitem = new MyComboitemConfig(
									namaIBL + ", Bulan " + pb.getNamaBulan());
							comboitem.setDescription(kodeIBL + "-"
									+ namaIBL + ",  " + pb.getNamaBulan()
									+ ", nominal Rp. " + Common.numberFormat.get().format(nominalModifikasi)
									+ (hasilDenda.intValue() > nominalModifikasi.intValue() ? pb.getInfoDenda() : "")
									+ (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan > 0
											? ", tahap " + tahapan
											: "")
									+ (pb.getDetailBiaya() != null && pb.getDetailBiaya().getSettingBiayaDetail() != null
											&& pb.getDetailBiaya().getDetailSettingBiaya() != null
											&& pb.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya() != null
											&& pb.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya()
													.getJumlahPembayaran() > 1
															? ", ke-" + pb.getDetailBiaya().getBayarKe()
															: ""));
							comboitem.setValue(pb);
							myItemBiaya.appendChild(comboitem);
						}
					}
				}

				EventListener itemBiayaEventListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Comboitem selectedComboitem = myItemBiaya.getSelectedItem();
						Object val = (selectedComboitem == null ? null : selectedComboitem.getValue());

						if (val != null || (arg0 != null && arg0.getData() != null)) {
							PengaturanPembayaranBulanan pb = (val instanceof PengaturanPembayaranBulanan)
									? (PengaturanPembayaranBulanan) val
									: null;
							DetailBiaya detailBiaya = (val instanceof DetailBiaya) ? (DetailBiaya) val : null;

							if (arg0 != null && arg0.getData() != null) {
								if (arg0.getData() instanceof PengaturanPembayaranBulanan)
									pb = (PengaturanPembayaranBulanan) arg0.getData();
								if (arg0.getData() instanceof DetailBiaya)
									pb = (PengaturanPembayaranBulanan) arg0.getData();
							}

							if (pb != null) {
								Double nominalModifikasi = pb.getNominal();
								if (pb.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()
										&& (nominalModifikasi == null || nominalModifikasi.intValue() == 0)) {
									Rows gridRows = (Rows) gridss.getRows();
									if (gridRows != null && gridRows.getChildren() != null) {
										List<Row> myRows = gridRows.getChildren();
										for (Row r : myRows) {
											detailBiaya = (DetailBiaya) r.getAttribute("myValue");
											if (detailBiaya != null && detailBiaya.getItemBiaya() != null
													&& detailBiaya.getItemBiaya().getId()
															.equals(pb.getDetailBiaya().getItemBiaya().getId())) {
												Double biaya = detailBiaya.getNilaiBiayaBaru() == null
														? detailBiaya.getNilaiBiaya()
														: detailBiaya.getNilaiBiayaBaru();
												try {
													Component component = (Component) r.getAttribute("tag");
													if (component instanceof MyDoublebox
															&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
														MyDoublebox jumlah = (MyDoublebox) component;
														biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
													} else if (component instanceof Label) {
														Label myLabel = (Label) component;
														biaya = Common.numberFormat.get().parse(myLabel.getValue())
																.doubleValue();
													}
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												nominalModifikasi = biaya;
												break;
											}
										}
									}
								}

								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains(
												"," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran : null;
								Double hasilDenda = pb.checkDenda(nominalModifikasi, tanggal.getValue(), jdw,
										jenisKegiatan);

								jumlahCicilan.setValue(hasilDenda);
								keteranganRow.setValue(myItemBiaya.getSelectedItem().getDescription());

								tanggal.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								tanggalKwitansi.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								keteranganRow.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								myCaraBayar.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));

								if (!tanggal.isDisabled() && tanggal.getValue() == null)
									tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
								if (!tanggalKwitansi.isDisabled() && tanggalKwitansi.getValue() == null)
									tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());

								if (pb.getDetailBiaya().getItemBiaya().getPenghitungan()
										.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									for (MyDoubleboxMin kurang : pengurangan) {
										DetailBiaya db = (DetailBiaya) kurang.getAttribute("itemBiaya");
										if (db != null && db.getId().equals(pb.getDetailBiaya().getId())) {
											Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
											jumlahCicilan.setValue(nom);
											break;
										}
									}
								}

								CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
								cp.setNilai(jumlahCicilan.getValue());
								cp.setPengaturanPembayaranBulanan(pb);
								row.setAttribute("cicilanPembayaran", cp);
								if (!cicilanPembayarans.contains(cp))
									cicilanPembayarans.add(cp);

								if (jumlahCicilan.getValue() != null)
									buttonHapus.setVisible(Math.abs(jumlahCicilan.getValue()) > 0.01);

								for (Combobox combobox : comboboxsItemBiaya) {
									if (combobox != myItemBiaya && !combobox.isDisabled()) {
										List<MyComboitemConfig> comboitems = combobox.getChildren();
										for (MyComboitemConfig ci : comboitems) {
											if (ci.getValue() instanceof PengaturanPembayaranBulanan) {
												PengaturanPembayaranBulanan pbTemp = (PengaturanPembayaranBulanan) ci
														.getValue();
												if (pbTemp.getId().equals(pb.getId())) {
													ci.detach();
													break;
												}
											}
										}
									}
								}

								row.setStyle("background-color: rgba(255,255,51,0.4)");
								Common.freeze(row, true);
								Common.freeze(hboxLampiran, false);
								hboxLampiran.setVisible(true);
								myCaraBayar.setDisabled(false);
								keteranganRow.setDisabled(false);

								if (cp.getJenisPembayaran() != null)
									Common.selectComboItem(myCaraBayar, cp.getJenisPembayaran());
								if (cp.getBuktiPembayaran() != null)
									myCaraBayar.setDisabled(true);

								if (tbmuser == null || tbmuser.getMahasiswa() == null) {
									tanggal.setDisabled(false);
									tanggalKwitansi.setDisabled(false);
									jumlahCicilan.setDisabled(tbmuser == null || (hasilDenda - nominalModifikasi) > 0.1
											|| !pb.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah());
									jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
								}

								try {
									boolean disableUbah = (tbmuser != null
											&& (tbmuser.getBiodataCalonMahasiswa() != null
													|| tbmuser.getMahasiswa() != null)
											&& pb.getDetailBiaya().getItemBiaya() != null
											&& !pb.getDetailBiaya().getItemBiaya().getMahasiswaBolehMencicilkan())
											|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
													&& tbmuser.getMahasiswa() == null
													&& pb.getDetailBiaya().getItemBiaya() != null
													&& !pb.getDetailBiaya().getItemBiaya().getAdminBolehMencicilkan());

									jumlahCicilan.disabledPaksa(disableUbah);
									jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
							} else if (detailBiaya != null) {
								CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
								cp.setNilai(jumlahCicilan.getValue());
								cp.setDetailBiaya(detailBiaya);
								row.setAttribute("cicilanPembayaran", cp);
								if (!cicilanPembayarans.contains(cp))
									cicilanPembayarans.add(cp);
								row.setStyle("background-color: rgba(255,255,51,0.4)");
							}
						}
						try {
							jumlahCicilahEventListener.onEvent(null);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				};

				myItemBiaya.addEventListener("onChange", itemBiayaEventListener);
				myItemBiaya.setAttribute("itemBiayaEventListener", itemBiayaEventListener);
				myItemBiaya.setDisabled(
						cicilanPembayaran.getId() != null && Math.abs(cicilanPembayaran.getNilai()) > 0.01);

				if (cicilanPembayaran.getId() != null && cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					yangSudahDibayarBulanans.add(cicilanPembayaran.getPengaturanPembayaranBulanan().getId());
				}

				PengaturanPembayaranBulanan nil = (PengaturanPembayaranBulanan) (myItemBiaya.getSelectedItem() == null
						? null
						: myItemBiaya.getSelectedItem().getValue());
				jumlahCicilan.setDisabled((tbmuser != null
						&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null))
						|| (tbmuser == null
								|| (nil == null ? true : !nil.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah())));

			} else {
				myItemBiaya.setDisabled(true);
				Common.insertComboItems(myItemBiaya, "nama", cicilanPembayaran.getId() == null ? yangSudahDibayar
						: new ArrayList<DetailBiaya>(itemBiayas.values()));
				if (cicilanPembayaran.getItemBiaya() != null) {
					List<Component> components = myItemBiaya.getChildren();
					for (Component c : components) {
						if (c instanceof Comboitem) {
							DetailBiaya db = (DetailBiaya) ((Comboitem) c).getValue();
							if (db != null && db.getItemBiaya() != null
									&& db.getBayarKe().equals(cicilanPembayaran.getBayarKe())
									&& db.getItemBiaya().getId().equals(cicilanPembayaran.getItemBiaya().getId())) {
								myItemBiaya.setSelectedItem(((Comboitem) c));
								break;
							}
						}
					}
				}

				EventListener itemBiayaEventListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DetailBiaya selectedItemBiaya = (DetailBiaya) myItemBiaya.getSelectedItem().getValue();
						if (selectedItemBiaya != null) {
							if (jumlahCicilan.getValue() != null)
								buttonHapus.setVisible(Math.abs(jumlahCicilan.getValue()) > 0.01);
							if (buttonHapus.isVisible()) {
								CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
								cp.setItemBiaya(selectedItemBiaya.getItemBiaya());
								cp.setBayarKe(selectedItemBiaya.getBayarKe());
								cp.setNilai(jumlahCicilan.getValue());
								row.setAttribute("cicilanPembayaran", cp);
								if (!cicilanPembayarans.contains(cp))
									cicilanPembayarans.add(cp);

								List<DetailBiaya> yangSudahDibayarList = updateDetalBiayaUntukDibayar();
								for (Combobox cb : comboboxsItemBiaya) {
									if (cb != myItemBiaya && cb.getSelectedItem() == null)
										Common.insertComboItems(cb, "nama", yangSudahDibayarList);
								}

								row.setStyle("background-color: rgba(255,255,51,0.4)");
								Common.freeze(row, true);
								Common.freeze(hboxLampiran, false);
								hboxLampiran.setVisible(true);
								myCaraBayar.setDisabled(false);
								keteranganRow.setDisabled(false);

								if (cp.getJenisPembayaran() != null)
									Common.selectComboItem(myCaraBayar, cp.getJenisPembayaran());
								if (cp.getBuktiPembayaran() != null)
									myCaraBayar.setDisabled(true);
							} else {
								if (selectedItemBiaya.getItemBiaya().getJenisPembayaran() != null) {
									Common.selectComboItem(myCaraBayar,
											selectedItemBiaya.getItemBiaya().getJenisPembayaran());
								}
							}

							try {
								boolean disableUbah = (tbmuser != null
										&& (tbmuser.getBiodataCalonMahasiswa() != null
												|| tbmuser.getMahasiswa() != null)
										&& !selectedItemBiaya.getItemBiaya().getMahasiswaBolehMencicilkan())
										|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
												&& tbmuser.getMahasiswa() == null
												&& !selectedItemBiaya.getItemBiaya().getAdminBolehMencicilkan());

								jumlahCicilan.disabledPaksa(disableUbah);
								jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				};

				myItemBiaya.addEventListener("onChange", itemBiayaEventListener);
				myItemBiaya.setAttribute("itemBiayaEventListener", itemBiayaEventListener);
				jumlahCicilan.setDisabled(tbmuser != null
						&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null));
			}

			myCaraBayar.setDisabled(true);
			myCaraBayar.setWidth("90%");
			if (tidakBolehUbah) {
				try {
					row.appendChild(
							new Label((cicilanPembayaran == null || cicilanPembayaran.getJenisPembayaran() == null
									? ConstantValues.TUNAI
									: cicilanPembayaran.getJenisPembayaran()).getNama()));
				} catch (Exception e) {
					row.appendChild(new Label());
				}
			} else {
				row.appendChild(myCaraBayar);
			}

			if (cicilanPembayaran != null && cicilanPembayaran.getJenisPembayaran() == null) {
				JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
						.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1).uniqueResult();
				cicilanPembayaran.setJenisPembayaran(jenisPembayaranDefault);
			}

			Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			try {
				Common.selectComboItem(myCaraBayar,
						cicilanPembayaran == null || cicilanPembayaran.getJenisPembayaran() == null
								? ConstantValues.TUNAI
								: cicilanPembayaran.getJenisPembayaran());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			keteranganRow.setWidth("90%");
			keteranganRow.setDisabled(true);

			if (tidakBolehUbah) {
				row.appendChild(new Label(cicilanPembayaran.getKeterangan()));
			} else {
				row.appendChild(keteranganRow);
			}

			jumlahCicilan.setDisabled((tbmuser != null
					&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null))
					|| (tbmuser == null || countPengaturanBulanan > 0 || (cicilanPembayaran != null
							&& cicilanPembayaran.getId() != null && Math.abs(cicilanPembayaran.getNilai()) > 0.01)));

			EventListener jumlahCicilanEventListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tanggal.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					tanggalKwitansi.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					keteranganRow.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					myItemBiaya.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					if (!tanggal.isDisabled() && tanggal.getValue() == null)
						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					if (!tanggalKwitansi.isDisabled() && tanggalKwitansi.getValue() == null)
						tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
					if (jumlahCicilan.getValue() != null)
						buttonHapus.setVisible(Math.abs(jumlahCicilan.getValue()) > 0.01);
					Common.freeze(hboxLampiran, false);
					hboxLampiran.setVisible(true);
				}
			};

			jumlahCicilan.addEventListener("onChange", jumlahCicilanEventListener);
			jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

			final CicilanPembayaran temCicilanPembayaran = cicilanPembayaran;

			Vbox hbox = new Vbox();
			hbox.setParent(row);

			button = new MyToolbarbuttonConfig("", "/img/svg/pencil-square.svg");
			button.setParent(hbox);
			button.setVisible(delete && temCicilanPembayaran != null
					&& bolehMerubahCicilan && cicilanPembayaran != null && cicilanPembayaran.getId() != null);
			button.setAttribute("janganDisabled", true);
			button.setTooltiptext("Ubah data pembayaran");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin mengubah data pembayaran ini? Perubahan yang dilakukan akan menggantikan data pembayaran sebelumnya. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
							"Konfirmasi Perubahan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										// Tampilan Edit Modals - dipadatkan
										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										Center center = new Center();
										center.setParent(borderlayout);
										ais.ui.util.ZkCompat.setFlex(center, true);
										MyGrid grid = new MyGrid();
										grid.setWidth("100%");
										grid.setHeight("100%");
										grid.setParent(center);
										Columns columns = new Columns();
										columns.setParent(grid);
										MyColumnConfig col1 = new MyColumnConfig();
										col1.setParent(columns);
										col1.setWidth("30%");
										new MyColumnConfig().setParent(columns);
										Rows rows = new Rows();
										rows.setParent(grid);

										final MyDatebox d = new MyDatebox(temCicilanPembayaran.getTanggal());
										final MyDoublebox comboboxBayar = new MyDoublebox(
												temCicilanPembayaran.getNilai());
										final MyDoublebox comboboxDenda = new MyDoublebox(
												temCicilanPembayaran.getDenda());
										final Combobox myCaraBayar = new Combobox();
										final Combobox myItemBayar = new Combobox();
										final Textbox ketBox = new Textbox(temCicilanPembayaran.getKeterangan());

										EventListener eventListenerSimpan = new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Session session = null;
												try {
													session = HibernateUtil.currentSession();
													session.refresh(temCicilanPembayaran);
													temCicilanPembayaran.setNilai(comboboxBayar.getValue());
													temCicilanPembayaran.setNilaiDiubah(comboboxBayar.getValue());
													temCicilanPembayaran.setDenda(comboboxDenda.getValue());
													temCicilanPembayaran.setTanggal(d.getValue());
													temCicilanPembayaran.setJenisPembayaran(
															(JenisPembayaran) (myCaraBayar.getSelectedItem() == null
																	? ConstantValues.TUNAI
																	: myCaraBayar.getSelectedItem().getValue()));

													DetailBiaya tempdetailBiaya = (DetailBiaya) (myItemBayar
															.getSelectedItem() == null ? null
																	: myItemBayar.getSelectedItem()
																			.getAttribute("detailBiaya"));
													PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) (myItemBayar
															.getSelectedItem() == null ? null
																	: myItemBayar.getSelectedItem().getAttribute(
																			"pengaturanPembayaranBulanan"));

													if (tempdetailBiaya != null) {
														temCicilanPembayaran.setDetailBiaya(tempdetailBiaya);
														temCicilanPembayaran
																.setItemBiaya(tempdetailBiaya.getItemBiaya());
													}
													if (temppengaturanPembayaranBulanan != null) {
														temCicilanPembayaran.setPengaturanPembayaranBulanan(
																temppengaturanPembayaranBulanan);
														temCicilanPembayaran.setDetailBiaya(
																temppengaturanPembayaranBulanan.getDetailBiaya());
														temCicilanPembayaran
																.setItemBiaya(temppengaturanPembayaranBulanan
																		.getDetailBiaya().getItemBiaya());
													}

													temCicilanPembayaran.setKeterangan(ketBox.getValue());
													Common.refreshUpdate(session, temCicilanPembayaran);
													session.flush();
												} catch (Exception ex) {
													Common.tampilErrorJikaAdmin(ex);
												}
											}
										};

										MyFormRow row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Bayar")));
										d.setReadonly(true);
										d.setFormat(Common.dateFormat3.get().toPattern());
										d.setWidth("95%");
										row.appendChild(d);
										d.addEventListener("onChange", eventListenerSimpan);

										row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item Biaya")));
										row.appendChild(myItemBayar);

										boolean bulanan = false;
										for (Object oo : dataTagihanData)
											if (oo instanceof PengaturanPembayaranBulanan)
												bulanan = true;

										for (Object oo : dataTagihanData) {
											DetailBiaya tempdetailBiaya = null;
											PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
											if (oo instanceof DetailBiaya)
												tempdetailBiaya = (DetailBiaya) oo;
											else if (oo instanceof PengaturanPembayaranBulanan) {
												temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) oo;
												if (temppengaturanPembayaranBulanan != null)
													tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
											}
											String desc = "";
											if (temppengaturanPembayaranBulanan != null) {
												desc = temppengaturanPembayaranBulanan.getKeterangan();
												desc = (desc.isEmpty()
														? (temppengaturanPembayaranBulanan.getDetailBiaya()
																.getItemBiaya().getNama())
														: desc) + ",  " + temppengaturanPembayaranBulanan.getNamaBulan()
														+ " , nominal Rp. " + Common.numberFormat.get()
																.format(temppengaturanPembayaranBulanan.getNominal());
											} else if (tempdetailBiaya != null) {
												desc = tempdetailBiaya.getKeterangan();
												desc = (desc.isEmpty() ? (tempdetailBiaya.getItemBiaya().getNama())
														: desc) + ", nominal Rp. "
														+ Common.numberFormat.get()
																.format(tempdetailBiaya.getNilaiBiaya());
											}
											Comboitem comboitem = new Comboitem(desc);
											comboitem.setAttribute("detailBiaya", tempdetailBiaya);
											comboitem.setAttribute("pengaturanPembayaranBulanan",
													temppengaturanPembayaranBulanan);
											myItemBayar.appendChild(comboitem);

											if (bulanan) {
												if (temppengaturanPembayaranBulanan != null
														&& temCicilanPembayaran.getPengaturanPembayaranBulanan() != null
														&& temppengaturanPembayaranBulanan.getId()
																.equals(temCicilanPembayaran
																		.getPengaturanPembayaranBulanan().getId()))
													myItemBayar.setSelectedItem(comboitem);
											} else {
												if (tempdetailBiaya != null
														&& temCicilanPembayaran.getDetailBiaya() != null
														&& tempdetailBiaya.getId()
																.equals(temCicilanPembayaran.getDetailBiaya().getId()))
													myItemBayar.setSelectedItem(comboitem);
											}
										}
										myItemBayar.setReadonly(true);
										myItemBayar.addEventListener("onChange", eventListenerSimpan);
										myItemBayar.setWidth("95%");

										row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar")));
										row.appendChild(myCaraBayar);
										myCaraBayar.setReadonly(true);
										myCaraBayar.setAttribute("janganDisabled", true);
										Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)));
										Common.selectComboItem(myCaraBayar,
												temCicilanPembayaran == null
														|| temCicilanPembayaran.getJenisPembayaran() == null
																? ConstantValues.TUNAI
																: temCicilanPembayaran.getJenisPembayaran());
										myCaraBayar.addEventListener("onChange", eventListenerSimpan);
										myCaraBayar.setWidth("95%");

										row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai Bayar")));
										comboboxBayar.setParent(row);
										comboboxBayar.setWidth("95%");
										comboboxBayar.addEventListener("onChange", eventListenerSimpan);

										row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Denda")));
										comboboxDenda.setParent(row);
										comboboxDenda.setWidth("95%");
										comboboxDenda.addEventListener("onChange", eventListenerSimpan);

										row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
										ketBox.setParent(row);
										ketBox.setWidth("95%");
										ketBox.setRows(5);
										ketBox.addEventListener("onChange", eventListenerSimpan);

										final MyWindow window = new MyWindow("Ubah Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("90%");
										window.setWidth("500px");

										South south = new South();
										ais.ui.util.ZkCompat.setFlex(south, true);
										south.setParent(borderlayout);
										Toolbar toolbar = new Toolbar();
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														onCariMahasiswa(
																new Event("", new MyToolbarbuttonConfig(), null));
													}
												});
											}
										});
										cancel.setParent(toolbar);
										borderlayout.setParent(window);
										window.setVisible(true);
										window.onModal();
									}
								}
							});
				}
			});

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setAttribute("janganDisabled", true);
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && temCicilanPembayaran != null && temCicilanPembayaran.getPostingHistory() == null
					&& bolehMerubahCicilan && cicilanPembayaran != null && cicilanPembayaran.getId() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan lagi. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Konfirmasi Penghapusan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											session.refresh(temCicilanPembayaran);
											session.delete(temCicilanPembayaran);
											session.flush();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.showFormat(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih menggunakan data ini; (2) hapus terlebih dahulu data yang berkaitan; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(hbox);

			row.setAttribute("buttonHapus", buttonHapus);
			buttonHapus.setTooltiptext("Hapus Data");
			buttonHapus.setVisible(false);
			buttonHapus.setAttribute("janganDisabled", true);
			buttonHapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin membatalkan data ini? Pembatalan akan mengubah status data terkait. Silakan tekan OK untuk melanjutkan pembatalan, atau Batal untuk mengurungkan.",
							"Konfirmasi Pembatalan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										try {
											CicilanPembayaran cp = (CicilanPembayaran) row
													.getAttribute("cicilanPembayaran");
											if (cp != null) {
												if (cp.getPengaturanPembayaranBulanan() != null
														&& cp.getPengaturanPembayaranBulanan().getId() != null) {
													detailPembayaranMahasiswaRenderer.bul
															.remove(cp.getPengaturanPembayaranBulanan().getId());
												} else if (cp.getItemBiaya() != null
														&& cp.getItemBiaya().getId() != null) {
													detailPembayaranMahasiswaRenderer.det
															.remove(cp.getItemBiaya().getId());
												}
											}

											cp.setPengaturanPembayaranBulanan(null);
											row.setAttribute("cicilanPembayaran", cp);
											cicilanPembayarans.remove(cp);

											myCaraBayar.setSelectedItem(null);
											myItemBiaya.setSelectedItem(null);
											jumlahCicilan.setValue(0.0);
											keteranganRow.setValue("");

											jumlahCicilan.setDisabled(tbmuser == null || countPengaturanBulanan > 0
													|| (cp != null && cp.getId() != null && cp.getNilai() > 0.0));
											tanggal.setDisabled(false);
											tanggal.setReadonly(false);
											tanggalKwitansi.setDisabled(false);
											tanggalKwitansi.setReadonly(false);
											keteranganRow.setReadonly(false);
											keteranganRow.setDisabled(false);
											myItemBiaya.setReadonly(false);
											myItemBiaya.setDisabled(false);

											if (jumlahCicilan.getValue() != null)
												buttonHapus.setVisible(Math.abs(jumlahCicilan.getValue()) > 0.01);
											row.setVisible(false);
											row.detach();
											jumlahCicilahEventListener.onEvent(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.showFormat(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih menggunakan data ini; (2) hapus terlebih dahulu data yang berkaitan; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													e.getMessage());
										}
									}
								}
							});
				}
			});
			buttonHapus.setParent(hbox);

			if (biodataCalonMahasiswaAktif != null)
				hbox.setVisible(false);
		}

		MyFormRow foot = new MyFormRow();
		foot.setStyle("border-bottom: 1px dashed;border-bottom-color: gray;");
		foot.setParent(rowUtama.getParent());
		Hbox hboxFoot = new Hbox();
		hboxFoot.setWidth("99%");
		hboxFoot.setParent(foot);

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Tambah Baru", "/img/add_item.png");
		toolbarbutton.setVisible(!jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());
		toolbarbutton.setParent(hboxFoot);
		hboxFoot.appendChild(new Space());
		hboxFoot.appendChild(new Space());

		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> listRow = gridCicilan.getRows().getChildren();
				for (Row row : listRow) {
					if (!row.isVisible()) {
						row.setVisible(true);
						MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
						buttonHapus.setVisible(true);
						Clients.scrollIntoView(row);
						break;
					}
				}
				if (countPengaturanBulanan > 0) {
					inputSesuaiTagihanBulanan(null);
				}
			}
		});

		// === Ringkasan pembayaran + tombol bayar (tata letak via helper bersama, dipakai Lama & Baru) ===
		footerDibayar = new MyLabelBoldAja();
		footerDibayarTerbilang = new MyLabelBoldAja();

		foot = new MyFormRow();
		foot.setParent(rowUtama.getParent());
		Box box = pasangRingkasanBayar(foot);

		menuBayar(box);
		jumlahCicilahEventListener.onEvent(null);

		if (!jenisKegiatan.getPenjelasanPembayaran().isEmpty()) {
			foot = new MyFormRow();
			foot.setParent(rowUtama.getParent());
			foot.appendChild(new Html(jenisKegiatan.getPenjelasanPembayaran()));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void inputSesuaiTagihan() throws Exception {
		if (jenisKegiatan != null) {
			Common.clear(rowsCicilan);
			int i = 0;
			double sisaSaldoIsi = capSaldoIsiCicilan; // mode "Dari Tabungan" (cap saldo)
			List<Row> rows = gridss.getRows().getChildren();
			for (Row rowData : rows) {
				ItemBiaya itemBiaya = null;
				DetailBiaya detailBiaya = null;
				if (rowData.getAttribute("pengaturanPembayaranBulanan") != null) {
					PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) rowData
							.getAttribute("pengaturanPembayaranBulanan");
					detailBiaya = pb.getDetailBiaya();
					itemBiaya = detailBiaya.getItemBiaya();
				} else if (rowData.getAttribute("myValue") != null) {
					detailBiaya = (DetailBiaya) rowData.getAttribute("myValue");
					itemBiaya = detailBiaya.getItemBiaya();
				}

				if (itemBiaya == null)
					continue;

				Double kekurangan = 0.0;
				try {
					kekurangan = Common.numberFormat.get()
							.parse(((MyLabelAgakKecil) rowData.getAttribute("kurang")).getValue()).doubleValue();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				CicilanPembayaran cp = new CicilanPembayaran(detailBiaya);
				for (CicilanPembayaran c : cicilanPembayarans) {
					if (c.getItemBiaya().getId().equals(itemBiaya.getId())) {
						cp = c;
						break;
					}
				}

				cp.setBayarKe(detailBiaya.getBayarKe());
				cp.setItemBiaya(itemBiaya);
				cp.setKe((i + 1));
				cp.setKegiatan(kegiatan);
				// Mode "Dari Tabungan" (capSaldoIsiCicilan>0): tiap tagihan dibatasi sisa saldo →
				// otomatis terangsur sesuai saldo, tak melebihi saldo (habis → 0, baris tak tampil).
				double nilaiIsiCicilan = kekurangan;
				if (capSaldoIsiCicilan > 0.1) {
					if (kekurangan <= 0.1 || sisaSaldoIsi <= 0.1) {
						nilaiIsiCicilan = 0.0;
					} else {
						nilaiIsiCicilan = Math.min(kekurangan, sisaSaldoIsi);
						sisaSaldoIsi -= nilaiIsiCicilan;
					}
				}
				cp.setNilai(nilaiIsiCicilan);
				cp.setNilaiAsli(cp.getNilai());
				cp.setValidator(tbmuser == null ? "" : tbmuser.toString());

				boolean tidakBolehUbah = cp.getId() != null
						|| (tbmuser != null
								&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
								&& cp.getItemBiaya() != null && !cp.getItemBiaya().getMahasiswaBolehMencicilkan())
						|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
								&& tbmuser.getMahasiswa() == null && cp.getItemBiaya() != null
								&& !cp.getItemBiaya().getAdminBolehMencicilkan());

				if (Math.abs(cp.getNilai()) > 0.1) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					Hbox hboxLampiran = Common.initCicilan(buktiPembayarans, rowsCicilan, row, i, cp, null);
					Common.freeze(hboxLampiran, false);
					hboxLampiran.setVisible(true);
					i++;

					final Textbox ketBox = new Textbox(cp == null ? "" : cp.getKeterangan());
					ketBox.setRows(2);
					final Combobox myCaraBayar = new Combobox();
					myCaraBayar.setReadonly(true);
					myCaraBayar.setAttribute("janganDisabled", true);
					final MyDoublebox jumlahCicilan = new MyDoublebox(cp == null ? 0.0 : cp.getNilai());

					if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
						for (MyDoubleboxMin kurang : pengurangan) {
							DetailBiaya pItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
							if (pItemBiaya != null && pItemBiaya.getId().equals(detailBiaya.getId())) {
								Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
								jumlahCicilan.setValue(nom);
								break;
							}
						}
					}

					jumlahCicilan.setWidth("90%");
					if (tidakBolehUbah) {
						jumlahCicilan.disabledPaksa(tidakBolehUbah);
						row.appendChild(new Label(Common.numberFormat.get().format(cp == null ? 0.0 : cp.getNilai())));
					} else {
						row.appendChild(jumlahCicilan);
					}
					jumlahCicilan.addEventListener("onChange", jumlahCicilahEventListener);
					jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

					final MyDatebox tanggal = new MyDatebox(
							cp == null ? ais.ui.util.WaktuUtil.getDate() : cp.getTanggal());
					tanggal.setFormat(Common.dateFormat31.get().toPattern());
					tanggal.setWidth("90%");
					final MyDatebox tanggalKwitansi = new MyDatebox(
							cp == null ? ais.ui.util.WaktuUtil.getDate() : cp.getTanggalKwitansi());
					tanggalKwitansi.setFormat(Common.dateFormat31.get().toPattern());
					tanggalKwitansi.setWidth("90%");

					if (tampilkanTanggalKwitansi) {
						Vbox v = new Vbox();
						v.appendChild(tanggal);
						v.appendChild(tanggalKwitansi);
						row.appendChild(v);
					} else {
						row.appendChild(tanggal);
					}

					final Combobox myItemBiaya = new Combobox();
					myItemBiaya.setReadonly(true);
					myItemBiaya.setWidth("90%");
					row.appendChild(myItemBiaya);
					myItemBiaya.setDisabled(true);
					Common.insertComboItems(myItemBiaya, "", new ArrayList(itemBiayas.values()));
					if (cp.getItemBiaya() != null)
						Common.selectComboItem(myItemBiaya, detailBiaya);

					myCaraBayar.setWidth("90%");
					row.appendChild(myCaraBayar);
					if (cp != null && cp.getJenisPembayaran() == null) {
						JenisPembayaran jpDefault = (JenisPembayaran) HibernateUtil.currentSession()
								.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setMaxResults(1).uniqueResult();
						cp.setJenisPembayaran(jpDefault);
					}

					Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					Common.selectComboItem(myCaraBayar,
							cp == null || cp.getJenisPembayaran() == null ? ConstantValues.TUNAI
									: cp.getJenisPembayaran());

					ketBox.setWidth("90%");
					row.appendChild(ketBox);
					MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
					if (buttonHapus != null)
						buttonHapus.setVisible(!jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());

					row.setAttribute("jumlahCicilan", jumlahCicilan);
					row.setAttribute("tanggal", tanggal);
					row.setAttribute("tanggalKwitansi", tanggalKwitansi);
					row.setAttribute("itemBiaya", myItemBiaya);
					row.setAttribute("caraBayar", myCaraBayar);
					row.setAttribute("keterangan", ketBox);

					if (tidakBolehUbah)
						Common.freeze(row, true);
				}
			}
		}
		jumlahCicilahEventListener.onEvent(null);
	}

	@SuppressWarnings({ "unchecked" })
	public void inputSesuaiTagihanBulanan(Integer bulan) throws Exception {
		List<Long> telahDibayar = new ArrayList<Long>();
		for (CicilanPembayaran cp : cicilanPembayarans) {
			if (cp.getPengaturanPembayaranBulanan() != null)
				telahDibayar.add(cp.getPengaturanPembayaranBulanan().getId());
		}
		List<PengaturanPembayaranBulanan> yangBelumDibayar = new ArrayList<PengaturanPembayaranBulanan>();
		for (PengaturanPembayaranBulanan pb : pengaturanPembayaranBulanans) {
			if (bulan == null || pb.getRealBulan().equals(bulan)) {
				Double nom = pb.getNominal();
				if (nom > 0.1 && !telahDibayar.contains(pb.getId()))
					yangBelumDibayar.add(pb);
			}
		}

		for (PengaturanPembayaranBulanan pb : pengaturanPembayaranBulanans) {
			if (bulan == null || pb.getRealBulan().equals(bulan)) {
				for (MyDoubleboxMin kurang : pengurangan) {
					DetailBiaya db = (DetailBiaya) kurang.getAttribute("itemBiaya");
					if (db != null && db.getId().equals(pb.getDetailBiaya().getId())) {
						Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
						if (nom < -0.01) {
							yangBelumDibayar.add(pb);
							break;
						}
					}
				}
			}
		}

		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		int i = 0;
		for (final Row row : mycicilanrows) {
			CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
			if (cp == null)
				cp = new CicilanPembayaran(null);

			boolean tidakBolehUbah = cp.getId() != null
					|| (tbmuser != null
							&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
							&& cp.getItemBiaya() != null && !cp.getItemBiaya().getMahasiswaBolehMencicilkan())
					|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
							&& cp.getItemBiaya() != null && !cp.getItemBiaya().getAdminBolehMencicilkan());

			if (cp.getPengaturanPembayaranBulanan() == null) {
				final PengaturanPembayaranBulanan pBulanan = i >= yangBelumDibayar.size() ? null
						: yangBelumDibayar.get(i);
				if (pBulanan != null && pBulanan.getPersentase() > 0.1) {
					Hbox hboxLampiran = (Hbox) row.getAttribute("hboxLampiran");
					hboxLampiran.setVisible(true);
					Double nom = pBulanan.getNominal();
					Double denda = 0.0;

					if (pBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
							.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
						for (MyDoubleboxMin kurang : pengurangan) {
							DetailBiaya db = (DetailBiaya) kurang.getAttribute("itemBiaya");
							if (db != null && db.getId().equals(pBulanan.getDetailBiaya().getId())) {
								nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
								break;
							}
						}
					} else {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim()
										.contains("," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran
												: null;
						denda = pBulanan.checkDenda(nom, ais.ui.util.WaktuUtil.getDate(), jdw, jenisKegiatan) - nom;
					}

					MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
					if (buttonHapus != null)
						buttonHapus.setVisible(!jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());

					cp.setDenda(denda);
					cp.setPengaturanPembayaranBulanan(pBulanan);
					final MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					jumlahCicilan.setValue(nom + denda);
					cp.setNilai(nom + denda);
					cp.setNilaiAsli(cp.getNilai());

					final Textbox ketBox = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					final MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
					final MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
					tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					tanggal.setDisabled(false);
					tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
					tanggalKwitansi.setDisabled(false);

					final CicilanPembayaran tempCp = cp;
					tanggal.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Double n = pBulanan.getNominal();
							Double d = 0.0;
							if (pBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								for (MyDoubleboxMin kurang : pengurangan) {
									DetailBiaya db = (DetailBiaya) kurang.getAttribute("itemBiaya");
									if (db != null && db.getId().equals(pBulanan.getDetailBiaya().getId())) {
										n = kurang.getValue() == null ? 0.0 : kurang.getValue();
										break;
									}
								}
							} else {
								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains(
												"," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran : null;
								d = pBulanan.checkDenda(n, tanggal.getValue(), jdw, jenisKegiatan) - n;
							}
							tempCp.setValidator(tbmuser == null ? "" : tbmuser.getUserId());
							tempCp.setDenda(d);
							tempCp.setPengaturanPembayaranBulanan(pBulanan);
							tempCp.setTanggal(tanggal.getValue());
							tempCp.setTanggalKwitansi(tanggalKwitansi.getValue());
							ketBox.setValue(tempCp.getKeterangan());
							ketBox.setDisabled(false);
							jumlahCicilan.setValue(n + d);
							row.setAttribute("cicilanPembayaran", tempCp);
						}
					});

					Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
					Common.selectComboItem(myItemBiaya, pBulanan);
					Combobox myCaraBayar = (Combobox) row.getAttribute("caraBayar");
					ketBox.setRows(2);
					ketBox.setValue(cp.getKeterangan());
					ketBox.setDisabled(false);

					String val = cp.getValidator();
					if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))
						val = (tbmuser == null ? "" : tbmuser.toString());

					cp.setValidator(val);
					cp.setKegiatan(kegiatan);
					cp.setKeterangan(ketBox.getValue());
					cp.setBayarKe(pBulanan.getDetailBiaya().getBayarKe());
					cp.setItemBiaya(pBulanan.getDetailBiaya().getItemBiaya());
					cp.setPengaturanPembayaranBulanan(pBulanan);
					cp.setNilai(jumlahCicilan.getValue());
					cp.setTanggal(tanggal.getValue());
					cp.setTanggalKwitansi(tanggalKwitansi.getValue());
					cp.setJenisPembayaran(
							(JenisPembayaran) (myCaraBayar.getSelectedItem() == null ? ConstantValues.TUNAI
									: myCaraBayar.getSelectedItem().getValue()));

					row.setAttribute("cicilanPembayaran", cp);
					cicilanPembayarans.add(cp);
					i++;

					row.setAttribute("jumlahCicilan", jumlahCicilan);
					row.setAttribute("tanggal", tanggal);
					row.setAttribute("tanggalKwitansi", tanggalKwitansi);
					row.setAttribute("itemBiaya", myItemBiaya);
					row.setAttribute("caraBayar", myCaraBayar);
					row.setAttribute("keterangan", ketBox);
					row.setVisible(true);
					if (tidakBolehUbah)
						Common.freeze(row, true);
				}
			}
		}
		jumlahCicilahEventListener.onEvent(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void listBiaya(final BiodataCalonMahasiswa calonMahasiswa, final Kegiatan kegiatan,
			final JenisKegiatan jenisKegiatan) throws Exception {
		gridCicilan = new MyGrid();
		if (kegiatan != null && kegiatan.getValidator() != null)
			validator.setValue(kegiatan.getValidator());

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		this.calonMahasiswa = calonMahasiswa;
		this.jenisKegiatan = jenisKegiatan;

		final ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(rowListBiaya);

		Common.insertCombo(akun = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		hboxJenisPembayaran = new Hbox(new Component[] { new Label(ais.common.Common.getBahasaConfig("Cara Pembayaran : ")), akun });
		hboxJenisPembayaran.setVisible(false);
		akun.setCols(50);
		hboxJenisPembayaran.setParent(groupbox);

		Hbox btn = new Hbox();
		btn.setParent(groupbox);
		// Wadah info "mode tagihan" (BULANAN vs BUKAN BULANAN) — sengaja dibuat KOSONG di
		// sini agar posisinya tepat di bawah baris tombol; isinya diisi belakangan oleh
		// isiInfoModeTagihan() setelah countPengaturanBulanan & dataTagihanData termuat.
		final ais.ui.util.MyDiv infoModeTagihan = new ais.ui.util.MyDiv();
		infoModeTagihan.setParent(groupbox);
		Button button = new MyToolbarbuttonConfig("Lihat Tagihan", "/img/Finance-Invoice-icon.png");
		button.setParent(btn);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// PERBAIKAN (data mahasiswa tertukar saat "Lihat Tagihan"): sebelumnya dibuka via
				// IFRAME + URL query-string (?calonMahasiswa=<id>), dengan alasan awal: konten
				// InformasiPembayaran yang BERAT (chart spider SVG + dasbor + riwayat) perlu dimuat
				// sebagai halaman TERPISAH agar respons AJAX dari KLIK ini tetap kecil (versi inline
				// lama sempat memasukkan seluruh render berat ke SATU respons AU yang bisa terpotong
				// proxy -> popup "server out of service"). Sekarang dipanggil LANGSUNG sebagai method
				// static (InformasiPembayaranMahasiswaAction.onViewExternal, pola sama dgn
				// SetingBiayaAction.onAddExternal) yang mengoper objek calonMahasiswa APA ADANYA
				// (bukan di-serialisasi ke ID di URL lalu di-parse ulang) -- menghilangkan celah ID
				// salah ter-embed di URL yang dicurigai sbg penyebab data mahasiswa tertukar. Render
				// beratnya TETAP di-defer lewat AsyncTaskManager (timer terpisah, sama seperti alur
				// .zul lama) sehingga respons klik tombol ini tetap kecil -- risiko "response terlalu
				// besar" yang jadi alasan iframe dulu TIDAK kembali muncul.
				ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(null, calonMahasiswa,
					jenisKegiatan);
			}
		});

		if (kegiatan == null || kegiatan.getId() == null) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(final Event arg0) throws Exception {
					try {
						detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event a) throws Exception {
							refresh = true;
							onCariMahasiswa(arg0);
						}
					});
				}
			}, "Loading tagihan..", false, 2000);
		}

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setParent(btn);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event arg0) throws Exception {
				try {
					detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						refresh = true;
						onCariMahasiswa(arg0);
					}
				});
			}
		});

		buttonReset = new MyToolbarbuttonConfig("Reset", "/img/Business-Process-icon.png");
		buttonReset.setParent(btn);
		buttonReset.setAttribute("janganDisabled", true);
		buttonReset.setVisible(kegiatan != null && kegiatan.getId() != null && tbmuser != null
				&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		buttonReset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin mengembalikan tagihan ini ke tagihan awal (default) sesuai billing pembayaran? Nilai tagihan akan disesuaikan kembali ke ketentuan awal. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
						"Konfirmasi Pengembalian Tagihan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
									try {
										detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg01) throws Exception {
											Session session = null;
											Transaction tx = null;
											try {
												session = HibernateUtil.openSession();
												tx = session.beginTransaction();
												KegiatanHelper.checkKegiatanCalonMahasiswa(kegiatan,
														kegiatan.getJenisKegiatan(), calonMahasiswa,
														kegiatan.getSemster(), kegiatan.getTahunAkademik(), true,
														kegiatan.getJadwalPembayaran(), true, false, null, session);
												tx.commit();
											} catch (Exception e) {
												try {
													if (tx != null && tx.isActive()) {
														tx.rollback();
													}
												} catch (Exception ex) {
													Common.tampilErrorJikaAdmin(ex);
												}
												Common.tampilErrorJikaAdmin(e);
											} finally {
												closeOpenedSession(session);
											}
											refresh = true;
											onCariMahasiswa(arg0);
										}
									});
								}
							}
						});
			}
		});

		sesuaikanDenganTagihan = new MyToolbarbuttonConfig("Pilih Semua", "/img/svg/check2.svg");
		sesuaikanDenganTagihan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				inputSesuaiTagihan();
			}
		});
		btn.appendChild(sesuaikanDenganTagihan);

		sesuaikanDenganTagihanBulanan = new MyToolbarbuttonConfig("Pilih Pemua", "/img/svg/check2.svg");
		sesuaikanDenganTagihanBulanan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				inputSesuaiTagihanBulanan(null);
			}
		});
		btn.appendChild(sesuaikanDenganTagihanBulanan);

		if (kegiatan != null && kegiatan.getId() != null && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null) {
			RevisiHelper.createNewRevisi(Kegiatan.class, kegiatan, "History").setParent(btn);
		}

		// Tombol Wizard: buka Wizard Pembayaran 5-langkah mandiri (WizardPembayaranMhsHelper).
		// Hanya tampil bila calon sudah memiliki entitas Mahasiswa (wizard bekerja per Mahasiswa);
		// tagihan dimuat ulang saat wizard ditutup.
		// Gerbang ON/OFF: Konfigurasi > Pembayaran Mahasiswa > "Wizard Pembayaran Mahasiswa".
		if (calonMahasiswa != null && calonMahasiswa.getMahasiswa() != null
				&& calonMahasiswa.getMahasiswa().getId() != null
				&& ais.action.master.helper.WizardPembayaranMhsHelper.aktif()) {
			final Mahasiswa mahasiswaWizard = calonMahasiswa.getMahasiswa();
			MyToolbarbuttonConfig btnWizardBayar = new MyToolbarbuttonConfig("Wizard", "/img/Finance-Invoice-icon.png");
			btnWizardBayar.setAttribute("janganDisabled", true);
			btnWizardBayar.setTooltiptext("Buka Wizard Pembayaran - bayar tagihan langkah demi langkah");
			btnWizardBayar.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.action.master.helper.WizardPembayaranMhsHelper.buka(mahasiswaWizard, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event a) throws Exception {
									onCariMahasiswa(a);
								}
							});
						}
					});
				}
			});
			btnWizardBayar.setParent(btn);
		}

		gridss = new MyGrid();
		gridss.setMold("paging");
		gridss.setPageSize(1000);
		gridss.setParent(groupbox);
		gridss.setWidth("95%");
		// du-nowarna: matikan pewarnaan status (belang) → tampil default (lihat css_utama).
		gridss.setSclass("dgrid du-nowarna");

		Columns columns = new Columns();
		columns.setParent(gridss);
		MyColumnConfig col1 = new MyColumnConfig();
		col1.setParent(columns);
		col1.setLabel("Item Biaya");
		MyColumnConfig col2 = new MyColumnConfig();
		col2.setParent(columns);
		col2.setLabel("Tagihan");
		col2.setWidth("22%");
		col2.setAlign("right");
		MyColumnConfig col3 = new MyColumnConfig();
		col3.setParent(columns);
		col3.setLabel("Dibayar");
		col3.setWidth("18%");
		col3.setAlign("right");
		MyColumnConfig col4 = new MyColumnConfig();
		col4.setParent(columns);
		col4.setLabel("Kekurangan");
		col4.setWidth("18%");
		col4.setAlign("right");

		Double sumBiaya = 0.0;
		Foot foot = new Foot();
		foot.setParent(gridss);

		Footer footer1 = new Footer();
		footer1.setParent(foot);
		labelFooterItemBiaya = new MyLabelBoldAja();
		labelFooterItemBiaya.setParent(footer1);
		labelFooterItemBiaya.setValue("Total");

		Footer footer2 = new Footer();
		footer2.setParent(foot);
		labelFooterTagihan = new MyLabelBoldAja();
		labelFooterTagihan.setParent(footer2);
		labelFooterTagihan.setStyle("font-weight: bold;text-align: right;");
		labelFooterTagihan.setWidth("100%");

		Footer footer3 = new Footer();
		footer3.setParent(foot);
		labelFooterDibayarAja = new MyLabelBoldAja();
		labelFooterDibayarAja.setParent(footer3);
		labelFooterDibayarAja.setValue(sumBiaya.toString());
		labelFooterDibayarAja.setStyle("font-weight: bold;text-align: right;");
		labelFooterDibayarAja.setWidth("100%");

		Footer footer4 = new Footer();
		footer4.setParent(foot);
		labelFooterKekurangan = new MyLabelBoldAja();
		labelFooterKekurangan.setParent(footer4);
		labelFooterKekurangan.setStyle("font-weight: bold;text-align: right;");
		labelFooterKekurangan.setWidth("100%");

		Vbox vbox = new Vbox();
		vbox.setParent(groupbox);
		terbilang = new Label();
		terbilang.setParent(vbox);
		terbilang.setWidth("100%");
		terbilang.setVisible(false);
		terbilangTagihan = new Label();
		terbilangTagihan.setParent(vbox);
		terbilangTagihan.setWidth("100%");
		terbilangTagihan.setVisible(false);
		terbilangSisa = new Label();
		terbilangSisa.setParent(vbox);
		terbilangSisa.setWidth("100%");
		terbilangSisa.setVisible(false);
		terbilangSisaPersen = new Label();
		terbilangSisaPersen.setParent(vbox);
		terbilangSisaPersen.setWidth("100%");
		terbilangSisaPersen.setVisible(false);

		if (jenisKegiatan != null) {
			Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
			itemBiayas = new HashMap<Long, DetailBiaya>();

			if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
				Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
				detailBiayas = new ArrayList();
				if (prodiLulus == null || prodiLulus.getId() == null) {
					Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
							: calonMahasiswa.getProdi1();
					detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan,
							myjurusan1, smt, refresh));
				} else {
					detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan,
							prodiLulus, smt, refresh));
				}
			} else if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
				Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
				detailBiayas = new ArrayList<DetailBiaya>();
				if (prodiLulus == null || prodiLulus.getId() == null) {
					Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
							: calonMahasiswa.getProdi1();
					detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan,
							myjurusan1, refresh));
				} else {
					detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan,
							prodiLulus, refresh));
				}
			}

			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = (DetailBiaya) o;
				if (detailBiaya != null && detailBiaya.getItemBiaya() != null)
					itemBiayas.put(detailBiaya.getId(), detailBiaya);
			}

			Session session = null;
			Collection biayaBulanan = null;
			try {
				session = HibernateUtil.currentSession();
				PembayaranUtil.getInstance();
				countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa, jenisKegiatan, smt,
						detailBiayas, refresh, false);
				if (countPengaturanBulanan > 0) {
					biayaBulanan = pembayaranUtil.getPengaturanPembayaranSemua(calonMahasiswa, session, smt,
							jenisKegiatan, detailBiayas, refresh, false);
				}

				// PENYEMBUHAN-DIRI: muatan pertama membaca cache (refresh=false); cache basi
				// dapat membuat KEDUA varian kosong padahal pengaturan billing/bulanan ada di
				// tabel. Hitung ulang SEKALI langsung dari database sebelum menyerah, sehingga
				// petugas tidak perlu menekan Refresh manual untuk melihat tagihan.
				if (!refresh && (detailBiayas == null || detailBiayas.isEmpty())
						&& (biayaBulanan == null || biayaBulanan.isEmpty())) {
					countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa, jenisKegiatan,
							smt, detailBiayas, true, false);
					if (countPengaturanBulanan > 0) {
						biayaBulanan = pembayaranUtil.getPengaturanPembayaranSemua(calonMahasiswa, session, smt,
								jenisKegiatan, detailBiayas, true, false);
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			pengurangan = new ArrayList<MyDoubleboxMin>();
			Collection ooo = (biayaBulanan != null ? biayaBulanan : detailBiayas);
			dataTagihanData = new ArrayList(ooo);
			// Benteng terakhir: bila tetap kosong, susun tampilan dari riwayat cicilan yang
			// pernah terbayar agar posisi pembayaran calon mahasiswa tetap terlihat.
			if (dataTagihanData.isEmpty())
				PembayaranUtilHelper.fallbackTagihanDariCicilan(calonMahasiswa, jenisKegiatan, dataTagihanData,
						itemBiayas, smt);
			Collections.sort(dataTagihanData);
			isiInfoModeTagihan(infoModeTagihan, smt);
			ListModel strset = new SimpleListModel(dataTagihanData);

			Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
					: kegiatan.ambilDetailKegiatan(refresh);
			if (kegiatan != null)
				kegiatan.resetTagihans();

			Integer tahunAngkatanMhs = calonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(Integer.parseInt(semester.getValue()),
					tahunAngkatanMhs, semesterMulai, calonMahasiswa.getSemesterMulai());
			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

			gridss.setRowRenderer(detailPembayaranMahasiswaRenderer = new DetailPembayaranMahasiswaRenderer(kegiatan,
					jadwalPembayaran, labelFooterTagihan, labelFooterDibayarAja, labelFooterKekurangan, terbilang,
					terbilangTagihan, terbilangSisa, terbilangSisaPersen, pengurangan, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (arg0 != null && arg0.getData() != null && arg0.getData() instanceof Kegiatan)
								DaftarUlangMahasiswaBaruAction.this.kegiatan = (Kegiatan) arg0.getData();
							hitungJumlahBiayaSeharusnya();
						}
					}, gridCicilan, null, calonMahasiswa, smt, tahunAkademik, dataTagihan = new HashMap<Long, Double>(),
					gridss, detailKegiatans, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event a) throws Exception {
									refresh = true;
									onCariMahasiswa(a);
								}
							});
						}
					}));
			gridss.setModel(strset);

			for (MyDoubleboxMin kurang : pengurangan) {
				kurang.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						hitungJumlahBiayaSeharusnya();
					}
				});
			}
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				listCicilan(kegiatan, refresh);
				if (biodataCalonMahasiswaAktif != null)
					Common.freeze(gridss, true);
				hitungJumlahBiayaSeharusnya();
			}
		});

		sesuaikanDenganTagihan.setVisible(countPengaturanBulanan == 0);
		sesuaikanDenganTagihanBulanan.setVisible(countPengaturanBulanan > 0);
	}

	/**
	 * Mengisi lencana informasi <b>mode tagihan</b> pada panel "Daftar Biaya" calon
	 * mahasiswa — kembaran {@code DaftarUlangMahasiswaLamaAction.isiInfoModeTagihan}
	 * dengan penyesuaian entitas ({@link BiodataCalonMahasiswa}). Konfigurasi billing di
	 * Setting Biaya berlaku per semester/jenis (bisa bulanan, bisa sekali tagih), namun
	 * layar tidak pernah memberi tahu mode mana yang sedang berlaku, sehingga daftar
	 * tagihan yang kosong atau berbentuk bulanan mudah disangka error oleh petugas.
	 * Lencana ini menjelaskannya dengan bahasa sehari-hari dalam tiga keadaan: BULANAN /
	 * ANGSURAN (chip biru, tagihan dipecah per bulan), BUKAN BULANAN (chip hijau, sekali
	 * tagih namun nominal boleh dibayar bertahap), dan BELUM ADA TAGIHAN (chip kuning,
	 * arahkan pengguna memeriksa Setting Biaya atau menekan Refresh). Gaya visual murni
	 * HTML+CSS (chip membulat, {@code max-width:100%}) agar rapi di desktop maupun layar
	 * mobile tanpa library tambahan; kegagalan render dicatat ke ErrorLog dan tidak boleh
	 * menggagalkan pemuatan daftar biaya.
	 *
	 * @param wadah wadah kosong tepat di bawah baris tombol (Lihat Tagihan / dst)
	 * @param smt   semester yang sedang dipilih
	 */
	private void isiInfoModeTagihan(org.zkoss.zk.ui.Component wadah, int smt) {
		try {
			if (wadah == null)
				return;
			Common.clear(wadah);
			boolean bulanan = countPengaturanBulanan > 0;
			int jumlahBaris = dataTagihanData == null ? 0 : dataTagihanData.size();
			int jumlahBulan = 0;
			if (dataTagihanData != null) {
				for (Object o : dataTagihanData) {
					if (o instanceof ais.database.model.PengaturanPembayaranBulanan)
						jumlahBulan++;
				}
			}

			String warnaLatar, warnaTeks, ikon, judul, keterangan;
			String diagnosa = diagnosaTagihanTidakMuncul(smt, jumlahBaris);
			if (jumlahBaris == 0) {
				warnaLatar = "#fef9c3";
				warnaTeks = "#854d0e";
				ikon = "&#9888;";
				judul = "Belum ada tagihan untuk periode ini";
				keterangan = "Tagihan belum dibuat di menu Setting Biaya untuk jenis pembayaran ini,"
						+ " atau seluruh tagihan sudah lunas. Tekan tombol Refresh untuk memuat ulang,"
						+ " atau periksa pengaturan billing-nya.";
			} else if (bulanan) {
				warnaLatar = "#e0f2fe";
				warnaTeks = "#075985";
				ikon = "&#128197;";
				judul = "Tagihan: BULANAN / ANGSURAN";
				keterangan = "Tagihan dibagi menjadi "
						+ (jumlahBulan > 0 ? jumlahBulan + " bulan/angsuran" : "beberapa bulan/angsuran")
						+ ". Bayar sesuai bulan atau angsuran yang jatuh tempo pada daftar di bawah.";
			} else {
				warnaLatar = "#dcfce7";
				warnaTeks = "#166534";
				ikon = "&#128181;";
				judul = "Tagihan: BUKAN BULANAN (sekali tagih)";
				keterangan = "Tagihan ditagih satu kali (tidak dipecah per bulan)."
						+ " Nominalnya tetap boleh dibayar bertahap saat melakukan pembayaran.";
				if (diagnosa.length() > 0) {
					warnaLatar = "#fef9c3";
					warnaTeks = "#854d0e";
					ikon = "&#9888;";
					judul = "Tagihan perlu diperiksa";
				}
			}

			Html chip = new Html("<div style=\"display:inline-block;max-width:100%;box-sizing:border-box;"
					+ "margin:4px 0 6px 0;padding:8px 14px;border-radius:10px;background:" + warnaLatar + ";"
					+ "color:" + warnaTeks + ";font-family:'Segoe UI',Arial,sans-serif;line-height:1.5;\">"
					+ "<span style=\"font-weight:700;font-size:12px;letter-spacing:.2px;\">" + ikon + " "
					+ judul + "</span>"
					+ "<div style=\"font-size:11.5px;opacity:.92;margin-top:2px;\">" + keterangan + "</div>"
					+ diagnosa
					+ "</div>");
			chip.setParent(wadah);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaBaruAction: gagal render info mode tagihan;"
					+ " calon=" + (calonMahasiswa == null ? null : calonMahasiswa.getId()) + ", smt=" + smt);
		}
	}

	private String diagnosaTagihanTidakMuncul(int smt, int jumlahBaris) {
		try {
			if (jumlahBaris != 0 && !totalTagihanTampilNol()) {
				return "";
			}
			List<String> alasan = new ArrayList<String>();
			if (jenisKegiatan != null && diLuarRangeTagihan(smt, jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt())) {
				alasan.add("Jenis kegiatan \"" + escHtmlTagihan(jenisKegiatan.getNamaKegiatan())
						+ "\" hanya berlaku semester " + rangeTextTagihan(jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt()) + ".");
			}
			List<DetailBiaya> kandidat = kumpulkanDetailBiayaDiagnosa();
			for (DetailBiaya db : kandidat) {
				if (db == null) continue;
				String nama = namaDetailBiayaDiagnosa(db);
				try {
					if (db.getItemBiaya() != null
							&& diLuarRangeTagihan(smt, db.getItemBiaya().getMinSmt(), db.getItemBiaya().getMaxSmt())) {
						alasan.add("Item biaya \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getItemBiaya().getMinSmt(), db.getItemBiaya().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangBaru:diagnosa item biaya"); }
				try {
					if (db.getSettingBiaya() != null
							&& diLuarRangeTagihan(smt, db.getSettingBiaya().getMinSmt(), db.getSettingBiaya().getMaxSmt())) {
						alasan.add("Setting biaya untuk \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getSettingBiaya().getMinSmt(), db.getSettingBiaya().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangBaru:diagnosa setting biaya"); }
				try {
					if (db.getSettingBiayaDetail() != null
							&& diLuarRangeTagihan(smt, db.getSettingBiayaDetail().getMinSmt(), db.getSettingBiayaDetail().getMaxSmt())) {
						alasan.add("Detail setting biaya untuk \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getSettingBiayaDetail().getMinSmt(), db.getSettingBiayaDetail().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangBaru:diagnosa detail setting biaya"); }
			}
			if (alasan.isEmpty()) {
				alasan.add("Tidak ada baris tagihan aktif yang cocok dengan kombinasi semester, jenis pembayaran, prodi/jenjang, angkatan, status awal, jenis seleksi, paket/gelombang, atau tagihan sudah lunas.");
			}
			StringBuffer sb = new StringBuffer();
			sb.append("<div style=\"margin-top:6px;font-size:11.5px;opacity:.96;\"><b>Kemungkinan penyebab:</b><ul style=\"margin:3px 0 0 18px;padding:0;\">");
			for (int i = 0; i < alasan.size() && i < 5; i++) {
				sb.append("<li>").append(alasan.get(i)).append("</li>");
			}
			sb.append("</ul></div>");
			return sb.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaBaruAction: diagnosa tagihan gagal");
			return "";
		}
	}

	private boolean totalTagihanTampilNol() {
		double total = 0.0;
		if (dataTagihanData != null) {
			for (Object o : dataTagihanData) {
				try {
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) o;
						total += p.getNominal() == null ? 0.0 : p.getNominal().doubleValue();
					} else if (o instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) o;
						Double n = db.getNilaiBiayaBaru() == null ? db.getNilaiBiaya() : db.getNilaiBiayaBaru();
						total += n == null ? 0.0 : n.doubleValue();
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangBaru:total tagihan tampil"); }
			}
		}
		return total <= 0.01;
	}

	@SuppressWarnings("unchecked")
	private List<DetailBiaya> kumpulkanDetailBiayaDiagnosa() {
		List<DetailBiaya> hasil = new ArrayList<DetailBiaya>();
		if (dataTagihanData != null) {
			for (Object o : dataTagihanData) {
				try {
					if (o instanceof PengaturanPembayaranBulanan) {
						DetailBiaya db = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
						if (db != null && !hasil.contains(db)) hasil.add(db);
					} else if (o instanceof DetailBiaya && !hasil.contains(o)) {
						hasil.add((DetailBiaya) o);
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangBaru:kumpul detail diagnosa"); }
			}
		}
		if (!hasil.isEmpty() || jenisKegiatan == null || jenisKegiatan.getId() == null) {
			return hasil;
		}
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			hasil.addAll(s.createCriteria(DetailBiaya.class)
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id"))
					.setMaxResults(20).list());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaBaruAction: query diagnosa tagihan");
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return hasil;
	}

	private boolean diLuarRangeTagihan(int smt, Integer min, Integer max) {
		int mi = min == null ? 0 : min.intValue();
		int ma = max == null ? 30 : max.intValue();
		return smt < mi || smt > ma;
	}

	private String rangeTextTagihan(Integer min, Integer max) {
		return (min == null ? "0" : min.toString()) + " s.d. " + (max == null ? "30" : max.toString());
	}

	private String namaDetailBiayaDiagnosa(DetailBiaya db) {
		try {
			return db.getItemBiaya() == null ? "Tanpa nama" : db.getItemBiaya().getNama();
		} catch (Exception e) {
			return "Tanpa nama";
		}
	}

	private String escHtmlTagihan(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private boolean validasiPembayaran(Kegiatan kegiatan, final BiodataCalonMahasiswa calonMahasiswa) throws Exception {
		return validasiPembayaran(kegiatan, calonMahasiswa, null);
	}

	@SuppressWarnings("unchecked")
	private boolean validasiPembayaran(Kegiatan kegiatan, final BiodataCalonMahasiswa calonMahasiswa,
			JenisPembayaran tabungan) throws Exception {
		if (gridss != null && gridss.getRows() != null && gridss.getRows().getChildren().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, data tagihan tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang data mahasiswa; (2) pastikan mahasiswa telah memiliki tagihan pada semester yang dipilih; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (gridCicilan != null) {
			Double jumlah = 0.0;
			List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
			Map<Long, Object[]> pBulanans = new HashMap<Long, Object[]>();
			double totalDeposit = 0.0;

			for (Row row : mycicilanrows) {
				try {
					MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
					Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
					JenisPembayaran jp = (JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
							? ConstantValues.TUNAI
							: myJenisPembayaran.getSelectedItem().getValue());

					if (tabungan != null)
						jp = tabungan;

					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					Double c = (jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
					CicilanPembayaran cp = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");

					if (cp != null && cp.getId() == null && jp != null && jp.getJenisTabungan() != null)
						totalDeposit += c;

					if (cp != null && cp.getId() == null && cp.getPengaturanPembayaranBulanan() != null) {
						PengaturanPembayaranBulanan pb = cp.getPengaturanPembayaranBulanan();
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim()
										.contains("," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran
												: null;

						if (pBulanans.containsKey(pb.getId())) {
							Double nom = ((Double) pBulanans.get(pb.getId())[0]) + c;
							Double denda = pb.checkDenda(nom, tanggal.getValue(), jdw, jenisKegiatan) - nom;
							pBulanans.put(pb.getId(), new Object[] { nom, denda, pb });
						} else {
							Double denda = pb.checkDenda(c, tanggal.getValue(), jdw, jenisKegiatan) - c;
							pBulanans.put(pb.getId(), new Object[] { c, denda, pb });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (totalDeposit > 0.1 && calonMahasiswa.getMahasiswa() != null) {
				Double nilaiDepositmahasiswa = ais.action.master.sekolah.util.DepositHelper
						.hitungDeposit(calonMahasiswa.getMahasiswa());
				if (nilaiDepositmahasiswa < totalDeposit) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, nilai deposit tidak mencukupi untuk melakukan pembayaran ini. Nilai pembayaran melalui deposit adalah {V1}, sedangkan sisa deposit yang tersedia hanya {V2}. Langkah yang dapat dilakukan: (1) kurangi nilai pembayaran yang mengambil dari deposit; (2) tambahkan saldo deposit terlebih dahulu; (3) gunakan metode pembayaran lain yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							Common.numberFormat.get().format(totalDeposit),
							Common.numberFormat.get().format(nilaiDepositmahasiswa));
					return false;
				}
			}

			if (Common.bolehKonfigurasi("check_apakah_melebihi_tagihan", Konfigurasi.TIDAK_AKTIF)) {
				for (Long k : pBulanans.keySet()) {
					PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) pBulanans.get(k)[2];
					Double nom = pb.getNominal();
					Double denda = (Double) pBulanans.get(k)[1];
					Double n = (Double) pBulanans.get(k)[0];
					if ((nom + denda) < n) {
						String namaIBVal = "";
						try {
							if (pb.getDetailBiaya() != null && pb.getDetailBiaya().getItemBiaya() != null)
								namaIBVal = pb.getDetailBiaya().getItemBiaya().getNama();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaBaruAction.java:2907");}
						MyMessageboxConfig.showFormat(
								"Mohon maaf, nilai pembayaran untuk item biaya \"{V1}\" pada bulan {V2} tidak boleh melebihi nilai tagihan. Nilai tagihan adalah {V3}, sedangkan nominal pembayaran yang dimasukkan adalah {V4}. Langkah yang dapat dilakukan: (1) periksa kembali nominal pembayaran yang dimasukkan; (2) sesuaikan agar tidak melebihi nilai tagihan; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								namaIBVal, pb.getNamaBulan(),
								Common.numberFormat.get().format(nom),
								Common.numberFormat.get().format(n));
						return false;
					}
				}
			}

			for (Row row : mycicilanrows) {
				if (Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF) && tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					if (jumlahCicilan.getValue() != null && Math.abs(jumlahCicilan.getValue()) > 0.1) {
						Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
						if (!myJenisPembayaran.isDisabled() && myJenisPembayaran.getSelectedItem() == null) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu memilih Cara Bayar terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Cara Bayar; (2) pilih cara pembayaran yang sesuai; (3) lanjutkan kembali proses penyimpanan.",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							myJenisPembayaran.focus();
							return false;
						}
					}
				}

				if (Common.bolehKonfigurasi("harus_menyertakan_bukti_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					if (jumlahCicilan.getValue() != null && Math.abs(jumlahCicilan.getValue()) > 0.01) {
						CicilanPembayaran cpSebelumnya = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
						if (cpSebelumnya.getIdLampiran() == null) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu melengkapi bukti pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) siapkan berkas bukti pembayaran dalam bentuk gambar atau PDF; (2) tekan tombol unggah dan pilih berkas tersebut; (3) lanjutkan kembali proses penyimpanan.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return false;
						}
					}
				}

				Combobox itemBiaya = (Combobox) row.getAttribute("itemBiaya");
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
				if ((jumlahCicilan.getValue() == null ? 0.0 : Math.abs(jumlahCicilan.getValue())) > 0.1
						&& !itemBiaya.isDisabled() && itemBiaya.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon Bapak/Ibu memilih Item Biaya terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Item Biaya; (2) pilih item biaya yang akan dibayar; (3) lanjutkan kembali proses penyimpanan.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					itemBiaya.focus();
					return false;
				}
				jumlah += (jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
			}
		}

		if (hboxJenisPembayaran.isVisible() && akun.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu memilih Cara Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Cara Pembayaran; (2) pilih cara pembayaran yang sesuai; (3) lanjutkan kembali proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		hitungJumlahBiayaSeharusnya();
		return true;
	}

	@SuppressWarnings({ "unchecked" })
	/*
	 * ===== PENGAMAN ANTI-PEMBAYARAN GANDA (double submit) =====
	 * Sama seperti di DaftarUlangMahasiswaLamaAction: tanpa pengaman, menekan
	 * BAYAR dua kali (bahkan berselang beberapa menit) untuk item & nominal yang
	 * sama membuat record CicilanPembayaran GANDA. Guard menolak pembayaran
	 * ber-signature sama dalam rentang cooldown + flag in-flight.
	 */
	private volatile boolean bayarSedangDiproses = false;
	private String lastBayarSignature = null;
	private long lastBayarTime = 0L;
	/** Pelewat sekali-pakai guard ganda: disetel true bila admin menegaskan lewat konfirmasi. */
	private boolean lewatiGuardGanda = false;

	public boolean onSave(Kegiatan keg, final BiodataCalonMahasiswa calonMahasiswa, Event event,
			JenisPembayaran tabungan) throws Exception {
		if (tbmuser == null
				|| (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)))
			return false;
		kegiatan = keg;
		if (!validasiPembayaran(keg, calonMahasiswa, tabungan))
			return false;

		// Pengaman anti-pembayaran ganda (lihat field & komentar di atas onSave).
		final String bayarSignature = buildBayarSignature(keg);
		final long bayarSekarang = System.currentTimeMillis();
		if (bayarSedangDiproses) {
			MyMessageboxConfig.show(
					"Pembayaran sedang diproses. Mohon tunggu sampai selesai dan jangan menekan tombol bayar berulang kali.",
					"Mohon Tunggu", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (bayarSignature != null && bayarSignature.equals(lastBayarSignature)
				&& (bayarSekarang - lastBayarTime) < getBayarCooldownMs() && !lewatiGuardGanda) {
			// BUKAN blokir keras: pembayaran sah (mis. angsuran berikutnya dgn nominal sama)
			// tak boleh terkunci sampai 5 menit. Tampilkan KONFIRMASI; bila admin menegaskan
			// OK, ulangi onSave dgn melewati guard sekali-pakai. Klik ganda cepat tetap
			// dicegah oleh flag in-flight bayarSedangDiproses di atas.
			final Kegiatan kegKonfirmasi = keg;
			final BiodataCalonMahasiswa calonKonfirmasi = calonMahasiswa;
			final Event eventKonfirmasi = event;
			final JenisPembayaran tabunganKonfirmasi = tabungan;
			MyMessageboxConfig.show(
					"Pembayaran dengan rincian (item & nominal) yang sama baru saja diproses.\n\n"
							+ "Bila ini BENAR-BENAR pembayaran berbeda (mis. angsuran berikutnya), tekan OK untuk tetap menyimpan.\n"
							+ "Bila ragu, tekan Batal untuk mencegah pembayaran ganda.",
					"Konfirmasi Kemungkinan Pembayaran Ganda",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.EXCLAMATION,
					new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							int i = Integer.parseInt(ev.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								lewatiGuardGanda = true;
								onSave(kegKonfirmasi, calonKonfirmasi, eventKonfirmasi, tabunganKonfirmasi);
							}
						}
					});
			return false;
		}
		lewatiGuardGanda = false;
		bayarSedangDiproses = true;

		Session session = null;
		try {
			session = HibernateUtil.currentSession();
			this.kegiatan = keg;
			this.calonMahasiswa = calonMahasiswa;

			if (kegiatan != null && kegiatan.getId() != null)
				kegiatan = (Kegiatan) session.load(Kegiatan.class, kegiatan.getId());
			else
				kegiatan = new Kegiatan();

			Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
			kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			kegiatan.setMahasiswa(null);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setCalonMahasiswa(calonMahasiswa);
			kegiatan.setSemster(smt);
			kegiatan.setTahunAkademik(labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
			kegiatan.setTanggal(WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(tbmuser == null ? "" : tbmuser.getUserNama());

			Double totalpengurangan = 0.0;
			for (MyDoubleboxMin kurang : pengurangan)
				totalpengurangan += kurang.getValue() == null ? 0.0 : kurang.getValue();
			kegiatan.setPengurangan(totalpengurangan);
			kegiatan.setKeterangan(keterangan.getValue().trim());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());
			Common.refreshSaveOrUpdate(session, kegiatan);

			if (jumlahYangAkanDibayar > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(jumlahYangAkanDibayar);
				logPembayaran.setKeterangan("Pembayaran manual");
				logPembayaran.setValidator(tbmuser == null ? null : tbmuser.getUserNama());
				Common.refreshSaveOrUpdate(logPembayaran);
			}

			if (gridCicilan != null && kegiatan.getId() != null) {
				Double check = 0.0;
				List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
				for (Row row : mycicilanrows) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					check += Math.abs(jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
				}

				if (check >= 1.0) {
					int i = 1;
					for (Row row : mycicilanrows) {
						MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
						MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
						MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
						Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
						Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
						Textbox ketBox = (Textbox) ((row.getAttribute("keterangan") != null
								&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
										: null);

						if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
							JenisPembayaran jp = (JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
									? ConstantValues.TUNAI
									: myJenisPembayaran.getSelectedItem().getValue());
							if (tabungan != null)
								jp = tabungan;

							CicilanPembayaran cpSebelumnya = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
							Long idLampiran = (Long) row.getAttribute("idLampiran");
							BuktiPembayaran bPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");

							String val = cpSebelumnya == null ? null : cpSebelumnya.getValidator();
							if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))
								val = (tbmuser == null ? "" : tbmuser.toString());

							Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
									: myItemBiaya.getSelectedItem().getValue();
							PengaturanPembayaranBulanan pb = cpSebelumnya.getPengaturanPembayaranBulanan();
							ItemBiaya itemBiaya = cpSebelumnya.getItemBiaya();
							DetailBiaya detailBiaya = null;

							if (jenisBiaya instanceof PengaturanPembayaranBulanan) {
								pb = (PengaturanPembayaranBulanan) jenisBiaya;
								detailBiaya = pb.getDetailBiaya();
								itemBiaya = detailBiaya.getItemBiaya();
							} else if (jenisBiaya instanceof DetailBiaya) {
								detailBiaya = (DetailBiaya) jenisBiaya;
								itemBiaya = detailBiaya.getItemBiaya();
							}

							CicilanPembayaran cp = cpSebelumnya == null ? new CicilanPembayaran(detailBiaya)
									: cpSebelumnya;
							// CATATAN: idempotency adaKembarDiDb DIHAPUS. Atas permintaan, pembayaran
							// dengan nominal SAMA dengan cicilan yang sudah ada HARUS tetap bisa
							// disimpan. Proteksi klik-ganda cepat tetap oleh flag in-flight
							// bayarSedangDiproses; kemungkinan ganda dikonfirmasi via dialog di onSave.
							if (cp.getId() == null) {
								cp.setDetailBiaya(detailBiaya);
								cp.setBuktiPembayaran(bPembayaran);
								cp.setIdLampiran(cpSebelumnya == null || cpSebelumnya.getId() == null ? null
										: cpSebelumnya.getIdLampiran());
								cp.setValidator(val);
								cp.setKe(i);
								cp.setKegiatan(kegiatan);
								cp.setKeterangan(ketBox.getValue());
								cp.setItemBiaya(itemBiaya);
								cp.setPengaturanPembayaranBulanan(pb);
								cp.setBayarKe(detailBiaya == null ? 1 : detailBiaya.getBayarKe());
								if (tabungan != null)
									cp.setDeposit(jumlahCicilan.getValue());
								cp.setJenisTabungan(tabungan);
								cp.setNilai(jumlahCicilan.getValue());
								cp.setTanggal(tanggal.getValue());
								cp.setTanggalKwitansi(tanggalKwitansi.getValue());
								cp.setJenisPembayaran(jp);
								cp.setCicilanSebelumnya(cpSebelumnya == null ? null : cpSebelumnya.getId());
								cp.setDenda(cpSebelumnya == null || cpSebelumnya.getId() == null ? null
										: cpSebelumnya.getDenda());

								if (pb != null)
									cp.setNilaiAsli(pb.getNominal());

								LampiranLain lainMahasiswa = buktiPembayarans.get(idLampiran);
								if (lainMahasiswa != null)
									cp.setIdLampiran(lainMahasiswa.getId());

								if (cp.getId() == null)
									session.save(cp);
								else
									Common.refreshUpdate(session, cp);

								if (bPembayaran != null) {
									bPembayaran.setCicilanPembayaran(cp);
									Common.refreshUpdate(session, bPembayaran);
								}

								JenisPembayaran j = (JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
										? null
										: myJenisPembayaran.getSelectedItem().getValue());
								if (j != null && cp.getJenisPembayaran() != null && !j.getId().equals(cp.getId())) {
									cp.setJenisPembayaran(j);
									Common.refreshUpdate(session, cp);
								}
							}
							row.setAttribute("cicilanPembayaran", cp);
							i++;
						}
					}
				} else {
					Common.simpanCicilanDefaultTanpaSesseion(kegiatan, nilaiBiayaHarusDiBayars, WaktuUtil.getDate(),
							keterangan.getValue(), (JenisPembayaran) (akun.getSelectedItem() == null ? null
									: akun.getSelectedItem().getValue()),
							detailBiayas);
				}
			} else {
				Common.simpanCicilanDefaultTanpaSesseion(kegiatan, nilaiBiayaHarusDiBayars, WaktuUtil.getDate(),
						keterangan.getValue(),
						(JenisPembayaran) (akun.getSelectedItem() == null ? null : akun.getSelectedItem().getValue()),
						detailBiayas);
			}

			session.flush();
			Common.freeze(panelMencicil, true);

			Double[] d = kegiatan.hitungTotalDanDendaFromCicilan();
			Double jumlah = d[0];
			Double denda = d[1];
			kegiatan.setDenda(denda.doubleValue());
			kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
			kegiatan.setAmount(jumlah.doubleValue());

			Common.refreshUpdate(session, kegiatan);
			session.flush();

			if (Common.bolehKonfigurasi("cetak_bukti_pembayaran_setelah_proses_pembayaran")) {

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
						buktiPembayaran = null;
						onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
					}
				});
			} else {
				MyMessageboxConfig.show(
						"Alhamdulillah, pembayaran telah berhasil dilakukan dan tercatat pada sistem. Terima kasih atas pembayaran yang telah Bapak/Ibu lakukan.",
						"Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimerNoBusy(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										buktiPembayaran = null;
										onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
									}
								});
							}
						});
			}
			lastBayarSignature = bayarSignature;
			lastBayarTime = bayarSekarang;
			return true;
		} catch (Exception e) {
			MyMessageboxConfig.show(
					"Mohon maaf, proses validasi pembayaran tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan data pembayaran; (2) ulangi proses pembayaran beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			// Lepas flag in-flight agar pembayaran berikutnya (mis. setelah gagal/retry)
			// tetap bisa diproses.
			bayarSedangDiproses = false;
		}
	}

	public double hitungJumlahBiayaSeharusnya() throws ParseException {
		nilaiBiayaHarusDiBayars = detailPembayaranMahasiswaRenderer.hitungUlang();
		if (myspaceBayar != null && (tbmuser == null || tbmuser.getMahasiswa() != null)) {
			Common.freeze(myspaceBayar, Math.abs(nilaiBiayaHarusDiBayars) < 0.01);
		}
		return nilaiBiayaHarusDiBayars;
	}


	/**
	 * Helper class untuk menyederhanakan deklarasi ratusan baris event listener
	 * tombol bank
	 **/
	private abstract class BasePaymentFlow implements EventListener {
		private String namaBank;
		private MyButtonConfig button;
		private String configAdminFee;
		private boolean showAdminFee;

		public BasePaymentFlow(String namaBank, MyButtonConfig button, String configAdminFee, boolean showAdminFee) {
			this.namaBank = namaBank;
			this.button = button;
			this.configAdminFee = configAdminFee;
			this.showAdminFee = showAdminFee;
		}

		@Override
		public void onEvent(final Event event) throws Exception {
			if (!checkKondisiSebelumbayarBaru())
				return;
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!apakah0(true))
						return;
					double biayaAdmin = 0.0;
					if (configAdminFee != null) {
						try {
							biayaAdmin = Double.parseDouble(Common.getKonfigurasi(configAdminFee, "0.0").getNilai());
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

					if (namaBank.toUpperCase().contains("BNI") || namaBank.toUpperCase().contains("BSI")) {
						if (jumlahYangAkanDibayar < 0.01) {
							jumlahYangAkanDibayar = hitungJumlahYangAkanDibayarDariTampilan();
						}
					}

					final double finalBiayaAdmin = biayaAdmin;
					String message = "Mohon Bapak/Ibu memeriksa kembali rincian pembayaran melalui " + namaBank + " berikut:\n"
							+ "Mahasiswa : " + calonMahasiswa.getNama() + "\nJumlah total tagihan : "
							+ labelFooterTagihan.getValue() + "\nJumlah yang akan dibayar : "
							+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
							+ (showAdminFee && finalBiayaAdmin > 0.1
									? "\nBiaya administrasi : " + Common.numberFormat.get().format(finalBiayaAdmin)
											+ "\nTotal yang akan dibayar : "
											+ Common.numberFormat.get().format(jumlahYangAkanDibayar + finalBiayaAdmin)
									: "")
							+ "\nTerbilang : "
							+ IndonesianNumberToWords.convert((long) (jumlahYangAkanDibayar + finalBiayaAdmin));

					MyMessageboxConfig.show(message, "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(final Event msgEvent) throws Exception {
									if (Integer.parseInt(msgEvent.getData().toString()) == MyMessageboxConfig.OK) {
										if (!validasiPembayaran(kegiatan, calonMahasiswa))
											return;
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event timerEvent) throws Exception {
												String tahunAkademik = labelTahunAkademik.getValue() == null ? ""
														: labelTahunAkademik.getValue();
												Double totalpengurangan = 0.0;
												for (MyDoubleboxMin kurang : pengurangan)
													totalpengurangan += kurang.getValue() == null ? 0.0
															: kurang.getValue();
												executePayment(event, finalBiayaAdmin, tahunAkademik, totalpengurangan,
														jumlahYangAkanDibayar);
												Common.freeze(center, true);
												Common.freeze(panelMencicil, true);
												button.setDisabled(true);
											}
										}, "Proses pembayaran ..");
									}
								}
							});
				}
			}, "Harap tunggu", false, 1500);
		}

		protected abstract void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
				Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception;
	}

	private void menuBayar(Box spaceBayar) {
		if (!edit)
			return;
		boolean telahLunas = false;
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		this.myspaceBayar = spaceBayar;
		spaceBayar.setPack("center");
		spaceBayar.setAlign("center");
		// Tombol gateway pembayaran default ZK = abu-abu muda -> teks putih nyaris tak terbaca.
		// Beri kelas agar CSS memberi WARNA KONTRAS (lihat .ais-bayar-gateway-area di css_utama.css).
		spaceBayar.setSclass(("ais-bayar-gateway-area "
				+ (spaceBayar.getSclass() == null ? "" : spaceBayar.getSclass())).trim());

		if (keterangan != null)
			keterangan.setDisabled(
					tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null);

		if (!telahLunas) {

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_doku", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarViaDoku = new MyButtonConfig("BAYAR VIA DOKU", "/img/msc-logo.png");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";doku;")))
					spaceBayar.appendChild(bayarViaDoku);
				bayarViaDoku.addEventListener("onClick", new BasePaymentFlow("DOKU", bayarViaDoku, null, false) {
					@Override
					protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
							Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
						DokuCommon.onSaveDoku(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran,
								1, tahunAkademik, keterangan.getValue().trim(), totalpengurangan,
								nilaiBiayaHarusDiBayars,
								DokuCommon.populateDokuRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
								DokuCommon.populateDetailBiaya(gridss, pengurangan), event);
					}
				});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_ipaymu", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarViaIpaymu = new MyButtonConfig("BAYAR VIA IPaymu", "/img/logo_ipaymu.png");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";ipaymu;")))
					spaceBayar.appendChild(bayarViaIpaymu);
				bayarViaIpaymu.addEventListener("onClick", new BasePaymentFlow("IPaymu", bayarViaIpaymu, null, false) {
					@Override
					protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
							Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
						IpaymuCommon.onSaveIpaymu(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan,
								jadwalPembayaran, 1, tahunAkademik, keterangan.getValue().trim(), totalpengurangan,
								nilaiBiayaHarusDiBayars,
								IpaymuCommon.populateIpaymuRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
								IpaymuCommon.populateDetailBiaya(gridss, pengurangan), event);
					}
				});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_faspay", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarViaFaspay = FaspayCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";faspay;")))
					spaceBayar.appendChild(bayarViaFaspay);
				bayarViaFaspay.addEventListener("onClick", new BasePaymentFlow(bayarViaFaspay.getLabel(),
						bayarViaFaspay, "faspay_biaya_administrasi", true) {
					@Override
					protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
							Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
						FaspayCommon.onSaveFaspay(
								Common.numberFormat.get().parse(Common.numberFormat.get().format(nilaiYgAkanDibayar))
										.doubleValue(),
								null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
								keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
								FaspayCommon.populateFaspayRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
								FaspayCommon.populateDetailBiaya(gridss, pengurangan), event);
					}
				});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_jatelindo", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_jatelindo_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarViaJatelindo = JatelindoCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";jatelindo;")))
					spaceBayar.appendChild(bayarViaJatelindo);
				bayarViaJatelindo.addEventListener("onClick", new BasePaymentFlow(bayarViaJatelindo.getLabel(),
						bayarViaJatelindo, "jatelindo_biaya_administrasi", true) {
					@Override
					protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
							Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
						JatelindoCommon.onSaveJatelindo(
								Common.numberFormat.get().parse(Common.numberFormat.get().format(nilaiYgAkanDibayar))
										.doubleValue(),
								null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
								keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
								JatelindoCommon.populateJatelindoRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
								JatelindoCommon.populateDetailBiaya(gridss, pengurangan), event);
					}
				});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_cimb", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarViaCimb = CimbCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";cimb;")))
					spaceBayar.appendChild(bayarViaCimb);
				bayarViaCimb.addEventListener("onClick",
						new BasePaymentFlow(bayarViaCimb.getLabel(), bayarViaCimb, null, false) {
							@Override
							protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
									Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
								CimbCommon.onSaveCimb(Common.numberFormat.get()
										.parse(Common.numberFormat.get().format(nilaiYgAkanDibayar)).doubleValue(),
										null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
										keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
										CimbCommon.populateCimbRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
										CimbCommon.populateDetailBiaya(gridss, pengurangan), event);
							}
						});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bni", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bni_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarViaBni = BniCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bni;")))
					spaceBayar.appendChild(bayarViaBni);
				bayarViaBni.addEventListener("onClick",
						new BasePaymentFlow(bayarViaBni.getLabel(), bayarViaBni, "bni_biaya_administrasi", true) {
							@Override
							protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
									Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
								BniCommon.onSaveBni(Common.numberFormat.get()
										.parse(Common.numberFormat.get().format(nilaiYgAkanDibayar)).doubleValue(),
										null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
										keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
										BniCommon.populateBniRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
										BniCommon.populateDetailBiaya(gridss, pengurangan), true, event,
										VirtualAccountBank.populateCicilan(gridCicilan));
							}
						});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bsi", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bsi_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarViaBsi = BsiCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bsi_lama;")))
					spaceBayar.appendChild(bayarViaBsi);
				bayarViaBsi.addEventListener("onClick",
						new BasePaymentFlow(bayarViaBsi.getLabel(), bayarViaBsi, "bsi_biaya_administrasi", true) {
							@Override
							protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
									Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
								BsiCommon.onSaveBsi(Common.numberFormat.get()
										.parse(Common.numberFormat.get().format(nilaiYgAkanDibayar)).doubleValue(),
										null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
										keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
										BsiCommon.populateBsiRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
										BsiCommon.populateDetailBiaya(gridss, pengurangan), true, event,
										VirtualAccountBank.populateCicilan(gridCicilan));
							}
						});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bri", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bri_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarViaBri = BriCommon.createButton();
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bri;")))
					spaceBayar.appendChild(bayarViaBri);
				bayarViaBri.addEventListener("onClick",
						new BasePaymentFlow(bayarViaBri.getLabel(), bayarViaBri, "bri_biaya_administrasi", true) {
							@Override
							protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
									Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
								BriCommon.onSaveBri(Common.numberFormat.get()
										.parse(Common.numberFormat.get().format(nilaiYgAkanDibayar)).doubleValue(),
										null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1, tahunAkademik,
										keterangan.getValue().trim(), totalpengurangan, nilaiBiayaHarusDiBayars,
										BriCommon.populateBriRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
										BriCommon.populateDetailBiaya(gridss, pengurangan), true, event);
							}
						});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_finpay", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarViaFinpay = new MyButtonConfig("BAYAR VIA FINPAY", "/img/spi-finpay.png");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";finpay;")))
					spaceBayar.appendChild(bayarViaFinpay);
				bayarViaFinpay.addEventListener("onClick", new BasePaymentFlow("FINPAY", bayarViaFinpay, null, false) {
					@Override
					protected void executePayment(Event event, double biayaAdministrasi, String tahunAkademik,
							Double totalpengurangan, Double nilaiYgAkanDibayar) throws Exception {
						FinpayCommon.onSaveFinpay(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan,
								jadwalPembayaran, 1, tahunAkademik, keterangan.getValue().trim(), totalpengurangan,
								nilaiBiayaHarusDiBayars,
								FinpayCommon.populateFinpayRequestDetail(gridCicilan, null, 1, jadwalPembayaran),
								FinpayCommon.populateDetailBiaya(gridss, pengurangan), event);
					}
				});
			}

			// ================== BANK GENERATORS TANPA POPUP ONSAVE, MELAINKAN DOWNLOAD
			// DATA/URL REDIRECT ================== //

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_btn", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_btn_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarBankBTN = new MyButtonConfig("BAYAR VIA BANK BTN");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";btn;")))
					spaceBayar.appendChild(bayarBankBTN);
				bayarBankBTN.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (!checkKondisiSebelumbayarBaru())
							return;
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!apakah0(true))
									return;
								if (!detailBiayas.isEmpty()) {
									Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
									VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankBtn
											.downloadData(calonMahasiswa, jadwalPembayaran, detailBiayas, gridCicilan,
													smt);
									File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
											+ virtualAccountBank.getId() + ".png");
									BarcodeCommon.generateCRCode(virtualAccountBank.getKode(), myfilebarcode1);
									Double biayaAdministrasi = 0.0;
									String myUrl = "/common/btn/no_va.zul?va="
											+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
											+ URLEncoder.encode("Rp. "
													+ Common.numberFormat.get().format(virtualAccountBank.getTotal()),
													"UTF-8")
											+ "&biayaAdministrasi="
											+ URLEncoder.encode(
													"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
													"UTF-8")
											+ "&nama=" + URLEncoder.encode(calonMahasiswa.getNama(), "UTF-8")
											+ "&kadalurasa="
											+ URLEncoder.encode(Common.dateFormat.get()
													.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
											+ "&biayaTotal="
											+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
													.format(virtualAccountBank.getTotal() + biayaAdministrasi), "UTF-8")
											+ "&qr="
											+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/"
													+ myfilebarcode1.getName(), "UTF-8")
											+ "&terbilang="
											+ URLEncoder.encode(
													IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
													"UTF-8")
											+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);
									Common.displayWindow(myUrl, true, "75%");
								} else {
									MyMessageboxConfig.show(
											"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}, "Proses pembayaran ..");
					}
				});
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bankaltimtara", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarBankBankaltimtara = new MyButtonConfig("BAYAR VIA BANK Bankaltimtara");
				bayarBankBankaltimtara.setWidth("130px");
				bayarBankBankaltimtara.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bankaltimtara;")))
					spaceBayar.appendChild(bayarBankBankaltimtara);
				bayarBankBankaltimtara.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (!checkKondisiSebelumbayarBaru())
							return;
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!apakah0(true))
									return;
								if (!detailBiayas.isEmpty()) {
									final MyWindow window = new MyWindow("Pilihlah Bayar Via", "none", false);
									window.setHeight("150px");
									window.setWidth("400px");
									Radiogroup radiogroup = new Radiogroup();
									radiogroup.setParent(window);
									Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
									borderlayout.setParent(radiogroup);
									Center center = new Center();
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);
									MyGrid grid = new MyGrid();
									grid.setWidth("100%");
									grid.setParent(center);
									grid.setHeight("100%");
									Rows rows = new Rows();
									rows.setParent(grid);

									for (final String kode : new String[] { "Virtual Account", "QRIS" }) {
										MyFormRow row = new MyFormRow();
										row.setValign("top");
										row.setParent(rows);
										MyRadioConfig radio = new MyRadioConfig(kode);
										radio.setParent(row);
										radio.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														Double biayaAdministrasi = 0.0;
														try {
															biayaAdministrasi = Double.parseDouble(Common
																	.getKonfigurasi("bankaltimtara_biaya_administrasi",
																			"0.0")
																	.getNilai());
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
														Integer smt = (Integer) semesterPilihan.getSelectedItem()
																.getValue();
														VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankBankaltimtara
																.downloadData(calonMahasiswa, jadwalPembayaran,
																		detailBiayas, gridCicilan, smt,
																		biayaAdministrasi,
																		kode.equalsIgnoreCase("Virtual Account"));
														File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT()
																+ "/crcode_" + virtualAccountBank.getId() + ".png");
														BarcodeCommon.generateCRCode(virtualAccountBank.getBarcode(),
																myfilebarcode1, 600, 600);
														String myUrl = "/common/bankaltimtara/no_va.zul?pakaiva="
																+ virtualAccountBank.getPakaiva() + "&va="
																+ URLEncoder
																		.encode(virtualAccountBank.getKode(), "UTF-8")
																+ "&nominal="
																+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
																		.format(virtualAccountBank.getTotal()), "UTF-8")
																+ "&biayaAdministrasi="
																+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
																		.format(biayaAdministrasi), "UTF-8")
																+ "&nama="
																+ URLEncoder.encode(calonMahasiswa.getNama(), "UTF-8")
																+ "&kadalurasa="
																+ URLEncoder.encode(Common.dateFormat.get()
																		.format(virtualAccountBank
																				.getKadaluarsaWaktu()),
																		"UTF-8")
																+ (virtualAccountBank.getKadaluarsaBarcode() == null
																		? ""
																		: "&kadalurasa_barcode=" + URLEncoder.encode(
																				Common.dateFormat5.get()
																						.format(virtualAccountBank
																								.getKadaluarsaBarcode()),
																				"UTF-8"))
																+ "&biayaTotal="
																+ URLEncoder
																		.encode("Rp. " + Common.numberFormat.get()
																				.format(virtualAccountBank.getTotal()
																						+ biayaAdministrasi),
																				"UTF-8")
																+ "&qr="
																+ URLEncoder.encode(Common.getRequestHostWithProtocol()
																		+ "/report/" + myfilebarcode1.getName(),
																		"UTF-8")
																+ (virtualAccountBank.getHtmlTemporaryData() == null
																		|| virtualAccountBank.getHtmlTemporaryData()
																				.isEmpty()
																						? ""
																						: "&html=" + URLEncoder.encode(
																								virtualAccountBank
																										.getHtmlTemporaryData(),
																								"UTF-8"))
																+ "&terbilang="
																+ URLEncoder.encode(IndonesianNumberToWords
																		.convert((long) (virtualAccountBank.getTotal()
																				+ biayaAdministrasi)),
																		"UTF-8")
																+ "&tampilBiayaAdministrasi="
																+ (biayaAdministrasi > 0.1);
														Common.displayWindow(myUrl, true, "75%");
													}
												});
												window.detach();
											}
										});
									}
									ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
									window.onModal();
								} else {
									MyMessageboxConfig.show(
											"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}, "Proses pembayaran ..");
					}
				});
			}

			// =========================================================================================
			// KEMBALIKAN LOGIKA BANK BRIVA
			// =========================================================================================
			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_briva", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarBankBriva = new MyButtonConfig("BAYAR VIA BRIVA");
				bayarBankBriva.setWidth("130px");
				bayarBankBriva.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";briva;"))) {
					spaceBayar.appendChild(bayarBankBriva);
				}

				bayarBankBriva.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (!checkKondisiSebelumbayarBaru())
							return;
						Common.createDefaultTimer(new EventListener() {
							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!apakah0(true))
									return;
								if (!detailBiayas.isEmpty()) {
									Double biayaAdministrasi = 0.0;
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("briva_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

									Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
									BankHost bankHost = pembayaranUtil.getBankHost(
											Common.getKonfigurasi("briva_bank_host_ip", "").getNilai(), "Bank Host");
									Map param = new HashMap();
									param.put("briva", true);

									VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankOnline
											.downloadData(calonMahasiswa, jadwalPembayaran, detailBiayas, gridCicilan,
													smt, param, biayaAdministrasi, bankHost);
									if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif"))
										return;

									if (virtualAccountBank != null) {
										File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
												+ virtualAccountBank.getId() + ".png");
										BarcodeCommon.generateCRCode(virtualAccountBank.getKode(), myfilebarcode1);

										String nama = calonMahasiswa.getNama();
										String myUrl = "/common/bri/no_va.zul?va="
												+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
												+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
														.format(virtualAccountBank.getTotal()), "UTF-8")
												+ "&biayaAdministrasi="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
														"UTF-8")
												+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
												+ URLEncoder.encode(Common.dateFormat.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
												+ "&biayaTotal="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(
																virtualAccountBank.getTotal() + biayaAdministrasi),
														"UTF-8")
												+ "&qr="
												+ URLEncoder.encode(Common.getRequestHostWithProtocol()
														+ "/report/" + myfilebarcode1.getName(), "UTF-8")
												+ "&terbilang="
												+ URLEncoder.encode(IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
														"UTF-8")
												+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);
										Common.displayWindow(myUrl, true, "75%");
									} else {
										MyMessageboxConfig.show(
												"Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Bapak/Ibu; (2) ulangi proses transaksi beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
												"Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								} else {
									MyMessageboxConfig.show(
											"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}, "Proses pembayaran ..");
					}
				});
			}

			// =========================================================================================
			// KEMBALIKAN LOGIKA BANK NTT (Menggunakan DownloadNoUjianCalonMahasiswaBankNtt)
			// =========================================================================================
			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarBankNTT = new MyButtonConfig("BAYAR VIA BANK NTT");
				bayarBankNTT.setWidth("130px");
				bayarBankNTT.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";ntt;"))) {
					spaceBayar.appendChild(bayarBankNTT);
				}

				bayarBankNTT.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (!checkKondisiSebelumbayarBaru())
							return;
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!apakah0(true))
									return;
								if (!detailBiayas.isEmpty()) {
									VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankNtt
											.downloadData(calonMahasiswa, jadwalPembayaran, detailBiayas, gridCicilan);
									String code = virtualAccountBank.getKode();
									File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
											+ virtualAccountBank.getId() + ".png");
									BarcodeCommon.generateCRCode(code, myfilebarcode1);

									Double biayaAdministrasi = 0.0;
									String nama = calonMahasiswa.getNama();
									String myUrl = "/common/ntt/no_va.zul?va="
											+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
											+ URLEncoder.encode("Rp. "
													+ Common.numberFormat.get().format(virtualAccountBank.getTotal()),
													"UTF-8")
											+ "&biayaAdministrasi="
											+ URLEncoder.encode(
													"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
													"UTF-8")
											+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
											+ URLEncoder.encode(Common.dateFormat.get()
													.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
											+ "&biayaTotal="
											+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
													.format(virtualAccountBank.getTotal() + biayaAdministrasi), "UTF-8")
											+ "&qr="
											+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/"
													+ myfilebarcode1.getName(), "UTF-8")
											+ "&terbilang="
											+ URLEncoder.encode(
													IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
													"UTF-8")
											+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);
									Common.displayWindow(myUrl, true, "75%");
								} else {
									MyMessageboxConfig.show(
											"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}, "Proses pembayaran ..");
					}
				});
			}

			// =========================================================================================
			// KEMBALIKAN LOGIKA BANK BJB (Menggunakan DownloadNoUjianCalonMahasiswaBankBjb)
			// =========================================================================================
			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bjb", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig bayarBankBJB = new MyButtonConfig("BAYAR VIA BANK BJB");
				bayarBankBJB.setWidth("130px");
				bayarBankBJB.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bjb;"))) {
					spaceBayar.appendChild(bayarBankBJB);
				}

				bayarBankBJB.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (!checkKondisiSebelumbayarBaru())
							return;
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!apakah0(true))
									return;
								if (!detailBiayas.isEmpty()) {
									VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankBjb
											.downloadData(calonMahasiswa, jadwalPembayaran, detailBiayas, gridCicilan);
									String code = virtualAccountBank.getKode();
									File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
											+ virtualAccountBank.getId() + ".png");
									BarcodeCommon.generateCRCode(code, myfilebarcode1);

									Double biayaAdministrasi = 0.0;
									// Fallback agar mencegah NullPointerException jika biodataCalonMahasiswaAktif
									// bernilai null saat inisialisasi awal
									String nama = biodataCalonMahasiswaAktif != null
											? biodataCalonMahasiswaAktif.getNama()
											: calonMahasiswa.getNama();

									String myUrl = "/common/bjb/no_va.zul?va="
											+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
											+ URLEncoder.encode("Rp. "
													+ Common.numberFormat.get().format(virtualAccountBank.getTotal()),
													"UTF-8")
											+ "&biayaAdministrasi="
											+ URLEncoder.encode(
													"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
													"UTF-8")
											+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
											+ URLEncoder.encode(Common.dateFormat.get()
													.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
											+ "&biayaTotal="
											+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
													.format(virtualAccountBank.getTotal() + biayaAdministrasi), "UTF-8")
											+ "&qr="
											+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/"
													+ myfilebarcode1.getName(), "UTF-8")
											+ "&terbilang="
											+ URLEncoder.encode(
													IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
													"UTF-8")
											+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);
									Common.displayWindow(myUrl, true, "75%");
								} else {
									MyMessageboxConfig.show(
											"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}, "Proses pembayaran ..");
					}
				});
			}

			// (Kumpulan tombol Online, NTT, BJB, flip, otto, briva, qris di refactor
			// menggunakan pola yang sama persis seperti sebelumnya)
			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR ONLINE");
				bayarBankOnline.setWidth("130px");
				bayarBankOnline.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";online;")))
					spaceBayar.appendChild(bayarBankOnline);
				bayarBankOnline.addEventListener("onClick",
						createOnlineBankListener("online_biaya_administrasi", "online_bank_host_ip",
								"prefix_kode_bank_lain_online", false, false, false, false, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig bayarBankOnline2 = new MyButtonConfig("BAYAR ONLINE 2");
				bayarBankOnline2.setWidth("130px");
				bayarBankOnline2.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";online_2;")))
					spaceBayar.appendChild(bayarBankOnline2);
				bayarBankOnline2.addEventListener("onClick",
						createOnlineBankListener("online_biaya_administrasi_2", "online_2_bank_host_ip",
								"prefix_kode_bank_lain_online_2", false, false, false, false, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_smartlink", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_smartlink_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig btnSmartlink = new MyButtonConfig("BAYAR VIA ONLINE");
				btnSmartlink.setWidth("130px");
				btnSmartlink.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";online;")))
					spaceBayar.appendChild(btnSmartlink);
				btnSmartlink.addEventListener("onClick",
						createOnlineBankListener("online_smartlink_biaya_administrasi", "online_bank_host_ip",
								"prefix_kode_bank_lain_online", true, false, false, false, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_maja", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_maja_pt_" + perguruanTinggi.getId())) {
				final MyButtonConfig btnMaja = new MyButtonConfig("BAYAR VIA BSI");
				btnMaja.setWidth("130px");
				btnMaja.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bsi;")))
					spaceBayar.appendChild(btnMaja);
				btnMaja.addEventListener("onClick", createOnlineBankListener("maja_biaya_administrasi",
						"maja_bank_host_ip", null, false, true, false, false, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_qris", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig btnQris = new MyButtonConfig("BAYAR QRIS");
				btnQris.setWidth("130px");
				btnQris.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";qris;")))
					spaceBayar.appendChild(btnQris);
				btnQris.addEventListener("onClick", createOnlineBankListener("qris_biaya_administrasi",
						"qris_bank_host_ip", null, false, false, true, false, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_finpay", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig btnFinpay = new MyButtonConfig("BAYAR FINPAY");
				btnFinpay.setWidth("130px");
				btnFinpay.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";finpay;")))
					spaceBayar.appendChild(btnFinpay);
				btnFinpay.addEventListener("onClick", createOnlineBankListener("finpay_biaya_administrasi",
						"finpay_bank_host_ip", null, false, false, false, true, false, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_flip", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig btnFlip = new MyButtonConfig("BAYAR VIA FLIP");
				btnFlip.setWidth("130px");
				btnFlip.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";flip;")))
					spaceBayar.appendChild(btnFlip);
				btnFlip.addEventListener("onClick", createOnlineBankListener("flip_biaya_administrasi",
						"flip_bank_host_ip", null, false, false, false, false, true, false));
			}

			if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_otto", Konfigurasi.TIDAK_AKTIF)) {
				final MyButtonConfig btnOtto = new MyButtonConfig("BAYAR OTTO");
				btnOtto.setWidth("130px");
				btnOtto.setHeight("55px");
				if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
						|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";otto;")))
					spaceBayar.appendChild(btnOtto);
				btnOtto.addEventListener("onClick", createOnlineBankListener("otto_biaya_administrasi",
						"otto_bank_host_ip", null, false, false, false, false, false, true));
			}

		}

		final MyButtonConfig save = new MyButtonConfig("Bayar", "/img/Money-icon_kecil.png");
		if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
				|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";tunai;"))) {
			spaceBayar.appendChild(save);
		}
		save.setDisabled(!simpan);
		save.setHeight("55px");
		save.setWidth("130px");
		String admLain = Common.getKonfigurasi("admin_lain_yang_tidak_bisa_membayar_langsung", "").getNilai();
		String[] aa = admLain.split(";");
		boolean admin_lain_yang_tidak_bisa_membayar_langsung = false;
		for (String a : aa) {
			try {
				admin_lain_yang_tidak_bisa_membayar_langsung = a.trim().equalsIgnoreCase(tbmuser.getUserId());
				if (admin_lain_yang_tidak_bisa_membayar_langsung)
					break;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		save.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_manual") && !admin_lain_yang_tidak_bisa_membayar_langsung);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!checkKondisiSebelumbayarBaru())
					return;
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!apakah0(false))
							return;
						double biayaAdministrasi = 0.0;
						try {
							String _cfgVal = Common.getKonfigurasi("manual_biaya_administrasi", "0.0").getNilai();
							if (_cfgVal != null && !_cfgVal.trim().isEmpty()) {
								biayaAdministrasi = Double.parseDouble(_cfgVal.trim());
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaBaruAction.java:4133");
							// config kosong atau non-numerik — gunakan default 0
						}

						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu memeriksa kembali rincian pembayaran berikut sebelum melanjutkan:\nNama Mahasiswa : "
										+ calonMahasiswa.getNama() + "\nJumlah total tagihan : "
										+ labelFooterTagihan.getValue() + " \nJumlah yang akan dibayar : "
										+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
										+ (biayaAdministrasi > 0.1
												? "\nBiaya administrasi : "
														+ Common.numberFormat.get().format(biayaAdministrasi)
														+ "\nTotal yang akan dibayar : "
														+ Common.numberFormat.get()
																.format(jumlahYangAkanDibayar + biayaAdministrasi)
												: " ")
										+ "\nTerbilang : "
										+ IndonesianNumberToWords
												.convert((long) (jumlahYangAkanDibayar + biayaAdministrasi)),
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(final Event event) throws Exception {
										if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
											if (onSave(kegiatan, calonMahasiswa, event, null)) {
												Common.freeze(center, true);
												Common.freeze(panelMencicil, true);
											}
										}
									}
								});
					}
				});
			}
		});

		Mahasiswa mahasiswa = calonMahasiswa.getMahasiswa();
		MyButtonConfig saveTabungan = new MyButtonConfig("Dari Tabungan", "/img/payments-icon.png");
		if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
				|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";tabungan;")))
			spaceBayar.appendChild(saveTabungan);
		saveTabungan.setHeight("55px");
		saveTabungan.setVisible(tabungan > 0.1 && mahasiswa != null);
		saveTabungan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Auto-isi DARI TABUNGAN: terangsur sesuai saldo. Bila belum diisi (0) ATAU
				// melebihi saldo → isi otomatis dibatasi saldo (saldo>=total → penuh).
				double saldoTbg = tabungan == null ? 0.0 : tabungan;
				double totalDiisi = hitungJumlahYangAkanDibayarDariTampilan();
				if (saldoTbg > 0.1 && (Math.abs(totalDiisi) < 0.01 || totalDiisi > saldoTbg + 0.01)) {
					try { capSaldoIsiCicilan = saldoTbg; inputSesuaiTagihan(); } finally { capSaldoIsiCicilan = 0.0; }
				}
				if (!checkKondisiSebelumbayarBaru())
					return;
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!apakah0(true))
							return;
						double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("manual_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu memeriksa kembali rincian pembayaran dari tabungan berikut sebelum melanjutkan:\nNama Mahasiswa : "
										+ calonMahasiswa.getMahasiswa().getNama() + "\nJumlah total tagihan : "
										+ labelFooterTagihan.getValue() + "\nJumlah yang akan dibayar : "
										+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
										+ (biayaAdministrasi > 0.1
												? "\nBiaya administrasi : "
														+ Common.numberFormat.get().format(biayaAdministrasi)
														+ "\nTotal yang akan dibayar : "
														+ Common.numberFormat.get()
																.format(jumlahYangAkanDibayar + biayaAdministrasi)
												: " ")
										+ "\nTerbilang : "
										+ IndonesianNumberToWords
												.convert((long) (jumlahYangAkanDibayar + biayaAdministrasi))
										+ "\nNominal Tabungan : " + Common.numberFormat.get().format(tabungan)
										+ "\nTerbilang : " + IndonesianNumberToWords.convert(tabungan.longValue()),
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(final Event event) throws Exception {
										if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
											final List<JenisPembayaran> jp = ConstantValues.simpleList(
													HibernateUtil.currentSession().createCriteria(JenisPembayaran.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.isNotNull("jenisTabungan")),
													JenisPembayaran.class);
											if (jp.size() == 1) {
												if (onSave(kegiatan, calonMahasiswa, event, jp.get(0))) {
													Common.freeze(DaftarUlangMahasiswaBaruAction.this.center, true);
													Common.freeze(panelMencicil, true);
												}
											} else if (!jp.isEmpty()) {
												final MyWindow window = new MyWindow("Pilihlah Jenis Tabungan", "none",
														false);
												window.setHeight("200px");
												window.setWidth("500px");
												Radiogroup radiogroup = new Radiogroup();
												radiogroup.setParent(window);
												Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
												borderlayout.setParent(radiogroup);
												Center center = new Center();
												center.setParent(borderlayout);
												ais.ui.util.ZkCompat.setFlex(center, true);
												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(center);
												grid.setHeight("100%");
												Rows rows = new Rows();
												rows.setParent(grid);

												for (final JenisPembayaran j : jp) {
													MyFormRow row = new MyFormRow();
													row.setValign("top");
													row.setParent(rows);
													MyRadioConfig radio = new MyRadioConfig(
															j.getNama() + " (" + (j.getJenisTabungan() + ")"));
													radio.setParent(row);
													radio.addEventListener("onClick", new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															if (onSave(kegiatan, calonMahasiswa, event, j)) {
																Common.freeze(
																		DaftarUlangMahasiswaBaruAction.this.center,
																		true);
																Common.freeze(panelMencicil, true);
															}
															window.detach();
														}
													});
												}
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
														.appendChild(window);
												window.onModal();
											} else {
												MyMessageboxConfig.show(
														"Mohon maaf, jenis pembayaran melalui tabungan tidak ditemukan pada pengaturan sistem. Langkah yang dapat dilakukan: (1) pastikan jenis pembayaran tabungan telah dikonfigurasikan; (2) gunakan metode pembayaran lain yang tersedia; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
											}
										}
									}
								});
					}
				});
			}
		});

		if (jadwalPembayaran != null && (jadwalPembayaran.getStartDate().after(WaktuUtil.getDate())
				|| jadwalPembayaran.getEndDate().before(WaktuUtil.getDate()))) {
			if (!(jadwalPembayaran.getAdminBolehMembayarkanDiluarjadwal() && tbmuser != null
					&& tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null)) {
				spaceBayar.setVisible(false);
				spaceBayar.setHeight("0px");
				try {
					MyMessageboxConfig.show(
							"Mohon maaf, tidak ada jadwal pembayaran yang berlaku, atau pembayaran telah terlambat, atau periode pembayaran belum dimulai. Langkah yang dapat dilakukan: (1) periksa kembali periode jadwal pembayaran yang berlaku; (2) pastikan tanggal saat ini berada dalam rentang jadwal pembayaran; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan atau Administrator sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		} else if (biodataCalonMahasiswaAktif != null) {
			save.setVisible(false);
			if (!TampilanPaymentGateway.adaPaymentGatewayYangAktif()) {
				spaceBayar.setVisible(false);
				spaceBayar.setHeight("0px");
			}
		}
	}

	private void tampilkanJendelaVA(VirtualAccountBank va, Double biayaAdministrasi, String fileZulPrefix,
			String prefixBankLainKode) throws Exception {
		MahasiswaVirtualAccountHelper.tampilkanHasilVirtualAccount(va, null, calonMahasiswa, biayaAdministrasi,
				fileZulPrefix, prefixBankLainKode);
	}

	// Helper method untuk mendelegasikan Online bank URLs dengan variasi parameter
	private EventListener createOnlineBankListener(final String adminConfig, final String hostConfig,
			final String prefixConfig, final boolean isSmartlink, final boolean isMaja, final boolean isQris,
			final boolean isFinpay, final boolean isFlip, final boolean isOtto) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!checkKondisiSebelumbayarBaru())
					return;
				Common.createDefaultTimer(new EventListener() {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!apakah0(true))
							return;
						if (!detailBiayas.isEmpty()) {
							Double biayaAdministrasi = 0.0;
							try {
								biayaAdministrasi = Double
										.parseDouble(Common.getKonfigurasi(adminConfig, "0.0").getNilai());
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							Integer smt = (Integer) semesterPilihan.getSelectedItem().getValue();
							BankHost bankHost = pembayaranUtil
									.getBankHost(Common.getKonfigurasi(hostConfig, "").getNilai(), "Bank Host");
							Map param = new HashMap();
							if (isSmartlink)
								param.put("smartlink", true);
							if (isMaja)
								param.put("maja", true);
							if (isQris)
								param.put("qris", true);
							if (isFinpay)
								param.put("finpay", true);
							if (isFlip)
								param.put("flip", true);
							if (isOtto)
								param.put("otto", true);

							VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankOnline
									.downloadData(calonMahasiswa, jadwalPembayaran, detailBiayas, gridCicilan, smt,
											param, biayaAdministrasi, bankHost);
							if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif"))
								return;

							if (isFlip) {
								if (virtualAccountBank != null)
									Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
											+ "', title: 'Book', w: 1200, h: 600});");
								else
									MyMessageboxConfig.show(
											"Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Bapak/Ibu; (2) ulangi proses transaksi beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
							if (isFinpay || isOtto) {
								if (virtualAccountBank != null)
									ExecutionsCtrl.getCurrent().sendRedirect(virtualAccountBank.getLink(), "_blank");
								else
									MyMessageboxConfig.show(
											"Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Bapak/Ibu; (2) ulangi proses transaksi beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
											"Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}

							if (virtualAccountBank != null && virtualAccountBank.getLink() != null
									&& !virtualAccountBank.getLink().isEmpty()) {
								Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
										+ "', title: 'Book', w: 1200, h: 600});");
								return;
							}

							if (virtualAccountBank != null && virtualAccountBank.getId() != null) {
								String kodebankLainOnline = prefixConfig != null
										? Common.getKonfigurasi(prefixConfig, "").getNilai()
										: "";
								tampilkanJendelaVA(virtualAccountBank, biayaAdministrasi,
										"/common/" + (isQris ? "qris" : "online") + "/no_va.zul",
										kodebankLainOnline);
							}
						} else {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.",
									"Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}, "Proses pembayaran ..");
			}
		};
	}


}
