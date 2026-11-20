package in.campusoa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lookup table of branches (CSE, IT, ECE ...). Read-only from the app's point of view. */
@Entity
@Table(name = "branch")
public class Branch {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected Branch() {}   // JPA needs a no-arg constructor

    public String getCode() { return code; }
    public String getName() { return name; }
    public short getSortOrder() { return sortOrder; }
}
