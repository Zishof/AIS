package ais.delivery.email.sender;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class MailHelper extends GenericAutowireComposer {

	private static final long serialVersionUID = -2744627984975080227L;

	private Textbox usernameInput;

	// private Proposal tglseminar;

	public void tampil(final MyWindow window) {

		Common.clear(window);
		window.getFellowIfAny("window");
		window.setWidth("700px");
		window.setHeight("200px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setStyle("border:0px;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(center);
		groupbox.appendChild(new MyCaptionStyled(
				"Masukkan username anda, sistem secara otomatis akan mengirimkan password ke email anda"));
		groupbox.setWidth("95%");
		groupbox.setHeight("100%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		grid.setStyle("border:0px;background: transparent;");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("70%");
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Id Pengguna"));
		row.appendChild(usernameInput = new Textbox());
		usernameInput.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		toolbar.setHeight("60px");
		MyButtonConfig button = new MyButtonConfig("Tutup");
		button.setTooltiptext("Tutup");
		button.setHeight("55px");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		MyButtonConfig save = new MyButtonConfig("Kirim");
		save.setHeight("55px");
		save.setTooltiptext("Kirim");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// onSave(event);

				sendAndLoad(usernameInput.getValue());
				window.setVisible(false);
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void sendAndLoad(String username) throws Exception {

		Session session = HibernateUtil.currentSession();
		Tbmuser user = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("userId", username)), Tbmuser.class);
		Mahasiswa mahasiswa = null;
		Siswa siswa = null;
		Dosen dosen = user == null ? null : user.getDosen();
		Pegawai pegawai = user == null ? null : user.getPegawai();

		String emailUser = null;
		String password;
		String passwordDecript;
		String subject = Common.getKonfigurasi("default_title_forgot_password",
				"Pemberitahuan password untuk login ke Sistem Informasi Akademik ").getNilai();
		String body = null;
		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

		JSONArray userIds = new JSONArray();
		userIds.put(username);

		if (dosen != null) {
			if (dosen.getEmail() == null || dosen.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(dosen.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = dosen.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else if (pegawai != null) {
			if (pegawai.getEmail() == null || pegawai.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(pegawai.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = pegawai.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else if (user != null) {
			if (user.getEmail() == null || user.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(user.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = user.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else {

			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
					session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username)), Mahasiswa.class);

			if (mahasiswa == null) {

				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorIndukNasional", username)),
						Siswa.class);

				if (siswa == null) {
					MyMessageboxConfig.show("Id pengguna tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.ERROR);
					return;
				} else {
					if (siswa.getAlamatEmail() == null || siswa.getAlamatEmail().trim().isEmpty()
							|| !Common.isValidEmailAddress(siswa.getAlamatEmail().trim())) {
						MyMessageboxConfig.show(
								"Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						return;
					}
					emailUser = siswa.getAlamatEmail();
					password = siswa.getPass();
					passwordDecript = Common.desEncrypter.get().decrypt(password);
					body = "Username anda : " + siswa.getNomorIndukNasional() + " . Password : " + passwordDecript;
				}
			}
			if (mahasiswa != null) {

				if (mahasiswa.getEmail() == null || mahasiswa.getEmail().trim().isEmpty()
						|| !Common.isValidEmailAddress(mahasiswa.getEmail().trim())) {
					MyMessageboxConfig.show(
							"Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					return;
				}
				emailUser = mahasiswa.getEmail();
				password = mahasiswa.getPass();
				passwordDecript = Common.desEncrypter.get().decrypt(password);
				body = "Username anda : " + mahasiswa.getNim() + " . Password : " + passwordDecript;
			}

		}

		MailSender.sendMail(userIds, subject, body, sender, emailUser,
				siswa != null ? siswa : mahasiswa != null ? mahasiswa : user);
		MyMessageboxConfig.show(
				"Password anda telah dikirim ke email anda (" + emailUser + "), silahkan cek email anda",
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}
}
