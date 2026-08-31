package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Staff;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan daftar ujian. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademikUjian}, {@code
 * Combobox semesterAbsensiUjian}, {@code Combobox perkuliahanUjian}, {@code Combobox jenisUjian}, {@code
 * MyDatebox tanggalUjian}, {@code Textbox waktuUjian}, {@code AmbilDataRuangBanbox ruangUjian}, {@code
 * MyCheckboxConfig tampilNilai}; inisialisasi/lifecycle ({@code initPerkuliahan()}); operasi domain lain ({@code
 * onLaporanAbsensiUjian()}, {@code generateParameter()}, {@code onLaporanUTS()}, {@code onLaporanUAS()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDaftarUjian extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 6669548741783963326L;
	private Combobox tahunAkademikUjian;
	private Combobox semesterAbsensiUjian;
	private Combobox perkuliahanUjian;
	private Combobox jenisUjian;
	private MyDatebox tanggalUjian;
	private Textbox waktuUjian;
	private AmbilDataRuangBanbox ruangUjian;
	private MyCheckboxConfig tampilNilai;
	private MyDatebox dibuatTanggalUjian;
	private Combobox fakultas;
	private Combobox jurusan;
	private MyCheckboxConfig tampilPembobotan;
	private AmbilDataDosenBanbox dosen;
	private Center center;
	private Toolbar toolbar;
	private Combobox program;
	private Textbox kelas;

	// class PerkuliahanEventListener implements EventListener {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// Common.clear(perkuliahanUjian);
	// perkuliahanUjian.setSelectedItem(null);
	// if (tahunAkademikUjian.getSelectedItem() == null)
	// return;
	// if (semesterAbsensiUjian.getSelectedItem() == null)
	// return;
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue()
	// == null)
	// return;
	// if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue()
	// == null)
	// return;
	// if (program.getSelectedItem() == null||program.getSelectedItem().getValue()
	// == null)
	// return;
	//
	// String myKelas = kelas.getValue();
	// String myProgram = (String) (program.getSelectedItem() ==
	// null||program.getSelectedItem().getValue() == null ? ""
	// : program.getSelectedItem().getLabel());
	//
	// List<Perkuliahan> items = DaoFactory
	// .getInstance()
	// .getPerkuliahanDao()
	// .findByCriteria(
	// Order.asc("waktuMulaiD"),
	// myKelas.trim().equals("") ? Restrictions
	// .sqlRestriction("1=1")
	// : Restrictions.ilike("kelas", myKelas,
	// MatchMode.ANYWHERE),
	//
	// myProgram.trim().equals("") ? Restrictions
	// .sqlRestriction("1=1") : Restrictions
	// .ilike("program", myProgram,
	// MatchMode.ANYWHERE),
	//
	// Restrictions.eq("tahunAjaran", tahunAkademikUjian
	// .getSelectedItem().getValue()),
	// Restrictions.eq("semester", semesterAbsensiUjian
	// .getSelectedItem().getValue()),
	// Restrictions.eq("jurusan", jurusan.getSelectedItem()
	// .getValue()));
	//
	// if (items.size() == 0)
	// return;
	// for (Perkuliahan o : items) {
	// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
	// comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1()
	// .getNama()) + " - " + o.getMatakuliah().getNama());
	// comboitem.setValue(o);
	//
	// String deskripsi = "Smt: "
	// + (o.getSemester() + (o.getKelas() == null
	// || o.getKelas().equals("") ? "" : " "
	// + o.getKelas()))
	// + ", Ruang: "
	// + (o.getRuang() == null ? "" : o.getRuang()
	// .getKodeRuangan())
	// + ", Hari: "
	// + o.getHari()
	// + ", Waktu: "
	// + o.getWaktuMulai()
	// + "-"
	// + o.getWaktuSelesai()
	// + ", Paralel: "
	// + (o.getMerupakan_paralel() == null
	// || !o.getMerupakan_paralel() ? "Bukan" : "Ya");
	//
	// comboitem.setDescription(deskripsi);
	//
	// perkuliahanUjian.appendChild(comboitem);
	// }
	//
	// }
	// }
	//
	// private PerkuliahanEventListener perkuliahanEventListener = new
	// PerkuliahanEventListener();

	class JurusanEventListener implements EventListener {

		@Override
		public void onEvent(Event event) throws Exception {
			// TODO Auto-generated method stub
			Common.clear(perkuliahanUjian);
			perkuliahanUjian.setSelectedItem(null);
			if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
				return;
			}
			if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null)
				return;

			String myKelas = kelas.getValue();
			String myProgram = (String) (program.getSelectedItem() == null
					|| program.getSelectedItem().getValue() == null ? "" : program.getSelectedItem().getLabel());

			if (tahunAkademikUjian.getSelectedItem() == null)
				return;
			if (semesterAbsensiUjian.getSelectedItem() == null)
				return;
			List<Perkuliahan> items = DaoFactory.getInstance().getPerkuliahanDao().findByCriteria(
					Order.asc("waktuMulaiD"),
					myKelas.trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("kelas", myKelas, MatchMode.ANYWHERE),

					myProgram.trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("program", myProgram, MatchMode.ANYWHERE),
					dosen == null || dosen.getAttribute("dosen") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("dosen1", dosen.getAttribute("dosen")),
					Restrictions.eq("tahunAjaran", tahunAkademikUjian.getSelectedItem().getValue()),
					Restrictions.eq("semester", semesterAbsensiUjian.getSelectedItem().getValue()),
					CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)

			);
			if (items.size() == 0)
				return;
			for (Perkuliahan o : items) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
						+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
				comboitem.setValue(o);

				String deskripsi = "Smt: "
						+ (o.getSemester()
								+ (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
						+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
						+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai() + ", Paralel: "
						+ (o.getMerupakan_paralel() == null || !o.getMerupakan_paralel() ? "Bukan" : "Ya");

				comboitem.setDescription(deskripsi);

				perkuliahanUjian.appendChild(comboitem);
			}

		}

	}

	private JurusanEventListener jurusanEventListener = new JurusanEventListener();

	public LaporanDaftarUjian() {
		super();
		try {

			initPerkuliahan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Ujian", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarUjian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		initPerkuliahan();
	}

	private void initPerkuliahan() throws Exception {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		tahunAkademikUjian = Common.generateTahunAjaran(tahunAkademikUjian);

		program = Common.initPrograms(null);

		// class JurusanEventListener implements EventListener {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// // TODO Auto-generated method stub
		// Common.clear(perkuliahanUjian);
		// perkuliahanUjian.setSelectedItem(null);
		// if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue()
		// == null) {
		// return;
		// }
		// Common.clear(perkuliahanUjian);
		// perkuliahanUjian.setSelectedItem(null);
		// if (tahunAkademikUjian.getSelectedItem() == null)
		// return;
		// if (semesterAbsensiUjian.getSelectedItem() == null)
		// return;
		// List<Perkuliahan> items = DaoFactory
		// .getInstance()
		// .getPerkuliahanDao()
		// .findByCriteria(
		// Order.desc("id"),
		// dosen == null
		// || dosen.getAttribute("dosen") == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions
		// .eq("dosen1",
		// dosen.getAttribute("dosen")),
		// Restrictions.eq("tahunAjaran",
		// tahunAkademikUjian.getSelectedItem()
		// .getValue()),
		// Restrictions.eq("semester",
		// semesterAbsensiUjian.getSelectedItem()
		// .getValue()),
		// Restrictions.eq("jurusan", jurusan
		// .getSelectedItem().getValue())
		//
		// );
		// if (items.size() == 0)
		// return;
		// for (Perkuliahan o : items) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel((o.getDosen1() == null ? "" : o
		// .getDosen1().getNama())
		// + " - "
		// + o.getMatakuliah().getNama());
		// comboitem.setValue(o);
		//
		// String deskripsi = "Smt: "
		// + (o.getSemester() + (o.getKelas() == null
		// || o.getKelas().equals("") ? "" : " "
		// + o.getKelas()))
		// + ", Ruang: "
		// + (o.getRuang() == null ? "" : o.getRuang()
		// .getKodeRuangan()) + ", Hari: "
		// + o.getHari() + ", Waktu: " + o.getWaktuMulai()
		// + "-" + o.getWaktuSelesai();
		//
		// comboitem.setDescription(deskripsi);
		//
		// perkuliahanUjian.appendChild(comboitem);
		// }
		//
		// }
		//
		// }
		// jurusan.addEventListener("onChange", new JurusanEventListener());

		jenisUjian = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("UTS");
		comboitem.setValue("UTS");
		jenisUjian.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("UAS");
		comboitem.setValue("UAS");
		jenisUjian.appendChild(comboitem);
		jenisUjian.setSelectedItem(comboitem);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAbsensiUjian(event);
			}
		};

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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + ""));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", jurusanEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian = new Combobox());
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.addEventListener("onChange", eventListener);
		semesterAbsensiUjian.addEventListener("onChange", jurusanEventListener);
		for (int i = 1; i <= 21; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);
		dosen.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikUjian);
		tahunAkademikUjian.setWidth("90%");
		tahunAkademikUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.addEventListener("onChange", jurusanEventListener);
		program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");
		kelas.addEventListener("onChange", jurusanEventListener);
		kelas.addEventListener("onOK", jurusanEventListener);
		kelas.addEventListener("onChange", eventListener);
		kelas.addEventListener("onOK", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Ujian"));
		row.appendChild(jenisUjian);
		jenisUjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Perkuliahan"));
		row.appendChild(perkuliahanUjian = new Combobox());
		perkuliahanUjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ujian"));
		row.appendChild(tanggalUjian = new MyDatebox());
		tanggalUjian.setWidth("90%");
		tanggalUjian.setReadonly(false); 
		tanggalUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
		row.appendChild(waktuUjian = new Textbox());
		waktuUjian.setWidth("90%");
		waktuUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		row.appendChild(ruangUjian = new AmbilDataRuangBanbox());
		ruangUjian.setWidth("90%");
		ruangUjian.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Laporan dibuat Tanggal"));
		row.appendChild(dibuatTanggalUjian = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		dibuatTanggalUjian.setWidth("90%");
		dibuatTanggalUjian.setReadonly(false); 
		dibuatTanggalUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Nilai"));
		row.appendChild(tampilNilai = new MyCheckboxConfig());
		tampilNilai.addEventListener("onCheck", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Pembobotan"));
		row.appendChild(tampilPembobotan = new MyCheckboxConfig());
		tampilPembobotan.addEventListener("onCheck", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		EventListener perkulihanEventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Perkuliahan perkuliahan = (Perkuliahan) (perkuliahanUjian.getSelectedItem() == null ? null
						: perkuliahanUjian.getSelectedItem().getValue());
				if (perkuliahan != null && jenisUjian.getSelectedItem() != null) {
					if (jenisUjian.getSelectedItem().getValue().equals("UTS")) {

						final Pertemuan pertemuan = Common.ambilPertemuan(ConstantValues.UTS, perkuliahan);
						tanggalUjian.setValue(pertemuan.getTanggal());
						waktuUjian.setValue(pertemuan.getWaktuMulai());
						ruangUjian.setAttribute("ruang", pertemuan.getRuang());
						ruangUjian.setValue(pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan());

					} else if (jenisUjian.getSelectedItem().getValue().equals("UAS")) {

						final Pertemuan pertemuan = Common.ambilPertemuan(ConstantValues.UAS, perkuliahan);
						tanggalUjian.setValue(pertemuan.getTanggal());
						waktuUjian.setValue(pertemuan.getWaktuMulai());
						ruangUjian.setAttribute("ruang", pertemuan.getRuang());
						ruangUjian.setValue(pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan());

					}

					eventListener.onEvent(event);
				}
			}
		};

		perkuliahanUjian.addEventListener("onChange", perkulihanEventListener);
		jenisUjian.addEventListener("onChange", perkulihanEventListener);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (semesterAbsensiUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Semester Perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Semester dari daftar dropdown yang tersedia; (2) Pastikan data semester sudah tersedia di sistem; (3) Ulangi proses cetak laporan daftar ujian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (perkuliahanUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Perkuliahan dari daftar yang muncul setelah memilih semester; (2) Pastikan data perkuliahan sudah tersedia di sistem; (3) Ulangi proses cetak laporan daftar ujian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (jenisUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Ujian belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Ujian (UTS/UAS/dll) dari daftar dropdown; (2) Pastikan data jenis ujian sudah tersedia di sistem; (3) Ulangi proses cetak laporan daftar ujian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();

				if (jenisUjian.getSelectedItem().getValue().equals("UTS")) {
					// onLaporanUTS();
					parameters.put("nama_laporan", "Daftar_Hadir_Ujian");
				} else if (jenisUjian.getSelectedItem().getValue().equals("UAS")) {
					// onLaporanUAS();

					Session session = HibernateUtil.currentSession();

					Perkuliahan perkuliahan = (Perkuliahan) (perkuliahanUjian.getSelectedItem() == null
							? new Perkuliahan()
							: perkuliahanUjian.getSelectedItem().getValue());

					List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);

					List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(perkuliahan.getId() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.eq("perkuliahan", perkuliahan))
							.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
							.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("semester", semesterAbsensiUjian.getSelectedItem().getValue()))

							.list();

					System.out.println("perkuliahan = " + perkuliahan.getId() + ", semester = "
							+ semesterAbsensiUjian.getSelectedItem().getValue() + "  detailperkuliahans = "
							+ detailperkuliahans.size());

					List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
					int i = 0;
					for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
						Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
						map.put("nim", detailperkuliahan.getMahasiswa().getNim());
						map.put("nama", detailperkuliahan.getMahasiswa().getNama());
						map.put("kode_matakuliah", detailperkuliahan.getPerkuliahan().getMatakuliah().getKode());

						i = 1;
						for (FormatNilai formatNilai : formatNilais) {
							map.put("nilai_" + i,
									tampilNilai.isChecked() ? (detailperkuliahan.retreiveDetailNilai(formatNilai))
											: null);
							i++;
						}
						map.put("nilai", tampilNilai.isChecked() ? detailperkuliahan.getTotalNilai() : null);
						map.put("nilai_huruf", tampilNilai.isChecked() ? detailperkuliahan.getNilaiHuruf() : "");

						maps.add(map);
					}

					parameters.put("nama_laporan", "Daftar_Hadir_Ujian_UAS_" + formatNilais.size());
					parameters.put("maps", maps);
				}

				return parameters;
			}
		}, "Daftar_Hadir_Ujian", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAbsensiUjian(arg0);

			}
		}));

		onLaporanAbsensiUjian(null);

		// class PerkuliahanEventListener implements EventListener {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// Common.clear(perkuliahanUjian);
		// perkuliahanUjian.setSelectedItem(null);
		// if (tahunAkademikUjian.getSelectedItem() == null)
		// return;
		// if (semesterAbsensiUjian.getSelectedItem() == null)
		// return;
		// List<Perkuliahan> items = DaoFactory
		// .getInstance()
		// .getPerkuliahanDao()
		// .findByCriteria(
		// Order.asc("waktuMulaiD"),
		// dosen == null
		// || dosen.getAttribute("dosen") == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions
		// .eq("dosen1",
		// dosen.getAttribute("dosen")),
		// tahunAkademikUjian.getSelectedItem() == null ? Restrictions
		// .sqlRestriction("1!=1") : Restrictions
		// .eq("tahunAjaran", tahunAkademikUjian
		// .getSelectedItem().getValue()),
		// semesterAbsensiUjian.getSelectedItem() == null ? Restrictions
		// .sqlRestriction("1!=1") : Restrictions
		// .eq("semester", semesterAbsensiUjian
		// .getSelectedItem().getValue()),
		// jurusan.getSelectedItem() == null ||
		// jurusan.getSelectedItem().getValue()==null ? Restrictions
		// .sqlRestriction("1!=1") : Restrictions
		// .eq("jurusan", jurusan
		// .getSelectedItem().getValue())
		//
		// );
		// if (items.size() == 0)
		// return;
		// for (Perkuliahan o : items) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel((o.getDosen1() == null ? "" : o
		// .getDosen1().getNama())
		// + " - "
		// + o.getMatakuliah().getNama());
		// comboitem.setValue(o);
		//
		// String deskripsi = "Smt: "
		// + (o.getSemester() + (o.getKelas() == null
		// || o.getKelas().equals("") ? "" : " "
		// + o.getKelas()))
		// + ", Ruang: "
		// + (o.getRuang() == null ? "" : o.getRuang()
		// .getKodeRuangan())
		// + ", Hari: "
		// + o.getHari()
		// + ", Waktu: "
		// + o.getWaktuMulai()
		// + "-"
		// + o.getWaktuSelesai()
		// + ", Paralel: "
		// + (o.getMerupakan_paralel() == null
		// || !o.getMerupakan_paralel() ? "Bukan"
		// : "Ya");
		//
		// comboitem.setDescription(deskripsi);
		//
		// perkuliahanUjian.appendChild(comboitem);
		// }
		// }
		// }
		//
		// PerkuliahanEventListener myeventListener = new
		// PerkuliahanEventListener();

		// if (tahunAkademikUjian != null) {
		// tahunAkademikUjian = Common.generateTahunAjaran(tahunAkademikUjian);
		// tahunAkademikUjian.addEventListener("onChange", myeventListener);
		// for (int i = 1; i <= 21; i++) {
		// comboitem = new MyComboitemConfig();
		// comboitem.setLabel(i + "");
		// comboitem.setValue(i);
		// semesterAbsensiUjian.appendChild(comboitem);
		// }
		// Common.selectComboItem(semesterAbsensiUjian, 1);
		// semesterAbsensiUjian.addEventListener("onChange", myeventListener);
		// dosen.addEventListener("onChange", eventListener);
		// try {
		// eventListener.onEvent(null);
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanDaftarUjian.java:694");
		// // TODO Auto-generated catch block
		// Common.tampilErrorJikaAdmin(e);
		// }
		// }

	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	@SuppressWarnings({})
	public void onLaporanAbsensiUjian(Event event) throws Exception {
		if (semesterAbsensiUjian.getSelectedItem() == null) {
			Common.clear(center);
			return;
		}
		if (perkuliahanUjian.getSelectedItem() == null) {
			Common.clear(center);
			return;
		}
		if (jenisUjian.getSelectedItem() == null) {
			Common.clear(center);
			return;
		}

		if (jenisUjian.getSelectedItem().getValue().equals("UTS")) {
			onLaporanUTS(event);
		} else if (jenisUjian.getSelectedItem().getValue().equals("UAS")) {
			onLaporanUAS(event);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		if (jenisUjian.getSelectedItem().getValue().equals("UTS")) {

			Perkuliahan perkuliahan = (Perkuliahan) (perkuliahanUjian.getSelectedItem() == null ? new Perkuliahan()
					: perkuliahanUjian.getSelectedItem().getValue());

			Staff staff = Common.getKaprodi(perkuliahan.getJurusan());

			if (tanggalUjian.getValue() != null) {
				Calendar calendar = Calendar.getInstance(Common.locale);
				calendar.setTime(tanggalUjian.getValue());
				int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

				int value = dayOfWeek - 2;
				value = (value < 0 ? 7 + value : value);
				parameters.put("hari", Common.haris[value]);
				parameters.put("tanggal", dateFormat.format(tanggalUjian.getValue()));
			} else {
				parameters.put("hari", "");
				parameters.put("tanggal", "");
			}

			parameters.put("hari_tanggal", tanggalUjian.getValue());

			parameters.put("perkuliahan", perkuliahan.getId());
			parameters.put("kelas", perkuliahan.getKelas());

			parameters.put("waktu", waktuUjian.getValue().trim());
			parameters.put("ruang", ruangUjian.getAttribute("ruang") == null ? ""
					: ((Ruang) ruangUjian.getAttribute("ruang")).getKodeRuangan());
			parameters.put("pudek_1", staff == null ? "" : staff.getNama());
			parameters.put("kaprodi", staff == null ? "" : staff.getNama());
			parameters.put("nip", staff == null ? "" : staff.getNip());
			parameters.put("tanggal_dibuat",
					dibuatTanggalUjian.getValue() == null ? "" : dateFormat.format(dibuatTanggalUjian.getValue()));
			parameters.put("tampil_nilai", tampilNilai.isChecked() ? 1 : 0);
			parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
			// parameters
			// .put("jenis_semester",
			// ((Integer) semesterAbsensiUjian.getSelectedItem()
			// .getValue()) % 2 == 0 ? Common.ROMAWI[((Integer)
			// semesterAbsensiUjian
			// .getSelectedItem().getValue())]
			// + " ("
			// + Perkuliahan.GENAP + ")"
			// : Common.ROMAWI[((Integer) semesterAbsensiUjian
			// .getSelectedItem().getValue())]
			// + " ("
			// + Perkuliahan.GANJIL
			// + ")");
			parameters.put("jenis_semester",
					((Integer) semesterAbsensiUjian.getSelectedItem().getValue()) % 2 == 0
							? (Common.ROMAWI[((Integer) semesterAbsensiUjian.getSelectedItem().getValue())] + " / "
									+ Perkuliahan.GENAP + " ")
							: (Common.ROMAWI[((Integer) semesterAbsensiUjian.getSelectedItem().getValue())] + " / "
									+ Perkuliahan.GANJIL + ""));

			Map<String, Dosen> mapDosen = perkuliahan.populateDosen();
			if (mapDosen.size() > 1) {
				String dosenPengampu = "";
				for (Dosen dosen : mapDosen.values()) {
					dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}
				parameters.put("dosen_pengajar", dosenPengampu);
			} else {
				parameters.put("dosen_pengajar",
						perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
			}

		} else if (jenisUjian.getSelectedItem().getValue().equals("UAS")) {

			Perkuliahan perkuliahan = (Perkuliahan) (perkuliahanUjian.getSelectedItem() == null ? new Perkuliahan()
					: perkuliahanUjian.getSelectedItem().getValue());

			Staff staff = Common.getKaprodi(perkuliahan.getJurusan());

			if (tanggalUjian.getValue() != null) {
				Calendar calendar = Calendar.getInstance(Common.locale);
				calendar.setTime(tanggalUjian.getValue());
				int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

				int value = dayOfWeek - 2;
				value = (value < 0 ? 7 + value : value);

				parameters.put("hari", Common.haris[value]);
				parameters.put("tanggal", dateFormat.format(tanggalUjian.getValue()));
			} else {
				parameters.put("hari", "");
				parameters.put("tanggal", "");
			}
			parameters.put("hari_tanggal", tanggalUjian.getValue());
			parameters.put("perkuliahan", perkuliahan.getId());
			parameters.put("waktu", waktuUjian.getValue().trim());
			parameters.put("ruang", ruangUjian.getAttribute("ruang") == null ? ""
					: ((Ruang) ruangUjian.getAttribute("ruang")).getKodeRuangan());
			parameters.put("kaprodi", staff == null ? "" : staff.getNama());
			parameters.put("pudek_1", staff == null ? "" : staff.getNama());
			parameters.put("nip", staff == null ? "" : staff.getNip());
			parameters.put("tanggal_dibuat",
					dibuatTanggalUjian.getValue() == null ? "" : dateFormat.format(dibuatTanggalUjian.getValue()));
			parameters.put("tampil_nilai", tampilNilai.isChecked() ? 1 : 0);
			parameters.put("tampil_pembobotan", tampilPembobotan.isChecked() ? 1 : 0);
			parameters.put("kelas", perkuliahan.getKelas());
			parameters.put("fakultas", perkuliahan.getJurusan().getFakultas().getNama());
			parameters.put("jenis_semester",
					((Integer) semesterAbsensiUjian.getSelectedItem().getValue()) % 2 == 0
							? (Common.ROMAWI[((Integer) semesterAbsensiUjian.getSelectedItem().getValue())] + " / "
									+ Perkuliahan.GENAP + " ")
							: (Common.ROMAWI[((Integer) semesterAbsensiUjian.getSelectedItem().getValue())] + " / "
									+ Perkuliahan.GANJIL + ""));
			parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
			parameters.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
			parameters.put("dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
			parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
			parameters.put("dosen_2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
			parameters.put("nip_dosen_2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getCode());

			parameters.put("kelas", perkuliahan.getKelas());
			parameters.put("jurusan", perkuliahan.getJurusan().getNama());

			List<FormatNilai> formatNilais = Common.getFormatNilais(HibernateUtil.currentSession(), perkuliahan);

			int i = 1;
			for (FormatNilai formatNilai : formatNilais) {

				if (tampilPembobotan.isChecked()) {
					parameters.put("col" + i, formatNilai.getNama() + "\n" + formatNilai.getPersen() + "%");
				} else {
					parameters.put("col" + i, formatNilai.getNama());
				}
				i++;
			}

			Map<String, Dosen> mapDosen = perkuliahan.populateDosen();
			if (mapDosen.size() > 1) {
				String dosenPengampu = "";
				for (Dosen dosen : mapDosen.values()) {
					dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}
				parameters.put("dosen_pengajar", dosenPengampu);
			} else {
				parameters.put("dosen_pengajar",
						perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
			}

		}

		List<NilaiHuruf> nilaiHurufs = HibernateUtil.currentSession().createCriteria(NilaiHuruf.class)
				.addOrder(Order.desc("nilaiDiIPK")).list();
		String nilaiHuruf = "";
		for (NilaiHuruf n : nilaiHurufs) {
			nilaiHuruf += n.getNilaiHuruf() + " = " + Common.numberFormat.get().format(n.getMulai()) + " s.d "
					+ Common.numberFormat.get().format(n.getSampai()) + "\n";
		}
		parameters.put("nilaiHuruf", nilaiHuruf);

		return parameters;

	}

	@SuppressWarnings({})
	private void onLaporanUTS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Daftar_Hadir_Ujian",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Ujian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked" })
	private void onLaporanUAS(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();

		Perkuliahan perkuliahan = (Perkuliahan) (perkuliahanUjian.getSelectedItem() == null ? new Perkuliahan()
				: perkuliahanUjian.getSelectedItem().getValue());

		List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);

		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan"))
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.add(perkuliahan.getId() == null ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.eq("perkuliahan", perkuliahan))
				.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
				.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
				.add(Restrictions.eq("semester", semesterAbsensiUjian.getSelectedItem().getValue()))

				.list();

		System.out.println("perkuliahan = " + perkuliahan.getId() + ", semester = "
				+ semesterAbsensiUjian.getSelectedItem().getValue() + "  detailperkuliahans = "
				+ detailperkuliahans.size());

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		int i = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
			map.put("nim", detailperkuliahan.getMahasiswa().getNim());
			map.put("nama", detailperkuliahan.getMahasiswa().getNama());
			map.put("kode_matakuliah", detailperkuliahan.getPerkuliahan().getMatakuliah().getKode());

			i = 1;
			for (FormatNilai formatNilai : formatNilais) {

				map.put("nilai_" + i,
						tampilNilai.isChecked() ? (detailperkuliahan.retreiveDetailNilai(formatNilai)) : null);
				i++;
			}
			map.put("nilai", tampilNilai.isChecked() ? detailperkuliahan.getTotalNilai() : null);
			map.put("nilai_huruf", tampilNilai.isChecked() ? detailperkuliahan.getNilaiHuruf() : "");

			maps.add(map);
		}

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"Daftar_Hadir_Ujian_UAS_" + formatNilais.size(), ais.ui.util.WaktuUtil.getDate(), maps, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Ujian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
