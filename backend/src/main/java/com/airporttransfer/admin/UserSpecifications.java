package com.airporttransfer.admin;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class UserSpecifications {

    private UserSpecifications() {
    }

    static Specification<User> matching(String q, Role role, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (role != null) {
                and.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                and.add(cb.equal(root.get("enabled"), enabled));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + escape(q.trim().toLowerCase(Locale.ROOT)) + "%";
                and.add(cb.or(
                    cb.like(cb.lower(root.<String>get("email")), like, '\\'),
                    cb.like(cb.lower(root.<String>get("fullName")), like, '\\'),
                    cb.like(cb.lower(root.<String>get("phone")), like, '\\')));
            }
            return cb.and(and.toArray(new Predicate[0]));
        };
    }

    /** The search text is data, not a pattern: neutralize LIKE wildcards. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
