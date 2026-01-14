package mephi.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.bookingservice.dto.BookingRequest;
import mephi.bookingservice.dto.BookingResponse;
import mephi.bookingservice.entity.BookingStatus;
import mephi.bookingservice.exception.BookingException;
import mephi.bookingservice.exception.ResourceNotFoundException;
import mephi.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(BookingControllerTest.TestSecurityConfig.class)
@DisplayName("BookingController MockMvc Tests")
class BookingControllerTest {
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain bookingTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/bookings").hasRole("ADMIN")
                    .requestMatchers("/bookings/status/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingRequest = new BookingRequest();
        bookingRequest.setRoomId(1L);
        bookingRequest.setHotelId(1L);
        bookingRequest.setCheckInDate(LocalDate.now().plusDays(1));
        bookingRequest.setCheckOutDate(LocalDate.now().plusDays(3));
        bookingRequest.setGuestCount(2);

        bookingResponse = BookingResponse.builder()
            .id(1L)
            .bookingReference("BK-123456")
            .roomId(1L)
            .hotelId(1L)
            .status(BookingStatus.CONFIRMED)
            .checkInDate(LocalDate.now().plusDays(1))
            .checkOutDate(LocalDate.now().plusDays(3))
            .guestCount(2)
            .totalPrice(BigDecimal.valueOf(200))
            .build();
    }

    @Nested
    @DisplayName("POST /bookings")
    class CreateBooking {
        @Test
        @DisplayName("should create booking when authenticated and valid request")
        void should_CreateBooking_When_ValidRequest() throws Exception {
            given(bookingService.createBooking(any(BookingRequest.class), eq("john_doe")))
                .willReturn(bookingResponse);

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/bookings")
                    .with(csrf())
                    .with(user(authUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingRequest))
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference", is("BK-123456")))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.totalPrice", is(200)));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            mockMvc.perform(
                post("/bookings")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingRequest))
            )
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when room not available")
        void should_Return400_When_RoomNotAvailable() throws Exception {
            given(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .willThrow(new BookingException("Room is not available"));

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/bookings")
                    .with(csrf())
                    .with(user(authUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingRequest))
            )
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when dates are invalid")
        void should_Return400_When_InvalidDates() throws Exception {
            bookingRequest.setCheckOutDate(LocalDate.now()); // Before check-in
            bookingRequest.setCheckInDate(LocalDate.now().plusDays(5));

            given(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .willThrow(new BookingException("Check-out date must be after check-in date"));

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/bookings")
                    .with(csrf())
                    .with(user(authUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingRequest))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /bookings/my")
    class GetMyBookings {
        @Test
        @DisplayName("should return user's bookings when authenticated")
        void should_ReturnUserBookings_When_Authenticated() throws Exception {
            given(bookingService.getBookingsByUser("john_doe")).willReturn(List.of(bookingResponse));

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/my")
                    .with(user(authUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookingReference", is("BK-123456")));
        }

        @Test
        @DisplayName("should return empty list when user has no bookings")
        void should_ReturnEmptyList_When_NoBookings() throws Exception {
            given(bookingService.getBookingsByUser("john_doe")).willReturn(List.of());

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/my")
                    .with(user(authUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            mockMvc.perform(get("/bookings/my"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /bookings/{id}")
    class GetBookingById {
        @Test
        @DisplayName("should return booking when found")
        void should_ReturnBooking_When_Found() throws Exception {
            given(bookingService.getBookingById(1L)).willReturn(bookingResponse);

            UserDetails authUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/1")
                    .with(user(authUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.bookingReference", is("BK-123456")));
        }

        @Test
        @DisplayName("should return 404 when booking not found")
        void should_Return404_When_BookingNotFound() throws Exception {
            given(bookingService.getBookingById(999L))
                .willThrow(new ResourceNotFoundException("Booking", 999L));

            UserDetails authUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/999")
                    .with(user(authUser))
            )
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /bookings/reference/{reference}")
    class GetBookingByReference {
        @Test
        @DisplayName("should return booking when found by reference")
        void should_ReturnBooking_When_FoundByReference() throws Exception {
            given(bookingService.getBookingByReference("BK-123456")).willReturn(bookingResponse);

            UserDetails authUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/reference/BK-123456")
                    .with(user(authUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference", is("BK-123456")));
        }

        @Test
        @DisplayName("should return 404 when reference not found")
        void should_Return404_When_ReferenceNotFound() throws Exception {
            given(bookingService.getBookingByReference("INVALID"))
                .willThrow(new ResourceNotFoundException("Booking", "reference", "INVALID"));

            UserDetails authUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/reference/INVALID")
                    .with(user(authUser))
            )
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /bookings/{id}/cancel")
    class CancelBooking {
        @Test
        @DisplayName("should cancel booking when user is owner")
        void should_CancelBooking_When_UserIsOwner() throws Exception {
            BookingResponse cancelledResponse = BookingResponse.builder()
                .id(1L)
                .bookingReference("BK-123456")
                .status(BookingStatus.CANCELLED)
                .build();

            given(bookingService.cancelBooking(eq(1L), eq("john_doe"), anyString(), eq(false)))
                .willReturn(cancelledResponse);

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/bookings/1/cancel")
                    .with(csrf())
                    .with(user(authUser))
                    .param("reason", "Change of plans")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @DisplayName("should cancel booking when user is admin")
        void should_CancelBooking_When_UserIsAdmin() throws Exception {
            BookingResponse cancelledResponse = BookingResponse.builder()
                .id(1L)
                .status(BookingStatus.CANCELLED)
                .build();

            given(bookingService.cancelBooking(eq(1L), eq("admin"), anyString(), eq(true)))
                .willReturn(cancelledResponse);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/bookings/1/cancel")
                    .with(csrf())
                    .with(user(adminUser))
                    .param("reason", "Admin cancellation")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @DisplayName("should return 400 when booking cannot be cancelled")
        void should_Return400_When_CannotCancel() throws Exception {
            given(bookingService.cancelBooking(eq(1L), anyString(), any(), anyBoolean()))
                .willThrow(new BookingException("Booking cannot be cancelled"));

            UserDetails authUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/bookings/1/cancel")
                    .with(csrf())
                    .with(user(authUser))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /bookings (Admin)")
    class GetAllBookingsAdmin {
        @Test
        @DisplayName("should return all bookings when admin")
        void should_ReturnAllBookings_When_Admin() throws Exception {
            given(bookingService.getAllBookings()).willReturn(List.of(bookingResponse));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                get("/bookings")
                    .with(user(adminUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void should_Return403_When_NotAdmin() throws Exception {
            UserDetails regularUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings")
                    .with(user(regularUser))
            )
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /bookings/status/{status} (Admin)")
    class GetBookingsByStatusAdmin {
        @Test
        @DisplayName("should return bookings filtered by status when admin")
        void should_ReturnBookingsByStatus_When_Admin() throws Exception {
            given(bookingService.getBookingsByStatus(BookingStatus.CONFIRMED))
                .willReturn(List.of(bookingResponse));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                get("/bookings/status/CONFIRMED")
                    .with(user(adminUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("CONFIRMED")));
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void should_Return403_When_NotAdmin() throws Exception {
            UserDetails regularUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                get("/bookings/status/CONFIRMED")
                    .with(user(regularUser))
            )
                .andExpect(status().isForbidden());
        }
    }
}
