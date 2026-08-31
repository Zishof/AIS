package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.JenisRaporSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
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
 * Controller/action ZK untuk jenis rapor siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Textbox nama}, {@code Combobox sekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code tampilkanButton()}, {@code reloadDataGambar()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class JenisRaporSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisRaporSiswa jenisRaporSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	protected LampiranLain lampiran;
	private HashMap<Long, LampiranLain> maps;
	private Rows rowsGridGaleri;

	private List<Checkbox> checkboxs = new ArrayList<Checkbox>();

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

		String[] contents = new String[] { "id", "nama", "sekolah", "keterangan", "untukSemuaSemester",
				"tingkatKebelakang", "tampilKeSiswa", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisRaporSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class JenisRaporSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisRaporSiswa jenisRaporSiswa = (JenisRaporSiswa) arg1;

			RevisiHelper.createNewRevisi(JenisRaporSiswa.class, jenisRaporSiswa, jenisRaporSiswa.getNama())
					.setParent(arg0);
			new Label(jenisRaporSiswa.getSekolah() == null ? "" : jenisRaporSiswa.getSekolah().getNama())
					.setParent(arg0);
			new Label(jenisRaporSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig tampilKeSiswa = new MyCheckboxConfig("Tampil ke siswa");
			tampilKeSiswa.setDisabled(!edit);
			tampilKeSiswa.setChecked(jenisRaporSiswa.getTampilKeSiswa());
			tampilKeSiswa.setParent(arg0);
			tampilKeSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setTampilKeSiswa(tampilKeSiswa.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig untukSemuaSemester = new MyCheckboxConfig("Semua Smt");
			untukSemuaSemester.setDisabled(!edit);
			untukSemuaSemester.setChecked(jenisRaporSiswa.getUntukSemuaSemester());
			untukSemuaSemester.setParent(arg0);
			untukSemuaSemester.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setUntukSemuaSemester(untukSemuaSemester.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyIntbox tingkatKebelakang = new MyIntbox(jenisRaporSiswa.getTingkatKebelakang());
			tingkatKebelakang.setDisabled(!edit);
			tingkatKebelakang.setParent(arg0);
			tingkatKebelakang.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setTingkatKebelakang(tingkatKebelakang.getValue());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig hitungRataRataKelas = new MyCheckboxConfig("Rata-Rata Kelas");
			hitungRataRataKelas.setDisabled(!edit);
			hitungRataRataKelas.setChecked(jenisRaporSiswa.getHitungRataRataKelas());
			hitungRataRataKelas.setParent(arg0);
			hitungRataRataKelas.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setHitungRataRataKelas(hitungRataRataKelas.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig ambilAbsenPiket = new MyCheckboxConfig("Absen");
			ambilAbsenPiket.setDisabled(!edit);
			ambilAbsenPiket.setChecked(jenisRaporSiswa.getAmbilAbsenPiket());
			ambilAbsenPiket.setParent(arg0);
			ambilAbsenPiket.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAmbilAbsenPiket(ambilAbsenPiket.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig ambilKegiatanSiswa = new MyCheckboxConfig("Kegiatan/Prestasi");
			ambilKegiatanSiswa.setDisabled(!edit);
			ambilKegiatanSiswa.setChecked(jenisRaporSiswa.getAmbilKegiatanSiswa());
			ambilKegiatanSiswa.setParent(arg0);
			ambilKegiatanSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAmbilKegiatanSiswa(ambilKegiatanSiswa.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig ambilPelanggaranSiswa = new MyCheckboxConfig("Pelanggaran");
			ambilPelanggaranSiswa.setDisabled(!edit);
			ambilPelanggaranSiswa.setChecked(jenisRaporSiswa.getAmbilPelanggaranSiswa());
			ambilPelanggaranSiswa.setParent(arg0);
			ambilPelanggaranSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAmbilPelanggaranSiswa(ambilPelanggaranSiswa.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig ambilApresiasiSiswa = new MyCheckboxConfig("Apresiasi");
			ambilApresiasiSiswa.setDisabled(!edit);
			ambilApresiasiSiswa.setChecked(jenisRaporSiswa.getAmbilApresiasiSiswa());
			ambilApresiasiSiswa.setParent(arg0);
			ambilApresiasiSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAmbilApresiasiSiswa(ambilApresiasiSiswa.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});
			
			
			final MyCheckboxConfig ambilCatatanSiswa = new MyCheckboxConfig("Catatan");
			ambilCatatanSiswa.setDisabled(!edit);
			ambilCatatanSiswa.setChecked(jenisRaporSiswa.getAmbilCatatanSiswa());
			ambilCatatanSiswa.setParent(arg0);
			ambilCatatanSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAmbilCatatanSiswa(ambilCatatanSiswa.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisRaporSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisRaporSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisRaporSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisRaporSiswa, JenisRaporSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisRaporSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisRaporSiswa = (JenisRaporSiswa) obj;
		init(jenisRaporSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init(final JenisRaporSiswa jenisRaporSiswa) {
		this.jenisRaporSiswa = jenisRaporSiswa;
		addWindow.setTitle(jenisRaporSiswa.getId() == null ? "Tambah Jenis Rapor Siswa" : "Ubah Jenis Rapor Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Rapor Siswa *"));
		row.appendChild(nama = new Textbox(jenisRaporSiswa.getNama()));
		nama.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jenisRaporSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jenisRaporSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisRaporSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Matapelajaran yang tidak dimasukkan ke raport ini"));

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowdata = (Rows) Common.tampilanScroll1(row).getParent();

		EventListener sKurikulum = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowdata);
				checkboxs.clear();

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				if (s != null) {
					Session session = HibernateUtil.currentSession();
					List<Matapelajaran> matapelajarans = ConstantValues
							.simpleList(
									session.createCriteria(Matapelajaran.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.setProjection(Projections.property("id"))
											.add(Restrictions.eq("sekolah", s)).addOrder(Order.asc("urutan")),
									Matapelajaran.class, false);

					System.out.println("matapelajarans -> " + matapelajarans.size());

					List<Long> longs = jenisRaporSiswa.ambilMk();

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

		sekolah.addEventListener("onChange", sKurikulum);

		Common.createDefaultTimer(sKurikulum);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisRaporSiswa.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR, "File Laporan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gambar / Background Pendukung"));

		Hbox myHbox = new Hbox();
		myHbox.setParent(row);
		myHbox.setHeight("30px");

		Hbox hboxGambar = new Hbox();
		hboxGambar.setParent(myHbox);
		tampilkanButton(hboxGambar);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyGrid myGridGaleri = new MyGrid();
		myGridGaleri.setParent(row);

		rowsGridGaleri = new Rows();
		myGridGaleri.appendChild(rowsGridGaleri);

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

		try {

			if (jenisRaporSiswa.getId() != null) {
				try {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", jenisRaporSiswa.getId()))
							.add(Restrictions.ilike("jenis", "Jenis_Rapor_Siswa_", MatchMode.START)).list();
					for (LampiranLain lampiran : lampiranLains) {
						maps.put(lampiran.getId(), lampiran);
					}

					StreamingHibernateUtil.getInstance().closeSession();

				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/JenisRaporSiswaAction.java:515");
				}
			}

			reloadDataGambar();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	private void tampilkanButton(final Hbox hboxGambar) {
		maps = new HashMap<Long, LampiranLain>();
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, jenisRaporSiswa.getId(),
				"Jenis_Rapor_Siswa_" + Common.getGeneratedBarCode(), "Gambar Pendukung", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar();
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);

							}
						});
					}
				});
	}

	private void reloadDataGambar() throws Exception {
		Common.clear(rowsGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);
			MyFormRow roww = new MyFormRow();
			roww.setParent(rowsGridGaleri);

			Vbox vbox = new Vbox();
			vbox.setParent(roww);

			Image image = new Image(link);
			image.setStyle("max-width: 256px !important;min-width: 60px !important;min-height: 300px !important;");
			image.setSclass("gambar_profile");
			image.setWidth("90%");
			image.setParent(vbox);

			A a = new A(link);
			a.setParent(vbox);
			a.setTarget("_blank");
			a.setHref(link);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(3);
			textbox.setParent(vbox);

			textbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setDeskripsi(textbox.getValue());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}
			});

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
										try {

											LampiranLain d = maps.remove(lampiranLain.getId());
											System.out.println("d = > " + d);

											try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reloadDataGambar();
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
			button.setParent(vbox);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Jenis Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
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

		Session session = HibernateUtil.currentSession();
		if (jenisRaporSiswa.getId() != null) {
			jenisRaporSiswa = (JenisRaporSiswa) session.load(JenisRaporSiswa.class, jenisRaporSiswa.getId());

		}

		jenisRaporSiswa.setNama(nama.getValue());
		jenisRaporSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		jenisRaporSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		jenisRaporSiswa.setKeterangan(keterangan.getValue());

		JSONArray array = new JSONArray();
		for (Checkbox checkbox : checkboxs) {
			if (checkbox.isChecked()) {
				Matapelajaran matapelajaran = (Matapelajaran) checkbox.getAttribute("matapelajaran");
				array.put(matapelajaran.getId());
			}
		}

		jenisRaporSiswa.setMpYgTidakDiambil(array.toString());

		Common.refreshSaveOrUpdate(session, jenisRaporSiswa);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisRaporSiswa.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		if (maps != null && !maps.isEmpty()) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				for (LampiranLain lampiran : maps.values()) {
					if (lampiran != null && lampiran.getId() != null) {
						session.refresh(lampiran);
						lampiran.setRef(jenisRaporSiswa.getId());

						session.getTransaction().begin();
						session.update(lampiran);
						session.getTransaction().commit();
					}
				}

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisRaporSiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisRaporSiswa> jenisRaporSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisRaporSiswa);
		grid.setRowRenderer(new JenisRaporSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
