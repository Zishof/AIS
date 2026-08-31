package ais.action.master.helper.generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.IsiAngketParameterUmumListener;
import ais.common.Common;
import ais.common.AngketBelumDiisiFilter;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk angket dosen window. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Groupbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Map mapsKey}, {@code Map
 * jumlahChecklistCache}, {@code Map checklistAktifIdCache}, {@code boolean masukan_hasuk_diisi}, {@code List
 * perkuliahans}, {@code Mahasiswa mahasiswa}, {@code MyWindow addWindow}, {@code boolean tampilClose};
 * inisialisasi/lifecycle ({@code init()}, {@code init()}); pembacaan/pencarian ({@code
 * hitungJumlahChecklistAktif()}, {@code ambilChecklistAktifIds()}, {@code ambilIdChecklistJawaban()}, {@code
 * catatChecklistAktif()}, {@code renderChecklistDosenNonAktif()}, {@code
 * ambilChecklistIdNonAktifDariJawaban()}); validasi/perhitungan ({@code hitungJumlahJawabanAktif()}, {@code
 * hitungJumlahJawabanNonAktif()}); mutasi data ({@code simpanParameterTambahan()}, {@code
 * simpanLampiranParameter()}, {@code onSave()}); pelaporan/ekspor ({@code renderParameterTambahanDosen()});
 * operasi domain lain ({@code formatStatusIsianAngket()}, {@code buildNamaIsiAngketParameterDosen()});
 * konfigurasi constructor: {@code masukan_hasuk_diisi}. Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Groupbox
 */
public class AngketDosenWindow extends Groupbox {

	/**
	 * 
	 */
	private Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> mapsKey = new HashMap<String, ChecklistBaruPenilaianDosenOlehMahasiswa>();
	private Map<String, Integer> jumlahChecklistCache = new HashMap<String, Integer>();
	private Map<String, Set<Long>> checklistAktifIdCache = new HashMap<String, Set<Long>>();
	private boolean masukan_hasuk_diisi;

	/**
	 * 
	 */
	private static final long serialVersionUID = -8503828719463870173L;
	private List<Long> perkuliahans;
	private Mahasiswa mahasiswa;
	private MyWindow addWindow;
	private boolean tampilClose;
	private String ta;
	private String jenis;

	public AngketDosenWindow(String ta, String jenis, List<Long> perkuliahans, Mahasiswa mahasiswa, MyWindow addWindow,
			boolean tampilClose) {
		super();

		this.perkuliahans = perkuliahans;
		this.mahasiswa = mahasiswa;
		this.addWindow = addWindow;
		this.tampilClose = tampilClose;
		this.ta = ta;
		this.jenis = jenis;

		masukan_hasuk_diisi = Common.bolehKonfigurasi("masukan_penialain_dosen_harus_diisi", Konfigurasi.TIDAK_AKTIF);

		init(false);
	}

	private void init(boolean refresh) {
		Common.clear(this);
		mapsKey = mahasiswa.byKey(HibernateUtil.currentSession(), refresh);
		jumlahChecklistCache.clear();
		checklistAktifIdCache.clear();
		setWidth("100%");
		setMold("3d");
		setStyle("border:0; background:#f8fafc; padding:0;");

		Div header = new Div();
		header.setParent(this);
		header.setStyle("padding:14px 16px; background:linear-gradient(135deg,#1e3a8a,#2563eb); color:white; border-radius:10px 10px 0 0;");
		Label title = new Label("Angket Penilaian Dosen Tahun Akademik " + ta + " " + jenis);
		title.setStyle("font-size:16px; font-weight:bold; color:white;");
		title.setParent(header);
		Label subtitle = new Label(ais.common.Common.getBahasaConfig("  Berikan penilaian secara jujur dan objektif untuk setiap dosen pengampu."));
		subtitle.setStyle("display:block; margin-top:4px; font-size:11px; color:#dbeafe;");
		subtitle.setParent(header);

		Toolbar toolbar = new Toolbar();
		/* FIX 20-08-2026: Toolbar ZK 5.5 dirender dengan overflow tersembunyi dan tinggi mengikuti isi bawaan,
		 * sehingga tombol "Selesai" yang diberi padding custom ikut terpotong. Tinggi dibuat otomatis,
		 * luapan ditampilkan, dan isinya dijaga tetap satu baris. */
		toolbar.setStyle("padding:8px 12px; background:#ffffff; border-bottom:1px solid #e5e7eb; height:auto; min-height:40px; overflow:visible; white-space:nowrap;");
		this.appendChild(toolbar);

		if (tampilClose) {

			MyToolbarbuttonConfig toolbarbuttonConfig = new MyToolbarbuttonConfig("Selesai", "/img/save.gif");
			toolbar.appendChild(toolbarbuttonConfig);
			toolbarbuttonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Clients.confirmClose(null);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String host = Common.getRequestHostWithProtocol();
							ExecutionsCtrl.getCurrent().sendRedirect((host == null || host.trim().isEmpty() ? "" : host) + "/main");
						}
					});

				}
			});

		}

		MyToolbarbuttonConfig toolbarbuttonConfig = new MyToolbarbuttonConfig("Refresh",
				"/img/svg/refresh.svg");
		toolbar.appendChild(toolbarbuttonConfig);
		toolbarbuttonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(true);
			}
		});

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("border:0; background:#ffffff;");
		grid.setParent(this);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig("Perkuliahan");
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig("Nama Dosen");
		column.setParent(columns);

		ListModel strset = new SimpleListModel(perkuliahans);
		grid.setRowRenderer(new ChecklistRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AngketDosenWindow}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AngketDosenWindow} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render()}, {@code
	 * createNewRowDosen}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AngketDosenWindow
	 */
	class ChecklistRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
					(Serializable) arg1);
			arg0.setStyle("background: transparent;");
			arg0.setValign("top");
			new Label(perkuliahan.infoSimple()).setParent(arg0);

			MyGrid myGrid = new MyGrid();
			myGrid.setHeight("100%");
			myGrid.setWidth("100%");
			myGrid.setParent(arg0);

			Rows rows = new Rows();
			rows.setParent(myGrid);

			Map<String, Dosen> map = perkuliahan.populateDosen();
			if (map.isEmpty()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada dosen")).setParent(arg0);
			} else {
				for (Dosen dosen : map.values()) {
					createNewRowDosen(perkuliahan, dosen, rows);
				}
			}

		}

		private void createNewRowDosen(final Perkuliahan perkuliahan, final Dosen dosen, Rows rows) throws Exception {
			final MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			Set<Long> checklistAktifIds = ambilChecklistAktifIds(perkuliahan, dosen);
			Integer jumlahChecklist = Integer.valueOf(checklistAktifIds.size());

			String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey.get(kodeUnik);
			Integer jumlahSaved = hitungJumlahJawabanAktif(checklistBaruPenilaianDosenOlehMahasiswa, checklistAktifIds);
			Integer jumlahRiwayatNonAktif = hitungJumlahJawabanNonAktif(checklistBaruPenilaianDosenOlehMahasiswa,
					checklistAktifIds);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			new Label(dosen.getNama()).setParent(vbox);

			final Label labelSudahTerisi;
			(labelSudahTerisi = new Label(formatStatusIsianAngket(jumlahChecklist, jumlahSaved,
					jumlahRiwayatNonAktif))).setParent(row);

			if (jumlahChecklist.equals(jumlahSaved)) {
				row.setStyle("border:0;background:#fef9c3;");
			}

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);
			MyButtonConfig button = new MyButtonConfig("Lakukan Penilaian", "/img/Check-icon.png");
			button.setOrient("vertical");
			/* FIX 20-08-2026: setWidth("100%") memaksa tombol selebar induknya, sehingga label ikut terpotong di sel/kolom sempit. Lebar dilepas agar tombol menyesuaikan isi, dan white-space:nowrap menjaga teks tetap satu baris. */
			button.setStyle("white-space:nowrap; border-radius:8px; font-weight:bold; padding:6px 10px;");
			button.setParent(toolbar);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							init(perkuliahan, dosen, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Set<Long> checklistAktifIds = ambilChecklistAktifIds(perkuliahan, dosen);
									Integer jumlahChecklist = Integer.valueOf(checklistAktifIds.size());

									String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_"
											+ dosen.getId();
									ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey
											.get(kodeUnik);
									Integer jumlahSaved = hitungJumlahJawabanAktif(checklistBaruPenilaianDosenOlehMahasiswa,
											checklistAktifIds);
									Integer jumlahRiwayatNonAktif = hitungJumlahJawabanNonAktif(
											checklistBaruPenilaianDosenOlehMahasiswa, checklistAktifIds);

									labelSudahTerisi.setValue(formatStatusIsianAngket(jumlahChecklist, jumlahSaved,
											jumlahRiwayatNonAktif));

									if (jumlahChecklist.equals(jumlahSaved)) {
										row.setStyle("border:0;background:#fef9c3;");
									}

								}
							});
							addWindow.setHeight("95%");
							addWindow.setWidth("95%");
							addWindow.setVisible(true);
							addWindow.onModal();
						}
					});

				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void init(final Perkuliahan perkuliahan, final Dosen dosen, final EventListener eventListener)
			throws Exception {
		addWindow.setTitle("Penilaian Dosen");
		final List<Component> pertanyaanFilter = new ArrayList<Component>();
		final MyCheckboxConfig tampilBelumDiisi = AngketBelumDiisiFilter.create(pertanyaanFilter);

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setStyle("overflow:hidden;");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("overflow:auto; background:#f8fafc;");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");
		grid.setStyle("border:0; background:#f8fafc; margin:0;");

		Columns mainColumns = new Columns();
		mainColumns.setParent(grid);
		MyColumnConfig mainColumn = new MyColumnConfig();
		mainColumn.setParent(mainColumns);
		mainColumn.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);
		Row row;

		row = new MyFormRow();
		row.setParent(rows);

		MyGrid gridDataDosenLagi = new MyGrid();
		gridDataDosenLagi.setParent(row);
		Columns columns = new Columns();
		columns.setParent(gridDataDosenLagi);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("128px");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rowsDataDosen = new Rows();
		rowsDataDosen.setParent(gridDataDosenLagi);

		Row rowDataDosen;

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);

		Image image = new Image(CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen)));
		image.setWidth("128px");
		A a = new A();
		a.appendChild(image);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.previewGambar(CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen)));
			}
		});
		a.setParent(rowDataDosen);

		MyGrid gridDataDosen = new MyGrid();
		gridDataDosen.setParent(rowDataDosen);
		columns = new Columns();
		columns.setParent(gridDataDosen);
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");
		column = new MyColumnConfig();
		column.setParent(columns);

		rowsDataDosen = new Rows();
		rowsDataDosen.setParent(gridDataDosen);

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Dosen  ")));
		rowDataDosen.appendChild(new Label(dosen.getNama()));

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("NIDN  ")));
		rowDataDosen.appendChild(new Label(dosen.getNidn() == null ? "-" : dosen.getNidn()));

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Perkuliahan  ")));
		rowDataDosen.appendChild(new Label(perkuliahan.infoSimple()));

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		String program = mahasiswa.getProgram();
		String angkatan = mahasiswa.getTahunangkatan() == null ? "" : mahasiswa.getTahunangkatan().toString();

		Session session = HibernateUtil.currentSession();
		List<GrupChecklistPenilaianDosen> grupChecklistPenilaianDosens = ConstantValues.simpleList(
				session.createCriteria(GrupChecklistPenilaianDosen.class)

						.createAlias("angketPenilaianDosen", "angketPenilaianDosen", Criteria.LEFT_JOIN)

						.add(Restrictions.or(Restrictions.isNull("angketPenilaianDosen.untukMahasiswa"),
								Restrictions.eq("angketPenilaianDosen.untukMahasiswa", true)))

						.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
								Restrictions.isNull("angketPenilaianDosen.fakultas")))

						.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
								Restrictions.isNull("angketPenilaianDosen.jurusan")))

						.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
								Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
										Restrictions.isNull("angketPenilaianDosen.program"))))

						.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.angkatan", ""),
								Restrictions.or(Restrictions.ilike("angketPenilaianDosen.angkatan", angkatan),
										Restrictions.isNull("angketPenilaianDosen.angkatan"))))

						.addOrder(Order.asc("angketPenilaianDosen.kode")).addOrder(Order.asc("isi"))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))),
				GrupChecklistPenilaianDosen.class);

		String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
			final ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = getOrCreateChecklistDosen(
					mahasiswa, dosen, perkuliahan);
			final Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan = new HashMap<IsiAngketParameterUmum, Object[]>();
			final Textbox masukan = new Textbox(checklistBaruPenilaianDosenOlehMahasiswa == null ? ""
					: checklistBaruPenilaianDosenOlehMahasiswa.getMasukan());
		masukan.setRows(3);
		masukan.setWidth("100%");
		masukan.setStyle("box-sizing:border-box; border:1px solid #cbd5e1; border-radius:8px; padding:8px; background:#ffffff;");

		masukan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
				ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey
						.get(kodeUnik);
				try {
					if (checklistBaruPenilaianDosenOlehMahasiswa != null) {
						checklistBaruPenilaianDosenOlehMahasiswa.setMasukan(masukan.getValue().trim());
						Common.refreshSaveOrUpdate(checklistBaruPenilaianDosenOlehMahasiswa);
					} else {
						checklistBaruPenilaianDosenOlehMahasiswa = new ChecklistBaruPenilaianDosenOlehMahasiswa();
						checklistBaruPenilaianDosenOlehMahasiswa.setMahasiswa(mahasiswa);
						checklistBaruPenilaianDosenOlehMahasiswa.setDosen(dosen);
						checklistBaruPenilaianDosenOlehMahasiswa.setPerkuliahan(perkuliahan);
						checklistBaruPenilaianDosenOlehMahasiswa.setKeterangan("");
						checklistBaruPenilaianDosenOlehMahasiswa.setMasukan(masukan.getValue().trim());
						Session session = HibernateUtil.currentSession();
						session.save(checklistBaruPenilaianDosenOlehMahasiswa);
						session.flush();
					}
					mapsKey.put(kodeUnik, checklistBaruPenilaianDosenOlehMahasiswa);
				} catch (Exception e) {
					init(true);
				}

			}
		});

		Long idAngket = null;
		Set<Long> checklistAktifDitampilkan = new HashSet<Long>();

		for (GrupChecklistPenilaianDosen g : grupChecklistPenilaianDosens) {
			List<ChecklistPenilaianDosen> checklistPenilaianDosens = ConstantValues
					.simpleList(
							session.createCriteria(ChecklistPenilaianDosen.class).addOrder(Order.asc("isi"))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
									.add(Restrictions.eq("grupChecklistPenilaianDosen", g)),
							ChecklistPenilaianDosen.class);
			catatChecklistAktif(checklistAktifDitampilkan, checklistPenilaianDosens);
			Integer jumlahChecklist = Integer.parseInt(Common
					.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5").getNilai().trim());
			try {
				jumlahChecklist = g.getAngketPenilaianDosen().getJumlahPilihan();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:477");

			}

			try {
				if (!checklistPenilaianDosens.isEmpty()
						&& (idAngket == null || !idAngket.equals(g.getAngketPenilaianDosen().getId()))) {

					row = new MyFormRow();
					row.setStyle("background: transparent;");
					row.setParent(rows);

					ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
					groupbox.setStyle("min-height: 200px;");
					groupbox.setParent(row);
					groupbox.appendChild(new MyCaptionStyled("Petunjuk : "));

					Vbox vboxText = new Vbox();
					vboxText.setParent(groupbox);
					String content = g.getAngketPenilaianDosen().getPetunjuk();

					content = content.replaceAll("\n", "<br>");

					Html html = new ais.ui.util.MyHtml(content);
					html.setStyle("font-family: sans-serif;font-size: 11px;");
					html.setParent(vboxText);

					idAngket = g.getAngketPenilaianDosen().getId();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:506");

			}

			row = new MyFormRow();
			row.setVisible(!checklistPenilaianDosens.isEmpty());
			row.setStyle("border:0;background:#dbeafe;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					g.getAngketPenilaianDosen() == null ? "" : g.getAngketPenilaianDosen().getIsi()));

			row = new MyFormRow();
			row.setVisible(!checklistPenilaianDosens.isEmpty());
			row.setStyle("border:0;background:#dbeafe;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(g.getIsi()));

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(!checklistPenilaianDosens.isEmpty());
			row.setStyle("border:0; background:transparent; padding:0;");

			Div checklistCard = new Div();
			checklistCard.setStyle("width:100%; box-sizing:border-box; margin:8px 0 14px 0; padding:12px; background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; box-shadow:0 4px 14px rgba(15,23,42,0.06);");
			row.appendChild(checklistCard);

			MyGrid gridChecklist = new MyGrid();
			gridChecklist.setWidth("100%");
			gridChecklist.setStyle("border:0; background:transparent;");
			gridChecklist.setParent(checklistCard);

			Columns columnsChecklist = new Columns();
			columnsChecklist.setParent(gridChecklist);
			MyColumnConfig columnChecklist = new MyColumnConfig("Pertanyaan dan Penilaian");
			columnChecklist.setWidth("100%");
			columnChecklist.setParent(columnsChecklist);

			Row rowChecklist;

			Rows rowsChecklist = new Rows();
			rowsChecklist.setParent(gridChecklist);
			for (final ChecklistPenilaianDosen c : checklistPenilaianDosens) {
				rowChecklist = new MyFormRow();
				rowChecklist.setValign("top");
				rowChecklist.setStyle("border:0; background:#ffffff;");
				rowChecklist.setParent(rowsChecklist);

				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				vbox.setStyle("box-sizing:border-box; padding:10px 12px; margin:0 0 8px 0; border:1px solid #e5e7eb; border-radius:10px; background:#f8fafc;");
				rowChecklist.appendChild(vbox);

				Label labelPertanyaan = new Label(c.getIsi());
				labelPertanyaan.setStyle("font-weight:bold; color:#0f172a; line-height:18px; margin-bottom:6px; display:block;");
				vbox.appendChild(labelPertanyaan);

				Integer checklistPenilaianDosenOlehMahasiswa = checklistBaruPenilaianDosenOlehMahasiswa == null ? 0
						: checklistBaruPenilaianDosenOlehMahasiswa.getValue(c);
				JSONObject pilihan = new JSONObject(c.getPilihan());
				final Radiogroup radiogroup = new Radiogroup();
				radiogroup.setStyle("display:block; margin-top:4px; margin-bottom:8px;");
				for (Integer i = 1; i <= jumlahChecklist; i++) {
					MyRadioConfig radio = new MyRadioConfig(
							pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					radio.setValue(i.toString());
					radio.setAttribute("value", i);
					if (checklistPenilaianDosenOlehMahasiswa != null) {
						radio.setSelected(checklistPenilaianDosenOlehMahasiswa.equals(i));
					}
					radiogroup.appendChild(radio);

				}
				vbox.appendChild(radiogroup);

				final Textbox keterangan = new Textbox(checklistBaruPenilaianDosenOlehMahasiswa == null ? ""
						: checklistBaruPenilaianDosenOlehMahasiswa.getKeteranganValue(c));
				keterangan.setWidth("100%");
				keterangan.setRows(2);
				keterangan.setStyle("box-sizing:border-box; border:1px solid #cbd5e1; border-radius:8px; padding:7px; background:#ffffff;");
				if (g.getAngketPenilaianDosen() != null && g.getAngketPenilaianDosen().getTampilKeterangan()) {
					Label labelKeterangan = new Label(ais.common.Common.getBahasaConfig("Keterangan tambahan"));
					labelKeterangan.setStyle("display:block; margin:6px 0 4px 0; font-size:11px; font-weight:bold; color:#475569;");
					vbox.appendChild(labelKeterangan);
					vbox.appendChild(keterangan);
				}

				EventListener listener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						Integer nilai = checklistBaruPenilaianDosenOlehMahasiswa == null ? 0
								: checklistBaruPenilaianDosenOlehMahasiswa.getValue(c);
						// FIX NPE: listener ini juga terpasang di onChange textarea "keterangan" (baris
						// addEventListener di bawah), jadi bisa terpicu SEBELUM user memilih radio apa pun
						// -> getSelectedItem() null. Cek null langsung, bukan andalkan try-catch NPE.
						org.zkoss.zul.Radio selectedRadio = radiogroup.getSelectedItem();
						if (selectedRadio != null && selectedRadio.getAttribute("value") instanceof Integer) {
							nilai = (Integer) selectedRadio.getAttribute("value");
						}

						String ket = keterangan.getValue().trim();
						// System.out.println("nilai -> " + nilai + ", ket -> "
						// + ket);
						onSave(dosen, perkuliahan, c, nilai, ket, masukan.getValue().trim());

					}
				};

				radiogroup.addEventListener("onCheck", listener);
				keterangan.addEventListener("onChange", listener);
				AngketBelumDiisiFilter.register(pertanyaanFilter, rowChecklist, checklistCard,
						tampilBelumDiisi, radiogroup);

			}

			renderParameterTambahanDosen(session, g, checklistBaruPenilaianDosenOlehMahasiswa, rows,
					dataParameterTambahan);

		}

		renderChecklistDosenNonAktif(session, checklistBaruPenilaianDosenOlehMahasiswa, checklistAktifDitampilkan, rows);

		rowDataDosen = new MyFormRow();
		rowDataDosen.setValign("top"); 
		rowDataDosen.setParent(rowsDataDosen);
		Label labelMasukan = new Label("Masukan/Saran/Komentar " + (masukan_hasuk_diisi ? "*" : ""));
		labelMasukan.setStyle("font-weight:bold; color:#0f172a;");
		rowDataDosen.appendChild(labelMasukan);
		rowDataDosen.appendChild(masukan);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup tanpa menyelesaikan penilaian");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					eventListener.onEvent(event);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:batal");
				}
				addWindow.setVisible(false);
			}
		});
		batal.setParent(toolbar);
		tampilBelumDiisi.setParent(toolbar);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Simpan dan Tutup", "/img/save.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (masukan_hasuk_diisi && masukan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Masukan/Saran/Komentar harus diisi", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									masukan.focus();
								}
							});
					return;
				}
					if (!simpanParameterTambahan(dataParameterTambahan)) {
						return;
					}

				eventListener.onEvent(event);

				addWindow.setVisible(false);

			}
		});
		cancel.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}


	private Integer hitungJumlahChecklistAktif(Perkuliahan perkuliahan, Dosen dosen) {
		return Integer.valueOf(ambilChecklistAktifIds(perkuliahan, dosen).size());
	}

	@SuppressWarnings("unchecked")
	private Set<Long> ambilChecklistAktifIds(Perkuliahan perkuliahan, Dosen dosen) {
		String key = mahasiswa == null || mahasiswa.getId() == null || perkuliahan == null || perkuliahan.getId() == null
				|| dosen == null || dosen.getId() == null ? "GLOBAL" : mahasiswa.getId() + "_" + perkuliahan.getId()
						+ "_" + dosen.getId();
		Set<Long> cached = checklistAktifIdCache.get(key);
		if (cached != null) {
			return cached;
		}
		Set<Long> result = new HashSet<Long>();
		try {
			Fakultas fakultas = mahasiswa == null || mahasiswa.getJurusan() == null ? null
					: mahasiswa.getJurusan().getFakultas();
			Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
			String program = mahasiswa == null ? "" : mahasiswa.getProgram();
			String angkatan = mahasiswa == null || mahasiswa.getTahunangkatan() == null ? ""
					: mahasiswa.getTahunangkatan().toString();

			Session session = HibernateUtil.currentSession();
			List<Long> ids = session.createCriteria(ChecklistPenilaianDosen.class)
					.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")
					.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen")
					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
							Restrictions.isNull("angketPenilaianDosen.fakultas")))
					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
							Restrictions.isNull("angketPenilaianDosen.jurusan")))
					.add(Restrictions.or(Restrictions.isNull("angketPenilaianDosen.untukMahasiswa"),
							Restrictions.eq("angketPenilaianDosen.untukMahasiswa", true)))
					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
							Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
									Restrictions.isNull("angketPenilaianDosen.program"))))
					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.angkatan", ""),
							Restrictions.or(Restrictions.ilike("angketPenilaianDosen.angkatan", angkatan),
									Restrictions.isNull("angketPenilaianDosen.angkatan"))))
					.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
							Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.setProjection(Projections.property("id")).list();
			if (ids != null) {
				for (Long id : ids) {
					if (id != null) {
						result.add(id);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		checklistAktifIdCache.put(key, result);
		jumlahChecklistCache.put(key, Integer.valueOf(result.size()));
		return result;
	}

	private Integer hitungJumlahJawabanAktif(ChecklistBaruPenilaianDosenOlehMahasiswa hasil,
			Set<Long> checklistAktifIds) {
		if (hasil == null || checklistAktifIds == null || checklistAktifIds.isEmpty()) {
			return Integer.valueOf(0);
		}
		Integer count = Integer.valueOf(0);
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			Long id = ambilIdChecklistJawaban(obj);
			if (id != null && checklistAktifIds.contains(id)) {
				count = Integer.valueOf(count.intValue() + 1);
			}
		}
		return count;
	}

	private Integer hitungJumlahJawabanNonAktif(ChecklistBaruPenilaianDosenOlehMahasiswa hasil,
			Set<Long> checklistAktifIds) {
		if (hasil == null) {
			return Integer.valueOf(0);
		}
		Integer count = Integer.valueOf(0);
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			Long id = ambilIdChecklistJawaban(obj);
			if (id != null && (checklistAktifIds == null || !checklistAktifIds.contains(id))) {
				count = Integer.valueOf(count.intValue() + 1);
			}
		}
		return count;
	}

	private Long ambilIdChecklistJawaban(Object[] obj) {
		if (obj == null || obj.length == 0 || obj[0] == null) {
			return null;
		}
		try {
			return Long.valueOf(String.valueOf(obj[0]));
		} catch (Exception e) {
			return null;
		}
	}

	private String formatStatusIsianAngket(Integer jumlahChecklist, Integer jumlahSaved, Integer jumlahRiwayatNonAktif) {
		int total = jumlahChecklist == null ? 0 : jumlahChecklist.intValue();
		int saved = jumlahSaved == null ? 0 : jumlahSaved.intValue();
		int riwayat = jumlahRiwayatNonAktif == null ? 0 : jumlahRiwayatNonAktif.intValue();
		String status = saved >= total ? "Telah diisi" : "Belum terisi";
		StringBuilder sb = new StringBuilder();
		sb.append(status).append(" - ").append(saved).append(" dari ").append(total).append(" isian aktif");
		if (riwayat > 0) {
			sb.append(", ").append(riwayat).append(" riwayat nonaktif");
		}
		return sb.toString();
	}

	
	private void catatChecklistAktif(Set<Long> target, List<ChecklistPenilaianDosen> checklistPenilaianDosens) {
		if (target == null || checklistPenilaianDosens == null) {
			return;
		}
		for (ChecklistPenilaianDosen c : checklistPenilaianDosens) {
			if (c != null && c.getId() != null) {
				target.add(c.getId());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void renderChecklistDosenNonAktif(Session session,
			ChecklistBaruPenilaianDosenOlehMahasiswa hasil, Set<Long> checklistAktifDitampilkan, Rows rows) {
		if (session == null || hasil == null || rows == null) {
			return;
		}
		List<Long> ids = ambilChecklistIdNonAktifDariJawaban(hasil, checklistAktifDitampilkan);
		if (ids.isEmpty()) {
			return;
		}
		List<ChecklistPenilaianDosen> list = ConstantValues.simpleList(
				session.createCriteria(ChecklistPenilaianDosen.class)
						.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen", Criteria.LEFT_JOIN)
						.add(Restrictions.in("id", ids)).addOrder(Order.asc("grupChecklistPenilaianDosen.id"))
						.addOrder(Order.asc("isi")),
				ChecklistPenilaianDosen.class);
		if (list == null || list.isEmpty()) {
			return;
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setStyle("border:0; background:transparent;");
		row.setParent(rows);

		Div card = new Div();
		card.setStyle("width:100%; box-sizing:border-box; margin:8px 0 14px 0; padding:12px; background:#fff7ed; "
				+ "border:1px solid #fed7aa; border-radius:12px; box-shadow:0 4px 14px rgba(154,52,18,0.08);");
		card.setParent(row);

		Label title = new Label(ais.common.Common.getBahasaConfig("Riwayat Jawaban pada Pertanyaan yang Sudah Dinonaktifkan"));
		title.setStyle("display:block; font-size:13px; font-weight:bold; color:#9a3412; margin-bottom:6px;");
		title.setParent(card);
		Label desc = new Label(
				"Bagian ini hanya menampilkan jawaban lama agar mahasiswa mengetahui bahwa pertanyaan tersebut pernah diisi. Nilai dan keterangan di bagian ini tidak dapat diubah karena pertanyaannya sudah tidak aktif.");
		desc.setStyle("display:block; font-size:11px; color:#9a3412; line-height:16px; margin-bottom:10px;");
		desc.setParent(card);

		for (ChecklistPenilaianDosen c : list) {
			if (c == null || c.getId() == null) {
				continue;
			}
			Div item = new Div();
			item.setStyle("padding:10px 12px; margin-bottom:8px; border:1px dashed #fdba74; border-radius:10px; background:#ffffff;");
			item.setParent(card);

			String grup = "";
			try {
				grup = c.getGrupChecklistPenilaianDosen() == null ? "" : c.getGrupChecklistPenilaianDosen().getIsi();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:848");
			}
			Label q = new Label((grup == null || grup.trim().length() == 0 ? "" : grup + " - ")
					+ (c.getIsi() == null ? "Pertanyaan lama" : c.getIsi()));
			q.setStyle("display:block; font-weight:bold; color:#0f172a; line-height:18px;");
			q.setParent(item);

			Integer nilai = hasil.getValue(c);
			String ket = hasil.getKeteranganValue(c);
			Label nilaiLabel = new Label("Nilai tersimpan: " + (nilai == null ? "-" : String.valueOf(nilai)));
			nilaiLabel.setStyle("display:block; margin-top:5px; color:#334155; font-size:12px;");
			nilaiLabel.setParent(item);
			if (ket != null && ket.trim().length() > 0) {
				Label ketLabel = new Label("Keterangan: " + ket.trim());
				ketLabel.setStyle("display:block; margin-top:4px; color:#64748b; font-size:11px;");
				ketLabel.setParent(item);
			}
		}
	}

	private List<Long> ambilChecklistIdNonAktifDariJawaban(ChecklistBaruPenilaianDosenOlehMahasiswa hasil,
			Set<Long> checklistAktifDitampilkan) {
		List<Long> ids = new ArrayList<Long>();
		if (hasil == null) {
			return ids;
		}
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			if (obj == null || obj.length == 0 || obj[0] == null) {
				continue;
			}
			Long id = null;
			try {
				id = Long.valueOf(String.valueOf(obj[0]));
			} catch (Exception e) {
				id = null;
			}
			if (id == null) {
				continue;
			}
			if (checklistAktifDitampilkan != null && checklistAktifDitampilkan.contains(id)) {
				continue;
			}
			if (!ids.contains(id)) {
				ids.add(id);
			}
		}
		return ids;
	}


	private ChecklistBaruPenilaianDosenOlehMahasiswa getOrCreateChecklistDosen(Mahasiswa mahasiswa, Dosen dosen,
			Perkuliahan perkuliahan) {
		String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
		ChecklistBaruPenilaianDosenOlehMahasiswa data = mapsKey.get(kodeUnik);
		if (data != null && data.getId() != null) {
			return data;
		}
		Session session = HibernateUtil.currentSession();
		try {
			data = (ChecklistBaruPenilaianDosenOlehMahasiswa) session
					.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("dosen", dosen))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
			if (data == null) {
				data = new ChecklistBaruPenilaianDosenOlehMahasiswa();
				data.setMahasiswa(mahasiswa);
				data.setDosen(dosen);
				data.setPerkuliahan(perkuliahan);
				data.setKeterangan("");
				data.setMasukan("");
				Common.refreshSaveOrUpdate(session, data);
				session.flush();
			}
			mapsKey.put(kodeUnik, data);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data;
	}

	private void renderParameterTambahanDosen(Session session, GrupChecklistPenilaianDosen grup,
			ChecklistBaruPenilaianDosenOlehMahasiswa hasil, Rows rows,
			Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan) {
		if (session == null || grup == null || hasil == null || rows == null || hasil.getId() == null) {
			return;
		}
		try {
			Number jumlah = (Number) session.createCriteria(ais.database.model.ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianDosen", grup)).setProjection(Projections.rowCount())
					.uniqueResult();
			if (jumlah == null || jumlah.intValue() <= 0) {
				return;
			}

			MyFormRow sectionRow = new MyFormRow();
			sectionRow.setValign("top");
			sectionRow.setStyle("border:0; background:transparent;");
			sectionRow.setParent(rows);

			Div section = new Div();
			section.setStyle("width:100%; box-sizing:border-box; margin:8px 0 14px 0; padding:12px; background:#f8fafc; border:1px solid #cbd5e1; border-radius:12px;");
			sectionRow.appendChild(section);

			Label title = new Label(ais.common.Common.getBahasaConfig("Parameter Tambahan Angket Dosen"));
			title.setStyle("display:block; font-size:13px; font-weight:bold; color:#334155; margin-bottom:8px;");
			title.setParent(section);

			MyGrid parameterGrid = new MyGrid();
			parameterGrid.setWidth("100%");
			parameterGrid.setStyle("border:0; background:transparent;");
			parameterGrid.setParent(section);

			Columns columns = new Columns();
			columns.setParent(parameterGrid);
			MyColumnConfig labelColumn = new MyColumnConfig();
			labelColumn.setWidth("28%");
			labelColumn.setParent(columns);
			MyColumnConfig inputColumn = new MyColumnConfig();
			inputColumn.setWidth("72%");
			inputColumn.setParent(columns);

			Rows parameterRowsContainer = new Rows();
			parameterRowsContainer.setParent(parameterGrid);

			IsiAngketParameterUmum isiAngketParameterUmum = getOrCreateIsiAngketParameterDosen(session, hasil);
			ArrayList<Row> parameterRows = new ArrayList<Row>();
			HashMap<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
			IsiAngketParameterUmumListener listener = new IsiAngketParameterUmumListener(isiAngketParameterUmum,
					parameterRows, lampiranLains, parameterRowsContainer, grup);
			dataParameterTambahan.put(isiAngketParameterUmum, new Object[] { listener, lampiranLains });
			listener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private IsiAngketParameterUmum getOrCreateIsiAngketParameterDosen(Session session,
			ChecklistBaruPenilaianDosenOlehMahasiswa hasil) {
		IsiAngketParameterUmum isi = null;
		try {
			isi = (IsiAngketParameterUmum) session.createCriteria(IsiAngketParameterUmum.class)
					.add(Restrictions.eq("checklistBaruPenilaianDosenOlehMahasiswa", hasil)).setMaxResults(1)
					.uniqueResult();
			if (isi == null) {
				isi = new IsiAngketParameterUmum();
				isi.setNama(buildNamaIsiAngketParameterDosen(hasil));
				isi.setMahasiswa(mahasiswa);
				isi.setDosen(hasil.getDosen());
				isi.setChecklistBaruPenilaianDosenOlehMahasiswa(hasil);
				isi.setParameterTambahan("");
				isi.setParameterTambahanInds("");
				Common.refreshSaveOrUpdate(session, isi);
				session.flush();
			} else if (isi.getNama() == null || isi.getNama().trim().isEmpty()) {
				isi.setNama(buildNamaIsiAngketParameterDosen(hasil));
				Common.refreshSaveOrUpdate(session, isi);
				session.flush();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return isi;
	}

	private String buildNamaIsiAngketParameterDosen(ChecklistBaruPenilaianDosenOlehMahasiswa hasil) {
		StringBuilder sb = new StringBuilder("Angket Parameter Dosen");
		try {
			if (hasil != null && hasil.getDosen() != null && hasil.getDosen().getNama() != null) {
				sb.append(" - " ).append(hasil.getDosen().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:1019");
		}
		try {
			if (mahasiswa != null && mahasiswa.getNama() != null) {
				sb.append(" oleh " ).append(mahasiswa.getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:1025");
		}
		try {
			if (hasil != null && hasil.getPerkuliahan() != null && hasil.getPerkuliahan().getId() != null) {
				sb.append(" / Perkuliahan #" ).append(hasil.getPerkuliahan().getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:1031");
		}
		String value = sb.toString();
		return value.length() > 250 ? value.substring(0, 250) : value;
	}

	@SuppressWarnings("unchecked")
	private boolean simpanParameterTambahan(Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan) {
		if (dataParameterTambahan == null || dataParameterTambahan.isEmpty()) {
			return true;
		}
		for (IsiAngketParameterUmum isiAngketParameterUmum : dataParameterTambahan.keySet()) {
			Object[] objects = dataParameterTambahan.get(isiAngketParameterUmum);
			IsiAngketParameterUmumListener listener = (IsiAngketParameterUmumListener) objects[0];
			try {
				if (!listener.validate()) {
					return false;
				}
				listener.onSave(isiAngketParameterUmum);
				HashMap<String, LampiranLain> lampiranLains = (HashMap<String, LampiranLain>) objects[1];
				if (lampiranLains != null && !lampiranLains.isEmpty()) {
					simpanLampiranParameter(isiAngketParameterUmum, lampiranLains);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return false;
			}
		}
		return true;
	}

	private void simpanLampiranParameter(IsiAngketParameterUmum isiAngketParameterUmum,
			HashMap<String, LampiranLain> lampiranLains) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				if (lampiranLain == null) {
					continue;
				}
				session.refresh(lampiranLain);
				lampiranLain.setRef(isiAngketParameterUmum.getId());
				session.update(lampiranLain);
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:1080");}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketDosenWindow.java:1085");}
			}
		}
	}

	public boolean onSave(Dosen dosen, Perkuliahan perkuliahan, ChecklistPenilaianDosen checklistPenilaianDosen,
			Integer nilai, String keterangan, String masukan) throws Exception {

		Session session = HibernateUtil.currentSession();

		String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
		try {
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey.get(kodeUnik);
			if (checklistBaruPenilaianDosenOlehMahasiswa == null) {
				checklistBaruPenilaianDosenOlehMahasiswa = new ChecklistBaruPenilaianDosenOlehMahasiswa();
			}
			checklistBaruPenilaianDosenOlehMahasiswa.setValue(nilai, mahasiswa, dosen, perkuliahan,
					checklistPenilaianDosen, keterangan);
			checklistBaruPenilaianDosenOlehMahasiswa.setMasukan(masukan);
			Common.refreshSaveOrUpdate(session, checklistBaruPenilaianDosenOlehMahasiswa);
			session.flush();
			mapsKey.put(kodeUnik, checklistBaruPenilaianDosenOlehMahasiswa);
		} catch (Exception e) {
			init(true);
		}
		return true;
	}
}
