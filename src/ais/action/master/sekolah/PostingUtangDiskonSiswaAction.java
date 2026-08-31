package ais.action.master.sekolah;

import java.util.Calendar;
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
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PostingUtangDiskonSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;
	private Textbox searchsiswa;
	private Combobox searchyayasan;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private Decimalbox searchbulan;
	private Decimalbox searchtahun;
	private Combobox searchsekolah;
	private Combobox searchitembiaya;
	private Decimalbox searchtahunMasuk;

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

		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglSampai != null) tglSampai.setValue(WaktuUtil.getDate());

		adminLain = Common.getApakahAdmin();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Tbmuser tbmuser = Common.getCurrentUser();

		Sekolah curr = tbmuser == null ? null : tbmuser.ambilSekolah();

		Common.insertComboDanSemua(searchitembiaya, new String[] { "nama", "kode" }, "keterangan", ItemBiayaSekolah.class,
				Restrictions.and(
						curr == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", curr)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

		);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi piutang ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Tagihan> tagihans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistoryDiskon")).list();

							for (Tagihan tagihan : tagihans) {
								tagihan.setPostingHistoryDiskon(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where tagihan="
												+ tagihan.getId() + " and jenis='"
												+ PostingHistory.JENIS_UTANG_DISKON_SISWA + "'" + " and closing is null")
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
		addWindow.setTitle("Posting Utang Siswa");
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

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
					MyMessageboxConfig.show("Tanggal harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi piutang ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting piutang siswa berhasil dilakukan",
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

											PostingHistory postingHistoryDiskon = new PostingHistory(
													PostingHistory.JENIS_UTANG_DISKON_SISWA);
											postingHistoryDiskon.setTanggal(tgl);
											postingHistoryDiskon.setTbmuser(tbmuser);
											postingHistoryDiskon.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistoryDiskon);
											session.getTransaction().commit();

											List<Tagihan> tagihans = initCriteria(true)
													.add(Restrictions.isNull("postingHistoryDiskon")).list();

											int rowIndex = 1;
											for (Tagihan tagihan : tagihans) {
												if (tagihan != null && tagihan.getItemBiayaSekolah() != null) {

													Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunUtangDiskon();
													Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunDiskon();

													if (akunDebet != null && akunKredit != null) {

														Akun akunDiskon = null;
														Akun akunUtangDiskon = null;
														Double diskonTidakLangsung = 0.0;

														Boolean apakahUangMasuk = true;
														String ket = "Utang diskon Tidak Langsung "
																+ ((tagihan.getSiswa() == null ? tagihan.getCalonSiswa()
																		: tagihan.getSiswa()))
																+ (tagihan.getTahunbulan() == null ? ""
																		: " tahun/bulan " + tagihan.getTahunbulan()
																				+ " - ")
																+ tagihan.getItemBiayaSekolah().getNama() + " - "
																+ tagihan.getKeterangan();

														label.setValue(
																ket + " ("
																		+ Common.numberFormat.get().format(
																				rowIndex * 100.0 / tagihans.size())
																		+ " %)");

														Double nilai = tagihan.getDiskonTidakLangsung();

														try {

															session.getTransaction().begin();
															if (nilai > 0.1) {
																CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																		akunDiskon, akunUtangDiskon,
																		postingHistoryDiskon, apakahUangMasuk, ket,
																		tagihan.getTanggalBayar(), nilai,
																		diskonTidakLangsung, tagihan,
																		tagihan.getPengaturanBiaya()
																				.getSekolah().getSatuanKerja(),
																		ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
															} else {
																CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																		akunDiskon, akunUtangDiskon,
																		postingHistoryDiskon, apakahUangMasuk, ket,
																		tagihan.getTanggalBayar(), nilai,
																		diskonTidakLangsung, tagihan,
																		tagihan.getPengaturanBiaya()
																				.getSekolah().getSatuanKerja(),
																		ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
															}
															session.getTransaction().commit();
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

														tagihan.setPostingHistoryDiskon(postingHistoryDiskon);
														session.getTransaction().begin();
														session.update(tagihan);
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

	class TagihanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Tagihan tagihan = (Tagihan) arg1;

			if (tagihan.getSiswa() != null) {

				CommonMedia.tampilkanGambarKecil(tagihan.getSiswa()).setParent(arg0);

				new Label(tagihan.getSiswa() == null ? "" : tagihan.getSiswa().getNomorInduk()).setParent(arg0);

				RevisiHelper.createNewRevisi(Tagihan.class, tagihan,
						tagihan.getSiswa() == null ? "" : tagihan.getSiswa().getNama()).setParent(arg0);

				new Label(tagihan.getItemBiayaSekolah().getNama()).setParent(arg0);

				new Label(tagihan.getDiskonTidakLangsung() == null ? "0"
						: Common.numberFormat.get().format(tagihan.getDiskonTidakLangsung())).setParent(arg0);

				// new Label(tagihan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(tagihan.getAmount()))
				// .setParent(arg0);
				new Label(tagihan.getSiswa() == null || tagihan.getSiswa().getSekolah() == null ? ""
						: tagihan.getSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(tagihan.getSiswa() == null || tagihan.getSiswa().getSekolah() == null
						|| tagihan.getSiswa().getSekolah().getYayasan() == null ? ""
								: tagihan.getSiswa().getSekolah().getYayasan().getNama())
						.setParent(arg0);
			} else if (tagihan.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(tagihan.getCalonSiswa()).setParent(arg0);

				new Label(tagihan.getCalonSiswa() == null ? "" : tagihan.getCalonSiswa().getNoRegistrasi())
						.setParent(arg0);
				RevisiHelper
						.createNewRevisi(Tagihan.class, tagihan,
								tagihan.getCalonSiswa() == null ? "" : tagihan.getCalonSiswa().getNama())
						.setParent(arg0);

				new Label(tagihan.getItemBiayaSekolah().getNama()).setParent(arg0);

				new Label(tagihan.getDiskonTidakLangsung() == null ? "0"
						: Common.numberFormat.get().format(tagihan.getDiskonTidakLangsung())).setParent(arg0);

				// new Label(tagihan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(tagihan.getAmount()))
				// .setParent(arg0);
				new Label(tagihan.getCalonSiswa() == null || tagihan.getCalonSiswa().getSekolah() == null ? ""
						: tagihan.getCalonSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(tagihan.getCalonSiswa() == null || tagihan.getCalonSiswa().getSekolah() == null
						|| tagihan.getCalonSiswa().getSekolah().getYayasan() == null ? ""
								: tagihan.getCalonSiswa().getSekolah().getYayasan().getNama())
						.setParent(arg0);
			}

			new Label(tagihan.getTahun() == null ? "" : tagihan.getTahun().toString()).setParent(arg0);
			new Label(tagihan.getBulan() == null ? "" : tagihan.getBulan().toString()).setParent(arg0);

			new Label(Common.dateFormat3.get().format(tagihan.getTanggalBayar())).setParent(arg0);
			Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunUtangDiskon();
			Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunDiskon();

			if (akunDebet != null && akunKredit != null) {
				Double nilai = tagihan.getDiskonTidakLangsung();

				Akun akunDiskon = null;
				Double diskonTidakLangsung = 0.0;

				if (diskonTidakLangsung != null && diskonTidakLangsung > 0.1 && akunDiskon == null) {
					new Label("Transaksi tidak valid. Ada diskon " + Common.numberFormat.get().format(diskonTidakLangsung)
							+ ", namun Akun diskon tidak ditemukan").setParent(arg0);
				} else {

					String deskripsi = "<table style='width:100%;'>" + "<thead>";
					deskripsi += "<tr>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Akun</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Debet</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Kredit</th>";
					deskripsi += "</tr>" + "</thead>" + "<tbody>";

					deskripsi += "<tr>";
					deskripsi += "<td style='border:solid;border-width: thin;' >" + akunDebet.getKode() + " - "
							+ akunDebet.getNama() + "</td>";

					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? Math.abs(nilai) : 0.0) + "</td>";
					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? 0.0 : Math.abs(nilai)) + "</td>";
					deskripsi += "</tr>";

					deskripsi += "<tr>";
					deskripsi += "<td style='border:solid;border-width: thin;' >" + akunKredit.getKode() + " - "
							+ akunKredit.getNama() + "</td>";

					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? 0.0 : Math.abs(nilai - diskonTidakLangsung))
							+ "</td>";
					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? Math.abs(nilai - diskonTidakLangsung) : 0.0)
							+ "</td>";
					deskripsi += "</tr>";

					if (diskonTidakLangsung != null && diskonTidakLangsung > 0.1 && akunDiskon != null) {
						deskripsi += "<tr>";
						deskripsi += "<td style='border:solid;border-width: thin;' >" + akunDiskon.getKode() + " - "
								+ akunDiskon.getNama() + "</td>";

						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>" + Common.numberFormat.get()
								.format(diskonTidakLangsung > 0.0 ? 0.0 : Math.abs(diskonTidakLangsung)) + "</td>";
						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>" + Common.numberFormat.get()
								.format(diskonTidakLangsung > 0.0 ? Math.abs(diskonTidakLangsung) : 0.0) + "</td>";
						deskripsi += "</tr>";
					}

					deskripsi += "</tbody></table>";
					new ais.ui.util.MyHtml(deskripsi).setParent(arg0);
				}
			} else {
				// Debet dari Akun Diskon, kredit dari Akun Utang Diskon — keduanya kolom ItemBiayaSekolah.
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + tagihan.getItemBiayaSekolah().getNama() + "\"",
								"Akun Diskon"),
						CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + tagihan.getItemBiayaSekolah().getNama() + "\"",
								"Akun Utang Diskon")));
			}

			new Label(tagihan.getPostingHistoryDiskon() == null ? Common.getBahasaConfig("Belum diposting")
					: tagihan.getPostingHistoryDiskon().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && tagihan.getPostingHistoryDiskon() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tagihan.setPostingHistoryDiskon(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where tagihan="
												+ tagihan.getId() + " and jenis='"
												+ PostingHistory.JENIS_UTANG_DISKON_SISWA + "'" + " and closing is null")
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

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && tagihan.getPostingHistoryDiskon() == null && tbmuser != null
						&& tagihan.getPengaturanBiaya().getSekolah().getSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistoryDiskon = new PostingHistory(
										PostingHistory.JENIS_UTANG_DISKON_SISWA);
								postingHistoryDiskon.setTbmuser(Common.getCurrentUser());
								postingHistoryDiskon.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistoryDiskon.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistoryDiskon);

								Akun akunDiskon = null;
								Akun akunUtangDiskon = null;
								Double diskonTidakLangsung = 0.0;

								Boolean apakahUangMasuk = true;

								String ket = "Utang diskon "
										+ ((tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa()))
										+ (tagihan.getTahunbulan() == null ? ""
												: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
										+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan();

								Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunUtangDiskon();
								Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunDiskon();

								if (akunDebet != null && akunKredit != null) {

									Double nilai = tagihan.getDiskonTidakLangsung();

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDiskon, akunUtangDiskon,
												postingHistoryDiskon, apakahUangMasuk, ket, tagihan.getTanggalBayar(),
												nilai, diskonTidakLangsung, tagihan, tagihan.getNominalBiaya()
														.getPengaturanBiaya().getSekolah().getSatuanKerja(),
												ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDiskon, akunUtangDiskon,
												postingHistoryDiskon, apakahUangMasuk, ket, tagihan.getTanggalBayar(),
												nilai, diskonTidakLangsung, tagihan, tagihan.getNominalBiaya()
														.getPengaturanBiaya().getSekolah().getSatuanKerja(),
												ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
									}

								}

								tagihan.setPostingHistoryDiskon(postingHistoryDiskon);
								session.update(tagihan);

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
		Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? null
						: searchyayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? null
						: searchsekolah.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(Tagihan.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistoryDiskon", sudahPostingDasbor))

				.createAlias("diskonSiswa", "diskonSiswa").add(Restrictions.eq("diskonSiswa.memotongTagihan", false))

				.add(Restrictions.isNotNull("tanggalBayar"))

				.add(Restrictions.gt("diskonTidakLangsung", 0.1))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggalbayar) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiayaSekolah", searchitembiaya.getSelectedItem().getValue()))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistoryDiskon")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistoryDiskon"))

				.add((searchbulan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("bulan", searchbulan.getValue().intValue())))
				.add((searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahun.getValue().intValue())));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

				.add(searchtahunMasuk.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.tahunMasuk", searchtahunMasuk.getValue().intValue()),
								Restrictions.eq("calonSiswa.tahunMasuk", searchtahunMasuk.getValue().intValue())))

				.add(sekolah == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.sekolah", sekolah),
								Restrictions.eq("calonSiswa.sekolah", sekolah)))

				.add(yayasan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.yayasan", yayasan),
								Restrictions.eq("calonSiswa.yayasan", yayasan)))

				.add(searchsiswa.getValue().trim()
						.isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("siswa.nomorIndukNasional",
														searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(),
																MatchMode.ANYWHERE),
														Restrictions.ilike(
																"calonSiswa.nama", searchsiswa.getValue().trim(),
																MatchMode.ANYWHERE))),
										Restrictions.or(
												Restrictions.or(Restrictions.or(
														Restrictions.ilike("siswa.nomorInduk",
																searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
														Restrictions.ilike("calonSiswa.nomorInduk",
																searchsiswa.getValue().trim(), MatchMode.ANYWHERE)),
														Restrictions.ilike("calonSiswa.noRegistrasi",
																searchsiswa.getValue().trim(), MatchMode.ANYWHERE)),
												Restrictions.ilike("calonSiswa.noUjian",
														searchsiswa.getValue().trim()))));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Tagihan> tagihan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tagihan);
		grid.setRowRenderer(new TagihanRenderer());
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
	// PEMELIHARAAN: akun & nilai HARUS tetap identik dengan {@link #onPostingSemua}.
	// =====================================================================

	/**
	 * Kriteria tagihan berdiskon TIDAK LANGSUNG pada rentang tanggal -- sama dengan penghitung
	 * baris "Siswa - Utang Diskon" pada dasbor.
	 *
	 * <p>Saringan {@code diskonSiswa.memotongTagihan = false} itulah intinya: diskon yang
	 * MEMOTONG tagihan tidak menimbulkan utang apa pun (nilai tagihannya sendiri yang
	 * berkurang). Yang berutang hanyalah diskon yang dijanjikan di luar tagihan.</p>
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(Tagihan.class).createAlias("diskonSiswa", "diskonSiswa")
				.add(Restrictions.eq("diskonSiswa.memotongTagihan", false))
				.add(Restrictions.isNotNull("tanggalBayar"))
				.add(Restrictions.gt("diskonTidakLangsung", 0.1));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggalbayar) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/** Batalkan posting SEMUA utang diskon dalam rentang (penanda {@code postingHistoryDiskon}). */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Tagihan> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistoryDiskon")).list();
			for (Tagihan tagihan : daftar) {
				try {
					String syarat = "tagihan=" + tagihan.getId() + " and jenis='"
							+ PostingHistory.JENIS_UTANG_DISKON_SISWA + "' and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					tagihan.setPostingHistoryDiskon(null);
					session.update(tagihan);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingUtangDiskonSiswaAction jalur API");
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
	 * Posting SEMUA utang diskon tidak langsung yang belum dijurnal dalam rentang.
	 *
	 * <p>Debet = {@code itemBiayaSekolah.akunDiskon} (bebannya), kredit =
	 * {@code itemBiayaSekolah.akunUtangDiskon} (kewajibannya), nilai =
	 * {@code diskonTidakLangsung}, tanggal jurnal = {@code tanggalBayar}. Bila nilainya
	 * &le; 0,1 posisi ditukar.</p>
	 *
	 * <p><b>Dua penyimpangan sadar:</b> dokumen berakun tidak lengkap dilewati, dan penanda
	 * posting hanya dipasang bila jurnalnya benar-benar tersimpan.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistoryDiskon"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(
					PostingHistory.JENIS_UTANG_DISKON_SISWA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal utang diskon siswa dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Long id : ids) {
				try {
					session = HibernateUtil.currentNativeSession();
					Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
							.add(Restrictions.idEq(id)).uniqueResult();
					if (tagihan == null || tagihan.getItemBiayaSekolah() == null) {
						continue;
					}
					Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunDiskon();
					Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunUtangDiskon();
					if (akunDebet == null || akunKredit == null) {
						continue;
					}
					String ket = "Utang diskon Tidak Langsung "
							+ (tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa())
							+ (tagihan.getTahunbulan() == null ? ""
									: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
							+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan();
					Double nilai = tagihan.getDiskonTidakLangsung();
					ais.database.model.rab.SatuanKerja satuanKerja = tagihan.getPengaturanBiaya() == null
							|| tagihan.getPengaturanBiaya().getSekolah() == null ? null
									: tagihan.getPengaturanBiaya().getSekolah().getSatuanKerja();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									true, ket, tagihan.getTanggalBayar(), nilai, Double.valueOf(0.0), tagihan,
									satuanKerja, ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									true, ket, tagihan.getTanggalBayar(), nilai, Double.valueOf(0.0), tagihan,
									satuanKerja, ais.action.master.helper.PostingJurnalHelper.REF_DISKON_SISWA, session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingUtangDiskonSiswaAction jalur API");
					}

					if (tersimpan) {
						tagihan.setPostingHistoryDiskon(postingHistory);
						session.getTransaction().begin();
						session.update(tagihan);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingUtangDiskonSiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingUtangDiskonSiswaAction jalur API");
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
