package ais.action.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.Mahasiswa;
import ais.database.model.OrangTua;
import ais.database.model.Pegawai;
import ais.database.model.Pekerjaan;
import ais.database.model.Penghasilan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk orang tua. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox
 * keterangan}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaOrangTua()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class OrangTuaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private OrangTua orangTua;
	private MyToolbarbuttonConfig add;
	private Textbox noKK;
	private Textbox nikAyah;
	private Textbox namaIbu;
	private Textbox namaAyah;
	private Textbox telpAyah;
	private MyDatebox tanggalLahirAyah;
	private Combobox jenisPekerjaanAyah;
	private Combobox jenisPenghasilanAyah;
	private Combobox jenjangPendidikanAyah;
	private Textbox nikIbu;
	private Textbox telpIbu;
	private MyDatebox tanggalLahirIbu;
	private Combobox jenisPekerjaanIbu;
	private Combobox jenisPenghasilanIbu;
	private Combobox jenjangPendidikanIbu;
	private Textbox nikWali;
	private Textbox namaWali;
	private Textbox telpWali;
	private MyDatebox tanggalLahirWali;
	private Combobox jenisPekerjaanWali;
	private Combobox jenisPenghasilanWali;
	private Combobox jenjangPendidikanWali;
	private Textbox alamat;
	private Textbox dusun;
	private Textbox rt;
	private Textbox rw;
	private Textbox kodepos;
	private Textbox kelurahan;
	private AmbilDataKecamatanBanbox kecamatan;

	private Label propinsi;
	private Label kota;

	private PerguruanTinggi perguruanTinggi;

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

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

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

		String[] contents = new String[] { "id", "pegawai", "noKK", "alamat",

				"dusun", "rt", "rw", "kodepos", "kelurahan", "kecamatan", "kota", "propinsi",

				"nikAyah", "namaAyah", "tanggalLahirAyah", "jenisPekerjaanAyah", "jenisPenghasilanAyah",
				"jenjangPendidikanAyah", "telpAyah",

				"nikIbu", "namaIbu", "tanggalLahirIbu", "jenisPekerjaanIbu", "jenisPenghasilanIbu",
				"jenjangPendidikanIbu", "telpIbu",

				"nikWali", "namaWali", "tanggalLahirWali", "jenisPekerjaanWali", "jenisPenghasilanWali",
				"jenjangPendidikanWali", "telpWali",

				"keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(OrangTua.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrangTua.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan Orang Tua Mahasiswa", "/img/excel.png");
		Common.appendKeToolbar(singkron, add, comp);
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_orang_tua_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Angkatan", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Intbox tahunAngkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

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
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan *"));
				row.appendChild(tahunAngkatan);
				tahunAngkatan.setWidth("90%");

				row = new MyFormRow();
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

						if (tahunAngkatan.getValue() == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun angkatan",
									"Kolom Tahun angkatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Tahun angkatan.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Orang Tua Mahasiswa");

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
								List<BiodataMahasiswa> ids =

										ConstantValues.simpleList(

												session.createCriteria(BiodataMahasiswa.class)
														.add(Restrictions.isNotNull("namaAyah"))
														.add(Restrictions.isNotNull("namaIbu"))

														.add(Restrictions.ne("namaAyah", ""))
														.add(Restrictions.ne("namaIbu", ""))

														.createAlias("mahasiswa", "mahasiswa")
														.add(j == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("mahasiswa.jurusan", j))
														.createAlias("mahasiswa.jurusan", "jurusan")
														.add(Restrictions.eq("mahasiswa.tahunangkatan",
																tahunAngkatan.getValue()))
														.add(f == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan.fakultas", f)),
												BiodataMahasiswa.class);
								HibernateUtil.closeSession();
								int i = 1;
								for (BiodataMahasiswa nim : ids) {
									label.setValue("Singkronkan orang tua mahasiswa  " + nim + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciMhs = String.valueOf(nim);
									try {
										Common.checkApakahMahasiswaOtomatisMenjadiOrangTua(nim);
										laporan.catatBerhasil(i - 1, kunciMhs, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, kunciMhs, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-mahasiswa): "
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

		singkron = new MyToolbarbuttonConfig("Singkronkan Orang Tua Siswa", "/img/excel.png");
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_orang_tua_siswa", Konfigurasi.TIDAK_AKTIF));
		Common.appendKeToolbar(singkron, add, comp);
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Angkatan", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Intbox tahunAngkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

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
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan *"));
				row.appendChild(tahunAngkatan);
				tahunAngkatan.setWidth("90%");

				final Combobox yayasan = new Combobox();
				final Combobox sekolah = new Combobox();
				Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
				row.appendChild(yayasan);
				yayasan.setWidth("90%");
				yayasan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
				row.appendChild(sekolah);
				sekolah.setWidth("90%");
				sekolah.setReadonly(true);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan data siswa",
						"/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (tahunAngkatan.getValue() == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun angkatan",
									"Kolom Tahun angkatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Tahun angkatan.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Orang Tua Siswa");

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
								Yayasan f = (Yayasan) (yayasan.getSelectedItem() == null ? null
										: yayasan.getSelectedItem().getValue());
								Sekolah j = (Sekolah) (sekolah.getSelectedItem() == null ? null
										: sekolah.getSelectedItem().getValue());
								Session session = HibernateUtil.currentNativeSession();
								List<Siswa> ids = ConstantValues.simpleList(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
										.add(Restrictions.isNotNull("sekolah"))

										.add(Restrictions.isNotNull("namaAyah")).add(Restrictions.isNotNull("namaIbu"))

										.add(Restrictions.ne("namaAyah", "")).add(Restrictions.ne("namaIbu", ""))

										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(j == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", j))
										.add(Restrictions.eq("tahunMasuk", tahunAngkatan.getValue()))
										.add(f == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("yayasan", f)),
										Siswa.class);
								HibernateUtil.closeSession();
								int i = 1;
								for (Siswa siswa : ids) {
									label.setValue("Singkronkan ortu siswa dengan " + siswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciSiswa = String.valueOf(siswa);
									try {
										Common.checkApakahSiswaOtomatisMenjadiOrangTua(siswa);
										laporan.catatBerhasil(i - 1, kunciSiswa, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, kunciSiswa, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-siswa): "
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

		MyToolbarbuttonConfig generatePasswordOrangTua = new MyToolbarbuttonConfig("Password Orang Tua",
				"/img/print.png");
		generatePasswordOrangTua.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Anda akan mengambil username dan password Orang Tua.", "Informasi",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											String strURL = Common.getKonfigurasi("ambil_kode_url",
													"https://dev.ecampus.id/ecampus/Api").getNilai();

											String link = Common.getRequestHostWithProtocol() + "/Api";
											String nama_pt = perguruanTinggi.getNama();

											HttpServletRequest request = (HttpServletRequest) Executions.getCurrent()
													.getNativeRequest();

											String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
											String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
											String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
											String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request,
															"background_login_perguruanTinggi_");

											final String filename = Sessions.getCurrent().getWebApp()
													.getRealPath("/tmp/user_password_orangTua_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											List<OrangTua> orangTuas = initCriteria(true)
													.add(Restrictions.isNotNull("namaAyah"))
													.add(Restrictions.isNotNull("namaIbu")).setMaxResults(1048576)
													.list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("OrangTua");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username");
											rowhead.createCell(2).setCellValue("Password");
											rowhead.createCell(3).setCellValue("Nama Ayah/Ibu");
											rowhead.createCell(4).setCellValue("Email");
											rowhead.createCell(5).setCellValue("HP");
											rowhead.createCell(6).setCellValue("Nama Anak");
											rowhead.createCell(7).setCellValue("Kode Install Mobile");

											for (OrangTua orangTua : orangTuas) {

												rowIndex++;
												Session session = HibernateUtil.currentNativeSession();

												Tbmuser tbmuser = null;

												try {

													tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("orangTua", orangTua)).setMaxResults(1)
															.uniqueResult();

													System.out.println("tbmuser -> " + tbmuser);

													if (tbmuser == null || tbmuser.getUserId() == null) {

														String newUsername = StringUtils.split(orangTua.getNamaAyah(),
																" ")[0] + "" + RandomStringUtils.randomNumeric(3);
														String passw = RandomStringUtils.randomNumeric(5);
														newUsername = newUsername.toLowerCase().trim();

														int count = ((Number) session.createCriteria(Tbmuser.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.add(Restrictions.eq("userId", newUsername))
																.setProjection(Projections.rowCount()).uniqueResult())
																.intValue();
														if (count == 0) {
															tbmuser = new Tbmuser();
															tbmuser.setUserId(newUsername);
															tbmuser.setEmail(orangTua.getEmailAyah());
															tbmuser.setIs_encripted(true);
															tbmuser.setRoot(false);
															tbmuser.setUserNama(orangTua.getNama());
															tbmuser.setOrangTua(orangTua);
															tbmuser.setUserPassword(
																	Common.desEncrypter.get().encrypt(passw.trim()));
															tbmuser.setUserRole(ConstantValues.roleOrangTua);
															tbmuser.setUserShow(1);

															session.getTransaction().begin();
															session.save(tbmuser);
															session.getTransaction().commit();
														}
													}

													if (tbmuser != null) {
														XSSFRow row = sheet.createRow(rowIndex);
														row.createCell(0).setCellValue(orangTua.getId());
														row.createCell(1).setCellValue(tbmuser.getUserId());

														try {
															row.createCell(2).setCellValue(Common.desEncrypter.get()
																	.decrypt(tbmuser.getUserPassword()));
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														row.createCell(3).setCellValue(
																orangTua.getNamaAyah() + " " + orangTua.getNamaIbu());
														row.createCell(4).setCellValue(orangTua.getEmailAyah());
														row.createCell(5).setCellValue(
																orangTua.getTelpAyah() + " " + orangTua.getTelpIbu());

														String nama = "";

														JSONObject o = new JSONObject(orangTua.getAnak());
														Iterator<String> keys = o.keys();
														while (keys.hasNext()) {
															String key = keys.next();
															if (key.startsWith("siswa")) {
																Siswa siswa = (Siswa) ConstantValues
																		.ambil(Siswa.class.getName(), ais.common.CommonJSONUtil.ambilLong(o,key));
																if (siswa != null) {
																	nama += nama.isEmpty() ? siswa.getNama()
																			: ", " + siswa.getNama();
																}
															} else if (key.startsWith("mahasiswa")) {
																Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(
																		Mahasiswa.class.getName(), ais.common.CommonJSONUtil.ambilLong(o,key));
																if (mahasiswa != null) {
																	nama += nama.isEmpty() ? mahasiswa.getNama()
																			: ", " + mahasiswa.getNama();
																}
															}
														}
														row.createCell(6).setCellValue(nama);

														String hasil = "";
														try {

															String username = tbmuser.getUserId() + ";"
																	+ Common.getRequestHostWithProtocol();

															JSONObject postData = new JSONObject();
															postData.put("username", username);
															postData.put("link", link);
															postData.put("nama_pt", nama_pt);
															postData.put("login_bg_pt",
																	background_login_PerguruanTinggi);
															postData.put("bg_pt", background_PerguruanTinggi);
															postData.put("logo_pt", logo_PerguruanTinggi);
															postData.put("banner_pt", banner_PerguruanTinggi);

															postData.put("motto_pt", perguruanTinggi.getMotto());
															postData.put("alamat_pt", perguruanTinggi.getAlamat1());
															postData.put("telp_pt", perguruanTinggi.getTelepon());
															postData.put("email_pt", perguruanTinggi.getEmail());
															postData.put("action", "code");

															System.out.println("linkPost -> " + strURL);
															System.out.println("postData -> " + postData);

															String[] command = { "curl", "-d", postData.toString(),
																	"-H", "Content-Type: application/json", strURL };

															ProcessBuilder process = new ProcessBuilder(command);
															Process p;
															p = process.start();
															BufferedReader reader = new BufferedReader(
																	new InputStreamReader(p.getInputStream()));
															StringBuilder builder = new StringBuilder();
															String line = null;
															while ((line = reader.readLine()) != null) {
																builder.append(line);
																builder.append(System.getProperty("line.separator"));
															}
															hasil = builder.toString();

															System.out.println(hasil);

															JSONObject jsonObject = new JSONObject(hasil);

															row.createCell(7)
																	.setCellValue(jsonObject.isNull("code") ? ""
																			: jsonObject.get("code") + "");

														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
														}
													}

												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												HibernateUtil.closeSession();
											}

											try {
												FileOutputStream fileOut = new FileOutputStream(filename);
												workbook.write(fileOut);
												fileOut.close();
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}

											try {
												File file = new File(filename);
												Filedownload.save(new FileInputStream(file),
														"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
														file.getName());
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

										}
									});

								}

							}
						});

			}
		});
		Common.appendKeToolbar(generatePasswordOrangTua, add, comp);
	}

	class OrangTuaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OrangTua orangTua = (OrangTua) arg1;
			new Label(orangTua.getNoKK()).setParent(arg0);
			RevisiHelper.createNewRevisi(OrangTua.class, orangTua, orangTua.getNamaAyah()).setParent(arg0);
			new Label(orangTua.getNamaIbu()).setParent(arg0);

			Hbox hb = new Hbox();
			hb.setParent(arg0);

			JSONObject o = new JSONObject(orangTua.getAnak());
			Iterator<String> keys = o.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (key.startsWith("siswa")) {
					Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), ais.common.CommonJSONUtil.ambilLong(o,key));
					if (siswa != null) {
						Vbox vbox = new Vbox();
						vbox.setPack("center");
						vbox.setParent(hb);
						CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
						A a;
						(a = new A(siswa.getNama())).setParent(vbox);
						String code = siswa.urlLogin();
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				} else if (key.startsWith("mahasiswa")) {
					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), ais.common.CommonJSONUtil.ambilLong(o,key));
					if (mahasiswa != null) {
						Vbox vbox = new Vbox();
						vbox.setPack("center");
						vbox.setParent(hb);
						CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
						A a;
						(a = new A(mahasiswa.getNama())).setParent(vbox);
						String code = mahasiswa.urlLogin();
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				}
			}

			new Label(orangTua.getAlamat()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(orangTua.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					orangTua.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(orangTua);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, orangTua, OrangTuaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new OrangTua());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		orangTua = (OrangTua) obj;
		init(orangTua);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private JSONObject jsonObject = null;
	private AmbilDataPegawaiBanbox orangTuaPegawai;

	@SuppressWarnings("deprecation")
	private void init(final OrangTua orangTua) throws Exception {
		this.orangTua = orangTua;
		addWindow.setTitle(orangTua.getId() == null ? "Tambah Orang Tua" : "Ubah Orang Tua");
		Common.clear(addWindow);

		Borderlayout borderlayoutUtama = new Borderlayout();
		borderlayoutUtama.setHeight("100%");
		borderlayoutUtama.setWidth("100%");
		addWindow.appendChild(borderlayoutUtama);

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(centerUtama);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabData = new MyTabConfig("Data Orang Tua");
		tabData.setParent(tabs);

		MyTabConfig tabDataAnak = new MyTabConfig("Data Anak");
		tabDataAnak.setParent(tabs);

		MyTabConfig tabMobile = new MyTabConfig("Mobile");
		tabMobile.setParent(tabs);

		tabMobile.setVisible(Common.getApakahAdmin()
				&& Common.bolehKonfigurasi("tampilkan_mobile_di_profile_orang_tua"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		final Tabpanel tabpanelAnak = new ais.ui.util.MyTabpanel();
		tabpanelAnak.setParent(tabpanels);
		tabDataAnak.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			private void reload(final Vbox vb) throws Exception {
				Common.clear(vb);
				jsonObject = null;

				boolean[] ptYa = Common.chekPtAtauSekolah();
				boolean pt = ptYa[0];
				boolean ya = ptYa[1];

				Hbox hbox = new Hbox();
				hbox.setPack("center");
				hbox.setParent(vb);

				if (ya) {
					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Anak Siswa",
							"/img/user_male_add.png");
					hbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Siswa> siswas = new ArrayList<Siswa>();

							JSONObject o = new JSONObject(orangTua.getAnak());
							Iterator<String> keys = o.keys();
							while (keys.hasNext()) {
								String key = keys.next();
								if (key.startsWith("siswa")) {
									Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), ais.common.CommonJSONUtil.ambilLong(o,key));
									if (siswa != null) {
										siswas.add(siswa);
									}
								}
							}

							AmbilDataSiswaBanyak ambil = new AmbilDataSiswaBanyak(siswas);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
							ambil.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									List<Siswa> siswas = (List<Siswa>) arg0.getData();
									if (siswas != null && siswas.size() != 0) {
										jsonObject = new JSONObject(orangTua.getAnak());
										for (Siswa siswa : siswas) {
											jsonObject.put("siswa_" + siswa.getId(), siswa.getId());
										}
										orangTua.setAnak(jsonObject.toString());
										if (orangTua.getId() != null) {
											Common.refreshSaveOrUpdate(orangTua);
										}
									}
									reload(vb);
								}
							});
							ambil.setWidth("850px");
							ambil.setHeight("97%");
							ambil.setVisible(true);
							ambil.onModal();
						}
					});
				}
				if (pt) {
					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Anak Mahasiswa",
							"/img/user_male_add.png");
					hbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();

							JSONObject o = new JSONObject(orangTua.getAnak());
							Iterator<String> keys = o.keys();
							while (keys.hasNext()) {
								String key = keys.next();
								if (key.startsWith("mahasiswa")) {
									Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
											ais.common.CommonJSONUtil.ambilLong(o,key));
									if (mahasiswa != null) {
										mahasiswas.add(mahasiswa);
									}
								}
							}

							AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(mahasiswas);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
							ambil.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
									if (mahasiswas != null && mahasiswas.size() != 0) {
										jsonObject = new JSONObject(orangTua.getAnak());
										for (Mahasiswa mahasiswa : mahasiswas) {
											jsonObject.put("mahasiswa_" + mahasiswa.getId(), mahasiswa.getId());
										}
										orangTua.setAnak(jsonObject.toString());
										if (orangTua.getId() != null) {
											Common.refreshSaveOrUpdate(orangTua);
										}
									}
									reload(vb);
								}
							});
							ambil.setWidth("850px");
							ambil.setHeight("97%");
							ambil.setVisible(true);
							ambil.onModal();
						}
					});
				}
				jsonObject = new JSONObject(orangTua.getAnak());
				Iterator<String> keys = jsonObject.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key.startsWith("siswa")) {
						final Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObject,key));
						if (siswa != null) {
							hbox = new Hbox();
							hbox.setPack("center");
							hbox.setParent(vb);

							CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
							A a;
							(a = new A(siswa.getNama())).setParent(hbox);
							String code = siswa.urlLogin();
							a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
									+ URLEncoder.encode(code, "UTF-8"));

							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
							button.setTooltiptext("Hapus Data");
							button.setVisible(delete);

							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {
															jsonObject.remove("siswa_" + siswa.getId());
															orangTua.setAnak(jsonObject.toString());
															if (orangTua.getId() != null) {
																Common.refreshSaveOrUpdate(orangTua);
															}
															reload(vb);
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
							button.setParent(hbox);
						}
					} else if (key.startsWith("mahasiswa")) {
						final Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObject,key));
						if (mahasiswa != null) {
							hbox = new Hbox();
							hbox.setPack("center");
							hbox.setParent(vb);
							CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
							A a;
							(a = new A(mahasiswa.getNama())).setParent(hbox);
							String code = mahasiswa.urlLogin();
							a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
									+ URLEncoder.encode(code, "UTF-8"));

							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
							button.setTooltiptext("Hapus Data");
							button.setVisible(delete);

							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {
															jsonObject.remove("mahasiswa_" + mahasiswa.getId());
															orangTua.setAnak(jsonObject.toString());
															if (orangTua.getId() != null) {
																Common.refreshSaveOrUpdate(orangTua);
															}
															reload(vb);
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
							button.setParent(hbox);
						}
					}
				}
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelAnak.getChildren().isEmpty()) {

					Vbox hb = new Vbox();
					hb.setParent(tabpanelAnak);

					reload(hb);
				}

			}
		});

		final Tabpanel tabpanelMobile = new ais.ui.util.MyTabpanel();
		tabpanelMobile.setParent(tabpanels);
		tabpanelMobile.setVisible(tabMobile.isVisible());
		tabMobile.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMobile.getChildren().isEmpty()) {
					if (!onSave(arg0)) {
						tabData.setSelected(true);
					} else {
						Session session = HibernateUtil.currentSession();
						Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("orangTua", orangTua)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult();

						if (tbmuser == null || tbmuser.getUserId() == null) {

							String newUsername = StringUtils.split(orangTua.getNamaAyah(), " ")[0] + ""
									+ RandomStringUtils.randomNumeric(3);
							String passw = RandomStringUtils.randomNumeric(5);
							newUsername = newUsername.toLowerCase().trim();

							tbmuser = new Tbmuser();
							tbmuser.setUserId(newUsername);
							tbmuser.setEmail(orangTua.getEmailAyah());
							tbmuser.setIs_encripted(true);
							tbmuser.setRoot(false);
							tbmuser.setUserNama(orangTua.getNama());
							tbmuser.setOrangTua(orangTua);
							tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
							tbmuser.setUserRole(ConstantValues.roleOrangTua);
							tbmuser.setUserShow(1);

							session.save(tbmuser);
							session.flush();
						}

						MainHelper.onDapatkanKode(tbmuser, tabpanelMobile, false);
					}
				}
			}

		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		tabpanel.appendChild(borderlayout);

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
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("I. Alamat dan Kartu Keluarga"));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Kartu Keluarga (KK)")));
		row.appendChild(noKK = new Textbox(orangTua.getNoKK() == null ? "" : orangTua.getNoKK()));
		noKK.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		MyFormRow parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		Hbox ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, orangTua, LampiranLainMahasiswa.KK, "Kartu Keluarga (KK)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat "));
		row.appendChild(alamat = new Textbox(orangTua.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();

		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Dusun / Kampung"));
		row.appendChild(dusun = new Textbox(orangTua.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT"));
		row.appendChild(rt = new Textbox(orangTua.getRt() == null ? "" : orangTua.getRt()));
		rt.setWidth("90%");
		rt.setMaxlength(3);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW"));
		row.appendChild(rw = new Textbox(orangTua.getRw() == null ? "" : orangTua.getRw()));
		rw.setWidth("90%");
		rw.setMaxlength(3);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(kodepos = new Textbox(orangTua.getKodepos() == null ? "" : orangTua.getKodepos()));
		kodepos.setWidth("90%");
		kodepos.setMaxlength(8);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan / Desa"));
		row.appendChild(kelurahan = new Textbox(orangTua.getKelurahan() == null ? "" : orangTua.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan"));
		row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
		kecamatan.setValue(orangTua.getKecamatan() == null ? "" : orangTua.getKecamatan().getNama());
		kecamatan.setAttribute("wilayah", orangTua.getKecamatan());
		kecamatan.setWidth("90%");
		propinsi = new Label(orangTua.getPropinsi() == null ? "" : orangTua.getPropinsi().getNama());
		kota = new Label(orangTua.getKota() == null ? "" : orangTua.getKota().getNama());

		Common.createFieldKota(rows, "Kota/Kabupaten", kota, propinsi, orangTua.getKota(), false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi"));
		row.appendChild(propinsi);
		propinsi.setWidth("90%");
		propinsi.setAttribute("wilayah", orangTua.getPropinsi());

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsi, kota, kecamatan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anak dari pegawai"));
		row.appendChild(orangTuaPegawai = new AmbilDataPegawaiBanbox(false));
		orangTuaPegawai.setAttribute("pegawai", orangTua.getPegawai());
		orangTuaPegawai.setAttribute("myValue", orangTua.getPegawai());
		orangTuaPegawai.setName(orangTua.getPegawai() == null ? "" : orangTua.getPegawai().getNama());
		orangTuaPegawai.setWidth("90%");
		orangTuaPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("II. Data Ayah"));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP Ayah "));
		row.appendChild(nikAyah = new Textbox(orangTua.getNikAyah()));
		nikAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, orangTua, LampiranLainMahasiswa.KTP_AYAH, "KTP Ayah");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ayah *"));
		row.appendChild(namaAyah = new Textbox(orangTua.getNamaAyah() == null ? "" : orangTua.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Telpon / HP Ayah"));
		row.appendChild(telpAyah = new Textbox(orangTua.getTelpAyah() == null ? "" : orangTua.getTelpAyah()));
		telpAyah.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir Ayah"));
		row.appendChild(tanggalLahirAyah = new MyDatebox(orangTua.getTanggalLahirAyah()));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan Ayah"));
		row.appendChild(jenisPekerjaanAyah = new Combobox());
		Common.insertCombo(jenisPekerjaanAyah, "nama", Pekerjaan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanAyah, orangTua.getJenisPekerjaanAyah());
		jenisPekerjaanAyah.setWidth("90%");
		jenisPekerjaanAyah.setReadonly(true);
		row.setVisible(!jenisPekerjaanAyah.getChildren().isEmpty());

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Rata-rata penghasilan ayah")));
		row.appendChild(jenisPenghasilanAyah = new Combobox());
		Common.insertCombo(jenisPenghasilanAyah, "nama", Penghasilan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanAyah, orangTua.getJenisPenghasilanAyah());
		jenisPenghasilanAyah.setWidth("90%");
		jenisPenghasilanAyah.setReadonly(true);
		row.setVisible(!jenisPenghasilanAyah.getChildren().isEmpty());

		row = new MyFormRow();

		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenjang Pendidikan Ayah ")));
		row.appendChild(jenjangPendidikanAyah = new Combobox());
		Common.insertCombo(jenjangPendidikanAyah, "nama", Jenjang.class,Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanAyah, orangTua.getJenjangPendidikanAyah());
		jenjangPendidikanAyah.setWidth("90%");
		jenjangPendidikanAyah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("III. Data Ibu"));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP Ibu "));
		row.appendChild(nikIbu = new Textbox(orangTua.getNikIbu()));
		nikIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, orangTua, LampiranLainMahasiswa.KTP_IBU, "KTP Ibu");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ibu *"));

		namaIbu = new Textbox(orangTua.getNamaIbu() == null ? "" : orangTua.getNamaIbu());
		row.appendChild(namaIbu);
		namaIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Telpon / HP Ibu"));
		row.appendChild(telpIbu = new Textbox(orangTua.getTelpIbu() == null ? "" : orangTua.getTelpIbu()));
		telpIbu.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir Ibu"));
		row.appendChild(tanggalLahirIbu = new MyDatebox(orangTua.getTanggalLahirIbu()));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan Ibu"));
		row.appendChild(jenisPekerjaanIbu = new Combobox());
		Common.insertCombo(jenisPekerjaanIbu, "nama", Pekerjaan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanIbu, orangTua.getJenisPekerjaanIbu());
		jenisPekerjaanIbu.setWidth("90%");
		jenisPekerjaanIbu.setReadonly(true);
		row.setVisible(!jenisPekerjaanIbu.getChildren().isEmpty());

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Rata-rata penghasilan ibu")));
		row.appendChild(jenisPenghasilanIbu = new Combobox());
		Common.insertCombo(jenisPenghasilanIbu, "nama", Penghasilan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanIbu, orangTua.getJenisPenghasilanIbu());
		jenisPenghasilanIbu.setWidth("90%");
		jenisPenghasilanIbu.setReadonly(true);
		row.setVisible(!jenisPenghasilanIbu.getChildren().isEmpty());

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang Pendidikan Ibu"));
		row.appendChild(jenjangPendidikanIbu = new Combobox());
		Common.insertCombo(jenjangPendidikanIbu, "nama", Jenjang.class,Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanIbu, orangTua.getJenjangPendidikanIbu());
		jenjangPendidikanIbu.setWidth("90%");
		jenjangPendidikanIbu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("IV. Data Wali"));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP Wali "));
		row.appendChild(nikWali = new Textbox(orangTua.getNikWali()));
		nikWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, orangTua, LampiranLainMahasiswa.KTP_IBU, "KTP Wali");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Wali"));

		namaWali = new Textbox(orangTua.getNamaWali() == null ? "" : orangTua.getNamaWali());
		row.appendChild(namaWali);
		namaWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Telpon / HP Wali"));
		row.appendChild(telpWali = new Textbox(orangTua.getTelpWali() == null ? "" : orangTua.getTelpWali()));
		telpWali.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir Wali"));
		row.appendChild(tanggalLahirWali = new MyDatebox(orangTua.getTanggalLahirWali()));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan Wali"));
		row.appendChild(jenisPekerjaanWali = new Combobox());
		Common.insertCombo(jenisPekerjaanWali, "nama", Pekerjaan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanWali, orangTua.getJenisPekerjaanWali());
		jenisPekerjaanWali.setWidth("90%");
		jenisPekerjaanWali.setReadonly(true);
		row.setVisible(!jenisPekerjaanWali.getChildren().isEmpty());

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Rata-rata penghasilan wali")));
		row.appendChild(jenisPenghasilanWali = new Combobox());
		Common.insertCombo(jenisPenghasilanWali, "nama", Penghasilan.class,Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanWali, orangTua.getJenisPenghasilanWali());
		jenisPenghasilanWali.setWidth("90%");
		jenisPenghasilanWali.setReadonly(true);
		row.setVisible(!jenisPenghasilanWali.getChildren().isEmpty());

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang Pendidikan Wali"));
		row.appendChild(jenjangPendidikanWali = new Combobox());
		Common.insertCombo(jenjangPendidikanWali, "nama", Jenjang.class,Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanWali, orangTua.getJenjangPendidikanWali());
		jenjangPendidikanWali.setWidth("90%");
		jenjangPendidikanWali.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(orangTua.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutUtama);

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

	}

	public boolean onSave(Event event) throws Exception {
		if (namaAyah.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ayah",
					"Kolom Nama Ayah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Ayah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (namaIbu.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ibu",
					"Kolom Nama Ibu belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Ibu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		boolean i = checkNamaOrangTua();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ayah dan Ibu",
					"Nama Ayah dan Ibu sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama ayah dan ibu yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (orangTua.getId() != null) {
			orangTua = (OrangTua) session.load(OrangTua.class, orangTua.getId());

		}

		Pegawai pegawaiSebelumnya = orangTua.getPegawai();

		orangTua.setDusun(dusun.getValue());
		orangTua.setKelurahan(kelurahan.getValue());
		orangTua.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
		orangTua.setPropinsi((Propinsi) (propinsi.getAttribute("wilayah")));
		orangTua.setKota((Kota) (kota.getAttribute("wilayah")));
		orangTua.setRt(rt.getValue());
		orangTua.setRw(rw.getValue());
		orangTua.setKodepos(kodepos.getValue());

		orangTua.setPegawai((Pegawai) orangTuaPegawai.getAttribute("pegawai"));

		orangTua.setNamaAyah(namaAyah.getValue().trim());
		orangTua.setNamaIbu(namaIbu.getValue().trim());
		orangTua.setKeterangan(keterangan.getValue());

		orangTua.setJenisPenghasilanAyah((Penghasilan) (jenisPenghasilanAyah.getSelectedItem() == null ? null
				: jenisPenghasilanAyah.getSelectedItem().getValue()));

		orangTua.setJenisPenghasilanIbu((Penghasilan) (jenisPenghasilanIbu.getSelectedItem() == null ? null
				: jenisPenghasilanIbu.getSelectedItem().getValue()));

		orangTua.setJenisPenghasilanWali((Penghasilan) (jenisPenghasilanWali.getSelectedItem() == null ? null
				: jenisPenghasilanWali.getSelectedItem().getValue()));

		orangTua.setJenisPekerjaanAyah((Pekerjaan) (jenisPekerjaanAyah.getSelectedItem() == null ? null
				: jenisPekerjaanAyah.getSelectedItem().getValue()));
		orangTua.setJenisPekerjaanIbu((Pekerjaan) (jenisPekerjaanIbu.getSelectedItem() == null ? null
				: jenisPekerjaanIbu.getSelectedItem().getValue()));

		orangTua.setJenisPekerjaanWali((Pekerjaan) (jenisPekerjaanWali.getSelectedItem() == null ? null
				: jenisPekerjaanWali.getSelectedItem().getValue()));

		orangTua.setJenjangPendidikanAyah((Jenjang) (jenjangPendidikanAyah.getSelectedItem() == null ? null
				: jenjangPendidikanAyah.getSelectedItem().getValue()));
		orangTua.setJenjangPendidikanIbu((Jenjang) (jenjangPendidikanIbu.getSelectedItem() == null ? null
				: jenjangPendidikanIbu.getSelectedItem().getValue()));

		orangTua.setJenjangPendidikanWali((Jenjang) (jenjangPendidikanWali.getSelectedItem() == null ? null
				: jenjangPendidikanWali.getSelectedItem().getValue()));

		orangTua.setNamaWali(namaWali.getValue());
		orangTua.setTanggalLahirWali(tanggalLahirWali.getValue());
		orangTua.setTanggalLahirIbu(tanggalLahirIbu.getValue());
		orangTua.setTanggalLahirAyah(tanggalLahirAyah.getValue());

		orangTua.setNamaAyah(namaAyah.getValue());
		orangTua.setNamaIbu(namaIbu.getValue());

		orangTua.setNoKK(noKK.getValue());

		orangTua.setNikWali(nikWali.getValue().trim());
		orangTua.setNikAyah(nikAyah.getValue().trim());
		orangTua.setNikIbu(nikIbu.getValue().trim());

		orangTua.setTelpAyah(telpAyah.getValue().trim());
		orangTua.setTelpIbu(telpIbu.getValue().trim());
		orangTua.setTelpWali(telpWali.getValue().trim());
		orangTua.setAlamat(alamat.getValue());

		if (jsonObject != null)
			orangTua.setAnak(jsonObject.toString());

		Common.refreshSaveOrUpdate(session, orangTua);

		Pegawai pegawai = orangTua.getPegawai();
		if (pegawai != null && pegawai.getId() != null) {
			session.refresh(pegawai);
			pegawai.setOrangTua(orangTua);
			Common.refreshUpdate(session, pegawai);
		} else if (pegawaiSebelumnya != null && pegawaiSebelumnya.getId() != null) {
			session.refresh(pegawaiSebelumnya);
			pegawaiSebelumnya.setOrangTua(null);
			Common.refreshUpdate(session, pegawaiSebelumnya);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrangTua.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("namaAyah", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("namaIbu", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<OrangTua> orangTua = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(orangTua);
		grid.setRowRenderer(new OrangTuaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaOrangTua() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(OrangTua.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("namaAyah", namaAyah.getValue().trim()))
				.add(Restrictions.eq("namaIbu", namaIbu.getValue().trim()))
				.add(this.orangTua.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.orangTua.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
