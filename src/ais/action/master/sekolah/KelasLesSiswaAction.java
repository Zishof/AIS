package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.DetailKelasLesSiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Sertifikat;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kelas les siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchsiswa}, {@code Textbox
 * searchruang}, {@code MyIntbox searchtingkat}, {@code Checkbox searchaktif}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}, {@code
 * initPengaturanBiaya()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onMasa()}, {@code onSertifikat()}, {@code onAdd()}). Bagian lain dari kontrak
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
public class KelasLesSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchsiswa;
	private Textbox searchruang;
	private MyIntbox searchtingkat;
	private Checkbox searchaktif;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataGuruBanbox searchguruPembina;

	private Textbox nama;
	private AmbilDataRuangBanbox ruang;
	private Combobox sekolah;
	private Intbox tingkat;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelasLesSiswa kelasLesSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox matapelajaran;
	private AmbilDataGuruBanbox guruPembina;
	private Textbox namaEn;
	private Textbox namaAr;
	private Textbox namaCh;

	private Tabpanel masa;

	public void onMasa(Event event) {
		if (masa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(masa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/masa_jadwal_pelajaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel sertifikat;

	public void onSertifikat(Event event) {
		if (sertifikat.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sertifikat);
			MyInclude iframe = new MyInclude("/pages/master/sertifikat.zul");
			iframe.setParent(window);
		}
	}

	private MyCheckboxConfig absensiharusGuruPembina;

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

		String[] contents = new String[] { "id", "nama", "ruang", "sekolah", "yayasan", "tingkat", "kapasitas", "aktif",
				"keterangan", "matapelajaran", "jenisBiayaSekolah", "masaJadwalPelajaran", "guruPembina",
				"absensiharusGuruPembina" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelasLesSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Kelas Les Siswa");

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

				final List<KelasLesSiswa> kelases = HibernateUtil.currentSession().createCriteria(KelasLesSiswa.class)
						.add(searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
						.add(searchyayasan.getSelectedItem() == null
								|| searchyayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))
						.list();
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						for (KelasLesSiswa kelasLesSiswa : kelases) {
							try {
								Session session = HibernateUtil.currentNativeSession();
								Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.add(Restrictions.isNotNull("siswa"))
										.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa));

								List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = ConstantValues
										.simpleList(criteria, KelasLesSiswaPunyaSiswa.class);

								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();

								int size = kelasLesSiswaPunyaSiswas.size();
								int rowIndex = 1;
								for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
									double persen = rowIndex * 100.0 / size;
									label.setValue("Sedang memproses data " + kelasLesSiswaPunyaSiswa.getSiswa()
											+ " di kelas " + kelasLesSiswa.getNama() + " ("
											+ Common.numberFormat.get().format(persen) + " %)");
									Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();

									Siswa.populate(siswa);

									CalonSiswa calonSiswa = kelasLesSiswaPunyaSiswa.getCalonSiswa();
									if (calonSiswa != null) {
										CalonSiswa.populate(calonSiswa);
									}

									String kunci = kelasLesSiswa.getNama() + " - " + siswa.toString();
									session = HibernateUtil.currentNativeSession();
									try {
										siswa.setKelasLes(kelasLesSiswa);
										session.getTransaction().begin();
										Common.refreshUpdate(session, siswa);
										Common.refreshUpdate(session, kelasLesSiswaPunyaSiswa);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
										laporan.catatBerhasil(rowIndex - 1, kunci, "Sinkronisasi berhasil");
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
										laporan.catatGagalDetail(rowIndex - 1, kunci, e);
									}

									HibernateUtil.closeSession();

									rowIndex++;
								}
								kelasLesSiswaPunyaSiswas.clear();
								kelasLesSiswaPunyaSiswas = null;

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								laporan.tambahCatatan("Kelas Les " + kelasLesSiswa.getNama() + " gagal diproses: "
										+ ais.common.LaporanUpload.detailTeknisException(e));
							}

						}

											} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}

		});
		Common.appendKeToolbar(button, add, comp);

//		MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Singkron dg pemb.", "/img/svg/check2.svg");
//		singkronDenganMhs.setVisible(Common
//				.getKonfigurasi("tampilkan_tombol_singkronkan_siswa_dengan_pembayaran_di_kelas_leas", Konfigurasi.AKTIF)
//				.getNilai().equals(Konfigurasi.AKTIF));
//		singkronDenganMhs.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//
//				final Label label = Common.displayLoadBar(new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						onSearchDefault(null);
//					}
//				});
//
//				new Thread(new Runnable() {
//
//					@SuppressWarnings("unchecked")
//					@Override
//					public void run() {
//
//						List<Long> longs = initCriteria(true).setProjection(Projections.property("id")).list();
//						int size = longs.size();
//						int index = 0;
//						for (Long id : longs) {
//							index++;
//
//							Session session = HibernateUtil.currentNativeSession();
//							List<Siswa> siswas = ConstantValues
//									.simpleList(session.createCriteria(KelasLesSiswaPunyaSiswa.class)
//											.setProjection(Projections.property("siswa.id"))
//											.add(Restrictions.eq("kelasLesSiswa.id", id)), Siswa.class, false);
//
//							label.setValue("Singkronkan pembayaran " + siswas.size() + " siswa ("
//									+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
//
//							for (Siswa siswa : siswas) {
//								if (siswa != null && siswa.getId() != null) {
//									Siswa.populate(siswa);
//								}
//							}
//						}
//						label.setValue("");
//					}
//				}).start();
//
//			}
//		});
//		add.getParent().appendChild(singkronDenganMhs);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class KelasLesSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelasLesSiswaHelper detailKelasLesSiswaHelper = new DetailKelasLesSiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						detailKelasLesSiswaHelper.displayDetailPA(kelasLesSiswa, detail, addWindow);
					}

				}

			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KelasLesSiswa.class, kelasLesSiswa, kelasLesSiswa.getNama()))
					.setParent(arg0);

			if (!kelasLesSiswa.getNama().equalsIgnoreCase(kelasLesSiswa.getNamaEn())) {
				new Label(kelasLesSiswa.getNamaEn()).setParent(a);
			}
			if (!kelasLesSiswa.getNama().equalsIgnoreCase(kelasLesSiswa.getNamaAr())) {
				new Label(kelasLesSiswa.getNamaAr()).setParent(a);
			}
			if (!kelasLesSiswa.getNama().equalsIgnoreCase(kelasLesSiswa.getNamaCh())) {
				new Label(kelasLesSiswa.getNamaCh()).setParent(a);
			}

			new Label(kelasLesSiswa.getRuang() == null ? ""
					: kelasLesSiswa.getRuang().getKodeRuangan() + "-" + kelasLesSiswa.getRuang().getNama())
					.setParent(arg0);
			new Label(kelasLesSiswa.getSertifikat() == null ? "" : kelasLesSiswa.getSertifikat().getNama())
					.setParent(arg0);
			new Label(kelasLesSiswa.getTingkat().toString()).setParent(arg0);

			new Label(kelasLesSiswa.getMatapelajaran() == null ? "" : kelasLesSiswa.getMatapelajaran().getNama())
					.setParent(arg0);

			new Label(kelasLesSiswa.getMasaJadwalPelajaran() == null ? ""
					: kelasLesSiswa.getMasaJadwalPelajaran().getNama()).setParent(arg0);

			new Label(
					kelasLesSiswa.getJenisBiayaSekolah() == null ? "" : kelasLesSiswa.getJenisBiayaSekolah().getNama())
					.setParent(arg0);

			try {
				Guru guru = kelasLesSiswa.getGuruPembina();
				new Label(guru == null ? "" : guru.getNamaGuru()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}

			int count = ((Number) HibernateUtil.currentSession().createCriteria(KelasLesSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(kelasLesSiswa.getKeterangan()).setParent(vbox);
			List<PengaturanBiaya> syarats = kelasLesSiswa.ambilPengaturanBiaya();
			if (!syarats.isEmpty()) {
				new MyLabelAgakKecilBoldBiru("Syarat Pembayaran").setParent(vbox);
				for (PengaturanBiaya biaya : syarats) {
					RevisiHelper.createNewRevisi(PengaturanBiaya.class, biaya, biaya.toString()).setParent(vbox);
				}
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelasLesSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasLesSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelasLesSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kelasLesSiswa, KelasLesSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelasLesSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelasLesSiswa = (KelasLesSiswa) obj;
		init(kelasLesSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private MyCheckboxConfig publikasiNilaiHarusTelahDiverifikasi;
	private MyCheckboxConfig guruBolehMemverifikasiSendiri;
	private Combobox masaJadwalPelajaran;
	protected HashSet<Long> selectedPengaturanBiaya;
	private Combobox sertifikatCombo;
	private Combobox jenisBiayaSekolah;

	@SuppressWarnings("deprecation")
	private void init(final KelasLesSiswa kelasLesSiswa) throws Exception {
		this.kelasLesSiswa = kelasLesSiswa;
		addWindow.setTitle(kelasLesSiswa.getId() == null ? "Tambah Kelas Les Siswa" : "Ubah Kelas Les Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Indonesia) *"));
		row.appendChild(nama = new Textbox(kelasLesSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (English)"));
		row.appendChild(namaEn = new Textbox(kelasLesSiswa.getNamaEn()));
		namaEn.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Aksara Arab)"));
		row.appendChild(namaAr = new Textbox(kelasLesSiswa.getNamaAr()));
		namaAr.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas (Aksara Tionghoa)"));
		row.appendChild(namaCh = new Textbox(kelasLesSiswa.getNamaCh()));
		namaCh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruangan"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setAttribute("ruang", kelasLesSiswa.getRuang());
		ruang.setValue(kelasLesSiswa.getRuang() == null ? ""
				: kelasLesSiswa.getRuang().getKodeRuangan() + "-" + kelasLesSiswa.getRuang().getNama());
		ruang.setWidth("90%");
		ruang.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kelasLesSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, kelasLesSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Pembelajaran *"));
		row.appendChild(masaJadwalPelajaran = new Combobox());
		masaJadwalPelajaran.setWidth("90%");
		masaJadwalPelajaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran *"));
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");
		matapelajaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat"));
		row.appendChild(tingkat = new Intbox(kelasLesSiswa.getTingkat()));

		row = new MyFormRow();
		row.setParent(rows);

		guruPembina = new AmbilDataGuruBanbox();
		if (searchguruPembina.getAttribute("guru") != null) {
			kelasLesSiswa.setGuruPembina((Guru) searchguruPembina.getAttribute("guru"));
			guruPembina.setDisabled(searchguruPembina.isDisabled());
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Wali Kelas"));

		row.appendChild(guruPembina);
		guruPembina.setAttribute("guru", kelasLesSiswa.getGuruPembina());
		guruPembina.setAttribute("myValue", kelasLesSiswa.getGuruPembina());
		try {
			guruPembina.setValue(
					kelasLesSiswa.getGuruPembina() == null ? "" : kelasLesSiswa.getGuruPembina().getNamaGuru());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/KelasLesSiswaAction.java:592");
			// TODO: handle exception
		}
		guruPembina.setWidth("90%");
		guruPembina.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Syarat pembayaran"));
		row.appendChild(jenisBiayaSekolah = new Combobox());
		jenisBiayaSekolah.setWidth("90%");
		jenisBiayaSekolah.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "nama", "sekolah" }, "periode",
						JenisBiayaSekolah.class, "== Tidak Ada ==",
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				Common.selectComboItem(true, jenisBiayaSekolah, kelasLesSiswa.getJenisBiayaSekolah());

			}
		};

		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		absensiharusGuruPembina = new MyCheckboxConfig("Hanya boleh di-absen oleh Wali Kelas");
		row.appendChild(absensiharusGuruPembina);
		absensiharusGuruPembina.setChecked(kelasLesSiswa.getAbsensiharusGuruPembina());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		publikasiNilaiHarusTelahDiverifikasi = new MyCheckboxConfig("Publikasi Nilai Harus Telah Diverifikasi");
		row.appendChild(publikasiNilaiHarusTelahDiverifikasi);
		publikasiNilaiHarusTelahDiverifikasi.setChecked(kelasLesSiswa.getPublikasiNilaiHarusTelahDiverifikasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		guruBolehMemverifikasiSendiri = new MyCheckboxConfig("Guru Boleh Mem-verifikasi Sendiri Nilai");
		row.appendChild(guruBolehMemverifikasiSendiri);
		guruBolehMemverifikasiSendiri.setChecked(kelasLesSiswa.getGuruBolehMemverifikasiSendiri());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikatCombo = new Combobox());
		Common.insertComboDanSemua(sertifikatCombo, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikatCombo, kelasLesSiswa.getSertifikat());
		sertifikatCombo.setWidth("90%");
		sertifikatCombo.setReadonly(true);

		Common.initKeterangan(rows, "Jika form kegiatan ini tanpa sertifikat, pilih tanpa sertifikat.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelasLesSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		final MyLabelStyled myRowStyledKelas = new MyLabelStyled("Syarat Pembayaran Ikut Kelas Les");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(myRowStyledKelas);

		final MyFormRow rowDataG = new MyFormRow();
		rowDataG.setVisible(false);
		rowDataG.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowDataG, "2");

		EventListener jenisBiayaSekolahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisBiayaSekolah jb = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
						: jenisBiayaSekolah.getSelectedItem().getValue());
				myRowStyledKelas.getParent().setVisible(jb == null);
				rowDataG.setVisible(jb == null);
			}
		};

		jenisBiayaSekolah.addEventListener("onChange", jenisBiayaSekolahEventListener);
		jenisBiayaSekolahEventListener.onEvent(null);

		final EventListener eventListenerKelasLesSiswa = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				rowDataG.setVisible(false);

				selectedPengaturanBiaya = new HashSet<Long>();
				KelasLesSiswaAction.initPengaturanBiaya(rowDataG, selectedPengaturanBiaya, s, kelasLesSiswa);

				myRowStyledKelas.getParent().setVisible(rowDataG.isVisible());

			}
		};

		EventListener kelasListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				org.hibernate.criterion.Criterion sekolahCrit = s == null
						? org.hibernate.criterion.Restrictions.conjunction()
						: org.hibernate.criterion.Restrictions.or(
								Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s));

				Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian", "sekolah" },
						Matapelajaran.class,
						Restrictions.and(sekolahCrit,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(true, matapelajaran, kelasLesSiswa.getMatapelajaran());
				matapelajaran.setReadonly(true);

				Common.insertCombo(masaJadwalPelajaran, new String[] { "nama" }, MasaJadwalPelajaran.class,
						Restrictions.and(sekolahCrit,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(true, masaJadwalPelajaran, kelasLesSiswa.getMasaJadwalPelajaran());

				eventListenerKelasLesSiswa.onEvent(arg0);
			}
		};

		sekolah.addEventListener("onChange", kelasListener);
		Common.createDefaultTimer(kelasListener);

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
		if (matapelajaran.getSelectedItem() == null || matapelajaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Matapelajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (masaJadwalPelajaran.getSelectedItem() == null || masaJadwalPelajaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Masa atau rentan waktu pembelajaran harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		String jenisS = KelasLesSiswa.populateJenisPengaturanPembayaran(selectedPengaturanBiaya);

		KelasLesSiswa copyDari = (KelasLesSiswa) kelasLesSiswa.getCopyDari();

		Session session = HibernateUtil.currentSession();
		if (kelasLesSiswa.getId() != null) {
			kelasLesSiswa = (KelasLesSiswa) session.load(KelasLesSiswa.class, kelasLesSiswa.getId());

		}
		kelasLesSiswa.setMasaJadwalPelajaran((MasaJadwalPelajaran) masaJadwalPelajaran.getSelectedItem().getValue());
		kelasLesSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		kelasLesSiswa.setNama(nama.getValue());

		kelasLesSiswa.setNamaAr(namaAr.getValue());
		kelasLesSiswa.setNamaCh(namaCh.getValue());
		kelasLesSiswa.setNamaEn(namaEn.getValue());

		kelasLesSiswa.setRuang((Ruang) ruang.getAttribute("ruang"));
		kelasLesSiswa.setTingkat(tingkat.getValue());
		kelasLesSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		kelasLesSiswa.setKeterangan(keterangan.getValue());
		kelasLesSiswa.setMatapelajaran((Matapelajaran) matapelajaran.getSelectedItem().getValue());
		kelasLesSiswa.setGuruPembina((Guru) guruPembina.getAttribute("guru"));

		kelasLesSiswa.setAbsensiharusGuruPembina(absensiharusGuruPembina.isChecked());

		kelasLesSiswa.setGuruBolehMemverifikasiSendiri(guruBolehMemverifikasiSendiri.isChecked());
		kelasLesSiswa.setPublikasiNilaiHarusTelahDiverifikasi(publikasiNilaiHarusTelahDiverifikasi.isChecked());

		kelasLesSiswa.setSertifikat((Sertifikat) (sertifikatCombo.getSelectedItem() == null ? null
				: sertifikatCombo.getSelectedItem().getValue()));

		kelasLesSiswa.setSyaratPengaturanPembayaran(jenisS);
		kelasLesSiswa.setJenisBiayaSekolah((JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
				: jenisBiayaSekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kelasLesSiswa);
		session.flush();

		if (copyDari != null && copyDari.getId() != null) {
			List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = session
					.createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("kelasLesSiswa", copyDari))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

			for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
				KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswaLama = (KelasLesSiswaPunyaSiswa) session
						.createCriteria(KelasLesSiswaPunyaSiswa.class)
						.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
						.add(Restrictions.eq("siswa", kelasLesSiswaPunyaSiswa.getSiswa())).setMaxResults(1)
						.uniqueResult();
				if (kelasLesSiswaPunyaSiswaLama == null) {
					kelasLesSiswaPunyaSiswaLama = new KelasLesSiswaPunyaSiswa();
					kelasLesSiswaPunyaSiswaLama.setSiswa(kelasLesSiswaPunyaSiswa.getSiswa());
					kelasLesSiswaPunyaSiswaLama.setKelasLesSiswa(kelasLesSiswa);
					session.save(kelasLesSiswaPunyaSiswaLama);
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

			idsiswas = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
					.setProjection(Projections.groupProperty("kelasLesSiswa.id"))
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

		Criteria criteria = session.createCriteria(KelasLesSiswa.class)

				.add(!searchsiswa.getValue().trim().isEmpty() && idsiswas.isEmpty()
						? Restrictions.sqlRestriction("false")
						: idsiswas.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", idsiswas))

				.add((searchguruPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguruPembina.getAttribute("guru") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("guruPembina", searchguruPembina.getAttribute("guru"))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchtingkat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchtingkat.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tingkat", searchtingkat.getValue())))

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

		List<KelasLesSiswa> kelasLesSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelasLesSiswa);
		grid.setRowRenderer(new KelasLesSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("unchecked")
	public static void initPengaturanBiaya(Row row, final Set<Long> selectedPengaturanBiaya, final Sekolah sekolah,
			final KelasLesSiswa kelasLesSiswa) {
		Common.clear(row);
		Session session = HibernateUtil.currentSession();
		List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(session.createCriteria(PengaturanBiaya.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("sekolah", sekolah))

				.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
				.createAlias("jenisBiayaSekolah", "jenisBiayaSekolah").addOrder(Order.asc("jenisBiayaSekolah.nama"))
				.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode")), PengaturanBiaya.class);

//		if(kelasLesSiswa != null && kelasLesSiswa.getJenisBiayaSekolah() != u)

		System.out.println("pengaturanBiayas -> " + pengaturanBiayas + ", sekolah " + sekolah);

		if (pengaturanBiayas != null && !pengaturanBiayas.isEmpty()) {
			row.setVisible(true);
			final MyGrid subGridPengaturanBiaya = new MyGrid();
			row.appendChild(subGridPengaturanBiaya);

			Columns subColumns = new Columns();
			subColumns.setParent(subGridPengaturanBiaya);
			Column c = new Column("Pilih Syarat Pembayaran");
			subColumns.appendChild(c);

			Rows subRows = new Rows();
			subRows.setParent(subGridPengaturanBiaya);

			MyFormRow subRow = new MyFormRow();
			subRow.setStyle("border:0px;background: transparent;");
			subRow.setParent(subRows);
			subRow.setValign("top");

			selectedPengaturanBiaya.addAll(kelasLesSiswa.ambilPengaturanBiayaId());

			Set<Long> ids = new HashSet<Long>();
			for (Long v : selectedPengaturanBiaya) {
				ids.add(v);
			}

			System.out.println("ids ->" + ids);

			Vbox vboxSkala = new Vbox();
			vboxSkala.setPack("top");
			vboxSkala.setParent(subRow);
			for (final PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {

				final Checkbox checkbox = new Checkbox(pengaturanBiaya.toString());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(ids.contains(pengaturanBiaya.getId()));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedPengaturanBiaya.add(pengaturanBiaya.getId());
						} else {
							for (Long a : selectedPengaturanBiaya) {
								if (a.equals(pengaturanBiaya.getId())) {
									selectedPengaturanBiaya.remove(a);
									break;
								}
							}
						}

						String jenisS = KelasLesSiswa.populateJenisPengaturanPembayaran(selectedPengaturanBiaya);
						kelasLesSiswa.setSyaratPengaturanPembayaran(jenisS);

						System.out.println(
								"selectedPengaturanBiaya => " + selectedPengaturanBiaya + ", jenisS " + jenisS);
					}
				});

			}

		} else {
			row.setVisible(false);
		}
	}

}
