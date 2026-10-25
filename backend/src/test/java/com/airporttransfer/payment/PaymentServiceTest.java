package com.airporttransfer.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airporttransfer.booking.Booking;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.booking.PaymentMethod;
import com.airporttransfer.booking.PaymentStatus;
import com.airporttransfer.common.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    PaymentRepository payments = mock(PaymentRepository.class);
    PaymentService service = new PaymentService(payments, List.of(new CashGateway()));

    private Booking booking(BookingStatus status, PaymentMethod method, PaymentStatus paymentStatus) {
        Booking b = mock(Booking.class);
        when(b.getId()).thenReturn(10L);
        when(b.getStatus()).thenReturn(status);
        when(b.getPaymentMethod()).thenReturn(method);
        when(b.getPaymentStatus()).thenReturn(paymentStatus);
        when(b.getAmount()).thenReturn(new BigDecimal("14.71"));
        when(b.getCurrency()).thenReturn("EUR");
        return b;
    }

    private static String code(Throwable t) {
        return ((ApiException) t).getCode();
    }

    @Test
    void cashCollectedRecordsTheExactSnapshotAmountAndMarksTheBookingPaid() {
        Booking b = booking(BookingStatus.IN_PROGRESS, PaymentMethod.CASH, PaymentStatus.UNPAID);
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment p = service.collectCash(b, 7L);

        assertThat(p.getAmount()).isEqualByComparingTo("14.71");
        assertThat(p.getCurrency()).isEqualTo("EUR");
        assertThat(p.getProvider()).isEqualTo("CASH");
        verify(b).markPaid(any());
    }

    @Test
    void cashCanAlsoBeRecordedAfterTheTripIsCompleted() {
        Booking b = booking(BookingStatus.COMPLETED, PaymentMethod.CASH, PaymentStatus.UNPAID);
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        service.collectCash(b, 7L);
        verify(b).markPaid(any());
    }

    @Test
    void cashCannotBeCollectedBeforeTheTripStartsOrTwiceOrOnACardBooking() {
        for (BookingStatus early : new BookingStatus[] {BookingStatus.PENDING, BookingStatus.CONFIRMED,
            BookingStatus.ASSIGNED, BookingStatus.CANCELLED}) {
            Booking b = booking(early, PaymentMethod.CASH, PaymentStatus.UNPAID);
            assertThatThrownBy(() -> service.collectCash(b, 7L)).extracting(PaymentServiceTest::code)
                .isEqualTo("PAYMENT_NOT_COLLECTABLE");
            verify(b, never()).markPaid(any());
        }
        Booking paid = booking(BookingStatus.COMPLETED, PaymentMethod.CASH, PaymentStatus.PAID);
        assertThatThrownBy(() -> service.collectCash(paid, 7L)).extracting(PaymentServiceTest::code)
            .isEqualTo("ALREADY_PAID");
        Booking card = booking(BookingStatus.COMPLETED, PaymentMethod.CARD, PaymentStatus.UNPAID);
        assertThatThrownBy(() -> service.collectCash(card, 7L)).extracting(PaymentServiceTest::code)
            .isEqualTo("PAYMENT_METHOD_MISMATCH");
    }

    @Test
    void cancellingAPaidBookingRefundsItThroughItsGateway() {
        Booking b = booking(BookingStatus.CANCELLED, PaymentMethod.CASH, PaymentStatus.PAID);
        Payment stored = new Payment(10L, "CASH", new BigDecimal("14.71"), "EUR", null, 7L);
        when(payments.findByBookingIdAndStatus(10L, Payment.Status.SUCCEEDED)).thenReturn(Optional.of(stored));

        service.refundIfPaid(b);

        assertThat(stored.getStatus()).isEqualTo(Payment.Status.REFUNDED);
        assertThat(stored.getExternalRef()).isEqualTo("manual");
        verify(b).markRefunded();
    }

    @Test
    void cancellingAnUnpaidBookingTouchesNoPayment() {
        Booking b = booking(BookingStatus.CANCELLED, PaymentMethod.CASH, PaymentStatus.UNPAID);
        service.refundIfPaid(b);
        verify(b, never()).markRefunded();
        verify(payments, never()).save(any());
    }

    @Test
    void onlyMethodsWithAGatewayAreAvailable() {
        assertThat(service.isAvailable(PaymentMethod.CASH)).isTrue();
        assertThat(service.isAvailable(PaymentMethod.CARD)).isFalse();
    }
}
