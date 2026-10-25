package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.HistoryEntryDto;
import com.airporttransfer.booking.StaffDtos.StaffBookingDto;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.PersonDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Builds the staff views of bookings, loading customers, drivers and history for a whole page at once. */
@Component
class BookingViews {

    private final BookingMapper mapper;
    private final UserRepository users;
    private final UserMapper userMapper;
    private final BookingStatusHistoryRepository history;

    BookingViews(BookingMapper mapper, UserRepository users, UserMapper userMapper,
                 BookingStatusHistoryRepository history) {
        this.mapper = mapper;
        this.users = users;
        this.userMapper = userMapper;
        this.history = history;
    }

    List<StaffBookingDto> staff(List<Booking> bookings, boolean withHistory) {
        Set<Long> ids = new HashSet<>();
        bookings.forEach(b -> {
            ids.add(b.getCustomerId());
            if (b.getAssignedDriverId() != null) {
                ids.add(b.getAssignedDriverId());
            }
        });
        Map<Long, User> people = users.findAllById(ids).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return bookings.stream().map(b -> new StaffBookingDto(
            mapper.toDto(b),
            person(people.get(b.getCustomerId())),
            b.getAssignedDriverId() == null ? null : person(people.get(b.getAssignedDriverId())),
            withHistory ? historyOf(b.getId()) : null)).toList();
    }

    PersonDto customer(Booking booking) {
        return users.findById(booking.getCustomerId()).map(userMapper::toPerson).orElse(null);
    }

    private List<HistoryEntryDto> historyOf(Long bookingId) {
        List<BookingStatusHistory> rows = history.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId);
        Set<Long> actorIds = rows.stream().map(BookingStatusHistory::getActorId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = users.findAllById(actorIds).stream()
            .collect(Collectors.toMap(User::getId, User::getFullName));
        return rows.stream().map(h -> new HistoryEntryDto(h.getFromStatus(), h.getToStatus(), h.getActorId(),
            h.getActorId() == null ? null : names.get(h.getActorId()), h.getActorRole(), h.getNote(),
            h.getCreatedAt())).toList();
    }

    private PersonDto person(User user) {
        return user == null ? null : userMapper.toPerson(user);
    }
}
