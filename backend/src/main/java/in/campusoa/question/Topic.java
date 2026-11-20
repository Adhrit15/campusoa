package in.campusoa.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Topic tag (DSA, DSA > Graphs, DBMS ...). Self-referencing: sub-topics point at their parent. */
@Entity
@Table(name = "topic")
public class Topic {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected Topic() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Long getParentId() { return parentId; }
    public short getSortOrder() { return sortOrder; }
}
