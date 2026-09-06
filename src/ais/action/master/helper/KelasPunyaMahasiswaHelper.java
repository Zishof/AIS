package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk layar "Mahasiswa pada Kelas" — menampilkan dan mengelola daftar
 * {@link Mahasiswa} yang tergabung dalam satu {@link Kelas} (kelas paralel akademik, bukan kelas
 * sekolah). Kelas ini mengimplementasikan {@link DataLoader} (pemuatan ulang grid) dan
 * {@link DataCriteria} (kriteria pencarian dipakai ulang untuk cetak laporan lewat
 * {@code Common#cetakData}).
 *
 * <p>
 * Fitur utama yang disediakan lewat toolbar pada {@link #display(Kelas, Component, MyWindow)}:
 * </p>
 * <ul>
 * <li>Pencarian mahasiswa anggota kelas berdasarkan NIM/nama, angkatan, Fakultas, dan Jurusan.</li>
 * <li>"Ambil Mahasiswa" — membuka {@link AmbilDataMahasiswaForKelasHelper} untuk menambah anggota
 * kelas dari daftar mahasiswa yang memenuhi filter Fakultas/Jurusan/angkatan kelas.</li>
 * <li>"Bersihkan" — menghapus keanggotaan seluruh mahasiswa dari kelas ini sekaligus (update massal
 * kolom {@code kelas} pada tabel {@code mahasiswa} lewat SQL native).</li>
 * <li>"Singkronkan" — untuk rentang semester yang dipilih, menyamakan kolom {@code kelas} pada
 * {@link KrsMahasiswa} (KRS reguler maupun Semester Pendek) tiap anggota dengan nama kelas ini,
 * dijalankan pada thread terpisah dengan progres ditampilkan lewat {@code Common#displayLoadBar}.</li>
 * <li>Hapus satu anggota (tombol per baris di {@link DetailKelasRenderer}), yang turut membersihkan
 * kolom {@code kelas} pada {@link KrsMahasiswa} terkait.</li>
 * <li>Upload massal keanggotaan kelas dari berkas Excel (.xlsx) lewat
 * {@link #uploadDataMahasiswa(File, EventListener)}.</li>
 * <li>Cetak laporan anggota kelas lewat {@code Common#cetakData}.</li>
 * </ul>
 */
public class KelasPunyaMahasiswaHelper implements DataLoader, DataCriteria {

	/** Grid berpaging yang menampilkan anggota {@link #kelas}, dirender oleh {@link DetailKelasRenderer}. */
	private MyGrid grid;
	/** Kelas paralel akademik yang anggotanya sedang ditampilkan/dikelola. */
	private Kelas kelas;
	/** Kotak pencarian toolbar: kata kunci NIM/nama mahasiswa yang menyaring {@link #initCriteria(boolean)}. */
	private Textbox nama;
	/** Kotak filter toolbar: tahun angkatan mahasiswa. */
	private Intbox angkatan;

	/** Combobox filter Fakultas; dikunci ({@code setDisabled(true)}) bila {@link #kelas} sudah terikat ke fakultas/jurusan tertentu. */
	private Combobox searchfakultas = new Combobox();
	/** Combobox filter Program Studi ({@link Jurusan}); dikunci ({@code setDisabled(true)}) bila {@link #kelas} sudah terikat ke jurusan tertentu. */
	private Combobox searchjurusan = new Combobox();

	/** Komponen paging grid; dimuat ulang lewat {@link #loadData} setiap kali halaman aktif berubah. */
	private Paging paging;

	/** Menyiapkan opsi combo Fakultas/Jurusan (via {@link Common#initFakultasDanJurusanDanSemua}) dan komponen {@link #paging} beserta listener reload halamannya. */
	public KelasPunyaMahasiswaHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelasPunyaMahasiswaHelper}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelasPunyaMahasiswaHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean delete}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelasPunyaMahasiswaHelper
	 */
	class DetailKelasRenderer extends ais.ui.util.MyRowRenderer {

		/** {@code true} bila user yang login memiliki hak {@link CommonPrivilages#DELETE}; mengontrol visibilitas tombol hapus per baris. */
		private boolean delete = false;

		/** Menentukan {@link #delete} sekali di awal (dibaca ulang setiap renderer dibuat) lewat {@link CommonPrivilages#checkPrevilages}. */
		public DetailKelasRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(row);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(row);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getFakultas().getNama() + "")
					.setParent(row);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama() + "").setParent(row);

			new Label(mahasiswa.getProgram() + "").setParent(row);

			new Label(mahasiswa.getOleh()).setParent(row);

			if (mahasiswa.getKelas() != null && !mahasiswa.getKelas().isEmpty()) {
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
				if (krsMahasiswa.getKelas() == null || !krsMahasiswa.getKelas().equals(mahasiswa.getKelas())) {
					krsMahasiswa.setKelas(mahasiswa.getKelas());
					Common.refreshSaveOrUpdate(krsMahasiswa);

				}
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Keluarkan dari kelas");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Keluarkan mahasiswa dari kelas ini? Data mahasiswa dan riwayat akademik tetap disimpan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											mahasiswa.setKelas(null);
											Common.refreshSaveOrUpdate(mahasiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													KrsMahasiswa krsMahasiswa = Common
															.singkronkanKrsMahasiswa(mahasiswa);
													krsMahasiswa.setKelas(null);
													Common.refreshSaveOrUpdate(krsMahasiswa);

													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Implementasi {@link DataCriteria}: membangun kriteria Hibernate untuk mahasiswa anggota
	 * {@link #kelas} (kolom {@code kelas} pada {@link Mahasiswa} disamakan persis dengan nama kelas),
	 * disaring lebih lanjut oleh filter toolbar (nama/NIM, angkatan, Fakultas, Jurusan). Dipakai
	 * ulang oleh {@link #loadData(Object)} untuk grid dan oleh {@code Common#cetakData} untuk cetak.
	 *
	 * @param order {@code true} untuk mengurutkan hasil berdasarkan NIM ascending
	 * @return kriteria Hibernate siap eksekusi/paginasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		criteria.createAlias("jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(kelas != null && !kelas.getNama().trim().isEmpty()
						? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nim"));

		return criteria;
	}

	/**
	 * Implementasi {@link DataLoader}: memuat ulang halaman aktif daftar mahasiswa anggota kelas ke
	 * {@link #grid} sesuai kriteria pencarian saat ini, dijalankan lewat
	 * {@code Common#createDefaultTimer} agar UI tidak diblokir. Parameter {@code value} tidak dipakai.
	 *
	 * @param value tidak digunakan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<Mahasiswa> myMahasiswas =

						ConstantValues
								.simpleList(
										initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
												.setFirstResult(Common.ROWS_COUNT_ON_PAGE
														* (paging == null ? 0 : paging.getActivePage())),
										Mahasiswa.class);
				ListModel strset = new SimpleListModel(myMahasiswas);
				grid.setRowRenderer(new DetailKelasRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	/** @return {@code this} sebagai {@link DataLoader}, diteruskan ke {@link AmbilDataMahasiswaForKelasHelper} agar dapat memicu {@link #loadData} setelah simpan. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun seluruh tampilan layar "Mahasiswa pada Kelas": toolbar filter/aksi (lihat dokumentasi
	 * kelas untuk daftar lengkap fitur toolbar) dan grid berpaginasi daftar anggota. Bila {@code kelas}
	 * sudah terikat ke Jurusan/Fakultas atau angkatan tertentu, kombo filter terkait langsung
	 * dipra-isi dan dikunci ({@code setDisabled(true)}) supaya pencarian tidak keluar dari lingkup
	 * kelas tersebut.
	 *
	 * @param kelas     kelas yang anggotanya akan ditampilkan/dikelola
	 * @param component komponen ZK induk tempat layar dirender (dibersihkan lebih dulu)
	 * @param window    jendela induk, diteruskan ke {@link AmbilDataMahasiswaForKelasHelper} saat
	 *                  fitur "Ambil Mahasiswa" dipakai
	 */
	public void display(final Kelas kelas, final Component component, final MyWindow window) {
		this.kelas = kelas;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti kelas " + kelas.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.setWidth("180px");
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.setWidth("90px");
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.setWidth("220px");
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.setWidth("260px");
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (kelas.getJurusan() != null) {
			Common.selectComboItem(true, searchfakultas, kelas.getJurusan().getFakultas());
			searchfakultas.setDisabled(true);
			Common.insertCombo(searchjurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", kelas.getJurusan().getFakultas()));
			Common.selectComboItem(true, searchjurusan, kelas.getJurusan());
			searchjurusan.setDisabled(true);
		} else if (kelas.getFakultas() != null) {
			Common.selectComboItem(true, searchfakultas, kelas.getFakultas());
			searchfakultas.setDisabled(true);
		}

		if (kelas.getTahunAngkatan() != null) {
			angkatan.setValue(kelas.getTahunAngkatan());
			angkatan.setDisabled(true);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Fakultas filterFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
						: searchfakultas.getSelectedItem().getValue());
				Jurusan filterJurusan = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
						: searchjurusan.getSelectedItem().getValue());
				if (filterJurusan == null && kelas != null) {
					filterJurusan = kelas.getJurusan();
				}
				if (filterFakultas == null && filterJurusan != null) {
					filterFakultas = filterJurusan.getFakultas();
				}
				if (filterFakultas == null && kelas != null) {
					filterFakultas = kelas.getFakultas();
				}
				AmbilDataMahasiswaForKelasHelper dataMahasiswaHelper = new AmbilDataMahasiswaForKelasHelper(kelas,
						filterFakultas, filterJurusan);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery("update mahasiswa set kelas = null where kelas = '"
												+ kelas.getNama() + "'").executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow();
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindow.setTitle("Simpan Dosen PA");
				addWindow.setWidth("500px");
				addWindow.setHeight("300px");
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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
				row.appendChild(new ais.ui.util.MyLabelConfig("Mulai dari semester :"));
				final Combobox formStartSemester = new Combobox();
				MyComboitemConfig comboitem;
				for (int i = 1; i <= 30; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(i + "");
					comboitem.setValue(i);
					formStartSemester.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semester saat ini");
				comboitem.setValue(null);
				formStartSemester.appendChild(comboitem);
				Common.selectComboItem(formStartSemester, null);

				row.appendChild(formStartSemester);
				formStartSemester.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sampai dengan semester :"));
				final Combobox formEndSemester = new Combobox();
				for (int i = 1; i <= 30; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(i + "");
					comboitem.setValue(i);
					formEndSemester.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semester saat ini");
				comboitem.setValue(null);
				formEndSemester.appendChild(comboitem);
				row.appendChild(formEndSemester);
				formEndSemester.setReadonly(true);
				Common.selectComboItem(formEndSemester, null);

				row = new MyFormRow();
				row.setParent(rows);
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
				cancel.setTooltiptext("keluar");
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						addWindow.detach();

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});

						@SuppressWarnings("unchecked")
						final List<Mahasiswa> mahasiswas = initCriteria(true).list();
						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								int rowIndex = 1;
								for (Mahasiswa mahasiswa : mahasiswas) {
									label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size()) + " %)");

									Integer mulai = (Integer) (formStartSemester.getSelectedItem() == null
											|| formStartSemester.getSelectedItem().getValue() == null
													? mahasiswa.currentSemester()
													: formStartSemester.getSelectedItem().getValue());
									Integer sampai = (Integer) (formEndSemester.getSelectedItem() == null
											|| formEndSemester.getSelectedItem().getValue() == null
													? mahasiswa.currentSemester()
													: formEndSemester.getSelectedItem().getValue());

									for (Integer smt = mulai; smt <= sampai; smt++) {
										KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null,
												null);
										KrsMahasiswa krsMahasiswaSp = Common.singkronkanKrsMahasiswa(mahasiswa, smt,
												null, Perkuliahan.SEMESTER_PENDEK);
										krsMahasiswa.setKelas(kelas.getNama());
										krsMahasiswaSp.setKelas(kelas.getNama());

										Session session = null;
										try {
											session = HibernateUtil.openSession();
											session.getTransaction().begin();
											Common.refreshUpdate(session, krsMahasiswa);
											Common.refreshUpdate(session, krsMahasiswaSp);
											session.getTransaction().commit();
										} finally {
											if (session != null) {
												try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "rollback sinkronisasi kelas"); }
												try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "clear sinkronisasi kelas"); }
												try { if (session.isConnected()) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "disconnect sinkronisasi kelas"); }
												try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "close sinkronisasi kelas"); }
											}
										}

									}

									rowIndex++;
								}
								mahasiswas.clear();
								label.setValue("");
															} finally {
										// Semua session worker ditutup per transaksi pada blok di atas.
								}
							}
						}).start();

					}
				});
				save.setTooltiptext("simpan");
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);

				addWindow.setVisible(true);
				addWindow.onModal();

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nim", "nama", "tahunangkatan",
				"jurusan.nama", "jurusan.fakultas", "program");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
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
					// System.out.println("file = " + file.getAbsolutePath());
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
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
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
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

	/**
	 * Memproses berkas Excel (.xlsx) yang diunggah untuk menetapkan keanggotaan {@link #kelas} secara
	 * massal. Setiap baris data (mulai baris ke-2, kolom pertama berisi identitas mahasiswa) dicoba
	 * dicocokkan sebagai objek {@link Mahasiswa} lewat {@code Common#getSheetContentAsObject}; bila
	 * gagal, dicoba fallback pencarian berdasarkan NIM mentah lewat
	 * {@code ConstantValues#ambilByNim}. Baris yang cocok diberi {@code kelas.getNama()} lalu
	 * disimpan dalam transaksi Hibernate per baris (rollback eksplisit bila satu baris gagal, agar
	 * baris berikutnya tidak ikut gagal karena transaksi yang masih aktif). Dijalankan pada thread
	 * terpisah; progres ditampilkan lewat {@link Label} yang dipantau {@link Timer} 200ms, dan hasil
	 * akhirnya dirangkum ke {@link ais.common.LaporanUpload} (baris berhasil/dilewati/gagal) yang
	 * diselesaikan lewat {@code eventListener} setelah proses tuntas.
	 *
	 * @param file          berkas .xlsx yang sudah diunggah dan disimpan sementara di server
	 * @param eventListener dipanggil (via {@link ais.common.LaporanUpload#selesaikan}) setelah proses
	 *                      upload dan pelaporan selesai, biasanya untuk menyegarkan grid
	 * @throws Exception diteruskan dari kegagalan pembacaan berkas Excel
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		// Laporan hasil per baris. Menggantikan Label "peringatan" yang disiapkan untuk
		// menampung keterangan baris bermasalah tetapi TIDAK PERNAH diisi, sehingga baris
		// yang tak cocok hilang tanpa kabar sementara notifikasi tetap berbunyi berhasil.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Mahasiswa ke Kelas");
		laporan.setNamaBerkasSumber(file.getName());

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
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								// Fallback pencarian lewat NIM. Layar ini sebelumnya TIDAK punya fallback,
								// sehingga baris gagal dicocokkan begitu sel tak terbaca sebagai objek/ID.
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}
							if (mahasiswa != null && mahasiswa.getId() != null) {

								mahasiswa.setKelas(kelas.getNama());

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswa);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() pada baris
									// berikutnya melempar "Transaction already active" -- satu baris bermasalah
									// membuat SELURUH baris sesudahnya ikut gagal tanpa jejak.
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
										"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
											+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/KelasPunyaMahasiswaHelper.java:729");
				}

				label.setValue("");
							} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "clear upload kelas"); }
						try { if (session.isConnected()) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "disconnect upload kelas"); }
						try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "close upload kelas"); }
					}
				}
			}
		}).start();
	}
}
