package ais.action.master;

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
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Deposit;
import ais.database.model.Fakultas;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisTabungan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk posting deposit. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchnama}, {@code Textbox searchnamamhs}, {@code Combobox searchfakultas}, {@code
 * MyCheckboxConfig searchtampil}, {@code MyCheckboxConfig searchtelahtampil}, {@code Combobox searchjurusan};
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
public class PostingDepositAction extends GenericAutowireComposer {

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

	private Combobox searchjurusan;
	private Combobox searchjenistabungan;
	private Combobox searchjenispembayaran;
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

		Common.insertComboDanSemua(searchjenistabungan, new String[] { "nama", "kode" }, "akun", JenisTabungan.class,
				Restrictions.eq("aktif", true));
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertComboDanSemua(searchjenispembayaran, new String[] { "nama", "kode" }, "akun",
				JenisPembayaran.class,
				Restrictions.and(satuanKerja == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

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
							List<Deposit> deposits = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (Deposit deposit : deposits) {
								deposit.setPostingHistory(null);
								Common.refreshSaveOrUpdate(deposit);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where deposit=" + deposit.getId() + " and closing is null")
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
		addWindow.setTitle("Posting Tabungan Mahasiswa");
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
		final MyDatebox waktu;
		row.appendChild(waktu = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		waktu.setWidth("90%");

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

				final Date tgl = waktu.getValue();
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi tabungan ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi tabungan berhasil dilakukan",
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

											List<Deposit> deposits = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											SatuanKerja s = (SatuanKerja) PostingDepositAction.this.satuanKerja
													.getAttribute("satuanKerja");

											int rowIndex = 1;
											for (Deposit deposit : deposits) {

												SatuanKerja satuanKerja = (SatuanKerja) (deposit != null
														&& deposit.getMahasiswa() != null
														&& deposit.getMahasiswa().getJurusan() != null
														&& deposit.getMahasiswa().getJurusan().getFakultas() != null
														&& deposit.getMahasiswa().getJurusan().getFakultas()
																.getSatuanKerja() != null
																		? deposit.getMahasiswa().getJurusan()
																				.getFakultas().getSatuanKerja()
																		: s);

												if (s != null) {
													satuanKerja = s;
												}

												if (deposit != null && deposit.getJenisTabungan() != null) {

													try {
														Akun akunKredit = deposit.getJenisTabungan().getAkun();
														Akun akunDebet = deposit.getJenisPembayaran().getAkun();
														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																if (deposit.getMahasiswa() != null) {

																	Mahasiswa mahasiswa = (deposit
																			.getMahasiswa() == null ? null
																					: deposit.getMahasiswa());

																	ket = "Tabungan "
																			+ (mahasiswa == null ? ""
																					: mahasiswa.getNim() + "-"
																							+ mahasiswa.getNama())
																			+ " - "
																			+ deposit.getJenisTabungan().getNama()
																			+ " - " + deposit.getKeterangan();
																} else if (deposit.getBiodataCalonMahasiswa() != null) {
																	BiodataCalonMahasiswa biodataCalonMahasiswa = deposit
																			.getBiodataCalonMahasiswa();
																	ket = "Tabungan "
																			+ (biodataCalonMahasiswa == null ? ""
																					: biodataCalonMahasiswa
																							.getNoRegistrasi()
																							+ "-"
																							+ biodataCalonMahasiswa
																									.getNama())
																			+ " - "
																			+ deposit.getJenisTabungan().getNama()
																			+ " - " + deposit.getKeterangan();
																}

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get()
																			.format(rowIndex * 100.0 / deposits.size())
																	+ " %)");

															Double nilai = deposit.getNominal();
															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, deposit.getWaktu(),
																			nilai, denda, deposit, satuanKerja,
																			session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, deposit.getWaktu(),
																			nilai, denda, deposit, satuanKerja,
																			session);
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
																} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingDepositAction.java:441");
																}
																continue;
															}

															deposit.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(deposit);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PostingDepositAction.java:451");
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

	/**
	 * Renderer lokal untuk layar/komponen {@link PostingDepositAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingDepositAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingDepositAction
	 */
	class DepositRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Deposit deposit = (Deposit) arg1;

			if (deposit.getMahasiswa() != null) {
				new Label(deposit.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper.createNewRevisi(Deposit.class, deposit, deposit.getMahasiswa().getNama()).setParent(arg0);

				new Label(deposit.getJenisTabungan().getNama()).setParent(arg0);

				new Label(deposit.getNominal() == null ? "0" : Common.numberFormat.get().format(deposit.getNominal()))
						.setParent(arg0);

				new Label(deposit.getMahasiswa() == null || deposit.getMahasiswa().getJurusan() == null ? ""
						: deposit.getMahasiswa().getJurusan().getNama()).setParent(arg0);
				new Label(deposit.getMahasiswa() == null || deposit.getMahasiswa().getJurusan() == null
						|| deposit.getMahasiswa().getJurusan().getFakultas() == null ? ""
								: deposit.getMahasiswa().getJurusan().getFakultas().getNama())
						.setParent(arg0);
			} else if (deposit.getBiodataCalonMahasiswa() != null) {
				new Label(deposit.getBiodataCalonMahasiswa().getNoRegistrasi()).setParent(arg0);

				RevisiHelper.createNewRevisi(Deposit.class, deposit, deposit.getBiodataCalonMahasiswa().getNama())
						.setParent(arg0);

				new Label(deposit.getJenisTabungan().getNama()).setParent(arg0);

				new Label(deposit.getNominal() == null ? "0" : Common.numberFormat.get().format(deposit.getNominal()))
						.setParent(arg0);

				Jurusan jurusan = deposit.getBiodataCalonMahasiswa().getProdi1();
				if (deposit.getBiodataCalonMahasiswa().getProdiLulus() != null) {
					jurusan = deposit.getBiodataCalonMahasiswa().getProdiLulus();
				}

				new Label(jurusan == null ? "" : jurusan.getNama()).setParent(arg0);
				new Label(jurusan == null || jurusan.getFakultas() == null ? "" : jurusan.getFakultas().getNama())
						.setParent(arg0);
			}

			new Label(Common.dateFormat3.get().format(deposit.getWaktu())).setParent(arg0);
			Akun akunKredit = deposit.getJenisTabungan().getAkun();
			Akun akunDebet = deposit.getJenisPembayaran().getAkun();
			if (akunDebet != null && akunKredit != null) {
				Double nilai = deposit.getNominal();

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				// Tabungan: debet dari Jenis Pembayaran, kredit dari Jenis Tabungan — bukan ItemBiaya.
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.langkahLengkapiKolomAkun("Jenis Pembayaran",
								"Jenis Pembayaran \"" + deposit.getJenisPembayaran().getNama() + "\"", "Akun"),
						CommonAkunting.langkahLengkapiKolomAkun("Jenis Tabungan",
								"Jenis Tabungan \"" + deposit.getJenisTabungan().getNama() + "\"", "Akun")));
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("deposit", deposit))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(deposit.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: deposit.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan Posting", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && deposit.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								deposit.setPostingHistory(null);
								Common.refreshSaveOrUpdate(deposit);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where deposit=" + deposit.getId() + " and closing is null")
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
				button.setVisible(edit && deposit.getPostingHistory() == null && tbmuser != null);
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

								Akun akunKredit = deposit.getJenisTabungan().getAkun();
								Akun akunDebet = deposit.getJenisPembayaran().getAkun();
								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										if (deposit.getMahasiswa() != null) {

											Mahasiswa mahasiswa = (deposit.getMahasiswa() == null ? null
													: deposit.getMahasiswa());

											ket = "Tabungan "
													+ (mahasiswa == null ? ""
															: mahasiswa.getNim() + "-" + mahasiswa.getNama())
													+ " - " + deposit.getJenisTabungan().getNama() + " - "
													+ deposit.getKeterangan();
										} else if (deposit.getBiodataCalonMahasiswa() != null) {
											BiodataCalonMahasiswa biodataCalonMahasiswa = deposit
													.getBiodataCalonMahasiswa();
											ket = "Tabungan "
													+ (biodataCalonMahasiswa == null ? ""
															: biodataCalonMahasiswa.getNoRegistrasi() + "-"
																	+ biodataCalonMahasiswa.getNama())
													+ " - " + deposit.getJenisTabungan().getNama() + " - "
													+ deposit.getKeterangan();
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Double nilai = deposit.getNominal();

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (deposit != null
											&& deposit.getMahasiswa() != null
											&& deposit.getMahasiswa().getJurusan() != null
											&& deposit.getMahasiswa().getJurusan().getFakultas() != null
											&& deposit.getMahasiswa().getJurusan().getFakultas()
													.getSatuanKerja() != null
															? deposit.getMahasiswa().getJurusan().getFakultas()
																	.getSatuanKerja()
															: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, deposit.getWaktu(), nilai, denda,
												deposit, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, deposit.getWaktu(), nilai, denda,
												deposit, satuanKerja, session);
									}

									deposit.setPostingHistory(postingHistory);
									session.update(deposit);
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

		Criteria criteria = session.createCriteria(Deposit.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
						Restrictions.isNotNull("biodataCalonMahasiswa")))

				.add(Restrictions.isNotNull("waktu"))
				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("nominal", 0.0)))

				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("nominal")))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.waktu) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchjenistabungan.getSelectedItem() == null
						|| searchjenistabungan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisTabungan", searchjenistabungan.getSelectedItem().getValue()))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistory")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(searchjenispembayaran.getSelectedItem() == null
						|| searchjenispembayaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPembayaran",
										searchjenispembayaran.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.createAlias("biodataCalonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

				.createAlias("biodataCalonMahasiswa.prodi1", "prodi1", Criteria.LEFT_JOIN)

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")

						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.or(Restrictions.eq("biodataCalonMahasiswa.prodi1", jurusan),
										Restrictions.eq("biodataCalonMahasiswa.prodiLulus", jurusan)))

				)

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.or(Restrictions.eq("prodi1.fakultas", fakultas),
										Restrictions.eq("prodiLulus.fakultas", fakultas))))

				.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Deposit> deposit = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(deposit);
		grid.setRowRenderer(new DepositRenderer());
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
	 * Kriteria tabungan/deposit mahasiswa pada rentang tanggal -- sama dengan penghitung baris
	 * "Mahasiswa - Tabungan/Deposit" pada dasbor
	 * ({@code DraftJurnalRingkasanUtil.kriteriaDepositMahasiswa}).
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(Deposit.class)
				.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
						Restrictions.isNotNull("biodataCalonMahasiswa")))
				.add(Restrictions.isNotNull("waktu"))
				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"),
						Restrictions.ne("nominal", 0.0)))
				.add(Restrictions.or(Restrictions.isNotNull("postingHistory"),
						Restrictions.isNotNull("nominal")));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.waktu) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/** Batalkan posting SEMUA tabungan/deposit mahasiswa terposting dalam rentang. */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Deposit> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Deposit deposit : daftar) {
				try {
					String syarat = "deposit=" + deposit.getId() + " and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					deposit.setPostingHistory(null);
					session.update(deposit);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingDepositAction jalur API");
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
	 * Posting SEMUA tabungan/deposit mahasiswa yang belum dijurnal dalam rentang.
	 *
	 * <p>Debet = akun jenis pembayaran (kas/bank yang menerima), kredit = akun jenis tabungan
	 * (titipan mahasiswa). Nilai dari {@code nominal}, tanggal jurnal dari {@code waktu}; bila
	 * nilainya &le; 0,1 posisi ditukar.</p>
	 *
	 * <p><b>Satuan kerja.</b> Layar memakai penyaring di halaman bila diisi, dan fakultas
	 * mahasiswanya bila tidak. Dari API penyaring itu tidak ada, jadi urutannya: satuan kerja
	 * FAKULTAS mahasiswa, lalu satuan kerja pengguna yang memposting sebagai cadangan.</p>
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
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal tabungan mahasiswa dari dasbor jurnal"
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
					Deposit deposit = (Deposit) session.createCriteria(Deposit.class)
							.add(Restrictions.idEq(id)).uniqueResult();
					if (deposit == null || deposit.getJenisTabungan() == null
							|| deposit.getJenisPembayaran() == null) {
						continue;
					}
					Akun akunKredit = deposit.getJenisTabungan().getAkun();
					Akun akunDebet = deposit.getJenisPembayaran().getAkun();
					if (akunDebet == null || akunKredit == null) {
						continue;
					}
					SatuanKerja satuanKerja = deposit.getMahasiswa() != null
							&& deposit.getMahasiswa().getJurusan() != null
							&& deposit.getMahasiswa().getJurusan().getFakultas() != null
							&& deposit.getMahasiswa().getJurusan().getFakultas().getSatuanKerja() != null
									? deposit.getMahasiswa().getJurusan().getFakultas().getSatuanKerja()
									: satuanKerjaPengguna;

					String ket;
					if (deposit.getMahasiswa() != null) {
						ket = "Tabungan " + deposit.getMahasiswa().getNim() + "-"
								+ deposit.getMahasiswa().getNama() + " - "
								+ deposit.getJenisTabungan().getNama() + " - " + deposit.getKeterangan();
					} else if (deposit.getBiodataCalonMahasiswa() != null) {
						ket = "Tabungan " + deposit.getBiodataCalonMahasiswa().getNoRegistrasi() + "-"
								+ deposit.getBiodataCalonMahasiswa().getNama() + " - "
								+ deposit.getJenisTabungan().getNama() + " - " + deposit.getKeterangan();
					} else {
						ket = "Tabungan - " + deposit.getJenisTabungan().getNama() + " - "
								+ deposit.getKeterangan();
					}
					Double nilai = deposit.getNominal();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									true, ket, deposit.getWaktu(), nilai, Double.valueOf(0.0), deposit,
									satuanKerja, session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									true, ket, deposit.getWaktu(), nilai, Double.valueOf(0.0), deposit,
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
						ais.common.ErrorAuditUtil.record(e, "PostingDepositAction jalur API");
					}

					if (tersimpan) {
						deposit.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(deposit);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingDepositAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingDepositAction jalur API");
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
