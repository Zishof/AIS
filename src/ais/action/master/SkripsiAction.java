package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.NeoFeederProgressHelper;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.AktifitasSkripsiHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataDosenSkripsiBanbox;
import ais.action.master.helper.AmbilDataMahasiswaSkripsiBanbox;
import ais.action.master.helper.AmbilJadwalSidangTugasAkhirBanbox;
import ais.action.master.helper.PenilaianSkripsiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiSkripsiHelper;
import ais.action.master.library.ItemAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.penelitiandanpengabdian.ArtikelAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanRekapitulasiJudisium;
import ais.action.report.format1.akademik.LaporanRekapitulasiSidang;
import ais.action.report.format1.akademik.LaporanSidang;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.Html2Text;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.DataPunyaBukuBahanAjar;
import ais.database.model.DataPunyaItem;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.GelombangPendaftaranSidangTugasAkhir;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalSidangTugasAkhir;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.NilaiToeflToaflMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusDomisiliSetelahLulus;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusPekerjaanSetelahLulus;
import ais.database.model.StatusSetelahLulus;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.MediaParameter;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;

public class SkripsiAction extends GenericAutowireComposer implements DataCriteria, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnim;
	private Textbox searchnama;
	private Textbox searchjudul;
	private Combobox searchsidang;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchTahunAkademik;
	private Combobox searchSemesterAbsensi;
	protected AmbilDataDosenBanbox searchdosenPemimbing;

	private MyCheckboxConfig searchBelumMasukFeeder;
	private MyCheckboxConfig searchMasukFeeder;

	// private Textbox judul;
	private AmbilDataDosenSkripsiBanbox pembimbing;
	private Combobox formatNilaiSkripsi;
	private MyDatebox awalBimbingan;
	private MyDatebox akhirBimbingan;
	private MyCheckboxConfig telahSidang;
	private MyCheckboxConfig lulusToefl;
	private MyCheckboxConfig lulusToafl;

	private Timebox waktuSidang;
	private Timebox waktuSampaiSidang;
	private AmbilDataDosenSkripsiBanbox ketuaSidang;
	private AmbilDataDosenSkripsiBanbox penguji1;
	private AmbilDataDosenSkripsiBanbox penguji2;
	private AmbilDataDosenSkripsiBanbox penguji3;

	private AmbilDataMahasiswaSkripsiBanbox mahasiswa;
	// private MyDoublebox nilaikomprehensif;

	private Mahasiswa dataMahasiswa = null;

	private MyDatebox tanggalLulus;
	private Combobox statusKeluar;
	private Textbox noIjazah1;
	private Textbox noIjazah2;
	private Textbox noAkta1;
	private Textbox noAkta2;
	private Intbox tahunWisuda;
	private MyDatebox tanggalYudisium;
	private Combobox tahunLulus;
	private Combobox semesterLulus;
	/** Holder komponen form "Informasi Kelulusan" hasil FormKelulusanHelper.build (reuse dgn MahasiswaAction). */
	private ais.action.master.helper.FormKelulusanHelper.Komponen kelulusanKomponen;

	private Checkbox persetujuanPembimbing1;
	private Checkbox persetujuanPembimbing2;
	private Checkbox persetujuanPembimbing3;

	private Checkbox persetujuanPenguji1;
	private Checkbox persetujuanPenguji2;
	private Checkbox persetujuanPenguji3;
	private Checkbox persetujuanPenguji4;

	private Textbox abstrack;
	private Textbox keyword;
	private Textbox judulCK;
	private Textbox judulEn;

	private Skripsi skripsi;

	private List<Skripsi> skripsis = new ArrayList<Skripsi>();
	List<Double> nilaiToeflMahasiswas = new ArrayList<Double>();
	List<Double> nilaiToaflMahasiswas = new ArrayList<Double>();

	private LampiranLain lainMahasiswaUploadLampiran1;
	private LampiranLain lainMahasiswaUploadLampiran2;
	private LampiranLain lainMahasiswaUploadLampiran3;
	private LampiranLain lainMahasiswaUploadLampiran4;
	private LampiranLain lainMahasiswaUploadLampiran5;
	private LampiranLain lainMahasiswaUploadLampiran6;
	private LampiranLain lainMahasiswaUploadLampiran7;
	private LampiranLain lainMahasiswaUploadLampiran8;
	private LampiranLain lainMahasiswaUploadLampiran9;
	private LampiranLain lainMahasiswaUploadLampiran10;

	private LampiranLain lainMahasiswaUploadLampiran11;
	private LampiranLain lainMahasiswaUploadLampiran12;
	private LampiranLain lainMahasiswaUploadLampiran13;
	private LampiranLain lainMahasiswaUploadLampiran14;
	private LampiranLain lainMahasiswaUploadLampiran15;

	private LampiranLain lainMahasiswaUploadLampiran16;
	private LampiranLain lainMahasiswaUploadLampiran17;
	private LampiranLain lainMahasiswaUploadLampiran18;
	private LampiranLain lainMahasiswaUploadLampiran19;
	private LampiranLain lainMahasiswaUploadLampiran20;

	private boolean edit;
	private boolean delete;

	private Row rowToefl;
	private Row rowToafl;

	private MyToolbarbuttonConfig add;

	private LampiranLain lainMahasiswa = null;
	private LampiranLain lainMahasiswaCover = null;
	private EventListener eventListener;

	private Row cari1;
	private Row cari1_2;
	private Row cari2;
	private Row cari2_2;
	private MyToolbarbuttonConfig find;

	private Tabpanel manajemenSidang;

	private Tabpanel manajemenGelombangSidang;

	public SkripsiAction() {
		super();
	}

	public SkripsiAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

	public void onGelombangJadwal(Event event) {
		if (manajemenGelombangSidang.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenGelombangSidang);
			MyInclude iframe = new MyInclude("/pages/master/gelombang_pendaftaran_sidang_tugas_akhir.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel formatNilaiTab;

	public void onFormatNilai(Event event) {
		if (formatNilaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(formatNilaiTab);
			MyInclude iframe = new MyInclude("/pages/master/format_nilai_skripsi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel komponenPenilaianTab;

	public void onKomponenPenilaian(Event event) {
		if (komponenPenilaianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(komponenPenilaianTab);
			MyInclude iframe = new MyInclude("/pages/master/komponen_penilaian_skripsi.zul");
			iframe.setParent(window);
		}
	}

	public void onJadwal(Event event) {
		if (manajemenSidang.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenSidang);
			MyInclude iframe = new MyInclude("/pages/master/jadwal_sidang_tugas_akhir.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanSidang;

	public void onTampilSidang(Event event) {
		if (laporanSidang.getChildren().size() == 0) {
			LaporanRekapitulasiSidang laporanRekapitulasiSidang = new LaporanRekapitulasiSidang();
			laporanRekapitulasiSidang.setHeight("100%");
			laporanRekapitulasiSidang.setWidth("100%");
			laporanRekapitulasiSidang.setParent(laporanSidang);
		}
	}

	private Tabpanel sidang;

	public void onSidang(Event event) {
		if (sidang.getChildren().size() == 0) {
			LaporanSidang laporanRekapitulasiSidang = new LaporanSidang();
			laporanRekapitulasiSidang.setHeight("100%");
			laporanRekapitulasiSidang.setWidth("100%");
			laporanRekapitulasiSidang.setParent(sidang);
		}

	}

	private Tabpanel laporanJudisium;

	public void onTampilJudisium(Event event) {
		if (laporanJudisium.getChildren().size() == 0) {
			LaporanRekapitulasiJudisium laporanRekapitulasiJudisium = new LaporanRekapitulasiJudisium();
			laporanRekapitulasiJudisium.setHeight("100%");
			laporanRekapitulasiJudisium.setWidth("100%");
			laporanRekapitulasiJudisium.setParent(laporanJudisium);
		}
	}

	public static String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.jurusan.nama",
			"judul", "judulen", "abstrack", "keyword", "formatNilaiSkripsi", "pembimbing", "ketuaSidang", "pembimbing3",
			"penguji1", "penguji2", "penguji3", "penguji4", "penguji5", "nilaiPembimbing", "nilaiKetuaSidang",
			"nilaiPenguji1", "nilaiPenguji2", "nilaiPenguji3", "nilaiPenguji4", "totalNilai", "nilaiHuruf", "totalIP",
			"tanggalSidang", "tanggalSeminar", "telahSidang", "ruangSidang", "waktuSidang", "waktuSampaiSidang",
			"semester", "tahunAkademik", "detailperkuliahan", "formatNilai", "gelombangPendaftaranSidangTugasAkhir",
			"lokasiUjian", "nomorSk", "tglSk", "selesaiDalamBulan", "tahun" };

	private Tbmuser tbmuser;
	private AmbilJadwalSidangTugasAkhirBanbox jadwalSidangTugasAkhir;
	private MyDatebox tanggalSidang;

	private static class DataAddingHelper {

		private XSSFCellStyle hlink_style;

		public DataAddingHelper(XSSFWorkbook workbook) {

			XSSFFont hlink_font = workbook.createFont();
			hlink_font.setUnderline(XSSFFont.U_SINGLE);
			hlink_font.setColor(new XSSFColor(Color.BLUE));

			hlink_style = workbook.createCellStyle();
			hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
			hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
			hlink_style.setFont(hlink_font);
		}

		public void process(XSSFRow row, int index, Skripsi skripsi, String jenis) throws Exception {

			try {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), jenis);

				XSSFCell cell = row.createCell(index);

				if (lam != null) {

					String nama = lam.getNama();

					cell.setCellStyle(hlink_style);
					cell.setCellValue(nama);
					String url = lam.createLinkUri(false);
					XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
							.createHyperlink(Hyperlink.LINK_URL);
					link.setAddress(url);
					cell.setHyperlink(link);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SkripsiAction.java:440");
				// TODO: handle exception
			}

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

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			if (cari1 != null) {
				cari1.setVisible(false);
				if (cari1_2 != null) cari1_2.setVisible(cari1.isVisible());
			}
			if (cari2 != null) {
				cari2.setVisible(false);
				if (cari2_2 != null) cari2_2.setVisible(cari2.isVisible());
			}
			if (find != null) {
				find.setVisible(false);
			}
			if (manajemenGelombangSidang != null) {
				manajemenGelombangSidang.getLinkedTab().setVisible(false);
				manajemenGelombangSidang.setVisible(false);
			}
			if (manajemenSidang != null) {
				manajemenSidang.getLinkedTab().setVisible(false);
				manajemenSidang.setVisible(false);
			}
		}

		if (tbmuser != null && (tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null)) {
			if (formatNilaiTab != null) {
				formatNilaiTab.setVisible(false);
				formatNilaiTab.getLinkedTab().setVisible(false);
			}
			if (komponenPenilaianTab != null) {
				komponenPenilaianTab.setVisible(false);
				komponenPenilaianTab.getLinkedTab().setVisible(false);
			}
		}

		Common.generateTahunAjaranDanSemua(searchTahunAkademik);
		if (searchTahunAkademik != null) { searchTahunAkademik.setWidth("90%"); }
		if (searchTahunAkademik != null) { searchTahunAkademik.setReadonly(true); }
		Common.selectComboItem(searchTahunAkademik, null);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchSemesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(searchSemesterAbsensi, null);
		if (searchSemesterAbsensi != null) { searchSemesterAbsensi.setReadonly(true); }

		comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Sudah sidang"); }
		if (comboitem != null) { comboitem.setValue(1); }
		searchsidang.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Belum sidang"); }
		if (comboitem != null) { comboitem.setValue(0); }
		searchsidang.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchsidang.appendChild(comboitem);
		if (searchsidang != null) { searchsidang.setReadonly(true); }

		if (searchsidang != null) { searchsidang.setSelectedItem(comboitem); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Skripsi skripsi = (Skripsi) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				DataAddingHelper dataAddingHelper = new DataAddingHelper(workbook);
				dataAddingHelper.process(row, contents.length + 0, skripsi, LampiranLain.SKRIPSI);
				dataAddingHelper.process(row, contents.length + 1, skripsi, LampiranLain.COVER_SKRIPSI);
				dataAddingHelper.process(row, contents.length + 2, skripsi, "uploadLampiran1");
				dataAddingHelper.process(row, contents.length + 3, skripsi, "uploadLampiran2");
				dataAddingHelper.process(row, contents.length + 4, skripsi, "uploadLampiran3");
				dataAddingHelper.process(row, contents.length + 5, skripsi, "uploadLampiran4");
				dataAddingHelper.process(row, contents.length + 6, skripsi, "uploadLampiran5");
				dataAddingHelper.process(row, contents.length + 7, skripsi, "uploadLampiran6");
				dataAddingHelper.process(row, contents.length + 8, skripsi, "uploadLampiran7");
				dataAddingHelper.process(row, contents.length + 9, skripsi, "uploadLampiran8");
				dataAddingHelper.process(row, contents.length + 10, skripsi, "uploadLampiran9");
				dataAddingHelper.process(row, contents.length + 11, skripsi, "uploadLampiran10");

				dataAddingHelper.process(row, contents.length + 12, skripsi, "uploadLampiran11");
				dataAddingHelper.process(row, contents.length + 13, skripsi, "uploadLampiran12");
				dataAddingHelper.process(row, contents.length + 14, skripsi, "uploadLampiran13");
				dataAddingHelper.process(row, contents.length + 15, skripsi, "uploadLampiran14");
				dataAddingHelper.process(row, contents.length + 16, skripsi, "uploadLampiran15");
			}
		};

		String label_skripsi = Common.getKonfigurasi("label_skripsi", "Skripsi").getNilai();
		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add(label_skripsi);
		columnHeadersAdding.add("Cover " + label_skripsi);
		columnHeadersAdding.add("lampiran 1");
		columnHeadersAdding.add("lampiran 2");
		columnHeadersAdding.add("lampiran 3");
		columnHeadersAdding.add("lampiran 4");
		columnHeadersAdding.add("lampiran 5");
		columnHeadersAdding.add("lampiran 6");
		columnHeadersAdding.add("lampiran 7");
		columnHeadersAdding.add("lampiran 8");
		columnHeadersAdding.add("lampiran 9");
		columnHeadersAdding.add("lampiran 10");
		columnHeadersAdding.add("lampiran 11");
		columnHeadersAdding.add("lampiran 12");
		columnHeadersAdding.add("lampiran 13");
		columnHeadersAdding.add("lampiran 14");
		columnHeadersAdding.add("lampiran 15");

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Skripsi.class, this, "Download Data",
				"/img/print.png", columnHeadersAdding, dataAdding, false, null, null, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
		if (add != null) { add.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null); }

		if (tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null) {
			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = media.getStreamData();
						// System.out.println("media = " + media);
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						// System.out.println("file = " +
						// file.getAbsolutePath());
						file.getParentFile().mkdirs();
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
						fileOutputStream.close();
						inputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final Label peringatan = new Label("");
								final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Skripsi");
								final Label downloadPath = new Label("");

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
											if (!downloadPath.getValue().isEmpty()) {
												try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
												catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
											}
											MyMessageboxConfig.show(
													"Upload data berhasil dilakukan." + report.getRingkasan()
															+ (peringatan.getValue().isEmpty() ? ""
																	: "\n" + peringatan.getValue()),
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION, new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});
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

											ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Skripsi.class);
											Session session1 = HibernateUtil.currentNativeSession();

											FormatNilaiSkripsi formatNilaiSkripsi = (FormatNilaiSkripsi) session1
													.createCriteria(FormatNilaiSkripsi.class).setMaxResults(1)
													.uniqueResult();
											HibernateUtil.closeSession();

											int rowCount = (sheet.getLastRowNum() + 1);
											for (int i = 1; i < rowCount; i++) {
												try {

													Mahasiswa mahasiswa = (Mahasiswa) Common
															.getSheetContentAsObject(sheet, 1, i, Mahasiswa.class);
													if (mahasiswa == null) {
														continue;
													}

													Long id = Common.getSheetContentAsLong(sheet, 0, i);
													NilaiHuruf nilaiHuruf = null;
													Skripsi skripsi = null;
													Session session = HibernateUtil.currentNativeSession();
													try {
														skripsi = id == null || id.equals(-1L) ? null
																: (Skripsi) session.createCriteria(Skripsi.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

														if (skripsi == null && mahasiswa != null) {
															skripsi = (Skripsi) session.createCriteria(Skripsi.class)
																	.add(Restrictions.eq("mahasiswa", mahasiswa))
																	.setMaxResults(1).uniqueResult();
														}

														if (skripsi == null) {
															skripsi = new Skripsi();
														}

														Common.setObjectValues(classMetadata, skripsi, contents, 1,
																sheet, i);
														skripsi.setMahasiswa(mahasiswa);

														if (skripsi.getFormatNilaiSkripsi() == null
																&& formatNilaiSkripsi == null) {
															skripsi.setFormatNilaiSkripsi(formatNilaiSkripsi);
														}

														Detailperkuliahan detailperkuliahan = skripsi
																.getDetailperkuliahan();
														Matakuliah matakuliah = detailperkuliahan == null ? null
																: detailperkuliahan.getPerkuliahan() != null
																		? detailperkuliahan.getPerkuliahan()
																				.getMatakuliah()
																		: detailperkuliahan.getMatakuliahKonversi();

														nilaiHuruf = skripsi.getDetailperkuliahan() == null
																? Common.getNilaiHuruf(skripsi.getTotalNilai(),
																		mahasiswa.getTahunangkatan(),
																		mahasiswa.getJurusan(),
																		mahasiswa.getJurusan().getFakultas(),
																		Common.getCurrentTahunAkademik(),
																		Common.isNowSemensterGanjil()
																				? Perkuliahan.GANJIL
																				: Perkuliahan.GENAP,
																		matakuliah == null ? "" : matakuliah.getKode(),
																		matakuliah == null ? null
																				: matakuliah.getJenisNilaiHuruf())
																: Common.getNilaiHuruf(skripsi.getTotalNilai(),
																		mahasiswa.getTahunangkatan(),
																		mahasiswa.getJurusan(),
																		mahasiswa.getJurusan().getFakultas(),
																		skripsi.getDetailperkuliahan()
																				.getTahunAkademik(),
																		skripsi.getDetailperkuliahan().getSemester()
																				% 2 == 0 ? Perkuliahan.GENAP
																						: Perkuliahan.GANJIL,
																		matakuliah == null ? "" : matakuliah.getKode(),
																		matakuliah == null ? null
																				: matakuliah.getJenisNilaiHuruf());

														if (nilaiHuruf != null) {
															skripsi.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
														}
														session.getTransaction().begin();
														session.saveOrUpdate(skripsi);
														session.getTransaction().commit();

														report.sukses(i, mahasiswa.getNim() + "/" + (skripsi.getJudul() != null ? skripsi.getJudul() : "-"), "");
														label.setValue("Upload data \"" + skripsi.getMahasiswa() + " - "
																+ skripsi.getTotalNilai() + "\" "
																+ (nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf())
																+ " ("
																+ Common.numberFormat.get().format(i * 100.0 / rowCount)
																+ " %)");

													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														report.gagal(i, mahasiswa.getNim() + "/" + (skripsi != null && skripsi.getJudul() != null ? skripsi.getJudul() : "-"), e, "Periksa data skripsi baris " + i);
													}

													HibernateUtil.closeSession();

													if (skripsi != null) {
														insertNilai(skripsi.getDetailperkuliahan(), mahasiswa,
																nilaiHuruf, skripsi.getTotalNilai());
														LibraryUtil.checkSkripsiForItem(skripsi, true, tbmuser);
													}

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													report.gagal(i, "row " + i, e, "Periksa data skripsi baris " + i);
												}

											}
										} catch (Exception e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SkripsiAction.java:794");
										}

										try {
											java.io.File rptFile = report.simpanLaporan();
											downloadPath.setValue(rptFile.getAbsolutePath());
										} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SkripsiAction laporan"); }
										label.setValue("");
																			} finally {
											ais.database.hibernate.HibernateUtil.closeSession();
										}
									}
								}).start();

							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});
			Common.appendKeToolbar(upload, add, comp);

			String[] contentsJudisium = new String[] { "smt", "mahasiswa.nim", "mahasiswa.nama",
					"mahasiswa.tempatlahir", "mahasiswa.tanggallahir", "judul", "pembimbing.nama", "ketuaSidang.nama",
					"pembimbing3.nama", "penguji1.nama", "penguji2.nama", "penguji3.nama", "penguji4.nama",
					"penguji5.nama", "totalNilai", "nilaiHuruf", "tanggalSidang", "tanggalSeminar",
					"mahasiswa.predikatKelulusan.nama", "mahasiswa.tanggalYudisium", "semester" };
			List<String> columnHeadersAddingBaru = MahasiswaAction.getColumnAdding(false);
			MahasiswaAction.DataAddingMahasiswa dataAddingBaru = new MahasiswaAction.DataAddingMahasiswa(false,
					contentsJudisium);

			MyToolbarbuttonConfig cetakToolbarbuttonJudusium = Common.cetakDataCustomButton(Skripsi.class, this,
					"Download Judisium", "/img/print.png", columnHeadersAddingBaru, dataAddingBaru, false, null, null,
					contentsJudisium);
			Common.appendKeToolbar(cetakToolbarbuttonJudusium, add, comp);
			add.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);

			final MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
			exportKeOjs.setStyle("font-size:9px;");
			Common.appendKeToolbar(exportKeOjs, add, comp);
			exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("ta_skripsi_mahasiswa_terhubung_ke_dspace"));

			exportKeOjs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});

					new Thread(new Runnable() {

						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							try {
								String cookie = DspaceCommon.login();
								List<Skripsi> skripsis = initCriteria(true).list();

								int rowIndex = 1;
								for (Skripsi skripsi : skripsis) {
									label.setValue("Sedang memproses data " + skripsi.toString() + " ("
											+ Common.numberFormat.get().format((rowIndex++) * 100.0 / skripsis.size())
											+ " %)");
									SkripsiAction.getDspace(cookie, skripsi, true);
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}
							label.setValue("");
						}
					}).start();
				}
			});

			MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
			Common.appendKeToolbar(batalExport, add, comp);
			batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("ta_skripsi_mahasiswa_terhubung_ke_dspace"));
			batalExport.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label label = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
												LogLoginAction.tampilDpsaceLog();
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {
												try {
													String cookie = DspaceCommon.login();
													List<Skripsi> skripsis = initCriteria(true).list();

													int rowIndex = 1;
													for (Skripsi skripsi : skripsis) {
														label.setValue(
																"Sedang memproses data " + skripsi.toString() + " ("
																		+ Common.numberFormat.get().format(
																				(rowIndex++) * 100.0 / skripsis.size())
																		+ " %)");
														DspaceInformation dspaceInformation = DspaceInformation
																.getDspaceInformation(Skripsi.class.getName(),
																		skripsi.getId());
														if (dspaceInformation != null) {
															int i = DspaceInformation.delete(cookie,
																	"items/" + dspaceInformation.getUuid(),
																	dspaceInformation.getPostInfo());
															if (i == 200) {

																Session session = HibernateUtil.currentNativeSession();
																session.getTransaction().begin();
																session.delete(dspaceInformation);
																session.getTransaction().commit();
																HibernateUtil.closeSession();
															}
														}
													}
												} catch (Exception e) {
													// TODO Auto-generated catch
													// block
													Common.tampilErrorJikaAdmin(e);
												}
												label.setValue("");
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

			final MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan dg status mhs",
					"/img/excel.png");
			Common.appendKeToolbar(singkron, add, comp);

			singkron.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final MyWindow window = new MyWindow("Pilih Fakultas dan Prodi", "none", true);
					window.setParent(page.getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");

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
					column.setWidth("30%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
					final Combobox fakultas;
					row.appendChild(fakultas = new Combobox());
					fakultas.setWidth("90%");
					fakultas.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
					final Combobox jurusan;
					row.appendChild(jurusan = new Combobox());
					jurusan.setWidth("90%");
					jurusan.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
					final Combobox tahunAkademik;
					row.appendChild(tahunAkademik = new Combobox());
					Common.generateTahunAjaranDanSemua(tahunAkademik);
					tahunAkademik.setWidth("90%");

					Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan data mahasiswa",
							"/img/save.gif");
					save.setTooltiptext("Proses");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Skripsi dengan Status Mahasiswa");

							final Label label = Common.displayLoadBar(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									laporan.selesaikan(new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											onSearchDefault(null);
											window.detach();
										}
									});
								}
							});

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {
									Fakultas f = (Fakultas) (fakultas.getSelectedItem() == null ? null
											: fakultas.getSelectedItem().getValue());
									Jurusan j = (Jurusan) (jurusan.getSelectedItem() == null ? null
											: jurusan.getSelectedItem().getValue());
									Session session = HibernateUtil.currentNativeSession();
									List<Skripsi> ids = session.createCriteria(Skripsi.class)
											.add(Restrictions.eq("tahunAkademik",
													tahunAkademik.getSelectedItem().getValue()))
											.createAlias("mahasiswa", "mahasiswa")
											.add(j == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("mahasiswa.jurusan", j))
											.createAlias("mahasiswa.jurusan", "jurusan")
											.add(f == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan.fakultas", f))
											.addOrder(Order.asc("id")).list();
									StatusKeluar LULUS = (StatusKeluar) session.createCriteria(StatusKeluar.class)
											.add(Restrictions.ilike("nama", "Lulus", MatchMode.ANYWHERE))
											.setMaxResults(1).uniqueResult();
									HibernateUtil.closeSession();
									int i = 1;
									for (Skripsi skripsi : ids) {
										label.setValue("Singkronkan data " + skripsi + " ("
												+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
										String kunciSkripsi = String.valueOf(skripsi);
										try {

										Mahasiswa mahasiswa = skripsi.getMahasiswa();

										if (skripsi.getDetailperkuliahan() != null) {
											Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
											detailperkuliahan.setTotalNilai(skripsi.getTotalNilai());
											detailperkuliahan.setTotalIP(skripsi.getTotalIP());
											detailperkuliahan.setNilaiHuruf(skripsi.getNilaiHuruf());
											detailperkuliahan.setLulus(skripsi.getLulus());

											Matakuliah matakuliah = detailperkuliahan == null ? null
													: detailperkuliahan.getPerkuliahan() != null
															? detailperkuliahan.getPerkuliahan().getMatakuliah()
															: detailperkuliahan.getMatakuliahKonversi();

											Double totalSementara = skripsi.getTotalNilai();
											NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalSementara,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

											detailperkuliahan.setTotalNilaiSementara(totalSementara);
											detailperkuliahan.setNilaiHurufSementara(
													nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setTotalIPSementara(
													nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

											session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshUpdate(session, detailperkuliahan);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
										}

										if (mahasiswa != null) {
											if (skripsi.getLulus()) {
												mahasiswa.setStatusKeluar(LULUS);
												try {
													mahasiswa.setTahunLulus(
															Integer.parseInt(skripsi.getTahunAkademik().split("/")[0]));
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SkripsiAction.java:1127");
													// TODO: handle exception
												}

											}

											Judisium judisium = Common.hitungJudisium(mahasiswa, null);
											if (judisium != null && judisium.getId() != null) {
												mahasiswa.setPredikatKelulusan(judisium);
											}
											mahasiswa.setJudulSkripsi(skripsi.getJudul());
											if (skripsi.getTanggalSidang() != null)
												mahasiswa.setTanggalLulus(skripsi.getTanggalSidang());
											if (mahasiswa.getTanggalYudisium() == null) {
												mahasiswa.setTanggalYudisium(skripsi.getTanggalSidang());
											}
											if (skripsi.getAwalBimbingan() != null)
												mahasiswa.setBlnAwalBimbingan(skripsi.getAwalBimbingan());
											if (skripsi.getAkhirBimbingan() != null)
												mahasiswa.setBlnAkhirBimbingan(skripsi.getAkhirBimbingan());
											if (mahasiswa.getTahunWisuda() == null
													|| mahasiswa.getTahunWisuda().equals(0)) {
												mahasiswa.setTahunWisuda(mahasiswa.getTahunLulus());
											}
											session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshUpdate(session, mahasiswa);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
										}

										laporan.catatBerhasil(i - 1, kunciSkripsi, "Sinkronisasi berhasil");
										} catch (Exception ePerItem) {
											Common.tampilErrorJikaAdmin(ePerItem);
											laporan.catatGagalDetail(i - 1, kunciSkripsi, ePerItem);
										}
										i++;
									}
																	} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-skripsi): "
												+ ais.common.LaporanUpload.detailTeknisException(e));
									} finally {
										label.setValue("");
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});

			final MyToolbarbuttonConfig singkronKrs = new MyToolbarbuttonConfig("Singkronkan dg KRS", "/img/excel.png");
			Common.appendKeToolbar(singkronKrs, add, comp);

			singkronKrs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final MyWindow window = new MyWindow("Pilih Fakultas dan Prodi", "none", true);
					window.setParent(page.getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");

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
					column.setWidth("30%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
					final Combobox fakultas;
					row.appendChild(fakultas = new Combobox());
					fakultas.setWidth("90%");
					fakultas.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
					final Combobox jurusan;
					row.appendChild(jurusan = new Combobox());
					jurusan.setWidth("90%");
					jurusan.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
					final Combobox tahunAkademik;
					row.appendChild(tahunAkademik = new Combobox());
					Common.generateTahunAjaranDanSemua(tahunAkademik);
					tahunAkademik.setWidth("90%");

					Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan KRS mahasiswa",
							"/img/save.gif");
					save.setTooltiptext("Proses");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Skripsi dengan KRS Mahasiswa");

							final Label label = Common.displayLoadBar(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									laporan.selesaikan(new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											onSearchDefault(null);
											window.detach();
										}
									});
								}
							});

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {
									Fakultas f = (Fakultas) (fakultas.getSelectedItem() == null ? null
											: fakultas.getSelectedItem().getValue());
									Jurusan j = (Jurusan) (jurusan.getSelectedItem() == null ? null
											: jurusan.getSelectedItem().getValue());
									Session session = HibernateUtil.currentNativeSession();
									List<Skripsi> ids = session.createCriteria(Skripsi.class)
											.add(Restrictions.eq("tahunAkademik",
													tahunAkademik.getSelectedItem().getValue()))
											.createAlias("mahasiswa", "mahasiswa")
											.add(j == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan", j))
											.createAlias("mahasiswa.jurusan", "jurusan")
											.add(f == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan.fakultas", f))
											.addOrder(Order.asc("id")).list();

									HibernateUtil.closeSession();
									int i = 1;
									for (Skripsi skripsi : ids) {
										label.setValue("Singkronkan data " + skripsi + " ("
												+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
										String kunciSkripsi = String.valueOf(skripsi);
										try {
										Mahasiswa mahasiswa = skripsi.getMahasiswa();
										Detailperkuliahan detailperkuliahan = Common
												.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa,
														skripsi.getSemester(),
														skripsi.getFormatNilaiSkripsi().getKodeMatakuliah().trim());

										detailperkuliahan.setTotalNilai(skripsi.getTotalNilai());
										detailperkuliahan.setTotalIP(skripsi.getTotalIP());
										detailperkuliahan.setNilaiHuruf(skripsi.getNilaiHuruf());
										detailperkuliahan.setLulus(skripsi.getLulus());

										Matakuliah matakuliah = detailperkuliahan == null ? null
												: detailperkuliahan.getPerkuliahan() != null
														? detailperkuliahan.getPerkuliahan().getMatakuliah()
														: detailperkuliahan.getMatakuliahKonversi();

										Double totalSementara = skripsi.getTotalNilai();
										NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalSementara,
												detailperkuliahan.getMahasiswa().getTahunangkatan(),
												detailperkuliahan.getMahasiswa().getJurusan(),
												detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
												detailperkuliahan.getTahunAkademik(),
												detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
														: Perkuliahan.GANJIL,
												matakuliah == null ? "" : matakuliah.getKode(),
												matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

										detailperkuliahan.setTotalNilaiSementara(totalSementara);
										detailperkuliahan.setNilaiHurufSementara(
												nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
										detailperkuliahan.setTotalIPSementara(
												nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

										skripsi.setDetailperkuliahan(detailperkuliahan);

										session = HibernateUtil.currentNativeSession();
										session.getTransaction().begin();
										Common.refreshUpdate(session, skripsi);
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
										HibernateUtil.closeSession();

										if (mahasiswa != null) {

											Judisium judisium = Common.hitungJudisium(mahasiswa, null);
											if (judisium != null && judisium.getId() != null) {
												mahasiswa.setPredikatKelulusan(judisium);
											}
											mahasiswa.setJudulSkripsi(skripsi.getJudul());
											if (skripsi.getTanggalSidang() != null)
												mahasiswa.setTanggalLulus(skripsi.getTanggalSidang());
											if (mahasiswa.getTanggalYudisium() == null) {
												mahasiswa.setTanggalYudisium(skripsi.getTanggalSidang());
											}
											if (skripsi.getAwalBimbingan() != null)
												mahasiswa.setBlnAwalBimbingan(skripsi.getAwalBimbingan());
											if (skripsi.getAkhirBimbingan() != null)
												mahasiswa.setBlnAkhirBimbingan(skripsi.getAkhirBimbingan());
											if (mahasiswa.getTahunWisuda() == null
													|| mahasiswa.getTahunWisuda().equals(0)) {
												mahasiswa.setTahunWisuda(mahasiswa.getTahunLulus());
											}
											session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshUpdate(session, mahasiswa);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
										}

										laporan.catatBerhasil(i - 1, kunciSkripsi, "Sinkronisasi berhasil");
										} catch (Exception ePerItem) {
											Common.tampilErrorJikaAdmin(ePerItem);
											laporan.catatGagalDetail(i - 1, kunciSkripsi, ePerItem);
										}
										i++;
									}
																	} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-skripsi): "
												+ ais.common.LaporanUpload.detailTeknisException(e));
									} finally {
										label.setValue("");
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});

			cetakToolbarbutton = Common.cetakDataCustomButton(Skripsi.class, this, "Data Pembimbing", "/img/print.png",
					new String[] { "mahasiswa.nim", "mahasiswa.nama", "judul", "pembimbing.nama", "ketuaSidang.nama" });
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
			downloadLampiran.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onDownloadLampiran(arg0);
				}
			});
			Common.appendKeToolbar(downloadLampiran, add, comp);

			final MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan Pustaka",
					"/img/svg/check2.svg");
			Common.appendKeToolbar(cetakSksDosen, add, comp);

			cetakSksDosen.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Skripsi dengan Pustaka");
							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi perpustakaan"));

							new Thread(new Runnable() {

								@Override
								public void run() {
									List<Skripsi> skripsis = initCriteria(true).list();
									int i = 0;
									int size = skripsis.size();
									for (Skripsi skripsi : skripsis) {
										String kunciSkripsi = String.valueOf(skripsi);
										try {
											if (label != null) {
												label.setValue(
														"(" + (Common.numberFormat.get().format(i * 100.0 / size))
																+ " %) sinkronisasi data pustaka " + skripsi + " ..");
											}
											LibraryUtil.checkSkripsiForItem(skripsi, true, tbmuser);
											laporan.catatBerhasil(i, kunciSkripsi, "Sinkronisasi pustaka berhasil");
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											laporan.catatGagalDetail(i, kunciSkripsi, e);
										}
										i++;
									}
									label.setValue("");
								}
							}).start();

							final Timer timer = new Timer(500);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									// System.out.println("process = " +
									// label.getValue());
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										Clients.clearBusy();
										timer.detach();
										laporan.selesaikan(null);
									}

								}
							});
							timer.start();

						}
					});
				}
			});

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
											final Label myLabelProsesDetail = NeoFeederProgressHelper
													.show("Sinkronisasi Neo Feeder", new EventListener() {

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

																MyMessageboxConfig.show(
																		"Error Terjadi, catatan error akan otomatis ter-download",
																		"Error Terjadi", MyMessageboxConfig.OK,
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
															// FIX IllegalStateException "Components can be accessed only in
															// event listeners": .setValue() langsung dari thread latar tidak
															// aman (lihat NeoFeederProgressHelper.updateProgres).
															NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																	"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);

														List<Skripsi> tbmusers = ConstantValues.simpleList(
																initCriteria(true)
																		.add(Restrictions.eq("setujuiSidang", true)),
																Skripsi.class);
														int size = tbmusers.size();
														int index = 1;
														for (Skripsi skripsi : tbmusers) {
															if (skripsi.getMahasiswa() != null
																	&& skripsi.getMahasiswa().getIdRegPd() != null) {
																NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																		"Memproses " + skripsi.getMahasiswa().getNim()
																				+ " " + skripsi.getMahasiswa().getNama()
																				+ " (" + Common.numberFormat.get()
																						.format((index * 100.0) / size)
																				+ "%");
																index++;
																// FIX: satu Skripsi gagal (exception tak terduga, mis.
																// jaringan/JSON) TIDAK BOLEH menghentikan seluruh batch
																// dan TIDAK BOLEH hilang begitu saja -- dicatat ke
																// errorLog agar terlihat pengguna di akhir proses.
																try {
																	feederImporter.aktivitasMahasiswa(skripsi, errorLog);
																	kirimNilaiSkripsiKeFeeder(feederImporter, skripsi,
																			errorLog);
																} catch (Exception exSatu) {
																	ais.common.Common.tampilErrorJikaAdmin(exSatu);
																	errorLog.add("[" + skripsi.getMahasiswa().getNim() + " "
																			+ skripsi.getMahasiswa().getNama()
																			+ "] Gagal mengirim data Skripsi ke Neo Feeder: "
																			+ exSatu.getMessage());
																}
															} else {
																errorLog.add("Mahasiswa " + skripsi.getMahasiswa()
																		+ " belum terdaftar");
															}
														}
														tbmusers.clear();
														tbmusers = null;
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Error: "
																+ PesanFormalHelper.pesanGagalException(
																		"pengiriman data Skripsi ke Neo Feeder", null, e,
																		new String[] {
																				"Periksa kembali koneksi ke server Neo Feeder dan coba ulangi proses pengiriman.",
																				"Pastikan data Mahasiswa, Program Studi, dan Dosen Pembimbing/Penguji yang terkait sudah tersinkron ke Feeder.",
																				"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																		.replace("\n", " "));
													}
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

						String[] kon = EksporFromFeederAction.koneksi();
						final String ip = kon[0];
						final String port = kon[1];
						final String username = kon[2];
						final String password = kon[3];
						final String url = kon[4];

						if (!EksporFromFeederAction.exists(url)) {

							MyMessageboxConfig.show(ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}

						final Label myLabelProsesDetail = NeoFeederProgressHelper.show("Sinkronisasi Neo Feeder", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (arg0 != null && !arg0.getName().isEmpty()) {
									EksporFromFeederAction.display();
									MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK,
											MyMessageboxConfig.EXCLAMATION);
								}
								onSearchDefault(null);
							}
						});

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
									FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port),
											null);

									String token = feederConnector.getToken(username, password);
									System.out.println("TOKEN => " + token);

									if (token == null || token.trim().isEmpty()
											|| token.trim().toLowerCase().startsWith("error")) {
										// FIX IllegalStateException "Components can be accessed only in
										// event listeners": .setValue() langsung dari thread latar tidak
										// aman (lihat NeoFeederProgressHelper.updateProgres).
										NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
												"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
										return;
									}

									String idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Laporan akhir studi");
									importAktifitasDariFeeder(feederConnector, token, idjenis);

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
							}
						}).start();

					}
				});
				Common.appendKeToolbar(buttonTagihan, add, comp);
			}

		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download Ref.", "/img/save.gif");
		if (save != null) { save.setTooltiptext("Download"); }
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/ref_"
								+ URLEncoder.encode(
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
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
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

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(16);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
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

						@SuppressWarnings("unchecked")
						@Override
						public void run() {

							try {

								List<Object[]> data = initCriteria(true)
										.setProjection(
												Projections.projectionList().add(Projections.property("mahasiswa.id"))
														.add(Projections.property("judul"))
														.add(Projections.property("referensi")))
										.setMaxResults(1048576).list();
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);
								String[] columns = new String[] { "id", "mahasiswa", "referensi", "nama mahasiswa",
										"judul" };
								for (int i = 0; i < columns.length; i++) {
									rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
								}

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

								for (Object[] o : data) {
									try {

										if (o == null) {
											continue;
										}
										label.setValue("Sedang memproses data " + o.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
												+ " %)");

										Number mhs = (Number) o[0];

										Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
												.ambil(Mahasiswa.class.getName(), mhs.longValue());
										if (mahasiswa != null) {
											try {
												JSONArray jsonArray = new JSONArray(o[2]);

												for (int ii = 0; ii < jsonArray.length(); ii++) {
													JSONObject oa = jsonArray.getJSONObject(ii);
													Long refO = ais.common.CommonJSONUtil.ambilLong(oa, "ref");
													String bibl = oa.getString("bibl");

													rowIndex++;
													XSSFRow row = sheet.createRow(rowIndex);
													row.createCell(0).setCellValue(refO);
													row.createCell(1).setCellValue(mahasiswa.getNim());
													row.createCell(2).setCellValue(bibl);
													row.createCell(3).setCellValue(mahasiswa.getNama());
													row.createCell(4).setCellValue(o[1] + "");
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
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

								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}
							HibernateUtil.closeSession();
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
		if (save != null) { save.setParent(add.getParent()); }

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Ref." + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setVisible(tbmuser != null
				&& (tbmuser.ambilDosen() == null
						|| Common.bolehKonfigurasi("tampilkan_tombol_upload_ref_di_dosen"))
				&& tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_upload_ref"));
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }

		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " +
					// file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadRef(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiSkripsiHelper revisiHelper = new RevisiSkripsiHelper(new EventListener() {

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

	public static void uploadRef(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Ref Skripsi");
		final Label downloadPath = new Label("");

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
					if (!downloadPath.getValue().isEmpty()) {
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
						catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
					}
					MyMessageboxConfig.show(
							"Upload data ref dilakukan." + report.getRingkasan()
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

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						// FIX compile "cannot find symbol": mahasiswa/skripsi dideklarasikan di luar
						// try supaya tetap terlihat di blok catch-nya di bawah (variabel lokal di
						// dalam try TIDAK terlihat di catch pasangannya).
						Mahasiswa mahasiswa = null;
						Skripsi skripsi = null;
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 1, i,
									Mahasiswa.class);

							if (mahasiswa == null) {
								continue;
							}

							skripsi = (Skripsi) ConstantValues.simpleObject(
									session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
											.addOrder(Order.desc("id")).setMaxResults(1),
									Skripsi.class);
							if (skripsi == null) {
								continue;
							}

							if (id == null) {
								id = Common.randLong();
							}

							String bibl = Common.getSheetContentAsString(sheet, 2, i);

							JSONArray jsonArray = new JSONArray(skripsi.getReferensi());
							JSONObject Data = null;
							for (int ii = 0; ii < jsonArray.length(); ii++) {
								JSONObject o = jsonArray.getJSONObject(ii);
								Long refO = ais.common.CommonJSONUtil.ambilLong(o, "ref");
								if (id != null && refO.equals(id)) {

									o.put("bibl", bibl);
									o.put("judul", bibl);

									Data = o;
								}
							}

							if (Data == null) {
								Data = new JSONObject();
								Data.put("ref", id);
								Data.put("bibl", bibl);
								Data.put("judul", bibl);
								jsonArray.put(Data);
							}

							skripsi.setReferensi(jsonArray.toString());

							session.getTransaction().begin();
							Common.refreshUpdate(session, skripsi);
							session.getTransaction().commit();

							report.sukses(i, mahasiswa.getNim() + "/" + (skripsi.getJudul() != null ? skripsi.getJudul() : "-"), "");
							label.setValue("Upload data \"" + skripsi.getJudul() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, mahasiswa != null ? mahasiswa.getNim() + "/" + (skripsi != null && skripsi.getJudul() != null ? skripsi.getJudul() : "-") : "row " + i, e, "Periksa data ref baris " + i);
						}

					}

					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SkripsiAction.java:2087");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SkripsiAction uploadRef laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public static void importAktifitasDariFeeder(FeederConnector feederConnector, String token, String idjenis)
			throws Exception {
		String tabel = "aktivitas_mahasiswa";
		Integer countInteger = feederConnector.getCount(token, tabel, "");

		String or = "id_jns_akt_mhs='" + idjenis + "'";
		System.out.println("or = " + or);
		for (int index = 0; index <= countInteger; index++) {
			List<Node> results = feederConnector.getRecordset(token, tabel, or, "", 1, index);
			if (results.isEmpty()) {
				break;
			}
			JSONObject aktivitas_mahasiswa = new JSONObject();
			for (Node result : results) {
				if (result.hasChildNodes()) {

					try {

						NodeList nodeList = result.getChildNodes();

						for (int i = 0; i < nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getTextContent() == null) {
								continue;
							}
							aktivitas_mahasiswa.put(node.getNodeName(), node.getTextContent());
						}

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			System.out.println("aktivitas_mahasiswa = " + aktivitas_mahasiswa);

			or = "id_akt_mhs='" + aktivitas_mahasiswa.getString("id_akt_mhs") + "'";
			System.out.println("sub or = " + or);
			results = feederConnector.getRecordset(token, "anggota_aktivitas_mahasiswa", or, "", 1, index);
			if (!results.isEmpty()) {

				List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
				for (Node result : results) {
					if (result.hasChildNodes()) {
						JSONObject anggota_aktivitas_mahasiswa = new JSONObject();
						try {

							NodeList nodeList = result.getChildNodes();

							for (int i = 0; i < nodeList.getLength(); i++) {
								Node node = nodeList.item(i);
								if (node.getTextContent() == null) {
									continue;
								}
								anggota_aktivitas_mahasiswa.put(node.getNodeName(), node.getTextContent());
							}
							jsonObjects.add(anggota_aktivitas_mahasiswa);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}
				System.out.println("anggota_aktivitas_mahasiswa = " + jsonObjects);
				if (!jsonObjects.isEmpty()
						&& idjenis.equals(FeederJSONImport.JENIS_KEGIATAN.get("Laporan akhir studi"))) {

					FeederJSONImport.skripsi(aktivitas_mahasiswa, jsonObjects.get(0).getString("id_reg_pd"));
				}
			}

		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Skripsi> mahasiswaRequestTugasAkhir = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/bimbingan_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Skripsi mahasiswa : mahasiswaRequestTugasAkhir) {

					try {

						LampiranLain lampiranLainBiodataCalonMahasiswa = LampiranLain.ambil(mahasiswa.getId(),
								LampiranLain.SKRIPSI);

						if (lampiranLainBiodataCalonMahasiswa != null
								&& lampiranLainBiodataCalonMahasiswa.getGdrive() != null) {
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
									+ mahasiswa.getMahasiswa().getNim() + "_" + mahasiswa.getMahasiswa().getNama() + "_"
									+ lampiranLainBiodataCalonMahasiswa.getJenis() + ".txt");
							ais.common.BacaTulisUtil.tulis(fileCopy,
									lampiranLainBiodataCalonMahasiswa.forwardGDriveUrl());
						} else if (lampiranLainBiodataCalonMahasiswa != null) {
							File file = lampiranLainBiodataCalonMahasiswa.ambilFile();
							File fileCopy = new File(
									fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getMahasiswa().getNim() + "_"
											+ mahasiswa.getMahasiswa().getNama() + "_" + file.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(file);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

						lampiranLainBiodataCalonMahasiswa = LampiranLain.ambil(mahasiswa.getId(),
								LampiranLain.COVER_SKRIPSI);

						if (lampiranLainBiodataCalonMahasiswa != null
								&& lampiranLainBiodataCalonMahasiswa.getGdrive() != null) {
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
									+ mahasiswa.getMahasiswa().getNim() + "_" + mahasiswa.getMahasiswa().getNama() + "_"
									+ lampiranLainBiodataCalonMahasiswa.getJenis() + ".txt");
							ais.common.BacaTulisUtil.tulis(fileCopy,
									lampiranLainBiodataCalonMahasiswa.forwardGDriveUrl());
						} else if (lampiranLainBiodataCalonMahasiswa != null) {

							File file = lampiranLainBiodataCalonMahasiswa.ambilFile();
							File fileCopy = new File(
									fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getMahasiswa().getNim() + "_"
											+ mahasiswa.getMahasiswa().getNama() + "_cover_" + file.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(file);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

	public static DspaceInformation getDspaceSkripsiTahun(String cookie, Skripsi skripsi) throws Exception {
		Jurusan jurusan = skripsi.getMahasiswa().getJurusan();

		String defaultD = "Skripsi";
		if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
			defaultD = "Thesis";
		}
		if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
			defaultD = "Disertasi";
		}
		if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
			defaultD = "Tugas Akhir";
		}
		String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
				.getNilai();

		String description = label_skripsi + " untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ skripsi.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", skripsi.getTahun().toString());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Thesis " + skripsi.getMahasiswa().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi(
				"dspace_label_collection_skripsi_tahun_" + jurusan.getId() + "_" + skripsi.getTahun(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + getDspaceSkripsi(cookie, skripsi) + "/collections");

	}

	public static DspaceInformation getDspaceSkripsi(String cookie, Skripsi skripsi) throws Exception {
		Jurusan jurusan = skripsi.getMahasiswa().getJurusan();

		String defaultD = "Skripsi";
		if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
			defaultD = "Thesis";
		}
		if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
			defaultD = "Disertasi";
		}
		if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
			defaultD = "Tugas Akhir";
		}
		String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
				.getNilai();

		String description = label_skripsi + " untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ skripsi.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_skripsi);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Thesis " + skripsi.getMahasiswa().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_skripsi_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	public static DspaceInformation getDspaceRevisiSkripsi(String cookie, Skripsi skripsi) throws Exception {
		Jurusan jurusan = skripsi.getMahasiswa().getJurusan();

		String defaultD = "Skripsi";
		if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
			defaultD = "Thesis";
		}
		if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
			defaultD = "Disertasi";
		}
		if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
			defaultD = "Tugas Akhir";
		}
		String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
				.getNilai();

		String description = "Revisi " + label_skripsi + " untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ skripsi.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Revisi " + label_skripsi);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Thesis " + skripsi.getMahasiswa().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_revisi_skripsi_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

	}

	public static DspaceInformation getDspaceArtefakSkripsi(String cookie, Skripsi skripsi) throws Exception {
		JSONObject jsonPost = new JSONObject();
		String info = skripsi.getMahasiswa().getNim() + "-" + skripsi.getMahasiswa().getNama() + " \""
				+ skripsi.getJudul() + "\"";
		jsonPost.put("name", info);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", "Berisi semua artefak " + info);
		jsonPost.put("shortDescription", "Artefak " + info);
		jsonPost.put("sidebarText", "Artefak " + info);
		return DspaceInformation.dspaceProcess(cookie, skripsi, jsonPost.toString(), true, "collections",
				"communities/" + getDspaceRevisiSkripsi(cookie, skripsi) + "/collections");
	}

	@SuppressWarnings("unchecked")
	public static DspaceInformation getDspace(String cookie, Skripsi skripsi, boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = skripsi.getMahasiswa().getNama();
		Jurusan jurusan = skripsi.getMahasiswa().getJurusan();

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		Map<String, Dosen> map = skripsi.populateDosenPembimbing();
		for (Dosen dosen : map.values()) {
			nama = dosen.getNama();

			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.advisor");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		map = skripsi.populateDosenPenguji();
		for (Dosen dosen : map.values()) {
			nama = dosen.getNama();

			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.other");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", skripsi.getAbstrack());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier");
		jsonMetadata.put("value", skripsi.getMahasiswa().getNim());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.type");
		jsonMetadata.put("value", "Thesis " + skripsi.getMahasiswa().getJurusan().getJenjang().getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", skripsi.getJudul());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", skripsi.getKeyword());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", skripsi.getMahasiswa().getJurusan().getNama());
		jsonArray.put(jsonMetadata);

		if (skripsi.getTanggalSidang() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(skripsi.getTanggalSidang()));
			jsonArray.put(jsonMetadata);
		}

		String ref = "";

		Session session = HibernateUtil.currentSession();
		List<DataPunyaItem> dataPunyaItems = session.createCriteria(DataPunyaItem.class)
				.add(Restrictions.eq("skripsi", skripsi)).list();
		for (DataPunyaItem dataPunyaItem : dataPunyaItems) {
			try {
				CSLItemData item = ItemAction.generateCSLItemData(dataPunyaItem.getItem());
				String bibl = Jsoup.parse(CSL.makeAdhocBibliography("apa", item).makeString()).text();

				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.citation");
				jsonMetadata.put("value", bibl);
				jsonArray.put(jsonMetadata);

				ref += bibl + "\n";

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		List<DataPunyaBukuBahanAjar> dataPunyaBukuBahanAjar = session.createCriteria(DataPunyaBukuBahanAjar.class)
				.add(Restrictions.eq("skripsi", skripsi)).list();
		for (DataPunyaBukuBahanAjar dataPunyaItem : dataPunyaBukuBahanAjar) {
			try {
				CSLItemData item = BukuBahanAjarAction.generateCSLItemData(dataPunyaItem.getBukuBahanAjar());
				String bibl = Jsoup.parse(CSL.makeAdhocBibliography("apa", item).makeString()).text();

				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.citation");
				jsonMetadata.put("value", bibl);
				jsonArray.put(jsonMetadata);

				ref += bibl + "\n";
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		List<DataPunyaArtikel> dataPunyaArtikels = session.createCriteria(DataPunyaArtikel.class)
				.add(Restrictions.eq("skripsi", skripsi)).list();
		for (DataPunyaArtikel dataPunyaArtikel : dataPunyaArtikels) {
			try {
				CSLItemData item = ArtikelAction.generateCSLItemData(dataPunyaArtikel.getArtikel());
				String bibl = Jsoup.parse(CSL.makeAdhocBibliography("apa", item).makeString()).text();

				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.citation");
				jsonMetadata.put("value", bibl);
				jsonArray.put(jsonMetadata);

				ref += bibl + "\n";
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		JSONArray referensis = new JSONArray(skripsi.getReferensi());
		for (int i = 0; i < referensis.length(); i++) {
			try {
				JSONObject jsonObject = referensis.getJSONObject(i);
				String bibl = Jsoup.parse(jsonObject.getString("bibl")).text();

				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.citation");
				jsonMetadata.put("value", bibl);
				jsonArray.put(jsonMetadata);
				ref += bibl + "\n";
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description");
		jsonMetadata.put("value", "Kata Kunci :\n" + skripsi.getKeyword() + "\n\nReferensi :\n" + ref);
		jsonArray.put(jsonMetadata);

		LampiranLain lampiranLain = LampiranLain.ambil(skripsi.getId(), LampiranLain.SKRIPSI);
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		boolean berdasarkanTahun = Common.bolehKonfigurasi("export_skripsi_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF);

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);
		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, skripsi, jsonPost.toString(),
				jsonArray.toString(), update, "items",
				"collections/" + (berdasarkanTahun ? getDspaceSkripsiTahun(cookie, skripsi)
						: getDspaceSkripsi(cookie, skripsi)) + "/items",
				"items/{uuid}/metadata");

		String defaultD = "Skripsi";
		if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
			defaultD = "Thesis";
		}
		if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
			defaultD = "Disertasi";
		}
		if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
			defaultD = "Tugas Akhir";
		}
		String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
				.getNilai();

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain, "File " + label_skripsi);

		}

		lampiranLain = LampiranLain.ambil(skripsi.getId(), LampiranLain.COVER_SKRIPSI);
		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Cover " + label_skripsi + " " + skripsi.getJudul());
		}

		lampiranLain = LampiranLain.ambil(skripsi.getId(), Skripsi.class.getName() + "_Presentasi");
		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Presentasi " + label_skripsi + " " + skripsi.getJudul());
		}

		return dspaceInformation;
	}

	protected AktifitasSkripsiHelper aktifitasSkripsiHelper = new AktifitasSkripsiHelper();
	private MyCheckboxConfig setujuiSidang;
	private Combobox tahunAkademik;
	private Combobox semester;
	private Combobox predikatKelulusan;
	private AmbilDataDosenSkripsiBanbox penguji4;
	private AmbilDataDosenSkripsiBanbox penguji5;
	private MyLabelBold p1;
	private MyLabelBold p2;
	private MyLabelBold u1;
	private MyLabelBold u2;
	private MyLabelBold u3;
	private MyLabelBold u4;
	private MyLabelBold u5;
	private MyLabelBold t;
	private MyLabelBold h;
	private MyButtonTabbox tabboxDataSkripsi;
	private AmbilDataDosenSkripsiBanbox pembimbing3;

	public static MyToolbarbuttonConfig tombolCetakSK(final Skripsi skripsi, final Dosen dosen, final String sebagai) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("SK", "/img/print.png");
		button.setTooltiptext("SK");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			private void d(Map parameters, Dosen dosen, Integer i, String key) {
				parameters.put(i == null ? "dosen_nama" : "dosen_nama_" + i, dosen.getNama());
				parameters.put(i == null ? "dosen_nidn" : "dosen_nidn_" + i, dosen.getNidn());
				parameters.put(i == null ? "dosen_nip" : "dosen_nip_" + i, dosen.getCode());
				parameters.put(i == null ? "dosen_ktp" : "dosen_ktp_" + i, dosen.getKtp());
				parameters.put(i == null ? "dosen_jurusan" : "dosen_jurusan_" + i,
						dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
				parameters.put(i == null ? "dosen_fakultas" : "dosen_fakultas_" + i,
						dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama());
				parameters.put(i == null ? "dosen_sebagai" : "dosen_sebagai_" + i, key);
				parameters.put(i == null ? "dosen_kode_golongan" : "dosen_kode_golongan_" + i,
						dosen.getGolonganPegawai() == null ? "" : dosen.getGolonganPegawai().getKode());
				parameters.put(i == null ? "dosen_nama_golongan" : "dosen_nama_golongan_" + i,
						dosen.getGolonganPegawai() == null ? "" : dosen.getGolonganPegawai().getNama());
				Common.insertProperty(Dosen.class, dosen, parameters, i == null ? "dosen" : "dosen_" + i);

				try {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

					FotoDosen fotobiodataDosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
							.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1).uniqueResult();
					if (fotobiodataDosen != null && fotobiodataDosen.ambilFile() != null) {
						parameters.put(i == null ? "foto_dosen" : "foto_dosen_" + i,
								fotobiodataDosen.ambilFile().getAbsolutePath());
					} else if (fotobiodataDosen != null) {
						parameters.put(i == null ? "foto_dosen" : "foto_dosen_" + i, fotobiodataDosen.createLinkUri());
					} else {
						File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
						parameters.put(i == null ? "foto_dosen" : "foto_dosen_" + i, file.getAbsolutePath());
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SkripsiAction.java:2643");
				}
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event event) throws Exception {
				Map parameters = ais.common.HashMapGenerator.getRand();

				if (skripsi.getMahasiswaRequestTugasAkhir() != null) {
					Common.insertProperty(MahasiswaRequestTugasAkhir.class, skripsi.getMahasiswaRequestTugasAkhir(),
							parameters, "bimbingan", 1, "mahasiswa");
				}
				Common.insertProperty(Skripsi.class, skripsi, parameters, "sidang", 1, "mahasiswa");

				Common.insertProperty(Mahasiswa.class, skripsi.getMahasiswa(), parameters, "mahasiswa");

				if (skripsi.getMahasiswa().getJurusan() != null) {
					Common.insertProperty(Jurusan.class, skripsi.getMahasiswa().getJurusan(), parameters, "jurusan");
				}
				if (skripsi.getMahasiswa().getJurusan() != null
						&& skripsi.getMahasiswa().getJurusan().getFakultas() != null) {
					Common.insertProperty(Fakultas.class, skripsi.getMahasiswa().getJurusan().getFakultas(), parameters,
							"fakultas");
				}

				parameters.put("id", skripsi.getId());
				parameters.put("tahun_akademik", skripsi.getTahunAkademik());
				parameters.put("jenis_semester",
						skripsi.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				parameters.put("semester", skripsi.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				parameters.put("dosen_id", dosen.getId());
				parameters.put("sebagai", sebagai);
				parameters.put("dosen_pembimbing", dosen.getNama());

				parameters.put("awal", skripsi == null || skripsi.getAwalBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAwalBimbingan()));

				parameters.put("akhir", skripsi == null || skripsi.getAkhirBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAkhirBimbingan()));

				String keyData = "";

				Map<String, Dosen> treeMap = skripsi.populateDosen();
				int i = 1;
				for (String key : treeMap.keySet()) {
					Dosen dosenDa = treeMap.get(key);
					if (dosenDa == null) {
						continue;
					}
					d(parameters, dosenDa, i, key);

					if (dosenDa.getId().equals(dosen.getId())) {
						keyData = key;
					}

					i++;
				}
				d(parameters, dosen, null, keyData);

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					Report.generatePDFReport(Report.PDF, parameters, "SK_Penguji_Skripsi",
							ais.ui.util.WaktuUtil.getDate());
				} else {
					Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters, parameters },
							new String[] { "SK_Penguji_Skripsi", "SK_Penguji_Skripsi_banyak" },
							new String[] { "SK " + sebagai, "SK Semua Mahasiswa" }, ais.ui.util.WaktuUtil.getDate());
				}
			}

		});
		return button;
	}

	class SkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Skripsi skripsi = (Skripsi) arg1;
			AuditListener.prosesUntukElearning(skripsi, "", skripsi.getId());
			if (skripsi.getMahasiswa() == null) {
				arg0.detach();
				return;
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			if (tbmuser.getMahasiswa() != null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						detail.setOpen(true);
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						// Tinggi pasti + scroll: tanpa ini Tabbox/Borderlayout di dalam Detail (tinggi auto) kolaps → data tidak tampil.
						groupbox.setStyle("height:72vh; min-height:480px; overflow:auto;");
						aktifitasSkripsiHelper.initDetail(skripsi, groupbox);
						detail.appendChild(groupbox);
					}
				});
			}
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						// Tinggi pasti + scroll: tanpa ini Tabbox/Borderlayout di dalam Detail (tinggi auto) kolaps → data tidak tampil.
						groupbox.setStyle("height:72vh; min-height:480px; overflow:auto;");
						aktifitasSkripsiHelper.initDetail(skripsi, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			SkripsiAction.tampilkanInfoMahasiswa(skripsi, new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {
					onSearchDefault(a);
				}
			}).setParent(arg0);

			SkripsiAction.tampilkanInfoDosen(skripsi, false, true).setParent(arg0);

			new Label(
					skripsi.getTotalNilai() == null ? "0.0" : Common.numberFormat.get().format(skripsi.getTotalNilai()))
					.setParent(arg0);

			new Label(skripsi.getNilaiHuruf() == null ? "" : skripsi.getNilaiHuruf()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);

			toolbar.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(skripsi, true);
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

											Common.refreshDelete(skripsi);

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

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (skripsi.getFeeder() != null && !skripsi.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox);
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

											final Label myLabelProsesDetail = NeoFeederProgressHelper
													.show("Sinkronisasi Neo Feeder", new EventListener() {

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
															NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																	"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																"Mengirim data " + skripsi);

														feederImporter.aktivitasMahasiswa(skripsi, errorLog);
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																"Mengirim nilai " + skripsi);
														kirimNilaiSkripsiKeFeeder(feederImporter, skripsi, errorLog);
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");

													} catch (Exception e) {
														// FIX: exception tak terduga (mis. jaringan/JSON) sebelumnya
														// hanya dicatat ke log admin (tampilErrorJikaAdmin) lalu
														// progres diset "" -- ditafsirkan SELESAI/SUKSES oleh
														// NeoFeederProgressHelper walau sebenarnya GAGAL, sehingga
														// pengguna tidak pernah tahu penyebab kegagalan. Sekarang
														// pesan error rinci ditampilkan ke pengguna.
														ais.common.Common.tampilErrorJikaAdmin(e);
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Error: "
																+ PesanFormalHelper.pesanGagalException(
																		"pengiriman data Skripsi \"" + skripsi + "\" ke Neo Feeder",
																		null, e,
																		new String[] {
																				"Periksa kembali koneksi ke server Neo Feeder dan coba ulangi proses pengiriman.",
																				"Pastikan data Mahasiswa, Program Studi, dan Dosen Pembimbing/Penguji terkait skripsi ini sudah tersinkron ke Feeder.",
																				"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																		.replace("\n", " "));
													}
												}
											}).start();

										}

									}
								});

					}
				});

			}

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Skripsi(), true);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private String ambilTahunAkademikTerpilih() {
		if (tahunAkademik != null && tahunAkademik.getSelectedItem() != null
				&& tahunAkademik.getSelectedItem().getValue() != null) {
			return tahunAkademik.getSelectedItem().getValue().toString();
		}
		if (skripsi != null && skripsi.getTahunAkademik() != null && !skripsi.getTahunAkademik().trim().isEmpty()) {
			return skripsi.getTahunAkademik();
		}
		return Common.getCurrentTahunAkademik();
	}

	private void muatGelombangPendaftaranSidang(Mahasiswa mhs) throws Exception {
		if (gelombangPendaftaranSidangTugasAkhir == null || mhs == null || mhs.getJurusan() == null) {
			return;
		}
		Common.insertComboDanSemua(gelombangPendaftaranSidangTugasAkhir, new String[] { "nama" }, "keterangan",
				GelombangPendaftaranSidangTugasAkhir.class, "== Pilih Gelombang Pendaftaran ==",
				Restrictions.and(Restrictions.eq("tahunAkademik", ambilTahunAkademikTerpilih()),
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("program"),
												Restrictions.eq("program", mhs.getProgram())),
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("fakultas"),
														Restrictions.eq("fakultas", mhs.getJurusan().getFakultas())),
												Restrictions.and(
														Restrictions.or(Restrictions.isNull("jurusan"),
																Restrictions.eq("jurusan", mhs.getJurusan())),
														Restrictions.or(
																tbmuser != null && tbmuser.getMahasiswa() == null
																		&& tbmuser.getSiswa() == null
																				? Restrictions.eq("tetapTampilDiAdmin",
																						true)
																				: Restrictions.sqlRestriction("false"),
																Restrictions.and(
																		Restrictions.le("mulai",
																				ais.ui.util.WaktuUtil.getDate()),
																		Restrictions.ge("sampai",
																				ais.ui.util.WaktuUtil
																						.getDate())))))))));
		Common.selectComboItem(true, gelombangPendaftaranSidangTugasAkhir,
				skripsi.getGelombangPendaftaranSidangTugasAkhir());
	}

	final EventListener mhsFormatEvent = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.clear(SkripsiAction.this.formatNilaiSkripsi);
			Mahasiswa mahasiswa = (Mahasiswa) SkripsiAction.this.mahasiswa.getAttribute("mahasiswa");

			Fakultas fakultas = mahasiswa == null || mahasiswa.getJurusan() == null
					? (tbmuser == null ? null : tbmuser.ambilFakultas())
					: mahasiswa.getJurusan().getFakultas();
			Jurusan jurusan = mahasiswa == null ? (tbmuser == null ? null : tbmuser.ambilJurusan())
					: mahasiswa.getJurusan();

			List<FormatNilaiSkripsi> formatNilaiSkripsis = HibernateUtil.currentSession()
					.createCriteria(FormatNilaiSkripsi.class)
					.add(tbmuser != null && tbmuser.getMahasiswa() != null
							? Restrictions.or(Restrictions.isNull("tidakBolehDipilihMahasiswa"),
									Restrictions.eq("tidakBolehDipilihMahasiswa", false))
							: Restrictions.sqlRestriction("true"))
					.add(Restrictions.or(Restrictions.isNull("fakultas"), Restrictions.eq("fakultas", fakultas)))
					.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
			for (FormatNilaiSkripsi formatNilaiSkripsi : formatNilaiSkripsis) {
				if (formatNilaiSkripsi.getTahunAngkatan().trim().isEmpty() || (mahasiswa != null && formatNilaiSkripsi
						.getTahunAngkatan().trim().contains(mahasiswa.getTahunangkatan().toString()))) {
					Comboitem comboitem = new Comboitem(formatNilaiSkripsi.getNama());
					String mk = "";
					for (String kode : formatNilaiSkripsi.getKodeMatakuliah().split(",")) {
						if (!kode.trim().isEmpty()) {
							Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.projectionList().add(Projections.property("kode"))
											.add(Projections.property("nama")))
									.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
											Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
									.setMaxResults(1).uniqueResult();
							if (nama != null && nama.length > 1) {
								mk += mk.isEmpty() ? (nama[0] + " - " + nama[1])
										: " atau " + (nama[0] + " - " + nama[1]);
							}
						}
					}

					for (String kode : formatNilaiSkripsi.getKodeMatakuliahDan().split(",")) {
						if (!kode.trim().isEmpty()) {
							Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.projectionList().add(Projections.property("kode"))
											.add(Projections.property("nama")))
									.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
											Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
									.setMaxResults(1).uniqueResult();
							if (nama != null && nama.length > 1) {
								mk += mk.isEmpty() ? (nama[0] + " - " + nama[1])
										: " dan " + (nama[0] + " - " + nama[1]);
							}
						}
					}
					comboitem.setValue(formatNilaiSkripsi);
					comboitem.setDescription(mk);

					SkripsiAction.this.formatNilaiSkripsi.appendChild(comboitem);
				}
			}
			Common.selectComboItem(true, formatNilaiSkripsi, skripsi.getFormatNilaiSkripsi());

			if (!formatNilaiSkripsi.getChildren().isEmpty() && formatNilaiSkripsi.getSelectedItem() == null) {
				formatNilaiSkripsi.setSelectedIndex(0);
			}

//			if (skripsi != null && skripsi.getMahasiswaRequestTugasAkhir() != null
//					&& skripsi.getMahasiswaRequestTugasAkhir().getFormatNilaiProposalSkripsi() != null
//					&& skripsi.getMahasiswaRequestTugasAkhir().getFormatNilaiProposalSkripsi()
//							.getFormatNilaiSkripsi() != null) {
//				Common.selectComboItem(true, formatNilaiSkripsi, skripsi.getMahasiswaRequestTugasAkhir()
//						.getFormatNilaiProposalSkripsi().getFormatNilaiSkripsi());
////				formatNilaiSkripsi.setDisabled(true);
//			}
		}
	};
	private BiodataMahasiswa biodataMahasiswa = null;
	private Textbox namaUntukIjazah;
	private MyTextbox tanggallahirManual;
	private Combobox statusSetelahLulus;
	private Combobox statusPekerjaanSetelahLulus;
	private Combobox statusDomisiliSetelahLulus;
	private Combobox gelombangPendaftaranSidangTugasAkhir;

	private Row rowUploadLampiran1;
	private Row rowUploadLampiran2;
	private Row rowUploadLampiran3;
	private Row rowUploadLampiran4;
	private Row rowUploadLampiran5;
	private Row rowUploadLampiran6;
	private Row rowUploadLampiran7;
	private Row rowUploadLampiran8;
	private Row rowUploadLampiran9;
	private Row rowUploadLampiran10;
	private Row rowUploadLampiran11;
	private Row rowUploadLampiran12;
	private Row rowUploadLampiran13;
	private Row rowUploadLampiran14;
	private Row rowUploadLampiran15;
	private Row rowUploadLampiran16;
	private Row rowUploadLampiran17;
	private Row rowUploadLampiran18;
	private Row rowUploadLampiran19;
	private Row rowUploadLampiran20;

	private MyDatebox tanggalSkRektor;
	private Textbox nomorSkpi;
	protected LampiranLain lainMahasiswaPresentasi;
	private JSONArray referensis;

	private Row rowPembimbing1;
	private Row rowPembimbing2;
	private Row rowPembimbing3;
	private Textbox feeder;

	private static void addReferensi(final JSONObject jsonObject, final Skripsi skripsi, Rows subrowsRefs,
			final EventListener eventListener) throws Exception {
		final Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject, "ref");
		final MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrowsRefs);
		subrow.setValign("top");
		subrow.setAttribute("o", jsonObject.toString());

		Vbox vbox = new Vbox();
		subrow.appendChild(vbox);

		try {
			String bibl = jsonObject.getString("bibl");
			vbox.appendChild(new ais.ui.util.MyHtml(bibl));
		} catch (Exception e) {
			vbox.appendChild(new MyLabelAgakKecilBold(jsonObject.getString("judul")));
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Hbox hbox = new Hbox();

		LampiranLain.createDownloadUploadFileLain(hbox, ref, "Lampiran_Referensi", "Lampiran Referensi", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true);

		hbox.setParent(vbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
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

									JSONArray jsonArrayCopy = new JSONArray();
									JSONArray jsonArray = new JSONArray(skripsi.getReferensi());

									for (int ii = 0; ii < jsonArray.length(); ii++) {
										JSONObject o = jsonArray.getJSONObject(ii);
										Long refO = ais.common.CommonJSONUtil.ambilLong(o, "ref");
										if (!refO.equals(ref)) {
											jsonArrayCopy.put(o);
										}
									}

									skripsi.setReferensi(jsonArrayCopy.toString());

									if (skripsi.getId() != null) {
										Common.refreshUpdate(skripsi);
									}

									eventListener.onEvent(new Event("", null, skripsi));
									subrow.detach();
								}

							}
						});

			}
		});
		button.setParent(subrow);
	}

	public static Grid initReferensi(final Skripsi skripsi, final EventListener eventListener) throws Exception {

		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumnRef = new MyColumnConfig();
		subcolumnRef.setParent(subcolumns);
		subcolumnRef.setWidth("90%");

		MyColumnConfig subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);

		final Rows subrowsRefs = new Rows();
		subrowsRefs.setParent(subGrid);
		JSONArray referensis = new JSONArray(skripsi.getReferensi());
		for (int i = 0; i < referensis.length(); i++) {
			JSONObject jsonObject = referensis.getJSONObject(i);
			addReferensi(jsonObject, skripsi, subrowsRefs, eventListener);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Daftar Pustaka", "/img/add_item.png");
		button.setTooltiptext("Tambah Daftar Pustaka");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow("Tambah Daftar Pustaka", "none", true);
				addWindow.setHeight("500px");
				addWindow.setWidth("400px");
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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

				final Long ref = Common.randLong();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
				final Textbox nama;
				row.appendChild(nama = new Textbox());
				nama.setWidth("90%");
				nama.setRows(2);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Pengarang *"));
				final Textbox pengarang;
				row.appendChild(pengarang = new Textbox());
				pengarang.setWidth("90%");
				pengarang.setRows(2);

				Common.initKeterangan(rows, "Pisahkan dengan tanda semikolon (;) jika pengarang lebih dari satu");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Terbit / Publikasi *"));
				final MyDatebox tanggal;
				row.appendChild(tanggal = new MyDatebox());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
				final Textbox penerbit;
				row.appendChild(penerbit = new Textbox());
				penerbit.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("ISBN"));
				final Textbox isbn;
				row.appendChild(isbn = new Textbox());
				isbn.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("ISSN / eISSN"));
				final Textbox issn;
				row.appendChild(issn = new Textbox());
				issn.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sumber / Link / URL"));
				final Textbox sumber;
				row.appendChild(sumber = new Textbox());
				sumber.setWidth("90%");
				sumber.setRows(2);

				MyFormRow rowLampiran = new MyFormRow();
				rowLampiran.setParent(rows);
				rowLampiran.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Referensi"));
				Hbox hbox = new Hbox();
				LampiranLain.createDownloadUploadFileLain(hbox, ref, "Lampiran_Referensi", "Lampiran Referensi", false,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				hbox.setParent(rowLampiran);

				Common.initKeterangan(rows,
						"Jika file lampiran dokumen lebih dari satu file, zip dulu semua file tersebut");

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
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Judul",
									"Kolom Judul belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Judul.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (pengarang.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Pengarang",
									"Kolom Pengarang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Pengarang.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (tanggal.getValue() == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Terbit / Publikasi",
									"Kolom Tanggal Terbit / Publikasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Tanggal Terbit / Publikasi.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						addWindow.detach();

						JSONObject jsonObject = new JSONObject();

						jsonObject.put("ref", ref);
						jsonObject.put("judul", nama.getValue().trim());
						jsonObject.put("penerbit", penerbit.getValue().trim());
						jsonObject.put("pengarang", pengarang.getValue().trim());
						jsonObject.put("tanggal", Common.dateFormat1.get().format(tanggal.getValue()));
						jsonObject.put("isbn", isbn.getValue().trim());
						jsonObject.put("issn", issn.getValue().trim());
						jsonObject.put("sumber", sumber.getValue().trim());

						String bibl = CSL.makeAdhocBibliography("apa", Common.convertToCSLItemData(jsonObject))
								.makeString();
						jsonObject.put("bibl", bibl);
						JSONArray jsonArray = new JSONArray(skripsi.getReferensi());
						jsonArray.put(jsonObject);
						skripsi.setReferensi(jsonArray.toString());

						if (skripsi.getId() != null) {
							Common.refreshUpdate(skripsi);
						}

						eventListener.onEvent(new Event("", null, skripsi));

						addReferensi(jsonObject, skripsi, subrowsRefs, eventListener);
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);
				addWindow.onModal();
			}
		});
		button.setParent(subcolumnRef);

		button = new MyToolbarbuttonConfig("Tambah Daftar Langsung / Per Sitasi", "/img/add_item.png");
		button.setTooltiptext("Tambah Daftar Langsung / Per Sitasi");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow("Tambah Daftar Langsung / Per Sitasi", "none", true);
				addWindow.setHeight("500px");
				addWindow.setWidth("400px");
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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

				final Long ref = Common.randLong();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sitasi *"));
				final Textbox nama;
				row.appendChild(nama = new Textbox());
				nama.setWidth("90%");
				nama.setRows(10);

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
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Sitasi",
									"Kolom Sitasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Sitasi.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						addWindow.detach();

						JSONObject jsonObject = new JSONObject();

						jsonObject.put("ref", ref);
						jsonObject.put("judul", nama.getValue().trim());
						jsonObject.put("bibl", nama.getValue().trim());
						JSONArray jsonArray = new JSONArray(skripsi.getReferensi());
						jsonArray.put(jsonObject);
						skripsi.setReferensi(jsonArray.toString());

						if (skripsi.getId() != null) {
							Common.refreshUpdate(skripsi);
						}

						eventListener.onEvent(new Event("", null, skripsi));

						addReferensi(jsonObject, skripsi, subrowsRefs, eventListener);
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);
				addWindow.onModal();
			}
		});
		button.setParent(subcolumnRef);

		return subGrid;
	}

	private Borderlayout initAbstrack(Skripsi skripsi, MyToolbarbuttonConfig save) throws Exception {

		lainMahasiswa = null;
		lainMahasiswaCover = null;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setStyle("border:0px;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		disposisiSop = null;
		center.appendChild(form(skripsi, disposisiSop, save, null));

		return borderlayout;

	}

	private Borderlayout initKelulusan(final Skripsi skripsi) throws Exception {

		final Mahasiswa mahasiswa = skripsi.getMahasiswa();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);

		Vbox vbox = new Vbox();
		vbox.setPack("center");
		vbox.setAlign("center");
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(west);
		final Image foto;
		vbox.appendChild(foto = new Image("/img/administrator-icon_default.png"));
		// foto.setHeight("300px");
		foto.setWidth("250px");
		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig(
				"Ganti Foto Kelulusan" + Common.ukuranLabelFileUpload(), "/img/File-Upload-icon.png");
		fileupload.setUpload(Common.ukuranFileUpload());
		fileupload.setUpload(Common.ukuranFileUpload());
		vbox.appendChild(fileupload);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Mahasiswa mahasiswa = (Mahasiswa) SkripsiAction.this.mahasiswa.getAttribute("mahasiswa");

						if (mahasiswa == null) {
							return;
						}

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						FotoMahasiswaLulus fotoMahasiswaLulus = (FotoMahasiswaLulus) streamingSession
								.createCriteria(FotoMahasiswaLulus.class)
								.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();
						if (fotoMahasiswaLulus != null) {
							streamingSession.getTransaction().begin();
							streamingSession.delete(fotoMahasiswaLulus);
							streamingSession.getTransaction().commit();
						}

						fotoMahasiswaLulus = new FotoMahasiswaLulus();
						fotoMahasiswaLulus.setNama(uploadEvent.getMedia().getName());
						fotoMahasiswaLulus.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoMahasiswaLulus.setMahasiswa(mahasiswa.getId());

						fotoMahasiswaLulus.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoMahasiswaLulus);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						MediaParameter mediaParameter = new MediaParameter(mahasiswa.getId().toString(), "nama", "foto",
								FotoMahasiswaLulus.class, "mahasiswa", 300, 250);
						String src = CommonMedia.getMedia(mediaParameter);
						foto.setSrc(src);
					} else {
						if (mahasiswa.getId() != null) {

							MediaParameter mediaParameter = new MediaParameter(mahasiswa.getId().toString(), "nama",
									"foto", FotoMahasiswaLulus.class, "mahasiswa", 300, 250);
							String src = CommonMedia.getMedia(mediaParameter);
							foto.setSrc(src);
						}
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		eventListener.onEvent(null);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		biodataMahasiswa = mahasiswa.ambilBiodata();

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama yang tertera di Ijazah Sebelumnya"));
		row.appendChild(
				namaUntukIjazah = new Textbox(biodataMahasiswa.getNamaUntukIjazah() == null ? mahasiswa.getNama()
						: biodataMahasiswa.getNamaUntukIjazah()));
		namaUntukIjazah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl lahir yang tertera di Ijazah Sebelumnya"));
		row.appendChild(tanggallahirManual = new MyTextbox(mahasiswa.getTanggallahirManual()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button button = new Button("Preview Ijazah", "/img/print.png");
		row.appendChild(button);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanTranskipAkademik laporanIjazahAkademik = new LaporanTranskipAkademik(mahasiswa);
				laporanIjazahAkademik.setTitle("Preview Ijazah");
				laporanIjazahAkademik.setClosable(true);
				laporanIjazahAkademik.setHeight("95%");
				laporanIjazahAkademik.setWidth("90%");
				laporanIjazahAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanIjazahAkademik.onModal();
			}
		});

		// Form INTI "Informasi Kelulusan" dibangun lewat FormKelulusanHelper agar SAMA PERSIS
		// dengan MahasiswaAction (reuse). Komponen disimpan untuk dipakai saat simpan.
		kelulusanKomponen = ais.action.master.helper.FormKelulusanHelper.build(rows, mahasiswa, tbmuser);
		statusKeluar = kelulusanKomponen.statusKeluar;
		predikatKelulusan = kelulusanKomponen.predikatKelulusan;
		statusSetelahLulus = kelulusanKomponen.statusSetelahLulus;
		statusPekerjaanSetelahLulus = kelulusanKomponen.statusPekerjaanSetelahLulus;
		statusDomisiliSetelahLulus = kelulusanKomponen.statusDomisiliSetelahLulus;
		noIjazah1 = kelulusanKomponen.noIjazah1;
		noIjazah2 = kelulusanKomponen.noIjazah2;
		noAkta1 = kelulusanKomponen.noAkta1;
		noAkta2 = kelulusanKomponen.noAkta2;
		nomorSkpi = kelulusanKomponen.nomorSkpi;
		tahunLulus = kelulusanKomponen.tahunLulus;
		semesterLulus = kelulusanKomponen.semesterLulus;
		tahunWisuda = kelulusanKomponen.tahunWisuda;
		tanggalLulus = kelulusanKomponen.tanggalLulus;
		tanggalYudisium = kelulusanKomponen.tanggalYudisium;
		tanggalSkRektor = kelulusanKomponen.tanggalSkRektor;

		return borderlayout;
	}

	private EventListener hasilSidangListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			FormatNilaiSkripsi f = (FormatNilaiSkripsi) (formatNilaiSkripsi.getSelectedItem() == null ? null
					: formatNilaiSkripsi.getSelectedItem().getValue());
			if (f != null) {
				SkripsiAction.this.skripsi.setFormatNilaiSkripsi(f);
				n1.setValue(f.getDosen1() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiKetuaSidang())
						+ "%)");

				n2.setValue(f.getDosen2() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPembimbing())
						+ "%)");
				n21.setValue(f.getDosen21() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPembimbing3())
						+ "%)");
				n3.setValue(
						f.getDosen3() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPenguji1()) + "%)");
				n4.setValue(
						f.getDosen4() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPenguji2()) + "%)");
				n5.setValue(
						f.getDosen5() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPenguji3()) + "%)");
				n6.setValue(
						f.getDosen6() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPenguji4()) + "%)");
				n6.setValue(
						f.getDosen7() + " (" + Common.numberFormat.get().format(f.getProsentasiNilaiPenguji5()) + "%)");

				n1.getParent().setVisible(f.getProsentasiNilaiKetuaSidang() > 0.1);
				n2.getParent().setVisible(f.getProsentasiNilaiPembimbing() > 0.1);
				n21.getParent().setVisible(f.getProsentasiNilaiPembimbing3() > 0.1);
				n3.getParent().setVisible(f.getProsentasiNilaiPenguji1() > 0.1);
				n4.getParent().setVisible(f.getProsentasiNilaiPenguji2() > 0.1);
				n5.getParent().setVisible(f.getProsentasiNilaiPenguji3() > 0.1);
				n6.getParent().setVisible(f.getProsentasiNilaiPenguji4() > 0.1);
				n7.getParent().setVisible(f.getProsentasiNilaiPenguji5() > 0.1);

				if (rowPembimbing1 != null)
					rowPembimbing1.setVisible(f.getProsentasiNilaiKetuaSidang() > 0.1);
				if (rowPembimbing2 != null)
					rowPembimbing2.setVisible(f.getProsentasiNilaiPembimbing() > 0.1);
				if (rowPembimbing3 != null)
					rowPembimbing3.setVisible(f.getProsentasiNilaiPembimbing3() > 0.1);

				if (rowPenguji1 != null)
					rowPenguji1.setVisible(f.getProsentasiNilaiPenguji1() > 0.1);
				if (rowPenguji2 != null)
					rowPenguji2.setVisible(f.getProsentasiNilaiPenguji2() > 0.1);
				if (rowPenguji3 != null)
					rowPenguji3.setVisible(f.getProsentasiNilaiPenguji3() > 0.1);
				if (rowPenguji4 != null)
					rowPenguji4.setVisible(f.getProsentasiNilaiPenguji4() > 0.1);
				if (rowPenguji5 != null)
					rowPenguji5.setVisible(f.getProsentasiNilaiPenguji5() > 0.1);

				if (SkripsiAction.this.skripsi.getSembunyikanNilaiKemahasiswa()) {
					n1.getParent().setVisible(false);
					n2.getParent().setVisible(false);
					n21.getParent().setVisible(false);
					n3.getParent().setVisible(false);
					n4.getParent().setVisible(false);
					n5.getParent().setVisible(false);
					n6.getParent().setVisible(false);
					n7.getParent().setVisible(false);
					t.getParent().setVisible(false);
					h.getParent().setVisible(false);
				}

			}

		}
	};
	private MyLabelConfig n1;
	private MyLabelConfig n3;
	private MyLabelConfig n4;
	private MyLabelConfig n5;
	private MyLabelConfig n6;
	private MyLabelConfig n2;
	private MyLabelConfig n21;
	private Textbox lokasiUjian;
	private MyDatebox tglSk;
	private Textbox nomorSk;
	private MyLabelBold p21;
	private Row rowPenguji1;
	private Row rowPenguji2;
	private Row rowPenguji4;
	private Row rowPenguji3;
	private Row rowPenguji5;
	private Checkbox persetujuanPenguji5;
	private MyLabelConfig n7;
	private boolean persetujuan;
	private DisposisiSop disposisiSop = null;

	public static MyWindow initComponenAddExternal(Skripsi skripsi, Mahasiswa mahasiswa) throws Exception {
		SkripsiAction skripsiAction = new SkripsiAction();
		skripsiAction.dataMahasiswa = mahasiswa;
		skripsiAction.eventListener = null;
		skripsiAction.addWindow = new MyWindow();
		skripsiAction.init(skripsi, false);
		skripsiAction.addWindow.setAttribute("skripsiAction", skripsiAction);
		return skripsiAction.addWindow;
	}

	public static void onAddExternal(EventListener eventListener, Skripsi skripsi, Mahasiswa mahasiswa)
			throws Exception {
		SkripsiAction skripsiAction = new SkripsiAction();
		skripsiAction.dataMahasiswa = mahasiswa;
		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		skripsiAction.init(skripsi, true);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();

	}

	private void init(final Skripsi skripsi, boolean tampilkanSimpan) throws Exception {
		this.skripsi = skripsi;
		Common.clear(addWindow);
		addWindow.setTitle(
				skripsi.getId() == null ? "Tambah Data " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai()
						: "Ubah Data " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai());
		addWindow.setWidth("90%");
		addWindow.setHeight("97%");
		formatNilaiSkripsi = new Combobox();

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setStyle("border:0px;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// Center bisa di-SCROLL: konten tab (mis. Penilaian setinggi 3500px) lebih tinggi dari
		// window -> pengguna dapat menggulir ke bawah (Tabbox tak lagi dipaku height:100%).
		center.setAutoscroll(true);

		int[] tabAktif = {0};
		tabboxDataSkripsi = MyButtonTabbox.buat(center, "100%", tabAktif);

		Div tabpanel = tabboxDataSkripsi.tambahTab(0, "Judul dan Pembimbing");
		tabpanel.appendChild(initAbstrack(skripsi, save));

		final Div tabpanelPenilaian = tabboxDataSkripsi.tambahTab(1, "Penilaian");
		tabpanelPenilaian.setHeight("3500px");
		tabboxDataSkripsi.setVisibleTombol(1, skripsi.getSetujuiSidang());
		tabboxDataSkripsi.onSetiapPilih(1, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!checkSyaratPembimbingDanPenguji()) {
					pilihTabJudulSkripsi();
					return;
				}

				if (!checkSyarat()) {
					pilihTabJudulSkripsi();
					return;
				}

				if (SkripsiAction.this.skripsi.getId() == null) {
					if (!onSave(arg0)) {
						pilihTabJudulSkripsi();
						return;
					}
				}

				if (!setujuiSidang.isChecked()) {
					MyMessageboxConfig.show("Persetujuan sidang belum dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tbmuser.getMahasiswa() != null) {
										pilihTabJudulSkripsi();
									} else {
										Clients.scrollIntoView(setujuiSidang);
									}
								}
							});

					return;
				}

				FormatNilaiSkripsi f = (FormatNilaiSkripsi) (formatNilaiSkripsi.getSelectedItem() == null ? null
						: formatNilaiSkripsi.getSelectedItem().getValue());
				if (f == null) {
					MyMessageboxConfig.show(
							"Format nilai " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai()
									+ " harus diisi",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									pilihTabJudulSkripsi();
								}
							});
					return;
				}
				SkripsiAction.this.skripsi.setFormatNilaiSkripsi(f);
				SkripsiAction.this.sinkronkanDosenDariFormKeSkripsi();

				Common.clear(tabpanelPenilaian);

				PenilaianSkripsiHelper penilaianSkripsiHelper = new PenilaianSkripsiHelper();
				penilaianSkripsiHelper.display(SkripsiAction.this.skripsi, tabpanelPenilaian,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								SkripsiAction.this.skripsi = (Skripsi) arg0.getData();

								p1.setValue(Common.numberFormat.get().format(skripsi.getNilaiKetuaSidang()));
								p2.setValue(Common.numberFormat.get().format(skripsi.getNilaiPembimbing()));
								p21.setValue(Common.numberFormat.get().format(skripsi.getNilaiPembimbing3()));

								u1.setValue(Common.numberFormat.get().format(skripsi.getNilaiPenguji1()));
								u2.setValue(Common.numberFormat.get().format(skripsi.getNilaiPenguji2()));
								u3.setValue(Common.numberFormat.get().format(skripsi.getNilaiPenguji3()));
								u4.setValue(Common.numberFormat.get().format(skripsi.getNilaiPenguji4()));
								u5.setValue(Common.numberFormat.get().format(skripsi.getNilaiPenguji5()));

								t.setValue(Common.numberFormat.get().format(skripsi.getTotalNilai()));
								h.setValue(skripsi.getNilaiHuruf());
							}
						});
			}
		});

		final Div tabpanelInfoKelulusan = tabboxDataSkripsi.tambahTab(2, "Informasi Kelulusan Mahasiswa");

		tabboxDataSkripsi.onSetiapPilih(2, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Mahasiswa mahasiswa = (Mahasiswa) SkripsiAction.this.mahasiswa.getAttribute("mahasiswa");

				if (mahasiswa == null) {
					MyMessageboxConfig.show("Sebelum mendapat info kelulusan mahasiswa, data mahasiswa harus diisi",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									pilihTabJudulSkripsi();
								}
							});
					return;
				}

				if (tabpanelInfoKelulusan.getChildren().isEmpty()) {
					SkripsiAction.this.skripsi.setMahasiswa(mahasiswa);
					tabpanelInfoKelulusan.appendChild(initKelulusan(SkripsiAction.this.skripsi));
				}
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setVisible(tampilkanSimpan);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					addWindow.detach();
				} else {
					addWindow.setVisible(false);
				}
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);

					if (eventListener != null) {
						addWindow.detach();
					} else {
						addWindow.setVisible(false);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	private void pilihTabJudulSkripsi() {
		if (tabboxDataSkripsi != null) {
			tabboxDataSkripsi.pilih(0);
		}
	}

	private Dosen ambilDosenDariForm(AmbilDataDosenSkripsiBanbox dosenBanbox) {
		return dosenBanbox == null ? null : (Dosen) dosenBanbox.getAttribute("myValue");
	}

	private void sinkronkanDosenDariFormKeSkripsi() {
		if (skripsi == null) {
			return;
		}

		Dosen dosenPembimbing = ambilDosenDariForm(pembimbing);
		Dosen dosenKetuaSidang = ambilDosenDariForm(ketuaSidang);
		Dosen dosenPembimbing3 = ambilDosenDariForm(pembimbing3);

		skripsi.setPembimbing(dosenPembimbing);
		skripsi.setKetuaSidang(dosenKetuaSidang);
		skripsi.setPembimbing3(dosenPembimbing3);
		skripsi.setPenguji1(ambilDosenDariForm(penguji1));
		skripsi.setPenguji2(ambilDosenDariForm(penguji2));
		skripsi.setPenguji3(ambilDosenDariForm(penguji3));
		skripsi.setPenguji4(ambilDosenDariForm(penguji4));
		skripsi.setPenguji5(ambilDosenDariForm(penguji5));

		MahasiswaRequestTugasAkhir requestTugasAkhir = skripsi.getMahasiswaRequestTugasAkhir();
		if (requestTugasAkhir != null) {
			requestTugasAkhir.setDosen1(dosenPembimbing);
			requestTugasAkhir.setDosen2(dosenKetuaSidang);
			requestTugasAkhir.setDosen3(dosenPembimbing3);
		}
	}

	public boolean checkSyaratPembimbingDanPenguji() throws Exception {

		if (Common.bolehKonfigurasi("dosen_penguji_dan_pembimbing_skripsi_boleh_sama", Konfigurasi.TIDAK_AKTIF)) {
			return true;
		}

		List<Long> checkDosens = new ArrayList<Long>();
		if (ketuaSidang.getAttribute("myValue") != null) {
			checkDosens.add(((Dosen) ketuaSidang.getAttribute("myValue")).getId());
		}
		if (pembimbing.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) pembimbing.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pilihTabJudulSkripsi();
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) pembimbing.getAttribute("myValue")).getId());
		}
		if (penguji1.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) penguji1.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.scrollIntoView(setujuiSidang);
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) penguji1.getAttribute("myValue")).getId());
		}
		if (penguji2.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) penguji2.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.scrollIntoView(setujuiSidang);
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) penguji2.getAttribute("myValue")).getId());
		}
		if (penguji3.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) penguji3.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.scrollIntoView(setujuiSidang);
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) penguji3.getAttribute("myValue")).getId());
		}
		if (penguji4.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) penguji4.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.scrollIntoView(setujuiSidang);
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) penguji4.getAttribute("myValue")).getId());
		}

		if (penguji5.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) penguji5.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Data dosen pembimbing atau dosen penguji tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.scrollIntoView(setujuiSidang);
							}
						});
				return false;
			}
			checkDosens.add(((Dosen) penguji5.getAttribute("myValue")).getId());
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean checkSyarat() throws Exception {
		Mahasiswa mahasiswa = (Mahasiswa) this.mahasiswa.getAttribute("mahasiswa");

		if (mahasiswa == null) {
			MyMessageboxConfig.show("Mahasiswa harus dipilih", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		FormatNilaiSkripsi formatNilaiSkripsi = (FormatNilaiSkripsi) (this.formatNilaiSkripsi.getSelectedItem() == null
				? null
				: this.formatNilaiSkripsi.getSelectedItem().getValue());
		if (formatNilaiSkripsi == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis pengajuan belum dipilih. Bapak/Ibu diharapkan memilih jenis pengajuan terlebih dahulu sebelum melanjutkan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			this.formatNilaiSkripsi.focus();
			return false;
		}
		Double harusLunas = formatNilaiSkripsi.getProsentaseLunas();
		Integer smt = (Integer) (semester.getSelectedItem() == null ? mahasiswa.currentSemester()
				: semester.getSelectedItem().getValue());
		if (harusLunas > 0.1
				&& !Common.checkStatusPembayaranMahasiswaPengajuanSidang(formatNilaiSkripsi, smt, mahasiswa)) {
			MyMessageboxConfig.show(
					"Untuk dapat mengajukan " + formatNilaiSkripsi.getNama() + ", mahasiswa \"" + mahasiswa.toString()
							+ "\" harus melunasi " + harusLunas + "% biaya perkuliahan di semester " + smt
							+ ". Harap hubungi bagian keuangan untuk informasi lebih lanjut",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

			return false;
		}

		if (formatNilaiSkripsi.getHarusMengembalikanBukuPerpustakaan()) {

			Session session = HibernateUtil.currentSession();
			List<PeminjamanPengadaanItemDetail> objects = session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
					.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.createAlias("peminjamanPengadaanItem.anggota", "anggota")
					.add(Restrictions.eq("anggota.mahasiswa", mahasiswa)).list();
			String content = "";
			for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : objects) {
				content += "Item atau buku \"" + peminjamanPengadaanItemDetail.getItem().getNama() + "\" terlambat "
						+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahHariTerlambat())
						+ " hari di perpustakaan "
						+ peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getPerpustakaan().getNama()
						+ ".\n";
			}

			if (!content.trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Pengajuan " + formatNilaiSkripsi.getNama()
								+ " harus telah mengembalikan semua buku perpustakaan, yaitu buku sbb :\n\n" + content,
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

				return false;
			}

		}

		return true;
	}

	public boolean onSave(Event event) throws Exception {

		if (!checkSyarat()) {
			return false;
		}
		sinkronkanDosenDariFormKeSkripsi();
		if (gelombangPendaftaranSidangTugasAkhir.getSelectedItem() == null
				|| gelombangPendaftaranSidangTugasAkhir.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gelombang Sidang",
					"Gelombang Pendaftaran Sidang tidak tersedia atau belum dipilih. "
					+ "Kemungkinan penyebab: (1) Belum ada gelombang sidang yang dibuat untuk Tahun Akademik ini, "
					+ "(2) Tanggal hari ini di luar rentang Mulai–Sampai gelombang, "
					+ "(3) Gelombang tidak aktif, atau "
					+ "(4) Program / Jurusan / Fakultas mahasiswa tidak sesuai dengan gelombang yang ada.",
					new String[] {
							"Buka tab 'Gelombang Sidang' (menu Tugas Akhir) → pastikan sudah ada gelombang aktif untuk Tahun Akademik ini.",
							"Pastikan tanggal hari ini berada dalam rentang Mulai–Sampai gelombang, ATAU centang 'Tetap Tampil Di Admin' pada gelombang tersebut.",
							"Pastikan kolom Program, Jurusan, dan Fakultas pada gelombang cocok dengan data mahasiswa (atau biarkan kosong agar berlaku untuk semua).",
							"Setelah gelombang tersedia, kembali ke form ini dan pilih gelombang sebelum menyimpan."
					});
			return false;
		}
		if (judulCK.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul",
					"Kolom Judul belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (semester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, semester belum dipilih. Bapak/Ibu diharapkan memilih semester terlebih dahulu sebelum menyimpan data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			mahasiswa.focus();
			return false;
		}

		Mahasiswa mahasiswa = (Mahasiswa) this.mahasiswa.getAttribute("mahasiswa");
		if (mahasiswa == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		FormatNilaiSkripsi formatNilaiSkripsi = (FormatNilaiSkripsi) (this.formatNilaiSkripsi.getSelectedItem() == null
				? null
				: this.formatNilaiSkripsi.getSelectedItem().getValue());
		if (formatNilaiSkripsi == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis pengajuan belum dipilih. Bapak/Ibu diharapkan memilih jenis pengajuan terlebih dahulu sebelum melanjutkan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			this.formatNilaiSkripsi.focus();
			return false;
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
				(Integer) semester.getSelectedItem().getValue(), null, null);
		int sks = krsMahasiswa.getSksk();
		int batasSks = formatNilaiSkripsi.getMinimalSks();
		if (batasSks > sks) {
			MyMessageboxConfig.show("Untuk dapat mengajukan sidang skripsi atau tugas akhir, mahasiswa \""
					+ mahasiswa.toString() + "\" harus memiliki minimal " + batasSks
					+ " SKS. Sedangkan SKS yang telah diperoleh " + sks + " SKS", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

			return false;
		}

		Double angkaKredit = Common.hitungAngkaKredit(mahasiswa);
		Double batasAngkaKredit = formatNilaiSkripsi.getMinimalAngkaKredit();
		if (batasAngkaKredit > angkaKredit) {
			MyMessageboxConfig.show("Untuk dapat mengajukan sidang skripsi atau tugas akhir, mahasiswa \""
					+ mahasiswa.toString() + "\" harus memiliki minimal " + batasAngkaKredit
					+ " Angka Kredit kegiatan kemahasiswaan. Sedangkan Angka Kredit yang telah diperoleh " + angkaKredit
					+ "", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

			return false;
		}

		double ipk = krsMahasiswa.getIpk();
		double batasIpk = formatNilaiSkripsi.getMinimalIpk();
		if (batasIpk > ipk) {
			MyMessageboxConfig.show(
					"Untuk dapat mengajukan sidang skripsi atau tugas akhir, mahasiswa \"" + mahasiswa.toString()
							+ "\" harus memiliki minimal IPK " + Common.numberFormat.get().format(batasIpk)
							+ ". Sedangkan IPK yang telah diperoleh " + Common.numberFormat.get().format(ipk),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

			return false;
		}

		// PRASYARAT MATAKULIAH LULUS: bila format menetapkan matkul wajib lulus, mahasiswa harus SUDAH
		// LULUS semua matkul tsb (dicocokkan per KODE atau NAMA) agar boleh mendaftar sidang.
		String prasyaratMk = formatNilaiSkripsi.getMatkulPrasyaratLulus();
		if (prasyaratMk != null && !prasyaratMk.trim().isEmpty()) {
			java.util.Set<String> lulusKodeNama = new java.util.HashSet<String>();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<Detailperkuliahan> dps = HibernateUtil.currentSession()
						.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa)).list();
				for (Detailperkuliahan dp : dps) {
					if (dp.getLulus() != null && dp.getLulus().booleanValue()) {
						Matakuliah mk = dp.getPerkuliahan() != null ? dp.getPerkuliahan().getMatakuliah()
								: dp.getMatakuliahKonversi();
						if (mk != null) {
							if (mk.getKode() != null) {
								lulusKodeNama.add(mk.getKode().trim().toUpperCase());
							}
							if (mk.getNama() != null) {
								lulusKodeNama.add(mk.getNama().trim().toUpperCase());
							}
						}
					}
				}
			} catch (Exception ePrasyarat) {
				Common.tampilErrorJikaAdmin(ePrasyarat);
			}
			java.util.List<String> belumLulus = new java.util.ArrayList<String>();
			for (String tok : prasyaratMk.split(",")) {
				String t = tok == null ? "" : tok.trim();
				if (t.isEmpty()) {
					continue;
				}
				if (!lulusKodeNama.contains(t.toUpperCase())) {
					belumLulus.add(t);
				}
			}
			if (!belumLulus.isEmpty()) {
				MyMessageboxConfig.show("Untuk dapat mendaftar sidang, mahasiswa \"" + mahasiswa.toString()
						+ "\" harus SUDAH LULUS matakuliah prasyarat berikut yang BELUM lulus: "
						+ belumLulus.toString().replace("[", "").replace("]", "") + ".", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (Common.bolehKonfigurasi("file_pdf_dan_cover_skripsi_wajib_diupload")) {

			if (skripsi != null && skripsi.getId() != null) {
				Jurusan jurusan = mahasiswa.getJurusan();
				String defaultD = "Skripsi";
				if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
					defaultD = "Thesis";
				}
				if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
					defaultD = "Disertasi";
				}
				if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
					defaultD = "Tugas Akhir";
				}
				String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
						.getNilai();

				LampiranLain lamPdf = LampiranLain.ambil(skripsi.getId(), LampiranLain.SKRIPSI);

				LampiranLain lamCover = LampiranLain.ambil(skripsi.getId(), LampiranLain.COVER_SKRIPSI);

				if (lamPdf == null || !lamPdf.getNama().toLowerCase().endsWith("pdf")) {
					MyMessageboxConfig.show(
							"File " + label_skripsi.replaceAll(";", " atau ")
									+ " harus diupload, file harus berupa file PDF",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}

				if (lamCover == null || !lamCover.getNama().toLowerCase().endsWith("jpg")) {
					MyMessageboxConfig.show(
							"Cover " + label_skripsi.replaceAll(";", " atau ")
									+ " harus diupload, cover harus berupa file JPG",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}

			} else {

				Jurusan jurusan = mahasiswa.getJurusan();
				String defaultD = "Skripsi";
				if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
					defaultD = "Thesis";
				}
				if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
					defaultD = "Disertasi";
				}
				if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
					defaultD = "Tugas Akhir";
				}
				String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
						.getNilai();

				if (lainMahasiswa == null || !lainMahasiswa.getNama().toLowerCase().endsWith("pdf")) {
					MyMessageboxConfig.show(
							"File " + label_skripsi.replaceAll(";", " atau ")
									+ " harus diisi (PDF), file harus berupa file PDF",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
				if (lainMahasiswaCover == null || !lainMahasiswaCover.getNama().toLowerCase().endsWith("jpg")) {
					MyMessageboxConfig.show(
							"Cover " + label_skripsi.replaceAll(";", " atau ")
									+ " harus diisi, cover harus berupa file JPG",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran1Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran1");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran1() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran1 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran1() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran2Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran2");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran2() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran2 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran2() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran3Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran3");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran3() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran3 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran3() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran4Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran4");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran4() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran4 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran4() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran5Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran5");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran5() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran5 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran5() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran6Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran6");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran6() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran6 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran6() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran7Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran7");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran7() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran7 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran7() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran8Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran8");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran8() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran8 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran8() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran9Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran9");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran9() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran9 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran9() + " wajib diupload !", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran10Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran10");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran10() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran10 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran10() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran11Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran11");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran11() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran11 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran11() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran12Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran12");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran12() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran12 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran12() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran13Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran13");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran13() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran13 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran13() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran14Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran14");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran14() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran14 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran14() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran15Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran15");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran15() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran15 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran15() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran16Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran16");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran16() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran16 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran16() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran17Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran17");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran17() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran17 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran17() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran18Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran18");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran18() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran18 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran18() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran19Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran19");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran19() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran19 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran19() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiSkripsi.getUploadLampiran20Wajib()) {
			if (skripsi != null && skripsi.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(skripsi.getId(), "rowUploadLampiran20");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran20() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran20 == null) {
					MyMessageboxConfig.show(formatNilaiSkripsi.getUploadLampiran20() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		FormatNilaiSkripsi f = formatNilaiSkripsi;

		if (telahSidang.isChecked()) {
			if (jadwalSidangTugasAkhir.getAttribute("jadwalSidangTugasAkhir") == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Jadwal Sidang",
						"Kolom Jadwal Sidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Jadwal Sidang.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}

			if (tanggalSidang.getValue() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal sidang",
						"Kolom Tanggal sidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Tanggal sidang.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}

			if (waktuSidang.getValue() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu mulai sidang",
						"Kolom Waktu mulai sidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Waktu mulai sidang.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}

			if (waktuSampaiSidang.getValue() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu sampai sidang",
						"Kolom Waktu sampai sidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Waktu sampai sidang.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}
		}

		if (!checkSyaratPembimbingDanPenguji()) {
			return false;
		}

		String sem = ((Integer) semester.getSelectedItem().getValue()) % 2 == 0 ? Perkuliahan.GENAP
				: Perkuliahan.GANJIL;
		if (Skripsi.checkMaksSksDosen((Dosen) pembimbing.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) ketuaSidang.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) penguji1.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) penguji2.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) penguji3.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) pembimbing3.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) penguji4.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (Skripsi.checkMaksSksDosen((Dosen) penguji5.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}

		Detailperkuliahan detailperkuliahan = null;
		Session session = HibernateUtil.currentSession();
		if (!formatNilaiSkripsi.getKodeMatakuliah().trim().isEmpty()) {
			detailperkuliahan = Common.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa,
					(Integer) semester.getSelectedItem().getValue(), formatNilaiSkripsi.getKodeMatakuliah().trim());
			if (detailperkuliahan == null) {
				String mk = "";
				for (String kode : formatNilaiSkripsi.getKodeMatakuliah().split(",")) {
					if (!kode.trim().isEmpty()) {
						Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.projectionList().add(Projections.property("kode"))
										.add(Projections.property("nama")))
								.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
										Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();
						if (nama != null && nama.length > 1) {
							mk += mk.isEmpty() ? (nama[0] + " - " + nama[1]) : " atau " + (nama[0] + " - " + nama[1]);
						}
					}
				}
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum mengambil salah satu dari matakuliah berikut:\n {V3}\nBapak/Ibu diharapkan memastikan salah satu matakuliah tersebut telah diambil terlebih dahulu sebelum melakukan pengajuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
						mahasiswa.getNama(), mk);
				return false;
			}
		}

		if (!formatNilaiSkripsi.getKodeMatakuliahDan().trim().isEmpty()) {
			detailperkuliahan = Common.checkApakahSudahMengambilKrsSeminarSkripsiDan(mahasiswa,
					formatNilaiSkripsi.getKodeMatakuliahDan().trim());
			if (detailperkuliahan == null) {
				String mk = "";
				for (String kode : formatNilaiSkripsi.getKodeMatakuliahDan().split(",")) {
					if (!kode.trim().isEmpty()) {
						Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.projectionList().add(Projections.property("kode"))
										.add(Projections.property("nama")))
								.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
										Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();
						if (nama != null && nama.length > 1) {
							mk += mk.isEmpty() ? (nama[0] + " - " + nama[1]) : " dan " + (nama[0] + " - " + nama[1]);
						}
					}
				}
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum mengambil matakuliah berikut:\n {V3}\nBapak/Ibu diharapkan memastikan seluruh matakuliah tersebut telah diambil terlebih dahulu sebelum melakukan pengajuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
						mahasiswa.getNama(), mk);
				return false;
			}
		}

		if (!formatNilaiSkripsi.getTahunAngkatan().trim().isEmpty()
				&& !formatNilaiSkripsi.getTahunAngkatan().trim().contains(mahasiswa.getTahunangkatan().toString())) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tidak dapat mengambil pengajuan \"{V3}\", dikarenakan pengajuan tersebut hanya diperuntukkan bagi angkatan {V4}.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
					mahasiswa.getNama(), formatNilaiSkripsi.getNama(), formatNilaiSkripsi.getTahunAngkatan());
			return false;
		}

		GelombangPendaftaranSidangTugasAkhir g = (GelombangPendaftaranSidangTugasAkhir) gelombangPendaftaranSidangTugasAkhir
				.getSelectedItem().getValue();

		if (skripsi.getGelombangPendaftaranSidangTugasAkhir() == null
				|| !g.getId().equals(skripsi.getGelombangPendaftaranSidangTugasAkhir().getId())) {
			int jumlah = ((Number) session.createCriteria(Skripsi.class)
					.add(Restrictions.eq("gelombangPendaftaranSidangTugasAkhir", g))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (g.getKuota() <= jumlah) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tidak dapat mendaftar pada gelombang \"{V3}\" dikarenakan kuota gelombang tersebut telah terpenuhi. Bapak/Ibu diharapkan memilih gelombang pendaftaran lain yang masih tersedia.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
						mahasiswa.getNama(), g.getNama());
				return false;
			}
		}

		if (mahasiswa != null) {
			String kodeItemBiaya = formatNilaiSkripsi.getKodeItemBiaya();
			if (!kodeItemBiaya.trim().isEmpty()) {

				for (String kode : kodeItemBiaya.trim().split(",")) {
					ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
							.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
					if (itemBiaya != null) {

						Double jumlah = mahasiswa.hitungTotalCicilanPembayaran(krsMahasiswa.getSemester(),
								formatNilaiSkripsi.getSekaliBayar(), null, kode);
						if (jumlah == 0) {

							if (!Common.checkBaypassStatusPembayaranMahasiswa(krsMahasiswa.getSemester(), null,
									mahasiswa, new HashSet<JenisKegiatan>())) {

								MyMessageboxConfig.showFormat(
										"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum melunasi biaya {V3} - {V4}. Untuk dapat melanjutkan pengajuan, Bapak/Ibu diharapkan menghubungi bagian keuangan terlebih dahulu guna menyelesaikan pembayaran biaya tersebut.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										mahasiswa.getNim(), mahasiswa.getNama(), itemBiaya.getKode(), itemBiaya.getNama());
								return false;
							}
						}
					}
				}

			}
		}

		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) session
				.createCriteria(MahasiswaRequestTugasAkhir.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
						Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS),
								Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
										Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS)))))
				.createAlias("formatNilaiProposalSkripsi", "formatNilaiProposalSkripsi")
				.add(Restrictions.or(Restrictions.isNull("formatNilaiProposalSkripsi.formatNilaiSkripsi"),
						Restrictions.eq("formatNilaiProposalSkripsi.formatNilaiSkripsi", formatNilaiSkripsi)))
				.setMaxResults(1).uniqueResult();

		if (skripsi.getId() != null) {
			skripsi = (Skripsi) session.load(Skripsi.class, skripsi.getId());
		}
		skripsi.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		skripsi.setSemester((Integer) this.semester.getSelectedItem().getValue());
		skripsi.setSetujuiSidang(setujuiSidang.isChecked());
		if (skripsi.getDetailperkuliahan() == null) {
			skripsi.setDetailperkuliahan(detailperkuliahan);
		}
		skripsi.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
		skripsi.setJadwalSidangTugasAkhir(
				(JadwalSidangTugasAkhir) jadwalSidangTugasAkhir.getAttribute("jadwalSidangTugasAkhir"));
		skripsi.setWaktuSampaiSidang(waktuSampaiSidang == null || waktuSampaiSidang.getValue() == null ? null
				: Common.timeFormat.get().format(waktuSampaiSidang.getValue()));
		skripsi.setWaktuSidang(waktuSidang == null || waktuSidang.getValue() == null ? null
				: Common.timeFormat.get().format(waktuSidang.getValue()));
		skripsi.setJudul(judulCK.getValue());
		skripsi.setJudulen(judulEn.getValue());
		skripsi.setKeyword(keyword.getValue().trim());

		skripsi.setAbstrack(abstrack.getValue());
		skripsi.setMahasiswa(mahasiswa);
		skripsi.setKetuaSidang((Dosen) ketuaSidang.getAttribute("myValue"));
		skripsi.setPembimbing((Dosen) pembimbing.getAttribute("myValue"));
		skripsi.setPembimbing3((Dosen) pembimbing3.getAttribute("myValue"));
		skripsi.setPenguji1((Dosen) penguji1.getAttribute("myValue"));
		skripsi.setPenguji2((Dosen) penguji2.getAttribute("myValue"));
		skripsi.setPenguji3((Dosen) penguji3.getAttribute("myValue"));
		skripsi.setPenguji4((Dosen) penguji4.getAttribute("myValue"));
		skripsi.setPenguji5((Dosen) penguji5.getAttribute("myValue"));
		sinkronkanDosenDariFormKeSkripsi();
		skripsi.setTanggalSidang(tanggalSidang.getValue());
		skripsi.setTelahSidang(telahSidang.isChecked() ? 1 : 0);
		skripsi.setLokasiUjian(lokasiUjian.getValue());
		// skripsi.setNilaikomprehensif(nilaikomprehensif.getValue());

		skripsi.setTglSk(tglSk.getValue());
		skripsi.setNomorSk(nomorSk.getValue());

		skripsi.setAwalBimbingan(awalBimbingan.getValue());
		skripsi.setAkhirBimbingan(akhirBimbingan.getValue());
		skripsi.setLulusToefl(lulusToefl.isChecked());
		skripsi.setLulusToafl(lulusToafl.isChecked());

		skripsi.setFormatNilaiSkripsi(f);
		skripsi.setGelombangPendaftaranSidangTugasAkhir(g);
		skripsi.setPersetujuanPembimbing1(persetujuanPembimbing1.isChecked());
		skripsi.setPersetujuanPembimbing2(persetujuanPembimbing2.isChecked());
		skripsi.setPersetujuanPembimbing3(persetujuanPembimbing3.isChecked());

		skripsi.setPersetujuanPenguji1(persetujuanPenguji1.isChecked());
		skripsi.setPersetujuanPenguji2(persetujuanPenguji2.isChecked());
		skripsi.setPersetujuanPenguji3(persetujuanPenguji3.isChecked());
		skripsi.setPersetujuanPenguji4(persetujuanPenguji4.isChecked());
		skripsi.setPersetujuanPenguji5(persetujuanPenguji5.isChecked());

		skripsi.setReferensi(referensis == null ? null : referensis.toString());
		skripsi.setFeeder(feeder.getValue().trim());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			skripsi.setDisposisiSop(disposisiSop);
		}

		if (skripsi.getId() != null) {
			session.update(skripsi);
		} else {
			session.save(skripsi);
		}

		session.flush();

		if (mahasiswaRequestTugasAkhir != null && skripsi.getTelahSidang().equals(1)) {
			mahasiswaRequestTugasAkhir.setStatus(MahasiswaRequestTugasAkhir.LULUS_STATUS);
		} else if (mahasiswaRequestTugasAkhir != null && skripsi.getTelahSidang().equals(0)) {
			mahasiswaRequestTugasAkhir.setStatus(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mahasiswa = skripsi.getMahasiswa();

				Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
				Matakuliah matakuliah = detailperkuliahan == null ? null
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();

				NilaiHuruf nilaiHuruf = skripsi.getDetailperkuliahan() == null
						? Common.getNilaiHuruf(skripsi.getTotalNilai(), mahasiswa.getTahunangkatan(),
								mahasiswa.getJurusan(), mahasiswa.getJurusan().getFakultas(),
								Common.getCurrentTahunAkademik(),
								Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf())
						: Common.getNilaiHuruf(skripsi.getTotalNilai(), mahasiswa.getTahunangkatan(),
								mahasiswa.getJurusan(), mahasiswa.getJurusan().getFakultas(),
								skripsi.getDetailperkuliahan().getTahunAkademik(),
								skripsi.getDetailperkuliahan().getSemester() % 2 == 0 ? Perkuliahan.GENAP
										: Perkuliahan.GANJIL,
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				insertNilai(skripsi.getDetailperkuliahan(), mahasiswa, nilaiHuruf, skripsi.getTotalNilai());

				if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa);
						lainMahasiswa.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswaCover != null && lainMahasiswaCover.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaCover);
						lainMahasiswaCover.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaCover);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswaPresentasi != null && lainMahasiswaPresentasi.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaPresentasi);
						lainMahasiswaPresentasi.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaPresentasi);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswaUploadLampiran1 != null && lainMahasiswaUploadLampiran1.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran1);
						lainMahasiswaUploadLampiran1.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran1);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran2 != null && lainMahasiswaUploadLampiran2.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran2);
						lainMahasiswaUploadLampiran2.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran2);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran2 != null && lainMahasiswaUploadLampiran2.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran2);
						lainMahasiswaUploadLampiran2.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran2);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran3 != null && lainMahasiswaUploadLampiran3.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran3);
						lainMahasiswaUploadLampiran3.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran3);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran4 != null && lainMahasiswaUploadLampiran4.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran4);
						lainMahasiswaUploadLampiran4.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran4);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran5 != null && lainMahasiswaUploadLampiran5.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran5);
						lainMahasiswaUploadLampiran5.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran5);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran6 != null && lainMahasiswaUploadLampiran6.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran6);
						lainMahasiswaUploadLampiran6.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran6);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran7 != null && lainMahasiswaUploadLampiran7.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran7);
						lainMahasiswaUploadLampiran7.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran7);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran8 != null && lainMahasiswaUploadLampiran8.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran8);
						lainMahasiswaUploadLampiran8.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran8);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran9 != null && lainMahasiswaUploadLampiran9.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran9);
						lainMahasiswaUploadLampiran9.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran9);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran10 != null && lainMahasiswaUploadLampiran10.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran10);
						lainMahasiswaUploadLampiran10.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran10);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran11 != null && lainMahasiswaUploadLampiran11.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran11);
						lainMahasiswaUploadLampiran11.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran11);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran12 != null && lainMahasiswaUploadLampiran12.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran12);
						lainMahasiswaUploadLampiran12.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran12);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran13 != null && lainMahasiswaUploadLampiran13.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran13);
						lainMahasiswaUploadLampiran13.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran13);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran14 != null && lainMahasiswaUploadLampiran14.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran14);
						lainMahasiswaUploadLampiran14.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran14);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran15 != null && lainMahasiswaUploadLampiran15.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran15);
						lainMahasiswaUploadLampiran15.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran15);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran16 != null && lainMahasiswaUploadLampiran16.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran16);
						lainMahasiswaUploadLampiran16.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran16);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran17 != null && lainMahasiswaUploadLampiran17.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran17);
						lainMahasiswaUploadLampiran17.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran17);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran18 != null && lainMahasiswaUploadLampiran18.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran18);
						lainMahasiswaUploadLampiran18.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran18);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran19 != null && lainMahasiswaUploadLampiran19.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran19);
						lainMahasiswaUploadLampiran19.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran19);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (lainMahasiswaUploadLampiran20 != null && lainMahasiswaUploadLampiran20.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswaUploadLampiran20);
						lainMahasiswaUploadLampiran20.setRef(skripsi.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswaUploadLampiran20);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
				}

				try {
					if (awalBimbingan != null && statusKeluar != null) {

						HibernateUtil.currentSession().refresh(mahasiswa);
						// Skripsi-specific (di luar form kelulusan generik):
						mahasiswa.setBlnAwalBimbingan(awalBimbingan.getValue());
						if (akhirBimbingan != null) {
							mahasiswa.setBlnAkhirBimbingan(akhirBimbingan.getValue());
						}
						mahasiswa.setJudulSkripsi(judulCK.getValue().trim());

						// Seluruh field "Informasi Kelulusan" dipetakan lewat FormKelulusanHelper agar SAMA
						// persis dengan MahasiswaAction (termasuk Email Atasan, Non Aktif, Tanggal Wisuda, dan
						// Nomor SK Drop Out yang sebelumnya belum tersimpan dari halaman Skripsi).
						ais.action.master.helper.FormKelulusanHelper.terapkan(kelulusanKomponen, mahasiswa, biodataMahasiswa);

						if (tanggallahirManual != null)
							mahasiswa.setTanggallahirManual(tanggallahirManual.getValue());

						Common.refreshSaveOrUpdate(mahasiswa);

						// Simpan biodata (Nama untuk Ijazah + Email Atasan dari helper) bila biodata tersedia.
						if (biodataMahasiswa != null) {
							if (namaUntukIjazah != null) {
								biodataMahasiswa.setNamaUntukIjazah(namaUntukIjazah.getValue());
							}
							Common.refreshUpdate(biodataMahasiswa);
						}

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LibraryUtil.checkSkripsiForItem(skripsi, false, tbmuser);
					}
				});

			}
		});

		if (eventListener != null) {
			eventListener.onEvent(new Event("", null, skripsi));
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		if (tbmuser.getMahasiswa() != null) {
			searchnim.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setValue(tbmuser.getMahasiswa().getNama());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);

		}

		Dosen dosenPemimbing = (Dosen) searchdosenPemimbing.getAttribute("myValue");

		Criterion criterion = Restrictions.eq("pembimbing", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("ketuaSidang", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", dosenPemimbing));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Skripsi.class)

				.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNotNull("feeder"), Restrictions.ne("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.add(searchTahunAkademik.getSelectedItem() == null
						|| searchTahunAkademik.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAkademik.getSelectedItem().getValue()))

				.add(searchSemesterAbsensi.getSelectedItem() == null
						|| searchSemesterAbsensi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction("this_.semester%2="
										+ (searchSemesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GANJIL)
												? "1"
												: "0")))

				.add(tbmuser.getMahasiswa() != null ? Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())
						: Restrictions.sqlRestriction("true"))
				.add(dosenPemimbing != null ? criterion : Restrictions.sqlRestriction("true"))
				.add(searchsidang.getSelectedItem() == null || searchsidang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("telahSidang", searchsidang.getSelectedItem().getValue()));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("judul", searchjudul.getValue(), MatchMode.ANYWHERE))

				.createAlias("gelombangPendaftaranSidangTugasAkhir", "gelombangPendaftaranSidangTugasAkhir",
						Criteria.LEFT_JOIN)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("gelombangPendaftaranSidangTugasAkhir.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE))

				.createCriteria("mahasiswa")

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchnim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (eventListener != null || searchjurusan == null) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event aa) throws Exception {
				Common.initPaging(initCriteria(false), paging);

				skripsis = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(skripsis);
				grid.setRowRenderer(new SkripsiRenderer());
				grid.setModelCheckMobile(strset);

			}
		});

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onPrint(Event event) throws Exception {
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		for (Skripsi skripsi : skripsis) {
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
			map.put("no_pokok", skripsi.getMahasiswa().getNim());
			map.put("nama", skripsi.getMahasiswa().getNama());
			map.put("judul", skripsi.getJudul());
			map.put("pembimbing", skripsi.getPembimbing().getNama());
			map.put("ketua", skripsi.getKetuaSidang() == null ? null : skripsi.getKetuaSidang().getNama());
			map.put("penguji1", skripsi.getPenguji1() == null ? null : skripsi.getPenguji1().getNama());
			map.put("penguji2", skripsi.getPenguji2() == null ? null : skripsi.getPenguji2().getNama());
			map.put("nilai", skripsi.getTotalNilai());
			map.put("huruf", skripsi.getNilaiHuruf());

			maps.add(map);

		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
			parameters.put("fakultas", "");
		} else if (searchfakultas.getSelectedItem() != null) {
			// System.out.println("searchfakultas");

			Fakultas fakultas = (Fakultas) searchfakultas.getSelectedItem().getValue();
			parameters.put("fakultas", "FAKULTAS " + fakultas.getNama().toUpperCase());
		}
		Report.generatePDFReport("pdf", parameters,
				searchsidang.getSelectedItem() == null || searchsidang.getSelectedItem().getValue() == null
						|| searchsidang.getSelectedItem().getValue().equals(0)
						? "Data_Skripsi_Mahasiswa_belum_terjadwal"
						: "Data_Skripsi_Mahasiswa",
				ais.ui.util.WaktuUtil.getDate(), maps);
	}

	public void onPrintJadwal(Event event) throws Exception {

		if (onSave(event)) {
			Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
			parameters.put("fakultas", skripsi.getMahasiswa().getJurusan().getFakultas().getNama());
			parameters.put("mahasiswa", skripsi.getMahasiswa().getId());
			parameters.put("waktu_sidang", waktuSidang.getValue() + "-" + waktuSampaiSidang.getValue());
			parameters.put("ruang_sidang", skripsi.getRuangSidang().getNama());
			parameters.put("tanggal_sidang", Common.dateFormat2.get().format(skripsi.getTanggalSidang()));
			Report.generatePDFReport("pdf", parameters, "Jadwal_Sidang_Skripsi", ais.ui.util.WaktuUtil.getDate());
		}

	}

	private void kirimNilaiSkripsiKeFeeder(FeederExporter feederImporter, Skripsi skripsi, List<String> errorLog) {
		if (feederImporter == null || skripsi == null) {
			return;
		}

		Mahasiswa mahasiswa = skripsi.getMahasiswa();
		String identitas = mahasiswa == null ? String.valueOf(skripsi)
				: (mahasiswa.getNim() + " " + mahasiswa.getNama());
		if (mahasiswa == null) {
			if (errorLog != null) {
				errorLog.add("[" + identitas + "] Nilai skripsi tidak dikirim karena data mahasiswa kosong.");
			}
			return;
		}

		Double nilai = skripsi.getTotalNilai();
		if (nilai == null || nilai <= 0.1) {
			if (errorLog != null) {
				errorLog.add("[" + identitas
						+ "] Nilai skripsi tidak dikirim karena total nilai ujian skripsi masih kosong/0.");
			}
			return;
		}

		Detailperkuliahan detailperkuliahan = pastikanDetailperkuliahanSkripsi(skripsi);
		if (detailperkuliahan == null) {
			if (errorLog != null) {
				errorLog.add("[" + identitas
						+ "] Nilai skripsi belum dapat dikirim ke Neo Feeder karena matakuliah/KRS skripsi belum ditemukan. Periksa Format Nilai Skripsi pada Kode Matakuliah atau Kode Item Biaya, lalu pastikan mahasiswa sudah mengambil KRS matakuliah tersebut.");
			}
			return;
		}

		NilaiHuruf nilaiHuruf = hitungNilaiHurufSkripsi(skripsi, detailperkuliahan);
		insertNilai(detailperkuliahan, mahasiswa, nilaiHuruf, nilai);
		feederImporter.nilai(detailperkuliahan, errorLog);
	}

	private Detailperkuliahan pastikanDetailperkuliahanSkripsi(Skripsi skripsi) {
		if (skripsi == null || skripsi.getMahasiswa() == null) {
			return null;
		}

		Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
		if (detailperkuliahan != null) {
			return detailperkuliahan;
		}

		FormatNilaiSkripsi formatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();
		if (formatNilaiSkripsi == null) {
			return null;
		}

		detailperkuliahan = cariDetailperkuliahanSkripsi(skripsi, formatNilaiSkripsi.getKodeMatakuliah());
		if (detailperkuliahan == null) {
			detailperkuliahan = cariDetailperkuliahanSkripsi(skripsi, formatNilaiSkripsi.getKodeItemBiaya());
		}
		if (detailperkuliahan != null) {
			skripsi.setDetailperkuliahan(detailperkuliahan);
			try {
				Common.refreshUpdate(skripsi);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return detailperkuliahan;
	}

	private Detailperkuliahan cariDetailperkuliahanSkripsi(Skripsi skripsi, String kode) {
		if (skripsi == null || kode == null || kode.trim().isEmpty()) {
			return null;
		}
		return Common.checkApakahSudahMengambilKrsSeminarSkripsi(skripsi.getMahasiswa(), skripsi.getSemester(),
				kode.trim());
	}

	private NilaiHuruf hitungNilaiHurufSkripsi(Skripsi skripsi, Detailperkuliahan detailperkuliahan) {
		if (skripsi == null || skripsi.getMahasiswa() == null) {
			return null;
		}
		Mahasiswa mahasiswa = skripsi.getMahasiswa();
		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
		JenisNilaiHurufMatakuliah jenisNilaiHuruf = matakuliah == null ? null : matakuliah.getJenisNilaiHuruf();
		if (skripsi.getFormatNilaiSkripsi() != null && skripsi.getFormatNilaiSkripsi().getJenisNilaiHuruf() != null) {
			jenisNilaiHuruf = skripsi.getFormatNilaiSkripsi().getJenisNilaiHuruf();
		}
		String tahunAkademik = detailperkuliahan == null ? Common.getCurrentTahunAkademik()
				: detailperkuliahan.getTahunAkademik();
		String ganjilGenap = detailperkuliahan == null
				? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
				: detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getGanjilGenap();
		return Common.getNilaiHuruf(skripsi.getTotalNilai(), mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
				mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getFakultas(), tahunAkademik,
				ganjilGenap, matakuliah == null ? "" : matakuliah.getKode(), jenisNilaiHuruf);
	}

	@SuppressWarnings("unchecked")
	public void insertNilai(Detailperkuliahan dpSkripsi, Mahasiswa mahasiswa, NilaiHuruf nilaiHuruf, Double nilai) {
		try {

			Session session = HibernateUtil.currentNativeSession();
			if (dpSkripsi != null && dpSkripsi.getPerkuliahan() != null) {

				List<FormatNilai> formatNilais = Common.getFormatNilais(session, dpSkripsi.getPerkuliahan());
				for (FormatNilai formatNilai : formatNilais) {
					dpSkripsi.populateDetailNilai(formatNilai, null, nilai, true, tbmuser);
				}

				dpSkripsi.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
				dpSkripsi.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
				dpSkripsi.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
				dpSkripsi.setTotalNilai(nilai);

				session.getTransaction().begin();
				Common.refreshUpdate(session, dpSkripsi);
				session.getTransaction().commit();
			} else {

				String label_skripsi = Common.getKonfigurasi("label_skripsi", "skripsi").getNilai();

				String[] skripsis = label_skripsi.split(";");
				Criterion criterion = Restrictions.ilike("nama", label_skripsi, MatchMode.EXACT);
				for (String s : skripsis) {
					criterion = Restrictions.or(criterion, Restrictions.ilike("nama", s, MatchMode.EXACT));
				}

				List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)).createCriteria("perkuliahan")
						.createCriteria("matakuliah").add(criterion).list();

				for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
					List<FormatNilai> formatNilais = Common.getFormatNilais(session,
							detailperkuliahan.getPerkuliahan());
					for (FormatNilai formatNilai : formatNilais) {
						detailperkuliahan.populateDetailNilai(formatNilai, null, nilai, true, tbmuser);
					}
					detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
					detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
					detailperkuliahan.setTotalNilai(nilai);

					Matakuliah matakuliah = detailperkuliahan == null ? null
							: detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: detailperkuliahan.getMatakuliahKonversi();

					Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
					nilaiHuruf = Common.getNilaiHuruf(totalSementara,
							detailperkuliahan.getMahasiswa().getTahunangkatan(),
							detailperkuliahan.getMahasiswa().getJurusan(),
							detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
							detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

					detailperkuliahan.setTotalNilaiSementara(totalSementara);
					detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

					session.getTransaction().begin();
					Common.refreshUpdate(session, detailperkuliahan);
					session.getTransaction().commit();
				}

				List<Detailperkuliahan> skripsiKonversis = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.createCriteria("matakuliahKonversi").add(criterion).list();
				for (Detailperkuliahan detailperkuliahan : skripsiKonversis) {
					detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
					detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setTotalNilai(nilai);

					session.getTransaction().begin();
					Common.refreshUpdate(session, detailperkuliahan);
					session.getTransaction().commit();
				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();
	}

	public static Vbox tampilkanJudul(Skripsi skripsi) throws Exception {
		Vbox vbox = new Vbox();

		RevisiHelper.createNewRevisi(Skripsi.class, skripsi, skripsi.getJudul()).setParent(vbox);
		vbox.appendChild(new MyLabelKecil(skripsi.getJudulen()));
//		vbox.appendChild(new MyLabelKecil(skripsi.getFeeder() == null ? "" : skripsi.getFeeder()));
		new MyLabelAgakKecilBold(Common.getBahasaConfig("Kata Kunci") + " : " + skripsi.getKeyword()).setParent(vbox);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null) {

			Jurusan jurusan = skripsi.getMahasiswa().getJurusan();
			String defaultD = "Skripsi";
			if (ConstantValues.s2 != null && jurusan.getJenjang().getId().equals(ConstantValues.s2.getId())) {
				defaultD = "Thesis";
			}
			if (ConstantValues.s3 != null && jurusan.getJenjang().getId().equals(ConstantValues.s3.getId())) {
				defaultD = "Disertasi";
			}
			if (ConstantValues.d3 != null && jurusan.getJenjang().getId().equals(ConstantValues.d3.getId())) {
				defaultD = "Tugas Akhir";
			}
			String label_skripsi = Common.getKonfigurasi("label_skripsi_" + jurusan.getJenjang().getId(), defaultD)
					.getNilai();

			Hbox myvbox = new Hbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), LampiranLain.SKRIPSI, label_skripsi, true,
					null, null, false, false, false, false);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), Skripsi.class.getName() + "_Presentasi",
					"Presentasi", false, null, null, false, false, false, false);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), LampiranLain.COVER_SKRIPSI, "Cover", false,
					null, null, false, false, false, false);
		}

		return vbox;
	}

	public static Label tampilkanInfoDosenSimple(Skripsi skripsi) throws Exception {
		FormatNilaiSkripsi formatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();
		if (formatNilaiSkripsi == null) {
			return new Label();
		}
		String data = "";
		if (skripsi.getPembimbing() != null) {
			data += data.isEmpty() ? skripsi.getPembimbing().getNama() + " (" + formatNilaiSkripsi.getDosen1() + ")"
					: "," + skripsi.getPembimbing().getNama() + " (" + formatNilaiSkripsi.getDosen1() + ")"
							+ (skripsi.getPersetujuanPembimbing1() ? " (Telah setuju))" : " (Belum setuju)");
		}
		if (skripsi.getKetuaSidang() != null) {
			data += data.isEmpty() ? skripsi.getKetuaSidang().getNama() + " (" + formatNilaiSkripsi.getDosen2() + ")"
					: "," + skripsi.getKetuaSidang().getNama() + " (" + formatNilaiSkripsi.getDosen2() + ")"
							+ (skripsi.getPersetujuanPembimbing2() ? " (Telah setuju))" : " (Belum setuju)");
		}
		if (skripsi.getPenguji1() != null) {
			data += data.isEmpty() ? skripsi.getPenguji1().getNama() + " (" + formatNilaiSkripsi.getDosen3() + ")"
					: "," + skripsi.getPenguji1().getNama() + " (" + formatNilaiSkripsi.getDosen3() + ")"
							+ (skripsi.getPersetujuanPenguji1() ? " (Telah setuju))" : " (Belum setuju)");
		}
		if (skripsi.getPenguji2() != null) {
			data += data.isEmpty() ? skripsi.getPenguji2().getNama() + " (" + formatNilaiSkripsi.getDosen4() + ")"
					: "," + skripsi.getPenguji2().getNama() + " (" + formatNilaiSkripsi.getDosen4() + ")"
							+ (skripsi.getPersetujuanPenguji2() ? " (Telah setuju))" : " (Belum setuju)");
		}
		if (skripsi.getPenguji3() != null) {
			data += data.isEmpty() ? skripsi.getPenguji3().getNama() + " (" + formatNilaiSkripsi.getDosen5() + ")"
					: "," + skripsi.getPenguji3().getNama() + " (" + formatNilaiSkripsi.getDosen5() + ")"
							+ (skripsi.getPersetujuanPenguji3() ? " (Telah setuju))" : " (Belum setuju)");
		}
		if (skripsi.getPenguji4() != null) {
			data += data.isEmpty() ? skripsi.getPenguji4().getNama() + " (" + formatNilaiSkripsi.getDosen6() + ")"
					: "," + skripsi.getPenguji4().getNama() + " (" + formatNilaiSkripsi.getDosen6() + ")"
							+ (skripsi.getPersetujuanPenguji4() ? " (Telah setuju))" : " (Belum setuju)");
		}

		return new MyLabelKecil(data);
	}

	public static Vbox tampilkanInfoDosen(final Skripsi skripsi, boolean tampilkanAsesor, boolean rinci)
			throws Exception {
		List<CommonVO> dataDosen = skripsi.dataDosen(true);
		return tampilkanInfoDosen(skripsi, tampilkanAsesor, rinci, dataDosen);
	}

	public static Vbox tampilkanInfoDosen(final Skripsi skripsi, boolean tampilkanAsesor, boolean rinci,
			List<CommonVO> dataDosen) throws Exception {

		FormatNilaiSkripsi formatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();
		if (formatNilaiSkripsi == null) {
			return new Vbox();
		}

		Vbox vbox = new Vbox();

		if (rinci) {
			tampilkanJudul(skripsi).setParent(vbox);
		}

		int tampilPerRow = Common.isMobile() ? 2 : 6;
		Tbmuser tbmuser = Common.getCurrentUser();
		Hbox hboxBaru = new Hbox();
		hboxBaru.setParent(vbox);
		int size = 0;

		if (skripsi.getSetujuiSidang() && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null) {

			for (CommonVO commonVO : dataDosen) {
				Dosen dosen = (Dosen) commonVO.getValueObject();
				final String key = commonVO.getName();
				if (!rinci && size % tampilPerRow == 0) {
					hboxBaru = new Hbox();
					hboxBaru.setParent(vbox);
				}
				size++;
				Vbox myvbox = new Vbox();
				myvbox.setParent(hboxBaru);

				if (rinci) {

					final Vbox baruvbox = new Vbox();
					baruvbox.setParent(myvbox);

					final Hbox baruHbox = new Hbox();
					baruHbox.setParent(myvbox);

					baruHbox.appendChild(new MyLabelKecil(key + " : "));

					final AmbilDataDosenBanbox dosenBanbox = new AmbilDataDosenBanbox();
					dosenBanbox.setParent(baruHbox);
					dosenBanbox.setCols(8);
					dosenBanbox.setValue(dosen == null ? "" : dosen.getNama());
					dosenBanbox.setReadonly(true);
					dosenBanbox.setAttribute("dosen", dosen);
					dosenBanbox.setAttribute("myValue", dosen);

					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.clear(baruvbox);
							Dosen dosen = (Dosen) dosenBanbox.getAttribute("dosen");
							if (dosen != null) {
								CommonMedia.tampilkanGambarKecil(dosen).setParent(baruvbox);
								baruvbox.appendChild(tombolCetakSK(skripsi, dosen, key));
							}
							skripsi.simpanDosen(dosen, key);
							if (arg0 != null && arg0.getTarget() != null) {
								Common.refreshUpdate(skripsi);
							}
						}
					};
					dosenBanbox.setEventListener(eventListener);
					eventListener.onEvent(null);
				} else {
					CommonMedia.tampilkanGambarKecil(dosen).setParent(myvbox);
				}
			}

		} else {
			TreeMap<String, Dosen> treeMap = skripsi.populateDosen();
			for (String key : treeMap.keySet()) {
				Dosen dosen = treeMap.get(key);
				if (dosen == null) {
					continue;
				}
				if (!rinci && size % tampilPerRow == 0) {
					hboxBaru = new Hbox();
					hboxBaru.setParent(vbox);
				}
				size++;
				Vbox myvbox = new Vbox();
				myvbox.setParent(hboxBaru);
				CommonMedia.tampilkanGambarKecil(dosen).setParent(myvbox);
				if (rinci) {
					new MyLabelKecil(key + " : " + dosen.getNama()).setParent(myvbox);
					tombolCetakSK(skripsi, dosen, key).setParent(myvbox);

					if (key.equals(formatNilaiSkripsi.getDosen1())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPembimbing1()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan1Label = skripsi.getPersetujuanPembimbing1()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPembimbing1 = new Checkbox("Persetujuan Pembimbing I");
						persetujuanPembimbing1.setChecked(skripsi.getPersetujuanPembimbing1());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPembimbing() != null
								&& skripsi.getPembimbing().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPembimbing1);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan1Label);
						}
						persetujuanPembimbing1.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPembimbing1(persetujuanPembimbing1.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals(formatNilaiSkripsi.getDosen2())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPembimbing2()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan2Label = skripsi.getPersetujuanPembimbing2()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPembimbing2 = new Checkbox("Persetujuan Pembimbing II");
						persetujuanPembimbing2.setChecked(skripsi.getPersetujuanPembimbing2());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getKetuaSidang() != null
								&& skripsi.getKetuaSidang().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPembimbing2);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan2Label);
						}
						persetujuanPembimbing2.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPembimbing2(persetujuanPembimbing2.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals("Pembimbing III")) {
						final Image imgPembimbing = (skripsi.getPersetujuanPembimbing3()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan3Label = skripsi.getPersetujuanPembimbing3()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPembimbing3 = new Checkbox("Persetujuan Pembimbing III");
						persetujuanPembimbing3.setChecked(skripsi.getPersetujuanPembimbing3());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPembimbing3() != null
								&& skripsi.getPembimbing3().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPembimbing3);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan3Label);
						}
						persetujuanPembimbing3.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPembimbing3(persetujuanPembimbing3.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals(formatNilaiSkripsi.getDosen3())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPenguji1()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan1Label = skripsi.getPersetujuanPenguji1()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPenguji1 = new Checkbox("Persetujuan Penguji I");
						persetujuanPenguji1.setChecked(skripsi.getPersetujuanPenguji1());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPenguji1() != null
								&& skripsi.getPenguji1().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPenguji1);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan1Label);
						}
						persetujuanPenguji1.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPenguji1(persetujuanPenguji1.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals(formatNilaiSkripsi.getDosen4())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPenguji2()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan2Label = skripsi.getPersetujuanPenguji2()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPenguji2 = new Checkbox("Persetujuan Penguji II");
						persetujuanPenguji2.setChecked(skripsi.getPersetujuanPenguji2());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPenguji2() != null
								&& skripsi.getPenguji2().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPenguji2);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan2Label);
						}
						persetujuanPenguji2.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPenguji2(persetujuanPenguji2.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals(formatNilaiSkripsi.getDosen5())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPenguji3()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan3Label = skripsi.getPersetujuanPenguji3()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPenguji3 = new Checkbox("Persetujuan Penguji III");
						persetujuanPenguji3.setChecked(skripsi.getPersetujuanPenguji3());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPenguji3() != null
								&& skripsi.getPenguji3().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPenguji3);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan3Label);
						}
						persetujuanPenguji3.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPenguji3(persetujuanPenguji3.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					} else if (key.equals(formatNilaiSkripsi.getDosen6())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPenguji4()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan4Label = skripsi.getPersetujuanPenguji4()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPenguji4 = new Checkbox("Persetujuan Penguji IV");
						persetujuanPenguji4.setChecked(skripsi.getPersetujuanPenguji4());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPenguji4() != null
								&& skripsi.getPenguji4().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPenguji4);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan4Label);
						}
						persetujuanPenguji4.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPenguji4(persetujuanPenguji4.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					}

					else if (key.equals(formatNilaiSkripsi.getDosen7())) {
						final Image imgPembimbing = (skripsi.getPersetujuanPenguji5()
								? new Image("/img/Cute-Ball-Go-icon.png")
								: new Image("/img/Button-Delete-icon.png"));
						final MyLabelAgakKecilBold statusPersetujuan5Label = skripsi.getPersetujuanPenguji5()
								? new MyLabelAgakKecilBold("Telah disetujui")
								: new MyLabelAgakKecilBold("Belum mensetujui");

						final Checkbox persetujuanPenguji5 = new Checkbox("Persetujuan Penguji V");
						persetujuanPenguji5.setChecked(skripsi.getPersetujuanPenguji5());
						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& skripsi.getPenguji5() != null
								&& skripsi.getPenguji5().getId().equals(tbmuser.getDosen().getId())) {
							myvbox.appendChild(persetujuanPenguji5);
						} else {
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							hbox.appendChild(imgPembimbing);
							hbox.appendChild(statusPersetujuan5Label);
						}
						persetujuanPenguji5.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								skripsi.setPersetujuanPenguji5(persetujuanPenguji5.isChecked());
								Common.refreshUpdate(skripsi);
							}
						});
					}
				}
			}
		}

		if (skripsi.getTanggal_dirubah() != null) {
			org.zkoss.zul.Label lblUpdate = new org.zkoss.zul.Label(
					"Diperbarui: " + Common.dateFormat.get().format(skripsi.getTanggal_dirubah()));
			lblUpdate.setStyle(
					"font-size:11px;color:#888;font-style:italic;margin-top:6px;display:block;");
			vbox.appendChild(lblUpdate);
		}

		return vbox;
	}

	public static Hbox tampilkanInfoMahasiswa(final Skripsi skripsi, final EventListener eventListener)
			throws Exception {

		Hbox hbox = new Hbox();
		if (skripsi.getMahasiswa() == null) {
			return hbox;
		}
		Mahasiswa mahasiswa = skripsi.getMahasiswa();
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vbox = new Vbox();

		vbox.setParent(hbox);

		new Label(mahasiswa.getNim()).setParent(vbox);
		new Label(mahasiswa.getNama()).setParent(vbox);

		new MyLabelKecil("TA/Smt : " + skripsi.getTahunAkademik() + " / " + skripsi.getSemester()).setParent(vbox);
		new MyLabelKecil("Gelombang/Persetujuan : "
				+ (skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? ""
						: skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama())
				+ " / " + (skripsi.getSetujuiSidang() ? "Ya" : "Tidak")).setParent(vbox);

		if (skripsi.getJadwalSidangTugasAkhir() != null) {
			new MyLabelKecil("Sidang : " + skripsi.getJadwalSidangTugasAkhir().getNama()).setParent(vbox);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			new MyLabelKecil(
					"Masa Bimbingan : " + (skripsi.getAwalBimbingan() == null ? "belum ditentukan"
							: Common.dateFormat6.get().format(skripsi.getAwalBimbingan()) + " s.d "
									+ (skripsi.getAwalBimbingan() == null ? "belum ditentukan"
											: Common.dateFormat6.get().format(skripsi.getAwalBimbingan()))))
					.setParent(vbox);
		} else {
			Hbox hbox2 = new Hbox();
			hbox2.setParent(vbox);
			hbox2.appendChild(new ais.ui.util.MyLabelAgakKecilBold("Masa Bimbingan"));
			final MyDatebox tanggalAwalBimbingan = new MyDatebox(skripsi.getAwalBimbingan());
			tanggalAwalBimbingan.setCols(4);
			hbox2.appendChild(tanggalAwalBimbingan);
			hbox2.appendChild(new ais.ui.util.MyLabelAgakKecilBold("s.d"));
			final MyDatebox tanggalAkhirBimbingan = new MyDatebox(skripsi.getAkhirBimbingan());
			tanggalAkhirBimbingan.setCols(4);
			hbox2.appendChild(tanggalAkhirBimbingan);

			EventListener ubah = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = skripsi.getMahasiswaRequestTugasAkhir();

					if (mahasiswaRequestTugasAkhir != null) {
						mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(tanggalAwalBimbingan.getValue());
						mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(tanggalAkhirBimbingan.getValue());
						Common.refreshUpdate(mahasiswaRequestTugasAkhir);
					}

					skripsi.setAwalBimbingan(tanggalAwalBimbingan.getValue());
					skripsi.setAkhirBimbingan(tanggalAkhirBimbingan.getValue());
					Common.refreshUpdate(skripsi);
				}
			};

			tanggalAwalBimbingan.addEventListener("onChange", ubah);
			tanggalAkhirBimbingan.addEventListener("onChange", ubah);
		}

		if (eventListener != null && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			Hbox hbox2 = new Hbox();
			hbox2.setParent(vbox);
			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Setujui Sidang");
			checkboxConfig.setChecked(skripsi.getSetujuiSidang());
			hbox2.appendChild(checkboxConfig);
			checkboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setSetujuiSidang(checkboxConfig.isChecked());
					Common.refreshUpdate(skripsi);
					Common.createDefaultTimer(eventListener);
				}
			});
		} else {

			if (skripsi.getTelahSidang().equals(1)) {
				new MyLabelAgakKecilBold("Telah sidang").setParent(vbox);
			} else {
				new MyLabelAgakKecilBold("Persetujuan : " + (skripsi.getSetujuiSidang() ? "Ya" : "Belum"))
						.setParent(vbox);
			}
		}

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Ubah Pengajuan", "/img/Document-Write-icon.png");
		toolbarbutton.setStyle("font-size:7px;");
		toolbarbutton.setVisible(tbmuser != null && !skripsi.getSetujuiSidang());
		vbox.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SkripsiAction.onAddExternal(eventListener, skripsi, skripsi.getMahasiswa());
			}
		});

		if (eventListener != null) {
			Hbox hbox2 = new Hbox();
			hbox2.setVisible(tbmuser != null && skripsi.getSetujuiSidang());
			hbox2.setParent(vbox);

			hbox2.appendChild(new ais.ui.util.MyLabelAgakKecilBold("Tanggal Sidang"));
			final MyDatebox tanggalSidang = new MyDatebox(skripsi.getTanggalSidang());

			if (tbmuser.getMahasiswa() != null) {
				hbox2.appendChild(new Label(skripsi.getTanggalSidang() == null ? ""
						: Common.dateFormat4.get().format(skripsi.getTanggalSidang())));
			} else {
				hbox2.appendChild(tanggalSidang);
			}

			tanggalSidang.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setTanggalSidang(tanggalSidang.getValue());
					Common.refreshUpdate(skripsi);
					Common.createDefaultTimer(eventListener);
				}
			});
		}

		return hbox;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {

		this.skripsi = (Skripsi) generalValueObject;
		this.disposisiSop = disposisiSop;

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
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("I. Data Mahasiswa"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa") + " *"));

		mahasiswa = new AmbilDataMahasiswaSkripsiBanbox();

		if (dataMahasiswa != null) {
			skripsi.setMahasiswa(dataMahasiswa);
			mahasiswa.setValue(dataMahasiswa == null ? "" : (dataMahasiswa.getNim() + " - " + dataMahasiswa.getNama()));
			mahasiswa.setId("" + dataMahasiswa == null ? "mhs_-1" : "mhs_" + dataMahasiswa.getId());
			mahasiswa.setAttribute("mahasiswa", dataMahasiswa);
			mahasiswa.setDisabled(true);
		} else {

			mahasiswa.setValue(skripsi.getMahasiswa() == null ? ""
					: (skripsi.getMahasiswa().getNim() + " - " + skripsi.getMahasiswa().getNama()));
			mahasiswa.setId("" + skripsi.getMahasiswa() == null ? "mhs_-1" : "mhs_" + skripsi.getId());
			mahasiswa.setAttribute("mahasiswa", skripsi.getMahasiswa());
		}
		if (persetujuan) {
			row.appendChild(new Label(skripsi.getMahasiswa() == null ? ""
					: skripsi.getMahasiswa().getNim() + " " + skripsi.getMahasiswa().getNama()));
		} else {
			row.appendChild(mahasiswa);
		}
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));

		gelombangPendaftaranSidangTugasAkhir = new Combobox();
		if (persetujuan) {
			row.appendChild(new Label(skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? ""
					: skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama()));
		} else {
			row.appendChild(gelombangPendaftaranSidangTugasAkhir);
		}
		gelombangPendaftaranSidangTugasAkhir.setReadonly(true);
		gelombangPendaftaranSidangTugasAkhir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pengajuan *")));

		if (persetujuan) {
			row.appendChild(new Label(
					skripsi.getFormatNilaiSkripsi() == null ? "" : skripsi.getFormatNilaiSkripsi().getNama()));
		} else {
			row.appendChild(formatNilaiSkripsi);
		}

		formatNilaiSkripsi.setReadonly(true);

		formatNilaiSkripsi.addEventListener("onChange", hasilSidangListener);
		formatNilaiSkripsi.setWidth("90%");

		mhsFormatEvent.onEvent(null);

//		if (skripsi != null && skripsi.getMahasiswaRequestTugasAkhir() != null
//				&& skripsi.getMahasiswaRequestTugasAkhir().getFormatNilaiProposalSkripsi() != null
//				&& skripsi.getMahasiswaRequestTugasAkhir().getFormatNilaiProposalSkripsi()
//						.getFormatNilaiSkripsi() != null) {
//			Common.selectComboItem(true, formatNilaiSkripsi,
//					skripsi.getMahasiswaRequestTugasAkhir().getFormatNilaiProposalSkripsi().getFormatNilaiSkripsi());
////			formatNilaiSkripsi.setDisabled(true);
//		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));

		tahunAkademik = new Combobox();
		if (persetujuan) {
			row.appendChild(new Label(skripsi.getTahunAkademik()));
		} else {
			row.appendChild(tahunAkademik);
		}

		Common.generateTahunAjaranDanSemua(tahunAkademik);
		if (skripsi.getTahunAkademik() != null) {
			Common.selectComboItem(tahunAkademik, skripsi.getTahunAkademik());
		}
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				muatGelombangPendaftaranSidang((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
			}
		});
		muatGelombangPendaftaranSidang((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		semester = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(skripsi.getSemester() + ""));
		} else {
			row.appendChild(semester);
		}

		semester.setReadonly(true);

		for (int i = 0; i <= 30; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		Common.selectComboItem(semester, skripsi.getSemester());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("II. Data Pengajuan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
		judulCK = new Textbox();

		if (persetujuan) {
			row.appendChild(new Label(skripsi.getJudul()));
		} else {
			row.appendChild(judulCK);
		}

		judulCK.setValue(skripsi.getJudul());
		judulCK.setWidth("90%");
		judulCK.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul dalam English"));
		judulEn = new Textbox();
		if (persetujuan) {
			row.appendChild(new Label(skripsi.getJudulen()));
		} else {
			row.appendChild(judulEn);
		}

		judulEn.setValue(skripsi.getJudulen());
		judulEn.setWidth("90%");
		judulEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Abstrak"));
		abstrack = new Textbox();
		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(skripsi.getAbstrack()));
		if (persetujuan) {
			row.appendChild(new Label(skripsi.getAbstrack()));
		} else {
			row.appendChild(abstrack);
		}

		abstrack.setValue(parser.getText());
		abstrack.setWidth("90%");
		abstrack.setRows(10);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata Kunci"));
		keyword = new Textbox();

		if (persetujuan) {
			row.appendChild(new Label(skripsi.getKeyword()));
		} else {
			row.appendChild(keyword);
		}

		keyword.setValue(skripsi.getKeyword());
		keyword.setWidth("90%");
		keyword.setRows(2);
		keyword.setMaxlength(255);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_daftar_pustaka_di_pengajuan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar Pustaka"));

		referensis = new JSONArray(skripsi.getReferensi());
		row.appendChild(SkripsiAction.initReferensi(skripsi, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Skripsi skripsi = (Skripsi) arg0.getData();
				referensis = new JSONArray(skripsi.getReferensi());
			}
		}));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getKonfigurasi("file_utama_saat_upload_skripsi", "File (PDF)").getNilai() + " "
						+ (Common.bolehKonfigurasi("file_pdf_dan_cover_skripsi_wajib_diupload") ? "*" : "")));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), LampiranLain.SKRIPSI, "File Utama", true,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_file_presentasi_di_pengajuan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Presentasi"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), Skripsi.class.getName() + "_Presentasi",
				"File Presentasi (PPT)", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaPresentasi = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Cover (JPG) " + (Common.bolehKonfigurasi("file_pdf_dan_cover_skripsi_wajib_diupload") ? "*" : "")));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), LampiranLain.COVER_SKRIPSI, "Cover ", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaCover = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		rowUploadLampiran1 = new MyFormRow();
		rowUploadLampiran1.setVisible(false);
		rows.appendChild(rowUploadLampiran1);

		rowUploadLampiran2 = new MyFormRow();
		rowUploadLampiran2.setVisible(false);
		rows.appendChild(rowUploadLampiran2);

		rowUploadLampiran3 = new MyFormRow();
		rowUploadLampiran3.setVisible(false);
		rows.appendChild(rowUploadLampiran3);

		rowUploadLampiran4 = new MyFormRow();
		rowUploadLampiran4.setVisible(false);
		rows.appendChild(rowUploadLampiran4);

		rowUploadLampiran5 = new MyFormRow();
		rowUploadLampiran5.setVisible(false);
		rows.appendChild(rowUploadLampiran5);

		rowUploadLampiran6 = new MyFormRow();
		rowUploadLampiran6.setVisible(false);
		rows.appendChild(rowUploadLampiran6);

		rowUploadLampiran7 = new MyFormRow();
		rowUploadLampiran7.setVisible(false);
		rows.appendChild(rowUploadLampiran7);

		rowUploadLampiran8 = new MyFormRow();
		rowUploadLampiran8.setVisible(false);
		rows.appendChild(rowUploadLampiran8);

		rowUploadLampiran9 = new MyFormRow();
		rowUploadLampiran9.setVisible(false);
		rows.appendChild(rowUploadLampiran9);

		rowUploadLampiran10 = new MyFormRow();
		rowUploadLampiran10.setVisible(false);
		rows.appendChild(rowUploadLampiran10);

		rowUploadLampiran11 = new MyFormRow();
		rowUploadLampiran11.setVisible(false);
		rows.appendChild(rowUploadLampiran11);

		rowUploadLampiran12 = new MyFormRow();
		rowUploadLampiran12.setVisible(false);
		rows.appendChild(rowUploadLampiran12);

		rowUploadLampiran13 = new MyFormRow();
		rowUploadLampiran13.setVisible(false);
		rows.appendChild(rowUploadLampiran13);

		rowUploadLampiran14 = new MyFormRow();
		rowUploadLampiran14.setVisible(false);
		rows.appendChild(rowUploadLampiran14);

		rowUploadLampiran15 = new MyFormRow();
		rowUploadLampiran15.setVisible(false);
		rows.appendChild(rowUploadLampiran15);

		rowUploadLampiran16 = new MyFormRow();
		rowUploadLampiran16.setVisible(false);
		rows.appendChild(rowUploadLampiran16);

		rowUploadLampiran17 = new MyFormRow();
		rowUploadLampiran17.setVisible(false);
		rows.appendChild(rowUploadLampiran17);

		rowUploadLampiran18 = new MyFormRow();
		rowUploadLampiran18.setVisible(false);
		rows.appendChild(rowUploadLampiran18);

		rowUploadLampiran19 = new MyFormRow();
		rowUploadLampiran19.setVisible(false);
		rows.appendChild(rowUploadLampiran19);

		rowUploadLampiran20 = new MyFormRow();
		rowUploadLampiran20.setVisible(false);
		rows.appendChild(rowUploadLampiran20);

		final EventListener eventListenerUpload = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowUploadLampiran1.setVisible(false);
				rowUploadLampiran2.setVisible(false);
				rowUploadLampiran3.setVisible(false);
				rowUploadLampiran4.setVisible(false);
				rowUploadLampiran5.setVisible(false);
				rowUploadLampiran6.setVisible(false);
				rowUploadLampiran7.setVisible(false);
				rowUploadLampiran8.setVisible(false);
				rowUploadLampiran9.setVisible(false);
				rowUploadLampiran10.setVisible(false);
				rowUploadLampiran11.setVisible(false);
				rowUploadLampiran12.setVisible(false);
				rowUploadLampiran13.setVisible(false);
				rowUploadLampiran14.setVisible(false);
				rowUploadLampiran15.setVisible(false);

				rowUploadLampiran16.setVisible(false);
				rowUploadLampiran17.setVisible(false);
				rowUploadLampiran18.setVisible(false);
				rowUploadLampiran19.setVisible(false);
				rowUploadLampiran20.setVisible(false);

				Common.clear(rowUploadLampiran1);
				Common.clear(rowUploadLampiran2);
				Common.clear(rowUploadLampiran3);
				Common.clear(rowUploadLampiran4);
				Common.clear(rowUploadLampiran5);
				Common.clear(rowUploadLampiran6);
				Common.clear(rowUploadLampiran7);
				Common.clear(rowUploadLampiran8);
				Common.clear(rowUploadLampiran9);
				Common.clear(rowUploadLampiran10);

				Common.clear(rowUploadLampiran11);
				Common.clear(rowUploadLampiran12);
				Common.clear(rowUploadLampiran13);
				Common.clear(rowUploadLampiran14);
				Common.clear(rowUploadLampiran15);

				Common.clear(rowUploadLampiran16);
				Common.clear(rowUploadLampiran17);
				Common.clear(rowUploadLampiran18);
				Common.clear(rowUploadLampiran19);
				Common.clear(rowUploadLampiran20);

				FormatNilaiSkripsi format = (FormatNilaiSkripsi) (formatNilaiSkripsi.getSelectedItem() == null ? null
						: formatNilaiSkripsi.getSelectedItem().getValue());
				if (format != null) {

					if (!format.getUploadLampiran1().isEmpty()) {
						rowUploadLampiran1.setVisible(true);
						rowUploadLampiran1.appendChild(new Label(
								format.getUploadLampiran1() + " " + (format.getUploadLampiran1Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran1",
								format.getUploadLampiran1(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran1 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran1);
					}

					if (!format.getUploadLampiran2().isEmpty()) {
						rowUploadLampiran2.setVisible(true);
						rowUploadLampiran2.appendChild(new Label(
								format.getUploadLampiran2() + " " + (format.getUploadLampiran2Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran2",
								format.getUploadLampiran2(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran2 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran2);
					}

					if (!format.getUploadLampiran3().isEmpty()) {
						rowUploadLampiran3.setVisible(true);
						rowUploadLampiran3.appendChild(new Label(
								format.getUploadLampiran3() + " " + (format.getUploadLampiran3Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran3",
								format.getUploadLampiran3(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran3 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran3);
					}

					if (!format.getUploadLampiran4().isEmpty()) {
						rowUploadLampiran4.setVisible(true);
						rowUploadLampiran4.appendChild(new Label(
								format.getUploadLampiran4() + " " + (format.getUploadLampiran4Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran4",
								format.getUploadLampiran4(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran4 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran4);
					}

					if (!format.getUploadLampiran5().isEmpty()) {
						rowUploadLampiran5.setVisible(true);
						rowUploadLampiran5.appendChild(new Label(
								format.getUploadLampiran5() + " " + (format.getUploadLampiran5Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran5",
								format.getUploadLampiran5(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran5 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran5);
					}

					if (!format.getUploadLampiran6().isEmpty()) {
						rowUploadLampiran6.setVisible(true);
						rowUploadLampiran6.appendChild(new Label(
								format.getUploadLampiran6() + " " + (format.getUploadLampiran6Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran6",
								format.getUploadLampiran6(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran6 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran6);
					}

					if (!format.getUploadLampiran7().isEmpty()) {
						rowUploadLampiran7.setVisible(true);
						rowUploadLampiran7.appendChild(new Label(
								format.getUploadLampiran7() + " " + (format.getUploadLampiran7Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran7",
								format.getUploadLampiran7(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran7 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran7);
					}

					if (!format.getUploadLampiran8().isEmpty()) {
						rowUploadLampiran8.setVisible(true);
						rowUploadLampiran8.appendChild(new Label(
								format.getUploadLampiran8() + " " + (format.getUploadLampiran8Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran8",
								format.getUploadLampiran8(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran8 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran8);
					}

					if (!format.getUploadLampiran9().isEmpty()) {
						rowUploadLampiran9.setVisible(true);
						rowUploadLampiran9.appendChild(new Label(
								format.getUploadLampiran9() + " " + (format.getUploadLampiran9Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran9",
								format.getUploadLampiran9(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran9 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran9);
					}

					if (!format.getUploadLampiran10().isEmpty()) {
						rowUploadLampiran10.setVisible(true);
						rowUploadLampiran10.appendChild(new Label(
								format.getUploadLampiran10() + " " + (format.getUploadLampiran10Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran10",
								format.getUploadLampiran10(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran10 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran10);
					}

					if (!format.getUploadLampiran11().isEmpty()) {
						rowUploadLampiran11.setVisible(true);
						rowUploadLampiran11.appendChild(new Label(
								format.getUploadLampiran11() + " " + (format.getUploadLampiran11Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran11",
								format.getUploadLampiran11(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran11 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran11);
					}

					if (!format.getUploadLampiran12().isEmpty()) {
						rowUploadLampiran12.setVisible(true);
						rowUploadLampiran12.appendChild(new Label(
								format.getUploadLampiran12() + " " + (format.getUploadLampiran12Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran12",
								format.getUploadLampiran12(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran12 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran12);
					}

					if (!format.getUploadLampiran13().isEmpty()) {
						rowUploadLampiran13.setVisible(true);
						rowUploadLampiran13.appendChild(new Label(
								format.getUploadLampiran13() + " " + (format.getUploadLampiran13Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran13",
								format.getUploadLampiran13(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran13 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran13);
					}

					if (!format.getUploadLampiran14().isEmpty()) {
						rowUploadLampiran14.setVisible(true);
						rowUploadLampiran14.appendChild(new Label(
								format.getUploadLampiran14() + " " + (format.getUploadLampiran14Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran14",
								format.getUploadLampiran14(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran14 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran14);
					}

					if (!format.getUploadLampiran15().isEmpty()) {
						rowUploadLampiran15.setVisible(true);
						rowUploadLampiran15.appendChild(new Label(
								format.getUploadLampiran15() + " " + (format.getUploadLampiran15Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran15",
								format.getUploadLampiran15(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran15 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran15);
					}

					if (!format.getUploadLampiran16().isEmpty()) {
						rowUploadLampiran16.setVisible(true);
						rowUploadLampiran16.appendChild(new Label(
								format.getUploadLampiran16() + " " + (format.getUploadLampiran16Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran16",
								format.getUploadLampiran16(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran16 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran16);
					}

					if (!format.getUploadLampiran17().isEmpty()) {
						rowUploadLampiran17.setVisible(true);
						rowUploadLampiran17.appendChild(new Label(
								format.getUploadLampiran17() + " " + (format.getUploadLampiran17Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran17",
								format.getUploadLampiran17(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran17 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran17);
					}

					if (!format.getUploadLampiran18().isEmpty()) {
						rowUploadLampiran18.setVisible(true);
						rowUploadLampiran18.appendChild(new Label(
								format.getUploadLampiran18() + " " + (format.getUploadLampiran18Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran18",
								format.getUploadLampiran18(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran18 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran18);
					}

					if (!format.getUploadLampiran19().isEmpty()) {
						rowUploadLampiran19.setVisible(true);
						rowUploadLampiran19.appendChild(new Label(
								format.getUploadLampiran19() + " " + (format.getUploadLampiran19Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran19",
								format.getUploadLampiran19(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran19 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran19);
					}

					if (!format.getUploadLampiran20().isEmpty()) {
						rowUploadLampiran20.setVisible(true);
						rowUploadLampiran20.appendChild(new Label(
								format.getUploadLampiran20() + " " + (format.getUploadLampiran20Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), "rowUploadLampiran20",
								format.getUploadLampiran20(), false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran20 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran20);
					}
				}
			}
		};

		formatNilaiSkripsi.addEventListener("onChange", eventListenerUpload);

		rowPembimbing1 = new MyFormRow();
		rowPembimbing1.setParent(rows);
		rowPembimbing1.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing I"));
		Hbox persetujuan = new Hbox();
		rowPembimbing1.appendChild(persetujuan);
		pembimbing = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && !this.persetujuan) {
			persetujuan.appendChild(pembimbing);
		} else {
			persetujuan
					.appendChild(new Label(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama()));
		}
		pembimbing.setValue(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama());
		pembimbing.setAttribute("myValue", skripsi.getPembimbing());
		pembimbing.setAttribute("dosen", skripsi.getPembimbing());
		pembimbing.setWidth("90%");

		final Image imgPembimbing = (skripsi.getPersetujuanPembimbing1() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan1Label = skripsi.getPersetujuanPembimbing1()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPembimbing1 = new Checkbox("Persetujuan Pembimbing I");
		persetujuanPembimbing1.setChecked(skripsi.getPersetujuanPembimbing1());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& !this.persetujuan && skripsi.getPembimbing() != null
				&& skripsi.getPembimbing().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPembimbing1);
		} else {
			persetujuan.appendChild(imgPembimbing);
			persetujuan.appendChild(statusPersetujuan1Label);
		}

		imgPembimbing.setVisible(pembimbing.getAttribute("dosen") != null);
		statusPersetujuan1Label.setVisible(pembimbing.getAttribute("dosen") != null);
		persetujuanPembimbing1.setVisible(pembimbing.getAttribute("dosen") != null);

		pembimbing.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPembimbing.setVisible(pembimbing.getAttribute("dosen") != null);
				statusPersetujuan1Label.setVisible(pembimbing.getAttribute("dosen") != null);
				persetujuanPembimbing1.setVisible(pembimbing.getAttribute("dosen") != null);
			}
		});

		rowPembimbing2 = new MyFormRow();
		rowPembimbing2.setParent(rows);
		rowPembimbing2.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing II"));
		persetujuan = new Hbox();
		rowPembimbing2.appendChild(persetujuan);
		ketuaSidang = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && !this.persetujuan) {
			persetujuan.appendChild(ketuaSidang);
		} else {
			persetujuan
					.appendChild(new Label(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama()));
		}
		ketuaSidang.setValue(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama());
		ketuaSidang.setAttribute("myValue", skripsi.getKetuaSidang());
		ketuaSidang.setAttribute("dosen", skripsi.getKetuaSidang());
		ketuaSidang.setWidth("90%");

		final Image imgKetuaSidang = (skripsi.getPersetujuanPembimbing2() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan2Label = skripsi.getPersetujuanPembimbing2()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPembimbing2 = new Checkbox("Persetujuan Pembimbing II");
		persetujuanPembimbing2.setChecked(skripsi.getPersetujuanPembimbing2());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& skripsi.getKetuaSidang() != null && !this.persetujuan
				&& skripsi.getKetuaSidang().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPembimbing2);
		} else {
			persetujuan.appendChild(imgKetuaSidang);
			persetujuan.appendChild(statusPersetujuan2Label);
		}

		imgKetuaSidang.setVisible(ketuaSidang.getAttribute("dosen") != null);
		statusPersetujuan2Label.setVisible(ketuaSidang.getAttribute("dosen") != null);
		persetujuanPembimbing2.setVisible(ketuaSidang.getAttribute("dosen") != null);

		ketuaSidang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgKetuaSidang.setVisible(ketuaSidang.getAttribute("dosen") != null);
				statusPersetujuan2Label.setVisible(ketuaSidang.getAttribute("dosen") != null);
				persetujuanPembimbing2.setVisible(ketuaSidang.getAttribute("dosen") != null);
			}
		});

		persetujuanPembimbing3 = new Checkbox("Persetujuan Pembimbing III");
		pembimbing3 = new AmbilDataDosenSkripsiBanbox();
		try {
			rowPembimbing3 = new MyFormRow();
			rowPembimbing3.setParent(rows);
			rowPembimbing3.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing III"));
			persetujuan = new Hbox();
			rowPembimbing3.appendChild(persetujuan);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && !this.persetujuan && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null) {
				persetujuan.appendChild(pembimbing3);
			} else {
				try {
					persetujuan.appendChild(
							new Label(skripsi.getPembimbing3() == null ? "" : skripsi.getPembimbing3().getNama()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SkripsiAction.java:7099");
					// TODO: handle exception
				}
			}
			pembimbing3.setValue(skripsi.getPembimbing3() == null ? "" : skripsi.getPembimbing3().getNama());
			pembimbing3.setAttribute("myValue", skripsi.getPembimbing3());
			pembimbing3.setAttribute("dosen", skripsi.getPembimbing3());
			pembimbing3.setWidth("90%");

			final Image imgPembimbing3 = (skripsi.getPersetujuanPembimbing3() ? new Image("/img/Cute-Ball-Go-icon.png")
					: new Image("/img/Button-Delete-icon.png"));
			final MyLabelAgakKecilBold statusPersetujuan3Label = skripsi.getPersetujuanPembimbing3()
					? new MyLabelAgakKecilBold("Telah disetujui")
					: new MyLabelAgakKecilBold("Belum mensetujui");

			persetujuanPembimbing3.setChecked(skripsi.getPersetujuanPembimbing3());
			if (tbmuser != null && tbmuser.ambilDosen() != null && !this.persetujuan
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") && skripsi.getPembimbing3() != null
					&& skripsi.getPembimbing3().getId().equals(tbmuser.getDosen().getId())) {
				persetujuan.appendChild(persetujuanPembimbing3);
			} else {
				persetujuan.appendChild(imgPembimbing3);
				persetujuan.appendChild(statusPersetujuan3Label);
			}

			imgPembimbing3.setVisible(pembimbing3.getAttribute("dosen") != null);
			statusPersetujuan3Label.setVisible(pembimbing3.getAttribute("dosen") != null);
			persetujuanPembimbing3.setVisible(pembimbing3.getAttribute("dosen") != null);

			pembimbing3.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					imgPembimbing3.setVisible(pembimbing3.getAttribute("dosen") != null);
					statusPersetujuan3Label.setVisible(pembimbing3.getAttribute("dosen") != null);
					persetujuanPembimbing3.setVisible(pembimbing3.getAttribute("dosen") != null);
				}
			});
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Bimbingan *"));
		awalBimbingan = new MyDatebox(skripsi.getAwalBimbingan() == null ? null : skripsi.getAwalBimbingan());
		if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) && !this.persetujuan
				|| Common.bolehKonfigurasi("saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing", Konfigurasi.TIDAK_AKTIF)) {
			row.appendChild(awalBimbingan);
		} else {
			row.appendChild(new Label(skripsi.getAwalBimbingan() == null ? ""
					: Common.dateFormat2.get().format(skripsi.getAwalBimbingan())));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Bimbingan *"));
		akhirBimbingan = new MyDatebox(skripsi.getAkhirBimbingan() == null ? null : skripsi.getAkhirBimbingan());
		if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) && !this.persetujuan
				|| Common.bolehKonfigurasi("saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing", Konfigurasi.TIDAK_AKTIF)) {
			row.appendChild(akhirBimbingan);
		} else {
			row.appendChild(new Label(skripsi.getAkhirBimbingan() == null ? ""
					: Common.dateFormat2.get().format(skripsi.getAkhirBimbingan())));
		}

		final EventListener mahasiswaEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(formatNilaiSkripsi);
				Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
				if (mhs != null) {

					muatGelombangPendaftaranSidang(mhs);

					mhsFormatEvent.onEvent(null);

					pembimbing.setDisabled(false);
					ketuaSidang.setDisabled(false);

					Session session = HibernateUtil.currentSession();
					MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) session
							.createCriteria(MahasiswaRequestTugasAkhir.class).add(Restrictions.eq("mahasiswa", mhs))
							.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS),
											Restrictions.or(
													Restrictions.eq("status",
															MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
													Restrictions.eq("status",
															MahasiswaRequestTugasAkhir.LULUS_STATUS)))))
							.createAlias("formatNilaiProposalSkripsi", "formatNilaiProposalSkripsi")
							.add(formatNilaiSkripsi.getSelectedItem() == null
									|| formatNilaiSkripsi.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.or(
													Restrictions
															.isNull("formatNilaiProposalSkripsi.formatNilaiSkripsi"),
													Restrictions.eq("formatNilaiProposalSkripsi.formatNilaiSkripsi",
															formatNilaiSkripsi.getSelectedItem().getValue())))
							.addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();

					if (mahasiswaRequestTugasAkhir != null && judulCK.getValue().trim().isEmpty()) {
						judulCK.setValue(mahasiswaRequestTugasAkhir.getJudul());
					}

					if (mahasiswaRequestTugasAkhir != null
							&& mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() != null) {
						awalBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
					}

					if (mahasiswaRequestTugasAkhir != null
							&& mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan() != null) {
						akhirBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
					}

					if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getDosen1() != null) {
						skripsi.setPembimbing(mahasiswaRequestTugasAkhir.getDosen1());
						pembimbing.setValue(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama());
						pembimbing.setAttribute("myValue", skripsi.getPembimbing());
						pembimbing.setAttribute("dosen", skripsi.getPembimbing());
						pembimbing.setDisabled(true);
					}

					if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getDosen2() != null) {
						skripsi.setKetuaSidang(mahasiswaRequestTugasAkhir.getDosen2());
						ketuaSidang
								.setValue(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama());
						ketuaSidang.setAttribute("myValue", skripsi.getKetuaSidang());
						ketuaSidang.setAttribute("dosen", skripsi.getKetuaSidang());
						ketuaSidang.setDisabled(true);
					}

					if (mhs != null) {

						Object nilaiSemesterDipilih = semester != null && semester.getSelectedItem() != null
								? semester.getSelectedItem().getValue()
								: null;
						if (nilaiSemesterDipilih == null
								|| (nilaiSemesterDipilih instanceof Integer
										&& ((Integer) nilaiSemesterDipilih).intValue() == 0)) {
							Common.selectComboItem(semester, mhs.currentSemester());
						}

						List<Double> nilaiToeflMahasiswas = session.createCriteria(NilaiToeflToaflMahasiswa.class)
								.add(Restrictions.eq("mahasiswa", mhs))
								.add(Restrictions.eq("jenisTest", NilaiToeflToaflMahasiswa.TOEFL))
								.setProjection(Projections.property("skor4")).list();

						List<Double> nilaiToaflMahasiswas = session.createCriteria(NilaiToeflToaflMahasiswa.class)
								.add(Restrictions.eq("mahasiswa", mhs))
								.add(Restrictions.eq("jenisTest", NilaiToeflToaflMahasiswa.TOAFL))
								.setProjection(Projections.property("skor4")).list();

						Double skorMaxToefl = 0.0;
						Double skorMaxToafl = 0.0;
						if (nilaiToeflMahasiswas.size() > 0) {
							skorMaxToefl = Collections.max(nilaiToeflMahasiswas);
						}
						if (nilaiToaflMahasiswas.size() > 0) {
							skorMaxToafl = Collections.max(nilaiToaflMahasiswas);
						}
						Common.clear(rowToefl);
						rowToefl.appendChild(new Label("Lulus TOEFL (Skor TOEFL = " + skorMaxToefl + " )"));
						rowToefl.appendChild(lulusToefl = new MyCheckboxConfig());
						Common.clear(rowToafl);
						rowToafl.appendChild(new Label("Lulus TOAFL (Skor TOAFL = " + skorMaxToafl + " )"));
						rowToafl.appendChild(lulusToafl = new MyCheckboxConfig());
						// belum auto check jika diatas standar prodi
					}

				}

				Common.createDefaultTimer(eventListenerUpload);
			}
		};

		mahasiswa.setEventListener(mahasiswaEventListener);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswaEventListener.onEvent(null);
			}
		});

		rowToefl = new MyFormRow();
		rowToefl.setStyle("border:0px;background: transparent;");
		rowToefl.setParent(rows);
		Session session = HibernateUtil.currentSession();
		nilaiToeflMahasiswas = session.createCriteria(NilaiToeflToaflMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", skripsi.getMahasiswa()))
				.add(Restrictions.eq("jenisTest", NilaiToeflToaflMahasiswa.TOEFL))
				.setProjection(Projections.property("skor4")).list();

		Double skorMaxToefl = 0.0;
		if (nilaiToeflMahasiswas.size() > 0) {
			skorMaxToefl = Collections.max(nilaiToeflMahasiswas);
		}

		lulusToefl = new MyCheckboxConfig();
		if (this.persetujuan) {
			rowToefl.appendChild(new Label("Lulus TOEFL (Skor TOEFL = " + skorMaxToefl + " )"));
			rowToafl.appendChild(new Label());
		} else {
			rowToefl.appendChild(new Label("Lulus TOEFL (Skor TOEFL = " + skorMaxToefl + " )"));
			rowToefl.appendChild(lulusToefl);
		}

		rowToafl = new MyFormRow();
		rowToafl.setStyle("border:0px;background: transparent;");
		rowToafl.setParent(rows);
		nilaiToaflMahasiswas = session.createCriteria(NilaiToeflToaflMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", skripsi.getMahasiswa()))
				.add(Restrictions.eq("jenisTest", NilaiToeflToaflMahasiswa.TOAFL))
				.setProjection(Projections.property("skor4")).list();

		Double skorMaxToafl = 0.0;
		if (nilaiToaflMahasiswas.size() > 0) {
			skorMaxToefl = Collections.max(nilaiToaflMahasiswas);
		}

		if (this.persetujuan) {
			rowToafl.appendChild(new Label("Lulus TOAFL (Skor TOAFL = " + skorMaxToafl + " )"));
			rowToafl.appendChild(new Label());
		} else {
			rowToafl.appendChild(new Label("Lulus TOAFL (Skor TOAFL = " + skorMaxToafl + " )"));
			rowToafl.appendChild(lulusToafl = new MyCheckboxConfig());
		}
		if (dataMahasiswa != null) {
			rowToefl.setVisible(false);
		}

		if (dataMahasiswa != null) {
			rowToafl.setVisible(false);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("III. Data Persetujuan"));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Setujui sidang"));
		setujuiSidang = new MyCheckboxConfig();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(setujuiSidang);
		} else {
			row.appendChild(new Label(skripsi.getSetujuiSidang() ? "Ya" : "Tidak"));
		}
		setujuiSidang.setChecked(skripsi.getSetujuiSidang());

		MyFormRow rowSidang = new MyFormRow();
		rowSidang.setStyle("border:0px;background: transparent;");
		rowSidang.setParent(rows);
		rowSidang.appendChild(new Label(ais.common.Common.getBahasaConfig("Jadwal Sidang")));

		jadwalSidangTugasAkhir = new AmbilJadwalSidangTugasAkhirBanbox();
		jadwalSidangTugasAkhir.setValue(
				skripsi.getJadwalSidangTugasAkhir() == null ? "" : skripsi.getJadwalSidangTugasAkhir().getNama());
		jadwalSidangTugasAkhir.setAttribute("jadwalSidangTugasAkhir", skripsi.getJadwalSidangTugasAkhir());
		jadwalSidangTugasAkhir.setAttribute("myValue", skripsi.getJadwalSidangTugasAkhir());
		jadwalSidangTugasAkhir.setWidth("90%");

		tbmuser = Common.getCurrentUser();
		if ((tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) || this.persetujuan) {
			rowSidang.appendChild(new Label(
					skripsi.getJadwalSidangTugasAkhir() == null ? "" : skripsi.getJadwalSidangTugasAkhir().getNama()));
		} else {
			rowSidang.appendChild(jadwalSidangTugasAkhir);
		}

		rowPenguji1 = new MyFormRow();
		rowPenguji1.setParent(rows);
		rowPenguji1.appendChild(new ais.ui.util.MyLabelConfig("Penguji I *"));
		persetujuan = new Hbox();
		rowPenguji1.appendChild(persetujuan);
		penguji1 = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && !this.persetujuan) {
			persetujuan.appendChild(penguji1);
		} else {
			persetujuan.appendChild(new Label(skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNama()));
		}
		penguji1.setValue(skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNama());
		penguji1.setAttribute("myValue", skripsi.getPenguji1());
		penguji1.setAttribute("dosen", skripsi.getPenguji1());
		penguji1.setWidth("90%");

		final Image imgPenguji1 = (skripsi.getPersetujuanPenguji1() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan1LabelPenguji = skripsi.getPersetujuanPenguji1()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPenguji1 = new Checkbox("Persetujuan Penguji I");
		persetujuanPenguji1.setChecked(skripsi.getPersetujuanPenguji1());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& !this.persetujuan && skripsi.getPenguji1() != null
				&& skripsi.getPenguji1().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPenguji1);
		} else {
			persetujuan.appendChild(imgPenguji1);
			persetujuan.appendChild(statusPersetujuan1LabelPenguji);
		}

		imgPenguji1.setVisible(penguji1.getAttribute("dosen") != null);
		statusPersetujuan1LabelPenguji.setVisible(penguji1.getAttribute("dosen") != null);
		persetujuanPenguji1.setVisible(penguji1.getAttribute("dosen") != null);

		penguji1.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPenguji1.setVisible(penguji1.getAttribute("dosen") != null);
				statusPersetujuan1LabelPenguji.setVisible(penguji1.getAttribute("dosen") != null);
				persetujuanPenguji1.setVisible(penguji1.getAttribute("dosen") != null);
			}
		});

		rowPenguji2 = new MyFormRow();
		rowPenguji2.setParent(rows);
		rowPenguji2.appendChild(new ais.ui.util.MyLabelConfig("Penguji II"));
		persetujuan = new Hbox();
		rowPenguji2.appendChild(persetujuan);
		penguji2 = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			persetujuan.appendChild(penguji2);
		} else {
			persetujuan.appendChild(new Label(skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNama()));
		}
		penguji2.setValue(skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNama());
		penguji2.setAttribute("myValue", skripsi.getPenguji2());
		penguji2.setAttribute("dosen", skripsi.getPenguji2());
		penguji2.setWidth("90%");

		final Image imgPenguji2 = (skripsi.getPersetujuanPenguji2() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan2LabelPenguji = skripsi.getPersetujuanPenguji2()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPenguji2 = new Checkbox("Persetujuan Penguji II");
		persetujuanPenguji2.setChecked(skripsi.getPersetujuanPenguji2());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& !this.persetujuan && skripsi.getPenguji2() != null
				&& skripsi.getPenguji2().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPenguji2);
		} else {
			persetujuan.appendChild(imgPenguji2);
			persetujuan.appendChild(statusPersetujuan2LabelPenguji);
		}

		imgPenguji2.setVisible(penguji2.getAttribute("dosen") != null);
		statusPersetujuan2LabelPenguji.setVisible(penguji2.getAttribute("dosen") != null);
		persetujuanPenguji2.setVisible(penguji2.getAttribute("dosen") != null);

		penguji2.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPenguji2.setVisible(penguji2.getAttribute("dosen") != null);
				statusPersetujuan2LabelPenguji.setVisible(penguji2.getAttribute("dosen") != null);
				persetujuanPenguji2.setVisible(penguji2.getAttribute("dosen") != null);
			}
		});

		rowPenguji3 = new MyFormRow();
		rowPenguji3.setParent(rows);
		rowPenguji3.appendChild(new ais.ui.util.MyLabelConfig("Penguji III"));
		persetujuan = new Hbox();
		rowPenguji3.appendChild(persetujuan);
		penguji3 = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			persetujuan.appendChild(penguji3);
		} else {
			persetujuan.appendChild(new Label(skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNama()));
		}
		penguji3.setValue(skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNama());
		penguji3.setAttribute("myValue", skripsi.getPenguji3());
		penguji3.setAttribute("dosen", skripsi.getPenguji3());
		penguji3.setWidth("90%");

		final Image imgPenguji3 = (skripsi.getPersetujuanPenguji3() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan3Label = skripsi.getPersetujuanPenguji3()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPenguji3 = new Checkbox("Persetujuan Penguji III");
		persetujuanPenguji3.setChecked(skripsi.getPersetujuanPenguji3());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& skripsi.getPenguji3() != null && skripsi.getPenguji3().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPenguji3);
		} else {
			persetujuan.appendChild(imgPenguji3);
			persetujuan.appendChild(statusPersetujuan3Label);
		}

		imgPenguji3.setVisible(penguji3.getAttribute("dosen") != null);
		statusPersetujuan3Label.setVisible(penguji3.getAttribute("dosen") != null);
		persetujuanPenguji3.setVisible(penguji3.getAttribute("dosen") != null);

		penguji3.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPenguji3.setVisible(penguji3.getAttribute("dosen") != null);
				statusPersetujuan3Label.setVisible(penguji3.getAttribute("dosen") != null);
				persetujuanPenguji3.setVisible(penguji3.getAttribute("dosen") != null);
			}
		});

		rowPenguji4 = new MyFormRow();
		rowPenguji4.setParent(rows);
		rowPenguji4.appendChild(new ais.ui.util.MyLabelConfig("Penguji IV"));
		persetujuan = new Hbox();
		rowPenguji4.appendChild(persetujuan);
		penguji4 = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			persetujuan.appendChild(penguji4);
		} else {
			persetujuan.appendChild(new Label(skripsi.getPenguji4() == null ? "" : skripsi.getPenguji4().getNama()));
		}
		penguji4.setValue(skripsi.getPenguji4() == null ? "" : skripsi.getPenguji4().getNama());
		penguji4.setAttribute("myValue", skripsi.getPenguji4());
		penguji4.setAttribute("dosen", skripsi.getPenguji4());
		penguji4.setWidth("90%");

		final Image imgPenguji4 = (skripsi.getPersetujuanPenguji4() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan4Label = skripsi.getPersetujuanPenguji4()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPenguji4 = new Checkbox("Persetujuan Penguji IV");
		persetujuanPenguji4.setChecked(skripsi.getPersetujuanPenguji4());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& !this.persetujuan && skripsi.getPenguji4() != null
				&& skripsi.getPenguji4().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPenguji4);
		} else {
			persetujuan.appendChild(imgPenguji4);
			persetujuan.appendChild(statusPersetujuan4Label);
		}

		imgPenguji4.setVisible(penguji4.getAttribute("dosen") != null);
		statusPersetujuan4Label.setVisible(penguji4.getAttribute("dosen") != null);
		persetujuanPenguji4.setVisible(penguji4.getAttribute("dosen") != null);

		penguji4.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPenguji4.setVisible(penguji4.getAttribute("dosen") != null);
				statusPersetujuan4Label.setVisible(penguji4.getAttribute("dosen") != null);
				persetujuanPenguji4.setVisible(penguji4.getAttribute("dosen") != null);
			}
		});

		rowPenguji5 = new MyFormRow();
		rowPenguji5.setParent(rows);
		rowPenguji5.appendChild(new ais.ui.util.MyLabelConfig("Penguji V"));
		persetujuan = new Hbox();
		rowPenguji5.appendChild(persetujuan);
		penguji5 = new AmbilDataDosenSkripsiBanbox();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			persetujuan.appendChild(penguji5);
		} else {
			persetujuan.appendChild(new Label(skripsi.getPenguji5() == null ? "" : skripsi.getPenguji5().getNama()));
		}
		penguji5.setValue(skripsi.getPenguji5() == null ? "" : skripsi.getPenguji5().getNama());
		penguji5.setAttribute("myValue", skripsi.getPenguji5());
		penguji5.setAttribute("dosen", skripsi.getPenguji5());
		penguji5.setWidth("90%");

		final Image imgPenguji5 = (skripsi.getPersetujuanPenguji5() ? new Image("/img/Cute-Ball-Go-icon.png")
				: new Image("/img/Button-Delete-icon.png"));
		final MyLabelAgakKecilBold statusPersetujuan5Label = skripsi.getPersetujuanPenguji5()
				? new MyLabelAgakKecilBold("Telah disetujui")
				: new MyLabelAgakKecilBold("Belum mensetujui");

		persetujuanPenguji5 = new Checkbox("Persetujuan Penguji V");
		persetujuanPenguji5.setChecked(skripsi.getPersetujuanPenguji5());
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& !this.persetujuan && skripsi.getPenguji5() != null
				&& skripsi.getPenguji5().getId().equals(tbmuser.getDosen().getId())) {
			persetujuan.appendChild(persetujuanPenguji5);
		} else {
			persetujuan.appendChild(imgPenguji5);
			persetujuan.appendChild(statusPersetujuan5Label);
		}

		imgPenguji5.setVisible(penguji5.getAttribute("dosen") != null);
		statusPersetujuan5Label.setVisible(penguji5.getAttribute("dosen") != null);
		persetujuanPenguji5.setVisible(penguji5.getAttribute("dosen") != null);

		penguji5.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgPenguji5.setVisible(penguji5.getAttribute("dosen") != null);
				statusPersetujuan5Label.setVisible(penguji5.getAttribute("dosen") != null);
				persetujuanPenguji5.setVisible(penguji5.getAttribute("dosen") != null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telah melakukan sidang ?"));

		telahSidang = new MyCheckboxConfig();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(telahSidang);
		} else {
			row.appendChild(new Label(
					(skripsi.getTelahSidang() == null ? false : skripsi.getTelahSidang().equals(1)) ? "Ya" : "Tidak"));
		}
		telahSidang.setChecked(skripsi.getTelahSidang() == null ? false : skripsi.getTelahSidang().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sidang *"));
		tanggalSidang = new MyDatebox(skripsi.getTanggalSidang());
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(tanggalSidang);
		} else {
			row.appendChild(new Label(skripsi.getTanggalSidang() == null ? ""
					: Common.dateFormat2.get().format(skripsi.getTanggalSidang())));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Sidang *"));

		waktuSidang = new ais.ui.util.MyTimebox();
		waktuSampaiSidang = new ais.ui.util.MyTimebox();

		hbox = new Hbox();

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			hbox.appendChild(waktuSidang);
			hbox.appendChild(new Label("-"));
			hbox.appendChild(waktuSampaiSidang);
		} else {
			hbox.appendChild(new Label(skripsi.getWaktuSidang()));
			hbox.appendChild(new Label("-"));
			hbox.appendChild(new Label(skripsi.getWaktuSampaiSidang()));
		}

		waktuSidang.setFormat(Common.timeFormat.get().toPattern());
		waktuSampaiSidang.setFormat(Common.timeFormat.get().toPattern());
		try {
			waktuSidang.setValue(Common.timeFormat.get().parse(skripsi.getWaktuSidang()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			waktuSampaiSidang.setValue(Common.timeFormat.get().parse(skripsi.getWaktuSampaiSidang()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		row.appendChild(hbox);
		waktuSidang.setCols(5);
		waktuSampaiSidang.setCols(5);

		EventListener setujuiSidangListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalSidangTugasAkhir.setDisabled(!setujuiSidang.isChecked());
				penguji1.setDisabled(!setujuiSidang.isChecked());
				penguji2.setDisabled(!setujuiSidang.isChecked());
				penguji3.setDisabled(!setujuiSidang.isChecked());
				penguji4.setDisabled(!setujuiSidang.isChecked());
				penguji5.setDisabled(!setujuiSidang.isChecked());
			}
		};

		setujuiSidang.addEventListener("onCheck", setujuiSidangListener);
		setujuiSidangListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Sidang"));
		lokasiUjian = new Textbox(skripsi.getLokasiUjian());
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(lokasiUjian);
		} else {
			row.appendChild(new Label(skripsi.getLokasiUjian()));
		}
		lokasiUjian.setWidth("90%");
		lokasiUjian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK Tugas"));
		nomorSk = new Textbox(skripsi.getNomorSk());
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(nomorSk);
		} else {
			row.appendChild(new Label(skripsi.getNomorSk()));
		}
		nomorSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Tugas"));
		tglSk = new MyDatebox(skripsi.getTglSk());
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && !this.persetujuan) {
			row.appendChild(tglSk);
		} else {
			row.appendChild(
					new Label(skripsi.getTglSk() == null ? "" : Common.dateFormat2.get().format(skripsi.getTglSk())));
		}

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n1 = new ais.ui.util.MyLabelConfig("Nilai Pembimbing I"));
		row.appendChild(p1 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiKetuaSidang())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n2 = new ais.ui.util.MyLabelConfig("Nilai Pembimbing II"));
		row.appendChild(p2 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPembimbing())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n21 = new ais.ui.util.MyLabelConfig("Nilai Pembimbing III"));
		row.appendChild(p21 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPembimbing3())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n3 = new ais.ui.util.MyLabelConfig("Nilai Penguji I"));
		row.appendChild(u1 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPenguji1())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n4 = new ais.ui.util.MyLabelConfig("Nilai Penguji II"));
		row.appendChild(u2 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPenguji2())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n5 = new ais.ui.util.MyLabelConfig("Nilai Penguji III"));
		row.appendChild(u3 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPenguji3())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n6 = new ais.ui.util.MyLabelConfig("Nilai Penguji IV"));
		row.appendChild(u4 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPenguji4())));

		row = new MyFormRow();
				row.setVisible(dataMahasiswa == null);
		row.setParent(rows);
		row.appendChild(n7 = new ais.ui.util.MyLabelConfig("Nilai Penguji V"));
		row.appendChild(u5 = new MyLabelBold(Common.numberFormat.get().format(skripsi.getNilaiPenguji5())));

		// row = new MyFormRow();
		// row.setVisible(false);
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Komprehensif"));
		// nilaikomprehensif = new MyDoublebox(
		// skripsi.getNilaikomprehensif() == null ? 0.0 :
		// skripsi.getNilaikomprehensif());
		// if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa()
		// == null) {
		// row.appendChild(nilaikomprehensif);
		// } else {
		// row.appendChild(
		// new Label(skripsi.getNilaikomprehensif() == null ? "" :
		// skripsi.getNilaikomprehensif().toString()));
		// }
		//
		// nilaikomprehensif.setDisabled(false);
		// nilaikomprehensif.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Total Nilai"));
		row.appendChild(t = new MyLabelBold(Common.numberFormat.get().format(skripsi.getTotalNilai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Huruf"));
		row.appendChild(h = new MyLabelBold(skripsi.getNilaiHuruf()));

		hasilSidangListener.onEvent(null);

		if (skripsi.getSembunyikanNilaiKemahasiswa()) {
			n1.getParent().setVisible(false);
			n2.getParent().setVisible(false);
			n21.getParent().setVisible(false);
			n3.getParent().setVisible(false);
			n4.getParent().setVisible(false);
			n5.getParent().setVisible(false);
			n6.getParent().setVisible(false);
			n7.getParent().setVisible(false);
			t.getParent().setVisible(false);
			h.getParent().setVisible(false);
		}

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		if (this.persetujuan) {
			rowToefl.appendChild(new Label(skripsi.getFeeder()));
		} else {
			row.appendChild(feeder = new Textbox(skripsi.getFeeder()));
		}
		feeder.setWidth("90%");
		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Sidang";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return skripsi;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return Skripsi.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
