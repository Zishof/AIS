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
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.asset.ParameterTambahanPerbaikanAsset;
import ais.database.model.asset.PerbaikanAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Pengelola baris-baris parameter tambahan dinamis pada form {@link PerbaikanAsset} (perbaikan
 * aset/inventaris). Parameter tambahan dikelompokkan lewat
 * {@link KelompokParameterTambahanPerbaikanAsset} dan tiap kelompok dapat memiliki beberapa
 * {@link ParameterTambahan} (field dinamis: teks, pilihan, lampiran, dsb.) yang dikonfigurasi
 * dari master data, bukan hardcode di form. Kelas ini men-generate baris ({@link Row}) form
 * tersebut secara runtime, memvalidasi isian wajib/lampiran wajib, dan menuliskan hasil isian
 * kembali ke entitas {@link PerbaikanAsset} saat disimpan.
 *
 * <p>
 * Dipakai sebagai {@link EventListener} yang dipasang pada event pemicu (mis. perubahan jenis
 * kelompok aset) lewat {@link #onEvent(Event)} — setiap kali event terpicu, baris parameter
 * lama dikosongkan lalu dibangun ulang berdasarkan {@code kelompokParameterTambahanPerbaikanAssets}
 * yang aktif saat itu. Nilai isian sebelumnya (bila form sedang mode edit) dipulihkan dari
 * {@link PerbaikanAsset#getParameterTambahanInds()} yang berformat baris
 * {@code "kelompokId->parameterId<=>nilai<=>keterangan"} dipisah newline.
 * </p>
 */
public class ParameterTambahanPerbaikanAssetListener implements EventListener {

	/** Daftar baris ZK berisi seluruh baris parameter tambahan yang sedang dirender/dikelola listener ini; dibaca ulang oleh {@link #validate()}, {@link #onSave(PerbaikanAsset)}, dan {@link #onEvent(Event)}. */
	private List<Row> parameterRows;
	/** Komponen {@link Rows} induk (form ZK) tempat baris-baris parameter tambahan ditambahkan. */
	private Rows rows;
	/** Entitas perbaikan aset yang formulir parameter tambahannya sedang dikelola oleh listener ini. */
	private PerbaikanAsset perbaikanAsset;
	/** Peta lampiran yang sudah diunggah, dikunci per jenis parameter tambahan (lihat {@link LampiranLain#resolveJenisParameterTambahan}), diteruskan ke {@link ParameterTambahan#initComponent}. */
	private Map<String, LampiranLain> lampiranLains;
	/** Kelompok parameter tambahan yang berlaku untuk jenis aset yang dipilih. */
	private Set<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets;

	/**
	 * @param perbaikanAsset                             entitas perbaikan aset yang sedang diedit
	 * @param kelompokParameterTambahanPerbaikanAssets    kelompok parameter tambahan yang berlaku
	 *                                                     untuk jenis aset yang dipilih
	 * @param parameterRows                               daftar baris form yang dikelola bersama
	 *                                                     (diisi/dikosongkan oleh listener ini)
	 * @param lampiranLains                                lampiran yang sudah diunggah, dikunci per
	 *                                                     {@code "kelompokId->parameterId"}
	 * @param rows                                         kontainer ZK tempat baris parameter
	 *                                                     ditambahkan
	 */
	public ParameterTambahanPerbaikanAssetListener(PerbaikanAsset perbaikanAsset,
			Set<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPerbaikanAssets = kelompokParameterTambahanPerbaikanAssets;
		this.rows = rows;
		this.perbaikanAsset = perbaikanAsset;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter yang
	 * ditandai {@link ParameterTambahan#getWajibDiisi()} harus memiliki nilai, dan parameter
	 * yang mensyaratkan lampiran ({@link ParameterTambahan#getLampiranWajibDiisi()} dan
	 * {@link ParameterTambahan#getHarusMenyertakanLampiran()}) harus punya entri di
	 * {@code lampiranLains}. Menampilkan {@link MyMessageboxConfig} begitu menemukan pelanggaran
	 * pertama dan langsung berhenti (tidak mengumpulkan semua error sekaligus).
	 *
	 * @return {@code true} bila semua baris valid (atau tidak ada baris parameter sama sekali);
	 *         {@code false} begitu ditemukan satu pelanggaran
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset = (KelompokParameterTambahanPerbaikanAsset) row
					.getAttribute("kelompokParameterTambahanPerbaikanAsset");
			if (parameterTambahan != null && kelompokParameterTambahanPerbaikanAsset != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(PerbaikanAsset.class,
						perbaikanAsset.getId(),
						kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId());

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

	/** Menulis nilai isian dari {@code parameterRows} saat ini ke entitas {@code perbaikanAsset} yang diberikan (biasanya dipanggil saat form disimpan). */
	public void onSave(PerbaikanAsset perbaikanAsset) {

		perbaikanAsset.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan pada form: mengosongkan baris lama,
	 * lalu untuk tiap kelompok pada {@code kelompokParameterTambahanPerbaikanAssets}, mengambil
	 * daftar {@link ParameterTambahan} aktif yang terhubung ke kelompok tersebut (via
	 * {@link ParameterTambahanPerbaikanAsset}), membuat satu baris judul kelompok dan satu baris
	 * per parameter (memakai {@link ParameterTambahan#initComponent}), memulihkan nilai
	 * tersimpan sebelumnya dari {@link PerbaikanAsset#getParameterTambahanInds()}, dan
	 * menyembunyikan baris judul kelompok bila tidak ada satu pun parameter yang tampil.
	 * Dipicu ulang setiap kali event pemilihan yang relevan (mis. jenis aset) berubah.
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
				perbaikanAsset.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset : kelompokParameterTambahanPerbaikanAssets) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanPerbaikanAsset.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanPerbaikanAsset.class)
									.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset",
											kelompokParameterTambahanPerbaikanAsset))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPerbaikanAsset",
											"kelompokParameterTambahanPerbaikanAsset")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PerbaikanAsset.class,
						perbaikanAsset.getId(),
						kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanPerbaikanAsset", kelompokParameterTambahanPerbaikanAsset);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = perbaikanAsset.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPerbaikanAssetListener.java:153");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							perbaikanAsset.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
