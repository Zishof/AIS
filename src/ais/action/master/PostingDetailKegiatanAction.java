package ais.action.master;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKegiatan;
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
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PostingDetailKegiatanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
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

	        FilterLanjutHelper.setup(comp);
}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<DetailKegiatan> detailKegiatans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (DetailKegiatan detailKegiatan : detailKegiatans) {
								detailKegiatan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(detailKegiatan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where detail_kegiatan="
												+ detailKegiatan.getId() + " and closing is null")
										.executeUpdate();
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
				// if (keterangan.getValue().trim().equals("")) {
				// MyMessageboxConfig.show("Keterangan harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pembayaran ?", "Pertanyaan",
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

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_MAHASISWA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<DetailKegiatan> detailKegiatans = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											SatuanKerja s = (SatuanKerja) PostingDetailKegiatanAction.this.satuanKerja
													.getAttribute("satuanKerja");

											int rowIndex = 1;
											for (DetailKegiatan detailKegiatan : detailKegiatans) {

												SatuanKerja satuanKerja = (SatuanKerja) (detailKegiatan
														.getKegiatan() != null
														&& detailKegiatan.getKegiatan().getMahasiswa() != null
														&& detailKegiatan.getKegiatan().getMahasiswa()
																.getJurusan() != null
														&& detailKegiatan.getKegiatan().getMahasiswa().getJurusan()
																.getFakultas() != null
														&& detailKegiatan.getKegiatan().getMahasiswa().getJurusan()
																.getFakultas().getSatuanKerja() != null
																		? detailKegiatan.getKegiatan().getMahasiswa()
																				.getJurusan().getFakultas()
																				.getSatuanKerja()
																		: s);

												if (detailKegiatan.getKegiatan() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa()
																.getProdiLulus() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa()
																.getProdiLulus().getFakultas() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa()
																.getProdiLulus().getFakultas()
																.getSatuanKerja() != null) {
													satuanKerja = detailKegiatan.getKegiatan().getCalonMahasiswa()
															.getProdiLulus().getFakultas().getSatuanKerja();
												} else if (detailKegiatan.getKegiatan() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa()
																.getProdiLulus() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1()
																.getFakultas() != null
														&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1()
																.getFakultas().getSatuanKerja() != null) {
													satuanKerja = detailKegiatan.getKegiatan().getCalonMahasiswa()
															.getProdi1().getFakultas().getSatuanKerja();
												}

												if (s != null) {
													satuanKerja = s;
												}

												if (detailKegiatan != null && detailKegiatan.getItemBiaya() != null) {

													try {
														Akun akunDebet = detailKegiatan.getItemBiaya()
																.ambilPiutang(detailKegiatan.getKegiatan());
														Akun akunKredit = detailKegiatan.getItemBiaya()
																.ambilAkun(detailKegiatan.getKegiatan());
														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																Mahasiswa mahasiswa = (detailKegiatan.getKegiatan()
																		.getMahasiswa() == null
																				? (detailKegiatan.getKegiatan()
																						.getCalonMahasiswa() == null
																								? null
																								: detailKegiatan
																										.getKegiatan()
																										.getCalonMahasiswa()
																										.getMahasiswa())
																				: detailKegiatan.getKegiatan()
																						.getMahasiswa());

																ket = "Piutang "
																		+ (mahasiswa == null
																				? detailKegiatan.getKegiatan()
																						.getCalonMahasiswa()
																				: mahasiswa)
																		+ " - "
																		+ detailKegiatan.getItemBiaya().getNama()
																		+ " - " + detailKegiatan.getKeterangan();

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															if (detailKegiatan
																	.getPengaturanPembayaranBulanan() != null) {
																PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
																		.getPengaturanPembayaranBulanan();
																ket = pengaturanPembayaranBulanan.getKeterangan();
																ket = (ket.isEmpty()
																		? (pengaturanPembayaranBulanan.getDetailBiaya()
																				.getItemBiaya().getNama())
																		: ket) + ", bulan "
																		+ pengaturanPembayaranBulanan.getNamaBulan();

																try {
																	Mahasiswa mahasiswa = (detailKegiatan.getKegiatan()
																			.getMahasiswa() == null
																					? (detailKegiatan.getKegiatan()
																							.getCalonMahasiswa() == null
																									? null
																									: detailKegiatan
																											.getKegiatan()
																											.getCalonMahasiswa()
																											.getMahasiswa())
																					: detailKegiatan.getKegiatan()
																							.getMahasiswa());

																	ket = "Piutang " + (mahasiswa == null
																			? detailKegiatan.getKegiatan()
																					.getCalonMahasiswa()
																			: mahasiswa) + " " + ket;
																} catch (Exception e) {
																	Common.tampilErrorJikaAdmin(e);
																}
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(
																			rowIndex * 100.0 / detailKegiatans.size())
																	+ " %)");

															Double nilai = detailKegiatan.getBiaya();
															try {

																ItemBiaya itemDenda = null;
																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;
																if (denda != null && denda > 0.1) {
																	itemDenda = ConstantValues.DENDA;
																	akunDenda = itemDenda == null ? null
																			: itemDenda.ambilPiutang(
																					detailKegiatan.getKegiatan());
																}

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			detailKegiatan.getTanggal(), nilai, denda,
																			detailKegiatan, satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			detailKegiatan.getTanggal(), nilai, denda,
																			detailKegiatan, satuanKerja, session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
																// Jurnal GAGAL dibuat untuk item ini -> JANGAN ditandai "posted"; batalkan lalu
																// lanjut ke item berikutnya (item lain tetap diproses, tidak terpengaruh).
																try {
																	if (session.getTransaction() != null && session.getTransaction().isActive()) {
																		session.getTransaction().rollback();
																	}
																} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingDetailKegiatanAction.java:517");
																}
																continue;
															}

															detailKegiatan.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															Common.refreshUpdate(session, detailKegiatan);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PostingDetailKegiatanAction.java:527");
														// TODO: handle
														// exception
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

	class DetailKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailKegiatan detailKegiatan = (DetailKegiatan) arg1;
			final Kegiatan kegiatan = detailKegiatan.getKegiatan();

			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(DetailKegiatan.class, detailKegiatan,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

				if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
							.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(detailKegiatan.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(
						detailKegiatan.getBiaya() == null ? "0" : Common.numberFormat.get().format(detailKegiatan.getBiaya()))
						.setParent(arg0);

				// new Label(kegiatan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(kegiatan.getAmount()))
				// .setParent(arg0);
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
						.createNewRevisi(DetailKegiatan.class, detailKegiatan,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

				if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
							.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(detailKegiatan.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(
						detailKegiatan.getBiaya() == null ? "0" : Common.numberFormat.get().format(detailKegiatan.getBiaya()))
						.setParent(arg0);

				// new Label(kegiatan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(kegiatan.getAmount()))
				// .setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						? ""
						: kegiatan.getCalonMahasiswa().getProdiLulus().getNama()).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						|| kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas() == null ? ""
								: kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas().getNama())
						.setParent(arg0);
			}
			new Label(kegiatan.getSemster() + "").setParent(arg0);
			new Label(Common.dateFormat3.get().format(detailKegiatan.getTanggal())).setParent(arg0);
			// new Label(kegiatan.getJenisKegiatan() == null ? "" :
			// kegiatan.getJenisKegiatan().getNamaKegiatan())
			// .setParent(arg0);
			Akun akunDebet = detailKegiatan.getItemBiaya().ambilPiutang(detailKegiatan.getKegiatan());
			Akun akunKredit = detailKegiatan.getItemBiaya().ambilAkun(detailKegiatan.getKegiatan());
			if (akunDebet != null && akunKredit != null) {
				Double nilai = detailKegiatan.getBiaya();

				ItemBiaya itemDenda = null;
				Akun akunDenda = null;
				Double denda = 0.0;
				if (denda != null && denda > 0.1) {
					itemDenda = ConstantValues.DENDA;
					akunDenda = itemDenda == null ? null : itemDenda.ambilPiutang(detailKegiatan.getKegiatan());
				}

				if (denda != null && denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan. "
							+ CommonAkunting.hintPemetaanItemBiaya(itemDenda, detailKegiatan.getKegiatan(),
									"Akun Piutang"))
							.setParent(arg0);
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
				// Jurnal akrual: debet = Akun Piutang, kredit = Akun Pendapatan — keduanya
				// dari pemetaan ItemBiaya (bukan Jenis Pembayaran seperti Posting Pembayaran).
				new Label(CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.hintPemetaanItemBiaya(detailKegiatan.getItemBiaya(), detailKegiatan.getKegiatan(),
								"Akun Piutang"),
						CommonAkunting.hintPemetaanItemBiaya(detailKegiatan.getItemBiaya(), detailKegiatan.getKegiatan(),
								"Akun Pendapatan")))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("detailKegiatan", detailKegiatan)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(detailKegiatan.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: detailKegiatan.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan Posting", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && detailKegiatan.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								detailKegiatan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(detailKegiatan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where detail_kegiatan="
												+ detailKegiatan.getId() + " and closing is null")
										.executeUpdate();

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
				button.setVisible(edit && detailKegiatan.getPostingHistory() == null && tbmuser != null);
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

								Akun akunDebet = detailKegiatan.getItemBiaya()
										.ambilPiutang(detailKegiatan.getKegiatan());
								Akun akunKredit = detailKegiatan.getItemBiaya().ambilAkun(detailKegiatan.getKegiatan());
								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {
										Mahasiswa mahasiswa = (detailKegiatan.getKegiatan().getMahasiswa() == null
												? (detailKegiatan.getKegiatan().getCalonMahasiswa() == null ? null
														: detailKegiatan.getKegiatan().getCalonMahasiswa()
																.getMahasiswa())
												: detailKegiatan.getKegiatan().getMahasiswa());

										ket = "Piutang "
												+ (mahasiswa == null ? detailKegiatan.getKegiatan().getCalonMahasiswa()
														: mahasiswa)
												+ " - " + detailKegiatan.getItemBiaya().getNama() + " - "
												+ detailKegiatan.getKeterangan();

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
												.getPengaturanPembayaranBulanan();
										ket = pengaturanPembayaranBulanan.getKeterangan();
										ket = (ket.isEmpty()
												? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
														.getNama())
												: ket) + ", bulan " + pengaturanPembayaranBulanan.getNamaBulan();

										try {
											Mahasiswa mahasiswa = (detailKegiatan.getKegiatan().getMahasiswa() == null
													? (detailKegiatan.getKegiatan().getCalonMahasiswa() == null ? null
															: detailKegiatan.getKegiatan().getCalonMahasiswa()
																	.getMahasiswa())
													: detailKegiatan.getKegiatan().getMahasiswa());

											ket = "Piutang " + (mahasiswa == null
													? detailKegiatan.getKegiatan().getCalonMahasiswa()
													: mahasiswa) + " " + ket;
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

									}
									Double nilai = detailKegiatan.getBiaya();

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (detailKegiatan.getKegiatan() != null
											&& detailKegiatan.getKegiatan().getMahasiswa() != null
											&& detailKegiatan.getKegiatan().getMahasiswa().getJurusan() != null
											&& detailKegiatan.getKegiatan().getMahasiswa().getJurusan()
													.getFakultas() != null
											&& detailKegiatan.getKegiatan().getMahasiswa().getJurusan().getFakultas()
													.getSatuanKerja() != null
															? detailKegiatan.getKegiatan().getMahasiswa().getJurusan()
																	.getFakultas().getSatuanKerja()
															: tbmuser.ambilSatuanKerja());

									if (detailKegiatan.getKegiatan() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdiLulus() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdiLulus()
													.getFakultas() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdiLulus()
													.getFakultas().getSatuanKerja() != null) {
										satuanKerja = detailKegiatan.getKegiatan().getCalonMahasiswa().getProdiLulus()
												.getFakultas().getSatuanKerja();
									} else if (detailKegiatan.getKegiatan() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdiLulus() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1()
													.getFakultas() != null
											&& detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1()
													.getFakultas().getSatuanKerja() != null) {
										satuanKerja = detailKegiatan.getKegiatan().getCalonMahasiswa().getProdi1()
												.getFakultas().getSatuanKerja();
									}

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, detailKegiatan.getTanggal(),
												nilai, denda, detailKegiatan, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, detailKegiatan.getTanggal(),
												nilai, denda, detailKegiatan, satuanKerja, session);
									}

									detailKegiatan.setPostingHistory(postingHistory);
									Common.refreshUpdate(session, detailKegiatan);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(edit && detailKegiatan.getPostingHistory() == null);
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
											try {

												Common.refreshDelete(detailKegiatan);

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadDataDenganProgressPosting(null);
													}
												});
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

		Criteria criteria = session.createCriteria(DetailKegiatan.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.sqlRestriction(
						"this_.item_biaya in (select item_biaya from item_biaya_punya_piutang where akun is not null and item_biaya is not null group by item_biaya)"))

				.add(Restrictions.isNotNull("tanggal"))
				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("biaya", 0.0)))

				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("biaya")))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiaya", searchitembiaya.getSelectedItem().getValue()))

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
								Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(),
														MatchMode.ANYWHERE),
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

		List<DetailKegiatan> detailKegiatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(detailKegiatan);
		grid.setRowRenderer(new DetailKegiatanRenderer());
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

}
