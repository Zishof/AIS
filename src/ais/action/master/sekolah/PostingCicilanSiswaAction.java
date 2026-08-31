package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
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
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Sekolah;
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
 * Controller/action ZK untuk posting cicilan siswa. Tipe ini merupakan titik masuk UI yang
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
 * {@code eksekusiPosting()}, {@code kriteriaPostingStatic()}, {@code batalkanPostingSemua()}, {@code
 * postingSemua()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PostingCicilanSiswaAction extends GenericAutowireComposer {

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
	private Combobox searchjenispembayaran;
	private Combobox searchjenis;
	private Decimalbox searchtahunMasuk;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

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
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		Common.insertComboDanSemua(searchjenis, new String[] { "nama", "sekolah" }, "keterangan",
				AkunPembayaranSiswa.class,
				Restrictions.and(
						curr == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", curr)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		Common.insertComboDanSemua(searchjenispembayaran, new String[] { "nama", "sekolah" }, "periode",
				JenisBiayaSekolah.class,
				Restrictions.and(
						curr == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", curr)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				sudahPostingDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterSudahPosting();
		ais.action.master.helper.PostingJurnalHelper.terapkanParameterTanggal(tglMulai, tglSampai);

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
							List<PembayaranSiswaDetail> pembayaranSiswaDetails = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranSiswaDetail pembayaranSiswaDetail : pembayaranSiswaDetails) {
								pembayaranSiswaDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranSiswaDetail);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where pembayaran_siswa_detail="
														+ pembayaranSiswaDetail.getId() + " and closing is null")
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
		addWindow.setTitle("Posting Pembayaran Siswa");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pembayaran ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Posting transaksi siswa berhasil dilakukan",
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
										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											Session session = HibernateUtil.currentNativeSession();
											try {
												PostingHistory postingHistory = new PostingHistory(
														PostingHistory.JENIS_PEMBAYARAN_SISWA);
												postingHistory.setTanggal(tgl);
												postingHistory.setTbmuser(tbmuser);
												postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
														+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
														+ Common.dateFormat.get().format(tglSampai.getValue()));
												
												session.getTransaction().begin();
												session.save(postingHistory);

												List<PembayaranSiswaDetail> pembayaranSiswaDetails = initCriteria(true)
														.add(Restrictions.isNull("postingHistory")).list();

												int rowIndex = 1;
												Map<Long, Integer> countTabunganMap = new HashMap<Long, Integer>();

												for (PembayaranSiswaDetail pembayaranSiswaDetail : pembayaranSiswaDetails) {
													if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getItemBiayaSekolah() != null) {
														
														Long psId = pembayaranSiswaDetail.getPembayaranSiswa() != null ? pembayaranSiswaDetail.getPembayaranSiswa().getId() : null;
														int totalDetail = 1;
														if (psId != null) {
															if (!countTabunganMap.containsKey(psId)) {
																Number count = (Number) session.createCriteria(PembayaranSiswaDetail.class)
																		.add(Restrictions.eq("pembayaranSiswa.id", psId))
																		.setProjection(Projections.rowCount())
																		.uniqueResult();
																countTabunganMap.put(psId, count != null && count.intValue() > 0 ? count.intValue() : 1);
															}
															totalDetail = countTabunganMap.get(psId);
														}
														
														label.setValue("Memproses (" + Common.numberFormat.get().format(rowIndex * 100.0 / pembayaranSiswaDetails.size()) + " %)");

														eksekusiPosting(session, pembayaranSiswaDetail, postingHistory, totalDetail);
														
														// Flush agar mencegah out of memory saat looping ribuan data
														if (rowIndex % 50 == 0) {
															session.flush();
														}
													}
													rowIndex++;
												}
												session.getTransaction().commit();

											} catch (Exception e) {
												session.getTransaction().rollback();
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {
												label.setValue("");
												HibernateUtil.closeSession();
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

	// Method helper sentral untuk logika jurnal Debet / Kredit & Memecah nilai tabungan secara efisien
	// Dipakai BERSAMA oleh tombol layar ZK dan mesin non-ZK di bawah; hanya memakai
	// parameternya sendiri, jadi aman dijadikan static. Satu implementasi jurnal.
	static void eksekusiPosting(Session session, PembayaranSiswaDetail detail, PostingHistory postingHistory, int totalDetailCount) throws Exception {
		if (detail.getPembayaranSiswa() == null || detail.getItemBiayaSekolah() == null) return;

		Akun akunKredit = detail.getItemBiayaSekolah().getAkunPiutang();
		Akun akunDebet = detail.getPembayaranSiswa().getAkunPembayaranSiswa() == null ? null : detail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkun();

		Date tglBayar = detail.getPembayaranSiswa().getTanggalBayar();
		Date tglTagihan = detail.getTagihan() != null ? detail.getTagihan().getTanggalTagihan() : null;

		if (tglBayar != null && tglTagihan != null && tglBayar.before(tglTagihan) && detail.getItemBiayaSekolah().getAkunDibayarDimuka() != null) {
			akunDebet = detail.getItemBiayaSekolah().getAkunDibayarDimuka();
		}

		if (akunDebet == null || akunKredit == null) return;

		String namaSiswa = detail.getTagihan().getSiswa() != null ? detail.getTagihan().getSiswa().getNama() : (detail.getTagihan().getCalonSiswa() != null ? detail.getTagihan().getCalonSiswa().getNama() : "");
		String ketTahunBulan = detail.getTagihan().getTahunbulan() != null ? " tahun/bulan " + detail.getTagihan().getTahunbulan() + " - " : " - ";
		String ket = "Pembayaran " + namaSiswa + ketTahunBulan + detail.getItemBiayaSekolah().getNama();

		Double nominalAsli = detail.getNominal() == null ? 0.0 : detail.getNominal();
		Double denda = detail.getTagihan().getDenda() == null ? 0.0 : detail.getTagihan().getDenda();
		Double tabunganUtuh = detail.getPembayaranSiswa().getDariTabungan() == null ? 0.0 : detail.getPembayaranSiswa().getDariTabungan();
		
		// Pemecahan nilai tabungan dibagi berdasarkan jumlah anak data (totalDetailCount)
		Double proporsiTabungan = (tabunganUtuh > 0 && totalDetailCount > 0) ? (tabunganUtuh / totalDetailCount) : 0.0;

		Akun akunDenda = detail.getItemBiayaSekolah().getAkunPiutangDenda();
		Akun akunPiutangDenda = null;

		List<Akun> akunsDebets = new ArrayList<Akun>();
		List<Akun> akunsKredits = new ArrayList<Akun>();
		List<Double> nilaiDebets = new ArrayList<Double>();
		List<Double> nilaiKredits = new ArrayList<Double>();

		Double sisaNominalDebet = nominalAsli;

		// Jika ada tabungan deposit
		if (proporsiTabungan > 0.1 && detail.getPembayaranSiswa().getAkunPembayaranSiswa() != null && detail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkunDeposit() != null) {
			akunsDebets.add(detail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkunDeposit());
			nilaiDebets.add(proporsiTabungan);
			sisaNominalDebet = sisaNominalDebet - proporsiTabungan;
		}

		akunsDebets.add(akunDebet);
		nilaiDebets.add(sisaNominalDebet);

		akunsKredits.add(akunKredit);
		nilaiKredits.add(nominalAsli - denda);

		if (denda > 0.1 && akunDenda != null) {
			akunsKredits.add(akunDenda);
			nilaiKredits.add(denda);
		}

		CommonAkunting.saveTransaksi(
				akunsDebets.toArray(new Akun[0]), 
				akunsKredits.toArray(new Akun[0]), 
				akunDenda, 
				akunPiutangDenda, 
				postingHistory, 
				true, // apakahUangMasuk
				ket,
				tglBayar,
				nilaiDebets.toArray(new Double[0]),
				nilaiKredits.toArray(new Double[0]), 
				denda, 
				detail,
				detail.getTagihan().getPengaturanBiaya().getSekolah().getSatuanKerja(),
				null, 
				session);

		detail.setPostingHistory(postingHistory);
		session.update(detail);
	}

	class PembayaranSiswaDetailRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranSiswaDetail pembayaranSiswaDetail = (PembayaranSiswaDetail) arg1;
			final PembayaranSiswa pembayaranSiswa = pembayaranSiswaDetail.getPembayaranSiswa();

			if (pembayaranSiswa.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pembayaranSiswa.getSiswa()).setParent(arg0);
				new Label(pembayaranSiswa.getSiswa() == null ? "" : pembayaranSiswa.getSiswa().getNomorInduk()).setParent(arg0);
				RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, pembayaranSiswaDetail, pembayaranSiswa.getSiswa() == null ? "" : pembayaranSiswa.getSiswa().getNama()).setParent(arg0);
				new Label(pembayaranSiswaDetail.getItemBiayaSekolah().getNama()).setParent(arg0);
				new Label(pembayaranSiswaDetail.getNominal() == null ? "0" : Common.numberFormat.get().format(pembayaranSiswaDetail.getNominal())).setParent(arg0);
				new Label(pembayaranSiswa.getSiswa() == null || pembayaranSiswa.getSiswa().getSekolah() == null ? "" : pembayaranSiswa.getSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(pembayaranSiswa.getSiswa() == null || pembayaranSiswa.getSiswa().getSekolah() == null || pembayaranSiswa.getSiswa().getSekolah().getYayasan() == null ? "" : pembayaranSiswa.getSiswa().getSekolah().getYayasan().getNama()).setParent(arg0);
			} else if (pembayaranSiswa.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pembayaranSiswa.getCalonSiswa()).setParent(arg0);
				new Label(pembayaranSiswa.getCalonSiswa() == null ? "" : pembayaranSiswa.getCalonSiswa().getNoRegistrasi()).setParent(arg0);
				RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, pembayaranSiswaDetail, pembayaranSiswa.getCalonSiswa() == null ? "" : pembayaranSiswa.getCalonSiswa().getNama()).setParent(arg0);
				new Label(pembayaranSiswaDetail.getItemBiayaSekolah().getNama()).setParent(arg0);
				new Label(pembayaranSiswaDetail.getNominal() == null ? "0" : Common.numberFormat.get().format(pembayaranSiswaDetail.getNominal())).setParent(arg0);
				new Label(pembayaranSiswa.getCalonSiswa() == null || pembayaranSiswa.getCalonSiswa().getSekolah() == null ? "" : pembayaranSiswa.getCalonSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(pembayaranSiswa.getCalonSiswa() == null || pembayaranSiswa.getCalonSiswa().getSekolah() == null || pembayaranSiswa.getCalonSiswa().getSekolah().getYayasan() == null ? "" : pembayaranSiswa.getCalonSiswa().getSekolah().getYayasan().getNama()).setParent(arg0);
			}

			new Label(pembayaranSiswaDetail.getTagihan() == null || pembayaranSiswaDetail.getTagihan().getTahun() == null ? "" : pembayaranSiswaDetail.getTagihan().getTahun().toString()).setParent(arg0);
			new Label(pembayaranSiswaDetail.getTagihan() == null || pembayaranSiswaDetail.getTagihan().getBulan() == null ? "" : pembayaranSiswaDetail.getTagihan().getBulan().toString()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar())).setParent(arg0);

			Akun akunKredit = pembayaranSiswaDetail.getItemBiayaSekolah().getAkunPiutang();
			Akun akunDebet = pembayaranSiswaDetail.getPembayaranSiswa() == null || pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa() == null ? null : pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkun();

			Date tglBayarInfo = pembayaranSiswaDetail.getPembayaranSiswa() != null ? pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar() : null;
			Date tglTagihanInfo = pembayaranSiswaDetail.getTagihan() != null ? pembayaranSiswaDetail.getTagihan().getTanggalTagihan() : null;

			// Perbaikan Bug pengecekan Waktu
			if (tglBayarInfo != null && tglTagihanInfo != null && tglBayarInfo.before(tglTagihanInfo) && pembayaranSiswaDetail.getItemBiayaSekolah().getAkunDibayarDimuka() != null) {
				akunDebet = pembayaranSiswaDetail.getItemBiayaSekolah().getAkunDibayarDimuka();
			}

			if (akunKredit != null && akunDebet != null) {
				Double denda = pembayaranSiswaDetail.getTagihan().getDenda() == null ? 0.0 : pembayaranSiswaDetail.getTagihan().getDenda();
				Akun akunDenda = pembayaranSiswaDetail.getItemBiayaSekolah().getAkunPiutangDenda();

				if (denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan. "
							+ CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
									"item biaya sekolah \"" + pembayaranSiswaDetail.getItemBiayaSekolah().getNama() + "\"",
									"Akun Piutang Denda"))
							.setParent(arg0);
				} else {
					Double nominalTampil = pembayaranSiswaDetail.getNominal() == null ? 0.0 : pembayaranSiswaDetail.getNominal();
					Double tabunganUtuhTampil = pembayaranSiswaDetail.getPembayaranSiswa().getDariTabungan() == null ? 0.0 : pembayaranSiswaDetail.getPembayaranSiswa().getDariTabungan();
					
					int totalDetailList = 1;
					if (pembayaranSiswa.getId() != null) {
						Number cInfo = (Number) HibernateUtil.currentSession().createCriteria(PembayaranSiswaDetail.class).add(Restrictions.eq("pembayaranSiswa.id", pembayaranSiswa.getId())).setProjection(Projections.rowCount()).uniqueResult();
						if (cInfo != null && cInfo.intValue() > 0) totalDetailList = cInfo.intValue();
					}
					
					Double proporsiTabunganTampil = (tabunganUtuhTampil > 0 && totalDetailList > 0) ? (tabunganUtuhTampil / totalDetailList) : 0.0;

					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Akun> akunsKredits = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();

					if (proporsiTabunganTampil > 0.1 && pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa() != null && pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkunDeposit() != null) {
						akunsDebets.add(pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getAkunDeposit());
						nilaiDebets.add(proporsiTabunganTampil);
						nominalTampil = nominalTampil - proporsiTabunganTampil;
					}

					akunsDebets.add(akunDebet);
					nilaiDebets.add(nominalTampil);

					akunsKredits.add(akunKredit);
					nilaiKredits.add((pembayaranSiswaDetail.getNominal() == null ? 0.0 : pembayaranSiswaDetail.getNominal()) - denda); 
					if (denda > 0.1 && akunDenda != null) {
						akunsKredits.add(akunDenda);
						nilaiKredits.add(denda);
					}
					GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);
				}
			} else {
				// Sekolah: debet dari Akun Pembayaran Siswa, kredit dari Akun Piutang ItemBiayaSekolah.
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.langkahLengkapiKolomAkun("Akun Pembayaran Siswa",
								"Akun Pembayaran Siswa yang dipakai", "Akun"),
						CommonAkunting.langkahLengkapiKolomAkun("Item Biaya Sekolah",
								"item biaya sekolah \"" + pembayaranSiswaDetail.getItemBiayaSekolah().getNama() + "\"",
								"Akun Piutang")));
			}

			new Label(pembayaranSiswaDetail.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting") : pembayaranSiswaDetail.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PembayaranSiswaUtil.cetakStruk(pembayaranSiswaDetail.getPembayaranSiswa());
				}
			});
			button.setParent(toolbar);

			if (akunDebet != null && akunKredit != null) {
				button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pembayaranSiswaDetail.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								pembayaranSiswaDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranSiswaDetail);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where pembayaran_siswa_detail=" + pembayaranSiswaDetail.getId() + " and closing is null")
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
				button.setVisible(edit && pembayaranSiswaDetail.getPostingHistory() == null && pembayaranSiswaDetail.getTagihan().getPengaturanBiaya().getSekolah().getSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();
								
								try {
									session.getTransaction().begin();
									PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PEMBAYARAN_SISWA);
									postingHistory.setTbmuser(Common.getCurrentUser());
									postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
									postingHistory.setKeterangan("Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
									session.save(postingHistory);
	
									int totalDetail = 1;
									if (pembayaranSiswa.getId() != null) {
										Number cDetail = (Number) session.createCriteria(PembayaranSiswaDetail.class).add(Restrictions.eq("pembayaranSiswa.id", pembayaranSiswa.getId())).setProjection(Projections.rowCount()).uniqueResult();
										if (cDetail != null && cDetail.intValue() > 0) totalDetail = cDetail.intValue();
									}
	
									eksekusiPosting(session, pembayaranSiswaDetail, postingHistory, totalDetail);
									session.getTransaction().commit();
								} catch (Exception e) {
									session.getTransaction().rollback();
									ais.common.Common.tampilErrorJikaAdmin(e);
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
		Yayasan yayasan = searchyayasan.getSelectedItem() == null ? null : (Yayasan) searchyayasan.getSelectedItem().getValue();
		Sekolah sekolah = searchsekolah.getSelectedItem() == null ? null : (Sekolah) searchsekolah.getSelectedItem().getValue();

		Criteria criteria = session.createCriteria(PembayaranSiswaDetail.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))
				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("itemBiayaSekolah", searchitembiaya.getSelectedItem().getValue()))
				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistory") : !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1") : Restrictions.isNull("postingHistory"))
				.createAlias("pembayaranSiswa", "pembayaranSiswa").createAlias("tagihan", "tagihan")
				.add(searchjenispembayaran.getSelectedItem() == null || searchjenispembayaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pembayaranSiswa.jenisBiayaSekolah", searchjenispembayaran.getSelectedItem().getValue()))
				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pembayaranSiswa.akunPembayaranSiswa", searchjenis.getSelectedItem().getValue()))
				.add((searchbulan.getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tagihan.bulan", searchbulan.getValue().intValue())))
				.add((searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tagihan.tahun", searchtahun.getValue().intValue())));

		// Optimalisasi Filter Waktu Menjadi Paramater Date Asli (Bukan String Concatenation yg lambat)
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(tglMulai.getValue());
		startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0);

		Calendar endCal = Calendar.getInstance();
		endCal.setTime(tglSampai.getValue());
		endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);

		criteria.add(Restrictions.between("pembayaranSiswa.tanggalBayar", startCal.getTime(), endCal.getTime()));

		if (order) criteria.addOrder(Order.desc("pembayaranSiswa.id"));
		criteria.createAlias("pembayaranSiswa.siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("pembayaranSiswa.calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.add(searchtahunMasuk.getValue() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.or(Restrictions.eq("siswa.tahunMasuk", searchtahunMasuk.getValue().intValue()), Restrictions.eq("calonSiswa.tahunMasuk", searchtahunMasuk.getValue().intValue())))
				.add(sekolah == null ? Restrictions.sqlRestriction("1=1") : Restrictions.or(Restrictions.eq("siswa.sekolah", sekolah), Restrictions.eq("calonSiswa.sekolah", sekolah)))
				.add(yayasan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.or(Restrictions.eq("siswa.yayasan", yayasan), Restrictions.eq("calonSiswa.yayasan", yayasan)));

		// Penyederhanaan Disjunction (OR) string filter
		String searchValue = searchsiswa.getValue().trim();
		if (!searchValue.isEmpty()) {
			Disjunction dis = Restrictions.disjunction();
			dis.add(Restrictions.ilike("siswa.nomorIndukNasional", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("siswa.nama", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("calonSiswa.nama", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("siswa.nomorInduk", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("calonSiswa.nomorInduk", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("calonSiswa.noRegistrasi", searchValue, MatchMode.ANYWHERE));
			dis.add(Restrictions.ilike("calonSiswa.noUjian", searchValue, MatchMode.ANYWHERE));
			criteria.add(dis);
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.initPaging(initCriteria(false), paging);
				List<PembayaranSiswaDetail> pembayaranSiswaDetail = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(pembayaranSiswaDetail);
				grid.setRowRenderer(new PembayaranSiswaDetailRenderer());
				grid.setModelCheckMobile(strset);
			}
		});
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
	// Logika jurnalnya TIDAK disalin: {@link #eksekusiPosting} dipakai bersama
	// oleh layar ZK dan jalur ini, sehingga tidak mungkin menyimpang.
	// =====================================================================

	/**
	 * Kriteria pembayaran siswa yang layak diposting pada rentang tanggal.
	 * Sama persis dengan penghitung baris "Siswa - Pembayaran" pada dasbor
	 * ({@code DraftJurnalRingkasanUtil.kriteriaPembayaranSiswaDetail}).
	 */
	private static Criteria kriteriaPostingStatic(Session session, Date mulai, Date sampai) {
		Criteria c = session.createCriteria(PembayaranSiswaDetail.class)
				.createAlias("pembayaranSiswa", "pembayaranSiswa").createAlias("tagihan", "tagihan");
		if (mulai != null && sampai != null) {
			c.add(Restrictions.between("pembayaranSiswa.tanggalBayar", mulai, sampai));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA pembayaran siswa terposting dalam rentang.
	 *
	 * <p>Memakai {@code currentNativeSession()} dengan transaksi eksplisit per dokumen; baris
	 * {@code akunting.transaksi} dihapus lebih dulu karena {@code grup_transaksi} adalah
	 * induknya. Jurnal yang SUDAH closing tidak ikut dihapus.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(Date mulai, Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<PembayaranSiswaDetail> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (PembayaranSiswaDetail detail : daftar) {
				try {
					String syarat = "pembayaran_siswa_detail=" + detail.getId() + " and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					detail.setPostingHistory(null);
					session.update(detail);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingCicilanSiswaAction jalur API");
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
	 * Posting SEMUA pembayaran siswa yang belum diposting dalam rentang.
	 *
	 * <p>Nilai jurnalnya dihitung {@link #eksekusiPosting} -- method yang SAMA dengan yang
	 * dipakai tombol di layar, termasuk pemecahan nilai tabungan menurut jumlah baris detail
	 * pada satu pembayaran dan pemindahan ke akun "dibayar di muka" bila tanggal bayarnya
	 * mendahului tanggal tagihan.</p>
	 *
	 * <p><b>Penyimpangan sadar:</b> tiap dokumen dijalankan dalam transaksinya SENDIRI. Layar
	 * membungkus seluruh batch dalam satu transaksi, sehingga satu dokumen bermasalah
	 * membatalkan ribuan dokumen lain yang sudah benar. Di sini yang gagal hanya dirinya
	 * sendiri, dicatat ke Error Log, dan sisanya tetap diproses.</p>
	 *
	 * @return jumlah dokumen yang BERHASIL diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PEMBAYARAN_SISWA);
			postingHistory.setTanggal(tglPosting == null ? new Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pembayaran siswa dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Long id : ids) {
				try {
					session = HibernateUtil.currentNativeSession();
					PembayaranSiswaDetail detail = (PembayaranSiswaDetail) session
							.createCriteria(PembayaranSiswaDetail.class).add(Restrictions.idEq(id))
							.uniqueResult();
					if (detail == null || detail.getPembayaranSiswa() == null
							|| detail.getItemBiayaSekolah() == null) {
						continue;
					}
					// Nilai tabungan dibagi rata ke seluruh baris detail pada satu pembayaran --
					// hitungannya sama dengan layar.
					int totalDetail = 1;
					Number jml = (Number) session.createCriteria(PembayaranSiswaDetail.class)
							.add(Restrictions.eq("pembayaranSiswa", detail.getPembayaranSiswa()))
							.setProjection(Projections.rowCount()).uniqueResult();
					if (jml != null && jml.intValue() > 0) {
						totalDetail = jml.intValue();
					}

					session.getTransaction().begin();
					eksekusiPosting(session, detail, postingHistory, totalDetail);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingCicilanSiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingCicilanSiswaAction jalur API");
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