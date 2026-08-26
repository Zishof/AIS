package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.servlet.landing.PesantrenWebsiteConfig;

/**
 * Editor konfigurasi website ePesantren per properti. Pengguna tidak pernah
 * mengedit JSON mentah; struktur JSON dibentuk kembali dari kontrol ZK ketika
 * Yayasan disimpan. Properti yang belum dikenal tetap dipertahankan dan ikut
 * ditampilkan agar penambahan schema di masa depan tidak menghilangkan data.
 */
public class PesantrenWebsiteEditor {

    private static final Map LABEL = new HashMap();
    private static final Map URUTAN = new HashMap();

    static {
        label("schemaVersion", "Versi schema");
        label("identity", "Identitas pesantren");
        label("theme", "Tema dan gambar");
        label("seo", "SEO dan metadata");
        label("announcement", "Bar pengumuman");
        label("navigation", "Navigasi utama");
        label("hero", "Hero halaman utama");
        label("profile", "Profil pondok");
        label("stats", "Statistik ringkas");
        label("unitsSection", "Bagian unit pendidikan");
        label("pillarsSection", "Judul bagian pilar layanan");
        label("pillars", "Pilar layanan");
        label("dailySection", "Judul alur operasional harian");
        label("dailyTimeline", "Alur operasional harian");
        label("workflowSection", "Judul bagian workflow");
        label("workflows", "Workflow operasional");
        label("diagramSection", "Judul bagian diagram");
        label("diagrams", "Diagram ekosistem");
        label("biometric", "Biometrik");
        label("servicesSection", "Judul bagian fasilitas digital");
        label("serviceGroups", "Kelompok fasilitas digital");
        label("gallerySection", "Judul bagian galeri");
        label("gallery", "Galeri");
        label("newsSection", "Bagian berita pondok");
        label("cta", "Ajakan bertindak (CTA)");
        label("contact", "Kontak resmi");
        label("footer", "Footer");
        label("visibility", "Pengaturan bagian yang ditampilkan");
        label("name", "Nama"); label("shortName", "Nama singkat");
        label("motto", "Motto"); label("description", "Deskripsi");
        label("primary", "Warna utama"); label("secondary", "Warna sekunder");
        label("ink", "Warna teks"); label("cream", "Warna latar lembut");
        label("logo", "URL logo"); label("heroImage", "URL gambar hero");
        label("pattern", "Tampilkan pola dekoratif");
        label("title", "Judul"); label("eyebrow", "Judul kecil");
        label("lead", "Ringkasan hero"); label("body", "Isi");
        label("label", "Label"); label("url", "Tautan/URL");
        label("enabled", "Aktif"); label("visible", "Tampil");
        label("value", "Nilai"); label("time", "Waktu");
        label("subtitle", "Subjudul"); label("image", "URL gambar");
        label("caption", "Keterangan gambar"); label("icon", "Ikon/kode");
        label("center", "Titik pusat diagram"); label("items", "Daftar item");
        label("steps", "Tahapan"); label("action", "Tombol aksi");
        label("primaryAction", "Tombol utama");
        label("secondaryAction", "Tombol kedua");
        label("tertiaryAction", "Tombol ketiga");
        label("primaryLabel", "Label tombol utama");
        label("primaryUrl", "URL tombol utama");
        label("secondaryLabel", "Label tombol kedua");
        label("address", "Alamat"); label("phone", "Telepon");
        label("whatsapp", "WhatsApp"); label("email", "Email");
        label("website", "Website resmi"); label("left", "Teks kiri");
        label("right", "Teks kanan"); label("type", "Tipe diagram");

        String[] urutan = new String[] { "schemaVersion", "identity", "theme", "seo", "announcement",
                "navigation", "hero", "profile", "stats", "unitsSection", "pillarsSection", "pillars",
                "dailySection", "dailyTimeline", "workflowSection", "workflows", "diagramSection", "diagrams",
                "biometric", "servicesSection", "serviceGroups", "gallerySection", "gallery", "newsSection",
                "cta", "contact", "footer", "visibility" };
        for (int i = 0; i < urutan.length; i++) URUTAN.put(urutan[i], Integer.valueOf(i));
    }

    private final ObjectEditor root;

    public PesantrenWebsiteEditor(String json) {
        JSONObject value;
        try {
            value = new JSONObject(json == null ? "{}" : json);
        } catch (Exception e) {
            value = new JSONObject();
        }
        root = new ObjectEditor(null, value, true);
    }

    public Component getComponent() {
        return root.component();
    }

    public String toJsonString() throws JSONException {
        JSONObject value = (JSONObject) root.value();
        value.put("schemaVersion", Integer.valueOf(PesantrenWebsiteConfig.SCHEMA_VERSION));
        return value.toString(2);
    }

    private static void label(String key, String value) {
        LABEL.put(key, value);
    }

    private static String labelFor(String key) {
        Object known = LABEL.get(key);
        if (known != null) return known.toString();
        String text = key == null ? "Nilai" : key.replace('_', ' ');
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && text.charAt(i - 1) != ' ') result.append(' ');
            result.append(c);
        }
        if (result.length() > 0) result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        return result.toString();
    }

    private static List orderedKeys(JSONObject object) {
        List keys = new ArrayList();
        for (Iterator it = object.keys(); it.hasNext();) keys.add(String.valueOf(it.next()));
        Collections.sort(keys, new Comparator() {
            public int compare(Object left, Object right) {
                Integer a = (Integer) URUTAN.get(String.valueOf(left));
                Integer b = (Integer) URUTAN.get(String.valueOf(right));
                if (a != null || b != null) {
                    if (a == null) return 1;
                    if (b == null) return -1;
                    return a.compareTo(b);
                }
                return labelFor(String.valueOf(left)).compareToIgnoreCase(labelFor(String.valueOf(right)));
            }
        });
        return keys;
    }

    private static NodeEditor editor(String key, Object value, boolean top) {
        if (value instanceof JSONObject) return new ObjectEditor(key, (JSONObject) value, top);
        if (value instanceof JSONArray) return new ArrayEditor(key, (JSONArray) value);
        return new ScalarEditor(key, value);
    }

    private static Object cloneValue(Object value) {
        try {
            if (value instanceof JSONObject) return new JSONObject(value.toString());
            if (value instanceof JSONArray) return new JSONArray(value.toString());
        } catch (Exception ignored) {
            // Nilai template berasal dari JSON valid; fallback di bawah menjaga editor tetap dapat digunakan.
        }
        return value;
    }

    private static abstract class NodeEditor {
        abstract Component component();
        abstract Object value() throws JSONException;
    }

    private static final class ScalarEditor extends NodeEditor {
        private final Object original;
        private final Hbox row = new Hbox();
        private Textbox text;
        private Checkbox check;

        ScalarEditor(String key, Object value) {
            original = value;
            row.setWidth("100%");
            row.setHflex("1");
            Label label = new Label(labelFor(key));
            label.setWidth("220px");
            label.setStyle("font-weight:600;padding-top:6px;");
            label.setParent(row);
            if (value instanceof Boolean) {
                check = new Checkbox("Ya, tampilkan/aktifkan");
                check.setChecked(((Boolean) value).booleanValue());
                check.setParent(row);
            } else {
                String current = value == null || value == JSONObject.NULL ? "" : String.valueOf(value);
                text = new Textbox(current);
                // Isi seluruh sisa lebar baris setelah kolom label. setWidth saja pada
                // Hbox ZK lama mengikuti lebar natural child, sehingga input terlihat
                // pendek walaupun container sudah 100%.
                text.setWidth("100%");
                text.setHflex("1");
                if (panjang(key, current)) text.setRows(3);
                if ("schemaVersion".equals(key)) text.setReadonly(true);
                text.setParent(row);
            }
        }

        Component component() { return row; }

        Object value() {
            if (check != null) return Boolean.valueOf(check.isChecked());
            String current = text == null ? "" : text.getValue();
            if (original instanceof Integer || original instanceof Long) {
                try { return Integer.valueOf(current.trim()); } catch (Exception e) { return Integer.valueOf(0); }
            }
            if (original instanceof Number) {
                try { return Double.valueOf(current.trim()); } catch (Exception e) { return Double.valueOf(0); }
            }
            return current;
        }

        private static boolean panjang(String key, String value) {
            if (value != null && value.length() > 100) return true;
            return "description".equals(key) || "body".equals(key) || "lead".equals(key)
                    || "caption".equals(key) || "text".equals(key);
        }
    }

    private static final class ObjectEditor extends NodeEditor {
        private final Vbox fields = new Vbox();
        private final List keys = new ArrayList();
        private final List editors = new ArrayList();
        private final Component rootComponent;

        ObjectEditor(String key, JSONObject value, boolean top) {
            fields.setWidth("100%");
            List ordered = orderedKeys(value);
            for (int i = 0; i < ordered.size(); i++) {
                String property = String.valueOf(ordered.get(i));
                Object childValue = value.opt(property);
                NodeEditor child = editor(property, childValue, false);
                keys.add(property);
                editors.add(child);
                child.component().setParent(fields);
            }
            if (top) {
                Vbox outer = new Vbox();
                outer.setWidth("100%");
                Label help = new Label("Edit setiap properti halaman utama. JSON disusun otomatis ketika Yayasan disimpan.");
                help.setWidth("100%");
                help.setStyle("display:block;padding:10px;background:#eef7f5;color:#0f5f58;border-radius:6px;");
                help.setParent(outer);
                fields.setParent(outer);
                rootComponent = outer;
            } else {
                Groupbox group = kelompok(labelFor(key));
                fields.setParent(group);
                rootComponent = group;
            }
        }

        Component component() { return rootComponent; }

        Object value() throws JSONException {
            JSONObject result = new JSONObject();
            for (int i = 0; i < keys.size(); i++) result.put(String.valueOf(keys.get(i)), ((NodeEditor) editors.get(i)).value());
            return result;
        }
    }

    private static final class ArrayEditor extends NodeEditor {
        private final Groupbox group;
        private final Vbox list = new Vbox();
        private final List items = new ArrayList();
        private Object template = "";

        ArrayEditor(String key, JSONArray value) {
            group = kelompok(labelFor(key) + " (daftar)");
            list.setWidth("100%");
            list.setParent(group);
            for (int i = 0; i < value.length(); i++) {
                Object item = value.opt(i);
                if (i == 0) template = cloneValue(item);
                items.add(new ArrayItem(item));
            }
            renderItems();
            Button add = new Button("+ Tambah item");
            add.setStyle("margin:8px 0;");
            add.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    items.add(new ArrayItem(cloneValue(template)));
                    renderItems();
                }
            });
            add.setParent(group);
        }

        Component component() { return group; }

        Object value() throws JSONException {
            JSONArray result = new JSONArray();
            for (int i = 0; i < items.size(); i++) result.put(((ArrayItem) items.get(i)).editor.value());
            return result;
        }

        private void renderItems() {
            while (list.getFirstChild() != null) list.removeChild(list.getFirstChild());
            for (int i = 0; i < items.size(); i++) {
                ArrayItem item = (ArrayItem) items.get(i);
                item.update(i);
                item.group.setParent(list);
            }
        }

        private final class ArrayItem {
            final Groupbox group = kelompok("Item");
            final Caption caption;
            final NodeEditor editor;

            ArrayItem(Object value) {
                caption = (Caption) group.getFirstChild();
                Hbox actions = new Hbox();
                actions.setStyle("margin:0 0 6px 220px;");
                Button up = new Button("Naik");
                Button down = new Button("Turun");
                Button remove = new Button("Hapus");
                up.setParent(actions); down.setParent(actions); remove.setParent(actions);
                actions.setParent(group);
                editor = PesantrenWebsiteEditor.editor("value", value, false);
                editor.component().setParent(group);
                up.addEventListener("onClick", aksiPindah(this, -1));
                down.addEventListener("onClick", aksiPindah(this, 1));
                remove.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        items.remove(ArrayItem.this);
                        renderItems();
                    }
                });
            }

            void update(int index) {
                caption.setLabel("Item " + (index + 1));
            }
        }

        private EventListener aksiPindah(final ArrayItem item, final int arah) {
            return new EventListener() {
                public void onEvent(Event event) throws Exception {
                    int from = items.indexOf(item);
                    int to = from + arah;
                    if (from >= 0 && to >= 0 && to < items.size()) {
                        Collections.swap(items, from, to);
                        renderItems();
                    }
                }
            };
        }
    }

    private static Groupbox kelompok(String title) {
        Groupbox group = new Groupbox();
        group.setWidth("100%");
        group.setHflex("1");
        group.setStyle("box-sizing:border-box;margin:6px 0;padding:6px;border:1px solid #d9e4e2;border-radius:6px;");
        Caption caption = new Caption(title);
        caption.setStyle("font-weight:700;color:#0f5f58;");
        caption.setParent(group);
        return group;
    }
}
