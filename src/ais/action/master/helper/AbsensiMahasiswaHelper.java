package ais.action.master.helper;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk layar "Informasi Kehadiran Mahasiswa": menampilkan ringkasan KRS (dosen
 * pembimbing akademik, IPS/IPK, SKS semester/kumulatif, tahun akademik/semester) diikuti grid
 * matakuliah yang diambil mahasiswa pada semester tersebut beserta ringkasan kehadiran per matakuliah
 * (jumlah status Masuk/Sakit/Izin/Alpa, dari singkatan M/S/I/A). Setiap baris dapat dibuka untuk
 * melihat rincian kehadiran per pertemuan (tanggal, materi, metode pembelajaran, jenis pertemuan,
 * status dan keterangan kehadiran mahasiswa) lewat {@link #tampilAbsensi}, dan menampilkan tautan
 * cetak laporan absensi rinci PDF per matakuliah.
 *
 * <p>
 * Baris footer grid menampilkan total SKS dan total rekap kehadiran (M/S/I/A) seluruh matakuliah,
 * dihitung dengan mengakumulasi variabel instance saat setiap baris dirender oleh
 * {@link DetailMahasiswaRenderer} — sehingga bergantung pada seluruh baris sudah dirender sebelum
 * footer dibangun (dilakukan lewat {@link Common#createDefaultTimer} setelah {@code loadData}).
 * </p>
 */
public class AbsensiMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private Mahasiswa mahasiswa;
	private Integer semester;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private Integer semesterPendek;

	/**
	 * @param semesterPendek tidak dipakai di badan konstruktor (nilai efektif diambil dari parameter
	 *                        {@code semesterPendek} pada {@link #display}); dipertahankan untuk
	 *                        kompatibilitas pemanggil
	 */
	public AbsensiMahasiswaHelper(Integer semesterPendek) {
	}

	/** Row renderer grid matakuliah: kode/nama/SKS (dengan info konversi ekivalensi bila berbeda), dosen, jadwal, dan ringkasan status kehadiran per matakuliah dengan tautan cetak laporan absensi rinci PDF. Turut mengakumulasi total SKS/M/S/I/A ke variabel instance untuk footer grid. */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
					(Serializable) data);
			if (perkuliahan == null) {
				row.setVisible(false);
				return;
			}
			Matakuliah matakuliah = perkuliahan.getMatakuliah();
			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), false);
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];
			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}
			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					tampilAbsensi(perkuliahan, detail);
				}
			});

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
					: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(row);
			new Label((matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")) + " SKS").setParent(row);
			totalSks += matakuliah.getSks();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, false);

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan);

			TreeMap<String, Long> pertemuanss = perkuliahan.ambilPertemuan();
			List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			pertemuanss.clear();
			pertemuanss = null;

			Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, mahasiswa, null, true, true);

			Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);

			if (statuses != null) {
				totalHadir += !statuses.containsKey("M") ? 0 : statuses.get("M");
				totalSakit += !statuses.containsKey("S") ? 0 : statuses.get("S");
				totalIzin += !statuses.containsKey("I") ? 0 : statuses.get("I");
				totalAlpa += !statuses.containsKey("A") ? 0 : statuses.get("A");
			}

			String abs = statuses == null ? "" : statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
			A a;
			row.appendChild(a = new A(abs));
			a.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {

					Map parameters = ais.common.HashMapGenerator.getRand();
					Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
							: perkuliahan.getJurusan().getKaprodi();
					parameters.put("perkuliahan", perkuliahan.getId());
					parameters.put("tampil_nilai", "1");
					parameters.put("kaprodi",
							kaprodi == null ? "(                                          )" : kaprodi.getNama());
					parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
					parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

					parameters.put("nama_kaprodi",
							kaprodi == null ? "(                                          )" : kaprodi.getNama());
					parameters.put("nip_kaprodi",
							kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

					parameters.put("nidn_kaprodi",
							kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

					Detailperkuliahan detailperkuliahan = mahasiswa.ambilDetailperkuliahan(perkuliahan);
					if (detailperkuliahan != null) {
						Map parametersBaru = new HashMap(parameters);
						List<Map<String, Serializable>> maps = CommonReportHelper.generateParameterMapAbsensiRinci(
								perkuliahan, detailperkuliahan, null, null, true, false);
						parametersBaru.put("maps", maps);
						Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
								ais.ui.util.WaktuUtil.getDate(), Common.locale, null, null);
					}
				}
			});

		}

	}

	/** Merender rincian kehadiran per pertemuan (tanggal, materi, metode, jenis pertemuan, status dan keterangan kehadiran mahasiswa) untuk satu {@link Perkuliahan} ke dalam {@code detail} yang dibuka. */
	@SuppressWarnings({})
	private void tampilAbsensi(final Perkuliahan perkuliahan, final MyDetail detail) {

		Common.clear(detail);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");

		groupbox.setWidth("95%");
		groupbox.setParent(detail);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Metode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pert.");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		// column.setWidth("35%");

		Rows rows = new Rows();
		rows.setParent(mygrid);
		for (Long pertemuanid : perkuliahan.ambilPertemuan().values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				MyDetail detailData = new MyDetail();
				detailData.setParent(row);
				detailData.setOpen(true);
				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {

					@Override
					public void loadData(Object value) {
						tampilAbsensi(perkuliahan, detail);

					}
				}).setParent(detailData);

				new Label(pertemuan.getTanggal() == null ? "" : dateFormat.format(pertemuan.getTanggal()))
						.setParent(row);
				new Label(pertemuan.getTopik()).setParent(row);
				new Label(pertemuan.getMetodePembelajaran()).setParent(row);
				new Label(pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getNama()).setParent(row);

				String kode = pertemuan.retreiveAbsensiKode(mahasiswa.getId());
				String nama = pertemuan.retreiveAbsensiNama(mahasiswa.getId());
				String keterangan = pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId());

				new Label(kode + " (" + nama + ")").setParent(row);
				new Label(keterangan).setParent(row);
			}
		}
	}

	private int totalSks = 0;

	private int totalHadir = 0;
	private int totalSakit = 0;
	private int totalIzin = 0;
	private int totalAlpa = 0;

	/** Mengambil ulang daftar perkuliahan (dan paralelnya) mahasiswa pada semester/semester-pendek saat ini, mereset akumulator total SKS/M/S/I/A, dan me-render ulang grid. Parameter {@code value} tidak dipakai. */
	public void loadData(Object value) {

		try {
			totalSks = 0;

			totalHadir = 0;
			totalSakit = 0;
			totalIzin = 0;
			totalAlpa = 0;

			List<Long> tempPerkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, semesterPendek);

			ListModel strset = new SimpleListModel(tempPerkuliahans);
			grid.setRowRenderer(new DetailMahasiswaRenderer());
			grid.setModelCheckMobile(strset);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Membangun seluruh UI layar informasi kehadiran (ringkasan KRS, grid matakuliah dengan rekap
	 * kehadiran, footer total) di dalam {@code component} untuk kombinasi mahasiswa/semester/tahapan
	 * yang diberikan, lalu memuat data awal.
	 *
	 * @param mahasiswa       mahasiswa yang kehadirannya ditampilkan
	 * @param tahunAjaran     tidak dipakai langsung di badan method (tahun akademik diambil dari
	 *                        sinkronisasi KRS)
	 * @param semester        nomor semester yang ditampilkan
	 * @param tahapan         tahapan KRS; bila {@code -1}, grid disembunyikan
	 * @param component       container ZK yang akan diisi
	 * @param semesterPendek  status semester pendek terkait, boleh {@code null}
	 * @param window          tidak dipakai langsung di badan method
	 * @param keDatabase      diteruskan ke {@code Common.singkronkanKrsMahasiswa}
	 */
	public void display(Mahasiswa mahasiswa, String tahunAjaran, Integer semester, Integer tahapan, Component component,
			Integer semesterPendek, MyWindow window, boolean keDatabase) {

		this.mahasiswa = mahasiswa;
		this.semester = semester;
		this.semesterPendek = semesterPendek;
		Common.clear(component);

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setWidth("95%");
		groupbox.setParent(component);
		groupbox.appendChild(new Caption("Informasi Kehadiran Mahasiswa"));
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase);

		Row rowUtama = Common.tampilanScroll1(groupbox);

		rowUtama.getGrid().setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			rowUtama.getGrid().setVisible(false);
		}
		rowUtama.getGrid().setHeight("100%");
		rowUtama.getGrid().setWidth("100%");

		rowUtama.appendChild(new MyLabelConfig("Dosen Pembimbing Akademik"));
		Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
		Label dosenPembimbing = new Label(dosenPembimbingAkademik == null ? "Belum memiliki dosen pembimbing akademik"
				: dosenPembimbingAkademik.getNama());
		dosenPembimbing.setParent(rowUtama);

		Row rowUtama1;
		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		} else {
			rowUtama1 = rowUtama;
		}

		rowUtama1.appendChild(new MyLabelConfig("IPS"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIps())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Semester"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("IPK"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIpk())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Kumulatif"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksk())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Keterangan"));
		Html keteranganKrs = new Html();
		ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(keteranganKrs, mahasiswa, krsMahasiswa, false);
		rowUtama1.appendChild(keteranganKrs);

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("Tahun Akademik"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getTahunAkademik()));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Semester"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getSemester() + " / "
				+ (krsMahasiswa.getSemesterPendek() == null
						? (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
						: Common.getBahasaConfig("Semester Pendek"))));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);
		grid.setSclass("dgrid");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari/Waktu/Ruang");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rekap");
		column.setWidth("15%");

		loadData(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Foot foot = new Foot();
				foot.setParent(grid);

				Footer footer = new Footer();
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);
				footer = new Footer("Total");
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(totalSks) + " SKS");
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);

				footer = new Footer("M:" + totalHadir + ", S:" + totalSakit + ", I:" + totalIzin + ", A:" + totalAlpa);
				footer.setParent(foot);
			}
		});
	}

}
