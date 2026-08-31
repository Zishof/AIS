package ais.action.report.format1.sirs.umum;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataSatkerBanbox;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Agama;
import ais.database.model.Ruang;
import ais.database.model.employ.Pendidikan;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Satker;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyDatebox;

/**
 * Penyusun/penyaji laporan untuk laporan data pasien per periode window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyDatebox tanggalMulai}, {@code
 * MyDatebox tanggalSampai}, {@code Combobox rjOrRi}, {@code Combobox poly}, {@code Combobox pendidikan}, {@code
 * Combobox jenisKelamin}, {@code Combobox jenisPasien}, {@code AmbilDataSatkerBanbox satker};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetakStatusPasien()}); operasi domain lain
 * ({@code createRuangPerawatan()}, {@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanDataPasienPerPeriodeWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private Combobox rjOrRi;
	private Combobox poly;

	private Combobox pendidikan;
	private Combobox jenisKelamin;
	private Combobox jenisPasien;
	private AmbilDataSatkerBanbox satker;

	private AmbilDataDokterBanbox dokter;
	private Combobox pekerjaan;
	private Combobox statusPerkawinan;
	private Combobox agama;

	private Textbox mr;
	private Textbox nama;
	private Textbox alamat;
	private Textbox telp;

	private Combobox kelasPerawatan;
	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;
	private AmbilDataTempatTidurBanbox tempatTidur;

	private Center center;

	public LaporanDataPasienPerPeriodeWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanDataPasienPerPeriodeWindow.java:90");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Data Pasien Per Periode Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanDataPasienPerPeriodeWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private Row createRuangPerawatan() throws Exception {

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class,
				Restrictions.ne("id", ConstantValues.kelasNormal.getId()));
		kelasPerawatan.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		ruangPerawatan.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);
				Common.insertCombo(
						kamarPerawatan,
						"nama",
						"keterangan",
						Kamar.class,
						Restrictions.and(
								ruangPerawatan.getSelectedItem() == null ? Restrictions
										.sqlRestriction("1=1") : Restrictions
										.eq("ruang", ruangPerawatan
												.getSelectedItem().getValue()),
								kelasPerawatan.getSelectedItem() == null ? Restrictions
										.sqlRestriction("1=1") : Restrictions
										.eq("kelasPerawatan", kelasPerawatan
												.getSelectedItem().getValue())));
			}

		};

		kelasPerawatan.addEventListener("onChange", myEventListener);
		ruangPerawatan.addEventListener("onChange", myEventListener);
		myEventListener.onEvent(null);

		row.setStyle("border:0px;background: transparent;");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(tempatTidur = new AmbilDataTempatTidurBanbox());
		tempatTidur.setWidth("90%");

		kelasPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (kelasPerawatan
						.getSelectedItem() == null ? null : kelasPerawatan
						.getSelectedItem().getValue());
				tempatTidur.setMyKelasPerawatan(mykelasPerawatan);
				onCetakStatusPasien(arg0);
			}
		});

		ruangPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (ruangPerawatan.getSelectedItem() == null ? null
						: ruangPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyRuang(myRuang);
				onCetakStatusPasien(arg0);
			}
		});

		kamarPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (kamarPerawatan.getSelectedItem() == null ? null
						: kamarPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyKamar(myKamar);
				if (myKamar != null) {

					Common.selectComboItem(kelasPerawatan,
							myKamar.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan",
							Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar.getRuang());
				}
				onCetakStatusPasien(arg0);
			}
		});

		tempatTidur.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TempatTidur myTempatTidur = (TempatTidur) (tempatTidur
						.getAttribute("tempatTidur"));
				if (myTempatTidur != null) {
					Kamar myKamar = myTempatTidur.getKamar();

					Common.selectComboItem(
							kelasPerawatan,
							myKamar == null ? null : myKamar
									.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan",
							Ruang.class);

					Common.selectComboItem(ruangPerawatan,
							myKamar == null ? null : myKamar.getRuang());

					Common.insertCombo(kamarPerawatan, "nama", "keterangan",
							Kamar.class, Restrictions.and(Restrictions.eq(
									"ruang", myTempatTidur.getRuang()),
									Restrictions.eq("kelasPerawatan",
											myTempatTidur.getKelasPerawatan())));
					Common.selectComboItem(kamarPerawatan,
							myTempatTidur.getKamar());
				}

				onCetakStatusPasien(arg0);
			}
		});

		return row;
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);

		Div div = new Div();
		div.setParent(north);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();

		column.setParent(columns);
		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		column.setParent(columns);
		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		column.setParent(columns);
		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai")));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
		row.appendChild(tanggalMulai = new MyDatebox(calendar.getTime()));
		tanggalMulai.setWidth("90%");
		tanggalMulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai")));
		row.appendChild(tanggalSampai = new MyDatebox(new Date()));
		tanggalSampai.setWidth("90%");
		tanggalSampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Rajal/Ranap"));
		row.appendChild(rjOrRi = new Combobox());
		rjOrRi.setWidth("90%");

		Comboitem comboitem = new Comboitem(Pendaftaran.RAWAT_JALAN);
		comboitem.setValue(Pendaftaran.RAWAT_JALAN);
		rjOrRi.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
		comboitem.setValue(Pendaftaran.RAWAT_INAP);
		rjOrRi.appendChild(comboitem);

		rjOrRi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		row.appendChild(poly = new Combobox());
		Common.insertCombo(poly, "nama", "jenis", Poly.class,
				Restrictions.isNull("polyDari"));
		poly.setWidth("90%");
		poly.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendidikan")));
		row.appendChild(pendidikan = new Combobox());
		Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		pendidikan.setWidth("90%");
		pendidikan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Combobox());
		comboitem = new Comboitem("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new Comboitem("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);
		jenisKelamin.setWidth("90%");
		jenisKelamin.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satker")));
		row.appendChild(satker = new AmbilDataSatkerBanbox());
		satker.setWidth("90%");
		satker.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetakStatusPasien(event);

			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter")));
		row.appendChild(dokter = new AmbilDataDokterBanbox());
		dokter.setWidth("90%");
		dokter.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetakStatusPasien(event);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pekerjaan")));
		row.appendChild(pekerjaan = new Combobox());
		pekerjaan = Common.initPekerjaan(pekerjaan);
		pekerjaan.setWidth("90%");
		pekerjaan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Perkawinan")));
		row.appendChild(statusPerkawinan = new Combobox());
		comboitem = new Comboitem("Belum Menikah");
		comboitem.setValue("Belum Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Menikah");
		comboitem.setValue("Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Duda");
		comboitem.setValue("Duda");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Janda");
		comboitem.setValue("Janda");
		statusPerkawinan.appendChild(comboitem);
		statusPerkawinan.setWidth("90%");
		statusPerkawinan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Agama")));
		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", Agama.class);
		agama.setWidth("90%");
		agama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		row.appendChild(mr = new Textbox());
		mr.setWidth("90%");
		mr.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Textbox());
		alamat.setWidth("90%");
		alamat.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp")));
		row.appendChild(telp = new Textbox());
		telp.setWidth("90%");
		telp.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		createRuangPerawatan().setParent(rows);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters()
					throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/laporan_data_pasien_periode"));
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Date myTanggalMulai = tanggalMulai.getValue();
		Date myTanggalSampai = tanggalSampai.getValue();

		String rj = (String) (rjOrRi.getSelectedItem() == null ? "-1" : rjOrRi
				.getSelectedItem().getValue());
		Poly poly = (Poly) (this.poly.getSelectedItem() == null ? null
				: this.poly.getSelectedItem().getValue());

		Pendidikan pendidikan = (Pendidikan) (this.pendidikan.getSelectedItem() == null ? null
				: this.pendidikan.getSelectedItem().getValue());
		String jenisKelamin = (String) (this.jenisKelamin.getSelectedItem() == null ? ""
				: this.jenisKelamin.getSelectedItem().getValue());
		JenisPasien jenisPasien = (JenisPasien) (this.jenisPasien
				.getSelectedItem() == null ? null : this.jenisPasien
				.getSelectedItem().getValue());
		Satker satker = (Satker) this.satker.getAttribute("satker");
		Dokter dokter = (Dokter) this.dokter.getAttribute("dokter");
		String pekerjaan = (String) (this.pekerjaan.getSelectedItem() == null ? ""
				: this.pekerjaan.getSelectedItem().getValue());
		String statusPerkawinan = (String) (this.statusPerkawinan
				.getSelectedItem() == null ? "" : this.statusPerkawinan
				.getSelectedItem().getValue());
		Agama agama = (Agama) (this.agama.getSelectedItem() == null ? null
				: this.agama.getSelectedItem().getValue());

		// private Combobox kelasPerawatan;
		// private Combobox ruangPerawatan;
		// private Combobox kamarPerawatan;
		// private AmbilDataTempatTidurBanbox tempatTidur;

		KelasPerawatan kelasPerawatan = (KelasPerawatan) (this.kelasPerawatan
				.getSelectedItem() == null ? null : this.kelasPerawatan
				.getSelectedItem().getValue());
		Ruang ruangPerawatan = (Ruang) (this.ruangPerawatan.getSelectedItem() == null ? null
				: this.ruangPerawatan.getSelectedItem().getValue());
		Kamar kamarPerawatan = (Kamar) (this.kamarPerawatan.getSelectedItem() == null ? null
				: this.kamarPerawatan.getSelectedItem().getValue());
		TempatTidur tempatTidur = (TempatTidur) this.tempatTidur
				.getAttribute("tempatTidur");

		Map parameters = new HashMap();
		parameters.put("mr", "%" + mr.getValue().trim() + "%");
		parameters.put("nama", "%" + nama.getValue().trim() + "%");
		parameters.put("alamat", "%" + alamat.getValue().trim() + "%");
		parameters.put("telp", "%" + telp.getValue().trim() + "%");

		parameters.put("kelasPerawatan", kelasPerawatan == null || kelasPerawatan.getId() == null ? -1L : kelasPerawatan.getId());
		parameters.put("ruangPerawatan", ruangPerawatan == null || ruangPerawatan.getId() == null ? -1L : ruangPerawatan.getId());
		parameters.put("kamarPerawatan", kamarPerawatan == null || kamarPerawatan.getId() == null ? -1L : kamarPerawatan.getId());
		parameters.put("tempatTidur",
				tempatTidur == null || tempatTidur.getId() == null ? -1L : tempatTidur.getId());

		parameters.put("pendidikan",
				pendidikan == null || pendidikan.getId() == null ? -1L : pendidikan.getId());
		parameters.put("jenisKelamin", jenisKelamin);
		parameters.put("jenisPasien",
				jenisPasien == null || jenisPasien.getId() == null ? -1L : jenisPasien.getId());
		parameters.put("satker", satker == null || satker.getId() == null ? -1L : satker.getId());
		parameters.put("dokter", dokter == null || dokter.getId() == null ? -1L : dokter.getId());
		parameters.put("pekerjaan", pekerjaan);
		parameters.put("statusPerkawinan", statusPerkawinan);
		parameters.put("agama", agama == null || agama.getId() == null ? -1L : agama.getId());

		parameters.put("rj", rj);
		parameters.put("poly", poly == null || poly.getId() == null ? -1L : poly.getId());

		parameters.put("tgl1", myTanggalMulai == null ? "2000-01-01"
				: Common.databaseDateFormat.get().format(myTanggalMulai));

		parameters.put("tgl2", myTanggalSampai == null ? "2000-01-01"
				: Common.databaseDateFormat.get().format(myTanggalSampai));

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {

			Map parameters = generateParameter();
			System.out.println(parameters);
			File file = Report.generateFileReportWithProgress(
					"sirs/laporan_data_pasien_periode", Report.XLS, parameters,
					"sirs/laporan_data_pasien_periode", new Date(), Sessions
							.getCurrent().getWebApp());
			CommonReport.tampilkanReportXLS(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanDataPasienPerPeriodeWindow.java:648");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Data Pasien Per Periode Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
