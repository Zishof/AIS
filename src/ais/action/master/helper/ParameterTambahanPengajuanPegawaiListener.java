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
import ais.database.model.PengajuanPegawai;
import ais.database.model.KelompokParameterTambahanPengajuanPegawai;
import ais.database.model.ParameterTambahanPengajuanPegawai;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun, memvalidasi, dan menyimpan baris-baris form "parameter
 * tambahan" dinamis pada layar pengajuan pegawai ({@link PengajuanPegawai}). Parameter
 * tambahan dikelompokkan lewat {@link KelompokParameterTambahanPengajuanPegawai} (mis.
 * kelompok dokumen persyaratan tertentu); untuk setiap kelompok yang aktif dan punya
 * definisi {@link ParameterTambahan} aktif, satu baris judul kelompok dan satu baris input
 * per parameter dirender ke {@link Rows} form.
 *
 * <p>
 * Nilai tersimpan sebelumnya (bila ada) diambil dari kolom teks terserialisasi
 * {@code pengajuanPegawai.getParameterTambahanInds()} — format satu baris per entri,
 * dipisah {@code "\n"}, dengan kunci {@code "<idKelompok>-><idParameter>"} dan nilai
 * dipisah token {@code "<=>"}. Komponen input aktual (textbox/combobox/upload lampiran,
 * dsb.) dibangun oleh {@link ParameterTambahan#initComponent}.
 * </p>
 */
public class ParameterTambahanPengajuanPegawaiListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private PengajuanPegawai pengajuanPegawai;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais;

	/**
	 * Membuat listener untuk satu form pengajuan pegawai.
	 *
	 * @param pengajuanPegawai                              entitas pengajuan yang sedang diedit
	 * @param kelompokParameterTambahanPengajuanPegawais     kelompok parameter tambahan yang akan dirender
	 * @param parameterRows                                  daftar baris ZK hasil render, diisi/dibersihkan ulang oleh listener ini
	 * @param lampiranLains                                  peta lampiran yang sudah diunggah, berkunci {@code "idKelompok->idParameter"}
	 * @param rows                                            kontainer {@link Rows} tempat baris form disisipkan
	 */
	public ParameterTambahanPengajuanPegawaiListener(PengajuanPegawai pengajuanPegawai,
			Set<KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPengajuanPegawais = kelompokParameterTambahanPengajuanPegawais;
		this.rows = rows;
		this.pengajuanPegawai = pengajuanPegawai;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang dirender: parameter
	 * wajib diisi ({@code getWajibDiisi()}) harus punya nilai (bukan kosong/{@code null}
	 * literal), dan parameter yang mewajibkan lampiran ({@code getHarusMenyertakanLampiran()})
	 * harus sudah punya entri di {@link #lampiranLains}. Menampilkan messagebox
	 * peringatan pada pelanggaran pertama yang ditemukan dan langsung berhenti.
	 *
	 * @return {@code true} bila semua parameter valid (atau tidak ada baris sama sekali); {@code false} pada pelanggaran pertama
	 * @throws Exception diteruskan dari {@link ParameterTambahan#ambilVal(Row, ParameterTambahan)}
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai = (KelompokParameterTambahanPengajuanPegawai) row
					.getAttribute("kelompokParameterTambahanPengajuanPegawai");
			if (parameterTambahan != null && kelompokParameterTambahanPengajuanPegawai != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(PengajuanPegawai.class,
						pengajuanPegawai.getId(),
						kelompokParameterTambahanPengajuanPegawai.getId() + "->" + parameterTambahan.getId());

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

	/**
	 * Menyerap nilai-nilai yang saat ini terisi pada {@link #parameterRows} kembali ke
	 * {@code pengajuanPegawai} (menulis ulang kolom {@code parameterTambahanInds}).
	 * Dipanggil sebelum entitas pengajuan disimpan.
	 *
	 * @param pengajuanPegawai entitas pengajuan yang akan diisi ulang parameter tambahannya
	 */
	public void onSave(PengajuanPegawai pengajuanPegawai) {

		pengajuanPegawai.populateParameterTambahan(parameterRows);

	}

	/**
	 * Merender ulang seluruh baris parameter tambahan dari awal: baris-baris lama
	 * disembunyikan lalu dibuang dari {@link #parameterRows}, kemudian untuk tiap
	 * kelompok pada {@link #kelompokParameterTambahanPengajuanPegawais} query ulang
	 * daftar {@link ParameterTambahan} aktif miliknya, urutkan, dan bangun baris judul
	 * kelompok + baris input per parameter (nilai tersimpan sebelumnya diambil dari
	 * {@code pengajuanPegawai.getParameterTambahanInds()}). Baris judul kelompok hanya
	 * ditampilkan bila kelompok tersebut punya minimal satu parameter yang komponennya
	 * jadi terlihat ({@link ParameterTambahan#initComponent} mengembalikan {@code true}).
	 * Biasa dipicu oleh perubahan input yang memengaruhi parameter tambahan mana yang
	 * relevan (mis. ganti jenis pengajuan).
	 *
	 * @param event event ZK yang memicu render ulang (isinya tidak dipakai langsung)
	 * @throws Exception diteruskan dari pembangunan komponen input/akses Hibernate
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
				pengajuanPegawai.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai : kelompokParameterTambahanPengajuanPegawais) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanPengajuanPegawai.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanPengajuanPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai",
											kelompokParameterTambahanPengajuanPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPengajuanPegawai",
											"kelompokParameterTambahanPengajuanPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PengajuanPegawai.class,
						pengajuanPegawai.getId(),
						kelompokParameterTambahanPengajuanPegawai.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanPengajuanPegawai", kelompokParameterTambahanPengajuanPegawai);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = pengajuanPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPengajuanPegawaiListener.java:153");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							pengajuanPegawai.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
