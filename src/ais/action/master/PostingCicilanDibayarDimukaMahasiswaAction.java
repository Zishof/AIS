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
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk posting cicilan dibayar dimuka mahasiswa. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchnama}, {@code Textbox searchnamamhs}, {@code Combobox searchfakultas}, {@code
 * MyCheckboxConfig searchtampil}, {@code MyCheckboxConfig searchtelahtampil}, {@code Combobox jenissemester};
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
public class PostingCicilanDibayarDimukaMahasiswaAction extends GenericAutowireComposer {

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

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran dibayar dimuka ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<CicilanPembayaran> cicilanPembayarans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistoryDimuka")).list();

							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								cicilanPembayaran.setPostingHistoryDimuka(null);
								Common.refreshSaveOrUpdate(cicilanPembayaran);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where cicilan_pembayaran="
												+ cicilanPembayaran.getId() + " and ref='dimuka'" + " and closing is null")
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pembayaran dibayar dimuka ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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
													PostingHistory.JENIS_MAHASISWA_DIBAYAR_DIMUKA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<CicilanPembayaran> cicilanPembayarans = initCriteria(true)
													.add(Restrictions.isNull("postingHistoryDimuka")).list();

											SatuanKerja s = (SatuanKerja) PostingCicilanDibayarDimukaMahasiswaAction.this.satuanKerja
													.getAttribute("satuanKerja");

											int rowIndex = 1;
											for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {

												SatuanKerja satuanKerja = (SatuanKerja) (cicilanPembayaran
														.getKegiatan() != null
														&& cicilanPembayaran.getKegiatan().getMahasiswa() != null
														&& cicilanPembayaran.getKegiatan().getMahasiswa()
																.getJurusan() != null
														&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan()
																.getFakultas() != null
														&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan()
																.getFakultas().getSatuanKerja() != null
																		? cicilanPembayaran.getKegiatan().getMahasiswa()
																				.getJurusan().getFakultas()
																				.getSatuanKerja()
																		: s);

												if (cicilanPembayaran.getKegiatan() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdiLulus() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdiLulus().getFakultas() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdiLulus().getFakultas()
																.getSatuanKerja() != null) {
													satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa()
															.getProdiLulus().getFakultas().getSatuanKerja();
												} else if (cicilanPembayaran.getKegiatan() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdiLulus() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdi1().getFakultas() != null
														&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getProdi1().getFakultas().getSatuanKerja() != null) {
													satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa()
															.getProdi1().getFakultas().getSatuanKerja();
												}

												if (s != null) {
													satuanKerja = s;
												}

												if (cicilanPembayaran != null
														&& cicilanPembayaran.getItemBiaya() != null) {

													try {
														Akun akunDebet = cicilanPembayaran.getItemBiaya()
																.ambilDibayarDimuka(cicilanPembayaran.getKegiatan());
														Akun akunPiutang = cicilanPembayaran.getItemBiaya()
																.ambilPiutang(cicilanPembayaran.getKegiatan());
														Akun akunKredit = akunPiutang != null ? akunPiutang
																: cicilanPembayaran.getItemBiaya()
																		.ambilAkun(cicilanPembayaran.getKegiatan());

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan()
																		.getMahasiswa() == null
																				? (cicilanPembayaran.getKegiatan()
																						.getCalonMahasiswa() == null
																								? null
																								: cicilanPembayaran
																										.getKegiatan()
																										.getCalonMahasiswa()
																										.getMahasiswa())
																				: cicilanPembayaran.getKegiatan()
																						.getMahasiswa());

																ket = "Pembayaran "
																		+ (mahasiswa == null
																				? cicilanPembayaran.getKegiatan()
																						.getCalonMahasiswa()
																				: mahasiswa)
																		+ " ke " + cicilanPembayaran.getKe() + " - "
																		+ cicilanPembayaran.getItemBiaya().getNama()
																		+ " - " + cicilanPembayaran.getKeterangan();

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															if (cicilanPembayaran
																	.getPengaturanPembayaranBulanan() != null) {
																PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
																		.getPengaturanPembayaranBulanan();
																ket = pengaturanPembayaranBulanan.getKeterangan();
																ket = (ket.isEmpty()
																		? (pengaturanPembayaranBulanan.getDetailBiaya()
																				.getItemBiaya().getNama())
																		: ket) + ", bulan "
																		+ pengaturanPembayaranBulanan.getNamaBulan();

																try {
																	Mahasiswa mahasiswa = (cicilanPembayaran
																			.getKegiatan().getMahasiswa() == null
																					? (cicilanPembayaran.getKegiatan()
																							.getCalonMahasiswa() == null
																									? null
																									: cicilanPembayaran
																											.getKegiatan()
																											.getCalonMahasiswa()
																											.getMahasiswa())
																					: cicilanPembayaran.getKegiatan()
																							.getMahasiswa());

																	ket = "Pembayaran " + (mahasiswa == null
																			? cicilanPembayaran.getKegiatan()
																					.getCalonMahasiswa()
																			: mahasiswa) + " " + ket;
																} catch (Exception e) {
																	Common.tampilErrorJikaAdmin(e);
																}
															}

															label.setValue(
																	ket + " ("
																			+ Common.numberFormat.get().format(rowIndex
																					* 100.0 / cicilanPembayarans.size())
																			+ " %)");

															Double nilai = cicilanPembayaran.getNilai();
															try {

																Akun akunDenda = null;
																Double denda = cicilanPembayaran.getDenda();
																if (denda != null && denda > 0.1) {
																	akunDenda = cicilanPembayaran.getItemBiaya()
																			.ambilPendapatanDenda(
																					cicilanPembayaran.getKegiatan());
																}

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutang, postingHistory,
																			apakahUangMasuk, ket,
																			cicilanPembayaran.getTanggalTagihan(),
																			nilai, denda, cicilanPembayaran,
																			satuanKerja, "dimuka", session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutang, postingHistory,
																			apakahUangMasuk, ket,
																			cicilanPembayaran.getTanggalTagihan(),
																			nilai, denda, cicilanPembayaran,
																			satuanKerja, "dimuka", session);
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
																} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanDibayarDimukaMahasiswaAction.java:517");
																}
																continue;
															}

															cicilanPembayaran.setPostingHistoryDimuka(postingHistory);
															session.getTransaction().begin();
															Common.refreshUpdate(session, cicilanPembayaran);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PostingCicilanDibayarDimukaMahasiswaAction.java:527");
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
	 * Renderer lokal untuk layar/komponen {@link PostingCicilanDibayarDimukaMahasiswaAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingCicilanDibayarDimukaMahasiswaAction}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingCicilanDibayarDimukaMahasiswaAction
	 */
	class CicilanPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) arg1;
			final Kegiatan kegiatan = cicilanPembayaran.getKegiatan();

			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
							.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(cicilanPembayaran.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(cicilanPembayaran.getNilai() == null ? "0"
						: Common.numberFormat.get().format(cicilanPembayaran.getNilai())).setParent(arg0);

				// new Label(kegiatan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(kegiatan.getAmount()))
				// .setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
						: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);

			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNim())
						.setParent(arg0);
				RevisiHelper
						.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
							.getPengaturanPembayaranBulanan();
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan();
					new Label(desc).setParent(arg0);
				} else {
					new Label(cicilanPembayaran.getItemBiaya().getNama()).setParent(arg0);
				}

				new Label(cicilanPembayaran.getNilai() == null ? "0"
						: Common.numberFormat.get().format(cicilanPembayaran.getNilai())).setParent(arg0);

				// new Label(kegiatan.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(kegiatan.getAmount()))
				// .setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						? ""
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

			Akun akunDebet = cicilanPembayaran.getItemBiaya().ambilDibayarDimuka(cicilanPembayaran.getKegiatan());

			Akun akunPiutang = cicilanPembayaran.getItemBiaya().ambilPiutang(cicilanPembayaran.getKegiatan());
			Akun akunKredit = akunPiutang != null ? akunPiutang
					: cicilanPembayaran.getItemBiaya().ambilAkun(cicilanPembayaran.getKegiatan());

			if (akunDebet != null && akunKredit != null) {
				Double nilai = cicilanPembayaran.getNilai();

				Akun akunDenda = null;
				Double denda = cicilanPembayaran.getDenda();
				if (denda != null && denda > 0.1) {
					akunDenda = cicilanPembayaran.getItemBiaya().ambilPendapatanDenda(cicilanPembayaran.getKegiatan());
				}

				if (denda != null && denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan. "
							+ CommonAkunting.hintPemetaanItemBiaya(cicilanPembayaran.getItemBiaya(),
									cicilanPembayaran.getKegiatan(), "Akun Pendapatan Denda"))
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
				// Debet DAN kredit di sini sama-sama di-resolve via pemetaan ItemBiaya (Akun
				// Dibayar Dimuka utk debet, Akun Piutang/Pendapatan utk kredit) — BUKAN pola
				// debet=Jenis Pembayaran seperti overload (Akun,Akun,ItemBiaya,Kegiatan), jadi
				// pakai overload generik dengan hint sendiri utk kedua sisi.
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.hintPemetaanItemBiaya(cicilanPembayaran.getItemBiaya(),
								cicilanPembayaran.getKegiatan(), "Akun Pendapatan Dibayar Dimuka"),
						CommonAkunting.hintPemetaanItemBiaya(cicilanPembayaran.getItemBiaya(),
								cicilanPembayaran.getKegiatan(), "Akun Pendapatan\" atau \"Akun Piutang")), cicilanPembayaran.getItemBiaya(), cicilanPembayaran.getKegiatan());
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("cicilanPembayaran", cicilanPembayaran)).setMaxResults(1)
					.add(Restrictions.eq("ref", "dimuka")).setProjection(Projections.property("kode")).uniqueResult();

			new Label(cicilanPembayaran.getPostingHistoryDimuka() == null ? Common.getBahasaConfig("Belum diposting")
					: cicilanPembayaran.getPostingHistoryDimuka().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan Posting", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && cicilanPembayaran.getPostingHistoryDimuka() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								cicilanPembayaran.setPostingHistoryDimuka(null);
								Common.refreshSaveOrUpdate(cicilanPembayaran);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where ref='dimuka' and cicilan_pembayaran="
												+ cicilanPembayaran.getId() + " and closing is null")
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
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Posting", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && cicilanPembayaran.getPostingHistoryDimuka() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_MAHASISWA_DIBAYAR_DIMUKA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = cicilanPembayaran.getItemBiaya()
										.ambilDibayarDimuka(cicilanPembayaran.getKegiatan());

								Akun akunPiutang = cicilanPembayaran.getItemBiaya()
										.ambilPiutang(cicilanPembayaran.getKegiatan());
								Akun akunKredit = akunPiutang != null ? akunPiutang
										: cicilanPembayaran.getItemBiaya().ambilAkun(cicilanPembayaran.getKegiatan());

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {
										Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan().getMahasiswa() == null
												? (cicilanPembayaran.getKegiatan().getCalonMahasiswa() == null ? null
														: cicilanPembayaran.getKegiatan().getCalonMahasiswa()
																.getMahasiswa())
												: cicilanPembayaran.getKegiatan().getMahasiswa());

										ket = "Pembayaran "
												+ (mahasiswa == null
														? cicilanPembayaran.getKegiatan().getCalonMahasiswa()
														: mahasiswa)
												+ " ke " + cicilanPembayaran.getKe() + " - "
												+ cicilanPembayaran.getItemBiaya().getNama() + " - "
												+ cicilanPembayaran.getKeterangan();

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
												.getPengaturanPembayaranBulanan();
										ket = pengaturanPembayaranBulanan.getKeterangan();
										ket = (ket.isEmpty()
												? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
														.getNama())
												: ket) + ", bulan " + pengaturanPembayaranBulanan.getNamaBulan();

										try {
											Mahasiswa mahasiswa = (cicilanPembayaran.getKegiatan()
													.getMahasiswa() == null
															? (cicilanPembayaran.getKegiatan()
																	.getCalonMahasiswa() == null
																			? null
																			: cicilanPembayaran.getKegiatan()
																					.getCalonMahasiswa().getMahasiswa())
															: cicilanPembayaran.getKegiatan().getMahasiswa());

											ket = "Pembayaran " + (mahasiswa == null
													? cicilanPembayaran.getKegiatan().getCalonMahasiswa()
													: mahasiswa) + " " + ket;
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

									}
									Double nilai = cicilanPembayaran.getNilai();

									Akun akunDenda = null;
									Double denda = cicilanPembayaran.getDenda();
									if (denda != null && denda > 0.1) {
										akunDenda = cicilanPembayaran.getItemBiaya()
												.ambilPendapatanDenda(cicilanPembayaran.getKegiatan());
									}

									SatuanKerja satuanKerja = (SatuanKerja) (cicilanPembayaran.getKegiatan() != null
											&& cicilanPembayaran.getKegiatan().getMahasiswa() != null
											&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan() != null
											&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan()
													.getFakultas() != null
											&& cicilanPembayaran.getKegiatan().getMahasiswa().getJurusan().getFakultas()
													.getSatuanKerja() != null
															? cicilanPembayaran.getKegiatan().getMahasiswa()
																	.getJurusan().getFakultas().getSatuanKerja()
															: tbmuser.ambilSatuanKerja());

									if (cicilanPembayaran.getKegiatan() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
													.getProdiLulus() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus()
													.getFakultas() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdiLulus()
													.getFakultas().getSatuanKerja() != null) {
										satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa()
												.getProdiLulus().getFakultas().getSatuanKerja();
									} else if (cicilanPembayaran.getKegiatan() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa()
													.getProdiLulus() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1()
													.getFakultas() != null
											&& cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1()
													.getFakultas().getSatuanKerja() != null) {
										satuanKerja = cicilanPembayaran.getKegiatan().getCalonMahasiswa().getProdi1()
												.getFakultas().getSatuanKerja();
									}

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutang,
												postingHistory, apakahUangMasuk, ket,
												cicilanPembayaran.getTanggalTagihan(), nilai, denda, cicilanPembayaran,
												satuanKerja, "dimuka", session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutang,
												postingHistory, apakahUangMasuk, ket,
												cicilanPembayaran.getTanggalTagihan(), nilai, denda, cicilanPembayaran,
												satuanKerja, "dimuka", session);
									}

									cicilanPembayaran.setPostingHistoryDimuka(postingHistory);
									Common.refreshUpdate(session, cicilanPembayaran);
								}

								loadDataDenganProgressPosting(null);
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

		Criteria criteria = session.createCriteria(CicilanPembayaran.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistoryDimuka", sudahPostingDasbor))

				.add(Restrictions.isNotNull("tanggalTagihan"))

				.add(Restrictions.sqlRestriction("date(this_.tanggal)<date(this_.tanggal_tagihan)"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_tagihan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchitembiaya.getSelectedItem() == null || searchitembiaya.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("itemBiaya", searchitembiaya.getSelectedItem().getValue()))

				.add(searchtelahtampil.isChecked() ? Restrictions.isNotNull("postingHistoryDimuka")
						: !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistoryDimuka"))

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

		List<CicilanPembayaran> cicilanPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cicilanPembayaran);
		grid.setRowRenderer(new CicilanPembayaranRenderer());
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
	 * Kriteria cicilan pembayaran yang dijurnal "dibayar di muka" -- sama dengan penghitung
	 * baris "Mahasiswa - Dibayar Dimuka" pada dasbor.
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
	 * Batalkan posting SEMUA jurnal "dibayar di muka" dalam rentang.
	 *
	 * <p>Hanya grup transaksi ber-{@code ref = 'dimuka'} yang dihapus; jurnal pembayaran biasa
	 * pada cicilan yang sama tidak tersentuh.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<CicilanPembayaran> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistoryDimuka")).list();
			for (CicilanPembayaran cicilan : daftar) {
				try {
					String syarat = "cicilan_pembayaran=" + cicilan.getId()
							+ " and ref='dimuka' and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					cicilan.setPostingHistoryDimuka(null);
					session.update(cicilan);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e,
							"PostingCicilanDibayarDimukaMahasiswaAction jalur API");
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
	 * Posting SEMUA jurnal "dibayar di muka" yang belum dibuat dalam rentang.
	 *
	 * <p>Debet = akun "dibayar di muka" item biaya, kredit = akun piutangnya bila ada, selain
	 * itu akun pendapatannya. Tanggal jurnal = {@code tanggalTagihan} (bukan tanggal bayar --
	 * inilah saat kewajibannya berubah menjadi pendapatan). Grup transaksinya ditandai
	 * {@code ref = "dimuka"} supaya terpisah dari jurnal pembayaran biasa.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistoryDimuka"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pembayaran dimuka mahasiswa dari dasbor jurnal"
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
					Akun akunDebet = cicilan.getItemBiaya().ambilDibayarDimuka(cicilan.getKegiatan());
					Akun akunPiutang = cicilan.getItemBiaya().ambilPiutang(cicilan.getKegiatan());
					Akun akunKredit = akunPiutang != null ? akunPiutang
							: cicilan.getItemBiaya().ambilAkun(cicilan.getKegiatan());
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
						ais.common.ErrorAuditUtil.record(e,
								"PostingCicilanDibayarDimukaMahasiswaAction jalur API");
					}

					Object siapa = cicilan.getKegiatan().getMahasiswa() == null
							? cicilan.getKegiatan().getCalonMahasiswa()
							: cicilan.getKegiatan().getMahasiswa();
					String ket = "Pembayaran dimuka " + siapa + " ke " + cicilan.getKe() + " - "
							+ cicilan.getItemBiaya().getNama() + " - " + cicilan.getKeterangan();
					Double nilai = cicilan.getNilai();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, akunPiutang,
									postingHistory, true, ket, cicilan.getTanggalTagihan(), nilai,
									Double.valueOf(0.0), cicilan, satuanKerja, "dimuka", session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, akunPiutang,
									postingHistory, true, ket, cicilan.getTanggalTagihan(), nilai,
									Double.valueOf(0.0), cicilan, satuanKerja, "dimuka", session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e,
								"PostingCicilanDibayarDimukaMahasiswaAction jalur API");
					}

					if (tersimpan) {
						cicilan.setPostingHistoryDimuka(postingHistory);
						session.getTransaction().begin();
						session.update(cicilan);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"PostingCicilanDibayarDimukaMahasiswaAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingCicilanDibayarDimukaMahasiswaAction jalur API");
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
