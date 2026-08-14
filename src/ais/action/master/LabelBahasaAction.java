package ais.action.master;

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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LabelBahasa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LabelBahasaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnamabahasa;
	/** Filter status terjemahan: "" (semua) / "BELUM" (English atau Arab belum diterjemah) / "SUDAH". */
	private org.zkoss.zul.Combobox searchStatusTerjemah;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private LabelBahasa labelBahasa;
	private MyToolbarbuttonConfig add;
	private Textbox indonesia;
	private Textbox english;
	private Textbox arab;
	private Textbox mandarin;

	public static String[] contents = new String[] { "id", "nama", "indonesia", "english", "arab", "mandarin",
			"keterangan" };

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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = HibernateUtil.currentSession();
					session.createSQLQuery("delete from label_bahasa where indonesia  ~ '^[0-9\\.]+$'").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where replace(indonesia,'.','')  ~ '^[0-9\\.]+$'")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where replace(indonesia,',','')  ~ '^[0-9\\.]+$'")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%(Mahasiswa)%';")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%(Dosen)%';")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where replace(indonesia,'/','')  ~ '^[0-9\\.]+$';")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%(Ganjil)%';")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%(Genap)%';")
							.executeUpdate();

					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.jpg%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.jpeg%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.png%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.pdf%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.doc%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.docx%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.ppt%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.pptx%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.xls%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia ilike '%.xlsx%';").executeUpdate();
					session.createSQLQuery("delete from label_bahasa where nama ilike '%..%';").executeUpdate();

					session.createSQLQuery("delete from label_bahasa where indonesia in (select nama from mahasiswa);")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where indonesia in (select nama from dosen);")
							.executeUpdate();
					session.createSQLQuery("delete from label_bahasa where nama SIMILAR TO '%[0-9]{2,}%';")
							.executeUpdate();

					session.createSQLQuery("delete from label_bahasa where (nama  ~* '[a-z]') is false;")
							.executeUpdate();

					session.createSQLQuery("delete from label_bahasa where olehid ilike '%DashboardPustaka%';")
							.executeUpdate();

					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%PendaftaranWisudaMahasiswaAction%';")
							.executeUpdate();

					session.createSQLQuery("delete from label_bahasa where olehid ilike '%TugasMandiriHelper%';")
							.executeUpdate();

					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%ais.database.model.BiodataCalonMahasiswa%';")
							.executeUpdate();

					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%ais.database.model.Mahasiswa%';")
							.executeUpdate();

					session.createSQLQuery("delete from label_bahasa where olehid ilike '%ais.database.model.Dosen%';")
							.executeUpdate();
					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%ais.action.master.helper.StudiMahasiswaHelper%';")
							.executeUpdate();
					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%ais.action.master.helper.DetailperkuliahanHelper%';")
							.executeUpdate();
					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%ais.action.master.dashboard.helper%';")
							.executeUpdate();

					session.createSQLQuery(
							"delete from label_bahasa where olehid ilike '%external_update;ais.common.Common%';")
							.executeUpdate();

					onSearchDefault(null);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, LabelBahasa.class, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Object[] data = arg0 == null || !(arg0.getData() instanceof Object[])
						? null : (Object[]) arg0.getData();
				LabelBahasa hasilUpload = data == null || data.length == 0 || !(data[0] instanceof LabelBahasa)
						? null : (LabelBahasa) data[0];
				if (hasilUpload != null && hasilUpload.getNama() != null) {
					MemoryDbUtil.getBahasaIndonesias().put(hasilUpload.getNama(), hasilUpload.getIndonesia());
					MemoryDbUtil.getBahasaEnglishs().put(hasilUpload.getNama(), hasilUpload.getEnglish());
					MemoryDbUtil.getBahasaArabs().put(hasilUpload.getNama(), hasilUpload.getArab());
					MemoryDbUtil.getBahasaMandarins().put(hasilUpload.getNama(), hasilUpload.getMandarin());
				}
			}

		}, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// Tombol "Terjemahkan Otomatis" (kamus internal, manual-assisted) — mengisi English/Arab untuk
		// baris SESUAI FILTER yang belum diterjemahkan.
		MyToolbarbuttonConfig terjemah = new MyToolbarbuttonConfig("Terjemahkan Otomatis",
				"/img/svg/check2-circle.svg");
		terjemah.setTooltiptext("Isi otomatis terjemahan English & Arab (kamus internal) untuk baris sesuai "
				+ "filter yang belum diterjemahkan. Kata yang belum dikenal kamus dibiarkan untuk dilengkapi manual.");
		terjemah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onTerjemahkanOtomatis(arg0);
			}
		});
		Common.appendKeToolbar(terjemah, add, comp);

		// Tombol "Terjemahkan Ulang" (kamus internal) — MENIMPA English/Arab dari teks Bahasa Indonesia
		// terkini untuk SEMUA baris sesuai filter (mengoreksi terjemahan yang belum cocok).
		MyToolbarbuttonConfig terjemahUlang = new MyToolbarbuttonConfig("Terjemahkan Ulang",
				"/img/svg/refresh-cw.svg");
		terjemahUlang.setTooltiptext("Terjemahkan ULANG (menimpa) kolom English & Arab dari teks Bahasa Indonesia "
				+ "terkini memakai kamus internal, untuk seluruh baris sesuai filter. Gunakan bila terjemahan "
				+ "yang ada belum cocok/berubah. Baris yang sudah disunting manual juga akan ikut ditimpa.");
		terjemahUlang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onTerjemahkanUlang(arg0);
			}
		});
		Common.appendKeToolbar(terjemahUlang, add, comp);

		// Tombol "Terjemahkan Ulang via AI" — MENIMPA English/Arab/Mandarin HANYA memakai AI (Ollama),
		// TANPA kamus internal. Baris yang gagal AI (Ollama mati) tidak ditimpa. Paralel maks 50.
		MyToolbarbuttonConfig terjemahAi = new MyToolbarbuttonConfig("Terjemahkan Ulang via AI", "/img/svg/gear.svg");
		terjemahAi.setTooltiptext("Terjemahkan ULANG (menimpa) English/Arab/Mandarin HANYA via AI Ollama (tanpa "
				+ "kamus internal) untuk baris sesuai filter. Lebih berkualitas tapi lebih lambat. Baris yang gagal "
				+ "dinilai AI (Ollama mati/sibuk) TIDAK ditimpa.");
		terjemahAi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onTerjemahkanUlangAiSaja(arg0);
			}
		});
		Common.appendKeToolbar(terjemahAi, add, comp);

		// Tombol "Hapus Tak Bermakna" (berbantuan AI Ollama) — menilai & MENGHAPUS label junk (nama/email/
		// karakter acak/data uji) untuk baris SESUAI FILTER, paralel maks 50 + progress popup.
		MyToolbarbuttonConfig hapusTakBermakna = new MyToolbarbuttonConfig("Hapus Tak Bermakna", "/img/svg/trash.svg");
		hapusTakBermakna.setTooltiptext("Menilai (AI Ollama) lalu MENGHAPUS PERMANEN label yang tidak bermakna "
				+ "(nama orang, email, karakter acak, data uji) untuk baris sesuai filter. Paralel maks 50 thread. "
				+ "Baris yang ragu / saat AI tak tersedia TIDAK dihapus.");
		hapusTakBermakna.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onHapusTakBermakna(arg0);
			}
		});
		Common.appendKeToolbar(hapusTakBermakna, add, comp);
	}

	/** Konfirmasi lalu jalankan hapus label tak bermakna (berbantuan Ollama, paralel + progres). */
	public void onHapusTakBermakna(final Event event) throws Exception {
		final org.zkoss.zk.ui.Page halaman = event.getTarget().getPage();
		MyMessageboxConfig.show(
				"Proses ini akan MENILAI (AI Ollama) lalu MENGHAPUS PERMANEN baris yang dinilai tidak bermakna "
						+ "(nama/email/karakter acak/data uji) sesuai filter saat ini. Baris yang ragu tidak dihapus. "
						+ "Lanjutkan?",
				"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
							mulaiHapusTakBermakna(halaman);
						}
					}
				});
	}

	private void mulaiHapusTakBermakna(org.zkoss.zk.ui.Page halaman) throws Exception {
		List<LabelBahasa> daftar = initCriteria(true).list();
		if (daftar == null || daftar.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, tidak ada baris yang sesuai filter untuk diproses.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		final ais.action.master.helper.HapusBahasaTakBermaknaHelper mesin =
				new ais.action.master.helper.HapusBahasaTakBermaknaHelper(daftar);

		final org.zkoss.zul.Window dlg = new org.zkoss.zul.Window();
		dlg.setTitle(Common.getBahasaConfig("Hapus Tak Bermakna (AI)"));
		dlg.setBorder("normal");
		dlg.setWidth("640px");
		dlg.setClosable(false);
		dlg.setPage(halaman);

		org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
		vb.setWidth("100%");
		vb.setStyle("padding:14px;");
		vb.setParent(dlg);

		org.zkoss.zul.Label judul = new org.zkoss.zul.Label(
				Common.getBahasaConfig("Menilai & menghapus label tidak bermakna (AI, paralel). Mohon tunggu...") + " ("
						+ mesin.getParalel() + " thread)");
		judul.setMultiline(true);
		judul.setParent(vb);

		final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter();
		meter.setWidth("100%");
		meter.setValue(0);
		meter.setStyle("margin:10px 0;height:18px;");
		meter.setParent(vb);

		final org.zkoss.zul.Label persenLbl = new org.zkoss.zul.Label("0%");
		persenLbl.setStyle("font-weight:bold;font-size:15px;");
		persenLbl.setParent(vb);

		org.zkoss.zul.Label capStream = new org.zkoss.zul.Label(Common.getBahasaConfig("Kandidat dihapus (terbaru):"));
		capStream.setStyle("font-size:11px;color:#64748b;margin-top:8px;");
		capStream.setParent(vb);
		org.zkoss.zul.Div streamBox = new org.zkoss.zul.Div();
		streamBox.setStyle("height:150px;overflow:auto;border:1px solid #e2e8f0;border-radius:6px;padding:8px;"
				+ "background:#fef2f2;font-size:12px;line-height:1.5;font-family:monospace;color:#b91c1c;");
		streamBox.setParent(vb);
		final org.zkoss.zul.Label streamLbl = new org.zkoss.zul.Label("");
		streamLbl.setMultiline(true);
		streamLbl.setPre(true);
		streamLbl.setParent(streamBox);

		final org.zkoss.zul.Label capThread = new org.zkoss.zul.Label(
				Common.getBahasaConfig("Progres per-thread") + " (" + mesin.getParalel() + "):");
		capThread.setStyle("font-size:11px;color:#64748b;margin-top:8px;");
		capThread.setParent(vb);
		org.zkoss.zul.Div threadBox = new org.zkoss.zul.Div();
		threadBox.setStyle("height:130px;overflow:auto;border:1px solid #e2e8f0;border-radius:6px;padding:8px;"
				+ "background:#0f172a;color:#93c5fd;font-size:11px;line-height:1.5;font-family:monospace;");
		threadBox.setParent(vb);
		final org.zkoss.zul.Label threadLbl = new org.zkoss.zul.Label("");
		threadLbl.setMultiline(true);
		threadLbl.setPre(true);
		threadLbl.setParent(threadBox);

		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
		timer.setDelay(400);
		timer.setRepeats(true);
		timer.setParent(dlg);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				int p = mesin.persen();
				meter.setValue(p);
				if (mesin.isFasePersiapan()) {
					persenLbl.setValue(p + "%  —  " + Common.getBahasaConfig("menilai") + " " + mesin.getDiperiksa()
							+ " / " + mesin.getTotalSumber() + "  ("
							+ Common.getBahasaConfig("kandidat dihapus") + " " + mesin.getAkanDihapus() + ")");
				} else {
					persenLbl.setValue(p + "%  —  " + Common.getBahasaConfig("menghapus") + " " + mesin.getDihapus()
							+ " / " + mesin.getAkanDihapus());
				}
				try {
					streamLbl.setValue(mesin.getTerkiniGabung());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:372");
				}
				try {
					String[] st = mesin.getStatusThread();
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < st.length; i++) {
						String v = st[i] == null || st[i].trim().length() == 0 ? "…" : st[i];
						sb.append("#").append(i + 1 < 10 ? "0" + (i + 1) : "" + (i + 1)).append("  ").append(v)
								.append("\n");
					}
					threadLbl.setValue(sb.toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:383");
				}
				if (mesin.isSelesai()) {
					timer.stop();
					try {
						onSearchDefault(null);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:389");
					}
					try {
						dlg.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:393");
					}
					MyMessageboxConfig.showFormat(
							"Selesai. {V1} baris diperiksa, {V2} baris tidak bermakna DIHAPUS. (Baris yang ragu / "
									+ "saat AI tak tersedia tidak dihapus.)",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							Integer.valueOf(mesin.getDiperiksa()), Integer.valueOf(mesin.getDihapus()));
				}
			}
		});

		mesin.mulai();
		dlg.doHighlighted();
		timer.start();
	}

	class LabelBahasaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LabelBahasa labelBahasa = (LabelBahasa) arg1;

			RevisiHelper.createNewRevisi(LabelBahasa.class, labelBahasa, labelBahasa.getNama()).setParent(arg0);
			new Label(labelBahasa.getIndonesia()).setParent(arg0);
			new Label(labelBahasa.getEnglish()).setParent(arg0);
			new Label(labelBahasa.getArab()).setParent(arg0);
			new Label(labelBahasa.getMandarin()).setParent(arg0);
			new Label(labelBahasa.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			// Tombol "Terjemahkan ulang baris ini" via AI (Ollama) — memaksa terjemah Ollama utk 1 baris
			// (English/Arab/Mandarin), lalu simpan ke DB + cache. Bila Ollama mati → otomatis kamus internal.
			final MyToolbarbuttonConfig terjemahBtn = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
			terjemahBtn.setTooltiptext("Terjemahkan ulang baris ini (AI)");
			terjemahBtn.setVisible(edit);
			// onClick: tampilkan indikator "Sedang menerjemahkan (AI)…" DULU, lalu kerja AI dijalankan lewat
			// echoEvent (round-trip ke-2) sehingga indikator sempat tampil (bukan UI beku tanpa umpan balik).
			terjemahBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					org.zkoss.zk.ui.util.Clients
							.showBusy(Common.getBahasaConfig("Sedang menerjemahkan (AI)…"));
					org.zkoss.zk.ui.event.Events.echoEvent("onKerjaTerjemahAI", terjemahBtn, null);
				}
			});
			terjemahBtn.addEventListener("onKerjaTerjemahAI", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						terjemahUlangSatuBaris(labelBahasa);
						onSearchDefault(event);
					} finally {
						org.zkoss.zk.ui.util.Clients.clearBusy();
					}
				}
			});
			terjemahBtn.setParent(toolbar);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(labelBahasa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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
											Common.refreshDelete(labelBahasa);
											onSearchDefault(event);
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
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Terjemahkan ULANG satu baris via AI (Ollama; otomatis fallback kamus internal bila Ollama mati).
	 * English/Arab/Mandarin diterjemah PARALEL (cepat), lalu disimpan ke DB + cache memori.
	 */
	private void terjemahUlangSatuBaris(final LabelBahasa lb) {
		try {
			if (lb == null || lb.getId() == null || lb.getIndonesia() == null
					|| lb.getIndonesia().trim().length() == 0) {
				return;
			}
			final String id = lb.getIndonesia().trim();
			java.util.concurrent.ExecutorService ex = java.util.concurrent.Executors.newFixedThreadPool(3);
			try {
				java.util.concurrent.Future<String> fEn = ex.submit(new java.util.concurrent.Callable<String>() {
					@Override
					public String call() {
						return ais.common.AiTerjemah.terjemah(id, "english");
					}
				});
				java.util.concurrent.Future<String> fAr = ex.submit(new java.util.concurrent.Callable<String>() {
					@Override
					public String call() {
						return ais.common.AiTerjemah.terjemah(id, "arab");
					}
				});
				java.util.concurrent.Future<String> fZh = ex.submit(new java.util.concurrent.Callable<String>() {
					@Override
					public String call() {
						return ais.common.AiTerjemah.terjemah(id, "mandarin");
					}
				});
				String en = fEn.get();
				String ar = fAr.get();
				String zh = fZh.get();

				Session session = HibernateUtil.currentSession();
				LabelBahasa db = (LabelBahasa) session.load(LabelBahasa.class, lb.getId());
				db.setEnglish(en);
				db.setArab(ar);
				db.setMandarin(zh);
				Common.refreshUpdate(session, db);

				MemoryDbUtil.getBahasaEnglishs().put(db.getNama(), en);
				MemoryDbUtil.getBahasaArabs().put(db.getNama(), ar);
				MemoryDbUtil.getBahasaMandarins().put(db.getNama(), zh);
			} finally {
				try {
					ex.shutdown();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:553");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new LabelBahasa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(LabelBahasa labelBahasa) {
		this.labelBahasa = labelBahasa;
		addWindow.setTitle(labelBahasa.getId() == null ? "Tambah Label Bahasa" : "Ubah Label Bahasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kunci Bahasa"));
		row.appendChild(nama = new Textbox(labelBahasa.getNama() == null ? "" : labelBahasa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa Indonesia"));
		row.appendChild(indonesia = new Textbox(labelBahasa.getIndonesia()));
		indonesia.setWidth("90%");
		indonesia.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("English"));
		row.appendChild(english = new Textbox(labelBahasa.getEnglish()));
		english.setWidth("90%");
		english.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Arab"));
		row.appendChild(arab = new Textbox(labelBahasa.getArab()));
		arab.setWidth("90%");
		arab.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mandarin"));
		row.appendChild(mandarin = new Textbox(labelBahasa.getMandarin()));
		mandarin.setWidth("90%");
		mandarin.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(labelBahasa.getKeterangan() == null ? "" : labelBahasa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kunci Bahasa",
					"Kolom Kunci Bahasa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kunci Bahasa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaLabelBahasa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kunci Bahasa",
					"Kunci Bahasa sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Kunci Bahasa yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (labelBahasa.getId() != null) {
			labelBahasa = (LabelBahasa) session.load(LabelBahasa.class, labelBahasa.getId());

		}

		labelBahasa.setNama(nama.getValue());
		labelBahasa.setKeterangan(keterangan.getValue());
		labelBahasa.setIndonesia(indonesia.getValue().trim());
		labelBahasa.setEnglish(english.getValue().trim());
		labelBahasa.setArab(arab.getValue().trim());
		labelBahasa.setMandarin(mandarin.getValue().trim());

		Common.refreshUpdate(session, labelBahasa);

		MemoryDbUtil.getBahasaIndonesias().put(labelBahasa.getNama(), labelBahasa.getIndonesia());
		MemoryDbUtil.getBahasaEnglishs().put(labelBahasa.getNama(), labelBahasa.getEnglish());
		MemoryDbUtil.getBahasaArabs().put(labelBahasa.getNama(), labelBahasa.getArab());
		MemoryDbUtil.getBahasaMandarins().put(labelBahasa.getNama(), labelBahasa.getMandarin());

		// Catat pula sebagai NILAI DEFAULT ke file seed (WEB-INF/DEFAULT_*.conf) — DB tetap acuan.
		ais.common.DefaultBahasaSeed.simpan(labelBahasa.getNama(), labelBahasa.getIndonesia(),
				labelBahasa.getEnglish(), labelBahasa.getArab());

		return true;
	}

	/**
	 * <b>Terjemahkan Otomatis</b> (manual-assisted, kamus internal): untuk SEMUA baris sesuai filter yang
	 * kolom English/Arab-nya masih KOSONG atau masih sama dengan teks Indonesia (belum diterjemahkan),
	 * isi otomatis dari {@link ais.common.KamusBahasaInternal}. Kata yang belum dikenal kamus dibiarkan agar
	 * dilengkapi manual. Hasil disimpan ke DB, cache memori, dan file seed .conf.
	 */
	public void onTerjemahkanOtomatis(Event event) throws Exception {
		mulaiTerjemahMassal(false, event.getTarget().getPage());
	}

	/**
	 * <b>Terjemahkan Ulang via AI</b>: MENIMPA English/Arab/Mandarin HANYA memakai AI Ollama (tanpa kamus
	 * internal), untuk baris sesuai filter. Baris yang gagal dinilai AI (Ollama mati/sibuk) TIDAK ditimpa.
	 */
	public void onTerjemahkanUlangAiSaja(Event event) throws Exception {
		final org.zkoss.zk.ui.Page halaman = event.getTarget().getPage();
		MyMessageboxConfig.show(
				"Proses \"Terjemahkan Ulang via AI\" akan MENIMPA English/Arab/Mandarin untuk baris sesuai filter "
						+ "HANYA menggunakan AI (Ollama), tanpa kamus internal. Lebih berkualitas namun lebih lambat, "
						+ "dan membutuhkan server AI aktif. Baris yang gagal dinilai AI tidak akan ditimpa. Lanjutkan?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (Integer.parseInt(e.getData().toString()) == MyMessageboxConfig.OK) {
							mulaiTerjemahMassal(true, halaman, true);
						}
					}
				});
	}

	/**
	 * <b>Terjemahkan Ulang</b> (manual-assisted, kamus internal): MENIMPA kolom English &amp; Arab dari teks
	 * Bahasa Indonesia terkini untuk SEMUA baris sesuai filter — berbeda dari {@link #onTerjemahkanOtomatis}
	 * yang hanya mengisi baris yang masih kosong/sama dengan Indonesia. Berguna untuk mengoreksi terjemahan
	 * yang belum cocok atau setelah teks Indonesia berubah. Karena menimpa (termasuk hasil suntingan manual),
	 * proses diawali konfirmasi.
	 */
	public void onTerjemahkanUlang(Event event) throws Exception {
		final org.zkoss.zk.ui.Page halaman = event.getTarget().getPage();
		MyMessageboxConfig.show(
				"Proses \"Terjemahkan Ulang\" akan MENGGANTI (menimpa) terjemahan English dan Arab untuk seluruh "
						+ "baris sesuai filter yang dipilih, berdasarkan teks Bahasa Indonesia terkini melalui kamus "
						+ "internal. Terjemahan yang sebelumnya sudah Bapak/Ibu perbaiki secara manual pada baris "
						+ "tersebut akan ikut ditimpa. Apabila hanya ingin mengisi baris yang belum diterjemahkan, "
						+ "gunakan tombol \"Terjemahkan Otomatis\". Apakah Bapak/Ibu yakin ingin melanjutkan?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (Integer.parseInt(e.getData().toString()) == MyMessageboxConfig.OK) {
							mulaiTerjemahMassal(true, halaman);
						}
					}
				});
	}

	/**
	 * Jalankan terjemah massal PARALEL (maks 50 thread) untuk seluruh baris sesuai filter, sambil
	 * menampilkan dialog PROGRESS BAR (persentase real-time). {@code timpa=false} → hanya isi baris yang
	 * belum diterjemah; {@code timpa=true} → menimpa semua (Terjemahkan Ulang). Proses berjalan di thread
	 * latar; Timer ZK memperbarui progres, dan saat selesai menutup dialog + menyegarkan grid + menampilkan
	 * ringkasan.
	 */
	@SuppressWarnings("unchecked")
	private void mulaiTerjemahMassal(boolean timpa, org.zkoss.zk.ui.Page halaman) throws Exception {
		mulaiTerjemahMassal(timpa, halaman, false);
	}

	private void mulaiTerjemahMassal(boolean timpa, org.zkoss.zk.ui.Page halaman, boolean aiOnly) throws Exception {
		List<LabelBahasa> daftar = initCriteria(true).list();
		if (daftar == null || daftar.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, tidak ada baris yang sesuai filter untuk diproses.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		final ais.action.master.helper.TerjemahMassalHelper mesin =
				new ais.action.master.helper.TerjemahMassalHelper(daftar, timpa, aiOnly);

		final org.zkoss.zul.Window dlg = new org.zkoss.zul.Window();
		dlg.setTitle(Common.getBahasaConfig(
				aiOnly ? "Terjemahkan Ulang via AI" : (timpa ? "Terjemahkan Ulang" : "Terjemahkan Otomatis")));
		dlg.setBorder("normal");
		dlg.setWidth("640px");
		dlg.setClosable(false);
		dlg.setPage(halaman);

		org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
		vb.setWidth("100%");
		vb.setStyle("padding:14px;");
		vb.setParent(dlg);

		org.zkoss.zul.Label judul = new org.zkoss.zul.Label(Common.getBahasaConfig(
				"Sedang memproses terjemahan secara paralel. Mohon tunggu...") + " (" + mesin.getParalel()
				+ " thread)");
		judul.setMultiline(true);
		judul.setParent(vb);

		final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter();
		meter.setWidth("100%");
		meter.setValue(0);
		meter.setStyle("margin:10px 0;height:18px;");
		meter.setParent(vb);

		final org.zkoss.zul.Label persenLbl = new org.zkoss.zul.Label("0%");
		persenLbl.setStyle("font-weight:bold;font-size:15px;");
		persenLbl.setParent(vb);

		// Aliran hasil terjemahan TERBARU (streaming) — "teks apa yang sedang diproses".
		org.zkoss.zul.Label capStream = new org.zkoss.zul.Label(
				Common.getBahasaConfig("Hasil terbaru (streaming):"));
		capStream.setStyle("font-size:11px;color:#64748b;margin-top:8px;");
		capStream.setParent(vb);
		org.zkoss.zul.Div streamBox = new org.zkoss.zul.Div();
		streamBox.setStyle("height:150px;overflow:auto;border:1px solid #e2e8f0;border-radius:6px;padding:8px;"
				+ "background:#f8fafc;font-size:12px;line-height:1.5;font-family:monospace;");
		streamBox.setParent(vb);
		final org.zkoss.zul.Label streamLbl = new org.zkoss.zul.Label("");
		streamLbl.setMultiline(true);
		streamLbl.setPre(true);
		streamLbl.setParent(streamBox);

		// Progres tiap thread (maks 50 paralel) — apa yang sedang dikerjakan tiap thread.
		final org.zkoss.zul.Label capThread = new org.zkoss.zul.Label(
				Common.getBahasaConfig("Progres per-thread") + " (" + mesin.getParalel() + "):");
		capThread.setStyle("font-size:11px;color:#64748b;margin-top:8px;");
		capThread.setParent(vb);
		org.zkoss.zul.Div threadBox = new org.zkoss.zul.Div();
		threadBox.setStyle("height:150px;overflow:auto;border:1px solid #e2e8f0;border-radius:6px;padding:8px;"
				+ "background:#0f172a;color:#93c5fd;font-size:11px;line-height:1.5;font-family:monospace;");
		threadBox.setParent(vb);
		final org.zkoss.zul.Label threadLbl = new org.zkoss.zul.Label("");
		threadLbl.setMultiline(true);
		threadLbl.setPre(true);
		threadLbl.setParent(threadBox);

		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
		timer.setDelay(400);
		timer.setRepeats(true);
		timer.setParent(dlg);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				int p = mesin.persen();
				meter.setValue(p);
				if (mesin.isFasePersiapan()) {
					persenLbl.setValue(p + "%  —  " + Common.getBahasaConfig("menerjemahkan") + " "
							+ mesin.getDiterjemah() + " / " + mesin.getTotalSumber());
				} else {
					persenLbl.setValue(p + "%  —  " + Common.getBahasaConfig("menyimpan") + " " + mesin.getDiproses()
							+ " / " + mesin.getTotal() + " (" + Common.getBahasaConfig("berhasil") + " "
							+ mesin.getDiperbarui() + ")");
				}
				// streaming hasil terbaru
				try {
					streamLbl.setValue(mesin.getTerkiniGabung());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:865");
				}
				// status per-thread
				try {
					String[] st = mesin.getStatusThread();
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < st.length; i++) {
						String v = st[i] == null || st[i].trim().length() == 0 ? "…" : st[i];
						sb.append("#").append(i + 1 < 10 ? "0" + (i + 1) : "" + (i + 1)).append("  ").append(v)
								.append("\n");
					}
					threadLbl.setValue(sb.toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:877");
				}
				if (mesin.isSelesai()) {
					timer.stop();
					mesin.terapkanKeMemori();
					try {
						onSearchDefault(null);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:884");
					}
					try {
						dlg.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LabelBahasaAction.java:888");
					}
					MyMessageboxConfig.showFormat(
							"Proses terjemahan (kamus internal, {V1} thread paralel) selesai. Sebanyak {V2} baris "
									+ "diproses, {V3} baris diperbarui. Mohon Bapak/Ibu periksa dan lengkapi secara manual "
									+ "istilah yang belum dikenali kamus agar terjemahan menjadi sempurna.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							Integer.valueOf(mesin.getParalel()), Integer.valueOf(mesin.getTotal()),
							Integer.valueOf(mesin.getDiperbarui()));
				}
			}
		});

		mesin.mulai();
		dlg.doHighlighted();
		timer.start();
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(LabelBahasa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		// Filter status terjemahan: "BELUM" = English ATAU Arab masih kosong / sama dengan Indonesia
		// (belum diterjemahkan); "SUDAH" = keduanya sudah beda dari Indonesia & tidak kosong.
		String statusTerjemah = searchStatusTerjemah == null || searchStatusTerjemah.getSelectedItem() == null ? ""
				: (String) searchStatusTerjemah.getSelectedItem().getValue();
		if ("BELUM".equals(statusTerjemah)) {
			criteria.add(Restrictions.or(
					Restrictions.or(
							Restrictions.or(Restrictions.isNull("english"),
									Restrictions.or(Restrictions.eq("english", ""),
											Restrictions.eqProperty("english", "indonesia"))),
							Restrictions.or(Restrictions.isNull("arab"),
									Restrictions.or(Restrictions.eq("arab", ""),
											Restrictions.eqProperty("arab", "indonesia")))),
					Restrictions.or(Restrictions.isNull("mandarin"),
							Restrictions.or(Restrictions.eq("mandarin", ""),
									Restrictions.eqProperty("mandarin", "indonesia")))));
		} else if ("SUDAH".equals(statusTerjemah)) {
			criteria.add(Restrictions.isNotNull("english"));
			criteria.add(Restrictions.ne("english", ""));
			criteria.add(Restrictions.neProperty("english", "indonesia"));
			criteria.add(Restrictions.isNotNull("arab"));
			criteria.add(Restrictions.ne("arab", ""));
			criteria.add(Restrictions.neProperty("arab", "indonesia"));
			criteria.add(Restrictions.isNotNull("mandarin"));
			criteria.add(Restrictions.ne("mandarin", ""));
			criteria.add(Restrictions.neProperty("mandarin", "indonesia"));
		}

		criteria

				.add(searchnamabahasa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnamabahasa.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("english", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("indonesia", searchnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("arab", searchnama.getValue().trim(), MatchMode.ANYWHERE))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<LabelBahasa> labelBahasa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(labelBahasa);
		grid.setRowRenderer(new LabelBahasaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaLabelBahasa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(LabelBahasa.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.labelBahasa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.labelBahasa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
