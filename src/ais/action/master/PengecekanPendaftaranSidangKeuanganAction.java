package ais.action.master;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendaftaranSidangDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranSidang;
import ais.database.model.PendaftaranWisuda;
import ais.action.master.helper.FilterLanjutHelper;

public class PengecekanPendaftaranSidangKeuanganAction extends
		GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6878830378969012479L;
	Bandbox bandboxMahasiswa;
	Row rowNama;
	Row rowFakultas;
	Row rowJurusan;
	Row rowJudulSkripsi;
	Row rowCekKeuangan;
	Row rowConfirm;
	Row rowDosenPembimbing1;
	Row rowDosenPembimbing2;
	Textbox nama;
	Textbox jurusan;
	Textbox fakultas;
	Textbox judulSkripsi;
	MyCheckboxConfig cekKeuangan;
	Textbox dosenPembimbing1;
	Textbox dosenPembimbing2;
	Label labelCekKeuangan;
	MyButtonConfig btnSetuju;
	MyButtonConfig btnTolak;

	PendaftaranWisuda pendaftaranWisuda;
	PendaftaranSidang pendaftaranSidang;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		Common.initLaguage();if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
	        FilterLanjutHelper.setup(comp);
}

	public void onPilihMahasiswa() throws Exception {
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa
				.getAttribute("mahasiswa");
		Session session = HibernateUtil.currentSession();

		pendaftaranSidang = (PendaftaranSidang) session
				.createCriteria(PendaftaranSidang.class)
				.createCriteria("skripsi")
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
				.uniqueResult();

		rowNama.setVisible(true);
		nama.setValue(pendaftaranSidang.getSkripsi().getMahasiswa().getNama());

		rowFakultas.setVisible(true);
		fakultas.setValue(pendaftaranSidang.getSkripsi().getMahasiswa()
				.getJurusan().getFakultas().getNama());

		rowJurusan.setVisible(true);
		jurusan.setValue(pendaftaranSidang.getSkripsi().getMahasiswa()
				.getJurusan().getNama());

		rowJudulSkripsi.setVisible(true);
		judulSkripsi.setValue(pendaftaranSidang.getSkripsi().getJudul());

		rowDosenPembimbing1.setVisible(true);
		dosenPembimbing1.setValue(pendaftaranSidang.getSkripsi()
				.getKetuaSidang().getNama());

		rowDosenPembimbing2.setVisible(true);
		dosenPembimbing2.setValue(pendaftaranSidang.getSkripsi()
				.getPembimbing().getNama());

		rowCekKeuangan.setVisible(true);
		cekKeuangan.setVisible(true);

		if (pendaftaranSidang.getDisetujuiOlehKeuangan() != null
				&& pendaftaranSidang.getDisetujuiOlehKeuangan().equals(1)) {
			cekKeuangan.setChecked(true);
			btnTolak.setDisabled(false);
			btnSetuju.setDisabled(true);
		} else {
			cekKeuangan.setChecked(false);
			btnTolak.setDisabled(true);
			btnSetuju.setDisabled(false);
		}

	}

	public void onSetuju() throws InterruptedException {
		if (cekKeuangan.isChecked()) {
			MyMessageboxConfig
					.show("Apakah anda yakin, mahasiswa ini tidak ada tunggakan atau masalah lain menyangkut keuangan ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.CANCEL)
										return;
									else {
										PendaftaranSidangDao daftarSidangDao = DaoFactory
												.getInstance()
												.getPendaftaranSidangDao();
										if (pendaftaranSidang.getId() != null) {
											pendaftaranSidang = daftarSidangDao
													.load(pendaftaranSidang
															.getId());
										}
										pendaftaranSidang
												.setDisetujuiOlehKeuangan(1);
										// daftarSidangDao.beginTransaction();
										if (pendaftaranSidang.getId() != null) {
											daftarSidangDao
													.update(pendaftaranSidang);
										}
										// daftarSidangDao.commitTransaction();
										cekKeuangan.setChecked(true);
										btnTolak.setDisabled(false);
										btnSetuju.setDisabled(true);
										return;
									}

								}
							});

		} else {
			MyMessageboxConfig.show(
					"Tandai Bagian Keuangan Menyetujui terlebih dahulu!",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	public void onTolak() throws InterruptedException {
		if (!cekKeuangan.isChecked()) {
			MyMessageboxConfig
					.show("Apakah anda yakin, mahasiswa ini belum menyelesaikan tunggakan atau masalah lain menyangkut keuangan ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.CANCEL)
										return;
									else {
										PendaftaranSidangDao daftarSidangDao = DaoFactory
												.getInstance()
												.getPendaftaranSidangDao();
										if (pendaftaranSidang.getId() != null) {
											pendaftaranSidang = daftarSidangDao
													.load(pendaftaranSidang
															.getId());
										}
										pendaftaranSidang
												.setDisetujuiOlehKeuangan(0);
										// daftarSidangDao.beginTransaction();
										if (pendaftaranSidang.getId() != null) {
											daftarSidangDao
													.update(pendaftaranSidang);
										}
										// daftarSidangDao.commitTransaction();
										cekKeuangan.setChecked(false);
										btnTolak.setDisabled(true);
										btnSetuju.setDisabled(false);
										return;
									}

								}
							});

		} else {
			MyMessageboxConfig
					.show("Hilangkan Tanda Check Bagian Keuangan Menyetujui terlebih dahulu!",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

}
