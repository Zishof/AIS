package ais.action.master.employ.helper;

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
import ais.database.model.employ.KelompokParameterTambahanCutiDanIzin;
import ais.database.model.employ.ParameterTambahanCutiDanIzin;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.CutiDanIzin;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener form dinamis untuk parameter tambahan pada pengajuan Cuti dan Izin pegawai. Mengikuti
 * pola parameter tambahan generik ({@link ParameterTambahan}): saat dipicu ({@link #onEvent}),
 * membangun ulang baris-baris input dinamis di grid {@code rows} sesuai kelompok parameter
 * tambahan aktif ({@link KelompokParameterTambahanCutiDanIzin}) yang berlaku untuk pengajuan cuti
 * ini, memuat nilai tersimpan sebelumnya (diparse dari string terserialisasi
 * {@code cutiDanIzin.getParameterTambahanInds()}), dan mendukung validasi wajib-isi/lampiran wajib
 * sebelum penyimpanan lewat {@link #validate()} serta pemetaan nilai kembali ke entitas lewat
 * {@link #onSave(CutiDanIzin)}. Pola tata letak baris ini sengaja disamakan dengan
 * {@code ParameterTambahanMahasiswaListener} (baris harus di-{@code setParent} secara eksplisit ke
 * grid; {@code initComponent()} sendiri hanya mengisi komponen ke dalam row).
 */
public class ParameterTambahanCutiDanIzinListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CutiDanIzin cutiDanIzin;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCutiDanIzin> kelompokParameterTambahanCutiDanIzins;

	/**
	 * Membuat listener untuk satu form pengajuan cuti/izin.
	 *
	 * @param cutiDanIzin                            entitas pengajuan cuti/izin yang sedang diedit
	 * @param kelompokParameterTambahanCutiDanIzins   kelompok parameter tambahan yang berlaku
	 * @param parameterRows                           daftar baris ZK dinamis yang dikelola listener ini (diisi/dibersihkan di {@link #onEvent})
	 * @param lampiranLains                           peta lampiran yang sudah diunggah, berkunci "kelompokId-&gt;parameterId"
	 * @param rows                                    grid ZK tempat baris parameter tambahan ditempelkan
	 */
	public ParameterTambahanCutiDanIzinListener(CutiDanIzin cutiDanIzin,
			Set<KelompokParameterTambahanCutiDanIzin> kelompokParameterTambahanCutiDanIzins, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCutiDanIzins = kelompokParameterTambahanCutiDanIzins;
		this.rows = rows;
		this.cutiDanIzin = cutiDanIzin;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter wajib diisi
	 * harus memiliki nilai (bukan kosong/{@code "null"}), dan parameter yang mewajibkan lampiran
	 * harus sudah memiliki entri di {@code lampiranLains}. Menampilkan {@link MyMessageboxConfig}
	 * dan menghentikan pemeriksaan pada pelanggaran pertama yang ditemukan.
	 *
	 * @return {@code true} bila seluruh parameter tambahan valid, {@code false} bila ada yang gagal
	 * @throws Exception diteruskan dari kegagalan membaca nilai baris
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin = (KelompokParameterTambahanCutiDanIzin) row
					.getAttribute("kelompokParameterTambahanCutiDanIzin");
			if (parameterTambahan != null && kelompokParameterTambahanCutiDanIzin != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CutiDanIzin.class, cutiDanIzin.getId(),
						kelompokParameterTambahanCutiDanIzin.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Mohon maaf, Pilihan \"" + parameterTambahan.getLabelInputan() + "\" belum dipilih. Langkah yang dapat dilakukan: (1) pilih nilai yang sesuai pada kolom \"" + parameterTambahan.getLabelInputan() + "\"; (2) pastikan pilihan wajib telah terisi sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
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

	/** Menyalin nilai-nilai yang sedang diisikan pada {@code parameterRows} kembali ke entitas {@code cutiDanIzin} (serialisasi ke {@code parameterTambahanInds}), dipanggil sebelum entitas disimpan. */
	public void onSave(CutiDanIzin cutiDanIzin) {

		cutiDanIzin.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan dinamis: menyembunyikan &amp; membersihkan
	 * baris lama, lalu untuk setiap kelompok parameter tambahan aktif yang berlaku, menambahkan
	 * baris judul kelompok dan baris input per parameter aktif (diurutkan), memuat nilai tersimpan
	 * dari {@code cutiDanIzin.getParameterTambahanInds()} bila ada, dan menempelkan setiap baris ke
	 * grid {@code rows}. Baris judul kelompok hanya ditampilkan bila minimal satu parameter di
	 * dalamnya benar-benar dirender ({@code tampil}).
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
				cutiDanIzin.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin : kelompokParameterTambahanCutiDanIzins) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCutiDanIzin.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCutiDanIzin.class)
									.add(Restrictions.eq("kelompokParameterTambahanCutiDanIzin",
											kelompokParameterTambahanCutiDanIzin))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCutiDanIzin",
											"kelompokParameterTambahanCutiDanIzin")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCutiDanIzin.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CutiDanIzin.class, cutiDanIzin.getId(),
						kelompokParameterTambahanCutiDanIzin.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanCutiDanIzin", kelompokParameterTambahanCutiDanIzin);
					// WAJIB: tempelkan baris input ke grid. initComponent() hanya mengisi komponen ke
					// dalam row & menambah row ke daftar parameterRows, TIDAK mem-parent row ke grid.
					// Tanpa baris ini, input parameter tambahan tidak pernah tampil (hanya judul
					// kelompok yang terlihat). Samakan dgn ParameterTambahanMahasiswaListener.
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = cutiDanIzin.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/ParameterTambahanCutiDanIzinListener.java:157");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							cutiDanIzin.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
