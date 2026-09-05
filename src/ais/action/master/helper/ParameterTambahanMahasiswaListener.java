package ais.action.master.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.KelompokParameterTambahanMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * {@link EventListener} yang membangun dan mengelola blok "parameter tambahan" pada formulir
 * biodata mahasiswa ({@link BiodataMahasiswa}) — field kustom dinamis (per {@link
 * ParameterTambahan}, dikelompokkan oleh {@link KelompokParameterTambahanMahasiswa}) yang dapat
 * dikonfigurasi admin, mis. pertanyaan tambahan spesifik institusi beserta lampiran
 * pendukungnya. Field yang ditampilkan disaring berdasarkan tahun angkatan mahasiswa (parameter
 * yang ditandai {@code tampilDiSemuaTahunAngkatan} atau yang daftar {@code tahunAngkatans}-nya
 * memuat angkatan mahasiswa tersebut) dan status aktif kelompok/parameternya.
 *
 * <p>
 * Nilai tersimpan sebagai teks terserialisasi baris-per-baris pada
 * {@code biodataMahasiswa.getParameterTambahanInds()}, dengan format
 * {@code "<idKelompok>-><idParameter><=>nilai<=>...<=>keterangan"} per baris — di-parse manual
 * lewat {@code split("\n")}/{@code split("<=>")} baik saat merender nilai awal field ({@link
 * #onEvent}) maupun saat memvalidasi ({@link #validate}).
 * </p>
 *
 * <p>
 * {@link #onEvent} (dipasang sebagai listener perubahan konteks, mis. saat tahun angkatan
 * berubah) membangun ulang seluruh baris field dari awal; {@link #onSave} menyerahkan proses
 * pengumpulan nilai balik ke {@link BiodataMahasiswa#populateParameterTambahan}; {@link
 * #validate} adalah method statis independen (tidak memerlukan instance dibangun via {@link
 * #onEvent} lebih dulu) yang memeriksa parameter wajib diisi/wajib lampiran dan menampilkan
 * pesan peringatan bila ada yang belum lengkap; {@link #check} hanya memeriksa apakah ada
 * parameter tambahan relevan sama sekali untuk mahasiswa ini (dipakai untuk menyembunyikan
 * seluruh blok bila tidak ada konfigurasi yang berlaku).
 * </p>
 */
public class ParameterTambahanMahasiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private BiodataMahasiswa biodataMahasiswa;
	private Map<String, LampiranLain> lampiranLains;

	/**
	 * @param biodataMahasiswa biodata mahasiswa yang parameter tambahannya dikelola
	 * @param parameterRows    daftar baris ZK yang dibangun/dikelola listener ini (dibaca ulang oleh {@link #onSave})
	 * @param lampiranLains    peta lampiran yang sudah diunggah per kunci parameter, diteruskan ke {@link ParameterTambahan#initComponent}
	 * @param rows             komponen {@link Rows} induk tempat baris field ditambahkan
	 */
	public ParameterTambahanMahasiswaListener(BiodataMahasiswa biodataMahasiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.biodataMahasiswa = biodataMahasiswa;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi bahwa seluruh parameter tambahan wajib (aktif, relevan dengan tahun angkatan
	 * mahasiswa) sudah diisi — termasuk lampiran wajib bila parameter mensyaratkannya. Berhenti
	 * pada kegagalan validasi pertama yang ditemukan (tidak mengumpulkan semua kegagalan
	 * sekaligus).
	 *
	 * @param biodataMahasiswa data yang divalidasi; {@code null} langsung dianggap valid
	 * @param eventListener    dipanggil setelah pesan peringatan ditutup (bila {@code tampilMessage}) atau langsung (bila tidak) pada kegagalan validasi
	 * @param tampilMessage    bila {@code true}, tampilkan {@link MyMessageboxConfig} peringatan; bila {@code false}, langsung panggil {@code eventListener} tanpa dialog
	 * @return {@code true} bila semua parameter wajib terpenuhi; {@code false} bila ada yang belum
	 */
	@SuppressWarnings("unchecked")
	public static boolean validate(BiodataMahasiswa biodataMahasiswa, EventListener eventListener,
			final Boolean tampilMessage) throws Exception {
		if (biodataMahasiswa == null) {
			return true;
		}
		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		List<ParameterTambahanMahasiswa> parameterTambahanMahasiswas = null;
		try {
			Session session = HibernateUtil.currentNativeSession();
			parameterTambahanMahasiswas = ConstantValues.simpleList(
					session.createCriteria(ParameterTambahanMahasiswa.class)
							.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
									gel == null ? Restrictions.sqlRestriction("false")
											: Restrictions.ilike("tahunAngkatans", ";" + gel + ";",
													MatchMode.ANYWHERE)))
							.createAlias("parameterTambahan", "parameterTambahan")
							.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
							.add(Restrictions.eq("parameterTambahan.aktif", true))
							.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true)),
					ParameterTambahanMahasiswa.class);
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ParameterTambahanMahasiswaListener.java:68");
		}
		HibernateUtil.closeSession();

		if (parameterTambahanMahasiswas == null) {
			return true;
		}

		for (ParameterTambahanMahasiswa row : parameterTambahanMahasiswas) {
			ParameterTambahan parameterTambahan = row.getParameterTambahan();
			KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa = row
					.getKelompokParameterTambahanMahasiswa();
			if (parameterTambahan != null && kelompokParameterTambahanMahasiswa != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataMahasiswa.class,
						biodataMahasiswa.getId(),
						kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId());

				String val = "";
				String[] spl = biodataMahasiswa.getParameterTambahanInds().split("\n");
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
					if (tampilMessage) {
						MyMessageboxConfig.show(
								"\"" + kelompokParameterTambahanMahasiswa.getNama()
										+ "\" harus Anda lengkapi !\n\nPilihan \"" + parameterTambahan.getLabelInputan()
										+ "\" harus dipilih",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, eventListener);
					} else {
						if (eventListener != null) {
							eventListener.onEvent(null);
						}
					}
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(biodataMahasiswa.getId(), jenis);

						if (lam == null) {
							if (tampilMessage) {
								MyMessageboxConfig.show(
										"\"" + kelompokParameterTambahanMahasiswa.getNama()
												+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
												+ parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										eventListener);
							} else {
								if (eventListener != null) {
									eventListener.onEvent(null);
								}
							}
							return false;
						}

					}
				}
			}
		}
		return true;
	}

	/** Mengumpulkan nilai seluruh field parameter tambahan yang sedang ditampilkan kembali ke {@code biodataMahasiswa}, lewat {@link BiodataMahasiswa#populateParameterTambahan}. */
	public void onSave(BiodataMahasiswa biodataMahasiswa) {
		biodataMahasiswa.populateParameterTambahan(parameterRows);
	}

	/** @return {@code true} bila ada minimal satu parameter tambahan aktif yang relevan untuk tahun angkatan {@link #biodataMahasiswa}; {@code false} bila tidak ada (blok dapat disembunyikan). */
	public boolean check() {
		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		int c = ((Number) HibernateUtil.currentSession().createCriteria(ParameterTambahanMahasiswa.class)
				.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
						gel == null ? Restrictions.sqlRestriction("false")
								: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return c != 0;
	}

	/**
	 * Membangun ulang dari awal seluruh baris field parameter tambahan: menghapus baris lama
	 * ({@link #parameterRows}), lalu untuk tiap kelompok parameter relevan (aktif, sesuai tahun
	 * angkatan) membuat baris judul kelompok diikuti satu baris per {@link ParameterTambahan}
	 * aktif di dalamnya — komponen input tiap field dibangun oleh {@link
	 * ParameterTambahan#initComponent}, diisi nilai awal yang diurai dari
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

		Session session = HibernateUtil.currentSession();

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		List<KelompokParameterTambahanMahasiswa> kelompokParameterTambahanMahasiswas = session
				.createCriteria(ParameterTambahanMahasiswa.class)
				.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
						gel == null ? Restrictions.sqlRestriction("false")
								: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
				.setProjection(Projections.groupProperty("kelompokParameterTambahanMahasiswa")).list();
		Collections.sort(kelompokParameterTambahanMahasiswas);

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				biodataMahasiswa.populateParameterTambahan(parameterRows);
			}
		};

		for (KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa : kelompokParameterTambahanMahasiswas) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setStyle("border:0px;background: transparent;");
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelBold(kelompokParameterTambahanMahasiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanMahasiswa.class)
									.add(Restrictions.eq("kelompokParameterTambahanMahasiswa",
											kelompokParameterTambahanMahasiswa))
									.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
											gel == null ? Restrictions.sqlRestriction("false")
													: Restrictions.ilike("tahunAngkatans", ";" + gel + ";",
															MatchMode.ANYWHERE)))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanMahasiswa",
											"kelompokParameterTambahanMahasiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);
			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataMahasiswa.class,
						biodataMahasiswa.getId(),
						kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanMahasiswa", kelompokParameterTambahanMahasiswa);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = biodataMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanMahasiswaListener.java:244");

							}
						}
					}

					tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							biodataMahasiswa.getId(), val, ket, parameterTambahan, isi);
				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
