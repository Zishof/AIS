package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.Sekolah;

public class KonfigurasiAbsenPiketGuruAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public void onTampil() {

		Sekolah sekolah = SekolahUtil.getSekolah();
		Long sekolahId = (sekolah == null || sekolah.getId() == null) ? 0L : sekolah.getId();

		Rows rows = (createSpan("Konfigurasi Absen Piket Guru"));

		rows.appendChild(createRowActiveDefault("Aktifkan jam ke I ?", sekolahId + "_absen_piket_guru_jam_ke_1",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Toleransi menit jam ke I", sekolahId + "_absen_piket_guru_toleransi_jam_ke_1", "30"));
		rows.appendChild(
				createRowNilai("Waktu absen jam ke I", sekolahId + "_absen_piket_guru_waktu_jam_ke_1", "07.30"));

		rows.appendChild(createRowActiveDefault("Aktifkan jam ke II ?", sekolahId + "_absen_piket_guru_jam_ke_2",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Toleransi menit jam ke II", sekolahId + "_absen_piket_guru_toleransi_jam_ke_2", "30"));
		rows.appendChild(
				createRowNilai("Waktu absen jam ke II", sekolahId + "_absen_piket_guru_waktu_jam_ke_2", "09.30"));

		rows.appendChild(createRowActiveDefault("Aktifkan jam ke III ?", sekolahId + "_absen_piket_guru_jam_ke_3",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Toleransi menit jam ke III", sekolahId + "_absen_piket_guru_toleransi_jam_ke_3", "30"));
		rows.appendChild(
				createRowNilai("Waktu absen jam ke III", sekolahId + "_absen_piket_guru_waktu_jam_ke_3", "12.30"));

		rows.appendChild(createRowActiveDefault("Aktifkan jam ke IV ?", sekolahId + "_absen_piket_guru_jam_ke_4",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Toleransi menit jam ke IV", sekolahId + "_absen_piket_guru_toleransi_jam_ke_4", "30"));
		rows.appendChild(
				createRowNilai("Waktu absen jam ke IV", sekolahId + "_absen_piket_guru_waktu_jam_ke_4", "14.30"));

		rows.appendChild(createRowActiveDefault("Aktifkan jam ke V ?", sekolahId + "_absen_piket_guru_jam_ke_5",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Toleransi menit jam ke V", sekolahId + "_absen_piket_guru_toleransi_jam_ke_5", "30"));
		rows.appendChild(
				createRowNilai("Waktu absen jam ke V", sekolahId + "_absen_piket_guru_waktu_jam_ke_5", "16.30"));

	}
}
