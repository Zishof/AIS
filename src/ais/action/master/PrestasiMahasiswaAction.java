package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapPrestasiMahasiswaBerdasarCabang;
import ais.action.master.dashboard.admin.DashboardRekapPrestasiMahasiswaBerdasarKategori;
import ais.action.master.prestasi.DasbordPrestasi;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanPrestasiMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.Html2Text;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CabangPrestasiMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPrestasiMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk prestasi mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchpenyelenggara}, {@code
 * AmbilDataMahasiswaBanbox searchmahasiswa}, {@code MyDatebox searchmulai}, {@code MyDatebox searchsampai};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code getDspace()}, {@code getDspaceTipePrestasiMahasiswa()}, {@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onDasbor()}, {@code
 * onKategoriPrestasiMahasiswa()}, {@code onCabangPrestasiMahasiswa()}, {@code onRekapCabang()}, {@code
 * onRekapKategori()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class PrestasiMahasiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchpenyelenggara;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchta;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchcabangPrestasiMahasiswa;
	private Combobox searchkategoriPrestasiMahasiswa;
	protected AmbilDataDosenBanbox searchdosen;

	private Textbox nama;
	private MyDatebox tanggal; 
	private MyDatebox tanggalSelesai;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox jurusan;
	private Combobox fakultas;
	private Checkbox prestasiLuarKampus;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private AmbilDataDosenBanbox dosenPembina1;
	private AmbilDataDosenBanbox dosenPembina2;

	private PrestasiMahasiswa prestasiMahasiswa;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainMahasiswa;
	private Tbmuser tbmuser;
	private Textbox tempat;
	private Textbox juara;
	private Intbox peringkat;
	private Textbox penyelenggara;
	private Textbox nomorSertifikat;

	private Combobox cabangPrestasiMahasiswa;
	private Combobox kategoriPrestasiMahasiswa;
	private Textbox jumlahPeserta;
	private Textbox capaian;
	private Textbox url;

	private MyToolbarbuttonConfig uploadData;

	private Mahasiswa mhs;
	private Row rowFakultas;
	private Row rowJurusan;

	private Tabpanel tabDasbor;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPrestasi dasbord = new DasbordPrestasi(DasbordPrestasi.Lingkup.MAHASISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Prestasi Mahasiswa",
				"Pencapaian dan penghargaan yang diraih mahasiswa.");
		}
	}

	private Tabpanel kategoriPrestasiMahasiswaTab;

	public void onKategoriPrestasiMahasiswa(Event event) {
		if (kategoriPrestasiMahasiswaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPrestasiMahasiswaTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_prestasi_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangPrestasiMahasiswaTab;

	public void onCabangPrestasiMahasiswa(Event event) {
		if (cabangPrestasiMahasiswaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(cabangPrestasiMahasiswaTab);
			MyInclude iframe = new MyInclude("/pages/master/cabang_prestasi_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangRekapTab;

	public void onRekapCabang(Event event) {
		if (cabangRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiMahasiswaBerdasarCabang window = new DashboardRekapPrestasiMahasiswaBerdasarCabang();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, cabangRekapTab,
				"Rekap per Cabang", "Sebaran prestasi mahasiswa berdasarkan cabang ilmu atau bidang kompetisi.");
		}
	}

	private Tabpanel kategoriRekapTab;
	private Intbox tahun;

	public void onRekapKategori(Event event) {
		if (kategoriRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiMahasiswaBerdasarKategori window = new DashboardRekapPrestasiMahasiswaBerdasarKategori();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kategoriRekapTab,
				"Rekap per Kategori", "Sebaran prestasi mahasiswa berdasarkan kategori kompetisi atau penghargaan.");
		}
	}

	private static String[] contents = new String[] { "id", "mahasiswa", "nama", "namaEn", "tempat", "penyelenggara",
			"juara", "peringkat", "tanggal", "tanggalSelesai", "nomorSertifikat", "cabangPrestasiMahasiswa",
			"kategoriPrestasiMahasiswa", "jumlahPeserta", "capaian", "url", "fakultas", "jurusan", "tahunAkademik",
			"jenisSemester", "tahun", "status", "dosenPembina1", "dosenPembina2", "keterangan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private MyColumnConfig colNama;
	private PrestasiMahasiswa prestasiMahasiswaSelected = null;
	private Textbox namaEn;
	private Combobox jenisAktfitasMahasiswa;
	private Textbox alamat;
	private Textbox noSk;
	private MyDatebox tglSk;
	protected LampiranLain lainMahasiswaSK1;
	protected LampiranLain lainMahasiswaSK2;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getMahasiswa() != null && colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPrestasiMahasiswaTab.getLinkedTab().setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		cabangPrestasiMahasiswaTab.getLinkedTab().setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		cabangRekapTab.getLinkedTab().setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

		kategoriRekapTab.getLinkedTab().setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPrestasiMahasiswa.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Internasional");
			session.save(kategoriPrestasiMahasiswa);

			kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Nasional");
			session.save(kategoriPrestasiMahasiswa);

			kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Regional");
			session.save(kategoriPrestasiMahasiswa);
		}
		count = ((Number) session.createCriteria(KategoriPrestasiMahasiswa.class)
				.add(Restrictions.eq("nama", "Kab/Kota")).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (count == 0) {
			KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Kab/Kota");
			session.save(kategoriPrestasiMahasiswa);

			kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Kecamatan");
			session.save(kategoriPrestasiMahasiswa);

			kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Kampus/Sekolah");
			session.save(kategoriPrestasiMahasiswa);

			kategoriPrestasiMahasiswa = new KategoriPrestasiMahasiswa();
			kategoriPrestasiMahasiswa.setNama("Lain-Lain");
			session.save(kategoriPrestasiMahasiswa);
		}

		count = ((Number) session.createCriteria(CabangPrestasiMahasiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			CabangPrestasiMahasiswa cabangPrestasiMahasiswa = new CabangPrestasiMahasiswa();
			cabangPrestasiMahasiswa.setNama("Seni");
			session.save(cabangPrestasiMahasiswa);

			cabangPrestasiMahasiswa = new CabangPrestasiMahasiswa();
			cabangPrestasiMahasiswa.setNama("Olah Raga");
			session.save(cabangPrestasiMahasiswa);

			cabangPrestasiMahasiswa = new CabangPrestasiMahasiswa();
			cabangPrestasiMahasiswa.setNama("Kejuaraan Ilmiah");
			session.save(cabangPrestasiMahasiswa);

		}

		count = ((Number) session.createCriteria(CabangPrestasiMahasiswa.class)
				.add(Restrictions.eq("nama", "Lain-Lain")).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (count == 0) {
			CabangPrestasiMahasiswa cabangPrestasiMahasiswa = new CabangPrestasiMahasiswa();
			cabangPrestasiMahasiswa.setKode("9");
			cabangPrestasiMahasiswa.setNama("Lain-Lain");
			session.save(cabangPrestasiMahasiswa);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchkategoriPrestasiMahasiswa, "nama", KategoriPrestasiMahasiswa.class);
		Common.insertComboDanSemua(searchcabangPrestasiMahasiswa, "nama", CabangPrestasiMahasiswa.class);

		Comboitem comboitem = new Comboitem(PrestasiMahasiswa.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiMahasiswa.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiMahasiswa.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiMahasiswa.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiMahasiswa.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PrestasiMahasiswa.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiMahasiswa.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PrestasiMahasiswa.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		}

		if (execution.getParameter("prestasi") != null) {
			prestasiMahasiswaSelected = (PrestasiMahasiswa) GeneralValueObject.ambilData(PrestasiMahasiswa.class,
					execution.getParameter("prestasi").toString());
		}

		if (execution.getParameter("jurusan") != null) {
			Jurusan jurusanSelected = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("jurusan")))).uniqueResult();
			Common.selectComboItem(true, searchjurusan, jurusanSelected);

			Common.selectComboItem(true, searchfakultas,
					jurusanSelected == null ? null : jurusanSelected.getFakultas());

		}

		if (execution.getParameter("cabangPrestasiMahasiswa") != null) {
			CabangPrestasiMahasiswa cabangPrestasiMahasiswaSelected = (CabangPrestasiMahasiswa) HibernateUtil
					.currentSession().createCriteria(CabangPrestasiMahasiswa.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("cabangPrestasiMahasiswa"))))
					.uniqueResult();
			Common.selectComboItem(true, searchcabangPrestasiMahasiswa, cabangPrestasiMahasiswaSelected);
		}

		if (execution.getParameter("kategoriPrestasiMahasiswa") != null) {
			KategoriPrestasiMahasiswa kategoriPrestasiMahasiswaSelected = (KategoriPrestasiMahasiswa) HibernateUtil
					.currentSession().createCriteria(KategoriPrestasiMahasiswa.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPrestasiMahasiswa"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPrestasiMahasiswa, kategoriPrestasiMahasiswaSelected);
		}

		if (execution.getParameter("tahunAjaran") != null) {
			String tahunAjaran = execution.getParameter("tahunAjaran");
			Common.selectComboItem(true, searchta, tahunAjaran);
		}

		if (mhs != null) {
			searchmahasiswa.setAttribute("mahasiswa", mhs);
			searchmahasiswa.setDisabled(true);
			searchmahasiswa.setValue(mhs.getNama());
		}

		if (add != null) { add.setVisible(tbmuser != null); }

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)
		// &&
		// tbmuser.getMahasiswa() == null
		// && tbmuser.ambilDosen() == null);
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onDasbor(null);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Lampiran");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				try {

					Object[] objects = (Object[]) arg0.getData();
					PrestasiMahasiswa prestasiMahasiswa = (PrestasiMahasiswa) objects[0];
					// Long id = (Long) objects[1];
					XSSFRow row = (XSSFRow) objects[2];
					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setUnderline(XSSFFont.U_SINGLE);
					hlink_font.setColor(new XSSFColor(Color.BLUE));

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
					hlink_style.setFont(hlink_font);

					XSSFCell cell = row.createCell(contents.length);
					try {
						try {

							LampiranLain fotobiodataCalonMahasiswa = LampiranLain.ambil(prestasiMahasiswa.getId(),
									PrestasiMahasiswa.class.getName());

							if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getGdrive() != null) {
								cell.setCellStyle(hlink_style);
								cell.setCellValue(fotobiodataCalonMahasiswa.getNama());
								String url = fotobiodataCalonMahasiswa.createLinkUri(false);

								XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
										.createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cell.setHyperlink(link);
							} else if (fotobiodataCalonMahasiswa != null) {
								cell.setCellStyle(hlink_style);
								cell.setCellValue(fotobiodataCalonMahasiswa.getNama());
								String url = fotobiodataCalonMahasiswa.createLinkUri();

								XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
										.createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cell.setHyperlink(link);

							} else {
								cell.setCellValue("Tidak ada lampiran");
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PrestasiMahasiswaAction.java:502");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PrestasiMahasiswaAction.java:504");
					// TODO: handle exception
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PrestasiMahasiswa.class, this,
				"Download", "/img/excel.png", columnHeadersAdding, dataAdding, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PrestasiMahasiswa.class, contents);
		upload.setVisible((add != null && add.isVisible()) && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (uploadData != null) { uploadData.setVisible(upload.isVisible()); }

		if (mhs != null) {

		}

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Surat Pendamping Ijazah", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (mhs != null) {
					CommonReportHelper.onCetakSuratPendampingIjazah(mhs);
				} else {
					LaporanPrestasiMahasiswa laporan = new LaporanPrestasiMahasiswa();
					laporan.setTitle("Surat Pendamping Ijazah");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(page.getFirstRoot());
					laporan.onModal();

				}
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("prestasi_mahasiswa_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
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
							String cookie = DspaceCommon.login();
							List<PrestasiMahasiswa> prestasiMahasiswas = initCriteria(true)
									// alias "mahasiswa" SUDAH dibuat di initCriteria(true) -> jangan buat lagi
									// (createAlias kedua memicu "duplicate alias: mahasiswa").
									.add(Restrictions.isNotNull("mahasiswa.jurusan"))
									.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI)).list();

							int rowIndex = 1;
							for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {
								label.setValue("Sedang memproses data " + prestasiMahasiswa.toString() + " ("
										+ Common.numberFormat.get().format((rowIndex++) * 100.0 / prestasiMahasiswas.size())
										+ " %)");
								PrestasiMahasiswaAction.getDspace(cookie, prestasiMahasiswa, true);
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
				&& Common.bolehKonfigurasi("prestasi_mahasiswa_terhubung_ke_dspace"));
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
												List<PrestasiMahasiswa> prestasiMahasiswas = initCriteria(true)
														.add(Restrictions.isNotNull("mahasiswa.jurusan"))
														.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI))
														.list();

												int rowIndex = 1;
												for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {
													label.setValue("Sedang memproses data "
															+ prestasiMahasiswa.toString() + " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / prestasiMahasiswas.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(PrestasiMahasiswa.class.getName(),
																	prestasiMahasiswa.getId());
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

													List<PrestasiMahasiswa> tbmusers = initCriteria(true)
															.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI))
															.list();
													int size = tbmusers.size();
													int index = 1;
													for (PrestasiMahasiswa prestasiMahasiswa : tbmusers) {
														myLabelProsesDetail.setValue("Memproses "
																+ prestasiMahasiswa.getKode() + " "
																+ prestasiMahasiswa.getNama() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.aktivitasMahasiswaPrestasi(prestasiMahasiswa,
																errorLog);
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
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data prestasi mahasiswa ke Neo Feeder",
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

	public static DspaceInformation getDspace(String cookie, PrestasiMahasiswa prestasiMahasiswa, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (prestasiMahasiswa.getMahasiswa() != null) {
			nama = prestasiMahasiswa.getMahasiswa().getNama();
		}

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

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(prestasiMahasiswa.getCapaian()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (prestasiMahasiswa.getCabangPrestasiMahasiswa() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", prestasiMahasiswa.getCabangPrestasiMahasiswa().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", prestasiMahasiswa.getNama());
		jsonArray.put(jsonMetadata);

		if (prestasiMahasiswa.getKategoriPrestasiMahasiswa() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", prestasiMahasiswa.getKategoriPrestasiMahasiswa().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", prestasiMahasiswa.getPenyelenggara());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", prestasiMahasiswa.getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", prestasiMahasiswa.getNomorSertifikat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", prestasiMahasiswa.getMahasiswa().getBahasa());
		jsonArray.put(jsonMetadata);

		if (prestasiMahasiswa.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(prestasiMahasiswa.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(prestasiMahasiswa.getId(), PrestasiMahasiswa.class.getName());
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, prestasiMahasiswa,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceTipePrestasiMahasiswa(cookie, prestasiMahasiswa) + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Sertifikat / Lampiran Bukti Prestasi");
		}

		return dspaceInformation;
	}

	public static DspaceInformation getDspaceTipePrestasiMahasiswa(String cookie, PrestasiMahasiswa prestasiMahasiswa)
			throws Exception {
		Jurusan jurusan = prestasiMahasiswa.getMahasiswa().getJurusan();

		String description = "Prestasi mahasiswa untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ prestasiMahasiswa.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Prestasi Mahasiswa");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Prestasi Mahasiswa "
				+ prestasiMahasiswa.getMahasiswa().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_prestasiMahasiswa_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	class PrestasiMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PrestasiMahasiswa prestasiMahasiswa = (PrestasiMahasiswa) arg1;

			try {
				if (prestasiMahasiswaSelected != null
						&& prestasiMahasiswaSelected.getId().equals(prestasiMahasiswa.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PrestasiMahasiswaAction.java:949");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(prestasiMahasiswa.getMahasiswa()).setParent(myvbox);

			new Label(prestasiMahasiswa.getMahasiswa().getNim() + "-" + prestasiMahasiswa.getMahasiswa().getNama())
					.setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PrestasiMahasiswa.class, prestasiMahasiswa,
					prestasiMahasiswa.getNama());

			new Label(prestasiMahasiswa.getNamaEn()).setParent(a);
			new Label(prestasiMahasiswa.getJenisAktfitasMahasiswa() == null ? ""
					: prestasiMahasiswa.getJenisAktfitasMahasiswa().getNama()).setParent(a);
			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, prestasiMahasiswa.getId(),
					PrestasiMahasiswa.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tempat: " + prestasiMahasiswa.getTempat()).setParent(myvbox);
			new MyLabelAgakKecil("Penyelenggara: " + prestasiMahasiswa.getPenyelenggara()).setParent(myvbox);
			new MyLabelAgakKecil("Juara: " + prestasiMahasiswa.getJuara()).setParent(myvbox);
			new MyLabelAgakKecil(
					"Peringkat: " + (prestasiMahasiswa.getPeringkat() == null ? "" : prestasiMahasiswa.getPeringkat()))
					.setParent(myvbox);
			new MyLabelAgakKecil("Tanggal: "
					+ (prestasiMahasiswa.getTanggal() == null ? ""
							: Common.dateFormat1.get().format(prestasiMahasiswa.getTanggal()))
					+ (prestasiMahasiswa.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(prestasiMahasiswa.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil(
					"TA/Smt: " + prestasiMahasiswa.getTahunAkademik() + "/" + prestasiMahasiswa.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Cabang: " + (prestasiMahasiswa.getCabangPrestasiMahasiswa() == null ? ""
					: prestasiMahasiswa.getCabangPrestasiMahasiswa().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Kategori: " + (prestasiMahasiswa.getKategoriPrestasiMahasiswa() == null ? ""
					: prestasiMahasiswa.getKategoriPrestasiMahasiswa().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Jml Peserta: " + prestasiMahasiswa.getJumlahPeserta()).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + prestasiMahasiswa.getUrl()).setParent(myvbox);

			new Label(
					(prestasiMahasiswa.getDosenPembina1() == null ? "" : prestasiMahasiswa.getDosenPembina1().getNama())
							+ (prestasiMahasiswa.getDosenPembina2() == null ? ""
									: ", " + prestasiMahasiswa.getDosenPembina2().getNama()))
					.setParent(arg0);

			new Label(prestasiMahasiswa.getCapaian()).setParent(arg0);

			new Label(prestasiMahasiswa.getNomorSertifikat()).setParent(arg0);
			// Kolom aksi rapi: tombol Ubah/Hapus dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// aksiBoxRef menampung Vbox pembungkus supaya visibilitas grup tetap bisa
			// diubah dari listener Combobox status (perilaku sama dgn Hbox toolbar lama).
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final Vbox[] aksiBoxRef = new Vbox[1];
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
			if (mhs == null && tbmuser != null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PrestasiMahasiswa.BELUM_DIPROSES);
				comboitem.setValue(PrestasiMahasiswa.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiMahasiswa.SEDANG_DIPROSES);
				comboitem.setValue(PrestasiMahasiswa.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiMahasiswa.DISETUJUI);
				comboitem.setValue(PrestasiMahasiswa.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiMahasiswa.DITOLAK);
				comboitem.setValue(PrestasiMahasiswa.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, prestasiMahasiswa.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prestasiMahasiswa.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(prestasiMahasiswa);
						if (aksiBoxRef[0] != null) {
							aksiBoxRef[0].setVisible(!prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
						}

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan.setVisible(prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
						}
						myHbox.setVisible(prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(prestasiMahasiswa.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Luar kampus ? " + (prestasiMahasiswa.getPrestasiLuarKampus() ? "Ya" : "Tidak"))
					.setParent(vbox);
			if (!prestasiMahasiswa.getPrestasiLuarKampus() && prestasiMahasiswa.getFakultas() != null)
				new MyLabelAgakKecil(
						prestasiMahasiswa.getFakultas() == null ? "Semua" : prestasiMahasiswa.getFakultas().getNama())
						.setParent(vbox);
			if (!prestasiMahasiswa.getPrestasiLuarKampus() && prestasiMahasiswa.getJurusan() != null)
				new MyLabelAgakKecil(
						prestasiMahasiswa.getJurusan() == null ? "Semua" : prestasiMahasiswa.getJurusan().getNama())
						.setParent(vbox);

			new Label(prestasiMahasiswa.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(prestasiMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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

											if (prestasiMahasiswaSelected != null && prestasiMahasiswaSelected.getId()
													.equals(prestasiMahasiswa.getId())) {
												prestasiMahasiswaSelected = null;
											}

											Common.refreshDelete(prestasiMahasiswa);
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
			aksiButtons.add(button);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			// Sel aksi ini juga memuat konten NON-tombol (indikator feeder + tombol
			// kirim feeder dgn kondisi visibilitas sendiri), jadi kebab dipasang ke
			// vbox1 (sel yang sudah ada), bukan langsung ke Row.
			aksiBoxRef[0] = ais.ui.util.UIHelper.buatBarisAksi(vbox1, 3, aksiButtons);
			aksiBoxRef[0].setVisible(!prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI) && tbmuser != null);

			/* UBAH 21-08-2026: status feeder dahulu menumpang di sel AKSI yang hanya selebar
			 * tombol kebab, sehingga labelnya terpenggal menurun satu kata per baris. Kini
			 * ditempelkan di bawah sel identitas baris. Bila sel itu tidak ditemukan,
			 * wadah lama tetap dipakai agar keterangannya tidak hilang. */
			org.zkoss.zul.Vbox wadahFeeder = ais.ui.util.UIHelper.selIdentitas(arg0);
			if (wadahFeeder == null) {
				wadahFeeder = vbox1;
			}
			myHbox.setParent(wadahFeeder);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (prestasiMahasiswa.getFeeder() != null && !prestasiMahasiswa.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				buttonTagihan.setVisible(prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
				buttonTagihan.setStyle("font-size:10px; white-space:nowrap;");
				buttonTagihan.setParent(wadahFeeder);
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
																feederConnector, token, null, null, null);
														myLabelProsesDetail
																.setValue("Mengirim data " + prestasiMahasiswa);

														feederImporter.aktivitasMahasiswaPrestasi(prestasiMahasiswa,
																errorLog);

														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														// FIX "gagal diam-diam": sebelumnya exception di sini hanya
														// dicatat ke log admin lalu progres diset "" (=SUKSES
														// palsu) di luar try, menutupi kegagalan dari pengguna.
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue(
																"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																		"pengiriman data prestasi mahasiswa \""
																				+ prestasiMahasiswa + "\" ke Neo Feeder",
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

			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PrestasiMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PrestasiMahasiswa prestasiMahasiswa) throws Exception {
		this.prestasiMahasiswa = prestasiMahasiswa;
		addWindow.setTitle(prestasiMahasiswa.getId() == null ? "Tambah Prestasi Kemahasiswaan" : "Ubah Prestasi Kemahasiswaan");
		Common.clear(addWindow);
		addWindow.setWidth("700px");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan *"));
		row.appendChild(nama = new Textbox(prestasiMahasiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(prestasiMahasiswa.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kejuaraan *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(prestasiMahasiswa.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(prestasiMahasiswa.getTanggalSelesai());
		hbox.appendChild(tanggalSelesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");
		mahasiswa.setReadonly(true);

		if (mhs != null) {
			mahasiswa.setAttribute("mahasiswa", mhs);
			mahasiswa.setDisabled(true);
			mahasiswa.setValue(mhs.getNama());
		} else {
			mahasiswa.setAttribute("mahasiswa", prestasiMahasiswa.getMahasiswa());
			mahasiswa.setValue(
					prestasiMahasiswa.getMahasiswa() == null ? "" : prestasiMahasiswa.getMahasiswa().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina I"));
		row.appendChild(dosenPembina1 = new AmbilDataDosenBanbox());
		dosenPembina1.setAttribute("myValue", prestasiMahasiswa.getDosenPembina1());
		dosenPembina1.setAttribute("dosen", prestasiMahasiswa.getDosenPembina1());
		dosenPembina1.setValue(
				prestasiMahasiswa.getDosenPembina1() == null ? "" : prestasiMahasiswa.getDosenPembina1().getNama());
		dosenPembina1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SK Dosen Pembina I"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiMahasiswa.getId(),
				PrestasiMahasiswa.class.getName() + "_SK Dosen Pembina I", "SK Dosen Pembina I", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaSK1 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina II"));
		row.appendChild(dosenPembina2 = new AmbilDataDosenBanbox());
		dosenPembina2.setAttribute("myValue", prestasiMahasiswa.getDosenPembina2());
		dosenPembina2.setAttribute("dosen", prestasiMahasiswa.getDosenPembina2());
		dosenPembina2.setValue(
				prestasiMahasiswa.getDosenPembina2() == null ? "" : prestasiMahasiswa.getDosenPembina2().getNama());
		dosenPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SK Dosen Pembina II"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiMahasiswa.getId(),
				PrestasiMahasiswa.class.getName() + "_SK Dosen Pembina II", "SK Dosen Pembina II", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaSK2 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Kejuaraan *"));
		row.appendChild(tempat = new Textbox(prestasiMahasiswa.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Juara ke *"));
		row.appendChild(juara = new Textbox(prestasiMahasiswa.getJuara()));
		juara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat"));
		row.appendChild(peringkat = new Intbox(prestasiMahasiswa.getPeringkat()));
		peringkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyelenggara *"));
		row.appendChild(penyelenggara = new Textbox(prestasiMahasiswa.getPenyelenggara()));
		penyelenggara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Prestasi *"));
		row.appendChild(nomorSertifikat = new Textbox(prestasiMahasiswa.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cabang *"));
		row.appendChild(cabangPrestasiMahasiswa = new Combobox());
		Common.insertCombo(cabangPrestasiMahasiswa, "nama", CabangPrestasiMahasiswa.class);
		Common.selectComboItem(cabangPrestasiMahasiswa, prestasiMahasiswa.getCabangPrestasiMahasiswa());
		cabangPrestasiMahasiswa.setWidth("90%");
		cabangPrestasiMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori *"));
		row.appendChild(kategoriPrestasiMahasiswa = new Combobox());
		Common.insertCombo(kategoriPrestasiMahasiswa, "nama", KategoriPrestasiMahasiswa.class);
		Common.selectComboItem(kategoriPrestasiMahasiswa, prestasiMahasiswa.getKategoriPrestasiMahasiswa());
		kategoriPrestasiMahasiswa.setWidth("90%");
		kategoriPrestasiMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Peserta *"));
		row.appendChild(jumlahPeserta = new Textbox(prestasiMahasiswa.getJumlahPeserta()));
		jumlahPeserta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(prestasiMahasiswa.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(prestasiMahasiswa.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah kejuaraan diluar kampus ?"));
		row.appendChild(prestasiLuarKampus = new Checkbox());
		prestasiLuarKampus.setChecked(prestasiMahasiswa.getPrestasiLuarKampus());

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (prestasiMahasiswa.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			prestasiMahasiswa.setFakultas(tbmuser.ambilFakultas());
		}
		rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, prestasiMahasiswa.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, prestasiMahasiswa.getJurusan());

		if (prestasiMahasiswa.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.getMahasiswa().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowFakultas.setVisible(!prestasiLuarKampus.isChecked());
				rowJurusan.setVisible(!prestasiLuarKampus.isChecked());
			}
		};

		prestasiLuarKampus.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, prestasiMahasiswa.getTahunAkademik());

		jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, prestasiMahasiswa.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(prestasiMahasiswa.getTahun()));
		tahun.setWidth("90%");
		tahun.setReadonly(true);

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					tahun.setValue(Integer.parseInt(tahunAkademik.getValue().split("/")[0]));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, prestasiMahasiswa.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			jenisAktfitasMahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi / Alamat"));
		row.appendChild(alamat = new Textbox(prestasiMahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK"));
		row.appendChild(noSk = new Textbox(prestasiMahasiswa.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tglSk = new MyDatebox(prestasiMahasiswa.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(prestasiMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat prestasi *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiMahasiswa.getId(), PrestasiMahasiswa.class.getName(),
				"Lampiran Sertifikat", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut");

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
		Date tanggalMulaiAman;
		Date tanggalSelesaiAman;
		Date tanggalSkAman;
		try {
			tanggalMulaiAman = tanggal.getValue();
			tanggalSelesaiAman = tanggalSelesai.getValue();
			tanggalSkAman = tglSk.getValue();
		} catch (org.zkoss.zk.ui.WrongValueException e) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data prestasi mahasiswa",
					"Ada tanggal yang belum lengkap atau formatnya tidak sesuai. Gunakan format tanggal dd-MM-yyyy.",
					new String[] { "Lengkapi atau kosongkan kembali tanggal yang belum valid.",
							"Ulangi proses penyimpanan setelah semua tanggal berformat dd-MM-yyyy." });
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kejuaraan",
					"Kolom Nama Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalMulaiAman == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Mulai Kejuaraan",
					"Kolom Tanggal Mulai Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Mulai Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalSelesaiAman == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Selesai Kejuaraan",
					"Kolom Tanggal Selesai Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Selesai Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tempat Kejuaraan",
					"Kolom Tempat Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tempat Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (juara.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Juara ke,",
					"Kolom Juara ke, belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Juara ke,.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (penyelenggara.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Penyelenggara",
					"Kolom Penyelenggara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Penyelenggara.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nomorSertifikat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nomor sertifikat kejuaraan",
					"Kolom Nomor sertifikat kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nomor sertifikat kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (cabangPrestasiMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cabang Kejuaraan",
					"Kolom Cabang Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Cabang Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (kategoriPrestasiMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kategori Kejuaraan",
					"Kolom Kategori Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kategori Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jumlahPeserta.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jumlah peserta kejuaraan",
					"Kolom Jumlah peserta kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jumlah peserta kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (capaian.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Capaian kejuaraan",
					"Kolom Capaian kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Capaian kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (!prestasiLuarKampus.isChecked()
				&& (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show(
					"Jika prestasi di dalam kampus, maka data " + Common.getBahasaConfig("Fakultas") + " harus diisi",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		try {

			if (prestasiMahasiswa != null && prestasiMahasiswa.getId() != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", prestasiMahasiswa.getId()))
						.add(Restrictions.eq("jenis", PrestasiMahasiswa.class.getName())).uniqueResult()).intValue();

				StreamingHibernateUtil.getInstance().closeSession();

				if (jumlah == 0) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainMahasiswa == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PrestasiMahasiswaAction.java:1760");
		}

		Session session = HibernateUtil.currentSession();
		if (prestasiMahasiswa.getId() != null) {
			prestasiMahasiswa = (PrestasiMahasiswa) session.load(PrestasiMahasiswa.class, prestasiMahasiswa.getId());

		}

		// private Combobox cabangPrestasiMahasiswa;
		// private Combobox kategoriPrestasiMahasiswa;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		prestasiMahasiswa.setPeringkat(peringkat.getValue());
		prestasiMahasiswa.setCabangPrestasiMahasiswa(
				(CabangPrestasiMahasiswa) (cabangPrestasiMahasiswa.getSelectedItem() == null ? null
						: cabangPrestasiMahasiswa.getSelectedItem().getValue()));
		prestasiMahasiswa.setKategoriPrestasiMahasiswa(
				(KategoriPrestasiMahasiswa) (kategoriPrestasiMahasiswa.getSelectedItem() == null ? null
						: kategoriPrestasiMahasiswa.getSelectedItem().getValue()));
		prestasiMahasiswa.setJumlahPeserta(jumlahPeserta.getValue());
		prestasiMahasiswa.setCapaian(capaian.getValue());
		prestasiMahasiswa.setUrl(url.getValue());

		prestasiMahasiswa.setPrestasiLuarKampus(prestasiLuarKampus.isChecked());
		prestasiMahasiswa.setTanggal(tanggalMulaiAman);
		prestasiMahasiswa.setTanggalSelesai(tanggalSelesaiAman);
		prestasiMahasiswa.setNama(nama.getValue());
		prestasiMahasiswa.setNamaEn(namaEn.getValue());
		prestasiMahasiswa.setTempat(tempat.getValue());
		prestasiMahasiswa.setJuara(juara.getValue());
		prestasiMahasiswa.setNomorSertifikat(nomorSertifikat.getValue());
		prestasiMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		prestasiMahasiswa.setKeterangan(keterangan.getValue());
		prestasiMahasiswa.setPenyelenggara(penyelenggara.getValue());
		prestasiMahasiswa.setTanggal(tanggalMulaiAman);

		prestasiMahasiswa.setDosenPembina1((Dosen) dosenPembina1.getAttribute("dosen"));
		prestasiMahasiswa.setDosenPembina2((Dosen) dosenPembina2.getAttribute("dosen"));

		prestasiMahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		prestasiMahasiswa.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		prestasiMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		prestasiMahasiswa.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		prestasiMahasiswa.setJenisAktfitasMahasiswa(
				(JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa.getSelectedItem() == null ? null
						: jenisAktfitasMahasiswa.getSelectedItem().getValue()));

		prestasiMahasiswa.setAlamat(alamat.getValue());
		prestasiMahasiswa.setNoSk(noSk.getValue());
		prestasiMahasiswa.setTglSk(tanggalSkAman);

		Common.refreshSaveOrUpdate(session, prestasiMahasiswa);

		if (lainMahasiswaSK1 != null && lainMahasiswaSK1.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaSK1);
				lainMahasiswaSK1.setRef(prestasiMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaSK1);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (lainMahasiswaSK2 != null && lainMahasiswaSK2.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaSK2);
				lainMahasiswaSK2.setRef(prestasiMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaSK2);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(prestasiMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionDosenPa = Restrictions.sqlRestriction("true");
		if (searchdosen != null && searchdosen.getAttribute("dosen") != null) {
			Dosen dsn = (Dosen) searchdosen.getAttribute("dosen");
			String sql = "this_.mahasiswa in (select id from mahasiswa where dosen=" + dsn.getId() + ")";
			criterionDosenPa = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PrestasiMahasiswa.class).createAlias("mahasiswa", "mahasiswa")
				.createAlias("mahasiswa.jurusan", "jurusan").add(criterionDosenPa)

				.add((searchmahasiswa == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmahasiswa.getAttribute("mahasiswa") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa", searchmahasiswa.getAttribute("mahasiswa"))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchmulai.getValue())))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchsampai.getValue())));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchpenyelenggara.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("penyelenggara", searchpenyelenggara.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchcabangPrestasiMahasiswa.getSelectedItem() == null
						|| searchcabangPrestasiMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("cabangPrestasiMahasiswa",
										searchcabangPrestasiMahasiswa.getSelectedItem().getValue()))

				.add(searchkategoriPrestasiMahasiswa.getSelectedItem() == null
						|| searchkategoriPrestasiMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPrestasiMahasiswa",
										searchkategoriPrestasiMahasiswa.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getMahasiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getMahasiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PrestasiMahasiswa> myPrestasiMahasiswas;

		if (prestasiMahasiswaSelected != null) {
			myPrestasiMahasiswas = new ArrayList<PrestasiMahasiswa>();
			myPrestasiMahasiswas.add(prestasiMahasiswaSelected);
			myPrestasiMahasiswas.addAll(initCriteria(true).add(Restrictions.ne("id", prestasiMahasiswaSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPrestasiMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPrestasiMahasiswas);
		grid.setRowRenderer(new PrestasiMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
