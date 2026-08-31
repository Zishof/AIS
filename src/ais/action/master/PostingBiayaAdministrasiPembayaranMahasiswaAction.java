package ais.action.master;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
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
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PostingBiayaAdministrasiPembayaranMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Combobox searchjenispembayaran;
	private Decimalbox searchtahun;
	private Combobox searchjenjang;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Tbmuser tbmuser;

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

		if (tglMulai != null) tglMulai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());

		tbmuser = Common.getCurrentUser();

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Common.insertCombo(searchjenispembayaran, new String[] { "namaKegiatan", "kode" }, JenisKegiatan.class,
				Restrictions.eq("aktif", true));

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
	        FilterLanjutHelper.setup(comp);
}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting semua biaya administrasi pembayaran ?",
			"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
			new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					int i = Integer.parseInt(event.getData().toString());
					if (i == MyMessageboxConfig.OK) {
						List<LogPembayaran> logPembayarans = initCriteria(true)
							.add(Restrictions.isNotNull("postingHistory")).list();
						for (LogPembayaran logPembayaran : logPembayarans) {
							logPembayaran.setPostingHistory(null);
							Common.refreshSaveOrUpdate(logPembayaran);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Transaksi"));
		// Hbox hbox = new Hbox();
		// row.appendChild(hbox);
		// final MyDatebox mulai;
		// hbox.appendChild(mulai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		// mulai.setReadonly(true);
		// hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		// final MyDatebox sampai;
		// hbox.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		// sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		final AmbilDataSatuanKerjaBanbox satuanKerja;
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting biaya admin pembayaran ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									final Tbmuser tbmuser = Common.getCurrentUser();
									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show(
													"Posting biaya admin pembayaran mahasiswa berhasil dilakukan",
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

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {
											Session session = HibernateUtil.currentNativeSession();
											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_MAHASISWA);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setTanggal(tgl);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<LogPembayaran> logPembayarans = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();
											int rowIndex = 1;
											for (LogPembayaran logPembayaran : logPembayarans) {
												if (logPembayaran != null) {

													Akun[] akuns = populateAkun(session, logPembayaran);
													Akun akunDebet = akuns[0];
													Akun akunKredit = akuns[1];

													if (akunDebet != null && akunKredit != null) {
														Boolean apakahUangMasuk = true;
														String ket = "Biaya administrasi "
																+ ((logPembayaran.getKegiatan().getMahasiswa() == null
																		? logPembayaran.getKegiatan()
																				.getCalonMahasiswa()
																		: logPembayaran.getKegiatan().getMahasiswa()))
																+ " untuk pembayaran "
																+ logPembayaran.getKegiatan().getJenisKegiatan()
																		.getNamaKegiatan()
																+ " pada waktu "
																+ Common.dateFormat3.get().format(logPembayaran.getTanggal());

														label.setValue(ket + " ("
																+ Common.numberFormat.get().format(
																		rowIndex * 100.0 / logPembayarans.size())
																+ " %)");

														Double nilai = logPembayaran.getBiayaAdministrasi();
														try {
															session.getTransaction().begin();
															CommonAkunting
																	.saveTransaksi(akunDebet, akunKredit, null, null,
																			postingHistory, apakahUangMasuk, ket,
																			logPembayaran.getTanggal(), nilai, 0.0,
																			logPembayaran,
																			(SatuanKerja) satuanKerja
																					.getAttribute("satuanKerja"),
																			session);
															session.getTransaction().commit();
														} catch (Exception e) {
															// TODO
															// Auto-generated
															// catch
															// block
															Common.tampilErrorJikaAdmin(e);
															// Jurnal GAGAL dibuat untuk item ini -> JANGAN ditandai "posted"; batalkan lalu
															// lanjut ke item berikutnya (item lain tetap diproses, tidak terpengaruh).
															try {
																if (session.getTransaction() != null && session.getTransaction().isActive()) {
																	session.getTransaction().rollback();
																}
															} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingBiayaAdministrasiPembayaranMahasiswaAction.java:395");
															}
															continue;
														}

														session.getTransaction().begin();
														logPembayaran.setPostingHistory(postingHistory);
														session.update(logPembayaran);
														session.getTransaction().commit();
													}

												}
												rowIndex++;
											}

											label.setValue("");
											HibernateUtil.closeSession();
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
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	// Dipakai BERSAMA oleh tombol layar ZK dan mesin non-ZK di bawah; hanya
	// memakai parameternya sendiri, jadi aman dijadikan static.
	static Akun[] populateAkun(Session session, LogPembayaran logPembayaran) {
		Akun akunDebet = null;
		Akun akunKredit = null;

		if (logPembayaran.getFaspayRequest() != null) {
			String kodeAkun = Common.getKonfigurasi("kode_akun_faspay", "").getNilai();
			String kodeAkunKredit = Common.getKonfigurasi("kode_akun_faspay_biaya_administrasi", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);
			akunDebet = jenisPembayaran.getAkun();
			akunKredit = kodeAkunKredit.trim().isEmpty() ? null
					: (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", kodeAkunKredit))
							.setMaxResults(1).uniqueResult();
		} else if (logPembayaran.getBniRequest() != null) {
			String kodeAkun = Common.getKonfigurasi("kode_akun_bni", "").getNilai();
			String kodeAkunKredit = Common.getKonfigurasi("kode_akun_bni_biaya_administrasi", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);
			akunDebet = jenisPembayaran.getAkun();
			akunKredit = kodeAkunKredit.trim().isEmpty() ? null
					: (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", kodeAkunKredit))
							.setMaxResults(1).uniqueResult();
		} else if (logPembayaran.getJatelindoRequest() != null) {
			String kodeAkun = Common.getKonfigurasi("kode_akun_jatelindo", "").getNilai();
			String kodeAkunKredit = Common.getKonfigurasi("kode_akun_jatelindo_biaya_administrasi", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);
			akunDebet = jenisPembayaran.getAkun();
			akunKredit = kodeAkunKredit.trim().isEmpty() ? null
					: (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", kodeAkunKredit))
							.setMaxResults(1).uniqueResult();
		} else {
			String kodeAkunKredit = Common.getKonfigurasi("kode_akun_manual_biaya_administrasi", "").getNilai();
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			akunDebet = jenisPembayaranDefault == null ? null : jenisPembayaranDefault.getAkun();
			akunKredit = kodeAkunKredit.trim().isEmpty() ? null
					: (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", kodeAkunKredit))
							.setMaxResults(1).uniqueResult();
		}

		return new Akun[] { akunDebet, akunKredit };
	}

	class LogPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LogPembayaran logPembayaran = (LogPembayaran) arg1;
			final Kegiatan kegiatan = logPembayaran.getKegiatan();

			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(LogPembayaran.class, kegiatan,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

				new Label(logPembayaran.getBiayaAdministrasi() == null ? "0"
						: Common.numberFormat.get().format(logPembayaran.getBiayaAdministrasi())).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
						: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null
						|| kegiatan.getMahasiswa().getJurusan().getFakultas() == null ? ""
								: kegiatan.getMahasiswa().getJurusan().getFakultas().getNama())
						.setParent(arg0);
			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNim())
						.setParent(arg0);
				RevisiHelper
						.createNewRevisi(LogPembayaran.class, kegiatan,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

				new Label(logPembayaran.getBiayaAdministrasi() == null ? "0"
						: Common.numberFormat.get().format(logPembayaran.getBiayaAdministrasi())).setParent(arg0);

				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						? ""
						: kegiatan.getCalonMahasiswa().getProdiLulus().getNama()).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						|| kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas() == null ? ""
								: kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas().getNama())
						.setParent(arg0);
			}
			new Label(kegiatan.getSemster() + "").setParent(arg0);
			new Label(Common.dateFormat3.get().format(logPembayaran.getTanggal())).setParent(arg0);
			new Label(kegiatan.getValidator()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			Akun[] akuns = populateAkun(session, logPembayaran);
			Akun akunDebet = akuns[0];
			Akun akunKredit = akuns[1];
			if (akunDebet != null && akunKredit != null) {
				Double nilai = logPembayaran.getBiayaAdministrasi();

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				// Akun debet/kredit di sini di-resolve dari Konfigurasi kode akun per gateway
				// (populateAkun) — BUKAN pemetaan ItemBiaya, jadi langkah perbaikannya beda.
				String kodeParamKredit = logPembayaran.getFaspayRequest() != null ? "kode_akun_faspay_biaya_administrasi"
						: logPembayaran.getBniRequest() != null ? "kode_akun_bni_biaya_administrasi"
						: logPembayaran.getJatelindoRequest() != null ? "kode_akun_jatelindo_biaya_administrasi"
						: "kode_akun_manual_biaya_administrasi";
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.langkahLengkapiKolomAkun("Jenis Pembayaran",
								"Jenis Pembayaran default (atau sesuai gateway yang dipakai)", "Akun"),
						CommonAkunting.langkahIsiKonfigurasiAkun(kodeParamKredit,
								"kode Akun kredit utk biaya administrasi transaksi ini")));
			}

			new Label(logPembayaran.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: logPembayaran.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan Posting", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && logPembayaran.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								logPembayaran.setPostingHistory(null);
								Common.refreshSaveOrUpdate(logPembayaran);

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
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Posting", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && logPembayaran.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun[] akuns = populateAkun(session, logPembayaran);
								Akun akunDebet = akuns[0];
								Akun akunKredit = akuns[1];
								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;
									String ket = "Biaya administrasi "
											+ ((logPembayaran.getKegiatan().getMahasiswa() == null
													? logPembayaran.getKegiatan().getCalonMahasiswa()
													: logPembayaran.getKegiatan().getMahasiswa()))
											+ " untuk pembayaran "
											+ logPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan()
											+ " pada waktu " + Common.dateFormat3.get().format(logPembayaran.getTanggal());
									Double nilai = logPembayaran.getBiayaAdministrasi();
									CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
											apakahUangMasuk, ket, logPembayaran.getTanggal(), nilai, 0.0, logPembayaran,
											tbmuser.ambilSatuanKerja(), session);

									logPembayaran.setPostingHistory(postingHistory);
									session.update(logPembayaran);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(LogPembayaran.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.add(Restrictions.gt("biayaAdministrasi", 0.1))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistory")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.createAlias("kegiatan", "kegiatan")

				.add(searchjenispembayaran.getSelectedItem() == null
						|| searchjenispembayaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kegiatan.jenisKegiatan",
										searchjenispembayaran.getSelectedItem().getValue()))

				.add((jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: (jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("kegiatan.semster", Common.genap)
								: Restrictions.in("kegiatan.semster", Common.ganjil))));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.eq("prodiLulus.fakultas", fakultas)))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<LogPembayaran> logPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(logPembayaran);
		grid.setRowRenderer(new LogPembayaranRenderer());
		grid.setModelCheckMobile(strset);

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
	//
	// Pemilihan akun per kanal pembayaran TIDAK disalin: {@link #populateAkun}
	// dipakai bersama dengan tombol di layar.
	// =====================================================================

	/**
	 * Kriteria log pembayaran ber-biaya administrasi pembayaran mahasiswa pada rentang tanggal -- sama dengan penghitung
	 * baris "Mahasiswa - Biaya Administrasi" pada dasbor
	 * ({@code DraftJurnalRingkasanUtil.kriteriaLogPembayaran}).
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(LogPembayaran.class)
				.add(Restrictions.gt("biayaAdministrasi", 0.1));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA biaya administrasi pembayaran mahasiswa dalam rentang.
	 *
	 * <p>Layar hanya melepas penandanya tanpa menghapus jurnal apa pun. Di sini jurnalnya
	 * IKUT dihapus ({@code akunting.transaksi} lebih dulu, lalu {@code grup_transaksi} yang
	 * merujuk log pembayaran ini dan belum closing) -- membatalkan posting tetapi meninggalkan
	 * jurnalnya membuat buku besar tidak lagi cocok dengan daftar draft.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<LogPembayaran> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (LogPembayaran log : daftar) {
				try {
					// Satu LogPembayaran memikul DUA kaki jurnal pada kolom referensi yang sama
					// (biaya administrasi di sini, biaya payment gateway di action sebelahnya).
					// Keduanya menulis ref null dan jenis riwayat yang sama, jadi satu-satunya
					// pembeda adalah RIWAYAT yang ditunjuk capnya. Tanpa saringan posting_history
					// ini, membatalkan kaki administrasi ikut menghapus jurnal kaki payment
					// gateway padahal capnya tetap terpasang -- dokumen tampak terposting
					// sementara jurnalnya sudah lenyap.
					if (log.getPostingHistory() == null || log.getPostingHistory().getId() == null) {
						continue;
					}
					String syarat = "log_pembayaran=" + log.getId() + " and posting_history="
							+ log.getPostingHistory().getId() + " and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					log.setPostingHistory(null);
					session.update(log);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingBiayaAdministrasiPembayaranMahasiswaAction jalur API");
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
	 * Posting SEMUA biaya administrasi pembayaran mahasiswa yang belum dijurnal dalam rentang.
	 *
	 * <p>Akun debet/kredit ditentukan {@link #populateAkun} menurut kanal pembayarannya
	 * (Faspay, BNI, Jatelindo, dst.) lewat konfigurasi kode akun -- method yang SAMA dengan
	 * yang dipakai tombol di layar.</p>
	 *
	 * <p><b>Satuan kerja.</b> Layar mengambilnya dari komponen penyaring di halaman; dari API
	 * komponen itu tidak ada, jadi dipakai satuan kerja PENGGUNA yang memposting -- sumber yang
	 * sama dengan tombol posting per-baris di layar ini.</p>
	 *
	 * @return jumlah dokumen yang BERHASIL diposting.
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
			postingHistory.setKeterangan("Posting massal biaya administrasi pembayaran mahasiswa dari dasbor jurnal"
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
					LogPembayaran log = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.idEq(id)).uniqueResult();
					if (log == null || log.getKegiatan() == null) {
						continue;
					}
					Akun[] akuns = populateAkun(session, log);
					if (akuns == null || akuns[0] == null || akuns[1] == null) {
						// Kode akun kanal pembayarannya belum diatur: dilewati, bukan ditandai.
						continue;
					}
					String ket = "Biaya administrasi "
							+ (log.getKegiatan().getMahasiswa() == null ? log.getKegiatan().getCalonMahasiswa()
									: log.getKegiatan().getMahasiswa())
							+ " untuk pembayaran "
							+ (log.getKegiatan().getJenisKegiatan() == null ? ""
									: log.getKegiatan().getJenisKegiatan().getNamaKegiatan())
							+ " pada waktu " + Common.dateFormat3.get().format(log.getTanggal());
					Double nilai = log.getBiayaAdministrasi();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						CommonAkunting.saveTransaksi(akuns[0], akuns[1], null, null, postingHistory, true,
								ket, log.getTanggal(), nilai, Double.valueOf(0.0), log, satuanKerjaPengguna,
								session);
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingBiayaAdministrasiPembayaranMahasiswaAction jalur API");
					}

					if (tersimpan) {
						log.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(log);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingBiayaAdministrasiPembayaranMahasiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingBiayaAdministrasiPembayaranMahasiswaAction jalur API");
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
