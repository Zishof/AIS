package ais.action.master.payroll.helper;

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
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.ParameterTambahanGajiPegawai;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Event listener ZK yang membangun form dinamis "parameter tambahan gaji pegawai" pada layar
 * pegawai/penggajian: untuk setiap {@link KelompokParameterTambahanGajiPegawai} aktif yang berlaku
 * bagi pegawai, listener merender baris judul kelompok diikuti baris input per
 * {@link ParameterTambahan} aktif dalam kelompok tersebut (komponen input dibangun oleh
 * {@link ParameterTambahan#initComponent}, sesuai tipe input yang dikonfigurasi per parameter),
 * mem-prefill nilai dari string terserialisasi {@code parameterTambahanInds} milik
 * {@link Pegawai} (format baris {@code "kelompokId->parameterId<=>nilai<=>keterangan"}), dan
 * menyembunyikan kelompok yang tidak menghasilkan input apa pun. Juga menyediakan
 * {@link #validate()} untuk memastikan parameter wajib diisi terisi dan lampiran wajib sudah
 * diunggah sebelum data disimpan, serta {@link #onSave(Pegawai)} untuk menuliskan kembali nilai
 * form ke entitas pegawai.
 */
public class ParameterTambahanGajiPegawaiListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private Pegawai gajiPegawai;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanGajiPegawai> kelompokParameterTambahanGajiPegawais;

	/**
	 * Membuat listener terikat pada satu pegawai dan komponen ZK target.
	 *
	 * @param gajiPegawai                            pegawai yang parameter tambahannya ditampilkan/diedit
	 * @param kelompokParameterTambahanGajiPegawais  kelompok parameter yang relevan untuk pegawai ini
	 * @param parameterRows                          daftar baris komponen dinamis yang dibangun, diisi/dibersihkan oleh listener
	 * @param lampiranLains                          peta lampiran yang sudah diunggah, berkunci {@code "kelompokId->parameterId"}
	 * @param rows                                   komponen {@link Rows} ZK tempat baris form dipasang
	 */
	public ParameterTambahanGajiPegawaiListener(Pegawai gajiPegawai,
			Set<KelompokParameterTambahanGajiPegawai> kelompokParameterTambahanGajiPegawais, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanGajiPegawais = kelompokParameterTambahanGajiPegawais;
		this.rows = rows;
		this.gajiPegawai = gajiPegawai;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang dirender: menolak (menampilkan pesan
	 * dan mengembalikan {@code false}) bila ada parameter wajib diisi yang masih kosong, atau
	 * parameter yang mewajibkan lampiran namun lampirannya belum diunggah.
	 *
	 * @return {@code true} bila seluruh parameter memenuhi aturan wajib isi/lampiran, {@code false} sebaliknya
	 * @throws Exception diteruskan apa adanya dari kegagalan pembacaan nilai komponen
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai = (KelompokParameterTambahanGajiPegawai) row
					.getAttribute("kelompokParameterTambahanGajiPegawai");
			if (parameterTambahan != null && kelompokParameterTambahanGajiPegawai != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(Pegawai.class, gajiPegawai.getId(),
						kelompokParameterTambahanGajiPegawai.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Mohon maaf, pilihan \"" + parameterTambahan.getLabelInputan() + "\" belum dipilih. Langkah yang dapat dilakukan: (1) pilih nilai yang sesuai pada kolom tersebut; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini. Jika masih mengalami kendala, hubungi Administrator.",
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

	/**
	 * Menuliskan kembali nilai-nilai form parameter tambahan yang sedang ditampilkan ke entitas
	 * pegawai (mendelegasikan ke {@link Pegawai#populateParameterTambahan(List)}), biasanya dipanggil
	 * sesaat sebelum penyimpanan.
	 *
	 * @param gajiPegawai pegawai target penulisan nilai parameter tambahan
	 */
	public void onSave(Pegawai gajiPegawai) {

		gajiPegawai.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris form parameter tambahan: membersihkan baris lama, lalu untuk
	 * setiap kelompok parameter aktif merender baris judul kelompok dan baris input per parameter
	 * aktif dalam kelompok, mem-prefill nilai dari data tersimpan pada {@code gajiPegawai}, dan
	 * menyembunyikan kelompok yang tidak memiliki parameter aktif apa pun untuk ditampilkan.
	 *
	 * @param event event ZK pemicu pembangunan ulang (mis. perubahan kelompok/jabatan pegawai)
	 * @throws Exception diteruskan apa adanya dari kegagalan query atau pembangunan komponen
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
				gajiPegawai.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : kelompokParameterTambahanGajiPegawais) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanGajiPegawai.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanGajiPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanGajiPegawai",
											kelompokParameterTambahanGajiPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanGajiPegawai",
											"kelompokParameterTambahanGajiPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanGajiPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(Pegawai.class, gajiPegawai.getId(),
						kelompokParameterTambahanGajiPegawai.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanGajiPegawai", kelompokParameterTambahanGajiPegawai);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = gajiPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/ParameterTambahanGajiPegawaiListener.java:153");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							gajiPegawai.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
