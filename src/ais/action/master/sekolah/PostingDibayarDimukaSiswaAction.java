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
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.PembayaranSiswaDetail;
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

/**
 * Controller/action ZK untuk posting dibayar dimuka siswa. Tipe ini merupakan titik masuk UI yang
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
 * loadDataDenganProgressPosting()}); mutasi data ({@code onBatalkanPostingSemua()}, {@code onPostingSemua()},
 * {@code kriteriaPostingStatic()}, {@code batalkanPostingSemua()}, {@code postingSemua()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PostingDibayarDimukaSiswaAction extends GenericAutowireComposer {

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

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi dibayar dimuka ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Tagihan> tagihans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistoryUangMuka")).list();

							for (Tagihan tagihan : tagihans) {
								tagihan.setPostingHistoryUangMuka(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where tagihan="
												+ tagihan.getId() + " and jenis='"
												+ PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA + "'" + " and closing is null")
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi dibayar dimuka ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting dibayar dimuka siswa berhasil dilakukan",
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

											PostingHistory postingHistoryUangMuka = new PostingHistory(
													PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA);
											postingHistoryUangMuka.setTanggal(tgl);
											postingHistoryUangMuka.setTbmuser(tbmuser);
											postingHistoryUangMuka.setKeterangan(keterangan.getValue().trim()
													+ " \nTgl:" + Common.dateFormat.get().format(tglMulai.getValue())
													+ " s.d " + Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistoryUangMuka);
											session.getTransaction().commit();

											List<Tagihan> tagihans = initCriteria(true)
													.add(Restrictions.isNull("postingHistoryUangMuka")).list();

											int rowIndex = 1;
											for (Tagihan tagihan : tagihans) {
												if (tagihan != null && tagihan.getItemBiayaSekolah() != null) {

													PembayaranSiswaDetail pembayaranSiswaDetail = tagihan
															.getPembayaranSiswaDetail();
													Akun akunDebet = pembayaranSiswaDetail.getPembayaranSiswa() == null
															|| pembayaranSiswaDetail.getPembayaranSiswa()
																	.getAkunPembayaranSiswa() == null
																			? null
																			: pembayaranSiswaDetail.getPembayaranSiswa()
																					.getAkunPembayaranSiswa()
																					.getAkunDeposit();

													if (akunDebet == null || pembayaranSiswaDetail.getPembayaranSiswa()
															.getCalonSiswa() != null) {
														akunDebet = pembayaranSiswaDetail.getPembayaranSiswa()
																.getAkunPembayaranSiswa() == null ? null
																		: pembayaranSiswaDetail.getPembayaranSiswa()
																				.getAkunPembayaranSiswa().getAkun();
													}

													if (pembayaranSiswaDetail.getPembayaranSiswa() != null
															&& pembayaranSiswaDetail.getPembayaranSiswa()
																	.getVirtualAccountBank() != null
															&& pembayaranSiswaDetail.getPembayaranSiswa()
																	.getVirtualAccountBank()
																	.getKanalPembayaran() != null
															&& pembayaranSiswaDetail.getPembayaranSiswa()
																	.getVirtualAccountBank().getKanalPembayaran()
																	.getAkun() != null) {
														akunDebet = pembayaranSiswaDetail.getPembayaranSiswa()
																.getVirtualAccountBank().getKanalPembayaran().getAkun();
													}

													Akun akunKredit = tagihan.getItemBiayaSekolah()
															.getAkunDibayarDimuka();

													if (akunDebet != null && akunKredit != null) {

														Akun akunDenda = null;
														Akun akunPiutangDenda = null;
														Double denda = 0.0;

														Boolean apakahUangMasuk = true;
														String ket = "Pembayaran siswa dibayar dimuka "
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

														Double nilai = tagihan.getPembayaranSiswaDetail().getNominal();

														try {

															session.getTransaction().begin();
															if (nilai > 0.1) {
																CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																		akunDenda, akunPiutangDenda,
																		postingHistoryUangMuka, apakahUangMasuk, ket,
																		tagihan.getPembayaranSiswaDetail()
																				.getPembayaranSiswa().getTanggal(),
																		nilai, denda, tagihan,
																		tagihan.getPengaturanBiaya()
																				.getSekolah().getSatuanKerja(),
																		ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
															} else {
																CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																		akunDenda, akunPiutangDenda,
																		postingHistoryUangMuka, apakahUangMasuk, ket,
																		tagihan.getPembayaranSiswaDetail()
																				.getPembayaranSiswa().getTanggal(),
																		nilai, denda, tagihan,
																		tagihan.getPengaturanBiaya()
																				.getSekolah().getSatuanKerja(),
																		ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
															}
															session.getTransaction().commit();
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

														tagihan.setPostingHistoryUangMuka(postingHistoryUangMuka);
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
	 * Renderer lokal untuk layar/komponen {@link PostingDibayarDimukaSiswaAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingDibayarDimukaSiswaAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingDibayarDimukaSiswaAction
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

			new Label(Common.dateFormat3.get().format(tagihan.getPembayaranSiswaDetail().getPembayaranSiswa().getTanggal()))
					.setParent(arg0);

			PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
			Akun akunDebet = pembayaranSiswaDetail.getPembayaranSiswa() == null
					|| pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa() == null ? null
							: pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkunDeposit();

			if (akunDebet == null || pembayaranSiswaDetail.getPembayaranSiswa().getCalonSiswa() != null) {
				akunDebet = pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa() == null ? null
						: pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkun();
			}

			if (pembayaranSiswaDetail.getPembayaranSiswa() != null
					&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank() != null
					&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank().getKanalPembayaran() != null
					&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank().getKanalPembayaran()
							.getAkun() != null) {
				akunDebet = pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank().getKanalPembayaran()
						.getAkun();
			}

			Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunDibayarDimuka();

			if (akunDebet != null && akunKredit != null) {
				Double nilai = tagihan.getPembayaranSiswaDetail().getNominal();

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				// Debet dari Akun Pembayaran Siswa/kanal VA, kredit dari Akun Dibayar Dimuka ItemBiayaSekolah.
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						"Langkah perbaikan: (1) Buka menu \"Akun Pembayaran Siswa\", cari Akun Pembayaran Siswa yang "
								+ "dipakai transaksi ini, lengkapi kolom \"Akun\" atau \"Akun Deposit\". (2) Bila "
								+ "pembayaran lewat Virtual Account, buka menu \"Kanal Pembayaran Virtual Account\", "
								+ "cari kanal yang dipakai, lengkapi kolom \"Akun\". (3) Klik Simpan. Setelah itu, "
								+ "muat ulang (refresh) halaman Posting ini — transaksi akan otomatis terhitung valid.",
						CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + tagihan.getItemBiayaSekolah().getNama() + "\"",
								"Akun Dibayar Dimuka")));
			}

			new Label(tagihan.getPostingHistoryUangMuka() == null ? Common.getBahasaConfig("Belum diposting")
					: tagihan.getPostingHistoryUangMuka().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PembayaranSiswaUtil.cetakStruk(tagihan.getPembayaranSiswaDetail().getPembayaranSiswa());
				}

			});
			button.setParent(toolbar);

			if (akunDebet != null && akunKredit != null) {
				button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && tagihan.getPostingHistoryUangMuka() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tagihan.setPostingHistoryUangMuka(null);
								Common.refreshSaveOrUpdate(tagihan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where tagihan="
												+ tagihan.getId() + " and jenis='"
												+ PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA + "'" + " and closing is null")
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
				button.setVisible(edit && tagihan.getPostingHistoryUangMuka() == null && tbmuser != null
						&& tagihan.getPengaturanBiaya().getSekolah().getSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistoryUangMuka = new PostingHistory(
										PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA);
								postingHistoryUangMuka.setTbmuser(Common.getCurrentUser());
								postingHistoryUangMuka.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistoryUangMuka.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistoryUangMuka);

								Akun akunDenda = null;
								Akun akunPiutangDenda = null;
								Double denda = 0.0;

								Boolean apakahUangMasuk = true;

								String ket = "Pembayaran siswa dibayar dimuka "
										+ ((tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa()))
										+ (tagihan.getTahunbulan() == null ? ""
												: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
										+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan();

								PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
								Akun akunDebet = pembayaranSiswaDetail.getPembayaranSiswa() == null
										|| pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa() == null
												? null
												: pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa()
														.getAkunDeposit();

								if (akunDebet == null
										|| pembayaranSiswaDetail.getPembayaranSiswa().getCalonSiswa() != null) {
									akunDebet = pembayaranSiswaDetail.getPembayaranSiswa()
											.getAkunPembayaranSiswa() == null ? null
													: pembayaranSiswaDetail.getPembayaranSiswa()
															.getAkunPembayaranSiswa().getAkun();
								}

								if (pembayaranSiswaDetail.getPembayaranSiswa() != null
										&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank() != null
										&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank()
												.getKanalPembayaran() != null
										&& pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank()
												.getKanalPembayaran().getAkun() != null) {
									akunDebet = pembayaranSiswaDetail.getPembayaranSiswa().getVirtualAccountBank()
											.getKanalPembayaran().getAkun();
								}

								Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunDibayarDimuka();

								if (akunDebet != null && akunKredit != null) {

									Double nilai = tagihan.getPembayaranSiswaDetail().getNominal();

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistoryUangMuka, apakahUangMasuk, ket,
												tagihan.getPembayaranSiswaDetail().getPembayaranSiswa().getTanggal(),
												nilai, denda, tagihan, tagihan.getPengaturanBiaya()
														.getSekolah().getSatuanKerja(),
												ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistoryUangMuka, apakahUangMasuk, ket,
												tagihan.getPembayaranSiswaDetail().getPembayaranSiswa().getTanggal(),
												nilai, denda, tagihan, tagihan.getPengaturanBiaya()
														.getSekolah().getSatuanKerja(),
												ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
									}

								}

								tagihan.setPostingHistoryUangMuka(postingHistoryUangMuka);
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

		Criteria criteria = session.createCriteria(Tagihan.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistoryUangMuka", sudahPostingDasbor))
				.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
				.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")

				.add(Restrictions.gt("pembayaranSiswaDetail.nominal", 0.1))

				.add(Restrictions.sqlRestriction("date(tanggal_bayar)<date(tanggal_tagihan)"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_bayar) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiayaSekolah", searchitembiaya.getSelectedItem().getValue()))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistoryUangMuka")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistoryUangMuka"))

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
	// PEMELIHARAAN: penentuan akun debet di sini HARUS tetap identik dengan
	// {@link #onPostingSemua} -- urutan pemilihannya berlapis tiga.
	// =====================================================================

	/**
	 * Kriteria tagihan yang dibayar SEBELUM masa tagihannya, sama persis dengan penghitung
	 * baris "Siswa - Dibayar Dimuka" pada dasbor
	 * ({@code DraftJurnalRingkasanUtil.kriteriaTagihanSiswaDibayarDimuka}).
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(Tagihan.class)
				.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
				.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")
				.add(Restrictions.gt("pembayaranSiswaDetail.nominal", 0.1))
				.add(Restrictions.sqlRestriction("date(tanggal_bayar)<date(tanggal_tagihan)"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(tanggal_bayar) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA jurnal "dibayar di muka" dalam rentang.
	 *
	 * <p>Penanda yang dilepas adalah {@code postingHistoryUangMuka} -- BUKAN {@code postingHistory}
	 * yang dipakai baris "Siswa - Piutang Tagihan". Satu baris {@code tagihan} memikul beberapa
	 * jurnal sekaligus, jadi saringan {@code jenis} pada penghapusan pun wajib.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Tagihan> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistoryUangMuka")).list();
			for (Tagihan tagihan : daftar) {
				try {
					String syarat = "tagihan=" + tagihan.getId() + " and jenis='"
							+ PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA
							+ "' and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					tagihan.setPostingHistoryUangMuka(null);
					session.update(tagihan);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingDibayarDimukaSiswaAction jalur API");
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
	 * Posting SEMUA pembayaran yang mendahului masa tagihannya.
	 *
	 * <p><b>Akun debet dipilih berlapis, urutannya penting</b> (disalin dari layar):</p>
	 * <ol>
	 *   <li>akun DEPOSIT pada cara pembayaran siswa;</li>
	 *   <li>bila itu kosong ATAU pembayarnya masih CALON siswa -&gt; akun biasa pada cara
	 *       pembayaran. Calon siswa memang belum punya saldo deposit, jadi menariknya dari
	 *       akun deposit akan menimbulkan saldo yang tidak pernah ada;</li>
	 *   <li>bila pembayarannya lewat virtual account yang kanalnya punya akun sendiri -&gt;
	 *       akun kanal itu yang menang, karena uangnya memang masuk ke sana.</li>
	 * </ol>
	 *
	 * <p>Kredit = {@code itemBiayaSekolah.akunDibayarDimuka}; nilai dari detail pembayaran;
	 * bila &le; 0,1 posisi ditukar; tanggal jurnal dari {@code pembayaranSiswa.tanggal}.</p>
	 *
	 * <p><b>Dua penyimpangan sadar:</b> dokumen berakun tidak lengkap dilewati, dan penanda
	 * posting hanya dipasang bila jurnalnya benar-benar tersimpan.</p>
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
					.add(Restrictions.isNull("postingHistoryUangMuka"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(
					PostingHistory.JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pembayaran siswa dibayar dimuka dari dasbor jurnal"
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
					if (tagihan == null || tagihan.getItemBiayaSekolah() == null
							|| tagihan.getPembayaranSiswaDetail() == null) {
						continue;
					}
					PembayaranSiswaDetail detail = tagihan.getPembayaranSiswaDetail();
					ais.database.model.sekolah.PembayaranSiswa bayar = detail.getPembayaranSiswa();

					Akun akunDebet = bayar == null || bayar.getAkunPembayaranSiswa() == null ? null
							: bayar.getAkunPembayaranSiswa().getAkunDeposit();
					if (akunDebet == null || (bayar != null && bayar.getCalonSiswa() != null)) {
						akunDebet = bayar == null || bayar.getAkunPembayaranSiswa() == null ? null
								: bayar.getAkunPembayaranSiswa().getAkun();
					}
					if (bayar != null && bayar.getVirtualAccountBank() != null
							&& bayar.getVirtualAccountBank().getKanalPembayaran() != null
							&& bayar.getVirtualAccountBank().getKanalPembayaran().getAkun() != null) {
						akunDebet = bayar.getVirtualAccountBank().getKanalPembayaran().getAkun();
					}
					Akun akunKredit = tagihan.getItemBiayaSekolah().getAkunDibayarDimuka();
					if (akunDebet == null || akunKredit == null) {
						// Jurnalnya tidak lengkap: dilewati, bukan ditandai terposting.
						continue;
					}

					String ket = "Pembayaran siswa dibayar dimuka "
							+ (tagihan.getSiswa() == null ? tagihan.getCalonSiswa() : tagihan.getSiswa())
							+ (tagihan.getTahunbulan() == null ? ""
									: " tahun/bulan " + tagihan.getTahunbulan() + " - ")
							+ tagihan.getItemBiayaSekolah().getNama() + " - " + tagihan.getKeterangan();
					Double nilai = detail.getNominal();
					ais.database.model.rab.SatuanKerja satuanKerja = tagihan.getPengaturanBiaya() == null
							|| tagihan.getPengaturanBiaya().getSekolah() == null ? null
									: tagihan.getPengaturanBiaya().getSekolah().getSatuanKerja();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									true, ket, bayar == null ? null : bayar.getTanggal(), nilai,
									Double.valueOf(0.0), tagihan, satuanKerja, ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									true, ket, bayar == null ? null : bayar.getTanggal(), nilai,
									Double.valueOf(0.0), tagihan, satuanKerja, ais.action.master.helper.PostingJurnalHelper.REF_DIMUKA_SISWA, session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingDibayarDimukaSiswaAction jalur API");
					}

					if (tersimpan) {
						tagihan.setPostingHistoryUangMuka(postingHistory);
						session.getTransaction().begin();
						session.update(tagihan);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingDibayarDimukaSiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingDibayarDimukaSiswaAction jalur API");
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
