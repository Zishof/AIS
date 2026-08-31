package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.BukuBahanAjarHelper;
import ais.action.master.helper.MatakuliahEkivalenHelper;
import ais.action.master.helper.MatakuliahPrasyaratHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiMatakuliahHelper;
import ais.action.master.helper.impor.ImportFromEpsbedHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.obe.CapaianLulusanVsKurikulumMatakuliahAction;
import ais.action.master.obe.MatakuliahVsBahanKajianAction;
import ais.action.master.obe.MatakuliahVsCapaianLulusanAction;
import ais.action.master.obe.MatakuliahVsKurikulumAction;
import ais.action.master.obe.MatakuliahVsKurikulumVsSemesterAction;
import ais.action.report.format1.akademik.LaporanRekapitulasiMahasiswaYangMengambilMatakuliah;
import ais.action.report.format1.akademik.LaporanRekapitulasiMahasiswaYangTidakMengambilMatakuliah;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.MatakuliahDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KelompokMatakuliah;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.MatakuliahPrasyarat;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Prefix;
import ais.database.model.StatusMatakuliah;
import ais.database.model.Tbmuser;
import ais.database.model.TingkatKesulitanMatakuliah;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.action.master.helper.GenerateAiHelper;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk matakuliah. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code MyCheckboxConfig searchmikinsendiri};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code getDspaceParentTahunAkademik()}, {@code getDspace()},
 * {@code onUploadDBF()}, {@code onLaporanRekapitulasiMahasiswaYangMengambilMatakuliah()}, {@code
 * onLaporanRekapitulasiMahasiswaYangTidakMengambilMatakuliah()}, {@code onSearchDefault()});
 * validasi/perhitungan ({@code checkKodeMatkul()}, {@code checkKodeSajaMatkul()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onCapaian()}, {@code tombolGenAiTeks()}, {@code generateTeksAiMk()},
 * {@code appendCsvMk()}, {@code generateBahanKajianAi()}, {@code generateCpmkAiMk()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class MatakuliahAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchkode;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private MyCheckboxConfig searchmikinsendiri;
	private Combobox searchjenjang;
	private MyCheckboxConfig searchBelumMasukFeeder;
	private MyCheckboxConfig searchMasukFeeder;
	private MyCheckboxConfig searchextraKulikuler;

	private MyCheckboxConfig searchpraktek;
	private MyCheckboxConfig searchprakteklapangan;
	private MyCheckboxConfig searchteori;
	private MyCheckboxConfig searchsimulasi;
	private MyCheckboxConfig searchmodul;
	private MyCheckboxConfig searchpra;
	private MyCheckboxConfig searchumum;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox namaEn;
	private Textbox keterangan;

	private Textbox deskripsiPembelajaran;
	private Textbox capaianPembelajaranProdi;

	private MyCheckboxConfig extraKulikuler;
	private Decimalbox sks;
	private MyDoublebox sksSubMk;
	private Decimalbox sksPraktek;
	private Decimalbox sksDiskusi;
	private Decimalbox sksPraktekLapangan;
	private Decimalbox sksSimulasi;

	protected Combobox kelompokMatakuliah;
	private Combobox jurusan;
	private Combobox fakultas;
	private MyCheckboxConfig milikUniversitas;
	private MyCheckboxConfig bolehDiambilProdiLain;
	private Combobox status;
	private MyCheckboxConfig terdapatPraktek;
	private MyCheckboxConfig terdapatDiskusi;
	private MyCheckboxConfig merupakanMkPraktek;
	private MyCheckboxConfig merupakanMkTeori;
	private MyCheckboxConfig merupakanModul;
	private MyCheckboxConfig merupakanPraPerkuliahan;
	private Textbox singkatan;
	private Combobox jenisMatakuliah;

	private Combobox prefix;
	private Combobox kesulitan;

	private MyCheckboxConfig adaSap;
	private MyCheckboxConfig adaSilabus;
	private MyCheckboxConfig adaBahanAjar;
	private MyCheckboxConfig adaAcaraPraktek;
	private MyCheckboxConfig adaDiktat;

	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;

	private Matakuliah matakuliah;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Label jenjang;
	// private MyColumnConfig Praktek;
	// private MyColumnConfig Diskusi;

	private Row rowSksPraktek;
	private Row rowSksDiskusi;

	private Tabpanel manajemenKelompokMatakuliah;
	private Tabpanel panelRekapitulasiMahasiswaYangMengambilMatakuliah;
	private Tabpanel panelRekapitulasiMahasiswaYangTidakMengambilMatakuliah;
	private Tabpanel prasyarat;
	private Tabpanel ekivalen;
	private Tbmuser tbmuser;
	private PerguruanTinggi perguruanTinggi;

	private Tabpanel manajemenCapaian;
	private Tabpanel manajemenBahanKajian;
	private Tabpanel manajemenKurikulum;
	private Tabpanel manajemenJenisMkKurikulum;
	private Tabpanel manajemenCapaianLulusanKurikulum;

	public void onCapaian(Event event) {
		if (manajemenCapaian.getChildren().size() == 0) {
			MatakuliahVsCapaianLulusanAction laporan = new MatakuliahVsCapaianLulusanAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenCapaian);
		}
	}

	/** Tombol kecil (ikon sparkles) untuk generate teks AI ke sebuah Textbox. */
	private MyToolbarbuttonConfig tombolGenAiTeks(final String judul, final String instruksi, final Textbox target) {
		MyToolbarbuttonConfig b = new MyToolbarbuttonConfig("", "/img/svg/sparkles.svg");
		try {
			b.setTooltiptext(Common.getBahasaConfig(judul));
		} catch (Exception e) {
		}
		b.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;padding:5px 9px;"
				+ "margin-left:6px;border:none;cursor:pointer;");
		b.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
				String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
				StringBuilder p = new StringBuilder();
				p.append(instruksi).append("\n\n");
				p.append("Nama Matakuliah: ").append(namaMk).append("\n");
				p.append("Kode: ").append(kodeMk).append("\n");
				String cur = target.getValue();
				if (cur != null && cur.trim().length() > 0) {
					p.append("Teks saat ini (boleh diperbaiki/dilengkapi): ").append(cur.trim()).append("\n");
				}
				generateTeksAiMk(Common.getBahasaConfig(judul), p.toString(), target);
			}
		});
		return b;
	}

	/** Generate teks AI (streaming) → popup editable → Terapkan mengisi Textbox target. */
	private void generateTeksAiMk(final String judul, final String prompt, final Textbox target) throws Exception {
		GenerateAiHelper.jalankanAiStreaming(judul, prompt, new GenerateAiHelper.HasilAi() {
			@Override
			public void selesai(String resp) throws Exception {
				final String hasil = resp.trim();
				final MyWindow win = new MyWindow(judul, "none", true);
				win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				win.setWidth("620px");
				Vbox vb = new Vbox();
				vb.setStyle("padding:16px;width:100%;box-sizing:border-box;");
				vb.setWidth("100%");
				vb.setParent(win);
				Label info = new Label(
						Common.getBahasaConfig("Tinjau/edit hasil AI lalu Terapkan untuk mengisi kolom."));
				info.setStyle("font-size:11px;color:#64748b;margin-bottom:8px;display:block;");
				vb.appendChild(info);
				final Textbox box = new Textbox();
				box.setMultiline(true);
				box.setRows(10);
				box.setWidth("100%");
				box.setValue(hasil);
				box.setParent(vb);
				Hbox bb = new Hbox();
				bb.setStyle("margin-top:14px;");
				vb.appendChild(bb);
				MyToolbarbuttonConfig ok = new MyToolbarbuttonConfig(Common.getBahasaConfig("Terapkan"),
						"/img/svg/check2.svg");
				ok.setStyle("color:#fff;background-color:#16a34a;border-radius:6px;padding:6px 14px;border:none;"
						+ "cursor:pointer;margin-right:6px;");
				ok.setParent(bb);
				ok.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						target.setValue(box.getValue() != null ? box.getValue().trim() : "");
						win.detach();
					}
				});
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
						"/img/svg/close-circle-line.svg");
				cancel.setStyle("color:#fff;background-color:#dc2626;border-radius:6px;padding:6px 14px;border:none;"
						+ "cursor:pointer;");
				cancel.setParent(bb);
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						win.detach();
					}
				});
				win.onModal();
			}
		});
	}

	private String appendCsvMk(String csv, Long id) {
		if (id == null) {
			return csv;
		}
		String sid = String.valueOf(id);
		if (csv == null || csv.trim().length() == 0) {
			return sid;
		}
		for (String p : csv.split(",")) {
			if (p.trim().equals(sid)) {
				return csv;
			}
		}
		return csv + "," + sid;
	}

	/** Generate Bahan Kajian via AI: rekomendasi BK cocok + usul BK baru → popup → Terapkan (update MK + refresh). */
	private void generateBahanKajianAi() throws Exception {
		final String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final Jurusan jurSel = (jurusan.getSelectedItem() == null) ? null
				: (Jurusan) jurusan.getSelectedItem().getValue();

		final List<String[]> pool = new java.util.ArrayList<String[]>();
		List<BahanKajian> bks = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(BahanKajian.class)
						.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
						.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurSel)))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
				BahanKajian.class);
		for (BahanKajian bk : bks) {
			pool.add(new String[]{ String.valueOf(bk.getId()), bk.getKode() != null ? bk.getKode() : "",
					bk.getNama() != null ? bk.getNama() : "" });
		}

		StringBuilder psb = new StringBuilder();
		psb.append("Nama Matakuliah: ").append(namaMk).append("\n");
		psb.append("Kode: ").append(kodeMk).append("\n");
		psb.append("\nDaftar Bahan Kajian yang tersedia:\n");
		if (pool.isEmpty()) {
			psb.append("(Belum ada Bahan Kajian terdaftar)\n");
		} else {
			for (String[] p : pool) {
				psb.append(p[1]).append(" - ").append(p[2]).append("\n");
			}
		}
		psb.append("\nTolong analisis: Bahan Kajian mana yang paling cocok untuk matakuliah ini?\n");
		psb.append("Juga usulkan Bahan Kajian BARU jika relevan tapi belum ada di daftar.\n\n");
		psb.append("Format jawaban WAJIB (jangan tambah teks lain):\n");
		psb.append("COCOK: [kode1, kode2, ...]\n");
		psb.append("ALASAN_COCOK: [alasan singkat]\n");
		psb.append("USUL_BARU:\n");
		psb.append("- [KODE_BARU]: [deskripsi singkat bahan kajian baru]\n");
		psb.append("(tulis TIDAK ADA jika tidak ada usulan baru)\n");
		final String promptAi = psb.toString();
		final List<String[]> poolFinal = pool;
		final Jurusan jurFinal = jurSel;

		GenerateAiHelper.jalankanAiStreaming(Common.getBahasaConfig("Generate Bahan Kajian berdasarkan AI"),
				promptAi, new GenerateAiHelper.HasilAi() {
					@Override
					public void selesai(String resp) throws Exception {
						final List<String> cocokKodes = new java.util.ArrayList<String>();
						final String[] alasanHolder = new String[]{ "" };
						final List<String[]> usulBaru = new java.util.ArrayList<String[]>();
						boolean inUsul = false;
						for (String l : resp.split("\\n")) {
							String t = l.trim();
							String tu = t.toUpperCase();
							if (tu.startsWith("COCOK:")) {
								String val = t.substring("COCOK:".length()).trim().replaceAll("[\\[\\]]", "");
								for (String part : val.split("[,;]")) {
									String kd = part.trim();
									if (!kd.isEmpty()) {
										cocokKodes.add(kd.toUpperCase());
									}
								}
								inUsul = false;
							} else if (tu.startsWith("ALASAN_COCOK:")) {
								alasanHolder[0] = t.substring("ALASAN_COCOK:".length()).trim().replaceAll("[\\[\\]]", "")
										.trim();
								inUsul = false;
							} else if (tu.startsWith("USUL_BARU:")) {
								inUsul = true;
							} else if (inUsul && t.startsWith("-")) {
								String item = t.substring(1).trim();
								int ci = item.indexOf(":");
								if (ci > 0) {
									String k = item.substring(0, ci).trim().replaceAll("[\\[\\]]", "");
									String n = item.substring(ci + 1).trim().replaceAll("[\\[\\]]", "");
									if (!k.isEmpty() && !n.isEmpty() && !k.equalsIgnoreCase("TIDAK ADA")) {
										usulBaru.add(new String[]{ k, n });
									}
								}
							}
						}
						final List<String[]> direko = new java.util.ArrayList<String[]>();
						for (String[] p : poolFinal) {
							String kode = p[1] != null ? p[1].trim().toUpperCase() : "";
							for (String kd : cocokKodes) {
								if (kd.equalsIgnoreCase(kode)) {
									direko.add(p);
									break;
								}
							}
						}

						final MyWindow win = new MyWindow(Common.getBahasaConfig("Rekomendasi AI - Bahan Kajian"),
								"none", true);
						win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						win.setWidth("620px");
						win.setHeight("80%");
						org.zkoss.zk.ui.Component scrollHost = Common.tampilanScroll(win);
						Vbox vb = new Vbox();
						vb.setStyle("padding:16px;width:100%;box-sizing:border-box;");
						vb.setWidth("100%");
						vb.setParent(scrollHost);

						if (!alasanHolder[0].isEmpty()) {
							Label a = new Label(Common.getBahasaConfig("Alasan AI") + ": " + alasanHolder[0]);
							a.setStyle("font-size:12px;color:#475569;background:#f1f5f9;padding:8px 12px;"
									+ "border-radius:6px;display:block;margin-bottom:10px;");
							a.setMultiline(true);
							vb.appendChild(a);
						}
						Label t1 = new Label(Common.getBahasaConfig("Bahan Kajian yang Direkomendasikan"));
						t1.setStyle("font-weight:bold;color:#0f172a;display:block;margin:6px 0;");
						vb.appendChild(t1);
						final List<Checkbox> cbCocok = new java.util.ArrayList<Checkbox>();
						if (direko.isEmpty()) {
							Label none = new Label(
									"(" + Common.getBahasaConfig("Tidak ada yang sesuai / semua sudah dipilih") + ")");
							none.setStyle("font-size:12px;color:#94a3b8;");
							vb.appendChild(none);
						} else {
							for (String[] p : direko) {
								Hbox r = new Hbox();
								r.setStyle("margin-bottom:6px;");
								vb.appendChild(r);
								Checkbox cb = new Checkbox();
								cb.setChecked(true);
								cb.setLabel(p[1] + " - " + p[2]);
								cb.setValue(p[0]);
								cb.setParent(r);
								cbCocok.add(cb);
							}
						}
						final List<Checkbox> cbUsul = new java.util.ArrayList<Checkbox>();
						final List<String[]> usulFinal = usulBaru;
						if (!usulBaru.isEmpty()) {
							Label t2 = new Label(Common.getBahasaConfig("Usulan Bahan Kajian Baru"));
							t2.setStyle("font-weight:bold;color:#0f172a;display:block;margin:8px 0 4px;");
							vb.appendChild(t2);
							Label i2 = new Label(Common
									.getBahasaConfig("Centang untuk membuat Bahan Kajian baru & menambahkannya."));
							i2.setStyle("font-size:11px;color:#64748b;margin-bottom:6px;");
							vb.appendChild(i2);
							for (String[] ub : usulBaru) {
								Hbox r = new Hbox();
								r.setStyle("margin-bottom:6px;");
								vb.appendChild(r);
								Checkbox cb = new Checkbox();
								cb.setChecked(false);
								cb.setLabel("[" + ub[0] + "] " + ub[1]);
								cb.setValue(ub[0]);
								cb.setParent(r);
								cbUsul.add(cb);
							}
						}

						Hbox bb = new Hbox();
						bb.setStyle("margin-top:16px;");
						vb.appendChild(bb);
						MyToolbarbuttonConfig ok = new MyToolbarbuttonConfig(Common.getBahasaConfig("Terapkan"),
								"/img/svg/check2.svg");
						ok.setStyle("color:#fff;background-color:#16a34a;border-radius:6px;padding:6px 14px;"
								+ "border:none;cursor:pointer;margin-right:6px;");
						ok.setParent(bb);
						ok.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								for (Checkbox cb : cbCocok) {
									if (cb.isChecked() && cb.getValue() != null
											&& !((String) cb.getValue()).trim().isEmpty()) {
										try {
											Long id = Long.parseLong(((String) cb.getValue()).trim());
											matakuliah.setBahanKajian(appendCsvMk(matakuliah.getBahanKajian(), id));
										} catch (NumberFormatException nfe) {
										}
									}
								}
								if (!cbUsul.isEmpty()) {
									org.hibernate.Session saveS = null;
									try {
										saveS = HibernateUtil.openSession();
										saveS.beginTransaction();
										for (int idx = 0; idx < cbUsul.size(); idx++) {
											Checkbox cb = cbUsul.get(idx);
											if (cb.isChecked() && idx < usulFinal.size()) {
												String[] ub = usulFinal.get(idx);
												BahanKajian nb = new BahanKajian();
												nb.setKode(ub[0]);
												nb.setNama(ub[1]);
												nb.setPerguruanTinggi(perguruanTinggi);
												nb.setJurusan(jurFinal);
												nb.setAktif(Boolean.TRUE);
												nb.setKhususBuatMk(matakuliah);
												saveS.save(nb);
												saveS.flush();
												if (nb.getId() != null) {
													matakuliah.setBahanKajian(
															appendCsvMk(matakuliah.getBahanKajian(), nb.getId()));
												}
											}
										}
										saveS.getTransaction().commit();
									} catch (Exception eSave) {
										if (saveS != null && saveS.getTransaction() != null) {
											try {
												saveS.getTransaction().rollback();
											} catch (Exception er) {
											}
										}
										ais.common.ErrorAuditUtil.record(eSave,
												"MatakuliahAction.generateBahanKajianAi.save");
									} finally {
										if (saveS != null) {
											try {
												saveS.close();
											} catch (Exception er) {
											}
										}
									}
								}
								win.detach();
								try {
									if (ubahBahanKajianRef != null) {
										ubahBahanKajianRef.onEvent(null);
									}
								} catch (Exception er) {
									ais.common.ErrorAuditUtil.record(er, "MatakuliahAction.generateBahanKajianAi.refresh");
								}
							}
						});
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
								"/img/svg/close-circle-line.svg");
						cancel.setStyle("color:#fff;background-color:#dc2626;border-radius:6px;padding:6px 14px;"
								+ "border:none;cursor:pointer;");
						cancel.setParent(bb);
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								win.detach();
							}
						});
						win.onModal();
					}
				});
	}

	/** Generate CPMK via AI: rekomendasi CPMK cocok + usul CPMK baru → popup → Terapkan (update MK + refresh). */
	private void generateCpmkAiMk() throws Exception {
		final String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final Jurusan jurSel = (jurusan.getSelectedItem() == null) ? null
				: (Jurusan) jurusan.getSelectedItem().getValue();

		final List<String[]> pool = new java.util.ArrayList<String[]>();
		List<CapaianPembelajaranLulusan> ls = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(CapaianPembelajaranLulusan.class)
						.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
						.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurSel)))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nama")),
				CapaianPembelajaranLulusan.class);
		for (CapaianPembelajaranLulusan c : ls) {
			String kd = c.getKode() != null ? c.getKode() : ("CPMK" + c.getId());
			pool.add(new String[]{ String.valueOf(c.getId()), kd, c.getNama() != null ? c.getNama() : "" });
		}

		StringBuilder psb = new StringBuilder();
		psb.append("Nama Matakuliah: ").append(namaMk).append("\n");
		psb.append("Kode: ").append(kodeMk).append("\n");
		psb.append("\nDaftar CPMK (Capaian Pembelajaran Matakuliah) yang tersedia:\n");
		if (pool.isEmpty()) {
			psb.append("(Belum ada CPMK terdaftar)\n");
		} else {
			for (String[] p : pool) {
				psb.append(p[1]).append(" - ").append(p[2]).append("\n");
			}
		}
		psb.append("\nTolong analisis: CPMK mana yang paling cocok dibebankan pada matakuliah ini?\n");
		psb.append("Juga usulkan CPMK BARU jika relevan tapi belum ada di daftar.\n\n");
		psb.append("Format jawaban WAJIB (jangan tambah teks lain):\n");
		psb.append("COCOK: [kode1, kode2, ...]\n");
		psb.append("ALASAN_COCOK: [alasan singkat]\n");
		psb.append("USUL_BARU:\n");
		psb.append("- [KODE_BARU]: [rumusan CPMK baru yang disarankan]\n");
		psb.append("(tulis TIDAK ADA jika tidak ada usulan baru)\n");
		final String promptAi = psb.toString();
		final List<String[]> poolFinal = pool;
		final Jurusan jurFinal = jurSel;

		GenerateAiHelper.jalankanAiStreaming(Common.getBahasaConfig("Generate CPMK berdasarkan AI"), promptAi,
				new GenerateAiHelper.HasilAi() {
					@Override
					public void selesai(String resp) throws Exception {
						final List<String> cocokKodes = new java.util.ArrayList<String>();
						final String[] alasanHolder = new String[]{ "" };
						final List<String[]> usulBaru = new java.util.ArrayList<String[]>();
						boolean inUsul = false;
						for (String l : resp.split("\\n")) {
							String t = l.trim();
							String tu = t.toUpperCase();
							if (tu.startsWith("COCOK:")) {
								String val = t.substring("COCOK:".length()).trim().replaceAll("[\\[\\]]", "");
								for (String part : val.split("[,;]")) {
									String kd = part.trim();
									if (!kd.isEmpty()) {
										cocokKodes.add(kd.toUpperCase());
									}
								}
								inUsul = false;
							} else if (tu.startsWith("ALASAN_COCOK:")) {
								alasanHolder[0] = t.substring("ALASAN_COCOK:".length()).trim().replaceAll("[\\[\\]]", "")
										.trim();
								inUsul = false;
							} else if (tu.startsWith("USUL_BARU:")) {
								inUsul = true;
							} else if (inUsul && t.startsWith("-")) {
								String item = t.substring(1).trim();
								int ci = item.indexOf(":");
								if (ci > 0) {
									String k = item.substring(0, ci).trim().replaceAll("[\\[\\]]", "");
									String n = item.substring(ci + 1).trim().replaceAll("[\\[\\]]", "");
									if (!k.isEmpty() && !n.isEmpty() && !k.equalsIgnoreCase("TIDAK ADA")) {
										usulBaru.add(new String[]{ k, n });
									}
								}
							}
						}
						final List<String[]> direko = new java.util.ArrayList<String[]>();
						for (String[] p : poolFinal) {
							String kode = p[1] != null ? p[1].trim().toUpperCase() : "";
							for (String kd : cocokKodes) {
								if (kd.equalsIgnoreCase(kode)) {
									direko.add(p);
									break;
								}
							}
						}

						final MyWindow win = new MyWindow(Common.getBahasaConfig("Rekomendasi AI - CPMK"), "none", true);
						win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						win.setWidth("640px");
						win.setHeight("80%");
						org.zkoss.zk.ui.Component scrollHost = Common.tampilanScroll(win);
						Vbox vb = new Vbox();
						vb.setStyle("padding:16px;width:100%;box-sizing:border-box;");
						vb.setWidth("100%");
						vb.setParent(scrollHost);

						if (!alasanHolder[0].isEmpty()) {
							Label a = new Label(Common.getBahasaConfig("Alasan AI") + ": " + alasanHolder[0]);
							a.setStyle("font-size:12px;color:#475569;background:#f1f5f9;padding:8px 12px;"
									+ "border-radius:6px;display:block;margin-bottom:10px;");
							a.setMultiline(true);
							vb.appendChild(a);
						}
						Label t1 = new Label(Common.getBahasaConfig("CPMK yang Direkomendasikan"));
						t1.setStyle("font-weight:bold;color:#0f172a;display:block;margin:6px 0;");
						vb.appendChild(t1);
						final List<Checkbox> cbCocok = new java.util.ArrayList<Checkbox>();
						if (direko.isEmpty()) {
							Label none = new Label(
									"(" + Common.getBahasaConfig("Tidak ada yang sesuai / semua sudah dipilih") + ")");
							none.setStyle("font-size:12px;color:#94a3b8;");
							vb.appendChild(none);
						} else {
							for (String[] p : direko) {
								Hbox r = new Hbox();
								r.setStyle("margin-bottom:6px;");
								vb.appendChild(r);
								Checkbox cb = new Checkbox();
								cb.setChecked(true);
								cb.setLabel(p[2]);
								cb.setValue(p[0]);
								cb.setParent(r);
								cbCocok.add(cb);
							}
						}
						final List<Checkbox> cbUsul = new java.util.ArrayList<Checkbox>();
						final List<String[]> usulFinal = usulBaru;
						if (!usulBaru.isEmpty()) {
							Label t2 = new Label(Common.getBahasaConfig("Usulan CPMK Baru"));
							t2.setStyle("font-weight:bold;color:#0f172a;display:block;margin:8px 0 4px;");
							vb.appendChild(t2);
							Label i2 = new Label(
									Common.getBahasaConfig("Centang untuk membuat CPMK baru & menambahkannya."));
							i2.setStyle("font-size:11px;color:#64748b;margin-bottom:6px;");
							vb.appendChild(i2);
							for (String[] ub : usulBaru) {
								Hbox r = new Hbox();
								r.setStyle("margin-bottom:6px;");
								vb.appendChild(r);
								Checkbox cb = new Checkbox();
								cb.setChecked(false);
								cb.setLabel("[" + ub[0] + "] " + ub[1]);
								cb.setValue(ub[0]);
								cb.setParent(r);
								cbUsul.add(cb);
							}
						}

						Hbox bb = new Hbox();
						bb.setStyle("margin-top:16px;");
						vb.appendChild(bb);
						MyToolbarbuttonConfig ok = new MyToolbarbuttonConfig(Common.getBahasaConfig("Terapkan"),
								"/img/svg/check2.svg");
						ok.setStyle("color:#fff;background-color:#16a34a;border-radius:6px;padding:6px 14px;"
								+ "border:none;cursor:pointer;margin-right:6px;");
						ok.setParent(bb);
						ok.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								for (Checkbox cb : cbCocok) {
									if (cb.isChecked() && cb.getValue() != null
											&& !((String) cb.getValue()).trim().isEmpty()) {
										try {
											Long id = Long.parseLong(((String) cb.getValue()).trim());
											matakuliah.setCapaianPembelajaranLulusan(
													appendCsvMk(matakuliah.getCapaianPembelajaranLulusan(), id));
										} catch (NumberFormatException nfe) {
										}
									}
								}
								if (!cbUsul.isEmpty()) {
									org.hibernate.Session saveS = null;
									try {
										saveS = HibernateUtil.openSession();
										saveS.beginTransaction();
										for (int idx = 0; idx < cbUsul.size(); idx++) {
											Checkbox cb = cbUsul.get(idx);
											if (cb.isChecked() && idx < usulFinal.size()) {
												String[] ub = usulFinal.get(idx);
												CapaianPembelajaranLulusan nc = new CapaianPembelajaranLulusan();
												nc.setKode(ub[0]);
												nc.setNama(ub[1]);
												nc.setPerguruanTinggi(perguruanTinggi);
												nc.setJurusan(jurFinal);
												nc.setAktif(Boolean.TRUE);
												nc.setKhususBuatMk(matakuliah);
												saveS.save(nc);
												saveS.flush();
												if (nc.getId() != null) {
													matakuliah.setCapaianPembelajaranLulusan(appendCsvMk(
															matakuliah.getCapaianPembelajaranLulusan(), nc.getId()));
												}
											}
										}
										saveS.getTransaction().commit();
									} catch (Exception eSave) {
										if (saveS != null && saveS.getTransaction() != null) {
											try {
												saveS.getTransaction().rollback();
											} catch (Exception er) {
											}
										}
										ais.common.ErrorAuditUtil.record(eSave, "MatakuliahAction.generateCpmkAiMk.save");
									} finally {
										if (saveS != null) {
											try {
												saveS.close();
											} catch (Exception er) {
											}
										}
									}
								}
								win.detach();
								try {
									if (ubahCapaianLulusanPadaMkRef != null) {
										ubahCapaianLulusanPadaMkRef.onEvent(null);
									}
								} catch (Exception er) {
									ais.common.ErrorAuditUtil.record(er, "MatakuliahAction.generateCpmkAiMk.refresh");
								}
							}
						});
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
								"/img/svg/close-circle-line.svg");
						cancel.setStyle("color:#fff;background-color:#dc2626;border-radius:6px;padding:6px 14px;"
								+ "border:none;cursor:pointer;");
						cancel.setParent(bb);
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								win.detach();
							}
						});
						win.onModal();
					}
				});
	}

	public void onBahanKajian(Event event) {
		if (manajemenBahanKajian.getChildren().size() == 0) {
			MatakuliahVsBahanKajianAction laporan = new MatakuliahVsBahanKajianAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenBahanKajian);
		}
	}

	public void onKurikulum(Event event) {
		if (manajemenKurikulum.getChildren().size() == 0) {
			MatakuliahVsKurikulumAction laporan = new MatakuliahVsKurikulumAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKurikulum);
		}
	}

	public void onJenisMkKurikulum(Event event) {
		if (manajemenJenisMkKurikulum.getChildren().size() == 0) {
			MatakuliahVsKurikulumVsSemesterAction laporan = new MatakuliahVsKurikulumVsSemesterAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenJenisMkKurikulum);
		}
	}

	public void onCapaianLulusanKurikulum(Event event) {
		if (manajemenCapaianLulusanKurikulum.getChildren().size() == 0) {
			CapaianLulusanVsKurikulumMatakuliahAction laporan = new CapaianLulusanVsKurikulumMatakuliahAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenCapaianLulusanKurikulum);
		}
	}

	public static DspaceInformation getDspaceParentTahunAkademik(String cookie, Matakuliah matakuliah)
			throws Exception {

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Artefak Perkuliahan");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", "Berisi semua artefak perkuliahan " + Common.getBahasaConfig("Jurusan") + " "
				+ matakuliah.getJurusan().getNama());
		jsonPost.put("shortDescription",
				"Artefak perkuliahan " + Common.getBahasaConfig("Jurusan") + " " + matakuliah.getJurusan().getNama());
		jsonPost.put("sidebarText",
				"Artefak perkuliahan " + Common.getBahasaConfig("Jurusan") + " " + matakuliah.getJurusan().getNama());

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_artefak_perkuliahan_" + matakuliah.getJurusan().getId(), "");

		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, matakuliah.getJurusan(), false) + "/communities");
	}

	public static DspaceInformation getDspace(String cookie, Matakuliah matakuliah) throws Exception {

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", matakuliah.getKode() + " - " + matakuliah.getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", "Berisi semua artefak matakuliah dengan kode " + matakuliah.getKode()
				+ " dan nama " + matakuliah.getNama());
		jsonPost.put("shortDescription", "Artefak matakuliah " + matakuliah.getKode() + " - " + matakuliah.getNama());
		jsonPost.put("sidebarText", "Artefak matakuliah " + matakuliah.getKode() + " - " + matakuliah.getNama());
		return DspaceInformation.dspaceProcess(cookie, matakuliah, jsonPost.toString(), false, "communities",
				"communities/" + MatakuliahAction.getDspaceParentTahunAkademik(cookie, matakuliah) + "/communities");
	}

	public void onUploadDBF(Event event) throws Exception {

		Clients.showBusy("Upload data matakuliah .......");

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
		if (media.getName().trim().equalsIgnoreCase("TBKMK.DBF")) {
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

			ImportFromEpsbedHelper.doImport(file);

			String sql = ImportFromEpsbedHelper.read("matakuliah.sql");
			Session session = HibernateUtil.currentSession();
			session.createSQLQuery(sql).executeUpdate();
		} else {
			MyMessageboxConfig.show("File yang anda upload harus TBKMK.DBF" + media, "Error", MyMessageboxConfig.OK,
					MyMessageboxConfig.ERROR);
		}

		Clients.clearBusy();
	}

	public void onManajemenKelompokMatakuliah(Event event) {
		if (manajemenKelompokMatakuliah.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompokMatakuliah);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_matakuliah.zul");
			iframe.setParent(window);
		}
	}

	public void onPrasyarat(Event event) {
		if (prasyarat.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(prasyarat);
			MyInclude iframe = new MyInclude("/pages/master/matakuliah_prasyarat.zul");
			iframe.setParent(window);
		}
	}

	public void onEkivalen(Event event) {
		if (ekivalen.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(ekivalen);
			MyInclude iframe = new MyInclude("/pages/master/matakuliah_ekivalen.zul");
			iframe.setParent(window);
		}
	}

	public void onLaporanRekapitulasiMahasiswaYangMengambilMatakuliah(Event event) {
		if (panelRekapitulasiMahasiswaYangMengambilMatakuliah.getChildren().size() == 0) {
			LaporanRekapitulasiMahasiswaYangMengambilMatakuliah window = new LaporanRekapitulasiMahasiswaYangMengambilMatakuliah();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(panelRekapitulasiMahasiswaYangMengambilMatakuliah);
		}
	}

	public void onLaporanRekapitulasiMahasiswaYangTidakMengambilMatakuliah(Event event) {
		if (panelRekapitulasiMahasiswaYangTidakMengambilMatakuliah.getChildren().size() == 0) {
			LaporanRekapitulasiMahasiswaYangTidakMengambilMatakuliah window = new LaporanRekapitulasiMahasiswaYangTidakMengambilMatakuliah();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(panelRekapitulasiMahasiswaYangTidakMengambilMatakuliah);
		}
	}

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
			Common.goLogoff();
			return;
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		tbmuser = Common.getCurrentUser();

		jenisMatakuliah = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Common.getBahasa("keberadaan_matakuliah_kampus"));
		if (comboitem != null) { comboitem.setValue(Common.getBahasa("keberadaan_matakuliah_kampus")); }
		jenisMatakuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Common.getBahasa("keberadaan_matakuliah_luar_kampus"));
		if (comboitem != null) { comboitem.setValue(Common.getBahasa("keberadaan_matakuliah_luar_kampus")); }
		jenisMatakuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Common.getBahasa("keberadaan_matakuliah_lain"));
		if (comboitem != null) { comboitem.setValue(Common.getBahasa("keberadaan_matakuliah_lain")); }
		jenisMatakuliah.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertCombo(jurusan = new Combobox(), "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Session session = HibernateUtil.currentSession();

		if (((Number) session.createCriteria(StatusMatakuliah.class).add(Restrictions.ilike("nama", "Wajib"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() == 0) {
			StatusMatakuliah statusMatakuliah = new StatusMatakuliah();
			statusMatakuliah.setNama("Wajib");
			session.save(statusMatakuliah);
			session.flush();
		}

		if (((Number) session.createCriteria(StatusMatakuliah.class).add(Restrictions.ilike("nama", "Pilihan"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() == 0) {
			StatusMatakuliah statusMatakuliah = new StatusMatakuliah();
			statusMatakuliah.setNama("Pilihan");
			session.save(statusMatakuliah);
			session.flush();
		}

		if (((Number) session.createCriteria(StatusMatakuliah.class).add(Restrictions.ilike("nama", "Wajib Peminatan"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() == 0) {
			StatusMatakuliah statusMatakuliah = new StatusMatakuliah();
			statusMatakuliah.setNama("Wajib Peminatan");
			session.save(statusMatakuliah);
			session.flush();
		}

		if (((Number) session.createCriteria(StatusMatakuliah.class)
				.add(Restrictions.ilike("nama", "Pilihan Peminatan")).setProjection(Projections.rowCount())
				.uniqueResult()).intValue() == 0) {
			StatusMatakuliah statusMatakuliah = new StatusMatakuliah();
			statusMatakuliah.setNama("Pilihan Peminatan");
			session.save(statusMatakuliah);
			session.flush();
		}

		if (((Number) session.createCriteria(StatusMatakuliah.class)
				.add(Restrictions.ilike("nama", "Tugas akhir/Skripsi/Tesis/Disertasi"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() == 0) {
			StatusMatakuliah statusMatakuliah = new StatusMatakuliah();
			statusMatakuliah.setNama("Tugas akhir/Skripsi/Tesis/Disertasi");
			session.save(statusMatakuliah);
			session.flush();
		}

		@SuppressWarnings("unchecked")
		List<StatusMatakuliah> statusMatakuliahs = ConstantValues.simpleList(
				session.createCriteria(StatusMatakuliah.class).addOrder(Order.asc("nama")), StatusMatakuliah.class);

		status = new Combobox();
		for (StatusMatakuliah statusMatakuliah : statusMatakuliahs) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(statusMatakuliah.getNama());
			comboitem.setValue(statusMatakuliah.getNama());
			status.appendChild(comboitem);
		}
		if (status != null) { status.setReadonly(true); }

		if (searchmikinsendiri != null) { searchmikinsendiri.setVisible(false); }
		searchmikinsendiri.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (searchmikinsendiri.isChecked()) {
					searchjurusan.setSelectedItem(null);
					searchfakultas.setSelectedItem(null);

					searchfakultas.setDisabled(true);
					searchjurusan.setDisabled(true);
					initCriteria(true);
				} else {
					fakultas = new Combobox();
					jurusan = new Combobox();
					Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);
				}
			}
		});

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertCombo(prefix = new Combobox(), "namaPrefix", Prefix.class);
		Common.insertCombo(kesulitan = new Combobox(), "keterangan", TingkatKesulitanMatakuliah.class);
		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (uploadData != null) { uploadData.setVisible(false); }
		if (downloadFormatMatakuliah != null) { downloadFormatMatakuliah.setVisible(false); }
		// uploadDBF.setVisible(uploadData.isVisible());

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("KODE KELOMPOK");
		columnHeadersAdding.add("NAMA KELOMPOK");
		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Matakuliah matakuliah = (Matakuliah) objects[0];
				// Long id = (Long) objects[1];
				XSSFRow row = (XSSFRow) objects[2];

				Session session = HibernateUtil.currentNativeSession();

				ProjectionList projections = Projections.projectionList();
				projections.add(Projections.property("kelompokMatakuliah.kode"));
				projections.add(Projections.property("kelompokMatakuliah.nama"));

				Object[] dosenPa = (Object[]) session.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
						.add(Restrictions.eq("matakuliah", matakuliah))
						.createAlias("kelompokMatakuliah", "kelompokMatakuliah").setProjection(projections)
						.setMaxResults(1).uniqueResult();

				row.createCell(contents.length)
						.setCellValue(dosenPa == null || dosenPa[0] == null ? "" : dosenPa[0].toString());

				row.createCell(contents.length + 1)
						.setCellValue(dosenPa == null || dosenPa[1] == null ? "" : dosenPa[1].toString());

				HibernateUtil.closeSession();
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Matakuliah.class, this,
				"Download Matakuliah", "/img/print.png", columnHeadersAdding, dataAdding, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Matakuliah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit); }
		Common.appendKeToolbar(upload, add, comp);

		Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig bersihkan = bersihkanKrsMahasiswaDouble("Lihat Matakuliah dengan kode double",
				"/img/excel.png");
		bersihkan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_nonaktifkan_mk_double"));

		Common.appendKeToolbar(bersihkan, add, comp);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													List<Matakuliah> tbmusers = ConstantValues
															.simpleList(initCriteria(true), Matakuliah.class);
													int size = tbmusers.size();
													int index = 1;
													for (Matakuliah matakuliah : tbmusers) {
														myLabelProsesDetail.setValue("Memproses " + matakuliah.getKode()
																+ " " + matakuliah.getNama() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.matakuliah(matakuliah, errorLog);
													}
													tbmusers.clear();
													tbmusers = null;
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												myLabelProsesDetail.setValue("");
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);

			buttonTagihan = new MyToolbarbuttonConfig("Ambil dari feeder", "/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													String filter = "";

													if (!searchnama.getValue().trim().isEmpty()) {
														for (String o : searchnama.getValue().trim().split(";")) {
															String s = "nama_mata_kuliah like '%" + o + "%'";
															filter += filter.isEmpty() ? s : " or " + s;
														}
													}

													if (!searchkode.getValue().trim().isEmpty()) {
														for (String o : searchkode.getValue().trim().split(";")) {
															String s = "kode_mata_kuliah like '%" + o + "%'";
															filter += filter.isEmpty() ? s : " or " + s;
														}
													}

													Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null
															|| searchjurusan.getSelectedItem().getValue() == null ? null
																	: searchjurusan.getSelectedItem().getValue());
													if (jur != null && jur.getFeeder() != null
															&& !jur.getFeeder().isEmpty()) {
														String s = "id_prodi='" + jur.getFeeder() + "'";
														filter += filter.isEmpty() ? s : " and " + s;
													}

													Integer countInteger = feederConnector.getCount(token,
															"GetCountMataKuliah", filter);
													boolean melihatKode = Common.bolehKonfigurasi("melihat_kode_mk_syn_data");
													String or = "kode_mata_kuliah asc";

													for (int index = 0; index <= countInteger; index += 100) {

														JSONArray dataMataKuliah = feederConnector.getData(
																"GetListMataKuliah", token, filter, or, "100",
																index + "");

														for (int i = 0; i < dataMataKuliah.length(); i++) {
															JSONObject jsonObject = dataMataKuliah.getJSONObject(i);
															FeederJSONImport.matakuliah(jsonObject, melihatKode);
														}

													}

												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												myLabelProsesDetail.setValue("");
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setDisabled(!edit); }
		if (button != null) { button.setVisible((add != null && add.isVisible()) && edit); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiMatakuliahHelper revisiHelper = new RevisiMatakuliahHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	}

	public MyToolbarbuttonConfig bersihkanKrsMahasiswaDouble(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Prodi", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox fakultas = new Combobox();
				final Combobox jurusan = new Combobox();

				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");
				fakultas.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");
				jurusan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig cariID;
				row.appendChild(
						cariID = new MyCheckboxConfig("Cari kesamaan kode matakuliah hanya di prodi yang sama"));
				cariID.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig cariNama;
				row.appendChild(cariNama = new MyCheckboxConfig(
						"Cari kesamaan kode dan nama matakuliah, jika tidak dipilih hanya kesamaan kode saja"));
				cariNama.setChecked(true);

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

					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final Fakultas f = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan j = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final boolean samaProdi = cariID.isChecked();
						final boolean samNama = cariNama.isChecked();

						final List<Long> dataDihapus = new ArrayList<Long>();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(8);
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										// ID mata kuliah yang ditandai MERAH (baris pertama tiap kelompok ganda) =
										// kandidat yang akan dinonaktifkan / diaktifkan kembali. dipakai oleh kedua
										// tombol di bawah.
										final String daftarIdCsv;
										{
											StringBuilder sb = new StringBuilder();
											for (Long id : dataDihapus) {
												if (sb.length() > 0) {
													sb.append(",");
												}
												sb.append(id);
											}
											daftarIdCsv = sb.toString();
										}
										final int jumlahDouble = dataDihapus.size();

										MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
												"Non aktifkan salah satu data double (warna merah)", "/img/excel.png");
										proses.setVisible(jumlahDouble > 0);
										proses.setTooltiptext(
												"Menonaktifkan mata kuliah yang ditandai warna merah agar tidak lagi terpakai");
										proses.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												MyMessageboxConfig.show(
														"Mohon perhatian Bapak/Ibu. Tindakan ini akan MENONAKTIFKAN sebanyak "
																+ jumlahDouble
																+ " data mata kuliah yang ditandai dengan warna merah, yaitu salah satu dari setiap pasangan/kelompok data ganda (double)."
																+ "\n\nKonsekuensi yang perlu Bapak/Ibu pahami sebelum melanjutkan:"
																+ "\n1. Mata kuliah yang dinonaktifkan TIDAK akan lagi muncul pada daftar mata kuliah aktif, pemilihan KRS, penjadwalan, maupun proses akademik lain yang menyaring hanya data aktif."
																+ "\n2. Data historis yang sudah terlanjur menaut ke mata kuliah tersebut (nilai, transkrip, jadwal lampau) TIDAK dihapus, namun keterkaitannya sebaiknya diperiksa terlebih dahulu agar tidak ada riwayat yang kehilangan rujukan."
																+ "\n3. Penonaktifan ini dapat dikembalikan sewaktu-waktu melalui tombol \"Kembalikan menjadi aktif (data double)\" di sebelah tombol ini."
																+ "\n\nApakah Bapak/Ibu benar-benar yakin ingin melanjutkan penonaktifan " + jumlahDouble
																+ " data mata kuliah ganda tersebut?",
														"Konfirmasi Penonaktifan Data Ganda",
														org.zkoss.zul.Messagebox.YES | org.zkoss.zul.Messagebox.NO,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@Override
															public void onEvent(Event ev) throws Exception {
																if (!"onYes".equals(ev.getName())) {
																	return;
																}
																if (daftarIdCsv.isEmpty()) {
																	return;
																}
																org.hibernate.Session session = HibernateUtil
																		.currentSession();
																String sql = "update matakuliah set aktif=false where id in ("
																		+ daftarIdCsv + ")";
																session.createSQLQuery(sql).executeUpdate();
																// Sinkronkan objek di memori dgn DB (cegah objek basi nilai aktif lama).
																sinkronkanMatakuliahDenganDatabase(session, dataDihapus);
																onSearchDefault(ev);
																window.detach();
															}
														});
											}
										});
										proses.setParent(toolbar);

										// Tombol KEBALIKAN: mengaktifkan kembali data ganda yang tadi dinonaktifkan.
										//
										// PENTING (perbaikan bug): tombol ini TIDAK boleh memakai dataDihapus. dataDihapus
										// adalah id baris pertama tiap grup double yang AKTIF saat popup dibuka; sedangkan
										// query deteksi double hanya melihat yang AKTIF. Sesudah "Non aktifkan", data yang
										// dinonaktifkan menjadi TIDAK aktif sehingga tak lagi masuk daftar itu, dan meng-
						               // update dataDihapus (yang sudah aktif) menjadi aktif=true tidak mengubah apa pun.
										//
										// Kebalikan yang BENAR: aktifkan kembali mata kuliah yang saat ini NON-AKTIF tetapi
										// masih memiliki "kembaran" yang aktif (kode/prodi/nama sama sesuai opsi pencarian) --
										// yaitu tepat mata kuliah yang tadi dinonaktifkan oleh fitur ini. Query dibangun ulang
										// memakai opsi yang sama (samaProdi/samNama) dan penyaring fakultas/prodi yang sama.
										MyToolbarbuttonConfig aktifkan = new MyToolbarbuttonConfig(
												"Kembalikan menjadi aktif (data double)", "/img/excel.png");
										// Selalu tampil: sesudah dinonaktifkan, jumlah double AKTIF bisa berkurang/0, tetapi
										// data non-aktif tetap perlu bisa dikembalikan.
										aktifkan.setVisible(true);
										aktifkan.setTooltiptext(
												"Mengaktifkan kembali mata kuliah ganda yang sebelumnya dinonaktifkan");
										aktifkan.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												MyMessageboxConfig.show(
														"Mohon perhatian Bapak/Ibu. Tindakan ini merupakan KEBALIKAN dari penonaktifan: sistem akan MENGAKTIFKAN KEMBALI seluruh mata kuliah yang sebelumnya dinonaktifkan karena ganda (yaitu mata kuliah non-aktif yang masih memiliki kembaran aktif dengan kode"
																+ (samaProdi ? "/prodi" : "") + (samNama ? "/nama" : "") + " yang sama)."
																+ "\n\nKonsekuensi yang perlu Bapak/Ibu pahami sebelum melanjutkan:"
																+ "\n1. Setelah diaktifkan kembali, data mata kuliah ganda tersebut AKAN MUNCUL LAGI secara bersamaan dengan pasangannya, sehingga kondisi DUPLIKAT (double) akan kembali terjadi pada daftar mata kuliah aktif, pemilihan KRS, dan penjadwalan."
																+ "\n2. Kemunculan data ganda dapat menimbulkan kebingungan dalam pemilihan mata kuliah serta potensi kesalahan pencatatan nilai/jadwal apabila mahasiswa atau petugas memilih data yang keliru."
																+ "\n3. Tindakan ini hanya sebaiknya dilakukan bila penonaktifan sebelumnya keliru atau perlu ditinjau ulang. Sesudah diaktifkan, Bapak/Ibu dapat menonaktifkannya lagi melalui tombol di sebelahnya."
																+ "\n\nApakah Bapak/Ibu benar-benar yakin ingin mengaktifkan kembali data mata kuliah ganda yang sebelumnya dinonaktifkan, dengan memahami bahwa kondisi duplikat akan muncul lagi?",
														"Konfirmasi Pengaktifan Kembali Data Ganda",
														org.zkoss.zul.Messagebox.YES | org.zkoss.zul.Messagebox.NO,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@Override
															public void onEvent(Event ev) throws Exception {
																if (!"onYes".equals(ev.getName())) {
																	return;
																}
																org.hibernate.Session session = HibernateUtil
																		.currentSession();
																// Query REVERSE: cari mata kuliah NON-AKTIF yang punya kembaran AKTIF dengan
																// kunci sama (kode + prodi/nama sesuai opsi), dibatasi penyaring fakultas/prodi
																// yang sama seperti deteksi double. Dipilih dulu ID-nya (bukan langsung UPDATE
																// berbasis EXISTS) agar diketahui persis id mana yang berubah, sehingga objek di
																// memori bisa disinkronkan dengan basis data sesudahnya.
																StringBuilder sel = new StringBuilder();
																sel.append("select m.id from matakuliah m where (m.aktif = false) and exists (");
																sel.append("select 1 from matakuliah a where a.id <> m.id and (a.aktif = true or a.aktif is null) and a.kode = m.kode");
																if (samaProdi) {
																	sel.append(" and a.jurusan = m.jurusan");
																}
																if (samNama) {
																	sel.append(" and a.nama = m.nama");
																}
																sel.append(")");
																if (j != null) {
																	sel.append(" and m.jurusan = ").append(j.getId());
																}
																if (f != null) {
																	sel.append(" and m.jurusan in (select id from jurusan where fakultas = ")
																			.append(f.getId()).append(")");
																}

																@SuppressWarnings("unchecked")
																List<Object> idRows = session.createSQLQuery(sel.toString())
																		.list();
																List<Long> idKembali = new ArrayList<Long>();
																StringBuilder csvKembali = new StringBuilder();
																for (Object r : idRows) {
																	if (r instanceof Number) {
																		Long id = Long.valueOf(((Number) r).longValue());
																		idKembali.add(id);
																		if (csvKembali.length() > 0) {
																			csvKembali.append(",");
																		}
																		csvKembali.append(id);
																	}
																}

																int jumlahKembali = 0;
																if (csvKembali.length() > 0) {
																	jumlahKembali = session.createSQLQuery(
																			"update matakuliah set aktif=true where id in ("
																					+ csvKembali + ")").executeUpdate();
																	// Sinkronkan objek di memori dengan DB (cegah objek basi nilai aktif lama).
																	sinkronkanMatakuliahDenganDatabase(session, idKembali);
																}
																onSearchDefault(ev);
																window.detach();
																MyMessageboxConfig.show(
																		jumlahKembali > 0
																				? jumlahKembali
																						+ " mata kuliah berhasil diaktifkan kembali. Data ganda kini muncul lagi bersama pasangannya."
																				: "Tidak ada mata kuliah non-aktif yang perlu dikembalikan (kemungkinan belum ada yang dinonaktifkan sebagai data ganda).",
																		"Informasi", MyMessageboxConfig.OK,
																		jumlahKembali > 0 ? MyMessageboxConfig.INFORMATION
																				: MyMessageboxConfig.EXCLAMATION);
															}
														});
											}
										});
										aktifkan.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA MK Double");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();

										String sql = "select c.kode,"
												+ (samaProdi ? "c.jurusan" : "max(c.jurusan) as jurusan") + ","
												+ (samNama ? "c.nama" : "max(c.nama) as nama")
												+ " from  matakuliah c left join jurusan d on (c.jurusan=d.id) \n"
												+ "where 1=1 and (c.aktif or c.aktif is null) "
												+ (f != null ? " and d.fakultas=" + f.getId() : "") + " " + " "
												+ (j != null ? " and c.jurusan=" + j.getId() : "") + ""
												+ " group by c.kode" + (samaProdi ? ",c.jurusan" : "")
												+ (samNama ? ",c.nama" : "") + (samNama ? ",c.nama" : "")
												+ " having count(*) > 1 ";
										List<Object[]> data = session.createSQLQuery(sql).list();
										intbox.setValue(data.size());
										System.out.println("sql = " + sql + "\n\ndata = " + data.size());

										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "id", "kode", "nama", "prodi", "sks",
												"kurikulum" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										for (Object[] datas : data) {
											int indexKe = 0;

											try {

												String kode = (String) datas[0];
												Long prodi = ((Number) datas[1]).longValue();
												String nama = (String) datas[2];

												List<Matakuliah> matakuliahs = session.createCriteria(Matakuliah.class)
														.addOrder(Order.asc("id"))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.ilike("kode", kode, MatchMode.EXACT))
														.add(samaProdi ? Restrictions.eq("jurusan.id", prodi)
																: Restrictions.sqlRestriction("true"))
														.add(samNama ? Restrictions.ilike("nama", nama, MatchMode.EXACT)
																: Restrictions.sqlRestriction("true"))
														.list();

												for (Matakuliah matakuliah : matakuliahs) {
													List<String> kurikulumsPakaiTerakhir = session
															.createCriteria(KurikulumPunyaMatakuliah.class)
															.createAlias("kurikulum", "kurikulum")
															.add(Restrictions.eq("matakuliah", matakuliah))
															.setProjection(Projections.property("kurikulum.nama"))
															.addOrder(Order.desc("kurikulum.tahun")).list();
													rowIndex++;
													XSSFRow row = sheet.createRow(rowIndex);
													XSSFCell cell0 = row.createCell(0);
													if (indexKe == 0) {
														dataDihapus.add(matakuliah.getId());
														cell0.setCellStyle(lockedNumericStyle);
													}
													indexKe++;

													label.setValue("Sedang memproses data " + matakuliah + " ");

													cell0.setCellValue(matakuliah.getId());
													row.createCell(1).setCellValue(kode);
													row.createCell(2).setCellValue(matakuliah.getNama());
													row.createCell(3).setCellValue(matakuliah.getJurusan() == null ? ""
															: matakuliah.getJurusan().getNama());
													row.createCell(4).setCellValue(matakuliah.getSks());

													String lgn = "";
													for (String d : kurikulumsPakaiTerakhir) {
														lgn += lgn.trim().isEmpty() ? d : "," + d;
													}

													row.createCell(5).setCellValue(lgn);
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}
										System.out.println("Your excel file has been generated! ");
										data.clear();
										data = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
									HibernateUtil.closeSession();
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	/**
	 * Menyinkronkan objek {@link Matakuliah} di dalam memori dengan kondisi terbaru di basis data
	 * SESUDAH menjalankan perintah UPDATE SQL manual (mis. {@code update matakuliah set aktif=...}).
	 *
	 * <p><b>Kenapa perlu.</b> {@code createSQLQuery(...).executeUpdate()} mengubah baris LANGSUNG di
	 * basis data, tetapi TIDAK menyentuh objek yang sudah termuat di sesi Hibernate, di cache aplikasi
	 * (MapDB), maupun instance kanonik pada {@link ais.common.EntityIdentityMap}. Akibatnya objek di
	 * memori bisa "basi" &mdash; masih menyimpan nilai {@code aktif} lama &mdash; sehingga tampilan
	 * atau proses lain yang memegang objek itu melihat data yang tidak sesuai basis data.</p>
	 *
	 * <p><b>Cara kerja.</b> Untuk setiap id: muat objek pada sesi (bila sudah ada di cache sesi,
	 * {@code session.refresh(...)} memaksa membaca ulang kolomnya dari basis data), lalu perbarui
	 * cache aplikasi + instance kanonik lewat {@link ais.common.DataUtil#masukkanDataLangsung} yang
	 * menjamin <b>hanya ada satu objek per (kelas, id)</b> dan seluruh pemegang referensi objek itu
	 * langsung melihat nilai terbaru. Setiap id dibungkus try/catch agar satu kegagalan tidak
	 * menghentikan sinkronisasi id lain. Memakai {@link HibernateUtil#currentSession()} (konteks ZK,
	 * TIDAK ditutup manual).</p>
	 *
	 * @param session sesi Hibernate aktif tempat UPDATE tadi dijalankan.
	 * @param ids     daftar id mata kuliah yang barusan diubah oleh UPDATE SQL.
	 */
	private void sinkronkanMatakuliahDenganDatabase(org.hibernate.Session session, java.util.Collection<Long> ids) {
		if (session == null || ids == null) {
			return;
		}
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			try {
				Object obj = session.get(Matakuliah.class, id);
				if (obj instanceof Matakuliah) {
					Matakuliah mk = (Matakuliah) obj;
					try {
						// Baca ulang kolom dari basis data ke instance yang mungkin masih basi di sesi.
						session.refresh(mk);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e,
								"auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:sinkronRefresh");
					}
					// Perbarui cache aplikasi (MapDB) + instance KANONIK (kelas,id) agar semua pemegang
					// objek mata kuliah ini melihat nilai aktif terbaru, bukan salinan basi.
					ais.common.DataUtil.masukkanDataLangsung(Matakuliah.class, mk, String.valueOf(id));
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:sinkronMatakuliah");
			}
		}
	}

	private String[] contents = new String[] { "id", "kode", "nama", "namaEn", "sks", "sksDiskusi", "sksPraktek",
			"sksPraktekLapangan", "sksSimulasi", "status", "jurusan", "singkatan", "jenisMatakuliah",
			"kelompokMatakuliah", "milikUniversitas", "bolehDiambilProdiLain", "extraKulikuler", "merupakanModul",
			"sksSubMk", "feeder", "aktif" };

	private MyToolbarbuttonConfig uploadData;
	private MyToolbarbuttonConfig downloadFormatMatakuliah;
	private MyCheckboxConfig terdapatSimulasi;
	private MyCheckboxConfig terdapatPraktekLapangan;
	private MyCheckboxConfig terdapatUts;
	private MyCheckboxConfig terdapatUas;
	private Row rowSksPraktekLapangan;
	private Row rowSKSSimulasi;
	private Textbox metodeKuliah;
	private Textbox feeder;
	private Row rowSksSubMk;
	private MyCheckboxConfig merupakanPerkuliahanUmum;
	private Decimalbox jumlahMaksimalSksJikaAmbilMkIni;
	private HashMap<Long, BahanKajian> selectedBahanKajian;
	private EventListener ubahBahanKajianRef;
	private EventListener ubahCapaianLulusanPadaMkRef;
	private Row rowBk;
	private HashMap<Long, CapaianLulusan> selectedCapaianLulusan;
	private HashMap<Long, CapaianPembelajaranLulusan> selectedCapaianPembelajaranLulusan;
	private Row rowCpl;
	private Combobox jenisNilaiHuruf;
	private Row rowCpmk;

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		private MatakuliahPrasyaratHelper matakuliahPrasyaratHelper = new MatakuliahPrasyaratHelper();
		private MatakuliahEkivalenHelper matakuliahEkivalenHelper = new MatakuliahEkivalenHelper(null, null, null);
		private BukuBahanAjarHelper bukuBahanAjarHelper = new BukuBahanAjarHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("450px");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Buku Ajar");
						tabSoal.setParent(tabs);

						MyTabConfig tabJawaban = new MyTabConfig("Matakuliah Prasyarat");
						tabJawaban.setParent(tabs);

						MyTabConfig tabEkivalen = new MyTabConfig("Matakuliah Ekivalen");
						tabEkivalen.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
						tabpanel.setParent(tabpanels);

						bukuBahanAjarHelper.display(matakuliah, tabpanel, null);

						tabpanel = new ais.ui.util.MyTabpanel();
						tabpanel.setParent(tabpanels);

						matakuliahPrasyaratHelper.display(matakuliah, tabpanel, addWindow);

						tabpanel = new ais.ui.util.MyTabpanel();
						tabpanel.setParent(tabpanels);

						matakuliahEkivalenHelper.display(matakuliah, tabpanel);
					}
				}

			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ")").setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(Matakuliah.class, matakuliah, matakuliah.getNama());

			new Label(matakuliah.getNamaEn()).setParent(a);

			MatakuliahPrasyaratAction.tampilPrasyarat(a, matakuliah);

			Vbox vbox = new Vbox();
			a.setParent(vbox);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (matakuliah.getFeeder() != null && !matakuliah.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											String[] kon = EksporFromFeederAction.koneksi();
											final String ip = kon[0];
											final String port = kon[1];
											final String username = kon[2];
											final String password = kon[3];
											final String url = kon[4];

											if (!EksporFromFeederAction.exists(url)) {

												MyMessageboxConfig.show(
														ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											final List<String> errorLog = new ArrayList<String>();

											final Label myLabelProsesDetail = Common
													.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(err, "Error Terjadi",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");

																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
															}

															onSearchDefault(null);
														}
													});

											new Thread(new Runnable() {

												@Override
												public void run() {
													try {
														FeederConnector feederConnector = new FeederConnector(ip,
																Integer.parseInt(port), null);

														String token = feederConnector.getToken(username, password);
														System.out.println("TOKEN => " + token);

														if (token == null || token.trim().isEmpty()
																|| token.trim().toLowerCase().startsWith("error")) {
															myLabelProsesDetail
																	.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null,
																myLabelProsesDetail);
														myLabelProsesDetail.setValue("Mengirim data " + matakuliah);
														feederImporter.matakuliah(matakuliah, errorLog);

													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													myLabelProsesDetail.setValue("");
												}
											}).start();

										}

									}
								});

					}
				});
				buttonTagihan.setParent(myHbox);
			}

			vbox.setParent(arg0);

			// Daftar kurikulum dirapikan: container ber-scroll (tidak menumpuk panjang),
			// diawali ringkasan jumlah, lalu tiap kurikulum ditampilkan sebagai "chip" rapi.
			org.zkoss.zul.Div kurBox = new org.zkoss.zul.Div();
			kurBox.setSclass("ais-kur-cell");
			@SuppressWarnings("unchecked")
			List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.add(Restrictions.isNotNull("kurikulum")).add(Restrictions.eq("matakuliah", matakuliah)),
					KurikulumPunyaMatakuliah.class);
			if (kurikulumPunyaMatakuliahs != null && !kurikulumPunyaMatakuliahs.isEmpty()) {
				Label kurCount = new Label(kurikulumPunyaMatakuliahs.size() + " kurikulum");
				kurCount.setSclass("ais-kur-count");
				kurCount.setParent(kurBox);
			} else {
				Label kurKosong = new Label("—");
				kurKosong.setSclass("ais-kur-empty");
				kurKosong.setParent(kurBox);
			}
			int i = 1;
			for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
				String namaKur = kurikulumPunyaMatakuliah.getKurikulum() == null ? "-"
						: kurikulumPunyaMatakuliah.getKurikulum().getNama();
				Label chip = new Label(i + ". " + namaKur + " · smt " + kurikulumPunyaMatakuliah.getSemester());
				chip.setSclass("ais-kur-chip");
				chip.setParent(kurBox);
				i++;
			}
			kurBox.setParent(arg0);
			kurikulumPunyaMatakuliahs = null;

			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getJenjang() == null ? ""
					: matakuliah.getJurusan().getJenjang().getNama()).setParent(arg0);

			new Label(matakuliah.getStatus()).setParent(arg0);

			KelompokMatakuliah mykelompokMatakuliah = null;

			try {
				mykelompokMatakuliah = matakuliah.getId() == null ? null
						: ((KelompokMatakuliah) ConstantValues.simpleObject(
								HibernateUtil.currentSession().createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
										.addOrder(Order.desc("id")).add(Restrictions.eq("matakuliah", matakuliah))
										.createAlias("kelompokMatakuliah", "kelompokMatakuliah")
										.add(Restrictions.or(Restrictions.isNull("kelompokMatakuliah.aktif"),
												Restrictions.eq("kelompokMatakuliah.aktif", true)))
										.setProjection(Projections.property("kelompokMatakuliah.id")).setMaxResults(1),
								KelompokMatakuliah.class, false));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:1519");
				// TODO: handle exception
			}

			new Label(mykelompokMatakuliah == null ? " (tanpa kelompok) " : mykelompokMatakuliah.getNama())
					.setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			if (matakuliah.getSks() > 0) {
				new MyLabelKecil("SKS:" + matakuliah.getSks()).setParent(vbox2);
			}
			if (matakuliah.getSksDiskusi() > 0) {
				new MyLabelKecil("Teori:" + matakuliah.getSksDiskusi()).setParent(vbox2);
			}
			if (matakuliah.getSksPraktek() > 0) {
				new MyLabelKecil("Praktek:" + matakuliah.getSksPraktek()).setParent(vbox2);
			}
			if (matakuliah.getSksPraktekLapangan() > 0) {
				new MyLabelKecil("Praktek Lap.:" + matakuliah.getSksPraktekLapangan()).setParent(vbox2);
			}
			if (matakuliah.getSksSimulasi() > 0) {
				new MyLabelKecil("Simulasi:" + matakuliah.getSksSimulasi()).setParent(vbox2);
			}

			new Label(matakuliah.getMilikUniversitas() == null ? "" : matakuliah.getMilikUniversitas() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(matakuliah.getBolehDiambilProdiLain() == null ? ""
					: matakuliah.getBolehDiambilProdiLain() ? "Ya" : "Tidak").setParent(arg0);

			new Label(matakuliah.getExtraKulikuler() == null ? "" : matakuliah.getExtraKulikuler() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label((matakuliah.getTerdapatUts() ? "Ya" : "Tidak") + "/"
					+ (matakuliah.getTerdapatUas() ? "Ya" : "Tidak")).setParent(arg0);

			final Label jumlah = new Label();
			arg0.appendChild(jumlah);
			final Timer timer = new Timer(500);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(timer);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					timer.detach();
					Session session = HibernateUtil.currentSession();
					Integer countEkivalen = ((Number) session.createCriteria(MatakuliahEkivalen.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("matakuliah", matakuliah)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					Integer countPrasyarat = ((Number) session.createCriteria(MatakuliahPrasyarat.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("matakuliah", matakuliah)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					Integer countPunyaBukuBahanAjar = ((Number) session
							.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
							.add(Restrictions.eq("matakuliah", matakuliah)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					String jml = countPunyaBukuBahanAjar + " bahan Ajar, " + countPrasyarat + " prasyarat, "
							+ countEkivalen + " ekivalen.";

					jumlah.setValue(jml);

				}
			});
			timer.start();

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(matakuliah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(matakuliah);
				}
			});

			// Kolom aksi dirapikan ke menu kebab (...) via UIHelper.buatBarisAksi supaya
			// konsisten dengan layar lain dan kolomnya menjadi kecil. Hbox bawaan
			// copyEditDeleteButtons diratakan satu level agar tiap tombol masuk popup.
			org.zkoss.zul.Hbox aksiHbox = Common.copyEditDeleteButtons(edit, delete, matakuliah, MatakuliahAction.this);
			/* WAJIB ambilItemAksi, BUKAN getChildren(): copyEditDeleteButtons sudah membangun
			 * kebab sendiri, sehingga anak langsung Hbox-nya adalah Popup + tombol pemicu "...",
			 * bukan tombol aksinya. Memakai getChildren() menghasilkan kebab BERSARANG (pengguna
			 * harus menekan "..." dua kali). ambilItemAksi menembus popup dan mengambil tombolnya. */
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>(ais.ui.util.UIHelper.ambilItemAksi(aksiHbox));
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	public void onAdd(Event event) throws Exception {
		init(new Matakuliah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		matakuliah = (Matakuliah) obj;
		init(matakuliah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final Matakuliah matakuliah) throws Exception {

		this.matakuliah = matakuliah;

		Common.clear(addWindow);
		addWindow.setTitle(Common.getBahasaConfig("Matakuliah"));
		addWindow.setWidth("850px");
		addWindow.setHeight("95%");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(kode = new Textbox(matakuliah.getKode() == null ? "" : matakuliah.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.getKonfigurasi("tampil_prefix_matakuliah", Konfigurasi.AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prefix"));
		Common.selectComboItem(prefix, matakuliah.getPrefix() == null ? null : matakuliah.getPrefix());
		row.appendChild(prefix);
		prefix.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.getKonfigurasi("tampil_tingkat_kesulitan_matakuliah", Konfigurasi.AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat Kesulitan"));
		Common.selectComboItem(kesulitan,
				matakuliah.getTingkatKesulitanMatakuliah() == null ? null : matakuliah.getTingkatKesulitanMatakuliah());
		row.appendChild(kesulitan);
		kesulitan.setWidth("90%");

		Integer countPerkuliahan = 0;
		Integer countDetailPerkuliahan = 0;
		try {

			if (Common.bolehKonfigurasi("aktifkan_Nama_dan_SKS_Matakuliah_tidak_dapat_diubah_jika_terdapat_matakuliah_yang_sudah_mengambil")) {

				if (matakuliah.getId() != null) {
					Session session = HibernateUtil.currentSession();
					countPerkuliahan = ((Number) session.createCriteria(Perkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.rowCount()).add(Restrictions.eq("matakuliah", matakuliah))
							.uniqueResult()).intValue();
					countDetailPerkuliahan = ((Number) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())
							.add(Restrictions.eq("matakuliahKonversi", matakuliah)).uniqueResult()).intValue();

				}

				row = new MyFormRow();
				row.setVisible((countPerkuliahan > 0 || countDetailPerkuliahan > 0));
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				row.appendChild(
						new ais.ui.util.MyLabelConfig("Nama dan SKS Matakuliah ini tidak dapat diubah karena terdapat "
								+ "matakuliah yang sudah mengambil mata kuliah ini."));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(matakuliah.getNama() == null ? "" : matakuliah.getNama()));
		nama.setWidth("90%");

		nama.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				singkatan.setValue(Common.generateSingkatan(nama.getValue()));
			}

		});

		Tbmuser tbmuser = Common.getCurrentUser();

		nama.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama English"));
		row.appendChild(namaEn = new Textbox(matakuliah.getNamaEn() == null ? "" : matakuliah.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Singkatan"));
		row.appendChild(singkatan = new Textbox(matakuliah.getSingkatan() == null ? "" : matakuliah.getSingkatan()));
		singkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan Sub Matakuliah / Modul"));
		row.appendChild(merupakanModul = new MyCheckboxConfig());
		merupakanModul.setChecked(matakuliah.getMerupakanModul());
		merupakanModul.setWidth("90%");

		rowSksSubMk = new MyFormRow();
		rowSksSubMk.setVisible(merupakanModul.isChecked());
		rowSksSubMk.setStyle("border:0px;background: transparent;");
		rowSksSubMk.setParent(rows);
		rowSksSubMk.appendChild(new Label("SKS Sub Matakuliah / Modul"));
		rowSksSubMk.appendChild(sksSubMk = new MyDoublebox(matakuliah.getSksSubMk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Extrakulikuler"));
		row.appendChild(extraKulikuler = new MyCheckboxConfig());
		extraKulikuler.setChecked(matakuliah.getExtraKulikuler());

		Common.initKeterangan(rows,
				"(nilai di matakuliah extrakulikuler tidak akan masuk ke KHS dan transkrip akademik)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Matakuliah"));
		Common.selectComboItem(status, matakuliah.getStatus());
		row.appendChild(status);
		status.setWidth("90%");

		KelompokMatakuliah mykelompokMatakuliah = matakuliah.getKelompokMatakuliah() != null
				? matakuliah.getKelompokMatakuliah()
				: (matakuliah.getId() == null ? null
						: ((KelompokMatakuliah) ConstantValues.simpleObject(
								HibernateUtil.currentSession().createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
										.addOrder(Order.desc("id")).add(Restrictions.eq("matakuliah", matakuliah))
										.createAlias("kelompokMatakuliah", "kelompokMatakuliah")
										.add(Restrictions.or(Restrictions.isNull("kelompokMatakuliah.aktif"),
												Restrictions.eq("kelompokMatakuliah.aktif", true)))
										.setProjection(Projections.property("kelompokMatakuliah.id")).setMaxResults(1),
								KelompokMatakuliah.class, false)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Matakuliah"));
		kelompokMatakuliah = new Combobox();
		Common.insertComboDanSemua(kelompokMatakuliah, new String[] { "nama", "namaen", "id" }, "keterangan",
				KelompokMatakuliah.class, "=Tanpa Kelompok=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(kelompokMatakuliah);
		Common.selectComboItem(kelompokMatakuliah, mykelompokMatakuliah);
		kelompokMatakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah milik Universitas"));
		row.appendChild(milikUniversitas = new MyCheckboxConfig());
		milikUniversitas.setChecked(matakuliah.getMilikUniversitas());
		milikUniversitas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan Matakuliah Pra Perkuliahan"));
		row.appendChild(merupakanPraPerkuliahan = new MyCheckboxConfig());
		merupakanPraPerkuliahan.setChecked(matakuliah.getMerupakanPraPerkuliahan());
		merupakanPraPerkuliahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan Matakuliah untuk Perkuliahan Umum"));
		row.appendChild(merupakanPerkuliahanUmum = new MyCheckboxConfig());
		merupakanPerkuliahanUmum.setChecked(matakuliah.getMerupakanPerkuliahanUmum());
		merupakanPerkuliahanUmum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Boleh Diambil Prodi Lain"));
		row.appendChild(bolehDiambilProdiLain = new MyCheckboxConfig());
		bolehDiambilProdiLain.setChecked(matakuliah.getBolehDiambilProdiLain());
		bolehDiambilProdiLain.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan MK Praktek (saja)"));
		row.appendChild(merupakanMkPraktek = new MyCheckboxConfig());
		merupakanMkPraktek.setChecked(matakuliah.getMerupakanMkPraktek());
		merupakanMkPraktek.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan MK Teori (saja)"));
		row.appendChild(merupakanMkTeori = new MyCheckboxConfig());
		merupakanMkTeori.setChecked(matakuliah.getMerupakanMkTeori());
		merupakanMkTeori.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Kegiatan Tatap Muka (Diskusi)"));
		row.appendChild(terdapatDiskusi = new MyCheckboxConfig());
		terdapatDiskusi.setChecked(matakuliah.getTerdapatDiskusi());
		terdapatDiskusi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Kegiatan Praktek"));
		row.appendChild(terdapatPraktek = new MyCheckboxConfig());
		terdapatPraktek.setChecked(matakuliah.getTerdapatPraktek());
		terdapatPraktek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Kegiatan Praktek Lapangan"));
		row.appendChild(terdapatPraktekLapangan = new MyCheckboxConfig());
		terdapatPraktekLapangan.setChecked(matakuliah.getTerdapatPraktekLapangan());
		terdapatPraktekLapangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Kegiatan Simulasi"));
		row.appendChild(terdapatSimulasi = new MyCheckboxConfig());
		terdapatSimulasi.setChecked(matakuliah.getTerdapatSimulasi());
		terdapatSimulasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Ujian Tengah Semester (UTS)"));
		row.appendChild(terdapatUts = new MyCheckboxConfig());
		terdapatUts.setChecked(matakuliah.getTerdapatUts());
		terdapatUts.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Ujian Akhir Semester (UAS)"));
		row.appendChild(terdapatUas = new MyCheckboxConfig());
		terdapatUas.setChecked(matakuliah.getTerdapatUas());
		terdapatUas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Mata Kuliah"));
		row.appendChild(sks = new Decimalbox(new BigDecimal(matakuliah.getSks() == null ? 2 : matakuliah.getSks())));
		sks.setWidth("90%");
		sks.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		rowSksDiskusi = new MyFormRow();
		rowSksDiskusi.setStyle("border:0px;background: transparent;");
		rowSksDiskusi.setParent(rows);
		rowSksDiskusi.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Tatap Muka (Diskusi)")));
		rowSksDiskusi.appendChild(sksDiskusi = new Decimalbox(new BigDecimal(matakuliah.getSksDiskusi())));
		sksDiskusi.setWidth("90%");
		sksDiskusi.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		rowSksPraktek = new MyFormRow();
		rowSksPraktek.setStyle("border:0px;background: transparent;");
		rowSksPraktek.setParent(rows);
		rowSksPraktek.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Praktikum")));
		rowSksPraktek.appendChild(sksPraktek = new Decimalbox(new BigDecimal(matakuliah.getSksPraktek())));
		sksPraktek.setWidth("90%");
		sksPraktek.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		rowSksPraktekLapangan = new MyFormRow();
		rowSksPraktekLapangan.setStyle("border:0px;background: transparent;");
		rowSksPraktekLapangan.setParent(rows);
		rowSksPraktekLapangan.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Praktek Lapangan")));
		rowSksPraktekLapangan
				.appendChild(sksPraktekLapangan = new Decimalbox(new BigDecimal(matakuliah.getSksPraktekLapangan())));
		sksPraktekLapangan.setWidth("90%");
		sksPraktekLapangan.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		rowSKSSimulasi = new MyFormRow();
		rowSKSSimulasi.setStyle("border:0px;background: transparent;");
		rowSKSSimulasi.setParent(rows);
		rowSKSSimulasi.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Simulasi")));
		rowSKSSimulasi.appendChild(sksSimulasi = new Decimalbox(new BigDecimal(matakuliah.getSksSimulasi())));
		sksSimulasi.setWidth("90%");
		sksSimulasi.setDisabled((countPerkuliahan > 0 || countDetailPerkuliahan > 0));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		Common.selectComboItem(fakultas,
				matakuliah.getJurusan() == null ? tbmuser.ambilFakultas() : matakuliah.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		Common.pilihJurusan(jurusan,
				matakuliah.getJurusan() == null ? tbmuser.ambilJurusan() : matakuliah.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		jenisNilaiHuruf = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai Huruf"));
		Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama" }, "keterangan",
				JenisNilaiHurufMatakuliah.class, "Nilai Huruf Default", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, jenisNilaiHuruf, matakuliah.getJenisNilaiHuruf());
		row.appendChild(jenisNilaiHuruf);
		jenisNilaiHuruf.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jml maksimal SKS saat ambil KRS"));
		row.appendChild(jumlahMaksimalSksJikaAmbilMkIni = new Decimalbox(
				new BigDecimal(matakuliah.getJumlahMaksimalSksJikaAmbilMkIni())));
		jumlahMaksimalSksJikaAmbilMkIni.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang = new Label());
		jenjang.setWidth("90%");

		final EventListener eventListenerJenjang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
				if (myJurusan != null && myJurusan.getJenjang() != null) {
					jenjang.setValue(myJurusan.getJenjang().getNama());
				}
			}
		};

		EventListener milikUniversitasEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (milikUniversitas.isChecked()) {
					fakultas.setDisabled(false);
					jurusan.setDisabled(false);
				} else {
					fakultas.setSelectedItem(null);
					jurusan.setSelectedItem(null);
					fakultas.setDisabled(true);
					jurusan.setDisabled(true);
				}
			}
		};

		EventListener terdapat = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				rowSksPraktek.setVisible(terdapatPraktek.isChecked());
				rowSksDiskusi.setVisible(terdapatDiskusi.isChecked());
				rowSksPraktekLapangan.setVisible(terdapatPraktekLapangan.isChecked());
				rowSKSSimulasi.setVisible(terdapatSimulasi.isChecked());
			}
		};

		milikUniversitas.addEventListener("onCheck", milikUniversitasEventListener);
		terdapatPraktek.addEventListener("onCheck", terdapat);
		terdapatDiskusi.addEventListener("onCheck", terdapat);
		terdapatPraktekLapangan.addEventListener("onCheck", terdapat);
		terdapatSimulasi.addEventListener("onCheck", terdapat);

		milikUniversitasEventListener.onEvent(null);
		terdapat.onEvent(null);

		EventListener merupakanModulEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowSksSubMk.setVisible(merupakanModul.isChecked());
				sks.getParent().setVisible(!merupakanModul.isChecked());
			}
		};

		merupakanModulEvent.onEvent(null);
		merupakanModul.addEventListener("onCheck", merupakanModulEvent);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Kuliah"));
		row.appendChild(metodeKuliah = new Textbox(matakuliah.getMetodeKuliah()));
		metodeKuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada SAP ?"));
		row.appendChild(adaSap = new MyCheckboxConfig());
		adaSap.setChecked(matakuliah.getAdaSap());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada Silabus ?"));
		row.appendChild(adaSilabus = new MyCheckboxConfig());
		adaSilabus.setChecked(matakuliah.getAdaSilabus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada Bahan Ajar ?"));
		row.appendChild(adaBahanAjar = new MyCheckboxConfig());
		adaBahanAjar.setChecked(matakuliah.getAdaBahanAjar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada Acara Praktek ?"));
		row.appendChild(adaAcaraPraktek = new MyCheckboxConfig());
		adaAcaraPraktek.setChecked(matakuliah.getAdaAcaraPraktek());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada Diktat ?"));
		row.appendChild(adaDiktat = new MyCheckboxConfig());
		adaDiktat.setChecked(matakuliah.getAdaDiktat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Efektif"));
		row.appendChild(tanggalMulai = new MyDatebox(matakuliah.getTanggalMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Efektif"));
		row.appendChild(tanggalSampai = new MyDatebox(matakuliah.getTanggalSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keberadaan"));
		Common.selectComboItem(jenisMatakuliah,
				matakuliah.getJenisMatakuliah() == null ? Common.getBahasa("keberadaan_matakuliah_kampus")
						: matakuliah.getJenisMatakuliah());
		row.appendChild(jenisMatakuliah);
		jenisMatakuliah.setWidth("90%");
		jenisMatakuliah.setReadonly(true);
		if (jenisMatakuliah.getSelectedItem() == null) {
			jenisMatakuliah.setSelectedIndex(0);
		}

		if (matakuliah.getId() != null) {
			AktifitasPerkuliahanHelper.tampilkanLampiran(rows, matakuliah.getId(), null, "_matakuliah", "", edit, "2");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(matakuliah.getKeterangan() == null ? "" : matakuliah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Deskripsi Pembelajaran"));
		row.appendChild(deskripsiPembelajaran = new Textbox(matakuliah.getDeskripsiPembelajaran()));
		deskripsiPembelajaran.setWidth("90%");
		deskripsiPembelajaran.setRows(3);
		row.appendChild(tombolGenAiTeks("Generate Deskripsi Pembelajaran berdasarkan AI",
				"Buatkan DEFAULT DESKRIPSI PEMBELAJARAN mata kuliah (1-2 kalimat, formal-akademis). "
						+ "Jawab HANYA isinya tanpa judul/format.",
				deskripsiPembelajaran));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Capaian / Kompetensi"));
		row.appendChild(capaianPembelajaranProdi = new Textbox(matakuliah.getCapaianPembelajaranProdi()));
		capaianPembelajaranProdi.setWidth("90%");
		capaianPembelajaranProdi.setRows(3);
		row.appendChild(tombolGenAiTeks("Generate Capaian / Kompetensi berdasarkan AI",
				"Buatkan DEFAULT CAPAIAN / KOMPETENSI mata kuliah (poin-poin ringkas atau 1-2 kalimat, "
						+ "formal-akademis). Jawab HANYA isinya tanpa judul/format.",
				capaianPembelajaranProdi));

		selectedBahanKajian = new HashMap<Long, BahanKajian>();

		rowBk = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowBk, "2");
		rowBk.setParent(rows);

		final EventListener ubahBahanKajian = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				eventListenerJenjang.onEvent(arg0);

				Jurusan s = (Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue());

				List<BahanKajian> bahanKajians = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(BahanKajian.class)
								.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
								.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", s)))
								.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						BahanKajian.class);

				selectedBahanKajian.clear();
				for (String d : matakuliah.getBahanKajian().split(",")) {
					try {
						if (!d.trim().isEmpty()) {
							Long idP = Long.parseLong(d);
							BahanKajian bahanKajian = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(),
									idP);
							if (bahanKajian != null && !selectedBahanKajian.containsKey(idP)) {
								selectedBahanKajian.put(idP, bahanKajian);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:2148");
						// TODO: handle exception
					}
				}

				Common.clear(rowBk);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowBk);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Bahan Kajian");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final BahanKajian bahanKajian : bahanKajians) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					final Checkbox checkbox = new Checkbox(bahanKajian.getKode() + " " + bahanKajian.getNama());
					checkbox.setParent(rowSkala);
					checkbox.setChecked(selectedBahanKajian.containsKey(bahanKajian.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedBahanKajian.put(bahanKajian.getId(), bahanKajian);
							} else {
								selectedBahanKajian.remove(bahanKajian.getId());
							}

						}
					});

				}
			}
		};
		ubahBahanKajianRef = ubahBahanKajian;

		// Tombol Generate Bahan Kajian via AI (pilih otomatis yang cocok + saran baru).
		MyFormRow rowAiBk = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowAiBk, "2");
		rowAiBk.setParent(rows);
		MyToolbarbuttonConfig btnAiBk = new MyToolbarbuttonConfig(
				Common.getBahasaConfig("Generate Bahan Kajian berdasarkan AI"), "/img/svg/sparkles.svg");
		btnAiBk.setStyle("font-size:12px;font-weight:bold;color:#ffffff;background-color:#7c3aed;"
				+ "border-radius:6px;padding:6px 15px;border:none;cursor:pointer;margin:4px 0;");
		btnAiBk.setParent(rowAiBk);
		btnAiBk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				generateBahanKajianAi();
			}
		});

		selectedCapaianLulusan = new HashMap<Long, CapaianLulusan>();

		rowCpl = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowCpl, "2");
		rowCpl.setParent(rows);

		final EventListener ubahCapaianLulusan = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				ubahBahanKajian.onEvent(arg0);

				Jurusan s = (Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue());

				List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(CapaianLulusan.class)
								.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
								.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", s)))
								.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						CapaianLulusan.class);

				selectedCapaianLulusan.clear();
				for (String d : matakuliah.getCapaianLulusan().split(",")) {
					try {
						if (!d.trim().isEmpty()) {
							Long idP = Long.parseLong(d);
							CapaianLulusan capaianLulusan = (CapaianLulusan) ConstantValues
									.ambil(CapaianLulusan.class.getName(), idP);
							if (capaianLulusan != null && !selectedCapaianLulusan.containsKey(idP)) {
								selectedCapaianLulusan.put(idP, capaianLulusan);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:2231");
						// TODO: handle exception
					}
				}

				Common.clear(rowCpl);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowCpl);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Capaian Lulusan");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final CapaianLulusan capaianLulusan : capaianLulusans) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					final Checkbox checkbox = new Checkbox(capaianLulusan.getKode() + " " + capaianLulusan.getNama());
					checkbox.setParent(rowSkala);
					checkbox.setChecked(selectedCapaianLulusan.containsKey(capaianLulusan.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedCapaianLulusan.put(capaianLulusan.getId(), capaianLulusan);
							} else {
								selectedCapaianLulusan.remove(capaianLulusan.getId());
							}

						}
					});

				}
			}
		};

		rowCpmk = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowCpmk, "2");
		rowCpmk.setParent(rows);

		selectedCapaianPembelajaranLulusan = new HashMap<Long, CapaianPembelajaranLulusan>();

		final EventListener ubahCapaianLulusanPadaMk = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				ubahCapaianLulusan.onEvent(arg0);

				Jurusan s = (Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue());

				List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(CapaianPembelajaranLulusan.class)
								.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
								.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", s)))
								.addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						CapaianPembelajaranLulusan.class);

				selectedCapaianPembelajaranLulusan.clear();
				for (String d : matakuliah.getCapaianPembelajaranLulusan().split(",")) {
					try {
						if (!d.trim().isEmpty()) {
							Long idP = Long.parseLong(d);
							CapaianPembelajaranLulusan capaianPembelajaranLulusan = (CapaianPembelajaranLulusan) ConstantValues
									.ambil(CapaianPembelajaranLulusan.class.getName(), idP);
							if (capaianPembelajaranLulusan != null
									&& !selectedCapaianPembelajaranLulusan.containsKey(idP)) {
								selectedCapaianPembelajaranLulusan.put(idP, capaianPembelajaranLulusan);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MatakuliahAction.java:2315");
						// TODO: handle exception
					}
				}

				Common.clear(rowCpl);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowCpl);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig(
						"Pilih Capaian Pembelajaran Lulusan yang dibebankan pada matakuliah (CPMK)");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final CapaianPembelajaranLulusan capaianPembelajaranLulusan : capaianPembelajaranLulusans) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					final Checkbox checkbox = new Checkbox(capaianPembelajaranLulusan.getNama());
					checkbox.setParent(rowSkala);
					checkbox.setChecked(
							selectedCapaianPembelajaranLulusan.containsKey(capaianPembelajaranLulusan.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedCapaianPembelajaranLulusan.put(capaianPembelajaranLulusan.getId(),
										capaianPembelajaranLulusan);
							} else {
								selectedCapaianPembelajaranLulusan.remove(capaianPembelajaranLulusan.getId());
							}

						}
					});

				}
			}
		};

		ubahCapaianLulusanPadaMkRef = ubahCapaianLulusanPadaMk;

		// Tombol Generate CPMK via AI (pilih otomatis yang cocok + saran baru).
		MyFormRow rowAiCpmk = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowAiCpmk, "2");
		rowAiCpmk.setParent(rows);
		MyToolbarbuttonConfig btnAiCpmk = new MyToolbarbuttonConfig(
				Common.getBahasaConfig("Generate CPMK berdasarkan AI"), "/img/svg/sparkles.svg");
		btnAiCpmk.setStyle("font-size:12px;font-weight:bold;color:#ffffff;background-color:#7c3aed;"
				+ "border-radius:6px;padding:6px 15px;border:none;cursor:pointer;margin:4px 0;");
		btnAiCpmk.setParent(rowAiCpmk);
		btnAiCpmk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				generateCpmkAiMk();
			}
		});

		jurusan.addEventListener("onChange", ubahCapaianLulusanPadaMk);

		ubahCapaianLulusanPadaMk.onEvent(null);

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		row.appendChild(feeder = new Textbox(matakuliah.getFeeder()));
		feeder.setWidth("90%");

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
					// loadKurikulum();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
//		if (singkatan.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Singkatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
		if (status.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Mata Kuliah",
					"Kolom Jenis Mata Kuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Mata Kuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prodi",
					"Kolom Prodi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prodi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jenisMatakuliah.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Keberadaan matakuliah",
					"Kolom Keberadaan matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Keberadaan matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Konfigurasi kodemk = Common.getKonfigurasi("matakuliah_kode_gak_blh_sama", Konfigurasi.TIDAK_AKTIF);

		if (kodemk.getNilai().equals(Konfigurasi.AKTIF)) {
			boolean i = checkKodeSajaMatkul();
			if (i) {
				MyMessageboxConfig.show("Kode matakuliah \"" + kode.getValue() + "\" tidak diperbolehkan sama",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Konfigurasi kodedanJurusan = Common.getKonfigurasi("matakuliah_kode_jurusan_gak_blh_sama", Konfigurasi.AKTIF);

		if (kodedanJurusan.getNilai().equals(Konfigurasi.AKTIF)) {
			boolean i = checkKodeMatkul();
			if (i) {
				MyMessageboxConfig.show(
						"Kode matakuliah \"" + kode.getValue() + "\" di dalam satu prodi tidak diperbolehkan sama",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		MatakuliahDao matakuliahDao = DaoFactory.getInstance().getMatakuliahDao();
		if (matakuliah.getId() != null) {
			matakuliah = matakuliahDao.load(matakuliah.getId());
		}

		matakuliah.setMerupakanPraPerkuliahan(merupakanPraPerkuliahan.isChecked());
		matakuliah.setSksSubMk(sksSubMk.getValue());
		matakuliah.setJumlahMaksimalSksJikaAmbilMkIni(jumlahMaksimalSksJikaAmbilMkIni.getValue() == null ? null
				: jumlahMaksimalSksJikaAmbilMkIni.getValue().intValue());
		matakuliah.setMerupakanModul(merupakanModul.isChecked());
		matakuliah.setFeeder(feeder.getValue().trim());
		matakuliah.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
		matakuliah.setCapaianPembelajaranProdi(capaianPembelajaranProdi.getValue());
		matakuliah.setMetodeKuliah(metodeKuliah.getValue());
		matakuliah.setAdaAcaraPraktek(adaAcaraPraktek.isChecked());
		matakuliah.setAdaBahanAjar(adaBahanAjar.isChecked());
		matakuliah.setAdaDiktat(adaDiktat.isChecked());
		matakuliah.setAdaSap(adaSap.isChecked());
		matakuliah.setAdaSilabus(adaSilabus.isChecked());
		matakuliah.setTanggalMulai(tanggalMulai.getValue());
		matakuliah.setTanggalSampai(tanggalSampai.getValue());

		matakuliah.setTerdapatPraktekLapangan(terdapatPraktekLapangan.isChecked());
		matakuliah.setTerdapatSimulasi(terdapatSimulasi.isChecked());

		matakuliah.setExtraKulikuler(extraKulikuler.isChecked());
		matakuliah.setTerdapatDiskusi(terdapatDiskusi.isChecked());
		matakuliah.setSksDiskusi(sksDiskusi.getValue() == null ? 0 : sksDiskusi.getValue().intValue());
		matakuliah.setSksPraktek(sksPraktek.getValue() == null ? 0 : sksPraktek.getValue().intValue());

		matakuliah.setSksPraktekLapangan(
				sksPraktekLapangan.getValue() == null ? 0 : sksPraktekLapangan.getValue().intValue());

		matakuliah.setSksSimulasi(sksSimulasi.getValue() == null ? 0 : sksSimulasi.getValue().intValue());

		matakuliah.setTerdapatPraktek(terdapatPraktek.isChecked());
		matakuliah.setNamaEn(namaEn.getValue());
		matakuliah.setPrefix((Prefix) (prefix.getSelectedItem() == null ? null : prefix.getSelectedItem().getValue()));
		matakuliah
				.setTingkatKesulitanMatakuliah((TingkatKesulitanMatakuliah) (kesulitan.getSelectedItem() == null ? null
						: kesulitan.getSelectedItem().getValue()));
		matakuliah.setSingkatan(singkatan.getValue());
		matakuliah.setKode(kode.getValue());
		matakuliah.setNama(nama.getValue());
		matakuliah.setSks(sks.getValue() == null ? 0 : sks.getValue().intValue());
		// matakuliah.setSemester(semester.getValue().intValue());
		matakuliah.setStatus(status.getSelectedItem().getValue().toString());
		matakuliah.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		matakuliah.setJenisMatakuliah((String) jenisMatakuliah.getSelectedItem().getValue());

		matakuliah.setKeterangan(keterangan.getValue());
		matakuliah.setMilikUniversitas(milikUniversitas.isChecked());
		matakuliah.setBolehDiambilProdiLain(bolehDiambilProdiLain.isChecked());
		matakuliah.setMerupakanMkPraktek(merupakanMkPraktek.isChecked());
		matakuliah.setMerupakanMkTeori(merupakanMkTeori.isChecked());
		matakuliah.setTerdapatUts(terdapatUts.isChecked());
		matakuliah.setTerdapatUas(terdapatUas.isChecked());
		matakuliah.setMerupakanPerkuliahanUmum(merupakanPerkuliahanUmum.isChecked());

		KelompokMatakuliah kelompokMatakuliah = (KelompokMatakuliah) (this.kelompokMatakuliah.getSelectedItem() == null
				? null
				: this.kelompokMatakuliah.getSelectedItem().getValue());

		matakuliah.setKelompokMatakuliah(kelompokMatakuliah);

		String jenisS = "";
		if (this.selectedCapaianLulusan != null) {
			for (Long kelasLesSiswa : this.selectedCapaianLulusan.keySet()) {
				jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
			}
		}
		matakuliah.setCapaianLulusan(jenisS);

		jenisS = "";
		if (this.selectedBahanKajian != null) {
			for (Long kelasLesSiswa : this.selectedBahanKajian.keySet()) {
				jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
			}
		}
		matakuliah.setBahanKajian(jenisS);

		jenisS = "";
		if (this.selectedCapaianPembelajaranLulusan != null) {
			for (Long kelasLesSiswa : this.selectedCapaianPembelajaranLulusan.keySet()) {
				jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
			}
		}
		matakuliah.setCapaianPembelajaranLulusan(jenisS);

		matakuliah.setJenisNilaiHuruf((JenisNilaiHurufMatakuliah) (jenisNilaiHuruf.getSelectedItem() == null ? null
				: jenisNilaiHuruf.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(matakuliah);

		Session session = HibernateUtil.currentSession();

		if (kelompokMatakuliah != null) {
			KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) session
					.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
					.add(Restrictions.eq("matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
			if (kelompokMatakuliahPunyaMatakuliah == null) {
				kelompokMatakuliahPunyaMatakuliah = new KelompokMatakuliahPunyaMatakuliah();
			}
			kelompokMatakuliahPunyaMatakuliah.setMatakuliah(matakuliah);
			kelompokMatakuliahPunyaMatakuliah.setKelompokMatakuliah(kelompokMatakuliah);
			session.saveOrUpdate(kelompokMatakuliahPunyaMatakuliah);
		}

		return true;
	}

	public Boolean checkKodeMatkul() {

		Integer fakultaskodeCount = null;
		Session session = HibernateUtil.currentSession();
		// Session session = HibernateUtil.currentSession();
		fakultaskodeCount = ((Number) session.createCriteria(Matakuliah.class).setProjection(Projections.rowCount())
				.add(Restrictions
						.sqlRestriction("upper(trim(this_.kode)) = upper(trim('" + kode.getValue().trim() + "'))"))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))
				.add(this.matakuliah.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.matakuliah.getId()))
				.uniqueResult()).intValue();

		return !fakultaskodeCount.equals(0);
	}

	public Boolean checkKodeSajaMatkul() {

		Integer fakultaskodeCount = null;
		Session session = HibernateUtil.currentSession();
		fakultaskodeCount = ((Number) session.createCriteria(Matakuliah.class).setProjection(Projections.rowCount())
				.add(Restrictions
						.sqlRestriction("upper(trim(this_.kode)) = upper(trim('" + kode.getValue().trim() + "'))"))
				.add(this.matakuliah.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.matakuliah.getId()))
				.uniqueResult()).intValue();

		return !fakultaskodeCount.equals(0);
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Matakuliah.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchmikinsendiri.isChecked() ? Restrictions.eq("milikUniversitas", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchextraKulikuler.isChecked() ? Restrictions.eq("extraKulikuler", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchpra.isChecked() ? Restrictions.eq("merupakanPraPerkuliahan", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchumum.isChecked() ? Restrictions.eq("merupakanPerkuliahanUmum", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchmodul.isChecked() ? Restrictions.eq("merupakanModul", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNotNull("feeder"), Restrictions.ne("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.add(searchpraktek.isChecked() ? Restrictions.gt("sksPraktek", 0) : Restrictions.sqlRestriction("1=1"))

				.add(searchprakteklapangan.isChecked() ? Restrictions.gt("sksPraktekLapangan", 0)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchteori.isChecked() ? Restrictions.gt("sksDiskusi", 0) : Restrictions.sqlRestriction("1=1"))

				.add(searchsimulasi.isChecked() ? Restrictions.gt("sksSimulasi", 0)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchpra.isChecked() || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchpra.isChecked() || searchjenjang.getSelectedItem() == null
						|| searchjenjang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", searchjenjang, false));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.isNull("jurusan"),
							Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi)));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Matakuliah> matakuliah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void uploadDataMatakuliah(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					MyMessageboxConfig.show(
							"Upload data matakuliah berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Matakuliah.class);
					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							Matakuliah matakuliah = id == null || id.equals(-1L) ? null
									: (Matakuliah) session.createCriteria(Matakuliah.class).add(Restrictions.idEq(id))
											.uniqueResult();

							if (matakuliah == null) {
								matakuliah = new Matakuliah();
							}

							Common.setObjectValues(classMetadata, matakuliah, contents, 1, sheet, i);

							session.getTransaction().begin();
							session.saveOrUpdate(matakuliah);
							session.getTransaction().commit();

							label.setValue("Upload data \"" + matakuliah.getKode() + " - " + matakuliah.getNama()
									+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MatakuliahAction.java:2767");
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}
}
