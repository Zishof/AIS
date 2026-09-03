package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.KelompokCalonMahasiswaAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.KelompokCalonMahasiswa;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Paket;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;

/**
 * Event listener ZK yang membangun form dinamis "parameter tambahan" pada formulir PMB (pendaftaran
 * calon mahasiswa/siswa baru), padanan {@code ParameterTambahanGajiPegawaiListener} untuk konteks
 * PMB. Kelompok parameter yang ditampilkan diresolusi berlapis: bila {@link GelombangPendaftaran}
 * yang dipilih punya daftar kelompok spesifik yang ditetapkan admin, HANYA daftar itu yang dipakai
 * (mengabaikan flag tampil per form); jika tidak, kelompok/parameter diresolusi dari
 * {@link ParameterTambahanPaket} yang cocok dengan paket dan gelombang terpilih (atau berlaku untuk
 * semua gelombang), difilter lagi oleh flag {@code tampilDiFormPendaftaran}/
 * {@code tampilDiFormSetelahLogin} sesuai konteks form ({@link #formPendaftaran}). Form tidak
 * ditampilkan sama sekali bila gelombang terpilih mengatur form tambahan dinonaktifkan untuk
 * konteks yang relevan. Sama seperti listener gaji pegawai, nilai di-prefill dari string
 * terserialisasi {@code parameterTambahanInds} milik {@link BiodataCalonMahasiswa}, komponen input
 * dibangun lewat {@link ParameterTambahan#initComponent}, dan {@link #validate()} memastikan
 * parameter wajib serta lampiran wajib terisi sebelum data disimpan.
 */
public class ParameterTambahanListener implements EventListener {

	private List<Row> parameterRows;
	private Combobox paket;
	private Combobox gelombangPendaftaran;
	private Rows rows;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Map<String, LampiranLain> lampiranLains;
	private Boolean formPendaftaran = true;

	/**
	 * Membuat listener terikat pada satu calon mahasiswa dan komponen ZK target.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang parameter tambahannya ditampilkan/diedit
	 * @param parameterRows          daftar baris komponen dinamis yang dibangun, diisi/dibersihkan oleh listener
	 * @param lampiranLains          peta lampiran yang sudah diunggah, berkunci {@code "kelompokId->parameterId"}
	 * @param paket                  combobox paket pendaftaran terpilih, menentukan parameter mana yang relevan
	 * @param gelombangPendaftaran   combobox gelombang pendaftaran terpilih (dipakai bila calon mahasiswa belum punya gelombang tersimpan)
	 * @param formPendaftaran        {@code true} bila dipakai pada form pendaftaran awal, {@code false} bila pada form setelah login
	 * @param rows                   komponen {@link Rows} ZK tempat baris form dipasang
	 */
	public ParameterTambahanListener(BiodataCalonMahasiswa biodataCalonMahasiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Combobox paket, Combobox gelombangPendaftaran,
			Boolean formPendaftaran, Rows rows) {
		this.formPendaftaran = formPendaftaran;
		this.parameterRows = parameterRows;
		this.paket = paket;
		this.gelombangPendaftaran = gelombangPendaftaran;
		this.rows = rows;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
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
			KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) row
					.getAttribute("kelompokParameterTambahanCalonMahasiswa");
			if (parameterTambahan != null && kelompokParameterTambahanCalonMahasiswa != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataCalonMahasiswa.class,
						biodataCalonMahasiswa.getId(),
						kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Mohon maaf, pilihan \"" + parameterTambahan.getLabelInputan() + "\" belum dipilih. Langkah yang dapat dilakukan: (1) pilih nilai pada field tersebut dari daftar yang tersedia; (2) pastikan field tidak dibiarkan kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
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
	 * calon mahasiswa, lalu memvalidasi ulang status awal mahasiswa terhadap kelompok calon
	 * mahasiswa pada gelombang pendaftarannya (skor/kriteria kelompok dapat berubah tergantung
	 * jawaban parameter tambahan) lewat {@link KelompokCalonMahasiswaAction#validasiStatusAwalMahasiswa}.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa target penulisan nilai parameter tambahan
	 */
	@SuppressWarnings("unchecked")
	public void onSave(BiodataCalonMahasiswa biodataCalonMahasiswa) {

		biodataCalonMahasiswa.populateParameterTambahan(parameterRows);

		List<KelompokCalonMahasiswa> kelompokCalonMahasiswas = HibernateUtil.currentSession()
				.createCriteria(KelompokCalonMahasiswa.class)
				.add(Restrictions.eq("gelombangPendaftaran", biodataCalonMahasiswa.getGelombangPendaftaran()))
				.addOrder(Order.asc("skorSampai")).list();
		KelompokCalonMahasiswaAction.validasiStatusAwalMahasiswa(biodataCalonMahasiswa, kelompokCalonMahasiswas);
	}

	/**
	 * Membangun ulang seluruh baris form parameter tambahan sesuai paket dan gelombang pendaftaran
	 * yang sedang terpilih: membersihkan baris lama, meresolusi daftar kelompok parameter yang
	 * relevan (daftar spesifik gelombang bila ada, jika tidak lewat query {@link ParameterTambahanPaket}
	 * yang difilter paket + gelombang + konteks form), lalu untuk setiap kelompok merender baris judul
	 * dan baris input per parameter aktif dalam kelompok, mem-prefill nilai dari data tersimpan pada
	 * {@code biodataCalonMahasiswa}. Berhenti lebih awal tanpa merender apa pun bila gelombang
	 * terpilih menonaktifkan tampilan form tambahan untuk konteks ini.
	 *
	 * @param event event ZK pemicu pembangunan ulang (mis. perubahan paket/gelombang pendaftaran)
	 * @throws Exception diteruskan apa adanya dari kegagalan query atau pembangunan komponen
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		boolean tampilkanLabelBesarPadaFormPMB = Common.bolehKonfigurasi("tampilkan_label_besar_pada_form_PMB");

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		Paket pp = (Paket) (paket == null || paket.getSelectedItem() == null ? null
				: paket.getSelectedItem().getValue());
		if (pp == null && biodataCalonMahasiswa != null) {
			pp = biodataCalonMahasiswa.getPaket();
		}
		GelombangPendaftaran gel = biodataCalonMahasiswa.getGelombangPendaftaran() != null
				? biodataCalonMahasiswa.getGelombangPendaftaran()
				: (GelombangPendaftaran) (gelombangPendaftaran == null || gelombangPendaftaran.getSelectedItem() == null
						? null
						: gelombangPendaftaran.getSelectedItem().getValue());

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();
		List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas;

		// Re-load gel dari session sekarang agar semua field/collection tidak basi
		GelombangPendaftaran gelFresh = (gel != null && gel.getId() != null)
				? (GelombangPendaftaran) session.get(GelombangPendaftaran.class, gel.getId())
				: null;
		GelombangPendaftaran gelEfektif = gelFresh != null ? gelFresh : gel;

		if (gelEfektif != null && formPendaftaran && (!gelEfektif.getTampilFormTambahanSaatRegistrasi() && tbmuser == null)) {
			return;
		} else if (gelEfektif != null && !formPendaftaran && !gelEfektif.getTampilFormTambahanSaatLoginCalonMhs()) {
			return;
		}

		boolean gelHasSpesifik = gelFresh != null
				&& !gelFresh.getKelompokParameterTambahanCalonMahasiswas().isEmpty();

		if (gelHasSpesifik) {
			// Gelombang punya daftar spesifik — pakai HANYA daftar itu
			kelompokParameterTambahanCalonMahasiswas = new ArrayList<KelompokParameterTambahanCalonMahasiswa>(
					gelFresh.getKelompokParameterTambahanCalonMahasiswas());
		} else {
			kelompokParameterTambahanCalonMahasiswas = session.createCriteria(ParameterTambahanPaket.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaGelombang", true),
							gelEfektif == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("gelombangs", ";" + gelEfektif.getId() + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanCalonMahasiswa", "kelompokParameterTambahanCalonMahasiswa")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
					.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa"))
					.add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", pp))).list();
		}
		Collections.sort(kelompokParameterTambahanCalonMahasiswas);

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				biodataCalonMahasiswa.populateParameterTambahan(parameterRows);
			}
		};

		for (KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {

			// Saat gelombang punya daftar spesifik, SEMUA kelompok yang dipilih ditampilkan
			// (admin sudah memilih secara eksplisit, flag tampilDiForm* tidak relevan)
			if (!gelHasSpesifik) {
				if (kelompokParameterTambahanCalonMahasiswa != null && formPendaftaran
						&& (!kelompokParameterTambahanCalonMahasiswa.getTampilDiFormPendaftaran())) {
					continue;
				} else if (kelompokParameterTambahanCalonMahasiswa != null && !formPendaftaran
						&& !kelompokParameterTambahanCalonMahasiswa.getTampilDiFormSetelahLogin()) {
					continue;
				}
			}

			Row rowParameterTambahan = new MyRowStyled();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCalonMahasiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			org.hibernate.Criteria crtParam = session.createCriteria(ParameterTambahanPaket.class)
					.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
							kelompokParameterTambahanCalonMahasiswa))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanCalonMahasiswa",
							"kelompokParameterTambahanCalonMahasiswa")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
					.setProjection(Projections.groupProperty("parameterTambahan.id"))
					.add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", pp)));
			if (!gelHasSpesifik) {
				// Filter per-gelombang hanya berlaku saat tidak ada daftar spesifik
				crtParam.add(Restrictions.or(Restrictions.eq("tampilDiSemuaGelombang", true),
						gelEfektif == null ? Restrictions.sqlRestriction("false")
								: Restrictions.ilike("gelombangs", ";" + gelEfektif.getId() + ";", MatchMode.ANYWHERE)));
			}
			List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(crtParam,
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(tampilkanLabelBesarPadaFormPMB && !parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataCalonMahasiswa.class,
						biodataCalonMahasiswa.getId(),
						kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId());

					Row row = new MyRowStyled();

					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");
					row.setAttribute("kelompokParameterTambahanCalonMahasiswa",
							kelompokParameterTambahanCalonMahasiswa);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = biodataCalonMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/ParameterTambahanListener.java:242");

							}
						}
					}

					tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							biodataCalonMahasiswa.getId(), val, ket, parameterTambahan, isi);

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
