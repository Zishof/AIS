package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.VOPembelajaran;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.PertemuanPunyaUjian} (relasi ujian yang terpasang pada satu pertemuan) —
 * lihat Javadoc class induk untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code nama}, {@code keterangan}. Kelas ini punya dua mode pembatasan sesuai
 * konstruktor yang dipanggil: (1) dibatasi ke SATU {@link ais.database.model.Pertemuan} tertentu lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter}, dipakai jalur "Revisi" lama; atau (2) dibatasi ke
 * SELURUH pertemuan milik satu {@link ais.database.model.VOPembelajaran} (mata kuliah/kelas yang sedang
 * dibuka) lewat {@link #filterPembelajaran(VOPembelajaran)}, dipakai jalur "Recovery" oleh
 * {@link RecoveryAktivitasPembelajaranHelper#bukaRecoveryUjian(VOPembelajaran, EventListener)}. Tidak
 * ada override hook {@code afterRestoreInTransaction}.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanPunyaUjianHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link PertemuanPunyaUjian} yang dibatasi ke satu
     * {@link Pertemuan} tertentu (jalur "Revisi" lama, dipanggil dari menu detail pertemuan).
     *
     * @param pertemuan     pertemuan yang riwayat relasi ujiannya ingin ditampilkan; diteruskan
     *                      sebagai nilai tetap ke {@link GenericRevisiHelper.FixedPropertyFilter}
     *                      pada properti {@code "pertemuan"}.
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}.
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}.
     */
    public RevisiPertemuanPunyaUjianHelper(Pertemuan pertemuan, EventListener eventListener) throws Exception {
        super(PertemuanPunyaUjian.class, "Revisi Ujian Pertemuan", eventListener, new String[] { "nama", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("pertemuan", pertemuan));
    }

	/**
	 * Membuka jendela recovery {@link PertemuanPunyaUjian} yang dibatasi ke SELURUH pertemuan milik
	 * satu {@link VOPembelajaran} (mata kuliah/kelas yang sedang dibuka), lewat filter yang dibangun
	 * {@link #filterPembelajaran(VOPembelajaran)}. Dipakai oleh tombol "Recovery" pada tab Ujian.
	 *
	 * @param pembelajaran  konteks mata kuliah/kelas yang membatasi cakupan pertemuan; boleh
	 *                      {@code null} (filter jatuh ke ID pertemuan tidak valid sehingga hasil kosong).
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}.
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}.
	 */
	public RevisiPertemuanPunyaUjianHelper(VOPembelajaran pembelajaran, EventListener eventListener)
			throws Exception {
		super(PertemuanPunyaUjian.class, "Recovery Jadwal Ujian Pertemuan", eventListener,
				new String[] { "nama", "keterangan" }, filterPembelajaran(pembelajaran));
	}

	/**
	 * Membangun {@link GenericRevisiHelper.QueryCustomizer} yang membatasi query riwayat pada FK
	 * {@code pertemuan} ke kumpulan ID pertemuan milik {@code pembelajaran} (diambil lewat
	 * {@link VOPembelajaran#ambilPertemuan()}). Bila {@code pembelajaran} null atau tidak punya
	 * pertemuan sama sekali, filter sengaja dibuat selalu tidak cocok
	 * ({@code AuditEntity.relatedId("pertemuan").eq(-1L)}) alih-alih menampilkan seluruh data —
	 * fail-closed agar recovery tidak pernah bocor ke pertemuan mata kuliah/kelas lain.
	 *
	 * @param pembelajaran konteks mata kuliah/kelas; boleh {@code null}.
	 * @return customizer yang menambahkan kondisi disjungsi ID pertemuan (atau kondisi selalu-kosong)
	 *         ke {@link AuditQuery}.
	 */
	private static GenericRevisiHelper.QueryCustomizer filterPembelajaran(final VOPembelajaran pembelajaran) {
		return new GenericRevisiHelper.QueryCustomizer() {
			@Override
			public void apply(Session session, AuditQuery query) throws Exception {
				TreeMap<String, Long> data = pembelajaran == null ? null : pembelajaran.ambilPertemuan();
				List<Long> ids = new ArrayList<Long>();
				if (data != null) {
					for (Long id : data.values()) {
						if (id != null) {
							ids.add(id);
						}
					}
				}
				if (ids.isEmpty()) {
					query.add(AuditEntity.relatedId("pertemuan").eq(Long.valueOf(-1L)));
				} else {
					AuditDisjunction salahSatuPertemuan = AuditEntity.disjunction();
					for (int i = 0; i < ids.size(); i++) {
						salahSatuPertemuan.add(AuditEntity.relatedId("pertemuan").eq(ids.get(i)));
					}
					query.add(salahSatuPertemuan);
				}
			}
		};
	}
}
