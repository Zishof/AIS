package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.BankSoalAction;
import ais.action.master.helper.generic.AmbilDataBankSoalBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DetailUjianHelper implements DataLoader {

	private Ujian ujian;
	private Grid grid;

	// private boolean delete = false;
	// private boolean edit = false;
	// private boolean add = false;
	private Textbox cari;

	private Paging paging;
	private Tbmuser tbmuser;
	protected int countHasil = 0;
	private boolean refreshSoal = false;
	private PertemuanPunyaUjian pertemuanPunyaUjian;

	public DetailUjianHelper() {
		tbmuser = Common.getCurrentUser();
	}

	public void display(final Ujian ujian, final Component detail, boolean tampilmenu, boolean delete) {
		display(ujian, detail, null, null, tampilmenu, delete);
	}

	private Component checkMerupakanPerkuliahan(Component detail, final PertemuanPunyaUjian pertemuanPunyaUjian) {
		Component parent = null;
		if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan() != null) {
			// STRUKTUR: Borderlayout(detail) -> Center (scrollable, menampung Tabbox) + South
			// (Toolbar Batal/Simpan yang SELALU tampil, tidak pernah menutupi/tertutup konten
			// tab manapun — termasuk tab "Anti Curang" yang isinya paling panjang). Pola ini
			// SAMA dengan pola reusable yang sudah dipakai luas di codebase (lih. BankSoalAction,
			// dashboard-dashboard, laporan, dll: Borderlayout+Common.tampilanScrollTabbox(center)+South),
			// sehingga konsisten & sudah terbukti aman terhadap bug ZK5 "tab panel ber-height
			// terukur => overflow:hidden => konten panjang terpotong tanpa scrollbar".
			Borderlayout borderlayoutUjian = new ais.ui.util.MyBorderlayout();
			borderlayoutUjian.setParent(detail);
			borderlayoutUjian.setWidth("100%");
			borderlayoutUjian.setHeight("100%");

			Center centerUjian = new Center();
			centerUjian.setParent(borderlayoutUjian);
			ais.ui.util.ZkCompat.setFlex(centerUjian, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(centerUjian));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");
			tabbox.setStyle("min-height:1000px");
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabSoal = new MyTabConfig("Soal");
			tabs.appendChild(tabSoal);
			MyTabConfig tabPeserta = new MyTabConfig("Peserta yg tidak perlu ikut");
			tabs.appendChild(tabPeserta);

			MyTabConfig tabSyarat = new MyTabConfig("Syarat Ikut Ujian");
			tabs.appendChild(tabSyarat);

			// Tab "Anti Curang" (CBT) PER-UJIAN — pindahan dari Konfigurasi global.
			MyTabConfig tabAntiCurang = new MyTabConfig("Anti Curang");
			tabs.appendChild(tabAntiCurang);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			parent = new ais.ui.util.MyTabpanel();
			parent.setParent(tabpanels);

			final Tabpanel tabpanelPeserta = new ais.ui.util.MyTabpanel();
			tabpanelPeserta.setParent(tabpanels);
			// min-height (bukan height tetap): agar konten yg lebih panjang dari 2000px tetap
			// bisa tumbuh & discroll via Center pembungkus, tidak terpotong overflow:hidden.
			tabpanelPeserta.setStyle("min-height:2000px");

			tabPeserta.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelPeserta.getChildren().isEmpty()) {

						MyDiv div = new MyDiv();
						div.setParent(tabpanelPeserta);

						Hbox hbox = new Hbox();
						hbox.appendChild(new MyLabelConfig("Peserta : "));
						final Textbox cari = new Textbox("");
						cari.setParent(hbox);
						cari.setCols(20);

						// OBE remedial: bila kurikulum OBE, tampilkan checklist Sub-CPMK yang DIKERJAKAN tiap
						// peserta (default: SEMUA Sub-CPMK yang terpilih di ujian; boleh dibatasi sebagian).
						final ais.database.model.Perkuliahan perkuliahanUjian = pertemuanPunyaUjian.getPertemuan()
								.getPerkuliahan();
						final boolean obe = perkuliahanUjian != null && perkuliahanUjian.getKurikulum() != null
								&& perkuliahanUjian.getKurikulum().apakahObe(perkuliahanUjian.getTahunAjaran(),
										perkuliahanUjian.getGanjilGenap());
						final java.util.List<ais.database.model.FormatNilai> subCpmkTerpilih =
								new java.util.ArrayList<ais.database.model.FormatNilai>();
						if (obe) {
							try {
								org.json.JSONObject jfn = new org.json.JSONObject(
										pertemuanPunyaUjian.getFormatNilais());
								for (ais.database.model.FormatNilai fn : Common
										.getFormatNilais(HibernateUtil.currentSession(), perkuliahanUjian)) {
									if (fn != null && fn.getId() != null && fn.getStatusPertemuan() != null
											&& !jfn.isNull(fn.getId().toString())) {
										subCpmkTerpilih.add(fn);
									}
								}
							} catch (Exception eFn) { ais.common.ErrorAuditUtil.record(eFn, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:200");
							}
						}
						final boolean adaSubCpmk = obe && !subCpmkTerpilih.isEmpty();

						final Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(div);

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig();
						column.appendChild(hbox);
						column.setParent(columns);
						column.setWidth(adaSubCpmk ? "35%" : "70%");
						column.setAlign("left");

						final MyCheckboxConfig checkboxConfigAll = new MyCheckboxConfig("Tidak perlu ikut");

						column = new MyColumnConfig();
						column.appendChild(checkboxConfigAll);
						column.setParent(columns);
						column.setWidth(adaSubCpmk ? "12%" : "30%");
						column.setAlign("left");

						if (adaSubCpmk) {
							MyColumnConfig columnNilai = new MyColumnConfig();
							columnNilai.appendChild(new MyLabelConfig("Nilai Manual (Nilai & Keterangan)"));
							columnNilai.setParent(columns);
							columnNilai.setWidth("23%");
							columnNilai.setAlign("left");

							MyColumnConfig columnCpmk = new MyColumnConfig();
							columnCpmk.appendChild(new MyLabelConfig("Sub-CPMK yang dikerjakan (OBE)"));
							columnCpmk.setParent(columns);
							columnCpmk.setWidth("30%");
							columnCpmk.setAlign("left");
						}

						ais.ui.util.ZkCompat.setFixedLayout(grid, true);
						grid.setHeight("100%");
						grid.setWidth("100%");

						grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
							@Override
							public void render(Row arg0, Object arg1) throws Exception {
								arg0.setValign("top");
								// Holder final agar baris bisa di-invalidate dari anonymous listener (Java 1.7).
								final Row rowPeserta = arg0;
								final Mahasiswa mahasiswa = (arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
								final BiodataCalonMahasiswa biodataCalonMahasiswa = (arg1 instanceof BiodataCalonMahasiswa)
										? (BiodataCalonMahasiswa) arg1
										: null;
								final Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
								final CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

								// Sel peserta memakai Div FLEX (bukan Hbox/Vbox berbasis <table> yang
								// ter-render rata-tengah sehingga nama "loncat-loncat"). Flexbox
								// menjamin foto + (NIM/Nama) rapat rata-KIRI & sejajar vertikal.
								org.zkoss.zul.Div selPeserta = new org.zkoss.zul.Div();
								selPeserta.setParent(arg0);
								selPeserta.setStyle("display:flex;align-items:center;gap:9px;width:100%;"
										+ "text-align:left;box-sizing:border-box;");
								if (mahasiswa != null) {
									CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(selPeserta);
								} else if (biodataCalonMahasiswa != null) {
									CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(selPeserta);
								}

								String nimTeks = mahasiswa != null ? mahasiswa.getNim()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : "";
								String namaTeks = mahasiswa != null ? mahasiswa.getNama()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "";
								new Html("<div style='text-align:left;line-height:1.45;'>"
										+ "<div style='font-size:11px;color:#64748b;font-weight:600;'>"
										+ ais.ui.util.DashboardUiKit.esc(nimTeks) + "</div>"
										+ "<div style='font-size:13px;color:#0f172a;font-weight:700;'>"
										+ ais.ui.util.DashboardUiKit.esc(namaTeks) + "</div></div>").setParent(selPeserta);

								Long id = mahasiswa != null ? mahasiswa.getId()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
												: siswa != null ? siswa.getId()
														: calonSiswa != null ? calonSiswa.getId() : null;

								// Referensi bersama untuk mengatur VISIBILITAS DINAMIS (permintaan user):
								//  - "Tidak perlu ikut" dicentang  -> sembunyikan SEMUA (Nilai Manual + Sub-CPMK).
								//  - Kotak Nilai Manual per Sub-CPMK -> HANYA muncul bila "Paksa" dicentang DAN
								//    Sub-CPMK (OBE) yang bersangkutan ikut dicentang. Bila Sub-CPMK tidak dicentang,
								//    kotak Nilai Manual-nya pun tidak muncul.
								final org.zkoss.zul.Div[] wrapNilaiRef = new org.zkoss.zul.Div[1];
								final org.zkoss.zul.Div[] wrapCpmkRef = new org.zkoss.zul.Div[1];
								final org.zkoss.zul.Checkbox[] cbPaksaRef = new org.zkoss.zul.Checkbox[1];
								final java.util.LinkedHashMap<Long, org.zkoss.zul.Hbox> nilaiRowMap = new java.util.LinkedHashMap<Long, org.zkoss.zul.Hbox>();
								final java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox> obeCbMap = new java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox>();

								final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Tidak perlu ikut");
								checkboxConfig.setDisabled(mahasiswa == null);
								checkboxConfig
										.setChecked(pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ","));
								checkboxConfig.setParent(arg0);

								// Menyegarkan tampilan Nilai Manual/Sub-CPMK sesuai tiga kondisi di atas.
								final EventListener perbaruiVisibilitasNilai = new EventListener() {
									@Override
									public void onEvent(Event evVis) throws Exception {
										boolean ikut = !checkboxConfig.isChecked();
										boolean paksa = cbPaksaRef[0] != null && cbPaksaRef[0].isChecked();
										if (wrapNilaiRef[0] != null) {
											wrapNilaiRef[0].setVisible(ikut);
										}
										if (wrapCpmkRef[0] != null) {
											wrapCpmkRef[0].setVisible(ikut);
										}
										for (java.util.Map.Entry<Long, org.zkoss.zul.Hbox> en : nilaiRowMap.entrySet()) {
											org.zkoss.zul.Checkbox obe = obeCbMap.get(en.getKey());
											boolean obeOn = obe == null || obe.isChecked();
											en.getValue().setVisible(ikut && paksa && obeOn);
										}
										// Render ulang baris agar tingginya menyesuaikan konten yang baru
										// tampil/tersembunyi (auto-height) — mencegah checkbox "Paksa" terpotong.
										if (rowPeserta != null) {
											rowPeserta.invalidate();
										}
									}
								};

								checkboxConfig.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (pertemuanPunyaUjian.getId() != null) {
											session.refresh(pertemuanPunyaUjian);
										}

										Long id = mahasiswa != null ? mahasiswa.getId()
												: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
														: siswa != null ? siswa.getId()
																: calonSiswa != null ? calonSiswa.getId() : null;

										String ids = "," + id + ",";
										String text = pertemuanPunyaUjian.getMhsYgTidakIkut();
										text = org.apache.commons.lang3.StringUtils.replace(text, ids, "");
										text = org.apache.commons.lang3.StringUtils.replace(text, id.toString(), "");

										pertemuanPunyaUjian
												.setMhsYgTidakIkut(text + (!checkboxConfig.isChecked() ? "" : ids));
										Common.refreshUpdate(session, pertemuanPunyaUjian);
										perbaruiVisibilitasNilai.onEvent(null);
									}
								});

								// Nilai Manual per Sub-CPMK (Feature 4): Doublebox tiap Sub-CPMK + checkbox "Paksa".
								if (adaSubCpmk) {
									if (mahasiswa != null) {
										org.zkoss.zul.Div wrapNilai = new org.zkoss.zul.Div();
										wrapNilai.setStyle("padding:2px 0;");
										wrapNilai.setParent(arg0);
										wrapNilaiRef[0] = wrapNilai;
										final String mhsKey = mahasiswa.getId().toString();
										// Baca JSON nilai manual untuk praisi Doublebox.
										org.json.JSONObject mhsEntryTmp = new org.json.JSONObject();
										try {
											String njStr = pertemuanPunyaUjian.getNilaiManualJson();
											if (njStr != null && !njStr.isEmpty()) {
												org.json.JSONObject jAll = new org.json.JSONObject(njStr);
												org.json.JSONObject ent = jAll.optJSONObject(mhsKey);
												if (ent != null) mhsEntryTmp = ent;
											}
										} catch (Exception eNilai) { /* abaikan */ }
										final org.json.JSONObject mhsEntryFinal = mhsEntryTmp;
										for (final ais.database.model.FormatNilai fn : subCpmkTerpilih) {
											if (fn == null || fn.getId() == null) continue;
											final String fnKey = "fn_" + fn.getId();
											org.zkoss.zul.Hbox rowN = new org.zkoss.zul.Hbox();
											rowN.setStyle("align-items:center;gap:4px;margin-bottom:2px;");
											rowN.setParent(wrapNilai);
											nilaiRowMap.put(fn.getId(), rowN);
											new org.zkoss.zul.Label(fn.getNama() + ": ").setParent(rowN);
											final org.zkoss.zul.Doublebox db = new org.zkoss.zul.Doublebox();
											Double initVal = null;
											if (!mhsEntryFinal.isNull(fnKey)) {
												try { initVal = mhsEntryFinal.getDouble(fnKey); } catch (Exception ex) {}
											}
											db.setValue(initVal);
											db.setWidth("90px");
											db.setParent(rowN);
											db.addEventListener("onChange", new EventListener() {
												@Override
												public void onEvent(Event ev) throws Exception {
													Session s = HibernateUtil.currentSession();
													if (pertemuanPunyaUjian.getId() != null) s.refresh(pertemuanPunyaUjian);
													String njStr2 = pertemuanPunyaUjian.getNilaiManualJson();
													org.json.JSONObject jAll = (njStr2 != null && !njStr2.isEmpty())
															? new org.json.JSONObject(njStr2) : new org.json.JSONObject();
													org.json.JSONObject entry = jAll.optJSONObject(mhsKey);
													if (entry == null) entry = new org.json.JSONObject();
													if (db.getValue() != null) {
														entry.put(fnKey, db.getValue());
													} else {
														entry.remove(fnKey);
													}
													jAll.put(mhsKey, entry);
													pertemuanPunyaUjian.setNilaiManualJson(jAll.toString());
													Common.refreshUpdate(s, pertemuanPunyaUjian);
												}
											});

											// KETERANGAN nilai manual per Sub-CPMK (sesuai permintaan): kotak teks di samping
											// kotak Nilai. Disimpan pada JSON yang sama dengan kunci "<fnKey>_ket". JANGAN pakai
											// setPlaceholder (memicu batch-error di ZK5) -> pakai setTooltiptext.
											final String fnKeyKet = fnKey + "_ket";
											final org.zkoss.zul.Textbox tbKet = new org.zkoss.zul.Textbox();
											tbKet.setValue(mhsEntryFinal.isNull(fnKeyKet) ? ""
													: mhsEntryFinal.optString(fnKeyKet, ""));
											tbKet.setWidth("200px");
											tbKet.setTooltiptext("Keterangan untuk nilai " + fn.getNama());
											tbKet.setParent(rowN);
											tbKet.addEventListener("onChange", new EventListener() {
												@Override
												public void onEvent(Event ev) throws Exception {
													Session s = HibernateUtil.currentSession();
													if (pertemuanPunyaUjian.getId() != null) s.refresh(pertemuanPunyaUjian);
													String njStr3 = pertemuanPunyaUjian.getNilaiManualJson();
													org.json.JSONObject jAll = (njStr3 != null && !njStr3.isEmpty())
															? new org.json.JSONObject(njStr3) : new org.json.JSONObject();
													org.json.JSONObject entry = jAll.optJSONObject(mhsKey);
													if (entry == null) entry = new org.json.JSONObject();
													String v = tbKet.getValue() == null ? "" : tbKet.getValue().trim();
													if (v.isEmpty()) {
														entry.remove(fnKeyKet);
													} else {
														entry.put(fnKeyKet, v);
													}
													jAll.put(mhsKey, entry);
													pertemuanPunyaUjian.setNilaiManualJson(jAll.toString());
													Common.refreshUpdate(s, pertemuanPunyaUjian);
												}
											});
										}
										// Checkbox "Paksa pakai nilai ini jika tetap ikut ujian"
										final org.zkoss.zul.Checkbox cbPaksa = new org.zkoss.zul.Checkbox(
												"Paksa pakai nilai ini jika tetap ikut ujian");
										cbPaksa.setChecked(mhsEntryFinal.optBoolean("paksa", false));
										cbPaksa.setStyle("margin-top:4px;display:block;");
										cbPaksa.setParent(wrapNilai);
										cbPaksaRef[0] = cbPaksa;
										cbPaksa.addEventListener("onCheck", new EventListener() {
											@Override
											public void onEvent(Event ev) throws Exception {
												Session s = HibernateUtil.currentSession();
												if (pertemuanPunyaUjian.getId() != null) s.refresh(pertemuanPunyaUjian);
												String njStr2 = pertemuanPunyaUjian.getNilaiManualJson();
												org.json.JSONObject jAll = (njStr2 != null && !njStr2.isEmpty())
														? new org.json.JSONObject(njStr2) : new org.json.JSONObject();
												org.json.JSONObject entry = jAll.optJSONObject(mhsKey);
												if (entry == null) entry = new org.json.JSONObject();
												entry.put("paksa", cbPaksa.isChecked());
												jAll.put(mhsKey, entry);
												pertemuanPunyaUjian.setNilaiManualJson(jAll.toString());
												Common.refreshUpdate(s, pertemuanPunyaUjian);
												perbaruiVisibilitasNilai.onEvent(null);
											}
										});
									} else {
										new org.zkoss.zul.Label("").setParent(arg0); // sel kosong agar kolom sejajar.
									}
								}

								// OBE remedial: kolom checklist Sub-CPMK yang DIKERJAKAN peserta ini (default semua).
								if (adaSubCpmk && mahasiswa != null) {
									org.zkoss.zul.Div wrapCpmk = new org.zkoss.zul.Div();
									wrapCpmk.setStyle("display:flex;flex-wrap:wrap;gap:4px 14px;");
									wrapCpmk.setParent(arg0);
									wrapCpmkRef[0] = wrapCpmk;
									final Long mhsIdCpmk = mahasiswa.getId();
									final java.util.Set<Long> terpilihPeserta = pertemuanPunyaUjian
											.ambilSubCpmkPeserta(mhsIdCpmk);
									final java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox> cbMap = new java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox>();
									EventListener onCpmk = new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											Session s = HibernateUtil.currentSession();
											if (pertemuanPunyaUjian.getId() != null) {
												s.refresh(pertemuanPunyaUjian);
											}
											org.json.JSONObject j = new org.json.JSONObject(
													pertemuanPunyaUjian.getSubCpmkPerPeserta());
											org.json.JSONArray arr = new org.json.JSONArray();
											int dipilih = 0;
											for (java.util.Map.Entry<Long, org.zkoss.zul.Checkbox> en : cbMap.entrySet()) {
												if (en.getValue().isChecked()) {
													arr.put(en.getKey().toString());
													dipilih++;
												}
											}
											if (dipilih >= cbMap.size()) {
												j.remove(mhsIdCpmk.toString()); // semua terpilih → kembali ke default.
											} else {
												j.put(mhsIdCpmk.toString(), arr);
											}
											pertemuanPunyaUjian.setSubCpmkPerPeserta(j.toString());
											Common.refreshUpdate(s, pertemuanPunyaUjian);
											// Sub-CPMK yang tidak dicentang -> kotak Nilai Manual-nya ikut disembunyikan.
											perbaruiVisibilitasNilai.onEvent(null);
										}
									};
									for (ais.database.model.FormatNilai fn : subCpmkTerpilih) {
										if (fn == null || fn.getId() == null) {
											continue;
										}
										org.zkoss.zul.Checkbox cb = new org.zkoss.zul.Checkbox(fn.getNama());
										cb.setChecked(terpilihPeserta == null || terpilihPeserta.contains(fn.getId()));
										cb.addEventListener("onCheck", onCpmk);
										cb.setParent(wrapCpmk);
										cbMap.put(fn.getId(), cb);
										obeCbMap.put(fn.getId(), cb);
									}
								} else if (adaSubCpmk) {
									new org.zkoss.zul.Label("").setParent(arg0); // sel kosong agar kolom sejajar.
								}

								// Terapkan visibilitas AWAL sesuai keadaan tersimpan (tidak-ikut / paksa / Sub-CPMK).
								perbaruiVisibilitasNilai.onEvent(null);
							}

						});

						EventListener cariAkun = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Mahasiswa> mahasiswasTemorary = pertemuanPunyaUjian.getPertemuan().getPerkuliahan()
										.ambilMahasiswa();
								List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
								for (Mahasiswa mahasiswa : mahasiswasTemorary) {
									BiodataCalonMahasiswa biodataCalonMahasiswa = null;
									if (cari.getValue().trim().isEmpty() ||

											(mahasiswa != null &&

													((mahasiswa.getNim() != null && mahasiswa.getNim().toLowerCase()
															.contains(cari.getValue().toLowerCase().trim()))

															||

															(mahasiswa.getNama() != null
																	&& mahasiswa.getNama().toLowerCase().contains(
																			cari.getValue().toLowerCase().trim()))

													)

											)

											||

											(biodataCalonMahasiswa != null &&

													((biodataCalonMahasiswa.getNoRegistrasi() != null
															&& biodataCalonMahasiswa.getNoRegistrasi().toLowerCase()
																	.contains(cari.getValue().toLowerCase().trim()))

															||

															(biodataCalonMahasiswa.getNama() != null
																	&& biodataCalonMahasiswa.getNama().toLowerCase()
																			.contains(cari.getValue().toLowerCase()
																					.trim()))

													)

											)

									) {
										copy.add(mahasiswa);
									}
								}
								ListModel strset = new SimpleListModel(copy);
								grid.setModel(strset);
								mahasiswasTemorary = null;
								copy = null;
							}
						};
						cariAkun.onEvent(null);
						cari.addEventListener("onOK", cariAkun);

						Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
						toolbarbutton.setParent(hbox);
						toolbarbutton.addEventListener("onClick", cariAkun);

						checkboxConfigAll.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Session session = HibernateUtil.currentSession();
								if (pertemuanPunyaUjian.getId() != null) {
									session.refresh(pertemuanPunyaUjian);
								}

								List<Mahasiswa> mahasiswasTemorary = pertemuanPunyaUjian.getPertemuan().getPerkuliahan()
										.ambilMahasiswa();
								List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
								for (Mahasiswa mahasiswa : mahasiswasTemorary) {
									BiodataCalonMahasiswa biodataCalonMahasiswa = null;
									Siswa siswa = null;
									CalonSiswa calonSiswa = null;
									if (cari.getValue().trim().isEmpty() ||

											(mahasiswa != null &&

													((mahasiswa.getNim() != null && mahasiswa.getNim().toLowerCase()
															.contains(cari.getValue().toLowerCase().trim()))

															||

															(mahasiswa.getNama() != null
																	&& mahasiswa.getNama().toLowerCase().contains(
																			cari.getValue().toLowerCase().trim()))

													)

											)

											||

											(biodataCalonMahasiswa != null &&

													((biodataCalonMahasiswa.getNoRegistrasi() != null
															&& biodataCalonMahasiswa.getNoRegistrasi().toLowerCase()
																	.contains(cari.getValue().toLowerCase().trim()))

															||

															(biodataCalonMahasiswa.getNama() != null
																	&& biodataCalonMahasiswa.getNama().toLowerCase()
																			.contains(cari.getValue().toLowerCase()
																					.trim()))

													)

											)

									) {

										Long id = mahasiswa != null ? mahasiswa.getId()
												: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
														: siswa != null ? siswa.getId()
																: calonSiswa != null ? calonSiswa.getId() : null;
										String ids = "," + id + ",";
										String text = pertemuanPunyaUjian.getMhsYgTidakIkut();
										text = org.apache.commons.lang3.StringUtils.replace(text, ids, "");
										text = org.apache.commons.lang3.StringUtils.replace(text, id.toString(), "");

										pertemuanPunyaUjian
												.setMhsYgTidakIkut(text + (!checkboxConfigAll.isChecked() ? "" : ids));

										copy.add(mahasiswa);
									}
								}

								Common.refreshUpdate(session, pertemuanPunyaUjian);

								ListModel strset = new SimpleListModel(copy);
								grid.setModel(strset);
								mahasiswasTemorary = null;
								copy = null;

							}
						});
					}
				}
			});

			final Tabpanel tabpanelSyarat = new ais.ui.util.MyTabpanel();
			tabpanelSyarat.setParent(tabpanels);
			tabpanelSyarat.setStyle("min-height:2000px");

			tabSyarat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelSyarat.getChildren().isEmpty()) {
						Row p = Common.tampilanScroll(tabpanelSyarat);
						Set<String> syaratAlert = new HashSet<String>();
						Tugas.tampilanSyarat(pertemuanPunyaUjian.getPertemuan(), null, pertemuanPunyaUjian.getUjian(),
								null, null, null, (Rows) p.getParent(), syaratAlert, null);
					}
				}
			});

			// Tab "Anti Curang" (CBT) PER-UJIAN — dibangun lazy saat diklik.
			final Tabpanel tabpanelAntiCurang = new ais.ui.util.MyTabpanel();
			tabpanelAntiCurang.setParent(tabpanels);
			// FIX: sebelumnya height TETAP "2000px" (default overflow:hidden ZK5) sehingga
			// field paling bawah ("Radius lokasi") ikut terpotong bila isi tab (banyak
			// checkbox+textarea+field GPS) melebihi 2000px. min-height membiarkan tab tumbuh
			// sepenuhnya; Center pembungkus (lihat borderlayoutUjian di atas) yang men-scroll.
			tabpanelAntiCurang.setStyle("min-height:2000px");
			tabAntiCurang.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelAntiCurang.getChildren().isEmpty()) {
						isiTabAntiCurang(tabpanelAntiCurang, pertemuanPunyaUjian);
					}
				}
			});

			// Footer Batal/Simpan — SATU set tombol untuk SELURUH jendela (bukan per-tab),
			// diletakkan di South Borderlayout sehingga SELALU terlihat & TIDAK PERNAH
			// menutupi konten tab manapun (Soal, Peserta yg tidak perlu ikut, Syarat Ikut
			// Ujian, Anti Curang): South adalah saudara (sibling) dari Center yang men-scroll,
			// bukan elemen mengambang di atas konten. Semua kontrol pada tab-tab ini SUDAH
			// tersimpan seketika saat diubah (onChange/onCheck -> simpan langsung ke DB), jadi
			// tombol "Simpan" di sini murni penegasan/penutup jendela bagi pengguna, tanpa
			// mengubah logic bisnis apapun.
			final Component detailWindow = detail;
			South southUjian = new South();
			ais.ui.util.ZkCompat.setFlex(southUjian, true);
			southUjian.setParent(borderlayoutUjian);
			southUjian.setStyle("background:#fff;border-top:1px solid #e2e8f0;box-shadow:0 -2px 6px rgba(0,0,0,0.06);");

			Toolbar toolbarFooterUjian = new Toolbar();
			toolbarFooterUjian.setParent(southUjian);
			toolbarFooterUjian.setStyle("display:flex;justify-content:flex-end;gap:10px;padding:10px 14px;width:100%;");

			MyToolbarbuttonConfig btnBatalUjian = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
					"/img/cancel.gif");
			btnBatalUjian.setTooltiptext(Common.getBahasaConfig("Tutup jendela"));
			btnBatalUjian.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (detailWindow instanceof Window) {
						((Window) detailWindow).detach();
					}
				}
			});
			btnBatalUjian.setParent(toolbarFooterUjian);

			MyToolbarbuttonConfig btnSimpanUjian = new MyToolbarbuttonConfig(Common.getBahasaConfig("Simpan"),
					"/img/save.gif");
			btnSimpanUjian.setTooltiptext(Common.getBahasaConfig("Simpan & tutup jendela"));
			btnSimpanUjian.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (detailWindow instanceof Window) {
						((Window) detailWindow).detach();
					}
				}
			});
			btnSimpanUjian.setParent(toolbarFooterUjian);

		} else {
			parent = detail;
		}

		return parent;

	}

	// =====================================================================================
	// Tab "Anti Curang" (CBT) PER-UJIAN — form terikat ke kolom ac_* di PertemuanPunyaUjian
	// =====================================================================================

	/** Setter boolean per-field (agar builder generik {@link #acCheckbox} bisa dipakai ulang). */
	private interface AcBoolSetter {
		void set(Boolean v);
	}

	/** Setter integer per-field. */
	private interface AcIntSetter {
		void set(Integer v);
	}

	/** Setter string per-field. */
	private interface AcStrSetter {
		void set(String v);
	}

	/** Setter double per-field. */
	private interface AcDblSetter {
		void set(Double v);
	}

	/** Simpan perubahan ujian ke DB (dipanggil tiap kontrol anti-curang berubah). */
	private void simpanAntiCurang(PertemuanPunyaUjian ppu) {
		try {
			// JANGAN session.refresh(ppu) di sini — refresh menimpa setter yang baru dipanggil
			// sehingga perubahan hilang sebelum disimpan. ppu masih attached ke session.
			Common.refreshUpdate(ppu);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun isi tab <b>"Anti Curang"</b> (CBT) sebuah ujian. Tiap kontrol terikat LANGSUNG ke kolom
	 * {@code ac_*} pada {@link PertemuanPunyaUjian}: setiap perubahan disimpan seketika
	 * ({@code setAntiCurang*} + {@link #simpanAntiCurang}). Nilai awal dibaca dari getter entity yang
	 * mengembalikan DEFAULT aman saat kolom masih null, sehingga ujian lama tampil dengan perilaku default.
	 * Pengaturan ini menggantikan konfigurasi anti-curang GLOBAL yang lama (dihapus dari KonfigurasiNewAction).
	 *
	 * @param parent kontainer tab (Tabpanel).
	 * @param ppu    ujian yang sedang dikelola.
	 */
	private void isiTabAntiCurang(Component parent, final PertemuanPunyaUjian ppu) {
		org.zkoss.zul.Vbox box = new org.zkoss.zul.Vbox();
		box.setWidth("100%");
		box.setStyle("padding:12px;");
		box.setParent(parent);

		new ais.ui.util.MyHtml("<div style='padding:6px 2px 10px;color:#475569;'>Pengaturan anti-curang berlaku "
				+ "<b>HANYA untuk ujian ini</b> (tidak lagi terpusat di konfigurasi). "
				+ "Nilai awal = default aman.</div>").setParent(box);

		acCheckbox(box, "Aktifkan fitur anti-curang saat ujian ini berlangsung "
				+ "(jika non-aktif, semua sub-fitur di bawah tidak berjalan).", ppu.getAntiCurangAktif(),
				new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangAktif(v);
					}
				}, ppu);
		acIntbox(box, "Batas pelanggaran sebelum ujian dianggap selesai otomatis (0 = tanpa batas, hanya dicatat):",
				ppu.getAntiCurangBatasPelanggaran(), new AcIntSetter() {
					public void set(Integer v) {
						ppu.setAntiCurangBatasPelanggaran(v);
					}
				}, ppu);
		acCheckbox(box, "Masuk mode layar penuh (fullscreen) otomatis saat ujian dimulai.",
				ppu.getAntiCurangAktifkanFullscreen(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangAktifkanFullscreen(v);
					}
				}, ppu);
		acCheckbox(box, "Deteksi peserta berpindah tab/aplikasi (setiap tab disembunyikan = 1 pelanggaran).",
				ppu.getAntiCurangDeteksiPindahTab(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangDeteksiPindahTab(v);
					}
				}, ppu);
		acCheckbox(box, "Deteksi peserta beralih ke jendela lain (Alt+Tab, klik di luar, Ctrl+Alt+Del).",
				ppu.getAntiCurangDeteksiBlurJendela(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangDeteksiBlurJendela(v);
					}
				}, ppu);
		acIntbox(box, "Jeda deteksi blur (milidetik) agar satu Alt+Tab tak terhitung ganda (default 5000):",
				ppu.getAntiCurangCooldownBlurMs(), new AcIntSetter() {
					public void set(Integer v) {
						ppu.setAntiCurangCooldownBlurMs(v);
					}
				}, ppu);
		acCheckbox(box, "Deteksi peserta keluar dari mode layar penuh selama ujian.",
				ppu.getAntiCurangDeteksiKeluarFullscreen(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangDeteksiKeluarFullscreen(v);
					}
				}, ppu);
		acCheckbox(box, "Nonaktifkan klik kanan (context menu) agar soal tak mudah disalin.",
				ppu.getAntiCurangBlokirKlikKanan(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangBlokirKlikKanan(v);
					}
				}, ppu);
		acCheckbox(box, "Blokir shortcut berbahaya (Ctrl+W/T/N/R, F5, F12, Alt+F4).",
				ppu.getAntiCurangBlokirShortcut(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangBlokirShortcut(v);
					}
				}, ppu);
		acCheckbox(box, "Tampilkan konfirmasi saat mencoba menutup/meninggalkan halaman ujian (beforeunload).",
				ppu.getAntiCurangPeringatanKeluarHalaman(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangPeringatanKeluarHalaman(v);
					}
				}, ppu);
		acCheckbox(box, "Blokir tombol tangkap layar (PrintScreen) selama ujian berlangsung.",
				ppu.getAntiCurangBlokirTangkapLayar(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangBlokirTangkapLayar(v);
					}
				}, ppu);
		acCheckbox(box, "Jangan izinkan ujian ini dikerjakan di lebih dari satu perangkat secara bersamaan (default aktif).",
				ppu.getAntiCurangLarangMultiDevice(), new AcBoolSetter() {
					public void set(Boolean v) {
						ppu.setAntiCurangLarangMultiDevice(v);
					}
				}, ppu);
		new ais.ui.util.MyHtml("<div style='padding:6px 2px 4px;font-weight:700;color:#334155;font-size:13px;'>Restriksi Lokasi Ujian</div>").setParent(box);
		new ais.ui.util.MyHtml("<div style='font-size:12px;color:#64748b;padding-bottom:8px;'>Kosongkan Latitude/Longitude atau set Radius=0 untuk menonaktifkan. Peserta hanya boleh mengerjakan ujian dalam radius yang ditentukan dari titik pusat.</div>").setParent(box);
		acDoublebox(box, "Latitude pusat lokasi ujian (contoh: -6.200000):", ppu.getAntiCurangLokasiLatitude(), new AcDblSetter() {
			public void set(Double v) { ppu.setAntiCurangLokasiLatitude(v); }
		}, ppu);
		acDoublebox(box, "Longitude pusat lokasi ujian (contoh: 106.800000):", ppu.getAntiCurangLokasiLongitude(), new AcDblSetter() {
			public void set(Double v) { ppu.setAntiCurangLokasiLongitude(v); }
		}, ppu);

		// Feature 5: tombol "Pilih di Peta" — buka jendela OSM iframe + form koordinat ZK.
		org.zkoss.zul.Div mapBtnDiv = new org.zkoss.zul.Div();
		mapBtnDiv.setStyle("margin:6px 0 10px;");
		mapBtnDiv.setParent(box);
		new ais.ui.util.MyHtml("<div style='font-size:11px;color:#64748b;margin-bottom:6px;'>"
				+ "Gunakan tombol di bawah untuk melihat lokasi di peta (OpenStreetMap) dan mengisi koordinat secara visual.</div>").setParent(mapBtnDiv);
		final Toolbarbutton btnPilihPeta = new MyToolbarbuttonConfig("Pilih di Peta", "/img/Configure.gif");
		btnPilihPeta.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				final double latDbl = ppu.getAntiCurangLokasiLatitude() != null
						? ppu.getAntiCurangLokasiLatitude() : -6.2;
				final double lngDbl = ppu.getAntiCurangLokasiLongitude() != null
						? ppu.getAntiCurangLokasiLongitude() : 106.8;

				final Window mapWin = new Window("Pilih Lokasi di Peta", "normal", true);
				mapWin.setClosable(true);
				mapWin.setSizable(true);
				mapWin.setWidth("820px");
				mapWin.setHeight("545px");
				mapWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				final Vbox vb = new Vbox();
				vb.setWidth("100%");
				vb.setStyle("padding:10px;");
				vb.setParent(mapWin);

				// OSM embed iframe (marker at current coordinates)
				String iframeSrc = "https://www.openstreetmap.org/export/embed.html?bbox="
						+ (lngDbl - 0.05) + "%2C" + (latDbl - 0.05) + "%2C"
						+ (lngDbl + 0.05) + "%2C" + (latDbl + 0.05)
						+ "&amp;layer=mapnik&amp;marker=" + latDbl + "%2C" + lngDbl;
				new Html("<iframe width='100%' height='340' frameborder='0' scrolling='no'"
						+ " marginheight='0' marginwidth='0' src='" + iframeSrc + "'"
						+ " style='border-radius:8px;border:1px solid #e2e8f0;'></iframe>").setParent(vb);

				new Html("<div style='font-size:11px;color:#64748b;margin:8px 0 4px;'>"
						+ "Klik kanan pada titik di peta untuk melihat koordinat, kemudian masukkan nilai Latitude dan Longitude di bawah, lalu klik <b>Simpan Lokasi</b>.</div>").setParent(vb);

				final Hbox coordRow = new Hbox();
				coordRow.setStyle("align-items:center;gap:10px;margin-top:6px;flex-wrap:wrap;");
				coordRow.setParent(vb);
				new org.zkoss.zul.Label("Latitude:").setParent(coordRow);
				final Textbox tbLat = new Textbox(String.valueOf(latDbl));
				tbLat.setWidth("130px");
				tbLat.setParent(coordRow);
				new org.zkoss.zul.Label("Longitude:").setParent(coordRow);
				final Textbox tbLng = new Textbox(String.valueOf(lngDbl));
				tbLng.setWidth("130px");
				tbLng.setParent(coordRow);

				final MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan Lokasi", "/img/save.gif");
				btnSimpan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						try {
							double newLat = Double.parseDouble(tbLat.getValue().trim());
							double newLng = Double.parseDouble(tbLng.getValue().trim());
							ppu.setAntiCurangLokasiLatitude(newLat);
							ppu.setAntiCurangLokasiLongitude(newLng);
							simpanAntiCurang(ppu);
							mapWin.detach();
							MyMessageboxConfig.show(
									"Lokasi berhasil disimpan. Lat: " + newLat + ", Lng: " + newLng,
									"Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						} catch (NumberFormatException nfe) {
							MyMessageboxConfig.show(
									"Format koordinat tidak valid. Contoh Latitude: -6.200000, Longitude: 106.800000.",
									"Format Tidak Valid", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				});
				btnSimpan.setParent(coordRow);

				mapWin.onModal();
			}
		});
		btnPilihPeta.setParent(mapBtnDiv);

		acIntbox(box, "Radius lokasi (meter, 0 = nonaktif):", ppu.getAntiCurangLokasiRadius(), new AcIntSetter() {
			public void set(Integer v) { ppu.setAntiCurangLokasiRadius(v); }
		}, ppu);
		acTextarea(box, "Teks peringatan: pindah tab/aplikasi", ppu.getAntiCurangPesanPindahTab(), new AcStrSetter() {
			public void set(String v) {
				ppu.setAntiCurangPesanPindahTab(v);
			}
		}, ppu);
		acTextarea(box, "Teks peringatan: beralih jendela (Alt+Tab)", ppu.getAntiCurangPesanBlurJendela(),
				new AcStrSetter() {
					public void set(String v) {
						ppu.setAntiCurangPesanBlurJendela(v);
					}
				}, ppu);
		acTextarea(box, "Teks peringatan: keluar fullscreen", ppu.getAntiCurangPesanKeluarFullscreen(),
				new AcStrSetter() {
					public void set(String v) {
						ppu.setAntiCurangPesanKeluarFullscreen(v);
					}
				}, ppu);
		acTextarea(box, "Teks peringatan: sebelum meninggalkan halaman ujian", ppu.getAntiCurangPesanKeluarHalaman(),
				new AcStrSetter() {
					public void set(String v) {
						ppu.setAntiCurangPesanKeluarHalaman(v);
					}
				}, ppu);
	}

	/** Baris checkbox anti-curang (label = deskripsi), tersimpan seketika saat di-check/uncheck. */
	private void acCheckbox(org.zkoss.zul.Vbox box, String label, Boolean nilai, final AcBoolSetter setter,
			final PertemuanPunyaUjian ppu) {
		final MyCheckboxConfig cb = new MyCheckboxConfig(label);
		cb.setChecked(nilai == null || nilai.booleanValue());
		cb.setStyle("display:block;margin:5px 0;");
		cb.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				setter.set(Boolean.valueOf(cb.isChecked()));
				simpanAntiCurang(ppu);
			}
		});
		box.appendChild(cb);
	}

	/** Baris input angka anti-curang (label + Intbox), tersimpan saat onChange. */
	private void acIntbox(org.zkoss.zul.Vbox box, String label, Integer nilai, final AcIntSetter setter,
			final PertemuanPunyaUjian ppu) {
		org.zkoss.zul.Div d = new org.zkoss.zul.Div();
		d.setStyle("margin:5px 0;");
		d.setParent(box);
		org.zkoss.zul.Label l = new org.zkoss.zul.Label(label + " ");
		l.setParent(d);
		// FIX NPE: nilai bisa null (getter belum pernah diisi) -> konstruktor Intbox(Integer)
		// ZK meledak saat memformat null. Default ke 0, konsisten dgn label "0 = nonaktif".
		final org.zkoss.zul.Intbox ib = new org.zkoss.zul.Intbox(nilai == null ? Integer.valueOf(0) : nilai);
		ib.setWidth("110px");
		ib.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				setter.set(ib.getValue());
				simpanAntiCurang(ppu);
			}
		});
		ib.setParent(d);
	}

	/** Baris input desimal anti-curang (label + Doublebox), tersimpan saat onChange. */
	private void acDoublebox(Component parent, String label, Double initialVal, final AcDblSetter setter,
			final PertemuanPunyaUjian ppu) {
		org.zkoss.zul.Hbox row = new org.zkoss.zul.Hbox();
		row.setParent(parent);
		row.setStyle("align-items:center;gap:10px;padding:4px 0;");
		new ais.ui.util.MyLabelConfig(label).setParent(row);
		final org.zkoss.zul.Doublebox db = new org.zkoss.zul.Doublebox();
		db.setValue(initialVal);
		db.setParent(row);
		db.addEventListener("onChange", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				setter.set(db.getValue());
				simpanAntiCurang(ppu);
			}
		});
	}

	/** Baris textarea anti-curang (label di atas + Textbox 2 baris), tersimpan saat onChange. */
	private void acTextarea(org.zkoss.zul.Vbox box, String label, String nilai, final AcStrSetter setter,
			final PertemuanPunyaUjian ppu) {
		org.zkoss.zul.Div d = new org.zkoss.zul.Div();
		d.setStyle("margin:7px 0;");
		d.setParent(box);
		org.zkoss.zul.Label l = new org.zkoss.zul.Label(label);
		l.setStyle("display:block;font-weight:600;margin-bottom:2px;");
		l.setParent(d);
		final org.zkoss.zul.Textbox tb = new org.zkoss.zul.Textbox(nilai);
		tb.setMultiline(true);
		tb.setRows(2);
		tb.setWidth("95%");
		tb.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				setter.set(tb.getValue());
				simpanAntiCurang(ppu);
			}
		});
		tb.setParent(d);
	}

	// =====================================================================================
	// Feature 2: Tab "Pengaturan OBE" — bobot Sub-CPMK per ujian + agregat dari ujian/tugas lain
	// =====================================================================================

	/**
	 * Membangun isi tab <b>"Pengaturan OBE"</b>.
	 * Menampilkan setiap Sub-CPMK (FormatNilai) yang dipilih di ujian ini beserta Doublebox bobot-nya,
	 * dilengkapi ringkasan bobot dari ujian/tugas lain dalam Perkuliahan yang sama.
	 */
	private void isiTabPengaturanOBE(Component parent, final PertemuanPunyaUjian ppu) {
		org.zkoss.zul.Vbox vbox = new org.zkoss.zul.Vbox();
		vbox.setWidth("100%");
		vbox.setStyle("padding:12px;");
		vbox.setParent(parent);

		if (ppu == null || ppu.getPertemuan() == null || ppu.getPertemuan().getPerkuliahan() == null) {
			new ais.ui.util.MyHtml("<div style='color:#64748b;padding:8px;'>"
				+ "(tidak dapat hitung total—tidak ada konteks perkuliahan)</div>").setParent(vbox);
			return;
		}

		final ais.database.model.Perkuliahan perkuliahan = ppu.getPertemuan().getPerkuliahan();
		boolean obe = perkuliahan.getKurikulum() != null
			&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap());

		if (!obe) {
			new ais.ui.util.MyHtml("<div style='color:#64748b;padding:8px;'>"
				+ "(Perkuliahan ini tidak menggunakan kurikulum OBE; Sub-CPMK tidak tersedia.)</div>").setParent(vbox);
			return;
		}

		try {
			Session session = HibernateUtil.currentSession();
			java.util.List<ais.database.model.FormatNilai> formatNilais =
				Common.getFormatNilais(session, perkuliahan);

			final JSONObject jsonPpu = new JSONObject(ppu.getFormatNilais() == null ? "{}" : ppu.getFormatNilais());

			// Kumpulkan FormatNilai yang dipilih di PPU ini
			java.util.List<ais.database.model.FormatNilai> fnDipilih =
				new java.util.ArrayList<ais.database.model.FormatNilai>();
			for (ais.database.model.FormatNilai fn : formatNilais) {
				if (fn != null && fn.getId() != null && fn.getStatusPertemuan() != null
						&& !jsonPpu.isNull(fn.getId().toString())) {
					fnDipilih.add(fn);
				}
			}

			if (fnDipilih.isEmpty()) {
				new ais.ui.util.MyHtml("<div style='color:#64748b;padding:8px;'>"
					+ "(Belum ada Sub-CPMK yang dipilih untuk ujian ini. "
					+ "Atur terlebih dahulu melalui tombol Pengaturan Data Ujian.)</div>").setParent(vbox);
				return;
			}

			new ais.ui.util.MyHtml("<div style='font-weight:700;color:#334155;font-size:13px;padding-bottom:8px;'>"
				+ "Bobot Sub-CPMK — ujian ini + dari ujian/tugas lain</div>").setParent(vbox);

			for (final ais.database.model.FormatNilai fn : fnDipilih) {
				if (fn == null || fn.getId() == null) continue;
				final String fnId = fn.getId().toString();
				final String bobotKey = fnId + "_bobot";

				double bobotSaatIniTmp = jsonPpu.isNull(bobotKey) ? 100.0 : jsonPpu.optDouble(bobotKey, 100.0);
				final double bobotSaatIni = bobotSaatIniTmp;

				// Hitung bobot dari PPU lain dalam perkuliahan yang sama
				double bobotLainTmp = 0.0;
				String pesanKetTmp = "";
				try {
					bobotLainTmp = hitungBobotPPULain(session, perkuliahan, ppu, fn);
				} catch (Exception eLain) {
					pesanKetTmp = "(tidak dapat hitung total—tidak ada konteks perkuliahan)";
					ais.common.ErrorAuditUtil.record(eLain, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:isiTabPengaturanOBE-hitung");
				}
				final double bobotLain = bobotLainTmp;
				final double totalBobot = bobotSaatIni + bobotLain;
				final String pesanKet = pesanKetTmp;

				org.zkoss.zul.Div rowDiv = new org.zkoss.zul.Div();
				rowDiv.setStyle("margin-bottom:10px;border:1px solid #e2e8f0;border-radius:6px;"
					+ "padding:8px 12px;background:#f8fafc;");
				rowDiv.setParent(vbox);

				org.zkoss.zul.Hbox hbox = new org.zkoss.zul.Hbox();
				hbox.setStyle("align-items:center;gap:10px;");
				ais.ui.util.MenuAksiBaris.pasang(hbox);
				hbox.setParent(rowDiv);

				new org.zkoss.zul.Label(fn.getNama() + ":").setParent(hbox);

				final org.zkoss.zul.Doublebox bobotBox = new org.zkoss.zul.Doublebox();
				bobotBox.setValue(Double.valueOf(bobotSaatIni));
				bobotBox.setWidth("80px");
				bobotBox.setParent(hbox);
				new org.zkoss.zul.Label("%").setParent(hbox);

				bobotBox.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						if (ppu.getId() != null) s.refresh(ppu);
						JSONObject j = new JSONObject(ppu.getFormatNilais() == null ? "{}" : ppu.getFormatNilais());
						if (bobotBox.getValue() != null) {
							j.put(bobotKey, bobotBox.getValue().doubleValue());
						} else {
							j.remove(bobotKey);
						}
						ppu.setFormatNilais(j.toString());
						Common.refreshUpdate(s, ppu);
					}
				});

				// Baris ringkasan: "[fn.getNama()]: Bobot saat ini X% + Dari Ujian/Tugas lain: Y% = Total Z%"
				if (pesanKet.isEmpty()) {
					String warna = totalBobot > 100.5 ? "#dc2626" : (totalBobot >= 99.5 ? "#16a34a" : "#64748b");
					String summary = fn.getNama() + ": Bobot saat ini "
						+ String.format("%.0f", Double.valueOf(bobotSaatIni)) + "% + Dari Ujian/Tugas lain: "
						+ String.format("%.0f", Double.valueOf(bobotLain)) + "% = Total "
						+ String.format("%.0f", Double.valueOf(totalBobot)) + "%";
					new ais.ui.util.MyHtml("<div style='font-size:11px;color:" + warna + ";margin-top:4px;'>"
						+ ais.ui.util.DashboardUiKit.esc(summary) + "</div>").setParent(rowDiv);
				} else {
					String summary = fn.getNama() + ": Bobot saat ini "
						+ String.format("%.0f", Double.valueOf(bobotSaatIni)) + "% + Dari Ujian/Tugas lain: "
						+ pesanKet;
					new ais.ui.util.MyHtml("<div style='font-size:11px;color:#94a3b8;margin-top:4px;'>"
						+ ais.ui.util.DashboardUiKit.esc(summary) + "</div>").setParent(rowDiv);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:isiTabPengaturanOBE");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menghitung total bobot yang dialokasikan ke {@link ais.database.model.FormatNilai} {@code fn}
	 * oleh semua {@link PertemuanPunyaUjian} lain dalam Perkuliahan yang sama (bukan {@code thisPpu}).
	 * Bobot disimpan di JSON formatNilais dengan kunci {@code "<fnId>_bobot"} (default 100).
	 */
	@SuppressWarnings("unchecked")
	private double hitungBobotPPULain(Session session, ais.database.model.Perkuliahan perkuliahan,
			PertemuanPunyaUjian thisPpu, ais.database.model.FormatNilai fn) {
		double total = 0.0;
		if (perkuliahan == null || perkuliahan.getId() == null || fn == null || fn.getId() == null) {
			return total;
		}
		final String fnId = fn.getId().toString();
		final String bobotKey = fnId + "_bobot";
		try {
			Criteria cr = session.createCriteria(PertemuanPunyaUjian.class)
				.createAlias("pertemuan", "pt")
				.add(Restrictions.eq("pt.perkuliahan", perkuliahan));
			if (thisPpu != null && thisPpu.getId() != null) {
				cr.add(Restrictions.ne("id", thisPpu.getId()));
			}
			java.util.List<PertemuanPunyaUjian> otherPpus = cr.list();
			for (PertemuanPunyaUjian other : otherPpus) {
				String fnsStr = other.getFormatNilais();
				if (fnsStr == null || fnsStr.trim().isEmpty()) continue;
				try {
					JSONObject j = new JSONObject(fnsStr);
					if (!j.isNull(fnId)) {
						double bobot = j.isNull(bobotKey) ? 100.0 : j.optDouble(bobotKey, 100.0);
						total += bobot;
					}
				} catch (Exception ej) { /* abaikan parsing error */ }
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:hitungBobotPPULain");
		}
		return total;
	}

	// =====================================================================================
	// Feature 6: Sub-CPMK dropdown per soal — agar dosen bisa langsung assign Sub-CPMK
	// dari kartu soal tanpa harus membuka tab Pengaturan.
	// =====================================================================================

	/**
	 * Menambahkan Combobox "Sub-CPMK" ke bawah kartu soal.
	 * Hanya aktif jika PPU adalah ujian OBE (formatNilais non-default).
	 */
	@SuppressWarnings("unchecked")
	private void tambahanSubCpmkDropdown(final Row rowParent, final UjianPunyaSoal ups,
			final PertemuanPunyaUjian ppu) {
		if (ppu == null || ups == null) return;
		String fnsStr = ppu.getFormatNilais();
		if (fnsStr == null || fnsStr.trim().isEmpty() || "{}".equals(fnsStr.trim())) return;
		if (ppu.getPertemuan() == null || ppu.getPertemuan().getPerkuliahan() == null) return;

		try {
			final ais.database.model.Perkuliahan perk = ppu.getPertemuan().getPerkuliahan();
			boolean obe = perk.getKurikulum() != null
				&& perk.getKurikulum().apakahObe(perk.getTahunAjaran(), perk.getGanjilGenap());
			if (!obe) return;

			Session session = HibernateUtil.currentSession();
			java.util.List<ais.database.model.FormatNilai> allFns = Common.getFormatNilais(session, perk);
			final JSONObject jFns = new JSONObject(ppu.getFormatNilais() == null ? "{}" : ppu.getFormatNilais());

			// Kumpulkan FormatNilai yang aktif di PPU ini
			final java.util.List<ais.database.model.FormatNilai> activeFns =
				new java.util.ArrayList<ais.database.model.FormatNilai>();
			for (ais.database.model.FormatNilai fn : allFns) {
				if (fn != null && fn.getId() != null && fn.getStatusPertemuan() != null
						&& !jFns.isNull(fn.getId().toString())) {
					activeFns.add(fn);
				}
			}
			if (activeFns.isEmpty()) return;

			// Nomor urut soal ini
			final int thisNomor = ups.getNomorUrut() == null ? 0 : ups.getNomorUrut().intValue();

			// Cari fn yang saat ini menaungi nomor soal ini
			ais.database.model.FormatNilai fnTerpilihTmp = null;
			outer:
			for (ais.database.model.FormatNilai fn : activeFns) {
				String val = jFns.isNull(fn.getId().toString()) ? ""
					: (jFns.get(fn.getId().toString()) + "");
				for (String part : val.split(",")) {
					try {
						if (part.trim().length() > 0 && Integer.parseInt(part.trim()) == thisNomor) {
							fnTerpilihTmp = fn;
							break outer;
						}
					} catch (Exception ex) { /* abaikan */ }
				}
			}
			final ais.database.model.FormatNilai fnTerpilih = fnTerpilihTmp;

			// Bangun UI
			org.zkoss.zul.Div cpmkDiv = new org.zkoss.zul.Div();
			cpmkDiv.setStyle("padding:6px 0 4px;border-top:1px dashed #e2e8f0;margin-top:4px;");
			cpmkDiv.setParent(rowParent);

			org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
			hb.setStyle("align-items:center;gap:8px;");
			hb.setParent(cpmkDiv);
			new org.zkoss.zul.Label("Sub-CPMK:").setParent(hb);

			final org.zkoss.zul.Combobox cbSubCpmk = new org.zkoss.zul.Combobox();
			cbSubCpmk.setReadonly(true);
			cbSubCpmk.setWidth("200px");

			org.zkoss.zul.Comboitem itemNone = new org.zkoss.zul.Comboitem("— Tidak ada —");
			itemNone.setValue(null);
			itemNone.setParent(cbSubCpmk);

			org.zkoss.zul.Comboitem selectedItem = itemNone;
			for (ais.database.model.FormatNilai fn : activeFns) {
				org.zkoss.zul.Comboitem item = new org.zkoss.zul.Comboitem(fn.getNama());
				item.setValue(fn.getId().toString());
				item.setParent(cbSubCpmk);
				if (fnTerpilih != null && fn.getId().equals(fnTerpilih.getId())) {
					selectedItem = item;
				}
			}
			cbSubCpmk.setSelectedItem(selectedItem);
			cbSubCpmk.setParent(hb);

			cbSubCpmk.addEventListener("onSelect", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Session s = HibernateUtil.currentSession();
					if (ppu.getId() != null) s.refresh(ppu);
					JSONObject j = new JSONObject(ppu.getFormatNilais() == null ? "{}" : ppu.getFormatNilais());

					// Hapus thisNomor dari semua fn
					for (ais.database.model.FormatNilai fn : activeFns) {
						String fnIdStr = fn.getId().toString();
						String val = j.isNull(fnIdStr) ? "" : (j.get(fnIdStr) + "");
						StringBuilder sb = new StringBuilder();
						for (String part : val.split(",")) {
							part = part.trim();
							if (part.isEmpty()) continue;
							try {
								if (Integer.parseInt(part) != thisNomor) {
									if (sb.length() > 0) sb.append(",");
									sb.append(part);
								}
							} catch (Exception ex) {
								if (sb.length() > 0) sb.append(",");
								sb.append(part);
							}
						}
						j.put(fnIdStr, sb.toString());
					}

					// Tambahkan ke fn yang dipilih
					org.zkoss.zul.Comboitem selected = cbSubCpmk.getSelectedItem();
					if (selected != null && selected.getValue() != null) {
						String selFnId = selected.getValue().toString();
						String val = j.isNull(selFnId) ? "" : (j.get(selFnId) + "");
						val = val.trim().isEmpty() ? ("" + thisNomor) : (val + "," + thisNomor);
						j.put(selFnId, val);
					}

					ppu.setFormatNilais(j.toString());
					Common.refreshUpdate(s, ppu);
				}
			});

		} catch (Exception eF6) {
			ais.common.ErrorAuditUtil.record(eF6, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:tambahanSubCpmkDropdown");
		}
	}

	/** Popup "Buat Soal Via AI": parameter → generate (streaming) → insert BankSoal + BankSoalDetail + tautan ujian. */
	private void bukaBuatSoalAi(final Pertemuan pertemuan) throws Exception {
		final String namaMk;
		if (ujian != null && ujian.getMatakuliah() != null && ujian.getMatakuliah().getNama() != null) {
			namaMk = ujian.getMatakuliah().getNama();
		} else if (pertemuan != null && pertemuan.getPerkuliahan() != null
				&& pertemuan.getPerkuliahan().getMatakuliah() != null
				&& pertemuan.getPerkuliahan().getMatakuliah().getNama() != null) {
			namaMk = pertemuan.getPerkuliahan().getMatakuliah().getNama();
		} else {
			namaMk = "";
		}
		StringBuilder pemb = new StringBuilder();
		if (pertemuan != null) {
			if (pertemuan.getTopik() != null && pertemuan.getTopik().trim().length() > 0) {
				pemb.append(pertemuan.getTopik().trim()).append("\n");
			}
			try {
				if (pertemuan.getBukuRujukan1() != null && pertemuan.getBukuRujukan1().trim().length() > 0) {
					pemb.append("Rujukan: ").append(pertemuan.getBukuRujukan1().trim()).append("\n");
				}
			} catch (Exception e) {
			}
			try {
				if (pertemuan.getCatatan() != null && pertemuan.getCatatan().trim().length() > 0) {
					pemb.append("Catatan: ").append(pertemuan.getCatatan().trim()).append("\n");
				}
			} catch (Exception e) {
			}
		}
		final String pembahasan = pemb.toString();

		final ais.ui.util.MyWindow win = new ais.ui.util.MyWindow(Common.getBahasaConfig("Buat Soal Via AI"), "none",
				true);
		win.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		win.setWidth("560px");
		Vbox vb = new Vbox();
		vb.setStyle("padding:16px;width:100%;box-sizing:border-box;");
		vb.setWidth("100%");
		vb.setParent(win);

		Label l1 = new Label(Common.getBahasaConfig("Nama Matakuliah") + ":");
		l1.setStyle("font-weight:bold;font-size:11px;");
		vb.appendChild(l1);
		final Textbox tNama = new Textbox(namaMk);
		tNama.setReadonly(true);
		tNama.setWidth("100%");
		vb.appendChild(tNama);

		Label l2 = new Label(Common.getBahasaConfig("Pembahasan / Topik (dari Pertemuan)") + ":");
		l2.setStyle("font-weight:bold;font-size:11px;margin-top:8px;");
		vb.appendChild(l2);
		final Textbox tPemb = new Textbox(pembahasan);
		tPemb.setReadonly(true);
		tPemb.setMultiline(true);
		tPemb.setRows(3);
		tPemb.setWidth("100%");
		vb.appendChild(tPemb);

		Label l3 = new Label(Common.getBahasaConfig("Topik Soal (tentang apa)") + ":");
		l3.setStyle("font-weight:bold;font-size:11px;margin-top:8px;");
		vb.appendChild(l3);
		final Textbox tTopik = new Textbox();
		tTopik.setWidth("100%");
		vb.appendChild(tTopik);

		Label l4 = new Label(Common.getBahasaConfig("Jumlah Soal") + ":");
		l4.setStyle("font-weight:bold;font-size:11px;margin-top:8px;");
		vb.appendChild(l4);
		final Intbox tJumlah = new Intbox(1);
		tJumlah.setWidth("90px");
		vb.appendChild(tJumlah);

		// Tipe soal MENGIKUTI jenis ujian di data master (Ujian.java) — tidak dipilih manual.
		final boolean isPg = ujian != null && BankSoal.PILIHAN_GANDA.equals(ujian.getJenis());
		Label l5 = new Label(Common.getBahasaConfig("Tipe Soal (mengikuti ujian)") + ": "
				+ (isPg ? Common.getBahasaConfig("Pilihan Ganda") : Common.getBahasaConfig("Essay")));
		l5.setStyle("font-weight:bold;font-size:11px;margin-top:8px;color:#334155;");
		vb.appendChild(l5);

		final Hbox rowOpsi = new Hbox();
		rowOpsi.setStyle("margin-top:6px;");
		rowOpsi.setVisible(isPg);
		Label l6 = new Label(Common.getBahasaConfig("Jumlah Opsi Jawaban") + ":");
		l6.setStyle("font-size:11px;");
		rowOpsi.appendChild(l6);
		final Intbox tOpsi = new Intbox(5);
		tOpsi.setWidth("60px");
		rowOpsi.appendChild(tOpsi);
		vb.appendChild(rowOpsi);

		Hbox bb = new Hbox();
		bb.setStyle("margin-top:16px;");
		vb.appendChild(bb);
		MyToolbarbuttonConfig btnGen = new MyToolbarbuttonConfig(Common.getBahasaConfig("Generate"),
				"/img/svg/sparkles.svg");
		btnGen.setStyle("color:#fff;background-color:#16a34a;border-radius:6px;padding:6px 14px;border:none;"
				+ "cursor:pointer;margin-right:6px;");
		btnGen.setParent(bb);
		btnGen.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int jml = (tJumlah.getValue() == null) ? 1 : tJumlah.getValue().intValue();
				if (jml < 1) {
					jml = 1;
				}
				if (jml > 30) {
					jml = 30;
				}
				int opsi = (tOpsi.getValue() == null) ? 5 : tOpsi.getValue().intValue();
				if (opsi < 2) {
					opsi = 2;
				}
				if (opsi > 8) {
					opsi = 8;
				}
				final int jmlF = jml;
				final int opsiF = opsi;
				final String topikSoal = (tTopik.getValue() == null) ? "" : tTopik.getValue().trim();
				win.detach();
				String konteksMk = bangunKonteksMkSoal(pertemuan);
				String soalAda = daftarSoalExisting();
				String prompt = bangunPromptSoalAi(namaMk, pembahasan, topikSoal, jmlF, isPg, opsiF, konteksMk,
						soalAda);
				GenerateAiHelper.jalankanAiStreaming(Common.getBahasaConfig("Buat Soal Via AI"), prompt,
						new GenerateAiHelper.HasilAi() {
							@Override
							public void selesai(String resp) throws Exception {
								int n = insertSoalAiDariJson(resp, isPg, pertemuan);
								ais.ui.util.MyMessageboxConfig.show(
										n + " " + Common.getBahasaConfig("soal berhasil dibuat via AI."),
										Common.getBahasaConfig("Informasi"), ais.ui.util.MyMessageboxConfig.OK,
										ais.ui.util.MyMessageboxConfig.INFORMATION);
								loadData(true);
							}
						}, 3072);
			}
		});
		MyToolbarbuttonConfig btnCancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
				"/img/svg/close-circle-line.svg");
		btnCancel.setStyle("color:#fff;background-color:#dc2626;border-radius:6px;padding:6px 14px;border:none;"
				+ "cursor:pointer;");
		btnCancel.setParent(bb);
		btnCancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});

		win.onModal();
	}

	private String bangunPromptSoalAi(String namaMk, String pembahasan, String topikSoal, int jml, boolean isPg,
			int opsi, String konteksMk, String soalAda) {
		StringBuilder p = new StringBuilder();
		if (konteksMk != null && konteksMk.length() > 0) {
			p.append(konteksMk).append("\n");
		}
		p.append("Buatkan ").append(jml).append(" soal ").append(isPg ? "PILIHAN GANDA" : "ESSAY");
		p.append(" untuk mata kuliah \"").append(namaMk).append("\", tingkat perguruan tinggi, Bahasa Indonesia.\n");
		if (topikSoal != null && topikSoal.length() > 0) {
			p.append("Topik khusus soal: ").append(topikSoal).append("\n");
		}
		if (pembahasan != null && pembahasan.trim().length() > 0) {
			p.append("Konteks/pembahasan pertemuan: ").append(pembahasan.trim()).append("\n");
		}
		if (soalAda != null && soalAda.length() > 0) {
			p.append("\nSOAL-SOAL YANG SUDAH ADA (JANGAN membuat soal yang sama/mirip; jadikan referensi untuk "
					+ "membuat soal BARU yang berbeda & saling melengkapi):\n").append(soalAda);
		}
		p.append("\n");
		if (isPg) {
			p.append("Setiap soal memiliki TEPAT ").append(opsi).append(" opsi jawaban (huruf A, B, ...), ");
			p.append("dan TEPAT SATU opsi yang betul.\n");
			p.append("Keluarkan HANYA JSON array valid (tanpa teks/penjelasan lain, tanpa markdown), format persis:\n");
			p.append("[{\"soal\":\"teks pertanyaan\",\"opsi\":[{\"huruf\":\"A\",\"teks\":\"opsi\",\"betul\":true},"
					+ "{\"huruf\":\"B\",\"teks\":\"opsi\",\"betul\":false}],\"skor\":1}]\n");
		} else {
			p.append("Keluarkan HANYA JSON array valid (tanpa teks/penjelasan lain, tanpa markdown), format persis:\n");
			p.append("[{\"soal\":\"teks pertanyaan\",\"kunci\":\"kunci/jawaban model untuk koreksi\",\"skor\":1}]\n");
		}
		return p.toString();
	}

	private static String safeSoal(String s) {
		return s == null ? "" : s.trim();
	}

	private static String potongSoal(String s, int m) {
		if (s == null) {
			return "";
		}
		s = s.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
		return s.length() > m ? s.substring(0, m) + "…" : s;
	}

	/** Konteks OBE/MK (nama/SKS/rumpun/prodi/deskripsi/CPMK+Sub/bahan kajian/topik pertemuan) untuk buat soal. */
	private String bangunKonteksMkSoal(Pertemuan pertemuan) {
		StringBuilder c = new StringBuilder();
		try {
			ais.database.model.Matakuliah mk = ujian != null ? ujian.getMatakuliah() : null;
			if (mk == null && pertemuan != null && pertemuan.getPerkuliahan() != null) {
				mk = pertemuan.getPerkuliahan().getMatakuliah();
			}
			if (mk == null) {
				return "";
			}
			c.append("=== KONTEKS MATA KULIAH (pakai agar soal relevan; jangan diulang di jawaban) ===\n");
			c.append("Matakuliah: ").append(safeSoal(mk.getNama()));
			if (mk.getKode() != null) {
				c.append(" (").append(mk.getKode()).append(")");
			}
			c.append("\n");
			try {
				if (mk.getKelompokMatakuliah() != null && mk.getKelompokMatakuliah().getNama() != null) {
					c.append("Rumpun/Kelompok: ").append(mk.getKelompokMatakuliah().getNama()).append("\n");
				}
			} catch (Exception e) {
			}
			try {
				c.append("SKS: ").append(mk.getSks()).append("\n");
			} catch (Exception e) {
			}
			try {
				if (mk.getJurusan() != null) {
					c.append("Program Studi: ").append(safeSoal(mk.getJurusan().getNama())).append("\n");
				}
			} catch (Exception e) {
			}
			String desk = safeSoal(mk.getDeskripsiPembelajaran());
			if (desk.length() == 0) {
				desk = safeSoal(mk.getKeterangan());
			}
			if (desk.length() > 0) {
				c.append("Deskripsi MK: ").append(potongSoal(desk, 500)).append("\n");
			}
			String cap = safeSoal(mk.getCapaianPembelajaranProdi());
			if (cap.length() > 0) {
				c.append("Capaian/Kompetensi: ").append(potongSoal(cap, 400)).append("\n");
			}
			// CPMK (+ Sub-CPMK)
			String cpmkCsv = mk.getCapaianPembelajaranLulusan();
			if (cpmkCsv != null) {
				StringBuilder cpmkSb = new StringBuilder();
				for (String s : cpmkCsv.split(",")) {
					s = s.trim();
					if (s.length() == 0) {
						continue;
					}
					try {
						ais.database.model.obe.CapaianPembelajaranLulusan cp = (ais.database.model.obe.CapaianPembelajaranLulusan) ConstantValues
								.ambil(ais.database.model.obe.CapaianPembelajaranLulusan.class.getName(),
										Long.parseLong(s));
						if (cp == null) {
							continue;
						}
						cpmkSb.append("  - ").append(safeSoal(cp.getKode())).append(": ")
								.append(safeSoal(cp.getNama())).append("\n");
						try {
							org.json.JSONArray fa = new org.json.JSONArray(cp.getFormula());
							for (int i = 0; i < fa.length(); i++) {
								org.json.JSONObject d = fa.getJSONObject(i);
								if (d.isNull("key")) {
									continue;
								}
								cpmkSb.append("      · ").append(d.isNull("kode") ? "" : d.get("kode") + "").append(" ")
										.append(d.isNull("nama") ? "" : d.get("nama") + "").append("\n");
							}
						} catch (Exception e) {
						}
					} catch (Exception e) {
					}
				}
				if (cpmkSb.length() > 0) {
					c.append("CPMK (+ Sub-CPMK):\n").append(cpmkSb);
				}
			}
			// Bahan Kajian
			String bkCsv = mk.getBahanKajian();
			if (bkCsv != null) {
				StringBuilder bkSb = new StringBuilder();
				for (String s : bkCsv.split(",")) {
					s = s.trim();
					if (s.length() == 0) {
						continue;
					}
					try {
						ais.database.model.obe.BahanKajian b = (ais.database.model.obe.BahanKajian) ConstantValues
								.ambil(ais.database.model.obe.BahanKajian.class.getName(), Long.parseLong(s));
						if (b != null && b.getNama() != null) {
							bkSb.append(b.getNama()).append("; ");
						}
					} catch (Exception e) {
					}
				}
				if (bkSb.length() > 0) {
					c.append("Bahan Kajian: ").append(bkSb).append("\n");
				}
			}
			if (pertemuan != null) {
				try {
					if (pertemuan.getTopik() != null && pertemuan.getTopik().trim().length() > 0) {
						c.append("Topik Pertemuan: ").append(potongSoal(pertemuan.getTopik(), 300)).append("\n");
					}
				} catch (Exception e) {
				}
			}
			c.append("=== AKHIR KONTEKS ===\n");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DetailUjianHelper.bangunKonteksMkSoal");
		}
		return c.toString();
	}

	/** Daftar soal yang SUDAH ada di ujian ini (+opsi jawaban PG) agar AI tak membuat soal duplikat. */
	private String daftarSoalExisting() {
		StringBuilder sb = new StringBuilder();
		try {
			if (ujian == null || ujian.getId() == null) {
				return "";
			}
			Session s = HibernateUtil.currentSession();
			List<UjianPunyaSoal> ups = ConstantValues.simpleList(
					s.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("ujian", ujian)), UjianPunyaSoal.class);
			int no = 0;
			for (UjianPunyaSoal u : ups) {
				if (no >= 40) {
					sb.append("... (dan soal lainnya)\n");
					break;
				}
				BankSoal bs = u.getBankSoal();
				if (bs == null) {
					continue;
				}
				no++;
				sb.append(no).append(". ").append(potongSoal(bs.getSoal(), 300)).append("\n");
				if (BankSoal.PILIHAN_GANDA.equals(bs.getJenis())) {
					try {
						List<Long> det = bs.ambilBankSoalDetail(false);
						for (Long did : det) {
							BankSoalDetail d = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
									did.toString());
							if (d == null) {
								continue;
							}
							String opt = safeSoal(d.getJawaban());
							if (opt.length() == 0) {
								continue;
							}
							sb.append("   ").append(safeSoal(d.getHuruf())).append(". ").append(potongSoal(opt, 150))
									.append(Boolean.TRUE.equals(d.getBetul()) ? " (benar)" : "").append("\n");
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DetailUjianHelper.daftarSoalExisting");
		}
		return sb.toString();
	}

	/** Parse JSON hasil AI lalu insert tiap soal (BankSoal + BankSoalDetail) + tautan ke ujian. Kembalikan jumlah dibuat. */
	private int insertSoalAiDariJson(String resp, boolean isPg, Pertemuan pertemuan) {
		int dibuat = 0;
		try {
			java.util.List<JSONObject> daftar = ekstrakObjekSoal(resp);
			if (daftar.isEmpty()) {
				return 0;
			}
			Session session = HibernateUtil.currentNativeSession();
			for (int i = 0; i < daftar.size(); i++) {
				JSONObject o = daftar.get(i);
				if (o == null) {
					continue;
				}
				String soalTeks = o.optString("soal", "").trim();
				if (soalTeks.length() == 0) {
					continue;
				}
				double skor = o.optDouble("skor", 1.0);

				BankSoal bs = new BankSoal();
				bs.setSkor(skor);
				bs.setSkorSalah(0.0);
				bs.setSkorDefault(0.0);
				bs.setKeterangan("");
				bs.setSoal(soalTeks);
				if (isPg) {
					bs.setJenis(BankSoal.PILIHAN_GANDA);
					bs.setJenisKoreksi(PenjelasanBankSoal.KOREKSI_OTOMATIS);
				} else {
					bs.setJenis(BankSoal.ESAY);
					bs.setJenisKoreksi(PenjelasanBankSoal.KOREKSI_MANUAL);
				}
				bs.setFakultas(ujian != null ? ujian.getFakultas() : null);
				bs.setJurusan(ujian != null ? ujian.getJurusan() : null);
				bs.setDosen(ujian != null ? ujian.getDosen() : null);
				bs.setGuru(ujian != null ? ujian.getGuru() : null);
				bs.setMatakuliah(ujian != null ? ujian.getMatakuliah() : null);
				if (bs.getMatakuliah() == null && pertemuan != null && pertemuan.getPerkuliahan() != null) {
					bs.setMatakuliah(pertemuan.getPerkuliahan().getMatakuliah());
				}
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, bs);
				session.getTransaction().commit();

				if (isPg) {
					JSONArray opsi = o.optJSONArray("opsi");
					int jmlBetul = 0;
					int jmlOpsi = 0;
					if (opsi != null) {
						for (int j = 0; j < opsi.length(); j++) {
							JSONObject op = opsi.optJSONObject(j);
							if (op == null) {
								continue;
							}
							String teks = op.optString("teks", "").trim();
							if (teks.length() == 0) {
								continue;
							}
							String huruf = op.optString("huruf", String.valueOf((char) ('A' + j))).trim();
							boolean betul = op.optBoolean("betul", false);
							if (betul) {
								jmlBetul++;
							}
							jmlOpsi++;
							BankSoalDetail d = new BankSoalDetail();
							d.setBankSoal(bs);
							d.setBetul(betul);
							d.setEssay("");
							d.setHuruf(huruf);
							d.setJawaban(teks);
							d.setKeterangan("");
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, d);
							session.getTransaction().commit();
						}
					}
					bs.setJenisPilihanGanda(jmlBetul > 1 ? BankSoal.COMBINATION_CHOICE
							: (jmlOpsi == 2 ? BankSoal.BENAR_SALAH : BankSoal.MULTIPLE_COICE));
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, bs);
					session.getTransaction().commit();
				} else {
					String kunci = o.optString("kunci", "").trim();
					BankSoalDetail d = new BankSoalDetail();
					d.setBankSoal(bs);
					d.setBetul(true);
					d.setEssay(kunci);
					d.setHuruf("");
					d.setJawaban("");
					d.setKeterangan("");
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, d);
					session.getTransaction().commit();
				}

				UjianPunyaSoal ups = new UjianPunyaSoal();
				ups.setBankSoal(bs);
				ups.setUjian(ujian);
				session.getTransaction().begin();
				session.save(ups);
				session.getTransaction().commit();

				dibuat++;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DetailUjianHelper.insertSoalAiDariJson");
		}
		return dibuat;
	}

	private static String potongJsonArray(String s) {
		if (s == null) {
			return null;
		}
		int a = s.indexOf('[');
		int b = s.lastIndexOf(']');
		if (a >= 0 && b > a) {
			return s.substring(a, b + 1);
		}
		return null;
	}

	/**
	 * Ekstraksi objek soal SANGAT tahan-banting dari respons AI. Menangani: JSON array
	 * rapi, respons terbungkus prosa/markdown fence (```json), objek tunggal tanpa array,
	 * dan respons TERPOTONG (num_predict habis) — objek {...} lengkap terakhir tetap
	 * diselamatkan. Inilah kunci agar "0 soal padahal ada respons" tak terjadi lagi.
	 */
	private static java.util.List<JSONObject> ekstrakObjekSoal(String resp) {
		java.util.List<JSONObject> hasil = new java.util.ArrayList<JSONObject>();
		if (resp == null || resp.trim().length() == 0) {
			return hasil;
		}
		String s = resp.trim();
		// Buang pembungkus markdown fence bila ada
		int fence = s.indexOf("```");
		if (fence >= 0) {
			int nl = s.indexOf('\n', fence);
			int fenceAkhir = s.lastIndexOf("```");
			if (nl >= 0 && fenceAkhir > nl) {
				s = s.substring(nl + 1, fenceAkhir).trim();
			} else if (nl >= 0) {
				s = s.substring(nl + 1).trim();
			}
		}
		// Jalur cepat: coba parse sebagai array utuh
		String arrStr = potongJsonArray(s);
		if (arrStr != null) {
			try {
				JSONArray arr = new JSONArray(arrStr);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject o = arr.optJSONObject(i);
					if (o != null && o.has("soal")) {
						hasil.add(o);
					}
				}
				if (!hasil.isEmpty()) {
					return hasil;
				}
			} catch (Exception abaikan) {
				// lanjut ke pemindai brace-depth
			}
		}
		// Jalur cadangan: pindai brace-depth sadar-string, selamatkan tiap {...} lengkap
		int depth = 0;
		int mulai = -1;
		boolean dalamString = false;
		boolean escape = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (dalamString) {
				if (escape) {
					escape = false;
				} else if (c == '\\') {
					escape = true;
				} else if (c == '"') {
					dalamString = false;
				}
				continue;
			}
			if (c == '"') {
				dalamString = true;
			} else if (c == '{') {
				if (depth == 0) {
					mulai = i;
				}
				depth++;
			} else if (c == '}') {
				if (depth > 0) {
					depth--;
					if (depth == 0 && mulai >= 0) {
						String frag = s.substring(mulai, i + 1);
						try {
							JSONObject o = new JSONObject(frag);
							if (o.has("soal")) {
								hasil.add(o);
							}
						} catch (Exception abaikan) {
						}
						mulai = -1;
					}
				}
			}
		}
		return hasil;
	}

	public void display(final Ujian ujian, Component detail, final Pertemuan pertemuan,
			final PertemuanPunyaUjian pertemuanPunyaUjian, boolean tampilmenu, boolean delete) {
		this.ujian = ujian;
		tbmuser = Common.getCurrentUser();
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
		Component parent = checkMerupakanPerkuliahan(detail, pertemuanPunyaUjian);

		final Groupbox groupbox = new Groupbox();
		groupbox.setParent(parent);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Soal", "/img/new.gif");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Long> bankSoals = ujian.ambilBankSoal(pertemuanPunyaUjian, true);

				AmbilDataBankSoalBanyak window = new AmbilDataBankSoalBanyak(bankSoals, ujian.getJenisKoreksi(),
						pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getMatakuliah(),
						pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getMatapelajaran());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("95%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BankSoal> bankSoals = (List<BankSoal>) arg0.getData();
						if (bankSoals != null) {

							Session session = HibernateUtil.currentSession();

							for (BankSoal bankSoal : bankSoals) {

								try {
									if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
										Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
										boolean ada = false;
										Dosen dosen = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getDosen()
												: (pertemuan == null || pertemuan.getPerkuliahan() == null ? null
														: pertemuan.getPerkuliahan().getDosen1());
										Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null
												? null
												: pertemuan.getPerkuliahan().getMatakuliah();
										Matapelajaran matapelajaran = pertemuan == null
												|| pertemuan.getJadwalPelajaran() == null ? null
														: pertemuan.getJadwalPelajaran().getMatapelajaran();
										Guru guru = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getGuru()
												: (pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
														: pertemuan.getJadwalPelajaran().getGuru());

										if (bankSoal.getDosen() == null && dosen != null) {
											bankSoal.setDosen(dosen);
											ada = true;
										}
										if (bankSoal.getMatakuliah() == null && matakuliah != null) {
											bankSoal.setMatakuliah(matakuliah);
											ada = true;
										}
										if (bankSoal.getMatapelajaran() == null && matapelajaran != null) {
											bankSoal.setMatapelajaran(matapelajaran);
											ada = true;
										}
										if (bankSoal.getGuru() == null || guru != null) {
											bankSoal.setGuru(guru);
											ada = true;
										}

										if (ada) {
											Common.refreshUpdate(bankSoal);
										}

									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:823");
								}

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) session
										.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("bankSoal", bankSoal))
										.add(Restrictions.eq("ujian", ujian)).setMaxResults(1).uniqueResult();
								if (ujianPunyaSoal == null) {
									ujianPunyaSoal = new UjianPunyaSoal();
									ujianPunyaSoal.setBankSoal(bankSoal);
									ujianPunyaSoal.setUjian(ujian);
									session.save(ujianPunyaSoal);
								}
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							});

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Soal Baru", "/img/svg/addthis.svg");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				BankSoal bankSoal = new BankSoal();
				bankSoal.setJenis(ujian.getJenis());
				bankSoal.setFakultas(ujian.getFakultas());
				bankSoal.setJurusan(ujian.getJurusan());
				bankSoal.setDosen(ujian.getDosen());
				bankSoal.setGuru(ujian.getGuru());
				bankSoal.setMatakuliah(ujian.getMatakuliah());

				bankSoal.setDosen(tbmuser == null || tbmuser.getDosen() == null
						? (pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getDosen1())
						: tbmuser.getDosen());
				bankSoal.setMatakuliah(pertemuan == null || pertemuan.getPerkuliahan() == null ? null
						: pertemuan.getPerkuliahan().getMatakuliah());
				bankSoal.setMatapelajaran(pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
						: pertemuan.getJadwalPelajaran().getMatapelajaran());

				bankSoal.setGuru(tbmuser == null || tbmuser.getGuru() == null
						? (pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getGuru())
						: tbmuser.getGuru());

				BankSoalAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						BankSoal bankSoal = (BankSoal) arg0.getData();
						Session session = HibernateUtil.currentSession();

						UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) session.createCriteria(UjianPunyaSoal.class)
								.add(Restrictions.eq("bankSoal", bankSoal)).add(Restrictions.eq("ujian", ujian))
								.setMaxResults(1).uniqueResult();
						if (ujianPunyaSoal == null) {
							ujianPunyaSoal = new UjianPunyaSoal();
							ujianPunyaSoal.setBankSoal(bankSoal);
							ujianPunyaSoal.setUjian(ujian);
							session.save(ujianPunyaSoal);
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(true);
							}
						});

					}
				}, bankSoal, ujian == null ? null : ujian.getJenisKoreksi());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Buat Soal Via AI", "/img/svg/sparkles.svg");
		button.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaBuatSoalAi(pertemuan);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DetailUjianHelper.doDownload(ujian, pertemuanPunyaUjian, null, false);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				uploadSoal(media, ujian);
			}
		});

		button.setParent(toolbar);

		if (pertemuanPunyaUjian != null) {
			button = new MyToolbarbuttonConfig("Hasil Ujian", "/img/album.png");
			button.setAttribute("janganDisabled", true);
			button.setVisible(tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null) {
						HasilUjianSiswaHelper hasilUjianMahasiswaHelper = new HasilUjianSiswaHelper(pertemuan);
						Window window = new Window("Hasil Ujian " + ujian.getNama() + " - " + pertemuan.toString(),
								"none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("90%");
						hasilUjianMahasiswaHelper.display(pertemuanPunyaUjian, window);
						window.onModal();
					} else {

						HasilUjianMahasiswaHelper hasilUjianMahasiswaHelper = new HasilUjianMahasiswaHelper(pertemuan);
						Window window = new Window("Hasil Ujian " + ujian.getNama() + " - " + pertemuan.toString(),
								"none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("90%");
						hasilUjianMahasiswaHelper.display(pertemuanPunyaUjian, window);
						window.onModal();
					}
				}
			});
			button.setParent(toolbar);
		}

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus Soal Double", "/img/svg/trash.svg");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus soal yang double ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<UjianPunyaSoal> ujianPunyaSoals = ujian == null || ujian.getId() == null
												? new ArrayList<UjianPunyaSoal>()
												: session.createCriteria(UjianPunyaSoal.class)
														.add(Restrictions.eq("ujian", ujian)).list();

										Set<Long> idSoal = new HashSet<Long>();
										for (UjianPunyaSoal ujianPunyaSoal : ujianPunyaSoals) {
											if (idSoal.contains(ujianPunyaSoal.getBankSoal().getId())) {
												session.delete(ujianPunyaSoal);
											} else {
												idSoal.add(ujianPunyaSoal.getBankSoal().getId());
											}
										}

										Set<String> Soal = new HashSet<String>();
										for (UjianPunyaSoal ujianPunyaSoal : ujianPunyaSoals) {
											if (Soal.contains(ujianPunyaSoal.getBankSoal().getSoal())) {
												session.delete(ujianPunyaSoal);
											} else {
												Soal.add(ujianPunyaSoal.getBankSoal().getSoal());
											}
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(true);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<UjianPunyaSoal> ujianPunyaSoals = ujian == null || ujian.getId() == null
												? new ArrayList<UjianPunyaSoal>()
												: session.createCriteria(UjianPunyaSoal.class)
														.add(Restrictions.eq("ujian", ujian)).list();
										for (UjianPunyaSoal ujianPunyaSoal : ujianPunyaSoals) {
											session.delete(ujianPunyaSoal);
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(true);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);

			}
		});

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Download Tanpa Tag HTML", "/img/excel.png");
		button.setVisible(tampilmenu && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				DetailUjianHelper.doDownload(ujian, pertemuanPunyaUjian, null, true);
			}
		});
		button.setParent(toolbar);

		cari = new Textbox();
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		toolbar.appendChild(cari);
		button = new MyToolbarbuttonConfig("", "/img/search.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		button.setParent(toolbar);

		paging = new Paging();
		Common.initPaging1(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);

			}
		});
		paging.setParent(groupbox);

		grid = new MyGrid();

		grid.setSclass("fgrid");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(5000);
		grid.setStyle("min-height:1400px");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);

		loadData(null);

	}

	public static void tampilSoalDanJawaban(Row rowParent, final BankSoal bankSoal, final UjianPunyaSoal ujianPunyaSoal,
			Tbmuser tbmuser, boolean refreshSoal, boolean tampilanHurufDiPilihanJawaban, boolean random,
			final EventListener eventListenerUbah, boolean edit, boolean hapus) throws Exception {
		rowParent.setStyle("background-color: rgba(255,255,255,0.4);border:0px;");
		rowParent.getGrid().setOddRowSclass("non-odd");
		rowParent.getGrid().setSclass("dgrid");

		MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
		groupboxStyled.appendChild(new Caption("Soal"));

		if (bankSoal != null && bankSoal.getPenjelasanBankSoal() != null) {

			Hbox hbox = new Hbox();
//			hbox.setStyle("min-height:5000px");
			hbox.setParent(rowParent);

			MyGroupboxStyled groupboxStyled1 = new MyGroupboxStyled();
			groupboxStyled1.appendChild(new Caption(bankSoal.getPenjelasanBankSoal().getNama()));
			groupboxStyled1.setParent(hbox);
			groupboxStyled1.appendChild(new Html(bankSoal.getPenjelasanBankSoal().getKeterangan()));

			groupboxStyled.setParent(hbox);
		} else {
			groupboxStyled.setParent(rowParent);
		}

		MyGroupboxStyled vboxPertanyaanUjian = new MyGroupboxStyled();
		vboxPertanyaanUjian.appendChild(new Caption("Pertanyaan"));
		vboxPertanyaanUjian.setParent(groupboxStyled);

		boolean vertical = true;

		Grid gridSoal = new Grid();
		gridSoal.setOddRowSclass("non-odd");
		gridSoal.setWidth("100%");
		gridSoal.setParent(groupboxStyled);
		gridSoal.setSclass("fgrid");
		gridSoal.setStyle("background:transparent;");

		Rows myrows = new Rows();
		myrows.setParent(gridSoal);

		Row myrowalampiran = new Row();
		myrowalampiran.setStyle("background:transparent;border:0px;");
		myrowalampiran.setParent(myrows);

		Row rowlampiran1Soal = new Row();
		rowlampiran1Soal.setStyle("background:transparent;border:0px;");
		rowlampiran1Soal.setParent(myrows);

		Row rowlampiran2Soal = new Row();
		rowlampiran2Soal.setStyle("background:transparent;border:0px;");
		rowlampiran2Soal.setParent(myrows);

		Row rowlampiran3Soal = new Row();
		rowlampiran3Soal.setStyle("background:transparent;border:0px;");
		rowlampiran3Soal.setParent(myrows);

		Row rowlampiran4Soal = new Row();
		rowlampiran4Soal.setStyle("background:transparent;border:0px;");
		rowlampiran4Soal.setParent(myrows);

		Row rowlampiran5Soal = new Row();
		rowlampiran5Soal.setStyle("background:transparent;border:0px;");
		rowlampiran5Soal.setParent(myrows);

		BankSoalAction.tampilkanLampiran(null, bankSoal, myrowalampiran, false, rowlampiran1Soal, rowlampiran2Soal,
				rowlampiran3Soal, rowlampiran4Soal, rowlampiran5Soal);

		Row rowjawaban1 = vertical ? new Row() : rowParent;
		rowjawaban1.setStyle("background:transparent;border:0px;");
		if (vertical) {
			rowjawaban1.setParent(myrows);
		}

		Vbox utamaVbo = new Vbox();
		utamaVbo.setParent(rowjawaban1);
		utamaVbo.setWidth("100%");

		if (bankSoal.getJenisPilihanGanda().equals(BankSoal.JAWABAN_SINGKAT)
				|| bankSoal.getJenisPilihanGanda().equals(BankSoal.RUMPANG)) {

			new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
					+ bankSoal.getSoal() + "</div>").setParent(vboxPertanyaanUjian);

			MyGroupboxStyled vboxSoalUjian1 = new MyGroupboxStyled();
			vboxSoalUjian1.appendChild(new Caption("Isilah bagian yang kosong pada kotak teks di bawah ini:"));

			vboxSoalUjian1.setParent(utamaVbo);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			vboxSoalUjian1.appendChild(grid);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth(bankSoal.getJenisPilihanGanda().equals(BankSoal.JAWABAN_SINGKAT) ? "0px" : "40px");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(refreshSoal);
			for (Long bankSoalDetailid : bankSoalDetails) {

				final BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
						.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
				if (bankSoalDetail != null && !bankSoalDetail.getJawaban().trim().isEmpty()
						&& Common.isNumber(bankSoalDetail.getHuruf())) {

					try {
						final Textbox jawaban = new Textbox();
						jawaban.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								bankSoalDetail.setJawaban(jawaban.getValue().trim());
								Common.refreshUpdate(bankSoalDetail);
							}
						});
						jawaban.setValue(bankSoalDetail.getJawaban());
						jawaban.setRows(1);
						jawaban.setWidth("95%");

						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						Label lb;
						row.appendChild(lb = new Label(bankSoalDetail.getHuruf()));
						row.appendChild(jawaban);
						lb.setStyle(
								"padding-right: 8px;padding-left: 8px;text-align: center;font-weight: bolder;font-size:16px;width: 24px !important;height: 24px !important;border-radius: 50px;border: 1px solid black;");

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:1304");
						// TODO: handle exception
					}
				}
			}
		}

		else if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {

			new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
					+ bankSoal.getSoal() + "</div>").setParent(vboxPertanyaanUjian);

			MyGroupboxStyled vboxSoalUjian1 = new MyGroupboxStyled();
			vboxSoalUjian1.appendChild(new Caption(bankSoal.getSoalMenjodohkan() ? "Menjodohkan Jawaban"
					: bankSoal.getSoalMengurutkan() ? "Mengurutkan Jawaban"
							: bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA) ? "Pilihan Jawaban"
									: "Kunci Jawaban"));

			if (bankSoal.getSoalMenjodohkan()) {

				vboxSoalUjian1.setParent(utamaVbo);

				try {
					JSONArray array = new JSONArray(bankSoal.getOpsiSoal());

					Set<String[]> stringsdata = new HashSet<String[]>();

					for (int k = 0; k < array.length(); k++) {
						try {
							JSONObject jsonObject = array.getJSONObject(k);

							if (jsonObject.isNull("key")) {
								continue;
							}

							String nama = "";

							if (!jsonObject.isNull("nama")) {
								nama = jsonObject.get("nama") + "";
							}

							String nomorData = "";

							if (!jsonObject.isNull("nomor")) {
								nomorData = jsonObject.get("nomor") + "";
							}

							stringsdata.add(new String[] { nama, nomorData });

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:1354");
						}
					}

					MyGroupboxStyled vboxSoalPasangan = new MyGroupboxStyled();
					vboxSoalPasangan.appendChild(new Caption("Opsi Pasangan"));
					vboxSoalPasangan.setParent(vboxSoalUjian1);

					final Listbox listboxPasangan = new Listbox();

					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							DropEvent dropEvent = (DropEvent) arg0;

							Component dragged = dropEvent.getDragged();
							Component self = dropEvent.getTarget();
							if (dragged instanceof Listitem && self instanceof Listitem) {
								dragged.getParent().insertBefore(dragged, self);

							}

						}
					};

					listboxPasangan.setParent(vboxSoalPasangan);
					listboxPasangan.setDroppable("true");
					listboxPasangan.addEventListener(Events.ON_DROP, eventListener);

					Integer indexData = 1;
					for (String[] dataStrings : stringsdata) {
						Listitem listitem = new Listitem();
						listitem.setAttribute("dataStrings", dataStrings);
						listitem.setDraggable("true");
						listitem.setDroppable("true");
						listitem.addEventListener(Events.ON_DROP, eventListener);

						Label label = new Label(dataStrings[0]);
						label.setStyle(
								"border: solid 2px;\r\n" + "    padding-top: 1px;\r\n" + "    border-radius: 15px;\r\n"
										+ "    text-align: justify;\r\n" + "    padding-left: 10px;\r\n"
										+ "    padding-bottom: 1px;\r\n" + "    padding-right: 15px;");

						Listcell listcell = new Listcell();
						listcell.setStyle(
								"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
						listcell.setParent(listitem);
						listcell.appendChild(label);

						listboxPasangan.appendChild(listitem);

						indexData++;
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:1411");
				}

			}

			else if (bankSoal.getSoalMengurutkan()) {

				vboxSoalUjian1.setParent(utamaVbo);

				List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(refreshSoal);

				TreeMap<Integer, BankSoalDetail> treeMap = new TreeMap<Integer, BankSoalDetail>();
				for (Long bankSoalDetailid : bankSoalDetails) {

					BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							bankSoalDetailid.toString());
					if (bankSoalDetail != null) {

						treeMap.put(bankSoalDetail.getUrutanDiujikan(), bankSoalDetail);

					}
				}

				Hlayout vboxJawaban = new Hlayout();
				vboxJawaban.setParent(vboxSoalUjian1);
				final Listbox listbox = new Listbox();
				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						DropEvent dropEvent = (DropEvent) arg0;

						Component dragged = dropEvent.getDragged();
						Component self = dropEvent.getTarget();
						if (dragged instanceof Listitem && self instanceof Listitem) {
							int indexOff1 = listbox.getIndexOfItem((Listitem) dragged);
							int indexOff2 = listbox.getIndexOfItem((Listitem) self);

							if (indexOff1 > indexOff2) {
								dragged.getParent().insertBefore(dragged, self);
							} else {
								dragged.getParent().insertBefore(self, dragged);
							}

							BankSoalDetail bankSoalDetailDrag = (BankSoalDetail) dragged.getAttribute("bankSoalDetail");
							BankSoalDetail bankSoalDetailSelf = (BankSoalDetail) self.getAttribute("bankSoalDetail");

							int indexDrag = bankSoalDetailDrag.getUrutanDiujikan();
							int indexSelf = bankSoalDetailSelf.getUrutanDiujikan();

							Session session = HibernateUtil.currentSession();
							session.refresh(bankSoalDetailDrag);
							session.refresh(bankSoalDetailSelf);

							bankSoalDetailDrag.setUrutanDiujikan(indexSelf);
							bankSoalDetailSelf.setUrutanDiujikan(indexDrag);

							Common.refreshUpdate(session, bankSoalDetailDrag);
							Common.refreshUpdate(session, bankSoalDetailSelf);
							session.flush();
						}

					}
				};

				listbox.setParent(vboxJawaban);
				listbox.setDroppable("true");
				listbox.addEventListener(Events.ON_DROP, eventListener);

				Integer indexData = 1;
				for (BankSoalDetail bankSoalDetail : treeMap.values()) {
					Listitem listitem = new Listitem();
					listitem.setAttribute("bankSoalDetail", bankSoalDetail);
					listitem.setDraggable("true");
					listitem.setDroppable("true");
					listitem.addEventListener(Events.ON_DROP, eventListener);

					Label label = new Label(bankSoalDetail.getJawaban());
					label.setStyle(
							"border: solid 2px;\r\n" + "    padding-top: 1px;\r\n" + "    border-radius: 15px;\r\n"
									+ "    text-align: justify;\r\n" + "    padding-left: 10px;\r\n"
									+ "    padding-bottom: 1px;\r\n" + "    padding-right: 15px;");

					Listcell listcell = new Listcell();
					listcell.setStyle(
							"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
					listcell.setParent(listitem);
					listcell.appendChild(label);

					listbox.appendChild(listitem);

					indexData++;
				}
			} else {

				if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null
						&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
								|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))) {
					Radiogroup radiogroup = new Radiogroup();
					radiogroup.setParent(utamaVbo);
					vboxSoalUjian1.setParent(radiogroup);
					radiogroup.setHeight("100%");
				} else {
					vboxSoalUjian1.setParent(utamaVbo);
				}

				Vbox vbox = new Vbox();
				vbox.setParent(vboxSoalUjian1);

				final List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(refreshSoal);
				for (Long bankSoalDetailid : bankSoalDetails) {

					final BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
							.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
					if (bankSoalDetail != null) {

						EventListener eventListener = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Checkbox checkbox = (Checkbox) arg0.getTarget();

								Session session = HibernateUtil.currentSession();
								session.refresh(bankSoalDetail);
								bankSoalDetail.setBetul(checkbox.isChecked());
								Common.refreshUpdate(session, bankSoalDetail);

								if (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
										|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)) {
									for (Long bid : bankSoalDetails) {
										BankSoalDetail b = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bid.toString());
										if (b != null) {
											if (!b.getId().equals(bankSoalDetail.getId())) {
												session.refresh(b);
												b.setBetul(false);
												Common.refreshUpdate(session, b);
											}
										}
									}
								}
							}
						};

						String soal = (tampilanHurufDiPilihanJawaban ? bankSoalDetail.getHuruf() + ". " : "")
								+ bankSoalDetail.getJawaban();

						if (ujianPunyaSoal != null && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null
								&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
										|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))) {
							Radio radio = new Radio(soal);
							radio.setParent(vbox);
							radio.setChecked(bankSoalDetail.getBetul());
							radio.addEventListener("onClick", eventListener);
						} else if (ujianPunyaSoal != null && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null) {
							Checkbox radio = new Checkbox(soal);
							radio.setParent(vbox);
							radio.setChecked(bankSoalDetail.getBetul());
							radio.addEventListener("onClick", eventListener);
						} else {
							new Label(soal).setParent(vbox);
						}

						LampiranLain lampiranLain = LampiranLain.ambil(bankSoalDetail.getBankSoal().getId(),
								"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
						if (lampiranLain != null) {
							Image img;
							vbox.appendChild(img = new Image(lampiranLain.createLinkUri()));
							img.setStyle("max-height: 300px");
						}
					}
				}
			}
		} else if (bankSoal.getJenis().equals(BankSoal.ESAY)) {

			new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
					+ bankSoal.getSoal() + "</div>").setParent(vboxPertanyaanUjian);

			MyGroupboxStyled vboxSoalUjian = new MyGroupboxStyled();
			vboxSoalUjian.appendChild(new Caption(
					bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA) ? "Pilihan Jawaban" : "Kunci Jawaban"));
			vboxSoalUjian.setParent(utamaVbo);

			Session session = HibernateUtil.currentSession();
			BankSoalDetail bankSoalDetail = (BankSoalDetail) ConstantValues.simpleObject(
					session.createCriteria(BankSoalDetail.class).add(Restrictions.isNotNull("essay"))
							.add(Restrictions.eq("bankSoal", bankSoal)).setMaxResults(1).addOrder(Order.asc("huruf")),
					BankSoalDetail.class);

			String c = bankSoalDetail == null ? "" : bankSoalDetail.getEssay();
			new ais.ui.util.MyHtml(c).setParent(vboxSoalUjian);

			vboxSoalUjian.setVisible(!c.isEmpty());
		} else if (bankSoal.getJenis().equals(BankSoal.JAWABAN_SINGKAT)) {
			Session session = HibernateUtil.currentSession();
			BankSoalDetail bankSoalDetail = (BankSoalDetail) ConstantValues.simpleObject(
					session.createCriteria(BankSoalDetail.class).add(Restrictions.isNotNull("essay"))
							.add(Restrictions.eq("bankSoal", bankSoal)).setMaxResults(1).addOrder(Order.asc("huruf")),
					BankSoalDetail.class);

			String c = bankSoalDetail == null ? "" : bankSoalDetail.getEssay();

			new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
					+ bankSoal.getSoal() + "  <b>Jawab:</b>" + c + "</div>").setParent(vboxPertanyaanUjian);
		}

		MyGroupboxStyled vboxSoalSkor = new MyGroupboxStyled();
		vboxSoalSkor.appendChild(new Caption("Skor"));
		vboxSoalSkor.setParent(utamaVbo);

		Vbox skorV = new Vbox();
		skorV.setParent(vboxSoalSkor);

		RevisiHelper
				.createNewRevisi(BankSoal.class, bankSoal, "Benar : " + Common.numberFormat.get().format(bankSoal.getSkor()))
				.setParent(skorV);

		new Label("Salah : " + Common.numberFormat.get().format(bankSoal.getSkorSalah())).setParent(skorV);

		new Label("Default : " + Common.numberFormat.get().format(bankSoal.getSkorDefault())).setParent(skorV);

		if (ujianPunyaSoal != null) {

			if (random) {
				new Label("Nomor Soal : Random / Acak").setParent(skorV);
			} else {
				Hbox hbox = new Hbox();
				hbox.setParent(skorV);
				new Label(ais.common.Common.getBahasaConfig("Nomor Soal : ")).setParent(hbox);
				final Intbox intbox = new Intbox(ujianPunyaSoal.getNomorUrut());
				intbox.setCols(5);
				intbox.setParent(hbox);
				intbox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ujianPunyaSoal.setNomorUrut(intbox.getValue());
						Common.refreshUpdate(ujianPunyaSoal);
					}
				});
			}

		}

		if (edit || hapus) {

			Hbox toolbar = new Hbox();
			toolbar.setParent(skorV);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Edit Data");
			button.setVisible(tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						BankSoalAction.onAddExternal(arg0, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(eventListenerUbah);
							}
						}, bankSoal, bankSoal.getJenisKoreksi());
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e,
								"auto-audit src/ais/action/master/helper/DetailUjianHelper.java:ubah-bank-soal");
						PesanFormalHelper.tampilkanGagal("pembukaan form ubah soal",
								"Form ubah soal belum dapat dibuka karena data soal ini perlu disegarkan terlebih dahulu.",
								new String[] { "Klik Refresh pada jendela Kelola Soal Ujian.",
										"Setelah daftar soal tampil ulang, klik Ubah pada soal yang sama.",
										"Jika masih terjadi, laporkan pesan ini beserta nama ujian dan nomor soal." });
					}

				}
			});

			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Copy", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Data");
			button.setVisible(tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final BankSoal originalBankSoal = bankSoal;
					Session session = HibernateUtil.currentSession();
					List<Long> bankSoalDetails = originalBankSoal.ambilBankSoalDetail(false);

					BankSoal newBankSoal = (BankSoal) bankSoal.clone();
					newBankSoal.setId(null);
					session.save(newBankSoal);
					for (Long bankSoalDetailid : bankSoalDetails) {
						BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
								.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
						if (bankSoalDetail != null) {
							BankSoalDetail c = (BankSoalDetail) bankSoalDetail.clone();
							c.setId(null);
							c.setKodeUnik(null);
							c.setBankSoal(newBankSoal);
							session.save(c);
						}
					}

					BankSoalAction.onAddExternal(arg0, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (ujianPunyaSoal != null) {
								BankSoal bankSoal = (BankSoal) arg0.getData();
								Session session = HibernateUtil.currentSession();

								Ujian ujian = ujianPunyaSoal.getUjian();

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) session
										.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("bankSoal", bankSoal))
										.add(Restrictions.eq("ujian", ujian)).setMaxResults(1).uniqueResult();
								if (ujianPunyaSoal == null) {
									ujianPunyaSoal = new UjianPunyaSoal();
									ujianPunyaSoal.setBankSoal(bankSoal);
									ujianPunyaSoal.setUjian(ujian);
									session.save(ujianPunyaSoal);
								}
							}

							Common.createDefaultTimer(eventListenerUbah);

						}
					}, newBankSoal, originalBankSoal, ujianPunyaSoal == null || ujianPunyaSoal.getUjian() == null ? null
							: ujianPunyaSoal.getUjian().getJenisKoreksi());

				}
			});

			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && hapus);
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
											if (ujianPunyaSoal != null) {
												// Cegah hapus soal yang SUDAH dijawab mahasiswa: penghapusan akan
												// melanggar FK hasil_ujian_mahasiswa_detail -> flush gagal & sesi
												// Hibernate jadi rusak (memicu NPE/error pada operasi berikutnya).
												if (soalSudahDipakaiHasilUjian(ujianPunyaSoal)) {
													MyMessageboxConfig.show(
															"Soal ini sudah dijawab/dipakai pada hasil ujian mahasiswa, sehingga tidak dapat dihapus. "
																	+ "Hapus dulu hasil ujian mahasiswa yang terkait bila memang ingin menghapus soal ini.",
															"Tidak Dapat Dihapus", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);
													return;
												}
												Common.refreshDelete(ujianPunyaSoal);
											} else {
												Common.refreshDelete(bankSoal);
											}
											Common.createDefaultTimer(eventListenerUbah);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
		}
	}

	/**
	 * Apakah soal ujian ({@link UjianPunyaSoal}) sudah dipakai pada hasil ujian
	 * mahasiswa (tabel {@code hasil_ujian_mahasiswa_detail})? Dipakai untuk mencegah
	 * penghapusan soal yang akan melanggar foreign key dan merusak sesi Hibernate
	 * (penyebab error berantai: ConstraintViolationException + NPE saat render ulang).
	 * Memakai {@code openSession()} yang ditutup di {@code finally}
	 * (clear/disconnect/close). Fail-open: bila pengecekan gagal, kembalikan
	 * {@code false} agar alur lama tetap berjalan (penghapusan tetap dibungkus try-catch).
	 */
	private static boolean soalSudahDipakaiHasilUjian(UjianPunyaSoal ujianPunyaSoal) {
		if (ujianPunyaSoal == null || ujianPunyaSoal.getId() == null) {
			return false;
		}
		org.hibernate.Session session = null;
		try {
			session = ais.database.hibernate.HibernateUtil.openSession();
			Number jumlah = (Number) session
					.createCriteria(ais.database.model.HasilUjianMahasiswaDetail.class)
					.add(org.hibernate.criterion.Restrictions.eq("ujianPunyaSoal", ujianPunyaSoal))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			return jumlah != null && jumlah.intValue() > 0;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:1819");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:1823");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:1827");
				}
			}
		}
	}

	public class DetailUjianRenderer extends ais.ui.util.MyRowRenderer {

		private EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		};

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// Lindungi dari item model null (mis. setelah operasi gagal): jangan render baris kosong.
			if (arg1 == null) {
				return;
			}
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					arg1.toString());
			if (ujianPunyaSoal != null) {
				BankSoal bankSoal = ujianPunyaSoal.getBankSoal();

				try {
					if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
						Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
						boolean ada = false;
						Dosen dosen = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getDosen1();
						Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getMatakuliah();
						Matapelajaran matapelajaran = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getMatapelajaran();
						Guru guru = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getGuru();
						if (bankSoal.getDosen() == null && dosen != null) {
							bankSoal.setDosen(dosen);
							ada = true;
						}
						if (bankSoal.getMatakuliah() == null && matakuliah != null) {
							bankSoal.setMatakuliah(matakuliah);
							ada = true;
						}
						if (bankSoal.getMatapelajaran() == null && matapelajaran != null) {
							bankSoal.setMatapelajaran(matapelajaran);
							ada = true;
						}
						if (bankSoal.getGuru() == null || guru != null) {
							bankSoal.setGuru(guru);
							ada = true;
						}

						if (ada) {
							Common.refreshUpdate(bankSoal);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:1889");
				}

				DetailUjianHelper.tampilSoalDanJawaban(arg0, bankSoal, ujianPunyaSoal, tbmuser, refreshSoal,
						ujian == null ? true : ujian.getTampilanHurufDiPilihanJawaban(),
						pertemuanPunyaUjian == null ? true : pertemuanPunyaUjian.getRandom(), ubahEventListener, true,
						true);

				// Feature 6: Sub-CPMK dropdown per soal (OBE only)
				tambahanSubCpmkDropdown(arg0, ujianPunyaSoal, pertemuanPunyaUjian);

				if (countHasil > 0) {
					Common.freeze(arg0, true);
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void doDownload(Criteria criteria, PertemuanPunyaUjian pertemuanPunyaUjian) throws Exception {

		List<BankSoal> bankSoals = criteria.list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(bankSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (BankSoal bankSoal : bankSoals) {

			colIndex = 1;
			if (bankSoal == null) {
				continue;
			}

			try {
				if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
					Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
					boolean ada = false;
					Dosen dosen = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
							: pertemuan.getPerkuliahan().getDosen1();
					Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
							: pertemuan.getPerkuliahan().getMatakuliah();
					Matapelajaran matapelajaran = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
							: pertemuan.getJadwalPelajaran().getMatapelajaran();
					Guru guru = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
							: pertemuan.getJadwalPelajaran().getGuru();
					if (bankSoal.getDosen() == null && dosen != null) {
						bankSoal.setDosen(dosen);
						ada = true;
					}
					if (bankSoal.getMatakuliah() == null && matakuliah != null) {
						bankSoal.setMatakuliah(matakuliah);
						ada = true;
					}
					if (bankSoal.getMatapelajaran() == null && matapelajaran != null) {
						bankSoal.setMatapelajaran(matapelajaran);
						ada = true;
					}
					if (bankSoal.getGuru() == null || guru != null) {
						bankSoal.setGuru(guru);
						ada = true;
					}

					if (ada) {
						Common.refreshUpdate(bankSoal);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2005");
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());

			List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
					: bankSoal.ambilBankSoalDetail(true);

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
				String benar = "";
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						if (detail.getBetul() != null && detail.getBetul()) {
							benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
						}
					}
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getJawaban() == null ? "" : detail.getJawaban());
					}
				}

			} else {
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getEssay() == null ? "" : detail.getEssay());
					}
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
					bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
			rowIndex++;

		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "bank_soal__.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}
	
	public static void doDownload(Ujian ujian, PertemuanPunyaUjian pertemuanPunyaUjian, Criteria criteria) throws Exception {
		doDownload(ujian, pertemuanPunyaUjian, criteria, false); 
	}

	@SuppressWarnings("unchecked")
	public static void doDownload(Ujian ujian, PertemuanPunyaUjian pertemuanPunyaUjian, Criteria criteria,
			boolean tanpahtml) throws Exception {

		Object[] objects = ujian.ambilUjianPunyaSoal(true, pertemuanPunyaUjian, "", 0, 10000);
		List<Long> ujianPunyaSoals = (List<Long>) objects[0];

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(ujianPunyaSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("NO."));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		if (ujian != null) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, 0, 0, ujian.getId());
		}
		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL UJIAN"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (Long ujianPunyaSoalid : ujianPunyaSoals) {
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					ujianPunyaSoalid.toString());
			if (ujianPunyaSoal != null) {
				BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
				colIndex = 1;
				if (bankSoal == null) {
					continue;
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
				if (tanpahtml) {
					try {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
								Jsoup.parse(bankSoal.getSoal()).text());
					} catch (Exception e) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());
					}
				} else {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());
				}

				List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
						: bankSoal.ambilBankSoalDetail(true);

				if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
					String benar = "";
					for (Long detailid : bankSoalDetail) {
						BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
								detailid.toString());
						if (detail != null) {
							if (detail.getBetul() != null && detail.getBetul()) {
								benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
							}
						}
					}
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
					for (Long detailid : bankSoalDetail) {
						BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
								detailid.toString());
						if (detail != null) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
									detail.getJawaban() == null ? "" : detail.getJawaban());
						}
					}

				} else {
					for (Long detailid : bankSoalDetail) {
						BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
								detailid.toString());
						if (detail != null) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
									detail.getEssay() == null ? "" : detail.getEssay());
						}
					}
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
						bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, ujianPunyaSoal.getNomorUrut());

				try {
					if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
						Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
						boolean ada = false;
						Dosen dosen = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getDosen1();
						Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
								: pertemuan.getPerkuliahan().getMatakuliah();
						Matapelajaran matapelajaran = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getMatapelajaran();
						Guru guru = pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getGuru();
						if (bankSoal.getDosen() == null && dosen != null) {
							bankSoal.setDosen(dosen);
							ada = true;
						}
						if (bankSoal.getMatakuliah() == null && matakuliah != null) {
							bankSoal.setMatakuliah(matakuliah);
							ada = true;
						}
						if (bankSoal.getMatapelajaran() == null && matapelajaran != null) {
							bankSoal.setMatapelajaran(matapelajaran);
							ada = true;
						}
						if (bankSoal.getGuru() == null || guru != null) {
							bankSoal.setGuru(guru);
							ada = true;
						}

						if (ada) {
							Common.refreshUpdate(bankSoal);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2233");
				}

				rowIndex++;
			}
		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "template_ujian_" + (ujian == null ? "" : ujian.getId() + "_" + ujian.getNama()) + "_.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	public static void doUpload(Media media, final DataLoader dataLoader, PertemuanPunyaUjian pertemuanPunyaUjian)
			throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);
			Tbmuser tbmuser = Common.getCurrentUser();
			int terupload = 0;
			ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Bank Soal");
			int rowIdx = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					rowIdx++;
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {
						if (strings.size() < 6) {
							continue;
						}

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						String penjelasan = "";
						try {
							penjelasan = strings.get(16);
						} catch (Exception e) {

						}

						PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) Common
								.getContentAsObject(penjelasan, PenjelasanBankSoal.class, null);

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) {

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) {

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(BankSoal.class)
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("id", Long.parseLong(id.trim())),
										Restrictions.ilike("soal", soal, MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
						}
							try {
								newBankSoal.setSkor(skor == null || skor.trim().length() == 0 ? 0.0
										: Double.parseDouble(skor.trim()));
							} catch (Exception e) {
								newBankSoal.setSkor(0.0);
							}
							try {
								newBankSoal.setSkorSalah(skorSalah == null || skorSalah.trim().length() == 0 ? 0.0
										: Double.parseDouble(skorSalah.trim()));
							} catch (Exception e) {
								newBankSoal.setSkorSalah(0.0);
							}
							try {
								newBankSoal.setSkorDefault(benarDefault == null || benarDefault.trim().length() == 0
										? 0.0 : Double.parseDouble(benarDefault.trim()));
							} catch (Exception e) {
								newBankSoal.setSkorDefault(0.0);
							}

						newBankSoal.setJenis(jenis);
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);

						try {
							if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
								Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();

								Dosen dosen = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getDosen()
										: (pertemuan == null || pertemuan.getPerkuliahan() == null ? null
												: pertemuan.getPerkuliahan().getDosen1());
								Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
										: pertemuan.getPerkuliahan().getMatakuliah();
								Matapelajaran matapelajaran = pertemuan == null
										|| pertemuan.getJadwalPelajaran() == null ? null
												: pertemuan.getJadwalPelajaran().getMatapelajaran();
								Guru guru = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getGuru()
										: (pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
												: pertemuan.getJadwalPelajaran().getGuru());

								if (newBankSoal.getDosen() == null && dosen != null) {
									newBankSoal.setDosen(dosen);
								}
								if (newBankSoal.getMatakuliah() == null && matakuliah != null) {
									newBankSoal.setMatakuliah(matakuliah);
								}
								if (newBankSoal.getMatapelajaran() == null && matapelajaran != null) {
									newBankSoal.setMatapelajaran(matapelajaran);
								}
								if (newBankSoal.getGuru() == null || guru != null) {
									newBankSoal.setGuru(guru);
								}

							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2389");
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();
						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:2427");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						terupload++;
						report.sukses(rowIdx, id + "/" + (soal.length() > 30 ? soal.substring(0, 30) : soal), "terupload=" + terupload);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2469");
					report.gagal(rowIdx, "row " + rowIdx, e, "Periksa data soal baris " + rowIdx);
				}

			}
			HibernateUtil.closeSession();

			try { Filedownload.save(report.simpanLaporan(), "text/plain"); }
			catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan DetailUjianHelper"); }

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload" + report.getRingkasan(), "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public static void doUpload(Media media, Ujian ujian, final DataLoader dataLoader,
			PertemuanPunyaUjian pertemuanPunyaUjian) throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			Tbmuser tbmuser = Common.getCurrentUser();

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);

			int terupload = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					/* Baris kosong/pendek sah muncul pada template Excel. Dua kolom pertama
					 * dibaca sebelum validasi ukuran pada versi lama sehingga baris tersebut
					 * menghasilkan IndexOutOfBoundsException. */
					if (strings == null || strings.size() < 2) {
						continue;
					}
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {
						if (strings.size() < 6) {
							continue;
						}

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						String penjelasan = "";
						try {
							penjelasan = strings.get(16);
						} catch (Exception e) {

						}
						PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) Common
								.getContentAsObject(penjelasan, PenjelasanBankSoal.class, null);

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) {

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) {

						}

						String no = "0";
						try {
							no = strings.get(19);
						} catch (Exception e) {

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(UjianPunyaSoal.class)
								.add(Restrictions.eq("ujian", ujian)).createAlias("bankSoal", "bankSoal")
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("bankSoal.id", Long.parseLong(id.trim())),
										Restrictions.ilike("bankSoal.soal", soal, MatchMode.EXACT)))
								.setProjection(Projections.property("bankSoal")).setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
							newBankSoal.setDosen(tbmuser == null ? null : tbmuser.ambilDosen());
							newBankSoal.setMatakuliah(ujian.getMatakuliah());
						}
							try {
								newBankSoal.setSkor(skor == null || skor.trim().length() == 0 ? 0.0
										: Double.parseDouble(skor.trim()));
							} catch (Exception e) {
								newBankSoal.setSkor(0.0);
							}
							try {
								newBankSoal.setSkorSalah(skorSalah == null || skorSalah.trim().length() == 0 ? 0.0
										: Double.parseDouble(skorSalah.trim()));
							} catch (Exception e) {
								newBankSoal.setSkorSalah(0.0);
							}
							try {
								newBankSoal.setSkorDefault(benarDefault == null || benarDefault.trim().length() == 0
										? 0.0 : Double.parseDouble(benarDefault.trim()));
							} catch (Exception e) {
								newBankSoal.setSkorDefault(0.0);
							}

						newBankSoal.setJenis(ujian == null ? jenis : ujian.getJenis());
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);

						try {
							if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
								Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();

								Dosen dosen = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getDosen()
										: (pertemuan == null || pertemuan.getPerkuliahan() == null ? null
												: pertemuan.getPerkuliahan().getDosen1());
								Matakuliah matakuliah = pertemuan == null || pertemuan.getPerkuliahan() == null ? null
										: pertemuan.getPerkuliahan().getMatakuliah();
								Matapelajaran matapelajaran = pertemuan == null
										|| pertemuan.getJadwalPelajaran() == null ? null
												: pertemuan.getJadwalPelajaran().getMatapelajaran();
								Guru guru = tbmuser != null && tbmuser.getDosen() != null ? tbmuser.getGuru()
										: (pertemuan == null || pertemuan.getJadwalPelajaran() == null ? null
												: pertemuan.getJadwalPelajaran().getGuru());

								if (newBankSoal.getDosen() == null && dosen != null) {
									newBankSoal.setDosen(dosen);
								}
								if (newBankSoal.getMatakuliah() == null && matakuliah != null) {
									newBankSoal.setMatakuliah(matakuliah);
								}
								if (newBankSoal.getMatapelajaran() == null && matapelajaran != null) {
									newBankSoal.setMatapelajaran(matapelajaran);
								}
								if (newBankSoal.getGuru() == null || guru != null) {
									newBankSoal.setGuru(guru);
								}

							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2643");
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
										jumlahJawaban++;
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:2683");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						if (ujian != null) {
							try {
								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) session
										.createCriteria(UjianPunyaSoal.class)
										.add(Restrictions.eq("bankSoal", newBankSoal))
										.add(Restrictions.eq("ujian", ujian)).setMaxResults(1).uniqueResult();
								if (ujianPunyaSoal == null) {
									ujianPunyaSoal = new UjianPunyaSoal();
									ujianPunyaSoal.setBankSoal(newBankSoal);
									ujianPunyaSoal.setUjian(ujian);
								}

								try {
									ujianPunyaSoal.setNomorUrut(Integer.parseInt(no.trim()));
								} catch (Exception e) {

								}

								session.getTransaction().begin();
								session.saveOrUpdate(ujianPunyaSoal);
								session.getTransaction().commit();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2744");
							}
						}

						terupload++;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailUjianHelper.java:2751");
				}

			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else

		{
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	private void uploadSoal(Media media, Ujian ujian) throws Exception {
		DetailUjianHelper.doUpload(media, ujian, this, pertemuanPunyaUjian);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {
		refreshSoal = value == null ? false : (Boolean) value;

		Object[] objects = ujian.ambilUjianPunyaSoal(refreshSoal, pertemuanPunyaUjian, cari.getValue().trim(),
				Common.ROWS_COUNT_ON_PAGE_1 * (paging == null ? 0 : paging.getActivePage()),
				Common.ROWS_COUNT_ON_PAGE_1);
		List<Long> ujianPunyaSoals = (List<Long>) objects[0];
		Integer size = (Integer) objects[1];

		System.out.println("ujianPunyaSoals => " + ujianPunyaSoals);
		ListModel strset = new SimpleListModel(ujianPunyaSoals);
		grid.setRowRenderer(new DetailUjianRenderer());
		grid.setModel(strset);
		grid.setSclass("fgrid");
//		grid.setOddRowSclass("non-odd");

		try {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE_1);
			paging.setMold("os");
			paging.setTotalSize(size);
			paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE_1);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailUjianHelper.java:2806");

		}
	}

}
