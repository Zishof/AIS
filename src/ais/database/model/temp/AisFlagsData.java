package ais.database.model.temp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ais_flags_data")
public class AisFlagsData implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // Gunakan length = 512. Sangat lega untuk menampung long path, 
    // performa B-Tree tetap di puncak, dan dijamin tidak akan error index row size.
    @Id
    @Column(name = "flag_key", unique = true, nullable = false, length = 512)
    private String flagKey;

    // Untuk value, biarkan menggunakan TEXT karena value tidak di-index (Primary Key)
    @Column(name = "flag_value", columnDefinition = "TEXT")
    private String flagValue;

    // Default constructor (Wajib untuk Hibernate)
    public AisFlagsData() {
    }

    public AisFlagsData(String flagKey, String flagValue) {
        this.flagKey = flagKey;
        this.flagValue = flagValue;
    }

    // --- GETTER & SETTER ---
    
    public String getFlagKey() {
        return flagKey;
    }

    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

    public String getFlagValue() {
        return flagValue;
    }

    public void setFlagValue(String flagValue) {
        this.flagValue = flagValue;
    }
}