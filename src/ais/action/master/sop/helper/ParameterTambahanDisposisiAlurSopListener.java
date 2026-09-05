package ais.action.master.sop.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.ParameterTambahanAlurSop;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun secara dinamis form "parameter tambahan" (field kustom yang
 * dikonfigurasi admin per {@link AlurSop}, dikelompokkan lewat
 * {@link KelompokParameterTambahanAlurSop}) pada layar disposisi alur SOP. Mengikuti pola yang
 * sama seperti
 * {@link ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener} untuk modul
 * Catatan Siswa, dengan tambahan dukungan mode {@code readonly}.
 *
 * <p>
 * Dipasang sebagai listener suatu event (mis. saat alur SOP dipilih): {@link
 * #onEvent(Event)} membaca kelompok parameter dari {@link AlurSop} terkait
 * {@code disposisiAlurSop}, membersihkan baris parameter lama, lalu untuk tiap kelompok
 * parameter aktif membangun ulang baris-baris input ({@link MyFormRow}) berdasarkan definisi
 * {@link ParameterTambahanAlurSop} yang aktif, mengisi nilai tersimpan (bila ada) dari kolom
 * {@code parameterTambahanInds} berformat baris {@code "kelompokId->parameterId<=>nilai<=>keterangan"}.
 * {@link #validate()} memeriksa field wajib isi dan lampiran wajib (dilewati bila
 * {@code readonly}); {@link #onSave(DisposisiAlurSop)} menuliskan kembali nilai form ke entitas
 * {@link DisposisiAlurSop}.
 * </p>
 */
public class ParameterTambahanDisposisiAlurSopListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private DisposisiAlurSop disposisiAlurSop;
	private Map<String, LampiranLain> lampiranLains;
	private boolean readonly;

	/**
	 * Membangun listener untuk satu instance form disposisi alur SOP.
	 *
	 * @param disposisiAlurSop entitas disposisi yang sedang diisi/diedit, sumber referensi ke {@link AlurSop}
	 * @param parameterRows    daftar baris ZK yang dikelola listener (diisi/dibersihkan di tempat)
	 * @param lampiranLains    peta lampiran yang sudah diunggah, berkunci {@code "kelompokId->parameterId"}
	 * @param rows             kontainer ZK {@link Rows} tempat baris parameter disisipkan
	 * @param readonly         bila {@code true}, form dirender read-only dan {@link #validate()} selalu lolos
	 */
	public ParameterTambahanDisposisiAlurSopListener(DisposisiAlurSop disposisiAlurSop, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, boolean readonly) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.disposisiAlurSop = disposisiAlurSop;
		this.lampiranLains = lampiranLains;
		this.readonly = readonly;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter wajib isi
	 * harus memiliki nilai, dan parameter yang mewajibkan lampiran harus sudah memiliki lampiran
	 * terunggah. Selalu lolos tanpa memeriksa apa pun bila {@link #readonly} bernilai
	 * {@code true}. Menampilkan pesan informasi dan menghentikan pemeriksaan pada pelanggaran
	 * pertama yang ditemukan.
	 *
	 * @return {@code true} bila seluruh parameter valid (atau readonly/tidak ada parameter), {@code false} bila ada pelanggaran
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty() || readonly) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop = (KelompokParameterTambahanAlurSop) row
					.getAttribute("kelompokParameterTambahanAlurSop");
			if (parameterTambahan != null && kelompokParameterTambahanAlurSop != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(DisposisiAlurSop.class,
						disposisiAlurSop.getId(),
						kelompokParameterTambahanAlurSop.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Mohon maaf, pilihan \"" + parameterTambahan.getLabelInputan() + "\" belum dipilih. Langkah yang dapat dilakukan: (1) periksa kolom \"" + parameterTambahan.getLabelInputan() + "\" dan pilih nilai yang sesuai; (2) pastikan semua field wajib telah diisi; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran() && !lampiranLains.keySet().contains(jenis)) {
						MyMessageboxConfig.show(
								"Mohon maaf, untuk pilihan \"" + parameterTambahan.getLabelInputan()
										+ "\", lampiran wajib diunggah. Langkah yang dapat dilakukan: (1) klik tombol unggah pada kolom \"" + parameterTambahan.getLabelInputan() + "\"; (2) pilih berkas lampiran yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}

		}
		return true;
	}

	/** Menuliskan kembali nilai-nilai yang sudah diisi pada {@link #parameterRows} ke entitas {@code disposisiAlurSop} sebelum disimpan. */
	public void onSave(DisposisiAlurSop disposisiAlurSop) {

		disposisiAlurSop.populateParameterTambahan(parameterRows);

	}

	private boolean tampil = false;

	/** Mengembalikan {@code true} bila kelompok parameter yang terakhir diproses {@link #onEvent(Event)} menghasilkan minimal satu baris parameter yang benar-benar dirender. */
	public boolean getTampil() {
		return tampil;
	}

	/**
	 * Membangun ulang seluruh baris input parameter tambahan berdasarkan {@link AlurSop} yang
	 * dirujuk {@link #disposisiAlurSop}: menyembunyikan/membersihkan baris lama, lalu untuk
	 * setiap kelompok parameter pada alur SOP tersebut menambahkan judul kelompok dan baris
	 * input per parameter aktif (diurutkan), mengisi nilai tersimpan dari
	 * {@code disposisiAlurSop.getParameterTambahanInds()} bila ada, menghormati flag
	 * {@link #readonly} saat merender tiap komponen. Sesi Hibernate yang dibuka di sini selalu
	 * ditutup di blok {@code finally}.
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
				disposisiAlurSop.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.openSession();
		try {
			AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
					.add(Restrictions.idEq(disposisiAlurSop.getAlurSop().getId())).uniqueResult();
			for (KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : alurSop
					.getKelompokParameterTambahanAlurSops()) {

				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setVisible(false);
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanAlurSop.getNama() + ""));
				parameterRows.add(rowParameterTambahan);

				List<ParameterTambahan> parameterTambahans = ConstantValues
						.simpleList(
								session.createCriteria(ParameterTambahanAlurSop.class)
										.add(Restrictions.eq("kelompokParameterTambahanAlurSop",
												kelompokParameterTambahanAlurSop))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanAlurSop", "kelompokParameterTambahanAlurSop")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanAlurSop.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				tampil = false;
				rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
				if (!parameterTambahans.isEmpty()) {

					for (ParameterTambahan parameterTambahan : parameterTambahans) {
						String jenis = LampiranLain.resolveJenisParameterTambahan(DisposisiAlurSop.class,
						disposisiAlurSop.getId(),
						kelompokParameterTambahanAlurSop.getId() + "->" + parameterTambahan.getId());

						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
						row.setValign("top");row.setAttribute("kelompokParameterTambahanAlurSop", kelompokParameterTambahanAlurSop);
						row.setParent(rows);
						row.appendChild(new Label(
								parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
						if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
							parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
						}
						String val = "";
						String ket = "";
						String[] spl = disposisiAlurSop.getParameterTambahanInds().split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
								val = value.length > 1 ? value[1].trim() : "";
								try {
									ket = value.length > 0 ? value[value.length - 1] : "";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ParameterTambahanDisposisiAlurSopListener.java:161");

								}
							}
						}

						tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
								disposisiAlurSop.getId(), val, ket, parameterTambahan, isi, readonly);

					}
				}

				rowParameterTambahan.setVisible(tampil);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			try { session.clear(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ParameterTambahanDisposisiAlurSopListener.java:178");}
		}
	}
}
