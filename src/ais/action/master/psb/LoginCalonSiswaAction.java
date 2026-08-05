package ais.action.master.psb;

import java.util.Calendar;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.action.maintenance.PSBAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;

public class LoginCalonSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox nomorInduk;
	private Textbox pinPassword;
	private Row tampilanPin;

	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	@SuppressWarnings("unused")
	private Textbox nis;

	private Label infoLogin;
	private North kop;
	private CalonSiswa calonSiswa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (kop != null) {
			Div kopBox = new Div();
			kop.appendChild(kopBox);
			PSBAction.initHeader(kopBox);
		}

		if (tampilanPin != null) { tampilanPin.setVisible(false); }

		if (infoLogin != null) {
			infoLogin.setValue(Common.getKonfigurasi("info_login_calon_siswa_baru",
					"Untuk dapat melakukan login, silahkan masukkan Nomor Registrasi yang anda dapatkan pada saat melakukan pendaftaran dan masukkan TANGGAL LAHIR.")
					.getNilai());
		}

		Sekolah sekolah = SekolahUtil.getSekolah();

		int tahunLoginCalonSiswa = 30;
		try {
			tahunLoginCalonSiswa = Integer.parseInt(Common
					.getKonfigurasi("tahun_login_calon_siswa"
							+ (sekolah == null || sekolah.getId() == null ? "" : "_" + sekolah.getId()), "30")
					.getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/psb/LoginCalonSiswaAction.java:89");
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
		Common.initLaguage();
	}

	public void onLogin(Event event) throws Exception {
		if (nomorInduk.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketik nomor pendaftaran atau nomor ujian Anda pada kolom yang tersedia; (2) Pastikan tidak ada spasi di awal atau akhir; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tahun Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Tahun; (2) Pilih tahun kelahiran Ananda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bulan Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Bulan; (2) Pilih bulan kelahiran Ananda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tanggal.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tanggal Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Tanggal; (2) Pilih tanggal kelahiran Ananda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();

		Calendar calendarEntry = ais.ui.util.WaktuUtil.getCalendar();
		calendarEntry.set(Calendar.YEAR, (Integer) tahun.getSelectedItem().getValue());
		calendarEntry.set(Calendar.MONTH, (Integer) bulan.getSelectedItem().getValue());
		calendarEntry.set(Calendar.DATE, (Integer) tanggal.getSelectedItem().getValue());

		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketik PIN atau kata sandi yang diberikan panitia pada kolom yang tersedia; (2) Pastikan penulisan sudah benar (perhatikan huruf besar/kecil); (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1).addOrder(Order.desc("id"))
					.add(Restrictions.eq("tanggalLahir", calendarEntry.getTime()))
					.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
					.add(Restrictions.or(
							Restrictions.ilike("nomorInduk", nomorInduk.getValue().trim(), MatchMode.EXACT),
							Restrictions.ilike("noUjian", nomorInduk.getValue().trim(), MatchMode.EXACT)))
					.uniqueResult();
		} else {
			calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1).addOrder(Order.desc("id"))

					.add(Restrictions.eq("tanggalLahir", calendarEntry.getTime()))

					.add(Restrictions.or(
							Restrictions.ilike("nomorInduk", nomorInduk.getValue().trim(), MatchMode.EXACT),
							Restrictions.ilike("noUjian", nomorInduk.getValue().trim(), MatchMode.EXACT)))
					.uniqueResult();
		}

		if (calonSiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				if (pinPassword.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketik PIN atau kata sandi yang diberikan panitia pada kolom yang tersedia; (2) Pastikan penulisan sudah benar (perhatikan huruf besar/kecil); (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1)
						.addOrder(Order.desc("id")).add(Restrictions.eq("tanggalLahir", calendarEntry.getTime()))
						.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
						.add(Restrictions.ilike("nama", nomorInduk.getValue().trim(), MatchMode.EXACT)).uniqueResult();
			} else {
				calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1)
						.addOrder(Order.desc("id")).add(Restrictions.eq("tanggalLahir", calendarEntry.getTime()))
						.add(Restrictions.ilike("nama", nomorInduk.getValue().trim(), MatchMode.EXACT)).uniqueResult();
			}
		}

		if (calonSiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Siswa dengan Nomor Pendaftaran / Nama \"{V1}\" tidak ditemukan, atau Tanggal Lahir serta PIN / Kata Sandi yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran / Nama dan PIN / Kata Sandi; (2) Pastikan pilihan Tanggal Lahir sudah benar; (3) Hubungi panitia penerimaan siswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, nomorInduk.getValue());
			} else {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Siswa dengan Nomor Pendaftaran / Nama \"{V1}\" tidak ditemukan, atau Tanggal Lahir yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran / Nama; (2) Pastikan pilihan Tanggal Lahir sudah benar; (3) Hubungi panitia penerimaan siswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, nomorInduk.getValue());
			}
			return;
		}

		if (calonSiswa != null) {

			if (calonSiswa.getGelombangPendaftaranPsb() != null) {
				if (Common.bolehKonfigurasi("calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login_baru", Konfigurasi.TIDAK_AKTIF)) {
					if (!GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa)) {
						return;
					}
				}
			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonSiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

			if (kondisiTglLahir) {
				System.out.println("Match!");

				calonSiswa.setTelahLogin(true);
				calonSiswa.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
				Common.refreshUpdate(session, calonSiswa);
				LoginCalonSiswaAction.this.calonSiswa = calonSiswa;

				try {
					String strHasilUjianMahasiswa = calonSiswa.retreive("hasilUjianMahasiswa");
					HasilUjianMahasiswa.tampilkanUjianKembali(strHasilUjianMahasiswa);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				onLogin();

			} else {
				MyMessageboxConfig.show("Mohon maaf, Nomor Referensi atau Tanggal Lahir yang Anda masukkan tidak sesuai dengan data kami. Langkah yang dapat dilakukan: (1) Periksa kembali nomor pendaftaran atau referensi yang dimasukkan; (2) Pastikan Tahun, Bulan, dan Tanggal Lahir sudah dipilih dengan benar; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, nomor registrasi Anda tidak ditemukan dalam sistem. Langkah yang dapat dilakukan: (1) Pastikan Anda telah menyelesaikan prosedur pembayaran pendaftaran terlebih dahulu; (2) Periksa kembali nomor registrasi yang dimasukkan; (3) Hubungi panitia penerimaan siswa baru untuk konfirmasi status pendaftaran Anda. Jika masih mengalami kendala, hubungi Administrator.",
					"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	public void onLogin() {

		Common.setLogin(calonSiswa);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Executions.getCurrent().sendRedirect("");
			}
		});

	}

	public void onReset() {
		nomorInduk.setValue("");
		if (pinPassword != null) { pinPassword.setValue(""); }
		tahun.setSelectedItem(null);
		bulan.setSelectedItem(null);
		tanggal.setSelectedItem(null);
	}

}
