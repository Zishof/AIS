package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.format1.rab.LaporanRealisasi;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Halaman <b>Realisasi Anggaran Bulanan (RAB)</b> - rekap penyerapan anggaran per bulan.
 *
 * <p>Menampilkan ringkasan item anggaran (pagu vs realisasi) yang dihitung dari data
 * {@code PenggunaanAnggaran}, dengan status keaktifan mengikuti logika object ({@code getAktif()}),
 * bukan sekadar join SQL. Penyusunan ringkasan dan pemeliharaan agregat realisasi parent dilakukan
 * lewat {@link ais.action.master.rab.util.WorkspaceTreeModel}.</p>
 *
 * <h3>Higiene session basis data</h3>
 * Kelas ini SUDAH menutup session dengan benar dan tidak diubah:
 * <ul>
 * <li>Pembacaan ringkasan via {@code openSession()} dibungkus {@code try/catch/finally} dan ditutup
 * di {@code finally} ({@code session.close()}).</li>
 * <li>Pemrosesan agregat per-data dijalankan di thread terpisah dengan {@code openSession()} sendiri
 * ("WAJIB: Session baru khusus thread ini"), commit/rollback rapi, dan ditutup di {@code finally}.</li>
 * <li>Operasi {@code currentNativeSession()} lain juga sudah berada dalam {@code try/finally} yang
 * memanggil {@code disconnect()} + {@code HibernateUtil.closeSession()}.</li>
 * </ul>
 * Dengan demikian tidak ada koneksi yang menggantung; sesuai aturan: native/openSession ditutup di
 * {@code finally}, sedangkan session ThreadLocal ({@code currentSession()}) dibiarkan ditutup otomatis.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 ({@code try/catch} gaya Java 1.6). Pemrosesan berat dijalankan
 * asinkron/berthread agar UI tetap responsif; pemakaian {@code ConstantValues.ambil} memanfaatkan
 * cache demi hemat memori dan kueri.
 */
public class RealisasiBulananAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	private Tree tree;
	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox sumberDana;
	private MyLabelConfig sumberDanaLabel;
	private WorkspaceTreeModel workspaceTreeModel;
	private TreeMap<Workspace, Treecell[]> treecellMap = new TreeMap<Workspace, Treecell[]>();
	private Integer revisi = 1;
	private Tabpanel realisasiAnggaranTab;

	public void onRealisasiAnggaran(Event event) {
		if (realisasiAnggaranTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(realisasiAnggaranTab);
			MyInclude iframe = new MyInclude("/pages/master/rab/penggunaan_anggaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabDasborRealisasi;

	public void onDasborRealisasi(Event event) {
		if (tabDasborRealisasi.getChildren().size() == 0) {
			int tahunSekarang = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			initDasborRealisasi(tahunSekarang, "", "");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		// OPTIMASI: Menggunakan satu listener terpusat untuk combobox agar hemat memory
		// class
		EventListener comboChangeListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				prosesPerubahanCombo(arg0);
			}
		};

		tahunWorkspace.addEventListener("onChange", comboChangeListener);
		this.satuanKerja.setEventListener(comboChangeListener);

		sumberDana.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		initTree();

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onDasborRealisasi(null);
				onSearchDefault(null);
			}
		});
	}

	public void initDasborRealisasi(final Integer tahunFilter, final String searchWorkspace, final String searchPA) {
		RealisasiBulananAction.initDasborRealisasi(tabDasborRealisasi, tahunFilter, searchWorkspace, searchPA);
	}

	
	// Tambahkan import jika belum ada di class ini:
	// import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
	// import ais.action.master.rab.util.SatuanKerjaTreeModel;
	// import ais.database.model.Tbmuser;
	// import ais.database.model.rab.SatuanKerja;
	// import ais.database.model.rab.PenggunaanAnggaran;
	// import ais.database.model.rab.Workspace;

	public static void initDasborRealisasi(final org.zkoss.zul.Tabpanel tabDasborRealisasi,
			final Integer tahunFilter, final String searchWorkspace, final String searchPA) {
		try {
			initDasborRealisasi(tabDasborRealisasi, tahunFilter, searchWorkspace, searchPA, null, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void initDasborRealisasi(final org.zkoss.zul.Tabpanel tabDasborRealisasi, 
			final Integer tahunFilter, final String searchWorkspace, final String searchPA,
			final SatuanKerja satuanKerjaFilter, final boolean onlyOverBudget) throws Exception {
		if (tabDasborRealisasi == null) return;
		ais.common.Common.clear(tabDasborRealisasi);
		Common.clear(tabDasborRealisasi);

		final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		final int tahunAktif = tahunFilter == null ? currentYear : tahunFilter.intValue();
		final Tbmuser tbmuser = Common.getCurrentUser();

		// 1. Toolbar filter
		Toolbar dasborToolbar = new Toolbar();
		dasborToolbar.setWidth("100%");
		dasborToolbar.setStyle("border:0; background:#ffffff; padding:8px; overflow:auto; box-sizing:border-box;");
		dasborToolbar.setParent(tabDasborRealisasi);

		dasborToolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun: ")));
		final Combobox cmbTahun = new Combobox();
		cmbTahun.setWidth("80px");
		cmbTahun.setReadonly(true);
		for (int i = currentYear + 3; i >= currentYear - 20; i--) {
			cmbTahun.appendItem(String.valueOf(i));
		}
		cmbTahun.setValue(String.valueOf(tahunAktif));
		cmbTahun.setParent(dasborToolbar);

		dasborToolbar.appendChild(new org.zkoss.zul.Space());
		dasborToolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja: ")));
		final AmbilDataSatuanKerjaBanbox satuanKerja = new AmbilDataSatuanKerjaBanbox();
		satuanKerja.setWidth(Common.isMobile() ? "180px" : "240px");
		satuanKerja.setReadonly(true);
		if (satuanKerjaFilter != null) {
			satuanKerja.setAttribute("satuanKerja", satuanKerjaFilter);
			try {
				satuanKerja.setValue(satuanKerjaFilter.getNama());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiBulananAction.java:227");
				// Beberapa implementasi bandbox custom tidak expose setValue secara langsung.
			}
		}
		satuanKerja.setParent(dasborToolbar);

		dasborToolbar.appendChild(new org.zkoss.zul.Space());
		dasborToolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari Anggaran (Workspace): ")));
		final Textbox txtSearchW = new Textbox(searchWorkspace != null ? searchWorkspace : "");
		txtSearchW.setWidth(Common.isMobile() ? "150px" : "190px");
		txtSearchW.setParent(dasborToolbar);

		dasborToolbar.appendChild(new org.zkoss.zul.Space());
		dasborToolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari Realisasi: ")));
		final Textbox txtSearchPA = new Textbox(searchPA != null ? searchPA : "");
		txtSearchPA.setWidth(Common.isMobile() ? "150px" : "190px");
		txtSearchPA.setParent(dasborToolbar);

		dasborToolbar.appendChild(new org.zkoss.zul.Space());
		final org.zkoss.zul.Checkbox chkOverBudget = new org.zkoss.zul.Checkbox("Over Budget saja");
		chkOverBudget.setChecked(onlyOverBudget);
		chkOverBudget.setTooltiptext("Jika dicentang, buku besar, summary, dan dashboard hanya menampilkan anggaran yang realisasinya melebihi nilai anggaran.");
		chkOverBudget.setParent(dasborToolbar);

		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari", "/img/search.gif");
		btnCari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Integer thn = Integer.parseInt(cmbTahun.getValue());
				SatuanKerja selectedSatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				initDasborRealisasi(tabDasborRealisasi, thn, txtSearchW.getValue(), txtSearchPA.getValue(), selectedSatuanKerja, chkOverBudget.isChecked());
			}
		});
		btnCari.setParent(dasborToolbar);

		MyToolbarbuttonConfig btnReset = new MyToolbarbuttonConfig("Reset", "/img/refresh.gif");
		btnReset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initDasborRealisasi(tabDasborRealisasi, currentYear, "", "", null, false);
			}
		});
		btnReset.setParent(dasborToolbar);

		// Ambil seluruh id satuan kerja yang dipilih, termasuk child, mengikuti pola AmbilDataSatuanKerjaBanbox.
		final java.util.Set<Long> satuanKerjaIds = new java.util.LinkedHashSet<Long>();
		String satuanKerjaFilterLabel = "Semua Satuan Kerja";
		try {
			SatuanKerja parent = satuanKerjaFilter;
			if (parent != null) {
				satuanKerjaFilterLabel = parent.getNama() == null ? "Satuan Kerja Terpilih" : parent.getNama();

				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (satuanKerjas == null) {
					satuanKerjas = new java.util.HashSet<SatuanKerja>();
				}

				satuanKerjas.clear();
				satuanKerjas.add(parent);
				SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);

				if (tbmuser != null && tbmuser.ambilSatuanKerja() != null) {
					satuanKerjas.add(tbmuser.ambilSatuanKerja());
				}

				for (SatuanKerja sk : satuanKerjas) {
					if (sk != null && sk.getId() != null) {
						satuanKerjaIds.add(sk.getId());
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// 2. Layout utama. Tren realisasi sengaja dipisah ke portal paling bawah.
		ais.ui.util.MyPortallayout portalLayout = new ais.ui.util.MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setStyle("background:#f6f8fb; box-sizing:border-box;");
		portalLayout.setParent(tabDasborRealisasi);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		ais.ui.util.MyPortalchildren pcLeft = new ais.ui.util.MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px; box-sizing:border-box;");
		pcLeft.setParent(portalLayout);

		ais.ui.util.MyPortalchildren pcRight = new ais.ui.util.MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px; box-sizing:border-box;");
		pcRight.setParent(portalLayout);

		ais.ui.util.MyPortalchildren pcBottom = new ais.ui.util.MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px; box-sizing:border-box;");
		pcBottom.setParent(portalLayout);

		ais.ui.util.MyPortalchildren pcChartsBottom = new ais.ui.util.MyPortalchildren();
		pcChartsBottom.setWidth("100%");
		pcChartsBottom.setStyle("padding:6px; box-sizing:border-box;");
		pcChartsBottom.setParent(portalLayout);

		ais.ui.util.MyPortalchildren pcTrendBottom = new ais.ui.util.MyPortalchildren();
		pcTrendBottom.setWidth("100%");
		pcTrendBottom.setStyle("padding:6px; box-sizing:border-box;");
		pcTrendBottom.setParent(portalLayout);

		Session session = null;
		final List<java.util.Map<String, Object>> mutasiList = new ArrayList<java.util.Map<String, Object>>();
		final java.util.Map<Long, java.util.Map<String, Object>> summaryWorkspaceMap = new java.util.LinkedHashMap<Long, java.util.Map<String, Object>>();
		final java.util.Map<String, java.util.Map<String, Object>> summarySumberRealisasiMap = new java.util.LinkedHashMap<String, java.util.Map<String, Object>>();
		final java.util.Map<Integer, Double> trendPerBulan = new java.util.TreeMap<Integer, Double>();
		final org.zkoss.zul.SimpleCategoryModel trendModel = new org.zkoss.zul.SimpleCategoryModel();

		double totalAnggaran = 0.0;
		double totalRealisasi = 0.0;
		double totalRealisasiObjectAktif = 0.0;
		int totalTransaksi = 0;
		int jumlahWorkspaceAdaRealisasi = 0;
		int jumlahWorkspaceBelumRealisasi = 0;
		int jumlahWorkspaceOverBudget = 0;
		int jumlahWorkspaceSerapanRendah = 0;
		int jumlahWorkspaceSerapanSedang = 0;
		int jumlahWorkspaceSerapanTinggi = 0;
		int jumlahPenggunaanAnggaranObjectAktif = 0;
		int jumlahPenggunaanAnggaranObjectTidakAktif = 0;
		int jumlahPenggunaanAnggaranAktifObjectDbBelumTrue = 0;
		int jumlahPenggunaanAnggaranDbTrueObjectTidakAktif = 0;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Ambil daftar workspace terlebih dahulu. Realisasi tidak lagi di-join melalui SQL,
			// karena status aktif PenggunaanAnggaran harus mengikuti logic object getAktif().
			StringBuilder sqlWorkspace = new StringBuilder();
			sqlWorkspace.append("SELECT ");
			sqlWorkspace.append("w.id as w_id, ");
			sqlWorkspace.append("w.kode as w_kode, ");
			sqlWorkspace.append("w.nama as w_nama, ");
			sqlWorkspace.append("w.harga_total as w_anggaran, ");
			sqlWorkspace.append("w.satuan_kerja as w_satuan_kerja ");
			sqlWorkspace.append("FROM rab.workspace w ");
			sqlWorkspace.append("WHERE w.tahun_workspace = :tahun ");
			sqlWorkspace.append("AND (w.aktif = true OR w.aktif IS NULL) ");
			sqlWorkspace.append("AND w.harga_total > 0 ");
			sqlWorkspace.append("AND w.leaf = true ");

			if (!satuanKerjaIds.isEmpty()) {
				sqlWorkspace.append("AND w.satuan_kerja IN (:satuanKerjaIds) ");
			}
			if (searchWorkspace != null && !searchWorkspace.trim().isEmpty()) {
				sqlWorkspace.append("AND (w.kode ILIKE :searchW OR w.nama ILIKE :searchW) ");
			}

			sqlWorkspace.append("ORDER BY w.id ASC");

			org.hibernate.SQLQuery queryWorkspace = session.createSQLQuery(sqlWorkspace.toString());
			queryWorkspace.setParameter("tahun", tahunAktif);
			if (!satuanKerjaIds.isEmpty()) {
				queryWorkspace.setParameterList("satuanKerjaIds", satuanKerjaIds);
			}
			if (searchWorkspace != null && !searchWorkspace.trim().isEmpty()) {
				queryWorkspace.setParameter("searchW", "%" + searchWorkspace.trim() + "%");
			}

			List<Object[]> rowsWorkspace = queryWorkspace.list();
			final java.util.Set<Long> workspaceIds = new java.util.LinkedHashSet<Long>();

			for (Object[] row : rowsWorkspace) {
				Long wId = row[0] != null ? ((Number) row[0]).longValue() : null;
				if (wId == null) continue;

				String wKode = row[1] != null ? (String) row[1] : "";
				String wNama = row[2] != null ? (String) row[2] : "";
				Double wAnggaran = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
				Long wSatuanKerjaId = row[4] != null ? ((Number) row[4]).longValue() : null;

				workspaceIds.add(wId);

				java.util.Map<String, Object> summary = new java.util.HashMap<String, Object>();
				summary.put("wId", wId);
				summary.put("anggaranFull", wKode + " - " + wNama);
				summary.put("satuanKerjaId", wSatuanKerjaId);
				summary.put("anggaranNilai", wAnggaran);
				summary.put("realisasiNilai", 0.0);
				summary.put("jumlahTransaksi", 0);
				summary.put("tanggalTerakhir", null);
				summary.put("hasRealisasi", false);
				summaryWorkspaceMap.put(wId, summary);
			}

			final java.util.Map<Long, Boolean> paAktifDbMap = new java.util.HashMap<Long, Boolean>();
			final java.util.Map<Long, Long> paWorkspaceDbMap = new java.util.HashMap<Long, Long>();
			if (!workspaceIds.isEmpty()) {
				// Raw DB aktif hanya dipakai sebagai informasi monitoring sinkronisasi,
				// bukan sebagai filter utama.
				try {
					org.hibernate.SQLQuery queryAktifDb = session.createSQLQuery("SELECT id, aktif, workspace FROM rab.penggunaan_anggaran WHERE workspace IN (:workspaceIds)");
					queryAktifDb.setParameterList("workspaceIds", workspaceIds);
					List<Object[]> rowsAktifDb = queryAktifDb.list();
					for (Object[] rowAktif : rowsAktifDb) {
						Long paIdRaw = rowAktif[0] != null ? ((Number) rowAktif[0]).longValue() : null;
						Boolean aktifRaw = rowAktif[1] == null ? null : (Boolean) rowAktif[1];
						Long workspaceRaw = rowAktif[2] != null ? ((Number) rowAktif[2]).longValue() : null;
						if (paIdRaw != null) {
							paAktifDbMap.put(paIdRaw, aktifRaw);
							if (workspaceRaw != null) {
								paWorkspaceDbMap.put(paIdRaw, workspaceRaw);
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				org.hibernate.Criteria paCriteria = session.createCriteria(ais.database.model.rab.PenggunaanAnggaran.class);
				paCriteria.createAlias("workspace", "w");
				paCriteria.add(org.hibernate.criterion.Restrictions.in("w.id", workspaceIds));

				List<ais.database.model.rab.PenggunaanAnggaran> penggunaanAnggarans = paCriteria.list();
				java.util.Collections.sort(penggunaanAnggarans, new java.util.Comparator<ais.database.model.rab.PenggunaanAnggaran>() {
					@Override
					public int compare(ais.database.model.rab.PenggunaanAnggaran o1, ais.database.model.rab.PenggunaanAnggaran o2) {
						Long w1 = null;
						Long w2 = null;
						try { w1 = o1 != null && o1.getWorkspace() != null ? o1.getWorkspace().getId() : null; } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try { w2 = o2 != null && o2.getWorkspace() != null ? o2.getWorkspace().getId() : null; } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						if (w1 == null && w2 != null) return -1;
						if (w1 != null && w2 == null) return 1;
						if (w1 != null && w2 != null && !w1.equals(w2)) return w1.compareTo(w2);

						Date d1 = null;
						Date d2 = null;
						try { d1 = o1 != null ? o1.getWaktu() : null; } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try { d2 = o2 != null ? o2.getWaktu() : null; } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						if (d1 == null && d2 != null) return -1;
						if (d1 != null && d2 == null) return 1;
						if (d1 != null && d2 != null && !d1.equals(d2)) return d1.compareTo(d2);

						Long id1 = o1 != null ? o1.getId() : null;
						Long id2 = o2 != null ? o2.getId() : null;
						if (id1 == null && id2 != null) return -1;
						if (id1 != null && id2 == null) return 1;
						if (id1 == null && id2 == null) return 0;
						return id1.compareTo(id2);
					}
				});

				String searchPALower = searchPA == null ? "" : searchPA.trim().toLowerCase();

				for (ais.database.model.rab.PenggunaanAnggaran pa : penggunaanAnggarans) {
					if (pa == null) continue;

					Long paId = pa.getId();
					Boolean rawAktifDb = paId == null ? null : paAktifDbMap.get(paId);
					boolean aktifObject = false;
					try {
						aktifObject = Boolean.TRUE.equals(pa.getAktif());
					} catch (Exception e) {
						aktifObject = false;
					}

					if (!aktifObject) {
						jumlahPenggunaanAnggaranObjectTidakAktif++;
						if (Boolean.TRUE.equals(rawAktifDb)) {
							jumlahPenggunaanAnggaranDbTrueObjectTidakAktif++;
						}
						continue;
					}

					ais.database.model.rab.Workspace workspacePa = null;
					try {
						workspacePa = pa.getWorkspace();
					} catch (Exception e) {
						workspacePa = null;
					}
					Long wId = workspacePa != null ? workspacePa.getId() : null;
					if (wId == null || !summaryWorkspaceMap.containsKey(wId)) {
						Long rawWorkspaceId = paId == null ? null : paWorkspaceDbMap.get(paId);
						if (rawWorkspaceId != null && summaryWorkspaceMap.containsKey(rawWorkspaceId)) {
							wId = rawWorkspaceId;
						} else {
							continue;
						}
					}

					String paKode = "";
					String paNama = "";
					String paKet = "";
					Date tgl = null;
					Double paRealisasi = 0.0;
					try { paKode = pa.getKode() == null ? "" : pa.getKode(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try { paNama = pa.getNama() == null ? "" : pa.getNama(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try { paKet = pa.getKeterangan() == null ? "" : pa.getKeterangan(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try { tgl = pa.getWaktu(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try { paRealisasi = pa.getNilai() == null ? 0.0 : pa.getNilai(); } catch (Exception e) { paRealisasi = 0.0; }

					if (!searchPALower.isEmpty()) {
						String textCariPa = (paKode + " " + paNama + " " + paKet).toLowerCase();
						if (textCariPa.indexOf(searchPALower) < 0) {
							continue;
						}
					}

					String sumberRealisasi = "Manual / Lainnya";
					try {
						if (pa.getGrupTransaksi() != null) sumberRealisasi = "Jurnal Umum";
						else if (pa.getUangMuka() != null) sumberRealisasi = "Uang Muka";
						else if (pa.getPermintaanPengadaanMasterAssetDetail() != null) sumberRealisasi = "Permintaan Pengadaan";
						else if (pa.getSaldoAwalMasterAssetDetail() != null) sumberRealisasi = "Saldo Awal / Rutin";
						else if (pa.getPembayaranGaji() != null) sumberRealisasi = "Pembayaran Gaji";
						else if (pa.getKasKecil() != null) sumberRealisasi = "Kas Kecil";
						else if (pa.getKasBesar() != null) sumberRealisasi = "Kas Besar";
						else if (pa.getPertangungjawaban() != null) sumberRealisasi = "Pertanggungjawaban";
					} catch (Exception e) {
						sumberRealisasi = "Manual / Lainnya";
					}

					java.util.Map<String, Object> summary = summaryWorkspaceMap.get(wId);
					double nilaiSebelumnya = summary.get("realisasiNilai") == null ? 0.0 : ((Double) summary.get("realisasiNilai")).doubleValue();
					summary.put("realisasiNilai", nilaiSebelumnya + paRealisasi);
					summary.put("jumlahTransaksi", ((Integer) summary.get("jumlahTransaksi")) + 1);
					summary.put("hasRealisasi", true);
					Date tanggalTerakhir = (Date) summary.get("tanggalTerakhir");
					if (tgl != null && (tanggalTerakhir == null || tgl.after(tanggalTerakhir))) {
						summary.put("tanggalTerakhir", tgl);
					}

					java.util.Map<String, Object> mapMutasi = new java.util.HashMap<String, Object>();
					mapMutasi.put("paId", paId);
					mapMutasi.put("wId", wId);
					mapMutasi.put("anggaranFull", summary.get("anggaranFull"));
					mapMutasi.put("realisasiFull", paKode + " - " + paNama);
					mapMutasi.put("anggaranNilai", summary.get("anggaranNilai"));
					mapMutasi.put("realisasiNilai", paRealisasi);
					mapMutasi.put("tgl", tgl);
					mapMutasi.put("ket", paKet);
					mapMutasi.put("hasRealisasi", true);
					mapMutasi.put("sumberRealisasi", sumberRealisasi);
					mapMutasi.put("rawAktifDb", rawAktifDb);
					mutasiList.add(mapMutasi);
				}
			}

			// Jika filter Cari Realisasi kosong, workspace tanpa PenggunaanAnggaran aktif tetap ditampilkan
			// sebagai baris "Belum ada realisasi". Jika Cari Realisasi diisi, workspace tanpa realisasi
			// yang cocok disembunyikan agar hasil pencarian fokus pada realisasi yang ditemukan.
			if (searchPA != null && !searchPA.trim().isEmpty()) {
				java.util.Iterator<java.util.Map.Entry<Long, java.util.Map<String, Object>>> itCariPa = summaryWorkspaceMap.entrySet().iterator();
				while (itCariPa.hasNext()) {
					java.util.Map.Entry<Long, java.util.Map<String, Object>> entry = itCariPa.next();
					if (!Boolean.TRUE.equals(entry.getValue().get("hasRealisasi"))) {
						itCariPa.remove();
					}
				}
			} else {
				for (java.util.Map<String, Object> summary : summaryWorkspaceMap.values()) {
					if (Boolean.TRUE.equals(summary.get("hasRealisasi"))) continue;
					java.util.Map<String, Object> mapMutasi = new java.util.HashMap<String, Object>();
					mapMutasi.put("paId", null);
					mapMutasi.put("wId", summary.get("wId"));
					mapMutasi.put("anggaranFull", summary.get("anggaranFull"));
					mapMutasi.put("realisasiFull", "-");
					mapMutasi.put("anggaranNilai", summary.get("anggaranNilai"));
					mapMutasi.put("realisasiNilai", 0.0);
					mapMutasi.put("tgl", null);
					mapMutasi.put("ket", "Belum ada realisasi aktif");
					mapMutasi.put("hasRealisasi", false);
					mapMutasi.put("sumberRealisasi", "-");
					mapMutasi.put("rawAktifDb", null);
					mutasiList.add(mapMutasi);
				}
			}



		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		for (java.util.Map<String, Object> summary : summaryWorkspaceMap.values()) {
			double anggaran = summary.get("anggaranNilai") == null ? 0.0 : ((Double) summary.get("anggaranNilai")).doubleValue();
			double realisasi = summary.get("realisasiNilai") == null ? 0.0 : ((Double) summary.get("realisasiNilai")).doubleValue();
			double saldo = anggaran - realisasi;
			double persen = anggaran > 0 ? (realisasi / anggaran) * 100.0 : 0.0;
			boolean hasRealisasi = Boolean.TRUE.equals(summary.get("hasRealisasi"));
			summary.put("saldoNilai", saldo);
			summary.put("lebihBudgetNilai", saldo < 0 ? Math.abs(saldo) : 0.0);
			summary.put("persenSerapan", persen);
			summary.put("status", saldo < 0 ? "Over Budget" : (hasRealisasi ? "Ada Realisasi" : "Belum Realisasi"));
		}

		// Filter khusus over budget diterapkan setelah akumulasi realisasi per workspace,
		// karena status over budget baru valid setelah total realisasi dibandingkan dengan anggaran workspace.
		if (onlyOverBudget) {
			java.util.Set<Long> overBudgetWorkspaceIds = new java.util.LinkedHashSet<Long>();
			java.util.Iterator<java.util.Map.Entry<Long, java.util.Map<String, Object>>> itSummary = summaryWorkspaceMap.entrySet().iterator();
			while (itSummary.hasNext()) {
				java.util.Map.Entry<Long, java.util.Map<String, Object>> entry = itSummary.next();
				java.util.Map<String, Object> summary = entry.getValue();
				double saldo = summary.get("saldoNilai") == null ? 0.0 : ((Double) summary.get("saldoNilai")).doubleValue();
				if (saldo < 0) {
					overBudgetWorkspaceIds.add(entry.getKey());
				} else {
					itSummary.remove();
				}
			}

			java.util.Iterator<java.util.Map<String, Object>> itMutasi = mutasiList.iterator();
			while (itMutasi.hasNext()) {
				java.util.Map<String, Object> m = itMutasi.next();
				Long wId = (Long) m.get("wId");
				if (!overBudgetWorkspaceIds.contains(wId)) {
					itMutasi.remove();
				}
			}
		}

		// Rehitung semua angka dashboard dari data final yang akan ditampilkan.
		totalAnggaran = 0.0;
		totalRealisasi = 0.0;
		totalTransaksi = 0;
		jumlahWorkspaceAdaRealisasi = 0;
		jumlahWorkspaceBelumRealisasi = 0;
		jumlahWorkspaceOverBudget = 0;
		jumlahWorkspaceSerapanRendah = 0;
		jumlahWorkspaceSerapanSedang = 0;
		jumlahWorkspaceSerapanTinggi = 0;
		jumlahPenggunaanAnggaranObjectAktif = 0;
		jumlahPenggunaanAnggaranAktifObjectDbBelumTrue = 0;
		totalRealisasiObjectAktif = 0.0;
		summarySumberRealisasiMap.clear();
		trendPerBulan.clear();

		for (java.util.Map<String, Object> summary : summaryWorkspaceMap.values()) {
			double anggaran = summary.get("anggaranNilai") == null ? 0.0 : ((Double) summary.get("anggaranNilai")).doubleValue();
			double realisasi = summary.get("realisasiNilai") == null ? 0.0 : ((Double) summary.get("realisasiNilai")).doubleValue();
			double saldo = summary.get("saldoNilai") == null ? 0.0 : ((Double) summary.get("saldoNilai")).doubleValue();
			double persen = summary.get("persenSerapan") == null ? 0.0 : ((Double) summary.get("persenSerapan")).doubleValue();
			boolean hasRealisasi = Boolean.TRUE.equals(summary.get("hasRealisasi"));

			totalAnggaran += anggaran;
			totalRealisasi += realisasi;
			totalTransaksi += summary.get("jumlahTransaksi") == null ? 0 : ((Integer) summary.get("jumlahTransaksi")).intValue();

			if (hasRealisasi) jumlahWorkspaceAdaRealisasi++;
			else jumlahWorkspaceBelumRealisasi++;

			if (saldo < 0) {
				jumlahWorkspaceOverBudget++;
			} else if (hasRealisasi && persen < 50.0) {
				jumlahWorkspaceSerapanRendah++;
			} else if (hasRealisasi && persen < 80.0) {
				jumlahWorkspaceSerapanSedang++;
			} else if (hasRealisasi) {
				jumlahWorkspaceSerapanTinggi++;
			}
		}

		for (java.util.Map<String, Object> m : mutasiList) {
			Date tgl = (Date) m.get("tgl");
			double realNilai = m.get("realisasiNilai") == null ? 0.0 : ((Double) m.get("realisasiNilai")).doubleValue();
			boolean hasRealisasi = Boolean.TRUE.equals(m.get("hasRealisasi"));
			if (hasRealisasi) {
				jumlahPenggunaanAnggaranObjectAktif++;
				totalRealisasiObjectAktif += realNilai;

				Boolean rawAktifDb = (Boolean) m.get("rawAktifDb");
				if (!Boolean.TRUE.equals(rawAktifDb)) {
					jumlahPenggunaanAnggaranAktifObjectDbBelumTrue++;
				}

				String sumber = m.get("sumberRealisasi") == null ? "Manual / Lainnya" : (String) m.get("sumberRealisasi");
				java.util.Map<String, Object> summarySumber = summarySumberRealisasiMap.get(sumber);
				if (summarySumber == null) {
					summarySumber = new java.util.HashMap<String, Object>();
					summarySumber.put("sumber", sumber);
					summarySumber.put("jumlahTransaksi", 0);
					summarySumber.put("totalNilai", 0.0);
					summarySumberRealisasiMap.put(sumber, summarySumber);
				}
				summarySumber.put("jumlahTransaksi", ((Integer) summarySumber.get("jumlahTransaksi")) + 1);
				summarySumber.put("totalNilai", ((Double) summarySumber.get("totalNilai")) + realNilai);
			}
			if (hasRealisasi && tgl != null && realNilai > 0) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(tgl);
				Integer bulan = cal.get(Calendar.MONTH);
				Double nilaiBulan = trendPerBulan.get(bulan);
				trendPerBulan.put(bulan, (nilaiBulan == null ? 0.0 : nilaiBulan.doubleValue()) + realNilai);
			}
		}

		for (int i = 0; i < 12; i++) {
			Double nilaiBulan = trendPerBulan.get(Integer.valueOf(i));
			if (nilaiBulan != null && nilaiBulan.doubleValue() > 0) {
				String bulanLabel = (Common.BULAN != null && Common.BULAN.length > i) ? Common.BULAN[i] : String.valueOf(i + 1);
				trendModel.setValue("Realisasi", bulanLabel, nilaiBulan);
			}
		}

		final double sisaAnggaranAll = totalAnggaran - totalRealisasi;
		final int jumlahWorkspace = summaryWorkspaceMap.size();
		final double persenSerapanAll = totalAnggaran > 0 ? (totalRealisasi / totalAnggaran) * 100.0 : 0.0;
		final double rataRealisasiPerWorkspace = jumlahWorkspace > 0 ? totalRealisasi / jumlahWorkspace : 0.0;

		final java.util.List<java.util.Map<String, Object>> summaryByRealisasi = new java.util.ArrayList<java.util.Map<String, Object>>(summaryWorkspaceMap.values());
		java.util.Collections.sort(summaryByRealisasi, new java.util.Comparator<java.util.Map<String, Object>>() {
			@Override
			public int compare(java.util.Map<String, Object> o1, java.util.Map<String, Object> o2) {
				double v1 = o1.get("realisasiNilai") == null ? 0.0 : ((Double) o1.get("realisasiNilai")).doubleValue();
				double v2 = o2.get("realisasiNilai") == null ? 0.0 : ((Double) o2.get("realisasiNilai")).doubleValue();
				return v2 > v1 ? 1 : (v2 < v1 ? -1 : 0);
			}
		});

		final java.util.List<java.util.Map<String, Object>> summaryBySaldo = new java.util.ArrayList<java.util.Map<String, Object>>(summaryWorkspaceMap.values());
		java.util.Collections.sort(summaryBySaldo, new java.util.Comparator<java.util.Map<String, Object>>() {
			@Override
			public int compare(java.util.Map<String, Object> o1, java.util.Map<String, Object> o2) {
				double v1 = o1.get("saldoNilai") == null ? 0.0 : ((Double) o1.get("saldoNilai")).doubleValue();
				double v2 = o2.get("saldoNilai") == null ? 0.0 : ((Double) o2.get("saldoNilai")).doubleValue();
				return v2 > v1 ? 1 : (v2 < v1 ? -1 : 0);
			}
		});

		final java.util.List<java.util.Map<String, Object>> summaryBySerapan = new java.util.ArrayList<java.util.Map<String, Object>>(summaryWorkspaceMap.values());
		java.util.Collections.sort(summaryBySerapan, new java.util.Comparator<java.util.Map<String, Object>>() {
			@Override
			public int compare(java.util.Map<String, Object> o1, java.util.Map<String, Object> o2) {
				double v1 = o1.get("persenSerapan") == null ? 0.0 : ((Double) o1.get("persenSerapan")).doubleValue();
				double v2 = o2.get("persenSerapan") == null ? 0.0 : ((Double) o2.get("persenSerapan")).doubleValue();
				return v2 > v1 ? 1 : (v2 < v1 ? -1 : 0);
			}
		});

		// Tombol Download Excel: buku besar + summary workspace.
		MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Download Excel", "/img/excel.gif");
		btnExcel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (mutasiList.isEmpty() && summaryWorkspaceMap.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada data untuk didownload", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				StringBuilder sb = new StringBuilder();
				sb.append("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:x='urn:schemas-microsoft-com:office:excel' xmlns='http://www.w3.org/TR/REC-html40'>");
				sb.append("<head><meta charset='UTF-8'></head><body>");
				sb.append("<h3>Dashboard Realisasi Anggaran</h3>");
				sb.append("<div>Tahun: ").append(tahunAktif).append("</div>");
				sb.append("<div>Mode Data: ").append(onlyOverBudget ? "Over Budget saja" : "Semua data").append("</div>");
				sb.append("<br/>");

				sb.append("<table border='1'>");
				sb.append("<tr style='background-color:#004085; color:white; font-weight:bold;'>");
				sb.append("<th>Anggaran (Workspace)</th><th>Realisasi (Penggunaan)</th><th>Tanggal</th><th>Keterangan</th><th>Nilai Anggaran</th><th>Nilai Realisasi</th><th>Saldo per Anggaran</th><th>Saldo Keseluruhan</th>");
				sb.append("</tr>");

				double runBalanceAll = 0.0;
				java.util.Map<Long, Double> saldoPerWsExcel = new java.util.HashMap<Long, Double>();

				for (java.util.Map<String, Object> m : mutasiList) {
					Long wId = (Long) m.get("wId");
					Double anggNilai = (Double) m.get("anggaranNilai");
					Double realNilai = (Double) m.get("realisasiNilai");
					if (!saldoPerWsExcel.containsKey(wId)) {
						saldoPerWsExcel.put(wId, anggNilai);
						runBalanceAll += anggNilai;
					}
					double currentWsSaldo = saldoPerWsExcel.get(wId) - realNilai;
					saldoPerWsExcel.put(wId, currentWsSaldo);
					runBalanceAll -= realNilai;

					sb.append("<tr>");
					sb.append("<td>").append(m.get("anggaranFull")).append("</td>");
					sb.append("<td>").append(m.get("realisasiFull")).append("</td>");
					sb.append("<td>").append(m.get("sumberRealisasi") == null ? "-" : m.get("sumberRealisasi")).append("</td>");
					Boolean rawAktifDbExcel = (Boolean) m.get("rawAktifDb");
					sb.append("<td>").append(rawAktifDbExcel == null ? "NULL" : rawAktifDbExcel.toString()).append("</td>");
					Date tgl = (Date) m.get("tgl");
					sb.append("<td>").append(tgl != null ? Common.dateFormat3.get().format(tgl) : "-").append("</td>");
					sb.append("<td>").append(m.get("ket")).append("</td>");
					sb.append("<td align='right'>").append(anggNilai).append("</td>");
					sb.append("<td align='right'>").append(realNilai).append("</td>");
					sb.append("<td align='right'>").append(currentWsSaldo).append("</td>");
					sb.append("<td align='right'>").append(runBalanceAll).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");

				sb.append("<br/><h3>Summary Rincian Saldo per Anggaran (Sum Workspace)</h3>");
				sb.append("<table border='1'>");
				sb.append("<tr style='background-color:#155724; color:white; font-weight:bold;'>");
				sb.append("<th>Workspace</th><th>Nilai Anggaran</th><th>Total Realisasi</th><th>Saldo</th><th>% Serapan</th><th>Transaksi</th><th>Terakhir Realisasi</th><th>Status</th>");
				sb.append("</tr>");
				for (java.util.Map<String, Object> s : summaryWorkspaceMap.values()) {
					Date tgl = (Date) s.get("tanggalTerakhir");
					sb.append("<tr>");
					sb.append("<td>").append(s.get("anggaranFull")).append("</td>");
					sb.append("<td align='right'>").append(s.get("anggaranNilai")).append("</td>");
					sb.append("<td align='right'>").append(s.get("realisasiNilai")).append("</td>");
					sb.append("<td align='right'>").append(s.get("saldoNilai")).append("</td>");
					sb.append("<td align='right'>").append(Common.numberFormat.get().format(s.get("persenSerapan"))).append("%</td>");
					sb.append("<td align='right'>").append(s.get("jumlahTransaksi")).append("</td>");
					sb.append("<td>").append(tgl != null ? Common.dateFormat3.get().format(tgl) : "-").append("</td>");
					sb.append("<td>").append(s.get("status")).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");

				sb.append("<br/><h3>Daftar Anggaran Over Budget</h3>");
				sb.append("<table border='1'>");
				sb.append("<tr style='background-color:#842029; color:white; font-weight:bold;'>");
				sb.append("<th>Workspace</th><th>Nilai Anggaran</th><th>Total Realisasi</th><th>Lebih Budget</th><th>% Serapan</th><th>Transaksi</th><th>Terakhir Realisasi</th><th>Catatan</th>");
				sb.append("</tr>");
				int excelOverBudgetCount = 0;
				for (java.util.Map<String, Object> s : summaryBySerapan) {
					double saldo = s.get("saldoNilai") == null ? 0.0 : ((Double) s.get("saldoNilai")).doubleValue();
					if (saldo >= 0) continue;
					Date tgl = (Date) s.get("tanggalTerakhir");
					sb.append("<tr>");
					sb.append("<td>").append(s.get("anggaranFull")).append("</td>");
					sb.append("<td align='right'>").append(s.get("anggaranNilai")).append("</td>");
					sb.append("<td align='right'>").append(s.get("realisasiNilai")).append("</td>");
					sb.append("<td align='right'>").append(Math.abs(saldo)).append("</td>");
					sb.append("<td align='right'>").append(Common.numberFormat.get().format(s.get("persenSerapan"))).append("%</td>");
					sb.append("<td align='right'>").append(s.get("jumlahTransaksi")).append("</td>");
					sb.append("<td>").append(tgl != null ? Common.dateFormat3.get().format(tgl) : "-").append("</td>");
					sb.append("<td>Realisasi melebihi pagu anggaran</td>");
					sb.append("</tr>");
					excelOverBudgetCount++;
				}
				if (excelOverBudgetCount == 0) {
					sb.append("<tr><td colspan='8'>Tidak ada anggaran yang over budget pada filter ini.</td></tr>");
				}
				sb.append("</table></body></html>");

				org.zkoss.zul.Filedownload.save(sb.toString().getBytes("UTF-8"), "application/vnd.ms-excel", "Dashboard_Realisasi_Anggaran.xls");
			}
		});
		dasborToolbar.appendChild(btnExcel);

		// 3. Ringkasan Anggaran
		org.zkoss.zul.Panel pnlDashboard = new org.zkoss.zul.Panel();
		pnlDashboard.setTitle("Ringkasan Anggaran Tahun " + tahunAktif + " - " + satuanKerjaFilterLabel);
		pnlDashboard.setBorder("normal");
		pnlDashboard.setParent(pcLeft);

		org.zkoss.zul.Panelchildren pchDashboard = new org.zkoss.zul.Panelchildren();
		pchDashboard.setStyle("padding:12px; background:#ffffff;");
		pchDashboard.setParent(pnlDashboard);

		org.zkoss.zul.Div cardsBox = new org.zkoss.zul.Div();
		cardsBox.setWidth("100%");
		cardsBox.setStyle("display:flex; flex-wrap:wrap; gap:8px; align-items:stretch;");
		cardsBox.setParent(pchDashboard);

		String cardBase = "padding:12px; border-radius:10px; margin:4px; width:155px; min-height:76px; text-align:center; box-sizing:border-box; box-shadow:0 2px 8px rgba(0,0,0,.08);";

		org.zkoss.zul.Div cardIn = new org.zkoss.zul.Div();
		cardIn.setStyle(cardBase + "background-color:#eef4fa; border:1px solid #b8daff;");
		cardIn.setParent(cardsBox);
		cardIn.appendChild(new Label(ais.common.Common.getBahasaConfig("TOTAL ANGGARAN")));
		cardIn.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblTotAngg = new Label("Rp " + Common.numberFormat.get().format(totalAnggaran));
		lblTotAngg.setStyle("font-weight:bold; color:#004085;");
		cardIn.appendChild(lblTotAngg);

		org.zkoss.zul.Div cardOut = new org.zkoss.zul.Div();
		cardOut.setStyle(cardBase + "background-color:#faeeee; border:1px solid #f5c6cb;");
		cardOut.setParent(cardsBox);
		cardOut.appendChild(new Label(ais.common.Common.getBahasaConfig("TOTAL REALISASI")));
		cardOut.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblTotReal = new Label("Rp " + Common.numberFormat.get().format(totalRealisasi));
		lblTotReal.setStyle("font-weight:bold; color:#721c24;");
		cardOut.appendChild(lblTotReal);

		org.zkoss.zul.Div cardSaldo = new org.zkoss.zul.Div();
		cardSaldo.setStyle(cardBase + "background-color:#eefaf1; border:1px solid #c3e6cb;");
		cardSaldo.setParent(cardsBox);
		cardSaldo.appendChild(new Label(ais.common.Common.getBahasaConfig("SISA ANGGARAN")));
		cardSaldo.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblTotSisa = new Label("Rp " + Common.numberFormat.get().format(sisaAnggaranAll));
		lblTotSisa.setStyle("font-weight:bold; color:#155724;");
		cardSaldo.appendChild(lblTotSisa);

		org.zkoss.zul.Div cardSerap = new org.zkoss.zul.Div();
		cardSerap.setStyle(cardBase + "background-color:#fff8e6; border:1px solid #ffeeba;");
		cardSerap.setParent(cardsBox);
		cardSerap.appendChild(new Label("% SERAPAN"));
		cardSerap.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblSerap = new Label(Common.numberFormat.get().format(persenSerapanAll) + "%");
		lblSerap.setStyle("font-weight:bold; color:#856404;");
		cardSerap.appendChild(lblSerap);

		org.zkoss.zul.Div cardWs = new org.zkoss.zul.Div();
		cardWs.setStyle(cardBase + "background-color:#f4f0ff; border:1px solid #d6c8ff;");
		cardWs.setParent(cardsBox);
		cardWs.appendChild(new Label(ais.common.Common.getBahasaConfig("WORKSPACE")));
		cardWs.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblWs = new Label(Common.numberFormat.get().format(jumlahWorkspace));
		lblWs.setStyle("font-weight:bold; color:#4b2e83;");
		cardWs.appendChild(lblWs);

		org.zkoss.zul.Div cardBelum = new org.zkoss.zul.Div();
		cardBelum.setStyle(cardBase + "background-color:#f8f9fa; border:1px solid #dee2e6;");
		cardBelum.setParent(cardsBox);
		cardBelum.appendChild(new Label(ais.common.Common.getBahasaConfig("BELUM REALISASI")));
		cardBelum.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblBelum = new Label(Common.numberFormat.get().format(jumlahWorkspaceBelumRealisasi));
		lblBelum.setStyle("font-weight:bold; color:#495057;");
		cardBelum.appendChild(lblBelum);

		org.zkoss.zul.Div cardOver = new org.zkoss.zul.Div();
		cardOver.setStyle(cardBase + "background-color:#fff0f0; border:1px solid #f1aeb5;");
		cardOver.setParent(cardsBox);
		cardOver.appendChild(new Label(ais.common.Common.getBahasaConfig("OVER BUDGET")));
		cardOver.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblOver = new Label(Common.numberFormat.get().format(jumlahWorkspaceOverBudget));
		lblOver.setStyle("font-weight:bold; color:#842029;");
		cardOver.appendChild(lblOver);

		org.zkoss.zul.Div cardPaAktif = new org.zkoss.zul.Div();
		cardPaAktif.setStyle(cardBase + "background-color:#e8f7ff; border:1px solid #b6e3ff;");
		cardPaAktif.setParent(cardsBox);
		cardPaAktif.appendChild(new Label(ais.common.Common.getBahasaConfig("PA AKTIF OBJECT")));
		cardPaAktif.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblPaAktif = new Label(Common.numberFormat.get().format(jumlahPenggunaanAnggaranObjectAktif));
		lblPaAktif.setStyle("font-weight:bold; color:#075985;");
		cardPaAktif.appendChild(lblPaAktif);

		org.zkoss.zul.Div cardPaMismatch = new org.zkoss.zul.Div();
		cardPaMismatch.setStyle(cardBase + "background-color:#fff7ed; border:1px solid #fed7aa;");
		cardPaMismatch.setParent(cardsBox);
		cardPaMismatch.appendChild(new Label(ais.common.Common.getBahasaConfig("DB BELUM TRUE")));
		cardPaMismatch.appendChild(new org.zkoss.zul.Html("<br/>"));
		Label lblPaMismatch = new Label(Common.numberFormat.get().format(jumlahPenggunaanAnggaranAktifObjectDbBelumTrue));
		lblPaMismatch.setStyle("font-weight:bold; color:#9a3412;");
		cardPaMismatch.appendChild(lblPaMismatch);

		if (onlyOverBudget) {
			pchDashboard.appendChild(new org.zkoss.zul.Html("<div style='margin-top:10px; padding:8px 10px; background:#fff3cd; border:1px solid #ffeeba; border-radius:8px; color:#856404;'>Mode filter aktif: <b>Over Budget saja</b>. Semua tabel dan dashboard dihitung ulang dari anggaran yang melebihi pagu.</div>"));
		}

		// 4. Panel informasi filter aktif, supaya kolom kanan tidak kosong setelah semua grafik dipindah ke bawah.
		org.zkoss.zul.Panel pnlFilterInfo = new org.zkoss.zul.Panel();
		pnlFilterInfo.setTitle("Informasi Filter & Status Data");
		pnlFilterInfo.setBorder("normal");
		pnlFilterInfo.setParent(pcRight);

		org.zkoss.zul.Panelchildren pchFilterInfo = new org.zkoss.zul.Panelchildren();
		pchFilterInfo.setStyle("padding:12px; background:#ffffff;");
		pchFilterInfo.setParent(pnlFilterInfo);
		pchFilterInfo.appendChild(new org.zkoss.zul.Html("<div style='line-height:1.8;'>"
				+ "<div><b>Tahun:</b> " + tahunAktif + "</div>"
				+ "<div><b>Satuan Kerja:</b> " + satuanKerjaFilterLabel + "</div>"
				+ "<div><b>Mode Data:</b> " + (onlyOverBudget ? "Over Budget saja" : "Semua data") + "</div>"
				+ "<div><b>Jumlah Anggaran Over Budget:</b> " + Common.numberFormat.get().format(jumlahWorkspaceOverBudget) + "</div>"
				+ "<div><b>Total Transaksi Realisasi Aktif:</b> " + Common.numberFormat.get().format(totalTransaksi) + " transaksi</div>"
				+ "<div><b>Filter Realisasi:</b> object PenggunaanAnggaran.getAktif() == true</div>"
				+ "<div><b>Object Aktif tetapi DB aktif belum true:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranAktifObjectDbBelumTrue) + " transaksi</div>"
				+ "<div><b>DB aktif true tetapi object tidak aktif/dilewati:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranDbTrueObjectTidakAktif) + " transaksi</div>"
				+ "</div>"));

		// 5. Dashboard tambahan non-grafik: monitoring filter aktif object PenggunaanAnggaran.
		org.zkoss.zul.Panel pnlMonitoringAktif = new org.zkoss.zul.Panel();
		pnlMonitoringAktif.setTitle("Monitoring Data Aktif Penggunaan Anggaran");
		pnlMonitoringAktif.setBorder("normal");
		pnlMonitoringAktif.setParent(pcRight);

		org.zkoss.zul.Panelchildren pchMonitoringAktif = new org.zkoss.zul.Panelchildren();
		pchMonitoringAktif.setStyle("padding:12px; background:#ffffff;");
		pchMonitoringAktif.setParent(pnlMonitoringAktif);
		pchMonitoringAktif.appendChild(new org.zkoss.zul.Html("<div style='line-height:1.8;'>"
				+ "<div><b>Realisasi aktif yang dipakai:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranObjectAktif) + " transaksi</div>"
				+ "<div><b>Total nilai realisasi aktif:</b> Rp " + Common.numberFormat.get().format(totalRealisasiObjectAktif) + "</div>"
				+ "<div><b>Object aktif, DB aktif belum true:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranAktifObjectDbBelumTrue) + " transaksi</div>"
				+ "<div><b>DB aktif true, object tidak aktif:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranDbTrueObjectTidakAktif) + " transaksi</div>"
				+ "<div><b>Object tidak aktif dan dilewati:</b> " + Common.numberFormat.get().format(jumlahPenggunaanAnggaranObjectTidakAktif) + " transaksi</div>"
				+ "<div style='margin-top:8px; color:#6c757d;'>Catatan: angka realisasi hanya memakai object <b>PenggunaanAnggaran</b> yang <b>getAktif()</b>-nya true. Kolom DB aktif hanya menjadi indikator sinkronisasi.</div>"
				+ "</div>"));

		// 6. Dashboard grafik: sebaran status anggaran. Parent berada di area grafik paling bawah.
		org.zkoss.zul.Panel pnlStatusWs = new org.zkoss.zul.Panel();
		pnlStatusWs.setTitle("Sebaran Status Anggaran");
		pnlStatusWs.setBorder("normal");
		pnlStatusWs.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchStatusWs = new org.zkoss.zul.Panelchildren();
		pchStatusWs.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchStatusWs.setParent(pnlStatusWs);

		int jumlahWorkspaceRealisasiNormal = jumlahWorkspaceAdaRealisasi - jumlahWorkspaceOverBudget;
		if (jumlahWorkspaceRealisasiNormal < 0) jumlahWorkspaceRealisasiNormal = 0;
		org.zkoss.zul.SimplePieModel statusWsModel = new org.zkoss.zul.SimplePieModel();
		if (jumlahWorkspace == 0) {
			statusWsModel.setValue("Tidak Ada Data", 1);
		} else {
			statusWsModel.setValue("Ada Realisasi Normal", jumlahWorkspaceRealisasiNormal);
			statusWsModel.setValue("Belum Realisasi", jumlahWorkspaceBelumRealisasi);
			statusWsModel.setValue("Over Budget", jumlahWorkspaceOverBudget);
		}
		org.zkoss.zul.Chart chartStatusWs = new org.zkoss.zul.Chart();
		chartStatusWs.setType("pie");
		chartStatusWs.setHeight("230");
		chartStatusWs.setWidth(Common.isMobile() ? "100%" : "430");
		chartStatusWs.setModel(statusWsModel);
		chartStatusWs.setParent(pchStatusWs);
		pchStatusWs.appendChild(new org.zkoss.zul.Html("<div style='margin-top:6px; line-height:1.7; font-size:12px;'>"
				+ "Normal: <b>" + Common.numberFormat.get().format(jumlahWorkspaceRealisasiNormal) + "</b> &nbsp; "
				+ "Belum: <b>" + Common.numberFormat.get().format(jumlahWorkspaceBelumRealisasi) + "</b> &nbsp; "
				+ "Over: <b>" + Common.numberFormat.get().format(jumlahWorkspaceOverBudget) + "</b>"
				+ "</div>"));

		// 6. Buku Besar & Rincian Saldo per Anggaran
		org.zkoss.zul.Panel pnlGrid = new org.zkoss.zul.Panel();
		pnlGrid.setTitle("Buku Besar & Rincian Saldo per Anggaran");
		pnlGrid.setBorder("normal");
		pnlGrid.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchGrid = new org.zkoss.zul.Panelchildren();
		pchGrid.setStyle("padding:10px; background:#ffffff;");
		pchGrid.setParent(pnlGrid);

		MyGrid gridDasbor = new MyGrid();
		gridDasbor.setMold("paging");
		gridDasbor.setPageSize(50);
		gridDasbor.setSclass("dgrid fgrid");
		gridDasbor.setWidth("100%");
		gridDasbor.setParent(pchGrid);

		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		cols.setParent(gridDasbor);
		new ais.ui.util.MyColumnConfig("Anggaran (Workspace)").setParent(cols);
		new ais.ui.util.MyColumnConfig("Realisasi (Penggunaan)").setParent(cols);
		new ais.ui.util.MyColumnConfig("Sumber").setParent(cols);
		new ais.ui.util.MyColumnConfig("DB Aktif").setParent(cols);
		new ais.ui.util.MyColumnConfig("Tanggal").setParent(cols);
		new ais.ui.util.MyColumnConfig("Keterangan").setParent(cols);

		ais.ui.util.MyColumnConfig colAngg = new ais.ui.util.MyColumnConfig("Nilai Anggaran");
		colAngg.setAlign("right"); colAngg.setParent(cols);
		ais.ui.util.MyColumnConfig colReal = new ais.ui.util.MyColumnConfig("Nilai Realisasi");
		colReal.setAlign("right"); colReal.setParent(cols);
		ais.ui.util.MyColumnConfig colSalPer = new ais.ui.util.MyColumnConfig("Saldo per Anggaran");
		colSalPer.setAlign("right"); colSalPer.setParent(cols);
		ais.ui.util.MyColumnConfig colSalTot = new ais.ui.util.MyColumnConfig("Saldo Keseluruhan");
		colSalTot.setAlign("right"); colSalTot.setParent(cols);

		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(gridDasbor);

		double runningBalanceAll = 0.0;
		java.util.Map<Long, Double> saldoPerWs = new java.util.HashMap<Long, Double>();
		java.util.Set<Long> printedAnggaran = new java.util.HashSet<Long>();

		for (java.util.Map<String, Object> m : mutasiList) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);

			Long wId = (Long) m.get("wId");
			Double anggNilai = (Double) m.get("anggaranNilai");
			Double realNilai = (Double) m.get("realisasiNilai");
			boolean hasRealisasi = Boolean.TRUE.equals(m.get("hasRealisasi"));

			if (!printedAnggaran.contains(wId)) {
				Label lblAnggaran = new Label((String) m.get("anggaranFull"));
				lblAnggaran.setStyle("font-weight:bold;");
				r.appendChild(lblAnggaran);
				printedAnggaran.add(wId);
			} else {
				r.appendChild(new Label(""));
			}

			r.appendChild(new Label((String) m.get("realisasiFull")));
			r.appendChild(new Label(m.get("sumberRealisasi") == null ? "-" : (String) m.get("sumberRealisasi")));
			Boolean rawAktifDbRow = (Boolean) m.get("rawAktifDb");
			r.appendChild(new Label(rawAktifDbRow == null ? "NULL" : rawAktifDbRow.toString()));
			Date tgl = (Date) m.get("tgl");
			r.appendChild(new Label(tgl != null ? Common.dateFormat3.get().format(tgl) : "-"));
			r.appendChild(new Label((String) m.get("ket")));

			if (!saldoPerWs.containsKey(wId)) {
				saldoPerWs.put(wId, anggNilai);
				runningBalanceAll += anggNilai;
				Label lblAnggRow = new Label(Common.numberFormat.get().format(anggNilai));
				lblAnggRow.setStyle("text-align:right;");
				r.appendChild(lblAnggRow);
			} else {
				r.appendChild(new Label("-"));
			}

			if (hasRealisasi) {
				double currentWsSaldo = saldoPerWs.get(wId) - realNilai;
				saldoPerWs.put(wId, currentWsSaldo);
				runningBalanceAll -= realNilai;
				r.appendChild(new Label(Common.numberFormat.get().format(realNilai)));
			} else {
				r.appendChild(new Label("-"));
			}

			Label lblPersonal = new Label(Common.numberFormat.get().format(saldoPerWs.get(wId)));
			lblPersonal.setStyle("font-weight:bold; color:" + (saldoPerWs.get(wId) < 0 ? "#842029" : "#004085") + ";");
			r.appendChild(lblPersonal);

			Label lblSaldoAll = new Label(Common.numberFormat.get().format(runningBalanceAll));
			lblSaldoAll.setStyle("font-weight:bold;");
			r.appendChild(lblSaldoAll);
		}

		org.zkoss.zul.Foot foot = new org.zkoss.zul.Foot();
		foot.setParent(gridDasbor);
		org.zkoss.zul.Footer f1 = new org.zkoss.zul.Footer(); f1.setParent(foot);
		org.zkoss.zul.Footer f2 = new org.zkoss.zul.Footer(); f2.setParent(foot);
		org.zkoss.zul.Footer f3 = new org.zkoss.zul.Footer(); f3.setParent(foot);
		org.zkoss.zul.Footer f4 = new org.zkoss.zul.Footer(); f4.setParent(foot);
		org.zkoss.zul.Footer f5 = new org.zkoss.zul.Footer(); f5.setParent(foot);
		org.zkoss.zul.Footer fKet = new org.zkoss.zul.Footer();
		fKet.setAlign("right"); fKet.setParent(foot);
		Label lblTotalText = new Label(ais.common.Common.getBahasaConfig("TOTAL :"));
		lblTotalText.setStyle("font-weight:bold;");
		fKet.appendChild(lblTotalText);
		org.zkoss.zul.Footer fDeb = new org.zkoss.zul.Footer();
		fDeb.setAlign("right"); fDeb.setParent(foot);
		Label lblTotDeb = new Label(Common.numberFormat.get().format(totalAnggaran));
		lblTotDeb.setStyle("font-weight:bold; color:#004085;");
		fDeb.appendChild(lblTotDeb);
		org.zkoss.zul.Footer fKre = new org.zkoss.zul.Footer();
		fKre.setAlign("right"); fKre.setParent(foot);
		Label lblTotKre = new Label(Common.numberFormat.get().format(totalRealisasi));
		lblTotKre.setStyle("font-weight:bold; color:#721c24;");
		fKre.appendChild(lblTotKre);
		org.zkoss.zul.Footer fSalPer = new org.zkoss.zul.Footer();
		fSalPer.setAlign("right"); fSalPer.setParent(foot);
		fSalPer.appendChild(new Label("-"));
		org.zkoss.zul.Footer fSal = new org.zkoss.zul.Footer();
		fSal.setAlign("right"); fSal.setParent(foot);
		Label lblTotSal = new Label(Common.numberFormat.get().format(sisaAnggaranAll));
		lblTotSal.setStyle("font-weight:bold; color:" + (sisaAnggaranAll < 0 ? "#842029" : "#155724") + ";");
		fSal.appendChild(lblTotSal);

		// 7. Summary Rincian Saldo per Anggaran (Sum Workspace), tepat di bawah Buku Besar.
		org.zkoss.zul.Panel pnlSummary = new org.zkoss.zul.Panel();
		pnlSummary.setTitle("Summary Rincian Saldo per Anggaran (Sum Workspace)");
		pnlSummary.setBorder("normal");
		pnlSummary.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchSummary = new org.zkoss.zul.Panelchildren();
		pchSummary.setStyle("padding:10px; background:#ffffff;");
		pchSummary.setParent(pnlSummary);

		MyGrid gridSummary = new MyGrid();
		gridSummary.setMold("paging");
		gridSummary.setPageSize(25);
		gridSummary.setSclass("dgrid fgrid");
		gridSummary.setWidth("100%");
		gridSummary.setParent(pchSummary);

		org.zkoss.zul.Columns sumCols = new org.zkoss.zul.Columns();
		sumCols.setParent(gridSummary);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(sumCols);
		ais.ui.util.MyColumnConfig sumAngg = new ais.ui.util.MyColumnConfig("Anggaran"); sumAngg.setAlign("right"); sumAngg.setParent(sumCols);
		ais.ui.util.MyColumnConfig sumReal = new ais.ui.util.MyColumnConfig("Realisasi"); sumReal.setAlign("right"); sumReal.setParent(sumCols);
		ais.ui.util.MyColumnConfig sumSaldo = new ais.ui.util.MyColumnConfig("Saldo"); sumSaldo.setAlign("right"); sumSaldo.setParent(sumCols);
		ais.ui.util.MyColumnConfig sumPersen = new ais.ui.util.MyColumnConfig("% Serapan"); sumPersen.setAlign("right"); sumPersen.setParent(sumCols);
		ais.ui.util.MyColumnConfig sumTrx = new ais.ui.util.MyColumnConfig("Transaksi"); sumTrx.setAlign("right"); sumTrx.setParent(sumCols);
		new ais.ui.util.MyColumnConfig("Terakhir Realisasi").setParent(sumCols);
		new ais.ui.util.MyColumnConfig("Status").setParent(sumCols);

		org.zkoss.zul.Rows sumRows = new org.zkoss.zul.Rows();
		sumRows.setParent(gridSummary);

		for (java.util.Map<String, Object> s : summaryWorkspaceMap.values()) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(sumRows);
			Date terakhir = (Date) s.get("tanggalTerakhir");
			double saldo = ((Double) s.get("saldoNilai")).doubleValue();
			Label status = new Label(String.valueOf(s.get("status")));
			status.setStyle("font-weight:bold; color:" + (saldo < 0 ? "#842029" : (Boolean.TRUE.equals(s.get("hasRealisasi")) ? "#155724" : "#6c757d")) + ";");

			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("realisasiNilai"))));
			Label lblSaldo = new Label(Common.numberFormat.get().format(saldo));
			lblSaldo.setStyle("font-weight:bold; color:" + (saldo < 0 ? "#842029" : "#155724") + ";");
			row.appendChild(lblSaldo);
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("persenSerapan")) + "%"));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("jumlahTransaksi"))));
			row.appendChild(new Label(terakhir != null ? Common.dateFormat3.get().format(terakhir) : "-"));
			row.appendChild(status);
		}

		// 8. Daftar khusus anggaran yang over budget.
		org.zkoss.zul.Panel pnlOverBudgetList = new org.zkoss.zul.Panel();
		pnlOverBudgetList.setTitle("Daftar Anggaran Over Budget");
		pnlOverBudgetList.setBorder("normal");
		pnlOverBudgetList.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchOverBudgetList = new org.zkoss.zul.Panelchildren();
		pchOverBudgetList.setStyle("padding:10px; background:#ffffff;");
		pchOverBudgetList.setParent(pnlOverBudgetList);

		MyGrid gridOverBudget = new MyGrid();
		gridOverBudget.setMold("paging");
		gridOverBudget.setPageSize(20);
		gridOverBudget.setSclass("dgrid fgrid");
		gridOverBudget.setWidth("100%");
		gridOverBudget.setParent(pchOverBudgetList);

		org.zkoss.zul.Columns colsOverBudget = new org.zkoss.zul.Columns();
		colsOverBudget.setParent(gridOverBudget);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsOverBudget);
		ais.ui.util.MyColumnConfig colObAngg = new ais.ui.util.MyColumnConfig("Anggaran"); colObAngg.setAlign("right"); colObAngg.setParent(colsOverBudget);
		ais.ui.util.MyColumnConfig colObReal = new ais.ui.util.MyColumnConfig("Realisasi"); colObReal.setAlign("right"); colObReal.setParent(colsOverBudget);
		ais.ui.util.MyColumnConfig colObLebih = new ais.ui.util.MyColumnConfig("Lebih Budget"); colObLebih.setAlign("right"); colObLebih.setParent(colsOverBudget);
		ais.ui.util.MyColumnConfig colObPersen = new ais.ui.util.MyColumnConfig("% Serapan"); colObPersen.setAlign("right"); colObPersen.setParent(colsOverBudget);
		ais.ui.util.MyColumnConfig colObTrx = new ais.ui.util.MyColumnConfig("Transaksi"); colObTrx.setAlign("right"); colObTrx.setParent(colsOverBudget);
		new ais.ui.util.MyColumnConfig("Terakhir Realisasi").setParent(colsOverBudget);
		new ais.ui.util.MyColumnConfig("Catatan").setParent(colsOverBudget);

		org.zkoss.zul.Rows rowsOverBudget = new org.zkoss.zul.Rows();
		rowsOverBudget.setParent(gridOverBudget);

		int overBudgetRowCount = 0;
		for (java.util.Map<String, Object> s : summaryBySerapan) {
			double saldo = s.get("saldoNilai") == null ? 0.0 : ((Double) s.get("saldoNilai")).doubleValue();
			if (saldo >= 0) continue;
			Date terakhir = (Date) s.get("tanggalTerakhir");
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsOverBudget);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("realisasiNilai"))));
			Label lblLebih = new Label(Common.numberFormat.get().format(Math.abs(saldo)));
			lblLebih.setStyle("font-weight:bold; color:#842029;");
			row.appendChild(lblLebih);
			Label lblPersenOb = new Label(Common.numberFormat.get().format(s.get("persenSerapan")) + "%");
			lblPersenOb.setStyle("font-weight:bold; color:#842029;");
			row.appendChild(lblPersenOb);
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("jumlahTransaksi"))));
			row.appendChild(new Label(terakhir != null ? Common.dateFormat3.get().format(terakhir) : "-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Realisasi melebihi pagu anggaran")));
			overBudgetRowCount++;
		}
		if (overBudgetRowCount == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsOverBudget);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada anggaran yang over budget pada filter ini.")));
		}

		// 9. Dasbor tambahan: workspace perlu perhatian, setelah summary dan sebelum tren.
		org.zkoss.zul.Panel pnlPerhatian = new org.zkoss.zul.Panel();
		pnlPerhatian.setTitle("Anggaran Perlu Perhatian");
		pnlPerhatian.setBorder("normal");
		pnlPerhatian.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchPerhatian = new org.zkoss.zul.Panelchildren();
		pchPerhatian.setStyle("padding:10px; background:#ffffff;");
		pchPerhatian.setParent(pnlPerhatian);

		MyGrid gridPerhatian = new MyGrid();
		gridPerhatian.setMold("paging");
		gridPerhatian.setPageSize(10);
		gridPerhatian.setSclass("dgrid fgrid");
		gridPerhatian.setWidth("100%");
		gridPerhatian.setParent(pchPerhatian);

		org.zkoss.zul.Columns colsPerhatian = new org.zkoss.zul.Columns();
		colsPerhatian.setParent(gridPerhatian);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsPerhatian);
		ais.ui.util.MyColumnConfig colPerSaldo = new ais.ui.util.MyColumnConfig("Saldo"); colPerSaldo.setAlign("right"); colPerSaldo.setParent(colsPerhatian);
		ais.ui.util.MyColumnConfig colPerSerap = new ais.ui.util.MyColumnConfig("% Serapan"); colPerSerap.setAlign("right"); colPerSerap.setParent(colsPerhatian);
		new ais.ui.util.MyColumnConfig("Alasan").setParent(colsPerhatian);

		org.zkoss.zul.Rows rowsPerhatian = new org.zkoss.zul.Rows();
		rowsPerhatian.setParent(gridPerhatian);

		int perhatianCount = 0;
		for (java.util.Map<String, Object> s : summaryWorkspaceMap.values()) {
			double saldo = ((Double) s.get("saldoNilai")).doubleValue();
			double persen = ((Double) s.get("persenSerapan")).doubleValue();
			boolean hasRealisasi = Boolean.TRUE.equals(s.get("hasRealisasi"));
			String alasan = null;
			if (saldo < 0) alasan = "Realisasi melebihi anggaran";
			else if (!hasRealisasi) alasan = "Belum ada realisasi";
			else if (persen >= 90.0) alasan = "Serapan sudah tinggi";
			if (alasan == null) continue;

			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsPerhatian);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			Label lblSaldo = new Label(Common.numberFormat.get().format(saldo));
			lblSaldo.setStyle("font-weight:bold; color:" + (saldo < 0 ? "#842029" : "#155724") + ";");
			row.appendChild(lblSaldo);
			row.appendChild(new Label(Common.numberFormat.get().format(persen) + "%"));
			row.appendChild(new Label(alasan));
			perhatianCount++;
		}
		if (perhatianCount == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsPerhatian);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada anggaran yang perlu perhatian khusus.")));
		}

		// 10. Dasbor tambahan: rekap sumber realisasi aktif berdasarkan object PenggunaanAnggaran.
		org.zkoss.zul.Panel pnlSumberRealisasi = new org.zkoss.zul.Panel();
		pnlSumberRealisasi.setTitle("Rekap Sumber Realisasi Aktif");
		pnlSumberRealisasi.setBorder("normal");
		pnlSumberRealisasi.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchSumberRealisasi = new org.zkoss.zul.Panelchildren();
		pchSumberRealisasi.setStyle("padding:10px; background:#ffffff;");
		pchSumberRealisasi.setParent(pnlSumberRealisasi);

		MyGrid gridSumberRealisasi = new MyGrid();
		gridSumberRealisasi.setMold("paging");
		gridSumberRealisasi.setPageSize(10);
		gridSumberRealisasi.setSclass("dgrid fgrid");
		gridSumberRealisasi.setWidth("100%");
		gridSumberRealisasi.setParent(pchSumberRealisasi);

		org.zkoss.zul.Columns colsSumberRealisasi = new org.zkoss.zul.Columns();
		colsSumberRealisasi.setParent(gridSumberRealisasi);
		new ais.ui.util.MyColumnConfig("Sumber Realisasi").setParent(colsSumberRealisasi);
		ais.ui.util.MyColumnConfig colSumberTrx = new ais.ui.util.MyColumnConfig("Transaksi Aktif"); colSumberTrx.setAlign("right"); colSumberTrx.setParent(colsSumberRealisasi);
		ais.ui.util.MyColumnConfig colSumberNilai = new ais.ui.util.MyColumnConfig("Total Nilai"); colSumberNilai.setAlign("right"); colSumberNilai.setParent(colsSumberRealisasi);
		ais.ui.util.MyColumnConfig colSumberPersen = new ais.ui.util.MyColumnConfig("% dari Realisasi"); colSumberPersen.setAlign("right"); colSumberPersen.setParent(colsSumberRealisasi);

		org.zkoss.zul.Rows rowsSumberRealisasi = new org.zkoss.zul.Rows();
		rowsSumberRealisasi.setParent(gridSumberRealisasi);

		java.util.List<java.util.Map<String, Object>> summarySumberList = new java.util.ArrayList<java.util.Map<String, Object>>(summarySumberRealisasiMap.values());
		java.util.Collections.sort(summarySumberList, new java.util.Comparator<java.util.Map<String, Object>>() {
			@Override
			public int compare(java.util.Map<String, Object> o1, java.util.Map<String, Object> o2) {
				double v1 = o1.get("totalNilai") == null ? 0.0 : ((Double) o1.get("totalNilai")).doubleValue();
				double v2 = o2.get("totalNilai") == null ? 0.0 : ((Double) o2.get("totalNilai")).doubleValue();
				return v2 > v1 ? 1 : (v2 < v1 ? -1 : 0);
			}
		});
		if (summarySumberList.isEmpty()) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsSumberRealisasi);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada realisasi aktif.")));
		} else {
			for (java.util.Map<String, Object> sumber : summarySumberList) {
				double nilai = sumber.get("totalNilai") == null ? 0.0 : ((Double) sumber.get("totalNilai")).doubleValue();
				double persen = totalRealisasiObjectAktif > 0 ? (nilai / totalRealisasiObjectAktif) * 100.0 : 0.0;
				org.zkoss.zul.Row row = new org.zkoss.zul.Row();
				row.setParent(rowsSumberRealisasi);
				row.appendChild(new Label((String) sumber.get("sumber")));
				row.appendChild(new Label(Common.numberFormat.get().format(sumber.get("jumlahTransaksi"))));
				row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
				row.appendChild(new Label(Common.numberFormat.get().format(persen) + "%"));
			}
		}

		// 11. Dasbor tambahan: daftar anggaran yang belum memiliki realisasi aktif object.
		org.zkoss.zul.Panel pnlTanpaRealisasiAktif = new org.zkoss.zul.Panel();
		pnlTanpaRealisasiAktif.setTitle("Anggaran Tanpa Realisasi Aktif");
		pnlTanpaRealisasiAktif.setBorder("normal");
		pnlTanpaRealisasiAktif.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchTanpaRealisasiAktif = new org.zkoss.zul.Panelchildren();
		pchTanpaRealisasiAktif.setStyle("padding:10px; background:#ffffff;");
		pchTanpaRealisasiAktif.setParent(pnlTanpaRealisasiAktif);

		MyGrid gridTanpaRealisasiAktif = new MyGrid();
		gridTanpaRealisasiAktif.setMold("paging");
		gridTanpaRealisasiAktif.setPageSize(15);
		gridTanpaRealisasiAktif.setSclass("dgrid fgrid");
		gridTanpaRealisasiAktif.setWidth("100%");
		gridTanpaRealisasiAktif.setParent(pchTanpaRealisasiAktif);

		org.zkoss.zul.Columns colsTanpaRealisasiAktif = new org.zkoss.zul.Columns();
		colsTanpaRealisasiAktif.setParent(gridTanpaRealisasiAktif);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsTanpaRealisasiAktif);
		ais.ui.util.MyColumnConfig colTraAngg = new ais.ui.util.MyColumnConfig("Anggaran"); colTraAngg.setAlign("right"); colTraAngg.setParent(colsTanpaRealisasiAktif);
		ais.ui.util.MyColumnConfig colTraSaldo = new ais.ui.util.MyColumnConfig("Saldo"); colTraSaldo.setAlign("right"); colTraSaldo.setParent(colsTanpaRealisasiAktif);
		new ais.ui.util.MyColumnConfig("Catatan").setParent(colsTanpaRealisasiAktif);

		org.zkoss.zul.Rows rowsTanpaRealisasiAktif = new org.zkoss.zul.Rows();
		rowsTanpaRealisasiAktif.setParent(gridTanpaRealisasiAktif);

		int tanpaRealisasiAktifCount = 0;
		for (java.util.Map<String, Object> s : summaryWorkspaceMap.values()) {
			if (Boolean.TRUE.equals(s.get("hasRealisasi"))) continue;
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTanpaRealisasiAktif);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("saldoNilai"))));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada PenggunaanAnggaran aktif berdasarkan object getAktif().")));
			tanpaRealisasiAktifCount++;
		}
		if (tanpaRealisasiAktifCount == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTanpaRealisasiAktif);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Semua anggaran pada filter ini sudah memiliki realisasi aktif atau tersaring oleh pencarian.")));
		}


		// 12. Komposisi dan indikator kontrol berada di area dashboard grafik paling bawah.
		org.zkoss.zul.Panel pnlKomposisi = new org.zkoss.zul.Panel();
		pnlKomposisi.setTitle("Komposisi Realisasi vs Saldo");
		pnlKomposisi.setBorder("normal");
		pnlKomposisi.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchKomposisi = new org.zkoss.zul.Panelchildren();
		pchKomposisi.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchKomposisi.setParent(pnlKomposisi);

		org.zkoss.zul.SimplePieModel pieModel = new org.zkoss.zul.SimplePieModel();
		if (totalAnggaran <= 0 && totalRealisasi <= 0) {
			pieModel.setValue("Tidak Ada Data", 1);
		} else {
			pieModel.setValue("Realisasi", totalRealisasi);
			if (sisaAnggaranAll >= 0) {
				pieModel.setValue("Sisa Anggaran", sisaAnggaranAll);
			} else {
				pieModel.setValue("Melebihi Anggaran", Math.abs(sisaAnggaranAll));
			}
		}
		org.zkoss.zul.Chart chartKomposisi = new org.zkoss.zul.Chart();
		chartKomposisi.setType("pie");
		chartKomposisi.setHeight(Common.isMobile() ? "250" : "300");
		chartKomposisi.setWidth(Common.isMobile() ? "100%" : "720");
		chartKomposisi.setModel(pieModel);
		chartKomposisi.setParent(pchKomposisi);

		org.zkoss.zul.Panel pnlKontrol = new org.zkoss.zul.Panel();
		pnlKontrol.setTitle("Indikator Kontrol Anggaran");
		pnlKontrol.setBorder("normal");
		pnlKontrol.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchKontrol = new org.zkoss.zul.Panelchildren();
		pchKontrol.setStyle("padding:12px; background:#ffffff;");
		pchKontrol.setParent(pnlKontrol);
		String kontrolColor = sisaAnggaranAll < 0 ? "#842029" : (persenSerapanAll >= 90.0 ? "#856404" : "#155724");
		String kontrolStatus = sisaAnggaranAll < 0 ? "Melebihi total anggaran" : (persenSerapanAll >= 90.0 ? "Serapan tinggi, perlu monitoring" : "Masih dalam batas aman");
		pchKontrol.appendChild(new org.zkoss.zul.Html("<div style='display:flex; flex-wrap:wrap; gap:10px;'>"
				+ "<div style='flex:1; min-width:180px; padding:12px; border:1px solid #dee2e6; border-radius:8px;'>"
				+ "<div style='font-size:12px; color:#6c757d;'>Status Kontrol</div>"
				+ "<div style='font-size:18px; font-weight:bold; color:" + kontrolColor + ";'>" + kontrolStatus + "</div>"
				+ "</div>"
				+ "<div style='flex:1; min-width:180px; padding:12px; border:1px solid #dee2e6; border-radius:8px;'>"
				+ "<div style='font-size:12px; color:#6c757d;'>Workspace Sudah Realisasi</div>"
				+ "<div style='font-size:18px; font-weight:bold;'>" + Common.numberFormat.get().format(jumlahWorkspaceAdaRealisasi) + " / " + Common.numberFormat.get().format(jumlahWorkspace) + "</div>"
				+ "</div>"
				+ "<div style='flex:1; min-width:180px; padding:12px; border:1px solid #dee2e6; border-radius:8px;'>"
				+ "<div style='font-size:12px; color:#6c757d;'>Total Transaksi Realisasi</div>"
				+ "<div style='font-size:18px; font-weight:bold;'>" + Common.numberFormat.get().format(totalTransaksi) + " transaksi</div>"
				+ "</div>"
				+ "<div style='flex:1; min-width:180px; padding:12px; border:1px solid #dee2e6; border-radius:8px;'>"
				+ "<div style='font-size:12px; color:#6c757d;'>Rata-rata Realisasi / Workspace</div>"
				+ "<div style='font-size:18px; font-weight:bold;'>Rp " + Common.numberFormat.get().format(rataRealisasiPerWorkspace) + "</div>"
				+ "</div>"
				+ "</div>"));

		// 11. Dasbor tambahan: Top Realisasi Workspace.
		org.zkoss.zul.Panel pnlTopRealisasi = new org.zkoss.zul.Panel();
		pnlTopRealisasi.setTitle("Top 10 Realisasi Workspace Tertinggi");
		pnlTopRealisasi.setBorder("normal");
		pnlTopRealisasi.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchTopRealisasi = new org.zkoss.zul.Panelchildren();
		pchTopRealisasi.setStyle("padding:10px; background:#ffffff;");
		pchTopRealisasi.setParent(pnlTopRealisasi);

		MyGrid gridTopRealisasi = new MyGrid();
		gridTopRealisasi.setMold("paging");
		gridTopRealisasi.setPageSize(10);
		gridTopRealisasi.setSclass("dgrid fgrid");
		gridTopRealisasi.setWidth("100%");
		gridTopRealisasi.setParent(pchTopRealisasi);

		org.zkoss.zul.Columns colsTopReal = new org.zkoss.zul.Columns();
		colsTopReal.setParent(gridTopRealisasi);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsTopReal);
		ais.ui.util.MyColumnConfig topRealAngg = new ais.ui.util.MyColumnConfig("Anggaran"); topRealAngg.setAlign("right"); topRealAngg.setParent(colsTopReal);
		ais.ui.util.MyColumnConfig topRealNilai = new ais.ui.util.MyColumnConfig("Realisasi"); topRealNilai.setAlign("right"); topRealNilai.setParent(colsTopReal);
		ais.ui.util.MyColumnConfig topRealPersen = new ais.ui.util.MyColumnConfig("% Serapan"); topRealPersen.setAlign("right"); topRealPersen.setParent(colsTopReal);
		ais.ui.util.MyColumnConfig topRealTrx = new ais.ui.util.MyColumnConfig("Transaksi"); topRealTrx.setAlign("right"); topRealTrx.setParent(colsTopReal);
		new ais.ui.util.MyColumnConfig("Status").setParent(colsTopReal);

		org.zkoss.zul.Rows rowsTopReal = new org.zkoss.zul.Rows();
		rowsTopReal.setParent(gridTopRealisasi);
		int topRealNo = 0;
		for (java.util.Map<String, Object> s : summaryByRealisasi) {
			if (topRealNo >= 10) break;
			double realisasi = s.get("realisasiNilai") == null ? 0.0 : ((Double) s.get("realisasiNilai")).doubleValue();
			if (realisasi <= 0) continue;
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTopReal);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			Label lblReal = new Label(Common.numberFormat.get().format(realisasi));
			lblReal.setStyle("font-weight:bold; color:#721c24;");
			row.appendChild(lblReal);
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("persenSerapan")) + "%"));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("jumlahTransaksi"))));
			row.appendChild(new Label(String.valueOf(s.get("status"))));
			topRealNo++;
		}
		if (topRealNo == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTopReal);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum ada realisasi.")));
		}

		// 12. Dasbor tambahan: sisa saldo terbesar.
		org.zkoss.zul.Panel pnlTopSaldo = new org.zkoss.zul.Panel();
		pnlTopSaldo.setTitle("Top 10 Sisa Saldo Anggaran Terbesar");
		pnlTopSaldo.setBorder("normal");
		pnlTopSaldo.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchTopSaldo = new org.zkoss.zul.Panelchildren();
		pchTopSaldo.setStyle("padding:10px; background:#ffffff;");
		pchTopSaldo.setParent(pnlTopSaldo);

		MyGrid gridTopSaldo = new MyGrid();
		gridTopSaldo.setMold("paging");
		gridTopSaldo.setPageSize(10);
		gridTopSaldo.setSclass("dgrid fgrid");
		gridTopSaldo.setWidth("100%");
		gridTopSaldo.setParent(pchTopSaldo);

		org.zkoss.zul.Columns colsTopSaldo = new org.zkoss.zul.Columns();
		colsTopSaldo.setParent(gridTopSaldo);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsTopSaldo);
		ais.ui.util.MyColumnConfig topSaldoAngg = new ais.ui.util.MyColumnConfig("Anggaran"); topSaldoAngg.setAlign("right"); topSaldoAngg.setParent(colsTopSaldo);
		ais.ui.util.MyColumnConfig topSaldoReal = new ais.ui.util.MyColumnConfig("Realisasi"); topSaldoReal.setAlign("right"); topSaldoReal.setParent(colsTopSaldo);
		ais.ui.util.MyColumnConfig topSaldoNilai = new ais.ui.util.MyColumnConfig("Saldo"); topSaldoNilai.setAlign("right"); topSaldoNilai.setParent(colsTopSaldo);
		ais.ui.util.MyColumnConfig topSaldoPersen = new ais.ui.util.MyColumnConfig("% Serapan"); topSaldoPersen.setAlign("right"); topSaldoPersen.setParent(colsTopSaldo);
		new ais.ui.util.MyColumnConfig("Status").setParent(colsTopSaldo);

		org.zkoss.zul.Rows rowsTopSaldo = new org.zkoss.zul.Rows();
		rowsTopSaldo.setParent(gridTopSaldo);
		int topSaldoNo = 0;
		for (java.util.Map<String, Object> s : summaryBySaldo) {
			if (topSaldoNo >= 10) break;
			double saldo = s.get("saldoNilai") == null ? 0.0 : ((Double) s.get("saldoNilai")).doubleValue();
			if (saldo <= 0) continue;
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTopSaldo);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("realisasiNilai"))));
			Label lblSaldoTop = new Label(Common.numberFormat.get().format(saldo));
			lblSaldoTop.setStyle("font-weight:bold; color:#155724;");
			row.appendChild(lblSaldoTop);
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("persenSerapan")) + "%"));
			row.appendChild(new Label(String.valueOf(s.get("status"))));
			topSaldoNo++;
		}
		if (topSaldoNo == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsTopSaldo);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada saldo positif.")));
		}

		// 13. Dasbor tambahan: serapan tertinggi dan potensi over budget.
		org.zkoss.zul.Panel pnlSerapanTinggi = new org.zkoss.zul.Panel();
		pnlSerapanTinggi.setTitle("Serapan Tertinggi & Potensi Over Budget");
		pnlSerapanTinggi.setBorder("normal");
		pnlSerapanTinggi.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchSerapanTinggi = new org.zkoss.zul.Panelchildren();
		pchSerapanTinggi.setStyle("padding:10px; background:#ffffff;");
		pchSerapanTinggi.setParent(pnlSerapanTinggi);

		MyGrid gridSerapanTinggi = new MyGrid();
		gridSerapanTinggi.setMold("paging");
		gridSerapanTinggi.setPageSize(10);
		gridSerapanTinggi.setSclass("dgrid fgrid");
		gridSerapanTinggi.setWidth("100%");
		gridSerapanTinggi.setParent(pchSerapanTinggi);

		org.zkoss.zul.Columns colsSerapanTinggi = new org.zkoss.zul.Columns();
		colsSerapanTinggi.setParent(gridSerapanTinggi);
		new ais.ui.util.MyColumnConfig("Workspace").setParent(colsSerapanTinggi);
		ais.ui.util.MyColumnConfig colSerapAngg = new ais.ui.util.MyColumnConfig("Anggaran"); colSerapAngg.setAlign("right"); colSerapAngg.setParent(colsSerapanTinggi);
		ais.ui.util.MyColumnConfig colSerapReal = new ais.ui.util.MyColumnConfig("Realisasi"); colSerapReal.setAlign("right"); colSerapReal.setParent(colsSerapanTinggi);
		ais.ui.util.MyColumnConfig colSerapSaldo = new ais.ui.util.MyColumnConfig("Saldo"); colSerapSaldo.setAlign("right"); colSerapSaldo.setParent(colsSerapanTinggi);
		ais.ui.util.MyColumnConfig colSerapPersen = new ais.ui.util.MyColumnConfig("% Serapan"); colSerapPersen.setAlign("right"); colSerapPersen.setParent(colsSerapanTinggi);
		new ais.ui.util.MyColumnConfig("Catatan").setParent(colsSerapanTinggi);

		org.zkoss.zul.Rows rowsSerapanTinggi = new org.zkoss.zul.Rows();
		rowsSerapanTinggi.setParent(gridSerapanTinggi);
		int serapanNo = 0;
		for (java.util.Map<String, Object> s : summaryBySerapan) {
			if (serapanNo >= 10) break;
			double persen = s.get("persenSerapan") == null ? 0.0 : ((Double) s.get("persenSerapan")).doubleValue();
			double saldo = s.get("saldoNilai") == null ? 0.0 : ((Double) s.get("saldoNilai")).doubleValue();
			if (persen < 80.0 && saldo >= 0) continue;
			String catatan = saldo < 0 ? "Over budget" : (persen >= 95.0 ? "Hampir habis" : "Serapan tinggi");
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsSerapanTinggi);
			row.appendChild(new Label((String) s.get("anggaranFull")));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("anggaranNilai"))));
			row.appendChild(new Label(Common.numberFormat.get().format(s.get("realisasiNilai"))));
			Label lblSaldoSerap = new Label(Common.numberFormat.get().format(saldo));
			lblSaldoSerap.setStyle("font-weight:bold; color:" + (saldo < 0 ? "#842029" : "#155724") + ";");
			row.appendChild(lblSaldoSerap);
			Label lblPersenSerap = new Label(Common.numberFormat.get().format(persen) + "%");
			lblPersenSerap.setStyle("font-weight:bold; color:" + (saldo < 0 ? "#842029" : "#856404") + ";");
			row.appendChild(lblPersenSerap);
			row.appendChild(new Label(catatan));
			serapanNo++;
		}
		if (serapanNo == 0) {
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsSerapanTinggi);
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label("-"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada workspace dengan serapan tinggi.")));
		}

		// 14. Dasbor tambahan: rekap realisasi bulanan dalam bentuk tabel.
		org.zkoss.zul.Panel pnlBulanan = new org.zkoss.zul.Panel();
		pnlBulanan.setTitle("Rekap Realisasi Bulanan");
		pnlBulanan.setBorder("normal");
		pnlBulanan.setParent(pcBottom);

		org.zkoss.zul.Panelchildren pchBulanan = new org.zkoss.zul.Panelchildren();
		pchBulanan.setStyle("padding:10px; background:#ffffff;");
		pchBulanan.setParent(pnlBulanan);

		MyGrid gridBulanan = new MyGrid();
		gridBulanan.setMold("paging");
		gridBulanan.setPageSize(12);
		gridBulanan.setSclass("dgrid fgrid");
		gridBulanan.setWidth("100%");
		gridBulanan.setParent(pchBulanan);

		org.zkoss.zul.Columns colsBulanan = new org.zkoss.zul.Columns();
		colsBulanan.setParent(gridBulanan);
		new ais.ui.util.MyColumnConfig("Bulan").setParent(colsBulanan);
		ais.ui.util.MyColumnConfig colBulananNilai = new ais.ui.util.MyColumnConfig("Total Realisasi"); colBulananNilai.setAlign("right"); colBulananNilai.setParent(colsBulanan);
		ais.ui.util.MyColumnConfig colBulananKontribusi = new ais.ui.util.MyColumnConfig("Kontribusi ke Total Realisasi"); colBulananKontribusi.setAlign("right"); colBulananKontribusi.setParent(colsBulanan);

		org.zkoss.zul.Rows rowsBulanan = new org.zkoss.zul.Rows();
		rowsBulanan.setParent(gridBulanan);
		for (int i = 0; i < 12; i++) {
			Double nilaiBulan = trendPerBulan.get(Integer.valueOf(i));
			double nilai = nilaiBulan == null ? 0.0 : nilaiBulan.doubleValue();
			String bulanLabel = (Common.BULAN != null && Common.BULAN.length > i) ? Common.BULAN[i] : String.valueOf(i + 1);
			double kontribusi = totalRealisasi > 0 ? (nilai / totalRealisasi) * 100.0 : 0.0;
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setParent(rowsBulanan);
			row.appendChild(new Label(bulanLabel));
			row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
			row.appendChild(new Label(Common.numberFormat.get().format(kontribusi) + "%"));
		}

		// 15. Dashboard grafik tambahan: Top realisasi anggaran.
		org.zkoss.zul.SimpleCategoryModel topRealisasiChartModel = new org.zkoss.zul.SimpleCategoryModel();
		int chartTopRealNo = 0;
		for (java.util.Map<String, Object> s : summaryByRealisasi) {
			if (chartTopRealNo >= 10) break;
			double realisasi = s.get("realisasiNilai") == null ? 0.0 : ((Double) s.get("realisasiNilai")).doubleValue();
			if (realisasi <= 0) continue;
			String label = String.valueOf(s.get("anggaranFull"));
			if (label.length() > 35) label = label.substring(0, 35) + "...";
			topRealisasiChartModel.setValue("Realisasi", label, realisasi);
			chartTopRealNo++;
		}
		if (chartTopRealNo == 0) {
			topRealisasiChartModel.setValue("Realisasi", "Tidak Ada Data", 0);
		}

		org.zkoss.zul.Panel pnlGrafikTopRealisasi = new org.zkoss.zul.Panel();
		pnlGrafikTopRealisasi.setTitle("Grafik Top 10 Realisasi Anggaran");
		pnlGrafikTopRealisasi.setBorder("normal");
		pnlGrafikTopRealisasi.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikTopRealisasi = new org.zkoss.zul.Panelchildren();
		pchGrafikTopRealisasi.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikTopRealisasi.setParent(pnlGrafikTopRealisasi);

		org.zkoss.zul.Chart chartTopRealisasi = new org.zkoss.zul.Chart();
		chartTopRealisasi.setType("bar");
		chartTopRealisasi.setHeight(Common.isMobile() ? "320" : "420");
		chartTopRealisasi.setWidth(Common.isMobile() ? "100%" : "900");
		chartTopRealisasi.setModel(topRealisasiChartModel);
		chartTopRealisasi.setParent(pchGrafikTopRealisasi);

		// 16. Dashboard grafik tambahan: Top sisa saldo anggaran.
		org.zkoss.zul.SimpleCategoryModel topSaldoChartModel = new org.zkoss.zul.SimpleCategoryModel();
		int chartTopSaldoNo = 0;
		for (java.util.Map<String, Object> s : summaryBySaldo) {
			if (chartTopSaldoNo >= 10) break;
			double saldo = s.get("saldoNilai") == null ? 0.0 : ((Double) s.get("saldoNilai")).doubleValue();
			if (saldo <= 0) continue;
			String label = String.valueOf(s.get("anggaranFull"));
			if (label.length() > 35) label = label.substring(0, 35) + "...";
			topSaldoChartModel.setValue("Saldo", label, saldo);
			chartTopSaldoNo++;
		}
		if (chartTopSaldoNo == 0) {
			topSaldoChartModel.setValue("Saldo", "Tidak Ada Data", 0);
		}

		org.zkoss.zul.Panel pnlGrafikTopSaldo = new org.zkoss.zul.Panel();
		pnlGrafikTopSaldo.setTitle("Grafik Top 10 Sisa Saldo Anggaran");
		pnlGrafikTopSaldo.setBorder("normal");
		pnlGrafikTopSaldo.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikTopSaldo = new org.zkoss.zul.Panelchildren();
		pchGrafikTopSaldo.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikTopSaldo.setParent(pnlGrafikTopSaldo);

		org.zkoss.zul.Chart chartTopSaldo = new org.zkoss.zul.Chart();
		chartTopSaldo.setType("bar");
		chartTopSaldo.setHeight(Common.isMobile() ? "320" : "420");
		chartTopSaldo.setWidth(Common.isMobile() ? "100%" : "900");
		chartTopSaldo.setModel(topSaldoChartModel);
		chartTopSaldo.setParent(pchGrafikTopSaldo);

		// 17. Dashboard grafik tambahan: sebaran tingkat serapan anggaran.
		org.zkoss.zul.SimplePieModel serapanModel = new org.zkoss.zul.SimplePieModel();
		if (jumlahWorkspace == 0) {
			serapanModel.setValue("Tidak Ada Data", 1);
		} else {
			serapanModel.setValue("Belum Realisasi", jumlahWorkspaceBelumRealisasi);
			serapanModel.setValue("Serapan < 50%", jumlahWorkspaceSerapanRendah);
			serapanModel.setValue("Serapan 50-79%", jumlahWorkspaceSerapanSedang);
			serapanModel.setValue("Serapan >= 80%", jumlahWorkspaceSerapanTinggi);
			serapanModel.setValue("Over Budget", jumlahWorkspaceOverBudget);
		}

		org.zkoss.zul.Panel pnlGrafikSerapan = new org.zkoss.zul.Panel();
		pnlGrafikSerapan.setTitle("Grafik Sebaran Tingkat Serapan Anggaran");
		pnlGrafikSerapan.setBorder("normal");
		pnlGrafikSerapan.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikSerapan = new org.zkoss.zul.Panelchildren();
		pchGrafikSerapan.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikSerapan.setParent(pnlGrafikSerapan);

		org.zkoss.zul.Chart chartSerapan = new org.zkoss.zul.Chart();
		chartSerapan.setType("pie");
		chartSerapan.setHeight(Common.isMobile() ? "260" : "320");
		chartSerapan.setWidth(Common.isMobile() ? "100%" : "720");
		chartSerapan.setModel(serapanModel);
		chartSerapan.setParent(pchGrafikSerapan);

		// 17b. Dashboard grafik tambahan: rekap realisasi bulanan dalam bentuk grafik batang.
		org.zkoss.zul.SimpleCategoryModel bulananChartModel = new org.zkoss.zul.SimpleCategoryModel();
		boolean adaRealisasiBulanan = false;
		for (int i = 0; i < 12; i++) {
			Double nilaiBulan = trendPerBulan.get(Integer.valueOf(i));
			double nilai = nilaiBulan == null ? 0.0 : nilaiBulan.doubleValue();
			String bulanLabel = (Common.BULAN != null && Common.BULAN.length > i) ? Common.BULAN[i] : String.valueOf(i + 1);
			bulananChartModel.setValue("Realisasi", bulanLabel, nilai);
			if (nilai > 0) adaRealisasiBulanan = true;
		}
		if (!adaRealisasiBulanan && trendPerBulan.isEmpty()) {
			bulananChartModel.setValue("Realisasi", "Tidak Ada Data", 0);
		}

		org.zkoss.zul.Panel pnlGrafikBulanan = new org.zkoss.zul.Panel();
		pnlGrafikBulanan.setTitle("Grafik Rekap Realisasi Bulanan");
		pnlGrafikBulanan.setBorder("normal");
		pnlGrafikBulanan.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikBulanan = new org.zkoss.zul.Panelchildren();
		pchGrafikBulanan.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikBulanan.setParent(pnlGrafikBulanan);

		org.zkoss.zul.Chart chartBulanan = new org.zkoss.zul.Chart();
		chartBulanan.setType("bar");
		chartBulanan.setHeight(Common.isMobile() ? "280" : "340");
		chartBulanan.setWidth(Common.isMobile() ? "100%" : "900");
		chartBulanan.setModel(bulananChartModel);
		chartBulanan.setParent(pchGrafikBulanan);

		// Dashboard grafik tambahan: sumber realisasi aktif, tetap di area grafik paling bawah.
		org.zkoss.zul.Panel pnlGrafikSumberRealisasi = new org.zkoss.zul.Panel();
		pnlGrafikSumberRealisasi.setTitle("Grafik Sumber Realisasi Aktif");
		pnlGrafikSumberRealisasi.setBorder("normal");
		pnlGrafikSumberRealisasi.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikSumberRealisasi = new org.zkoss.zul.Panelchildren();
		pchGrafikSumberRealisasi.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikSumberRealisasi.setParent(pnlGrafikSumberRealisasi);
		org.zkoss.zul.SimpleCategoryModel modelSumberRealisasi = new org.zkoss.zul.SimpleCategoryModel();
		if (summarySumberList.isEmpty()) {
			modelSumberRealisasi.setValue("Nilai", "Tidak Ada Data", 0.0);
		} else {
			for (java.util.Map<String, Object> sumber : summarySumberList) {
				double nilai = sumber.get("totalNilai") == null ? 0.0 : ((Double) sumber.get("totalNilai")).doubleValue();
				modelSumberRealisasi.setValue("Nilai", (String) sumber.get("sumber"), nilai);
			}
		}
		org.zkoss.zul.Chart chartSumberRealisasi = new org.zkoss.zul.Chart();
		chartSumberRealisasi.setType("bar");
		chartSumberRealisasi.setHeight(Common.isMobile() ? "260" : "320");
		chartSumberRealisasi.setWidth(Common.isMobile() ? "100%" : "760");
		chartSumberRealisasi.setModel(modelSumberRealisasi);
		chartSumberRealisasi.setParent(pchGrafikSumberRealisasi);

		// Dashboard grafik tambahan: monitoring sinkronisasi aktif object vs kolom DB aktif.
		org.zkoss.zul.Panel pnlGrafikSinkronAktif = new org.zkoss.zul.Panel();
		pnlGrafikSinkronAktif.setTitle("Grafik Monitoring Sinkronisasi Aktif Penggunaan Anggaran");
		pnlGrafikSinkronAktif.setBorder("normal");
		pnlGrafikSinkronAktif.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikSinkronAktif = new org.zkoss.zul.Panelchildren();
		pchGrafikSinkronAktif.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikSinkronAktif.setParent(pnlGrafikSinkronAktif);
		org.zkoss.zul.SimplePieModel modelSinkronAktif = new org.zkoss.zul.SimplePieModel();
		int jumlahAktifSinkronDb = jumlahPenggunaanAnggaranObjectAktif - jumlahPenggunaanAnggaranAktifObjectDbBelumTrue;
		if (jumlahAktifSinkronDb < 0) jumlahAktifSinkronDb = 0;
		if (jumlahPenggunaanAnggaranObjectAktif == 0 && jumlahPenggunaanAnggaranDbTrueObjectTidakAktif == 0) {
			modelSinkronAktif.setValue("Tidak Ada Data", 1);
		} else {
			modelSinkronAktif.setValue("Object aktif & DB true", jumlahAktifSinkronDb);
			modelSinkronAktif.setValue("Object aktif, DB belum true", jumlahPenggunaanAnggaranAktifObjectDbBelumTrue);
			modelSinkronAktif.setValue("DB true, object tidak aktif", jumlahPenggunaanAnggaranDbTrueObjectTidakAktif);
		}
		org.zkoss.zul.Chart chartSinkronAktif = new org.zkoss.zul.Chart();
		chartSinkronAktif.setType("pie");
		chartSinkronAktif.setHeight(Common.isMobile() ? "250" : "300");
		chartSinkronAktif.setWidth(Common.isMobile() ? "100%" : "720");
		chartSinkronAktif.setModel(modelSinkronAktif);
		chartSinkronAktif.setParent(pchGrafikSinkronAktif);

		// Dashboard grafik tambahan: top over budget supaya daftar over budget juga terlihat secara visual.
		org.zkoss.zul.Panel pnlGrafikTopOverBudget = new org.zkoss.zul.Panel();
		pnlGrafikTopOverBudget.setTitle("Grafik Top 10 Anggaran Over Budget");
		pnlGrafikTopOverBudget.setBorder("normal");
		pnlGrafikTopOverBudget.setParent(pcChartsBottom);

		org.zkoss.zul.Panelchildren pchGrafikTopOverBudget = new org.zkoss.zul.Panelchildren();
		pchGrafikTopOverBudget.setStyle("padding:12px; text-align:center; background:#ffffff;");
		pchGrafikTopOverBudget.setParent(pnlGrafikTopOverBudget);
		org.zkoss.zul.SimpleCategoryModel modelTopOverBudget = new org.zkoss.zul.SimpleCategoryModel();
		int chartOverBudgetCount = 0;
		for (java.util.Map<String, Object> s : summaryBySerapan) {
			if (chartOverBudgetCount >= 10) break;
			double lebihBudget = s.get("lebihBudgetNilai") == null ? 0.0 : ((Double) s.get("lebihBudgetNilai")).doubleValue();
			if (lebihBudget <= 0) continue;
			String label = (String) s.get("anggaranFull");
			if (label != null && label.length() > 35) label = label.substring(0, 35) + "...";
			modelTopOverBudget.setValue("Lebih Budget", label, lebihBudget);
			chartOverBudgetCount++;
		}
		if (chartOverBudgetCount == 0) {
			modelTopOverBudget.setValue("Lebih Budget", "Tidak Ada Over Budget", 0.0);
		}
		org.zkoss.zul.Chart chartTopOverBudget = new org.zkoss.zul.Chart();
		chartTopOverBudget.setType("bar");
		chartTopOverBudget.setHeight(Common.isMobile() ? "260" : "320");
		chartTopOverBudget.setWidth(Common.isMobile() ? "100%" : "760");
		chartTopOverBudget.setModel(modelTopOverBudget);
		chartTopOverBudget.setParent(pchGrafikTopOverBudget);

		// 18. Tren Realisasi Anggaran, sengaja diletakkan paling bawah.
		org.zkoss.zul.Panel pnlChart = new org.zkoss.zul.Panel();
		pnlChart.setTitle("Tren Realisasi Anggaran");
		pnlChart.setBorder("normal");
		pnlChart.setParent(pcTrendBottom);

		org.zkoss.zul.Panelchildren pchChart = new org.zkoss.zul.Panelchildren();
		pchChart.setStyle("text-align:center; padding:10px; background:#ffffff;");
		pchChart.setParent(pnlChart);

		if (trendPerBulan.isEmpty()) {
			trendModel.setValue("Realisasi", "Tidak Ada Data", 0);
		}

		org.zkoss.zul.Chart chartTrend = new org.zkoss.zul.Chart();
		chartTrend.setType("line");
		chartTrend.setHeight(Common.isMobile() ? "260" : "320");
		chartTrend.setWidth(Common.isMobile() ? "100%" : "900");
		chartTrend.setModel(trendModel);
		chartTrend.setParent(pchChart);
	}

	// OPTIMASI: Method khusus untuk memproses filter, menghilangkan duplikasi code
	// OPTIMASI & PERBAIKAN BUG TRANSACTION:
	// Method khusus untuk memproses filter combo
	private void prosesPerubahanCombo(Event arg0) throws Exception {
		sumberDana.setSelectedItem(null);
		SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
				: tahunWorkspace.getSelectedItem().getValue());

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			// WAJIB: Buka transaksi sebelum memanggil Common.insertCombo
			// karena di dalamnya terdapat perintah createCriteria()
			session.getTransaction().begin();

			Common.insertCombo(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
					Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							Restrictions.and(Restrictions.eq("tahun", thn),
									Restrictions.or(Restrictions.isNull("satuanKerja"),
											Restrictions.eq("satuanKerja", mySatuanKerja)))));

			if (mySatuanKerja != null) {
				if (sumberDana.getChildren().size() > 1) {
					sumberDana.setVisible(true);
					sumberDanaLabel.setVisible(true);
				} else if (sumberDana.getChildren().size() == 0) {
					sumberDana.setVisible(false);
					sumberDanaLabel.setVisible(false);

					// Buat Sumber Dana Baru (Langsung simpan di transaksi yang sama)
					SumberDana sumberDanaData = new SumberDana();
					sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
					sumberDanaData.setSatuanKerja(mySatuanKerja);
					sumberDanaData.setTahun(thn);
					session.save(sumberDanaData);

					Common.selectComboItem(true, sumberDana, sumberDanaData);
				} else if (sumberDana.getChildren().size() == 1) {
					sumberDana.setVisible(false);
					sumberDanaLabel.setVisible(false);
					sumberDana.setSelectedIndex(0);
				}
			}

			// Commit seluruh proses (baik read maupun insert)
			session.getTransaction().commit();

		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}

		// Lanjutkan ke reload tree
		onReloadTree(arg0);
	}

	private void initTree() throws Exception {
		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Integer bulanMulai = 1;
		try {
			bulanMulai = Integer.parseInt(Common.getKonfigurasi("rab_mulai_bulan", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiBulananAction.java:2037");
			// Ignore fallback to 1
		}

		List<String> bulans = new ArrayList<String>();
		for (int i = 1; i <= 12; i++) {
			if (bulanMulai > 12) {
				bulanMulai = 1;
			}
			bulans.add(WorkspaceRevisiBulananAction.BULANS.get(bulanMulai));
			bulanMulai++;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Anggaran");
		treecol.setWidth("22%");
		treecol.setParent(treecols);

		for (String bulan : bulans) {
			treecol = new Treecol(bulan);
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Anggaran");
		treecol.setParent(treecols);
		treecol = new Treecol("Realisasi");
		treecol.setParent(treecols);
		treecol = new Treecol("Sisa");
		treecol.setParent(treecols);
		treecol = new Treecol("%");
		treecol.setParent(treecols);
		treecol.setWidth("4%");
		treecol = new Treecol("");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onRefreshRealisasi(Event event) throws Exception {
		RealisasiBulananAction.onRefreshRealisasi(tahunWorkspace, satuanKerja, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Gunakan Timer bawaan ZK untuk memastikan refresh UI berjalan aman di siklus
				// event berikutnya
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null); // Refresh UI Tree setelah selesai
					}
				});
			}
		}, workspaceTreeModel);
	}

	public static void onRefreshRealisasi(Combobox tahunWorkspace, AmbilDataSatuanKerjaBanbox satuanKerja,
			final EventListener callbackListener, final WorkspaceTreeModel workspaceTreeModel) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		// 1. Ambil nilai komponen UI ke variabel final SEBELUM masuk Thread
		final SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		final Integer tahunFilter = tahunWorkspace.getSelectedItem() == null ? null
				: (Integer) tahunWorkspace.getSelectedItem().getValue();

		// 2. Dialog Konfirmasi
		MyMessageboxConfig.show(
				"Apakah Anda yakin ingin menghitung ulang seluruh realisasi anggaran? Proses ini akan dijalankan secara paralel di latar belakang.",
				"Konfirmasi Perhitungan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event ev) throws Exception {
						int response = Integer.parseInt(ev.getData().toString());
						if (response == MyMessageboxConfig.OK) {

							// 3. Persiapan Desktop ZKoss
							final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent()
									.getDesktop();

							// Listener callback kita simpan dulu
							final Label label = Common.displayLoadBar(callbackListener);

							/* 4. Jalankan tugas latar utama yang mengontrol Thread Pool. OPTIMASI
							 * FASE 5: server push dulu dinyalakan di atas tetapi TIDAK PERNAH
							 * dimatikan, sehingga browser terus polling (menahan thread Tomcat)
							 * selama tab terbuka walau proses sudah selesai. Tugas juga dijalankan
							 * pada thread MENTAH tanpa batas. jalankanDenganPush() menyalakan push
							 * ber-reference-count, memakai pool daemon berbatas milik
							 * AsyncTaskManager, lalu MELEPAS push di finally. */
							ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

								// Helper untuk merubah tulisan Label Load Bar dari dalam Thread
								private void updateProgress(final org.zkoss.zk.ui.Desktop desktop, final Label label,
										final int percent, final String message) {
									try {
										org.zkoss.zk.ui.Executions.schedule(desktop, new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												if (label != null) {
													label.setValue("Loading... " + percent + "% (" + message + ")");
												}
											}
										}, null);
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									Session fetchSession = null;
									java.util.List<Long> workspaceIds = new java.util.ArrayList<Long>();

									try {
										// Sesi 1: Khusus untuk menarik ID data
										fetchSession = HibernateUtil.currentNativeSession();
										updateProgress(desktop, label, 0, "Mengambil data rantai anggaran...");

										workspaceIds = fetchSession.createCriteria(Workspace.class)
												.setProjection(org.hibernate.criterion.Projections.property("id"))
												.add(Restrictions.or(Restrictions.eq("carryOver", true),
														Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true))))
												.add(Restrictions.eq("leaf", true))
												.add(Restrictions.eq("satuanKerja", mySatuanKerja))
												.add(tahunFilter == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("tahunWorkspace", tahunFilter))
												.list();

									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									} finally {
										if (fetchSession != null && fetchSession.isOpen()) {
											fetchSession.disconnect();
											fetchSession.close();
										}
									}

									final int totalData = workspaceIds.size();
									if (totalData == 0) {
										matikanLoadBar();
										return;
									}

									// 5. PARALLEL PROCESSING SETUP
									// Maksimal 100 thread yang berjalan bersamaan (sesuai permintaan)
									int threadPoolSize = Math.min(100, totalData);
									ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

									// AtomicInteger aman dipakai untuk multi-threading
									final AtomicInteger completedCount = new AtomicInteger(0);

									for (final Long wId : workspaceIds) {
										executor.submit(new Runnable() {
											@Override
											public void run() {
												Session taskSession = null;
												try {
													// WAJIB: Buka Session baru khusus untuk thread ini!
													taskSession = HibernateUtil.getSessionFactory().openSession();
													taskSession.getTransaction().begin();

													// Menggunakan ConstantValues.ambil sesuai permintaan
													Workspace workspace = (Workspace) ConstantValues
															.ambil(Workspace.class.getName(), wId, true);

													if (workspace != null) {
														// Eksekusi fungsi bisnis menggunakan taskSession
														WorkspaceTreeModel.ubahRealisasiParents(workspace,
																workspaceTreeModel, taskSession);
													}

													taskSession.getTransaction().commit();

												} catch (Exception e) {
													if (taskSession != null
															&& taskSession.getTransaction().isActive()) {
														taskSession.getTransaction().rollback();
													}
													ais.common.Common.tampilErrorJikaAdmin(e);
												} finally {
													// Selalu tutup sesi setelah selesai memproses 1 data
													if (taskSession != null && taskSession.isOpen()) {
														taskSession.close();
													}

													// Update Progress secara asinkronus ke UI (Cegah lagging: Update
													// per 5 item atau di item terakhir)
													int currentCount = completedCount.incrementAndGet();
													if (currentCount % 5 == 0 || currentCount == totalData) {
														int percent = (int) (((double) currentCount * 100.0)
																/ totalData);
														updateProgress(desktop, label, percent, "Memproses Paralel: "
																+ currentCount + " / " + totalData);
													}
												}
											}
										});
									}

									// Beritahu executor bahwa tidak ada tugas baru lagi
									executor.shutdown();

									try {
										// Tunggu sampai seluruh 100 thread paralel menyelesaikan semua antriannya
										// Max timeout diset 2 jam (sesuaikan dengan ekspektasi volume data)
										executor.awaitTermination(2, TimeUnit.HOURS);
									} catch (InterruptedException e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}

									// 6. Matikan Loading Bar setelah seluruh thread parallel selesai
									matikanLoadBar();
								}

								private void matikanLoadBar() {
									try {
										org.zkoss.zk.ui.Executions.schedule(desktop, new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												if (label != null) {
													label.setValue("");
												}
											}
										}, null);
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}

							});
						}
					}
				});
	}

	public void onReloadTree(Event event) throws Exception {
		if (tahunWorkspace.getSelectedItem() == null || satuanKerja.getAttribute("satuanKerja") == null
				|| this.sumberDana.getSelectedItem() == null) {
			return;
		}

		SatuanKerja mySatuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana mySumberDana = (SumberDana) this.sumberDana.getSelectedItem().getValue();

		Session session = null;
		try {
			// PENTING: pakai native session, BUKAN currentSession(). currentSession() bisa
			// mengembalikan session ZK yang dibungkus ThreadLocalSessionContext (transaction
			// protected) sehingga createCriteria melempar "createCriteria is not valid without
			// active transaction". Native session aman untuk query baca tanpa transaksi eksplisit.
			session = HibernateUtil.currentNativeSession();
			revisi = (Integer) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("satuanKerja", mySatuanKerja))
					.add(mySumberDana == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("sumberDana", mySumberDana))
					.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue()))
					.setProjection(Projections.max("revisi")).uniqueResult();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			// Native session WAJIB ditutup di finally (disconnect + close), lalu lepaskan
			// dari ThreadLocal. JANGAN menutup currentSession milik request secara manual.
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiBulananAction.java:2317");
					// abaikan
				}
			}
			HibernateUtil.closeSession();
		}

		revisi = revisi == null ? -1 : revisi;

		workspaceTreeModel = new WorkspaceTreeModel((Integer) tahunWorkspace.getSelectedItem().getValue(), revisi,
				mySatuanKerja, mySumberDana);

		if (workspaceTreeModel.getSatuanKerjas().size() == 1 && mySumberDana == null) {
			MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		tree.setModel(workspaceTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Workspace workspace = (Workspace) arg1;

				try {
					final Treerow treerow = treeitem.getTreerow() == null ? new Treerow() : treeitem.getTreerow();
					treerow.setParent(treeitem);
					Common.clear(treerow);

					if (workspace.getJenisWorkspace() != null) {
						JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();
						treerow.setStyle((jenisWorkspace.getWarna() != null
								? "background-color:" + jenisWorkspace.getWarna() + ";"
								: "")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";"
										: ""));
					}

					if (workspaceTreeModel.getChildCount(workspace) != 0) {
						hasSomeChilds(treerow, workspace);
					} else {
						noChildNotEnabled(treerow, workspace);
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					toolbar.setParent(arg0);
					Integer count = WorkspaceTreeModel.getJumlahJurnal(workspace);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
							"[" + Common.numberFormat.get().format(count) + "]", "/img/shopping_cart1.png");
					button.setStyle("font-size:xx-small;text-align: left;");
					button.setTooltiptext("Realisasi Menggunakan Jurnal Umum");
					button.setVisible(workspaceTreeModel.getChildCount(workspace) == 0);

					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							final MyWindow window = new MyWindow(
									"Realisasi Jurnal Umum untuk item perencanaan " + workspace, "none", true);
							page.getFirstRoot().appendChild(window);

							window.setHeight("90%");
							window.setWidth("100%");
							window.setClosable(false);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							// Hati-hati penggunaan 'session' Http dari ZK disini,
							// memastikan tidak tertukar dengan Hibernate Session
							org.zkoss.zk.ui.Session zkSession = org.zkoss.zk.ui.Sessions.getCurrent();
							zkSession.setAttribute("workspace", workspace);
							zkSession.setAttribute("acara", null);
							zkSession.setAttribute("workspaceTreeModel", workspaceTreeModel);
							zkSession.setAttribute("jenisJurnal", Transaksi.JURNAL_KAS_KELUAR);

							MyIframe include = new MyIframe(
									"/pages/master/rab/penggunaan_anggaran.zul?workspace=" + workspace.getId());
							center.appendChild(include);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setHeight("30px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setParent(toolbar);
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									final Timer timer = new Timer(1000);
									timer.setParent(page.getFirstRoot());
									timer.addEventListener("onTimer", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Session session = null;
											try {
												// HANYA BUKA 1 SESSION UNTUK SELURUH PROSES!
												session = HibernateUtil.currentNativeSession();
												WorkspaceTreeModel.ubahRealisasiParents(workspace, workspaceTreeModel,
														session);

											} catch (Exception e) {
												if (session != null && session.getTransaction().isActive()) {
													session.getTransaction().rollback();
												}
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {

												// Tutup koneksi di paling akhir
												if (session != null && session.isOpen()) {
													session.disconnect();
													session.close();
												}
												HibernateUtil.closeSession();
											}

											render(treeitem, workspace);
										}
									});
									timer.start();
									window.detach();
								}
							});
							window.onModal();
						}
					});
					button.setParent(toolbar);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
	}

	private void createCell(Treerow treerow, Double value, boolean isBold, Workspace workspace) {
		Treecell cell = new Treecell(Common.numberFormat.get().format(value == null ? 0.0 : value));
		if (workspace != null)
			cell.setAttribute("workspace", workspace);
		cell.setStyle("font-size:xx-small;text-align: right;" + (isBold ? "font-weight: bolder;" : ""));
		cell.setParent(treerow);
	}

	private void hasSomeChilds(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		// OPTIMASI: Menggunakan helper method untuk efisiensi kode UI rendering
		createCell(treerow, workspace.getBulan1(), true, workspace);
		createCell(treerow, workspace.getBulan2(), true, workspace);
		createCell(treerow, workspace.getBulan3(), true, workspace);
		createCell(treerow, workspace.getBulan4(), true, workspace);
		createCell(treerow, workspace.getBulan5(), true, workspace);
		createCell(treerow, workspace.getBulan6(), true, workspace);
		createCell(treerow, workspace.getBulan7(), true, workspace);
		createCell(treerow, workspace.getBulan8(), true, workspace);
		createCell(treerow, workspace.getBulan9(), true, workspace);
		createCell(treerow, workspace.getBulan10(), true, workspace);
		createCell(treerow, workspace.getBulan11(), true, workspace);
		createCell(treerow, workspace.getBulan12(), true, workspace);

		Double hargaTotal = workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal();
		Double totalRealisasi = workspace.getRealisasiProses() == null ? 0.0 : workspace.getRealisasiProses();

		createCell(treerow, hargaTotal, true, null);

		Treecell treecellNilai = new Treecell(Common.numberFormat.get().format(totalRealisasi));
		treecellNilai.setStyle("font-size:xx-small;color:blue;font-weight: bolder;text-align: right;");
		treecellNilai.setParent(treerow);

		// PERBAIKAN: Mencegah NullPointerException atau Error Divide By Zero
		Double persen = hargaTotal > 0 ? (totalRealisasi * 100.0) / hargaTotal : 0.0;
		Double sisa = hargaTotal - totalRealisasi;

		Treecell treecellSisa = new Treecell(Common.numberFormat.get().format(sisa));
		treecellSisa.setStyle("font-size:xx-small;color:red;font-weight: bolder;text-align: right;");
		treecellSisa.setParent(treerow);

		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + " %");
		treecellPersen.setStyle("font-size:xx-small;color:red;font-weight: normal;text-align: right;");
		treecellPersen.setParent(treerow);

		treecellMap.remove(workspace);
		treecellMap.put(workspace, new Treecell[] { treecellNilai, treecellPersen, treecellSisa });
	}

	private void noChildNotEnabled(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setAttribute("workspace", workspace);
		treecell.setParent(treerow);

		// OPTIMASI: Menggunakan helper method agar tidak berulang kali inisialisasi
		// Object
		createCell(treerow, workspace.getBulan1(), false, null);
		createCell(treerow, workspace.getBulan2(), false, null);
		createCell(treerow, workspace.getBulan3(), false, null);
		createCell(treerow, workspace.getBulan4(), false, null);
		createCell(treerow, workspace.getBulan5(), false, null);
		createCell(treerow, workspace.getBulan6(), false, null);
		createCell(treerow, workspace.getBulan7(), false, null);
		createCell(treerow, workspace.getBulan8(), false, null);
		createCell(treerow, workspace.getBulan9(), false, null);
		createCell(treerow, workspace.getBulan10(), false, null);
		createCell(treerow, workspace.getBulan11(), false, null);
		createCell(treerow, workspace.getBulan12(), false, null);

		Double hargaTotal = workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal();
		Double realisasi = workspace.getRealisasiProses() == null ? 0.0 : workspace.getRealisasiProses();

		createCell(treerow, hargaTotal, false, null);

		Treecell treecellNilai = new Treecell(Common.numberFormat.get().format(realisasi));
		treecellNilai.setStyle("font-size:xx-small;color:blue;font-weight: bolder;text-align: right;");
		treecellNilai.setParent(treerow);

		// PERBAIKAN: Mencegah NullPointerException atau Error Divide By Zero
		Double persen = hargaTotal > 0 ? (realisasi * 100.0) / hargaTotal : 0.0;
		Double sisa = hargaTotal - realisasi;

		Treecell treecellSisa = new Treecell(Common.numberFormat.get().format(sisa));
		treecellSisa.setStyle("font-size:xx-small;color:red;font-weight: bolder;text-align: right;");
		treecellSisa.setParent(treerow);

		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + " %");
		treecellPersen.setStyle("font-size:xx-small;color:red;font-weight: normal;text-align: right;");
		treecellPersen.setParent(treerow);
	}

	public void onCetak(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		LaporanRealisasi laporanPerencanaan = new LaporanRealisasi(
				(SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		laporanPerencanaan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporanPerencanaan);
		laporanPerencanaan.setHeight("95%");
		laporanPerencanaan.setWidth("90%");
		laporanPerencanaan.setClosable(true);
		laporanPerencanaan.onModal();
	}

	public void onSearchDefault(Event event) throws Exception {
		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}

		Integer thn = (Integer) tahunWorkspace.getSelectedItem().getValue();
		SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

		if (mySatuanKerja != null && thn != null) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				SumberDana sumberDanaData = (SumberDana) session.createCriteria(SumberDana.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("satuanKerja", mySatuanKerja)).add(Restrictions.eq("tahun", thn))
						.setMaxResults(1).uniqueResult();

				if (sumberDanaData == null) {
					sumberDanaData = new SumberDana();
					sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
					sumberDanaData.setSatuanKerja(mySatuanKerja);
					sumberDanaData.setTahun(thn);

					session.getTransaction().begin();
					session.save(sumberDanaData);
					session.getTransaction().commit();
				}
				Common.selectComboItem(true, sumberDana, sumberDanaData);
			} catch (Exception e) {
				if (session != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null && session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();
			}
		}
		onReloadTree(event);
	}
}