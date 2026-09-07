package ais.database.hibernate;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;

/**
 * Menandai entity yang sedang diisi Hibernate melalui property setter.
 *
 * Beberapa entity lama menjalankan aturan bisnis dari setter. Aturan tersebut tidak boleh
 * memicu lazy-load ketika Hibernate masih membaca ResultSet untuk entity yang sama.
 */
public class HibernateHydrationGuard implements PreLoadEventListener, PostLoadEventListener {

	private static final long serialVersionUID = 1L;

	private static final ThreadLocal<Map<Object, Boolean>> ENTITAS_SEDANG_DIMUAT =
			new ThreadLocal<Map<Object, Boolean>>();

	@Override
	public void onPreLoad(PreLoadEvent event) {
		Object entity = event == null ? null : event.getEntity();
		if (entity == null) {
			return;
		}
		Map<Object, Boolean> entities = ENTITAS_SEDANG_DIMUAT.get();
		if (entities == null) {
			entities = new IdentityHashMap<Object, Boolean>();
			ENTITAS_SEDANG_DIMUAT.set(entities);
		}
		entities.put(entity, Boolean.TRUE);
	}

	@Override
	public void onPostLoad(PostLoadEvent event) {
		Object entity = event == null ? null : event.getEntity();
		Map<Object, Boolean> entities = ENTITAS_SEDANG_DIMUAT.get();
		if (entities == null) {
			return;
		}
		if (entity != null) {
			entities.remove(entity);
		}
		if (entities.isEmpty()) {
			ENTITAS_SEDANG_DIMUAT.remove();
		}
	}

	public static boolean sedangMemuat(Object entity) {
		Map<Object, Boolean> entities = ENTITAS_SEDANG_DIMUAT.get();
		return entity != null && entities != null && entities.containsKey(entity);
	}

	/** Bersihkan sisa penanda bila materialisasi entity berhenti sebelum event post-load. */
	public static void bersihkanThread() {
		ENTITAS_SEDANG_DIMUAT.remove();
	}
}
