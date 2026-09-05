package ais.action.master.lkp.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KelompokParameterTambahanKegiatan;
import ais.database.model.lkp.ParameterTambahanKegiatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun, memvalidasi, dan menyimpan form parameter tambahan dinamis pada
 * form {@link RealisasiKerjaPegawai} (realisasi kerja pegawai LKP). Parameter tambahan dan
 * kelompoknya ditentukan oleh konfigurasi {@link KegiatanTugasJabatan} terkait — form ini
 * membangun baris input secara dinamis sesuai {@link KelompokParameterTambahanKegiatan} dan
 * {@link ParameterTambahan} yang aktif untuk kegiatan tersebut, termasuk validasi wajib isi dan
 * wajib lampiran per parameter.
 */
public class ParameterTambahanKegiatanListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private RealisasiKerjaPegawai realisasiKerjaPegawai;
	private Map<String, LampiranLain> lampiranLains;

	/** Menyiapkan listener untuk satu {@code realisasiKerjaPegawai}, memakai {@code parameterRows} sebagai daftar baris dinamis yang dikelola dan {@code lampiranLains} sebagai cache lampiran yang sudah diunggah. */
	public ParameterTambahanKegiatanListener(RealisasiKerjaPegawai realisasiKerjaPegawai, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.realisasiKerjaPegawai = realisasiKerjaPegawai;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi nilai parameter tambahan yang sudah diisi pada form untuk tiap baris di
	 * {@link #parameterRows}: menolak (menampilkan pesan dan mengembalikan {@code false}) bila ada
	 * parameter wajib isi yang masih kosong, atau parameter yang mewajibkan lampiran tapi belum
	 * ada lampiran diunggah (dicek dari {@link #lampiranLains} untuk realisasi baru, atau dari
	 * {@link LampiranLain#ambil} untuk realisasi yang sudah tersimpan).
	 *
	 * @return {@code true} bila seluruh parameter wajib sudah terpenuhi
	 */
	public boolean validate(RealisasiKerjaPegawai realisasiKerjaPegawai) throws Exception {

		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan = (KelompokParameterTambahanKegiatan) row
					.getAttribute("kelompokParameterTambahanKegiatan");
			if (parameterTambahan != null && kelompokParameterTambahanKegiatan != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(RealisasiKerjaPegawai.class,
						realisasiKerjaPegawai.getId(),
						kelompokParameterTambahanKegiatan.getId() + "->" + parameterTambahan.getId());

				String val = "";
				String[] spl = realisasiKerjaPegawai.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				boolean wajib = parameterTambahan.getWajibDiisi()
						&& !parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TIDAK_ADA)
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"));
				System.out.println(
						"parameterTambahan => " + parameterTambahan + ", val => " + val + ", wajib => " + wajib);

				if (wajib) {

					MyMessageboxConfig.show(
							"\"" + kelompokParameterTambahanKegiatan.getNama()
									+ "\" harus Anda lengkapi !\n\nPilihan \"" + parameterTambahan.getLabelInputan()
									+ "\" harus dipilih",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						if (realisasiKerjaPegawai.getId() == null) {

							if (!lampiranLains.containsKey(jenis)) {
								MyMessageboxConfig.show(
										"\"" + kelompokParameterTambahanKegiatan.getNama()
												+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
												+ parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

								return false;
							}
						} else {

							LampiranLain lam = LampiranLain.ambil(realisasiKerjaPegawai.getId(), jenis);

							if (lam == null) {

								MyMessageboxConfig.show(
										"\"" + kelompokParameterTambahanKegiatan.getNama()
												+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
												+ parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

								return false;
							}
						}

					}
				}
			}
		}
		return true;
	}

	/** Menyalin nilai parameter tambahan yang sudah diisi pada {@link #parameterRows} ke {@code realisasiKerjaPegawai}, dipanggil saat form disimpan. */
	public void onSave(RealisasiKerjaPegawai realisasiKerjaPegawai) {
		realisasiKerjaPegawai.populateParameterTambahan(parameterRows);
	}

	/** Memeriksa apakah kegiatan tugas jabatan terkait {@link #realisasiKerjaPegawai} punya minimal satu kelompok parameter tambahan aktif dengan minimal satu parameter aktif — dipakai untuk menentukan apakah blok form parameter tambahan perlu ditampilkan. */
	public boolean check() {
		if (realisasiKerjaPegawai == null) {
			return false;
		}
		Session session = HibernateUtil.currentSession();
		KegiatanTugasJabatan kegiatanTugasJabatan = realisasiKerjaPegawai.getTargetKerjaPegawai()
				.getKegiatanTugasJabatan();
		session.refresh(kegiatanTugasJabatan);
		Set<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans = kegiatanTugasJabatan
				.getKelompokParameterTambahanKegiatans();

		if (kelompokParameterTambahanKegiatans.isEmpty()) {
			return false;
		}

		int c = ((Number) session.createCriteria(ParameterTambahanKegiatan.class)
				.add(Restrictions.in("kelompokParameterTambahanKegiatan", kelompokParameterTambahanKegiatans))
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanKegiatan", "kelompokParameterTambahanKegiatan")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanKegiatan.aktif", true))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return c != 0;
	}

	/**
	 * Membangun ulang seluruh baris form parameter tambahan: menghapus baris lama, lalu untuk
	 * tiap {@link KelompokParameterTambahanKegiatan} aktif pada kegiatan tugas jabatan terkait,
	 * menambahkan label kelompok dan satu baris input per {@link ParameterTambahan} aktif
	 * (dibangun lewat {@link ParameterTambahan#initComponent}), mengisi nilai yang sudah tersimpan
	 * sebelumnya (bila ada) dari {@code parameterTambahanInds}. Dipanggil ulang setiap kali
	 * kegiatan/target kerja yang dipilih berubah (sebagai {@link EventListener}).
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		Session session = HibernateUtil.currentSession();
		KegiatanTugasJabatan kegiatanTugasJabatan = realisasiKerjaPegawai.getTargetKerjaPegawai()
				.getKegiatanTugasJabatan();
		session.refresh(kegiatanTugasJabatan);
		Set<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans = kegiatanTugasJabatan
				.getKelompokParameterTambahanKegiatans();

		if (kelompokParameterTambahanKegiatans.isEmpty()) {
			return;
		}

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiKerjaPegawai.populateParameterTambahan(parameterRows);
			}
		};

		for (KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan : kelompokParameterTambahanKegiatans) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setStyle("border:0px;background: transparent;");
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelBold(kelompokParameterTambahanKegiatan.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = session.createCriteria(ParameterTambahanKegiatan.class)
					.add(Restrictions.eq("kelompokParameterTambahanKegiatan", kelompokParameterTambahanKegiatan))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanKegiatan", "kelompokParameterTambahanKegiatan")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanKegiatan.aktif", true))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
			Collections.sort(parameterTambahans);

			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {
				boolean tampil = false;
				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(RealisasiKerjaPegawai.class,
						realisasiKerjaPegawai.getId(),
						kelompokParameterTambahanKegiatan.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanKegiatan", kelompokParameterTambahanKegiatan);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = realisasiKerjaPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/lkp/helper/ParameterTambahanKegiatanListener.java:214");

							}
						}
					}

					tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							realisasiKerjaPegawai.getId(), val, ket, parameterTambahan, isi);

				}

				rowParameterTambahan.setVisible(tampil);
			}
		}
	}
}
