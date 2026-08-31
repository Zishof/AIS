package ais.action.master.sekolah;

import java.util.ArrayList;
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
import org.zkoss.zul.Tabpanel;
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
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk posting piutang siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchsiswa}, {@code Combobox searchyayasan}, {@code MyCheckboxConfig searchtampil}, {@code
 * MyCheckboxConfig searchtelahtampil}, {@code Decimalbox searchbulan}, {@code Decimalbox searchtahun};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefaultTanpaProgress()}, {@code onSearchDefault()}, {@code
 * loadDataDenganProgressPosting()}); mutasi data ({@code onPostingPembayaran()}, {@code
 * onBatalkanPostingSemua()}, {@code onPostingSemua()}, {@code kriteriaPostingStatic()}, {@code
 * batalkanPostingSemua()}, {@code postingSemua()}); operasi domain lain ({@code onPiutangDenda()}, {@code
 * onTabungan()}, {@code onUtangDiskon()}, {@code onDibayarDimuka()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class PostingPiutangSiswaAction extends GenericAutowireComposer {

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

	private Tabpanel tabpanelPostingPiutangDenda;

	public void onPiutangDenda(Event event) {
		if (tabpanelPostingPiutangDenda.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingPiutangDenda);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/posting_piutang_denda.zul");
			iframe.setParent(window);
		}
	}
	
	
	private Tabpanel tabpanelPostingTabungan;

	public void onTabungan(Event event) {
		if (tabpanelPostingTabungan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingTabungan);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/posting_tabungan_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabpanelPostingUtangDiskon;

	public void onUtangDiskon(Event event) {
		if (tabpanelPostingUtangDiskon.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingUtangDiskon);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/posting_utang_diskon.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabpanelPostingDibayarDimuka;

	public void onDibayarDimuka(Event event) {
		if (tabpanelPostingDibayarDimuka.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingDibayarDimuka);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/posting_dibayar_dimuka.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabpanelPostingPembayaran;

	public void onPostingPembayaran(Event event) {
		if (tabpanelPostingPembayaran.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingPembayaran);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/posting_pembayaran.zul");
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
		if (sent == null) return;
		if (sent != null) { sent.setVisible(edit); }

		Tbmuser tbmuser = Common.getCurrentUser();

		Sekolah curr = tbmuser == null ? null : tbmuser.ambilSekolah();

		Common.insertComboDanSemua(searchitembiaya, new String[] { "nama", "kode" }, "keterangan",
				ItemBiayaSekolah.class,
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
							List<Tagihan> tagihans = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (Tagihan tagihan : tagihans) {
								tagihan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where tagihan=" + tagihan.getId()
														+ " and jenis='" + PostingHistory.JENIS_PIUTANG_SISWA + "'" + " and closing is null")
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
		addWindow.setTitle("Posting Piutang Siswa");
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

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PIUTANG_SISWA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<Tagihan> tagihans = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (Tagihan tagihan : tagihans) {
												if (tagihan != null && tagihan.getItemBiayaSekolah() != null) {

													Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunPiutang();
													Akun akunKredit = tagihan.getItemBiayaSekolah().getAkun();

													if (akunDebet != null && akunKredit != null) {

														Akun akunDenda = null;
														Akun akunPiutangDenda = null;
														Double denda = 0.0;

														Boolean apakahUangMasuk = true;
														String ket = "Piutang "
																+ ((tagihan.getSiswa() == null ? tagihan.getCalonSiswa()
																		: tagihan.getSiswa()))
																+ (tagihan.getTahunbulan() == null ? ""
																		: " tahun/bulan " + tagihan.getTahunbulan()
																				+ " - ")
																+ tagihan.getItemBiayaSekolah().getNama() + " - "
																+ tagihan.getKeterangan()
																+ (tagihan.getDiskonSiswa() != null
																		? " - " + tagihan.getDiskonSiswa().getNama()
																		: "");

														label.setValue(
																ket + " ("
																		+ Common.numberFormat.get().format(
																				rowIndex * 100.0 / tagihans.size())
																		+ " %)");

														Double nilai = tagihan.getNominal();

														if (tagihan.getDiskon() > 0.1) {

															if (tagihan.getItemBiayaSekolah().getAkunPiutang() != null
																	&& tagihan.getItemBiayaSekolah()
																			.getAkunDiskon() != null
																	&& akunKredit != null) {

																try {

																	Akun[] akunDebets = new Akun[] {
																			tagihan.getItemBiayaSekolah()
																					.getAkunPiutang(),
																			tagihan.getItemBiayaSekolah()
																					.getAkunDiskon() };
																	Double[] nilais = new Double[] {
																			nilai - tagihan.getDiskon(),
																			tagihan.getDiskon() };

																	CommonAkunting.saveTransaksi(akunDebets,
																			new Akun[] { akunKredit }, akunDenda,
																			akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			tagihan.getTanggalTagihan(), nilais,
																			new Double[] { nilai }, denda, tagihan,
																			tagihan.getNominalBiaya()
																					.getPengaturanBiaya().getSekolah()
																					.getSatuanKerja(),
																			session);

																} catch (Exception e) {
																	Common.tampilErrorJikaAdmin(e);
																}
															}
														} else {

															try {

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			tagihan.getTanggalTagihan(), nilai, denda,
																			tagihan,
																			tagihan.getNominalBiaya()
																					.getPengaturanBiaya().getSekolah()
																					.getSatuanKerja(),
																			session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			tagihan.getTanggalTagihan(), nilai, denda,
																			tagihan,
																			tagihan.getNominalBiaya()
																					.getPengaturanBiaya().getSekolah()
																					.getSatuanKerja(),
																			session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
														}

														tagihan.setPostingHistory(postingHistory);
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

	/**
	 * Renderer lokal untuk layar/komponen {@link PostingPiutangSiswaAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingPiutangSiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingPiutangSiswaAction
	 */
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

				new Label(tagihan.getNominal() == null ? "0" : Common.numberFormat.get().format(tagihan.getNominal()))
						.setParent(arg0);

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

				new Label(tagihan.getNominal() == null ? "0" : Common.numberFormat.get().format(tagihan.getNominal()))
						.setParent(arg0);

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

			new Label(Common.dateFormat3.get().format(tagihan.getTanggalTagihan())).setParent(arg0);
			Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunPiutang();
			Akun akunKredit = tagihan.getItemBiayaSekolah().getAkun();

			if (tagihan.getDiskon() > 0.1) {

				if (tagihan.getItemBiayaSekolah().getAkunPiutang() != null
						&& tagihan.getItemBiayaSekolah().getAkunDiskon() != null && akunKredit != null) {

					Double nilai = tagihan.getNominal();

					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Akun> akunsKredits = new ArrayList<Akun>();

					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();

					akunsDebets.add(tagihan.getItemBiayaSekolah().getAkunPiutang());
					akunsDebets.add(tagihan.getItemBiayaSekolah().getAkunDiskon());

					nilaiDebets.add(nilai - tagihan.getDiskon());
					nilaiDebets.add(tagihan.getDiskon());

					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai);

					GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

				} else {
					// Diskon: butuh Akun Piutang + Akun Diskon (dan Akun kredit) — ItemBiayaSekolah.
					String namaItem = tagihan.getItemBiayaSekolah().getNama();
					StringBuilder pesanDiskon = new StringBuilder("Transaksi tidak valid, diskon tidak ada.");
					if (tagihan.getItemBiayaSekolah().getAkunPiutang() == null) {
						pesanDiskon.append(" ").append(CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + namaItem + "\"", "Akun Piutang"));
					}
					if (tagihan.getItemBiayaSekolah().getAkunDiskon() == null) {
						pesanDiskon.append(" ").append(CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + namaItem + "\"", "Akun Diskon"));
					}
					if (akunKredit == null) {
						pesanDiskon.append(" ").append(CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + namaItem + "\"", "Akun"));
					}
					new Label(pesanDiskon.toString()).setParent(arg0);
				}

			} else {
				if (akunDebet != null && akunKredit != null) {
					Double nilai = tagihan.getNominal();

					GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
				} else {
					// Debet dari Akun Piutang, kredit dari Akun — keduanya kolom ItemBiayaSekolah.
					ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
							CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
									"item biaya sekolah \"" + tagihan.getItemBiayaSekolah().getNama() + "\"",
									"Akun Piutang"),
							CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
									"item biaya sekolah \"" + tagihan.getItemBiayaSekolah().getNama() + "\"",
									"Akun")));
				}
			}

			new Label(tagihan.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting") : tagihan.getPostingHistory().toString())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && tagihan.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tagihan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where tagihan=" + tagihan.getId()
														+ " and jenis='" + PostingHistory.JENIS_PIUTANG_SISWA + "'" + " and closing is null")
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
				button.setVisible(edit && tagihan.getPostingHistory() == null && tbmuser != null
						&& tagihan.getPengaturanBiaya().getSekolah().getSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PIUTANG_SISWA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDenda = null;
								Akun akunPiutangDenda = null;
								Double denda = 0.0;

								Boolean apakahUangMasuk = true;

								String ket = "Piutang "
										+ ((tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa()))
										+ (tagihan.getTahunbulan() == null ? ""
												: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
										+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan()
										+ (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama()
												: "");

								Double nilai = tagihan.getNominal();

								if (tagihan.getDiskon() > 0.1) {
									Akun akunKredit = tagihan.getItemBiayaSekolah().getAkun();

									if (tagihan.getItemBiayaSekolah().getAkunPiutang() != null
											&& tagihan.getItemBiayaSekolah().getAkunDiskon() != null
											&& akunKredit != null) {

										Akun[] akunDebet = new Akun[] { tagihan.getItemBiayaSekolah().getAkunPiutang(),
												tagihan.getItemBiayaSekolah().getAkunDiskon() };
										Double[] nilais = new Double[] { nilai - tagihan.getDiskon(),
												tagihan.getDiskon() };

										CommonAkunting.saveTransaksi(akunDebet, new Akun[] { akunKredit }, akunDenda,
												akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
												tagihan.getTanggalTagihan(), nilais, new Double[] { nilai }, denda,
												tagihan, tagihan.getPengaturanBiaya().getSekolah()
														.getSatuanKerja(),
												session);
									}
								} else {

									Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunPiutang();
									Akun akunKredit = tagihan.getItemBiayaSekolah().getAkun();

									if (akunDebet != null && akunKredit != null) {

										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													tagihan.getTanggalTagihan(), nilai, denda, tagihan,
													tagihan.getPengaturanBiaya().getSekolah()
															.getSatuanKerja(),
													session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													tagihan.getTanggalTagihan(), nilai, denda, tagihan,
													tagihan.getPengaturanBiaya().getSekolah()
															.getSatuanKerja(),
													session);
										}

									}
								}

								tagihan.setPostingHistory(postingHistory);
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
		if (searchyayasan == null) return null;
		Session session = HibernateUtil.currentSession();
		Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? null
						: searchyayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? null
						: searchsekolah.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(Tagihan.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
				.add(Restrictions.isNotNull("itemBiayaSekolah.akunPiutang"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.createAlias("nominalBiaya", "nominalBiaya")
				.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
				.createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah")

				.add(Restrictions.or(Restrictions.eq("jenisBiayaSekolah.periode", "Bulanan"),
						Restrictions.and(Restrictions.eq("bayarKe", 1),
								Restrictions.eq("jenisBiayaSekolah.periode", "Insidentil"))))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_tagihan) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiayaSekolah", searchitembiaya.getSelectedItem().getValue()))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistory")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

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
	//
	// Kembaran non-ZK dari tombol "Posting Semua"/"Batalkan Posting Semua".
	// PEMELIHARAAN: penentuan akun dan nilai di sini HARUS tetap identik dengan
	// {@link #onPostingSemua}.
	// =====================================================================

	/**
	 * Kriteria tagihan siswa yang layak dijurnal piutang pada rentang tanggal -- sama persis
	 * dengan penghitung baris "Siswa - Piutang Tagihan" pada dasbor
	 * ({@code DraftJurnalRingkasanUtil.kriteriaTagihanSiswaPiutang}).
	 *
	 * <p>Saringan periodenya bukan hiasan: hanya biaya <b>Bulanan</b>, atau biaya
	 * <b>Insidentil</b> pada cicilan pertama, yang menerbitkan piutang. Tanpa itu, tiap
	 * cicilan biaya insidentil akan membuat piutang baru untuk tagihan yang sama.</p>
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(Tagihan.class)
				.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
				.add(Restrictions.isNotNull("itemBiayaSekolah.akunPiutang"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("nominalBiaya", "nominalBiaya")
				.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
				.createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah")
				.add(Restrictions.or(Restrictions.eq("jenisBiayaSekolah.periode", "Bulanan"),
						Restrictions.and(Restrictions.eq("bayarKe", 1),
								Restrictions.eq("jenisBiayaSekolah.periode", "Insidentil"))));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(tanggal_tagihan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA piutang tagihan siswa dalam rentang.
	 *
	 * <p><b>Saringan {@code jenis} itu wajib.</b> Satu baris {@code tagihan} dapat memikul
	 * BEBERAPA jurnal sekaligus -- piutang, dibayar di muka, denda, dan utang diskon. Menghapus
	 * tanpa menyebut {@code jenis} akan ikut membatalkan jurnal saudara-saudaranya. Syaratnya
	 * disalin apa adanya dari {@link #onBatalkanPostingSemua}.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Tagihan> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Tagihan tagihan : daftar) {
				try {
					String syarat = "tagihan=" + tagihan.getId() + " and jenis='"
							+ PostingHistory.JENIS_PIUTANG_SISWA + "' and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					tagihan.setPostingHistory(null);
					session.update(tagihan);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingPiutangSiswaAction jalur API");
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
	 * Posting SEMUA piutang tagihan siswa yang belum dijurnal dalam rentang.
	 *
	 * <ul>
	 *   <li>debet = {@code itemBiayaSekolah.akunPiutang}, kredit = {@code itemBiayaSekolah.akun};</li>
	 *   <li>bila tagihannya berdiskon ({@code diskon > 0,1}) dan akun diskonnya ada, sisi DEBET
	 *       dipecah dua: {@code nominal - diskon} ke akun piutang dan {@code diskon} ke akun
	 *       diskon, sementara kreditnya tetap satu baris senilai nominal penuh;</li>
	 *   <li>bila nominalnya &le; 0,1 posisi debet/kredit ditukar -- sama seperti layar;</li>
	 *   <li>tanggal jurnal = {@code tanggalTagihan}, satuan kerja = sekolah pemilik tagihan.</li>
	 * </ul>
	 *
	 * <p><b>Dua penyimpangan sadar dari layar:</b> dokumen yang akunnya belum lengkap dilewati
	 * (layar diam-diam melewatkan penulisan jurnal tetapi TETAP memasang penanda posting), dan
	 * penanda posting hanya dipasang bila jurnalnya benar-benar tersimpan.</p>
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

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PIUTANG_SISWA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal piutang tagihan siswa dari dasbor jurnal"
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
					Akun akunDebet = tagihan.getItemBiayaSekolah().getAkunPiutang();
					Akun akunKredit = tagihan.getItemBiayaSekolah().getAkun();
					if (akunDebet == null || akunKredit == null) {
						// Jurnalnya tidak lengkap: dilewati, bukan ditandai terposting.
						continue;
					}
					ais.database.model.rab.SatuanKerja satuanKerja = tagihan.getNominalBiaya() == null
							|| tagihan.getNominalBiaya().getPengaturanBiaya() == null
							|| tagihan.getNominalBiaya().getPengaturanBiaya().getSekolah() == null ? null
									: tagihan.getNominalBiaya().getPengaturanBiaya().getSekolah()
											.getSatuanKerja();

					String ket = "Piutang "
							+ (tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa())
							+ (tagihan.getTahunbulan() == null ? ""
									: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
							+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan()
							+ (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama()
									: "");
					Double nilai = tagihan.getNominal();
					Double diskon = tagihan.getDiskon() == null ? Double.valueOf(0.0) : tagihan.getDiskon();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (diskon > 0.1 && tagihan.getItemBiayaSekolah().getAkunDiskon() != null) {
							CommonAkunting.saveTransaksi(
									new Akun[] { akunDebet, tagihan.getItemBiayaSekolah().getAkunDiskon() },
									new Akun[] { akunKredit }, null, null, postingHistory, true, ket,
									tagihan.getTanggalTagihan(),
									new Double[] { nilai - diskon, diskon }, new Double[] { nilai },
									Double.valueOf(0.0), tagihan, satuanKerja, session);
						} else if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									true, ket, tagihan.getTanggalTagihan(), nilai, Double.valueOf(0.0),
									tagihan, satuanKerja, session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									true, ket, tagihan.getTanggalTagihan(), nilai, Double.valueOf(0.0),
									tagihan, satuanKerja, session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingPiutangSiswaAction jalur API");
					}

					if (tersimpan) {
						// Penanda posting hanya dipasang bila jurnalnya BENAR-BENAR tersimpan.
						tagihan.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(tagihan);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingPiutangSiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingPiutangSiswaAction jalur API");
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
