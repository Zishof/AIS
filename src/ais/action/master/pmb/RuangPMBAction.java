package ais.action.master.pmb;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.A;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Gedung;
import ais.database.model.GeneralValueObject;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;
import ais.database.model.UjianPMB;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk ruang pmb. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox
 * searchkodeRuangan}, {@code Textbox kodeRuangan}, {@code Textbox calonmhs}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code getDataAlbumPMBAdmin()}, {@code ambil()}, {@code ambilClass()});
 * validasi/perhitungan ({@code cekRuanganIsi()}); mutasi data ({@code onSave()}, {@code setPersetujuan()});
 * pelaporan/ekspor ({@code onCetakAbsensi()}, {@code onCetakBau()}, {@code onCetakAlbum()}, {@code
 * cetakData()}); operasi domain lain ({@code onGaleriFoto()}, {@code onManajemenJadwalUjianPMB()}, {@code
 * onJenisSeleksi()}, {@code onJenisPembiayaan()}, {@code onPaketPMB()}, {@code onManajemenUjianPMB()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class RuangPMBAction extends GenericAutowireComposer implements FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox nama;
	private Textbox searchkodeRuangan;
	private Textbox kodeRuangan;
	private Textbox calonmhs;
	private Textbox searchkapasitasruangan;
	private Combobox searchTahunAjaran;

	private Decimalbox kapasitasRuangan;
	private Combobox searchgedung;
	private Combobox searchUjianPMB;
	private Combobox gedung;
	private Combobox paket;
	private Combobox searchpaket;
	private MyToolbarbuttonConfig add;
	private RuangPMB ruangPMB;
	private Combobox ujianPMB;

	private Tabpanel manajemenUjianPMB;
	private Tabpanel manajemenJadwalUjianPMB;
	private Tabpanel manajemenGelombangPMB;
	private Tabpanel manajemenPaketPMB;

	private Tabpanel galeriFoto;

	public void onGaleriFoto(Event event) {
		if (galeriFoto.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(galeriFoto);
			MyInclude iframe = new MyInclude("/pages/master/galeri_foto_pmb.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private PerguruanTinggi selectedPerguruanTinggi;

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		Common.insertComboDanSemua(gedung = new Combobox(), "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchgedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		List<Paket> pakets = HibernateUtil.currentSession().createCriteria(Paket.class)
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		pakets.add(null);
		Common.insertComboItems(paket = new Combobox(), "nama", pakets);
		if (paket != null) { paket.setReadonly(true); }
		Common.selectComboItem(paket, null);

		Common.insertComboItems(searchpaket, "nama", pakets);
		if (searchpaket != null) { searchpaket.setReadonly(true); }
		Common.selectComboItem(searchpaket, null);

		Common.generateTahunAjaran(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();
		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		// Common.insertComboDanSemua(searchUjianPMB, "nama",
		// "gelombangPendaftaran", UjianPMB.class);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<UjianPMB> ujianPMBs = HibernateUtil.currentSession().createCriteria(UjianPMB.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
						.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("gelombangPendaftaran.perguruanTinggi",
												selectedPerguruanTinggi),
										Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))
						.add(searchTahunAjaran.getSelectedItem() == null
								|| searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue()))
						.list();

				Common.insertComboItems(searchUjianPMB, "nama", "tahunAkademik", ujianPMBs);

			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Perbaiki Urutan Nomor Ujian di Ruang Ujian",
				"/img/svg/check2-circle.svg");
		if (button != null) { button.setParent(add.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(final Event arg0) throws Exception {
						final Paket paket = (Paket) (searchpaket.getSelectedItem() == null ? null
								: searchpaket.getSelectedItem().getValue());

						List<RuangPMB> ruangPMBsUI = initCriteria(true).list();
						if (ruangPMBsUI == null || ruangPMBsUI.isEmpty()) return;

						// PENGELOMPOKAN DATA BERDASARKAN GELOMBANG UNTUK MULTI-THREADING
						final Map<Long, List<Long>> mapGelombangRooms = new java.util.HashMap<Long, List<Long>>();
						for (RuangPMB r : ruangPMBsUI) {
							if (r.getUjianPMB() != null && r.getUjianPMB().getGelombangPendaftaran() != null) {
								Long gelId = r.getUjianPMB().getGelombangPendaftaran().getId();
								if (!mapGelombangRooms.containsKey(gelId)) {
									mapGelombangRooms.put(gelId, new ArrayList<Long>());
								}
								mapGelombangRooms.get(gelId).add(r.getId());
							}
						}

						List<Long> gelombangIds = new ArrayList<Long>(mapGelombangRooms.keySet());

						// EKSEKUSI PARALEL BERDASARKAN GELOMBANG PENDAFTARAN
						ParallelTaskExecutor.process(gelombangIds, 100, new ParallelTaskExecutor.Task<Long>() {
							@Override
							public void execute(Long gelId) throws Exception {
								Session session = null;
								try {
									session = HibernateUtil.openSession();
									session.getTransaction().begin();

									List<Long> roomIds = mapGelombangRooms.get(gelId);
									List<RuangPMB> ruangPMBs = session.createCriteria(RuangPMB.class)
											.add(Restrictions.in("id", roomIds))
											.addOrder(Order.asc("id")).list();

									for (RuangPMB ruangPMB : ruangPMBs) {
										// Hitung Kapasitas
										int count = ((Number) session.createCriteria(RuangPaketPMB.class)
												.add(Restrictions.eq("ruangPMB.id", ruangPMB.getId()))
												.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
												.add(paket == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("biodataCalonMahasiswa.paket", paket))
												.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
												.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
												.add(Restrictions.eq("biodataCalonMahasiswa.gelombangPendaftaran.id", gelId))
												.setProjection(Projections.rowCount()).uniqueResult()).intValue();

										if (ruangPMB.getKapasitasRuangan().intValue() != count) {
											
											// Ambil Ruang ini dan selanjutnya
											List<RuangPMB> ruangIniDanSelanjutnya = session.createCriteria(RuangPMB.class)
													.createAlias("ujianPMB", "ujianPMB")
													.add(Restrictions.eq("ujianPMB.gelombangPendaftaran.id", gelId))
													.add(Restrictions.ge("id", ruangPMB.getId()))
													.addOrder(Order.asc("id")).list();

											if (!ruangIniDanSelanjutnya.isEmpty()) {
												List<BiodataCalonMahasiswa> biodataCalonMahasiswas = session.createCriteria(RuangPaketPMB.class)
														.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
														.add(Restrictions.eq("biodataCalonMahasiswa.gelombangPendaftaran.id", gelId))
														.add(paket == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("biodataCalonMahasiswa.paket", paket))
														.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
														.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
														.setProjection(Projections.property("biodataCalonMahasiswa"))
														.add(Restrictions.in("ruangPMB", ruangIniDanSelanjutnya))
														.addOrder(Order.asc("biodataCalonMahasiswa.noUjian")).list();

												// OPTIMASI MEMORI O(1): Mencegah query N+1 di dalam loop
												Map<Long, RuangPaketPMB> rpMap = new java.util.HashMap<Long, RuangPaketPMB>();
												if (!biodataCalonMahasiswas.isEmpty()) {
													List<RuangPaketPMB> existingRp = session.createCriteria(RuangPaketPMB.class)
															.add(Restrictions.in("biodataCalonMahasiswa", biodataCalonMahasiswas)).list();
													for (RuangPaketPMB rp : existingRp) {
														rpMap.put(rp.getBiodataCalonMahasiswa().getId(), rp);
													}
												}

												int jumlahTotal = 0;
												for (RuangPMB pmb : ruangIniDanSelanjutnya) {
													for (int i = 0; i < pmb.getKapasitasRuangan(); i++) {
														if (jumlahTotal < biodataCalonMahasiswas.size()) {
															BiodataCalonMahasiswa bcm = biodataCalonMahasiswas.get(jumlahTotal);
															
															// Pengambilan data menggunakan Map O(1) sangat cepat
															RuangPaketPMB ruangPaketPMB = rpMap.get(bcm.getId());
															
															if (ruangPaketPMB != null) {
																ruangPaketPMB.setRuangPMB(pmb);
																session.update(ruangPaketPMB);
															} else {
																ruangPaketPMB = new RuangPaketPMB();
																ruangPaketPMB.setBiodataCalonMahasiswa(bcm);
																ruangPaketPMB.setRuangPMB(pmb);
																session.save(ruangPaketPMB);
															}
														}
														jumlahTotal++;
													}
												}
											}
											break; // Perbaikan urutan sudah mencakup seluruh ruangan sisa, stop outer loop
										}
									}

									session.getTransaction().commit();
								} catch (Exception e) {
									if (session != null && session.getTransaction().isActive()) {
										session.getTransaction().rollback();
									}
									ais.common.Common.tampilErrorJikaAdmin(e);
								} finally {
									if (session != null && session.isOpen()) {
										try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:333");}
										try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:334");}
										try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:335");}
									}
								}
							}
						});

						onSearchDefault(arg0);
					}
				});
			}
		});

		button = new MyToolbarbuttonConfig("Bersihkan data dan Perbaiki Urutan Nomor Ujian di Ruang Ujian",
				"/img/svg/check2-circle.svg");
		if (button != null) { button.setParent(add.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(final Event arg0) throws Exception {

						final Paket paket = (Paket) (searchpaket.getSelectedItem() == null ? null
								: searchpaket.getSelectedItem().getValue());

						List<RuangPMB> ruangPMBsUI = initCriteria(true).list();
						if (ruangPMBsUI == null || ruangPMBsUI.isEmpty()) return;

						// PENGELOMPOKAN DATA BERDASARKAN GELOMBANG UNTUK MULTI-THREADING
						final Map<Long, List<Long>> mapGelombangRooms = new java.util.HashMap<Long, List<Long>>();
						for (RuangPMB r : ruangPMBsUI) {
							if (r.getUjianPMB() != null && r.getUjianPMB().getGelombangPendaftaran() != null) {
								Long gelId = r.getUjianPMB().getGelombangPendaftaran().getId();
								if (!mapGelombangRooms.containsKey(gelId)) {
									mapGelombangRooms.put(gelId, new ArrayList<Long>());
								}
								mapGelombangRooms.get(gelId).add(r.getId());
							}
						}

						List<Long> gelombangIds = new ArrayList<Long>(mapGelombangRooms.keySet());

						// EKSEKUSI PARALEL BERDASARKAN GELOMBANG PENDAFTARAN
						ParallelTaskExecutor.process(gelombangIds, 100, new ParallelTaskExecutor.Task<Long>() {
							@Override
							public void execute(Long gelId) throws Exception {
								Session session = null;
								try {
									session = HibernateUtil.openSession();
									session.getTransaction().begin();

									List<Long> roomIds = mapGelombangRooms.get(gelId);

									// Eksekusi DELETE batch untuk room IDs spesifik milik Thread ini
									session.createQuery("delete from RuangPaketPMB where ruangPMB.id in (:ids)")
											.setParameterList("ids", roomIds)
											.executeUpdate();

									// Ambil list Siswa valid
									List<BiodataCalonMahasiswa> biodataCalonMahasiswas = session.createCriteria(BiodataCalonMahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(paket == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("paket", paket))
											.add(Restrictions.eq("gelombangPendaftaran.id", gelId))
											.add(Restrictions.ne("noUjian", "")).add(Restrictions.isNotNull("noUjian"))
											.addOrder(Order.asc("noUjian")).list();

									// Ambil Ruang yang akan diisi ulang
									List<RuangPMB> ruangPMBs2 = session.createCriteria(RuangPMB.class)
											.add(Restrictions.in("id", roomIds))
											.addOrder(Order.asc("id")).list();

									// OPTIMASI MEMORI O(1): Tarik existings jika masih ada relasi gantung
									Map<Long, RuangPaketPMB> rpMap = new java.util.HashMap<Long, RuangPaketPMB>();
									if (!biodataCalonMahasiswas.isEmpty()) {
										List<RuangPaketPMB> existingRp = session.createCriteria(RuangPaketPMB.class)
												.add(Restrictions.in("biodataCalonMahasiswa", biodataCalonMahasiswas)).list();
										for (RuangPaketPMB rp : existingRp) {
											rpMap.put(rp.getBiodataCalonMahasiswa().getId(), rp);
										}
									}

									int jumlahTotal = 0;
									for (RuangPMB pmb : ruangPMBs2) {
										pmb.setPenuh(0);
										session.update(pmb); // Perbarui Status Penuh

										for (int i = 0; i < pmb.getKapasitasRuangan(); i++) {
											if (jumlahTotal < biodataCalonMahasiswas.size()) {
												BiodataCalonMahasiswa bcm = biodataCalonMahasiswas.get(jumlahTotal);
												
												// Menggunakan Map O(1) sangat cepat, tidak memblokir CPU
												RuangPaketPMB ruangPaketPMB = rpMap.get(bcm.getId());
												
												if (ruangPaketPMB != null) {
													ruangPaketPMB.setRuangPMB(pmb);
													session.update(ruangPaketPMB);
												} else {
													ruangPaketPMB = new RuangPaketPMB();
													ruangPaketPMB.setBiodataCalonMahasiswa(bcm);
													ruangPaketPMB.setRuangPMB(pmb);
													session.save(ruangPaketPMB);
												}
											}
											jumlahTotal++;
										}
									}

									session.getTransaction().commit();
								} catch (Exception e) {
									if (session != null && session.getTransaction().isActive()) {
										session.getTransaction().rollback();
									}
									ais.common.Common.tampilErrorJikaAdmin(e);
								} finally {
									// Pembersihan Mutlak Memori Hibernate
									if (session != null && session.isOpen()) {
										try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:453");}
										try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:454");}
										try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/RuangPMBAction.java:455");}
									}
								}
							}
						});

						onSearchDefault(arg0);
					}
				});
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public void onManajemenJadwalUjianPMB(Event event) {
		if (manajemenJadwalUjianPMB.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenJadwalUjianPMB);
			MyInclude iframe = new MyInclude("/pages/master/jadwal_ujian_pmb.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenJenisSeleksi;
	private DisposisiSop disposisiSop;

	public void onJenisSeleksi(Event event) {
		if (manajemenJenisSeleksi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenJenisSeleksi);
			MyInclude iframe = new MyInclude("/pages/master/jenis_seleksi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenJenisPembiayaan;

	public void onJenisPembiayaan(Event event) {
		if (manajemenJenisPembiayaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenJenisPembiayaan);
			MyInclude iframe = new MyInclude("/pages/master/jenis_pembiayaan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onPaketPMB(Event event) {
		if (manajemenPaketPMB.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenPaketPMB);
			MyInclude iframe = new MyInclude("/pages/master/paket.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenUjianPMB(Event event) {
		if (manajemenUjianPMB.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenUjianPMB);
			MyInclude iframe = new MyInclude("/pages/master/ujian_pmb.zul");
			iframe.setParent(window);
		}
	}

	public void onGelombangPMB(Event event) {
		if (manajemenGelombangPMB.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenGelombangPMB);
			MyInclude iframe = new MyInclude("/pages/master/gelombang_pendaftaran.zul");
			iframe.setParent(window);
		}
	}

	class RuangPMBRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RuangPMB ruangPMB = (RuangPMB) arg1;

			(new RuangPmbCalonMahasiswaDetailAction(ruangPMB)).setParent(arg0);

			Integer isi = RuangPMBAction.cekRuanganIsi(ruangPMB);

			if (ruangPMB.getPenuh().equals(0) && isi.equals(ruangPMB.getKapasitasRuangan())) {
				ruangPMB.setPenuh(1);
				Common.refreshUpdate(ruangPMB);
			}

			RevisiHelper.createNewRevisi(RuangPMB.class, ruangPMB, ruangPMB.getNama()).setParent(arg0);

			new Label(ruangPMB.getKodeRuangan()).setParent(arg0);
			new Label(ruangPMB.getGedung() == null ? "" : ruangPMB.getGedung().getNama()).setParent(arg0);

			new Label(
					ruangPMB.getKapasitasRuangan() == null ? "" : ruangPMB.getKapasitasRuangan().toString() + "/" + isi)
					.setParent(arg0);
			new Label(ruangPMB.getPaket() == null ? "Semua" : ruangPMB.getPaket().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(ruangPMB.getUjianPMB() == null ? "" : ruangPMB.getUjianPMB().getNama()).setParent(vbox);
			new Label(ruangPMB.getUjianPMB() == null || ruangPMB.getUjianPMB().getGelombangPendaftaran() == null ? ""
					: ruangPMB.getUjianPMB().getGelombangPendaftaran().getNama()).setParent(vbox);

//			new Label(ruangPMB.getTahun() == null ? "" : ruangPMB.getTahun().toString()).setParent(arg0);
			new Label(ruangPMB.getTahunAkademik()).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			new Label(ruangPMB.getKeterangan()).setParent(vbox2);

			if (ruangPMB.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + ruangPMB.getDisposisiSop().getKeterangan() + " ("
						+ ruangPMB.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(ruangPMB.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Penuh");
			checkbox.setChecked(ruangPMB.getPenuh().equals(1));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruangPMB.setPenuh(checkbox.isChecked() ? 1 : 0);
					Common.refreshSaveOrUpdate(ruangPMB);
				}
			});

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ruangPMB);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE));
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
											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session, ruangPMB.getDisposisiSop())) {
												Common.refreshDelete(session, ruangPMB);
												onSearchDefault(event);
											}
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

			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakAbsensiPMB(ruangPMB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Verifikasi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Verifikasi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakVerifikasiPMB(ruangPMB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Berita Acara", "/img/album.png");
			button.setOrient("vertical");
			button.setTooltiptext("Berita Acara Ujian");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakBau(ruangPMB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Cover Album", "/img/album_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cover Album");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakAbsensi(ruangPMB);
				}
			});

			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Album Absensi", "/img/absensi_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Album Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onCetakAlbum(ruangPMB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Pilihan Prodi", "/img/absensi_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Album Pilihan Prodi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakAbsensiPMBFoto(ruangPMB).onModal();
				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RuangPMB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RuangPMB ruangPMB) throws Exception {

		addWindow.setTitle(ruangPMB.getId() == null ? "Tambah Ruang PMB" : "Ubah Ruang PMB");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop=null;center.appendChild(form(ruangPMB, null, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (ujianPMB.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Ujian PMB belum dipilih. Langkah yang dapat dilakukan: (1) pilih Ujian PMB dari daftar dropdown yang tersedia; (2) pastikan data ujian sudah dikonfigurasi di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Ruangan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama dengan nama ruangan yang benar; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeRuangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Ruangan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Ruangan dengan kode yang sesuai; (2) pastikan kode tidak mengandung spasi ekstra; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kapasitasRuangan.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kapasitas Ruangan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kapasitas Ruangan dengan angka kapasitas maksimum; (2) pastikan nilai berupa angka yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ruangPMB.getId() != null) {
			ruangPMB = (RuangPMB) session.load(RuangPMB.class, ruangPMB.getId());
		}
		ruangPMB.setDisposisiSop(disposisiSop);
		ruangPMB.setUjianPMB((UjianPMB) ujianPMB.getSelectedItem().getValue());
		ruangPMB.setNama(nama.getValue());
		ruangPMB.setKodeRuangan(kodeRuangan.getValue());
		ruangPMB.setGedung((Gedung) (gedung.getSelectedItem() == null ? null : gedung.getSelectedItem().getValue()));
		ruangPMB.setKapasitasRuangan(
				kapasitasRuangan.getValue() == null ? null : Integer.parseInt(kapasitasRuangan.getValue().toString()));
		ruangPMB.setPaket((Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, ruangPMB);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RuangPMB.class);

		if (!calonmhs.getValue().trim().isEmpty()) {
			criteria = session.createCriteria(RuangPaketPMB.class).setProjection(Projections.groupProperty("ruangPMB"))
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
					.add(Restrictions.or(
							Restrictions.ilike("biodataCalonMahasiswa.noUjian", calonmhs.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("biodataCalonMahasiswa.nama", calonmhs.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", calonmhs.getValue().trim(),
											MatchMode.ANYWHERE))))
					.createCriteria("ruangPMB");
		}

		if (order && calonmhs.getValue().trim().isEmpty())
			criteria.addOrder(Order.asc("kodeRuangan")).addOrder(Order.asc("id"));

		criteria

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkodeRuangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kodeRuangan", searchkodeRuangan.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkapasitasruangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))
				.add(searchpaket.getSelectedItem() == null || searchpaket.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))
				.add(searchgedung.getSelectedItem() == null || searchgedung.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()))
				.add(searchUjianPMB.getSelectedItem() == null || searchUjianPMB.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ujianPMB", searchUjianPMB.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<RuangPMB> ruangPMB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(ruangPMB);
		grid.setRowRenderer(new RuangPMBRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static Integer cekRuanganIsi(RuangPMB ruangPMB) {
		Integer count = 0;
		Session session = HibernateUtil.currentSession();

		count = ((Number) session.createCriteria(RuangPaketPMB.class).add(Restrictions.eq("ruangPMB", ruangPMB))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAbsensi(RuangPMB ruang) throws Exception {

		this.ruangPMB = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak absensi " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		Report.generatePDFReport(Report.PDF, parameters, "Coverspmbi", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakBau(RuangPMB ruang) throws Exception {

		this.ruangPMB = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak Bau " + ruang.getId());
		parameters.put("ruang", ruang.getId());

		Report.generatePDFReport(Report.PDF, parameters, "BeritaAcaraUjian", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAlbum(RuangPMB ruang) throws Exception {

		this.ruangPMB = ruang;
		// final Map<String, Long> parameters = new HashMap<String, Long>();
		final Map parameters = ais.common.HashMapGenerator.getRand();
		List<Map<String, Object>> maps = getDataAlbumPMBAdmin(ruang);
		parameters.put("ujian", ruang.getUjianPMB() == null ? -1L : ruang.getUjianPMB().getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("tahunakademik", ruang.getTahunAkademik());
		parameters.put("gelombang_pendaftaran",
				ruang.getUjianPMB() == null || ruang.getUjianPMB().getGelombangPendaftaran() == null ? ""
						: ruang.getUjianPMB().getGelombangPendaftaran().getNama());
		parameters.put("ket_ruang",
				ruang.getNama() + (ruang.getGedung() == null ? "" : " ( " + ruang.getGedung().getNama() + " )"));

//		System.out.println("Cetak Album PMB paket " + ruang.getPaket().getNama() + " ruang " + ruang.getNama());

		parameters.put("paket", ruang.getPaket() == null ? "" : ruang.getPaket().getNama());
		Report.generatePDFReport("pdf", parameters, "AlbumPMBHari", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getDataAlbumPMBAdmin(RuangPMB ruang) throws Exception {
		this.ruangPMB = ruang;
		Session session = HibernateUtil.currentSession();
		List<RuangPaketPMB> listPendaftaranWisuda = session.createCriteria(RuangPaketPMB.class)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa").addOrder(Order.asc("id"))
				.add(Restrictions.eq("ruangPMB", ruang)).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangPaketPMB beanPendaftaranWisuda = (RuangPaketPMB) itr.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("nama", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getNoUjian());
				map.put("prodi_1", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi1() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi1().getNama());
				map.put("prodi_2", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi2() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi2().getNama());
				map.put("ttl",
						beanPendaftaranWisuda.getBiodataCalonMahasiswa().getTempatLahir().toUpperCase() + " / "
								+ Common.dateFormat2.get()
										.format(beanPendaftaranWisuda.getBiodataCalonMahasiswa().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getJenisKelamin());
				String kota = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKotaCalon() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKotaCalon().getNama();
				String propinsi = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getPropinsiCalon() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getPropinsiCalon().getNama();
				map.put("alamat",
						beanPendaftaranWisuda.getBiodataCalonMahasiswa().getAlamat().toUpperCase() + ", "
								+ beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKelurahanCalon().toUpperCase()
								+ ", " + beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKecamatanCalon() + ", "
								+ kota.toUpperCase() + ", " + propinsi.toUpperCase());

				beanPendaftaranWisuda.getBiodataCalonMahasiswa().putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {

		this.ruangPMB = (RuangPMB) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");
		
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		
		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Ujian *"));
		row.appendChild(ujianPMB = new Combobox());

		List<UjianPMB> ujianPMBs = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(UjianPMB.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
						.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("gelombangPendaftaran.perguruanTinggi",
												selectedPerguruanTinggi),
										Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))
						.add(searchTahunAjaran.getSelectedItem() == null
								|| searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue())),
				UjianPMB.class);

		Common.insertComboItems(ujianPMB, "nama", "tahunAkademik", ujianPMBs);

		Common.selectComboItem(ujianPMB, ruangPMB.getUjianPMB());
		ujianPMB.setWidth("90%");
		ujianPMB.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Ruangan *"));
		row.appendChild(kodeRuangan = new Textbox(ruangPMB.getKodeRuangan() == null ? "" : ruangPMB.getKodeRuangan()));
		kodeRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ruangan *"));
		row.appendChild(nama = new Textbox(ruangPMB.getNama() == null ? "" : ruangPMB.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gedung"));
		Common.selectComboItem(gedung, ruangPMB.getGedung());
		row.appendChild(gedung);
		gedung.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Ruangan *"));
		row.appendChild(kapasitasRuangan = new Decimalbox(
				new BigDecimal(ruangPMB.getKapasitasRuangan() == null ? 30 : ruangPMB.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		row.appendChild(paket = new Combobox());
		Common.insertComboDanSemua(paket, "nama", "keterangan", Paket.class,
				Restrictions.and(
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(paket, ruangPMB.getPaket());
		paket.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan pilihan paket jika ruangan ini berlaku untuk semua paket");

		if (ruangPMB.getId() != null) {
			if (cekRuanganIsi(ruangPMB) > 0) {
				paket.setDisabled(true);
			} else {
				paket.setDisabled(false);
			}
		}

		if (paket.getSelectedItem() == null && searchpaket.getSelectedItem() != null) {
			Common.selectComboItem(paket, searchpaket.getSelectedItem().getValue());
		}

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Penentuan ruangan dan kuota calon mahasiswa";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return ruangPMB;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return RuangPMB.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		// TODO Auto-generated method stub

	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
