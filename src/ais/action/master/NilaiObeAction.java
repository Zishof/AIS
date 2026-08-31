package ais.action.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.NilaiHuruf;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelConfigAgakBesar;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Controller/action ZK untuk nilai obe. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String STYLE_CARD}, {@code String
 * STYLE_INFO}, {@code String STYLE_BADGE}, {@code String STYLE_NUMBER}, {@code String STYLE_LABEL}, {@code
 * String STYLE_GRID}, {@code String STYLE_TOOLBAR}, {@code AmbilDataMahasiswaBanbox mahasiswa};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initMahasiswaFilter()},
 * {@code initToolbarButtons()}, {@code initMk()}, {@code initRinci()}); pembacaan/pencarian ({@code
 * reloadRinci()}, {@code loadAllCplForCpmk()}, {@code loadProfilMap()}, {@code getScale4Local()}, {@code
 * onSearchDefault()}, {@code loadCpmkListForDashboard()}); validasi/perhitungan ({@code calculateObe()}, {@code
 * calculateStudentObeDashboard()}, {@code hitungHurufObeSingle()}, {@code hitungPersenPerKomponenObe()}, {@code
 * recalculateTotalPerFormatObe()}, {@code hitungKonversiObe()}); mutasi data ({@code
 * saveMhsCatatanForStudent()}, {@code buildClassAveragesHtml()}, {@code parseTugasJSONForStudent()}, {@code
 * parseIdsToSet()}); pelaporan/ekspor ({@code cetak()}, {@code onCetak()}); operasi domain lain ({@code
 * showPortofolioMk()}, {@code showCqiEditor()}, {@code buildPortofolioMkHtml()}, {@code parameter()}, {@code
 * createInfoRow()}, {@code buildFormatNilaiKeyMap()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class NilaiObeAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	private static final String STYLE_CARD = "background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;"
			+ "box-shadow:0 8px 22px rgba(15,23,42,.06);padding:12px;margin:8px 0;box-sizing:border-box;";
	private static final String STYLE_INFO = "padding:10px 12px;border-radius:12px;background:#f8fafc;border:1px solid #e2e8f0;"
			+ "color:#334155;font-size:12px;line-height:1.5;";
	private static final String STYLE_BADGE = "display:inline-block;padding:5px 10px;border-radius:999px;background:#eff6ff;"
			+ "color:#1d4ed8;border:1px solid #bfdbfe;font-weight:bold;font-size:11px;";
	private static final String STYLE_NUMBER = "font-weight:bold;color:#0f172a;text-align:right;";
	private static final String STYLE_LABEL = "font-weight:600;color:#475569;font-size:12px;";
	private static final String STYLE_GRID = "border:1px solid #e5e7eb;border-radius:14px;overflow:auto;background:#ffffff;"
			+ "box-shadow:0 6px 18px rgba(15,23,42,.05);";
	private static final String STYLE_TOOLBAR = "background:#f8fafc;border:1px solid #e5e7eb;border-radius:12px;"
			+ "padding:10px;min-height:46px;height:auto;overflow:visible;box-sizing:border-box;margin-bottom:8px;";

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Rows rowsUtama;

	private Perkuliahan perkuliahan = null;
	private Toolbar toolbar;
	private Tbmuser tbmuser;
	private JSONObject jsonArraykurikulumPunyaMatakuliah;
	private Row rowRinci;
	private List<FormatNilai> formatNilais = null;
	private Mahasiswa mhs = null;
	private boolean refresh = false;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();

		try {
			if (toolbar != null) {
				toolbar.setStyle(STYLE_TOOLBAR);
			}
			String perkuliahanParam = execution == null ? null : execution.getParameter("perkuliahan");
			if (!isBlank(perkuliahanParam)) {
				perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
						Long.valueOf(perkuliahanParam.trim()));
			}

			if (perkuliahan == null) {
				showWarning("Data perkuliahan tidak ditemukan. Silakan buka kembali dari menu perkuliahan.");
				return;
			}

			initMahasiswaFilter();
			initToolbarButtons();

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				mahasiswa.setAttribute("mahasiswa", tbmuser.getMahasiswa());
				mahasiswa.setAttribute("myValue", tbmuser.getMahasiswa());
				mahasiswa.setValue(safeText(tbmuser.getMahasiswa().getNim()) + "-" + safeText(tbmuser.getMahasiswa().getNama()));
				mahasiswa.setDisabled(true);
				onSearchDefault(null);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initMahasiswaFilter() throws Exception {
		List<Long> mahasiswas = new ArrayList<Long>();
		Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
				!Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim"),
				false);
		if (detailperkuliahans != null) {
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) (detailperkuliahanid == null ? null
						: GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString()));
				if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null
						&& Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
					mahasiswas.add(detailperkuliahan.getMahasiswa().getId());
				}
			}
		}
		if (mahasiswas.isEmpty()) {
			mahasiswas.add(Long.valueOf(-1L));
		}

		mahasiswa = new AmbilDataMahasiswaBanbox(mahasiswas);
		if (toolbar != null) {
			Label label = new Label(ais.common.Common.getBahasaConfig("Mahasiswa: "));
			label.setStyle(STYLE_LABEL);
			toolbar.appendChild(label);
			toolbar.appendChild(mahasiswa);
		}
		mahasiswa.setWidth("280px");
		mahasiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private void initToolbarButtons() {
		if (toolbar == null) {
			return;
		}
		MyToolbarbuttonConfig refreshBtn = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refreshBtn.setTooltiptext("Refresh");
		refreshBtn.setStyle("font-weight:bold;border-radius:999px;padding:6px 12px;background:#2563eb;color:#ffffff;border:0;");
		refreshBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		refreshBtn.setParent(toolbar);

		// Portofolio MK button (Loop 1 class-level view)
		MyToolbarbuttonConfig portofolioBtn = new MyToolbarbuttonConfig("Portofolio MK", "/img/svg/document.svg");
		portofolioBtn.setTooltiptext("Lihat Portofolio Mata Kuliah (Loop 1 OBE) — struktur CPMK, bobot, dan template CQI");
		portofolioBtn.setStyle("font-weight:bold;border-radius:999px;padding:6px 12px;background:#7c3aed;color:#ffffff;border:0;");
		portofolioBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showPortofolioMk();
			}
		});
		portofolioBtn.setParent(toolbar);
	}

	private void showPortofolioMk() throws InterruptedException {
		if (perkuliahan == null || perkuliahan.getKurikulumPunyaMatakuliah() == null) {
			ais.ui.util.MyMessageboxConfig.show("Kurikulum OBE pada perkuliahan ini belum tersedia.",
					"Info", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		try {
			final ais.ui.util.MyWindow win = new ais.ui.util.MyWindow();
			win.setTitle("Portofolio Mata Kuliah OBE — " + safeText(perkuliahan.getMatakuliah().getNama()));
			win.setWidth("900px");
			win.setHeight("92%");
			win.setClosable(true);
			win.setBorder("normal");
			org.zkoss.zk.ui.Page page = ais.ui.util.ZkCompat.getCurrentPage();
			if (page != null) page.getFirstRoot().appendChild(win);

			// Borderlayout: toolbar tetap di atas (North), isi portofolio bisa di-SCROLL (Center autoscroll)
			// agar konten yang panjang tidak terpotong saat popup muncul.
			org.zkoss.zul.Borderlayout bl = new org.zkoss.zul.Borderlayout();
			bl.setWidth("100%");
			bl.setHeight("100%");
			ais.ui.util.ZkCompat.setFlex(bl, true);
			bl.setParent(win);

			org.zkoss.zul.North north = new org.zkoss.zul.North();
			north.setBorder("none");
			north.setParent(bl);

			Toolbar tb = new Toolbar();
			tb.setStyle("padding:6px 10px;background:#f8fafc;border-bottom:1px solid #e2e8f0;");
			tb.setParent(north);

			// Array trick: capture content reference before it is created below
			final org.zkoss.zul.Html[] contentRef = new org.zkoss.zul.Html[1];

			MyToolbarbuttonConfig cqiBtn = new MyToolbarbuttonConfig("Edit CQI", "/img/svg/edit.svg");
			cqiBtn.setStyle("font-weight:bold;border-radius:6px;padding:5px 14px;background:#c2410c;color:#fff;border:0;");
			cqiBtn.setTooltiptext("Buka editor Continuous Quality Improvement (CQI) per CPMK untuk semester ini");
			cqiBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					showCqiEditor(win, contentRef[0]);
				}
			});
			cqiBtn.setParent(tb);

			org.zkoss.zul.Center center = new org.zkoss.zul.Center();
			center.setBorder("none");
			center.setAutoscroll(true);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(bl);

			org.zkoss.zul.Html content = new org.zkoss.zul.Html(buildPortofolioMkHtml());
			contentRef[0] = content;
			content.setParent(center);
			win.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void showCqiEditor(final ais.ui.util.MyWindow parentWin, final org.zkoss.zul.Html contentToRefresh) {
		try {
			if (perkuliahan == null) return;
			Matakuliah mk = perkuliahan.getMatakuliah();
			if (mk == null) return;

			// Load CPMK, CPL, PL lists
			final List<CapaianPembelajaranLulusan> cpmkList = new ArrayList<CapaianPembelajaranLulusan>();
			@SuppressWarnings("unchecked")
			final List<CapaianLulusan> cplList = new ArrayList<CapaianLulusan>();
			@SuppressWarnings("unchecked")
			final List<ProfilLulusan> plList = new ArrayList<ProfilLulusan>();
			Session sess = null;
			try {
				sess = HibernateUtil.openSession();
				Set<Long> cpmkIds = parseIdsToSet(mk.getCapaianPembelajaranLulusan());
				if (!cpmkIds.isEmpty()) {
					cpmkList.addAll((List<CapaianPembelajaranLulusan>) sess.createCriteria(CapaianPembelajaranLulusan.class)
							.add(Restrictions.in("id", cpmkIds)).addOrder(Order.asc("kode")).list());
				}
				Set<Long> cplIds = parseIdsToSet(mk.getCapaianLulusan());
				if (!cplIds.isEmpty()) {
					cplList.addAll((List<CapaianLulusan>) sess.createCriteria(CapaianLulusan.class)
							.add(Restrictions.in("id", cplIds)).addOrder(Order.asc("kode")).list());
				}
				Set<Long> plIds = parseIdsToSet(mk.getProfilLulusan());
				if (!plIds.isEmpty()) {
					plList.addAll((List<ProfilLulusan>) sess.createCriteria(ProfilLulusan.class)
							.add(Restrictions.in("id", plIds)).addOrder(Order.asc("kode")).list());
				}
			} finally {
				closeSessionQuietly(sess);
			}

			// Parse existing CQI JSON → map cpmkKode → entry + __meta__
			final java.util.Map<String, JSONObject> cqiMap = new java.util.LinkedHashMap<String, JSONObject>();
			JSONObject cqiMetaEntry = new JSONObject();
			JSONObject cplAnalisisObj = new JSONObject();
			JSONObject plAnalisisObj  = new JSONObject();
			if (!perkuliahan.getCqiData().isEmpty()) {
				try {
					JSONArray arr = new JSONArray(perkuliahan.getCqiData());
					for (int i = 0; i < arr.length(); i++) {
						JSONObject e = arr.optJSONObject(i);
						if (e == null) continue;
						String ck = e.optString("cpmk", "");
						if ("__meta__".equals(ck)) {
							cqiMetaEntry = e;
							try { cplAnalisisObj = new JSONObject(e.optString("cplAnalisis","{}")); } catch (Exception ign2) { ais.common.ErrorAuditUtil.record(ign2, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:295");}
							try { plAnalisisObj  = new JSONObject(e.optString("plAnalisis", "{}")); } catch (Exception ign2) { ais.common.ErrorAuditUtil.record(ign2, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:296");}
						} else if (!ck.isEmpty()) {
							cqiMap.put(ck, e);
						}
					}
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:301");}
			}
			final JSONObject metaEntry     = cqiMetaEntry;
			final JSONObject cplAnalObj    = cplAnalisisObj;
			final JSONObject plAnalObj     = plAnalisisObj;

			// Build editor window
			final ais.ui.util.MyWindow win = new ais.ui.util.MyWindow();
			win.setTitle("CQI Editor — " + safeText(mk.getNama()) + " | " + safeText(perkuliahan.getTahunAjaran()));
			win.setWidth("1100px");
			win.setHeight("92%");
			win.setClosable(true);
			win.setBorder("normal");
			org.zkoss.zk.ui.Page page = ZkCompat.getCurrentPage();
			if (page != null) page.getFirstRoot().appendChild(win);

			org.zkoss.zul.Borderlayout bl = new org.zkoss.zul.Borderlayout();
			ZkCompat.setFlex(bl, true);
			bl.setParent(win);

			org.zkoss.zul.Center center = new org.zkoss.zul.Center();
			center.setAutoscroll(true);
			center.setParent(bl);

			// ZK Center (LayoutRegion) HANYA boleh punya SATU anak. Sebelumnya infoHtml DAN grid
			// keduanya di-setParent(center) → "Only one child is allowed: <Center>". Bungkus keduanya
			// dalam satu Vbox sebagai satu-satunya anak Center (perilaku tampilan tetap sama).
			Vbox centerWrap = new Vbox();
			centerWrap.setWidth("100%");
			centerWrap.setVflex("1");
			centerWrap.setParent(center);

			// Instruction panel
			org.zkoss.zul.Html infoHtml = new org.zkoss.zul.Html(
				"<div style='font-family:system-ui,sans-serif;padding:10px 14px;background:#fff7ed;border-bottom:1px solid #fed7aa;" +
				"font-size:12px;color:#78350f;'>" +
				"<b>CQI (Continuous Quality Improvement) — Loop 1 per Semester</b><br>" +
				"Isi rencana perbaikan per CPMK, analisis per CPL, dan analisis per PL. " +
				"Status dosen: <code>Planned</code>/<code>In Progress</code>/<code>Completed</code>/<code>Ditunda</code>. " +
				"Status admin: <code>Pending</code>/<code>Disetujui</code>/<code>Revisi</code>." +
				"</div>");
			infoHtml.setParent(centerWrap);
			new org.zkoss.zul.Html("<div style='padding:7px 14px;background:#ede9fe;font-size:12px;"
				+ "font-weight:700;color:#5b21b6;border-bottom:1px solid #c4b5fd;'>"
				+ "&#9312; Analisis per CPMK</div>").setParent(centerWrap);

			// Grid with per-CPMK rows
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(centerWrap);

			org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
			cols.setParent(grid);
			ais.ui.util.MyColumnConfig c0 = new ais.ui.util.MyColumnConfig("CPMK");
			c0.setWidth("12%"); c0.setParent(cols);
			ais.ui.util.MyColumnConfig c1 = new ais.ui.util.MyColumnConfig("Masalah / Gap Ketercapaian");
			c1.setParent(cols);
			ais.ui.util.MyColumnConfig c2 = new ais.ui.util.MyColumnConfig("Analisis Penyebab");
			c2.setParent(cols);
			ais.ui.util.MyColumnConfig c3 = new ais.ui.util.MyColumnConfig("Rencana Tindak Lanjut");
			c3.setParent(cols);
			ais.ui.util.MyColumnConfig c4 = new ais.ui.util.MyColumnConfig("PJ");
			c4.setWidth("10%"); c4.setParent(cols);
			ais.ui.util.MyColumnConfig c5 = new ais.ui.util.MyColumnConfig("Target Waktu");
			c5.setWidth("12%"); c5.setParent(cols);
			ais.ui.util.MyColumnConfig c6 = new ais.ui.util.MyColumnConfig("Status");
			c6.setWidth("9%"); c6.setParent(cols);
			ais.ui.util.MyColumnConfig c7 = new ais.ui.util.MyColumnConfig("Evaluasi Admin");
			c7.setWidth("12%"); c7.setParent(cols);
			ais.ui.util.MyColumnConfig c8 = new ais.ui.util.MyColumnConfig("Status Admin");
			c8.setWidth("8%"); c8.setParent(cols);

			Rows rows = new Rows();
			rows.setParent(grid);

			// Per-CPMK form fields (stored for save handler)
			final List<String> cpmkKodes = new ArrayList<String>();
			final List<ais.ui.util.MyTextbox> masalahList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> analisisList = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> rencanaList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> pjList       = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> targetList   = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> statusList   = new ArrayList<ais.ui.util.MyTextbox>();

			final boolean isAdminCqi;
			{
				String roleIdCqi = (tbmuser == null || tbmuser.hakAkses() == null)
						? "" : (tbmuser.hakAkses().getRoleId() == null ? "" : tbmuser.hakAkses().getRoleId().trim());
				isAdminCqi = "am".equals(roleIdCqi) || "adAkdmk".equals(roleIdCqi)
						|| "Akademik".equals(roleIdCqi) || "admfak".equals(roleIdCqi)
						|| "admprd".equals(roleIdCqi) || "Kajur".equals(roleIdCqi);
			}
			final List<ais.ui.util.MyTextbox> adminKomList    = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> adminStatusList = new ArrayList<ais.ui.util.MyTextbox>();

			for (CapaianPembelajaranLulusan cpmk : cpmkList) {
				String kode = safeText(cpmk.getKode());
				JSONObject entry = cqiMap.containsKey(kode) ? cqiMap.get(kode) : new JSONObject();
				cpmkKodes.add(kode);

				Row r = new Row();
				r.setValign("top");
				r.setParent(rows);

				Vbox vb = new Vbox();
				Label lKode = new Label(kode);
				lKode.setStyle("font-weight:bold;color:#7c3aed;");
				lKode.setParent(vb);
				Label lNama = new Label(safeText(cpmk.getNama()));
				lNama.setStyle("font-size:10px;color:#64748b;");
				lNama.setParent(vb);
				vb.setParent(r);

				ais.ui.util.MyTextbox tbMasalah = new ais.ui.util.MyTextbox(entry.optString("masalah",""));
				tbMasalah.setRows(3); tbMasalah.setWidth("95%");
				tbMasalah.setTooltiptext("Deskripsikan masalah/gap ketercapaian CPMK ini");
				masalahList.add(tbMasalah); tbMasalah.setParent(r);

				ais.ui.util.MyTextbox tbAnalisis = new ais.ui.util.MyTextbox(entry.optString("analisis",""));
				tbAnalisis.setRows(3); tbAnalisis.setWidth("95%");
				tbAnalisis.setTooltiptext("Analisis akar penyebab masalah");
				analisisList.add(tbAnalisis); tbAnalisis.setParent(r);

				ais.ui.util.MyTextbox tbRencana = new ais.ui.util.MyTextbox(entry.optString("rencana",""));
				tbRencana.setRows(3); tbRencana.setWidth("95%");
				tbRencana.setTooltiptext("Rencana tindak lanjut perbaikan untuk semester berikutnya");
				rencanaList.add(tbRencana); tbRencana.setParent(r);

				ais.ui.util.MyTextbox tbPj = new ais.ui.util.MyTextbox(entry.optString("pj",""));
				tbPj.setWidth("95%");
				tbPj.setTooltiptext("Penanggung jawab tindak lanjut");
				pjList.add(tbPj); tbPj.setParent(r);

				ais.ui.util.MyTextbox tbTarget = new ais.ui.util.MyTextbox(entry.optString("targetWaktu",""));
				tbTarget.setWidth("95%");
				tbTarget.setTooltiptext("Target waktu — mis. Semester Genap 2025/2026");
				targetList.add(tbTarget); tbTarget.setParent(r);

				ais.ui.util.MyTextbox tbStatus = new ais.ui.util.MyTextbox(entry.optString("status","Planned"));
				tbStatus.setWidth("95%");
				tbStatus.setTooltiptext("Status: Planned / In Progress / Completed / Ditunda");
				statusList.add(tbStatus); tbStatus.setParent(r);

				// Admin: komentar
				String adminKomVal = entry.optString("adminKomentar","");
				if (isAdminCqi) {
					ais.ui.util.MyTextbox tbAdminKom = new ais.ui.util.MyTextbox(adminKomVal);
					tbAdminKom.setRows(2); tbAdminKom.setWidth("95%");
					tbAdminKom.setTooltiptext("Evaluasi/komentar admin atas analisis dosen untuk CPMK ini");
					adminKomList.add(tbAdminKom); tbAdminKom.setParent(r);
				} else {
					adminKomList.add(null);
					if (adminKomVal.isEmpty()) {
						r.appendChild(new Label("-"));
					} else {
						r.appendChild(new org.zkoss.zul.Html("<span style='font-size:10px;color:#1e40af;font-style:italic;'>"
								+ escapeHtml(adminKomVal) + "</span>"));
					}
				}

				// Admin: status (Pending/Disetujui/Revisi)
				String adminStVal = entry.optString("adminStatus", "Pending");
				if (isAdminCqi) {
					ais.ui.util.MyTextbox tbAdminSt = new ais.ui.util.MyTextbox(adminStVal);
					tbAdminSt.setWidth("95%");
					tbAdminSt.setTooltiptext("Status admin: Pending / Disetujui / Revisi");
					adminStatusList.add(tbAdminSt); tbAdminSt.setParent(r);
				} else {
					adminStatusList.add(null);
					String stColor = "Disetujui".equals(adminStVal) ? "#166534"
							: "Revisi".equals(adminStVal) ? "#991b1b" : "#78350f";
					r.appendChild(new org.zkoss.zul.Html(
						"<span style='font-size:10px;font-weight:700;color:" + stColor + ";'>"
						+ escapeHtml(adminStVal) + "</span>"));
				}
			}

			// ══ CPL Analysis Section ══════════════════════════════════════════
			final List<String> cplKodes = new ArrayList<String>();
			final List<ais.ui.util.MyTextbox> cplCatatanList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> cplAnalisisList = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> cplRencanaList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> cplAdminKomList = new ArrayList<ais.ui.util.MyTextbox>();
			if (!cplList.isEmpty()) {
				new org.zkoss.zul.Html("<div style='padding:7px 14px;background:#dbeafe;font-size:12px;"
					+ "font-weight:700;color:#1e3a8a;border-top:2px solid #93c5fd;"
					+ "border-bottom:1px solid #93c5fd;margin-top:4px;'>"
					+ "&#9313; Analisis per CPL (Capaian Pembelajaran Lulusan)</div>").setParent(centerWrap);
				MyGrid gridCpl = new MyGrid();
				gridCpl.setWidth("100%"); gridCpl.setParent(centerWrap);
				org.zkoss.zul.Columns colsCpl = new org.zkoss.zul.Columns(); colsCpl.setParent(gridCpl);
				ais.ui.util.MyColumnConfig cc0 = new ais.ui.util.MyColumnConfig("CPL");
				cc0.setWidth("12%"); cc0.setParent(colsCpl);
				new ais.ui.util.MyColumnConfig("Catatan Ketercapaian").setParent(colsCpl);
				new ais.ui.util.MyColumnConfig("Analisis / Hambatan").setParent(colsCpl);
				new ais.ui.util.MyColumnConfig("Rencana Peningkatan").setParent(colsCpl);
				ais.ui.util.MyColumnConfig cc4 = new ais.ui.util.MyColumnConfig("Komentar Admin");
				cc4.setWidth("14%"); cc4.setParent(colsCpl);
				Rows rowsCpl = new Rows(); rowsCpl.setParent(gridCpl);
				for (CapaianLulusan cpl : cplList) {
					String kode = safeText(cpl.getKode());
					JSONObject ce = null;
					try { ce = cplAnalObj.optJSONObject(kode); } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:503");}
					if (ce == null) ce = new JSONObject();
					cplKodes.add(kode);
					Row rc = new Row(); rc.setValign("top"); rc.setParent(rowsCpl);
					Vbox vcb = new Vbox();
					Label lcK = new Label(kode); lcK.setStyle("font-weight:bold;color:#1d4ed8;"); lcK.setParent(vcb);
					Label lcN = new Label(safeText(cpl.getNama())); lcN.setStyle("font-size:10px;color:#64748b;"); lcN.setParent(vcb);
					vcb.setParent(rc);
					ais.ui.util.MyTextbox tbCpCat = new ais.ui.util.MyTextbox(ce.optString("catatan",""));
					tbCpCat.setRows(3); tbCpCat.setWidth("95%");
					tbCpCat.setTooltiptext("Catatan ketercapaian CPL di mata kuliah ini");
					cplCatatanList.add(tbCpCat); tbCpCat.setParent(rc);
					ais.ui.util.MyTextbox tbCpAn = new ais.ui.util.MyTextbox(ce.optString("analisis",""));
					tbCpAn.setRows(3); tbCpAn.setWidth("95%");
					tbCpAn.setTooltiptext("Analisis hambatan atau kesenjangan pencapaian CPL");
					cplAnalisisList.add(tbCpAn); tbCpAn.setParent(rc);
					ais.ui.util.MyTextbox tbCpRen = new ais.ui.util.MyTextbox(ce.optString("rencana",""));
					tbCpRen.setRows(3); tbCpRen.setWidth("95%");
					tbCpRen.setTooltiptext("Rencana peningkatan untuk semester berikutnya");
					cplRencanaList.add(tbCpRen); tbCpRen.setParent(rc);
					String cpAdmVal = ce.optString("adminKomentar","");
					if (isAdminCqi) {
						ais.ui.util.MyTextbox tbCpAdm = new ais.ui.util.MyTextbox(cpAdmVal);
						tbCpAdm.setRows(2); tbCpAdm.setWidth("95%");
						tbCpAdm.setTooltiptext("Komentar admin atas analisis CPL");
						cplAdminKomList.add(tbCpAdm); tbCpAdm.setParent(rc);
					} else {
						cplAdminKomList.add(null);
						rc.appendChild(new org.zkoss.zul.Html(cpAdmVal.isEmpty()
							? "<span style='color:#94a3b8;'>-</span>"
							: "<span style='font-size:10px;color:#1e40af;font-style:italic;'>" + escapeHtml(cpAdmVal) + "</span>"));
					}
				}
			}

			// ══ PL Analysis Section ═══════════════════════════════════════════
			final List<String> plKodes = new ArrayList<String>();
			final List<ais.ui.util.MyTextbox> plCatatanList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> plAnalisisList = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> plRencanaList  = new ArrayList<ais.ui.util.MyTextbox>();
			final List<ais.ui.util.MyTextbox> plAdminKomList = new ArrayList<ais.ui.util.MyTextbox>();
			if (!plList.isEmpty()) {
				new org.zkoss.zul.Html("<div style='padding:7px 14px;background:#fef9c3;font-size:12px;"
					+ "font-weight:700;color:#713f12;border-top:2px solid #fde047;"
					+ "border-bottom:1px solid #fde047;margin-top:4px;'>"
					+ "&#9314; Analisis per PL (Profil Lulusan)</div>").setParent(centerWrap);
				MyGrid gridPl = new MyGrid();
				gridPl.setWidth("100%"); gridPl.setParent(centerWrap);
				org.zkoss.zul.Columns colsPl = new org.zkoss.zul.Columns(); colsPl.setParent(gridPl);
				ais.ui.util.MyColumnConfig pp0 = new ais.ui.util.MyColumnConfig("PL");
				pp0.setWidth("12%"); pp0.setParent(colsPl);
				new ais.ui.util.MyColumnConfig("Kontribusi MK").setParent(colsPl);
				new ais.ui.util.MyColumnConfig("Analisis Gap").setParent(colsPl);
				new ais.ui.util.MyColumnConfig("Rekomendasi").setParent(colsPl);
				ais.ui.util.MyColumnConfig pp4 = new ais.ui.util.MyColumnConfig("Komentar Admin");
				pp4.setWidth("14%"); pp4.setParent(colsPl);
				Rows rowsPl = new Rows(); rowsPl.setParent(gridPl);
				for (ProfilLulusan pl : plList) {
					String kode = safeText(pl.getKode());
					JSONObject pe = null;
					try { pe = plAnalObj.optJSONObject(kode); } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:563");}
					if (pe == null) pe = new JSONObject();
					plKodes.add(kode);
					Row rp = new Row(); rp.setValign("top"); rp.setParent(rowsPl);
					Vbox vpb = new Vbox();
					Label lpK = new Label(kode); lpK.setStyle("font-weight:bold;color:#b45309;"); lpK.setParent(vpb);
					Label lpN = new Label(safeText(pl.getNama())); lpN.setStyle("font-size:10px;color:#64748b;"); lpN.setParent(vpb);
					vpb.setParent(rp);
					ais.ui.util.MyTextbox tbPlCat = new ais.ui.util.MyTextbox(pe.optString("catatan",""));
					tbPlCat.setRows(3); tbPlCat.setWidth("95%");
					tbPlCat.setTooltiptext("Kontribusi MK terhadap profil lulusan ini");
					plCatatanList.add(tbPlCat); tbPlCat.setParent(rp);
					ais.ui.util.MyTextbox tbPlAn = new ais.ui.util.MyTextbox(pe.optString("analisis",""));
					tbPlAn.setRows(3); tbPlAn.setWidth("95%");
					tbPlAn.setTooltiptext("Analisis gap antara target PL dan capaian aktual");
					plAnalisisList.add(tbPlAn); tbPlAn.setParent(rp);
					ais.ui.util.MyTextbox tbPlRen = new ais.ui.util.MyTextbox(pe.optString("rencana",""));
					tbPlRen.setRows(3); tbPlRen.setWidth("95%");
					tbPlRen.setTooltiptext("Rekomendasi perbaikan untuk mendukung PL ini");
					plRencanaList.add(tbPlRen); tbPlRen.setParent(rp);
					String plAdmVal = pe.optString("adminKomentar","");
					if (isAdminCqi) {
						ais.ui.util.MyTextbox tbPlAdm = new ais.ui.util.MyTextbox(plAdmVal);
						tbPlAdm.setRows(2); tbPlAdm.setWidth("95%");
						tbPlAdm.setTooltiptext("Komentar admin atas analisis PL");
						plAdminKomList.add(tbPlAdm); tbPlAdm.setParent(rp);
					} else {
						plAdminKomList.add(null);
						rp.appendChild(new org.zkoss.zul.Html(plAdmVal.isEmpty()
							? "<span style='color:#94a3b8;'>-</span>"
							: "<span style='font-size:10px;color:#1e40af;font-style:italic;'>" + escapeHtml(plAdmVal) + "</span>"));
					}
				}
			}

			// ══ Admin Global Evaluation (admin only) ══════════════════════════
			final ais.ui.util.MyTextbox[] adminGlobalStatusRef  = new ais.ui.util.MyTextbox[]{null};
			final ais.ui.util.MyTextbox[] adminGlobalCatatanRef = new ais.ui.util.MyTextbox[]{null};
			if (isAdminCqi) {
				new org.zkoss.zul.Html("<div style='padding:7px 14px;background:#fce7f3;font-size:12px;"
					+ "font-weight:700;color:#831843;border-top:2px solid #f9a8d4;"
					+ "border-bottom:1px solid #f9a8d4;margin-top:4px;'>"
					+ "&#9733; Evaluasi Admin Global (Rangkuman CQI Keseluruhan)</div>").setParent(centerWrap);
				MyGrid gridAdm = new MyGrid();
				gridAdm.setWidth("100%"); gridAdm.setParent(centerWrap);
				org.zkoss.zul.Columns colsAdm = new org.zkoss.zul.Columns(); colsAdm.setParent(gridAdm);
				ais.ui.util.MyColumnConfig ag0 = new ais.ui.util.MyColumnConfig("Status Global");
				ag0.setWidth("15%"); ag0.setParent(colsAdm);
				new ais.ui.util.MyColumnConfig("Catatan Evaluasi Global").setParent(colsAdm);
				Rows rowsAdm = new Rows(); rowsAdm.setParent(gridAdm);
				Row radm = new Row(); radm.setValign("top"); radm.setParent(rowsAdm);
				ais.ui.util.MyTextbox tbGlSt = new ais.ui.util.MyTextbox(
						metaEntry.optString("adminGlobalStatus","Pending"));
				tbGlSt.setWidth("95%");
				tbGlSt.setTooltiptext("Status keseluruhan CQI: Pending / Disetujui / Revisi");
				adminGlobalStatusRef[0] = tbGlSt; tbGlSt.setParent(radm);
				ais.ui.util.MyTextbox tbGlCat = new ais.ui.util.MyTextbox(
						metaEntry.optString("adminGlobalCatatan",""));
				tbGlCat.setRows(3); tbGlCat.setWidth("99%");
				tbGlCat.setTooltiptext("Catatan evaluasi admin secara keseluruhan atas CQI dosen di semester ini");
				adminGlobalCatatanRef[0] = tbGlCat; tbGlCat.setParent(radm);
			}

			// South: Save / Cancel
			org.zkoss.zul.South south = new org.zkoss.zul.South();
			ZkCompat.setFlex(south, true);
			south.setStyle("background:#fff;border-top:1px solid #e2e8f0;padding:10px;");
			south.setParent(bl);

			Toolbar stb = new Toolbar();
			stb.setStyle("float:right;background:transparent;border:none;");
			stb.setParent(south);

			MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			btnBatal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception { win.detach(); }
			});
			btnBatal.setParent(stb);

			MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan CQI", "/img/save.gif");
			btnSimpan.setStyle("font-weight:bold;background:#c2410c;color:#fff;border-radius:6px;padding:5px 14px;border:0;");
			btnSimpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					JSONArray result = new JSONArray();
					// ① CPMK entries
					for (int i = 0; i < cpmkKodes.size(); i++) {
						JSONObject e2 = new JSONObject();
						e2.put("cpmk",       cpmkKodes.get(i));
						e2.put("masalah",    masalahList.get(i).getValue().trim());
						e2.put("analisis",   analisisList.get(i).getValue().trim());
						e2.put("rencana",    rencanaList.get(i).getValue().trim());
						e2.put("pj",         pjList.get(i).getValue().trim());
						e2.put("targetWaktu",targetList.get(i).getValue().trim());
						e2.put("status",     statusList.get(i).getValue().trim().isEmpty()
								? "Planned" : statusList.get(i).getValue().trim());
						e2.put("adminKomentar", isAdminCqi && adminKomList.get(i) != null
								? adminKomList.get(i).getValue().trim()
								: (cqiMap.containsKey(cpmkKodes.get(i))
								   ? cqiMap.get(cpmkKodes.get(i)).optString("adminKomentar","") : ""));
						e2.put("adminStatus", isAdminCqi && adminStatusList.get(i) != null
								? adminStatusList.get(i).getValue().trim()
								: (cqiMap.containsKey(cpmkKodes.get(i))
								   ? cqiMap.get(cpmkKodes.get(i)).optString("adminStatus","Pending") : "Pending"));
						result.put(e2);
					}
					// ② __meta__ entry: CPL, PL, admin global
					JSONObject meta = new JSONObject();
					meta.put("cpmk", "__meta__");
					meta.put("adminGlobalStatus", adminGlobalStatusRef[0] != null
							? adminGlobalStatusRef[0].getValue().trim()
							: metaEntry.optString("adminGlobalStatus","Pending"));
					meta.put("adminGlobalCatatan", adminGlobalCatatanRef[0] != null
							? adminGlobalCatatanRef[0].getValue().trim()
							: metaEntry.optString("adminGlobalCatatan",""));
					JSONObject cplSave = new JSONObject();
					for (int i = 0; i < cplKodes.size(); i++) {
						JSONObject ce2 = new JSONObject();
						ce2.put("catatan",  cplCatatanList.get(i).getValue().trim());
						ce2.put("analisis", cplAnalisisList.get(i).getValue().trim());
						ce2.put("rencana",  cplRencanaList.get(i).getValue().trim());
						String cplOldAdm = "";
						try { JSONObject co = cplAnalObj.optJSONObject(cplKodes.get(i));
							if (co != null) cplOldAdm = co.optString("adminKomentar",""); } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:687");}
						ce2.put("adminKomentar", isAdminCqi && cplAdminKomList.get(i) != null
								? cplAdminKomList.get(i).getValue().trim() : cplOldAdm);
						cplSave.put(cplKodes.get(i), ce2);
					}
					meta.put("cplAnalisis", cplSave.toString());
					JSONObject plSave = new JSONObject();
					for (int i = 0; i < plKodes.size(); i++) {
						JSONObject pe2 = new JSONObject();
						pe2.put("catatan",  plCatatanList.get(i).getValue().trim());
						pe2.put("analisis", plAnalisisList.get(i).getValue().trim());
						pe2.put("rencana",  plRencanaList.get(i).getValue().trim());
						String plOldAdm = "";
						try { JSONObject po = plAnalObj.optJSONObject(plKodes.get(i));
							if (po != null) plOldAdm = po.optString("adminKomentar",""); } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:701");}
						pe2.put("adminKomentar", isAdminCqi && plAdminKomList.get(i) != null
								? plAdminKomList.get(i).getValue().trim() : plOldAdm);
						plSave.put(plKodes.get(i), pe2);
					}
					meta.put("plAnalisis", plSave.toString());
					result.put(meta);
					perkuliahan.setCqiData(result.toString());
					Common.refreshSaveOrUpdate(perkuliahan);
					win.detach();
					if (contentToRefresh != null) {
						contentToRefresh.setContent(buildPortofolioMkHtml());
					}
					MyMessageboxConfig.show("CQI berhasil disimpan.",
							"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			});
			btnSimpan.setParent(stb);

			win.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private String buildPortofolioMkHtml() {
		KurikulumPunyaMatakuliah kpm = perkuliahan == null ? null : perkuliahan.getKurikulumPunyaMatakuliah();
		Matakuliah mk = perkuliahan == null ? null : perkuliahan.getMatakuliah();
		if (kpm == null || mk == null) return "<p>Data tidak tersedia.</p>";

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:system-ui,sans-serif;padding:16px;max-width:860px;'>");

		// ── Identitas MK ──────────────────────────────────────────────────
		sb.append("<div style='background:#eff6ff;border-radius:14px;padding:16px;margin-bottom:14px;"
				+ "border:1px solid #bfdbfe;'>");
		sb.append("<div style='font-size:14px;font-weight:900;color:#1d4ed8;margin-bottom:8px;'>"
				+ "&#128218; Portofolio Mata Kuliah — Loop 1 Monitoring CPMK</div>");
		sb.append("<table style='font-size:12px;border-collapse:collapse;width:100%;'>");
		String[][] identitas = {
			{"Mata Kuliah", safeText(mk.getNama()) + " (" + safeText(mk.getKode()) + ")"},
			{"SKS", mk.getSks() != null ? mk.getSks().toString() : "-"},
			{"Semester/TA", safeText(perkuliahan.getGanjilGenap()) + " / " + safeText(perkuliahan.getTahunAjaran())},
			{"Prodi", perkuliahan.getJurusan() != null ? safeText(perkuliahan.getJurusan().getNama()) : "-"},
			{"Dosen", perkuliahan.getDosen1() != null ? safeText(perkuliahan.getDosen1().getNama()) : "-"},
			{"Target Minimal", (kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 75.0) + "%"},
		};
		for (String[] row : identitas) {
			sb.append("<tr><td style='padding:3px 10px 3px 0;color:#475569;font-weight:700;width:140px;'>")
			  .append(escapeHtml(row[0])).append("</td>")
			  .append("<td style='padding:3px 0;color:#0f172a;'>").append(escapeHtml(row[1])).append("</td></tr>");
		}
		sb.append("</table></div>");

		// ── Tabel CPMK ────────────────────────────────────────────────────
		Session sess = null;
		try {
			sess = HibernateUtil.openSession();
			java.util.Set<Long> cpmkIds = parseIdsToSet(mk.getCapaianPembelajaranLulusan());
			List<CapaianPembelajaranLulusan> cpmkList = cpmkIds.isEmpty()
				? new java.util.ArrayList<CapaianPembelajaranLulusan>()
				: (List<CapaianPembelajaranLulusan>) sess.createCriteria(CapaianPembelajaranLulusan.class)
					.add(Restrictions.in("id", cpmkIds))
					.addOrder(Order.asc("kode")).list();

			sb.append("<div style='border:1px solid #e5e7eb;border-radius:14px;overflow:auto;margin-bottom:14px;'>");
			sb.append("<div style='padding:10px 14px;background:#f8fafc;border-bottom:1px solid #e5e7eb;"
					+ "font-weight:900;color:#0f172a;'>&#127919; Rencana Penilaian CPMK</div>");
			sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
			sb.append("<tr style='background:#f1f5f9;color:#334155;'>");
			for (String h : new String[]{"Kode CPMK", "Isi CPMK", "Bobot (%)", "Minimal (%)", "Sub-CPMK", "Teknik Penilaian"}) {
				sb.append("<th style='padding:7px 8px;text-align:left;border-bottom:1px solid #e5e7eb;white-space:nowrap;'>")
				  .append(escapeHtml(h)).append("</th>");
			}
			sb.append("</tr>");
			if (cpmkList.isEmpty()) {
				sb.append("<tr><td colspan='6' style='padding:10px;color:#94a3b8;'>"
						+ "CPMK belum didefinisikan untuk mata kuliah ini.</td></tr>");
			} else {
				for (CapaianPembelajaranLulusan c : cpmkList) {
					int subCount = 0;
					List<String> subKodes = new java.util.ArrayList<String>();
					if (c.getFormula() != null && !c.getFormula().trim().isEmpty()) {
						try {
							org.json.JSONArray arr = new org.json.JSONArray(c.getFormula());
							subCount = arr.length();
							for (int i = 0; i < arr.length(); i++) {
								String kd = arr.getJSONObject(i).optString("kode", "");
								if (!kd.isEmpty()) subKodes.add(kd);
							}
						} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:792");}
					}
					double bobot = c.getBobot() != null ? c.getBobot() : 0.0;
					double minimal = c.getMinimal() != null ? c.getMinimal() : 75.0;
					String bobotColor = bobot > 0 ? "#1e293b" : "#94a3b8";
					sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>");
					sb.append("<td style='padding:6px 8px;font-weight:700;color:#7c3aed;'>")
					  .append(escapeHtml(safeText(c.getKode()))).append("</td>");
					sb.append("<td style='padding:6px 8px;color:#334155;'>")
					  .append(escapeHtml(safeText(c.getNama()))).append("</td>");
					sb.append("<td style='padding:6px 8px;text-align:center;font-weight:700;color:").append(bobotColor).append(";'>")
					  .append(bobot > 0 ? String.format("%.0f", bobot) : "-").append("</td>");
					sb.append("<td style='padding:6px 8px;text-align:center;color:#475569;'>")
					  .append(String.format("%.0f", minimal)).append("</td>");
					sb.append("<td style='padding:6px 8px;text-align:center;color:#0369a1;font-weight:700;'>")
					  .append(subCount > 0 ? subCount + (subKodes.isEmpty() ? "" : " (" + String.join(", ", subKodes) + ")") : "-")
					  .append("</td>");
					// Cari teknik penilaian dari teknikPerCpmk
					String cpmkKodeKey = safeText(c.getKode());
					String teknikDisplay = "";
					if (!kpm.getTeknikPerCpmk().isEmpty()) {
						for (String line : kpm.getTeknikPerCpmk().split("\n")) {
							String l = line.trim();
							if (l.isEmpty() || !l.contains(":")) continue;
							String[] kv = l.split(":", 2);
							if (kv[0].trim().equalsIgnoreCase(cpmkKodeKey)) {
								teknikDisplay = kv[1].trim();
								break;
							}
						}
					}
					sb.append("<td style='padding:6px 8px;color:#0369a1;font-size:10px;'>")
					  .append(teknikDisplay.isEmpty() ? "<span style='color:#94a3b8;'>—</span>"
					  			: escapeHtml(teknikDisplay)).append("</td>");
					sb.append("</tr>");
				}
			}
			sb.append("</table></div>");

			// ── CPL yang dibebankan ───────────────────────────────────────
			String cplCsv = mk.getCapaianLulusan();
			if (cplCsv != null && !cplCsv.trim().isEmpty()) {
				java.util.Set<Long> cplIds = parseIdsToSet(cplCsv);
				if (!cplIds.isEmpty()) {
					List<ais.database.model.obe.CapaianLulusan> cplList =
						(List<ais.database.model.obe.CapaianLulusan>) sess.createCriteria(
							ais.database.model.obe.CapaianLulusan.class)
								.add(Restrictions.in("id", cplIds))
								.addOrder(Order.asc("kode")).list();
					sb.append("<div style='border:1px solid #e5e7eb;border-radius:14px;overflow:auto;margin-bottom:14px;'>");
					sb.append("<div style='padding:10px 14px;background:#f0fdf4;border-bottom:1px solid #bbf7d0;"
							+ "font-weight:900;color:#166534;'>&#127919; CPL yang Dibebankan pada MK ini</div>");
					sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
					sb.append("<tr style='background:#f1f5f9;'>");
					for (String h : new String[]{"Kode CPL", "Deskripsi CPL", "Kategori (SN-Dikti)"}) {
						sb.append("<th style='padding:7px 8px;text-align:left;border-bottom:1px solid #e5e7eb;'>")
						  .append(escapeHtml(h)).append("</th>");
					}
					sb.append("</tr>");
					for (ais.database.model.obe.CapaianLulusan cpl : cplList) {
						sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>")
						  .append("<td style='padding:6px 8px;font-weight:700;color:#166534;'>")
						  .append(escapeHtml(safeText(cpl.getKode()))).append("</td>")
						  .append("<td style='padding:6px 8px;color:#334155;'>")
						  .append(escapeHtml(safeText(cpl.getNama()))).append("</td>")
						  .append("<td style='padding:6px 8px;color:#475569;'>")
						  .append(escapeHtml(safeText(cpl.getKategori()))).append("</td></tr>");
					}
					sb.append("</table></div>");
				}
			}

		} catch (Exception e) {
			sb.append("<div style='color:#ef4444;padding:10px;'>Error memuat data CPMK: "
					+ escapeHtml(e.getMessage()) + "</div>");
		} finally {
			closeSessionQuietly(sess);
		}

		// ── CQI (Continuous Quality Improvement) ─────────────────────────────
		sb.append("<div style='border:1px solid #fed7aa;border-radius:14px;overflow:auto;margin-top:10px;'>");
		sb.append("<div style='padding:10px 14px;background:#fff7ed;border-bottom:1px solid #fed7aa;"
				+ "font-weight:900;color:#c2410c;'>&#128295; CQI — Rencana Perbaikan Berkelanjutan (Loop 1 PPEPP)</div>");
		// Parse existing CQI data (CPMK items + __meta__)
		String cqiRaw = perkuliahan != null ? perkuliahan.getCqiData() : "";
		JSONArray cqiArr = new JSONArray();
		JSONObject cqiMeta = new JSONObject();
		JSONObject cplAn = new JSONObject();
		JSONObject plAn  = new JSONObject();
		if (!cqiRaw.isEmpty()) {
			try {
				cqiArr = new JSONArray(cqiRaw);
				for (int i = 0; i < cqiArr.length(); i++) {
					JSONObject ei = cqiArr.optJSONObject(i);
					if (ei == null) continue;
					if ("__meta__".equals(ei.optString("cpmk",""))) {
						cqiMeta = ei;
						try { cplAn = new JSONObject(ei.optString("cplAnalisis","{}")); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:889");}
						try { plAn  = new JSONObject(ei.optString("plAnalisis",  "{}")); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:890");}
					}
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:893");}
		}

		// Admin global status banner
		String glStatus = cqiMeta.optString("adminGlobalStatus","");
		if (!glStatus.isEmpty()) {
			String glBg = "Disetujui".equals(glStatus) ? "#dcfce7" : "Revisi".equals(glStatus) ? "#fee2e2" : "#fef9c3";
			String glTx = "Disetujui".equals(glStatus) ? "#166534" : "Revisi".equals(glStatus) ? "#991b1b" : "#78350f";
			String glCat = cqiMeta.optString("adminGlobalCatatan","");
			sb.append("<div style='padding:8px 14px;background:").append(glBg)
			  .append(";font-size:11px;color:").append(glTx).append(";border-bottom:1px solid #e2e8f0;'>")
			  .append("<b>Status Admin Global: ").append(escapeHtml(glStatus)).append("</b>");
			if (!glCat.isEmpty()) sb.append(" — ").append(escapeHtml(glCat));
			sb.append("</div>");
		}

		// Count real CPMK entries (exclude __meta__)
		int cqiCount = 0;
		for (int i = 0; i < cqiArr.length(); i++) {
			JSONObject ei = cqiArr.optJSONObject(i);
			if (ei != null && !"__meta__".equals(ei.optString("cpmk",""))) cqiCount++;
		}

		if (cqiCount == 0) {
			sb.append("<div style='padding:12px;font-size:12px;color:#92400e;font-style:italic;'>"
					+ "&#9432; Belum ada data CQI. Klik tombol <b>Edit CQI</b> di atas untuk memasukkan rencana perbaikan per CPMK.</div>");
		} else {
			// CPMK table
			sb.append("<div style='padding:7px 14px;background:#ede9fe;font-size:11px;font-weight:700;color:#5b21b6;'>&#9312; Analisis per CPMK</div>");
			sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
			sb.append("<tr style='background:#fef3c7;'>");
			for (String h : new String[]{"No", "CPMK", "Masalah / Gap", "Analisis Penyebab",
					"Rencana Tindak Lanjut", "PJ", "Target Waktu", "Status", "Komentar Admin", "Status Admin"}) {
				sb.append("<th style='padding:6px 8px;text-align:left;border-bottom:1px solid #fcd34d;'>")
				  .append(escapeHtml(h)).append("</th>");
			}
			sb.append("</tr>");
			int rowNum = 0;
			for (int i = 0; i < cqiArr.length(); i++) {
				JSONObject e = cqiArr.optJSONObject(i);
				if (e == null || "__meta__".equals(e.optString("cpmk",""))) continue;
				rowNum++;
				String status = e.optString("status", "Planned");
				String stColor = "Completed".equals(status) ? "#166534"
						: "In Progress".equals(status) ? "#1e40af" : "#92400e";
				String admSt = e.optString("adminStatus","Pending");
				String admStColor = "Disetujui".equals(admSt) ? "#166534"
						: "Revisi".equals(admSt) ? "#991b1b" : "#78350f";
				sb.append("<tr style='border-bottom:1px solid #fef3c7;'>");
				sb.append("<td style='padding:5px 8px;text-align:center;color:#d97706;font-weight:700;'>").append(rowNum).append("</td>");
				sb.append("<td style='padding:5px 8px;font-weight:700;color:#7c3aed;'>").append(escapeHtml(e.optString("cpmk",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(e.optString("masalah",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;color:#475569;'>").append(escapeHtml(e.optString("analisis",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(e.optString("rencana",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;color:#475569;'>").append(escapeHtml(e.optString("pj",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;color:#475569;'>").append(escapeHtml(e.optString("targetWaktu",""))).append("</td>");
				sb.append("<td style='padding:5px 8px;font-weight:700;color:").append(stColor).append(";'>")
				  .append(escapeHtml(status)).append("</td>");
				String admKom = e.optString("adminKomentar","");
				sb.append("<td style='padding:5px 8px;font-style:italic;color:#1e40af;'>").append(escapeHtml(admKom.isEmpty() ? "-" : admKom)).append("</td>");
				sb.append("<td style='padding:5px 8px;font-weight:700;color:").append(admStColor).append(";'>")
				  .append(escapeHtml(admSt)).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");

			// CPL analysis display
			if (cplAn.length() > 0) {
				sb.append("<div style='padding:7px 14px;background:#dbeafe;font-size:11px;font-weight:700;color:#1e3a8a;margin-top:4px;'>&#9313; Analisis per CPL</div>");
				sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
				sb.append("<tr style='background:#eff6ff;'>");
				for (String h2 : new String[]{"CPL","Catatan Ketercapaian","Analisis / Hambatan","Rencana Peningkatan","Komentar Admin"}) {
					sb.append("<th style='padding:6px 8px;text-align:left;border-bottom:1px solid #bfdbfe;'>").append(escapeHtml(h2)).append("</th>");
				}
				sb.append("</tr>");
				java.util.Iterator<String> cplKeys = cplAn.keys();
				while (cplKeys.hasNext()) {
					String ck = cplKeys.next();
					JSONObject cv = cplAn.optJSONObject(ck);
					if (cv == null) continue;
					sb.append("<tr style='border-bottom:1px solid #eff6ff;'>");
					sb.append("<td style='padding:5px 8px;font-weight:700;color:#1d4ed8;'>").append(escapeHtml(ck)).append("</td>");
					sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(cv.optString("catatan",""))).append("</td>");
					sb.append("<td style='padding:5px 8px;color:#475569;'>").append(escapeHtml(cv.optString("analisis",""))).append("</td>");
					sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(cv.optString("rencana",""))).append("</td>");
					String cAdm = cv.optString("adminKomentar","");
					sb.append("<td style='padding:5px 8px;font-style:italic;color:#1e40af;'>").append(escapeHtml(cAdm.isEmpty() ? "-" : cAdm)).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
			}

			// PL analysis display
			if (plAn.length() > 0) {
				sb.append("<div style='padding:7px 14px;background:#fef9c3;font-size:11px;font-weight:700;color:#713f12;margin-top:4px;'>&#9314; Analisis per PL (Profil Lulusan)</div>");
				sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
				sb.append("<tr style='background:#fefce8;'>");
				for (String h3 : new String[]{"PL","Kontribusi MK","Analisis Gap","Rekomendasi","Komentar Admin"}) {
					sb.append("<th style='padding:6px 8px;text-align:left;border-bottom:1px solid #fde047;'>").append(escapeHtml(h3)).append("</th>");
				}
				sb.append("</tr>");
				java.util.Iterator<String> plKeys = plAn.keys();
				while (plKeys.hasNext()) {
					String pk = plKeys.next();
					JSONObject pv = plAn.optJSONObject(pk);
					if (pv == null) continue;
					sb.append("<tr style='border-bottom:1px solid #fefce8;'>");
					sb.append("<td style='padding:5px 8px;font-weight:700;color:#b45309;'>").append(escapeHtml(pk)).append("</td>");
					sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(pv.optString("catatan",""))).append("</td>");
					sb.append("<td style='padding:5px 8px;color:#475569;'>").append(escapeHtml(pv.optString("analisis",""))).append("</td>");
					sb.append("<td style='padding:5px 8px;'>").append(escapeHtml(pv.optString("rencana",""))).append("</td>");
					String pAdm = pv.optString("adminKomentar","");
					sb.append("<td style='padding:5px 8px;font-style:italic;color:#1e40af;'>").append(escapeHtml(pAdm.isEmpty() ? "-" : pAdm)).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
			}
		}
		sb.append("</div>");

		sb.append("</div>");  // end outer div
		return sb.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) throws Exception {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getId() != null) {
				session.refresh(kurikulumPunyaMatakuliah);
			}
			if (kurikulumPunyaMatakuliah == null || kurikulumPunyaMatakuliah.getMatakuliah() == null) {
				return ais.common.HashMapGenerator.getRand();
			}

			Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
			Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("id", kurikulumPunyaMatakuliah.getId());
			Common.insertProperty(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah, parameters, "", 2);

			Set<Long> cplIds = parseIdsToSet(matakuliah.getCapaianLulusan());
			List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(
					session.createCriteria(CapaianLulusan.class)
							.add(cplIds.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", cplIds))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					CapaianLulusan.class);

			List<Map> capaianLulusansD = new ArrayList<Map>();
			for (CapaianLulusan capaianLulusan : capaianLulusans) {
				Map map = new HashMap();
				Common.insertProperty(CapaianLulusan.class, capaianLulusan, map, "");
				capaianLulusansD.add(map);
				Common.insertProperty(CapaianLulusan.class, capaianLulusan, parameters,
						"capaianLulusan_" + capaianLulusan.getId());
			}
			parameters.put("capaianLulusans", capaianLulusansD);

			Set<Long> cpmkIds = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());
			List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans = ConstantValues.simpleList(
					session.createCriteria(CapaianPembelajaranLulusan.class)
							.add(cpmkIds.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", cpmkIds))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					CapaianPembelajaranLulusan.class);

			List<Map> subCpmkD = new ArrayList<Map>();
			List<Map> capaianPembelajaranLulusansD = new ArrayList<Map>();
			for (CapaianPembelajaranLulusan capaianPembelajaranLulusan : capaianPembelajaranLulusans) {
				Map map = new HashMap();
				Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusan, map, "");
				capaianPembelajaranLulusansD.add(map);
				Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusan, parameters,
						"capaianPembelajaranLulusan_" + capaianPembelajaranLulusan.getId());

				JSONArray array = safeJsonArray(capaianPembelajaranLulusan.getFormula());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.isNull("key")) {
						continue;
					}
					Map map1 = new HashMap();
					Iterator iterator = jsonObject.keys();
					while (iterator.hasNext()) {
						try {
							String keyData = (String) iterator.next();
							map1.put(keyData, jsonObject.get(keyData));
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					map1.put("kode", jsonObject.isNull("kode") ? "" : jsonObject.get("kode") + "");
					map1.put("nama", jsonObject.isNull("nama") ? "" : jsonObject.get("nama") + "");
					map1.put("bobot", Double.valueOf(normalizeBobotToPercent(safeDouble(jsonObject, "bobot", 0.0))));
					subCpmkD.add(map1);
				}
			}
			parameters.put("subCpmk", subCpmkD);
			parameters.put("capaianPembelajaranLulusans", capaianPembelajaranLulusansD);

			Set<Long> bahanIds = parseIdsToSet(matakuliah.getBahanKajian());
			List<Map> bahanKajians = new ArrayList<Map>();
			for (Long idBahan : bahanIds) {
				BahanKajian bahanKajian = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), idBahan);
				if (bahanKajian != null) {
					Map map = new HashMap();
					Common.insertProperty(BahanKajian.class, bahanKajian, map, "");
					bahanKajians.add(map);
					Common.insertProperty(BahanKajian.class, bahanKajian, parameters,
							"bahanKajian_" + bahanKajian.getId());
				}
			}
			parameters.put("bahanKajians", bahanKajians);
			return parameters;
		} finally {
			closeSessionQuietly(session);
		}
	}

	public static void cetak(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) throws Exception {
		Report.generatePDFReport(Report.PDF, parameter(kurikulumPunyaMatakuliah), "rps_obe",
				kurikulumPunyaMatakuliah == null ? null : kurikulumPunyaMatakuliah.getTanggal_dirubah());
	}

	public void onCetak(Event event) throws Exception {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan == null ? null
				: perkuliahan.getKurikulumPunyaMatakuliah();
		if (kurikulumPunyaMatakuliah == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Data Matakuliah dan Kurikulum",
					"Kolom Data Matakuliah dan Kurikulum belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Data Matakuliah dan Kurikulum.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return;
		}
		cetak(kurikulumPunyaMatakuliah);
	}

	private void initMk() throws Exception {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.getKurikulumPunyaMatakuliah();
		if (kurikulumPunyaMatakuliah == null || perkuliahan.getMatakuliah() == null) {
			showWarning("Kurikulum OBE atau mata kuliah belum tersedia pada perkuliahan ini.");
			return;
		}

		MyGroupConfig group = new MyGroupConfig("MATA KULIAH");
		rowsUtama.appendChild(group);

		Vbox card = new Vbox();
		card.setWidth("100%");
		card.setStyle(STYLE_CARD);
		Row wrapper = new Row();
		ais.ui.util.ZkCompat.setSpans(wrapper, "2");
		wrapper.setValign("top");
		wrapper.setParent(rowsUtama);
		wrapper.appendChild(card);

		card.appendChild(new Html(buildMkHeroHtml(kurikulumPunyaMatakuliah)));
		MyGrid infoGrid = new MyGrid();
		infoGrid.setWidth("100%");
		infoGrid.setStyle("border:0;background:transparent;");
		infoGrid.setParent(card);
		Rows rows = new Rows();
		rows.setParent(infoGrid);

		createInfoRow(rows, "Kode", new Label(safeText(perkuliahan.getMatakuliah().getKode())));
		createInfoRow(rows, "Nama", RevisiHelper.createNewRevisi(Matakuliah.class, perkuliahan.getMatakuliah(),
				perkuliahan.getMatakuliah().getNama()));
		createInfoRow(rows, "Rumpun MK",
				new Label(perkuliahan.getMatakuliah().getKelompokMatakuliah() == null ? ""
						: safeText(perkuliahan.getMatakuliah().getKelompokMatakuliah().getNama())));
		createInfoRow(rows, "SKS Mata Kuliah",
				new Label(Common.numberFormat.get().format(safeDouble(perkuliahan.getMatakuliah().getSks()))));
		if (safeDouble(perkuliahan.getMatakuliah().getSksDiskusi()) > 0) {
			createInfoRow(rows, "SKS Tatap Muka",
					new Label(Common.numberFormat.get().format(perkuliahan.getMatakuliah().getSksDiskusi())));
		}
		if (safeDouble(perkuliahan.getMatakuliah().getSksPraktek()) > 0) {
			createInfoRow(rows, "SKS Praktikum",
					new Label(Common.numberFormat.get().format(perkuliahan.getMatakuliah().getSksPraktek())));
		}
		if (safeDouble(perkuliahan.getMatakuliah().getSksPraktekLapangan()) > 0) {
			createInfoRow(rows, "SKS Praktikum Lapangan",
					new Label(Common.numberFormat.get().format(perkuliahan.getMatakuliah().getSksPraktekLapangan())));
		}
		if (safeDouble(perkuliahan.getMatakuliah().getSksSimulasi()) > 0) {
			createInfoRow(rows, "SKS Simulasi",
					new Label(Common.numberFormat.get().format(perkuliahan.getMatakuliah().getSksSimulasi())));
		}
		createInfoRow(rows, "Kurikulum", new Label(kurikulumPunyaMatakuliah.getKurikulum() == null ? ""
				: safeText(kurikulumPunyaMatakuliah.getKurikulum().getNama())));
		createInfoRow(rows, "Prodi", new Label(perkuliahan.getJurusan() == null ? "" : safeText(perkuliahan.getJurusan().getNama())));
		createInfoRow(rows, "Semester", new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getSemester())));
		createInfoRow(rows, "Tanggal Penyusunan",
				new Label(kurikulumPunyaMatakuliah.getTanggalPenyusunan() == null ? ""
						: Common.dateFormat6.get().format(kurikulumPunyaMatakuliah.getTanggalPenyusunan())));
	}

	private void createInfoRow(Rows parent, String labelText, Component value) {
		Row row = new Row();
		row.setValign("middle");
		row.setStyle("border-bottom:1px dashed #e2e8f0;background:transparent;");
		row.setParent(parent);
		MyLabelConfigAgakBesar label = new MyLabelConfigAgakBesar(labelText);
		label.setStyle(STYLE_LABEL);
		row.appendChild(label);
		if (value instanceof Label) {
			((Label) value).setStyle(STYLE_BADGE);
		}
		row.appendChild(value);
	}

	@SuppressWarnings("deprecation")
	private void initRinci() throws Exception {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.getKurikulumPunyaMatakuliah();
		MyGroupConfig group = new MyGroupConfig("Nilai Rinci");
		rowsUtama.appendChild(group);

		try {
			jsonArraykurikulumPunyaMatakuliah = new JSONObject(kurikulumPunyaMatakuliah.getRincian());
		} catch (Exception e) {
			jsonArraykurikulumPunyaMatakuliah = new JSONObject();
		}
		rowRinci = new Row();
		ais.ui.util.ZkCompat.setSpans(rowRinci, "2");
		rowRinci.setValign("top");
		rowRinci.setParent(rowsUtama);
		reloadRinci();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void reloadRinci() throws Exception {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.getKurikulumPunyaMatakuliah();
		Common.clear(rowRinci);

		Vbox outer = new Vbox();
		outer.setWidth("100%");
		outer.setStyle(STYLE_CARD);
		outer.setParent(rowRinci);

		if (refresh) {
			perkuliahan.reInitDetailperkuliahan(HibernateUtil.currentSession());
		}
		Long detailperkuliahanid = perkuliahan.ambilDetailperkuliahan(mhs);
		Detailperkuliahan detailperkuliahan = (Detailperkuliahan) (detailperkuliahanid == null ? null
				: GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString()));
		if (detailperkuliahan == null) {
			outer.appendChild(new Html("<div style='" + STYLE_INFO + "'>Data KRS/detail perkuliahan mahasiswa belum ditemukan atau belum disetujui.</div>"));
			return;
		}

		TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah.populateRinci(jsonArraykurikulumPunyaMatakuliah);
		Session session = HibernateUtil.currentSession();
		ObeCalculationResult result = calculateObe(session, maps, detailperkuliahan);

		outer.appendChild(new Html(buildSummaryHtml(result, kurikulumPunyaMatakuliah)));
		outer.appendChild(new Html("<div style='" + STYLE_INFO + " margin-bottom:10px;'>Rumus mengikuti file Excel: "
				+ "<b>Nilai Teknik = Bobot CPMK x Nilai Mahasiswa</b>; "
				+ "<b>Nilai Bobot tiap CPMK</b> adalah akumulasi nilai teknik per CPMK; "
				+ "<b>Ketercapaian CPL pada MK (%) = Total Nilai CPL / Total Nilai MK x 100</b>.</div>"));

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(outer);
		grid.setHeight("100%");
		grid.setStyle(STYLE_GRID);

		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "PL", "70px", null);
		addColumn(columns, "CPL", "90px", null);
		addColumn(columns, "Minggu", "78px", null);
		addColumn(columns, "CPMK", "90px", null);
		addColumn(columns, "Sub-CPMK", "110px", null);
		addColumn(columns, "Indikator Penilaian", null, null);
		addColumn(columns, "Teknik", "120px", null);
		addColumn(columns, "Bobot CPMK", "95px", "right");
		addColumn(columns, "Nilai Mhs", "85px", "right");
		addColumn(columns, "Nilai Teknik", "105px", "right");
		addColumn(columns, "Nilai Bobot CPMK", "120px", "right");
		addColumn(columns, "Nilai Bobot CPL", "115px", "right");
		addColumn(columns, "Ketercapaian CPL pada MK (%)", "130px", "right");
		addColumn(columns, "Keterangan", "150px", null);

		Rows rows = new Rows();
		rows.setParent(grid);

		Set<String> shownCpmk = new HashSet<String>();
		Set<String> shownCpl = new HashSet<String>();
		Set<String> renderedUnique = new HashSet<String>();

		for (ObeRowData data : result.rows) {
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #f1f5f9;");

			appendText(row, data.kodeProfilLulusan, data.namaProfilLulusan);
			appendText(row, data.kodeCapaian, data.namaCapaian);
			appendText(row, data.mingguText, null);
			appendText(row, data.kodeCpmk, data.namaCpmk);
			appendText(row, data.kodeSubCpmk, data.namaSubCpmk);
			appendText(row, data.indikator, null);
			appendText(row, data.teknik, null);

			if (!renderedUnique.contains(data.uniqueKey) && data.formatNilai != null) {
				appendNumber(row, Common.numberFormat.get().format(data.bobotPercent) + "%");
				appendNumber(row, Common.numberFormat.get().format(data.nilaiMahasiswa));
				appendNumber(row, Common.numberFormat.get().format(data.nilaiTeknik));

				if (!shownCpmk.contains(data.kodeCpmk)) {
					appendNumber(row, Common.numberFormat.get().format(safeMapValue(result.totalCpmk, data.kodeCpmk)));
					shownCpmk.add(data.kodeCpmk);
				} else {
					appendText(row, "", null);
				}

				if (!shownCpl.contains(data.kodeCapaian)) {
					Double totalCpl = safeMapValue(result.totalCpl, data.kodeCapaian);
					appendNumber(row, Common.numberFormat.get().format(totalCpl));
					Double ketercapaian = result.totalNilaiMk > 0 ? (totalCpl / result.totalNilaiMk) * 100.0 : 0.0;
					appendNumber(row, Common.numberFormat.get().format(ketercapaian));
					shownCpl.add(data.kodeCapaian);
				} else {
					appendText(row, "", null);
					appendText(row, "", null);
				}

				if (data.nilaiMahasiswa >= safeDouble(kurikulumPunyaMatakuliah.getMinimalKetercapaian(), 0.0)) {
					appendText(row, "Tidak ada perbaikan", null);
				} else {
					MyLabelAgakKecilBoldMerah label = new MyLabelAgakKecilBoldMerah("Perbaikan pada " + data.indikator);
					row.appendChild(label);
				}
				renderedUnique.add(data.uniqueKey);
			} else {
				appendEmpty(row, 7);
			}
			row.setParent(rows);
		}

		Foot foot = new Foot();
		foot.setParent(grid);
		for (int i = 0; i < 6; i++) {
			foot.appendChild(new Footer());
		}
		foot.appendChild(new Footer("Total"));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.totalBobotPercent) + "%"));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.rataNilaiMahasiswa)));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.totalNilaiMk)));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.totalNilaiMk)));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.totalNilaiMk)));
		foot.appendChild(new Footer(Common.numberFormat.get().format(result.totalKetercapaianCplPercent)));
		foot.appendChild(new Footer());
	}

	private ObeCalculationResult calculateObe(Session session, TreeMap<Integer, Map> maps,
			Detailperkuliahan detailperkuliahan) throws Exception {
		ObeCalculationResult result = new ObeCalculationResult();
		if (maps == null || maps.isEmpty()) {
			return result;
		}
		if (formatNilais == null) {
			formatNilais = new ArrayList<FormatNilai>();
		}

		Map<String, FormatNilai> formatByKey = buildFormatNilaiKeyMap(formatNilais);
		Set<Long> cpmkIds = new HashSet<Long>();
		for (FormatNilai f : formatNilais) {
			if (f != null && f.getCapaianPembelajaranLulusan() != null
					&& f.getCapaianPembelajaranLulusan().getId() != null) {
				cpmkIds.add(f.getCapaianPembelajaranLulusan().getId());
			}
		}

		List<CapaianLulusan> allCpl = loadAllCplForCpmk(session, cpmkIds);
		Set<Long> allPlIds = parseIdsToSet(perkuliahan.getMatakuliah() == null ? null : perkuliahan.getMatakuliah().getProfilLulusan());
		Map<Long, ProfilLulusan> profilById = loadProfilMap(session, allPlIds);

		Set<String> uniqueProcessed = new HashSet<String>();
		for (Map map : maps.values()) {
			ObeRowData rowData = buildObeRowData(map, formatByKey, allCpl, profilById, detailperkuliahan);
			if (rowData == null) {
				continue;
			}
			result.rows.add(rowData);

			if (!uniqueProcessed.contains(rowData.uniqueKey) && rowData.formatNilai != null) {
				result.totalBobotPercent += rowData.bobotPercent;
				result.totalNilaiMk += rowData.nilaiTeknik;
				result.totalNilaiMahasiswaMentah += rowData.nilaiMahasiswa;
				result.jumlahNilaiMahasiswa++;
				addToMap(result.totalCpmk, rowData.kodeCpmk, rowData.nilaiTeknik);
				addToMap(result.totalCpl, rowData.kodeCapaian, rowData.nilaiTeknik);
				uniqueProcessed.add(rowData.uniqueKey);
			}
		}

		result.rataNilaiMahasiswa = result.jumlahNilaiMahasiswa > 0
				? result.totalNilaiMahasiswaMentah / result.jumlahNilaiMahasiswa
				: 0.0;
		for (String key : result.totalCpl.keySet()) {
			Double totalCpl = result.totalCpl.get(key);
			result.totalKetercapaianCplPercent += result.totalNilaiMk > 0 ? (totalCpl / result.totalNilaiMk) * 100.0 : 0.0;
		}
		return result;
	}

	private Map<String, FormatNilai> buildFormatNilaiKeyMap(List<FormatNilai> data) {
		Map<String, FormatNilai> map = new HashMap<String, FormatNilai>();
		if (data == null) {
			return map;
		}
		for (FormatNilai f : data) {
			if (f == null || f.getCapaianPembelajaranLulusan() == null
					|| f.getCapaianPembelajaranLulusan().getId() == null || isBlank(f.getKodeSubCpmk())) {
				continue;
			}
			map.put(buildFormatKey(f.getCapaianPembelajaranLulusan().getId(), f.getKodeSubCpmk()), f);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private List<CapaianLulusan> loadAllCplForCpmk(Session session, Set<Long> cpmkIds) {
		List<CapaianLulusan> result = new ArrayList<CapaianLulusan>();
		if (session == null || cpmkIds == null || cpmkIds.isEmpty()) {
			return result;
		}
		try {
			List<CapaianLulusan> all = ConstantValues.simpleList(
					session.createCriteria(CapaianLulusan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
					CapaianLulusan.class);
			for (CapaianLulusan c : all) {
				if (c == null) {
					continue;
				}
				for (Long id : cpmkIds) {
					if (containsDelimitedId(c.getCapaianPembelajaranLulusan(), id)) {
						result.add(c);
						break;
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<Long, ProfilLulusan> loadProfilMap(Session session, Set<Long> ids) {
		Map<Long, ProfilLulusan> map = new HashMap<Long, ProfilLulusan>();
		if (session == null || ids == null || ids.isEmpty()) {
			return map;
		}
		try {
			List<ProfilLulusan> data = ConstantValues.simpleList(
					session.createCriteria(ProfilLulusan.class).add(Restrictions.in("id", ids)).addOrder(Order.asc("kode")),
					ProfilLulusan.class);
			for (ProfilLulusan p : data) {
				if (p != null && p.getId() != null) {
					map.put(p.getId(), p);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return map;
	}

	private ObeRowData buildObeRowData(Map map, Map<String, FormatNilai> formatByKey, List<CapaianLulusan> allCpl,
			Map<Long, ProfilLulusan> profilById, Detailperkuliahan detailperkuliahan) {
		try {
			JSONObject jsonObject = (JSONObject) map.get("jsonObject");
			JSONObject subCpmk = (JSONObject) map.get("subCpmk");
			CapaianPembelajaranLulusan cpmk = (CapaianPembelajaranLulusan) map.get("capaianPembelajaranLulusanData");
			if (jsonObject == null || subCpmk == null || cpmk == null || cpmk.getId() == null) {
				return null;
			}

			String subKey = getSubCpmkKey(jsonObject, subCpmk);
			FormatNilai formatNilai = formatByKey.get(buildFormatKey(cpmk.getId(), subKey));
			ObeRowData data = new ObeRowData();
			data.jsonObject = jsonObject;
			data.subCpmk = subCpmk;
			data.cpmk = cpmk;
			data.formatNilai = formatNilai;
			data.uniqueKey = subKey + "_" + cpmk.getId();
			data.kodeCpmk = safeText(cpmk.getKode());
			data.namaCpmk = safeText(cpmk.getNama());
			data.kodeSubCpmk = subCpmk.isNull("kode") ? "" : safeText(subCpmk.optString("kode"));
			data.namaSubCpmk = subCpmk.isNull("nama") ? "Belum ditentukan" : safeText(subCpmk.optString("nama"));
			data.indikator = jsonObject.isNull("indikator") ? "" : safeText(jsonObject.optString("indikator"));
			data.teknik = jsonObject.isNull("teknikDanKriteria") ? "" : safeText(jsonObject.optString("teknikDanKriteria"));
			data.mingguText = safeInt(jsonObject, "mulaiMingguKe", 0) + " sd "
					+ safeInt(jsonObject, "sampaiMingguKe", 0);
			data.bobotRaw = safeDouble(subCpmk, "bobot", 0.0);
			data.bobotFraction = normalizeBobotToFraction(data.bobotRaw);
			data.bobotPercent = normalizeBobotToPercent(data.bobotRaw);
			data.nilaiMahasiswa = formatNilai == null ? 0.0 : safeDouble(detailperkuliahan.retreiveDetailNilai(formatNilai));
			data.nilaiTeknik = data.nilaiMahasiswa * data.bobotFraction;

			List<CapaianLulusan> relatedCpl = new ArrayList<CapaianLulusan>();
			for (CapaianLulusan c : allCpl) {
				if (c != null && containsDelimitedId(c.getCapaianPembelajaranLulusan(), cpmk.getId())) {
					relatedCpl.add(c);
				}
			}

			Set<Long> profileIds = new HashSet<Long>();
			StringBuilder kodeCpl = new StringBuilder();
			StringBuilder namaCpl = new StringBuilder();
			for (CapaianLulusan c : relatedCpl) {
				appendWithComma(kodeCpl, safeText(c.getKode()));
				appendWithNewLine(namaCpl, safeText(c.getNama()));
				profileIds.addAll(parseIdsToSet(c.getProfil()));
			}
			data.kodeCapaian = kodeCpl.toString();
			data.namaCapaian = namaCpl.toString();

			StringBuilder kodePl = new StringBuilder();
			StringBuilder namaPl = new StringBuilder();
			for (Long id : profileIds) {
				ProfilLulusan p = profilById.get(id);
				if (p != null) {
					appendWithNewLine(kodePl, safeText(p.getKode()));
					appendWithNewLine(namaPl, safeText(p.getNama()));
				}
			}
			data.kodeProfilLulusan = kodePl.toString();
			data.namaProfilLulusan = namaPl.toString();
			return data;
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private String buildMkHeroHtml(KurikulumPunyaMatakuliah kpm) {
		String mk = perkuliahan.getMatakuliah() == null ? "" : safeText(perkuliahan.getMatakuliah().getNama());
		String kur = kpm.getKurikulum() == null ? "" : safeText(kpm.getKurikulum().getNama());
		return "<div style=\"padding:4px 2px 12px 2px;\">"
				+ "<div style=\"font-size:11px;text-transform:uppercase;letter-spacing:.08em;color:#2563eb;font-weight:900;\">Nilai OBE Mahasiswa</div>"
				+ "<div style=\"font-size:18px;font-weight:900;color:#0f172a;margin-top:4px;\">" + escapeHtml(mk)
				+ "</div>" + "<div style=\"font-size:12px;color:#64748b;margin-top:4px;\">Kurikulum: "
				+ escapeHtml(kur) + "</div>" + "</div>";
	}

	private String buildSummaryHtml(ObeCalculationResult result, KurikulumPunyaMatakuliah kpm) {
		double minimal = safeDouble(kpm.getMinimalKetercapaian(), 0.0);
		String status = result.rataNilaiMahasiswa >= minimal ? "Tercapai" : "Perlu Perbaikan";
		String statusColor = result.rataNilaiMahasiswa >= minimal ? "#166534" : "#b91c1c";
		return "<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;margin-bottom:10px;\">"
				+ buildMiniCard("Total Bobot", Common.numberFormat.get().format(result.totalBobotPercent) + "%", "Bobot CPMK")
				+ buildMiniCard("Total Nilai MK", Common.numberFormat.get().format(result.totalNilaiMk), "Σ Bobot x Nilai")
				+ buildMiniCard("Rata-rata Nilai", Common.numberFormat.get().format(result.rataNilaiMahasiswa), "Nilai mahasiswa")
				+ "<div style=\"padding:12px;border-radius:14px;background:#ffffff;border:1px solid #e5e7eb;\">"
				+ "<div style=\"font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;\">Status</div>"
				+ "<div style=\"font-size:18px;font-weight:900;color:" + statusColor + ";margin-top:4px;\">" + status
				+ "</div><div style=\"font-size:11px;color:#64748b;margin-top:4px;\">Minimal "
				+ Common.numberFormat.get().format(minimal) + "</div></div></div>";
	}

	private String buildMiniCard(String title, String value, String subtitle) {
		return "<div style=\"padding:12px;border-radius:14px;background:#ffffff;border:1px solid #e5e7eb;\">"
				+ "<div style=\"font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;\">" + escapeHtml(title)
				+ "</div><div style=\"font-size:22px;font-weight:900;color:#0f172a;margin-top:4px;\">"
				+ escapeHtml(value) + "</div><div style=\"font-size:11px;color:#64748b;margin-top:4px;\">"
				+ escapeHtml(subtitle) + "</div></div>";
	}


	private String buildStudentObeChartsHtml(StudentObeDashboardResult data) {
		StringBuilder sb = new StringBuilder();
		if (data == null || !data.hasData) {
			return "";
		}
		sb.append("<div style='margin:12px 0 14px 0;'>")
				.append("<div style='display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:8px;'>")
				.append("<div>")
				.append("<div style='font-size:13px;font-weight:900;color:#0f172a;'>Ringkasan Visual OBE Mahasiswa</div>")
				.append("<div style='font-size:11px;color:#64748b;margin-top:2px;'>Trend, distribusi, dan spider web chart dihitung untuk mahasiswa yang sedang dipilih.</div>")
				.append("</div>")
				.append("<div style='font-size:11px;color:#475569;background:#f8fafc;border:1px solid #e2e8f0;border-radius:999px;padding:5px 9px;'>Target: ")
				.append(Common.numberFormat.get().format(data.targetMinimal)).append("</div>")
				.append("</div>");

		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;'>");
		sb.append(buildStudentAssessmentTrendChart("Trend Komponen Asesmen OBE", data.assessmentRows, data.targetMinimal));
		sb.append(buildStudentTrendChart("Trend Sub-CPMK / Format Nilai", data.subCpmkRows, data.targetMinimal));
		sb.append(buildStudentOutcomeComparisonChart(data));
		sb.append(buildStudentCpmkAchievementChart(data));
		sb.append(buildStudentCplAchievementChart(data));
		sb.append(buildStudentDistributionChart(data));
		sb.append(buildStudentAssessmentSpiderWebChart("Spider Web Komponen Asesmen", data.assessmentRows, data.targetMinimal));
		sb.append(buildStudentSubCpmkSpiderWebChart("Spider Web Sub-CPMK", data.subCpmkRows, data.targetMinimal));
		sb.append(buildStudentSpiderWebChart("Spider Web CPMK", data.cpmkRows, data.targetMinimal));
		sb.append(buildStudentSpiderWebChart("Spider Web CPL", data.cplRows, data.targetMinimal));
		sb.append(buildStudentSpiderWebChart("Spider Web PL", data.plRows, data.targetMinimal));
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildStudentAssessmentTrendChart(String title, List<StudentAssessmentRow> rows, double target) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>")
				.append(escapeHtml(title)).append("</div>")
				.append("<div style='font-size:10px;color:#64748b;margin:-4px 0 8px 0;'>Komponen sama seperti tab DATA pada rekap OBE: Sub-CPMK + nama ujian/tugas, difilter untuk mahasiswa ini.</div>")
				.append("<div style='max-height:270px;overflow:auto;padding-right:4px;'>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada komponen ujian/tugas yang memiliki nilai OBE.</div>");
		} else {
			int max = Math.min(rows.size(), 14);
			for (int i = 0; i < max; i++) {
				StudentAssessmentRow row = rows.get(i);
				String code = row == null ? "" : row.subCpmkKode + " - " + row.namaKomponen;
				String name = row == null ? "" : "Nilai " + Common.numberFormat.get().format(row.nilai)
						+ "; kontribusi " + Common.numberFormat.get().format(row.nilaiBerbobot)
						+ "; bobot komponen " + Common.numberFormat.get().format(row.bobotKomponen * 100.0) + "%";
				sb.append(buildCssBarRow(code, name, row == null ? 0.0 : row.nilai, target));
			}
			if (rows.size() > max) {
				sb.append("<div style='font-size:10px;color:#64748b;margin-top:6px;'>+")
						.append(rows.size() - max).append(" komponen lain ditampilkan pada tabel rinci.</div>");
			}
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildStudentTrendChart(String title, List<StudentSubCpmkRow> rows, double target) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>")
				.append(escapeHtml(title)).append("</div>")
				.append("<div style='max-height:250px;overflow:auto;padding-right:4px;'>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data Sub-CPMK.</div>");
		} else {
			int max = Math.min(rows.size(), 12);
			for (int i = 0; i < max; i++) {
				StudentSubCpmkRow row = rows.get(i);
				double nilai = clampPercent(row == null ? 0.0 : row.konversi);
				sb.append(buildCssBarRow(row == null ? "" : row.kode, row == null ? "" : row.nama, nilai, target));
			}
			if (rows.size() > max) {
				sb.append("<div style='font-size:10px;color:#64748b;margin-top:6px;'>+")
						.append(rows.size() - max).append(" Sub-CPMK lain ditampilkan pada tabel rinci.</div>");
			}
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildStudentOutcomeComparisonChart(StudentObeDashboardResult data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>Perbandingan Rata-rata Outcome</div>");
		double avgSub = averageSubCpmk(data == null ? null : data.subCpmkRows);
		double avgCpmk = averageOutcome(data == null ? null : data.cpmkRows);
		double avgCpl = averageOutcome(data == null ? null : data.cplRows);
		double avgPl = averageOutcome(data == null ? null : data.plRows);
		double target = data == null ? 0.0 : data.targetMinimal;
		sb.append(buildCssBarRow("Sub-CPMK", "Rata-rata nilai konversi format", avgSub, target));
		sb.append(buildCssBarRow("CPMK", "Rata-rata capaian CPMK", avgCpmk, target));
		sb.append(buildCssBarRow("CPL", "Rata-rata capaian CPL", avgCpl, target));
		sb.append(buildCssBarRow("PL", "Rata-rata capaian profil lulusan", avgPl, target));
		sb.append("<div style='font-size:10px;color:#64748b;margin-top:8px;'>Bar hijau berarti memenuhi target minimal, bar merah berarti belum memenuhi.</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Batang ketercapaian tiap CPMK dibanding target minimal. Berbeda dengan
	 * "Perbandingan Rata-rata Outcome" (yang menampilkan rata-rata per level),
	 * grafik ini menunjukkan nilai SETIAP CPMK sehingga cepat terlihat CPMK mana
	 * yang belum tuntas. Memakai ulang buildCssBarRow agar gaya konsisten.
	 */
	private String buildStudentCpmkAchievementChart(StudentObeDashboardResult data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:2px;'>Ketercapaian per CPMK vs Target</div>")
				.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Nilai tiap CPMK dibanding nilai minimal; cepat terlihat CPMK mana yang belum tuntas.</div>")
				.append("<div style='max-height:250px;overflow:auto;padding-right:4px;'>");
		double target = data == null ? 0.0 : data.targetMinimal;
		List<StudentOutcomeRow> rows = data == null ? null : data.cpmkRows;
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data CPMK.</div>");
		} else {
			for (StudentOutcomeRow row : rows) {
				double nilai = clampPercent(row == null ? 0.0 : row.nilai);
				sb.append(buildCssBarRow(row == null ? "" : row.kode, row == null ? "" : row.nama, nilai, target));
			}
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Batang ketercapaian tiap CPL dibanding target minimal. Sejajar dengan grafik
	 * per-CPMK, namun pada tingkat Capaian Pembelajaran Lulusan. Memakai ulang
	 * buildCssBarRow agar gaya konsisten dengan grafik lain.
	 */
	private String buildStudentCplAchievementChart(StudentObeDashboardResult data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:2px;'>Ketercapaian per CPL vs Target</div>")
				.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Nilai tiap Capaian Pembelajaran Lulusan dibanding nilai minimal.</div>")
				.append("<div style='max-height:250px;overflow:auto;padding-right:4px;'>");
		double target = data == null ? 0.0 : data.targetMinimal;
		List<StudentOutcomeRow> rows = data == null ? null : data.cplRows;
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data CPL.</div>");
		} else {
			for (StudentOutcomeRow row : rows) {
				double nilai = clampPercent(row == null ? 0.0 : row.nilai);
				sb.append(buildCssBarRow(row == null ? "" : row.kode, row == null ? "" : row.nama, nilai, target));
			}
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildStudentDistributionChart(StudentObeDashboardResult data) {
		int lulusSub = countSubStatus(data == null ? null : data.subCpmkRows, true);
		int belumSub = countSubStatus(data == null ? null : data.subCpmkRows, false);
		int lulusCpmk = countOutcomeStatus(data == null ? null : data.cpmkRows, true);
		int belumCpmk = countOutcomeStatus(data == null ? null : data.cpmkRows, false);
		int lulusCpl = countOutcomeStatus(data == null ? null : data.cplRows, true);
		int belumCpl = countOutcomeStatus(data == null ? null : data.cplRows, false);
		int lulusPl = countOutcomeStatus(data == null ? null : data.plRows, true);
		int belumPl = countOutcomeStatus(data == null ? null : data.plRows, false);
		int skala4 = 0;
		int skala3 = 0;
		int skala2 = 0;
		int skala1 = 0;
		if (data != null && data.subCpmkRows != null) {
			for (StudentSubCpmkRow row : data.subCpmkRows) {
				int scale = getScale4Local(row == null ? 0.0 : row.konversi);
				if (scale == 4) {
					skala4++;
				} else if (scale == 3) {
					skala3++;
				} else if (scale == 2) {
					skala2++;
				} else {
					skala1++;
				}
			}
		}
		int maxStatus = Math.max(1, Math.max(Math.max(lulusSub + belumSub, lulusCpmk + belumCpmk), Math.max(lulusCpl + belumCpl, lulusPl + belumPl)));
		int maxSkala = Math.max(1, Math.max(Math.max(skala4, skala3), Math.max(skala2, skala1)));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>Distribusi Capaian</div>")
				.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;'>")
				.append("<div>")
				.append("<div style='font-size:11px;font-weight:900;color:#475569;margin-bottom:5px;'>Status L/TL</div>")
				.append(buildStackBar("Sub-CPMK", lulusSub, belumSub, maxStatus))
				.append(buildStackBar("CPMK", lulusCpmk, belumCpmk, maxStatus))
				.append(buildStackBar("CPL", lulusCpl, belumCpl, maxStatus))
				.append(buildStackBar("PL", lulusPl, belumPl, maxStatus))
				.append("</div><div>")
				.append("<div style='font-size:11px;font-weight:900;color:#475569;margin-bottom:5px;'>Skala 4 Sub-CPMK</div>")
				.append(buildSmallCountBar("4", skala4, maxSkala, "#166534"))
				.append(buildSmallCountBar("3", skala3, maxSkala, "#2563eb"))
				.append(buildSmallCountBar("2", skala2, maxSkala, "#d97706"))
				.append(buildSmallCountBar("1", skala1, maxSkala, "#b91c1c"))
				.append("</div></div></div>");
		return sb.toString();
	}

	private String buildStudentSpiderWebChart(String title, List<StudentOutcomeRow> rows, double target) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>")
				.append(escapeHtml(title)).append("</div>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data untuk spider web chart.</div></div>");
			return sb.toString();
		}
		int n = Math.min(rows.size(), 8);
		int center = 120;
		int radius = 82;
		StringBuilder points = new StringBuilder();
		StringBuilder axis = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		StringBuilder targetPoints = new StringBuilder();
		for (int i = 0; i < n; i++) {
			StudentOutcomeRow row = rows.get(i);
			double angle = (-Math.PI / 2.0) + (2.0 * Math.PI * i / n);
			double gridX = center + Math.cos(angle) * radius;
			double gridY = center + Math.sin(angle) * radius;
			axis.append("<line x1='").append(center).append("' y1='").append(center).append("' x2='")
					.append(formatSvg(gridX)).append("' y2='").append(formatSvg(gridY))
					.append("' stroke='#e2e8f0' stroke-width='1'/>");
			double nilai = clampPercent(row == null ? 0.0 : row.nilai);
			double x = center + Math.cos(angle) * radius * (nilai / 100.0);
			double y = center + Math.sin(angle) * radius * (nilai / 100.0);
			if (points.length() > 0) {
				points.append(' ');
			}
			points.append(formatSvg(x)).append(',').append(formatSvg(y));
			double t = clampPercent(target);
			double tx = center + Math.cos(angle) * radius * (t / 100.0);
			double ty = center + Math.sin(angle) * radius * (t / 100.0);
			if (targetPoints.length() > 0) {
				targetPoints.append(' ');
			}
			targetPoints.append(formatSvg(tx)).append(',').append(formatSvg(ty));
			double lx = center + Math.cos(angle) * (radius + 16);
			double ly = center + Math.sin(angle) * (radius + 16);
			labels.append("<text x='").append(formatSvg(lx)).append("' y='").append(formatSvg(ly))
					.append("' font-size='9' text-anchor='middle' fill='#475569'>")
					.append(escapeHtml(row == null ? "" : row.kode)).append("</text>");
		}
		sb.append("<div style='display:flex;align-items:center;justify-content:center;'>")
				.append("<svg viewBox='0 0 240 240' style='width:100%;max-width:260px;height:240px;'>")
				.append("<circle cx='120' cy='120' r='82' fill='#f8fafc' stroke='#e2e8f0'/>")
				.append("<circle cx='120' cy='120' r='61.5' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='120' cy='120' r='41' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='120' cy='120' r='20.5' fill='none' stroke='#e2e8f0'/>")
				.append(axis.toString())
				.append("<polygon points='").append(targetPoints.toString())
				.append("' fill='rgba(245,158,11,.10)' stroke='#f59e0b' stroke-width='1.5' stroke-dasharray='4 3'/>")
				.append("<polygon points='").append(points.toString())
				.append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2'/>")
				.append(labels.toString())
				.append("</svg></div>")
				.append("<div style='font-size:10px;color:#64748b;text-align:center;'>Garis oranye = target minimal, area biru = capaian mahasiswa.</div>")
				.append("</div>");
		return sb.toString();
	}


	private String buildStudentSubCpmkSpiderWebChart(String title, List<StudentSubCpmkRow> rows, double target) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>")
				.append(escapeHtml(title)).append("</div>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data Sub-CPMK untuk spider web chart.</div></div>");
			return sb.toString();
		}
		int n = Math.min(rows.size(), 8);
		int center = 135;
		int radius = 78;
		StringBuilder points = new StringBuilder();
		StringBuilder axis = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		StringBuilder targetPoints = new StringBuilder();
		for (int i = 0; i < n; i++) {
			StudentSubCpmkRow row = rows.get(i);
			double angle = (-Math.PI / 2.0) + (2.0 * Math.PI * i / n);
			double gridX = center + Math.cos(angle) * radius;
			double gridY = center + Math.sin(angle) * radius;
			axis.append("<line x1='").append(center).append("' y1='").append(center).append("' x2='")
					.append(formatSvg(gridX)).append("' y2='").append(formatSvg(gridY))
					.append("' stroke='#e2e8f0' stroke-width='1'/>");
			double nilai = clampPercent(row == null ? 0.0 : row.konversi);
			double x = center + Math.cos(angle) * radius * (nilai / 100.0);
			double y = center + Math.sin(angle) * radius * (nilai / 100.0);
			if (points.length() > 0) points.append(' ');
			points.append(formatSvg(x)).append(',').append(formatSvg(y));
			double t = clampPercent(target);
			double tx = center + Math.cos(angle) * radius * (t / 100.0);
			double ty = center + Math.sin(angle) * radius * (t / 100.0);
			if (targetPoints.length() > 0) targetPoints.append(' ');
			targetPoints.append(formatSvg(tx)).append(',').append(formatSvg(ty));
			double lx = center + Math.cos(angle) * (radius + 34);
			double ly = center + Math.sin(angle) * (radius + 34);
			labels.append("<text x='").append(formatSvg(lx)).append("' y='").append(formatSvg(ly))
					.append("' font-size='8.5' text-anchor='middle' fill='#475569'>")
					.append(escapeHtml(shortChartLabel(row == null ? "" : row.kode))).append("</text>");
		}
		sb.append("<div style='display:flex;align-items:center;justify-content:center;'>")
				.append("<svg viewBox='0 0 270 270' style='width:100%;max-width:300px;height:270px;'>")
				.append("<circle cx='135' cy='135' r='78' fill='#f8fafc' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='58.5' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='39' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='19.5' fill='none' stroke='#e2e8f0'/>")
				.append(axis.toString())
				.append("<polygon points='").append(targetPoints.toString())
				.append("' fill='rgba(245,158,11,.10)' stroke='#f59e0b' stroke-width='1.5' stroke-dasharray='4 3'/>")
				.append("<polygon points='").append(points.toString())
				.append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2'/>")
				.append(labels.toString()).append("</svg></div>")
				.append("<div style='font-size:10px;color:#64748b;text-align:center;'>Garis oranye = target, area biru = capaian Sub-CPMK.</div>")
				.append("</div>");
		return sb.toString();
	}

	private String buildStudentAssessmentSpiderWebChart(String title, List<StudentAssessmentRow> rows, double target) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;padding:12px;box-shadow:0 6px 18px rgba(15,23,42,.05);'>")
				.append("<div style='font-size:12px;font-weight:900;color:#0f172a;margin-bottom:8px;'>")
				.append(escapeHtml(title)).append("</div>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#64748b;padding:8px;'>Belum ada data komponen asesmen untuk spider web chart.</div></div>");
			return sb.toString();
		}
		int n = Math.min(rows.size(), 8);
		int center = 135;
		int radius = 78;
		StringBuilder points = new StringBuilder();
		StringBuilder axis = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		StringBuilder targetPoints = new StringBuilder();
		for (int i = 0; i < n; i++) {
			StudentAssessmentRow row = rows.get(i);
			double angle = (-Math.PI / 2.0) + (2.0 * Math.PI * i / n);
			double gridX = center + Math.cos(angle) * radius;
			double gridY = center + Math.sin(angle) * radius;
			axis.append("<line x1='").append(center).append("' y1='").append(center).append("' x2='")
					.append(formatSvg(gridX)).append("' y2='").append(formatSvg(gridY))
					.append("' stroke='#e2e8f0' stroke-width='1'/>");
			double nilai = clampPercent(row == null ? 0.0 : row.nilai);
			double x = center + Math.cos(angle) * radius * (nilai / 100.0);
			double y = center + Math.sin(angle) * radius * (nilai / 100.0);
			if (points.length() > 0) points.append(' ');
			points.append(formatSvg(x)).append(',').append(formatSvg(y));
			double t = clampPercent(target);
			double tx = center + Math.cos(angle) * radius * (t / 100.0);
			double ty = center + Math.sin(angle) * radius * (t / 100.0);
			if (targetPoints.length() > 0) targetPoints.append(' ');
			targetPoints.append(formatSvg(tx)).append(',').append(formatSvg(ty));
			double lx = center + Math.cos(angle) * (radius + 34);
			double ly = center + Math.sin(angle) * (radius + 34);
			labels.append("<text x='").append(formatSvg(lx)).append("' y='").append(formatSvg(ly))
					.append("' font-size='8.5' text-anchor='middle' fill='#475569'>")
					.append(escapeHtml(shortChartLabel(row == null ? "" : row.namaKomponen))).append("</text>");
		}
		sb.append("<div style='display:flex;align-items:center;justify-content:center;'>")
				.append("<svg viewBox='0 0 270 270' style='width:100%;max-width:300px;height:270px;'>")
				.append("<circle cx='135' cy='135' r='78' fill='#f8fafc' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='58.5' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='39' fill='none' stroke='#e2e8f0'/>")
				.append("<circle cx='135' cy='135' r='19.5' fill='none' stroke='#e2e8f0'/>")
				.append(axis.toString())
				.append("<polygon points='").append(targetPoints.toString())
				.append("' fill='rgba(245,158,11,.10)' stroke='#f59e0b' stroke-width='1.5' stroke-dasharray='4 3'/>")
				.append("<polygon points='").append(points.toString())
				.append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2'/>")
				.append(labels.toString()).append("</svg></div>")
				.append("<div style='font-size:10px;color:#64748b;text-align:center;'>Label mengikuti komponen ujian/tugas pada rekap OBE.</div>")
				.append("</div>");
		return sb.toString();
	}

	private String shortChartLabel(String label) {
		String text = label == null ? "" : label.trim();
		if (text.length() > 18) {
			return text.substring(0, 16) + "..";
		}
		return text;
	}

	private String buildCssBarRow(String code, String name, double value, double target) {
		double clamped = clampPercent(value);
		boolean ok = value >= target;
		String color = ok ? "#16a34a" : "#ef4444";
		String bg = ok ? "#dcfce7" : "#fee2e2";
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-bottom:8px;'>")
				.append("<div style='display:flex;justify-content:space-between;gap:8px;align-items:flex-start;'>")
				.append("<div style='min-width:0;'>")
				.append("<div style='font-size:11px;font-weight:900;color:#0f172a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>")
				.append(escapeHtml(code)).append("</div>")
				.append("<div style='font-size:10px;color:#64748b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>")
				.append(escapeHtml(name)).append("</div></div>")
				.append("<div style='font-size:11px;font-weight:900;color:").append(color).append(";'>")
				.append(Common.numberFormat.get().format(value)).append("</div></div>")
				.append("<div style='height:9px;border-radius:999px;background:#f1f5f9;overflow:hidden;margin-top:4px;'>")
				.append("<div style='height:9px;width:").append(Common.numberFormat.get().format(clamped))
				.append("%;background:").append(color).append(";border-radius:999px;'></div></div>")
				.append("<div style='height:2px;background:").append(bg).append(";width:")
				.append(Common.numberFormat.get().format(clampPercent(target))).append("%;margin-top:2px;'></div>")
				.append("</div>");
		return sb.toString();
	}

	private String buildStackBar(String label, int ok, int notOk, int max) {
		int total = ok + notOk;
		double okPct = total <= 0 ? 0.0 : (ok * 100.0) / total;
		double barWidth = max <= 0 ? 0.0 : (total * 100.0) / max;
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-bottom:8px;'>")
				.append("<div style='display:flex;justify-content:space-between;font-size:10px;color:#475569;'>")
				.append("<b>").append(escapeHtml(label)).append("</b><span>L:").append(ok).append(" / TL:").append(notOk).append("</span></div>")
				.append("<div style='height:10px;background:#f1f5f9;border-radius:999px;overflow:hidden;margin-top:3px;width:")
				.append(Common.numberFormat.get().format(Math.max(8.0, barWidth))).append("%;'>")
				.append("<div style='height:10px;background:#16a34a;width:").append(Common.numberFormat.get().format(okPct))
				.append("%;float:left;'></div>")
				.append("<div style='height:10px;background:#ef4444;width:").append(Common.numberFormat.get().format(100.0 - okPct))
				.append("%;float:left;'></div></div></div>");
		return sb.toString();
	}

	private String buildSmallCountBar(String label, int value, int max, String color) {
		double width = max <= 0 ? 0.0 : (value * 100.0) / max;
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:32px 1fr 28px;gap:6px;align-items:center;margin-bottom:7px;font-size:10px;color:#475569;'>")
				.append("<b>Sk ").append(escapeHtml(label)).append("</b>")
				.append("<div style='height:9px;background:#f1f5f9;border-radius:999px;overflow:hidden;'>")
				.append("<div style='height:9px;width:").append(Common.numberFormat.get().format(width))
				.append("%;background:").append(color).append(";border-radius:999px;'></div></div>")
				.append("<b style='text-align:right;'>").append(value).append("</b></div>");
		return sb.toString();
	}

	private double averageSubCpmk(List<StudentSubCpmkRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		int count = 0;
		for (StudentSubCpmkRow row : rows) {
			if (row != null) {
				total += row.konversi;
				count++;
			}
		}
		return count <= 0 ? 0.0 : total / count;
	}

	private double averageOutcome(List<StudentOutcomeRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		int count = 0;
		for (StudentOutcomeRow row : rows) {
			if (row != null) {
				total += row.nilai;
				count++;
			}
		}
		return count <= 0 ? 0.0 : total / count;
	}

	private int countSubStatus(List<StudentSubCpmkRow> rows, boolean ok) {
		int count = 0;
		if (rows == null) {
			return count;
		}
		for (StudentSubCpmkRow row : rows) {
			if (row == null) {
				continue;
			}
			boolean rowOk = "L".equalsIgnoreCase(row.status) || "Lulus".equalsIgnoreCase(row.status)
					|| "Tercapai".equalsIgnoreCase(row.status);
			if (rowOk == ok) {
				count++;
			}
		}
		return count;
	}

	private int countOutcomeStatus(List<StudentOutcomeRow> rows, boolean ok) {
		int count = 0;
		if (rows == null) {
			return count;
		}
		for (StudentOutcomeRow row : rows) {
			if (row == null) {
				continue;
			}
			boolean rowOk = "L".equalsIgnoreCase(row.status) || "Lulus".equalsIgnoreCase(row.status)
					|| "Tercapai".equalsIgnoreCase(row.status);
			if (rowOk == ok) {
				count++;
			}
		}
		return count;
	}

	private int getScale4Local(double score) {
		if (score >= 80.0) {
			return 4;
		}
		if (score >= 70.0) {
			return 3;
		}
		if (score >= 60.0) {
			return 2;
		}
		return 1;
	}

	private double clampPercent(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return 0.0;
		}
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 100.0) {
			return 100.0;
		}
		return value;
	}

	private String formatSvg(double value) {
		return Common.numberFormat.get().format(value).replace(',', '.');
	}

	private void addColumn(Columns columns, String label, String width, String align) {
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(label);
		if (width != null) {
			column.setWidth(width);
		}
		if (align != null) {
			column.setAlign(align);
		}
	}

	private void appendText(Row row, String value, String tooltip) {
		Label label = new Label(value == null ? "" : value);
		label.setStyle("font-size:11px;color:#334155;white-space:pre-line;");
		if (tooltip != null && tooltip.length() > 0) {
			label.setTooltiptext(tooltip);
		}
		row.appendChild(label);
	}

	private void appendNumber(Row row, String value) {
		Label label = new Label(value == null ? "" : value);
		label.setStyle(STYLE_NUMBER);
		row.appendChild(label);
	}

	private void appendEmpty(Row row, int count) {
		for (int i = 0; i < count; i++) {
			row.appendChild(new Label());
		}
	}

	public void onSearchDefault(Event event) throws Exception {
		Common.clear(rowsUtama);
		if (mahasiswa == null) {
			return;
		}
		this.mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
		if (perkuliahan == null) {
			showWarning("Data perkuliahan tidak ditemukan.");
			return;
		}
		formatNilais = Common.getFormatNilais(perkuliahan, event != null);
		if (mhs == null) {
			showWarning("Silakan pilih mahasiswa terlebih dahulu.");
			return;
		}
		if (perkuliahan.getKurikulumPunyaMatakuliah() == null) {
			showWarning("Kurikulum OBE pada perkuliahan ini belum tersedia.");
			return;
		}
		refresh = event != null;
		initMk();
		initRinci();
		initRekapObeMahasiswa();
	}


	/**
	 * Dashboard tambahan di bawah tabel existing.
	 * Perhitungan dibuat mengikuti pola RekapHasilTugasPerTugasDanUjianObe,
	 * tetapi hanya untuk satu mahasiswa yang sedang dipilih.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initRekapObeMahasiswa() throws Exception {
		if (rowsUtama == null || perkuliahan == null || mhs == null || perkuliahan.getKurikulumPunyaMatakuliah() == null) {
			return;
		}

		MyGroupConfig group = new MyGroupConfig("Dashboard Rekap OBE Mahasiswa");
		rowsUtama.appendChild(group);

		Row row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setValign("top");
		row.setParent(rowsUtama);

		Vbox outer = new Vbox();
		outer.setWidth("100%");
		outer.setStyle(STYLE_CARD);
		outer.setParent(row);

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			StudentObeDashboardResult dashboard = calculateStudentObeDashboard(session);
			outer.appendChild(new Html(buildStudentObeDashboardHtml(dashboard)));
			// Rata-rata kelas (hanya untuk dosen/admin)
			String classAvgHtml = buildClassAveragesHtml();
			if (!classAvgHtml.isEmpty()) {
				outer.appendChild(new Html(classAvgHtml));
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			outer.appendChild(new Html("<div style='" + STYLE_INFO
					+ "'>Dashboard rekap OBE belum dapat ditampilkan: " + escapeHtml(e.getMessage()) + "</div>"));
		} finally {
			closeSessionQuietly(session);
		}
		// OBE Point 1 — Catatan Analisis Dosen per Mahasiswa
		// Editable for dosen/admin; read-only display for mahasiswa
		{
			String _catRole = (tbmuser == null || tbmuser.hakAkses() == null)
					? "" : (tbmuser.hakAkses().getRoleId() == null ? "" : tbmuser.hakAkses().getRoleId().trim());
			boolean _isMhsRole = "mhs".equalsIgnoreCase(_catRole) || "mahasiswa".equalsIgnoreCase(_catRole);
			Row catRow = new Row();
			ais.ui.util.ZkCompat.setSpans(catRow, "2");
			catRow.setParent(rowsUtama);
			Vbox vbCat = new Vbox();
			vbCat.setWidth("100%");
			vbCat.setStyle(STYLE_CARD);
			vbCat.setParent(catRow);
			new Html("<div style='font-weight:700;font-size:13px;color:#1e40af;"
					+ "margin-bottom:8px;'>Catatan Analisis Dosen untuk Mahasiswa Ini</div>").setParent(vbCat);
			String _existCat = "";
			if (perkuliahan.getCqiData() != null && !perkuliahan.getCqiData().isEmpty()) {
				try {
					org.json.JSONArray _jarr = new org.json.JSONArray(perkuliahan.getCqiData());
					for (int _ji = 0; _ji < _jarr.length(); _ji++) {
						org.json.JSONObject _je = _jarr.optJSONObject(_ji);
						if (_je != null && "__meta__".equals(_je.optString("cpmk", ""))) {
							org.json.JSONObject _jmc = new org.json.JSONObject(_je.optString("mhsCatatan", "{}"));
							_existCat = _jmc.optString(String.valueOf(mhs.getId()), "");
							break;
						}
					}
				} catch (Exception _jign) { ais.common.ErrorAuditUtil.record(_jign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:2228");}
			}
			if (!_isMhsRole) {
				final org.zkoss.zul.Textbox _tbMhsCat = new org.zkoss.zul.Textbox(_existCat);
				_tbMhsCat.setRows(4); _tbMhsCat.setWidth("100%"); _tbMhsCat.setMultiline(true);
				_tbMhsCat.setTooltiptext("Catatan/analisis pembelajaran untuk mahasiswa ini (tidak tampil ke mahasiswa jika kosong)");
				_tbMhsCat.setParent(vbCat);
				org.zkoss.zul.Button _btnMhsCat = new org.zkoss.zul.Button("Simpan Catatan");
				_btnMhsCat.setStyle("margin-top:6px;");
				_btnMhsCat.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
					public void onEvent(org.zkoss.zk.ui.event.Event _ev) throws Exception {
						saveMhsCatatanForStudent(mhs.getId(), _tbMhsCat.getValue());
					}
				});
				_btnMhsCat.setParent(vbCat);
			} else if (!_existCat.isEmpty()) {
				new Html("<div style='background:#f0f9ff;border:1px solid #bae6fd;padding:10px;"
						+ "border-radius:6px;white-space:pre-wrap;font-size:13px;'>"
						+ escapeHtml(_existCat) + "</div>").setParent(vbCat);
			} else {
				new Html("<div style='color:#94a3b8;font-style:italic;padding:8px;'>"
						+ "Belum ada catatan dari dosen untuk mahasiswa ini.</div>").setParent(vbCat);
			}
		}
	}

	private void saveMhsCatatanForStudent(Long mhsId, String catatan) {
		if (perkuliahan == null || mhsId == null) return;
		try {
			org.json.JSONArray result = new org.json.JSONArray();
			org.json.JSONObject meta = new org.json.JSONObject();
			if (perkuliahan.getCqiData() != null && !perkuliahan.getCqiData().isEmpty()) {
				try {
					org.json.JSONArray existing = new org.json.JSONArray(perkuliahan.getCqiData());
					for (int i = 0; i < existing.length(); i++) {
						org.json.JSONObject e = existing.optJSONObject(i);
						if (e == null) continue;
						if ("__meta__".equals(e.optString("cpmk", ""))) {
							meta = e;
						} else {
							result.put(e);
						}
					}
				} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:2271");}
			}
			org.json.JSONObject mhsCat = new org.json.JSONObject();
			try { mhsCat = new org.json.JSONObject(meta.optString("mhsCatatan", "{}")); } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:2274");}
			if (catatan == null || catatan.trim().isEmpty()) {
				mhsCat.remove(String.valueOf(mhsId));
			} else {
				mhsCat.put(String.valueOf(mhsId), catatan.trim());
			}
			meta.put("mhsCatatan", mhsCat.toString());
			if (!meta.has("cpmk")) meta.put("cpmk", "__meta__");
			result.put(meta);
			perkuliahan.setCqiData(result.toString());
			Common.refreshSaveOrUpdate(perkuliahan);
			MyMessageboxConfig.show("Catatan berhasil disimpan",
					"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private StudentObeDashboardResult calculateStudentObeDashboard(Session session) throws Exception {
		StudentObeDashboardResult result = new StudentObeDashboardResult();
		result.namaMahasiswa = mhs == null ? "" : safeText(mhs.getNim()) + " - " + safeText(mhs.getNama());
		KurikulumPunyaMatakuliah kpm = perkuliahan == null ? null : perkuliahan.getKurikulumPunyaMatakuliah();
		Matakuliah mk = perkuliahan == null ? null : perkuliahan.getMatakuliah();
		if (kpm == null || mk == null || mhs == null) {
			return result;
		}
		result.targetMinimal = safeDouble(kpm.getMinimalKetercapaian(), 60.0);
		if (formatNilais == null) {
			formatNilais = Common.getFormatNilais(perkuliahan, false);
		}
		if (formatNilais == null) {
			formatNilais = new ArrayList<FormatNilai>();
		}

		List<CapaianPembelajaranLulusan> listCpmkUnique = loadCpmkListForDashboard(session, mk);
		Map<Long, List<FormatNilai>> mapCpmkIdToFns = new HashMap<Long, List<FormatNilai>>();
		for (FormatNilai fn : formatNilais) {
			if (fn == null || fn.getId() == null || fn.getCapaianPembelajaranLulusan() == null
					|| fn.getCapaianPembelajaranLulusan().getId() == null) {
				continue;
			}
			Long cpmkId = fn.getCapaianPembelajaranLulusan().getId();
			List<FormatNilai> fns = mapCpmkIdToFns.get(cpmkId);
			if (fns == null) {
				fns = new ArrayList<FormatNilai>();
				mapCpmkIdToFns.put(cpmkId, fns);
			}
			fns.add(fn);
		}

		List<CapaianLulusan> listCpl = loadCplListForDashboard(session, mk);
		List<ProfilLulusan> listPl = loadProfilListForDashboard(session, listCpl);

		Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
		Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
		Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
		Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
		Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
		Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();

		List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(
				session.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p")
						.add(Restrictions.eq("p.perkuliahan", perkuliahan)),
				PertemuanPunyaUjian.class);

		List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(
				session.createCriteria(Pertemuan.class).add(Restrictions.eq("perkuliahan", perkuliahan))
						.add(Restrictions.isNotNull("judultugas")),
				Pertemuan.class);

		Collection<Long> pertemuansList = perkuliahan.ambilPertemuan() == null ? new ArrayList<Long>()
				: perkuliahan.ambilPertemuan().values();
		List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>()
				: ConstantValues.simpleList(session.createCriteria(TugasPertemuan.class)
						.add(Restrictions.in("pertemuan", pertemuansList)), TugasPertemuan.class);
		List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(
				session.createCriteria(TugasKelompok.class).add(Restrictions.eq("perkuliahan", perkuliahan)),
				TugasKelompok.class);

		if (pertemuanPunyaUjians != null && !pertemuanPunyaUjians.isEmpty()) {
			List<HasilUjianMahasiswa> listHasil = ConstantValues.simpleList(
					session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
							.add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians))
							.add(Restrictions.eq("mahasiswa", mhs)),
					HasilUjianMahasiswa.class);
			for (HasilUjianMahasiswa hum : listHasil) {
				if (hum == null || hum.getNilaiObe() == null || hum.getPertemuanPunyaUjian() == null) {
					continue;
				}
				try {
					JSONObject j = new JSONObject(hum.getNilaiObe());
					for (FormatNilai fn : formatNilais) {
						if (fn == null || fn.getId() == null || j.isNull(fn.getId().toString())) {
							continue;
						}
						addMapType(mapPertemuanPunyaUjian, fn.getId(), hum.getPertemuanPunyaUjian().getId(),
								hum.getPertemuanPunyaUjian());
						Double skor = Double.valueOf(j.getDouble(fn.getId().toString()));
						Double max = j.isNull(fn.getId() + "_max") ? Double.valueOf(0.0)
								: Double.valueOf(j.getDouble(fn.getId() + "_max"));
						Double didapat = max.doubleValue() == 0.0 ? Double.valueOf(0.0)
								: Double.valueOf((skor.doubleValue() * 100.0) / max.doubleValue());
						addNilaiUtama(dataNiliasUtama, mhs.getId(), fn.getId(),
								PertemuanPunyaUjian.class.getName() + "_" + hum.getPertemuanPunyaUjian().getId(), didapat);
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		parseTugasJSONForStudent(pertemuansTugas, formatNilais, mhs, dataNiliasUtama, mapTugas,
				Pertemuan.class.getName());
		parseTugasJSONForStudent(pertemuansTugasLanjut, formatNilais, mhs, dataNiliasUtama, mapTugasLanjut,
				TugasPertemuan.class.getName());
		parseTugasJSONForStudent(pertemuansTugasKelompoks, formatNilais, mhs, dataNiliasUtama, mapTugasKelompok,
				TugasKelompok.class.getName());

		for (FormatNilai fn : formatNilais) {
			if (fn == null || fn.getId() == null) {
				continue;
			}
			Map<Long, PertemuanPunyaUjian> md = mapPertemuanPunyaUjian.get(fn.getId());
			Map<Long, Pertemuan> mt = mapTugas.get(fn.getId());
			Map<Long, TugasPertemuan> mtl = mapTugasLanjut.get(fn.getId());
			Map<Long, TugasKelompok> mtk = mapTugasKelompok.get(fn.getId());
			if (md != null) {
				for (PertemuanPunyaUjian p : md.values()) {
					extractBobotObe(p.getFormatNilais(), fn.getId(), PertemuanPunyaUjian.class.getName() + "_" + p.getId(), dataBobot);
				}
			}
			if (mt != null) {
				for (Pertemuan p : mt.values()) {
					extractBobotObe(p.getFormatNilais(), fn.getId(), Pertemuan.class.getName() + "_" + p.getId(), dataBobot);
				}
			}
			if (mtl != null) {
				for (TugasPertemuan p : mtl.values()) {
					extractBobotObe(p.getFormatNilais(), fn.getId(), TugasPertemuan.class.getName() + "_" + p.getId(), dataBobot);
				}
			}
			if (mtk != null) {
				for (TugasKelompok p : mtk.values()) {
					extractBobotObe(p.getFormatNilais(), fn.getId(), TugasKelompok.class.getName() + "_" + p.getId(), dataBobot);
				}
			}
		}

		Map<String, Double> persensData = new HashMap<String, Double>();
		for (FormatNilai fn : formatNilais) {
			if (fn == null || fn.getId() == null) {
				continue;
			}
			Map<Long, PertemuanPunyaUjian> md = mapPertemuanPunyaUjian.get(fn.getId());
			Map<Long, Pertemuan> mt = mapTugas.get(fn.getId());
			Map<Long, TugasPertemuan> mtl = mapTugasLanjut.get(fn.getId());
			Map<Long, TugasKelompok> mtk = mapTugasKelompok.get(fn.getId());
			if (md != null) {
				for (PertemuanPunyaUjian p : md.values()) {
					hitungPersenPerKomponenObe(PertemuanPunyaUjian.class.getName(), p.getId(), fn, dataBobot, persensData);
				}
			}
			if (mt != null) {
				for (Pertemuan p : mt.values()) {
					hitungPersenPerKomponenObe(Pertemuan.class.getName(), p.getId(), fn, dataBobot, persensData);
				}
			}
			if (mtl != null) {
				for (TugasPertemuan p : mtl.values()) {
					hitungPersenPerKomponenObe(TugasPertemuan.class.getName(), p.getId(), fn, dataBobot, persensData);
				}
			}
			if (mtk != null) {
				for (TugasKelompok p : mtk.values()) {
					hitungPersenPerKomponenObe(TugasKelompok.class.getName(), p.getId(), fn, dataBobot, persensData);
				}
			}
		}

		Map<Long, Double> skorFormatTotalMap = new HashMap<Long, Double>();
		Map<Long, Double> skorFormatBobotMap = new HashMap<Long, Double>();
		for (FormatNilai fn : formatNilais) {
			if (fn == null || fn.getId() == null) {
				continue;
			}
			Map<Long, PertemuanPunyaUjian> md = mapPertemuanPunyaUjian.get(fn.getId());
			Map<Long, Pertemuan> mt = mapTugas.get(fn.getId());
			Map<Long, TugasPertemuan> mtl = mapTugasLanjut.get(fn.getId());
			Map<Long, TugasKelompok> mtk = mapTugasKelompok.get(fn.getId());
			if (md == null && mt == null && mtl == null && mtk == null) {
				continue;
			}
			appendAssessmentRowsForStudent(result, fn, md, mt, mtl, mtk, dataNiliasUtama, persensData, mhs.getId());
			Double totalNilaiPerFormat = recalculateTotalPerFormatObe(dataNiliasUtama, persensData, mhs.getId(), fn.getId(), md,
					mt, mtl, mtk);
			double bobotFormatFraction = normalizePercentFraction(fn.getPersen());
			result.totalNilaiBobotAkhir += totalNilaiPerFormat.doubleValue();
			result.totalBobotFormatAkhir += bobotFormatFraction;
			skorFormatTotalMap.put(fn.getId(), totalNilaiPerFormat);
			skorFormatBobotMap.put(fn.getId(), Double.valueOf(bobotFormatFraction));

			StudentSubCpmkRow sub = new StudentSubCpmkRow();
			sub.kode = safeText(fn.getKodeSubCpmk());
			sub.nama = safeText(fn.getNama());
			sub.cpmkKode = fn.getCapaianPembelajaranLulusan() == null ? "" : safeText(fn.getCapaianPembelajaranLulusan().getKode());
			sub.cpmkNama = fn.getCapaianPembelajaranLulusan() == null ? "" : safeText(fn.getCapaianPembelajaranLulusan().getNama());
			sub.bobot = bobotFormatFraction * 100.0;
			sub.total = totalNilaiPerFormat.doubleValue();
			sub.konversi = hitungKonversiObe(fn, totalNilaiPerFormat).doubleValue();
			sub.status = sub.konversi >= safeDouble(fn.ambilMinimal(), result.targetMinimal) ? "L" : "TL";
			result.subCpmkRows.add(sub);
		}

		result.nilaiAkhir = result.totalBobotFormatAkhir > 0.0 ? result.totalNilaiBobotAkhir / result.totalBobotFormatAkhir : 0.0;
		result.nilaiHuruf = hitungHurufObeSingle(result.nilaiAkhir);
		result.statusAkhir = result.nilaiAkhir >= result.targetMinimal ? "Lulus" : "Tidak Lulus";
		result.hasData = result.totalBobotFormatAkhir > 0.0 || !result.subCpmkRows.isEmpty();

		Map<Long, Double> sCpmkTotal = new HashMap<Long, Double>();
		Map<Long, Double> sCpmkTarget = new HashMap<Long, Double>();
		for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
			if (cpmk == null || cpmk.getId() == null) {
				continue;
			}
			Double totalCpmk = Double.valueOf(0.0);
			Double targetCpmk = Double.valueOf(0.0);
			List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
			if (fns != null) {
				for (FormatNilai fn : fns) {
					Double t = skorFormatTotalMap.get(fn.getId());
					Double b = skorFormatBobotMap.get(fn.getId());
					totalCpmk += t == null ? 0.0 : t.doubleValue();
					targetCpmk += b == null ? 0.0 : b.doubleValue();
				}
			}
			double valCpmk = targetCpmk.doubleValue() > 0.0 ? totalCpmk.doubleValue() / targetCpmk.doubleValue() : 0.0;
			sCpmkTotal.put(cpmk.getId(), totalCpmk);
			sCpmkTarget.put(cpmk.getId(), targetCpmk);
			StudentOutcomeRow row = new StudentOutcomeRow();
			row.kode = safeText(cpmk.getKode());
			row.nama = safeText(cpmk.getNama());
			row.bobot = targetCpmk.doubleValue() * 100.0;
			row.total = totalCpmk.doubleValue();
			row.nilai = valCpmk;
			row.status = row.nilai >= result.targetMinimal ? "Tercapai" : "Belum Tercapai";
			if (targetCpmk.doubleValue() > 0.0 || totalCpmk.doubleValue() > 0.0) {
				result.cpmkRows.add(row);
			}
		}

		Map<Long, Double> sCplTotal = new HashMap<Long, Double>();
		Map<Long, Double> sCplTarget = new HashMap<Long, Double>();
		for (CapaianLulusan cpl : listCpl) {
			if (cpl == null || cpl.getId() == null) {
				continue;
			}
			Double totalCpl = Double.valueOf(0.0);
			Double targetCpl = Double.valueOf(0.0);
			String mappedCpmk = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
			for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
				if (cpmk != null && containsIdInCsv(mappedCpmk, cpmk.getId())) {
					double relWeight = normalizeRelationWeight(cpmk.getBobot());
					Double totalCpmk = sCpmkTotal.get(cpmk.getId());
					Double targetCpmk = sCpmkTarget.get(cpmk.getId());
					totalCpl += (totalCpmk == null ? 0.0 : totalCpmk.doubleValue()) * relWeight;
					targetCpl += (targetCpmk == null ? 0.0 : targetCpmk.doubleValue()) * relWeight;
				}
			}
			double valCpl = targetCpl.doubleValue() > 0.0 ? totalCpl.doubleValue() / targetCpl.doubleValue() : 0.0;
			sCplTotal.put(cpl.getId(), totalCpl);
			sCplTarget.put(cpl.getId(), targetCpl);
			StudentOutcomeRow row = new StudentOutcomeRow();
			row.kode = safeText(cpl.getKode());
			row.nama = safeText(cpl.getNama());
			row.bobot = targetCpl.doubleValue() * 100.0;
			row.total = totalCpl.doubleValue();
			row.nilai = valCpl;
			row.status = row.nilai >= result.targetMinimal ? "Tercapai" : "Belum Tercapai";
			if (targetCpl.doubleValue() > 0.0 || totalCpl.doubleValue() > 0.0) {
				result.cplRows.add(row);
			}
		}

		for (ProfilLulusan pl : listPl) {
			if (pl == null || pl.getId() == null) {
				continue;
			}
			Double totalPl = Double.valueOf(0.0);
			Double targetPl = Double.valueOf(0.0);
			for (CapaianLulusan cpl : listCpl) {
				if (cpl != null && containsIdInCsv(cpl.getProfil(), pl.getId())) {
					Double totalCpl = sCplTotal.get(cpl.getId());
					Double targetCpl = sCplTarget.get(cpl.getId());
					totalPl += totalCpl == null ? 0.0 : totalCpl.doubleValue();
					targetPl += targetCpl == null ? 0.0 : targetCpl.doubleValue();
				}
			}
			double valPl = targetPl.doubleValue() > 0.0 ? totalPl.doubleValue() / targetPl.doubleValue() : 0.0;
			StudentOutcomeRow row = new StudentOutcomeRow();
			row.kode = safeText(pl.getKode());
			row.nama = safeText(pl.getNama());
			row.bobot = targetPl.doubleValue() * 100.0;
			row.total = totalPl.doubleValue();
			row.nilai = valPl;
			row.status = row.nilai >= result.targetMinimal ? "Tercapai" : "Belum Tercapai";
			if (targetPl.doubleValue() > 0.0 || totalPl.doubleValue() > 0.0) {
				result.plRows.add(row);
			}
		}
		return result;
	}

	private List<CapaianPembelajaranLulusan> loadCpmkListForDashboard(Session session, Matakuliah mk) {
		List<CapaianPembelajaranLulusan> result = new ArrayList<CapaianPembelajaranLulusan>();
		if (session == null || mk == null) {
			return result;
		}
		try {
			Set<Long> cpmkIds = parseIdsToSet(mk.getCapaianPembelajaranLulusan());
			if (!cpmkIds.isEmpty()) {
				result = ConstantValues.simpleList(session.createCriteria(CapaianPembelajaranLulusan.class)
						.add(Restrictions.in("id", cpmkIds)).addOrder(Order.asc("kode")),
						CapaianPembelajaranLulusan.class);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return result;
	}

	private List<CapaianLulusan> loadCplListForDashboard(Session session, Matakuliah mk) {
		List<CapaianLulusan> result = new ArrayList<CapaianLulusan>();
		if (session == null || mk == null) {
			return result;
		}
		try {
			Set<Long> cplIds = parseIdsToSet(mk.getCapaianLulusan());
			if (!cplIds.isEmpty()) {
				result = ConstantValues.simpleList(session.createCriteria(CapaianLulusan.class).add(Restrictions.in("id", cplIds))
						.addOrder(Order.asc("kode")), CapaianLulusan.class);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return result;
	}

	private List<ProfilLulusan> loadProfilListForDashboard(Session session, List<CapaianLulusan> listCpl) {
		List<ProfilLulusan> result = new ArrayList<ProfilLulusan>();
		if (session == null || listCpl == null || listCpl.isEmpty()) {
			return result;
		}
		try {
			Set<Long> plIds = new HashSet<Long>();
			for (CapaianLulusan cpl : listCpl) {
				if (cpl != null) {
					plIds.addAll(parseIdsToSet(cpl.getProfil()));
				}
			}
			if (!plIds.isEmpty()) {
				result = ConstantValues.simpleList(session.createCriteria(ProfilLulusan.class).add(Restrictions.in("id", plIds))
						.addOrder(Order.asc("kode")), ProfilLulusan.class);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return result;
	}

	private String buildStudentObeDashboardHtml(StudentObeDashboardResult data) {
		StringBuilder sb = new StringBuilder();
		if (data == null || !data.hasData) {
			return "<div style='" + STYLE_INFO + "'>Belum ada data asesmen OBE yang dapat direkap untuk mahasiswa terpilih.</div>";
		}
		String statusColor = "Lulus".equals(data.statusAkhir) ? "#166534" : "#b91c1c";
		sb.append("<div style='padding:4px 2px 12px 2px;'>")
				.append("<div style='font-size:11px;text-transform:uppercase;letter-spacing:.08em;color:#7c3aed;font-weight:900;'>Rekap Hasil Sub-CPMK, CPMK, CPL, dan PL</div>")
				.append("<div style='font-size:16px;font-weight:900;color:#0f172a;margin-top:4px;'>")
				.append(escapeHtml(data.namaMahasiswa)).append("</div>")
				.append("<div style='font-size:12px;color:#64748b;margin-top:4px;'>Mengikuti pola perhitungan rekap OBE seluruh mahasiswa, tetapi difilter untuk satu mahasiswa terpilih.</div>")
				.append("</div>");
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;margin-bottom:12px;'>");
		sb.append(buildMiniCard("Total Bobot Target", Common.numberFormat.get().format(data.totalBobotFormatAkhir * 100.0) + "%",
				"Bobot format penilaian"));
		sb.append(buildMiniCard("Total Nilai Berbobot", Common.numberFormat.get().format(data.totalNilaiBobotAkhir),
				"Σ nilai x bobot"));
		sb.append(buildMiniCard("Nilai Akhir OBE", Common.numberFormat.get().format(data.nilaiAkhir), data.nilaiHuruf));
		sb.append("<div style='padding:12px;border-radius:14px;background:#ffffff;border:1px solid #e5e7eb;'>")
				.append("<div style='font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;'>Status</div>")
				.append("<div style='font-size:20px;font-weight:900;color:").append(statusColor).append(";margin-top:4px;'>")
				.append(escapeHtml(data.statusAkhir)).append("</div>")
				.append("<div style='font-size:11px;color:#64748b;margin-top:4px;'>Target minimal ")
				.append(Common.numberFormat.get().format(data.targetMinimal)).append("</div></div>");
		sb.append("</div>");

		// Tambahan V33: overview visual/grafik ringan untuk satu mahasiswa.
		// Tidak mengubah tabel existing, hanya menambahkan dashboard di atas rekap rinci.
		sb.append(buildStudentObeChartsHtml(data));

		sb.append(buildAssessmentHtmlTable("Rekap Komponen Ujian/Tugas per Sub-CPMK", data.assessmentRows));
		sb.append(buildSubCpmkHtmlTable("Rekap Sub-CPMK / Format Nilai", data.subCpmkRows));
		sb.append(buildOutcomeHtmlTable("Rekap CPMK", data.cpmkRows));
		sb.append(buildOutcomeHtmlTable("Rekap CPL", data.cplRows));
		sb.append(buildOutcomeHtmlTable("Rekap Profil Lulusan (PL)", data.plRows));
		return sb.toString();
	}


	private String buildAssessmentHtmlTable(String title, List<StudentAssessmentRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:12px;border:1px solid #e5e7eb;border-radius:14px;overflow:auto;background:#ffffff;'>")
				.append("<div style='padding:10px 12px;background:#f8fafc;border-bottom:1px solid #e5e7eb;font-weight:900;color:#0f172a;'>")
				.append(escapeHtml(title)).append("</div>")
				.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>")
				.append("<tr style='background:#f1f5f9;color:#334155;'>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>Sub-CPMK</th>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>Komponen Ujian/Tugas</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Nilai</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Bobot Komponen</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Nilai Berbobot</th>")
				.append("<th style='padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;'>Status</th></tr>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<tr><td colspan='6' style='padding:10px;color:#64748b;'>Tidak ada komponen ujian/tugas yang memiliki nilai OBE.</td></tr>");
		} else {
			for (StudentAssessmentRow r : rows) {
				sb.append("<tr>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;vertical-align:top;'><b>")
						.append(escapeHtml(r.subCpmkKode)).append("</b><br/><span style='color:#64748b;'>")
						.append(escapeHtml(r.cpmkKode)).append("</span></td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;vertical-align:top;'><b>")
						.append(escapeHtml(r.namaKomponen)).append("</b><br/><span style='color:#64748b;'>")
						.append(escapeHtml(r.jenisKomponen)).append("</span></td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.nilai)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.bobotKomponen * 100.0)).append("%</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;font-weight:900;color:#0f172a;'>")
						.append(Common.numberFormat.get().format(r.nilaiBerbobot)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:center;'>")
						.append(buildStatusBadge(r.status)).append("</td></tr>");
			}
		}
		sb.append("</table></div>");
		return sb.toString();
	}

	private String buildSubCpmkHtmlTable(String title, List<StudentSubCpmkRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:12px;border:1px solid #e5e7eb;border-radius:14px;overflow:auto;background:#ffffff;'>")
				.append("<div style='padding:10px 12px;background:#f8fafc;border-bottom:1px solid #e5e7eb;font-weight:900;color:#0f172a;'>")
				.append(escapeHtml(title)).append("</div>")
				.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>")
				.append("<tr style='background:#f1f5f9;color:#334155;'>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>CPMK</th>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>Sub-CPMK / Format</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Bobot</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Total</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Konversi</th>")
				.append("<th style='padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;'>Status</th></tr>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<tr><td colspan='6' style='padding:10px;color:#64748b;'>Tidak ada data Sub-CPMK yang memiliki asesmen.</td></tr>");
		} else {
			for (StudentSubCpmkRow r : rows) {
				sb.append("<tr>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;vertical-align:top;'><b>")
						.append(escapeHtml(r.cpmkKode)).append("</b><br/><span style='color:#64748b;'>")
						.append(escapeHtml(r.cpmkNama)).append("</span></td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;vertical-align:top;'><b>")
						.append(escapeHtml(r.kode)).append("</b><br/><span style='color:#64748b;'>")
						.append(escapeHtml(r.nama)).append("</span></td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.bobot)).append("%</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.total)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;font-weight:900;color:#0f172a;'>")
						.append(Common.numberFormat.get().format(r.konversi)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:center;'>")
						.append(buildStatusBadge(r.status)).append("</td></tr>");
			}
		}
		sb.append("</table></div>");
		return sb.toString();
	}

	private String buildOutcomeHtmlTable(String title, List<StudentOutcomeRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:12px;border:1px solid #e5e7eb;border-radius:14px;overflow:auto;background:#ffffff;'>")
				.append("<div style='padding:10px 12px;background:#f8fafc;border-bottom:1px solid #e5e7eb;font-weight:900;color:#0f172a;'>")
				.append(escapeHtml(title)).append("</div>")
				.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>")
				.append("<tr style='background:#f1f5f9;color:#334155;'>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>Kode</th>")
				.append("<th style='padding:8px;text-align:left;border-bottom:1px solid #e5e7eb;'>Nama</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Bobot Target</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Total Berbobot</th>")
				.append("<th style='padding:8px;text-align:right;border-bottom:1px solid #e5e7eb;'>Nilai Konversi</th>")
				.append("<th style='padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;'>Status</th></tr>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<tr><td colspan='6' style='padding:10px;color:#64748b;'>Tidak ada data yang terpetakan.</td></tr>");
		} else {
			for (StudentOutcomeRow r : rows) {
				sb.append("<tr>").append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;font-weight:900;'>")
						.append(escapeHtml(r.kode)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;color:#475569;'>")
						.append(escapeHtml(r.nama)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.bobot)).append("%</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;'>")
						.append(Common.numberFormat.get().format(r.total)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:right;font-weight:900;color:#0f172a;'>")
						.append(Common.numberFormat.get().format(r.nilai)).append("</td>")
						.append("<td style='padding:8px;border-bottom:1px solid #f1f5f9;text-align:center;'>")
						.append(buildStatusBadge(r.status)).append("</td></tr>");
			}
		}
		sb.append("</table></div>");
		return sb.toString();
	}

	private String buildStatusBadge(String status) {
		String s = status == null ? "" : status;
		boolean ok = "L".equalsIgnoreCase(s) || "Lulus".equalsIgnoreCase(s) || "Tercapai".equalsIgnoreCase(s);
		String bg = ok ? "#dcfce7" : "#fee2e2";
		String fg = ok ? "#166534" : "#991b1b";
		return "<span style='display:inline-block;padding:4px 9px;border-radius:999px;background:" + bg + ";color:" + fg
				+ ";font-weight:900;'>" + escapeHtml(s) + "</span>";
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private String buildClassAveragesHtml() {
		if (perkuliahan == null) return "";
		// Only show for non-mahasiswa (dosen/admin view)
		if (tbmuser != null && tbmuser.getMahasiswa() != null) return "";

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border:1px solid #e5e7eb;border-radius:14px;padding:14px;"
				+ "margin:10px 0;box-shadow:0 4px 14px rgba(15,23,42,.05);font-family:system-ui,sans-serif;'>");
		sb.append("<div style='font-size:13px;font-weight:800;color:#0f172a;margin-bottom:10px;'>"
				+ "&#128200; Rata-rata Kelas — Sub-CPMK / CPMK / CPL / PL</div>");
		sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:12px;padding:8px 12px;"
				+ "background:#f8fafc;border-radius:8px;border:1px solid #e2e8f0;'>"
				+ "Nilai rata-rata seluruh mahasiswa aktif di kelas ini untuk setiap tingkatan OBE.</div>");

		Session session = null;
		Mahasiswa savedMhs = this.mhs;
		try {
			session = HibernateUtil.openSession();

			java.util.Collection<Long> dpIds = perkuliahan.ambilDetailperkuliahan(null, null, "",
					!Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim"), false);

			// Aggregate per-level totals: kode -> [total, count]
			java.util.Map<String, double[]> subCpmkSum = new java.util.LinkedHashMap<String, double[]>();
			java.util.Map<String, double[]> cpmkSum    = new java.util.LinkedHashMap<String, double[]>();
			java.util.Map<String, double[]> cplSum     = new java.util.LinkedHashMap<String, double[]>();
			java.util.Map<String, double[]> plSum      = new java.util.LinkedHashMap<String, double[]>();
			int studentCount = 0;

			if (dpIds != null) {
				for (Long dpId : dpIds) {
					if (dpId == null) continue;
					Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(
							Detailperkuliahan.class, dpId.toString());
					if (dp == null || dp.getMahasiswa() == null
							|| !Detailperkuliahan.DISETUJUI.equals(dp.getPersetujuan())) continue;

					try {
						this.mhs = dp.getMahasiswa();
						StudentObeDashboardResult res = calculateStudentObeDashboard(session);
						if (!res.hasData) continue;
						studentCount++;

						for (StudentSubCpmkRow row : res.subCpmkRows) {
							String key = (row.kode == null || row.kode.isEmpty()) ? row.nama : row.kode;
							if (key == null || key.isEmpty()) continue;
							double[] arr = subCpmkSum.containsKey(key) ? subCpmkSum.get(key) : new double[]{0.0, 0.0};
							arr[0] += row.konversi; arr[1] += 1;
							subCpmkSum.put(key, arr);
						}
						for (StudentOutcomeRow row : res.cpmkRows) {
							String key = row.kode;
							if (key == null || key.isEmpty()) continue;
							double[] arr = cpmkSum.containsKey(key) ? cpmkSum.get(key) : new double[]{0.0, 0.0};
							arr[0] += row.nilai; arr[1] += 1;
							cpmkSum.put(key, arr);
						}
						for (StudentOutcomeRow row : res.cplRows) {
							String key = row.kode;
							if (key == null || key.isEmpty()) continue;
							double[] arr = cplSum.containsKey(key) ? cplSum.get(key) : new double[]{0.0, 0.0};
							arr[0] += row.nilai; arr[1] += 1;
							cplSum.put(key, arr);
						}
						for (StudentOutcomeRow row : res.plRows) {
							String key = row.kode;
							if (key == null || key.isEmpty()) continue;
							double[] arr = plSum.containsKey(key) ? plSum.get(key) : new double[]{0.0, 0.0};
							arr[0] += row.nilai; arr[1] += 1;
							plSum.put(key, arr);
						}
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:2875");}
				}
			}

			if (studentCount == 0) {
				sb.append("<div style='color:#94a3b8;padding:10px;font-size:12px;'>"
						+ "Belum ada mahasiswa aktif dengan data OBE di kelas ini.</div>");
				sb.append("</div>");
				return sb.toString();
			}

			sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>"
					+ "Dihitung dari <b>" + studentCount + " mahasiswa</b> aktif.</div>");

			// CQI summary indicator
			String cqiRawAvg = perkuliahan.getCqiData();
			if (!cqiRawAvg.isEmpty()) {
				try {
					JSONArray cqiArrAvg = new JSONArray(cqiRawAvg);
					int cqiFilled = 0; int cqiTotal = 0;
					String admGlSt = "";
					for (int ci = 0; ci < cqiArrAvg.length(); ci++) {
						JSONObject ce = cqiArrAvg.optJSONObject(ci);
						if (ce == null) continue;
						if ("__meta__".equals(ce.optString("cpmk",""))) {
							admGlSt = ce.optString("adminGlobalStatus","");
						} else {
							cqiTotal++;
							if (!ce.optString("masalah","").isEmpty() || !ce.optString("analisis","").isEmpty()) cqiFilled++;
						}
					}
					String cqiBadgeBg = cqiFilled >= cqiTotal && cqiTotal > 0 ? "#dcfce7" : cqiFilled > 0 ? "#fef9c3" : "#f1f5f9";
					String cqiBadgeTx = cqiFilled >= cqiTotal && cqiTotal > 0 ? "#166534" : cqiFilled > 0 ? "#713f12" : "#64748b";
					sb.append("<div style='display:flex;gap:8px;margin-bottom:10px;flex-wrap:wrap;'>");
					sb.append("<span style='font-size:10px;font-weight:700;padding:3px 10px;border-radius:20px;background:")
					  .append(cqiBadgeBg).append(";color:").append(cqiBadgeTx).append(";'>")
					  .append("&#128295; CQI: ").append(cqiFilled).append("/").append(cqiTotal).append(" CPMK dianalisis</span>");
					if (!admGlSt.isEmpty()) {
						String stBg = "Disetujui".equals(admGlSt) ? "#dcfce7" : "Revisi".equals(admGlSt) ? "#fee2e2" : "#fef9c3";
						String stTx = "Disetujui".equals(admGlSt) ? "#166534" : "Revisi".equals(admGlSt) ? "#991b1b" : "#78350f";
						sb.append("<span style='font-size:10px;font-weight:700;padding:3px 10px;border-radius:20px;background:")
						  .append(stBg).append(";color:").append(stTx).append(";'>")
						  .append("&#10003; Admin: ").append(escapeHtml(admGlSt)).append("</span>");
					}
					sb.append("</div>");
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/NilaiObeAction.java:2920");}
			}

			KurikulumPunyaMatakuliah kpm = perkuliahan.getKurikulumPunyaMatakuliah();
			double target = kpm != null && kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 75.0;

			sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin-top:8px;'>");
			sb.append(buildAvgCard("Sub-CPMK", subCpmkSum, target, "#eff6ff", "#1d4ed8"));
			sb.append(buildAvgCard("CPMK", cpmkSum, target, "#f0fdf4", "#166534"));
			sb.append(buildAvgCard("CPL", cplSum, target, "#fdf4ff", "#7c3aed"));
			sb.append(buildAvgCard("Profil Lulusan (PL)", plSum, target, "#fff7ed", "#c2410c"));
			sb.append("</div>");

		} catch (Exception e) {
			sb.append("<div style='color:#ef4444;padding:10px;font-size:11px;'>Error menghitung rata-rata kelas: "
					+ escapeHtml(e.getMessage()) + "</div>");
		} finally {
			this.mhs = savedMhs;
			closeSessionQuietly(session);
		}

		sb.append("</div>");
		return sb.toString();
	}

	private String buildAvgCard(String title, java.util.Map<String, double[]> sumMap,
			double target, String bg, String accent) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:").append(bg).append(";border-radius:12px;padding:12px;'>");
		sb.append("<div style='font-size:11px;font-weight:800;color:").append(accent)
		  .append(";margin-bottom:8px;'>").append(escapeHtml(title)).append("</div>");
		if (sumMap == null || sumMap.isEmpty()) {
			sb.append("<div style='font-size:11px;color:#94a3b8;'>Belum ada data.</div>");
		} else {
			sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
			for (java.util.Map.Entry<String, double[]> entry : sumMap.entrySet()) {
				double[] val = entry.getValue();
				double avg = val != null && val[1] > 0 ? val[0] / val[1] : 0.0;
				boolean lulus = avg >= target;
				int barWidth = (int) Math.min(100.0, avg);
				String barColor = lulus ? "#16a34a" : "#dc2626";
				String bar = "<div style='height:5px;border-radius:3px;background:#e2e8f0;'>"
						+ "<div style='height:5px;border-radius:3px;background:" + barColor
						+ ";width:" + barWidth + "%;'></div></div>";
				sb.append("<tr><td style='padding:2px 0;color:#334155;font-weight:600;width:45%;'>")
				  .append(escapeHtml(entry.getKey())).append("</td>")
				  .append("<td style='padding:2px 4px;font-weight:700;text-align:right;color:")
				  .append(lulus ? "#166534" : "#991b1b").append(";'>")
				  .append(String.format("%.1f", avg)).append("</td></tr>")
				  .append("<tr><td colspan='2' style='padding-bottom:4px;'>").append(bar).append("</td></tr>");
			}
			sb.append("</table>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String hitungHurufObeSingle(Double hasilKonversi) {
		try {
			if (perkuliahan == null || perkuliahan.getMatakuliah() == null) {
				return "-";
			}
			boolean coba = true;
			NilaiHuruf huruf = Common.getNilaiHuruf(safeDouble(hasilKonversi),
					mhs == null ? 0 : mhs.getTahunangkatan(),
					mhs == null ? perkuliahan.getJurusan() : mhs.getJurusan(),
					mhs == null || mhs.getJurusan() == null ?
							(perkuliahan.getJurusan() == null ? null : perkuliahan.getJurusan().getFakultas())
							: mhs.getJurusan().getFakultas(),
					perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap(), perkuliahan.getMatakuliah().getKode(), coba,
					true, perkuliahan.getMatakuliah().getJenisNilaiHuruf());
			return huruf == null ? "-" : huruf.getNilaiHuruf();
		} catch (Exception e) {
			return "-";
		}
	}


	private void appendAssessmentRowsForStudent(StudentObeDashboardResult result, FormatNilai fn,
			Map<Long, PertemuanPunyaUjian> md, Map<Long, Pertemuan> mt, Map<Long, TugasPertemuan> mtl,
			Map<Long, TugasKelompok> mtk, Map<Long, Map<Long, Map<String, Double>>> data,
			Map<String, Double> pData, Long mhsId) {
		if (result == null || fn == null || fn.getId() == null || mhsId == null) {
			return;
		}
		if (md != null) {
			appendAssessmentRowsFromCollection(result, fn, md.values(), data, pData, mhsId,
					PertemuanPunyaUjian.class.getName(), "Ujian");
		}
		if (mt != null) {
			appendAssessmentRowsFromCollection(result, fn, mt.values(), data, pData, mhsId,
					Pertemuan.class.getName(), "Tugas Pertemuan");
		}
		if (mtl != null) {
			appendAssessmentRowsFromCollection(result, fn, mtl.values(), data, pData, mhsId,
					TugasPertemuan.class.getName(), "Tugas Lanjutan");
		}
		if (mtk != null) {
			appendAssessmentRowsFromCollection(result, fn, mtk.values(), data, pData, mhsId,
					TugasKelompok.class.getName(), "Tugas Kelompok");
		}
	}

	private <T> void appendAssessmentRowsFromCollection(StudentObeDashboardResult result, FormatNilai fn,
			Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData,
			Long mhsId, String prefix, String jenis) {
		if (items == null) {
			return;
		}
		for (T item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				if (itemId == null) {
					continue;
				}
				String key = prefix + "_" + itemId;
				Double nilai = getNilaiObeForStudent(data, mhsId, fn.getId(), key);
				Double persen = pData == null ? null : pData.get(key + "_" + fn.getId());
				double bobotKomponen = persen == null ? 0.0 : persen.doubleValue();
				double nilaiBerbobot = (nilai == null ? 0.0 : nilai.doubleValue()) * bobotKomponen;
				StudentAssessmentRow row = new StudentAssessmentRow();
				row.subCpmkKode = safeSubCpmkCode(fn);
				row.subCpmkNama = safeText(fn.getNama());
				row.cpmkKode = fn.getCapaianPembelajaranLulusan() == null ? "" : safeText(fn.getCapaianPembelajaranLulusan().getKode());
				row.namaKomponen = getAssessmentComponentName(item, jenis);
				row.jenisKomponen = jenis;
				row.nilai = nilai == null ? 0.0 : nilai.doubleValue();
				row.bobotKomponen = bobotKomponen;
				row.nilaiBerbobot = nilaiBerbobot;
				row.status = row.nilai >= result.targetMinimal ? "L" : "TL";
				if (row.nilai != 0.0 || row.bobotKomponen != 0.0 || row.nilaiBerbobot != 0.0) {
					result.assessmentRows.add(row);
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
	}

	private Double getNilaiObeForStudent(Map<Long, Map<Long, Map<String, Double>>> data, Long mhsId, Long formatId,
			String key) {
		try {
			Double value = data.get(mhsId).get(formatId).get(key);
			return value == null ? Double.valueOf(0.0) : value;
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	private String safeSubCpmkCode(FormatNilai fn) {
		String kode = fn == null ? "" : safeText(fn.getKodeSubCpmk());
		if (kode == null || kode.trim().length() == 0) {
			kode = fn == null ? "" : safeText(fn.getNama());
		}
		return kode;
	}

	private String getAssessmentComponentName(Object item, String jenis) {
		if (item == null) {
			return jenis == null ? "Komponen" : jenis;
		}
		try {
			if (item instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) item;
				return ppu.getUjian() == null ? "Ujian" : safeText(ppu.getUjian().getNama());
			}
			if (item instanceof Pertemuan) {
				return safeText(((Pertemuan) item).getJudultugas());
			}
			if (item instanceof TugasPertemuan) {
				return safeText(((TugasPertemuan) item).getJudultugas());
			}
			if (item instanceof TugasKelompok) {
				return safeText(((TugasKelompok) item).getJudultugas());
			}
			Object value = item.getClass().getMethod("getJudultugas").invoke(item);
			return safeText(value);
		} catch (Exception e) {
			return jenis == null ? "Komponen" : jenis;
		}
	}

	private <T> void parseTugasJSONForStudent(List<T> listTugas, List<FormatNilai> fns, Mahasiswa mahasiswaData,
			Map<Long, Map<Long, Map<String, Double>>> dataMain, Map<Long, Map<Long, T>> mapOut, String prefixClass)
			throws Exception {
		if (listTugas == null || fns == null || mahasiswaData == null) {
			return;
		}
		for (T o : listTugas) {
			if (o == null) {
				continue;
			}
			String jsonStr = (String) o.getClass().getMethod("getKeteranganNilai").invoke(o);
			Long objId = (Long) o.getClass().getMethod("getId").invoke(o);
			if (jsonStr == null || jsonStr.trim().isEmpty() || objId == null) {
				continue;
			}
			JSONObject j = new JSONObject(jsonStr);
			for (FormatNilai fn : fns) {
				if (fn == null || fn.getId() == null) {
					continue;
				}
				String k = mahasiswaData.getId() + "_mhs_nilai_" + fn.getId();
				if (!j.isNull(k)) {
					addNilaiUtama(dataMain, mahasiswaData.getId(), fn.getId(), prefixClass + "_" + objId,
							Double.valueOf(j.getDouble(k)));
					addMapType(mapOut, fn.getId(), objId, o);
				}
			}
		}
	}

	private void addNilaiUtama(Map<Long, Map<Long, Map<String, Double>>> data, Long mhsId, Long formatId, String key,
			Double val) {
		Map<Long, Map<String, Double>> mhsMap = data.get(mhsId);
		if (mhsMap == null) {
			mhsMap = new HashMap<Long, Map<String, Double>>();
			data.put(mhsId, mhsMap);
		}
		Map<String, Double> fmtMap = mhsMap.get(formatId);
		if (fmtMap == null) {
			fmtMap = new HashMap<String, Double>();
			mhsMap.put(formatId, fmtMap);
		}
		Double current = fmtMap.get(key) == null ? Double.valueOf(0.0) : fmtMap.get(key);
		fmtMap.put(key, Double.valueOf(current.doubleValue() + safeDouble(val)));
	}

	private <T> void addMapType(Map<Long, Map<Long, T>> map, Long formatId, Long itemId, T item) {
		Map<Long, T> inner = map.get(formatId);
		if (inner == null) {
			inner = new HashMap<Long, T>();
			map.put(formatId, inner);
		}
		inner.put(itemId, item);
	}

	private void extractBobotObe(String jsonStr, Long formatId, String key, Map<Long, Map<String, Double>> dataBobot) {
		if (jsonStr == null || jsonStr.trim().isEmpty() || formatId == null) {
			return;
		}
		try {
			JSONObject jObj = new JSONObject(jsonStr);
			if (!jObj.isNull(formatId.toString())) {
				Double bobot = jObj.isNull(formatId.toString() + "_bobot") ? Double.valueOf(100.0)
						: Double.valueOf(jObj.getDouble(formatId.toString() + "_bobot"));
				Map<String, Double> mapB = dataBobot.get(formatId);
				if (mapB == null) {
					mapB = new HashMap<String, Double>();
					dataBobot.put(formatId, mapB);
				}
				mapB.put(key, bobot);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void hitungPersenPerKomponenObe(String keyPrefix, Long itemId, FormatNilai fn,
			Map<Long, Map<String, Double>> dataBobot, Map<String, Double> persensData) {
		if (fn == null || fn.getId() == null || itemId == null) {
			return;
		}
		Double persenFraction = Double.valueOf(0.0);
		Map<String, Double> mapB = dataBobot.get(fn.getId());
		if (mapB != null) {
			Double totalB = Double.valueOf(0.0);
			Double nilaiB = Double.valueOf(0.0);
			for (String keyd : mapB.keySet()) {
				Double d = mapB.get(keyd);
				if (d == null) {
					d = Double.valueOf(0.0);
				}
				if (keyd != null && keyd.equalsIgnoreCase(keyPrefix + "_" + itemId)) {
					nilaiB += d.doubleValue();
				}
				totalB += d.doubleValue();
			}
			if (totalB.doubleValue() > 0.0) {
				persenFraction = Double.valueOf((nilaiB.doubleValue() / totalB.doubleValue()) * normalizePercentFraction(fn.getPersen()));
			}
		}
		persensData.put(keyPrefix + "_" + itemId + "_" + fn.getId(), persenFraction);
	}

	private Double recalculateTotalPerFormatObe(Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData,
			Long mhsId, Long formatId, Map<Long, PertemuanPunyaUjian> md, Map<Long, Pertemuan> mt,
			Map<Long, TugasPertemuan> mtl, Map<Long, TugasKelompok> mtk) {
		Double total = Double.valueOf(0.0);
		if (md != null) {
			total += sumNilaiObe(md.values(), data, pData, mhsId, formatId, PertemuanPunyaUjian.class.getName());
		}
		if (mt != null) {
			total += sumNilaiObe(mt.values(), data, pData, mhsId, formatId, Pertemuan.class.getName());
		}
		if (mtl != null) {
			total += sumNilaiObe(mtl.values(), data, pData, mhsId, formatId, TugasPertemuan.class.getName());
		}
		if (mtk != null) {
			total += sumNilaiObe(mtk.values(), data, pData, mhsId, formatId, TugasKelompok.class.getName());
		}
		return total;
	}

	private <T> Double sumNilaiObe(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data,
			Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
		Double sum = Double.valueOf(0.0);
		if (items == null) {
			return sum;
		}
		for (T item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				String key = prefix + "_" + itemId;
				Double nilai = Double.valueOf(0.0);
				try {
					nilai = data.get(mhsId).get(formatId).get(key);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				Double persenData = Double.valueOf(0.0);
				try {
					Double persen = pData.get(key + "_" + formatId);
					persenData = Double.valueOf((persen == null ? 0.0 : persen.doubleValue()) * (nilai == null ? 0.0 : nilai.doubleValue()));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				sum += persenData.doubleValue();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		return sum;
	}

	private Double hitungKonversiObe(FormatNilai formatNilai, Double totalNilaiPerformatNilai) {
		double persenFraction = normalizePercentFraction(formatNilai == null ? null : formatNilai.getPersen());
		return Double.valueOf(persenFraction <= 0.0 ? 0.0 : safeDouble(totalNilaiPerformatNilai) / persenFraction);
	}

	private static double normalizePercentFraction(Double value) {
		double d = safeDouble(value);
		if (d < 0.0) {
			d = 0.0;
		}
		return d > 1.0 ? d / 100.0 : d;
	}

	private static double normalizeRelationWeight(Double value) {
		double d = safeDouble(value);
		if (d <= 0.0) {
			return 1.0;
		}
		return d > 1.0 ? d / 100.0 : d;
	}

	private static boolean containsIdInCsv(String csv, Long id) {
		if (csv == null || id == null) {
			return false;
		}
		String target = String.valueOf(id.longValue());
		String[] parts = csv.split(",");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] != null && target.equals(parts[i].trim())) {
				return true;
			}
		}
		return false;
	}

	private void showWarning(String message) {
		if (rowsUtama != null) {
			Row row = new Row();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setValign("top");
			row.setParent(rowsUtama);
			row.appendChild(new Html("<div style='" + STYLE_INFO + "'>" + escapeHtml(message) + "</div>"));
		}
	}

	private static String getSubCpmkKey(JSONObject jsonObject, JSONObject subCpmk) {
		if (subCpmk != null && !subCpmk.isNull("key")) {
			return safeText(subCpmk.opt("key"));
		}
		return String.valueOf(safeInt(jsonObject, "mulaiMingguKe", 0));
	}

	private static String buildFormatKey(Long cpmkId, String subKey) {
		return String.valueOf(cpmkId) + "|" + safeText(subKey).trim().toLowerCase();
	}

	private static double normalizeBobotToFraction(Double bobotRaw) {
		double bobot = safeDouble(bobotRaw);
		if (bobot > 1.0) {
			return bobot / 100.0;
		}
		return bobot;
	}

	private static double normalizeBobotToPercent(Double bobotRaw) {
		double bobot = safeDouble(bobotRaw);
		if (bobot <= 1.0) {
			return bobot * 100.0;
		}
		return bobot;
	}

	private static Set<Long> parseIdsToSet(String commaSeparatedIds) {
		Set<Long> longs = new HashSet<Long>();
		if (commaSeparatedIds == null || commaSeparatedIds.trim().length() == 0) {
			return longs;
		}
		String[] ids = commaSeparatedIds.split(",");
		for (int i = 0; i < ids.length; i++) {
			String d = ids[i] == null ? "" : ids[i].trim();
			if (d.length() == 0) {
				continue;
			}
			Long parsed = parseSingleIdToken(d);
			if (parsed != null) {
				longs.add(parsed);
			}
		}
		return longs;
	}

	/**
	 * Mengubah satu token menjadi Long id secara defensif:
	 * <ul>
	 * <li>Token numerik murni di-parse langsung (mis. "2551").</li>
	 * <li>Token komposit ber-underscore adalah data lama pada kolom
	 * capaian_lulusan.profil. RpsObeAction.reloadPl menuliskannya sebagai
	 * "&lt;profilLulusanId&gt;_&lt;kurikulumPunyaMatakuliahId&gt;" (mis.
	 * "6_2551" = ProfilLulusan 6, KPM 2551). Token 3-segmen seperti
	 * "3_2547_2551" adalah korupsi penggabungan dari delete substring lama,
	 * tetapi segmen PERTAMA tetap id ProfilLulusan. Karena itu id yang benar
	 * diambil dari segmen PERTAMA (sebelum underscore pertama), BUKAN
	 * terakhir.</li>
	 * <li>Token lain yang tetap tidak bisa di-parse dilewati (return null)
	 * tanpa memunculkan error ke admin, mengikuti perilaku dashboard OBE
	 * lain yang sudah mengabaikan token tidak valid secara senyap.</li>
	 * </ul>
	 */
	private static Long parseSingleIdToken(String token) {
		if (token == null) {
			return null;
		}
		String t = token.trim();
		if (t.length() == 0) {
			return null;
		}
		try {
			return Long.valueOf(t);
		} catch (NumberFormatException eFull) {
			int us = t.indexOf('_');
			if (us > 0) {
				String head = t.substring(0, us).trim();
				if (head.length() > 0) {
					try {
						return Long.valueOf(head);
					} catch (NumberFormatException eHead) {
						return null;
					}
				}
			}
			return null;
		}
	}

	private static boolean containsDelimitedId(String data, Long id) {
		if (data == null || id == null) {
			return false;
		}
		String normalized = data.trim();
		if (!normalized.startsWith(",")) {
			normalized = "," + normalized;
		}
		if (!normalized.endsWith(",")) {
			normalized = normalized + ",";
		}
		return normalized.indexOf("," + id + ",") >= 0;
	}

	private static void addToMap(Map<String, Double> map, String key, double value) {
		String safeKey = key == null ? "" : key;
		Double old = map.get(safeKey);
		map.put(safeKey, Double.valueOf((old == null ? 0.0 : old.doubleValue()) + value));
	}

	private static Double safeMapValue(Map<String, Double> map, String key) {
		if (map == null) {
			return Double.valueOf(0.0);
		}
		Double value = map.get(key == null ? "" : key);
		return value == null ? Double.valueOf(0.0) : value;
	}

	private static JSONArray safeJsonArray(String value) {
		try {
			return isBlank(value) ? new JSONArray() : new JSONArray(value);
		} catch (Exception e) {
			return new JSONArray();
		}
	}

	private static double safeDouble(Object value) {
		return safeDouble(value, 0.0);
	}

	private static double safeDouble(Object value, double def) {
		try {
			if (value == null) {
				return def;
			}
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			String text = String.valueOf(value).trim();
			if (text.length() == 0) {
				return def;
			}
			return Double.parseDouble(text);
		} catch (Exception e) {
			return def;
		}
	}

	private static double safeDouble(JSONObject jsonObject, String key, double def) {
		try {
			return jsonObject == null || jsonObject.isNull(key) ? def : safeDouble(jsonObject.get(key), def);
		} catch (Exception e) {
			return def;
		}
	}

	private static int safeInt(JSONObject jsonObject, String key, int def) {
		try {
			return jsonObject == null || jsonObject.isNull(key) ? def : Integer.parseInt(jsonObject.get(key) + "");
		} catch (Exception e) {
			return def;
		}
	}

	private static String safeText(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static void appendWithComma(StringBuilder sb, String value) {
		if (value == null || value.length() == 0) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(",");
		}
		sb.append(value);
	}

	private static void appendWithNewLine(StringBuilder sb, String value) {
		if (value == null || value.length() == 0) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(",\n");
		}
		sb.append(value);
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static void closeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			try {
				if (session.isOpen()) {
					session.clear();
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				session.disconnect();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	private static class StudentObeDashboardResult {
		String namaMahasiswa = "";
		String nilaiHuruf = "-";
		String statusAkhir = "";
		double targetMinimal = 60.0;
		double totalNilaiBobotAkhir = 0.0;
		double totalBobotFormatAkhir = 0.0;
		double nilaiAkhir = 0.0;
		boolean hasData = false;
		List<StudentAssessmentRow> assessmentRows = new ArrayList<StudentAssessmentRow>();
		List<StudentSubCpmkRow> subCpmkRows = new ArrayList<StudentSubCpmkRow>();
		List<StudentOutcomeRow> cpmkRows = new ArrayList<StudentOutcomeRow>();
		List<StudentOutcomeRow> cplRows = new ArrayList<StudentOutcomeRow>();
		List<StudentOutcomeRow> plRows = new ArrayList<StudentOutcomeRow>();
	}


	private static class StudentAssessmentRow {
		String subCpmkKode = "";
		String subCpmkNama = "";
		String cpmkKode = "";
		String namaKomponen = "";
		String jenisKomponen = "";
		double nilai = 0.0;
		double bobotKomponen = 0.0;
		double nilaiBerbobot = 0.0;
		String status = "";
	}

	private static class StudentSubCpmkRow {
		String kode = "";
		String nama = "";
		String cpmkKode = "";
		String cpmkNama = "";
		double bobot = 0.0;
		double total = 0.0;
		double konversi = 0.0;
		String status = "";
	}

	private static class StudentOutcomeRow {
		String kode = "";
		String nama = "";
		double bobot = 0.0;
		double total = 0.0;
		double nilai = 0.0;
		String status = "";
	}

	private static class ObeCalculationResult {
		List<ObeRowData> rows = new ArrayList<ObeRowData>();
		Map<String, Double> totalCpmk = new HashMap<String, Double>();
		Map<String, Double> totalCpl = new HashMap<String, Double>();
		double totalBobotPercent = 0.0;
		double totalNilaiMk = 0.0;
		double totalNilaiMahasiswaMentah = 0.0;
		double rataNilaiMahasiswa = 0.0;
		double totalKetercapaianCplPercent = 0.0;
		int jumlahNilaiMahasiswa = 0;
	}

	private static class ObeRowData {
		JSONObject jsonObject;
		JSONObject subCpmk;
		CapaianPembelajaranLulusan cpmk;
		FormatNilai formatNilai;
		String uniqueKey;
		String kodeProfilLulusan = "";
		String namaProfilLulusan = "";
		String kodeCapaian = "";
		String namaCapaian = "";
		String mingguText = "";
		String kodeCpmk = "";
		String namaCpmk = "";
		String kodeSubCpmk = "";
		String namaSubCpmk = "";
		String indikator = "";
		String teknik = "";
		double bobotRaw = 0.0;
		double bobotFraction = 0.0;
		double bobotPercent = 0.0;
		double nilaiMahasiswa = 0.0;
		double nilaiTeknik = 0.0;
	}
}
