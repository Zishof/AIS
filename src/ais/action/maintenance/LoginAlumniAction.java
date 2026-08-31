package ais.action.maintenance;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.BiodataMahasiswaAction;
import ais.action.master.ChecklistPenilaianUmumOlehPesertaAction;
import ais.action.master.helper.ParameterTambahanAlumniListener;
import ais.action.report.CommonReportHelper;
import ais.common.ChecklistPenilaianHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk login alumni. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox namaLengkap}, {@code Combobox
 * tahun}, {@code Combobox bulan}, {@code Combobox tanggal}, {@code MyButtonConfig cetak}, {@code MyButtonConfig
 * cari}, {@code MyWindow window}, {@code Row tanggalLahirData}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initBiodata()}); mutasi data ({@code
 * onSave()}, {@code onReset()}); operasi domain lain ({@code onLogin()}, {@code onAddExternal()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class LoginAlumniAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox namaLengkap;

	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	MyButtonConfig cetak;
	MyButtonConfig cari;
	private MyWindow window;
	private Row tanggalLahirData;
	private Mahasiswa mahasiswa;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	private Tbmuser tbmuser;

	private Boolean digunakanUntukPenggunaAlumni = false;
	private String q = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.initTemp();
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
		digunakanUntukPenggunaAlumni = execution.getParameter("digunakanUntukPenggunaAlumni") != null
				&& execution.getParameter("digunakanUntukPenggunaAlumni").equals("true");
		if (digunakanUntukPenggunaAlumni) {
			session.setAttribute("digunakanUntukPenggunaAlumni", digunakanUntukPenggunaAlumni);
		}

		if (execution.getParameter("digunakanUntukPenggunaAlumni") != null
				&& execution.getParameter("digunakanUntukPenggunaAlumni").equalsIgnoreCase("false")) {
			session.setAttribute("digunakanUntukPenggunaAlumni", false);
		}

		if (session.getAttribute("digunakanUntukPenggunaAlumni") != null) {
			digunakanUntukPenggunaAlumni = (Boolean) session.getAttribute("digunakanUntukPenggunaAlumni");
		}

		tanggalLahirData.setVisible(Common.bolehKonfigurasi("tanggal_lahir_login_alumni"));

		q = execution.getParameter("q");

		if (execution.getParameter("digunakanUntukPenggunaAlumni") != null) {
			execution.sendRedirect("loginAlumni?q=" + Common.randLong());
			return;
		}

		tbmuser = Common.getCurrentFromSpringUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswa = tbmuser.getMahasiswa();
			System.out.println("Match!");
			Common.clear(window);

			LoginAlumniAction.this.mahasiswa = mahasiswa;
			init(mahasiswa, window);
		} else {

			int tahunLoginCalonSiswa = 50;
			try {
				tahunLoginCalonSiswa = Integer
						.parseInt(Common.getKonfigurasi("tahun_login_alumni", "50").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/LoginAlumniAction.java:147");
				// TODO: handle exception
			}

			MyComboitemConfig comboitem;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
					- tahunLoginCalonSiswa; i < ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 1; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tahun.appendChild(comboitem);
			}

			for (int i = 1; i <= 31; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tanggal.appendChild(comboitem);
			}

			Common.createComboBulan(bulan);
		}
		Common.initLaguage();
	        FilterLanjutHelper.setup(comp);
}

	public void onLogin(Event event) throws Exception {
		if (namaLengkap.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, kolom Nama Lengkap / Email / NIM belum diisi. Langkah yang dapat dilakukan: (1) isi kolom pencarian dengan Nama Lengkap, alamat Email, atau NIM alumni; (2) pastikan data yang dimasukkan sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tahun.getSelectedItem() == null || tahun.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun lahir belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun lahir dari dropdown yang tersedia; (2) pastikan tahun sesuai dengan data alumni yang terdaftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (bulan.getSelectedItem() == null || bulan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Bulan lahir belum dipilih. Langkah yang dapat dilakukan: (1) pilih bulan lahir dari dropdown yang tersedia; (2) pastikan bulan sesuai dengan data alumni yang terdaftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tanggal.getSelectedItem() == null || tanggal.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal lahir belum dipilih. Langkah yang dapat dilakukan: (1) pilih tanggal lahir dari dropdown yang tersedia; (2) pastikan tanggal sesuai dengan data alumni yang terdaftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, (Integer) tahun.getSelectedItem().getValue());
		calendar.set(Calendar.MONTH, (Integer) bulan.getSelectedItem().getValue());
		calendar.set(Calendar.DATE, (Integer) tanggal.getSelectedItem().getValue());

		mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
				session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("statusKeluar.id", 1L))

						.add(tanggalLahirData.isVisible() ? Restrictions.eq("tanggallahir", calendar.getTime())
								: Restrictions.sqlRestriction("true"))
						.setMaxResults(1)
						.add(Restrictions.or(Restrictions.ilike("nim", namaLengkap.getValue().trim(), MatchMode.EXACT),
								Restrictions.or(
										Restrictions.ilike("nama", namaLengkap.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("email", namaLengkap.getValue().trim(), MatchMode.EXACT)))),
				Mahasiswa.class);

		if (mahasiswa == null) {
			PesanFormalHelper.tampilkanGagal("proses masuk (login) Alumni",
					"Data mahasiswa dengan nama, email, atau NIM \"" + namaLengkap.getValue()
							+ "\" tidak ditemukan pada basis data alumni yang berstatus aktif/lulus. Kemungkinan penyebabnya "
							+ "adalah salah ketik pada isian tersebut, atau data mahasiswa yang bersangkutan memang belum "
							+ "terdaftar/berstatus tidak aktif pada sistem ini.",
					new String[] {
							"Periksa kembali ejaan nama lengkap, alamat email, atau NIM yang Bapak/Ibu masukkan.",
							"Pastikan Bapak/Ibu memasukkan data sesuai dengan yang tercatat resmi pada saat masih berstatus mahasiswa.",
							"Jika data memang sudah benar namun tetap tidak ditemukan, hubungi bagian akademik/alumni kampus." });
			return;
		}

		if (mahasiswa != null) {
			if (tanggalLahirData.isVisible()) {
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggallahir());
				int thn = calendar.get(Calendar.YEAR);
				int bln = calendar.get(Calendar.MONTH);
				int tgl = calendar.get(Calendar.DATE);
				boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
						: tahun.getSelectedItem().getValue().equals(thn))
						&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
						&& (tanggal.getSelectedItem() == null ? false
								: tanggal.getSelectedItem().getValue().equals(tgl));

				if (kondisiTglLahir) {
					System.out.println("Match!");
					Common.clear(window);

					LoginAlumniAction.this.mahasiswa = mahasiswa;
					init(mahasiswa, window);
				} else {
					PesanFormalHelper.tampilkanGagal("verifikasi tanggal lahir pada proses masuk (login) Alumni",
							"Tanggal, bulan, dan tahun lahir yang Bapak/Ibu pilih tidak sesuai dengan data tanggal lahir "
									+ "yang tercatat pada data mahasiswa dengan nama/email/NIM tersebut.",
							new String[] {
									"Periksa kembali tanggal, bulan, dan tahun lahir yang Bapak/Ibu pilih pada formulir.",
									"Pastikan nama lengkap/email/NIM yang dimasukkan sudah benar-benar sesuai dengan identitas Bapak/Ibu.",
									"Jika tanggal lahir yang dimasukkan sudah benar namun tetap gagal, hubungi bagian akademik/alumni kampus untuk memeriksa data tersebut." });
				}
			} else {
				LoginAlumniAction.this.mahasiswa = mahasiswa;
				init(mahasiswa, window);
			}
		} else {
			PesanFormalHelper.tampilkanGagal("proses masuk (login) Alumni",
					"Data mahasiswa yang sesuai dengan isian Bapak/Ibu tidak dapat ditemukan pada sistem.",
					new String[] {
							"Periksa kembali seluruh isian pada formulir login (nama/email/NIM, tahun, bulan, dan tanggal).",
							"Jika seluruh data yang dimasukkan sudah benar namun tetap gagal, hubungi bagian akademik/alumni kampus." });
		}
	}

	private Row rowParameterTambahan;

	private ParameterTambahanAlumniListener parameterTambahanAlumniListener;

	private EventListener eventListener = null;

	// private Row rowParameterTambahanAngket;

	// private MyTabConfig tabAlumni = null;

	// private Tabpanel tabpanelAlumni;

	public static void onAddExternal(EventListener eventListener, Mahasiswa mahasiswa) throws Exception {
		LoginAlumniAction skripsiAction = new LoginAlumniAction();
		skripsiAction.eventListener = eventListener;
		skripsiAction.window = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.window);
		skripsiAction.window.setHeight("95%");
		skripsiAction.window.setWidth("90%");
		skripsiAction.window.setTitle("Tracer Study");
		skripsiAction.window.setClosable(true);

		skripsiAction.init(mahasiswa, skripsiAction.window);

		skripsiAction.window.setVisible(true);
		skripsiAction.window.setClosable(true);
		skripsiAction.window.onModal();

	}

	@SuppressWarnings({ "deprecation" })
	private void init(final Mahasiswa biodata, final MyWindow window) throws Exception {

		this.mahasiswa = biodata;
		this.window = window;

		final BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center1 = new Center();
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(center1);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rows = new Rows();
		rows.setParent(grid);

		if (q != null) {

			String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
			String image = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia("logo_perguruanTinggi_");
			String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getAlamat1();
			String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getTelepon();

			String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia((javax.servlet.http.HttpServletRequest) execution.getNativeRequest(),
							"banner_perguruanTinggi_");
			if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
				background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
			}

			Row row = new Row();
			row.setValign("top");
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);

			if (Common.isMobile()) {

				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				vbox.setPack("center");
				vbox.setAlign("center");
				row.appendChild(vbox);

				vbox.setStyle("background:url('" + background_PerguruanTinggi + ""
						+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

				Image imgLogo;
				vbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
				imgLogo.setHeight("80px");

				Label namaSeleksi = new Label(
						judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
								: judul);
				vbox.appendChild(namaSeleksi);

				Label namaSekolah = new Label(
						Common.getKonfigurasi("label_alumni_kampus", "Informasi dan Tracer Study Alumni").getNilai());
				vbox.appendChild(namaSekolah);

				Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
						? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
								+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
						: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
				vbox.appendChild(alamatSekolah);

				namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style_mobile",
						"font-size: large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());
				namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style_mobile",
						"font-size: 12px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());
				alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style_mobile",
						"font-size: 9px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());

			} else {

				Hbox hbox = new Hbox();
				hbox.appendChild(new Space());
				hbox.appendChild(new Space());
				hbox.setStyle("background:url('" + background_PerguruanTinggi + ""
						+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
				Image imgLogo;
				hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
				hbox.appendChild(new Space());
//				hbox.appendChild(new Space());
				hbox.setWidth("100%");
				row.appendChild(hbox);

				Vbox vbox = new Vbox();

				vbox.setWidth("100%");
				vbox.setPack("center");
				hbox.appendChild(vbox);

				imgLogo.setHeight("80px");

				Label namaSeleksi = new Label(
						judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
								: judul);
				vbox.appendChild(namaSeleksi);

				Label namaSekolah = new Label(
						Common.getKonfigurasi("label_alumni_kampus", "Informasi dan Tracer Study Alumni").getNilai());
				vbox.appendChild(namaSekolah);

				Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
						? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
								+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
						: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
				vbox.appendChild(alamatSekolah);

				namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style",
						"font-size: xx-large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());
				namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style",
						"font-size: medium;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());
				alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style",
						"font-size: 11px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
						.getNilai());

			}

		}

		Row row = new Row();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelStyled("Data Alumni"));

		row = new Row();
		row.setParent(rows);

		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.setParent(rows);
		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(row);
		final Image foto;
		vbox.appendChild(foto = new Image("/img/administrator-icon_default.png"));
		foto.setWidth("128px");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

						if (media instanceof org.zkoss.image.AImage) {

							if (Common.bolehKonfigurasi("file_gambar_biodata_harus_berformat_jpg")
									&& (!uploadEvent.getMedia().getContentType().equals("image/jpeg"))) {
								MyMessageboxConfig.show("Mohon maaf, file gambar yang diupload tidak ber-format JPG atau JPEG. Langkah yang dapat dilakukan: (1) konversi foto ke format JPG atau JPEG terlebih dahulu; (2) pastikan ekstensi file adalah .jpg atau .jpeg; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							} else {

								Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
								FotoMahasiswa fotoMahasiswa = (FotoMahasiswa) streamingSession
										.createCriteria(FotoMahasiswa.class).addOrder(Order.desc("id"))
										.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1)
										.uniqueResult();
								if (fotoMahasiswa != null) {
									streamingSession.getTransaction().begin();
									streamingSession.delete(fotoMahasiswa);
									streamingSession.getTransaction().commit();
								}

								fotoMahasiswa = new FotoMahasiswa();
								fotoMahasiswa.setNama(uploadEvent.getMedia().getName());
								fotoMahasiswa.setKeterangan(uploadEvent.getMedia().getContentType());
								fotoMahasiswa.setMahasiswa(mahasiswa.getId());

								fotoMahasiswa.setFoto(new javax.sql.rowset.serial.SerialBlob(uploadEvent.getMedia().getByteData()));

								streamingSession.getTransaction().begin();
								streamingSession.save(fotoMahasiswa);
								streamingSession.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
							}
						} else {
							MyMessageboxConfig.show("Mohon maaf, file yang diupload bukan berupa gambar. Langkah yang dapat dilakukan: (1) pilih file berformat gambar (JPG, PNG, atau JPEG); (2) pastikan file bukan dokumen teks atau PDF; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}

					} else {
						if (mahasiswa.getId() != null) {
							foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
						}
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					PesanFormalHelper.tampilkanGagalException("pengunggahan foto Alumni/Mahasiswa", e,
							new String[] {
									"Periksa kembali berkas foto yang diunggah (pastikan berformat gambar dan ukurannya tidak terlalu besar).",
									"Coba unggah ulang foto tersebut beberapa saat lagi.",
									"Jika kendala berulang, lanjutkan pengisian data terlebih dahulu tanpa foto, lalu unggah foto di lain waktu." });
				}

			}
		};

		eventListener.onEvent(null);

		row = new Row();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(new MyLabelBolder(mahasiswa.getNim()));

		row = new Row();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new MyLabelBolder(mahasiswa.getNama()));

		row = new Row();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new MyLabelBolder(mahasiswa.getJurusan().getFakultas().getNama()));

		row = new Row();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new MyLabelBolder(mahasiswa.getJurusan().getNama()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lulus"));
		row.appendChild(new MyLabelBolder(
				mahasiswa.getTanggalLulus() == null ? "" : Common.dateFormat6.get().format(mahasiswa.getTanggalLulus())));

		row = new Row();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Toolbarbutton aa;
		row.appendChild(aa = new MyToolbarbuttonConfig("Data Biodata Lengkap", "/img/education.png"));
		aa.setStyle("font-size:9px;");
		aa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyWindow window = new MyWindow("Biodata Lengkap", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("90%");
				window.setWidth("900px");

				Borderlayout borderlayout = new Borderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				center.appendChild(initBiodata(mahasiswa));
				window.onModal();
			}
		});

		rowParameterTambahan = new Row();
		rowParameterTambahan.setStyle("border:0px;background: transparent;");

		List<Row> parameterRows = new ArrayList<Row>();

		parameterTambahanAlumniListener = new ParameterTambahanAlumniListener(biodataMahasiswa, parameterRows,
				lampiranLains, rows, digunakanUntukPenggunaAlumni);

		boolean visible = parameterTambahanAlumniListener.check();

		rowParameterTambahan.setVisible(visible);
		rowParameterTambahan.setParent(rows);

		parameterTambahanAlumniListener.onEvent(null);

		if (!digunakanUntukPenggunaAlumni) {
			row = new Row();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);

			Tbmuser tbmuser = Common.getCurrentUser() == null ? new Tbmuser() : Common.getCurrentUser();
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			if (this.mahasiswa != null) {
				tbmuser = new Tbmuser();
				tbmuser.setMahasiswa(this.mahasiswa);
				mahasiswa = this.mahasiswa;
			}
			List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser);
			for (Object[] obj : datas) {
				String tahunAkademik = (String) obj[0];
				String semester = (String) obj[1];

				ChecklistPenilaianUmumOlehPesertaAction checklistPenilaianUmumOlehPesertaAction = new ChecklistPenilaianUmumOlehPesertaAction();
				checklistPenilaianUmumOlehPesertaAction
						.initData(mahasiswa, dosen, tbmuser, tahunAkademik, semester, null, null, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, false, null, false).setParent(row);
			}

		}

		Toolbar toolbar = new Toolbar();
		toolbar.setAlign("center");
		toolbar.setHeight("30px");

		if (q != null) {
			South south = new South();
			south.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setHeight("30px");
			toolbar.setParent(south);
		} else {
			row = new Row();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			toolbar.setParent(row);
		}

		MyButtonConfig cetak = new MyButtonConfig("  SIMPAN DATA  ", "/img/save.gif");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(null)) {

					Common.refreshSaveOrUpdate(biodataMahasiswa);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (biodataMahasiswaAction != null) {
								try {
									biodataMahasiswaAction.onSave(mahasiswa, true, null,
											parameterTambahanAlumniListener);
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("penyimpanan sebagian data biodata Alumni/Mahasiswa", e,
					new String[] {
							"Data pokok Alumni yang telah Bapak/Ibu isi tetap tersimpan; hanya sebagian data biodata tambahan yang gagal disimpan.",
							"Silakan buka kembali menu pengisian data alumni untuk melengkapi/menyimpan ulang bagian data yang belum tersimpan.",
							"Jika kendala berulang, hubungi Administrator Sistem atau bagian akademik/alumni kampus." });
		}
							}

							MyMessageboxConfig.show(
									"Terima Kasih atas waktunya,\n\n\nPengisian data alumni atau lulusan perguruan tinggi telah selesai.. Klik OK untuk melihat hasil pengisian data..",
									"Terima Kasih", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final File bio = CommonReportHelper
													.onCetakBiodataMahasiswa(biodataMahasiswa);
											CommonEmail.infoLengkapMahasiswa(biodataMahasiswa, bio);

											if (LoginAlumniAction.this.eventListener != null) {
												LoginAlumniAction.this.eventListener.onEvent(arg0);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													ExecutionsCtrl.sendRedirect(Common.getRequestHostWithProtocol()
															+ "/report/" + bio.getName());
												}
											});
										}

									});

						}

					});
				}
			}
		});
		cetak.setParent(toolbar);

	}

	public boolean onSave(Event event) throws Exception {

		Tbmuser tbmuser = new Tbmuser(mahasiswa);
		if (Common.getCurrentUser() == null) {
			Sessions.getCurrent().setAttribute("usersTemp", tbmuser);
		}

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
		parameterTambahanAlumniListener.onSave(biodataMahasiswa);

		if (!digunakanUntukPenggunaAlumni) {
			List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser);
			for (Object[] obj : datas) {
				final String tahunakademik = (String) obj[0];
				final String semester = (String) obj[1];
				if (ChecklistPenilaianHelper.checkStatusChecklistUmum(tahunakademik, semester, tbmuser)) {

					MyMessageboxConfig.showFormatCb(
							"Penilaian Angket Alumni untuk tahun akademik {V1} semester {V2} sebagian atau seluruhnya belum Bapak/Ibu lakukan. Sebelum dapat melanjutkan akses aplikasi akademik ini, mohon Bapak/Ibu berkenan mengisi Angket Alumni berikut terlebih dahulu.\n\nSilakan tekan tombol OK untuk melanjutkan, kemudian tekan tombol \"Lakukan Penilaian\" untuk memulai pengisian.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, tahunakademik, semester);
					return false;
				}
			}
		}

		if (!ParameterTambahanAlumniListener.validate(biodataMahasiswa, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		}, true, digunakanUntukPenggunaAlumni)) {
			return false;
		}

		return true;
	}

	public void onReset() {
		namaLengkap.setValue("");
		tahun.setSelectedItem(null);
		bulan.setSelectedItem(null);
		tanggal.setSelectedItem(null);
	}

	private BiodataMahasiswaAction biodataMahasiswaAction = new BiodataMahasiswaAction();

	private Borderlayout initBiodata(Mahasiswa mahasiswa) throws Exception {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return new ais.ui.util.MyBorderlayout();
		}
		biodataMahasiswaAction.setTampilFotoBiodata(false);
		Borderlayout borderlayout = biodataMahasiswaAction.initMain(mahasiswa);

		return borderlayout;
	}
}
