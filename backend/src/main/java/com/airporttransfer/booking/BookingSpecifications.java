package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.BookingFilter;
import com.airporttransfer.user.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class BookingSpecifications {

    private BookingSpecifications() {
    }

    static Specification<Booking> matching(BookingFilter f) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (f.status() != null) {
                and.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.from() != null) {
                and.add(cb.greaterThanOrEqualTo(root.<java.time.Instant>get("pickupAt"), f.from()));
            }
            if (f.to() != null) {
                and.add(cb.lessThan(root.<java.time.Instant>get("pickupAt"), f.to()));
            }
            if (f.airportId() != null) {
                and.add(cb.equal(root.get("airport").get("id"), f.airportId()));
            }
            if (f.driverId() != null) {
                and.add(cb.equal(root.get("assignedDriverId"), f.driverId()));
            }
            if (Boolean.TRUE.equals(f.unassigned())) {
                and.add(cb.isNull(root.get("assignedDriverId")));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + escape(f.q().trim().toLowerCase(Locale.ROOT)) + "%";
                Subquery<Long> customers = query.subquery(Long.class);
                Root<User> u = customers.from(User.class);
                customers.select(u.<Long>get("id")).where(cb.or(
                    cb.like(cb.lower(u.<String>get("email")), like, '\\'),
                    cb.like(cb.lower(u.<String>get("fullName")), like, '\\'),
                    cb.like(cb.lower(u.<String>get("phone")), like, '\\')));
                and.add(cb.or(
                    cb.like(cb.lower(root.<String>get("reference")), like, '\\'),
                    root.get("customerId").in(customers)));
            }
            return cb.and(and.toArray(new Predicate[0]));
        };
    }

    /** The search text is data, not a pattern: neutralize LIKE wildcards. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
