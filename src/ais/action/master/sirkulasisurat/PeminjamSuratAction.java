package ais.action.master.sirkulasisurat;

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
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PeminjamSuratAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataPegawaiBanbox pegawai;
	private Textbox kode;
	private Textbox kodeIdentitas;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox telp;
	private Textbox hp;
	private Textbox email;
	private MyCheckboxConfig aktif;

	private boolean edit = false;
	private boolean delete = false;

	private PeminjamSurat peminjamSurat;
	private MyToolbarbuttonConfig add;
	private Textbox alamat;
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
				"perpustakaan", "mahasiswa", "dosen", "pegawai", "tbmuser", "tipe", "keterangan", "telp", "hp", "email",
				"aktif", "tanggal", "" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PeminjamSurat.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan Mahasiswa", "/img/excel.png");
		Common.appendKeToolbar(singkron, add, comp);
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_mahasiswa_dengan_peminjam_surat"));
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
							MyMessageboxConfig.show("Mohon maaf, Tahun Angkatan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Angkatan dengan tahun yang sesuai; (2) pastikan format tahun yang dimasukkan benar (contoh: 2023); (3) ulangi proses sinkronisasi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peminjam Surat Mahasiswa");

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
									label.setValue("Singkronkan peminjamSurat perpustakaan dengan nim " + nim + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahMahasiswaOtomatisMenjadiPeminjamSuratPerpustakaan(nim);
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
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_dosen_dengan_peminjam_surat"));
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

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peminjam Surat Dosen");

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
									label.setValue("Singkronkan peminjamSurat perpustakaan dengan nidn " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahDosenOtomatisMenjadiPeminjamSuratPerpustakaan(nidn);
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
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_pegawai_dengan_peminjam_surat"));
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

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peminjam Surat Pegawai");

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
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
								int i = 1;
								for (String nidn : ids) {
									label.setValue("Singkronkan peminjamSurat perpustakaan dengan NIK " + nidn + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									try {
										Common.checkApakahPegawaiOtomatisMenjadiPeminjamSuratPerpustakaan(
												nidn);
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
		singkron.setVisible(Common.bolehKonfigurasi("singkronkan_siswa_dengan_peminjam_surat"));
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
							MyMessageboxConfig.show("Mohon maaf, Tahun Angkatan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Angkatan dengan tahun yang sesuai; (2) pastikan format tahun yang dimasukkan benar (contoh: 2023); (3) ulangi proses sinkronisasi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Peminjam Surat Siswa");

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
									label.setValue("Singkronkan peminjamSurat perpustakaan dengan " + siswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciSiswa = String.valueOf(siswa);
									try {
										Common.checkApakahSiswaOtomatisMenjadiPeminjamSuratPerpustakaan(siswa);
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

	class PeminjamSuratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PeminjamSurat peminjamSurat = (PeminjamSurat) arg1;

			if (peminjamSurat.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getMahasiswa()).setParent(arg0);
			} else if (peminjamSurat.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getSiswa()).setParent(arg0);
			} else if (peminjamSurat.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getGuru()).setParent(arg0);
			} else if (peminjamSurat.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getDosen()).setParent(arg0);
			} else if (peminjamSurat.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getPegawai()).setParent(arg0);
			} else if (peminjamSurat.getTbmuser() != null) {
				CommonMedia.tampilkanGambarKecil(peminjamSurat.getTbmuser()).setParent(arg0);
			} else {
				new Label("").setParent(arg0);
			}

			new Label(peminjamSurat.getKode()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PeminjamSurat.class, peminjamSurat, peminjamSurat.getNama()))
					.setParent(arg0);

			if (peminjamSurat.getMahasiswa() != null && peminjamSurat.getMahasiswa().getJurusan() != null) {
				new Label(peminjamSurat.getMahasiswa().getJurusan().getNama()).setParent(a);
			}
			if (peminjamSurat.getMahasiswa() != null && peminjamSurat.getMahasiswa().getJurusan() != null
					&& peminjamSurat.getMahasiswa().getJurusan().getFakultas() != null) {
				new Label(peminjamSurat.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(a);
			}

			if (peminjamSurat.getDosen() != null && peminjamSurat.getDosen().getJurusan() != null) {
				new Label(peminjamSurat.getDosen().getJurusan().getNama()).setParent(a);
			}
			if (peminjamSurat.getDosen() != null && peminjamSurat.getDosen().getFakultas() != null) {
				new Label(peminjamSurat.getDosen().getFakultas().getNama()).setParent(a);
			}

			new Label(peminjamSurat.getKodeIdentitas()).setParent(arg0);
			a = new Vbox();
			a.setParent(arg0);
			new Label(peminjamSurat.getTelp()).setParent(a);
			new Label(peminjamSurat.getHp()).setParent(a);
			new Label(peminjamSurat.getEmail()).setParent(arg0);
			new Label(peminjamSurat.getAlamat()).setParent(arg0);
			new Label(peminjamSurat.getAktif() ? "Aktif" : "Tidak Aktif").setParent(arg0);
			new Label(Common.dateFormat1.get().format(peminjamSurat.getTanggal())).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(peminjamSurat);
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

											Common.refreshDelete(peminjamSurat);

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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PeminjamSurat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PeminjamSurat peminjamSurat) throws Exception {
		this.peminjamSurat = peminjamSurat;
		addWindow.setTitle(peminjamSurat.getId() == null ? "Tambah Peminjam Surat" : "Ubah Peminjam Surat");
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

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Peminjam Surat"));
		String mykode = peminjamSurat.getKode();
		row.appendChild(kode = new Textbox(peminjamSurat.getKode() == null ? mykode : peminjamSurat.getKode()));
		kode.setWidth("90%");

		rowMahasiswa.setVisible(Common.bolehKonfigurasi("singkronkan_mahasiswa_dengan_peminjam_surat"));
		rowMahasiswa.setStyle("border:0px;background: transparent;");
		rowMahasiswa.setParent(rows);
		rowMahasiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa")));
		rowMahasiswa.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", peminjamSurat.getMahasiswa());
		mahasiswa.setValue(peminjamSurat.getMahasiswa() == null ? ""
				: peminjamSurat.getMahasiswa().getNim() + " - " + peminjamSurat.getMahasiswa().getNama());
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

			}
		});

		rowDosen.setVisible(Common.bolehKonfigurasi("singkronkan_dosen_dengan_peminjam_surat"));
		rowDosen.setStyle("border:0px;background: transparent;");
		rowDosen.setParent(rows);
		rowDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Dosen")));
		rowDosen.appendChild(dosen = new AmbilDataDosenBanbox(false));
		dosen.setAttribute("dosen", peminjamSurat.getDosen());
		dosen.setValue(peminjamSurat.getDosen() == null ? ""
				: peminjamSurat.getDosen().getCode() + " - " + peminjamSurat.getDosen().getNama());
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

			}
		});

		rowGuru.setVisible(Common.bolehKonfigurasi("singkronkan_guru_dengan_peminjam_surat"));
		rowGuru.setStyle("border:0px;background: transparent;");
		rowGuru.setParent(rows);
		rowGuru.appendChild(new Label(ais.common.Common.getBahasaConfig("Guru")));
		rowGuru.appendChild(guru = new AmbilDataGuruBanbox(false));
		guru.setAttribute("guru", peminjamSurat.getGuru());
		guru.setValue(peminjamSurat.getGuru() == null ? ""
				: peminjamSurat.getGuru().getKode() + " - " + peminjamSurat.getGuru().getNama());
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

			}
		});

		rowPegawai.setVisible(Common.bolehKonfigurasi("singkronkan_pegawai_dengan_peminjam_surat"));
		rowPegawai.setStyle("border:0px;background: transparent;");
		rowPegawai.setParent(rows);
		rowPegawai.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		rowPegawai.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setAttribute("pegawai", peminjamSurat.getPegawai());
		pegawai.setValue(peminjamSurat.getPegawai() == null ? ""
				: peminjamSurat.getPegawai().getCode() + " - " + peminjamSurat.getPegawai().getNama());
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

			}
		});

		rowSiswa.setVisible(Common.bolehKonfigurasi("singkronkan_siswa_dengan_peminjam_surat"));
		rowSiswa.setStyle("border:0px;background: transparent;");
		rowSiswa.setParent(rows);
		rowSiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa")));
		rowSiswa.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", peminjamSurat.getSiswa());
		siswa.setValue(peminjamSurat.getSiswa() == null ? ""
				: peminjamSurat.getSiswa().getNim() + " - " + peminjamSurat.getSiswa().getNama());
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

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Identitas Peminjam"));
		row.appendChild(kodeIdentitas = new Textbox(peminjamSurat.getKodeIdentitas()));
		kodeIdentitas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Peminjam"));
		row.appendChild(nama = new Textbox(peminjamSurat.getNama() == null ? "" : peminjamSurat.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp"));
		row.appendChild(telp = new Textbox(peminjamSurat.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
		row.appendChild(hp = new Textbox(peminjamSurat.getHp()));
		hp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(peminjamSurat.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(peminjamSurat.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(peminjamSurat.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(peminjamSurat.getKeterangan() == null ? "" : peminjamSurat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Peminjam belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Peminjam; (2) isikan kode peminjam secara lengkap dan benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Peminjam belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Peminjam; (2) isikan nama peminjam secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeIdentitas.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Identitas Peminjam belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Identitas/Nomor Identitas Peminjam; (2) isikan nomor identitas yang sesuai (KTP/NIM/NIP); (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!email.getValue().trim().isEmpty() && !Common.isValidEmailAddress(email.getValue().trim())) {
			MyMessageboxConfig.show("Mohon maaf, format alamat email yang dimasukkan tidak valid. Langkah yang dapat dilakukan: (1) periksa kembali kolom Email; (2) pastikan format email benar, contoh: nama@domain.com; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (peminjamSurat.getId() != null) {
			peminjamSurat = (PeminjamSurat) session.load(PeminjamSurat.class, peminjamSurat.getId());
		}

		peminjamSurat.setSiswa((Siswa) siswa.getAttribute("siswa"));
		peminjamSurat.setGuru((Guru) guru.getAttribute("guru"));
		peminjamSurat.setAktif(aktif.isChecked());

		peminjamSurat.setHp(hp.getValue());
		peminjamSurat.setTelp(telp.getValue());
		peminjamSurat.setEmail(email.getValue());
		peminjamSurat.setKodeIdentitas(kodeIdentitas.getValue().trim());
		peminjamSurat.setAlamat(alamat.getValue());
		peminjamSurat.setMahasiswa(null);
		peminjamSurat.setDosen(null);
		peminjamSurat.setPegawai(null);
		peminjamSurat.setGuru(null);
		peminjamSurat.setKode(kode.getValue().trim());
		peminjamSurat.setNama(nama.getValue());
		peminjamSurat.setKeterangan(keterangan.getValue());
		peminjamSurat.setDibuatOleh(Common.getCurrentUser());
		peminjamSurat.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		peminjamSurat.setDosen((Dosen) dosen.getAttribute("dosen"));
		peminjamSurat.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		peminjamSurat.setGuru((Guru) guru.getAttribute("dosen"));

		Common.refreshSaveOrUpdate(session, peminjamSurat);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PeminjamSurat.class)

		;
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria

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

		List<PeminjamSurat> peminjamSurat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(peminjamSurat);
		grid.setRowRenderer(new PeminjamSuratRenderer());
		grid.setModelCheckMobile(strset);

	}

}
