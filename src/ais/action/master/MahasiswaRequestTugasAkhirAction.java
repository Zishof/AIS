package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AktifitasTugasAkhirHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilJadwalSeminarTugasAkhirBanbox;
import ais.action.master.helper.KrsDanSkripsiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanRekapitulasiSeminar;
import ais.action.report.format1.akademik.LaporanSeminar;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaRequestTugasAkhirDao;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalSeminarTugasAkhir;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.TahapanAtauCapaianPembelajaran;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import de.undercouch.citeproc.CSL;

public class MahasiswaRequestTugasAkhirAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, FormSop {

	public MahasiswaRequestTugasAkhirAction() {
		super();
	}

	public MahasiswaRequestTugasAkhirAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	protected Textbox searchnim;
	protected Textbox searchjudul;
	protected Textbox searchnama;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	protected Decimalbox searchtahun;
	protected Combobox searchstatus;
	protected Combobox searchprogram;
	protected Combobox searchStatusAwalMahasiswa;
	protected Combobox searchjenjang;

	protected Combobox searchreqstatus;
	protected Combobox searchta;
	protected Combobox searchsemester;

	protected Textbox searchdosen;
	protected AmbilDataDosenBanbox searchdosenPemimbing;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox tahunAkademik;
	private Combobox semester;
	private Radiogroup status;
	private Textbox keterangan;
	private Textbox judul;
	private MyDatebox tanggalAwalBimbingan;
	private MyDatebox tanggalAkhirBimbingan;
	private Combobox formatNilaiProposalSkripsi;
	private Combobox cbSyaratSebelumnya;

	private boolean edit = false;
	private boolean delete = false;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private MyToolbarbuttonConfig add;
	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	private EventListener eventListener;
	private AmbilDataDosenBanbox dosen1;
	private AmbilDataDosenBanbox dosen2;
	private AmbilDataDosenBanbox dosen3;
	private AmbilDataDosenBanbox dosen4;
	private AmbilDataDosenBanbox dosen5;
	private AmbilDataDosenBanbox dosen6;

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

	private Row cari1;
	private Row cari1_2;
	private Row cari2;
	private Row cari2_2;
	private Row cari3;
	private Row cari3_2;
	private MyToolbarbuttonConfig find;

	public boolean checkSyaratPembimbingDanPenguji() throws Exception {

		if (Common.bolehKonfigurasi("dosen_penguji_dan_pembimbing_skrips_boleh_sama", Konfigurasi.TIDAK_AKTIF)) {
			return true;
		}

		List<Long> checkDosens = new ArrayList<Long>();
		if (dosen1.getAttribute("myValue") != null) {
			checkDosens.add(((Dosen) dosen1.getAttribute("myValue")).getId());
		}
		if (dosen2.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) dosen2.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Mohon maaf, dosen pembimbing dan dosen penguji tidak boleh merupakan orang yang sama. Bapak/Ibu diharapkan memilih dosen yang berbeda untuk masing-masing peran (pembimbing dan penguji) terlebih dahulu sebelum melanjutkan.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				return false;
			}
			checkDosens.add(((Dosen) dosen2.getAttribute("myValue")).getId());
		}
		if (dosen3.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) dosen3.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Mohon maaf, dosen pembimbing dan dosen penguji tidak boleh merupakan orang yang sama. Bapak/Ibu diharapkan memilih dosen yang berbeda untuk masing-masing peran (pembimbing dan penguji) terlebih dahulu sebelum melanjutkan.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				return false;
			}
			checkDosens.add(((Dosen) dosen3.getAttribute("myValue")).getId());
		}
		if (dosen4.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) dosen4.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Mohon maaf, dosen pembimbing dan dosen penguji tidak boleh merupakan orang yang sama. Bapak/Ibu diharapkan memilih dosen yang berbeda untuk masing-masing peran (pembimbing dan penguji) terlebih dahulu sebelum melanjutkan.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				return false;
			}
			checkDosens.add(((Dosen) dosen4.getAttribute("myValue")).getId());
		}
		if (dosen5.getAttribute("myValue") != null) {
			if (checkDosens.contains(((Dosen) dosen5.getAttribute("myValue")).getId())) {
				MyMessageboxConfig.show("Mohon maaf, dosen pembimbing dan dosen penguji tidak boleh merupakan orang yang sama. Bapak/Ibu diharapkan memilih dosen yang berbeda untuk masing-masing peran (pembimbing dan penguji) terlebih dahulu sebelum melanjutkan.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				return false;
			}
			checkDosens.add(((Dosen) dosen5.getAttribute("myValue")).getId());
		}

		return true;
	}

	private Tabpanel manajemenSeminar;

	public void onJadwal(Event event) {
		if (manajemenSeminar.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenSeminar);
			MyInclude iframe = new MyInclude("/pages/master/jadwal_seminar_tugas_akhir.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanSeminar;

	public void onTampilSeminar(Event event) {
		if (laporanSeminar.getChildren().size() == 0) {
			LaporanRekapitulasiSeminar laporanRekapitulasiSeminar = new LaporanRekapitulasiSeminar();
			laporanRekapitulasiSeminar.setHeight("100%");
			laporanRekapitulasiSeminar.setWidth("100%");
			laporanRekapitulasiSeminar.setParent(laporanSeminar);
		}
	}

	private Tabpanel laporan1Seminar;

	public void onTampilSeminar1(Event event) {
		if (laporan1Seminar.getChildren().size() == 0) {
			LaporanSeminar laporanRekapitulasiSeminar = new LaporanSeminar();
			laporanRekapitulasiSeminar.setHeight("100%");
			laporanRekapitulasiSeminar.setWidth("100%");
			laporanRekapitulasiSeminar.setParent(laporan1Seminar);
		}
	}

	private Tabpanel formatNilaiTab;

	public void onFormatNilai(Event event) {
		if (formatNilaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(formatNilaiTab);
			MyInclude iframe = new MyInclude("/pages/master/format_nilai_proposal_skripsi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tahapanTab;

	public void onTahapan(Event event) {
		if (tahapanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tahapanTab);
			MyInclude iframe = new MyInclude("/pages/master/tahapan_penyusunan_tugas_akhir.zul");
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
			MyInclude iframe = new MyInclude("/pages/master/komponen_penilaian_proposal_skripsi.zul");
			iframe.setParent(window);
		}
	}

	private JSONArray referensis;

	private static void addReferensi(final JSONObject jsonObject,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, Rows subrowsRefs,
			final EventListener eventListener) throws Exception {
		final Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject,"ref");
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
									JSONArray jsonArray = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());

									for (int ii = 0; ii < jsonArray.length(); ii++) {
										JSONObject o = jsonArray.getJSONObject(ii);
										Long refO = ais.common.CommonJSONUtil.ambilLong(o,"ref");
										if (!refO.equals(ref)) {
											jsonArrayCopy.put(o);
										}
									}

									mahasiswaRequestTugasAkhir.setReferensi(jsonArrayCopy.toString());

									if (mahasiswaRequestTugasAkhir.getId() != null) {
										Common.refreshUpdate(mahasiswaRequestTugasAkhir);
									}

									eventListener.onEvent(new Event("", null, mahasiswaRequestTugasAkhir));
									subrow.detach();
								}

							}
						});

			}
		});
		button.setParent(subrow);
	}

	public static Grid initReferensi(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final EventListener eventListener) throws Exception {

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
		JSONArray referensis = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());
		for (int i = 0; i < referensis.length(); i++) {
			JSONObject jsonObject = referensis.getJSONObject(i);
			addReferensi(jsonObject, mahasiswaRequestTugasAkhir, subrowsRefs, eventListener);
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
						Date tanggalTerbit = null;
						try {
							tanggalTerbit = tanggal.getValue();
						} catch (org.zkoss.zk.ui.WrongValueException tanggalTidakValid) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Terbit / Publikasi",
									"Format tanggal belum benar. Gunakan format dd-MM-yyyy, misalnya 24-08-2026.",
									new String[] { "Perbaiki nilai tanggal yang ditandai pada formulir.",
											"Simpan kembali setelah tanggal valid." });
							return;
						}
						if (tanggalTerbit == null) {
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
						jsonObject.put("tanggal", Common.dateFormat1.get().format(tanggalTerbit));
						jsonObject.put("isbn", isbn.getValue().trim());
						jsonObject.put("issn", issn.getValue().trim());
						jsonObject.put("sumber", sumber.getValue().trim());

						String bibl = CSL.makeAdhocBibliography("apa", Common.convertToCSLItemData(jsonObject))
								.makeString();
						jsonObject.put("bibl", bibl);
						JSONArray jsonArray = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());
						jsonArray.put(jsonObject);
						mahasiswaRequestTugasAkhir.setReferensi(jsonArray.toString());

						if (mahasiswaRequestTugasAkhir.getId() != null) {
							Common.refreshUpdate(mahasiswaRequestTugasAkhir);
						}

						eventListener.onEvent(new Event("", null, mahasiswaRequestTugasAkhir));

						addReferensi(jsonObject, mahasiswaRequestTugasAkhir, subrowsRefs, eventListener);
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
							PesanFormalHelper.tampilkanGagal("penyimpanan data Judul",
									"Kolom Judul belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Judul.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						addWindow.detach();

						JSONObject jsonObject = new JSONObject();

						jsonObject.put("ref", ref);
						jsonObject.put("judul", nama.getValue().trim());

						String bibl = CSL.makeAdhocBibliography("apa", Common.convertToCSLItemData(jsonObject))
								.makeString();
						jsonObject.put("bibl", bibl);
						JSONArray jsonArray = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());
						jsonArray.put(jsonObject);
						mahasiswaRequestTugasAkhir.setReferensi(jsonArray.toString());

						if (mahasiswaRequestTugasAkhir.getId() != null) {
							Common.refreshUpdate(mahasiswaRequestTugasAkhir);
						}

						eventListener.onEvent(new Event("", null, mahasiswaRequestTugasAkhir));

						addReferensi(jsonObject, mahasiswaRequestTugasAkhir, subrowsRefs, eventListener);
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

	public static String[] contents = new String[] { "id", "mahasiswa", "semester", "tahunAkademik", "nama",
			"keterangan", "status", "judul", "jumlahJudul", "judul1", "judul2", "judul3", "judul4", "judul5", "judul6",
			"judul7", "judul8", "judul9", "judul10", "dosen1", "dosen2", "dosen3", "dosen4", "dosen5", "dosen6",
			"nilaiDosen1", "nilaiDosen2", "nilaiDosen3", "nilaiDosen4", "nilaiDosen5", "nilaiDosen6",
			"formatNilaiProposalSkripsi", "tahapanPenyusunanTugasAkhir", "tglSk", "noSk", "lokasiUjian", "formatNilai",
			"tanggalSeminar", "waktuSeminar", "waktuSampaiSeminar", "catatanSeminar", "tanpaPerbaikan",
			"tanggalAwalBimbingan", "tanggalAkhirBimbingan", "jadwalSeminarTugasAkhir", "detailperkuliahan",
			"detailNilai", "lulus", "totalIP", "nilaiHuruf", "totalNilai" };

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
			if (cari3 != null) {
				cari3.setVisible(false);
				if (cari3_2 != null) cari3_2.setVisible(cari3.isVisible());
			}
			if (find != null) {
				find.setVisible(false);
			}

			if (manajemenSeminar != null) {
				manajemenSeminar.getLinkedTab().setVisible(false);
				manajemenSeminar.setVisible(false);
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

			if (tahapanTab != null) {
				tahapanTab.setVisible(false);
				tahapanTab.getLinkedTab().setVisible(false);
			}

		}

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(TahapanAtauCapaianPembelajaran.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			TahapanAtauCapaianPembelajaran tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Proposal");
			tahapanPenyusunanTugasAkhir.setProsentase(40.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Proposal");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s1);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(80.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s1);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setProsentase(100.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s1);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Proposal");
			tahapanPenyusunanTugasAkhir.setProsentase(40.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Proposal");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(80.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setProsentase(100.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Proposal");
			tahapanPenyusunanTugasAkhir.setProsentase(40.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Proposal");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d4);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(80.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d4);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setProsentase(100.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d4);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Proposal");
			tahapanPenyusunanTugasAkhir.setProsentase(40.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Proposal");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s2);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(80.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Tahapan Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s2);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setProsentase(100.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Pendadaran");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s2);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Proposal Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(30.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Proposal Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setProsentase(50.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Laporan Hasil Akhir Disetujui");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Disertasi Dianggap Layak");
			tahapanPenyusunanTugasAkhir.setProsentase(60.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Disertasi Dianggap Layak");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Tertutup");
			tahapanPenyusunanTugasAkhir.setProsentase(80.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Tertutup");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s3);
			session.save(tahapanPenyusunanTugasAkhir);

			tahapanPenyusunanTugasAkhir = new TahapanAtauCapaianPembelajaran();
			tahapanPenyusunanTugasAkhir.setNama("Lulus Ujian Terbuka");
			tahapanPenyusunanTugasAkhir.setProsentase(100.0);
			tahapanPenyusunanTugasAkhir.setKeterangan("Lulus Ujian Terbuka");
			tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.s3);
			session.save(tahapanPenyusunanTugasAkhir);

		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaran(searchta);
		if (searchta != null) { searchta.setSelectedItem(null); }
		MyComboitemConfig comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.REQUEST_STATUS); }
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.AKTIF_STATUS); }
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.SEMINAR_STATUS); }
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.MENGULANG_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.MENGULANG_STATUS); }
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.LULUS_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.LULUS_STATUS); }
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
		if (comboitem != null) { comboitem.setValue(MahasiswaRequestTugasAkhir.GAGAL_STATUS); }
		searchreqstatus.appendChild(comboitem);

		for (int i = 1; i < 20; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			searchsemester.appendChild(comboitem);
		}

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosenPemimbing.setValue(mydosen.getNama());
			searchdosenPemimbing.setAttribute("myValue", mydosen);
			searchdosenPemimbing.setAttribute("dosen", mydosen);
			searchdosenPemimbing.setDisabled(true);
		}

		searchdosenPemimbing.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() != null) {
			searchnim.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setValue(tbmuser.getMahasiswa().getNama());
			searchnim.setDisabled(true);
			searchnama.setDisabled(true);
		}

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MahasiswaRequestTugasAkhir.class, contents);
		upload.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDownloadLampiran(arg0);
			}
		});
		Common.appendKeToolbar(downloadLampiran, add, comp);

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

													List<MahasiswaRequestTugasAkhir> tbmusers = ConstantValues
															.simpleList(initCriteria(true).add(Restrictions.or(
																	Restrictions.eq("status",
																			MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
																	Restrictions.or(Restrictions.eq("status",
																			MahasiswaRequestTugasAkhir.AKTIF_STATUS),
																			Restrictions.eq("status",
																					MahasiswaRequestTugasAkhir.LULUS_STATUS)))),
																	MahasiswaRequestTugasAkhir.class);
													int size = tbmusers.size();
													int index = 1;
													for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : tbmusers) {
														if (mahasiswaRequestTugasAkhir.getMahasiswa() != null
																&& mahasiswaRequestTugasAkhir.getMahasiswa()
																		.getIdRegPd() != null) {
															myLabelProsesDetail.setValue("Memproses "
																	+ mahasiswaRequestTugasAkhir.getMahasiswa().getNim()
																	+ " "
																	+ mahasiswaRequestTugasAkhir.getMahasiswa()
																			.getNama()
																	+ " ("
																	+ Common.numberFormat.get().format((index * 100.0) / size)
																	+ "%");
															index++;
															feederImporter.aktivitasMahasiswaBimbingan(
																	mahasiswaRequestTugasAkhir, errorLog);
														} else {
															errorLog.add("Mahasiswa "
																	+ mahasiswaRequestTugasAkhir.getMahasiswa()
																	+ " belum terdaftar");
														}
													}
													tbmusers.clear();
													tbmusers = null;

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan dari pengguna.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + PesanFormalHelper.pesanGagalException(
																	"pengiriman data pengajuan Tugas Akhir Mahasiswa ke Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
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
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhir = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/proposal_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (MahasiswaRequestTugasAkhir mahasiswa : mahasiswaRequestTugasAkhir) {

					LampiranLain lam = LampiranLain.ambil(mahasiswa.getId(),
							MahasiswaRequestTugasAkhir.class.getName());

					if (lam != null && lam.getGdrive() != null) {
						File fileCopy = new File(
								fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getMahasiswa().getNim() + "_"
										+ mahasiswa.getMahasiswa().getNama() + "_" + lam.getJenis() + ".txt");
						ais.common.BacaTulisUtil.tulis(fileCopy, lam.forwardGDriveUrl());
					} else if (lam != null) {
						File file = lam.ambilFile();
						if (file == null || !file.exists() || !file.isFile()) {
							ais.common.ErrorAuditUtil.record(new java.io.FileNotFoundException(
									file == null ? "Lampiran tugas akhir tidak ditemukan" : file.getAbsolutePath()),
									"MahasiswaRequestTugasAkhirAction.downloadLampiran:skip-file-hilang");
							continue;
						}
						File fileCopy = new File(
								fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getMahasiswa().getNim() + "_"
										+ mahasiswa.getMahasiswa().getNama() + "_" + file.getName());
						System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
						File parentDir = fileCopy.getParentFile();
						if (parentDir != null && !parentDir.exists()) {
							parentDir.mkdirs();
						}
						FileOutputStream fileOutputStream = null;
						FileInputStream fileInputStream = null;
						try {
							fileOutputStream = new FileOutputStream(fileCopy);
							fileInputStream = new FileInputStream(file);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
						} finally {
							try {
								if (fileInputStream != null) {
									fileInputStream.close();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "MahasiswaRequestTugasAkhirAction.close-input");
							}
							try {
								if (fileOutputStream != null) {
									fileOutputStream.close();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "MahasiswaRequestTugasAkhirAction.close-output");
							}
						}
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

	protected AktifitasTugasAkhirHelper aktifitasTugasAkhirHelper = new AktifitasTugasAkhirHelper();
	private AmbilJadwalSeminarTugasAkhirBanbox jadwalSeminarTugasAkhir;
	private Row rowSeminar;
	private Row rowTanggalAwalBimbingan;
	private Row rowTanggalAkhirBimbingan;
	private Row rowFormat;
	private Row rowTahapan;
	private Combobox tahapanPenyusunanTugasAkhir;
	private Textbox judul2;
	private Textbox judul3;
	private Textbox judul4;
	private Textbox judul5;
	private Textbox judul6;
	private Textbox judul1;
	private Combobox jumlahJudul;
	private Textbox judul7;
	private Textbox judul8;
	private Textbox judul9;
	private Textbox judul10;
	private Textbox noSk;
	// private MyDatebox tanggalSeminar;
	// private Row rowTanggalSeminar;
	// private Timebox waktuSeminar;
	// private Timebox waktuSampaiSeminar;
	// private Row rowCatatanSeminar;
	// private Textbox catatanSeminar;
	// private Row rowTanpaPerbaikan;
	// private MyCheckboxConfig tanpaPerbaikan;
	private MyDatebox tglSk;
	private Textbox lokasiUjian;
	private Textbox feeder;
	private DisposisiSop disposisiSop;
	private boolean persetujuan;

	public static Hbox tampilkanInfoMahasiswa(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final EventListener eventListener) throws Exception {
		Hbox hbox = new Hbox();

		Mahasiswa mahasiswa = mahasiswaRequestTugasAkhir.getMahasiswa();
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vbox = new Vbox();

		vbox.setParent(hbox);

		new Label(mahasiswaRequestTugasAkhir.getMahasiswa().getNim()).setParent(vbox);
		new Label(mahasiswaRequestTugasAkhir.getMahasiswa().getNama()).setParent(vbox);

		new MyLabelKecil(
				"TA/Smt/Jenis : " + mahasiswaRequestTugasAkhir.getTahunAkademik() + " / "
						+ mahasiswaRequestTugasAkhir.getSemester() + " / "
						+ (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
								: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama()))
				.setParent(vbox);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (eventListener != null && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && tbmuser.getBiodataCalonMahasiswa() == null) {
			final Radiogroup status = new Radiogroup();
			EventListener event = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final String statuspilih = status.getSelectedItem() == null
							? MahasiswaRequestTugasAkhir.REQUEST_STATUS
							: status.getSelectedItem().getLabel();

					if (mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null
							&& (statuspilih.equalsIgnoreCase(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
									|| statuspilih.equalsIgnoreCase(MahasiswaRequestTugasAkhir.SEMINAR_STATUS))) {

						boolean admin = Common.getApakahAdmin();
						if (admin) {
							MyMessageboxConfig.show(
									"Tanggal awal bimbingan belum diisi, apakah ingin menentukan awal bimbingan per hari ini ?",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {

												mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(new Date());

												Calendar calendar = WaktuUtil.getCalendar();
												calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
												mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(calendar.getTime());

												mahasiswaRequestTugasAkhir.setStatus(statuspilih);
												Common.refreshUpdate(mahasiswaRequestTugasAkhir);
												Common.createDefaultTimer(eventListener);
											} else {
												mahasiswaRequestTugasAkhir.setStatus(statuspilih);
												Common.refreshUpdate(mahasiswaRequestTugasAkhir);
												Common.createDefaultTimer(eventListener);
											}

										}
									});

						} else {
							mahasiswaRequestTugasAkhir.setStatus(statuspilih);
							Common.refreshUpdate(mahasiswaRequestTugasAkhir);
							Common.createDefaultTimer(eventListener);
						}
					} else {
						mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(new Date());

						Calendar calendar = WaktuUtil.getCalendar();
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
						mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(calendar.getTime());

						mahasiswaRequestTugasAkhir.setStatus(statuspilih);
						Common.refreshUpdate(mahasiswaRequestTugasAkhir);
						Common.createDefaultTimer(eventListener);
					}
				}
			};

			Hbox hbox2 = new Hbox();
			hbox2.setParent(vbox);

			hbox2.appendChild(status);
			MyRadioConfig radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.REQUEST_STATUS));
			status.appendChild(radio);
			radio.addEventListener("onClick", event);
			radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS));
			status.appendChild(radio);
			radio.addEventListener("onClick", event);

			radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS));
			status.appendChild(radio);
			radio.addEventListener("onClick", event);

			radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.MENGULANG_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS));
			status.appendChild(radio);

			radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.LULUS_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));
			status.appendChild(radio);
			radio.addEventListener("onClick", event);
			radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
			radio.setStyle("font-size:9px");
			radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
					&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.GAGAL_STATUS));
			status.appendChild(radio);
			radio.addEventListener("onClick", event);

			Skripsi skripsi = mahasiswaRequestTugasAkhir.ambilSkripsi();
			if (skripsi != null && skripsi.getTelahSidang().equals(1)) {
				Common.freeze(status, true);
			}

		} else {
			new MyLabelAgakKecilBold("Status : " + mahasiswaRequestTugasAkhir.getStatus()).setParent(vbox);
		}

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Ubah Pengajuan", "/img/Document-Write-icon.png");
		toolbarbutton.setStyle("font-size:7px;");
		toolbarbutton.setVisible(tbmuser != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.REQUEST_STATUS));
		vbox.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MahasiswaRequestTugasAkhirAction.onAddExternal(eventListener, mahasiswaRequestTugasAkhir);
			}
		});

		if (eventListener != null) {
			Hbox hbox2 = new Hbox();
			hbox2.setVisible(tbmuser != null
					&& (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
							|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
							|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)));
			hbox2.setParent(vbox);

			final AmbilJadwalSeminarTugasAkhirBanbox jadwalSeminarTugasAkhir = new AmbilJadwalSeminarTugasAkhirBanbox();
			jadwalSeminarTugasAkhir.setValue(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir() == null ? ""
					: mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama());
			jadwalSeminarTugasAkhir.setAttribute("jadwalSeminarTugasAkhir",
					mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
			jadwalSeminarTugasAkhir.setAttribute("myValue", mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
			jadwalSeminarTugasAkhir.setWidth("90%");

			hbox2.appendChild(new MyLabelAgakKecilBold("Jadwal Seminar"));

			if (tbmuser.getMahasiswa() != null) {
				hbox2.appendChild(new Label(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir() == null ? ""
						: mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama()));
			} else {
				hbox2.appendChild(jadwalSeminarTugasAkhir);
			}

			hbox2 = new Hbox();
			hbox2.setVisible(tbmuser != null
					&& (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
							|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
							|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)));
			hbox2.setParent(vbox);
			hbox2.appendChild(new ais.ui.util.MyLabelAgakKecilBold("Masa Bimbingan"));
			final MyDatebox tanggalAwalBimbingan = new MyDatebox(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
			tanggalAwalBimbingan.setCols(4);
			if (tbmuser.getMahasiswa() != null) {
				hbox2.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null ? ""
						: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan())));
			} else {
				hbox2.appendChild(tanggalAwalBimbingan);
			}

			hbox2.appendChild(new ais.ui.util.MyLabelAgakKecilBold("s.d"));
			final MyDatebox tanggalAkhirBimbingan = new MyDatebox(
					mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
			tanggalAkhirBimbingan.setCols(4);
			if (tbmuser.getMahasiswa() != null) {
				hbox2.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan() == null ? ""
						: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan())));
			} else {
				hbox2.appendChild(tanggalAkhirBimbingan);
			}

			EventListener ubah = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(tanggalAwalBimbingan.getValue());
					mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(tanggalAkhirBimbingan.getValue());
					mahasiswaRequestTugasAkhir.setJadwalSeminarTugasAkhir(
							(JadwalSeminarTugasAkhir) jadwalSeminarTugasAkhir.getAttribute("jadwalSeminarTugasAkhir"));
					Common.refreshUpdate(mahasiswaRequestTugasAkhir);

					Common.createDefaultTimer(eventListener);
				}
			};

			jadwalSeminarTugasAkhir.setEventListener(ubah);
			tanggalAwalBimbingan.addEventListener("onChange", ubah);
			tanggalAkhirBimbingan.addEventListener("onChange", ubah);

			boolean admin = Common.getApakahAdmin();
			tanggalAwalBimbingan.setDisabled(!admin);
			tanggalAkhirBimbingan.setDisabled(!admin);
		}

		return hbox;
	}

	public static Vbox tampilkanInfoJudul(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) throws Exception {
		Vbox vbox = new Vbox();

		if (mahasiswaRequestTugasAkhir.getJudul().isEmpty()) {
			RevisiHelper.createNewRevisi(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir,
					mahasiswaRequestTugasAkhir.getJudul1()).setParent(vbox);
			if (!mahasiswaRequestTugasAkhir.getJudul2().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul2()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul3().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul3()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul4().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul4()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul5().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul5()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul6().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul6()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul7().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul7()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul8().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul8()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul9().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul9()).setParent(vbox);
			}
			if (!mahasiswaRequestTugasAkhir.getJudul10().isEmpty()) {
				new Label(mahasiswaRequestTugasAkhir.getJudul10()).setParent(vbox);
			}

		} else {
			RevisiHelper.createNewRevisi(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir,
					mahasiswaRequestTugasAkhir.getJudul()).setParent(vbox);
		}

		vbox.appendChild(new MyLabelKecil(
				mahasiswaRequestTugasAkhir.getFeeder() == null ? "" : mahasiswaRequestTugasAkhir.getFeeder()));

		Hbox myvbox = new Hbox();
		myvbox.setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName(), "Proposal", true, null, null, false, false, false, false);

		hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName() + "_Presentasi", "File Presentasi (PPT)", true, null, null,
				false, false, false, false);

		return vbox;
	}

	public static Label tampilkanInfoDosenSimple(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir)
			throws Exception {

		FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = mahasiswaRequestTugasAkhir
				.getFormatNilaiProposalSkripsi();
		if (formatNilaiProposalSkripsi == null) {
			return new Label();
		}

		String data = "";
		if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen1().getNama() + " (" + formatNilaiProposalSkripsi.getDosen1()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen1().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen1() + ")";
		}
		if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen2().getNama() + " (" + formatNilaiProposalSkripsi.getDosen2()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen2().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen2() + ")";
		}
		if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen3().getNama() + " (" + formatNilaiProposalSkripsi.getDosen3()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen3().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen3() + ")";
		}

		if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen4().getNama() + " (" + formatNilaiProposalSkripsi.getDosen4()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen4().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen4() + ")";
		}

		if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen5().getNama() + " (" + formatNilaiProposalSkripsi.getDosen5()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen5().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen5() + ")";
		}

		if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
			data += data.isEmpty()
					? mahasiswaRequestTugasAkhir.getDosen6().getNama() + " (" + formatNilaiProposalSkripsi.getDosen6()
							+ ")"
					: "," + mahasiswaRequestTugasAkhir.getDosen6().getNama() + " ("
							+ formatNilaiProposalSkripsi.getDosen6() + ")";
		}

		return new MyLabelKecil(data);
	}

	public static Vbox tampilkanInfoDosen(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, boolean rinci)
			throws Exception {
		List<CommonVO> dataDosen = mahasiswaRequestTugasAkhir.dataDosen(true);
		return tampilkanInfoDosen(mahasiswaRequestTugasAkhir, rinci, dataDosen);
	}

	public static Vbox tampilkanInfoDosen(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, boolean rinci,
			List<CommonVO> dataDosen) throws Exception {
		Vbox vbox = new Vbox();

		FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = mahasiswaRequestTugasAkhir
				.getFormatNilaiProposalSkripsi();
		if (formatNilaiProposalSkripsi == null) {
			return vbox;
		}

		if (rinci) {
			tampilkanInfoJudul(mahasiswaRequestTugasAkhir).setParent(vbox);
		}

		int tampilPerRow = Common.isMobile() ? 2 : 6;

		Hbox hboxBaru = new Hbox();
		hboxBaru.setParent(vbox);
		int size = 0;

		Tbmuser tbmuser = Common.getCurrentUser();
		if ((mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS))
				&& tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

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
								baruvbox.appendChild(tombolCetakSK(mahasiswaRequestTugasAkhir, key, dosen));
							}
							mahasiswaRequestTugasAkhir.simpanDosen(dosen, key);
							if (arg0 != null && arg0.getTarget() != null) {
								Common.refreshUpdate(mahasiswaRequestTugasAkhir);
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

			TreeMap<String, Dosen> treeMap = mahasiswaRequestTugasAkhir.populateDosen();

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
					myvbox.appendChild(tombolCetakSK(mahasiswaRequestTugasAkhir, key, dosen));
				}
			}
		}

		return vbox;
	}

	public static MyToolbarbuttonConfig tombolCetakSK(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final String jenisPembimbing, final Dosen dosen) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("SK", "/img/print.png");
		button.setTooltiptext("SK");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

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
								parameters.put(i == null ? "foto_dosen" : "foto_dosen_" + i,
										fotobiodataDosen.createLinkUri());
							} else {
								File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
								parameters.put(i == null ? "foto_dosen" : "foto_dosen_" + i, file.getAbsolutePath());
							}

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MahasiswaRequestTugasAkhirAction.java:1798");
						}
					}

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map parameters = ais.common.HashMapGenerator.getRand();

						Common.insertProperty(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir, parameters,
								"bimbingan", 1, "mahasiswa");

						Common.insertProperty(Mahasiswa.class, mahasiswaRequestTugasAkhir.getMahasiswa(), parameters,
								"mahasiswa");

						if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan() != null) {
							Common.insertProperty(Jurusan.class, mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan(),
									parameters, "jurusan");
						}
						if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan() != null
								&& mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas() != null) {
							Common.insertProperty(Fakultas.class,
									mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas(), parameters,
									"fakultas");
						}

						parameters.put("id", mahasiswaRequestTugasAkhir.getId());
						parameters.put("jenis", jenisPembimbing);
						parameters.put("tahun_akademik", mahasiswaRequestTugasAkhir.getTahunAkademik());
						parameters.put("jenis_semester",
								mahasiswaRequestTugasAkhir.getSemester() % 2 == 1 ? Perkuliahan.GANJIL
										: Perkuliahan.GENAP);
						parameters.put("dosen_id", dosen.getId());
						parameters.put("sebagai", jenisPembimbing);
						parameters.put("dosen_pembimbing", dosen.getNama());

						String keyData = "";

						Map<String, Dosen> treeMap = mahasiswaRequestTugasAkhir.populateDosen();
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
//						System.out.println("parameters -> " + parameters);

						Tbmuser tbmuser = Common.getCurrentUser();
						if (tbmuser != null && tbmuser.getMahasiswa() != null) {
							Report.generatePDFReport(Report.PDF, parameters, "SK_Bimbingan_Skripsi",
									ais.ui.util.WaktuUtil.getDate());
						} else {
							Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters, parameters },
									new String[] { "SK_Bimbingan_Skripsi", "SK_Bimbingan_Skripsi_banyak" },
									new String[] { "SK " + jenisPembimbing, "SK Semua Mahasiswa" },
									ais.ui.util.WaktuUtil.getDate());
						}
					}
				});

			}

		});
		return button;
	}

	public static MyToolbarbuttonConfig tombolCetakPengantar(
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pengantar", "/img/print.png");
		button.setTooltiptext("Pengantar");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event event) throws Exception {
				final Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("id", mahasiswaRequestTugasAkhir.getId());

				Common.insertProperty(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir, parameters,
						"bimbingan");

				Report.generatePDFReport(Report.PDF, parameters, "Pengantar_Penelitian_Skripsi",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		return button;
	}

	class MahasiswaRequestTugasAkhirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg1;
			AuditListener.prosesUntukElearning(mahasiswaRequestTugasAkhir, "", mahasiswaRequestTugasAkhir.getId());

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
						aktifitasTugasAkhirHelper.initDetail(mahasiswaRequestTugasAkhir, groupbox);
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
						aktifitasTugasAkhirHelper.initDetail(mahasiswaRequestTugasAkhir, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			MahasiswaRequestTugasAkhirAction.tampilkanInfoMahasiswa(mahasiswaRequestTugasAkhir, null).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(mahasiswaRequestTugasAkhir.getStatus()).setParent(vbox);
			if (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
					|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
					|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
					|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)) {
				MahasiswaRequestTugasAkhirAction.tombolCetakPengantar(mahasiswaRequestTugasAkhir).setParent(vbox);
			}
			new Label(mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir() == null ? ""
					: mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir().getNama() + " ("
							+ Common.numberFormat.get()
									.format(mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir().getProsentase())
							+ " %)")
					.setParent(vbox);

			MahasiswaRequestTugasAkhirAction.tampilkanInfoDosen(mahasiswaRequestTugasAkhir, true).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(
					edit && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(mahasiswaRequestTugasAkhir);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(
					delete && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan lagi. Tekan tombol \"OK\" untuk melanjutkan penghapusan, atau tombol \"Cancel\" untuk membatalkan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(mahasiswaRequestTugasAkhir);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) Pastikan seluruh data yang berkaitan telah dilepas atau dihapus terlebih dahulu; (2) Ulangi proses penghapusan; (3) Apabila kendala masih berlanjut, mohon menghubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			Vbox aksiBox = ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			aksiBox.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}

	}

	public static void onAddExternal(EventListener eventListener, MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir)
			throws Exception {
		MahasiswaRequestTugasAkhirAction mahasiswaRequestTugasAkhirAction = new MahasiswaRequestTugasAkhirAction();
		mahasiswaRequestTugasAkhirAction.eventListener = eventListener;
		mahasiswaRequestTugasAkhirAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(mahasiswaRequestTugasAkhirAction.addWindow);
		mahasiswaRequestTugasAkhirAction.addWindow.setHeight("95%");
		mahasiswaRequestTugasAkhirAction.addWindow.setWidth("750px");

		mahasiswaRequestTugasAkhirAction.init(mahasiswaRequestTugasAkhir);

		mahasiswaRequestTugasAkhirAction.addWindow.setVisible(true);
		mahasiswaRequestTugasAkhirAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new MahasiswaRequestTugasAkhir());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) throws Exception {

		lampiranLains = new HashMap<String, LampiranLain>();
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		addWindow.setTitle("Pengajuan Proposal / Seminar / Kompre Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(mahasiswaRequestTugasAkhir, disposisiSop, save, null));

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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Mohon maaf, data mahasiswa belum diisi. Bapak/Ibu diharapkan memilih mahasiswa terlebih dahulu sebelum menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			mahasiswa.focus();
			return false;
		}
		if (semester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, semester belum dipilih. Bapak/Ibu diharapkan memilih semester terlebih dahulu sebelum menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			mahasiswa.focus();
			return false;
		}

		Mahasiswa mahasiswa = (Mahasiswa) this.mahasiswa.getAttribute("mahasiswa");
		FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = (FormatNilaiProposalSkripsi) (this.formatNilaiProposalSkripsi
				.getSelectedItem() == null ? null : this.formatNilaiProposalSkripsi.getSelectedItem().getValue());
		if (formatNilaiProposalSkripsi == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis pengajuan belum dipilih. Bapak/Ibu diharapkan memilih jenis pengajuan terlebih dahulu sebelum menyimpan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			this.formatNilaiProposalSkripsi.focus();
			return false;
		}

		// Gunakan pilihan dari cbSyaratSebelumnya jika tersedia (user bisa override);
		// jika tidak ada (null dipilih), lewati validasi syarat sebelumnya.
		FormatNilaiProposalSkripsi formatSebelumnya = null;
		if (cbSyaratSebelumnya != null && cbSyaratSebelumnya.getSelectedItem() != null) {
			formatSebelumnya = (FormatNilaiProposalSkripsi) cbSyaratSebelumnya.getSelectedItem().getValue();
		} else {
			formatSebelumnya = formatNilaiProposalSkripsi.ambilSebelumnya();
		}
		if (formatSebelumnya != null) {
			List<String> s = new ArrayList<String>();
			s.add(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
			s.add(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
			s.add(MahasiswaRequestTugasAkhir.LULUS_STATUS);
			Boolean i = checkNamaMahasiswaRequestTugasAkhir(s, formatSebelumnya, mahasiswa);
			if (!i) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum mengajukan \"{V3}\" dengan status {V4}, {V5}, atau {V6}, sehingga belum dapat mendaftar pada pengajuan \"{V7}\". Bapak/Ibu diharapkan menyelesaikan tahapan pengajuan sebelumnya terlebih dahulu.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
						mahasiswa.getNama(), formatSebelumnya.getNama(), MahasiswaRequestTugasAkhir.AKTIF_STATUS,
						MahasiswaRequestTugasAkhir.SEMINAR_STATUS, MahasiswaRequestTugasAkhir.LULUS_STATUS,
						formatNilaiProposalSkripsi.getNama());
				return false;
			}

		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
				(Integer) semester.getSelectedItem().getValue(), null, null);
		int sks = krsMahasiswa.getSksk();
		int batasSks = formatNilaiProposalSkripsi.getMinimalSks();
		if (batasSks > sks) {

			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk dapat mengajukan \"{V1}\", mahasiswa \"{V2}\" terlebih dahulu harus memiliki minimal {V3} SKS, sedangkan SKS yang telah diperoleh baru sebanyak {V4} SKS. Bapak/Ibu diharapkan melengkapi jumlah SKS yang dipersyaratkan sebelum melakukan pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					formatNilaiProposalSkripsi.getNama(), mahasiswa.toString(), batasSks, sks);

			return false;
		}

		if (!checkSyaratPembimbingDanPenguji()) {
			return false;
		}

		Double angkaKredit = Common.hitungAngkaKredit(mahasiswa);
		Double batasAngkaKredit = formatNilaiProposalSkripsi.getMinimalAngkaKredit();

		if (batasAngkaKredit > angkaKredit) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk dapat mengajukan \"{V1}\", mahasiswa \"{V2}\" terlebih dahulu harus memiliki minimal {V3} Angka Kredit kegiatan kemahasiswaan, sedangkan Angka Kredit yang telah diperoleh baru sebanyak {V4}. Bapak/Ibu diharapkan melengkapi Angka Kredit yang dipersyaratkan sebelum melakukan pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					formatNilaiProposalSkripsi.getNama(), mahasiswa.toString(), batasAngkaKredit, angkaKredit);

			return false;
		}

		double ipk = krsMahasiswa.getIpk();
		double batasIpk = formatNilaiProposalSkripsi.getMinimalIpk();

		if (batasIpk > ipk) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk dapat mengajukan \"{V1}\", mahasiswa \"{V2}\" terlebih dahulu harus memiliki IPK minimal {V3}, sedangkan IPK yang telah diperoleh baru {V4}. Bapak/Ibu diharapkan memenuhi ketentuan IPK minimal sebelum melakukan pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					formatNilaiProposalSkripsi.getNama(), mahasiswa.toString(),
					Common.numberFormat.get().format(batasIpk), Common.numberFormat.get().format(ipk));

			return false;
		}

		if (semester.getValue() != null
				&& !Common.checkStatusPembayaranMahasiswaPengajuanSkripsi(formatNilaiProposalSkripsi,
						(Integer) semester.getSelectedItem().getValue(), mahasiswa)) {
			Double harusLunas = formatNilaiProposalSkripsi.getProsentaseLunas();
			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk dapat mengajukan \"{V1}\", mahasiswa \"{V2}\" terlebih dahulu harus melunasi minimal {V3}% biaya perkuliahan pada semester {V4}. Bapak/Ibu diharapkan menghubungi bagian keuangan untuk memperoleh informasi rincian tagihan serta menyelesaikan kewajiban pembayaran, kemudian mengulangi proses pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					formatNilaiProposalSkripsi.getNama(), mahasiswa.toString(), harusLunas, (semester.getValue()));

			return false;
		}

		if (formatNilaiProposalSkripsi.getHarusMengembalikanBukuPerpustakaan()) {

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
				MyMessageboxConfig.showFormat(
						"Mohon maaf, untuk dapat mengajukan \"{V1}\", Bapak/Ibu terlebih dahulu harus mengembalikan seluruh buku perpustakaan yang masih dipinjam. Berikut rincian buku yang belum dikembalikan:\n\n{V2}\nBapak/Ibu diharapkan segera mengembalikan buku-buku tersebut, kemudian mengulangi proses pengajuan.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						formatNilaiProposalSkripsi.getNama(), content);

				return false;
			}

		}

		Mahasiswa mhs = mahasiswa;
		Detailperkuliahan check = null;
		if (!formatNilaiProposalSkripsi.getKodeMatakuliah().trim().isEmpty()) {
			check = Common.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa,
					(Integer) semester.getSelectedItem().getValue(),
					formatNilaiProposalSkripsi.getKodeMatakuliah().trim());
			if (check == null) {
				String mk = "";
				for (String kode : formatNilaiProposalSkripsi.getKodeMatakuliah().split(",")) {
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
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mhs.getNim(),
						mhs.getNama(), mk);
				return false;
			}
		}

		String kodeMatakuliahDanEfektif = KrsDanSkripsiHelper.kodeMatakuliahDanEfektif(
				formatNilaiProposalSkripsi.getKodeMatakuliahDan(),
				formatNilaiProposalSkripsi.getKodeMatakuliah());
		if (!kodeMatakuliahDanEfektif.isEmpty()) {
			Detailperkuliahan checkDan = Common.checkApakahSudahMengambilKrsSeminarSkripsiDan(mahasiswa,
					kodeMatakuliahDanEfektif);
			if (checkDan == null) {
				String mk = "";
				for (String kode : kodeMatakuliahDanEfektif.split(",")) {
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
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mhs.getNim(),
						mhs.getNama(), mk);
				return false;
			}
			if (check == null) {
				check = checkDan;
			}
		}

		if (!formatNilaiProposalSkripsi.getTahunAngkatan().trim().isEmpty() && !formatNilaiProposalSkripsi
				.getTahunAngkatan().trim().contains(mahasiswa.getTahunangkatan().toString())) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tidak dapat mengambil pengajuan \"{V3}\", dikarenakan pengajuan tersebut hanya diperuntukkan bagi angkatan {V4}.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
					mahasiswa.getNama(), formatNilaiProposalSkripsi.getNama(),
					formatNilaiProposalSkripsi.getTahunAngkatan());
			return false;
		}

		if (mhs != null) {
			String kodeItemBiaya = formatNilaiProposalSkripsi.getKodeItemBiaya();
			if (!kodeItemBiaya.trim().isEmpty()) {

				Session session = HibernateUtil.currentSession();
				for (String kode : kodeItemBiaya.trim().split(",")) {
					ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
							.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
					if (itemBiaya != null) {
						Double jumlah = mahasiswa.hitungTotalCicilanPembayaran(krsMahasiswa.getSemester(),
								formatNilaiProposalSkripsi.getSekaliBayar(), null, kode);
						if (jumlah == 0) {
							if (!Common.checkBaypassStatusPembayaranMahasiswa(krsMahasiswa.getSemester(), null,
									mahasiswa, new HashSet<JenisKegiatan>())) {
								MyMessageboxConfig.showFormat(
										"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum melunasi biaya {V3} - {V4}. Untuk dapat melanjutkan pengajuan, Bapak/Ibu diharapkan menghubungi bagian keuangan terlebih dahulu guna menyelesaikan pembayaran biaya tersebut.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										mhs.getNim(), mhs.getNama(), itemBiaya.getKode(), itemBiaya.getNama());
								return false;
							}
						}
					}
				}

			}
		}

		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Bapak/Ibu diharapkan memilih tahun akademik terlebih dahulu sebelum menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (judul1.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Judul I yang diajukan belum diisi. Bapak/Ibu diharapkan mengisi Judul I terlebih dahulu sebelum menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (formatNilaiProposalSkripsi.getAdaProposal()) {
			if (!lampiranLains.keySet().contains(MahasiswaRequestTugasAkhir.class.getName())) {
				MyMessageboxConfig.show("Mohon maaf, berkas Proposal belum diunggah. Bapak/Ibu diharapkan mengunggah berkas Proposal terlebih dahulu sebelum menyimpan pengajuan.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (formatNilaiProposalSkripsi.getAdaPresentasi()) {
			if (!lampiranLains.keySet().contains(MahasiswaRequestTugasAkhir.class.getName() + "_Presentasi")) {
				MyMessageboxConfig.show("Mohon maaf, berkas presentasi belum diunggah. Bapak/Ibu diharapkan mengunggah berkas presentasi terlebih dahulu sebelum menyimpan pengajuan.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (formatNilaiProposalSkripsi.getHanyaBisaDilakukanSekali()) {
			Boolean i = checkNamaMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir.REQUEST_STATUS,
					formatNilaiProposalSkripsi, mahasiswa);
			if (i) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa yang bersangkutan tercatat telah mengajukan \"{V1}\" sebelumnya. Pengajuan ini hanya dapat dilakukan satu kali sehingga pengajuan tidak dapat diproses kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						formatNilaiProposalSkripsi.getNama());
				return false;
			}
		}

		String statuspilih = status.getSelectedItem() == null ? MahasiswaRequestTugasAkhir.REQUEST_STATUS
				: status.getSelectedItem().getLabel();

		if (statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)) {

			if (jadwalSeminarTugasAkhir.getAttribute("jadwalSeminarTugasAkhir") == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, karena status pengajuan telah \"{V1}\", maka Jadwal {V2} wajib diisi terlebih dahulu. Bapak/Ibu diharapkan melengkapi jadwal tersebut sebelum menyimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						MahasiswaRequestTugasAkhir.SEMINAR_STATUS, formatNilaiProposalSkripsi.getNama());
				return false;
			}

			if (tanggalAwalBimbingan.getValue() == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, karena status pengajuan telah \"{V1}\", maka tanggal awal bimbingan wajib diisi terlebih dahulu. Bapak/Ibu diharapkan melengkapi tanggal tersebut sebelum menyimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
				return false;
			}

			if (tanggalAkhirBimbingan.getValue() == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, karena status pengajuan telah \"{V1}\", maka tanggal akhir bimbingan wajib diisi terlebih dahulu. Bapak/Ibu diharapkan melengkapi tanggal tersebut sebelum menyimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
				return false;
			}

			if (judul.getValue().trim().isEmpty()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, karena status pengajuan telah \"{V1}\", maka Judul yang disetujui wajib diisi terlebih dahulu. Bapak/Ibu diharapkan melengkapi judul tersebut sebelum menyimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
						MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
				judul.focus();
				return false;
			}
		}

		String sem = ((Integer) semester.getSelectedItem().getValue()) % 2 == 0 ? Perkuliahan.GENAP
				: Perkuliahan.GANJIL;
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen1.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen2.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen3.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen4.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen5.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}
		if (MahasiswaRequestTugasAkhir.checkMaksSksDosen((Dosen) dosen6.getAttribute("myValue"),
				(String) tahunAkademik.getSelectedItem().getValue(), sem, 1)) {
			return false;
		}

		boolean i = checkNamaMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir.AKTIF_STATUS,
				formatNilaiProposalSkripsi, mahasiswa);
		if (i) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, pengajuan \"{V1}\" untuk mahasiswa ini tercatat telah berstatus {V2}, sehingga pengajuan yang sama tidak dapat diproses kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					formatNilaiProposalSkripsi.getNama(), MahasiswaRequestTugasAkhir.AKTIF_STATUS);
			return false;
		}
		i = checkNamaMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir.SEMINAR_STATUS, formatNilaiProposalSkripsi,
				mahasiswa);
		if (i) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, pengajuan \"{V1}\" untuk mahasiswa ini tercatat telah berstatus {V2}, sehingga pengajuan yang sama tidak dapat diproses kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					formatNilaiProposalSkripsi.getNama(), MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
			return false;
		}
		i = checkNamaMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir.LULUS_STATUS, formatNilaiProposalSkripsi,
				mahasiswa);
		if (i) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, pengajuan \"{V1}\" untuk mahasiswa ini tercatat telah berstatus {V2}, sehingga pengajuan yang sama tidak dapat diproses kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					formatNilaiProposalSkripsi.getNama(), MahasiswaRequestTugasAkhir.LULUS_STATUS);
			return false;
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran1Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan1");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran1() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran1 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran1() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran2Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan2");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran2() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran2 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran2() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran3Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan3");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran3() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran3 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran3() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran4Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan4");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran4() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran4 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran4() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran5Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan5");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran5() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran5 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran5() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran6Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan6");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran6() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran6 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran6() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran7Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan7");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran7() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran7 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran7() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran8Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan8");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran8() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran8 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran8() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran9Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan9");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran9() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran9 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran9() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran10Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan10");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran10() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran10 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran10() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran11Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan11");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran11() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran11 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran11() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran12Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan12");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran12() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran12 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran12() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran13Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan13");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran13() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran13 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran13() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran14Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan14");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran14() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran14 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran14() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran15Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan15");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran15() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran15 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran15() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran16Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan16");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran16() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran16 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran16() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran17Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan17");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran17() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran17 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran17() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran18Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan18");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran18() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran18 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran18() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran19Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan19");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran19() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran19 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran19() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (formatNilaiProposalSkripsi.getUploadLampiran20Wajib()) {
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(mahasiswaRequestTugasAkhir.getId(),
						"rowUploadLampiranPengajuan20");
				if (lam == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran20() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} else {
				if (lainMahasiswaUploadLampiran20 == null) {
					MyMessageboxConfig.show(formatNilaiProposalSkripsi.getUploadLampiran20() + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		MahasiswaRequestTugasAkhirDao mahasiswaRequestTugasAkhirDao = DaoFactory.getInstance()
				.getMahasiswaRequestTugasAkhirDao();
		if (mahasiswaRequestTugasAkhir.getId() != null) {
			mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhirDao.load(mahasiswaRequestTugasAkhir.getId());

		}

		mahasiswaRequestTugasAkhir.setFormatNilaiProposalSkripsi(formatNilaiProposalSkripsi);
		mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(tanggalAwalBimbingan.getValue());
		mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(tanggalAkhirBimbingan.getValue());

		mahasiswaRequestTugasAkhir.setTahapanPenyusunanTugasAkhir(
				(TahapanAtauCapaianPembelajaran) (tahapanPenyusunanTugasAkhir.getSelectedItem() == null ? null
						: tahapanPenyusunanTugasAkhir.getSelectedItem().getValue()));

		if (mahasiswaRequestTugasAkhir.getDetailperkuliahan() == null) {
			mahasiswaRequestTugasAkhir.setDetailperkuliahan(check);
		}

		mahasiswaRequestTugasAkhir.setJadwalSeminarTugasAkhir(
				(JadwalSeminarTugasAkhir) jadwalSeminarTugasAkhir.getAttribute("jadwalSeminarTugasAkhir"));
		mahasiswaRequestTugasAkhir.setStatus(statuspilih);
		mahasiswaRequestTugasAkhir.setMahasiswa(mahasiswa);
		mahasiswaRequestTugasAkhir.setNama(mahasiswaRequestTugasAkhir.getMahasiswa().getNama());
		mahasiswaRequestTugasAkhir.setKeterangan(keterangan.getValue());
		mahasiswaRequestTugasAkhir.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		mahasiswaRequestTugasAkhir.setSemester((Integer) semester.getSelectedItem().getValue());
		mahasiswaRequestTugasAkhir.setJudul(judul.getValue());
		mahasiswaRequestTugasAkhir.setDosen1((Dosen) dosen1.getAttribute("dosen"));
		mahasiswaRequestTugasAkhir.setDosen2((Dosen) dosen2.getAttribute("dosen"));
		mahasiswaRequestTugasAkhir.setDosen3((Dosen) dosen3.getAttribute("dosen"));
		mahasiswaRequestTugasAkhir.setDosen4((Dosen) dosen4.getAttribute("dosen"));
		mahasiswaRequestTugasAkhir.setDosen5((Dosen) dosen5.getAttribute("dosen"));
		mahasiswaRequestTugasAkhir.setDosen6((Dosen) dosen6.getAttribute("dosen"));

		mahasiswaRequestTugasAkhir.setJudul1(judul1.getValue());
		mahasiswaRequestTugasAkhir.setJudul2(judul2.getValue());
		mahasiswaRequestTugasAkhir.setJudul3(judul3.getValue());
		mahasiswaRequestTugasAkhir.setJudul4(judul4.getValue());
		mahasiswaRequestTugasAkhir.setJudul5(judul5.getValue());
		mahasiswaRequestTugasAkhir.setJudul6(judul6.getValue());
		mahasiswaRequestTugasAkhir.setJudul7(judul7.getValue());
		mahasiswaRequestTugasAkhir.setJudul8(judul8.getValue());
		mahasiswaRequestTugasAkhir.setJudul9(judul9.getValue());
		mahasiswaRequestTugasAkhir.setJudul10(judul10.getValue());
		mahasiswaRequestTugasAkhir.setJumlahJudul(
				(Integer) (jumlahJudul.getSelectedItem() == null || jumlahJudul.getSelectedItem().getValue() == null ? 1
						: jumlahJudul.getSelectedItem().getValue()));

		mahasiswaRequestTugasAkhir.setReferensi(referensis == null ? null : referensis.toString());

		mahasiswaRequestTugasAkhir.setTglSk(tglSk.getValue());
		mahasiswaRequestTugasAkhir.setNoSk(noSk.getValue());
		mahasiswaRequestTugasAkhir.setLokasiUjian(lokasiUjian.getValue());

		mahasiswaRequestTugasAkhir.setFeeder(feeder.getValue().trim());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			mahasiswaRequestTugasAkhir.setDisposisiSop(disposisiSop);
		}

		Common.refreshSaveOrUpdate(mahasiswaRequestTugasAkhir);

		for (LampiranLain lain : lampiranLains.values()) {

			Session session = StreamingHibernateUtil.getInstance().currentSession();

			session.refresh(lain);
			lain.setRef(mahasiswaRequestTugasAkhir.getId());

			session.getTransaction().begin();
			session.update(lain);
			session.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();

		}

		if (lainMahasiswaUploadLampiran1 != null && lainMahasiswaUploadLampiran1.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaUploadLampiran1);
				lainMahasiswaUploadLampiran1.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran2.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran2.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran3.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran4.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran5.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran6.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran7.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran8.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran9.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran10.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran11.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran12.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran13.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran14.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran15.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran16.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran17.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran18.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran19.setRef(mahasiswaRequestTugasAkhir.getId());

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
				lainMahasiswaUploadLampiran20.setRef(mahasiswaRequestTugasAkhir.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaUploadLampiran20);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (eventListener != null) {
			eventListener.onEvent(new Event("", null, mahasiswaRequestTugasAkhir));
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Dosen dosenPemimbing = (Dosen) searchdosenPemimbing.getAttribute("myValue");

		Criterion criterion = Restrictions.eq("dosen1", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosenPemimbing));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaRequestTugasAkhir.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(tbmuser.getMahasiswa() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))

				.add(searchreqstatus.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchreqstatus.getSelectedItem().getValue()))

				.add(searchsemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion)

				.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("judul", searchjudul.getValue(), MatchMode.ANYWHERE))
				.createCriteria("mahasiswa")
				.add(!searchdosen.getValue().trim().isEmpty() ? Restrictions.sqlRestriction(
						"this_.mahasiswa in (select aaa.id from mahasiswa aaa inner join dosen bbb on (aaa.dosen=bbb.id) where bbb.nama ilike '%"
								+ searchdosen.getValue().trim() + "%')")
						: Restrictions.sqlRestriction("1=1"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchdosen == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhir = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(mahasiswaRequestTugasAkhir);
		grid.setRowRenderer(new MahasiswaRequestTugasAkhirRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaMahasiswaRequestTugasAkhir(List<String> statuspilih,
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Mahasiswa mahasiswa) {

		Criterion c = Restrictions.sqlRestriction("false");
		for (String status : statuspilih) {
			c = Restrictions.or(c, Restrictions.eq("status", status));
		}

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(MahasiswaRequestTugasAkhir.class)
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi)).add(c)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(this.mahasiswaRequestTugasAkhir.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.mahasiswaRequestTugasAkhir.getId()))
				.uniqueResult()).intValue();

		System.out.println("checkNamaMahasiswaRequestTugasAkhir statuspilih -> " + statuspilih + ", " + kotaCount + " "
				+ formatNilaiProposalSkripsi + " " + mahasiswa);

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaMahasiswaRequestTugasAkhir(String statuspilih,
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Mahasiswa mahasiswa) {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(MahasiswaRequestTugasAkhir.class)
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi))
				.add(Restrictions.eq("status", statuspilih)).setProjection(Projections.rowCount())
				.add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(this.mahasiswaRequestTugasAkhir.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.mahasiswaRequestTugasAkhir.getId()))
				.uniqueResult()).intValue();

		System.out.println("checkNamaMahasiswaRequestTugasAkhir statuspilih -> " + statuspilih + ", " + kotaCount + " "
				+ formatNilaiProposalSkripsi + " " + mahasiswa);

		return !kotaCount.equals(0);
	}

	public static DspaceInformation getDspaceTugasAkhir(String cookie,
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) throws Exception {
		Jurusan jurusan = mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan();
		String description = "Bimbingan Tugas Akhir " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Bimbingan Tugas Akhir");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Bimbingan Tugas Akhir " + jurusan.getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_mahasiswaRequestTugasAkhir_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

	}

	public static DspaceInformation getDspace(String cookie, MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir)
			throws Exception {
		JSONObject jsonPost = new JSONObject();
		String info = mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + "-"
				+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama() + " \"" + mahasiswaRequestTugasAkhir.getJudul()
				+ "\"";
		jsonPost.put("name", info);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", "Berisi semua artefak " + info);
		jsonPost.put("shortDescription", "Artefak " + info);
		jsonPost.put("sidebarText", "Artefak " + info);
		return DspaceInformation.dspaceProcess(cookie, mahasiswaRequestTugasAkhir, jsonPost.toString(), true,
				"collections",
				"communities/" + getDspaceTugasAkhir(cookie, mahasiswaRequestTugasAkhir) + "/collections");
	}

	private String ambilTahunAkademikTerpilih() {
		if (tahunAkademik != null && tahunAkademik.getSelectedItem() != null
				&& tahunAkademik.getSelectedItem().getValue() != null) {
			return tahunAkademik.getSelectedItem().getValue().toString();
		}
		if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getTahunAkademik() != null
				&& !mahasiswaRequestTugasAkhir.getTahunAkademik().trim().isEmpty()) {
			return mahasiswaRequestTugasAkhir.getTahunAkademik();
		}
		return Common.getCurrentTahunAkademik();
	}

	private Integer ambilSemesterPengajuanTerpilih(Mahasiswa mahasiswa) {
		if (semester != null && semester.getSelectedItem() != null && semester.getSelectedItem().getValue() != null) {
			return (Integer) semester.getSelectedItem().getValue();
		}
		if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getSemester() != null) {
			return mahasiswaRequestTugasAkhir.getSemester();
		}
		return mahasiswa == null ? null : mahasiswa.currentSemester();
	}

	private HistoryStatusMahasiswa ambilHistoryPengajuan(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return null;
		}
		try {
			return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa,
					ambilTahunAkademikTerpilih(), ambilSemesterPengajuanTerpilih(mahasiswa), true);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private boolean samaData(GeneralValueObject data1, GeneralValueObject data2) {
		if (data1 == null || data2 == null) {
			return false;
		}
		return data1.getId() != null && data1.getId().equals(data2.getId());
	}

	private boolean samaProgram(String programFormat, String programMahasiswa) {
		if (programFormat == null || programFormat.trim().isEmpty()) {
			return false;
		}
		return programMahasiswa != null && programFormat.trim().equalsIgnoreCase(programMahasiswa.trim());
	}

	private boolean cocokTahunAngkatan(String tahunAngkatan, Mahasiswa mahasiswa) {
		return tahunAngkatan == null || tahunAngkatan.trim().isEmpty()
				|| (mahasiswa != null && mahasiswa.getTahunangkatan() != null
						&& tahunAngkatan.trim().contains(mahasiswa.getTahunangkatan().toString()));
	}

	private int skorFilterFormatNilaiProposalSkripsi(FormatNilaiProposalSkripsi format, Mahasiswa mahasiswa,
			Fakultas fakultas, Jurusan jurusan, String programPengajuan, StatusAwalMahasiswa statusAwalPengajuan) {
		if (format == null) {
			return -1;
		}
		int skor = 0;
		if (samaData(format.getJurusan(), jurusan)) {
			skor += 16;
		}
		if (samaData(format.getFakultas(), fakultas)) {
			skor += 8;
		}
		if (samaProgram(format.getProgram(), programPengajuan)) {
			skor += 4;
		}
		if (samaData(format.getStatusAwalMahasiswa(), statusAwalPengajuan)) {
			skor += 2;
		}
		if (format.getTahunAngkatan() != null && !format.getTahunAngkatan().trim().isEmpty()
				&& cocokTahunAngkatan(format.getTahunAngkatan(), mahasiswa)) {
			skor += 1;
		}
		return skor;
	}

	private void urutkanFormatNilaiProposalSkripsi(List<FormatNilaiProposalSkripsi> formats, final Mahasiswa mahasiswa,
			final Fakultas fakultas, final Jurusan jurusan, final String programPengajuan,
			final StatusAwalMahasiswa statusAwalPengajuan) {
		Collections.sort(formats, new java.util.Comparator<FormatNilaiProposalSkripsi>() {
			@Override
			public int compare(FormatNilaiProposalSkripsi f1, FormatNilaiProposalSkripsi f2) {
				int skor1 = skorFilterFormatNilaiProposalSkripsi(f1, mahasiswa, fakultas, jurusan, programPengajuan,
						statusAwalPengajuan);
				int skor2 = skorFilterFormatNilaiProposalSkripsi(f2, mahasiswa, fakultas, jurusan, programPengajuan,
						statusAwalPengajuan);
				if (skor1 != skor2) {
					return skor2 - skor1;
				}
				return f1.getNama().compareToIgnoreCase(f2.getNama());
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {
		this.mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) generalValueObject;
		this.disposisiSop = disposisiSop;
		tbmuser = Common.getCurrentUser();
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rows = new Rows();
		rows.setParent(grid);

		if (tbmuser.getMahasiswa() != null) {
			mahasiswaRequestTugasAkhir.setMahasiswa(tbmuser.getMahasiswa());
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("I. Data Mahasiswa"));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa (*)"));
		mahasiswa = new AmbilDataMahasiswaBanbox(false);
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getMahasiswa() == null ? ""
					: mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + " "
							+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama()));
		} else {
			row.appendChild(mahasiswa);
		}
		mahasiswa.setWidth("90%");
		mahasiswa.setAttribute("mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa());
		mahasiswa.setAttribute("myValue", mahasiswaRequestTugasAkhir.getMahasiswa());
		mahasiswa.setValue(mahasiswaRequestTugasAkhir.getMahasiswa() == null ? ""
				: mahasiswaRequestTugasAkhir.getMahasiswa().toString());
		mahasiswa.setDisabled(tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null);

		rowFormat = new MyFormRow();
		rowFormat.setStyle("border:0px;background: transparent;");
		rowFormat.setParent(rows);
		rowFormat.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pengajuan (*)")));
		formatNilaiProposalSkripsi = new Combobox();
		if (persetujuan) {
			rowFormat.appendChild(new Label(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
					: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama()));
		} else {
			rowFormat.appendChild(formatNilaiProposalSkripsi);
		}
		formatNilaiProposalSkripsi.setWidth("90%");

		final EventListener mhsFormatEvent = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(MahasiswaRequestTugasAkhirAction.this.formatNilaiProposalSkripsi);
				Mahasiswa mahasiswa = (Mahasiswa) MahasiswaRequestTugasAkhirAction.this.mahasiswa
						.getAttribute("mahasiswa");

				Fakultas fakultas = mahasiswa == null ? tbmuser.ambilFakultas() : mahasiswa.getJurusan().getFakultas();
				Jurusan jurusan = mahasiswa == null ? tbmuser.ambilJurusan() : mahasiswa.getJurusan();
				Integer semesterPengajuan = ambilSemesterPengajuanTerpilih(mahasiswa);
				HistoryStatusMahasiswa historyPengajuan = ambilHistoryPengajuan(mahasiswa);
				String programPengajuan = historyPengajuan == null ? HistoryStatusMahasiswa.ambilProgram(mahasiswa,
						semesterPengajuan, mahasiswa == null ? null : mahasiswa.getProgram())
						: historyPengajuan.getProgram();
				StatusAwalMahasiswa statusAwalPengajuan = historyPengajuan == null
						? HistoryStatusMahasiswa.ambilStatusAwal(mahasiswa, semesterPengajuan,
								mahasiswa == null ? null : mahasiswa.getStatusAwalMahasiswa())
						: historyPengajuan.getStatusAwalMahasiswa();

				List<FormatNilaiProposalSkripsi> formatNilaiProposalSkripsis = HibernateUtil.currentSession()
						.createCriteria(FormatNilaiProposalSkripsi.class)

						.add(tbmuser != null && tbmuser.getMahasiswa() != null
								? Restrictions.or(Restrictions.isNull("tidakBolehDipilihMahasiswa"),
										Restrictions.eq("tidakBolehDipilihMahasiswa", false))
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.or(Restrictions.isNull("fakultas"), Restrictions.eq("fakultas", fakultas)))
						.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
						.add(programPengajuan == null || programPengajuan.trim().isEmpty()
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("program"),
										Restrictions.eq("program", programPengajuan)))
						.add(statusAwalPengajuan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("statusAwalMahasiswa"),
										Restrictions.eq("statusAwalMahasiswa", statusAwalPengajuan)))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
				urutkanFormatNilaiProposalSkripsi(formatNilaiProposalSkripsis, mahasiswa, fakultas, jurusan,
						programPengajuan, statusAwalPengajuan);
				for (FormatNilaiProposalSkripsi formatNilaiProposalSkripsi : formatNilaiProposalSkripsis) {

					if (cocokTahunAngkatan(formatNilaiProposalSkripsi.getTahunAngkatan(), mahasiswa)) {
						Comboitem comboitem = new Comboitem(formatNilaiProposalSkripsi.getNama());
						String mk = "";
						for (String kode : formatNilaiProposalSkripsi.getKodeMatakuliah().split(",")) {
							if (!kode.trim().isEmpty()) {
								Object[] nama = (Object[]) HibernateUtil.currentSession()
										.createCriteria(Matakuliah.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
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

						for (String kode : formatNilaiProposalSkripsi.getKodeMatakuliahDan().split(",")) {
							if (!kode.trim().isEmpty()) {
								Object[] nama = (Object[]) HibernateUtil.currentSession()
										.createCriteria(Matakuliah.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
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

						comboitem.setValue(formatNilaiProposalSkripsi);
						comboitem.setDescription(mk);

						MahasiswaRequestTugasAkhirAction.this.formatNilaiProposalSkripsi.appendChild(comboitem);
					}
				}
				Common.selectComboItem(true, formatNilaiProposalSkripsi,
						mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi());
				if (!formatNilaiProposalSkripsi.getChildren().isEmpty()) {
					formatNilaiProposalSkripsi.setSelectedIndex(0);
				}

				if (mahasiswa != null) {
					Common.insertComboDanSemua(tahapanPenyusunanTugasAkhir, new String[] { "nama", "prosentase" },
							"keterangan", TahapanAtauCapaianPembelajaran.class, "== Pilih salah satu tahapan ==",
							Restrictions.eq("jenjang", mahasiswa.getJenjang()));
					Common.selectComboItem(tahapanPenyusunanTugasAkhir,
							mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir());
				}
			}
		};

		formatNilaiProposalSkripsi.setReadonly(true);

		final MyFormRow rowSyaratSebelumnya = new MyFormRow();
		rowSyaratSebelumnya.setParent(rows);
		rowSyaratSebelumnya.appendChild(new ais.ui.util.MyLabelConfig("Syarat pengajuan sebelumnya"));
		cbSyaratSebelumnya = new Combobox();
		cbSyaratSebelumnya.setWidth("90%");
		cbSyaratSebelumnya.setReadonly(true);
		cbSyaratSebelumnya.setTooltiptext("Pilih syarat pengajuan sebelumnya yang harus dipenuhi mahasiswa. Pilih \"Tidak ada\" untuk melewati validasi syarat.");
		Comboitem ciTidakAda = new Comboitem("-- Tidak ada (lewati syarat) --");
		ciTidakAda.setValue(null);
		cbSyaratSebelumnya.appendChild(ciTidakAda);
		rowSyaratSebelumnya.appendChild(cbSyaratSebelumnya);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		tahunAkademik = new Combobox();
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTahunAkademik()));
		} else {
			row.appendChild(tahunAkademik);
		}

		Common.generateTahunAjaranDanSemua(tahunAkademik);
		if (mahasiswaRequestTugasAkhir.getTahunAkademik() != null) {
			Common.selectComboItem(tahunAkademik, mahasiswaRequestTugasAkhir.getTahunAkademik());
		}
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", mhsFormatEvent);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester = new Combobox());

		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getSemester() + ""));
		} else {
			row.appendChild(semester);
		}

		semester.setReadonly(true);

		for (int i = 0; i <= 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		Common.selectComboItem(semester, mahasiswaRequestTugasAkhir.getSemester());
		semester.addEventListener("onChange", mhsFormatEvent);

//		row = new MyFormRow();
//		row.setVisible(tbmuser.getMahasiswa() != null);
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Status (*)"));
//		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getStatus()));

		EventListener event = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (mahasiswaRequestTugasAkhir.getId() != null && arg0 != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(mahasiswaRequestTugasAkhir);
				}

				final String statuspilih = status.getSelectedItem() == null ? MahasiswaRequestTugasAkhir.REQUEST_STATUS
						: status.getSelectedItem().getLabel();
				rowSeminar.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				rowTanggalAwalBimbingan.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				rowTanggalAkhirBimbingan.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				noSk.getParent()
						.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));
				tglSk.getParent()
						.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				lokasiUjian.getParent()
						.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
								|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				rowTahapan.setVisible(statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
						|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

				if (mahasiswaRequestTugasAkhir != null && (mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null
						&& (statuspilih.equalsIgnoreCase(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
								|| statuspilih.equalsIgnoreCase(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)))) {

					boolean admin = Common.getApakahAdmin();
					if (admin) {
						MyMessageboxConfig.show(
								"Tanggal awal bimbingan belum diisi, apakah ingin menentukan awal bimbingan per hari ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {

										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(new Date());

											Calendar calendar = WaktuUtil.getCalendar();
											calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
											mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(calendar.getTime());

											mahasiswaRequestTugasAkhir.setStatus(statuspilih);

											if (mahasiswaRequestTugasAkhir.getId() != null) {
												Common.refreshUpdate(mahasiswaRequestTugasAkhir);
											}

										} else {
											mahasiswaRequestTugasAkhir.setStatus(statuspilih);

											if (mahasiswaRequestTugasAkhir.getId() != null) {
												Common.refreshUpdate(mahasiswaRequestTugasAkhir);
											}
										}
										tanggalAwalBimbingan
												.setValue(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
										tanggalAkhirBimbingan
												.setValue(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
									}
								});

					} else {
						mahasiswaRequestTugasAkhir.setStatus(statuspilih);

						if (mahasiswaRequestTugasAkhir.getId() != null && arg0 != null) {
							Common.refreshUpdate(mahasiswaRequestTugasAkhir);
						}
						tanggalAwalBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
						tanggalAkhirBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
					}
				} else {
					mahasiswaRequestTugasAkhir.setStatus(statuspilih);
					if (mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null) {
						mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(new Date());

						Calendar calendar = WaktuUtil.getCalendar();
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
						mahasiswaRequestTugasAkhir.setTanggalAkhirBimbingan(calendar.getTime());

						if (mahasiswaRequestTugasAkhir.getId() != null && arg0 != null) {
							Common.refreshUpdate(mahasiswaRequestTugasAkhir);
						}
					}

					tanggalAwalBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
					tanggalAkhirBimbingan.setValue(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
				}

			}
		};

		row = new MyFormRow();
		row.setVisible(tbmuser.getMahasiswa() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status (*)"));
		row.appendChild(new Label(mahasiswaRequestTugasAkhir.getStatus()));

		row = new MyFormRow();
		row.setVisible(tbmuser.getMahasiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status (*)"));

		status = new Radiogroup();
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getStatus()));
		} else {
			row.appendChild(status);
		}

		MyRadioConfig radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
		radio.setValue(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
		radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.REQUEST_STATUS));
		status.appendChild(radio);
		radio.addEventListener("onClick", event);

		radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
		radio.setValue(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
		radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS));
		status.appendChild(radio);
		radio.addEventListener("onClick", event);

		radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
		radio.setValue(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
		radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS));
		status.appendChild(radio);
		radio.addEventListener("onClick", event);

		radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.LULUS_STATUS);
		radio.setValue(MahasiswaRequestTugasAkhir.LULUS_STATUS);
		radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));
		status.appendChild(radio);
		radio.addEventListener("onClick", event);
		radio = new MyRadioConfig(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
		radio.setValue(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
		radio.setChecked(mahasiswaRequestTugasAkhir.getStatus() != null
				&& mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.GAGAL_STATUS));
		status.appendChild(radio);
		radio.addEventListener("onClick", event);

		Skripsi skripsi = mahasiswaRequestTugasAkhir.ambilSkripsi();
		if (skripsi != null && skripsi.getTelahSidang().equals(1)) {
			Common.freeze(status, true);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("II. Data Proposal dan Seminar"));

		final MyFormRow rowUpload = new MyFormRow();
		rowUpload.setVisible(false);
		rowUpload.setStyle("border:0px;background: transparent;");
		rowUpload.setParent(rows);
		rowUpload.appendChild(new Label(ais.common.Common.getBahasaConfig("Proposal Pengajuan (*)")));

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("border:0px;background: transparent;");

		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName(), "Proposal", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						lampiranLains.put(MahasiswaRequestTugasAkhir.class.getName(), lainMahasiswa);
					}
				}, lampiranLains, false, false, false, !persetujuan);
		hbox.setParent(rowUpload);

		final Row ket = Common.initKeterangan(rows,
				"Jika file lampiran lebih dari satu file, zip dulu file tersebut kemudian upload");
		ket.setVisible(false);

		final MyFormRow rowUploadFilePpt = new MyFormRow();
		rowUploadFilePpt.setVisible(false);
		rowUploadFilePpt.setStyle("border:0px;background: transparent;");
		rowUploadFilePpt.setParent(rows);
		rowUploadFilePpt.appendChild(new Label(ais.common.Common.getBahasaConfig("Presentasi Pengajuan (*)")));

		hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("border:0px;background: transparent;");

		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName() + "_Presentasi", "File Presentasi (PPT)", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						lampiranLains.put(MahasiswaRequestTugasAkhir.class.getName() + "_Presentasi", lainMahasiswa);
					}
				}, lampiranLains, false, false, false, !persetujuan);
		hbox.setParent(rowUploadFilePpt);

		rowSeminar = new MyFormRow();
		rowSeminar.setStyle("border:0px;background: transparent;");
		rowSeminar.setParent(rows);
		rowSeminar.appendChild(new Label("Jadwal Proposal / Seminar "));

		jadwalSeminarTugasAkhir = new AmbilJadwalSeminarTugasAkhirBanbox();
		jadwalSeminarTugasAkhir.setValue(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir() == null ? ""
				: mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama());
		jadwalSeminarTugasAkhir.setAttribute("jadwalSeminarTugasAkhir",
				mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
		jadwalSeminarTugasAkhir.setAttribute("myValue", mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
		jadwalSeminarTugasAkhir.setWidth("90%");

		if (tbmuser.getMahasiswa() != null || persetujuan) {
			rowSeminar.appendChild(new Label(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir() == null ? ""
					: mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama()));
		} else {
			rowSeminar.appendChild(jadwalSeminarTugasAkhir);
		}

		rowTanggalAwalBimbingan = new MyFormRow();
		rowTanggalAwalBimbingan.setStyle("border:0px;background: transparent;");
		rowTanggalAwalBimbingan.setParent(rows);
		rowTanggalAwalBimbingan.appendChild(new Label(ais.common.Common.getBahasaConfig("Tgl. Awal Bimbingan")));

		tanggalAwalBimbingan = new MyDatebox(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());

		if (tbmuser.getMahasiswa() != null || persetujuan) {
			rowTanggalAwalBimbingan
					.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null ? ""
							: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan())));
		} else {
			rowTanggalAwalBimbingan.appendChild(tanggalAwalBimbingan);
		}

		rowTanggalAkhirBimbingan = new MyFormRow();
		rowTanggalAkhirBimbingan.setStyle("border:0px;background: transparent;");
		rowTanggalAkhirBimbingan.setParent(rows);
		rowTanggalAkhirBimbingan.appendChild(new Label(ais.common.Common.getBahasaConfig("Tgl. Akhir Bimbingan")));

		tanggalAkhirBimbingan = new MyDatebox(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());

		if (tbmuser.getMahasiswa() != null || persetujuan) {
			rowTanggalAkhirBimbingan
					.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan() == null ? ""
							: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan())));
		} else {
			rowTanggalAkhirBimbingan.appendChild(tanggalAkhirBimbingan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK"));
		noSk = new Textbox(mahasiswaRequestTugasAkhir.getNoSk());

		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getNoSk()));
		} else {
			row.appendChild(noSk);
		}

		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		tglSk = new MyDatebox(mahasiswaRequestTugasAkhir.getTglSk());
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTglSk() == null ? ""
					: Common.dateFormat2.get().format(mahasiswaRequestTugasAkhir.getTglSk())));
		} else {
			row.appendChild(tglSk);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Seminar / Ujian"));

		lokasiUjian = new Textbox(mahasiswaRequestTugasAkhir.getLokasiUjian());
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getLokasiUjian()));
		} else {
			row.appendChild(lokasiUjian);
		}
		lokasiUjian.setWidth("90%");

		rowTahapan = new MyFormRow();
		rowTahapan.setStyle("border:0px;background: transparent;");
		rowTahapan.setParent(rows);
		rowTahapan.appendChild(new Label("Tahapan / Progres Penyusunan"));
		tahapanPenyusunanTugasAkhir = new Combobox();
		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir() == null ? ""
					: mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir().getNama()));
		} else {
			row.appendChild(tahapanPenyusunanTugasAkhir);
		}

		tahapanPenyusunanTugasAkhir.setWidth("90%");
		tahapanPenyusunanTugasAkhir.setReadonly(true);

		event.onEvent(null);

		int jmlJdulMax = 10;

		try {
			jmlJdulMax = Integer.parseInt(Common.getKonfigurasi("jml_jdul_max", "10").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaRequestTugasAkhirAction.java:3971");
			// TODO: handle exception
		}

		jumlahJudul = new Combobox();
		for (int i = 1; i <= jmlJdulMax; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			jumlahJudul.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("III. Data Pengajuan Judul"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Judul yang Diajukan"));
		row.appendChild(jumlahJudul);

		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getJumlahJudul() + ""));
		} else {
			row.appendChild(jumlahJudul);
		}

		jumlahJudul.setWidth("90%");
		Common.selectComboItem(jumlahJudul, mahasiswaRequestTugasAkhir.getJumlahJudul());
		jumlahJudul.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul I yang diajukan"));
		row.appendChild(judul1 = new Textbox(mahasiswaRequestTugasAkhir.getJudul1()));

		judul1.setWidth("90%");
		judul1.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul II yang diajukan"));
		row.appendChild(judul2 = new Textbox(mahasiswaRequestTugasAkhir.getJudul2()));
		judul2.setWidth("90%");
		judul2.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul III yang diajukan"));
		row.appendChild(judul3 = new Textbox(mahasiswaRequestTugasAkhir.getJudul3()));
		judul3.setWidth("90%");
		judul3.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul IV yang diajukan"));
		row.appendChild(judul4 = new Textbox(mahasiswaRequestTugasAkhir.getJudul4()));
		judul4.setWidth("90%");
		judul4.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul V yang diajukan"));
		row.appendChild(judul5 = new Textbox(mahasiswaRequestTugasAkhir.getJudul5()));
		judul5.setWidth("90%");
		judul5.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul VI yang diajukan"));
		row.appendChild(judul6 = new Textbox(mahasiswaRequestTugasAkhir.getJudul6()));
		judul6.setWidth("90%");
		judul6.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul VII yang diajukan"));
		row.appendChild(judul7 = new Textbox(mahasiswaRequestTugasAkhir.getJudul7()));
		judul7.setWidth("90%");
		judul7.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul VIII yang diajukan"));
		row.appendChild(judul8 = new Textbox(mahasiswaRequestTugasAkhir.getJudul8()));
		judul8.setWidth("90%");
		judul8.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul IX yang diajukan"));
		row.appendChild(judul9 = new Textbox(mahasiswaRequestTugasAkhir.getJudul9()));
		judul9.setWidth("90%");
		judul9.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul X yang diajukan"));
		row.appendChild(judul10 = new Textbox(mahasiswaRequestTugasAkhir.getJudul10()));
		judul10.setWidth("90%");
		judul10.setRows(2);

		final EventListener jumlahJudulEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer jml = (Integer) (jumlahJudul.getSelectedItem() == null
						|| jumlahJudul.getSelectedItem().getValue() == null ? 1
								: jumlahJudul.getSelectedItem().getValue());

				judul2.getParent().setVisible(jml >= 2);
				judul3.getParent().setVisible(jml >= 3);
				judul4.getParent().setVisible(jml >= 4);
				judul5.getParent().setVisible(jml >= 5);
				judul6.getParent().setVisible(jml >= 6);
				judul7.getParent().setVisible(jml >= 7);
				judul8.getParent().setVisible(jml >= 8);
				judul9.getParent().setVisible(jml >= 9);
				judul10.getParent().setVisible(jml >= 10);

				judul1.setDisabled(persetujuan);
				judul2.setDisabled(persetujuan);
				judul3.setDisabled(persetujuan);
				judul4.setDisabled(persetujuan);
				judul5.setDisabled(persetujuan);
				judul6.setDisabled(persetujuan);
				judul7.setDisabled(persetujuan);
				judul8.setDisabled(persetujuan);
				judul9.setDisabled(persetujuan);
				judul10.setDisabled(persetujuan);
			}
		};

		jumlahJudul.addEventListener("onChange", jumlahJudulEventListener);
		jumlahJudulEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul yang disetujui"));
		judul = new Textbox(mahasiswaRequestTugasAkhir.getJudul());

		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getJudul()));
		} else {
			row.appendChild(judul);
		}

		judul.setReadonly(tbmuser == null || tbmuser.getMahasiswa() != null);
		judul.setWidth("90%");
		judul.setRows(2);

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

				rowSyaratSebelumnya.setVisible(false);
				if (cbSyaratSebelumnya != null) {
					Common.clear(cbSyaratSebelumnya);
					Comboitem ciReset = new Comboitem("-- Tidak ada (lewati syarat) --");
					ciReset.setValue(null);
					cbSyaratSebelumnya.appendChild(ciReset);
				}

				Mahasiswa mahasiswa = (Mahasiswa) MahasiswaRequestTugasAkhirAction.this.mahasiswa
						.getAttribute("mahasiswa");
				FormatNilaiProposalSkripsi format = (FormatNilaiProposalSkripsi) (formatNilaiProposalSkripsi
						.getSelectedItem() == null ? null : formatNilaiProposalSkripsi.getSelectedItem().getValue());
				if (mahasiswa != null && format != null) {

					if (!format.getUploadLampiran1().isEmpty()) {
						rowUploadLampiran1.setVisible(true);
						rowUploadLampiran1.appendChild(new Label(
								format.getUploadLampiran1() + " " + (format.getUploadLampiran1Wajib() ? "*" : "")));
						Hbox hbox = new Hbox();
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan1", format.getUploadLampiran1(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan2", format.getUploadLampiran2(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan3", format.getUploadLampiran3(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan4", format.getUploadLampiran4(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan5", format.getUploadLampiran5(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan6", format.getUploadLampiran6(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan7", format.getUploadLampiran7(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan8", format.getUploadLampiran8(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan9", format.getUploadLampiran9(), false, new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan10", format.getUploadLampiran10(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan11", format.getUploadLampiran11(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan12", format.getUploadLampiran12(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan13", format.getUploadLampiran13(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan14", format.getUploadLampiran14(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan15", format.getUploadLampiran15(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan16", format.getUploadLampiran16(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan17", format.getUploadLampiran17(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan18", format.getUploadLampiran18(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan19", format.getUploadLampiran19(), false,
								new EventListener() {

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
						LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
								"rowUploadLampiranPengajuan20", format.getUploadLampiran20(), false,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										lainMahasiswaUploadLampiran20 = (LampiranLain) arg0.getData();
									}
								}, null, false, false, false, !persetujuan);
						hbox.setParent(rowUploadLampiran20);
					}

					FormatNilaiProposalSkripsi formatSebelumnya = format.ambilSebelumnya();
					// Populate dropdown syarat dengan semua pilihan dari Jenis Pengajuan
					if (cbSyaratSebelumnya != null) {
						Common.clear(cbSyaratSebelumnya);
						Comboitem ciNone = new Comboitem("-- Tidak ada (lewati syarat) --");
						ciNone.setValue(null);
						cbSyaratSebelumnya.appendChild(ciNone);
						for (Object itemObj : MahasiswaRequestTugasAkhirAction.this.formatNilaiProposalSkripsi.getItems()) {
							Comboitem srcItem = (Comboitem) itemObj;
							Comboitem newItem = new Comboitem(srcItem.getLabel());
							newItem.setValue(srcItem.getValue());
							newItem.setDescription(srcItem.getDescription());
							cbSyaratSebelumnya.appendChild(newItem);
						}
						Common.selectComboItem(true, cbSyaratSebelumnya, formatSebelumnya);
					}
					rowSyaratSebelumnya.setVisible(true);

//					formatNilaiProposalSkripsi.setDisabled(true);

					rowUpload.setVisible(format.getAdaProposal());
					ket.setVisible(format.getAdaProposal());

					rowUploadFilePpt.setVisible(format.getAdaPresentasi());

					if (semester.getSelectedItem().getValue().equals(0)) {
						String tahunAkademik = (String) (MahasiswaRequestTugasAkhirAction.this.tahunAkademik
								.getSelectedItem() == null ? null
										: MahasiswaRequestTugasAkhirAction.this.tahunAkademik.getSelectedItem()
												.getValue());
						if (tahunAkademik == null) {
							MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Bapak/Ibu diharapkan memilih tahun akademik terlebih dahulu sebelum melanjutkan.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}
						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
						Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
						Common.selectComboItem(semester, smt);
					}

					MahasiswaRequestTugasAkhirAction.this.mahasiswa.setDisabled(true);

					String statuspilih = status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
							? MahasiswaRequestTugasAkhir.REQUEST_STATUS
							: status.getSelectedItem().getValue().toString();

					if (statuspilih.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
							|| statuspilih.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
							|| statuspilih.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
							|| statuspilih.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)) {
//						MahasiswaRequestTugasAkhirAction.this.formatNilaiProposalSkripsi.setDisabled(true);

						judul1.setDisabled(true);
						judul2.setDisabled(true);
						judul3.setDisabled(true);
						judul4.setDisabled(true);
						judul5.setDisabled(true);
						judul6.setDisabled(true);
						judul7.setDisabled(true);
						judul8.setDisabled(true);
						judul9.setDisabled(true);
						judul10.setDisabled(true);

					} else {

						judul1.setDisabled(false);
						judul2.setDisabled(false);
						judul3.setDisabled(false);
						judul4.setDisabled(false);
						judul5.setDisabled(false);
						judul6.setDisabled(false);
						judul7.setDisabled(false);
						judul8.setDisabled(false);
						judul9.setDisabled(false);
						judul10.setDisabled(false);

//						MahasiswaRequestTugasAkhirAction.this.formatNilaiProposalSkripsi.setDisabled(false);
					}

					judul.setReadonly(tbmuser == null || tbmuser.getMahasiswa() != null);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.appendChild(new MyLabelStyled("IV. Data Dosen Pembimbing"));

					row = new MyFormRow();
					row.setValign("top");
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen1() != null
							&& format.getProsentasiNilaiPembimbing1() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen1()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen1() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen1().getNama()));

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen2() != null
							&& format.getProsentasiNilaiPembimbing2() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen2()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen2() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen2().getNama()));

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen3() != null
							&& format.getProsentasiNilaiPembimbing3() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen3()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen3() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen3().getNama()));

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen4() != null
							&& format.getProsentasiNilaiPenguji1() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen4()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen4() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen4().getNama()));

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen5() != null
							&& format.getProsentasiNilaiPenguji2() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen5()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen5() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen5().getNama()));

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() != null && mahasiswaRequestTugasAkhir.getDosen6() != null
							&& format.getProsentasiNilaiPenguji3() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen6()));
					row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswaRequestTugasAkhir.getDosen6() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen6().getNama()));

					boolean mhsBolehRequestDosen = Common.bolehKonfigurasi("mahasiswa_boleh_memilih_sendiri_dosen_pembimbing_skripsi", Konfigurasi.TIDAK_AKTIF);

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible((mhsBolehRequestDosen || tbmuser.getMahasiswa() == null)
							&& format.getProsentasiNilaiPembimbing1() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen1()));
					dosen1 = new AmbilDataDosenBanbox();
					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen1() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen1().getNama()));
					} else {
						row.appendChild(dosen1);
					}
					dosen1.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen1());
					dosen1.setValue(mahasiswaRequestTugasAkhir.getDosen1() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen1().getNama());
					dosen1.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible((mhsBolehRequestDosen || tbmuser.getMahasiswa() == null)
							&& format.getProsentasiNilaiPembimbing2() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen2()));
					dosen2 = new AmbilDataDosenBanbox();

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen2() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen2().getNama()));
					} else {
						row.appendChild(dosen2);
					}

					dosen2.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen2());
					dosen2.setValue(mahasiswaRequestTugasAkhir.getDosen2() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen2().getNama());
					dosen2.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible((mhsBolehRequestDosen || tbmuser.getMahasiswa() == null)
							&& format.getProsentasiNilaiPembimbing3() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen3()));
					dosen3 = new AmbilDataDosenBanbox();

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen3() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen3().getNama()));
					} else {
						row.appendChild(dosen3);
					}

					dosen3.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen3());
					dosen3.setValue(mahasiswaRequestTugasAkhir.getDosen3() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen3().getNama());
					dosen3.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() == null && format.getProsentasiNilaiPenguji1() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen4()));
					dosen4 = new AmbilDataDosenBanbox();

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen4() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen4().getNama()));
					} else {
						row.appendChild(dosen4);
					}

					dosen4.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen4());
					dosen4.setValue(mahasiswaRequestTugasAkhir.getDosen4() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen4().getNama());
					dosen4.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() == null && format.getProsentasiNilaiPenguji2() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen5()));
					dosen5 = new AmbilDataDosenBanbox();

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen5() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen5().getNama()));
					} else {
						row.appendChild(dosen5);
					}

					dosen5.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen5());
					dosen5.setValue(mahasiswaRequestTugasAkhir.getDosen5() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen5().getNama());
					dosen5.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setVisible(tbmuser.getMahasiswa() == null && format.getProsentasiNilaiPenguji3() > 0.1);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(format.getDosen6()));
					dosen6 = new AmbilDataDosenBanbox();

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getDosen6() == null ? ""
								: mahasiswaRequestTugasAkhir.getDosen6().getNama()));
					} else {
						row.appendChild(dosen6);
					}

					dosen6.setAttribute("dosen", mahasiswaRequestTugasAkhir.getDosen6());
					dosen6.setValue(mahasiswaRequestTugasAkhir.getDosen6() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen6().getNama());
					dosen6.setWidth("90%");

					row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("jenis", true);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
					keterangan = new Textbox(mahasiswaRequestTugasAkhir.getKeterangan() == null ? ""
							: mahasiswaRequestTugasAkhir.getKeterangan());

					if (persetujuan) {
						row.appendChild(new Label(mahasiswaRequestTugasAkhir.getKeterangan()));
					} else {
						row.appendChild(keterangan);
					}

					keterangan.setWidth("90%");
					keterangan.setRows(3);
				}
			}
		};

		formatNilaiProposalSkripsi.addEventListener("onChange", eventListenerUpload);
		status.addEventListener("onChange", eventListenerUpload);
		mahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mhsFormatEvent.onEvent(null);
				eventListenerUpload.onEvent(null);
			}
		});

		mhsFormatEvent.onEvent(null);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerUpload.onEvent(null);
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_daftar_pustaka_di_pengajuan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar Pustaka"));

		referensis = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());
		row.appendChild(MahasiswaRequestTugasAkhirAction.initReferensi(mahasiswaRequestTugasAkhir, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg0.getData();
				referensis = new JSONArray(mahasiswaRequestTugasAkhir.getReferensi());
			}
		}));

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		feeder = new Textbox(mahasiswaRequestTugasAkhir.getFeeder());

		if (persetujuan) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getFeeder()));
		} else {
			row.appendChild(feeder);
		}

		feeder.setWidth("90%");

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Judul";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return mahasiswaRequestTugasAkhir;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return MahasiswaRequestTugasAkhir.class;
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
