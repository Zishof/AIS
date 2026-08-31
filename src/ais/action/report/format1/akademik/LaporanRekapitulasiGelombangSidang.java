package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaranSidangTugasAkhir;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi gelombang sidang. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox fakultas}, {@code Combobox
 * jurusan}, {@code Intbox angkatan}, {@code Combobox status}, {@code AmbilDataDosenBanbox searchdosen}, {@code
 * AmbilDataMahasiswaBanbox searchmahasiswa}, {@code Center center}, {@code Toolbar toolbar};
 * inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code generateParameter()}, {@code
 * generateDataDanImageAlbum()}, {@code onLaporan()}); konfigurasi constructor: {@code fakultas}, {@code
 * jurusan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapitulasiGelombangSidang extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Intbox angkatan;
	private Combobox status;
	private AmbilDataDosenBanbox searchdosen;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Center center;
	private Toolbar toolbar;
	private GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir;
	private Combobox program;
	private Combobox searchTahunAkademik;
	private Combobox searchSemesterAbsensi;
	private Combobox searchsidang;
	private Combobox statusLulus;

	@SuppressWarnings("rawtypes")
	private List<Map> maps = null;

	public LaporanRekapitulasiGelombangSidang() {
		super();
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Gelombang Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiGelombangSidang(
			GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) {
		super();
		this.gelombangPendaftaranSidangTugasAkhir = gelombangPendaftaranSidangTugasAkhir;
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Gelombang Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiGelombangSidang(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(null);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan = new Intbox());
		angkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		Common.insertComboDanSemua(status = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.eq("aktif", true));
		row.appendChild(status);
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar"));
		Common.insertComboDanSemua(statusLulus = new Combobox(), new String[] { "nama" }, StatusKeluar.class);
		row.appendChild(statusLulus);
		statusLulus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setWidth("90%");

		if (gelombangPendaftaranSidangTugasAkhir != null) {
			Common.generateTahunAjaranDanSemua(searchTahunAkademik = new Combobox());
			Common.selectComboItem(searchTahunAkademik, null);
		} else {
			Common.generateTahunAjaranDanSemua(searchTahunAkademik = new Combobox());
			Common.selectComboItem(searchTahunAkademik, Common.getCurrentTahunAkademik());
		}
		searchTahunAkademik.setWidth("90%");
		searchTahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		row.appendChild(searchTahunAkademik);

		searchSemesterAbsensi = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchSemesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(searchSemesterAbsensi, null);
		searchSemesterAbsensi.setReadonly(true);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchSemesterAbsensi);

		searchsidang = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Sudah sidang");
		comboitem.setValue(1);
		searchsidang.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Belum sidang");
		comboitem.setValue(0);
		searchsidang.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchsidang.appendChild(comboitem);
		searchsidang.setReadonly(true);

		searchsidang.setSelectedItem(comboitem);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sidang"));
		row.appendChild(searchsidang);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(event);

			}
		};
		button.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekap_gelombang_sidang_mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporan(arg0);
			}
		}));
		if (gelombangPendaftaranSidangTugasAkhir != null) {
			onLaporan(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("jadwal",
				gelombangPendaftaranSidangTugasAkhir == null || gelombangPendaftaranSidangTugasAkhir.getId() == null ? -1L : gelombangPendaftaranSidangTugasAkhir.getId());

		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());
		parameters.put("angkatan", angkatan.getValue() == null ? -1 : angkatan.getValue());
		parameters.put("status", status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? -1L
				: ((StatusMahasiswa) status.getSelectedItem().getValue()).getId());
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());

		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void generateDataDanImageAlbum(Label label) {
		Dosen dosenPemimbing = (Dosen) searchdosen.getAttribute("myValue");
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");
		Criterion criterion = Restrictions.eq("pembimbing", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("ketuaSidang", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", dosenPemimbing));

		Session session = HibernateUtil.currentSession();

		TreeMap<String, String> treeMap = new TreeMap<String, String>();
		treeMap.put("01. Pembimbing I", "pembimbing");
		treeMap.put("02. Pembimbing II", "ketuaSidang");

		treeMap.put("03. Penguji I", "penguji1");
		treeMap.put("04. Penguji II", "penguji2");
		treeMap.put("05. Penguji III", "penguji3");
		treeMap.put("06. Penguji IV", "penguji4");
		treeMap.put("07. Penguji V", "penguji5");

		maps = new ArrayList<Map>();
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Skripsi.class);
		for (String namaPengguji : treeMap.keySet()) {
			String col = treeMap.get(namaPengguji);
			Criteria criteria = session.createCriteria(Skripsi.class).add(Restrictions.isNotNull(col))

					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

					.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.statusAwalMahasiswa", status.getSelectedItem().getValue()))

					.add(statusLulus.getSelectedItem() == null || statusLulus.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.statusKeluar", statusLulus.getSelectedItem().getValue()))

					.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.program", program.getSelectedItem().getValue()))

					.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", jurusan, false))

					.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

					.add(angkatan.getValue() == null || angkatan.getValue() < 1900 ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

					.add(searchTahunAkademik.getSelectedItem() == null
							|| searchTahunAkademik.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAkademik",
											searchTahunAkademik.getSelectedItem().getValue()))

					.add(searchSemesterAbsensi.getSelectedItem() == null
							|| searchSemesterAbsensi.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction("this_.semester%2=" + (searchSemesterAbsensi
											.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

					.add(dosenPemimbing != null ? criterion : Restrictions.sqlRestriction("true"))
					.add(searchsidang.getSelectedItem() == null || searchsidang.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("telahSidang", searchsidang.getSelectedItem().getValue()))
					.addOrder(Order.desc("gelombangPendaftaranSidangTugasAkhir.id"))
					.addOrder(Order.desc("mahasiswa.nim"))

					.add(gelombangPendaftaranSidangTugasAkhir == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("gelombangPendaftaranSidangTugasAkhir",
									gelombangPendaftaranSidangTugasAkhir))

					.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa", mahasiswa))
					.setProjection(Projections.property("id"));

			List<Long> skripsis = criteria.list();
			int index = 0;
			int size = skripsis.size();
			for (Long skripsiId : skripsis) {
				Skripsi skripsi = (Skripsi) ConstantValues.ambil(Skripsi.class.getName(), skripsiId);
				if (skripsi != null) {
					mahasiswa = skripsi.getMahasiswa();
					label.setValue("Memproses data " + mahasiswa + " untuk " + namaPengguji + " ("
							+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
					index++;
					Map map = new HashMap();
					
					
					
					Common.insertProperty(Skripsi.class, skripsi, map, "");
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, skripsi.getSemester(), null,
							null);
					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map, "krs");

					mahasiswa.putPhotoLulus(map);
					Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
					map.put("judisium", judisium == null ? "" : judisium.getNama());
					map.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
					map.put("dosen_pa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
					map.put("dosen_nidn", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
					map.put("dosen_code", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getCode());
					map.put("dosen_nip",
							krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
					map.put("sks", krsMahasiswa.getSksk());
					map.put("semester", krsMahasiswa.getSemester());
					map.put("sksk", krsMahasiswa.getSksk());
					map.put("ipk", krsMahasiswa.getIpk());
					map.put("ipk_ceil", Math.ceil(krsMahasiswa.getIpk()));
					map.put("ipk_floor", Math.floor(krsMahasiswa.getIpk()));
					map.put("ipk_round", Math.round(krsMahasiswa.getIpk()));
					map.put("ipk_terbilang",
							IndonesianNumberToWords.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));
					map.put("ip", krsMahasiswa.getIps());
					map.put("ip_ceil", Math.ceil(krsMahasiswa.getIps()));
					map.put("ip_floor", Math.floor(krsMahasiswa.getIps()));
					map.put("ip_round", Math.floor(krsMahasiswa.getIps()));
					map.put("mutu", mahasiswa.hitungMutu());

					map.put("nim", mahasiswa.getNim());
					map.put("nama_mhs", mahasiswa.getNama());
					map.put("dosen_id", skripsi.getPembimbing() == null ? -1L : skripsi.getPembimbing().getId());
					map.put("id_jadwal", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? -1L
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getId());
					map.put("nama", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? ""
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama());
					map.put("mulai", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? null
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getMulai());
					map.put("sampai", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? null
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getSampai());

					map.put("dosen1", skripsi.getPembimbing() == null ? null : skripsi.getPembimbing().getNama());
					map.put("dosen2", skripsi.getKetuaSidang() == null ? null : skripsi.getKetuaSidang().getNama());
					map.put("dosen3", skripsi.getPenguji1() == null ? null : skripsi.getPenguji1().getNama());
					map.put("dosen4", skripsi.getPenguji2() == null ? null : skripsi.getPenguji2().getNama());
					map.put("dosen5", skripsi.getPenguji3() == null ? null : skripsi.getPenguji3().getNama());
					map.put("dosen6", skripsi.getPenguji4() == null ? null : skripsi.getPenguji4().getNama());
					map.put("dosen7", skripsi.getPenguji5() == null ? null : skripsi.getPenguji5().getNama());

					map.put("jur", mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getNama());
					map.put("fak", mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? null
							: mahasiswa.getJurusan().getFakultas().getNama());
					map.put("tahunangkatan", mahasiswa.getTahunangkatan());
					map.put("status", skripsi.getNilaiKetuaSidang());
					map.put("judul", skripsi.getJudul());
					map.put("kelamin", mahasiswa.getKelamin());

					map.put("status_sidang", skripsi.getTelahSidang().equals(1) ? "Sudah" : "Belum");

					if (mahasiswa.getStatusKeluar() != null && mahasiswa.getSemesterLulus() != null
							&& mahasiswa.getSemesterLulus() <= skripsi.getSemester()) {
						map.put("status_aktif", mahasiswa.getStatusKeluar().getNama());
					} else {
						HistoryStatusMahasiswa historyStatusMahasiswaLoal = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
						map.put("status_aktif", historyStatusMahasiswaLoal == null ? ""
								: historyStatusMahasiswaLoal.getStatusMahasiswa().getNama());
					}

					map.put("pembimbing", namaPengguji);

					map.put("telah_sidang", skripsi.getTelahSidang());
					map.put("status_sidang", skripsi.getTelahSidang().equals(1) ? "Sudah" : "Belum");
					map.put("tanggal_sidang", skripsi.getTanggalSidang());
					map.put("awal_bimbingan", skripsi.getAwalBimbingan());
					map.put("akhir_bimbingan", skripsi.getAkhirBimbingan());

					try {
						Dosen d = (Dosen) classMetadata.getPropertyValue(skripsi, col, EntityMode.POJO);
						map.put("dosen", d == null ? "" : d.getNama());
						map.put("dosen_id", d == null ? "" : d.getId());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapitulasiGelombangSidang.java:516");
						// TODO: handle exception
					}

					maps.add(map);
				}
			}
			skripsis = null;
		}

		ais.action.report.helper.LoadingReportUtil.selesai(label);

	}

	@SuppressWarnings({})
	public void onLaporan(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"Rekap_gelombang_sidang_mahasiswa", ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiGelombangSidang.java:553");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Gelombang Sidang", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}
