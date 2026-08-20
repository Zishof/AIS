package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/** Immutable business audit event for repository workflow transitions. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = false)
@Table(schema = "public", name = "repo_workflow_event")
public class RepoWorkflowEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long itemId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String commentText;
    private String actorId;
    private String actorName;
    private String requestId;
    private Date createdAt;

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, nullable = false, unique = true)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    @Column(name = "from_status", length = 40)
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    @Column(name = "to_status", nullable = false, length = 40)
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    @Column(name = "action", nullable = false, length = 40)
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    @Column(name = "comment_text", columnDefinition = "TEXT")
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    @Column(name = "actor_id", nullable = false, length = 255)
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    @Column(name = "actor_name", length = 500)
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    @Column(name = "request_id", length = 100)
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
