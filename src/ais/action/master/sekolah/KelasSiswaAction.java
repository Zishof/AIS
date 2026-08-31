package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.DetailKelasSiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kelas siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchta}, {@code Textbox searchnama}, {@code Textbox
 * searchsiswa}, {@code Textbox searchruang}, {@code MyIntbox searchtingkat}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
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
public class KelasSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchta;
	private Textbox searchnama;
	private Textbox searchsiswa;
	private Textbox searchruang;
	private MyIntbox searchtingkat;
	private Checkbox searchaktif;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataGuruBanbox searchguruPembina;
	private AmbilDataGuruBanbox searchguruBk;

	private Textbox nama;
	private AmbilDataRuangBanbox ruang;
	private Combobox sekolah;
	private Intbox tingkat;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelasSiswa kelasSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox tahunAjaran;
	private Combobox kurikulumSekolah;
	private AmbilDataGuruBanbox guruPembina;
	private Textbox namaEn;
	private Textbox namaAr;
	private Textbox namaCh;
	private AmbilDataGuruBanbox guruBk;

	private MyCheckboxConfig absensiharusGuruPembina;
	private MyCheckboxConfig absensiharusGuruBk;
	private Tbmuser tbmuser;

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
		Common.generateTahunAjaran(searchta);
		tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchguruPembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchguruBk.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "nama", "ruang", "sekolah", "yayasan", "tingkat", "kapasitas", "aktif",
				"keterangan", "kurikulumSekolah", "guruPembina", "absensiharusGuruPembina", "guruBk",
				"absensiharusGuruBk", "tahunAjaran" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelasSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Ajaran", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columnsa = new Columns();
				columnsa.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columnsa);
				column = new MyColumnConfig();
				column.setParent(columnsa);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				final Combobox yayasan = new Combobox();
				final Combobox sekolah = new Combobox();
				Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
				row.appendChild(yayasan);
				yayasan.setWidth("90%");
				yayasan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						if (tahunAkademik.getSelectedItem() == null
								|| tahunAkademik.getSelectedItem().getValue() == null) {
							MyMessageboxConfig.show("Tahun Ajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						window.detach();

						String ta = (String) tahunAkademik.getSelectedItem().getValue();

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Kelas Siswa");

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								laporan.selesaikan(new EventListener() {
									@Override
									public void onEvent(Event event2) throws Exception {
										onSearchDefault(null);
									}
								});
							}
						});

						final List<KelasSiswa> kelases = HibernateUtil.currentSession().createCriteria(KelasSiswa.class)
								.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))
								.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))
								.add(Restrictions.eq("tahunAjaran", ta)).list();
						new Thread(new Runnable() {

							@Override
							public void run() {
								try {

								Session session = HibernateUtil.currentNativeSession();
								int baris = 1;
								for (KelasSiswa kelasSiswa : kelases) {
									try {
										Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
												.setProjection(Projections.groupProperty("siswa.id"))
												.add(Restrictions.eq("kelasSiswa", kelasSiswa));

										List<Long> siswaIds = criteria.list();

										int rowIndex = 1;
										for (Long siswaId : siswaIds) {
											Siswa siswa = siswaId == null ? null : (Siswa) session.get(Siswa.class, siswaId);
											if (siswa == null) {
												rowIndex++;
												continue;
											}
											String namaSiswa = siswa.getNama() == null ? siswa.getId().toString() : siswa.getNama();
											label.setValue("Sedang memproses data " + namaSiswa + " di kelas "
													+ kelasSiswa.getNama() + " ("
													+ Common.numberFormat.get().format(rowIndex * 100.0 / siswaIds.size())
													+ " %)");

											String kunci = kelasSiswa.getNama() + " - " + namaSiswa;
											try {
												/*
												 * Jangan menyimpan melalui siswa.setKelas() + update entity. Properti
												 * current_kelas_id dipetakan lewat Siswa.getKelas(), sedangkan getter itu
												 * juga mencari kelas secara dinamis berdasarkan TA dan cache. Saat flush,
												 * Hibernate memanggil getter tersebut kembali sehingga kelas yang baru
												 * dipilih dapat berubah menjadi null/kelas lama. Perbarui kolom FK secara
												 * langsung, sama dengan jalur upload anggota kelas.
												 */
												session.getTransaction().begin();
												session.createSQLQuery(
														"update sekolah.siswa set current_kelas_id=:kelasId where id=:siswaId")
														.setLong("kelasId", kelasSiswa.getId().longValue())
														.setLong("siswaId", siswaId.longValue()).executeUpdate();
												session.getTransaction().commit();
												session.clear();
												laporan.catatBerhasil(baris - 1, kunci, "Sinkronisasi berhasil");
											} catch (Exception ePerSiswa) {
												try {
													if (session.getTransaction() != null && session.getTransaction().isActive()) {
														session.getTransaction().rollback();
													}
												} catch (Exception exRollback) { ais.common.ErrorAuditUtil.record(exRollback, "auto-audit(empty-catch) src/ais/action/master/sekolah/KelasSiswaAction.java:rollback-sync-kelas"); }
												try { session.clear(); } catch (Exception exClear) { ais.common.ErrorAuditUtil.record(exClear, "auto-audit(empty-catch) src/ais/action/master/sekolah/KelasSiswaAction.java:clear-sync-kelas"); }
												Common.tampilErrorJikaAdmin(ePerSiswa);
												laporan.catatGagalDetail(baris - 1, kunci, ePerSiswa);
											}

											rowIndex++;
											baris++;
										}
										siswaIds.clear();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										laporan.tambahCatatan("Kelas " + kelasSiswa.getNama() + " gagal diproses: "
												+ ais.common.LaporanUpload.detailTeknisException(e));
									}
								}
								HibernateUtil.closeSession();
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
		Common.appendKeToolbar(button, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelasSiswaAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelasSiswaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DetailKelasSiswaHelper
	 * detailKelasSiswaHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelasSiswaAction
	 */
	class KelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelasSiswaHelper detailKelasSiswaHelper = new DetailKelasSiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswa kelasSiswa = (KelasSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						detailKelasSiswaHelper.displayDetailPA(kelasSiswa, detail, addWindow);
					}

				}

			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KelasSiswa.class, kelasSiswa, kelasSiswa.getNama())).setParent(arg0);

			if (!kelasSiswa.getNama().equalsIgnoreCase(kelasSiswa.getNamaEn())) {
				new Label(kelasSiswa.getNamaEn()).setParent(a);
			}
			if (!kelasSiswa.getNama().equalsIgnoreCase(kelasSiswa.getNamaAr())) {
				new Label(kelasSiswa.getNamaAr()).setParent(a);
			}
			if (!kelasSiswa.getNama().equalsIgnoreCase(kelasSiswa.getNamaCh())) {
				new Label(kelasSiswa.getNamaCh()).setParent(a);
			}

			new Label(kelasSiswa.getRuang() == null ? ""
					: kelasSiswa.getRuang().getKodeRuangan() + "-" + kelasSiswa.getRuang().getNama()).setParent(arg0);
			new Label(kelasSiswa.getSekolah() == null ? "" : kelasSiswa.getSekolah().getNama()).setParent(arg0);
			new Label(kelasSiswa.getTingkat().toString()).setParent(arg0);
			new Label(kelasSiswa.getTahunAjaran()).setParent(arg0);

			new Label(kelasSiswa.getKurikulumSekolah() == null ? "" : kelasSiswa.getKurikulumSekolah().getNama())
					.setParent(arg0);

			try {
				Guru guru = kelasSiswa.getGuruPembina();
				new Label(guru == null ? "" : guru.getNamaGuru()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			try {
				Guru guru = kelasSiswa.getGuruBk();
				new Label(guru == null ? "" : guru.getNamaGuru()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}

			int count = ((Number) HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", kelasSiswa)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);

			new Label(kelasSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelasSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelasSiswa);
				}
			});

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, edit, delete, kelasSiswa, KelasSiswaAction.this))
					.setParent(arg0);

			GeneralValueObject.tampilKunci(toolbar, kelasSiswa, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelasSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelasSiswa = (KelasSiswa) obj;
		init(kelasSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private List<Checkbox> checkboxs = new ArrayList<Checkbox>();
	private MyCheckboxConfig publikasiNilaiHarusTelahDiverifikasi;
	private MyCheckboxConfig guruBolehMemverifikasiSendiri;

	@SuppressWarnings("deprecation")
	private void init(final KelasSiswa kelasSiswa) throws Exception {
		this.kelasSiswa = kelasSiswa;
		addWindow.setTitle(kelasSiswa.getId() == null ? "Tambah Kelas Siswa" : "Ubah Kelas Siswa");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				kelasSiswa.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Indonesia) *"));
		row.appendChild(nama = new Textbox(kelasSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (English)"));
		row.appendChild(namaEn = new Textbox(kelasSiswa.getNamaEn()));
		namaEn.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Aksara Arab)"));
		row.appendChild(namaAr = new Textbox(kelasSiswa.getNamaAr()));
		namaAr.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Aksara Tionghoa)"));
		row.appendChild(namaCh = new Textbox(kelasSiswa.getNamaCh()));
		namaCh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruangan"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setAttribute("ruang", kelasSiswa.getRuang());
		ruang.setValue(kelasSiswa.getRuang() == null ? ""
				: kelasSiswa.getRuang().getKodeRuangan() + "-" + kelasSiswa.getRuang().getNama());
		ruang.setWidth("90%");
		ruang.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kelasSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, kelasSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat"));
		row.appendChild(tingkat = new Intbox(kelasSiswa.getTingkat()));

		row = new MyFormRow();
		row.setParent(rows);

		guruPembina = new AmbilDataGuruBanbox();
		if (searchguruPembina.getAttribute("guru") != null) {
			kelasSiswa.setGuruPembina((Guru) searchguruPembina.getAttribute("guru"));
			guruPembina.setDisabled(searchguruPembina.isDisabled());
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Wali Kelas"));

		row.appendChild(guruPembina);
		guruPembina.setAttribute("guru", kelasSiswa.getGuruPembina());
		guruPembina.setAttribute("myValue", kelasSiswa.getGuruPembina());
		try {
			guruPembina.setValue(kelasSiswa.getGuruPembina() == null ? "" : kelasSiswa.getGuruPembina().getNamaGuru());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/KelasSiswaAction.java:577");
			// TODO: handle exception
		}
		guruPembina.setWidth("90%");
		guruPembina.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		absensiharusGuruPembina = new MyCheckboxConfig("Hanya boleh di-absen oleh Wali Kelas");
		row.appendChild(absensiharusGuruPembina);
		absensiharusGuruPembina.setChecked(kelasSiswa.getAbsensiharusGuruPembina());

		row = new MyFormRow();
		row.setParent(rows);

		guruBk = new AmbilDataGuruBanbox();
		if (searchguruBk.getAttribute("guru") != null) {
			kelasSiswa.setGuruBk((Guru) searchguruBk.getAttribute("guru"));
			guruBk.setDisabled(searchguruBk.isDisabled());
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru BK"));

		row.appendChild(guruBk);
		guruBk.setAttribute("guru", kelasSiswa.getGuruBk());
		guruBk.setAttribute("myValue", kelasSiswa.getGuruBk());
		try {
			guruBk.setValue(kelasSiswa.getGuruBk() == null ? "" : kelasSiswa.getGuruBk().getNamaGuru());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/KelasSiswaAction.java:605");
			// TODO: handle exception
		}
		guruBk.setWidth("90%");
		guruBk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		absensiharusGuruBk = new MyCheckboxConfig("Hanya boleh di-absen oleh Guru BK");
		row.appendChild(absensiharusGuruBk);
		absensiharusGuruBk.setChecked(kelasSiswa.getAbsensiharusGuruBk());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		publikasiNilaiHarusTelahDiverifikasi = new MyCheckboxConfig("Publikasi Nilai Harus Telah Diverifikasi");
		row.appendChild(publikasiNilaiHarusTelahDiverifikasi);
		publikasiNilaiHarusTelahDiverifikasi.setChecked(kelasSiswa.getPublikasiNilaiHarusTelahDiverifikasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		guruBolehMemverifikasiSendiri = new MyCheckboxConfig("Guru Boleh Mem-verifikasi Sendiri Nilai");
		row.appendChild(guruBolehMemverifikasiSendiri);
		guruBolehMemverifikasiSendiri.setChecked(kelasSiswa.getGuruBolehMemverifikasiSendiri());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelasSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum *"));
		row.appendChild(kurikulumSekolah = new Combobox());
		kurikulumSekolah.setWidth("90%");
		kurikulumSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Matapelajaran yang tidak diajarkan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowdata = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener sKurikulum = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowdata);
				checkboxs.clear();
				KurikulumSekolah myKurikulumSekolah = (KurikulumSekolah) (kurikulumSekolah.getSelectedItem() == null
						? null
						: kurikulumSekolah.getSelectedItem().getValue());
				if (myKurikulumSekolah != null) {

					Session session = HibernateUtil.currentSession();
					List<Matapelajaran> matapelajarans = ConstantValues
							.simpleList(session.createCriteria(KurikulumPunyaMatapelajaran.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.property("matapelajaran.id"))
									.add(Restrictions.eq("kurikulumSekolah", myKurikulumSekolah))
									.createAlias("matapelajaran", "matapelajaran")
									.add(Restrictions.eq("matapelajaran.aktif", true))
									.addOrder(Order.asc("matapelajaran.urutan")), Matapelajaran.class, false);

					System.out.println("matapelajarans -> " + matapelajarans.size());

					List<Long> longs = kelasSiswa.ambilMk();

					for (Matapelajaran matapelajaran : matapelajarans) {

						MyFormRow rowData1 = new MyFormRow();
						rowData1.setParent(rowdata);

						Checkbox checkbox = new Checkbox(matapelajaran.getNama());
						checkbox.setAttribute("matapelajaran", matapelajaran);
						checkbox.setChecked(longs.contains(matapelajaran.getId()));
						checkboxs.add(checkbox);
						rowData1.appendChild(checkbox);

					}
				}
			}

		};

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kurikulumSekolah);

				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? Common.getCurrentUser().ambilSekolah()
								: sekolah.getSelectedItem().getValue());
				if (mySekolah != null) {
					Common.insertCombo(kurikulumSekolah, "nama", "sekolah", KurikulumSekolah.class,
							Restrictions.and(Restrictions.eq("aktif", true), Restrictions.eq("sekolah", mySekolah)));
				}
				Common.selectComboItem(true, kurikulumSekolah, kelasSiswa.getKurikulumSekolah());
				Common.createDefaultTimer(sKurikulum);

			}
		};
		sekolah.addEventListener("onChange", s);
		kurikulumSekolah.addEventListener("onChange", sKurikulum);

		Common.createDefaultTimer(s);

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

		if (kelasSiswa.getDikunci() != null) {
			save.setVisible(false);
			Common.freezeGanti(center, true);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kelas harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kurikulumSekolah.getSelectedItem() == null || kurikulumSekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Kurikulum harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();

		KelasSiswa copyDari = (KelasSiswa) kelasSiswa.getCopyDari();

		if (kelasSiswa.getId() != null) {
			kelasSiswa = (KelasSiswa) session.load(KelasSiswa.class, kelasSiswa.getId());

		}
		kelasSiswa.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		kelasSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		kelasSiswa.setNama(nama.getValue());

		kelasSiswa.setNamaAr(namaAr.getValue());
		kelasSiswa.setNamaCh(namaCh.getValue());
		kelasSiswa.setNamaEn(namaEn.getValue());

		kelasSiswa.setRuang((Ruang) ruang.getAttribute("ruang"));
		kelasSiswa.setTingkat(tingkat.getValue());
		kelasSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		kelasSiswa.setKeterangan(keterangan.getValue());
		kelasSiswa.setKurikulumSekolah((KurikulumSekolah) kurikulumSekolah.getSelectedItem().getValue());
		kelasSiswa.setGuruPembina((Guru) guruPembina.getAttribute("guru"));
		kelasSiswa.setGuruBk((Guru) guruBk.getAttribute("guru"));

		kelasSiswa.setAbsensiharusGuruBk(absensiharusGuruBk.isChecked());
		kelasSiswa.setAbsensiharusGuruPembina(absensiharusGuruPembina.isChecked());

		kelasSiswa.setGuruBolehMemverifikasiSendiri(guruBolehMemverifikasiSendiri.isChecked());
		kelasSiswa.setPublikasiNilaiHarusTelahDiverifikasi(publikasiNilaiHarusTelahDiverifikasi.isChecked());

		JSONArray array = new JSONArray();
		for (Checkbox checkbox : checkboxs) {
			if (checkbox.isChecked()) {
				Matapelajaran matapelajaran = (Matapelajaran) checkbox.getAttribute("matapelajaran");
				array.put(matapelajaran.getId());
			}
		}

		kelasSiswa.setMpYgTidakDiambil(array.toString());

		Common.refreshSaveOrUpdate(session, kelasSiswa);
		session.flush();

		if (copyDari != null && copyDari.getId() != null) {
			List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", copyDari))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

			for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
				KelasSiswaPunyaSiswa kelasSiswaPunyaSiswaLama = (KelasSiswaPunyaSiswa) session
						.createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.eq("kelasSiswa", kelasSiswa))
						.add(Restrictions.eq("siswa", kelasSiswaPunyaSiswa.getSiswa())).setMaxResults(1).uniqueResult();
				if (kelasSiswaPunyaSiswaLama == null) {
					kelasSiswaPunyaSiswaLama = new KelasSiswaPunyaSiswa();
					kelasSiswaPunyaSiswaLama.setSiswa(kelasSiswaPunyaSiswa.getSiswa());
					kelasSiswaPunyaSiswaLama.setKelasSiswa(kelasSiswa);
					session.save(kelasSiswaPunyaSiswaLama);
					session.flush();
				}
			}

		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> idsiswas = new ArrayList<Long>();
		if (!searchsiswa.getValue().trim().isEmpty()) {

			idsiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.groupProperty("kelasSiswa.id"))
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),

									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("calonSiswa.nomorInduk", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE))),

							Restrictions.or(
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("siswa.nomorIndukSantri", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.ilike("siswa.nomorInduk",
															searchsiswa.getValue().trim(), MatchMode.ANYWHERE))))))

					.list();

		}

		System.out.println("idsiswas -> " + idsiswas);

		Criteria criteria = session.createCriteria(KelasSiswa.class)

				.add(!searchsiswa.getValue().trim().isEmpty() && idsiswas.isEmpty()
						? Restrictions.sqlRestriction("false")
						: idsiswas.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", idsiswas))

				.add((searchguruPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguruPembina.getAttribute("guru") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("guruPembina", searchguruPembina.getAttribute("guru"))))

				.add((searchguruBk == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguruBk.getAttribute("guru") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("guruBk", searchguruBk.getAttribute("guru"))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchtingkat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchtingkat.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tingkat", searchtingkat.getValue())))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (!searchruang.getValue().trim().isEmpty()) {
			criteria.createAlias("ruang", "ruang").add(Restrictions.or(
					Restrictions.ilike("ruang.nama", searchruang.getValue().trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("ruang.kodeRuangan", searchruang.getValue().trim(), MatchMode.ANYWHERE)));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelasSiswa> kelasSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelasSiswa);
		grid.setRowRenderer(new KelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
