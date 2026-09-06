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
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.CatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.ParameterTambahanCatatanAdministrasi;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * {@link EventListener} yang membangun dan mengelola blok "parameter tambahan" pada formulir
 * {@link CatatanAdministrasi} — field kustom dinamis (per {@link ParameterTambahan},
 * dikelompokkan oleh {@link KelompokParameterTambahanCatatanAdministrasi}) yang dapat
 * dikonfigurasi admin untuk melengkapi catatan administrasi dengan data tambahan spesifik
 * institusi beserta lampiran pendukungnya. Sepasang dengan {@link
 * ParameterTambahanMahasiswaListener} yang menangani pola serupa untuk biodata mahasiswa; namun
 * berbeda dari kelas itu, kelompok parameter yang relevan di sini diterima sebagai parameter
 * konstruktor ({@link #kelompokParameterTambahanCatatanAdministrasis}), bukan ditentukan
 * otomatis dari tahun angkatan.
 *
 * <p>
 * Nilai tersimpan sebagai teks terserialisasi baris-per-baris pada
 * {@code catatanAdministrasi.getParameterTambahanInds()}, format
 * {@code "<idKelompok>-><idParameter><=>nilai<=>...<=>keterangan"} per baris, diurai manual
 * ({@code split("\n")}/{@code split("<=>")}) saat merender nilai awal field di {@link #onEvent}.
 * {@link #validate}, berbeda dari {@link ParameterTambahanMahasiswaListener#validate}, membaca
 * nilai langsung dari komponen ZK yang sudah dirender ({@link ParameterTambahan#ambilVal})
 * alih-alih dari teks tersimpan, sehingga hanya valid dipanggil setelah {@link #onEvent}
 * membangun baris-barisnya.
 * </p>
 */
public class ParameterTambahanCatatanAdministrasiListener implements EventListener {

	/** Daftar baris ZK berisi seluruh baris parameter tambahan yang sedang dirender/dikelola listener ini; dibaca ulang oleh {@link #validate()}, {@link #onSave(CatatanAdministrasi)}, dan {@link #onEvent(Event)}. */
	private List<Row> parameterRows;
	/** Komponen {@link Rows} induk (form ZK) tempat baris-baris parameter tambahan ditambahkan. */
	private Rows rows;
	/** Entitas catatan administrasi yang formulir parameter tambahannya sedang dikelola oleh listener ini. */
	private CatatanAdministrasi catatanAdministrasi;
	/** Peta lampiran yang sudah diunggah, dikunci per jenis parameter tambahan (lihat {@link LampiranLain#resolveJenisParameterTambahan}), diteruskan ke {@link ParameterTambahan#initComponent}. */
	private Map<String, LampiranLain> lampiranLains;
	/** Kelompok-kelompok parameter tambahan yang relevan untuk konteks ini (ditentukan pemanggil, bukan otomatis -- lihat javadoc kelas). */
	private Set<KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis;

	/**
	 * @param catatanAdministrasi                             catatan administrasi yang parameter tambahannya dikelola
	 * @param kelompokParameterTambahanCatatanAdministrasis    kelompok parameter yang relevan untuk konteks ini (ditentukan pemanggil, bukan otomatis)
	 * @param parameterRows                                    daftar baris ZK yang dibangun/dikelola listener ini (dibaca ulang oleh {@link #onSave}/{@link #validate})
	 * @param lampiranLains                                    peta lampiran yang sudah diunggah per kunci parameter, diteruskan ke {@link ParameterTambahan#initComponent}
	 * @param rows                                              komponen {@link Rows} induk tempat baris field ditambahkan
	 */
	public ParameterTambahanCatatanAdministrasiListener(CatatanAdministrasi catatanAdministrasi,
			Set<KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanAdministrasis = kelompokParameterTambahanCatatanAdministrasis;
		this.rows = rows;
		this.catatanAdministrasi = catatanAdministrasi;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi bahwa seluruh parameter tambahan wajib pada baris-baris yang sudah dirender
	 * (lihat {@link #onEvent}) sudah diisi, termasuk lampiran wajib bila disyaratkan. Berhenti
	 * pada kegagalan validasi pertama yang ditemukan.
	 *
	 * @return {@code true} bila semua parameter wajib terpenuhi (atau belum ada baris sama sekali); {@code false} bila ada yang belum lengkap (pesan sudah ditampilkan)
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanCatatanAdministrasi) row
					.getAttribute("kelompokParameterTambahanCatatanAdministrasi");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanAdministrasi != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanAdministrasi.class,
						catatanAdministrasi.getId(),
						kelompokParameterTambahanCatatanAdministrasi.getId() + "->" + parameterTambahan.getId());

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

	/** Mengumpulkan nilai seluruh field parameter tambahan yang sedang ditampilkan kembali ke {@code catatanAdministrasi}, lewat {@link CatatanAdministrasi#populateParameterTambahan}. */
	public void onSave(CatatanAdministrasi catatanAdministrasi) {

		catatanAdministrasi.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang dari awal seluruh baris field parameter tambahan: menghapus baris lama
	 * ({@link #parameterRows}), lalu untuk tiap kelompok dalam {@link
	 * #kelompokParameterTambahanCatatanAdministrasis} membuat baris judul kelompok diikuti satu
	 * baris per {@link ParameterTambahan} aktif di dalamnya — komponen input tiap field dibangun
	 * oleh {@link ParameterTambahan#initComponent}, diisi nilai awal yang diurai dari
	 * {@code parameterTambahanInds}. Baris judul kelompok hanya ditampilkan bila minimal satu
	 * field di dalamnya nyata dirender.
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
				catatanAdministrasi.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : kelompokParameterTambahanCatatanAdministrasis) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanAdministrasi.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi",
											kelompokParameterTambahanCatatanAdministrasi))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanAdministrasi",
											"kelompokParameterTambahanCatatanAdministrasi")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanAdministrasi.class,
						catatanAdministrasi.getId(),
						kelompokParameterTambahanCatatanAdministrasi.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanAdministrasi", kelompokParameterTambahanCatatanAdministrasi);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanAdministrasi.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanCatatanAdministrasiListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanAdministrasi.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
