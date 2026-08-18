package ais.action.master.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DesEncrypter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class ResetPasswordDosenMahasiswaHelper extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6947829244115144706L;

	private Row rowNama;
	private Row rowButton;
	private Label labelNama;
	private Textbox userid;
	MyButtonConfig reset;
	MyButtonConfig cari;

	private Mahasiswa mahasiswa;
	private Tbmuser tbmuser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		reset.setDisabled(true);

		reset.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				if (onReset()) {
					String pesan = userid.getValue().trim();

					MyMessageboxConfig
							.show("Password untuk User ID : " + userid.getValue() + " telah diset menjadi : " + pesan);
					System.out.println(
							"Password untuk User ID : " + userid.getValue() + " telah diset menjadi : " + pesan);

					reset.setDisabled(true);

				}

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	public void onCari() throws Exception {
		if (userid.getValue().equals("")) {
			MyMessageboxConfig.show("Masukkan User ID Dosen / NIM Mahasiswa", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		tbmuser = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
						.add(Restrictions.isNotNull("dosen")).add(Restrictions.eq("userId", userid.getValue())),
				Tbmuser.class);
		if (tbmuser == null || tbmuser.getUserId() == null) {
			mahasiswa = (Mahasiswa) ConstantValues
					.simpleObject(session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).add(Restrictions.eq("nim", userid.getValue())), Mahasiswa.class);
			if (mahasiswa == null) {
				MyMessageboxConfig.show("User ID Dosen / NIM Mahasiswa tidak valid", "Peringatan", 1,
						MyMessageboxConfig.EXCLAMATION);
				return;

			} else {
				rowNama.setVisible(true);
				labelNama.setValue(mahasiswa.getNama());
			}
		} else {
			rowNama.setVisible(true);
			labelNama.setValue(tbmuser.getUserNama());
		}
		rowButton.setVisible(true);
		reset.setDisabled(false);

	}

	public boolean onReset() throws Exception {
		DesEncrypter desEncrypter = Common.desEncrypter.get();
		if (tbmuser != null) {
			tbmuser.setUserPassword(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(tbmuser);
		} else if (mahasiswa != null) {
			mahasiswa.setPass(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(mahasiswa);
		}
		return true;

	}

}
