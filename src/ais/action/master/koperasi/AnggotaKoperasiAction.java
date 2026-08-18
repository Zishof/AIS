package ais.action.master.koperasi;

import java.util.Calendar;
import java.util.List;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AnggotaKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Combobox searchTipeAnggotaKoperasi;
	private Combobox searchJenisAnggotaKoperasi;
	private Combobox searchKoperasi;
	private Combobox searchStatus;

	private Combobox tipe;
	private Combobox jenisAnggotaKoperasi;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataPegawaiBanbox pegawai;
	private Combobox koperasi;
	private Textbox kode;
	private Textbox kodeIdentitas;
	private Combobox jenisIdentitas;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox telp;
	private Textbox hp;
	private Textbox email;
	private MyCheckboxConfig aktif;
	private MyCheckboxConfig pihakTerkait;

	private Textbox userid;
	private Textbox userPassword;

	private boolean edit = false;
	private boolean delete = false;

	private AnggotaKoperasi anggotaKoperasi;
	private MyToolbarbuttonConfig add;
	private Koperasi myKoperasi;
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

		tampilkanStatistikAnggota(comp);

		Common.insertComboDanSemua(searchTipeAnggotaKoperasi, "nama", TipeAnggotaKoperasi.class,
				Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchJenisAnggotaKoperasi, "nama", JenisAnggotaKoperasi.class,
				Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchKoperasi, "nama", Koperasi.class, Restrictions.eq("aktif", true));

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

		// private Combobox searchJenisAnggotaKoperasi;
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

		String[] contents = new String[] { "id", "koperasi", "kodeIdentitas", "jenisIdentitas", "kode", "nama",
				"alamat", "koperasi", "mahasiswa", "dosen", "pegawai", "tbmuser", "jenisAnggotaKoperasi", "tipe",
				"keterangan", "telp", "hp", "email", "jenisIdentitasAnggotaKoperasi", "tipeAnggotaKoperasi", "aktif",
				"tanggal", "" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AnggotaKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan Mahasiswa", "/img/excel.png");
		Common.appendKeToolbar(singkron, add, comp);
		singkron.setVisible(
				Common.bolehKonfigurasi("singkronkan_mahasiswa_dengan_anggotaKoperasi_koperasi"));
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				myKoperasi = (Koperasi) (searchKoperasi.getSelectedItem() == null ? null
						: searchKoperasi.getSelectedItem().getValue());
				if (myKoperasi == null) {
					MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia di kolom Koperasi; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

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
							MyMessageboxConfig.show("Mohon maaf, tahun angkatan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Angkatan dengan angka tahun yang benar (contoh: 2024); (2) pastikan format angka valid; (3) ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Anggota Koperasi Mahasiswa");

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
								// JANGAN tutup session di sini -- checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi
								// dipanggil BERULANG di bawah dan SENGAJA tak lagi menutup session sendiri (lihat
								// javadoc method tsb); menutup lebih awal cuma memaksa reconnect tiap baris.
								int i = 1;
								int gagal = 0;
								for (String nim : ids) {
									label.setValue("Singkronkan anggotaKoperasi koperasi dengan nim " + nim + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi(nim, myKoperasi);
										laporan.catatBerhasil(i - 1, nim, "Sinkronisasi berhasil");
									} catch (Exception exSatu) {
										// SATU nim bermasalah TIDAK BOLEH menghentikan sisa batch -- catat & lanjut.
										gagal++;
										ais.common.ErrorAuditUtil.record(exSatu, "auto-audit src/ais/action/master/koperasi/AnggotaKoperasiAction.java:335");
										laporan.catatGagalDetail(i - 1, nim, exSatu);
									}
									i++;
								}
								if (gagal > 0) {
									System.err.println("Singkronkan Mahasiswa: " + gagal + " dari " + ids.size()
											+ " NIM gagal disinkronkan (lihat laporan .txt untuk detail).");
								}
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
		singkron.setVisible(
				Common.bolehKonfigurasi("singkronkan_dosen_dengan_anggotaKoperasi_koperasi"));
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				myKoperasi = (Koperasi) (searchKoperasi.getSelectedItem() == null ? null
						: searchKoperasi.getSelectedItem().getValue());
				if (myKoperasi == null) {
					MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia di kolom Koperasi; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

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

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Anggota Koperasi Dosen");

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
									label.setValue("Singkronkan anggotaKoperasi koperasi dengan nidn " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahDosenOtomatisMenjadiAnggotaKoperasi(nidn, myKoperasi);
										laporan.catatBerhasil(i - 1, nidn, "Sinkronisasi berhasil");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, nidn, ePerItem);
									}
									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-dosen): "
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
		singkron.setVisible(
				Common.bolehKonfigurasi("singkronkan_pegawai_dengan_anggotaKoperasi_koperasi"));
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				myKoperasi = (Koperasi) (searchKoperasi.getSelectedItem() == null ? null
						: searchKoperasi.getSelectedItem().getValue());
				if (myKoperasi == null) {
					MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia di kolom Koperasi; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

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

				MyFormRow row = new MyFormRow();
				row.setValign("top");
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

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Anggota Koperasi Pegawai");

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
									label.setValue("Singkronkan anggotaKoperasi koperasi dengan NIK " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(nidn, myKoperasi);
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
		singkron.setVisible(
				Common.bolehKonfigurasi("singkronkan_siswa_dengan_anggotaKoperasi_koperasi"));
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				myKoperasi = (Koperasi) (searchKoperasi.getSelectedItem() == null ? null
						: searchKoperasi.getSelectedItem().getValue());
				if (myKoperasi == null) {
					MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia di kolom Koperasi; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

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
							MyMessageboxConfig.show("Mohon maaf, tahun angkatan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Angkatan dengan angka tahun yang benar (contoh: 2024); (2) pastikan format angka valid; (3) ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Anggota Koperasi Siswa");

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
								List<Siswa> ids = ConstantValues.simpleList(session.createCriteria(Siswa.class)
										.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
										.add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(j == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", j))
										.add(Restrictions.eq("tahunMasuk", tahunAngkatan.getValue()))
										.add(f == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("yayasan", f)),
										Siswa.class);
								// JANGAN tutup session di sini -- checkApakahSiswaOtomatisMenjadiAnggotaKoperasi
								// dipanggil BERULANG di bawah dan SENGAJA tak lagi menutup session sendiri (lihat
								// javadoc method tsb); menutup lebih awal cuma memaksa reconnect tiap baris.
								int i = 1;
								int gagal = 0;
								for (Siswa siswa : ids) {
									label.setValue("Singkronkan anggotaKoperasi koperasi dengan " + siswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciSiswa = String.valueOf(siswa);
									try {
										Common.checkApakahSiswaOtomatisMenjadiAnggotaKoperasi(siswa, myKoperasi);
										laporan.catatBerhasil(i - 1, kunciSiswa, "Sinkronisasi berhasil");
									} catch (Exception exSatu) {
										// SATU siswa bermasalah TIDAK BOLEH menghentikan sisa batch -- catat & lanjut.
										gagal++;
										ais.common.ErrorAuditUtil.record(exSatu, "auto-audit src/ais/action/master/koperasi/AnggotaKoperasiAction.java:784");
										laporan.catatGagalDetail(i - 1, kunciSiswa, exSatu);
									}
									i++;
								}
								if (gagal > 0) {
									System.err.println("Singkronkan Siswa: " + gagal + " dari " + ids.size()
											+ " siswa gagal disinkronkan (lihat laporan .txt untuk detail).");
								}
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

	class AnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) arg1;

			if (anggotaKoperasi.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getMahasiswa()).setParent(arg0);
			} else if (anggotaKoperasi.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getSiswa()).setParent(arg0);
			} else if (anggotaKoperasi.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getGuru()).setParent(arg0);
			} else if (anggotaKoperasi.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getDosen()).setParent(arg0);
			} else if (anggotaKoperasi.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getPegawai()).setParent(arg0);
			} else if (anggotaKoperasi.getTbmuser() != null) {
				CommonMedia.tampilkanGambarKecil(anggotaKoperasi.getTbmuser()).setParent(arg0);
			} else {
				new Label("").setParent(arg0);
			}

			new Label(anggotaKoperasi.getKode()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(AnggotaKoperasi.class, anggotaKoperasi, anggotaKoperasi.getNama()))
					.setParent(arg0);

			if (anggotaKoperasi.getMahasiswa() != null && anggotaKoperasi.getMahasiswa().getJurusan() != null) {
				new Label(anggotaKoperasi.getMahasiswa().getJurusan().getNama()).setParent(a);
			}
			if (anggotaKoperasi.getMahasiswa() != null && anggotaKoperasi.getMahasiswa().getJurusan() != null
					&& anggotaKoperasi.getMahasiswa().getJurusan().getFakultas() != null) {
				new Label(anggotaKoperasi.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(a);
			}

			if (anggotaKoperasi.getDosen() != null && anggotaKoperasi.getDosen().getJurusan() != null) {
				new Label(anggotaKoperasi.getDosen().getJurusan().getNama()).setParent(a);
			}
			if (anggotaKoperasi.getDosen() != null && anggotaKoperasi.getDosen().getFakultas() != null) {
				new Label(anggotaKoperasi.getDosen().getFakultas().getNama()).setParent(a);
			}

			new Label(anggotaKoperasi.getJenisIdentitas()).setParent(arg0);
			new Label(anggotaKoperasi.getKodeIdentitas()).setParent(arg0);

			new Label(anggotaKoperasi.getJenisAnggotaKoperasi() == null ? ""
					: anggotaKoperasi.getJenisAnggotaKoperasi().getNama()).setParent(arg0);
			new Label(anggotaKoperasi.getTipe()).setParent(arg0);
			new Label(anggotaKoperasi.getTelp()).setParent(arg0);
			new Label(anggotaKoperasi.getHp()).setParent(arg0);
			new Label(anggotaKoperasi.getEmail()).setParent(arg0);
			new Label(anggotaKoperasi.getAlamat()).setParent(arg0);
			new Label(anggotaKoperasi.getAktif() ? "Aktif" : "Tidak Aktif").setParent(arg0);
			new Label(Common.dateFormat1.get().format(anggotaKoperasi.getTanggal())).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, anggotaKoperasi, AnggotaKoperasiAction.this).setParent(arg0);
		}

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		anggotaKoperasi = (AnggotaKoperasi) obj;
		init(anggotaKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new AnggotaKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AnggotaKoperasi anggotaKoperasi) throws Exception {
		this.anggotaKoperasi = anggotaKoperasi;
		addWindow.setTitle(anggotaKoperasi.getId() == null ? "Tambah Anggota Koperasi" : "Ubah Anggota Koperasi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koperasi"));
		row.appendChild(koperasi = new Combobox());
		myKoperasi = Common.getCurrentKoperasi();
		Common.insertCombo(koperasi, "nama", Koperasi.class, Restrictions.eq("aktif", true));

		if (myKoperasi != null) {
			koperasi.setDisabled(true);
			Common.selectComboItem(true, koperasi, myKoperasi);
		}

		if (anggotaKoperasi.getKoperasi() != null) {
			Common.selectComboItem(true, koperasi, anggotaKoperasi.getKoperasi());
		}
		koperasi.setWidth("90%");
		koperasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Anggota Koperasi"));
		String mykode = anggotaKoperasi.getKode();
		row.appendChild(kode = new Textbox(anggotaKoperasi.getKode() == null ? mykode : anggotaKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Anggota Koperasi"));
		row.appendChild(tipe = new Combobox());
		Common.insertCombo(tipe, "nama", TipeAnggotaKoperasi.class);
		Common.selectComboItem(tipe, anggotaKoperasi.getTipeAnggotaKoperasi());
		tipe.setWidth("90%");
		tipe.setReadonly(true);

		Tbmuser tbmuser = anggotaKoperasi.getTbmuser();

		rowUsername.setStyle("border:0px;background: transparent;");
		rowUsername.setParent(rows);
		rowUsername.appendChild(new Label(ais.common.Common.getBahasaConfig("Username Anggota Koperasi")));
		rowUsername.appendChild(userid = new Textbox(tbmuser == null ? "" : tbmuser.getUserId()));
		userid.setWidth("90%");
		userid.setDisabled(tbmuser != null && tbmuser.getUserId() != null);

		if (anggotaKoperasi.getId() != null && tbmuser != null) {
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
		mahasiswa.setAttribute("mahasiswa", anggotaKoperasi.getMahasiswa());
		mahasiswa.setValue(anggotaKoperasi.getMahasiswa() == null ? ""
				: anggotaKoperasi.getMahasiswa().getNim() + " - " + anggotaKoperasi.getMahasiswa().getNama());
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

				JenisAnggotaKoperasi m = (JenisAnggotaKoperasi) HibernateUtil.currentSession()
						.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("nama", "NIM")).setMaxResults(1)
						.uniqueResult();
				Common.selectComboItem(jenisAnggotaKoperasi, m);
			}
		});

		rowDosen.setVisible(false);
		rowDosen.setStyle("border:0px;background: transparent;");
		rowDosen.setParent(rows);
		rowDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Dosen")));
		rowDosen.appendChild(dosen = new AmbilDataDosenBanbox(false));
		dosen.setAttribute("dosen", anggotaKoperasi.getDosen());
		dosen.setValue(anggotaKoperasi.getDosen() == null ? ""
				: anggotaKoperasi.getDosen().getCode() + " - " + anggotaKoperasi.getDosen().getNama());
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

				JenisAnggotaKoperasi m = (JenisAnggotaKoperasi) HibernateUtil.currentSession()
						.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("nama", "NIDN"))
						.setMaxResults(1).uniqueResult();
				Common.selectComboItem(jenisAnggotaKoperasi, m);
			}
		});

		rowGuru.setVisible(false);
		rowGuru.setStyle("border:0px;background: transparent;");
		rowGuru.setParent(rows);
		rowGuru.appendChild(new Label(ais.common.Common.getBahasaConfig("Guru")));
		rowGuru.appendChild(guru = new AmbilDataGuruBanbox(false));
		guru.setAttribute("guru", anggotaKoperasi.getGuru());
		guru.setValue(anggotaKoperasi.getGuru() == null ? ""
				: anggotaKoperasi.getGuru().getKode() + " - " + anggotaKoperasi.getGuru().getNama());
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

				JenisAnggotaKoperasi m = (JenisAnggotaKoperasi) HibernateUtil.currentSession()
						.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("nama", "KTP")).setMaxResults(1)
						.uniqueResult();
				Common.selectComboItem(jenisAnggotaKoperasi, m);
			}
		});

		rowPegawai.setVisible(false);
		rowPegawai.setStyle("border:0px;background: transparent;");
		rowPegawai.setParent(rows);
		rowPegawai.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		rowPegawai.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setAttribute("pegawai", anggotaKoperasi.getPegawai());
		pegawai.setValue(anggotaKoperasi.getPegawai() == null ? ""
				: anggotaKoperasi.getPegawai().getCode() + " - " + anggotaKoperasi.getPegawai().getNama());
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

				JenisAnggotaKoperasi m = (JenisAnggotaKoperasi) HibernateUtil.currentSession()
						.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("nama", "NIP")).setMaxResults(1)
						.uniqueResult();
				Common.selectComboItem(jenisAnggotaKoperasi, m);
			}
		});

		rowSiswa.setVisible(false);
		rowSiswa.setStyle("border:0px;background: transparent;");
		rowSiswa.setParent(rows);
		rowSiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa")));
		rowSiswa.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", anggotaKoperasi.getSiswa());
		siswa.setValue(anggotaKoperasi.getSiswa() == null ? ""
				: anggotaKoperasi.getSiswa().getNim() + " - " + anggotaKoperasi.getSiswa().getNama());
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

				JenisAnggotaKoperasi m = (JenisAnggotaKoperasi) HibernateUtil.currentSession()
						.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("nama", "NIS")).setMaxResults(1)
						.uniqueResult();
				Common.selectComboItem(jenisAnggotaKoperasi, m);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Anggota Koperasi"));
		row.appendChild(jenisAnggotaKoperasi = new Combobox());
		Common.insertCombo(jenisAnggotaKoperasi, "nama", JenisAnggotaKoperasi.class);
		Common.selectComboItem(jenisAnggotaKoperasi, anggotaKoperasi.getJenisAnggotaKoperasi());
		jenisAnggotaKoperasi.setWidth("90%");
		jenisAnggotaKoperasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Identitas Anggota Koperasi"));
		row.appendChild(jenisIdentitas = new Combobox());
		Common.insertCombo(jenisIdentitas, "nama", JenisIdentitasAnggotaKoperasi.class);
		Common.selectComboItem(jenisIdentitas, anggotaKoperasi.getJenisIdentitasAnggotaKoperasi());
		jenisIdentitas.setWidth("90%");
		jenisIdentitas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Identitas Anggota Koperasi"));
		row.appendChild(kodeIdentitas = new Textbox(anggotaKoperasi.getKodeIdentitas()));
		kodeIdentitas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Anggota Koperasi"));
		row.appendChild(nama = new Textbox(anggotaKoperasi.getNama() == null ? "" : anggotaKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp"));
		row.appendChild(telp = new Textbox(anggotaKoperasi.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
		row.appendChild(hp = new Textbox(anggotaKoperasi.getHp()));
		hp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(anggotaKoperasi.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(anggotaKoperasi.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pihak Terkait (Pengurus/Pengawas)"));
		row.appendChild(pihakTerkait = new MyCheckboxConfig("Batas pinjaman 10% Modal Sendiri (BMPP)"));
		pihakTerkait.setChecked(anggotaKoperasi.getPihakTerkait());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(anggotaKoperasi.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				anggotaKoperasi.getKeterangan() == null ? "" : anggotaKoperasi.getKeterangan()));
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

		myKoperasi = (Koperasi) (koperasi.getSelectedItem() == null ? null : koperasi.getSelectedItem().getValue());

		if (myKoperasi == null) {
			MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia di kolom Koperasi; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tipe.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, tipe anggota koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih tipe anggota dari daftar yang tersedia; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kode anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Anggota dengan kode unik; (2) pastikan kode belum digunakan anggota lain; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		boolean i = checkKode();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, kode anggota koperasi sudah digunakan. Langkah yang dapat dilakukan: (1) gunakan kode lain yang belum terpakai; (2) cari anggota dengan kode tersebut di daftar sebelum mendaftarkan ulang; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Anggota dengan nama lengkap; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		// if (email.getValue().trim().equals("")) {
		// MyMessageboxConfig.show("Email AnggotaKoperasi harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (jenisAnggotaKoperasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis anggota koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih jenis anggota dari daftar yang tersedia; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisIdentitas.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis identitas belum dipilih. Langkah yang dapat dilakukan: (1) pilih jenis identitas (KTP/SIM/Paspor/dll.) dari daftar yang tersedia; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeIdentitas.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nomor identitas anggota belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nomor Identitas sesuai jenis identitas yang dipilih; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// Kebijakan kontak per tipe member (TipeAnggotaKoperasi.getWajibHp/getWajibEmail;
		// getter sudah menerapkan default per nama tipe saat kolom DB masih null).
		TipeAnggotaKoperasi tipeKontak = (TipeAnggotaKoperasi) tipe.getSelectedItem().getValue();
		if (tipeKontak != null && Boolean.TRUE.equals(tipeKontak.getWajibHp()) && hp.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor HP wajib diisi untuk tipe member " + tipeKontak.getNama()
					+ ". Langkah yang dapat dilakukan: (1) isi kolom HP dengan nomor yang dapat dihubungi; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tipeKontak != null && Boolean.TRUE.equals(tipeKontak.getWajibEmail()) && email.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Email wajib diisi untuk tipe member " + tipeKontak.getNama()
					+ ". Langkah yang dapat dilakukan: (1) isi kolom Email dengan alamat email yang aktif; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!email.getValue().trim().isEmpty() && !Common.isValidEmailAddress(email.getValue().trim())) {
			MyMessageboxConfig.show("Mohon maaf, format email tidak valid. Langkah yang dapat dilakukan: (1) periksa kembali alamat email (contoh: nama@domain.com); (2) hapus spasi atau karakter tidak valid; (3) ulangi penyimpanan.", "Informasi", MyMessageboxConfig.OK,
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

		Tbmuser tbmuser = anggotaKoperasi.getTbmuser();
		if (rowUsername.isVisible()) {

			if (userid.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, username anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Username dengan nama pengguna yang unik; (2) pastikan username belum dipakai pengguna lain; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (userPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, password anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Password dengan kata sandi yang aman (minimal 6 karakter); (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
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

			if (tbmuser != null && tbmuser.getDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				MyMessageboxConfig.show("Username ini merupakan pengguna Dosen. Langkah yang dapat dilakukan: (1) ubah Tipe Anggota menjadi Dosen; (2) atau pilih username pengguna lain yang sesuai tipe anggota yang dipilih; (3) ulangi penyimpanan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				MyMessageboxConfig.show(
						"Username ini merupakan pengguna Mahasiswa. Langkah yang dapat dilakukan: (1) ubah Tipe Anggota menjadi Mahasiswa; (2) atau pilih username pengguna lain yang sesuai tipe anggota yang dipilih; (3) ulangi penyimpanan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.getSiswa() != null) {
				MyMessageboxConfig.show("Username ini merupakan pengguna Siswa. Langkah yang dapat dilakukan: (1) ubah Tipe Anggota menjadi Siswa; (2) atau pilih username pengguna lain yang sesuai tipe anggota yang dipilih; (3) ulangi penyimpanan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.getGuru() != null) {
				MyMessageboxConfig.show("Username ini merupakan pengguna Guru. Langkah yang dapat dilakukan: (1) ubah Tipe Anggota menjadi Guru; (2) atau pilih username pengguna lain yang sesuai tipe anggota yang dipilih; (3) ulangi penyimpanan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser != null && tbmuser.ambilPegawai() != null) {
				MyMessageboxConfig.show(
						"Username ini merupakan pengguna Pegawai. Langkah yang dapat dilakukan: (1) ubah Tipe Anggota menjadi Pegawai; (2) atau pilih username pengguna lain yang sesuai tipe anggota yang dipilih; (3) ulangi penyimpanan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (tbmuser == null || tbmuser.getUserId() == null) {
				tbmuser = new Tbmuser();
				tbmuser.setUserId(userid.getValue().trim());
				tbmuser.setUserRole(ConstantValues.roleAnggotaKoperasi);
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
		if (anggotaKoperasi.getId() != null) {
			anggotaKoperasi = (AnggotaKoperasi) session.load(AnggotaKoperasi.class, anggotaKoperasi.getId());
		}

		anggotaKoperasi.setSiswa((Siswa) siswa.getAttribute("siswa"));
		anggotaKoperasi.setGuru((Guru) guru.getAttribute("guru"));
		anggotaKoperasi.setAktif(aktif.isChecked());
		anggotaKoperasi.setPihakTerkait(pihakTerkait.isChecked());
		anggotaKoperasi.setTipeAnggotaKoperasi((TipeAnggotaKoperasi) tipe.getSelectedItem().getValue());
		anggotaKoperasi.setJenisIdentitasAnggotaKoperasi(
				(JenisIdentitasAnggotaKoperasi) jenisIdentitas.getSelectedItem().getValue());
		anggotaKoperasi.setHp(hp.getValue());
		anggotaKoperasi.setTelp(telp.getValue());
		anggotaKoperasi.setEmail(email.getValue());
		anggotaKoperasi.setJenisIdentitas(jenisIdentitas.getSelectedItem().getLabel());
		anggotaKoperasi.setKodeIdentitas(kodeIdentitas.getValue().trim());
		anggotaKoperasi.setAlamat(alamat.getValue());
		anggotaKoperasi.setTipe(tipe.getSelectedItem().getLabel());
		anggotaKoperasi.setMahasiswa(null);
		anggotaKoperasi.setDosen(null);
		anggotaKoperasi.setGuru(null);
		anggotaKoperasi.setPegawai(null);
		anggotaKoperasi.setKode(kode.getValue().trim());
		anggotaKoperasi.setNama(nama.getValue());
		anggotaKoperasi.setKeterangan(keterangan.getValue());
		anggotaKoperasi
				.setJenisAnggotaKoperasi((JenisAnggotaKoperasi) (jenisAnggotaKoperasi.getSelectedItem() == null ? null
						: jenisAnggotaKoperasi.getSelectedItem().getValue()));
		anggotaKoperasi.setDibuatOleh(Common.getCurrentUser());
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Mahasiswa")) {
			anggotaKoperasi.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		}
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Dosen")) {
			anggotaKoperasi.setDosen((Dosen) dosen.getAttribute("dosen"));
		}
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Guru")) {
			anggotaKoperasi.setGuru((Guru) guru.getAttribute("guru"));
		}
		if (((Comboitem) tipe.getSelectedItem()).getLabel().equals("Pegawai")) {
			anggotaKoperasi.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		}

		anggotaKoperasi.setTbmuser(tbmuser);
		anggotaKoperasi.setKoperasi(myKoperasi);

		Common.refreshSaveOrUpdate(session, anggotaKoperasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AnggotaKoperasi.class)

				.add(searchStatus.getSelectedItem() == null || searchStatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("aktif", searchStatus.getSelectedItem().getValue()))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("mahasiswa.tahunangkatan")).addOrder(Order.asc("nama"));
		criteria

				.add(searchKoperasi.getSelectedItem() == null || searchKoperasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("koperasi", searchKoperasi.getSelectedItem().getValue()))

				.add(searchTipeAnggotaKoperasi.getSelectedItem() == null
						|| searchTipeAnggotaKoperasi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tipeAnggotaKoperasi",
										searchTipeAnggotaKoperasi.getSelectedItem().getValue()))

				.add(searchJenisAnggotaKoperasi.getSelectedItem() == null
						|| searchJenisAnggotaKoperasi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisAnggotaKoperasi",
										searchJenisAnggotaKoperasi.getSelectedItem().getValue()))

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

		List<AnggotaKoperasi> anggotaKoperasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(anggotaKoperasi);
		grid.setRowRenderer(new AnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKode() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AnggotaKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.anggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.anggotaKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkEmail() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AnggotaKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("email", email.getValue().trim()))
				.add(this.anggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.anggotaKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * Fitur "Dasbor Statistik Anggota" -- gap-closure permintaan kartu ringkasan (total/aktif/nonaktif/
	 * wajib PIN) + rincian batang per jenis keanggotaan LANGSUNG di layar CRUD Anggota ini, pola SAMA
	 * PERSIS dgn {@code ProdukAction.tampilkanStatistikProduk}. Query SQL disalin persis dari
	 * {@code KantinHelper.anggotaStatistik} (aksi server yang SAMA dipakai Desktop/Android) -- SENGAJA
	 * TIDAK di-scope per toko (method server itu sendiri org-wide, anggota koperasi bukan entitas
	 * per-toko spt produk) supaya angkanya taat asas lintas ketiga tampilan. Dihitung SEKALI saat
	 * halaman dibuka, TIDAK ikut berubah mengikuti filter Jenis/Tipe/Status di atas.
	 */
	private void tampilkanStatistikAnggota(Component comp) {
		try {
			Session session = HibernateUtil.currentSession();
			java.sql.Connection conn = session.connection();

			long totalAnggota = 0, totalAktif = 0, totalNonaktif = 0, totalWajibPin = 0;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT COUNT(*), COUNT(CASE WHEN COALESCE(a.aktif,true)=true THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(a.aktif,true)=false THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(j.wajib_pin,false)=true THEN 1 END) "
							+ "FROM koperasi.anggota_koperasi a "
							+ "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id");
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			if (rsKpi.next()) {
				totalAnggota = rsKpi.getLong(1);
				totalAktif = rsKpi.getLong(2);
				totalNonaktif = rsKpi.getLong(3);
				totalWajibPin = rsKpi.getLong(4);
			}
			rsKpi.close(); psKpi.close();

			java.util.LinkedHashMap<String, Double> byJenis = new java.util.LinkedHashMap<String, Double>();
			java.sql.PreparedStatement psJenis = conn.prepareStatement(
					"SELECT COALESCE(j.nama,'Tanpa Jenis') lbl, COUNT(*) cnt FROM koperasi.anggota_koperasi a "
							+ "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id "
							+ "GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			java.sql.ResultSet rsJenis = psJenis.executeQuery();
			while (rsJenis.next()) byJenis.put(rsJenis.getString(1), rsJenis.getDouble(2));
			rsJenis.close(); psJenis.close();

			java.util.List<ais.ui.util.DashboardUiKit.Stat> kartu = new java.util.ArrayList<ais.ui.util.DashboardUiKit.Stat>();
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Total Anggota", ais.ui.util.DashboardUiKit.money(totalAnggota), null, ais.ui.util.DashboardUiKit.PRIMARY));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Aktif", ais.ui.util.DashboardUiKit.money(totalAktif), null, ais.ui.util.DashboardUiKit.GOOD));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Non-Aktif", ais.ui.util.DashboardUiKit.money(totalNonaktif), null, ais.ui.util.DashboardUiKit.MUTED));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Wajib PIN", ais.ui.util.DashboardUiKit.money(totalWajibPin), null, ais.ui.util.DashboardUiKit.WARN));

			StringBuilder sb = new StringBuilder();
			sb.append(ais.ui.util.DashboardUiKit.cards(kartu));
			sb.append(ais.ui.util.DashboardUiKit.openGrid(260));
			sb.append(ais.ui.util.DashboardUiKit.barList("Per Jenis Keanggotaan", null, byJenis, ais.ui.util.DashboardUiKit.PRIMARY, "anggota", false, "Belum ada anggota."));
			sb.append(ais.ui.util.DashboardUiKit.closeGrid());

			ais.ui.util.DashboardUiKit.html(sb.toString()).setParent(comp);
		} catch (Exception e) {
			// Dasbor statistik gagal dihitung TIDAK BOLEH menggagalkan layar Anggota itu sendiri --
			// CRUD tetap harus bisa dipakai walau statistik tak tampil.
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnggotaKoperasiAction.tampilkanStatistikAnggota");
		}
	}

}
