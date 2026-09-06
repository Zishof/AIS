package ais.action.master.helper;

import java.io.File;
import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk menuliskan catatan dosen Pembimbing Akademik terhadap satu {@link KrsMahasiswa}
 * (satu baris KRS mahasiswa pada semester/tahapan/semester-pendek tertentu) — dua catatan terpisah
 * disediakan: catatan Rencana Studi (tampil di halaman KRS) dan catatan Penilaian/KHS (tampil di
 * halaman KHS), keduanya dapat diwajibkan memiliki panjang minimal karakter lewat konfigurasi
 * {@code minimal_catatan_krs}/{@code minimal_catatan_khs}. Jendela juga menyediakan unggah lampiran
 * pendukung catatan.
 *
 * <p>
 * Setelah catatan tersimpan, bila field catatan KRS tidak kosong, sebuah email/notifikasi otomatis
 * dikirim (asinkron lewat {@code Common#createDefaultTimer}) ke mahasiswa, dosen PA-nya, dan
 * pengguna yang menulis catatan — dilampiri berkas PDF KRS hasil cetak
 * ({@code Report#generateFileReport}) — lewat {@link MailSender#sendMailLampiran}.
 * </p>
 */
public class CatatanHelper {

	/** Mahasiswa pemilik KRS yang diberi catatan; sumber NIM/nama/email untuk notifikasi. */
	private Mahasiswa mahasiswa;
	/** Kotak isian catatan Rencana Studi (KRS), di-autowire ke {@link org.zkoss.zul.Textbox} saat {@link #display(EventListener)} dipanggil. */
	private Textbox catatan;
	/** Kotak isian catatan Penilaian (KHS), di-autowire ke {@link org.zkoss.zul.Textbox} saat {@link #display(EventListener)} dipanggil. */
	private Textbox catatanKhs;
	/** Nomor semester KRS yang sedang diberi catatan. */
	private Integer semester;
	/** Dosen Pembimbing Akademik mahasiswa; salah satu penerima notifikasi email catatan KRS. */
	private Dosen dosenpa;
	/** Tahun akademik KRS yang sedang diberi catatan. */
	private String tahunAkademik;

	/** Nomor tahapan kurikulum (bila fitur tahapan aktif); boleh {@code null}. */
	private Integer tahapan;
	/** Penanda konteks Semester Pendek/Antara; boleh {@code null} bila bukan semester pendek. */
	private Integer semesterPendek;
	/** Penanda apakah KRS ini bersifat remedial, dipakai saat mencetak lampiran PDF KRS. */
	private boolean remedial;
	/** Panjang minimal karakter catatan KRS, dibaca dari konfigurasi {@code minimal_catatan_krs} (0 = tidak wajib). */
	private int minimalCatatanKrs = 0;
	/** Panjang minimal karakter catatan KHS, dibaca dari konfigurasi {@code minimal_catatan_khs} (0 = tidak wajib). */
	private int minimalCatatanKhs = 0;

	/**
	 * @param mahasiswa      mahasiswa pemilik KRS yang akan diberi catatan
	 * @param semester       nomor semester KRS
	 * @param tahapan        nomor tahapan (bila fitur tahapan kurikulum aktif), boleh {@code null}
	 * @param dosenpa        dosen Pembimbing Akademik, dipakai sebagai salah satu penerima notifikasi email
	 * @param tahunAkademik  tahun akademik KRS
	 * @param semesterPendek penanda konteks Semester Pendek/Antara, boleh {@code null}
	 * @param remedial       penanda apakah KRS ini remedial (dipakai saat mencetak lampiran PDF KRS)
	 */
	public CatatanHelper(Mahasiswa mahasiswa, Integer semester, final Integer tahapan, Dosen dosenpa,
			String tahunAkademik, Integer semesterPendek, boolean remedial) {
		this.mahasiswa = mahasiswa;
		this.semester = semester;
		this.semesterPendek = semesterPendek;

		this.tahapan = tahapan;
		this.dosenpa = dosenpa;
		this.tahunAkademik = tahunAkademik;
		this.remedial = remedial;
	}

	/**
	 * Membangun jendela input catatan KRS/KHS. Setelah disimpan (dengan validasi panjang minimal
	 * karakter bila dikonfigurasi), {@code eventListener} dipanggil segera, lalu — bila catatan KRS
	 * tidak kosong — email notifikasi terlampir PDF KRS dikirim secara asinkron ke mahasiswa/dosen
	 * PA/penulis catatan (lihat dokumentasi kelas).
	 *
	 * @param eventListener dipanggil segera setelah catatan tersimpan (sebelum proses kirim email),
	 *                      dengan data event berupa {@link KrsMahasiswa} yang diperbarui
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau akses database
	 */
	public void display(final EventListener eventListener) throws Exception {
		KrsMahasiswa krsMahasiswa = ambilKrsMahasiswaUntukCatatan();
		final MyWindow window = new MyWindow("Masukkan catatan mahasiswa untuk semester " + semester, "normal", true);
		window.setHeight("300px");
		window.setWidth("850px");

		minimalCatatanKrs = Integer.parseInt(Common.getKonfigurasi("minimal_catatan_krs", "0").getNilai());
		minimalCatatanKhs = Integer.parseInt(Common.getKonfigurasi("minimal_catatan_khs", "0").getNilai());

		Component component = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		window.setParent(component);

		window.setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Rencana Studi (KRS)"));
		catatan = new Textbox(krsMahasiswa.getCatatan());
		row.appendChild(catatan);
		catatan.setRows(2);
		row.setValign("top");
		catatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Penilaian (KHS)"));
		catatanKhs = new Textbox(krsMahasiswa.getCatatanKhs());
		row.appendChild(catatanKhs);
		catatanKhs.setRows(2);
		row.setValign("top");
		catatanKhs.setWidth("90%");

		Common.initKeterangan(rows,
				"Berisi catatan atau saran untuk mahasiswa untuk semester dan tahun akademik berjalan."
						+ (minimalCatatanKrs > 0
								? " Untuk catatan KRS minimal harus terdapat " + minimalCatatanKrs + " karakter."
								: "")
						+ (minimalCatatanKhs > 0
								? " Untuk catatan KHS minimal harus terdapat " + minimalCatatanKhs + " karakter."
								: ""));

		Common.initKeterangan(rows,
				"Catatan ini akan tampil di halaman KRS dan KHS Mahasiswa, baik di halaman mahasiswa yang bersangkutan maupun dosen pembimbing akademik");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Vbox vbox1 = new Vbox();
		vbox1.setParent(row);
		Hbox hbox1 = new Hbox();

		LampiranLain.createDownloadUploadFileLain(hbox1, krsMahasiswa.getId(), "KRS_DISETUJUI", "Catatan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true);

		hbox1.setParent(vbox1);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (catatan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Masukkan catatan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (!catatan.getValue().trim().isEmpty() && catatan.getValue().trim().length() < minimalCatatanKrs) {
					MyMessageboxConfig.show("Catatan KRS minimal harus terdapat " + minimalCatatanKrs + " karakter",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (!catatanKhs.getValue().trim().isEmpty()
						&& catatanKhs.getValue().trim().length() < minimalCatatanKhs) {
					MyMessageboxConfig.show("Catatan KHS minimal harus terdapat " + minimalCatatanKhs + " karakter",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				final KrsMahasiswa krsMahasiswa = ambilKrsMahasiswaUntukCatatan();
				krsMahasiswa.setCatatan(catatan.getValue());
				krsMahasiswa.setCatatanKhs(catatanKhs.getValue());
				Common.refreshUpdate(krsMahasiswa);

				window.detach();
				eventListener.onEvent(new Event("", window, krsMahasiswa));

				if (!catatan.getValue().trim().isEmpty()) {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							Tbmuser tbmuser = Common.getCurrentUser();
							String emailUser = "";

							JSONArray userIds = new JSONArray();
							userIds.put(tbmuser.getUserId());

							if (dosenpa != null && dosenpa.getEmail() != null
									&& Common.isValidEmailAddress(dosenpa.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? dosenpa.getEmail().trim()
										: "," + dosenpa.getEmail().trim();
							}

							List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("dosen", dosenpa))
									.setProjection(Projections.groupProperty("userId")).list();
							for (String email : emails) {
								userIds.put(email);
							}

							if (mahasiswa != null && mahasiswa.getEmail() != null
									&& Common.isValidEmailAddress(mahasiswa.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? mahasiswa.getEmail().trim()
										: "," + mahasiswa.getEmail().trim();
							}

							userIds.put(mahasiswa.getNim());

							if (tbmuser != null && tbmuser.getEmail() != null
									&& Common.isValidEmailAddress(tbmuser.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? tbmuser.getEmail().trim()
										: "," + tbmuser.getEmail().trim();
							}

							// System.out.println("emailUser = " + emailUser);

							if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
								String info = "Mahasiswa: " + mahasiswa.getNim() + " " + mahasiswa.getNama()
										+ ", semester: " + semester + ", Tahun Akademik: " + tahunAkademik;
								String subject = "Informasi KRS => " + info;

								File file = Report.generateFileReport(Report.PDF,
										CommonReportHelper.generateParameterKrs(mahasiswa, semester, tahapan,
												semesterPendek, remedial, false, false),
										"Cetak_KRS_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), Common.locale);

								String body = "Anda mendapatkan informasi dari catatan dari " + tbmuser.getUserNama()
										+ " (" + tbmuser.getUserId() + ")" + "<br>Isi catatan KRS adalah : "
										+ catatan.getValue() + "<br>Isi catatan KHS adalah : " + catatanKhs.getValue()
										+ "<br>Terlampir KRS yang Anda ambil.<br>Untuk informasi lebih lanjut bisa dilihat pada link "
										+ Common.getRequestHostWithProtocol()
										+ ", kemudian click menu KRS, cari catatan sbb : " + info
										+ "<br><br>Terima Kasih";
								String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
								MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, krsMahasiswa,
										file);
							}

							// Callback tampilan sudah dijalankan segera setelah catatan berhasil
							// disimpan. Jangan jalankan kembali sesudah email karena selain merender
							// dua kali, callback lama dapat memulai sinkronisasi KRS saat transaksi
							// penyimpanan sebelumnya belum selesai.
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	/**
	 * Mengambil baris {@link KrsMahasiswa} yang menjadi target catatan, membuatnya (sinkronisasi)
	 * bila belum ada.
	 *
	 * <p>Pertama mencoba {@link Common#ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa, Integer, Integer, Integer)}
	 * (operasi baca murni); bila baris belum terbentuk, baru dilakukan sinkronisasi sekali lewat
	 * {@link Common#singkronkanKrsMahasiswa(Mahasiswa, Integer, Integer, Integer, boolean)} — baris KRS
	 * memang diperlukan sebagai target lampiran/catatan.</p>
	 *
	 * @return baris KRS mahasiswa untuk kombinasi semester/tahapan/semester-pendek pada instance ini
	 */
	private KrsMahasiswa ambilKrsMahasiswaUntukCatatan() {
		KrsMahasiswa krsMahasiswa = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
				semesterPendek);
		if (krsMahasiswa == null || krsMahasiswa.getId() == null) {
			// Baris KRS memang diperlukan oleh lampiran/catatan. Sinkronisasi hanya
			// dilakukan sekali bila datanya belum pernah terbentuk.
			krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, true);
		}
		return krsMahasiswa;
	}

}
