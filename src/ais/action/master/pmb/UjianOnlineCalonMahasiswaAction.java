package ais.action.master.pmb;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.PertemuanPunyaUjianHelper;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pertemuan;
import ais.database.model.PesertaUjian;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk ujian online calon mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox noRegistrasi}, {@code Textbox
 * pinPassword}, {@code Row tampilanPin}, {@code Row tampilanNoRegistrasi}, {@code Row tampilanTanggalLahir},
 * {@code Row labelInformasi}, {@code Combobox tahun}, {@code Combobox bulan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code afterLogin()}, {@code init()}); mutasi data ({@code
 * onReset()}); operasi domain lain ({@code onLogin()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class UjianOnlineCalonMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox pinPassword;
	private Row tampilanPin;
	private Row tampilanNoRegistrasi;
	private Row tampilanTanggalLahir;
	private Row labelInformasi;

	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	private Label infoLogin;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private Textbox username;

	private Textbox password;

	private boolean tampilkanUsernameDanPasswordPadaFormPMB = false;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		biodataCalonMahasiswa = Common.isLogin();
		if (biodataCalonMahasiswa != null) {
			afterLogin();
		} else {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {

				biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();

				Common.clear(window);
				biodataCalonMahasiswa.put(Common.dateFormat9.get().format(ais.ui.util.WaktuUtil.getDate()), "login_terakhir");
				UjianOnlineCalonMahasiswaAction.this.biodataCalonMahasiswa = biodataCalonMahasiswa;
				init(biodataCalonMahasiswa, window);
			} else {

				tampilkanUsernameDanPasswordPadaFormPMB = Common.bolehKonfigurasi("tampilkan_username_dan_password_form_PMB", Konfigurasi.TIDAK_AKTIF);
				tampilanNoRegistrasi.setVisible(!tampilkanUsernameDanPasswordPadaFormPMB);
				tampilanTanggalLahir.setVisible(!tampilkanUsernameDanPasswordPadaFormPMB);
				labelInformasi.setVisible(!tampilkanUsernameDanPasswordPadaFormPMB);

				if (tampilkanUsernameDanPasswordPadaFormPMB) {
					Rows rows = (Rows) tampilanTanggalLahir.getParent();
					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Username "));
					row.appendChild(username = new Textbox());
					username.setWidth("90%");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Password "));
					row.appendChild(password = new Textbox());
					password.setWidth("90%");
					password.setType("password");
				}

				if (tampilanPin != null) { tampilanPin.setVisible(false); }

				if (infoLogin != null) {
					infoLogin.setValue(Common.getKonfigurasi("info_login_ujian_calon_mahasiswa",
							"Untuk dapat melakukan login, silahkan masukkan Nomor Registrasi yang anda dapatkan pada saat melakukan pendaftaran dan masukkan TANGGAL LAHIR.")
							.getNilai());
				}

				MyComboitemConfig comboitem;
				for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 80; i < ais.ui.util.WaktuUtil
						.getCalendar().get(Calendar.YEAR); i++) {
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
		}
	        FilterLanjutHelper.setup(comp);
}

	public void onLogin(Event event) throws Exception {
		Session session = HibernateUtil.currentSession();
		if (tampilkanUsernameDanPasswordPadaFormPMB) {

			if (username == null || username.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Nama Pengguna (username) belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nama Pengguna Anda pada kolom yang tersedia; (2) Pastikan penulisan sesuai dengan data akun Anda; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (password == null || password.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Kata Sandi (password) belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Kata Sandi Anda pada kolom yang tersedia; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			biodataCalonMahasiswa = (BiodataCalonMahasiswa) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.eq("username", username.getValue().trim()),
							Restrictions.eq("password", Common.desEncrypter.get().encrypt(password.getValue().trim()))))
					.uniqueResult();

			if (biodataCalonMahasiswa != null) {
				Common.clear(window);
				biodataCalonMahasiswa.put(Common.dateFormat9.get().format(ais.ui.util.WaktuUtil.getDate()), "login_terakhir");
				UjianOnlineCalonMahasiswaAction.this.biodataCalonMahasiswa = biodataCalonMahasiswa;
				init(biodataCalonMahasiswa, window);
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Nama Pengguna dan Kata Sandi yang Anda masukkan tidak sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nama Pengguna dan Kata Sandi; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

		} else {
			if (noRegistrasi.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nomor Pendaftaran Anda pada kolom yang tersedia; (2) Pastikan nomor sesuai dengan yang tertera pada bukti pendaftaran; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			if (tampilanPin != null && tampilanPin.isVisible()) {
				if (pinPassword.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(
							"Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan PIN / Kata Sandi Anda pada kolom yang tersedia; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Ulangi proses masuk.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(session
						.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)

						.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
						.add(Restrictions.or(
								Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
								Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
						BiodataCalonMahasiswa.class);
			} else {
				biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(session
						.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)

						.add(Restrictions.or(
								Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
								Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
						BiodataCalonMahasiswa.class);
			}
			if (biodataCalonMahasiswa == null) {
				if (tampilanPin != null && tampilanPin.isVisible()) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data peserta dengan Nomor Pendaftaran \"{V1}\" dan PIN / Kata Sandi yang Anda masukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran dan PIN / Kata Sandi; (2) Pastikan tidak terdapat spasi berlebih; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
				} else {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, peserta dengan Nomor Pendaftaran \"{V1}\" tidak ditemukan, atau batas waktu untuk masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran; (2) Pastikan periode masuk masih berlaku; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
				}
				return;
			}

			if (biodataCalonMahasiswa != null) {

//				if (biodataCalonMahasiswa.getDitolak()) {
//					Messagebox.show(
//							"Maaf, Anda tidak diterima / ditolak untuk login, hubungi panitia untuk informasi lebih lanjut",
//							"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//					return;
//				}
//				if (biodataCalonMahasiswa.getMundur()) {
//					Messagebox.show(
//							"Maaf, Anda dinyatakan mengundurkan diri, hubungi panitia untuk informasi lebih lanjut",
//							"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//					return;
//				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(biodataCalonMahasiswa.getTanggalLahir());
				int thn = calendar.get(Calendar.YEAR);
				int bln = calendar.get(Calendar.MONTH);
				int tgl = calendar.get(Calendar.DATE);
				boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
						: tahun.getSelectedItem().getValue().equals(thn))
						&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
						&& (tanggal.getSelectedItem() == null ? false
								: tanggal.getSelectedItem().getValue().equals(tgl));

				if (kondisiTglLahir) {
					Common.setLogin(biodataCalonMahasiswa);
					afterLogin();
				} else {
					MyMessageboxConfig.show(
							"Mohon maaf, Nomor Pendaftaran atau Tanggal Lahir yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali Nomor Pendaftaran Anda; (2) Pastikan pilihan Tanggal, Bulan, dan Tahun Lahir sudah benar; (3) Ulangi proses masuk.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Nomor Pendaftaran Anda tidak ditemukan. Langkah yang dapat dilakukan: (1) Pastikan Anda telah mengikuti prosedur pembayaran dengan benar; (2) Periksa kembali penulisan Nomor Pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		}
	}

	public void afterLogin() throws Exception {

		if (biodataCalonMahasiswa.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin()) {
			try {
				JenisKegiatan jenisKegiatan = PembayaranUtil.getInstance()
						.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
				Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();
				if (kegiatan == null || kegiatan.getId() == null) {
					int smt = 0;
					Session session = HibernateUtil.currentSession();
					kegiatan = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan, biodataCalonMahasiswa, smt,
							biodataCalonMahasiswa.getTahunAkademik(), true, false, null, session);
				}
				if (!ais.common.CommonPMB.isPembayaranRegistrasiTerpenuhi(kegiatan)) {
					String infoBelumbayarSaatLogincalonMahasiswa = Common.getKonfigurasi(
							"infoBelumbayarSaatLogincalonMahasiswa",
							"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat menlanjutkan proses ini karena belum melakukan proses pembayaran.")
							.getNilai();
					infoBelumbayarSaatLogincalonMahasiswa = org.apache.commons.lang.StringUtils
							.replace(infoBelumbayarSaatLogincalonMahasiswa, "[noreg]", noRegistrasi.getValue());
					MyMessageboxConfig.show(infoBelumbayarSaatLogincalonMahasiswa, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					window.detach();
					return;
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		System.out.println("Match!");
		Common.clear(window);
		biodataCalonMahasiswa.put(Common.dateFormat9.get().format(ais.ui.util.WaktuUtil.getDate()), "login_terakhir");
		UjianOnlineCalonMahasiswaAction.this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		init(biodataCalonMahasiswa, window);
	}

	private MyWindow window;

	@SuppressWarnings({ "unchecked" })
	private void init(final BiodataCalonMahasiswa biodata, final MyWindow window) throws Exception {

		if (biodata.getGelombangPendaftaran() != null
				&& biodata.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir() != null
				&& biodata.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir()
						.before(ais.ui.util.WaktuUtil.getDate())) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, masa masuk (login) untuk gelombang pendaftaran \"{V1}\" telah berakhir pada tanggal {V2}. Langkah yang dapat dilakukan: (1) Periksa kembali jadwal gelombang pendaftaran yang Anda ikuti; (2) Pastikan Anda masuk pada periode yang masih berlaku; (3) Hubungi panitia penerimaan mahasiswa baru apabila memerlukan bantuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					biodata.getGelombangPendaftaran().getNama(),
					Common.dateFormat4.get().format(biodata.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir()));
			return;
		}

		this.biodataCalonMahasiswa = biodata;
		this.window = window;
		final PertemuanPunyaUjianHelper pertemuanPunyaUjianHelper = new PertemuanPunyaUjianHelper(null,
				biodataCalonMahasiswa);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Pendaftaran"));
		Label lbl;
		row.appendChild(lbl = new Label(
				biodataCalonMahasiswa.getNoRegistrasi() == null ? "" : biodataCalonMahasiswa.getNoRegistrasi()));
		lbl.setStyle("font-weight: bold; font-size: large;");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Lengkap"));
		Label nama;
		row.appendChild(
				nama = new Label(biodataCalonMahasiswa.getNama() == null ? "" : biodataCalonMahasiswa.getNama()));
		nama.setStyle("font-weight: bold; font-size: large;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("60%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(PesertaUjian.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.setProjection(Projections.groupProperty("pertemuan")).createCriteria("pertemuan")
				.add(Restrictions.eq("mandiri", true))
				.add(Restrictions.or(Restrictions.isNull("mulai"),
						Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate())))
				.add(Restrictions.or(Restrictions.isNull("selesai"),
						Restrictions.ge("selesai", ais.ui.util.WaktuUtil.getDate())))
				.list();
		if (pertemuans.size() == 1) {
			final MyWindow windowUjian = new MyWindow("Ujian Online", "none", true);
			windowUjian.setParent(page.getFirstRoot());
			windowUjian.setHeight("95%");
			windowUjian.setWidth("90%");
			pertemuanPunyaUjianHelper.display(pertemuans.get(0), windowUjian);

			// Tombol Tutup eksplisit: window ber-border "none" tidak menampilkan caption/close-X,
			// jadi perlu cara jelas untuk menutup jendela ujian ini (tidak menyentuh logic ujian di dalamnya).
			Toolbar toolbarTutupUjian = new Toolbar();
			toolbarTutupUjian.setWidth("100%");
			toolbarTutupUjian.setAlign("end");
			toolbarTutupUjian.setStyle("padding:4px;");
			MyToolbarbuttonConfig tutupUjianButton = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutupUjianButton.setTooltiptext("Tutup jendela ujian ini");
			tutupUjianButton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event evt) throws Exception {
					windowUjian.detach();
				}
			});
			tutupUjianButton.setParent(toolbarTutupUjian);
			if (windowUjian.getFirstChild() != null) {
				windowUjian.insertBefore(toolbarTutupUjian, windowUjian.getFirstChild());
			} else {
				toolbarTutupUjian.setParent(windowUjian);
			}

			windowUjian.onModal();
		} else {
			for (final Pertemuan pertemuan : pertemuans) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(pertemuan.getTopik()));
				row.appendChild(
						new Label(pertemuan.getMulai() == null ? "" : Common.dateFormat4.get().format(pertemuan.getMulai())));
				row.appendChild(new Label(
						pertemuan.getSelesai() == null ? "" : Common.dateFormat4.get().format(pertemuan.getSelesai())));

				Hbox toolbar = new Hbox();
				toolbar.setParent(row);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ikut/Lihat hasil ujian",
						"/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ikut Ujian");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						final MyWindow window = new MyWindow("Ujian Online", "none", true);
						window.setParent(page.getFirstRoot());
						window.setHeight("95%");
						window.setWidth("90%");
						pertemuanPunyaUjianHelper.display(pertemuan, window);

						// Tombol Tutup eksplisit: window ber-border "none" tidak menampilkan caption/close-X,
						// jadi perlu cara jelas untuk menutup jendela ujian ini (tidak menyentuh logic ujian di dalamnya).
						Toolbar toolbarTutupUjian = new Toolbar();
						toolbarTutupUjian.setWidth("100%");
						toolbarTutupUjian.setAlign("end");
						toolbarTutupUjian.setStyle("padding:4px;");
						MyToolbarbuttonConfig tutupUjianButton = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						tutupUjianButton.setTooltiptext("Tutup jendela ujian ini");
						tutupUjianButton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event evt) throws Exception {
								window.detach();
							}
						});
						tutupUjianButton.setParent(toolbarTutupUjian);
						if (window.getFirstChild() != null) {
							window.insertBefore(toolbarTutupUjian, window.getFirstChild());
						} else {
							toolbarTutupUjian.setParent(window);
						}

						window.onModal();
					}

				});
				button.setParent(toolbar);

			}
		}

	}

	public void onReset() {
		noRegistrasi.setValue("");
		pinPassword.setValue("");
		tahun.setSelectedItem(null);
		bulan.setSelectedItem(null);
		tanggal.setSelectedItem(null);
	}

}
