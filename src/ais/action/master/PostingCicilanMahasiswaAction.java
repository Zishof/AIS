package ais.action.master;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
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

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PostingCicilanMahasiswaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;
	private Textbox searchnama;
	private Textbox searchnamamhs;
	private Combobox searchfakultas;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Combobox searchitembiaya;
	private Combobox searchjenispembayaran;
	private Decimalbox searchtahun;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox searchjenjang;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private Tabpanel tabpanelPostingDibayarDimuka;
	private Tabpanel tabpanelPostingTabungan;
	private Tabpanel tabpanelPostingPengeluaran;
	private Tabpanel tabpanelPostingPiutang;
	private Tabpanel tabpanelPostingBiayaAdmin;
	private Tabpanel tabpanelPostingBiayaPaymentGateway;
	
	private Tbmuser tbmuser;

	public void onDibayarDimuka(Event event) {
		if (tabpanelPostingDibayarDimuka.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingDibayarDimuka);
			MyInclude iframe = new MyInclude("/pages/master/posting_cicilan_mahasiswa_dibayar_dimuka.zul");
			iframe.setParent(window);
		}
	}

	public void onTabungan(Event event) {
		if (tabpanelPostingTabungan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingTabungan);
			MyInclude iframe = new MyInclude("/pages/master/posting_tabungan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onPengeluaran(Event event) {
		if (tabpanelPostingPengeluaran.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingPengeluaran);
			MyInclude iframe = new MyInclude("/pages/master/posting_pengeluaran_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onPiutang(Event event) {
		if (tabpanelPostingPiutang.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingPiutang);
			MyInclude iframe = new MyInclude("/pages/master/posting_piutang_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onBiayaAdmin(Event event) {
		if (tabpanelPostingBiayaAdmin.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingBiayaAdmin);
			MyInclude iframe = new MyInclude("/pages/master/posting_biaya_administrasi_pembayaran_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onBiayaPaymentGateway(Event event) {
		if (tabpanelPostingBiayaPaymentGateway.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingBiayaPaymentGateway);
			MyInclude iframe = new MyInclude("/pages/master/posting_biaya_payment_gateway_pembayaran_mahasiswa.zul");
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
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Common.insertComboDanSemua(searchitembiaya, new String[] { "nama", "kode" }, "penghitungan", ItemBiaya.class,
				Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchjenispembayaran, new String[] { "namaKegiatan", "kode" }, "keterangan",
				JenisKegiatan.class, Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		sudahPostingDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterSudahPosting();
		ais.action.master.helper.PostingJurnalHelper.terapkanParameterTanggal(tglMulai, tglSampai);

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);
			}
		});
	        FilterLanjutHelper.setup(comp, 2);
}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								tx = session.beginTransaction();
								
								List<CicilanPembayaran> cicilanPembayarans = initCriteria(true, session)
										.add(Restrictions.isNotNull("postingHistory")).list();

								int rowProcessed = 0;
								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									cicilanPembayaran.setPostingHistory(null);
									session.update(cicilanPembayaran);
									
									session.createSQLQuery("delete from akunting.grup_transaksi where cicilan_pembayaran="
											+ cicilanPembayaran.getId() + " and ref != 'dimuka'" + " and closing is null")
											.executeUpdate();
											
									rowProcessed++;
									if (rowProcessed % 50 == 0) {
										session.flush();
										session.clear();
									}
								}
								tx.commit();
							} catch (Exception e) {
								if (tx != null && tx.isActive()) {
									try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanMahasiswaAction.java:280");}
								}
								ais.common.Common.tampilErrorJikaAdmin(e);
							} finally {
								closeSession(session);
							}
						}

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});
	}

	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Pembayaran Mahasiswa");
		addWindow.setWidth("800px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));

		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal",
							"Kolom Tanggal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Tanggal.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return;
				}
				
				if (keterangan.getValue().trim().equals("")) {
				    PesanFormalHelper.tampilkanGagal("penyimpanan data Keterangan",
				    		"Kolom Keterangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
				    		new String[] {
				    				"Isi/pilih terlebih dahulu Keterangan.",
				    				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
				    		});
				    return;
				}

				// Mengambil nilai UI sebelum thread paralel berjalan untuk menghindari exception desktop context ZK
				final String strKeterangan = keterangan.getValue().trim();
				final Date dateMulai = tglMulai.getValue();
				final Date dateSampai = tglSampai.getValue();
				final Tbmuser currentUser = Common.getCurrentUser();
				final SatuanKerja uiSatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				
				final List<Long> cicilanIds = new ArrayList<Long>();
				Session uiSession = null;
				try {
					uiSession = HibernateUtil.getSessionFactory().openSession();
					Criteria crit = initCriteria(true, uiSession).add(Restrictions.isNull("postingHistory"));
					crit.setProjection(Projections.property("id"));
					List res = crit.list();
					if (res != null) cicilanIds.addAll(res);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				} finally {
					closeSession(uiSession);
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pembayaran ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Posting transaksi mahasiswa berhasil dilakukan",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});
											addWindow.detach();
										}
									});

									// BACKGROUND TASK PROCESS (Multi-threading Parallel 150 Threads)
									new Thread(new Runnable() {
										@Override
										public void run() {

											// Setup Executor
											ExecutorService executor = Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(50));
											final AtomicInteger processedCount = new AtomicInteger(0);
											final int totalData = cicilanIds.size();

											for (final Long id : cicilanIds) {
												executor.execute(new Runnable() {
													@Override
													public void run() {
														Session session = null;
														Transaction tx = null;

														try {
															// Tiap thread buka session secara independen & aman
															session = HibernateUtil.getSessionFactory().openSession();
															tx = session.beginTransaction();

															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session.get(CicilanPembayaran.class, id);
															if (cicilanPembayaran == null) {
																tx.rollback();
																return;
															}

															

															SatuanKerja satuanKerja = uiSatuanKerja;

															if (cicilanPembayaran.getKegiatan() != null
																	&& cicilanPembayaran.getKegiatan().getMahasiswa() != null
																	&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan() != null
																	&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas() != null
																	&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas().getSatuanKerja() != null) {
																satuanKerja = cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas().getSatuanKerja();
															}

															if (cicilanPembayaran.getKegiatan() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas().getSatuanKerja() != null) {
																satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas().getSatuanKerja();
															} else if (cicilanPembayaran.getKegiatan() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas() != null
																	&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas().getSatuanKerja() != null) {
																satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas().getSatuanKerja();
															}

															if (uiSatuanKerja != null) {
																satuanKerja = uiSatuanKerja;
															}

															if (cicilanPembayaran.getItemBiaya() != null) {
																Akun akunDebet = cicilanPembayaran.getItemBiaya().getJenisPembayaran() != null
																		&& cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun() != null
																				? cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun()
																				: cicilanPembayaran.getJenisPembayaran().getAkun();
																				
																Akun akunPiutang = cicilanPembayaran.getItemBiaya().ambilPiutang(cicilanPembayaran.getKegiatan());
																Akun akunKredit = akunPiutang != null ? akunPiutang
																		: cicilanPembayaran.getItemBiaya().ambilAkun(cicilanPembayaran.getKegiatan());

																Date tanggalTagihan = cicilanPembayaran.getTanggalTagihan();
																if (tanggalTagihan != null) {
																	if (Integer.parseInt(Common.dateFormat83.get().format(tanggalTagihan)) > Integer.parseInt(Common.dateFormat83.get().format(cicilanPembayaran.getTanggal()))) {
																		Akun akunDibayarDimuka = cicilanPembayaran.getItemBiaya().ambilDibayarDimuka(cicilanPembayaran.getKegiatan());
																		if (akunDibayarDimuka != null) {
																			akunKredit = akunDibayarDimuka;
																		}
																	}
																}

																if (akunDebet != null && akunKredit != null) {
																	Boolean apakahUangMasuk = true;
																	String ket = "";
																	try {
																		Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan().getMahasiswa() == null
																				? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? null : cicilanPembayaran.getKegiatan().getCalonMahasiswa().getMahasiswa())
																				: cicilanPembayaran.getKegiatan().getMahasiswa());

																		StringBuilder ketBuilder = new StringBuilder();
																		ketBuilder.append("Pembayaran ").append(mahasiswa == null ? cicilanPembayaran.getKegiatan().getCalonMahasiswa() : mahasiswa)
																				  .append(" ke ").append(cicilanPembayaran.getKe()).append(" - ")
																				  .append(cicilanPembayaran.getItemBiaya().getNama()).append(" - ")
																				  .append(cicilanPembayaran.getKeterangan());
																		ket = ketBuilder.toString();
																	} catch (Exception e) {
																		Common.tampilErrorJikaAdmin(e);
																	}

																	if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
																		PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran.getPengaturanPembayaranBulanan();
																		ket = pengaturanPembayaranBulanan.getKeterangan();
																		ket = (ket.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : ket) 
																				+ ", bulan " + pengaturanPembayaranBulanan.getNamaBulan();

																		try {
																			Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan().getMahasiswa() == null
																					? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? null : cicilanPembayaran.getKegiatan().getCalonMahasiswa().getMahasiswa())
																					: cicilanPembayaran.getKegiatan().getMahasiswa());

																			ket = "Pembayaran " + (mahasiswa == null ? cicilanPembayaran.getKegiatan().getCalonMahasiswa() : mahasiswa) + " " + ket;
																		} catch (Exception e) {
																			Common.tampilErrorJikaAdmin(e);
																		}
																	}
																	
																	// ==============================================================
																	// INSTRUKSI: Simpan 1 PostingHistory untuk tiap proses dilakukan
																	// ==============================================================
																	PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
																	postingHistory.setTanggal(tgl);
																	postingHistory.setTbmuser(currentUser);
																	
																	StringBuilder sbKet = new StringBuilder();
																	sbKet.append(strKeterangan).append(" \nTgl:")
																		 .append(Common.dateFormat.get().format(dateMulai)).append(" s.d ")
																		 .append(Common.dateFormat.get().format(dateSampai));
																	postingHistory.setKeterangan(sbKet.toString());
																	
																	session.save(postingHistory);
																	

																	Double nilai = cicilanPembayaran.getNilai();
																	try {
																		Akun akunDenda = null;
																		Double denda = cicilanPembayaran.getDenda();
																		if (denda != null && denda > 0.1) {
																			akunDenda = cicilanPembayaran.getItemBiaya().ambilPendapatanDenda(cicilanPembayaran.getKegiatan());
																		}

																		if (nilai > 0.1) {
																			CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutang, postingHistory,
																					apakahUangMasuk, ket, cicilanPembayaran.getTanggal(), nilai, denda, cicilanPembayaran, satuanKerja, session);
																		} else {
																			CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutang, postingHistory,
																					apakahUangMasuk, ket, cicilanPembayaran.getTanggal(), nilai, denda, cicilanPembayaran, satuanKerja, session);
																		}
																	} catch (Exception e) {
																		Common.tampilErrorJikaAdmin(e);
																		// Jurnal GAGAL dibuat untuk cicilan ini -> JANGAN ditandai "posted". Batalkan transaksi
																		// item ini lalu lewati; item lain diproses pada thread/transaksi terpisah, tidak terpengaruh.
																		if (tx != null && tx.isActive()) {
																			try {
																				tx.rollback();
																			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanMahasiswaAction.java:595");
																			}
																		}
																		return;
																	}

																	cicilanPembayaran.setPostingHistory(postingHistory);
																	session.update(cicilanPembayaran);
																}
															}
															
															tx.commit();
															
														} catch (Exception e) {
															if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanMahasiswaAction.java:609");} }
															ais.common.Common.tampilErrorJikaAdmin(e);
														} finally {
															closeSession(session);
															
															// Update Loading Bar UI dari dalam background thread
															int current = processedCount.incrementAndGet();
															int sisa = totalData - current;
															double pct = (current * 100.0) / totalData;
															
															try { 
																DecimalFormat df = new DecimalFormat("#.##");
																label.setValue("Memproses: " + current + " dari " + totalData + " data (" + df.format(pct) + "%). Sisa: " + sisa + " data"); 
															} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
														}
													}
												});
											}

											// Menunggu semua thread paralel selesai bekerja
											executor.shutdown();
											try {
												executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
											} catch (InterruptedException e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}

											// Setel label kosong untuk men-trigger UI menutup LoadBar
											try { label.setValue(""); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}
									}).start();
								}
							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	class CicilanPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) arg1;
			final Kegiatan kegiatan = cicilanPembayaran.getKegiatan();

			if (kegiatan.getMahasiswa() != null) {
				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);
				RevisiHelper.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama()).setParent(arg0);

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : desc) 
							+ ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(cicilanPembayaran.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(cicilanPembayaran.getNilai() == null ? "0" : Common.numberFormat.get().format(cicilanPembayaran.getNilai())).setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
						: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);

			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNim()).setParent(arg0);
				RevisiHelper.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama()).setParent(arg0);

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : desc) 
							+ ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(cicilanPembayaran.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(cicilanPembayaran.getNilai() == null ? "0" : Common.numberFormat.get().format(cicilanPembayaran.getNilai())).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null ? ""
						: kegiatan.getCalonMahasiswa().getProdiLulus().getNama()).setParent(arg0);
			}
			new Label(kegiatan.getSemster() + "").setParent(arg0);

			Date tanggalTagihan = cicilanPembayaran.getTanggalTagihan();
			if (tanggalTagihan != null) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label("Tagihan " + Common.dateFormat3.get().format(tanggalTagihan)).setParent(vbox);
				new Label("Dibayar " + Common.dateFormat3.get().format(cicilanPembayaran.getTanggal())).setParent(vbox);
			} else {
				new Label(Common.dateFormat3.get().format(cicilanPembayaran.getTanggal())).setParent(arg0);
			}

			Akun akunDebet = null;
			try {
				akunDebet = cicilanPembayaran.getItemBiaya().getJenisPembayaran() != null
						&& cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun() != null
								? cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun()
								: cicilanPembayaran.getJenisPembayaran().getAkun();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			Akun akunPiutang = cicilanPembayaran.getItemBiaya().ambilPiutang(cicilanPembayaran.getKegiatan());
			Akun akunKredit = akunPiutang != null ? akunPiutang : cicilanPembayaran.getItemBiaya().ambilAkun(cicilanPembayaran.getKegiatan());

			if (tanggalTagihan != null) {
				if (Integer.parseInt(Common.dateFormat83.get().format(tanggalTagihan)) > Integer.parseInt(Common.dateFormat83.get().format(cicilanPembayaran.getTanggal()))) {
					Akun akunDibayarDimuka = cicilanPembayaran.getItemBiaya().ambilDibayarDimuka(cicilanPembayaran.getKegiatan());
					if (akunDibayarDimuka != null) {
						akunKredit = akunDibayarDimuka;
					}
				}
			}

			if (akunDebet != null && akunKredit != null) {
				Double nilai = cicilanPembayaran.getNilai();
				Akun akunDenda = null;
				Double denda = cicilanPembayaran.getDenda();
				
				if (denda != null && denda > 0.1) {
					akunDenda = cicilanPembayaran.getItemBiaya().ambilPendapatanDenda(cicilanPembayaran.getKegiatan());
				}

				if (denda != null && denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan").setParent(arg0);
				} else {
					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Akun> akunsKredits = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();

					akunsDebets.add(akunDebet);
					nilaiDebets.add(nilai);

					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai - denda);
					if (denda != null && denda > 0.1 && akunDenda != null) {
						akunsKredits.add(akunDenda);
						nilaiKredits.add(denda);
					}

					GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);
				}
			} else {
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						cicilanPembayaran.getItemBiaya(), cicilanPembayaran.getKegiatan()), cicilanPembayaran.getItemBiaya(), cicilanPembayaran.getKegiatan());
			}

			String bukti = "";
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				bukti = (String) session.createCriteria(GrupTransaksi.class)
						.add(Restrictions.eq("cicilanPembayaran", cicilanPembayaran)).setMaxResults(1)
						.add(Restrictions.or(Restrictions.isNull("ref"), Restrictions.eq("ref", "")))
						.setProjection(Projections.property("kode")).uniqueResult();
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				closeSession(session);
			}

			new Label(cicilanPembayaran.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: cicilanPembayaran.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan Posting", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && cicilanPembayaran.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session localSession = null;
								Transaction tx = null;
								try {
									localSession = HibernateUtil.getSessionFactory().openSession();
									tx = localSession.beginTransaction();
									cicilanPembayaran.setPostingHistory(null);
									localSession.update(cicilanPembayaran);
									
									localSession.createSQLQuery("delete from akunting.grup_transaksi where (ref is null or ref='') and cicilan_pembayaran="
													+ cicilanPembayaran.getId() + " and ref != 'dimuka'" + " and closing is null")
											.executeUpdate();
									tx.commit();
								} catch (Exception e) {
									if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanMahasiswaAction.java:819");} }
									ais.common.Common.tampilErrorJikaAdmin(e);
								} finally {
									closeSession(localSession);
								}

								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});
					}
				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Posting", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && cicilanPembayaran.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session localSession = null;
								Transaction tx = null;
								
								try {
									localSession = HibernateUtil.getSessionFactory().openSession();
									tx = localSession.beginTransaction();

									PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
									postingHistory.setTbmuser(Common.getCurrentUser());
									postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
									postingHistory.setKeterangan("Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
													+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
									localSession.save(postingHistory);

									Akun akunDebet = cicilanPembayaran.getItemBiaya().getJenisPembayaran() != null
											&& cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun() != null
													? cicilanPembayaran.getItemBiaya().getJenisPembayaran().getAkun()
													: cicilanPembayaran.getJenisPembayaran().getAkun();

									Akun akunPiutang = cicilanPembayaran.getItemBiaya().ambilPiutang(cicilanPembayaran.getKegiatan());
									Akun akunKredit = akunPiutang != null ? akunPiutang
											: cicilanPembayaran.getItemBiaya().ambilAkun(cicilanPembayaran.getKegiatan());

									Date tanggalTagihan = cicilanPembayaran.getTanggalTagihan();
									if (tanggalTagihan != null) {
										if (Integer.parseInt(Common.dateFormat83.get().format(tanggalTagihan)) > Integer.parseInt(Common.dateFormat83.get().format(cicilanPembayaran.getTanggal()))) {
											Akun akunDibayarDimuka = cicilanPembayaran.getItemBiaya().ambilDibayarDimuka(cicilanPembayaran.getKegiatan());
											if (akunDibayarDimuka != null) {
												akunKredit = akunDibayarDimuka;
											}
										}
									}

									if (akunDebet != null && akunKredit != null) {
										Boolean apakahUangMasuk = true;
										String ket = "";
										
										try {
											Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan().getMahasiswa() == null
													? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? null : cicilanPembayaran.getKegiatan().getCalonMahasiswa().getMahasiswa())
													: cicilanPembayaran.getKegiatan().getMahasiswa());

											StringBuilder ketBuilder = new StringBuilder();
											ketBuilder.append("Pembayaran ").append(mahasiswa == null ? cicilanPembayaran.getKegiatan().getCalonMahasiswa() : mahasiswa)
													  .append(" ke ").append(cicilanPembayaran.getKe()).append(" - ")
													  .append(cicilanPembayaran.getItemBiaya().getNama()).append(" - ")
													  .append(cicilanPembayaran.getKeterangan());
											ket = ketBuilder.toString();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran.getPengaturanPembayaranBulanan();
											ket = pengaturanPembayaranBulanan.getKeterangan();
											ket = (ket.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : ket) + ", bulan " + pengaturanPembayaranBulanan.getNamaBulan();

											try {
												Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan().getMahasiswa() == null
																? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? null : cicilanPembayaran.getKegiatan().getCalonMahasiswa().getMahasiswa())
																: cicilanPembayaran.getKegiatan().getMahasiswa());
												ket = "Pembayaran " + (mahasiswa == null ? cicilanPembayaran.getKegiatan().getCalonMahasiswa() : mahasiswa) + " " + ket;
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}
										
										Double nilai = cicilanPembayaran.getNilai();
										Akun akunDenda = null;
										Double denda = cicilanPembayaran.getDenda();
										if (denda != null && denda > 0.1) {
											akunDenda = cicilanPembayaran.getItemBiaya().ambilPendapatanDenda(cicilanPembayaran.getKegiatan());
										}

										SatuanKerja satuanKerja = (SatuanKerja) (cicilanPembayaran.getKegiatan() != null
												&& cicilanPembayaran.getKegiatan().getMahasiswa() != null
												&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan() != null
												&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas() != null
												&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas().getSatuanKerja() != null
																? cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas().getSatuanKerja()
																: tbmuser.ambilSatuanKerja());

										if (cicilanPembayaran.getKegiatan() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas().getSatuanKerja() != null) {
											satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus().getFakultas().getSatuanKerja();
										} else if (cicilanPembayaran.getKegiatan() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas() != null
												&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas().getSatuanKerja() != null) {
											satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas().getSatuanKerja();
										}

										if (tbmuser.ambilSatuanKerja() != null) {
											satuanKerja = tbmuser.ambilSatuanKerja();
										}

										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutang,
													postingHistory, apakahUangMasuk, ket, cicilanPembayaran.getTanggal(),
													nilai, denda, cicilanPembayaran, satuanKerja, localSession);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutang,
													postingHistory, apakahUangMasuk, ket, cicilanPembayaran.getTanggal(),
													nilai, denda, cicilanPembayaran, satuanKerja, localSession);
										}

										cicilanPembayaran.setPostingHistory(postingHistory);
										// FIX NonUniqueObjectException: cicilanPembayaran adalah instance milik
										// SESSION LUAR (grid), sementara CommonAkunting.saveTransaksi(...) di atas
										// bisa saja sudah memuat instance LAIN dengan id yang SAMA ke localSession
										// (mis. lewat referensi Kegiatan/DetailBiaya). update() akan meledak bila
										// localSession sudah punya representasi lain utk id ini -- merge() aman:
										// menyalin state ke instance yang sudah ada di session, atau memuatnya bila
										// belum ada, tanpa melempar NonUniqueObjectException.
										localSession.merge(cicilanPembayaran);
									}
									tx.commit();
									loadDataDenganProgressPosting(null);
								} catch (Exception e) {
									if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanMahasiswaAction.java:964");} }
									ais.common.Common.tampilErrorJikaAdmin(e);
								} finally {
									closeSession(localSession);
								}
							}
						});
					}
				});
				aksiButtons.add(button);
			}
			// Tampilkan kebab HANYA bila ada tombol; bila akun debet/kredit belum dipetakan
			// daftar aksi kosong -> isi sel dengan Label kosong supaya kolom tetap sejajar
			// tanpa memunculkan tombol "..." yang isinya hampa.
			if (aksiButtons.isEmpty()) {
				new Label("").setParent(arg0);
			} else {
				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			}
		}
	}

	public Criteria initCriteria(boolean order, Session session) {
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
						
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(CicilanPembayaran.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))
				.add(Restrictions.ne("nilai", 0.0))
				.add(Restrictions.isNotNull("nilai"))
				.add(restriksiTanggalPosting())
				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiaya", searchitembiaya.getSelectedItem().getValue()))
				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistory")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))
				.createAlias("kegiatan", "kegiatan")
				.add(searchjenispembayaran.getSelectedItem() == null || searchjenispembayaran.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kegiatan.jenisKegiatan", searchjenispembayaran.getSelectedItem().getValue()))
				.add((jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: (jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("kegiatan.semster", Common.genap)
								: Restrictions.in("kegiatan.semster", Common.ganjil))));

		if (order) criteria.addOrder(Order.desc("id"));
		
		criteria.createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodi1", "prodi1", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodi2", "prodi2", Criteria.LEFT_JOIN)
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))
				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.or(
										Restrictions.or(Restrictions.eq("calonMahasiswa.prodiLulus", jurusan),
												Restrictions.eq("calonMahasiswa.prodi1", jurusan)),
										Restrictions.eq("calonMahasiswa.prodi2", jurusan))))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.or(
										Restrictions.or(Restrictions.eq("prodiLulus.fakultas", fakultas),
												Restrictions.eq("prodi1.fakultas", fakultas)),
										Restrictions.eq("prodi2.fakultas", fakultas))))
				.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nama", searchnamamhs.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE)),
										Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(), MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));

		return criteria;
	}

	private org.hibernate.criterion.Criterion restriksiTanggalPosting() {
		if (tglMulai == null || tglSampai == null || tglMulai.getValue() == null || tglSampai.getValue() == null) {
			return Restrictions.sqlRestriction("1=1");
		}
		String mulai = Common.databaseDateFormat.get().format(tglMulai.getValue());
		String sampai = Common.databaseDateFormat.get().format(tglSampai.getValue());
		/* FIX 20-08-2026 (pencatatan realisasi): filter lama memakai
		 * coalesce(tanggal_tagihan, tanggal) sehingga baris IKUT TERSARING berdasarkan tanggal
		 * TAGIHAN. Akibatnya pembayaran yang direalisasikan 10-08 muncul pada rentang Juli hanya
		 * karena tagihannya bertanggal 17-07 -- pengelompokan bulan jurnal pembayaran jadi salah.
		 * Layar ini adalah JURNAL PEMBAYARAN, jadi dasar periodenya HARUS tanggal realisasi
		 * pembayaran (kolom tanggal) -- konsisten dengan tanggal yang dipakai saat posting jurnal
		 * (lihat pemanggilan posting yang memakai cicilanPembayaran.getTanggal()).
		 * Tanggal tagihan tetap ditampilkan di kolom Waktu sebagai informasi. */
		return Restrictions.sqlRestriction("date(this_.tanggal) between date('" + mulai + "') and date('"
				+ sampai + "')");
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Common.initPaging(initCriteria(false, session), paging);

			List<CicilanPembayaran> cicilanPembayaran = initCriteria(true, session)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
					
			ListModel strset = new SimpleListModel(cicilanPembayaran);
			grid.setRowRenderer(new CicilanPembayaranRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
	}

	/**
	 * Helper universal untuk menutup session secara ketat dan mencegah memory leak.
	 */
	private void closeSession(Session session) {
		if (session != null) {
			try { if (session.isOpen()) session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try { if (session.isOpen()) session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
	}


	public void onSearchDefault(Event event) {

		// Filter saling-eksklusif (mutex filter posting): centang salah satu -> yang lain
		// otomatis di-uncheck DAN dinonaktifkan; uncheck kembali -> keduanya aktif lagi.
		if (searchtampil != null && searchtelahtampil != null) {
			if (event instanceof org.zkoss.zk.ui.event.ForwardEvent) {
				org.zkoss.zk.ui.Component asalCb = ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin()
						.getTarget();
				if (asalCb == searchtelahtampil && searchtelahtampil.isChecked()) {
					searchtampil.setChecked(false);
				} else if (asalCb == searchtampil && searchtampil.isChecked()) {
					searchtelahtampil.setChecked(false);
				}
			}
			searchtampil.setDisabled(searchtelahtampil.isChecked());
			searchtelahtampil.setDisabled(searchtampil.isChecked());
		}
		loadDataDenganProgressPosting(event);
	}

	/* Status posting kiriman dasbor draft jurnal (null = dibuka dari menu). */
	private Boolean sudahPostingDasbor = null;

	private boolean postingJurnalLoadingAktif = false;
	private boolean postingJurnalReloadTertunda = false;

	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}


	// =====================================================================
	// JALUR NON-ZK (dasbor Draft Jurnal lewat API POS)
	// PEMELIHARAAN: akun & nilai HARUS tetap identik dengan {@link #onPostingSemua}.
	// =====================================================================

	/**
	 * Kriteria cicilan pembayaran mahasiswa pada rentang tanggal -- sama dengan penghitung
	 * baris "Mahasiswa - Pembayaran" pada dasbor (saringan {@code date(this_.tanggal)}).
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(CicilanPembayaran.class);
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA pembayaran mahasiswa dalam rentang.
	 *
	 * <p><b>Saringan {@code ref != 'dimuka'} itu wajib.</b> Cicilan yang sama juga memikul
	 * jurnal "dibayar di muka" yang ditandai {@code ref = 'dimuka'} dan diurus layar lain.
	 * Syaratnya disalin apa adanya dari {@link #onBatalkanPostingSemua}.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<CicilanPembayaran> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (CicilanPembayaran cicilan : daftar) {
				try {
					String syarat = "cicilan_pembayaran=" + cicilan.getId()
							+ " and (ref is null or ref != 'dimuka') and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					cicilan.setPostingHistory(null);
					session.update(cicilan);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
			}
		}
		return n;
	}

	/**
	 * Posting SEMUA pembayaran mahasiswa yang belum dijurnal dalam rentang.
	 *
	 * <ul>
	 *   <li>debet = akun jenis pembayaran pada ITEM BIAYA bila ada, selain itu akun jenis
	 *       pembayaran pada cicilannya;</li>
	 *   <li>kredit = akun piutang item biaya bila ada, selain itu akun pendapatannya;</li>
	 *   <li><b>bila tanggal tagihannya MELEWATI tanggal bayar</b> (dibayar lebih awal) dan item
	 *       biayanya punya akun "dibayar di muka", kredit dipindahkan ke akun itu -- pembayaran
	 *       yang mendahului masa tagihan bukan pendapatan, melainkan kewajiban;</li>
	 *   <li>bila ada denda &gt; 0,1, akun pendapatan dendanya ikut dikirim ke
	 *       {@code saveTransaksi};</li>
	 *   <li>bila nilainya &le; 0,1 posisi debet/kredit ditukar.</li>
	 * </ul>
	 *
	 * <p>Satuan kerja: fakultas mahasiswa/calon; cadangan satuan kerja pengguna.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pembayaran mahasiswa dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			SatuanKerja satuanKerjaPengguna = oleh == null ? null : oleh.ambilSatuanKerja();

			for (Long id : ids) {
				try {
					session = HibernateUtil.currentNativeSession();
					CicilanPembayaran cicilan = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(id)).uniqueResult();
					if (cicilan == null || cicilan.getItemBiaya() == null || cicilan.getKegiatan() == null) {
						continue;
					}
					Akun akunDebet = cicilan.getItemBiaya().getJenisPembayaran() != null
							&& cicilan.getItemBiaya().getJenisPembayaran().getAkun() != null
									? cicilan.getItemBiaya().getJenisPembayaran().getAkun()
									: (cicilan.getJenisPembayaran() == null ? null
											: cicilan.getJenisPembayaran().getAkun());
					Akun akunPiutang = cicilan.getItemBiaya().ambilPiutang(cicilan.getKegiatan());
					Akun akunKredit = akunPiutang != null ? akunPiutang
							: cicilan.getItemBiaya().ambilAkun(cicilan.getKegiatan());
					java.util.Date tanggalTagihan = cicilan.getTanggalTagihan();
					if (tanggalTagihan != null && cicilan.getTanggal() != null) {
						try {
							if (Integer.parseInt(Common.dateFormat83.get().format(tanggalTagihan)) > Integer
									.parseInt(Common.dateFormat83.get().format(cicilan.getTanggal()))) {
								Akun dimuka = cicilan.getItemBiaya().ambilDibayarDimuka(cicilan.getKegiatan());
								if (dimuka != null) {
									akunKredit = dimuka;
								}
							}
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
						}
					}
					if (akunDebet == null || akunKredit == null) {
						continue;
					}

					SatuanKerja satuanKerja = satuanKerjaPengguna;
					try {
						if (cicilan.getKegiatan().getMahasiswa() != null
								&& cicilan.getKegiatan().getMahasiswa().getJurusan() != null
								&& cicilan.getKegiatan().getMahasiswa().getJurusan().getFakultas() != null
								&& cicilan.getKegiatan().getMahasiswa().getJurusan().getFakultas()
										.getSatuanKerja() != null) {
							satuanKerja = cicilan.getKegiatan().getMahasiswa().getJurusan().getFakultas()
									.getSatuanKerja();
						} else if (cicilan.getKegiatan().getCalonMahasiswa() != null
								&& cicilan.getKegiatan().getCalonMahasiswa().getProdi1() != null
								&& cicilan.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas() != null
								&& cicilan.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas()
										.getSatuanKerja() != null) {
							satuanKerja = cicilan.getKegiatan().getCalonMahasiswa().getProdi1().getFakultas()
									.getSatuanKerja();
						}
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
					}

					Object siapa = cicilan.getKegiatan().getMahasiswa() == null
							? cicilan.getKegiatan().getCalonMahasiswa()
							: cicilan.getKegiatan().getMahasiswa();
					String ket = "Pembayaran " + siapa + " ke " + cicilan.getKe() + " - "
							+ cicilan.getItemBiaya().getNama() + " - " + cicilan.getKeterangan();

					Double nilai = cicilan.getNilai();
					Double denda = cicilan.getDenda();
					Akun akunDenda = null;
					if (denda != null && denda > 0.1) {
						akunDenda = cicilan.getItemBiaya().ambilPendapatanDenda(cicilan.getKegiatan());
					}
					if (denda == null) {
						denda = Double.valueOf(0.0);
					}

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutang,
									postingHistory, true, ket, cicilan.getTanggal(), nilai, denda, cicilan,
									satuanKerja, session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutang,
									postingHistory, true, ket, cicilan.getTanggal(), nilai, denda, cicilan,
									satuanKerja, session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
					}

					if (tersimpan) {
						cicilan.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(cicilan);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingCicilanMahasiswaAction jalur API");
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil posting
			}
		}
		return n;
	}
}
