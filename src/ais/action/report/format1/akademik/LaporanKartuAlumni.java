package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Penyusun/penyaji laporan untuk laporan kartu alumni. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Toolbar
 * toolbar}, {@code Paging paging}, {@code Textbox cari}, {@code MyGrid grid}, {@code Map map}, {@code MyDatebox
 * tanggal}, {@code Combobox jurusan}; inisialisasi/lifecycle ({@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); pelaporan/ekspor ({@code onReport()}); operasi domain lain
 * ({@code generateParameter()}, {@code siapkanParemeterGambar()}, {@code siapkanParemeter()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanKartuAlumni extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private Paging paging = new Paging();
	private Textbox cari;

	private MyGrid grid;

	Map<Long, Mahasiswa> map = new java.util.HashMap<Long, Mahasiswa>();

	private MyDatebox tanggal;

	private Combobox jurusan;

	public LaporanKartuAlumni() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kartu Alumni", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
		borderlayout1.setParent(west);

		North north = new North();
		north.setParent(borderlayout1);
		north.setHeight("80px");
		north.setBorder("none");

		MyGrid mygrid = new MyGrid();// grid.setOddRowSclass("non-odd");
		mygrid.setWidth("100%");
		mygrid.setParent(north);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("60px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal : "));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setReadonly(true);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(tanggal);

		MyButtonConfig button = new MyButtonConfig("Tampilkan Kartu");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new Row();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Mhs : "));

		hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(jurusan = new Combobox());
		jurusan.setWidth("60px");
		Common.insertComboDanSemua(jurusan, "nama", Jurusan.class);

		cari = new Textbox();
		cari.setParent(hbox);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		button = new MyButtonConfig("Cari");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		South south1 = new South();
		south1.setParent(borderlayout1);
		south1.setHeight("40px");

		Vbox vbox = new Vbox();
		vbox.setParent(south1);

		paging.setParent(vbox);
		paging.setHeight("30px");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center1);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);
		column = new MyColumnConfig();
		column.setWidth("45px");
		column.setParent(columns);

		column = new MyColumnConfig("Foto");
		column.setWidth("65px");
		column.setParent(columns);

		column = new MyColumnConfig("Kode");
		column.setParent(columns);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "format1/kartu_alumni", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onSearchDefault(null);
	}

	@SuppressWarnings("unchecked")
	protected void onSearchDefault(Object object) {
		Common.initPaging(initCriteria(false), paging);
		List<Mahasiswa> mahasiswa = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Mahasiswa.class);

		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		mahasiswas.addAll(map.values());
		mahasiswas.addAll(mahasiswa);
		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link LaporanKartuAlumni}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanKartuAlumni} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see LaporanKartuAlumni
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			arg0.setAttribute("mahasiswa", mahasiswa);
			checkbox.setChecked(map.keySet().contains(mahasiswa.getId()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						map.put(mahasiswa.getId(), mahasiswa);
					} else {
						map.remove(mahasiswa.getId());
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			new Label(mahasiswa.getNim()).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama()).setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		Criterion criterion = Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE);
		criterion = Restrictions.or(criterion, Restrictions.ilike("email", cari.getValue().trim(), MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("nim", cari.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(map.isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.not(Restrictions.in("id", map.keySet())))
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion);
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		List list = new ArrayList();
		List<Row> row = grid.getRows().getChildren();
		for (Row ro : row) {
			MyCheckboxConfig checkbox = (MyCheckboxConfig) ro.getAttribute("checkbox");
			if (checkbox != null && checkbox.isChecked()) {
				Mahasiswa mahasiswa = (Mahasiswa) ro.getAttribute("mahasiswa");
				if (mahasiswa.getAktif()) {
					list.add(siapkanParemeter(mahasiswa));
				}
			}
		}

//		List list = new ArrayList();
//		for (Mahasiswa mahasiswa : map.values()) {
//			if (mahasiswa.getAktif()) {
//				list.add(siapkanParemeter(mahasiswa));
//			}
//		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters = siapkanParemeterGambar(parameters, null);
		parameters.put("tanggal_kartu", tanggal.getValue());
		parameters.put("maps", list);
		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map siapkanParemeterGambar(Map parameters, Jurusan jurusan) throws Exception {

		File fileStempel = new File(Common.REAL_PATH + "/img/stempel.png");
		LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN_STR + (jurusan == null ? "" : jurusan.getId()));

		if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
			fileStempel = lainMahasiswa.ambilFile();
			System.out.println("fileStempel = " + fileStempel);
		}

		File fileTtd = new File(Common.REAL_PATH + "/img/tandatangan.png");

		lainMahasiswa = LampiranLain.ambil(LampiranLain.TANDA_TANGAN_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_ALUMNI_PERPUSTAKAAN_STR + (jurusan == null ? "" : jurusan.getId()));

		if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
			fileTtd = lainMahasiswa.ambilFile();
			System.out.println("fileTtd = " + fileTtd);
		}

		File fileBg1 = new File(Common.REAL_PATH + "/img/bg2.png");

		lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN_STR + (jurusan == null ? "" : jurusan.getId()));

		if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
			fileBg1 = lainMahasiswa.ambilFile();
			System.out.println("fileBg1 = " + fileBg1);
		}

		File fileBg2 = new File(Common.REAL_PATH + "/img/bg1.png");

		lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR + (jurusan == null ? "" : jurusan.getId()));

		if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
			fileBg2 = lainMahasiswa.ambilFile();
			System.out.println("fileBg2 = " + fileBg2);
		}

		String defaultValue = "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini harus dibawa sebagai identitas alumni.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Mahasiswa harus mematuhi semua tata tertib .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
				+ "website : " + Common.getRequestHostWithProtocol();

		String tataTertib = Common.getKonfigurasi("tata_tertib_kartu_alumni", defaultValue).getNilai();

		parameters.put("tataTertib", tataTertib);
		parameters.put("fileStempel", fileStempel.getAbsolutePath());
		parameters.put("fileTtd", fileTtd.getAbsolutePath());
		parameters.put("fileBg1", fileBg1.getAbsolutePath());
		parameters.put("fileBg2", fileBg2.getAbsolutePath());

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map siapkanParemeter(Mahasiswa mahasiswa) throws Exception {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + mahasiswa.getId() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(mahasiswa.getNim());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", mahasiswa.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("warna", mahasiswa.getJurusan().getFakultas().getWarna());
		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "mahasiswa");
		parameters.put("kode", mahasiswa.getNim());
		parameters.put("nama", mahasiswa.getNama());
		parameters.put("angkatan", mahasiswa.getTahunangkatan());
		parameters.put("alamat", mahasiswa.getAlamat());
		parameters.put("tempatlahir", mahasiswa.getTempatlahir());
		parameters.put("tanggallahir", mahasiswa.getTanggallahir());
		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());
		parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("nama_perguruan_tinggi", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
				: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
		parameters.put("ttl", mahasiswa.getTempatlahir().toUpperCase() + " / "
				+ (mahasiswa.getTanggallahir() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggallahir())));

		parameters.put("telp", mahasiswa.getTelp());
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mahasiswa.getTanggalMasuk());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 5);
		parameters.put("tanggal_kadaluarsa", calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mahasiswa.getTanggalMasuk());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put("tanggal_kadaluarsa_1", calendar.getTime());

		mahasiswa.putPhotoLulus(parameters);

		if (Common.bolehKonfigurasi("upload_file_di_konfigurasi_tiap_jurusan_bisa_beda", Konfigurasi.TIDAK_AKTIF)) {
			Jurusan selectedJurusan = mahasiswa.getJurusan();
			parameters = siapkanParemeterGambar(parameters, selectedJurusan);
		}

		if (Common.bolehKonfigurasi("apakah_tampilan_cr_code")) {

			String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder
					.encode(Common.desEncrypter.get().encrypt(mahasiswa.getNim() + "abcdefghijklmnopqrstuvwxyz"), "UTF-8");

			File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + mahasiswa.getId() + ".png");

			BarcodeCommon.generateCRCode(code, myfilebarcode1);
			parameters.put("cr_code", myfilebarcode1.getAbsolutePath());
		}
		parameters.put("qr_code", Common.desEncrypter.get().encrypt(Mahasiswa.class.getName() + ":" + mahasiswa.getId()));
		String code = parameters.get("qr_code")+"";
		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					String namaFile = "format1/kartu_alumni";

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), namaFile,
							ais.ui.util.WaktuUtil.getDate(), null, toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kartu Alumni", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
