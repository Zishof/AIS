package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.KelasPmbPunyaBiodataCalonMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.KelasPmb;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelasPmbAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Decimalbox kapasitasRuangan;
	private Combobox gelombangPendaftaran;
	private Combobox searchgelombangPendaftaran;
	
	// PENAMBAHAN FIELD BARU
	private Combobox tahunAkademik;
	private Combobox jenisSemester;

	private MyToolbarbuttonConfig add;
	private KelasPmb kelasPmb;
	private AmbilDataKelasBanbox kelas;
	private Combobox fakultas;
	private Combobox jurusan;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		// TOMBOL BARU: Sinkronisasi Massal Kelas dengan Calon Mahasiswa (Parallel Threaded & Overflow Algorithm)
		MyToolbarbuttonConfig btnSync = new MyToolbarbuttonConfig("Singkronkan Data Kelas Dengan Calon Mahasiswa", "/img/svg/check2-circle.svg");
		if (add.getParent() != null) {
			btnSync.setParent(add.getParent());
			btnSync.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			btnSync.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Anda yakin ingin menyinkronkan Calon Mahasiswa secara otomatis? (Otomasi Distribusi Kelas Penuh/Overflow ke Kelas Berikutnya berdasarkan Prodi & Kuota)", "Konfirmasi Sinkronisasi",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												executeSyncMassal();
											}
										});
									}
								}
							});
				}
			});
		}

		Session hSession = null;
		try {
			hSession = HibernateUtil.openSession();
			Common.insertComboDanSemua(searchgelombangPendaftaran, "nama", GelombangPendaftaran.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			
			
			searchgelombangPendaftaran.setReadonly(true);

			if (execution.getParameter("gelombangPendaftaran") != null) {
				Long idGelombang = Long.parseLong(execution.getParameter("gelombangPendaftaran"));
				GelombangPendaftaran gel = (GelombangPendaftaran) hSession.createCriteria(GelombangPendaftaran.class)
						.add(Restrictions.idEq(idGelombang))
						.uniqueResult();
				if (gel != null) {
					Common.selectComboItem(true, searchgelombangPendaftaran, gel);
					searchgelombangPendaftaran.setDisabled(true);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (hSession != null && hSession.isOpen()) {
				try { hSession.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { hSession.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try { hSession.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	// -------------------------------------------------------------------------------------
	// LOGIKA UTAMA SINKRONISASI KELAS (PARALLEL & ALGORITMA OVERFLOW KELAS A KE KELAS B)
	// -------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void executeSyncMassal() {
		Session sessionUtama = null;
		List<KelasPmb> listKelasUI = new ArrayList<KelasPmb>();
		
		try {
			sessionUtama = HibernateUtil.openSession();
			listKelasUI = initCriteria(sessionUtama, true).list();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (sessionUtama != null && sessionUtama.isOpen()) {
				try { sessionUtama.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		if (listKelasUI == null || listKelasUI.isEmpty()) return;

		// 1. KELOMPOKKAN KELAS BERDASARKAN GELOMBANG/TA & JURUSAN/FAKULTAS
		// Tujuannya agar Kelas A dan Kelas B pada Prodi yang sama masuk dalam satu antrean pembagian/overflow
		final Map<String, List<Long>> mapGroupKelas = new java.util.HashMap<String, List<Long>>();
		
		for (KelasPmb kp : listKelasUI) {
			String timeKey = "";
			if (kp.getGelombangPendaftaran() != null) {
				timeKey = "GEL|" + kp.getGelombangPendaftaran().getId();
			} else if (kp.getTahunAkademik() != null && kp.getJenisSemester() != null) {
				timeKey = "TA|" + kp.getTahunAkademik() + "|" + kp.getJenisSemester();
			} else {
				continue; // Abaikan data yg tidak valid
			}
			
			String scopeKey = "";
			if (kp.getJurusan() != null) {
				scopeKey = "PRODI|" + kp.getJurusan().getId();
			} else if (kp.getFakultas() != null) {
				scopeKey = "FAK|" + kp.getFakultas().getId();
			} else {
				scopeKey = "ALL";
			}
			
			String groupKey = timeKey + "###" + scopeKey;
			
			if (!mapGroupKelas.containsKey(groupKey)) {
				mapGroupKelas.put(groupKey, new ArrayList<Long>());
			}
			mapGroupKelas.get(groupKey).add(kp.getId());
		}

		List<String> groupKeys = new ArrayList<String>(mapGroupKelas.keySet());

		// Laporan rinci per grup kelas (berhasil/gagal+penyebab teknis lengkap+langkah
		// mengatasi) - sebelumnya error di 1 grup cuma tampilErrorJikaAdmin generik,
		// tanpa pernah menyebut grup MANA yang gagal atau kenapa.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Data Kelas dengan Calon Mahasiswa");
		final java.util.concurrent.atomic.AtomicInteger nomorBarisLaporan = new java.util.concurrent.atomic.AtomicInteger(0);

		// 2. EKSEKUSI MULTI-THREAD PARALEL
		try {
			ParallelTaskExecutor.process(groupKeys, 100, new ParallelTaskExecutor.Task<String>() {
				@Override
				public void execute(String gKey) throws Exception {
					Session session = null;
					int nomorBaris = nomorBarisLaporan.getAndIncrement();
					String kunciGrup = "Grup: " + gKey.replace("###", " / ");
					try {
						session = HibernateUtil.openSession();
						session.getTransaction().begin();

						String[] parts = gKey.split("###");
						String timeKey = parts[0];
						String scopeKey = parts[1];
						kunciGrup = "Waktu: " + timeKey + " | Cakupan: " + scopeKey;

						List<Long> kelasIds = mapGroupKelas.get(gKey);
						
						// AMBIL KELAS & URUTKAN BY NAMA (PENTING! Agar Kelas A Diisi Lebih Dulu Dari Kelas B)
						List<KelasPmb> kelasPmbs = session.createCriteria(KelasPmb.class)
								.add(Restrictions.in("id", kelasIds))
								.addOrder(Order.asc("nama")).list();

						List<KelasPmb> availableClasses = new ArrayList<KelasPmb>();
						Map<Long, Integer> sisaKuotaMap = new java.util.HashMap<Long, Integer>();
						int totalSisaKuota = 0;

						// Kalkulasi kursi tersedia secara kolektif untuk grup ini (Antrean Kelas A, Kelas B, dst)
						for (KelasPmb kp : kelasPmbs) {
							int countIsi = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.eq("kelasPmb.id", kp.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();

							int sisaKuota = kp.getKapasitasRuangan() - countIsi;

							if (sisaKuota > 0) {
								availableClasses.add(kp);
								sisaKuotaMap.put(kp.getId(), sisaKuota);
								totalSisaKuota += sisaKuota;
								
								// Pastikan status penuh di-reset jika ada sisa kursi yg ditinggalkan
								if (kp.getPenuh() != null && kp.getPenuh().equals(1)) {
									kp.setPenuh(0);
									session.update(kp);
								}
							} else if (sisaKuota <= 0 && (kp.getPenuh() == null || kp.getPenuh().equals(0))) {
								kp.setPenuh(1);
								session.update(kp);
							}
						}

						// Jika masih ada sisa kursi di grup ini (Kelas A, B, dst)
						if (totalSisaKuota > 0 && !availableClasses.isEmpty()) {
							
							Criteria cMhs = session.createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.isNotNull("prodiLulus")) // Harus berstatus Diterima/Lulus
									.add(Restrictions.isNull("kelasPmb"));     // Belum memiliki kelas

							// Filter Waktu: Adaptif (Gelombang atau TA/Semester)
							if (timeKey.startsWith("GEL|")) {
								Long gelId = Long.parseLong(timeKey.replace("GEL|", ""));
								cMhs.add(Restrictions.eq("gelombangPendaftaran.id", gelId));
							} else if (timeKey.startsWith("TA|")) {
								String[] taSmt = timeKey.replace("TA|", "").split("\\|");
								String ta = taSmt[0];
								String smt = taSmt[1];
								
								cMhs.createAlias("gelombangPendaftaran", "gel", Criteria.LEFT_JOIN);
								cMhs.add(Restrictions.or(
									Restrictions.and(Restrictions.eq("tahunAkademik", ta), Restrictions.eq("semesterMulai", smt)),
									Restrictions.and(Restrictions.eq("gel.tahunAkademik", ta), Restrictions.eq("gel.jenisSemester", smt))
								));
							}

							// Filter Target Ruang: Prodi/Fakultas Sesuai Scope Grup Ini
							if (scopeKey.startsWith("PRODI|")) {
								Long prodiId = Long.parseLong(scopeKey.replace("PRODI|", ""));
								cMhs.add(Restrictions.eq("prodiLulus.id", prodiId));
							} else if (scopeKey.startsWith("FAK|")) {
								Long fakId = Long.parseLong(scopeKey.replace("FAK|", ""));
								cMhs.createAlias("prodiLulus", "pl");
								cMhs.add(Restrictions.eq("pl.fakultas.id", fakId));
							}

							// URUTKAN BERDASARKAN YANG LEBIH DULU DAPAT NIM, ATAU YANG DAFTAR DULUAN
							cMhs.addOrder(Order.asc("nim")).addOrder(Order.asc("noRegistrasi"));
							cMhs.setMaxResults(totalSisaKuota); // Memori Sangat Efisien (Hanya ditarik sebatas sisa bangku keseluruhan)

							List<BiodataCalonMahasiswa> mhsToSync = cMhs.list();

							if (!mhsToSync.isEmpty()) {
								int currentClassIndex = 0;
								KelasPmb currentClass = availableClasses.get(currentClassIndex);
								int currentSisa = sisaKuotaMap.get(currentClass.getId());

								// SKEMA OTOMASI OVERFLOW (Kelas A Penuh -> Geser otomatis ke Kelas B)
								for (BiodataCalonMahasiswa bcm : mhsToSync) {
									bcm.setKelasPmb(currentClass);
									session.update(bcm);
									
									currentSisa--;
									
									// Jika kelas saat ini sudah penuh terisi 30/30 (misalnya), geser kursor pointer ke kelas berikutnya
									if (currentSisa <= 0) {
										currentClass.setPenuh(1);
										session.update(currentClass);
										
										currentClassIndex++;
										if (currentClassIndex < availableClasses.size()) {
											currentClass = availableClasses.get(currentClassIndex);
											currentSisa = sisaKuotaMap.get(currentClass.getId());
										} else {
											break; // Seluruh bangku di seluruh kelas grup ini telah ludes terisi
										}
									}
								}
							}
						}

						session.getTransaction().commit();
						laporan.catatBerhasil(nomorBaris, kunciGrup, "Sinkronisasi berhasil");
					} catch (Exception e) {
						if (session != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
						ais.common.Common.tampilErrorJikaAdmin(e);
						laporan.catatGagalDetail(nomorBaris, kunciGrup, e);
					} finally {
						// Mutlak dibersihkan
						if (session != null && session.isOpen()) {
							try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/KelasPmbAction.java:357");}
							try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/KelasPmbAction.java:358");}
							try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/KelasPmbAction.java:359");}
						}
					}
				}
			});
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-grup): " + ais.common.LaporanUpload.detailTeknisException(e));
		}

		laporan.selesaikan(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class KelasPmbRenderer extends ais.ui.util.MyRowRenderer {

		private KelasPmbPunyaBiodataCalonMahasiswaHelper kelasPmbPunyaBiodataCalonMahasiswaHelper = new KelasPmbPunyaBiodataCalonMahasiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KelasPmb kelasPmb = (KelasPmb) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						kelasPmbPunyaBiodataCalonMahasiswaHelper.display(kelasPmb, detail, addWindow);
					}
				}
			});

			Integer isi = cekRuanganIsi(kelasPmb);

			if (kelasPmb.getPenuh() != null && kelasPmb.getPenuh().equals(0) && isi.equals(kelasPmb.getKapasitasRuangan())) {
				kelasPmb.setPenuh(1);
				try {
					Common.refreshUpdate(kelasPmb);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			RevisiHelper.createNewRevisi(KelasPmb.class, kelasPmb, kelasPmb.getNama()).setParent(arg0);
			new Label(kelasPmb.getFakultas() == null ? "Semua" : kelasPmb.getFakultas().getNama()).setParent(arg0);
			new Label(kelasPmb.getJurusan() == null ? "Semua" : kelasPmb.getJurusan().getNama()).setParent(arg0);
			new Label(kelasPmb.getKapasitasRuangan() == null ? "" : kelasPmb.getKapasitasRuangan().toString() + "/" + isi).setParent(arg0);
			
			// MENAMPILKAN LABEL GELOMBANG ATAU TAHUN AKADEMIK-SEMESTER
			String lblGelombang = kelasPmb.getGelombangPendaftaran() != null ? kelasPmb.getGelombangPendaftaran().getNama() 
					: (kelasPmb.getTahunAkademik() + " - " + kelasPmb.getJenisSemester());
			new Label(lblGelombang).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Penuh");
			checkbox.setChecked(kelasPmb.getPenuh() != null && kelasPmb.getPenuh().equals(1));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasPmb.setPenuh(checkbox.isChecked() ? 1 : 0);
					try {
						Common.refreshSaveOrUpdate(kelasPmb);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);
			
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelasPmb);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);

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
											Common.refreshDelete(kelasPmb);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sebagai berikut:" + e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(toolbar);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KelasPmb());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelasPmb kelasPmb) {
		this.kelasPmb = kelasPmb;
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
		addWindow.setTitle(kelasPmb.getId() == null ? "Tambah Kelas Mahasiswa" : "Ubah Kelas Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		row.appendChild(kelas = new AmbilDataKelasBanbox());
		kelas.setAttribute("kelas", kelasPmb.getKelas());
		kelas.setValue(kelasPmb.getKelas() == null ? "" : kelasPmb.getKelas().getNama());
		kelas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, kelasPmb.getFakultas() == null ? tbmuser.ambilFakultas() : kelasPmb.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, kelasPmb.getJurusan() == null ? tbmuser.ambilJurusan() : kelasPmb.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Jurusan")
				+ " jika kelas ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan") + ")");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Kelas"));
		row.appendChild(kapasitasRuangan = new Decimalbox(kelasPmb.getKapasitasRuangan() == null ? BigDecimal.ZERO : new BigDecimal(kelasPmb.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		// PENAMBAHAN UI: TAHUN AKADEMIK DAN SEMESTER
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, kelasPmb.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, kelasPmb.getJenisSemester());
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang")); // Dihilangkan tanda * (Opsional)
		Common.insertComboDanSemua(gelombangPendaftaran = new Combobox(), "nama", GelombangPendaftaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		gelombangPendaftaran.setDisabled(false);
		Common.selectComboItem(gelombangPendaftaran, kelasPmb.getGelombangPendaftaran() == null ? null : kelasPmb.getGelombangPendaftaran());
		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setReadonly(true);

		// EVENT LISTENER: Disable TA dan SMT jika Gelombang Dipilih
		EventListener gelombangListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean hasGelombang = gelombangPendaftaran.getSelectedItem() != null && gelombangPendaftaran.getSelectedItem().getValue() != null;
				tahunAkademik.setDisabled(hasGelombang);
				jenisSemester.setDisabled(hasGelombang);
			}
		};
		gelombangPendaftaran.addEventListener("onChange", gelombangListener);
		try {
			gelombangListener.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (kelasPmb.getId() != null) {
			if (cekRuanganIsi(kelasPmb) > 0) {
				gelombangPendaftaran.setDisabled(true);
				tahunAkademik.setDisabled(true);
				jenisSemester.setDisabled(true);
			}
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
		if (kelas.getAttribute("kelas") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas siswa / kelas les siswa",
					"Kolom Kelas siswa / kelas les siswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelas siswa / kelas les siswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		
		if (kapasitasRuangan.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kapasitas Kelas",
					"Kolom Kapasitas Kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kapasitas Kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// VALIDASI BARU: Gelombang atau TA/SMT
		Object valGel = gelombangPendaftaran.getSelectedItem() != null ? gelombangPendaftaran.getSelectedItem().getValue() : null;
		Object valTa = tahunAkademik.getSelectedItem() != null ? tahunAkademik.getSelectedItem().getValue() : null;
		Object valSmt = jenisSemester.getSelectedItem() != null ? jenisSemester.getSelectedItem().getValue() : null;

		if (valGel == null && (valTa == null || valSmt == null || valTa.toString().trim().isEmpty() || valSmt.toString().trim().isEmpty())) {
			MyMessageboxConfig.show("Jika Gelombang tidak dipilih, maka Tahun Akademik dan Semester WAJIB diisi!", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.getTransaction().begin();

			if (kelasPmb.getId() != null) {
				kelasPmb = (KelasPmb) session.load(KelasPmb.class, kelasPmb.getId());
			}
			
			kelasPmb.setKapasitasRuangan(Integer.parseInt(kapasitasRuangan.getValue().toString()));
			kelasPmb.setKelas((Kelas) kelas.getAttribute("kelas"));
			
			Fakultas fakVal = fakultas.getSelectedItem() == null ? null : (Fakultas) fakultas.getSelectedItem().getValue();
			kelasPmb.setFakultas(fakVal);
			
			Jurusan jurVal = jurusan.getSelectedItem() == null ? null : (Jurusan) jurusan.getSelectedItem().getValue();
			kelasPmb.setJurusan(jurVal);

			// SAVE LOGIC GELOMBANG VS TA/SMT
			if (valGel != null) {
				kelasPmb.setGelombangPendaftaran((GelombangPendaftaran) valGel);
				kelasPmb.setTahunAkademik(null);
				kelasPmb.setJenisSemester(null);
			} else {
				kelasPmb.setGelombangPendaftaran(null);
				kelasPmb.setTahunAkademik(valTa.toString());
				kelasPmb.setJenisSemester(valSmt.toString());
			}

			Common.refreshSaveOrUpdate(session, kelasPmb);
			
			if (session.getTransaction().isActive()) {
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
			return false;
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
		return true;
	}

	public Criteria initCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(KelasPmb.class);
		
		String namaVal = searchnama.getValue() == null ? "" : searchnama.getValue().trim();
		Object gelombangVal = searchgelombangPendaftaran.getSelectedItem() == null ? null : searchgelombangPendaftaran.getSelectedItem().getValue();

		if (!namaVal.isEmpty()) {
			criteria.add(Restrictions.ilike("nama", namaVal, MatchMode.ANYWHERE));
		}
		if (gelombangVal != null) {
			criteria.add(Restrictions.eq("gelombangPendaftaran", gelombangVal));
		}
		if (order) {
			criteria.addOrder(Order.asc("id"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			
			Criteria countCriteria = initCriteria(session, false);
			Common.initPaging(countCriteria, paging);

			Criteria dataCriteria = initCriteria(session, true);
			List<KelasPmb> listKelasPmb = dataCriteria.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel strset = new SimpleListModel(listKelasPmb);
			grid.setRowRenderer(new KelasPmbRenderer());
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

	public Integer cekRuanganIsi(KelasPmb kelasPmb) {
		if (kelasPmb == null || kelasPmb.getId() == null) {
			return 0;
		}

		Integer count = 0;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.eq("kelasPmb.id", kelasPmb.getId()))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					
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
		return count;
	}
}
