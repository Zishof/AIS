package ais.action.master.kursus;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.JenisIdentitasPeserta;
import ais.database.model.kursus.JenisPeserta;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.TipePeserta;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Controller/action ZK untuk peserta kursus. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Combobox searchTipePeserta}, {@code Combobox
 * searchJenisPeserta}, {@code Combobox searchStatus}, {@code Combobox tipe}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); validasi/perhitungan ({@code checkKode()}, {@code checkEmail()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code siapkanParemeterGambar()}, {@code siapkanParemeter()}, {@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PesertaKursusAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Combobox searchTipePeserta;
	private Combobox searchJenisPeserta;
	private Combobox searchStatus;

	private Combobox tipe;
	private Combobox jenisPeserta;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataPegawaiBanbox pegawai;
	private Textbox kode;
	private Textbox kodeIdentitas;
	private Combobox jenisIdentitas;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox telp;
	private Textbox hp;
	private Textbox email;
	private MyCheckboxConfig aktif;

	private Textbox userid;
	private Textbox userPassword;

	private boolean edit = false;
	private boolean delete = false;

	private PesertaKursus pesertaKursus;
	private MyToolbarbuttonConfig add;
	private Textbox alamat;
	private Row rowUsername;
	private Row rowPassword;
	private AmbilDataSiswaBanbox siswa;
	private AmbilDataGuruBanbox guru;

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
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchTipePeserta, "nama", TipePeserta.class);
		Common.insertComboDanSemua(searchJenisPeserta, "nama", JenisPeserta.class);

		Comboitem comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchStatus.appendChild(comboitem);
		comboitem = new Comboitem("Aktif");
		if (comboitem != null) { comboitem.setValue(true); }
		searchStatus.appendChild(comboitem);
		comboitem = new Comboitem("Tidak Aktif");
		if (comboitem != null) { comboitem.setValue(false); }
		searchStatus.appendChild(comboitem);
		if (searchStatus != null) { searchStatus.setSelectedIndex(0); }
		if (searchStatus != null) { searchStatus.setReadonly(true); }

		// private Combobox searchJenisPeserta;
		// private Combobox searchProdi;

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

		String[] contents = new String[] { "id", "kodeIdentitas", "jenisIdentitas", "kode", "nama", "alamat",
				"perpustakaan", "mahasiswa", "dosen", "pegawai", "tbmuser", "jenisPeserta", "tipe", "keterangan",
				"telp", "hp", "email", "jenisIdentitasPeserta", "tipePeserta", "aktif", "tanggal", "" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PesertaKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan Mahasiswa", "/img/excel.png");
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

				MyFormRow row = new MyFormRow();row.setValign("top");
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
							MyMessageboxConfig.show("Tahun angkatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peserta Kursus Mahasiswa");

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
								List<String> ids = session.createCriteria(Mahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(j == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan", j))
										.createAlias("jurusan", "jurusan")
										.add(Restrictions.eq("tahunangkatan", tahunAngkatan.getValue()))
										.add(f == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.fakultas", f))
										.setProjection(Projections.groupProperty("nim")).list();
								HibernateUtil.closeSession();
								int i = 1;
								for (String nim : ids) {
									label.setValue("Singkronkan peserta Kursus dengan nim " + nim + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahMahasiswaOtomatisMenjadiPesertaKursusPerpustakaan(nim);
										laporan.catatBerhasil(i - 1, nim, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, nim, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-nim): "
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

		singkron = new MyToolbarbuttonConfig("Singkronkan Dosen", "/img/excel.png");
		Common.appendKeToolbar(singkron, add, comp);
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Singkronkan Data Dosen", "none", true);
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

				MyFormRow row = new MyFormRow();row.setValign("top");
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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan data dosen",
						"/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peserta Kursus Dosen");

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
								List<String> ids = session.createCriteria(Dosen.class)
										.add(j == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan", j))
										.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
										.add(f == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.fakultas", f))
										.add(Restrictions.ne("nidn", "")).add(Restrictions.isNotNull("nidn"))
										.setProjection(Projections.groupProperty("nidn")).list();
								HibernateUtil.closeSession();
								int i = 1;
								for (String nidn : ids) {
									label.setValue("Singkronkan pesertaKursus perpustakaan dengan nidn " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahDosenOtomatisMenjadiPesertaKursusPerpustakaan(nidn);
										laporan.catatBerhasil(i - 1, nidn, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, nidn, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-nidn): "
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

		singkron = new MyToolbarbuttonConfig("Singkronkan Pegawai", "/img/excel.png");
		Common.appendKeToolbar(singkron, add, comp);
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Singkronkan Data Pegawai", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				Column column = new Column();
				column.setWidth("20%");
				column.setParent(columns);
				column = new Column();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
				final AmbilDataSatuanKerjaBanbox searchSatker;
				row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
				searchSatker.setWidth("90%");
				searchSatker.setReadonly(true);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan data pegawai",
						"/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
						final Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil
								.ambilSatuanKerjas();
						if (parent != null) {
							satuanKerjas.clear();
							satuanKerjas.add(parent);
							SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
							satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peserta Kursus Pegawai");

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

								Session session = HibernateUtil.currentNativeSession();
								List<String> ids = session.createCriteria(Pegawai.class)
										.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(
														parent == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.sqlRestriction("false"),
														Restrictions.in("satuanKerja", satuanKerjas)))
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))
										.add(Restrictions.isNull("dosen")).add(Restrictions.ne("mycode", ""))
										.add(Restrictions.isNotNull("mycode"))
										.setProjection(Projections.groupProperty("mycode")).list();
								HibernateUtil.closeSession();
								int i = 1;
								for (String nidn : ids) {
									label.setValue("Singkronkan pesertaKursus perpustakaan dengan NIK " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahPegawaiOtomatisMenjadiPesertaKursusPerpustakaan(nidn);
										laporan.catatBerhasil(i - 1, nidn, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, nidn, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-pegawai): "
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

		singkron = new MyToolbarbuttonConfig("Singkronkan Siswa", "/img/excel.png");
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

				MyFormRow row = new MyFormRow();row.setValign("top");
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
							MyMessageboxConfig.show("Tahun angkatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peserta Kursus Siswa");

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
								List<Siswa> ids = ConstantValues.simpleList(
										session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
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
									label.setValue("Singkronkan pesertaKursus perpustakaan dengan " + siswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciSiswa = String.valueOf(siswa);
									try {
										Common.checkApakahSiswaOtomatisMenjadiPesertaKursusPerpustakaan(siswa);
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
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map siapkanParemeterGambar(Map parameters) throws Exception {

		File fileStempel = new File(Common.REAL_PATH + "/img/stempel.png");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN,
					LampiranLain.STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileStempel = lainMahasiswa.ambilFile();
				System.out.println("fileStempel = " + fileStempel);

			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		File fileTtd = new File(Common.REAL_PATH + "/img/tandatangan.png");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.TANDA_TANGAN_KARTU_ANGGOTA_PERPUSTAKAAN,
					LampiranLain.TTD_KARTU_ANGGOTA_PERPUSTAKAAN_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileTtd = lainMahasiswa.ambilFile();
				System.out.println("fileTtd = " + fileTtd);

			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		File fileBg1 = new File(Common.REAL_PATH + "/img/bg2.png");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN,
					LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileBg1 = lainMahasiswa.ambilFile();
				System.out.println("fileBg1 = " + fileBg1);

			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		File fileBg2 = new File(Common.REAL_PATH + "/img/bg1.png");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN,
					LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileBg2 = lainMahasiswa.ambilFile();
				System.out.println("fileBg2 = " + fileBg2);

			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		String defaultValue = "1. Kartu ini ditertibkan oleh Perpustakaan ....... Segala penggunaan kartu oleh Perpustakaan ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini wajib dibawa setiap masuk ke perpustakaan.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Setiap pengunjung harus mematuhi semua tata tertib perpustakaan .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke perpustakaan .......\n" + "\n\n\n"
				+ "Perpustakaan .......\n" + "website : http://ecampus.id";

		String tataTertib = Common.getKonfigurasi("tata_tertib_perpustakaan", defaultValue).getNilai();

		parameters.put("tataTertib", tataTertib);
		parameters.put("fileStempel", fileStempel.getAbsolutePath());
		parameters.put("fileTtd", fileTtd.getAbsolutePath());
		parameters.put("fileBg1", fileBg1.getAbsolutePath());
		parameters.put("fileBg2", fileBg2.getAbsolutePath());

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map siapkanParemeter(PesertaKursus pesertaKursus) throws Exception {

		final File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + pesertaKursus.getKode() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(pesertaKursus.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pesertaKursus.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("kode", pesertaKursus.getKode());
		parameters.put("nama", pesertaKursus.getNama());
		parameters.put("alamat", pesertaKursus.getAlamat());
		parameters.put("telp", pesertaKursus.getTelp());
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (pesertaKursus.getMahasiswa() != null) {
			calendar.setTime(pesertaKursus.getMahasiswa().getTanggalMasuk());
		}
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 5);
		parameters.put("tanggal_kadaluarsa", calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (pesertaKursus.getMahasiswa() != null) {
			calendar.setTime(pesertaKursus.getMahasiswa().getTanggalMasuk());
		}
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_1", calendar.getTime());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_2", calendar.getTime());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_3", calendar.getTime());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_4", calendar.getTime());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_5", calendar.getTime());

		File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
		parameters.put("foto", file.getAbsolutePath());
		parameters.put("foto_lulus", file.getAbsolutePath());

		if (pesertaKursus.getMahasiswa() != null) {
			pesertaKursus.getMahasiswa().putPhoto(parameters);

			parameters.put("nama_fakultas", pesertaKursus.getMahasiswa().getJurusan().getFakultas().getNama());
			parameters.put("nama_perguruan_tinggi",
					pesertaKursus.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: pesertaKursus.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi().getNama());
			parameters.put("ttl",
					pesertaKursus.getMahasiswa().getTempatlahir().toUpperCase() + " / "
							+ (pesertaKursus.getMahasiswa().getTanggallahir() == null ? ""
									: Common.dateFormat2.get().format(pesertaKursus.getMahasiswa().getTanggallahir())));

			parameters.put("email", pesertaKursus.getMahasiswa().getEmail());
			parameters.put("nama", pesertaKursus.getMahasiswa().getNama());
			parameters.put("angkatan", pesertaKursus.getMahasiswa().getTahunangkatan());
			parameters.put("alamat_data", pesertaKursus.getMahasiswa().getAlamat());
			parameters.put("tempatlahir", pesertaKursus.getMahasiswa().getTempatlahir());
			parameters.put("tanggallahir", pesertaKursus.getMahasiswa().getTanggallahir());
			parameters.put("jurusan", pesertaKursus.getMahasiswa().getJurusan().getNama());
			parameters.put("jenjang", pesertaKursus.getMahasiswa().getJurusan().getJenjang().getNama());

		} else if (pesertaKursus.getDosen() != null) {
			pesertaKursus.getDosen().putPhoto(parameters);

			parameters.put("nama_fakultas", pesertaKursus.getDosen().getJurusan() == null ? ""
					: pesertaKursus.getDosen().getJurusan().getFakultas().getNama());
			parameters.put("nama_perguruan_tinggi", pesertaKursus.getDosen().getJurusan() == null ? ""
					: pesertaKursus.getDosen().getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: pesertaKursus.getDosen().getJurusan().getFakultas().getPerguruanTinggi().getNama());
			parameters.put("ttl",
					pesertaKursus.getDosen().getTempatlahir().toUpperCase() + " / "
							+ (pesertaKursus.getDosen().getTanggallahir() == null ? ""
									: Common.dateFormat2.get().format(pesertaKursus.getDosen().getTanggallahir())));

			parameters.put("email", pesertaKursus.getDosen().getEmail());
			parameters.put("nama", pesertaKursus.getDosen().getNama());
			parameters.put("angkatan", 0);
			parameters.put("alamat_data", pesertaKursus.getDosen().getAlamat());
			parameters.put("tempatlahir", pesertaKursus.getDosen().getTempatlahir());
			parameters.put("tanggallahir", pesertaKursus.getDosen().getTanggallahir());
			parameters.put("jurusan", pesertaKursus.getDosen().getJurusan() == null ? ""
					: pesertaKursus.getDosen().getJurusan().getNama());
			parameters.put("jenjang", pesertaKursus.getDosen().getJurusan() == null ? ""
					: pesertaKursus.getDosen().getJurusan().getJenjang().getNama());

		} else if (pesertaKursus.getPegawai() != null) {
			pesertaKursus.getPegawai().putPhoto(parameters);

			parameters.put("ttl",
					pesertaKursus.getPegawai().getTempatlahir().toUpperCase() + " / "
							+ (pesertaKursus.getPegawai().getTanggallahir() == null ? ""
									: Common.dateFormat2.get().format(pesertaKursus.getPegawai().getTanggallahir())));

			parameters.put("email", pesertaKursus.getPegawai().getEmail());
			parameters.put("nama", pesertaKursus.getPegawai().getNama());
			parameters.put("angkatan", 0);
			parameters.put("alamat_data", pesertaKursus.getPegawai().getAlamat());
			parameters.put("tempatlahir", pesertaKursus.getPegawai().getTempatlahir());
			parameters.put("tanggallahir", pesertaKursus.getPegawai().getTanggallahir());
			parameters.put("jurusan", pesertaKursus.getPegawai().getJurusan() == null ? ""
					: pesertaKursus.getPegawai().getJurusan().getNama());
			parameters.put("jenjang", pesertaKursus.getPegawai().getJurusan() == null ? ""
					: pesertaKursus.getPegawai().getJurusan().getJenjang().getNama());
		}

		parameters.put("alamat", pesertaKursus.getAlamat());

		return parameters;
	}

	class PesertaKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PesertaKursus pesertaKursus = (PesertaKursus) arg1;

			if (pesertaKursus.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getMahasiswa()).setParent(arg0);
			} else if (pesertaKursus.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getSiswa()).setParent(arg0);
			} else if (pesertaKursus.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getGuru()).setParent(arg0);
			} else if (pesertaKursus.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getDosen()).setParent(arg0);
			} else if (pesertaKursus.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getPegawai()).setParent(arg0);
			} else if (pesertaKursus.getTbmuser() != null) {
				CommonMedia.tampilkanGambarKecil(pesertaKursus.getTbmuser()).setParent(arg0);
			} else {
				new Label("").setParent(arg0);
			}

			new Label(pesertaKursus.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(PesertaKursus.class, pesertaKursus, pesertaKursus.getNama()).setParent(arg0);
			new Label(pesertaKursus.getJenisIdentitas()).setParent(arg0);
			new Label(pesertaKursus.getKodeIdentitas()).setParent(arg0);

			new Label(pesertaKursus.getJenisPeserta() == null ? "" : pesertaKursus.getJenisPeserta().getNama())
					.setParent(arg0);
			new Label(pesertaKursus.getTipe()).setParent(arg0);
			new Label(pesertaKursus.getTelp()).setParent(arg0);
			new Label(pesertaKursus.getHp()).setParent(arg0);
			new Label(pesertaKursus.getEmail()).setParent(arg0);
			new Label(pesertaKursus.getAlamat()).setParent(arg0);
			new Label(pesertaKursus.getAktif() ? "Aktif" : "Tidak Aktif").setParent(arg0);
			new Label(Common.dateFormat1.get().format(pesertaKursus.getTanggal())).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Kartu Peserta");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					if (!pesertaKursus.getAktif()) {
						MyMessageboxConfig.show("PesertaKursus ini tidak aktif", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					List list = new ArrayList();
					list.add(siapkanParemeter(pesertaKursus));
					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters = PesertaKursusAction.siapkanParemeterGambar(parameters);
					parameters.put("maps", list);

					Report.generatePDFReport(Report.PDF, parameters, "library/kartu_pesertaKursus",
							pesertaKursus.getTanggal_dirubah());
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pesertaKursus);
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

											Common.refreshDelete(pesertaKursus);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PesertaKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PesertaKursus pesertaKursus) throws Exception {
		this.pesertaKursus = pesertaKursus;
		addWindow.setTitle(pesertaKursus.getId() == null ? "Tambah Peserta" : "Ubah Peserta");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		final MyFormRow rowMahasiswa = new MyFormRow();
		final MyFormRow rowSiswa = new MyFormRow();
		final MyFormRow rowDosen = new MyFormRow();
		final MyFormRow rowGuru = new MyFormRow();
		final MyFormRow rowPegawai = new MyFormRow();
		rowUsername = new MyFormRow();
		rowPassword = new MyFormRow();
		EventListener jenisEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Comboitem selectedRadio = tipe.getSelectedItem();
				if (selectedRadio != null) {
					System.out.println("selectedRadio.getLabel() = " + selectedRadio.getLabel());

					rowSiswa.setVisible(selectedRadio.getLabel().equalsIgnoreCase("Siswa"));
					rowGuru.setVisible(selectedRadio.getLabel().equalsIgnoreCase("Guru"));

					rowMahasiswa.setVisible(selectedRadio.getLabel().equalsIgnoreCase("Mahasiswa"));
					rowDosen.setVisible(selectedRadio.getLabel().equalsIgnoreCase("Dosen"));
					rowPegawai.setVisible(selectedRadio.getLabel().equalsIgnoreCase("Pegawai"));

					rowUsername.setVisible(!rowMahasiswa.isVisible() && !rowDosen.isVisible() && !rowPegawai.isVisible()
							&& !rowSiswa.isVisible() && !rowGuru.isVisible());
					rowPassword.setVisible(rowUsername.isVisible());

				}
			}
		};

		MyFormRow

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Peserta"));
		String mykode = pesertaKursus.getKode();
		row.appendChild(kode = new Textbox(pesertaKursus.getKode() == null ? mykode : pesertaKursus.getKode()));
		kode.setWidth("90%");

		// EventListener eventListener = new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// myPerpustakaan = (Perpustakaan) (perpustakaan.getSelectedItem() ==
		// null ? null
		// : perpustakaan.getSelectedItem().getValue());
		// if (myPerpustakaan != null) {
		// String mykode = LibraryUtil.generateCode(PesertaKursus.class, 8,
		// "AGT", myPerpustakaan);
		// kode.setValue(mykode);
		// }
		// }
		// };
		// perpustakaan.addEventListener("onChange", eventListener);
		// if (kode.getValue().trim().equals("")) {
		// eventListener.onEvent(null);
		// }

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Peserta"));
		row.appendChild(tipe = new Combobox());
		Common.insertCombo(tipe, "nama", TipePeserta.class);
		Common.selectComboItem(tipe, pesertaKursus.getTipePeserta());
		tipe.setWidth("90%");
		tipe.setReadonly(true);
		// List<TipePeserta> tipePesertas = HibernateUtil.currentSession()
		// .createCriteria(TipePeserta.class).list();
		// for (TipePeserta tipePeserta : tipePesertas) {
		// MyRadioConfig radio = new MyRadioConfig(tipePeserta.getNama());
		// radio.setChecked(pesertaKursus.getTipePeserta() != null
		// && pesertaKursus.getTipePeserta().getId() != null
		// && pesertaKursus.getTipePeserta().getId()
		// .equals(tipePeserta.getId()));
		// radio.setAttribute("tipePeserta", tipePeserta);
		// radio.addEventListener("onCheck", jenisEventListener);
		// tipe.appendChild(radio);
		// }

		// tipe.setDisabled(pesertaKursus.getId() != null);

		Tbmuser tbmuser = pesertaKursus.getTbmuser();

		rowUsername.setStyle("border:0px;background: transparent;");
		rowUsername.setParent(rows);
		rowUsername.appendChild(new Label(ais.common.Common.getBahasaConfig("Username Peserta")));
		rowUsername.appendChild(userid = new Textbox(tbmuser == null ? "" : tbmuser.getUserId()));
		userid.setWidth("90%");
		userid.setDisabled(tbmuser != null && tbmuser.getUserId() != null);

		if (pesertaKursus.getId() != null && tbmuser != null) {
			userid.setDisabled(true);
		}

		rowPassword.setStyle("border:0px;background: transparent;");
		rowPassword.setParent(rows);
		rowPassword.appendChild(new Label(ais.common.Common.getBahasaConfig("Password")));
		rowPassword.appendChild(userPassword = new Textbox(
				tbmuser == null ? "" : tbmuser.getUserPassword() == null ? "" : tbmuser.getUserPassword()));

		if (tbmuser != null && tbmuser.getIs_encripted() != null && tbmuser.getIs_encripted()) {
			try {
				userPassword.setValue(tbmuser == null ? ""
						: tbmuser.getUserPassword() == null ? ""
								: Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		userPassword.setType("password");
		userPassword.setWidth("90%");

		rowMahasiswa.setVisible(false);
		rowMahasiswa.setStyle("border:0px;background: transparent;");
		rowMahasiswa.setParent(rows);
		rowMahasiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa")));
		rowMahasiswa.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", pesertaKursus.getMahasiswa());
		mahasiswa.setValue(pesertaKursus.getMahasiswa() == null ? ""
				: pesertaKursus.getMahasiswa().getNim() + " - " + pesertaKursus.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");
		mahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa myMahasiswa = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
				kodeIdentitas.setValue(myMahasiswa.getNim());
				nama.setValue(myMahasiswa.getNama());
				if (alamat.getValue().trim().equals("")) {
					alamat.setValue(myMahasiswa.getAlamat());
				}
				if (telp.getValue().trim().equals("")) {
					telp.setValue(myMahasiswa.getTelp());
				}
				if (hp.getValue().trim().equals("")) {
					hp.setValue(myMahasiswa.getTelp());
				}
				if (email.getValue().trim().equals("")) {
					email.setValue(myMahasiswa.getEmail());
				}

				JenisPeserta m = (JenisPeserta) HibernateUtil.currentSession().createCriteria(JenisPeserta.class)
						.add(Restrictions.eq("nama", "NIM")).setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisPeserta, m);
			}
		});

		rowDosen.setVisible(false);
		rowDosen.setStyle("border:0px;background: transparent;");
		rowDosen.setParent(rows);
		rowDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Dosen")));
		rowDosen.appendChild(dosen = new AmbilDataDosenBanbox(false));
		dosen.setAttribute("dosen", pesertaKursus.getDosen());
		dosen.setValue(pesertaKursus.getDosen() == null ? ""
				: pesertaKursus.getDosen().getCode() + " - " + pesertaKursus.getDosen().getNama());
		dosen.setWidth("90%");
		dosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Dosen mydosen = (Dosen) dosen.getAttribute("dosen");
				kodeIdentitas.setValue(mydosen.getCode());
				nama.setValue(mydosen.getNama());
				if (alamat.getValue().trim().equals("")) {
					alamat.setValue(mydosen.getAlamat());
				}
				if (telp.getValue().trim().equals("")) {
					telp.setValue(mydosen.getTelp());
				}
				if (hp.getValue().trim().equals("")) {
					hp.setValue(mydosen.getTelp());
				}
				if (email.getValue().trim().equals("")) {
					email.setValue(mydosen.getEmail());
				}

				JenisPeserta m = (JenisPeserta) HibernateUtil.currentSession().createCriteria(JenisPeserta.class)
						.add(Restrictions.eq("nama", "NIDN")).setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisPeserta, m);
			}
		});

		rowGuru.setVisible(false);
		rowGuru.setStyle("border:0px;background: transparent;");
		rowGuru.setParent(rows);
		rowGuru.appendChild(new Label(ais.common.Common.getBahasaConfig("Guru")));
		rowGuru.appendChild(guru = new AmbilDataGuruBanbox(false));
		guru.setAttribute("guru", pesertaKursus.getGuru());
		guru.setValue(pesertaKursus.getGuru() == null ? ""
				: pesertaKursus.getGuru().getKode() + " - " + pesertaKursus.getGuru().getNama());
		guru.setWidth("90%");
		guru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Guru myguru = (Guru) guru.getAttribute("guru");
				kodeIdentitas.setValue(myguru.getKode());
				nama.setValue(myguru.getNama());
				if (alamat.getValue().trim().equals("")) {
					alamat.setValue(myguru.getAlamatGuru());
				}
				if (telp.getValue().trim().equals("")) {
					telp.setValue(myguru.getTeleponGuru());
				}
				if (hp.getValue().trim().equals("")) {
					hp.setValue(myguru.getHp());
				}
				if (email.getValue().trim().equals("")) {
					email.setValue(myguru.getAlamatEmail());
				}

				JenisPeserta m = (JenisPeserta) HibernateUtil.currentSession().createCriteria(JenisPeserta.class)
						.add(Restrictions.eq("nama", "KTP")).setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisPeserta, m);
			}
		});

		rowPegawai.setVisible(false);
		rowPegawai.setStyle("border:0px;background: transparent;");
		rowPegawai.setParent(rows);
		rowPegawai.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		rowPegawai.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setAttribute("pegawai", pesertaKursus.getPegawai());
		pegawai.setValue(pesertaKursus.getPegawai() == null ? ""
				: pesertaKursus.getPegawai().getCode() + " - " + pesertaKursus.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai mypegawai = (Pegawai) pegawai.getAttribute("pegawai");
				kodeIdentitas.setValue(mypegawai.getCode());
				nama.setValue(mypegawai.getNama());
				if (alamat.getValue().trim().equals("")) {
					alamat.setValue(mypegawai.getAlamat());
				}
				if (telp.getValue().trim().equals("")) {
					telp.setValue(mypegawai.getTelp());
				}
				if (hp.getValue().trim().equals("")) {
					hp.setValue(mypegawai.getTelp());
				}
				if (email.getValue().trim().equals("")) {
					email.setValue(mypegawai.getEmail());
				}

				JenisPeserta m = (JenisPeserta) HibernateUtil.currentSession().createCriteria(JenisPeserta.class)
						.add(Restrictions.eq("nama", "NIP")).setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisPeserta, m);
			}
		});

		rowSiswa.setVisible(false);
		rowSiswa.setStyle("border:0px;background: transparent;");
		rowSiswa.setParent(rows);
		rowSiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa")));
		rowSiswa.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", pesertaKursus.getSiswa());
		siswa.setValue(pesertaKursus.getSiswa() == null ? ""
				: pesertaKursus.getSiswa().getNim() + " - " + pesertaKursus.getSiswa().getNama());
		siswa.setWidth("90%");
		siswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa mySiswa = (Siswa) siswa.getAttribute("siswa");
				kodeIdentitas.setValue(mySiswa.getNim());
				nama.setValue(mySiswa.getNama());
				if (alamat.getValue().trim().equals("")) {
					alamat.setValue(mySiswa.getAlamatSiswa());
				}
				if (telp.getValue().trim().equals("")) {
					telp.setValue(mySiswa.getTeleponSiswa());
				}
				if (hp.getValue().trim().equals("")) {
					hp.setValue(mySiswa.getTeleponSiswa());
				}
				if (email.getValue().trim().equals("")) {
					email.setValue(mySiswa.getAlamatEmail());
				}

				JenisPeserta m = (JenisPeserta) HibernateUtil.currentSession().createCriteria(JenisPeserta.class)
						.add(Restrictions.eq("nama", "NIS")).setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisPeserta, m);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Peserta"));
		row.appendChild(jenisPeserta = new Combobox());
		Common.insertCombo(jenisPeserta, "nama", JenisPeserta.class);
		Common.selectComboItem(jenisPeserta, pesertaKursus.getJenisPeserta());
		jenisPeserta.setWidth("90%");
		jenisPeserta.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Identitas Peserta"));
		row.appendChild(jenisIdentitas = new Combobox());
		Common.insertCombo(jenisIdentitas, "nama", JenisIdentitasPeserta.class);
		Common.selectComboItem(jenisIdentitas, pesertaKursus.getJenisIdentitasPeserta());
		jenisIdentitas.setWidth("90%");
		jenisIdentitas.setReadonly(true);

		// List<JenisIdentitasPeserta> jenisIdentitasPesertas = HibernateUtil
		// .currentSession().createCriteria(JenisIdentitasPeserta.class)
		// .list();
		// for (JenisIdentitasPeserta jenisIdentitasPeserta :
		// jenisIdentitasPesertas) {
		// MyRadioConfig radio = new
		// MyRadioConfig(jenisIdentitasPeserta.getNama());
		// radio.setChecked(pesertaKursus.getJenisIdentitasPeserta() != null
		// && pesertaKursus.getJenisIdentitasPeserta().getId() != null
		// && pesertaKursus.getJenisIdentitasPeserta().getId()
		// .equals(jenisIdentitasPeserta.getId()));
		// radio.setAttribute("jenisIdentitasPeserta", jenisIdentitasPeserta);
		// jenisIdentitas.appendChild(radio);
		// }

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Identitas Peserta"));
		row.appendChild(kodeIdentitas = new Textbox(pesertaKursus.getKodeIdentitas()));
		kodeIdentitas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Peserta"));
		row.appendChild(nama = new Textbox(pesertaKursus.getNama() == null ? "" : pesertaKursus.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp"));
		row.appendChild(telp = new Textbox(pesertaKursus.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
		row.appendChild(hp = new Textbox(pesertaKursus.getHp()));
		hp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(pesertaKursus.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(pesertaKursus.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(pesertaKursus.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(pesertaKursus.getKeterangan() == null ? "" : pesertaKursus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		jenisEventListener.onEvent(null);
		tipe.addEventListener("onChange", jenisEventListener);

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

		if (tipe.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tipe Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		boolean i = checkKode();
		if (i) {
			MyMessageboxConfig.show("Kode Peserta sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		// if (email.getValue().trim().equals("")) {
		// MyMessageboxConfig.show("Email PesertaKursus harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (jenisPeserta.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisIdentitas.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Identitas harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeIdentitas.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Identitas PesertaKursus harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!email.getValue().trim().isEmpty() && !Common.isValidEmailAddress(email.getValue().trim())) {
			MyMessageboxConfig.show("Format email harus benar", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// i = checkEmail();
		// if (i) {
		// MyMessageboxConfig.show("Email \"" + email.getValue() + "\" sudah
		// terpakai, silahkan pilih email yang lain",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		Tbmuser tbmuser = pesertaKursus.getTbmuser();
		if (rowUsername.isVisible()) {

			if (userid.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Username PesertaKursus harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (userPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Username PesertaKursus harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			// i = Common.checkUsername(userid.getValue().trim(),
			// tbmuser == null ? null : tbmuser.getUserId(), null);
			// if (i) {
			// MyMessageboxConfig
			// .show("Username \""
			// + userid.getValue()
			// + "\" sudah terpakai, silahkan pilih username yang lain",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			// return false;
			// }

			Session session = HibernateUtil.currentSession();
			tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("userId", userid.getValue().trim())).setMaxResults(1).uniqueResult();

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				MyMessageboxConfig.show("Username ini merupakan pengguna dosen, Anda harus memilih tipe peserta Dosen",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				MyMessageboxConfig.show(
						"Username ini merupakan pengguna mahasiswa, Anda harus memilih tipe peserta Mahasiswa",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.ambilPegawai() != null) {
				MyMessageboxConfig.show(
						"Username ini merupakan pengguna pegawai, Anda harus memilih tipe peserta Pegawai",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser == null || tbmuser.getUserId() == null) {
				tbmuser = new Tbmuser();
				tbmuser.setUserId(userid.getValue().trim());
				tbmuser.setUserRole(ConstantValues.rolePesertaKursusPerpustakaan);
				tbmuser.setRoot(true);
				tbmuser.setUserShow(1);
				tbmuser.setIs_encripted(true);
			}
			tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(userPassword.getValue().trim()));
			tbmuser.setEmail(email.getValue());
			tbmuser.setNama(nama.getValue());
			tbmuser.setUserNama(nama.getValue());
			Common.refreshSaveOrUpdate(session, tbmuser);
		}

		Session session = HibernateUtil.currentSession();
		if (pesertaKursus.getId() != null) {
			pesertaKursus = (PesertaKursus) session.load(PesertaKursus.class, pesertaKursus.getId());
		}

		pesertaKursus.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pesertaKursus.setGuru((Guru) guru.getAttribute("guru"));
		pesertaKursus.setAktif(aktif.isChecked());
		pesertaKursus.setTipePeserta((TipePeserta) tipe.getSelectedItem().getValue());
		pesertaKursus.setJenisIdentitasPeserta((JenisIdentitasPeserta) jenisIdentitas.getSelectedItem().getValue());
		pesertaKursus.setHp(hp.getValue());
		pesertaKursus.setTelp(telp.getValue());
		pesertaKursus.setEmail(email.getValue());
		pesertaKursus.setJenisIdentitas(jenisIdentitas.getSelectedItem().getLabel());
		pesertaKursus.setKodeIdentitas(kodeIdentitas.getValue().trim());
		pesertaKursus.setAlamat(alamat.getValue());
		pesertaKursus.setTipe(tipe.getSelectedItem().getLabel());
		pesertaKursus.setMahasiswa(null);
		pesertaKursus.setDosen(null);
		pesertaKursus.setPegawai(null);
		pesertaKursus.setKode(kode.getValue().trim());
		pesertaKursus.setNama(nama.getValue());
		pesertaKursus.setKeterangan(keterangan.getValue());
		pesertaKursus.setJenisPeserta((JenisPeserta) (jenisPeserta.getSelectedItem() == null ? null
				: jenisPeserta.getSelectedItem().getValue()));
		pesertaKursus.setDibuatOleh(Common.getCurrentUser());
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Mahasiswa")) {
			pesertaKursus.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		}
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Dosen")) {
			pesertaKursus.setDosen((Dosen) dosen.getAttribute("dosen"));
		}
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Pegawai")) {
			pesertaKursus.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		}

		pesertaKursus.setTbmuser(tbmuser);

		Common.refreshSaveOrUpdate(session, pesertaKursus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesertaKursus.class)

				.add(searchStatus.getSelectedItem() == null || searchStatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("aktif", searchStatus.getSelectedItem().getValue()))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("mahasiswa.tahunangkatan")).addOrder(Order.asc("nama"));
		criteria

				.add(searchTipePeserta.getSelectedItem() == null
						|| searchTipePeserta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tipePeserta", searchTipePeserta.getSelectedItem().getValue()))

				.add(searchJenisPeserta.getSelectedItem() == null
						|| searchJenisPeserta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPeserta", searchJenisPeserta.getSelectedItem().getValue()))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("kodeIdentitas", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("email", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
												Restrictions.ilike("kode", searchkode.getValue(),
														MatchMode.ANYWHERE)))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PesertaKursus> pesertaKursus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pesertaKursus);
		grid.setRowRenderer(new PesertaKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKode() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PesertaKursus.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.pesertaKursus.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pesertaKursus.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkEmail() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PesertaKursus.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("email", email.getValue().trim()))
				.add(this.pesertaKursus.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pesertaKursus.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}
}
