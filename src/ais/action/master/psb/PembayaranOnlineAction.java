package ais.action.master.psb;

import java.util.Calendar;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;

public class PembayaranOnlineAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox pinPassword;
	private Row tampilanPin;

	private CalonSiswa calonSiswa;

	private Center center;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketik nomor pendaftaran atau nomor ujian Anda pada kolom yang tersedia; (2) Pastikan tidak ada spasi di awal atau akhir; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketik PIN atau kata sandi yang diberikan panitia pada kolom yang tersedia; (2) Pastikan penulisan sudah benar (perhatikan huruf besar/kecil); (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
					.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
					.add(Restrictions.or(
							Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
							Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		} else {
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(
					session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		}
		if (calonSiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Siswa dengan nomor pendaftaran \"" + noRegistrasi.getValue() + "\" dan PIN / Password yang dimasukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan PIN / Password sudah benar; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Siswa dengan nomor pendaftaran \"" + noRegistrasi.getValue() + "\" tidak ditemukan, atau waktu terakhir bisa masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan masih dalam periode waktu yang ditentukan; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
			return;
		}

		if (calonSiswa != null) {

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

				Common.clear(center);

				MyInclude iframe = new MyInclude();
				iframe.setHeight("100%");
				iframe.setWidth("100%");
				iframe.setParent(center);
				iframe.setSrc("/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId());

				menuLogin.setVisible(false);

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

	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
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
		Common.initLaguage();
	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
