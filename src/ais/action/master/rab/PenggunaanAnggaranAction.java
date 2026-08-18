package ais.action.master.rab;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class PenggunaanAnggaranAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private MyDatebox start;
	private MyDatebox end;
	private Checkbox searchaktif;

	private MyCheckboxConfig tampilkanJurnalUmum;
	private MyCheckboxConfig tampilkanPermintaanPengadaanMasterAssetDetail;
	private MyCheckboxConfig tampilkanUangMuka;
	private MyCheckboxConfig tampilkanPertangungjawaban;
	private MyCheckboxConfig tampilkanSaldoAwalMasterAssetDetail;
	private MyCheckboxConfig tampilkanPembayaranGaji;
	private MyCheckboxConfig tampilkanKasKecil;
	private MyCheckboxConfig tampilkanKasBesar;

	private AmbilDataWorkspaceBanbox searchAnggaran;

	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		Session sessionTarik = null;
		try {
			if (execution.getParameter("workspace") != null) {
				sessionTarik = HibernateUtil.getSessionFactory().openSession();
				Workspace workspace = (Workspace) ConstantValues.simpleObject(sessionTarik.createCriteria(Workspace.class)
						.add(Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("workspace")))), Workspace.class);

				if (workspace != null) {
					searchAnggaran.setAttribute("workspace", workspace);
					searchAnggaran.setValue(workspace.getNama());
					searchAnggaran.setDisabled(true);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (sessionTarik != null && sessionTarik.isOpen()) {
				try { sessionTarik.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { sessionTarik.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { sessionTarik.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		if (add != null) {
			add.setVisible(false);
			add.setTooltiptext("Tambah");
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 20);
		if (start != null) start.setValue(calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchAnggaran.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "waktu", "nilai",
				"disposisiSop", "workspace", "workspace.hargaTotal", "grupTransaksi",
				"permintaanPengadaanMasterAssetDetail", "uangMuka", "saldoAwalMasterAssetDetail", "pembayaranGaji",
				"kasKecil", "kasBesar" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenggunaanAnggaran.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Proses Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				if (start.getValue() == null || end.getValue() == null) {
					return; // Mencegah null pointer exception
				}

				// 1. Ekstrak nilai UI ke variabel final SEBELUM masuk ke Background Thread.
				final String startDateStr = Common.databaseDateFormat.get().format(start.getValue());
				final String endDateStr = Common.databaseDateFormat.get().format(end.getValue());

				final boolean isUangMuka = tampilkanUangMuka.isChecked();
				final boolean isPengadaan = tampilkanPermintaanPengadaanMasterAssetDetail.isChecked();
				final boolean isPembayaranGaji = tampilkanPembayaranGaji.isChecked();
				final boolean isKasKecil = tampilkanKasKecil.isChecked();
				final boolean isKasBesar = tampilkanKasBesar.isChecked();
				final boolean isJurnalUmum = tampilkanJurnalUmum.isChecked();
				final boolean isSaldoAwal = tampilkanSaldoAwalMasterAssetDetail.isChecked();
				final boolean isPertangungjawaban = tampilkanPertangungjawaban.isChecked();
				final boolean isSearchAktif = searchaktif.isChecked();

				// 2. Dialog Konfirmasi
				MyMessageboxConfig.show(
						"Apakah Anda yakin ingin melakukan Proses Ulang? Tindakan ini mungkin membutuhkan waktu beberapa saat.",
						"Konfirmasi Proses", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event ev) throws Exception {
								int response = Integer.parseInt(ev.getData().toString());
								if (response == MyMessageboxConfig.OK) {

									// 3. Persiapan ZK Server Push & Progress Bar
									final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
									if (!desktop.isServerPushEnabled()) {
										desktop.enableServerPush(true);
									}

									final Label label = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(null);
												}
											});
										}
									});

									// 4. Jalankan Background Thread
									new Thread(new Runnable() {

										// Helper untuk update UI
										private void updateProgress(final org.zkoss.zk.ui.Desktop desktop,
												final Label label, final int percent, final String message) {
											try {
												org.zkoss.zk.ui.Executions.schedule(desktop, new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {
														if (label != null) {
															label.setValue("Loading... " + percent + "% (" + message + ")");
														}
													}
												}, null);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
										}

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											Session session = null;
											try {
												// PROTEKSI UTAMA: Wajib pakai openSession()
												session = HibernateUtil.getSessionFactory().openSession();

												updateProgress(desktop, label, 0, "Mengambil data rantai anggaran...");

												// ====== 1. UANG MUKA ======
												if (isUangMuka) {
													updateProgress(desktop, label, 10, "Memproses Uang Muka...");
													List<UangMuka> uangMukas = session.createCriteria(UangMuka.class)
															.add(Restrictions.isNotNull("workspace"))
															.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < uangMukas.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(uangMukas.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:258");} }
													}
													uangMukas.clear(); // Bersihkan RAM
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:261");}
												}

												// ====== 2. PENGADAAN ======
												if (isPengadaan) {
													updateProgress(desktop, label, 20, "Memproses Pengadaan Master Asset...");
													List<PermintaanPengadaanMasterAssetDetail> details = session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
															.createAlias("permintaanPengadaanMasterAsset", "permintaanPengadaanMasterAsset")
															.add(Restrictions.isNotNull("permintaanPengadaanMasterAsset.workspace"))
															.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("permintaanPengadaanMasterAsset.aktif"), Restrictions.eq("permintaanPengadaanMasterAsset.aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < details.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(details.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:276");} }
													}
													details.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:279");}
												}

												// ====== 3. GAJI ======
												if (isPembayaranGaji) {
													updateProgress(desktop, label, 30, "Memproses Pembayaran Gaji...");
													List<PembayaranGaji> pembayaranGajis = session.createCriteria(PembayaranGaji.class)
															.add(Restrictions.isNotNull("workspace"))
															.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < pembayaranGajis.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(pembayaranGajis.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:293");} }
													}
													pembayaranGajis.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:296");}
												}

												// ====== 4. KAS KECIL ======
												if (isKasKecil) {
													updateProgress(desktop, label, 40, "Memproses Kas Kecil...");
													List<KasKecil> kasKecils = session.createCriteria(KasKecil.class)
															.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < kasKecils.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(kasKecils.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:309");} }
													}
													kasKecils.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:312");}
												}

												// ====== 5. KAS BESAR ======
												if (isKasBesar) {
													updateProgress(desktop, label, 50, "Memproses Kas Besar...");
													List<KasBesar> kasBesars = session.createCriteria(KasBesar.class)
															.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < kasBesars.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(kasBesars.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:325");} }
													}
													kasBesars.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:328");}
												}

												// ====== 6. JURNAL UMUM ======
												if (isJurnalUmum) {
													updateProgress(desktop, label, 60, "Memproses Jurnal Umum...");
													List<GrupTransaksi> grupTransaksis = session.createCriteria(GrupTransaksi.class)
															.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
															.add(Restrictions.isNotNull("workspace"))
															.add(Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.list();

													for (int i = 0; i < grupTransaksis.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(grupTransaksis.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:342");} }
													}
													grupTransaksis.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:345");}
												}

												// ====== 7. SALDO AWAL ======
												if (isSaldoAwal) {
													updateProgress(desktop, label, 70, "Memproses Saldo Awal Asset...");
													List<SaldoAwalMasterAssetDetail> saldoAwalDetails = session.createCriteria(SaldoAwalMasterAssetDetail.class)
															.createAlias("saldoAwal", "saldoAwal")
															.add(Restrictions.isNotNull("workspace"))
															.add(Restrictions.sqlRestriction("date(tanggal_pembuatan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("saldoAwal.aktif"), Restrictions.eq("saldoAwal.aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < saldoAwalDetails.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(saldoAwalDetails.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:360");} }
													}
													saldoAwalDetails.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:363");}
												}

												// ====== 8. PERTANGUNGJAWABAN ======
												if (isPertangungjawaban) {
													updateProgress(desktop, label, 80, "Memproses Pertangungjawaban...");
													List<Pertangungjawaban> pertangungjawabans = session.createCriteria(Pertangungjawaban.class)
															.createAlias("uangMuka", "uangMuka")
															.add(Restrictions.isNotNull("uangMuka.workspace"))
															.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('" + startDateStr + "') and date('" + endDateStr + "')"))
															.add(isSearchAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
															.list();

													for (int i = 0; i < pertangungjawabans.size(); i++) {
														PenggunaanAnggaran.prosesSimpan(pertangungjawabans.get(i), session);
														if (i > 0 && i % 50 == 0) { try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:378");} }
													}
													pertangungjawabans.clear();
																	try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:381");}
												}

												// ====== 9. VALIDASI AKHIR ======
												updateProgress(desktop, label, 95, "Validasi Akhir (Pembersihan Duplikasi)...");

												Transaction tx = session.beginTransaction();
												try {
													for (int ii = 0; ii < 20; ii++) {
														session.createSQLQuery("delete from rab.penggunaan_anggaran where id in (select max(id) from rab.penggunaan_anggaran group by ref having count(*)>1);")
																.executeUpdate();
													}
													tx.commit();
												} catch (Exception exDel) {
													if (tx != null && tx.isActive()) tx.rollback();
													throw exDel;
												}

											} catch (Exception e) {
												try {
													if (session != null && session.getTransaction() != null
															&& session.getTransaction().isActive()) {
														session.getTransaction().rollback();
													}
												} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:405");
												}
												try {
													if (session != null) {
														session.clear();
													}
												} catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/rab/PenggunaanAnggaranAction.java:411");
												}
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {
												// Tutup Label UI
												try {
													org.zkoss.zk.ui.Executions.schedule(desktop, new EventListener() {
														@Override
														public void onEvent(Event event) throws Exception {
															if (label != null) {
																label.setValue(""); 
															}
														}
													}, null);
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												// PENUTUPAN SESSION AMAN & KETAT
												if (session != null && session.isOpen()) {
													try { session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
													try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
													try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
												}
											}
										}
									}).start();
								}
							}
						});
			}
		});
		// add bisa null (komponen "add" tidak ada / belum ter-autowire), maka jangan deref
		// add.getParent() langsung -> NPE. Pakai helper null-safe yang juga dipakai untuk
		// tombol cetak di atas: ia mencari toolbar dari fellow "find"/"add" bila add null.
		Common.appendKeToolbar(button, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class PenggunaanAnggaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenggunaanAnggaran penggunaanAnggaran = (PenggunaanAnggaran) arg1;

			new Label(penggunaanAnggaran.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(PenggunaanAnggaran.class, penggunaanAnggaran, penggunaanAnggaran.getNama()).setParent(arg0);

			new Label(penggunaanAnggaran.getWaktu() == null ? "" : Common.dateFormat5.get().format(penggunaanAnggaran.getWaktu())).setParent(arg0);

			new Label(penggunaanAnggaran.getWorkspace() == null || penggunaanAnggaran.getWorkspace().getHargaTotal() == null ? ""
							: Common.numberFormat.get().format(penggunaanAnggaran.getWorkspace().getHargaTotal())).setParent(arg0);

			new Label(penggunaanAnggaran.getNilai() == null ? "" : Common.numberFormat.get().format(penggunaanAnggaran.getNilai())).setParent(arg0);

			new Label(penggunaanAnggaran.getWorkspace() == null ? "" : penggunaanAnggaran.getWorkspace().getNama()).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(penggunaanAnggaran.getKeterangan()).setParent(a);

			if (penggunaanAnggaran.getDisposisiSop() != null) {
				A aa = new A();
				aa.setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + penggunaanAnggaran.getDisposisiSop().getKeterangan() + " ("
						+ penggunaanAnggaran.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penggunaanAnggaran.getDisposisiSop().getId(), null, null, true, arg0.getTarget());
					}
				});
			}

			// Pengecekan NullPointerException yang aman
			if (penggunaanAnggaran.getPertangungjawaban() != null && penggunaanAnggaran.getPertangungjawaban().getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(penggunaanAnggaran.getPertangungjawaban().getDaftarPengajuanTransfer(), a);
			} else if (penggunaanAnggaran.getUangMuka() != null && penggunaanAnggaran.getUangMuka().getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(penggunaanAnggaran.getUangMuka().getDaftarPengajuanTransfer(), a);
			} else if (penggunaanAnggaran.getKasBesar() != null && penggunaanAnggaran.getKasBesar().getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(penggunaanAnggaran.getKasBesar().getDaftarPengajuanTransfer(), a);
			} else if (penggunaanAnggaran.getKasKecil() != null && penggunaanAnggaran.getKasKecil().getPenggantianKasKecil() != null
					&& penggunaanAnggaran.getKasKecil().getPenggantianKasKecil().getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(penggunaanAnggaran.getKasKecil().getPenggantianKasKecil().getDaftarPengajuanTransfer(), a);
			}

			new Label(penggunaanAnggaran.getAktif() != null && penggunaanAnggaran.getAktif() ? "Ya" : "Tidak").setParent(arg0);
		}
	}

	// Helper Method untuk menjaga DataCriteria interface aman
	public Criteria initCriteria(Session session, boolean order) {
		Criterion criterion = Restrictions.sqlRestriction("false");

		if (tampilkanJurnalUmum.isChecked() && tampilkanPermintaanPengadaanMasterAssetDetail.isChecked()
				&& tampilkanUangMuka.isChecked() && tampilkanSaldoAwalMasterAssetDetail.isChecked()
				&& tampilkanPembayaranGaji.isChecked() && tampilkanKasKecil.isChecked() && tampilkanKasBesar.isChecked()
				&& tampilkanPertangungjawaban.isChecked()) {
			criterion = Restrictions.sqlRestriction("true");
		} else {
			if (tampilkanJurnalUmum.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("grupTransaksi"));
			if (tampilkanPermintaanPengadaanMasterAssetDetail.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("permintaanPengadaanMasterAssetDetail"));
			if (tampilkanUangMuka.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("uangMuka"));
			if (tampilkanPertangungjawaban.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("pertangungjawaban"));
			if (tampilkanSaldoAwalMasterAssetDetail.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("saldoAwalMasterAssetDetail"));
			if (tampilkanPembayaranGaji.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("pembayaranGaji"));
			if (tampilkanKasKecil.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("kasKecil"));
			if (tampilkanKasBesar.isChecked()) criterion = Restrictions.or(criterion, Restrictions.isNotNull("kasBesar"));
		}

		Workspace workspace = (Workspace) searchAnggaran.getAttribute("workspace");
		Criteria criteria = session.createCriteria(PenggunaanAnggaran.class);

		if (workspace != null && workspace.getId() != null) {
			List<Long> workspaceIds = Workspace.getAllWorkspaceIds(session, workspace.getId());
			if (workspaceIds != null && !workspaceIds.isEmpty()) {
				criteria.add(Restrictions.in("workspace.id", workspaceIds));
			} else {
				criteria.add(Restrictions.eq("workspace", workspace));
			}
		} else {
			criteria.add(Restrictions.sqlRestriction("true"));
		}

		criteria.add(criterion);

		criteria.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"));

		if (start.getValue() != null && end.getValue() != null) {
			String startDateStr = Common.databaseDateFormat.get().format(start.getValue());
			String endDateStr = Common.databaseDateFormat.get().format(end.getValue());
			criteria.add(Restrictions.sqlRestriction("date(this_.waktu) between date('" + startDateStr + "') and date('" + endDateStr + "')"));
		}

		if (order) criteria.addOrder(Order.desc("waktu"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	// Fallback wajib karena implement DataCriteria, tapi akan kita pass nilai null
	// karena pemanggilan asli harus menyertakan Session
	public Criteria initCriteria(boolean order) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			
			Common.initPaging(initCriteria(session, false), paging);

			List<PenggunaanAnggaran> penggunaanAnggaran = initCriteria(session, true)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel strset = new SimpleListModel(penggunaanAnggaran);
			grid.setRowRenderer(new PenggunaanAnggaranRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}
}