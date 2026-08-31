package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * Controller/action ZK untuk pembayaran via payment gateway. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox noRegistrasi}, {@code Textbox
 * pinPassword}, {@code Row tampilanPin}, {@code BiodataCalonMahasiswa biodataCalonMahasiswa}, {@code North
 * uploadMenu}, {@code North menuLogin}, {@code Combobox tahun}, {@code Combobox bulan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code afterLogin()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onReset()}); operasi domain lain ({@code onLogin()}). Bagian lain
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
public class PembayaranViaPaymentGatewayAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox pinPassword;
	private Row tampilanPin;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private North uploadMenu;
	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	private MyGrid grid;

//	private MyColumnConfig colFormulirPendaftaran;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

//		if (colFormulirPendaftaran != null) {
//			colFormulirPendaftaran.setVisible(Common
//					.getKonfigurasi("calon_mahasiswa_harus_melakukan_pembayaran_sebelum_bisa_login", Konfigurasi.AKTIF)
//					.getNilai().equals(Konfigurasi.AKTIF));
//			colFormulirPendaftaran.setWidth(colFormulirPendaftaran.isVisible() ? "20%" : "0%");
//		}

		biodataCalonMahasiswa = Common.isLogin();
		if (biodataCalonMahasiswa != null) {
			afterLogin();
		} else {

			if (tampilanPin != null) { tampilanPin.setVisible(false); }
			MyComboitemConfig comboitem;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 80; i < ais.ui.util.WaktuUtil
					.getCalendar().get(Calendar.YEAR) + 1; i++) {
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

	public void afterLogin() throws Exception {
		onSearchDefault(null);

		menuLogin.setVisible(false);

		Common.clear(uploadMenu);

		uploadMenu.setHeight("0px");

//		MyGrid grid = new MyGrid();
//		grid.setWidth("100%");
//		grid.setParent(uploadMenu);
//		grid.setWidth("100%");
//		grid.setHeight("100%");
//
//		Columns columns = new Columns();
//		columns.setParent(grid);
//		MyColumnConfig column = new MyColumnConfig();
//		column.setParent(columns);
//		column.setWidth("40%");
//
//		column = new MyColumnConfig();
//		column.setParent(columns);
//
//		final Rows rows = new Rows();
//		rows.setParent(grid);
//
//		MyFormRow row = new MyFormRow();row.setValign("top");
////		row.setParent(rows);
//
//		row.appendChild(new ais.ui.util.MyLabelBold("Nomor Pendaftaran"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoRegistrasi()));
//
//		if (biodataCalonMahasiswa.getNoUjian() != null) {
////			row.setParent(rows);
//			row.appendChild(new ais.ui.util.MyLabelBold("Nomor Ujian"));
//			row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoUjian()));
//		}
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelBold("Nama"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNama()));
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nomor Pendaftaran Anda pada kolom yang tersedia; (2) Pastikan nomor sesuai dengan yang tertera pada bukti pendaftaran; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan PIN / Kata Sandi Anda pada kolom yang tersedia; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
							.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		} else {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		}
		if (biodataCalonMahasiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" dan PIN / Kata Sandi yang Anda masukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran dan PIN / Kata Sandi; (2) Pastikan tidak terdapat spasi berlebih; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			} else {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran; (2) Pastikan nomor sesuai dengan bukti pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			}
			return;
		}

		if (biodataCalonMahasiswa != null) {

//			if (biodataCalonMahasiswa.getDitolak()) {
//				Messagebox.show(
//						"Maaf, Anda tidak diterima / ditolak untuk login, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}
//			if (biodataCalonMahasiswa.getMundur()) {
//				Messagebox.show("Maaf, Anda dinyatakan mengundurkan diri, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(biodataCalonMahasiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

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

	public void onSearchDefault(Event event) throws Exception {

		List<BiodataCalonMahasiswa> ruangPaketList = new ArrayList<BiodataCalonMahasiswa>();

		ruangPaketList.add(biodataCalonMahasiswa);

		ListModel strset = new SimpleListModel(ruangPaketList);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(arg0);

			new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoUjian()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNama().toUpperCase()).setParent(arg0);

			if (biodataCalonMahasiswa.getProdiLulus() == null) {
				Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();
				if (kegiatan == null || kegiatan.getId() == null) {
					JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
							.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
					kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);
					biodataCalonMahasiswa.setPembayaranRegistrasi(kegiatan);
				}
			} else {
				Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranDaftarUlang();
				if (kegiatan == null || kegiatan.getId() == null) {
					JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
							.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);
					biodataCalonMahasiswa.setPembayaranDaftarUlang(kegiatan);
				}
			}

			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();
			if (kegiatan == null || (kegiatan.getAmount() < 0.01 && kegiatan.getPersentaseLunas() < 0.01)) {

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				MyButtonConfig button = new MyButtonConfig("Bayar Biaya Pendaftaran");
				button.setWidth("90%");
				button.setParent(vbox);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanPaymentGateway.tampilPembayaranRegistrasi(biodataCalonMahasiswa);
					}
				});

				new MyLabelAgakKecilBold(

						kegiatan != null && (kegiatan.getAmount() + kegiatan.getAmountTerhutang()) < 0.01
								? "Tidak ada tagihan"
								:

								kegiatan == null || (kegiatan.getAmount() < 0.01 && kegiatan.getPersentaseLunas() < 0.01)
										? "Belum Bayar"
										: kegiatan.getPersentaseLunas().intValue() == 100
												? "Lunas " + Common.numberFormat.get().format(kegiatan.getAmount())
												: "Bayar " + Common.numberFormat.get().format(kegiatan.getAmount())
														+ " dari tagihan "
														+ Common.numberFormat.get().format(
																kegiatan.getAmount() + kegiatan.getAmountTerhutang())
														+ " atau "
														+ Common.numberFormat.get().format(kegiatan.getPersentaseLunas())
														+ "%")
						.setParent(vbox);

			} else {
				new MyLabelAgakKecilBold(

						kegiatan != null && (kegiatan.getAmount() + kegiatan.getAmountTerhutang()) < 0.01
								? "Tidak ada tagihan"
								:

								kegiatan == null || (kegiatan.getAmount() < 0.01 && kegiatan.getPersentaseLunas() < 0.01)
										? "Belum Bayar"
										: kegiatan.getPersentaseLunas().intValue() == 100
												? "Lunas " + Common.numberFormat.get().format(kegiatan.getAmount())
												: "Bayar " + Common.numberFormat.get().format(kegiatan.getAmount())
														+ " dari tagihan "
														+ Common.numberFormat.get().format(
																kegiatan.getAmount() + kegiatan.getAmountTerhutang())
														+ " atau "
														+ Common.numberFormat.get().format(kegiatan.getPersentaseLunas())
														+ "%")
						.setParent(arg0);
			}

			Kegiatan kegiatanDaftarUlang = biodataCalonMahasiswa.getPembayaranDaftarUlang();

			if (Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_daftar_ulang_mahasiswa_baru")) {

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);

				if (Common.getKonfigurasi("calon_mahasiswa_harus_lulus_sebelum_bayar_daftar_ulang", Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.TIDAK_AKTIF) || biodataCalonMahasiswa.getProdiLulus() != null) {
					MyButtonConfig button = new MyButtonConfig("Bayar/Lihat Biaya Daftar Ulang");
					button.setWidth("90%");
					button.setParent(vbox);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanPaymentGateway.tampilPembayaranDaftarUlang(biodataCalonMahasiswa);
						}
					});
				}

				new MyLabelAgakKecilBold(biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() == null
						? "Belum dinyatakan Lulus/Diterima"
						: (

						kegiatanDaftarUlang != null
								&& (kegiatanDaftarUlang.getAmount() + kegiatanDaftarUlang.getAmountTerhutang()) < 0.01
										? "Tidak ada tagihan"
										:

										kegiatanDaftarUlang == null || (kegiatanDaftarUlang.getAmount() < 0.01
												&& kegiatanDaftarUlang.getPersentaseLunas() < 0.01)
														? "Belum Bayar"
														: "Bayar "
																+ Common.numberFormat.get()
																		.format(kegiatanDaftarUlang.getAmount())
																+ " dari tagihan "
																+ Common.numberFormat.get().format(kegiatanDaftarUlang
																		.getAmount()
																		+ kegiatanDaftarUlang.getAmountTerhutang())
																+ " atau "
																+ Common.numberFormat.get().format(
																		kegiatanDaftarUlang.getPersentaseLunas())
																+ "%"))
						.setParent(vbox);

			} else {
				new MyLabelAgakKecilBold(biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() == null
						? "Belum dinyatakan Lulus/Diterima"
						: (kegiatanDaftarUlang != null
								&& (kegiatanDaftarUlang.getAmount() + kegiatanDaftarUlang.getAmountTerhutang()) < 0.01
										? "Tidak ada tagihan"
										:

										kegiatanDaftarUlang == null || (kegiatanDaftarUlang.getAmount() < 0.01
												&& kegiatanDaftarUlang.getPersentaseLunas() < 0.01)
														? "Belum Bayar"
														: "Bayar "
																+ Common.numberFormat.get()
																		.format(kegiatanDaftarUlang.getAmount())
																+ " dari tagihan "
																+ Common.numberFormat.get().format(kegiatanDaftarUlang
																		.getAmount()
																		+ kegiatanDaftarUlang.getAmountTerhutang())
																+ " atau "
																+ Common.numberFormat.get().format(
																		kegiatanDaftarUlang.getPersentaseLunas())
																+ "%"))
						.setParent(arg0);
			}

		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
