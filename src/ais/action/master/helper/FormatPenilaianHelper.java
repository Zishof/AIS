package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.PembobotanNilaiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PembombotanNilai;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper terfokus untuk format penilaian. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code PembombotanNilai
 * selectedPembombotanNilai}, {@code TampilDetailNilaiInterface tampilDetailNilaiInterface}, {@code Perkuliahan
 * perkuliahan}, {@code MyWindow window}, {@code String filterKeyword}, {@code Label labelJumlah};
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code save()}, {@code displayProses()});
 * operasi domain lain ({@code apakahPerkuliahanObe()}, {@code belumAdaStrukturPenilaianObe()}, {@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class FormatPenilaianHelper {
	public static boolean apakahPerkuliahanObe(Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return false;
		}
		ais.database.model.Kurikulum kurikulum = perkuliahan.getKurikulum();
		if (kurikulum == null && perkuliahan.getKurikulumPunyaMatakuliah() != null) {
			kurikulum = perkuliahan.getKurikulumPunyaMatakuliah().getKurikulum();
		}
		return kurikulum != null
				&& kurikulum.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap());
	}

	/**
	 * Menentukan apakah struktur penilaian OBE kelas ini belum lengkap. Format Nilai
	 * OBE dibentuk dari CPMK pada Matakuliah dan Sub-CPMK/rincian pada
	 * KurikulumPunyaMatakuliah; salah satunya kosong berarti pengguna harus diarahkan
	 * lebih dahulu ke fasilitas salin RPS semester sebelumnya.
	 */
	private static boolean belumAdaStrukturPenilaianObe(Perkuliahan perkuliahan) {
		if (perkuliahan == null || perkuliahan.getKurikulumPunyaMatakuliah() == null) {
			return true;
		}
		ais.database.model.KurikulumPunyaMatakuliah kpm = perkuliahan.getKurikulumPunyaMatakuliah();
		ais.database.model.Matakuliah matakuliah = kpm.getMatakuliah() == null
				? perkuliahan.getMatakuliah() : kpm.getMatakuliah();
		String cpmk = matakuliah == null ? "" : matakuliah.getCapaianPembelajaranLulusan();
		String rincian = kpm.getRincian();
		return cpmk == null || cpmk.trim().isEmpty()
				|| rincian == null || rincian.trim().isEmpty() || "{}".equals(rincian.trim());
	}

	private MyGrid grid;

	private PembombotanNilai selectedPembombotanNilai;

	private TampilDetailNilaiInterface tampilDetailNilaiInterface;

	private Perkuliahan perkuliahan;

	private MyWindow window;

	private String filterKeyword = "";

	private Label labelJumlah;

	/**
	 * Renderer lokal untuk layar/komponen {@link FormatPenilaianHelper}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FormatPenilaianHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see FormatPenilaianHelper
	 */
	class PembombotanNilaiRenderer extends ais.ui.util.MyRowRenderer {

		public PembombotanNilaiRenderer() {

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembombotanNilai pembombotanNilai = (PembombotanNilai) arg1;

			Radio checkbox = new Radio(pembombotanNilai.getNama());
			checkbox.setTooltiptext(pembombotanNilai.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(pembombotanNilai.getId() + "");

			if (perkuliahan != null && perkuliahan.getPembombotanNilai() != null
					&& perkuliahan.getPembombotanNilai().getId() != null) {
				checkbox.setChecked(pembombotanNilai.getId() != null
						&& pembombotanNilai.getId().equals(perkuliahan.getPembombotanNilai().getId()));
			}

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					selectedPembombotanNilai = pembombotanNilai;
				}
			});

			if (pembombotanNilai.getDimilikiOleh() == null) {
				new Label(pembombotanNilai.getKeterangan()).setParent(arg0);
				new Label(pembombotanNilai.getOleh()).setParent(arg0);
				new Label("").setParent(arg0);
			} else {
				new Label(pembombotanNilai.getKeterangan()).setParent(arg0);
				new Label(pembombotanNilai.getDimilikiOleh().getNama()).setParent(arg0);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(pembombotanNilai.getDimilikiOleh() != null);

				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						PembobotanNilaiAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								selectedPembombotanNilai = (PembombotanNilai) arg0.getData();

								Session session = HibernateUtil.currentSession();
								if (save(session)) {
									window.setVisible(false);
								}
							}
						}, pembombotanNilai);
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(pembombotanNilai.getDimilikiOleh() != null);

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
												Common.refreshDelete(HibernateUtil.currentSession(), pembombotanNilai);
												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus format Nilai", "Format Nilai ini kemungkinan besar sudah pernah dipakai pada proses penilaian mahasiswa, sehingga tidak dapat dihapus demi menjaga konsistensi data nilai yang sudah tersimpan.", e, new String[]{"Buat format Nilai baru sebagai pengganti, alih-alih menghapus format yang sudah pernah dipakai.", "Pastikan format Nilai yang ingin dihapus benar-benar belum pernah digunakan pada proses penilaian apa pun.", "Jika Bapak/Ibu yakin format ini seharusnya bisa dihapus, hubungi Administrator untuk pengecekan lebih lanjut."});
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

	}

	public boolean save(Session session) throws InterruptedException {

		// Pastikan selectedPembombotanNilai sudah tersimpan sebelum set ke perkuliahan.
		// Bila transient (id == null), simpan dulu agar flush tidak lempar TransientObjectException.
		if (selectedPembombotanNilai != null && selectedPembombotanNilai.getId() == null) {
			try {
				session.saveOrUpdate(selectedPembombotanNilai);
			} catch (Exception eSave) {
				ais.common.Common.tampilErrorJikaAdmin(eSave);
			}
		}
		perkuliahan.setPembombotanNilai(selectedPembombotanNilai);

		Common.refreshUpdate(session, perkuliahan);

		PembombotanNilai.setDefaultPembobotan(perkuliahan, session, true);
		this.tampilDetailNilaiInterface.realoadNilai(perkuliahan);
		return true;
	}

	public void displayProses(final Perkuliahan perkuliahan, final MyWindow window,
			final TampilDetailNilaiInterface tampilDetailNilaiInterface) {

		if (apakahPerkuliahanObe(perkuliahan)) {
			try {
				boolean strukturObeKosong = belumAdaStrukturPenilaianObe(perkuliahan);
				// OBE: Format Nilai TIDAK diedit manual - komponen & bobotnya ditentukan CPMK/Sub-CPMK
				// di RPS OBE. Alih-alih sekadar alert pencegahan, TAMPILKAN halaman RPS OBE (fokus tab
				// "CPMK & Sub-CPMK") di dalam window ini agar dosen bisa langsung mengatur CPMK/bobot.
				Common.clear(window);
				window.setTitle(Common.getBahasaConfig(strukturObeKosong
						? "RPS OBE - Salin Format Semester Sebelumnya"
						: "RPS OBE - CPMK & Sub-CPMK"));
				window.setWidth("95%");
				window.setHeight("95%");
				window.setMaximizable(true);
				window.setSizable(true);
				java.util.Map<String, Object> argRps = new java.util.HashMap<String, Object>();
				argRps.put("perkuliahan", String.valueOf(perkuliahan.getId()));
				// Jika struktur masih kosong, buka tab Mata Kuliah karena di sanalah tombol
				// "Salin Data dari RPS Lain" tersedia. Setelah disalin, refresh otomatis
				// membangun CPMK/Sub-CPMK dan Format Nilai tanpa menyalin nilai mahasiswa.
				argRps.put("tabAktif", strukturObeKosong ? "Mata Kuliah" : "CPMK & Sub-CPMK");
				// Oper juga id KurikulumPunyaMatakuliah & Matakuliah dari perkuliahan ini agar RpsObeAction
				// otomatis memuat tab OBE: cabang "kur" di doAfterCompose memanggil onSearchDefault sehingga
				// tab CPMK/Sub-CPMK langsung terisi (tanpa perlu pengguna memilih MK/kurikulum lagi).
				if (perkuliahan.getKurikulumPunyaMatakuliah() != null) {
					argRps.put("kur", String.valueOf(perkuliahan.getKurikulumPunyaMatakuliah().getId()));
					if (perkuliahan.getKurikulumPunyaMatakuliah().getMatakuliah() != null) {
						argRps.put("mk",
								String.valueOf(perkuliahan.getKurikulumPunyaMatakuliah().getMatakuliah().getId()));
					}
				}
				if (!argRps.containsKey("mk") && perkuliahan.getMatakuliah() != null) {
					argRps.put("mk", String.valueOf(perkuliahan.getMatakuliah().getId()));
				}
				// KE-FIX (UiException "Not unique in ID space <MyWindow addWindow>: window"):
				// rps_obe.zul mendeklarasikan root <window id="window">. Bila tombol "Format Nilai"
				// terklik dobel (race dua request onClick beruntun) sebelum Common.clear(window) di
				// atas benar-benar melepas hasil createComponents sebelumnya, percobaan kedua
				// menambah anak ber-id "window" yang sama ke idspace "window" (addWindow) ini dan
				// meledak. Jaga idempoten: lepas fellow "window" lama (jika masih ada) sebelum
				// membuat yang baru, alih-alih mengandalkan clear() saja.
				org.zkoss.zk.ui.Component existing = window.getFellowIfAny("window");
				if (existing != null) {
					existing.detach();
				}
				org.zkoss.zk.ui.Executions.createComponents(
						"/WEB-INF/z/x/y/pages/master/rps_obe.zul", window, argRps);
				try {
					window.doModal();
				} catch (Exception eModal) { ais.common.ErrorAuditUtil.record(eModal, "auto-audit(empty-catch) src/ais/action/master/helper/FormatPenilaianHelper.java:231");
				}
				return;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				try {
					MyMessageboxConfig.show(
							"Perkuliahan ini menggunakan kurikulum OBE. Silakan atur bobot penilaian di menu RPS OBE (tab CPMK & Sub-CPMK).",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/master/helper/FormatPenilaianHelper.java:240");
				}
				return;
			}
		}

		Common.clear(window);
		this.window = window;
		filterKeyword = "";
		labelJumlah = null;

		window.setTitle("Pilihan Pembobotan Nilai");
		window.setWidth("75%");
		window.setHeight("95%");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(window);

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar pembobotan penilaian");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyButtonConfig button = new MyButtonConfig("Tambah Format Nilai Baru", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				PembobotanNilaiAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						selectedPembombotanNilai = (PembombotanNilai) arg0.getData();

						Session session = HibernateUtil.currentSession();
						if (save(session)) {
							window.setVisible(false);
						}

					}
				}, new PembombotanNilai());
			}

		});
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			button.setVisible(Common.bolehKonfigurasi("dosen_dapat_menambah_format_penilaian_sendiri"));
		}
		button.setParent(toolbar);

		new org.zkoss.zul.Separator().setParent(toolbar);

		Hbox cariBox = new Hbox();
		cariBox.setAlign("center");
		cariBox.setSpacing("4px");
		cariBox.setParent(toolbar);

		Label cariLabel = new Label("Cari:");
		cariLabel.setStyle("font-weight:600; color:#555; white-space:nowrap;");
		cariLabel.setParent(cariBox);

		final Textbox cariTextbox = new Textbox();
		cariTextbox.setWidth("280px");
		cariTextbox.addEventListener("onChanging", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				filterKeyword = ((InputEvent) event).getValue();
				onSearchDefault(event);
			}
		});
		cariTextbox.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				filterKeyword = cariTextbox.getValue();
				onSearchDefault(event);
			}
		});
		cariTextbox.setParent(cariBox);

		labelJumlah = new Label();
		labelJumlah.setStyle("color:#888; font-size:0.9em; margin-left:6px; white-space:nowrap;");
		labelJumlah.setParent(cariBox);

		if (Common.bolehKonfigurasi("hanya_super_admin_yang_boleh_menambah_format_nilai_baru", Konfigurasi.TIDAK_AKTIF)) {
			button.setVisible(Common.getApakahAdmin());
		}

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column = new MyColumnConfig("Format Nilai");
		column.setParent(columns);
		column.setWidth("50%");

		column = new MyColumnConfig();
		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Dibuat oleh");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		mybutton.setTooltiptext("Tutup");
		mybutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		mybutton.setParent(toolbar);

		mybutton = new MyToolbarbuttonConfig("Pilih Format Nilai", "/img/save.gif");
		mybutton.setTooltiptext("Simpan");
		mybutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();
				if (save(session)) {
					window.setVisible(false);
				}

			}
		});
		mybutton.setParent(toolbar);

		mybutton = new MyToolbarbuttonConfig("Simpan Format Nilai Ke Semua Semester", "/img/save.gif");
		mybutton.setTooltiptext("Simpan");
		mybutton.setVisible(Common.bolehKonfigurasi("aktifkan_simpan_format_nilai_ke_semua_smt", Konfigurasi.TIDAK_AKTIF));
		mybutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();

				final MyWindow addWindow = new MyWindow("", "none", false);
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindow.setWidth("500px");
				addWindow.setHeight("400px");
				addWindow.setPosition("center,center");
				addWindow.onModal();

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(addWindow);
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
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
				final Combobox tahunAjaran;
				Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(null),
						perkuliahan.getTahunAjaran());
				row.appendChild(tahunAjaran);
				tahunAjaran.setWidth("90%");

				final Combobox ganjilGenap = new Combobox();
				ganjilGenap.setReadonly(true);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				ganjilGenap.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				ganjilGenap.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.SP);
				comboitem.setValue(Perkuliahan.SP);
				ganjilGenap.appendChild(comboitem);

				Common.selectComboItem(true, ganjilGenap, perkuliahan.getGanjilGenap());
				ganjilGenap.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
				row.appendChild(ganjilGenap);
				ganjilGenap.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
				final Combobox program = new Combobox();
				Common.initPrograms(program);
				Common.selectComboItem(true, program,
						perkuliahan.getProgram() == null
								? (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama())
								: perkuliahan.getProgram());
				row.appendChild(program);
				program.setWidth("90%");
				program.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				final Combobox fakultas = new Combobox();
				final Combobox jurusan = new Combobox();

				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				Common.selectComboItem(true, fakultas, perkuliahan.getJurusan() == null ? tbmuser.ambilFakultas()
						: perkuliahan.getJurusan().getFakultas());
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
					Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
				}

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan")));
				Common.selectComboItem(true, jurusan,
						perkuliahan.getJurusan() == null ? tbmuser.ambilJurusan() : perkuliahan.getJurusan());
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Dosen")));
				final AmbilDataDosenBanbox dosen1;
				row.appendChild(dosen1 = new AmbilDataDosenBanbox());
				jurusan.setWidth("90%");

				Common.initKeterangan(rows, "* Format Nilai yang sudah dikunci tidak bisa diubah");

				South south = new South();
				south.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(south, true);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);

				MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				mybutton.setTooltiptext("Tutup");
				mybutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						addWindow.detach();
					}
				});
				mybutton.setParent(toolbar);

				mybutton = new MyToolbarbuttonConfig("Pilih Format Nilai", "/img/save.gif");
				mybutton.setTooltiptext("Simpan");
				mybutton.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						Session session = HibernateUtil.currentSession();

						Criteria criteria = session.createCriteria(Perkuliahan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNull("dikunci"))

								.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

								.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", program.getSelectedItem().getValue()))

								.add(tahunAjaran.getSelectedItem() == null
										|| tahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("tahunAjaran",
														tahunAjaran.getSelectedItem().getValue()))

								.add(ganjilGenap.getSelectedItem() == null
										|| ganjilGenap.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("ganjilGenap",
														ganjilGenap.getSelectedItem().getValue()))

								.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

								.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

						;

						Dosen dosen = (Dosen) dosen1.getAttribute("dosen");
						if (dosen != null) {
							Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.eq("dosen1", dosen),
											Restrictions.eq("dosen2", dosen));

							criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

							criteria.add(criterion);
						}

						List<Perkuliahan> perkuliahans = criteria.list();

						System.out.println("ubah format " + perkuliahans.size());

						EventListener eventListener = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						};

						for (Perkuliahan perkuliahan : perkuliahans) {
							try {
								perkuliahan.setPembombotanNilai(selectedPembombotanNilai);

								Common.refreshUpdate(session, perkuliahan);

								PembombotanNilai.setDefaultPembobotan(perkuliahan, session, true);

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FormatPenilaianHelper.java:624");
							}
						}

						session.flush();

						for (Perkuliahan perkuliahan : perkuliahans) {
							try {
								Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
										eventListener, null);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FormatPenilaianHelper.java:635");
							}
						}

						window.setVisible(false);
						addWindow.detach();

						tampilDetailNilaiInterface.realoadNilai(perkuliahan);
					}
				});
				mybutton.setParent(toolbar);

			}
		});
		mybutton.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void display(Perkuliahan perkuliahan, final MyWindow window,
			final TampilDetailNilaiInterface tampilDetailNilaiInterface) throws Exception {
		perkuliahan = perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan : perkuliahan.getPerkuliahan_paralel();
		this.tampilDetailNilaiInterface = tampilDetailNilaiInterface;
		this.perkuliahan = perkuliahan;
		this.selectedPembombotanNilai = perkuliahan.getPembombotanNilai();

		// OBE: bobot penilaian ditentukan CPMK/Sub-CPMK di RPS OBE, TIDAK ada "ubah format" manual yang
		// dapat mereset nilai. Karena itu klik "Format Nilai" pada perkuliahan OBE cukup MEMBUKA RPS OBE
		// (tab CPMK & Sub-CPMK) via displayProses — LEWATI konfirmasi "yakin ubah format (nilai bisa reset
		// ke 0)" di bawah yang menyesatkan untuk OBE.
		if (apakahPerkuliahanObe(perkuliahan)) {
			displayProses(perkuliahan, window, tampilDetailNilaiInterface);
			return;
		}

		int adaygkosong = ((Number) HibernateUtil.currentSession().createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.ge("totalNilai", 0.1))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (adaygkosong > 0) {
			MyMessageboxConfig.show(
					"Data nilai sebagian atau semua telah diinputkan, apakah Anda yakin ingin mengubah format nilai ?. Jika format nilai diubah saat penilaian telah dilakukan, maka kemungkinan data nilai yang tidak sesuai format akan kembali ke nilai awal atau 0",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								displayProses(FormatPenilaianHelper.this.perkuliahan, window,
										tampilDetailNilaiInterface);
							}

						}
					});
		} else {
			displayProses(perkuliahan, window, tampilDetailNilaiInterface);
		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Dosen> dosens = new ArrayList<Dosen>();
		if (Common.getCurrentUser().getDosen() != null) {
			dosens.add(Common.getCurrentUser().getDosen());
		}
		if (perkuliahan.getDosen1() != null) {
			dosens.add(perkuliahan.getDosen1());
		}
		if (perkuliahan.getDosen2() != null) {
			dosens.add(perkuliahan.getDosen2());
		}
		if (perkuliahan.getDosen3() != null) {
			dosens.add(perkuliahan.getDosen3());
		}
		if (perkuliahan.getDosen4() != null) {
			dosens.add(perkuliahan.getDosen4());
		}
		if (perkuliahan.getDosen5() != null) {
			dosens.add(perkuliahan.getDosen5());
		}
		if (perkuliahan.getDosen6() != null) {
			dosens.add(perkuliahan.getDosen6());
		}
		if (perkuliahan.getDosen7() != null) {
			dosens.add(perkuliahan.getDosen7());
		}
		if (perkuliahan.getDosen8() != null) {
			dosens.add(perkuliahan.getDosen8());
		}
		if (perkuliahan.getDosen9() != null) {
			dosens.add(perkuliahan.getDosen9());
		}
		if (perkuliahan.getDosen10() != null) {
			dosens.add(perkuliahan.getDosen10());
		}

		System.out.println("dosens => " + dosens);

		Criteria criteria = session.createCriteria(PembombotanNilai.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions
						.or(dosens.isEmpty() ? Restrictions.eq("dimilikiOleh", Common.getCurrentUser().getDosen())
								: Restrictions.in("dimilikiOleh", dosens), Restrictions.isNull("dimilikiOleh")))
				.addOrder(Order.asc("dimilikiOleh"));

		if (filterKeyword != null && !filterKeyword.trim().isEmpty()) {
			String kw = "%" + filterKeyword.trim() + "%";
			criteria.add(Restrictions.or(
					Restrictions.ilike("nama", kw),
					Restrictions.ilike("keterangan", kw)));
		}

		List<PembombotanNilai> formatNilai = ConstantValues.simpleList(criteria.addOrder(Order.asc("id")),
				PembombotanNilai.class);

		if (labelJumlah != null) {
			if (filterKeyword != null && !filterKeyword.trim().isEmpty()) {
				labelJumlah.setValue(formatNilai.size() + " ditemukan");
			} else {
				labelJumlah.setValue(formatNilai.size() + " format tersedia");
			}
		}

		ListModel strset = new SimpleListModel(formatNilai);
		grid.setRowRenderer(new PembombotanNilaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
