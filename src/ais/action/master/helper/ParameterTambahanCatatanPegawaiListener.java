package ais.action.master.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CatatanPegawai;
import ais.database.model.KelompokParameterTambahanCatatanPegawai;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanPegawai;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * {@link EventListener} yang membangun secara dinamis input-input "parameter tambahan" pada form
 * {@link CatatanPegawai}, dikelompokkan menurut {@link KelompokParameterTambahanCatatanPegawai}.
 * Dipasang sebagai listener pemicu (mis. saat kelompok/jenis catatan berubah); setiap kali
 * dipanggil ({@link #onEvent}), seluruh baris parameter lama dihapus dan dibangun ulang dari
 * konfigurasi {@link ParameterTambahanCatatanPegawai} yang aktif untuk setiap kelompok, dengan nilai
 * yang sudah tersimpan (bila ada) diurai dari string terserialisasi
 * {@code catatanPegawai.getParameterTambahanInds()} (format baris {@code "kelompokId->parameterId<=>nilai<=>keterangan"}).
 * Komponen input aktual dibangun oleh {@code ParameterTambahan.initComponent} generik (dipakai
 * bersama oleh listener sejenis lain, mis. {@link ParameterTambahanPengaduanListener}).
 *
 * <p>
 * {@link #validate()} harus dipanggil sebelum simpan formulir: memeriksa parameter wajib diisi sudah
 * terisi dan lampiran wajib (bila diminta) sudah diunggah, menampilkan dialog peringatan dan
 * menghentikan proses simpan bila ada yang belum lengkap. {@link #onSave(CatatanPegawai)} menuliskan
 * nilai-nilai terisi kembali ke entitas {@link CatatanPegawai} yang diberikan.
 * </p>
 */
public class ParameterTambahanCatatanPegawaiListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CatatanPegawai catatanPegawai;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCatatanPegawai> kelompokParameterTambahanCatatanPegawais;

	/**
	 * @param catatanPegawai                            entitas catatan pegawai yang formulirnya dibangun
	 * @param kelompokParameterTambahanCatatanPegawais  kelompok-kelompok parameter yang ditampilkan
	 * @param parameterRows                              list keluaran yang diisi baris-baris parameter yang dibangun (dipakai bersama pemanggil untuk validasi/simpan)
	 * @param lampiranLains                               peta lampiran yang sudah diunggah, dikunci per "kelompokId->parameterId"
	 * @param rows                                        container {@link Rows} tempat baris parameter ditambahkan
	 */
	public ParameterTambahanCatatanPegawaiListener(CatatanPegawai catatanPegawai,
			Set<KelompokParameterTambahanCatatanPegawai> kelompokParameterTambahanCatatanPegawais, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanPegawais = kelompokParameterTambahanCatatanPegawais;
		this.rows = rows;
		this.catatanPegawai = catatanPegawai;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter yang sedang dibangun: parameter wajib-diisi harus memiliki
	 * nilai, dan parameter yang mewajibkan lampiran harus sudah memiliki entri di {@code lampiranLains}.
	 * Menampilkan dialog peringatan berisi label parameter yang gagal validasi (validasi berhenti pada
	 * kegagalan pertama).
	 *
	 * @return {@code true} bila seluruh parameter valid; {@code false} bila ada yang gagal
	 * @throws Exception diteruskan dari kegagalan menampilkan dialog
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai = (KelompokParameterTambahanCatatanPegawai) row
					.getAttribute("kelompokParameterTambahanCatatanPegawai");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanPegawai != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanPegawai.class,
						catatanPegawai.getId(),
						kelompokParameterTambahanCatatanPegawai.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Pilihan \"" + parameterTambahan.getLabelInputan() + "\" harus dipilih",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran() && !lampiranLains.keySet().contains(jenis)) {
						MyMessageboxConfig.show(
								"Untuk pilihan \"" + parameterTambahan.getLabelInputan()
										+ "\", lampiran harus di-upload",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Menuliskan nilai-nilai parameter tambahan yang sedang terisi (dari {@code parameterRows}) ke entitas {@code catatanPegawai} yang diberikan, siap disimpan. */
	public void onSave(CatatanPegawai catatanPegawai) {

		catatanPegawai.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan: menghapus baris lama, lalu untuk setiap
	 * kelompok mengambil parameter yang aktif dan dikonfigurasi untuk kelompok tersebut, mengurai
	 * nilai tersimpan dari {@code catatanPegawai.getParameterTambahanInds()}, dan membangun komponen
	 * inputnya lewat {@code ParameterTambahan.initComponent}. Baris header kelompok hanya ditampilkan
	 * bila ada parameter yang berhasil dibangun untuk kelompok tersebut.
	 *
	 * @param event event pemicu (tidak dipakai isinya)
	 * @throws Exception diteruskan dari kegagalan Hibernate atau pembangunan komponen
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				catatanPegawai.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai : kelompokParameterTambahanCatatanPegawais) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanPegawai.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai",
											kelompokParameterTambahanCatatanPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanPegawai",
											"kelompokParameterTambahanCatatanPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanPegawai.class,
						catatanPegawai.getId(),
						kelompokParameterTambahanCatatanPegawai.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanPegawai", kelompokParameterTambahanCatatanPegawai);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanCatatanPegawaiListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanPegawai.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
