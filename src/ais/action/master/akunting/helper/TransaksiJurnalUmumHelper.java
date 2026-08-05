package ais.action.master.akunting.helper;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.akunting.JenisUangMukaAction;
import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.format1.akunting.LaporanBuktiPengeluaranKas;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransaksiJurnalUmumHelper extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400318L;
	private MyGrid grid;

	private static long ids = 100000L;

	private boolean edit = false;
	private boolean delete = false;

	private String parentCode;

	private Textbox noref;
	private MyDatebox tanggal;
	// private AmbilDataPegawaiBanbox pegawaiBanbox;

	private GrupTransaksi grupTransaksi;

	private Label labelTotalDebet = new Label("0");
	private Label labelTotalKredit = new Label("0");
	private Label labelTotalSelisih = new Label("0");

	private EventListener eventListener;
	private MyToolbarbuttonConfig save;

	private List<Transaksi> newTransaksis = new ArrayList<Transaksi>();
	private MyToolbarbuttonConfig buttonadd;
	private Double totalKredit;
	private Double totalDebet;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataWorkspaceBanbox workspace;
	protected LampiranLain buktiTransaksi;

	private Combobox jenisTransaksi;
	private Textbox kepada;
	private Textbox nomorTagihan;
	private Tbmuser tbmuser;
	private MyLabelBoldAja norefJurnal;
	private MyToolbarbuttonConfig penutup;
	private boolean jurnalKasKecil = false;
	private java.util.Map<Long, Object[]> petaRincian = new java.util.HashMap<Long, Object[]>();

	public TransaksiJurnalUmumHelper(GrupTransaksi grupTransaksi) throws Exception {
		super();
		this.grupTransaksi = grupTransaksi;
		initWindow();
	}

	public TransaksiJurnalUmumHelper(GrupTransaksi grupTransaksi, String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		this.grupTransaksi = grupTransaksi;
		initWindow();
	}

	public static String generateCode(String prefix, int tambahan) {
		Calendar tanggal = ais.ui.util.WaktuUtil.getCalendar();
		return generateCode(prefix, tanggal, tambahan);
	}

	public static String generateCode(String prefix, Calendar tanggal, int tambahan) {
		String code = "";
		if (prefix != null && !prefix.trim().isEmpty()) {
			Session session = HibernateUtil.currentSession();
			String bul = "000" + (tanggal.get(Calendar.MONTH) + 1);

			boolean thn = Common.bolehKonfigurasi("tambah_tahun_di_penomoran_jurnal_umum", Konfigurasi.TIDAK_AKTIF);
			boolean looping = Common.bolehKonfigurasi("looping_menggunakan_tahun_pada_jurnal_umum", Konfigurasi.TIDAK_AKTIF);

			String thnData = (tanggal.get(Calendar.YEAR) + "").substring(2);

			if (thn) {
				bul = thnData + "/" + bul.substring(bul.length() - 2);
			} else {
				bul = bul.substring(bul.length() - 2);
			}

			String prex = prefix + "/" + (looping ? thnData : bul) + "/";
			int count = ((Number) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.ilike("kode", prex, MatchMode.START)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			count = count + tambahan;

			String c = "0000000" + count;
			c = c.substring(c.length() - 5);

			code = prex + c;

			int ada = ((Number) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("kode", code))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (ada > 0) {
				return generateCode(prefix, tanggal, ++tambahan);
			}
		}
		return code;
	}

	public void initWindow() throws Exception {
		parentCode = grupTransaksi.getParentCode();
		if (parentCode == null) {

			Long milis = ais.ui.util.WaktuUtil.getDate().getTime() + (++ids);
			parentCode = "PARENT-" + Long.toHexString(milis).toUpperCase();
		}

		tbmuser = Common.getCurrentUser();
		// Kolom "Keterangan Biaya" hanya untuk jurnal Kas Kecil (punya rincian formula).
		jurnalKasKecil = grupTransaksi != null
			&& (grupTransaksi.getKasKecil() != null || grupTransaksi.getPenggantianKasKecil() != null);

		setWidth("95%");
		setHeight("95%");
		setClosable(false);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(this);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setTitle("Akun Transaksi Jurnal Umum");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setTitle("Jurnal Umum");
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(west);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Columns columns = new Columns();

		columns.setParent(searchgrid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal *"));
		row.appendChild(
				tanggal = new MyDatebox(grupTransaksi.getTanggalTransaksi() == null ? ais.ui.util.WaktuUtil.getDate()
						: grupTransaksi.getTanggalTransaksi()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());
		tanggal.setWidth("90%");

		norefJurnal = new MyLabelBoldAja();
		noref = new Textbox(grupTransaksi.getKode() == null
				? CommonAkunting.generateNoJurnal(grupTransaksi.getJenisTransaksi(), false)
				: grupTransaksi.getKode());
		norefJurnal.setValue(noref.getValue());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Jurnal *"));
		if (Common.bolehKonfigurasi("nomor_jurnal_tidak_boleh_diubah")) {
			row.appendChild(norefJurnal);
		} else {
			row.appendChild(noref);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Transaksi *"));
		row.appendChild(jenisTransaksi = new Combobox());
		Common.insertComboDanSemua(jenisTransaksi, new String[] { "nama" }, "keterangan", JenisTransaksi.class,
				"Tidak Ditentukan", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		jenisTransaksi.setWidth("90%");
		Common.selectComboItem(jenisTransaksi,
				grupTransaksi.getJenisTransaksi() == null && grupTransaksi.getId() == null
						? HibernateUtil.currentSession().createCriteria(JenisTransaksi.class)
								.add(Restrictions.eq("defaultItem", true)).setMaxResults(1).uniqueResult()
						: grupTransaksi.getJenisTransaksi());
		row.setParent(rows);
		jenisTransaksi.setReadonly(true);

		jenisTransaksi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisTransaksi jt = (JenisTransaksi) (jenisTransaksi.getSelectedItem() == null ? null
						: jenisTransaksi.getSelectedItem().getValue());
				if (jt != null) {
					if (penutup != null) {
						penutup.setVisible(jt.getNama() != null && jt.getNama().toLowerCase().contains("utup"));
					}
					grupTransaksi.setJenisTransaksi(jt);
					if (jt.getNomorSurat() != null) {
						noref.setValue(CommonAkunting.generateNoJurnal(grupTransaksi.getJenisTransaksi(), false));
					} else {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(tanggal.getValue());
						noref.setValue(generateCode(jt.getKode(), calendar, 1));
					}
				}

				norefJurnal.setValue(noref.getValue());
			}
		});

		JenisTransaksi jt = (JenisTransaksi) (jenisTransaksi.getSelectedItem() == null ? null
				: jenisTransaksi.getSelectedItem().getValue());
		if (jt != null && penutup != null) {
			penutup.setVisible(jt.getNama() != null && jt.getNama().toLowerCase().contains("utup"));
		}

		tanggal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisTransaksi jt = (JenisTransaksi) (jenisTransaksi.getSelectedItem() == null ? null
						: jenisTransaksi.getSelectedItem().getValue());
				if (jt != null) {
					grupTransaksi.setJenisTransaksi(jt);
					if (jt.getNomorSurat() != null) {
						noref.setValue(CommonAkunting.generateNoJurnal(grupTransaksi.getJenisTransaksi(), false));
					} else {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(tanggal.getValue());
						noref.setValue(generateCode(jt.getKode(), calendar, 1));
					}
				}

				norefJurnal.setValue(noref.getValue());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kepada/Dari"));
		row.appendChild(kepada = new Textbox(grupTransaksi.getKepada()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cash/Cek/Bilyet Giro"));
		row.appendChild(nomorTagihan = new Textbox(grupTransaksi.getNomorTagihan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(grupTransaksi.getSatuanKerja() == null
				? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
						: Common.getCurrentUser().ambilSatuanKerja().toString())
				: grupTransaksi.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				grupTransaksi.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: grupTransaksi.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggaran"));
		row.appendChild(workspace = new AmbilDataWorkspaceBanbox(false));
		workspace.setValue(grupTransaksi.getWorkspace() == null ? null : grupTransaksi.getWorkspace().toString());
		workspace.setAttribute("workspace", grupTransaksi.getWorkspace());
		workspace.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, grupTransaksi.getId(), "Bukti Transaksi",
				"Bukti Transaksi (JPG)", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						buktiTransaksi = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		Borderlayout myBorderlayout = new ais.ui.util.MyBorderlayout();
		myBorderlayout.setParent(center);

		North north = new North();
		north.setParent(myBorderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		buttonadd = new MyToolbarbuttonConfig("Tambah Data Transaksi", "/img/new.gif");
		buttonadd.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}
		});
		toolbar.appendChild(buttonadd);

		String[] contents = new String[] { "id", "tanggalTransaksi", "tanggalDimasukkan", "akun", "debet", "kredit",
				"keterangan", "jenisTransaksi" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();

				Criteria criteria = session.createCriteria(Transaksi.class)
						.add(Restrictions.eq("parentCode", parentCode));

				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {
				TransaksiJurnalUmumHelper.this.onSearchDefault(null);
			}

		}, Transaksi.class, new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				Transaksi transaksi = (Transaksi) data[0];
				Session session = (Session) data[1];
				List apakahSimpan = (List) data[3];
				if (transaksi.getAkun() == null || transaksi.getTanggalTransaksi() == null) {
					apakahSimpan.add(false);
					return;
				}

				GrupTransaksi grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
						.add(Restrictions.eq("kode", parentCode)).setMaxResults(1).uniqueResult();
				if (grupTransaksi == null) {
					grupTransaksi = new GrupTransaksi();
					grupTransaksi.setSatuanKerja(tbmuser.ambilSatuanKerja());
					grupTransaksi.setTotalKredit(0.0);
					grupTransaksi.setTotalDebet(0.0);
					grupTransaksi.setTbmuser(tbmuser);
					grupTransaksi.setTanggalTransaksi(transaksi.getTanggalTransaksi());
					grupTransaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
					grupTransaksi.setPegawai(tbmuser.ambilPegawai());
					grupTransaksi.setParentCode(parentCode);
					grupTransaksi.setKode(parentCode);
					grupTransaksi.setKeterangan(transaksi.getKeterangan());
					grupTransaksi.setJenisTransaksi(transaksi.getJenisTransaksi());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, grupTransaksi);
					session.getTransaction().commit();
				} else {
					grupTransaksi.setJenisTransaksi(transaksi.getJenisTransaksi());
					grupTransaksi.setTanggalTransaksi(transaksi.getTanggalTransaksi());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, grupTransaksi);
					session.getTransaction().commit();
				}
				transaksi.setGrupTransaksi(grupTransaksi);
				transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
				transaksi.setSimpan(true);
				transaksi.setParentCode(parentCode);
				transaksi.setKode(parentCode);
				transaksi.setMerupakanDebet(transaksi.getDebet() > 0.1);
			}
		}, contents);
		upload.setVisible(edit && delete);
		toolbar.appendChild(upload);

		penutup = new MyToolbarbuttonConfig("Tambah Data Jurnal Penutup", "/img/new.gif");
		penutup.setVisible(false);
		penutup.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Calendar calendar = Calendar.getInstance();
						calendar.setTime(tanggal.getValue());
						int tahun = calendar.get(Calendar.YEAR);

						String sql = "select d.id as id_akun, d.kode as kode_akun, d.nama as akun, "
								+ "(sum(case when date(a1.tanggal_transaksi) between date('" + tahun
								+ "-01-01') and date('" + tahun
								+ "-12-31') then (debet) else 0 end)) as saldo_awal_debet, "
								+ "(sum(case when date(a1.tanggal_transaksi) between date('" + tahun
								+ "-01-01') and date('" + tahun
								+ "-12-31') then (kredit) else 0 end)) as saldo_awal_kredit "
								+ " from akunting.transaksi a "
								+ "inner join akunting.grup_transaksi a1 on (a1.id=a.grup_transaksi) ";

						if (!ConstantValues.otomatisTerposting) {
							sql += "  inner join akunting.posting_history dd on (dd.id=a1.posting_history and dd.posting=true) ";
						}

						sql += " inner join akunting.akun d on (a.akun = d.id) where a1.posting_history is not null "
								+ " and case when :satuan_kerja = -1 then true else :satuan_kerja = a1.satuan_kerja end "
								+ " and date(a1.tanggal_transaksi) between date('" + tahun + "-01-01') and date('"
								+ tahun + "-12-31') "
								+ " and a1.closing is not null and d.kode not ilike '1%' and d.kode not ilike '2%' and d.kode not ilike '3%' group by d.id order by d.kode";

						System.out.println(sql);

						SatuanKerja a = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
						Long satker = a == null || a.getId() == null ? -1L : a.getId();

						Session session = HibernateUtil.currentNativeSession();

						List<Object[]> objects = session.createSQLQuery(sql).setLong("satuan_kerja", satker).list();
						session.createSQLQuery(
								"delete from akunting.transaksi where parent_code = '" + parentCode + "'")
								.executeUpdate();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						newTransaksis.clear();

						Double totalD = 0.0;
						Double totalK = 0.0;

						for (Object[] o : objects) {

							Number id_akun = o[0] == null ? 0.0 : (Number) o[0];

							Double saldo_awal_debet = o[3] == null ? 0.0 : ((Number) o[3]).doubleValue();
							Double saldo_awal_kredit = o[4] == null ? 0.0 : ((Number) o[4]).doubleValue();

							Double saldoAwalDebet = ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
									- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit)) > 0.1
											? ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
													- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit))
											: 0.0;

							Double saldoAwalKredit = ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
									- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit)) < 0.0
											? ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
													- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit))
											: 0.0;

							Transaksi transaksi = new Transaksi();
							transaksi.setMerupakanDebet(saldoAwalDebet.intValue() == 0);
							transaksi.setAkun(new Akun(id_akun.longValue()));
							transaksi.setParentCode(parentCode);
							transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
							transaksi.setDebet(Math.abs(saldoAwalKredit));
							transaksi.setKredit(Math.abs(saldoAwalDebet));

							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.save(transaksi);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();
							newTransaksis.add(transaksi);

							totalD += transaksi.getDebet();
							totalK += transaksi.getKredit();
						}

						Double selisih = totalD - totalK;

						Long kodeAkunPenutup = null;
						try {
							String id_kode_akun_penutup = Common.getKonfigurasi("id_kode_akun_penutup", "").getNilai()
									.trim();
							kodeAkunPenutup = id_kode_akun_penutup.isEmpty() ? null
									: Long.parseLong(id_kode_akun_penutup);

							if (kodeAkunPenutup != null) {
								Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), kodeAkunPenutup);
								if (akun != null) {

									Transaksi transaksi = new Transaksi();
									transaksi.setMerupakanDebet(totalD <= totalK);
									transaksi.setAkun(akun);
									transaksi.setParentCode(parentCode);
									transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
									transaksi.setDebet(totalD <= totalK ? Math.abs(selisih) : 0.0);
									transaksi.setKredit(totalD > totalK ? Math.abs(selisih) : 0.0);

									session = HibernateUtil.currentNativeSession();
									session.getTransaction().begin();
									session.save(transaksi);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();
									newTransaksis.add(transaksi);

								}
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/TransaksiJurnalUmumHelper.java:596");
						}

						onSearchDefault(null);
					}
				});

			}
		});
		toolbar.appendChild(penutup);

		Center mycenter = new Center();
		mycenter.setParent(myBorderlayout);

		// Grid LANGSUNG jadi anak Center (pola baku Center->Grid->Rows->Row, dibungkus
		// method reusable Common.jadikanCenterScrollable — JANGAN pakai Div pembungkus,
		// sering tidak memunculkan scrollbar sama sekali di ZK yang dipakai).
		Common.jadikanCenterScrollable(mycenter);
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(mycenter);

		columns = new Columns();

		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Akun");
		column.setWidth("150px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Akun");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("25%");

		if (jurnalKasKecil) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan Biaya");
			column.setWidth("20%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai (Debet)");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai (Kredit)");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("80px");

		South south = new South();
		south.setParent(borderlayout);
		//
		// div = new Div();
		// div.setParent(south);

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		// foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Transaksi"));
		if (jurnalKasKecil) {
			foot.appendChild(new Footer());
		}

		Footer footer = new Footer();
		footer.setParent(foot);
		footer.setAlign("right");
		labelTotalDebet.setParent(footer);
		labelTotalDebet.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		footer = new Footer();
		footer.setParent(foot);
		footer.setAlign("right");
		labelTotalKredit.setParent(footer);
		labelTotalKredit.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		footer = new Footer();
		footer.setParent(foot);
		footer.setAlign("right");
		labelTotalSelisih.setParent(footer);
		labelTotalSelisih.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		final MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();
				session.createSQLQuery(
						"delete from akunting.transaksi where simpan = false and parent_code = '" + parentCode + "'")
						.executeUpdate();
				List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
						.add(Restrictions.eq("simpan", true)).add(Restrictions.eq("parentCode", parentCode)).list();

				if (transaksis.size() == 0 && grupTransaksi.getId() != null) {
					Common.refreshDelete((grupTransaksi));
				}

				if (eventListener != null) {
					eventListener.onEvent(event);
				}

				TransaksiJurnalUmumHelper.this.detach();
			}
		});
		batal.setParent(toolbar);

		save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");
		save.setVisible(edit);
//		if (countAll != 0 && countPosting.equals(countAll)) {
//			save.setVisible(false);
//		}
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentNativeSession();
						Date maxClosing = (Date) session.createCriteria(Closing.class)
								.setProjection(Projections.max("tanggal")).uniqueResult();
						if (maxClosing != null && tanggal.getValue() != null && tanggal.getValue().before(maxClosing)) {

							MyMessageboxConfig.showFormat(
									"Mohon maaf, tanggal jurnal yang Bapak/Ibu masukkan ({V2}) telah melewati tanggal closing (tutup buku), yaitu {V1}. Data pada periode yang sudah closing tidak dapat diubah. Langkah yang dapat dilakukan: (1) gunakan tanggal jurnal setelah tanggal closing {V1}; (2) apabila memang perlu mengubah periode yang telah closing, mohon menghubungi bagian keuangan atau administrator untuk membuka kembali periode tersebut; (3) periksa kembali tanggal transaksi sebelum menyimpan.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									Common.dateFormat4.get().format(maxClosing),
									Common.dateFormat4.get().format(tanggal.getValue()));
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();
							return;
						}

						List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
								.addOrder(Order.desc("debet")).addOrder(Order.asc("tanggalTransaksi"))
								.add(Restrictions.eq("parentCode", parentCode)).list();

						session.getTransaction().begin();
						for (Transaksi transaksi : transaksis) {
							transaksi.setSimpan(true);
							Common.refreshUpdate(session, transaksi);
						}
						session.getTransaction().commit();

						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}

						HibernateUtil.closeSession();

						onSaveTransaksiUtama(arg0);

						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
						TransaksiJurnalUmumHelper.this.detach();

						LaporanBuktiPengeluaranKas buktiPengeluaranKas = new LaporanBuktiPengeluaranKas(grupTransaksi);
						buktiPengeluaranKas.setTitle("Laporan");
						buktiPengeluaranKas.setClosable(true);
						buktiPengeluaranKas.setHeight("90%");
						buktiPengeluaranKas.setWidth("900px");
						buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						buktiPengeluaranKas.onModal();
					}
				});

			}
		});
		save.setParent(toolbar);

		onSearchDefault(null);

		if (!Common.getApakahAdmin()) {
//			if (countAll != 0 && countPosting.equals(countAll)) {
//				Common.freeze(this, true);
//				batal.setDisabled(false);
//			}

			if (grupTransaksi != null && grupTransaksi.getPostingHistory() != null) {
				Timer timer = new Timer(100);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						MyMessageboxConfig.show(
								"Transaksi ini sudah di posting.. Anda tidak bisa mengubah data transaksi ini !",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						Common.freeze(TransaksiJurnalUmumHelper.this, true);
						batal.setDisabled(false);
					}
				});
				timer.start();
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungTotal();
				Sekolah currentSekolah = SekolahUtil.getSekolah();
				if (currentSekolah != null && currentSekolah.getSatuanKerja() != null) {
					satuanKerja.setValue(currentSekolah.getSatuanKerja().toString());
					satuanKerja.setAttribute("satuanKerja", currentSekolah.getSatuanKerja());
					satuanKerja.setDisabled(true);
				}
			}
		});
	}

	private class TransaksiJurnalPenerimaanEditor {
		private Transaksi transaksi;

		private TransaksiJurnalPenerimaanEditor(Transaksi transaksi) {
			this.transaksi = transaksi;
		}

		public boolean onSave(Event event) throws Exception {
			if (noref.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Mohon maaf, Nomor Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nomor Jurnal dengan nomor yang valid dan unik; (2) Pastikan nomor tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			// if (pegawaiBanbox.getAttribute("pegawai") == null) {
			// MyMessageboxConfig.show("Pegawai harus diisi", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			// return false;
			// }
			if (jenisTransaksi.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf, Jenis Transaksi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Transaksi dari dropdown yang tersedia; (2) Pastikan jenis transaksi yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (tanggal.getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getAkun() == null) {
				MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKeterangan() == null || transaksi.getKeterangan().trim().equals("")) {
				MyMessageboxConfig.show("Mohon maaf, Keterangan Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Keterangan dengan deskripsi transaksi yang jelas; (2) Pastikan keterangan tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getTanggalTransaksi() == null) {
				MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi (detail) belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker pada baris detail; (2) Pastikan tanggal valid dan tidak melebihi tanggal jurnal utama; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getDebet() == null) {
				MyMessageboxConfig.show("Mohon maaf, Nilai Debet belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai Debet dengan nominal yang valid; (2) Salah satu dari Debet atau Kredit harus bernilai nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKredit() == null) {
				MyMessageboxConfig.show("Mohon maaf, Nilai Kredit belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai Kredit dengan nominal yang valid; (2) Salah satu dari Debet atau Kredit harus bernilai nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKredit() > 1.0 && transaksi.getDebet() > 1.0) {
				MyMessageboxConfig.show("Mohon maaf, salah satu nilai Kredit atau Debet harus nol. Langkah yang dapat dilakukan: (1) Kosongkan kolom Debet jika ini transaksi Kredit, atau kosongkan Kredit jika ini transaksi Debet; (2) Pastikan hanya satu sisi yang bernilai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
//			if (transaksi.getKredit() < 1.0 && transaksi.getDebet() < 1.0) {
//				MyMessageboxConfig.show("Salah satu nilai Kredit atau Debet harus bukan nol", "Peringatan",
//						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
//				return false;
//			}

			if (grupTransaksi.getId() == null) {
				onSaveTransaksiUtama(event);
			}

			transaksi.setJumlahTransaksi(transaksi.getKredit() < 1.0 ? transaksi.getKredit() : transaksi.getDebet());

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal.getValue());
			transaksi.setBulan(calendar.get(Calendar.MONTH) + 1);
			transaksi.setTahun(calendar.get(Calendar.YEAR));
			transaksi.setTanggalDimasukkan(tanggal.getValue());
			transaksi.setTanggalTransaksi(tanggal.getValue());

			transaksi.setKode(noref.getValue().trim());
			transaksi.setMerupakanDebet(false);
			transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
			transaksi.setGrupTransaksi(grupTransaksi);

			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.openSession();
				if (!pastikanKolomTransaksiAkuntingAman(session)) {
					transaksi.setKeterangan(potongTextDb(transaksi.getKeterangan(), 250));
				}
				tx = session.beginTransaction();
				if (transaksi.getId() != null) {
					session.update(transaksi);
				} else {
					session.save(transaksi);
				}
				tx.commit();
			} catch (Exception e) {
				try {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/TransaksiJurnalUmumHelper.java:943");
				}
				throw e;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

			hitungTotal();
			return true;
		}

		private boolean pastikanKolomTransaksiAkuntingAman(Session session) {
			Session ddlSession = null;
			Transaction tx = null;
			try {
				ddlSession = HibernateUtil.openSession();
				tx = ddlSession.beginTransaction();
				ddlSession.createSQLQuery("ALTER TABLE akunting.transaksi ALTER COLUMN keterangan TYPE text").executeUpdate();
				tx.commit();
				return true;
			} catch (Exception e) {
				try {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/TransaksiJurnalUmumHelper.java:968");
				}
				return false;
			} finally {
				Common.closeNativeSessionQuietly(ddlSession);
			}
		}

		private String potongTextDb(String value, int max) {
			if (value == null) {
				return null;
			}
			String aman = value.replace('\0', ' ').trim();
			if (max > 0 && aman.length() > max) {
				return aman.substring(0, max);
			}
			return aman;
		}


	}

	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		private void initNotEdit(final Row arg0, final Object arg1) throws Exception {
			Common.clear(arg0);
			final Transaksi transaksi = (Transaksi) arg1;
			arg0.setAttribute("transaksi", transaksi);
			transaksi.setParentCode(parentCode);
//			new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Transaksi.class, transaksi, transaksi.getAkun().getKode()).setParent(arg0);

			Vbox hbox = new Vbox();
			hbox.setParent(arg0);

			new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama()).setParent(hbox);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/pencil-square.svg");
			button.setParent(hbox);
			button.setVisible(edit);
			button.setAttribute("janganDisabled", true);
			button.setTooltiptext("Ubah data pembayaran");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mengubah data pembayaran ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

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
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

											MyColumnConfig column = new MyColumnConfig();
											column.setParent(columns);
											column.setWidth("30%");

											column = new MyColumnConfig();
											column.setParent(columns);

											Rows rows = new Rows();
											rows.setParent(grid);

											final AmbilDataAkunBanbox akunBanbox = new AmbilDataAkunBanbox();
											akunBanbox.setValue(
													transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode());
											akunBanbox.setAttribute("akun", transaksi.getAkun());

											EventListener eventListenerSimpan = new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Session session = HibernateUtil.currentSession();
													session.refresh(transaksi);
													transaksi.setAkunOver((Akun) akunBanbox.getAttribute("akun"));

													Common.refreshUpdate(session, transaksi);
													session.flush();
												}
											};

											MyFormRow row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun *")));

											akunBanbox.setReadonly(true);
											akunBanbox.setWidth("95%");
											row.appendChild(akunBanbox);
											akunBanbox.setEventListener(eventListenerSimpan);

											final MyWindow window = new MyWindow("Ubah Data", "none", true);
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											window.setHeight("250px");
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
															onSearchDefault(arg0);
														}
													});

												}
											});
											cancel.setParent(toolbar);

											borderlayout.setParent(window);
											window.setVisible(true);
											window.onModal();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);

										}

									}

								}
							});
				}

			});

			// new Label(
			// transaksi.getTanggalTransaksi() == null ? ""
			// : Common.dateFormat3.get().format(transaksi
			// .getTanggalTransaksi())).setParent(arg0);
			new Label(transaksi.getKeterangan()).setParent(arg0);
			if (jurnalKasKecil) {
				Object[] rNot = transaksi.getId() == null ? null : petaRincian.get(transaksi.getId());
				new Label(rNot == null ? "" : (String) rNot[2]).setParent(arg0);
			}
			new Label(transaksi.getDebet() == null ? "0" : Common.numberFormat.get().format(transaksi.getDebet()))
					.setParent(arg0);
			new Label(transaksi.getKredit() == null ? "0" : Common.numberFormat.get().format(transaksi.getKredit()))
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setSpacing("2px");
			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.setAttribute("janganDisabled", true);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					initEdit(arg0, arg1);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(transaksi);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

		private void initEdit(final Row arg0, final Object arg1) throws Exception {
			Common.clear(arg0);
			final Transaksi transaksi = (Transaksi) arg1;
			arg0.setAttribute("transaksi", transaksi);
			final TransaksiJurnalPenerimaanEditor transaksiJurnalPenerimaanEditor = new TransaksiJurnalPenerimaanEditor(
					transaksi);
			transaksi.setParentCode(parentCode);
			transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
			transaksi.setTanggalTransaksi(tanggal.getValue());

			final AmbilDataAkunBanbox akunBanbox = new AmbilDataAkunBanbox();
			akunBanbox.setValue(transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode());
			akunBanbox.setAttribute("akun", transaksi.getAkun());
			akunBanbox.setParent(arg0);

			final Label namaAkun;
			(namaAkun = new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama())).setParent(arg0);

			akunBanbox.setWidth("95%");

			// final Datebox tanggal = new MyDatebox(
			// transaksi.getTanggalTransaksi() == null ?
			// ais.ui.util.WaktuUtil.getDate()
			// : transaksi.getTanggalTransaksi());
			// tanggal.setFormat(Common.dateFormat.get().toPattern());
			// tanggal.setParent(arg0);
			// tanggal.addEventListener("onChange", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// transaksi.setTanggalTransaksi(tanggal.getValue());
			//
			// }
			// });
			// tanggal.setWidth("90%");

			final Textbox keterangan = new Textbox(transaksi.getKeterangan());
			keterangan.setParent(arg0);
			keterangan.setRows(3);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksi.setKeterangan(keterangan.getValue().trim());
				}
			});
			keterangan.setWidth("90%");

			// Kolom "Keterangan Biaya" hanya untuk jurnal Kas Kecil; simpan balik ke KasKecil.getFormula().
			if (jurnalKasKecil) {
				final Object[] rincian = transaksi.getId() == null ? null : petaRincian.get(transaksi.getId());
				final Textbox keteranganBiaya = new Textbox(rincian == null ? "" : (String) rincian[2]);
				keteranganBiaya.setParent(arg0);
				keteranganBiaya.setRows(3);
				keteranganBiaya.setWidth("90%");
				// Baris tanpa rincian (mis. baris kredit Kas Kecil / bukan biaya) TIDAK diaktifkan.
				keteranganBiaya.setReadonly(rincian == null);
				keteranganBiaya.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						if (rincian == null) {
							return;
						}
						String nilaiBaru = keteranganBiaya.getValue() == null ? "" : keteranganBiaya.getValue().trim();
						Session session = HibernateUtil.currentSession();
						boolean ok = GrupTransaksi.simpanNamaRincianKasKecil(session,
							(Long) rincian[0], ((Integer) rincian[1]).intValue(), nilaiBaru);
						if (ok) {
							rincian[2] = nilaiBaru;
						}
					}
				});
			}

			final Doublebox debet = new MyDoublebox(transaksi.getDebet());
			arg0.setAttribute("debet", debet);
			debet.setParent(arg0);
			debet.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksi.setDebet(debet.getValue());
					hitungTotal();
				}
			});
			debet.setWidth("90%");
			debet.setFormat("#,##0.##");
			debet.setSclass("rightDisplay");

			final Doublebox kredit = new MyDoublebox(transaksi.getKredit());
			arg0.setAttribute("kredit", kredit);
			kredit.setParent(arg0);
			kredit.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksi.setKredit(kredit.getValue());
					hitungTotal();
				}
			});
			kredit.setWidth("90%");
			kredit.setFormat("#,##0.##");
			kredit.setSclass("rightDisplay");

			final EventListener myEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Akun akun = (Akun) akunBanbox.getAttribute("akun");
					if (akun != null) {
						// if (akun.getDebetCredit() != null
						// && akun.getDebetCredit().equals(Akun.DEBET)) {
						// debet.setDisabled(false);
						// kredit.setDisabled(true);
						// } else {
						// debet.setDisabled(true);
						// kredit.setDisabled(false);
						// }
						//
						// if (debet.isDisabled()) {
						// debet.setValue(0.0);
						// }
						// if (kredit.isDisabled()) {
						// kredit.setValue(0.0);
						// }
					}
				}
			};

			akunBanbox.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Akun akun = (Akun) akunBanbox.getAttribute("akun");
					namaAkun.setValue(akun == null ? "" : akun.getNama());
					transaksi.setAkun(akun);
					myEventListener.onEvent(arg0);
				}
			});

			final Timer timer = new Timer(500);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					myEventListener.onEvent(null);
					timer.detach();
				}
			});
			timer.start();

			Hbox toolbar = new Hbox();
			toolbar.setSpacing("2px");
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("", "/img/svg/close-circle-line.svg");
			cancel.setTooltiptext("Batal");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (transaksi.getKode() != null) {
						initNotEdit(arg0, arg1);
					} else {
						Session session = HibernateUtil.currentSession();
						session.delete(arg1);
						onSearchDefault(null);
						arg0.detach();
					}

				}
			});
			cancel.setParent(toolbar);
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("", "/img/svg/save-2-fill.svg");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (transaksiJurnalPenerimaanEditor.onSave(event)) {
						initNotEdit(arg0, transaksi);
					}
				}
			});
			save.setParent(toolbar);
		}

		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Transaksi transaksi = (Transaksi) arg1;
			if (transaksi == null) {
				arg0.detach();
				return;
			}
			if (transaksi.getAkun() == null) {
				initEdit(arg0, arg1);
			} else {
				initNotEdit(arg0, arg1);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (noref.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nomor Jurnal dengan nomor yang valid dan unik; (2) Pastikan nomor tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (jenisTransaksi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Transaksi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Transaksi dari dropdown yang tersedia; (2) Pastikan jenis transaksi yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses tambah. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		// if (pegawaiBanbox.getAttribute("pegawai") == null) {
		// MyMessageboxConfig.show("Pegawai harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		final Transaksi transaksi = new Transaksi();
		transaksi.setMerupakanDebet(false);
		transaksi.setAkun(null);
		transaksi.setParentCode(parentCode);
		// transaksi.setGrupTransaksi(grupTransaksi);
		transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
		Session session = HibernateUtil.currentSession();
		session.save(transaksi);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				newTransaksis.add(transaksi);
				onSearchDefault(null);
			}
		});
	}

	public Boolean checkNamaAgama(String kode, GrupTransaksi grupTransaksi) {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(GrupTransaksi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.trim()))
				.add(grupTransaksi == null || grupTransaksi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", grupTransaksi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public boolean onSaveTransaksiUtama(Event event) throws Exception {

		if (noref.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nomor Jurnal dengan nomor yang valid dan unik; (2) Pastikan nomor tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisTransaksi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Transaksi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Transaksi dari dropdown yang tersedia; (2) Pastikan jenis transaksi yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
				|| grid.getRows().getChildren().size() == 0) {
			return false;
		}

		if (workspace != null && workspace.getAttribute("workspace") != null) {
			Workspace workValidasi = (Workspace) workspace.getAttribute("workspace");
			Double saldoSekarang = JenisUangMukaAction.hitungSaldo(null, null,
					this.grupTransaksi == null ? null : this.grupTransaksi.getId(), null, workValidasi,
					tanggal == null ? null : tanggal.getValue());
			Double totalPengajuan = totalDebet == null ? 0.0D : totalDebet;

			if (Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {
				if (saldoSekarang.doubleValue() < totalPengajuan.doubleValue()) {
					MyMessageboxConfig.show("Nilai yang diajukan tidak boleh melebihi sisa saldo. Sisa saldo: "
							+ Common.numberFormat.get().format(saldoSekarang) + ", nilai jurnal: "
							+ Common.numberFormat.get().format(totalPengajuan), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		Session session = HibernateUtil.currentNativeSession();

		Date maxClosing = (Date) session.createCriteria(Closing.class).setProjection(Projections.max("tanggal"))
				.uniqueResult();
		if (maxClosing != null && tanggal.getValue() != null && tanggal.getValue().before(maxClosing)) {

			MyMessageboxConfig.showFormat(
					"Mohon maaf, tanggal jurnal yang Bapak/Ibu masukkan ({V2}) telah melewati tanggal closing (tutup buku), yaitu {V1}. Data pada periode yang sudah closing tidak dapat diubah. Langkah yang dapat dilakukan: (1) gunakan tanggal jurnal setelah tanggal closing {V1}; (2) apabila memang perlu mengubah periode yang telah closing, mohon menghubungi bagian keuangan atau administrator untuk membuka kembali periode tersebut; (3) periksa kembali tanggal transaksi sebelum menyimpan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					Common.dateFormat4.get().format(maxClosing),
					Common.dateFormat4.get().format(tanggal.getValue()));
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
			return false;
		}

		GrupTransaksi grupTransaksi;
		if (this.grupTransaksi.getId() != null) {
			grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.idEq(this.grupTransaksi.getId())).uniqueResult();
		} else {
			if (noref.getValue().trim().isEmpty()) {
				JenisTransaksi jt = (JenisTransaksi) (jenisTransaksi.getSelectedItem() == null ? null
						: jenisTransaksi.getSelectedItem().getValue());

				if (jt != null) {
					if (jt.getNomorSurat() != null) {
						noref.setValue(CommonAkunting.generateNoJurnal(jt, true));
					} else {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(tanggal.getValue());
						noref.setValue(generateCode(jt.getKode(), calendar, 1));
					}
				}
			}
			grupTransaksi = this.grupTransaksi;
		}

		boolean i = checkNamaAgama(noref.getValue(), grupTransaksi);
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Jurnal sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan nomor jurnal yang berbeda dan belum terdaftar; (2) Periksa daftar jurnal yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan kode baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
			return false;
		}

		grupTransaksi.setKode(noref.getValue());

		if (Common.bolehKonfigurasi("file_bukti_transaksi_jurnal_wajib_diupload", Konfigurasi.TIDAK_AKTIF)) {

			if (grupTransaksi != null && grupTransaksi.getId() != null) {
				LampiranLain lain = LampiranLain.ambil(grupTransaksi.getId(), "Bukti Transaksi Jurnal Umum");

				if (lain == null) {
					MyMessageboxConfig.show("Mohon maaf, File Bukti Transaksi belum diupload. Langkah yang dapat dilakukan: (1) Upload file bukti transaksi berformat JPG melalui tombol upload yang tersedia; (2) Pastikan file yang diupload berformat JPG dan ukurannya wajar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					HibernateUtil.closeSession();
					return false;
				}
			} else {
				if (buktiTransaksi == null || !buktiTransaksi.getNama().toLowerCase().endsWith("jpg")) {
					MyMessageboxConfig.show("Mohon maaf, File Bukti Transaksi belum diisi atau bukan format JPG. Langkah yang dapat dilakukan: (1) Pilih dan upload file bukti transaksi berformat JPG; (2) Pastikan file yang dipilih berekstensi .jpg; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					HibernateUtil.closeSession();
					return false;
				}

			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		grupTransaksi.setNomorTagihan(nomorTagihan.getValue().trim());
		grupTransaksi.setKepada(kepada.getValue());
		grupTransaksi.setJenisTransaksi((JenisTransaksi) jenisTransaksi.getSelectedItem().getValue());
		grupTransaksi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		grupTransaksi.setTotalKredit(totalKredit);
		grupTransaksi.setTotalDebet(totalDebet);
		grupTransaksi.setTbmuser(Common.getCurrentUser());
		grupTransaksi.setTanggalTransaksi(tanggal.getValue());
		grupTransaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
		grupTransaksi.setPegawai(tbmuser == null ? null : tbmuser.ambilPegawai());
		grupTransaksi.setParentCode(parentCode);
		grupTransaksi.setWorkspace((Workspace) workspace.getAttribute("workspace"));

		session.getTransaction().begin();
		if (grupTransaksi.getId() != null) {
			session.update(grupTransaksi);
		} else {
			session.save(grupTransaksi);
		}
		session.getTransaction().commit();

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (buktiTransaksi != null && buktiTransaksi.getId() != null) {
				session.refresh(buktiTransaksi);
				buktiTransaksi.setRef(grupTransaksi.getId());

				session.getTransaction().begin();
				session.update(buktiTransaksi);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	private List<Transaksi> transaksis = null;

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Transaksi.class).addOrder(Order.desc("debet"))
				.addOrder(Order.asc("tanggalTransaksi")).addOrder(Order.desc("id"));

		if (grupTransaksi != null && grupTransaksi.getId() != null) {
			// PERBAIKAN: jurnal yang dibuat dari modul lain (transitori, pelunasan, uang muka, dsb)
			// TIDAK memakai parentCode yang sama dengan helper ini (parentCode-nya null → di-generate
			// acak di initWindow), sehingga query lama berbasis parentCode tak menemukan transaksinya
			// (grid kosong walau tabel utama tampil). Muat SEMUA transaksi milik grup ini lewat FK
			// grupTransaksi ATAU baris baru yang sedang ditambah (parentCode). Tidak memfilter
			// jenisJurnal agar konsisten dengan tabel utama (populateDeskripsiLengkap).
			criteria.add(Restrictions.or(Restrictions.eq("grupTransaksi", grupTransaksi),
					Restrictions.eq("parentCode", parentCode)));
		} else {
			// Jurnal baru: hanya baris dengan parentCode ini (jurnal umum).
			criteria.add(Restrictions.eq("parentCode", parentCode))
					.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM));
		}

		transaksis = criteria.list();
		// Baris baru (akun==null, belum tersimpan) selalu di atas agar mudah diisi
		java.util.List<Transaksi> editRows = new java.util.ArrayList<Transaksi>();
		java.util.List<Transaksi> doneRows = new java.util.ArrayList<Transaksi>();
		for (Transaksi t : transaksis) {
			if (t.getAkun() == null) editRows.add(t); else doneRows.add(t);
		}
		transaksis = new java.util.ArrayList<Transaksi>();
		transaksis.addAll(editRows);
		transaksis.addAll(doneRows);
		try {
			petaRincian = jurnalKasKecil
				? grupTransaksi.petakanRincianKasKecilUntukEdit(session, transaksis)
				: new java.util.HashMap<Long, Object[]>();
		} catch (Exception eRincian) {
			petaRincian = new java.util.HashMap<Long, Object[]>();
		}
		// Paging 100 baris/halaman (permintaan pengguna), pager di ATAS, mold OS. Grid
		// mengisi tinggi Center (height:100%) dan scroll sendiri bila 100 baris tak muat.
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.setPagingPosition("top");
		try {
			grid.getPagingChild().setMold("os");
		} catch (Exception ePaging) { ais.common.ErrorAuditUtil.record(ePaging, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/TransaksiJurnalUmumHelper.java:1652");
		}
		ListModel strset = new SimpleListModel(transaksis);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModelCheckMobile(strset);
		// PERBAIKAN SCROLL: grid mold="paging" meng-cache tinggi area body+scroll di
		// browser saat pertama kali dirender. onSearchDefault dipanggil ulang tiap kali
		// baris ditambah/dihapus (mis. tombol "Tambah Data Transaksi") lewat setModel di
		// atas, yang hanya mengirim AU delta (update sebagian) — tinggi/scroll lama yang
		// sudah di-cache TIDAK ikut dihitung ulang, sehingga scrollbar tidak muncul atau
		// tidak bisa turun sampai baris paling bawah. invalidate() memaksa grid dikirim
		// ulang penuh ke browser pada respons berikutnya sehingga tinggi & scroll dihitung
		// ulang sesuai jumlah baris terkini.
		grid.invalidate();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungTotal();
			}
		});
	}

	private void hitungTotal() {
		totalKredit = 0.0;
		totalDebet = 0.0;

		if (transaksis != null) {

			for (Transaksi transaksi : transaksis) {
				try {
//					Transaksi transaksi = (Transaksi) row.getAttribute("transaksi");

//					if (row.getAttribute("kredit") != null && row.getAttribute("kredit") instanceof Doublebox) {
//						Doublebox label = (Doublebox) row.getAttribute("kredit");
//						Double n = label.getValue();
//						totalKredit += n;
//					} else {
					Double n = transaksi == null ? 0.0 : transaksi.getKredit();
					totalKredit += n;
//					}

//					if (row.getAttribute("debet") != null && row.getAttribute("debet") instanceof Doublebox) {
//						Doublebox label = (Doublebox) row.getAttribute("debet");
//						Double n = label.getValue();
//						totalDebet += n;
//					} else {
					n = transaksi == null ? 0.0 : transaksi.getDebet();
					totalDebet += n;
//					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		labelTotalDebet.setValue(Common.numberFormat.get().format(totalDebet));
		labelTotalKredit.setValue(Common.numberFormat.get().format(totalKredit));
		labelTotalSelisih.setValue(Common.numberFormat.get().format(Math.abs(totalDebet - totalKredit)));
		grupTransaksi.setTotalKredit(totalKredit);
		grupTransaksi.setTotalDebet(totalDebet);

		if (grupTransaksi.getTotalDebet().toString().equals("0.0")
				|| grupTransaksi.getTotalKredit().toString().equals("0.0")
				|| !Common.numberFormat.get().format(grupTransaksi.getTotalDebet())
						.equals(Common.numberFormat.get().format(grupTransaksi.getTotalKredit()))) {
			save.setDisabled(true);
		} else {
			save.setDisabled(false);
		}
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

}
