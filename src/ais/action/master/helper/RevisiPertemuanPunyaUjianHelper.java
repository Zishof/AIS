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
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanPunyaUjianHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiPertemuanPunyaUjianHelper(Pertemuan pertemuan, EventListener eventListener) throws Exception {
        super(PertemuanPunyaUjian.class, "Revisi Ujian Pertemuan", eventListener, new String[] { "nama", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("pertemuan", pertemuan));
    }

	public RevisiPertemuanPunyaUjianHelper(VOPembelajaran pembelajaran, EventListener eventListener)
			throws Exception {
		super(PertemuanPunyaUjian.class, "Recovery Jadwal Ujian Pertemuan", eventListener,
				new String[] { "nama", "keterangan" }, filterPembelajaran(pembelajaran));
	}

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
