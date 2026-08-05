package ais.action.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.koperasi.helper.AmbilDataAnggotaKoperasiBanbox;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.format1.keuangan.LaporanDeposit;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Deposit;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisTabungan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DepositAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private static final int TREND_MUTASI_PAGE_SIZE = 10;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnamaPenabung;
	private Textbox searchnama;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataCalonMahasiswaBanbox calonMahasiswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Deposit deposit;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser;
	private MyDoublebox nominal;
	private MyDatebox waktu;

	private Tabpanel tabCaraBayarTabungan;

	private void loadIncludeOnce(Tabpanel panel, String url) {
		if (panel == null || url == null || url.trim().length() == 0) {
			return;
		}
		if (panel.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(panel);
			MyInclude iframe = new MyInclude(url);
			iframe.setHeight("100%");
			iframe.setWidth("100%");
			iframe.setParent(window);
		}
	}

	public void onCaraBayarTabungan(Event event) {
		loadIncludeOnce(tabCaraBayarTabungan, "/pages/master/jenis_pembayaran.zul");
	}

	private Tabpanel tabLaporanDeposit;
	private Combobox jenisPembayaran;

	public void onLaporanDeposit(Event event) {
		if (tabLaporanDeposit.getChildren().size() == 0) {
			LaporanDeposit laporan = new LaporanDeposit();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(tabLaporanDeposit);
		}
	}

	private Tabpanel tabJenisDeposit;

	public void onJenis(Event event) {
		loadIncludeOnce(tabJenisDeposit, "/pages/master/jenis_tabungan.zul");
	}

	private Tabpanel tabPenggunaanDeposit;
	private Combobox jenisTabungan;
	private boolean pt;
	private boolean ya;
	private AmbilDataSiswaBanbox siswa;
	private AmbilDataCalonSiswaBanbox calonSiswa;

	public void onPenggunaan(Event event) {
		if (isLoginPenabungTerbatas()) {
			return;
		}
		if (tbmuser != null && tbmuser.getMahasiswa() != null && tabPenggunaanDeposit != null) {
			Common.clear(tabPenggunaanDeposit);
		}
		loadIncludeOnce(tabPenggunaanDeposit, "/pages/master/cicilan_pembayaran.zul?deposit=true");
	}

	private Row hbFakultasLabel;
	private Row hbYayasan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataAnggotaKoperasiBanbox anggotaKoperasi;

	protected Tabpanel tabMutasiPanel;
	private Mahasiswa mhs;
	private Siswa sis;
	private Map<Long, Double> saldoDepositCache = new HashMap<Long, Double>();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private boolean isLoginPenabungTerbatas() {
		return (mhs != null && mhs.getId() != null) || (sis != null && sis.getId() != null);
	}

	private Long getLoginMahasiswaId() {
		return mhs == null ? null : mhs.getId();
	}

	private Long getLoginSiswaId() {
		return sis == null ? null : sis.getId();
	}

	private String getNamaLoginPenabung() {
		if (mhs != null) {
			return mhs.getNama() == null ? "" : mhs.getNama();
		}
		if (sis != null) {
			return sis.getNama() == null ? "" : sis.getNama();
		}
		return "";
	}

	private void setVisibleTabpanel(Tabpanel panel, boolean visible) {
		if (panel == null) {
			return;
		}
		try {
			panel.setVisible(visible);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (panel.getLinkedTab() != null) {
				panel.getLinkedTab().setVisible(visible);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void applyModePenabungTerbatas() {
		if (!isLoginPenabungTerbatas()) {
			return;
		}

		edit = false;
		delete = false;
		if (add != null) {
			add.setVisible(false);
		}

		setVisibleTabpanel(tabJenisDeposit, false);
		setVisibleTabpanel(tabCaraBayarTabungan, false);
		setVisibleTabpanel(tabLaporanDeposit, false);
		setVisibleTabpanel(tabPenggunaanDeposit, false);

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(false);
		}
		if (hbYayasan != null) {
			hbYayasan.setVisible(false);
		}
		if (searchnamaPenabung != null) {
			searchnamaPenabung.setValue(getNamaLoginPenabung());
			searchnamaPenabung.setDisabled(true);
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		mhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		sis = tbmuser == null ? null : tbmuser.getSiswa();

		
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			pt = false;
			ya = true;
		}

		if (searchfakultas != null && searchjurusan != null && hbFakultasLabel != null && hbYayasan != null
				&& searchyayasan != null && searchsekolah != null) {
			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
			Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, false, false);

			hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);
			hbYayasan.setVisible(ya);
		}

		if (!pt && tabPenggunaanDeposit != null) {
			setVisibleTabpanel(tabPenggunaanDeposit, false);
		}
		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
		if (tbmrole != null && !tbmrole.getBolehEntryTopup()) {
			if (add != null) {
				add.setVisible(false);
			}
			edit = false;
			delete = false;
		} else {
			if (add != null) {
				add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && tbmuser != null
						&& !isLoginPenabungTerbatas());
			}
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		if (add != null) {
			add.setTooltiptext("Tambah");
		}

		if (tabJenisDeposit != null && add != null) {
			tabJenisDeposit.setVisible((add != null && add.isVisible()));
			if (tabJenisDeposit.getLinkedTab() != null) {
				tabJenisDeposit.getLinkedTab().setVisible((add != null && add.isVisible()));
			}
		}
		applyModePenabungTerbatas();

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initMutasiTabungan("");
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "mahasiswa", "biodataCalonMahasiswa", "siswa", "calonSiswa",
				"anggotaKoperasi", "nominal", "waktu", "jenisPembayaran", "jenisTabungan", "keterangan" };
		if (add != null && add.getParent() != null) {
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Deposit.class, this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, Deposit.class, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
			Common.appendKeToolbar(upload, add, comp);
		}

	}

	// -----------------------------------------------------------------
		// PEMBARUAN: NATIVE SQL QUERY MUTASI, FOOTER GRID, FILTER, EXCEL & SALDO PER
		// PENABUNG
		// -----------------------------------------------------------------
		

	// -----------------------------------------------------------------
	// Dasbor, mutasi, dan analisis tabungan berbasis HTML/CSS.
	// Semua grafik dibuat langsung dari HTML/CSS agar ringan, responsif, dan aman untuk ZKoss 5.5.
	// -----------------------------------------------------------------

	private void initMutasiTabungan(final String searchName) {
		initMutasiTabungan(searchName, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	private void initMutasiTabungan(final String searchName, final Fakultas filterFakultas, final Jurusan filterJurusan,
			final Yayasan filterYayasan, final Sekolah filterSekolah) {
		if (tabMutasiPanel == null) {
			return;
		}
		Common.clear(tabMutasiPanel);

		Toolbar mutasiToolbar = new Toolbar();
		mutasiToolbar.setParent(tabMutasiPanel);
		mutasiToolbar.setStyle("padding:8px;background:#f8fafc;border-bottom:1px solid #e2e8f0;");
		if (isLoginPenabungTerbatas()) {
			mutasiToolbar.setVisible(false);
		}

		mutasiToolbar.appendChild(new Label("Cari Nama/Nomor: "));
		final Textbox txtSearchMutasi = new Textbox(searchName != null ? searchName : "");
		txtSearchMutasi.setWidth("170px");
		txtSearchMutasi.setParent(mutasiToolbar);

		final Combobox cmbFilterFakultas = new Combobox();
		final Combobox cmbFilterJurusan = new Combobox();
		final Combobox cmbFilterYayasan = new Combobox();
		final Combobox cmbFilterSekolah = new Combobox();

		Common.initFakultasDanJurusanDanSemua(null, null, cmbFilterFakultas, cmbFilterJurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, cmbFilterYayasan, cmbFilterSekolah, false, false);

		if (filterFakultas != null) {
			Common.selectComboItem(cmbFilterFakultas, filterFakultas);
		}
		if (filterJurusan != null) {
			Common.selectComboItem(cmbFilterJurusan, filterJurusan);
		}
		if (filterYayasan != null) {
			Common.selectComboItem(cmbFilterYayasan, filterYayasan);
		}
		if (filterSekolah != null) {
			Common.selectComboItem(cmbFilterSekolah, filterSekolah);
		}

		cmbFilterFakultas.setVisible(pt && cmbFilterFakultas.getChildren().size() > 1);
		cmbFilterJurusan.setVisible(pt);
		cmbFilterYayasan.setVisible(ya);
		cmbFilterSekolah.setVisible(ya);

		appendFilter(mutasiToolbar, " Fak: ", cmbFilterFakultas);
		appendFilter(mutasiToolbar, " Prodi: ", cmbFilterJurusan);
		appendFilter(mutasiToolbar, " Yayasan: ", cmbFilterYayasan);
		appendFilter(mutasiToolbar, " Sekolah: ", cmbFilterSekolah);

		MyToolbarbuttonConfig btnCariMutasi = new MyToolbarbuttonConfig("Cari", "/img/search.gif");
		btnCariMutasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Fakultas fak = cmbFilterFakultas.getSelectedItem() != null
						? (Fakultas) cmbFilterFakultas.getSelectedItem().getValue()
						: null;
				Jurusan jur = cmbFilterJurusan.getSelectedItem() != null
						? (Jurusan) cmbFilterJurusan.getSelectedItem().getValue()
						: null;
				Yayasan yay = cmbFilterYayasan.getSelectedItem() != null
						? (Yayasan) cmbFilterYayasan.getSelectedItem().getValue()
						: null;
				Sekolah sek = cmbFilterSekolah.getSelectedItem() != null
						? (Sekolah) cmbFilterSekolah.getSelectedItem().getValue()
						: null;
				initMutasiTabungan(txtSearchMutasi.getValue(), fak, jur, yay, sek);
			}
		});
		btnCariMutasi.setParent(mutasiToolbar);

		MyToolbarbuttonConfig btnResetMutasi = new MyToolbarbuttonConfig("Reset", "/img/svg/refresh.svg");
		btnResetMutasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initMutasiTabungan("", null, null, null, null);
			}
		});
		btnResetMutasi.setParent(mutasiToolbar);

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setStyle("background:#f8fafc;");
		portalLayout.setParent(tabMutasiPanel);

		/*
		 * Panel buku besar dan summary sengaja dibuat sebagai portal pertama agar
		 * selalu tampil paling atas. Setelah itu baru panel analisis, grafik, dan
		 * rekomendasi operasional.
		 */
		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px;");
		pcTop.setParent(portalLayout);

		String pcWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px;");
		pcBottom.setParent(portalLayout);

		Session session = null;
		final List<Map<String, Object>> mutasiList = new ArrayList<Map<String, Object>>();
		Map<String, Map<String, Object>> summaryData = new LinkedHashMap<String, Map<String, Object>>();
		Map<String, Double> trendMasuk = new LinkedHashMap<String, Double>();
		Map<String, Double> trendKeluar = new LinkedHashMap<String, Double>();
		Map<String, Double> pemasukanPerJenis = new LinkedHashMap<String, Double>();
		Map<String, Double> pengeluaranPerJenis = new LinkedHashMap<String, Double>();
		Map<String, Integer> jumlahPerJenis = new LinkedHashMap<String, Integer>();
		double totalTabungan = 0.0;
		double totalRealisasi = 0.0;
		double sisaTabungan = 0.0;
		Date transaksiTerakhir = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<Long> longsJurusans = ambilJurusanIds(session, filterFakultas);

			SQLQuery query = buildMutasiQuery(session, searchName, filterFakultas, filterJurusan, filterYayasan,
					filterSekolah, longsJurusans);
			List<Object[]> rowsData = query.list();

			for (Object[] row : rowsData) {
				String personKey = row[0] != null ? String.valueOf(row[0]) : "OTH_";
				String nomor = row[1] != null ? String.valueOf(row[1]) : "";
				String nama = row[2] != null ? String.valueOf(row[2]) : "Anonim";
				Date tanggal = row[3] instanceof Date ? (Date) row[3] : null;
				Double nominal = row[4] != null ? Double.valueOf(((Number) row[4]).doubleValue()) : Double.valueOf(0.0);
				String keteranganData = row[5] != null ? String.valueOf(row[5]) : "Mutasi Tabungan";
				String jenisMutasi = row.length > 6 && row[6] != null ? String.valueOf(row[6]) : "LAINNYA";
				String fullNama = (nomor.length() > 0 ? nomor + " - " : "") + nama;

				addIntegerValue(jumlahPerJenis, jenisMutasi, 1);
				if (nominal.doubleValue() >= 0.0) {
					totalTabungan += nominal.doubleValue();
					addMapValue(trendMasuk, monthKey(tanggal), nominal.doubleValue());
					addMapValue(pemasukanPerJenis, jenisMutasi, nominal.doubleValue());
				} else {
					totalRealisasi += Math.abs(nominal.doubleValue());
					addMapValue(trendKeluar, monthKey(tanggal), Math.abs(nominal.doubleValue()));
					addMapValue(pengeluaranPerJenis, jenisMutasi, Math.abs(nominal.doubleValue()));
				}
				if (tanggal != null && (transaksiTerakhir == null || tanggal.after(transaksiTerakhir))) {
					transaksiTerakhir = tanggal;
				}

				Map<String, Object> mapMutasi = new HashMap<String, Object>();
				mapMutasi.put("personKey", personKey);
				mapMutasi.put("nama", fullNama);
				mapMutasi.put("tanggal", tanggal);
				mapMutasi.put("keterangan", keteranganData);
				mapMutasi.put("jenisMutasi", jenisMutasi);
				mapMutasi.put("debit", nominal.doubleValue() > 0.0 ? nominal : Double.valueOf(0.0));
				mapMutasi.put("kredit", nominal.doubleValue() < 0.0 ? Double.valueOf(Math.abs(nominal.doubleValue()))
						: Double.valueOf(0.0));
				mutasiList.add(mapMutasi);
				akumulasiSummary(summaryData, personKey, fullNama, nominal.doubleValue());
			}
			sisaTabungan = totalTabungan - totalRealisasi;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		mutasiToolbar.appendChild(createExcelButton(mutasiList));

		renderGridMutasi(pcTop, mutasiList, totalTabungan, totalRealisasi, sisaTabungan);
		renderSummaryPenabung(pcTop, summaryData);
		renderDashboardDeposit(pcLeft, totalTabungan, totalRealisasi, sisaTabungan, mutasiList.size(), summaryData.size(),
				transaksiTerakhir);
		renderGrafikDeposit(pcRight, trendMasuk, trendKeluar, summaryData, totalTabungan, totalRealisasi);
		renderDashboardJenisMutasi(pcLeft, pemasukanPerJenis, pengeluaranPerJenis, jumlahPerJenis);
		renderDashboardKesehatanSaldo(pcRight, totalTabungan, totalRealisasi, sisaTabungan, summaryData, mutasiList.size());
		renderWatchlistSaldoPenabung(pcRight, summaryData);
		renderInsightDeposit(pcBottom, totalTabungan, totalRealisasi, sisaTabungan, mutasiList.size(), summaryData.size());
	}

	private void appendFilter(Toolbar toolbar, String label, Combobox combo) {
		if (toolbar == null || combo == null) {
			return;
		}
		Label l = new Label(label);
		l.setVisible(combo.isVisible());
		toolbar.appendChild(l);
		toolbar.appendChild(combo);
	}

	@SuppressWarnings("unchecked")
	private List<Long> ambilJurusanIds(Session session, Fakultas filterFakultas) {
		if (filterFakultas == null || filterFakultas.getId() == null || session == null) {
			return null;
		}
		return session.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", true))
				.setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", filterFakultas)).list();
	}

	private SQLQuery buildMutasiQuery(Session session, String searchName, Fakultas filterFakultas, Jurusan filterJurusan,
			Yayasan filterYayasan, Sekolah filterSekolah, List<Long> longsJurusans) {
		StringBuilder sql = new StringBuilder();
		sql.append("select ");
		sql.append("(case when d.id is not null then 'SSW_'||d.id when e.id is not null then 'CSW_'||e.id ");
		sql.append("when b.id is not null then 'MHS_'||b.id when c.id is not null then 'BCM_'||c.id ");
		sql.append("when f.id is not null then 'AKP_'||f.id else 'OTH_' end) as person_key, ");
		sql.append("(case when d.nomor_induk is not null then d.nomor_induk when e.nomor_induk is not null then e.nomor_induk ");
		sql.append("when b.nim is not null then b.nim when c.no_registrasi is not null then c.no_registrasi else f.kode end) as nomor, ");
		sql.append("(case when d.nama_siswa is not null then d.nama_siswa when e.nama_siswa is not null then e.nama_siswa ");
		sql.append("when b.nama is not null then b.nama when c.nama is not null then c.nama else f.nama end) as nama, ");
		sql.append("a.tanggal, a.deposit, a.keterangan, a.jenis_mutasi ");
		sql.append("from ( ");
		sql.append("select a.waktu as tanggal, a.mahasiswa, a.biodata_calon_mahasiswa, a.siswa, a.calon_siswa, a.anggota_koperasi, a.nominal as deposit, a.keterangan, 'TOPUP_TABUNGAN' as jenis_mutasi from deposit a ");
		sql.append("union all ");
		sql.append("select a.tanggal, b.mahasiswa, b.calon_mahasiswa as biodata_calon_mahasiswa, null as siswa, null as calon_siswa, null as anggota_koperasi, -(a.deposit) as deposit, a.keterangan, 'PEMAKAIAN_TAGIHAN_MAHASISWA' as jenis_mutasi from cicilan_pembayaran a inner join kegiatan b on (a.kegiatan=b.id) where a.deposit > 0.1 ");
		sql.append("union all ");
		sql.append("select a.waktu as tanggal, a.mahasiswa, a.calon_mahasiswa as biodata_calon_mahasiswa, a.siswa, a.calon_siswa, null as anggota_koperasi, -(a.nominal) as deposit, a.keterangan, 'PENGELUARAN_MANUAL' as jenis_mutasi from pengeluaran_mahasiswa a ");
		sql.append("union all ");
		sql.append("select a.tanggal as tanggal, null as mahasiswa, null as biodata_calon_mahasiswa, a.siswa_id as siswa, a.calon_siswa_id as calon_siswa, null as anggota_koperasi, -(a.daritabungan) as deposit, a.keterangan, 'PEMAKAIAN_TAGIHAN_SISWA' as jenis_mutasi from sekolah.pembayaran_siswa a where a.daritabungan > 0.1 ");
		sql.append(") a ");
		sql.append("left join mahasiswa b on (a.mahasiswa=b.id) ");
		sql.append("left join biodata_calon_mahasiswa c on (a.biodata_calon_mahasiswa=c.id) ");
		sql.append("left join sekolah.siswa d on (a.siswa=d.id) ");
		sql.append("left join sekolah.calon_siswa e on (a.calon_siswa=e.id) ");
		sql.append("left join koperasi.anggota_koperasi f on (a.anggota_koperasi=f.id) ");
		sql.append("where 1=1 ");
		if (getLoginMahasiswaId() != null) {
			sql.append("and a.mahasiswa = :loginMahasiswaId ");
		}
		if (getLoginSiswaId() != null) {
			sql.append("and a.siswa = :loginSiswaId ");
		}
		if (searchName != null && searchName.trim().length() > 0) {
			sql.append("and (b.nama ilike :search or c.nama ilike :search or d.nama_siswa ilike :search ");
			sql.append("or e.nama_siswa ilike :search or f.nama ilike :search or f.kode ilike :search ");
			sql.append("or b.nim ilike :search or c.no_registrasi ilike :search or d.nomor_induk ilike :search or e.nomor_induk ilike :search) ");
		}
		if (filterSekolah != null && filterSekolah.getId() != null) {
			sql.append("and (d.sekolah_id = :sekolahId or e.sekolah_id = :sekolahId) ");
		}
		if (filterYayasan != null && filterYayasan.getId() != null) {
			sql.append("and (d.yayasan_id = :yayasanId or e.yayasan_id = :yayasanId) ");
		}
		if (filterJurusan != null && filterJurusan.getId() != null) {
			sql.append("and (b.jurusan = :jurusanId or c.prodi_1 = :jurusanId or c.prodi_lulus = :jurusanId) ");
		} else if (filterFakultas != null && filterFakultas.getId() != null) {
			if (longsJurusans != null && !longsJurusans.isEmpty()) {
				sql.append("and (b.jurusan in (:listJurusan) or c.prodi_1 in (:listJurusan) or c.prodi_lulus in (:listJurusan)) ");
			} else {
				sql.append("and 1=0 ");
			}
		}
		sql.append("order by a.tanggal asc, person_key asc ");

		SQLQuery query = session.createSQLQuery(sql.toString());
		query.setFetchSize(500);
		if (getLoginMahasiswaId() != null) {
			query.setParameter("loginMahasiswaId", getLoginMahasiswaId());
		}
		if (getLoginSiswaId() != null) {
			query.setParameter("loginSiswaId", getLoginSiswaId());
		}
		if (searchName != null && searchName.trim().length() > 0) {
			query.setParameter("search", "%" + searchName.trim() + "%");
		}
		if (filterSekolah != null && filterSekolah.getId() != null) {
			query.setParameter("sekolahId", filterSekolah.getId());
		}
		if (filterYayasan != null && filterYayasan.getId() != null) {
			query.setParameter("yayasanId", filterYayasan.getId());
		}
		if (filterJurusan != null && filterJurusan.getId() != null) {
			query.setParameter("jurusanId", filterJurusan.getId());
		} else if (filterFakultas != null && filterFakultas.getId() != null && longsJurusans != null
				&& !longsJurusans.isEmpty()) {
			query.setParameterList("listJurusan", longsJurusans);
		}
		return query;
	}

	private void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			session.disconnect();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void addMapValue(Map<String, Double> map, String key, double value) {
		if (key == null || key.trim().length() == 0) {
			key = "Tidak diketahui";
		}
		Double old = map.get(key);
		map.put(key, Double.valueOf((old == null ? 0.0 : old.doubleValue()) + value));
	}

	private void addIntegerValue(Map<String, Integer> map, String key, int value) {
		if (key == null || key.trim().length() == 0) {
			key = "Tidak diketahui";
		}
		Integer old = map.get(key);
		map.put(key, Integer.valueOf((old == null ? 0 : old.intValue()) + value));
	}

	private String labelJenisMutasi(String jenis) {
		if (jenis == null) {
			return "Lainnya";
		}
		if ("TOPUP_TABUNGAN".equals(jenis)) {
			return "Topup Tabungan";
		}
		if ("PEMAKAIAN_TAGIHAN_MAHASISWA".equals(jenis)) {
			return "Pembayaran Tagihan Mahasiswa";
		}
		if ("PEMAKAIAN_TAGIHAN_SISWA".equals(jenis)) {
			return "Pembayaran Tagihan Siswa";
		}
		if ("PENGELUARAN_MANUAL".equals(jenis)) {
			return "Pengeluaran Manual";
		}
		return jenis.replace('_', ' ');
	}

	private String monthKey(Date tanggal) {
		if (tanggal == null) {
			return "Tanpa Tanggal";
		}
		try {
			return Common.dateFormat83.get().format(tanggal);
		} catch (Exception e) {
			return "Tanpa Tanggal";
		}
	}

	private void akumulasiSummary(Map<String, Map<String, Object>> summaryData, String personKey, String nama,
			double nominal) {
		Map<String, Object> data = summaryData.get(personKey);
		if (data == null) {
			data = new HashMap<String, Object>();
			data.put("nama", nama);
			data.put("sumDebit", Double.valueOf(0.0));
			data.put("sumKredit", Double.valueOf(0.0));
			data.put("sumSaldo", Double.valueOf(0.0));
			summaryData.put(personKey, data);
		}
		if (nominal >= 0.0) {
			data.put("sumDebit", Double.valueOf(((Double) data.get("sumDebit")).doubleValue() + nominal));
		} else {
			data.put("sumKredit", Double.valueOf(((Double) data.get("sumKredit")).doubleValue() + Math.abs(nominal)));
		}
		data.put("sumSaldo", Double.valueOf(((Double) data.get("sumSaldo")).doubleValue() + nominal));
	}

	private String formatMoney(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat3.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private String html(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private String dashboardCard(String title, String value, String desc) {
		return "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:13px;"
				+ "box-shadow:0 2px 8px rgba(15,23,42,0.07);min-width:145px;flex:1;'>"
				+ "<div style='font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;'>" + html(title)
				+ "</div><div style='font-size:18px;color:#0f172a;font-weight:800;margin-top:5px;'>" + html(value)
				+ "</div><div style='font-size:11px;color:#64748b;margin-top:4px;'>" + html(desc) + "</div></div>";
	}

	private void renderDashboardDeposit(Component parent, double totalMasuk, double totalKeluar, double saldo,
			int jumlahTransaksi, int jumlahPenabung, Date transaksiTerakhir) {
		Panel panel = new Panel();
		panel.setTitle("Dasbor Tabungan / Deposit");
		panel.setBorder("normal");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);

		double rasioPemakaian = totalMasuk > 0.0 ? (totalKeluar * 100.0 / totalMasuk) : 0.0;
		if (rasioPemakaian > 100.0) {
			rasioPemakaian = 100.0;
		}
		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:10px;line-height:1.45;'>Ringkasan uang masuk, uang terpakai, saldo akhir, dan jumlah penabung dalam satu tampilan mudah dibaca.</div>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;margin-bottom:12px;'>"
				+ dashboardCard("Total Masuk", "Rp " + formatMoney(totalMasuk), "Topup/tabungan diterima")
				+ dashboardCard("Total Terpakai", "Rp " + formatMoney(totalKeluar), "Pembayaran dari tabungan")
				+ dashboardCard("Saldo Bersih", "Rp " + formatMoney(saldo), "Sisa keseluruhan")
				+ dashboardCard("Penabung", String.valueOf(jumlahPenabung), jumlahTransaksi + " transaksi")
				+ "</div>"
				+ "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;'>"
				+ "<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;'>"
				+ "<div style='font-size:13px;font-weight:700;color:#0f172a;'>Rasio Pemakaian Tabungan</div>"
				+ "<div style='font-size:12px;color:#334155;font-weight:700;'>" + formatMoney(rasioPemakaian) + "%</div></div>"
				+ "<div style='height:14px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:14px;width:" + formatMoney(rasioPemakaian).replace(',', '.')
				+ "%;background:linear-gradient(90deg,#22c55e,#f59e0b);'></div></div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:8px;'>Mutasi terakhir: "
				+ html(formatDate(transaksiTerakhir)) + ". Angka dihitung dari topup, pemakaian untuk pembayaran, dan pengeluaran tabungan.</div>"
				+ "</div></div>";
		children.appendChild(new org.zkoss.zul.Html(html));
	}

	private int percentValue(double value, double total) {
		if (total <= 0.0) {
			return 0;
		}
		int p = (int) Math.round((value * 100.0d) / total);
		if (p < 0) {
			return 0;
		}
		if (p > 100) {
			return 100;
		}
		return p;
	}

	private String safeWidth(double value, double total) {
		return String.valueOf(percentValue(value, total));
	}

	private void renderDashboardJenisMutasi(Component parent, Map<String, Double> pemasukanPerJenis,
			Map<String, Double> pengeluaranPerJenis, Map<String, Integer> jumlahPerJenis) {
		Panel panel = new Panel();
		panel.setTitle("Analisis Jenis Pemasukan dan Pengeluaran Tabungan");
		panel.setBorder("normal");
		panel.setStyle("margin-top:12px;");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);

		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:10px;line-height:1.45;'>Sumber uang masuk dan uang keluar dipisahkan agar arus tabungan lebih mudah dicek.</div>"
				+ buildJenisMutasiHtml("Pemasukan Tabungan", pemasukanPerJenis, jumlahPerJenis, "#16a34a")
				+ buildJenisMutasiHtml("Pengeluaran Tabungan", pengeluaranPerJenis, jumlahPerJenis, "#dc2626")
				+ "</div>";
		children.appendChild(new org.zkoss.zul.Html(html));
	}

	private String buildJenisMutasiHtml(String title, Map<String, Double> data, Map<String, Integer> jumlahPerJenis,
			String color) {
		StringBuilder sb = new StringBuilder();
		double total = 0.0;
		if (data != null) {
			for (Double value : data.values()) {
				if (value != null) {
					total += value.doubleValue();
				}
			}
		}
		sb.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;margin-bottom:12px;'>");
		sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin-bottom:8px;'>").append(html(title)).append("</div>");
		if (data == null || data.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada data pada kelompok ini.</div>");
		} else {
			for (String key : data.keySet()) {
				Double value = data.get(key);
				double nominal = value == null ? 0.0 : value.doubleValue();
				Integer jumlah = jumlahPerJenis == null ? null : jumlahPerJenis.get(key);
				sb.append("<div style='margin:8px 0;'>");
				sb.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;font-weight:700;color:#334155;'>")
						.append("<span>").append(html(labelJenisMutasi(key))).append("</span>")
						.append("<span>Rp ").append(html(formatMoney(nominal))).append(" • ")
						.append(jumlah == null ? 0 : jumlah.intValue()).append(" transaksi</span></div>");
				sb.append("<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'>")
						.append("<div style='height:10px;width:").append(safeWidth(nominal, total))
						.append("%;background:").append(color).append(";border-radius:999px;'></div></div>");
				sb.append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderDashboardKesehatanSaldo(Component parent, double totalMasuk, double totalKeluar, double saldo,
			Map<String, Map<String, Object>> summaryData, int jumlahTransaksi) {
		Panel panel = new Panel();
		panel.setTitle("Radar Kesehatan Saldo Tabungan");
		panel.setBorder("normal");
		panel.setStyle("margin-top:12px;");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);

		int saldoPositif = 0;
		int saldoNolAtauMinus = 0;
		for (Map<String, Object> data : summaryData.values()) {
			Double saldoPenabung = data == null ? null : (Double) data.get("sumSaldo");
			if (saldoPenabung != null && saldoPenabung.doubleValue() > 0.0) {
				saldoPositif++;
			} else {
				saldoNolAtauMinus++;
			}
		}
		int rasioPakai = percentValue(totalKeluar, totalMasuk <= 0.0 ? 1.0 : totalMasuk);
		int rasioSisa = percentValue(saldo, totalMasuk <= 0.0 ? 1.0 : totalMasuk);
		int rasioPenabungAktif = percentValue(saldoPositif, Math.max(1, summaryData.size()));
		int keamananPemakaian = 100 - rasioPakai;
		if (keamananPemakaian < 0) {
			keamananPemakaian = 0;
		}
		int aktivitasMutasi = jumlahTransaksi <= 0 ? 0 : Math.min(100, 20 + (jumlahTransaksi * 4));
		int saldoPenabungPositif = percentValue(saldoPositif, Math.max(1, saldoPositif + saldoNolAtauMinus));

		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:10px;line-height:1.45;'>Kesehatan tabungan dibaca dari sisa saldo, keamanan pemakaian, penabung aktif, dan kelengkapan mutasi.</div>"
				+ "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:14px;'>"
				+ buildSpiderWebHtml("Spider Kesehatan Tabungan",
						new String[] { "Saldo", "Aman", "Aktif", "Positif", "Mutasi" },
						new int[] { rasioSisa, keamananPemakaian, rasioPenabungAktif, saldoPenabungPositif, aktivitasMutasi })
				+ buildMiniGauge("Sisa saldo terhadap total masuk", rasioSisa, "#16a34a")
				+ buildMiniGauge("Keamanan pemakaian", keamananPemakaian, "#2563eb")
				+ buildMiniGauge("Penabung masih bersaldo", rasioPenabungAktif, "#7c3aed")
				+ "<div style='margin-top:10px;font-size:11px;color:#64748b;'>Penabung saldo positif: "
				+ saldoPositif + ", saldo nol/minus: " + saldoNolAtauMinus + ".</div>"
				+ "</div></div>";
		children.appendChild(new org.zkoss.zul.Html(html));
	}

	private String buildSpiderWebHtml(String title, String[] labels, int[] values) {
		if (labels == null || values == null || labels.length == 0 || labels.length != values.length) {
			return "";
		}
		int n = labels.length;
		double cx = 110.0d;
		double cy = 100.0d;
		double radius = 72.0d;
		StringBuilder axis = new StringBuilder();
		StringBuilder points = new StringBuilder();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double angle = (-90.0d + (360.0d * i / n)) * Math.PI / 180.0d;
			double outerX = cx + radius * Math.cos(angle);
			double outerY = cy + radius * Math.sin(angle);
			int value = values[i];
			if (value < 0) {
				value = 0;
			} else if (value > 100) {
				value = 100;
			}
			double pointX = cx + radius * value / 100.0d * Math.cos(angle);
			double pointY = cy + radius * value / 100.0d * Math.sin(angle);
			axis.append("<line x1='").append(toSvg(cx)).append("' y1='").append(toSvg(cy)).append("' x2='")
					.append(toSvg(outerX)).append("' y2='").append(toSvg(outerY))
					.append("' stroke='#cbd5e1' stroke-width='1'/>");
			if (points.length() > 0) {
				points.append(" ");
			}
			points.append(toSvg(pointX)).append(",").append(toSvg(pointY));
			double labelX = cx + (radius + 17.0d) * Math.cos(angle);
			double labelY = cy + (radius + 17.0d) * Math.sin(angle);
			text.append("<text x='").append(toSvg(labelX)).append("' y='").append(toSvg(labelY))
					.append("' text-anchor='middle' font-size='9' fill='#334155'>")
					.append(html(labels[i])).append("</text>");
		}
		StringBuilder rings = new StringBuilder();
		for (int r = 1; r <= 4; r++) {
			double rr = radius * r / 4.0d;
			rings.append("<circle cx='").append(toSvg(cx)).append("' cy='").append(toSvg(cy)).append("' r='")
					.append(toSvg(rr)).append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:12px;'>");
		sb.append("<div style='min-width:235px;'><div style='font-size:13px;font-weight:800;color:#0f172a;margin-bottom:4px;'>")
				.append(html(title)).append("</div>");
		sb.append("<svg width='235' height='205' viewBox='0 0 220 200' style='max-width:100%;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;'>")
				.append(rings).append(axis)
				.append("<polygon points='").append(points)
				.append("' fill='rgba(37,99,235,0.22)' stroke='#2563eb' stroke-width='2'/>")
				.append(text).append("</svg></div>");
		sb.append("<div style='flex:1;min-width:190px;'>");
		for (int i = 0; i < n; i++) {
			int value = values[i];
			if (value < 0) {
				value = 0;
			} else if (value > 100) {
				value = 100;
			}
			sb.append(buildMiniGauge(labels[i], value, "#2563eb"));
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String toSvg(double value) {
		return String.valueOf(Math.round(value * 10.0d) / 10.0d);
	}

	private String buildMiniGauge(String label, int percent, String color) {
		return "<div style='margin-bottom:10px;'><div style='display:flex;justify-content:space-between;font-size:11px;font-weight:700;color:#334155;'>"
				+ "<span>" + html(label) + "</span><span>" + percent + "%</span></div>"
				+ "<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'>"
				+ "<div style='height:12px;width:" + percent + "%;background:" + color
				+ ";border-radius:999px;'></div></div></div>";
	}

	private void renderInsightDeposit(Component parent, double totalMasuk, double totalKeluar, double saldo,
			int transaksi, int penabung) {
		Panel panel = new Panel();
		panel.setTitle("Insight dan Tindak Lanjut Tabungan");
		panel.setBorder("normal");
		panel.setStyle("margin-top:12px;");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);

		String status;
		String saran;
		if (totalMasuk <= 0.0) {
			status = "Belum Ada Mutasi";
			saran = "Belum ada topup atau mutasi tabungan pada filter ini. Periksa kembali filter atau periode data.";
		} else if (saldo <= 0.0) {
			status = "Saldo Menipis";
			saran = "Saldo bersih sudah habis atau negatif. Petugas dapat mengingatkan penabung agar melakukan topup sebelum transaksi berikutnya.";
		} else if (totalKeluar > totalMasuk * 0.8d) {
			status = "Pemakaian Tinggi";
			saran = "Pemakaian tabungan sudah mendekati total topup. Pantau transaksi pembayaran agar saldo tidak tiba-tiba habis.";
		} else {
			status = "Relatif Sehat";
			saran = "Saldo tabungan masih tersedia dan pemakaian masih terkendali. Tetap pantau mutasi rutin melalui buku besar.";
		}
		String html = "<div style='font-family:Arial,sans-serif;background:white;border:1px solid #e2e8f0;border-radius:14px;padding:14px;'>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:8px;'>Kesimpulan singkat dari data tabungan untuk membantu menentukan langkah berikutnya.</div>"
				+ "<div style='font-size:18px;font-weight:800;color:#0f172a;margin-bottom:6px;'>" + html(status) + "</div>"
				+ "<div style='font-size:12px;color:#334155;line-height:1.55;'>" + html(saran) + "</div>"
				+ "<div style='display:flex;gap:8px;flex-wrap:wrap;margin-top:12px;'>"
				+ dashboardCard("Transaksi", String.valueOf(transaksi), "Jumlah mutasi pada filter ini")
				+ dashboardCard("Penabung", String.valueOf(penabung), "Jumlah orang/member yang terlibat")
				+ dashboardCard("Saldo", "Rp " + formatMoney(saldo), "Saldo bersih keseluruhan")
				+ "</div></div>";
		children.appendChild(new org.zkoss.zul.Html(html));
	}

	private void renderGrafikDeposit(Component parent, final Map<String, Double> trendMasuk,
			final Map<String, Double> trendKeluar, Map<String, Map<String, Object>> summaryData, double totalMasuk,
			double totalKeluar) {
		Panel panel = new Panel();
		panel.setTitle("Grafik & Analisis Deposit");
		panel.setBorder("normal");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);

		children.appendChild(new org.zkoss.zul.Html(
				"<div style='font-family:Arial,sans-serif;font-size:11px;color:#64748b;margin-bottom:10px;line-height:1.45;'>Perubahan tabungan dari waktu ke waktu, komposisi mutasi, dan penabung dengan saldo terbesar ditampilkan dalam grafik ringan.</div>"));
		renderTrendMutasiBulanan(children, trendMasuk, trendKeluar);
		children.appendChild(new org.zkoss.zul.Html("<div style='font-family:Arial,sans-serif;'>"
				+ buildKomposisiHtml(totalMasuk, totalKeluar) + buildTopPenabungHtml(summaryData) + "</div>"));
	}

	private void renderTrendMutasiBulanan(Component parent, final Map<String, Double> trendMasuk,
			final Map<String, Double> trendKeluar) {
		final List<String> keys = buildTrendKeys(trendMasuk, trendKeluar);
		final double max = hitungMaxTrend(trendMasuk, trendKeluar, keys);
		final org.zkoss.zul.Vbox trendBox = new org.zkoss.zul.Vbox();
		trendBox.setWidth("100%");
		trendBox.setParent(parent);

		final org.zkoss.zul.Html trendHtml = new org.zkoss.zul.Html();
		trendHtml.setParent(trendBox);
		renderTrendHtmlPage(trendHtml, trendMasuk, trendKeluar, keys, max, 0);

		if (keys.size() > TREND_MUTASI_PAGE_SIZE) {
			final Paging trendPaging = new Paging();
			trendPaging.setPageSize(TREND_MUTASI_PAGE_SIZE);
			trendPaging.setTotalSize(keys.size());
			trendPaging.setDetailed(true);
			trendPaging.setStyle("margin:0 0 12px 0;");
			trendPaging.setParent(trendBox);
			trendPaging.addEventListener("onPaging", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					int activePage = 0;
					try {
						activePage = ((org.zkoss.zul.event.PagingEvent) event).getActivePage();
					} catch (Exception e) {
						activePage = trendPaging.getActivePage();
					}
					renderTrendHtmlPage(trendHtml, trendMasuk, trendKeluar, keys, max, activePage);
				}
			});
		}
	}

	private List<String> buildTrendKeys(Map<String, Double> trendMasuk, Map<String, Double> trendKeluar) {
		TreeSet<String> keySet = new TreeSet<String>();
		if (trendMasuk != null) {
			keySet.addAll(trendMasuk.keySet());
		}
		if (trendKeluar != null) {
			keySet.addAll(trendKeluar.keySet());
		}
		return new ArrayList<String>(keySet);
	}

	private double hitungMaxTrend(Map<String, Double> trendMasuk, Map<String, Double> trendKeluar,
			List<String> keys) {
		double max = 0.0;
		if (keys != null) {
			for (String key : keys) {
				double in = getTrendValue(trendMasuk, key);
				double out = getTrendValue(trendKeluar, key);
				if (in > max) {
					max = in;
				}
				if (out > max) {
					max = out;
				}
			}
		}
		return max <= 0.0 ? 1.0 : max;
	}

	private double getTrendValue(Map<String, Double> trend, String key) {
		if (trend == null || key == null || !trend.containsKey(key) || trend.get(key) == null) {
			return 0.0;
		}
		return trend.get(key).doubleValue();
	}

	private void renderTrendHtmlPage(org.zkoss.zul.Html target, Map<String, Double> trendMasuk,
			Map<String, Double> trendKeluar, List<String> keys, double max, int activePage) {
		if (target == null) {
			return;
		}
		target.setContent(buildTrendHtmlPage(trendMasuk, trendKeluar, keys, max, activePage));
	}

	private String buildTrendHtmlPage(Map<String, Double> trendMasuk, Map<String, Double> trendKeluar,
			List<String> keys, double max, int activePage) {
		if (keys == null) {
			keys = new ArrayList<String>();
		}
		if (activePage < 0) {
			activePage = 0;
		}
		int total = keys.size();
		int startIndex = activePage * TREND_MUTASI_PAGE_SIZE;
		if (startIndex >= total && total > 0) {
			activePage = (total - 1) / TREND_MUTASI_PAGE_SIZE;
			startIndex = activePage * TREND_MUTASI_PAGE_SIZE;
		}
		int endIndex = Math.min(startIndex + TREND_MUTASI_PAGE_SIZE, total);

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;margin-bottom:8px;'>");
		sb.append("<div style='display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:8px;'>");
		sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;'>Tren Mutasi Bulanan</div>");
		if (total > 0) {
			sb.append("<div style='font-size:11px;color:#64748b;font-weight:700;'>Menampilkan ")
					.append(startIndex + 1).append("-").append(endIndex).append(" dari ").append(total)
					.append(" data</div>");
		}
		sb.append("</div>");
		sb.append("<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>Setiap halaman menampilkan maksimal 10 bulan agar grafik tetap ringkas. Bar hijau menunjukkan uang masuk, sedangkan bar merah menunjukkan uang keluar.</div>");
		if (total == 0) {
			sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada mutasi tabungan pada filter ini.</div>");
		} else {
			for (int i = startIndex; i < endIndex; i++) {
				String key = keys.get(i);
				double in = getTrendValue(trendMasuk, key);
				double out = getTrendValue(trendKeluar, key);
				sb.append("<div style='margin:8px 0;'>");
				sb.append("<div style='font-size:11px;color:#334155;font-weight:700;margin-bottom:3px;'>")
						.append(html(key)).append("</div>");
				sb.append("<div style='display:flex;align-items:center;gap:6px;font-size:10px;color:#64748b;'>Masuk <div style='height:10px;background:#22c55e;border-radius:999px;width:")
						.append(safeWidth(in, max)).append("%;'></div><span>Rp ").append(html(formatMoney(in)))
						.append("</span></div>");
				sb.append("<div style='display:flex;align-items:center;gap:6px;font-size:10px;color:#64748b;margin-top:2px;'>Keluar <div style='height:10px;background:#ef4444;border-radius:999px;width:")
						.append(safeWidth(out, max)).append("%;'></div><span>Rp ").append(html(formatMoney(out)))
						.append("</span></div>");
				sb.append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildKomposisiHtml(double totalMasuk, double totalKeluar) {
		double total = totalMasuk + totalKeluar;
		double masukPct = total > 0.0 ? totalMasuk * 100.0 / total : 0.0;
		double keluarPct = total > 0.0 ? totalKeluar * 100.0 / total : 0.0;
		return "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;margin-bottom:12px;'>"
				+ "<div style='font-size:14px;font-weight:800;color:#0f172a;margin-bottom:8px;'>Komposisi Mutasi</div>"
				+ "<div style='height:18px;background:#e2e8f0;border-radius:999px;overflow:hidden;display:flex;'>"
				+ "<div title='Masuk' style='height:18px;background:#22c55e;width:" + formatMoney(masukPct).replace(',', '.')
				+ "%;'></div>"
				+ "<div title='Keluar' style='height:18px;background:#ef4444;width:" + formatMoney(keluarPct).replace(',', '.')
				+ "%;'></div></div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:8px;'>Masuk: Rp " + html(formatMoney(totalMasuk))
				+ " (" + html(formatMoney(masukPct)) + "%) &nbsp; | &nbsp; Keluar: Rp " + html(formatMoney(totalKeluar))
				+ " (" + html(formatMoney(keluarPct)) + "%)</div></div>";
	}

	private String buildTopPenabungHtml(Map<String, Map<String, Object>> summaryData) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(summaryData.values());
		Collections.sort(rows, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				Double d1 = (Double) o1.get("sumSaldo");
				Double d2 = (Double) o2.get("sumSaldo");
				return d2.compareTo(d1);
			}
		});

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;'>");
		sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin-bottom:8px;'>Top Saldo Penabung</div>");
		if (rows.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada data saldo.</div>");
		} else {
			int limit = Math.min(5, rows.size());
			for (int i = 0; i < limit; i++) {
				Map<String, Object> row = rows.get(i);
				double saldo = ((Double) row.get("sumSaldo")).doubleValue();
				sb.append("<div style='display:flex;justify-content:space-between;border-bottom:1px dashed #e2e8f0;padding:6px 0;'>")
						.append("<span style='font-size:11px;color:#334155;'>").append(html(String.valueOf(row.get("nama"))))
						.append("</span><span style='font-size:11px;font-weight:800;color:#0f172a;'>Rp ").append(html(formatMoney(saldo)))
						.append("</span></div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderWatchlistSaldoPenabung(Component parent, Map<String, Map<String, Object>> summaryData) {
		Panel panel = new Panel();
		panel.setTitle("Watchlist Saldo Penabung");
		panel.setBorder("normal");
		panel.setStyle("margin-top:12px;");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);
		children.appendChild(new org.zkoss.zul.Html(buildWatchlistSaldoHtml(summaryData)));
	}

	private String buildWatchlistSaldoHtml(Map<String, Map<String, Object>> summaryData) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;'>");
		sb.append("<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:10px;'>Penabung dengan saldo nol/minus diprioritaskan untuk follow-up, sedangkan saldo terbesar membantu membaca konsentrasi dana.</div>");
		if (summaryData == null || summaryData.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada data saldo untuk watchlist.</div></div>");
			return sb.toString();
		}
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(summaryData.values());
		Collections.sort(rows, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				Double d1 = (Double) o1.get("sumSaldo");
				Double d2 = (Double) o2.get("sumSaldo");
				return d1.compareTo(d2);
			}
		});
		sb.append("<div style='display:grid;grid-template-columns:repeat(2,minmax(220px,1fr));gap:10px;'>");
		sb.append("<div><div style='font-size:13px;font-weight:800;color:#991b1b;margin-bottom:6px;'>Saldo Nol / Minus</div>");
		int rendah = 0;
		for (Map<String, Object> row : rows) {
			double saldo = ((Double) row.get("sumSaldo")).doubleValue();
			if (saldo > 0.0) {
				continue;
			}
			appendWatchlistRow(sb, row, saldo, "#991b1b");
			rendah++;
			if (rendah >= 5) {
				break;
			}
		}
		if (rendah == 0) {
			sb.append("<div style='font-size:12px;color:#166534;background:#f0fdf4;border-radius:8px;padding:8px;'>Tidak ada saldo nol/minus.</div>");
		}
		sb.append("</div>");

		Collections.sort(rows, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				Double d1 = (Double) o1.get("sumSaldo");
				Double d2 = (Double) o2.get("sumSaldo");
				return d2.compareTo(d1);
			}
		});
		sb.append("<div><div style='font-size:13px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Saldo Terbesar</div>");
		int tinggi = Math.min(5, rows.size());
		for (int i = 0; i < tinggi; i++) {
			Map<String, Object> row = rows.get(i);
			double saldo = ((Double) row.get("sumSaldo")).doubleValue();
			appendWatchlistRow(sb, row, saldo, "#0f172a");
		}
		sb.append("</div></div></div>");
		return sb.toString();
	}

	private void appendWatchlistRow(StringBuilder sb, Map<String, Object> row, double saldo, String color) {
		sb.append("<div style='display:flex;justify-content:space-between;gap:8px;border-bottom:1px dashed #e2e8f0;padding:6px 0;'>");
		sb.append("<span style='font-size:11px;color:#334155;'>").append(html(String.valueOf(row.get("nama"))))
				.append("</span>");
		sb.append("<span style='font-size:11px;font-weight:800;color:").append(color).append(";'>Rp ")
				.append(html(formatMoney(saldo))).append("</span>");
		sb.append("</div>");
	}

	private MyToolbarbuttonConfig createExcelButton(final List<Map<String, Object>> mutasiList) {
		MyToolbarbuttonConfig btnExcelMutasi = new MyToolbarbuttonConfig("Download Excel", "/img/excel.gif");
		btnExcelMutasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (mutasiList == null || mutasiList.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada data mutasi untuk didownload", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:x='urn:schemas-microsoft-com:office:excel' xmlns='http://www.w3.org/TR/REC-html40'>");
				sb.append("<head><meta charset='UTF-8'></head><body><table border='1'>");
				sb.append("<tr style='background-color:#0f172a;color:white;font-weight:bold;'>");
				sb.append("<th>Nama</th><th>Tanggal</th><th>Jenis Mutasi</th><th>Keterangan</th><th>Masuk</th><th>Keluar</th><th>Saldo Per Penabung</th><th>Saldo Total</th></tr>");

				double runningBalance = 0.0;
				double totalDebit = 0.0;
				double totalKredit = 0.0;
				Map<String, Double> saldoPerOrangExcel = new HashMap<String, Double>();
				for (Map<String, Object> m : mutasiList) {
					String pKey = (String) m.get("personKey");
					double debit = ((Double) m.get("debit")).doubleValue();
					double kredit = ((Double) m.get("kredit")).doubleValue();
					double curPersonal = saldoPerOrangExcel.containsKey(pKey) ? saldoPerOrangExcel.get(pKey).doubleValue() : 0.0;
					curPersonal += debit - kredit;
					saldoPerOrangExcel.put(pKey, Double.valueOf(curPersonal));
					runningBalance += debit - kredit;
					totalDebit += debit;
					totalKredit += kredit;

					sb.append("<tr>");
					sb.append("<td>").append(html(String.valueOf(m.get("nama")))).append("</td>");
					sb.append("<td>").append(formatDate((Date) m.get("tanggal"))).append("</td>");
					sb.append("<td>").append(html(labelJenisMutasi(String.valueOf(m.get("jenisMutasi"))))).append("</td>");
					sb.append("<td>").append(html(String.valueOf(m.get("keterangan")))).append("</td>");
					sb.append("<td align='right'>").append(debit).append("</td>");
					sb.append("<td align='right'>").append(kredit).append("</td>");
					sb.append("<td align='right'>").append(curPersonal).append("</td>");
					sb.append("<td align='right'>").append(runningBalance).append("</td>");
					sb.append("</tr>");
				}
				sb.append("<tr style='font-weight:bold;background-color:#f1f5f9;'><td colspan='4' align='right'>TOTAL</td>");
				sb.append("<td align='right'>").append(totalDebit).append("</td><td align='right'>").append(totalKredit)
						.append("</td><td align='right'>-</td><td align='right'>").append(totalDebit - totalKredit)
						.append("</td></tr></table></body></html>");
				Filedownload.save(sb.toString().getBytes("UTF-8"), "application/vnd.ms-excel",
						"Buku_Besar_Mutasi_Tabungan.xls");
			}
		});
		return btnExcelMutasi;
	}

	private void renderGridMutasi(Component parent, List<Map<String, Object>> mutasiList, double totalMasuk,
			double totalKeluar, double saldo) {
		Panel pnlMutasi = new Panel();
		pnlMutasi.setTitle("Buku Besar Mutasi Tabungan");
		pnlMutasi.setBorder("normal");
		pnlMutasi.setParent(parent);

		Panelchildren pchMutasi = new Panelchildren();
		pchMutasi.setStyle("padding:10px;background:#f8fafc;");
		pchMutasi.setParent(pnlMutasi);
		pchMutasi.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;margin-bottom:8px;line-height:1.45;'>Urutan mutasi tabungan, uang masuk, uang keluar, saldo per penabung, dan saldo total berjalan.</div>"));

		MyGrid gridMutasi = new MyGrid();
		gridMutasi.setMold("paging");
		gridMutasi.setPageSize(10);
		gridMutasi.setSclass("dgrid fgrid");
		gridMutasi.setWidth("100%");
		gridMutasi.setParent(pchMutasi);

		Columns cols = new Columns();
		cols.setParent(gridMutasi);
		new MyColumnConfig("Nama").setParent(cols);
		new MyColumnConfig("Tanggal").setParent(cols);
		new MyColumnConfig("Jenis Mutasi").setParent(cols);
		new MyColumnConfig("Keterangan").setParent(cols);
		MyColumnConfig colDeb = new MyColumnConfig("Masuk (Debit)");
		colDeb.setAlign("right");
		colDeb.setParent(cols);
		MyColumnConfig colKre = new MyColumnConfig("Keluar (Kredit)");
		colKre.setAlign("right");
		colKre.setParent(cols);
		MyColumnConfig colSalPer = new MyColumnConfig("Saldo Per Penabung");
		colSalPer.setAlign("right");
		colSalPer.setParent(cols);
		MyColumnConfig colSal = new MyColumnConfig("Saldo Total");
		colSal.setAlign("right");
		colSal.setParent(cols);

		Rows rows = new Rows();
		rows.setParent(gridMutasi);

		double runningBalance = 0.0;
		Map<String, Double> saldoPerOrang = new HashMap<String, Double>();
		for (Map<String, Object> m : mutasiList) {
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label(String.valueOf(m.get("nama"))));
			r.appendChild(new Label(formatDate((Date) m.get("tanggal"))));
			r.appendChild(new Label(labelJenisMutasi(String.valueOf(m.get("jenisMutasi")))));
			r.appendChild(new Label(String.valueOf(m.get("keterangan"))));
			double debit = ((Double) m.get("debit")).doubleValue();
			double kredit = ((Double) m.get("kredit")).doubleValue();
			String pKey = (String) m.get("personKey");
			double curPersonal = saldoPerOrang.containsKey(pKey) ? saldoPerOrang.get(pKey).doubleValue() : 0.0;
			curPersonal += debit - kredit;
			saldoPerOrang.put(pKey, Double.valueOf(curPersonal));
			runningBalance += debit - kredit;
			r.appendChild(new Label(formatMoney(debit)));
			r.appendChild(new Label(formatMoney(kredit)));
			Label lblPersonal = new Label(formatMoney(curPersonal));
			lblPersonal.setStyle("font-weight:bold;color:#0f766e;");
			r.appendChild(lblPersonal);
			r.appendChild(new Label(formatMoney(runningBalance)));
		}
		renderMutasiFooter(gridMutasi, totalMasuk, totalKeluar, saldo);
	}

	private void renderMutasiFooter(MyGrid gridMutasi, double totalMasuk, double totalKeluar, double saldo) {
		Foot foot = new Foot();
		foot.setParent(gridMutasi);
		new Footer().setParent(foot);
		new Footer().setParent(foot);
		new Footer().setParent(foot);
		Footer fKet = new Footer();
		fKet.setAlign("right");
		fKet.setParent(foot);
		Label lblTotalText = new Label(ais.common.Common.getBahasaConfig("TOTAL :"));
		lblTotalText.setStyle("font-weight:bold;");
		fKet.appendChild(lblTotalText);

		Footer fDeb = new Footer();
		fDeb.setAlign("right");
		fDeb.setParent(foot);
		Label lblTotDeb = new Label(formatMoney(totalMasuk));
		lblTotDeb.setStyle("font-weight:bold;color:#0f766e;");
		fDeb.appendChild(lblTotDeb);

		Footer fKre = new Footer();
		fKre.setAlign("right");
		fKre.setParent(foot);
		Label lblTotKre = new Label(formatMoney(totalKeluar));
		lblTotKre.setStyle("font-weight:bold;color:#b91c1c;");
		fKre.appendChild(lblTotKre);

		Footer fSalPer = new Footer();
		fSalPer.setAlign("right");
		fSalPer.setParent(foot);
		fSalPer.appendChild(new Label("-"));

		Footer fSal = new Footer();
		fSal.setAlign("right");
		fSal.setParent(foot);
		Label lblTotSal = new Label(formatMoney(saldo));
		lblTotSal.setStyle("font-weight:bold;color:#0f172a;");
		fSal.appendChild(lblTotSal);
	}

	private void renderSummaryPenabung(Component parent, Map<String, Map<String, Object>> summaryData) {
		Panel pnlSummary = new Panel();
		pnlSummary.setTitle("Summary Buku Besar Mutasi Tabungan");
		pnlSummary.setBorder("normal");
		pnlSummary.setStyle("margin-top:12px;");
		pnlSummary.setParent(parent);

		Panelchildren pchSummary = new Panelchildren();
		pchSummary.setStyle("padding:10px;background:#f8fafc;");
		pchSummary.setParent(pnlSummary);
		pchSummary.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;margin-bottom:8px;line-height:1.45;'>Mutasi digabung per penabung agar saldo setiap orang lebih cepat diketahui.</div>"));

		MyGrid gridSummary = new MyGrid();
		gridSummary.setMold("paging");
		gridSummary.setPageSize(10);
		gridSummary.setSclass("dgrid fgrid");
		gridSummary.setWidth("100%");
		gridSummary.setParent(pchSummary);

		Columns colsSum = new Columns();
		colsSum.setParent(gridSummary);
		new MyColumnConfig("Nama").setParent(colsSum);
		MyColumnConfig colSumDeb = new MyColumnConfig("Sum Debit");
		colSumDeb.setAlign("right");
		colSumDeb.setParent(colsSum);
		MyColumnConfig colSumKre = new MyColumnConfig("Sum Kredit");
		colSumKre.setAlign("right");
		colSumKre.setParent(colsSum);
		MyColumnConfig colSumSal = new MyColumnConfig("Sum Saldo");
		colSumSal.setAlign("right");
		colSumSal.setParent(colsSum);

		Rows rowsSum = new Rows();
		rowsSum.setParent(gridSummary);

		double totSumDebit = 0.0;
		double totSumKredit = 0.0;
		double totSumSaldo = 0.0;
		for (Map<String, Object> s : summaryData.values()) {
			MyFormRow rSum = new MyFormRow();
			rSum.setParent(rowsSum);
			double sDeb = ((Double) s.get("sumDebit")).doubleValue();
			double sKre = ((Double) s.get("sumKredit")).doubleValue();
			double sSal = ((Double) s.get("sumSaldo")).doubleValue();
			totSumDebit += sDeb;
			totSumKredit += sKre;
			totSumSaldo += sSal;
			rSum.appendChild(new Label(String.valueOf(s.get("nama"))));
			rSum.appendChild(new Label(formatMoney(sDeb)));
			rSum.appendChild(new Label(formatMoney(sKre)));
			Label lblSal = new Label(formatMoney(sSal));
			lblSal.setStyle("font-weight:bold;color:#0f766e;");
			rSum.appendChild(lblSal);
		}
		renderSummaryFooter(gridSummary, totSumDebit, totSumKredit, totSumSaldo);
	}

	private void renderSummaryFooter(MyGrid gridSummary, double debit, double kredit, double saldo) {
		Foot footSum = new Foot();
		footSum.setParent(gridSummary);
		Footer fSumNama = new Footer();
		fSumNama.setAlign("right");
		fSumNama.setParent(footSum);
		Label lblSumTotalText = new Label(ais.common.Common.getBahasaConfig("TOTAL :"));
		lblSumTotalText.setStyle("font-weight:bold;");
		fSumNama.appendChild(lblSumTotalText);
		Footer fSumDeb = new Footer();
		fSumDeb.setAlign("right");
		fSumDeb.setParent(footSum);
		fSumDeb.appendChild(new Label(formatMoney(debit)));
		Footer fSumKre = new Footer();
		fSumKre.setAlign("right");
		fSumKre.setParent(footSum);
		fSumKre.appendChild(new Label(formatMoney(kredit)));
		Footer fSumSal = new Footer();
		fSumSal.setAlign("right");
		fSumSal.setParent(footSum);
		Label lblSumTotSal = new Label(formatMoney(saldo));
		lblSumTotSal.setStyle("font-weight:bold;color:#0f766e;");
		fSumSal.appendChild(lblSumTotSal);
	}

	private double hitungSaldoPerOrang(Deposit deposit) {
		if (deposit == null) {
			return 0.0;
		}
		if (deposit.getId() != null && saldoDepositCache != null && saldoDepositCache.containsKey(deposit.getId())) {
			Double saldo = saldoDepositCache.get(deposit.getId());
			return saldo == null ? 0.0 : saldo.doubleValue();
		}

		Session session = HibernateUtil.currentSession();
		double totalSaldo = 0.0;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("select sum(a.deposit) from ( ");
			sql.append("select a.mahasiswa, a.biodata_calon_mahasiswa, a.siswa, a.calon_siswa, a.anggota_koperasi, a.nominal as deposit from deposit a ");
			sql.append("union all ");
			sql.append("select b.mahasiswa, b.calon_mahasiswa as biodata_calon_mahasiswa, null as siswa, null as calon_siswa, null as anggota_koperasi, -(a.deposit) as deposit from cicilan_pembayaran a inner join kegiatan b on (a.kegiatan=b.id) where a.deposit > 0.1 ");
			sql.append("union all ");
			sql.append("select a.mahasiswa, a.calon_mahasiswa as biodata_calon_mahasiswa, a.siswa, a.calon_siswa, null as anggota_koperasi, -(a.nominal) as deposit from pengeluaran_mahasiswa a ");
			sql.append("union all ");
			sql.append("select null as mahasiswa, null as biodata_calon_mahasiswa, a.siswa_id as siswa, a.calon_siswa_id as calon_siswa, null as anggota_koperasi, -(a.daritabungan) as deposit from sekolah.pembayaran_siswa a where a.daritabungan > 0.1 ");
			sql.append(") a where 1=1 ");

			String field = null;
			Long id = null;
			if (deposit.getMahasiswa() != null && deposit.getMahasiswa().getId() != null) {
				field = "mahasiswa";
				id = deposit.getMahasiswa().getId();
			} else if (deposit.getBiodataCalonMahasiswa() != null && deposit.getBiodataCalonMahasiswa().getId() != null) {
				field = "biodata_calon_mahasiswa";
				id = deposit.getBiodataCalonMahasiswa().getId();
			} else if (deposit.getSiswa() != null && deposit.getSiswa().getId() != null) {
				field = "siswa";
				id = deposit.getSiswa().getId();
			} else if (deposit.getCalonSiswa() != null && deposit.getCalonSiswa().getId() != null) {
				field = "calon_siswa";
				id = deposit.getCalonSiswa().getId();
			} else if (deposit.getAnggotaKoperasi() != null && deposit.getAnggotaKoperasi().getId() != null) {
				field = "anggota_koperasi";
				id = deposit.getAnggotaKoperasi().getId();
			}
			if (field == null || id == null) {
				return 0.0;
			}
			sql.append("and a.").append(field).append(" = :personId");
			SQLQuery query = session.createSQLQuery(sql.toString());
			query.setParameter("personId", id);
			Number result = (Number) query.uniqueResult();
			if (result != null) {
				totalSaldo = result.doubleValue();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (deposit.getId() != null && saldoDepositCache != null) {
			saldoDepositCache.put(deposit.getId(), Double.valueOf(totalSaldo));
		}
		return totalSaldo;
	}


	private void preloadSaldoDepositCache(List<Deposit> deposits) {
		saldoDepositCache = new HashMap<Long, Double>();
		if (deposits == null || deposits.isEmpty()) {
			return;
		}
		List<Long> mahasiswaIds = new ArrayList<Long>();
		List<Long> calonMahasiswaIds = new ArrayList<Long>();
		List<Long> siswaIds = new ArrayList<Long>();
		List<Long> calonSiswaIds = new ArrayList<Long>();
		List<Long> anggotaIds = new ArrayList<Long>();
		Map<Long, String> depositPersonKey = new HashMap<Long, String>();
		for (Deposit dep : deposits) {
			String key = personKey(dep);
			if (dep != null && dep.getId() != null && key != null) {
				depositPersonKey.put(dep.getId(), key);
			}
			addPersonId(dep, mahasiswaIds, calonMahasiswaIds, siswaIds, calonSiswaIds, anggotaIds);
		}
		if (mahasiswaIds.isEmpty() && calonMahasiswaIds.isEmpty() && siswaIds.isEmpty() && calonSiswaIds.isEmpty()
				&& anggotaIds.isEmpty()) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.currentSession();
			StringBuilder sql = new StringBuilder();
			sql.append("select (case when a.mahasiswa is not null then 'MHS_'||a.mahasiswa ");
			sql.append("when a.biodata_calon_mahasiswa is not null then 'BCM_'||a.biodata_calon_mahasiswa ");
			sql.append("when a.siswa is not null then 'SSW_'||a.siswa ");
			sql.append("when a.calon_siswa is not null then 'CSW_'||a.calon_siswa ");
			sql.append("when a.anggota_koperasi is not null then 'AKP_'||a.anggota_koperasi else 'OTH_' end) as person_key, ");
			sql.append("sum(a.deposit) as saldo from ( ");
			sql.append("select a.mahasiswa, a.biodata_calon_mahasiswa, a.siswa, a.calon_siswa, a.anggota_koperasi, a.nominal as deposit from deposit a ");
			sql.append("union all ");
			sql.append("select b.mahasiswa, b.calon_mahasiswa as biodata_calon_mahasiswa, null as siswa, null as calon_siswa, null as anggota_koperasi, -(a.deposit) as deposit from cicilan_pembayaran a inner join kegiatan b on (a.kegiatan=b.id) where a.deposit > 0.1 ");
			sql.append("union all ");
			sql.append("select a.mahasiswa, a.calon_mahasiswa as biodata_calon_mahasiswa, a.siswa, a.calon_siswa, null as anggota_koperasi, -(a.nominal) as deposit from pengeluaran_mahasiswa a ");
			sql.append("union all ");
			sql.append("select null as mahasiswa, null as biodata_calon_mahasiswa, a.siswa_id as siswa, a.calon_siswa_id as calon_siswa, null as anggota_koperasi, -(a.daritabungan) as deposit from sekolah.pembayaran_siswa a where a.daritabungan > 0.1 ");
			sql.append(") a where 1=0 ");
			if (!mahasiswaIds.isEmpty()) {
				sql.append("or a.mahasiswa in (:mahasiswaIds) ");
			}
			if (!calonMahasiswaIds.isEmpty()) {
				sql.append("or a.biodata_calon_mahasiswa in (:calonMahasiswaIds) ");
			}
			if (!siswaIds.isEmpty()) {
				sql.append("or a.siswa in (:siswaIds) ");
			}
			if (!calonSiswaIds.isEmpty()) {
				sql.append("or a.calon_siswa in (:calonSiswaIds) ");
			}
			if (!anggotaIds.isEmpty()) {
				sql.append("or a.anggota_koperasi in (:anggotaIds) ");
			}
			sql.append("group by person_key");
			SQLQuery query = session.createSQLQuery(sql.toString());
			if (!mahasiswaIds.isEmpty()) {
				query.setParameterList("mahasiswaIds", mahasiswaIds);
			}
			if (!calonMahasiswaIds.isEmpty()) {
				query.setParameterList("calonMahasiswaIds", calonMahasiswaIds);
			}
			if (!siswaIds.isEmpty()) {
				query.setParameterList("siswaIds", siswaIds);
			}
			if (!calonSiswaIds.isEmpty()) {
				query.setParameterList("calonSiswaIds", calonSiswaIds);
			}
			if (!anggotaIds.isEmpty()) {
				query.setParameterList("anggotaIds", anggotaIds);
			}
			Map<String, Double> saldoByPerson = new HashMap<String, Double>();
			List rows = query.list();
			for (Object rowObj : rows) {
				Object[] row = (Object[]) rowObj;
				if (row != null && row.length > 1 && row[0] != null) {
					saldoByPerson.put(String.valueOf(row[0]),
							Double.valueOf(row[1] == null ? 0.0 : ((Number) row[1]).doubleValue()));
				}
			}
			for (Deposit dep : deposits) {
				if (dep != null && dep.getId() != null) {
					String key = depositPersonKey.get(dep.getId());
					Double saldo = key == null ? null : saldoByPerson.get(key);
					saldoDepositCache.put(dep.getId(), saldo == null ? Double.valueOf(0.0) : saldo);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			saldoDepositCache = new HashMap<Long, Double>();
		}
	}

	private void addPersonId(Deposit dep, List<Long> mahasiswaIds, List<Long> calonMahasiswaIds, List<Long> siswaIds,
			List<Long> calonSiswaIds, List<Long> anggotaIds) {
		if (dep == null) {
			return;
		}
		if (dep.getMahasiswa() != null && dep.getMahasiswa().getId() != null) {
			addUniqueLong(mahasiswaIds, dep.getMahasiswa().getId());
		} else if (dep.getBiodataCalonMahasiswa() != null && dep.getBiodataCalonMahasiswa().getId() != null) {
			addUniqueLong(calonMahasiswaIds, dep.getBiodataCalonMahasiswa().getId());
		} else if (dep.getSiswa() != null && dep.getSiswa().getId() != null) {
			addUniqueLong(siswaIds, dep.getSiswa().getId());
		} else if (dep.getCalonSiswa() != null && dep.getCalonSiswa().getId() != null) {
			addUniqueLong(calonSiswaIds, dep.getCalonSiswa().getId());
		} else if (dep.getAnggotaKoperasi() != null && dep.getAnggotaKoperasi().getId() != null) {
			addUniqueLong(anggotaIds, dep.getAnggotaKoperasi().getId());
		}
	}

	private void addUniqueLong(List<Long> ids, Long id) {
		if (ids != null && id != null && !ids.contains(id)) {
			ids.add(id);
		}
	}

	private String personKey(Deposit dep) {
		if (dep == null) {
			return null;
		}
		if (dep.getMahasiswa() != null && dep.getMahasiswa().getId() != null) {
			return "MHS_" + dep.getMahasiswa().getId();
		}
		if (dep.getBiodataCalonMahasiswa() != null && dep.getBiodataCalonMahasiswa().getId() != null) {
			return "BCM_" + dep.getBiodataCalonMahasiswa().getId();
		}
		if (dep.getSiswa() != null && dep.getSiswa().getId() != null) {
			return "SSW_" + dep.getSiswa().getId();
		}
		if (dep.getCalonSiswa() != null && dep.getCalonSiswa().getId() != null) {
			return "CSW_" + dep.getCalonSiswa().getId();
		}
		if (dep.getAnggotaKoperasi() != null && dep.getAnggotaKoperasi().getId() != null) {
			return "AKP_" + dep.getAnggotaKoperasi().getId();
		}
		return null;
	}

	class DepositRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Deposit deposit = (Deposit) arg1;
			if (deposit.getAnggotaKoperasi() != null) {
				CommonMedia.tampilkanGambarKecil(deposit.getAnggotaKoperasi()).setParent(arg0);
				RevisiHelper
						.createNewRevisi(Deposit.class, deposit,
								deposit.getAnggotaKoperasi().getKode() + "-" + deposit.getAnggotaKoperasi().getNama())
						.setParent(arg0);
			} else if (deposit.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(deposit.getSiswa()).setParent(arg0);
				RevisiHelper
						.createNewRevisi(Deposit.class, deposit,
								deposit.getSiswa().getNomorInduk() + "-" + deposit.getSiswa().getNama())
						.setParent(arg0);
			} else if (deposit.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(deposit.getCalonSiswa()).setParent(arg0);
				RevisiHelper
						.createNewRevisi(Deposit.class, deposit,
								deposit.getCalonSiswa().getNoRegistrasi() + "-" + deposit.getCalonSiswa().getNama())
						.setParent(arg0);
			} else if (deposit.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(deposit.getMahasiswa()).setParent(arg0);
				RevisiHelper
						.createNewRevisi(Deposit.class, deposit,
								deposit.getMahasiswa().getNim() + "-" + deposit.getMahasiswa().getNama())
						.setParent(arg0);
			} else if (deposit.getBiodataCalonMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(deposit.getBiodataCalonMahasiswa()).setParent(arg0);
				RevisiHelper
						.createNewRevisi(Deposit.class, deposit, deposit.getBiodataCalonMahasiswa().getNoRegistrasi()
								+ "-" + deposit.getBiodataCalonMahasiswa().getNama())
						.setParent(arg0);
			}

			// 1. Label Kolom Nominal Deposit
			new Label(Common.numberFormat.get().format(deposit.getNominal())).setParent(arg0);

			// ---> 2. TAMBAHKAN KOLOM SALDO DI SINI <---
			double saldo = hitungSaldoPerOrang(deposit);
			Label lblSaldo = new Label(Common.numberFormat.get().format(saldo));
			lblSaldo.setStyle("font-weight: bold; color: #004085;"); // Opsional: Diberi warna & bold agar mudah
																		// dibedakan
			lblSaldo.setParent(arg0);
			// ------------------------------------------

			// 3. Kolom Lainnya
			new Label(Common.dateFormat.get().format(deposit.getWaktu())).setParent(arg0);
			new Label(deposit.getJenisPembayaran() == null ? "" : deposit.getJenisPembayaran().getNama())
					.setParent(arg0);
			new Label(deposit.getJenisTabungan() == null ? "" : deposit.getJenisTabungan().getNama()).setParent(arg0);
			new Label(deposit.getKeterangan()).setParent(arg0);

			// 4. Tombol Aksi
			if (tbmuser != null && !isLoginPenabungTerbatas()) {
				Common.copyEditDeleteButtons(edit, delete, deposit, DepositAction.this).setParent(arg0);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Deposit());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		deposit = (Deposit) obj;
		init(deposit);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Deposit deposit) throws Exception {
		this.deposit = deposit;
		addWindow.setTitle(deposit.getId() == null ? "Tambah Tabungan" : "Ubah Tabungan");
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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		MyFormRow row = new MyFormRow();
		row.setVisible(pt);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setValue(deposit.getMahasiswa() == null ? "" : deposit.getMahasiswa().getNama());
		mahasiswa.setAttribute("mahasiswa", deposit.getMahasiswa());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Mahasiswa *"));
		row.appendChild(calonMahasiswa = new AmbilDataCalonMahasiswaBanbox());
		calonMahasiswa.setValue(
				deposit.getBiodataCalonMahasiswa() == null ? "" : deposit.getBiodataCalonMahasiswa().getNama());
		calonMahasiswa.setAttribute("calonMahasiswa", deposit.getBiodataCalonMahasiswa());
		calonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setValue(deposit.getSiswa() == null ? "" : deposit.getSiswa().getNama());
		siswa.setAttribute("siswa", deposit.getSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Siswa *"));
		row.appendChild(calonSiswa = new AmbilDataCalonSiswaBanbox());
		calonSiswa.setValue(deposit.getCalonSiswa() == null ? "" : deposit.getCalonSiswa().getNama());
		calonSiswa.setAttribute("calonSiswa", deposit.getCalonSiswa());
		calonSiswa.setWidth("90%");

		final boolean memberTampilDiDeposit = Common.bolehKonfigurasi("member_tampil_di_deposit");
		row = new MyFormRow();
		row.setVisible(memberTampilDiDeposit);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota / Member"));
		row.appendChild(anggotaKoperasi = new AmbilDataAnggotaKoperasiBanbox());
		anggotaKoperasi.setValue(deposit.getAnggotaKoperasi() == null ? "" : deposit.getAnggotaKoperasi().getNama());
		anggotaKoperasi.setAttribute("anggotaKoperasi", deposit.getAnggotaKoperasi());
		anggotaKoperasi.setWidth("90%");

		EventListener a = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				mahasiswa.getParent().setVisible(false);
				calonMahasiswa.getParent().setVisible(false);

				siswa.getParent().setVisible(false);
				calonSiswa.getParent().setVisible(false);
				anggotaKoperasi.getParent()
						.setVisible(anggotaKoperasi.getAttribute("anggotaKoperasi") != null && memberTampilDiDeposit);

				if (calonMahasiswa.getAttribute("calonMahasiswa") != null) {
					calonMahasiswa.getParent().setVisible(true);
				} else if (mahasiswa.getAttribute("mahasiswa") != null) {
					mahasiswa.getParent().setVisible(true);
				} else if (siswa.getAttribute("siswa") != null) {
					siswa.getParent().setVisible(true);
				} else if (calonSiswa.getAttribute("calonSiswa") != null) {
					calonSiswa.getParent().setVisible(true);
				} else {
					mahasiswa.getParent().setVisible(pt);
					calonMahasiswa.getParent().setVisible(pt);

					siswa.getParent().setVisible(ya);
					calonSiswa.getParent().setVisible(ya);
				}

				anggotaKoperasi.getParent()
						.setVisible(!(calonMahasiswa.getAttribute("calonMahasiswa") != null
								|| mahasiswa.getAttribute("mahasiswa") != null
								|| siswa.getAttribute("siswa") != null
								|| calonSiswa.getAttribute("calonSiswa") != null) && memberTampilDiDeposit);

			}
		};

		mahasiswa.setEventListener(a);
		calonMahasiswa.setEventListener(a);
		siswa.setEventListener(a);
		calonSiswa.setEventListener(a);
		anggotaKoperasi.setEventListener(a);
		a.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nominal *"));
		row.appendChild(nominal = new MyDoublebox(deposit.getNominal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu *"));
		row.appendChild(waktu = new MyDatebox(deposit.getWaktu()));
		waktu.setFormat(Common.dateFormat.get().toPattern());
		waktu.setReadonly(true);
		waktu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran *"));

		if (deposit != null && deposit.getJenisPembayaran() == null) {
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			deposit.setJenisPembayaran(jenisPembayaranDefault);
		}

		Common.insertCombo(jenisPembayaran = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.and(Restrictions.isNull("jenisTabungan"),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		row.appendChild(jenisPembayaran);
		Common.selectComboItem(jenisPembayaran, deposit.getJenisPembayaran());
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Tabungan *"));

		Common.insertCombo(jenisTabungan = new Combobox(), "nama", "akun", JenisTabungan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(jenisTabungan);
		Common.selectComboItem(jenisTabungan, deposit.getJenisTabungan());
		jenisTabungan.setWidth("90%");
		jenisTabungan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(deposit.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && !isLoginPenabungTerbatas()
				&& tbmuser.getBiodataCalonMahasiswa() == null);
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

	private Object getAttributeQuietly(Component component, String name) {
		try {
			return component == null ? null : component.getAttribute(name);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean onSave(Event event) throws Exception {
		Object attrMahasiswa = getAttributeQuietly(mahasiswa, "mahasiswa");
		Object attrCalonMahasiswa = getAttributeQuietly(calonMahasiswa, "calonMahasiswa");
		Object attrAnggotaKoperasi = getAttributeQuietly(anggotaKoperasi, "anggotaKoperasi");
		Object attrSiswa = getAttributeQuietly(siswa, "siswa");
		Object attrCalonSiswa = getAttributeQuietly(calonSiswa, "calonSiswa");
		if (attrMahasiswa == null && attrCalonMahasiswa == null && attrAnggotaKoperasi == null && attrSiswa == null
				&& attrCalonSiswa == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Penambung",
					"Kolom Penambung belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Penambung.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nominal.getValue() == null || Math.abs(nominal.getValue().doubleValue()) < 0.1) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nominal",
					"Kolom Nominal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nominal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisPembayaran == null || jenisPembayaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cara pembayaran",
					"Kolom Cara pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Cara pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisTabungan == null || jenisTabungan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis tabungan",
					"Kolom Jenis tabungan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis tabungan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (deposit.getId() != null) {
			deposit = (Deposit) session.load(Deposit.class, deposit.getId());
		}

		deposit.setAnggotaKoperasi((AnggotaKoperasi) attrAnggotaKoperasi);
		deposit.setBiodataCalonMahasiswa((BiodataCalonMahasiswa) attrCalonMahasiswa);
		deposit.setMahasiswa((Mahasiswa) attrMahasiswa);
		deposit.setSiswa((Siswa) attrSiswa);
		deposit.setCalonSiswa((CalonSiswa) attrCalonSiswa);
		deposit.setJenisPembayaran((JenisPembayaran) jenisPembayaran.getSelectedItem().getValue());
		deposit.setJenisTabungan((JenisTabungan) jenisTabungan.getSelectedItem().getValue());
		deposit.setNominal(nominal.getValue());
		deposit.setWaktu(waktu == null || waktu.getValue() == null ? new Date() : waktu.getValue());
		deposit.setKeterangan(keterangan == null ? null : keterangan.getValue());

		Common.refreshSaveOrUpdate(session, deposit);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Sekolah sekolah = (Sekolah) (searchsekolah == null || searchsekolah.getSelectedItem() == null ? null
				: searchsekolah.getSelectedItem().getValue());
		Yayasan yayasan = (Yayasan) (searchyayasan == null || searchyayasan.getSelectedItem() == null ? null
				: searchyayasan.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (searchjurusan == null || searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas == null || searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());

		if (fakultas != null || jurusan != null || !ya) {
			sekolah = null;
			yayasan = null;
		}

		Session session = HibernateUtil.currentSession();

		List<Long> longsJurusans = fakultas != null && fakultas.getId() != null
				? session.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", true))
						.setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", fakultas)).list()
				: null;

		Criteria criteria = session.createCriteria(Deposit.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("anggotaKoperasi", "anggotaKoperasi", Criteria.LEFT_JOIN)
				.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("siswa.sekolah", sekolah),
								Restrictions.eq("calonSiswa.sekolah", sekolah)))

				.add(jurusan == null || jurusan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.or(Restrictions.eq("biodataCalonMahasiswa.prodi1", jurusan),
										Restrictions.eq("biodataCalonMahasiswa.prodiLulus", jurusan))))

				.add(fakultas != null && fakultas.getId() != null && longsJurusans != null && longsJurusans.isEmpty()
						? Restrictions.sqlRestriction("false")
						: longsJurusans == null || longsJurusans.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.in("mahasiswa.jurusan.id", longsJurusans),
										Restrictions.or(
												Restrictions.in("biodataCalonMahasiswa.prodi1.id", longsJurusans),
												Restrictions.in("biodataCalonMahasiswa.prodiLulus.id", longsJurusans))))

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("siswa.yayasan"),
								Restrictions.or(Restrictions.eq("siswa.yayasan", yayasan),
										Restrictions.eq("calonSiswa.yayasan", yayasan))));

		if (getLoginMahasiswaId() != null) {
			criteria.add(Restrictions.eq("mahasiswa.id", getLoginMahasiswaId()));
		} else if (getLoginSiswaId() != null) {
			criteria.add(Restrictions.eq("siswa.id", getLoginSiswaId()));
		}

		String keywordPenabung = searchnamaPenabung == null || searchnamaPenabung.getValue() == null ? ""
				: searchnamaPenabung.getValue().trim();
		if (!isLoginPenabungTerbatas() && keywordPenabung.length() > 0) {
			criteria.add(Restrictions.or(Restrictions.ilike("anggotaKoperasi.kode", keywordPenabung, MatchMode.ANYWHERE),
					Restrictions.or(Restrictions.ilike("anggotaKoperasi.nama", keywordPenabung, MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("calonSiswa.namaSiswa", keywordPenabung, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.ilike("siswa.namaSiswa", keywordPenabung, MatchMode.ANYWHERE),
											Restrictions.or(Restrictions.ilike("mahasiswa.nama", keywordPenabung, MatchMode.ANYWHERE),
													Restrictions.ilike("biodataCalonMahasiswa.nama", keywordPenabung, MatchMode.ANYWHERE)))))));
		}

		if (order)
			criteria.addOrder(Order.desc("waktu"));
		String keywordKeterangan = searchnama == null || searchnama.getValue() == null ? "" : searchnama.getValue().trim();
		criteria.add(keywordKeterangan.length() == 0 ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", keywordKeterangan, MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		try {
			Criteria countCriteria = initCriteria(false);
			if (paging != null) {
				Common.initPaging(countCriteria, paging);
			}

			List<Deposit> deposits = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			preloadSaldoDepositCache(deposits);

			ListModel strset = new SimpleListModel(deposits);
			if (grid != null) {
				grid.setRowRenderer(new DepositRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

}
